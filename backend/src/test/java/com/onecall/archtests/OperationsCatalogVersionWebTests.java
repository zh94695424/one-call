package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand.*;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

public final class OperationsCatalogVersionWebTests {
    static final UUID VERSION = new UUID(0, 38), PRODUCT = new UUID(1, 38), EMPLOYEE = new UUID(2, 38);
    static final String BASE = "/api/v1/operations/catalog/versions/" + VERSION;
    final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    final SyntheticEmployeeSessions sessions = new SyntheticEmployeeSessions();
    final OperationsCatalogSaleCommand command = mock(OperationsCatalogSaleCommand.class);
    final AtomicInteger customerCalls = new AtomicInteger();
    final JsonMapper mapper = JsonMapper.builder().build();
    final CatalogVersionState state = new CatalogVersionState(VERSION, PRODUCT, 1, "ACTIVE", 2, Instant.parse("2026-11-18T00:00:00Z"), VERSION);
    MockMvc mvc;
    ServletInputStream bodyStream;
    final AtomicInteger bodyReads = new AtomicInteger(), bodyBytes = new AtomicInteger();
    @BeforeEach void setup() {
        sessions.sessions.put("valid", new VerifiedEmployeeSession(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("wrong", new VerifiedEmployeeSession(EMPLOYEE, "customer"));
        beans.registerSingleton("verifier", sessions); beans.registerSingleton("command", command);
        when(command.publish(any())).thenReturn(state); when(command.offsale(any())).thenReturn(state);
        mvc = mvc();
    }
    MockMvc mvc() {
        var problems = new OperationsProblemWriter();
        return MockMvcBuilders.standaloneSetup(new OperationsCatalogVersionController(beans.getBeanProvider(OperationsCatalogSaleCommand.class), problems))
                .addFilters((request, response, chain) -> chain.doFilter(bodyStream == null ? request : new HttpServletRequestWrapper((HttpServletRequest) request) {
                    @Override public ServletInputStream getInputStream() { bodyReads.incrementAndGet(); return bodyStream; }
                }, response), new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(beans.getBeanProvider(EmployeeSessionVerifier.class)), problems),
                        new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter())).build();
    }
    String body(String suffix) { return suffix.equals("publications") ? "{\"expectedActiveVersionId\":null,\"expectedRevision\":1}" : "{\"expectedRevision\":1}"; }
    MockHttpServletRequestBuilder request(String suffix) { return post(BASE + "/" + suffix).servletPath(BASE + "/" + suffix).header("Authorization", "Bearer valid").contentType("application/json").accept("application/json").content(body(suffix)); }
    MockHttpServletRequestBuilder streamed(String suffix, byte[] content, int failureAfter) {
        bodyReads.set(0); bodyBytes.set(0);
        bodyStream = new ServletInputStream() {
            @Override public int read() throws IOException {
                if (failureAfter == 0) throw new AssertionError("body read before authentication");
                if (failureAfter > 0 && bodyBytes.get() >= failureAfter) throw new IOException("PRIVATE truncated jdbc: password");
                if (bodyBytes.get() == content.length) return -1;
                return Byte.toUnsignedInt(content[bodyBytes.getAndIncrement()]);
            }
            @Override public int read(byte[] target, int offset, int length) throws IOException {
                if (length == 0) return 0;
                int first = read();
                if (first < 0) return -1;
                target[offset] = (byte) first;
                int available = Math.min(length - 1, content.length - bodyBytes.get());
                if (failureAfter > 0) available = Math.min(available, failureAfter - bodyBytes.get());
                System.arraycopy(content, bodyBytes.get(), target, offset + 1, available);
                bodyBytes.addAndGet(available);
                return available + 1;
            }
            @Override public boolean isFinished() { return bodyBytes.get() == content.length; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
        };
        return request(suffix).content(content);
    }
    byte[] padded(String suffix, int size) { return (body(suffix) + " ".repeat(size - body(suffix).length())).getBytes(StandardCharsets.UTF_8); }
    void headers(MockHttpServletResponse response) {
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store"); assertThat(response.getHeader("Pragma")).isEqualTo("no-cache"); assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff"); assertThat(response.getContentLength()).isEqualTo(response.getContentAsByteArray().length);
    }
    void problem(MvcResult result, int status, String code) throws Exception {
        var response = result.getResponse(); assertThat(response.getStatus()).isEqualTo(status); headers(response);
        var json = mapper.readTree(response.getContentAsByteArray()); assertThat(json.size()).isEqualTo(5); assertThat(json.get("type").asString()).isEqualTo("about:blank"); assertThat(json.get("status").asInt()).isEqualTo(status); assertThat(json.get("code").asString()).isEqualTo(code);
        assertThat(json.get("traceId").asString()).isEqualTo(trace(result).toString());
        assertThat(response.getContentType()).isEqualTo("application/problem+json"); assertThat(response.getHeader("WWW-Authenticate")).isEqualTo(status == 401 ? "Bearer" : null);
        assertThat(response.getContentAsString()).doesNotContain("PRIVATE", "catalog_order_role", "jdbc:", "snapshot", "password", "SELECT "); assertThat(customerCalls.get()).isZero();
    }
    void admitted(String suffix) throws Exception {
        for (boolean withCustomer : List.of(false, true)) {
            if (withCustomer) beans.registerSingleton("customer", (CustomerSessionVerifier) (bearer, audience) -> { customerCalls.incrementAndGet(); throw new AssertionError("customer auth invoked"); });
            for (String context : List.of("", "/ctx")) {
                var result = mvc.perform(post(context + BASE + "/" + suffix).contextPath(context).servletPath(BASE + "/" + suffix).header("Authorization", "Bearer valid").contentType("application/json").accept("application/json").content(body(suffix))).andReturn();
                assertThat(result.getResponse().getStatus()).isEqualTo(200); headers(result.getResponse());
                UUID trace = trace(result);
                if (suffix.equals("publications")) verify(command, atLeastOnce()).publish(argThat(value -> value.caller().employeeId().equals(EMPLOYEE) && value.caller().traceId().equals(trace)));
                else verify(command, atLeastOnce()).offsale(argThat(value -> value.caller().employeeId().equals(EMPLOYEE) && value.caller().traceId().equals(trace)));
            }
        }
        assertThat(customerCalls.get()).isZero();
        for (boolean declaredLength : List.of(true, false)) {
            var request = streamed(suffix, padded(suffix, 8192), -1);
            if (!declaredLength) request.with(value -> { value.setContent(null); return value; });
            var result = mvc.perform(request).andReturn();
            assertThat(result.getResponse().getStatus()).isEqualTo(200); headers(result.getResponse());
            assertThat(bodyReads.get()).isEqualTo(1); assertThat(bodyBytes.get()).isEqualTo(8192);
        }
    }
    UUID trace(MvcResult result) throws Exception {
        var method = OperationsBoundaryFilter.class.getDeclaredMethod("trace", jakarta.servlet.http.HttpServletRequest.class); method.setAccessible(true); return (UUID) method.invoke(null, result.getRequest());
    }
    @Test void admitsExactPublicationPost() throws Exception { admitted("publications"); }
    @Test void admitsExactOffsalePost() throws Exception { admitted("offsales"); }
    @Test void classifiesReadsSubjectsAndCatalogCommandsSeparately() throws Exception {
        for (String suffix : List.of("publications", "offsales")) assertThat(mvc.perform(request(suffix)).andReturn().getResponse().getStatus()).isEqualTo(200);
        problem(mvc.perform(get("/api/v1/operations/catalog/products").servletPath("/api/v1/operations/catalog/products")).andReturn(), 401, "UNAUTHENTICATED");
        problem(mvc.perform(post("/api/v1/operations/subjects/" + VERSION + "/suspensions").servletPath("/api/v1/operations/subjects/" + VERSION + "/suspensions").header("Authorization", "Bearer valid").contentType("application/json").content("{}")).andReturn(), 422, "VALIDATION_FAILED");
    }
    @Test void catalogCommandsDoNotRequireIdempotencyKey() throws Exception {
        for (String suffix : List.of("publications", "offsales")) for (String key : List.of("", "invalid key", "private")) { var request = request(suffix); if (!key.isEmpty()) request.header("Idempotency-Key", key); assertThat(mvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(200); }
    }
    @Test void subjectCommandsRetainExactIdempotencyRule() throws Exception {
        String path = "/api/v1/operations/subjects/" + VERSION + "/suspensions";
        for (String key : List.of("", "x".repeat(129))) { var request = post(path).servletPath(path).header("Authorization", "Bearer valid").contentType("application/json").accept("application/json").content("{}"); if (!key.isEmpty()) request.header("Idempotency-Key", key); problem(mvc.perform(request).andReturn(), 422, "VALIDATION_FAILED"); }
        verifyNoInteractions(command);
    }
    @Test void authenticationPrecedesStructuralParsing() throws Exception {
        for (String suffix : List.of("publications", "offsales")) for (String token : List.of("", "invalid", "wrong")) {
            bodyStream = null;
            var malformed = post(BASE + "/" + suffix).servletPath(BASE + "/" + suffix).contentType("application/json").content("bad");
            if (!token.isEmpty()) malformed.header("Authorization", "Bearer " + token);
            problem(mvc.perform(malformed).andReturn(), 401, "UNAUTHENTICATED");
            var request = streamed(suffix, new byte[0], 0).with(value -> { value.removeHeader("Authorization"); return value; }); if (!token.isEmpty()) request.header("Authorization", "Bearer " + token); problem(mvc.perform(request).andReturn(), 401, "UNAUTHENTICATED");
            assertThat(bodyReads.get()).isZero(); assertThat(bodyBytes.get()).isZero();
        }
        sessions.failure = new IllegalStateException("PRIVATE"); for (String suffix : List.of("publications", "offsales")) {
            bodyStream = null; problem(mvc.perform(request(suffix).content("bad")).andReturn(), 503, "SERVICE_UNAVAILABLE");
            problem(mvc.perform(streamed(suffix, new byte[0], 0)).andReturn(), 503, "SERVICE_UNAVAILABLE");
            assertThat(bodyReads.get()).isZero(); assertThat(bodyBytes.get()).isZero();
        }
        verifyNoInteractions(command);
    }
    @Test void structuralValidationPrecedesAuthorityWitness() throws Exception {
        beans.destroySingleton("command");
        for (String suffix : List.of("publications", "offsales")) { problem(mvc.perform(request(suffix).content("bad")).andReturn(), 422, "VALIDATION_FAILED"); problem(mvc.perform(request(suffix)).andReturn(), 503, "SERVICE_UNAVAILABLE"); }
        beans.registerSingleton("one", command); beans.registerSingleton("two", mock(OperationsCatalogSaleCommand.class));
        for (String suffix : List.of("publications", "offsales")) problem(mvc.perform(request(suffix)).andReturn(), 503, "SERVICE_UNAVAILABLE"); verifyNoInteractions(command);
        beans.destroySingleton("one"); beans.destroySingleton("two");
        var unavailable = new org.springframework.beans.factory.support.RootBeanDefinition(OperationsCatalogSaleCommand.class);
        var providerCalls = new AtomicInteger();
        unavailable.setLazyInit(true); unavailable.setInstanceSupplier(() -> { providerCalls.incrementAndGet(); throw new IllegalStateException("PRIVATE SQL catalog_order_role password"); }); beans.registerBeanDefinition("unavailable", unavailable);
        for (String suffix : List.of("publications", "offsales")) {
            for (int size : List.of(8193, 2 * 1024 * 1024)) for (boolean declaredLength : List.of(true, false)) {
                var request = streamed(suffix, padded(suffix, size), -1);
                if (!declaredLength) request.with(value -> { value.setContent(null); return value; });
                problem(mvc.perform(request).andReturn(), 422, "VALIDATION_FAILED");
                assertThat(bodyReads.get()).isEqualTo(1); assertThat(bodyBytes.get()).isEqualTo(8193);
            }
            bodyStream = null;
            problem(mvc.perform(post(BASE + "/" + suffix).servletPath(BASE + "/" + suffix).header("Authorization", "Bearer valid").contentType("application/json").accept("application/json")).andReturn(), 422, "VALIDATION_FAILED");
            problem(mvc.perform(streamed(suffix, new byte[0], -1)).andReturn(), 422, "VALIDATION_FAILED");
            assertThat(bodyReads.get()).isEqualTo(1); assertThat(bodyBytes.get()).isZero();
            problem(mvc.perform(streamed(suffix, body(suffix).getBytes(StandardCharsets.UTF_8), 7)).andReturn(), 422, "VALIDATION_FAILED");
            assertThat(bodyReads.get()).isEqualTo(1); assertThat(bodyBytes.get()).isEqualTo(7);
        }
        assertThat(providerCalls.get()).isZero(); verifyNoInteractions(command);
        for (String suffix : List.of("publications", "offsales")) {
            problem(mvc.perform(streamed(suffix, padded(suffix, 8192), -1)).andReturn(), 503, "SERVICE_UNAVAILABLE");
            assertThat(bodyReads.get()).isEqualTo(1); assertThat(bodyBytes.get()).isEqualTo(8192);
        }
        assertThat(providerCalls.get()).isEqualTo(2); verifyNoInteractions(command); bodyStream = null;
        beans.removeBeanDefinition("unavailable"); beans.registerSingleton("command", command);
        when(command.publish(any())).thenThrow(new IllegalStateException("PRIVATE processing")); when(command.offsale(any())).thenThrow(new IllegalStateException("PRIVATE processing"));
        for (String suffix : List.of("publications", "offsales")) problem(mvc.perform(request(suffix)).andReturn(), 500, "INTERNAL");
    }
    @Test void disabledEmployeeReturnsForbiddenAfterValidParsing() throws Exception { when(command.publish(any())).thenThrow(new Failure(Code.FORBIDDEN)); when(command.offsale(any())).thenThrow(new Failure(Code.FORBIDDEN)); for (String suffix : List.of("publications", "offsales")) { problem(mvc.perform(request(suffix).content("bad")).andReturn(), 422, "VALIDATION_FAILED"); problem(mvc.perform(request(suffix)).andReturn(), 403, "FORBIDDEN"); } }
    @Test void opsReturnsForbidden() throws Exception { when(command.publish(any())).thenThrow(new Failure(Code.FORBIDDEN)); when(command.offsale(any())).thenThrow(new Failure(Code.FORBIDDEN)); for (String suffix : List.of("publications", "offsales")) problem(mvc.perform(request(suffix)).andReturn(), 403, "FORBIDDEN"); }
    @Test void rejectsInvalidContentTypeAndShippedAcceptOutcome() throws Exception { for (String suffix : List.of("publications", "offsales")) { problem(mvc.perform(request(suffix).contentType("text/plain")).andReturn(), 422, "VALIDATION_FAILED"); problem(mvc.perform(request(suffix).accept("text/html")).andReturn(), 500, "INTERNAL"); } verifyNoInteractions(command); }
    @Test void successHasExactStateAndHeaders() throws Exception {
        assertThat(mvc.perform(request("publications")).andReturn().getResponse().getContentAsByteArray()).isEqualTo(("{\"catalogVersionId\":\"00000000-0000-0000-0000-000000000026\",\"productId\":\"00000000-0000-0001-0000-000000000026\",\"versionNo\":1,\"saleStatus\":\"ACTIVE\",\"revision\":2,\"publishedAt\":\"2026-11-18T00:00:00Z\",\"currentActiveVersionId\":\"00000000-0000-0000-0000-000000000026\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for (UUID active : Arrays.asList(VERSION, null, new UUID(9, 38))) {
            var expected = new CatalogVersionState(VERSION, PRODUCT, 1, active == VERSION ? "ACTIVE" : "OFFSALE", 2, state.publishedAt(), active);
            when(command.publish(any())).thenReturn(expected); when(command.offsale(any())).thenReturn(expected);
            for (String suffix : List.of("publications", "offsales")) { var response = mvc.perform(request(suffix)).andReturn().getResponse(); assertThat(response.getStatus()).isEqualTo(200); assertThat(mapper.readTree(response.getContentAsByteArray())).isEqualTo(mapper.valueToTree(expected)); headers(response); }
        }
        var filter = new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter());
        for (String suffix : List.of("publications", "offsales")) {
            var request = new MockHttpServletRequest("POST", BASE + "/" + suffix); request.setServletPath(BASE + "/" + suffix); request.setUserPrincipal(() -> "employee"); request.setAttribute("sentinel", VERSION);
            var response = new MockHttpServletResponse(); response.setHeader("Sentinel", "unchanged"); response.setStatus(209); response.getWriter().write("unchanged"); var calls = new AtomicInteger();
            filter.doFilter(request, response, (passedRequest, passedResponse) -> { assertThat(passedRequest).isSameAs(request); assertThat(passedResponse).isSameAs(response); assertThat(((jakarta.servlet.http.HttpServletRequest) passedRequest).getUserPrincipal()).isSameAs(request.getUserPrincipal()); calls.incrementAndGet(); });
            assertThat(calls.get()).isEqualTo(1); assertThat(response.getStatus()).isEqualTo(209); assertThat(response.getHeader("Sentinel")).isEqualTo("unchanged"); assertThat(response.getContentAsString()).isEqualTo("unchanged");
        }
    }
    void conflict(Code code, String title) throws Exception {
        when(command.publish(any())).thenThrow(new Failure(code)); when(command.offsale(any())).thenThrow(new Failure(code));
        for (String suffix : List.of("publications", "offsales")) { var result = mvc.perform(request(suffix)).andReturn(); problem(result, 409, code.name()); assertThat(mapper.readTree(result.getResponse().getContentAsByteArray()).get("title").asString()).isEqualTo(title); }
    }
    @Test void staleViewHasExact409ProblemHeadersAndLength() throws Exception { conflict(Code.PUBLICATION_STALE_VIEW, "Publication stale view"); }
    @Test void concealsOtherCatalogMethodsAndUris() throws Exception {
        for (String path : List.of(BASE + "/publications;x", BASE + "/publications/", BASE + "/publications/extra", BASE + "%2f/publications", BASE + "/unknown", "/api/v1/operations/catalog/versions//publications")) {
            for (String token : List.of("", "invalid", "valid")) { var request = post(path).with(value -> { value.setServletPath(path); return value; }).contentType("application/json").content("{}"); if (!token.isEmpty()) request.header("Authorization", "Bearer " + token); problem(mvc.perform(request).andReturn(), 404, "NOT_FOUND"); }
        }
        for (String method : List.of("GET", "PUT", "PATCH", "DELETE")) problem(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), BASE + "/publications").servletPath(BASE + "/publications").header("Authorization", "Bearer valid")).andReturn(), 404, "NOT_FOUND"); verifyNoInteractions(command);
    }
    @Test void publicationConflictHasExact409ProblemHeadersAndLength() throws Exception { conflict(Code.CATALOG_PUBLICATION_CONFLICT, "Catalog publication conflict"); }
}
