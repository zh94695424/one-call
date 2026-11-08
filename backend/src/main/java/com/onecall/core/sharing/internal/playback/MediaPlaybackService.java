package com.onecall.core.sharing.internal.playback;

import com.fasterxml.jackson.annotation.JsonValue;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class MediaPlaybackService {
    private final MediaCredentialVerifier verifier;
    private final PlaybackStateReader state;
    private final EvidenceMediaObjectReader evidence;
    private final ControlledMediaReader reader;
    private final Clock clock;

    public MediaPlaybackService(MediaCredentialVerifier verifier, PlaybackStateReader state,
            EvidenceMediaObjectReader evidence, ControlledMediaReader reader, Clock clock) {
        if (verifier == null || state == null || evidence == null || reader == null || clock == null)
            throw new IllegalArgumentException("Invalid playback dependencies");
        this.verifier = verifier;
        this.state = state;
        this.evidence = evidence;
        this.reader = reader;
        this.clock = clock;
    }

    public Result authorizeAndOpen(OpaquePlaybackCredential credential, Purpose expectedPurpose) {
        if (active()) return new Result.Unavailable();
        if (credential == null || credential.value() == null || credential.value().isBlank()
                || credential.value().indexOf('\0') >= 0
                || credential.value().codePointCount(0, credential.value().length()) > 2048
                || !StandardCharsets.UTF_8.newEncoder().canEncode(credential.value()))
            return denied(Reason.MALFORMED_CREDENTIAL);
        if (expectedPurpose == null) return denied(Reason.WRONG_PURPOSE);
        try {
            var verification = verifier.verify(credential);
            if (verification instanceof MediaCredentialVerifier.Result.Invalid) return denied(Reason.INVALID_CREDENTIAL);
            if (!(verification instanceof MediaCredentialVerifier.Result.Verified verified)) return new Result.Unavailable();
            var claim = verified.claim();
            if (!validShape(claim)) return denied(Reason.MALFORMED_CLAIM);
            if (!"onecall:media-playback:v1".equals(claim.audience())) return denied(Reason.WRONG_AUDIENCE);
            if (claim.purpose() != expectedPurpose) return denied(Reason.WRONG_PURPOSE);
            Reason time = timeFailure(claim, clock.instant());
            if (time != null) return denied(time);
            var checked = state.check(claim);
            if (checked instanceof PlaybackStateReader.Result.Denied failure) return denied(failure.reason());
            if (!(checked instanceof PlaybackStateReader.Result.Allowed allowed)
                    || allowed.checkedAt() == null || timeFailure(claim, allowed.checkedAt()) != null || active())
                return new Result.Unavailable();
            var resolved = evidence.resolve(claim.evidenceId());
            if (resolved instanceof EvidenceMediaObjectReader.Result.Missing) return denied(Reason.EVIDENCE_MISSING);
            if (!(resolved instanceof EvidenceMediaObjectReader.Result.Found found)) return new Result.Unavailable();
            var media = found.media();
            if (!claim.mediaObjectId().equals(media.mediaObjectId())) return denied(Reason.SCOPE_MISMATCH);
            if (media.durationSeconds() == null) return new Result.Unavailable();
            if (media.durationSeconds() != claim.durationSeconds()) return denied(Reason.SCOPE_MISMATCH);
            if (media.sizeBytes() <= 0 || media.sha256() == null || !media.sha256().matches("[0-9a-f]{64}")
                    || media.mimeType() == null || media.mimeType().isBlank() || media.durationSeconds() <= 0)
                return new Result.Unavailable();
            time = timeFailure(claim, clock.instant());
            if (time != null) return denied(time);
            if (active()) return new Result.Unavailable();
            var scope = new ControlledMediaReader.ReadScope(media, claim.purpose(), claim.window(), claim.expiresAt());
            var opened = reader.open(scope);
            if (opened instanceof ControlledMediaReader.Result.Opened success
                    && success.handle() != null && scope.equals(success.handle().scope()))
                return new Result.Opened(success.handle());
            return new Result.Unavailable();
        } catch (RuntimeException failure) {
            return new Result.Unavailable();
        }
    }

    static boolean validShape(VerifiedMediaClaim claim) {
        if (claim == null || claim.audience() == null || claim.shareGrantId() == null || claim.shareAdmissionId() == null
                || claim.evidenceId() == null || claim.mediaObjectId() == null || claim.purpose() == null
                || claim.issuedAt() == null || claim.notBefore() == null || claim.expiresAt() == null
                || claim.revocationGeneration() < 0 || claim.codeGeneration() < 0 || claim.durationSeconds() <= 0
                || claim.issuedAt().isAfter(claim.notBefore()) || !claim.notBefore().isBefore(claim.expiresAt())
                || claim.window() == null || claim.window().startInclusive() == null || claim.window().endExclusive() == null)
            return false;
        return !claim.window().startInclusive().isNegative()
                && claim.window().startInclusive().compareTo(claim.window().endExclusive()) < 0
                && claim.window().endExclusive().compareTo(Duration.ofSeconds(claim.durationSeconds())) <= 0;
    }

    static Reason timeFailure(VerifiedMediaClaim claim, Instant now) {
        if (now.isBefore(claim.issuedAt()) || now.isBefore(claim.notBefore())) return Reason.NOT_YET_VALID;
        return !now.isBefore(claim.expiresAt()) ? Reason.EXPIRED : null;
    }
    private static boolean active() { return TransactionSynchronizationManager.isActualTransactionActive(); }
    private static Result denied(Reason reason) { return new Result.Denied(reason); }

    public enum Reason {
        MALFORMED_CREDENTIAL, INVALID_CREDENTIAL, MALFORMED_CLAIM, WRONG_AUDIENCE, WRONG_PURPOSE,
        NOT_YET_VALID, EXPIRED, SCOPE_MISMATCH, REVOKED, STALE_GENERATION, ADMISSION_MISMATCH,
        CURRENT_STATE_DENIED, EVIDENCE_MISSING
    }
    public sealed interface Result permits Result.Opened, Result.Denied, Result.Unavailable {
        record Opened(ControlledMediaReader.ControlledReadHandle handle) implements Result {
            public Opened {
                if (handle == null) throw new IllegalArgumentException("Invalid playback result");
            }
            @JsonValue public String redacted() { return "PlaybackOpened[REDACTED]"; }
            @Override public String toString() { return redacted(); }
        }
        record Denied(Reason reason) implements Result {
            public Denied {
                if (reason == null) throw new IllegalArgumentException("Invalid playback reason");
            }
        }
        record Unavailable() implements Result { }
    }
}
