package com.onecall.core.sharing.internal.admission;

import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class JooqShareAdmissionRepository implements ShareAdmissionRepository {
    private final DSLContext database;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private static final String GRANT_COLUMNS = "grant_row.id,grant_row.share_id,grant_row.evidence_id,grant_row.created_by_account_id,grant_row.owner_organization_id,grant_row.status,"
            + "grant_row.verification_mode,grant_row.valid_from,grant_row.expires_at,grant_row.code_generation,grant_row.revocation_generation,"
            + "grant_row.revision,grant_row.max_admissions,grant_row.admissions_used,grant_row.code_salt,grant_row.code_hash,grant_row.content_snapshot,"
            + "evidence_row.subject_id,evidence_row.identity_at_capture";

    public JooqShareAdmissionRepository(DSLContext database) { this.database = database; }

    @Override public Optional<GrantSnapshot> findGrantByTokenDigest(String digest) {
        return grant(database.fetchOne("SELECT " + GRANT_COLUMNS + " FROM sharing.evidence_share_grant grant_row "
                + "JOIN evidence.evidence_record evidence_row ON evidence_row.id=grant_row.evidence_id WHERE grant_row.capability_token_hash=?", digest));
    }
    @Override public Optional<GrantSnapshot> lockGrant(UUID grantId) {
        principal();
        return grant(database.fetchOne("SELECT " + GRANT_COLUMNS + " FROM sharing.evidence_share_grant grant_row "
                + "JOIN evidence.evidence_record evidence_row ON evidence_row.id=grant_row.evidence_id WHERE grant_row.id=? FOR UPDATE OF grant_row", grantId));
    }
    private void principal() {
        Record identity = database.fetchOne("SELECT current_user,session_user");
        if (identity == null || !"sharing_admission_role".equals(identity.get(0, String.class))
                || !"sharing_admission_role".equals(identity.get(1, String.class))) throw unavailable();
    }
    private Optional<GrantSnapshot> grant(Record row) {
        if (row == null) return Optional.empty();
        JsonNode identity = json(row, "identity_at_capture");
        return Optional.of(new GrantSnapshot(row.get("id", UUID.class), row.get("share_id", String.class),
                row.get("evidence_id", UUID.class), row.get("created_by_account_id", UUID.class), row.get("owner_organization_id", UUID.class),
                row.get("status", String.class), row.get("verification_mode", String.class), time(row, "valid_from"),
                time(row, "expires_at"), row.get("code_generation", Integer.class), row.get("revocation_generation", Integer.class),
                row.get("revision", Long.class), row.get("max_admissions", Long.class), row.get("admissions_used", Long.class),
                row.get("code_salt", String.class), row.get("code_hash", String.class), row.get("subject_id", UUID.class),
                identity != null && identity.isObject() && identity.has("bindingId") ? UUID.fromString(identity.path("bindingId").asString()) : null,
                identity != null && !identity.isObject(),
                json(row, "content_snapshot")));
    }
    @Override public void acquireOrganizationLifecycleLock(UUID organizationId) { lock("organization_access.organization:" + organizationId); }
    @Override public void acquireMembershipLifecycleLock(UUID organizationId, UUID accountId) { lock("organization_access.membership:" + organizationId + ":" + accountId); }
    @Override public void acquireAccountLifecycleLock(UUID accountId) { lock("customer_iam.account:" + accountId); }
    private void lock(String key) { database.fetch("SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))", key); }
    @Override public Optional<OrganizationState> readCurrentOrganization(UUID organizationId) {
        return Optional.ofNullable(database.fetchOne("SELECT id,status FROM organization_access.organization WHERE id=?", organizationId))
                .map(row -> new OrganizationState(row.get("id", UUID.class), row.get("status", String.class)));
    }
    @Override public Optional<MembershipState> readCurrentMembership(UUID organizationId, UUID accountId) {
        var rows = database.fetch("SELECT organization_id,account_id,status FROM organization_access.organization_membership WHERE organization_id=? AND account_id=? AND status <> 'REVOKED'", organizationId, accountId);
        return rows.size() != 1 ? Optional.empty() : Optional.of(new MembershipState(organizationId, accountId, rows.getFirst().get("status", String.class)));
    }
    @Override public Optional<AccountState> readCurrentAccount(UUID accountId) {
        return Optional.ofNullable(database.fetchOne("SELECT id,status FROM customer_iam.customer_account WHERE id=?", accountId))
                .map(row -> new AccountState(row.get("id", UUID.class), row.get("status", String.class)));
    }
    @Override public Optional<SubjectOwnership> readSubjectOwnership(UUID subjectId) {
        return Optional.ofNullable(database.fetchOne("SELECT id,owner_account_id,owner_organization_id FROM subject_number.business_subject WHERE id=?", subjectId))
                .map(row -> new SubjectOwnership(subjectId, row.get("owner_account_id", UUID.class), row.get("owner_organization_id", UUID.class)));
    }
    private BindingState binding(Record row) {
        return new BindingState(row.get("id", UUID.class), row.get("subject_id", UUID.class), row.get("status", String.class), time(row, "verified_at"), row.get("revision", Long.class));
    }
    @Override public Optional<BindingState> readCapturedBinding(UUID bindingId) {
        return Optional.ofNullable(database.fetchOne("SELECT id,subject_id,status,verified_at,revision FROM subject_number.caller_number_binding WHERE id=?", bindingId)).map(this::binding);
    }
    @Override public List<BindingState> readCurrentBindings(UUID subjectId) {
        return List.copyOf(database.fetch("SELECT id,subject_id,status,verified_at,revision FROM subject_number.caller_number_binding WHERE subject_id=? AND status='ACTIVE' AND verified_at IS NOT NULL", subjectId).map(this::binding));
    }
    @Override public Optional<RiskState> readSubjectRisk(UUID subjectId, List<UUID> bindingIds) {
        if (bindingIds.isEmpty() || readSubjectOwnership(subjectId).isEmpty()) return Optional.empty();
        for (UUID bindingId : bindingIds) {
            if (readCapturedBinding(bindingId).filter(binding -> binding.subjectId().equals(subjectId) && binding.verifiedAt() != null).isEmpty()) return Optional.empty();
        }
        var rows = database.fetch("SELECT id,risk_type,state,revision FROM subject_number.subject_risk_state WHERE caller_number_binding_id = ANY(?::uuid[])", (Object) bindingIds.toArray(UUID[]::new));
        return Optional.of(new RiskState(subjectId, bindingIds, rows.map(row -> new RiskRow(row.get("id", UUID.class), row.get("risk_type", String.class), row.get("state", String.class), row.get("revision", Long.class)))));
    }
    @Override public Optional<AdmissionRow> findAdmission(UUID grantId, String admissionKey) {
        return Optional.ofNullable(database.fetchOne("SELECT id,share_grant_id,admission_key,admission_no,verification_mode,verification_result,admitted_at FROM sharing.share_admission WHERE share_grant_id=? AND admission_key=?", grantId, admissionKey)).map(this::admission);
    }
    @Override public Optional<AdmissionRow> findAdmissionById(UUID grantId, UUID admissionId) {
        return Optional.ofNullable(database.fetchOne("SELECT id,share_grant_id,admission_key,admission_no,verification_mode,verification_result,admitted_at FROM sharing.share_admission WHERE share_grant_id=? AND id=?", grantId, admissionId)).map(this::admission);
    }
    private AdmissionRow admission(Record row) {
        return new AdmissionRow(row.get("id", UUID.class), row.get("share_grant_id", UUID.class), row.get("admission_key", String.class), row.get("admission_no", Long.class), row.get("verification_mode", String.class), row.get("verification_result", String.class), time(row, "admitted_at"));
    }
    @Override public Optional<AdmissionWitness> findAdmissionWitness(UUID grantId, UUID admissionId) {
        return findAccessEvent(grantId, "admission-granted:" + grantId + ":" + admissionId).filter(event -> "ADMISSION_GRANTED".equals(event.eventType()) && "SUCCESS".equals(event.result())).map(event -> {
            JsonNode payload = event.payload();
            return new AdmissionWitness(event.eventId(), payload, event.payloadSha256(), payload.path("verificationMode").asString(),
                    generation(payload, "codeGeneration"), generation(payload, "revocationGeneration"),
                    instant(payload, "grantExpiresAt"), instant(payload, "admissionExpiresAt"),
                    instant(payload, "sessionExpiresAt"), uuid(payload, "recipientAccountId"), uuid(payload, "recipientOrganizationId"));
        });
    }
    private UUID uuid(JsonNode node, String key) { return node.has(key) ? UUID.fromString(node.path(key).asString()) : null; }
    private Instant instant(JsonNode node, String key) {
        String value = node.path(key).asString(null);
        if (value == null) return null;
        try { return Instant.parse(value); } catch (RuntimeException malformed) { return null; }
    }
    private static int generation(JsonNode payload, String key) {
        JsonNode value = payload.path(key);
        if (!value.isIntegralNumber() || !value.canConvertToInt()) throw unavailable();
        int generation = value.asInt();
        if (generation < 0) throw unavailable();
        return generation;
    }
    @Override public Optional<AccessEventRow> findAccessEvent(UUID grantId, String eventId) {
        return Optional.ofNullable(database.fetchOne("SELECT event_id,event_type,payload_redacted,payload_sha256,result FROM sharing.share_access_event WHERE share_grant_id=? AND event_domain='SHARE_ACCESS' AND event_id=?", grantId, eventId))
                .map(row -> new AccessEventRow(row.get("event_id", String.class), row.get("event_type", String.class), json(row, "payload_redacted"), row.get("payload_sha256", String.class), row.get("result", String.class)));
    }
    @Override public List<AccessEventRow> findAccessEventsByRequestKeyDigest(UUID grantId, UUID admissionId, String requestKeyDigest) {
        return database.fetch("SELECT event_id,event_type,payload_redacted,payload_sha256,result FROM sharing.share_access_event WHERE share_grant_id=? AND share_admission_id=? AND event_domain='SHARE_ACCESS' AND payload_redacted->>'requestKeyDigest'=? ORDER BY occurred_at,id", grantId, admissionId, requestKeyDigest)
                .map(row -> new AccessEventRow(row.get("event_id", String.class), row.get("event_type", String.class), json(row, "payload_redacted"), row.get("payload_sha256", String.class), row.get("result", String.class)));
    }
    @Override public EventWriteResult insertAccessEvent(AccessEventInsert event) {
        principal();
        int inserted = database.execute("INSERT INTO sharing.share_access_event (id,share_grant_id,share_admission_id,event_domain,event_id,event_type,result,occurred_at,risk_digest,payload_redacted,payload_sha256) VALUES (?,?,?,'SHARE_ACCESS',?,?,?,?::timestamptz,NULL,?::jsonb,?) ON CONFLICT DO NOTHING",
                UUID.randomUUID(), event.grantId(), event.admissionId(), event.eventId(), event.eventType(), event.result(), event.occurredAt().atOffset(ZoneOffset.UTC), mapper.writeValueAsString(event.payload()), event.payloadSha256());
        AccessEventRow first = findAccessEvent(event.grantId(), event.eventId()).orElseThrow(JooqShareAdmissionRepository::unavailable);
        boolean identical = first.eventType().equals(event.eventType()) && first.result().equals(event.result()) && first.payloadSha256().equals(event.payloadSha256());
        return new EventWriteResult(!identical ? EventWriteStatus.CONFLICT : inserted == 1 ? EventWriteStatus.INSERTED : EventWriteStatus.REUSED_IDENTICAL, first);
    }
    @Override public AdmissionRow insertAdmission(AdmissionInsert admission) {
        principal();
        database.execute("INSERT INTO sharing.share_admission (id,share_grant_id,admission_key,admission_no,verification_mode,verification_result,admitted_at) VALUES (?,?,?,?,?,?,?::timestamptz)", admission.id(), admission.grantId(), admission.admissionKey(), admission.admissionNo(), admission.verificationMode(), admission.verificationResult(), admission.admittedAt().atOffset(ZoneOffset.UTC));
        return new AdmissionRow(admission.id(), admission.grantId(), admission.admissionKey(), admission.admissionNo(), admission.verificationMode(), admission.verificationResult(), admission.admittedAt());
    }
    @Override public int updateGrantAdmissionCount(UUID grantId, long expectedRevision, long admissionNo) {
        principal();
        return database.execute("UPDATE sharing.evidence_share_grant SET admissions_used=?,revision=revision+1 WHERE id=? AND revision=?", admissionNo, grantId, expectedRevision);
    }
    @Override public int expireGrant(UUID grantId, long expectedRevision) {
        principal();
        return database.execute("UPDATE sharing.evidence_share_grant SET status='EXPIRED',revocation_generation=revocation_generation+1,revision=revision+1 WHERE id=? AND revision=?", grantId, expectedRevision);
    }
    @Override public Instant now() { return database.fetchOne("SELECT clock_timestamp()").get(0, OffsetDateTime.class).toInstant(); }
    private Instant time(Record row, String column) { OffsetDateTime value = row.get(column, OffsetDateTime.class); return value == null ? null : value.toInstant(); }
    private JsonNode json(Record row, String column) { return mapper.readTree(row.get(column, JSONB.class).data()); }
    private static ShareAdmissionException unavailable() { return new ShareAdmissionException(ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE); }
}
