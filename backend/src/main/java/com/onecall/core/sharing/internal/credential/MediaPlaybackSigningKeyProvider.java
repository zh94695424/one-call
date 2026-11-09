package com.onecall.core.sharing.internal.credential;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

public interface MediaPlaybackSigningKeyProvider {
    Result currentKeys();

    enum Role { ACTIVE, VERIFY_ONLY }

    record Entry(String kid, byte[] material, Role role) {
        public Entry { material = material == null ? null : material.clone(); }
        @Override public byte[] material() { return material == null ? null : material.clone(); }
        @JsonValue public String redacted() { return "Entry[REDACTED]"; }
        @Override public String toString() { return redacted(); }
    }

    sealed interface Result permits Result.Available, Result.Unavailable {
        record Available(Entry active, List<Entry> verifyOnly) implements Result {
            public Available { verifyOnly = verifyOnly == null ? null : List.copyOf(verifyOnly); }
            @JsonValue public String redacted() { return "Available[REDACTED]"; }
            @Override public String toString() { return redacted(); }
        }
        record Unavailable() implements Result { }
    }
}
