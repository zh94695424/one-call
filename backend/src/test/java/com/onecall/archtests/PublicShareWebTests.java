package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.sharing.internal.publicquery.*;
import com.onecall.core.sharing.internal.publicadmission.*;
import com.onecall.core.sharing.internal.publicquery.PublicShareView.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        String admissionPath = PATH + "/admissions";
        for (String verification : List.of("{\"mode\":\"NONE\"}", "{\"mode\":\"EXTRACTION_CODE\",\"code\":\"private\"}", "{\"mode\":\"DESIGNATED_MOBILE\",\"verificationProofId\":\"private\"}")) {
            String body = "{\"admissionRequestId\":\"stable\",\"verification\":" + verification + "}";
            assertThat(fixture.mvc.perform(post(admissionPath).servletPath(admissionPath).contentType("application/json").content(body)).andReturn().getResponse().getStatus()).isEqualTo(201);
        }
        String memberBody = "{\"admissionRequestId\":\"member\",\"verification\":{\"mode\":\"ORGANIZATION_MEMBER\"}}";
        assertThat(fixture.mvc.perform(post(admissionPath).servletPath(admissionPath).header("Authorization", "Bearer test")
                .contentType("application/json").content(memberBody)).andReturn().getResponse().getStatus()).isEqualTo(201);
        var capturedCaller = org.mockito.ArgumentCaptor.forClass(Optional.class);
        verify(fixture.admissions, atLeastOnce()).admit(any(), any(), capturedCaller.capture());
        assertThat(capturedCaller.getValue()).isPresent();
        assertThat(capturedCaller.getValue().orElseThrow()).isInstanceOfSatisfying(TrustedShareAdmissionCaller.class, trusted -> {
            assertThat(trusted.accountId()).isEqualTo(ACCOUNT);
            assertThat(trusted.organizationId()).isEqualTo(ORGANIZATION);
        });
        var committed = (ShareAdmissionService.AdmissionCommitted) fixture.admissions.admit(null,null,null);
        when(fixture.admissions.admit(any(), any(), any())).thenReturn(new ShareAdmissionService.DeniedCommitted(ShareAdmissionException.Code.SHARE_EXPIRED));
        problem(fixture.mvc.perform(post(admissionPath).servletPath(admissionPath).header("Authorization", "Bearer test")
                .contentType("application/json").content(memberBody)).andReturn().getResponse(), 410, "SHARE_EXPIRED");
        when(fixture.admissions.admit(any(),any(),any())).thenReturn(new ShareAdmissionService.ReplayCommitted(committed.view()));
        assertThat(fixture.mvc.perform(post(admissionPath).servletPath(admissionPath).contentType("application/json").content("{\"admissionRequestId\":\"stable\",\"verification\":{\"mode\":\"NONE\"}}")).andReturn().getResponse().getStatus()).isEqualTo(201);
        problem(fixture.mvc.perform(post(admissionPath).servletPath(admissionPath).contentType("application/json").content("{}")).andReturn().getResponse(),422,"VALIDATION_FAILED");
        problem(fixture.mvc.perform(post(admissionPath).servletPath(admissionPath).contentType("text/plain").content("{}")).andReturn().getResponse(),422,"VALIDATION_FAILED");
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
        for (String header : List.of("bad", "Bearer", "Basic private")) {
            problem(fixture.mvc.perform(post(PATH + "/admissions").servletPath(PATH + "/admissions").header("Authorization",header).content("{}")).andReturn().getResponse(),401,"UNAUTHENTICATED");
        }
        problem(fixture.mvc.perform(post(PATH + "/admissions").servletPath(PATH + "/admissions").header("Authorization","Bearer a","Bearer b").content("{}")).andReturn().getResponse(),401,"UNAUTHENTICATED");
        verifyNoInteractions(fixture.admissions);
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
        problem(missing.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH)
                .contentType("application/json").content(MEDIA_BODY)).andReturn().getResponse(), 503, "SERVICE_UNAVAILABLE");
        verifyNoInteractions(missing.service, missing.admissions, missing.mediaAuthorizations, missing.contexts, missing.verifier);
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
            case "SHARE_GENERATION_STALE" -> "Share generation stale";
            case "SHARE_SESSION_EXPIRED" -> "Share session expired";
            case "SHARE_LOCKED" -> "Share locked";
            case "SHARE_ADMISSION_LIMIT_REACHED" -> "Share admission limit reached";
            case "SHARE_SESSION_POLICY_UNAVAILABLE" -> "Share session policy unavailable";
            case "DEPENDENCY_UNAVAILABLE" -> "Dependency unavailable";
            case "SHARE_WRONG_TENANT" -> "Share tenant mismatch";
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
        final ShareAdmissionService admissions = mock(ShareAdmissionService.class);
        final ShareMediaAuthorizationService mediaAuthorizations = mock(ShareMediaAuthorizationService.class);
        final CustomerContextService contexts = mock(CustomerContextService.class);
        final CustomerSessionVerifier verifier = mock(CustomerSessionVerifier.class);
        final MockMvc mvc;
        final StaticListableBeanFactory beans = new StaticListableBeanFactory();
        VerificationMode mode = VerificationMode.NONE;

        Fixture(boolean withService, boolean withVerifier) {
            if (withService) {
                beans.addBean("publicShareQueryService", service);
                beans.addBean("shareAdmissionService", admissions);
                beans.addBean("shareMediaAuthorizationService", mediaAuthorizations);
            }
            when(admissions.admit(any(), any(), any())).thenReturn(new ShareAdmissionService.AdmissionCommitted(new ShareAdmissionView(UUID.randomUUID(),"NONE","NOT_REQUIRED",
                    Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:01:00Z"), Instant.parse("2026-01-01T00:02:00Z"),
                    new ShareAdmissionView.MediaAuthorization("synthetic-credential", Instant.parse("2026-01-01T00:00:30Z")))));
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
            var builder = MockMvcBuilders.standaloneSetup(new PublicSharesController(beans.getBeanProvider(PublicShareQueryService.class), beans.getBeanProvider(CustomerContextService.class), writer, beans.getBeanProvider(com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService.class), beans.getBeanProvider(ShareMediaAuthorizationService.class)))
                    .addFilters(new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), writer));
            if (converters.length > 0) builder.setMessageConverters(converters);
            return builder.build();
        }
    }
    private static final String MEDIA_PATH = PATH + "/admissions/30000000-0000-0000-0000-000000000001/media-authorizations";
    private static final String MEDIA_BODY = "{\"authorizationRequestId\":\"request-1\",\"purpose\":\"PLAYBACK\"}";

    @Test void mediaAuthorizationUsesExactRouteAndReturnsOnlyCredentialAndExpiresAt() throws Exception {
        Fixture fixture = new Fixture(true, true);
        Instant expiry = Instant.parse("2026-11-07T01:02:03Z");
        when(fixture.mediaAuthorizations.authorize(any(), any(), any(), any())).thenReturn(new ShareAdmissionView.MediaAuthorization("credential-canary", expiry));
        var response = fixture.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH).contentType("application/json").content(MEDIA_BODY)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        JsonNode json = MAPPER.readTree(response.getContentAsString());
        keys(json, "credential", "expiresAt");
        assertThat(json.path("credential").asString()).isEqualTo("credential-canary");
        assertThat(json.path("expiresAt").asString()).isEqualTo(expiry.toString());
        var captor = org.mockito.ArgumentCaptor.forClass(ShareMediaAuthorizationService.CallerResolver.class);
        verify(fixture.mediaAuthorizations).authorize(any(), eq(UUID.fromString("30000000-0000-0000-0000-000000000001")),
                argThat(r -> r.authorizationRequestId().equals("request-1") && r.purpose() == MediaAuthorizationRequest.Purpose.PLAYBACK), captor.capture());
        assertThat(captor.getValue().resolve()).isEmpty();
        problem(fixture.mvc.perform(post(PATH + "/admissions/a/media-authorizations").servletPath(PATH + "/admissions/a/media-authorizations")
                .contentType("application/json").content(MEDIA_BODY)).andReturn().getResponse(), 422, "VALIDATION_FAILED");
        var trailing = fixture.mvc.perform(post("/api/v1/public/shares/token/admissions/30000000-0000-0000-0000-000000000001/media-authorizations/")
                .with(r -> { r.setServletPath(MEDIA_PATH); return r; })).andReturn().getResponse();
        problem(trailing, 404, "NOT_FOUND");
    }

    @Test void mediaAuthorizationRejectsClosedBodyAndInvalidPurposeWith422() throws Exception {
        Fixture fixture = new Fixture(true, true);
        List<byte[]> invalid = List.of("{}".getBytes(), ("{"+"\"authorizationRequestId\":\"x\",\"purpose\":\"PLAYBACK\",\"extra\":1}").getBytes(),
                ("{\"authorizationRequestId\":\"x\",\"authorizationRequestId\":\"y\",\"purpose\":\"PLAYBACK\"}").getBytes(),
                "{\"authorizationRequestId\":\"x\",\"purpose\":\"PLAYBACK\"} {}".getBytes(),
                "{\"authorizationRequestId\":\"x\",\"purpose\":\"NOPE\"}".getBytes(),
                "{\"authorizationRequestId\":\"x\u0000\",\"purpose\":\"PLAYBACK\"}".getBytes(),
                ("{\"authorizationRequestId\":\""+"x".repeat(129)+"\",\"purpose\":\"PLAYBACK\"}").getBytes(),
                new byte[]{'{','"','a','"',':','"',(byte)0xc3,'"','}'});
        for (byte[] body : invalid) problem(fixture.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH).contentType("application/json").content(body)).andReturn().getResponse(), 422, "VALIDATION_FAILED");
        problem(fixture.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH).contentType("text/plain").content(MEDIA_BODY))
                .andReturn().getResponse(), 422, "VALIDATION_FAILED");
        verifyNoInteractions(fixture.mediaAuthorizations);
    }

    @Test void mediaAuthorizationMapsFrozenDeniedStatusesAndPrecedence() throws Exception {
        Fixture fixture = new Fixture(true, true);
        for (var entry : Map.of(ShareAdmissionException.Code.FORBIDDEN, 403, ShareAdmissionException.Code.SHARE_TOKEN_INVALID, 404,
                ShareAdmissionException.Code.SHARE_REVOKED, 410, ShareAdmissionException.Code.SHARE_EXPIRED, 410,
                ShareAdmissionException.Code.SHARE_SESSION_EXPIRED, 410, ShareAdmissionException.Code.SHARE_GENERATION_STALE, 409,
                ShareAdmissionException.Code.VALIDATION_FAILED, 422, ShareAdmissionException.Code.RATE_LIMITED, 429,
                ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE, 503).entrySet()) {
            doAnswer(i -> { throw new ShareAdmissionException(entry.getKey()); }).when(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());
            problem(fixture.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH).contentType("application/json").content(MEDIA_BODY)).andReturn().getResponse(), entry.getValue(), entry.getKey().name());
        }
        problem(fixture.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH).header("Authorization", "bad").contentType("application/json").content(MEDIA_BODY)).andReturn().getResponse(), 401, "UNAUTHENTICATED");
        verify(fixture.mediaAuthorizations, times(9)).authorize(any(), any(), any(), any());
    }

    @Test void organizationMemberMediaAuthorizationRequiresTrustedCurrentSession() throws Exception {
        Fixture fixture = new Fixture(true, true);
        when(fixture.mediaAuthorizations.authorize(any(), any(), any(), any())).thenAnswer(i -> {
            verifyNoInteractions(fixture.contexts);
            Optional<?> caller = i.<ShareMediaAuthorizationService.CallerResolver>getArgument(3).resolve();
            assertThat(caller).isPresent();
            assertThat(caller.orElseThrow()).isEqualTo(new TrustedShareAdmissionCaller(ACCOUNT, ORGANIZATION));
            return new ShareAdmissionView.MediaAuthorization("ok", Instant.parse("2026-11-07T01:00:00Z"));
        });
        var request = post(MEDIA_PATH).servletPath(MEDIA_PATH).header("Authorization", "Bearer test").contentType("application/json").content(MEDIA_BODY);
        assertThat(fixture.mvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(200);
        verify(fixture.contexts).getCustomerMe(any());
        verify(fixture.verifier).verify(any(), eq("onecall:customer-session:v1"));
        verify(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());

        record WorkspaceDenial(SelectedCustomerWorkspace selected, CustomerContext current, String code) { }
        UUID otherOrganization = UUID.fromString("40000000-0000-0000-0000-000000000002");
        var multipleOrganizations = new ArrayList<>(context(true).workspaces());
        multipleOrganizations.add(new CustomerContext.WorkspaceSummary(CustomerContext.WorkspaceType.ORGANIZATION,
                otherOrganization, "Other", CustomerContext.OrganizationRole.MEMBER, CustomerContext.WorkspaceStatus.ACTIVE));
        for (var denial : List.of(
                new WorkspaceDenial(null, new CustomerContext(new CustomerContext.AccountSummary(
                        UUID.fromString("10000000-0000-0000-0000-000000000002"), "Synthetic", CustomerContext.AccountStatus.ACTIVE),
                        context(true).workspaces(), TrustedCustomerCaller.Channel.WEB), "SHARE_WRONG_TENANT"),
                new WorkspaceDenial(null, new CustomerContext(new CustomerContext.AccountSummary(
                        ACCOUNT, "Synthetic", CustomerContext.AccountStatus.DISABLED),
                        context(true).workspaces(), TrustedCustomerCaller.Channel.WEB), "FORBIDDEN"),
                new WorkspaceDenial(new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, ORGANIZATION), context(true), "SHARE_WRONG_TENANT"),
                new WorkspaceDenial(new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, null), context(true), "SHARE_WRONG_TENANT"),
                new WorkspaceDenial(new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, otherOrganization), context(true), "SHARE_WRONG_TENANT"),
                new WorkspaceDenial(null, context(false), "FORBIDDEN"),
                new WorkspaceDenial(null, new CustomerContext(context(true).account(), multipleOrganizations, TrustedCustomerCaller.Channel.WEB), "FORBIDDEN"))) {
            when(fixture.verifier.verify(any(), anyString())).thenReturn(new VerifiedCustomerSession(ACCOUNT,
                    TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1", denial.selected()));
            when(fixture.contexts.getCustomerMe(any())).thenReturn(denial.current());
            doAnswer(i -> {
                verifyNoInteractions(fixture.contexts);
                i.<ShareMediaAuthorizationService.CallerResolver>getArgument(3).resolve();
                throw new AssertionError("Invalid workspace must not authorize a member");
            }).when(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());
            clearInvocations(fixture.contexts, fixture.verifier, fixture.mediaAuthorizations);
            problem(fixture.mvc.perform(request).andReturn().getResponse(), 403, denial.code());
            verify(fixture.contexts, times(1)).getCustomerMe(argThat(caller -> caller.accountId().equals(ACCOUNT)
                    && Objects.equals(caller.selectedWorkspace(), denial.selected())));
            verify(fixture.verifier, times(1)).verify(any(), eq("onecall:customer-session:v1"));
            verify(fixture.mediaAuthorizations, times(1)).authorize(any(), any(), any(), any());
            verifyNoMoreInteractions(fixture.contexts, fixture.verifier, fixture.mediaAuthorizations);
        }
        when(fixture.verifier.verify(any(), anyString())).thenReturn(new VerifiedCustomerSession(ACCOUNT,
                TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1"));
        when(fixture.contexts.getCustomerMe(any())).thenReturn(context(true));

        clearInvocations(fixture.contexts, fixture.verifier, fixture.mediaAuthorizations);
        doAnswer(i -> {
            verifyNoInteractions(fixture.contexts);
            assertThat(i.<ShareMediaAuthorizationService.CallerResolver>getArgument(3).resolve()).isEmpty();
            verifyNoInteractions(fixture.contexts);
            throw new ShareAdmissionException(ShareAdmissionException.Code.UNAUTHENTICATED);
        }).when(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());
        problem(fixture.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH).contentType("application/json").content(MEDIA_BODY))
                .andReturn().getResponse(), 401, "UNAUTHENTICATED");
        verify(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());
        verifyNoInteractions(fixture.contexts, fixture.verifier);

        clearInvocations(fixture.mediaAuthorizations);
        when(fixture.contexts.getCustomerMe(any())).thenThrow(new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE));
        clearInvocations(fixture.contexts);
        doAnswer(i -> {
            verifyNoInteractions(fixture.contexts);
            i.<ShareMediaAuthorizationService.CallerResolver>getArgument(3).resolve();
            throw new AssertionError("Unavailable context must not authorize a member");
        }).when(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());
        problem(fixture.mvc.perform(request).andReturn().getResponse(), 503, "SERVICE_UNAVAILABLE");
        verify(fixture.contexts).getCustomerMe(any());
        verify(fixture.verifier).verify(any(), eq("onecall:customer-session:v1"));
        verify(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());

        clearInvocations(fixture.contexts, fixture.verifier, fixture.mediaAuthorizations);
        doAnswer(i -> {
            verifyNoInteractions(fixture.contexts);
            assertThat(i.<ShareMediaAuthorizationService.CallerResolver>getArgument(3)).isNotNull();
            return new ShareAdmissionView.MediaAuthorization("non-member-grant", Instant.parse("2026-11-07T01:00:00Z"));
        }).when(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());
        assertThat(fixture.mvc.perform(request).andReturn().getResponse().getStatus()).isEqualTo(200);
        verify(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());
        verify(fixture.verifier).verify(any(), eq("onecall:customer-session:v1"));
        verifyNoInteractions(fixture.contexts);

        clearInvocations(fixture.verifier, fixture.mediaAuthorizations);
        doAnswer(i -> {
            verifyNoInteractions(fixture.contexts);
            assertThat(i.<ShareMediaAuthorizationService.CallerResolver>getArgument(3)).isNotNull();
            throw new ShareAdmissionException(ShareAdmissionException.Code.SHARE_TOKEN_INVALID);
        }).when(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());
        String invalidPath = MEDIA_PATH.replace("/shares/token/", "/shares/invalid-token/");
        problem(fixture.mvc.perform(post(invalidPath).servletPath(invalidPath).header("Authorization", "Bearer test")
                .contentType("application/json").content(MEDIA_BODY)).andReturn().getResponse(), 404, "SHARE_TOKEN_INVALID");
        verify(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());
        verify(fixture.verifier).verify(any(), eq("onecall:customer-session:v1"));
        verifyNoInteractions(fixture.contexts);

        clearInvocations(fixture.mediaAuthorizations, fixture.verifier);
        problem(fixture.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH).header("Authorization", "bad")
                .contentType("application/json").content(MEDIA_BODY)).andReturn().getResponse(), 401, "UNAUTHENTICATED");
        verifyNoInteractions(fixture.contexts, fixture.mediaAuthorizations, fixture.verifier);
    }

    @Test void mediaAuthorizationReplayAndConflictUseFrozenProblemSurface() throws Exception {
        Fixture fixture = new Fixture(true, true);
        Instant expiry = Instant.parse("2026-11-07T01:00:00Z");
        when(fixture.mediaAuthorizations.authorize(any(), any(), any(), any()))
                .thenReturn(new ShareAdmissionView.MediaAuthorization("SECRET_CANARY", expiry))
                .thenThrow(new ShareAdmissionException(ShareAdmissionException.Code.SHARE_GENERATION_STALE));
        var request = post(MEDIA_PATH).servletPath(MEDIA_PATH).contentType("application/json").content(MEDIA_BODY);
        var success = fixture.mvc.perform(request).andReturn().getResponse();
        assertThat(success.getStatus()).isEqualTo(200);
        assertThat(success.getContentType()).startsWith("application/json");
        assertThat(success.getHeader("Cache-Control")).isEqualTo("no-store");
        var json = MAPPER.readTree(success.getContentAsString());
        keys(json, "credential", "expiresAt");
        assertThat(json).isEqualTo(MAPPER.valueToTree(Map.of("credential", "SECRET_CANARY", "expiresAt", expiry.toString())));
        assertThat(success.getContentAsString()).doesNotContain("token", "request-1", "30000000-0000-0000-0000-000000000001",
                ACCOUNT.toString(), ORGANIZATION.toString(), "grant", "admissionId", "object", "url", "redis", "provider");
        var conflict = fixture.mvc.perform(request).andReturn().getResponse();
        problem(conflict, 409, "SHARE_GENERATION_STALE");
        assertThat(conflict.getContentAsString()).doesNotContain("SECRET_CANARY", "credential", "expiresAt", "token", "request-1",
                "30000000-0000-0000-0000-000000000001", ORGANIZATION.toString(), "grant", "object", "url", "redis", "provider");
        var requests = org.mockito.ArgumentCaptor.forClass(MediaAuthorizationRequest.class);
        verify(fixture.mediaAuthorizations, times(2)).authorize(any(), eq(UUID.fromString("30000000-0000-0000-0000-000000000001")),
                requests.capture(), any());
        assertThat(requests.getAllValues()).containsExactly(new MediaAuthorizationRequest("request-1", MediaAuthorizationRequest.Purpose.PLAYBACK),
                new MediaAuthorizationRequest("request-1", MediaAuthorizationRequest.Purpose.PLAYBACK));
        verifyNoInteractions(fixture.contexts, fixture.verifier);
        for (var code : List.of(ShareAdmissionException.Code.SHARE_LOCKED, ShareAdmissionException.Code.SHARE_ADMISSION_LIMIT_REACHED,
                ShareAdmissionException.Code.SHARE_SESSION_POLICY_UNAVAILABLE)) {
            doAnswer(i -> { throw new ShareAdmissionException(code); }).when(fixture.mediaAuthorizations).authorize(any(), any(), any(), any());
            var response = fixture.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH).contentType("application/json").content(MEDIA_BODY)).andReturn().getResponse();
            assertThat(response.getStatus()).isIn(409, 503);
            assertThat(response.getContentAsString()).contains("\"code\":\"" + code.name() + "\"");
        }
    }

    @Test void mediaAuthorizationResponseAndProblemNeverLeakInternalMediaFacts() throws Exception {
        Fixture fixture = new Fixture(true, true);
        when(fixture.mediaAuthorizations.authorize(any(), any(), any(), any())).thenReturn(new ShareAdmissionView.MediaAuthorization("SECRET_CANARY", Instant.parse("2026-11-07T01:00:00Z")));
        String body = fixture.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH).contentType("application/json").content(MEDIA_BODY)).andReturn().getResponse().getContentAsString();
        assertThat(body).contains("SECRET_CANARY").doesNotContain("token", "admissionId", "grant", "object", "url", "redis", "provider");
        when(fixture.mediaAuthorizations.authorize(any(), any(), any(), any())).thenThrow(new ShareAdmissionException(ShareAdmissionException.Code.SERVICE_UNAVAILABLE));
        String problem = fixture.mvc.perform(post(MEDIA_PATH).servletPath(MEDIA_PATH).contentType("application/json").content(MEDIA_BODY)).andReturn().getResponse().getContentAsString();
        assertThat(problem).doesNotContain("SECRET_CANARY", "30000000-0000-0000-0000-000000000001", "token", "object", "redis", "provider");
    }

}
