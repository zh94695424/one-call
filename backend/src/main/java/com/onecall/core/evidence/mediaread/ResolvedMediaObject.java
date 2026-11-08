package com.onecall.core.evidence.mediaread;

import java.util.Objects;
import java.util.UUID;

public record ResolvedMediaObject(UUID mediaObjectId, String sha256, long sizeBytes, String mimeType,
        Integer durationSeconds) {
    public ResolvedMediaObject {
        Objects.requireNonNull(mediaObjectId);
        Objects.requireNonNull(sha256);
        Objects.requireNonNull(mimeType);
        if (!sha256.matches("[0-9a-f]{64}") || sizeBytes < 1 || mimeType.isBlank()
                || (durationSeconds != null && durationSeconds < 1))
            throw new IllegalArgumentException("Invalid resolved media metadata");
    }

    @Override public String toString() { return "ResolvedMediaObject[REDACTED]"; }
}
