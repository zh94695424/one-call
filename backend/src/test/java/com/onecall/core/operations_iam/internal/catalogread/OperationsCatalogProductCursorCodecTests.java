package com.onecall.core.operations_iam.internal.catalogread;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadPorts.Anchor;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.nio.ByteBuffer;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class OperationsCatalogProductCursorCodecTests {
    final OperationsCatalogProductReadRequest request = new OperationsCatalogProductReadRequest(20, null);
    final Anchor anchor = new Anchor("AB", PRODUCT);
    final OperationsCatalogProductCursorCodec codec = new OperationsCatalogProductCursorCodec(properties());
    private void fails(Code code, Runnable operation) { assertThatThrownBy(operation::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(code)); }
    private String reframe(java.util.function.UnaryOperator<byte[]> mutation) throws Exception {
        byte[] frame = Base64.getUrlDecoder().decode(codec.encode(anchor, request));
        byte[] header = Arrays.copyOf(frame, 9), nonce = Arrays.copyOfRange(frame, 9, 21);
        var aad = new java.io.ByteArrayOutputStream();
        for (String value : List.of(OperationsCatalogProductReadRequest.PURPOSE, OperationsCatalogProductReadRequest.ENDPOINT, OperationsCatalogProductReadRequest.SORT)) aad.writeBytes(OperationsCatalogProductReadRequest.lp(value));
        aad.writeBytes(header);
        var cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(new byte[32], "AES"), new GCMParameterSpec(128, nonce)); cipher.updateAAD(aad.toByteArray());
        byte[] plain = mutation.apply(cipher.doFinal(Arrays.copyOfRange(frame, 21, frame.length)));
        new java.security.SecureRandom().nextBytes(nonce); cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[32], "AES"), new GCMParameterSpec(128, nonce)); cipher.updateAAD(aad.toByteArray());
        byte[] encrypted = cipher.doFinal(plain);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(21 + encrypted.length).put(header).put(nonce).put(encrypted).array());
    }
    private String alter(String token, int offset) { byte[] bytes = Base64.getUrlDecoder().decode(token); bytes[offset] ^= 1; return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    @Test void roundTripsExactFrame() {
        String token = codec.encode(anchor, request); assertThat(codec.decode(token, request)).isEqualTo(anchor);
        byte[] bytes = Base64.getUrlDecoder().decode(token);
        assertThat(bytes[0]).isEqualTo((byte) 1); assertThat(bytes[1]).isEqualTo((byte) 7);
        assertThat(Arrays.copyOfRange(bytes, 2, 9)).isEqualTo("Test-37".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        assertThat(bytes).hasSize(9 + 12 + 40 + 16); assertThat(codec.encode(anchor, request)).isNotEqualTo(token);
        byte[] material = new byte[32]; Arrays.fill(material, (byte) 7);
        var active = new OperationsSubjectReadProperties.KeyEntry("Active-37", Base64.getUrlEncoder().withoutPadding().encodeToString(material));
        var rotated = new OperationsCatalogProductCursorCodec(new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor(active.id(), List.of(active, properties().cursor().keys().getFirst()))));
        assertThat(rotated.decode(token, request)).isEqualTo(anchor);
        String current = rotated.encode(anchor, request); byte[] activeFrame = Base64.getUrlDecoder().decode(current);
        assertThat(new String(activeFrame, 2, Byte.toUnsignedInt(activeFrame[1]), java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("Active-37");
        assertThat(rotated.decode(current, request)).isEqualTo(anchor); fails(Code.CURSOR_INVALID, () -> codec.decode(current, request));
    }
    @Test void rejectsTampering() {
        String token = codec.encode(anchor, request);
        for (int offset : List.of(0, 1, 2, 10, 25, Base64.getUrlDecoder().decode(token).length - 1)) fails(Code.CURSOR_INVALID, () -> codec.decode(alter(token, offset), request));
    }
    @Test void rejectsWrongPurpose() throws Exception {
        String token = codec.encode(anchor, request); byte[] frame = Base64.getUrlDecoder().decode(token);
        byte[] header = Arrays.copyOf(frame, 9), nonce = Arrays.copyOfRange(frame, 9, 21);
        var aad = new java.io.ByteArrayOutputStream();
        for (String value : List.of(OperationsCatalogProductReadRequest.PURPOSE, OperationsCatalogProductReadRequest.ENDPOINT, OperationsCatalogProductReadRequest.SORT)) aad.writeBytes(OperationsCatalogProductReadRequest.lp(value));
        aad.writeBytes(header);
        var cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(new byte[32], "AES"), new GCMParameterSpec(128, nonce)); cipher.updateAAD(aad.toByteArray());
        byte[] plain = cipher.doFinal(Arrays.copyOfRange(frame, 21, frame.length)); plain[0] = 2;
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[32], "AES"), new GCMParameterSpec(128, nonce)); cipher.updateAAD(aad.toByteArray());
        byte[] encrypted = cipher.doFinal(plain);
        String wrong = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(21 + encrypted.length).put(header).put(nonce).put(encrypted).array());
        fails(Code.CURSOR_INVALID, () -> codec.decode(wrong, request));
        for (String tuple : List.of("purpose", "endpoint", "sort")) {
            var wrongAad = new java.io.ByteArrayOutputStream();
            wrongAad.writeBytes(OperationsCatalogProductReadRequest.lp(tuple.equals("purpose") ? "wrong" : OperationsCatalogProductReadRequest.PURPOSE));
            wrongAad.writeBytes(OperationsCatalogProductReadRequest.lp(tuple.equals("endpoint") ? "wrong" : OperationsCatalogProductReadRequest.ENDPOINT));
            wrongAad.writeBytes(OperationsCatalogProductReadRequest.lp(tuple.equals("sort") ? "wrong" : OperationsCatalogProductReadRequest.SORT)); wrongAad.writeBytes(header);
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[32], "AES"), new GCMParameterSpec(128, nonce)); cipher.updateAAD(wrongAad.toByteArray());
            byte[] payload = cipher.doFinal(plain);
            String invalid = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(21 + payload.length).put(header).put(nonce).put(payload).array());
            fails(Code.CURSOR_INVALID, () -> codec.decode(invalid, request));
        }
    }
    @Test void rejectsFilterMismatch() throws Exception {
        fails(Code.CURSOR_FILTER_MISMATCH, () -> codec.decode(codec.encode(anchor, request), new OperationsCatalogProductReadRequest(1, null)));
        String differentDigest = reframe(bytes -> { bytes[5] ^= 1; return bytes; });
        fails(Code.CURSOR_FILTER_MISMATCH, () -> codec.decode(differentDigest, request));
    }
    @Test void rejectsMissingAndRetiredKeys() {
        var next = new OperationsSubjectReadProperties.KeyEntry("Next", Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]));
        var removed = new OperationsCatalogProductCursorCodec(new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor("Next", List.of(next))));
        fails(Code.CURSOR_INVALID, () -> removed.decode(codec.encode(anchor, request), request));
        fails(Code.SERVICE_UNAVAILABLE, () -> new OperationsCatalogProductCursorCodec(null));
        fails(Code.SERVICE_UNAVAILABLE, () -> new OperationsCatalogProductCursorCodec(new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor("missing", List.of(next)))));
    }
    @Test void rejectsMalformedFrame() throws Exception {
        for (String value : List.of("", "!", "AA", "AQE", "AA==", codec.encode(anchor, request) + "A", reframe(bytes -> { bytes[37] = 65; return bytes; }), reframe(bytes -> { bytes[38] = (byte) 255; return bytes; }), reframe(bytes -> Arrays.copyOf(bytes, bytes.length + 1)))) fails(Code.CURSOR_INVALID, () -> codec.decode(value, request));
    }
    @Test void rejectsOversizedEncodedCursor() { fails(Code.CURSOR_INVALID, () -> codec.decode("A".repeat(513), request)); }
    @Test void keepsGeneratedCursorWithinOpenApiMaximum() {
        var key = new OperationsSubjectReadProperties.KeyEntry("K".repeat(32), properties().cursor().keys().getFirst().material());
        var maximum = new OperationsCatalogProductCursorCodec(new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor(key.id(), List.of(key))));
        var fullAnchor = new Anchor("A".repeat(64), PRODUCT); String token = maximum.encode(fullAnchor, request);
        assertThat(token).hasSize(219); assertThat(token.length()).isLessThanOrEqualTo(512); assertThat(Base64.getUrlDecoder().decode(token)).hasSize(164); assertThat(maximum.decode(token, request)).isEqualTo(fullAnchor);
    }
}
