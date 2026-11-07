package com.onecall.core.sharing.internal.admission;

import java.time.Instant;
import java.util.UUID;

@FunctionalInterface
public interface ShareMediaCredentialProvider {
    Result issue(MediaCredentialRequest request);

    default MediaAuthorizationCredentialResult issueOrReplay(MediaAuthorizationCredentialRequest request) {
        return new MediaAuthorizationCredentialResult.Unavailable();
    }

    record MediaCredentialRequest(UUID shareGrantId, UUID admissionId, int revocationGeneration,
            int codeGeneration, Instant expiresAt) {
        @Override
        public String toString() { return "[REDACTED]"; }
    }
    sealed interface Result permits Issued, Unavailable, Malformed { }
    record Issued(String credential, Instant expiresAt) implements Result {
        @Override
        public String toString() { return "[REDACTED]"; }
    }
    record Unavailable() implements Result { }
    record Malformed() implements Result { }

    record MediaAuthorizationCredentialRequest(UUID shareGrantId, UUID shareAdmissionId, String purpose, String eventType,
            String requestKeyDigest, int codeGeneration, int revocationGeneration, Instant grantExpiresAt,
            Instant admissionExpiresAt, Instant sessionExpiresAt, Instant credentialExpiresAt,
            String preProviderFingerprint, String eventId) {
        @Override public String toString() { return "[REDACTED]"; }
    }
    sealed interface MediaAuthorizationCredentialResult permits MediaAuthorizationCredentialResult.Issued,
            MediaAuthorizationCredentialResult.Unavailable, MediaAuthorizationCredentialResult.Malformed {
        record Issued(String credential, Instant expiresAt) implements MediaAuthorizationCredentialResult {
            @Override public String toString() { return "[REDACTED]"; }
        }
        record Unavailable() implements MediaAuthorizationCredentialResult { }
        record Malformed() implements MediaAuthorizationCredentialResult { }
    }
}
