package com.onecall.core.sharing.internal.admission;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public interface ShareAdmissionRepository {
    Optional<GrantSnapshot> findGrantByTokenDigest(String digest);
    Optional<GrantSnapshot> lockGrant(UUID grantId);
    void acquireOrganizationLifecycleLock(UUID organizationId);
    void acquireMembershipLifecycleLock(UUID organizationId, UUID accountId);
    void acquireAccountLifecycleLock(UUID accountId);
    Optional<OrganizationState> readCurrentOrganization(UUID organizationId);
    Optional<MembershipState> readCurrentMembership(UUID organizationId, UUID accountId);
    Optional<AccountState> readCurrentAccount(UUID accountId);
    Optional<SubjectOwnership> readSubjectOwnership(UUID subjectId);
    Optional<BindingState> readCapturedBinding(UUID bindingId);
    List<BindingState> readCurrentBindings(UUID subjectId);
    Optional<RiskState> readSubjectRisk(UUID subjectId, List<UUID> bindingIds);
    Optional<AdmissionRow> findAdmission(UUID grantId, String admissionKey);
    Optional<AdmissionWitness> findAdmissionWitness(UUID grantId, UUID admissionId);
    Optional<AccessEventRow> findAccessEvent(UUID grantId, String eventId);
    EventWriteResult insertAccessEvent(AccessEventInsert event);
    AdmissionRow insertAdmission(AdmissionInsert admission);
    int updateGrantAdmissionCount(UUID grantId, long expectedRevision, long admissionNo);
    int expireGrant(UUID grantId, long expectedRevision);
    Instant now();

    record GrantSnapshot(UUID id, String shareId, UUID evidenceId, UUID createdByAccountId, UUID ownerOrganizationId,
            String status, String verificationMode, Instant validFrom, Instant expiresAt,
            int codeGeneration, int revocationGeneration, long revision, Long maxAdmissions,
            long admissionsUsed, String codeSalt, String codeHash, UUID subjectId, UUID capturedBindingId,
            boolean identityAtCaptureMalformed, JsonNode contentSnapshot) {
        @Override public String toString() { return "[REDACTED]"; }
    }
    record OrganizationState(UUID id, String status) { }
    record MembershipState(UUID organizationId, UUID accountId, String status) { }
    record AccountState(UUID id, String status) { }
    record SubjectOwnership(UUID subjectId, UUID ownerAccountId, UUID ownerOrganizationId) { }
    record BindingState(UUID id, UUID subjectId, String status, Instant verifiedAt, long revision) { }
    record RiskRow(UUID id, String type, String state, long revision) { }
    record RiskState(UUID subjectId, List<UUID> bindingIds, List<RiskRow> rows) {
        public RiskState { bindingIds = List.copyOf(bindingIds); rows = List.copyOf(rows); }
        public boolean anyOpen() { return rows.stream().anyMatch(row -> "OPEN".equals(row.state())); }
    }
    record AdmissionRow(UUID id, UUID grantId, String admissionKey, long admissionNo,
            String verificationMode, String verificationResult, Instant admittedAt) {
        @Override public String toString() { return "[REDACTED]"; }
    }
    record AdmissionWitness(String eventId, JsonNode payload, String payloadSha256, String verificationMode,
            int codeGeneration, int revocationGeneration, Instant grantExpiresAt, Instant admissionExpiresAt,
            Instant sessionExpiresAt, UUID recipientAccountId, UUID recipientOrganizationId) {
        @Override public String toString() { return "[REDACTED]"; }
    }
    record AccessEventRow(String eventId, String eventType, JsonNode payload, String payloadSha256, String result) { }
    enum EventWriteStatus { INSERTED, REUSED_IDENTICAL, CONFLICT }
    record EventWriteResult(EventWriteStatus status, AccessEventRow first) { }
    record AccessEventInsert(UUID grantId, UUID admissionId, String eventId, String eventType,
            String result, Instant occurredAt, JsonNode payload, String payloadSha256) { }
    record AdmissionInsert(UUID id, UUID grantId, String admissionKey, long admissionNo,
            String verificationMode, String verificationResult, Instant admittedAt) { }
}
