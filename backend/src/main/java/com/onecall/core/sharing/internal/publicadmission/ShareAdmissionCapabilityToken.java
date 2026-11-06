package com.onecall.core.sharing.internal.publicadmission;

import java.nio.charset.StandardCharsets;

public final class ShareAdmissionCapabilityToken {
    private final byte[] bytes;

    public ShareAdmissionCapabilityToken(String value) {
        if (value == null || value.isEmpty() || value.codePointCount(0, value.length()) > 512
                || value.indexOf('\0') >= 0 || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) {
            throw new ShareAdmissionException(ShareAdmissionException.Code.VALIDATION_FAILED);
        }
        bytes = value.getBytes(StandardCharsets.UTF_8);
    }

    public byte[] utf8() { return bytes.clone(); }

    @Override
    public String toString() { return "[REDACTED]"; }
}
