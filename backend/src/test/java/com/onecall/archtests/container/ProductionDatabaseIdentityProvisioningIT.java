package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.assertThat;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.CustomerContextService;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import com.onecall.core.sharing.internal.application.CustomerShareQueryService;
import com.onecall.core.sharing.internal.application.SharePageRequest;
import com.onecall.core.sharing.internal.application.TrustedShareCaller;
import com.onecall.core.sharing.internal.persistence.PublicSharePersistenceConfiguration.SharingPublicReadPersistenceProperties;
import com.onecall.core.sharing.internal.persistence.ShareAdmissionPersistenceConfiguration;
import com.onecall.core.sharing.internal.persistence.PublicShareQueryRepository;
import com.onecall.core.sharing.internal.persistence.SharingCommandRepository;
import com.onecall.core.sharing.internal.persistence.ShareQueryRepository;
import com.onecall.core.sharing.internal.application.ShareQueryException;
import com.onecall.core.sharing.internal.application.ShareSummary;
import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository;
import com.onecall.core.evidence.internal.mediaread.EvidenceMediaReadPersistenceProperties;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.JSONB;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(OutputCaptureExtension.class)
class ProductionDatabaseIdentityProvisioningIT {
    private static final Path SCRIPT = Path.of("ops/postgresql/pa25-runtime-identities.sql");
    private static final String CONTAINER_SCRIPT = "/tmp/pa25-runtime-identities.sql";
    private static final List<String> EXTERNAL = List.of("pa25_query", "pa25_customer", "pa25_command");
    private static final List<String> DIRECT = List.of("sharing_public_read_role", "sharing_admission_role", "evidence_media_reader_role");
    private static final List<String> EFFECTIVE = List.of("sharing_role", "customer_iam_role", "sharing_command_role", "evidence_role",
            "sharing_public_read_role", "sharing_admission_role", "evidence_media_reader_role");
    private static final List<String> LOGINS = java.util.stream.Stream.concat(EXTERNAL.stream(), DIRECT.stream()).toList();
    private static final List<String> MANAGED = java.util.stream.Stream.concat(EXTERNAL.stream(), EFFECTIVE.stream()).toList();
    private static final List<String> SLOTS = List.of("sharing_query", "customer_context", "sharing_command");
    private static final List<String> POOLS = List.of("sharingQuery", "customerQuery", "sharingCommand", "publicShareQuery", "sharingAdmission", "evidenceMediaRead");
    private static final Map<String, List<String>> TARGETS = Map.of(
            EXTERNAL.get(0), List.of("sharing_role"), EXTERNAL.get(1), List.of("customer_iam_role"),
            EXTERNAL.get(2), List.of("sharing_command_role", "evidence_role"));
    private static final List<String> SNAPSHOTS = List.of(
            "SELECT row_to_json(value)::text FROM pg_authid value ORDER BY oid",
            "SELECT (to_jsonb(value)-'oid')::text FROM pg_auth_members value ORDER BY roleid,member,grantor",
            "SELECT row_to_json(value)::text FROM pg_shdescription value ORDER BY objoid,classoid",
            "SELECT row_to_json(value)::text FROM pg_shdepend value ORDER BY dbid,classid,objid,objsubid,refclassid,refobjid,deptype",
            "SELECT row_to_json(value)::text FROM pg_namespace value ORDER BY oid",
            "SELECT row_to_json(value)::text FROM pg_class value WHERE relnamespace IN (SELECT oid FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname<>'information_schema') ORDER BY oid",
            "SELECT row_to_json(value)::text FROM pg_attribute value WHERE attrelid IN (SELECT oid FROM pg_class WHERE relnamespace IN (SELECT oid FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname<>'information_schema')) ORDER BY attrelid,attnum",
            "SELECT row_to_json(value)::text FROM pg_proc value ORDER BY oid",
            "SELECT row_to_json(value)::text FROM pg_default_acl value ORDER BY oid",
            "SELECT row_to_json(value)::text FROM pg_db_role_setting value ORDER BY setdatabase,setrole",
            "SELECT oid||'|'||datname||'|'||datdba||'|'||coalesce(datacl::text,'') FROM pg_database ORDER BY oid",
            "SELECT coalesce(to_regclass('public.flyway_schema_history')::text,'missing')");
    private final String adminPassword = UUID.randomUUID().toString();
    private final Map<String, String> passwords = new LinkedHashMap<>();
    private final List<IdentityFact> facts = new ArrayList<>();
    private final UUID account = UUID.randomUUID();
    private final UUID evidence = UUID.randomUUID();
    private final UUID grant = UUID.randomUUID();
    private GenericContainer<?> postgres;
    private Flyway flyway;
    private ConfigurableApplicationContext application;
    private CapturedOutput output;
    private String url;
    private Map<String, String> entryHashes;

    @BeforeEach
    void start(CapturedOutput captured) throws Exception {
        output = captured;
        entryHashes = migrationHashes();
        LOGINS.forEach(login -> passwords.put(login, UUID.randomUUID().toString()));
        postgres = new GenericContainer<>(DockerImageName.parse("postgres:16"))
                .withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", adminPassword)
                .withEnv("POSTGRES_DB", "pa25").withEnv("POSTGRES_INITDB_ARGS", "--lc-collate=C --lc-ctype=C")
                .withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
                .withStartupTimeout(Duration.ofMinutes(2));
        postgres.start();
        url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/pa25";
        flyway = Flyway.configure().dataSource(url, "fixture_admin", adminPassword).locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(16);
        try (Connection admin = admin()) {
            assertThat(one(admin, "SELECT current_setting('server_version_num')::integer BETWEEN 160000 AND 169999")).isEqualTo("t");
            assertThat(one(admin, "SELECT count(*) FROM pg_roles WHERE rolname !~ '^pg_' AND rolname NOT IN ('postgres','fixture_admin')")).isEqualTo("31");
            System.out.println("PA25 synthetic PostgreSQL " + one(admin, "SHOW server_version"));
        }
        postgres.copyFileToContainer(MountableFile.forHostPath(SCRIPT.toAbsolutePath()), CONTAINER_SCRIPT);
    }

    @AfterEach
    void stop() {
        if (application != null) application.close();
        if (postgres != null) postgres.stop();
    }

    @Test
    void createsExactIdentityGraphAndReplaysWithoutDrift() throws Exception {
        success("apply");
        assertMarkers();
        assertGraph();
        List<List<String>> initial = snapshot();
        success("check");
        success("apply");
        success("apply");
        success("check");
        assertThat(snapshot().equals(initial)).isTrue();
        setPasswords();
        List<List<String>> credentialed = snapshot();
        success("apply");
        success("check");
        assertThat(snapshot().equals(credentialed)).isTrue();
        assertMarkers();
    }

    @Test
    void bindsEnvironmentBackedDatasourcesToExactlySixIdentities() throws Exception {
        provision();
        startApplication();
        assertThat(application.getBeansOfType(HikariDataSource.class)).hasSize(6);
        for (int index = 0; index < LOGINS.size(); index++) {
            HikariDataSource pool = application.getBean(POOLS.get(index) + "DataSource", HikariDataSource.class);
            assertThat(pool.getUsername().equals(LOGINS.get(index))).isTrue();
            assertThat(pool.getJdbcUrl().equals(url)).isTrue();
            try (Connection connection = pool.getConnection()) { identity(connection, LOGINS.get(index), LOGINS.get(index)); }
        }
        for (String invalid : List.of("", EXTERNAL.get(2))) {
            safeInvalid(() -> new SharingPublicReadPersistenceProperties(url, invalid, passwords.get(DIRECT.get(0))));
            safeInvalid(() -> new ShareAdmissionPersistenceConfiguration.Properties(url, invalid, passwords.get(DIRECT.get(1))));
            safeInvalid(() -> new EvidenceMediaReadPersistenceProperties(true, url, invalid, passwords.get(DIRECT.get(2))));
        }
        application.close();
        application = new SpringApplicationBuilder(OneCallApplication.class).web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off").run();
        assertThat(application.getBeansOfType(HikariDataSource.class)).isEmpty();
    }

    @Test
    void sharingQueryAssumesOnlySharingRoleAndRestoresPoolIdentity() throws Exception {
        provision();
        seedAccount();
        startApplication();
        try (Connection connection = login(EXTERNAL.get(0))) { denied(connection, "SELECT id FROM customer_iam.customer_account", "42501"); }
        var service = application.getBean(CustomerShareQueryService.class);
        var result = service.listCustomerShares(new TrustedShareCaller(account, Optional.empty()),
                new SharePageRequest(Optional.empty(), Optional.empty()));
        assertThat(result.items()).isEmpty();
        assertFacts("sharingQuery", "sharing_role", true);
        restored("sharingQuery", EXTERNAL.get(0));
        transactionProbe("sharingQuery", "sharing_role", true, true);
        restored("sharingQuery", EXTERNAL.get(0));
    }

    @Test
    void customerQueryAssumesOnlyCustomerRoleAndEnforcesReadOnly() throws Exception {
        provision();
        seedAccount();
        startApplication();
        var caller = new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB);
        assertThat(application.getBean(CustomerContextService.class).getCustomerMe(caller).account().accountId()).isEqualTo(account);
        assertFacts("customerQuery", "customer_iam_role", true);
        try (Connection connection = login(EXTERNAL.get(1))) {
            execute(connection, "SET ROLE customer_iam_role");
            assertThat(one(connection, "SHOW transaction_read_only")).isEqualTo("off");
            denied(connection, "UPDATE customer_iam.customer_account SET display_name=display_name", "42501");
        }
        restored("customerQuery", EXTERNAL.get(1));
    }

    @Test
    void commandSwitchesToEvidenceAndRestoresOnTheSameConnection() throws Exception {
        provision();
        seedSharing();
        startApplication();
        template("sharingCommand").executeWithoutResult(status -> {
            DSLContext context = dsl("sharingCommand");
            SharingCommandRepository repository = application.getBean(SharingCommandRepository.class);
            repository.assertCommandRole(context);
            int pid = context.fetchOne("SELECT pg_backend_pid()").get(0, Integer.class);
            context.fetch("SELECT id FROM sharing.evidence_share_grant");
            repository.switchToEvidenceRole(context);
            assertThat(application.getBean("shareableEvidenceProjectionReader", ShareableEvidenceProjectionReader.class)
                    .readShareableProjection(context, evidence))
                    .isInstanceOf(ShareableEvidenceProjectionReader.ReadResult.Found.class);
            assertThat(context.fetchOne("SELECT pg_backend_pid()").get(0, Integer.class)).isEqualTo(pid);
            assertThat(context.fetchOne("SELECT session_user").get(0, String.class)).isEqualTo(EXTERNAL.get(2));
            repository.restoreAndAssertCommandRole(context);
            status.setRollbackOnly();
        });
        assertFacts("sharingCommand", "evidence_role", false);
        restored("sharingCommand", EXTERNAL.get(2));
        executeAdmin("REVOKE evidence_role FROM pa25_command");
        reject("check", null);
        template("sharingCommand").executeWithoutResult(status -> {
            DSLContext context = dsl("sharingCommand");
            context.execute("SET LOCAL ROLE sharing_command_role");
            jooqDenied(context, "SET LOCAL ROLE evidence_role", "42501");
            status.setRollbackOnly();
        });
        restored("sharingCommand", EXTERNAL.get(2));
        success("apply");
        assertGraph();
    }

    @Test
    void directLoginsRemainDirectAndCannotAssumeOtherIdentities() throws Exception {
        provision();
        startApplication();
        for (String direct : DIRECT) {
            try (Connection connection = login(direct)) {
                identity(connection, direct, direct);
                execute(connection, "RESET ROLE; SET ROLE NONE");
                identity(connection, direct, direct);
            }
        }
        for (String login : LOGINS) {
            String state = null;
            try (Connection ignored = DriverManager.getConnection(url, login, UUID.randomUUID().toString())) {
                throw new AssertionError("password authentication unexpectedly accepted");
            } catch (SQLException failure) { state = failure.getSQLState(); }
            assertThat(state).isEqualTo("28P01");
        }
        transactionProbe("publicShareQuery", null, true, false);
        transactionProbe("sharingAdmission", null, false, false);
        template("publicShareQuery").executeWithoutResult(status -> assertThat(application.getBean(PublicShareQueryRepository.class)
                .findByTokenDigest("a".repeat(64))).isEmpty());
        template("sharingAdmission").executeWithoutResult(status -> assertThat(application.getBean(ShareAdmissionRepository.class)
                .findGrantById(UUID.randomUUID())).isEmpty());
        assertThat(application.getBean(EvidenceMediaObjectReader.class).resolve(UUID.randomUUID()))
                .isInstanceOf(EvidenceMediaObjectReader.Result.Missing.class);
        assertFacts("evidenceMediaRead", DIRECT.get(2), true);
        assertGraph();
    }

    @Test
    void deniesEveryUnapprovedRoleTargetIncludingOwnersAndPredefinedRoles() throws Exception {
        provision();
        List<String> extra;
        try (Connection admin = admin()) {
            extra = strings(admin, "SELECT rolname FROM pg_roles WHERE rolname LIKE '%\\_owner' ESCAPE '\\' OR rolname LIKE 'pg\\_%' ESCAPE '\\' OR rolname='fixture_admin' ORDER BY 1");
        }
        int allowed = 0;
        int rejected = 0;
        for (String login : LOGINS) {
            try (Connection connection = login(login)) {
                for (String target : EFFECTIVE) {
                    boolean permitted = target.equals(login) || TARGETS.getOrDefault(login, List.of()).contains(target);
                    for (String prefix : List.of("SET ROLE ", "SET LOCAL ROLE ")) {
                        connection.setAutoCommit(false);
                        if (permitted) {
                            execute(connection, prefix + identifier(target));
                            identity(connection, target, login);
                        } else denied(connection, prefix + identifier(target), "42501");
                        connection.rollback();
                        identity(connection, login, login);
                        connection.commit();
                        connection.setAutoCommit(true);
                    }
                    if (permitted) allowed++; else rejected++;
                }
                for (String target : java.util.stream.Stream.concat(extra.stream(), LOGINS.stream()).distinct().toList()) {
                    if (!target.equals(login)) denied(connection, "SET ROLE " + identifier(target), "42501");
                }
            }
        }
        assertThat(allowed).isEqualTo(7);
        assertThat(rejected).isEqualTo(35);
    }

    @Test
    void readGraphsDenyWritesAndDistinguishAclFromReadOnlyFailures() throws Exception {
        provision();
        startApplication();
        for (String pool : List.of("sharingQuery", "customerQuery", "publicShareQuery", "evidenceMediaRead")) {
            String role = switch (pool) { case "sharingQuery" -> "sharing_role"; case "customerQuery" -> "customer_iam_role"; default -> null; };
            transactionProbe(pool, role, true, false);
        }
        try (Connection connection = login(EXTERNAL.get(1))) {
            execute(connection, "SET ROLE customer_iam_role");
            denied(connection, "DELETE FROM sharing.evidence_share_grant", "42501");
            assertThat(one(connection, "SHOW transaction_read_only")).isEqualTo("off");
            denied(connection, "UPDATE customer_iam.customer_account SET display_name=display_name", "42501");
        }
        try (Connection connection = login(EXTERNAL.get(2))) {
            execute(connection, "BEGIN READ ONLY; SET LOCAL ROLE sharing_command_role");
            denied(connection, "UPDATE sharing.evidence_share_grant SET status=status WHERE false", "25006");
            execute(connection, "ROLLBACK");
        }
        for (String login : List.of(EXTERNAL.get(0), EXTERNAL.get(1), DIRECT.get(0), DIRECT.get(2))) {
            try (Connection connection = login(login)) { denied(connection, "SET ROLE sharing_command_role", "42501"); }
        }
    }

    @Test
    void commandAndAdmissionWriteOnlyWithinTheirFrozenBoundaries() throws Exception {
        provision();
        seedSharing();
        startApplication();
        List<List<String>> before = snapshot();
        for (String pool : List.of("sharingCommand", "sharingAdmission")) {
            template(pool).executeWithoutResult(status -> {
                DSLContext context = dsl(pool);
                if (pool.equals("sharingCommand")) {
                    SharingCommandRepository repository = application.getBean(SharingCommandRepository.class);
                    repository.assertCommandRole(context);
                    var inserted = repository.insertGrantReturning(context, newGrant(UUID.randomUUID()));
                    assertThat(inserted.isPresent()).isTrue();
                    assertThat(repository.findCreationIdentity(context, inserted.orElseThrow().shareId()).isPresent()).isTrue();
                } else {
                    ShareAdmissionRepository repository = application.getBean(ShareAdmissionRepository.class);
                    var inserted = repository.insertAdmission(new ShareAdmissionRepository.AdmissionInsert(
                            UUID.randomUUID(), grant, "synthetic-pa25", 1, "NONE", "NOT_REQUIRED", java.time.Instant.now()));
                    assertThat(inserted.grantId().equals(grant)).isTrue();
                    assertThat(repository.updateGrantAdmissionCount(grant, 1, 1)).isEqualTo(1);
                }
                assertThat(context.fetchOne("SHOW transaction_read_only").get(0, String.class)).isEqualTo("off");
                jooqDenied(context, "CREATE TABLE sharing.pa25_forbidden(id integer)", "42501");
                status.setRollbackOnly();
            });
            assertThat(snapshot().equals(before)).isTrue();
        }
        for (String login : LOGINS) {
            try (Connection connection = login(login)) {
                if (TARGETS.containsKey(login)) execute(connection, "SET ROLE " + identifier(TARGETS.get(login).getFirst()));
                denied(connection, "DELETE FROM customer_iam.customer_account", "42501");
            }
        }
        try (Connection connection = login(EXTERNAL.get(2))) {
            execute(connection, "BEGIN; SET LOCAL ROLE evidence_role");
            denied(connection, "UPDATE evidence.evidence_record SET status=status WHERE false", "42501");
            execute(connection, "ROLLBACK");
        }
    }

    @Test
    void missingAndStaleMembershipsFailCheckAndOnlyAbsentEdgesAreReconciled() throws Exception {
        provision();
        for (var entry : TARGETS.entrySet()) {
            for (String target : entry.getValue()) {
                List<List<String>> before = snapshot();
                executeAdmin("REVOKE " + identifier(target) + " FROM " + identifier(entry.getKey()));
                reject("check", null);
                success("apply");
                assertThat(snapshot().equals(before)).isTrue();
                assertMarkers();
            }
        }
        rejectBothWithNames(List.of("pa25_replacement", EXTERNAL.get(1), EXTERNAL.get(2)), null);
        for (String fixed : List.of("sharing_command_role", DIRECT.get(0))) {
            contaminate("ALTER ROLE " + identifier(fixed) + " RENAME TO pa25_missing",
                    "ALTER ROLE pa25_missing RENAME TO " + identifier(fixed), null);
        }
    }

    @Test
    void extraReversedTransitiveAndCrossServiceMembershipsFailClosed() throws Exception {
        provision();
        for (String login : LOGINS) {
            String role = TARGETS.getOrDefault(login, List.of()).contains("sharing_role") ? "customer_iam_role" : "sharing_role";
            contaminate("GRANT " + role + " TO " + identifier(login) + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE",
                    "REVOKE " + role + " FROM " + identifier(login), null);
        }
        executeAdmin("REVOKE sharing_role FROM pa25_query");
        try {
            executeAdmin("GRANT pa25_query TO sharing_role WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
            try (Connection admin = admin()) {
                assertThat(one(admin, "SELECT count(*) FROM pg_auth_members WHERE roleid='pa25_query'::regrole AND member='sharing_role'::regrole AND NOT admin_option AND NOT inherit_option AND set_option")).isEqualTo("1");
                assertThat(one(admin, "SELECT count(*) FROM pg_auth_members WHERE roleid='sharing_role'::regrole AND member='pa25_query'::regrole")).isEqualTo("0");
            }
            rejectBoth("PA25_TOPOLOGY_REJECTED");
        } finally {
            executeAdmin("REVOKE pa25_query FROM sharing_role; GRANT sharing_role TO pa25_query WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
        }
        for (String sql : List.of("GRANT sharing_role TO customer_iam_role",
                "GRANT sharing_owner TO pa25_query", "GRANT pg_read_all_data TO pa25_query", "GRANT fixture_admin TO pa25_query")) {
            String undo = sql.replace("GRANT ", "REVOKE ").replace(" TO ", " FROM ");
            contaminate(sql, undo, null);
        }
        executeAdmin("CREATE ROLE pa25_intermediate NOLOGIN");
        try {
            contaminate("GRANT sharing_role TO pa25_intermediate; GRANT pa25_intermediate TO pa25_query",
                    "REVOKE sharing_role FROM pa25_intermediate; REVOKE pa25_intermediate FROM pa25_query", null);
            contaminate("GRANT pa25_query TO pa25_intermediate", "REVOKE pa25_query FROM pa25_intermediate", null);
        } finally { executeAdmin("DROP ROLE pa25_intermediate"); }
        executeAdmin("REVOKE sharing_role FROM pa25_query; DROP ROLE pa25_query");
        contaminate("GRANT sharing_role TO pa25_customer", "REVOKE sharing_role FROM pa25_customer", null);
    }

    @Test
    void adminInheritSetAndGrantorOptionDriftFailClosed() throws Exception {
        provision();
        for (String options : List.of("ADMIN TRUE, INHERIT FALSE, SET TRUE", "ADMIN FALSE, INHERIT TRUE, SET TRUE", "ADMIN FALSE, INHERIT FALSE, SET FALSE")) {
            contaminate("GRANT sharing_role TO pa25_query WITH " + options,
                    "GRANT sharing_role TO pa25_query WITH ADMIN FALSE, INHERIT FALSE, SET TRUE", null);
        }
        executeAdmin("CREATE ROLE pa25_other_admin SUPERUSER NOLOGIN; CREATE ROLE pa25_non_admin NOLOGIN");
        try {
            contaminate("INSERT INTO pg_auth_members(oid,roleid,member,grantor,admin_option,inherit_option,set_option) SELECT (SELECT (max(oid)::bigint+1)::oid FROM pg_auth_members),roleid,member,'pa25_other_admin'::regrole,false,false,true FROM pg_auth_members WHERE roleid='sharing_role'::regrole AND member='pa25_query'::regrole",
                    "DELETE FROM pg_auth_members WHERE grantor='pa25_other_admin'::regrole", null);
            contaminate("UPDATE pg_auth_members SET grantor='pa25_non_admin'::regrole WHERE roleid='sharing_role'::regrole AND member='pa25_query'::regrole",
                    "UPDATE pg_auth_members SET grantor='fixture_admin'::regrole WHERE roleid='sharing_role'::regrole AND member='pa25_query'::regrole", null);
        } finally { executeAdmin("DROP ROLE pa25_other_admin; DROP ROLE pa25_non_admin"); }
    }

    @Test
    void unsafeAttributesAndLoginShapeMismatchesFailClosed() throws Exception {
        provision();
        for (String role : MANAGED) {
            for (String flag : List.of("SUPERUSER", "CREATEDB", "CREATEROLE", "REPLICATION", "BYPASSRLS")) {
                contaminate("ALTER ROLE " + identifier(role) + " " + flag,
                        "ALTER ROLE " + identifier(role) + " NO" + flag, null);
            }
            boolean login = LOGINS.contains(role);
            boolean inherit = List.of("sharing_role", "customer_iam_role", "evidence_role").contains(role);
            contaminate("ALTER ROLE " + identifier(role) + (login ? " NOLOGIN" : " LOGIN"),
                    "ALTER ROLE " + identifier(role) + (login ? " LOGIN" : " NOLOGIN"), null);
            contaminate("ALTER ROLE " + identifier(role) + (inherit ? " NOINHERIT" : " INHERIT"),
                    "ALTER ROLE " + identifier(role) + (inherit ? " INHERIT" : " NOINHERIT"), null);
            contaminate("ALTER ROLE " + identifier(role) + " CONNECTION LIMIT 1", "ALTER ROLE " + identifier(role) + " CONNECTION LIMIT -1", null);
            String restoreValidity = role.equals(DIRECT.get(2))
                    ? "ALTER ROLE " + identifier(role) + " VALID UNTIL 'infinity'"
                    : "UPDATE pg_authid SET rolvaliduntil=NULL WHERE rolname=" + literal(role);
            contaminate("ALTER ROLE " + identifier(role) + " VALID UNTIL '2000-01-01 00:00:00+00'",
                    restoreValidity, null);
        }
        for (String role : EFFECTIVE.subList(0, 4)) {
            password(role, UUID.randomUUID().toString());
            try { rejectBoth(null); } finally { password(role, null); }
        }
    }

    @Test
    void ownershipAndUnexpectedSharedDependenciesFailClosed() throws Exception {
        provision();
        for (String role : LOGINS) {
            contaminate("CREATE SCHEMA pa25_owned AUTHORIZATION " + identifier(role), "DROP SCHEMA pa25_owned", null);
            contaminate("CREATE TABLE public.pa25_owned(id integer); ALTER TABLE public.pa25_owned OWNER TO " + identifier(role),
                    "DROP TABLE public.pa25_owned", null);
            contaminate("CREATE TYPE public.pa25_owned AS ENUM ('synthetic'); ALTER TYPE public.pa25_owned OWNER TO " + identifier(role),
                    "DROP TYPE public.pa25_owned", null);
            contaminate("CREATE FUNCTION public.pa25_owned() RETURNS integer LANGUAGE sql AS 'SELECT 1'; ALTER FUNCTION public.pa25_owned() OWNER TO " + identifier(role),
                    "DROP FUNCTION public.pa25_owned()", null);
        }
        contaminate("ALTER TABLE sharing.evidence_share_grant OWNER TO fixture_admin", "ALTER TABLE sharing.evidence_share_grant OWNER TO sharing_owner", null);
        contaminate("ALTER SCHEMA sharing OWNER TO fixture_admin", "ALTER SCHEMA sharing OWNER TO sharing_owner", null);
        contaminate("ALTER FUNCTION sharing.guard_share_grant() OWNER TO fixture_admin", "ALTER FUNCTION sharing.guard_share_grant() OWNER TO sharing_owner", null);
        contaminate("ALTER DATABASE pa25 OWNER TO pa25_query", "ALTER DATABASE pa25 OWNER TO fixture_admin", null);
        String largeObject;
        try (Connection admin = admin()) { largeObject = one(admin, "SELECT lo_create(0)"); }
        try { contaminate("ALTER LARGE OBJECT " + largeObject + " OWNER TO pa25_query", "ALTER LARGE OBJECT " + largeObject + " OWNER TO fixture_admin", null); }
        finally { executeAdmin("SELECT lo_unlink(" + largeObject + ")"); }
        var directory = postgres.execInContainer("sh", "-c", "mkdir /tmp/pa25_tablespace && chown postgres:postgres /tmp/pa25_tablespace");
        assertThat(directory.getExitCode()).isZero();
        contaminate("CREATE TABLESPACE pa25_tablespace OWNER pa25_query LOCATION '/tmp/pa25_tablespace'", "DROP TABLESPACE pa25_tablespace", null);
        executeAdmin("CREATE DATABASE pa25_other");
        try (Connection other = admin("pa25_other")) {
            execute(other, "CREATE SCHEMA pa25_owned AUTHORIZATION pa25_query");
            rejectBoth(null);
        } finally { executeAdmin("DROP DATABASE pa25_other"); }
    }

    @Test
    void directPrivilegesMissingGrantsAndGrantOptionsFailClosed() throws Exception {
        provision();
        for (String grant : List.of("USAGE ON SCHEMA sharing", "SELECT ON sharing.evidence_share_grant",
                "SELECT (id) ON sharing.evidence_share_grant", "EXECUTE ON FUNCTION sharing.guard_share_grant()")) {
            contaminate("GRANT " + grant + " TO pa25_query", "REVOKE " + grant + " FROM pa25_query", null);
        }
        executeAdmin("CREATE SEQUENCE public.pa25_sequence");
        try { contaminate("GRANT USAGE ON SEQUENCE public.pa25_sequence TO pa25_query", "REVOKE USAGE ON SEQUENCE public.pa25_sequence FROM pa25_query", null); }
        finally { executeAdmin("DROP SEQUENCE public.pa25_sequence"); }
        for (String role : EFFECTIVE) {
            contaminate("GRANT CREATE ON SCHEMA sharing TO " + identifier(role), "REVOKE CREATE ON SCHEMA sharing FROM " + identifier(role), null);
        }
        for (String role : EXTERNAL) {
            contaminate("GRANT CONNECT ON DATABASE pa25 TO " + identifier(role), "REVOKE CONNECT ON DATABASE pa25 FROM " + identifier(role), null);
        }
        for (String role : EFFECTIVE) {
            String schema = role.equals("customer_iam_role") ? "customer_iam" : role.startsWith("evidence") ? "evidence" : "sharing";
            String owner = schema + "_owner";
            contaminate("SET ROLE " + owner + "; REVOKE USAGE ON SCHEMA " + schema + " FROM " + identifier(role) + "; RESET ROLE",
                    "SET ROLE " + owner + "; GRANT USAGE ON SCHEMA " + schema + " TO " + identifier(role) + "; RESET ROLE", null);
        }
        contaminate("SET ROLE sharing_owner; REVOKE SELECT (share_id) ON sharing.evidence_share_grant FROM sharing_role; RESET ROLE",
                "SET ROLE sharing_owner; GRANT SELECT (share_id) ON sharing.evidence_share_grant TO sharing_role; RESET ROLE", null);
        contaminate("SET ROLE sharing_owner; REVOKE SELECT (share_id) ON sharing.evidence_share_grant FROM sharing_role; GRANT SELECT (id) ON sharing.evidence_share_grant TO sharing_role; RESET ROLE",
                "SET ROLE sharing_owner; REVOKE SELECT (id) ON sharing.evidence_share_grant FROM sharing_role; GRANT SELECT (share_id) ON sharing.evidence_share_grant TO sharing_role; RESET ROLE", null);
        contaminate("SET ROLE sharing_owner; GRANT SELECT (share_id) ON sharing.evidence_share_grant TO sharing_role WITH GRANT OPTION; RESET ROLE",
                "SET ROLE sharing_owner; REVOKE GRANT OPTION FOR SELECT (share_id) ON sharing.evidence_share_grant FROM sharing_role; RESET ROLE", null);
        for (String grant : List.of("INSERT ON customer_iam.customer_account", "UPDATE (display_name) ON customer_iam.customer_account",
                "EXECUTE ON FUNCTION customer_iam.guard_customer_account()", "SELECT (provider_subject_hmac) ON customer_iam.auth_identity")) {
            contaminate("GRANT " + grant + " TO customer_iam_role", "REVOKE " + grant + " FROM customer_iam_role", "PA25_ACL_REJECTED");
        }
        for (String grant : List.of("INSERT ON evidence.media_object", "UPDATE (status) ON evidence.evidence_record",
                "EXECUTE ON FUNCTION evidence.guard_recording()", "SELECT (bucket) ON evidence.media_object")) {
            contaminate("GRANT " + grant + " TO evidence_role", "REVOKE " + grant + " FROM evidence_role", "PA25_ACL_REJECTED");
        }
        contaminate("SET ROLE evidence_owner; REVOKE SELECT (id) ON evidence.evidence_record FROM evidence_role; RESET ROLE",
                "SET ROLE evidence_owner; GRANT SELECT (id) ON evidence.evidence_record TO evidence_role; RESET ROLE", "PA25_ACL_REJECTED");
        contaminate("SET ROLE customer_iam_owner; GRANT SELECT (id) ON customer_iam.customer_account TO customer_iam_role WITH GRANT OPTION; RESET ROLE",
                "SET ROLE customer_iam_owner; REVOKE GRANT OPTION FOR SELECT (id) ON customer_iam.customer_account FROM customer_iam_role; RESET ROLE", "PA25_ACL_REJECTED");
        String aclRewrite = "UPDATE pg_attribute SET attacl=ARRAY(SELECT makeaclitem(acl.grantee,CASE WHEN acl.grantee='sharing_role'::regrole THEN %s::regrole ELSE acl.grantor END,acl.privilege_type,acl.is_grantable) FROM aclexplode(attacl) acl) WHERE attrelid='sharing.evidence_share_grant'::regclass AND attname='share_id'";
        contaminate(aclRewrite.formatted("'fixture_admin'"), aclRewrite.formatted("'sharing_owner'"), null);
    }

    @Test
    void publicDatabaseAndDefaultAclDriftFailClosed() throws Exception {
        provision();
        for (String grant : List.of("USAGE ON SCHEMA sharing", "SELECT ON sharing.evidence_share_grant",
                "SELECT (id) ON sharing.evidence_share_grant", "EXECUTE ON FUNCTION sharing.guard_share_grant()")) {
            contaminate("GRANT " + grant + " TO PUBLIC", "REVOKE " + grant + " FROM PUBLIC", null);
        }
        for (String privilege : List.of("CREATE", "TEMPORARY")) {
            contaminate("GRANT " + privilege + " ON DATABASE pa25 TO PUBLIC", "REVOKE " + privilege + " ON DATABASE pa25 FROM PUBLIC", null);
        }
        contaminate("REVOKE CONNECT ON DATABASE pa25 FROM PUBLIC", "GRANT CONNECT ON DATABASE pa25 TO PUBLIC", null);
        for (String scope : List.of("", " IN SCHEMA sharing")) {
            for (String target : List.of("PUBLIC", "pa25_query", "sharing_admission_role")) {
                contaminate("ALTER DEFAULT PRIVILEGES FOR ROLE sharing_owner" + scope + " GRANT SELECT ON TABLES TO " + target,
                        "ALTER DEFAULT PRIVILEGES FOR ROLE sharing_owner" + scope + " REVOKE SELECT ON TABLES FROM " + target, null);
            }
        }
        contaminate("ALTER DEFAULT PRIVILEGES FOR ROLE pa25_query GRANT SELECT ON TABLES TO sharing_role",
                "ALTER DEFAULT PRIVILEGES FOR ROLE pa25_query REVOKE SELECT ON TABLES FROM sharing_role", null);
        executeAdmin("ALTER DEFAULT PRIVILEGES FOR ROLE pa25_query GRANT SELECT ON TABLES TO sharing_role; "
                + "ALTER DEFAULT PRIVILEGES FOR ROLE pa25_query REVOKE ALL ON TABLES FROM pa25_query, sharing_role");
        try (Connection admin = admin()) {
            assertThat(strings(admin, "SELECT pg_get_userbyid(defaclrole)||'|'||cardinality(defaclacl) "
                    + "FROM pg_default_acl WHERE defaclrole='pa25_query'::regrole"))
                    .isEqualTo(List.of("pa25_query|0"));
        }
        rejectBoth("PA25_DEFAULT_ACL_REJECTED");
    }

    @Test
    void roleAndDatabaseRoleSettingsFailClosed() throws Exception {
        provision();
        for (String role : MANAGED) {
            for (String setting : List.of("search_path TO sharing", "default_transaction_read_only TO on", "session_preload_libraries TO ''")) {
                String key = setting.substring(0, setting.indexOf(' '));
                contaminate("ALTER ROLE " + identifier(role) + " SET " + setting,
                        "ALTER ROLE " + identifier(role) + " RESET " + key, "PA25_SETTINGS_REJECTED");
            }
            contaminate("ALTER ROLE " + identifier(role) + " IN DATABASE pa25 SET search_path TO sharing",
                    "ALTER ROLE " + identifier(role) + " IN DATABASE pa25 RESET search_path", "PA25_SETTINGS_REJECTED");
        }
        for (String setting : List.of("role TO 'sharing_role'", "search_path TO sharing", "default_transaction_read_only TO on", "session_preload_libraries TO ''")) {
            String key = setting.substring(0, setting.indexOf(' '));
            contaminate("ALTER DATABASE pa25 SET " + setting, "ALTER DATABASE pa25 RESET " + key, "PA25_SETTINGS_REJECTED");
        }
        contaminate("ALTER ROLE pa25_query SET role TO 'sharing_role'", "ALTER ROLE pa25_query RESET role", "PA25_SETTINGS_REJECTED");
        executeAdmin("CREATE DATABASE pa25_other");
        try {
            contaminate("ALTER ROLE pa25_query IN DATABASE pa25_other SET search_path TO sharing",
                    "ALTER ROLE pa25_query IN DATABASE pa25_other RESET search_path", "PA25_SETTINGS_REJECTED");
        } finally { executeAdmin("DROP DATABASE pa25_other"); }
    }

    @Test
    void invalidInputsHistoryAndAuthorityRejectAtomically() throws Exception {
        for (String mode : List.of("", "APPLY", "unknown")) reject(mode, "PA25_INPUT_REJECTED");
        rejectInvocation(List.of(), "PA25_INPUT_REJECTED", "pa25", "fixture_admin");
        for (String name : List.of("", "pg_bad", "public", "sharing_role", "sharing_owner", "fixture_admin", "a".repeat(64), "bad'; SELECT 1; --")) {
            rejectBothWithNames(List.of(name, EXTERNAL.get(1), EXTERNAL.get(2)), null);
        }
        rejectBothWithNames(List.of(EXTERNAL.get(1), EXTERNAL.get(1), EXTERNAL.get(2)), null);
        for (int slot = 0; slot < 3; slot++) {
            String role = EXTERNAL.get(slot);
            executeAdmin("CREATE ROLE " + identifier(role) + " LOGIN NOINHERIT");
            try {
                rejectBoth("PA25_PROVENANCE_REJECTED");
                for (String target : TARGETS.get(role)) executeAdmin("GRANT " + identifier(target) + " TO " + identifier(role) + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
                rejectBoth("PA25_PROVENANCE_REJECTED");
                for (String target : TARGETS.get(role)) executeAdmin("REVOKE " + identifier(target) + " FROM " + identifier(role));
            } finally { executeAdmin("DROP ROLE " + identifier(role)); }
        }
        String finalHistory;
        try (Connection admin = admin()) { finalHistory = one(admin, "SELECT to_jsonb(value)::text FROM public.flyway_schema_history value WHERE installed_rank=16"); }
        contaminate("DELETE FROM public.flyway_schema_history WHERE installed_rank=16",
                "INSERT INTO public.flyway_schema_history SELECT * FROM jsonb_populate_record(NULL::public.flyway_schema_history," + literal(finalHistory) + "::jsonb)", "PA25_HISTORY_REJECTED");
        try (Connection admin = admin()) { assertThat(one(admin, "SELECT count(*) FROM pg_roles WHERE rolname IN ('pa25_query','pa25_customer','pa25_command')")).isEqualTo("0"); }
        for (String assignment : List.of("checksum=0", "description='wrong'", "script='wrong.sql'", "installed_rank=17", "type='BASELINE'", "success=false", "version='16'")) {
            contaminate("UPDATE public.flyway_schema_history SET " + assignment + " WHERE installed_rank=16",
                    "DELETE FROM public.flyway_schema_history WHERE version IN ('016','16'); INSERT INTO public.flyway_schema_history SELECT * FROM jsonb_populate_record(NULL::public.flyway_schema_history," + literal(finalHistory) + "::jsonb)", "PA25_HISTORY_REJECTED");
        }
        for (String extraVersion : List.of("016", "017")) {
            contaminate("INSERT INTO public.flyway_schema_history SELECT installed_rank+1000," + literal(extraVersion) + ",description,type,script,checksum,installed_by,installed_on,execution_time,success FROM public.flyway_schema_history WHERE installed_rank=16",
                    "DELETE FROM public.flyway_schema_history WHERE installed_rank=1016", "PA25_HISTORY_REJECTED");
        }
        String predecessor;
        try (Connection admin = admin()) { predecessor = one(admin, "SELECT to_jsonb(value)::text FROM public.flyway_schema_history value WHERE installed_rank=15"); }
        contaminate("UPDATE public.flyway_schema_history SET checksum=0 WHERE installed_rank=15",
                "DELETE FROM public.flyway_schema_history WHERE installed_rank=15; INSERT INTO public.flyway_schema_history SELECT * FROM jsonb_populate_record(NULL::public.flyway_schema_history," + literal(predecessor) + "::jsonb)", "PA25_HISTORY_REJECTED");
        provision();
        for (String role : EXTERNAL) {
            String marker;
            try (Connection admin = admin()) { marker = one(admin, "SELECT shobj_description(" + literal(role) + "::regrole,'pg_authid')"); }
            for (String malformed : Arrays.asList(null, "", "onecall:pa25:v2:", marker + " ", marker.replace("db=", "db=0"))) {
                contaminate("COMMENT ON ROLE " + identifier(role) + " IS " + literal(malformed),
                        "COMMENT ON ROLE " + identifier(role) + " IS " + literal(marker), "PA25_PROVENANCE_REJECTED");
            }
            String otherMarker;
            try (Connection admin = admin()) { otherMarker = one(admin, "SELECT shobj_description('" + (role.equals(EXTERNAL.get(0)) ? EXTERNAL.get(1) : EXTERNAL.get(0)) + "'::regrole,'pg_authid')"); }
            contaminate("COMMENT ON ROLE " + identifier(role) + " IS " + literal(otherMarker),
                    "COMMENT ON ROLE " + identifier(role) + " IS " + literal(marker), "PA25_PROVENANCE_REJECTED");
            executeAdmin("ALTER ROLE " + identifier(role) + " RENAME TO pa25_renamed");
            try {
                List<String> names = new ArrayList<>(EXTERNAL);
                names.set(EXTERNAL.indexOf(role), "pa25_renamed");
                rejectBothWithNames(names, "PA25_PROVENANCE_REJECTED");
            } finally { executeAdmin("ALTER ROLE pa25_renamed RENAME TO " + identifier(role)); }
        }
        contaminate("UPDATE public.flyway_schema_history SET success=false WHERE version='013'",
                "UPDATE public.flyway_schema_history SET success=true WHERE version='013'", "PA25_HISTORY_REJECTED");
        contaminate("UPDATE public.flyway_schema_history SET script='wrong' WHERE version='013'",
                "UPDATE public.flyway_schema_history SET script='V013__evidence_media_reader_role.sql' WHERE version='013'", "PA25_HISTORY_REJECTED");
        contaminate("UPDATE public.flyway_schema_history SET version='012' WHERE version='013'",
                "UPDATE public.flyway_schema_history SET version='013' WHERE installed_rank=13", "PA25_HISTORY_REJECTED");
        contaminate("ALTER TABLE public.flyway_schema_history RENAME TO pa25_history",
                "ALTER TABLE public.pa25_history RENAME TO flyway_schema_history", "PA25_HISTORY_REJECTED");
        executeAdmin("CREATE ROLE pa25_unprivileged LOGIN NOSUPERUSER NOINHERIT");
        try {
            rejectInvocation(arguments("check", EXTERNAL), "PA25_AUTHORITY_REJECTED", "pa25", "pa25_unprivileged");
        } finally { executeAdmin("DROP ROLE pa25_unprivileged"); }
        executeAdmin("CREATE ROLE pa25_delegate SUPERUSER NOLOGIN");
        try {
            List<String> invocation = new ArrayList<>(arguments("check", EXTERNAL));
            invocation.addAll(List.of("-c", "SET ROLE pa25_delegate"));
            rejectInvocation(invocation, "PA25_AUTHORITY_REJECTED", "pa25", "fixture_admin");
        } finally { executeAdmin("DROP ROLE pa25_delegate"); }
        String historyRow;
        try (Connection admin = admin()) { historyRow = one(admin, "SELECT to_jsonb(value)::text FROM public.flyway_schema_history value WHERE installed_rank=13"); }
        contaminate("DELETE FROM public.flyway_schema_history WHERE installed_rank=13",
                "INSERT INTO public.flyway_schema_history SELECT * FROM jsonb_populate_record(NULL::public.flyway_schema_history," + literal(historyRow) + "::jsonb)", "PA25_HISTORY_REJECTED");
        try (Connection otherAdmin = admin("postgres")) { execute(otherAdmin, "CREATE DATABASE pa25_copy TEMPLATE pa25"); }
        try { rejectInvocation(arguments("check", EXTERNAL), "PA25_PROVENANCE_REJECTED", "pa25_copy", "fixture_admin"); }
        finally { executeAdmin("DROP DATABASE pa25_copy"); }
        String marker;
        try (Connection admin = admin()) { marker = one(admin, "SELECT shobj_description('pa25_query'::regrole,'pg_authid')"); }
        executeAdmin("REVOKE sharing_role FROM pa25_query; DROP ROLE pa25_query; CREATE ROLE pa25_query LOGIN NOINHERIT; COMMENT ON ROLE pa25_query IS " + literal(marker));
        rejectBoth("PA25_PROVENANCE_REJECTED");
        assertThat(Files.readString(SCRIPT).contains("NOT BETWEEN 160000 AND 169999")).isTrue();
    }

    @Test
    void frozenMigrationsLogicalAclsAndHistoryRemainUnchanged() throws Exception {
        List<String> comments;
        List<String> aclBefore;
        try (Connection admin = admin()) {
            comments = strings(admin, "SELECT row_to_json(value)::text FROM pg_shdescription value ORDER BY objoid,classoid");
            aclBefore = catalogAcls(admin);
        }
        success("apply");
        assertThat(migrationHashes().equals(entryHashes)).isTrue();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        flyway.validate();
        assertThat(flyway.info().applied()).hasSize(16);
        assertExactFrozenAcls();
        assertMarkers();
        try (Connection admin = admin()) {
            assertThat(catalogAcls(admin).equals(aclBefore)).isTrue();
            assertThat(strings(admin, "SELECT row_to_json(value)::text FROM pg_shdescription value WHERE objoid NOT IN ('pa25_query'::regrole,'pa25_customer'::regrole,'pa25_command'::regrole) ORDER BY objoid,classoid").equals(comments)).isTrue();
            assertThat(one(admin, "SELECT count(*) FROM pg_roles WHERE rolname !~ '^pg_' AND rolname NOT IN ('postgres','fixture_admin')")).isEqualTo("34");
            assertThat(one(admin, "SELECT count(*) FROM pg_namespace WHERE nspname LIKE 'pa25%'")).isEqualTo("0");
        }
    }

    @Test
    void credentialsEndpointsAndSqlDiagnosticsAreRedacted() throws Exception {
        provision();
        startApplication();
        int offset = output.getAll().length();
        success("apply");
        success("check");
        String canary = "pa25_canary_'_SQL_DETAIL_BINDINGS_jdbc:postgresql://not-a-real-endpoint/fixture";
        rejectBothWithNames(List.of(canary, EXTERNAL.get(1), EXTERNAL.get(2)), "PA25_INPUT_REJECTED");
        contaminate("COMMENT ON ROLE pa25_query IS " + literal(canary),
                markerRestoreSql(EXTERNAL.get(0)), "PA25_PROVENANCE_REJECTED");
        try (Connection connection = login(EXTERNAL.get(0))) { denied(connection, "SET ROLE fixture_admin", "42501"); }
        transactionProbe("sharingQuery", "sharing_role", true, true);
        ShareQueryRepository failedRepository = new ShareQueryRepository() {
            @Override public void requireActiveCreatorContext(TrustedShareCaller caller) {
                throw new org.jooq.exception.DataAccessException(canary, new SQLException(canary, "42501"));
            }
            @Override public List<ShareSummary> fetch(TrustedShareCaller caller, SharePageRequest request, int fetchSize) { return List.of(); }
        };
        var failedService = new CustomerShareQueryService(template("sharingQuery"), dsl("sharingQuery"), failedRepository);
        boolean failed = false;
        try { failedService.listCustomerShares(new TrustedShareCaller(account, Optional.empty()), new SharePageRequest(Optional.empty(), Optional.empty())); }
        catch (ShareQueryException failure) { failed = !failure.getMessage().contains(canary); }
        assertThat(failed).isTrue();
        for (String diagnostic : List.of(new SharingPublicReadPersistenceProperties(url, DIRECT.get(0), canary).toString(),
                new ShareAdmissionPersistenceConfiguration.Properties(url, DIRECT.get(1), canary).toString(),
                new EvidenceMediaReadPersistenceProperties(true, url, DIRECT.get(2), canary).toString())) {
            assertThat(!diagnostic.contains(canary) && !diagnostic.contains(url)).isTrue();
        }
        String diagnostics = output.getAll().substring(offset);
        assertThat(!diagnostics.contains(canary) && !diagnostics.contains(url) && !diagnostics.contains(adminPassword)).isTrue();
        for (String password : passwords.values()) assertThat(!diagnostics.contains(password)).isTrue();
        assertThat(!diagnostics.contains("onecall:pa25:v1:") && !diagnostics.contains("SCRAM-SHA-256$")).isTrue();
    }

    private void provision() throws Exception { success("apply"); setPasswords(); }

    private void setPasswords() throws Exception {
        for (String login : LOGINS) password(login, passwords.get(login));
    }

    private void password(String role, String value) throws Exception {
        try (Connection admin = admin()) { execute(admin, "ALTER ROLE " + identifier(role) + " PASSWORD " + literal(value)); }
    }

    private Connection admin() throws SQLException { return admin("pa25"); }
    private Connection admin(String database) throws SQLException {
        var properties = new java.util.Properties();
        properties.setProperty("user", "fixture_admin");
        properties.setProperty("password", adminPassword);
        properties.setProperty("options", "-c default_transaction_read_only=off -c role=none -c session_preload_libraries=");
        return DriverManager.getConnection(url.substring(0, url.lastIndexOf('/') + 1) + database, properties);
    }
    private Connection login(String role) throws SQLException { return DriverManager.getConnection(url, role, passwords.get(role)); }

    private static String identifier(String value) { return '"' + value.replace("\"", "\"\"") + '"'; }
    private static String literal(String value) { return value == null ? "NULL" : "'" + value.replace("'", "''") + "'"; }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) { statement.execute(sql); }
        catch (SQLException failure) { throw new SQLException("PA25 fixture statement rejected", failure.getSQLState()); }
    }

    private void executeAdmin(String sql) throws SQLException { try (Connection admin = admin()) { execute(admin, sql); } }

    private static List<String> strings(Connection connection, String sql) throws SQLException {
        List<String> result = new ArrayList<>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            while (rows.next()) result.add(rows.getString(1));
        } catch (SQLException failure) { throw new SQLException("PA25 fixture query rejected", failure.getSQLState()); }
        return result;
    }

    private static String one(Connection connection, String sql) throws SQLException {
        List<String> values = strings(connection, sql);
        assertThat(values.size()).isEqualTo(1);
        return values.getFirst();
    }

    private static void identity(Connection connection, String current, String session) throws SQLException {
        assertThat(one(connection, "SELECT current_user||'|'||session_user").equals(current + "|" + session)).isTrue();
    }

    private static void denied(Connection connection, String sql, String expectedState) throws SQLException {
        String state = null;
        try { execute(connection, sql); } catch (SQLException failure) { state = failure.getSQLState(); }
        assertThat(state).isEqualTo(expectedState);
    }

    private static void jooqDenied(DSLContext context, String sql, String expectedState) {
        String state = null;
        try { context.execute(sql); }
        catch (org.jooq.exception.DataAccessException failure) { state = failure.sqlState(); }
        assertThat(state).isEqualTo(expectedState);
    }

    private List<List<String>> snapshot() throws SQLException {
        List<List<String>> result = new ArrayList<>();
        try (Connection admin = admin()) {
            for (String sql : SNAPSHOTS) result.add(strings(admin, sql));
            for (String table : strings(admin, "SELECT quote_ident(namespace.nspname)||'.'||quote_ident(relation.relname) FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace WHERE relation.relkind='r' AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema' ORDER BY 1")) {
                result.add(strings(admin, "SELECT row_to_json(value)::text FROM " + table + " value ORDER BY row_to_json(value)::text COLLATE \"C\""));
            }
        }
        return result;
    }

    private static List<String> arguments(String mode, List<String> names) {
        return List.of("-v", "mode=" + mode, "-v", "sharing_login=" + names.get(0),
                "-v", "customer_login=" + names.get(1), "-v", "command_login=" + names.get(2));
    }

    private org.testcontainers.containers.Container.ExecResult invocation(List<String> variables, String database, String user) throws Exception {
        List<String> command = new ArrayList<>();
        if (user.equals("fixture_admin")) {
            command.addAll(List.of("env", "PGOPTIONS=-c default_transaction_read_only=off -c role=none -c session_preload_libraries="));
        }
        command.addAll(List.of("psql", "-X", "-w", "-q", "-U", user, "-d", database));
        command.addAll(variables);
        command.addAll(List.of("-f", CONTAINER_SCRIPT));
        return postgres.execInContainer(command.toArray(String[]::new));
    }

    private void success(String mode) throws Exception {
        var result = invocation(arguments(mode, EXTERNAL), "pa25", "fixture_admin");
        assertThat(result.getExitCode()).as("PA25 script exit; raw diagnostics withheld").isZero();
        assertThat(result.getStderr().isBlank()).isTrue();
        assertThat(result.getStdout().strip()).isEqualTo("PA25_" + mode.toUpperCase(java.util.Locale.ROOT) + "_OK logins=6 effective_roles=7 memberships=4");
    }

    private void reject(String mode, String category) throws Exception { rejectInvocation(arguments(mode, EXTERNAL), category, "pa25", "fixture_admin"); }
    private void rejectBoth(String category) throws Exception { reject("apply", category); reject("check", category); }
    private void rejectBothWithNames(List<String> names, String category) throws Exception {
        for (String mode : List.of("apply", "check")) rejectInvocation(arguments(mode, names), category, "pa25", "fixture_admin");
    }

    private void rejectInvocation(List<String> variables, String category, String database, String user) throws Exception {
        List<List<String>> before = snapshot();
        var result = invocation(variables, database, user);
        assertThat(result.getExitCode()).isNotZero();
        assertThat(result.getStdout().isBlank()).isTrue();
        String diagnostics = result.getStderr().strip();
        assertThat(diagnostics.matches("psql:/tmp/pa25-runtime-identities\\.sql:[0-9]+: ERROR:  PA25_(INPUT|AUTHORITY|PROVENANCE|HISTORY|ATTRIBUTES|SETTINGS|TOPOLOGY|OWNERSHIP|ACL|DATABASE|DEFAULT_ACL|DEPENDENCY|APPLY)_REJECTED")).isTrue();
        if (category != null) assertThat(diagnostics.endsWith(category)).isTrue();
        for (String sensitive : java.util.stream.Stream.concat(passwords.values().stream(), LOGINS.stream()).toList()) {
            assertThat(!diagnostics.contains(sensitive)).isTrue();
        }
        assertThat(!diagnostics.contains(url) && !diagnostics.contains(adminPassword) && !diagnostics.contains("onecall:pa25:")).isTrue();
        assertThat(snapshot().equals(before)).as("rejection leaves the complete in-memory snapshot unchanged").isTrue();
    }

    private void contaminate(String sql, String undo, String category) throws Exception {
        executeAdmin(sql);
        try { rejectBoth(category); } finally { executeAdmin(undo); }
    }

    private void assertMarkers() throws Exception {
        try (Connection admin = admin()) {
            for (int index = 0; index < EXTERNAL.size(); index++) {
                String role = EXTERNAL.get(index);
                String expected = one(admin, "SELECT 'onecall:pa25:v1:db='||(SELECT oid FROM pg_database WHERE datname=current_database())||':role='||oid||':slot=" + SLOTS.get(index) + ":name='||rolname FROM pg_roles WHERE rolname=" + literal(role));
                String actual = one(admin, "SELECT shobj_description(" + literal(role) + "::regrole,'pg_authid')");
                assertThat(Arrays.equals(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8), actual.getBytes(java.nio.charset.StandardCharsets.UTF_8))).isTrue();
            }
        }
    }

    private String markerRestoreSql(String role) throws Exception {
        try (Connection admin = admin()) {
            return "COMMENT ON ROLE " + identifier(role) + " IS " + literal(one(admin, "SELECT shobj_description(" + literal(role) + "::regrole,'pg_authid')"));
        }
    }

    private void assertGraph() throws Exception {
        List<String> expected = List.of("customer_iam_role|pa25_customer|false|false|true|true",
                "evidence_role|pa25_command|false|false|true|true", "sharing_command_role|pa25_command|false|false|true|true",
                "sharing_role|pa25_query|false|false|true|true");
        try (Connection admin = admin()) {
            assertThat(strings(admin, "SELECT target.rolname||'|'||member_role.rolname||'|'||membership.admin_option||'|'||membership.inherit_option||'|'||membership.set_option||'|'||grantor.rolsuper FROM pg_auth_members membership JOIN pg_roles target ON target.oid=membership.roleid JOIN pg_roles member_role ON member_role.oid=membership.member JOIN pg_roles grantor ON grantor.oid=membership.grantor WHERE target.rolname NOT LIKE 'pg_%' OR member_role.rolname NOT LIKE 'pg_%' ORDER BY 1")).containsExactlyElementsOf(expected);
        }
    }

    private void seedAccount() throws SQLException {
        executeAdmin("SET ROLE customer_iam_owner; INSERT INTO customer_iam.customer_account(id,display_name) VALUES ('" + account + "','synthetic'); RESET ROLE");
    }

    private SharingCommandRepository.ShareGrant newGrant(UUID id) {
        String digest = "a".repeat(64);
        String snapshot = """
                {"content":{"evidenceId":"%s"},"verification":{"mode":"NONE"},
                 "creation":{"creatorAccountId":"%s","evidenceId":"%s",
                 "scopeDigest":"%s","creationRequestDigest":"%s","creationFingerprint":"%s"}}
                """.formatted(evidence, account, evidence, digest, digest, digest);
        return new SharingCommandRepository.ShareGrant(id, "synthetic-" + id,
                UUID.randomUUID().toString().replace("-", "").repeat(2), evidence, account, null,
                ShareSummary.VerificationMode.NONE, null, null, 0, java.time.Instant.now().minusSeconds(60),
                java.time.Instant.now().plusSeconds(3600), null, 0, false, JSONB.valueOf(snapshot), digest,
                ShareSummary.ShareStatus.ACTIVE, 0, 1);
    }

    private void seedSharing() throws SQLException {
        seedAccount();
        UUID subject = UUID.randomUUID();
        UUID recording = UUID.randomUUID();
        String event = UUID.randomUUID().toString();
        try (Connection admin = admin()) {
            admin.setAutoCommit(false);
            execute(admin, "SET LOCAL ROLE subject_number_owner");
            bound(admin, "INSERT INTO subject_number.business_subject(id,source) VALUES (?,'IVR')", subject);
            execute(admin, "SET LOCAL ROLE evidence_owner");
            bound(admin, """
                    INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,
                        source_event_id,identity_at_capture,consent_version,snapshot,snapshot_sha256,status,solidified_at)
                    VALUES (?,?,'CALL_RECORDING',?,'EVIDENCE',?,
                        jsonb_build_object('subjectId',?::text,'capturedAt','2026-01-01T00:00:00Z','sourceChannel','IVR'),
                        'synthetic',jsonb_build_object('capture','{}'::jsonb,'content','{}'::jsonb,
                            'sourceEventDomain','EVIDENCE','sourceEventId',?::text,
                            'source',jsonb_build_object('subjectId',?::text,'recordingId',?::text)),repeat('a',64),'SOLIDIFIED',now())
                    """, evidence, subject, recording, event, subject, event, subject, recording);
            bound(admin, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) VALUES (?,?,'EVIDENCE',?,'EVIDENCE_CREATED','{}'::jsonb,repeat('b',64))",
                    UUID.randomUUID(), evidence, event);
            UUID media = UUID.randomUUID();
            bound(admin, "INSERT INTO evidence.media_object(id,storage_provider,bucket,object_key,object_version_id,sha256,size_bytes,mime_type,crypto_key_ref) VALUES (?,'COS','synthetic',?,'v1',repeat('c',64),1,'audio/wav','synthetic')", media, media.toString());
            bound(admin, "INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) VALUES (?,?,?,'ORIGINAL')", UUID.randomUUID(), evidence, media);
            execute(admin, "SET CONSTRAINTS ALL IMMEDIATE");
            execute(admin, "SET LOCAL ROLE sharing_owner");
            var parent = newGrant(grant);
            bound(admin, """
                    INSERT INTO sharing.evidence_share_grant(id,share_id,capability_token_hash,evidence_id,created_by_account_id,
                        verification_mode,valid_from,expires_at,content_snapshot,content_sha256)
                    VALUES (?,?,?,?,?,'NONE',?,?,?::jsonb,?)
                    """, parent.id(), parent.shareId(), parent.capabilityTokenHash(), evidence, account,
                    parent.validFrom().atOffset(java.time.ZoneOffset.UTC), parent.expiresAt().atOffset(java.time.ZoneOffset.UTC),
                    parent.contentSnapshot().data(), parent.contentSha256());
            admin.commit();
        }
    }

    private static void bound(Connection connection, String sql, Object... parameters) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) statement.setObject(index + 1, parameters[index]);
            statement.execute();
        } catch (SQLException failure) { throw new SQLException("PA25 fixture binding rejected", failure.getSQLState()); }
    }

    private void startApplication() {
        Map<String, Object> environment = new LinkedHashMap<>();
        List<String> prefixes = List.of("ONECALL_DB", "ONECALL_CUSTOMER_DB", "ONECALL_SHARING_COMMAND_DB",
                "ONECALL_SHARING_PUBLIC_READ_DATASOURCE", "ONECALL_SHARING_ADMISSION_DATASOURCE", "ONECALL_EVIDENCE_MEDIA_READ_DATASOURCE");
        for (int index = 0; index < LOGINS.size(); index++) {
            String prefix = prefixes.get(index);
            environment.put(prefix + "_URL", url);
            environment.put(prefix + "_USERNAME", LOGINS.get(index));
            environment.put(prefix + "_PASSWORD", passwords.get(LOGINS.get(index)));
        }
        environment.put("ONECALL_SHARING_COMMAND_ENABLED", "true");
        environment.put("ONECALL_SHARING_ADMISSION_DATASOURCE_ENABLED", "true");
        environment.put("ONECALL_EVIDENCE_MEDIA_READ_DATASOURCE_ENABLED", "true");
        application = new SpringApplicationBuilder(OneCallApplication.class).web(WebApplicationType.NONE)
                .profiles("persistence", "web").properties("spring.main.banner-mode=off")
                .initializers(context -> context.getEnvironment().getPropertySources()
                    .addFirst(new SystemEnvironmentPropertySource("pa25-synthetic-environment", environment)))
                .run("--server.port=0");
        for (String pool : POOLS) {
            HikariDataSource source = application.getBean(pool + "DataSource", HikariDataSource.class);
            source.setMaximumPoolSize(1);
            source.setMinimumIdle(0);
            dsl(pool).configuration().set(new DefaultExecuteListenerProvider(new ExecuteListener() {
                @Override public void executeStart(ExecuteContext context) {
                    try {
                        Connection connection = context.connection();
                        facts.add(new IdentityFact(pool, one(connection, "SELECT current_user"), one(connection, "SELECT session_user"),
                                one(connection, "SHOW transaction_read_only"), one(connection, "SHOW transaction_isolation"),
                                one(connection, "SELECT pg_backend_pid()")));
                    } catch (SQLException failure) { throw new IllegalStateException("PA25 identity capture rejected"); }
                }
            }));
        }
    }

    private DSLContext dsl(String pool) { return application.getBean(pool + "Dsl", DSLContext.class); }
    private TransactionTemplate template(String pool) { return application.getBean(pool + "TransactionTemplate", TransactionTemplate.class); }

    private void transactionProbe(String pool, String role, boolean readOnly, boolean rollback) {
        template(pool).executeWithoutResult(status -> {
            DSLContext context = dsl(pool);
            if (role != null) context.execute("SET LOCAL ROLE " + identifier(role));
            context.fetch("SELECT 1");
            assertThat(context.fetchOne("SHOW transaction_read_only").get(0, String.class)).isEqualTo(readOnly ? "on" : "off");
            assertThat(context.fetchOne("SHOW transaction_isolation").get(0, String.class)).isEqualTo("read committed");
            if (rollback) status.setRollbackOnly();
        });
    }

    private void restored(String pool, String login) throws SQLException {
        HikariDataSource source = application.getBean(pool + "DataSource", HikariDataSource.class);
        String pid;
        try (Connection connection = source.getConnection()) { identity(connection, login, login); pid = one(connection, "SELECT pg_backend_pid()"); }
        try (Connection connection = source.getConnection()) {
            identity(connection, login, login);
            assertThat(one(connection, "SELECT pg_backend_pid()").equals(pid)).isTrue();
        }
    }

    private void assertFacts(String pool, String role, boolean readOnly) {
        List<IdentityFact> selected = facts.stream().filter(fact -> fact.pool().equals(pool) && fact.current().equals(role)).toList();
        assertThat(!selected.isEmpty()).isTrue();
        for (IdentityFact fact : selected) {
            assertThat(fact.session().equals(LOGINS.get(POOLS.indexOf(pool)))).isTrue();
            assertThat(fact.readOnly()).isEqualTo(readOnly ? "on" : "off");
            assertThat(fact.isolation()).isEqualTo("read committed");
        }
    }

    private record IdentityFact(String pool, String current, String session, String readOnly, String isolation, String pid) {}

    private static void safeInvalid(Runnable constructor) {
        boolean rejected = false;
        try { constructor.run(); } catch (IllegalArgumentException expected) { rejected = true; }
        assertThat(rejected).isTrue();
    }

    private static Map<String, String> migrationHashes() throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        try (var paths = Files.list(Path.of("src/main/resources/db/migration"))) {
            for (Path path : paths.filter(path -> path.getFileName().toString().matches("V0(?:0[1-9]|1[0-3])__.*\\.sql")).sorted().toList()) {
                result.put(path.getFileName().toString(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
            }
        }
        assertThat(result.size()).isEqualTo(13);
        return result;
    }

    private static List<String> catalogAcls(Connection admin) throws SQLException {
        return strings(admin, "SELECT value FROM (SELECT 'n|'||oid||'|'||coalesce(nspacl::text,'') value FROM pg_namespace UNION ALL SELECT 'c|'||oid||'|'||coalesce(relacl::text,'') FROM pg_class WHERE relnamespace IN (SELECT oid FROM pg_namespace WHERE nspname !~ '^pg_') UNION ALL SELECT 'a|'||attrelid||'|'||attnum||'|'||coalesce(attacl::text,'') FROM pg_attribute WHERE attrelid IN (SELECT oid FROM pg_class WHERE relnamespace IN (SELECT oid FROM pg_namespace WHERE nspname !~ '^pg_')) UNION ALL SELECT 'p|'||oid||'|'||coalesce(proacl::text,'') FROM pg_proc UNION ALL SELECT 'd|'||oid||'|'||defaclacl::text FROM pg_default_acl) facts ORDER BY value COLLATE \"C\"");
    }

    private void assertExactFrozenAcls() throws Exception {
        Set<String> expected = Set.of(
                "customer_iam_role|schema|subject_number||USAGE|subject_number_owner|false",
                "customer_iam_role|column|subject_number.business_subject|id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.business_subject|owner_account_id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.business_subject|owner_organization_id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.business_subject|real_name_status|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.business_subject|created_at|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.caller_number_binding|id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.caller_number_binding|subject_id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.caller_number_binding|number_masked|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.caller_number_binding|number_type|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.caller_number_binding|status|SELECT|subject_number_owner|false",
                "customer_iam_role|schema|activation_consent||USAGE|activation_consent_owner|false",
                "customer_iam_role|column|activation_consent.service_activation|subject_id|SELECT|activation_consent_owner|false",
                "customer_iam_role|column|activation_consent.service_activation|status|SELECT|activation_consent_owner|false",

                "customer_iam_role|column|customer_iam.customer_account|display_name|SELECT|customer_iam_owner|false",
                "customer_iam_role|column|customer_iam.customer_account|id|SELECT|customer_iam_owner|false",
                "customer_iam_role|column|customer_iam.customer_account|status|SELECT|customer_iam_owner|false",
                "customer_iam_role|column|organization_access.organization|display_name|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization|id|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization|status|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization_membership|account_id|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization_membership|organization_id|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization_membership|role|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization_membership|status|SELECT|organization_access_owner|false",
                "customer_iam_role|schema|customer_iam||USAGE|customer_iam_owner|false",
                "customer_iam_role|schema|organization_access||USAGE|organization_access_owner|false",
                "evidence_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.media_object|id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.media_object|mime_type|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.media_object|sha256|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.recording|media_object_id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.recording|validated_duration|SELECT|evidence_owner|false",
                "evidence_role|schema|evidence||USAGE|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.media_object|id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.media_object|mime_type|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.media_object|sha256|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.recording|id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.recording|media_object_id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.recording|status|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.recording|validated_duration|SELECT|evidence_owner|false",
                "evidence_media_reader_role|schema|evidence||USAGE|evidence_owner|false",
                "sharing_admission_role|column|customer_iam.customer_account|id|SELECT|customer_iam_owner|false",
                "sharing_admission_role|column|customer_iam.customer_account|status|SELECT|customer_iam_owner|false",
                "sharing_admission_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
                "sharing_admission_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false",
                "sharing_admission_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
                "sharing_admission_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "sharing_admission_role|column|organization_access.organization|id|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization|status|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|account_id|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|id|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|organization_id|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|revision|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|role|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|status|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|admissions_used|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|admissions_used|UPDATE|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|capability_token_hash|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|code_generation|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|code_hash|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|code_salt|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|content_sha256|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|content_snapshot|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|created_by_account_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|download_allowed|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|evidence_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|expires_at|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|max_admissions|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|owner_organization_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|revision|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|revision|UPDATE|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|revocation_generation|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|revocation_generation|UPDATE|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|share_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|status|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|status|UPDATE|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|valid_from|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|verification_mode|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_domain|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_domain|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_type|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_type|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|occurred_at|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|occurred_at|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|payload_redacted|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|payload_redacted|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|payload_sha256|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|payload_sha256|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|result|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|result|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|risk_digest|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|risk_digest|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|share_admission_id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|share_admission_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|share_grant_id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|share_grant_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admission_key|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admission_key|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admission_no|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admission_no|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admitted_at|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admitted_at|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|share_grant_id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|share_grant_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|verification_mode|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|verification_mode|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|verification_result|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|verification_result|SELECT|sharing_owner|false",
                "sharing_admission_role|column|subject_number.business_subject|id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.business_subject|owner_account_id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.business_subject|owner_organization_id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.business_subject|revision|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|number_hmac|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|number_type|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|revision|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|status|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|subject_id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|verified_at|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|cleared_at|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|observed_at|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|revision|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|risk_type|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false",
                "sharing_admission_role|schema|customer_iam||USAGE|customer_iam_owner|false",
                "sharing_admission_role|schema|evidence||USAGE|evidence_owner|false",
                "sharing_admission_role|schema|organization_access||USAGE|organization_access_owner|false",
                "sharing_admission_role|schema|sharing||USAGE|sharing_owner|false",
                "sharing_admission_role|schema|subject_number||USAGE|subject_number_owner|false",
                "sharing_command_role|column|customer_iam.customer_account|id|SELECT|customer_iam_owner|false",
                "sharing_command_role|column|customer_iam.customer_account|status|SELECT|customer_iam_owner|false",
                "sharing_command_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|call_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|created_at|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|reservation_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|session_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|organization_access.organization|id|SELECT|organization_access_owner|false",
                "sharing_command_role|column|organization_access.organization|status|SELECT|organization_access_owner|false",
                "sharing_command_role|column|organization_access.organization_membership|account_id|SELECT|organization_access_owner|false",
                "sharing_command_role|column|organization_access.organization_membership|organization_id|SELECT|organization_access_owner|false",
                "sharing_command_role|column|organization_access.organization_membership|role|SELECT|organization_access_owner|false",
                "sharing_command_role|column|organization_access.organization_membership|status|SELECT|organization_access_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|admissions_used|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|code_generation|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|code_hash|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|code_salt|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|expires_at|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|revision|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|revocation_generation|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|status|UPDATE|sharing_owner|false",
                "sharing_command_role|column|subject_number.business_subject|id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.business_subject|owner_account_id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.business_subject|owner_organization_id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.business_subject|revision|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.caller_number_binding|id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.caller_number_binding|revision|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.caller_number_binding|status|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.caller_number_binding|subject_id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.caller_number_binding|verified_at|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.subject_risk_state|revision|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.subject_risk_state|risk_type|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false",
                "sharing_command_role|routine|audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)||EXECUTE|audit_owner|false",
                "sharing_command_role|routine|platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)||EXECUTE|platform_reliability_owner|false",
                "sharing_command_role|schema|audit||USAGE|audit_owner|false",
                "sharing_command_role|schema|customer_iam||USAGE|customer_iam_owner|false",
                "sharing_command_role|schema|evidence||USAGE|evidence_owner|false",
                "sharing_command_role|schema|organization_access||USAGE|organization_access_owner|false",
                "sharing_command_role|schema|platform||USAGE|platform_reliability_owner|false",
                "sharing_command_role|schema|sharing||USAGE|sharing_owner|false",
                "sharing_command_role|schema|subject_number||USAGE|subject_number_owner|false",
                "sharing_command_role|table|sharing.evidence_share_grant||INSERT|sharing_owner|false",
                "sharing_command_role|table|sharing.evidence_share_grant||SELECT|sharing_owner|false",
                "sharing_command_role|table|sharing.share_access_event||INSERT|sharing_owner|false",
                "sharing_command_role|table|sharing.share_access_event||SELECT|sharing_owner|false",
                "sharing_command_role|table|sharing.share_admission||INSERT|sharing_owner|false",
                "sharing_command_role|table|sharing.share_admission||SELECT|sharing_owner|false",
                "sharing_public_read_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.media_object|id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.media_object|mime_type|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.media_object|sha256|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.recording|media_object_id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.recording|validated_duration|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|capability_token_hash|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|content_snapshot|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|download_allowed|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|evidence_id|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|expires_at|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|owner_organization_id|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|status|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|valid_from|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|verification_mode|SELECT|sharing_owner|false",
                "sharing_public_read_role|schema|evidence||USAGE|evidence_owner|false",
                "sharing_public_read_role|schema|sharing||USAGE|sharing_owner|false",
                "customer_iam_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false",
                "customer_iam_role|schema|evidence||USAGE|evidence_owner|false",
                "customer_iam_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "customer_iam_role|schema|catalog_order||USAGE|catalog_order_owner|false",
                "customer_iam_role|column|catalog_order.customer_order|subject_id|SELECT|catalog_order_owner|false",
                "customer_iam_role|column|catalog_order.customer_order|account_id|SELECT|catalog_order_owner|false",
                "customer_iam_role|column|catalog_order.customer_order|owner_organization_id|SELECT|catalog_order_owner|false",
                "sharing_role|column|customer_iam.customer_account|id|SELECT|customer_iam_owner|false",
                "sharing_role|column|customer_iam.customer_account|status|SELECT|customer_iam_owner|false",
                "sharing_role|column|organization_access.organization|id|SELECT|organization_access_owner|false",
                "sharing_role|column|organization_access.organization|status|SELECT|organization_access_owner|false",
                "sharing_role|column|organization_access.organization_membership|account_id|SELECT|organization_access_owner|false",
                "sharing_role|column|organization_access.organization_membership|organization_id|SELECT|organization_access_owner|false",
                "sharing_role|column|organization_access.organization_membership|status|SELECT|organization_access_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|admissions_used|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|code_generation|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|created_by_account_id|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|download_allowed|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|evidence_id|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|expires_at|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|max_admissions|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|owner_organization_id|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|revocation_generation|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|share_id|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|status|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|valid_from|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|verification_mode|SELECT|sharing_owner|false",
                "sharing_role|schema|customer_iam||USAGE|customer_iam_owner|false",
                "sharing_role|schema|organization_access||USAGE|organization_access_owner|false",
                "sharing_role|schema|sharing||USAGE|sharing_owner|false");
        try (Connection admin = admin()) {
            List<String> actual = strings(admin, """
                    WITH facts AS (
                        SELECT acl.grantee, 'schema' kind, namespace.nspname::text object_name, '' column_name,
                            acl.privilege_type, acl.grantor, acl.is_grantable
                        FROM pg_namespace namespace CROSS JOIN LATERAL aclexplode(namespace.nspacl) acl
                        UNION ALL
                        SELECT acl.grantee, CASE WHEN relation.relkind='S' THEN 'sequence' ELSE 'table' END,
                            namespace.nspname||'.'||relation.relname, '', acl.privilege_type, acl.grantor, acl.is_grantable
                        FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
                        CROSS JOIN LATERAL aclexplode(relation.relacl) acl
                        UNION ALL
                        SELECT acl.grantee, 'column', namespace.nspname||'.'||relation.relname, attribute.attname,
                            acl.privilege_type, acl.grantor, acl.is_grantable
                        FROM pg_attribute attribute JOIN pg_class relation ON relation.oid=attribute.attrelid
                        JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
                        CROSS JOIN LATERAL aclexplode(attribute.attacl) acl
                        UNION ALL
                        SELECT acl.grantee, 'routine', routine.oid::regprocedure::text, '',
                            acl.privilege_type, acl.grantor, acl.is_grantable
                        FROM pg_proc routine CROSS JOIN LATERAL aclexplode(routine.proacl) acl
                    ) SELECT grantee::regrole::text||'|'||kind||'|'||object_name||'|'||column_name||'|'||privilege_type||'|'||grantor::regrole::text||'|'||is_grantable
                    FROM facts WHERE grantee IN ('sharing_role'::regrole,'customer_iam_role'::regrole,
                        'sharing_command_role'::regrole,'evidence_role'::regrole,'sharing_public_read_role'::regrole,
                        'sharing_admission_role'::regrole,'evidence_media_reader_role'::regrole)
                    ORDER BY 1
                    """);
            assertThat(expected.size()).isEqualTo(269);
            assertThat(actual.size()).isEqualTo(269);
            assertThat(Set.copyOf(actual).equals(expected)).as("independent frozen-source ACL equality").isTrue();
        }
    }
}
