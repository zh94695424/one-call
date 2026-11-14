package com.onecall.core.operations_iam.internal.subjectread;

import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.Filters;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;

public record OperationsSubjectReadRequest(Filters filters, int limit, String cursor) {
    private static final Set<String> KEYS = Set.of("cursor", "limit", "subjectId", "source", "claimStatus",
            "activationStatus", "realNameStatus", "riskState");

    public static OperationsSubjectReadRequest list(RawListRequest raw) {
        if (raw == null) throw invalid();
        shape(raw.query(), KEYS);
        var query = raw.query();
        UUID subject = query.containsKey("subjectId") ? uuid(value(query, "subjectId")) : null;
        String source = enumeration(value(query, "source"), Set.of("IVR", "MINIAPP", "WEB", "OPERATIONS"));
        String claim = enumeration(value(query, "claimStatus"), Set.of("ACCEPTED", "REJECTED", "CONFLICT"));
        String activation = enumeration(value(query, "activationStatus"), Set.of("INACTIVE", "ACTIVE", "SUSPENDED"));
        String realName = enumeration(value(query, "realNameStatus"), Set.of("UNVERIFIED", "VERIFIED"));
        String risk = enumeration(value(query, "riskState"), Set.of("OPEN", "CLEARED"));
        String text = value(query, "limit");
        int limit = 20;
        if (text != null) {
            if (!text.matches("[0-9]{1,3}")) throw invalid();
            limit = Integer.parseInt(text);
            if (limit < 1 || limit > 100) throw invalid();
        }
        accept(raw.accept());
        return new OperationsSubjectReadRequest(new Filters(subject, source, claim, activation, realName, risk), limit, value(query, "cursor"));
    }

    public static UUID detail(RawDetailRequest raw) {
        if (raw == null) throw invalid();
        shape(raw.query(), Set.of());
        UUID subject = uuid(raw.subjectId());
        accept(raw.accept());
        return subject;
    }

    private static void shape(Map<String, List<String>> query, Set<String> keys) {
        if (query == null) throw invalid();
        for (var entry : query.entrySet()) {
            if (entry.getKey() == null || !keys.contains(entry.getKey()) || entry.getValue() == null || entry.getValue().size() != 1
                    || entry.getValue().getFirst() == null) throw invalid();
        }
    }

    private static String value(Map<String, List<String>> query, String key) {
        return query.containsKey(key) ? query.get(key).getFirst() : null;
    }

    public static UUID uuid(String text) {
        try {
            UUID result = UUID.fromString(text);
            if (!result.toString().equals(text)) throw invalid();
            return result;
        } catch (IllegalArgumentException | NullPointerException invalid) { throw invalid(); }
    }

    private static String enumeration(String text, Set<String> allowed) {
        if (text != null && !allowed.contains(text)) throw invalid();
        return text;
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

    public byte[] digest() {
        String canonical = "onecall:operations-subject-filters:v1\n"
                + "subjectId=" + normalized(filters.subjectId()) + "\n"
                + "source=" + normalized(filters.source()) + "\n"
                + "claimStatus=" + normalized(filters.claimStatus()) + "\n"
                + "activationStatus=" + normalized(filters.activationStatus()) + "\n"
                + "realNameStatus=" + normalized(filters.realNameStatus()) + "\n"
                + "riskState=" + normalized(filters.riskState()) + "\n";
        try { return MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException impossible) { throw new Failure(Code.INTERNAL); }
    }

    private static String normalized(Object value) { return value == null ? "~" : value.toString(); }
    private static Failure invalid() { return new Failure(Code.VALIDATION_FAILED); }
    @Override public String toString() { return "OperationsSubjectReadRequest[REDACTED]"; }
}
