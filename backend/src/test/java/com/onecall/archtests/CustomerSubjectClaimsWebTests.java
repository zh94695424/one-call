package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.subject_number.customerclaim.*;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.*;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException.Code;
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

@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class CustomerSubjectClaimsWebTests {
    private org.springframework.boot.test.system.CapturedOutput output;
    @org.junit.jupiter.api.BeforeEach
    void captureOutput(org.springframework.boot.test.system.CapturedOutput captured) { output = captured; }
    private static final String ROUTE = "/api/v1/customer/subject-claims";
    private static final UUID SUBJECT = UUID.randomUUID(), BINDING = UUID.randomUUID(), ACCOUNT = UUID.randomUUID();
    private static final Instant TIME = Instant.parse("2026-11-13T00:00:00Z");
    private static final String BODY = "{\"subjectId\":\"" + SUBJECT + "\",\"bindingId\":\"" + BINDING + "\",\"verificationMethod\":\"SMS\",\"verificationProofId\":\"proof-attempt\"}";
    private static final class Fixture {
        final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        final List<ClaimCommand> commands = new CopyOnWriteArrayList<>();
        final List<PersonalClaimCaller> callers = new CopyOnWriteArrayList<>();
        final Map<String, VerifiedCustomerSession> sessions = new ConcurrentHashMap<>();
        ClaimStatus status = ClaimStatus.ACCEPTED;
        Code failure;
        RuntimeException unexpected;
        final MockMvc mvc;
        Fixture() {
            sessions.put("valid", session(ACCOUNT, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
            CustomerSessionVerifier verifier = (bearer, audience) -> {
                var session = sessions.get(bearer.value());
                if (session == null) throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                return session;
            };
            beans.registerSingleton("verifier", verifier);
            beans.registerSingleton("owner", (CustomerSubjectClaimService) (command, caller) -> {
                commands.add(command); callers.add(caller);
                if (failure != null) throw new SubjectClaimException(failure);
                if (unexpected != null) throw unexpected;
                return new ClaimResult(UUID.nameUUIDFromBytes(command.requestId().getBytes(java.nio.charset.StandardCharsets.UTF_8)), status, command.subjectId(), status == ClaimStatus.ACCEPTED ? TIME : null);
            });
            beans.registerSingleton("composer", new CustomerSubjectClaimCommandService(beans.getBeanProvider(CustomerSubjectClaimService.class)));
            var writer = new CustomerProblemWriter();
            mvc = MockMvcBuilders.standaloneSetup(new CustomerSubjectClaimsController(beans.getBeanProvider(CustomerSubjectClaimCommandService.class), writer))
                    .addFilters(new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), writer)).build();
        }
        MockHttpServletResponse send(MockHttpServletRequestBuilder request) throws Exception { return mvc.perform(request).andReturn().getResponse(); }
        MockHttpServletResponse send() throws Exception { return send(request()); }
    }
    private static VerifiedCustomerSession session(UUID account, SelectedCustomerWorkspace workspace) {
        return new VerifiedCustomerSession(account, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1", workspace);
    }
    private static MockHttpServletRequestBuilder request() { return post(ROUTE).servletPath(ROUTE).header("Authorization", "Bearer valid").header("Idempotency-Key", "key").contentType("application/json").content(BODY); }
    private static JsonNode json(MockHttpServletResponse response) { return JsonMapper.builder().build().readTree(response.getContentAsByteArray()); }
    private static void problem(MockHttpServletResponse response, int status, String code) {
        assertThat(response.getStatus()).isEqualTo(status); assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(json(response).path("code").asString()).isEqualTo(code); assertThat(UUID.fromString(json(response).path("traceId").asString())).isNotNull();
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store"); assertThat(response.getHeader("Pragma")).isEqualTo("no-cache"); assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }
    @Test
    void exactPostReturnsOnlyFrozenAcceptedFields() throws Exception {
        var fixture = new Fixture(); var response = fixture.send(); assertThat(response.getStatus()).isEqualTo(200);
        assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("claimId", "status", "subjectId", "verifiedAt");
        assertThat(json(response).path("verifiedAt").asString()).isEqualTo(TIME.toString()); assertThat(json(response).path("status").asString()).isEqualTo("ACCEPTED");
        assertThat(response.getHeader("Location")).isNull(); assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }
    @Test
    void missingInvalidDuplicateAndWrongAudienceSessionAreUnauthenticated() throws Exception {
        var fixture = new Fixture(); fixture.sessions.put("wrong", new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, "wrong"));
        for (String token : List.of("ABSENT", "invalid", "wrong")) {
            var request = post(ROUTE).servletPath(ROUTE).content("invalid");
            if (!token.equals("ABSENT")) request.header("Authorization", "Bearer " + token);
            var response = fixture.send(request); problem(response, 401, "UNAUTHENTICATED"); assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        }
        problem(fixture.send(request().header("Authorization", "Bearer valid")), 401, "UNAUTHENTICATED"); assertThat(fixture.commands).isEmpty();
    }
    @Test
    void onlyTrustedPersonalWorkspaceCanReachOwnerCommand() throws Exception {
        var fixture = new Fixture();
        for (var workspace : List.of(new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, UUID.randomUUID()), new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, UUID.randomUUID()))) {
            fixture.sessions.put("valid", session(ACCOUNT, workspace)); problem(fixture.send(), 403, "FORBIDDEN");
        }
        assertThat(fixture.commands).isEmpty(); fixture.sessions.put("valid", session(ACCOUNT, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
        problem(fixture.send(request().content(BODY.replace("{", "{\"accountId\":\"" + UUID.randomUUID() + "\","))), 422, "VALIDATION_FAILED");
        fixture.send(request().queryParam("accountId", UUID.randomUUID().toString())); assertThat(fixture.callers.getFirst().accountId()).isEqualTo(ACCOUNT);
    }
    @Test
    void missingTrustedWorkspaceAndUnavailableDependenciesFailClosed() throws Exception {
        var fixture = new Fixture(); fixture.sessions.put("valid", session(ACCOUNT, null)); problem(fixture.send(), 503, "SERVICE_UNAVAILABLE");
        fixture.sessions.put("valid", session(ACCOUNT, new SelectedCustomerWorkspace(null, null))); problem(fixture.send(), 503, "SERVICE_UNAVAILABLE");
        fixture.sessions.put("valid", session(ACCOUNT, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
        fixture.beans.destroySingleton("owner"); problem(fixture.send(), 503, "SERVICE_UNAVAILABLE");
        fixture.beans.destroySingleton("verifier"); problem(fixture.send(), 503, "SERVICE_UNAVAILABLE");
    }
    @Test
    void closedBodyRejectsUnknownDuplicateMissingAndCoercedFields() throws Exception {
        var fixture = new Fixture();
        for (String body : List.of("[]", "null", BODY + "{}", BODY.replace("{", "{\"owner\":\"x\","), BODY.replace("{", "{\"subjectId\":\"" + SUBJECT + "\","),
                BODY.replace("\"verificationMethod\":\"SMS\",", ""), BODY.replace("\"SMS\"", "null"), BODY.replace("\"SMS\"", "123")))
            problem(fixture.send(request().content(body)), 422, "VALIDATION_FAILED");
        assertThat(fixture.commands).isEmpty();
    }
    @Test
    void idempotencyHeaderHasExactBoundsAndOpaqueIdentity() throws Exception {
        var fixture = new Fixture();
        for (String key : List.of("", " ", "x".repeat(129))) {
            var request = post(ROUTE).servletPath(ROUTE).header("Authorization", "Bearer valid").header("Idempotency-Key", key).contentType("application/json").content(BODY);
            problem(fixture.send(request), 422, "VALIDATION_FAILED");
        }
        problem(fixture.send(post(ROUTE).servletPath(ROUTE).header("Authorization", "Bearer valid").contentType("application/json").content(BODY)), 422, "VALIDATION_FAILED");
        problem(fixture.send(request().header("Idempotency-Key", "second")), 422, "VALIDATION_FAILED");
        for (String key : List.of("x", "x".repeat(128), " exact, opaque ", "😀".repeat(128))) {
            var response = fixture.send(post(ROUTE).servletPath(ROUTE).header("Authorization", "Bearer valid").header("Idempotency-Key", key).contentType("application/json").content(BODY));
            assertThat(response.getStatus()).isEqualTo(200); assertThat(fixture.commands.getLast().requestId().equals(key)).isTrue();
        }
    }
    @Test
    void methodUuidAndProofReferenceValidationUseUniform422() throws Exception {
        var fixture = new Fixture();
        for (var method : VerificationMethod.values()) assertThat(fixture.send(request().content(BODY.replace("SMS", method.name()))).getStatus()).isEqualTo(200);
        for (String body : List.of(BODY.replace("SMS", "sms"), BODY.replace(SUBJECT.toString(), "1-1-1-1-1"), BODY.replace("proof-attempt", " "), BODY.replace("proof-attempt", "x".repeat(129)), BODY.replace("proof-attempt", "\\u0000")))
            problem(fixture.send(request().content(body)), 422, "VALIDATION_FAILED");
        assertThat(fixture.commands).hasSize(4);
    }
    @Test
    void domainProblemsExposeOnlyThreeFrozen409Codes() throws Exception {
        var fixture = new Fixture(); Set<String> conflicts = new HashSet<>();
        for (Code code : Code.values()) {
            fixture.failure = code; var response = fixture.send();
            int status = switch (code) { case UNAUTHENTICATED -> 401; case FORBIDDEN -> 403; case VALIDATION_FAILED -> 422; case RATE_LIMITED -> 429; case SERVICE_UNAVAILABLE -> 503; case INTERNAL -> 500; default -> 409; };
            problem(response, status, code.name()); if (status == 409) conflicts.add(code.name());
        }
        assertThat(conflicts).containsExactlyInAnyOrder("CLAIM_RISK_BLOCKED", "CLAIM_REPLAY_CONFLICT", "CLAIM_CONTEXT_CONFLICT");
    }
    @Test
    void storedRejectedAndConflictReplayOmitVerifiedAt() throws Exception {
        var fixture = new Fixture();
        for (var status : List.of(ClaimStatus.REJECTED, ClaimStatus.CONFLICT)) {
            fixture.status = status; var response = fixture.send(); assertThat(response.getStatus()).isEqualTo(200);
            assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("claimId", "status", "subjectId"); assertThat(json(response).path("status").asString()).isEqualTo(status.name());
        }
    }
    @Test
    void exactRouteMediaNegotiationAndAuthenticationPrecedenceHold() throws Exception {
        var fixture = new Fixture();
        for (String route : List.of(ROUTE + "/", ROUTE + "/extra", ROUTE + ";x", ROUTE.toUpperCase(), ROUTE + "%2Fextra"))
            problem(fixture.send(post(route).with(request -> { request.setServletPath(route); return request; })), 404, "NOT_FOUND");
        for (String verb : List.of("GET", "PUT", "DELETE", "PATCH", "OPTIONS"))
            problem(fixture.send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(verb), ROUTE).servletPath(ROUTE)), 404, "NOT_FOUND");
        for (String media : List.of("text/plain", "bad/media;=", "application/json,application/json")) problem(fixture.send(request().contentType(media)), 422, "VALIDATION_FAILED");
        var raw = new org.springframework.mock.web.MockHttpServletRequest("POST", ROUTE);
        raw.setServletPath(ROUTE); raw.addHeader("Authorization", "Bearer valid"); raw.addHeader("Idempotency-Key", "key");
        var duplicateMedia = new jakarta.servlet.http.HttpServletRequestWrapper(raw) {
            @Override public Enumeration<String> getHeaders(String name) {
                return name.equalsIgnoreCase("Content-Type") ? Collections.enumeration(List.of("application/json", "application/json")) : super.getHeaders(name);
            }
        };
        var duplicateResponse = new MockHttpServletResponse();
        new CustomerBoundaryFilter(new CustomerBearerAuthenticator(fixture.beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter())
                .doFilter(duplicateMedia, duplicateResponse, (request, response) -> fail("Duplicate media must not dispatch"));
        problem(duplicateResponse, 422, "VALIDATION_FAILED");
        problem(fixture.send(request().accept("text/plain")), 500, "INTERNAL");
        problem(fixture.send(post(ROUTE).servletPath(ROUTE).contentType("text/plain").content("bad")), 401, "UNAUTHENTICATED");
    }
    @Test
    void responsesProblemsAndFailuresNeverExposeSensitiveCanaries() throws Exception {
        var fixture = new Fixture(); String canary = "PRIVATE_PROOF_CANARY";
        fixture.unexpected = new IllegalStateException(canary); var response = fixture.send(request().content(BODY.replace("proof-attempt", canary)));
        problem(response, 500, "INTERNAL"); assertThat(response.getContentAsString().contains(canary)).isFalse();
        assertThat(fixture.commands.getFirst().toString().contains(canary)).isFalse();
        assertThat(new CustomerSubjectClaimJsonBinder().bind(BODY.replace("proof-attempt", canary).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().contains(canary)).isFalse();
        assertThat(output.getAll().contains(canary)).isFalse();
    }
    @Test
    void concurrentSessionsKeepCallerAndCommandContextIsolated() throws Exception {
        var fixture = new Fixture(); UUID other = UUID.randomUUID(); fixture.sessions.put("other", session(other, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
        var gate = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { assertThat(gate.await(10, TimeUnit.SECONDS)).isTrue(); return fixture.send(); });
            var second = executor.submit(() -> { assertThat(gate.await(10, TimeUnit.SECONDS)).isTrue(); return fixture.send(post(ROUTE).servletPath(ROUTE).header("Authorization", "Bearer other").header("Idempotency-Key", "other-key").contentType("application/json").content(BODY)); });
            gate.countDown(); assertThat(first.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(200); assertThat(second.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(200);
        }
        assertThat(fixture.callers.stream().map(PersonalClaimCaller::accountId)).containsExactlyInAnyOrder(ACCOUNT, other);
        assertThat(fixture.commands.stream().map(ClaimCommand::requestId)).containsExactlyInAnyOrder("key", "other-key");
    }
}
