package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

@SpringBootTest(classes = CustomerCatalogOffersWebTests.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("web")
class CustomerCatalogOffersWebTests {
    private static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID ORGANIZATION = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID VERSION = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID OFFER = UUID.fromString("80000000-0000-0000-0000-000000000001");
    private static final String PATH = "/api/v1/customer/catalog/offers";
    private static volatile boolean missingKey;
    @LocalServerPort private int port;
    @Autowired private CustomerCatalogOfferQueryService service;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({CustomerWebConfiguration.class, CustomerCatalogOffersController.class, CustomerApiErrorController.class, Beans.class})
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
                    case "organization" -> new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, ORGANIZATION);
                    default -> new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null);
                };
                return new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB, token.equals("wrong-audience") ? "foreign" : audience, workspace);
            };
        }
        @Bean
        CustomerCatalogOfferQueryService customerCatalogOfferQueryService() { return mock(CustomerCatalogOfferQueryService.class); }
        @Bean
        @org.springframework.context.annotation.Scope("prototype")
        CustomerCatalogOfferCursorCodec customerCatalogOfferCursorCodec() { return codec(missingKey); }
    }

    private static CustomerCatalogOfferCursorCodec codec(boolean missing) {
        var properties = new CustomerCatalogOfferCursorProperties();
        if (!missing) properties.setCursorHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
        return new CustomerCatalogOfferCursorCodec(properties);
    }
    private static CustomerCatalogOfferCursorCodec.Binding binding(String audience, UUID organization, Optional<String> offerType) {
        return new CustomerCatalogOfferCursorCodec.Binding(ACCOUNT, organization, "WEB", audience, offerType);
    }
    private static String cursor(CustomerCatalogOfferCursorCodec.Binding binding) {
        return codec(false).encode(binding, new CustomerCatalogOfferCursorCodec.Anchor("ALPHA", 1, OFFER));
    }

    @BeforeEach
    void resetState() {
        missingKey = false; reset(service);
        when(service.listCustomerCatalogOffers(any(), any())).thenReturn(new CustomerCatalogOfferQueryService.Page(
                List.of(new CustomerCatalogOfferQueryService.Item("ALPHA", VERSION, OFFER, "WEB", 1, "SUBSCRIPTION", "PERSONAL",
                        1200L, "CNY", "MONTHLY", 1, List.of(new CustomerCatalogOfferQueryService.BenefitLine("EVIDENCE_COUNT", 3, 30, 2)), "stored-token")),
                Optional.empty(), false));
    }

    private HttpResponse<String> request(String method, String path, String token, String... headers) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(java.time.Duration.ofSeconds(20));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        for (int index = 0; index < headers.length; index += 2) builder.header(headers[index], headers[index + 1]);
        try (var client = HttpClient.newHttpClient()) {
            return client.send(builder.method(method, HttpRequest.BodyPublishers.ofString("{\"ownerAccountId\":\"BODY_CANARY\"}")).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
    private HttpResponse<String> get(String query, String token, String... headers) throws Exception { return request("GET", PATH + query, token, headers); }
    private JsonNode check(HttpResponse<String> response, int expected, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(expected);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        var json = JsonMapper.builder().build().readTree(response.body());
        if (code == null) {
            assertThat(response.headers().firstValue("Content-Type")).contains("application/json");
            assertThat(json.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
        } else {
            assertThat(response.headers().firstValue("Content-Type")).contains("application/problem+json");
            assertThat(json.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
            assertThat(json.path("code").asString()).isEqualTo(code);
            assertThat(json.path("status").asInt()).isEqualTo(expected);
        }
        return json;
    }

    @Test
    void successPageHasExactFieldsBytesHeadersAndNullPriceForQuote() throws Exception {
        when(service.listCustomerCatalogOffers(any(), any())).thenReturn(new CustomerCatalogOfferQueryService.Page(
                List.of(new CustomerCatalogOfferQueryService.Item("BETA", VERSION, OFFER, "WEB", 1, "QUOTE", "ENTERPRISE",
                        null, "CNY", "QUOTE", 1, List.of(), "stored-token")), Optional.empty(), false));
        var response = get("", "organization");
        var json = check(response, 200, null);
        assertThat(response.headers().firstValue("Content-Length")).isPresent();
        var item = json.path("items").get(0);
        assertThat(item.propertyNames()).containsExactly("productCode", "catalogVersionId", "catalogOfferId", "channel",
                "versionNo", "offerType", "audience", "priceCents", "currency", "billingCycle", "installments", "benefitLines", "versionToken");
        assertThat(item.path("priceCents").isNull()).isTrue();
        assertThat(item.path("currency").asString()).isEqualTo("CNY");
        assertThat(item.path("versionToken").asString()).isEqualTo("stored-token");
        assertThat(item.path("benefitLines").isArray()).isTrue();
        assertThat(json.path("nextCursor").isNull()).isTrue();
        assertThat(response.body()).doesNotContain("BODY_CANARY", "ownerAccountId", "snapshot", "revision");
    }

    @Test
    void tailHasExplicitNullNextCursorAndHasMoreFalse() throws Exception {
        var json = check(get("", "personal"), 200, null);
        assertThat(json.path("nextCursor").isNull()).isTrue();
        assertThat(json.path("hasMore").asBoolean()).isFalse();
        assertThat(json.path("items").get(0).path("benefitLines").get(0).propertyNames())
                .containsExactly("entitlementType", "quantity", "validityDays", "grantIntervalMonths");
    }

    @Test
    void unauthenticatedAndVerifierOutageMap401And503() throws Exception {
        check(get("", null), 401, "UNAUTHENTICATED");
        check(get("", "bad"), 401, "UNAUTHENTICATED");
        check(get("", "wrong-audience"), 401, "UNAUTHENTICATED");
        check(get("", "outage"), 503, "SERVICE_UNAVAILABLE");
        check(get("", "unselected"), 503, "SERVICE_UNAVAILABLE");
        verifyNoInteractions(service);
    }

    @Test
    void unsupportedMethodsAliasesTrailingAndEncodedVariantsAre404BeforeAuth() throws Exception {
        for (String method : List.of("POST", "PUT", "DELETE", "PATCH", "OPTIONS")) check(request(method, PATH, null), 404, "NOT_FOUND");
        assertThat(request("HEAD", PATH, null).statusCode()).isEqualTo(404);
        assertThat(request("TRACE", PATH, null).statusCode()).isEqualTo(500);
        for (String path : List.of(PATH + "/", PATH + "/extra", PATH + ";x", "/api/v1/customer/catalog//offers", "/api/v1/customer/catalog/%61ffers", PATH + "%2F"))
            check(request("GET", path, null), 404, "NOT_FOUND");
        verifyNoInteractions(service);
    }

    @Test
    void malformedOrRepeatedLimitIs422() throws Exception {
        for (String limit : List.of("0", "101", "-1", "bad", "1.0", "1&limit=2", "999999999999999999999"))
            check(get("?limit=" + limit, "personal"), 422, "VALIDATION_FAILED");
        verifyNoInteractions(service);
    }

    @Test
    void malformedOrRepeatedEnumFiltersAre422() throws Exception {
        for (String query : List.of("?channel=BOGUS", "?channel=WEB&channel=WEB", "?audience=BOGUS", "?audience=PERSONAL&audience=PERSONAL",
                "?offerType=BOGUS", "?offerType=QUOTE&offerType=QUOTE", "?channel=", "?offerType="))
            check(get(query, "personal"), 422, "VALIDATION_FAILED");
        verifyNoInteractions(service);
    }

    @Test
    void conflictingChannelOrAudienceIs403Forbidden() throws Exception {
        check(get("?channel=MINIAPP", "personal"), 403, "FORBIDDEN");
        check(get("?audience=ENTERPRISE", "personal"), 403, "FORBIDDEN");
        check(get("?audience=PERSONAL", "organization"), 403, "FORBIDDEN");
        check(get("?channel=MINIAPP&cursor=bad", "personal"), 403, "FORBIDDEN");
        verifyNoInteractions(service);
    }

    @Test
    void offerTypeNarrowingAndEmptyIntersectionAre200() throws Exception {
        check(get("?offerType=SUBSCRIPTION", "personal"), 200, null);
        verify(service).listCustomerCatalogOffers(any(), argThat(request -> request.offerType().equals(Optional.of("SUBSCRIPTION"))));
        clearInvocations(service);
        when(service.listCustomerCatalogOffers(any(), any())).thenReturn(new CustomerCatalogOfferQueryService.Page(List.of(), Optional.empty(), false));
        var json = check(get("?offerType=QUOTE", "personal"), 200, null);
        assertThat(json.path("items")).isEmpty();
        assertThat(json.path("nextCursor").isNull()).isTrue();
        check(get("?channel=WEB&audience=PERSONAL", "personal"), 200, null);
    }

    @Test
    void malformedTamperedAndCrossScopeCursorsAre400CursorInvalid() throws Exception {
        String valid = cursor(binding("PERSONAL", null, Optional.empty()));
        byte[] bytes = Base64.getUrlDecoder().decode(valid); bytes[bytes.length - 1] ^= 1;
        for (String value : List.of("", "bad", valid + "&cursor=" + valid, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)))
            check(get("?cursor=" + value, "personal"), 400, "CURSOR_INVALID");
        check(get("?cursor=" + cursor(binding("PERSONAL", ORGANIZATION, Optional.empty())), "personal"), 400, "CURSOR_INVALID");
        check(get("?cursor=" + cursor(new CustomerCatalogOfferCursorCodec.Binding(UUID.randomUUID(), null, "WEB", "PERSONAL", Optional.empty())), "personal"), 400, "CURSOR_INVALID");
        verifyNoInteractions(service);
    }

    @Test
    void normalizedFilterChangeIs400CursorFilterMismatch() throws Exception {
        String value = cursor(binding("PERSONAL", null, Optional.empty()));
        check(get("?cursor=" + value + "&offerType=SUBSCRIPTION", "personal"), 400, "CURSOR_FILTER_MISMATCH");
        String narrowed = cursor(binding("PERSONAL", null, Optional.of("SUBSCRIPTION")));
        check(get("?cursor=" + narrowed, "personal"), 400, "CURSOR_FILTER_MISMATCH");
        verifyNoInteractions(service);
    }

    @Test
    void missingCursorKeyIs503WithoutEagerPool() throws Exception {
        missingKey = true;
        check(get("", "personal"), 503, "SERVICE_UNAVAILABLE");
        check(get("?cursor=" + cursor(binding("PERSONAL", null, Optional.empty())), "personal"), 503, "SERVICE_UNAVAILABLE");
        verifyNoInteractions(service);
    }

    @Test
    void errorPrecedenceIsDeterministic() throws Exception {
        when(service.listCustomerCatalogOffers(any(), any())).thenThrow(new CustomerContextException(CustomerContextException.Code.FORBIDDEN));
        missingKey = true;
        check(get("?limit=bad&cursor=bad", "bad"), 401, "UNAUTHENTICATED");
        check(get("?limit=bad&cursor=bad", "unselected"), 503, "SERVICE_UNAVAILABLE");
        check(get("?limit=bad&cursor=bad", "personal"), 422, "VALIDATION_FAILED");
        check(get("?cursor=bad", "personal"), 400, "CURSOR_INVALID");
        check(get("?channel=MINIAPP&cursor=bad", "personal"), 403, "FORBIDDEN");
        check(get("", "personal"), 503, "SERVICE_UNAVAILABLE");
        check(get("?cursor=" + cursor(binding("PERSONAL", null, Optional.empty())), "personal"), 503, "SERVICE_UNAVAILABLE");
        check(get("?limit=bad", "personal", "Accept", "text/html"), 500, "INTERNAL");
        verifyNoInteractions(service);
        missingKey = false;
        check(get("?cursor=" + cursor(binding("PERSONAL", ORGANIZATION, Optional.empty())), "personal"), 400, "CURSOR_INVALID");
        check(get("", "personal"), 403, "FORBIDDEN");
    }

    @Test
    void problemEnvelopesCarryTraceIdsAndNoSecrets() throws Exception {
        var first = check(get("?cursor=SECRET_CANARY", "personal", "X-Trace-Id", "TRACE_CANARY"), 400, "CURSOR_INVALID");
        var denied = get("", null);
        var second = check(denied, 401, "UNAUTHENTICATED");
        assertThat(first.path("traceId").asString()).isNotEqualTo(second.path("traceId").asString());
        assertThat(UUID.fromString(first.path("traceId").asString())).isNotNull();
        assertThat(denied.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        var response = get("?cursor=SECRET_CANARY", "personal", "X-Trace-Id", "TRACE_CANARY");
        assertThat(response.body()).doesNotContain("SECRET_CANARY", "TRACE_CANARY", "jdbc:", "password", "Exception");
    }

    @Test
    void acceptAndContentTypeNegotiationStayJson() throws Exception {
        check(get("", "personal", "Accept", "application/json"), 200, null);
        check(get("", "personal"), 200, null);
        for (String accept : List.of("text/html", "application/json;q=0", "not a media type"))
            check(get("", "personal", "Accept", accept), 500, "INTERNAL");
        check(get("", null, "Accept", "text/html"), 401, "UNAUTHENTICATED");
        for (String path : List.of("/api/v1/customer/me", "/api/v1/customer/business-numbers", "/api/v1/customer/shares")) {
            check(request("GET", path, null), 401, "UNAUTHENTICATED");
        }
    }
}
