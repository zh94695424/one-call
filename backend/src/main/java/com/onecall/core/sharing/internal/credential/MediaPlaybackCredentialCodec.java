package com.onecall.core.sharing.internal.credential;

import com.onecall.core.sharing.internal.playback.VerifiedMediaClaim;
import com.onecall.core.sharing.internal.playback.VerifiedMediaClaim.PlaybackWindow;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class MediaPlaybackCredentialCodec {
    static final String AUDIENCE = "onecall:media-playback:v1";
    private static final byte[] DOMAIN = "ONECALL-MEDIA-PLAYBACK\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] MAC_DOMAIN = "onecall-media-playback\0v1\0".getBytes(StandardCharsets.US_ASCII);

    private MediaPlaybackCredentialCodec() { }

    static long micros(Instant instant) {
        return Math.addExact(Math.multiplyExact(instant.getEpochSecond(), 1_000_000L), instant.getNano() / 1_000);
    }

    static Instant instant(long micros) {
        return Instant.ofEpochSecond(Math.floorDiv(micros, 1_000_000L), Math.floorMod(micros, 1_000_000L) * 1_000);
    }

    static String encode(VerifiedMediaClaim claim, MediaPlaybackSigningKeyProvider.Entry entry) throws Exception {
        ByteBuffer payload = ByteBuffer.allocate(4096);
        payload.put(DOMAIN).put((byte) 1);
        putText(payload, claim.audience());
        putUuid(payload, claim.shareGrantId());
        putUuid(payload, claim.shareAdmissionId());
        putUuid(payload, claim.evidenceId());
        putUuid(payload, claim.mediaObjectId());
        payload.putInt(claim.revocationGeneration()).putInt(claim.codeGeneration());
        putText(payload, claim.purpose().name());
        payload.putLong(micros(claim.issuedAt())).putLong(micros(claim.notBefore())).putLong(micros(claim.expiresAt()));
        payload.putInt(claim.durationSeconds()).putLong(claim.window().startInclusive().toNanos()).putLong(claim.window().endExclusive().toNanos());
        byte[] bytes = Arrays.copyOf(payload.array(), payload.position());
        String token = "ocmp1." + entry.kid() + "." + base64(bytes) + "." + base64(mac(entry, bytes));
        if (token.length() > 2048) throw new IllegalArgumentException("Invalid credential shape");
        return token;
    }

    static VerifiedMediaClaim decode(String token, java.util.Map<String, MediaPlaybackSigningKeyProvider.Entry> entries) throws Exception {
        if (token == null || token.isEmpty() || token.length() > 2048 || !StandardCharsets.US_ASCII.newEncoder().canEncode(token))
            throw new IllegalArgumentException("Invalid credential shape");
        String[] parts = token.split("\\.", -1);
        if (parts.length != 4 || !"ocmp1".equals(parts[0]) || !parts[1].matches("[A-Za-z0-9_-]{1,32}"))
            throw new IllegalArgumentException("Invalid credential shape");
        var entry = entries.get(parts[1]);
        if (entry == null) throw new IllegalArgumentException("Invalid credential shape");
        byte[] bytes = unbase64(parts[2]);
        byte[] signature = unbase64(parts[3]);
        if (bytes.length > 4096 || signature.length != 32 || !MessageDigest.isEqual(signature, mac(entry, bytes)))
            throw new IllegalArgumentException("Invalid credential shape");
        ByteBuffer payload = ByteBuffer.wrap(bytes);
        byte[] domain = new byte[DOMAIN.length];
        payload.get(domain);
        if (!Arrays.equals(domain, DOMAIN) || payload.get() != 1) throw new IllegalArgumentException("Invalid credential shape");
        String audience = text(payload);
        UUID grant = uuid(payload);
        UUID admission = uuid(payload);
        UUID evidence = uuid(payload);
        UUID object = uuid(payload);
        int revocation = payload.getInt();
        int code = payload.getInt();
        Purpose purpose = Purpose.valueOf(text(payload));
        Instant issued = instant(payload.getLong());
        Instant notBefore = instant(payload.getLong());
        Instant expires = instant(payload.getLong());
        int duration = payload.getInt();
        var window = new PlaybackWindow(Duration.ofNanos(payload.getLong()), Duration.ofNanos(payload.getLong()));
        if (payload.hasRemaining()) throw new IllegalArgumentException("Invalid credential shape");
        return new VerifiedMediaClaim(audience, grant, admission, evidence, object, revocation, code, purpose,
                issued, notBefore, expires, duration, window);
    }

    static boolean valid(VerifiedMediaClaim claim, Instant now) {
        return AUDIENCE.equals(claim.audience()) && claim.revocationGeneration() >= 0 && claim.codeGeneration() >= 0
                && claim.durationSeconds() > 0 && claim.issuedAt().equals(claim.notBefore())
                && !claim.issuedAt().isAfter(now) && claim.expiresAt().isAfter(now) && claim.expiresAt().isAfter(claim.issuedAt())
                && !claim.window().startInclusive().isNegative() && claim.window().endExclusive().compareTo(claim.window().startInclusive()) > 0
                && claim.window().endExclusive().compareTo(Duration.ofSeconds(claim.durationSeconds())) <= 0;
    }

    private static void putText(ByteBuffer payload, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 65535) throw new IllegalArgumentException("Invalid credential shape");
        payload.putShort((short) bytes.length).put(bytes);
    }
    private static String text(ByteBuffer payload) throws Exception {
        int length = Short.toUnsignedInt(payload.getShort());
        if (length > payload.remaining()) throw new IllegalArgumentException("Invalid credential shape");
        ByteBuffer bytes = payload.slice();
        bytes.limit(length);
        String result = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(bytes).toString();
        payload.position(payload.position() + length);
        return result;
    }
    private static void putUuid(ByteBuffer payload, UUID value) { payload.putLong(value.getMostSignificantBits()).putLong(value.getLeastSignificantBits()); }
    private static UUID uuid(ByteBuffer payload) { return new UUID(payload.getLong(), payload.getLong()); }
    private static String base64(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static byte[] unbase64(String text) {
        byte[] bytes = Base64.getUrlDecoder().decode(text);
        if (!base64(bytes).equals(text)) throw new IllegalArgumentException("Invalid credential shape");
        return bytes;
    }
    private static byte[] mac(MediaPlaybackSigningKeyProvider.Entry entry, byte[] payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(entry.material(), "HmacSHA256"));
        mac.update(MAC_DOMAIN);
        mac.update(entry.kid().getBytes(StandardCharsets.US_ASCII));
        mac.update((byte) 0);
        return mac.doFinal(payload);
    }
}
