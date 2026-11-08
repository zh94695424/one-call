package com.onecall.core.sharing.internal.playback;

import com.fasterxml.jackson.annotation.JsonValue;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record VerifiedMediaClaim(String audience, UUID shareGrantId, UUID shareAdmissionId,
        UUID evidenceId, UUID mediaObjectId, int revocationGeneration, int codeGeneration, Purpose purpose,
        Instant issuedAt, Instant notBefore, Instant expiresAt, int durationSeconds, PlaybackWindow window) {
    @JsonValue public String redacted() { return "VerifiedMediaClaim[REDACTED]"; }
    @Override public String toString() { return redacted(); }

    public record PlaybackWindow(Duration startInclusive, Duration endExclusive) {
        @JsonValue public String redacted() { return "PlaybackWindow[REDACTED]"; }
        @Override public String toString() { return redacted(); }
    }
}
