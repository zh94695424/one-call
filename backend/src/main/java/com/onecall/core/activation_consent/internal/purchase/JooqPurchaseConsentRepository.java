package com.onecall.core.activation_consent.internal.purchase;

import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService.*;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class JooqPurchaseConsentRepository {
    private static final String COLUMNS = "id,subject_id,purpose,channel,event_domain,event_id,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,request_fingerprint,accepted_at,snapshot,snapshot_sha256";
    private final DataSource source;
    private final DSLContext dsl;
    private final ThreadLocal<LockedActivation> heldActivation = new ThreadLocal<>();
    JooqPurchaseConsentRepository(DataSource source, DSLContext dsl) { this.source = source; this.dsl = dsl; }
    Connection enter(TransactionWitness witness) {
        Connection connection = connection();
        assertWitness(witness, "catalog_order_role", connection);
        dsl.execute("SET LOCAL ROLE activation_consent_role");
        assertWitness(witness, "activation_consent_role", connection);
        return connection;
    }
    void restore(TransactionWitness witness, Connection connection) {
        assertWitness(witness, "activation_consent_role", connection);
        dsl.execute("SET LOCAL ROLE catalog_order_role");
        assertWitness(witness, "catalog_order_role", connection);
    }
    private Connection connection() {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !(TransactionSynchronizationManager.getResource(source) instanceof ConnectionHolder holder)) throw new Failure(Kind.UNAVAILABLE);
        Connection connection = holder.getConnection();
        try { if (connection.getAutoCommit() || connection.isReadOnly() || connection.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED) throw new Failure(Kind.UNAVAILABLE); }
        catch (java.sql.SQLException failure) { throw new Failure(Kind.UNAVAILABLE); }
        return connection;
    }
    private void assertWitness(TransactionWitness witness, String role, Connection connection) {
        if (witness == null || connection() != connection) throw new Failure(Kind.UNAVAILABLE);
        var row = dsl.fetchSingle("SELECT session_user AS login,current_user AS role,pg_backend_pid() AS backend,txid_current() AS transaction");
        if (!witness.login().equals(row.get("login", String.class)) || !role.equals(row.get("role", String.class))
                || witness.backendPid() != row.get("backend", Integer.class) || witness.transactionId() != row.get("transaction", Long.class)) throw new Failure(Kind.UNAVAILABLE);
    }
    LockedActivation lock(UUID subjectId, TransactionWitness witness) {
        Record row = dsl.fetchOne("SELECT id,subject_id,status,revision FROM activation_consent.service_activation WHERE subject_id=? FOR UPDATE", subjectId);
        LockedActivation locked = row == null ? new LockedActivation(subjectId, null, null, null, witness)
                : new LockedActivation(subjectId, row.get("id", UUID.class), row.get("status", String.class), row.get("revision", Integer.class), witness);
        heldActivation.set(locked);
        TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCompletion(int status) { heldActivation.remove(); }
        });
        return locked;
    }
    void verifyLock(LockedActivation locked, TransactionWitness witness) {
        if (locked == null || !locked.equals(heldActivation.get()) || !witness.equals(locked.witness()) || locked.activationId() == null || !"ACTIVE".equals(locked.status())) throw new Failure(Kind.REQUIRED);
        Record row = dsl.fetchOne("SELECT id,subject_id,status,revision FROM activation_consent.service_activation WHERE id=? AND subject_id=?", locked.activationId(), locked.subjectId());
        if (row == null || !"ACTIVE".equals(row.get("status", String.class)) || !locked.revision().equals(row.get("revision", Integer.class))) throw new Failure(Kind.UNAVAILABLE);
        Boolean held = dsl.fetchSingle("SELECT EXISTS(SELECT 1 FROM pg_catalog.pg_locks WHERE pid=pg_backend_pid() AND locktype='relation' AND relation='activation_consent.service_activation'::regclass AND mode='RowShareLock' AND granted)").get(0, Boolean.class);
        if (!Boolean.TRUE.equals(held)) throw new Failure(Kind.UNAVAILABLE);
    }
    Record event(String eventId) { return dsl.fetchOne("SELECT " + COLUMNS + " FROM activation_consent.consent_record WHERE event_domain='PURCHASE' AND event_id=?", eventId); }
    Record consent(UUID id) { return dsl.fetchOne("SELECT " + COLUMNS + " FROM activation_consent.consent_record WHERE id=?", id); }
    ConsentDescriptor describe(Record row, boolean appended) {
        if (row == null) throw new Failure(Kind.REQUIRED);
        String snapshot = row.get("snapshot", String.class), hash = row.get("snapshot_sha256", String.class);
        tools.jackson.databind.JsonNode node;
        try { node = PurchaseConsentSemantics.JSON.readTree(snapshot); }
        catch (RuntimeException failure) { throw new Failure(Kind.INTEGRITY); }
        if (!PurchaseConsentSemantics.sha(PurchaseConsentSemantics.canonical(node)).equals(hash) || !node.has("contentSha256")) throw new Failure(Kind.INTEGRITY);
        ConsentDescriptor descriptor = new ConsentDescriptor(row.get("id", UUID.class), row.get("subject_id", UUID.class), row.get("purpose", String.class), row.get("channel", String.class), row.get("event_domain", String.class), row.get("event_id", String.class),
                row.get("policy_code", String.class), row.get("policy_version", String.class), row.get("prompt_version", String.class), row.get("action", String.class), row.get("source_type", String.class), row.get("source_ref", String.class),
                row.get("account_id", UUID.class), row.get("request_fingerprint", String.class), node.get("contentSha256").asString(), hash, row.get("accepted_at", OffsetDateTime.class).toInstant(), appended);
        if ("PURCHASE".equals(descriptor.purpose()) && "PURCHASE".equals(descriptor.eventDomain()) && "ACCOUNT".equals(descriptor.sourceType()) && descriptor.accountId() != null) {
            PurchaseConsentInput original = new PurchaseConsentInput(descriptor.subjectId(), descriptor.accountId(), descriptor.channel(), descriptor.eventId(), descriptor.action(), descriptor.policyCode(), descriptor.policyVersion(), descriptor.promptVersion(), descriptor.contentSha256());
            var expected = (tools.jackson.databind.node.ObjectNode) PurchaseConsentSemantics.JSON.readTree(PurchaseConsentSemantics.snapshot(original)); expected.put("sourceRef", descriptor.sourceRef());
            if (!PurchaseConsentSemantics.fingerprint(original).equals(descriptor.requestFingerprint()) || !PurchaseConsentSemantics.sha(PurchaseConsentSemantics.canonical(expected)).equals(hash)) throw new Failure(Kind.INTEGRITY);
        }
        return descriptor;
    }
    ConsentDescriptor insert(PurchaseConsentInput input) {
        UUID id = UUID.randomUUID();
        String snapshot = PurchaseConsentSemantics.snapshot(input);
        Record row = dsl.fetchSingle("INSERT INTO activation_consent.consent_record (id,subject_id,purpose,channel,event_domain,event_id,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,snapshot,snapshot_sha256,request_fingerprint) VALUES (?,?,'PURCHASE',?,'PURCHASE',?,?,?,?,?,'ACCOUNT',?,?,?::jsonb,?,?) RETURNING " + COLUMNS,
                id, input.subjectId(), input.channel(), input.eventId(), input.policyCode(), input.policyVersion(), input.promptVersion(), input.action(), input.eventId(), input.accountId(), snapshot, PurchaseConsentSemantics.sha(snapshot), PurchaseConsentSemantics.fingerprint(input));
        return describe(row, true);
    }
    void fact(ConsentDescriptor consent, FactPhase phase, UUID traceId) {
        ConsentDescriptor stored = describe(consent(consent.id()), consent.newlyAppended());
        if (!stored.equals(consent)) throw new Failure(Kind.INTEGRITY);
        if (!consent.newlyAppended()) return;
        String payload = PurchaseConsentSemantics.payload(consent);
        if (phase == FactPhase.OUTBOX) dsl.fetch("SELECT platform.outbox_append('PURCHASE',?,'CONSENT_RECORD',?,'PURCHASE_CONSENT_RECORDED',?::jsonb,NULL,?)", "consent_record:" + consent.id(), consent.id(), payload, PurchaseConsentSemantics.sha(payload));
        else dsl.fetch("SELECT audit.append_fact(?::timestamptz,'ACCOUNT',?,'PURCHASE_CONSENT_RECORDED','CONSENT_RECORD',?,NULL,NULL,?::jsonb,NULL,?)", consent.acceptedAt().atOffset(java.time.ZoneOffset.UTC), consent.accountId(), consent.id(), payload, traceId.toString());
    }
}
