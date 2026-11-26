package com.onecall.core.catalog_order.customerquery;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerOrderReader {
    List<SummaryRow> fetchOrders(Scope scope, Optional<Anchor> anchor, int fetchLimit);
    Optional<DetailRow> fetchOrder(Scope scope, UUID orderId);
    List<IssuanceRow> fetchIssuances(List<UUID> orderIds);

    record Scope(UUID accountId, UUID organizationId) {}
    record Anchor(Instant createdAt, UUID orderId) {}
    record SummaryRow(UUID orderId, String orderNo, UUID subjectId, String channel, UUID catalogVersionId,
            UUID catalogOfferId, Long amountCents, String currency, String paymentStatus, Instant createdAt) {}
    record DetailRow(SummaryRow summary, String snapshot, String snapshotSha256, UUID purchaseConsentId,
            Instant paidAt, Instant closedAt) {}
    record IssuanceRow(UUID orderId, String status) {}
}
