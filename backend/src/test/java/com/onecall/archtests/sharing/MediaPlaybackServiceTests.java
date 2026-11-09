package com.onecall.archtests.sharing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.onecall.core.sharing.internal.playback.MediaPlaybackService.Reason.*;

import com.onecall.core.evidence.mediaread.*;
import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository;
import com.onecall.core.sharing.internal.playback.*;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.time.*;
import java.util.*;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class MediaPlaybackServiceTests {
    static final Instant NOW = Instant.parse("2026-01-01T00:00:10Z");
    private CapturedOutput output;

    @BeforeEach void captureOutput(CapturedOutput output) { this.output = output; }

    @Test void validPlaybackResolvesVerifiedEvidenceAndOpensExactlyBoundedHandle() {
        Fixture fixture = new Fixture();
        var opened = (MediaPlaybackService.Result.Opened) fixture.run();
        assertThat(opened.handle().scope()).isEqualTo(fixture.scope());
        assertThat(fixture.calls).containsExactly("verify", "state", "evidence", "open");
    }

    @Test void validInDurationSubWindowPassesShapeAndTypedOpen() {
        Fixture fixture = new Fixture();
        fixture.claim = replace(fixture.claim,"window",new VerifiedMediaClaim.PlaybackWindow(Duration.ofSeconds(1),Duration.ofSeconds(8)));
        assertThat(fixture.run()).isInstanceOf(MediaPlaybackService.Result.Opened.class);
        assertThat(fixture.scope().window()).isEqualTo(fixture.claim.window());
        assertThat(fixture.calls).containsExactly("verify","state","evidence","open");
    }

    @Test void seekAndRenewRequireExactPurposeAndNeverIssueOrExtendCredentials() {
        for (Purpose purpose : Purpose.values()) {
            Fixture fixture = new Fixture(); fixture.claim = purpose(fixture.claim, purpose);
            assertThat(fixture.service().authorizeAndOpen(fixture.credential, purpose)).isInstanceOf(MediaPlaybackService.Result.Opened.class);
            assertThat(fixture.scope().expiresAt()).isEqualTo(NOW.plusSeconds(20));
            for (Purpose other : Purpose.values()) if (other != purpose) {
                fixture.calls.clear();
                assertThat(fixture.service().authorizeAndOpen(fixture.credential, other)).isEqualTo(denied(WRONG_PURPOSE));
                assertThat(fixture.calls).containsExactly("verify");
            }
        }
    }

    @Test void malformedOpaqueCredentialStopsBeforeVerifier() {
        Fixture fixture = new Fixture();
        assertThat(fixture.service().authorizeAndOpen(null, Purpose.PLAYBACK)).isEqualTo(denied(MALFORMED_CREDENTIAL));
        for (String value : Arrays.asList(null, "", " \n", "a\0b", "\ud800", "a".repeat(2049)))
            assertThat(fixture.service().authorizeAndOpen(new OpaquePlaybackCredential(value), Purpose.PLAYBACK)).isEqualTo(denied(MALFORMED_CREDENTIAL));
        assertThat(fixture.calls).isEmpty();
        fixture.credential = new OpaquePlaybackCredential(" exact opaque 🐈 ");
        assertThat(fixture.run()).isInstanceOf(MediaPlaybackService.Result.Opened.class);
    }

    @Test void invalidUnavailableNullAndThrowingVerifierFailClosed() {
        Fixture fixture = new Fixture();
        for (MediaCredentialVerifier.Result result : Arrays.asList(new MediaCredentialVerifier.Result.Invalid(), new MediaCredentialVerifier.Result.Unavailable(), null)) {
            fixture.verification = result; fixture.calls.clear();
            assertThat(fixture.run()).isEqualTo(result instanceof MediaCredentialVerifier.Result.Invalid ? denied(INVALID_CREDENTIAL) : unavailable());
            assertThat(fixture.calls).containsExactly("verify");
        }
        fixture.verifierFailure = true;
        assertThat(fixture.run()).isEqualTo(unavailable());
    }

    @Test void malformedVerifiedClaimStopsBeforeCurrentState() {
        Fixture fixture = new Fixture();
        VerifiedMediaClaim valid = fixture.claim;
        for (String field : List.of("audience", "shareGrantId", "shareAdmissionId", "evidenceId", "mediaObjectId", "purpose", "issuedAt", "notBefore", "expiresAt", "window")) {
            fixture.claim = replace(valid, field, null); fixture.calls.clear();
            assertThat(fixture.run()).isEqualTo(denied(MALFORMED_CLAIM));
            assertThat(fixture.calls).containsExactly("verify");
        }
        for (String field : List.of("revocationGeneration", "codeGeneration", "durationSeconds")) {
            fixture.claim = replace(valid, field, -1);
            assertThat(fixture.run()).isEqualTo(denied(MALFORMED_CLAIM));
        }
        fixture.claim = null;
        assertThat(fixture.run()).isEqualTo(denied(MALFORMED_CLAIM));
        fixture.claim = replace(valid, "issuedAt", valid.notBefore().plusNanos(1));
        assertThat(fixture.run()).isEqualTo(denied(MALFORMED_CLAIM));
    }

    @Test void audienceIsExactAndCannotBeReplacedByTrustedNetworkContext() {
        Fixture fixture = new Fixture(); VerifiedMediaClaim valid = fixture.claim;
        for (String audience : List.of("", "ONECALL:MEDIA-PLAYBACK:V1", "onecall:media-playback:v1 ", " onecall:media-playback:v1", "onecall:media-playback:v1/", "other:media-playback:v1")) {
            fixture.claim = replace(valid, "audience", audience); fixture.calls.clear();
            assertThat(fixture.run()).isEqualTo(denied(WRONG_AUDIENCE));
            assertThat(fixture.calls).containsExactly("verify");
        }
    }

    @Test void purposeMismatchPrecedesTimeAndStateChecks() {
        Fixture fixture = new Fixture();
        fixture.now = fixture.claim.expiresAt(); fixture.claim = purpose(fixture.claim, Purpose.RENEW);
        assertThat(fixture.run()).isEqualTo(denied(WRONG_PURPOSE));
        assertThat(fixture.calls).containsExactly("verify"); fixture.calls.clear();
        assertThat(fixture.service().authorizeAndOpen(fixture.credential, null)).isEqualTo(denied(WRONG_PURPOSE));
        assertThat(fixture.calls).isEmpty();
    }

    @Test void exclusiveExpiryAndNotBeforeBoundariesAreDeterministic() {
        Fixture fixture = new Fixture();
        fixture.now = fixture.claim.notBefore();
        assertThat(fixture.run()).isInstanceOf(MediaPlaybackService.Result.Opened.class);
        fixture.now = fixture.claim.notBefore().minusNanos(1);
        assertThat(fixture.run()).isEqualTo(denied(NOT_YET_VALID));
        fixture.now = fixture.claim.expiresAt();
        assertThat(fixture.run()).isEqualTo(denied(EXPIRED));
        fixture.now = fixture.claim.expiresAt().minusNanos(1);
        assertThat(fixture.run()).isInstanceOf(MediaPlaybackService.Result.Opened.class);
        fixture.claim = replace(replace(fixture.claim, "issuedAt", NOW.plusSeconds(1)), "notBefore", NOW.plusSeconds(1));
        fixture.now = NOW;
        assertThat(fixture.run()).isEqualTo(denied(NOT_YET_VALID));
    }

    @Test void invalidDurationAndReadWindowsAreNeverClamped() {
        Fixture fixture = new Fixture(); VerifiedMediaClaim valid = fixture.claim;
        for (var window : List.of(new VerifiedMediaClaim.PlaybackWindow(null, Duration.ofSeconds(1)),
                new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, null),
                new VerifiedMediaClaim.PlaybackWindow(Duration.ofNanos(-1), Duration.ofSeconds(1)),
                new VerifiedMediaClaim.PlaybackWindow(Duration.ofSeconds(1), Duration.ofSeconds(1)),
                new VerifiedMediaClaim.PlaybackWindow(Duration.ofSeconds(2), Duration.ofSeconds(1)),
                new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(Long.MAX_VALUE)),
                new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(11)))) {
            fixture.claim = replace(valid, "window", window);
            assertThat(fixture.run()).isEqualTo(denied(MALFORMED_CLAIM));
        }
        fixture.claim = replace(valid, "durationSeconds", 0);
        assertThat(fixture.run()).isEqualTo(denied(MALFORMED_CLAIM));
        assertThat(fixture.calls).doesNotContain("state", "evidence", "open");
    }

    @Test void stateDenialsUnavailableAndMalformedResultsPreventEvidenceLookup() {
        Fixture fixture = new Fixture();
        for (var reason : MediaPlaybackService.Reason.values()) {
            fixture.state = new PlaybackStateReader.Result.Denied(reason);
            assertThat(fixture.run()).isEqualTo(denied(reason));
        }
        for (PlaybackStateReader.Result result : Arrays.asList(null, new PlaybackStateReader.Result.Unavailable(),
                new PlaybackStateReader.Result.Allowed(null), new PlaybackStateReader.Result.Allowed(fixture.claim.issuedAt().minusNanos(1)),
                new PlaybackStateReader.Result.Allowed(fixture.claim.notBefore().minusNanos(1)),
                new PlaybackStateReader.Result.Allowed(fixture.claim.expiresAt()),
                new PlaybackStateReader.Result.Allowed(fixture.claim.expiresAt().plusNanos(1)))) {
            fixture.state = result;
            assertThat(fixture.run()).isEqualTo(unavailable());
        }
        fixture.stateFailure = true;
        assertThat(fixture.run()).isEqualTo(unavailable());
        assertThat(fixture.calls).doesNotContain("evidence", "open");
    }

    @Test void crossShareAdmissionAndEvidenceStateCannotReachControlledOpen() {
        var stateFixture = new SharingPlaybackStateReaderTests.Fixture();
        Fixture fixture = new Fixture(); fixture.claim = stateFixture.claim;
        fixture.realState = stateFixture.reader();
        when(stateFixture.repository.findGrantById(fixture.claim.shareGrantId())).thenReturn(Optional.empty());
        assertThat(fixture.run()).isEqualTo(denied(ADMISSION_MISMATCH));
        stateFixture.stub();
        fixture.claim = replace(stateFixture.claim, "shareAdmissionId", UUID.randomUUID());
        assertThat(fixture.run()).isEqualTo(denied(ADMISSION_MISMATCH));
        fixture.claim = replace(stateFixture.claim, "evidenceId", UUID.randomUUID());
        assertThat(fixture.run()).isEqualTo(denied(SCOPE_MISMATCH));
        assertThat(fixture.calls).doesNotContain("evidence", "open");
    }

    @Test void missingEvidenceDeniesWithoutControlledOpen() {
        Fixture fixture = new Fixture(); fixture.resolution = new EvidenceMediaObjectReader.Result.Missing();
        assertThat(fixture.run()).isEqualTo(denied(EVIDENCE_MISSING));
        assertThat(fixture.calls).containsExactly("verify", "state", "evidence");
    }

    @Test void unavailableThrowingAndAmbiguousEvidenceNeverFallsBack() {
        Fixture fixture = new Fixture();
        for (EvidenceMediaObjectReader.Result result : Arrays.asList(null, new EvidenceMediaObjectReader.Result.Unavailable())) {
            fixture.calls.clear(); fixture.resolution = result;
            assertThat(fixture.run()).isEqualTo(unavailable());
            assertThat(fixture.calls).containsExactly("verify", "state", "evidence");
        }
        fixture.evidenceFailure = true;
        assertThat(fixture.run()).isEqualTo(unavailable());
    }

    @Test void resolvedObjectIdentityAndDurationMustMatchVerifiedScope() {
        Fixture fixture = new Fixture();
        for (ResolvedMediaObject media : List.of(new ResolvedMediaObject(UUID.randomUUID(), "a".repeat(64), 123, "audio/wav", 10),
                new ResolvedMediaObject(fixture.claim.mediaObjectId(), "a".repeat(64), 123, "audio/wav", 11))) {
            fixture.resolution = new EvidenceMediaObjectReader.Result.Found(media);
            assertThat(fixture.run()).isEqualTo(denied(SCOPE_MISMATCH));
        }
        fixture.resolution = new EvidenceMediaObjectReader.Result.Found(new ResolvedMediaObject(fixture.claim.mediaObjectId(), "a".repeat(64), 123, "audio/wav", null));
        assertThat(fixture.run()).isEqualTo(unavailable());
        assertThat(fixture.calls).doesNotContain("open");
    }

    @Test void evidenceMetadataCannotBeOverriddenByCallerOrClaim() throws Exception {
        assertThat(MediaPlaybackService.class.getMethod("authorizeAndOpen", OpaquePlaybackCredential.class, Purpose.class).getParameterCount()).isEqualTo(2);
        Fixture fixture = new Fixture();
        var scope = ((MediaPlaybackService.Result.Opened) fixture.run()).handle().scope();
        assertThat(scope.media()).isSameAs(fixture.media);
        assertThat(scope.window()).isEqualTo(fixture.claim.window());
        assertThatThrownBy(() -> new ControlledMediaReader.ReadScope(fixture.media, Purpose.PLAYBACK,
                new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(11)), NOW))
                .hasMessage("Invalid playback scope").hasNoCause();
        assertThatThrownBy(() -> new ControlledMediaReader.ControlledReadHandle(null)).hasMessage("Invalid playback handle");
    }

    @Test void readerUnavailableNullAndThrowingResultsReturnNoHandle() {
        Fixture fixture = new Fixture();
        for (ControlledMediaReader.Result result : Arrays.asList(null, new ControlledMediaReader.Result.Unavailable())) {
            fixture.openResult = result; fixture.calls.clear();
            assertThat(fixture.run()).isEqualTo(unavailable());
            assertThat(fixture.calls).containsExactly("verify", "state", "evidence", "open");
        }
        fixture.readerFailure = true;
        assertThat(fixture.run()).isEqualTo(unavailable());
    }

    @Test void readerCannotWidenOrMisbindReturnedHandle() {
        Fixture fixture = new Fixture();
        List<UnaryOperator<ControlledMediaReader.ReadScope>> mutations = List.of(
                scope -> new ControlledMediaReader.ReadScope(new ResolvedMediaObject(UUID.randomUUID(), "a".repeat(64),123,"audio/wav",10), scope.purpose(), scope.window(), scope.expiresAt()),
                scope -> new ControlledMediaReader.ReadScope(new ResolvedMediaObject(scope.media().mediaObjectId(), "b".repeat(64),123,"audio/wav",10), scope.purpose(), scope.window(), scope.expiresAt()),
                scope -> new ControlledMediaReader.ReadScope(new ResolvedMediaObject(scope.media().mediaObjectId(), "a".repeat(64),124,"audio/wav",10), scope.purpose(), scope.window(), scope.expiresAt()),
                scope -> new ControlledMediaReader.ReadScope(new ResolvedMediaObject(scope.media().mediaObjectId(), "a".repeat(64),123,"audio/mpeg",10), scope.purpose(), scope.window(), scope.expiresAt()),
                scope -> new ControlledMediaReader.ReadScope(new ResolvedMediaObject(scope.media().mediaObjectId(), "a".repeat(64),123,"audio/wav",11), scope.purpose(), scope.window(), scope.expiresAt()),
                scope -> new ControlledMediaReader.ReadScope(scope.media(), Purpose.RENEW, scope.window(), scope.expiresAt()),
                scope -> new ControlledMediaReader.ReadScope(scope.media(), scope.purpose(), new VerifiedMediaClaim.PlaybackWindow(Duration.ZERO, Duration.ofSeconds(10)), scope.expiresAt()),
                scope -> new ControlledMediaReader.ReadScope(scope.media(), scope.purpose(), scope.window(), scope.expiresAt().plusSeconds(1)));
        for (var mutation : mutations) {
            fixture.openResult = new ControlledMediaReader.Result.Opened(new ControlledMediaReader.ControlledReadHandle(mutation.apply(fixture.scope())));
            fixture.calls.clear(); assertThat(fixture.run()).isEqualTo(unavailable());
            assertThat(fixture.calls).containsExactly("verify", "state", "evidence", "open");
        }
    }

    @Test void expiryCrossingDuringResolutionPreventsOpen() {
        Fixture fixture = new Fixture(); fixture.afterResolve = () -> fixture.now = fixture.claim.expiresAt();
        assertThat(fixture.run()).isEqualTo(denied(EXPIRED));
        assertThat(fixture.calls).containsExactly("verify", "state", "evidence");
    }

    @Test void ambientTransactionsAndPostOpenDependencyIoAreForbidden() {
        Fixture fixture = new Fixture(); TransactionSynchronizationManager.setActualTransactionActive(true);
        try { assertThat(fixture.run()).isEqualTo(unavailable()); assertThat(fixture.calls).isEmpty(); }
        finally { TransactionSynchronizationManager.clear(); }
        fixture.sealAfterOpen = true;
        assertThat(fixture.run()).isInstanceOf(MediaPlaybackService.Result.Opened.class);
        assertThat(fixture.calls).containsExactly("verify", "state", "evidence", "open");
    }

    @Test void credentialsClaimsScopesHandlesOutcomesAndFailuresAreRedacted() {
        Fixture fixture = new Fixture(); var result = (MediaPlaybackService.Result.Opened) fixture.run();
        assertThat(result.handle().scope()).isEqualTo(fixture.scope());
        JsonMapper mapper = JsonMapper.builder().build();
        for (Object value : List.of(fixture.credential, fixture.claim, fixture.claim.window(), fixture.scope(), result.handle(), result,
                new ControlledMediaReader.Result.Opened(result.handle()))) {
            assertThat(value.toString()).contains("[REDACTED]").doesNotContain(fixture.claim.mediaObjectId().toString(), "private", "audio/wav", "123");
            assertThat(mapper.writeValueAsString(value)).isEqualTo(mapper.writeValueAsString(value.toString()));
            assertThat(mapper.writeValueAsString(Map.of("nested", value))).doesNotContain(fixture.claim.mediaObjectId().toString(), "private", "audio/wav");
        }
        fixture.readerFailure = true;
        assertThat(mapper.writeValueAsString(fixture.run())).doesNotContain("private", "https://", "bucket");
        assertThatThrownBy(() -> new MediaPlaybackService.Result.Opened(null)).hasMessage("Invalid playback result").hasNoCause();
        assertThat(output.getAll()).doesNotContain("private-credential", "bucket/private", "https://private/secret", fixture.claim.mediaObjectId().toString());
    }

    static VerifiedMediaClaim replace(VerifiedMediaClaim claim, String field, Object value) {
        try {
            var components = VerifiedMediaClaim.class.getRecordComponents();
            Object[] values = new Object[components.length]; Class<?>[] types = new Class<?>[components.length];
            for (int index = 0; index < components.length; index++) {
                values[index] = components[index].getName().equals(field) ? value : components[index].getAccessor().invoke(claim);
                types[index] = components[index].getType();
            }
            return VerifiedMediaClaim.class.getConstructor(types).newInstance(values);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    static VerifiedMediaClaim purpose(VerifiedMediaClaim claim, Purpose purpose) { return replace(claim, "purpose", purpose); }
    static MediaPlaybackService.Result denied(MediaPlaybackService.Reason reason) { return new MediaPlaybackService.Result.Denied(reason); }
    static MediaPlaybackService.Result unavailable() { return new MediaPlaybackService.Result.Unavailable(); }

    static class Fixture {
        VerifiedMediaClaim claim = new VerifiedMediaClaim("onecall:media-playback:v1", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                0, 0, Purpose.PLAYBACK, NOW.minusSeconds(2), NOW.minusSeconds(1), NOW.plusSeconds(20), 10,
                new VerifiedMediaClaim.PlaybackWindow(Duration.ofSeconds(1), Duration.ofSeconds(9)));
        OpaquePlaybackCredential credential = new OpaquePlaybackCredential("private-credential");
        ResolvedMediaObject media = new ResolvedMediaObject(claim.mediaObjectId(), "a".repeat(64), 123, "audio/wav", 10);
        MediaCredentialVerifier.Result verification;
        PlaybackStateReader.Result state;
        EvidenceMediaObjectReader.Result resolution;
        ControlledMediaReader.Result openResult;
        PlaybackStateReader realState;
        boolean verifierFailure, stateFailure, evidenceFailure, readerFailure, sealAfterOpen, sealed;
        Instant now = NOW;
        Runnable afterResolve = () -> {};
        final List<String> calls = new ArrayList<>();
        Fixture() {
            verification = new MediaCredentialVerifier.Result.Verified(claim);
            state = new PlaybackStateReader.Result.Allowed(NOW);
            resolution = new EvidenceMediaObjectReader.Result.Found(media);
            openResult = new ControlledMediaReader.Result.Opened(new ControlledMediaReader.ControlledReadHandle(scope()));
        }
        ControlledMediaReader.ReadScope scope() { return new ControlledMediaReader.ReadScope(media, claim.purpose(), claim.window(), claim.expiresAt()); }
        void call(String name) {
            assertThat(sealed).isFalse(); assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse(); calls.add(name);
        }
        MediaPlaybackService service() {
            return new MediaPlaybackService(value -> {
                call("verify"); assertThat(value).isSameAs(credential);
                if (verifierFailure) throw new IllegalStateException("bucket/private https://private/secret");
                return verification instanceof MediaCredentialVerifier.Result.Verified ? new MediaCredentialVerifier.Result.Verified(claim) : verification;
            }, value -> {
                call("state"); if (stateFailure) throw new IllegalStateException("private state");
                return realState == null ? state : realState.check(value);
            }, evidenceId -> {
                call("evidence"); assertThat(evidenceId).isEqualTo(claim.evidenceId());
                if (evidenceFailure) throw new IllegalStateException("private evidence");
                afterResolve.run(); return resolution;
            }, scope -> {
                call("open"); assertThat(scope).isEqualTo(scope());
                if (sealAfterOpen) sealed = true;
                if (readerFailure) throw new IllegalStateException("bucket/private https://private/secret");
                if (openResult instanceof ControlledMediaReader.Result.Opened success && success.handle().scope().equals(new ControlledMediaReader.ReadScope(media, Purpose.PLAYBACK,
                        new VerifiedMediaClaim.PlaybackWindow(Duration.ofSeconds(1), Duration.ofSeconds(9)), NOW.plusSeconds(20))))
                    return new ControlledMediaReader.Result.Opened(new ControlledMediaReader.ControlledReadHandle(scope));
                return openResult;
            }, new Clock() {
                @Override public ZoneId getZone() { return ZoneOffset.UTC; }
                @Override public Clock withZone(ZoneId zone) { return this; }
                @Override public Instant instant() { assertThat(sealed).isFalse(); return now; }
            });
        }
        MediaPlaybackService.Result run() { return service().authorizeAndOpen(credential, Purpose.PLAYBACK); }
    }
}
