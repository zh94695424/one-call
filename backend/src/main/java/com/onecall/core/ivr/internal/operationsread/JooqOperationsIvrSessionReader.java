package com.onecall.core.ivr.internal.operationsread;

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

@Component("operationsIvrSessionReader")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class JooqOperationsIvrSessionReader implements IvrSessionReader {
    private static final String SESSION = """
            SELECT s.id, s.call_id, s.subject_id, s.status, s.node_id,
                   s.protocol_version, s.prompt_version, s.guidance_status,
                   s.caller_masked, s.access_number_masked, s.created_at, s.confirmed_at
            FROM ivr.inbound_session s
            """;
    private final DSLContext dsl;
    private final OperationsReadContext context;
    public JooqOperationsIvrSessionReader(@Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context) { this.dsl = dsl; this.context = context; }

    @Override public List<IvrSessionSummary> fetch(Filters filters, Optional<Anchor> anchor, int fetchLimit) {
        if (filters == null || anchor == null || fetchLimit < 2 || fetchLimit > 101) throw new Failure(Code.SERVICE_UNAVAILABLE);
        return context.scoped(Role.IVR, () -> {
            var sql = new StringBuilder(SESSION).append("WHERE TRUE\n");
            var binds = new ArrayList<Object>();
            if (filters.subjectId() != null) { sql.append("AND s.subject_id = CAST(? AS uuid)\n"); binds.add(filters.subjectId()); }
            if (filters.callId() != null) { sql.append("AND s.call_id = ?\n"); binds.add(filters.callId()); }
            if (filters.taskId() != null) {
                sql.append("AND EXISTS (SELECT 1 FROM ivr.call_record c JOIN ivr.outbound_call_task t ON t.id = c.task_id WHERE c.session_id = s.id AND t.task_id = ?)\n");
                binds.add(filters.taskId());
            }
            if (filters.status() != null) { sql.append("AND s.status = ?\n"); binds.add(filters.status()); }
            if (anchor.isPresent()) {
                sql.append("AND (s.created_at, s.id) < (CAST(? AS timestamptz), CAST(? AS uuid))\n");
                binds.add(OffsetDateTime.ofInstant(anchor.get().createdAt(), ZoneOffset.UTC)); binds.add(anchor.get().sessionId());
            }
            sql.append("ORDER BY s.created_at DESC, s.id DESC\nLIMIT ?"); binds.add(fetchLimit);
            return dsl.fetch(sql.toString(), binds.toArray()).map(JooqOperationsIvrSessionReader::session);
        });
    }
    @Override public Optional<DetailFacts> detail(UUID sessionId) {
        return context.scoped(Role.IVR, () -> {
            var root = dsl.fetchOne(SESSION + "WHERE s.id = CAST(? AS uuid)", sessionId);
            if (root == null) return Optional.empty();
            var session = session(root);
            var calls = dsl.fetch("""
                    SELECT session_id, call_id, status, started_at, ended_at, authoritative_seconds
                    FROM ivr.call_record WHERE session_id = CAST(? AS uuid)
                    """, sessionId).map(row -> new CallFacts(row.get("session_id", UUID.class), row.get("call_id", String.class),
                            new IvrCallSummary(row.get("status", String.class), time(row, "started_at"), time(row, "ended_at"), row.get("authoritative_seconds", Integer.class))));
            var decisions = dsl.fetch("""
                    SELECT id, purpose, action, reason_code, reservation_id, decided_at, session_id, call_id
                    FROM ivr.ivr_decision WHERE session_id = CAST(? AS uuid) OR call_id = ?
                    ORDER BY decided_at ASC, id ASC
                    """, sessionId, session.callId()).map(row -> new DecisionFacts(row.get("session_id", UUID.class), row.get("call_id", String.class),
                            new IvrDecisionSummary(row.get("id", UUID.class), row.get("purpose", String.class), row.get("action", String.class),
                                    row.get("reason_code", String.class), row.get("reservation_id", UUID.class), time(row, "decided_at"))));
            var legs = dsl.fetch("""
                    SELECT leg_id, leg_type, direction, state, started_at, answered_at, ended_at, end_reason
                    FROM ivr.call_leg WHERE call_id = ?
                    ORDER BY started_at ASC, leg_id COLLATE "C" ASC
                    """, session.callId()).map(row -> new IvrLegSummary(row.get("leg_id", String.class), row.get("leg_type", String.class),
                            row.get("direction", String.class), row.get("state", String.class), time(row, "started_at"), time(row, "answered_at"),
                            time(row, "ended_at"), row.get("end_reason", String.class)));
            var events = dsl.fetch("""
                    SELECT event_id, event_type, sequence_no, occurred_at, processing_status, reason_code
                    FROM ivr.call_event WHERE call_id = ?
                    ORDER BY sequence_no ASC NULLS LAST, occurred_at ASC, event_id COLLATE "C" ASC
                    """, session.callId()).map(row -> new IvrCallEventSummary(row.get("event_id", String.class), row.get("event_type", String.class),
                            row.get("sequence_no", Long.class), time(row, "occurred_at"), row.get("processing_status", String.class), row.get("reason_code", String.class)));
            return Optional.of(new DetailFacts(session, calls, decisions, legs, events));
        });
    }
    private static IvrSessionSummary session(Record row) {
        return new IvrSessionSummary(row.get("id", UUID.class), row.get("call_id", String.class), row.get("subject_id", UUID.class),
                row.get("status", String.class), row.get("node_id", String.class), row.get("protocol_version", String.class),
                row.get("prompt_version", String.class), row.get("guidance_status", String.class), row.get("caller_masked", String.class),
                row.get("access_number_masked", String.class), time(row, "created_at"), time(row, "confirmed_at"));
    }
    private static Instant time(Record row, String field) {
        OffsetDateTime value = row.get(field, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
