package com.onecall.core.sharing.internal.gateway;

import com.onecall.core.evidence.mediaread.ControlledMediaByteSource.ByteInterval;
import java.util.List;

public final class SingleByteRange {
    private SingleByteRange() { }

    public sealed interface Spec permits Full, Closed, OpenEnded, Suffix { }
    public record Full() implements Spec { }
    public record Closed(long first, long last) implements Spec { }
    public record OpenEnded(long first) implements Spec { }
    public record Suffix(long length) implements Spec { }
    public sealed interface ParseResult permits ParseResult.Accepted, ParseResult.Malformed {
        record Accepted(Spec spec) implements ParseResult { }
        record Malformed() implements ParseResult { }
    }
    public sealed interface Resolution permits Resolution.Selected, Resolution.Unsatisfiable {
        record Selected(ByteInterval interval, boolean partial) implements Resolution { }
        record Unsatisfiable() implements Resolution { }
    }

    public static ParseResult parse(List<String> values) {
        if (values == null) return new ParseResult.Malformed();
        if (values.isEmpty()) return new ParseResult.Accepted(new Full());
        if (values.size() != 1) return new ParseResult.Malformed();
        String value = values.getFirst();
        if (value == null || value.length() > 128 || !value.matches("bytes=([0-9]+-[0-9]*|-[0-9]+)"))
            return new ParseResult.Malformed();
        int dash = value.indexOf('-', 6);
        try {
            if (dash == 6) return new ParseResult.Accepted(new Suffix(Long.parseLong(value.substring(7))));
            long first = Long.parseLong(value.substring(6, dash));
            if (dash == value.length() - 1) return new ParseResult.Accepted(new OpenEnded(first));
            long last = Long.parseLong(value.substring(dash + 1));
            return first > last ? new ParseResult.Malformed() : new ParseResult.Accepted(new Closed(first, last));
        } catch (NumberFormatException failure) { return new ParseResult.Malformed(); }
    }

    public static Resolution resolve(Spec spec, long objectSize) {
        if (spec == null || objectSize <= 0) throw new IllegalArgumentException("Invalid range scope");
        if (spec instanceof Full) return new Resolution.Selected(new ByteInterval(0, objectSize), false);
        if (spec instanceof Closed closed) {
            if (closed.first() < 0 || closed.last() < closed.first()) throw new IllegalArgumentException("Invalid range scope");
            if (closed.first() >= objectSize) return new Resolution.Unsatisfiable();
            return new Resolution.Selected(new ByteInterval(closed.first(), Math.min(closed.last(), objectSize - 1) + 1), true);
        }
        if (spec instanceof OpenEnded open) {
            if (open.first() < 0) throw new IllegalArgumentException("Invalid range scope");
            if (open.first() >= objectSize) return new Resolution.Unsatisfiable();
            return new Resolution.Selected(new ByteInterval(open.first(), objectSize), true);
        }
        long suffix = ((Suffix) spec).length();
        if (suffix < 0) throw new IllegalArgumentException("Invalid range scope");
        if (suffix == 0) return new Resolution.Unsatisfiable();
        return new Resolution.Selected(new ByteInterval(objectSize - Math.min(suffix, objectSize), objectSize), true);
    }
}
