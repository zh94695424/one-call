package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.one;
import static com.onecall.archtests.container.OperationsSubjectReadFixture.problem;

import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.*;

class OperationsSubjectReadRoleLeastPrivilegeIT {
    static final OperationsSubjectReadFixture fixture = new OperationsSubjectReadFixture();
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }

    @Test
    void syntheticLoginHasExactlySixNonInheritedSetMemberships() throws Exception {
        try (var connection = fixture.admin()) {
            assertThat(one(connection, "SELECT rolsuper OR rolinherit OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls FROM pg_roles WHERE rolname=?", fixture.login)).isEqualTo("f");
            var roles = new ArrayList<String>();
            try (var statement = connection.prepareStatement("SELECT r.rolname,m.admin_option,m.inherit_option,m.set_option FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.roleid JOIN pg_roles l ON l.oid=m.member WHERE l.rolname=? ORDER BY r.rolname")) {
                statement.setString(1, fixture.login);
                try (var rows = statement.executeQuery()) {
                    while (rows.next()) {
                        roles.add(rows.getString(1));
                        assertThat(rows.getBoolean(2)).isFalse();
                        assertThat(rows.getBoolean(3)).isFalse();
                        assertThat(rows.getBoolean(4)).isTrue();
                    }
                }
            }
            assertThat(roles).containsExactlyInAnyOrderElementsOf(OperationsSubjectReadFixture.ROLES);
        }
    }

    @Test
    void eachOwnerReadsOnlyItsOwnApprovedProjection() throws Exception {
        var tables = Map.of("operations_iam_role", "operations_iam.employee_account", "subject_number_role", "subject_number.business_subject",
                "activation_consent_role", "activation_consent.service_activation", "entitlement_metering_role", "entitlement_metering.trial_grant",
                "evidence_role", "evidence.evidence_record", "catalog_order_role", "catalog_order.customer_order");
        for (var owner : tables.entrySet()) {
            try (var connection = fixture.runtime()) {
                connection.setReadOnly(true);
                connection.setAutoCommit(false);
                execute(connection, "SET LOCAL ROLE " + owner.getKey());
                assertThat(one(connection, "SELECT count(*) FROM " + owner.getValue())).isNotNull();
                assertThat(one(connection, "SELECT current_user")).isEqualTo(owner.getKey());
                connection.rollback();
            }
        }
    }

    @Test
    void operationsRoleCannotReadForeignBaseTablesWith42501() throws Exception {
        for (String table : List.of("subject_number.business_subject", "activation_consent.service_activation", "entitlement_metering.trial_grant", "evidence.evidence_record", "catalog_order.customer_order")) {
            try (var connection = fixture.runtime()) {
                connection.setAutoCommit(false);
                execute(connection, "SET LOCAL ROLE operations_iam_role");
                assertThatThrownBy(() -> one(connection, "SELECT count(*) FROM " + table))
                        .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
                connection.rollback();
            }
        }
    }

    @Test
    void eachMissingMembershipFailsClosedWithoutPrivilegeRepair() throws Exception {
        UUID subject = fixture.subject();
        for (String role : OperationsSubjectReadFixture.ROLES) {
            try (var connection = fixture.admin()) { execute(connection, "REVOKE " + role + " FROM " + fixture.login); }
            try {
                problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "/" + subject, "admin"), 503, "SERVICE_UNAVAILABLE");
                try (var connection = fixture.admin()) {
                    assertThat(one(connection, "SELECT count(*) FROM pg_auth_members m JOIN pg_roles l ON l.oid=m.member WHERE l.rolname=?", fixture.login)).isEqualTo("5");
                }
            } finally {
                try (var connection = fixture.admin()) { execute(connection, "GRANT " + role + " TO " + fixture.login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE"); }
            }
        }
    }

    @Test
    void allOwnerReadsUseSameBackendPidTransactionAndSnapshot() throws Exception {
        UUID subject = fixture.subject();
        var observations = new ArrayList<String>();
        var roles = new ArrayList<String>();
        var providers = fixture.dsl.configuration().executeListenerProviders();
        fixture.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext context) {
                if (!context.sql().stripLeading().startsWith("SELECT") && !context.sql().stripLeading().startsWith("WITH")) return;
                try (var statement = context.connection().createStatement(); var row = statement.executeQuery(
                        "SELECT current_user,pg_backend_pid()::text||'|'||txid_current_snapshot()::text||'|'||current_setting('transaction_read_only')")) {
                    assertThat(row.next()).isTrue();
                    roles.add(row.getString(1));
                    observations.add(row.getString(2));
                } catch (SQLException failure) { throw new IllegalStateException(failure); }
            }
        });
        try {
            fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
            assertThat(observations).isNotEmpty().allMatch(observation -> observation.equals(observations.getFirst()));
            assertThat(observations.getFirst()).endsWith("|on");
            assertThat(roles).containsAll(OperationsSubjectReadFixture.ROLES);
        } finally { fixture.dsl.configuration().set(providers); }
    }

    @Test
    void wrongResourceEntryRoleAndMissingTransactionAreRejected() throws Exception {
        var scope = fixture.context.getBean("operationsSubjectReadContext", OperationsReadContext.class);
        assertThatThrownBy(scope::enter).isInstanceOf(Failure.class);
        var wrongResource = new OperationsReadContext(org.mockito.Mockito.mock(javax.sql.DataSource.class), fixture.dsl);
        assertThatThrownBy(() -> fixture.transaction.execute(status -> {
            wrongResource.enter();
            return null;
        })).isInstanceOf(Failure.class);
        assertThatThrownBy(() -> fixture.transaction.execute(status -> {
            fixture.dsl.execute("SET LOCAL ROLE subject_number_role");
            try { scope.enter(); } finally { fixture.dsl.execute("SET LOCAL ROLE NONE"); }
            return null;
        })).isInstanceOf(Failure.class);
    }

    @Test
    void readOnlyDatabaseRejectsWritesAndLockingQueries() throws Exception {
        for (String sql : List.of("UPDATE subject_number.business_subject SET source=source", "SELECT * FROM subject_number.business_subject FOR UPDATE")) {
            try (var connection = fixture.runtime()) {
                connection.setReadOnly(true);
                connection.setAutoCommit(false);
                execute(connection, "SET LOCAL ROLE subject_number_role");
                assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class,
                        failure -> assertThat(failure.getSQLState()).isEqualTo("25006"));
                connection.rollback();
            }
        }
    }

    @Test
    void normalAndExceptionalHandoffsRestoreOperationsRole() throws Exception {
        var scope = fixture.context.getBean("operationsSubjectReadContext", OperationsReadContext.class);
        fixture.transaction.execute(status -> {
            scope.enter();
            try {
                for (var role : List.of(OperationsReadContext.Role.SUBJECT, OperationsReadContext.Role.ACTIVATION, OperationsReadContext.Role.TRIAL, OperationsReadContext.Role.EVIDENCE, OperationsReadContext.Role.ORDER)) {
                    scope.scoped(role, () -> fixture.dsl.fetchSingle("SELECT current_user"));
                    assertThat(fixture.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo("operations_iam_role");
                    assertThatThrownBy(() -> scope.scoped(role, () -> { throw new IllegalStateException("PRIVATE_OWNER"); })).isInstanceOf(IllegalStateException.class);
                    assertThat(fixture.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo("operations_iam_role");
                }
            } finally { scope.exit(); }
            return null;
        });
    }

    @Test
    void failedRestorationRollsBackAndReusesSameCleanPhysicalConnection() throws Exception {
        fixture.pool.setMaximumPoolSize(1);
        fixture.pool.getHikariPoolMXBean().softEvictConnections();
        UUID subject = fixture.subject();
        var pids = new ArrayList<String>();
        var armed = new AtomicBoolean(true);
        var providers = fixture.dsl.configuration().executeListenerProviders();
        fixture.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext context) {
                if (!"SET LOCAL ROLE NONE".equals(context.sql())) return;
                try { pids.add(one(context.connection(), "SELECT pg_backend_pid()")); }
                catch (Exception failure) { throw new IllegalStateException(failure); }
                if (armed.getAndSet(false)) throw new Failure(Code.SERVICE_UNAVAILABLE);
            }
        });
        try {
            problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "/" + subject, "admin"), 503, "SERVICE_UNAVAILABLE");
            fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
            assertThat(pids).hasSize(2);
            assertThat(pids.getFirst()).isEqualTo(pids.getLast());
            try (var connection = fixture.pool.getConnection()) {
                assertThat(one(connection, "SELECT current_user")).isEqualTo(fixture.login);
                assertThat(connection.getAutoCommit()).isTrue();
                assertThat(connection.isReadOnly()).isFalse();
            }
        } finally { fixture.dsl.configuration().set(providers); }
    }

    @Test
    void poolReadOnlyIsolationAutocommitAndLocalSettingsAreRestored() throws Exception {
        UUID subject = fixture.subject();
        fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
        try (var connection = fixture.pool.getConnection()) {
            assertThat(connection.getAutoCommit()).isTrue();
            assertThat(connection.isReadOnly()).isFalse();
            assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
            assertThat(one(connection, "SELECT current_user")).isEqualTo(fixture.login);
            for (String setting : List.of("statement_timeout", "lock_timeout", "idle_in_transaction_session_timeout")) assertThat(one(connection, "SHOW " + setting)).isEqualTo("0");
        }
    }

    @Test
    void frozenAclOracleMigrationsRolesAndFunctionsRemainUnchanged() throws Exception {
        String before;
        try (var connection = fixture.admin()) { before = catalog(connection); }
        UUID subject = fixture.subject();
        fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
        try (var connection = fixture.admin()) {
            assertThat(catalog(connection)).isEqualTo(before);
            assertThat(one(connection, "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL")).isEqualTo("19");
        }
    }

    @Test
    void noResidualLocksTransactionsAuditOrOutboxFactsRemain() throws Exception {
        UUID subject = fixture.subject();
        fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
        problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "/" + subject, "disabled"), 403, "FORBIDDEN");
        problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "/" + UUID.randomUUID(), "admin"), 404, "NOT_FOUND");
        try (var connection = fixture.admin()) {
            assertThat(one(connection, "SELECT count(*) FROM audit.audit_fact")).isEqualTo("0");
            assertThat(one(connection, "SELECT count(*) FROM platform.outbox_event")).isEqualTo("0");
            assertThat(one(connection, "SELECT count(*) FROM pg_stat_activity WHERE usename=? AND (xact_start IS NOT NULL OR state <> 'idle')", fixture.login)).isEqualTo("0");
            assertThat(one(connection, "SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.usename=? AND l.locktype='relation'", fixture.login)).isEqualTo("0");
        }
    }

    private static String catalog(Connection connection) throws Exception {
        return one(connection, "SELECT md5(string_agg(oid::text||rolname||rolsuper::text||rolinherit::text,',' ORDER BY oid)) FROM pg_roles")
                + one(connection, "SELECT md5(string_agg(oid::text||coalesce(proacl::text,'')||prosrc,',' ORDER BY oid)) FROM pg_proc")
                + one(connection, "SELECT md5(string_agg(oid::text||coalesce(relacl::text,''),',' ORDER BY oid)) FROM pg_class")
                + one(connection, "SELECT md5(string_agg(roleid::text||':'||member::text||':'||grantor::text||':'||admin_option::text||':'||inherit_option::text||':'||set_option::text,',' ORDER BY roleid,member,grantor)) FROM pg_auth_members")
                + one(connection, "SELECT md5(string_agg(attrelid::text||':'||attnum::text||':'||coalesce(attacl::text,''),',' ORDER BY attrelid,attnum)) FROM pg_attribute");
    }

    private static String one(Connection connection, String sql, Object... arguments) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < arguments.length; index++) statement.setObject(index + 1, arguments[index]);
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                return rows.getString(1);
            }
        }
    }
}
