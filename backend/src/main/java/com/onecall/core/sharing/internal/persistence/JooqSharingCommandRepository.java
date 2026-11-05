package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus;
import com.onecall.core.sharing.internal.application.ShareSummary.VerificationMode;
import com.onecall.core.sharing.internal.command.ShareCommandCaller;
import com.onecall.core.sharing.internal.provider.ExtractionCodeMaterial;
import com.onecall.core.sharing.internal.command.ShareCommandException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class JooqSharingCommandRepository implements SharingCommandRepository {
    private static final String COLUMNS = "id, share_id, capability_token_hash, evidence_id, created_by_account_id, "
            + "owner_organization_id, verification_mode, code_salt, code_hash, code_generation, valid_from, "
            + "expires_at, max_admissions, admissions_used, download_allowed, content_snapshot, content_sha256, "
            + "status, revocation_generation, revision";
    private final DSLContext context;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public JooqSharingCommandRepository(DSLContext context) { this.context = Objects.requireNonNull(context); }

    @Override
    public void assertCommandRole(DSLContext transactionDsl) { role(transactionDsl, "sharing_command_role"); }

    @Override
    public void switchToEvidenceRole(DSLContext transactionDsl) { role(transactionDsl, "evidence_role"); }

    @Override
    public void restoreAndAssertCommandRole(DSLContext transactionDsl) { role(transactionDsl, "sharing_command_role"); }

    private void role(DSLContext transactionDsl, String role) {
        Objects.requireNonNull(transactionDsl);
        try {
            transactionDsl.execute("SET LOCAL ROLE " + role);
            Record current = transactionDsl.fetchOne("SELECT current_user");
            if (current == null || !role.equals(current.get(0, String.class))) throw unavailable();
        } catch (RuntimeException failure) { throw unavailable(); }
    }

    @Override
    public void acquireCreatorWorkspaceLocks(DSLContext transactionDsl, ShareCommandCaller caller) {
        try {
            caller.organizationId().ifPresent(organization -> {
                lock(transactionDsl, "organization_access.organization:" + organization);
                lock(transactionDsl, "organization_access.membership:" + organization + ":" + caller.accountId());
            });
            lock(transactionDsl, "customer_iam.account:" + caller.accountId());
        } catch (RuntimeException failure) { throw unavailable(); }
    }

    private void lock(DSLContext transactionDsl, String key) {
        transactionDsl.fetch("SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?, 0))", key);
    }

    @Override
    public Instant sampleDatabaseTime(DSLContext transactionDsl) {
        try {
            return Objects.requireNonNull(transactionDsl.fetchOne("SELECT clock_timestamp()"))
                    .get(0, OffsetDateTime.class).toInstant();
        } catch (RuntimeException failure) { throw unavailable(); }
    }

    @Override
    public Optional<ActiveCallerContext> readActiveCallerContext(DSLContext transactionDsl, ShareCommandCaller caller) {
        try {
            Record account = transactionDsl.fetchOne("SELECT id, status FROM customer_iam.customer_account WHERE id = ?", caller.accountId());
            if (account == null || !"ACTIVE".equals(account.get("status", String.class))) return Optional.empty();
            if (caller.organizationId().isEmpty()) return Optional.of(new ActiveCallerContext(caller.accountId(),
                    "ACTIVE", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
            UUID organization = caller.organizationId().orElseThrow();
            Record member = transactionDsl.fetchOne("SELECT o.status AS organization_status, m.role, m.status AS membership_status "
                    + "FROM organization_access.organization o JOIN organization_access.organization_membership m "
                    + "ON m.organization_id = o.id WHERE o.id = ? AND m.account_id = ? AND m.status <> 'REVOKED'", organization, caller.accountId());
            if (member == null || !"ACTIVE".equals(member.get("organization_status", String.class))
                    || !"ACTIVE".equals(member.get("membership_status", String.class))) return Optional.empty();
            String role = Objects.requireNonNull(member.get("role", String.class));
            if (!java.util.Set.of("ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR").contains(role)) return Optional.empty();
            return Optional.of(new ActiveCallerContext(caller.accountId(), "ACTIVE", caller.organizationId(),
                    Optional.of("ACTIVE"), Optional.of(role), Optional.of("ACTIVE")));
        } catch (RuntimeException failure) { throw unavailable(); }
    }

    @Override
    public Optional<EvidenceReference> readEvidenceReference(DSLContext transactionDsl, UUID evidenceId) {
        try {
            Record row = transactionDsl.fetchOne("SELECT id, subject_id, status, identity_at_capture FROM evidence.evidence_record WHERE id = ?", evidenceId);
            return row == null ? Optional.empty() : Optional.of(new EvidenceReference(row.get("id", UUID.class),
                    row.get("subject_id", UUID.class), row.get("status", String.class), row.get("identity_at_capture", JSONB.class)));
        } catch (RuntimeException failure) { throw unavailable(); }
    }

    @Override
    public Optional<CommandContext> readCommandContext(DSLContext transactionDsl, ShareCommandCaller caller, UUID evidenceId) {
        return readActiveCallerContext(transactionDsl, caller).flatMap(active -> readEvidenceReference(transactionDsl, evidenceId)
                .map(reference -> new CommandContext(active.accountId(), active.accountStatus(), active.organizationId(),
                        active.organizationStatus(), active.membershipRole(), active.membershipStatus(), reference)));
    }

    @Override
    public Optional<RiskOwnershipSnapshot> readRiskOwnership(DSLContext transactionDsl, EvidenceReference reference) {
        try {
            Record subject = transactionDsl.fetchOne("SELECT id, owner_account_id, owner_organization_id, revision "
                    + "FROM subject_number.business_subject WHERE id = ?", reference.subjectId());
            if (subject == null) return Optional.empty();
            Optional<UUID> owner = Optional.ofNullable(subject.get("owner_account_id", UUID.class));
            Optional<UUID> organization = Optional.ofNullable(subject.get("owner_organization_id", UUID.class));
            JsonNode identity = mapper.readTree(reference.identityAtCapture().data());
            if (!identity.isObject()) return Optional.empty();
            Optional<UUID> captured = Optional.empty();
            if (identity.has("bindingId")) {
                JsonNode binding = identity.get("bindingId");
                if (!binding.isString() || !binding.asString().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) return Optional.empty();
                captured = Optional.of(UUID.fromString(binding.asString()));
            }
            var current = transactionDsl.fetch("SELECT id, subject_id, status, verified_at, revision "
                    + "FROM subject_number.caller_number_binding WHERE subject_id = ? AND status = 'ACTIVE' AND verified_at IS NOT NULL", reference.subjectId());
            Optional<UUID> currentId = current.size() == 1 ? Optional.of(current.getFirst().get("id", UUID.class)) : Optional.empty();
            UUID resolved;
            String resolution;
            if (captured.isPresent()) {
                Record binding = transactionDsl.fetchOne("SELECT id, subject_id, status, verified_at, revision "
                        + "FROM subject_number.caller_number_binding WHERE id = ? AND subject_id = ?", captured.get(), reference.subjectId());
                if (binding == null) return Optional.empty();
                resolved = captured.get();
                resolution = "CAPTURED";
            } else {
                if (current.size() != 1) return Optional.empty();
                resolved = currentId.orElseThrow();
                resolution = "CURRENT";
            }
            boolean risk = !transactionDsl.fetch("SELECT caller_number_binding_id, risk_type, state, revision "
                    + "FROM subject_number.subject_risk_state WHERE caller_number_binding_id = ? AND state = 'OPEN'", resolved).isEmpty();
            return Optional.of(new RiskOwnershipSnapshot(reference.subjectId(), owner, organization,
                    owner.isPresent() || organization.isPresent(), captured, currentId, currentId.isPresent(), risk, resolution));
        } catch (RuntimeException failure) { return Optional.empty(); }
    }

    @Override
    public Optional<ShareGrant> findCreationIdentity(DSLContext transactionDsl, String shareId) {
        Record row = transactionDsl.fetchOne("SELECT " + COLUMNS + " FROM sharing.evidence_share_grant WHERE share_id = ?", shareId);
        return Optional.ofNullable(row).map(this::grant);
    }

    @Override
    public Optional<UUID> findGrantIdByTokenHash(DSLContext transactionDsl, String capabilityTokenHash) {
        Record row = transactionDsl.fetchOne("SELECT id FROM sharing.evidence_share_grant WHERE capability_token_hash = ?", capabilityTokenHash);
        return Optional.ofNullable(row).map(value -> value.get("id", UUID.class));
    }

    @Override
    public Optional<ShareGrant> insertGrantReturning(DSLContext transactionDsl, ShareGrant grant) {
        Record row = transactionDsl.fetchOne("INSERT INTO sharing.evidence_share_grant (" + COLUMNS + ") "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::timestamptz, ?::timestamptz, ?, ?, ?, ?::jsonb, ?, ?, ?, ?) "
                + "ON CONFLICT (share_id) DO NOTHING RETURNING " + COLUMNS,
                grant.id(), grant.shareId(), grant.capabilityTokenHash(), grant.evidenceId(), grant.createdByAccountId(),
                grant.grantOwnerOrganizationId(), grant.verificationMode().name(), grant.codeSalt(), grant.codeHash(), grant.codeGeneration(),
                grant.validFrom().atOffset(ZoneOffset.UTC), grant.expiresAt().atOffset(ZoneOffset.UTC), grant.maxAdmissions(),
                grant.admissionsUsed(), grant.downloadAllowed(), grant.contentSnapshot().data(), grant.contentSha256(),
                grant.status().name(), grant.revocationGeneration(), grant.revision());
        return Optional.ofNullable(row).map(this::grant);
    }

    private ShareGrant grant(Record row) {
        return new ShareGrant(row.get("id", UUID.class), row.get("share_id", String.class), row.get("capability_token_hash", String.class),
                row.get("evidence_id", UUID.class), row.get("created_by_account_id", UUID.class), row.get("owner_organization_id", UUID.class),
                VerificationMode.valueOf(row.get("verification_mode", String.class)), row.get("code_salt", String.class),
                row.get("code_hash", String.class), row.get("code_generation", Integer.class), row.get("valid_from", OffsetDateTime.class).toInstant(),
                row.get("expires_at", OffsetDateTime.class).toInstant(), row.get("max_admissions", Integer.class), row.get("admissions_used", Integer.class),
                row.get("download_allowed", Boolean.class), row.get("content_snapshot", JSONB.class), row.get("content_sha256", String.class),
                ShareStatus.valueOf(row.get("status", String.class)), row.get("revocation_generation", Integer.class), row.get("revision", Integer.class));
    }

    @Override
    public Optional<ShareGrant> lockGrant(DSLContext transactionDsl, String shareId, ShareCommandCaller caller) {
        try {
            return Optional.ofNullable(transactionDsl.fetchOne("SELECT " + COLUMNS
                    + " FROM sharing.evidence_share_grant WHERE share_id = ? AND created_by_account_id = ?"
                    + " AND owner_organization_id IS NOT DISTINCT FROM ?::uuid FOR UPDATE",
                    shareId, caller.accountId(), caller.organizationId().orElse(null))).map(this::grant);
        } catch (RuntimeException failure) { throw unavailable(); }
    }

    @Override
    public Optional<PersistedTransition> findTransition(DSLContext transactionDsl, UUID grantId, String eventId) {
        try {
            Record row = transactionDsl.fetchOne("SELECT share_grant_id, event_domain, event_id, event_type, result, occurred_at,"
                    + " payload_redacted, payload_sha256 FROM sharing.share_access_event"
                    + " WHERE share_grant_id = ? AND event_domain = ? AND event_id = ?", grantId, "SHARE_ACCESS", eventId);
            if (row == null) return Optional.empty();
            JsonNode payload = mapper.readTree(row.get("payload_redacted", JSONB.class).data());
            if (!"REVOKED".equals(row.get("event_type", String.class)) || !"SUCCESS".equals(row.get("result", String.class))
                    || !payload.isObject() || payload.size() != 7
                    || !"REVOKE".equals(payload.path("actionType").asString())
                    || !"REVOKED".equals(payload.path("status").asString())) throw unavailable();
            for (String key : java.util.List.of("actionRequestDigest", "actionFingerprint")) {
                if (!payload.path(key).isString() || !payload.path(key).asString().matches("[0-9a-f]{64}")) throw unavailable();
            }
            for (String key : java.util.List.of("codeGeneration", "revocationGeneration", "revision")) {
                JsonNode value = payload.path(key);
                if (!value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() < (key.equals("codeGeneration") ? 0 : 1))
                    throw unavailable();
            }
            String canonical = com.onecall.core.sharing.internal.commandservice.DefaultCustomerShareCommandService.canonical(payload);
            String digest = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            if (!digest.equals(row.get("payload_sha256", String.class))) throw unavailable();
            return Optional.of(new PersistedTransition(row.get("share_grant_id", UUID.class), row.get("event_id", String.class),
                    "REVOKE", payload.path("actionRequestDigest").asString(), payload.path("actionFingerprint").asString(),
                    ShareStatus.REVOKED, payload.path("codeGeneration").asInt(), payload.path("revocationGeneration").asInt(),
                    payload.path("revision").asInt(), row.get("occurred_at", OffsetDateTime.class).toInstant()));
        } catch (Exception failure) { throw unavailable(); }
    }

    @Override
    public void appendAccessEvent(DSLContext transactionDsl, AccessEvent event) {
        transactionDsl.execute("INSERT INTO sharing.share_access_event (id, share_grant_id, share_admission_id, event_domain,"
                + " event_id, event_type, result, occurred_at, risk_digest, payload_redacted, payload_sha256)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?::timestamptz, ?, ?::jsonb, ?)", event.id(), event.shareGrantId(),
                event.shareAdmissionId().orElse(null), event.eventDomain(), event.eventId(), event.eventType(), event.result(),
                event.occurredAt().atOffset(ZoneOffset.UTC), event.riskDigest().orElse(null), event.payloadRedacted().data(), event.payloadSha256());
    }

    @Override
    public TransitionResult transitionGrant(DSLContext transactionDsl, UUID grantId, int expectedRevision, ShareStatus expectedStatus,
            Optional<Integer> expectedRevocationGeneration, Optional<Integer> expectedCodeGeneration,
            ShareStatus targetStatus, int targetRevision, int targetCodeGeneration,
            int targetRevocationGeneration, Optional<ExtractionCodeMaterial> replacement) {
        Objects.requireNonNull(expectedRevocationGeneration);
        Objects.requireNonNull(expectedCodeGeneration);
        Objects.requireNonNull(replacement);
        if (targetStatus != ShareStatus.REVOKED || expectedCodeGeneration.isPresent() || replacement.isPresent()) throw unavailable();
        var bindings = new java.util.ArrayList<Object>(java.util.List.of(targetStatus.name(), targetRevocationGeneration,
                targetRevision, grantId, expectedRevision, expectedStatus.name()));
        String witness = expectedRevocationGeneration.map(value -> {
            bindings.add(value);
            return " AND revocation_generation = ?";
        }).orElse("");
        Record row = transactionDsl.fetchOne("UPDATE sharing.evidence_share_grant SET status = ?, revocation_generation = ?, revision = ?"
                + " WHERE id = ? AND revision = ? AND status = ?" + witness + " RETURNING " + COLUMNS, bindings.toArray());
        return row == null ? new TransitionResult.NotUpdated() : new TransitionResult.Updated(grant(row));
    }

    @Override
    public void appendOutbox(DSLContext transactionDsl, OutboxFact fact) {
        transactionDsl.fetchOne("SELECT platform.outbox_append(?::text, ?::text, ?::text, ?::uuid, ?::text, ?::jsonb, ?::text, ?::text)",
                fact.eventDomain(), fact.dedupeKey(), fact.aggregateType(), fact.aggregateId(), fact.eventType(),
                fact.payloadRedacted().map(JSONB::data).orElse(null), fact.payloadCiphertext().orElse(null), fact.payloadSha256());
    }

    @Override
    public void appendAudit(DSLContext transactionDsl, AuditFact fact) {
        transactionDsl.fetchOne("SELECT audit.append_fact(?::timestamptz, ?::text, ?::uuid, ?::text, ?::text, ?::uuid, ?::text, ?::jsonb, ?::jsonb, ?::text, ?::text)",
                fact.occurredAt().atOffset(ZoneOffset.UTC), fact.actorType(), fact.actorId().orElse(null), fact.actionCode(), fact.objectType(),
                fact.objectId().orElse(null), fact.reasonCode().orElse(null), fact.beforeSnapshot().map(JSONB::data).orElse(null),
                fact.afterSnapshot().map(JSONB::data).orElse(null), fact.requestId().orElse(null), fact.traceId().orElse(null));
    }

    private static ShareCommandException unavailable() {
        return new ShareCommandException(ShareCommandException.Code.SERVICE_UNAVAILABLE);
    }
}
