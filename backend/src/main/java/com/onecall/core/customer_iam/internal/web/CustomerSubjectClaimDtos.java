package com.onecall.core.customer_iam.internal.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.ClaimStatus;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.VerificationMethod;
import java.time.Instant;
import java.util.UUID;

public final class CustomerSubjectClaimDtos {
    private CustomerSubjectClaimDtos() { }
    public record ClaimRequest(UUID subjectId, UUID bindingId, VerificationMethod verificationMethod, String verificationProofId) {
        @Override public String toString() { return "ClaimRequest[REDACTED]"; }
    }
    public record ClaimResult(UUID claimId, ClaimStatus status, UUID subjectId,
                              @JsonInclude(JsonInclude.Include.NON_NULL) Instant verifiedAt) { }
}
