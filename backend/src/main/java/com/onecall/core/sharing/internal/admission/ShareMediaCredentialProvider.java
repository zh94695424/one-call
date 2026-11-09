package com.onecall.core.sharing.internal.admission;

import com.fasterxml.jackson.annotation.JsonValue;
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

    record AuthorizedMediaFacts(UUID evidenceId, UUID mediaObjectId, String sha256, long sizeBytes,
            String mimeType, int durationSeconds, long windowStartNanos, long windowEndNanos) {
        public AuthorizedMediaFacts {
            if (evidenceId == null || mediaObjectId == null || sha256 == null || !sha256.matches("[0-9a-f]{64}")
                    || sizeBytes <= 0 || mimeType == null || mimeType.isBlank() || durationSeconds <= 0
                    || windowStartNanos != 0 || windowEndNanos != Math.multiplyExact((long) durationSeconds, 1_000_000_000L))
                throw new IllegalArgumentException("Invalid authorized media facts");
        }
        @JsonValue public String redacted() { return "[REDACTED]"; }
        @Override public String toString() { return redacted(); }
    }
    record ReplayAnchor(long issuedAtMicros, long notBeforeMicros, String kid) {
        public ReplayAnchor {
            if (issuedAtMicros != notBeforeMicros || kid == null || !kid.matches("[A-Za-z0-9_-]{1,32}"))
                throw new IllegalArgumentException("Invalid replay anchor");
        }
        @JsonValue public String redacted() { return "[REDACTED]"; }
        @Override public String toString() { return redacted(); }
    }
    record MediaAuthorizationCredentialRequest(UUID shareGrantId, UUID shareAdmissionId, String purpose, String eventType,
            String requestKeyDigest, int codeGeneration, int revocationGeneration, Instant grantExpiresAt,
            Instant admissionExpiresAt, Instant sessionExpiresAt, Instant credentialExpiresAt,
            String preProviderFingerprint, String eventId, AuthorizedMediaFacts mediaFacts, ReplayAnchor priorReplayAnchor) {
        public MediaAuthorizationCredentialRequest {
            if (shareGrantId == null || shareAdmissionId == null || mediaFacts == null
                    || requestKeyDigest == null || !requestKeyDigest.matches("[0-9a-f]{64}")
                    || preProviderFingerprint == null || !preProviderFingerprint.matches("[0-9a-f]{64}")
                    || eventId == null || eventId.isBlank() || codeGeneration < 0 || revocationGeneration < 0
                    || grantExpiresAt == null || admissionExpiresAt == null || sessionExpiresAt == null || credentialExpiresAt == null
                    || !("PLAYBACK".equals(purpose) && "PLAYBACK_AUTHORIZED".equals(eventType)
                        || "SEEK".equals(purpose) && "SEEK_AUTHORIZED".equals(eventType)
                        || "RENEW".equals(purpose) && "RENEWED".equals(eventType)))
                throw new IllegalArgumentException("Invalid media authorization request");
        }
        @JsonValue public String redacted() { return "[REDACTED]"; }
        @Override public String toString() { return redacted(); }
    }
    sealed interface MediaAuthorizationCredentialResult permits MediaAuthorizationCredentialResult.Issued,
            MediaAuthorizationCredentialResult.Unavailable, MediaAuthorizationCredentialResult.Malformed {
        record Issued(String credential, Instant expiresAt, ReplayAnchor replayAnchor) implements MediaAuthorizationCredentialResult {
            @JsonValue public String redacted() { return "[REDACTED]"; }
            @Override public String toString() { return redacted(); }
        }
        record Unavailable() implements MediaAuthorizationCredentialResult { }
        record Malformed() implements MediaAuthorizationCredentialResult { }
    }
}
