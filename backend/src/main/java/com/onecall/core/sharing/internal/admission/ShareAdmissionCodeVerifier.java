package com.onecall.core.sharing.internal.admission;

import java.util.UUID;

@FunctionalInterface
public interface ShareAdmissionCodeVerifier {
    Result verify(CodeVerificationRequest request);

    record CodeVerificationRequest(UUID shareGrantId, String code, String codeSalt, String codeHash,
            int codeGeneration, ShareAdmissionPolicyProvider.ApprovedTicketPolicy policy) {
        @Override
        public String toString() { return "[REDACTED]"; }
    }
    sealed interface Result permits Verified, Denied, Unavailable, Malformed { }
    record Verified() implements Result { }
    record Denied(String reasonCode) implements Result {
        @Override
        public String toString() { return "[REDACTED]"; }
    }
    record Unavailable() implements Result { }
    record Malformed() implements Result { }
}
