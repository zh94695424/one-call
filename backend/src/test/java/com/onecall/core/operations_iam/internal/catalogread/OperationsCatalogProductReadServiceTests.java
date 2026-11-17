package com.onecall.core.operations_iam.internal.catalogread;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders.*;
import com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class OperationsCatalogProductReadServiceTests {
    private void fails(Code code, Runnable action) { assertThatThrownBy(action::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(code)); }
    @Test void activeAdminReadsList() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); assertThat(fixture.service.list(fixture.caller, raw(Map.of())).items()).hasSize(1); }
    @Test void activeOpsReadsDetail() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.employee.get("role", String.class)).thenReturn("OPS"); fixture.beans.destroySingleton("properties"); assertThat(fixture.service.detail(fixture.caller, detail()).versions()).hasSize(1); }
    @Test void authorityRunsBeforeRequestAndRing() throws Exception {
        var fixture = new OperationsCatalogProductReadTestProviders(); fixture.service.list(fixture.caller, raw(Map.of()));
        var order = inOrder(fixture.scope, fixture.dsl, fixture.reader); order.verify(fixture.scope).enter();
        order.verify(fixture.dsl).fetchOne(eq("SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)"), any(Object[].class));
        order.verify(fixture.reader).roots(Optional.empty(), 21); order.verify(fixture.reader).summaries(List.of(PRODUCT)); order.verify(fixture.scope).exit();
    }
    @Test void disabledWinsOverMalformedFilter() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.employee.get("status", String.class)).thenReturn("DISABLED"); fails(Code.FORBIDDEN, () -> fixture.service.list(fixture.caller, raw(Map.of("bad", List.of("bad"))))); verifyNoInteractions(fixture.reader); }
    @Test void wrongAudienceWinsOverMissingRing() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); fixture.beans.destroySingleton("properties"); fails(Code.UNAUTHENTICATED, () -> fixture.service.list(new Caller(EMPLOYEE, "customer"), raw(Map.of()))); verifyNoInteractions(fixture.dsl, fixture.reader); }
    @Test void malformedAuthorityWinsOverInvalidCursor() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.employee.get("revision", Integer.class)).thenReturn(0); fails(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of("cursor", List.of("!"))))); verifyNoInteractions(fixture.reader); }
    @Test void ringAuthorityPrecedenceMatrix() throws Exception {
        for (String authority : List.of("valid", "disabled", "wrong", "malformed")) for (String ring : List.of("valid", "missing", "ambiguous", "malformed")) {
            var fixture = new OperationsCatalogProductReadTestProviders(); var resolution = new java.util.concurrent.atomic.AtomicInteger();
            fixture.beans.destroySingleton("properties");
            if (!ring.equals("missing")) {
                var bean = new org.springframework.beans.factory.support.RootBeanDefinition(OperationsSubjectReadProperties.class);
                bean.setInstanceSupplier(() -> { resolution.incrementAndGet(); return ring.equals("malformed") ? new OperationsSubjectReadProperties(true, null, null) : properties(); });
                fixture.beans.registerBeanDefinition("ring", bean);
                if (ring.equals("ambiguous")) fixture.beans.registerSingleton("otherRing", properties());
            }
            if (authority.equals("disabled")) when(fixture.employee.get("status", String.class)).thenReturn("DISABLED");
            if (authority.equals("malformed")) when(fixture.employee.get("role", String.class)).thenReturn("OTHER");
            Caller caller = authority.equals("wrong") ? new Caller(EMPLOYEE, "wrong") : fixture.caller;
            if (authority.equals("valid") && ring.equals("valid")) assertThat(fixture.service.list(caller, raw(Map.of())).items()).hasSize(1);
            else fails(authority.equals("disabled") ? Code.FORBIDDEN : authority.equals("wrong") ? Code.UNAUTHENTICATED : Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(caller, raw(Map.of())));
            if (!authority.equals("valid")) { assertThat(resolution).hasValue(0); verifyNoInteractions(fixture.reader); }
        }
    }
    @Test void usesOneRepeatableReadTransaction() throws Exception {
        var fixture = new OperationsCatalogProductReadTestProviders(); doAnswer(invocation -> { assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue(); assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ); return null; }).when(fixture.scope).enter();
        fixture.service.list(fixture.caller, raw(Map.of())); verify(fixture.source, times(1)).getConnection(); verify(fixture.connection).commit();
        long budget = java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        for (boolean list : List.of(true, false)) {
            for (int expiredCheck = 1; expiredCheck <= 9; expiredCheck++) {
                var timed = new OperationsCatalogProductReadTestProviders();
                var clockReads = new java.util.concurrent.atomic.AtomicInteger();
                int expiresAt = expiredCheck;
                var service = new DefaultOperationsCatalogProductReadService(timed.transaction, timed.dsl, timed.scope,
                        timed.beans.getBeanProvider(OperationsSubjectReadProperties.class), timed.beans.getBeanProvider(CatalogProductReader.class),
                        () -> clockReads.getAndIncrement() >= expiresAt ? budget : 0L);
                when(timed.reader.roots(any(), anyInt())).thenReturn(List.of(product(), new ProductFacts(new UUID(2, 2), "AC", "STANDARD")));
                fails(Code.SERVICE_UNAVAILABLE, () -> {
                    if (list) service.list(timed.caller, raw(Map.of("limit", List.of("1"))));
                    else service.detail(timed.caller, detail());
                });
                assertThat(clockReads).hasValue(expiredCheck + 1);
                verify(timed.scope, times(expiredCheck > 1 ? 1 : 0)).enter();
                if (expiredCheck <= 2) verifyNoInteractions(timed.dsl);
                else verify(timed.dsl).fetchOne(anyString(), any(Object[].class));
                if (list) {
                    verify(timed.reader, times(expiredCheck > 5 ? 1 : 0)).roots(Optional.empty(), 2);
                    verify(timed.reader, times(expiredCheck > 7 ? 1 : 0)).summaries(List.of(PRODUCT));
                } else {
                    verify(timed.reader, times(expiredCheck > 4 ? 1 : 0)).product(PRODUCT);
                    verify(timed.reader, times(expiredCheck > 6 ? 1 : 0)).versions(PRODUCT);
                    verify(timed.reader, times(expiredCheck > 8 ? 1 : 0)).benefits(List.of(VERSION));
                }
                verifyNoMoreInteractions(timed.reader);
                verify(timed.scope).exit(); verify(timed.connection).rollback(); verify(timed.connection, never()).commit();
            }
            for (boolean expireAtExit : List.of(true, false)) {
                var completed = new OperationsCatalogProductReadTestProviders();
                var clock = new java.util.concurrent.atomic.AtomicLong();
                var service = new DefaultOperationsCatalogProductReadService(completed.transaction, completed.dsl, completed.scope,
                        completed.beans.getBeanProvider(OperationsSubjectReadProperties.class), completed.beans.getBeanProvider(CatalogProductReader.class), clock::get);
                if (expireAtExit) doAnswer(invocation -> { clock.set(budget); return null; }).when(completed.scope).exit();
                else doAnswer(invocation -> { clock.set(budget); return null; }).when(completed.connection).commit();
                when(completed.reader.roots(any(), anyInt())).thenReturn(List.of(product(), new ProductFacts(new UUID(2, 2), "AC", "STANDARD")));
                if (list) {
                    var page = service.list(completed.caller, raw(Map.of("limit", List.of("1"))));
                    assertThat(page.items()).hasSize(1); assertThat(page.hasMore()).isTrue();
                    assertThat(new OperationsCatalogProductCursorCodec(properties()).decode(page.nextCursor(), new OperationsCatalogProductReadRequest(1, null))).isEqualTo(new Anchor("AB", PRODUCT));
                } else assertThat(service.detail(completed.caller, detail()).versions()).hasSize(1);
                assertThat(clock).hasValue(budget);
                verify(completed.scope).exit(); verify(completed.connection).commit(); verify(completed.connection, never()).rollback();
            }
        }
    }
    @Test void usesTwoPhaseRootAndVersionRead() throws Exception {
        var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.reader.roots(any(), anyInt())).thenReturn(List.of(product(), new ProductFacts(new UUID(2, 2), "AC", "STANDARD")));
        fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("1")))); verify(fixture.reader).summaries(List.of(PRODUCT)); verify(fixture.reader, never()).versions(any());
        String cursor = new OperationsCatalogProductCursorCodec(properties()).encode(new Anchor("AB", PRODUCT), new OperationsCatalogProductReadRequest(1, null));
        clearInvocations(fixture.reader); fails(Code.CURSOR_FILTER_MISMATCH, () -> fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("2"), "cursor", List.of(cursor))))); verifyNoInteractions(fixture.reader);
    }
    @Test void mapsZeroVersionRootToEmptyArray() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.reader.summaries(anyList())).thenReturn(List.of()); assertThat(fixture.service.list(fixture.caller, raw(Map.of())).items().getFirst().versions()).isEmpty(); }
    @Test void mapsDraftPublishedAtToNull() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); assertThat(fixture.service.detail(fixture.caller, detail()).versions().getFirst().publishedAt()).isNull(); }
    @Test void mapsQuotePriceToNull() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); var quote = with(with(with(with(version(), "offerType", "QUOTE"), "audience", "ENTERPRISE"), "priceCents", null), "billingCycle", "QUOTE"); when(fixture.reader.versions(PRODUCT)).thenReturn(List.of(quote)); assertThat(fixture.service.detail(fixture.caller, detail()).versions().getFirst().priceCents()).isNull(); }
    @Test void rejectsNonCnyCurrency() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.reader.versions(PRODUCT)).thenReturn(List.of(with(version(), "currency", "USD"))); fails(Code.SERVICE_UNAVAILABLE, () -> fixture.service.detail(fixture.caller, detail())); }
    @Test void rejectsInconsistentPersistedFacts() throws Exception {
        for (var invalid : List.of(with(version(), "revision", 0), with(version(), "versionNo", 0), with(version(), "saleStatus", "ACTIVE"), with(version(), "priceCents", null), with(version(), "installments", 2), with(version(), "audience", "ENTERPRISE"))) {
            var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.reader.versions(PRODUCT)).thenReturn(List.of(invalid)); fails(Code.SERVICE_UNAVAILABLE, () -> fixture.service.detail(fixture.caller, detail()));
        }
    }
    @Test void mapsTailCursorToNull() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); var page = fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("1")))); assertThat(page.nextCursor()).isNull(); assertThat(page.hasMore()).isFalse(); }
    @Test void mapsLookaheadToHasMore() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.reader.roots(any(), anyInt())).thenReturn(List.of(product(), new ProductFacts(new UUID(2, 2), "AC", "STANDARD"))); var page = fixture.service.list(fixture.caller, raw(Map.of("limit", List.of("1")))); assertThat(page.hasMore()).isTrue(); assertThat(page.nextCursor().length()).isLessThanOrEqualTo(512); assertThat(new OperationsCatalogProductCursorCodec(properties()).decode(page.nextCursor(), new OperationsCatalogProductReadRequest(1, null))).isEqualTo(new Anchor("AB", PRODUCT)); }
    @Test void restoresRoleOnSuccess() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); fixture.service.detail(fixture.caller, detail()); verify(fixture.scope).exit(); verify(fixture.connection).commit(); }
    @Test void restoresRoleOnFailure() throws Exception {
        var fixture = new OperationsCatalogProductReadTestProviders(); when(fixture.reader.roots(any(), anyInt())).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE")); fails(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of()))); verify(fixture.scope).exit(); verify(fixture.connection).rollback();
        doThrow(new Failure(Code.SERVICE_UNAVAILABLE)).when(fixture.scope).exit(); fails(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, raw(Map.of())));
    }
}
