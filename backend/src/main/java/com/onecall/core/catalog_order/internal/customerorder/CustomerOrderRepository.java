package com.onecall.core.catalog_order.internal.customerorder;

import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService.*;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface CustomerOrderRepository {
    TransactionWitness begin();
    void restore(TransactionWitness witness);
    void lockSubject(UUID subjectId);
    void authorize(Command command, Caller caller);
    Catalog catalog(UUID offerId);
    void lockPublication(UUID productId);
    List<BenefitRow> benefits(UUID versionId);
    StoredOrder existing(Caller caller, String key);
    Idempotency beginIdempotency(String key, String fingerprint);
    boolean unknown(UUID subjectId, UUID offerId);
    void verifyConsent(ConsentDescriptor consent);
    StoredOrder insert(Command command, Caller caller, Catalog catalog, ConsentDescriptor consent, String snapshot, String fingerprint);
    void complete(Idempotency record, Order order, String snapshotHash);
    void outbox(StoredOrder order, Caller caller);
    void audit(StoredOrder order, Caller caller, UUID traceId);
    record Catalog(UUID offerId, UUID versionId, UUID productId, String productCode, int versionNo,
            String channel, String offerStatus, String versionStatus, String versionToken, String offerType,
            String audience, Long priceCents, String currency, String billingCycle, int installments,
            int grantIntervalMonths, String snapshot, String snapshotSha256) {}
    record BenefitRow(UUID id, String entitlementType, long quantity, int validityDays, int grantIntervalMonths) {}
    record StoredOrder(Order order, UUID accountId, UUID organizationId, String fingerprint, String fullSnapshot) {}
    record Idempotency(UUID id, int revision, String outcome, String resultType, UUID resultId, String result, String hash) {}
}
