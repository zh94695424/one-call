package com.onecall.core.activation_consent.internal.operations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

public interface OperationsLifecycleRepository {
    record Head(UUID id, UUID subjectId, String status, int revision, Instant createdAt, Instant updatedAt) { }
    record Transition(UUID id, UUID activationId, UUID subjectId, String fromStatus, String toStatus, int fromRevision, int toRevision,
            String channel, String actionDomain, String actionEvent, String statusDomain, String statusEvent, String fingerprint,
            UUID accountId, UUID employeeId, String reason, UUID consentId, Instant occurredAt) {
        @Override public String toString() { return "Transition[REDACTED]"; }
    }
    void enter(DSLContext context);
    void assertRole(DSLContext context);
    void lockSubject(DSLContext context, UUID subjectId);
    Optional<Head> lockHead(DSLContext context, UUID subjectId);
    boolean subjectExists(DSLContext context, UUID subjectId);
    Optional<Transition> actionTransition(DSLContext context, String eventId);
    Optional<Transition> latestTransition(DSLContext context, Head head);
    Instant decisionTime(DSLContext context);
    int advance(DSLContext context, Head head, String target, Instant occurredAt);
    void appendTransition(DSLContext context, Transition transition);
    void appendOutbox(DSLContext context, Transition transition);
    void appendAudit(DSLContext context, Transition transition);
}
