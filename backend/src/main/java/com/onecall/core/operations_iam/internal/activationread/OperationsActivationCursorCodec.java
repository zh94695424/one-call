package com.onecall.core.operations_iam.internal.activationread;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.Anchor;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class OperationsActivationCursorCodec {
    private static final byte[] PURPOSE = "onecall:operations-activation-list:cursor:v1\0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SORT = "occurredAt desc, transitionId desc\0".getBytes(StandardCharsets.UTF_8);
    private final String active;
    private final Map<String, SecretKeySpec> keys;
    private final SecureRandom random = new SecureRandom();

    public OperationsActivationCursorCodec(OperationsSubjectReadProperties properties) {
        if (properties == null || properties.cursor() == null) throw new Failure(Code.SERVICE_UNAVAILABLE);
        var cursor = properties.cursor();
        var snapshot = new LinkedHashMap<String, SecretKeySpec>();
        for (var entry : cursor.validatedKeys()) snapshot.put(entry.id(), new SecretKeySpec(entry.bytes(), "AES"));
        keys = Map.copyOf(snapshot);
        active = cursor.activeKeyId();
    }

    public String encode(Anchor anchor) {
        if (anchor == null || anchor.occurredAt() == null || anchor.transitionId() == null
                || anchor.occurredAt().getNano() % 1000 != 0) throw new Failure(Code.SERVICE_UNAVAILABLE);
        try {
            byte[] header = header(active);
            byte[] nonce = new byte[12];
            random.nextBytes(nonce);
            byte[] payload = ByteBuffer.allocate(28).putLong(anchor.occurredAt().getEpochSecond()).putInt(anchor.occurredAt().getNano())
                    .putLong(anchor.transitionId().getMostSignificantBits()).putLong(anchor.transitionId().getLeastSignificantBits()).array();
            byte[] encrypted = cipher(Cipher.ENCRYPT_MODE, keys.get(active), nonce, header).doFinal(payload);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(header.length + 12 + encrypted.length)
                    .put(header).put(nonce).put(encrypted).array());
        } catch (GeneralSecurityException unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }

    public Anchor decode(String cursor) {
        try {
            if (cursor == null || cursor.isEmpty() || cursor.length() > 512 || !cursor.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException();
            byte[] envelope = Base64.getUrlDecoder().decode(cursor);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(envelope).equals(cursor) || envelope.length < 59) throw new IllegalArgumentException();
            var buffer = ByteBuffer.wrap(envelope);
            if (buffer.get() != 1) throw new IllegalArgumentException();
            int length = Byte.toUnsignedInt(buffer.get());
            if (length < 1 || length > 32 || envelope.length != 58 + length) throw new IllegalArgumentException();
            byte[] identifier = new byte[length];
            buffer.get(identifier);
            String keyId = new String(identifier, StandardCharsets.US_ASCII);
            if (!keyId.matches("[A-Za-z0-9_-]{1,32}") || !keys.containsKey(keyId)) throw new IllegalArgumentException();
            byte[] nonce = new byte[12];
            buffer.get(nonce);
            byte[] encrypted = new byte[44];
            buffer.get(encrypted);
            byte[] plain = cipher(Cipher.DECRYPT_MODE, keys.get(keyId), nonce, header(keyId)).doFinal(encrypted);
            if (plain.length != 28) throw new IllegalArgumentException();
            var payload = ByteBuffer.wrap(plain);
            long seconds = payload.getLong();
            int nanos = payload.getInt();
            if (nanos < 0 || nanos >= 1_000_000_000 || nanos % 1000 != 0) throw new IllegalArgumentException();
            return new Anchor(Instant.ofEpochSecond(seconds, nanos), new UUID(payload.getLong(), payload.getLong()));
        } catch (BadPaddingException | RuntimeException invalid) { throw new Failure(Code.CURSOR_INVALID); }
        catch (GeneralSecurityException unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }

    private static byte[] header(String keyId) {
        byte[] identifier = keyId.getBytes(StandardCharsets.US_ASCII);
        return ByteBuffer.allocate(2 + identifier.length).put((byte) 1).put((byte) identifier.length).put(identifier).array();
    }

    private static Cipher cipher(int mode, SecretKeySpec key, byte[] nonce, byte[] header) throws GeneralSecurityException {
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(PURPOSE);
        cipher.updateAAD(header);
        cipher.updateAAD(SORT);
        cipher.updateAAD(MessageDigest.getInstance("SHA-256").digest("{}".getBytes(StandardCharsets.UTF_8)));
        return cipher;
    }
    @Override public String toString() { return "OperationsActivationCursorCodec[REDACTED]"; }
}
