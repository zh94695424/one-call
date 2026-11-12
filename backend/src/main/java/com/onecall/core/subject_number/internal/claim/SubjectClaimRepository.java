package com.onecall.core.subject_number.internal.claim;

import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.ClaimStatus;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.VerificationMethod;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

public interface SubjectClaimRepository {
    void assertRole(DSLContext tx);
    Optional<BindingIdentity> findBindingIdentity(DSLContext tx, UUID bindingId);
    Optional<ClaimRow> findByRequestId(DSLContext tx, String requestId);
    Optional<ClaimRow> findAcceptedForAccount(DSLContext tx, UUID subjectId, UUID accountId);
    void lock(DSLContext tx, String namespaceKey);
    Optional<SubjectRow> lockSubject(DSLContext tx, UUID subjectId);
    Optional<BindingRow> lockBinding(DSLContext tx, UUID bindingId);
    List<RiskRow> lockRisks(DSLContext tx, UUID bindingId);
    Optional<AccountRow> readAccount(DSLContext tx, UUID accountId);
    List<AcceptedOwnerRow> readAcceptedOwners(DSLContext tx, UUID subjectId);
    Instant databaseTime(DSLContext tx);
    void insertClaim(DSLContext tx, NewClaim row);
    int setUnownedOwner(DSLContext tx, UUID subjectId, UUID accountId, int expectedRevision, Instant updatedAt);
    void appendAudit(DSLContext tx, AuditDecision decision);

    record BindingIdentity(UUID id, UUID subjectId, String numberHmac) {
        @Override public String toString() { return "BindingIdentity[REDACTED]"; }
    }
    record SubjectRow(UUID id, UUID ownerAccountId, UUID ownerOrganizationId, int revision) {
        @Override public String toString() { return "SubjectRow[REDACTED]"; }
    }
    record BindingRow(UUID id, UUID subjectId, String numberHmac, String status, Instant verifiedAt) {
        @Override public String toString() { return "BindingRow[REDACTED]"; }
    }
    record RiskRow(UUID id, String state) {
        @Override public String toString() { return "RiskRow[REDACTED]"; }
    }
    record AccountRow(UUID id, String status) {
        @Override public String toString() { return "AccountRow[REDACTED]"; }
    }
    record AcceptedOwnerRow(UUID id, UUID accountId, UUID organizationId) {
        @Override public String toString() { return "AcceptedOwnerRow[REDACTED]"; }
    }
    record ClaimRow(UUID id, UUID subjectId, UUID accountId, UUID organizationId,
                    VerificationMethod verificationMethod, String verificationRef, String requestId,
                    ClaimStatus status, Instant riskCheckedAt, Instant verifiedAt) {
        @Override public String toString() { return "ClaimRow[REDACTED]"; }
    }
    record NewClaim(UUID id, UUID subjectId, UUID accountId, UUID organizationId,
                    VerificationMethod verificationMethod, String verificationRef, String requestId,
                    ClaimStatus status, Instant riskCheckedAt, Instant verifiedAt) {
        @Override public String toString() { return "NewClaim[REDACTED]"; }
    }
    enum Reason {
        CLAIM_ACCEPTED, CLAIM_REPLAYED, CLAIM_DUPLICATE, CLAIM_RISK_BLOCKED, CLAIM_REPLAY_CONFLICT,
        CLAIM_CONTEXT_CONFLICT, CLAIM_OWNER_CONFLICT, CLAIM_SUBJECT_BINDING_MISMATCH,
        CLAIM_BINDING_NOT_ACTIVE, CLAIM_ACCOUNT_INACTIVE, CLAIM_REPLAY_UNRESOLVABLE, CLAIM_DATA_INTEGRITY
    }
    enum Outcome { ACCEPTED, REPLAYED, DUPLICATE, DENIED, CONFLICT }
    enum RiskEvaluation { NO_OPEN, OPEN, NOT_REQUIRED }
    record AuditDecision(Instant time, UUID accountId, UUID subjectId, String requestId, UUID traceId,
                         Reason reason, Outcome outcome, RiskEvaluation riskEvaluation) {
        @Override public String toString() { return "AuditDecision[REDACTED]"; }
    }
}
