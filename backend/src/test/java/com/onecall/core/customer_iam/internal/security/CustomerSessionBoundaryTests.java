package com.onecall.core.customer_iam.internal.security;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.customer_iam.internal.web.CustomerProblemWriter;
import com.onecall.core.customer_iam.internal.web.CustomerRequestPrincipal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class CustomerSessionBoundaryTests {
    private static final String AUDIENCE = "onecall:customer-session:v1";
    private final UUID account = UUID.randomUUID();

    private CustomerBearerAuthenticator authenticator(CustomerSessionVerifier... verifiers) {
        var factory = new DefaultListableBeanFactory();
        for (int index = 0; index < verifiers.length; index++) { factory.registerSingleton("verifier" + index, verifiers[index]); }
        return new CustomerBearerAuthenticator(factory.getBeanProvider(CustomerSessionVerifier.class));
    }

    private TrustedCustomerCaller invoke(CustomerBearerAuthenticator authenticator) {
        return authenticator.authenticate(Collections.enumeration(List.of("Bearer OPAQUE.CANARY.bytes")));
    }

    @Test
    void verifierReceivesOnlyOpaqueBearerAndCustomerAudience() {
        AtomicInteger calls = new AtomicInteger();
        var caller = invoke(authenticator((bearer, audience) -> {
            assertThat(bearer.value()).isEqualTo("OPAQUE.CANARY.bytes");
            assertThat(audience).isEqualTo(AUDIENCE);
            calls.incrementAndGet();
            return new VerifiedCustomerSession(account, TrustedCustomerCaller.Channel.MINIAPP, audience);
        }));
        assertThat(caller).isEqualTo(new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.MINIAPP));
        assertThat(calls).hasValue(1);
    }

    @Test
    void invalidRevokedExpiredDisabledAndWrongAudienceSessionsHaveSameDenialShape() throws Exception {
        for (String state : List.of("invalid", "revoked", "expired", "disabled", "employee", "ivr", "public-share")) {
            CustomerSessionVerifier verifier = (bearer, audience) -> {
                if (List.of("employee", "ivr", "public-share").contains(state)) {
                    return new VerifiedCustomerSession(account, TrustedCustomerCaller.Channel.WEB, state);
                }
                throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
            };
            assertThatThrownBy(() -> invoke(authenticator(verifier)))
                    .isInstanceOfSatisfying(CustomerContextException.class, failure -> {
                        assertThat(failure.code()).isEqualTo(CustomerContextException.Code.UNAUTHENTICATED);
                        assertThat(failure.getCause()).isNull();
                    });
            var response = new MockHttpServletResponse();
            new CustomerProblemWriter().write(response, CustomerContextException.Code.UNAUTHENTICATED, UUID.randomUUID());
            var json = JsonMapper.builder().build().readTree(response.getContentAsString());
            assertThat(json.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
            assertThat(json.get("title").asString()).isEqualTo("Authentication required");
            assertThat(response.getStatus()).isEqualTo(401);
        }
    }

    @Test
    void malformedVerifierResultsFailClosed() {
        for (VerifiedCustomerSession result : java.util.Arrays.asList(null,
                new VerifiedCustomerSession(null, TrustedCustomerCaller.Channel.WEB, AUDIENCE),
                new VerifiedCustomerSession(account, null, AUDIENCE),
                new VerifiedCustomerSession(account, TrustedCustomerCaller.Channel.WEB, null))) {
            unavailable(authenticator((bearer, audience) -> result));
        }
    }

    @Test
    void missingAmbiguousAndUnavailableVerifiersFailClosed() {
        unavailable(authenticator());
        AtomicInteger calls = new AtomicInteger();
        CustomerSessionVerifier verifier = (bearer, audience) -> {
            calls.incrementAndGet();
            return new VerifiedCustomerSession(account, TrustedCustomerCaller.Channel.WEB, audience);
        };
        unavailable(authenticator(verifier, verifier));
        assertThat(calls).hasValue(0);
        unavailable(authenticator((bearer, audience) -> {
            throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.SERVICE_UNAVAILABLE);
        }));
        unavailable(authenticator((bearer, audience) -> { throw new IllegalStateException("CANARY"); }));
        unavailable(authenticator((bearer, audience) -> { throw new CustomerContextException(CustomerContextException.Code.FORBIDDEN); }));
    }

    @Test
    void secretWrappersAndPrincipalNeverRetainOrExposeBearer() throws Exception {
        var bearer = new OpaqueCustomerBearer("CANARY");
        assertThat(bearer.toString()).isEqualTo("[REDACTED]");
        assertThat(java.util.Arrays.stream(OpaqueCustomerBearer.class.getDeclaredMethods()).map(java.lang.reflect.Method::getName))
                .doesNotContain("getValue", "getBearer", "getToken");
        var principal = new CustomerRequestPrincipal(invoke(authenticator((secret, audience) ->
                new VerifiedCustomerSession(account, TrustedCustomerCaller.Channel.WEB, audience))));
        assertThat(principal.getName()).isEqualTo(account.toString());
        assertThat(principal.toString()).doesNotContain("CANARY", "OPAQUE");
        assertThat(JsonMapper.builder().build().writeValueAsString(principal)).doesNotContain("CANARY", "OPAQUE");
        assertThat(java.util.Arrays.stream(CustomerRequestPrincipal.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("caller");
        assertThat(JsonMapper.builder().build().writeValueAsString(bearer)).isEqualTo("{}");
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/customer/me");
        request.setServletPath("/api/v1/customer/me");
        request.setUserPrincipal(() -> "INCOMING_CANARY");
        request.addHeader("Authorization", "Bearer OPAQUE.CANARY.bytes");
        var filter = new com.onecall.core.customer_iam.internal.web.CustomerBoundaryFilter(
                authenticator((secret, audience) -> new VerifiedCustomerSession(account, TrustedCustomerCaller.Channel.WEB, audience)),
                new CustomerProblemWriter());
        filter.doFilter(request, new MockHttpServletResponse(), (incoming, outgoing) -> {
            var wrapped = (jakarta.servlet.http.HttpServletRequest) incoming;
            assertThat(wrapped.getUserPrincipal()).isInstanceOf(CustomerRequestPrincipal.class).isNotSameAs(request.getUserPrincipal());
            assertThat(wrapped.getRemoteUser()).isEqualTo(account.toString());
            for (String name : Collections.list(wrapped.getAttributeNames())) {
                assertThat(String.valueOf(wrapped.getAttribute(name))).doesNotContain("CANARY", "Bearer", "OPAQUE");
            }
        });
    }

    private void unavailable(CustomerBearerAuthenticator authenticator) {
        assertThatThrownBy(() -> invoke(authenticator)).isInstanceOfSatisfying(CustomerContextException.class, failure -> {
            assertThat(failure.code()).isEqualTo(CustomerContextException.Code.SERVICE_UNAVAILABLE);
            assertThat(failure.getMessage()).isEqualTo("SERVICE_UNAVAILABLE");
            assertThat(failure.getCause()).isNull();
        });
    }
}
