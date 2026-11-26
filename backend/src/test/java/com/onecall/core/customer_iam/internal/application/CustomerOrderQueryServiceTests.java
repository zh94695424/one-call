package com.onecall.core.customer_iam.internal.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import com.onecall.core.catalog_order.customerorder.CustomerOrderSnapshotReader;
import com.onecall.core.catalog_order.customerquery.CustomerOrderReader;
import com.onecall.core.catalog_order.customerquery.CustomerOrderReader.*;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerOrderQueryServiceTests {
    final TransactionTemplate transaction = mock(TransactionTemplate.class);
    final DSLContext context = mock(DSLContext.class);
    final JooqCustomerBusinessNumberAccessRepository lifecycle = mock(JooqCustomerBusinessNumberAccessRepository.class);
    final CustomerOrderReader reader = mock(CustomerOrderReader.class);
    final CustomerOrderSnapshotReader snapshots = mock(CustomerOrderSnapshotReader.class);
    final UUID account = UUID.randomUUID(), identity = UUID.randomUUID(), version = UUID.randomUUID(), offer = UUID.randomUUID();
    final Instant createdAt = Instant.parse("2026-11-25T00:00:00Z");
    final TrustedCustomerCaller caller = new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB,
            new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null));
    final Scope scope = new Scope(account, null);
    final CustomerOrderQueryService service = new CustomerOrderQueryService(transaction, context, lifecycle, reader, snapshots);
    final CustomerOrderQueryService.Request request = new CustomerOrderQueryService.Request(Optional.empty(), Optional.empty());
    CustomerOrderQueryServiceTests() {
        when(transaction.execute(any())).thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(new SimpleTransactionStatus()));
        Record role = mock(Record.class); when(role.get(0, String.class)).thenReturn("customer_iam_role");
        when(context.fetchOne("select current_user")).thenReturn(role);
        when(reader.fetchOrders(any(), any(), anyInt())).thenReturn(List.of());
        when(reader.fetchOrder(any(), any())).thenReturn(Optional.empty());
        when(reader.fetchIssuances(any())).thenReturn(List.of());
        when(snapshots.read(any(), any())).thenReturn(new Snapshot(1, version, offer, "WEB", "token", "SYNTHETIC", "ONE_TIME",
                "PERSONAL", 123, "CNY", "ONCE", 1, List.of(new Benefit("AI_SECONDS", 1, 1, 0)), "a".repeat(64)));
    }
    SummaryRow row(UUID orderId, Instant time) { return new SummaryRow(orderId, "order", UUID.randomUUID(), "WEB", version, offer, 123L, "CNY", "UNKNOWN", time); }
    void detail() { when(reader.fetchOrder(scope, identity)).thenReturn(Optional.of(new DetailRow(row(identity, createdAt), "stored", "a".repeat(64), UUID.randomUUID(), null, null))); }
    static void code(Runnable action, CustomerContextException.Code code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomerContextException.class, failure -> assertThat(failure.code()).isEqualTo(code));
    }
    @Test void activeContextIsRequiredBeforeBothReads() {
        service.listCustomerOrders(caller, request); service.getCustomerOrder(caller, identity);
        var ordered = inOrder(context, lifecycle, reader);
        ordered.verify(context).execute("SET LOCAL ROLE customer_iam_role"); ordered.verify(context).fetchOne("select current_user");
        ordered.verify(lifecycle).requireActiveContext(any()); ordered.verify(reader).fetchOrders(scope, Optional.empty(), 21);
        ordered.verify(context).execute("SET LOCAL ROLE customer_iam_role"); ordered.verify(context).fetchOne("select current_user");
        ordered.verify(lifecycle).requireActiveContext(any()); ordered.verify(reader).fetchOrder(scope, identity);
        doThrow(new CustomerContextException(CustomerContextException.Code.FORBIDDEN)).when(lifecycle).requireActiveContext(any());
        code(() -> service.listCustomerOrders(caller, request), CustomerContextException.Code.FORBIDDEN);
        code(() -> service.getCustomerOrder(caller, identity), CustomerContextException.Code.FORBIDDEN);
        code(() -> service.listCustomerOrders(new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB), request), CustomerContextException.Code.FORBIDDEN);
    }
    @Test void emptyPageSkipsIssuanceQuery() {
        var page = service.listCustomerOrders(caller, request);
        assertThat(page.items()).isEmpty(); assertThat(page.nextAnchor()).isEmpty(); assertThat(page.hasMore()).isFalse();
        verify(reader, never()).fetchIssuances(any());
    }
    @Test void sentinelIsNotProjectedOrBatched() {
        UUID sentinel = UUID.randomUUID();
        when(reader.fetchOrders(scope, Optional.empty(), 2)).thenReturn(List.of(row(identity, createdAt),
                new SummaryRow(sentinel, null, null, null, null, null, null, null, null, createdAt.minusSeconds(1))));
        var page = service.listCustomerOrders(caller, new CustomerOrderQueryService.Request(Optional.empty(), Optional.of(1)));
        assertThat(page.items()).hasSize(1); assertThat(page.hasMore()).isTrue();
        assertThat(page.nextAnchor()).contains(new Anchor(createdAt, identity));
        verify(reader).fetchIssuances(List.of(identity));
    }
    @Test void issuanceMapsEveryHomogeneousAndMixedSet() {
        when(reader.fetchOrders(scope, Optional.empty(), 21)).thenReturn(List.of(row(identity, createdAt)));
        String[] statuses = {"PENDING", "ISSUED", "FAILED"};
        for (int mask = 0; mask < 8; mask++) {
            var rows = new java.util.ArrayList<IssuanceRow>();
            for (int index = 0; index < 3; index++) if ((mask & (1 << index)) != 0) { rows.add(new IssuanceRow(identity, statuses[index])); rows.add(new IssuanceRow(identity, statuses[index])); }
            when(reader.fetchIssuances(List.of(identity))).thenReturn(rows);
            String expected = mask == 0 ? "NONE" : Integer.bitCount(mask) == 1 ? rows.getFirst().status() : "MIXED";
            assertThat(service.listCustomerOrders(caller, request).items().getFirst().issuanceStatus()).isEqualTo(expected);
        }
    }
    @Test void rawNullAndUnknownStatusesMapList500AndDetail404() {
        detail(); when(reader.fetchOrders(scope, Optional.empty(), 21)).thenReturn(List.of(row(identity, createdAt)));
        for (IssuanceRow corrupt : List.of(new IssuanceRow(identity, null), new IssuanceRow(identity, "UNKNOWN"), new IssuanceRow(UUID.randomUUID(), "ISSUED"))) {
            when(reader.fetchIssuances(List.of(identity))).thenReturn(List.of(corrupt));
            code(() -> service.listCustomerOrders(caller, request), CustomerContextException.Code.INTERNAL);
            assertThat(service.getCustomerOrder(caller, identity)).isEmpty();
        }
    }
    @Test void corruptSnapshotAndRowFactsMapDetail404() {
        detail(); assertThat(service.getCustomerOrder(caller, identity)).isPresent();
        List<Benefit> agreed = List.of(new Benefit("AI_SECONDS", 1, 1, 0));
        String agreedHash = "a".repeat(64);
        for (Snapshot mismatch : List.of(
                new Snapshot(1, UUID.randomUUID(), offer, "WEB", "token", "SYNTHETIC", "ONE_TIME", "PERSONAL", 123L, "CNY", "ONCE", 1, agreed, agreedHash),
                new Snapshot(1, version, UUID.randomUUID(), "WEB", "token", "SYNTHETIC", "ONE_TIME", "PERSONAL", 123L, "CNY", "ONCE", 1, agreed, agreedHash),
                new Snapshot(1, version, offer, "MINIAPP", "token", "SYNTHETIC", "ONE_TIME", "PERSONAL", 123L, "CNY", "ONCE", 1, agreed, agreedHash),
                new Snapshot(1, version, offer, "WEB", "token", "SYNTHETIC", "ONE_TIME", "PERSONAL", 124L, "CNY", "ONCE", 1, agreed, agreedHash),
                new Snapshot(1, version, offer, "WEB", "token", "SYNTHETIC", "ONE_TIME", "PERSONAL", 123L, "USD", "ONCE", 1, agreed, agreedHash),
                new Snapshot(1, version, offer, "WEB", "token", "SYNTHETIC", "ONE_TIME", "PERSONAL", 123L, "CNY", "ONCE", 1, agreed, "b".repeat(64)))) {
            clearInvocations(snapshots);
            when(snapshots.read(any(), any())).thenReturn(mismatch);
            assertThat(service.getCustomerOrder(caller, identity)).as("mismatched snapshot %s", mismatch).isEmpty();
            verify(snapshots).read("stored", agreedHash);
        }
        clearInvocations(snapshots);
        when(snapshots.read(any(), any())).thenReturn(new Snapshot(1, version, offer, "WEB", "token", "SYNTHETIC",
                "ONE_TIME", "PERSONAL", 123L, "CNY", "ONCE", 1, agreed, agreedHash));
        assertThat(service.getCustomerOrder(caller, identity)).as("fully agreeing row and snapshot").isPresent();
        verify(snapshots).read("stored", agreedHash);
        when(snapshots.read(any(), any())).thenThrow(new CustomerOrderSnapshotReader.InvalidSnapshotException());
        assertThat(service.getCustomerOrder(caller, identity)).isEmpty();
        doThrow(new IllegalStateException("private runtime canary")).when(snapshots).read(any(), any());
        code(() -> service.getCustomerOrder(caller, identity), CustomerContextException.Code.INTERNAL);
        reset(snapshots); detail();
        when(reader.fetchOrder(scope, identity)).thenReturn(Optional.of(new DetailRow(row(identity, createdAt), "stored", "bad", null, null, null)));
        assertThat(service.getCustomerOrder(caller, identity)).isEmpty();
    }
    @Test void dependencyRoleAndCompletionFailuresMap503() {
        when(context.fetchOne("select current_user")).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE"));
        code(() -> service.listCustomerOrders(caller, request), CustomerContextException.Code.SERVICE_UNAVAILABLE);
        code(() -> service.getCustomerOrder(caller, identity), CustomerContextException.Code.SERVICE_UNAVAILABLE);
        doThrow(new org.springframework.transaction.TransactionSystemException("PRIVATE")).when(transaction).execute(any());
        code(() -> service.listCustomerOrders(caller, request), CustomerContextException.Code.SERVICE_UNAVAILABLE);
        code(() -> service.getCustomerOrder(caller, identity), CustomerContextException.Code.SERVICE_UNAVAILABLE);
    }
    @Test void transactionUsesOneRepeatableReadSnapshot() {
        detail(); service.getCustomerOrder(caller, identity);
        verify(transaction, times(1)).execute(any()); verify(reader).fetchOrder(scope, identity);
        verify(reader).fetchIssuances(List.of(identity)); verify(snapshots).read("stored", "a".repeat(64));
        verifyNoMoreInteractions(reader);
    }
}
