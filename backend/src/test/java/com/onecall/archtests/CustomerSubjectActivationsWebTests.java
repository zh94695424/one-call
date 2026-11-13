package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService;
import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.*;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code;
import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CustomerSubjectActivationsWebTests {
    static final UUID SUBJECT = UUID.randomUUID(), ACCOUNT = UUID.randomUUID();
    static final String ROUTE = "/api/v1/customer/subjects/" + SUBJECT + "/activations";
    static final String BODY = "{\"activationEventId\":\"event\",\"decision\":\"ACCEPT\",\"policyCode\":\"FIRST-TRIAL\",\"policyVersion\":\"1\",\"promptVersion\":\"1\",\"contentSha256\":\"" + "a".repeat(64) + "\"}";
    static final class Fixture {
        final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        final List<ActivationCommand> commands = new ArrayList<>();
        final List<ActivationCaller> callers = new ArrayList<>();
        VerifiedCustomerSession session = new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1", new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null));
        Code failure;
        RuntimeException unexpected;
        final MockMvc mvc;
        Fixture() {
            beans.registerSingleton("verifier", (CustomerSessionVerifier) (bearer, audience) -> {
                if (!"valid".equals(bearer.value())) throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                return session;
            });
            beans.registerSingleton("owner", (CustomerSubjectActivationService) (command, caller) -> {
                commands.add(command); callers.add(caller);
                if (failure != null) throw new SubjectActivationException(failure);
                if (unexpected != null) throw unexpected;
                return new ActivationResult("ACTIVE", 2, UUID.randomUUID(), UUID.randomUUID(), "GRANTED", new ActivationTrial(600, Instant.parse("2026-11-14T00:00:00Z")));
            });
            beans.registerSingleton("composer", new CustomerSubjectActivationCommandService(beans.getBeanProvider(CustomerSubjectActivationService.class)));
            var writer = new CustomerProblemWriter();
            mvc = MockMvcBuilders.standaloneSetup(new CustomerSubjectActivationsController(beans.getBeanProvider(CustomerSubjectActivationCommandService.class), writer))
                    .addFilters(new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), writer)).build();
        }
        MockHttpServletResponse send(MockHttpServletRequestBuilder request) throws Exception { return mvc.perform(request).andReturn().getResponse(); }
        MockHttpServletResponse send() throws Exception { return send(request()); }
    }
    static MockHttpServletRequestBuilder request() { return post(ROUTE).servletPath(ROUTE).header("Authorization", "Bearer valid").contentType("application/json").content(BODY); }
    static JsonNode json(MockHttpServletResponse response) { return JsonMapper.builder().build().readTree(response.getContentAsByteArray()); }
    static void problem(MockHttpServletResponse response, int status, String code) {
        assertThat(response.getStatus()).isEqualTo(status); assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId"); assertThat(json(response).path("code").asString()).isEqualTo(code);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store"); assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }
    @Test
    void acceptUsesExactRouteAndFrozenResultShape() throws Exception {
        var response = new Fixture().send(); assertThat(response.getStatus()).isEqualTo(200); assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("activationStatus", "activationRevision", "transitionId", "consentId", "trialOutcome", "trial");
        assertThat(json(response).path("trial").propertyNames()).containsExactlyInAnyOrder("grantedSeconds", "expiresAt"); assertThat(json(response).path("activationStatus").asString()).isEqualTo("ACTIVE");
        var result = json(response);
        assertThat(result.path("activationStatus").isString()).isTrue();
        assertThat(result.path("activationRevision").isIntegralNumber()).isTrue();
        assertThat(result.path("activationRevision").asLong()).isPositive();
        for (String field : List.of("transitionId", "consentId")) {
            assertThat(result.path(field).isString()).isTrue();
            assertThat(UUID.fromString(result.path(field).asString()).toString()).isEqualTo(result.path(field).asString());
        }
        assertThat(result.path("trialOutcome").isString()).isTrue();
        assertThat(result.path("trialOutcome").asString()).isEqualTo("GRANTED");
        assertThat(result.path("trial").path("grantedSeconds").isIntegralNumber()).isTrue();
        assertThat(result.path("trial").path("grantedSeconds").asLong()).isPositive();
        assertThat(result.path("trial").path("expiresAt").isString()).isTrue();
        String expiresAt = result.path("trial").path("expiresAt").asString();
        assertThat(Instant.parse(expiresAt).toString()).isEqualTo(expiresAt);
    }
    @Test
    void activationUsesBodyEventWithoutIdempotencyHeader() throws Exception {
        var fixture = new Fixture(); assertThat(fixture.send().getStatus()).isEqualTo(200); assertThat(fixture.send(request().header("Idempotency-Key", "different")).getStatus()).isEqualTo(200); assertThat(fixture.commands).allMatch(command -> "event".equals(command.activationEventId()));
    }
    @Test
    void verifiedSessionSuppliesAccountChannelAndWorkspace() throws Exception {
        var fixture = new Fixture(); assertThat(fixture.send(request().param("accountId", "spoof").header("X-Role", "ADMINISTRATOR")).getStatus()).isEqualTo(200);
        assertThat(fixture.callers.getFirst()).isEqualTo(new ActivationCaller(ACCOUNT, Channel.WEB, WorkspaceType.PERSONAL, null));
        UUID organization = UUID.randomUUID();
        fixture.session = new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1", new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, organization));
        assertThat(fixture.send(request().param("organizationId", UUID.randomUUID().toString()).header("X-Role", "ADMINISTRATOR")).getStatus()).isEqualTo(200);
        assertThat(fixture.callers.getLast()).isEqualTo(new ActivationCaller(ACCOUNT, Channel.WEB, WorkspaceType.ORGANIZATION, organization));
        problem(fixture.send(request().content(BODY.replace("{", "{\"accountId\":\"spoof\","))), 422, "VALIDATION_FAILED");
    }
    @Test
    void authenticationPrecedesValidationAndPoolResolution() throws Exception {
        var fixture = new Fixture(); problem(fixture.send(post(ROUTE).servletPath(ROUTE).content("bad")), 401, "UNAUTHENTICATED"); assertThat(fixture.commands).isEmpty();
    }
    @Test
    void malformedTrustedContextFailsClosed() throws Exception {
        var fixture = new Fixture(); fixture.session = new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1", null); problem(fixture.send(request().content("bad")), 503, "SERVICE_UNAVAILABLE"); assertThat(fixture.commands).isEmpty();
    }
    @Test
    void closedBodyRejectsUnknownDuplicateMissingAndWrongTypedFields() throws Exception {
        var fixture = new Fixture(); for (String body : List.of("[]", "{}", BODY.replace("\"event\"", "null"), BODY.replace("\"event\"", "42"), BODY.replace("{", "{\"decision\":\"ACCEPT\","), BODY.replace("{", "{\"trialOutcome\":\"GRANTED\","))) problem(fixture.send(request().content(body)), 422, "VALIDATION_FAILED"); assertThat(fixture.commands).isEmpty();
    }
    @Test
    void bodyRejectsMalformedUtf8TrailingJsonAndInvalidLengths() throws Exception {
        var binder = new CustomerSubjectActivationJsonBinder();
        for (String value : List.of(" ", "x".repeat(129), "\\u0000", "\\ud800")) assertThatThrownBy(() -> binder.bind(BODY.replace("\"event\"", "\"" + value + "\"").getBytes(StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);
        assertThat(binder.bind(BODY.replace("\"event\"", "\"" + "😀".repeat(128) + "\"").getBytes(StandardCharsets.UTF_8)).activationEventId()).hasSize(256);
        assertThatThrownBy(() -> binder.bind((BODY + "{}").getBytes(StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> binder.bind(new byte[] {(byte) 0xc3, 0x28})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> binder.bind(BODY.replace("FIRST-TRIAL", "x".repeat(65)).getBytes(StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);
        String oversized = BODY + " ".repeat(8193 - BODY.getBytes(StandardCharsets.UTF_8).length);
        var fixture = new Fixture();
        problem(fixture.send(request().content(oversized)), 422, "VALIDATION_FAILED");
        problem(fixture.send(request().content(oversized).header("Transfer-Encoding", "chunked").with(servletRequest -> {
            var chunked = org.mockito.Mockito.spy(servletRequest);
            org.mockito.Mockito.doReturn(-1).when(chunked).getContentLength();
            org.mockito.Mockito.doReturn(-1L).when(chunked).getContentLengthLong();
            return chunked;
        })), 422, "VALIDATION_FAILED");
        assertThat(fixture.commands).isEmpty(); assertThat(fixture.callers).isEmpty();
        assertThat(fixture.send(request().content(oversized.substring(0, 8192))).getStatus()).isEqualTo(200);
    }
    @Test
    void pathContentTypeAndDecisionValidationUseFrozenErrors() throws Exception {
        var fixture = new Fixture(); String invalidPath = ROUTE.replace(SUBJECT.toString(), "1-1-1-1-1"); problem(fixture.send(post(invalidPath).servletPath(invalidPath).header("Authorization", "Bearer valid").contentType("application/json").content(BODY)), 422, "VALIDATION_FAILED");
        problem(fixture.send(request().contentType("text/plain")), 422, "VALIDATION_FAILED"); for (String body : List.of(BODY.replace("ACCEPT", "accept"), BODY.replace("a".repeat(64), "A".repeat(64)))) problem(fixture.send(request().content(body)), 422, "VALIDATION_FAILED");
    }
    @Test
    void declineReturnsOnlyCommittedTypedProblem() throws Exception {
        var fixture = new Fixture(); fixture.failure = Code.ACTIVATION_CONSENT_DECLINED; problem(fixture.send(request().content(BODY.replace("ACCEPT", "DECLINE"))), 409, "ACTIVATION_CONSENT_DECLINED"); assertThat(fixture.commands.getFirst().decision()).isEqualTo(Decision.DECLINE);
    }
    @Test
    void domainFailuresUseOnlyOperationErrorSet() throws Exception {
        int[] statuses = {401, 403, 404, 422, 409, 409, 429, 503, 500}; var fixture = new Fixture();
        for (Code code : Code.values()) { fixture.failure = code; problem(fixture.send(), statuses[code.ordinal()], code.name()); }
    }
    @Test
    void encodedAndUnrelatedRoutesRemainClosed() throws Exception {
        var fixture = new Fixture(); for (String route : List.of(ROUTE + "/extra", ROUTE.replace("/activations", "%2factivations"), ROUTE.replace(SUBJECT.toString(), "%31"))) problem(fixture.send(post(java.net.URI.create(route)).servletPath(route).header("Authorization", "Bearer valid")), 404, "NOT_FOUND"); problem(fixture.send(get(ROUTE).servletPath(ROUTE)), 404, "NOT_FOUND");
    }
    @Test
    void responsesProblemsAndLogsRedactInternalActivationFacts() throws Exception {
        var fixture = new Fixture(); fixture.unexpected = new RuntimeException("SQL_PASSWORD_POLICY_FINGERPRINT_CANARY"); var response = fixture.send(); problem(response, 500, "INTERNAL"); assertThat(response.getContentAsString()).doesNotContain("CANARY", ACCOUNT.toString(), "snapshot", "fingerprint");
        assertThat(new ActivationCaller(ACCOUNT, Channel.WEB, WorkspaceType.PERSONAL, null).toString()).isEqualTo("ActivationCaller[REDACTED]");
    }
}
