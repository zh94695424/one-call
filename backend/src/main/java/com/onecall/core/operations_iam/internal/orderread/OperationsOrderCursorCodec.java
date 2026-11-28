package com.onecall.core.operations_iam.internal.orderread;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.Anchor;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class OperationsOrderCursorCodec {
    private static final int PAYLOAD_LENGTH = 61;
    private final Map<String, byte[]> keys;
    private final String activeKey;
    private final SecureRandom random = new SecureRandom();

    public OperationsOrderCursorCodec(OperationsSubjectReadProperties properties) {
        try {
            if (properties == null || properties.cursor() == null) throw new IllegalArgumentException();
            var configured = new HashMap<String, byte[]>();
            for (var entry : properties.cursor().validatedKeys()) configured.put(entry.id(), entry.bytes());
            keys = Map.copyOf(configured);
            activeKey = properties.cursor().activeKeyId();
        } catch (RuntimeException failure) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }
    static byte[] frame(String encoded) {
        try {
            if (encoded == null || encoded.isEmpty() || encoded.length() > 512 || !encoded.matches("[A-Za-z0-9_-]+")) throw invalid();
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(encoded) || bytes.length < 2) throw invalid();
            int length = Byte.toUnsignedInt(bytes[1]);
            if (bytes[0] != 1 || length < 1 || length > 32 || bytes.length != 2 + length + 12 + PAYLOAD_LENGTH + 16) throw invalid();
            byte[] identifier = Arrays.copyOfRange(bytes, 2, 2 + length);
            String key = new String(identifier, StandardCharsets.US_ASCII);
            if (!key.matches("[A-Za-z0-9_-]{1,32}") || !Arrays.equals(identifier, key.getBytes(StandardCharsets.US_ASCII))) throw invalid();
            return bytes;
        } catch (Failure known) { throw known; }
        catch (RuntimeException malformed) { throw invalid(); }
    }
    public String encode(Anchor anchor, OperationsOrderReadRequest request) {
        if (anchor == null || anchor.orderId() == null || !OperationsOrderReadPorts.validInstant(anchor.createdAt())) throw new Failure(Code.INTERNAL);
        try {
            byte[] keyId = activeKey.getBytes(StandardCharsets.US_ASCII);
            byte[] header = ByteBuffer.allocate(2 + keyId.length).put((byte) 1).put((byte) keyId.length).put(keyId).array();
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            byte[] payload = ByteBuffer.allocate(PAYLOAD_LENGTH).put((byte) 1).put(request.digest())
                    .putLong(anchor.createdAt().getEpochSecond()).putInt(anchor.createdAt().getNano())
                    .putLong(anchor.orderId().getMostSignificantBits()).putLong(anchor.orderId().getLeastSignificantBits()).array();
            byte[] ciphertext = crypt(Cipher.ENCRYPT_MODE, keys.get(activeKey), nonce, header, payload);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(header.length + nonce.length + ciphertext.length)
                    .put(header).put(nonce).put(ciphertext).array());
        } catch (GeneralSecurityException failure) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }
    public Anchor decode(String encoded, OperationsOrderReadRequest request) {
        try {
            byte[] bytes = frame(encoded);
            int keyLength = Byte.toUnsignedInt(bytes[1]);
            String keyId = new String(bytes, 2, keyLength, StandardCharsets.US_ASCII);
            byte[] key = keys.get(keyId);
            if (key == null) throw invalid();
            int headerLength = 2 + keyLength;
            var payload = ByteBuffer.wrap(crypt(Cipher.DECRYPT_MODE, key,
                    Arrays.copyOfRange(bytes, headerLength, headerLength + 12), Arrays.copyOf(bytes, headerLength),
                    Arrays.copyOfRange(bytes, headerLength + 12, bytes.length)));
            if (payload.remaining() != PAYLOAD_LENGTH || payload.get() != 1) throw invalid();
            byte[] digest = new byte[32];
            payload.get(digest);
            long seconds = payload.getLong();
            int nanos = payload.getInt();
            if (nanos < 0 || nanos > 999999999) throw invalid();
            Instant instant = Instant.ofEpochSecond(seconds, nanos);
            UUID identity = new UUID(payload.getLong(), payload.getLong());
            if (payload.hasRemaining() || !OperationsOrderReadPorts.validInstant(instant)) throw invalid();
            if (!MessageDigest.isEqual(digest, request.digest())) throw new Failure(Code.CURSOR_FILTER_MISMATCH);
            return new Anchor(instant, identity);
        } catch (Failure known) { throw known; }
        catch (GeneralSecurityException | RuntimeException malformed) { throw invalid(); }
    }
    private static byte[] crypt(int mode, byte[] key, byte[] nonce, byte[] header, byte[] payload) throws GeneralSecurityException {
        var aad = new ByteArrayOutputStream();
        aad.writeBytes(OperationsOrderReadRequest.lp(OperationsOrderReadRequest.PURPOSE));
        aad.writeBytes(OperationsOrderReadRequest.lp(OperationsOrderReadRequest.ENDPOINT));
        aad.writeBytes(OperationsOrderReadRequest.lp(OperationsOrderReadRequest.SORT));
        aad.writeBytes(header);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD(aad.toByteArray());
        return cipher.doFinal(payload);
    }
    private static Failure invalid() { return new Failure(Code.CURSOR_INVALID); }
    @Override public String toString() { return "OperationsOrderCursorCodec[REDACTED]"; }
}
