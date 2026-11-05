package com.onecall.core.evidence.shareprojection;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jooq.JSONB;

public record ShareableEvidenceProjection(UUID evidenceId, UUID subjectId, String status,
        String sourceType, String sourceRefId, JSONB identityAtCapture, List<ArtifactProjection> artifacts) {
    public ShareableEvidenceProjection {
        Objects.requireNonNull(evidenceId);
        Objects.requireNonNull(subjectId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(sourceType);
        Objects.requireNonNull(sourceRefId);
        Objects.requireNonNull(identityAtCapture);
        artifacts = List.copyOf(artifacts);
    }

    public record ArtifactProjection(String artifactRole, String mediaSha256, long mediaSizeBytes,
            String mediaMimeType, Integer validatedDurationSeconds) {
        public ArtifactProjection {
            Objects.requireNonNull(artifactRole);
            Objects.requireNonNull(mediaSha256);
            Objects.requireNonNull(mediaMimeType);
        }
    }
}
