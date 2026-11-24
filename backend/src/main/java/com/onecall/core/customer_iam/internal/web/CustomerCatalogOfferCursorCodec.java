package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class CustomerCatalogOfferCursorCodec {
    private static final byte[] DOMAIN = "onecall:customer-catalog-offers:cursor:v1\n".getBytes(StandardCharsets.UTF_8);
    private final byte[] key;

    public record Binding(UUID accountId, UUID organizationId, String channel, String audience, Optional<String> offerType) {
        public Binding {
            Objects.requireNonNull(accountId);
            Objects.requireNonNull(channel);
            Objects.requireNonNull(audience);
            Objects.requireNonNull(offerType);
            if (!Set.of("MINIAPP", "WEB").contains(channel) || !Set.of("PERSONAL", "ENTERPRISE").contains(audience)
                    || offerType.isPresent() && !Set.of("SUBSCRIPTION", "ONE_TIME", "ADD_ON", "QUOTE").contains(offerType.get()))
                throw new IllegalArgumentException("Invalid binding");
        }
    }

    public record Anchor(String productCode, int versionNo, UUID catalogOfferId) {
        public Anchor {
            Objects.requireNonNull(productCode);
            Objects.requireNonNull(catalogOfferId);
            if (!productCode.matches("^[A-Z][A-Z0-9_-]{1,63}$") || versionNo < 1)
                throw new IllegalArgumentException("Invalid anchor");
        }
    }

    public CustomerCatalogOfferCursorCodec(CustomerCatalogOfferCursorProperties properties) {
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
            if (value == null || value.length() > 512 || !value.matches("[A-Za-z0-9_-]+"))
                throw new IllegalArgumentException();
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            if (bytes.length < 120 || bytes.length > 182 || value.length() < 160 || value.length() > 243
                    || value.length() != (4 * bytes.length + 2) / 3
                    || !Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value))
                throw new IllegalArgumentException();
            if (bytes[0] != 1) throw new IllegalArgumentException();
            int codeLength = bytes[65] & 0xff;
            if (codeLength < 2 || codeLength > 64 || codeLength != bytes.length - 118)
                throw new IllegalArgumentException();
            String productCode = new String(bytes, 66, codeLength, StandardCharsets.US_ASCII);
            if (!productCode.matches("^[A-Z][A-Z0-9_-]{1,63}$")) throw new IllegalArgumentException();
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int versionNo = buffer.getInt(66 + codeLength);
            if (versionNo < 1) throw new IllegalArgumentException();
            UUID catalogOfferId = new UUID(buffer.getLong(70 + codeLength), buffer.getLong(78 + codeLength));
            return new Anchor(productCode, versionNo, catalogOfferId);
        } catch (RuntimeException failure) {
            throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
        }
    }

    public String encode(Binding binding, Anchor anchor) {
        try {
            byte[] code = anchor.productCode().getBytes(StandardCharsets.US_ASCII);
            ByteBuffer payload = ByteBuffer.allocate(118 + code.length);
            payload.put((byte) 1).put(scopeBinding(binding)).put(filterDigest(binding)).put((byte) code.length).put(code)
                    .putInt(anchor.versionNo()).putLong(anchor.catalogOfferId().getMostSignificantBits())
                    .putLong(anchor.catalogOfferId().getLeastSignificantBits());
            payload.put(mac(Arrays.copyOf(payload.array(), 86 + code.length)));
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.array());
            validateStructure(encoded);
            return encoded;
        } catch (RuntimeException failure) {
            throw new CustomerContextException(CustomerContextException.Code.INTERNAL);
        }
    }

    public Anchor decode(Binding binding, String value) {
        Anchor anchor = validateStructure(value);
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        int macStart = 86 + (bytes[65] & 0xff);
        if (!MessageDigest.isEqual(mac(Arrays.copyOf(bytes, macStart)), Arrays.copyOfRange(bytes, macStart, macStart + 32))
                || !MessageDigest.isEqual(scopeBinding(binding), Arrays.copyOfRange(bytes, 1, 33)))
            throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
        if (!MessageDigest.isEqual(filterDigest(binding), Arrays.copyOfRange(bytes, 33, 65)))
            throw new CustomerContextException(CustomerContextException.Code.CURSOR_FILTER_MISMATCH);
        return anchor;
    }

    private static byte[] scopeBinding(Binding binding) {
        try {
            String workspace = binding.organizationId() == null ? "PERSONAL" : "ORGANIZATION:" + binding.organizationId();
            return MessageDigest.getInstance("SHA-256").digest(("onecall:customer-catalog-offers:v1\n"
                    + "productCode:asc,versionNo:desc,catalogOfferId:asc\n" + binding.accountId() + "\n" + workspace + "\n")
                    .getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException failure) {
            throw new CustomerContextException(CustomerContextException.Code.INTERNAL);
        }
    }

    private static byte[] filterDigest(Binding binding) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(("onecall:customer-catalog-offers:filters:v1\n"
                    + binding.channel() + "\n" + binding.audience() + "\n" + binding.offerType().orElse("ALL") + "\n")
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
