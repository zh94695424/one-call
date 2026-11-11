package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = CustomerSubjectsWebTests.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("web")
class CustomerSubjectsWebTests {
    private static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID SUBJECT = UUID.fromString("abcdefab-0000-0000-0000-000000000001");
    private static final String PATH = "/api/v1/customer/subjects/" + SUBJECT;
    @LocalServerPort private int port;
    @Autowired private CustomerSubjectDetailQueryService service;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({CustomerWebConfiguration.class, CustomerSubjectsController.class, CustomerApiErrorController.class, Beans.class})
    static class Application { }

    @TestConfiguration(proxyBeanMethods = false)
    static class Beans {
        @Bean
        CustomerSubjectDetailQueryService customerSubjectDetailQueryService() { return mock(CustomerSubjectDetailQueryService.class); }
        @Bean
        CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> {
                String token = bearer.value();
                if (token.equals("outage")) throw new IllegalStateException("VERIFIER_CANARY");
                if (token.equals("bad")) throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                var workspace = switch (token) {
                    case "unselected" -> null;
                    case "null-kind" -> new SelectedCustomerWorkspace(null, null);
                    case "personal-org" -> new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, ORGANIZATION);
                    case "org-null" -> new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, null);
                    case "organization" -> new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, ORGANIZATION);
                    default -> new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null);
                };
                return new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                        token.equals("wrong-audience") ? "foreign" : audience, workspace);
            };
        }
    }

    @BeforeEach
    void setup() {
        reset(service);
        when(service.getCustomerSubject(any(), eq(SUBJECT))).thenReturn(Optional.of(
                new CustomerSubjectDetailQueryService.Detail(SUBJECT, "SUSPENDED", 2147483648L, 3)));
    }

    private HttpResponse<String> request(String method, String path, String token, HttpRequest.BodyPublisher body, String... headers) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(java.time.Duration.ofSeconds(20));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        for (int index = 0; index < headers.length; index += 2) builder.header(headers[index], headers[index + 1]);
        try (var client = HttpClient.newHttpClient()) {
            return client.send(builder.method(method, body).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
    private HttpResponse<String> get(String path, String token, String... headers) throws Exception {
        return request("GET", path, token, HttpRequest.BodyPublishers.noBody(), headers);
    }
    private JsonNode check(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("Pragma")).contains("no-cache");
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(response.body()).doesNotContain("CANARY", "jdbc:", "password", "ciphertext");
        if (response.body().isEmpty()) return null;
        var json = JsonMapper.builder().build().readTree(response.body());
        if (code != null) {
            assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/problem+json");
            assertThat(json.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
            assertThat(json.path("code").asString()).isEqualTo(code);
            assertThat(json.path("status").asInt()).isEqualTo(status);
            assertThat(UUID.fromString(json.path("traceId").asString())).isNotNull();
        }
        return json;
    }
    private void four(JsonNode json) {
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("subjectId", "activationStatus", "evidenceCount", "orderCount");
        assertThat(json.path("subjectId").asString()).isEqualTo(SUBJECT.toString());
        assertThat(json.path("activationStatus").asString()).isEqualTo("SUSPENDED");
        assertThat(json.path("evidenceCount").isIntegralNumber()).isTrue();
        assertThat(json.path("evidenceCount").asLong()).isEqualTo(2147483648L);
        assertThat(json.path("orderCount").asLong()).isEqualTo(3);
    }

    @Test
    void getUsesExactAuthenticatedSubjectRouteAndFourFieldResponse() throws Exception {
        var response = get(PATH, "personal");
        four(check(response, 200, null));
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/json");
        verify(service).getCustomerSubject(new TrustedCustomerCaller(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)), SUBJECT);
    }

    @Test
    void unsupportedMethodsExtraSegmentsAndTrailingRoutesRemainNotFound() throws Exception {
        for (String method : List.of("POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")) {
            var response = request(method, PATH, "personal", HttpRequest.BodyPublishers.noBody());
            assertThat(response.statusCode()).isEqualTo(404);
            if (!method.equals("HEAD")) check(response, 404, "NOT_FOUND");
        }
        for (String path : List.of(PATH + "/", PATH + "/json", "/api/v1/customer/subjects/", "/api/v1/customer/subjects",
                "/api/v1/customer/subject/" + SUBJECT, PATH + ";x=y", PATH + "%2Fjson", PATH + "%3Bx=y"))
            assertThat(get(path, "personal").statusCode()).isEqualTo(404);
        verifyNoInteractions(service);
    }

    @Test
    void missingMalformedDuplicateAndRejectedBearerNeverResolveService() throws Exception {
        for (String token : new String[]{null, "bad", "wrong-audience", "", "a b"}) check(get(PATH, token), 401, "UNAUTHENTICATED");
        check(get(PATH, "personal", "Authorization", "Bearer personal"), 401, "UNAUTHENTICATED");
        verifyNoInteractions(service);
    }

    @Test
    void unavailableVerifierFailsClosedBeforeSubjectResolution() throws Exception {
        check(get(PATH, "outage"), 503, "SERVICE_UNAVAILABLE");
        for (int count : new int[]{0, 2}) {
            var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
            for (int index = 0; index < count; index++) factory.registerSingleton("verifier" + index,
                    (CustomerSessionVerifier) (bearer, audience) -> { throw new AssertionError("Must not run"); });
            var filter = new CustomerBoundaryFilter(new CustomerBearerAuthenticator(factory.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter());
            var request = new org.springframework.mock.web.MockHttpServletRequest("GET", PATH);
            request.setServletPath(PATH); request.addHeader("Authorization", "Bearer personal");
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            filter.doFilter(request, response, (incoming, outgoing) -> { throw new AssertionError("Must stop"); });
            assertThat(response.getStatus()).isEqualTo(503);
        }
        verifyNoInteractions(service);
    }

    @Test
    void trustedSelectedWorkspaceIsTheOnlyAuthority() throws Exception {
        for (String token : List.of("personal", "organization")) {
            four(check(get(PATH, token, "X-Account-Id", "CANARY", "X-Workspace-Id", "CANARY", "X-Role", "ADMINISTRATOR"), 200, null));
            verify(service).getCustomerSubject(new TrustedCustomerCaller(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                    new SelectedCustomerWorkspace(token.equals("personal") ? CustomerContext.WorkspaceType.PERSONAL
                            : CustomerContext.WorkspaceType.ORGANIZATION, token.equals("personal") ? null : ORGANIZATION)), SUBJECT);
        }
    }

    @Test
    void malformedAbbreviatedAndDottedSubjectValuesReturnValidationFailed() throws Exception {
        for (String value : List.of("bad", "1-1-1-1-1", SUBJECT + ".json", "00000000-0000-0000-0000-00000000000z"))
            check(get("/api/v1/customer/subjects/" + value, "personal"), 422, "VALIDATION_FAILED");
        verifyNoInteractions(service);
        four(check(get(PATH.toUpperCase().replace("/API/V1/CUSTOMER/SUBJECTS/", "/api/v1/customer/subjects/"), "personal"), 200, null));
        check(get(PATH + "/json", "personal"), 404, "NOT_FOUND");
    }

    @Test
    void extraQueryParametersAndGetBodiesAreIgnoredWithoutChangingScope() throws Exception {
        String query = "?ownerAccountId=foreign&ownerAccountId=other&accountId=bad&workspace=bad&role=bad&cursor=bad&cursor=bad";
        for (String token : List.of("personal", "organization")) {
            four(check(get(PATH + query, token), 200, null));
            for (String body : List.of("not json CANARY", "{\"ownerAccountId\":\"CANARY\"}")) {
                four(check(request("GET", PATH + query, token, HttpRequest.BodyPublishers.ofString(body), "Content-Type", "application/json"), 200, null));
                four(check(request("GET", PATH + query, token, HttpRequest.BodyPublishers.ofInputStream(() ->
                        new java.io.ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8))), "Content-Type", "text/plain"), 200, null));
            }
        }
        clearInvocations(service);
        check(get("/api/v1/customer/subjects/bad" + query, "personal"), 422, "VALIDATION_FAILED");
        verifyNoInteractions(service);
        when(service.getCustomerSubject(any(), eq(SUBJECT))).thenReturn(Optional.empty());
        check(get(PATH + query, "personal"), 404, "NOT_FOUND");
    }

    @Test
    void invalidTrustedWorkspacePrecedesPathValidation() throws Exception {
        for (String token : List.of("unselected", "null-kind", "personal-org", "org-null"))
            check(get("/api/v1/customer/subjects/bad", token), 503, "SERVICE_UNAVAILABLE");
        verifyNoInteractions(service);
    }

    @Test
    void missingServiceAndPersistenceFailuresUseSanitizedUnavailable() throws Exception {
        when(service.getCustomerSubject(any(), any())).thenThrow(new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE));
        check(get(PATH, "personal"), 503, "SERVICE_UNAVAILABLE");
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        var controller = new CustomerSubjectsController(factory.getBeanProvider(CustomerSubjectDetailQueryService.class), new CustomerProblemWriter());
        var principal = new CustomerRequestPrincipal(new TrustedCustomerCaller(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
        assertThatThrownBy(() -> controller.getCustomerSubject(principal, SUBJECT.toString(),
                new org.springframework.mock.web.MockHttpServletRequest(), new org.springframework.mock.web.MockHttpServletResponse()))
                .isInstanceOfSatisfying(CustomerContextException.class, error -> assertThat(error.code()).isEqualTo(CustomerContextException.Code.SERVICE_UNAVAILABLE));
    }

    @Test
    void subjectNotFoundForbiddenAndInternalUseExistingProblemEnvelope() throws Exception {
        when(service.getCustomerSubject(any(), any())).thenReturn(Optional.empty());
        check(get(PATH, "personal"), 404, "NOT_FOUND");
        when(service.getCustomerSubject(any(), any())).thenReturn(null);
        check(get(PATH, "personal"), 500, "INTERNAL");
        for (var code : List.of(CustomerContextException.Code.FORBIDDEN, CustomerContextException.Code.INTERNAL)) {
            doThrow(new CustomerContextException(code)).when(service).getCustomerSubject(any(), any());
            check(get(PATH, "personal"), code == CustomerContextException.Code.FORBIDDEN ? 403 : 500, code.name());
        }
    }

    @Test
    void responseNeverAddsOptionalClaimOwnershipOrContentFields() throws Exception {
        var response = get(PATH, "personal"); four(check(response, 200, null));
        assertThat(response.body()).doesNotContain("numberMasked", "trial", "consentRequirements", "claimState", "owner", "role", "snapshot", "media", "risk");
    }

    @Test
    void subjectReadDoesNotRequireCursorKeyAndExistingRoutesStayUnchanged() throws Exception {
        four(check(get(PATH + "?cursor=bad", "personal"), 200, null));
        for (String path : List.of("/api/v1/customer/me", "/api/v1/customer/shares", "/api/v1/customer/business-numbers")) {
            check(get(path, null), 401, "UNAUTHENTICATED");
            assertThat(request("POST", path, "personal", HttpRequest.BodyPublishers.noBody()).statusCode()).isEqualTo(path.endsWith("shares") ? 422 : 404);
        }
    }
}
