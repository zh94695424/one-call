package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.onecall.archtests.operations.OperationsOrderReadTestProviders.*;
import com.onecall.archtests.operations.OperationsOrderReadTestProviders;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.*;
import tools.jackson.databind.json.JsonMapper;

class OperationsOrderReadWebTests {
    @Test void exactFrozenSummaryAndDetailKeysPassThroughRealFilterChain() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        for (String role : List.of("ADMIN", "OPS")) {
            when(graph.employee.get("role", String.class)).thenReturn(role);
            var page = graph.mvc.perform(get(BASE).servletPath(BASE).header("Authorization", "Bearer valid"))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse();
            var mapper = JsonMapper.builder().build();
            var json = mapper.readTree(page.getContentAsString());
            assertThat(json.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
            assertThat(json.get("items").get(0).propertyNames()).containsExactlyInAnyOrder("orderId", "orderNo", "subjectId", "channel",
                    "catalogVersionId", "catalogOfferId", "amountCents", "currency", "paymentStatus", "issuanceStatus", "createdAt");
            String path = BASE + "/" + ORDER;
            var detail = graph.mvc.perform(get(path).servletPath(path).header("Authorization", "Bearer valid"))
                    .andExpect(status().isOk()).andReturn().getResponse();
            assertThat(mapper.readTree(detail.getContentAsString()).propertyNames()).containsExactlyInAnyOrder("orderId", "orderNo", "subjectId", "channel",
                    "catalogVersionId", "catalogOfferId", "amountCents", "currency", "paymentStatus", "issuanceStatus", "createdAt",
                    "snapshot", "snapshotSha256", "purchaseConsentId", "paidAt", "closedAt");
        }
    }
    @Test void rejectedRoutesAndMethodsNeverAuthenticateOrResolveOrderPorts() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        for (String path : List.of(BASE + "/", BASE + "//x", BASE + "/x/y", BASE + "/x;y", BASE + "/x%25", BASE + "/x/"))
            for (String method : List.of("GET", "POST", "HEAD", "OPTIONS", "TRACE", "PUT", "DELETE", "PATCH"))
                graph.mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path).with(request -> { request.setServletPath(path); return request; }))
                        .andExpect(status().isNotFound());
        for (String method : List.of("POST", "HEAD", "OPTIONS", "TRACE", "PUT", "DELETE", "PATCH"))
            for (String path : List.of(BASE, BASE + "/" + ORDER))
                graph.mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path).servletPath(path))
                        .andExpect(status().isNotFound());
        verifyNoInteractions(graph.orders, graph.issuances, graph.dsl);
    }
    @Test void authenticationAuthorityAndValidationPrecedenceHaveExactMappings() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        for (String token : List.of("wrong", "unknown"))
            graph.mvc.perform(get(BASE).servletPath(BASE).header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
        graph.mvc.perform(get(BASE).servletPath(BASE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(graph.orders, graph.issuances);
        when(graph.employee.get("status", String.class)).thenReturn("DISABLED");
        graph.mvc.perform(get(BASE).servletPath(BASE).param("unknown", "!").header("Authorization", "Bearer valid")).andExpect(status().isForbidden());
        when(graph.employee.get("role", String.class)).thenReturn("INVALID");
        graph.mvc.perform(get(BASE).servletPath(BASE).header("Authorization", "Bearer valid")).andExpect(status().isServiceUnavailable());
        when(graph.employee.get("status", String.class)).thenReturn("ACTIVE");
        when(graph.employee.get("role", String.class)).thenReturn("ADMIN");
        graph.mvc.perform(get(BASE).servletPath(BASE).param("paymentStatus", "INVALID").param("cursor", "!").header("Authorization", "Bearer valid"))
                .andExpect(status().isUnprocessableContent());
        graph.mvc.perform(get(BASE).servletPath(BASE).param("cursor", "!").header("Authorization", "Bearer valid")).andExpect(status().isBadRequest());
        String path = BASE + "/not-a-uuid";
        graph.mvc.perform(get(path).servletPath(path).header("Authorization", "Bearer valid")).andExpect(status().isUnprocessableContent());
    }
    @Test void customerPassThroughPreservesIdentityAndSentinelsAndNeverVerifiesCustomer() throws Exception {
        var beans = new DefaultListableBeanFactory();
        var verifier = mock(CustomerSessionVerifier.class);
        beans.registerSingleton("customer", verifier);
        var filter = new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter());
        for (String context : List.of("", "/test")) for (String path : List.of(BASE, BASE + "/literal-not-uuid")) {
            var request = new MockHttpServletRequest("GET", context + path);
            request.setContextPath(context); request.setServletPath(path);
            var principal = new OperationsRequestPrincipal(new com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE));
            request.setUserPrincipal(principal); request.setAttribute("sentinel", "unchanged");
            var response = new MockHttpServletResponse(); response.setStatus(202); response.setHeader("X-Sentinel", "same");
            response.getWriter().write("sentinel");
            var calls = new AtomicInteger();
            filter.doFilter(request, response, (incoming, outgoing) -> {
                assertThat(incoming).isSameAs(request); assertThat(outgoing).isSameAs(response);
                assertThat(((jakarta.servlet.http.HttpServletRequest) incoming).getUserPrincipal()).isSameAs(principal);
                calls.incrementAndGet();
            });
            assertThat(calls.get()).isEqualTo(1); assertThat(response.getStatus()).isEqualTo(202);
            assertThat(response.getContentAsString()).isEqualTo("sentinel");
            assertThat(response.getHeaderNames()).containsExactly("X-Sentinel");
            assertThat(Collections.list(request.getAttributeNames())).containsExactly("sentinel");
        }
        verifyNoInteractions(verifier);
    }
    @Test void idempotencyHeadersAreIrrelevantAndDependencyResponsesAreSanitized() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        graph.mvc.perform(get(BASE).servletPath(BASE).header("Authorization", "Bearer valid").header("Idempotency-Key", "one", "two"))
                .andExpect(status().isOk());
        when(graph.orders.fetchOrders(any(), any(), anyInt())).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE SQLSTATE"));
        var response = graph.mvc.perform(get(BASE).servletPath(BASE).header("Authorization", "Bearer valid"))
                .andExpect(status().isServiceUnavailable()).andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff")).andReturn().getResponse();
        assertThat(response.getContentAsString()).doesNotContain("PRIVATE", "SQLSTATE", EMPLOYEE.toString()).contains("SERVICE_UNAVAILABLE");
        doThrow(new Failure(Code.RATE_LIMITED)).when(graph.orders).fetchOrders(any(), any(), anyInt());
        graph.mvc.perform(get(BASE).servletPath(BASE).header("Authorization", "Bearer valid")).andExpect(status().isTooManyRequests());
    }
    @Test void errorDispatchFamilyConcealsRejectedAliasesButNeverCanonicalGetFailures() throws Exception {
        var beans = new DefaultListableBeanFactory();
        var verifier = mock(EmployeeSessionVerifier.class);
        beans.registerSingleton("verifier", verifier);
        var problems = new OperationsProblemWriter();
        var filter = new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(beans.getBeanProvider(EmployeeSessionVerifier.class)), problems);
        var mapper = JsonMapper.builder().build();
        for (String context : List.of("", "/mounted")) {
            for (String uri : List.of(BASE, BASE + "/" + ORDER, BASE + "/", BASE + "//x", BASE + "/x;y", BASE + "/%2fx", BASE + "/a/b")) {
                var request = errorRequest(context, "TRACE", uri, 405);
                var response = new MockHttpServletResponse();
                var chained = new AtomicInteger();
                filter.doFilter(request, response, (incoming, outgoing) -> chained.incrementAndGet());
                assertThat(response.getStatus()).as("concealed " + uri).isEqualTo(404);
                assertThat(chained.get()).isZero();
                assertThat(response.getContentType()).isEqualTo("application/problem+json");
                assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
                var json = mapper.readTree(response.getContentAsString());
                assertThat(json.get("code").asString()).isEqualTo("NOT_FOUND");
                assertThat(json.get("status").asInt()).isEqualTo(404);
                assertThat(json.get("type").asString()).isEqualTo("about:blank");
            }
            var alias = errorRequest(context, "GET", BASE + "/%zz", 400);
            var aliasResponse = new MockHttpServletResponse();
            var aliasChained = new AtomicInteger();
            filter.doFilter(alias, aliasResponse, (incoming, outgoing) -> aliasChained.incrementAndGet());
            assertThat(aliasResponse.getStatus()).isEqualTo(404);
            assertThat(aliasChained.get()).isZero();
            for (String uri : List.of(BASE, BASE + "/" + ORDER)) {
                for (Integer status : List.of(500, 503)) {
                    var canonical = errorRequest(context, "GET", uri, status);
                    var canonicalResponse = new MockHttpServletResponse();
                    var canonicalChained = new AtomicInteger();
                    filter.doFilter(canonical, canonicalResponse, (incoming, outgoing) -> canonicalChained.incrementAndGet());
                    assertThat(canonicalChained.get()).as("canonical GET failure chained " + uri).isEqualTo(1);
                    assertThat(canonicalResponse.getStatus()).isNotEqualTo(404);
                }
                var missing = errorRequest(context, null, uri, 500);
                var missingResponse = new MockHttpServletResponse();
                var missingChained = new AtomicInteger();
                filter.doFilter(missing, missingResponse, (incoming, outgoing) -> missingChained.incrementAndGet());
                assertThat(missingChained.get()).as("missing ERROR_METHOD type-safe").isEqualTo(1);
                var nonString = errorRequest(context, 42, uri, 500);
                var nonStringResponse = new MockHttpServletResponse();
                var nonStringChained = new AtomicInteger();
                filter.doFilter(nonString, nonStringResponse, (incoming, outgoing) -> nonStringChained.incrementAndGet());
                assertThat(nonStringChained.get()).as("non-String ERROR_METHOD type-safe").isEqualTo(1);
            }
            var activations = errorRequest(context, "TRACE", "/api/v1/operations/activations", 405);
            var activationsResponse = new MockHttpServletResponse();
            filter.doFilter(activations, activationsResponse, (incoming, outgoing) -> { });
            assertThat(activationsResponse.getStatus()).isEqualTo(404);
            for (String uri : List.of("/api/v1/operations/subjects", "/api/v1/operations/ordersfoo", "/api/v1/customer/orders")) {
                var sibling = errorRequest(context, "TRACE", uri, 405);
                var siblingResponse = new MockHttpServletResponse();
                var siblingChained = new AtomicInteger();
                filter.doFilter(sibling, siblingResponse, (incoming, outgoing) -> siblingChained.incrementAndGet());
                assertThat(siblingChained.get()).as("sibling/unrelated preserved " + uri).isEqualTo(1);
            }
        }
        verifyNoInteractions(verifier);
    }
    private static MockHttpServletRequest errorRequest(String context, Object method, String uri, Integer status) {
        var request = new MockHttpServletRequest(method instanceof String text ? text : "GET", context + uri);
        request.setContextPath(context);
        request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR);
        if (method != null) request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_METHOD, method);
        request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI, context + uri);
        if (status != null) request.setAttribute(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, status);
        return request;
    }
}
