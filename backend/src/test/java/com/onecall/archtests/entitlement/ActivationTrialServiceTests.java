package com.onecall.archtests.entitlement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService.*;
import com.onecall.core.entitlement_metering.internal.activation.*;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ActivationTrialServiceTests {
    static final UUID POLICY = UUID.randomUUID(), SUBJECT = UUID.randomUUID(), TRIAL = UUID.randomUUID(), GRANT = UUID.randomUUID(), BALANCE = UUID.randomUUID();
    static final String SNAPSHOT = "{\"policyCode\":\"FIRST-TRIAL\",\"promptVersion\":\"1\",\"contentSha256\":\"" + "a".repeat(64) + "\"}";
    static PolicyResult parse(String snapshot, int version) { return ActivationTrialPolicySnapshot.parse(POLICY, version, snapshot, "b".repeat(64)); }
    static final class Harness {
        final List<String> sql = new ArrayList<>();
        final TransactionTemplate transaction;
        final DefaultActivationTrialService service;
        final Map<String, Object> trial = new LinkedHashMap<>(), grant = new LinkedHashMap<>(), balance = new LinkedHashMap<>();
        String role = "activation_consent_role", outcome = "GRANTED";
        int policies = 1;
        boolean restoreFails;
        boolean missingBalance;
        Harness() throws Exception {
            var source = mock(DataSource.class); var connection = mock(Connection.class); when(source.getConnection()).thenReturn(connection); when(connection.getAutoCommit()).thenReturn(true);
            var manager = new DataSourceTransactionManager(source); transaction = new TransactionTemplate(manager);
            OffsetDateTime time = OffsetDateTime.parse("2026-11-13T00:00:00Z");
            trial.putAll(Map.of("id", TRIAL, "subject_id", SUBJECT, "trial_policy_version_id", POLICY, "entitlement_grant_id", GRANT, "source_action_domain", "ACTIVATION", "source_action_event_id", "event", "request_fingerprint", "c".repeat(64), "granted_at", time, "expires_at", time.plusDays(1)));
            trial.put("campaign_key", "FIRST-TRIAL");
            grant.putAll(Map.of("id", GRANT, "subject_id", SUBJECT, "source_type", "TRIAL", "source_ref_id", POLICY, "entitlement_type", "RECORDING_SECONDS", "granted_units", 600L, "valid_from", time, "granted_at", time, "expires_at", time.plusDays(1)));
            balance.putAll(Map.of("id", BALANCE, "grant_id", GRANT, "granted_units", 600L));
            grant.put("order_id", null); grant.put("installment_no", null);
            DSLContext context = mock(DSLContext.class, invocation -> {
                if (invocation.getArguments().length == 0 || !(invocation.getArgument(0) instanceof String query)) return RETURNS_DEFAULTS.answer(invocation);
                sql.add(query);
                if (invocation.getMethod().getName().equals("execute")) {
                    if (query.equals("SET LOCAL ROLE activation_consent_role") && restoreFails) throw new RuntimeException("PRIVATE_RESTORE");
                    if (query.startsWith("SET LOCAL ROLE ")) role = query.substring("SET LOCAL ROLE ".length());
                    return 0;
                }
                if (query.equals("SELECT current_user")) return rows(List.of(Map.of("current_user", role))).getFirst();
                if (query.contains("trial_policy_version WHERE")) {
                    var policy = Map.<String, Object>of("id", POLICY, "version_no", 1, "granted_seconds", 600, "valid_days", 1, "snapshot", SNAPSHOT, "snapshot_sha256", "b".repeat(64));
                    return rows(policies == 0 ? List.of() : policies == 1 ? List.of(policy) : List.of(policy, policy));
                }
                if (query.contains("issue_first_trial")) return rows(List.of(Map.of("trial_grant_id", TRIAL, "entitlement_grant_id", GRANT, "entitlement_balance_id", BALANCE, "outcome", outcome, "first_granted_at", time)));
                if (query.contains("FROM entitlement_metering.trial_grant")) return rows(List.of(trial));
                if (query.contains("FROM entitlement_metering.entitlement_grant")) return rows(List.of(grant));
                if (query.contains("FROM entitlement_metering.entitlement_balance")) return rows(missingBalance ? List.of() : List.of(balance));
                return RETURNS_DEFAULTS.answer(invocation);
            });
            service = new DefaultActivationTrialService(manager, context);
        }
        static Result<org.jooq.Record> rows(List<Map<String, Object>> values) {
            if (values.isEmpty()) return DSL.using(SQLDialect.POSTGRES).newResult(new Field<?>[0]);
            Field<?>[] fields = values.getFirst().entrySet().stream().map(entry -> DSL.field(entry.getKey(), entry.getValue() == null ? Object.class : entry.getValue().getClass())).toArray(Field<?>[]::new);
            var result = DSL.using(SQLDialect.POSTGRES).newResult(fields);
            for (var value : values) { var row = DSL.using(SQLDialect.POSTGRES).newRecord(fields); for (var entry : value.entrySet()) row.set(DSL.field(entry.getKey(), Object.class), entry.getValue()); result.add(row); }
            return result;
        }
        <Value> Value within(java.util.function.Supplier<Value> work) { return transaction.execute(status -> work.get()); }
    }
    @Test
    void snapshotRequiresExactRootConsentMetadataWithoutFallback() {
        assertThat(parse(SNAPSHOT, 1).valid()).isTrue();
        for (String invalid : List.of("{}", "null", "[]", SNAPSHOT.replace("\"FIRST-TRIAL\"", "null"), SNAPSHOT.replace("\"1\"", "1"), "{\"nested\":" + SNAPSHOT + "}")) assertThat(parse(invalid, 1).valid()).isFalse();
        assertThat(parse(SNAPSHOT.replace("{", "{\"extension\":{},"), 1).valid()).isTrue();
    }
    @Test
    void policyVersionUsesSamePositiveDecimalVersionIdentity() {
        for (String value : List.of("01", "+1", POLICY.toString(), "2")) assertThat(parse(SNAPSHOT.replace("{", "{\"policyVersion\":\"" + value + "\","), 1).valid()).isFalse();
        assertThat(parse(SNAPSHOT.replace("{", "{\"policyVersion\":\"1\","), 1).policy().policyVersion()).isEqualTo("1"); assertThat(parse(SNAPSHOT, 0).valid()).isFalse();
    }
    @Test
    void missingDisabledUnpublishedAndMalformedPoliciesFailClosed() throws Exception {
        var harness = new Harness(); harness.policies = 0; assertThat(harness.within(harness.service::readCurrentPolicy).valid()).isFalse();
        assertThat(harness.sql).anyMatch(query -> query.contains("enabled AND published_at IS NOT NULL")); harness.policies = 2;
        assertThatThrownBy(() -> harness.within(harness.service::readCurrentPolicy)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.kind()).isEqualTo(Failure.Kind.INTEGRITY));
        assertThat(ActivationTrialPolicySnapshot.parse(POLICY, 1, SNAPSHOT, "BAD").valid()).isFalse();
    }
    @Test
    void mandatoryOwnerScopeRequiresExistingActivationTransaction() throws Exception {
        var harness = new Harness(); assertThatThrownBy(harness.service::readCurrentPolicy).isInstanceOf(Failure.class); assertThat(harness.sql).isEmpty(); assertThat(harness.within(harness.service::readCurrentPolicy).valid()).isTrue();
    }
    @Test
    void ownerSwitchesAssertsAndRestoresExactRoles() throws Exception {
        var harness = new Harness(); harness.within(harness.service::readCurrentPolicy); assertThat(harness.role).isEqualTo("activation_consent_role");
        assertThat(harness.sql.getFirst()).isEqualTo("SELECT current_user"); assertThat(harness.sql).containsSubsequence("SET LOCAL ROLE entitlement_metering_role", "SELECT current_user", "SET LOCAL ROLE activation_consent_role", "SELECT current_user");
        harness.role = "unexpected"; assertThatThrownBy(() -> harness.within(harness.service::readCurrentPolicy)).isInstanceOf(Failure.class);
        harness.role = "activation_consent_role"; harness.restoreFails = true; assertThatThrownBy(() -> harness.within(harness.service::readCurrentPolicy)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.kind()).isEqualTo(Failure.Kind.UNAVAILABLE));
    }
    @Test
    void freshIssueLoadsStoredTrialGrantAndBalanceByReturnedIds() throws Exception {
        var harness = new Harness(); var result = harness.within(() -> harness.service.issueFirstTrial(new TrialIssueCommand(SUBJECT, POLICY, "event", "c".repeat(64))));
        assertThat(result.outcome()).isEqualTo("GRANTED"); assertThat(result.originalTrial().balanceId()).isEqualTo(BALANCE); assertThat(result.originalTrial().grantedSeconds()).isEqualTo(600); assertThat(harness.role).isEqualTo("activation_consent_role");
    }
    @Test
    void replayAndAlreadyGrantedReturnOriginalImmutableFacts() throws Exception {
        var harness = new Harness(); harness.outcome = "REPLAY"; var replay = harness.within(() -> harness.service.issueFirstTrial(new TrialIssueCommand(SUBJECT, POLICY, "event", "c".repeat(64))));
        harness.outcome = "ALREADY_GRANTED"; var later = harness.within(() -> harness.service.issueFirstTrial(new TrialIssueCommand(SUBJECT, UUID.randomUUID(), "later", "d".repeat(64))));
        assertThat(later.originalTrial()).isEqualTo(replay.originalTrial()); assertThat(later.originalTrial().expiresAt()).isEqualTo(InstantHolder.EXPIRY);
        assertThat(harness.sql).noneMatch(query -> query.contains("available_units") || query.contains("remaining_units"));
        for (String outcome : List.of("GRANTED", "REPLAY", "ALREADY_GRANTED")) {
            for (boolean sameAction : List.of(false, true)) {
                var candidate = new Harness(); candidate.outcome = outcome;
                var command = new TrialIssueCommand(SUBJECT, POLICY, sameAction ? "event" : "later", "c".repeat(64));
                if (sameAction == !"ALREADY_GRANTED".equals(outcome)) {
                    assertThat(candidate.within(() -> candidate.service.issueFirstTrial(command)).outcome()).isEqualTo(outcome);
                } else {
                    assertThatThrownBy(() -> candidate.within(() -> candidate.service.issueFirstTrial(command))).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.kind()).isEqualTo(Failure.Kind.INTEGRITY));
                }
                assertThat(candidate.role).isEqualTo("activation_consent_role");
            }
        }
        for (String outcome : List.of("GRANTED", "REPLAY")) {
            for (boolean wrongPolicy : List.of(false, true)) {
                var candidate = new Harness(); candidate.outcome = outcome;
                var command = new TrialIssueCommand(SUBJECT, wrongPolicy ? UUID.randomUUID() : POLICY, "event", wrongPolicy ? "c".repeat(64) : "d".repeat(64));
                assertThatThrownBy(() -> candidate.within(() -> candidate.service.issueFirstTrial(command))).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.kind()).isEqualTo(Failure.Kind.INTEGRITY));
                assertThat(candidate.role).isEqualTo("activation_consent_role");
            }
        }
    }
    @Test
    void incompleteOrInconsistentTrialFactsFailWithoutSynthesis() throws Exception {
        for (int corruption = 0; corruption < 4; corruption++) {
            var harness = new Harness(); if (corruption == 0) harness.missingBalance = true; if (corruption == 1) harness.balance.put("granted_units", 601L); if (corruption == 2) harness.grant.put("source_ref_id", UUID.randomUUID()); if (corruption == 3) harness.trial.put("request_fingerprint", "BAD");
            assertThatThrownBy(() -> harness.within(() -> harness.service.readOriginalTrial(SUBJECT))).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.kind()).isEqualTo(Failure.Kind.INTEGRITY)); assertThat(harness.role).isEqualTo("activation_consent_role");
        }
    }
    private static final class InstantHolder { static final java.time.Instant EXPIRY = java.time.Instant.parse("2026-11-14T00:00:00Z"); }
}
