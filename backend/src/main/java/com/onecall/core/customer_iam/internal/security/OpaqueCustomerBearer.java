package com.onecall.core.customer_iam.internal.security;

public final class OpaqueCustomerBearer {
    private final String value;

    OpaqueCustomerBearer(String value) { this.value = value; }

    public String value() { return value; }

    @Override
    public String toString() { return "[REDACTED]"; }
}
