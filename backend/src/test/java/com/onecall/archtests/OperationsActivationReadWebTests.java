package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static com.onecall.archtests.operations.OperationsActivationReadTestProviders.*;
import com.onecall.archtests.operations.OperationsActivationReadTestProviders;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

class OperationsActivationReadWebTests {
    private MockHttpServletResponse response(OperationsActivationReadTestProviders fixture, MockHttpServletRequestBuilder request) throws Exception { return fixture.mvc.perform(request).andReturn().getResponse(); }
    private MockHttpServletRequestBuilder valid() { return get(BASE).servletPath(BASE).header("Authorization", "Bearer valid"); }
    private void problem(MockHttpServletResponse response, int status, String code) throws Exception {
        assertThat(response.getStatus()).isEqualTo(status); assertThat(response.getContentType()).startsWith("application/problem+json");
        var body = JsonMapper.builder().build().readTree(response.getContentAsString());
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId"); assertThat(body.path("code").asString()).isEqualTo(code); assertThat(body.path("status").asInt()).isEqualTo(status); UUID.fromString(body.path("traceId").asString());
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store"); assertThat(response.getHeaders("Set-Cookie")).isEmpty(); assertThat(response.getContentAsString()).doesNotContain("PRIVATE", "jdbc:", EMPLOYEE.toString(), "Bearer");
        if (status == 401) assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }
    @Test
    void exactGetReturnsOnlyFrozenPageAndElevenSummaryProperties() throws Exception {
        var fixture = new OperationsActivationReadTestProviders();
        var facts = new TransitionFacts(UUID.fromString("70000000-0000-0000-0000-000000000034"), UUID.fromString("80000000-0000-0000-0000-000000000034"), "projection-event-34", "INACTIVE", "ACTIVE", 17, 18, "IVR", "IVR_CONFIRM", "PROJECTION_REASON_34", TIME.minusSeconds(34));
        when(fixture.reader.fetch(any(), anyInt())).thenReturn(List.of(facts));
        var response = response(fixture, valid()); assertThat(response.getStatus()).isEqualTo(200);
        var body = JsonMapper.builder().build().readTree(response.getContentAsString()); assertThat(body.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
        assertThat(body.path("nextCursor").isNull()).isTrue(); assertThat(body.path("hasMore").asBoolean()).isFalse();
        var item = body.path("items").get(0); assertThat(item.propertyNames()).containsExactlyInAnyOrder("transitionId", "subjectId", "statusEventId", "fromStatus", "toStatus", "fromRevision", "toRevision", "sourceChannel", "sourceActionDomain", "reasonCode", "occurredAt");
        assertThat(body.path("items").size()).isEqualTo(1);
        assertThat(item.path("transitionId").asString()).isEqualTo(facts.transitionId().toString());
        assertThat(item.path("subjectId").asString()).isEqualTo(facts.subjectId().toString());
        assertThat(item.path("statusEventId").asString()).isEqualTo(facts.statusEventId());
        assertThat(item.path("fromStatus").asString()).isEqualTo(facts.fromStatus());
        assertThat(item.path("toStatus").asString()).isEqualTo(facts.toStatus());
        assertThat(item.path("fromRevision").isIntegralNumber()).isTrue(); assertThat(item.path("fromRevision").asInt()).isEqualTo(facts.fromRevision());
        assertThat(item.path("toRevision").isIntegralNumber()).isTrue(); assertThat(item.path("toRevision").asInt()).isEqualTo(facts.toRevision());
        assertThat(item.path("sourceChannel").asString()).isEqualTo(facts.sourceChannel());
        assertThat(item.path("sourceActionDomain").asString()).isEqualTo(facts.sourceActionDomain());
        assertThat(item.path("reasonCode").asString()).isEqualTo(facts.reasonCode());
        assertThat(item.path("occurredAt").asString()).isEqualTo(facts.occurredAt().toString());
    }
    @Test
    void otherMethodsAndNoncanonicalRoutesRemainConcealed() throws Exception {
        var fixture = new OperationsActivationReadTestProviders(); var service = mock(OperationsActivationReadService.class); fixture.beans.destroySingleton("service"); fixture.beans.registerSingleton("service", service);
        for (String method : List.of("POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE")) assertThat(response(fixture, request(method, java.net.URI.create(BASE)).servletPath(BASE)).getStatus()).isEqualTo(404);
        for (String path : List.of(BASE + "/", "/api//v1/operations/activations", "/api/v1//operations/activations", "/api/v1/operations//activations", BASE + ";x", BASE + "/x", "/api/v1/operations/%61ctivations", BASE + "%2f", BASE + "%2F", BASE + "%3b", BASE + "%25", "/%61pi/v1/operations/activations", "/api/v1/operations/employees")) assertThat(response(fixture, get(path).with(request -> { request.setServletPath(path); return request; })).getStatus()).isEqualTo(404);
        assertThat(response(fixture, get(BASE + "/hidden").servletPath(BASE)).getStatus()).isEqualTo(404); verifyNoInteractions(service, fixture.reader, fixture.sessions);
        var filter = new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(fixture.beans.getBeanProvider(EmployeeSessionVerifier.class)), new OperationsProblemWriter());
        for (String contextPath : List.of("", "/mounted")) for (String original : List.of(BASE, "/api/v1/customer/me", "/api/v1/operations/subjects", BASE + "/")) for (String method : List.of("TRACE", "GET")) {
            var request = new org.springframework.mock.web.MockHttpServletRequest(method, contextPath + "/custom-error");
            request.setContextPath(contextPath); request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR);
            request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI, contextPath + original);
            request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_METHOD, method);
            var response = new MockHttpServletResponse(); var chained = new java.util.concurrent.atomic.AtomicBoolean();
            filter.doFilter(request, response, (incoming, outgoing) -> { assertThat(incoming).isSameAs(request); assertThat(outgoing).isSameAs(response); chained.set(true); response.setStatus(500); response.setHeader("Unrelated-Error", "preserved"); response.getWriter().write("unchanged"); });
            boolean concealed = BASE.equals(original) && "TRACE".equals(method);
            assertThat(chained.get()).as(contextPath + original + " " + method).isEqualTo(!concealed);
            if (concealed) problem(response, 404, "NOT_FOUND");
            else { assertThat(response.getStatus()).isEqualTo(500); assertThat(response.getHeader("Unrelated-Error")).isEqualTo("preserved"); assertThat(response.getContentAsString()).isEqualTo("unchanged"); }
        }
        verifyNoInteractions(service, fixture.reader, fixture.sessions);
    }
    @Test
    void employeeAuthenticationPrecedesQueryValidation() throws Exception {
        var fixture = new OperationsActivationReadTestProviders();
        for (String header : List.of("", "Basic private", "Bearer customer", "Bearer wrong", "Bearer rejected")) problem(response(fixture, get(BASE).servletPath(BASE).header("Authorization", header).param("unknown", "x").header("Accept", "text/html")), 401, "UNAUTHENTICATED");
        problem(response(fixture, get(BASE).servletPath(BASE).param("unknown", "x")), 401, "UNAUTHENTICATED");
        problem(response(fixture, valid().header("Authorization", "Bearer valid")), 401, "UNAUTHENTICATED"); verifyNoInteractions(fixture.reader, fixture.dsl);
        var verifier = mock(EmployeeSessionVerifier.class); when(verifier.verify(any(), eq(EmployeeBearerAuthenticator.AUDIENCE))).thenReturn(new VerifiedEmployeeSession(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE)); fixture.beans.destroySingleton("verifier"); fixture.beans.registerSingleton("verifier", verifier);
        response(fixture, valid()); verify(verifier).verify(any(), eq("onecall:employee-session:v1"));
    }
    @Test
    void missingAmbiguousAndBrokenVerifiersFailClosed() throws Exception {
        for (String kind : List.of("missing", "ambiguous", "broken", "null", "unavailable")) {
            var fixture = new OperationsActivationReadTestProviders(); fixture.beans.destroySingleton("verifier");
            if ("ambiguous".equals(kind)) { fixture.beans.registerSingleton("first", fixture.sessions); fixture.beans.registerSingleton("second", mock(EmployeeSessionVerifier.class)); }
            if ("broken".equals(kind)) { var definition = new RootBeanDefinition(EmployeeSessionVerifier.class); definition.setInstanceSupplier(() -> { throw new IllegalStateException("PRIVATE"); }); fixture.beans.registerBeanDefinition("broken", definition); }
            if ("null".equals(kind) || "unavailable".equals(kind)) { var verifier = mock(EmployeeSessionVerifier.class); if ("unavailable".equals(kind)) when(verifier.verify(any(), anyString())).thenThrow(new IllegalStateException("PRIVATE")); fixture.beans.registerSingleton("verifier", verifier); }
            problem(response(fixture, valid().param("unknown", "x")), 503, "SERVICE_UNAVAILABLE"); verifyNoInteractions(fixture.dsl, fixture.reader);
        }
    }
    @Test
    void currentEmployeeAuthorityPrecedesActivationDependencies() throws Exception {
        var fixture = new OperationsActivationReadTestProviders(); fixture.beans.destroySingleton("properties"); fixture.beans.destroySingleton("reader"); when(fixture.employee.get("status", String.class)).thenReturn("DISABLED");
        problem(response(fixture, valid().param("unknown", "x")), 403, "FORBIDDEN"); verifyNoInteractions(fixture.reader);
        when(fixture.employee.get("status", String.class)).thenReturn("ACTIVE"); when(fixture.employee.get("role", String.class)).thenReturn("UNSUPPORTED"); problem(response(fixture, valid()), 503, "SERVICE_UNAVAILABLE");
        when(fixture.dsl.fetchOne(anyString(), any(Object[].class))).thenReturn(null); problem(response(fixture, valid()), 401, "UNAUTHENTICATED");
        fixture = new OperationsActivationReadTestProviders(); for (String role : List.of("ADMIN", "OPS")) { when(fixture.employee.get("role", String.class)).thenReturn(role); assertThat(response(fixture, valid()).getStatus()).isEqualTo(200); }
    }
    @Test
    void missingAmbiguousAndBrokenServicesFailClosed() throws Exception {
        for (int count : List.of(0, 2, 3)) {
            var fixture = new OperationsActivationReadTestProviders(); fixture.beans.destroySingleton("service");
            if (count == 2) { fixture.beans.registerSingleton("first", fixture.service); fixture.beans.registerSingleton("second", mock(OperationsActivationReadService.class)); }
            if (count == 3) { var definition = new RootBeanDefinition(OperationsActivationReadService.class); definition.setInstanceSupplier(() -> { throw new IllegalStateException("PRIVATE"); }); fixture.beans.registerBeanDefinition("broken", definition); }
            problem(response(fixture, valid()), 503, "SERVICE_UNAVAILABLE");
        }
    }
    @Test
    void queryShapeLimitsAndAcceptUseOnlyFrozenErrors() throws Exception {
        var fixture = new OperationsActivationReadTestProviders();
        for (String name : List.of("subjectId", "source", "claimStatus", "activationStatus", "realNameStatus", "riskState", "offset", "total", "search")) problem(response(fixture, valid().param(name, "x")), 422, "VALIDATION_FAILED");
        for (String limit : List.of("0", "101", "-1", "+1", "", "1.0", " 1", "0001")) problem(response(fixture, valid().param("limit", limit)), 422, "VALIDATION_FAILED");
        problem(response(fixture, valid().param("cursor", "a", "a")), 422, "VALIDATION_FAILED");
        for (String accept : List.of("text/html", "application/json;q=0", "application/json;charset=GBK", "[")) problem(response(fixture, valid().header("Accept", accept)), 422, "VALIDATION_FAILED");
        for (String limit : List.of("1", "20", "100", "001")) assertThat(response(fixture, valid().param("limit", limit)).getStatus()).isEqualTo(200);
    }
    @Test
    void cursorFailuresRemainSanitizedAndLimitIsUnbound() throws Exception {
        var fixture = new OperationsActivationReadTestProviders();
        for (String cursor : List.of("", "a".repeat(513), "bad")) problem(response(fixture, valid().param("cursor", cursor)), 400, "CURSOR_INVALID");
        var codec = new com.onecall.core.operations_iam.internal.activationread.OperationsActivationCursorCodec(properties()); String token = codec.encode(new Anchor(TIME, UUID.randomUUID()));
        for (String limit : List.of("1", "100")) assertThat(response(fixture, valid().param("cursor", token).param("limit", limit)).getStatus()).isEqualTo(200);
        fixture.beans.destroySingleton("properties"); problem(response(fixture, valid()), 503, "SERVICE_UNAVAILABLE");
    }
    @Test
    void problemsHaveExactSafeEnvelopeHeadersAndChallenge() throws Exception {
        var fixture = new OperationsActivationReadTestProviders(); var service = mock(OperationsActivationReadService.class); fixture.beans.destroySingleton("service"); fixture.beans.registerSingleton("service", service);
        for (var entry : Map.of(Code.CURSOR_INVALID, 400, Code.UNAUTHENTICATED, 401, Code.FORBIDDEN, 403, Code.VALIDATION_FAILED, 422, Code.RATE_LIMITED, 429, Code.SERVICE_UNAVAILABLE, 503, Code.INTERNAL, 500).entrySet()) {
            doThrow(new Failure(entry.getKey())).when(service).list(any(), any()); problem(response(fixture, valid()), entry.getValue(), entry.getKey().name());
        }
        doThrow(new Failure(Code.NOT_FOUND)).when(service).list(any(), any()); problem(response(fixture, valid()), 500, "INTERNAL");
    }
    @Test
    void customerAndOperationsFiltersAdmitOnlyExactActivationGet() throws Exception {
        var fixture = new OperationsActivationReadTestProviders(); assertThat(response(fixture, valid()).getStatus()).isEqualTo(200);
        problem(response(fixture, get(BASE).servletPath(BASE)), 401, "UNAUTHENTICATED");
        assertThat(response(fixture, get("/api/v1/customer/me").servletPath("/api/v1/customer/me").header("Authorization", "Bearer valid")).getStatus()).isEqualTo(404);
        assertThat(response(fixture, get("/api/v1/public/shares/x").servletPath("/api/v1/public/shares/x").header("Authorization", "Bearer valid")).getStatus()).isEqualTo(404);
    }
    @Test
    void subjectGetAndLifecyclePostAdmissionRemainUnchanged() throws Exception {
        var fixture = new OperationsActivationReadTestProviders();
        var problems = new OperationsProblemWriter();
        var reads = mock(OperationsSubjectReadService.class);
        when(reads.list(any(), any())).thenReturn(new SubjectPage(List.of(), null, false));
        fixture.beans.registerSingleton("subjectReads", reads);
        MockMvc mvc = fixture.mvc(new OperationsSubjectReadController(fixture.beans.getBeanProvider(OperationsSubjectReadService.class), problems), new OperationsSubjectsController(fixture.beans.getBeanProvider(com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.class), problems));
        assertThat(mvc.perform(get("/api/v1/operations/subjects").servletPath("/api/v1/operations/subjects").header("Authorization", "Bearer valid")).andReturn().getResponse().getStatus()).isEqualTo(200);
        for (String path : List.of("/api/v1/operations/subjects", "/api/v1/operations/subjects/" + UUID.randomUUID())) problem(response(fixture, get(path).servletPath(path)), 401, "UNAUTHENTICATED");
        for (String action : List.of("suspensions", "resumptions")) { String path = "/api/v1/operations/subjects/" + UUID.randomUUID() + "/" + action; problem(response(fixture, post(path).servletPath(path)), 401, "UNAUTHENTICATED"); assertThat(response(fixture, post(path).servletPath(path).header("Authorization", "Bearer valid")).getStatus()).isEqualTo(422); }
    }
    @Test
    void unknownAndPartialResponseFailuresDoNotLeak() throws Exception {
        var fixture = new OperationsActivationReadTestProviders(); var service = mock(OperationsActivationReadService.class); fixture.beans.destroySingleton("service"); fixture.beans.registerSingleton("service", service);
        when(service.list(any(), any())).thenThrow(new IllegalStateException("PRIVATE")); problem(response(fixture, valid()), 500, "INTERNAL");
        doReturn(null).when(service).list(any(), any()); problem(response(fixture, valid()), 500, "INTERNAL");
        doThrow(new Failure(Code.SERVICE_UNAVAILABLE)).when(service).list(any(), any()); var response = response(fixture, valid()); problem(response, 503, "SERVICE_UNAVAILABLE"); assertThat(response.getContentAsString()).doesNotContain("items", "nextCursor");
        var item = mock(OperationsActivationReadService.ActivationSummary.class);
        when(item.occurredAt()).thenThrow(new IllegalStateException("PRIVATE_SERIALIZATION"));
        doReturn(new OperationsActivationReadService.ActivationPage(List.of(item), "PRIVATE_CURSOR", true)).when(service).list(any(), any());
        var serialization = response(fixture, valid()); problem(serialization, 500, "INTERNAL"); assertThat(serialization.getContentAsString()).doesNotContain("items", "nextCursor", "PRIVATE_CURSOR");
    }
}
