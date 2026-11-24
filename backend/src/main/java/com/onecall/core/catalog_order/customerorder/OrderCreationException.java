package com.onecall.core.catalog_order.customerorder;

public final class OrderCreationException extends RuntimeException {
    public enum Code {
        UNAUTHENTICATED, FORBIDDEN, VALIDATION_FAILED, IDEMPOTENCY_CONFLICT,
        ORDER_UNKNOWN_PAYMENT_PENDING, PURCHASE_CONSENT_REQUIRED, PURCHASE_CONSENT_STALE,
        PURCHASE_CONSENT_DECLINED, ACTIVATION_NOT_ACTIVE, REAL_NAME_REQUIRED,
        RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL
    }
    private final Code code;
    public OrderCreationException(Code code) { super(code.name()); this.code = code; }
    public Code code() { return code; }
}
