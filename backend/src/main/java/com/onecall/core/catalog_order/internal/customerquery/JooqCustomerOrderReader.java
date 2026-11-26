package com.onecall.core.catalog_order.internal.customerquery;

import com.onecall.core.catalog_order.customerquery.CustomerOrderReader;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;

public final class JooqCustomerOrderReader implements CustomerOrderReader {
    private static final String SUMMARY = "o.id, o.order_no, o.subject_id, o.channel, o.catalog_version_id, "
            + "o.catalog_offer_id, o.amount_cents, o.currency, o.payment_status, o.created_at";
    private final DSLContext context;

    public JooqCustomerOrderReader(DSLContext context) { this.context = context; }

    @Override
    public List<SummaryRow> fetchOrders(Scope scope, Optional<Anchor> anchor, int fetchLimit) {
        Objects.requireNonNull(anchor);
        if (fetchLimit < 2 || fetchLimit > 101) throw new IllegalArgumentException("Invalid fetch limit");
        List<Object> parameters = parameters(scope);
        String seek = "";
        if (anchor.isPresent()) {
            seek = " AND (o.created_at, o.id) < (CAST(? AS timestamptz), CAST(? AS uuid))";
            parameters.add(OffsetDateTime.ofInstant(anchor.get().createdAt(), ZoneOffset.UTC));
            parameters.add(anchor.get().orderId());
        }
        parameters.add(fetchLimit);
        return context.fetch("SELECT " + SUMMARY + " FROM catalog_order.customer_order o WHERE "
                + ownership(scope) + seek + " ORDER BY o.created_at DESC, o.id DESC LIMIT ?", parameters.toArray())
                .map(JooqCustomerOrderReader::summary);
    }

    @Override
    public Optional<DetailRow> fetchOrder(Scope scope, UUID orderId) {
        List<Object> parameters = parameters(scope);
        parameters.add(Objects.requireNonNull(orderId));
        return context.fetchOptional("SELECT " + SUMMARY
                + ", o.snapshot::text AS snapshot, o.snapshot_sha256, o.purchase_consent_id, o.paid_at, o.closed_at"
                + " FROM catalog_order.customer_order o WHERE " + ownership(scope) + " AND o.id = CAST(? AS uuid)",
                parameters.toArray()).map(row -> new DetailRow(summary(row), row.get("snapshot", String.class),
                        row.get("snapshot_sha256", String.class), row.get("purchase_consent_id", UUID.class),
                        instant(row, "paid_at"), instant(row, "closed_at")));
    }

    @Override
    public List<IssuanceRow> fetchIssuances(List<UUID> orderIds) {
        Objects.requireNonNull(orderIds);
        if (orderIds.isEmpty() || orderIds.size() > 100 || orderIds.stream().anyMatch(Objects::isNull)
                || orderIds.stream().distinct().count() != orderIds.size())
            throw new IllegalArgumentException("Invalid order batch");
        return context.fetch("SELECT order_id, status FROM entitlement_metering.entitlement_issuance WHERE order_id IN ("
                + String.join(",", Collections.nCopies(orderIds.size(), "CAST(? AS uuid)")) + ")", orderIds.toArray())
                .map(row -> new IssuanceRow(row.get("order_id", UUID.class), row.get("status", String.class)));
    }

    private static String ownership(Scope scope) {
        return "o.account_id = CAST(? AS uuid) AND o.owner_organization_id "
                + (scope.organizationId() == null ? "IS NULL" : "= CAST(? AS uuid)");
    }

    private static List<Object> parameters(Scope scope) {
        Objects.requireNonNull(scope);
        List<Object> parameters = new ArrayList<>();
        parameters.add(Objects.requireNonNull(scope.accountId()));
        if (scope.organizationId() != null) parameters.add(scope.organizationId());
        return parameters;
    }

    private static SummaryRow summary(Record row) {
        return new SummaryRow(row.get("id", UUID.class), row.get("order_no", String.class), row.get("subject_id", UUID.class),
                row.get("channel", String.class), row.get("catalog_version_id", UUID.class), row.get("catalog_offer_id", UUID.class),
                row.get("amount_cents", Long.class), row.get("currency", String.class), row.get("payment_status", String.class),
                instant(row, "created_at"));
    }

    private static java.time.Instant instant(Record row, String column) {
        OffsetDateTime value = row.get(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
