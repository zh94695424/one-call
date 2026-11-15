package com.onecall.core.operations_iam.subjectread;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class OperationsActivationReadPorts {
    private OperationsActivationReadPorts() { }

    public interface ActivationHistoryReader {
        List<TransitionFacts> fetch(Optional<Anchor> anchor, int fetchSize);
    }

    public record Anchor(Instant occurredAt, UUID transitionId) {
        @Override public String toString() { return "Anchor[REDACTED]"; }
    }

    public record TransitionFacts(UUID transitionId, UUID subjectId, String statusEventId, String fromStatus,
            String toStatus, Integer fromRevision, Integer toRevision, String sourceChannel,
            String sourceActionDomain, String reasonCode, Instant occurredAt) {
        @Override public String toString() { return "TransitionFacts[REDACTED]"; }
    }
}
