package com.onecall.archtests;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.customer_iam.internal.security.CustomerBearerAuthenticator;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier;
import com.onecall.core.customer_iam.internal.web.CustomerBoundaryFilter;
import com.onecall.core.customer_iam.internal.web.CustomerProblemWriter;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class OperationsCatalogDraftWebTests {
    static final UUID PRODUCT = new UUID(0, 1), VERSION = new UUID(0, 2), EMPLOYEE = new UUID(0, 3);
    static final String BASE = "/api/v1/operations/catalog/versions";
    static final String COMMERCIAL = "\"offerType\":\"ONE_TIME\",\"audience\":\"PERSONAL\",\"priceCents\":0,\"currency\":\"CNY\",\"billingCycle\":\"ONCE\",\"installments\":1,\"grantIntervalMonths\":0";
    static final String CREATE = "{\"productId\":\"" + PRODUCT + "\",\"versionNo\":1," + COMMERCIAL + ",\"benefitLines\":[],\"channels\":[]}";
    static final String REPLACE = "{\"expectedRevision\":1," + COMMERCIAL + "}";
    static final String HASH = "eecbe2e59df56e661efa42fb92262cc6a8d507646ecf964b7aa1820f6ed7a35a";
    final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    final OperationsCatalogDraftCommand command = mock(OperationsCatalogDraftCommand.class);
    final SyntheticEmployeeSessions sessions = new SyntheticEmployeeSessions();
    final AtomicInteger customerCalls = new AtomicInteger(), consumed = new AtomicInteger();
    final CatalogVersionDraft draft = new CatalogVersionDraft(VERSION, PRODUCT, 1, "DRAFT", "ONE_TIME", "PERSONAL", 0L, "CNY", "ONCE", 1, 0, List.of(), List.of(), 1, Instant.parse("2026-11-20T00:00:00.123456Z"), null, HASH);
    MockMvc mvc;
    byte[] streamed;
    int failAfter = -1;
    @BeforeEach void setup() {
        sessions.sessions.put("valid", new VerifiedEmployeeSession(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE)); sessions.sessions.put("wrong", new VerifiedEmployeeSession(EMPLOYEE, "customer"));
        beans.registerSingleton("verifier", sessions); beans.registerSingleton("command", command); beans.registerSingleton("customer", (CustomerSessionVerifier) (token, audience) -> { customerCalls.incrementAndGet(); throw new AssertionError("customer invoked"); });
        when(command.create(any())).thenReturn(new CreateResult(draft, true)); when(command.replace(any())).thenReturn(draft);
        var problems = new OperationsProblemWriter();
        mvc = MockMvcBuilders.standaloneSetup(new OperationsCatalogDraftController(beans.getBeanProvider(OperationsCatalogDraftCommand.class), problems))
                .addFilters((request, response, chain) -> chain.doFilter(streamed == null ? request : new HttpServletRequestWrapper((HttpServletRequest) request) {
                    @Override public ServletInputStream getInputStream() { return new ServletInputStream() {
                        @Override public int read() throws IOException { if (failAfter >= 0 && consumed.get() >= failAfter) throw new IOException("PRIVATE"); return consumed.get() == streamed.length ? -1 : Byte.toUnsignedInt(streamed[consumed.getAndIncrement()]); }
                        @Override public boolean isFinished() { return consumed.get() == streamed.length; }
                        @Override public boolean isReady() { return true; }
                        @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
                    }; }
                }, response), new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(beans.getBeanProvider(EmployeeSessionVerifier.class)), problems), new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter())).build();
    }
    String path(boolean replace) { return replace ? BASE + "/" + VERSION + "/draft" : BASE; }
    MockHttpServletRequestBuilder request(org.springframework.http.HttpMethod method, String path) { return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(method, path); }
    MockHttpServletRequestBuilder request(boolean replace) { String path = path(replace); return (replace ? put(path) : post(path)).servletPath(path).header("Authorization", "Bearer valid").contentType("application/json").accept("application/json").content(replace ? REPLACE : CREATE); }
    void headers(MvcResult result) { var response = result.getResponse(); assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store"); assertThat(response.getHeader("Pragma")).isEqualTo("no-cache"); assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff"); assertThat(response.getContentLength()).isEqualTo(response.getContentAsByteArray().length); assertThat(customerCalls.get()).isZero(); }
    void problem(MvcResult result, int status, String code, String title) throws Exception { var response = result.getResponse(); headers(result); assertThat(response.getStatus()).isEqualTo(status); assertThat(response.getContentType()).isEqualTo("application/problem+json"); var json = JsonMapper.builder().build().readTree(response.getContentAsByteArray()); UUID.fromString(json.get("traceId").asString()); assertThat(response.getContentAsString()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status + ",\"code\":\"" + code + "\",\"traceId\":\"" + json.get("traceId").asString() + "\"}"); assertThat(response.getHeader("WWW-Authenticate")).isEqualTo(status == 401 ? "Bearer" : null); }
    @Test void admitsOnlyExactCreatePostAndReplacePut() throws Exception { for (boolean replace : List.of(false, true)) { var result = mvc.perform(request(replace)).andReturn(); assertThat(result.getResponse().getStatus()).isEqualTo(replace ? 200 : 201); headers(result); } }
    @Test void concealsAllOtherCatalogMutationTemplatesBeforeAuthentication() throws Exception { for (String method : List.of("GET", "PATCH", "DELETE", "HEAD", "OPTIONS", "PUT")) problem(mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), BASE).servletPath(BASE)).andReturn(), 404, "NOT_FOUND", "Not found"); for (String path : List.of(BASE + "/", BASE + "/x/draft/", BASE + "/x;bad/draft", BASE + "/x%2f/draft", BASE + "/x/other", BASE + "/x/draft/extra")) problem(mvc.perform(request(path.equals(BASE + "/") ? org.springframework.http.HttpMethod.POST : org.springframework.http.HttpMethod.PUT, path).with(value -> { value.setServletPath(path); return value; })).andReturn(), 404, "NOT_FOUND", "Not found"); String draftPath = BASE + "/" + VERSION + "/draft"; problem(mvc.perform(post(draftPath).servletPath(draftPath)).andReturn(), 404, "NOT_FOUND", "Not found"); verifyNoInteractions(command); }
    @Test void preservesFullFilterChainAndNeverInvokesCustomerAuthentication() throws Exception { for (boolean replace : List.of(false, true)) { var result = mvc.perform(request(replace)).andReturn(); headers(result); if (replace) verify(command).replace(argThat(value -> value.caller().employeeId().equals(EMPLOYEE))); else verify(command).create(argThat(value -> value.caller().employeeId().equals(EMPLOYEE))); } assertThat(customerCalls.get()).isZero(); }
    @Test void authenticationAndAudiencePrecedeBodyAndProviderResolution() throws Exception { beans.destroySingleton("command"); for (boolean replace : List.of(false, true)) for (String token : List.of("", "wrong", "invalid")) { streamed = new byte[50]; consumed.set(0); failAfter = 0; problem(mvc.perform(request(replace).header("Authorization", "Bearer " + token).contentType("text/plain")).andReturn(), 401, "UNAUTHENTICATED", "Authentication required"); assertThat(consumed.get()).isZero(); } }
    @Test void shippedContentTypeAndAcceptOutcomesRemainExact() throws Exception { for (boolean replace : List.of(false, true)) { for (String media : List.of("text/plain", "application/json;charset=iso-8859-1", "application/json;foo=bar")) problem(mvc.perform(request(replace).contentType(media)).andReturn(), 422, "VALIDATION_FAILED", "Request validation failed"); for (String accept : List.of("text/plain", "application/json;q=0", "bad")) problem(mvc.perform(request(replace).accept(accept)).andReturn(), 500, "INTERNAL", "Internal error"); assertThat(mvc.perform(request(replace).contentType("Application/JSON;charset=UTF-8")).andReturn().getResponse().getStatus()).isEqualTo(replace ? 200 : 201); } }
    @Test void structuralValidationPrecedesProviderAndAuthority() throws Exception { beans.destroySingleton("command"); for (boolean replace : List.of(false, true)) { problem(mvc.perform(request(replace).content("{}")).andReturn(), 422, "VALIDATION_FAILED", "Request validation failed"); problem(mvc.perform(request(replace)).andReturn(), 503, "SERVICE_UNAVAILABLE", "Service unavailable"); problem(mvc.perform(request(replace).content((replace ? REPLACE : CREATE).replace("PERSONAL", "ENTERPRISE"))).andReturn(), 503, "SERVICE_UNAVAILABLE", "Service unavailable"); } beans.registerSingleton("command", command); when(command.create(any())).thenThrow(new Failure(Code.VALIDATION_FAILED)); problem(mvc.perform(request(false).content(CREATE.replace("PERSONAL", "ENTERPRISE"))).andReturn(), 422, "VALIDATION_FAILED", "Request validation failed"); }
    @Test void doesNotRequireOrUseIdempotencyKey() throws Exception { for (boolean replace : List.of(false, true)) for (String key : List.of("", "arbitrary", " ")) { assertThat(mvc.perform(request(replace).header("Idempotency-Key", key).queryParam("ignored", "true")).andReturn().getResponse().getStatus()).isEqualTo(replace ? 200 : 201); } }
    @Test void strictBodyParsingRejectsUnknownMetadataAndNestedDuplicates() throws Exception { for (String field : List.of("productId", "versionNo", "benefitLines", "channels", "saleStatus", "status", "publishedAt", "createdAt", "versionToken", "createdByEmployeeId", "snapshot", "snapshotSha256", "revision", "versionId", "catalogVersionId", "other")) for (String value : List.of("null", "[]", "1", "\"DRAFT\"")) problem(mvc.perform(request(true).content("{\"" + field + "\":" + value + "," + REPLACE.substring(1))).andReturn(), 422, "VALIDATION_FAILED", "Request validation failed"); problem(mvc.perform(request(false).content(CREATE.replace("\"benefitLines\":[]", "\"benefitLines\":[{\"entitlementType\":\"AI_SECONDS\",\"quantity\":1,\"quantity\":2,\"validityDays\":1,\"grantIntervalMonths\":0}]"))).andReturn(), 422, "VALIDATION_FAILED", "Request validation failed"); verifyNoInteractions(command); }
    @Test void bodyConsumptionIsBoundedAt8193BytesForBothRoutes() throws Exception { for (boolean replace : List.of(false, true)) { consumed.set(0); streamed = " ".repeat(9000).getBytes(StandardCharsets.UTF_8); problem(mvc.perform(request(replace)).andReturn(), 422, "VALIDATION_FAILED", "Request validation failed"); assertThat(consumed.get()).isEqualTo(8193); consumed.set(0); failAfter = 3; streamed = (replace ? REPLACE : CREATE).getBytes(StandardCharsets.UTF_8); problem(mvc.perform(request(replace)).andReturn(), 422, "VALIDATION_FAILED", "Request validation failed"); failAfter = -1; } }
    @Test void create201AndReplay200HaveExactBytesHeadersAndLength() throws Exception { var first = mvc.perform(request(false)).andReturn(); headers(first); assertThat(first.getResponse().getStatus()).isEqualTo(201); assertThat(first.getResponse().getContentAsByteArray()).hasSize(475); assertThat(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(first.getResponse().getContentAsByteArray()))).isEqualTo("7f759e65fa0db683941cf207f7d3aee6a15a3b9a7a70cb9459bf799f0a3868e9"); when(command.create(any())).thenReturn(new CreateResult(draft, false)); var replay = mvc.perform(request(false)).andReturn(); assertThat(replay.getResponse().getStatus()).isEqualTo(200); assertThat(replay.getResponse().getContentAsByteArray()).isEqualTo(first.getResponse().getContentAsByteArray()); headers(replay); }
    @Test void replaceAndReplay200HaveExactBytesHeadersAndLength() throws Exception { var first = mvc.perform(request(true)).andReturn(); var replay = mvc.perform(request(true)).andReturn(); assertThat(first.getResponse().getStatus()).isEqualTo(200); assertThat(replay.getResponse().getContentAsByteArray()).isEqualTo(first.getResponse().getContentAsByteArray()); assertThat(first.getResponse().getContentAsByteArray()).hasSize(475); headers(first); headers(replay); }
    @Test void conflictAndStale409HaveExactProblemBytesHeadersAndLength() throws Exception { for (Code code : List.of(Code.CATALOG_DRAFT_CONFLICT, Code.CATALOG_DRAFT_STALE_VIEW)) { doThrow(new Failure(code)).when(command).replace(any()); problem(mvc.perform(request(true)).andReturn(), 409, code.name(), code == Code.CATALOG_DRAFT_CONFLICT ? "Catalog draft conflict" : "Catalog draft stale view"); } }
    @Test void authorityAndMissingResourceErrorsUseFrozenPrecedence() throws Exception { for (Code code : List.of(Code.FORBIDDEN, Code.NOT_FOUND, Code.SERVICE_UNAVAILABLE)) { doThrow(new Failure(code)).when(command).create(any()); problem(mvc.perform(request(false)).andReturn(), code == Code.FORBIDDEN ? 403 : code == Code.NOT_FOUND ? 404 : 503, code.name(), code == Code.FORBIDDEN ? "Access denied" : code == Code.NOT_FOUND ? "Not found" : "Service unavailable"); } }
    @Test void missingAmbiguousAndThrowingProvidersAreSanitized503() throws Exception { beans.destroySingleton("command"); problem(mvc.perform(request(false)).andReturn(), 503, "SERVICE_UNAVAILABLE", "Service unavailable"); beans.registerSingleton("command", command); beans.registerSingleton("duplicate", mock(OperationsCatalogDraftCommand.class)); problem(mvc.perform(request(true)).andReturn(), 503, "SERVICE_UNAVAILABLE", "Service unavailable"); beans.destroySingleton("duplicate"); beans.destroySingleton("command"); beans.registerBeanDefinition("command", org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition(OperationsCatalogDraftCommand.class, () -> { throw new IllegalStateException("PRIVATE jdbc: password"); }).getBeanDefinition()); problem(mvc.perform(request(false)).andReturn(), 503, "SERVICE_UNAVAILABLE", "Service unavailable"); }
    @Test void unexpectedFailuresUseSanitizedInternalProblemWithoutInventedLimiter() throws Exception { when(command.replace(any())).thenThrow(new IllegalStateException("PRIVATE jdbc: password")); problem(mvc.perform(request(true)).andReturn(), 500, "INTERNAL", "Internal error"); assertThat(Code.values()).extracting(Enum::name).containsExactly("UNAUTHENTICATED", "FORBIDDEN", "NOT_FOUND", "CATALOG_DRAFT_CONFLICT", "CATALOG_DRAFT_STALE_VIEW", "VALIDATION_FAILED", "SERVICE_UNAVAILABLE", "INTERNAL"); }
    @Test void preservesExistingReadSubjectAndPublicationAdmissionBehavior() throws Exception { for (String path : List.of("/api/v1/operations/catalog/products", "/api/v1/operations/subjects", "/api/v1/operations/activations", "/api/v1/operations/ivr-sessions")) problem(mvc.perform(get(path).servletPath(path)).andReturn(), 401, "UNAUTHENTICATED", "Authentication required"); String path = BASE + "/" + VERSION + "/publications"; problem(mvc.perform(post(path).servletPath(path)).andReturn(), 401, "UNAUTHENTICATED", "Authentication required"); }
}
