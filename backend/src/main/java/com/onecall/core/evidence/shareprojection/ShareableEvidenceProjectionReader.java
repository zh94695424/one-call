package com.onecall.core.evidence.shareprojection;

import java.util.Objects;
import java.util.UUID;
import org.jooq.DSLContext;

public interface ShareableEvidenceProjectionReader {
    ReadResult readShareableProjection(DSLContext transactionDsl, UUID evidenceId);

    sealed interface ReadResult {
        record Found(ShareableEvidenceProjection projection) implements ReadResult {
            public Found { Objects.requireNonNull(projection); }
        }
        record Missing() implements ReadResult { }
        record Unavailable() implements ReadResult { }
    }
}
