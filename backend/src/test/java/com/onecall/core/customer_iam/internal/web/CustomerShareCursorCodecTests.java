package com.onecall.core.customer_iam.internal.web;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.sharing.internal.application.ShareCursor;
import com.onecall.core.sharing.internal.application.SharePage;
import com.onecall.core.sharing.internal.application.ShareSummary;
import com.onecall.core.sharing.internal.application.TrustedShareCaller;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

class CustomerShareCursorCodecTests {
    private final CustomerShareCursorCodec codec = new CustomerShareCursorCodec();
    private final UUID account = UUID.fromString("1cc53284-542c-43a4-985d-5bf80534a138");
    private final UUID organization = UUID.fromString("86237139-06c5-4b50-bcb0-671a02cc50df");

    @Test
    void personalAndOrganizationCursorsRoundTripCanonicalAnchors() {
        for (TrustedShareCaller caller : List.of(personal(), organization())) {
            for (Instant instant : List.of(Instant.parse("1969-12-31T23:59:59.123456Z"),
                    Instant.parse("2026-01-02T03:04:05.123456Z"))) {
                ShareCursor cursor = new ShareCursor(caller.accountId(), caller.organizationId(), instant, "share-α");
                String wire = codec.encode(caller, cursor);
                assertThat(codec.decode(caller, wire)).isEqualTo(cursor);
                assertThat(codec.encode(caller, cursor)).isEqualTo(wire);
                assertThat(wire).matches("[A-Za-z0-9_-]+");
            }
        }
    }

    @Test
    void cursorBindingRejectsOtherAccountsAndWorkspaces() {
        var cursor = new ShareCursor(account, Optional.empty(), Instant.EPOCH, "id");
        String personal = codec.encode(personal(), cursor);
        assertThatThrownBy(() -> codec.decode(new TrustedShareCaller(UUID.randomUUID(), Optional.empty()), personal))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.decode(organization(), personal)).isInstanceOf(IllegalArgumentException.class);
        var otherAccountOrganization = new TrustedShareCaller(UUID.randomUUID(), Optional.of(organization));
        assertThatThrownBy(() -> codec.decode(otherAccountOrganization, personal)).isInstanceOf(IllegalArgumentException.class);
        var organizationCursor = new ShareCursor(account, Optional.of(organization), Instant.EPOCH, "id");
        assertThatThrownBy(() -> codec.decode(new TrustedShareCaller(account, Optional.of(UUID.randomUUID())),
                codec.encode(organization(), organizationCursor))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.decode(personal(), codec.encode(organization(), organizationCursor)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedAndNonCanonicalCursorsAreRejected() {
        for (String value : List.of("", "=", "AA==", "not a cursor", "AQ", "_".repeat(513), "A")) {
            assertThatThrownBy(() -> codec.decode(personal(), value)).isInstanceOf(IllegalArgumentException.class);
        }
        var anchor = new ShareCursor(account, Optional.empty(), Instant.parse("2026-01-02T03:04:05.123456Z"), "a");
        byte[] canonical = Base64.getUrlDecoder().decode(codec.encode(personal(), anchor));
        for (byte[] malformed : List.of(
                changed(canonical, bytes -> bytes[0] = 2),
                changed(canonical, bytes -> bytes[0] = 0),
                changed(canonical, bytes -> bytes[1] ^= 1),
                changed(canonical, bytes -> bytes[45] = 2),
                changed(canonical, bytes -> java.nio.ByteBuffer.wrap(bytes).putInt(41, 1)),
                changed(canonical, bytes -> java.nio.ByteBuffer.wrap(bytes).putLong(33, Long.MAX_VALUE)),
                changed(canonical, bytes -> java.nio.ByteBuffer.wrap(bytes).putInt(41, 1_000_000_000)),
                changed(canonical, bytes -> bytes[bytes.length - 1] |= 1),
                changed(canonical, bytes -> setScalar(bytes, 0)),
                changed(canonical, bytes -> setScalar(bytes, 0xd800)),
                changed(canonical, bytes -> setScalar(bytes, 0x110000)),
                java.util.Arrays.copyOf(canonical, canonical.length + 1))) {
            assertThatThrownBy(() -> codec.decode(personal(), Base64.getUrlEncoder().withoutPadding().encodeToString(malformed)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void anchorBoundsMatchPa10Validation() {
        for (String id : List.of("", "   ", "bad\u0000id", "x".repeat(129), "\ud800")) {
            var invalid = new ShareCursor(account, Optional.empty(), Instant.EPOCH, id);
            assertThatThrownBy(() -> codec.encode(personal(), invalid)).isInstanceOf(IllegalArgumentException.class);
        }
        var precise = new ShareCursor(account, Optional.empty(), Instant.ofEpochSecond(1, 1), "id");
        assertThatThrownBy(() -> codec.encode(personal(), precise)).isInstanceOf(IllegalArgumentException.class);
        assertThat(codec.decode(personal(), codec.encode(personal(), new ShareCursor(account, Optional.empty(),
                Instant.parse("-4712-01-01T00:00:00Z"), "lower")))).isNotNull();
        assertThat(codec.decode(personal(), codec.encode(personal(), new ShareCursor(account, Optional.empty(),
                Instant.parse("+294276-12-31T23:59:59.999999Z"), "upper")))).isNotNull();
        for (Instant outside : List.of(Instant.parse("-4713-12-31T23:59:59.999999Z"),
                Instant.parse("+294277-01-01T00:00:00Z"))) {
            assertThatThrownBy(() -> codec.encode(personal(), new ShareCursor(account, Optional.empty(), outside, "id")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void maximumUnicodeShareIdsFitFrozenWireBound() {
        for (String id : List.of("a".repeat(128), "界".repeat(128), "😀".repeat(128),
                "a界😀".repeat(42) + "a界")) {
            var cursor = new ShareCursor(account, Optional.empty(), Instant.EPOCH, id);
            String encoded = codec.encode(personal(), cursor);
            assertThat(encoded.length()).isLessThanOrEqualTo(510);
            assertThat(codec.decode(personal(), encoded)).isEqualTo(cursor);
            assertThat(codec.encode(personal(), cursor)).isEqualTo(encoded);
            assertThat(Base64.getUrlDecoder().decode(encoded)).hasSize(id.codePointCount(0, id.length()) == 128
                    ? 46 + (21 * 128 + 7) / 8 : 46 + (21 * id.codePointCount(0, id.length()) + 7) / 8);
        }
        String maximum = codec.encode(personal(), new ShareCursor(account, Optional.empty(), Instant.EPOCH, "😀".repeat(128)));
        assertThat(Base64.getUrlDecoder().decode(maximum)).hasSize(382);
        assertThat(maximum).hasSize(510);
    }

    @Test
    void wirePageHasOnlyFrozenFieldsAndNullableCursor() throws Exception {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .run(context -> {
        JsonMapper mapper = context.getBean(JsonMapper.class);
        var empty = mapper.readTree(mapper.writeValueAsBytes(new CustomerSharePage(List.of(), null, false)));
        assertThat(empty.properties().stream().map(java.util.Map.Entry::getKey))
                .containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
        assertThat(empty.has("nextCursor")).isTrue();
        assertThat(empty.get("nextCursor").isNull()).isTrue();
        var summaries = new java.util.ArrayList<ShareSummary>();
        for (var mode : ShareSummary.VerificationMode.values()) {
            for (var status : ShareSummary.ShareStatus.values()) {
                summaries.add(new ShareSummary("s" + summaries.size(), UUID.randomUUID(), mode, status,
                        Instant.parse("2026-01-02T03:04:05.123456Z"), Instant.parse("2026-01-02T03:04:06Z"),
                        null, 0, false, mode == ShareSummary.VerificationMode.EXTRACTION_CODE ? 1 : 0, 0));
            }
        }
        var page = CustomerSharePage.from(new SharePage(summaries, Optional.empty(), false), null);
        var json = mapper.readTree(mapper.writeValueAsBytes(page));
        assertThat(json.properties().stream().map(java.util.Map.Entry::getKey))
                .containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
        assertThat(json.get("items")).hasSize(16);
        assertThat(json.get("items").get(0).properties().stream().map(java.util.Map.Entry::getKey))
                .containsExactlyInAnyOrder("shareId", "evidenceId", "verificationMode", "status", "validFrom",
                        "expiresAt", "maxAdmissions", "admissionsUsed", "downloadAllowed", "codeGeneration", "revocationGeneration");
        assertThat(json.get("items").get(0).get("maxAdmissions").isNull()).isTrue();
        assertThat(json.get("items").get(0).get("validFrom").asString()).isEqualTo("2026-01-02T03:04:05.123456Z");
        assertThat(json.get("items").get(0).get("expiresAt").asString()).isEqualTo("2026-01-02T03:04:06Z");
        assertThat(mapper.readTree(mapper.writeValueAsBytes(new CustomerSharePage(List.of(), "opaque-cursor", true)))
                .get("nextCursor").asString()).isEqualTo("opaque-cursor");
        assertThat(json.get("items")).extracting(node -> node.get("verificationMode").asString())
                .contains("NONE", "EXTRACTION_CODE", "DESIGNATED_MOBILE", "ORGANIZATION_MEMBER");
        assertThat(json.get("items")).extracting(node -> node.get("status").asString())
                .contains("ACTIVE", "EXPIRED", "REVOKED", "LOCKED");
        var lookahead = mapper.readTree(mapper.writeValueAsBytes(new CustomerSharePage(page.items(), "next", true)));
        assertThat(lookahead.get("hasMore").asBoolean()).isTrue();
        assertThat(lookahead.get("nextCursor").asString()).isEqualTo("next");
        assertThat(mapper.writeValueAsString(page).toLowerCase(java.util.Locale.ROOT)).doesNotContain(
                "capability", "extractioncode", "hash", "salt", "objectkey", "media", "url", "caller", "token", "secret");
        });
    }

    private TrustedShareCaller personal() { return new TrustedShareCaller(account, Optional.empty()); }
    private TrustedShareCaller organization() { return new TrustedShareCaller(account, Optional.of(organization)); }

    private static byte[] changed(byte[] source, java.util.function.Consumer<byte[]> mutation) {
        byte[] copy = source.clone();
        mutation.accept(copy);
        return copy;
    }

    private static void setScalar(byte[] payload, int scalar) {
        for (int bitIndex = 0; bitIndex < 21; bitIndex++) {
            int mask = 1 << (7 - (bitIndex % 8));
            int position = 46 + bitIndex / 8;
            if (((scalar >>> (20 - bitIndex)) & 1) == 1) payload[position] |= (byte) mask;
            else payload[position] &= (byte) ~mask;
        }
    }
}
