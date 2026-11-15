package com.onecall.core.operations_iam.internal.activationread;

import com.onecall.core.operations_iam.subjectread.OperationsActivationReadService.RawListRequest;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;

public record OperationsActivationReadRequest(int limit, String cursor) {
    private static final Set<String> KEYS = Set.of("cursor", "limit");

    public static OperationsActivationReadRequest parse(RawListRequest raw) {
        if (raw == null) throw invalid();
        shape(raw.query());
        String text = value(raw.query(), "limit");
        int limit = 20;
        if (text != null) {
            if (!text.matches("[0-9]{1,3}")) throw invalid();
            limit = Integer.parseInt(text);
            if (limit < 1 || limit > 100) throw invalid();
        }
        accept(raw.accept());
        return new OperationsActivationReadRequest(limit, value(raw.query(), "cursor"));
    }

    private static void shape(Map<String, List<String>> query) {
        if (query == null) throw invalid();
        for (var entry : query.entrySet()) {
            if (entry.getKey() == null || !KEYS.contains(entry.getKey()) || entry.getValue() == null
                    || entry.getValue().size() != 1 || entry.getValue().getFirst() == null) throw invalid();
        }
    }

    private static String value(Map<String, List<String>> query, String key) {
        return query.containsKey(key) ? query.get(key).getFirst() : null;
    }

    private static void accept(List<String> headers) {
        if (headers == null) throw invalid();
        if (headers.isEmpty()) return;
        try {
            if (headers.stream().anyMatch(header -> header == null || header.isBlank())) throw invalid();
            var ranges = MediaType.parseMediaTypes(headers);
            var compatible = ranges.stream().filter(media -> media.isCompatibleWith(MediaType.APPLICATION_JSON)
                    && media.getParameters().keySet().stream().allMatch(key -> "q".equalsIgnoreCase(key)
                    || "charset".equalsIgnoreCase(key) && "UTF-8".equalsIgnoreCase(media.getParameter(key))))
                    .sorted((left, right) -> {
                        int leftRank = (left.isWildcardType() ? 0 : 1) + (left.isWildcardSubtype() ? 0 : 1);
                        int rightRank = (right.isWildcardType() ? 0 : 1) + (right.isWildcardSubtype() ? 0 : 1);
                        int comparison = Integer.compare(rightRank, leftRank);
                        return comparison != 0 ? comparison : Double.compare(right.getQualityValue(), left.getQualityValue());
                    }).findFirst();
            if (compatible.isEmpty() || compatible.get().getQualityValue() == 0) throw invalid();
        } catch (IllegalArgumentException malformed) { throw invalid(); }
    }

    private static Failure invalid() { return new Failure(Code.VALIDATION_FAILED); }
    @Override public String toString() { return "OperationsActivationReadRequest[REDACTED]"; }
}
