package com.onecall.core.operations_iam.internal.security;

public final class EmployeeSessionVerificationException extends RuntimeException {
    public enum Code { UNAUTHENTICATED, SERVICE_UNAVAILABLE }
    private final Code code;
    public EmployeeSessionVerificationException(Code code) { super("Employee verification failed", null, false, false); this.code = code; }
    public Code code() { return code; }
    @Override public String toString() { return "EmployeeSessionVerificationException[REDACTED]"; }
}
