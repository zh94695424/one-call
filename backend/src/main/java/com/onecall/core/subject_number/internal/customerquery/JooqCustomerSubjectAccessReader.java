package com.onecall.core.subject_number.internal.customerquery;

import com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

public final class JooqCustomerSubjectAccessReader implements CustomerSubjectAccessReader {
    private final DSLContext context;

    public JooqCustomerSubjectAccessReader(DSLContext context) { this.context = context; }

    @Override
    public Optional<UUID> findOwnedSubject(UUID subjectId, UUID accountId, UUID organizationId) {
        Objects.requireNonNull(subjectId);
        Objects.requireNonNull(accountId);
        var row = organizationId == null
                ? context.fetchOne("SELECT s.id FROM subject_number.business_subject s WHERE s.id = ? AND s.owner_account_id = ?", subjectId, accountId)
                : context.fetchOne("SELECT s.id FROM subject_number.business_subject s WHERE s.id = ? AND s.owner_organization_id = ?", subjectId, organizationId);
        if (row == null) return Optional.empty();
        return Optional.of(Objects.requireNonNull(row.get(0, UUID.class)));
    }

    @Override
    public boolean hasOpenRisk(UUID subjectId) {
        Objects.requireNonNull(subjectId);
        var row = context.fetchOne("""
                SELECT EXISTS (SELECT 1 FROM subject_number.caller_number_binding b
                JOIN subject_number.subject_risk_state r ON r.caller_number_binding_id = b.id
                WHERE b.subject_id = ? AND r.state = ?)
                """, subjectId, "OPEN");
        if (row == null) throw new IllegalStateException("Invalid projection");
        return Objects.requireNonNull(row.get(0, Boolean.class));
    }
}
