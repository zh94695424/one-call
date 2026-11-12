package com.onecall.core.subject_number.internal.claim;

import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.VerificationMethod;
import java.util.UUID;

public interface SubjectClaimProofVerifier {
    SubjectClaimProviderResult<ApprovedProof> verify(ProofRequest request);
    record ProofRequest(UUID accountId, UUID subjectId, UUID bindingId,
                        VerificationMethod method, String verificationProofId) {
        @Override public String toString() { return "ProofRequest[REDACTED]"; }
    }
    record ApprovedProof(UUID accountId, UUID subjectId, UUID bindingId,
                         VerificationMethod method, String proofRef) {
        @Override public String toString() { return "ApprovedProof[REDACTED]"; }
    }
}
