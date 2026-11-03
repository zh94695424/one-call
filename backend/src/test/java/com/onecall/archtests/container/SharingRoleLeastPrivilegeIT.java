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

    @BeforeAll
    static void start() throws Exception {
        POSTGRES.start();
        url = "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432) + "/sharing_least_privilege";
        flyway = Flyway.configure().dataSource(url, "fixture_admin", PASSWORD).locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(10);
        try (Connection admin = admin(); Statement statement = admin.createStatement()) {
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
        Flyway failing = Flyway.configure().dataSource(scratchUrl, "fixture_admin", password).locations("filesystem:" + migrationDir).load();
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
        assertThat(flyway.info().applied()).hasSize(10);
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
                    .locations("classpath:db/migration").load();
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
        try (Connection connection = login(COMMAND_LOGIN); Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET ROLE sharing_command_role");
            statement.executeQuery("select * from sharing.evidence_share_grant").close();
            statement.executeQuery("select * from sharing.share_admission").close();
            denied(connection, "update sharing.evidence_share_grant set max_admissions=max_admissions", "42501");
            denied(connection, "delete from sharing.evidence_share_grant", "42501");
            denied(connection, "truncate sharing.evidence_share_grant", "42501");
            denied(connection, "select * from ivr.call", "42501");
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
