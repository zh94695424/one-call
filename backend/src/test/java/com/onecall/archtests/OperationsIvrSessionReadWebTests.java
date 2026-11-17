package com.onecall.archtests;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.internal.security.*;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;


class OperationsIvrSessionReadWebTests {
    private MockHttpServletRequestBuilder valid() { return get(BASE).servletPath(BASE).header("Authorization", "Bearer valid"); }
    private MockHttpServletRequestBuilder detail() { String path = BASE + "/" + fact(1).sessionId(); return get(path).servletPath(path).header("Authorization", "Bearer valid"); }
    private MockHttpServletResponse response(OperationsIvrSessionReadTestProviders fixture, MockHttpServletRequestBuilder request) throws Exception { return fixture.mvc.perform(request).andReturn().getResponse(); }
    private void problem(MockHttpServletResponse response, Code code) throws Exception {
        int status = switch (code) { case CURSOR_INVALID, CURSOR_FILTER_MISMATCH -> 400; case UNAUTHENTICATED -> 401; case FORBIDDEN -> 403; case NOT_FOUND -> 404; case VALIDATION_FAILED -> 422; case RATE_LIMITED -> 429; case SERVICE_UNAVAILABLE -> 503; case INTERNAL -> 500; };
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        var body = JsonMapper.builder().build().readTree(response.getContentAsString());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(body.path("code").asString()).isEqualTo(code.name()); assertThat(body.path("status").asInt()).isEqualTo(status);
        UUID.fromString(body.path("traceId").asString()); assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentAsString()).doesNotContain("PRIVATE", "jdbc:", EMPLOYEE.toString(), "Bearer");
        if (status == 401) assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }
    @Test
    void exactListGetReturnsOnlyFrozenPageAndSummary() throws Exception {
        var response = response(new OperationsIvrSessionReadTestProviders(), valid()); assertThat(response.getStatus()).isEqualTo(200); var body = JsonMapper.builder().build().readTree(response.getContentAsString()); assertThat(body.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore"); assertThat(body.path("items").get(0).propertyNames()).containsExactlyInAnyOrder("sessionId", "callId", "status", "nodeId", "protocolVersion", "promptVersion", "accessNumberMasked", "createdAt");
    }
    @Test
    void exactDetailGetReturnsOnlyFrozenSafeChain() throws Exception {
        var response = response(new OperationsIvrSessionReadTestProviders(), detail()); assertThat(response.getStatus()).isEqualTo(200); var body = JsonMapper.builder().build().readTree(response.getContentAsString()); assertThat(body.propertyNames()).containsExactlyInAnyOrder("sessionId", "callId", "status", "nodeId", "protocolVersion", "promptVersion", "accessNumberMasked", "createdAt", "call", "decisions", "legs", "callEvents", "evidence"); assertThat(body.path("call").propertyNames()).containsExactly("status");
    }
    @Test
    void wrongMethodsAndUnknownOperationsPathsStayConcealed() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); for (String path : List.of(BASE, BASE + "/" + EMPLOYEE)) for (String method : List.of("POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE")) problem(response(fixture, request(method, java.net.URI.create(path)).servletPath(path)), Code.NOT_FOUND);
        problem(response(fixture, get("/api/v1/operations/employees").servletPath("/api/v1/operations/employees")), Code.NOT_FOUND);
    }
    @Test
    void encodedSemicolonDoubleSlashAndExtraSegmentsStayConcealed() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); for (String path : List.of(BASE + ";x", BASE + "/", BASE + "/x/y", BASE + "/x;y", BASE + "/%31", BASE + "%2F", "/api//v1/operations/ivr-sessions")) problem(response(fixture, get(java.net.URI.create(path)).with(request -> { request.setServletPath(path.replace("%31", "1")); return request; })), Code.NOT_FOUND);
    }
    @Test
    void authenticationPrecedesMalformedQueryAndPath() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); problem(response(fixture, get(BASE).servletPath(BASE).param("limit", "bad")), Code.UNAUTHENTICATED); String path = BASE + "/bad"; problem(response(fixture, get(path).servletPath(path)), Code.UNAUTHENTICATED); verifyNoInteractions(fixture.reader, fixture.evidence);
    }
    @Test
    void currentAuthorityPrecedesOwnerDependencies() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); fixture.beans.destroySingleton("reader"); when(fixture.employee.get("status", String.class)).thenReturn("DISABLED"); problem(response(fixture, valid().param("limit", "bad")), Code.FORBIDDEN); when(fixture.employee.get("role", String.class)).thenReturn("BAD"); problem(response(fixture, valid()), Code.SERVICE_UNAVAILABLE);
    }
    @Test
    void missingAmbiguousAndBrokenVerifiersFailClosed() throws Exception {
        for (int mode = 0; mode < 3; mode++) { var fixture = new OperationsIvrSessionReadTestProviders(); if (mode == 0) fixture.beans.destroySingleton("verifier"); if (mode == 1) fixture.beans.registerSingleton("otherVerifier", mock(EmployeeSessionVerifier.class)); if (mode == 2) doThrow(new IllegalStateException("PRIVATE")).when(fixture.sessions).verify(any(), anyString()); problem(response(fixture, valid()), Code.SERVICE_UNAVAILABLE); }
    }
    @Test
    void missingAmbiguousAndBrokenServicesFailClosed() throws Exception {
        for (int mode = 0; mode < 3; mode++) { var fixture = new OperationsIvrSessionReadTestProviders(); if (mode == 0) fixture.beans.destroySingleton("service"); if (mode == 1) fixture.beans.registerSingleton("otherService", mock(OperationsIvrSessionReadService.class)); if (mode == 2) { fixture.beans.destroySingleton("service"); var definition = new org.springframework.beans.factory.support.RootBeanDefinition(OperationsIvrSessionReadService.class, () -> { throw new IllegalStateException("PRIVATE"); }); fixture.beans.registerBeanDefinition("service", definition); } problem(response(fixture, valid()), Code.SERVICE_UNAVAILABLE); }
    }
    @Test
    void duplicateUnknownMalformedParametersAndPathAre422() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); problem(response(fixture, valid().param("limit", "1", "1")), Code.VALIDATION_FAILED); problem(response(fixture, valid().param("unknown", "x")), Code.VALIDATION_FAILED); problem(response(fixture, valid().param("subjectId", "bad")), Code.VALIDATION_FAILED); problem(response(fixture, get(BASE + "/bad").servletPath(BASE + "/bad").header("Authorization", "Bearer valid")), Code.VALIDATION_FAILED); problem(response(fixture, detail().param("limit", "1")), Code.VALIDATION_FAILED);
    }
    @Test
    void malformedTamperedAndFilterMismatchCursorsAre400() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); for (String token : List.of("!", "", "x".repeat(513))) problem(response(fixture, valid().param("cursor", token)), Code.CURSOR_INVALID);
        byte[] digest = OperationsIvrSessionReadRequest.parse(raw(Map.of())).digest(); String cursor = new OperationsIvrSessionCursorCodec(properties()).encode(new Anchor(TIME, EMPLOYEE), digest);
        problem(response(fixture, valid().param("cursor", cursor).param("status", "REJECTED")), Code.CURSOR_FILTER_MISMATCH);
    }
    @Test
    void problemsHaveExactSafeEnvelopeHeadersAndChallenge() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); for (Code code : Code.values()) { doThrow(new Failure(code)).when(fixture.reader).fetch(any(), any(), anyInt()); problem(response(fixture, valid()), code); }
    }
    @Test
    void optionalFieldsAreOmittedAndRequiredNullsRemainPresent() throws Exception {
        var invalid = new ArrayList<IvrCallSummary>();
        for (String status : List.of("ACTIVE", "UNKNOWN")) {
            invalid.add(new IvrCallSummary(status, TIME, TIME.plusSeconds(1), null));
            invalid.add(new IvrCallSummary(status, TIME, null, 1));
        }
        for (String status : List.of("ENDED", "FAILED", "REJECTED")) invalid.add(new IvrCallSummary(status, TIME, null, null));
        invalid.add(new IvrCallSummary("ENDED", TIME, TIME.minusSeconds(1), null));
        for (var call : invalid) {
            var provider = new OperationsIvrSessionReadTestProviders();
            var root = fact(1);
            when(provider.reader.detail(any())).thenReturn(Optional.of(new DetailFacts(root, List.of(new CallFacts(root.sessionId(), root.callId(), call)), List.of(), List.of(), List.of())));
            problem(response(provider, detail()), Code.SERVICE_UNAVAILABLE);
            verifyNoInteractions(provider.evidence);
        }
        var fixture = new OperationsIvrSessionReadTestProviders(); var root = fact(1); var decision = new IvrDecisionSummary(EMPLOYEE, "DIRECT_AUTHORIZATION", null, "DIRECT_AUTHORIZED", EMPLOYEE, TIME); when(fixture.reader.detail(any())).thenReturn(Optional.of(new DetailFacts(root, detailFact(root).calls(), List.of(new DecisionFacts(root.sessionId(), root.callId(), decision)), List.of(), List.of())));
        var body = JsonMapper.builder().build().readTree(response(fixture, detail()).getContentAsString()); assertThat(body.path("call").has("endedAt")).isFalse(); assertThat(body.path("decisions").get(0).path("action").isNull()).isTrue(); assertThat(body.path("decisions").get(0).has("action")).isTrue(); assertThat(JsonMapper.builder().build().readTree(response(fixture, valid()).getContentAsString()).path("nextCursor").isNull()).isTrue();
    }
    @Test
    void noCallerRoleWorkspaceBodyOrHeaderBecomesAuthority() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); problem(response(fixture, valid().param("role", "ADMIN")), Code.VALIDATION_FAILED); assertThat(response(fixture, valid().header("X-Role", "ADMIN").header("X-Workspace", "other").content("PRIVATE")).getStatus()).isEqualTo(200); verify(fixture.employee, times(2)).get("role", String.class);
    }
    @Test
    void customerAndOperationsFiltersAdmitOnlyExactTwoGets() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); assertThat(response(fixture, valid()).getStatus()).isEqualTo(200); assertThat(response(fixture, detail()).getStatus()).isEqualTo(200); verify(fixture.reader).fetch(any(), any(), eq(21)); verify(fixture.reader).detail(fact(1).sessionId());
    }
    @Test
    void existingSubjectActivationAndLifecycleRoutesRemainUnchanged() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); for (String path : List.of("/api/v1/operations/subjects", "/api/v1/operations/activations", "/api/v1/operations/subjects/" + EMPLOYEE)) problem(response(fixture, get(path).servletPath(path)), Code.UNAUTHENTICATED);
    }
    @Test
    void partialAndUnknownFailuresNeverLeakSensitiveCanaries() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); doThrow(new IllegalStateException("PRIVATE jdbc:")).when(fixture.reader).detail(any()); problem(response(fixture, detail()), Code.INTERNAL); doThrow(new org.jooq.exception.DataAccessException("PRIVATE jdbc:")).when(fixture.reader).detail(any()); problem(response(fixture, detail()), Code.SERVICE_UNAVAILABLE);
    }
}
