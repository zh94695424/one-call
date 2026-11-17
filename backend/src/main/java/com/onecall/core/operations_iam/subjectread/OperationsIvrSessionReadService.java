package com.onecall.core.operations_iam.subjectread;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import java.time.Instant;
import java.util.*;

public interface OperationsIvrSessionReadService {
    IvrSessionPage list(Caller caller, RawListRequest request);
    IvrSessionDetail detail(Caller caller, RawDetailRequest request);

    record RawListRequest(Map<String, List<String>> query, List<String> accept) {
        public RawListRequest { query = copyQuery(query); accept = copyValues(accept); }
        @Override public String toString() { return "RawListRequest[REDACTED]"; }
    }
    record RawDetailRequest(String sessionId, Map<String, List<String>> query, List<String> accept) {
        public RawDetailRequest { query = copyQuery(query); accept = copyValues(accept); }
        @Override public String toString() { return "RawDetailRequest[REDACTED]"; }
    }
    private static Map<String, List<String>> copyQuery(Map<String, List<String>> query) {
        if (query == null) return null;
        var copy = new LinkedHashMap<String, List<String>>();
        query.forEach((key, values) -> copy.put(key, copyValues(values)));
        return Collections.unmodifiableMap(copy);
    }
    private static List<String> copyValues(List<String> values) {
        return values == null ? null : Collections.unmodifiableList(new ArrayList<>(values));
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IvrSessionSummary(UUID sessionId, String callId, UUID subjectId, String status, String nodeId,
            String protocolVersion, String promptVersion, String guidanceStatus, String callerMasked,
            String accessNumberMasked, Instant createdAt, Instant confirmedAt) { }
    record IvrSessionPage(List<IvrSessionSummary> items,
            @JsonInclude(JsonInclude.Include.ALWAYS) String nextCursor, boolean hasMore) {
        public IvrSessionPage { items = List.copyOf(items); }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IvrSessionDetail(UUID sessionId, String callId, UUID subjectId, String status, String nodeId,
            String protocolVersion, String promptVersion, String guidanceStatus, String callerMasked,
            String accessNumberMasked, Instant createdAt, Instant confirmedAt, IvrCallSummary call,
            List<IvrDecisionSummary> decisions, List<IvrLegSummary> legs,
            List<IvrCallEventSummary> callEvents, List<IvrEvidenceSummary> evidence) {
        public IvrSessionDetail {
            decisions = List.copyOf(decisions); legs = List.copyOf(legs);
            callEvents = List.copyOf(callEvents); evidence = List.copyOf(evidence);
        }
    }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IvrCallSummary(String status, Instant startedAt, Instant endedAt, Integer authoritativeSeconds) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IvrDecisionSummary(UUID decisionId, String purpose, @JsonInclude(JsonInclude.Include.ALWAYS) String action,
            String reasonCode, UUID reservationId, Instant decidedAt) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IvrLegSummary(String legId, String legType, String direction, String state, Instant startedAt,
            Instant answeredAt, Instant endedAt, String endReason) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IvrCallEventSummary(String eventId, String eventType, Long sequenceNo, Instant occurredAt,
            String processingStatus, String reasonCode) { }
    record IvrEvidenceSummary(UUID evidenceId, String status, List<String> artifactRoles) {
        public IvrEvidenceSummary { artifactRoles = List.copyOf(artifactRoles); }
    }
}
