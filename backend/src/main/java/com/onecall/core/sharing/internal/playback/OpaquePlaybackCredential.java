package com.onecall.core.sharing.internal.playback;

import com.fasterxml.jackson.annotation.JsonValue;

public final class OpaquePlaybackCredential {
    private final String value;

    public OpaquePlaybackCredential(String value) { this.value = value; }
    public String value() { return value; }
    @JsonValue public String redacted() { return "OpaquePlaybackCredential[REDACTED]"; }
    @Override public String toString() { return redacted(); }
}
