package com.onecall.core.operations_iam.internal.orderread;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.archtests.operations.OperationsOrderReadTestProviders.*;

import com.onecall.archtests.operations.OperationsOrderReadTestProviders;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

class OperationsOrderReadServiceTests {
    private void fails(Runnable action, Code code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(code));
    }
    private DefaultOperationsOrderReadService timed(OperationsOrderReadTestProviders graph, LongSupplier clock) {
        return new DefaultOperationsOrderReadService(graph.transaction, graph.dsl, graph.scope,
                graph.beans.getBeanProvider(OperationsSubjectReadProperties.class),
                graph.beans.getBeanProvider(OrderReader.class), graph.beans.getBeanProvider(IssuanceReader.class), clock);
    }
    @Test void authorityPrecedesParsingPropertiesAndOwners() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        when(graph.employee.get("status", String.class)).thenReturn("DISABLED");
        fails(() -> graph.service.list(graph.caller, raw(Map.of("invalid", List.of("!")))), Code.FORBIDDEN);
        verifyNoInteractions(graph.orders, graph.issuances);
        when(graph.employee.get("role", String.class)).thenReturn("SUPPORT");
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
        when(graph.employee.get("role", String.class)).thenReturn(null);
        fails(() -> graph.service.detail(graph.caller, detail()), Code.SERVICE_UNAVAILABLE);
        when(graph.dsl.fetchOne(anyString(), any(Object[].class))).thenReturn(null);
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.UNAUTHENTICATED);
    }
    @Test void activeAdminAndOpsUseCurrentRowAndExactProjection() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        for (String role : List.of("ADMIN", "OPS")) {
            when(graph.employee.get("role", String.class)).thenReturn(role);
            var page = graph.service.list(graph.caller, raw(Map.of()));
            assertThat(page.items()).hasSize(1);
            assertThat(page.items().getFirst().paymentStatus()).isEqualTo("UNKNOWN");
            assertThat(page.items().getFirst().issuanceStatus()).isEqualTo("NONE");
            assertThat(graph.service.detail(graph.caller, detail()).snapshot()).isEqualTo(facts().snapshot());
        }
    }
    @Test void emptyPageAndMissingDetailNeverResolveIssuance() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        graph.beans.destroySingleton("issuances");
        when(graph.orders.fetchOrders(any(), any(), anyInt())).thenReturn(List.of());
        var page = graph.service.list(graph.caller, raw(Map.of()));
        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.hasMore()).isFalse();
        when(graph.orders.fetchOrder(ORDER)).thenReturn(Optional.empty());
        fails(() -> graph.service.detail(graph.caller, detail()), Code.NOT_FOUND);
        verifyNoInteractions(graph.issuances);
        graph.beans.destroySingleton("properties");
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
        when(graph.orders.fetchOrder(ORDER)).thenReturn(Optional.of(facts()));
        fails(() -> graph.service.detail(graph.caller, detail()), Code.SERVICE_UNAVAILABLE);
    }
    @Test void allStatusSetsAreIndependentOfPaymentAndDuplicateRowsAreLegitimate() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        var statuses = List.of("PENDING", "ISSUED", "FAILED");
        for (int mask = 0; mask < 8; mask++) {
            var rows = new ArrayList<IssuanceFacts>();
            var distinct = new HashSet<String>();
            for (int index = 0; index < 3; index++) if ((mask & (1 << index)) != 0) {
                distinct.add(statuses.get(index));
                rows.add(new IssuanceFacts(ORDER, statuses.get(index)));
                rows.add(new IssuanceFacts(ORDER, statuses.get(index)));
            }
            when(graph.issuances.fetchIssuances(List.of(ORDER))).thenReturn(rows);
            String expected = distinct.isEmpty() ? "NONE" : distinct.size() == 1 ? distinct.iterator().next() : "MIXED";
            assertThat(graph.service.list(graph.caller, raw(Map.of())).items().getFirst().issuanceStatus()).isEqualTo(expected);
            assertThat(graph.service.detail(graph.caller, detail()).issuanceStatus()).isEqualTo(expected);
        }
    }
    @Test void malformedFactsSeparateListInternalDetailNotFoundAndDependencyUnavailable() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        when(graph.issuances.fetchIssuances(anyList())).thenReturn(List.of(new IssuanceFacts(ORDER, "UNKNOWN")));
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.INTERNAL);
        fails(() -> graph.service.detail(graph.caller, detail()), Code.NOT_FOUND);
        when(graph.issuances.fetchIssuances(anyList())).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE"));
        fails(() -> graph.service.detail(graph.caller, detail()), Code.SERVICE_UNAVAILABLE);
        when(graph.orders.fetchOrders(any(), any(), anyInt())).thenReturn(List.of(order(), order()));
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.INTERNAL);
        when(graph.orders.fetchOrder(ORDER)).thenReturn(Optional.of(new DetailFacts(order(), "a".repeat(64), facts().snapshot(), null, null, null)));
        fails(() -> graph.service.detail(graph.caller, detail()), Code.NOT_FOUND);
    }
    @Test void sentinelOnlyDeterminesOrderingAndHasMore() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        var sentinel = new OrderFacts(new UUID(0, 1), null, SUBJECT, null, null, null, null, null, null, TIME.minusSeconds(1));
        when(graph.orders.fetchOrders(any(), any(), eq(2))).thenReturn(List.of(order(), sentinel));
        var page = graph.service.list(graph.caller, raw(Map.of("limit", List.of("1"))));
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isNotBlank();
        verify(graph.issuances).fetchIssuances(List.of(ORDER));
        when(graph.orders.fetchOrders(any(), any(), eq(2))).thenReturn(List.of(sentinel, order()));
        fails(() -> graph.service.list(graph.caller, raw(Map.of("limit", List.of("1")))), Code.INTERNAL);
    }
    @Test void transactionCompletionAndRoleCleanupOverrideDomainOrSuccess() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        doThrow(new Failure(Code.SERVICE_UNAVAILABLE)).when(graph.scope).exit();
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
        doNothing().when(graph.scope).exit();
        doThrow(new java.sql.SQLException("PRIVATE")).when(graph.connection).commit();
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
    }
    @Test void detailDoesNotResolveCursorPropertiesAndFilterDisagreementIsInternal() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        graph.beans.destroySingleton("properties");
        assertThat(graph.service.detail(graph.caller, detail()).orderId()).isEqualTo(ORDER);
        graph.beans.registerSingleton("properties", properties());
        fails(() -> graph.service.list(graph.caller, raw(Map.of("paymentStatus", List.of("PAID")))), Code.INTERNAL);
    }
    @Test void ownerPortContractRejectionIsServiceUnavailableNotInternal() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        when(graph.orders.fetchOrders(any(), any(), anyInt())).thenThrow(new ContractRejectionException("Invalid order query"));
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
        when(graph.orders.fetchOrder(ORDER)).thenThrow(new ContractRejectionException("Missing order identity"));
        fails(() -> graph.service.detail(graph.caller, detail()), Code.SERVICE_UNAVAILABLE);
        doReturn(List.of(order())).when(graph.orders).fetchOrders(any(), any(), anyInt());
        doReturn(Optional.of(facts())).when(graph.orders).fetchOrder(ORDER);
        when(graph.issuances.fetchIssuances(anyList())).thenThrow(new ContractRejectionException("Invalid order batch"));
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
        fails(() -> graph.service.detail(graph.caller, detail()), Code.SERVICE_UNAVAILABLE);
        doThrow(new IllegalStateException(new ContractRejectionException("Invalid order query"))).when(graph.orders).fetchOrders(any(), any(), anyInt());
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
    }
    @Test void nonPortIllegalArgumentFailuresRemainInternalNotServiceUnavailable() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        doThrow(new IllegalStateException(new IllegalArgumentException("library defect"))).when(graph.orders).fetchOrders(any(), any(), anyInt());
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.INTERNAL);
        doThrow(new IllegalArgumentException("plain IllegalArgumentException")).when(graph.orders).fetchOrders(any(), any(), anyInt());
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.INTERNAL);
        doThrow(new NumberFormatException("IAE subclass")).when(graph.orders).fetchOrders(any(), any(), anyInt());
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.INTERNAL);
        doThrow(new IllegalStateException(new IllegalArgumentException("detail defect"))).when(graph.orders).fetchOrder(ORDER);
        fails(() -> graph.service.detail(graph.caller, detail()), Code.INTERNAL);
        doReturn(List.of(order())).when(graph.orders).fetchOrders(any(), any(), anyInt());
        doReturn(Optional.of(facts())).when(graph.orders).fetchOrder(ORDER);
        doThrow(new IllegalArgumentException("issuance defect")).when(graph.issuances).fetchIssuances(anyList());
        fails(() -> graph.service.list(graph.caller, raw(Map.of())), Code.INTERNAL);
        fails(() -> graph.service.detail(graph.caller, detail()), Code.INTERNAL);
    }
    @Test void budgetExhaustionAtListDetailIssuanceAndCompletionNeverEmitsPartialPageOrContinuation() throws Exception {
        var sentinel = new OrderFacts(new UUID(0, 1), null, SUBJECT, null, null, null, null, null, null, TIME.minusSeconds(1));
        for (String stage : List.of("entry", "authority", "orders", "issuance", "detail", "detailIssuance", "exit", "commit")) {
            var graph = new OperationsOrderReadTestProviders();
            var elapsed = new AtomicLong();
            var service = timed(graph, elapsed::get);
            when(graph.orders.fetchOrders(any(), any(), anyInt())).thenAnswer(invocation -> {
                if ("orders".equals(stage)) elapsed.set(5_000_000_000L);
                return List.of(order(), sentinel);
            });
            when(graph.orders.fetchOrder(ORDER)).thenAnswer(invocation -> {
                if ("detail".equals(stage)) elapsed.set(5_000_000_000L);
                return Optional.of(facts());
            });
            when(graph.issuances.fetchIssuances(anyList())).thenAnswer(invocation -> {
                if (Set.of("issuance", "detailIssuance").contains(stage)) elapsed.set(5_000_000_000L);
                return List.of();
            });
            switch (stage) {
                case "entry" -> doAnswer(invocation -> { elapsed.set(5_000_000_000L); return null; }).when(graph.scope).enter();
                case "authority" -> when(graph.dsl.fetchOne(anyString(), any(Object[].class))).thenAnswer(invocation -> {
                    elapsed.set(5_000_000_000L); return graph.employee;
                });
                case "exit" -> doAnswer(invocation -> { elapsed.set(5_000_000_000L); return null; }).when(graph.scope).exit();
                case "commit" -> doAnswer(invocation -> { elapsed.set(5_000_000_000L); return null; }).when(graph.connection).commit();
                default -> { }
            }
            if (Set.of("detail", "detailIssuance").contains(stage)) fails(() -> service.detail(graph.caller, detail()), Code.SERVICE_UNAVAILABLE);
            else fails(() -> service.list(graph.caller, raw(Map.of("limit", List.of("1")))), Code.SERVICE_UNAVAILABLE);
            verify(graph.scope).exit();
            if (Set.of("exit", "commit").contains(stage)) {
                verify(graph.connection).commit();
                verify(graph.connection, never()).rollback();
            } else {
                verify(graph.connection).rollback();
                verify(graph.connection, never()).commit();
            }
            if (Set.of("entry", "authority").contains(stage)) verifyNoInteractions(graph.orders, graph.issuances);
            if (Set.of("orders", "detail").contains(stage)) verifyNoInteractions(graph.issuances);
        }
    }
    @Test void fiveSecondRequestBudgetIsExactAndStrictAtTheListFetchCheckpoint() throws Exception {
        var withinGraph = new OperationsOrderReadTestProviders();
        var withinElapsed = new AtomicLong();
        var within = timed(withinGraph, withinElapsed::get);
        when(withinGraph.orders.fetchOrders(any(), any(), anyInt())).thenAnswer(invocation -> {
            withinElapsed.set(4_999_999_999L); return List.of(order());
        });
        var page = within.list(withinGraph.caller, raw(Map.of()));
        assertThat(page.items()).hasSize(1);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
        verify(withinGraph.connection).commit();
        verify(withinGraph.connection, never()).rollback();
        var exhaustedGraph = new OperationsOrderReadTestProviders();
        var exhaustedElapsed = new AtomicLong();
        var exhausted = timed(exhaustedGraph, exhaustedElapsed::get);
        when(exhaustedGraph.orders.fetchOrders(any(), any(), anyInt())).thenAnswer(invocation -> {
            exhaustedElapsed.set(5_000_000_000L); return List.of(order());
        });
        fails(() -> exhausted.list(exhaustedGraph.caller, raw(Map.of())), Code.SERVICE_UNAVAILABLE);
        verifyNoInteractions(exhaustedGraph.issuances);
        verify(exhaustedGraph.connection).rollback();
        verify(exhaustedGraph.connection, never()).commit();
    }
}
