package com.onecall.core.subject_number.internal.operationsread;

import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

@Component("operationsSubjectReader")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class JooqOperationsSubjectReader implements SubjectReader {
    private static final String SAFE_SUBJECTS = """
            WITH safe_subjects AS (
                SELECT s.id, s.source, s.real_name_status, s.created_at,
                       display.number_masked, latest.status AS claim_status,
                       EXISTS (SELECT 1 FROM subject_number.caller_number_binding rb
                               JOIN subject_number.subject_risk_state r ON r.caller_number_binding_id = rb.id
                               WHERE rb.subject_id = s.id AND r.state = 'OPEN') AS risk_open
                FROM subject_number.business_subject s
                LEFT JOIN LATERAL (
                    SELECT b.number_masked FROM subject_number.caller_number_binding b
                    WHERE b.subject_id = s.id
                    ORDER BY CASE b.status WHEN 'ACTIVE' THEN 0 WHEN 'DISABLED' THEN 1 ELSE 2 END, b.id ASC LIMIT 1
                ) display ON true
                LEFT JOIN LATERAL (
                    SELECT c.status FROM subject_number.subject_claim c WHERE c.subject_id = s.id
                    ORDER BY c.risk_checked_at DESC, c.id DESC LIMIT 1
                ) latest ON true
            )
            SELECT id, source, real_name_status, created_at, number_masked, claim_status, risk_open FROM safe_subjects
            """;
    private final DSLContext dsl;
    private final OperationsReadContext context;
    public JooqOperationsSubjectReader(@Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context) { this.dsl = dsl; this.context = context; }

    @Override public List<Candidate> fetchCandidates(Filters filters, Optional<Anchor> anchor, int fetchSize) {
        if (filters == null || anchor == null || fetchSize < 2 || fetchSize > 101) throw new Failure(Code.SERVICE_UNAVAILABLE);
        return context.scoped(Role.SUBJECT, () -> {
            OffsetDateTime time = anchor.map(value -> value.createdAt().atOffset(ZoneOffset.UTC)).orElse(null);
            UUID identifier = anchor.map(Anchor::subjectId).orElse(null);
            Boolean risk = filters.riskState() == null ? null : "OPEN".equals(filters.riskState());
            return dsl.fetch(SAFE_SUBJECTS + """
                    WHERE (CAST(? AS uuid) IS NULL OR id = CAST(? AS uuid))
                      AND (CAST(? AS text) IS NULL OR source = CAST(? AS text))
                      AND (CAST(? AS text) IS NULL OR real_name_status = CAST(? AS text))
                      AND (CAST(? AS text) IS NULL OR claim_status = CAST(? AS text))
                      AND (CAST(? AS boolean) IS NULL OR risk_open = CAST(? AS boolean))
                      AND (CAST(? AS timestamptz) IS NULL OR (created_at, id) < (CAST(? AS timestamptz), CAST(? AS uuid)))
                    ORDER BY created_at DESC, id DESC LIMIT ?
                    """, filters.subjectId(), filters.subjectId(), filters.source(), filters.source(),
                    filters.realNameStatus(), filters.realNameStatus(), filters.claimStatus(), filters.claimStatus(),
                    risk, risk, time, time, identifier, fetchSize).map(row -> new Candidate(subject(row)));
        });
    }

    @Override public Optional<DetailFacts> detail(UUID subjectId, int historyLimit) {
        if (subjectId == null || historyLimit != 101) throw new Failure(Code.SERVICE_UNAVAILABLE);
        return context.scoped(Role.SUBJECT, () -> {
            var row = dsl.fetchOne(SAFE_SUBJECTS + " WHERE id = CAST(? AS uuid)", subjectId);
            if (row == null) return Optional.empty();
            var bindings = dsl.fetch("""
                    SELECT id, number_masked, number_type, status, verified_at
                    FROM subject_number.caller_number_binding
                    WHERE subject_id = CAST(? AS uuid) ORDER BY id ASC LIMIT 101
                    """, subjectId).map(binding -> new BindingFacts(binding.get("id", UUID.class), binding.get("number_masked", String.class),
                    binding.get("number_type", String.class), binding.get("status", String.class), instant(binding, "verified_at")));
            var claims = dsl.fetch("""
                    SELECT id, status, verification_method, risk_checked_at, verified_at
                    FROM subject_number.subject_claim
                    WHERE subject_id = CAST(? AS uuid) ORDER BY risk_checked_at DESC, id DESC LIMIT 101
                    """, subjectId).map(claim -> new ClaimFacts(claim.get("id", UUID.class), claim.get("status", String.class),
                    claim.get("verification_method", String.class), instant(claim, "risk_checked_at"), instant(claim, "verified_at")));
            return Optional.of(new DetailFacts(subject(row), bindings, claims));
        });
    }

    private static SubjectFacts subject(Record row) {
        Boolean open = row.get("risk_open", Boolean.class);
        if (open == null) throw new Failure(Code.SERVICE_UNAVAILABLE);
        return new SubjectFacts(row.get("id", UUID.class), row.get("source", String.class), row.get("number_masked", String.class),
                row.get("claim_status", String.class), row.get("real_name_status", String.class), open, instant(row, "created_at"));
    }
    private static Instant instant(Record row, String field) {
        var timestamp = row.get(field, OffsetDateTime.class);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
