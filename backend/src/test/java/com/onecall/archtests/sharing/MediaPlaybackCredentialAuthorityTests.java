package com.onecall.archtests.sharing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider;
import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.*;
import com.onecall.core.sharing.internal.credential.*;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Entry;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Role;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

class MediaPlaybackCredentialAuthorityTests {
    private static final Instant NOW = Instant.parse("2026-01-02T03:04:05.678901Z");
    private static final Instant EXPIRY = NOW.plusSeconds(3600);
    private static final byte[] KEY = HexFormat.of().parseHex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
    private static final String PREFIX = "4f4e4543414c4c2d4d454449412d504c41594241434b000100196f6e6563616c6c3a6d656469612d706c61796261636b3a763100000000000000000000000000000001000000000000000000000000000000020000000000000000000000000000000300000000000000000000000000000004000000070000000b";
    private static final String SUFFIX = "0006475ef6574f350006475ef6574f350006475fcceaf33500000e1000000000000000000000034630b8a000";

    @Test void goldenPlaybackRoundTripsExactClaim() throws Exception { golden(Purpose.PLAYBACK, "0008504c41594241434b", 177, 289, "657fe80ef35b9f8a33d0ea0fb6667ecaa2c447ca56f4b64557c30d7c5f8b3195"); }
    @Test void goldenSeekRoundTripsExactClaim() throws Exception { golden(Purpose.SEEK, "00045345454b", 173, 284, "528d2bd11ad6dc249cb1d7658ab976857d9030bbd83307427928ac17fb027684"); }
    @Test void goldenRenewRoundTripsExactClaim() throws Exception { golden(Purpose.RENEW, "000552454e4557", 174, 285, "af77d4d861dbb28e4497d654db1d683b906cdd586f2d80893d7c7592411370e3"); }

    @Test void wireEnvelopeAndDomainSeparationAreExact() throws Exception {
        String token = issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null)).credential();
        for (String invalid : List.of("", token + ".", token + " ", " " + token, token.replace("ocmp1", "ocmp2"), token.replace('.', '/'), "é" + token)) invalid(invalid);
        byte[] bytes = payload(token); bytes[0] ^= 1; invalid(sign(bytes, "K1"));
        bytes = payload(token); bytes[23] = 2; invalid(sign(bytes, "K1"));
        assertThat(token.split("\\.")).hasSize(4);
    }

    @Test void truncationTrailingLengthsAndOverflowAreRejected() throws Exception {
        byte[] bytes = payload(issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null)).credential());
        for (int length = 0; length < bytes.length; length++) invalid(sign(Arrays.copyOf(bytes, length), "K1"));
        invalid(sign(Arrays.copyOf(bytes, bytes.length + 1), "K1"));
        for (int offset : List.of(24, 123)) {
            byte[] changed = bytes.clone(); changed[offset] = (byte) 255; changed[offset + 1] = (byte) 255; invalid(sign(changed, "K1"));
        }
        invalid(sign(new byte[4097], "K1")); invalid("x".repeat(2049));
        for (int size : List.of(0, 1, 31, 33)) invalid("ocmp1.K1." + b64(bytes) + "." + b64(new byte[size]));
    }

    @Test void utf8Base64AndUnknownEnumsAreRejected() throws Exception {
        String token = issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null)).credential();
        String[] parts = token.split("\\.");
        invalid("ocmp1.K1." + parts[2] + "=." + parts[3]); invalid(token + "="); invalid(token.replace(parts[2], "!"));
        invalid(token.substring(0, token.length() - 1) + "V");
        byte[] bytes = payload(token); bytes[26] = (byte) 0xff; invalid(sign(bytes, "K1"));
        bytes = payload(token); bytes[125] = 'X'; invalid(sign(bytes, "K1"));
    }

    @Test void claimShapeAudienceAndTimeBoundsFailClosed() throws Exception {
        byte[] bytes = payload(issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null)).credential());
        byte[] changed = bytes.clone(); changed[26] = 'X'; invalid(sign(changed, "K1"));
        for (int offset : List.of(115, 119, 157)) { changed = bytes.clone(); ByteBuffer.wrap(changed).putInt(offset, -1); invalid(sign(changed, "K1")); }
        for (int offset : List.of(133, 141, 149)) {
            changed = bytes.clone(); ByteBuffer.wrap(changed).putLong(offset, offset == 149 ? micros(NOW) : micros(NOW.plusSeconds(1))); invalid(sign(changed, "K1"));
        }
        for (long start : List.of(-1L, 3_600_000_000_000L)) { changed = bytes.clone(); ByteBuffer.wrap(changed).putLong(161, start); invalid(sign(changed, "K1")); }
        for (long end : List.of(0L, -1L, 3_600_000_000_001L)) { changed = bytes.clone(); ByteBuffer.wrap(changed).putLong(169, end); invalid(sign(changed, "K1")); }
        assertThat(authority(Clock.fixed(EXPIRY, ZoneOffset.UTC), keys()).verify(new OpaquePlaybackCredential(sign(bytes, "K1"))))
                .isInstanceOf(MediaCredentialVerifier.Result.Invalid.class);
    }

    @Test void tamperAcrossEverySignedFieldIsRejected() {
        String token = issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null)).credential();
        String signature = token.split("\\.")[3]; byte[] bytes = payload(token);
        for (int offset = 0; offset < bytes.length; offset++) {
            byte[] changed = bytes.clone(); changed[offset] ^= 1; invalid("ocmp1.K1." + b64(changed) + "." + signature);
        }
        invalid(token.replace(".K1.", ".K2."));
    }

    @Test void freshIssuanceUsesOnlyActiveKey() {
        var rotated = authority(Clock.fixed(NOW, ZoneOffset.UTC), snapshot(entry("K2", Role.ACTIVE), List.of(entry("K1", Role.VERIFY_ONLY))));
        assertThat(issued(rotated, request(Purpose.PLAYBACK, EXPIRY, null)).credential()).startsWith("ocmp1.K2.");
    }

    @Test void verifyOnlyOverlapVerifiesAndReplaysRecordedKey() {
        var first = issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null));
        var rotated = authority(Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC), snapshot(entry("K2", Role.ACTIVE), List.of(entry("K1", Role.VERIFY_ONLY))));
        assertThat(rotated.verify(new OpaquePlaybackCredential(first.credential()))).isInstanceOf(MediaCredentialVerifier.Result.Verified.class);
        assertThat(issued(rotated, request(Purpose.PLAYBACK, EXPIRY, first.replayAnchor()))).isEqualTo(first);
    }

    @Test void disabledAbsentAndUnknownKeysFailClosed() {
        var first = issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null));
        var rotated = authority(Clock.fixed(NOW, ZoneOffset.UTC), snapshot(entry("K2", Role.ACTIVE), List.of()));
        assertThat(rotated.verify(new OpaquePlaybackCredential(first.credential()))).isInstanceOf(MediaCredentialVerifier.Result.Invalid.class);
        assertThat(rotated.issueOrReplay(request(Purpose.PLAYBACK, EXPIRY, first.replayAnchor()))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
        unavailable(authority(Clock.fixed(NOW, ZoneOffset.UTC), () -> new MediaPlaybackSigningKeyProvider.Result.Unavailable()));
    }

    @Test void invalidKeySnapshotsAndProviderFailuresAreUnavailable() {
        for (MediaPlaybackSigningKeyProvider provider : Arrays.<MediaPlaybackSigningKeyProvider>asList(() -> null,
                () -> { throw new IllegalStateException("private-key-canary"); }, snapshot(null, List.of()),
                snapshot(entry("K1", Role.VERIFY_ONLY), List.of()), snapshot(entry("K1", Role.ACTIVE), null),
                snapshot(entry("K1", Role.ACTIVE), List.of(entry("K1", Role.VERIFY_ONLY))),
                snapshot(entry("K1", Role.ACTIVE), List.of(entry("K2", Role.ACTIVE))),
                snapshot(new Entry("bad.kid", KEY, Role.ACTIVE), List.of()), snapshot(new Entry("K1", new byte[31], Role.ACTIVE), List.of()),
                snapshot(new Entry("K1", null, Role.ACTIVE), List.of()), snapshot(new Entry("K1", KEY, null), List.of()))) {
            AtomicInteger calls = new AtomicInteger();
            var authority = authority(Clock.fixed(NOW, ZoneOffset.UTC), () -> { calls.incrementAndGet(); return provider.currentKeys(); });
            unavailable(authority); assertThat(calls).hasValue(5);
        }
        ObjectProvider<MediaPlaybackSigningKeyProvider> broken = mock(ObjectProvider.class);
        when(broken.stream()).thenThrow(new IllegalStateException("private-provider-canary"));
        unavailable(new MediaPlaybackCredentialAuthority(broken, Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    @Test void zeroAndAmbiguousProvidersAreUnavailableEvenWithPrimary() {
        unavailable(authority(Clock.fixed(NOW, ZoneOffset.UTC)));
        AtomicInteger calls = new AtomicInteger();
        MediaPlaybackSigningKeyProvider provider = () -> { calls.incrementAndGet(); return keys().currentKeys(); };
        unavailable(authority(Clock.fixed(NOW, ZoneOffset.UTC), provider, provider)); assertThat(calls).hasValue(0);
    }

    @Test void keyMaterialAndOverlapListsAreDefensivelyCopied() {
        byte[] bytes = KEY.clone(); var entry = new Entry("K1", bytes, Role.ACTIVE); bytes[0] ^= 1;
        byte[] exposed = entry.material(); exposed[1] ^= 1; assertThat(entry.material()).containsExactly(KEY);
        List<Entry> overlap = new ArrayList<>(List.of(entry("K2", Role.VERIFY_ONLY)));
        var snapshot = new MediaPlaybackSigningKeyProvider.Result.Available(entry, overlap); overlap.clear();
        assertThat(snapshot.verifyOnly()).hasSize(1); assertThatThrownBy(() -> snapshot.verifyOnly().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void nonMicrosecondClocksNormalizeDeterministically() {
        var sub = authority(Clock.fixed(NOW.plusNanos(999), ZoneOffset.UTC), keys());
        assertThat(issued(sub, request(Purpose.PLAYBACK, EXPIRY.plusNanos(999), null)))
                .isEqualTo(issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null)));
    }

    @Test void advancingClockReplayReturnsFirstCredential() {
        var first = issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null));
        assertThat(issued(authority(Clock.fixed(NOW.plusSeconds(60), ZoneOffset.UTC), keys()), request(Purpose.PLAYBACK, EXPIRY, first.replayAnchor()))).isEqualTo(first);
    }

    @Test void freshAuthorityReconstructsCommittedAnchor() {
        var first = issued(authority(), request(Purpose.RENEW, EXPIRY, null));
        assertThat(issued(authority(), request(Purpose.RENEW, EXPIRY, first.replayAnchor()))).isEqualTo(first);
    }

    @Test void malformedLegacyAndMissingAnchorsNeverMintReplacement() {
        for (String kid : Arrays.asList(null, "", "bad.kid", "x".repeat(33))) assertThatThrownBy(() -> new ReplayAnchor(1, 1, kid)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReplayAnchor(1, 2, "K1")).isInstanceOf(IllegalArgumentException.class);
        assertThat(authority().issueOrReplay(request(Purpose.PLAYBACK, EXPIRY, new ReplayAnchor(micros(NOW.plusSeconds(1)), micros(NOW.plusSeconds(1)), "K1"))))
                .isInstanceOf(MediaAuthorizationCredentialResult.Malformed.class);
    }

    @Test void replayCannotSwitchRecordedKidOrExtendExpiry() {
        var first = issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null));
        assertThat(authority().issueOrReplay(request(Purpose.PLAYBACK, EXPIRY.plusSeconds(1), first.replayAnchor())))
                .isInstanceOf(MediaAuthorizationCredentialResult.Malformed.class);
        assertThat(authority().issueOrReplay(request(Purpose.PLAYBACK, EXPIRY, new ReplayAnchor(micros(NOW), micros(NOW), "removed"))))
                .isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
        var sub = request(Purpose.PLAYBACK, EXPIRY.minusSeconds(1), first.replayAnchor());
        assertThat(issued(authority(), sub).expiresAt()).isEqualTo(EXPIRY.minusSeconds(1));
    }

    @Test void authorizedFactsRequireFullWindowAndValidMetadata() {
        for (boolean evidenceId : List.of(false, true)) assertThatThrownBy(() -> new AuthorizedMediaFacts(evidenceId ? null : id(3), evidenceId ? id(4) : null,
                "a".repeat(64), 16, "audio/wav", 3600, 0, 3_600_000_000_000L)).isInstanceOf(IllegalArgumentException.class);
        for (long size : List.of(0L, -1L)) assertThatThrownBy(() -> new AuthorizedMediaFacts(id(3), id(4), "a".repeat(64), size, "audio/wav", 3600, 0, 3_600_000_000_000L)).isInstanceOf(IllegalArgumentException.class);
        for (String mime : Arrays.asList(null, "", " \t")) assertThatThrownBy(() -> new AuthorizedMediaFacts(id(3), id(4), "a".repeat(64), 16, mime, 3600, 0, 3_600_000_000_000L)).isInstanceOf(IllegalArgumentException.class);
        for (int duration : List.of(0, -1)) assertThatThrownBy(() -> new AuthorizedMediaFacts(id(3), id(4), "a".repeat(64), 16, "audio/wav", duration, 0, 3600)).isInstanceOf(IllegalArgumentException.class);
        for (long start : List.of(-1L, 1L)) assertThatThrownBy(() -> new AuthorizedMediaFacts(id(3), id(4), "a".repeat(64), 16, "audio/wav", 3600, start, 3_600_000_000_000L)).isInstanceOf(IllegalArgumentException.class);
        for (long end : List.of(0L, 1L, -1L)) assertThatThrownBy(() -> new AuthorizedMediaFacts(id(3), id(4), "a".repeat(64), 16, "audio/wav", 3600, 0, end)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthorizedMediaFacts(id(3), id(4), "a".repeat(64), 16, "audio/wav", Integer.MAX_VALUE, 0, Long.MAX_VALUE)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new AuthorizedMediaFacts(id(3), id(4), "a".repeat(64), 16, "audio/wav", Integer.MAX_VALUE, 0,
                Math.multiplyExact((long) Integer.MAX_VALUE, 1_000_000_000L)).windowEndNanos()).isEqualTo(2_147_483_647_000_000_000L);
        for (String hash : Arrays.asList(null, "A".repeat(64), "a")) assertThatThrownBy(() -> new AuthorizedMediaFacts(id(3), id(4), hash, 16, "audio/wav", 3600, 0, 3_600_000_000_000L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void issuanceUsesOnlyWitnessExpiryBound() {
        assertThat(issued(authority(), request(Purpose.PLAYBACK, EXPIRY, null)).expiresAt()).isEqualTo(EXPIRY);
        for (Instant expiry : List.of(EXPIRY.minusNanos(1000), EXPIRY.plusNanos(1000), NOW))
            assertThat(authority().issueOrReplay(request(Purpose.PLAYBACK, expiry, null))).isInstanceOf(MediaAuthorizationCredentialResult.Malformed.class);
    }

    @Test void legacyP2IssueRemainsUnavailableWithValidKeys() {
        assertThat(authority().issue(new MediaCredentialRequest(id(1), id(2), 7, 11, EXPIRY))).isInstanceOf(ShareMediaCredentialProvider.Unavailable.class);
        assertThat(authority().issue(null)).isInstanceOf(ShareMediaCredentialProvider.Unavailable.class);
        ObjectProvider<MediaPlaybackSigningKeyProvider> keys=mock(ObjectProvider.class);
        assertThat(new MediaPlaybackCredentialAuthority(keys,Clock.fixed(NOW,ZoneOffset.UTC)).issue(null)).isInstanceOf(ShareMediaCredentialProvider.Unavailable.class);
        verifyNoInteractions(keys);
    }

    @Test void carriersResultsKeysAndFailuresAreRedacted() {
        var request = request(Purpose.PLAYBACK, EXPIRY, null); var issued = issued(authority(), request);
        var mapper = JsonMapper.builder().build();
        for (Object carrier : List.of(request, request.mediaFacts(), issued, issued.replayAnchor(), entry("K1", Role.ACTIVE), keys().currentKeys(),
                new MediaAuthorizationCredentialResult.Malformed(), new MediaAuthorizationCredentialResult.Unavailable())) {
            for (String text : List.of(carrier.toString(), mapper.writeValueAsString(carrier)))
                assertThat(text).doesNotContain(issued.credential(), issued.credential().split("\\.")[2], issued.credential().split("\\.")[3], HexFormat.of().formatHex(KEY), "a".repeat(64));
        }
    }

    @Test void purposeEventAndRequestIdentityValidationFailClosed() {
        var original = request(Purpose.PLAYBACK, EXPIRY, null);
        for (String field : List.of("grantId", "admissionId", "facts", "purpose", "unknownPurpose", "eventId", "blankEventId", "fingerprint",
                "invalidFingerprint", "uppercaseFingerprint", "codeGeneration", "revocationGeneration", "grantExpiry", "admissionExpiry", "sessionExpiry", "credentialExpiry"))
            assertThatThrownBy(() -> new MediaAuthorizationCredentialRequest(field.equals("grantId") ? null : id(1), field.equals("admissionId") ? null : id(2),
                    field.equals("purpose") ? null : field.equals("unknownPurpose") ? "UNKNOWN" : "PLAYBACK", "PLAYBACK_AUTHORIZED", "a".repeat(64),
                    field.equals("codeGeneration") ? -1 : 11, field.equals("revocationGeneration") ? -1 : 7,
                    field.equals("grantExpiry") ? null : EXPIRY, field.equals("admissionExpiry") ? null : EXPIRY,
                    field.equals("sessionExpiry") ? null : EXPIRY, field.equals("credentialExpiry") ? null : EXPIRY,
                    field.equals("fingerprint") ? null : field.equals("invalidFingerprint") ? "invalid" : field.equals("uppercaseFingerprint") ? "A".repeat(64) : "a".repeat(64),
                    field.equals("eventId") ? null : field.equals("blankEventId") ? " \t" : "event", field.equals("facts") ? null : original.mediaFacts(), null))
                    .as("invalid request field %s", field).isInstanceOf(IllegalArgumentException.class);
        for (String event : Arrays.asList(null, "", "SEEK_AUTHORIZED")) assertThatThrownBy(() -> new MediaAuthorizationCredentialRequest(id(1), id(2), "PLAYBACK", event,
                "a".repeat(64), 11, 7, EXPIRY, EXPIRY, EXPIRY, EXPIRY, "a".repeat(64), "event", original.mediaFacts(), null)).isInstanceOf(IllegalArgumentException.class);
        for (String digest : Arrays.asList(null, "", "A".repeat(64))) assertThatThrownBy(() -> new MediaAuthorizationCredentialRequest(id(1), id(2), "PLAYBACK", "PLAYBACK_AUTHORIZED",
                digest, 11, 7, EXPIRY, EXPIRY, EXPIRY, EXPIRY, "a".repeat(64), "event", original.mediaFacts(), null)).isInstanceOf(IllegalArgumentException.class);
    }

    private void golden(Purpose purpose, String purposeHex, int length, int tokenLength, String expectedMac) throws Exception {
        var issued = issued(authority(), request(purpose, EXPIRY, null)); String[] parts = issued.credential().split("\\.");
        assertThat(HexFormat.of().formatHex(payload(issued.credential()))).isEqualTo(PREFIX + purposeHex + SUFFIX);
        assertThat(payload(issued.credential())).hasSize(length); assertThat(issued.credential()).hasSize(tokenLength);
        assertThat(HexFormat.of().formatHex(Base64.getUrlDecoder().decode(parts[3]))).isEqualTo(expectedMac);
        var verified = (MediaCredentialVerifier.Result.Verified) authority().verify(new OpaquePlaybackCredential(issued.credential()));
        var expected = new VerifiedMediaClaim("onecall:media-playback:v1", id(1), id(2), id(3), id(4), 7, 11, purpose, NOW, NOW, EXPIRY,
                3600, new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(3600)));
        assertThat(verified.claim()).isEqualTo(expected);
        var shape = MediaPlaybackService.class.getDeclaredMethod("validShape", VerifiedMediaClaim.class);
        shape.setAccessible(true); assertThat(shape.invoke(null, verified.claim())).isEqualTo(true);
    }
    private static UUID id(int suffix) { return new UUID(0, suffix); }
    private static long micros(Instant time) { return Math.addExact(Math.multiplyExact(time.getEpochSecond(), 1_000_000L), time.getNano() / 1_000); }
    private static Entry entry(String kid, Role role) { return new Entry(kid, KEY, role); }
    private static MediaPlaybackSigningKeyProvider snapshot(Entry active, List<Entry> overlap) { return () -> new MediaPlaybackSigningKeyProvider.Result.Available(active, overlap); }
    private static MediaPlaybackSigningKeyProvider keys() { return DeterministicShareCommandProviders.mediaPlaybackSigningKeys(); }
    private static MediaPlaybackCredentialAuthority authority() { return authority(Clock.fixed(NOW, ZoneOffset.UTC), keys()); }
    private static MediaPlaybackCredentialAuthority authority(Clock clock, MediaPlaybackSigningKeyProvider... keys) {
        var beans = new DefaultListableBeanFactory();
        for (int index = 0; index < keys.length; index++) beans.registerSingleton("key" + index, keys[index]);
        return new MediaPlaybackCredentialAuthority(beans.getBeanProvider(MediaPlaybackSigningKeyProvider.class), clock);
    }
    private static MediaAuthorizationCredentialRequest request(Purpose purpose, Instant expiry, ReplayAnchor anchor) {
        String event = switch (purpose) { case PLAYBACK -> "PLAYBACK_AUTHORIZED"; case SEEK -> "SEEK_AUTHORIZED"; case RENEW -> "RENEWED"; };
        return new MediaAuthorizationCredentialRequest(id(1), id(2), purpose.name(), event, "a".repeat(64), 11, 7, EXPIRY, EXPIRY, EXPIRY,
                expiry, "a".repeat(64), "event", DeterministicShareCommandProviders.mediaFacts(id(3)), anchor);
    }
    private static MediaAuthorizationCredentialResult.Issued issued(MediaPlaybackCredentialAuthority authority, MediaAuthorizationCredentialRequest request) {
        var result=authority.issueOrReplay(request);
        assertThat(result).isInstanceOf(MediaAuthorizationCredentialResult.Issued.class);
        return (MediaAuthorizationCredentialResult.Issued) result;
    }
    private void invalid(String token) { assertThat(authority().verify(new OpaquePlaybackCredential(token))).isInstanceOf(MediaCredentialVerifier.Result.Invalid.class); }
    private void unavailable(MediaPlaybackCredentialAuthority authority) {
        for (String token : List.of("pa20-positive-playback-token", "ocmp1", "ocmp1.bad.!.*"))
            assertThat(authority.verify(new OpaquePlaybackCredential(token))).isInstanceOf(MediaCredentialVerifier.Result.Unavailable.class);
        var opaque = mock(OpaquePlaybackCredential.class);
        assertThat(authority.verify(opaque)).isInstanceOf(MediaCredentialVerifier.Result.Unavailable.class); verify(opaque, never()).value();
        var state=mock(PlaybackStateReader.class);
        var evidence=mock(com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.class);
        var reader=mock(ControlledMediaReader.class);
        var service=new MediaPlaybackService(authority,state,evidence,reader,Clock.fixed(NOW,ZoneOffset.UTC));
        assertThat(service.authorizeAndOpen(new OpaquePlaybackCredential("pa20-positive-playback-token"),Purpose.PLAYBACK))
                .isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
        verifyNoInteractions(state,evidence,reader);
    }
    private static byte[] payload(String token) { return Base64.getUrlDecoder().decode(token.split("\\.")[2]); }
    private static String b64(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static String sign(byte[] bytes, String kid) throws Exception {
        var mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
        mac.update(("onecall-media-playback\0v1\0" + kid + "\0").getBytes(StandardCharsets.US_ASCII));
        return "ocmp1." + kid + "." + b64(bytes) + "." + b64(mac.doFinal(bytes));
    }
}
