package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.*;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

@SpringBootTest(classes = CustomerBusinessNumbersWebTests.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("web")
class CustomerBusinessNumbersWebTests {
    private static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID BINDING = UUID.fromString("80000000-0000-0000-0000-000000000001");
    private static final UUID SUBJECT = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static volatile boolean missingKey;
    @LocalServerPort private int port;
    @Autowired private CustomerBusinessNumberQueryService service;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({CustomerWebConfiguration.class, CustomerBusinessNumbersController.class, CustomerApiErrorController.class, Beans.class})
    static class Application { }

    @TestConfiguration(proxyBeanMethods = false)
    static class Beans {
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
                return new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, token.equals("wrong-audience") ? "foreign" : audience, workspace);
            };
        }
        @Bean
        CustomerBusinessNumberQueryService customerBusinessNumberQueryService() { return mock(CustomerBusinessNumberQueryService.class); }
        @Bean
        @org.springframework.context.annotation.Scope("prototype")
        CustomerBusinessNumberCursorCodec customerBusinessNumberCursorCodec() { return codec(missingKey); }
    }

    private static CustomerBusinessNumberCursorCodec codec(boolean missing) {
        var properties = new CustomerBusinessNumberCursorProperties();
        if (!missing) properties.setCursorHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
        return new CustomerBusinessNumberCursorCodec(properties);
    }

    @BeforeEach
    void resetState() {
        missingKey = false; reset(service);
        when(service.listCustomerBusinessNumbers(any(), any())).thenReturn(new CustomerBusinessNumberQueryService.Page(
                List.of(new CustomerBusinessNumberQueryService.Item(BINDING, SUBJECT, "138****0000", "MOBILE", "RETIRED", "SUSPENDED", "VERIFIED")), Optional.empty(), false));
    }

    private HttpResponse<String> request(String method, String path, String token, String... headers) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(java.time.Duration.ofSeconds(20));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        for (int index = 0; index < headers.length; index += 2) builder.header(headers[index], headers[index + 1]);
        try (var client = HttpClient.newHttpClient()) {
            return client.send(builder.method(method, HttpRequest.BodyPublishers.ofString("{\"ownerAccountId\":\"BODY_CANARY\"}")).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
    private HttpResponse<String> get(String query, String token) throws Exception { return request("GET", "/api/v1/customer/business-numbers" + query, token); }
    private JsonNode check(HttpResponse<String> response, int expected, String code) {
        assertThat(response.statusCode()).isEqualTo(expected);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("Pragma")).contains("no-cache");
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        var json = JsonMapper.builder().build().readTree(response.body());
        if (code != null) {
            assertThat(json.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
            assertThat(json.path("code").asString()).isEqualTo(code);
            assertThat(UUID.fromString(json.path("traceId").asString())).isNotNull();
        }
        assertThat(response.body()).doesNotContain("CANARY", "jdbc:", "ciphertext", "password", "ownerAccountId", "cursorHmacKey");
        return json;
    }
    private String cursor(Scope scope) { return codec(false).encode(scope, new Anchor(Instant.parse("2026-11-11T00:00:00Z"), BINDING)); }

    @Test
    void exactGetRouteReturnsOnlyFrozenPageAndSevenItemFields() throws Exception {
        var json = check(get("", "personal"), 200, null);
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
        assertThat(json.path("nextCursor").isNull()).isTrue(); assertThat(json.path("hasMore").asBoolean()).isFalse();
        var item = json.path("items").get(0);
        assertThat(item.propertyNames()).containsExactlyInAnyOrder("bindingId", "subjectId", "numberMasked", "numberType", "bindingStatus", "activationStatus", "realNameStatus");
        assertThat(item.path("bindingStatus").asString()).isEqualTo("RETIRED");
        assertThat(item.path("activationStatus").asString()).isEqualTo("SUSPENDED");
    }

    @Test
    void missingInvalidDuplicateAndWrongAudienceBearerAreUnauthenticated() throws Exception {
        for (String token : new String[]{null, "bad", "wrong-audience"}) check(get("?limit=bad&cursor=bad", token), 401, "UNAUTHENTICATED");
        check(request("GET", "/api/v1/customer/business-numbers", "personal", "Authorization", "Bearer personal"), 401, "UNAUTHENTICATED");
        verifyNoInteractions(service);
    }

    @Test
    void unavailableVerifierAndMissingOrMalformedWorkspaceAreUnavailable() throws Exception {
        for (String token : List.of("outage", "unselected", "null-kind", "personal-org", "org-null")) check(get("?limit=bad", token), 503, "SERVICE_UNAVAILABLE");
        verifyNoInteractions(service);
        for (int count : new int[]{0, 2}) {
            var beans = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
            for (int index = 0; index < count; index++) beans.registerSingleton("verifier" + index, (CustomerSessionVerifier) (bearer, audience) -> {
                throw new AssertionError("Ambiguous verifier must never run");
            });
            var filter = new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter());
            var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/customer/business-numbers");
            request.setServletPath("/api/v1/customer/business-numbers"); request.addHeader("Authorization", "Bearer personal");
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            filter.doFilter(request, response, (incoming, outgoing) -> { throw new AssertionError("Unavailable verifier must stop before controller"); });
            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(JsonMapper.builder().build().readTree(response.getContentAsString()).path("code").asString()).isEqualTo("SERVICE_UNAVAILABLE");
        }
    }

    @Test
    void personalAndOrganizationSelectorsComeOnlyFromVerifiedPrincipal() throws Exception {
        for (String token : List.of("personal", "organization")) {
            check(request("GET", "/api/v1/customer/business-numbers?accountId=" + UUID.randomUUID() + "&organizationId=" + UUID.randomUUID() + "&workspace=foreign", token,
                    "X-Account-Id", "HEADER_CANARY", "X-Organization-Id", "HEADER_CANARY", "Cookie", "workspace=COOKIE_CANARY"), 200, null);
            verify(service).listCustomerBusinessNumbers(argThat(caller -> caller.accountId().equals(ACCOUNT)
                    && (token.equals("personal") ? caller.selectedWorkspace().organizationId() == null : ORGANIZATION.equals(caller.selectedWorkspace().organizationId()))), any());
            clearInvocations(service);
        }
    }

    @Test
    void absentAndValidLimitsUseDefaultAndInclusiveBounds() throws Exception {
        for (String limit : List.of("", "1", "20", "100", "001")) {
            check(get(limit.isEmpty() ? "" : "?limit=" + limit, "personal"), 200, null);
            verify(service).listCustomerBusinessNumbers(any(), argThat(value -> value.effectiveLimit() == (limit.isEmpty() ? 20 : Integer.parseInt(limit))));
            clearInvocations(service);
        }
    }

    @Test
    void invalidAndDuplicateLimitsAreValidationFailed() throws Exception {
        for (String limit : List.of("", "0", "101", "-1", "%2B1", "%20", "1.0", "1e1", "%EF%BC%91", "999999999999999999999", "1&limit=1", "1&limit=2"))
            check(get("?limit=" + limit, "personal"), 422, "VALIDATION_FAILED");
        verifyNoInteractions(service);
    }

    @Test
    void malformedDuplicateAndTamperedCursorsAreCursorInvalid() throws Exception {
        String valid = cursor(new Scope(ACCOUNT, null));
        byte[] bytes = Base64.getUrlDecoder().decode(valid); bytes[92] ^= 1;
        for (String value : List.of("", "bad", valid + "&cursor=" + valid, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))) check(get("?cursor=" + value, "personal"), 400, "CURSOR_INVALID");
        verifyNoInteractions(service);
    }

    @Test
    void crossAccountPersonalOrganizationAndCrossOrganizationCursorsAreInvalid() throws Exception {
        for (var scope : List.of(new Scope(UUID.randomUUID(), null), new Scope(ACCOUNT, ORGANIZATION), new Scope(ACCOUNT, UUID.randomUUID()), new Scope(UUID.randomUUID(), ORGANIZATION)))
            check(get("?cursor=" + cursor(scope), "personal"), 400, "CURSOR_INVALID");
        check(get("?cursor=" + cursor(new Scope(ACCOUNT, null)), "organization"), 400, "CURSOR_INVALID");
        verifyNoInteractions(service);
    }

    @Test
    void authenticationWorkspaceLimitCursorAndLifecycleErrorPrecedenceIsExact() throws Exception {
        when(service.listCustomerBusinessNumbers(any(), any())).thenThrow(new CustomerContextException(CustomerContextException.Code.FORBIDDEN));
        missingKey = true;
        check(get("?limit=bad&cursor=bad", "bad"), 401, "UNAUTHENTICATED");
        check(get("?limit=bad&cursor=bad", "unselected"), 503, "SERVICE_UNAVAILABLE");
        check(get("?limit=bad&cursor=bad", "personal"), 422, "VALIDATION_FAILED");
        check(get("?cursor=bad", "personal"), 400, "CURSOR_INVALID");
        check(get("", "personal"), 503, "SERVICE_UNAVAILABLE");
        check(get("?cursor=" + cursor(new Scope(ACCOUNT, null)), "personal"), 503, "SERVICE_UNAVAILABLE");
        verifyNoInteractions(service);
        missingKey = false;
        check(get("?cursor=" + cursor(new Scope(UUID.randomUUID(), null)), "personal"), 400, "CURSOR_INVALID");
        check(get("", "personal"), 403, "FORBIDDEN");
    }

    @Test
    void serviceConfigurationAndUnexpectedFailuresUseSanitizedProblems() throws Exception {
        for (var code : List.of(CustomerContextException.Code.SERVICE_UNAVAILABLE, CustomerContextException.Code.INTERNAL)) {
            doThrow(new CustomerContextException(code)).when(service).listCustomerBusinessNumbers(any(), any());
            check(get("", "personal"), code == CustomerContextException.Code.INTERNAL ? 500 : 503, code.name());
        }
        doThrow(new IllegalStateException("PASSWORD_SQL_KEY_CANARY")).when(service).listCustomerBusinessNumbers(any(), any());
        check(get("", "personal"), 500, "INTERNAL");
    }

    @Test
    void nonGetAndNonCanonicalRoutesStayNotFoundWithoutNewSurface() throws Exception {
        for (String method : List.of("POST", "PUT", "DELETE", "PATCH", "OPTIONS")) check(request(method, "/api/v1/customer/business-numbers", null), 404, "NOT_FOUND");
        assertThat(request("HEAD", "/api/v1/customer/business-numbers", null).statusCode()).isEqualTo(404);
        for (String path : List.of("/api/v1/customer/business-numbers/", "/api/v1/customer/business-numbers/extra", "/api/v1/customer/business%2Dnumbers")) check(request("GET", path, null), 404, "NOT_FOUND");
        verifyNoInteractions(service);
    }

    @Test
    void successAndProblemsNeverLeakSecretsAndRetainNoStoreHeaders() throws Exception {
        check(get("", "personal"), 200, null);
        var denied = get("", null); check(denied, 401, "UNAUTHENTICATED");
        assertThat(denied.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        check(request("GET", "/api/v1/customer/business-numbers?cursor=SECRET_CANARY", "personal", "X-Trace-Id", "TRACE_CANARY"), 400, "CURSOR_INVALID");
    }
}
