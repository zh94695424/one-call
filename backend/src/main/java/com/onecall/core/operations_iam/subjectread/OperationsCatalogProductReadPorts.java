package com.onecall.core.operations_iam.subjectread;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class OperationsCatalogProductReadPorts {
    private OperationsCatalogProductReadPorts() { }
    public interface CatalogProductReader {
        List<ProductFacts> roots(Optional<Anchor> anchor, int fetchSize);
        List<VersionFacts> summaries(List<UUID> productIds);
        ProductFacts product(UUID productId);
        List<VersionFacts> versions(UUID productId);
        List<BenefitFacts> benefits(List<UUID> versionIds);
    }
    public record Anchor(String productCode, UUID productId) {
        @Override public String toString() { return "Anchor[REDACTED]"; }
    }
    public record ProductFacts(UUID productId, String productCode, String productFamily) {
        @Override public String toString() { return "ProductFacts[REDACTED]"; }
    }
    public record VersionFacts(UUID productId, UUID catalogVersionId, Integer versionNo, String saleStatus, String offerType,
            String audience, Long priceCents, String currency, String billingCycle, Integer installments,
            Integer grantIntervalMonths, Instant publishedAt, Integer revision) {
        @Override public String toString() { return "VersionFacts[REDACTED]"; }
    }
    public record BenefitFacts(UUID benefitId, UUID catalogVersionId, String entitlementType, Long quantity,
            Integer validityDays, Integer grantIntervalMonths) {
        @Override public String toString() { return "BenefitFacts[REDACTED]"; }
    }
}
