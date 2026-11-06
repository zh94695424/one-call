package com.onecall.core.sharing.internal.publicquery;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublicShareView(VerificationMode verificationMode, Status status, Instant expiresAt,
        boolean downloadAllowed, Content content, VerificationChallenge verificationChallenge) {
    public enum VerificationMode { NONE, EXTRACTION_CODE, DESIGNATED_MOBILE, ORGANIZATION_MEMBER }
    public enum Status { ACTIVE, EXPIRED, REVOKED, LOCKED }
    public enum ArtifactRole { ORIGINAL, DERIVED }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(Instant capturedAt, Integer validatedDurationSeconds, List<Artifact> artifacts) {
        public Content { artifacts = List.copyOf(artifacts); }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Artifact(ArtifactRole artifactRole, String mimeType, long sizeBytes, String sha256) { }

    public record VerificationChallenge(boolean customerSessionRequired) { }
}
