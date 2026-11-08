package com.onecall.archtests.sharing;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.evidence.mediaread.ControlledMediaByteSource;
import com.onecall.core.sharing.internal.gateway.SingleByteRange;
import com.onecall.core.sharing.internal.gateway.SingleByteRange.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class SingleByteRangeTests {
    private Resolution resolve(String value, long size) {
        var parsed = SingleByteRange.parse(value == null ? List.of() : List.of(value));
        assertThat(parsed).isInstanceOf(ParseResult.Accepted.class);
        return SingleByteRange.resolve(((ParseResult.Accepted) parsed).spec(), size);
    }
    private void selected(String value, long size, long start, long end, boolean partial) {
        assertThat(resolve(value, size)).isEqualTo(new Resolution.Selected(new ControlledMediaByteSource.ByteInterval(start, end), partial));
    }

    @Test void absentRangeSelectsExactlyFullPositiveObject() throws Exception {
        for (long size : List.of(1L, 16L, Long.MAX_VALUE)) selected(null, size, 0, size, false);
        assertThatThrownBy(() -> resolve(null, 0)).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid range scope");
        assertThat(Resolution.Selected.class.getRecordComponents()[0].getType()).isEqualTo(ControlledMediaByteSource.ByteInterval.class);
        assertThat(ControlledMediaByteSource.OpenRequest.class.getRecordComponents()[1].getType()).isEqualTo(ControlledMediaByteSource.ByteInterval.class);
    }
    @Test void closedRangesClipOnlyTheirEndWithoutOverflow() {
        selected("bytes=2-5", 16, 2, 6, true);
        selected("bytes=14-99", 16, 14, 16, true);
        selected("bytes=000-000", 1, 0, 1, true);
        selected("bytes=9223372036854775806-9223372036854775807", Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MAX_VALUE, true);
        assertThat(resolve("bytes=16-99", 16)).isInstanceOf(Resolution.Unsatisfiable.class);
    }
    @Test void openEndedAndSuffixRangesResolveOneBoundedInterval() {
        selected("bytes=12-", 16, 12, 16, true);
        selected("bytes=-4", 16, 12, 16, true);
        selected("bytes=-99", 16, 0, 16, true);
        selected("bytes=-9223372036854775807", Long.MAX_VALUE, 0, Long.MAX_VALUE, true);
        assertThat(resolve("bytes=-0", 16)).isInstanceOf(Resolution.Unsatisfiable.class);
        assertThat(resolve("bytes=16-", 16)).isInstanceOf(Resolution.Unsatisfiable.class);
    }
    @Test void malformedUnitsWhitespaceAndMultipleRangesAreRejected() {
        for (String value : List.of("", "items=0-1", "Bytes=0-1", " bytes=0-1", "bytes=0-1 ", "bytes=-", "bytes=+0-1",
                "bytes=٠-1", "bytes=0-1,4-5", "bytes=0--1", "bytes=0=1", "bytes=0-1\r\n", "bytes=" + "0".repeat(121) + "-1"))
            assertThat(SingleByteRange.parse(List.of(value))).as(value).isInstanceOf(ParseResult.Malformed.class);
        assertThat(SingleByteRange.parse(List.of("bytes=0-1", "bytes=2-3"))).isInstanceOf(ParseResult.Malformed.class);
        assertThat(SingleByteRange.parse(List.of("bytes=" + "0".repeat(119) + "-1"))).isInstanceOf(ParseResult.Accepted.class);
    }
    @Test void reversedAndOverflowDecimalBoundsNeverWrapOrClamp() {
        for (String value : List.of("bytes=5-2", "bytes=9223372036854775808-", "bytes=0-9223372036854775808", "bytes=-9223372036854775808"))
            assertThat(SingleByteRange.parse(List.of(value))).isInstanceOf(ParseResult.Malformed.class);
        assertThat(resolve("bytes=9223372036854775807-", 16)).isInstanceOf(Resolution.Unsatisfiable.class);
    }
    @Test void normalizationMatchesDeterministicSmallObjectOracle() {
        for (int size = 1; size <= 32; size++) {
            for (int first = 0; first <= size + 1; first++) {
                for (int last = first; last <= size + 2; last++) {
                    var result = resolve("bytes=" + first + "-" + last, size);
                    if (first >= size) assertThat(result).isInstanceOf(Resolution.Unsatisfiable.class);
                    else {
                        var selected = (Resolution.Selected) result;
                        var indices = new java.util.ArrayList<Integer>();
                        for (int index = 0; index < size; index++) if (index >= first && index <= last) indices.add(index);
                        assertThat(selected.interval().length()).isEqualTo(indices.size());
                        assertThat(selected.interval().startInclusive()).isEqualTo(indices.getFirst().longValue());
                        assertThat(selected.interval().endExclusive()).isEqualTo(indices.getLast() + 1);
                    }
                }
                if (first < size) selected("bytes=" + first + "-", size, first, size, true);
                if (first > 0) selected("bytes=-" + first, size, Math.max(0, size - first), size, true);
            }
        }
    }
}
