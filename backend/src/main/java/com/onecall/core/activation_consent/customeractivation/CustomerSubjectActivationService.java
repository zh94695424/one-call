package com.onecall.core.activation_consent.customeractivation;

import java.time.Instant;
import java.util.UUID;

public interface CustomerSubjectActivationService {
    ActivationResult activate(ActivationCommand command, ActivationCaller caller);
    enum Decision { ACCEPT, DECLINE }
    enum Channel { MINIAPP, WEB }
    enum WorkspaceType { PERSONAL, ORGANIZATION }
    record ActivationCaller(UUID accountId, Channel channel, WorkspaceType workspaceType, UUID organizationId) {
        @Override public String toString() { return "ActivationCaller[REDACTED]"; }
    }
    record ActivationCommand(UUID subjectId, String activationEventId, Decision decision, String policyCode,
            String policyVersion, String promptVersion, String contentSha256, UUID traceId) {
        @Override public String toString() { return "ActivationCommand[REDACTED]"; }
    }
    record ActivationResult(String activationStatus, int activationRevision, UUID transitionId, UUID consentId,
            String trialOutcome, ActivationTrial trial) { }
    record ActivationTrial(long grantedSeconds, Instant expiresAt) { }
}
