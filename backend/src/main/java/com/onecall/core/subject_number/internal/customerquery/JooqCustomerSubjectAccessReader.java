package com.onecall.core.subject_number.internal.customerquery;

import com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    @Override
    public Set<UUID> findOwnedSubjectIds(UUID accountId, UUID organizationId) {
        Objects.requireNonNull(accountId);
        var rows = organizationId == null
                ? context.fetch("SELECT DISTINCT s.id FROM subject_number.business_subject s WHERE s.owner_account_id = ?", accountId)
                : context.fetch("SELECT DISTINCT s.id FROM subject_number.business_subject s WHERE s.owner_organization_id = ?", organizationId);
        return Set.copyOf(rows.getValues(0, UUID.class));
    }

    @Override
    public boolean hasOpenRisk(Set<UUID> subjectIds) {
        Objects.requireNonNull(subjectIds);
        if (subjectIds.isEmpty()) return false;
        var subjects = org.jooq.impl.DSL.field("b.subject_id", UUID.class).in(subjectIds);
        var query = org.jooq.impl.DSL.selectOne()
                .from(org.jooq.impl.DSL.table("subject_number.caller_number_binding b"))
                .join(org.jooq.impl.DSL.table("subject_number.subject_risk_state r"))
                .on(org.jooq.impl.DSL.condition("r.caller_number_binding_id = b.id"))
                .where(subjects).and(org.jooq.impl.DSL.field("r.state", String.class).eq("OPEN"));
        return context.fetchExists(query);
    }
}
