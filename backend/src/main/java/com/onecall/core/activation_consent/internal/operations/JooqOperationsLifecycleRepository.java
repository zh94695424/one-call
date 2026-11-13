package com.onecall.core.activation_consent.internal.operations;

import com.onecall.core.activation_consent.internal.command.ActivationSemantics;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Code;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Failure;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;

public final class JooqOperationsLifecycleRepository implements OperationsLifecycleRepository {
    private static final String COLUMNS = "id,activation_id,subject_id,from_status,to_status,from_revision,to_revision,source_channel,source_action_domain,source_action_event_id,status_event_domain,status_event_id,request_fingerprint,actor_account_id,actor_employee_id,reason_code,consent_record_id,occurred_at";
    @Override public void enter(DSLContext context) { context.execute("SET LOCAL ROLE activation_consent_role"); assertRole(context); }
    @Override public void assertRole(DSLContext context) { assertRole(context, "activation_consent_role"); }
    private void assertRole(DSLContext context, String expected) {
        var row = context.fetchOne("SELECT current_user");
        if (row == null || !expected.equals(row.get(0, String.class))) throw new Failure(Code.SERVICE_UNAVAILABLE);
    }
    @Override public void lockSubject(DSLContext context, UUID subjectId) {
        context.fetch("SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))", "subject_number.subject:" + subjectId);
    }
    @Override public Optional<Head> lockHead(DSLContext context, UUID subjectId) {
        return context.fetchOptional("SELECT id,subject_id,status,revision,created_at,updated_at FROM activation_consent.service_activation WHERE subject_id=? FOR UPDATE", subjectId)
                .map(row -> new Head(row.get("id", UUID.class), row.get("subject_id", UUID.class), row.get("status", String.class), integer(row, "revision"), instant(row, "created_at"), instant(row, "updated_at")));
    }
    @Override public boolean subjectExists(DSLContext context, UUID subjectId) { return context.fetchOptional("SELECT id FROM subject_number.business_subject WHERE id=?", subjectId).isPresent(); }
    @Override public Optional<Transition> actionTransition(DSLContext context, String eventId) {
        return context.fetchOptional("SELECT " + COLUMNS + " FROM activation_consent.service_activation_transition WHERE source_action_domain='OPERATIONS' AND source_action_event_id=?", eventId).map(this::transition);
    }
    @Override public Optional<Transition> latestTransition(DSLContext context, Head head) {
        return context.fetchOptional("SELECT " + COLUMNS + " FROM activation_consent.service_activation_transition WHERE activation_id=? AND to_revision=?", head.id(), head.revision()).map(this::transition);
    }
    private Transition transition(Record row) {
        return new Transition(row.get("id", UUID.class), row.get("activation_id", UUID.class), row.get("subject_id", UUID.class), row.get("from_status", String.class), row.get("to_status", String.class),
                integer(row, "from_revision"), integer(row, "to_revision"), row.get("source_channel", String.class), row.get("source_action_domain", String.class), row.get("source_action_event_id", String.class),
                row.get("status_event_domain", String.class), row.get("status_event_id", String.class), row.get("request_fingerprint", String.class), row.get("actor_account_id", UUID.class),
                row.get("actor_employee_id", UUID.class), row.get("reason_code", String.class), row.get("consent_record_id", UUID.class), instant(row, "occurred_at"));
    }
    private int integer(Record row, String column) { Integer value = row.get(column, Integer.class); if (value == null) throw new Failure(Code.SERVICE_UNAVAILABLE); return value; }
    private Instant instant(Record row, String column) { OffsetDateTime value = row.get(column, OffsetDateTime.class); if (value == null) throw new Failure(Code.SERVICE_UNAVAILABLE); return value.toInstant(); }
    @Override public Instant decisionTime(DSLContext context) {
        var row = context.fetchOne("SELECT clock_timestamp() AS decision_time");
        if (row == null) throw new Failure(Code.SERVICE_UNAVAILABLE);
        return instant(row, "decision_time");
    }
    @Override public int advance(DSLContext context, Head head, String target, Instant time) {
        assertRole(context);
        return context.execute("UPDATE activation_consent.service_activation SET status=?,revision=revision+1,updated_at=?::timestamptz WHERE id=? AND subject_id=? AND status=? AND revision=?",
                target, time.atOffset(ZoneOffset.UTC), head.id(), head.subjectId(), head.status(), head.revision());
    }
    @Override public void appendTransition(DSLContext context, Transition row) {
        assertRole(context);
        int inserted = context.execute("INSERT INTO activation_consent.service_activation_transition (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::timestamptz)",
                row.id(), row.activationId(), row.subjectId(), row.fromStatus(), row.toStatus(), row.fromRevision(), row.toRevision(), row.channel(), row.actionDomain(), row.actionEvent(),
                row.statusDomain(), row.statusEvent(), row.fingerprint(), row.accountId(), row.employeeId(), row.reason(), row.consentId(), row.occurredAt().atOffset(ZoneOffset.UTC));
        if (inserted != 1) throw new Failure(Code.SERVICE_UNAVAILABLE);
    }
    @Override public void appendOutbox(DSLContext context, Transition row) {
        assertRole(context);
        String payload = ActivationSemantics.canonical(ActivationSemantics.fields("activationId", row.activationId(), "transitionId", row.id(), "subjectId", row.subjectId(), "fromStatus", row.fromStatus(), "toStatus", row.toStatus(),
                "fromRevision", row.fromRevision(), "toRevision", row.toRevision(), "sourceChannel", row.channel(), "sourceActionDomain", row.actionDomain(), "sourceActionEventId", row.actionEvent(),
                "statusEventDomain", row.statusDomain(), "statusEventId", row.statusEvent(), "occurredAt", row.occurredAt().toString()));
        var result = context.fetchOne("SELECT platform.outbox_append('ACTIVATION_STATUS',?,'SERVICE_ACTIVATION',?,'ACTIVATION_TRANSITION_APPLIED',?::jsonb,NULL,?)", "activation_transition:" + row.id(), row.activationId(), payload, ActivationSemantics.sha(payload));
        if (result == null || result.get(0, UUID.class) == null) throw new Failure(Code.SERVICE_UNAVAILABLE);
    }
    @Override public void appendAudit(DSLContext context, Transition row) {
        assertRole(context);
        try {
            context.execute("SET LOCAL ROLE operations_service_role");
            assertRole(context, "operations_service_role");
            String before = ActivationSemantics.canonical(ActivationSemantics.fields("status", row.fromStatus(), "revision", row.fromRevision(), "reasonCode", row.reason()));
            String after = ActivationSemantics.canonical(ActivationSemantics.fields("status", row.toStatus(), "revision", row.toRevision(), "reasonCode", row.reason()));
            var result = context.fetchOne("SELECT audit.append_fact(?::timestamptz,'EMPLOYEE',?,?,'SUBJECT',?,?,?::jsonb,?::jsonb,?,?)", row.occurredAt().atOffset(ZoneOffset.UTC),
                    row.employeeId(), "SUSPENDED".equals(row.toStatus()) ? "SUBJECT_SUSPEND" : "SUBJECT_RESUME", row.subjectId(), row.reason(), before, after, row.statusEvent(), UUID.randomUUID().toString());
            if (result == null || result.get(0, UUID.class) == null) throw new Failure(Code.SERVICE_UNAVAILABLE);
        } finally {
            try { context.execute("SET LOCAL ROLE activation_consent_role"); assertRole(context); }
            catch (RuntimeException restoration) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
        }
    }
}
