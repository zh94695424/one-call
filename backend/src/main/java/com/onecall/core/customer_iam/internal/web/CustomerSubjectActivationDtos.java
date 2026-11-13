package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.Decision;
import java.time.Instant;
import java.util.UUID;

public final class CustomerSubjectActivationDtos {
    private CustomerSubjectActivationDtos() { }
    public record ActivationRequest(String activationEventId, Decision decision, String policyCode, String policyVersion, String promptVersion, String contentSha256) {
        @Override public String toString() { return "ActivationRequest[REDACTED]"; }
    }
    public record ActivationResult(String activationStatus, int activationRevision, UUID transitionId, UUID consentId, String trialOutcome, ActivationTrial trial) { }
    public record ActivationTrial(long grantedSeconds, Instant expiresAt) { }
}
