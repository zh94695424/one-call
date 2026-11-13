package com.onecall.core.activation_consent.internal.command;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code;
import com.onecall.core.activation_consent.internal.command.SubjectActivationRepository.*;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class DefaultCustomerSubjectActivationService implements CustomerSubjectActivationService {
    private static final Set<String> SERIALIZATION_KEYS = Set.of("uq_consent_event", "uq_service_activation_subject", "uq_activation_transition_action",
            "uq_activation_transition_revision", "uq_activation_transition_status_event", "uq_trial_grant_subject", "uq_trial_grant_source");
    private static final Set<String> ROLES = Set.of("ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR");
    private final TransactionTemplate transaction;
    private final TransactionTemplate mandatory;
    private final DataSourceTransactionManager manager;
    private final DSLContext context;
    private final SubjectActivationRepository repository;
    private final ActivationTrialService trialService;
    public DefaultCustomerSubjectActivationService(TransactionTemplate transaction, DSLContext context,
            SubjectActivationRepository repository, ActivationTrialService trialService) {
        this.transaction = transaction;
        this.context = context;
        this.repository = repository;
        this.trialService = trialService;
        if (!(transaction.getTransactionManager() instanceof DataSourceTransactionManager configured)) throw failure(Code.SERVICE_UNAVAILABLE);
        manager = configured;
        mandatory = new TransactionTemplate(manager);
        mandatory.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
    }
    private record ActivationDecision(ActivationResult result) { }
    @Override public ActivationResult activate(ActivationCommand command, ActivationCaller caller) {
        validate(command, caller);
        ActivationDecision decision = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                decision = transaction.execute(status -> recordClientActivation(command, caller));
                break;
            } catch (RuntimeException problem) {
                if (retryable(problem)) { if (attempt == 2) throw failure(Code.SERVICE_UNAVAILABLE); continue; }
                throw mapped(problem);
            }
        }
        if (decision == null) throw failure(Code.INTERNAL);
        if (decision.result() == null) throw failure(Code.ACTIVATION_CONSENT_DECLINED);
        return decision.result();
    }
    private ActivationDecision recordClientActivation(ActivationCommand command, ActivationCaller caller) {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || manager.getDataSource() == null
                || !TransactionSynchronizationManager.hasResource(manager.getDataSource())) throw failure(Code.SERVICE_UNAVAILABLE);
        return mandatory.execute(status -> execute(command, caller));
    }
    private ActivationDecision execute(ActivationCommand command, ActivationCaller caller) {
        repository.enter(context);
        repository.lock(context, "subject_number.subject:" + command.subjectId());
        Optional<Head> currentHead = repository.lockHead(context, command.subjectId());
        Optional<Subject> initialSubject = repository.subject(context, command.subjectId());
        if (caller.workspaceType() == WorkspaceType.ORGANIZATION) {
            repository.lock(context, "organization_access.organization:" + caller.organizationId());
            repository.lock(context, "organization_access.membership:" + caller.organizationId() + ":" + caller.accountId());
        }
        repository.lock(context, "customer_iam.account:" + caller.accountId());
        Optional<Subject> subject = repository.subject(context, command.subjectId());
        Access access = repository.access(context, caller);
        if (!"ACTIVE".equals(access.accountStatus()) || caller.workspaceType() == WorkspaceType.ORGANIZATION
                && (!"ACTIVE".equals(access.organizationStatus()) || !"ACTIVE".equals(access.membershipStatus()) || !ROLES.contains(access.membershipRole()))) throw failure(Code.FORBIDDEN);
        if (!initialSubject.equals(subject)) throw failure(Code.INTERNAL);
        if (subject.isEmpty()) throw failure(Code.NOT_FOUND);
        Subject owner = subject.get();
        if (owner.ownerAccountId() != null && owner.ownerOrganizationId() != null) throw failure(Code.INTERNAL);
        if (caller.workspaceType() == WorkspaceType.PERSONAL ? !caller.accountId().equals(owner.ownerAccountId()) || owner.ownerOrganizationId() != null
                : !caller.organizationId().equals(owner.ownerOrganizationId()) || owner.ownerAccountId() != null) throw failure(Code.NOT_FOUND);
        String fingerprint = ActivationSemantics.consentFingerprint(command, caller);
        Optional<Consent> existing = repository.consent(context, command.activationEventId());
        Optional<Transition> ownTransition = repository.actionTransition(context, command.activationEventId());
        if (existing.isPresent()) return replay(command, caller, fingerprint, existing.get(), ownTransition, currentHead);
        if (ownTransition.isPresent()) throw failure(Code.INTERNAL);
        PolicyResult policyResult = trialService.readCurrentPolicy();
        repository.assertRole(context);
        if (policyResult == null || !policyResult.valid()) throw failure(Code.CONSENT_REQUIRED);
        CurrentPolicy policy = policyResult.policy();
        if (!command.policyCode().equals(policy.policyCode()) || !command.policyVersion().equals(policy.policyVersion())
                || !command.promptVersion().equals(policy.promptVersion()) || !command.contentSha256().equals(policy.contentSha256())) throw failure(Code.CONSENT_REQUIRED);
        UUID consentId = UUID.randomUUID();
        if (command.decision() == Decision.DECLINE) {
            appendConsent(command, caller, consentId, fingerprint, null);
            repository.assertRole(context);
            return new ActivationDecision(null);
        }
        if (currentHead.isPresent() && "SUSPENDED".equals(currentHead.get().status())) throw failure(Code.FORBIDDEN);
        Optional<OriginalTrial> original = trialService.readOriginalTrial(command.subjectId());
        repository.assertRole(context);
        if (original.isPresent() && "ACTIVATION".equals(original.get().sourceActionDomain())
                && command.activationEventId().equals(original.get().sourceActionEventId())) throw failure(Code.INTERNAL);
        String outcome = original.isPresent() ? "ALREADY_GRANTED" : "GRANTED";
        Head head = currentHead.orElseGet(() -> new Head(UUID.randomUUID(), command.subjectId(), "INACTIVE", 1));
        Instant occurredAt = Instant.now();
        boolean transitionNeeded = "INACTIVE".equals(head.status());
        Transition anchor;
        if (transitionNeeded) {
            int revision = Math.addExact(head.revision(), 1);
            String statusEvent = ActivationSemantics.statusEvent(head.id(), revision);
            anchor = new Transition(UUID.randomUUID(), head.id(), command.subjectId(), "INACTIVE", "ACTIVE", head.revision(), revision, caller.channel().name(),
                    "ACTIVATION", command.activationEventId(), "ACTIVATION_STATUS", statusEvent, ActivationSemantics.transitionFingerprint(command, caller, fingerprint, statusEvent),
                    caller.accountId(), null, "CLIENT_ACTIVATION_ACCEPTED", consentId, occurredAt);
        } else {
            if (!"ACTIVE".equals(head.status()) || original.isEmpty()) throw failure(Code.INTERNAL);
            anchor = repository.latestTransition(context, head).orElseThrow(() -> failure(Code.INTERNAL));
            validateAnchor(anchor, head, head.revision(), command.subjectId());
        }
        var descriptor = new ActivationSemantics.Descriptor(anchor.toRevision(), anchor.id(), outcome, policy.id(), command.contentSha256());
        appendConsent(command, caller, consentId, fingerprint, descriptor);
        if (currentHead.isEmpty()) repository.createHead(context, head, occurredAt);
        if (transitionNeeded) {
            if (!repository.advance(context, head, occurredAt)) {
                repository.lockHead(context, command.subjectId());
                throw failure(Code.INTERNAL);
            }
            repository.appendTransition(context, anchor);
        }
        String trialFingerprint = ActivationSemantics.trialFingerprint(command, caller, fingerprint, policy.id());
        TrialIssueResult issued = trialService.issueFirstTrial(new TrialIssueCommand(command.subjectId(), policy.id(), command.activationEventId(), trialFingerprint));
        repository.assertRole(context);
        if (issued == null || !outcome.equals(issued.outcome()) || issued.originalTrial() == null || !command.subjectId().equals(issued.originalTrial().subjectId())
                || original.isPresent() && !original.get().equals(issued.originalTrial())) throw failure(Code.INTERNAL);
        validateTrial(command, caller, fingerprint, descriptor, issued.originalTrial());
        if (transitionNeeded) repository.appendOutbox(context, anchor);
        repository.assertRole(context);
        return new ActivationDecision(result(consentId, descriptor, issued.originalTrial()));
    }
    private void appendConsent(ActivationCommand command, ActivationCaller caller, UUID consentId, String fingerprint, ActivationSemantics.Descriptor descriptor) {
        String snapshot = ActivationSemantics.descriptor(command, descriptor);
        repository.appendConsent(context, new Consent(consentId, command.subjectId(), "SERVICE_ACTIVATION", caller.channel().name(), "ACTIVATION", command.activationEventId(),
                command.policyCode(), command.policyVersion(), command.promptVersion(), command.decision().name(), "ACCOUNT", ActivationSemantics.sourceRef(command, caller), caller.accountId(),
                null, null, snapshot, ActivationSemantics.sha(snapshot), fingerprint));
    }
    private ActivationDecision replay(ActivationCommand command, ActivationCaller caller, String fingerprint, Consent consent, Optional<Transition> ownTransition, Optional<Head> head) {
        if (!command.subjectId().equals(consent.subjectId()) || !caller.accountId().equals(consent.accountId()) || !caller.channel().name().equals(consent.channel())) throw failure(Code.CONSENT_REQUIRED);
        if (!Set.of("ACCEPT", "DECLINE").contains(consent.action()) || !"SERVICE_ACTIVATION".equals(consent.purpose()) || !"ACTIVATION".equals(consent.eventDomain())
                || !command.activationEventId().equals(consent.eventId()) || !"ACCOUNT".equals(consent.sourceType()) || consent.employeeId() != null || consent.ivrSessionId() != null) throw failure(Code.INTERNAL);
        var storedCommand = new ActivationCommand(consent.subjectId(), consent.eventId(), Decision.valueOf(consent.action()), consent.policyCode(), consent.policyVersion(), consent.promptVersion(), "", command.traceId());
        if (!ActivationSemantics.sourceRef(storedCommand, caller).equals(consent.sourceRef()) || !ActivationSemantics.consentFingerprint(storedCommand, caller).equals(consent.fingerprint())) throw failure(Code.INTERNAL);
        boolean accepted = "ACCEPT".equals(consent.action());
        var descriptor = ActivationSemantics.decode(consent.snapshot(), consent.snapshotSha256(), consent.policyCode(), consent.policyVersion(), consent.promptVersion(), accepted);
        if (!fingerprint.equals(consent.fingerprint()) || !command.contentSha256().equals(descriptor.contentSha256())) throw failure(Code.CONSENT_REQUIRED);
        if (!accepted) {
            if (ownTransition.isPresent()) throw failure(Code.INTERNAL);
            repository.assertRole(context);
            return new ActivationDecision(null);
        }
        Head current = head.orElseThrow(() -> failure(Code.INTERNAL));
        Transition anchor = repository.transition(context, descriptor.transitionId()).orElseThrow(() -> failure(Code.INTERNAL));
        validateAnchor(anchor, current, descriptor.revision(), command.subjectId());
        if (ownTransition.isPresent()) {
            Transition own = ownTransition.get();
            if (!own.equals(anchor) || !consent.id().equals(own.consentId()) || !caller.accountId().equals(own.accountId()) || own.employeeId() != null
                    || !caller.channel().name().equals(own.channel()) || !"INACTIVE".equals(own.fromStatus()) || !"CLIENT_ACTIVATION_ACCEPTED".equals(own.reason())
                    || !ActivationSemantics.transitionFingerprint(command, caller, fingerprint, own.statusEvent()).equals(own.fingerprint())) throw failure(Code.INTERNAL);
        } else if (consent.id().equals(anchor.consentId()) || "GRANTED".equals(descriptor.outcome()) || "REPLAY".equals(descriptor.outcome())) throw failure(Code.INTERNAL);
        OriginalTrial original = trialService.readOriginalTrial(command.subjectId()).orElseThrow(() -> failure(Code.INTERNAL));
        repository.assertRole(context);
        validateTrial(command, caller, fingerprint, descriptor, original);
        return new ActivationDecision(result(consent.id(), descriptor, original));
    }
    private static void validateAnchor(Transition anchor, Head head, int revision, UUID subjectId) {
        if (!subjectId.equals(head.subjectId()) || !subjectId.equals(anchor.subjectId()) || !head.id().equals(anchor.activationId()) || !"ACTIVE".equals(anchor.toStatus())
                || anchor.toRevision() != revision || anchor.fromRevision() < 0 || anchor.toRevision() != anchor.fromRevision() + 1 || head.revision() < revision
                || !"ACTIVATION_STATUS".equals(anchor.statusDomain()) || !ActivationSemantics.statusEvent(head.id(), revision).equals(anchor.statusEvent())) throw failure(Code.INTERNAL);
    }
    private static void validateTrial(ActivationCommand command, ActivationCaller caller, String fingerprint, ActivationSemantics.Descriptor descriptor, OriginalTrial original) {
        boolean sameAction = "ACTIVATION".equals(original.sourceActionDomain()) && command.activationEventId().equals(original.sourceActionEventId());
        if (!command.subjectId().equals(original.subjectId()) || original.grantedSeconds() < 1 || original.expiresAt() == null || original.grantedAt() == null
                || !original.expiresAt().isAfter(original.grantedAt()) || original.trialGrantId() == null || original.entitlementGrantId() == null || original.balanceId() == null
                || ("ALREADY_GRANTED".equals(descriptor.outcome()) == sameAction) || sameAction && (!descriptor.policyId().equals(original.trialPolicyVersionId())
                || !ActivationSemantics.trialFingerprint(command, caller, fingerprint, descriptor.policyId()).equals(original.requestFingerprint()))) throw failure(Code.INTERNAL);
    }
    private static ActivationResult result(UUID consentId, ActivationSemantics.Descriptor descriptor, OriginalTrial original) {
        return new ActivationResult("ACTIVE", descriptor.revision(), descriptor.transitionId(), consentId, descriptor.outcome(), new ActivationTrial(original.grantedSeconds(), original.expiresAt()));
    }
    private static void validate(ActivationCommand command, ActivationCaller caller) {
        if (caller == null || caller.accountId() == null || caller.channel() == null || caller.workspaceType() == null
                || (caller.workspaceType() == WorkspaceType.PERSONAL) != (caller.organizationId() == null)) throw failure(Code.SERVICE_UNAVAILABLE);
        if (command == null || command.subjectId() == null || command.decision() == null || !text(command.activationEventId(), 128)
                || !text(command.policyCode(), 64) || !text(command.policyVersion(), 64) || !text(command.promptVersion(), 64)
                || command.contentSha256() == null || !command.contentSha256().matches("[0-9a-f]{64}")) throw failure(Code.VALIDATION_FAILED);
    }
    private static boolean text(String value, int limit) { return value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= limit && value.indexOf('\0') < 0 && StandardCharsets.UTF_8.newEncoder().canEncode(value); }
    private static SQLException sql(Throwable failure) {
        SQLException database = null;
        for (int depth = 0; failure != null && depth < 20; depth++, failure = failure.getCause()) if (failure instanceof SQLException current) database = current;
        return database;
    }
    private static boolean retryable(RuntimeException problem) {
        SQLException database = sql(problem);
        if (database == null) return false;
        String state = database.getSQLState();
        if ("40001".equals(state) || "40P01".equals(state)) return true;
        if (!"23505".equals(state) || database.getMessage() == null) return false;
        return SERIALIZATION_KEYS.stream().anyMatch(key -> database.getMessage().contains("constraint \"" + key + "\""));
    }
    private static SubjectActivationException mapped(RuntimeException problem) {
        if (problem instanceof SubjectActivationException known) return known;
        if (problem instanceof Failure known) return failure(switch (known.kind()) {
            case INVALID_POLICY, ACTION_CONFLICT -> Code.CONSENT_REQUIRED;
            case INTEGRITY -> Code.INTERNAL;
            case UNAVAILABLE -> Code.SERVICE_UNAVAILABLE;
        });
        SQLException database = sql(problem);
        if (database != null) {
            String state = Objects.toString(database.getSQLState(), "");
            String message = Objects.toString(database.getMessage(), "");
            if ("P0001".equals(state) && message.length() <= 8192 && (Set.of("TRIAL_POLICY_NOT_FOUND", "TRIAL_ACTION_CONFLICT").contains(message)
                    || message.lines().limit(64).map(String::strip).anyMatch(line -> Set.of("ERROR: TRIAL_POLICY_NOT_FOUND", "ERROR: TRIAL_ACTION_CONFLICT").contains(line)))) return failure(Code.CONSENT_REQUIRED);
            if (state.startsWith("08") || state.startsWith("28") || Set.of("42501", "0L000", "53300", "53400", "57014", "55P03", "57P01", "57P02", "57P03").contains(state)) return failure(Code.SERVICE_UNAVAILABLE);
        }
        if (problem instanceof org.springframework.transaction.CannotCreateTransactionException || problem instanceof org.springframework.transaction.TransactionTimedOutException) return failure(Code.SERVICE_UNAVAILABLE);
        return failure(Code.INTERNAL);
    }
    private static SubjectActivationException failure(Code code) { return new SubjectActivationException(code); }
}
