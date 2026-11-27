package com.onecall.core.operations_iam.subjectread;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OperationsOrderReadService {
    OrderPage list(Caller caller, RawListRequest request);
    Order detail(Caller caller, RawDetailRequest request);

    record RawListRequest(Map<String, List<String>> query, List<String> accept) {
        public RawListRequest { query = copyQuery(query); accept = copyValues(accept); }
        @Override public String toString() { return "RawListRequest[REDACTED]"; }
    }
    record RawDetailRequest(String orderId, Map<String, List<String>> query, List<String> accept) {
        public RawDetailRequest { query = copyQuery(query); accept = copyValues(accept); }
        @Override public String toString() { return "RawDetailRequest[REDACTED]"; }
    }
    private static Map<String, List<String>> copyQuery(Map<String, List<String>> query) {
        if (query == null) return null;
        var result = new LinkedHashMap<String, List<String>>();
        query.forEach((key, values) -> result.put(key, copyValues(values)));
        return Collections.unmodifiableMap(result);
    }
    private static List<String> copyValues(List<String> values) {
        return values == null ? null : Collections.unmodifiableList(new ArrayList<>(values));
    }
    record OrderSummary(UUID orderId, String orderNo, UUID subjectId, String channel, UUID catalogVersionId,
            UUID catalogOfferId, long amountCents, String currency, String paymentStatus, String issuanceStatus,
            Instant createdAt) { }
    record OrderPage(List<OrderSummary> items, @JsonInclude(JsonInclude.Include.ALWAYS) String nextCursor, boolean hasMore) {
        public OrderPage { items = List.copyOf(items); }
    }
    record BenefitLine(String entitlementType, long quantity, int validityDays, int grantIntervalMonths) { }
    record Snapshot(int schemaVersion, UUID catalogVersionId, UUID catalogOfferId, String channel, String versionToken,
            String productCode, String offerType, String audience, long amountCents, String currency, String billingCycle,
            int installments, List<BenefitLine> benefitLines, String snapshotSha256) {
        public Snapshot { benefitLines = List.copyOf(benefitLines); }
    }
    record Order(UUID orderId, String orderNo, UUID subjectId, String channel, UUID catalogVersionId,
            UUID catalogOfferId, long amountCents, String currency, String paymentStatus, String issuanceStatus,
            Instant createdAt, String snapshotSha256, Snapshot snapshot, UUID purchaseConsentId,
            @JsonInclude(JsonInclude.Include.ALWAYS) Instant paidAt,
            @JsonInclude(JsonInclude.Include.ALWAYS) Instant closedAt) { }
}
