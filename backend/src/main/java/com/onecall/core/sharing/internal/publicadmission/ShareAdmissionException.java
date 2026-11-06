package com.onecall.core.sharing.internal.publicadmission;

public final class ShareAdmissionException extends RuntimeException {
    public enum Code {
        UNAUTHENTICATED, FORBIDDEN, VALIDATION_FAILED, SHARE_VERIFICATION_FAILED,
        SHARE_RISK_BLOCKED, SHARE_WRONG_TENANT, SHARE_TOKEN_INVALID, SHARE_REVOKED,
        SHARE_EXPIRED, SHARE_LOCKED, SHARE_ADMISSION_LIMIT_REACHED, SHARE_GENERATION_STALE,
        SHARE_SESSION_EXPIRED, SHARE_SESSION_POLICY_UNAVAILABLE, DEPENDENCY_UNAVAILABLE,
        RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL
    }

    private final Code code;

    public ShareAdmissionException(Code code) {
        super("Share admission unavailable");
        this.code = java.util.Objects.requireNonNull(code);
    }

    public Code code() { return code; }
}
