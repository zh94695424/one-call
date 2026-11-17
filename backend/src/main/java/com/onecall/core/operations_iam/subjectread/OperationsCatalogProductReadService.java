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

public interface OperationsCatalogProductReadService {
    CatalogProductPage list(Caller caller, RawListRequest request);
    CatalogProductDetail detail(Caller caller, RawDetailRequest request);

    record RawListRequest(Map<String, List<String>> query, List<String> accept) {
        public RawListRequest { query = copyQuery(query); accept = copyValues(accept); }
        @Override public String toString() { return "RawListRequest[REDACTED]"; }
    }
    record RawDetailRequest(String productId, Map<String, List<String>> query, List<String> accept) {
        public RawDetailRequest { query = copyQuery(query); accept = copyValues(accept); }
        @Override public String toString() { return "RawDetailRequest[REDACTED]"; }
    }
    private static Map<String, List<String>> copyQuery(Map<String, List<String>> query) {
        if (query == null) return null;
        var copy = new LinkedHashMap<String, List<String>>();
        query.forEach((key, values) -> copy.put(key, copyValues(values)));
        return Collections.unmodifiableMap(copy);
    }
    private static List<String> copyValues(List<String> values) {
        return values == null ? null : Collections.unmodifiableList(new ArrayList<>(values));
    }
    record CatalogProductVersionSummary(UUID catalogVersionId, int versionNo, String saleStatus, String offerType, String audience) { }
    record CatalogProductSummary(UUID productId, String productCode, String productFamily, List<CatalogProductVersionSummary> versions) {
        public CatalogProductSummary { versions = List.copyOf(versions); }
    }
    record CatalogProductPage(List<CatalogProductSummary> items,
            @JsonInclude(JsonInclude.Include.ALWAYS) String nextCursor, boolean hasMore) {
        public CatalogProductPage { items = List.copyOf(items); }
    }
    record CatalogBenefitLine(String entitlementType, long quantity, int validityDays, int grantIntervalMonths) { }
    record CatalogVersionSummary(UUID catalogVersionId, int versionNo, String saleStatus, String offerType, String audience,
            @JsonInclude(JsonInclude.Include.ALWAYS) Long priceCents, String currency, String billingCycle, int installments,
            List<CatalogBenefitLine> benefitLines, @JsonInclude(JsonInclude.Include.ALWAYS) Instant publishedAt, int revision) {
        public CatalogVersionSummary { benefitLines = List.copyOf(benefitLines); }
    }
    record CatalogProductDetail(UUID productId, String productCode, String productFamily, List<CatalogVersionSummary> versions) {
        public CatalogProductDetail { versions = List.copyOf(versions); }
    }
}
