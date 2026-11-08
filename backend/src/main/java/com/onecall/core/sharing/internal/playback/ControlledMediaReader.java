package com.onecall.core.sharing.internal.playback;

import com.fasterxml.jackson.annotation.JsonValue;
import com.onecall.core.evidence.mediaread.ResolvedMediaObject;
import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;
import java.time.Duration;
import java.time.Instant;

public interface ControlledMediaReader {
    Result open(ReadScope scope);

    record ReadScope(ResolvedMediaObject media, Purpose purpose,
            VerifiedMediaClaim.PlaybackWindow window, Instant expiresAt) {
        public ReadScope {
            if (media == null || purpose == null || expiresAt == null || window == null
                    || window.startInclusive() == null || window.endExclusive() == null
                    || window.startInclusive().isNegative()
                    || window.startInclusive().compareTo(window.endExclusive()) >= 0
                    || media.durationSeconds() != null
                    && window.endExclusive().compareTo(Duration.ofSeconds(media.durationSeconds())) > 0)
                throw new IllegalArgumentException("Invalid playback scope");
        }
        @JsonValue public String redacted() { return "ReadScope[REDACTED]"; }
        @Override public String toString() { return redacted(); }
    }
    record ControlledReadHandle(ReadScope scope) {
        public ControlledReadHandle {
            if (scope == null) throw new IllegalArgumentException("Invalid playback handle");
        }
        @JsonValue public String redacted() { return "ControlledReadHandle[REDACTED]"; }
        @Override public String toString() { return redacted(); }
    }
    sealed interface Result permits Result.Opened, Result.Unavailable {
        record Opened(ControlledReadHandle handle) implements Result {
            public Opened {
                if (handle == null) throw new IllegalArgumentException("Invalid playback result");
            }
            @JsonValue public String redacted() { return "ControlledOpened[REDACTED]"; }
            @Override public String toString() { return redacted(); }
        }
        record Unavailable() implements Result { }
    }
}
