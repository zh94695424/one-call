package com.onecall.core.customer_iam.internal.web;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader.Anchor;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerEvidenceCursorCodecTests {
    private final UUID account = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final Scope personal = new Scope(account, null);
    private final Anchor anchor = new Anchor(Instant.parse("2026-11-12T00:00:00.123456Z"),
            UUID.fromString("80000000-0000-0000-0000-000000000001"));
    private CustomerEvidenceCursorCodec codec(byte[] key) {
        var properties = new CustomerEvidenceCursorProperties();
        properties.setCursorHmacKey(Base64.getEncoder().encodeToString(key));
        return new CustomerEvidenceCursorCodec(properties);
    }
    private void invalid(Runnable action, CustomerContextException.Code code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomerContextException.class,
                failure -> assertThat(failure.code()).isEqualTo(code));
    }
    private void invalid(String value) {
        invalid(() -> codec(new byte[32]).decode(personal, value), CustomerContextException.Code.CURSOR_INVALID);
    }

    @Test
    void personalAndOrganizationAnchorsRoundTripCanonically() {
        var codec = codec(new byte[32]);
        for (Scope scope : List.of(personal, new Scope(account, UUID.randomUUID()))) {
            String value = codec.encode(scope, anchor);
            assertThat(codec.decode(scope, value)).isEqualTo(anchor);
            assertThat(codec.encode(scope, codec.decode(scope, value))).isEqualTo(value);
        }
    }

    @Test
    void payloadVersionLengthDomainAndMicrosecondPrecisionAreExact() throws Exception {
        String value = codec(new byte[32]).encode(personal, anchor);
        assertThat(value).hasSize(124).doesNotContain("=");
        byte[] bytes = Base64.getUrlDecoder().decode(value);
        assertThat(bytes).hasSize(93); assertThat(bytes[0]).isEqualTo((byte) 1);
        var payload = ByteBuffer.wrap(bytes); payload.position(33);
        assertThat(payload.getLong()).isEqualTo(anchor.createdAt().getEpochSecond());
        assertThat(payload.getInt()).isEqualTo(123456000);
        assertThat(new UUID(payload.getLong(), payload.getLong())).isEqualTo(anchor.evidenceId());
        var mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(new byte[32], "HmacSHA256"));
        mac.update("onecall:customer-evidence:cursor:v1\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(Arrays.copyOfRange(bytes, 61, 93)).containsExactly(mac.doFinal(Arrays.copyOf(bytes, 61)));
        invalid(() -> codec(new byte[32]).encode(personal, new Anchor(anchor.createdAt().plusNanos(1), anchor.evidenceId())),
                CustomerContextException.Code.INTERNAL);
    }

    @Test
    void invalidAlphabetPaddingWhitespaceAndAlternateEncodingAreRejected() {
        String valid = codec(new byte[32]).encode(personal, anchor);
        for (String value : List.of("", " ", valid + "=", valid + "\n", "+" + valid.substring(1), "/" + valid.substring(1))) invalid(value);
        invalid((String) null);
    }

    @Test
    void truncatedExtendedUnknownVersionAndInvalidTimesAreRejected() {
        byte[] original = Base64.getUrlDecoder().decode(codec(new byte[32]).encode(personal, anchor));
        invalid(Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(original, 92)));
        invalid(Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(original, 94)));
        for (int nanos : new int[] {-1, 1, 999999999, 1000000000}) {
            byte[] mutated = original.clone(); ByteBuffer.wrap(mutated).putInt(41, nanos);
            invalid(Base64.getUrlEncoder().withoutPadding().encodeToString(mutated));
        }
        for (long seconds : new long[] {Long.MIN_VALUE, Long.MAX_VALUE, Instant.MAX.getEpochSecond()}) {
            byte[] mutated = original.clone(); ByteBuffer.wrap(mutated).putLong(33, seconds);
            invalid(Base64.getUrlEncoder().withoutPadding().encodeToString(mutated));
        }
        byte[] version = original.clone(); version[0] = 2;
        invalid(Base64.getUrlEncoder().withoutPadding().encodeToString(version));
    }

    @Test
    void everyPayloadBindingAndMacByteMutationIsRejected() {
        byte[] original = Base64.getUrlDecoder().decode(codec(new byte[32]).encode(personal, anchor));
        for (int index = 0; index < original.length; index++) {
            byte[] mutated = original.clone(); mutated[index] ^= 1;
            invalid(Base64.getUrlEncoder().withoutPadding().encodeToString(mutated));
        }
    }

    @Test
    void accountWorkspaceKindOrganizationAndOtherEndpointReplayAreRejected() {
        var codec = codec(new byte[32]);
        String valid = codec.encode(personal, anchor);
        for (Scope other : List.of(new Scope(UUID.randomUUID(), null), new Scope(account, UUID.randomUUID())))
            invalid(() -> codec.decode(other, valid), CustomerContextException.Code.CURSOR_INVALID);
        var organization = new Scope(account, UUID.randomUUID());
        invalid(() -> codec.decode(new Scope(account, UUID.randomUUID()), codec.encode(organization, anchor)), CustomerContextException.Code.CURSOR_INVALID);
        invalid(() -> codec.decode(personal, codec.encode(organization, anchor)), CustomerContextException.Code.CURSOR_INVALID);
        var properties = new CustomerBusinessNumberCursorProperties();
        properties.setCursorHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
        var business = new CustomerBusinessNumberCursorCodec(properties);
        var businessAnchor = new com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Anchor(anchor.createdAt(), anchor.evidenceId());
        invalid(business.encode(personal, businessAnchor));
        invalid(() -> business.decode(personal, valid), CustomerContextException.Code.CURSOR_INVALID);
    }

    @Test
    void missingMalformedWrongLengthAndNoncanonicalKeysAreUnavailable() {
        for (String value : Arrays.asList(null, "", "secret-CANARY", " ", Base64.getEncoder().encodeToString(new byte[31]),
                Base64.getEncoder().encodeToString(new byte[33]), Base64.getEncoder().withoutPadding().encodeToString(new byte[32]))) {
            var properties = new CustomerEvidenceCursorProperties(); properties.setCursorHmacKey(value);
            invalid(() -> new CustomerEvidenceCursorCodec(properties), CustomerContextException.Code.SERVICE_UNAVAILABLE);
            assertThat(properties.toString()).doesNotContain("secret-CANARY");
        }
    }

    @Test
    void keyReplacementInvalidatesOldCursorAndLimitIsNotBound() {
        var old = codec(new byte[32]); String value = old.encode(personal, anchor);
        byte[] replacement = new byte[32]; Arrays.fill(replacement, (byte) 1);
        invalid(() -> codec(replacement).decode(personal, value), CustomerContextException.Code.CURSOR_INVALID);
        for (int limit : List.of(1, 20, 100)) {
            var request = new com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryService.Request(
                    java.util.Optional.of(old.decode(personal, value)), java.util.Optional.of(limit));
            assertThat(request.anchor()).contains(anchor); assertThat(request.effectiveLimit()).isEqualTo(limit);
        }
    }
}
