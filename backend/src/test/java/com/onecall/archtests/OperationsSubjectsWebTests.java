package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.customer_iam.internal.security.*;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class OperationsSubjectsWebTests {
    static final UUID SUBJECT = UUID.randomUUID(), EMPLOYEE = UUID.randomUUID();
    static final String BASE = "/api/v1/operations/subjects/" + SUBJECT;
    static final String BODY = "{\"reasonCode\":\"reason\",\"expectedActivationRevision\":2}";
    static final ActivationTransition RESULT = new ActivationTransition(UUID.randomUUID(), "activation-status:" + UUID.randomUUID() + ":3", "ACTIVE", "SUSPENDED", 2, 3, "OPERATIONS", "OPERATIONS", "reason", Instant.parse("2026-11-13T00:00:00Z"));
    static final class Fixture {
        final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        final SyntheticEmployeeSessions sessions = new SyntheticEmployeeSessions();
        final List<Command> commands = new CopyOnWriteArrayList<>();
        final List<Caller> callers = new CopyOnWriteArrayList<>();
        volatile Code failure;
        final MockMvc mvc;
        Fixture() {
            sessions.sessions.put("valid", new VerifiedEmployeeSession(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE));
            beans.registerSingleton("verifier", sessions);
            beans.registerSingleton("operationsSubjectLifecycleService", new OperationsSubjectLifecycleService() {
                public ActivationTransition suspend(Command command, Caller caller) { return capture(command, caller); }
                public ActivationTransition resume(Command command, Caller caller) { return capture(command, caller); }
                private ActivationTransition capture(Command command, Caller caller) { commands.add(command); callers.add(caller); if (failure != null) throw new Failure(failure); return RESULT; }
            });
            var problems = new OperationsProblemWriter();
            mvc = MockMvcBuilders.standaloneSetup(new OperationsSubjectsController(beans.getBeanProvider(OperationsSubjectLifecycleService.class), problems))
                    .addFilters(new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(beans.getBeanProvider(EmployeeSessionVerifier.class)), problems),
                            new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter())).build();
        }
        MockHttpServletResponse send(MockHttpServletRequestBuilder request) throws Exception { return mvc.perform(request).andReturn().getResponse(); }
        MockHttpServletResponse send() throws Exception { return send(request("suspensions")); }
    }
    static MockHttpServletRequestBuilder request(String suffix) { return post(BASE + "/" + suffix).servletPath(BASE + "/" + suffix).header("Authorization", "Bearer valid").header("Idempotency-Key", "action").contentType("application/json").content(BODY); }
    static JsonNode json(MockHttpServletResponse response) { return JsonMapper.builder().build().readTree(response.getContentAsByteArray()); }
    static void problem(MockHttpServletResponse response, int status, String code) {
        assertThat(response.getStatus()).isEqualTo(status); assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(json(response).path("code").asString()).isEqualTo(code); UUID.fromString(json(response).path("traceId").asString());
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store"); assertThat(response.getHeader("Pragma")).isEqualTo("no-cache"); assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff"); assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    }

    @Test
    void exactTwoPostsReturnOnlyTenFrozenFields() throws Exception {
        var fixture = new Fixture();
        for (String route : List.of("suspensions", "resumptions")) { var response = fixture.send(request(route)); assertThat(response.getStatus()).isEqualTo(200); assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("transitionId", "statusEventId", "fromStatus", "toStatus", "fromRevision", "toRevision", "sourceChannel", "sourceActionDomain", "reasonCode", "occurredAt"); assertThat(json(response).path("fromRevision").isIntegralNumber()).isTrue(); }
        assertThat(fixture.commands).hasSize(2);
    }
    @Test
    void wrongMethodAndNoncanonicalRoutesAreConcealed() throws Exception {
        var fixture = new Fixture();
        for (String path : List.of(BASE, BASE + "/suspensions/", BASE + "/suspensions;x", BASE + "/a%2Fb/suspensions", BASE + "//suspensions", "/api/v1/operations/employees")) problem(fixture.send(post(path).with(servlet -> { servlet.setServletPath(path); return servlet; })), 404, "NOT_FOUND");
        problem(fixture.send(get(BASE + "/suspensions").servletPath(BASE + "/suspensions")), 404, "NOT_FOUND");
    }
    @Test
    void authenticationPrecedesMalformedInputAndMediaNegotiation() throws Exception {
        var fixture = new Fixture();
        problem(fixture.send(post(BASE + "/suspensions").servletPath(BASE + "/suspensions").content("{").contentType("text/html").accept("text/html")), 401, "UNAUTHENTICATED");
        fixture.beans.destroySingleton("verifier"); problem(fixture.send(request("suspensions").content("{").accept("text/html")), 503, "SERVICE_UNAVAILABLE");
    }
    @Test
    void closedBodyRejectsUnknownDuplicateMissingAndCoercedFields() throws Exception {
        var fixture = new Fixture();
        for (String body : List.of("{}", "null", "[]", "1", BODY + "{}", "{\"reasonCode\":\"r\",\"reasonCode\":\"r\",\"expectedActivationRevision\":2}", BODY.replace("2}", "\"2\"}"), BODY.replace("2}", "2.0}"), BODY.replace("2}", "true}"), BODY.replace("2}", "null}"), BODY.replace("2}", "2,\"employeeId\":\"spoof\"}"))) problem(fixture.send(request("suspensions").content(body)), 422, "VALIDATION_FAILED");
        problem(fixture.send(request("suspensions").content(new byte[] {(byte) 0xc3, 0x28})), 422, "VALIDATION_FAILED");
        problem(fixture.send(request("suspensions").content(" ".repeat(8193))), 422, "VALIDATION_FAILED"); assertThat(fixture.commands).isEmpty();
    }
    @Test
    void reasonAndRevisionBoundsPreserveFrozenInputSemantics() throws Exception {
        var fixture = new Fixture();
        for (String reason : List.of(" ", "a".repeat(65), "")) problem(fixture.send(request("suspensions").content(BODY.replace("reason\"", reason + "\""))), 422, "VALIDATION_FAILED");
        String reason = "😀".repeat(64); String body = JsonMapper.builder().build().writeValueAsString(Map.of("reasonCode", reason, "expectedActivationRevision", new BigInteger("999999999999999999999999")));
        assertThat(fixture.send(request("suspensions").content(body)).getStatus()).isEqualTo(200); assertThat(fixture.commands.getLast().reasonCode()).isEqualTo(reason);
        for (String revision : List.of("0", "-1", "1e0")) problem(fixture.send(request("suspensions").content(BODY.replace("2}", revision + "}"))), 422, "VALIDATION_FAILED");
    }
    @Test
    void idempotencyHeaderBoundsAndExactOpaqueTextArePreserved() throws Exception {
        var fixture = new Fixture();
        for (String key : List.of("", " ", "a".repeat(129), "a\0b")) problem(fixture.send(request("suspensions").with(servlet -> { servlet.removeHeader("Idempotency-Key"); servlet.addHeader("Idempotency-Key", key); return servlet; })), 422, "VALIDATION_FAILED");
        problem(fixture.send(request("suspensions").header("Idempotency-Key", "action")), 422, "VALIDATION_FAILED");
        problem(fixture.send(request("suspensions").with(servlet -> { servlet.removeHeader("Idempotency-Key"); return servlet; })), 422, "VALIDATION_FAILED");
        for (String key : List.of("x", "a".repeat(128), " action ")) { assertThat(fixture.send(request("suspensions").with(servlet -> { servlet.removeHeader("Idempotency-Key"); servlet.addHeader("Idempotency-Key", key); return servlet; })).getStatus()).isEqualTo(200); assertThat(fixture.commands.getLast().actionEventId()).isEqualTo(key); }
    }
    @Test
    void uuidAndContentNegotiationUseFrozenProblemSet() throws Exception {
        var fixture = new Fixture();
        String path = "/api/v1/operations/subjects/INVALID/suspensions";
        problem(fixture.send(post(path).servletPath(path).header("Authorization", "Bearer valid").header("Idempotency-Key", "key").contentType("application/json").content(BODY)), 422, "VALIDATION_FAILED");
        problem(fixture.send(request("suspensions").contentType("text/plain")), 422, "VALIDATION_FAILED");
        problem(fixture.send(request("suspensions").accept("text/html")), 500, "INTERNAL");
    }
    @Test
    void wrongRoleAudienceAndUnavailableDependenciesMapExactly() throws Exception {
        var fixture = new Fixture();
        fixture.failure = Code.FORBIDDEN; problem(fixture.send(), 403, "FORBIDDEN");
        fixture.failure = Code.SERVICE_UNAVAILABLE; problem(fixture.send(), 503, "SERVICE_UNAVAILABLE");
        fixture.sessions.sessions.put("valid", new VerifiedEmployeeSession(EMPLOYEE, "onecall:customer-session:v1")); problem(fixture.send(), 401, "UNAUTHENTICATED");
        fixture.sessions.sessions.put("valid", new VerifiedEmployeeSession(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE)); fixture.beans.destroySingleton("operationsSubjectLifecycleService"); problem(fixture.send(), 503, "SERVICE_UNAVAILABLE");
    }
    @Test
    void successReplayAndConflictHaveIdenticalFrozenProjectionRules() throws Exception {
        var fixture = new Fixture(); assertThat(json(fixture.send())).isEqualTo(json(fixture.send())); fixture.failure = Code.ACTIVATION_STATE_CONFLICT; problem(fixture.send(), 409, "ACTIVATION_STATE_CONFLICT");
    }
    @Test
    void problemsAndResponsesNeverLeakSensitiveCanaries() throws Exception {
        var fixture = new Fixture(); fixture.sessions.failure = new IllegalStateException("SECRET SQL password Bearer valid");
        var first = fixture.send(); var second = fixture.send(); problem(first, 503, "SERVICE_UNAVAILABLE");
        assertThat(first.getContentAsString()).doesNotContain("SECRET", "SQL", "password", "Bearer valid", "<html", "Exception"); assertThat(json(first).path("traceId").asString()).isNotEqualTo(json(second).path("traceId").asString());
    }
    @Test
    void operationsAndCustomerFiltersNeverCrossAuthenticate() throws Exception {
        var fixture = new Fixture(); assertThat(fixture.send().getStatus()).isEqualTo(200);
        fixture.sessions.sessions.clear(); problem(fixture.send(), 401, "UNAUTHENTICATED"); assertThat(fixture.commands).hasSize(1);
        String source = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/onecall/core/customer_iam/internal/web/CustomerBoundaryFilter.java")); assertThat(source.indexOf("operationsPath.matches")).isLessThan(source.indexOf("UUID traceId = UUID.randomUUID()")); assertThat(source).doesNotContain("EmployeeSession", "EmployeeBearer");
    }
    @Test
    void concurrentRequestsKeepCallerActionAndReasonIsolated() throws Exception {
        var fixture = new Fixture();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<?>>();
            for (int index = 0; index < 16; index++) { String token = "token" + index; UUID employee = UUID.randomUUID(); fixture.sessions.sessions.put(token, new VerifiedEmployeeSession(employee, EmployeeBearerAuthenticator.AUDIENCE));
                futures.add(executor.submit(() -> { try { assertThat(fixture.send(request("suspensions").with(servlet -> { servlet.removeHeader("Authorization"); servlet.addHeader("Authorization", "Bearer " + token); servlet.removeHeader("Idempotency-Key"); servlet.addHeader("Idempotency-Key", token); return servlet; }).content(BODY.replace("reason\"", token + "\""))).getStatus()).isEqualTo(200); } catch (Exception failure) { throw new RuntimeException(failure); } })); }
            for (var future : futures) future.get(15, TimeUnit.SECONDS);
        }
        assertThat(fixture.commands).hasSize(16).allMatch(command -> command.actionEventId().equals(command.reasonCode())); assertThat(fixture.callers.stream().map(Caller::employeeId).distinct()).hasSize(16);
    }
}
