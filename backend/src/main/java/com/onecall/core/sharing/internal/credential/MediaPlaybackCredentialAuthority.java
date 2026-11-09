package com.onecall.core.sharing.internal.credential;

import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider;
import com.onecall.core.sharing.internal.playback.MediaCredentialVerifier;
import com.onecall.core.sharing.internal.playback.OpaquePlaybackCredential;
import com.onecall.core.sharing.internal.playback.VerifiedMediaClaim;
import com.onecall.core.sharing.internal.playback.VerifiedMediaClaim.PlaybackWindow;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;

public final class MediaPlaybackCredentialAuthority implements ShareMediaCredentialProvider, MediaCredentialVerifier {
    private final ObjectProvider<MediaPlaybackSigningKeyProvider> keys;
    private final Clock clock;

    public MediaPlaybackCredentialAuthority(ObjectProvider<MediaPlaybackSigningKeyProvider> keys, Clock clock) {
        this.keys = Objects.requireNonNull(keys);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override public ShareMediaCredentialProvider.Result issue(MediaCredentialRequest request) {
        return new ShareMediaCredentialProvider.Unavailable();
    }

    @Override public MediaAuthorizationCredentialResult issueOrReplay(MediaAuthorizationCredentialRequest request) {
        Map<String, MediaPlaybackSigningKeyProvider.Entry> snapshot = snapshot();
        if (snapshot == null) return new MediaAuthorizationCredentialResult.Unavailable();
        try {
            Instant now = MediaPlaybackCredentialCodec.instant(MediaPlaybackCredentialCodec.micros(clock.instant()));
            Instant bound = MediaPlaybackCredentialCodec.instant(Math.min(MediaPlaybackCredentialCodec.micros(request.grantExpiresAt()),
                    Math.min(MediaPlaybackCredentialCodec.micros(request.admissionExpiresAt()), MediaPlaybackCredentialCodec.micros(request.sessionExpiresAt()))));
            Instant expiry = MediaPlaybackCredentialCodec.instant(MediaPlaybackCredentialCodec.micros(request.credentialExpiresAt()));
            ReplayAnchor anchor = request.priorReplayAnchor();
            MediaPlaybackSigningKeyProvider.Entry selected;
            if (anchor == null) {
                if (!expiry.equals(bound)) return new MediaAuthorizationCredentialResult.Malformed();
                selected = snapshot.values().stream().filter(entry -> entry.role() == MediaPlaybackSigningKeyProvider.Role.ACTIVE).findFirst().orElseThrow();
                long micros = MediaPlaybackCredentialCodec.micros(now);
                anchor = new ReplayAnchor(micros, micros, selected.kid());
            } else {
                selected = snapshot.get(anchor.kid());
                if (selected == null) return new MediaAuthorizationCredentialResult.Unavailable();
            }
            if (expiry.isAfter(bound)) return new MediaAuthorizationCredentialResult.Malformed();
            var facts = request.mediaFacts();
            var claim = new VerifiedMediaClaim(MediaPlaybackCredentialCodec.AUDIENCE, request.shareGrantId(), request.shareAdmissionId(),
                    facts.evidenceId(), facts.mediaObjectId(), request.revocationGeneration(), request.codeGeneration(), Purpose.valueOf(request.purpose()),
                    MediaPlaybackCredentialCodec.instant(anchor.issuedAtMicros()), MediaPlaybackCredentialCodec.instant(anchor.notBeforeMicros()),
                    expiry, facts.durationSeconds(), new PlaybackWindow(Duration.ofNanos(facts.windowStartNanos()), Duration.ofNanos(facts.windowEndNanos())));
            if (!MediaPlaybackCredentialCodec.valid(claim, now)) return new MediaAuthorizationCredentialResult.Malformed();
            return new MediaAuthorizationCredentialResult.Issued(MediaPlaybackCredentialCodec.encode(claim, selected), expiry, anchor);
        } catch (Exception malformed) {
            return new MediaAuthorizationCredentialResult.Malformed();
        }
    }

    @Override public MediaCredentialVerifier.Result verify(OpaquePlaybackCredential credential) {
        Map<String, MediaPlaybackSigningKeyProvider.Entry> snapshot = snapshot();
        if (snapshot == null) return new MediaCredentialVerifier.Result.Unavailable();
        try {
            var claim = MediaPlaybackCredentialCodec.decode(credential.value(), snapshot);
            Instant now = MediaPlaybackCredentialCodec.instant(MediaPlaybackCredentialCodec.micros(clock.instant()));
            return MediaPlaybackCredentialCodec.valid(claim, now) ? new MediaCredentialVerifier.Result.Verified(claim) : new MediaCredentialVerifier.Result.Invalid();
        } catch (Exception invalid) {
            return new MediaCredentialVerifier.Result.Invalid();
        }
    }

    private Map<String, MediaPlaybackSigningKeyProvider.Entry> snapshot() {
        try {
            var providers = keys.stream().limit(2).toList();
            if (providers.size() != 1 || providers.getFirst() == null) return null;
            if (!(providers.getFirst().currentKeys() instanceof MediaPlaybackSigningKeyProvider.Result.Available available)
                    || available.verifyOnly() == null || !validEntry(available.active(), MediaPlaybackSigningKeyProvider.Role.ACTIVE)) return null;
            Map<String, MediaPlaybackSigningKeyProvider.Entry> entries = new HashMap<>();
            entries.put(available.active().kid(), available.active());
            for (var entry : available.verifyOnly()) {
                if (!validEntry(entry, MediaPlaybackSigningKeyProvider.Role.VERIFY_ONLY) || entries.putIfAbsent(entry.kid(), entry) != null) return null;
            }
            return Map.copyOf(entries);
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    private boolean validEntry(MediaPlaybackSigningKeyProvider.Entry entry, MediaPlaybackSigningKeyProvider.Role role) {
        return entry != null && entry.kid() != null && entry.kid().matches("[A-Za-z0-9_-]{1,32}")
                && entry.material() != null && entry.material().length == 32 && entry.role() == role;
    }
}
