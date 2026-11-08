package com.onecall.core.evidence.internal.mediaread;

import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.evidence.mediaread.ResolvedMediaObject;
import java.util.Objects;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.transaction.support.TransactionTemplate;

public class JooqEvidenceMediaObjectReader implements EvidenceMediaObjectReader {
    private final DSLContext dsl;
    private final TransactionTemplate transactions;

    public JooqEvidenceMediaObjectReader(DSLContext dsl, TransactionTemplate transactions) {
        this.dsl = Objects.requireNonNull(dsl);
        this.transactions = Objects.requireNonNull(transactions);
    }

    @Override
    public Result resolve(UUID evidenceId) {
        Objects.requireNonNull(evidenceId);
        try {
            Result result = transactions.execute(status -> read(evidenceId));
            return result == null ? new Result.Unavailable() : result;
        } catch (RuntimeException failure) {
            return new Result.Unavailable();
        }
    }

    private Result read(UUID evidenceId) {
        var rows = dsl.fetch("SELECT er.id er_id, er.source_type, er.source_ref_id, er.status er_status, "
                + "a.evidence_id, a.media_object_id, a.artifact_role, m.id media_id, m.sha256, m.size_bytes, "
                + "m.mime_type, r.id recording_id, r.media_object_id recording_media_id, r.status recording_status, "
                + "r.validated_duration FROM evidence.evidence_record er "
                + "LEFT JOIN evidence.evidence_artifact a ON a.evidence_id = er.id AND a.artifact_role = 'ORIGINAL' "
                + "LEFT JOIN evidence.media_object m ON m.id = a.media_object_id "
                + "LEFT JOIN evidence.recording r ON er.source_type = 'CALL_RECORDING' "
                + "AND r.id = er.source_ref_id AND r.media_object_id = m.id WHERE er.id = ?", evidenceId);
        if (rows.isEmpty()) return new Result.Missing();
        if (rows.size() != 1) return new Result.Unavailable();
        Record row = rows.get(0);
        UUID evidence = row.get("er_id", UUID.class);
        UUID artifactEvidence = row.get("evidence_id", UUID.class);
        UUID artifactMedia = row.get("media_object_id", UUID.class);
        UUID mediaId = row.get("media_id", UUID.class);
        UUID sourceRef = row.get("source_ref_id", UUID.class);
        UUID recordingId = row.get("recording_id", UUID.class);
        UUID recordingMedia = row.get("recording_media_id", UUID.class);
        String sourceType = row.get("source_type", String.class);
        String status = row.get("er_status", String.class);
        String role = row.get("artifact_role", String.class);
        String recordingStatus = row.get("recording_status", String.class);
        String hash = row.get("sha256", String.class);
        Long size = row.get("size_bytes", Long.class);
        String mime = row.get("mime_type", String.class);
        Integer duration = row.get("validated_duration", Integer.class);
        if (!evidenceId.equals(evidence) || !"SOLIDIFIED".equals(status) || !"CALL_RECORDING".equals(sourceType)
                || sourceRef == null || !evidence.equals(artifactEvidence) || !"ORIGINAL".equals(role)
                || artifactMedia == null || !artifactMedia.equals(mediaId) || !sourceRef.equals(recordingId)
                || !mediaId.equals(recordingMedia) || !"VALIDATED".equals(recordingStatus)
                || hash == null || size == null || mime == null) return new Result.Unavailable();
        return new Result.Found(new ResolvedMediaObject(mediaId, hash, size, mime, duration));
    }

    @Override public String toString() { return "JooqEvidenceMediaObjectReader[REDACTED]"; }
}
