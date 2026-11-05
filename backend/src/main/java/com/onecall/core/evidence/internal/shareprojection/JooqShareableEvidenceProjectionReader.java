package com.onecall.core.evidence.internal.shareprojection;

import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjection;
import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjection.ArtifactProjection;
import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class JooqShareableEvidenceProjectionReader implements ShareableEvidenceProjectionReader {
    @Override
    @Transactional(transactionManager = "sharingCommandTransactionManager", propagation = Propagation.MANDATORY)
    public ReadResult readShareableProjection(DSLContext transactionDsl, UUID evidenceId) {
        Objects.requireNonNull(transactionDsl);
        Objects.requireNonNull(evidenceId);
        try {
            Record evidence = transactionDsl.fetchOne("SELECT id, subject_id, status, source_type, source_ref_id, identity_at_capture "
                    + "FROM evidence.evidence_record WHERE id = ?", evidenceId);
            if (evidence == null) return new ReadResult.Missing();
            var rows = transactionDsl.fetch("SELECT a.artifact_role, m.sha256, m.size_bytes, m.mime_type, r.validated_duration "
                    + "FROM evidence.evidence_artifact a JOIN evidence.media_object m ON m.id = a.media_object_id "
                    + "LEFT JOIN evidence.recording r ON r.media_object_id = a.media_object_id "
                    + "WHERE a.evidence_id = ?", evidenceId);
            var artifacts = new ArrayList<ArtifactProjection>();
            for (Record row : rows) {
                String role = Objects.requireNonNull(row.get("artifact_role", String.class));
                String hash = Objects.requireNonNull(row.get("sha256", String.class));
                long size = Objects.requireNonNull(row.get("size_bytes", Long.class));
                String mime = Objects.requireNonNull(row.get("mime_type", String.class));
                Integer duration = row.get("validated_duration", Integer.class);
                if ((!role.equals("ORIGINAL") && !role.equals("DERIVED")) || !hash.matches("[0-9a-f]{64}")
                        || size < 1 || mime.isBlank() || (duration != null && duration < 1)) return new ReadResult.Unavailable();
                ArtifactProjection artifact = new ArtifactProjection(role, hash, size, mime, duration);
                if (artifacts.stream().anyMatch(existing -> existing.artifactRole().equals(role)
                        && existing.mediaSha256().equals(hash) && existing.mediaSizeBytes() == size
                        && existing.mediaMimeType().equals(mime))) return new ReadResult.Unavailable();
                artifacts.add(artifact);
            }
            if (artifacts.stream().filter(artifact -> artifact.artifactRole().equals("ORIGINAL")).count() != 1)
                return new ReadResult.Unavailable();
            artifacts.sort(Comparator.comparingInt((ArtifactProjection artifact) -> artifact.artifactRole().equals("ORIGINAL") ? 0 : 1)
                    .thenComparing(ArtifactProjection::mediaSha256).thenComparingLong(ArtifactProjection::mediaSizeBytes)
                    .thenComparing(ArtifactProjection::mediaMimeType));
            return new ReadResult.Found(new ShareableEvidenceProjection(evidence.get("id", UUID.class),
                    evidence.get("subject_id", UUID.class), evidence.get("status", String.class),
                    evidence.get("source_type", String.class), evidence.get("source_ref_id", UUID.class).toString(),
                    evidence.get("identity_at_capture", JSONB.class), artifacts));
        } catch (RuntimeException failure) { return new ReadResult.Unavailable(); }
    }
}
