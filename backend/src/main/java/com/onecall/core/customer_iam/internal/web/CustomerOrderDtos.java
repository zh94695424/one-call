package com.onecall.core.customer_iam.internal.web;

import java.util.UUID;
import java.util.List;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.Snapshot;
import com.onecall.core.customer_iam.internal.application.CustomerOrderQueryService;

public final class CustomerOrderDtos {
    private CustomerOrderDtos() {}
    public record OrderRequest(UUID subjectId, UUID catalogOfferId, String channel, String decision,
            String policyCode, String policyVersion, String promptVersion, String contentSha256, String expectedVersionToken) {}
    public record Page(List<Summary> items, String nextCursor, boolean hasMore) {
        public Page { items = List.copyOf(items); }
    }
    public record Summary(UUID orderId, String orderNo, UUID subjectId, String channel, UUID catalogVersionId,
            UUID catalogOfferId, long amountCents, String currency, String paymentStatus, String issuanceStatus, String createdAt) {
        static Summary from(CustomerOrderQueryService.Summary value) {
            return new Summary(value.orderId(), value.orderNo(), value.subjectId(), value.channel(), value.catalogVersionId(),
                    value.catalogOfferId(), value.amountCents(), value.currency(), value.paymentStatus(), value.issuanceStatus(),
                    java.time.format.DateTimeFormatter.ISO_INSTANT.format(value.createdAt()));
        }
    }
    public record Detail(UUID orderId, String orderNo, UUID subjectId, String channel, UUID catalogVersionId,
            UUID catalogOfferId, long amountCents, String currency, String paymentStatus, String issuanceStatus, String createdAt,
            String snapshotSha256, Snapshot snapshot, UUID purchaseConsentId, String paidAt, String closedAt) {
        static Detail from(CustomerOrderQueryService.Detail value) {
            var summary = Summary.from(value.summary());
            return new Detail(summary.orderId(), summary.orderNo(), summary.subjectId(), summary.channel(), summary.catalogVersionId(),
                    summary.catalogOfferId(), summary.amountCents(), summary.currency(), summary.paymentStatus(), summary.issuanceStatus(),
                    summary.createdAt(), value.snapshotSha256(), value.snapshot(), value.purchaseConsentId(),
                    value.paidAt() == null ? null : java.time.format.DateTimeFormatter.ISO_INSTANT.format(value.paidAt()),
                    value.closedAt() == null ? null : java.time.format.DateTimeFormatter.ISO_INSTANT.format(value.closedAt()));
        }
    }
}
