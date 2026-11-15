package com.onecall.core.operations_iam.internal.activationread;

import static org.assertj.core.api.Assertions.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadService.RawListRequest;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class OperationsActivationReadRequestTests {
    private OperationsActivationReadRequest parse(Map<String, List<String>> query) {
        return OperationsActivationReadRequest.parse(new RawListRequest(query, List.of()));
    }
    private void invalid(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED));
    }
    @Test
    void absentLimitDefaultsToTwenty() {
        assertThat(parse(Map.of()).limit()).isEqualTo(20);
        assertThat(parse(Map.of()).cursor()).isNull();
    }
    @Test
    void limitsOneThroughOneHundredAreAccepted() {
        for (int limit = 1; limit <= 100; limit++) assertThat(parse(Map.of("limit", List.of("" + limit))).limit()).isEqualTo(limit);
        for (String limit : List.of("01", "001", "020")) assertThat(parse(Map.of("limit", List.of(limit))).limit()).isEqualTo(Integer.parseInt(limit));
    }
    @Test
    void invalidLimitLexemesAndBoundsAreValidationFailed() {
        for (String limit : List.of("0", "101", "-1", "+1", "", "1.0", "1e1", " 1", "1 ", "0001", "١")) invalid(() -> parse(Map.of("limit", List.of(limit))));
    }
    @Test
    void onlyCursorAndLimitNamesAreAccepted() {
        for (String name : List.of("employeeId", "role", "subjectId", "source", "claimStatus", "activationStatus", "realNameStatus", "riskState", "offset", "total", "search", "filter", "Limit")) invalid(() -> parse(Map.of(name, List.of("x"))));
    }
    @Test
    void duplicateAndNullQueryValuesAreValidationFailed() {
        invalid(() -> OperationsActivationReadRequest.parse(null));
        invalid(() -> parse(null));
        for (List<String> values : Arrays.asList(null, List.<String>of(), Arrays.asList((String) null), List.of("1", "1"), List.of("1", "2"))) {
            var query = new HashMap<String, List<String>>(); query.put("limit", values); invalid(() -> parse(query));
        }
        var query = new HashMap<String, List<String>>(); query.put(null, List.of("x")); invalid(() -> parse(query));
    }
    @Test
    void cursorContentIsDeferredToCodecAfterShapeValidation() {
        for (String cursor : List.of("", "x".repeat(513), "not a cursor")) assertThat(parse(Map.of("cursor", List.of(cursor))).cursor()).isEqualTo(cursor);
        invalid(() -> parse(Map.of("cursor", List.of("a", "a"))));
    }
    @Test
    void acceptNegotiationMatchesExistingJsonReadRules() {
        for (String accept : List.of("*/*", "application/json", "application/*", "application/json;charset=UTF-8", "text/html, application/json", "application/json;q=0.5, */*;q=1")) {
            assertThat(OperationsActivationReadRequest.parse(new RawListRequest(Map.of(), List.of(accept))).limit()).isEqualTo(20);
        }
        for (String accept : List.of("", "text/html", "application/json;q=0", "application/json;q=0, */*;q=1", "application/json;charset=GBK", "application/json;bad=x", "application/json;q=2", "[")) invalid(() -> OperationsActivationReadRequest.parse(new RawListRequest(Map.of(), List.of(accept))));
        invalid(() -> OperationsActivationReadRequest.parse(new RawListRequest(Map.of(), null)));
        invalid(() -> OperationsActivationReadRequest.parse(new RawListRequest(Map.of(), Arrays.asList((String) null))));
    }
    @Test
    void rawAndParsedRequestsAreImmutableAndRedacted() {
        var values = new ArrayList<>(List.of("PRIVATE")); var query = new HashMap<String, List<String>>(); query.put("cursor", values);
        var accepts = new ArrayList<String>(); var raw = new RawListRequest(query, accepts);
        values.clear(); query.clear(); accepts.add("text/html");
        assertThat(raw.query().get("cursor")).containsExactly("PRIVATE"); assertThat(raw.accept()).isEmpty();
        assertThatThrownBy(() -> raw.query().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> raw.query().get("cursor").clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(raw.toString()).doesNotContain("PRIVATE"); assertThat(OperationsActivationReadRequest.parse(raw).toString()).doesNotContain("PRIVATE");
    }
}
