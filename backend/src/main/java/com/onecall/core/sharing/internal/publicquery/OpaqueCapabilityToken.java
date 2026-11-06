package com.onecall.core.sharing.internal.publicquery;

import java.nio.charset.StandardCharsets;

public final class OpaqueCapabilityToken {
    private final String value;

    public OpaqueCapabilityToken(String value) {
        if (value == null || value.codePointCount(0, value.length()) < 1
                || value.codePointCount(0, value.length()) > 512 || value.indexOf('\0') >= 0
                || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) {
            throw new PublicShareQueryException(PublicShareQueryException.Code.VALIDATION_FAILED);
        }
        this.value = value;
    }

    byte[] utf8() { return value.getBytes(StandardCharsets.UTF_8); }

    @Override
    public String toString() { return "[REDACTED]"; }
}
