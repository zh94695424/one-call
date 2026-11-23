package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import com.onecall.core.activation_consent.internal.operations.*;
import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.*;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OperationsLifecycleRoleLeastPrivilegeIT {
    OperationsSubjectLifecycleFixture fixture;
    @BeforeAll void start() throws Exception { fixture = new OperationsSubjectLifecycleFixture(); fixture.start(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @AfterEach void clean() throws Exception { fixture.cleanPool(); }
    @AfterAll void stop() { if (fixture != null) fixture.close(); }

    void deniedSql(String role, String sql) throws Exception {
        try (var connection = fixture.runtime()) { connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE " + role);
            assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501")); connection.rollback(); }
    }

    void cleanReusedBackend(String failedBackend) throws Exception {
        assertThat(failedBackend).isNotNull();
        try (var connection = fixture.pool.getConnection()) {
            assertThat(CustomerBusinessNumbersFixture.one(connection, "SELECT pg_backend_pid()")).isEqualTo(failedBackend);
            assertThat(connection.getAutoCommit()).isTrue();
            assertThat(CustomerBusinessNumbersFixture.one(connection, "SELECT current_user||'|'||session_user")).isEqualTo(fixture.login + "|" + fixture.login);
        }
        fixture.cleanPool();
    }

    @Test
    void activationRoleCannotReadOrLockEmployeeTable() throws Exception {
        for (String sql : List.of("SELECT id FROM operations_iam.employee_account", "SELECT id FROM operations_iam.employee_account FOR UPDATE")) deniedSql("activation_consent_role", sql);
    }
    @Test
    void employeeOwnerLocksOnlyItsOwnRowWithoutWrites() throws Exception {
        var before = fixture.business(); fixture.transaction.execute(status -> { fixture.repository.enter(fixture.dsl); var facts = fixture.employeeLock.lockEmployeeAdministrator(fixture.employee).orElseThrow(); assertThat(facts.employeeId()).isEqualTo(fixture.employee); assertThat(facts.status()).isEqualTo("ACTIVE"); assertThat(facts.role()).isEqualTo("OPS"); assertThat(facts.revision()).isEqualTo(1); return null; }); assertThat(fixture.business()).isEqualTo(before);
        assertThat(fixture.statements.stream().map(OperationsSubjectLifecycleFixture.StatementFact::sql)).noneMatch(sql -> sql.startsWith("INSERT ") || sql.startsWith("UPDATE ") || sql.startsWith("DELETE "));
    }
    @Test
    void handoffUsesOneBackendPidSessionAndTransactionId() throws Exception {
        fixture.transaction.execute(status -> { fixture.repository.enter(fixture.dsl); String identity = fixture.identity(); fixture.employeeLock.lockEmployeeAdministrator(fixture.employee); assertThat(fixture.identity()).isEqualTo(identity); return null; });
        var facts = fixture.statements.stream().filter(row -> row.sql().contains("employee_account") || row.sql().contains("admin_mutation")).toList(); assertThat(facts).hasSize(2); assertThat(facts.stream().map(row -> row.identity().substring(row.identity().indexOf('|'))).distinct()).hasSize(1); assertThat(facts).allMatch(row -> row.identity().startsWith("operations_iam_role|" + fixture.login + "|"));
    }
    @Test
    void mandatoryCallOutsideTransactionOrWrongEntryRoleIsRejected() throws Exception {
        assertThatThrownBy(() -> fixture.employeeLock.lockEmployeeAdministrator(fixture.employee)).isInstanceOfSatisfying(EmployeeAdministratorLock.Failure.class, failure -> assertThat(failure.kind()).isEqualTo(EmployeeAdministratorLock.Failure.Kind.UNAVAILABLE));
        assertThatThrownBy(() -> fixture.transaction.execute(status -> fixture.employeeLock.lockEmployeeAdministrator(fixture.employee))).isInstanceOf(EmployeeAdministratorLock.Failure.class);
    }
    @Test
    void activationRoleIsRestoredBeforeEveryOwnerWrite() throws Exception {
        assertThat(fixture.suspend("roles").statusCode()).isEqualTo(200); var writes = fixture.statements.stream().filter(row -> row.sql().startsWith("UPDATE ") || row.sql().startsWith("INSERT ") || row.sql().contains("outbox_append")).toList(); assertThat(writes).hasSize(3).allMatch(row -> row.identity().startsWith("activation_consent_role|" + fixture.login + "|")); fixture.cleanPool();
    }
    @Test
    void auditUsesOnlyExistingFunctionCapabilityAndRestoresRole() throws Exception {
        assertThat(fixture.suspend("audit").statusCode()).isEqualTo(200); var audit = fixture.statements.stream().filter(row -> row.sql().contains("audit.append_fact")).toList(); assertThat(audit).hasSize(1).allMatch(row -> row.identity().startsWith("operations_service_role|")); assertThat(fixture.statements.stream().map(OperationsSubjectLifecycleFixture.StatementFact::sql)).noneMatch(sql -> sql.startsWith("INSERT INTO audit.")); fixture.cleanPool();
    }
    @Test
    void failedRestorationRollsBackAndPoolReuseCannotLeakRole() throws Exception {
        int maximum = fixture.pool.getMaximumPoolSize(), minimum = fixture.pool.getMinimumIdle();
        try {
            fixture.pool.setMinimumIdle(0); fixture.pool.setMaximumPoolSize(1); fixture.pool.getHikariPoolMXBean().softEvictConnections();
            var failedBackend = new java.util.concurrent.atomic.AtomicReference<String>();
            String before = fixture.allFacts(); var employees = new DefaultListableBeanFactory(); employees.registerSingleton("aborted", (EmployeeAdministratorLock) identity -> { failedBackend.set(fixture.identity().split("\\|")[2]); fixture.dsl.execute("SET LOCAL ROLE operations_iam_role"); try { fixture.dsl.fetch("SELECT 1/0"); } finally { fixture.dsl.execute("SET LOCAL ROLE activation_consent_role"); } return Optional.empty(); });
            var service = new DefaultOperationsSubjectLifecycleService(fixture.transaction, fixture.dsl, fixture.repository, employees.getBeanProvider(EmployeeAdministratorLock.class));
            assertThatThrownBy(() -> service.suspend(fixture.command(fixture.subject, "aborted", "reason", BigInteger.TWO), fixture.caller())).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
            cleanReusedBackend(failedBackend.get()); assertThat(fixture.allFacts()).isEqualTo(before);
            failedBackend.set(null);
            var listeners = fixture.dsl.configuration().executeListenerProviders();
            var abort = new org.jooq.impl.DefaultExecuteListener() {
                @Override public void executeEnd(org.jooq.ExecuteContext execution) {
                    if (execution.sql().contains("employee_account WHERE id=? FOR UPDATE")) {
                        try (var statement = execution.connection().createStatement()) {
                            failedBackend.set(CustomerBusinessNumbersFixture.one(execution.connection(), "SELECT pg_backend_pid()"));
                            statement.execute("SELECT 1/0");
                        } catch (SQLException failure) { throw new org.jooq.exception.DataAccessException("Fixture aborted employee read", failure); }
                    }
                }
            };
            var faultListeners = Arrays.copyOf(listeners, listeners.length + 1); faultListeners[listeners.length] = new org.jooq.impl.DefaultExecuteListenerProvider(abort);
            fixture.dsl.configuration().set(faultListeners);
            try {
                assertThatThrownBy(() -> fixture.service.suspend(fixture.command(fixture.subject, "real-restoration", "reason", BigInteger.TWO), fixture.caller())).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
            } finally { fixture.dsl.configuration().set(listeners); }
            cleanReusedBackend(failedBackend.get()); assertThat(fixture.allFacts()).isEqualTo(before);
        } finally { fixture.pool.setMaximumPoolSize(maximum); fixture.pool.setMinimumIdle(minimum); }
    }
    @Test
    void ownerAndAuditRolesCannotWriteForeignBaseTables() throws Exception {
        for (String role : List.of("activation_consent_role", "operations_service_role", "operations_iam_role")) for (String sql : List.of("UPDATE subject_number.business_subject SET revision=revision+1", "UPDATE entitlement_metering.entitlement_balance SET revision=revision+1", "SELECT id FROM subject_number.business_subject FOR UPDATE")) deniedSql(role, sql);
        deniedSql("activation_consent_role", "UPDATE operations_iam.employee_account SET status='DISABLED'"); deniedSql("operations_service_role", "UPDATE activation_consent.service_activation SET revision=revision+1");
    }
    @Test
    void locksFollowSubjectActivationAdministratorEmployeeOutboxAuditOrder() throws Exception {
        fixture.concurrent(fixture.subject, "suspensions", "order", 2, fixture.subject, "suspensions", "order", 2);
        List<String> sql = fixture.statements.stream().map(OperationsSubjectLifecycleFixture.StatementFact::sql).toList();
        int subject = -1, head = -1, administrator = -1, employee = -1, outbox = -1, audit = -1;
        for (int index = 0; index < sql.size(); index++) { String statement = sql.get(index); if (subject < 0 && statement.contains("hashtextextended(?,0)")) subject = index; if (head < 0 && statement.contains("service_activation WHERE subject_id=? FOR UPDATE")) head = index; if (administrator < 0 && statement.contains("admin_mutation")) administrator = index; if (employee < 0 && statement.contains("employee_account WHERE id=? FOR UPDATE")) employee = index; if (outbox < 0 && statement.contains("outbox_append")) outbox = index; if (audit < 0 && statement.contains("audit.append_fact")) audit = index; }
        assertThat(subject).isGreaterThanOrEqualTo(0); assertThat(head).isGreaterThan(subject); assertThat(administrator).isGreaterThan(head); assertThat(employee).isGreaterThan(administrator); assertThat(outbox).isGreaterThan(employee); assertThat(audit).isGreaterThan(outbox);
    }
    @Test
    void runtimeLoginHasOnlyThreeFixtureMembershipsAndNoPrivilegeRepair() throws Exception {
        assertThat(fixture.scalar("SELECT string_agg(r.rolname,',' ORDER BY r.rolname) FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.roleid WHERE m.member='" + fixture.login + "'::regrole")).isEqualTo("activation_consent_role,operations_iam_role,operations_service_role");
        assertThat(fixture.scalar("SELECT count(*) FROM pg_roles WHERE rolname='" + fixture.login + "' AND NOT rolsuper AND NOT rolinherit AND NOT rolbypassrls AND NOT rolcreaterole")).isEqualTo("1"); fixture.suspend("login"); assertThat(fixture.statements.stream().map(OperationsSubjectLifecycleFixture.StatementFact::sql)).noneMatch(sql -> sql.contains("GRANT ") || sql.contains("CREATE ROLE") || sql.contains("ALTER ROLE"));
    }
    @Test
    void frozenSeventeenMigrationsAndIndependent288AclOracleRemainExact() throws Exception {
        var type = CustomerEvidenceReadPrivilegeIT.class; var field = type.getDeclaredField("ACL"); field.setAccessible(true); var oracle = type.getDeclaredMethod("currentOracle"); oracle.setAccessible(true);
        Set<String> actual = new TreeSet<>(); try (var connection = fixture.admin(); var statement = connection.createStatement(); var rows = statement.executeQuery((String) field.get(null))) { while (rows.next()) actual.add(rows.getString(1)); } Set<String> expected = new TreeSet<>((Set<String>) oracle.invoke(new CustomerEvidenceReadPrivilegeIT())); expected.addAll(EvidenceRoleIvrAssociationAclIT.associationDelta()); assertThat(expected).hasSize(290); Set<String> historical = new TreeSet<>(expected); expected = new TreeSet<>(CustomerCatalogOfferReadPrivilegesIT.current432Oracle()); expected.removeIf(value -> value.startsWith("operations_iam_role|") || value.startsWith("ivr_role|")); Set<String> recovered = new TreeSet<>(expected); recovered.removeAll(CustomerCatalogOfferReadPrivilegesIT.catalogDeltaOracle()); assertThat(recovered).isEqualTo(historical); assertThat(actual).hasSize(312).isEqualTo(expected); assertThat(fixture.scalar("SELECT count(*) FROM public.flyway_schema_history WHERE success AND version::integer BETWEEN 1 AND 17")).isEqualTo("17"); assertThat(fixture.scalar("SELECT count(*) FROM public.flyway_schema_history WHERE version='018'")).isEqualTo("1");
        assertThat(fixture.scalar("SELECT count(*) FROM public.flyway_schema_history WHERE version='019' AND success")).isEqualTo("1");
    }
    @Test
    void failureCleanupLeavesNoTransactionRoleLockOrPartialFact() throws Exception {
        String before = fixture.allFacts(); for (String stage : List.of("advance", "appendTransition", "appendOutbox", "appendAudit")) { var service = fixture.with(fixture.failAfter(stage)); assertThatThrownBy(() -> service.suspend(fixture.command(fixture.subject, stage, "reason", BigInteger.TWO), fixture.caller())).isInstanceOf(Failure.class); assertThat(fixture.allFacts()).isEqualTo(before); fixture.cleanPool(); }
    }
}
