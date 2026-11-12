package com.onecall.core.customer_iam.internal.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CustomerEvidenceDtos {
    private CustomerEvidenceDtos() { }

    public record EvidencePage(List<EvidenceSummary> items, String nextCursor, boolean hasMore) {
        public EvidencePage { items = List.copyOf(items); }
        public static EvidencePage from(CustomerEvidenceQueryService.Page page, String cursor) {
            return new EvidencePage(page.items().stream().map(value -> new EvidenceSummary(value.evidenceId(),
                    value.subjectId(), value.status(), value.sourceType(), value.capturedAt(), value.consentVersion(),
                    value.createdAt(), value.artifactRoles())).toList(), cursor, page.hasMore());
        }
    }
    public record EvidenceSummary(UUID evidenceId, UUID subjectId, String status, String sourceType,
                                  Instant capturedAt, String consentVersion, Instant createdAt, List<String> artifactRoles) {
        public EvidenceSummary { artifactRoles = List.copyOf(artifactRoles); }
    }
    public record EvidenceDetail(UUID evidenceId, UUID subjectId, String status, String sourceType,
                                 Instant capturedAt, String consentVersion,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) Instant solidifiedAt,
                                 List<EvidenceArtifact> artifacts) {
        public EvidenceDetail { artifacts = List.copyOf(artifacts); }
        public static EvidenceDetail from(CustomerEvidenceQueryService.Detail value) {
            return new EvidenceDetail(value.evidenceId(), value.subjectId(), value.status(), value.sourceType(),
                    value.capturedAt(), value.consentVersion(), value.solidifiedAt(), value.artifacts().stream()
                    .map(artifact -> new EvidenceArtifact(artifact.artifactRole(), artifact.mimeType(), artifact.sizeBytes(),
                            artifact.validatedDurationSeconds(), artifact.sha256())).toList());
        }
    }
    public record EvidenceArtifact(String artifactRole, String mimeType, Long sizeBytes,
                                   @JsonInclude(JsonInclude.Include.NON_NULL) Integer validatedDurationSeconds,
                                   String sha256) { }
}
