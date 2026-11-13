package com.onecall.core.operations_iam.internal.security;

public final class OpaqueEmployeeBearer {
    private final String value;
    OpaqueEmployeeBearer(String value) { this.value = value; }
    public String value() { return value; }
    @Override public String toString() { return "OpaqueEmployeeBearer[REDACTED]"; }
}
