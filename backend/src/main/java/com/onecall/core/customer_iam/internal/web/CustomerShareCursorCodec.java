package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.sharing.internal.application.ShareCursor;
import com.onecall.core.sharing.internal.application.TrustedShareCaller;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

final class CustomerShareCursorCodec {
    private static final String PREFIX = "onecall:customer-shares:v1\nvalidFrom:desc,shareId:desc\n";
    private static final int MAX_LENGTH = 512;

    String encode(TrustedShareCaller caller, ShareCursor cursor) {
        if (!validCaller(caller) || cursor == null || !caller.accountId().equals(cursor.accountId())
                || !caller.organizationId().equals(cursor.organizationId()) || !validAnchor(cursor.validFrom(), cursor.shareId())) {
            throw new IllegalArgumentException("Invalid cursor");
        }
        int[] scalars = cursor.shareId().codePoints().toArray();
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(1);
        payload.writeBytes(binding(caller));
        payload.writeBytes(ByteBuffer.allocate(12).putLong(cursor.validFrom().getEpochSecond())
                .putInt(cursor.validFrom().getNano()).array());
        payload.write(scalars.length);
        int bits = 0;
        int count = 0;
        for (int scalar : scalars) {
            for (int shift = 20; shift >= 0; shift--) {
                bits = (bits << 1) | ((scalar >>> shift) & 1);
                if (++count == 8) {
                    payload.write(bits);
                    bits = 0;
                    count = 0;
                }
            }
        }
        if (count > 0) payload.write(bits << (8 - count));
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray());
        if (encoded.length() > MAX_LENGTH) throw new IllegalArgumentException("Invalid cursor");
        return encoded;
    }

    ShareCursor decode(TrustedShareCaller caller, String value) {
        try {
            if (!validCaller(caller) || value == null || value.isEmpty() || value.length() > MAX_LENGTH
                    || !value.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException();
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            if (bytes.length > 384 || !Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value)
                    || bytes.length < 47 || bytes[0] != 1) throw new IllegalArgumentException();
            if (!MessageDigest.isEqual(binding(caller), java.util.Arrays.copyOfRange(bytes, 1, 33)))
                throw new IllegalArgumentException();
            ByteBuffer header = ByteBuffer.wrap(bytes);
            header.position(33);
            long seconds = header.getLong();
            int nanos = header.getInt();
            int length = Byte.toUnsignedInt(header.get());
            if (nanos < 0 || nanos >= 1_000_000_000 || nanos % 1000 != 0
                    || length < 1 || length > 128 || bytes.length != 46 + (21 * length + 7) / 8)
                throw new IllegalArgumentException();
            byte[] packed = java.util.Arrays.copyOfRange(bytes, 46, bytes.length);
            int[] scalars = new int[length];
            int bitIndex = 0;
            for (int index = 0; index < length; index++) {
                int scalar = 0;
                for (int bit = 0; bit < 21; bit++, bitIndex++) {
                    scalar = (scalar << 1) | ((packed[bitIndex / 8] >>> (7 - bitIndex % 8)) & 1);
                }
                if (scalar > Character.MAX_CODE_POINT || scalar >= Character.MIN_SURROGATE && scalar <= Character.MAX_SURROGATE
                        || scalar == 0) throw new IllegalArgumentException();
                scalars[index] = scalar;
            }
            if (bitIndex % 8 != 0 && (packed[packed.length - 1] & ((1 << (8 - bitIndex % 8)) - 1)) != 0)
                throw new IllegalArgumentException();
            StringBuilder id = new StringBuilder();
            for (int scalar : scalars) id.appendCodePoint(scalar);
            var instant = java.time.Instant.ofEpochSecond(seconds, nanos);
            if (!validAnchor(instant, id.toString())) throw new IllegalArgumentException();
            ShareCursor cursor = new ShareCursor(caller.accountId(), caller.organizationId(), instant, id.toString());
            if (!encode(caller, cursor).equals(value)) throw new IllegalArgumentException();
            return cursor;
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    private static boolean validCaller(TrustedShareCaller caller) {
        return caller != null && caller.accountId() != null && caller.organizationId() != null;
    }

    private static byte[] binding(TrustedShareCaller caller) {
        String scope = caller.organizationId().map(id -> "ORGANIZATION:" + id.toString().toLowerCase(java.util.Locale.ROOT))
                .orElse("PERSONAL");
        try {
            return MessageDigest.getInstance("SHA-256").digest((PREFIX + caller.accountId().toString().toLowerCase(java.util.Locale.ROOT)
                    + "\n" + scope).getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static boolean validAnchor(java.time.Instant instant, String shareId) {
        if (instant == null || shareId == null || shareId.codePointCount(0, shareId.length()) < 1
                || shareId.codePointCount(0, shareId.length()) > 128 || shareId.replace(" ", "").isEmpty()
                || shareId.indexOf('\0') >= 0 || !StandardCharsets.UTF_8.newEncoder().canEncode(shareId)
                || instant.getNano() % 1000 != 0) return false;
        try {
            int year = instant.atOffset(ZoneOffset.UTC).getYear();
            return year >= -4712 && year <= 294276;
        } catch (DateTimeException failure) {
            return false;
        }
    }
}
