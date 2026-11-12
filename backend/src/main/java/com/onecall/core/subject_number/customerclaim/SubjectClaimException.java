package com.onecall.core.subject_number.customerclaim;

public final class SubjectClaimException extends RuntimeException {
    public enum Code { UNAUTHENTICATED, FORBIDDEN, VALIDATION_FAILED, CLAIM_RISK_BLOCKED,
        CLAIM_REPLAY_CONFLICT, CLAIM_CONTEXT_CONFLICT, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL }
    private final Code code;

    public SubjectClaimException(Code code) {
        super(code.name(), null, false, false);
        this.code = code;
    }

    public Code code() { return code; }
}
