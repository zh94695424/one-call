package com.onecall.core.operations_iam.internal.catalogread;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders.*;
import com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class OperationsCatalogProductReadRequestTests {
    private void fails(Code code, Runnable operation) { assertThatThrownBy(operation::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(code)); }
    @Test void rejectsUnknownAndRepeatedParameters() {
        for (var query : List.of(Map.of("unknown", List.of("1")), Map.of("limit", List.of("1", "2")), Map.of("cursor", List.of("a", "b")))) fails(Code.VALIDATION_FAILED, () -> OperationsCatalogProductReadRequest.parse(raw(query)));
    }
    @Test void acceptsDefaultAndBoundaryLimits() {
        assertThat(OperationsCatalogProductReadRequest.parse(raw(Map.of())).limit()).isEqualTo(20);
        for (int limit : List.of(1, 20, 100)) assertThat(OperationsCatalogProductReadRequest.parse(raw(Map.of("limit", List.of("" + limit)))).limit()).isEqualTo(limit);
    }
    @Test void rejectsInvalidLimits() {
        for (String limit : List.of("", "0", "101", "-1", "1.0", " 1", "99999")) fails(Code.VALIDATION_FAILED, () -> OperationsCatalogProductReadRequest.parse(raw(Map.of("limit", List.of(limit)))));
    }
    @Test void rejectsMalformedCanonicalUuid() {
        for (String id : List.of("1-1-1-1-1", "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "bad", PRODUCT + "/")) fails(Code.VALIDATION_FAILED, () -> OperationsCatalogProductReadRequest.detail(new RawDetailRequest(id, Map.of(), List.of())));
        assertThat(OperationsCatalogProductReadRequest.detail(detail())).isEqualTo(PRODUCT);
    }
    @Test void blankCursorIsCursorInvalid() {
        for (String cursor : List.of("", " ", "\t")) fails(Code.CURSOR_INVALID, () -> OperationsCatalogProductReadRequest.parse(raw(Map.of("cursor", List.of(cursor)))));
    }
    @Test void authorityFailurePrecedesMalformedRequest() throws Exception {
        var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.employee.get("status", String.class)).thenReturn("DISABLED");
        fails(Code.FORBIDDEN, () -> fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("bad"))))); verifyNoInteractions(fixture.reader);
    }
    @Test void detailRejectsAnyQueryParameter() {
        for (String key : List.of("limit", "cursor", "unknown")) fails(Code.VALIDATION_FAILED, () -> OperationsCatalogProductReadRequest.detail(new RawDetailRequest(PRODUCT.toString(), Map.of(key, List.of("1")), List.of())));
    }
    @Test void requestDigestIsCanonical() throws Exception {
        var request = OperationsCatalogProductReadRequest.parse(raw(Map.of()));
        var bytes = new ByteArrayOutputStream();
        var values = List.of(OperationsCatalogProductReadRequest.PURPOSE.getBytes(StandardCharsets.UTF_8), OperationsCatalogProductReadRequest.ENDPOINT.getBytes(StandardCharsets.UTF_8), OperationsCatalogProductReadRequest.SORT.getBytes(StandardCharsets.UTF_8), ByteBuffer.allocate(4).putInt(20).array());
        int index = 0;
        for (String name : List.of("purpose", "endpoint", "sort", "limit")) {
            byte[] field = name.getBytes(StandardCharsets.UTF_8), value = values.get(index++);
            bytes.writeBytes(ByteBuffer.allocate(4).putInt(field.length).array()); bytes.writeBytes(field); bytes.write(1);
            bytes.writeBytes(ByteBuffer.allocate(4).putInt(value.length).array()); bytes.writeBytes(value);
        }
        assertThat(request.digest()).isEqualTo(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))
                .isEqualTo(OperationsCatalogProductReadRequest.parse(raw(Map.of("limit", List.of("20"), "cursor", List.of("opaque")))).digest())
                .isNotEqualTo(new OperationsCatalogProductReadRequest(1, null).digest());
    }
}
