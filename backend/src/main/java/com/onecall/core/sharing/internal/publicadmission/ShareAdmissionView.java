package com.onecall.core.sharing.internal.publicadmission;

import java.time.Instant;
import java.util.UUID;

public record ShareAdmissionView(UUID admissionId, String verificationMode, String verificationResult,
        Instant admittedAt, Instant sessionExpiresAt, Instant admissionExpiresAt, MediaAuthorization mediaAuthorization) {
    public record MediaAuthorization(String credential, Instant expiresAt) {
        @Override
        public String toString() { return "[REDACTED]"; }
    }

    @Override
    public String toString() { return "[REDACTED]"; }
}
