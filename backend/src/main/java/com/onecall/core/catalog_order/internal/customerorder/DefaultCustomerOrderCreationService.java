package com.onecall.core.catalog_order.internal.customerorder;

import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService;
import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService.*;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException.Code;
import java.util.Objects;
import org.springframework.transaction.support.TransactionTemplate;

final class DefaultCustomerOrderCreationService implements CustomerOrderCreationService {
    private final TransactionTemplate transaction;
    private final CustomerOrderRepository repository;
    private final PurchaseConsentService consent;
    DefaultCustomerOrderCreationService(TransactionTemplate transaction, CustomerOrderRepository repository, PurchaseConsentService consent) {
        this.transaction = transaction; this.repository = repository; this.consent = consent;
    }
    @Override public Result create(Command command, Caller caller) {
        CustomerOrderSemantics.arguments(command, caller);
        boolean[] callbackReturned = {false};
        try {
            return transaction.execute(status -> {
                TransactionWitness witness = repository.begin();
                RuntimeException primary = null;
                try {
                    Result result = checkout(command, caller, witness);
                    callbackReturned[0] = true;
                    return result;
                }
                catch (RuntimeException failure) { primary = failure; status.setRollbackOnly(); throw failure; }
                finally {
                    try { repository.restore(witness); }
                    catch (RuntimeException cleanup) {
                        var sanitized = new OrderCreationException(Code.SERVICE_UNAVAILABLE);
                        if (primary != null) primary.addSuppressed(sanitized); else { status.setRollbackOnly(); throw sanitized; }
                    }
                }
            });
        } catch (OrderCreationException failure) { throw failure; }
        catch (PurchaseConsentService.Failure failure) {
            throw new OrderCreationException(switch (failure.kind()) {
                case REQUIRED -> Code.PURCHASE_CONSENT_REQUIRED;
                case STALE -> Code.PURCHASE_CONSENT_STALE;
                case DECLINED -> Code.PURCHASE_CONSENT_DECLINED;
                case CONFLICT -> Code.IDEMPOTENCY_CONFLICT;
                case UNAVAILABLE -> Code.SERVICE_UNAVAILABLE;
                case INTEGRITY -> Code.INTERNAL;
            });
        } catch (RuntimeException failure) {
            if (callbackReturned[0]) throw new OrderCreationException(Code.SERVICE_UNAVAILABLE);
            throw JooqCustomerOrderRepository.translate(failure, false);
        }
    }
    private Result checkout(Command command, Caller caller, TransactionWitness witness) {
        CustomerOrderSemantics.key(command.clientIdempotencyKey());
        repository.lockSubject(command.subjectId());
        LockedActivation activation = consent.lockServiceActivation(command.subjectId(), witness);
        repository.authorize(command, caller);
        if (!"ACTIVE".equals(activation.status()) || !command.subjectId().equals(activation.subjectId()) || !witness.equals(activation.witness())) throw new OrderCreationException(Code.ACTIVATION_NOT_ACTIVE);
        var preliminary = repository.catalog(command.catalogOfferId());
        if (preliminary == null) throw CustomerOrderSemantics.invalid();
        repository.lockPublication(preliminary.productId());
        var catalog = repository.catalog(command.catalogOfferId());
        if (catalog == null || !catalog.productId().equals(preliminary.productId())) throw CustomerOrderSemantics.integrity();
        CustomerOrderSemantics.payable(catalog, command, caller);
        var existing = repository.existing(caller, command.clientIdempotencyKey());
        String fingerprint = CustomerOrderSemantics.fingerprint(command, caller, catalog);
        var idempotency = repository.beginIdempotency(CustomerOrderSemantics.platformKey(caller.accountId(), command.clientIdempotencyKey()), fingerprint);
        if ("CONFLICT".equals(idempotency.outcome())) throw new OrderCreationException(Code.IDEMPOTENCY_CONFLICT);
        PurchaseConsentInput input = new PurchaseConsentInput(command.subjectId(), caller.accountId(), caller.channel(), CustomerOrderSemantics.event(caller.accountId(), command.clientIdempotencyKey()),
                command.decision(), command.policyCode(), command.policyVersion(), command.promptVersion(), command.contentSha256());
        if ("REPLAY".equals(idempotency.outcome())) {
            if (existing == null || !"ORDER".equals(idempotency.resultType()) || !existing.order().orderId().equals(idempotency.resultId())
                    || !existing.order().snapshotSha256().equals(idempotency.hash()) || !fingerprint.equals(existing.fingerprint())
                    || !Objects.equals(caller.organizationId(), existing.organizationId())
                    || !CustomerOrderSemantics.canonical(CustomerOrderSemantics.parse(idempotency.result())).equals(CustomerOrderSemantics.canonical(existing.order()))) throw CustomerOrderSemantics.integrity();
            consent.validateReplay(input, existing.order().purchaseConsentId(), witness);
            return new Result(true, existing.order());
        }
        if (!"NEW".equals(idempotency.outcome()) || existing != null) throw CustomerOrderSemantics.integrity();
        if (repository.unknown(command.subjectId(), catalog.offerId())) throw new OrderCreationException(Code.ORDER_UNKNOWN_PAYMENT_PENDING);
        String snapshot = CustomerOrderSemantics.snapshot(catalog, repository.benefits(catalog.versionId()));
        var accepted = consent.appendPurchaseConsent(input, activation, witness);
        repository.verifyConsent(accepted);
        var order = repository.insert(command, caller, catalog, accepted, snapshot, fingerprint);
        repository.complete(idempotency, order.order(), order.order().snapshotSha256());
        consent.appendPurchaseConsentFacts(accepted, FactPhase.OUTBOX, command.traceId(), witness);
        repository.outbox(order, caller);
        consent.appendPurchaseConsentFacts(accepted, FactPhase.AUDIT, command.traceId(), witness);
        repository.audit(order, caller, command.traceId());
        return new Result(false, order.order());
    }
}
