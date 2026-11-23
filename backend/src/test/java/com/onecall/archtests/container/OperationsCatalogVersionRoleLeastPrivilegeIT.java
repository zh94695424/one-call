package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.one;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;

public final class OperationsCatalogVersionRoleLeastPrivilegeIT {
    static final OperationsCatalogVersionLifecycleFixture fixture = new OperationsCatalogVersionLifecycleFixture();
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @AfterEach void clean() throws Exception { fixture.clean(); }
    @Test void disposableLoginHasAdminFalseInheritFalseSetTrueRoles() throws Exception {
        try (var connection = fixture.admin()) { assertThat(one(connection, "SELECT string_agg(r.rolname||':'||m.admin_option||':'||m.inherit_option||':'||m.set_option,',' ORDER BY r.rolname) FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.roleid WHERE m.member=?::regrole", fixture.shared.login)).isEqualTo("catalog_order_role:false:false:true,operations_iam_role:false:false:true"); }
    }
    void role(String role) throws Exception { try (var connection = fixture.shared.runtime()) { connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE " + role); assertThat(one(connection, "SELECT current_user")).isEqualTo(role); assertThat(one(connection, "SELECT session_user")).isEqualTo(fixture.shared.login); connection.rollback(); assertThat(one(connection, "SELECT current_user")).isEqualTo(fixture.shared.login); } }
    @Test void loginCanSetOperationsIamRole() throws Exception { role("operations_iam_role"); }
    @Test void loginCanSetCatalogOrderRole() throws Exception { role("catalog_order_role"); }
    @Test void operationsIamRoleReadsOnlyAuthorityColumns() throws Exception { UUID version = fixture.draft(); fixture.success(fixture.request(version, true, null, 1)); var reads = fixture.calls("FROM operations_iam.employee_account"); assertThat(reads).hasSize(1); assertThat(reads.getFirst().sql()).isEqualTo("SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = ?"); assertThat(reads.getFirst().role()).isEqualTo("operations_iam_role"); }
    void denied(String role, String sql, String state) throws Exception { try (var connection = fixture.shared.runtime()) { connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE " + role); assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo(state)); connection.rollback(); } }
    @Test void catalogRoleCannotReadEmployeeAccount() throws Exception { denied("catalog_order_role", "SELECT id,status,role,revision FROM operations_iam.employee_account", "42501"); }
    @Test void catalogRoleMatchesExactV004GrantMatrixAndNegativeProbes() throws Exception {
        var actual = new TreeSet<String>();
        try (var connection = fixture.admin(); var statement = connection.createStatement(); var rows = statement.executeQuery("""
            WITH objects AS (
              SELECT 'table' kind,c.relname identity,'' column_name,c.relacl acl FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='catalog_order' AND c.relkind='r'
              UNION ALL SELECT 'column',c.relname,a.attname,a.attacl FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='catalog_order' AND a.attnum>0 AND NOT a.attisdropped
              UNION ALL SELECT 'routine',p.proname,'',p.proacl FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='catalog_order'
              UNION ALL SELECT 'schema',nspname,'',nspacl FROM pg_namespace WHERE nspname='catalog_order'
            ) SELECT kind||'|'||identity||'|'||column_name||'|'||privilege_type||'|'||grantor::regrole::text||'|'||is_grantable FROM objects CROSS JOIN LATERAL aclexplode(acl) grants WHERE grantee='catalog_order_role'::regrole ORDER BY 1
            """)) { while (rows.next()) actual.add(rows.getString(1)); }
        var expected = new TreeSet<String>(); String suffix = "|catalog_order_owner|false";
        for (String table : List.of("catalog_product", "catalog_version", "catalog_offer", "catalog_benefit_line", "customer_order", "payment_order", "payment_event")) for (String privilege : List.of("SELECT", "INSERT")) expected.add("table|" + table + "||" + privilege + suffix);
        Map.of("catalog_version", List.of("sale_status", "offer_type", "audience", "price_cents", "currency", "billing_cycle", "installments", "grant_interval_months", "snapshot", "snapshot_sha256", "published_at", "revision"), "catalog_offer", List.of("offer_status", "version_token", "revision"), "customer_order", List.of("payment_status", "paid_at", "closed_at", "revision"), "payment_order", List.of("status", "provider_transaction_id", "updated_at", "revision")).forEach((table, columns) -> columns.forEach(column -> expected.add("column|" + table + "|" + column + "|UPDATE" + suffix)));
        for (String routine : List.of("guard_catalog_version_published", "guard_catalog_offer", "guard_customer_order_snapshot", "guard_payment_order_identity")) expected.add("routine|" + routine + "||EXECUTE" + suffix);
        expected.add("schema|catalog_order||USAGE" + suffix); assertThat(actual).hasSize(42).isEqualTo(expected);
        for (String sql : List.of("DELETE FROM catalog_order.catalog_version", "INSERT INTO audit.audit_fact(id) VALUES (gen_random_uuid())", "INSERT INTO platform.outbox_event(id) VALUES (gen_random_uuid())", "UPDATE operations_iam.employee_account SET revision=revision+1")) denied("catalog_order_role", sql, "42501");
        try (var connection = fixture.admin()) { for (String routine : List.of("platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)", "audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)")) assertThat(one(connection, "SELECT has_function_privilege('catalog_order_role',?,'EXECUTE')", routine)).isEqualTo("t"); }
    }
    @Test void v001ThroughV018ProvideAllRequiredObjects() throws Exception { assertThat(fixture.value("SELECT count(*) FROM public.flyway_schema_history WHERE success AND version IS NOT NULL")).isEqualTo("19"); for (String object : List.of("catalog_order.catalog_version", "operations_iam.employee_account", "audit.audit_fact", "platform.outbox_event")) assertThat(fixture.value("SELECT to_regclass(?) IS NOT NULL", object)).isEqualTo("t"); assertThat(fixture.value("SELECT version()")).contains("PostgreSQL 16."); }
    @Test void noV019IsNeededOrPresent() throws Exception { try (var paths = Files.list(Path.of("src/main/resources/db/migration"))) { var names = paths.map(path -> path.getFileName().toString()).sorted().toList(); assertThat(names).hasSize(19); assertThat(names).contains("V019__customer_catalog_offer_read_privileges.sql"); var historical = names.stream().filter(name -> !name.equals("V019__customer_catalog_offer_read_privileges.sql")).toList(); assertThat(historical).hasSize(18).noneMatch(name -> name.startsWith("V019")); assertThat(names).noneMatch(name -> name.startsWith("V020")); } assertThat(fixture.value("SELECT count(*) FROM public.flyway_schema_history WHERE version='019'")).isEqualTo("1"); }
    @Test void setLocalRoleIsExactAndRestored() throws Exception {
        fixture.success(fixture.request(fixture.draft(), true, null, 1)); var switches = fixture.calls("SET LOCAL ROLE"); assertThat(switches).extracting(OperationsCatalogVersionLifecycleFixture.Entry::sql).containsExactly("SET LOCAL ROLE operations_iam_role", "SET LOCAL ROLE catalog_order_role", "SET LOCAL ROLE NONE");
        assertThat(fixture.entries).extracting(OperationsCatalogVersionLifecycleFixture.Entry::pid).containsOnly(switches.getFirst().pid()); assertThat(fixture.entries).extracting(OperationsCatalogVersionLifecycleFixture.Entry::transaction).containsOnly(switches.getFirst().transaction()); assertThat(switches.getLast().role()).isEqualTo(fixture.shared.login);
        fixture.entries.clear(); fixture.problem(fixture.request(UUID.randomUUID(), true, null, 1, "ops"), 403, "FORBIDDEN"); assertThat(fixture.calls("SET LOCAL ROLE")).extracting(OperationsCatalogVersionLifecycleFixture.Entry::sql).containsExactly("SET LOCAL ROLE operations_iam_role", "SET LOCAL ROLE NONE");
    }
    @Test void restorationFailureEvictsDirtyConnection() throws Exception {
        UUID version = fixture.draft(); String before = fixture.lifecycle(); fixture.fail("BEFORE_ROLE_RESTORE"); fixture.problem(fixture.request(version, true, null, 1), 503, "SERVICE_UNAVAILABLE"); fixture.facts(0, 0); assertThat(fixture.lifecycle()).isEqualTo(before);
        fixture.fail("TERMINATE_BEFORE_ROLE_RESTORE"); fixture.problem(fixture.request(version, true, null, 1), 503, "SERVICE_UNAVAILABLE"); fixture.facts(0, 0); assertThat(fixture.lifecycle()).isEqualTo(before); fixture.readWitness(UUID.fromString(fixture.value("SELECT product_id FROM catalog_order.catalog_version WHERE id=?", version))); fixture.clean();
    }
    @Test void readOnlyOrWrongRoleCannotMutateCatalog() throws Exception { UUID version = fixture.draft(); try (var connection = fixture.shared.runtime()) { connection.setReadOnly(true); connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE catalog_order_role"); assertThatThrownBy(() -> execute(connection, "UPDATE catalog_order.catalog_version SET revision=revision+1 WHERE id=?", version)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("25006")); connection.rollback(); } denied("operations_iam_role", "UPDATE catalog_order.catalog_version SET revision=revision+1", "42501"); }
    @Test void publishedTriggerRejectsContentMutation() throws Exception { UUID version = fixture.version(fixture.product(), 1, "ACTIVE"); denied("catalog_order_role", "UPDATE catalog_order.catalog_version SET snapshot='{}',snapshot_sha256=repeat('b',64),revision=revision+1 WHERE id='" + version + "'", "P0001"); }
    @Test void activePartialUniqueIndexHolds() throws Exception { UUID product = fixture.product(); fixture.version(product, 1, "ACTIVE"); assertThatThrownBy(() -> fixture.version(product, 2, "ACTIVE")).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("23505")); fixture.invariant(); assertThat(fixture.value("SELECT indexdef FROM pg_indexes WHERE schemaname='catalog_order' AND indexname='uq_catalog_version_active'")).contains("UNIQUE", "WHERE (sale_status = 'ACTIVE'::text)"); }
    @Test void historicalRowsRemainUnchangedAfterRepeatedCommands() throws Exception { UUID product = fixture.product(), historical = fixture.version(product, 1, "OFFSALE"), version = fixture.version(product, 2, "DRAFT"); String before = fixture.value("SELECT row_to_json(row)::text FROM catalog_order.catalog_version row WHERE id=?", historical); fixture.success(fixture.request(version, true, null, 1)); fixture.success(fixture.request(version, true, version, 2)); fixture.success(fixture.request(version, false, null, 2)); assertThat(fixture.value("SELECT row_to_json(row)::text FROM catalog_order.catalog_version row WHERE id=?", historical)).isEqualTo(before); fixture.facts(2, 2); }
}
