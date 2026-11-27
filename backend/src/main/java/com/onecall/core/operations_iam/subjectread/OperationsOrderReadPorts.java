package com.onecall.core.operations_iam.subjectread;

import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.Snapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OperationsOrderReadPorts {
    Set<String> PAYMENT_STATUSES = Set.of("UNPAID", "UNKNOWN", "PAID", "CLOSED");
    final class ContractRejectionException extends IllegalArgumentException {
        public ContractRejectionException(String message) { super(message); }
    }
    interface OrderReader {
        List<OrderFacts> fetchOrders(Filters filters, Optional<Anchor> anchor, int fetchLimit);
        Optional<DetailFacts> fetchOrder(UUID orderId);
    }
    interface IssuanceReader {
        List<IssuanceFacts> fetchIssuances(List<UUID> orderIds);
    }
    record Filters(UUID subjectId, String paymentStatus) { }
    record Anchor(Instant createdAt, UUID orderId) { }
    record OrderFacts(UUID orderId, String orderNo, UUID subjectId, String channel, UUID catalogVersionId,
            UUID catalogOfferId, Long amountCents, String currency, String paymentStatus, Instant createdAt) { }
    record DetailFacts(OrderFacts order, String snapshotSha256, Snapshot snapshot, UUID purchaseConsentId,
            Instant paidAt, Instant closedAt) { }
    record IssuanceFacts(UUID orderId, String status) { }
}
