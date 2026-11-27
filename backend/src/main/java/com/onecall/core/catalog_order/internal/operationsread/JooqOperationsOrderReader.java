package com.onecall.core.catalog_order.internal.operationsread;

import com.onecall.core.catalog_order.customerorder.CustomerOrderSnapshotReader.InvalidSnapshotException;
import com.onecall.core.catalog_order.internal.customerorder.DefaultCustomerOrderSnapshotReader;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.BenefitLine;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.Snapshot;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

@Component("operationsOrderReader")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class JooqOperationsOrderReader implements OrderReader {
    private static final String SUMMARY = "o.id, o.order_no, o.subject_id, o.channel, o.catalog_version_id, "
            + "o.catalog_offer_id, o.amount_cents, o.currency, o.payment_status, o.created_at";
    private final DSLContext dsl;
    private final OperationsReadContext context;
    private final DefaultCustomerOrderSnapshotReader snapshots = new DefaultCustomerOrderSnapshotReader();

    public JooqOperationsOrderReader(@Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context) {
        this.dsl = dsl; this.context = context;
    }
    @Override public List<OrderFacts> fetchOrders(Filters filters, Optional<Anchor> anchor, int fetchLimit) {
        if (filters == null || anchor == null || fetchLimit < 2 || fetchLimit > 101
                || filters.paymentStatus() != null && !OperationsOrderReadPorts.PAYMENT_STATUSES.contains(filters.paymentStatus())
                || anchor.isPresent() && (!validInstant(anchor.get().createdAt()) || anchor.get().orderId() == null))
            throw new ContractRejectionException("Invalid order query");
        var predicates = new ArrayList<String>();
        var parameters = new ArrayList<Object>();
        if (filters.subjectId() != null) { predicates.add("o.subject_id = CAST(? AS uuid)"); parameters.add(filters.subjectId()); }
        if (filters.paymentStatus() != null) { predicates.add("o.payment_status = ?"); parameters.add(filters.paymentStatus()); }
        if (anchor.isPresent()) {
            predicates.add("(o.created_at, o.id) < (CAST(? AS timestamptz), CAST(? AS uuid))");
            parameters.add(OffsetDateTime.ofInstant(anchor.get().createdAt(), ZoneOffset.UTC));
            parameters.add(anchor.get().orderId());
        }
        parameters.add(fetchLimit);
        String where = predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates);
        return context.scoped(Role.ORDER, () -> dsl.fetch("SELECT " + SUMMARY + " FROM catalog_order.customer_order o"
                + where + " ORDER BY o.created_at DESC, o.id DESC LIMIT ?", parameters.toArray()).map(JooqOperationsOrderReader::summary));
    }
    @Override public Optional<DetailFacts> fetchOrder(UUID orderId) {
        if (orderId == null) throw new ContractRejectionException("Missing order identity");
        return context.scoped(Role.ORDER, () -> dsl.fetchOptional("SELECT " + SUMMARY
                + ", o.snapshot::text AS snapshot, o.snapshot_sha256, o.purchase_consent_id, o.paid_at, o.closed_at"
                + " FROM catalog_order.customer_order o WHERE o.id = CAST(? AS uuid)", orderId).map(row -> detail(row, orderId)));
    }
    private DetailFacts detail(Record row, UUID identity) {
        var root = summary(row);
        com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.Snapshot stored;
        try { stored = snapshots.read(row.get("snapshot", String.class), row.get("snapshot_sha256", String.class)); }
        catch (InvalidSnapshotException invalid) { throw new Failure(Code.NOT_FOUND); }
        if (!identity.equals(root.orderId()) || !stored.catalogVersionId().equals(root.catalogVersionId())
                || !stored.catalogOfferId().equals(root.catalogOfferId()) || !stored.channel().equals(root.channel())
                || root.amountCents() == null || stored.amountCents() != root.amountCents()
                || !stored.currency().equals(root.currency()))
            throw new Failure(Code.NOT_FOUND);
        var snapshot = new Snapshot(stored.schemaVersion(), stored.catalogVersionId(), stored.catalogOfferId(), stored.channel(),
                stored.versionToken(), stored.productCode(), stored.offerType(), stored.audience(), stored.amountCents(),
                stored.currency(), stored.billingCycle(), stored.installments(), stored.benefitLines().stream()
                .map(benefit -> new BenefitLine(benefit.entitlementType(), benefit.quantity(), benefit.validityDays(), benefit.grantIntervalMonths())).toList(),
                stored.snapshotSha256());
        return new DetailFacts(root, stored.snapshotSha256(), snapshot, row.get("purchase_consent_id", UUID.class),
                instant(row, "paid_at"), instant(row, "closed_at"));
    }
    private static OrderFacts summary(Record row) {
        return new OrderFacts(row.get("id", UUID.class), row.get("order_no", String.class), row.get("subject_id", UUID.class),
                row.get("channel", String.class), row.get("catalog_version_id", UUID.class), row.get("catalog_offer_id", UUID.class),
                row.get("amount_cents", Long.class), row.get("currency", String.class), row.get("payment_status", String.class), instant(row, "created_at"));
    }
    private static Instant instant(Record row, String column) {
        var value = row.get(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
    private static boolean validInstant(Instant value) {
        return value != null && value.getNano() % 1000 == 0
                && !value.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
                && !value.isAfter(Instant.parse("9999-12-31T23:59:59.999999Z"));
    }
}
