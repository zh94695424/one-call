package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.one;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.Decision;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService.Failure;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActivationCommandRoleLeastPrivilegeIT {
    CustomerSubjectActivationFixture fixture;
    @BeforeAll void start() throws Exception { fixture = new CustomerSubjectActivationFixture(); fixture.start(); }
    @AfterAll void stop() { if (fixture != null) fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    void denied(String role, String sql) throws Exception {
        try (var connection = fixture.runtime()) { connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE " + role); assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501")); connection.rollback(); }
    }
    void clean() throws Exception {
        try (var connection = fixture.pool.getConnection()) { assertThat(one(connection, "SELECT current_user||'|'||session_user")).isEqualTo(fixture.login + "|" + fixture.login); assertThat(connection.getAutoCommit()).isTrue(); assertThat(connection.isReadOnly()).isFalse(); }
        assertThat(fixture.pool.getHikariPoolMXBean().getActiveConnections()).isZero();
    }
    @Test
    void syntheticLoginHasOnlyTwoNonInheritedSetMemberships() throws Exception {
        assertThat(fixture.scalar("SELECT rolcanlogin AND NOT rolinherit AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolreplication AND NOT rolbypassrls FROM pg_roles WHERE rolname='" + fixture.login + "'")).isEqualTo("t");
        assertThat(fixture.scalar("SELECT string_agg(parent.rolname,',' ORDER BY parent.rolname) FROM pg_auth_members membership JOIN pg_roles parent ON parent.oid=membership.roleid JOIN pg_roles child ON child.oid=membership.member WHERE child.rolname='" + fixture.login + "'")).isEqualTo("activation_consent_role,entitlement_metering_role");
        assertThat(fixture.scalar("SELECT bool_and(NOT admin_option AND NOT inherit_option AND set_option) FROM pg_auth_members WHERE member=(SELECT oid FROM pg_roles WHERE rolname='" + fixture.login + "')")).isEqualTo("t"); clean();
    }
    @Test
    void activationRoleCannotReadAnyEntitlementBaseTable() throws Exception {
        for (String table : List.of("trial_policy_version", "trial_grant", "entitlement_grant", "entitlement_balance", "entitlement_issuance", "usage_reservation", "usage_fact")) {
            denied("activation_consent_role", "SELECT * FROM entitlement_metering." + table); denied("activation_consent_role", "SELECT id FROM entitlement_metering." + table);
        }
        clean();
    }
    @Test
    void activationRoleHasExistingSchemaUsageAndIssueExecuteOnly() throws Exception {
        try (var connection = fixture.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE activation_consent_role");
            assertThat(one(connection, "SELECT has_schema_privilege(current_user,'entitlement_metering','USAGE')")).isEqualTo("t");
            assertThat(one(connection, "SELECT has_function_privilege(current_user,'entitlement_metering.issue_first_trial(uuid,text,uuid,text,text,text)','EXECUTE')")).isEqualTo("t");
            assertThat(one(connection, "SELECT has_table_privilege(current_user,'entitlement_metering.trial_policy_version','SELECT')")).isEqualTo("f"); connection.rollback();
        }
        assertThat(fixture.scalar("SELECT count(*) FROM pg_proc p CROSS JOIN LATERAL aclexplode(p.proacl) acl WHERE p.oid='entitlement_metering.issue_first_trial(uuid,text,uuid,text,text,text)'::regprocedure AND acl.grantee=0")).isEqualTo("0");
    }
    @Test
    void entitlementOwnerReadsCompletePolicyAndOriginalTrialFacts() throws Exception {
        var accepted = fixture.accept("event"); fixture.transaction.execute(status -> { fixture.repository.enter(fixture.dsl); var policy = fixture.trials.readCurrentPolicy(); assertThat(policy.valid()).isTrue(); assertThat(policy.policy().id()).isEqualTo(fixture.policy); var original = fixture.trials.readOriginalTrial(fixture.subject).orElseThrow(); assertThat(original.grantedSeconds()).isEqualTo(accepted.trial().grantedSeconds()); assertThat(original.expiresAt()).isEqualTo(accepted.trial().expiresAt()); assertThat(original.balanceId()).isNotNull(); fixture.repository.assertRole(fixture.dsl); return null; }); clean();
    }
    @Test
    void handoffUsesOneBackendPidAndOneTransactionId() {
        var priorListeners = fixture.dsl.configuration().executeListenerProviders();
        List<String> ownerIdentities = new ArrayList<>();
        fixture.dsl.configuration().set(new org.jooq.impl.DefaultExecuteListenerProvider(new org.jooq.impl.DefaultExecuteListener() {
            @Override public void executeEnd(org.jooq.ExecuteContext execution) {
                if (!"SET LOCAL ROLE entitlement_metering_role".equals(execution.sql())) return;
                try { ownerIdentities.add(one(execution.connection(), "SELECT current_user||'|'||session_user||'|'||pg_backend_pid()||'|'||txid_current()")); }
                catch (SQLException failure) { throw new IllegalStateException("Owner identity observation failed"); }
            }
        }));
        try {
            fixture.transaction.execute(status -> {
                fixture.repository.enter(fixture.dsl); String initial = fixture.identity(); fixture.trials.readCurrentPolicy(); assertThat(fixture.identity()).isEqualTo(initial);
                fixture.trials.readOriginalTrial(fixture.subject); assertThat(fixture.identity()).isEqualTo(initial); assertThat(initial).startsWith("activation_consent_role|" + fixture.login + "|");
                assertThat(ownerIdentities).containsExactly(initial.replace("activation_consent_role|", "entitlement_metering_role|"), initial.replace("activation_consent_role|", "entitlement_metering_role|"));
                assertThat(fixture.dsl.fetchOne("SHOW transaction_isolation").get(0, String.class)).isEqualTo("read committed"); assertThat(fixture.dsl.fetchOne("SHOW transaction_read_only").get(0, String.class)).isEqualTo("off"); return null;
            });
        } finally { fixture.dsl.configuration().set(priorListeners); }
    }
    @Test
    void mandatoryCallsOutsideTransactionAndUnexpectedRoleAreRejected() throws Exception {
        assertThatThrownBy(fixture.trials::readCurrentPolicy).isInstanceOf(Failure.class);
        assertThatThrownBy(() -> fixture.transaction.execute(status -> fixture.trials.readCurrentPolicy())).isInstanceOf(Failure.class); clean();
    }
    @Test
    void activationRoleIsRestoredBeforeLocalWritesAndCompletion() throws Exception {
        fixture.transaction.execute(status -> { fixture.repository.enter(fixture.dsl); fixture.trials.readCurrentPolicy(); fixture.repository.assertRole(fixture.dsl); fixture.trials.readOriginalTrial(fixture.subject); fixture.repository.assertRole(fixture.dsl); return null; });
        fixture.accept("event"); assertThat(fixture.count("activation_consent.consent_record")).isEqualTo(1); assertThat(fixture.count("activation_consent.service_activation_transition")).isEqualTo(1); clean();
    }
    @Test
    void rollbackAndPoolReuseCannotLeakEntitlementRole() throws Exception {
        assertThatThrownBy(() -> fixture.transaction.execute(status -> { fixture.repository.enter(fixture.dsl); fixture.trials.readCurrentPolicy(); fixture.dsl.execute("SELECT 1/0"); return null; })).isInstanceOf(RuntimeException.class); clean();
        assertThatThrownBy(() -> fixture.transaction.execute(status -> { fixture.repository.enter(fixture.dsl); try { fixture.dsl.execute("SELECT 1/0"); } catch (RuntimeException aborted) { fixture.trials.readCurrentPolicy(); } return null; })).isInstanceOf(RuntimeException.class); clean();
        fixture.accept("after-rollback"); clean();
    }
    @Test
    void noForeignWritesRowLocksOrExtraPrivilegesAreNeeded() throws Exception {
        for (String sql : List.of("SELECT id FROM customer_iam.customer_account FOR UPDATE", "SELECT id FROM organization_access.organization FOR UPDATE", "SELECT id FROM subject_number.business_subject FOR UPDATE", "UPDATE customer_iam.customer_account SET status='DISABLED'", "UPDATE subject_number.business_subject SET revision=revision+1")) denied("activation_consent_role", sql);
        denied("entitlement_metering_role", "SELECT id FROM activation_consent.consent_record"); denied("entitlement_metering_role", "UPDATE activation_consent.service_activation SET revision=revision+1"); fixture.accept("event"); clean();
    }
    @Test
    void fixtureCleanupAndFrozenAclInventoryRemainExact() throws Exception {
        var oracleType = CustomerEvidenceReadPrivilegeIT.class; var aclField = oracleType.getDeclaredField("ACL"); aclField.setAccessible(true); var oracleMethod = oracleType.getDeclaredMethod("currentOracle"); oracleMethod.setAccessible(true);
        Set<String> actual = new TreeSet<>(); try (var connection = fixture.admin(); var statement = connection.createStatement(); var rows = statement.executeQuery((String) aclField.get(null))) { while (rows.next()) actual.add(rows.getString(1)); }
        Set<String> expected = new TreeSet<>((Set<String>) oracleMethod.invoke(new CustomerEvidenceReadPrivilegeIT())); expected.addAll(EvidenceRoleIvrAssociationAclIT.associationDelta()); assertThat(expected).hasSize(290); Set<String> historical = new TreeSet<>(expected); expected = new TreeSet<>(CustomerCatalogOfferReadPrivilegesIT.current432Oracle()); expected.removeIf(value -> value.startsWith("operations_iam_role|") || value.startsWith("ivr_role|")); Set<String> recovered = new TreeSet<>(expected); recovered.removeAll(CustomerCatalogOfferReadPrivilegesIT.catalogDeltaOracle()); assertThat(recovered).isEqualTo(historical);
        assertThat(actual).hasSize(312).isEqualTo(expected);
        var delta = oracleType.getDeclaredMethod("expectedDelta"); delta.setAccessible(true); assertThat((Set<?>) delta.invoke(new CustomerEvidenceReadPrivilegeIT())).hasSize(19);
        assertThat(fixture.scalar("SELECT count(*) FROM public.flyway_schema_history WHERE success")).isEqualTo("19");
        fixture.accept("event"); fixture.reset(); assertThat(fixture.counts()).containsOnly(0L); clean();
    }
}
