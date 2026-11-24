package com.onecall.core.catalog_order.customercatalogquery;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CustomerCatalogOfferReader {
    List<OfferRow> fetchOffers(Scope scope, Optional<Anchor> anchor, int fetchSize);
    Map<UUID, List<BenefitRow>> fetchBenefits(Set<UUID> catalogVersionIds);

    record Scope(String channel, String audience, Optional<String> offerType) {
        public Scope {
            Objects.requireNonNull(channel);
            Objects.requireNonNull(audience);
            Objects.requireNonNull(offerType);
            if (!Set.of("MINIAPP", "WEB").contains(channel) || !Set.of("PERSONAL", "ENTERPRISE").contains(audience)
                    || offerType.isPresent() && !Set.of("SUBSCRIPTION", "ONE_TIME", "ADD_ON", "QUOTE").contains(offerType.get()))
                throw new IllegalArgumentException("Invalid scope");
        }
    }

    record Anchor(String productCode, int versionNo, UUID catalogOfferId) {
        public Anchor {
            Objects.requireNonNull(productCode);
            Objects.requireNonNull(catalogOfferId);
            if (!productCode.matches("^[A-Z][A-Z0-9_-]{1,63}$") || versionNo < 1)
                throw new IllegalArgumentException("Invalid anchor");
        }
    }

    record OfferRow(String productCode, UUID catalogVersionId, UUID catalogOfferId, String channel,
                    int versionNo, String offerType, String audience, Long priceCents, String currency,
                    String billingCycle, int installments, String versionToken) { }

    record BenefitRow(UUID catalogVersionId, String entitlementType, long quantity, int validityDays, int grantIntervalMonths) { }
}
