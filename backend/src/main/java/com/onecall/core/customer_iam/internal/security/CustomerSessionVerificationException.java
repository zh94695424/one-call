package com.onecall.core.customer_iam.internal.security;

public final class CustomerSessionVerificationException extends RuntimeException {
    public enum Code { UNAUTHENTICATED, SERVICE_UNAVAILABLE }
    private final Code code;

    public CustomerSessionVerificationException(Code code) {
        super(code.name(), null, false, false);
        this.code = code;
    }

    public Code code() { return code; }
}
