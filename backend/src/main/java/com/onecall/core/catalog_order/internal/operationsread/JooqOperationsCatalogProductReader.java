package com.onecall.core.catalog_order.internal.operationsread;

import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("operationsCatalogProductReader")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class JooqOperationsCatalogProductReader implements CatalogProductReader {
    private final DSLContext dsl;
    private final OperationsReadContext context;
    public JooqOperationsCatalogProductReader(@Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context) { this.dsl = dsl; this.context = context; }
    @Override public List<ProductFacts> roots(Optional<Anchor> anchor, int fetchSize) {
        if (anchor == null || fetchSize < 2 || fetchSize > 101) throw unavailable();
        return context.scoped(Role.ORDER, () -> anchor.isEmpty()
                ? dsl.fetch("SELECT id, product_code, product_family FROM catalog_order.catalog_product ORDER BY product_code COLLATE \"C\" ASC, id ASC LIMIT ?", fetchSize).map(JooqOperationsCatalogProductReader::root)
                : dsl.fetch("SELECT id, product_code, product_family FROM catalog_order.catalog_product WHERE (product_code COLLATE \"C\", id) > (CAST(? AS text) COLLATE \"C\", CAST(? AS uuid)) ORDER BY product_code COLLATE \"C\" ASC, id ASC LIMIT ?",
                        anchor.get().productCode(), anchor.get().productId(), fetchSize).map(JooqOperationsCatalogProductReader::root));
    }
    @Override public List<VersionFacts> summaries(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty() || productIds.size() > 100 || productIds.stream().anyMatch(java.util.Objects::isNull)) throw unavailable();
        return context.scoped(Role.ORDER, () -> dsl.fetch("SELECT product_id, id, version_no, sale_status, offer_type, audience FROM catalog_order.catalog_version WHERE product_id IN ("
                + placeholders(productIds.size()) + ") ORDER BY product_id, version_no, id", productIds.toArray()).map(row -> new VersionFacts(
                        row.get("product_id", UUID.class), row.get("id", UUID.class), row.get("version_no", Integer.class), row.get("sale_status", String.class),
                        row.get("offer_type", String.class), row.get("audience", String.class), null, null, null, null, null, null, null)));
    }
    @Override public ProductFacts product(UUID productId) {
        if (productId == null) throw unavailable();
        return context.scoped(Role.ORDER, () -> {
            var row = dsl.fetchOne("SELECT id, product_code, product_family FROM catalog_order.catalog_product WHERE id = CAST(? AS uuid)", productId);
            return row == null ? null : root(row);
        });
    }
    @Override public List<VersionFacts> versions(UUID productId) {
        if (productId == null) throw unavailable();
        return context.scoped(Role.ORDER, () -> dsl.fetch("SELECT product_id, id, version_no, sale_status, offer_type, audience, price_cents, currency, billing_cycle, installments, grant_interval_months, published_at, revision FROM catalog_order.catalog_version WHERE product_id = CAST(? AS uuid) ORDER BY product_id, version_no, id", productId)
                .map(row -> {
                    OffsetDateTime published = row.get("published_at", OffsetDateTime.class);
                    return new VersionFacts(row.get("product_id", UUID.class), row.get("id", UUID.class), row.get("version_no", Integer.class), row.get("sale_status", String.class),
                            row.get("offer_type", String.class), row.get("audience", String.class), row.get("price_cents", Long.class), row.get("currency", String.class),
                            row.get("billing_cycle", String.class), row.get("installments", Integer.class), row.get("grant_interval_months", Integer.class),
                            published == null ? null : published.toInstant(), row.get("revision", Integer.class));
                }));
    }
    @Override public List<BenefitFacts> benefits(List<UUID> versionIds) {
        if (versionIds == null || versionIds.isEmpty() || versionIds.stream().anyMatch(java.util.Objects::isNull)) throw unavailable();
        return context.scoped(Role.ORDER, () -> dsl.fetch("SELECT id, catalog_version_id, entitlement_type, quantity, validity_days, grant_interval_months FROM catalog_order.catalog_benefit_line WHERE catalog_version_id IN ("
                + placeholders(versionIds.size()) + ") ORDER BY catalog_version_id, id", versionIds.toArray()).map(row -> new BenefitFacts(row.get("id", UUID.class),
                        row.get("catalog_version_id", UUID.class), row.get("entitlement_type", String.class), row.get("quantity", Long.class), row.get("validity_days", Integer.class), row.get("grant_interval_months", Integer.class))));
    }
    private static String placeholders(int size) { return String.join(", ", Collections.nCopies(size, "CAST(? AS uuid)")); }
    private static ProductFacts root(Record row) { return new ProductFacts(row.get("id", UUID.class), row.get("product_code", String.class), row.get("product_family", String.class)); }
    private static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
}
