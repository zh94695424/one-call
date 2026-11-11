package com.onecall.core.subject_number.internal.customerquery;

import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

public final class JooqCustomerBusinessNumberReader implements CustomerBusinessNumberReader {
    private final DSLContext context;

    public JooqCustomerBusinessNumberReader(DSLContext context) { this.context = context; }

    @Override
    public List<Row> fetch(Scope scope, Optional<Anchor> anchor, int fetchSize) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(anchor);
        if (fetchSize < 2 || fetchSize > 101) throw new IllegalArgumentException("Invalid fetch size");
        String owner = scope.organizationId() == null ? "s.owner_account_id = ?" : "s.owner_organization_id = ?";
        List<Object> parameters = new ArrayList<>();
        parameters.add(scope.organizationId() == null ? scope.accountId() : scope.organizationId());
        String seek = "";
        if (anchor.isPresent()) {
            seek = " AND (s.created_at < CAST(? AS timestamptz) OR (s.created_at = CAST(? AS timestamptz) AND b.id > ?))";
            var time = anchor.get().createdAt().atOffset(java.time.ZoneOffset.UTC);
            parameters.add(time);
            parameters.add(time);
            parameters.add(anchor.get().bindingId());
        }
        parameters.add(fetchSize);
        return context.fetch("""
                SELECT b.id AS binding_id, s.id AS subject_id, b.number_masked,
                       b.number_type, b.status AS binding_status, s.real_name_status,
                       s.created_at AS subject_created_at
                FROM subject_number.business_subject s
                JOIN subject_number.caller_number_binding b ON b.subject_id = s.id
                WHERE
                """ + owner + seek + " ORDER BY s.created_at DESC, b.id ASC LIMIT ?", parameters.toArray())
                .map(row -> new Row(row.get("binding_id", UUID.class), row.get("subject_id", UUID.class),
                        row.get("number_masked", String.class), row.get("number_type", String.class),
                        row.get("binding_status", String.class), row.get("real_name_status", String.class),
                        row.get("subject_created_at", OffsetDateTime.class).toInstant()));
    }
}
