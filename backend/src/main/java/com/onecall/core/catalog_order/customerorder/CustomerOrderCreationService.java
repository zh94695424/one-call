package com.onecall.core.catalog_order.customerorder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CustomerOrderCreationService {
    Result create(Command command, Caller caller);
    record Command(UUID subjectId, UUID catalogOfferId, String channel, String decision,
            String policyCode, String policyVersion, String promptVersion, String contentSha256,
            String expectedVersionToken, String clientIdempotencyKey, UUID traceId) {}
    record Caller(UUID accountId, String channel, String workspaceType, UUID organizationId) {}
    record Result(boolean replay, Order order) {}
    record Order(UUID orderId, String orderNo, UUID subjectId, String channel, UUID catalogVersionId,
            UUID catalogOfferId, long amountCents, String currency, String paymentStatus,
            String issuanceStatus, Instant createdAt, String snapshotSha256, Snapshot snapshot,
            UUID purchaseConsentId) {}
    record Snapshot(int schemaVersion, UUID catalogVersionId, UUID catalogOfferId, String channel,
            String versionToken, String productCode, String offerType, String audience,
            long amountCents, String currency, String billingCycle, int installments,
            List<Benefit> benefitLines, String snapshotSha256) {
        public Snapshot { benefitLines = List.copyOf(benefitLines); }
    }
    record Benefit(String entitlementType, long quantity, int validityDays, int grantIntervalMonths) {}
}
