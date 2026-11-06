package com.onecall.core.sharing.internal.publicquery;

import static com.onecall.core.sharing.internal.publicquery.PublicShareQueryException.Code.*;

import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjection;
import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjection.ArtifactProjection;
import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import com.onecall.core.sharing.internal.persistence.PublicShareQueryRepository;
import com.onecall.core.sharing.internal.persistence.PublicShareQueryRepository.PublicGrant;
import com.onecall.core.sharing.internal.provider.Sha256Provider;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class PublicShareQueryService {
    @FunctionalInterface
    public interface MemberAccess { MemberMatch check(UUID grantOrganizationId); }
    public enum MemberMatch { MATCH, NONMATCH, UNAUTHENTICATED, UNAVAILABLE, INTERNAL }

    private final TransactionTemplate transaction;
    private final DSLContext context;
    private final PublicShareQueryRepository repository;
    private final ShareableEvidenceProjectionReader reader;
    private final Sha256Provider sha256;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public PublicShareQueryService(@Qualifier("publicShareQueryTransactionTemplate") TransactionTemplate transaction,
            @Qualifier("sharingCommandDsl") DSLContext context,
            @Qualifier("publicShareQueryRepository") PublicShareQueryRepository repository,
            @Qualifier("shareableEvidenceProjectionReader") ShareableEvidenceProjectionReader reader,
            @Qualifier("sha256Provider") Sha256Provider sha256) {
        this.transaction = transaction;
        this.context = context;
        this.repository = repository;
        this.reader = reader;
        this.sha256 = sha256;
    }

    public PublicShareView getPublicShare(OpaqueCapabilityToken token, MemberAccess memberAccess) {
        if (token == null) throw error(VALIDATION_FAILED);
        try {
            String digest = digest(token);
            PublicShareView result = transaction.execute(status -> {
                context.execute("SET TRANSACTION READ ONLY");
                requireReadOnly();
                sharingRole();
                PublicGrant grant = repository.findByTokenDigest(digest).orElseThrow(() -> error(SHARE_TOKEN_INVALID));
                Instant now = repository.sampleDatabaseTime();
                require(grant.evidenceId() != null && grant.ownerOrganizationId() != null && grant.status() != null
                        && grant.verificationMode() != null && grant.validFrom() != null && grant.expiresAt() != null
                        && grant.expiresAt().isAfter(grant.validFrom()) && now != null);
                if (grant.status() == PublicShareView.Status.REVOKED) throw error(SHARE_REVOKED);
                if (grant.status() == PublicShareView.Status.EXPIRED || !now.isBefore(grant.expiresAt())) throw error(SHARE_EXPIRED);
                if (now.isBefore(grant.validFrom())) throw error(SHARE_TOKEN_INVALID);
                JsonNode snapshot = metadata(grant);
                if (grant.verificationMode() == PublicShareView.VerificationMode.ORGANIZATION_MEMBER) {
                    require(grant.ownerOrganizationId().isPresent() && memberAccess != null);
                    final MemberMatch match;
                    try { match = memberAccess.check(grant.ownerOrganizationId().orElseThrow()); }
                    catch (RuntimeException failure) { throw error(SERVICE_UNAVAILABLE); }
                    require(match != null);
                    switch (match) {
                        case NONMATCH -> {
                            return new PublicShareView(grant.verificationMode(), grant.status(), grant.expiresAt(),
                                    grant.downloadAllowed(), null, new PublicShareView.VerificationChallenge(true));
                        }
                        case UNAUTHENTICATED -> throw error(UNAUTHENTICATED);
                        case UNAVAILABLE -> throw error(SERVICE_UNAVAILABLE);
                        case INTERNAL -> throw error(INTERNAL);
                        case MATCH -> { }
                    }
                }
                var content = content(grant, snapshot);
                return new PublicShareView(grant.verificationMode(), grant.status(), grant.expiresAt(),
                        grant.downloadAllowed(), content, null);
            });
            require(result != null);
            return result;
        } catch (PublicShareQueryException known) {
            throw error(known.code());
        } catch (RuntimeException failure) {
            throw error(SERVICE_UNAVAILABLE);
        }
    }

    private JsonNode metadata(PublicGrant grant) {
        JsonNode snapshot = mapper.readTree(grant.contentSnapshot());
        require(snapshot != null && snapshot.isObject() && snapshot.size() == 3);
        var content = snapshot.path("content");
        var creation = snapshot.path("creation");
        var verification = snapshot.path("verification");
        require(content.isObject() && content.size() == 7 && creation.isObject() && creation.size() == 11 && verification.isObject()
                && content.path("schemaVersion").isIntegralNumber() && content.path("schemaVersion").asInt() == 1
                && creation.path("schemaVersion").isIntegralNumber() && creation.path("schemaVersion").asInt() == 1
                && grant.evidenceId().toString().equals(text(content, "evidenceId"))
                && grant.evidenceId().toString().equals(text(creation, "evidenceId"))
                && grant.verificationMode().name().equals(text(verification, "mode"))
                && grant.verificationMode().name().equals(text(creation, "verificationMode"))
                && creation.path("downloadAllowed").isBoolean()
                && creation.path("downloadAllowed").asBoolean() == grant.downloadAllowed()
                && !timestamp(text(creation, "expiresAt")).isAfter(grant.expiresAt()));
        UUID.fromString(text(creation, "creatorAccountId"));
        for (String name : List.of("scopeDigest", "creationRequestDigest", "creationFingerprint"))
            require(text(creation, name).matches("[0-9a-f]{64}"));
        var maximum = creation.path("maxAdmissions");
        require(maximum.isNull() || maximum.isIntegralNumber() && maximum.canConvertToInt() && maximum.asInt() > 0);
        if (grant.verificationMode() == PublicShareView.VerificationMode.DESIGNATED_MOBILE) {
            require(verification.size() == 3 && text(verification, "mobileHmac").matches("[0-9a-f]{64}")
                    && !text(verification, "keyVersion").isBlank() && text(verification, "keyVersion").length() <= 64);
        } else require(verification.size() == 1);
        if (grant.ownerOrganizationId().isPresent()) {
            require(grant.ownerOrganizationId().get().toString().equals(text(creation, "ownerOrganizationId")));
        } else {
            require(creation.has("ownerOrganizationId") && creation.path("ownerOrganizationId").isNull());
        }
        return content;
    }

    private PublicShareView.Content content(PublicGrant grant, JsonNode content) {
        ShareableEvidenceProjectionReader.ReadResult result;
        try {
            context.execute("SET LOCAL ROLE evidence_role");
            role("evidence_role");
            requireReadOnly();
            try { result = reader.readShareableProjection(context, grant.evidenceId()); }
            catch (RuntimeException failure) { throw error(SERVICE_UNAVAILABLE); }
        } finally {
            sharingRole();
            requireReadOnly();
        }
        if (result instanceof ShareableEvidenceProjectionReader.ReadResult.Missing) throw error(FORBIDDEN);
        require(result instanceof ShareableEvidenceProjectionReader.ReadResult.Found);
        ShareableEvidenceProjection projection = ((ShareableEvidenceProjectionReader.ReadResult.Found) result).projection();
        require(projection != null);
        if (List.of("PENDING", "FAILED", "QUARANTINED").contains(projection.status())) throw error(FORBIDDEN);
        require("SOLIDIFIED".equals(projection.status()) && grant.evidenceId().equals(projection.evidenceId())
                && projection.subjectId().toString().equals(text(content, "subjectId"))
                && projection.sourceType().equals(text(content, "sourceType"))
                && projection.sourceRefId().equals(text(content, "sourceRefId")));
        JsonNode identity = mapper.readTree(projection.identityAtCapture().data());
        require(identity != null && identity.isObject() && identity.equals(content.path("identityAtCapture")));
        Instant capturedAt = timestamp(text(identity, "capturedAt"));
        var artifacts = new ArrayList<>(projection.artifacts());
        artifacts.sort(Comparator.comparingInt((ArtifactProjection artifact) -> artifact.artifactRole().equals("ORIGINAL") ? 0 : 1)
                .thenComparing(ArtifactProjection::mediaSha256).thenComparingLong(ArtifactProjection::mediaSizeBytes)
                .thenComparing(ArtifactProjection::mediaMimeType));
        require(artifacts.stream().filter(artifact -> artifact.artifactRole().equals("ORIGINAL")).count() == 1);
        JsonNode frozen = content.path("artifacts");
        require(frozen.isArray() && frozen.size() == artifacts.size());
        var seen = new HashSet<List<Object>>();
        var projected = new ArrayList<PublicShareView.Artifact>();
        Integer duration = null;
        for (int index = 0; index < artifacts.size(); index++) {
            var artifact = artifacts.get(index);
            var item = frozen.get(index);
            var artifactRole = PublicShareView.ArtifactRole.valueOf(artifact.artifactRole());
            require(artifact.mediaSha256().matches("[0-9a-f]{64}") && artifact.mediaSizeBytes() >= 0
                    && !artifact.mediaMimeType().isBlank() && artifact.mediaMimeType().length() <= 128
                    && (artifact.validatedDurationSeconds() == null || artifact.validatedDurationSeconds() >= 0)
                    && seen.add(List.of(artifact.artifactRole(), artifact.mediaSha256(), artifact.mediaSizeBytes(), artifact.mediaMimeType()))
                    && item.isObject() && item.size() == 5
                    && artifact.artifactRole().equals(text(item, "artifactRole"))
                    && artifact.mediaSha256().equals(text(item, "mediaSha256"))
                    && artifact.mediaMimeType().equals(text(item, "mediaMimeType"))
                    && item.path("mediaSizeBytes").isIntegralNumber() && item.path("mediaSizeBytes").canConvertToLong()
                    && item.path("mediaSizeBytes").asLong() == artifact.mediaSizeBytes());
            var frozenDuration = item.path("validatedDurationSeconds");
            require(artifact.validatedDurationSeconds() == null ? frozenDuration.isNull()
                    : frozenDuration.isIntegralNumber() && frozenDuration.canConvertToInt()
                    && frozenDuration.asInt() == artifact.validatedDurationSeconds());
            if (artifactRole == PublicShareView.ArtifactRole.ORIGINAL) duration = artifact.validatedDurationSeconds();
            projected.add(new PublicShareView.Artifact(artifactRole, artifact.mediaMimeType(), artifact.mediaSizeBytes(), artifact.mediaSha256()));
        }
        return new PublicShareView.Content(capturedAt, duration, projected);
    }

    private void sharingRole() {
        context.execute("SET LOCAL ROLE sharing_command_role");
        role("sharing_command_role");
    }

    private void role(String expected) {
        var row = context.fetchOne("SELECT current_user");
        require(row != null && expected.equals(row.get(0, String.class)));
    }

    private void requireReadOnly() {
        var row = context.fetchOne("SELECT current_setting('transaction_read_only')");
        require(row != null && "on".equals(row.get(0, String.class)));
    }

    private static String text(JsonNode object, String name) {
        require(object.path(name).isString());
        return object.path(name).asString();
    }

    private static Instant timestamp(String value) { return OffsetDateTime.parse(value).toInstant(); }
    private String digest(OpaqueCapabilityToken token) {
        try {
            String value = sha256.digest(token.utf8());
            require(value != null && value.matches("[0-9a-f]{64}"));
            return value;
        } catch (RuntimeException failure) { throw error(SERVICE_UNAVAILABLE); }
    }
    private static void require(boolean valid) { if (!valid) throw error(SERVICE_UNAVAILABLE); }
    private static PublicShareQueryException error(PublicShareQueryException.Code code) { return new PublicShareQueryException(code); }
}
