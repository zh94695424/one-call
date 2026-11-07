package com.onecall.core.sharing.internal.admission;

import java.util.UUID;

@FunctionalInterface
public interface DesignatedMobileProofVerifier {
    Result verify(MobileProofRequest request);

    record MobileProofRequest(UUID shareGrantId, String shareId, String action, String mobileHmac,
            String keyVersion, String verificationProofId) {
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
