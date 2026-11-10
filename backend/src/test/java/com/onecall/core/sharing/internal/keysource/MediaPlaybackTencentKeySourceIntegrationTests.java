package com.onecall.core.sharing.internal.keysource;

import static com.onecall.core.sharing.internal.keysource.MediaPlaybackKeyBundleParserTests.*;
import static com.onecall.core.sharing.internal.keysource.TencentSsmSecretStringClientTests.response;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.evidence.mediaread.ResolvedMediaObject;
import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.*;
import com.onecall.core.sharing.internal.credential.MediaPlaybackCredentialAuthority;
import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider;
import com.onecall.core.sharing.internal.keysource.TencentSsmMediaPlaybackSigningKeyProvider.Settings;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.ssm.v20190923.SsmClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
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
class MediaPlaybackTencentKeySourceIntegrationTests {
    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");
    private static final Instant EXPIRY = NOW.plusSeconds(60);
    @TempDir Path directory;
    private CapturedOutput output;
    @BeforeEach void captureOutput(CapturedOutput output) { this.output = output; }

    @Test void tencentBundleIssuesActiveForEveryPurpose() throws Exception {
        var source = source("version-A", bundle(entry("Z", "VERIFY_ONLY", 3), entry("K2", "ACTIVE", 2), entry("K1", "VERIFY_ONLY", 1)));
        var authority = authority(source, NOW);
        for (Purpose purpose : Purpose.values()) {
            var issued = issued(authority, purpose, null);
            assertThat(issued.credential().startsWith("ocmp1.K2.")).isTrue();
            assertThat(issued.replayAnchor().kid()).isEqualTo("K2");
            assertThat(issued.replayAnchor().issuedAtMicros()).isEqualTo(NOW.getEpochSecond() * 1_000_000);
            var verified = authority.verify(new OpaquePlaybackCredential(issued.credential()));
            assertThat(verified).isInstanceOf(MediaCredentialVerifier.Result.Verified.class);
            assertThat(((MediaCredentialVerifier.Result.Verified) verified).claim()).isEqualTo(new VerifiedMediaClaim(
                    "onecall:media-playback:v1", id(1), id(2), id(3), id(4), 7, 11, purpose, NOW, NOW, EXPIRY, 10,
                    new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(10))));
            assertThat(issued.expiresAt()).isEqualTo(EXPIRY);
        }
    }

    @Test void explicitVersionRolloutVerifiesOldAndNewKeys() throws Exception {
        var before = authority(source("version-A", bundle(entry("K1", "ACTIVE", 1))), NOW);
        var after = authority(source("version-B", overlap(false)), NOW);
        for (Purpose purpose : Purpose.values()) {
            var old = issued(before, purpose, null); var current = issued(after, purpose, null);
            verified(after, old); verified(after, current);
            assertThat(current.replayAnchor().kid()).isEqualTo("K2");
            assertThat(old.credential().equals(current.credential())).isFalse();
        }
    }

    @Test void overlapReplaysExactCredentialForEveryPurpose() throws Exception {
        var before = authority(source("version-A", bundle(entry("K1", "ACTIVE", 1))), NOW);
        var after = authority(source("version-B", overlap(false)), NOW.plusSeconds(1));
        for (Purpose purpose : Purpose.values()) {
            var old = issued(before, purpose, null); var replay = issued(after, purpose, old.replayAnchor());
            assertThat(replay.credential().equals(old.credential())).isTrue();
            assertThat(replay.replayAnchor()).isEqualTo(old.replayAnchor());
            assertThat(replay.expiresAt()).isEqualTo(old.expiresAt()); verified(after, replay);
        }
    }

    @Test void removedRecordedKidNeverMintsReplacement() throws Exception {
        var before = authority(source("version-A", bundle(entry("K1", "ACTIVE", 1))), NOW);
        var after = authority(source("version-C", bundle(entry("K2", "ACTIVE", 2))), NOW);
        for (Purpose purpose : Purpose.values()) {
            var old = issued(before, purpose, null);
            assertThat(after.verify(new OpaquePlaybackCredential(old.credential()))).isInstanceOf(MediaCredentialVerifier.Result.Invalid.class);
            assertThat(after.issueOrReplay(request(purpose, old.replayAnchor()))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
            assertThat(issued(after, purpose, null).replayAnchor().kid()).isEqualTo("K2");
            assertThat(old.replayAnchor().kid()).isEqualTo("K1");
        }
    }

    @Test void unavailableSourcePrecedesOpaqueTokenAccess() throws Exception {
        var before = authority(source("version-A", bundle(entry("K1", "ACTIVE", 1))), NOW);
        for (String failure : failures()) {
            var authority = authority(failing(failure), NOW);
            var opaque = mock(OpaquePlaybackCredential.class);
            assertThat(authority.verify(opaque)).isInstanceOf(MediaCredentialVerifier.Result.Unavailable.class);
            verifyNoInteractions(opaque);
            for (Purpose purpose : Purpose.values()) {
                var old = issued(before, purpose, null);
                assertThat(authority.issueOrReplay(request(purpose, null))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
                assertThat(authority.issueOrReplay(request(purpose, old.replayAnchor()))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
            }
        }
    }

    @Test void unavailableSourcePreventsStateEvidenceAndOpen() throws Exception {
        for (String failure : failures()) {
            var authority = authority(failing(failure), NOW);
            var state = mock(PlaybackStateReader.class); var evidence = mock(EvidenceMediaObjectReader.class); var reader = mock(ControlledMediaReader.class);
            var service = new MediaPlaybackService(authority, state, evidence, reader, Clock.fixed(NOW, ZoneOffset.UTC));
            for (Purpose purpose : Purpose.values()) {
                var good = issued(authority(source("version-A", bundle(entry("K1", "ACTIVE", 1))), NOW), purpose, null);
                assertThat(service.authorizeAndOpen(new OpaquePlaybackCredential(good.credential()), purpose)).isInstanceOf(MediaPlaybackService.Result.Unavailable.class);
            }
            verifyNoInteractions(state, evidence, reader);
        }
    }

    @Test void tencentClaimsStillRequireCurrentState() throws Exception {
        var authority = authority(source("version-A", bundle(entry("K1", "ACTIVE", 1))), NOW);
        for (Purpose purpose : Purpose.values()) {
            var credential = new OpaquePlaybackCredential(issued(authority, purpose, null).credential());
            for (String denial : List.of("revoked", "expired", "stale-generation", "session", "member", "risk")) {
                var reason = switch (denial) {
                    case "revoked" -> MediaPlaybackService.Reason.REVOKED;
                    case "expired", "session" -> MediaPlaybackService.Reason.EXPIRED;
                    case "stale-generation" -> MediaPlaybackService.Reason.STALE_GENERATION;
                    default -> MediaPlaybackService.Reason.CURRENT_STATE_DENIED;
                };
                var state = mock(PlaybackStateReader.class); var evidence = mock(EvidenceMediaObjectReader.class); var reader = mock(ControlledMediaReader.class);
                when(state.check(any())).thenReturn(new PlaybackStateReader.Result.Denied(reason));
                var service = new MediaPlaybackService(authority, state, evidence, reader, Clock.fixed(NOW, ZoneOffset.UTC));
                assertThat(service.authorizeAndOpen(credential, purpose)).isEqualTo(new MediaPlaybackService.Result.Denied(reason));
                verify(state).check(any()); verifyNoInteractions(evidence, reader);
            }
            for (String mismatch : List.of("object", "duration", "window")) {
                var state = mock(PlaybackStateReader.class); var evidence = mock(EvidenceMediaObjectReader.class); var reader = mock(ControlledMediaReader.class);
                when(state.check(any())).thenReturn(new PlaybackStateReader.Result.Allowed(mismatch.equals("window") ? EXPIRY : NOW));
                when(evidence.resolve(id(3))).thenReturn(new EvidenceMediaObjectReader.Result.Found(
                        new ResolvedMediaObject(mismatch.equals("object") ? id(5) : id(4), "a".repeat(64), 123, "audio/wav", mismatch.equals("duration") ? 9 : 10)));
                var service = new MediaPlaybackService(authority, state, evidence, reader, Clock.fixed(NOW, ZoneOffset.UTC));
                assertThat(service.authorizeAndOpen(credential, purpose)).isNotInstanceOf(MediaPlaybackService.Result.Opened.class);
                verifyNoInteractions(reader);
            }
        }
    }

    @Test void equalMaterialNeverAliasesRecordedKid() throws Exception {
        var before = authority(source("version-A", bundle(entry("K1", "ACTIVE", 1))), NOW);
        var after = authority(source("version-B", overlap(true)), NOW);
        var removed = authority(source("version-C", bundle(entry("K2", "ACTIVE", 1))), NOW);
        for (Purpose purpose : Purpose.values()) {
            var old = issued(before, purpose, null); verified(after, old);
            assertThat(after.verify(new OpaquePlaybackCredential(old.credential().replace(".K1.", ".K2.")))).isInstanceOf(MediaCredentialVerifier.Result.Invalid.class);
            assertThat(issued(after, purpose, old.replayAnchor()).credential().equals(old.credential())).isTrue();
            assertThat(issued(after, purpose, null).replayAnchor().kid()).isEqualTo("K2");
            assertThat(removed.issueOrReplay(request(purpose, old.replayAnchor()))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
            assertThat(removed.verify(new OpaquePlaybackCredential(old.credential()))).isInstanceOf(MediaCredentialVerifier.Result.Invalid.class);
        }
    }

    @Test void transportAndParserFailuresHaveNoFallback() throws Exception {
        Path decoy = Files.writeString(directory.resolve("decoy"), bundle(entry("K9", "ACTIVE", 9)));
        Settings settings = settings("version-A"); SsmClient sdk = mock(SsmClient.class);
        when(sdk.GetSecretValue(any())).thenReturn(response(settings, bundle(entry("K1", "ACTIVE", 1))))
                .thenThrow(new TencentCloudSDKException(UUID.randomUUID().toString())).thenReturn(response(settings, "{}"))
                .thenReturn(response(settings, bundle(entry("K1", "ACTIVE", 1))));
        var authority = authority(provider(settings, sdk), NOW);
        try (var files = mockConstruction(FileMediaPlaybackSigningKeyProvider.class)) {
            var old = issued(authority, Purpose.PLAYBACK, null);
            assertThat(authority.verify(new OpaquePlaybackCredential(old.credential()))).isInstanceOf(MediaCredentialVerifier.Result.Unavailable.class);
            assertThat(authority.issueOrReplay(request(Purpose.PLAYBACK, old.replayAnchor()))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
            verified(authority, old); assertThat(files.constructed()).isEmpty();
        }
        verify(sdk, times(4)).GetSecretValue(any()); verifyNoMoreInteractions(sdk);
        assertThat(Files.exists(decoy)).isTrue();
    }

    @Test void sdkToAuthorityFlowNeverLeaksCanaries() throws Exception {
        String canary = "synthetic-" + UUID.randomUUID();
        Settings settings = Settings.create("test-region", canary, canary, null, null); SsmClient sdk = mock(SsmClient.class);
        var response = response(settings, bundle(entry("K1", "ACTIVE", 171))); response.setRequestId(canary);
        when(sdk.GetSecretValue(any())).thenReturn(response);
        var client = new TencentSsmSecretStringClient(settings, configured -> sdk);
        var provider = new TencentSsmMediaPlaybackSigningKeyProvider(settings, client); var authority = authority(provider, NOW);
        var mapper = JsonMapper.builder().build(); String rendered = "";
        for (Purpose purpose : Purpose.values()) {
            var issued = issued(authority, purpose, null); var replay = issued(authority, purpose, issued.replayAnchor());
            var opaque = new OpaquePlaybackCredential(issued.credential());
            var verified = (MediaCredentialVerifier.Result.Verified) authority.verify(opaque);
            for (Object carrier : List.of(settings, provider, client, provider.currentKeys(), request(purpose, null), issued, replay,
                    issued.replayAnchor(), opaque, verified, verified.claim())) rendered += carrier + mapper.writeValueAsString(carrier);
            for (String secret : List.of(issued.credential(), issued.credential().split("\\.")[2], issued.credential().split("\\.")[3]))
                assertThat(rendered.contains(secret)).isFalse();
        }
        when(sdk.GetSecretValue(any())).thenThrow(new TencentCloudSDKException(canary, new IOException(canary)));
        assertThat(authority.verify(new OpaquePlaybackCredential(canary))).isInstanceOf(MediaCredentialVerifier.Result.Unavailable.class);
        assertThat(authority.issueOrReplay(request(Purpose.PLAYBACK, null))).isInstanceOf(MediaAuthorizationCredentialResult.Unavailable.class);
        rendered += output.getAll();
        for (String secret : List.of(canary, hex(171), Base64.getEncoder().encodeToString(material(171)), Base64.getUrlEncoder().withoutPadding().encodeToString(material(171))))
            assertThat(rendered.contains(secret)).isFalse();
    }

    private static Settings settings(String version) { return Settings.create("test-region", "synthetic-name", version, null, null); }
    private static TencentSsmMediaPlaybackSigningKeyProvider source(String version, String json) throws Exception {
        Settings settings = settings(version); SsmClient sdk = mock(SsmClient.class);
        when(sdk.GetSecretValue(any())).thenAnswer(invocation -> {
            com.tencentcloudapi.ssm.v20190923.models.GetSecretValueRequest request = invocation.getArgument(0);
            assertThat(request.getVersionId().equals(version)).isTrue();
            return response(settings, json);
        });
        return provider(settings, sdk);
    }
    private static TencentSsmMediaPlaybackSigningKeyProvider provider(Settings settings, SsmClient sdk) {
        return new TencentSsmMediaPlaybackSigningKeyProvider(settings, new TencentSsmSecretStringClient(settings, configured -> sdk));
    }
    private static List<String> failures() {
        return List.of("metadata", "factory", "runtime", "AuthFailure", "UnauthorizedOperation", "ResourceNotFound", "SecretDisabled", "SecretDestroyed",
                "RequestLimitExceeded", "InternalError", "TLS", "timeout", "null", "identity", "binary", "empty", "schema", "duplicate", "role", "key", "utf16", "oversized");
    }
    private static TencentSsmMediaPlaybackSigningKeyProvider failing(String category) throws Exception {
        Settings settings = category.equals("metadata") ? null : settings("version-A"); SsmClient sdk = mock(SsmClient.class);
        when(sdk.GetSecretValue(any())).thenAnswer(invocation -> {
            if (category.equals("runtime")) throw new IllegalStateException(new IOException());
            if (category.equals("null")) return null;
            var response = response(settings, switch (category) {
                case "empty" -> " "; case "schema" -> "{}"; case "duplicate" -> bundle(entry("K1", "ACTIVE", 1), entry("K1", "VERIFY_ONLY", 1));
                case "role" -> bundle(entry("K1", "DESTROYED", 1)); case "key" -> bundle(entry("bad.kid", "ACTIVE", 1));
                case "utf16" -> "\ud800"; case "oversized" -> " ".repeat(65_537); case "identity", "binary" -> bundle(entry("K1", "ACTIVE", 1));
                default -> throw new TencentCloudSDKException(UUID.randomUUID().toString(), UUID.randomUUID().toString(), category);
            });
            if (category.equals("identity")) response.setVersionId("other");
            if (category.equals("binary")) response.setSecretBinary("synthetic");
            return response;
        });
        return new TencentSsmMediaPlaybackSigningKeyProvider(settings, new TencentSsmSecretStringClient(settings, configured -> {
            if (category.equals("factory")) throw new IllegalStateException(); return sdk;
        }));
    }
    private static String overlap(boolean equal) { return bundle(entry("K2", "ACTIVE", equal ? 1 : 2), entry("K1", "VERIFY_ONLY", 1)); }
    private static UUID id(int value) { return new UUID(0, value); }
    private static MediaPlaybackCredentialAuthority authority(MediaPlaybackSigningKeyProvider provider, Instant now) {
        var beans = new DefaultListableBeanFactory(); beans.registerSingleton("keys", provider);
        return new MediaPlaybackCredentialAuthority(beans.getBeanProvider(MediaPlaybackSigningKeyProvider.class), Clock.fixed(now, ZoneOffset.UTC));
    }
    private static MediaAuthorizationCredentialRequest request(Purpose purpose, ReplayAnchor anchor) {
        String event = switch (purpose) { case PLAYBACK -> "PLAYBACK_AUTHORIZED"; case SEEK -> "SEEK_AUTHORIZED"; case RENEW -> "RENEWED"; };
        return new MediaAuthorizationCredentialRequest(id(1), id(2), purpose.name(), event, "a".repeat(64), 11, 7,
                EXPIRY, EXPIRY, EXPIRY, EXPIRY, "a".repeat(64), "event", new AuthorizedMediaFacts(id(3), id(4), "a".repeat(64), 123, "audio/wav", 10, 0, 10_000_000_000L), anchor);
    }
    private static MediaAuthorizationCredentialResult.Issued issued(MediaPlaybackCredentialAuthority authority, Purpose purpose, ReplayAnchor anchor) {
        var result = authority.issueOrReplay(request(purpose, anchor)); assertThat(result).isInstanceOf(MediaAuthorizationCredentialResult.Issued.class);
        return (MediaAuthorizationCredentialResult.Issued) result;
    }
    private static void verified(MediaPlaybackCredentialAuthority authority, MediaAuthorizationCredentialResult.Issued issued) {
        assertThat(authority.verify(new OpaquePlaybackCredential(issued.credential()))).isInstanceOf(MediaCredentialVerifier.Result.Verified.class);
    }
}
