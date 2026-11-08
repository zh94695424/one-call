package com.onecall.core.sharing.internal.playback;

import java.time.Instant;

public interface PlaybackStateReader {
    Result check(VerifiedMediaClaim claim);

    sealed interface Result permits Result.Allowed, Result.Denied, Result.Unavailable {
        record Allowed(Instant checkedAt) implements Result { }
        record Denied(MediaPlaybackService.Reason reason) implements Result {
            public Denied {
                if (reason == null) throw new IllegalArgumentException("Invalid playback reason");
            }
        }
        record Unavailable() implements Result { }
    }
}
