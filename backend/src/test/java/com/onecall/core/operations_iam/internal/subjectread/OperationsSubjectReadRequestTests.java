package com.onecall.core.operations_iam.internal.subjectread;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.RawDetailRequest;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.RawListRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperationsSubjectReadRequestTests {
    private static final String SUBJECT = "abcdef01-2345-6789-abcd-0123456789ab";

    @Test
    void listAllowsExactlyEightQueryKeys() {
        var query = new LinkedHashMap<String, List<String>>();
        query.put("cursor", List.of("opaque"));
        query.put("limit", List.of("100"));
        query.put("subjectId", List.of(SUBJECT));
        query.put("source", List.of("OPERATIONS"));
        query.put("claimStatus", List.of("CONFLICT"));
        query.put("activationStatus", List.of("SUSPENDED"));
        query.put("realNameStatus", List.of("VERIFIED"));
        query.put("riskState", List.of("OPEN"));
        var parsed = list(query);
        assertThat(parsed.limit()).isEqualTo(100);
        assertThat(parsed.cursor()).isEqualTo("opaque");
        assertThat(parsed.filters().subjectId()).isEqualTo(UUID.fromString(SUBJECT));
        assertThat(parsed.filters().source()).isEqualTo("OPERATIONS");
        assertThat(parsed.filters().claimStatus()).isEqualTo("CONFLICT");
        assertThat(parsed.filters().activationStatus()).isEqualTo("SUSPENDED");
        assertThat(parsed.filters().realNameStatus()).isEqualTo("VERIFIED");
        assertThat(parsed.filters().riskState()).isEqualTo("OPEN");
        for (String key : List.of("q", "search", "number", "offset", "employeeId", "role", "organizationId", "Source")) {
            invalid(() -> list(Map.of(key, List.of("canary"))));
        }
    }

    @Test
    void duplicatesIncludingIdenticalCursorValuesAreRejectedFirst() {
        for (String key : List.of("cursor", "limit", "subjectId", "source", "claimStatus", "activationStatus", "realNameStatus", "riskState")) {
            invalid(() -> list(Map.of(key, List.of("same", "same"))));
            invalid(() -> list(Map.of(key, List.of("first", "second"))));
        }
        var query = new LinkedHashMap<String, List<String>>();
        query.put("cursor", List.of("bad", "bad"));
        query.put("subjectId", List.of("bad"));
        invalid(() -> list(query));
        invalid(() -> list(Map.of("limit", List.of())));
        invalid(() -> list(Map.of("cursor", Arrays.asList((String) null))));
    }

    @Test
    void canonicalLowercaseUuidRulesApplyToPathAndFilter() {
        for (String valid : List.of(SUBJECT, "00000000-0000-0000-0000-000000000000")) {
            assertThat(list(Map.of("subjectId", List.of(valid))).filters().subjectId()).isEqualTo(UUID.fromString(valid));
            assertThat(OperationsSubjectReadRequest.detail(new RawDetailRequest(valid, Map.of(), List.of()))).isEqualTo(UUID.fromString(valid));
        }
        for (String value : Arrays.asList(null, "", SUBJECT.toUpperCase(), "1-1-1-1-1", "{" + SUBJECT + "}", " " + SUBJECT,
                SUBJECT + " ", SUBJECT.replace("-", ""), "%61" + SUBJECT.substring(1), SUBJECT + ".json")) {
            invalid(() -> OperationsSubjectReadRequest.detail(new RawDetailRequest(value, Map.of(), List.of())));
            invalid(() -> list(Map.of("subjectId", Arrays.asList(value))));
        }
    }

    @Test
    void enumValuesAreExactWithoutTrimmingOrCaseFolding() {
        var values = Map.of("source", List.of("IVR", "MINIAPP", "WEB", "OPERATIONS"),
                "claimStatus", List.of("ACCEPTED", "REJECTED", "CONFLICT"),
                "activationStatus", List.of("INACTIVE", "ACTIVE", "SUSPENDED"),
                "realNameStatus", List.of("UNVERIFIED", "VERIFIED"), "riskState", List.of("OPEN", "CLEARED"));
        values.forEach((key, allowed) -> {
            for (String value : allowed) {
                assertThatCode(() -> list(Map.of(key, List.of(value)))).doesNotThrowAnyException();
                for (String invalid : List.of(value.toLowerCase(java.util.Locale.ROOT), " " + value, value + " ", value + ",")) {
                    invalid(() -> list(Map.of(key, List.of(invalid))));
                }
            }
            invalid(() -> list(Map.of(key, List.of("UNKNOWN"))));
        });
    }

    @Test
    void limitDefaultRangeAndDecimalGrammarAreExact() {
        assertThat(list(Map.of()).limit()).isEqualTo(20);
        for (int limit = 1; limit <= 100; limit++) {
            assertThat(list(Map.of("limit", List.of(Integer.toString(limit)))).limit()).isEqualTo(limit);
        }
        assertThat(list(Map.of("limit", List.of("020"))).limit()).isEqualTo(20);
        assertThat(list(Map.of("limit", List.of("001"))).limit()).isEqualTo(1);
        for (String value : List.of("0", "000", "101", "1000", "999999999999", "+1", "-1", " 1", "1 ", "1.0", "1e1", "１", "١", "")) {
            invalid(() -> list(Map.of("limit", List.of(value))));
        }
    }

    @Test
    void emptyAndUnknownNoncursorInputsAreValidationFailures() {
        for (String key : List.of("limit", "subjectId", "source", "claimStatus", "activationStatus", "realNameStatus", "riskState")) {
            invalid(() -> list(Map.of(key, List.of(""))));
        }
        invalid(() -> OperationsSubjectReadRequest.list(null));
        invalid(() -> OperationsSubjectReadRequest.list(new RawListRequest(null, List.of())));
        invalid(() -> OperationsSubjectReadRequest.detail(null));
        var query = new LinkedHashMap<String, List<String>>();
        query.put("limit", null);
        invalid(() -> list(query));
        query.clear();
        query.put(null, List.of("value"));
        invalid(() -> list(query));
    }

    @Test
    void singleMalformedCursorIsDeferredUntilAfterNoncursorValidation() {
        for (String cursor : List.of("", "!not-a-token", "a".repeat(513))) {
            assertThat(list(Map.of("cursor", List.of(cursor))).cursor()).isEqualTo(cursor);
            invalid(() -> list(Map.of("cursor", List.of(cursor), "limit", List.of("0"))));
            invalid(() -> list(Map.of("cursor", List.of(cursor), "source", List.of("unknown"))));
        }
    }

    @Test
    void filterDigestHasExactOrderedAbsentSentinelEncoding() throws Exception {
        String empty = "onecall:operations-subject-filters:v1\nsubjectId=~\nsource=~\nclaimStatus=~\nactivationStatus=~\nrealNameStatus=~\nriskState=~\n";
        assertThat(list(Map.of()).digest()).containsExactly(sha256(empty));
        var query = Map.of("subjectId", List.of(SUBJECT), "source", List.of("WEB"), "claimStatus", List.of("ACCEPTED"),
                "activationStatus", List.of("ACTIVE"), "realNameStatus", List.of("VERIFIED"), "riskState", List.of("CLEARED"));
        String all = "onecall:operations-subject-filters:v1\nsubjectId=" + SUBJECT
                + "\nsource=WEB\nclaimStatus=ACCEPTED\nactivationStatus=ACTIVE\nrealNameStatus=VERIFIED\nriskState=CLEARED\n";
        assertThat(list(query).digest()).containsExactly(sha256(all));
        for (String key : query.keySet()) {
            var reduced = new LinkedHashMap<>(query);
            reduced.remove(key);
            assertThat(list(reduced).digest()).isNotEqualTo(list(query).digest());
        }
    }

    @Test
    void parameterOrderAndPageSizeNeverChangeNormalizedFilterDigest() {
        var first = new LinkedHashMap<String, List<String>>();
        first.put("source", List.of("IVR"));
        first.put("riskState", List.of("OPEN"));
        var second = new LinkedHashMap<String, List<String>>();
        second.put("riskState", List.of("OPEN"));
        second.put("source", List.of("IVR"));
        second.put("cursor", List.of("opaque"));
        for (String limit : List.of("1", "020", "100")) {
            second.put("limit", List.of(limit));
            assertThat(OperationsSubjectReadRequest.list(new RawListRequest(second, List.of("application/*"))).digest())
                    .containsExactly(list(first).digest());
        }
        var values = new ArrayList<>(List.of("IVR"));
        var mutable = new LinkedHashMap<String, List<String>>();
        mutable.put("source", values);
        var raw = new RawListRequest(mutable, List.of());
        values.set(0, "WEB");
        mutable.clear();
        assertThat(OperationsSubjectReadRequest.list(raw).filters().source()).isEqualTo("IVR");
    }

    @Test
    void detailQueriesAndIncompatibleOrMalformedAcceptAreRejected() {
        for (String key : List.of("cursor", "limit", "subjectId", "source", "claimStatus", "activationStatus", "realNameStatus", "riskState", "q")) {
            invalid(() -> OperationsSubjectReadRequest.detail(new RawDetailRequest(SUBJECT, Map.of(key, List.of("x")), List.of())));
        }
        for (List<String> accept : List.of(List.<String>of(), List.of("application/json"), List.of("application/*"), List.of("*/*"),
                List.of("application/json;charset=UTF-8"), List.of("text/html", "application/json"), List.of("application/json;q=0.5, */*;q=1"))) {
            assertThatCode(() -> OperationsSubjectReadRequest.list(new RawListRequest(Map.of(), accept))).doesNotThrowAnyException();
            assertThat(OperationsSubjectReadRequest.detail(new RawDetailRequest(SUBJECT, Map.of(), accept))).isEqualTo(UUID.fromString(SUBJECT));
        }
        for (List<String> accept : Arrays.asList(null, Arrays.asList((String) null), List.of(""), List.of("text/html"),
                List.of("application/json;q=0"), List.of("application/json;q=broken"), List.of("application/json;q=0, */*;q=1"),
                List.of("application/json;q=0", "*/*;q=1"), List.of("application/json;charset=ISO-8859-1"), List.of("not-a-media-type"))) {
            invalid(() -> OperationsSubjectReadRequest.list(new RawListRequest(Map.of(), accept)));
            invalid(() -> OperationsSubjectReadRequest.detail(new RawDetailRequest(SUBJECT, Map.of(), accept)));
        }
    }

    private static OperationsSubjectReadRequest list(Map<String, List<String>> query) {
        return OperationsSubjectReadRequest.list(new RawListRequest(query, List.of()));
    }

    private static byte[] sha256(String text) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void invalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(Failure.class, failure -> {
            assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED);
            assertThat(failure.getMessage()).isEqualTo("Operations read failed");
            assertThat(failure.getCause()).isNull();
        });
    }
}
