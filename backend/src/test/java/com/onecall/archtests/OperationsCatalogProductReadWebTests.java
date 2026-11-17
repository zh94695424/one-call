package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders.*;
import com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService.RawDetailRequest;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;

class OperationsCatalogProductReadWebTests {
    private org.springframework.mock.web.MockHttpServletResponse fetch(OperationsCatalogProductReadTestProviders fixture, String path) throws Exception {
        return fixture.mvc.perform(get(path).with(request -> { request.setServletPath(path.split("\\?")[0]); return request; }).header("Authorization", "Bearer valid")).andReturn().getResponse();
    }
    private void problem(org.springframework.mock.web.MockHttpServletResponse response, int status, String code) throws Exception {
        assertThat(response.getStatus()).isEqualTo(status); var body = JsonMapper.builder().build().readTree(response.getContentAsString());
        assertThat(body.get("code").asString()).isEqualTo(code); assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentAsString()).doesNotContain("PRIVATE", "jdbc:", "catalog_order_role", "SELECT ", EMPLOYEE.toString());
    }
    @Test void admitsExactListGet() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); var response = fetch(fixture, BASE); assertThat(response.getStatus()).isEqualTo(200); assertThat(response.getHeader("Content-Length")).isEqualTo(Integer.toString(response.getContentAsByteArray().length)); verify(fixture.reader).roots(Optional.empty(), 21); }
    @Test void admitsExactDetailGet() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); var response = fetch(fixture, BASE + "/" + PRODUCT); assertThat(response.getStatus()).isEqualTo(200); assertThat(response.getHeader("Content-Length")).isEqualTo(Integer.toString(response.getContentAsByteArray().length)); verify(fixture.reader).product(PRODUCT); }
    @Test void concealsOtherMethodsAndUris() throws Exception {
        var fixture = new OperationsCatalogProductReadTestProviders();
        for (String method : List.of("POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")) problem(fixture.mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), BASE).servletPath(BASE).header("Authorization", "Bearer valid")).andReturn().getResponse(), 404, "NOT_FOUND");
        for (String path : List.of(BASE + "/", BASE + "/" + PRODUCT + "/extra", BASE + ";x", BASE + "/%61")) problem(fetch(fixture, path), 404, "NOT_FOUND");
        verifyNoInteractions(fixture.reader);
    }
    @Test void rejectsInvalidAccept() throws Exception {
        var fixture = new OperationsCatalogProductReadTestProviders();
        for (String accept : List.of("text/plain", "application/json;q=0", "application/json;charset=ISO-8859-1", "application/json;unknown=x")) problem(fixture.mvc.perform(get(BASE).servletPath(BASE).header("Authorization", "Bearer valid").header("Accept", accept)).andReturn().getResponse(), 422, "VALIDATION_FAILED");
        verifyNoInteractions(fixture.reader);
    }
    @Test void serializesDeclaredNullPublishedAt() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); var body = JsonMapper.builder().build().readTree(fetch(fixture, BASE + "/" + PRODUCT).getContentAsString()); assertThat(body.get("versions").get(0).has("publishedAt")).isTrue(); assertThat(body.get("versions").get(0).get("publishedAt").isNull()).isTrue(); }
    @Test void serializesRequiredNullPriceCents() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); var quote = with(with(with(with(version(), "offerType", "QUOTE"), "audience", "ENTERPRISE"), "priceCents", null), "billingCycle", "QUOTE"); when(fixture.reader.versions(PRODUCT)).thenReturn(List.of(quote)); var body = JsonMapper.builder().build().readTree(fetch(fixture, BASE + "/" + PRODUCT).getContentAsString()); assertThat(body.get("versions").get(0).has("priceCents")).isTrue(); assertThat(body.get("versions").get(0).get("priceCents").isNull()).isTrue(); }
    @Test void serializesTailNullNextCursor() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); var body = JsonMapper.builder().build().readTree(fetch(fixture, BASE).getContentAsString()); assertThat(body.has("nextCursor")).isTrue(); assertThat(body.get("nextCursor").isNull()).isTrue(); assertThat(body.get("hasMore").asBoolean()).isFalse(); }
    @Test void rejectsDetailQueryParameter() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); problem(fetch(fixture, BASE + "/" + PRODUCT + "?limit=1"), 422, "VALIDATION_FAILED"); verifyNoInteractions(fixture.reader); }
    @Test void mapsFrozenErrors() throws Exception {
        var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.reader.product(PRODUCT)).thenReturn(null); problem(fetch(fixture, BASE + "/" + PRODUCT), 404, "NOT_FOUND");
        problem(fetch(fixture, BASE + "/bad"), 422, "VALIDATION_FAILED"); problem(fetch(fixture, BASE + "?cursor="), 400, "CURSOR_INVALID");
        problem(fixture.mvc.perform(get(BASE).servletPath(BASE)).andReturn().getResponse(), 401, "UNAUTHENTICATED");
        when(fixture.employee.get("status", String.class)).thenReturn("DISABLED"); problem(fetch(fixture, BASE + "?limit=bad"), 403, "FORBIDDEN");
        when(fixture.employee.get("status", String.class)).thenReturn("ACTIVE");
        for (var entry : Map.of(Code.RATE_LIMITED, 429, Code.SERVICE_UNAVAILABLE, 503, Code.INTERNAL, 500, Code.CURSOR_FILTER_MISMATCH, 400).entrySet()) {
            doThrow(new Failure(entry.getKey())).when(fixture.reader).roots(any(), anyInt()); problem(fetch(fixture, BASE), entry.getValue(), entry.getKey().name());
        }
    }
    @Test void doesNotExposeUnapprovedFields() throws Exception {
        var fixture = new OperationsCatalogProductReadTestProviders(); var mapper = JsonMapper.builder().build(); var page = mapper.readTree(fetch(fixture, BASE).getContentAsString());
        assertThat(page.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
        assertThat(page.get("items").get(0).propertyNames()).containsExactlyInAnyOrder("productId", "productCode", "productFamily", "versions");
        assertThat(page.get("items").get(0).get("versions").get(0).propertyNames()).containsExactlyInAnyOrder("catalogVersionId", "versionNo", "saleStatus", "offerType", "audience");
        var detail = mapper.readTree(fetch(fixture, BASE + "/" + PRODUCT).getContentAsString());
        assertThat(detail.get("versions").get(0).propertyNames()).containsExactlyInAnyOrder("catalogVersionId", "versionNo", "saleStatus", "offerType", "audience", "priceCents", "currency", "billingCycle", "installments", "benefitLines", "publishedAt", "revision");
    }
}
