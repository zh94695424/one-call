package com.onecall.core.evidence.customerquery;

import java.util.UUID;

public interface CustomerSubjectEvidenceCountReader {
    long countForSubject(UUID subjectId);
}
