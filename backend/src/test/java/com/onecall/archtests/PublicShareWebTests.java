package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.sharing.internal.publicquery.*;
import com.onecall.core.sharing.internal.publicquery.PublicShareView.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class PublicShareWebTests {
    private static final String PATH = "/api/v1/public/shares/token";
    private static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final java.util.Set<String> TRACES = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Test
    void exactPublicGetRoutePrecedesOptionalAuthentication() throws Exception {
        Fixture fixture = new Fixture(true, true);
        assertThat(fixture.mvc.perform(get(PATH).servletPath(PATH)).andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(fixture.mvc.perform(get("/ctx" + PATH).contextPath("/ctx").servletPath(PATH)).andReturn().getResponse().getStatus()).isEqualTo(200);
        clearInvocations(fixture.service, fixture.verifier);
        for (String path : List.of("/api/v1/public/shares", PATH + "/", PATH + "/admissions", PATH + ";x=1",
                "/api/v1/public/shares/a%2Fb", "/api/v1/public/shares/a%20b", "/api/v1/public//shares/token", "/API/v1/public/shares/token")) {
            problem(fixture.mvc.perform(get(path).with(request -> { request.setServletPath(path); return request; }).header("Authorization", "bad").header("Accept", "text/html"))
                    .andReturn().getResponse(), 404, "NOT_FOUND");
        }
        for (String method : List.of("POST", "HEAD", "OPTIONS", "PUT", "DELETE")) {
            var response = fixture.mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), PATH).servletPath(PATH)
                    .header("Authorization", "bad")).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(404);
            problem(response, 404, "NOT_FOUND");
        }
        verifyNoInteractions(fixture.service, fixture.verifier);
        problem(fixture.mvc.perform(get("/api/v1/customer/me").servletPath("/api/v1/customer/me")).andReturn().getResponse(), 401, "UNAUTHENTICATED");
        when(fixture.service.getPublicShare(any(), any())).thenThrow(new PublicShareQueryException(PublicShareQueryException.Code.SHARE_TOKEN_INVALID));
        problem(fixture.mvc.perform(get(PATH).servletPath(PATH)).andReturn().getResponse(), 404, "SHARE_TOKEN_INVALID");
    }

    @Test
    void optionalBearerFailuresKeepFrozenPrecedence() throws Exception {
        Fixture absent = new Fixture(true, false);
        assertThat(absent.mvc.perform(get(PATH).servletPath(PATH)).andReturn().getResponse().getStatus()).isEqualTo(200);
        problem(absent.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse(), 503, "SERVICE_UNAVAILABLE");
        Fixture fixture = new Fixture(true, true);
        for (String header : List.of("", " ", "bad", "Bearer", "Bearer test extra", "Basic test")) {
            problem(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", header).header("Accept", "text/html"))
                    .andReturn().getResponse(), 401, "UNAUTHENTICATED");
        }
        problem(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer a", "Bearer b")).andReturn().getResponse(), 401, "UNAUTHENTICATED");
        verifyNoInteractions(fixture.verifier, fixture.service);
        assertThat(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse().getStatus()).isEqualTo(200);
        when(fixture.verifier.verify(any(), anyString())).thenReturn(new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, "wrong"));
        problem(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse(), 401, "UNAUTHENTICATED");
        when(fixture.verifier.verify(any(), anyString())).thenReturn(null);
        problem(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse(), 503, "SERVICE_UNAVAILABLE");
        when(fixture.verifier.verify(any(), anyString())).thenThrow(new IllegalStateException("SECRET_CANARY"));
        problem(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse(), 503, "SERVICE_UNAVAILABLE");
        for (var code : CustomerSessionVerificationException.Code.values()) {
            doThrow(new CustomerSessionVerificationException(code)).when(fixture.verifier).verify(any(), anyString());
            problem(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse(),
                    code == CustomerSessionVerificationException.Code.UNAUTHENTICATED ? 401 : 503,
                    code == CustomerSessionVerificationException.Code.UNAUTHENTICATED ? "UNAUTHENTICATED" : "SERVICE_UNAVAILABLE");
        }
        Fixture ambiguous = new Fixture(true, true);
        ambiguous.beans.addBean("secondVerifier", mock(CustomerSessionVerifier.class));
        problem(ambiguous.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse(), 503, "SERVICE_UNAVAILABLE");
        verifyNoInteractions(ambiguous.verifier);
    }

    @Test
    void tokenShapeAndLookupFailuresUseClosedProblems() throws Exception {
        Fixture fixture = new Fixture(true, true);
        for (int size : List.of(1, 512)) {
            String path = "/api/v1/public/shares/" + "a".repeat(size);
            assertThat(fixture.mvc.perform(get(path).servletPath(path)).andReturn().getResponse().getStatus()).isEqualTo(200);
        }
        Fixture missing = new Fixture(false, false);
        String oversized = "/api/v1/public/shares/" + "a".repeat(513);
        problem(missing.mvc.perform(get(oversized).servletPath(oversized)).andReturn().getResponse(), 422, "VALIDATION_FAILED");
        problem(missing.mvc.perform(get(PATH).servletPath(PATH)).andReturn().getResponse(), 503, "SERVICE_UNAVAILABLE");
        Map<PublicShareQueryException.Code, Integer> statuses = Map.of(
                PublicShareQueryException.Code.UNAUTHENTICATED, 401, PublicShareQueryException.Code.FORBIDDEN, 403,
                PublicShareQueryException.Code.VALIDATION_FAILED, 422, PublicShareQueryException.Code.SHARE_TOKEN_INVALID, 404,
                PublicShareQueryException.Code.SHARE_REVOKED, 410, PublicShareQueryException.Code.SHARE_EXPIRED, 410,
                PublicShareQueryException.Code.RATE_LIMITED, 429, PublicShareQueryException.Code.SERVICE_UNAVAILABLE, 503,
                PublicShareQueryException.Code.INTERNAL, 500);
        for (var entry : statuses.entrySet()) {
            doThrow(new PublicShareQueryException(entry.getKey())).when(fixture.service).getPublicShare(any(), any());
            problem(fixture.mvc.perform(get(PATH).servletPath(PATH).header("X-Trace-Id", "CLIENT_CANARY")).andReturn().getResponse(), entry.getValue(), entry.getKey().name());
        }
        doThrow(new IllegalStateException("SECRET_CANARY")).when(fixture.service).getPublicShare(any(), any());
        problem(fixture.mvc.perform(get(PATH).servletPath(PATH)).andReturn().getResponse(), 500, "INTERNAL");
    }

    @Test
    void publicDtoIsClosedAndContentIsOmittedForNonmembers() throws Exception {
        Fixture fixture = new Fixture(true, true);
        for (VerificationMode mode : VerificationMode.values()) {
            fixture.mode = mode;
            var response = fixture.mvc.perform(get(PATH).servletPath(PATH)).andReturn().getResponse();
            JsonNode json = MAPPER.readTree(response.getContentAsString());
            keys(json, "verificationMode", "status", "expiresAt", "downloadAllowed", mode == VerificationMode.ORGANIZATION_MEMBER ? "verificationChallenge" : "content");
            if (mode == VerificationMode.ORGANIZATION_MEMBER) {
                assertThat(json.has("content")).isFalse();
                keys(json.path("verificationChallenge"), "customerSessionRequired");
                assertThat(json.path("verificationChallenge").path("customerSessionRequired").asBoolean()).isTrue();
            } else {
                keys(json.path("content"), "capturedAt", "artifacts");
                keys(json.path("content").path("artifacts").get(0), "artifactRole", "mimeType", "sizeBytes", "sha256");
                assertThat(json.path("content").path("artifacts").get(0).path("sha256").asString()).isEqualTo("a".repeat(64));
                assertThat(json.has("verificationChallenge")).isFalse();
            }
            assertThat(response.getContentAsString()).doesNotContain("null", ACCOUNT.toString(), ORGANIZATION.toString(), "token", "mediaObject", "sourceRef");
        }
    }

    @Test
    void memberSessionMatchAndNonmatchNeverLeakContent() throws Exception {
        Fixture fixture = new Fixture(true, true);
        fixture.mode = VerificationMode.ORGANIZATION_MEMBER;
        for (SelectedCustomerWorkspace selection : new SelectedCustomerWorkspace[] {null,
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null),
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, UUID.randomUUID())}) {
            when(fixture.verifier.verify(any(), anyString())).thenReturn(new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1", selection));
            var response = fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")
                    .header("X-Organization-Id", UUID.randomUUID().toString())).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(MAPPER.readTree(response.getContentAsString()).has("content")).isTrue();
        }
        when(fixture.contexts.getCustomerMe(any())).thenReturn(context(false));
        var nonmatch = fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse();
        assertThat(nonmatch.getStatus()).isEqualTo(200);
        assertThat(MAPPER.readTree(nonmatch.getContentAsString()).has("content")).isFalse();
        for (var code : List.of(CustomerContextException.Code.UNAUTHENTICATED, CustomerContextException.Code.SERVICE_UNAVAILABLE, CustomerContextException.Code.INTERNAL)) {
            doThrow(new CustomerContextException(code)).when(fixture.contexts).getCustomerMe(any());
            problem(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse(),
                    code == CustomerContextException.Code.UNAUTHENTICATED ? 401 : code == CustomerContextException.Code.INTERNAL ? 500 : 503, code.name());
        }
        clearInvocations(fixture.contexts);
        assertThat(fixture.mvc.perform(get(PATH).servletPath(PATH).principal(() -> "CLIENT_CANARY")).andReturn().getResponse().getStatus()).isEqualTo(200);
        fixture.mode = VerificationMode.NONE;
        assertThat(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Authorization", "Bearer test")).andReturn().getResponse().getStatus()).isEqualTo(200);
        verifyNoInteractions(fixture.contexts);
    }

    @Test
    void prefetchAndNegotiationHaveNoAuthorizationSideEffects() throws Exception {
        Fixture fixture = new Fixture(true, true);
        String baseline = fixture.mvc.perform(get(PATH).servletPath(PATH)).andReturn().getResponse().getContentAsString();
        for (String purpose : List.of("prefetch", "prefetch;prerender", "navigate")) {
            var response = fixture.mvc.perform(get(PATH + "?organizationId=CLIENT_CANARY").servletPath(PATH)
                    .header("Purpose", purpose).header("Sec-Purpose", purpose).header("Idempotency-Key", "CLIENT_CANARY")
                    .content("{\"verification\":\"CLIENT_CANARY\"}")).andReturn().getResponse();
            assertThat(response.getContentAsString()).isEqualTo(baseline);
            assertThat(response.getHeader("Set-Cookie")).isNull();
            assertThat(response.getHeader("Location")).isNull();
            assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        }
        for (String accept : List.of("text/html", "application/json;q=0", "invalid")) {
            problem(fixture.mvc.perform(get(PATH).servletPath(PATH).header("Accept", accept)).andReturn().getResponse(), 500, "INTERNAL");
        }
        verifyNoInteractions(fixture.verifier, fixture.contexts);
        var failingConverter = new org.springframework.http.converter.AbstractHttpMessageConverter<PublicShareView>(org.springframework.http.MediaType.APPLICATION_JSON) {
            protected boolean supports(Class<?> type) { return type == PublicShareView.class; }
            protected PublicShareView readInternal(Class<? extends PublicShareView> type, org.springframework.http.HttpInputMessage input) { throw new UnsupportedOperationException(); }
            protected void writeInternal(PublicShareView value, org.springframework.http.HttpOutputMessage output) throws java.io.IOException {
                output.getBody().write("PARTIAL_SECRET_CANARY".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                throw new java.io.IOException("SERIALIZATION_CANARY");
            }
        };
        var broken = fixture.mvc(failingConverter).perform(get(PATH).servletPath(PATH)).andReturn().getResponse();
        problem(broken, 500, "INTERNAL");
        assertThat(broken.getContentAsString()).doesNotContain("PARTIAL", "SERIALIZATION_CANARY", "content");
    }

    private static CustomerContext context(boolean member) {
        var personal = new CustomerContext.WorkspaceSummary(CustomerContext.WorkspaceType.PERSONAL, null, null, null, CustomerContext.WorkspaceStatus.ACTIVE);
        var organization = new CustomerContext.WorkspaceSummary(CustomerContext.WorkspaceType.ORGANIZATION, ORGANIZATION, "Synthetic", CustomerContext.OrganizationRole.MEMBER, CustomerContext.WorkspaceStatus.ACTIVE);
        return new CustomerContext(new CustomerContext.AccountSummary(ACCOUNT, "Synthetic", CustomerContext.AccountStatus.ACTIVE),
                member ? List.of(personal, organization) : List.of(personal), TrustedCustomerCaller.Channel.WEB);
    }

    private static void keys(JsonNode json, String... expected) { assertThat(json.properties().stream().map(Map.Entry::getKey)).containsExactlyInAnyOrder(expected); }
    private static void problem(MockHttpServletResponse response, int status, String code) throws Exception {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        var json = MAPPER.readTree(response.getContentAsString());
        keys(json, "type", "title", "status", "code", "traceId");
        assertThat(json.path("type").asString()).isEqualTo("about:blank");
        assertThat(json.path("code").asString()).isEqualTo(code);
        assertThat(json.path("status").asInt()).isEqualTo(status);
        assertThat(UUID.fromString(json.path("traceId").asString())).isNotNull();
        assertThat(TRACES.add(json.path("traceId").asString())).isTrue();
        assertThat(json.path("title").asString()).isEqualTo(switch (code) {
            case "NOT_FOUND" -> "Not found";
            case "SHARE_TOKEN_INVALID" -> "Invalid share token";
            case "SHARE_REVOKED" -> "Share revoked";
            case "SHARE_EXPIRED" -> "Share expired";
            case "UNAUTHENTICATED" -> "Authentication required";
            case "FORBIDDEN" -> "Access denied";
            case "VALIDATION_FAILED" -> "Request validation failed";
            case "RATE_LIMITED" -> "Too many requests";
            case "SERVICE_UNAVAILABLE" -> "Service unavailable";
            default -> "Internal error";
        });
        assertThat(response.getContentAsString()).doesNotContain("SECRET_CANARY", "CLIENT_CANARY", "/api/", ACCOUNT.toString(), "\"capabilityToken\"");
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo(status == 401 ? "Bearer" : null);
    }

    private static final class Fixture {
        final PublicShareQueryService service = mock(PublicShareQueryService.class);
        final CustomerContextService contexts = mock(CustomerContextService.class);
        final CustomerSessionVerifier verifier = mock(CustomerSessionVerifier.class);
        final MockMvc mvc;
        final StaticListableBeanFactory beans = new StaticListableBeanFactory();
        VerificationMode mode = VerificationMode.NONE;

        Fixture(boolean withService, boolean withVerifier) {
            if (withService) beans.addBean("publicShareQueryService", service);
            beans.addBean("customerContextService", contexts);
            if (withVerifier) beans.addBean("customerSessionVerifier", verifier);
            when(verifier.verify(any(), anyString())).thenReturn(new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1"));
            when(contexts.getCustomerMe(any())).thenReturn(context(true));
            when(service.getPublicShare(any(), any())).thenAnswer(invocation -> {
                var match = mode == VerificationMode.ORGANIZATION_MEMBER
                        ? invocation.<PublicShareQueryService.MemberAccess>getArgument(1).check(ORGANIZATION) : PublicShareQueryService.MemberMatch.MATCH;
                if (match == PublicShareQueryService.MemberMatch.UNAUTHENTICATED) throw new PublicShareQueryException(PublicShareQueryException.Code.UNAUTHENTICATED);
                if (match == PublicShareQueryService.MemberMatch.UNAVAILABLE) throw new PublicShareQueryException(PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
                if (match == PublicShareQueryService.MemberMatch.INTERNAL) throw new PublicShareQueryException(PublicShareQueryException.Code.INTERNAL);
                return new PublicShareView(mode, Status.ACTIVE, Instant.parse("2026-11-07T00:00:00Z"), false,
                        match == PublicShareQueryService.MemberMatch.MATCH ? new Content(Instant.parse("2026-11-06T00:00:00Z"), null,
                                List.of(new Artifact(ArtifactRole.ORIGINAL, "audio/wav", 20, "a".repeat(64)))) : null,
                        match == PublicShareQueryService.MemberMatch.MATCH ? null : new VerificationChallenge(true));
            });
            mvc = mvc();
        }
        MockMvc mvc(org.springframework.http.converter.HttpMessageConverter<?>... converters) {
            var writer = new CustomerProblemWriter();
            var builder = MockMvcBuilders.standaloneSetup(new PublicSharesController(beans.getBeanProvider(PublicShareQueryService.class), beans.getBeanProvider(CustomerContextService.class), writer))
                    .addFilters(new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), writer));
            if (converters.length > 0) builder.setMessageConverters(converters);
            return builder.build();
        }
    }
}
