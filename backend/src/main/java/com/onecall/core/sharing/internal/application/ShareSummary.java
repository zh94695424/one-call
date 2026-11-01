package com.onecall.core.sharing.internal.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ShareSummary(String shareId, UUID evidenceId, VerificationMode verificationMode,
        ShareStatus status, Instant validFrom, Instant expiresAt, Integer maxAdmissions,
        int admissionsUsed, boolean downloadAllowed, int codeGeneration, int revocationGeneration) {

    public ShareSummary {
        Objects.requireNonNull(shareId);
        Objects.requireNonNull(evidenceId);
        Objects.requireNonNull(verificationMode);
        Objects.requireNonNull(status);
        Objects.requireNonNull(validFrom);
        Objects.requireNonNull(expiresAt);
        if (shareId.codePointCount(0, shareId.length()) < 1
                || shareId.codePointCount(0, shareId.length()) > 128 || shareId.replace(" ", "").isEmpty()
                || !expiresAt.isAfter(validFrom) || admissionsUsed < 0
                || (maxAdmissions != null && (maxAdmissions < 1 || admissionsUsed > maxAdmissions))
                || codeGeneration < 0 || revocationGeneration < 0
                || (verificationMode == VerificationMode.EXTRACTION_CODE ? codeGeneration < 1 : codeGeneration != 0)) {
            throw new IllegalArgumentException("Invalid share metadata");
        }
    }

    public enum VerificationMode { NONE, EXTRACTION_CODE, DESIGNATED_MOBILE, ORGANIZATION_MEMBER }
    public enum ShareStatus { ACTIVE, EXPIRED, REVOKED, LOCKED }
}
