package com.onecall.core.operations_iam.internal.catalogread;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadPorts.Anchor;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class OperationsCatalogProductCursorCodec {
    private final String activeKeyId;
    private final Map<String, byte[]> keys = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public OperationsCatalogProductCursorCodec(OperationsSubjectReadProperties properties) {
        try {
            if (properties == null || properties.cursor() == null) throw new IllegalArgumentException();
            for (var key : properties.cursor().validatedKeys()) keys.put(key.id(), key.bytes());
            activeKeyId = properties.cursor().activeKeyId();
        } catch (RuntimeException invalid) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }
    public String encode(Anchor anchor, OperationsCatalogProductReadRequest request) {
        if (anchor == null || anchor.productId() == null || anchor.productCode() == null
                || !anchor.productCode().matches("[A-Z][A-Z0-9_-]{1,63}")) throw new Failure(Code.SERVICE_UNAVAILABLE);
        byte[] code = anchor.productCode().getBytes(StandardCharsets.UTF_8);
        byte[] plain = ByteBuffer.allocate(38 + code.length).put((byte) 1).putInt(request.limit())
                .put(request.digest(), 0, 16).putLong(anchor.productId().getMostSignificantBits())
                .putLong(anchor.productId().getLeastSignificantBits()).put((byte) code.length).put(code).array();
        byte[] header = header(activeKeyId);
        byte[] nonce = new byte[12]; random.nextBytes(nonce);
        try {
            byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, keys.get(activeKeyId), nonce, header, plain);
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(header.length + nonce.length + encrypted.length)
                    .put(header).put(nonce).put(encrypted).array());
            if (encoded.length() > 512) throw new Failure(Code.SERVICE_UNAVAILABLE);
            return encoded;
        } catch (GeneralSecurityException failure) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }
    public Anchor decode(String encoded, OperationsCatalogProductReadRequest request) {
        try {
            if (encoded == null || encoded.isEmpty() || encoded.length() > 512 || !encoded.matches("[A-Za-z0-9_-]+")) throw invalid();
            byte[] frame = Base64.getUrlDecoder().decode(encoded);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(frame).equals(encoded)) throw invalid();
            var bytes = ByteBuffer.wrap(frame);
            if (bytes.get() != 1) throw invalid();
            int keyLength = Byte.toUnsignedInt(bytes.get());
            if (keyLength < 1 || keyLength > 32 || bytes.remaining() < keyLength + 12 + 16 + 40) throw invalid();
            byte[] keyIdBytes = new byte[keyLength]; bytes.get(keyIdBytes);
            String keyId = new String(keyIdBytes, StandardCharsets.US_ASCII);
            if (!keyId.matches("[A-Za-z0-9_-]{1,32}") || !Arrays.equals(keyIdBytes, keyId.getBytes(StandardCharsets.US_ASCII))) throw invalid();
            byte[] key = keys.get(keyId);
            if (key == null) throw invalid();
            byte[] header = Arrays.copyOf(frame, 2 + keyLength);
            byte[] nonce = new byte[12]; bytes.get(nonce);
            byte[] encrypted = new byte[bytes.remaining()]; bytes.get(encrypted);
            var plain = ByteBuffer.wrap(crypt(Cipher.DECRYPT_MODE, key, nonce, header, encrypted));
            if (plain.get() != 1) throw invalid();
            int limit = plain.getInt();
            byte[] digest = new byte[16]; plain.get(digest);
            UUID productId = new UUID(plain.getLong(), plain.getLong());
            int codeLength = Byte.toUnsignedInt(plain.get());
            if (codeLength < 2 || codeLength > 64 || plain.remaining() != codeLength) throw invalid();
            byte[] codeBytes = new byte[codeLength]; plain.get(codeBytes);
            String productCode = new String(codeBytes, StandardCharsets.US_ASCII);
            if (!productCode.matches("[A-Z][A-Z0-9_-]{1,63}") || !Arrays.equals(codeBytes, productCode.getBytes(StandardCharsets.US_ASCII))
                    || limit < 1 || limit > 100) throw invalid();
            if (limit != request.limit() || !MessageDigest.isEqual(digest, Arrays.copyOf(request.digest(), 16))) throw new Failure(Code.CURSOR_FILTER_MISMATCH);
            return new Anchor(productCode, productId);
        } catch (Failure known) { throw known; }
        catch (GeneralSecurityException | RuntimeException malformed) { throw invalid(); }
    }
    private static byte[] header(String keyId) {
        byte[] bytes = keyId.getBytes(StandardCharsets.US_ASCII);
        return ByteBuffer.allocate(2 + bytes.length).put((byte) 1).put((byte) bytes.length).put(bytes).array();
    }
    private static byte[] crypt(int mode, byte[] key, byte[] nonce, byte[] header, byte[] bytes) throws GeneralSecurityException {
        var aad = new ByteArrayOutputStream();
        aad.writeBytes(OperationsCatalogProductReadRequest.lp(OperationsCatalogProductReadRequest.PURPOSE));
        aad.writeBytes(OperationsCatalogProductReadRequest.lp(OperationsCatalogProductReadRequest.ENDPOINT));
        aad.writeBytes(OperationsCatalogProductReadRequest.lp(OperationsCatalogProductReadRequest.SORT));
        aad.writeBytes(header);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD(aad.toByteArray());
        return cipher.doFinal(bytes);
    }
    private static Failure invalid() { return new Failure(Code.CURSOR_INVALID); }
}
