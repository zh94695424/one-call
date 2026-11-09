package com.onecall.core.sharing.internal.keysource;

import static com.onecall.core.sharing.internal.keysource.MediaPlaybackKeyBundleParserTests.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.evidence.mediaread.ResolvedMediaObject;
import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.*;
import com.onecall.core.sharing.internal.credential.MediaPlaybackCredentialAuthority;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class MediaPlaybackFileKeySourceIntegrationTests {
    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");
    private static final Instant EXPIRY = NOW.plusSeconds(60);
    @TempDir Path directory;
    private CapturedOutput output;

    @BeforeEach void captureOutput(CapturedOutput output) { this.output = output; }

    @Test void fileBundleIssuesOnlyActiveForEveryPurpose() throws Exception {
        Path path = source(bundle(entry("Z", "VERIFY_ONLY", 3), entry("K2", "ACTIVE", 2), entry("K1", "VERIFY_ONLY", 1)));
        var authority = authority(path, NOW);
        for (Purpose purpose : Purpose.values()) {
            var issued = issued(authority, purpose, null);
            assertThat(issued.credential().startsWith("ocmp1.K2.")).isTrue();
            assertThat(issued.replayAnchor().kid()).isEqualTo("K2");
            var result = authority.verify(new OpaquePlaybackCredential(issued.credential()));
            assertThat(result).isInstanceOf(MediaCredentialVerifier.Result.Verified.class);
            var claim = ((MediaCredentialVerifier.Result.Verified) result).claim();
            assertThat(claim).isEqualTo(new VerifiedMediaClaim("onecall:media-playback:v1", id(1), id(2), id(3), id(4),
                    7, 11, purpose, NOW, NOW, EXPIRY, 10, new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(10))));
            assertThat(issued.expiresAt()).isEqualTo(EXPIRY);
        }
    }

    @Test void rotationOverlapVerifiesOldAndNewCredentials() throws Exception {
        Path path = source(bundle(entry("K1", "ACTIVE", 1)));
        var authority = authority(path, NOW);
        for (Purpose purpose : Purpose.values()) {
            Files.writeString(path, bundle(entry("K1", "ACTIVE", 1)));
            var first = issued(authority, purpose, null);
            Files.writeString(path, overlap(false));
            var second = issued(authority, purpose, null);
            verified(authority, first); verified(authority, second);
            assertThat(second.credential().startsWith("ocmp1.K2.")).isTrue();
            assertThat(second.replayAnchor().kid()).isEqualTo("K2");
            assertThat(second.credential().equals(first.credential())).isFalse();
        }
    }

    @Test void overlapReplaysExactPreviouslyIssuedCredential() throws Exception {
        Path path = source(bundle(entry("K1", "ACTIVE", 1)));
        for (Purpose purpose : Purpose.values()) {
            Files.writeString(path, bundle(entry("K1", "ACTIVE", 1)));
            var first = issued(authority(path, NOW), purpose, null);
            Files.writeString(path, overlap(false));
            var replay = issued(authority(path, NOW.plusSeconds(1)), purpose, first.replayAnchor());
            assertThat(replay.credential().equals(first.credential())).isTrue();
            assertThat(replay.expiresAt()).isEqualTo(first.expiresAt());
            assertThat(replay.replayAnchor().equals(first.replayAnchor())).isTrue();
            assertThat(replay.replayAnchor().kid()).isEqualTo("K1");
            verified(authority(path, NOW.plusSeconds(1)), replay);
        }
    }

    @Test void recordedKeyRemovalNeverMintsReplacement() throws Exception {
        Path path = source(bundle(entry("K1", "ACTIVE", 1)));
        var authority = authority(path, NOW);
        for (Purpose purpose : Purpose.values()) {
            Files.writeString(path, bundle(entry("K1", "ACTIVE", 1)));
            var first = issued(authority, purpose, null);
            var anchor = first.replayAnchor();
            Files.writeString(path, bundle(entry("K2", "ACTIVE", 2)));
            assertThat(authority.verify(new OpaquePlaybackCredential(first.credential()))).isInstanceOf(MediaCredentialVerifier.Result.Invalid.class);
            assertThat(authority.issueOrReplay(request(purpose, anchor))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
            assertThat(issued(authority, purpose, null).replayAnchor().kid()).isEqualTo("K2");
            assertThat(anchor.kid()).isEqualTo("K1");
            assertThat(anchor.issuedAtMicros()).isEqualTo(NOW.getEpochSecond() * 1_000_000);
        }
    }

    @Test void badBundlePrecedesTokenParsingAndDownstreamReads() throws Exception {
        Path path = directory.resolve("source");
        var authority = authority(path, NOW);
        for (String invalid : List.of("missing", "{", "{}", "DISABLED", "DESTROYED", "EXPIRED")) {
            if (!invalid.equals("missing")) Files.writeString(path, invalid.startsWith("{") ? invalid
                    : bundle(entry("K1", "ACTIVE", 1), entry("K2", invalid, 2)));
            var opaque = mock(OpaquePlaybackCredential.class);
            assertThat(authority.verify(opaque)).isInstanceOf(MediaCredentialVerifier.Result.Unavailable.class);
            verifyNoInteractions(opaque);
            var state = mock(PlaybackStateReader.class);
            var evidence = mock(EvidenceMediaObjectReader.class);
            var reader = mock(ControlledMediaReader.class);
            var service = new MediaPlaybackService(authority, state, evidence, reader, Clock.fixed(NOW, ZoneOffset.UTC));
            for (Purpose purpose : Purpose.values()) {
                assertThat(authority.issueOrReplay(request(purpose, null))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
                assertThat(service.authorizeAndOpen(new OpaquePlaybackCredential("ocmp1.bad.!.*"), purpose))
                        .isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
            }
            verifyNoInteractions(state, evidence, reader);
        }
    }

    @Test void fileBackedClaimsStillRequireCurrentState() throws Exception {
        Path path = source(bundle(entry("K1", "ACTIVE", 1)));
        var authority = authority(path, NOW);
        for (Purpose purpose : Purpose.values()) {
            var credential = new OpaquePlaybackCredential(issued(authority, purpose, null).credential());
            for (var reason : List.of(MediaPlaybackService.Reason.REVOKED, MediaPlaybackService.Reason.STALE_GENERATION,
                    MediaPlaybackService.Reason.EXPIRED, MediaPlaybackService.Reason.SCOPE_MISMATCH,
                    MediaPlaybackService.Reason.CURRENT_STATE_DENIED, MediaPlaybackService.Reason.ADMISSION_MISMATCH)) {
                var state = mock(PlaybackStateReader.class);
                var evidence = mock(EvidenceMediaObjectReader.class);
                var reader = mock(ControlledMediaReader.class);
                when(state.check(any())).thenReturn(new PlaybackStateReader.Result.Denied(reason));
                var service = new MediaPlaybackService(authority, state, evidence, reader, Clock.fixed(NOW, ZoneOffset.UTC));
                assertThat(service.authorizeAndOpen(credential, purpose)).isEqualTo(new MediaPlaybackService.Result.Denied(reason));
                verify(state).check(any()); verifyNoInteractions(evidence, reader);
            }
            var state = mock(PlaybackStateReader.class);
            var evidence = mock(EvidenceMediaObjectReader.class);
            var reader = mock(ControlledMediaReader.class);
            when(state.check(any())).thenReturn(new PlaybackStateReader.Result.Allowed(NOW));
            when(evidence.resolve(id(3))).thenReturn(new EvidenceMediaObjectReader.Result.Found(
                    new ResolvedMediaObject(id(5), "a".repeat(64), 123, "audio/wav", 10)));
            var service = new MediaPlaybackService(authority, state, evidence, reader, Clock.fixed(NOW, ZoneOffset.UTC));
            assertThat(service.authorizeAndOpen(credential, purpose)).isEqualTo(new MediaPlaybackService.Result.Denied(MediaPlaybackService.Reason.SCOPE_MISMATCH));
            verifyNoInteractions(reader);
            when(evidence.resolve(id(3))).thenReturn(new EvidenceMediaObjectReader.Result.Found(
                    new ResolvedMediaObject(id(4), "a".repeat(64), 123, "audio/wav", 10)));
            when(reader.open(any())).thenAnswer(invocation -> new ControlledMediaReader.Result.Opened(
                    new ControlledMediaReader.ControlledReadHandle(invocation.getArgument(0))));
            assertThat(service.authorizeAndOpen(credential, purpose)).isInstanceOf(MediaPlaybackService.Result.Opened.class);
            verify(reader).open(any());
        }
    }

    @Test void bundleFailuresAndCredentialsNeverLeak() throws Exception {
        String canary = "bundle-error-" + UUID.randomUUID();
        Path path = Files.writeString(directory.resolve(canary), bundle(entry("K1", "ACTIVE", 171)));
        var provider = new FileMediaPlaybackSigningKeyProvider(path.toString());
        var authority = authority(provider, NOW);
        var first = issued(authority, Purpose.PLAYBACK, null);
        verified(authority, first);
        var replay = issued(authority, Purpose.PLAYBACK, first.replayAnchor());
        assertThat(replay.credential().equals(first.credential())).isTrue();
        var opaque = new OpaquePlaybackCredential(first.credential());
        var verified = (MediaCredentialVerifier.Result.Verified) authority.verify(opaque);
        var mapper = JsonMapper.builder().build();
        StringBuilder rendered = new StringBuilder();
        for (Object carrier : List.of(provider, new MediaPlaybackKeyBundleParser(), provider.currentKeys(),
                ((MediaPlaybackSigningKeyProvider.Result.Available) provider.currentKeys()).active(), request(Purpose.PLAYBACK, null),
                first, replay, first.replayAnchor(), opaque, verified, verified.claim()))
            rendered.append(carrier).append(mapper.writeValueAsString(carrier));
        Files.writeString(path, "{\"" + canary + "\":\"" + hex(171) + "\"}");
        assertThat(authority.verify(opaque)).isInstanceOf(MediaCredentialVerifier.Result.Unavailable.class);
        Files.delete(path);
        assertThat(authority.issueOrReplay(request(Purpose.PLAYBACK, null))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
        var failing = spy(provider);
        doThrow(new IOException(canary)).when(failing).readBoundedSource(path);
        assertThat(authority(failing, NOW).verify(opaque)).isInstanceOf(MediaCredentialVerifier.Result.Unavailable.class);
        rendered.append(output.getAll());
        for (String secret : List.of(canary, path.toString(), hex(171), Base64.getEncoder().encodeToString(material(171)),
                Base64.getUrlEncoder().withoutPadding().encodeToString(material(171)), first.credential(),
                first.credential().split("\\.")[2], first.credential().split("\\.")[3]))
            assertThat(rendered.toString().contains(secret)).isFalse();
    }

    @Test void equalMaterialAcrossDistinctKidsDoesNotChangeRecordedKid() throws Exception {
        Path path = source(bundle(entry("K1", "ACTIVE", 1)));
        var authority = authority(path, NOW);
        for (Purpose purpose : Purpose.values()) {
            Files.writeString(path, bundle(entry("K1", "ACTIVE", 1)));
            var first = issued(authority, purpose, null);
            Files.writeString(path, overlap(true));
            verified(authority, first);
            assertThat(authority.verify(new OpaquePlaybackCredential(first.credential().replace(".K1.", ".K2."))))
                    .isInstanceOf(MediaCredentialVerifier.Result.Invalid.class);
            var replay = issued(authority, purpose, first.replayAnchor());
            assertThat(replay.credential().equals(first.credential())).isTrue();
            assertThat(replay.replayAnchor().kid()).isEqualTo("K1");
            assertThat(issued(authority, purpose, null).replayAnchor().kid()).isEqualTo("K2");
            Files.writeString(path, bundle(entry("K2", "ACTIVE", 1)));
            assertThat(authority.issueOrReplay(request(purpose, first.replayAnchor()))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
            assertThat(authority.verify(new OpaquePlaybackCredential(first.credential()))).isInstanceOf(MediaCredentialVerifier.Result.Invalid.class);
        }
    }

    private Path source(String json) throws IOException { return Files.writeString(directory.resolve("source"), json); }
    private static String overlap(boolean equal) { return bundle(entry("K2", "ACTIVE", equal ? 1 : 2), entry("K1", "VERIFY_ONLY", 1)); }
    private static UUID id(int value) { return new UUID(0, value); }
    private static MediaPlaybackCredentialAuthority authority(Path path, Instant now) {
        return authority(new FileMediaPlaybackSigningKeyProvider(path.toString()), now);
    }
    private static MediaPlaybackCredentialAuthority authority(MediaPlaybackSigningKeyProvider provider, Instant now) {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("keys", provider);
        return new MediaPlaybackCredentialAuthority(beans.getBeanProvider(MediaPlaybackSigningKeyProvider.class), Clock.fixed(now, ZoneOffset.UTC));
    }
    private static MediaAuthorizationCredentialRequest request(Purpose purpose, ReplayAnchor anchor) {
        String event = switch (purpose) { case PLAYBACK -> "PLAYBACK_AUTHORIZED"; case SEEK -> "SEEK_AUTHORIZED"; case RENEW -> "RENEWED"; };
        return new MediaAuthorizationCredentialRequest(id(1), id(2), purpose.name(), event, "a".repeat(64), 11, 7,
                EXPIRY, EXPIRY, EXPIRY, EXPIRY, "a".repeat(64), "event",
                new AuthorizedMediaFacts(id(3), id(4), "a".repeat(64), 123, "audio/wav", 10, 0, 10_000_000_000L), anchor);
    }
    private static MediaAuthorizationCredentialResult.Issued issued(MediaPlaybackCredentialAuthority authority, Purpose purpose, ReplayAnchor anchor) {
        var result = authority.issueOrReplay(request(purpose, anchor));
        assertThat(result).isInstanceOf(MediaAuthorizationCredentialResult.Issued.class);
        return (MediaAuthorizationCredentialResult.Issued) result;
    }
    private static void verified(MediaPlaybackCredentialAuthority authority, MediaAuthorizationCredentialResult.Issued issued) {
        assertThat(authority.verify(new OpaquePlaybackCredential(issued.credential()))).isInstanceOf(MediaCredentialVerifier.Result.Verified.class);
    }
}
