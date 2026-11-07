package com.onecall.core.sharing.internal.admission;

import java.time.Instant;
import java.util.UUID;

@FunctionalInterface
public interface ShareMediaCredentialProvider {
    Result issue(MediaCredentialRequest request);

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
}
