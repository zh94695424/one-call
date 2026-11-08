package com.onecall.core.evidence.mediaread;

import com.fasterxml.jackson.annotation.JsonValue;
import java.io.IOException;

public interface ControlledMediaByteSource {
    Result open(OpenRequest request) throws IOException;

    record ByteInterval(long startInclusive, long endExclusive) {
        public ByteInterval {
            if (startInclusive < 0 || endExclusive <= startInclusive)
                throw new IllegalArgumentException("Invalid byte interval");
        }
        public long length() { return endExclusive - startInclusive; }
        @JsonValue public String redacted() { return "ByteInterval[REDACTED]"; }
        @Override public String toString() { return redacted(); }
    }

    record OpenRequest(ResolvedMediaObject media, ByteInterval interval) {
        public OpenRequest {
            if (media == null || interval == null || interval.endExclusive() > media.sizeBytes())
                throw new IllegalArgumentException("Invalid byte request");
        }
        @JsonValue public String redacted() { return "OpenRequest[REDACTED]"; }
        @Override public String toString() { return redacted(); }
    }

    sealed interface Result permits Result.Opened, Result.Unavailable {
        record Opened(ReadSession session) implements Result {
            public Opened {
                if (session == null) throw new IllegalArgumentException("Invalid byte session");
            }
            @JsonValue public String redacted() { return "ByteOpened[REDACTED]"; }
            @Override public String toString() { return redacted(); }
        }
        record Unavailable() implements Result { }
    }

    interface ReadSession extends AutoCloseable {
        OpenRequest request();
        int read(byte[] destination, int offset, int length) throws IOException;
        void abort() throws IOException;
        @Override void close() throws IOException;
        @JsonValue default String redacted() { return "MediaReadSession[REDACTED]"; }
    }
}
