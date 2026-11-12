package com.onecall.core.subject_number.internal.claim;

public interface SubjectClaimReferenceCipher {
    SubjectClaimProviderResult<SealedReference> seal(byte[] canonicalV1Payload);
    SubjectClaimProviderResult<byte[]> open(SealedReference reference);
    record SealedReference(String envelope) {
        @Override public String toString() { return "SealedReference[REDACTED]"; }
    }
}
