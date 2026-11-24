package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.one;
import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerOrderRoleLeastPrivilegeIT {
    CustomerOrdersFixture fixture;
    @BeforeAll void start() throws Exception { fixture = new CustomerOrdersFixture(); fixture.start(); }
    @AfterAll void stop() { if (fixture != null) fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    void denied(String role, String sql) throws Exception {
        try (var connection = fixture.runtime()) {
            connection.setAutoCommit(false); if (role != null) execute(connection, "SET LOCAL ROLE " + role);
            assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501")); connection.rollback();
        }
    }
    @Test void postgres16Flyway19AndExactPa25TopologyAreUsed() throws Exception {
        assertThat(Integer.parseInt(fixture.value("SHOW server_version_num"))).isBetween(160000, 169999);
        assertThat(fixture.value("SELECT count(*) FROM flyway_schema_history WHERE success")).isEqualTo("19");
        var check = fixture.base.psql("check"); assertThat(check.getExitCode()).as(check.getStderr()).isZero(); assertThat(check.getStdout()).contains("PA25_CHECK_OK logins=7 effective_roles=9 memberships=11");
        org.flywaydb.core.Flyway.configure().dataSource(fixture.base.url, "fixture_admin", fixture.base.password).locations("classpath:db/migration").load().validate();
    }
    @Test void loHasNoRightsUntilLiteralOwnerRoleSwitch() throws Exception {
        denied(null, "SELECT id FROM catalog_order.customer_order"); denied(null, "SELECT id FROM activation_consent.consent_record");
        try (var connection = fixture.runtime()) {
            assertThat(one(connection, "SELECT rolsuper OR rolinherit OR rolcreaterole OR rolcreatedb FROM pg_roles WHERE rolname=current_user")).isEqualTo("f");
            assertThat(one(connection, "SELECT count(*) FROM pg_auth_members m JOIN pg_roles member ON member.oid=m.member WHERE member.rolname='pa36_operations' AND m.set_option AND NOT m.inherit_option AND NOT m.admin_option")).isEqualTo("7");
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE catalog_order_role"); execute(connection, "SELECT id FROM catalog_order.customer_order"); execute(connection, "SET LOCAL ROLE activation_consent_role"); execute(connection, "SELECT id FROM activation_consent.consent_record"); connection.rollback();
        }
    }
    @Test void customerAndActivationPoolsCannotSubstituteForLoTransaction() throws Exception {
        try (var connection = fixture.base.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            assertThatThrownBy(() -> execute(connection, "SELECT id FROM catalog_order.customer_order")).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501")); connection.rollback();
        }
        var owner = fixture.context.getBean(PurchaseConsentService.class);
        assertThatThrownBy(() -> owner.lockServiceActivation(fixture.subject, new PurchaseConsentService.TransactionWitness("pa36_operations", 1, 1))).isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }
    @Test void orderAndConsentShareConnectionTransactionAndLogin() throws Exception {
        fixture.success(fixture.post("witness"), 201);
        assertThat(fixture.entries).extracting(CustomerOrdersFixture.Entry::login).containsOnly("pa36_operations");
        assertThat(fixture.entries).extracting(CustomerOrdersFixture.Entry::pid).containsOnly(fixture.entries.getFirst().pid());
        assertThat(fixture.entries).extracting(CustomerOrdersFixture.Entry::txid).containsOnly(fixture.entries.getFirst().txid());
        assertThat(fixture.entries.stream().filter(entry -> entry.sql().startsWith("INSERT INTO activation_consent"))).isNotEmpty().allSatisfy(entry -> assertThat(entry.role()).isEqualTo("activation_consent_role"));
        assertThat(fixture.entries.stream().filter(entry -> entry.sql().startsWith("INSERT INTO catalog_order"))).isNotEmpty().allSatisfy(entry -> assertThat(entry.role()).isEqualTo("catalog_order_role"));
    }
    @Test void activationRowLockIsTakenOnlyByOwnerBeforeIdempotency() throws Exception {
        fixture.success(fixture.post("locks"), 201);
        var locks = fixture.entries.stream().filter(entry -> entry.sql().contains("FOR UPDATE")).toList(); assertThat(locks).hasSize(1); assertThat(locks.getFirst().role()).isEqualTo("activation_consent_role");
        assertThat(fixture.entries.indexOf(locks.getFirst())).isLessThan(index("idempotency_begin"));
        denied("catalog_order_role", "SELECT id FROM activation_consent.service_activation FOR UPDATE");
    }
    @Test void catalogReadsOnlyPermittedForeignColumnsWithoutLocks() throws Exception {
        for (String sql : List.of("SELECT snapshot FROM activation_consent.consent_record", "SELECT legal_name_ciphertext FROM customer_iam.identity_profile", "SELECT id FROM customer_iam.customer_account FOR UPDATE", "UPDATE activation_consent.service_activation SET status='ACTIVE'")) denied("catalog_order_role", sql);
        try (var connection = fixture.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE catalog_order_role");
            execute(connection, "SELECT id,subject_id,purpose,channel,event_domain,event_id,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,request_fingerprint,accepted_at FROM activation_consent.consent_record"); connection.rollback();
        }
    }
    @Test void roleHandoffsRestoreOnSuccessReplayAndTypedDenial() throws Exception {
        fixture.success(fixture.post("clean"), 201); fixture.clean(); fixture.success(fixture.post("clean"), 200); fixture.clean();
        fixture.problem(fixture.post("web", "stale", fixture.body().replace("SYNTHETIC_PURCHASE", "stale")), 409, "PURCHASE_CONSENT_STALE"); fixture.clean();
        var transitions = fixture.entries.stream().filter(entry -> entry.sql().startsWith("SET LOCAL ROLE ")).toList();
        assertThat(transitions).isNotEmpty().extracting(CustomerOrdersFixture.Entry::sql).contains("SET LOCAL ROLE activation_consent_role", "SET LOCAL ROLE catalog_order_role", "SET LOCAL ROLE NONE");
        assertThat(transitions.stream().filter(entry -> entry.sql().equals("SET LOCAL ROLE activation_consent_role"))).isNotEmpty().allSatisfy(entry -> assertThat(entry.role()).isEqualTo("activation_consent_role"));
        assertThat(transitions.stream().filter(entry -> entry.sql().equals("SET LOCAL ROLE catalog_order_role"))).isNotEmpty().allSatisfy(entry -> assertThat(entry.role()).isEqualTo("catalog_order_role"));
        assertThat(transitions.stream().filter(entry -> entry.sql().equals("SET LOCAL ROLE NONE"))).isNotEmpty().allSatisfy(entry -> assertThat(entry.role()).isEqualTo(entry.login()));
    }
    @Test void sqlAbortAndRestorationFaultRollbackAndCleanPool() throws Exception {
        for (String mode : List.of("SQL_ABORT", "RESTORE", "TERMINATE")) {
            fixture.reset(); var before = fixture.rows(); AtomicBoolean armed = new AtomicBoolean(true);
            fixture.before = execution -> {
                if (armed.get() && (mode.equals("RESTORE") ? execution.sql().equals("SET LOCAL ROLE NONE") : execution.sql().startsWith("INSERT INTO catalog_order.customer_order"))) {
                    armed.set(false);
                    if (mode.equals("RESTORE")) throw new IllegalStateException("PRIVATE cleanup");
                    if (mode.equals("TERMINATE")) {
                        try { fixture.sql("SELECT pg_terminate_backend(?)", Integer.parseInt(one(execution.connection(), "SELECT pg_backend_pid()"))); }
                        catch (Exception failure) { throw new AssertionError(failure); }
                        return;
                    }
                    try { execute(execution.connection(), "SELECT snapshot FROM activation_consent.consent_record"); }
                    catch (SQLException failure) { throw new org.jooq.exception.DataAccessException("PRIVATE SQL abort", failure); }
                }
            };
            fixture.problem(fixture.post("rollback"), 503, "SERVICE_UNAVAILABLE"); assertThat(armed).isFalse(); fixture.before = null; assertThat(fixture.rows()).isEqualTo(before); fixture.clean();
        }
        for (String mode : List.of("ASSERT_CLEAN", "CLOSE")) for (String retry : List.of("UNCHANGED", "ACCOUNT", "ACTIVATION", "WITHDRAWN", "OFFSALE", "TOKEN")) {
            fixture.reset(); var before = fixture.rows(); AtomicBoolean armed = new AtomicBoolean(true), committed = new AtomicBoolean();
            fixture.before = execution -> {
                if (execution.sql().contains("idempotency_begin") && armed.getAndSet(false)) {
                    Connection connection = ((org.springframework.jdbc.datasource.ConnectionHolder) TransactionSynchronizationManager.getResource(fixture.pool)).getConnection();
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCommit() {
                            try {
                                fixture.bundle(before); committed.set(true);
                                if (mode.equals("ASSERT_CLEAN")) {
                                    var field = fixture.pool.getClass().getDeclaredField("cleanupFailure"); field.setAccessible(true);
                                    @SuppressWarnings("unchecked") ThreadLocal<Boolean> failure = (ThreadLocal<Boolean>) field.get(fixture.pool); failure.set(true);
                                }
                                else execute(connection, "SET SESSION statement_timeout = '123ms'");
                            } catch (Exception failure) { throw new AssertionError(failure); }
                        }
                    });
                }
            };
            var ambiguous = fixture.post("ambiguous"); assertThat(committed).as(mode + "/" + retry).isTrue(); assertThat(ambiguous.statusCode()).as(mode + "/" + retry + ": " + ambiguous.body()).isEqualTo(503); fixture.problem(ambiguous, 503, "SERVICE_UNAVAILABLE"); fixture.before = null; fixture.bundle(before); fixture.clean();
            switch (retry) {
                case "ACCOUNT" -> fixture.sql("UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1");
                case "ACTIVATION" -> fixture.sql("UPDATE activation_consent.service_activation SET status='SUSPENDED',revision=revision+1");
                case "WITHDRAWN" -> fixture.sql("UPDATE catalog_order.catalog_offer SET offer_status='WITHDRAWN',revision=revision+1");
                case "OFFSALE" -> fixture.sql("UPDATE catalog_order.catalog_version SET sale_status='OFFSALE',revision=revision+1");
                case "TOKEN" -> fixture.sql("UPDATE catalog_order.catalog_offer SET version_token='changed',revision=revision+1");
                default -> { }
            }
            var retryBefore = fixture.rows(); var response = fixture.post("ambiguous");
            switch (retry) {
                case "ACCOUNT" -> fixture.problem(response, 403, "FORBIDDEN"); case "ACTIVATION" -> fixture.problem(response, 403, "ACTIVATION_NOT_ACTIVE");
                case "WITHDRAWN", "OFFSALE" -> fixture.problem(response, 422, "VALIDATION_FAILED"); case "TOKEN" -> fixture.problem(response, 409, "IDEMPOTENCY_CONFLICT"); default -> fixture.success(response, 200);
            }
            assertThat(fixture.rows()).isEqualTo(retryBefore); fixture.clean();
        }
    }
    @Test void missingMembershipOrRoleFailsClosedWithoutAclRepair() throws Exception {
        var baseline = fixture.rows(); List<Connection> held = new ArrayList<>();
        try {
            for (int index = 0; index < 4; index++) held.add(fixture.pool.getConnection());
            assertThat(fixture.pool.getMaximumPoolSize()).isEqualTo(4); assertThat(fixture.pool.getConnectionTimeout()).isEqualTo(3000);
            fixture.problem(fixture.post("exhausted"), 503, "SERVICE_UNAVAILABLE"); assertThat(fixture.rows()).isEqualTo(baseline);
        } finally { for (Connection connection : held) connection.close(); }
        fixture.clean();
        fixture.session("web", "WEB", UUID.randomUUID()); var before = fixture.rows(); fixture.problem(fixture.post("membership"), 403, "FORBIDDEN"); assertThat(fixture.rows()).isEqualTo(before);
        assertThat(fixture.base.psql("check").getExitCode()).isZero();
    }
    int index(String text) { for (int index = 0; index < fixture.entries.size(); index++) if (fixture.entries.get(index).sql().contains(text) || fixture.entries.get(index).binds().stream().anyMatch(bind -> Objects.toString(bind).contains(text))) return index; throw new AssertionError("Missing SQL/key " + text); }
    @Test void lockOrderAndTimeoutsMatchExistingLifecycleAndCatalogKeys() throws Exception {
        fixture.success(fixture.post("rank"), 201);
        assertThat(index("subject_number.subject:")).isLessThan(index("FOR UPDATE")); assertThat(index("FOR UPDATE")).isLessThan(index("customer_iam.account:"));
        assertThat(index("customer_iam.account:")).isLessThan(index("catalog_order.publication:")); assertThat(index("catalog_order.publication:")).isLessThan(index("idempotency_begin"));
        assertThat(fixture.entries).noneMatch(entry -> entry.binds().stream().anyMatch(bind -> Objects.toString(bind).startsWith("organization_access.")));
        assertThat(fixture.entries).extracting(CustomerOrdersFixture.Entry::sql).contains("SET LOCAL statement_timeout = '2000ms'", "SET LOCAL lock_timeout = '250ms'", "SET LOCAL idle_in_transaction_session_timeout = '5000ms'");
    }
    @Test void platformAuditAndOutboxTablesAreFunctionOnly() throws Exception {
        for (String role : List.of("catalog_order_role", "activation_consent_role")) for (String table : List.of("platform.outbox_event", "platform.idempotency_record", "audit.audit_fact")) denied(role, "DELETE FROM " + table);
        var before = fixture.rows(); fixture.success(fixture.post("facts"), 201); fixture.bundle(before);
    }
    @Test void noNewDatabaseObjectPrivilegeOrMigrationAppears() throws Exception {
        String sql = "SELECT md5(string_agg(oid::text||':'||coalesce(relacl::text,''),',' ORDER BY oid)) FROM pg_class WHERE relnamespace IN (SELECT oid FROM pg_namespace WHERE nspname IN ('catalog_order','activation_consent','platform','audit','customer_iam','organization_access','subject_number'))";
        String before = fixture.value(sql); fixture.success(fixture.post("no-ddl"), 201); assertThat(fixture.value(sql)).isEqualTo(before); assertThat(fixture.base.psql("check").getExitCode()).isZero();
    }
}
