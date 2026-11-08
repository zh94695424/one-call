package com.onecall.core.evidence.mediaread;

import java.util.Objects;
import java.util.UUID;

public interface EvidenceMediaObjectReader {
    Result resolve(UUID evidenceId);

    sealed interface Result permits Result.Found, Result.Missing, Result.Unavailable {
        record Found(ResolvedMediaObject media) implements Result {
            public Found { Objects.requireNonNull(media); }
            @Override public String toString() { return "Found[REDACTED]"; }
        }
        record Missing() implements Result { }
        record Unavailable() implements Result { }
    }
}
