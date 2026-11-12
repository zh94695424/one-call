package com.onecall.core.subject_number.customerclaim;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public interface CustomerSubjectClaimService {
    ClaimResult create(ClaimCommand command, PersonalClaimCaller caller);

    record PersonalClaimCaller(UUID accountId) {
        public PersonalClaimCaller {
            if (accountId == null) throw new SubjectClaimException(SubjectClaimException.Code.UNAUTHENTICATED);
        }
    }

    record ClaimCommand(UUID subjectId, UUID bindingId, VerificationMethod verificationMethod,
                        String verificationProofId, String requestId, UUID traceId) {
        public ClaimCommand {
            if (subjectId == null || bindingId == null || verificationMethod == null || traceId == null
                    || !opaque(verificationProofId) || !opaque(requestId))
                throw new SubjectClaimException(SubjectClaimException.Code.VALIDATION_FAILED);
        }
        private static boolean opaque(String value) {
            return value != null && !value.isBlank()
                    && value.codePointCount(0, value.length()) <= 128 && value.indexOf('\0') < 0
                    && StandardCharsets.UTF_8.newEncoder().canEncode(value);
        }
        @Override public String toString() { return "ClaimCommand[REDACTED]"; }
    }

    enum VerificationMethod { SMS, WECHAT_PHONE, CALLER_CHALLENGE, MANUAL_REVIEW }
    enum ClaimStatus { ACCEPTED, REJECTED, CONFLICT }
    record ClaimResult(UUID claimId, ClaimStatus status, UUID subjectId, Instant verifiedAt) {
        public ClaimResult {
            if (claimId == null || status == null || subjectId == null
                    || (status == ClaimStatus.ACCEPTED) != (verifiedAt != null))
                throw new SubjectClaimException(SubjectClaimException.Code.INTERNAL);
        }
    }
}
