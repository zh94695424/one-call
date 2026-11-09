package com.onecall.core.sharing.internal.admission;

import static com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException.Code.*;

import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository.*;
import com.onecall.core.sharing.internal.publicadmission.*;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.AuthorizedMediaFacts;
import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.ReplayAnchor;
import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.MediaAuthorizationCredentialResult;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class DefaultShareMediaAuthorizationService implements ShareMediaAuthorizationService {
    private final TransactionTemplate writes;
    private final TransactionTemplate reads;
    private final ShareAdmissionRepository repository;
    private final ShareAdmissionSha256Provider sha256;
    private final ShareMediaCredentialProvider credentials;
    private final EvidenceMediaObjectReader evidence;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public DefaultShareMediaAuthorizationService(TransactionTemplate transactions, ShareAdmissionRepository repository,
            ShareAdmissionSha256Provider sha256, ShareMediaCredentialProvider credentials, EvidenceMediaObjectReader evidence) {
        this.writes = transactions;
        this.reads = new TransactionTemplate(Objects.requireNonNull(transactions.getTransactionManager()), transactions);
        this.reads.setReadOnly(true);
        this.reads.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.repository = repository;
        this.sha256 = sha256;
        this.credentials = credentials;
        this.evidence = evidence;
    }

    @Override
    public ShareAdmissionView.MediaAuthorization authorize(ShareAdmissionCapabilityToken capabilityToken, UUID admissionId,
            MediaAuthorizationRequest request, CallerResolver callerResolver) throws ShareAdmissionException {
        validate(request, admissionId);
        final String tokenDigest = digest(capabilityToken.utf8());
        Preliminary preliminary;
        try {
            preliminary = reads.execute(status -> {
                GrantSnapshot grant = repository.findGrantByTokenDigest(tokenDigest).orElseThrow(() -> failure(SHARE_TOKEN_INVALID));
                return new Preliminary(grant, "ORGANIZATION_MEMBER".equals(grant.verificationMode()));
            });
            if (preliminary.member()) {
                if (callerResolver == null) throw failure(UNAUTHENTICATED);
                preliminary = new Preliminary(preliminary.grant(), true, Optional.ofNullable(callerResolver.resolve()).orElse(Optional.empty()));
                if (preliminary.caller().isEmpty()) throw failure(UNAUTHENTICATED);
            } else {
                preliminary = new Preliminary(preliminary.grant(), false, Optional.empty());
            }
            Preliminary authorized = preliminary;
            Decision initial = writes.execute(status -> authoritative(authorized.grant(), admissionId, request, authorized.caller().orElse(null)));
            outside();
            Decision decision = resolved(initial, request);
            Instant bound = bound(decision.witness(), decision.now());
            String eventType = eventType(request.purpose());
            String requestKeyDigest = digest(request.authorizationRequestId().getBytes(StandardCharsets.UTF_8));
            String eventId = "share-access:" + hash(fields("shareGrantId", decision.grant().id().toString(), "shareAdmissionId", admissionId.toString(),
                    "eventType", eventType, "requestKeyDigest", requestKeyDigest));
            ShareMediaCredentialProvider.MediaAuthorizationCredentialResult result;
            try {
                result = invoke(decision, request, eventId, decision.priorSuccess());
            } catch (RuntimeException unavailable) {
                if (decision.priorSuccess() == null) appendFailure(decision, request, requestKeyDigest, "MEDIA_CREDENTIAL_UNAVAILABLE");
                throw failure(DEPENDENCY_UNAVAILABLE);
            }
            Instant providerExpiry = result instanceof ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued issued
                    && issued.expiresAt() != null ? issued.expiresAt().truncatedTo(ChronoUnit.MICROS) : null;
            if (!(result instanceof ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued issued)
                    || !validCredential(issued.credential(), providerExpiry, decision.now(), bound) || issued.replayAnchor() == null
                    || !anchorTime(issued.replayAnchor()).isBefore(providerExpiry)
                    || decision.priorSuccess() != null && !matchesCommitted(issued, decision.priorSuccess())) {
                String reason = result instanceof ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Malformed
                        || result instanceof ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued ? "MEDIA_CREDENTIAL_MALFORMED" : "MEDIA_CREDENTIAL_UNAVAILABLE";
                if (decision.priorSuccess() == null) appendFailure(decision, request, requestKeyDigest, reason);
                throw failure(DEPENDENCY_UNAVAILABLE);
            }
            Map<String, Object> payload = successPayload(decision, request, providerExpiry, issued.replayAnchor());
            String payloadHash = hash(node(payload));
            EventWriteResult written = writes.execute(status -> repository.insertAccessEvent(new AccessEventInsert(decision.grant().id(), admissionId,
                    eventId, eventType, "SUCCESS", repository.now(), node(payload), payloadHash)));
            outside();
            validateSuccess(decision, request, written.first(), eventId);
            if (!payloadHash.equals(written.first().payloadSha256())) {
                var reconstructed = invoke(decision, request, eventId, written.first());
                if (!(reconstructed instanceof MediaAuthorizationCredentialResult.Issued winner)
                        || !matchesCommitted(winner, written.first()) || !validCredential(winner.credential(), normalize(winner.expiresAt()), decision.now(), bound))
                    throw failure(DEPENDENCY_UNAVAILABLE);
                return new ShareAdmissionView.MediaAuthorization(winner.credential(), normalize(winner.expiresAt()));
            }
            return new ShareAdmissionView.MediaAuthorization(issued.credential(), providerExpiry);
        } catch (ShareAdmissionException known) { throw known; }
        catch (RuntimeException unavailable) { throw failure(DEPENDENCY_UNAVAILABLE); }
    }

    private Decision authoritative(GrantSnapshot original, UUID admissionId, MediaAuthorizationRequest request, TrustedShareAdmissionCaller caller) {
        if (caller != null) {
            if (!Objects.equals(original.ownerOrganizationId(), caller.organizationId())) throw failure(SHARE_WRONG_TENANT);
            repository.acquireOrganizationLifecycleLock(caller.organizationId());
            repository.acquireMembershipLifecycleLock(caller.organizationId(), caller.accountId());
            repository.acquireAccountLifecycleLock(caller.accountId());
        }
        GrantSnapshot grant = repository.lockGrant(original.id()).orElseThrow(() -> failure(SHARE_TOKEN_INVALID));
        Instant now = repository.now();
        AdmissionRow admission = repository.findAdmissionById(grant.id(), admissionId).orElseThrow(() -> failure(FORBIDDEN));
        if ("REVOKED".equals(grant.status())) throw failure(SHARE_REVOKED);
        if (grant.expiresAt() == null || !now.isBefore(grant.expiresAt())) throw failure(SHARE_EXPIRED);
        AdmissionWitness witness;
        try { witness = repository.findAdmissionWitness(grant.id(), admissionId).orElseThrow(() -> failure(FORBIDDEN)); }
        catch (ShareAdmissionException known) { throw known; }
        catch (RuntimeException malformed) { throw failure(DEPENDENCY_UNAVAILABLE); }
        validateWitnessBounds(witness);
        Set<String> witnessKeys = new HashSet<>();
        witness.payload().propertyNames().forEach(witnessKeys::add);
        Set<String> expectedWitnessKeys = "ORGANIZATION_MEMBER".equals(witness.verificationMode())
                ? Set.of("verificationMode", "codeGeneration", "revocationGeneration", "grantExpiresAt", "admissionExpiresAt", "sessionExpiresAt", "recipientAccountId", "recipientOrganizationId")
                : Set.of("verificationMode", "codeGeneration", "revocationGeneration", "grantExpiresAt", "admissionExpiresAt", "sessionExpiresAt");
        if (!Objects.equals(admission.grantId(), grant.id()) || !Objects.equals(admission.verificationMode(), grant.verificationMode())
                || !Objects.equals(admission.verificationMode(), witness.verificationMode())
                || !Objects.equals(witness.eventId(), "admission-granted:" + grant.id() + ":" + admissionId)
                || witness.eventId() == null
                || witness.payloadSha256() == null || witness.verificationMode() == null || witness.grantExpiresAt() == null
                || witness.admissionExpiresAt() == null || witness.sessionExpiresAt() == null
                || !Objects.equals(witness.payloadSha256(), hash(witness.payload()))
                || !witnessKeys.equals(expectedWitnessKeys)
                || admission.admittedAt() == null || !admission.admittedAt().isBefore(witness.sessionExpiresAt())
                || witness.sessionExpiresAt().isAfter(witness.admissionExpiresAt()) || witness.admissionExpiresAt().isAfter(witness.grantExpiresAt())) throw failure(DEPENDENCY_UNAVAILABLE);
        if (grant.validFrom() == null || now.isBefore(grant.validFrom())) throw failure(DEPENDENCY_UNAVAILABLE);
        if (!now.isBefore(witness.sessionExpiresAt()) || !now.isBefore(witness.admissionExpiresAt())) throw failure(SHARE_SESSION_EXPIRED);
        if ("EXPIRED".equals(grant.status())) throw failure(SHARE_EXPIRED);
        if (!Objects.equals(witness.codeGeneration(), grant.codeGeneration()) || !Objects.equals(witness.revocationGeneration(), grant.revocationGeneration())) throw failure(SHARE_GENERATION_STALE);
        if (!"ACTIVE".equals(grant.status()) && !"LOCKED".equals(grant.status())) throw failure(SHARE_GENERATION_STALE);
        if (caller != null) {
            OrganizationState organization = repository.readCurrentOrganization(caller.organizationId()).orElseThrow(() -> failure(FORBIDDEN));
            MembershipState membership = repository.readCurrentMembership(caller.organizationId(), caller.accountId()).orElseThrow(() -> failure(FORBIDDEN));
            AccountState account = repository.readCurrentAccount(caller.accountId()).orElseThrow(() -> failure(FORBIDDEN));
            if (!"ACTIVE".equals(organization.status()) || !"ACTIVE".equals(membership.status()) || !"ACTIVE".equals(account.status())
                    || !Objects.equals(witness.recipientAccountId(), caller.accountId()) || !Objects.equals(witness.recipientOrganizationId(), caller.organizationId())) throw failure(FORBIDDEN);
        }
        if (grant.identityAtCaptureMalformed()) throw failure(DEPENDENCY_UNAVAILABLE);
        SubjectOwnership ownership = repository.readSubjectOwnership(grant.subjectId()).orElseThrow(() -> failure(DEPENDENCY_UNAVAILABLE));
        if (ownership.ownerAccountId() == null && ownership.ownerOrganizationId() == null
                || !Objects.equals(ownership.ownerOrganizationId(), grant.ownerOrganizationId())
                || grant.ownerOrganizationId() == null && !Objects.equals(ownership.ownerAccountId(), grant.createdByAccountId()))
            throw failure(DEPENDENCY_UNAVAILABLE);
        List<BindingState> current = repository.readCurrentBindings(grant.subjectId());
        List<BindingState> selected = grant.capturedBindingId() == null ? current
                : repository.readCapturedBinding(grant.capturedBindingId()).map(List::of).orElse(List.of());
        if (selected.size() != 1 || selected.stream().anyMatch(binding -> !binding.subjectId().equals(grant.subjectId())
                || !"ACTIVE".equals(binding.status()) || binding.verifiedAt() == null
                || current.stream().noneMatch(active -> active.id().equals(binding.id()) && active.subjectId().equals(grant.subjectId())
                        && "ACTIVE".equals(active.status()) && active.verifiedAt() != null))) throw failure(SHARE_RISK_BLOCKED);
        RiskState risk = repository.readSubjectRisk(grant.subjectId(), selected.stream().map(BindingState::id).toList()).orElseThrow(() -> failure(SHARE_RISK_BLOCKED));
        if (risk.rows().isEmpty() || risk.anyOpen()) throw failure(SHARE_RISK_BLOCKED);
        String requestKeyDigest = digest(request.authorizationRequestId().getBytes(StandardCharsets.UTF_8));
        String eventType = eventType(request.purpose());
        List<AccessEventRow> prior = repository.findAccessEventsByRequestKeyDigest(grant.id(), admissionId, requestKeyDigest);
        return new Decision(grant, admissionId, witness, now, null, requestKeyDigest, eventType, null, prior, null);
    }

    private void appendFailure(Decision decision, MediaAuthorizationRequest request, String requestKeyDigest, String reason) {
        Map<String, Object> payload = failurePayload(decision, request, reason);
        String failureId = "share-access:" + hash(fields("shareGrantId", decision.grant().id().toString(), "shareAdmissionId", decision.admissionId().toString(),
                "eventType", "FAILURE", "requestKeyDigest", requestKeyDigest));
        writes.execute(status -> repository.insertAccessEvent(new AccessEventInsert(decision.grant().id(), decision.admissionId(),
                failureId, "FAILURE", "FAILED", repository.now(), node(payload), hash(node(payload)))));
    }

    private Decision resolved(Decision initial, MediaAuthorizationRequest request) {
        AuthorizedMediaFacts facts;
        try {
            if (!(evidence.resolve(initial.grant().evidenceId()) instanceof EvidenceMediaObjectReader.Result.Found found)
                    || found.media().durationSeconds() == null) throw failure(DEPENDENCY_UNAVAILABLE);
            var media = found.media();
            facts = new AuthorizedMediaFacts(initial.grant().evidenceId(), media.mediaObjectId(), media.sha256(), media.sizeBytes(),
                    media.mimeType(), media.durationSeconds(), 0, Math.multiplyExact((long) media.durationSeconds(), 1_000_000_000L));
        } catch (RuntimeException unavailable) { throw failure(DEPENDENCY_UNAVAILABLE); }
        var pending = new Decision(initial.grant(), initial.admissionId(), initial.witness(), initial.now(), null,
                initial.requestKeyDigest(), initial.eventType(), null, initial.prior(), facts);
        String fingerprint = hash(basePayload(pending, request));
        var decision = new Decision(initial.grant(), initial.admissionId(), initial.witness(), initial.now(), null,
                initial.requestKeyDigest(), initial.eventType(), fingerprint, initial.prior(), facts);
        AccessEventRow success = null;
        for (var event : initial.prior()) {
            if (event.payload() == null || !Objects.equals(event.payload().path("preProviderFingerprint").asString(), fingerprint)
                    || !Objects.equals(event.payload().path("purpose").asString(), request.purpose().name())) throw failure(SHARE_GENERATION_STALE);
            if ("FAILURE".equals(event.eventType()) && "FAILED".equals(event.result())) {
                String failureId = eventId(decision, "FAILURE");
                if (!failureId.equals(event.eventId()) || !hash(event.payload()).equals(event.payloadSha256())
                        || !hash(node(failurePayload(decision, request, event.payload().path("reasonCode").asString()))).equals(event.payloadSha256()))
                    throw failure(DEPENDENCY_UNAVAILABLE);
            } else {
                validateSuccess(decision, request, event, eventId(decision, decision.eventType()));
                if (success != null) throw failure(DEPENDENCY_UNAVAILABLE);
                success = event;
            }
        }
        return new Decision(initial.grant(), initial.admissionId(), initial.witness(), initial.now(), success,
                initial.requestKeyDigest(), initial.eventType(), fingerprint, initial.prior(), facts);
    }

    private String eventId(Decision decision, String eventType) {
        return "share-access:" + hash(fields("shareGrantId", decision.grant().id().toString(), "shareAdmissionId", decision.admissionId().toString(),
                "eventType", eventType, "requestKeyDigest", decision.requestKeyDigest()));
    }

    private Map<String, Object> basePayload(Decision decision, MediaAuthorizationRequest request) {
        Map<String, Object> values = fields("shareGrantId", decision.grant().id().toString(), "shareAdmissionId", decision.admissionId().toString(),
                "eventType", decision.eventType(), "purpose", request.purpose().name(), "requestKeyDigest", decision.requestKeyDigest(),
                "codeGeneration", decision.grant().codeGeneration(), "revocationGeneration", decision.grant().revocationGeneration(),
                "grantExpiresAt", utc(decision.witness().grantExpiresAt()), "admissionExpiresAt", utc(decision.witness().admissionExpiresAt()),
                "sessionExpiresAt", utc(decision.witness().sessionExpiresAt()), "credentialExpiresAt", utc(bound(decision.witness(), decision.now())));
        var facts = decision.facts();
        values.putAll(fields("evidenceId", facts.evidenceId().toString(), "mediaObjectId", facts.mediaObjectId().toString(), "sha256", facts.sha256(),
                "sizeBytes", facts.sizeBytes(), "mimeType", facts.mimeType(), "durationSeconds", facts.durationSeconds(),
                "windowStartNanos", facts.windowStartNanos(), "windowEndNanos", facts.windowEndNanos()));
        return values;
    }

    private Map<String, Object> successPayload(Decision decision, MediaAuthorizationRequest request, Instant expiry, ReplayAnchor anchor) {
        var values = basePayload(decision, request);
        values.putAll(fields("credentialExpiresAt", utc(expiry), "result", "SUCCESS", "preProviderFingerprint", decision.preProviderFingerprint(),
                "issuedAtMicros", anchor.issuedAtMicros(), "notBeforeMicros", anchor.notBeforeMicros(), "kid", anchor.kid()));
        return values;
    }

    private Map<String, Object> failurePayload(Decision decision, MediaAuthorizationRequest request, String reason) {
        var values = basePayload(decision, request);
        values.remove("codeGeneration");
        values.remove("revocationGeneration");
        values.putAll(fields("eventType", "FAILURE", "result", "FAILED", "reasonCode", reason, "preProviderFingerprint", decision.preProviderFingerprint()));
        return values;
    }

    private ReplayAnchor anchor(AccessEventRow event) {
        var payload = event.payload();
        if (!payload.path("issuedAtMicros").isIntegralNumber() || !payload.path("notBeforeMicros").isIntegralNumber()
                || !payload.path("issuedAtMicros").canConvertToLong() || !payload.path("notBeforeMicros").canConvertToLong()
                || !payload.path("kid").isTextual()) throw failure(DEPENDENCY_UNAVAILABLE);
        return new ReplayAnchor(payload.path("issuedAtMicros").asLong(), payload.path("notBeforeMicros").asLong(), payload.path("kid").asString());
    }

    private Instant anchorTime(ReplayAnchor anchor) {
        return Instant.ofEpochSecond(Math.floorDiv(anchor.issuedAtMicros(), 1_000_000L), Math.floorMod(anchor.issuedAtMicros(), 1_000_000L) * 1_000);
    }

    private Instant expiry(AccessEventRow event) {
        if (!event.payload().path("credentialExpiresAt").isTextual()) throw failure(DEPENDENCY_UNAVAILABLE);
        return Instant.parse(event.payload().path("credentialExpiresAt").asString());
    }

    private void validateSuccess(Decision decision, MediaAuthorizationRequest request, AccessEventRow event, String eventId) {
        if (event == null || event.payload() == null) throw failure(DEPENDENCY_UNAVAILABLE);
        if (!eventId.equals(event.eventId()) || !decision.eventType().equals(event.eventType()) || !"SUCCESS".equals(event.result())
                || !decision.preProviderFingerprint().equals(event.payload().path("preProviderFingerprint").asString())) throw failure(SHARE_GENERATION_STALE);
        Instant expiry = expiry(event);
        if (!hash(event.payload()).equals(event.payloadSha256()) || !expiry.equals(normalize(expiry))
                || !expiry.isAfter(decision.now()) || expiry.isAfter(bound(decision.witness(), decision.now()))
                || !anchorTime(anchor(event)).isBefore(expiry)
                || !hash(node(successPayload(decision, request, expiry, anchor(event)))).equals(event.payloadSha256())) throw failure(DEPENDENCY_UNAVAILABLE);
    }

    private boolean matchesCommitted(MediaAuthorizationCredentialResult.Issued issued, AccessEventRow event) {
        return issued.expiresAt() != null && normalize(issued.expiresAt()).equals(expiry(event)) && anchor(event).equals(issued.replayAnchor());
    }

    private MediaAuthorizationCredentialResult invoke(Decision decision, MediaAuthorizationRequest request, String eventId, AccessEventRow committed) {
        outside();
        return credentials.issueOrReplay(new ShareMediaCredentialProvider.MediaAuthorizationCredentialRequest(decision.grant().id(), decision.admissionId(),
                request.purpose().name(), decision.eventType(), decision.requestKeyDigest(), decision.grant().codeGeneration(), decision.grant().revocationGeneration(),
                normalize(decision.witness().grantExpiresAt()), normalize(decision.witness().admissionExpiresAt()), normalize(decision.witness().sessionExpiresAt()),
                committed == null ? bound(decision.witness(), decision.now()) : expiry(committed), decision.preProviderFingerprint(), eventId,
                decision.facts(), committed == null ? null : anchor(committed)));
    }

    private Instant bound(AdmissionWitness witness, Instant now) {
        if (witness.grantExpiresAt() == null || witness.admissionExpiresAt() == null || witness.sessionExpiresAt() == null
                || !witness.grantExpiresAt().isAfter(Instant.EPOCH) || !witness.admissionExpiresAt().isAfter(Instant.EPOCH)
                || !witness.sessionExpiresAt().isAfter(Instant.EPOCH)) throw failure(SHARE_SESSION_POLICY_UNAVAILABLE);
        Instant bound = normalize(minimum(witness.grantExpiresAt(), witness.admissionExpiresAt(), witness.sessionExpiresAt()));
        if (!now.isBefore(witness.sessionExpiresAt()) || !now.isBefore(witness.admissionExpiresAt())) throw failure(SHARE_SESSION_EXPIRED);
        if (!bound.isAfter(now)) throw failure(SHARE_EXPIRED);
        return bound;
    }
    private void validateWitnessBounds(AdmissionWitness witness) {
        if (witness.grantExpiresAt() == null || witness.admissionExpiresAt() == null || witness.sessionExpiresAt() == null
                || !witness.grantExpiresAt().isAfter(Instant.EPOCH) || !witness.admissionExpiresAt().isAfter(Instant.EPOCH)
                || !witness.sessionExpiresAt().isAfter(Instant.EPOCH)) throw failure(SHARE_SESSION_POLICY_UNAVAILABLE);
    }
    private boolean validCredential(String credential, Instant expiry, Instant now, Instant bound) {
        return credential != null && !credential.isBlank() && credential.codePointCount(0, credential.length()) <= 2048
                && credential.indexOf('\0') < 0 && StandardCharsets.UTF_8.newEncoder().canEncode(credential)
                && expiry != null && expiry.isAfter(now) && !expiry.isAfter(bound);
    }
    private void validate(MediaAuthorizationRequest request, UUID admissionId) {
        if (admissionId == null || request == null || request.authorizationRequestId() == null || request.authorizationRequestId().isBlank()
                || request.authorizationRequestId().codePointCount(0, request.authorizationRequestId().length()) > 128
                || request.authorizationRequestId().indexOf('\0') >= 0 || !StandardCharsets.UTF_8.newEncoder().canEncode(request.authorizationRequestId()) || request.purpose() == null) throw failure(VALIDATION_FAILED);
    }
    private String eventType(MediaAuthorizationRequest.Purpose purpose) { return switch (purpose) { case PLAYBACK -> "PLAYBACK_AUTHORIZED"; case SEEK -> "SEEK_AUTHORIZED"; case RENEW -> "RENEWED"; }; }
    private String digest(byte[] bytes) { String value = sha256.digest(bytes); if (value == null || !value.matches("[0-9a-f]{64}")) throw failure(DEPENDENCY_UNAVAILABLE); return value; }
    private String hash(Object value) { return digest(mapper.writeValueAsString(value instanceof JsonNode node ? canonical(node) : value).getBytes(StandardCharsets.UTF_8)); }
    private JsonNode node(Map<String, Object> values) { return mapper.valueToTree(values); }
    private Object canonical(JsonNode value) { if (value.isObject()) { Map<String,Object> sorted = new TreeMap<>(); value.properties().forEach(entry -> sorted.put(entry.getKey(), canonical(entry.getValue()))); return sorted; } if (value.isArray()) { List<Object> list = new ArrayList<>(); value.forEach(item -> list.add(canonical(item))); return list; } return value; }
    private Map<String,Object> fields(Object... values) { Map<String,Object> result = new TreeMap<>(); for (int i=0;i<values.length;i+=2) result.put((String) values[i], values[i+1]); return result; }
    private Instant normalize(Instant value) { return value.truncatedTo(ChronoUnit.MICROS); }
    private String utc(Instant value) { Instant normalized = normalize(value); return new java.time.format.DateTimeFormatterBuilder().appendInstant(normalized.getNano() == 0 ? 0 : 6).toFormatter().format(normalized); }
    private Instant minimum(Instant... values) { return Arrays.stream(values).min(Instant::compareTo).orElseThrow(); }
    private void outside() { if (TransactionSynchronizationManager.isActualTransactionActive()) throw failure(DEPENDENCY_UNAVAILABLE); }
    private static ShareAdmissionException failure(ShareAdmissionException.Code code) { return new ShareAdmissionException(code); }
    private record Preliminary(GrantSnapshot grant, boolean member, Optional<TrustedShareAdmissionCaller> caller) { Preliminary(GrantSnapshot grant, boolean member) { this(grant, member, Optional.empty()); } }
    private record Decision(GrantSnapshot grant, UUID admissionId, AdmissionWitness witness, Instant now, AccessEventRow priorSuccess,
            String requestKeyDigest, String eventType, String preProviderFingerprint, List<AccessEventRow> prior, AuthorizedMediaFacts facts) { }
}
