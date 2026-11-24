package com.onecall.core.catalog_order.internal.customerorder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService;
import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService.*;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException.Code;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerOrderCreationServiceTests {
    static class Harness {
        final CustomerOrderRepository repository = mock(CustomerOrderRepository.class);
        final PurchaseConsentService consent = mock(PurchaseConsentService.class);
        final TransactionTemplate transaction = mock(TransactionTemplate.class);
        final TransactionStatus status = mock(TransactionStatus.class);
        final TransactionWitness witness = new TransactionWitness("lo", 1, 2);
        final Caller caller = CustomerOrderSemanticsTests.caller();
        final Command command = CustomerOrderSemanticsTests.command("key", null);
        final CustomerOrderRepository.Catalog catalog = CustomerOrderSemanticsTests.catalog(CustomerOrderSemanticsTests.source("{}"));
        final LockedActivation locked = new LockedActivation(command.subjectId(), UUID.randomUUID(), "ACTIVE", 1, witness);
        final ConsentDescriptor accepted = new ConsentDescriptor(UUID.randomUUID(), command.subjectId(), "PURCHASE", "WEB", "PURCHASE", CustomerOrderSemantics.event(caller.accountId(), "key"), "SYNTHETIC", "1", "1", "ACCEPT", "ACCOUNT", CustomerOrderSemantics.event(caller.accountId(), "key"), caller.accountId(), "a".repeat(64), "a".repeat(64), "b".repeat(64), Instant.parse("2026-11-24T00:00:00Z"), true);
        final CustomerOrderRepository.StoredOrder stored;
        final DefaultCustomerOrderCreationService service;
        Harness() {
            doAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(status)).when(transaction).execute(any());
            when(repository.begin()).thenReturn(witness); when(consent.lockServiceActivation(command.subjectId(), witness)).thenReturn(locked);
            when(repository.catalog(command.catalogOfferId())).thenReturn(catalog); when(repository.benefits(catalog.versionId())).thenReturn(CustomerOrderSemanticsTests.benefits());
            when(repository.beginIdempotency(anyString(), anyString())).thenReturn(new CustomerOrderRepository.Idempotency(UUID.randomUUID(), 1, "NEW", null, null, null, null));
            when(consent.appendPurchaseConsent(any(), eq(locked), eq(witness))).thenReturn(accepted);
            String snapshot = CustomerOrderSemantics.snapshot(catalog, CustomerOrderSemanticsTests.benefits()), hash = CustomerOrderSemantics.sha(snapshot);
            UUID id = UUID.randomUUID();
            stored = new CustomerOrderRepository.StoredOrder(new Order(id, CustomerOrderSemantics.orderNo(id), command.subjectId(), "WEB", catalog.versionId(), catalog.offerId(), 123, "CNY", "UNPAID", "NONE", accepted.acceptedAt(), hash, CustomerOrderSemantics.projection(snapshot, hash), accepted.id()), caller.accountId(), null, CustomerOrderSemantics.fingerprint(command, caller, catalog), snapshot);
            when(repository.insert(any(), any(), any(), any(), anyString(), anyString())).thenReturn(stored);
            service = new DefaultCustomerOrderCreationService(transaction, repository, consent);
        }
        Result run() { return service.create(command, caller); }
        void replay() {
            when(repository.existing(caller, "key")).thenReturn(stored);
            when(repository.beginIdempotency(anyString(), anyString())).thenReturn(new CustomerOrderRepository.Idempotency(UUID.randomUUID(), 2, "REPLAY", "ORDER", stored.order().orderId(), CustomerOrderSemantics.canonical(stored.order()), stored.order().snapshotSha256()));
            when(consent.validateReplay(any(), eq(accepted.id()), eq(witness))).thenReturn(accepted);
        }
    }
    static void fails(Harness harness, Code code) { assertThatThrownBy(harness::run).isInstanceOfSatisfying(OrderCreationException.class, failure -> assertThat(failure.code()).isEqualTo(code)); }
    @Test void newPersonalOrderRunsOneOwnerBoundTransaction() {
        var harness = new Harness(); assertThat(harness.run()).isEqualTo(new Result(false, harness.stored.order()));
        verify(harness.transaction, times(1)).execute(any()); verify(harness.repository).restore(harness.witness); verify(harness.status, never()).setRollbackOnly();
    }
    @Test void authorityRealNameAndActivationPrecedeReplay() {
        for (Code code : new Code[]{Code.FORBIDDEN, Code.REAL_NAME_REQUIRED}) {
            var harness = new Harness(); harness.replay(); doThrow(new OrderCreationException(code)).when(harness.repository).authorize(any(), any()); fails(harness, code);
            verify(harness.repository, never()).beginIdempotency(anyString(), anyString()); verify(harness.consent, never()).appendPurchaseConsent(any(), any(), any());
        }
        var harness = new Harness(); when(harness.consent.lockServiceActivation(any(), any())).thenReturn(new LockedActivation(harness.command.subjectId(), null, null, null, harness.witness)); fails(harness, Code.ACTIVATION_NOT_ACTIVE);
    }
    @Test void nonpayableCatalogPrecedesUnknownAndConsent() {
        var harness = new Harness(); when(harness.repository.catalog(any())).thenReturn(null); fails(harness, Code.VALIDATION_FAILED);
        verify(harness.repository, never()).unknown(any(), any()); verify(harness.repository, never()).beginIdempotency(anyString(), anyString());
    }
    @Test void replayValidatesOriginalConsentAndReturnsImmutableResult() {
        var harness = new Harness(); harness.replay(); assertThat(harness.run()).isEqualTo(new Result(true, harness.stored.order()));
        verify(harness.consent).validateReplay(any(), eq(harness.accepted.id()), eq(harness.witness)); verify(harness.consent, never()).appendPurchaseConsent(any(), any(), any()); verify(harness.repository, never()).complete(any(), any(), anyString());
    }
    @Test void changedInputConflictsBeforeNewConsent() {
        var harness = new Harness(); when(harness.repository.beginIdempotency(anyString(), anyString())).thenReturn(new CustomerOrderRepository.Idempotency(null, 0, "CONFLICT", null, null, null, null)); fails(harness, Code.IDEMPOTENCY_CONFLICT);
        verify(harness.consent, never()).appendPurchaseConsent(any(), any(), any()); verify(harness.status).setRollbackOnly();
    }
    @Test void unknownOrderBlocksOnlyNewCheckout() {
        var harness = new Harness(); when(harness.repository.unknown(any(), any())).thenReturn(true); fails(harness, Code.ORDER_UNKNOWN_PAYMENT_PENDING);
        harness.replay(); assertThat(harness.run().replay()).isTrue(); verify(harness.consent, never()).appendPurchaseConsent(any(), any(), any());
    }
    @Test void consentDenialRollsBackNewIdempotency() {
        for (Kind kind : Kind.values()) {
            var harness = new Harness(); when(harness.consent.appendPurchaseConsent(any(), any(), any())).thenThrow(new Failure(kind));
            assertThatThrownBy(harness::run).isInstanceOf(OrderCreationException.class); verify(harness.status).setRollbackOnly(); verify(harness.repository, never()).insert(any(), any(), any(), any(), anyString(), anyString());
        }
    }
    @Test void completeOutboxAndAuditHaveExactOwnerOrder() {
        var harness = new Harness(); harness.run(); var order = inOrder(harness.repository, harness.consent);
        order.verify(harness.repository).begin(); order.verify(harness.repository).lockSubject(harness.command.subjectId()); order.verify(harness.consent).lockServiceActivation(harness.command.subjectId(), harness.witness);
        order.verify(harness.repository).authorize(harness.command, harness.caller); order.verify(harness.repository).lockPublication(harness.catalog.productId());
        order.verify(harness.repository).complete(any(), eq(harness.stored.order()), eq(harness.stored.order().snapshotSha256()));
        order.verify(harness.consent).appendPurchaseConsentFacts(harness.accepted, FactPhase.OUTBOX, harness.command.traceId(), harness.witness); order.verify(harness.repository).outbox(harness.stored, harness.caller);
        order.verify(harness.consent).appendPurchaseConsentFacts(harness.accepted, FactPhase.AUDIT, harness.command.traceId(), harness.witness); order.verify(harness.repository).audit(harness.stored, harness.caller, harness.command.traceId()); order.verify(harness.repository).restore(harness.witness);
    }
    @Test void failureAndRestorationPreservePrimaryException() {
        var harness = new Harness(); var primary = new OrderCreationException(Code.IDEMPOTENCY_CONFLICT);
        doThrow(primary).when(harness.repository).authorize(any(), any()); doThrow(new IllegalStateException("PRIVATE")).when(harness.repository).restore(any());
        assertThatThrownBy(harness::run).isSameAs(primary); assertThat(primary.getSuppressed()).hasSize(1);
        assertThat(JooqCustomerOrderRepository.translate(new org.springframework.transaction.TransactionSystemException("PRIVATE rollback", new java.sql.SQLException("Connection closed")), false).code()).isEqualTo(Code.SERVICE_UNAVAILABLE);
        for (String state : new String[]{"55P03", "57014", "40P01", "42501", "08006"}) assertThat(JooqCustomerOrderRepository.translate(new java.sql.SQLException("PRIVATE", state), false).code()).isEqualTo(Code.SERVICE_UNAVAILABLE);
        for (String state : new String[]{"23505", "23514", "23503", "P0001"}) assertThat(JooqCustomerOrderRepository.translate(new java.sql.SQLException("PRIVATE", state), false).code()).isEqualTo(Code.INTERNAL);
    }
    @Test void noPaymentIssuanceOrRemoteCollaboratorIsInvoked() {
        var harness = new Harness(); harness.run();
        assertThat(java.util.Arrays.stream(DefaultCustomerOrderCreationService.class.getDeclaredFields()).map(field -> field.getType().getName())).containsExactlyInAnyOrder(TransactionTemplate.class.getName(), CustomerOrderRepository.class.getName(), PurchaseConsentService.class.getName());
        assertThat(java.util.Arrays.stream(CustomerOrderRepository.class.getDeclaredMethods()).map(java.lang.reflect.Method::getName)).noneMatch(name -> name.matches("(?i).*(payment|trial|grant|issuance|http).*"));
    }
}
