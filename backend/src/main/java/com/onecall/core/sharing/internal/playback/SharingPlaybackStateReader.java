package com.onecall.core.sharing.internal.playback;

import static com.onecall.core.sharing.internal.playback.MediaPlaybackService.Reason.*;

import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository;
import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository.*;
import com.onecall.core.sharing.internal.admission.ShareAdmissionSha256Provider;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class SharingPlaybackStateReader implements PlaybackStateReader {
    private final TransactionTemplate reads;
    private final ShareAdmissionRepository repository;
    private final ShareAdmissionSha256Provider sha256;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public SharingPlaybackStateReader(TransactionTemplate admissionTransactions,
            ShareAdmissionRepository repository, ShareAdmissionSha256Provider sha256) {
        if (admissionTransactions == null || admissionTransactions.getTransactionManager() == null
                || repository == null || sha256 == null) throw new IllegalArgumentException("Invalid playback dependencies");
        this.reads = new TransactionTemplate(admissionTransactions.getTransactionManager(), admissionTransactions);
        this.reads.setReadOnly(true);
        this.reads.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.reads.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.repository = repository;
        this.sha256 = sha256;
    }

    @Override public Result check(VerifiedMediaClaim claim) {
        if (!MediaPlaybackService.validShape(claim)) return denied(MALFORMED_CLAIM);
        try {
            Result result = reads.execute(status -> snapshot(claim));
            return result == null ? new Result.Unavailable() : result;
        } catch (RuntimeException failure) { return new Result.Unavailable(); }
    }

    private Result snapshot(VerifiedMediaClaim claim) {
        GrantSnapshot grant = repository.findGrantById(claim.shareGrantId()).orElse(null);
        if (grant == null || !claim.shareGrantId().equals(grant.id())) return denied(ADMISSION_MISMATCH);
        AdmissionRow admission = repository.findAdmissionById(grant.id(), claim.shareAdmissionId()).orElse(null);
        if (admission == null || !claim.shareAdmissionId().equals(admission.id()) || !grant.id().equals(admission.grantId()))
            return denied(ADMISSION_MISMATCH);
        if (!claim.evidenceId().equals(grant.evidenceId())) return denied(SCOPE_MISMATCH);
        Instant now = repository.now();
        Result grantTime = grantTime(grant, now);
        if (grantTime != null) return grantTime;
        AdmissionWitness witness = repository.findAdmissionWitness(grant.id(), admission.id()).orElse(null);
        if (witness == null) return denied(ADMISSION_MISMATCH);
        if (!validWitness(grant, admission, witness)) return new Result.Unavailable();
        if (admission.admittedAt() == null || !admission.admittedAt().isBefore(witness.sessionExpiresAt())
                || witness.sessionExpiresAt().isAfter(witness.admissionExpiresAt())
                || witness.admissionExpiresAt().isAfter(witness.grantExpiresAt())
                || witness.grantExpiresAt().isAfter(grant.expiresAt())) return new Result.Unavailable();
        if (claim.issuedAt().isBefore(admission.admittedAt()) || claim.notBefore().isBefore(grant.validFrom())
                || claim.expiresAt().isAfter(grant.expiresAt()) || claim.expiresAt().isAfter(witness.grantExpiresAt())
                || claim.expiresAt().isAfter(witness.admissionExpiresAt()) || claim.expiresAt().isAfter(witness.sessionExpiresAt()))
            return denied(SCOPE_MISMATCH);
        Result bounds = frozenTime(claim, witness, now);
        if (bounds != null) return bounds;
        if (claim.revocationGeneration() != grant.revocationGeneration()
                || claim.revocationGeneration() != witness.revocationGeneration()
                || claim.codeGeneration() != grant.codeGeneration() || claim.codeGeneration() != witness.codeGeneration())
            return denied(STALE_GENERATION);
        if ("ORGANIZATION_MEMBER".equals(witness.verificationMode())) {
            UUID accountId = witness.recipientAccountId();
            UUID organizationId = witness.recipientOrganizationId();
            if (!organizationId.equals(grant.ownerOrganizationId())) return denied(CURRENT_STATE_DENIED);
            OrganizationState organization = repository.readCurrentOrganization(organizationId).orElse(null);
            MembershipState membership = repository.readCurrentMembership(organizationId, accountId).orElse(null);
            AccountState account = repository.readCurrentAccount(accountId).orElse(null);
            if (organization == null || membership == null || account == null || !organizationId.equals(organization.id())
                    || !organizationId.equals(membership.organizationId()) || !accountId.equals(membership.accountId())
                    || !accountId.equals(account.id()) || !"ACTIVE".equals(organization.status())
                    || !"ACTIVE".equals(membership.status()) || !"ACTIVE".equals(account.status())) return denied(CURRENT_STATE_DENIED);
        }
        if (grant.identityAtCaptureMalformed() || grant.subjectId() == null) return new Result.Unavailable();
        SubjectOwnership ownership = repository.readSubjectOwnership(grant.subjectId()).orElse(null);
        if (ownership == null || !grant.subjectId().equals(ownership.subjectId())
                || ownership.ownerAccountId() == null && ownership.ownerOrganizationId() == null
                || !Objects.equals(ownership.ownerOrganizationId(), grant.ownerOrganizationId())
                || grant.ownerOrganizationId() == null && (grant.createdByAccountId() == null
                    || !grant.createdByAccountId().equals(ownership.ownerAccountId()))) return new Result.Unavailable();
        List<BindingState> current = repository.readCurrentBindings(grant.subjectId());
        if (current == null || current.stream().anyMatch(Objects::isNull)) return new Result.Unavailable();
        List<BindingState> selected = grant.capturedBindingId() == null ? current
                : repository.readCapturedBinding(grant.capturedBindingId()).map(List::of).orElse(List.of());
        if (selected.size() != 1) return denied(CURRENT_STATE_DENIED);
        BindingState binding = selected.getFirst();
        if (binding.id() == null || !grant.subjectId().equals(binding.subjectId()) || !"ACTIVE".equals(binding.status())
                || binding.verifiedAt() == null || grant.capturedBindingId() != null && !grant.capturedBindingId().equals(binding.id())
                || current.stream().noneMatch(active -> binding.id().equals(active.id())
                    && grant.subjectId().equals(active.subjectId()) && "ACTIVE".equals(active.status()) && active.verifiedAt() != null))
            return denied(CURRENT_STATE_DENIED);
        RiskState risk = repository.readSubjectRisk(grant.subjectId(), List.of(binding.id())).orElse(null);
        if (risk == null || risk.rows().isEmpty()) return denied(CURRENT_STATE_DENIED);
        if (!grant.subjectId().equals(risk.subjectId()) || !risk.bindingIds().equals(List.of(binding.id()))
                || risk.rows().stream().anyMatch(row -> row == null || row.id() == null || row.type() == null
                    || !Set.of("OPEN", "CLEARED").contains(row.state()))) return new Result.Unavailable();
        if (risk.anyOpen()) return denied(CURRENT_STATE_DENIED);
        Instant finalNow = repository.now();
        grantTime = grantTime(grant, finalNow);
        if (grantTime != null) return grantTime;
        bounds = frozenTime(claim, witness, finalNow);
        return bounds == null ? new Result.Allowed(finalNow) : bounds;
    }

    private Result grantTime(GrantSnapshot grant, Instant now) {
        if (now == null) return new Result.Unavailable();
        if ("REVOKED".equals(grant.status())) return denied(REVOKED);
        if ("EXPIRED".equals(grant.status())) return denied(EXPIRED);
        if (grant.validFrom() == null || grant.expiresAt() == null || !grant.validFrom().isBefore(grant.expiresAt()))
            return new Result.Unavailable();
        if (!now.isBefore(grant.expiresAt())) return denied(EXPIRED);
        if (now.isBefore(grant.validFrom())) return denied(NOT_YET_VALID);
        if (!"ACTIVE".equals(grant.status()) && !"LOCKED".equals(grant.status())) return denied(CURRENT_STATE_DENIED);
        return null;
    }

    private Result frozenTime(VerifiedMediaClaim claim, AdmissionWitness witness, Instant now) {
        if (!now.isBefore(witness.sessionExpiresAt()) || !now.isBefore(witness.admissionExpiresAt())) return denied(EXPIRED);
        var reason = MediaPlaybackService.timeFailure(claim, now);
        return reason == null ? null : denied(reason);
    }

    private boolean validWitness(GrantSnapshot grant, AdmissionRow admission, AdmissionWitness witness) {
        String mode = witness.verificationMode();
        if (mode == null || !Set.of("NONE", "EXTRACTION_CODE", "DESIGNATED_MOBILE", "ORGANIZATION_MEMBER").contains(mode)
                || !mode.equals(grant.verificationMode()) || !mode.equals(admission.verificationMode())
                || !("NONE".equals(mode) ? "NOT_REQUIRED" : "VERIFIED").equals(admission.verificationResult())
                || !("admission-granted:" + grant.id() + ":" + admission.id()).equals(witness.eventId())
                || witness.payload() == null || !witness.payload().isObject()
                || witness.payloadSha256() == null || !witness.payloadSha256().matches("[0-9a-f]{64}")
                || witness.codeGeneration() < 0 || witness.revocationGeneration() < 0
                || !positive(witness.grantExpiresAt()) || !positive(witness.admissionExpiresAt()) || !positive(witness.sessionExpiresAt()))
            return false;
        Set<String> keys = new HashSet<>(Set.of("verificationMode", "codeGeneration", "revocationGeneration",
                "grantExpiresAt", "admissionExpiresAt", "sessionExpiresAt"));
        boolean member = "ORGANIZATION_MEMBER".equals(mode);
        if (member) keys.addAll(Set.of("recipientAccountId", "recipientOrganizationId"));
        Set<String> actual = new HashSet<>();
        witness.payload().propertyNames().forEach(actual::add);
        if (!actual.equals(keys) || !text(witness.payload(), "verificationMode", mode)
                || !generation(witness.payload(), "codeGeneration", witness.codeGeneration())
                || !generation(witness.payload(), "revocationGeneration", witness.revocationGeneration())
                || !time(witness.payload(), "grantExpiresAt", witness.grantExpiresAt())
                || !time(witness.payload(), "admissionExpiresAt", witness.admissionExpiresAt())
                || !time(witness.payload(), "sessionExpiresAt", witness.sessionExpiresAt())) return false;
        if (member ? !recipient(witness.payload(), "recipientAccountId", witness.recipientAccountId())
                || !recipient(witness.payload(), "recipientOrganizationId", witness.recipientOrganizationId())
                : witness.recipientAccountId() != null || witness.recipientOrganizationId() != null) return false;
        String digest = sha256.digest(mapper.writeValueAsString(canonical(witness.payload())).getBytes(StandardCharsets.UTF_8));
        return digest != null && digest.matches("[0-9a-f]{64}") && digest.equals(witness.payloadSha256());
    }

    private boolean positive(Instant value) { return value != null && value.isAfter(Instant.EPOCH); }
    private boolean text(JsonNode payload, String key, String expected) {
        JsonNode value = payload.path(key);
        return value.isString() && expected.equals(value.asString());
    }
    private boolean generation(JsonNode payload, String key, int expected) {
        JsonNode value = payload.path(key);
        return value.isIntegralNumber() && value.canConvertToInt() && value.asInt() >= 0 && value.asInt() == expected;
    }
    private boolean time(JsonNode payload, String key, Instant expected) {
        String utc = new java.time.format.DateTimeFormatterBuilder().appendInstant(expected.getNano() == 0 ? 0 : 6).toFormatter().format(expected);
        return text(payload, key, utc) && Instant.parse(payload.path(key).asString()).equals(expected);
    }
    private boolean recipient(JsonNode payload, String key, UUID expected) {
        return expected != null && text(payload, key, expected.toString()) && UUID.fromString(payload.path(key).asString()).equals(expected);
    }
    private Object canonical(JsonNode value) {
        if (value.isObject()) {
            Map<String, Object> sorted = new TreeMap<>();
            value.properties().forEach(entry -> sorted.put(entry.getKey(), canonical(entry.getValue())));
            return sorted;
        }
        if (value.isArray()) {
            List<Object> values = new ArrayList<>();
            value.forEach(item -> values.add(canonical(item)));
            return values;
        }
        return value;
    }
    private static Result denied(MediaPlaybackService.Reason reason) { return new Result.Denied(reason); }
}
