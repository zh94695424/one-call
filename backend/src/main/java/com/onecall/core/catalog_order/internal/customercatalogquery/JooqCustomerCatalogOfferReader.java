package com.onecall.core.catalog_order.internal.customercatalogquery;

import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;

public final class JooqCustomerCatalogOfferReader implements CustomerCatalogOfferReader {
    private final DSLContext context;

    public JooqCustomerCatalogOfferReader(DSLContext context) { this.context = context; }

    @Override
    public List<OfferRow> fetchOffers(Scope scope, Optional<Anchor> anchor, int fetchSize) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(anchor);
        if (fetchSize < 2 || fetchSize > 101) throw new IllegalArgumentException("Invalid fetch size");
        List<Object> parameters = new ArrayList<>();
        parameters.add(scope.channel());
        parameters.add(scope.audience());
        String narrowing = "";
        if (scope.offerType().isPresent()) {
            narrowing = " AND v.offer_type = ?";
            parameters.add(scope.offerType().get());
        }
        String seek = "";
        if (anchor.isPresent()) {
            seek = " AND (p.product_code COLLATE \"C\" > ?"
                    + " OR (p.product_code COLLATE \"C\" = ? AND v.version_no < ?)"
                    + " OR (p.product_code COLLATE \"C\" = ? AND v.version_no = ? AND o.id > CAST(? AS uuid)))";
            parameters.add(anchor.get().productCode());
            parameters.add(anchor.get().productCode());
            parameters.add(anchor.get().versionNo());
            parameters.add(anchor.get().productCode());
            parameters.add(anchor.get().versionNo());
            parameters.add(anchor.get().catalogOfferId());
        }
        parameters.add(fetchSize);
        return context.fetch("""
                SELECT p.product_code, v.id AS catalog_version_id, o.id AS catalog_offer_id, o.channel,
                       v.version_no, v.offer_type, v.audience, v.price_cents, v.currency, v.billing_cycle,
                       v.installments, o.version_token
                FROM catalog_order.catalog_offer o
                JOIN catalog_order.catalog_version v ON v.id = o.catalog_version_id
                JOIN catalog_order.catalog_product p ON p.id = v.product_id
                WHERE v.sale_status = 'ACTIVE' AND o.offer_status = 'ACTIVE'
                  AND o.channel = ? AND v.audience = ?""" + narrowing + seek + """
                 ORDER BY p.product_code COLLATE "C" ASC, v.version_no DESC, o.id ASC
                LIMIT ?""", parameters.toArray())
                .map(row -> new OfferRow(row.get("product_code", String.class), row.get("catalog_version_id", UUID.class),
                        row.get("catalog_offer_id", UUID.class), row.get("channel", String.class),
                        row.get("version_no", Integer.class), row.get("offer_type", String.class),
                        row.get("audience", String.class), row.get("price_cents", Long.class),
                        row.get("currency", String.class), row.get("billing_cycle", String.class),
                        row.get("installments", Integer.class), row.get("version_token", String.class)));
    }

    @Override
    public Map<UUID, List<BenefitRow>> fetchBenefits(Set<UUID> catalogVersionIds) {
        Objects.requireNonNull(catalogVersionIds);
        if (catalogVersionIds.isEmpty() || catalogVersionIds.size() > 100 || catalogVersionIds.stream().anyMatch(Objects::isNull))
            throw new IllegalArgumentException("Invalid version ids");
        String placeholders = String.join(", ", Collections.nCopies(catalogVersionIds.size(), "CAST(? AS uuid)"));
        Map<UUID, List<BenefitRow>> grouped = new LinkedHashMap<>();
        for (var row : context.fetch("SELECT catalog_version_id, entitlement_type, quantity, validity_days, grant_interval_months"
                + " FROM catalog_order.catalog_benefit_line WHERE catalog_version_id IN (" + placeholders + ")"
                + " ORDER BY catalog_version_id, entitlement_type COLLATE \"C\" ASC", catalogVersionIds.toArray())) {
            BenefitRow benefit = new BenefitRow(row.get("catalog_version_id", UUID.class), row.get("entitlement_type", String.class),
                    row.get("quantity", Long.class), row.get("validity_days", Integer.class), row.get("grant_interval_months", Integer.class));
            grouped.computeIfAbsent(benefit.catalogVersionId(), key -> new ArrayList<>()).add(benefit);
        }
        Map<UUID, List<BenefitRow>> immutable = new LinkedHashMap<>();
        for (var entry : grouped.entrySet()) immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        return Map.copyOf(immutable);
    }
}
