package com.onecall.core.subject_number.internal.claim;

import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.ClaimStatus;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.VerificationMethod;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;

public final class JooqSubjectClaimRepository implements SubjectClaimRepository {
    private static final String CLAIM_COLUMNS = "id,subject_id,account_id,organization_id,verification_method,verification_ref_ciphertext,request_id,status,risk_checked_at,verified_at";

    @Override public void assertRole(DSLContext tx) {
        tx.execute("SET LOCAL ROLE subject_number_role");
        if (!"subject_number_role".equals(tx.fetchOne("SELECT current_user").get(0, String.class)))
            throw new SubjectClaimException(SubjectClaimException.Code.SERVICE_UNAVAILABLE);
    }
    @Override public Optional<BindingIdentity> findBindingIdentity(DSLContext tx, UUID bindingId) {
        return tx.fetchOptional("SELECT id,subject_id,number_hmac FROM subject_number.caller_number_binding WHERE id=?", bindingId)
                .map(row -> new BindingIdentity(uuid(row, "id"), uuid(row, "subject_id"), row.get("number_hmac", String.class)));
    }
    @Override public Optional<ClaimRow> findByRequestId(DSLContext tx, String requestId) {
        return tx.fetchOptional("SELECT " + CLAIM_COLUMNS + " FROM subject_number.subject_claim WHERE request_id=?", requestId).map(this::claim);
    }
    @Override public Optional<ClaimRow> findAcceptedForAccount(DSLContext tx, UUID subjectId, UUID accountId) {
        return tx.fetchOptional("SELECT " + CLAIM_COLUMNS + " FROM subject_number.subject_claim WHERE subject_id=? AND account_id=? AND status='ACCEPTED'",
                subjectId, accountId).map(this::claim);
    }
    @Override public void lock(DSLContext tx, String namespaceKey) {
        tx.fetch("SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))", namespaceKey);
    }
    @Override public Optional<SubjectRow> lockSubject(DSLContext tx, UUID subjectId) {
        return tx.fetchOptional("SELECT id,owner_account_id,owner_organization_id,revision FROM subject_number.business_subject WHERE id=? FOR UPDATE", subjectId)
                .map(row -> new SubjectRow(uuid(row, "id"), uuid(row, "owner_account_id"), uuid(row, "owner_organization_id"), row.get("revision", Integer.class)));
    }
    @Override public Optional<BindingRow> lockBinding(DSLContext tx, UUID bindingId) {
        return tx.fetchOptional("SELECT id,subject_id,number_hmac,status,verified_at FROM subject_number.caller_number_binding WHERE id=? FOR SHARE", bindingId)
                .map(row -> new BindingRow(uuid(row, "id"), uuid(row, "subject_id"), row.get("number_hmac", String.class), row.get("status", String.class), instant(row, "verified_at")));
    }
    @Override public List<RiskRow> lockRisks(DSLContext tx, UUID bindingId) {
        return tx.fetch("SELECT id,state FROM subject_number.subject_risk_state WHERE caller_number_binding_id=? ORDER BY id FOR SHARE", bindingId)
                .map(row -> new RiskRow(uuid(row, "id"), row.get("state", String.class)));
    }
    @Override public Optional<AccountRow> readAccount(DSLContext tx, UUID accountId) {
        return tx.fetchOptional("SELECT id,status FROM customer_iam.customer_account WHERE id=?", accountId)
                .map(row -> new AccountRow(uuid(row, "id"), row.get("status", String.class)));
    }
    @Override public List<AcceptedOwnerRow> readAcceptedOwners(DSLContext tx, UUID subjectId) {
        return tx.fetch("SELECT id,account_id,organization_id FROM subject_number.subject_claim WHERE subject_id=? AND status='ACCEPTED' ORDER BY id", subjectId)
                .map(row -> new AcceptedOwnerRow(uuid(row, "id"), uuid(row, "account_id"), uuid(row, "organization_id")));
    }
    @Override public Instant databaseTime(DSLContext tx) {
        return tx.fetchOne("SELECT clock_timestamp() AS decision_time").get("decision_time", OffsetDateTime.class).toInstant();
    }
    @Override public void insertClaim(DSLContext tx, NewClaim row) {
        tx.execute("INSERT INTO subject_number.subject_claim (" + CLAIM_COLUMNS + ") VALUES (?,?,?,NULL,?,?,?,?,?::timestamptz,?::timestamptz)",
                row.id(), row.subjectId(), row.accountId(), row.verificationMethod().name(), row.verificationRef(), row.requestId(),
                row.status().name(), timestamp(row.riskCheckedAt()), timestamp(row.verifiedAt()));
    }
    @Override public int setUnownedOwner(DSLContext tx, UUID subjectId, UUID accountId, int expectedRevision, Instant updatedAt) {
        return tx.execute("UPDATE subject_number.business_subject SET owner_account_id=?,revision=revision+1,updated_at=?::timestamptz WHERE id=? AND owner_account_id IS NULL AND owner_organization_id IS NULL AND revision=?",
                accountId, timestamp(updatedAt), subjectId, expectedRevision);
    }
    @Override public void appendAudit(DSLContext tx, AuditDecision decision) {
        String snapshot = "{\"reason\":\"" + decision.reason().name() + "\",\"outcome\":\"" + decision.outcome().name()
                + "\",\"riskEvaluation\":\"" + decision.riskEvaluation().name() + "\"}";
        tx.fetch("SELECT audit.append_fact(?::timestamptz,'ACCOUNT',?::uuid,'SUBJECT_CLAIM_DECIDED','BUSINESS_SUBJECT',?::uuid,?::text,NULL::jsonb,?::jsonb,?::text,?::text)",
                timestamp(decision.time()), decision.accountId(), decision.subjectId(), decision.reason().name(), snapshot, decision.requestId(), decision.traceId().toString());
    }
    private ClaimRow claim(Record row) {
        return new ClaimRow(uuid(row, "id"), uuid(row, "subject_id"), uuid(row, "account_id"), uuid(row, "organization_id"),
                VerificationMethod.valueOf(row.get("verification_method", String.class)), row.get("verification_ref_ciphertext", String.class),
                row.get("request_id", String.class), ClaimStatus.valueOf(row.get("status", String.class)), instant(row, "risk_checked_at"), instant(row, "verified_at"));
    }
    private static UUID uuid(Record row, String column) { return row.get(column, UUID.class); }
    private static Instant instant(Record row, String column) {
        OffsetDateTime value = row.get(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
    private static OffsetDateTime timestamp(Instant value) { return value == null ? null : value.atOffset(ZoneOffset.UTC); }
}
