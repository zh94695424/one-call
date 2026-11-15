package com.onecall.core.activation_consent.internal.operationsread;

import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("operationsActivationHistoryReader")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class JooqOperationsActivationHistoryReader implements ActivationHistoryReader {
    private final DSLContext dsl;
    private final OperationsReadContext context;

    public JooqOperationsActivationHistoryReader(@Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context) { this.dsl = dsl; this.context = context; }

    @Override public List<TransitionFacts> fetch(Optional<Anchor> anchor, int fetchSize) {
        if (anchor == null || fetchSize < 2 || fetchSize > 101) throw new Failure(Code.SERVICE_UNAVAILABLE);
        return context.scoped(Role.ACTIVATION, () -> {
            var rows = anchor.isEmpty()
                    ? dsl.fetch("""
                            SELECT id, subject_id, status_event_id, from_status, to_status,
                                   from_revision, to_revision, source_channel, source_action_domain,
                                   reason_code, occurred_at
                            FROM activation_consent.service_activation_transition
                            ORDER BY occurred_at DESC, id DESC
                            LIMIT ?
                            """, fetchSize)
                    : dsl.fetch("""
                            SELECT id, subject_id, status_event_id, from_status, to_status,
                                   from_revision, to_revision, source_channel, source_action_domain,
                                   reason_code, occurred_at
                            FROM activation_consent.service_activation_transition
                            WHERE (occurred_at, id) < (CAST(? AS timestamptz), CAST(? AS uuid))
                            ORDER BY occurred_at DESC, id DESC
                            LIMIT ?
                            """, OffsetDateTime.ofInstant(anchor.get().occurredAt(), ZoneOffset.UTC), anchor.get().transitionId(), fetchSize);
            return rows.map(row -> {
                OffsetDateTime occurredAt = row.get("occurred_at", OffsetDateTime.class);
                return new TransitionFacts(row.get("id", UUID.class), row.get("subject_id", UUID.class), row.get("status_event_id", String.class),
                        row.get("from_status", String.class), row.get("to_status", String.class), row.get("from_revision", Integer.class),
                        row.get("to_revision", Integer.class), row.get("source_channel", String.class), row.get("source_action_domain", String.class),
                        row.get("reason_code", String.class), occurredAt == null ? null : occurredAt.toInstant());
            });
        });
    }
}
