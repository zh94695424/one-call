package com.onecall.core.catalog_order.internal.customerquery;

import com.onecall.core.catalog_order.customerquery.CustomerSubjectOrderCountReader;
import java.util.Objects;
import java.util.UUID;
import org.jooq.DSLContext;

public final class JooqCustomerSubjectOrderCountReader implements CustomerSubjectOrderCountReader {
    private final DSLContext context;

    public JooqCustomerSubjectOrderCountReader(DSLContext context) { this.context = context; }

    @Override
    public long countForWorkspace(UUID subjectId, UUID accountId, UUID organizationId) {
        Objects.requireNonNull(subjectId);
        Objects.requireNonNull(accountId);
        var row = organizationId == null
                ? context.fetchOne("SELECT count(subject_id) AS order_count FROM catalog_order.customer_order WHERE subject_id = ? AND account_id = ? AND owner_organization_id IS NULL", subjectId, accountId)
                : context.fetchOne("SELECT count(subject_id) AS order_count FROM catalog_order.customer_order WHERE subject_id = ? AND owner_organization_id = ?", subjectId, organizationId);
        if (row == null) throw new IllegalStateException("Invalid projection");
        return Objects.requireNonNull(row.get("order_count", Long.class));
    }
}
