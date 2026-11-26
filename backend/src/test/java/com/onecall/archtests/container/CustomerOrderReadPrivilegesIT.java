package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.Transferable;

public class CustomerOrderReadPrivilegesIT {
    static final Path SCRIPT = Path.of("src/main/resources/db/migration/V020__customer_order_read_privileges.sql");
    CustomerBusinessNumbersFixture fixture;
    static Set<String> orderReadDeltaOracle() {
        Set<String> result = new TreeSet<>();
        for (String column : List.of("id", "order_no", "channel", "catalog_version_id", "catalog_offer_id",
                "purchase_consent_id", "amount_cents", "currency", "payment_status", "snapshot", "snapshot_sha256",
                "created_at", "paid_at", "closed_at"))
            assertThat(result.add("customer_iam_role|column|catalog_order.customer_order|" + column + "|SELECT|catalog_order_owner|false")).isTrue();
        for (String column : List.of("order_id", "status"))
            assertThat(result.add("customer_iam_role|column|entitlement_metering.entitlement_issuance|" + column + "|SELECT|entitlement_metering_owner|false")).isTrue();
        result.add("customer_iam_role|schema|entitlement_metering||USAGE|entitlement_metering_owner|false");
        assertThat(result).hasSize(17);
        return Set.copyOf(result);
    }
    static Set<String> current449Oracle() {
        Set<String> baseline = CustomerCatalogOfferReadPrivilegesIT.current432Oracle();
        Set<String> delta = orderReadDeltaOracle();
        assertThat(baseline).hasSize(432).doesNotContainAnyElementsOf(delta);
        Set<String> result = new TreeSet<>(baseline);
        result.addAll(delta);
        assertThat(result).hasSize(449);
        assertThat(result.stream().filter(tuple -> tuple.startsWith("customer_iam_role|"))).hasSize(92);
        assertThat(result.stream().filter(tuple -> !tuple.startsWith("customer_iam_role|"))).hasSize(357);
        return Set.copyOf(result);
    }
    static java.util.Map<String, List<String>> approvedColumns() {
        java.util.Map<String, List<String>> result = new java.util.TreeMap<>();
        for (String tuple : current449Oracle()) {
            String[] fields = tuple.split("\\|", -1);
            if (fields[0].equals("customer_iam_role") && fields[1].equals("column")) {
                var columns = new java.util.ArrayList<>(result.getOrDefault(fields[2], List.of()));
                columns.add(fields[3]);
                result.put(fields[2], List.copyOf(columns));
            }
        }
        assertThat(result.values().stream().mapToInt(List::size).sum()).isEqualTo(85);
        return result;
    }
    @BeforeEach void start() throws Exception { fixture = new CustomerBusinessNumbersFixture(); fixture.start("019"); }
    @AfterEach void stop() { if (fixture != null) fixture.close(); }
    Set<String> acls(Connection connection) throws Exception {
        Set<String> result = new TreeSet<>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(CustomerCatalogOfferReadPrivilegesIT.ACL)) {
            while (rows.next()) assertThat(result.add(rows.getString(1))).isTrue();
        }
        return result;
    }
    void upgrade() {
        var flyway = Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password)
                .locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
    void historicalProvision() throws Exception {
        fixture.postgres.copyFileToContainer(Transferable.of(CustomerCatalogOfferReadPrivilegesIT.historicalV019Pa25Script()
                .getBytes(StandardCharsets.UTF_8)), "/tmp/pa42-historical.sql");
        for (String mode : List.of("apply", "check"))
            assertThat(fixture.postgres.execInContainer("psql", "-X", "-w", "-v", "ON_ERROR_STOP=1", "-U", "fixture_admin",
                    "-d", "pa27", "-v", "mode=" + mode, "-v", "sharing_login=pa27_query", "-v", "customer_login=pa27_customer",
                    "-v", "command_login=pa27_command", "-v", "operations_login=pa36_operations", "-f", "/tmp/pa42-historical.sql").getExitCode()).isZero();
    }
    void denied(Connection connection, String sql) throws Exception {
        var savepoint = connection.setSavepoint();
        try {
            assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class,
                    failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
        } finally { connection.rollback(savepoint); }
    }
    @Test void freshAndV019UpgradeReachTwentyMigrationsAnd449Tuples() throws Exception {
        try (var connection = fixture.admin()) { assertThat(acls(connection)).isEqualTo(CustomerCatalogOfferReadPrivilegesIT.current432Oracle()); }
        upgrade();
        try (var connection = fixture.admin()) {
            Set<String> complete = acls(connection);
            assertThat(complete).isEqualTo(current449Oracle());
            complete.removeAll(CustomerCatalogOfferReadPrivilegesIT.current432Oracle());
            assertThat(complete).isEqualTo(orderReadDeltaOracle());
            assertThat(one(connection, "SELECT count(*) FROM flyway_schema_history WHERE success")).isEqualTo("20");
            assertThat(one(connection, "SELECT count(*) FROM flyway_schema_history WHERE version='020'")).isEqualTo("1");
        }
        fixture.close(); fixture = new CustomerBusinessNumbersFixture(); fixture.start(null);
        try (var connection = fixture.admin()) { assertThat(acls(connection)).isEqualTo(current449Oracle()); }
    }
    @Test void v020HasOnlyExactSeventeenTupleDelta() throws Exception {
        historicalProvision(); upgrade(); fixture.provision();
        try (var connection = fixture.admin()) { assertThat(acls(connection)).isEqualTo(current449Oracle()); }
        CustomerCatalogOfferReadPrivilegesIT.historicalV018Pa25Script();
        String current = Files.readString(Path.of("ops/postgresql/pa25-runtime-identities.sql"));
        String historical = CustomerCatalogOfferReadPrivilegesIT.historicalV019Pa25Script();
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        String marker = "$frozen$";
        int currentStart = current.indexOf(marker) + marker.length();
        int historicalStart = historical.indexOf(marker) + marker.length();
        var currentTuples = new java.util.ArrayList<tools.jackson.databind.JsonNode>();
        var oldTuples = new java.util.ArrayList<tools.jackson.databind.JsonNode>();
        mapper.readTree(current.substring(currentStart, current.indexOf(marker, currentStart))).forEach(currentTuples::add);
        mapper.readTree(historical.substring(historicalStart, historical.indexOf(marker, historicalStart))).forEach(oldTuples::add);
        assertThat(currentTuples).hasSize(449);
        assertThat(oldTuples).hasSize(432);
        assertThat(currentTuples.subList(0, 432)).isEqualTo(oldTuples);
    }
    @Test void v020ReplaysOldAndCompleteStatesExactly() throws Exception {
        historicalProvision();
        String business = fixture.businessSnapshot();
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false);
            execute(connection, Files.readString(SCRIPT)); connection.commit();
            Set<String> first = acls(connection);
            execute(connection, Files.readString(SCRIPT)); connection.commit();
            assertThat(acls(connection)).isEqualTo(first).isEqualTo(current449Oracle());
            assertThat(one(connection, "SELECT current_user||'|'||session_user")).isEqualTo("fixture_admin|fixture_admin");
            assertThat(one(connection, "SELECT count(*) FROM flyway_schema_history")).isEqualTo("19");
        }
        upgrade();
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false); execute(connection, Files.readString(SCRIPT)); connection.commit();
            assertThat(acls(connection)).isEqualTo(current449Oracle());
        }
        assertThat(fixture.businessSnapshot()).isEqualTo(business);
    }
    @Test void customerRoleCanReadOnlyApprovedColumns() throws Exception {
        upgrade(); fixture.provision();
        try (var connection = fixture.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            execute(connection, "SELECT id,order_no,account_id,owner_organization_id,subject_id,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,created_at,paid_at,closed_at FROM catalog_order.customer_order");
            execute(connection, "SELECT order_id,status FROM entitlement_metering.entitlement_issuance");
            for (String column : List.of("revision", "version_token", "idempotency_key", "request_fingerprint"))
                denied(connection, "SELECT " + column + " FROM catalog_order.customer_order");
            for (String sql : List.of("SELECT * FROM catalog_order.customer_order", "SELECT * FROM entitlement_metering.entitlement_issuance",
                    "SELECT id FROM entitlement_metering.entitlement_issuance", "UPDATE catalog_order.customer_order SET payment_status='UNKNOWN'",
                    "DELETE FROM entitlement_metering.entitlement_issuance", "SET LOCAL ROLE catalog_order_owner", "SET LOCAL ROLE catalog_order_role",
                    "CREATE TABLE catalog_order.pa42_forbidden(id integer)", "CREATE SCHEMA pa42_forbidden",
                    "ALTER DEFAULT PRIVILEGES FOR ROLE catalog_order_owner GRANT SELECT ON TABLES TO customer_iam_role",
                    "SELECT grant_id,issued_at FROM entitlement_metering.entitlement_issuance",
                    "SELECT * FROM entitlement_metering.entitlement_grant",
                    "INSERT INTO catalog_order.customer_order DEFAULT VALUES"))
                denied(connection, sql);
            var publicGrant = connection.setSavepoint();
            execute(connection, "GRANT SELECT(id) ON catalog_order.customer_order TO PUBLIC");
            assertThat(one(connection, "SELECT has_column_privilege(0, 'catalog_order.customer_order', 'id', 'SELECT')"))
                    .as("a non-owner GRANT without grant option is a warning/no-op on PostgreSQL 16")
                    .isEqualTo("f");
            connection.rollback(publicGrant);
            assertThat(one(connection, "SELECT has_column_privilege(0, 'catalog_order.customer_order', 'id', 'SELECT')")).isEqualTo("f");
            try (var admin = fixture.admin()) {
                assertThat(one(admin, """
                        SELECT EXISTS (
                            SELECT 1
                            FROM pg_class table_ref
                            JOIN pg_namespace schema_ref ON schema_ref.oid = table_ref.relnamespace
                            JOIN pg_attribute column_ref ON column_ref.attrelid = table_ref.oid
                            CROSS JOIN LATERAL aclexplode(COALESCE(column_ref.attacl, '{}'::aclitem[])) acl
                            WHERE schema_ref.nspname = 'catalog_order'
                              AND table_ref.relname = 'customer_order'
                              AND column_ref.attname = 'id'
                              AND acl.grantee = 0
                              AND acl.privilege_type = 'SELECT'
                        )""")).isEqualTo("f");
                assertThat(acls(admin)).isEqualTo(current449Oracle());
            }
            connection.commit();
            assertThat(one(connection, "SELECT current_user||'|'||session_user")).isEqualTo("pa27_customer|pa27_customer");
        }
    }
    String snapshot(Connection connection) throws Exception {
        var method = CustomerCatalogOfferReadPrivilegesIT.class.getDeclaredMethod("snapshot", Connection.class);
        method.setAccessible(true);
        return (String) method.invoke(new CustomerCatalogOfferReadPrivilegesIT(), connection);
    }
    void rejects(String mutation, String category) throws Exception {
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false);
            execute(connection, mutation);
            String role = one(connection, "SELECT quote_ident(current_user)");
            execute(connection, "RESET ROLE");
            String before = snapshot(connection);
            execute(connection, "SET LOCAL ROLE " + role);
            var savepoint = connection.setSavepoint();
            assertThatThrownBy(() -> execute(connection, Files.readString(SCRIPT))).isInstanceOfSatisfying(SQLException.class, failure -> {
                assertThat(failure.getSQLState()).isEqualTo("P0001");
                assertThat(failure.getMessage()).contains("V020_" + category + "_REJECTED").doesNotContain(fixture.password);
            });
            connection.rollback(savepoint);
            execute(connection, "RESET ROLE");
            assertThat(snapshot(connection)).isEqualTo(before);
            connection.rollback();
        }
    }
    @Test void v020RejectsPartialExtraWrongGrantorAndGrantOptionStatesAtomically() throws Exception {
        List<String> ordered = orderReadDeltaOracle().stream().sorted().toList();
        for (int size = 1; size < 17; size++) {
            StringBuilder mutation = new StringBuilder();
            for (String tuple : ordered.subList(0, size)) {
                String[] fields = tuple.split("\\|", -1);
                mutation.append("SET LOCAL ROLE ").append(fields[5]).append("; GRANT ")
                        .append(fields[4]).append(fields[1].equals("column") ? "(" + fields[3] + ") ON " : " ON SCHEMA ")
                        .append(fields[2]).append(" TO customer_iam_role; RESET ROLE;");
            }
            rejects(mutation.toString(), "ACL");
        }
        rejects("GRANT SELECT(id) ON catalog_order.customer_order TO customer_iam_role", "ACL");
        rejects("GRANT SELECT(revision) ON catalog_order.customer_order TO customer_iam_role", "ACL");
        rejects("SET LOCAL ROLE catalog_order_owner; GRANT SELECT(id) ON catalog_order.customer_order TO customer_iam_role WITH GRANT OPTION; RESET ROLE", "ACL");
        rejects("GRANT USAGE ON SCHEMA entitlement_metering TO customer_iam_role", "ACL");
    }
    @Test void v020RejectsAuthorityHistoryTopologyAndProvenanceDrift() throws Exception {
        rejects("UPDATE flyway_schema_history SET checksum=0 WHERE installed_rank=19", "HISTORY");
        rejects("UPDATE flyway_schema_history SET installed_rank=21 WHERE installed_rank=19", "HISTORY");
        rejects("ALTER ROLE customer_iam_role SET search_path=public", "SETTINGS");
        rejects("ALTER ROLE customer_iam_role BYPASSRLS", "ATTRIBUTES");
        rejects("GRANT catalog_order_role TO customer_iam_role", "TOPOLOGY");
        rejects("ALTER TABLE catalog_order.customer_order OWNER TO customer_iam_owner", "OWNERSHIP");
        rejects("ALTER DEFAULT PRIVILEGES FOR ROLE catalog_order_owner IN SCHEMA catalog_order GRANT SELECT ON TABLES TO customer_iam_role", "DEFAULT_ACL");
    }
    @Test void authorityProvenanceDatabaseAndDependencyDriftRejectAtomically() throws Exception {
        rejects("SET LOCAL ROLE customer_iam_role", "AUTHORITY");
        rejects("CREATE ROLE pa42_forged LOGIN; COMMENT ON ROLE pa42_forged IS 'onecall:pa25:forged'", "PROVENANCE");
        rejects("GRANT CREATE ON DATABASE pa27 TO customer_iam_role", "DATABASE");
        rejects("INSERT INTO pg_shdepend(dbid,classid,objid,objsubid,refclassid,refobjid,deptype) SELECT oid,'pg_class'::regclass,'catalog_order.customer_order'::regclass,0,'pg_authid'::regclass,'customer_iam_role'::regrole,'o' FROM pg_database WHERE datname=current_database()", "DEPENDENCY");
    }
    @Test void completedHistoryCannotRepairMissingGrantsOrAcceptInvalidSelfRows() throws Exception {
        upgrade();
        rejects("REVOKE SELECT(id,order_no,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,created_at,paid_at,closed_at) ON catalog_order.customer_order FROM customer_iam_role; REVOKE SELECT(order_id,status) ON entitlement_metering.entitlement_issuance FROM customer_iam_role; REVOKE USAGE ON SCHEMA entitlement_metering FROM customer_iam_role", "ACL");
        rejects("UPDATE flyway_schema_history SET checksum=NULL WHERE installed_rank=20", "HISTORY");
        rejects("UPDATE flyway_schema_history SET description='forged' WHERE installed_rank=20", "HISTORY");
        rejects("UPDATE flyway_schema_history SET script='V020__wrong.sql' WHERE installed_rank=20", "HISTORY");
        rejects("UPDATE flyway_schema_history SET success=false WHERE installed_rank=20", "HISTORY");
        rejects("UPDATE flyway_schema_history SET installed_rank=21 WHERE installed_rank=20", "HISTORY");
    }
    @Test void localRoleIsVerifiedAndRestoredAfterCommitRollbackAndReuse() throws Exception {
        upgrade(); fixture.provision();
        for (boolean commit : List.of(true, false)) {
            try (var connection = fixture.runtime()) {
                connection.setAutoCommit(false);
                execute(connection, "SET LOCAL ROLE customer_iam_role");
                assertThat(one(connection, "SELECT current_user||'|'||session_user"))
                        .isEqualTo("customer_iam_role|pa27_customer");
                denied(connection, "SELECT * FROM catalog_order.customer_order");
                if (commit) connection.commit(); else connection.rollback();
                assertThat(one(connection, "SELECT current_user||'|'||session_user")).isEqualTo("pa27_customer|pa27_customer");
            }
        }
    }
    void injectedFailure(String source, String category) throws Exception {
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false);
            String before = snapshot(connection);
            var savepoint = connection.setSavepoint();
            assertThatThrownBy(() -> execute(connection, source)).isInstanceOfSatisfying(SQLException.class, failure -> {
                assertThat(failure.getSQLState()).isEqualTo("P0001");
                assertThat(failure.getMessage()).contains("V020_" + category + "_REJECTED")
                        .doesNotContain("PRIVATE_CANARY", fixture.password);
            });
            connection.rollback(savepoint);
            assertThat(snapshot(connection)).isEqualTo(before);
            connection.rollback();
        }
    }
    @Test void successAndFailurePreserveEveryWriteInventory() throws Exception {
        historicalProvision();
        String source = Files.readString(SCRIPT);
        injectedFailure(source.replace("IF pass = 1 THEN invariant_before := invariant_after;",
                "IF pass = 2 THEN invariant_after := invariant_after || '{\"fault\":true}'::jsonb; END IF; IF pass = 1 THEN invariant_before := invariant_after;"),
                "PRESERVATION");
        injectedFailure(source.replace("EXECUTE format('SET LOCAL ROLE %I', session_user);",
                "EXECUTE format('SET LOCAL ROLE %I', session_user); RAISE EXCEPTION 'PRIVATE_CANARY';"), "RECONCILIATION");
        String business = fixture.businessSnapshot();
        upgrade();
        assertThat(fixture.businessSnapshot()).isEqualTo(business);
        try (var connection = fixture.admin()) {
            String before = snapshot(connection);
            connection.setAutoCommit(false);
            execute(connection, source);
            connection.commit();
            assertThat(snapshot(connection)).isEqualTo(before);
        }
    }
}
