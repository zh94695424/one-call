package com.onecall.core.sharing.internal.publicadmission;

public record ShareAdmissionRequest(String admissionRequestId, Verification verification) {
    public sealed interface Verification {
        record None() implements Verification {
            @Override
            public String toString() { return "[REDACTED]"; }
        }
        record ExtractionCode(String code) implements Verification {
            @Override
            public String toString() { return "[REDACTED]"; }
        }
        record DesignatedMobile(String verificationProofId) implements Verification {
            @Override
            public String toString() { return "[REDACTED]"; }
        }
        record OrganizationMember() implements Verification {
            @Override
            public String toString() { return "[REDACTED]"; }
        }
    }

    @Override
    public String toString() { return "[REDACTED]"; }
}
