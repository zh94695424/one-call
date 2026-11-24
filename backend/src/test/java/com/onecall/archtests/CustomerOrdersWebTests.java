package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException.Code;
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
import tools.jackson.databind.json.JsonMapper;

class CustomerOrdersWebTests {
    static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001"), SUBJECT = UUID.randomUUID(), OFFER = UUID.randomUUID(), VERSION = UUID.randomUUID();
    static final String ROUTE = "/api/v1/customer/orders", BODY = "{\"subjectId\":\"" + SUBJECT + "\",\"catalogOfferId\":\"" + OFFER + "\",\"channel\":\"WEB\",\"decision\":\"ACCEPT\",\"policyCode\":\"SYNTHETIC\",\"policyVersion\":\"1\",\"promptVersion\":\"1\",\"contentSha256\":\"" + "a".repeat(64) + "\"}";
    static final Order ORDER = new Order(UUID.randomUUID(), "OC-synthetic", SUBJECT, "WEB", VERSION, OFFER, 123, "CNY", "UNPAID", "NONE", Instant.parse("2026-11-24T00:00:00.123456Z"), "b".repeat(64),
            new Snapshot(1, VERSION, OFFER, "WEB", "exact", "SYNTHETIC", "ONE_TIME", "PERSONAL", 123, "CNY", "ONCE", 1, List.of(new Benefit("AI_SECONDS", 100, 30, 0)), "b".repeat(64)), UUID.randomUUID());
    static class Fixture {
        final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        final List<Command> commands = new ArrayList<>(); final List<Caller> callers = new ArrayList<>();
        VerifiedCustomerSession session = new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1", new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null));
        Code failure; RuntimeException unexpected; boolean replay;
        final MockMvc mvc;
        Fixture() {
            beans.registerSingleton("verifier", (CustomerSessionVerifier) (bearer, audience) -> {
                if (!"valid".equals(bearer.value())) throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                return session;
            });
            beans.registerSingleton("owner", (CustomerOrderCreationService) (command, caller) -> {
                commands.add(command); callers.add(caller); if (failure != null) throw new OrderCreationException(failure);
                if (unexpected != null) throw unexpected; return new Result(replay, ORDER);
            });
            beans.registerSingleton("adapter", new CustomerOrderCommandService(beans.getBeanProvider(CustomerOrderCreationService.class)));
            var writer = new CustomerProblemWriter();
            mvc = MockMvcBuilders.standaloneSetup(new CustomerOrdersController(beans.getBeanProvider(CustomerOrderCommandService.class), writer))
                    .addFilters(new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), writer)).build();
        }
        MockHttpServletResponse send(MockHttpServletRequestBuilder request) throws Exception { return mvc.perform(request).andReturn().getResponse(); }
        MockHttpServletResponse send() throws Exception { return send(request()); }
    }
    static MockHttpServletRequestBuilder request() { return post(ROUTE).servletPath(ROUTE).header("Authorization", "Bearer valid").header("Idempotency-Key", "key").contentType("application/json").content(BODY); }
    static void problem(MockHttpServletResponse response, int status, String code) throws Exception {
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(status);
        var node = JsonMapper.builder().build().readTree(response.getContentAsString());
        assertThat(node.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId"); assertThat(node.get("code").asString()).isEqualTo(code);
        assertThat(UUID.fromString(node.get("traceId").asString())).isNotNull(); assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache"); assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getContentAsString()).doesNotContain("PRIVATE", "SQLException", "secret");
    }
    @Test void personalOrderReturns201AndFrozenProjection() throws Exception {
        var fixture = new Fixture(); var response = fixture.send(); assertThat(response.getStatus()).isEqualTo(201);
        var node = JsonMapper.builder().build().readTree(response.getContentAsString());
        assertThat(node.propertyNames()).containsExactlyInAnyOrder("orderId", "orderNo", "subjectId", "channel", "catalogVersionId", "catalogOfferId", "amountCents", "currency", "paymentStatus", "issuanceStatus", "createdAt", "snapshotSha256", "snapshot", "purchaseConsentId");
        assertThat(node.get("snapshot").propertyNames()).doesNotContain("terms", "schedule", "lineKey", "versionNo");
        assertThat(node.get("snapshot").get("benefitLines").get(0).propertyNames()).containsExactlyInAnyOrder("entitlementType", "quantity", "validityDays", "grantIntervalMonths");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store"); assertThat(response.getHeader("Location")).isNull();
    }
    @Test void identicalReplayReturns200AndSameBody() throws Exception {
        var fixture = new Fixture(); String first = fixture.send().getContentAsString(); fixture.replay = true;
        var replay = fixture.send(); assertThat(replay.getStatus()).isEqualTo(200); assertThat(replay.getContentAsString()).isEqualTo(first);
    }
    @Test void authenticationPrecedesBodyAndHeaderValidation() throws Exception {
        var fixture = new Fixture(); problem(fixture.send(post(ROUTE).servletPath(ROUTE).content("bad")), 401, "UNAUTHENTICATED");
        problem(fixture.send(request().header("Authorization", "Bearer other").content("bad")), 401, "UNAUTHENTICATED"); assertThat(fixture.commands).isEmpty();
    }
    @Test void trustedChannelWorkspaceAndAccountCannotBeOverridden() throws Exception {
        var fixture = new Fixture(); assertThat(fixture.send(request().header("X-Role", "ADMIN").param("accountId", "forged")).getStatus()).isEqualTo(201);
        assertThat(fixture.callers.getFirst()).isEqualTo(new Caller(ACCOUNT, "WEB", "PERSONAL", null));
        problem(fixture.send(request().content(BODY.replace("WEB", "MINIAPP"))), 422, "VALIDATION_FAILED");
        problem(fixture.send(request().content(BODY.replace("{", "{\"accountId\":\"forged\","))), 422, "VALIDATION_FAILED");
    }
    @Test void strictBodyRejectsDuplicatesUnknownFieldsAndCoercion() throws Exception {
        var fixture = new Fixture();
        for (String body : List.of("null", "[]", "{}", BODY + "{}", BODY.replace("{", "{\"decision\":\"ACCEPT\","), BODY.replace("{", "{\"amountCents\":0,"), BODY.replace("\"" + SUBJECT + "\"", "123"), BODY + " ".repeat(8193))) problem(fixture.send(request().content(body)), 422, "VALIDATION_FAILED");
        problem(fixture.send(request().content(new byte[]{(byte) 0xc3, 0x28})), 422, "VALIDATION_FAILED"); assertThat(fixture.commands).isEmpty();
    }
    @Test void acceptOnlyAndPolicyFieldBoundsAreExact() throws Exception {
        var binder = new CustomerOrderJsonBinder();
        for (String body : List.of(BODY.replace("ACCEPT", "DECLINE"), BODY.replace("SYNTHETIC", "x".repeat(65)), BODY.replace("SYNTHETIC", " "), BODY.replace("a".repeat(64), "A".repeat(64)), BODY.replace("}", ",\"expectedVersionToken\":null}"))) assertThatThrownBy(() -> binder.bind(body.getBytes(StandardCharsets.UTF_8))).isInstanceOf(IllegalArgumentException.class);
        assertThat(binder.bind(BODY.replace("SYNTHETIC", "x".repeat(64)).getBytes(StandardCharsets.UTF_8)).policyCode()).hasSize(64);
    }
    @Test void idempotencyHeaderIsSingleExactAndBounded475() throws Exception {
        var fixture = new Fixture();
        for (String key : List.of("x", "x".repeat(74), "x".repeat(75), "x".repeat(128), "x".repeat(475), "😀".repeat(475), " left,right ")) {
            var request = request(); request.with(servlet -> { servlet.removeHeader("Idempotency-Key"); servlet.addHeader("Idempotency-Key", key); return servlet; });
            assertThat(fixture.send(request).getStatus()).isEqualTo(201); assertThat(fixture.commands.getLast().clientIdempotencyKey()).isEqualTo(key);
        }
        for (String key : List.of(" ", "x".repeat(476), "x".repeat(512), "\0", "\ud800")) {
            var request = request().with(servlet -> { servlet.removeHeader("Idempotency-Key"); servlet.addHeader("Idempotency-Key", key); return servlet; });
            problem(fixture.send(request), 422, "VALIDATION_FAILED");
        }
        problem(fixture.send(request().header("Idempotency-Key", "second")), 422, "VALIDATION_FAILED");
    }
    @Test void typedProblemsHaveFrozenStatusTitlesAndTrace() throws Exception {
        var fixture = new Fixture();
        for (Code code : Code.values()) {
            fixture.failure = code;
            int status = switch (code) { case UNAUTHENTICATED -> 401; case FORBIDDEN, ACTIVATION_NOT_ACTIVE -> 403; case VALIDATION_FAILED, REAL_NAME_REQUIRED -> 422; case RATE_LIMITED -> 429; case SERVICE_UNAVAILABLE -> 503; case INTERNAL -> 500; default -> 409; };
            var response = fixture.send(); problem(response, status, code.name()); if (status == 401) assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        }
    }
    @Test void ordersRouteDoesNotAdmitReadOrAliasEndpoints() throws Exception {
        var fixture = new Fixture();
        for (String path : List.of(ROUTE + "/", ROUTE + ";x", ROUTE + "/id", "/api/v1/customer/%6frders")) problem(fixture.send(post(path).with(servlet -> { servlet.setServletPath(path); return servlet; })), 404, "NOT_FOUND");
        problem(fixture.send(get(ROUTE).servletPath(ROUTE)), 404, "NOT_FOUND"); assertThat(fixture.commands).isEmpty();
    }
    @Test void unavailableOwnersAndUnexpectedFailuresAreSanitized() throws Exception {
        var transport = new Fixture();
        var response = transport.send(request().with(servlet -> {
            var failing = org.mockito.Mockito.spy(servlet);
            var stream = org.mockito.Mockito.mock(jakarta.servlet.ServletInputStream.class);
            org.mockito.Mockito.doReturn(stream).when(failing).getInputStream();
            try { org.mockito.Mockito.doThrow(new java.io.IOException("PRIVATE secret")).when(stream).readNBytes(8193); }
            catch (java.io.IOException impossible) { throw new AssertionError(impossible); }
            return failing;
        }));
        problem(response, 503, "SERVICE_UNAVAILABLE");
        var node = JsonMapper.builder().build().readTree(response.getContentAsString());
        assertThat(node.get("type").asString()).isEqualTo("about:blank");
        assertThat(node.get("title").asString()).isEqualTo("Service unavailable");
        assertThat(node.get("status").asInt()).isEqualTo(503);
        assertThat(transport.commands).isEmpty();
        var fixture = new Fixture(); fixture.unexpected = new IllegalStateException("PRIVATE secret"); problem(fixture.send(), 500, "INTERNAL"); fixture.unexpected = null;
        fixture.beans.destroySingleton("owner"); problem(fixture.send(), 503, "SERVICE_UNAVAILABLE");
        fixture.session = new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1", null); problem(fixture.send(request().content("bad")), 503, "SERVICE_UNAVAILABLE");
    }
}
