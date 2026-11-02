package com.onecall.core.customer_iam.internal.application;

public final class CustomerContextException extends RuntimeException {
    public enum Code { UNAUTHENTICATED, FORBIDDEN, VALIDATION_FAILED, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL, CURSOR_INVALID }
    private final Code code;

    public CustomerContextException(Code code) {
        super(code.name(), null, false, false);
        this.code = code;
    }

    public Code code() { return code; }
}
