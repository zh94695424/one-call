package com.onecall.core.sharing.internal.commandservice;

import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjection;
import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjection.ArtifactProjection;
import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus;
import com.onecall.core.sharing.internal.application.ShareSummary.VerificationMode;
import com.onecall.core.sharing.internal.command.CustomerShareCommand;
import com.onecall.core.sharing.internal.command.CustomerShareCommandResult;
import com.onecall.core.sharing.internal.command.CustomerShareCommandResult.ShareState;
import com.onecall.core.sharing.internal.command.CustomerShareCommandService;
import com.onecall.core.sharing.internal.command.ShareCommandCaller;
import com.onecall.core.sharing.internal.command.ShareCommandException;
import com.onecall.core.sharing.internal.command.ShareCommandException.Code;
import com.onecall.core.sharing.internal.persistence.SharingCommandRepository;
import com.onecall.core.sharing.internal.persistence.SharingCommandRepository.AuditFact;
import com.onecall.core.sharing.internal.persistence.SharingCommandRepository.OutboxFact;
import com.onecall.core.sharing.internal.persistence.SharingCommandRepository.ShareGrant;
import com.onecall.core.sharing.internal.provider.CapabilityTokenProvider;
import com.onecall.core.sharing.internal.provider.DesignatedMobileBindingProvider;
import com.onecall.core.sharing.internal.provider.ExtractionCodeMaterial;
import com.onecall.core.sharing.internal.provider.ProviderResult;
import com.onecall.core.sharing.internal.provider.Sha256Provider;
import com.onecall.core.sharing.internal.provider.ShareCreationPolicyProvider;
import com.onecall.core.sharing.internal.provider.ShareCreationPolicyProvider.CreationPolicyDecision;
import com.onecall.core.sharing.internal.provider.ShareCreationPolicyProvider.CreationPolicyRequest;
import com.onecall.core.sharing.internal.provider.ShareExtractionCodeProvider;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public final class DefaultCustomerShareCommandService implements CustomerShareCommandService {
    private final TransactionTemplate template;
    private final DSLContext context;
    private final SharingCommandRepository repository;
    private final ShareableEvidenceProjectionReader reader;
    private final ShareCreationPolicyProvider policy;
    private final CapabilityTokenProvider tokens;
    private final ShareExtractionCodeProvider codes;
    private final DesignatedMobileBindingProvider mobiles;
    private final Sha256Provider sha256;
    private static final JsonMapper mapper = JsonMapper.builder().disable(JsonWriteFeature.WRITE_HEX_UPPER_CASE).build();

    public DefaultCustomerShareCommandService(TransactionTemplate template, DSLContext context,
            SharingCommandRepository repository, ShareableEvidenceProjectionReader reader,
            ShareCreationPolicyProvider policy, CapabilityTokenProvider tokens, ShareExtractionCodeProvider codes,
            DesignatedMobileBindingProvider mobiles, Sha256Provider sha256) {
        this.template = Objects.requireNonNull(template);
        this.context = Objects.requireNonNull(context);
        this.repository = Objects.requireNonNull(repository);
        this.reader = Objects.requireNonNull(reader);
        this.policy = Objects.requireNonNull(policy);
        this.tokens = Objects.requireNonNull(tokens);
        this.codes = Objects.requireNonNull(codes);
        this.mobiles = Objects.requireNonNull(mobiles);
        this.sha256 = Objects.requireNonNull(sha256);
    }

    @Override
    public CustomerShareCommandResult execute(CustomerShareCommand command, ShareCommandCaller caller) {
        if (!(command instanceof CustomerShareCommand.Create create) || caller == null) throw error(Code.VALIDATION_FAILED);
        validate(create);
        try { return Objects.requireNonNull(template.execute(status -> create(create, caller))); }
        catch (ShareCommandException failure) { throw failure; }
        catch (org.springframework.transaction.CannotCreateTransactionException failure) { throw error(Code.SERVICE_UNAVAILABLE); }
        catch (RuntimeException failure) { throw error(databaseFailureCode(failure)); }
    }

    private static Code databaseFailureCode(RuntimeException failure) {
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        var pending = new java.util.ArrayDeque<Throwable>();
        pending.add(failure);
        boolean materialViolation = false;
        while (!pending.isEmpty()) {
            Throwable current = pending.removeFirst();
            if (!seen.add(current)) continue;
            if (current instanceof SQLException sql) {
                if ("23505".equals(sql.getSQLState())) return Code.INTERNAL;
                if ("23514".equals(sql.getSQLState())) materialViolation = true;
                if ("P0001".equals(sql.getSQLState()) && sql.getMessage() != null) {
                    String guard = sql.getMessage().split("\\R", 2)[0].replaceFirst("^ERROR: ", "");
                    if (java.util.Set.of("SHARE_GRANT_IMMUTABLE", "SHARE_GRANT_EXPIRY_DECREASE",
                            "SHARE_GRANT_GENERATION_INVALID", "SHARE_GRANT_CODE_ROTATION_INVALID",
                            "SHARE_GRANT_STATUS_TERMINAL", "SHARE_GRANT_STATUS_TRANSITION",
                            "SHARE_GRANT_EXPIRY_INVALID", "SHARE_GRANT_EXTENSION_INVALID",
                            "SHARE_GRANT_REVISION_INVALID", "SHARE_ADMISSION_REFUSED",
                            "SHARING_ROW_APPEND_ONLY").contains(guard)) materialViolation = true;
                }
                if (sql.getNextException() != null) pending.add(sql.getNextException());
            }
            if (current.getCause() != null) pending.add(current.getCause());
        }
        return materialViolation ? Code.SERVICE_UNAVAILABLE : Code.INTERNAL;
    }

    private CustomerShareCommandResult create(CustomerShareCommand.Create command, ShareCommandCaller caller) {
        repository.assertCommandRole(context);
        repository.acquireCreatorWorkspaceLocks(context, caller);
        var commandContext = repository.readCommandContext(context, caller, command.evidenceId())
                .orElseThrow(() -> error(Code.FORBIDDEN));
        var reference = commandContext.evidenceReference();
        if (!"SOLIDIFIED".equals(reference.evidenceStatus())) throw error(Code.FORBIDDEN);
        ShareableEvidenceProjectionReader.ReadResult read;
        try {
            repository.switchToEvidenceRole(context);
            try { read = reader.readShareableProjection(context, command.evidenceId()); }
            catch (RuntimeException failure) { throw error(Code.SERVICE_UNAVAILABLE); }
        } finally { repository.restoreAndAssertCommandRole(context); }
        if (read instanceof ShareableEvidenceProjectionReader.ReadResult.Missing) throw error(Code.FORBIDDEN);
        if (!(read instanceof ShareableEvidenceProjectionReader.ReadResult.Found found)) throw error(Code.SERVICE_UNAVAILABLE);
        ShareableEvidenceProjection projection = found.projection();
        if (!"SOLIDIFIED".equals(projection.status())) throw error(Code.FORBIDDEN);
        if (!reference.evidenceId().equals(projection.evidenceId()) || !reference.subjectId().equals(projection.subjectId())
                || !reference.evidenceStatus().equals(projection.status())
                || !tree(reference.identityAtCapture()).equals(tree(projection.identityAtCapture()))) throw error(Code.SERVICE_UNAVAILABLE);
        Instant occurredAt = repository.sampleDatabaseTime(context);
        if (occurredAt == null || occurredAt.getNano() % 1000 != 0) throw error(Code.SERVICE_UNAVAILABLE);
        if (!command.expiresAt().isAfter(occurredAt)
                || (command.verificationMode() == VerificationMode.ORGANIZATION_MEMBER && caller.organizationId().isEmpty()))
            throw error(Code.VALIDATION_FAILED);
        ObjectNode verification = mapper.createObjectNode().put("mode", command.verificationMode().name());
        if (command.verificationMode() == VerificationMode.DESIGNATED_MOBILE) {
            var material = mobile(command.designatedMobile());
            verification.put("mobileHmac", material.mobileHmac()).put("keyVersion", material.keyVersion());
        }
        ObjectNode content = content(projection);
        ObjectNode scope = mapper.createObjectNode();
        scope.set("contentObject", content);
        scope.set("verificationObject", verification);
        String scopeDigest = digest(scope);
        String requestDigest = hash(command.creationRequestId());
        ObjectNode creation = mapper.createObjectNode().put("schemaVersion", 1).put("creatorAccountId", caller.accountId().toString());
        if (caller.organizationId().isPresent()) creation.put("ownerOrganizationId", caller.organizationId().get().toString());
        else creation.putNull("ownerOrganizationId");
        creation.put("evidenceId", command.evidenceId().toString()).put("verificationMode", command.verificationMode().name())
                .put("downloadAllowed", command.downloadAllowed()).put("expiresAt", utc(command.expiresAt()));
        if (command.maxAdmissions() == null) creation.putNull("maxAdmissions");
        else creation.put("maxAdmissions", command.maxAdmissions());
        creation.put("scopeDigest", scopeDigest).put("creationRequestDigest", requestDigest);
        String fingerprint = digest(creation);
        creation.put("creationFingerprint", fingerprint);
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.set("content", content);
        snapshot.set("verification", verification);
        snapshot.set("creation", creation);
        ObjectNode complete = scope.deepCopy();
        complete.set("creationObject", creation);
        String contentHash = digest(complete);
        var risk = repository.readRiskOwnership(context, reference).orElseThrow(() -> error(Code.SHARE_RISK_BLOCKED));
        if (!risk.subjectId().equals(reference.subjectId()) || !risk.ownerResolved() || risk.openRisk()
                || !(risk.resolution().equals("CAPTURED") || risk.resolution().equals("CURRENT"))) throw error(Code.SHARE_RISK_BLOCKED);
        boolean owned = caller.organizationId().isPresent()
                ? caller.organizationId().equals(risk.subjectOwnerOrganizationId())
                : risk.subjectOwnerOrganizationId().isEmpty() && risk.ownerAccountId().equals(Optional.of(caller.accountId()));
        if (!owned) throw error(Code.FORBIDDEN);
        ProviderResult<CreationPolicyDecision> decision;
        try { decision = policy.evaluate(new CreationPolicyRequest(caller, command.evidenceId(), command.verificationMode(),
                caller.organizationId(), json(content), json(verification))); }
        catch (RuntimeException failure) { throw error(Code.SHARE_ACCESS_POLICY_UNAVAILABLE); }
        if (decision instanceof ProviderResult.Denied<CreationPolicyDecision>
                || decision instanceof ProviderResult.Allowed<CreationPolicyDecision> allowed && allowed.value() == CreationPolicyDecision.DENY)
            throw error(Code.SHARE_CREATION_CONFLICT);
        if (!(decision instanceof ProviderResult.Allowed<CreationPolicyDecision> allowed) || allowed.value() != CreationPolicyDecision.ALLOW)
            throw error(Code.SHARE_ACCESS_POLICY_UNAVAILABLE);
        String shareId = "share:" + digest(mapper.createObjectNode().put("creatorAccountId", caller.accountId().toString())
                .put("creationRequestId", command.creationRequestId()));
        var existing = repository.findCreationIdentity(context, shareId);
        if (existing.isPresent()) return replay(existing.get(), fingerprint, caller);
        UUID grantId = UUID.randomUUID();
        var token = token(grantId);
        ExtractionCodeMaterial code = command.verificationMode() == VerificationMode.EXTRACTION_CODE ? code(grantId) : null;
        if (repository.findGrantIdByTokenHash(context, token.sha256()).isPresent()) throw error(Code.INTERNAL);
        ShareGrant proposed = new ShareGrant(grantId, shareId, token.sha256(), command.evidenceId(), caller.accountId(),
                caller.organizationId().orElse(null), command.verificationMode(), code == null ? null : code.salt(),
                code == null ? null : code.codeHash(), code == null ? 0 : 1, occurredAt, command.expiresAt(), command.maxAdmissions(),
                0, command.downloadAllowed(), json(snapshot), contentHash, ShareStatus.ACTIVE, 0, 1);
        var inserted = repository.insertGrantReturning(context, proposed);
        if (inserted.isEmpty()) return replay(repository.findCreationIdentity(context, shareId)
                .orElseThrow(() -> error(Code.INTERNAL)), fingerprint, caller);
        ShareGrant grant = inserted.get();
        ObjectNode payload = mapper.createObjectNode().put("shareId", shareId).put("grantId", grantId.toString())
                .put("status", "ACTIVE").put("admissionsUsed", 0).put("codeGeneration", proposed.codeGeneration())
                .put("revocationGeneration", 0).put("revision", 1).put("creationFingerprint", fingerprint).put("occurredAt", utc(occurredAt));
        repository.appendOutbox(context, new OutboxFact("SHARE_ACCESS", "share-created:" + grantId,
                "EVIDENCE_SHARE_GRANT", grantId, "SHARE_CREATED", Optional.of(json(payload)), Optional.empty(), digest(payload)));
        repository.appendAudit(context, new AuditFact(occurredAt, "ACCOUNT", Optional.of(caller.accountId()), "SHARE_GRANT_CREATE",
                "EVIDENCE_SHARE_GRANT", Optional.of(grantId), Optional.empty(), Optional.empty(),
                Optional.of(json(mapper.createObjectNode().put("status", "ACTIVE").put("revision", 1))),
                Optional.of(requestDigest), command.traceId().map(UUID::toString)));
        return new CustomerShareCommandResult.Created(state(grant), token.plaintext(), Optional.ofNullable(code).map(ExtractionCodeMaterial::code));
    }

    private CustomerShareCommandResult replay(ShareGrant grant, String fingerprint, ShareCommandCaller caller) {
        JsonNode creation = tree(grant.contentSnapshot()).path("creation");
        if (!caller.accountId().equals(grant.createdByAccountId())
                || !caller.organizationId().equals(Optional.ofNullable(grant.grantOwnerOrganizationId()))
                || !fingerprint.equals(creation.path("creationFingerprint").asString())) throw error(Code.SHARE_CREATION_CONFLICT);
        return new CustomerShareCommandResult.CreationReplayed(state(grant));
    }

    private ShareState state(ShareGrant grant) {
        return new ShareState(grant.shareId(), grant.evidenceId(), grant.verificationMode(), grant.status(), grant.validFrom(),
                grant.expiresAt(), grant.maxAdmissions(), grant.admissionsUsed(), grant.downloadAllowed(), grant.codeGeneration(), grant.revocationGeneration());
    }

    private ObjectNode content(ShareableEvidenceProjection projection) {
        ObjectNode content = mapper.createObjectNode().put("schemaVersion", 1).put("evidenceId", projection.evidenceId().toString())
                .put("subjectId", projection.subjectId().toString()).put("sourceType", projection.sourceType()).put("sourceRefId", projection.sourceRefId());
        content.set("identityAtCapture", tree(projection.identityAtCapture()));
        var artifacts = content.putArray("artifacts");
        projection.artifacts().stream().sorted(Comparator.comparingInt((ArtifactProjection artifact) -> artifact.artifactRole().equals("ORIGINAL") ? 0 : 1)
                .thenComparing(ArtifactProjection::mediaSha256).thenComparingLong(ArtifactProjection::mediaSizeBytes)
                .thenComparing(ArtifactProjection::mediaMimeType)).forEach(artifact -> {
                    ObjectNode item = artifacts.addObject().put("artifactRole", artifact.artifactRole()).put("mediaSha256", artifact.mediaSha256())
                            .put("mediaSizeBytes", artifact.mediaSizeBytes()).put("mediaMimeType", artifact.mediaMimeType());
                    if (artifact.validatedDurationSeconds() == null) item.putNull("validatedDurationSeconds");
                    else item.put("validatedDurationSeconds", artifact.validatedDurationSeconds());
                });
        return content;
    }

    private DesignatedMobileBindingProvider.MobileHmacMaterial mobile(String value) {
        ProviderResult<DesignatedMobileBindingProvider.MobileHmacMaterial> result;
        try { result = mobiles.bind(value); }
        catch (RuntimeException failure) { throw error(Code.SERVICE_UNAVAILABLE); }
        if (result instanceof ProviderResult.Denied<?> denied && denied.code() == Code.VALIDATION_FAILED) throw error(Code.VALIDATION_FAILED);
        try {
            var material = requireMaterial(result);
            if (!material.mobileHmac().matches("[0-9a-f]{64}") || !length(material.keyVersion(), 1, 64)) throw error(Code.SERVICE_UNAVAILABLE);
            return material;
        } catch (RuntimeException failure) { throw error(Code.SERVICE_UNAVAILABLE); }
    }

    private CapabilityTokenProvider.CapabilityTokenMaterial token(UUID grantId) {
        try {
            var material = requireMaterial(tokens.issue(grantId));
            if (!length(material.plaintext(), 1, 512) || !material.sha256().matches("[0-9a-f]{64}")
                    || !hash(material.plaintext()).equals(material.sha256())) throw error(Code.SERVICE_UNAVAILABLE);
            return material;
        } catch (RuntimeException failure) { throw error(Code.SERVICE_UNAVAILABLE); }
    }

    private ExtractionCodeMaterial code(UUID grantId) {
        try {
            var material = requireMaterial(codes.generate(grantId, 1));
            if (!length(material.code(), 1, 128) || !material.salt().matches("[A-Za-z0-9_-]{22,256}")
                    || !material.codeHash().matches("[0-9a-f]{64}")) throw error(Code.SERVICE_UNAVAILABLE);
            return material;
        } catch (RuntimeException failure) { throw error(Code.SERVICE_UNAVAILABLE); }
    }

    private static <T> T requireMaterial(ProviderResult<T> result) {
        if (result instanceof ProviderResult.Allowed<T> allowed) return Objects.requireNonNull(allowed.value());
        throw error(Code.SERVICE_UNAVAILABLE);
    }

    private static void validate(CustomerShareCommand.Create command) {
        if (!length(command.creationRequestId(), 1, 128) || command.creationRequestId().isBlank()
                || command.expiresAt().getNano() % 1000 != 0 || command.maxAdmissions() != null && command.maxAdmissions() < 1
                || (command.verificationMode() == VerificationMode.DESIGNATED_MOBILE
                    ? !length(command.designatedMobile(), 1, 32) || command.designatedMobile().isBlank() : command.designatedMobile() != null))
            throw error(Code.VALIDATION_FAILED);
    }

    private static boolean length(String value, int minimum, int maximum) {
        return value != null && value.codePointCount(0, value.length()) >= minimum
                && value.codePointCount(0, value.length()) <= maximum && value.indexOf('\0') < 0
                && StandardCharsets.UTF_8.newEncoder().canEncode(value);
    }

    public static String utc(Instant instant) {
        return new DateTimeFormatterBuilder().appendInstant(instant.getNano() == 0 ? 0 : 6).toFormatter().format(instant);
    }

    private JsonNode tree(JSONB value) {
        try { return mapper.readTree(value.data()); }
        catch (RuntimeException failure) { throw error(Code.SERVICE_UNAVAILABLE); }
    }

    private JSONB json(JsonNode value) { return JSONB.valueOf(canonical(value)); }
    private String digest(JsonNode value) { return hash(canonical(value)); }
    private String hash(String value) {
        try {
            String digest = sha256.digest(value.getBytes(StandardCharsets.UTF_8));
            if (digest == null || !digest.matches("[0-9a-f]{64}")) throw error(Code.SERVICE_UNAVAILABLE);
            return digest;
        } catch (RuntimeException failure) { throw error(Code.SERVICE_UNAVAILABLE); }
    }

    public static String canonical(JsonNode value) {
        if (value.isObject()) {
            var sorted = new TreeMap<String, JsonNode>();
            value.properties().forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
            return "{" + sorted.entrySet().stream().map(entry -> quote(entry.getKey()) + ":" + canonical(entry.getValue()))
                    .collect(java.util.stream.Collectors.joining(",")) + "}";
        }
        if (value.isArray()) {
            var elements = new java.util.ArrayList<String>();
            value.forEach(element -> elements.add(canonical(element)));
            return "[" + String.join(",", elements) + "]";
        }
        if (value.isString()) return quote(value.asString());
        if (value.isNumber()) {
            double number = value.asDouble();
            if (!Double.isFinite(number)) throw error(Code.SERVICE_UNAVAILABLE);
            if (number == 0) return "0";
            var exact = new java.math.BigDecimal(number);
            var decimal = exact;
            for (int precision = 1; precision <= 17; precision++) {
                var candidate = exact.round(new java.math.MathContext(precision, java.math.RoundingMode.HALF_EVEN));
                if (Double.doubleToLongBits(candidate.doubleValue()) == Double.doubleToLongBits(number)) {
                    decimal = candidate.stripTrailingZeros();
                    break;
                }
            }
            double magnitude = Math.abs(number);
            if (magnitude >= 1e-6 && magnitude < 1e21) return decimal.toPlainString();
            String digits = decimal.unscaledValue().abs().toString();
            int exponent = decimal.precision() - decimal.scale() - 1;
            return (number < 0 ? "-" : "") + digits.charAt(0) + (digits.length() > 1 ? "." + digits.substring(1) : "")
                    + "e" + (exponent >= 0 ? "+" : "") + exponent;
        }
        if (value.isBoolean() || value.isNull()) return value.toString();
        throw error(Code.SERVICE_UNAVAILABLE);
    }

    private static String quote(String value) {
        if (!StandardCharsets.UTF_8.newEncoder().canEncode(value)) throw error(Code.SERVICE_UNAVAILABLE);
        return mapper.writeValueAsString(value);
    }

    private static ShareCommandException error(Code code) { return new ShareCommandException(code); }
}
