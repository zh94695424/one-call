package com.onecall.core.sharing.internal.command;

import java.util.Objects;

public final class ShareCommandException extends RuntimeException {
    private final Code code;

    public ShareCommandException(Code code) {
        super(Objects.requireNonNull(code).name());
        this.code = code;
    }

    public Code code() { return code; }

    public enum Code {
        UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, VALIDATION_FAILED, RATE_LIMITED,
        SERVICE_UNAVAILABLE, INTERNAL, SHARE_RISK_BLOCKED, SHARE_ACCESS_POLICY_UNAVAILABLE,
        SHARE_CREATION_CONFLICT, SHARE_TRANSITION_CONFLICT, SHARE_GENERATION_STALE, SHARE_REVOKED
    }
}
