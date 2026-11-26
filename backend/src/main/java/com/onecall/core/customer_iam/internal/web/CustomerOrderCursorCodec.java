package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.catalog_order.customerquery.CustomerOrderReader.Anchor;
import com.onecall.core.catalog_order.customerquery.CustomerOrderReader.Scope;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class CustomerOrderCursorCodec {
    private static final byte[] DOMAIN = "onecall:customer-orders:cursor:v1\n".getBytes(StandardCharsets.UTF_8);
    private final byte[] key;

    public CustomerOrderCursorCodec(CustomerCatalogOfferCursorProperties properties) {
        try {
            String value = properties.getCursorHmacKey();
            key = Base64.getDecoder().decode(value);
            if (key.length != 32 || !Base64.getEncoder().encodeToString(key).equals(value))
                throw new IllegalArgumentException();
        } catch (RuntimeException failure) {
            throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
        }
    }

    public static Anchor validateStructure(String value) {
        try {
            if (value == null || value.length() != 167 || !value.matches("[A-Za-z0-9_-]+"))
                throw new IllegalArgumentException();
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            if (bytes.length != 125 || bytes[0] != 1
                    || !Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value))
                throw new IllegalArgumentException();
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            long seconds = buffer.getLong(65);
            int nanos = buffer.getInt(73);
            if (nanos < 0 || nanos > 999999999 || nanos % 1000 != 0) throw new IllegalArgumentException();
            Instant createdAt = Instant.ofEpochSecond(seconds, nanos);
            if (createdAt.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
                    || createdAt.isAfter(Instant.parse("9999-12-31T23:59:59.999999Z")))
                throw new IllegalArgumentException();
            return new Anchor(createdAt, new UUID(buffer.getLong(77), buffer.getLong(85)));
        } catch (RuntimeException failure) {
            throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
        }
    }

    public String encode(Scope scope, Anchor anchor) {
        try {
            ByteBuffer payload = ByteBuffer.allocate(125);
            payload.put((byte) 1).put(scopeBinding(scope)).put(filterDigest())
                    .putLong(anchor.createdAt().getEpochSecond()).putInt(anchor.createdAt().getNano())
                    .putLong(anchor.orderId().getMostSignificantBits()).putLong(anchor.orderId().getLeastSignificantBits());
            payload.put(mac(Arrays.copyOf(payload.array(), 93)));
            String value = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
            validateStructure(value);
            return value;
        } catch (RuntimeException failure) {
            throw new CustomerContextException(CustomerContextException.Code.INTERNAL);
        }
    }

    public Anchor decode(Scope scope, String value) {
        Anchor anchor = validateStructure(value);
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        if (!MessageDigest.isEqual(mac(Arrays.copyOf(bytes, 93)), Arrays.copyOfRange(bytes, 93, 125))
                || !MessageDigest.isEqual(scopeBinding(scope), Arrays.copyOfRange(bytes, 1, 33)))
            throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
        if (!MessageDigest.isEqual(filterDigest(), Arrays.copyOfRange(bytes, 33, 65)))
            throw new CustomerContextException(CustomerContextException.Code.CURSOR_FILTER_MISMATCH);
        return anchor;
    }

    private static byte[] scopeBinding(Scope scope) {
        if (scope == null || scope.accountId() == null) throw new IllegalArgumentException();
        return digest("route=/api/v1/customer/orders\nsort=createdAt desc,orderId desc\naccountId=" + scope.accountId()
                + "\nworkspaceType=" + (scope.organizationId() == null ? "PERSONAL" : "ORGANIZATION")
                + "\norganizationId=" + (scope.organizationId() == null ? "NONE" : scope.organizationId()) + "\n");
    }

    private static byte[] filterDigest() { return digest("subjectId=ALL&paymentStatus=ALL"); }

    private static byte[] digest(String value) {
        try { return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); }
        catch (java.security.GeneralSecurityException failure) { throw new IllegalStateException(); }
    }

    private byte[] mac(byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update(DOMAIN);
            return mac.doFinal(payload);
        } catch (java.security.GeneralSecurityException failure) { throw new IllegalStateException(); }
    }
}
