package com.onecall.core.activation_consent.internal.command;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.ActivationCaller;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;

public final class JooqSubjectActivationRepository implements SubjectActivationRepository {
    private static final String TRANSITION_COLUMNS = "id,activation_id,subject_id,from_status,to_status,from_revision,to_revision,source_channel,source_action_domain,source_action_event_id,status_event_domain,status_event_id,request_fingerprint,actor_account_id,actor_employee_id,reason_code,consent_record_id,occurred_at";
    @Override public void enter(DSLContext context) { context.execute("SET LOCAL ROLE activation_consent_role"); assertRole(context); }
    @Override public void assertRole(DSLContext context) {
        if (!"activation_consent_role".equals(context.fetchOne("SELECT current_user").get(0, String.class))) throw new SubjectActivationException(Code.SERVICE_UNAVAILABLE);
    }
    @Override public void lock(DSLContext context, String key) { context.fetch("SELECT pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))", key); }
    @Override public Optional<Head> lockHead(DSLContext context, UUID subjectId) {
        return context.fetchOptional("SELECT id,subject_id,status,revision FROM activation_consent.service_activation WHERE subject_id=? FOR UPDATE", subjectId)
                .map(row -> new Head(row.get("id", UUID.class), row.get("subject_id", UUID.class), row.get("status", String.class), row.get("revision", Integer.class)));
    }
    @Override public Optional<Subject> subject(DSLContext context, UUID subjectId) {
        return context.fetchOptional("SELECT id,owner_account_id,owner_organization_id,revision FROM subject_number.business_subject WHERE id=?", subjectId)
                .map(row -> new Subject(row.get("id", UUID.class), row.get("owner_account_id", UUID.class), row.get("owner_organization_id", UUID.class), row.get("revision", Integer.class)));
    }
    @Override public Access access(DSLContext context, ActivationCaller caller) {
        String account = context.fetchOptional("SELECT status FROM customer_iam.customer_account WHERE id=?", caller.accountId()).map(row -> row.get("status", String.class)).orElse(null);
        if (caller.organizationId() == null) return new Access(account, null, null, null);
        String organization = context.fetchOptional("SELECT status FROM organization_access.organization WHERE id=?", caller.organizationId()).map(row -> row.get("status", String.class)).orElse(null);
        var membership = context.fetchOptional("SELECT status,role FROM organization_access.organization_membership WHERE organization_id=? AND account_id=? AND status<>'REVOKED'", caller.organizationId(), caller.accountId());
        return new Access(account, organization, membership.map(row -> row.get("status", String.class)).orElse(null), membership.map(row -> row.get("role", String.class)).orElse(null));
    }
    @Override public Optional<Consent> consent(DSLContext context, String eventId) {
        return context.fetchOptional("SELECT id,subject_id,purpose,channel,event_domain,event_id,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,employee_id,ivr_session_id,snapshot,snapshot_sha256,request_fingerprint "
                + "FROM activation_consent.consent_record WHERE event_domain='ACTIVATION' AND event_id=?", eventId).map(row -> new Consent(row.get("id", UUID.class), row.get("subject_id", UUID.class),
                        row.get("purpose", String.class), row.get("channel", String.class), row.get("event_domain", String.class), row.get("event_id", String.class), row.get("policy_code", String.class),
                        row.get("policy_version", String.class), row.get("prompt_version", String.class), row.get("action", String.class), row.get("source_type", String.class), row.get("source_ref", String.class),
                        row.get("account_id", UUID.class), row.get("employee_id", UUID.class), row.get("ivr_session_id", UUID.class), row.get("snapshot", String.class), row.get("snapshot_sha256", String.class), row.get("request_fingerprint", String.class)));
    }
    @Override public Optional<Transition> actionTransition(DSLContext context, String eventId) {
        return context.fetchOptional("SELECT " + TRANSITION_COLUMNS + " FROM activation_consent.service_activation_transition WHERE source_action_domain='ACTIVATION' AND source_action_event_id=?", eventId).map(this::transitionRow);
    }
    @Override public Optional<Transition> transition(DSLContext context, UUID transitionId) {
        return context.fetchOptional("SELECT " + TRANSITION_COLUMNS + " FROM activation_consent.service_activation_transition WHERE id=?", transitionId).map(this::transitionRow);
    }
    @Override public Optional<Transition> latestTransition(DSLContext context, Head head) {
        return context.fetchOptional("SELECT " + TRANSITION_COLUMNS + " FROM activation_consent.service_activation_transition WHERE activation_id=? AND to_revision=?", head.id(), head.revision()).map(this::transitionRow);
    }
    private Transition transitionRow(Record row) {
        return new Transition(row.get("id", UUID.class), row.get("activation_id", UUID.class), row.get("subject_id", UUID.class), row.get("from_status", String.class), row.get("to_status", String.class),
                row.get("from_revision", Integer.class), row.get("to_revision", Integer.class), row.get("source_channel", String.class), row.get("source_action_domain", String.class), row.get("source_action_event_id", String.class),
                row.get("status_event_domain", String.class), row.get("status_event_id", String.class), row.get("request_fingerprint", String.class), row.get("actor_account_id", UUID.class), row.get("actor_employee_id", UUID.class),
                row.get("reason_code", String.class), row.get("consent_record_id", UUID.class), row.get("occurred_at", OffsetDateTime.class).toInstant());
    }
    @Override public void appendConsent(DSLContext context, Consent consent) {
        assertRole(context);
        context.execute("INSERT INTO activation_consent.consent_record (id,subject_id,purpose,channel,event_domain,event_id,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,snapshot,snapshot_sha256,request_fingerprint) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?::jsonb,?,?)",
                consent.id(), consent.subjectId(), consent.purpose(), consent.channel(), consent.eventDomain(), consent.eventId(), consent.policyCode(), consent.policyVersion(), consent.promptVersion(), consent.action(),
                consent.sourceType(), consent.sourceRef(), consent.accountId(), consent.snapshot(), consent.snapshotSha256(), consent.fingerprint());
    }
    @Override public void createHead(DSLContext context, Head head, Instant occurredAt) {
        assertRole(context);
        context.execute("INSERT INTO activation_consent.service_activation (id,subject_id,status,revision,created_at,updated_at) VALUES (?,?,'INACTIVE',1,?::timestamptz,?::timestamptz)", head.id(), head.subjectId(), occurredAt.atOffset(ZoneOffset.UTC), occurredAt.atOffset(ZoneOffset.UTC));
    }
    @Override public boolean advance(DSLContext context, Head head, Instant occurredAt) {
        assertRole(context);
        return context.execute("UPDATE activation_consent.service_activation SET status='ACTIVE',revision=revision+1,updated_at=?::timestamptz WHERE id=? AND subject_id=? AND status='INACTIVE' AND revision=?",
                occurredAt.atOffset(ZoneOffset.UTC), head.id(), head.subjectId(), head.revision()) == 1;
    }
    @Override public void appendTransition(DSLContext context, Transition transition) {
        assertRole(context);
        context.execute("INSERT INTO activation_consent.service_activation_transition (" + TRANSITION_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?::timestamptz)", transition.id(), transition.activationId(), transition.subjectId(),
                transition.fromStatus(), transition.toStatus(), transition.fromRevision(), transition.toRevision(), transition.channel(), transition.actionDomain(), transition.actionEvent(), transition.statusDomain(), transition.statusEvent(),
                transition.fingerprint(), transition.accountId(), transition.employeeId(), transition.reason(), transition.consentId(), transition.occurredAt().atOffset(ZoneOffset.UTC));
    }
    @Override public void appendOutbox(DSLContext context, Transition transition) {
        assertRole(context);
        String payload = ActivationSemantics.canonical(ActivationSemantics.fields("activationId", transition.activationId(), "transitionId", transition.id(), "subjectId", transition.subjectId(), "fromStatus", transition.fromStatus(),
                "toStatus", transition.toStatus(), "fromRevision", transition.fromRevision(), "toRevision", transition.toRevision(), "sourceChannel", transition.channel(), "sourceActionDomain", transition.actionDomain(),
                "sourceActionEventId", transition.actionEvent(), "statusEventDomain", transition.statusDomain(), "statusEventId", transition.statusEvent(), "occurredAt", transition.occurredAt().toString()));
        context.fetch("SELECT platform.outbox_append('ACTIVATION_STATUS',?,'SERVICE_ACTIVATION',?,'ACTIVATION_TRANSITION_APPLIED',?::jsonb,NULL,?)", "activation_transition:" + transition.id(), transition.activationId(), payload, ActivationSemantics.sha(payload));
    }
}
