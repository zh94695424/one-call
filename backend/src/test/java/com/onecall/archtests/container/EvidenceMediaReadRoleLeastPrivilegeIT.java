package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.EvidenceMediaReadIT.Fixture.execute;
import static com.onecall.archtests.container.EvidenceMediaReadIT.Fixture.strings;

import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.Result;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EvidenceMediaReadRoleLeastPrivilegeIT {
    private static final EvidenceMediaReadIT.Fixture BASE = new EvidenceMediaReadIT.Fixture();
    private static final String ROLE = "evidence_media_reader_role";
    private static final Map<String, List<String>> COLUMNS = Map.of(
            "evidence_record", List.of("id", "source_type", "source_ref_id", "status"),
            "evidence_artifact", List.of("evidence_id", "media_object_id", "artifact_role"),
            "media_object", List.of("id", "sha256", "size_bytes", "mime_type"),
            "recording", List.of("id", "media_object_id", "status", "validated_duration"));
    private static Map<String, String> before;

    @BeforeAll static void start() throws Exception { before = hashes(); BASE.start(); BASE.seed(); }
    @AfterAll static void stop() { BASE.close(); }

    @Test
    void roleAttributesPasswordTopologyAndV013HistoryAreExact() throws Exception {
        try (Connection connection = BASE.admin()) {
            assertThat(strings(connection, "SELECT rolcanlogin||'|'||rolsuper||'|'||rolcreatedb||'|'||rolcreaterole||'|'||rolinherit||'|'||rolreplication||'|'||rolbypassrls||'|'||rolconnlimit||'|'||(rolvaliduntil='infinity'::timestamptz)||'|'||(rolconfig IS NULL) FROM pg_roles WHERE rolname='" + ROLE + "'"))
                    .containsExactly("true|false|false|false|false|false|false|-1|true|true");
            assertThat(strings(connection, "SELECT (rolpassword IS NULL) FROM pg_authid WHERE rolname='" + ROLE + "'"))
                    .containsExactly("t");
            assertThat(strings(connection, "SELECT member::text FROM pg_auth_members WHERE member='" + ROLE + "'::regrole OR roleid='" + ROLE + "'::regrole")).isEmpty();
            assertThat(strings(connection, "SELECT setrole::text FROM pg_db_role_setting WHERE setrole='" + ROLE + "'::regrole")).isEmpty();
            assertThat(strings(connection, "SELECT version||'|'||description||'|'||script||'|'||type||'|'||success FROM public.flyway_schema_history WHERE version IN ('012','013') ORDER BY installed_rank"))
                    .containsExactly("012|sharing admission least privilege|V012__sharing_admission_least_privilege.sql|SQL|true",
                            "013|evidence media reader role|V013__evidence_media_reader_role.sql|SQL|true");
            assertThat(strings(connection, "SELECT current_setting('server_version_num')::integer BETWEEN 160000 AND 169999")).containsExactly("t");
        }
        assertThat(BASE.flyway.info().applied()).hasSize(16);
    }

    @Test
    void exactSixteenDependenciesAndOwnerGrantedColumnSchemaAclsAreEnumerated() throws Exception {
        try (Connection connection = BASE.admin()) { assertExactAcls(connection); }
    }

    @Test
    void actualLoginSelectsEveryGrantedColumnAndResolvesMatchingObject() throws Exception {
        BASE.password(ROLE, BASE.password);
        try (Connection connection = BASE.login(ROLE)) {
            assertThat(strings(connection, "SELECT current_user||'|'||session_user")).containsExactly(ROLE + "|" + ROLE);
            for (var entry : COLUMNS.entrySet()) {
                try (var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT " + String.join(",", entry.getValue()) + " FROM evidence." + entry.getKey())) {
                    assertThat(rows.getMetaData().getColumnCount()).isEqualTo(entry.getValue().size());
                    assertThat(rows.next()).isTrue();
                }
            }
        } finally { BASE.password(ROLE, null); }
        BASE.withReader(reader -> assertThat(reader.resolve(BASE.evidence)).isInstanceOf(Result.Found.class));
    }

    @Test
    void actualLoginDeniesAdjacentReadsWritesDdlSequencesFunctionsAndEscalation() throws Exception {
        List<String> denied = new ArrayList<>();
        try (Connection admin = BASE.admin()) {
            for (String column : strings(admin, "SELECT relation.relname||'.'||attribute.attname FROM pg_attribute attribute JOIN pg_class relation ON relation.oid=attribute.attrelid WHERE relation.relnamespace='evidence'::regnamespace AND relation.relkind='r' AND attribute.attnum>0 AND NOT attribute.attisdropped ORDER BY 1")) {
                String[] parts = column.split("\\.");
                if (!COLUMNS.getOrDefault(parts[0], List.of()).contains(parts[1])) denied.add("SELECT " + parts[1] + " FROM evidence." + parts[0]);
            }
            for (String table : List.of("media_object", "recording", "evidence_record", "evidence_artifact", "evidence_event")) {
                denied.add("INSERT INTO evidence." + table + " DEFAULT VALUES");
                denied.add("UPDATE evidence." + table + " SET id=id");
                denied.add("DELETE FROM evidence." + table);
                denied.add("TRUNCATE evidence." + table);
                denied.add("SELECT id FROM evidence." + table + " FOR UPDATE");
            }
            denied.add("SELECT id FROM sharing.evidence_share_grant");
            denied.add("CREATE TABLE evidence.pa19a_forbidden(id integer)");
            denied.add("CREATE SCHEMA pa19a_forbidden");
            denied.add("CREATE TEMP TABLE pa19a_forbidden(id integer)");
            for (String role : List.of("evidence_owner", "evidence_role", "sharing_role", "sharing_command_role", "sharing_admission_role", "fixture_admin")) denied.add("SET ROLE " + role);
            for (String function : strings(admin, "SELECT oid::regprocedure::text FROM pg_proc WHERE pronamespace='evidence'::regnamespace ORDER BY 1")) denied.add("SELECT " + function);
            execute(admin, "CREATE SEQUENCE evidence.pa19a_sequence_probe");
            denied.add("SELECT nextval('evidence.pa19a_sequence_probe')");
            assertThat(strings(admin, "SELECT has_database_privilege('" + ROLE + "',current_database(),'CREATE')||'|'||has_database_privilege('" + ROLE + "',current_database(),'TEMP')")).containsExactly("false|false");
            assertThat(strings(admin, "SELECT nspname FROM pg_namespace WHERE nspname <> 'information_schema' AND nspname !~ '^pg_' AND (has_schema_privilege('" + ROLE + "',oid,'CREATE') OR (nspname <> 'evidence' AND has_schema_privilege('" + ROLE + "',oid,'USAGE')))")).isEmpty();
            assertThat(strings(admin, "SELECT relation.oid::text FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_' AND relation.relkind IN ('r','p','v','m','f') AND has_table_privilege('" + ROLE + "',relation.oid,'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')")).isEmpty();
        }
        BASE.password(ROLE, BASE.password);
        try (Connection connection = BASE.login(ROLE)) {
            for (String sql : denied) denied(connection, sql);
        } finally {
            BASE.password(ROLE, null);
            try (Connection admin = BASE.admin()) { execute(admin, "DROP SEQUENCE evidence.pa19a_sequence_probe"); }
        }
        try (Connection admin = BASE.admin()) {
            for (String privilege : List.of("SELECT WITH GRANT OPTION", "INSERT", "UPDATE", "REFERENCES")) {
                assertThat(strings(admin, "SELECT has_column_privilege('" + ROLE + "','evidence.media_object','id','" + privilege + "')")).containsExactly("f");
            }
        }
        BASE.password("sharing_admission_role", BASE.password);
        try (Connection connection = BASE.login("sharing_admission_role")) {
            for (String table : List.of("media_object", "recording", "evidence_artifact")) denied(connection, "SELECT id FROM evidence." + table);
            try (var statement = connection.createStatement()) { statement.executeQuery("SELECT id,subject_id,status,identity_at_capture FROM evidence.evidence_record").close(); }
        } finally { BASE.password("sharing_admission_role", null); }
    }

    @Test
    void zeroAclCompatibleRoleAndTwiceReplayedGrantedStateAreAccepted() throws Exception {
        String migration = EvidenceMediaReadIT.Fixture.migration();
        try (Connection connection = BASE.admin()) {
            connection.setAutoCommit(false);
            for (var entry : COLUMNS.entrySet()) execute(connection, "REVOKE SELECT (" + String.join(",", entry.getValue()) + ") ON evidence." + entry.getKey() + " FROM " + ROLE);
            execute(connection, "REVOKE USAGE ON SCHEMA evidence FROM " + ROLE);
            assertThat(dependencies(connection)).isEmpty();
            execute(connection, migration);
            assertExactAcls(connection);
            connection.rollback();
            for (int replay = 0; replay < 2; replay++) {
                execute(connection, migration);
                assertExactAcls(connection);
                connection.commit();
            }
        }
        assertThat(BASE.flyway.migrate().migrationsExecuted).isZero();
        try (Connection connection = BASE.admin()) { assertExactAcls(connection); }
    }

    @Test
    void incompatibleAttributesMembershipsSettingsAndAnyUnexpectedDependencyFailClosed() throws Exception {
        List<String> injections = List.of("ALTER ROLE " + ROLE + " SUPERUSER", "ALTER ROLE " + ROLE + " INHERIT",
                "ALTER ROLE " + ROLE + " NOLOGIN", "ALTER ROLE " + ROLE + " CREATEDB", "ALTER ROLE " + ROLE + " CREATEROLE",
                "ALTER ROLE " + ROLE + " REPLICATION", "ALTER ROLE " + ROLE + " BYPASSRLS", "ALTER ROLE " + ROLE + " CONNECTION LIMIT 1",
                "ALTER ROLE " + ROLE + " VALID UNTIL '2030-01-01'", "ALTER ROLE " + ROLE + " PASSWORD 'synthetic-incompatible'",
                "ALTER ROLE " + ROLE + " SET search_path=evidence", "GRANT evidence_role TO " + ROLE,
                "GRANT " + ROLE + " TO evidence_role", "GRANT CREATE ON DATABASE evidence_media_read TO " + ROLE,
                "GRANT TEMP ON DATABASE evidence_media_read TO " + ROLE,
                "GRANT USAGE ON SCHEMA sharing TO " + ROLE,
                "GRANT SELECT ON evidence.media_object TO " + ROLE,
                "GRANT SELECT (crypto_key_ref) ON evidence.media_object TO " + ROLE,
                "SET LOCAL ROLE evidence_owner; REVOKE SELECT (sha256) ON evidence.media_object FROM " + ROLE + "; GRANT SELECT (crypto_key_ref) ON evidence.media_object TO " + ROLE + "; RESET ROLE",
                "GRANT UPDATE (id) ON evidence.media_object TO " + ROLE,
                "SET LOCAL ROLE evidence_owner; GRANT SELECT (id) ON evidence.media_object TO " + ROLE + " WITH GRANT OPTION; RESET ROLE",
                "GRANT EXECUTE ON FUNCTION evidence.guard_recording() TO " + ROLE,
                "GRANT SELECT (id) ON evidence.media_object TO PUBLIC",
                "GRANT USAGE ON SCHEMA evidence TO PUBLIC",
                "GRANT EXECUTE ON FUNCTION evidence.guard_recording() TO PUBLIC",
                "CREATE SCHEMA pa19a_owned AUTHORIZATION " + ROLE,
                "ALTER DEFAULT PRIVILEGES FOR ROLE evidence_owner IN SCHEMA evidence GRANT SELECT ON TABLES TO " + ROLE,
                "ALTER DEFAULT PRIVILEGES FOR ROLE evidence_owner IN SCHEMA evidence GRANT SELECT ON TABLES TO PUBLIC",
                "SET LOCAL ROLE evidence_owner; REVOKE SELECT (sha256) ON evidence.media_object FROM " + ROLE + "; RESET ROLE");
        try (Connection connection = BASE.admin()) {
            connection.setAutoCommit(false);
            for (String injection : injections) rejected(connection, injection);
            assertExactAcls(connection);
            connection.rollback();
        }
    }

    @Test
    void failedDuplicateIncompatibleOrMissingRequiredHistoryIsRejected() throws Exception {
        try (Connection connection = BASE.admin()) {
            connection.setAutoCommit(false);
            for (String injection : List.of("UPDATE public.flyway_schema_history SET success=false WHERE version='012'",
                    "DELETE FROM public.flyway_schema_history WHERE version='012'",
                    "UPDATE public.flyway_schema_history SET success=false WHERE version='013'",
                    "UPDATE public.flyway_schema_history SET script='wrong.sql' WHERE version='013'",
                    "UPDATE public.flyway_schema_history SET description='wrong' WHERE version='013'",
                    "UPDATE public.flyway_schema_history SET type='BASELINE' WHERE version='013'",
                    "UPDATE public.flyway_schema_history SET version='13' WHERE version='013'",
                    "INSERT INTO public.flyway_schema_history SELECT installed_rank+1000,version,description,type,script,checksum,installed_by,installed_on,execution_time,success FROM public.flyway_schema_history WHERE version='012'",
                    "INSERT INTO public.flyway_schema_history SELECT installed_rank+1000,version,description,type,script,checksum,installed_by,installed_on,execution_time,success FROM public.flyway_schema_history WHERE version='013'")) rejected(connection, injection);
            execute(connection, "DELETE FROM public.flyway_schema_history WHERE version='013'");
            execute(connection, EvidenceMediaReadIT.Fixture.migration());
            assertExactAcls(connection);
            connection.rollback();
        }
    }

    @Test
    void privilegeOnlyMigrationIsAtomicAndV001ThroughV012RemainByteIdentical() throws Exception {
        assertThat(hashes()).containsExactlyInAnyOrderEntriesOf(before);
        String migration = EvidenceMediaReadIT.Fixture.migration();
        assertThat(migration).doesNotContain("CREATE TABLE", "CREATE FUNCTION", "CREATE SCHEMA", "CREATE SEQUENCE", "REVOKE", "object_key", "object_version_id", "bucket");
        try (Connection connection = BASE.admin()) {
            connection.setAutoCommit(false);
            List<String> beforeDependencies = dependencies(connection);
            for (var entry : COLUMNS.entrySet()) execute(connection, "REVOKE SELECT (" + String.join(",", entry.getValue()) + ") ON evidence." + entry.getKey() + " FROM " + ROLE);
            execute(connection, "REVOKE USAGE ON SCHEMA evidence FROM " + ROLE);
            execute(connection, "DROP ROLE " + ROLE);
            var beforeMigration = connection.setSavepoint();
            assertThatThrownBy(() -> execute(connection, migration + "\nSELECT 1/0;"))
                    .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("22012"));
            connection.rollback(beforeMigration);
            assertThat(strings(connection, "SELECT rolname FROM pg_roles WHERE rolname='" + ROLE + "'")).isEmpty();
            connection.rollback();
            assertThat(dependencies(connection)).containsExactlyElementsOf(beforeDependencies);
            assertExactAcls(connection);
        }
        assertThat(BASE.flyway.migrate().migrationsExecuted).isZero();
    }

    private static void denied(Connection connection, String sql) {
        assertThatThrownBy(() -> execute(connection, sql)).as(sql)
                .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
    }

    private static void rejected(Connection connection, String injection) throws Exception {
        List<String> beforeDependencies = dependencies(connection);
        var savepoint = connection.setSavepoint();
        execute(connection, injection);
        assertThatThrownBy(() -> execute(connection, EvidenceMediaReadIT.Fixture.migration())).as(injection)
                .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("P0001"));
        connection.rollback(savepoint);
        connection.releaseSavepoint(savepoint);
        assertThat(dependencies(connection)).containsExactlyElementsOf(beforeDependencies);
    }

    private static List<String> dependencies(Connection connection) throws SQLException {
        return strings(connection, "SELECT dependency.classid::regclass::text||'|'||CASE WHEN dependency.classid='pg_namespace'::regclass THEN namespace.nspname ELSE relation.relname||'.'||attribute.attname END||'|'||dependency.deptype::text||'|'||(dependency.dbid=(SELECT oid FROM pg_database WHERE datname=current_database())) FROM pg_shdepend dependency LEFT JOIN pg_class relation ON dependency.classid='pg_class'::regclass AND relation.oid=dependency.objid LEFT JOIN pg_attribute attribute ON attribute.attrelid=relation.oid AND attribute.attnum=dependency.objsubid LEFT JOIN pg_namespace namespace ON dependency.classid='pg_namespace'::regclass AND namespace.oid=dependency.objid WHERE dependency.refclassid='pg_authid'::regclass AND dependency.refobjid='" + ROLE + "'::regrole ORDER BY 1");
    }

    private static void assertExactAcls(Connection connection) throws SQLException {
        List<String> expectedDependencies = new ArrayList<>(List.of("pg_namespace|evidence|a|true"));
        List<String> expectedColumns = new ArrayList<>();
        COLUMNS.forEach((table, columns) -> columns.forEach(column -> {
            expectedDependencies.add("pg_class|" + table + "." + column + "|a|true");
            expectedColumns.add(table + "." + column + "|evidence_owner|" + ROLE + "|SELECT|false");
        }));
        expectedDependencies.sort(String::compareTo); expectedColumns.sort(String::compareTo);
        assertThat(expectedDependencies).hasSize(16);
        assertThat(dependencies(connection)).containsExactlyElementsOf(expectedDependencies);
        assertThat(strings(connection, "SELECT relation.relname||'.'||attribute.attname||'|'||acl.grantor::regrole::text||'|'||acl.grantee::regrole::text||'|'||acl.privilege_type||'|'||acl.is_grantable FROM pg_attribute attribute JOIN pg_class relation ON relation.oid=attribute.attrelid CROSS JOIN LATERAL aclexplode(attribute.attacl) acl WHERE acl.grantee='" + ROLE + "'::regrole ORDER BY 1"))
                .containsExactlyElementsOf(expectedColumns);
        assertThat(strings(connection, "SELECT namespace.nspname||'|'||acl.grantor::regrole::text||'|'||acl.grantee::regrole::text||'|'||acl.privilege_type||'|'||acl.is_grantable FROM pg_namespace namespace CROSS JOIN LATERAL aclexplode(namespace.nspacl) acl WHERE acl.grantee='" + ROLE + "'::regrole ORDER BY 1"))
                .containsExactly("evidence|evidence_owner|" + ROLE + "|USAGE|false");
        assertThat(strings(connection, "SELECT oid::text FROM pg_class CROSS JOIN LATERAL aclexplode(relacl) acl WHERE acl.grantee='" + ROLE + "'::regrole")).isEmpty();
        assertThat(strings(connection, "SELECT oid::text FROM pg_default_acl CROSS JOIN LATERAL aclexplode(defaclacl) acl WHERE acl.grantee IN (0,'" + ROLE + "'::regrole) OR acl.grantor='" + ROLE + "'::regrole")).isEmpty();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                "SELECT relation.relname,attribute.attname,has_column_privilege('" + ROLE + "',relation.oid,attribute.attnum,'SELECT'),has_column_privilege('" + ROLE + "',relation.oid,attribute.attnum,'INSERT,UPDATE,REFERENCES'),has_column_privilege('" + ROLE + "',relation.oid,attribute.attnum,'SELECT WITH GRANT OPTION') FROM pg_attribute attribute JOIN pg_class relation ON relation.oid=attribute.attrelid WHERE relation.relnamespace='evidence'::regnamespace AND relation.relkind IN ('r','p','v','m','f') AND attribute.attnum>0 AND NOT attribute.attisdropped")) {
            int selected = 0;
            while (rows.next()) {
                boolean expected = COLUMNS.getOrDefault(rows.getString(1), List.of()).contains(rows.getString(2));
                assertThat(rows.getBoolean(3)).isEqualTo(expected);
                assertThat(rows.getBoolean(4)).isFalse();
                assertThat(rows.getBoolean(5)).isFalse();
                if (expected) selected++;
            }
            assertThat(selected).isEqualTo(15);
        }
        assertThat(strings(connection, "SELECT oid::text FROM pg_proc WHERE pronamespace='evidence'::regnamespace AND has_function_privilege('" + ROLE + "',oid,'EXECUTE')")).isEmpty();
        assertThat(strings(connection, "SELECT schemaname||'.'||sequencename FROM pg_sequences WHERE has_sequence_privilege('" + ROLE + "',format('%I.%I',schemaname,sequencename),'USAGE,SELECT,UPDATE')")).isEmpty();
        assertThat(strings(connection, "SELECT oid::text FROM pg_class WHERE relnamespace='evidence'::regnamespace AND relkind='S'")).isEmpty();
    }

    private static Map<String, String> hashes() throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        try (var paths = Files.list(Path.of("src/main/resources/db/migration"))) {
            for (Path path : paths.filter(path -> path.getFileName().toString().matches("V0(?:0[1-9]|1[0-2])__.*\\.sql")).sorted().toList()) {
                result.put(path.getFileName().toString(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
            }
        }
        assertThat(result).hasSize(12);
        return result;
    }
}
