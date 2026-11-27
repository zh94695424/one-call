package com.onecall.core.operations_iam.internal.orderread;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.operations.OperationsOrderReadTestProviders.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.Anchor;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class OperationsOrderCursorCodecTests {
    private final OperationsOrderCursorCodec codec = new OperationsOrderCursorCodec(properties());
    private OperationsOrderReadRequest request(Map<String, List<String>> query) { return OperationsOrderReadRequest.parse(raw(query)); }
    private void fails(String cursor, OperationsOrderReadRequest request, Code code) {
        assertThatThrownBy(() -> codec.decode(cursor, request)).isInstanceOfSatisfying(Failure.class,
                failure -> assertThat(failure.code()).isEqualTo(code));
    }
    @Test void authenticatedRoundTripIsRandomizedOpaqueAndLimitIndependent() {
        var request = request(Map.of("subjectId", List.of(SUBJECT.toString()), "paymentStatus", List.of("UNKNOWN")));
        var anchor = new Anchor(TIME, ORDER);
        String cursor = codec.encode(anchor, request);
        assertThat(cursor).hasSizeLessThanOrEqualTo(512).doesNotContain(ORDER.toString(), SUBJECT.toString());
        assertThat(codec.encode(anchor, request)).isNotEqualTo(cursor);
        assertThat(codec.decode(cursor, request(Map.of("limit", List.of("100"), "paymentStatus", List.of("UNKNOWN"), "subjectId", List.of(SUBJECT.toString()))))).isEqualTo(anchor);
    }
    @Test void filterChangesRequireAuthenticationBeforeMismatch() {
        var unfiltered = request(Map.of());
        String cursor = codec.encode(new Anchor(TIME, ORDER), unfiltered);
        fails(cursor, request(Map.of("subjectId", List.of(SUBJECT.toString()))), Code.CURSOR_FILTER_MISMATCH);
        fails(cursor, request(Map.of("paymentStatus", List.of("UNKNOWN"))), Code.CURSOR_FILTER_MISMATCH);
        byte[] bytes = Base64.getUrlDecoder().decode(cursor);
        for (int offset : List.of(0, 2, 10, 22, bytes.length - 1)) {
            byte[] mutant = bytes.clone(); mutant[offset] ^= 1;
            fails(Base64.getUrlEncoder().withoutPadding().encodeToString(mutant),
                    request(Map.of("paymentStatus", List.of("PAID"))), Code.CURSOR_INVALID);
        }
    }
    @Test void canonicalFrameAndAnchorValidationAreStrict() {
        for (String value : List.of("", " ", "1", "offset=10", "A".repeat(513), "YWJj=", "!"))
            fails(value, request(Map.of()), Code.CURSOR_INVALID);
        for (Instant instant : List.of(TIME.plusNanos(1), Instant.parse("0000-12-31T23:59:59Z"), Instant.parse("+10000-01-01T00:00:00Z")))
            assertThatThrownBy(() -> codec.encode(new Anchor(instant, ORDER), request(Map.of()))).isInstanceOf(Failure.class);
        for (Instant instant : List.of(Instant.parse("0001-01-01T00:00:00Z"), Instant.parse("9999-12-31T23:59:59.999999Z")))
            assertThat(codec.decode(codec.encode(new Anchor(instant, ORDER), request(Map.of())), request(Map.of())).createdAt()).isEqualTo(instant);
    }
    @Test void rotationRetainsOnlyExplicitKeysAndConfigurationFailsClosed() {
        var old = properties().cursor().keys().getFirst();
        byte[] key = new byte[32]; Arrays.fill(key, (byte) 43);
        var active = new OperationsSubjectReadProperties.KeyEntry("rotated", Base64.getUrlEncoder().withoutPadding().encodeToString(key));
        var rotated = new OperationsOrderCursorCodec(new OperationsSubjectReadProperties(true, null,
                new OperationsSubjectReadProperties.Cursor(active.id(), List.of(active, old))));
        String cursor = codec.encode(new Anchor(TIME, ORDER), request(Map.of()));
        assertThat(rotated.decode(cursor, request(Map.of())).orderId()).isEqualTo(ORDER);
        fails(rotated.encode(new Anchor(TIME, ORDER), request(Map.of())), request(Map.of()), Code.CURSOR_INVALID);
        assertThatThrownBy(() -> new OperationsOrderCursorCodec(null)).isInstanceOfSatisfying(Failure.class,
                failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
        assertThat(codec.toString()).contains("REDACTED").doesNotContain(old.material());
    }
}
