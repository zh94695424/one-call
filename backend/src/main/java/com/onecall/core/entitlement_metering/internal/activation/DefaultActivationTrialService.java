package com.onecall.core.entitlement_metering.internal.activation;

import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService.Failure.Kind;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component("activationTrialService")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.activation-command", name = "enabled", havingValue = "true")
public final class DefaultActivationTrialService implements ActivationTrialService {
    private final DSLContext context;
    private final DataSourceTransactionManager manager;
    private final TransactionTemplate mandatory;
    public DefaultActivationTrialService(@Qualifier("activationCommandTransactionManager") DataSourceTransactionManager manager,
            @Qualifier("activationCommandDsl") DSLContext context) {
        this.context = context;
        this.manager = manager;
        mandatory = new TransactionTemplate(manager);
        mandatory.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
    }
    private <Result> Result scope(Supplier<Result> work) {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || manager.getDataSource() == null
                || !TransactionSynchronizationManager.hasResource(manager.getDataSource())) throw new Failure(Kind.UNAVAILABLE);
        return mandatory.execute(status -> {
            assertRole("activation_consent_role");
            RuntimeException primary = null;
            try {
                context.execute("SET LOCAL ROLE entitlement_metering_role");
                assertRole("entitlement_metering_role");
                return work.get();
            } catch (RuntimeException failure) { primary = failure; throw failure; }
            finally {
                try {
                    context.execute("SET LOCAL ROLE activation_consent_role");
                    assertRole("activation_consent_role");
                } catch (RuntimeException restoration) {
                    if (primary != null) primary.addSuppressed(new Failure(Kind.UNAVAILABLE));
                    else throw new Failure(Kind.UNAVAILABLE);
                }
            }
        });
    }
    private void assertRole(String expected) {
        if (!expected.equals(context.fetchOne("SELECT current_user").get(0, String.class))) throw new Failure(Kind.UNAVAILABLE);
    }
    @Override public PolicyResult readCurrentPolicy() {
        return scope(() -> {
            var rows = context.fetch("SELECT id,campaign_key,version_no,granted_seconds,valid_days,enabled,published_at,snapshot,snapshot_sha256 "
                    + "FROM entitlement_metering.trial_policy_version WHERE campaign_key='FIRST-TRIAL' AND enabled AND published_at IS NOT NULL");
            if (rows.isEmpty()) return new PolicyResult(null);
            if (rows.size() != 1) throw new Failure(Kind.INTEGRITY);
            var row = rows.getFirst();
            if (row.get("granted_seconds", Integer.class) < 1 || row.get("valid_days", Integer.class) < 1) return new PolicyResult(null);
            return ActivationTrialPolicySnapshot.parse(row.get("id", UUID.class), row.get("version_no", Integer.class),
                    row.get("snapshot", String.class), row.get("snapshot_sha256", String.class));
        });
    }
    @Override public Optional<OriginalTrial> readOriginalTrial(UUID subjectId) { return scope(() -> load(subjectId)); }
    private Optional<OriginalTrial> load(UUID subjectId) {
        var rows = context.fetch("SELECT id,subject_id,campaign_key,trial_policy_version_id,entitlement_grant_id,source_action_domain,source_action_event_id,request_fingerprint,granted_at,expires_at "
                + "FROM entitlement_metering.trial_grant WHERE subject_id=?", subjectId);
        if (rows.isEmpty()) return Optional.empty();
        if (rows.size() != 1) throw new Failure(Kind.INTEGRITY);
        var trial = rows.getFirst();
        var grants = context.fetch("SELECT id,subject_id,source_type,source_ref_id,entitlement_type,granted_units,valid_from,expires_at,order_id,installment_no,granted_at "
                + "FROM entitlement_metering.entitlement_grant WHERE id=?", trial.get("entitlement_grant_id", UUID.class));
        var balances = context.fetch("SELECT id,grant_id,granted_units FROM entitlement_metering.entitlement_balance WHERE grant_id=?", trial.get("entitlement_grant_id", UUID.class));
        if (grants.size() != 1 || balances.size() != 1) throw new Failure(Kind.INTEGRITY);
        var grant = grants.getFirst();
        var balance = balances.getFirst();
        Long units = grant.get("granted_units", Long.class);
        OffsetDateTime granted = trial.get("granted_at", OffsetDateTime.class);
        OffsetDateTime expires = trial.get("expires_at", OffsetDateTime.class);
        String domain = trial.get("source_action_domain", String.class);
        String event = trial.get("source_action_event_id", String.class);
        String fingerprint = trial.get("request_fingerprint", String.class);
        if (!"FIRST-TRIAL".equals(trial.get("campaign_key", String.class)) || !subjectId.equals(trial.get("subject_id", UUID.class)) || !subjectId.equals(grant.get("subject_id", UUID.class))
                || !"TRIAL".equals(grant.get("source_type", String.class)) || !"RECORDING_SECONDS".equals(grant.get("entitlement_type", String.class))
                || !Objects.equals(trial.get("trial_policy_version_id", UUID.class), grant.get("source_ref_id", UUID.class))
                || trial.get("trial_policy_version_id") == null || trial.get("id") == null || grant.get("id") == null || balance.get("id") == null
                || !Objects.equals(balance.get("grant_id", UUID.class), grant.get("id", UUID.class))
                || grant.get("order_id") != null || grant.get("installment_no") != null || units == null || units < 1
                || !units.equals(balance.get("granted_units", Long.class)) || granted == null || expires == null || !expires.isAfter(granted)
                || !Objects.equals(granted, grant.get("granted_at", OffsetDateTime.class)) || !Objects.equals(granted, grant.get("valid_from", OffsetDateTime.class))
                || !Objects.equals(expires, grant.get("expires_at", OffsetDateTime.class)) || !java.util.Set.of("ACTIVATION", "IVR_CONFIRM").contains(domain)
                || event == null || event.isBlank() || event.codePointCount(0, event.length()) > 128
                || fingerprint == null || !fingerprint.matches("[0-9a-f]{64}")) throw new Failure(Kind.INTEGRITY);
        return Optional.of(new OriginalTrial(trial.get("id", UUID.class), grant.get("id", UUID.class), balance.get("id", UUID.class), subjectId,
                trial.get("trial_policy_version_id", UUID.class), domain, event, fingerprint, units, granted.toInstant(), expires.toInstant()));
    }
    @Override public TrialIssueResult issueFirstTrial(TrialIssueCommand command) {
        return scope(() -> {
            var rows = context.fetch("SELECT trial_grant_id,entitlement_grant_id,entitlement_balance_id,outcome,first_granted_at "
                    + "FROM entitlement_metering.issue_first_trial(?,'FIRST-TRIAL',?,'ACTIVATION',?,?)", command.subjectId(), command.policyVersionId(),
                    command.activationEventId(), command.requestFingerprint());
            if (rows.size() != 1) throw new Failure(Kind.INTEGRITY);
            var row = rows.getFirst();
            OriginalTrial original = load(command.subjectId()).orElseThrow(() -> new Failure(Kind.INTEGRITY));
            String outcome = row.get("outcome", String.class);
            if (!java.util.Set.of("GRANTED", "REPLAY", "ALREADY_GRANTED").contains(outcome)
                    || !original.trialGrantId().equals(row.get("trial_grant_id", UUID.class))
                    || !original.entitlementGrantId().equals(row.get("entitlement_grant_id", UUID.class))
                    || !original.balanceId().equals(row.get("entitlement_balance_id", UUID.class))
                    || !original.grantedAt().equals(row.get("first_granted_at", OffsetDateTime.class).toInstant())) throw new Failure(Kind.INTEGRITY);
            boolean sameAction = "ACTIVATION".equals(original.sourceActionDomain()) && command.activationEventId().equals(original.sourceActionEventId());
            if (("ALREADY_GRANTED".equals(outcome) == sameAction) || sameAction && (!command.policyVersionId().equals(original.trialPolicyVersionId())
                    || !command.requestFingerprint().equals(original.requestFingerprint()))) throw new Failure(Kind.INTEGRITY);
            return new TrialIssueResult(outcome, original);
        });
    }
}
