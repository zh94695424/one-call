package com.onecall.core.customer_iam.internal.web;

import static org.assertj.core.api.Assertions.*;
import com.onecall.core.catalog_order.customerquery.CustomerOrderReader.*;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class CustomerOrderCursorCodecTests {
    static final byte[] KEY = new byte[32];
    static final Scope SCOPE = new Scope(UUID.randomUUID(), null);
    static final Anchor ANCHOR = new Anchor(Instant.parse("2026-11-25T00:00:00.123456Z"), UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
    static CustomerCatalogOfferCursorProperties properties(byte[] key) {
        var properties = new CustomerCatalogOfferCursorProperties();
        properties.setCursorHmacKey(Base64.getEncoder().encodeToString(key));
        return properties;
    }
    static void invalid(Runnable action, CustomerContextException.Code code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomerContextException.class, failure -> assertThat(failure.code()).isEqualTo(code));
    }
    @Test void canonicalRoundTripAndScopeBindingAreExact() {
        var codec = new CustomerOrderCursorCodec(properties(KEY));
        String cursor = codec.encode(SCOPE, ANCHOR);
        assertThat(cursor).hasSize(167).doesNotContain("=");
        assertThat(codec.decode(SCOPE, cursor)).isEqualTo(ANCHOR);
        assertThat(CustomerOrderCursorCodec.validateStructure(cursor)).isEqualTo(ANCHOR);
        invalid(() -> codec.decode(new Scope(UUID.randomUUID(), null), cursor), CustomerContextException.Code.CURSOR_INVALID);
        invalid(() -> codec.decode(new Scope(SCOPE.accountId(), UUID.randomUUID()), cursor), CustomerContextException.Code.CURSOR_INVALID);
    }
    @Test void malformedAndTamperedCursorIsInvalid() {
        var codec = new CustomerOrderCursorCodec(properties(KEY));
        String cursor = codec.encode(SCOPE, ANCHOR);
        for (String value : java.util.List.of("", "a", "a".repeat(513), cursor + "=", cursor.substring(1), "!" + cursor.substring(1)))
            invalid(() -> CustomerOrderCursorCodec.validateStructure(value), CustomerContextException.Code.CURSOR_INVALID);
        byte[] bytes = Base64.getUrlDecoder().decode(cursor); bytes[94] ^= 1;
        invalid(() -> codec.decode(SCOPE, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)), CustomerContextException.Code.CURSOR_INVALID);
        invalid(() -> new CustomerOrderCursorCodec(new CustomerCatalogOfferCursorProperties()), CustomerContextException.Code.SERVICE_UNAVAILABLE);
    }
    @Test void filterDigestMismatchIsDistinct() throws Exception {
        var codec = new CustomerOrderCursorCodec(properties(KEY));
        byte[] bytes = Base64.getUrlDecoder().decode(codec.encode(SCOPE, ANCHOR)); bytes[33] ^= 1;
        String unauthenticated = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        invalid(() -> codec.decode(SCOPE, unauthenticated), CustomerContextException.Code.CURSOR_INVALID);
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
        mac.update("onecall:customer-orders:cursor:v1\n".getBytes(StandardCharsets.UTF_8));
        System.arraycopy(mac.doFinal(Arrays.copyOf(bytes, 93)), 0, bytes, 93, 32);
        invalid(() -> codec.decode(SCOPE, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)), CustomerContextException.Code.CURSOR_FILTER_MISMATCH);
    }
    @Test void timestampUuidAndLengthBoundariesAreExact() {
        var codec = new CustomerOrderCursorCodec(properties(KEY));
        for (Instant value : java.util.List.of(Instant.parse("0001-01-01T00:00:00Z"), Instant.parse("9999-12-31T23:59:59.999999Z"))) {
            var anchor = new Anchor(value, new UUID(Long.MIN_VALUE, Long.MAX_VALUE));
            assertThat(codec.decode(SCOPE, codec.encode(SCOPE, anchor))).isEqualTo(anchor);
        }
        byte[] bytes = Base64.getUrlDecoder().decode(codec.encode(SCOPE, ANCHOR));
        java.nio.ByteBuffer.wrap(bytes).putInt(73, 1);
        invalid(() -> CustomerOrderCursorCodec.validateStructure(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)), CustomerContextException.Code.CURSOR_INVALID);
    }
    @Test void catalogOfferReplayAndOldKeyAreRejected() {
        var codec = new CustomerOrderCursorCodec(properties(KEY));
        var catalog = new CustomerCatalogOfferCursorCodec(properties(KEY));
        var binding = new CustomerCatalogOfferCursorCodec.Binding(SCOPE.accountId(), null, "WEB", "PERSONAL", Optional.empty());
        String offer = catalog.encode(binding, new CustomerCatalogOfferCursorCodec.Anchor("SYNTHETIC", 1, UUID.randomUUID()));
        invalid(() -> codec.decode(SCOPE, offer), CustomerContextException.Code.CURSOR_INVALID);
        String order = codec.encode(SCOPE, ANCHOR);
        invalid(() -> catalog.decode(binding, order), CustomerContextException.Code.CURSOR_INVALID);
        byte[] replacement = KEY.clone(); replacement[0] = 1;
        invalid(() -> new CustomerOrderCursorCodec(properties(replacement)).decode(SCOPE, order), CustomerContextException.Code.CURSOR_INVALID);
        invalid(() -> new CustomerCatalogOfferCursorCodec(properties(replacement)).decode(binding, offer), CustomerContextException.Code.CURSOR_INVALID);
    }
}
