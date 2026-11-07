package com.onecall.core.sharing.internal.admission;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@FunctionalInterface
public interface ShareAdmissionTicketProvider {
    Result write(TicketWriteRequest request);

    default void expire(String key) { }

    record TicketWriteRequest(String key, UUID shareGrantId, UUID admissionId, int revocationGeneration,
            int codeGeneration, Instant sessionExpiresAt, Instant admissionExpiresAt, Instant grantExpiresAt,
            Instant expiresAt, Duration ttl, String credential) {
        @Override
        public String toString() { return "[REDACTED]"; }
    }
    sealed interface Result permits Written, Unavailable, Malformed { }
    record Written() implements Result { }
    record Unavailable() implements Result { }
    record Malformed() implements Result { }
}
