package com.onecall.core.evidence.internal.customerevidencequery;

import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

public final class JooqCustomerEvidenceReader implements CustomerEvidenceReader {
    private static final String HEADER = """
            SELECT e.id, e.subject_id, e.status, e.source_type,
                CASE WHEN jsonb_typeof(e.identity_at_capture->'capturedAt') = 'string'
                     THEN e.identity_at_capture->>'capturedAt' END AS captured_at_text,
                e.consent_version, e.created_at, e.solidified_at
            FROM evidence.evidence_record e
            """;
    private final DSLContext context;

    public JooqCustomerEvidenceReader(DSLContext context) { this.context = context; }

    @Override
    public Set<UUID> findSubjectIdsWithEvidence(Set<UUID> ownedSubjectIds) {
        Objects.requireNonNull(ownedSubjectIds);
        if (ownedSubjectIds.isEmpty()) return Set.of();
        var subject = DSL.field("e.subject_id", UUID.class);
        return Set.copyOf(context.selectDistinct(subject).from(DSL.table("evidence.evidence_record e"))
                .where(subject.in(ownedSubjectIds)).fetch(subject));
    }

    @Override
    public List<Header> fetchPage(Set<UUID> subjectIds, Optional<Anchor> anchor, int fetchSize) {
        Objects.requireNonNull(subjectIds);
        Objects.requireNonNull(anchor);
        if (fetchSize < 2 || fetchSize > 101) throw new IllegalArgumentException("Invalid fetch size");
        if (subjectIds.isEmpty()) return List.of();
        var predicate = DSL.field("e.subject_id", UUID.class).in(subjectIds);
        org.jooq.Condition restriction = predicate;
        if (anchor.isPresent()) {
            restriction = restriction.and(DSL.condition("(e.created_at, e.id) < (CAST(? AS timestamptz), CAST(? AS uuid))",
                    anchor.get().createdAt().atOffset(java.time.ZoneOffset.UTC), anchor.get().evidenceId()));
        }
        return context.fetch(DSL.sql(HEADER + " WHERE {0} ORDER BY e.created_at DESC, e.id DESC LIMIT {1}",
                restriction, DSL.val(fetchSize))).map(this::header);
    }

    @Override
    public Map<UUID, List<String>> findArtifactRoles(Set<UUID> evidenceIds) {
        Objects.requireNonNull(evidenceIds);
        if (evidenceIds.isEmpty()) return Map.of();
        if (evidenceIds.size() > 100) throw new IllegalArgumentException("Invalid evidence count");
        var evidence = DSL.field("a.evidence_id", UUID.class);
        var role = DSL.field("a.artifact_role", String.class);
        var rows = context.selectDistinct(evidence, role).from(DSL.table("evidence.evidence_artifact a"))
                .where(evidence.in(evidenceIds)).fetch();
        Map<UUID, List<String>> result = new LinkedHashMap<>();
        for (var row : rows) result.computeIfAbsent(row.get(evidence), ignored -> new ArrayList<>()).add(row.get(role));
        result.replaceAll((identifier, roles) -> roles.stream()
                .sorted(java.util.Comparator.comparingInt(value -> "ORIGINAL".equals(value) ? 0 : 1)).toList());
        return Map.copyOf(result);
    }

    @Override
    public Optional<Header> findHeader(UUID evidenceId) {
        Objects.requireNonNull(evidenceId);
        return Optional.ofNullable(context.fetchOne(HEADER + " WHERE e.id = ?", evidenceId)).map(this::header);
    }

    @Override
    public List<Artifact> findArtifacts(UUID evidenceId) {
        Objects.requireNonNull(evidenceId);
        return context.fetch("""
                SELECT a.artifact_role, m.mime_type, m.size_bytes, r.validated_duration, m.sha256
                FROM evidence.evidence_record e
                JOIN evidence.evidence_artifact a ON a.evidence_id = e.id
                LEFT JOIN evidence.media_object m ON m.id = a.media_object_id
                LEFT JOIN evidence.recording r ON a.artifact_role = 'ORIGINAL'
                    AND r.id = e.source_ref_id AND r.media_object_id = a.media_object_id
                    AND r.status = 'VALIDATED'
                WHERE e.id = ?
                ORDER BY CASE a.artifact_role WHEN 'ORIGINAL' THEN 0 WHEN 'DERIVED' THEN 1 ELSE 2 END,
                    m.sha256 COLLATE "C", m.size_bytes, m.mime_type COLLATE "C"
                """, evidenceId).map(row -> new Artifact(row.get(0, String.class), row.get(1, String.class),
                        row.get(2, Long.class), row.get(3, Integer.class), row.get(4, String.class)));
    }

    private Header header(Record row) {
        return new Header(row.get(0, UUID.class), row.get(1, UUID.class), row.get(2, String.class),
                row.get(3, String.class), row.get(4, String.class), row.get(5, String.class),
                instant(row, 6), instant(row, 7));
    }

    private Instant instant(Record row, int index) {
        OffsetDateTime value = row.get(index, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
