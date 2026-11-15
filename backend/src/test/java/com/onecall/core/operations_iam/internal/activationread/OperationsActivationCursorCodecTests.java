package com.onecall.core.operations_iam.internal.activationread;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.operations.OperationsActivationReadTestProviders.properties;
import com.onecall.core.operations_iam.internal.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.Anchor;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.junit.jupiter.api.Test;

class OperationsActivationCursorCodecTests {
    private final Anchor anchor = new Anchor(Instant.parse("2026-11-15T00:00:00.123456Z"), UUID.fromString("ffffffff-ffff-ffff-8000-000000000000"));
    private final OperationsActivationCursorCodec codec = new OperationsActivationCursorCodec(properties());
    private void failure(Code code, Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(Failure.class, problem -> {
            assertThat(problem.code()).isEqualTo(code); assertThat(problem.getCause()).isNull();
        });
    }
    private String encoded(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private OperationsSubjectReadProperties ring(String active, OperationsSubjectReadProperties.KeyEntry... entries) {
        return new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor(active, Arrays.asList(entries)));
    }
    private OperationsSubjectReadProperties.KeyEntry key(String id, int value) {
        byte[] material = new byte[32]; Arrays.fill(material, (byte) value);
        return new OperationsSubjectReadProperties.KeyEntry(id, encoded(material));
    }
    @Test
    void roundTripPreservesMicrosecondAnchorAndUuid() {
        for (Instant time : List.of(Instant.MIN, Instant.MAX.minusNanos(999), Instant.EPOCH, Instant.ofEpochSecond(-1, 999999000), anchor.occurredAt())) {
            var value = new Anchor(time, anchor.transitionId()); assertThat(codec.decode(codec.encode(value))).isEqualTo(value);
        }
    }
    @Test
    void encodingIsRandomizedOpaqueCanonicalAndBounded() {
        String first = codec.encode(anchor); String second = codec.encode(anchor);
        assertThat(first).isNotEqualTo(second).matches("[A-Za-z0-9_-]{1,120}").doesNotContain(anchor.transitionId().toString(), anchor.occurredAt().toString());
        byte[] frame = Base64.getUrlDecoder().decode(first); assertThat(frame).hasSize(58 + "Test-34".length());
        assertThat(encoded(frame)).isEqualTo(first); assertThat(frame[0]).isEqualTo((byte) 1);
    }
    @Test
    void malformedLengthsAlphabetVersionsAndFramesAreCursorInvalid() {
        for (String value : List.of("", "a".repeat(513), "a=", "a b", "a+", "a/", "a", codec.encode(anchor) + "=", "_")) failure(Code.CURSOR_INVALID, () -> codec.decode(value));
        failure(Code.CURSOR_INVALID, () -> codec.decode(null));
        byte[] valid = Base64.getUrlDecoder().decode(codec.encode(anchor));
        for (int length : List.of(0, 1, 58, valid.length - 1, valid.length + 1)) failure(Code.CURSOR_INVALID, () -> codec.decode(encoded(Arrays.copyOf(valid, length))));
        for (int index : List.of(0, 1, 2)) { byte[] frame = valid.clone(); frame[index] = (byte) 255; failure(Code.CURSOR_INVALID, () -> codec.decode(encoded(frame))); }
    }
    @Test
    void tamperedHeaderNonceCiphertextAndTagAreCursorInvalid() {
        byte[] valid = Base64.getUrlDecoder().decode(codec.encode(anchor));
        for (int index = 0; index < valid.length; index++) {
            byte[] frame = valid.clone(); frame[index] ^= 1; failure(Code.CURSOR_INVALID, () -> codec.decode(encoded(frame)));
        }
    }
    @Test
    void subjectAndOtherPurposeCursorsAreCursorInvalid() throws Exception {
        var subject = new OperationsSubjectCursorCodec(properties()); byte[] digest = new byte[32];
        String token = subject.encode(new com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.Anchor(anchor.occurredAt(), anchor.transitionId()), digest);
        failure(Code.CURSOR_INVALID, () -> codec.decode(token));
        failure(Code.CURSOR_INVALID, () -> subject.decode(codec.encode(anchor), digest));
        for (String purpose : List.of("other\0", "onecall:operations-activation-list:cursor:v1")) {
            String wrong = authenticated(payload(anchor.occurredAt().getEpochSecond(), anchor.occurredAt().getNano()), purpose, "occurredAt desc, transitionId desc\0", "{}");
            failure(Code.CURSOR_INVALID, () -> codec.decode(wrong));
        }
        for (String sort : List.of("occurredAt asc, transitionId desc\0", "occurredAt desc, transitionId desc")) {
            String wrong = authenticated(payload(0, 0), "onecall:operations-activation-list:cursor:v1\0", sort, "{}"); failure(Code.CURSOR_INVALID, () -> codec.decode(wrong));
        }
        String wrong = authenticated(payload(0, 0), "onecall:operations-activation-list:cursor:v1\0", "occurredAt desc, transitionId desc\0", "{\"filter\":1}");
        failure(Code.CURSOR_INVALID, () -> codec.decode(wrong));
    }
    @Test
    void wrongUnknownAndRetiredKeysAreCursorInvalid() {
        for (var configuration : List.of(ring("unknown", key("unknown", 0)), ring("Test-34", key("Test-34", 1)))) {
            var other = new OperationsActivationCursorCodec(configuration); failure(Code.CURSOR_INVALID, () -> other.decode(codec.encode(anchor)));
        }
    }
    @Test
    void activeAndRetainedKeysSupportRotation() {
        var old = key("old", 1); var active = key("active", 2);
        var previous = new OperationsActivationCursorCodec(ring("old", old)); var rotated = new OperationsActivationCursorCodec(ring("active", old, active));
        assertThat(rotated.decode(previous.encode(anchor))).isEqualTo(anchor);
        String token = rotated.encode(anchor); byte[] frame = Base64.getUrlDecoder().decode(token);
        assertThat(new String(frame, 2, frame[1], StandardCharsets.US_ASCII)).isEqualTo("active");
        failure(Code.CURSOR_INVALID, () -> previous.decode(token));
    }
    @Test
    void missingInvalidAndDuplicateKeyConfigurationIsUnavailable() {
        failure(Code.SERVICE_UNAVAILABLE, () -> new OperationsActivationCursorCodec(null));
        failure(Code.SERVICE_UNAVAILABLE, () -> new OperationsActivationCursorCodec(new OperationsSubjectReadProperties(true, null, null)));
        for (var configuration : List.of(ring("missing"), ring("bad", key("other", 0)), ring("same", key("same", 0), key("same", 1)), ring("one", key("one", 0), key("two", 0)), ring("bad id", key("bad id", 0)), ring("bad", new OperationsSubjectReadProperties.KeyEntry("bad", "invalid")), ring("one", key("one", 0), key("two", 1), key("three", 2), key("four", 3)))) {
            failure(Code.SERVICE_UNAVAILABLE, () -> new OperationsActivationCursorCodec(configuration));
        }
    }
    @Test
    void authenticatedMalformedAnchorPayloadsFailClosed() throws Exception {
        for (byte[] invalid : List.of(new byte[27], new byte[29], payload(Long.MAX_VALUE, 0), payload(Long.MIN_VALUE, 0), payload(0, -1), payload(0, 1000000000), payload(0, 1))) {
            String token = authenticated(invalid, "onecall:operations-activation-list:cursor:v1\0", "occurredAt desc, transitionId desc\0", "{}");
            failure(Code.CURSOR_INVALID, () -> codec.decode(token));
        }
        for (Anchor invalid : Arrays.asList(null, new Anchor(null, anchor.transitionId()), new Anchor(Instant.EPOCH, null), new Anchor(Instant.ofEpochSecond(0, 1), anchor.transitionId()))) failure(Code.SERVICE_UNAVAILABLE, () -> codec.encode(invalid));
    }
    @Test
    void cursorHasNoLimitOrFilterBinding() throws Exception {
        assertThat(Arrays.stream(Anchor.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("occurredAt", "transitionId");
        String token = authenticated(payload(anchor.occurredAt().getEpochSecond(), anchor.occurredAt().getNano()), "onecall:operations-activation-list:cursor:v1\0", "occurredAt desc, transitionId desc\0", "{}");
        for (int limit : List.of(1, 20, 100)) {
            var request = OperationsActivationReadRequest.parse(new com.onecall.core.operations_iam.subjectread.OperationsActivationReadService.RawListRequest(Map.of("cursor", List.of(token), "limit", List.of("" + limit)), List.of()));
            assertThat(codec.decode(request.cursor())).isEqualTo(anchor);
        }
    }
    private byte[] payload(long seconds, int nanos) {
        return ByteBuffer.allocate(28).putLong(seconds).putInt(nanos).putLong(anchor.transitionId().getMostSignificantBits()).putLong(anchor.transitionId().getLeastSignificantBits()).array();
    }
    private String authenticated(byte[] plain, String purpose, String sort, String filters) throws Exception {
        byte[] id = "Test-34".getBytes(StandardCharsets.US_ASCII);
        byte[] header = ByteBuffer.allocate(2 + id.length).put((byte) 1).put((byte) id.length).put(id).array(); byte[] nonce = new byte[12];
        var cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[32], "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD(purpose.getBytes(StandardCharsets.UTF_8)); cipher.updateAAD(header); cipher.updateAAD(sort.getBytes(StandardCharsets.UTF_8)); cipher.updateAAD(MessageDigest.getInstance("SHA-256").digest(filters.getBytes(StandardCharsets.UTF_8)));
        byte[] encrypted = cipher.doFinal(plain); return encoded(ByteBuffer.allocate(header.length + 12 + encrypted.length).put(header).put(nonce).put(encrypted).array());
    }
}
