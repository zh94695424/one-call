package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerContextRepository;
import com.onecall.core.evidence.internal.shareprojection.JooqShareableEvidenceProjectionReader;
import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

class DatabaseRuntimeAclRbacHardeningIT {
    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
    private static final String SCRIPT = "V014__database_runtime_acl_rbac_hardening.sql";
    private static final List<String> EXTERNAL = List.of("pa26_lq", "pa26_lc", "pa26_lw");
    private static final List<String> DIRECT = List.of("sharing_public_read_role", "sharing_admission_role", "evidence_media_reader_role");
    private static final Map<String, List<String>> CUSTOMER = Map.of(
            "customer_iam.customer_account", List.of("id", "display_name", "status"),
            "organization_access.organization", List.of("id", "display_name", "status"),
            "organization_access.organization_membership", List.of("organization_id", "account_id", "role", "status"));
    private static final Map<String, List<String>> EVIDENCE = Map.of(
            "evidence.evidence_record", List.of("id", "subject_id", "status", "source_type", "source_ref_id", "identity_at_capture"),
            "evidence.evidence_artifact", List.of("evidence_id", "media_object_id", "artifact_role"),
            "evidence.media_object", List.of("id", "sha256", "size_bytes", "mime_type"),
            "evidence.recording", List.of("media_object_id", "validated_duration"));
    private static final Map<String, String> OWNERS = Map.of(
            "customer_iam", "customer_iam_owner", "organization_access", "organization_access_owner", "evidence", "evidence_owner");
    private static final Map<String, String> FROZEN = Map.ofEntries(
            Map.entry("001", "946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045"),
            Map.entry("002", "534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5"),
            Map.entry("003", "1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1"),
            Map.entry("004", "a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc"),
            Map.entry("005", "2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310"),
            Map.entry("006", "830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475"),
            Map.entry("007", "bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659"),
            Map.entry("008", "2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e"),
            Map.entry("009", "ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae"),
            Map.entry("010", "fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343"),
            Map.entry("011", "f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b"),
            Map.entry("012", "13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828"),
            Map.entry("013", "691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6"));
    private static final String OBJECTS = """
            WITH objects AS (
                SELECT 'schema' AS kind, namespace.oid, 0 AS subid, namespace.nspname AS identity,
                    '' AS column_name, namespace.nspowner AS owner,
                    coalesce(namespace.nspacl, acldefault('n', namespace.nspowner)) AS acl
                FROM pg_namespace namespace WHERE namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
                UNION ALL
                SELECT CASE WHEN relation.relkind='S' THEN 'sequence' ELSE 'table' END, relation.oid, 0,
                    namespace.nspname||'.'||relation.relname, '', relation.relowner,
                    coalesce(relation.relacl, acldefault(CASE WHEN relation.relkind='S' THEN 's'::"char" ELSE 'r'::"char" END, relation.relowner))
                FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
                WHERE namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
                UNION ALL
                SELECT 'column', relation.oid, attribute.attnum, namespace.nspname||'.'||relation.relname,
                    attribute.attname, relation.relowner, attribute.attacl
                FROM pg_attribute attribute JOIN pg_class relation ON relation.oid=attribute.attrelid
                    JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
                WHERE attribute.attnum>0 AND NOT attribute.attisdropped AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
                UNION ALL
                SELECT 'routine', routine.oid, 0, routine.oid::regprocedure::text, '', routine.proowner,
                    coalesce(routine.proacl, acldefault('f', routine.proowner))
                FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid=routine.pronamespace
                WHERE namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
            )
            """;
    private String password;
    private GenericContainer<?> postgres;
    private String url;
    private Flyway latest;
    private UUID account;
    private UUID evidence;
    private UUID subject;

    @BeforeEach
    void start() throws Exception {
        password = UUID.randomUUID().toString();
        postgres = new GenericContainer<>(DockerImageName.parse("postgres:16"))
                .withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", password)
                .withEnv("POSTGRES_DB", "pa26").withEnv("POSTGRES_INITDB_ARGS", "--lc-collate=C --lc-ctype=C")
                .withExposedPorts(5432).waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
                .withStartupTimeout(Duration.ofMinutes(2));
        postgres.start();
        url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/pa26";
        assertThat(Flyway.configure().dataSource(url, "fixture_admin", password).locations("classpath:db/migration")
                .target("013").load().migrate().migrationsExecuted).isEqualTo(13);
        latest = Flyway.configure().dataSource(url, "fixture_admin", password).locations("classpath:db/migration").target("014").load();
        postgres.copyFileToContainer(MountableFile.forHostPath(Path.of("ops/postgresql/pa25-runtime-identities.sql").toAbsolutePath()), "/tmp/pa26-identities.sql");
        account = UUID.randomUUID(); evidence = UUID.randomUUID(); subject = UUID.randomUUID();
    }

    @AfterEach
    void stop() { if (postgres != null) postgres.stop(); }

    @Test
    void v014UpgradesFrozenPrefixAndMatchesIndependentAclOracle() throws Exception {
        for (boolean credentialed : List.of(false, true)) {
            if (credentialed) { stop(); start(); provisionFrozenGraph(); }
            String privateBefore = identities();
            assertThat(latest.migrate().migrationsExecuted).isEqualTo(1);
            assertOracle();
            assertThat(identities().equals(privateBefore)).isTrue();
            var current = Flyway.configure().dataSource(url, "fixture_admin", password).locations("classpath:db/migration").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
            current.validate();
            assertThat(current.info().applied()).hasSize(15);
            psql("apply"); psql("check");
        }
    }

    @Test
    void v014FreshChainHasExactHistoryAndImmutablePredecessors() throws Exception {
        stop();
        password = UUID.randomUUID().toString();
        postgres = new GenericContainer<>(DockerImageName.parse("postgres:16"))
                .withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", password).withEnv("POSTGRES_DB", "pa26")
                .withExposedPorts(5432).waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));
        postgres.start();
        url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/pa26";
        latest = Flyway.configure().dataSource(url, "fixture_admin", password).locations("classpath:db/migration").target("014").load();
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(14);
        latest.validate();
        assertThat(latest.info().applied()).hasSize(14);
        assertThat(latest.info().pending()).isEmpty();
        try (Connection connection = admin()) {
            assertThat(strings(connection, "SELECT installed_rank||'|'||version||'|'||type||'|'||success FROM public.flyway_schema_history ORDER BY installed_rank"))
                    .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 14)
                            .mapToObj(rank -> rank + "|%03d|SQL|true".formatted(rank)).toList());
            var resolved = latest.info().applied();
            for (var migration : resolved) {
                assertThat(one(connection, "SELECT count(*) FROM public.flyway_schema_history WHERE script='" + migration.getScript()
                        + "' AND checksum=" + migration.getChecksum() + " AND description='" + migration.getDescription() + "'"))
                        .isEqualTo("1");
            }
        }
        assertFrozen();
        assertOracle();
    }

    @Test
    void customerContextUsesOnlyApprovedColumnsAcrossAllCurrentStates() throws Exception {
        latest.migrate(); provisionFrozenGraph(); seedAccount();
        var caller = new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB);
        List<UUID> organizations = List.of(UUID.fromString("00000000-0000-0000-0000-000000000002"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        try (Connection connection = admin()) {
            for (UUID organization : organizations) execute(connection, "SET ROLE organization_access_owner; INSERT INTO organization_access.organization(id,display_name) VALUES ('"
                    + organization + "','synthetic'); INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role) VALUES ('"
                    + UUID.randomUUID() + "','" + organization + "','" + account + "','ADMINISTRATOR'); RESET ROLE");
        }
        try (Connection connection = login("pa26_lc")) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            var repository = new JooqCustomerContextRepository(DSL.using(connection, SQLDialect.POSTGRES));
            var context = repository.findActiveContext(caller).orElseThrow();
            assertThat(context.workspaces()).hasSize(3);
            assertThat(context.workspaces().stream().skip(1).map(workspace -> workspace.organizationId()).toList())
                    .containsExactly(organizations.get(1), organizations.get(0));
            assertThat(repository.findActiveContext(new TrustedCustomerCaller(UUID.randomUUID(), TrustedCustomerCaller.Channel.WEB))).isEmpty();
            connection.rollback();
        }
        try (Connection connection = admin()) {
            execute(connection, "SET ROLE organization_access_owner; UPDATE organization_access.organization SET status='DISABLED',revision=revision+1 WHERE id='"
                    + organizations.get(0) + "'; UPDATE organization_access.organization_membership SET status='SUSPENDED',revision=revision+1 WHERE organization_id='"
                    + organizations.get(1) + "'; RESET ROLE");
        }
        try (Connection connection = login("pa26_lc")) {
            execute(connection, "SET ROLE customer_iam_role");
            assertThat(new JooqCustomerContextRepository(DSL.using(connection, SQLDialect.POSTGRES)).findActiveContext(caller).orElseThrow().workspaces()).hasSize(1);
        }
        try (Connection connection = admin()) { execute(connection, "SET ROLE customer_iam_owner; UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1; RESET ROLE"); }
        try (Connection connection = login("pa26_lc")) {
            execute(connection, "SET ROLE customer_iam_role");
            assertThat(new JooqCustomerContextRepository(DSL.using(connection, SQLDialect.POSTGRES)).findActiveContext(caller)).isEmpty();
        }
    }

    @Test
    void sharingProjectionUsesOnlyApprovedEvidenceColumns() throws Exception {
        latest.migrate(); provisionFrozenGraph(); seedEvidence();
        try (Connection connection = login("pa26_lw")) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE evidence_role");
            var reader = new JooqShareableEvidenceProjectionReader();
            var context = DSL.using(connection, SQLDialect.POSTGRES);
            var found = (ShareableEvidenceProjectionReader.ReadResult.Found) reader.readShareableProjection(context, evidence);
            assertThat(found.projection().artifacts()).hasSize(2);
            assertThat(found.projection().artifacts().getFirst().validatedDurationSeconds()).isEqualTo(60);
            assertThat(found.projection().artifacts().getLast().validatedDurationSeconds()).isNull();
            assertThat(reader.readShareableProjection(context, UUID.randomUUID())).isInstanceOf(ShareableEvidenceProjectionReader.ReadResult.Missing.class);
            connection.rollback();
        }
        try (Connection connection = admin()) {
            UUID media = UUID.randomUUID();
            execute(connection, "SET ROLE evidence_owner; INSERT INTO evidence.media_object(id,storage_provider,bucket,object_key,object_version_id,sha256,size_bytes,mime_type,crypto_key_ref) VALUES ('" + media + "','COS','synthetic','" + media + "','v1',repeat('d',64),1,'audio/wav','synthetic')");
            execute(connection, "INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) VALUES ('" + UUID.randomUUID() + "','" + evidence + "','" + media + "','DERIVED'); RESET ROLE");
        }
        try (Connection connection = login("pa26_lw")) {
            execute(connection, "SET ROLE evidence_role");
            assertThat(new JooqShareableEvidenceProjectionReader().readShareableProjection(DSL.using(connection, SQLDialect.POSTGRES), evidence))
                    .isInstanceOf(ShareableEvidenceProjectionReader.ReadResult.Unavailable.class);
        }
    }

    @Test
    void customerRoleDeniesEveryRemovedColumnWriteRoutineAndForeignObject() throws Exception {
        latest.migrate(); provisionFrozenGraph();
        denyRemoved("customer_iam_role", "pa26_lc", CUSTOMER,
                List.of("customer_iam.customer_account", "customer_iam.auth_identity", "customer_iam.identity_profile",
                        "organization_access.organization", "organization_access.organization_membership"));
    }

    @Test
    void evidenceRoleDeniesEveryRemovedColumnWriteRoutineAndForeignObject() throws Exception {
        latest.migrate(); provisionFrozenGraph();
        denyRemoved("evidence_role", "pa26_lw", EVIDENCE,
                List.of("evidence.evidence_record", "evidence.evidence_artifact", "evidence.media_object", "evidence.recording", "evidence.evidence_event"));
    }

    @Test
    void assumedRolesAndPoolRestorationPreserveRuntimeBoundaries() throws Exception {
        latest.migrate(); provisionFrozenGraph();
        List<String> targets;
        try (Connection connection = admin()) { targets = strings(connection, "SELECT rolname FROM pg_roles WHERE rolname LIKE 'pg_%' OR rolname LIKE '%_owner' OR rolname IN ('sharing_role','customer_iam_role','sharing_command_role','evidence_role','sharing_public_read_role','sharing_admission_role','evidence_media_reader_role') ORDER BY 1"); }
        for (String login : List.of("pa26_lc", "pa26_lw")) {
            List<String> approved = login.equals("pa26_lc") ? List.of("customer_iam_role") : List.of("sharing_command_role", "evidence_role");
            try (Connection connection = login(login)) {
                denied(connection, "SELECT id FROM customer_iam.customer_account", "42501");
                denied(connection, "SELECT id FROM evidence.evidence_record", "42501");
                String pid = one(connection, "SELECT pg_backend_pid()");
                for (String sourceRole : approved) {
                    execute(connection, "SET ROLE " + sourceRole);
                    for (String target : targets) {
                        if (approved.contains(target)) execute(connection, "SET ROLE " + target);
                        else denied(connection, "SET ROLE " + target, "42501");
                        assertThat(one(connection, "SELECT session_user")).isEqualTo(login);
                        assertThat(one(connection, "SELECT pg_backend_pid()")).isEqualTo(pid);
                    }
                }
                execute(connection, "SET ROLE " + login + "; RESET ROLE; SET ROLE NONE");
                assertThat(one(connection, "SELECT current_user")).isEqualTo(login);
            }
            try (var pool = new HikariDataSource()) {
                pool.setJdbcUrl(url); pool.setUsername(login); pool.setPassword(password); pool.setMaximumPoolSize(1);
                for (boolean commit : List.of(false, true)) {
                    try (Connection connection = pool.getConnection()) {
                        connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE " + approved.getFirst());
                        if (commit) connection.commit(); else connection.rollback();
                    }
                    try (Connection connection = pool.getConnection()) {
                        assertThat(one(connection, "SELECT session_user=current_user")).isEqualTo("t");
                        assertThat(connection.getAutoCommit()).isTrue(); assertThat(connection.isReadOnly()).isFalse();
                    }
                }
            }
        }
        for (String direct : DIRECT) {
            try (Connection connection = login(direct)) {
                for (String role : List.of("customer_iam_role", "evidence_role", "sharing_command_role", "customer_iam_owner", "pg_read_all_data")) denied(connection, "SET ROLE " + role, "42501");
            }
        }
        try (Connection connection = login("pa26_lw")) {
            execute(connection, "BEGIN READ ONLY; SET LOCAL ROLE sharing_command_role");
            denied(connection, "UPDATE sharing.evidence_share_grant SET status=status WHERE false", "25006");
            execute(connection, "ROLLBACK");
        }
    }

    @Test
    void ownerInvokerGuardsAndSharingWritesRemainOperational() throws Exception {
        latest.migrate(); provisionFrozenGraph(); seedEvidence();
        UUID identity = UUID.randomUUID(); UUID profile = UUID.randomUUID();
        try (Connection connection = admin()) {
            execute(connection, "SET ROLE customer_iam_owner");
            execute(connection, "INSERT INTO customer_iam.auth_identity(id,account_id,provider,provider_scope,provider_subject_ciphertext,provider_subject_hmac,verified_at,key_version) VALUES ('"
                    + identity + "','" + account + "','PHONE','synthetic','v1.key.synthetic',repeat('a',64),now(),1)");
            execute(connection, "INSERT INTO customer_iam.identity_profile(id,account_id) VALUES ('" + profile + "','" + account + "')");
            execute(connection, "UPDATE customer_iam.customer_account SET display_name='updated',revision=revision+1; UPDATE customer_iam.auth_identity SET status='DISABLED',revision=revision+1; UPDATE customer_iam.identity_profile SET revision=revision+1");
            for (String table : List.of("customer_account", "auth_identity", "identity_profile")) denied(connection, "UPDATE customer_iam." + table + " SET id='" + UUID.randomUUID() + "'", "P0001");
            denied(connection, "INSERT INTO customer_iam.identity_profile(id,account_id) VALUES ('" + UUID.randomUUID() + "','" + UUID.randomUUID() + "')", "23503");
        }
        try (Connection connection = login("pa26_lw")) {
            execute(connection, "SET ROLE sharing_command_role");
            execute(connection, "INSERT INTO sharing.evidence_share_grant(id,share_id,capability_token_hash,evidence_id,created_by_account_id,verification_mode,valid_from,expires_at,content_snapshot,content_sha256) VALUES ('"
                    + UUID.randomUUID() + "','synthetic-pa26',repeat('e',64),'" + evidence + "','" + account + "','NONE',now(),now()+interval '1 hour',"
                    + "jsonb_build_object('content',jsonb_build_object('evidenceId','" + evidence + "'),'verification',jsonb_build_object('mode','NONE'),"
                    + "'creation',jsonb_build_object('creatorAccountId','" + account + "','evidenceId','" + evidence + "','scopeDigest',repeat('a',64),'creationRequestDigest',repeat('a',64),'creationFingerprint',repeat('a',64))),repeat('a',64))");
        }
    }

    @Test
    void v014ReplaysWithoutChangingAclsMembershipsOrBusinessObjects() throws Exception {
        for (boolean credentialed : List.of(false, true)) {
            if (credentialed) { stop(); start(); provisionFrozenGraph(); }
            latest.migrate(); seedEvidence();
            String before = snapshot(); String credentials = identities();
            for (int replay = 0; replay < 2; replay++) {
                try (Connection connection = admin()) { execute(connection, Files.readString(MIGRATIONS.resolve(SCRIPT))); }
                assertThat(snapshot().equals(before)).isTrue();
                assertThat(identities().equals(credentials)).isTrue();
                assertOracle();
            }
            assertThat(latest.migrate().migrationsExecuted).isZero(); latest.validate();
            var current = Flyway.configure().dataSource(url, "fixture_admin", password).locations("classpath:db/migration").load();
            assertThat(current.migrate().migrationsExecuted).isEqualTo(1);
            current.validate();
            assertThat(current.info().applied()).hasSize(15);
            psql("apply"); psql("check"); psql("apply"); psql("check");
        }
    }

    @Test
    void v014RejectsUnsafeRolesMembershipsOwnershipAndSettingsAtomically() throws Exception {
        for (var rejection : List.of(
                Map.entry("ALTER ROLE customer_iam_role LOGIN", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE evidence_role SUPERUSER", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE customer_iam_role NOINHERIT", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE evidence_role PASSWORD 'synthetic-nonproduction'", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE customer_iam_role CONNECTION LIMIT 1", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE customer_iam_role CREATEDB", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE evidence_role CREATEROLE", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE customer_iam_role REPLICATION", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE evidence_role BYPASSRLS", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE evidence_role VALID UNTIL 'infinity'", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE evidence_role SET search_path TO evidence", "V014_SETTINGS_REJECTED"),
                Map.entry("ALTER DATABASE pa26 SET search_path TO evidence", "V014_SETTINGS_REJECTED"),
                Map.entry("ALTER TABLE customer_iam.customer_account OWNER TO evidence_owner", "V014_OWNERSHIP_REJECTED"),
                Map.entry("GRANT pg_read_all_data TO customer_iam_role", "V014_TOPOLOGY_REJECTED"),
                Map.entry("CREATE ROLE pa26_partial LOGIN NOINHERIT; GRANT customer_iam_role TO pa26_partial", "V014_TOPOLOGY_REJECTED"),
                Map.entry("CREATE ROLE pa26_partial LOGIN NOINHERIT; COMMENT ON ROLE pa26_partial IS 'onecall:pa25:v1:stale'", "V014_PROVENANCE_REJECTED")))
            rejectsAtomically(rejection.getKey(), rejection.getValue());
        provisionFrozenGraph();
        for (var rejection : List.of(
                Map.entry("REVOKE evidence_role FROM pa26_lw", "V014_TOPOLOGY_REJECTED"),
                Map.entry("DROP ROLE pa26_lq", "V014_PROVENANCE_REJECTED"),
                Map.entry("GRANT evidence_role TO pa26_lw WITH INHERIT TRUE", "V014_TOPOLOGY_REJECTED"),
                Map.entry("GRANT customer_iam_role TO pa26_lc WITH SET FALSE", "V014_TOPOLOGY_REJECTED"),
                Map.entry("GRANT sharing_role TO pa26_lq WITH ADMIN TRUE", "V014_TOPOLOGY_REJECTED"),
                Map.entry("COMMENT ON ROLE pa26_lc IS 'onecall:pa25:v1:wrong-slot'", "V014_PROVENANCE_REJECTED"),
                Map.entry("ALTER ROLE pa26_lc INHERIT", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE pa26_lw SET search_path TO evidence", "V014_SETTINGS_REJECTED"),
                Map.entry("ALTER ROLE pa26_lc RENAME TO pa26_renamed", "V014_PROVENANCE_REJECTED"),
                Map.entry("ALTER ROLE pa26_lw NOLOGIN", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE pa26_lc SUPERUSER", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE pa26_lw CREATEDB", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE pa26_lc CREATEROLE", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE pa26_lw REPLICATION", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE pa26_lc BYPASSRLS", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE pa26_lw VALID UNTIL 'infinity'", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("ALTER ROLE pa26_lc CONNECTION LIMIT 2", "V014_ATTRIBUTES_REJECTED"),
                Map.entry("GRANT customer_iam_role TO evidence_role", "V014_TOPOLOGY_REJECTED"),
                Map.entry("GRANT pg_write_all_data TO pa26_lw", "V014_TOPOLOGY_REJECTED"),
                Map.entry("GRANT SELECT(id) ON customer_iam.customer_account TO pa26_lc", "V014_ACL_REJECTED")))
            rejectsAtomically(rejection.getKey(), rejection.getValue());
        try (Connection connection = admin()) {
            String marker = one(connection, "SELECT shobj_description('pa26_lc'::regrole,'pg_authid')");
            for (String malformed : List.of(marker.replaceFirst("db=[0-9]+", "db=0"), marker.replaceFirst("role=[0-9]+", "role=0"),
                    marker.replace("slot=customer_context", "slot=sharing_command"), marker.replace("name=pa26_lc", "name=pa26_lw"), marker + " "))
                rejectsAtomically("COMMENT ON ROLE pa26_lc IS '" + malformed + "'", "V014_PROVENANCE_REJECTED");
        }
        try (Connection connection = login("pa26_lc")) { rejected(connection, "V014_AUTHORITY_REJECTED"); }
        try (Connection connection = admin()) {
            execute(connection, "SET ROLE customer_iam_owner"); rejected(connection, "V014_AUTHORITY_REJECTED");
        }
    }

    @Test
    void v014RejectsPublicDefaultAndUnexpectedAclDriftAtomically() throws Exception {
        for (var rejection : List.of(
                Map.entry("GRANT USAGE ON SCHEMA evidence TO PUBLIC", "V014_ACL_REJECTED"),
                Map.entry("GRANT SELECT ON customer_iam.auth_identity TO PUBLIC", "V014_ACL_REJECTED"),
                Map.entry("CREATE SEQUENCE public.pa26_unexpected; GRANT USAGE ON SEQUENCE public.pa26_unexpected TO evidence_role", "V014_ACL_REJECTED"),
                Map.entry("ALTER DEFAULT PRIVILEGES FOR ROLE customer_iam_owner GRANT SELECT ON TABLES TO PUBLIC", "V014_DEFAULT_ACL_REJECTED"),
                Map.entry("GRANT SELECT(provider_subject_hmac) ON customer_iam.auth_identity TO PUBLIC", "V014_ACL_REJECTED"),
                Map.entry("GRANT EXECUTE ON FUNCTION evidence.guard_recording() TO PUBLIC", "V014_ACL_REJECTED"),
                Map.entry("ALTER DEFAULT PRIVILEGES FOR ROLE evidence_owner GRANT SELECT ON TABLES TO evidence_role", "V014_DEFAULT_ACL_REJECTED"),
                Map.entry("ALTER DEFAULT PRIVILEGES FOR ROLE evidence_owner IN SCHEMA evidence GRANT EXECUTE ON FUNCTIONS TO PUBLIC", "V014_DEFAULT_ACL_REJECTED"),
                Map.entry("GRANT SELECT(provider_subject_hmac) ON customer_iam.auth_identity TO customer_iam_role", "V014_ACL_REJECTED"),
                Map.entry("REVOKE INSERT ON evidence.evidence_event FROM evidence_role", "V014_ACL_REJECTED"),
                Map.entry("GRANT SELECT ON customer_iam.customer_account TO customer_iam_role WITH GRANT OPTION", "V014_ACL_REJECTED"),
                Map.entry("REVOKE SELECT(id) ON organization_access.organization FROM customer_iam_role", "V014_ACL_REJECTED")))
            rejectsAtomically(rejection.getKey(), rejection.getValue());
        latest.migrate();
        for (var rejection : List.of(
                Map.entry("GRANT INSERT ON customer_iam.customer_account TO customer_iam_role", "V014_ACL_REJECTED"),
                Map.entry("GRANT UPDATE(status) ON evidence.evidence_record TO evidence_role", "V014_ACL_REJECTED"),
                Map.entry("GRANT EXECUTE ON FUNCTION evidence.guard_recording() TO evidence_role", "V014_ACL_REJECTED"),
                Map.entry("REVOKE SELECT(id) ON evidence.evidence_record FROM evidence_role", "V014_ACL_REJECTED"),
                Map.entry("GRANT SELECT(bucket) ON evidence.media_object TO evidence_role", "V014_ACL_REJECTED"),
                Map.entry("GRANT USAGE ON SCHEMA platform TO customer_iam_role", "V014_ACL_REJECTED")))
            rejectsAtomically(rejection.getKey(), rejection.getValue());
    }

    @Test
    void v014IsPrivilegeOnlyAndLeavesUnrelatedBoundariesEquivalent() throws Exception {
        seedEvidence();
        String before = structure(); String unrelated = unrelatedAcls(); String business = business(); String graph = identities();
        latest.migrate();
        assertThat(structure().equals(before)).isTrue();
        assertThat(unrelatedAcls().equals(unrelated)).isTrue();
        assertThat(business().equals(business)).isTrue();
        assertThat(identities().equals(graph)).isTrue();
        assertFrozen(); assertOracle();
        assertThat(Files.readString(MIGRATIONS.resolve(SCRIPT))).doesNotContain("CASCADE", "CREATE FUNCTION", "CREATE TABLE", "ALTER ROLE", "CREATE ROLE", "ALTER OWNER");
    }

    private void assertOracle() throws Exception {
        Set<String> expected = expected();
        try (Connection connection = admin()) {
            Set<String> actual = new LinkedHashSet<>(strings(connection, OBJECTS + """
                    SELECT grantee::regrole::text||'|'||kind||'|'||identity||'|'||column_name||'|'||privilege_type||'|'||grantor::regrole::text||'|'||is_grantable
                    FROM objects CROSS JOIN LATERAL aclexplode(acl) grants
                    WHERE grantee IN (0,'customer_iam_role'::regrole,'evidence_role'::regrole)
                        OR grantor IN ('customer_iam_role'::regrole,'evidence_role'::regrole)
                    """));
            assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
            assertThat(actual.stream().filter(tuple -> tuple.startsWith("customer_iam_role|")).count()).isEqualTo(12);
            assertThat(actual.stream().filter(tuple -> tuple.startsWith("evidence_role|")).count()).isEqualTo(16);
            Set<String> effective = new LinkedHashSet<>(strings(connection, OBJECTS + """
                    , roles AS (SELECT oid,rolname FROM pg_roles WHERE rolname IN ('customer_iam_role','evidence_role')),
                    privileges AS (
                        SELECT roles.rolname, objects.kind, identity, column_name, privilege
                        FROM objects CROSS JOIN roles
                        CROSS JOIN (VALUES ('SELECT'),('INSERT'),('UPDATE'),('DELETE'),('TRUNCATE'),('REFERENCES'),('TRIGGER'),
                            ('USAGE'),('CREATE'),('EXECUTE'),('SELECT WITH GRANT OPTION'),('UPDATE WITH GRANT OPTION'),
                            ('INSERT WITH GRANT OPTION'),('USAGE WITH GRANT OPTION'),('EXECUTE WITH GRANT OPTION')) rights(privilege)
                        WHERE CASE kind
                            WHEN 'schema' THEN CASE WHEN privilege IN ('USAGE','CREATE','USAGE WITH GRANT OPTION') THEN has_schema_privilege(roles.oid,objects.oid,privilege) ELSE false END
                            WHEN 'table' THEN CASE WHEN privilege IN ('SELECT','INSERT','UPDATE','DELETE','TRUNCATE','REFERENCES','TRIGGER','SELECT WITH GRANT OPTION','UPDATE WITH GRANT OPTION','INSERT WITH GRANT OPTION') THEN has_table_privilege(roles.oid,objects.oid,privilege) ELSE false END
                            WHEN 'column' THEN CASE WHEN privilege IN ('SELECT','INSERT','UPDATE','REFERENCES','SELECT WITH GRANT OPTION','UPDATE WITH GRANT OPTION','INSERT WITH GRANT OPTION') THEN has_column_privilege(roles.oid,objects.oid,subid::smallint,privilege) ELSE false END
                            WHEN 'sequence' THEN CASE WHEN privilege IN ('SELECT','UPDATE','USAGE') THEN has_sequence_privilege(roles.oid,objects.oid,privilege) ELSE false END
                            WHEN 'routine' THEN CASE WHEN privilege IN ('EXECUTE','EXECUTE WITH GRANT OPTION') THEN has_function_privilege(roles.oid,objects.oid,privilege) ELSE false END
                            ELSE false END
                    ) SELECT rolname||'|'||kind||'|'||identity||'|'||column_name||'|'||privilege FROM privileges
                    """));
            assertThat(effective).containsExactlyInAnyOrderElementsOf(expected.stream().map(tuple -> tuple.substring(0, tuple.lastIndexOf('|', tuple.lastIndexOf('|') - 1))).toList());
            for (String role : List.of("customer_iam_role", "evidence_role")) assertThat(one(connection, "SELECT has_database_privilege('" + role + "',current_database(),'CREATE,TEMPORARY')")).isEqualTo("f");
        }
    }

    private static Set<String> expected() {
        Set<String> tuples = new LinkedHashSet<>();
        for (var role : Map.of("customer_iam_role", CUSTOMER, "evidence_role", EVIDENCE).entrySet()) {
            Set<String> schemas = new LinkedHashSet<>();
            role.getValue().forEach((table, columns) -> {
                String schema = table.substring(0, table.indexOf('.')); schemas.add(schema);
                for (String column : columns) tuples.add(role.getKey() + "|column|" + table + "|" + column + "|SELECT|" + OWNERS.get(schema) + "|false");
            });
            for (String schema : schemas) tuples.add(role.getKey() + "|schema|" + schema + "||USAGE|" + OWNERS.get(schema) + "|false");
        }
        return tuples;
    }

    private void denyRemoved(String role, String login, Map<String, List<String>> allowed, List<String> tables) throws Exception {
        String before = business();
        try (Connection administrator = admin(); Connection connection = login(login)) {
            execute(connection, "SET ROLE " + role);
            assertThat(one(connection, "SHOW transaction_read_only")).isEqualTo("off");
            for (String table : tables) {
                var columns = strings(administrator, "SELECT attname FROM pg_attribute WHERE attrelid='" + table + "'::regclass AND attnum>0 AND NOT attisdropped ORDER BY attnum");
                for (String column : columns) {
                    if (!allowed.getOrDefault(table, List.of()).contains(column)) denied(connection, "SELECT " + column + " FROM " + table, "42501");
                    denied(connection, "UPDATE " + table + " SET " + column + "=" + column + " WHERE false", "42501");
                    denied(connection, "INSERT INTO " + table + " (" + column + ") SELECT " + column + " FROM " + table + " WHERE false", "42501");
                }
                for (String statement : List.of("SELECT * FROM " + table, "INSERT INTO " + table + " DEFAULT VALUES", "DELETE FROM " + table + " WHERE false", "TRUNCATE " + table,
                        "GRANT SELECT ON " + table + " TO pa26_lq", "CREATE TRIGGER pa26_forbidden BEFORE UPDATE ON " + table
                                + " FOR EACH ROW EXECUTE FUNCTION " + (role.equals("customer_iam_role") ? "customer_iam.guard_customer_account()" : "evidence.guard_recording()")))
                    denied(connection, statement, "42501");
                if (allowed.containsKey(table)) {
                    execute(connection, "SELECT " + String.join(",", allowed.get(table)) + " FROM " + table);
                    for (String lock : List.of("UPDATE", "SHARE")) denied(connection, "SELECT " + allowed.get(table).getFirst() + " FROM " + table + " FOR " + lock, "42501");
                }
            }
            for (String foreign : List.of("platform.outbox_event", "platform.idempotency_record", "audit.audit_fact", "sharing.evidence_share_grant", "subject_number.business_subject", "ivr.call_record", "ivr.inbound_session", "entitlement_metering.usage_reservation")) denied(connection, "SELECT * FROM " + foreign, "42501");
            denied(connection, "SELECT * FROM " + (role.equals("customer_iam_role") ? "evidence.evidence_record" : "customer_iam.customer_account"), "42501");
            var routines = strings(administrator, "SELECT format('%I.%I',namespace.nspname,routine.proname)||'('||coalesce((SELECT string_agg('NULL::'||argument::regtype::text,',') FROM unnest(routine.proargtypes::oid[]) argument),'')||')' FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid=routine.pronamespace WHERE namespace.nspname IN ('customer_iam','evidence','platform','audit') ORDER BY 1");
            for (String routine : routines) denied(connection, "SELECT " + routine, "42501");
            denied(connection, "CREATE TABLE " + (role.equals("customer_iam_role") ? "customer_iam" : "evidence") + ".pa26_forbidden(id integer)", "42501");
        }
        assertThat(business().equals(before)).isTrue(); assertOracle();
    }

    private void provisionFrozenGraph() throws Exception {
        try (Connection connection = admin()) {
            List<String> slots = List.of("sharing_query", "customer_context", "sharing_command");
            for (int index = 0; index < EXTERNAL.size(); index++) {
                String login = EXTERNAL.get(index);
                execute(connection, "CREATE ROLE " + login + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '" + password + "'");
                String marker = one(connection, "SELECT 'onecall:pa25:v1:db='||(SELECT oid FROM pg_database WHERE datname=current_database())||':role='||oid||':slot=" + slots.get(index) + ":name='||rolname FROM pg_roles WHERE rolname='" + login + "'");
                execute(connection, "COMMENT ON ROLE " + login + " IS '" + marker + "'");
            }
            execute(connection, "GRANT sharing_role TO pa26_lq WITH ADMIN FALSE, INHERIT FALSE, SET TRUE; GRANT customer_iam_role TO pa26_lc WITH ADMIN FALSE, INHERIT FALSE, SET TRUE; GRANT sharing_command_role,evidence_role TO pa26_lw WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
            for (String direct : DIRECT) execute(connection, "ALTER ROLE " + direct + " PASSWORD '" + password + "'");
        }
    }

    private void psql(String mode) throws Exception {
        var result = postgres.execInContainer("psql", "-X", "-w", "-U", "fixture_admin", "-d", "pa26", "-v", "mode=" + mode,
                "-v", "sharing_login=pa26_lq", "-v", "customer_login=pa26_lc", "-v", "command_login=pa26_lw", "-f", "/tmp/pa26-identities.sql");
        assertThat(result.getExitCode()).isZero();
        assertThat(result.getStdout().strip()).isEqualTo("PA25_" + mode.toUpperCase(java.util.Locale.ROOT) + "_OK logins=6 effective_roles=7 memberships=4");
        assertThat(result.getStderr()).isEmpty();
    }

    private void rejectsAtomically(String mutation, String expectedCategory) throws Exception {
        try (Connection connection = admin()) {
            connection.setAutoCommit(false); execute(connection, mutation);
            String before = catalog(connection);
            var savepoint = connection.setSavepoint();
            rejected(connection, expectedCategory);
            connection.rollback(savepoint);
            assertThat(catalog(connection).equals(before)).isTrue();
            connection.rollback();
        }
    }

    private void seedAccount() throws Exception {
        try (Connection connection = admin()) { execute(connection, "SET ROLE customer_iam_owner; INSERT INTO customer_iam.customer_account(id,display_name) VALUES ('" + account + "','synthetic'); RESET ROLE"); }
    }

    private void seedEvidence() throws Exception {
        seedAccount();
        UUID recording = UUID.randomUUID(); String event = UUID.randomUUID().toString();
        UUID session = UUID.randomUUID(); String call = "pa26-" + UUID.randomUUID();
        try (Connection connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, "SET LOCAL ROLE subject_number_owner; INSERT INTO subject_number.business_subject(id,source) VALUES ('" + subject + "','IVR'); SET LOCAL ROLE ivr_owner");
            execute(connection, "INSERT INTO ivr.inbound_session(id,call_id,subject_id,trusted_caller,access_number_masked,node_id,protocol_version,prompt_version) VALUES ('"
                    + session + "','" + call + "','" + subject + "',false,'synthetic','fixture','v1','v1')");
            execute(connection, "INSERT INTO ivr.call_record(id,call_id,session_id,subject_id,caller_ciphertext,caller_number_hmac,caller_masked,access_number_masked,node_id,protocol_version) VALUES ('"
                    + UUID.randomUUID() + "','" + call + "','" + session + "','" + subject + "','v1.fixture.payload',repeat('f',64),'synthetic','synthetic','fixture','v1'); SET LOCAL ROLE evidence_owner");
            execute(connection, """
                    INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,source_event_id,
                        identity_at_capture,consent_version,snapshot,snapshot_sha256,status,solidified_at)
                    VALUES ('%s','%s','CALL_RECORDING','%s','EVIDENCE','%s',
                        jsonb_build_object('subjectId','%s','capturedAt','2026-01-01T00:00:00Z','sourceChannel','IVR'),
                        'synthetic',jsonb_build_object('capture','{}'::jsonb,'content','{}'::jsonb,'sourceEventDomain','EVIDENCE',
                        'sourceEventId','%s','source',jsonb_build_object('subjectId','%s','recordingId','%s')),repeat('a',64),'SOLIDIFIED',now())
                    """.formatted(evidence, subject, recording, event, subject, event, subject, recording));
            execute(connection, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) VALUES ('" + UUID.randomUUID() + "','" + evidence + "','EVIDENCE','" + event + "','EVIDENCE_CREATED','{}'::jsonb,repeat('b',64))");
            for (String role : List.of("ORIGINAL", "DERIVED")) {
                UUID media = UUID.randomUUID();
                execute(connection, "INSERT INTO evidence.media_object(id,storage_provider,bucket,object_key,object_version_id,sha256,size_bytes,mime_type,crypto_key_ref) VALUES ('" + media + "','COS','synthetic','" + media + "','v1',repeat('" + (role.equals("ORIGINAL") ? "c" : "d") + "',64),1,'audio/wav','synthetic')");
                execute(connection, "INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) VALUES ('" + UUID.randomUUID() + "','" + evidence + "','" + media + "','" + role + "')");
                if (role.equals("ORIGINAL")) execute(connection, "INSERT INTO evidence.recording(id,call_id,recording_id,media_object_id,declared_format,declared_size,declared_duration,declared_sha256,validated_duration,validated_sha256,status,upload_expires_at,validated_at) VALUES ('"
                        + recording + "','" + call + "','synthetic-" + recording + "','" + media + "','audio/wav',1,60,repeat('c',64),60,repeat('c',64),'VALIDATED',now()+interval '1 hour',now())");
            }
            execute(connection, "SET CONSTRAINTS ALL IMMEDIATE"); connection.commit();
        }
    }

    private String identities() throws Exception { try (Connection connection = admin()) { return one(connection, "SELECT jsonb_build_array((SELECT jsonb_agg(to_jsonb(auth) ORDER BY oid) FROM pg_authid auth),(SELECT jsonb_agg(to_jsonb(edge) ORDER BY oid) FROM pg_auth_members edge),(SELECT jsonb_agg(to_jsonb(marker) ORDER BY objoid,classoid) FROM pg_shdescription marker))::text"); } }
    private String snapshot() throws Exception { return structure() + unrelatedAcls() + business() + identities() + acls(); }
    private String acls() throws Exception { try (Connection connection = admin()) { return catalog(connection); } }
    private String catalog(Connection connection) throws Exception { return one(connection, OBJECTS + "SELECT coalesce(jsonb_agg(jsonb_build_array(kind,oid,subid,acl::text) ORDER BY kind,oid,subid),'[]'::jsonb)::text FROM objects") + one(connection, "SELECT jsonb_build_array((SELECT jsonb_agg(to_jsonb(auth) ORDER BY oid) FROM pg_authid auth),(SELECT jsonb_agg(to_jsonb(edge) ORDER BY oid) FROM pg_auth_members edge),(SELECT jsonb_agg(to_jsonb(default_row) ORDER BY oid) FROM pg_default_acl default_row),(SELECT jsonb_agg(to_jsonb(setting_row) ORDER BY setdatabase,setrole) FROM pg_db_role_setting setting_row))::text"); }
    private String unrelatedAcls() throws Exception { try (Connection connection = admin()) { return one(connection, OBJECTS + "SELECT coalesce(jsonb_agg(jsonb_build_array(kind,oid,subid,grantee,grantor,privilege_type,is_grantable) ORDER BY kind,oid,subid,grantee,grantor,privilege_type),'[]'::jsonb)::text FROM objects CROSS JOIN LATERAL aclexplode(acl) grants WHERE grantee NOT IN ('customer_iam_role'::regrole,'evidence_role'::regrole) AND grantor NOT IN ('customer_iam_role'::regrole,'evidence_role'::regrole)"); } }
    private String structure() throws Exception {
        try (Connection connection = admin()) {
            return one(connection, """
                    SELECT jsonb_build_array(
                        (SELECT jsonb_agg(to_jsonb(namespace)-'nspacl' ORDER BY namespace.oid) FROM pg_namespace namespace WHERE nspname !~ '^pg_' AND nspname<>'information_schema'),
                        (SELECT jsonb_agg(to_jsonb(relation)-'relacl' ORDER BY relation.oid) FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace WHERE nspname !~ '^pg_' AND nspname<>'information_schema' AND relname<>'flyway_schema_history'),
                        (SELECT jsonb_agg(to_jsonb(attribute)-'attacl' ORDER BY attrelid,attnum) FROM pg_attribute attribute JOIN pg_class relation ON relation.oid=attribute.attrelid JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace WHERE nspname !~ '^pg_' AND nspname<>'information_schema' AND relname<>'flyway_schema_history'),
                        (SELECT jsonb_agg(to_jsonb(routine)-'proacl' ORDER BY routine.oid) FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid=routine.pronamespace WHERE nspname !~ '^pg_' AND nspname<>'information_schema'),
                        (SELECT jsonb_agg(to_jsonb(constraint_row) ORDER BY oid) FROM pg_constraint constraint_row),
                        (SELECT jsonb_agg(to_jsonb(trigger_row) ORDER BY oid) FROM pg_trigger trigger_row),
                        (SELECT jsonb_agg(to_jsonb(index_row) ORDER BY indexrelid) FROM pg_index index_row),
                        (SELECT jsonb_agg(to_jsonb(default_row) ORDER BY oid) FROM pg_attrdef default_row),
                        (SELECT jsonb_agg(to_jsonb(type_row) ORDER BY oid) FROM pg_type type_row))::text
                    """);
        }
    }
    private String business() throws Exception {
        try (Connection connection = admin()) {
            StringBuilder facts = new StringBuilder();
            for (String table : strings(connection, "SELECT namespace.nspname||'.'||relation.relname FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace WHERE relkind='r' AND nspname !~ '^pg_' AND nspname NOT IN ('information_schema','public') ORDER BY 1")) facts.append(table).append(one(connection, "SELECT coalesce(jsonb_agg(to_jsonb(row_value) ORDER BY to_jsonb(row_value)::text),'[]'::jsonb)::text FROM " + table + " row_value"));
            return facts.toString();
        }
    }
    private static void assertFrozen() throws Exception {
        try (var paths = Files.list(MIGRATIONS)) {
            var resources = paths.filter(path -> path.getFileName().toString().matches("V\\d{3}__.*\\.sql")).sorted().toList();
            assertThat(resources).hasSize(15);
            for (Path path : resources) {
                String version = path.getFileName().toString().substring(1, 4);
                if (FROZEN.containsKey(version)) assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readString(path, StandardCharsets.UTF_8)
                        .replace("\r\n", "\n").replace("\r", "\n").getBytes(StandardCharsets.UTF_8)))).isEqualTo(FROZEN.get(version));
            }
        }
    }
    private Connection admin() throws SQLException { return DriverManager.getConnection(url, "fixture_admin", password); }
    private Connection login(String name) throws SQLException { return DriverManager.getConnection(url, name, password); }
    private static void execute(Connection connection, String sql) throws SQLException { try (var statement = connection.createStatement()) { statement.execute(sql); } }
    private static String one(Connection connection, String sql) throws SQLException { try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) { assertThat(result.next()).isTrue(); return result.getString(1); } }
    private static List<String> strings(Connection connection, String sql) throws SQLException { List<String> values = new ArrayList<>(); try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) { while (result.next()) values.add(result.getString(1)); } return values; }
    private static void rejected(Connection connection, String expectedCategory) throws Exception {
        assertThat(denied(connection, Files.readString(MIGRATIONS.resolve(SCRIPT)), "P0001").getMessage())
                .startsWith("ERROR: " + expectedCategory + "\n");
    }
    private static SQLException denied(Connection connection, String sql, String state) throws SQLException {
        try { execute(connection, sql); throw new AssertionError("Expected SQLSTATE " + state); }
        catch (SQLException failure) { assertThat(failure.getSQLState()).isEqualTo(state); return failure; }
    }
}
