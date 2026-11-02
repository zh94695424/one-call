package com.onecall.archtests;

import static com.onecall.core.customer_iam.internal.application.CustomerContext.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.application.CustomerContextService;
import com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.customer_iam.internal.persistence.CustomerContextRepository;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerificationException;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier;
import com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession;
import com.onecall.core.customer_iam.internal.web.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jooq.DSLContext;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = CustomerMeWebTests.WebTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.lazy-initialization=false")
@ActiveProfiles("web")
class CustomerMeWebTests {
    private static final UUID FIRST = UUID.randomUUID();
    private static final UUID SECOND = UUID.randomUUID();
    private static final AtomicInteger VERIFICATIONS = new AtomicInteger();
    private static final AtomicInteger QUERIES = new AtomicInteger();
    private static final AtomicInteger SEND_ERROR = new AtomicInteger();
    private static volatile String failure;
    private static volatile boolean organizations;
    private static volatile SelectedCustomerWorkspace selectedWorkspace;
    private static volatile AccountStatus accountStatus = AccountStatus.ACTIVE;
    private static volatile boolean accountMissing;
    private final JsonMapper mapper = JsonMapper.builder().build();
    @LocalServerPort
    private int port;
    @Autowired
    private ApplicationContext context;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({CustomerWebConfiguration.class, CustomerMeController.class, CustomerApiErrorController.class, WebTestBeans.class})
    static class WebTestApplication { }

    @TestConfiguration(proxyBeanMethods = false)
    static class WebTestBeans {
        @Bean
        CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> {
                VERIFICATIONS.incrementAndGet();
                if (bearer.value().equals("rejected")) {
                    throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                }
                if (bearer.value().equals("outage")) { throw new IllegalStateException("ADAPTER_CANARY"); }
                return new VerifiedCustomerSession(bearer.value().equals("second") ? SECOND : FIRST,
                        bearer.value().equals("second") ? TrustedCustomerCaller.Channel.MINIAPP : TrustedCustomerCaller.Channel.WEB,
                        audience, selectedWorkspace);
            };
        }

        @Bean
        CustomerContextRepository customerContextRepository() {
            return caller -> {
                QUERIES.incrementAndGet();
                int error = SEND_ERROR.getAndSet(0);
                if (error != 0) {
                    try {
                        var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                        attributes.getResponse().sendError(error);
                    } catch (java.io.IOException exception) { throw new IllegalStateException("Test response failure"); }
                    throw new IllegalStateException("Test error dispatch");
                }
                if ("programming".equals(failure)) { throw new IllegalArgumentException("APPLICATION_CANARY"); }
                if ("database".equals(failure)) { throw new org.jooq.exception.DataAccessException("SQL_CANARY"); }
                if (accountMissing) { return Optional.empty(); }
                var rows = new java.util.ArrayList<WorkspaceSummary>();
                rows.add(new WorkspaceSummary(WorkspaceType.PERSONAL, null, null, null, WorkspaceStatus.ACTIVE));
                if (organizations) {
                    for (OrganizationRole role : OrganizationRole.values()) {
                        rows.add(new WorkspaceSummary(WorkspaceType.ORGANIZATION, UUID.nameUUIDFromBytes(role.name().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                                "Organization", role, WorkspaceStatus.ACTIVE));
                    }
                }
                return Optional.of(new CustomerContext(new AccountSummary(caller.accountId(), "Customer", accountStatus), rows, caller.channel()));
            };
        }

        @Bean
        TransactionTemplate customerQueryTransactionTemplate() {
            TransactionTemplate template = mock(TransactionTemplate.class);
            when(template.execute(any())).thenAnswer(invocation ->
                    invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(new SimpleTransactionStatus()));
            return template;
        }

        @Bean
        DSLContext customerQueryDsl() {
            DSLContext dsl = mock(DSLContext.class);
            var role = mock(org.jooq.Record.class);
            when(role.get(0, String.class)).thenReturn("customer_iam_role");
            when(dsl.fetchOne("select current_user")).thenReturn(role);
            return dsl;
        }

        @Bean
        CustomerContextService customerContextService(@Qualifier("customerQueryTransactionTemplate") TransactionTemplate template,
                @Qualifier("customerQueryDsl") DSLContext dsl, @Qualifier("customerContextRepository") CustomerContextRepository repository) {
            return new CustomerContextService(template, dsl, repository);
        }
    }

    @BeforeEach
    void resetState() {
        VERIFICATIONS.set(0);
        QUERIES.set(0);
        SEND_ERROR.set(0);
        failure = null;
        organizations = false;
        selectedWorkspace = null;
        accountStatus = AccountStatus.ACTIVE;
        accountMissing = false;
    }

    private HttpResponse<String> request(String method, String path, String bearer, String accept, String... extra) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(30));
            if (bearer != null) { builder.header("Authorization", bearer); }
            if (accept != null) { builder.header("Accept", accept); }
            for (int index = 0; index < extra.length; index += 2) { builder.header(extra[index], extra[index + 1]); }
            return client.send(builder.method(method, method.equals("GET") && extra.length > 0
                    ? HttpRequest.BodyPublishers.ofString("{\"accountId\":\"CLIENT_CANARY\"}") : HttpRequest.BodyPublishers.noBody())
                    .build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private JsonNode problem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/problem+json");
        headers(response);
        var json = mapper.readTree(response.body());
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(json.get("status").asInt()).isEqualTo(status);
        assertThat(json.get("code").asString()).isEqualTo(code);
        assertThat(json.get("title").asString()).isEqualTo(switch (code) {
            case "UNAUTHENTICATED" -> "Authentication required";
            case "NOT_FOUND" -> "Not found";
            case "SERVICE_UNAVAILABLE" -> "Service unavailable";
            default -> "Internal error";
        });
        assertThat(json.get("type").asString()).isEqualTo("about:blank");
        assertThat(UUID.fromString(json.get("traceId").asString())).isNotNull();
        assertThat(response.body()).doesNotContain("CANARY", "<html", "exception", "timestamp", "Authorization");
        if (status == 401) { assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer"); }
        else { assertThat(response.headers().allValues("WWW-Authenticate")).isEmpty(); }
        return json;
    }

    private void headers(HttpResponse<String> response) {
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("Pragma")).contains("no-cache");
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(response.headers().allValues("Access-Control-Allow-Origin")).isEmpty();
    }

    @Test
    void authenticatedGetReturnsFrozenCustomerContext() throws Exception {
        var personal = new SelectedCustomerWorkspace(WorkspaceType.PERSONAL, null);
        var organization = new SelectedCustomerWorkspace(WorkspaceType.ORGANIZATION, UUID.randomUUID());
        var cases = java.util.Arrays.asList((SelectedCustomerWorkspace) null, personal, organization,
                new SelectedCustomerWorkspace(null, null), new SelectedCustomerWorkspace(WorkspaceType.PERSONAL, UUID.randomUUID()),
                new SelectedCustomerWorkspace(WorkspaceType.ORGANIZATION, null));
        var frozenBodies = new java.util.HashMap<Boolean, String>();
        for (SelectedCustomerWorkspace selection : cases) {
            selectedWorkspace = selection;
            for (boolean multi : List.of(false, true)) {
                organizations = multi;
                var response = request("GET", "/api/v1/customer/me", "Bearer first", "application/json");
                assertThat(response.statusCode()).isEqualTo(200);
                headers(response);
                var json = mapper.readTree(response.body());
                assertThat(json.propertyNames()).containsExactlyInAnyOrder("account", "workspaces", "channel");
                assertThat(json.toString()).doesNotContain("selectedWorkspace");
                assertThat(json.get("workspaces").size()).isEqualTo(multi ? 5 : 1);
                String baseline = frozenBodies.putIfAbsent(multi, response.body());
                if (baseline != null) { assertThat(response.body()).isEqualTo(baseline); }
            }
        }
        selectedWorkspace = null;
        for (boolean multi : List.of(false, true)) {
            organizations = multi;
            for (String session : List.of("first", "second")) {
                var response = request("GET", "/api/v1/customer/me", "Bearer " + session, "application/json");
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().firstValue("Content-Type")).contains("application/json");
                headers(response);
                var json = mapper.readTree(response.body());
                assertThat(json.propertyNames()).containsExactlyInAnyOrder("account", "workspaces", "channel");
                assertThat(json.get("account").propertyNames()).containsExactlyInAnyOrder("accountId", "displayName", "status");
                assertThat(json.get("account").get("status").asString()).isEqualTo("ACTIVE");
                assertThat(json.get("channel").asString()).isEqualTo(session.equals("first") ? "WEB" : "MINIAPP");
                assertThat(json.get("workspaces").size()).isEqualTo(multi ? 5 : 1);
                assertThat(json.get("workspaces").get(0).propertyNames()).containsExactlyInAnyOrder("workspaceType", "status");
                for (int index = 1; index < json.get("workspaces").size(); index++) {
                    assertThat(json.get("workspaces").get(index).propertyNames())
                            .containsExactlyInAnyOrder("workspaceType", "organizationId", "organizationDisplayName", "role", "status");
                }
            }
        }
        for (String name : List.of("customerPersistenceConfiguration", "sharingPersistenceConfiguration", "customerQueryDataSource", "sharingQueryDataSource")) {
            assertThat(context.containsBean(name)).isFalse();
        }
        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
        assertThat(context.getBeansOfType(com.onecall.core.sharing.internal.application.CustomerShareQueryService.class)).isEmpty();
        assertThat(context.getBeansOfType(CustomerSharesController.class)).isEmpty();
        assertThat(context.getBeansOfType(org.springframework.jdbc.datasource.DataSourceTransactionManager.class)).isEmpty();
        assertThat(context.getBeansOfType(CustomerBoundaryFilter.class)).isEmpty();
        assertThat(context.getBeansOfType(CustomerMeController.class)).hasSize(1);
        assertThat(context.getBeansOfType(CustomerProblemWriter.class)).hasSize(1);
        assertThat(context.getBeansOfType(CustomerApiErrorController.class)).hasSize(1);
        assertThat(context.getBeansOfType(org.springframework.boot.web.servlet.FilterRegistrationBean.class).values().stream()
                .filter(registration -> registration.getFilter() instanceof CustomerBoundaryFilter)).hasSize(1);
        var registeredMappings = context.getBean(org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class)
                .getHandlerMethods();
        assertThat(registeredMappings.entrySet().stream().filter(entry -> entry.getKey().getMethodsCondition().getMethods()
                .contains(org.springframework.web.bind.annotation.RequestMethod.GET)
                && entry.getKey().getPatternValues().contains("/api/v1/customer/me"))).hasSize(1);
        assertThat(registeredMappings.entrySet().stream().filter(entry -> entry.getKey().getPatternValues().contains("/error"))).hasSize(1);
        assertThat(context.containsBean("basicErrorController")).isFalse();
        assertThat(registeredMappings.values()).noneMatch(method -> method.getBeanType().equals(
                org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController.class));
    }

    @Test
    void missingMalformedAndDuplicateBearersReturnFrozen401() throws Exception {
        for (String header : java.util.Arrays.asList(null, "", "Basic bytes", "Bearer", "Bearer ", "Bearer a b", "Bearer a,b",
                "Bearer\tbytes", "Bearer =bytes", "Bearer bytes=extra", "Bearer bytes!")) {
            problem(request("GET", "/api/v1/customer/me?access_token=QUERY_CANARY", header, null, "Cookie", "access_token=COOKIE_CANARY"),
                    401, "UNAUTHENTICATED");
        }
        for (SelectedCustomerWorkspace selection : java.util.Arrays.asList(null,
                new SelectedCustomerWorkspace(WorkspaceType.PERSONAL, null),
                new SelectedCustomerWorkspace(WorkspaceType.ORGANIZATION, UUID.randomUUID()))) {
            selectedWorkspace = selection;
            accountMissing = true;
            problem(request("GET", "/api/v1/customer/me", "Bearer first", null), 401, "UNAUTHENTICATED");
            accountMissing = false;
            accountStatus = AccountStatus.DISABLED;
            problem(request("GET", "/api/v1/customer/me", "Bearer first", null), 401, "UNAUTHENTICATED");
            accountStatus = AccountStatus.ACTIVE;
        }
        problem(request("GET", "/api/v1/customer/me", "Bearer first", null, "Authorization", "Bearer second"), 401, "UNAUTHENTICATED");
        assertThat(VERIFICATIONS).hasValue(6);
        assertThat(QUERIES).hasValue(6);
        for (String header : List.of("bEaReR  opaque._~+/-==", "Bearer eyJaccount.fake.signature")) {
            assertThat(request("GET", "/api/v1/customer/me", header, null).statusCode()).isEqualTo(200);
        }
    }

    @Test
    void clientSelectorsHeadersCookiesAndBodyCannotOverrideContext() throws Exception {
        for (String session : List.of("first", "second", "first")) {
            var response = request("GET", "/api/v1/customer/me?accountId=CLIENT_CANARY&organizationId=CLIENT_CANARY&workspace=CLIENT_CANARY&role=ADMINISTRATOR&channel=IVR",
                    "Bearer " + session, null, "X-Account-Id", "CLIENT_CANARY", "X-Organization-Id", "CLIENT_CANARY",
                    "X-Role", "ADMINISTRATOR", "X-Channel", "IVR", "X-Forwarded-User", "CLIENT_CANARY", "Cookie", "accountId=CLIENT_CANARY");
            assertThat(response.statusCode()).isEqualTo(200);
            var json = mapper.readTree(response.body());
            assertThat(json.get("account").get("accountId").asString()).isEqualTo((session.equals("first") ? FIRST : SECOND).toString());
            assertThat(json.get("channel").asString()).isEqualTo(session.equals("first") ? "WEB" : "MINIAPP");
            assertThat(response.body()).doesNotContain("CLIENT_CANARY");
        }
    }

    @Test
    void reachableProblemsAndResponseHeadersAreClosedAndSanitized() throws Exception {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            problem(request("GET", "/api/v1/customer/me", null, null, "X-Trace-Id", "TRACE_CANARY"), 401, "UNAUTHENTICATED");
            problem(request("GET", "/api/v1/customer/me", "Bearer rejected", null), 401, "UNAUTHENTICATED");
            assertThat(QUERIES).hasValue(0);
            problem(request("GET", "/api/v1/customer/me", "Bearer outage", null), 503, "SERVICE_UNAVAILABLE");
            failure = "programming";
            problem(request("GET", "/api/v1/customer/me", "Bearer first", null), 500, "INTERNAL");
            failure = "database";
            problem(request("GET", "/api/v1/customer/me", "Bearer first", null), 503, "SERVICE_UNAVAILABLE");
            problem(request("GET", "/missing?CANARY", "Bearer HEADER_CANARY", null), 404, "NOT_FOUND");
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain("CANARY", "Bearer", "Authorization");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

    @Test
    void otherPathsMethodsAndAliasesAreNotCustomerOperations() throws Exception {
        for (String method : List.of("POST", "OPTIONS", "HEAD")) {
            var response = request(method, "/api/v1/customer/me", "Bearer first", null);
            assertThat(response.statusCode()).isEqualTo(404);
            headers(response);
            if (!method.equals("HEAD")) { problem(response, 404, "NOT_FOUND"); }
            else { assertThat(response.body()).isEmpty(); }
        }
        for (String path : List.of("/api/v1/customer/me/", "/api/v1/customer/%6de", "/api/v1/customer/unimplemented", "/api/v1/employee/me", "/error", "/")) {
            problem(request("GET", path, "Bearer first", null), 404, "NOT_FOUND");
        }
        assertThat(VERIFICATIONS).hasValue(0);
        assertThat(QUERIES).hasValue(0);
    }

    @Test
    void concurrentRequestsKeepAccountAndChannelIsolated() throws Exception {
        var executor = Executors.newFixedThreadPool(4);
        try {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int index = 0; index < 16; index++) {
                String session = index % 2 == 0 ? "first" : "second";
                futures.add(executor.submit(() -> {
                    var response = request("GET", "/api/v1/customer/me", "Bearer " + session, null);
                    assertThat(response.statusCode()).isEqualTo(200);
                    var json = mapper.readTree(response.body());
                    assertThat(json.get("account").get("accountId").asString()).isEqualTo((session.equals("first") ? FIRST : SECOND).toString());
                    assertThat(json.get("channel").asString()).isEqualTo(session.equals("first") ? "WEB" : "MINIAPP");
                    return null;
                }));
            }
            for (var future : futures) { future.get(30, TimeUnit.SECONDS); }
        } finally { executor.shutdownNow(); assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue(); }
    }

    @Test
    void applicationNegotiationAndErrorDispatchUseFrozenProblems() throws Exception {
        for (String accept : List.of("application/json", "*/*")) {
            assertThat(request("GET", "/api/v1/customer/me", "Bearer first", accept).statusCode()).isEqualTo(200);
        }
        for (String accept : List.of("text/html", "application/x-unknown", "application/json;q=0", "application/json;q=0, */*", "application/json;q=oops")) {
            problem(request("GET", "/api/v1/customer/me", "Bearer first", accept), 500, "INTERNAL");
        }
        int before = VERIFICATIONS.get();
        problem(request("GET", "/api/v1/customer/me", null, "text/html"), 401, "UNAUTHENTICATED");
        assertThat(VERIFICATIONS).hasValue(before);
        try {
            for (int status : List.of(400, 401, 403, 404, 405, 406, 415, 422, 429, 500, 503)) {
                SEND_ERROR.set(status);
                int expected = status == 401 || status == 404 || status == 503 ? status : 500;
                String code = expected == 401 ? "UNAUTHENTICATED" : expected == 404 ? "NOT_FOUND" : expected == 503 ? "SERVICE_UNAVAILABLE" : "INTERNAL";
                problem(request("GET", "/api/v1/customer/me", "Bearer first", "application/json"), expected, code);
                assertThat(SEND_ERROR).hasValue(0);
            }
        } finally { SEND_ERROR.set(0); }
        assertThat(context.containsBean("basicErrorController")).isFalse();
        var mappings = context.getBean(org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class);
        assertThat(mappings.getHandlerMethods().values()).noneMatch(method -> method.getBeanType().getSimpleName().equals("BasicErrorController"));
    }

    @Test
    void reservedProblemVocabularyIsWriterOnly() throws Exception {
        var writer = new CustomerProblemWriter();
        var cases = List.of(
                new Object[]{CustomerContextException.Code.UNAUTHENTICATED, 401, "Authentication required"},
                new Object[]{CustomerContextException.Code.FORBIDDEN, 403, "Access denied"},
                new Object[]{CustomerContextException.Code.VALIDATION_FAILED, 422, "Request validation failed"},
                new Object[]{CustomerContextException.Code.RATE_LIMITED, 429, "Too many requests"},
                new Object[]{CustomerContextException.Code.SERVICE_UNAVAILABLE, 503, "Service unavailable"},
                new Object[]{CustomerContextException.Code.INTERNAL, 500, "Internal error"},
                new Object[]{CustomerContextException.Code.CURSOR_INVALID, 400, "Invalid cursor"});
        for (Object[] problemCase : cases) {
            var response = new MockHttpServletResponse();
            writer.write(response, (CustomerContextException.Code) problemCase[0], UUID.randomUUID());
            assertThat(response.getStatus()).isEqualTo(problemCase[1]);
            var json = mapper.readTree(response.getContentAsString());
            assertThat(json.get("title").asString()).isEqualTo(problemCase[2]);
            assertThat(json.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        }
        var response = new MockHttpServletResponse();
        writer.notFound(response, UUID.randomUUID());
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).contains("Not found").doesNotContain("null", "detail", "instance", "errors");
    }
}
