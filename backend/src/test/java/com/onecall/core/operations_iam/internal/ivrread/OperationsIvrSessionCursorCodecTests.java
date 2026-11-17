package com.onecall.core.operations_iam.internal.ivrread;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders.*;
import com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders;
import com.onecall.core.operations_iam.internal.ivrread.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;


class OperationsIvrSessionCursorCodecTests {
    private final Anchor anchor = new Anchor(TIME, EMPLOYEE);
    private final byte[] digest = OperationsIvrSessionReadRequest.parse(raw(Map.of())).digest();
    private OperationsIvrSessionCursorCodec codec() { return new OperationsIvrSessionCursorCodec(properties()); }
    private void invalid(String token) { assertThatThrownBy(() -> codec().decode(token, digest)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.CURSOR_INVALID)); }
    private byte[] frame() { return Base64.getUrlDecoder().decode(codec().encode(anchor, digest)); }
    private String token(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private byte[] aad(String purpose, String endpoint, String sort, byte[] header) {
        var output = new java.io.ByteArrayOutputStream();
        for (byte[] value : new byte[][] {purpose.getBytes(java.nio.charset.StandardCharsets.UTF_8), endpoint.getBytes(java.nio.charset.StandardCharsets.UTF_8), sort.getBytes(java.nio.charset.StandardCharsets.UTF_8), header}) {
            output.writeBytes(java.nio.ByteBuffer.allocate(4).putInt(value.length).array()); output.writeBytes(value);
        } return output.toByteArray();
    }
    private String seal(byte[] plain, String purpose, String endpoint, String sort) throws Exception {
        byte[] identifier = properties().cursor().activeKeyId().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] header = java.nio.ByteBuffer.allocate(2 + identifier.length).put((byte) 1).put((byte) identifier.length).put(identifier).array();
        byte[] nonce = new byte[12]; var cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(new byte[32], "AES"), new javax.crypto.spec.GCMParameterSpec(128, nonce));
        cipher.updateAAD(aad(purpose, endpoint, sort, header));
        byte[] encrypted = cipher.doFinal(plain); return token(java.nio.ByteBuffer.allocate(header.length + 12 + encrypted.length).put(header).put(nonce).put(encrypted).array());
    }
    private byte[] plain() { return java.nio.ByteBuffer.allocate(60).putLong(TIME.getEpochSecond()).putInt(TIME.getNano()).putLong(EMPLOYEE.getMostSignificantBits()).putLong(EMPLOYEE.getLeastSignificantBits()).put(digest).array(); }
    private String seal(byte[] plain) throws Exception { return seal(plain, "onecall:operations-ivr-session-list:cursor:v1", "GET /api/v1/operations/ivr-sessions", "createdAt desc, sessionId desc"); }
    @Test
    void roundTripPreservesMicrosecondAnchorAndUuid() throws Exception {
        assertThat(codec().decode(codec().encode(anchor, digest), digest)).isEqualTo(anchor);
    }
    @Test
    void encodingIsRandomizedOpaqueCanonicalAndBounded() throws Exception {
        var first = codec().encode(anchor, digest); var second = codec().encode(anchor, digest); assertThat(first).isNotEqualTo(second).matches("[A-Za-z0-9_-]{122,163}");
        byte[] frame = Base64.getUrlDecoder().decode(first); assertThat(frame.length).isBetween(91, 122); assertThat(token(frame)).isEqualTo(first);
        int headerSize = 2 + Byte.toUnsignedInt(frame[1]); assertThat(frame.length - headerSize - 12).isEqualTo(76);
        var cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(javax.crypto.Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(new byte[32], "AES"), new javax.crypto.spec.GCMParameterSpec(128, Arrays.copyOfRange(frame, headerSize, headerSize + 12)));
        cipher.updateAAD(aad("onecall:operations-ivr-session-list:cursor:v1", "GET /api/v1/operations/ivr-sessions", "createdAt desc, sessionId desc", Arrays.copyOf(frame, headerSize)));
        assertThat(cipher.doFinal(Arrays.copyOfRange(frame, headerSize + 12, frame.length))).isEqualTo(plain());
        assertThat(java.util.HexFormat.of().formatHex(frame)).doesNotContain(java.util.HexFormat.of().formatHex(Arrays.copyOf(plain(), 28)), java.util.HexFormat.of().formatHex(digest));
    }
    @Test
    void lengthAlphabetVersionAndFrameFailuresAreCursorInvalid() throws Exception {
        for (String token : List.of("", "!", "a=", "a".repeat(513), "A")) invalid(token);
        byte[] frame = frame(); frame[0] = 2; invalid(token(frame)); invalid(token(Arrays.copyOf(frame(), 90))); frame = frame(); frame[1] = 0; invalid(token(frame)); invalid(token(Arrays.copyOf(frame(), frame().length + 1)));
    }
    @Test
    void tamperedHeaderNonceCiphertextAndTagAreCursorInvalid() throws Exception {
        for (int index : List.of(0, 1, 2, 10, 20, frame().length - 1)) { byte[] frame = frame(); frame[index] ^= 1; invalid(token(frame)); }
    }
    @Test
    void wrongPurposeEndpointAndSortAreCursorInvalid() throws Exception {
        invalid(seal(plain(), "wrong", "GET /api/v1/operations/ivr-sessions", "createdAt desc, sessionId desc"));
        invalid(seal(plain(), "onecall:operations-ivr-session-list:cursor:v1", "GET /other", "createdAt desc, sessionId desc"));
        invalid(seal(plain(), "onecall:operations-ivr-session-list:cursor:v1", "GET /api/v1/operations/ivr-sessions", "other"));
    }
    @Test
    void subjectAndActivationCursorsAreCursorInvalid() throws Exception {
        invalid(new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectCursorCodec(properties()).encode(new OperationsSubjectReadPorts.Anchor(TIME, EMPLOYEE), digest));
        invalid(new com.onecall.core.operations_iam.internal.activationread.OperationsActivationCursorCodec(properties()).encode(new OperationsActivationReadPorts.Anchor(TIME, EMPLOYEE)));
    }
    @Test
    void unknownAndRetiredKeysAreCursorInvalid() throws Exception {
        byte[] frame = frame(); frame[2] = 'X'; invalid(token(frame));
        var old = properties(); byte[] key = new byte[32]; key[0] = 1;
        var rotated = new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor("new", List.of(new OperationsSubjectReadProperties.KeyEntry("new", token(key)))));
        assertThatThrownBy(() -> new OperationsIvrSessionCursorCodec(rotated).decode(codec().encode(anchor, digest), digest)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.CURSOR_INVALID));
    }
    @Test
    void authenticatedChangedFilterDigestIsFilterMismatch() throws Exception {
        byte[] other = digest.clone(); other[0] ^= 1;
        assertThatThrownBy(() -> codec().decode(codec().encode(anchor, other), digest)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.CURSOR_FILTER_MISMATCH));
    }
    @Test
    void digestTamperingIsInvalidNotFilterMismatch() throws Exception {
        byte[] frame = frame(); frame[frame.length - 20] ^= 1; invalid(token(frame));
    }
    @Test
    void activeAndRetainedSharedKeysSupportRotation() throws Exception {
        byte[] material = new byte[32]; material[0] = 1; var key = new OperationsSubjectReadProperties.KeyEntry("new", token(material));
        var props = new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor("new", List.of(key, properties().cursor().keys().getFirst())));
        var rotated = new OperationsIvrSessionCursorCodec(props); assertThat(rotated.decode(codec().encode(anchor, digest), digest)).isEqualTo(anchor);
        assertThat(rotated.decode(rotated.encode(anchor, digest), digest)).isEqualTo(anchor); invalid(rotated.encode(anchor, digest));
    }
    @Test
    void missingInvalidAndAmbiguousKeyMaterialIsUnavailable() throws Exception {
        for (var cursor : List.of(new OperationsSubjectReadProperties.Cursor("missing", List.of()), new OperationsSubjectReadProperties.Cursor("bad", List.of(new OperationsSubjectReadProperties.KeyEntry("bad", "secret"))), new OperationsSubjectReadProperties.Cursor("Test-35", List.of(properties().cursor().keys().getFirst(), properties().cursor().keys().getFirst())))) assertThatThrownBy(() -> new OperationsIvrSessionCursorCodec(new OperationsSubjectReadProperties(true, null, cursor))).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> new OperationsIvrSessionCursorCodec(null)).isInstanceOf(Failure.class);
    }
    @Test
    void invalidAnchorPrecisionAndRedactedRepresentationsFailClosed() throws Exception {
        assertThatThrownBy(() -> codec().encode(new Anchor(TIME.plusNanos(1), EMPLOYEE), digest)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
        for (int nanos : List.of(-1, 1, 1_000_000_000)) { byte[] plain = plain(); java.nio.ByteBuffer.wrap(plain).putInt(8, nanos); invalid(seal(plain)); }
        invalid(seal(new byte[59])); invalid(seal(new byte[61]));
        assertThat(codec().toString()).isEqualTo("OperationsIvrSessionCursorCodec[REDACTED]"); assertThat(anchor.toString()).isEqualTo("Anchor[REDACTED]");
    }
}
