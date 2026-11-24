package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerCatalogOfferQueryService;
import java.util.List;
import java.util.UUID;

public record CustomerCatalogOfferPage(List<Item> items, String nextCursor, boolean hasMore) {
    public CustomerCatalogOfferPage { items = List.copyOf(items); }
    public enum Channel { MINIAPP, WEB }
    public enum OfferType { SUBSCRIPTION, ONE_TIME, ADD_ON, QUOTE }
    public enum Audience { PERSONAL, ENTERPRISE }
    public enum Currency { CNY }
    public enum BillingCycle { MONTHLY, QUARTERLY, ANNUAL, ONCE, QUOTE }
    public enum EntitlementType { RECORDING_SECONDS, AI_SECONDS, EVIDENCE_COUNT, STORAGE_BYTES }
    public record Item(String productCode, UUID catalogVersionId, UUID catalogOfferId, Channel channel,
            int versionNo, OfferType offerType, Audience audience, Long priceCents, Currency currency,
            BillingCycle billingCycle, int installments, List<BenefitLine> benefitLines, String versionToken) {
        public Item { benefitLines = List.copyOf(benefitLines); }
    }
    public record BenefitLine(EntitlementType entitlementType, long quantity, int validityDays, int grantIntervalMonths) { }

    static CustomerCatalogOfferPage from(CustomerCatalogOfferQueryService.Page page, String cursor) {
        return new CustomerCatalogOfferPage(page.items().stream().map(item -> new Item(item.productCode(),
                item.catalogVersionId(), item.catalogOfferId(), Channel.valueOf(item.channel()), item.versionNo(),
                OfferType.valueOf(item.offerType()), Audience.valueOf(item.audience()), item.priceCents(),
                Currency.valueOf(item.currency()), BillingCycle.valueOf(item.billingCycle()), item.installments(),
                item.benefitLines().stream().map(line -> new BenefitLine(EntitlementType.valueOf(line.entitlementType()),
                        line.quantity(), line.validityDays(), line.grantIntervalMonths())).toList(),
                item.versionToken())).toList(), cursor, page.hasMore());
    }
}
