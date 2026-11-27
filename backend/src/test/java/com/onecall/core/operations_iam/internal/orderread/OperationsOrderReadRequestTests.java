package com.onecall.core.operations_iam.internal.orderread;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.operations.OperationsOrderReadTestProviders.*;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.util.*;
import org.junit.jupiter.api.Test;

class OperationsOrderReadRequestTests {
    private void invalid(Map<String, List<String>> query, Code code) {
        assertThatThrownBy(() -> OperationsOrderReadRequest.parse(raw(query))).isInstanceOfSatisfying(Failure.class,
                failure -> assertThat(failure.code()).isEqualTo(code));
    }
    @Test void defaultsBoundsAndCardinalityAreExact() {
        assertThat(OperationsOrderReadRequest.parse(raw(Map.of())).limit()).isEqualTo(20);
        for (String limit : List.of("1", "001", "020", "100"))
            assertThat(OperationsOrderReadRequest.parse(raw(Map.of("limit", List.of(limit)))).limit()).isBetween(1, 100);
        for (String limit : List.of("", "0", "101", "-1", "+1", "1.0", "1e1", " 1", "999999999999999"))
            invalid(Map.of("limit", List.of(limit)), Code.VALIDATION_FAILED);
        invalid(Map.of("limit", List.of("1", "2")), Code.VALIDATION_FAILED);
        invalid(Map.of("role", List.of("ADMIN")), Code.VALIDATION_FAILED);
        invalid(Map.of("limit", List.of()), Code.VALIDATION_FAILED);
    }
    @Test void normalizedFilterDigestHasFourDistinctDomainsAndIgnoresLimitAndOrder() {
        var digests = new HashSet<String>();
        for (boolean subject : List.of(false, true)) for (boolean payment : List.of(false, true)) {
            var query = new LinkedHashMap<String, List<String>>();
            if (subject) query.put("subjectId", List.of("ABCDEFAB-0000-0000-0000-000000000001"));
            if (payment) query.put("paymentStatus", List.of("UNKNOWN"));
            var request = OperationsOrderReadRequest.parse(raw(query));
            assertThat(request.digest()).hasSize(32);
            digests.add(HexFormat.of().formatHex(request.digest()));
            query.put("limit", List.of("100"));
            if (subject) query.put("subjectId", List.of("abcdefab-0000-0000-0000-000000000001"));
            assertThat(OperationsOrderReadRequest.parse(raw(query)).digest()).isEqualTo(request.digest());
        }
        assertThat(digests).hasSize(4);
    }
    @Test void malformedFiltersBeatCursorAndDetailsHaveNoQuery() {
        for (String value : List.of("", "paid", " PAID", "PAID,UNPAID"))
            invalid(Map.of("paymentStatus", List.of(value), "cursor", List.of("!")), Code.VALIDATION_FAILED);
        for (String value : List.of("", "1-1-1-1-1", ORDER + " "))
            invalid(Map.of("subjectId", List.of(value)), Code.VALIDATION_FAILED);
        invalid(Map.of("cursor", List.of("!")), Code.CURSOR_INVALID);
        assertThatThrownBy(() -> OperationsOrderReadRequest.detail(new RawDetailRequest(ORDER.toString(), Map.of("limit", List.of("1")), List.of())))
                .isInstanceOf(Failure.class);
        assertThat(OperationsOrderReadRequest.detail(detail())).isEqualTo(ORDER);
    }
    @Test void acceptSpecificityAndRedactionRemainFrozen() {
        for (String header : List.of("application/json", "application/*", "*/*", "application/json;charset=UTF-8"))
            assertThat(OperationsOrderReadRequest.parse(new RawListRequest(Map.of(), List.of(header)))).isNotNull();
        for (String header : List.of("text/html", "application/json;q=0,*/*;q=1", "application/json;charset=ISO-8859-1", "application/json;foo=bar", ""))
            assertThatThrownBy(() -> OperationsOrderReadRequest.parse(new RawListRequest(Map.of(), List.of(header)))).isInstanceOf(Failure.class);
        assertThat(raw(Map.of("cursor", List.of("PRIVATE"))).toString()).doesNotContain("PRIVATE");
        assertThat(OperationsOrderReadRequest.parse(raw(Map.of())).toString()).contains("REDACTED");
    }
}
