package com.onecall.core.entitlement_metering.internal.operationsread;

import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("operationsTrialReader")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class JooqOperationsTrialReader implements TrialReader {
    private final DSLContext dsl;
    private final OperationsReadContext context;
    public JooqOperationsTrialReader(@Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context) { this.dsl = dsl; this.context = context; }

    @Override public Optional<TrialFacts> find(UUID subjectId, Instant now) {
        if (subjectId == null || now == null) throw new Failure(Code.SERVICE_UNAVAILABLE);
        return context.scoped(Role.TRIAL, () -> {
            var rows = dsl.fetch("""
                    SELECT t.id AS trial_id, t.subject_id, t.campaign_key, t.entitlement_grant_id,
                           t.expires_at AS trial_expires_at, g.id AS grant_id,
                           g.subject_id AS grant_subject_id, g.source_type, g.entitlement_type,
                           g.granted_units, g.expires_at AS grant_expires_at,
                           b.id AS balance_id, b.grant_id AS balance_grant_id,
                           b.granted_units AS balance_granted_units, b.reserved_units,
                           b.consumed_units, b.status AS balance_status
                    FROM entitlement_metering.trial_grant t
                    LEFT JOIN entitlement_metering.entitlement_grant g ON g.id = t.entitlement_grant_id
                    LEFT JOIN entitlement_metering.entitlement_balance b ON b.grant_id = g.id
                    WHERE t.subject_id = CAST(? AS uuid) LIMIT 2
                    """, subjectId);
            if (rows.size() > 1) throw new Failure(Code.SERVICE_UNAVAILABLE);
            if (rows.isEmpty()) return Optional.empty();
            var row = rows.getFirst();
            return Optional.of(new TrialFacts(row.get("trial_id", UUID.class), row.get("subject_id", UUID.class), row.get("campaign_key", String.class),
                    row.get("entitlement_grant_id", UUID.class), instant(row, "trial_expires_at"), row.get("grant_id", UUID.class),
                    row.get("grant_subject_id", UUID.class), row.get("source_type", String.class), row.get("entitlement_type", String.class),
                    row.get("granted_units", Long.class), instant(row, "grant_expires_at"), row.get("balance_id", UUID.class),
                    row.get("balance_grant_id", UUID.class), row.get("balance_granted_units", Long.class), row.get("reserved_units", Long.class),
                    row.get("consumed_units", Long.class), row.get("balance_status", String.class)));
        });
    }
    private static Instant instant(Record row, String field) {
        var timestamp = row.get(field, OffsetDateTime.class);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
