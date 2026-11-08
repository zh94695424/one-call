package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

class SharingRoleLeastPrivilegeIT {
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String QUERY_LOGIN = "query_probe_" + UUID.randomUUID().toString().replace("-", "");
    private static final String COMMAND_LOGIN = "command_probe_" + UUID.randomUUID().toString().replace("-", "");
    private static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse("postgres:16"))
            .withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", PASSWORD)
            .withEnv("POSTGRES_DB", "sharing_least_privilege").withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
            .withStartupTimeout(Duration.ofMinutes(2));
    private static String url;
    private static Flyway flyway;
    private static final String PUBLIC_ROLE = "sharing_public_read_role";
    private static final java.util.Map<String, List<String>> PUBLIC_COLUMNS = java.util.Map.of(
            "sharing.evidence_share_grant", List.of("capability_token_hash", "evidence_id", "owner_organization_id", "verification_mode", "status", "valid_from", "expires_at", "download_allowed", "content_snapshot"),
            "evidence.evidence_record", List.of("id", "subject_id", "status", "source_type", "source_ref_id", "identity_at_capture"),
            "evidence.evidence_artifact", List.of("evidence_id", "media_object_id", "artifact_role"),
            "evidence.media_object", List.of("id", "sha256", "size_bytes", "mime_type"),
            "evidence.recording", List.of("media_object_id", "validated_duration"));
    private static final java.util.Map<String, List<String>> DENIED_COLUMNS = java.util.Map.of(
            "sharing.evidence_share_grant", List.of("id", "share_id", "created_by_account_id", "code_salt", "code_hash", "code_generation", "max_admissions", "admissions_used", "content_sha256", "revocation_generation", "revision"),
            "evidence.evidence_record", List.of("source_event_domain", "source_event_id", "call_id", "session_id", "reservation_id", "consent_version", "revision", "snapshot", "snapshot_sha256", "created_at", "solidified_at"),
            "evidence.evidence_artifact", List.of("id", "created_at"),
            "evidence.media_object", List.of("storage_provider", "bucket", "object_key", "object_version_id", "crypto_key_ref", "worm_status", "created_at"),
            "evidence.recording", List.of("id", "call_id", "recording_id", "declared_format", "declared_size", "declared_duration", "declared_sha256", "validated_sha256", "status", "upload_expires_at", "created_at", "validated_at", "revision"));
    private static java.util.Map<String, String> migrationBytesBefore;

    @BeforeAll
    static void start() throws Exception {
        migrationBytesBefore = legacyBytes();
        POSTGRES.start();
        url = "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/sharing_least_privilege";
        flyway = Flyway.configure().dataSource(url, "fixture_admin", PASSWORD).locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(13);
        try (Connection admin = admin(); Statement statement = admin.createStatement()) {
            try (var rows = statement.executeQuery("SELECT rolpassword IS NULL FROM pg_authid WHERE rolname='sharing_public_read_role'")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getBoolean(1)).isTrue();
            }
            assignPublicPassword(admin);
            statement.execute("CREATE ROLE " + QUERY_LOGIN + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + PASSWORD + "'");
            statement.execute("CREATE ROLE " + COMMAND_LOGIN + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + PASSWORD + "'");
            statement.execute("GRANT sharing_role TO " + QUERY_LOGIN);
            statement.execute("GRANT sharing_command_role TO " + COMMAND_LOGIN);
        }
    }

    @AfterAll
    static void stop() {
        POSTGRES.stop();
    }

    private static Connection admin() throws SQLException {
        return DriverManager.getConnection(url, "fixture_admin", PASSWORD);
    }

    private static Connection login(String name) throws SQLException {
        return DriverManager.getConnection(url, name, PASSWORD);
    }

    private static void assignPublicPassword(Connection connection) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (var parameters = connection.prepareStatement("SELECT set_config('pa16.fixture_password', ?, true)")) {
            parameters.setString(1, PASSWORD);
            parameters.execute();
        }
        try (var statement = connection.createStatement()) {
            statement.execute("DO $auth$ BEGIN EXECUTE format('ALTER ROLE sharing_public_read_role PASSWORD %L', current_setting('pa16.fixture_password')); END $auth$");
        }
        connection.commit();
        connection.setAutoCommit(autoCommit);
    }

    private static java.util.Map<String, String> legacyBytes() throws Exception {
        var result = new java.util.TreeMap<String, String>();
        try (var paths = Files.list(Path.of("src/main/resources/db/migration"))) {
            for (Path path : paths.filter(candidate -> candidate.getFileName().toString().matches("V00[1-9]__.*|V010__.*")).toList()) {
                result.put(path.getFileName().toString(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
            }
        }
        assertThat(result).hasSize(10);
        return result;
    }

    private static String v011Sql() throws Exception {
        try (var input = SharingRoleLeastPrivilegeIT.class.getClassLoader().getResourceAsStream("db/migration/V011__sharing_public_read_least_privilege.sql")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> strings(Connection connection, String sql) throws SQLException {
        var result = new java.util.ArrayList<String>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            while (rows.next()) result.add(rows.getString(1));
        }
        result.sort(String::compareTo);
        return result;
    }

    private static List<String> structure(Connection connection) throws SQLException {
        var result = new java.util.ArrayList<String>();
        for (String catalog : List.of("pg_namespace", "pg_class", "pg_attribute", "pg_attrdef", "pg_constraint", "pg_index", "pg_trigger", "pg_proc")) {
            result.addAll(strings(connection, "SELECT '" + catalog + "|' || (to_jsonb(facts) - 'relacl' - 'attacl' - 'nspacl' - 'proacl')::text FROM " + catalog + " facts"));
        }
        result.sort(String::compareTo);
        return result;
    }

    private static List<String> applicationData(Connection connection) throws SQLException {
        var result = new java.util.ArrayList<String>();
        for (String table : strings(connection, "SELECT quote_ident(namespace.nspname)||'.'||quote_ident(relation.relname) FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_' AND relation.relkind IN ('r','p') AND relation.relname <> 'flyway_schema_history'")) {
            result.add(table + "|" + strings(connection, "SELECT md5(coalesce(string_agg(to_jsonb(facts)::text,chr(10) ORDER BY to_jsonb(facts)::text),'')) FROM " + table + " facts").getFirst());
        }
        return result;
    }

    private static List<String> publicState(Connection connection) throws SQLException {
        var result = new java.util.ArrayList<>(aclSnapshot(connection));
        for (String sql : List.of(
                "SELECT 'role|' || (to_jsonb(role) - 'rolpassword')::text FROM pg_roles role WHERE rolname='sharing_public_read_role'",
                "SELECT 'membership|' || to_jsonb(membership)::text FROM pg_auth_members membership",
                "SELECT 'settings|' || to_jsonb(setting)::text FROM pg_db_role_setting setting",
                "SELECT 'dependency|' || to_jsonb(dependency)::text FROM pg_shdepend dependency",
                "SELECT 'database|' || datname || '|' || coalesce(datacl::text,'') FROM pg_database",
                "SELECT 'default|' || to_jsonb(privilege)::text FROM pg_default_acl privilege",
                "SELECT 'history|' || to_jsonb(history)::text FROM flyway_schema_history history")) {
            result.addAll(strings(connection, sql));
        }
        result.sort(String::compareTo);
        return result;
    }

    private static void rejectedV011(Connection connection, String injection, String migration, String state) throws Exception {
        connection.setAutoCommit(false);
        try (var statement = connection.createStatement()) {
            if (injection != null) statement.execute(injection);
            var before = publicState(connection);
            var definitions = structure(connection);
            Savepoint savepoint = connection.setSavepoint();
            assertThatThrownBy(() -> statement.execute(migration)).isInstanceOfSatisfying(SQLException.class,
                    failure -> assertThat(failure.getSQLState()).isEqualTo(state));
            connection.rollback(savepoint);
            assertThat(publicState(connection)).containsExactlyElementsOf(before);
            assertThat(structure(connection)).containsExactlyElementsOf(definitions);
        } finally {
            connection.rollback();
            connection.setAutoCommit(true);
        }
    }

    @Test
    void v011UpgradeIsPrivilegeOnlyAtomicAndRejectsIncompatibleState() throws Exception {
        var scratch = new GenericContainer<>(DockerImageName.parse("postgres:16"))
                .withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", PASSWORD)
                .withEnv("POSTGRES_DB", "pa16_upgrade").withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
                .withStartupTimeout(Duration.ofMinutes(2));
        scratch.start();
        try {
            String scratchUrl = "jdbc:postgresql://" + scratch.getHost() + ":" + scratch.getMappedPort(5432) + "/pa16_upgrade";
            var beforeMigration = Flyway.configure().dataSource(scratchUrl, "fixture_admin", PASSWORD).locations("classpath:db/migration").target("10").load();
            assertThat(beforeMigration.migrate().migrationsExecuted).isEqualTo(10);
            assertThat(beforeMigration.info().current().getVersion().getVersion()).isEqualTo("010");
            try (var connection = DriverManager.getConnection(scratchUrl, "fixture_admin", PASSWORD)) {
                String migration = v011Sql();
                rejectedV011(connection, null, migration + "\nSELECT 1/0;", "22012");
                try (var statement = connection.createStatement()) {
                    statement.execute("GRANT TEMPORARY ON DATABASE pa16_upgrade TO evidence_role");
                }
                var definitions = structure(connection);
                var data = applicationData(connection);
                var oldAcl = aclSnapshot(connection);
                var oldDatabaseAcl = strings(connection, "SELECT acl.grantor||'|'||acl.grantee||'|'||acl.privilege_type||'|'||acl.is_grantable FROM pg_database database CROSS JOIN LATERAL aclexplode(coalesce(database.datacl,acldefault('d',database.datdba))) acl WHERE datname=current_database() AND NOT (acl.grantee=0 AND acl.privilege_type='TEMPORARY')");
                var latest = Flyway.configure().dataSource(scratchUrl, "fixture_admin", PASSWORD).locations("classpath:db/migration").target("11").load();
                assertThat(latest.migrate().migrationsExecuted).isEqualTo(1);
                assertThat(latest.info().current().getVersion().getVersion()).isEqualTo("011");
                assertThat(latest.info().applied()).hasSize(11);
                latest.validate();
                assertThat(latest.migrate().migrationsExecuted).isZero();
                assertThat(structure(connection)).containsExactlyElementsOf(definitions);
                assertThat(applicationData(connection)).containsExactlyElementsOf(data);
                long publicOid;
                try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT oid, (SELECT rolpassword IS NULL FROM pg_authid WHERE rolname='sharing_public_read_role') FROM pg_roles WHERE rolname='sharing_public_read_role'")) {
                    rows.next(); publicOid = rows.getLong(1); assertThat(rows.getBoolean(2)).isTrue();
                }
                assertThat(aclSnapshot(connection).stream().filter(row -> !row.contains("|" + publicOid + "|"))).containsExactlyElementsOf(oldAcl);
                assertThat(strings(connection, "SELECT acl.grantor||'|'||acl.grantee||'|'||acl.privilege_type||'|'||acl.is_grantable FROM pg_database database CROSS JOIN LATERAL aclexplode(database.datacl) acl WHERE datname=current_database()"))
                        .containsExactlyElementsOf(oldDatabaseAcl);
                var beforeReplay = publicState(connection);
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement()) { statement.execute(migration); }
                connection.commit(); connection.setAutoCommit(true);
                assertThat(publicState(connection)).containsExactlyElementsOf(beforeReplay);
                assignPublicPassword(connection);
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement()) { statement.execute(migration); }
                connection.commit(); connection.setAutoCommit(true);
                assertThat(publicState(connection)).containsExactlyElementsOf(beforeReplay);
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement()) {
                    statement.execute("REVOKE USAGE ON SCHEMA sharing FROM sharing_public_read_role; REVOKE SELECT (id) ON evidence.media_object FROM sharing_public_read_role");
                    statement.execute(migration);
                    assertThat(publicState(connection)).containsExactlyElementsOf(beforeReplay);
                } finally { connection.rollback(); connection.setAutoCommit(true); }
                for (String injection : List.of(
                        "ALTER ROLE sharing_public_read_role SUPERUSER",
                        "ALTER ROLE sharing_public_read_role NOLOGIN",
                        "ALTER ROLE sharing_public_read_role CREATEDB",
                        "ALTER ROLE sharing_public_read_role CREATEROLE",
                        "ALTER ROLE sharing_public_read_role INHERIT",
                        "ALTER ROLE sharing_public_read_role REPLICATION",
                        "ALTER ROLE sharing_public_read_role BYPASSRLS",
                        "ALTER ROLE sharing_public_read_role CONNECTION LIMIT 1",
                        "ALTER ROLE sharing_public_read_role VALID UNTIL '2030-01-01'",
                        "GRANT sharing_command_role TO sharing_public_read_role",
                        "GRANT sharing_public_read_role TO sharing_role",
                        "ALTER SCHEMA sharing OWNER TO sharing_public_read_role",
                        "ALTER TABLE sharing.evidence_share_grant OWNER TO sharing_public_read_role",
                        "ALTER ROLE sharing_public_read_role SET statement_timeout='1s'",
                        "GRANT SELECT ON sharing.evidence_share_grant TO sharing_public_read_role",
                        "GRANT SELECT (code_hash) ON sharing.evidence_share_grant TO sharing_public_read_role",
                        "SET LOCAL ROLE evidence_owner; GRANT SELECT (id) ON evidence.media_object TO sharing_public_read_role WITH GRANT OPTION; RESET ROLE",
                        "GRANT CREATE ON SCHEMA sharing TO sharing_public_read_role",
                        "GRANT USAGE ON SCHEMA public TO sharing_public_read_role",
                        "GRANT EXECUTE ON FUNCTION evidence.guard_recording() TO sharing_public_read_role",
                        "ALTER DEFAULT PRIVILEGES IN SCHEMA sharing GRANT SELECT ON TABLES TO sharing_public_read_role",
                        "GRANT USAGE ON SCHEMA public TO PUBLIC",
                        "GRANT SELECT ON sharing.share_admission TO PUBLIC")) {
                    rejectedV011(connection, injection, migration, "P0001");
                }
                for (String privilege : List.of("TEMPORARY", "CREATE", "CONNECT")) {
                    rejectedV011(connection, "GRANT " + privilege + " ON DATABASE pa16_upgrade TO sharing_public_read_role", migration, "P0001");
                }
                rejectedV011(connection, "GRANT CREATE ON DATABASE pa16_upgrade TO PUBLIC", migration, "P0001");
                rejectedV011(connection, "GRANT CONNECT ON DATABASE postgres TO sharing_public_read_role", migration, "P0001");
                rejectedV011(connection, "ALTER ROLE sharing_public_read_role IN DATABASE pa16_upgrade SET statement_timeout='1s'", migration, "P0001");
                rejectedV011(connection, "CREATE SEQUENCE sharing.pa16_probe_seq; GRANT USAGE ON SEQUENCE sharing.pa16_probe_seq TO sharing_public_read_role", migration, "P0001");
                rejectedV011(connection, "GRANT TEMPORARY ON DATABASE pa16_upgrade TO PUBLIC", migration + "\nSELECT 1/0;", "22012");
                assertThat(structure(connection)).containsExactlyElementsOf(definitions);
                assertThat(applicationData(connection)).containsExactlyElementsOf(data);
                System.out.println("PA16 V010->V011=1 repeat=0 replay=idempotent password-independent=true rollback=22012 incompatibility=P0001 structure=unchanged PUBLIC=TEMP-only");
            }
        } finally { scratch.stop(); }
    }

    @Test
    void v011PublicReadRoleAndAclAllowlistAreExact() throws Exception {
        try (var connection = admin(); var statement = connection.createStatement()) {
            assertThat(strings(connection, "SELECT rolcanlogin||'|'||rolsuper||'|'||rolcreatedb||'|'||rolcreaterole||'|'||rolinherit||'|'||rolreplication||'|'||rolbypassrls||'|'||rolconnlimit||'|'||(rolvaliduntil IS NULL)||'|'||(rolconfig IS NULL) FROM pg_roles WHERE rolname='sharing_public_read_role'"))
                    .containsExactly("true|false|false|false|false|false|false|-1|true|true");
            for (String sql : List.of(
                    "SELECT 1 FROM pg_auth_members WHERE member='sharing_public_read_role'::regrole OR roleid='sharing_public_read_role'::regrole",
                    "SELECT 1 FROM pg_db_role_setting WHERE setrole='sharing_public_read_role'::regrole",
                    "SELECT 1 FROM pg_shdepend WHERE refclassid='pg_authid'::regclass AND refobjid='sharing_public_read_role'::regrole AND deptype<>'a'",
                    "SELECT 1 FROM pg_default_acl CROSS JOIN LATERAL aclexplode(defaclacl) acl WHERE acl.grantee='sharing_public_read_role'::regrole OR acl.grantor='sharing_public_read_role'::regrole",
                    "SELECT 1 FROM pg_database CROSS JOIN LATERAL aclexplode(datacl) acl WHERE acl.grantee='sharing_public_read_role'::regrole",
                    "SELECT 1 FROM pg_tablespace CROSS JOIN LATERAL aclexplode(spcacl) acl WHERE acl.grantee='sharing_public_read_role'::regrole",
                    "SELECT 1 FROM pg_parameter_acl CROSS JOIN LATERAL aclexplode(paracl) acl WHERE acl.grantee='sharing_public_read_role'::regrole")) assertThat(strings(connection, sql)).isEmpty();
            var expected = new java.util.ArrayList<String>();
            PUBLIC_COLUMNS.forEach((table, columns) -> columns.forEach(column -> expected.add(table + "." + column + "|SELECT|false|true")));
            expected.sort(String::compareTo);
            assertThat(strings(connection, "SELECT namespace.nspname||'.'||relation.relname||'.'||attribute.attname||'|'||acl.privilege_type||'|'||acl.is_grantable||'|'||(acl.grantor=relation.relowner) FROM pg_attribute attribute JOIN pg_class relation ON relation.oid=attribute.attrelid JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace CROSS JOIN LATERAL aclexplode(attribute.attacl) acl WHERE acl.grantee='sharing_public_read_role'::regrole"))
                    .containsExactlyElementsOf(expected);
            assertThat(strings(connection, "SELECT namespace.nspname||'|'||acl.privilege_type||'|'||acl.is_grantable||'|'||(acl.grantor=namespace.nspowner) FROM pg_namespace namespace CROSS JOIN LATERAL aclexplode(namespace.nspacl) acl WHERE acl.grantee='sharing_public_read_role'::regrole"))
                    .containsExactly("evidence|USAGE|false|true", "sharing|USAGE|false|true");
            assertThat(strings(connection, "SELECT 1 FROM pg_class CROSS JOIN LATERAL aclexplode(relacl) acl WHERE acl.grantee='sharing_public_read_role'::regrole")).isEmpty();
            assertThat(strings(connection, "SELECT 1 FROM pg_proc CROSS JOIN LATERAL aclexplode(proacl) acl WHERE acl.grantee='sharing_public_read_role'::regrole")).isEmpty();
            try (var rows = statement.executeQuery("SELECT has_database_privilege('sharing_public_read_role',current_database(),'CREATE'),has_database_privilege('sharing_public_read_role',current_database(),'TEMPORARY'),has_database_privilege('sharing_public_read_role',current_database(),'CONNECT')")) {
                rows.next(); assertThat(rows.getBoolean(1)).isFalse(); assertThat(rows.getBoolean(2)).isFalse(); assertThat(rows.getBoolean(3)).isTrue();
            }
            String application = "namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_'";
            try (var rows = statement.executeQuery("SELECT namespace.nspname,has_schema_privilege('sharing_public_read_role',namespace.oid,'USAGE'),has_schema_privilege('sharing_public_read_role',namespace.oid,'CREATE') FROM pg_namespace namespace WHERE " + application)) {
                while (rows.next()) { assertThat(rows.getBoolean(2)).isEqualTo(List.of("sharing", "evidence").contains(rows.getString(1))); assertThat(rows.getBoolean(3)).isFalse(); }
            }
            try (var rows = statement.executeQuery("SELECT namespace.nspname||'.'||relation.relname,attribute.attname,has_column_privilege('sharing_public_read_role',relation.oid,attribute.attnum,'SELECT'),has_column_privilege('sharing_public_read_role',relation.oid,attribute.attnum,'INSERT'),has_column_privilege('sharing_public_read_role',relation.oid,attribute.attnum,'UPDATE'),has_column_privilege('sharing_public_read_role',relation.oid,attribute.attnum,'REFERENCES') FROM pg_attribute attribute JOIN pg_class relation ON relation.oid=attribute.attrelid JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace WHERE " + application + " AND relation.relkind IN ('r','p','v','m','f') AND attribute.attnum>0 AND NOT attribute.attisdropped")) {
                while (rows.next()) {
                    assertThat(rows.getBoolean(3)).isEqualTo(PUBLIC_COLUMNS.getOrDefault(rows.getString(1), List.of()).contains(rows.getString(2)));
                    for (int index = 4; index <= 6; index++) assertThat(rows.getBoolean(index)).isFalse();
                }
            }
            try (var rows = statement.executeQuery("SELECT relation.oid FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace WHERE " + application + " AND relation.relkind IN ('r','p','v','m','f')")) {
                while (rows.next()) {
                    for (String privilege : List.of("SELECT", "INSERT", "UPDATE", "DELETE", "TRUNCATE", "REFERENCES", "TRIGGER"))
                        assertThat(strings(connection, "SELECT has_table_privilege('sharing_public_read_role'," + rows.getLong(1) + ", '" + privilege + "')")).containsExactly("f");
                }
            }
            assertThat(strings(connection, "SELECT 1 FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid=routine.pronamespace WHERE " + application + " AND has_function_privilege('sharing_public_read_role',routine.oid,'EXECUTE')")).isEmpty();
            assertThat(strings(connection, "SELECT 1 FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace WHERE " + application + " AND relation.relkind='S' AND (has_sequence_privilege('sharing_public_read_role',relation.oid,'USAGE') OR has_sequence_privilege('sharing_public_read_role',relation.oid,'SELECT') OR has_sequence_privilege('sharing_public_read_role',relation.oid,'UPDATE'))")).isEmpty();
            for (String sql : List.of(
                    "SELECT 1 FROM pg_namespace namespace CROSS JOIN LATERAL aclexplode(nspacl) acl WHERE " + application + " AND acl.grantee=0",
                    "SELECT 1 FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace CROSS JOIN LATERAL aclexplode(relacl) acl WHERE " + application + " AND acl.grantee=0",
                    "SELECT 1 FROM pg_attribute attribute JOIN pg_class relation ON relation.oid=attribute.attrelid JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace CROSS JOIN LATERAL aclexplode(attacl) acl WHERE " + application + " AND acl.grantee=0",
                    "SELECT 1 FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid=routine.pronamespace CROSS JOIN LATERAL aclexplode(coalesce(proacl,acldefault('f',proowner))) acl WHERE " + application + " AND acl.grantee=0",
                    "SELECT 1 FROM pg_database CROSS JOIN LATERAL aclexplode(datacl) acl WHERE datname=current_database() AND acl.grantee=0 AND acl.privilege_type IN ('CREATE','TEMPORARY')")) assertThat(strings(connection, sql)).isEmpty();
            for (var entry : DENIED_COLUMNS.entrySet()) {
                var complement = strings(connection, "SELECT attname FROM pg_attribute WHERE attrelid='" + entry.getKey() + "'::regclass AND attnum>0 AND NOT attisdropped");
                complement.removeAll(PUBLIC_COLUMNS.get(entry.getKey()));
                assertThat(complement).containsExactlyInAnyOrderElementsOf(entry.getValue());
            }
            System.out.println("PA16 catalog role=direct-login membership=0 ownership=0 schemaUSAGE=2 columnSELECT=24 tableSELECT=0 grants=owner/no-option PUBLIC=CONNECT-preserved");
        }
    }

    @Test
    void v011PublicReadAllowsOnlyProjectionAndDeniesWritesAndEscalation() throws Exception {
        List<String> routines;
        List<String> definitions;
        List<String> data;
        try (var admin = admin(); var statement = admin.createStatement()) {
            definitions = structure(admin);
            data = applicationData(admin);
            statement.execute("CREATE SEQUENCE sharing.pa16_probe_seq; REVOKE ALL ON SEQUENCE sharing.pa16_probe_seq FROM PUBLIC");
            routines = strings(admin, "SELECT namespace.nspname||'.'||routine.proname||'('||pg_get_function_identity_arguments(routine.oid)||')' FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid=routine.pronamespace WHERE namespace.nspname IN ('sharing','evidence')");
            assertThat(routines).containsExactlyInAnyOrder("sharing.guard_share_grant()", "sharing.guard_share_admission()", "sharing.reject_row_mutation()", "evidence.guard_recording()", "evidence.guard_evidence_record()", "evidence.guard_evidence_record_creation_cycle()", "evidence.guard_evidence_event_creation_cycle()", "evidence.reject_row_mutation()");
        }
        try (var connection = login(PUBLIC_ROLE); var statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET TRANSACTION READ WRITE");
            assertPublicIdentity(connection, "off");
            int allowed = 0;
            for (var entry : PUBLIC_COLUMNS.entrySet()) {
                for (String column : entry.getValue()) { statement.executeQuery("SELECT " + column + " FROM " + entry.getKey()).close(); allowed++; }
                statement.executeQuery("SELECT " + String.join(",", entry.getValue()) + " FROM " + entry.getKey()).close();
                for (String lock : List.of("FOR UPDATE", "FOR NO KEY UPDATE", "FOR SHARE", "FOR KEY SHARE"))
                    denied(connection, "SELECT " + entry.getValue().getFirst() + " FROM " + entry.getKey() + " " + lock, "42501");
            }
            assertThat(allowed).isEqualTo(24);
            statement.executeQuery("SELECT evidence_id, owner_organization_id, verification_mode, status, valid_from, expires_at, download_allowed, content_snapshot FROM sharing.evidence_share_grant WHERE capability_token_hash = repeat('0',64)").close();
            statement.executeQuery("SELECT id, subject_id, status, source_type, source_ref_id, identity_at_capture FROM evidence.evidence_record WHERE id = '00000000-0000-0000-0000-000000000000'").close();
            statement.executeQuery("SELECT a.artifact_role, m.sha256, m.size_bytes, m.mime_type, r.validated_duration FROM evidence.evidence_artifact a JOIN evidence.media_object m ON m.id=a.media_object_id LEFT JOIN evidence.recording r ON r.media_object_id=a.media_object_id WHERE a.evidence_id = '00000000-0000-0000-0000-000000000000'").close();
            statement.executeQuery("SELECT clock_timestamp()").close();
            int deniedColumns = 0;
            for (var entry : DENIED_COLUMNS.entrySet()) {
                for (String column : entry.getValue()) { denied(connection, "SELECT " + column + " FROM " + entry.getKey(), "42501"); deniedColumns++; }
            }
            var protectedTables = new java.util.ArrayList<>(PUBLIC_COLUMNS.keySet());
            try (var admin = admin()) {
                for (String table : List.of("sharing.share_admission", "sharing.share_access_event", "evidence.evidence_event")) {
                    var columns = strings(admin, "SELECT attname FROM pg_attribute WHERE attrelid='" + table + "'::regclass AND attnum>0 AND NOT attisdropped");
                    assertThat(columns).hasSize(table.endsWith("share_admission") ? 7 : table.endsWith("share_access_event") ? 11 : 8);
                    for (String column : columns) { denied(connection, "SELECT " + column + " FROM " + table, "42501"); deniedColumns++; }
                    protectedTables.add(table);
                }
            }
            assertThat(deniedColumns).isEqualTo(70);
            for (String table : protectedTables) denied(connection, "SELECT * FROM " + table, "42501");
            protectedTables.addAll(List.of("audit.audit_fact", "platform.outbox_event", "platform.idempotency_record"));
            for (String table : protectedTables) {
                String column = PUBLIC_COLUMNS.containsKey(table) ? PUBLIC_COLUMNS.get(table).getFirst() : "id";
                denied(connection, "INSERT INTO " + table + " DEFAULT VALUES", "42501");
                denied(connection, "UPDATE " + table + " SET " + column + "=" + column + " WHERE false", "42501");
                denied(connection, "DELETE FROM " + table + " WHERE false", "42501");
                denied(connection, "TRUNCATE " + table, "42501");
            }
            for (String table : List.of("audit.audit_fact", "platform.outbox_event", "platform.idempotency_record")) denied(connection, "SELECT id FROM " + table, "42501");
            for (String sql : List.of(
                    "CREATE TABLE sharing.pa16_probe(id int)", "CREATE TABLE evidence.pa16_probe(id int)", "CREATE TABLE public.pa16_probe(id int)",
                    "CREATE SCHEMA pa16_probe", "CREATE TEMP TABLE pa16_probe(id int)",
                    "ALTER TABLE sharing.evidence_share_grant ADD COLUMN pa16_probe int", "DROP TABLE sharing.share_access_event",
                    "CREATE SEQUENCE sharing.pa16_probe", "CREATE FUNCTION sharing.pa16_probe() RETURNS int LANGUAGE sql AS 'SELECT 1'",
                    "CREATE ROLE pa16_probe_role NOLOGIN", "CREATE VIEW sharing.pa16_probe_view AS SELECT evidence_id FROM sharing.evidence_share_grant",
                    "CREATE INDEX pa16_probe_index ON sharing.evidence_share_grant (evidence_id)",
                    "MERGE INTO sharing.evidence_share_grant AS target USING (VALUES (NULL::uuid)) AS source(evidence_id) ON target.evidence_id=source.evidence_id WHEN MATCHED THEN UPDATE SET evidence_id=source.evidence_id",
                    "COPY sharing.evidence_share_grant (evidence_id) FROM STDIN",
                    "SET ROLE sharing_command_role", "SET LOCAL ROLE evidence_role", "SET ROLE sharing_owner", "SET ROLE evidence_owner",
                    "SET SESSION AUTHORIZATION sharing_command_role", "GRANT sharing_command_role TO sharing_public_read_role", "ALTER ROLE sharing_public_read_role SUPERUSER",
                    "SELECT nextval('sharing.pa16_probe_seq')", "SELECT last_value FROM sharing.pa16_probe_seq", "SELECT setval('sharing.pa16_probe_seq',1)",
                    "SELECT platform.outbox_append(NULL::text,NULL::text,NULL::text,NULL::uuid,NULL::text,NULL::jsonb,NULL::text,NULL::text)",
                    "SELECT audit.append_fact(NULL::timestamptz,NULL::text,NULL::uuid,NULL::text,NULL::text,NULL::uuid,NULL::text,NULL::jsonb,NULL::jsonb,NULL::text,NULL::text)",
                    "SELECT platform.idempotency_begin(NULL::text,NULL::text,NULL::text)",
                    "SELECT platform.idempotency_complete(NULL::uuid,NULL::integer,NULL::text,NULL::uuid,NULL::jsonb,NULL::text)")) {
                denied(connection, sql, "42501"); assertPublicIdentity(connection, "off");
            }
            for (String routine : routines) denied(connection, "SELECT " + routine, "42501");
            statement.execute("RESET ROLE"); assertPublicIdentity(connection, "off");
            statement.execute("SET ROLE NONE"); assertPublicIdentity(connection, "off");
            assertThat(strings(connection, "SELECT 1 FROM pg_locks WHERE pid=pg_backend_pid() AND locktype='tuple'")).isEmpty();
            connection.rollback();
            statement.execute("SET TRANSACTION READ ONLY"); assertPublicIdentity(connection, "on");
            denied(connection, "UPDATE sharing.evidence_share_grant SET admissions_used=admissions_used WHERE false", "25006");
            for (var entry : PUBLIC_COLUMNS.entrySet()) statement.executeQuery("SELECT " + String.join(",",entry.getValue()) + " FROM " + entry.getKey()).close();
            connection.rollback();
            try (var autoCommit = login(PUBLIC_ROLE); var createDatabase = autoCommit.createStatement()) {
                assertThat(autoCommit.getAutoCommit()).isTrue();
                assertThatThrownBy(() -> createDatabase.execute("CREATE DATABASE pa16_probe_database"))
                        .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
            }
            try (var admin = admin()) {
                assertThat(strings(admin, "SELECT 1 FROM pg_database WHERE datname='pa16_probe_database'")).isEmpty();
                assertThat(strings(admin, "SELECT 1 FROM pg_roles WHERE rolname='pa16_probe_role'")).isEmpty();
            }
            System.out.println("PA16 allowedReads=24 deniedColumns=70 negativeSQLSTATE=42501 READ_ONLY_SQLSTATE=25006 rowLocks=0 roleEscalation=denied");
        } finally {
            try (var admin = admin(); var statement = admin.createStatement()) {
                statement.execute("DROP SEQUENCE sharing.pa16_probe_seq");
                assertThat(structure(admin)).containsExactlyElementsOf(definitions);
                assertThat(applicationData(admin)).containsExactlyElementsOf(data);
            }
        }
    }

    private static void assertPublicIdentity(Connection connection, String readOnly) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT current_user, session_user, current_setting('transaction_read_only'), pg_backend_pid()")) {
            rows.next(); assertThat(rows.getString(1)).isEqualTo(PUBLIC_ROLE); assertThat(rows.getString(2)).isEqualTo(PUBLIC_ROLE);
            assertThat(rows.getString(3)).isEqualTo(readOnly); assertThat(rows.getInt(4)).isPositive();
        }
    }

    @Test
    void v011LegacyMigrationBytesRemainUnchanged() throws Exception {
        assertThat(legacyBytes()).containsExactlyInAnyOrderEntriesOf(migrationBytesBefore);
        v010LegacyMigrationDigestsRemainCanonical();
        try (var input = getClass().getClassLoader().getResourceAsStream("db/migration/V010__sharing_role_least_privilege.sql")) {
            var canonical = new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8);
            assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical)))
                    .isEqualTo("fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343");
        }
        System.out.println("PA16 V001-V010 raw-byte identity=10/10 canonical-LF identity=10/10");
    }

    private static List<String> aclSnapshot(Connection connection) throws SQLException {
        List<String> snapshot = new java.util.ArrayList<>();
        List<String> queries = List.of(
                "SELECT 'schema|'||n.nspname||'|'||n.nspowner||'|'||x.grantor||'|'||x.grantee||'|'||x.privilege_type||'|'||x.is_grantable FROM pg_namespace n CROSS JOIN LATERAL aclexplode(coalesce(n.nspacl,acldefault('n',n.nspowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering')",
                "SELECT 'relation|'||n.nspname||'.'||c.relname||'|'||c.relowner||'|'||x.grantor||'|'||x.grantee||'|'||x.privilege_type||'|'||x.is_grantable FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(c.relacl,acldefault('r',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND c.relkind<>'S'",
                "SELECT 'column|'||n.nspname||'.'||c.relname||'.'||a.attname||'|'||x.grantor||'|'||x.grantee||'|'||x.privilege_type||'|'||x.is_grantable FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(a.attacl,acldefault('c',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND a.attnum>0 AND NOT a.attisdropped",
                "SELECT 'procedure|'||n.nspname||'.'||p.proname||'('||pg_get_function_identity_arguments(p.oid)||')|'||p.proowner||'|'||x.grantor||'|'||x.grantee||'|'||x.privilege_type||'|'||x.is_grantable FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace CROSS JOIN LATERAL aclexplode(coalesce(p.proacl,acldefault('f',p.proowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering')",
                "SELECT 'sequence|'||n.nspname||'.'||c.relname||'|'||c.relowner||'|'||x.grantor||'|'||x.grantee||'|'||x.privilege_type||'|'||x.is_grantable FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(c.relacl,acldefault('S',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND c.relkind='S'",
                "SELECT 'role|'||rolname||'|'||rolcanlogin||'|'||rolsuper||'|'||rolcreatedb||'|'||rolcreaterole||'|'||rolinherit||'|'||rolreplication||'|'||rolbypassrls||'|'||rolconnlimit||'|'||(rolpassword IS NULL) FROM pg_authid WHERE rolname IN ('sharing_owner','sharing_role','sharing_command_role')",
                "SELECT 'membership|'||r.rolname||'|'||m.rolname||'|'||a.admin_option FROM pg_auth_members a JOIN pg_roles r ON r.oid=a.roleid JOIN pg_roles m ON m.oid=a.member WHERE r.rolname IN ('sharing_owner','sharing_role','sharing_command_role') OR m.rolname IN ('sharing_owner','sharing_role','sharing_command_role')");
        for (String query : queries) {
            try (Statement statement = connection.createStatement(); var rows = statement.executeQuery(query)) {
                while (rows.next()) snapshot.add(rows.getString(1));
            }
        }
        snapshot.sort(String::compareTo);
        return snapshot;
    }

    private static void provePostAclRollback(String scratchUrl, String password) throws Exception {
        Path migrations = Files.createTempDirectory("pa13-v010-rollback-");
        Path migrationDir = Files.createDirectories(migrations.resolve("db/migration"));
        List<String> paths = List.of("V001__platform_schemas_roles_idempotency_outbox_audit.sql", "V002__identity_organization_and_employee.sql", "V003__subject_number_and_claim.sql", "V004__catalog_order_and_payment.sql", "V005__ivr_task_session_call.sql", "V006__trial_entitlement_issuance_reservation_usage.sql", "V007__activation_and_consent.sql", "V008__evidence_media.sql", "V009__sharing.sql", "V010__sharing_role_least_privilege.sql");
        for (String path : paths) {
            try (InputStream source = SharingRoleLeastPrivilegeIT.class.getClassLoader().getResourceAsStream("db/migration/" + path)) {
                byte[] bytes = source.readAllBytes();
                if (path.startsWith("V010")) {
                    bytes = (new String(bytes, StandardCharsets.UTF_8) + "\nSELECT 1/0;\n").getBytes(StandardCharsets.UTF_8);
                }
                Files.write(migrationDir.resolve(path), bytes);
            }
        }
        Flyway beforeV010 = Flyway.configure().dataSource(scratchUrl, "fixture_admin", password).locations("filesystem:" + migrationDir).target("9").load();
        beforeV010.migrate();
        assertThat(beforeV010.info().current().getVersion().getVersion()).isEqualTo("009");
        try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", password);
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE ROLE sharing_command_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT");
            statement.execute("GRANT INSERT (id) ON evidence.evidence_event TO sharing_command_role");
            statement.execute("CREATE SEQUENCE evidence.pa13_rollback_sequence");
            statement.execute("GRANT ALL ON SEQUENCE evidence.pa13_rollback_sequence TO PUBLIC, sharing_role, sharing_command_role");
        }
        List<String> before;
        try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", password)) {
            before = aclSnapshot(connection);
        }
        Flyway failing = Flyway.configure().dataSource(scratchUrl, "fixture_admin", password).locations("filesystem:" + migrationDir).target("10").load();
        assertThatThrownBy(failing::migrate).isInstanceOf(RuntimeException.class);
        try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", password); Statement statement = connection.createStatement();
                var rows = statement.executeQuery("select count(*) from flyway_schema_history where version='010'")) {
            rows.next();
            assertThat(rows.getInt(1)).isZero();
            assertThat(aclSnapshot(connection)).containsExactlyElementsOf(before);
        } finally {
            try (var walk = Files.walk(migrations)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                });
            }
        }
    }

    private static void assertV010NotPartiallyApplied(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                var rows = statement.executeQuery("select has_schema_privilege('sharing_command_role','sharing','USAGE'), (select count(*) from pg_class c join pg_namespace n on n.oid=c.relnamespace cross join lateral aclexplode(coalesce(c.relacl,acldefault('r',c.relowner))) a where n.nspname='sharing' and a.grantee='sharing_command_role'::regrole)")) {
            rows.next();
            assertThat(rows.getBoolean(1)).isFalse();
            assertThat(rows.getLong(2)).isZero();
        }
    }

    private static void assertMigrationRejectedAndRestored(Flyway migration, String scratchUrl, String password) throws Exception {
        List<String> before;
        try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", password);
                Statement statement = connection.createStatement()) {
            before = aclSnapshot(connection);
            statement.execute("delete from flyway_schema_history where version='010'");
        }
        assertThatThrownBy(migration::migrate).isInstanceOf(RuntimeException.class);
        try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", password);
                Statement statement = connection.createStatement();
                var rows = statement.executeQuery("select count(*) from flyway_schema_history where version='010' and success")) {
            rows.next();
            assertThat(rows.getInt(1)).isZero();
            assertThat(aclSnapshot(connection)).containsExactlyElementsOf(before);
        }
    }

    private static void denied(Connection connection, String sql, String state) {
        Savepoint probe;
        try {
            probe = connection.setSavepoint();
        } catch (SQLException failure) {
            throw new AssertionError(failure);
        }
        try {
            assertThatThrownBy(() -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            }).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo(state));
        } finally {
            try {
                connection.rollback(probe);
            } catch (SQLException failure) {
                throw new AssertionError(failure);
            }
        }
    }

    @Test
    void v010MigrationIdentityAndPostgres16() throws Exception {
        try (Connection connection = admin(); Statement statement = connection.createStatement();
                var rows = statement.executeQuery("select current_setting('server_version_num')::int, version()")) {
            rows.next();
            assertThat(rows.getInt(1)).isBetween(160000, 169999);
            System.out.println("PA13 PostgreSQL 16 evidence: " + rows.getString(2) + "; image=" + POSTGRES.getDockerImageName()
                    + "; imageId=" + POSTGRES.getContainerInfo().getImageId());
        }
        assertThat(flyway.info().applied()).hasSize(13);
        assertThat(flyway.info().applied()[10].getVersion().getVersion()).isEqualTo("011");
        assertThat(flyway.info().applied()[11].getVersion().getVersion()).isEqualTo("012");
        assertThat(flyway.info().applied()[11].getScript()).isEqualTo("V012__sharing_admission_least_privilege.sql");
        assertThat(flyway.info().applied()[12].getVersion().getVersion()).isEqualTo("013");
        assertThat(flyway.info().applied()[12].getScript()).isEqualTo("V013__evidence_media_reader_role.sql");
        assertThat(flyway.info().applied()[9].getVersion().getVersion()).isEqualTo("010");
        assertThat(flyway.info().applied()[9].getScript()).isEqualTo("V010__sharing_role_least_privilege.sql");
        assertThat(flyway.info().applied()[9].getDescription()).isEqualTo("sharing role least privilege");
        try (Connection connection = admin(); Statement statement = connection.createStatement()) {
            statement.execute("REVOKE sharing_command_role FROM " + COMMAND_LOGIN);
            try (var membership = statement.executeQuery("select pg_has_role('" + QUERY_LOGIN + "','sharing_role','MEMBER')")) {
                membership.next();
                assertThat(membership.getBoolean(1)).isTrue();
            }
            try (InputStream migration = getClass().getClassLoader().getResourceAsStream("db/migration/V010__sharing_role_least_privilege.sql")) {
                statement.execute(new String(migration.readAllBytes(), StandardCharsets.UTF_8));
            } finally {
                statement.execute("GRANT sharing_command_role TO " + COMMAND_LOGIN);
            }
        }
        String scratchPassword = UUID.randomUUID().toString();
        GenericContainer<?> scratchPostgres = new GenericContainer<>(DockerImageName.parse("postgres:16"))
                .withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", scratchPassword)
                .withEnv("POSTGRES_DB", "pa13_stale").withExposedPorts(5432)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
                .withStartupTimeout(Duration.ofMinutes(2));
        scratchPostgres.start();
        String scratchUrl = "jdbc:postgresql://" + scratchPostgres.getHost() + ":" + scratchPostgres.getMappedPort(5432) + "/pa13_stale";
        try {
            Flyway beforeV010 = Flyway.configure().dataSource(scratchUrl, "fixture_admin", scratchPassword)
                    .locations("classpath:db/migration").target("9").load();
            assertThat(beforeV010.migrate().migrationsExecuted).isEqualTo(9);
            provePostAclRollback(scratchUrl, scratchPassword);
            try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", scratchPassword);
                    Statement statement = connection.createStatement()) {
                statement.execute("CREATE ROLE stale_command_member NOLOGIN");
                statement.execute("GRANT stale_command_member TO sharing_command_role");
            }
            Flyway staleMigration = Flyway.configure().dataSource(scratchUrl, "fixture_admin", scratchPassword)
                    .locations("classpath:db/migration").target("10").load();
            assertThatThrownBy(staleMigration::migrate).isInstanceOf(RuntimeException.class);
            try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", scratchPassword);
                    Statement statement = connection.createStatement();
                    var rows = statement.executeQuery("select count(*) from flyway_schema_history where version='010' and success")) {
                rows.next();
                assertThat(rows.getInt(1)).isZero();
                assertV010NotPartiallyApplied(connection);
            }
            try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", scratchPassword);
                    Statement statement = connection.createStatement()) {
                statement.execute("REVOKE stale_command_member FROM sharing_command_role");
                statement.execute("ALTER ROLE sharing_command_role BYPASSRLS");
            }
            assertThatThrownBy(staleMigration::migrate).isInstanceOf(RuntimeException.class);
            try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", scratchPassword);
                    Statement statement = connection.createStatement();
                    var rows = statement.executeQuery("select count(*) from flyway_schema_history where version='010' and success")) {
                rows.next();
                assertThat(rows.getInt(1)).isZero();
                assertV010NotPartiallyApplied(connection);
            }
            try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", scratchPassword);
                    Statement statement = connection.createStatement()) {
                statement.execute("ALTER ROLE sharing_command_role NOBYPASSRLS");
                statement.execute("GRANT INSERT (id) ON evidence.evidence_event TO sharing_command_role");
                statement.execute("CREATE SEQUENCE evidence.pa13_stale_sequence");
                statement.execute("GRANT ALL ON SEQUENCE evidence.pa13_stale_sequence TO PUBLIC, sharing_role, sharing_command_role");
                statement.execute("CREATE PROCEDURE sharing.pa13_stale_procedure() LANGUAGE SQL AS 'SELECT 1'");
                statement.execute("GRANT EXECUTE ON PROCEDURE sharing.pa13_stale_procedure() TO PUBLIC, sharing_role, sharing_command_role");
            }
            assertThat(staleMigration.migrate().migrationsExecuted).isEqualTo(1);
            try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", scratchPassword);
                    Statement statement = connection.createStatement();
                    var rows = statement.executeQuery("select has_column_privilege('sharing_command_role','evidence.evidence_event','id','INSERT'), has_sequence_privilege('sharing_role','evidence.pa13_stale_sequence','USAGE'), has_sequence_privilege('sharing_command_role','evidence.pa13_stale_sequence','USAGE'), exists (select 1 from pg_class c join pg_namespace n on n.oid=c.relnamespace cross join lateral aclexplode(coalesce(c.relacl,acldefault('S',c.relowner))) a where n.nspname='evidence' and c.relname='pa13_stale_sequence' and a.grantee=0)")) {
                rows.next();
                assertThat(rows.getBoolean(1)).isFalse();
                assertThat(rows.getBoolean(2)).isFalse();
                assertThat(rows.getBoolean(3)).isFalse();
                assertThat(rows.getBoolean(4)).isFalse();
            }
            try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", scratchPassword);
                    Statement statement = connection.createStatement();
                    var rows = statement.executeQuery("select (select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace cross join lateral aclexplode(coalesce(p.proacl,acldefault('f',p.proowner))) x where n.nspname='sharing' and p.proname='pa13_stale_procedure' and x.grantee in (0,'sharing_role'::regrole,'sharing_command_role'::regrole)), (select count(*) from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname in ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') and has_function_privilege('sharing_command_role',p.oid,'EXECUTE'))")) {
                rows.next();
                assertThat(rows.getInt(1)).isZero();
                assertThat(rows.getInt(2)).isEqualTo(2);
            }
            try (Connection connection = DriverManager.getConnection(scratchUrl, "fixture_admin", scratchPassword);
                    Statement statement = connection.createStatement()) {
                statement.execute("GRANT sharing_owner TO sharing_role");
                assertMigrationRejectedAndRestored(staleMigration, scratchUrl, scratchPassword);
                statement.execute("REVOKE sharing_owner FROM sharing_role");
                statement.execute("CREATE ROLE stale_owner_login LOGIN");
                statement.execute("GRANT sharing_owner TO stale_owner_login");
                assertMigrationRejectedAndRestored(staleMigration, scratchUrl, scratchPassword);
                statement.execute("REVOKE sharing_owner FROM stale_owner_login");
                statement.execute("DROP ROLE stale_owner_login");
                statement.execute("ALTER ROLE sharing_owner LOGIN");
                assertMigrationRejectedAndRestored(staleMigration, scratchUrl, scratchPassword);
                statement.execute("ALTER ROLE sharing_owner NOLOGIN");
                statement.execute("ALTER ROLE sharing_role CREATEROLE");
                assertMigrationRejectedAndRestored(staleMigration, scratchUrl, scratchPassword);
                statement.execute("ALTER ROLE sharing_role NOCREATEROLE");
            }
            assertThat(staleMigration.migrate().migrationsExecuted).isEqualTo(1);
        } finally {
            scratchPostgres.stop();
        }
    }

    @Test
    void v010RoleAttributesAndMembershipContainment() throws Exception {
        try (Connection connection = admin(); Statement statement = connection.createStatement(); var rows = statement.executeQuery(
                "select rolcanlogin,rolsuper,rolcreatedb,rolcreaterole,rolinherit from pg_roles where rolname='sharing_command_role'")) {
            rows.next();
            assertThat(rows.getBoolean(1)).isFalse();
            assertThat(rows.getBoolean(2)).isFalse();
            assertThat(rows.getBoolean(3)).isFalse();
            assertThat(rows.getBoolean(4)).isFalse();
            assertThat(rows.getBoolean(5)).isFalse();
        }
        try (Connection connection = admin(); Statement statement = connection.createStatement(); var rows = statement.executeQuery(
                "select pg_has_role('" + QUERY_LOGIN + "','sharing_command_role','MEMBER'), pg_has_role('" + COMMAND_LOGIN + "','sharing_role','MEMBER'), pg_has_role('sharing_role','sharing_command_role','MEMBER')")) {
            rows.next();
            assertThat(rows.getBoolean(1)).isFalse();
            assertThat(rows.getBoolean(2)).isFalse();
            assertThat(rows.getBoolean(3)).isFalse();
        }
        try (Connection connection = login(QUERY_LOGIN); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            assertThatThrownBy(() -> statement.execute("SET ROLE sharing_command_role"))
                    .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
        }
    }

    @Test
    void v010QueryRoleExactCatalogAcl() throws Exception {
        try (Connection connection = admin()) {
            PostgresContainerIT.assertV009AclCatalog(connection);
        }
    }

    @Test
    void v010CommandRoleExactCatalogAcl() throws Exception {
        try (Connection connection = admin()) {
            PostgresContainerIT.assertV009AclCatalog(connection);
        }
    }

    @Test
    void v010QueryRolePositiveAndNegativeBehavior() throws Exception {
        try (Connection connection = login(QUERY_LOGIN); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET ROLE sharing_role");
            statement.executeQuery("select share_id,evidence_id,verification_mode,status,valid_from,expires_at,max_admissions,admissions_used,download_allowed,code_generation,revocation_generation,created_by_account_id,owner_organization_id from sharing.evidence_share_grant").close();
            denied(connection, "select capability_token_hash from sharing.evidence_share_grant", "42501");
            denied(connection, "select id from sharing.evidence_share_grant", "42501");
            denied(connection, "update sharing.evidence_share_grant set revision=revision+1", "42501");
            denied(connection, "select * from sharing.share_admission", "42501");
        }
    }

    @Test
    void v010CommandRolePositiveAndNegativeBehavior() throws Exception {
        UUID outboxId;
        UUID auditId;
        try (Connection connection = login(COMMAND_LOGIN); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET ROLE sharing_command_role");
            statement.executeQuery("select * from sharing.evidence_share_grant").close();
            statement.executeQuery("select * from sharing.share_admission").close();
            try (ResultSet result = statement.executeQuery("select platform.outbox_append('SHARE_ACCESS','pa13-command-role-probe','SHARE_ACCESS',gen_random_uuid(),'SHARE_ACCESS_PROBE','{\"probe\":true}'::jsonb,null,'" + "a".repeat(64) + "')")) {
                assertThat(result.next()).isTrue();
                outboxId = result.getObject(1, UUID.class);
            }
            try (ResultSet result = statement.executeQuery("select audit.append_fact(now(),'SYSTEM',null,'SHARE_ACCESS_PROBE','SYSTEM',null,null,null,'{\"probe\":true}'::jsonb,'pa13-command-role-probe',null)")) {
                assertThat(result.next()).isTrue();
                auditId = result.getObject(1, UUID.class);
            }
            denied(connection, "update sharing.evidence_share_grant set max_admissions=max_admissions", "42501");
            denied(connection, "delete from sharing.evidence_share_grant", "42501");
            denied(connection, "truncate sharing.evidence_share_grant", "42501");
            denied(connection, "select * from ivr.call", "42501");
            connection.rollback();
        }
        try (Connection connection = admin(); PreparedStatement outbox = connection.prepareStatement("select exists (select 1 from platform.outbox_event where id = ?)" );
             PreparedStatement audit = connection.prepareStatement("select exists (select 1 from audit.audit_fact where id = ?)")) {
            outbox.setObject(1, outboxId);
            audit.setObject(1, auditId);
            try (ResultSet result = outbox.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBoolean(1)).isFalse();
            }
            try (ResultSet result = audit.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getBoolean(1)).isFalse();
            }
        }
    }

    @Test
    void v010PrivilegeFailuresAre42501AndReadOnlyAuthorizedWriteIs25006() throws Exception {
        try (Connection connection = login(QUERY_LOGIN); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET ROLE sharing_role");
            denied(connection, "create table sharing.probe(id int)", "42501");
        }
        try (Connection connection = login(COMMAND_LOGIN); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET TRANSACTION READ ONLY");
            statement.execute("SET LOCAL ROLE sharing_command_role");
            denied(connection, "update sharing.evidence_share_grant set revision=revision+1", "25006");
            connection.rollback();
        }
    }

    @Test
    void v010LegacyMigrationDigestsRemainCanonical() throws Exception {
        List<String> paths = List.of("V001__platform_schemas_roles_idempotency_outbox_audit.sql", "V002__identity_organization_and_employee.sql", "V003__subject_number_and_claim.sql", "V004__catalog_order_and_payment.sql", "V005__ivr_task_session_call.sql", "V006__trial_entitlement_issuance_reservation_usage.sql", "V007__activation_and_consent.sql", "V008__evidence_media.sql", "V009__sharing.sql");
        List<String> expected = List.of("946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045", "534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5", "1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1", "a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc", "2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310", "830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475", "bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659", "2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e", "ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae");
        for (int index = 0; index < paths.size(); index++) {
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("db/migration/" + paths.get(index))) {
                byte[] source = input.readAllBytes();
                String normalized = new String(source, StandardCharsets.UTF_8).replace("\r\n", "\n");
                assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8))))
                        .isEqualTo(expected.get(index));
            }
        }
    }
}
