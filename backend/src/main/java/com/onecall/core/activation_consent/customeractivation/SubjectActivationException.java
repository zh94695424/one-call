package com.onecall.core.activation_consent.customeractivation;

public final class SubjectActivationException extends RuntimeException {
    public enum Code { UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, VALIDATION_FAILED, CONSENT_REQUIRED,
        ACTIVATION_CONSENT_DECLINED, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL }
    private final Code code;
    public SubjectActivationException(Code code) { super("Subject activation failed"); this.code = code; }
    public Code code() { return code; }
}
