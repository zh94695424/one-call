package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader.Anchor;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader.Artifact;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader.Header;
import com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = CustomerEvidenceWebTests.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("web")
class CustomerEvidenceWebTests {
    private static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID SUBJECT = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID EVIDENCE = UUID.fromString("abcdefab-0000-0000-0000-000000000001");
    private static final Instant TIME = Instant.parse("2026-01-02T03:04:05.123456Z");
    private static final String LIST = "/api/v1/customer/evidence";
    private static final String DETAIL = LIST + "/" + EVIDENCE;
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    @LocalServerPort private int port;
    @Autowired private CustomerEvidenceQueryService service;
    @Autowired private CustomerEvidenceCursorCodec codec;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({CustomerWebConfiguration.class, CustomerEvidenceController.class, CustomerApiErrorController.class,
            CustomerMeController.class, CustomerSharesController.class, CustomerBusinessNumbersController.class,
            CustomerSubjectsController.class, Beans.class})
    static class Application { }

    @TestConfiguration(proxyBeanMethods = false)
    static class Beans {
        @Bean
        CustomerEvidenceQueryService customerEvidenceQueryService() { return mock(CustomerEvidenceQueryService.class); }
        @Bean
        CustomerEvidenceCursorCodec customerEvidenceCursorCodec() { return spy(new CustomerEvidenceCursorCodec(properties())); }
        @Bean
        CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> {
                String token = bearer.value();
                if (token.equals("outage")) throw new IllegalStateException("VERIFIER_CANARY");
                if (List.of("rejected", "expired", "revoked").contains(token))
                    throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                if (token.equals("null-session")) return null;
                var workspace = switch (token) {
                    case "unselected" -> null;
                    case "null-kind" -> new SelectedCustomerWorkspace(null, null);
                    case "personal-org" -> new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, ORGANIZATION);
                    case "org-null" -> new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, null);
                    case "organization", "ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR" ->
                        new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, ORGANIZATION);
                    default -> new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null);
                };
                return new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                        token.equals("wrong-audience") ? "foreign" : audience, workspace);
            };
        }
    }

    private static CustomerEvidenceCursorProperties properties() {
        var properties = new CustomerEvidenceCursorProperties();
        properties.setCursorHmacKey(KEY);
        return properties;
    }

    private static TrustedCustomerCaller caller(boolean organization) {
        return new TrustedCustomerCaller(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                new SelectedCustomerWorkspace(organization ? CustomerContext.WorkspaceType.ORGANIZATION
                        : CustomerContext.WorkspaceType.PERSONAL, organization ? ORGANIZATION : null));
    }

    private static CustomerEvidenceQueryService.Detail detail(String status, Instant solidified) {
        return new CustomerEvidenceQueryService.Detail(EVIDENCE, SUBJECT, status, "CALL_RECORDING", TIME, "v1", solidified,
                List.of(new Artifact("ORIGINAL", "audio/wav", 2147483648L, 37, "a".repeat(64)),
                        new Artifact("DERIVED", "audio/wav", 2147483648L, null, "a".repeat(64))));
    }

    @BeforeEach
    void setup() {
        reset(service);
        clearInvocations(codec);
        when(service.listCustomerEvidence(any(), any())).thenReturn(new CustomerEvidenceQueryService.Page(
                List.of(new CustomerEvidenceQueryService.Summary(EVIDENCE, SUBJECT, "PENDING", "CALL_RECORDING", TIME,
                        "v1", TIME, List.of("ORIGINAL", "DERIVED"))), Optional.empty(), false));
        when(service.getCustomerEvidence(any(), any())).thenReturn(Optional.of(detail("PENDING", null)));
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
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("Pragma")).contains("no-cache");
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(response.body()).doesNotContain("CANARY", "jdbc:", "password", "ciphertext", "Exception");
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

    @Test
    void exactListRouteReturnsOnlyFrozenPageAndSummaryKeys() throws Exception {
        var json = check(get(LIST, "personal"), 200, null);
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
        assertThat(json.path("nextCursor").isNull()).isTrue();
        assertThat(json.path("hasMore").asBoolean()).isFalse();
        var summary = json.path("items").get(0);
        assertThat(summary.propertyNames()).containsExactlyInAnyOrder("evidenceId", "subjectId", "status", "sourceType",
                "capturedAt", "consentVersion", "createdAt", "artifactRoles");
        assertThat(summary.path("evidenceId").asString()).isEqualTo(EVIDENCE.toString());
        assertThat(summary.path("subjectId").asString()).isEqualTo(SUBJECT.toString());
        assertThat(summary.path("sourceType").asString()).isEqualTo("CALL_RECORDING");
        assertThat(summary.path("consentVersion").asString()).isEqualTo("v1");
        assertThat(summary.path("createdAt").asString()).isEqualTo(TIME.toString());
        assertThat(summary.path("capturedAt").asString()).isEqualTo(TIME.toString());
        assertThat(summary.path("artifactRoles").valueStream().map(JsonNode::asString)).containsExactly("ORIGINAL", "DERIVED");
        verify(service).listCustomerEvidence(caller(false), new CustomerEvidenceQueryService.Request(Optional.empty(), Optional.empty()));
        for (String status : List.of("PENDING", "SOLIDIFIED", "QUARANTINED", "FAILED")) {
            when(service.listCustomerEvidence(any(), any())).thenReturn(new CustomerEvidenceQueryService.Page(List.of(
                    new CustomerEvidenceQueryService.Summary(EVIDENCE, SUBJECT, status, "CALL_RECORDING", TIME, "v1", TIME, List.of())), Optional.empty(), false));
            var item = check(get(LIST, "personal"), 200, null).path("items").get(0);
            assertThat(item.propertyNames()).containsExactlyInAnyOrder("evidenceId", "subjectId", "status", "sourceType", "capturedAt", "consentVersion", "createdAt", "artifactRoles");
            assertThat(item.path("status").asString()).isEqualTo(status);
            assertThat(item.path("artifactRoles").isArray()).isTrue();
            assertThat(item.path("artifactRoles").size()).isZero();
        }
        when(service.listCustomerEvidence(any(), any())).thenReturn(new CustomerEvidenceQueryService.Page(List.of(), Optional.empty(), false));
        assertThat(check(get(LIST, "personal"), 200, null)).isEqualTo(JsonMapper.builder().build()
                .readTree("{\"items\":[],\"nextCursor\":null,\"hasMore\":false}"));
        var anchor = new Anchor(TIME, EVIDENCE);
        when(service.listCustomerEvidence(any(), any())).thenReturn(new CustomerEvidenceQueryService.Page(List.of(), Optional.of(anchor), true));
        var page = check(get(LIST, "personal"), 200, null);
        assertThat(page.path("hasMore").asBoolean()).isTrue();
        assertThat(codec.decode(new Scope(ACCOUNT, null), page.path("nextCursor").asString())).isEqualTo(anchor);
    }

    @Test
    void exactDetailRouteReturnsOnlyFrozenDetailAndArtifactKeys() throws Exception {
        for (String status : List.of("PENDING", "SOLIDIFIED", "QUARANTINED", "FAILED")) {
            when(service.getCustomerEvidence(any(), any())).thenReturn(Optional.of(detail(status, status.equals("SOLIDIFIED") ? TIME : null)));
            var json = check(get(DETAIL, "personal"), 200, null);
            var keys = new ArrayList<>(List.of("evidenceId", "subjectId", "status", "sourceType", "capturedAt", "consentVersion", "artifacts"));
            if (status.equals("SOLIDIFIED")) keys.add("solidifiedAt");
            assertThat(json.propertyNames()).containsExactlyInAnyOrderElementsOf(keys);
            assertThat(json.path("status").asString()).isEqualTo(status);
            var original = json.path("artifacts").get(0);
            assertThat(original.propertyNames()).containsExactlyInAnyOrder("artifactRole", "mimeType", "sizeBytes", "sha256", "validatedDurationSeconds");
            assertThat(original.path("sizeBytes").asLong()).isEqualTo(2147483648L);
            assertThat(original.path("validatedDurationSeconds").asInt()).isEqualTo(37);
            var derived = json.path("artifacts").get(1);
            assertThat(derived.propertyNames()).containsExactlyInAnyOrder("artifactRole", "mimeType", "sizeBytes", "sha256");
            assertThat(derived.path("artifactRole").asString()).isEqualTo("DERIVED");
        }
        verifyNoInteractions(codec);
    }

    @Test
    void unsupportedMethodsAliasesExtraSegmentsAndTrailingRoutesAreNotFound() throws Exception {
        for (String path : List.of(LIST, DETAIL)) for (String method : List.of("HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"))
            check(request(method, path, null, HttpRequest.BodyPublishers.noBody()), 404, "NOT_FOUND");
        for (String path : List.of(LIST + "/", DETAIL + "/", LIST + ".json", DETAIL + "/extra"))
            check(get(path, null), 404, "NOT_FOUND");
        var factory = new DefaultListableBeanFactory();
        var filter = new CustomerBoundaryFilter(new CustomerBearerAuthenticator(factory.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter());
        for (String path : List.of(LIST + ";x", LIST + "/%61", DETAIL + ";x", DETAIL + "/extra", LIST)) {
            var request = new MockHttpServletRequest("GET", path.equals(LIST) ? "/mismatch" : path);
            request.setServletPath(path);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (incoming, outgoing) -> { throw new AssertionError("Route must stop before auth"); });
            assertThat(response.getStatus()).isEqualTo(404);
        }
        verifyNoInteractions(service, codec);
    }

    @Test
    void missingMalformedDuplicateRejectedAndWrongAudienceBearersFailBeforeInputs() throws Exception {
        for (String path : List.of(LIST + "?limit=bad&cursor=bad", LIST + "/bad")) {
            check(get(path, null), 401, "UNAUTHENTICATED");
            for (String token : List.of("rejected", "expired", "revoked", "wrong-audience"))
                assertThat(get(path, token).headers().firstValue("WWW-Authenticate")).contains("Bearer");
            for (String token : List.of("rejected", "expired", "revoked", "wrong-audience")) check(get(path, token), 401, "UNAUTHENTICATED");
            for (String header : List.of("Basic CANARY", "Bearer", "Bearer a,b", "Bearer a b"))
                check(get(path, null, "Authorization", header), 401, "UNAUTHENTICATED");
            check(get(path, "personal", "Authorization", "Bearer organization"), 401, "UNAUTHENTICATED");
        }
        verifyNoInteractions(service, codec);
    }

    @Test
    void unavailableVerifierAndMalformedTrustedWorkspaceFailClosed() throws Exception {
        for (String token : List.of("outage", "null-session", "unselected", "null-kind", "personal-org", "org-null")) {
            check(get(LIST + "?limit=bad&cursor=bad", token), 503, "SERVICE_UNAVAILABLE");
            check(get(LIST + "/bad", token), 503, "SERVICE_UNAVAILABLE");
        }
        for (int count : List.of(0, 2)) {
            var factory = new DefaultListableBeanFactory();
            for (int index = 0; index < count; index++) factory.registerSingleton("verifier" + index, mock(CustomerSessionVerifier.class));
            var filter = new CustomerBoundaryFilter(new CustomerBearerAuthenticator(factory.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter());
            var request = new MockHttpServletRequest("GET", LIST + "/bad");
            request.setServletPath(LIST + "/bad"); request.addHeader("Authorization", "Bearer personal");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (incoming, outgoing) -> { throw new AssertionError("Verifier must stop"); });
            assertThat(response.getStatus()).isEqualTo(503);
        }
        verifyNoInteractions(service, codec);
    }

    @Test
    void queryHeadersCookiesAttributesAndBodiesCannotSelectOwnersOrFilters() throws Exception {
        String selectors = "?accountId=CANARY&ownerAccountId=CANARY&ownerAccountId=other&organizationId=CANARY&workspace=CANARY"
                + "&status=FAILED&status=PENDING&subjectId=CANARY&sort=ascending&role=ADMINISTRATOR";
        for (String token : List.of("personal", "organization", "ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR")) {
            clearInvocations(service);
            check(get(LIST + selectors, token, "X-Account-Id", "CANARY", "X-Workspace-Id", "CANARY", "Cookie", "workspace=CANARY"), 200, null);
            verify(service).listCustomerEvidence(eq(caller(!token.equals("personal"))), any());
            clearInvocations(service);
            check(get(DETAIL + selectors + "&limit=bad&limit=bad&cursor=bad&cursor=bad", token), 200, null);
            verify(service).getCustomerEvidence(caller(!token.equals("personal")), EVIDENCE);
            for (String path : List.of(LIST + selectors, DETAIL + selectors)) {
                check(request("GET", path, token, HttpRequest.BodyPublishers.ofString("not JSON CANARY"), "Content-Type", "application/json"), 200, null);
                check(request("GET", path, token, HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream(
                        "{broken CANARY".getBytes(java.nio.charset.StandardCharsets.UTF_8))), "Content-Type", "text/plain"), 200, null);
            }
            verify(service, times(2)).listCustomerEvidence(eq(caller(!token.equals("personal"))),
                    eq(new CustomerEvidenceQueryService.Request(Optional.empty(), Optional.empty())));
            verify(service, times(3)).getCustomerEvidence(caller(!token.equals("personal")), EVIDENCE);
            verifyNoMoreInteractions(service);
        }
        var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("customerEvidenceQueryService", service);
        factory.registerSingleton("customerEvidenceCursorCodec", codec);
        var controller = new CustomerEvidenceController(factory.getBeanProvider(CustomerEvidenceQueryService.class),
                factory.getBeanProvider(CustomerEvidenceCursorCodec.class), new CustomerProblemWriter());
        var request = new MockHttpServletRequest();
        request.setAttribute("accountId", SUBJECT); request.setAttribute("workspace", "CANARY");
        clearInvocations(service);
        controller.listCustomerEvidence(new CustomerRequestPrincipal(caller(false)), request);
        verify(service).listCustomerEvidence(eq(caller(false)), any());
    }

    @Test
    void limitDefaultBoundsLexicalDuplicatesAndOverflowMatchCustomerPrecedent() throws Exception {
        for (String value : List.of("", "1", "100", "020")) {
            clearInvocations(service);
            check(get(LIST + (value.isEmpty() ? "" : "?limit=" + value), "personal"), 200, null);
            verify(service).listCustomerEvidence(eq(caller(false)), argThat(request -> request.effectiveLimit() ==
                    (value.isEmpty() ? 20 : Integer.parseInt(value))));
        }
        clearInvocations(service, codec);
        for (String value : List.of("", "%20", "+1", "-1", "1.0", "0", "101", "2147483648", "999999999999999999999"))
            check(get(LIST + "?limit=" + value + "&cursor=bad", "personal"), 422, "VALIDATION_FAILED");
        check(get(LIST + "?limit=20&limit=20", "personal"), 422, "VALIDATION_FAILED");
        verifyNoInteractions(service, codec);
    }

    @Test
    void malformedDuplicateTamperedAndCrossContextCursorsAreInvalid() throws Exception {
        String cursor = codec.encode(new Scope(ACCOUNT, null), new Anchor(TIME, EVIDENCE));
        clearInvocations(codec);
        for (String value : List.of("", "bad", cursor + "=", cursor.substring(1), "%20" + cursor))
            check(get(LIST + "?cursor=" + value, "personal"), 400, "CURSOR_INVALID");
        check(get(LIST + "?cursor=" + cursor + "&cursor=" + cursor, "personal"), 400, "CURSOR_INVALID");
        verifyNoInteractions(service, codec);
        byte[] bytes = Base64.getUrlDecoder().decode(cursor); bytes[92] ^= 1;
        check(get(LIST + "?cursor=" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes), "personal"), 400, "CURSOR_INVALID");
        for (Scope scope : List.of(new Scope(SUBJECT, null), new Scope(ACCOUNT, ORGANIZATION), new Scope(ACCOUNT, SUBJECT)))
            check(get(LIST + "?cursor=" + codec.encode(scope, new Anchor(TIME, EVIDENCE)), "personal"), 400, "CURSOR_INVALID");
        check(get(LIST + "?cursor=" + cursor, "organization"), 400, "CURSOR_INVALID");
        var properties = new CustomerBusinessNumberCursorProperties(); properties.setCursorHmacKey(KEY);
        String other = new CustomerBusinessNumberCursorCodec(properties).encode(new Scope(ACCOUNT, null),
                new com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Anchor(TIME, EVIDENCE));
        check(get(LIST + "?cursor=" + other, "personal"), 400, "CURSOR_INVALID");
        verifyNoInteractions(service);
        check(get(LIST + "?cursor=" + cursor + "&limit=1", "personal"), 200, null);
        verify(service).listCustomerEvidence(caller(false), new CustomerEvidenceQueryService.Request(Optional.of(new Anchor(TIME, EVIDENCE)), Optional.of(1)));
    }

    @Test
    void fullUuidValidationAndUppercaseAcceptanceAreExact() throws Exception {
        for (String value : List.of("bad", "1-1-1-1-1", EVIDENCE + ".json", "00000000-0000-0000-0000-00000000000z"))
            check(get(LIST + "/" + value, "personal"), 422, "VALIDATION_FAILED");
        verifyNoInteractions(service, codec);
        check(get(LIST + "/" + EVIDENCE.toString().toUpperCase(Locale.ROOT), "personal"), 200, null);
        verify(service).getCustomerEvidence(caller(false), EVIDENCE);
        verifyNoInteractions(codec);
    }

    @Test
    void authenticationScopeLimitCursorLifecycleAndMissingPrecedenceIsExact() throws Exception {
        check(get(LIST + "/bad/extra?limit=bad", null), 404, "NOT_FOUND");
        check(get(LIST + "?limit=bad&cursor=bad", null), 401, "UNAUTHENTICATED");
        check(get(LIST + "?limit=bad&cursor=bad", "unselected"), 503, "SERVICE_UNAVAILABLE");
        check(get(LIST + "?limit=bad&cursor=bad", "personal"), 422, "VALIDATION_FAILED");
        check(get(LIST + "?cursor=bad", "personal"), 400, "CURSOR_INVALID");
        verifyNoInteractions(service, codec);
        doThrow(new CustomerContextException(CustomerContextException.Code.FORBIDDEN)).when(service).getCustomerEvidence(any(), any());
        check(get(LIST + "/bad", "personal"), 422, "VALIDATION_FAILED");
        check(get(DETAIL, "personal"), 403, "FORBIDDEN");
        doReturn(Optional.empty()).when(service).getCustomerEvidence(any(), any());
        check(get(DETAIL, "personal"), 404, "NOT_FOUND");
        doThrow(new CustomerContextException(CustomerContextException.Code.FORBIDDEN)).when(service).listCustomerEvidence(any(), any());
        check(get(LIST + "?cursor=bad", "personal"), 400, "CURSOR_INVALID");
        check(get(LIST, "personal"), 403, "FORBIDDEN");
        var composition = new Composition();
        doAnswer(invocation -> composition.query.getCustomerEvidence(invocation.getArgument(0), invocation.getArgument(1)))
                .when(service).getCustomerEvidence(any(), any());
        doAnswer(invocation -> composition.query.listCustomerEvidence(invocation.getArgument(0), invocation.getArgument(1)))
                .when(service).listCustomerEvidence(any(), any());
        doThrow(new CustomerContextException(CustomerContextException.Code.FORBIDDEN)).when(composition.lifecycle).requireActiveContext(any());
        check(get(DETAIL, "personal"), 403, "FORBIDDEN");
        check(get(LIST, "personal"), 403, "FORBIDDEN");
        verifyNoInteractions(composition.subjects, composition.evidence);
        doNothing().when(composition.lifecycle).requireActiveContext(any());
        when(composition.evidence.findHeader(EVIDENCE)).thenReturn(Optional.empty());
        check(get(DETAIL, "personal"), 404, "NOT_FOUND");
        verifyNoInteractions(composition.subjects);
        when(composition.evidence.findHeader(EVIDENCE)).thenReturn(Optional.of(composition.header));
        when(composition.subjects.findOwnedSubject(any(), any(), any())).thenReturn(Optional.empty());
        when(composition.subjects.hasOpenRisk(SUBJECT)).thenReturn(true);
        check(get(DETAIL, "personal"), 404, "NOT_FOUND");
        verify(composition.subjects, never()).hasOpenRisk(SUBJECT);
        verify(composition.evidence, never()).findArtifacts(any());
        when(composition.subjects.findOwnedSubject(SUBJECT, ACCOUNT, null)).thenReturn(Optional.of(SUBJECT));
        check(get(DETAIL, "personal"), 403, "FORBIDDEN");
        verify(composition.evidence, never()).findArtifacts(any());
        when(composition.subjects.hasOpenRisk(SUBJECT)).thenReturn(false);
        check(get(DETAIL, "personal"), 200, null);
        when(composition.subjects.findOwnedSubjectIds(ACCOUNT, null)).thenReturn(Set.of(SUBJECT));
        when(composition.evidence.findSubjectIdsWithEvidence(Set.of(SUBJECT))).thenReturn(Set.of(SUBJECT));
        when(composition.subjects.hasOpenRisk(Set.of(SUBJECT))).thenReturn(true);
        check(get(LIST + "?limit=1", "personal"), 403, "FORBIDDEN");
        verify(composition.evidence, never()).fetchPage(any(), any(), anyInt());
        var order = inOrder(composition.context, composition.lifecycle, composition.evidence, composition.subjects);
        order.verify(composition.context).execute("SET LOCAL ROLE customer_iam_role");
        order.verify(composition.context).fetchOne("select current_user");
        order.verify(composition.lifecycle).requireActiveContext(new Scope(ACCOUNT, null));
    }

    @Test
    void missingServicesKeysAndUnexpectedFailuresUseSanitizedProblems() throws Exception {
        for (var code : List.of(CustomerContextException.Code.SERVICE_UNAVAILABLE, CustomerContextException.Code.INTERNAL)) {
            doThrow(new CustomerContextException(code)).when(service).listCustomerEvidence(any(), any());
            doThrow(new CustomerContextException(code)).when(service).getCustomerEvidence(any(), any());
            for (String path : List.of(LIST, DETAIL)) check(get(path, "personal"), code == CustomerContextException.Code.INTERNAL ? 500 : 503, code.name());
        }
        doReturn(null).when(service).listCustomerEvidence(any(), any());
        doReturn(null).when(service).getCustomerEvidence(any(), any());
        for (String path : List.of(LIST, DETAIL)) check(get(path, "personal"), 500, "INTERNAL");
        var composition = new Composition();
        doAnswer(invocation -> composition.query.getCustomerEvidence(invocation.getArgument(0), invocation.getArgument(1)))
                .when(service).getCustomerEvidence(any(), any());
        doAnswer(invocation -> composition.query.listCustomerEvidence(invocation.getArgument(0), invocation.getArgument(1)))
                .when(service).listCustomerEvidence(any(), any());
        for (RuntimeException failure : List.of(new org.jooq.exception.DataAccessException("SQL CANARY", new java.sql.SQLException("password CANARY")),
                new org.springframework.dao.DataAccessResourceFailureException("jdbc:CANARY"),
                new org.springframework.transaction.CannotCreateTransactionException("CANARY"))) {
            doThrow(failure).when(composition.lifecycle).requireActiveContext(any());
            for (String path : List.of(LIST, DETAIL)) check(get(path, "personal"), 503, "SERVICE_UNAVAILABLE");
        }
        doThrow(new IllegalStateException("SQL password CANARY")).when(service).listCustomerEvidence(any(), any());
        doThrow(new IllegalStateException("SQL password CANARY")).when(service).getCustomerEvidence(any(), any());
        for (String path : List.of(LIST, DETAIL)) check(get(path, "personal"), 500, "INTERNAL");
        var factory = new DefaultListableBeanFactory();
        var controller = new CustomerEvidenceController(factory.getBeanProvider(CustomerEvidenceQueryService.class),
                factory.getBeanProvider(CustomerEvidenceCursorCodec.class), new CustomerProblemWriter());
        var principal = new CustomerRequestPrincipal(caller(false));
        assertThatThrownBy(() -> controller.listCustomerEvidence(principal, new MockHttpServletRequest()))
                .isInstanceOfSatisfying(CustomerContextException.class, failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> controller.getCustomerEvidence(principal, EVIDENCE.toString(), new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isInstanceOfSatisfying(CustomerContextException.class, failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.SERVICE_UNAVAILABLE));
    }

    private static final class Composition {
        private final org.springframework.transaction.support.TransactionTemplate transaction = mock(org.springframework.transaction.support.TransactionTemplate.class);
        private final org.jooq.DSLContext context = mock(org.jooq.DSLContext.class);
        private final JooqCustomerBusinessNumberAccessRepository lifecycle = mock(JooqCustomerBusinessNumberAccessRepository.class);
        private final CustomerSubjectAccessReader subjects = mock(CustomerSubjectAccessReader.class);
        private final CustomerEvidenceReader evidence = mock(CustomerEvidenceReader.class);
        private final Header header = new Header(EVIDENCE, SUBJECT, "PENDING", "CALL_RECORDING", TIME.toString(), "v1", TIME, null);
        private final CustomerEvidenceQueryService query;

        private Composition() {
            when(transaction.execute(any())).thenAnswer(invocation -> ((org.springframework.transaction.support.TransactionCallback<?>) invocation.getArgument(0))
                    .doInTransaction(mock(org.springframework.transaction.TransactionStatus.class)));
            var role = mock(org.jooq.Record.class);
            when(role.get(0, String.class)).thenReturn("customer_iam_role");
            when(context.fetchOne("select current_user")).thenReturn(role);
            when(evidence.findHeader(EVIDENCE)).thenReturn(Optional.of(header));
            when(evidence.findArtifacts(EVIDENCE)).thenReturn(List.of());
            query = new CustomerEvidenceQueryService(transaction, context, lifecycle, subjects, evidence);
        }
    }

    @Test
    void responsesNegotiationAndExistingRoutesPreserveCustomerBoundary() throws Exception {
        for (String path : List.of(LIST, DETAIL)) {
            check(get(path, "personal", "Accept", "application/json"), 200, null);
            for (String accept : List.of("text/html", "application/json;q=0", "not a media type"))
                check(get(path, "personal", "Accept", accept), 500, "INTERNAL");
            check(get(path, null, "Accept", "text/html"), 401, "UNAUTHENTICATED");
            assertThat(get(path, "personal").body()).doesNotContain("snapshot", "identity", "owner", "objectKey", "bucket", "crypto", "share", "playback", "download", "mediaObjectId");
        }
        for (String path : List.of("/api/v1/customer/me", "/api/v1/customer/shares", "/api/v1/customer/business-numbers", "/api/v1/customer/subjects/" + SUBJECT)) {
            check(get(path, null), 401, "UNAUTHENTICATED");
            check(get(path, "personal"), 503, "SERVICE_UNAVAILABLE");
            check(request("POST", path, "personal", HttpRequest.BodyPublishers.noBody()), path.endsWith("shares") ? 422 : 404,
                    path.endsWith("shares") ? "VALIDATION_FAILED" : "NOT_FOUND");
        }
    }
}
