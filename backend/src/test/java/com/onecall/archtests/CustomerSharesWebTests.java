package com.onecall.archtests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace;
import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.CustomerContextService;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerificationException;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier;
import com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession;
import com.onecall.core.customer_iam.internal.web.CustomerApiErrorController;
import com.onecall.core.customer_iam.internal.web.CustomerMeController;
import com.onecall.core.customer_iam.internal.web.CustomerSharesController;
import com.onecall.core.customer_iam.internal.web.CustomerWebConfiguration;
import com.onecall.core.OneCallApplication;
import com.onecall.core.sharing.internal.application.CustomerShareQueryService;
import com.onecall.core.sharing.internal.application.ShareCursor;
import com.onecall.core.sharing.internal.application.SharePage;
import com.onecall.core.sharing.internal.application.SharePageRequest;
import com.onecall.core.sharing.internal.application.ShareQueryException;
import com.onecall.core.sharing.internal.application.ShareSummary;
import com.onecall.core.sharing.internal.application.TrustedShareCaller;
import com.onecall.core.sharing.internal.command.CustomerShareCommand;
import com.onecall.core.sharing.internal.command.CustomerShareCommandResult;
import com.onecall.core.sharing.internal.command.CustomerShareCommandService;
import com.onecall.core.sharing.internal.command.ShareCommandCaller;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = CustomerSharesWebTests.WebTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.lazy-initialization=false")
@ActiveProfiles("web")
class CustomerSharesWebTests {
    private static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID ORGANIZATION = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ORGANIZATION = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final AtomicInteger VERIFICATIONS = new AtomicInteger();
    private static final AtomicInteger SELECTION = new AtomicInteger();
    private static volatile ShareQueryException.Code FAILURE;
    private static volatile boolean UNAVAILABLE_VERIFIER;
    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private CustomerShareQueryService service;

    @Autowired
    private ApplicationContext context;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({CustomerWebConfiguration.class, CustomerSharesController.class, CustomerMeController.class,
            CustomerApiErrorController.class, WebBeans.class})
    static class WebTestApplication { }

    @TestConfiguration(proxyBeanMethods = false)
    static class WebBeans {
        @Bean
        CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> {
                VERIFICATIONS.incrementAndGet();
                if (UNAVAILABLE_VERIFIER || bearer.value().equals("outage")) throw new IllegalStateException("VERIFIER_CANARY");
                if (bearer.value().equals("rejected")) throw new CustomerSessionVerificationException(
                        CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                if (bearer.value().equals("malformed")) return new VerifiedCustomerSession(null, null, audience);
                var workspace = switch (bearer.value()) {
                    case "organization" -> new SelectedCustomerWorkspace(
                            com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.ORGANIZATION, ORGANIZATION);
                    case "bad-null-type" -> new SelectedCustomerWorkspace(null, null);
                    case "bad-personal-org" -> new SelectedCustomerWorkspace(
                            com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.PERSONAL, ORGANIZATION);
                    case "bad-org-null" -> new SelectedCustomerWorkspace(
                            com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.ORGANIZATION, null);
                    case "unselected" -> null;
                    default -> new SelectedCustomerWorkspace(
                            com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.PERSONAL, null);
                };
                return new VerifiedCustomerSession(bearer.value().equals("other") ? OTHER_ACCOUNT : ACCOUNT,
                        TrustedCustomerCaller.Channel.WEB, bearer.value().equals("wrong-audience") ? "other-audience" : audience, workspace);
            };
        }

        @Bean
        CustomerShareQueryService customerShareQueryService() { return createServiceMock(); }

        @Bean
        CustomerContextService customerContextService() {
            var mockService = mock(CustomerContextService.class);
            when(mockService.getCustomerMe(any())).thenAnswer(invocation -> {
                TrustedCustomerCaller caller = invocation.getArgument(0);
                return new CustomerContext(new CustomerContext.AccountSummary(caller.accountId(), "Customer", CustomerContext.AccountStatus.ACTIVE),
                        List.of(new CustomerContext.WorkspaceSummary(CustomerContext.WorkspaceType.PERSONAL, null, null, null,
                                CustomerContext.WorkspaceStatus.ACTIVE)), caller.channel());
            });
            return mockService;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class WebOnlyBeans {
        @Bean
        CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                    audience, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null));
        }

        @Bean
        CustomerContextService customerContextService() {
            var service = mock(CustomerContextService.class);
            when(service.getCustomerMe(any())).thenReturn(new CustomerContext(
                    new CustomerContext.AccountSummary(ACCOUNT, "Customer", CustomerContext.AccountStatus.ACTIVE),
                    List.of(), TrustedCustomerCaller.Channel.WEB));
            return service;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MissingVerifierBeans {
        @Bean
        CustomerContextService customerContextService() { return mock(CustomerContextService.class); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AmbiguousVerifierBeans {
        @Bean("verifierOne")
        CustomerSessionVerifier verifierOne() { return healthyVerifier(); }

        @Bean("verifierTwo")
        CustomerSessionVerifier verifierTwo() { return healthyVerifier(); }

        @Bean
        CustomerShareQueryService customerShareQueryService() { return createServiceMock(); }

        @Bean
        CustomerContextService customerContextService() { return mock(CustomerContextService.class); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class VerifierConstructionFailureBeans {
        @Bean
        @Lazy
        CustomerSessionVerifier customerSessionVerifier() { throw new IllegalStateException("VERIFIER_CONSTRUCTION_SECRET"); }

        @Bean
        CustomerShareQueryService customerShareQueryService() { return createServiceMock(); }

        @Bean
        CustomerContextService customerContextService() { return mock(CustomerContextService.class); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AmbiguousQueryBeans {
        @Bean
        CustomerSessionVerifier customerSessionVerifier() { return healthyVerifier(); }

        @Bean("queryOne")
        @Qualifier("customerShareQueryService")
        CustomerShareQueryService queryOne() { return mock(CustomerShareQueryService.class); }

        @Bean("queryTwo")
        @Qualifier("customerShareQueryService")
        CustomerShareQueryService queryTwo() { return mock(CustomerShareQueryService.class); }

        @Bean
        CustomerContextService customerContextService() { return mock(CustomerContextService.class); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class QueryConstructionFailureBeans {
        @Bean
        CustomerSessionVerifier customerSessionVerifier() { return healthyVerifier(); }

        @Bean("customerShareQueryService")
        @Lazy
        CustomerShareQueryService customerShareQueryService() {
            throw new IllegalStateException("QUERY_CONSTRUCTION_SECRET");
        }

        @Bean
        CustomerContextService customerContextService() { return mock(CustomerContextService.class); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class QueryInvocationFailureBeans {
        @Bean
        CustomerSessionVerifier customerSessionVerifier() { return healthyVerifier(); }

        @Bean("customerShareQueryService")
        CustomerShareQueryService customerShareQueryService() {
            var service = mock(CustomerShareQueryService.class);
            when(service.listCustomerShares(any(), any())).thenAnswer(invocation -> {
                SELECTION.incrementAndGet();
                throw new IllegalStateException("QUERY_INVOCATION_SECRET");
            });
            return service;
        }

        @Bean
        CustomerContextService customerContextService() { return mock(CustomerContextService.class); }
    }

    private static CustomerSessionVerifier healthyVerifier() {
        return (bearer, audience) -> new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                audience, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null));
    }

    @BeforeEach
    void resetState() {
        VERIFICATIONS.set(0);
        SELECTION.set(0);
        FAILURE = null;
        UNAVAILABLE_VERIFIER = false;
        reset(service);
        when(service.listCustomerShares(any(), any())).thenAnswer(invocation -> {
            SELECTION.incrementAndGet();
            if (FAILURE != null) throw new ShareQueryException(FAILURE);
            TrustedShareCaller caller = invocation.getArgument(0);
            SharePageRequest request = invocation.getArgument(1);
            var item = summary("share-one", Instant.parse("2026-01-02T03:04:05.123456Z"));
            Optional<ShareCursor> next = request.limit().orElse(20) == 1
                    ? Optional.of(new ShareCursor(caller.accountId(), caller.organizationId(), item.validFrom(), item.shareId()))
                    : Optional.empty();
            return new SharePage(List.of(item), next, next.isPresent());
        });
    }

    @Test
    void authenticatedPersonalAndOrganizationGetsUseTrustedCreatorScope() throws Exception {
        var personal = send("GET", "/api/v1/customer/shares", "personal", "application/json", null);
        var organization = send("GET", "/api/v1/customer/shares?limit=1", "organization", "application/json", null);
        assertThat(personal.statusCode()).isEqualTo(200);
        assertThat(personal.uri().toString()).isEqualTo("http://127.0.0.1:" + port + "/api/v1/customer/shares");
        assertThat(personal.headers().firstValue("content-type").orElse("")).startsWith("application/json");
        assertThat(personal.body()).contains("\"shareId\":\"share-one\"", "\"nextCursor\":null", "\"hasMore\":false");
        assertThat(organization.statusCode()).isEqualTo(200);
        assertThat(organization.uri().toString()).isEqualTo("http://127.0.0.1:" + port + "/api/v1/customer/shares?limit=1");
        assertThat(organization.headers().firstValue("content-type").orElse("")).startsWith("application/json");
        assertThat(organization.body()).contains("\"shareId\":\"share-one\"", "\"hasMore\":true");
        var calls = mockingDetails(service).getInvocations().stream().filter(call -> call.getMethod().getName().equals("listCustomerShares")).toList();
        assertThat(calls).hasSize(2);
        assertThat(calls.get(0).getArgument(0, TrustedShareCaller.class)).isEqualTo(new TrustedShareCaller(ACCOUNT, Optional.empty()));
        assertThat(calls.get(1).getArgument(0, TrustedShareCaller.class)).isEqualTo(new TrustedShareCaller(ACCOUNT, Optional.of(ORGANIZATION)));
        assertThat(SELECTION).hasValue(2);
    }

    @Test
    void missingInvalidAndDuplicateBearersFailBeforePageParsing() throws Exception {
        for (String bearer : new String[]{null, "rejected", "malformed"}) {
            var response = send("GET", "/api/v1/customer/shares?limit=0", bearer, null, null);
            assertProblem(response, bearer == null || "rejected".equals(bearer) ? 401 : 503,
                    bearer == null || "rejected".equals(bearer) ? "UNAUTHENTICATED" : "SERVICE_UNAVAILABLE",
                    bearer == null || "rejected".equals(bearer) ? "Authentication required" : "Service unavailable");
        }
        assertProblem(send("GET", "/api/v1/customer/shares?limit=0", "wrong-audience", null, null),
                401, "UNAUTHENTICATED", "Authentication required");
        var outage = send("GET", "/api/v1/customer/shares?limit=0", "outage", null, null);
        assertProblem(outage, 503, "SERVICE_UNAVAILABLE", "Service unavailable");
        assertThat(outage.body()).doesNotContain("VERIFIER_CANARY", "IllegalStateException");
        var duplicateAuthorization = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                + "/api/v1/customer/shares?limit=0"))
                .header("Authorization", "Bearer personal")
                .header("Authorization", "Bearer organization")
                .GET().build();
        var duplicateResponse = client.send(duplicateAuthorization, HttpResponse.BodyHandlers.ofString());
        assertProblem(duplicateResponse, 401, "UNAUTHENTICATED", "Authentication required");
        assertThat(SELECTION).hasValue(0);
        assertThat(VERIFICATIONS).hasValue(4);
    }

    @Test
    void clientSelectorsAndRequestAttributesCannotOverrideTrustedScope() throws Exception {
        SELECTION.set(0);
        VERIFICATIONS.set(0);
        var selectorBody = "{\"accountId\":\"" + OTHER_ACCOUNT + "\",\"organizationId\":\"" + OTHER_ORGANIZATION
                + "\",\"ownerAccountId\":\"" + OTHER_ACCOUNT
                + "\",\"role\":\"ADMIN\",\"channel\":\"MINIAPP\",\"workspaceId\":\"" + OTHER_ORGANIZATION + "\"}";
        var selectorRequest = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port
                        + "/api/v1/customer/shares?accountId=" + OTHER_ACCOUNT + "&organizationId=" + OTHER_ORGANIZATION
                        + "&ownerAccountId=" + OTHER_ACCOUNT + "&role=ADMIN&channel=MINIAPP&workspaceId=" + OTHER_ORGANIZATION))
                .header("Authorization", "Bearer personal")
                .header("X-Account-Id", OTHER_ACCOUNT.toString())
                .header("X-Organization-Id", OTHER_ORGANIZATION.toString())
                .header("X-Owner-Account-Id", OTHER_ACCOUNT.toString())
                .header("X-Role", "ADMIN")
                .header("X-Channel", "MINIAPP")
                .header("X-Workspace-Id", OTHER_ORGANIZATION.toString())
                .header("Cookie", "accountId=" + OTHER_ACCOUNT + "; organizationId=" + OTHER_ORGANIZATION
                        + "; ownerAccountId=" + OTHER_ACCOUNT + "; role=ADMIN; channel=MINIAPP; workspaceId=" + OTHER_ORGANIZATION)
                .header("Content-Type", "application/json")
                .method("GET", HttpRequest.BodyPublishers.ofString(selectorBody)).build();
        var response = client.send(selectorRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(mockingDetails(service).getInvocations().iterator().next().getArgument(0, TrustedShareCaller.class))
                .isEqualTo(new TrustedShareCaller(ACCOUNT, Optional.empty()));
        assertThat(SELECTION).hasValue(1);
        assertThat(VERIFICATIONS).hasValue(1);
        assertThat(send("GET", "/api/v1/customer/shares", "unselected", null, null).statusCode()).isEqualTo(503);
        assertThat(SELECTION).hasValue(1);

        var boundary = context.getBeansOfType(org.springframework.boot.web.servlet.FilterRegistrationBean.class).values().stream()
                .filter(bean -> bean.getFilter() instanceof com.onecall.core.customer_iam.internal.web.CustomerBoundaryFilter)
                .map(bean -> (com.onecall.core.customer_iam.internal.web.CustomerBoundaryFilter) bean.getFilter())
                .findFirst().orElseThrow();
        var probe = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/customer/shares");
        probe.setServletPath("/api/v1/customer/shares");
        probe.addHeader("Authorization", "Bearer personal");
        probe.setUserPrincipal(() -> "spoofed-user");
        probe.setAttribute("accountId", OTHER_ACCOUNT);
        probe.setAttribute("organizationId", OTHER_ORGANIZATION);
        probe.setAttribute("role", "ADMIN");
        probe.setAttribute("channel", "MINIAPP");
        probe.setAttribute("workspaceId", OTHER_ORGANIZATION);
        var probeResponse = new org.springframework.mock.web.MockHttpServletResponse();
        var probeChain = mock(jakarta.servlet.FilterChain.class);
        doAnswer(invocation -> {
            var downstream = invocation.getArgument(0, jakarta.servlet.http.HttpServletRequest.class);
            assertThat(downstream.getUserPrincipal())
                    .isInstanceOf(com.onecall.core.customer_iam.internal.web.CustomerRequestPrincipal.class);
            var trusted = (com.onecall.core.customer_iam.internal.web.CustomerRequestPrincipal) downstream.getUserPrincipal();
            assertThat(trusted.caller()).isEqualTo(new TrustedCustomerCaller(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                    new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
            assertThat(downstream.getUserPrincipal().getName()).isNotEqualTo("spoofed-user");
            return null;
        }).when(probeChain).doFilter(any(), any());
        boundary.doFilter(probe, probeResponse, probeChain);
        verify(probeChain).doFilter(any(), any());
        assertThat(VERIFICATIONS).hasValue(3);
    }

    @Test
    void limitAndCursorValidationHaveDeterministicPrecedence() throws Exception {
        for (String query : List.of("?limit=0", "?limit=101", "?limit=1&limit=2", "?limit=", "?limit=" + "1".repeat(512),
                "?cursor=x&cursor=y", "?cursor=" + "x".repeat(512), "?cursor=" + "x".repeat(513))) {
            int expected = query.contains("cursor") ? 400 : 422;
            assertThat(send("GET", "/api/v1/customer/shares" + query, "personal", null, null).statusCode()).isEqualTo(expected);
        }
        assertThat(send("GET", "/api/v1/customer/shares?limit=0&cursor=x", "personal", null, null).statusCode()).isEqualTo(422);
        assertThat(SELECTION).hasValue(0);
        var firstPage = send("GET", "/api/v1/customer/shares?limit=1", "personal", null, null);
        assertThat(firstPage.statusCode()).isEqualTo(200);
        var firstPageJson = tools.jackson.databind.json.JsonMapper.builder().build().readTree(firstPage.body());
        String cursor = firstPageJson.get("nextCursor").asString();
        assertThat(cursor).isNotBlank();
        assertThat(send("GET", "/api/v1/customer/shares?limit=2&cursor="
                + java.net.URLEncoder.encode(cursor, java.nio.charset.StandardCharsets.UTF_8), "personal", null, null).statusCode()).isEqualTo(200);
        assertThat(SELECTION).hasValue(2);
        int afterCursorCreation = SELECTION.get();
        var crossContext = send("GET", "/api/v1/customer/shares?cursor="
                + java.net.URLEncoder.encode(cursor, java.nio.charset.StandardCharsets.UTF_8), "other", null, null);
        assertThat(crossContext.statusCode()).isEqualTo(400);
        assertThat(SELECTION).hasValue(afterCursorCreation);
        assertThat(afterCursorCreation).isEqualTo(2);
        assertThat(send("GET", "/api/v1/customer/shares?limit=01", "personal", null, null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/customer/shares?limit=100", "personal", null, null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/customer/shares?limit=1", "personal", null, null).statusCode()).isEqualTo(200);
        assertThat(SELECTION).hasValue(afterCursorCreation + 3);
    }

    @Test
    void queryErrorsUseExactClosedProblems() throws Exception {
        for (var entry : List.of(new Object[]{ShareQueryException.Code.FORBIDDEN, 403},
                new Object[]{ShareQueryException.Code.VALIDATION_FAILED, 422},
                new Object[]{ShareQueryException.Code.CURSOR_INVALID, 400},
                new Object[]{ShareQueryException.Code.SERVICE_UNAVAILABLE, 503},
                new Object[]{ShareQueryException.Code.INTERNAL, 500})) {
            FAILURE = (ShareQueryException.Code) entry[0];
            var response = send("GET", "/api/v1/customer/shares", "personal", null, null);
            int status = (int) entry[1];
            String code = ((ShareQueryException.Code) entry[0]).name();
            String title = switch (code) {
                case "FORBIDDEN" -> "Access denied";
                case "VALIDATION_FAILED" -> "Request validation failed";
                case "CURSOR_INVALID" -> "Invalid cursor";
                case "SERVICE_UNAVAILABLE" -> "Service unavailable";
                default -> "Internal error";
            };
            assertProblem(response, status, code, title);
            assertThat(response.body()).doesNotContain("Share query", "stackTrace");
            assertThat(response.body()).doesNotContain("items", "share-one");
        }
        assertThat(context.getBean("customerShareQueryService")).isSameAs(service);

        var writerResponse = new org.springframework.mock.web.MockHttpServletResponse();
        UUID traceId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        new com.onecall.core.customer_iam.internal.web.CustomerProblemWriter().write(writerResponse,
                com.onecall.core.customer_iam.internal.application.CustomerContextException.Code.RATE_LIMITED, traceId);
        assertThat(writerResponse.getStatus()).isEqualTo(429);
        assertThat(writerResponse.getContentType()).isEqualTo("application/problem+json");
        var rateLimitProblem = tools.jackson.databind.json.JsonMapper.builder().build().readTree(writerResponse.getContentAsString());
        assertThat(rateLimitProblem.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(rateLimitProblem.get("type").asString()).isEqualTo("about:blank");
        assertThat(rateLimitProblem.get("title").asString()).isEqualTo("Too many requests");
        assertThat(rateLimitProblem.get("status").asInt()).isEqualTo(429);
        assertThat(rateLimitProblem.get("code").asString()).isEqualTo("RATE_LIMITED");
        assertThat(rateLimitProblem.get("traceId").asString()).isEqualTo(traceId.toString());
        assertThat(writerResponse.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(writerResponse.getHeader("Pragma")).isEqualTo("no-cache");
        assertThat(writerResponse.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    private static void assertProblem(HttpResponse<String> response, int status, String code, String title) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        if (status == 401) assertThat(response.headers().firstValue("www-authenticate")).contains("Bearer");
        else assertThat(response.headers().firstValue("www-authenticate")).isEmpty();
        assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("application/problem+json");
        assertThat(response.headers().firstValue("cache-control").orElse("")).isEqualTo("no-store");
        assertThat(response.headers().firstValue("pragma").orElse("")).isEqualTo("no-cache");
        assertThat(response.headers().firstValue("x-content-type-options").orElse("")).isEqualTo("nosniff");
        var problem = tools.jackson.databind.json.JsonMapper.builder().build().readTree(response.body());
        assertThat(problem.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(problem.get("type").asString()).isEqualTo("about:blank");
        assertThat(problem.get("title").asString()).isEqualTo(title);
        assertThat(problem.get("status").asInt()).isEqualTo(status);
        assertThat(problem.get("code").asString()).isEqualTo(code);
        assertThat(problem.get("traceId").asString()).matches("[0-9a-f-]{36}");
    }

    @Test
    void exactGetAndPostRoutesPassHighestPriorityFilter() throws Exception {
        try (var prefixed = new org.springframework.boot.builder.SpringApplicationBuilder(OneCallApplication.class, WebBeans.class)
                .profiles("web").properties("server.port=0", "server.servlet.context-path=/pa12",
                        "spring.main.banner-mode=off", "spring.main.allow-bean-definition-overriding=true").run()) {
            int prefixedPort = ((org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext) prefixed)
                    .getWebServer().getPort();
            var personal = request(prefixedPort, "GET", "/pa12/api/v1/customer/shares", "personal", null, null);
            var organization = request(prefixedPort, "GET", "/pa12/api/v1/customer/shares?limit=1", "organization", null, null);
            assertThat(personal.statusCode()).isEqualTo(200);
            assertThat(personal.body()).isEqualTo("{\"items\":[{\"shareId\":\"context-share\","
                    + "\"evidenceId\":\"30000000-0000-0000-0000-000000000001\","
                    + "\"verificationMode\":\"NONE\",\"status\":\"ACTIVE\","
                    + "\"validFrom\":\"2026-01-02T03:04:05.123456Z\","
                    + "\"expiresAt\":\"2026-01-02T04:04:05.123456Z\",\"maxAdmissions\":null,"
                    + "\"admissionsUsed\":0,\"downloadAllowed\":false,\"codeGeneration\":0,"
                    + "\"revocationGeneration\":0}],\"nextCursor\":null,\"hasMore\":false}");
            assertThat(organization.statusCode()).isEqualTo(200);
            assertThat(organization.body()).contains("\"shareId\":\"context-share\"", "\"hasMore\":true");
            assertThat(SELECTION).hasValue(2);
            assertThat(VERIFICATIONS).hasValue(2);
            var calls = mockingDetails(prefixed.getBean(CustomerShareQueryService.class)).getInvocations().stream()
                    .filter(call -> call.getMethod().getName().equals("listCustomerShares")).toList();
            assertThat(calls).hasSize(2);
            assertThat(calls.get(0).getArgument(0, TrustedShareCaller.class))
                    .isEqualTo(new TrustedShareCaller(ACCOUNT, Optional.empty()));
            assertThat(calls.get(1).getArgument(0, TrustedShareCaller.class))
                    .isEqualTo(new TrustedShareCaller(ACCOUNT, Optional.of(ORGANIZATION)));
            for (String path : List.of("/pa12/api/v1/customer/shares/", "/pa12/api/v1/customer/%73hares",
                    "/pa12/api/v1/customer/shares;v=1", "/pa12/error")) {
                SELECTION.set(0);
                VERIFICATIONS.set(0);
                assertThat(request(prefixedPort, "GET", path, "personal", null, null).statusCode()).isEqualTo(404);
                assertThat(SELECTION).hasValue(0);
                assertThat(VERIFICATIONS).hasValue(0);
            }
            for (String method : List.of("HEAD", "OPTIONS", "PUT", "PATCH", "DELETE")) {
                SELECTION.set(0);
                VERIFICATIONS.set(0);
                assertThat(request(prefixedPort, method, "/pa12/api/v1/customer/shares", "personal", null, null).statusCode())
                        .as("method %s", method).isEqualTo(404);
                assertThat(SELECTION).hasValue(0);
                assertThat(VERIFICATIONS).hasValue(0);
            }
            SELECTION.set(0);
            VERIFICATIONS.set(0);
            var revoke = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + prefixedPort
                            + "/pa12/api/v1/customer/shares/share:fixture/revocations"))
                    .header("Authorization", "Bearer personal").header("Idempotency-Key", "revoke-key")
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertProblem(revoke, 503, "SERVICE_UNAVAILABLE", "Service unavailable");
            assertThat(SELECTION).hasValue(0);
            assertThat(VERIFICATIONS).hasValue(1);
            for (String suffix : List.of("revocations", "code-rotations")) {
                String transition = "/pa12/api/v1/customer/shares/share:fixture/" + suffix;
                for (String method : suffix.equals("revocations") ? List.of("GET", "HEAD", "OPTIONS", "PUT", "PATCH", "DELETE")
                        : List.of("GET", "HEAD", "OPTIONS", "PUT", "PATCH", "DELETE", "POST")) {
                    SELECTION.set(0);
                    VERIFICATIONS.set(0);
                    var rejected = request(prefixedPort, method, transition, "personal", null, null);
                    if (method.equals("HEAD")) {
                        assertThat(rejected.statusCode()).isEqualTo(404);
                        assertThat(rejected.body()).isEmpty();
                        assertThat(rejected.headers().firstValue("content-type").orElse("")).startsWith("application/problem+json");
                        assertThat(rejected.headers().firstValue("cache-control")).contains("no-store");
                        assertThat(rejected.headers().firstValue("pragma")).contains("no-cache");
                        assertThat(rejected.headers().firstValue("x-content-type-options")).contains("nosniff");
                    } else assertProblem(rejected, 404, "NOT_FOUND", "Not found");
                    assertThat(SELECTION).hasValue(0);
                    assertThat(VERIFICATIONS).hasValue(0);
                }
                String encodedSuffix = suffix.equals("revocations") ? "%72evocations" : "%63ode-rotations";
                for (String path : List.of(transition + "/extra", transition + "/",
                        "/pa12/api/v1/customer/shares/%73hare:fixture/" + suffix,
                        "/pa12/api/v1/customer/%73hares/share:fixture/" + suffix,
                        "/pa12/api/v1/customer/shares/share:fixture;v=1/" + suffix,
                        "/pa12/api/v1/customer/shares/share:fixture/" + suffix.substring(0, suffix.length() - 1),
                        "/pa12/api/v1/customer/shares/share:fixture/" + encodedSuffix)) {
                    SELECTION.set(0);
                    VERIFICATIONS.set(0);
                    assertProblem(request(prefixedPort, "POST", path, "personal", null, null), 404, "NOT_FOUND", "Not found");
                    assertThat(SELECTION).hasValue(0);
                    assertThat(VERIFICATIONS).hasValue(0);
                }
            }
            var create = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + prefixedPort + "/pa12/api/v1/customer/shares"))
                    .header("Authorization", "Bearer personal").header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"creationRequestId\":\"route\",\"evidenceId\":\"30000000-0000-0000-0000-000000000001\","
                            + "\"verificationMode\":\"NONE\",\"expiresAt\":\"2027-01-01T00:00:00Z\",\"downloadAllowed\":false}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertProblem(create, 503, "SERVICE_UNAVAILABLE", "Service unavailable");
            SELECTION.set(0);
            VERIFICATIONS.set(0);
            var me = request(prefixedPort, "GET", "/pa12/api/v1/customer/me", "personal", null, null);
            assertThat(me.statusCode()).isEqualTo(200);
            assertThat(me.body()).isEqualTo("{\"account\":{\"accountId\":\"" + ACCOUNT
                    + "\",\"displayName\":\"Customer\",\"status\":\"ACTIVE\"},\"workspaces\":[{\"workspaceType\":\"PERSONAL\","
                    + "\"status\":\"ACTIVE\"}],"
                    + "\"channel\":\"WEB\"}");
            assertThat(me.body()).doesNotContain("selectedWorkspace");
            assertThat(SELECTION).hasValue(0);
            assertThat(VERIFICATIONS).hasValue(1);
            for (String path : List.of("/api/v1/customer/me", "/api/v1/customer/shares")) {
                SELECTION.set(0);
                VERIFICATIONS.set(0);
                assertThat(request(prefixedPort, "GET", path, "personal", null, null).statusCode()).isEqualTo(404);
                assertThat(SELECTION).hasValue(0);
                assertThat(VERIFICATIONS).hasValue(0);
            }

            var boundary = prefixed.getBeansOfType(org.springframework.boot.web.servlet.FilterRegistrationBean.class).values().stream()
                    .filter(bean -> bean.getFilter() instanceof com.onecall.core.customer_iam.internal.web.CustomerBoundaryFilter)
                    .map(bean -> (com.onecall.core.customer_iam.internal.web.CustomerBoundaryFilter) bean.getFilter())
                    .findFirst().orElseThrow();
            var traceRequest = new org.springframework.mock.web.MockHttpServletRequest("TRACE", "/pa12/api/v1/customer/shares");
            traceRequest.setContextPath("/pa12");
            traceRequest.setServletPath("/api/v1/customer/shares");
            var traceResponse = new org.springframework.mock.web.MockHttpServletResponse();
            var traceChain = mock(jakarta.servlet.FilterChain.class);
            boundary.doFilter(traceRequest, traceResponse, traceChain);
            assertThat(traceResponse.getStatus()).isEqualTo(404);
            verifyNoInteractions(traceChain);
            var commands = mock(CustomerShareCommandService.class);
            var item = summary("share:fixture", Instant.parse("2026-01-02T03:04:05.123456Z"));
            when(commands.execute(any(), any())).thenReturn(new CustomerShareCommandResult.Revoked(new CustomerShareCommandResult.ShareState(
                    item.shareId(), item.evidenceId(), item.verificationMode(), ShareSummary.ShareStatus.REVOKED, item.validFrom(), item.expiresAt(),
                    item.maxAdmissions(), item.admissionsUsed(), item.downloadAllowed(), item.codeGeneration(), 1)));
            prefixed.getBeanFactory().registerSingleton("customerShareCommandService", commands);
            var available = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + prefixedPort
                            + "/pa12/api/v1/customer/shares/share:fixture/revocations"))
                    .header("Authorization", "Bearer personal").header("Idempotency-Key", "revoke-key")
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertThat(available.statusCode()).isEqualTo(200);
            assertThat(available.body()).contains("\"status\":\"REVOKED\"", "\"revocationGeneration\":1")
                    .doesNotContain("capabilityToken", "extractionCode", "revoke-key");
            verify(commands).execute(any(CustomerShareCommand.Revoke.class), org.mockito.ArgumentMatchers.eq(new ShareCommandCaller(ACCOUNT, Optional.empty())));
        }
    }

    @Test
    void meRemainsCompatibleWithLegacyAndSelectedSessions() throws Exception {
        assertThat(send("GET", "/api/v1/customer/shares", "bad-null-type", null, null).statusCode()).isEqualTo(503);
        assertThat(send("GET", "/api/v1/customer/shares", "bad-personal-org", null, null).statusCode()).isEqualTo(503);
        assertThat(send("GET", "/api/v1/customer/shares", "bad-org-null", null, null).statusCode()).isEqualTo(503);
        assertThat(SELECTION).hasValue(0);
        var baseline = send("GET", "/api/v1/customer/me", "personal", null, null);
        assertThat(baseline.statusCode()).isEqualTo(200);
        for (String bearer : List.of("unselected", "bad-null-type", "bad-personal-org", "bad-org-null", "organization")) {
            var selected = send("GET", "/api/v1/customer/me", bearer, null, null);
            assertThat(selected.statusCode()).isEqualTo(200);
            assertThat(selected.body()).isEqualTo(baseline.body());
            assertThat(selected.body()).doesNotContain("selectedWorkspace");
        }
        assertThat(SELECTION).hasValue(0);
    }

    @Test
    void concurrentRequestsKeepAccountAndSelectedWorkspaceIsolated() throws Exception {
        var requests = List.of("personal", "organization", "other", "personal", "organization", "other",
                "personal", "organization", "other", "personal", "organization", "other");
        var executor = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int index = 0; index < requests.size(); index++) {
                String bearer = requests.get(index);
                int limit = index + 2;
                futures.add(executor.submit(() -> assertThat(send("GET", "/api/v1/customer/shares?limit=" + limit, bearer, null, null).statusCode()).isEqualTo(200)));
            }
            for (var future : futures) future.get(20, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
        assertThat(SELECTION).hasValue(12);
        var invocations = mockingDetails(service).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("listCustomerShares")).toList();
        assertThat(invocations).hasSize(requests.size());
        assertThat(invocations.stream().map(invocation -> ((SharePageRequest) invocation.getArgument(1)).limit().orElseThrow()))
                .containsExactlyInAnyOrderElementsOf(java.util.stream.IntStream.range(2, requests.size() + 2).boxed().toList());
        for (var invocation : invocations) {
            TrustedShareCaller caller = invocation.getArgument(0);
            SharePageRequest pageRequest = invocation.getArgument(1);
            int index = pageRequest.limit().orElseThrow() - 2;
            String bearer = requests.get(index);
            assertThat(caller).as("server-generated account/scope for request %s (%s)", index, bearer)
                    .isEqualTo(new TrustedShareCaller(bearer.equals("other") ? OTHER_ACCOUNT : ACCOUNT,
                            bearer.equals("organization") ? Optional.of(ORGANIZATION) : Optional.empty()));
        }
    }

    @Test
    void negotiationAndBufferedFailuresPreservePa11Boundary() throws Exception {
        for (String accept : List.of("application/json", "*/*"))
            assertThat(send("GET", "/api/v1/customer/shares", "personal", accept, null).statusCode()).isEqualTo(200);
        int beforeRejectedAccepts = SELECTION.get();
        for (String accept : List.of("text/html", "application/json;q=0", "application/json;q=bad"))
            assertThat(send("GET", "/api/v1/customer/shares?limit=0", "personal", accept, null).statusCode()).isEqualTo(500);
        for (String bearer : List.of("unselected", "bad-null-type", "bad-personal-org", "bad-org-null")) {
            assertThat(send("GET", "/api/v1/customer/shares?limit=0", bearer, "text/html", null).statusCode()).isEqualTo(500);
            assertThat(send("GET", "/api/v1/customer/shares?limit=0", bearer, "application/json", null).statusCode()).isEqualTo(503);
        }
        assertThat(SELECTION).hasValue(beforeRejectedAccepts);

        try (var faultContext = new org.springframework.boot.builder.SpringApplicationBuilder(
                OneCallApplication.class, SerializerFaultBeans.class)
                .profiles("web").properties("server.port=0", "spring.main.banner-mode=off",
                        "spring.main.allow-bean-definition-overriding=true").run()) {
            int faultPort = ((org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext) faultContext)
                    .getWebServer().getPort();
            var response = request(faultPort, "GET", "/api/v1/customer/shares", "personal", "application/json", null);
            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(response.headers().firstValue("content-type").orElse("")).startsWith("application/problem+json");
            var problem = tools.jackson.databind.json.JsonMapper.builder().build().readTree(response.body());
            assertThat(problem.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
            assertThat(problem.get("type").asString()).isEqualTo("about:blank");
            assertThat(problem.get("title").asString()).isEqualTo("Internal error");
            assertThat(problem.get("status").asInt()).isEqualTo(500);
            assertThat(problem.get("code").asString()).isEqualTo("INTERNAL");
            assertThat(problem.get("traceId").asString()).matches("[0-9a-f-]{36}");
            assertThat(response.body()).doesNotContain("shareId", "SERIALIZER_SENTINEL", "SerializationFault",
                    "deterministic serializer failure", "stackTrace", "at com.");
        }
        var normalResponse = send("GET", "/api/v1/customer/shares", "personal", "application/json", null);
        assertThat(normalResponse.statusCode()).isEqualTo(200);
        assertThat(normalResponse.body()).contains("\"shareId\":\"share-one\"")
                .doesNotContain("SERIALIZER_SENTINEL", "deterministic serializer failure");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SerializerFaultBeans {
        @Bean
        tools.jackson.databind.JacksonModule serializerFaultModule() {
            return new tools.jackson.databind.module.SimpleModule().addSerializer(
                    com.onecall.core.customer_iam.internal.web.CustomerSharePage.Item.class,
                    new tools.jackson.databind.ValueSerializer<>() {
                        @Override
                        public void serialize(com.onecall.core.customer_iam.internal.web.CustomerSharePage.Item value,
                                tools.jackson.core.JsonGenerator generator,
                                tools.jackson.databind.SerializationContext serializers) throws tools.jackson.core.JacksonException {
                            if (value.shareId().equals("serializer-fault")) {
                                generator.writeStartObject();
                                generator.writeStringProperty("shareId", "SERIALIZER_SENTINEL");
                                throw tools.jackson.databind.DatabindException.from(serializers,
                                        "deterministic serializer failure");
                            }
                            tools.jackson.databind.json.JsonMapper.builder().build().writeValue(generator, value);
                        }
                    });
        }

        @Bean
        CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> {
                VERIFICATIONS.incrementAndGet();
                return new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, audience,
                        new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null));
            };
        }

        @Bean
        CustomerShareQueryService customerShareQueryService() {
            var mockService = mock(CustomerShareQueryService.class);
            when(mockService.listCustomerShares(any(), any())).thenAnswer(invocation -> {
                SELECTION.incrementAndGet();
                var item = summary("serializer-fault", Instant.parse("2026-01-02T03:04:05.123456Z"));
                return new SharePage(List.of(item), Optional.empty(), false);
            });
            return mockService;
        }

        @Bean
        CustomerContextService customerContextService() {
            return mock(CustomerContextService.class);
        }
    }

    @Test
    void webWithoutSharingPersistenceFailsClosedAndDefaultStaysDatabaseFree() throws Exception {
        try (var webOnly = new org.springframework.boot.builder.SpringApplicationBuilder(OneCallApplication.class, WebOnlyBeans.class)
                .profiles("web").properties("server.port=0", "spring.main.banner-mode=off",
                        "spring.main.allow-bean-definition-overriding=true").run()) {
            int webOnlyPort = ((org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext) webOnly)
                    .getWebServer().getPort();
            var shares = request(webOnlyPort, "GET", "/api/v1/customer/shares", "personal", null, null);
            assertProblem(shares, 503, "SERVICE_UNAVAILABLE", "Service unavailable");
            assertThat(shares.body()).doesNotContain("items", "shareId", "Exception", "SQL");
            var me = request(webOnlyPort, "GET", "/api/v1/customer/me", "personal", null, null);
            assertThat(me.statusCode()).isEqualTo(200);
            assertThat(me.body()).doesNotContain("selectedWorkspace");
            assertThat(webOnly.getBeansOfType(CustomerShareQueryService.class)).isEmpty();
            assertThat(webOnly.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
            assertThat(webOnly.getBeanFactory().containsSingleton("sharingQueryDataSource")).isFalse();
        }
        SELECTION.set(0);
        for (var scenario : List.of(
                new Object[]{AmbiguousQueryBeans.class, 503, "SERVICE_UNAVAILABLE", "Service unavailable", "QUERY"},
                new Object[]{QueryConstructionFailureBeans.class, 503, "SERVICE_UNAVAILABLE", "Service unavailable", "QUERY_CONSTRUCTION_SECRET"},
                new Object[]{QueryInvocationFailureBeans.class, 500, "INTERNAL", "Internal error", "QUERY_INVOCATION_SECRET"})) {
            Class<?> configuration = (Class<?>) scenario[0];
            int status = (int) scenario[1];
            String code = (String) scenario[2];
            String title = (String) scenario[3];
            String secret = (String) scenario[4];
            try (var isolated = new org.springframework.boot.builder.SpringApplicationBuilder(OneCallApplication.class, configuration)
                    .profiles("web").properties("server.port=0", "spring.main.banner-mode=off",
                            "spring.main.allow-bean-definition-overriding=true").run()) {
                int isolatedPort = ((org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext) isolated)
                        .getWebServer().getPort();
                var response = request(isolatedPort, "GET", "/api/v1/customer/shares", "personal", null, null);
                assertProblem(response, status, code, title);
                assertThat(response.body()).doesNotContain(secret, "items", "shareId", "Exception", "stackTrace");
                assertThat(isolated.getBeansOfType(CustomerSessionVerifier.class)).hasSize(1);
                assertThat(isolated.getBeansOfType(CustomerContextService.class)).hasSize(1);
                assertThat(SELECTION).hasValue(configuration == QueryInvocationFailureBeans.class ? 1 : 0);
            }
        }
        for (Class<?> configuration : List.of(MissingVerifierBeans.class, AmbiguousVerifierBeans.class,
                VerifierConstructionFailureBeans.class)) {
            try (var isolated = new org.springframework.boot.builder.SpringApplicationBuilder(OneCallApplication.class, configuration)
                    .profiles("web").properties("server.port=0", "spring.main.banner-mode=off",
                            "spring.main.allow-bean-definition-overriding=true").run()) {
                int isolatedPort = ((org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext) isolated)
                        .getWebServer().getPort();
                var response = request(isolatedPort, "GET", "/api/v1/customer/shares", "personal", null, null);
                assertProblem(response, 503, "SERVICE_UNAVAILABLE", "Service unavailable");
                assertThat(response.body()).doesNotContain("VERIFIER_CONSTRUCTION_SECRET", "Exception", "SQL", "items");
                assertThat(isolated.getBeansOfType(CustomerShareQueryService.class)).hasSize(configuration == MissingVerifierBeans.class ? 0 : 1);
            }
        }
        try (var defaultContext = new org.springframework.boot.builder.SpringApplicationBuilder(OneCallApplication.class)
                .properties("spring.main.banner-mode=off").run()) {
            assertThat(defaultContext).isNotInstanceOf(
                    org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext.class);
            assertThat(defaultContext.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
            assertThat(defaultContext.getBeanFactory().containsSingleton("sharingQueryDataSource")).isFalse();
            assertThat(defaultContext.getBeansOfType(CustomerSessionVerifier.class)).isEmpty();
            assertThat(defaultContext.getBeansOfType(CustomerContextService.class)).isEmpty();
            assertThat(defaultContext.getBeansOfType(CustomerShareQueryService.class)).isEmpty();
        }
    }

    private HttpResponse<String> send(String method, String path, String bearer, String accept, String extraHeader) throws Exception {
        return request(port, method, path, bearer, accept, extraHeader);
    }

    private HttpResponse<String> request(int targetPort, String method, String path, String bearer, String accept, String extraHeader) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + targetPort + path)).timeout(java.time.Duration.ofSeconds(10));
        if (bearer != null) builder.header("Authorization", "Bearer " + bearer);
        if (accept != null) builder.header("Accept", accept);
        if (extraHeader != null) {
            int separator = extraHeader.indexOf(':');
            builder.header(extraHeader.substring(0, separator), extraHeader.substring(separator + 1).trim());
        }
        if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) builder.method(method, HttpRequest.BodyPublishers.ofString("{}"));
        else builder.method(method, HttpRequest.BodyPublishers.noBody());
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static ShareSummary summary(String id, Instant instant) {
        return new ShareSummary(id, UUID.fromString("30000000-0000-0000-0000-000000000001"),
                ShareSummary.VerificationMode.NONE, ShareSummary.ShareStatus.ACTIVE, instant, instant.plusSeconds(3600),
                null, 0, false, 0, 0);
    }

    private static CustomerShareQueryService createServiceMock() {
        var mockService = mock(CustomerShareQueryService.class);
        when(mockService.listCustomerShares(any(), any())).thenAnswer(invocation -> {
            SELECTION.incrementAndGet();
            TrustedShareCaller caller = invocation.getArgument(0);
            SharePageRequest request = invocation.getArgument(1);
            var item = summary("context-share", Instant.parse("2026-01-02T03:04:05.123456Z"));
            Optional<ShareCursor> next = request.limit().orElse(20) == 1
                    ? Optional.of(new ShareCursor(caller.accountId(), caller.organizationId(), item.validFrom(), item.shareId()))
                    : Optional.empty();
            return new SharePage(List.of(item), next, next.isPresent());
        });
        return mockService;
    }
}
