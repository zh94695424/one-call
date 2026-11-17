package com.onecall.core.evidence.internal.operationsread;

import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("operationsIvrEvidenceReader")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class JooqOperationsIvrEvidenceReader implements IvrEvidenceReader {
    private final DSLContext dsl;
    private final OperationsReadContext context;
    public JooqOperationsIvrEvidenceReader(@Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context) { this.dsl = dsl; this.context = context; }
    @Override public List<EvidenceFacts> find(UUID sessionId, String callId) {
        return context.scoped(Role.EVIDENCE, () -> {
            var rows = dsl.fetch("""
                    SELECT id, status, session_id, call_id
                    FROM evidence.evidence_record
                    WHERE session_id = CAST(? AS uuid) OR call_id = ?
                    ORDER BY id ASC
                    """, sessionId, callId);
            var roles = new LinkedHashMap<UUID, Set<String>>();
            for (var row : rows) {
                UUID id = row.get("id", UUID.class);
                if (id == null || roles.put(id, new HashSet<>()) != null || !callId.equals(row.get("call_id", String.class))
                        || row.get("session_id", UUID.class) != null && !sessionId.equals(row.get("session_id", UUID.class))) throw unavailable();
            }
            var ids = new ArrayList<>(roles.keySet());
            for (int start = 0; start < ids.size(); start += 1000) {
                var chunk = ids.subList(start, Math.min(start + 1000, ids.size()));
                var allowed = new HashSet<>(chunk);
                String placeholders = String.join(",", Collections.nCopies(chunk.size(), "CAST(? AS uuid)"));
                var artifacts = dsl.fetch("SELECT evidence_id, artifact_role FROM evidence.evidence_artifact WHERE evidence_id IN (" + placeholders
                        + ") GROUP BY evidence_id, artifact_role ORDER BY evidence_id ASC, CASE artifact_role WHEN 'ORIGINAL' THEN 0 WHEN 'DERIVED' THEN 1 ELSE 2 END", chunk.toArray());
                for (var row : artifacts) {
                    UUID id = row.get("evidence_id", UUID.class); String role = row.get("artifact_role", String.class);
                    if (!allowed.contains(id) || role == null || !Set.of("ORIGINAL", "DERIVED").contains(role)) throw unavailable();
                    roles.get(id).add(role);
                }
            }
            return rows.map(row -> {
                UUID id = row.get("id", UUID.class);
                var ordered = new ArrayList<String>();
                for (String role : List.of("ORIGINAL", "DERIVED")) if (roles.get(id).contains(role)) ordered.add(role);
                return new EvidenceFacts(id, row.get("status", String.class), row.get("session_id", UUID.class), row.get("call_id", String.class), ordered);
            });
        });
    }
    private static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
}
