package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import static com.onecall.archtests.container.OperationsActivationReadFixture.*;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.*;

class OperationsActivationReadRoleLeastPrivilegeIT {
    static final OperationsActivationReadFixture fixture = new OperationsActivationReadFixture();
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @Test
    void runtimeLoginHasOnlyExistingExplicitRoleMembershipSemantics() throws Exception {
        try (var connection = fixture.admin()) {
            assertThat(one(connection, "SELECT rolsuper OR rolinherit OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls FROM pg_roles WHERE rolname=?", fixture.login)).isEqualTo("f"); var roles = new ArrayList<String>();
            try (var statement = connection.prepareStatement("SELECT r.rolname,m.admin_option,m.inherit_option,m.set_option FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.roleid JOIN pg_roles l ON l.oid=m.member WHERE l.rolname=?")) { statement.setString(1, fixture.login); try (var rows = statement.executeQuery()) { while (rows.next()) { roles.add(rows.getString(1)); assertThat(rows.getBoolean(2)).isFalse(); assertThat(rows.getBoolean(3)).isFalse(); assertThat(rows.getBoolean(4)).isTrue(); } } } assertThat(roles).containsExactlyInAnyOrderElementsOf(ROLES);
        }
    }
    @Test
    void operationsRoleCannotSelectTransitionsButActivationRoleCan() throws Exception {
        fixture.transition(1);
        denied("operations_iam_role", "SELECT id FROM activation_consent.service_activation_transition", "42501");
        try (var connection = fixture.runtime()) { connection.setReadOnly(true); connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE activation_consent_role"); assertThat(one(connection, "SELECT count(*) FROM activation_consent.service_activation_transition")).isEqualTo("1"); connection.rollback(); }
    }
    @Test
    void activationRoleCannotReadUnrelatedOwnerPayloads() throws Exception {
        for (String query : List.of("SELECT display_name FROM customer_iam.customer_account", "SELECT display_name FROM operations_iam.employee_account", "SELECT number_ciphertext FROM subject_number.caller_number_binding", "SELECT verification_ref_ciphertext FROM subject_number.subject_claim")) denied("activation_consent_role", query, "42501");
        try (var connection = fixture.admin()) { assertThat(one(connection, "SELECT has_column_privilege('activation_consent_role','subject_number.business_subject','id','SELECT')")).isEqualTo("t"); assertThat(one(connection, "SELECT has_column_privilege('activation_consent_role','operations_iam.employee_account','display_name','SELECT')")).isEqualTo("f"); }
    }
    @Test
    void missingRoleCapabilityFailsClosedWithoutRepair() throws Exception {
        var queries = new ArrayList<String>(); var providers = fixture.dsl.configuration().executeListenerProviders(); fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(ExecuteContext context) { queries.add(context.sql()); } });
        try { try (var connection = fixture.admin()) { execute(connection, "REVOKE activation_consent_role FROM " + fixture.login); } problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); assertThat(queries).noneMatch(query -> query.contains("GRANT") || query.contains("CREATE ROLE")); }
        finally { fixture.dsl.configuration().set(providers); try (var connection = fixture.admin()) { execute(connection, "GRANT activation_consent_role TO " + fixture.login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE"); } } fixture.get(BASE);
    }
    @Test
    void readOnlyTransactionRejectsWritesAndLockingQueries() throws Exception {
        fixture.transition(1);
        for (String query : List.of("INSERT INTO activation_consent.service_activation(id,subject_id) SELECT id,subject_id FROM activation_consent.service_activation", "UPDATE activation_consent.service_activation SET revision=revision+1", "DELETE FROM activation_consent.service_activation_transition", "SELECT id FROM activation_consent.service_activation FOR UPDATE", "SELECT id FROM activation_consent.service_activation FOR SHARE")) {
            try (var connection = fixture.runtime()) { connection.setReadOnly(true); connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE activation_consent_role"); assertThatThrownBy(() -> execute(connection, query)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isIn("25006", "42501")); connection.rollback(); }
        }
        assertThatThrownBy(() -> fixture.transaction.execute(status -> { fixture.scope.enter(); try { return fixture.scope.scoped(OperationsReadContext.Role.ACTIVATION, () -> fixture.dsl.execute("UPDATE activation_consent.service_activation SET revision=revision+1")); } finally { fixture.scope.exit(); } })).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
    }
    @Test
    void handoffsAssertAndRestoreExactRolesConnectionAndBackend() throws Exception {
        var samples = new ArrayList<String>(); var connections = new ArrayList<org.postgresql.PGConnection>(); var providers = fixture.dsl.configuration().executeListenerProviders();
        fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(ExecuteContext context) { if (context.sql().contains("FROM operations_iam.employee_account") || context.sql().contains("FROM activation_consent.service_activation_transition")) try { samples.add(one(context.connection(), "SELECT current_user||':'||txid_current_snapshot()::text||':'||pg_backend_pid()||':'||current_setting('transaction_read_only')||':'||current_setting('transaction_isolation')")); connections.add(context.connection().unwrap(org.postgresql.PGConnection.class)); } catch (Exception failure) { throw new IllegalStateException(failure); } } });
        try { fixture.get(BASE); assertThat(samples).hasSize(2); assertThat(samples.getFirst()).startsWith("operations_iam_role:"); assertThat(samples.getLast()).startsWith("activation_consent_role:"); assertThat(samples.getFirst().substring(samples.getFirst().indexOf(':'))).isEqualTo(samples.getLast().substring(samples.getLast().indexOf(':'))); assertThat(samples.getFirst()).endsWith(":on:repeatable read"); assertThat(connections.getFirst()).isSameAs(connections.getLast()); }
        finally { fixture.dsl.configuration().set(providers); }
        fixture.transaction.execute(status -> { fixture.scope.enter(); try { assertThatThrownBy(() -> fixture.scope.scoped(OperationsReadContext.Role.ACTIVATION, () -> { throw new IllegalStateException("PRIVATE"); })).isInstanceOf(IllegalStateException.class); assertThat(fixture.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo("operations_iam_role"); } finally { fixture.scope.exit(); } assertThat(fixture.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo(fixture.login); return null; });
    }
    @Test
    void wrongEntryResourceAndTransactionStateFailClosed() throws Exception {
        assertThatThrownBy(fixture.scope::enter).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
        for (String mode : List.of("notReadOnly", "isolation", "role", "resource", "backend")) {
            var template = new org.springframework.transaction.support.TransactionTemplate(fixture.transaction.getTransactionManager()); template.setReadOnly(!"notReadOnly".equals(mode)); template.setIsolationLevel("isolation".equals(mode) ? Connection.TRANSACTION_READ_COMMITTED : Connection.TRANSACTION_REPEATABLE_READ);
            assertThatThrownBy(() -> template.execute(status -> {
                if ("role".equals(mode)) fixture.dsl.execute("SET LOCAL ROLE activation_consent_role");
                if ("resource".equals(mode)) { new OperationsReadContext(org.mockito.Mockito.mock(javax.sql.DataSource.class), fixture.dsl).enter(); return null; }
                fixture.scope.enter(); try { if ("backend".equals(mode)) fixture.dsl.execute("SET LOCAL ROLE activation_consent_role"); return fixture.scope.scoped(OperationsReadContext.Role.ACTIVATION, () -> "unexpected"); } finally { fixture.scope.exit(); }
            })).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
        }
        fixture.get(BASE);
    }
    @Test
    void failedRestorationRollsBackAndEvictsDirtyConnections() throws Exception {
        fixture.pool.setMaximumPoolSize(1); fixture.pool.getHikariPoolMXBean().softEvictConnections();
        var providers = fixture.dsl.configuration().executeListenerProviders(); var armed = new AtomicBoolean(true);
        fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(ExecuteContext context) { if ("SET LOCAL ROLE NONE".equals(context.sql()) && armed.getAndSet(false)) throw new Failure(Code.SERVICE_UNAVAILABLE); } });
        try { problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); } finally { fixture.dsl.configuration().set(providers); } fixture.get(BASE);
        var backends = new ArrayList<String>(); armed.set(true);
        fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeEnd(ExecuteContext context) { if (context.sql().contains("FROM activation_consent.service_activation_transition") && armed.getAndSet(false)) try { backends.add(one(context.connection(), "SELECT pg_backend_pid()")); execute(context.connection(), "SET statement_timeout = '1234ms'"); } catch (Exception failure) { throw new IllegalStateException(failure); } } });
        try { problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); } finally { fixture.dsl.configuration().set(providers); }
        fixture.get(BASE); try (var connection = fixture.pool.getConnection()) { assertThat(one(connection, "SELECT pg_backend_pid()")).isNotEqualTo(backends.getFirst()); assertThat(one(connection, "SELECT current_user")).isEqualTo(fixture.login); assertThat(one(connection, "SHOW statement_timeout")).isEqualTo("0"); assertThat(one(connection, "SHOW TIME ZONE")).isEqualTo("UTC"); }
    }
    @Test
    void poolSettingsAndLocksAreCleanAfterSuccessAndFailure() throws Exception {
        fixture.get(BASE); problem(fixture.request("GET", BASE + "?cursor=bad", "admin"), 400, "CURSOR_INVALID");
        try (var connection = fixture.pool.getConnection()) { assertThat(connection.getAutoCommit()).isTrue(); assertThat(connection.isReadOnly()).isFalse(); assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED); assertThat(one(connection, "SELECT current_user")).isEqualTo(fixture.login); for (String setting : List.of("statement_timeout", "lock_timeout", "idle_in_transaction_session_timeout")) assertThat(one(connection, "SHOW " + setting)).isEqualTo("0"); }
        try (var connection = fixture.admin()) { assertThat(one(connection, "SELECT count(*) FROM pg_stat_activity WHERE usename=? AND (xact_start IS NOT NULL OR state <> 'idle')", fixture.login)).isEqualTo("0"); assertThat(one(connection, "SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.usename=? AND l.locktype='relation'", fixture.login)).isEqualTo("0"); }
    }
    @Test
    void frozenAclRolesFunctionsAndAllFactsHaveZeroDelta() throws Exception {
        fixture.transition(1); String catalog; try (var connection = fixture.admin()) { catalog = catalog(connection); } var facts = fixture.fingerprints(); fixture.get(BASE); problem(fixture.request("GET", BASE + "?cursor=bad", "admin"), 400, "CURSOR_INVALID");
        try (var connection = fixture.admin()) { assertThat(catalog(connection)).isEqualTo(catalog); assertThat(one(connection, "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL")).isEqualTo("20"); }
        assertThat(fixture.fingerprints()).isEqualTo(facts);
        try (var baseline = new CustomerBusinessNumbersFixture()) {
            baseline.start(null);
            var oracle = baseline.psql("check");
            assertThat(oracle.getExitCode()).as(oracle.getStdout() + oracle.getStderr()).isZero();
            assertThat(oracle.getStdout()).contains("PA25_CHECK_OK");
        }
    }
    private void denied(String role, String query, String state) throws Exception { try (var connection = fixture.runtime()) { connection.setReadOnly(true); connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE " + role); assertThatThrownBy(() -> execute(connection, query)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo(state)); connection.rollback(); } }
    private String catalog(Connection connection) throws Exception {
        return one(connection, "SELECT md5(string_agg(oid::text||rolname||rolsuper::text||rolinherit::text,',' ORDER BY oid)) FROM pg_roles") + one(connection, "SELECT md5(string_agg(oid::text||coalesce(proacl::text,'')||prosrc,',' ORDER BY oid)) FROM pg_proc") + one(connection, "SELECT md5(string_agg(oid::text||coalesce(relacl::text,''),',' ORDER BY oid)) FROM pg_class") + one(connection, "SELECT md5(string_agg(roleid::text||':'||member::text||':'||grantor::text||':'||admin_option::text||':'||inherit_option::text||':'||set_option::text,',' ORDER BY roleid,member,grantor)) FROM pg_auth_members") + one(connection, "SELECT md5(string_agg(attrelid::text||':'||attnum::text||':'||coalesce(attacl::text,''),',' ORDER BY attrelid,attnum)) FROM pg_attribute");
    }
    private String one(Connection connection, String sql, Object... arguments) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < arguments.length; index++) statement.setObject(index + 1, arguments[index]);
            try (var rows = statement.executeQuery()) { assertThat(rows.next()).isTrue(); return rows.getString(1); }
        }
    }
}
