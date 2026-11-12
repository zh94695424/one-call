package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader.Anchor;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class CustomerEvidenceCursorCodec {
    private static final byte[] DOMAIN = "onecall:customer-evidence:cursor:v1\n".getBytes(StandardCharsets.UTF_8);
    private final byte[] key;

    public CustomerEvidenceCursorCodec(CustomerEvidenceCursorProperties properties) {
        try {
            String value = properties.getCursorHmacKey();
            byte[] decoded = Base64.getDecoder().decode(value);
            if (decoded.length != 32 || !Base64.getEncoder().encodeToString(decoded).equals(value))
                throw new IllegalArgumentException();
            key = decoded;
        } catch (RuntimeException failure) {
            throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
        }
    }

    public static Anchor validateStructure(String value) {
        try {
            if (value == null || value.length() != 124 || !value.matches("[A-Za-z0-9_-]+"))
                throw new IllegalArgumentException();
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            if (bytes.length != 93 || bytes[0] != 1 || !Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value))
                throw new IllegalArgumentException();
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.position(33);
            long seconds = buffer.getLong();
            int nanos = buffer.getInt();
            if (nanos < 0 || nanos >= 1_000_000_000 || nanos % 1000 != 0) throw new IllegalArgumentException();
            Instant time = Instant.ofEpochSecond(seconds, nanos);
            int year = time.atOffset(ZoneOffset.UTC).getYear();
            if (year < -4712 || year > 294276 || time.getEpochSecond() != seconds || time.getNano() != nanos)
                throw new IllegalArgumentException();
            return new Anchor(time, new UUID(buffer.getLong(), buffer.getLong()));
        } catch (RuntimeException failure) {
            throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
        }
    }

    public String encode(Scope scope, Anchor anchor) {
        try {
            ByteBuffer payload = ByteBuffer.allocate(93);
            payload.put((byte) 1).put(binding(scope)).putLong(anchor.createdAt().getEpochSecond())
                    .putInt(anchor.createdAt().getNano()).putLong(anchor.evidenceId().getMostSignificantBits())
                    .putLong(anchor.evidenceId().getLeastSignificantBits());
            payload.put(mac(Arrays.copyOf(payload.array(), 61)));
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
            validateStructure(encoded);
            return encoded;
        } catch (RuntimeException failure) {
            throw new CustomerContextException(CustomerContextException.Code.INTERNAL);
        }
    }

    public Anchor decode(Scope scope, String value) {
        Anchor anchor = validateStructure(value);
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        if (!MessageDigest.isEqual(mac(Arrays.copyOf(bytes, 61)), Arrays.copyOfRange(bytes, 61, 93))
                || !MessageDigest.isEqual(binding(scope), Arrays.copyOfRange(bytes, 1, 33)))
            throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
        return anchor;
    }

    private static byte[] binding(Scope scope) {
        try {
            String workspace = scope.organizationId() == null ? "PERSONAL" : "ORGANIZATION:" + scope.organizationId();
            return MessageDigest.getInstance("SHA-256").digest(("onecall:customer-evidence:v1\n"
                    + "createdAt:desc,evidenceId:desc\n" + scope.accountId() + "\n" + workspace + "\n")
                    .getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException failure) {
            throw new CustomerContextException(CustomerContextException.Code.INTERNAL);
        }
    }

    private byte[] mac(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update(DOMAIN);
            return mac.doFinal(payload);
        } catch (java.security.GeneralSecurityException failure) {
            throw new CustomerContextException(CustomerContextException.Code.INTERNAL);
        }
    }
}
