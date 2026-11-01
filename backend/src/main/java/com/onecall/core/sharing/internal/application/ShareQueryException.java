package com.onecall.core.sharing.internal.application;

public final class ShareQueryException extends RuntimeException {
    public enum Code { FORBIDDEN, VALIDATION_FAILED, CURSOR_INVALID, SERVICE_UNAVAILABLE, INTERNAL }

    private final Code code;

    public ShareQueryException(Code code) {
        super(switch (code) {
            case FORBIDDEN -> "Creator context is not active";
            case VALIDATION_FAILED -> "Invalid share page request";
            case CURSOR_INVALID -> "Invalid share cursor";
            case SERVICE_UNAVAILABLE -> "Share query is unavailable";
            case INTERNAL -> "Share query failed";
        });
        this.code = code;
    }

    public Code code() { return code; }
}
