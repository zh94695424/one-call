package com.onecall.core.sharing.internal.publicquery;

public final class PublicShareQueryException extends RuntimeException {
    public enum Code {
        UNAUTHENTICATED, FORBIDDEN, VALIDATION_FAILED, SHARE_TOKEN_INVALID, SHARE_REVOKED,
        SHARE_EXPIRED, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL
    }

    private final Code code;

    public PublicShareQueryException(Code code) {
        super(java.util.Objects.requireNonNull(code).name(), null, false, false);
        this.code = code;
    }

    public Code code() { return code; }
}
