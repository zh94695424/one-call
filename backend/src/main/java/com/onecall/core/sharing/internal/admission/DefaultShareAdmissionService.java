package com.onecall.core.sharing.internal.admission;

import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository.*;
import com.onecall.core.sharing.internal.publicadmission.*;
import static com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException.Code.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class DefaultShareAdmissionService implements ShareAdmissionService {
    private final TransactionTemplate writes;
    private final TransactionTemplate reads;
    private final TransactionTemplate observations;
    private final ShareAdmissionRepository repository;
    private final ShareAdmissionSha256Provider sha256;
    private final ShareAdmissionPolicyProvider policies;
    private final ShareAdmissionCodeVerifier codes;
    private final DesignatedMobileProofVerifier mobiles;
    private final ShareMediaCredentialProvider credentials;
    private final ShareAdmissionTicketProvider tickets;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public DefaultShareAdmissionService(TransactionTemplate writes, ShareAdmissionRepository repository,
            ShareAdmissionSha256Provider sha256, ShareAdmissionPolicyProvider policies, ShareAdmissionCodeVerifier codes,
            DesignatedMobileProofVerifier mobiles, ShareMediaCredentialProvider credentials, ShareAdmissionTicketProvider tickets) {
        this.writes = writes;
        this.reads = new TransactionTemplate(Objects.requireNonNull(writes.getTransactionManager()), writes);
        this.reads.setReadOnly(true);
        this.observations = new TransactionTemplate(Objects.requireNonNull(writes.getTransactionManager()), writes);
        this.observations.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.repository = repository;
        this.sha256 = sha256;
        this.policies = policies;
        this.codes = codes;
        this.mobiles = mobiles;
        this.credentials = credentials;
        this.tickets = tickets;
    }

    @Override public AdmissionOutcome admit(ShareAdmissionCapabilityToken token, ShareAdmissionRequest request,
            Optional<TrustedShareAdmissionCaller> caller) {
        try {
            validate(request);
            String digest = checkedDigest(token.utf8());
            Preliminary preliminary = reads.execute(status -> {
                GrantSnapshot grant = repository.findGrantByTokenDigest(digest).orElseThrow(() -> failure(SHARE_TOKEN_INVALID));
                String key = admissionKey(grant.id(), request.admissionRequestId());
                lifecycle(grant, caller);
                Optional<AdmissionRow> row = repository.findAdmission(grant.id(), key);
                row.ifPresent(admission -> repository.findAdmissionWitness(grant.id(), admission.id()));
                return new Preliminary(grant, key, row.isPresent());
            });
            ShareAdmissionPolicyProvider.ApprovedTicketPolicy policy = null;
            Proof proof = null;
            if (!preliminary.exists()) {
                outside();
                policy = policy(preliminary.grant(), true);
                proof = proof(preliminary.grant(), request, caller, policy);
            }
            var freshPolicy = policy;
            var freshProof = proof;
            Durable durable = writes.execute(status -> authoritative(preliminary.grant(), preliminary.key(), request,
                    caller, freshPolicy, freshProof));
            if (durable.denial() == SHARE_VERIFICATION_FAILED && freshProof != null && freshProof.denied()) {
                observation(preliminary.grant(), request, freshProof.reason());
            }
            if (durable.denial() != null) return new DeniedCommitted(durable.denial());
            outside();
            var context = ticketContext(durable);
            if (context.durable().denial() != null) return new DeniedCommitted(context.durable().denial());
            if (durable.replay()) policy = policy(context.durable().grant(), false);
            return issue(context, Objects.requireNonNull(policy));
        } catch (ShareAdmissionException known) {
            return new DeniedCommitted(known.code());
        } catch (RuntimeException unexpected) {
            return new DeniedCommitted(DEPENDENCY_UNAVAILABLE);
        }
    }

    private Durable authoritative(GrantSnapshot original, String key, ShareAdmissionRequest request,
            Optional<TrustedShareAdmissionCaller> caller, ShareAdmissionPolicyProvider.ApprovedTicketPolicy policy, Proof proof) {
        lifecycle(original, caller);
        GrantSnapshot grant = repository.lockGrant(original.id()).orElseThrow(() -> failure(SHARE_TOKEN_INVALID));
        Instant now = repository.now();
        Facts facts = facts(grant, caller);
        Optional<AdmissionRow> existing = repository.findAdmission(grant.id(), key);
        AdmissionWitness witness = existing.flatMap(row -> repository.findAdmissionWitness(grant.id(), row.id())).orElse(null);
        var denial = predicates(grant, original, now, facts, caller, existing, witness);
        if (denial != null) return denied(denial);
        if (existing.isPresent()) {
            AdmissionRow row = existing.orElseThrow();
            validateWitness(grant, row, Objects.requireNonNull(witness));
            if (!now.isBefore(witness.sessionExpiresAt()) || !now.isBefore(witness.admissionExpiresAt())) return denied(SHARE_SESSION_EXPIRED);
            return new Durable(grant, row, witness, true, null);
        }
        if (proof == null || policy == null || proof.unavailable()) return denied(DEPENDENCY_UNAVAILABLE);
        if (proof.denied()) return denied(SHARE_VERIFICATION_FAILED);
        if ("LOCKED".equals(grant.status())) return denied(SHARE_LOCKED);
        if (grant.maxAdmissions() != null && grant.admissionsUsed() >= grant.maxAdmissions()) return denied(SHARE_ADMISSION_LIMIT_REACHED);
        final Instant session;
        try { session = minimum(now.plus(policy.sessionTtl()), grant.expiresAt()); }
        catch (RuntimeException malformedPolicy) { return denied(SHARE_SESSION_POLICY_UNAVAILABLE); }
        UUID admissionId = UUID.randomUUID();
        AdmissionRow row = repository.insertAdmission(new AdmissionInsert(admissionId, grant.id(), key,
                grant.admissionsUsed() + 1, grant.verificationMode(), "NONE".equals(grant.verificationMode()) ? "NOT_REQUIRED" : "VERIFIED", now));
        Map<String, Object> payload = fields("verificationMode", grant.verificationMode(), "revocationGeneration", grant.revocationGeneration(),
                "codeGeneration", grant.codeGeneration(), "grantExpiresAt", utc(grant.expiresAt()),
                "admissionExpiresAt", utc(grant.expiresAt()), "sessionExpiresAt", utc(session));
        UUID recipientAccount = null;
        UUID recipientOrganization = null;
        if ("ORGANIZATION_MEMBER".equals(grant.verificationMode())) {
            recipientAccount = caller.orElseThrow().accountId();
            recipientOrganization = caller.orElseThrow().organizationId();
            payload.put("recipientAccountId", recipientAccount.toString());
            payload.put("recipientOrganizationId", recipientOrganization.toString());
        }
        String eventId = "admission-granted:" + grant.id() + ":" + admissionId;
        JsonNode json = node(payload);
        event(grant.id(), admissionId, eventId, "ADMISSION_GRANTED", "SUCCESS", now, json);
        if (repository.updateGrantAdmissionCount(grant.id(), grant.revision(), row.admissionNo()) != 1) throw failure(DEPENDENCY_UNAVAILABLE);
        witness = new AdmissionWitness(eventId, json, hash(json), grant.verificationMode(), grant.codeGeneration(), grant.revocationGeneration(),
                grant.expiresAt(), grant.expiresAt(), session, recipientAccount, recipientOrganization);
        return new Durable(grant, row, witness, false, null);
    }

    private ShareAdmissionException.Code predicates(GrantSnapshot grant, GrantSnapshot original, Instant now,
            Facts facts, Optional<TrustedShareAdmissionCaller> caller, Optional<AdmissionRow> existing, AdmissionWitness witness) {
        if ("REVOKED".equals(grant.status())) return SHARE_REVOKED;
        if ("EXPIRED".equals(grant.status())) return SHARE_EXPIRED;
        if (!now.isBefore(grant.expiresAt())) { expire(grant, now); return SHARE_EXPIRED; }
        if (now.isBefore(grant.validFrom())) return SHARE_LOCKED;
        if (witness != null ? !grant.verificationMode().equals(witness.verificationMode())
                || grant.codeGeneration() != witness.codeGeneration() || grant.revocationGeneration() != witness.revocationGeneration()
                : !grant.verificationMode().equals(original.verificationMode()) || grant.codeGeneration() != original.codeGeneration()
                || grant.revocationGeneration() != original.revocationGeneration()) return SHARE_GENERATION_STALE;
        if (!facts.resolved()) return DEPENDENCY_UNAVAILABLE;
        if (facts.open()) return SHARE_RISK_BLOCKED;
        if ("ORGANIZATION_MEMBER".equals(grant.verificationMode())) {
            if (caller.isEmpty()) return UNAUTHENTICATED;
            var trusted = caller.orElseThrow();
            if (!Objects.equals(grant.ownerOrganizationId(), trusted.organizationId())
                    || witness != null && (!trusted.accountId().equals(witness.recipientAccountId())
                    || !Objects.equals(trusted.organizationId(), witness.recipientOrganizationId()))) return SHARE_WRONG_TENANT;
            if (!facts.member()) return FORBIDDEN;
        }
        if (existing.isPresent() && witness == null) return DEPENDENCY_UNAVAILABLE;
        return null;
    }

    private void validateWitness(GrantSnapshot grant, AdmissionRow row, AdmissionWitness witness) {
        Set<String> expected = "ORGANIZATION_MEMBER".equals(row.verificationMode())
                ? Set.of("verificationMode", "codeGeneration", "revocationGeneration", "grantExpiresAt", "admissionExpiresAt", "sessionExpiresAt", "recipientAccountId", "recipientOrganizationId")
                : Set.of("verificationMode", "codeGeneration", "revocationGeneration", "grantExpiresAt", "admissionExpiresAt", "sessionExpiresAt");
        if (!witness.payload().path("codeGeneration").isInt() || !witness.payload().path("revocationGeneration").isInt()
                || witness.codeGeneration() < 0 || witness.revocationGeneration() < 0
                || !witness.eventId().equals("admission-granted:" + grant.id() + ":" + row.id())
                || !witness.payload().isObject() || !witness.payload().propertyNames().equals(expected)
                || !hash(witness.payload()).equals(witness.payloadSha256()) || !row.verificationMode().equals(witness.verificationMode())
                || witness.sessionExpiresAt().isAfter(witness.admissionExpiresAt())
                || witness.admissionExpiresAt().isAfter(witness.grantExpiresAt())
                || !row.admittedAt().isBefore(witness.sessionExpiresAt())) throw failure(DEPENDENCY_UNAVAILABLE);
    }

    private void lifecycle(GrantSnapshot grant, Optional<TrustedShareAdmissionCaller> caller) {
        if (!"ORGANIZATION_MEMBER".equals(grant.verificationMode()) || caller.isEmpty()) return;
        var trusted = caller.orElseThrow();
        repository.acquireOrganizationLifecycleLock(grant.ownerOrganizationId());
        repository.acquireMembershipLifecycleLock(grant.ownerOrganizationId(), trusted.accountId());
        repository.acquireAccountLifecycleLock(trusted.accountId());
    }
    private Facts facts(GrantSnapshot grant, Optional<TrustedShareAdmissionCaller> caller) {
        boolean member = false;
        if ("ORGANIZATION_MEMBER".equals(grant.verificationMode()) && caller.isPresent()) {
            UUID account = caller.orElseThrow().accountId();
            member = repository.readCurrentOrganization(grant.ownerOrganizationId()).filter(row -> "ACTIVE".equals(row.status())).isPresent()
                    && repository.readCurrentMembership(grant.ownerOrganizationId(), account).filter(row -> "ACTIVE".equals(row.status())).isPresent()
                    && repository.readCurrentAccount(account).filter(row -> "ACTIVE".equals(row.status())).isPresent();
        }
        var owner = repository.readSubjectOwnership(grant.subjectId());
        List<BindingState> bindings = grant.capturedBindingId() == null ? repository.readCurrentBindings(grant.subjectId())
                : repository.readCapturedBinding(grant.capturedBindingId()).map(List::of).orElse(List.of());
        if (grant.identityAtCaptureMalformed()
                || owner.isEmpty() || owner.orElseThrow().ownerAccountId() == null && owner.orElseThrow().ownerOrganizationId() == null
                || bindings.isEmpty() || grant.capturedBindingId() == null && bindings.size() != 1 || bindings.stream().anyMatch(binding -> !binding.subjectId().equals(grant.subjectId()) || binding.verifiedAt() == null)
                || !Objects.equals(owner.orElseThrow().ownerOrganizationId(), grant.ownerOrganizationId())
                || grant.ownerOrganizationId() == null && !Objects.equals(owner.orElseThrow().ownerAccountId(), grant.createdByAccountId())) return new Facts(false, false, member);
        var risk = repository.readSubjectRisk(grant.subjectId(), bindings.stream().map(BindingState::id).toList());
        return new Facts(risk.isPresent(), risk.map(RiskState::anyOpen).orElse(false), member);
    }

    private ShareAdmissionPolicyProvider.ApprovedTicketPolicy policy(GrantSnapshot grant, boolean fresh) {
        outside();
        try {
            var result = policies.ticketPolicy(new ShareAdmissionPolicyProvider.SharePolicyRequest(grant.id(), grant.verificationMode()));
            if (result instanceof ShareAdmissionPolicyProvider.ApprovedTicketPolicy approved
                    && positive(approved.ticketTtl()) && (!fresh || positive(approved.sessionTtl()))) return approved;
        } catch (RuntimeException unavailable) { throw failure(SHARE_SESSION_POLICY_UNAVAILABLE); }
        throw failure(SHARE_SESSION_POLICY_UNAVAILABLE);
    }
    private boolean positive(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) return false;
        try { Instant.EPOCH.plus(duration); return true; }
        catch (RuntimeException malformed) { return false; }
    }
    private Proof proof(GrantSnapshot grant, ShareAdmissionRequest request, Optional<TrustedShareAdmissionCaller> caller,
            ShareAdmissionPolicyProvider.ApprovedTicketPolicy policy) {
        outside();
        String mode = mode(request.verification());
        if (!mode.equals(grant.verificationMode())) return new Proof(true, false, null);
        if (request.verification() instanceof ShareAdmissionRequest.Verification.None) return new Proof(false, false, null);
        if (request.verification() instanceof ShareAdmissionRequest.Verification.OrganizationMember) return new Proof(false, false, null);
        try {
            if (request.verification() instanceof ShareAdmissionRequest.Verification.ExtractionCode extraction) {
                var result = codes.verify(new ShareAdmissionCodeVerifier.CodeVerificationRequest(grant.id(), extraction.code(), grant.codeSalt(), grant.codeHash(), grant.codeGeneration(), policy));
                if (result instanceof ShareAdmissionCodeVerifier.Verified) return new Proof(false, false, null);
                if (result instanceof ShareAdmissionCodeVerifier.Denied denied) return new Proof(true, false, reason(denied.reasonCode()));
            } else if (request.verification() instanceof ShareAdmissionRequest.Verification.DesignatedMobile mobile) {
                var verification = grant.contentSnapshot().path("verification");
                var result = mobiles.verify(new DesignatedMobileProofVerifier.MobileProofRequest(grant.id(), grant.shareId(), "ADMISSION",
                        verification.path("mobileHmac").asString(), verification.path("keyVersion").asString(), mobile.verificationProofId()));
                if (result instanceof DesignatedMobileProofVerifier.Verified) return new Proof(false, false, null);
                if (result instanceof DesignatedMobileProofVerifier.Denied denied) return new Proof(true, false, reason(denied.reasonCode()));
            }
        } catch (RuntimeException unavailable) { return new Proof(false, true, null); }
        return new Proof(false, true, null);
    }
    private String reason(String value) { return value == null ? null : value.matches("[A-Z][A-Z0-9_]{0,63}") ? value : null; }
    private void observation(GrantSnapshot grant, ShareAdmissionRequest request, String reason) {
        String requestDigest = digest(request.admissionRequestId());
        String identity = "share-observation:" + hash(node(fields("shareGrantId", grant.id().toString(), "eventType", "VERIFICATION_FAILED", "requestKeyDigest", requestDigest)));
        observations.execute(status -> {
            Instant now = repository.now();
            event(grant.id(), null, identity, "VERIFICATION_FAILED", "DENIED", now, node(fields("shareGrantId", grant.id().toString(),
                    "eventType", "VERIFICATION_FAILED", "result", "DENIED", "reasonCode", reason, "requestKeyDigest", requestDigest, "verificationMode", mode(request.verification()))));
            return null;
        });
    }
    private void expire(GrantSnapshot grant, Instant now) {
        String expiry = utc(grant.expiresAt());
        String identity = "share-transition:" + hash(node(fields("shareGrantId", grant.id().toString(), "actionType", "EXPIRE", "sourceIdentity", fields("kind", "EXPIRY", "expiresAt", expiry))));
        String fingerprint = hash(node(fields("shareGrantId", grant.id().toString(), "actionType", "EXPIRE", "reasonCode", null, "newExpiresAt", null, "expiresAt", expiry)));
        JsonNode payload = node(fields("actionType", "EXPIRE", "expiresAt", expiry, "actionFingerprint", fingerprint, "status", "EXPIRED",
                "codeGeneration", grant.codeGeneration(), "revocationGeneration", grant.revocationGeneration() + 1, "revision", grant.revision() + 1));
        var previous = repository.findAccessEvent(grant.id(), identity);
        if (previous.isPresent()) {
            if (!previous.orElseThrow().payloadSha256().equals(hash(payload))) throw failure(DEPENDENCY_UNAVAILABLE);
            return;
        }
        if (repository.expireGrant(grant.id(), grant.revision()) != 1) throw failure(DEPENDENCY_UNAVAILABLE);
        event(grant.id(), null, identity, "EXPIRED", "DENIED", now, payload);
    }

    private TicketContext ticketContext(Durable durable) {
        return writes.execute(status -> {
            lifecycle(durable.grant(), Optional.ofNullable(durable.witness().recipientAccountId()).map(account -> new TrustedShareAdmissionCaller(account, durable.witness().recipientOrganizationId())));
            var caller = Optional.ofNullable(durable.witness().recipientAccountId()).map(account -> new TrustedShareAdmissionCaller(account, durable.witness().recipientOrganizationId()));
            GrantSnapshot grant = repository.lockGrant(durable.grant().id()).orElseThrow(() -> failure(SHARE_TOKEN_INVALID));
            Instant now = repository.now();
            Facts facts = facts(grant, caller);
            var denial = predicates(grant, durable.grant(), now, facts, caller, Optional.of(durable.row()), durable.witness());
            if (denial == null && (!now.isBefore(durable.witness().sessionExpiresAt()) || !now.isBefore(durable.witness().admissionExpiresAt()))) {
                denial = SHARE_SESSION_EXPIRED;
            }
            return new TicketContext(denial == null ? new Durable(grant, durable.row(), durable.witness(), durable.replay(), null) : denied(denial), now);
        });
    }
    private AdmissionOutcome issue(TicketContext context, ShareAdmissionPolicyProvider.ApprovedTicketPolicy policy) {
        Durable current = context.durable();
        String reason = "INTERNAL_FAILURE";
        try {
            Instant now = reads.execute(status -> repository.now());
            Instant expires = minimum(now.plus(policy.ticketTtl()), current.witness().sessionExpiresAt(), current.witness().admissionExpiresAt(), current.grant().expiresAt());
            if (!now.isBefore(expires)) return ticketFailed(current, "TICKET_WINDOW_EXPIRED", SHARE_SESSION_EXPIRED);
            outside();
            reason = "MEDIA_CREDENTIAL_UNAVAILABLE";
            var credential = credentials.issue(new ShareMediaCredentialProvider.MediaCredentialRequest(current.grant().id(), current.row().id(), current.witness().revocationGeneration(), current.witness().codeGeneration(), expires));
            if (credential instanceof ShareMediaCredentialProvider.Unavailable) return ticketFailed(current, reason, DEPENDENCY_UNAVAILABLE);
            reason = "MEDIA_CREDENTIAL_MALFORMED";
            if (!(credential instanceof ShareMediaCredentialProvider.Issued issued) || issued.credential() == null || issued.credential().isBlank()
                    || issued.credential().codePointCount(0, issued.credential().length()) > 2048
                    || issued.credential().indexOf('\0') >= 0 || !StandardCharsets.UTF_8.newEncoder().canEncode(issued.credential())
                    || issued.expiresAt() == null || issued.expiresAt().isAfter(expires) || !issued.expiresAt().isAfter(now)) return ticketFailed(current, reason, DEPENDENCY_UNAVAILABLE);
            Instant writtenAt = reads.execute(status -> repository.now());
            Duration ttl = Duration.between(writtenAt, issued.expiresAt());
            if (!positive(ttl)) return ticketFailed(current, "TICKET_WINDOW_EXPIRED", SHARE_SESSION_EXPIRED);
            outside();
            reason = "TICKET_WRITE_UNAVAILABLE";
            var result = tickets.write(new ShareAdmissionTicketProvider.TicketWriteRequest("share-ticket:" + current.grant().id() + ":" + current.row().id(), current.grant().id(), current.row().id(),
                    current.witness().revocationGeneration(), current.witness().codeGeneration(), current.witness().sessionExpiresAt(), current.witness().admissionExpiresAt(), current.grant().expiresAt(), issued.expiresAt(), ttl, issued.credential()));
            if (!(result instanceof ShareAdmissionTicketProvider.Written)) return ticketFailed(current,
                    result instanceof ShareAdmissionTicketProvider.Unavailable ? reason : "TICKET_WRITE_MALFORMED", DEPENDENCY_UNAVAILABLE);
            reason = "TICKET_EVENT_UNAVAILABLE";
            JsonNode payload = node(fields("shareGrantId", current.grant().id().toString(), "admissionId", current.row().id().toString()));
            writes.execute(status -> { event(current.grant().id(), current.row().id(), "ticket-issued:" + hash(payload), "TICKET_ISSUED", "SUCCESS", repository.now(), payload); return null; });
            var view = new ShareAdmissionView(current.row().id(), current.row().verificationMode(), current.row().verificationResult(), current.row().admittedAt(), current.witness().sessionExpiresAt(), current.witness().admissionExpiresAt(), new ShareAdmissionView.MediaAuthorization(issued.credential(), issued.expiresAt()));
            return current.replay() ? new ReplayCommitted(view) : new AdmissionCommitted(view);
        } catch (RuntimeException unavailable) { return ticketFailed(current, reason, DEPENDENCY_UNAVAILABLE); }
    }
    private AdmissionOutcome ticketFailed(Durable durable, String reason, ShareAdmissionException.Code code) {
        outside();
        try { tickets.expire("share-ticket:" + durable.grant().id() + ":" + durable.row().id()); }
        catch (RuntimeException unavailable) { }
        try {
            JsonNode identity = node(fields("shareGrantId", durable.grant().id().toString(), "admissionId", durable.row().id().toString(), "admissionKeyDigest", digest(durable.row().admissionKey())));
            Map<String, Object> payload = fields("shareGrantId", durable.grant().id().toString(), "admissionId", durable.row().id().toString(), "admissionKeyDigest", digest(durable.row().admissionKey()), "reasonCode", reason);
            writes.execute(status -> { event(durable.grant().id(), durable.row().id(), "ticket-failed:" + hash(identity), "TICKET_FAILED", "FAILED", repository.now(), node(payload)); return null; });
        } catch (RuntimeException unavailable) { return new DeniedCommitted(DEPENDENCY_UNAVAILABLE); }
        return new DeniedCommitted(code);
    }
    private void event(UUID grant, UUID admission, String identity, String type, String result, Instant now, JsonNode payload) {
        if (repository.insertAccessEvent(new AccessEventInsert(grant, admission, identity, type, result, now, payload, hash(payload))).status() == EventWriteStatus.CONFLICT) throw failure(DEPENDENCY_UNAVAILABLE);
    }
    public String admissionKey(UUID grant, String requestId) { return "admission:" + hash(node(fields("shareGrantId", grant.toString(), "admissionRequestId", requestId))); }
    private String checkedDigest(byte[] bytes) {
        String value = sha256.digest(bytes);
        if (value == null || !value.matches("[0-9a-f]{64}")) throw failure(DEPENDENCY_UNAVAILABLE);
        return value;
    }
    private String digest(String value) { return checkedDigest(value.getBytes(StandardCharsets.UTF_8)); }
    private String hash(JsonNode value) { return digest(mapper.writeValueAsString(canonical(value))); }
    private Object canonical(JsonNode value) {
        if (value.isObject()) { Map<String, Object> sorted = new TreeMap<>(); value.properties().forEach(entry -> sorted.put(entry.getKey(), canonical(entry.getValue()))); return sorted; }
        if (value.isArray()) { List<Object> entries = new ArrayList<>(); value.forEach(entry -> entries.add(canonical(entry))); return entries; }
        return value;
    }
    private JsonNode node(Map<String, Object> value) { return mapper.valueToTree(value); }
    private Map<String, Object> fields(Object... pairs) {
        Map<String, Object> result = new TreeMap<>();
        for (int index = 0; index < pairs.length; index += 2) result.put((String) pairs[index], pairs[index + 1]);
        return result;
    }
    private String utc(Instant instant) { return new java.time.format.DateTimeFormatterBuilder().appendInstant(instant.getNano() == 0 ? 0 : 6).toFormatter().format(instant); }
    private Instant minimum(Instant... instants) { return java.util.Arrays.stream(instants).min(Instant::compareTo).orElseThrow(); }
    private void outside() { if (TransactionSynchronizationManager.isActualTransactionActive()) throw failure(DEPENDENCY_UNAVAILABLE); }
    private void validate(ShareAdmissionRequest request) {
        if (request == null || request.verification() == null || request.admissionRequestId() == null || request.admissionRequestId().isBlank()
                || request.admissionRequestId().codePointCount(0, request.admissionRequestId().length()) > 128
                || request.admissionRequestId().indexOf('\0') >= 0 || !StandardCharsets.UTF_8.newEncoder().canEncode(request.admissionRequestId())) throw failure(VALIDATION_FAILED);
        if (request.verification() instanceof ShareAdmissionRequest.Verification.ExtractionCode extraction) text(extraction.code());
        if (request.verification() instanceof ShareAdmissionRequest.Verification.DesignatedMobile mobile) text(mobile.verificationProofId());
    }
    private void text(String value) { if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > 128 || value.indexOf('\0') >= 0 || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) throw failure(VALIDATION_FAILED); }
    private String mode(ShareAdmissionRequest.Verification verification) {
        if (verification instanceof ShareAdmissionRequest.Verification.None) return "NONE";
        if (verification instanceof ShareAdmissionRequest.Verification.ExtractionCode) return "EXTRACTION_CODE";
        if (verification instanceof ShareAdmissionRequest.Verification.DesignatedMobile) return "DESIGNATED_MOBILE";
        return "ORGANIZATION_MEMBER";
    }
    private Durable denied(ShareAdmissionException.Code code) { return new Durable(null, null, null, false, code); }
    private static ShareAdmissionException failure(ShareAdmissionException.Code code) { return new ShareAdmissionException(code); }
    private record TicketContext(Durable durable, Instant now) { }
    private record Preliminary(GrantSnapshot grant, String key, boolean exists) { }
    private record Facts(boolean resolved, boolean open, boolean member) { }
    private record Proof(boolean denied, boolean unavailable, String reason) { }
    private record Durable(GrantSnapshot grant, AdmissionRow row, AdmissionWitness witness, boolean replay, ShareAdmissionException.Code denial) { }
}
