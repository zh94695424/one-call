package com.onecall.core.operations_iam.internal.ivrread;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders.*;
import com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders;
import com.onecall.core.operations_iam.internal.ivrread.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;


class OperationsIvrSessionReadRequestTests {
    private OperationsIvrSessionReadRequest parse(Map<String, List<String>> query) { return OperationsIvrSessionReadRequest.parse(raw(query)); }
    private void invalid(Map<String, List<String>> query) { assertThatThrownBy(() -> parse(query)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED)); }
    @Test
    void listAllowsExactlySixQueryKeys() throws Exception {
        var request = parse(Map.of("cursor", List.of("opaque"), "limit", List.of("2"), "subjectId", List.of(EMPLOYEE.toString()), "callId", List.of("call"), "taskId", List.of("task"), "status", List.of("CONFIRMED")));
        assertThat(request.filters()).isEqualTo(new Filters(EMPLOYEE, "call", "task", "CONFIRMED")); assertThat(request.limit()).isEqualTo(2);
    }
    @Test
    void duplicateUnknownAndNullParametersAreValidationFailed() throws Exception {
        for (var query : List.of(Map.of("unknown", List.of("x")), Map.of("limit", List.of("1", "1")), Map.of("limit", List.<String>of()))) invalid(query);
        invalid(null); var query = new HashMap<String, List<String>>(); query.put(null, List.of("x")); invalid(query); query.clear(); query.put("limit", null); invalid(query); query.put("limit", Arrays.asList((String) null)); invalid(query);
    }
    @Test
    void absentLimitDefaultsToTwenty() throws Exception {
        assertThat(parse(Map.of()).limit()).isEqualTo(20);
    }
    @Test
    void limitGrammarAndOneThroughOneHundredBoundsAreExact() throws Exception {
        for (String valid : List.of("1", "100", "001", "020")) assertThat(parse(Map.of("limit", List.of(valid))).limit()).isEqualTo(Integer.parseInt(valid));
        for (String bad : List.of("", "0", "101", "1000", "-1", "+1", "1.0", "1e1", " 1", "１")) invalid(Map.of("limit", List.of(bad)));
    }
    @Test
    void subjectAndPathUuidsAreCanonicalLowercase() throws Exception {
        for (String id : List.of(EMPLOYEE.toString(), "abcdefab-abcd-abcd-abcd-abcdefabcdef")) {
        assertThat(parse(Map.of("subjectId", List.of(id))).filters().subjectId().toString()).isEqualTo(id);
        assertThat(OperationsIvrSessionReadRequest.parseDetail(new RawDetailRequest(id, Map.of(), List.of())).toString()).isEqualTo(id); }
        for (String id : List.of("ABCDEFAB-ABCD-ABCD-ABCD-ABCDEFABCDEF", "1-1-1-1-1", "bad", "")) {
        invalid(Map.of("subjectId", List.of(id))); assertThatThrownBy(() -> OperationsIvrSessionReadRequest.parseDetail(new RawDetailRequest(id, Map.of(), List.of()))).isInstanceOf(Failure.class); }
    }
    @Test
    void callAndTaskIdsUseExactCodePointBoundsWithoutTrimming() throws Exception {
        for (String key : List.of("callId", "taskId")) { int max = key.equals("callId") ? 128 : 64;
        String value = "😀".repeat(max); assertThat(parse(Map.of(key, List.of(value))).filters().toString()).isEqualTo("Filters[REDACTED]");
        invalid(Map.of(key, List.of(value + "x"))); invalid(Map.of(key, List.of(" "))); invalid(Map.of(key, List.of("")));
        var request = parse(Map.of(key, List.of(" A "))); assertThat(key.equals("callId") ? request.filters().callId() : request.filters().taskId()).isEqualTo(" A "); }
    }
    @Test
    void sessionStatusesAreExactAndCaseSensitive() throws Exception {
        for (String value : List.of("RECEIVED", "CONFIRMED", "ABANDONED", "REJECTED")) assertThat(parse(Map.of("status", List.of(value))).filters().status()).isEqualTo(value);
        for (String value : List.of("received", "ACTIVE", "RECEIVED ", "")) invalid(Map.of("status", List.of(value)));
    }
    @Test
    void detailRejectsEveryQueryParameter() throws Exception {
        for (String key : List.of("cursor", "limit", "subjectId", "callId", "taskId", "status", "unknown")) assertThatThrownBy(() -> OperationsIvrSessionReadRequest.parseDetail(new RawDetailRequest(EMPLOYEE.toString(), Map.of(key, List.of("x")), List.of()))).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED));
    }
    @Test
    void singleMalformedCursorIsDeferredToCodec() throws Exception {
        for (String cursor : List.of("", "!", "x".repeat(513))) assertThat(parse(Map.of("cursor", List.of(cursor))).cursor()).isEqualTo(cursor);
    }
    @Test
    void acceptNegotiationUsesExistingJsonReadRules() throws Exception {
        for (String accept : List.of("application/json", "*/*", "application/*", "application/json;charset=UTF-8", "text/plain, application/json;q=0.5")) assertThat(OperationsIvrSessionReadRequest.parse(new RawListRequest(Map.of(), List.of(accept)))).isNotNull();
        for (String accept : List.of("", "text/plain", "application/json;q=0", "application/json;extra=no", "application/json;charset=GBK")) assertThatThrownBy(() -> OperationsIvrSessionReadRequest.parse(new RawListRequest(Map.of(), List.of(accept)))).isInstanceOf(Failure.class);
    }
    @Test
    void presenceAwareDigestHasFixedFieldOrderAndUnambiguousLengths() throws Exception {
        var buffer = new java.io.ByteArrayOutputStream(); buffer.writeBytes("onecall:operations-ivr-session-filters:v1\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for (String key : List.of("subjectId", "callId", "taskId", "status")) { buffer.writeBytes(key.getBytes(java.nio.charset.StandardCharsets.US_ASCII)); buffer.write(0); buffer.writeBytes(new byte[4]); }
        assertThat(parse(Map.of()).digest()).isEqualTo(java.security.MessageDigest.getInstance("SHA-256").digest(buffer.toByteArray()));
        assertThat(parse(Map.of("callId", List.of("a"), "taskId", List.of("bc"))).digest()).isNotEqualTo(parse(Map.of("callId", List.of("ab"), "taskId", List.of("c"))).digest());
    }
    @Test
    void queryOrderAndLimitDoNotChangeDigest() throws Exception {
        var first = new LinkedHashMap<String, List<String>>(); first.put("callId", List.of("a")); first.put("taskId", List.of("b"));
        var second = new LinkedHashMap<String, List<String>>(); second.put("taskId", List.of("b")); second.put("limit", List.of("100")); second.put("callId", List.of("a"));
        assertThat(parse(first).digest()).isEqualTo(parse(second).digest());
    }
}
