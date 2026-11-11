package com.onecall.core.evidence.internal.customerquery;

import com.onecall.core.evidence.customerquery.CustomerSubjectEvidenceCountReader;
import java.util.Objects;
import java.util.UUID;
import org.jooq.DSLContext;

public final class JooqCustomerSubjectEvidenceCountReader implements CustomerSubjectEvidenceCountReader {
    private final DSLContext context;

    public JooqCustomerSubjectEvidenceCountReader(DSLContext context) { this.context = context; }

    @Override
    public long countForSubject(UUID subjectId) {
        Objects.requireNonNull(subjectId);
        var row = context.fetchOne("SELECT count(subject_id) AS evidence_count FROM evidence.evidence_record WHERE subject_id = ?", subjectId);
        if (row == null) throw new IllegalStateException("Invalid projection");
        return Objects.requireNonNull(row.get("evidence_count", Long.class));
    }
}
