package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.one;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.problem;
import static com.onecall.archtests.container.OperationsCatalogProductReadFixture.BASE;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.*;

class OperationsCatalogProductReadRoleLeastPrivilegeIT {
    static final OperationsCatalogProductReadFixture fixture = new OperationsCatalogProductReadFixture();
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @AfterEach void clean() throws Exception { fixture.unchanged(); }
    private void denied(String sql) throws Exception {
        try (var connection = fixture.shared.runtime()) {
            connection.setReadOnly(true); connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); connection.setAutoCommit(false);
            try { execute(connection, "SET LOCAL ROLE catalog_order_role"); assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("25006")); }
            finally { connection.rollback(); }
        }
    }
    private Set<String> matrix() throws Exception {
        try (var connection = fixture.admin(); var statement = connection.createStatement(); var rows = statement.executeQuery("""
                WITH objects AS (
                    SELECT 'table' kind,c.relname identity,'' column_name,c.relacl acl FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='catalog_order' AND c.relkind='r'
                    UNION ALL SELECT 'column',c.relname,a.attname,a.attacl FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='catalog_order' AND a.attnum>0 AND NOT a.attisdropped
                    UNION ALL SELECT 'routine',p.proname,'',p.proacl FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='catalog_order'
                    UNION ALL SELECT 'schema',nspname,'',nspacl FROM pg_namespace WHERE nspname='catalog_order'
                ) SELECT kind||'|'||identity||'|'||column_name||'|'||privilege_type||'|'||grantor::regrole::text||'|'||is_grantable FROM objects CROSS JOIN LATERAL aclexplode(acl) grants WHERE grantee='catalog_order_role'::regrole ORDER BY 1
                """)) {
            var result = new TreeSet<String>(); while (rows.next()) result.add(rows.getString(1)); return result;
        }
    }
    @Test void observesFrozenRoleMatrix() throws Exception {
        var expected = new TreeSet<String>(); String suffix = "|catalog_order_owner|false";
        for (String table : List.of("catalog_product", "catalog_version", "catalog_offer", "catalog_benefit_line", "customer_order", "payment_order", "payment_event")) for (String privilege : List.of("SELECT", "INSERT")) expected.add("table|" + table + "||" + privilege + suffix);
        var updates = Map.of("catalog_version", List.of("sale_status", "offer_type", "audience", "price_cents", "currency", "billing_cycle", "installments", "grant_interval_months", "snapshot", "snapshot_sha256", "published_at", "revision"), "catalog_offer", List.of("offer_status", "version_token", "revision"), "customer_order", List.of("payment_status", "paid_at", "closed_at", "revision"), "payment_order", List.of("status", "provider_transaction_id", "updated_at", "revision"));
        updates.forEach((table, columns) -> columns.forEach(column -> expected.add("column|" + table + "|" + column + "|UPDATE" + suffix)));
        for (String function : List.of("guard_catalog_version_published", "guard_catalog_offer", "guard_customer_order_snapshot", "guard_payment_order_identity")) expected.add("routine|" + function + "||EXECUTE" + suffix);
        expected.add("schema|catalog_order||USAGE" + suffix); assertThat(matrix()).hasSize(42).isEqualTo(expected);
        try (var connection = fixture.admin()) {
            assertThat(one(connection, "SELECT has_table_privilege('catalog_order_role','catalog_order.catalog_version','DELETE')")).isEqualTo("f");
            for (String schema : List.of("platform", "audit", "customer_iam", "organization_access", "subject_number", "activation_consent"))
                assertThat(one(connection, "SELECT has_schema_privilege('catalog_order_role',?,'USAGE')", schema)).isEqualTo("t");
            for (String routine : List.of("platform.idempotency_begin(text,text,text)", "platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)", "platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)", "audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)"))
                assertThat(one(connection, "SELECT has_function_privilege('catalog_order_role',?,'EXECUTE')", routine)).isEqualTo("t");
            var crossColumns = Map.of("customer_iam.customer_account", Set.of("id", "status"), "customer_iam.identity_profile", Set.of("account_id", "real_name_status"),
                    "organization_access.organization", Set.of("id", "status"), "organization_access.organization_membership", Set.of("organization_id", "account_id", "role", "status"),
                    "subject_number.business_subject", Set.of("id", "owner_account_id", "owner_organization_id", "revision"));
            for (var table : crossColumns.entrySet()) {
                try (var statement = connection.prepareStatement("SELECT a.attname FROM pg_attribute a CROSS JOIN LATERAL aclexplode(a.attacl) grants WHERE a.attrelid=CAST(? AS regclass) AND a.attnum>0 AND NOT a.attisdropped AND grantee='catalog_order_role'::regrole AND privilege_type='SELECT' AND NOT is_grantable")) {
                    statement.setString(1, table.getKey()); try (var rows = statement.executeQuery()) { var columns = new TreeSet<String>(); while (rows.next()) columns.add(rows.getString(1)); assertThat(columns).isEqualTo(table.getValue()); }
                }
            }
        }
    }
    @Test void selectsApprovedProjection() throws Exception {
        UUID id = fixture.product(1, "AB"); fixture.version(id, 1, "DRAFT", false); var statements = new ArrayList<String>();
        fixture.shared.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(org.jooq.ExecuteContext context) { if (context.sql().contains("FROM catalog_order.")) statements.add(context.sql()); } });
        fixture.get(BASE); fixture.get(BASE + "/" + id); assertThat(statements).hasSize(5);
        for (String sql : statements) assertThat(sql).doesNotContain("catalog_offer", "snapshot", "created_by", "payment", "SELECT *", "FOR UPDATE", "advisory");
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/catalog_order/internal/operationsread/JooqOperationsCatalogProductReader.java")); assertThat(source).contains("SELECT product_id, id, version_no, sale_status, offer_type, audience FROM");
    }
    @Test void deniesInsertByReadOnlyTransaction() throws Exception { denied("INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (gen_random_uuid(),'AB','STANDARD')"); }
    @Test void deniesUpdateByReadOnlyTransaction() throws Exception { denied("UPDATE catalog_order.catalog_version SET revision=revision+1"); }
    @Test void deniesDeleteByReadOnlyTransaction() throws Exception { denied("DELETE FROM catalog_order.catalog_product"); }
    @Test void deniesForUpdate() throws Exception { denied("SELECT id FROM catalog_order.catalog_product FOR UPDATE"); }
    @Test void deniesDdl() throws Exception { denied("CREATE TABLE catalog_order.pa37_forbidden(id integer)"); }
    @Test void assertsNoAdvisoryLocks() throws Exception {
        fixture.product(1, "AB"); var counts = new ArrayList<String>(); var failed = new AtomicBoolean(false);
        fixture.shared.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(org.jooq.ExecuteContext context) {
            try {
                counts.add(one(context.connection(), "SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid() AND locktype='advisory'"));
                assertThat(context.sql().toLowerCase(Locale.ROOT)).doesNotContain("advisory");
                if (context.sql().contains("FROM catalog_order.catalog_product") && failed.get()) throw new org.jooq.exception.DataAccessException("PRIVATE");
            } catch (SQLException failure) { throw new IllegalStateException(failure); }
        } });
        fixture.get(BASE); failed.set(true); problem(fixture.shared.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE");
        assertThat(counts).isNotEmpty().containsOnly("0"); fixture.shared.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]); fixture.shared.clean();
    }
    @Test void restoresRoleAfterSuccess() throws Exception {
        fixture.shared.transaction.execute(status -> { fixture.shared.scope.enter(); try {
            int backend = fixture.shared.dsl.fetchSingle("SELECT pg_backend_pid()").get(0, Integer.class);
            fixture.shared.scope.scoped(Role.ORDER, () -> { assertThat(fixture.shared.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo("catalog_order_role"); assertThat(fixture.shared.dsl.fetchSingle("SELECT pg_backend_pid()").get(0, Integer.class)).isEqualTo(backend); return null; });
            assertThat(fixture.shared.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo("operations_iam_role");
        } finally { fixture.shared.scope.exit(); } return null; }); fixture.get(BASE); fixture.shared.clean();
    }
    @Test void restoresRoleAfterFailure() throws Exception {
        fixture.shared.transaction.execute(status -> { fixture.shared.scope.enter(); try { assertThatThrownBy(() -> fixture.shared.scope.scoped(Role.ORDER, () -> { throw new IllegalStateException("PRIVATE"); })).isInstanceOf(IllegalStateException.class); assertThat(fixture.shared.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo("operations_iam_role"); } finally { fixture.shared.scope.exit(); } return null; });
        problem(fixture.shared.request("GET", BASE + "?cursor=!", "admin"), 400, "CURSOR_INVALID");
        for (String path : List.of(BASE, BASE + "/" + new UUID(0, 1))) {
            problem(fixture.shared.request("POST", path, null), 404, "NOT_FOUND");
            problem(fixture.shared.request("GET", path + ";x", null), 404, "NOT_FOUND");
        }
        fixture.shared.clean();
    }
    @Test void assertsReadOnlyState() throws Exception {
        fixture.shared.transaction.execute(status -> { fixture.shared.scope.enter(); try { fixture.shared.scope.scoped(Role.ORDER, () -> { assertThat(fixture.shared.dsl.fetchSingle("SHOW transaction_read_only").get(0, String.class)).isEqualTo("on"); assertThat(fixture.shared.dsl.fetchSingle("SHOW transaction_isolation").get(0, String.class)).isEqualTo("repeatable read"); return null; }); } finally { fixture.shared.scope.exit(); } return null; });
    }
    @Test void assertsTimeoutState() throws Exception {
        fixture.shared.transaction.execute(status -> { fixture.shared.scope.enter(); try { for (var setting : Map.of("statement_timeout", "2s", "lock_timeout", "250ms", "idle_in_transaction_session_timeout", "5s").entrySet()) assertThat(fixture.shared.dsl.fetchSingle("SHOW " + setting.getKey()).get(0, String.class)).isEqualTo(setting.getValue()); } finally { fixture.shared.scope.exit(); } return null; }); fixture.shared.clean();
    }
    @Test void assertsNoNewGrant() throws Exception {
        String before = matrix().toString(); fixture.get(BASE); assertThat(matrix().toString()).isEqualTo(before);
        var field = EvidenceRoleIvrAssociationAclIT.class.getDeclaredField("ACL"); field.setAccessible(true);
        try (var connection = fixture.admin(); var statement = connection.createStatement(); var rows = statement.executeQuery((String) field.get(null))) {
            var actual = new TreeSet<String>(); while (rows.next()) actual.add(rows.getString(1)); assertThat(actual).hasSize(410).isEqualTo(EvidenceRoleIvrAssociationAclIT.currentOracle());
        }
    }
    @Test void assertsNoNewRole() throws Exception { String before = fixture.catalog(); fixture.get(BASE); assertThat(fixture.catalog()).isEqualTo(before); }
    @Test void assertsNoNewSchema() throws Exception { String before = fixture.catalog(); fixture.get(BASE); problem(fixture.shared.request("GET", BASE + "?cursor=!", "admin"), 400, "CURSOR_INVALID"); assertThat(fixture.catalog()).isEqualTo(before); }
    @Test void assertsNoNewFunction() throws Exception { String before = fixture.catalog(); fixture.get(BASE); assertThat(fixture.catalog()).isEqualTo(before); }
}
