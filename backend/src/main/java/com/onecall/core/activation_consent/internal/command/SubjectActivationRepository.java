package com.onecall.core.activation_consent.internal.command;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.ActivationCaller;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

public interface SubjectActivationRepository {
    record Head(UUID id, UUID subjectId, String status, int revision) { }
    record Subject(UUID id, UUID ownerAccountId, UUID ownerOrganizationId, int revision) { }
    record Access(String accountStatus, String organizationStatus, String membershipStatus, String membershipRole) { }
    record Consent(UUID id, UUID subjectId, String purpose, String channel, String eventDomain, String eventId,
            String policyCode, String policyVersion, String promptVersion, String action, String sourceType, String sourceRef,
            UUID accountId, UUID employeeId, UUID ivrSessionId, String snapshot, String snapshotSha256, String fingerprint) {
        @Override public String toString() { return "Consent[REDACTED]"; }
    }
    record Transition(UUID id, UUID activationId, UUID subjectId, String fromStatus, String toStatus, int fromRevision, int toRevision,
            String channel, String actionDomain, String actionEvent, String statusDomain, String statusEvent, String fingerprint,
            UUID accountId, UUID employeeId, String reason, UUID consentId, Instant occurredAt) {
        @Override public String toString() { return "Transition[REDACTED]"; }
    }
    void enter(DSLContext context);
    void assertRole(DSLContext context);
    void lock(DSLContext context, String key);
    Optional<Head> lockHead(DSLContext context, UUID subjectId);
    Optional<Subject> subject(DSLContext context, UUID subjectId);
    Access access(DSLContext context, ActivationCaller caller);
    Optional<Consent> consent(DSLContext context, String eventId);
    Optional<Transition> actionTransition(DSLContext context, String eventId);
    Optional<Transition> transition(DSLContext context, UUID transitionId);
    Optional<Transition> latestTransition(DSLContext context, Head head);
    void appendConsent(DSLContext context, Consent consent);
    void createHead(DSLContext context, Head head, Instant occurredAt);
    boolean advance(DSLContext context, Head head, Instant occurredAt);
    void appendTransition(DSLContext context, Transition transition);
    void appendOutbox(DSLContext context, Transition transition);
}
