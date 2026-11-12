package com.onecall.core.evidence.customerevidencequery;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface CustomerEvidenceReader {
    Set<UUID> findSubjectIdsWithEvidence(Set<UUID> ownedSubjectIds);
    List<Header> fetchPage(Set<UUID> subjectIds, Optional<Anchor> anchor, int fetchSize);
    Map<UUID, List<String>> findArtifactRoles(Set<UUID> evidenceIds);
    Optional<Header> findHeader(UUID evidenceId);
    List<Artifact> findArtifacts(UUID evidenceId);

    record Anchor(Instant createdAt, UUID evidenceId) { }
    record Header(UUID evidenceId, UUID subjectId, String status, String sourceType,
                  String capturedAtText, String consentVersion, Instant createdAt,
                  Instant solidifiedAt) { }
    record Artifact(String artifactRole, String mimeType, Long sizeBytes,
                    Integer validatedDurationSeconds, String sha256) { }
}
