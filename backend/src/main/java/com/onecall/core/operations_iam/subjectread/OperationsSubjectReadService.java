package com.onecall.core.operations_iam.subjectread;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OperationsSubjectReadService {
    SubjectPage list(Caller caller, RawListRequest request);
    OperationsSubjectDetail detail(Caller caller, RawDetailRequest request);

    record RawListRequest(Map<String, List<String>> query, List<String> accept) {
        public RawListRequest { query = copyQuery(query); accept = copyValues(accept); }
        @Override public String toString() { return "RawListRequest[REDACTED]"; }
    }
    record RawDetailRequest(String subjectId, Map<String, List<String>> query, List<String> accept) {
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
        return values == null ? null : Collections.unmodifiableList(new java.util.ArrayList<>(values));
    }

    record SubjectSummary(UUID subjectId, String source,
            @JsonInclude(JsonInclude.Include.NON_NULL) String maskedNumber,
            @JsonInclude(JsonInclude.Include.NON_NULL) String claimStatus,
            String activationStatus, String realNameStatus, String riskState, Instant createdAt) { }
    record SubjectPage(List<SubjectSummary> items, @JsonInclude(JsonInclude.Include.ALWAYS) String nextCursor, boolean hasMore) {
        public SubjectPage { items = List.copyOf(items); }
    }
    record OperationsSubjectDetail(UUID subjectId, String source,
            @JsonInclude(JsonInclude.Include.NON_NULL) String maskedNumber,
            String realNameStatus, String riskState,
            @JsonInclude(JsonInclude.Include.NON_NULL) ActivationState activation,
            @JsonInclude(JsonInclude.Include.NON_NULL) TrialSummary trial,
            List<BindingSummary> bindings, List<ClaimSummary> claims,
            long evidenceCount, long orderCount, Instant createdAt) {
        public OperationsSubjectDetail { bindings = List.copyOf(bindings); claims = List.copyOf(claims); }
    }
    record BindingSummary(UUID bindingId, String numberMasked, String numberType, String bindingStatus,
            @JsonInclude(JsonInclude.Include.NON_NULL) Instant verifiedAt) { }
    record ClaimSummary(UUID claimId, String status, String verificationMethod, Instant riskCheckedAt,
            @JsonInclude(JsonInclude.Include.NON_NULL) Instant verifiedAt) { }
    record ActivationState(String status, int revision) { }
    record TrialSummary(String status, long grantedSeconds, long remainingSeconds, Instant expiresAt) { }

    enum Code { CURSOR_INVALID, CURSOR_FILTER_MISMATCH, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND,
        VALIDATION_FAILED, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL }
    final class Failure extends RuntimeException {
        private final Code code;
        public Failure(Code code) { super("Operations read failed", null, false, false); this.code = code; }
        public Code code() { return code; }
        @Override public String toString() { return "Failure[REDACTED]"; }
    }
}
