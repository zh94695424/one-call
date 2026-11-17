package com.onecall.core.operations_iam.subjectread;

import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import java.time.Instant;
import java.util.*;

public final class OperationsIvrSessionReadPorts {
    private OperationsIvrSessionReadPorts() { }
    public interface IvrSessionReader {
        List<IvrSessionSummary> fetch(Filters filters, Optional<Anchor> anchor, int fetchLimit);
        Optional<DetailFacts> detail(UUID sessionId);
    }
    public interface IvrEvidenceReader { List<EvidenceFacts> find(UUID sessionId, String callId); }
    public record Filters(UUID subjectId, String callId, String taskId, String status) {
        @Override public String toString() { return "Filters[REDACTED]"; }
    }
    public record Anchor(Instant createdAt, UUID sessionId) {
        @Override public String toString() { return "Anchor[REDACTED]"; }
    }
    public record CallFacts(UUID sessionId, String callId, IvrCallSummary call) { }
    public record DecisionFacts(UUID sessionId, String callId, IvrDecisionSummary decision) { }
    public record EvidenceFacts(UUID evidenceId, String status, UUID sessionId, String callId, List<String> artifactRoles) {
        public EvidenceFacts { if (artifactRoles != null) artifactRoles = List.copyOf(artifactRoles); }
    }
    public record DetailFacts(IvrSessionSummary session, List<CallFacts> calls, List<DecisionFacts> decisions,
            List<IvrLegSummary> legs, List<IvrCallEventSummary> callEvents) {
        public DetailFacts {
            calls = List.copyOf(calls); decisions = List.copyOf(decisions);
            legs = List.copyOf(legs); callEvents = List.copyOf(callEvents);
        }
    }
}
