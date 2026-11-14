package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static com.onecall.archtests.operations.OperationsSubjectReadTestProviders.*;

import com.onecall.archtests.operations.OperationsSubjectReadTestProviders;
import com.onecall.core.operations_iam.internal.security.EmployeeSessionVerifier;
import com.onecall.core.operations_iam.internal.security.VerifiedEmployeeSession;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class OperationsSubjectReadWebTests {
    private static final List<String> PATHS = List.of(BASE, BASE + "/" + SUBJECT);

    private static MockHttpServletRequestBuilder request(String path) {
        return get(path).with(request -> {
            request.setServletPath(path);
            request.setRequestURI(path);
            return request;
        }).header("Authorization", "Bearer valid");
    }

    private static MockHttpServletResponse send(OperationsSubjectReadTestProviders fixture, MockHttpServletRequestBuilder request) throws Exception {
        return fixture.mvc.perform(request).andReturn().getResponse();
    }

    private static JsonNode json(MockHttpServletResponse response) { return JsonMapper.builder().build().readTree(response.getContentAsByteArray()); }

    private static void problem(MockHttpServletResponse response, int status, String code) {
        assertThat(response.getStatus()).as(response.getContentAsByteArray().length == 0 ? "empty" : json(response).toString()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(json(response).path("code").asString()).isEqualTo(code);
        assertThat(json(response).path("status").asInt()).isEqualTo(status);
        UUID.fromString(json(response).path("traceId").asString());
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    }

    @Test
    void exactListRouteReturnsOnlyFrozenPageAndSummaryProperties() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        var response = send(fixture, request(BASE));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).startsWith("application/json");
        var body = json(response);
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
        assertThat(body.path("items").get(0).propertyNames()).containsExactlyInAnyOrder(
                "subjectId", "source", "activationStatus", "realNameStatus", "riskState", "createdAt");
        assertThat(body.path("items").get(0).path("subjectId").asString()).isEqualTo(SUBJECT.toString());
    }

    @Test
    void exactDetailRouteReturnsOnlyFrozenSafeProjection() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        var response = send(fixture, request(PATHS.getLast()));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("subjectId", "source", "realNameStatus", "riskState",
                "bindings", "claims", "evidenceCount", "orderCount", "createdAt");
    }

    @Test
    void hiddenMethodsAndNoncanonicalOperationsRoutesRemainNotFound() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        for (String path : List.of(BASE + "/", BASE + "//", BASE + "/employees/hidden", BASE + "/a;b", BASE + "/%61",
                "/api/v1/operations/employees", BASE + "/" + SUBJECT + "/extra")) {
            problem(send(fixture, request(path)), 404, "NOT_FOUND");
        }
        for (String path : PATHS) {
            problem(send(fixture, post(path).servletPath(path)), 404, "NOT_FOUND");
            problem(send(fixture, delete(path).servletPath(path)), 404, "NOT_FOUND");
        }
        verifyNoInteractions(fixture.subjects);
    }

    @Test
    void employeeAuthenticationPrecedesEveryReadInput() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        for (String path : PATHS) {
            problem(send(fixture, get(path).servletPath(path).param("limit", "bad").header("Accept", "text/html")), 401, "UNAUTHENTICATED");
            problem(send(fixture, request(path).header("Authorization", "Bearer other")), 401, "UNAUTHENTICATED");
        }
        verifyNoInteractions(fixture.subjects, fixture.dsl);
    }

    @Test
    void zeroTwoAndUncreatableVerifiersAreSanitizedUnavailable() throws Exception {
        for (int count : List.of(0, 2, 3)) {
            var fixture = new OperationsSubjectReadTestProviders();
            fixture.beans.destroySingleton("verifier");
            if (count == 2) {
                fixture.beans.registerSingleton("one", fixture.sessions);
                fixture.beans.registerSingleton("two", mock(EmployeeSessionVerifier.class));
            }
            if (count == 3) fixture.beans.registerBeanDefinition("broken", new RootBeanDefinition(EmployeeSessionVerifier.class,
                    () -> { throw new IllegalStateException("PRIVATE_VERIFIER"); }));
            for (String path : PATHS) problem(send(fixture, request(path)), 503, "SERVICE_UNAVAILABLE");
            verifyNoInteractions(fixture.dsl, fixture.subjects);
        }
    }

    @Test
    void onlyCurrentActiveAdminAndOpsMayRead() throws Exception {
        for (String path : PATHS) {
            var fixture = new OperationsSubjectReadTestProviders();
            when(fixture.employee.get("status", String.class)).thenReturn("DISABLED");
            problem(send(fixture, request(path).header("Accept", "text/html")), 403, "FORBIDDEN");
            verifyNoInteractions(fixture.subjects);
            when(fixture.employee.get("status", String.class)).thenReturn("ACTIVE");
            for (String role : List.of("ADMIN", "OPS")) {
                when(fixture.employee.get("role", String.class)).thenReturn(role);
                for (String accept : List.of("text/html", "application/json;q=0", "application/json;q=broken", "application/json;q=0, */*;q=1")) {
                    problem(send(fixture, request(path).header("Accept", accept)), 422, "VALIDATION_FAILED");
                }
                verifyNoInteractions(fixture.subjects);
            }
        }
    }

    @Test
    void missingTwoAndUncreatableReadServicesFailClosed() throws Exception {
        for (int count : List.of(0, 2, 3)) {
            var fixture = new OperationsSubjectReadTestProviders();
            fixture.beans.destroySingleton("readService");
            if (count == 2) {
                fixture.beans.registerSingleton("one", fixture.service);
                fixture.beans.registerSingleton("two", mock(OperationsSubjectReadService.class));
            }
            if (count == 3) fixture.beans.registerBeanDefinition("broken", new RootBeanDefinition(OperationsSubjectReadService.class,
                    () -> { throw new IllegalStateException("PRIVATE_SERVICE"); }));
            for (String path : PATHS) problem(send(fixture, request(path)), 503, "SERVICE_UNAVAILABLE");
        }
    }

    @Test
    void queryDuplicatesUnknownKeysAndCanonicalUuidUse422() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        for (String key : List.of("limit", "cursor", "source", "subjectId")) {
            problem(send(fixture, request(BASE).param(key, "a", "a")), 422, "VALIDATION_FAILED");
        }
        problem(send(fixture, request(BASE).param("employeeId", EMPLOYEE.toString())), 422, "VALIDATION_FAILED");
        problem(send(fixture, request(BASE + "/" + SUBJECT.toString().toUpperCase())), 422, "VALIDATION_FAILED");
        verifyNoInteractions(fixture.subjects);
    }

    @Test
    void allEnumsAndLimitDefaultsAndBoundsAreExact() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        for (String key : List.of("source", "claimStatus", "activationStatus", "realNameStatus", "riskState")) {
            for (String value : List.of("", "unknown", " active", "active")) problem(send(fixture, request(BASE).param(key, value)), 422, "VALIDATION_FAILED");
        }
        for (String limit : List.of("0", "101", "+1", "1.0", " 1")) problem(send(fixture, request(BASE).param("limit", limit)), 422, "VALIDATION_FAILED");
        for (String limit : List.of("1", "20", "100")) assertThat(send(fixture, request(BASE).param("limit", limit)).getStatus()).isEqualTo(200);
        assertThat(send(fixture, request(BASE)).getStatus()).isEqualTo(200);
    }

    @Test
    void cursorInvalidAndFilterMismatchUseFrozen400Problems() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        problem(send(fixture, request(BASE).param("cursor", "malformed")), 400, "CURSOR_INVALID");
        fixture.beans.destroySingleton("readService");
        var service = mock(OperationsSubjectReadService.class);
        when(service.list(any(), any())).thenThrow(new Failure(Code.CURSOR_FILTER_MISMATCH));
        fixture.beans.registerSingleton("replacement", service);
        problem(send(fixture, request(BASE).param("cursor", "opaque")), 400, "CURSOR_FILTER_MISMATCH");
    }

    @Test
    void optionalFieldsAreOmittedButTailCursorIsExplicitNull() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        var body = json(send(fixture, request(BASE)));
        assertThat(body.has("nextCursor")).isTrue();
        assertThat(body.path("nextCursor").isNull()).isTrue();
        assertThat(body.path("hasMore").asBoolean()).isFalse();
        assertThat(body.path("items").get(0).has("maskedNumber")).isFalse();
        assertThat(body.path("items").get(0).has("claimStatus")).isFalse();
        var detail = json(send(fixture, request(PATHS.getLast())));
        for (String key : List.of("activation", "trial", "maskedNumber")) assertThat(detail.has(key)).isFalse();
    }

    @Test
    void notFoundFollowsAuthorityAndValidation() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        when(fixture.subjects.detail(SUBJECT, 101)).thenReturn(Optional.empty());
        problem(send(fixture, request(PATHS.getLast())), 404, "NOT_FOUND");
        problem(send(fixture, request(PATHS.getLast()).param("unknown", "x")), 422, "VALIDATION_FAILED");
        when(fixture.employee.get("status", String.class)).thenReturn("DISABLED");
        problem(send(fixture, request(PATHS.getLast()).param("unknown", "x")), 403, "FORBIDDEN");
    }

    @Test
    void getBodiesAndCommandHeadersNeverBecomeReadAuthority() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        for (String path : PATHS) {
            var response = send(fixture, request(path).contentType("text/plain").content("{\"employeeId\":\"PRIVATE\",\"role\":\"ADMIN\"}")
                    .header("Idempotency-Key", "ignored", "ignored-again").header("X-Employee-Id", "PRIVATE"));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString()).doesNotContain("PRIVATE");
        }
    }

    @Test
    void customerAndOperationsFiltersPassOnlyExactAdmittedRoutes() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        for (String path : PATHS) assertThat(send(fixture, request(path)).getStatus()).isEqualTo(200);
        problem(send(fixture, request(BASE + "/" + SUBJECT + "/employees")), 404, "NOT_FOUND");
        problem(send(fixture, request(BASE + "/" + SUBJECT + ";x")), 404, "NOT_FOUND");
    }

    @Test
    void jsonMediaAndProblemHeadersNeverExposeSensitiveCanaries() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        for (String path : PATHS) {
            when(fixture.employee.get("status", String.class)).thenReturn("DISABLED");
            problem(send(fixture, request(path).header("Accept", "text/html")), 403, "FORBIDDEN");
            when(fixture.employee.get("status", String.class)).thenReturn("ACTIVE");
            for (String role : List.of("ADMIN", "OPS")) {
                when(fixture.employee.get("role", String.class)).thenReturn(role);
                for (String accept : List.of("text/html", "application/json;q=0", "application/json;q=broken", "application/json;q=0, */*;q=1")) {
                    problem(send(fixture, request(path).header("Accept", accept)), 422, "VALIDATION_FAILED");
                }
            }
            for (String accept : List.of("application/json", "application/*", "*/*")) {
                var response = send(fixture, request(path).header("Accept", accept));
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(response.getContentType()).startsWith("application/json");
            }
            assertThat(send(fixture, request(path).header("Accept", "application/json", "*/*")).getStatus()).isEqualTo(200);
            var first = send(fixture, request(path).header("Accept", "text/html"));
            var second = send(fixture, request(path).header("Accept", "text/html"));
            problem(first, 422, "VALIDATION_FAILED");
            problem(second, 422, "VALIDATION_FAILED");
            assertThat(json(first).path("traceId").asString()).isNotEqualTo(json(second).path("traceId").asString());
            assertThat(first.getContentAsString()).doesNotContain("Bearer", "SELECT", "password", "<html", EMPLOYEE.toString());
        }
    }

    @Test
    void concurrentReadCallersAndFiltersRemainIsolated() throws Exception {
        var fixture = new OperationsSubjectReadTestProviders();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var good = executor.submit(() -> send(fixture, request(BASE)));
            var bad = executor.submit(() -> send(fixture, get(BASE).servletPath(BASE)));
            assertThat(good.get().getStatus()).isEqualTo(200);
            problem(bad.get(), 401, "UNAUTHENTICATED");
        }
    }
}
