package com.onecall.core.operations_iam.subjectread;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OperationsActivationReadService {
    ActivationPage list(Caller caller, RawListRequest request);

    record RawListRequest(Map<String, List<String>> query, List<String> accept) {
        public RawListRequest {
            if (query != null) {
                var copy = new LinkedHashMap<String, List<String>>();
                query.forEach((key, values) -> copy.put(key, copyValues(values)));
                query = Collections.unmodifiableMap(copy);
            }
            accept = copyValues(accept);
        }
        @Override public String toString() { return "RawListRequest[REDACTED]"; }
    }

    private static List<String> copyValues(List<String> values) {
        return values == null ? null : Collections.unmodifiableList(new ArrayList<>(values));
    }

    record ActivationSummary(UUID transitionId, UUID subjectId, String statusEventId, String fromStatus,
            String toStatus, int fromRevision, int toRevision, String sourceChannel, String sourceActionDomain,
            String reasonCode, Instant occurredAt) { }

    record ActivationPage(List<ActivationSummary> items,
            @JsonInclude(JsonInclude.Include.ALWAYS) String nextCursor, boolean hasMore) {
        public ActivationPage { items = List.copyOf(items); }
    }
}
