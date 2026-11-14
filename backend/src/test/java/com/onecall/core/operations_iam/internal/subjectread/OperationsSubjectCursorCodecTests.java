package com.onecall.core.operations_iam.internal.subjectread;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.Cursor;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.Datasource;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.KeyEntry;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.Anchor;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.RawDetailRequest;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.RawListRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class OperationsSubjectCursorCodecTests {
    private static final String PURPOSE = "onecall:operations-subject-list:cursor:v1\0";
    private static final Anchor ANCHOR = new Anchor(Instant.parse("2026-11-14T12:34:56.123456Z"),
            UUID.fromString("abcdef01-2345-6789-abcd-0123456789ab"));
    private static final byte[] DIGEST = OperationsSubjectReadRequest.list(new RawListRequest(Map.of(), List.of())).digest();
    private static final KeyEntry CURRENT = key("Current-2026_11", 17);
    private static final KeyEntry PREVIOUS = key("previous_2026-10", 31);

    @Test
    void encryptedRoundTripPreservesExactTimestampUuidAndDigest() throws Exception {
        var codec = codec(CURRENT.id(), CURRENT);
        for (Instant time : List.of(Instant.EPOCH, ANCHOR.createdAt(), Instant.parse("1969-12-31T23:59:59.999999Z"))) {
            var anchor = new Anchor(time, ANCHOR.subjectId());
            String token = codec.encode(anchor, DIGEST);
            assertThat(codec.decode(token, DIGEST)).isEqualTo(anchor);
            byte[] envelope = decode(token);
            var header = ByteBuffer.wrap(envelope);
            assertThat(header.get()).isEqualTo((byte) 1);
            int length = Byte.toUnsignedInt(header.get());
            byte[] identifier = new byte[length];
            header.get(identifier);
            assertThat(new String(identifier, StandardCharsets.US_ASCII)).isEqualTo(CURRENT.id());
            byte[] nonce = new byte[12];
            header.get(nonce);
            byte[] encrypted = new byte[header.remaining()];
            header.get(encrypted);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(CURRENT.bytes(), "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(PURPOSE.getBytes(StandardCharsets.UTF_8));
            cipher.updateAAD(Arrays.copyOf(envelope, 2 + length));
            var payload = ByteBuffer.wrap(cipher.doFinal(encrypted));
            assertThat(payload.remaining()).isEqualTo(60);
            assertThat(payload.getLong()).isEqualTo(time.getEpochSecond());
            assertThat(payload.getInt()).isEqualTo(time.getNano());
            assertThat(new UUID(payload.getLong(), payload.getLong())).isEqualTo(anchor.subjectId());
            byte[] digest = new byte[32];
            payload.get(digest);
            assertThat(digest).containsExactly(DIGEST);
            assertThat(payload.hasRemaining()).isFalse();
        }
    }

    @Test
    void sameAnchorProducesRandomizedOpaqueBoundedTokens() {
        var codec = codec(CURRENT.id(), CURRENT);
        var tokens = new HashSet<String>();
        var nonces = new HashSet<String>();
        for (int index = 0; index < 100; index++) {
            String token = codec.encode(ANCHOR, DIGEST);
            assertThat(token).matches("[A-Za-z0-9_-]{122,163}")
                    .doesNotContain(ANCHOR.subjectId().toString(), ANCHOR.createdAt().toString(), "offset", "created_at");
            assertThat(tokens.add(token)).isTrue();
            byte[] envelope = decode(token);
            int start = 2 + Byte.toUnsignedInt(envelope[1]);
            assertThat(nonces.add(encode(Arrays.copyOfRange(envelope, start, start + 12)))).isTrue();
            assertThat(codec.decode(token, DIGEST)).isEqualTo(ANCHOR);
        }
        for (String identifier : List.of("a", "A".repeat(32))) {
            var edge = codec(identifier, key(identifier, 5));
            assertThat(edge.encode(ANCHOR, DIGEST)).hasSize(identifier.length() == 1 ? 122 : 163);
        }
    }

    @Test
    void missingMalformedAndAmbiguousKeyMaterialFailsClosed() {
        failure(() -> new OperationsSubjectCursorCodec(null), Code.SERVICE_UNAVAILABLE);
        failure(() -> new OperationsSubjectCursorCodec(new OperationsSubjectReadProperties(true, null, null)), Code.SERVICE_UNAVAILABLE);
        var rings = new ArrayList<Cursor>();
        rings.add(new Cursor(null, List.of(CURRENT)));
        rings.add(new Cursor(CURRENT.id(), null));
        rings.add(new Cursor(CURRENT.id(), List.of()));
        rings.add(new Cursor("missing", List.of(CURRENT)));
        rings.add(new Cursor(CURRENT.id(), Arrays.asList(CURRENT, null)));
        rings.add(new Cursor(CURRENT.id(), List.of(CURRENT, key(CURRENT.id(), 2))));
        rings.add(new Cursor(CURRENT.id(), List.of(CURRENT, new KeyEntry("duplicate-material", CURRENT.material()))));
        rings.add(new Cursor(CURRENT.id(), List.of(CURRENT, PREVIOUS, key("third", 3), key("fourth", 4))));
        for (String identifier : Arrays.asList(null, "", " ", "has.dot", "slash/kid", "é", "a".repeat(33))) {
            rings.add(new Cursor(identifier, List.of(new KeyEntry(identifier, CURRENT.material()))));
        }
        for (String material : Arrays.asList(null, "", " ", CURRENT.material() + "=", "!".repeat(43),
                encode(new byte[31]), encode(new byte[33]), "A".repeat(42) + "B")) {
            rings.add(new Cursor("bad", List.of(new KeyEntry("bad", material))));
        }
        for (Cursor cursor : rings) {
            failure(() -> new OperationsSubjectCursorCodec(new OperationsSubjectReadProperties(true, null, cursor)), Code.SERVICE_UNAVAILABLE);
        }
        var entries = new ArrayList<>(List.of(CURRENT));
        var immutable = new Cursor(CURRENT.id(), entries);
        entries.clear();
        assertThat(immutable.validatedKeys()).containsExactly(CURRENT);
        assertThatThrownBy(() -> immutable.keys().clear()).isInstanceOf(UnsupportedOperationException.class);
        byte[] bytes = CURRENT.bytes();
        bytes[0] ^= 1;
        assertThat(CURRENT.bytes()).isNotEqualTo(bytes);
    }

    @Test
    void configurationFailurePrecedesMalformedToken() {
        for (String token : Arrays.asList(null, "", "!", "a".repeat(513))) {
            failure(() -> new OperationsSubjectCursorCodec(new OperationsSubjectReadProperties(true, null,
                    new Cursor("missing", List.of(CURRENT)))).decode(token, DIGEST), Code.SERVICE_UNAVAILABLE);
        }
    }

    @Test
    void strictBase64VersionLengthsAndTrailingBytesAreInvalid() {
        var codec = codec(CURRENT.id(), CURRENT);
        String token = codec.encode(ANCHOR, DIGEST);
        for (String malformed : Arrays.asList(null, "", " ", "a", token + "=", "=" + token, token + "\n", "a".repeat(513),
                token.replace('-', '+').replace('_', '/') + "/")) {
            failure(() -> codec.decode(malformed, DIGEST), Code.CURSOR_INVALID);
        }
        byte[] original = decode(token);
        for (int size = 0; size < original.length; size++) {
            String shortened = encode(Arrays.copyOf(original, size));
            failure(() -> codec.decode(shortened, DIGEST), Code.CURSOR_INVALID);
        }
        failure(() -> codec.decode(encode(Arrays.copyOf(original, original.length + 1)), DIGEST), Code.CURSOR_INVALID);
        for (int version : List.of(0, 2, 255)) {
            byte[] changed = original.clone();
            changed[0] = (byte) version;
            failure(() -> codec.decode(encode(changed), DIGEST), Code.CURSOR_INVALID);
        }
        for (int length : List.of(0, 33, 255)) {
            byte[] changed = original.clone();
            changed[1] = (byte) length;
            failure(() -> codec.decode(encode(changed), DIGEST), Code.CURSOR_INVALID);
        }
        String noncanonical = token.substring(0, token.length() - 1) + "B";
        if (!noncanonical.equals(token)) failure(() -> codec.decode(noncanonical, DIGEST), Code.CURSOR_INVALID);
    }

    @Test
    void tamperedNonceHeaderPayloadAndTagAreInvalid() {
        var codec = codec(CURRENT.id(), CURRENT);
        byte[] original = decode(codec.encode(ANCHOR, DIGEST));
        for (int index = 0; index < original.length; index++) {
            byte[] changed = original.clone();
            changed[index] ^= 1;
            failure(() -> codec.decode(encode(changed), DIGEST), Code.CURSOR_INVALID);
        }
    }

    @Test
    void wrongPurposeKeyAndRetiredKidAreInvalid() throws Exception {
        var codec = codec(CURRENT.id(), CURRENT);
        byte[] payload = payload(ANCHOR.createdAt().getEpochSecond(), ANCHOR.createdAt().getNano(), DIGEST);
        String crossPurpose = seal(CURRENT, payload, "onecall:customer-evidence:cursor:v1\n");
        failure(() -> codec.decode(crossPurpose, DIGEST), Code.CURSOR_INVALID);
        String token = codec.encode(ANCHOR, DIGEST);
        failure(() -> codec(CURRENT.id(), key(CURRENT.id(), 66)).decode(token, DIGEST), Code.CURSOR_INVALID);
        failure(() -> codec(PREVIOUS.id(), PREVIOUS).decode(token, DIGEST), Code.CURSOR_INVALID);
        failure(() -> codec(CURRENT.id().toLowerCase(java.util.Locale.ROOT),
                new KeyEntry(CURRENT.id().toLowerCase(java.util.Locale.ROOT), CURRENT.material())).decode(token, DIGEST), Code.CURSOR_INVALID);
    }

    @Test
    void onlyAuthenticatedDifferentDigestIsFilterMismatch() {
        var codec = codec(CURRENT.id(), CURRENT);
        String token = codec.encode(ANCHOR, DIGEST);
        byte[] different = DIGEST.clone();
        different[0] ^= 1;
        failure(() -> codec.decode(token, different), Code.CURSOR_FILTER_MISMATCH);
        byte[] tampered = decode(token);
        tampered[tampered.length - 1] ^= 1;
        failure(() -> codec.decode(encode(tampered), different), Code.CURSOR_INVALID);
        assertThat(codec.decode(token, DIGEST.clone())).isEqualTo(ANCHOR);
    }

    @Test
    void activeKeyIssuesAndRetainedKeysVerifyAcrossRotation() {
        var old = codec(PREVIOUS.id(), PREVIOUS);
        String oldToken = old.encode(ANCHOR, DIGEST);
        var rotating = codec(CURRENT.id(), PREVIOUS, CURRENT);
        assertThat(rotating.decode(oldToken, DIGEST)).isEqualTo(ANCHOR);
        String newToken = rotating.encode(ANCHOR, DIGEST);
        byte[] envelope = decode(newToken);
        assertThat(new String(envelope, 2, Byte.toUnsignedInt(envelope[1]), StandardCharsets.US_ASCII)).isEqualTo(CURRENT.id());
        var preparedOld = codec(PREVIOUS.id(), CURRENT, PREVIOUS);
        assertThat(preparedOld.decode(newToken, DIGEST)).isEqualTo(ANCHOR);
        assertThat(rotating.decode(oldToken, DIGEST)).isEqualTo(ANCHOR);
        assertThat(codec(CURRENT.id(), CURRENT, PREVIOUS).decode(newToken, DIGEST)).isEqualTo(ANCHOR);
        var third = key("Third_CASE-id", 42);
        assertThat(codec(third.id(), CURRENT, third, PREVIOUS).decode(oldToken, DIGEST)).isEqualTo(ANCHOR);
    }

    @Test
    void retirementNeverFallsBackOrReissuesAReplacement() {
        var old = codec(PREVIOUS.id(), PREVIOUS);
        String retired = old.encode(ANCHOR, DIGEST);
        var current = codec(CURRENT.id(), CURRENT);
        failure(() -> current.decode(retired, DIGEST), Code.CURSOR_INVALID);
        byte[] changedDigest = DIGEST.clone();
        changedDigest[0] ^= 1;
        failure(() -> current.decode(retired, changedDigest), Code.CURSOR_INVALID);
        assertThat(old.decode(retired, DIGEST)).isEqualTo(ANCHOR);
        assertThat(current.decode(current.encode(ANCHOR, DIGEST), DIGEST)).isEqualTo(ANCHOR);
    }

    @Test
    void invalidNanosAndPlaintextShapesNeverReachQueries() throws Exception {
        var codec = codec(CURRENT.id(), CURRENT);
        for (int nanos : List.of(-1, 1, 999, 999999999, 1000000000, Integer.MAX_VALUE)) {
            String token = seal(CURRENT, payload(ANCHOR.createdAt().getEpochSecond(), nanos, DIGEST), PURPOSE);
            failure(() -> codec.decode(token, DIGEST), Code.CURSOR_INVALID);
        }
        for (int nanos : List.of(0, 1000, 999999000)) {
            String token = seal(CURRENT, payload(ANCHOR.createdAt().getEpochSecond(), nanos, DIGEST), PURPOSE);
            assertThat(codec.decode(token, DIGEST).createdAt()).isEqualTo(Instant.ofEpochSecond(ANCHOR.createdAt().getEpochSecond(), nanos));
        }
        for (long seconds : List.of(Long.MIN_VALUE, Long.MAX_VALUE)) {
            String token = seal(CURRENT, payload(seconds, 0, DIGEST), PURPOSE);
            failure(() -> codec.decode(token, DIGEST), Code.CURSOR_INVALID);
        }
        for (int size : List.of(0, 59, 61, 120)) {
            String token = seal(CURRENT, Arrays.copyOf(payload(0, 0, DIGEST), size), PURPOSE);
            failure(() -> codec.decode(token, DIGEST), Code.CURSOR_INVALID);
        }
        failure(() -> codec.encode(new Anchor(Instant.ofEpochSecond(1, 1), ANCHOR.subjectId()), DIGEST), Code.SERVICE_UNAVAILABLE);
        failure(() -> codec.encode(null, DIGEST), Code.SERVICE_UNAVAILABLE);
        failure(() -> codec.encode(ANCHOR, new byte[31]), Code.SERVICE_UNAVAILABLE);
    }

    @Test
    void keysRequestsAnchorsAndFailuresHaveRedactedRepresentations() {
        String secret = "cursor-secret-canary";
        var datasource = new Datasource("jdbc:postgresql://private-host/database", "private-login", "password-canary");
        var properties = new OperationsSubjectReadProperties(true, datasource, new Cursor(CURRENT.id(), List.of(CURRENT)));
        var rawList = new RawListRequest(Map.of("cursor", List.of(secret)), List.of("accept-canary"));
        var rawDetail = new RawDetailRequest(ANCHOR.subjectId().toString(), Map.of("private", List.of(secret)), List.of("accept-canary"));
        var request = OperationsSubjectReadRequest.list(new RawListRequest(Map.of("cursor", List.of(secret)), List.of()));
        for (Object value : List.of(CURRENT, properties.cursor(), datasource, properties, codec(CURRENT.id(), CURRENT), rawList, rawDetail, request, ANCHOR)) {
            assertThat(value.toString()).contains("REDACTED")
                    .doesNotContain(secret, CURRENT.material(), "password-canary", "private-login", "private-host", "accept-canary",
                            ANCHOR.subjectId().toString(), ANCHOR.createdAt().toString());
        }
        for (Code code : Code.values()) {
            var failure = new Failure(code);
            assertThat(failure.toString()).isEqualTo("Failure[REDACTED]");
            assertThat(failure.getMessage()).isEqualTo("Operations read failed");
            assertThat(failure.getCause()).isNull();
            assertThat(failure.getStackTrace()).isEmpty();
        }
    }

    private static OperationsSubjectCursorCodec codec(String active, KeyEntry... keys) {
        return new OperationsSubjectCursorCodec(new OperationsSubjectReadProperties(true, null, new Cursor(active, List.of(keys))));
    }

    private static KeyEntry key(String identifier, int fill) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) fill);
        return new KeyEntry(identifier, encode(bytes));
    }

    private static byte[] payload(long seconds, int nanos, byte[] digest) {
        return ByteBuffer.allocate(60).putLong(seconds).putInt(nanos).putLong(ANCHOR.subjectId().getMostSignificantBits())
                .putLong(ANCHOR.subjectId().getLeastSignificantBits()).put(digest).array();
    }

    private static String seal(KeyEntry key, byte[] payload, String purpose) throws Exception {
        byte[] identifier = key.id().getBytes(StandardCharsets.US_ASCII);
        byte[] header = ByteBuffer.allocate(2 + identifier.length).put((byte) 1).put((byte) identifier.length).put(identifier).array();
        byte[] nonce = new byte[12];
        new java.security.SecureRandom().nextBytes(nonce);
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.bytes(), "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8));
        cipher.updateAAD(header);
        byte[] encrypted = cipher.doFinal(payload);
        return encode(ByteBuffer.allocate(header.length + nonce.length + encrypted.length).put(header).put(nonce).put(encrypted).array());
    }

    private static byte[] decode(String token) {
        return Base64.getUrlDecoder().decode(token);
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void failure(Runnable action, Code code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(Failure.class, failure -> {
            assertThat(failure.code()).isEqualTo(code);
            assertThat(failure.getMessage()).isEqualTo("Operations read failed");
            assertThat(failure.getCause()).isNull();
        });
    }
}
