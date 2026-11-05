package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus;
import com.onecall.core.sharing.internal.application.ShareSummary.VerificationMode;
import com.onecall.core.sharing.internal.command.ShareCommandCaller;
import com.onecall.core.sharing.internal.provider.ExtractionCodeMaterial;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;

public interface SharingCommandRepository {
    void assertCommandRole(DSLContext transactionDsl);
    void switchToEvidenceRole(DSLContext transactionDsl);
    void restoreAndAssertCommandRole(DSLContext transactionDsl);
    void acquireCreatorWorkspaceLocks(DSLContext transactionDsl, ShareCommandCaller caller);
    Instant sampleDatabaseTime(DSLContext transactionDsl);
    Optional<ActiveCallerContext> readActiveCallerContext(DSLContext transactionDsl, ShareCommandCaller caller);
    Optional<EvidenceReference> readEvidenceReference(DSLContext transactionDsl, UUID evidenceId);
    Optional<CommandContext> readCommandContext(DSLContext transactionDsl, ShareCommandCaller caller, UUID evidenceId);
    Optional<RiskOwnershipSnapshot> readRiskOwnership(DSLContext transactionDsl, EvidenceReference evidenceReference);
    Optional<ShareGrant> findCreationIdentity(DSLContext transactionDsl, String shareId);
    Optional<UUID> findGrantIdByTokenHash(DSLContext transactionDsl, String capabilityTokenHash);
    Optional<ShareGrant> insertGrantReturning(DSLContext transactionDsl, ShareGrant grant);
    Optional<ShareGrant> lockGrant(DSLContext transactionDsl, String shareId, ShareCommandCaller caller);
    Optional<PersistedTransition> findTransition(DSLContext transactionDsl, UUID grantId, String eventId);
    void appendAccessEvent(DSLContext transactionDsl, AccessEvent event);
    TransitionResult transitionGrant(DSLContext transactionDsl, UUID grantId,
            int expectedRevision, ShareStatus expectedStatus,
            Optional<Integer> expectedRevocationGeneration, Optional<Integer> expectedCodeGeneration,
            ShareStatus targetStatus, int targetRevision, int targetCodeGeneration,
            int targetRevocationGeneration, Optional<ExtractionCodeMaterial> replacement);
    void appendOutbox(DSLContext transactionDsl, OutboxFact outbox);
    void appendAudit(DSLContext transactionDsl, AuditFact audit);

    record PersistedTransition(UUID shareGrantId, String eventId, String actionType,
            String actionRequestDigest, String actionFingerprint, ShareStatus status,
            int codeGeneration, int revocationGeneration, int revision, Instant occurredAt) {
        public PersistedTransition {
            Objects.requireNonNull(shareGrantId);
            Objects.requireNonNull(eventId);
            Objects.requireNonNull(actionType);
            Objects.requireNonNull(actionRequestDigest);
            Objects.requireNonNull(actionFingerprint);
            Objects.requireNonNull(status);
            Objects.requireNonNull(occurredAt);
        }
    }

    record AccessEvent(UUID id, UUID shareGrantId, Optional<UUID> shareAdmissionId,
            String eventDomain, String eventId, String eventType, String result,
            Instant occurredAt, Optional<String> riskDigest, JSONB payloadRedacted, String payloadSha256) {
        public AccessEvent {
            Objects.requireNonNull(id);
            Objects.requireNonNull(shareGrantId);
            Objects.requireNonNull(shareAdmissionId);
            Objects.requireNonNull(eventDomain);
            Objects.requireNonNull(eventId);
            Objects.requireNonNull(eventType);
            Objects.requireNonNull(result);
            Objects.requireNonNull(occurredAt);
            Objects.requireNonNull(riskDigest);
            Objects.requireNonNull(payloadRedacted);
            Objects.requireNonNull(payloadSha256);
        }
    }

    sealed interface TransitionResult {
        record Updated(ShareGrant current) implements TransitionResult {
            public Updated { Objects.requireNonNull(current); }
        }
        record NotUpdated() implements TransitionResult { }
    }

    record ShareGrant(UUID id, String shareId, String capabilityTokenHash, UUID evidenceId,
            UUID createdByAccountId, UUID grantOwnerOrganizationId, VerificationMode verificationMode,
            String codeSalt, String codeHash, int codeGeneration, Instant validFrom, Instant expiresAt,
            Integer maxAdmissions, int admissionsUsed, boolean downloadAllowed, JSONB contentSnapshot,
            String contentSha256, ShareStatus status, int revocationGeneration, int revision) {
        public ShareGrant {
            Objects.requireNonNull(id);
            Objects.requireNonNull(shareId);
            Objects.requireNonNull(capabilityTokenHash);
            Objects.requireNonNull(evidenceId);
            Objects.requireNonNull(createdByAccountId);
            Objects.requireNonNull(verificationMode);
            Objects.requireNonNull(validFrom);
            Objects.requireNonNull(expiresAt);
            Objects.requireNonNull(contentSnapshot);
            Objects.requireNonNull(contentSha256);
            Objects.requireNonNull(status);
        }
    }

    record ActiveCallerContext(UUID accountId, String accountStatus, Optional<UUID> organizationId,
            Optional<String> organizationStatus, Optional<String> membershipRole, Optional<String> membershipStatus) {
        public ActiveCallerContext {
            Objects.requireNonNull(accountId);
            Objects.requireNonNull(accountStatus);
            Objects.requireNonNull(organizationId);
            Objects.requireNonNull(organizationStatus);
            Objects.requireNonNull(membershipRole);
            Objects.requireNonNull(membershipStatus);
        }
    }

    record CommandContext(UUID accountId, String accountStatus, Optional<UUID> organizationId,
            Optional<String> organizationStatus, Optional<String> membershipRole, Optional<String> membershipStatus,
            EvidenceReference evidenceReference) {
        public CommandContext {
            Objects.requireNonNull(accountId);
            Objects.requireNonNull(accountStatus);
            Objects.requireNonNull(organizationId);
            Objects.requireNonNull(organizationStatus);
            Objects.requireNonNull(membershipRole);
            Objects.requireNonNull(membershipStatus);
            Objects.requireNonNull(evidenceReference);
        }
    }

    record EvidenceReference(UUID evidenceId, UUID subjectId, String evidenceStatus, JSONB identityAtCapture) {
        public EvidenceReference {
            Objects.requireNonNull(evidenceId);
            Objects.requireNonNull(subjectId);
            Objects.requireNonNull(evidenceStatus);
            Objects.requireNonNull(identityAtCapture);
        }
    }

    record RiskOwnershipSnapshot(UUID subjectId, Optional<UUID> ownerAccountId,
            Optional<UUID> subjectOwnerOrganizationId, boolean ownerResolved, Optional<UUID> capturedBindingId,
            Optional<UUID> currentBindingId, boolean currentBindingActiveVerified, boolean openRisk, String resolution) {
        public RiskOwnershipSnapshot {
            Objects.requireNonNull(subjectId);
            Objects.requireNonNull(ownerAccountId);
            Objects.requireNonNull(subjectOwnerOrganizationId);
            Objects.requireNonNull(capturedBindingId);
            Objects.requireNonNull(currentBindingId);
            Objects.requireNonNull(resolution);
        }
    }

    record AuditFact(Instant occurredAt, String actorType, Optional<UUID> actorId, String actionCode,
            String objectType, Optional<UUID> objectId, Optional<String> reasonCode,
            Optional<JSONB> beforeSnapshot, Optional<JSONB> afterSnapshot,
            Optional<String> requestId, Optional<String> traceId) {
        public AuditFact {
            Objects.requireNonNull(occurredAt);
            Objects.requireNonNull(actorType);
            Objects.requireNonNull(actorId);
            Objects.requireNonNull(actionCode);
            Objects.requireNonNull(objectType);
            Objects.requireNonNull(objectId);
            Objects.requireNonNull(reasonCode);
            Objects.requireNonNull(beforeSnapshot);
            Objects.requireNonNull(afterSnapshot);
            Objects.requireNonNull(requestId);
            Objects.requireNonNull(traceId);
        }
    }

    record OutboxFact(String eventDomain, String dedupeKey, String aggregateType, UUID aggregateId,
            String eventType, Optional<JSONB> payloadRedacted, Optional<String> payloadCiphertext, String payloadSha256) {
        public OutboxFact {
            Objects.requireNonNull(eventDomain);
            Objects.requireNonNull(dedupeKey);
            Objects.requireNonNull(aggregateType);
            Objects.requireNonNull(aggregateId);
            Objects.requireNonNull(eventType);
            Objects.requireNonNull(payloadRedacted);
            Objects.requireNonNull(payloadCiphertext);
            Objects.requireNonNull(payloadSha256);
        }
    }
}
