package com.onecall.core.operations_iam.internal.ivrread;

import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.Filters;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;

public record OperationsIvrSessionReadRequest(int limit, String cursor, Filters filters) {
    private static final Set<String> KEYS = Set.of("cursor", "limit", "subjectId", "callId", "taskId", "status");

    public static OperationsIvrSessionReadRequest parse(RawListRequest raw) {
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
        String subject = value(raw.query(), "subjectId");
        String call = bounded(value(raw.query(), "callId"), 128);
        String task = bounded(value(raw.query(), "taskId"), 64);
        String status = value(raw.query(), "status");
        if (status != null && !Set.of("RECEIVED", "CONFIRMED", "ABANDONED", "REJECTED").contains(status)) throw invalid();
        return new OperationsIvrSessionReadRequest(limit, value(raw.query(), "cursor"),
                new Filters(subject == null ? null : uuid(subject), call, task, status));
    }

    public static UUID parseDetail(RawDetailRequest raw) {
        if (raw == null || raw.query() == null || !raw.query().isEmpty()) throw invalid();
        accept(raw.accept());
        return uuid(raw.sessionId());
    }

    private static UUID uuid(String value) {
        try {
            UUID parsed = UUID.fromString(value);
            if (!parsed.toString().equals(value)) throw invalid();
            return parsed;
        } catch (IllegalArgumentException | NullPointerException failure) { throw invalid(); }
    }

    private static String bounded(String value, int maximum) {
        if (value != null && (value.isBlank() || value.codePointCount(0, value.length()) > maximum)) throw invalid();
        return value;
    }

    public byte[] digest() {
        var bytes = new ByteArrayOutputStream();
        bytes.writeBytes("onecall:operations-ivr-session-filters:v1\n".getBytes(StandardCharsets.UTF_8));
        String[] names = {"subjectId", "callId", "taskId", "status"};
        String[] values = {filters.subjectId() == null ? null : filters.subjectId().toString(), filters.callId(), filters.taskId(), filters.status()};
        for (int index = 0; index < names.length; index++) {
            bytes.writeBytes(names[index].getBytes(StandardCharsets.US_ASCII));
            String value = values[index];
            byte[] encoded = value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
            bytes.write(value == null ? 0 : 1);
            bytes.writeBytes(ByteBuffer.allocate(4).putInt(encoded.length).array());
            bytes.writeBytes(encoded);
        }
        try { return MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()); }
        catch (NoSuchAlgorithmException failure) { throw new Failure(Code.INTERNAL); }
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
    @Override public String toString() { return "OperationsIvrSessionReadRequest[REDACTED]"; }
}
