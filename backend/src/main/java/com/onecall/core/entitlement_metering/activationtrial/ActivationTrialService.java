package com.onecall.core.entitlement_metering.activationtrial;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ActivationTrialService {
    PolicyResult readCurrentPolicy();
    Optional<OriginalTrial> readOriginalTrial(UUID subjectId);
    TrialIssueResult issueFirstTrial(TrialIssueCommand command);
    record PolicyResult(CurrentPolicy policy) {
        public boolean valid() { return policy != null; }
        @Override public String toString() { return "PolicyResult[REDACTED]"; }
    }
    record CurrentPolicy(UUID id, int versionNo, String policyCode, String policyVersion, String promptVersion,
            String contentSha256, String snapshotSha256) {
        @Override public String toString() { return "CurrentPolicy[REDACTED]"; }
    }
    record OriginalTrial(UUID trialGrantId, UUID entitlementGrantId, UUID balanceId, UUID subjectId,
            UUID trialPolicyVersionId, String sourceActionDomain, String sourceActionEventId, String requestFingerprint,
            long grantedSeconds, Instant grantedAt, Instant expiresAt) {
        @Override public String toString() { return "OriginalTrial[REDACTED]"; }
    }
    record TrialIssueCommand(UUID subjectId, UUID policyVersionId, String activationEventId, String requestFingerprint) {
        @Override public String toString() { return "TrialIssueCommand[REDACTED]"; }
    }
    record TrialIssueResult(String outcome, OriginalTrial originalTrial) {
        @Override public String toString() { return "TrialIssueResult[REDACTED]"; }
    }
    final class Failure extends RuntimeException {
        public enum Kind { INVALID_POLICY, ACTION_CONFLICT, INTEGRITY, UNAVAILABLE }
        private final Kind kind;
        public Failure(Kind kind) { super("Activation trial unavailable"); this.kind = kind; }
        public Kind kind() { return kind; }
    }
}
