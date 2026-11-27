package com.onecall.core.operations_iam.internal.orderread;

import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.Filters;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.MediaType;

public record OperationsOrderReadRequest(int limit, String cursor, Filters filters) {
    static final String PURPOSE = "onecall:operations-order-list:cursor:v1";
    static final String ENDPOINT = "GET /api/v1/operations/orders";
    static final String SORT = "createdAt desc, orderId desc";

    public static OperationsOrderReadRequest parse(RawListRequest raw) {
        if (raw == null) throw invalid();
        shape(raw.query(), Set.of("cursor", "limit", "subjectId", "paymentStatus"));
        int limit = 20;
        String limitText = value(raw.query(), "limit");
        if (limitText != null) {
            if (!limitText.matches("[0-9]{1,3}")) throw invalid();
            limit = Integer.parseInt(limitText);
            if (limit < 1 || limit > 100) throw invalid();
        }
        String subject = value(raw.query(), "subjectId");
        String payment = value(raw.query(), "paymentStatus");
        UUID subjectId = subject == null ? null : uuid(subject);
        if (payment != null && !OperationsOrderReadPorts.PAYMENT_STATUSES.contains(payment)) throw invalid();
        accept(raw.accept());
        String cursor = value(raw.query(), "cursor");
        if (cursor != null) OperationsOrderCursorCodec.frame(cursor);
        return new OperationsOrderReadRequest(limit, cursor, new Filters(subjectId, payment));
    }
    public static UUID detail(RawDetailRequest raw) {
        if (raw == null) throw invalid();
        shape(raw.query(), Set.of());
        UUID identity = uuid(raw.orderId());
        accept(raw.accept());
        return identity;
    }
    private static UUID uuid(String value) {
        if (value == null || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw invalid();
        return UUID.fromString(value);
    }
    private static void shape(Map<String, List<String>> query, Set<String> allowed) {
        if (query == null) throw invalid();
        query.forEach((key, values) -> {
            if (key == null || !allowed.contains(key) || values == null || values.size() != 1 || values.getFirst() == null)
                throw invalid();
        });
    }
    private static String value(Map<String, List<String>> query, String key) {
        return query.containsKey(key) ? query.get(key).getFirst() : null;
    }
    public byte[] digest() {
        var bytes = new ByteArrayOutputStream();
        bytes.writeBytes(lp("subjectId"));
        bytes.write(filters.subjectId() == null ? 0 : 1);
        if (filters.subjectId() != null) {
            UUID subject = filters.subjectId();
            bytes.writeBytes(ByteBuffer.allocate(16).putLong(subject.getMostSignificantBits()).putLong(subject.getLeastSignificantBits()).array());
        }
        bytes.writeBytes(lp("paymentStatus"));
        bytes.write(filters.paymentStatus() == null ? 0 : 1);
        if (filters.paymentStatus() != null) bytes.writeBytes(lp(filters.paymentStatus()));
        try { return MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()); }
        catch (NoSuchAlgorithmException impossible) { throw new Failure(Code.INTERNAL); }
    }
    static byte[] lp(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        return ByteBuffer.allocate(4 + bytes.length).putInt(bytes.length).put(bytes).array();
    }
    private static void accept(List<String> headers) {
        if (headers == null) throw invalid();
        if (headers.isEmpty()) return;
        try {
            if (headers.stream().anyMatch(header -> header == null || header.isBlank())) throw invalid();
            var accepted = MediaType.parseMediaTypes(headers).stream()
                    .filter(media -> media.isCompatibleWith(MediaType.APPLICATION_JSON)
                            && media.getParameters().keySet().stream().allMatch(key -> "q".equalsIgnoreCase(key)
                            || "charset".equalsIgnoreCase(key) && "UTF-8".equalsIgnoreCase(media.getParameter(key))))
                    .sorted((left, right) -> {
                        int leftRank = (left.isWildcardType() ? 0 : 1) + (left.isWildcardSubtype() ? 0 : 1);
                        int rightRank = (right.isWildcardType() ? 0 : 1) + (right.isWildcardSubtype() ? 0 : 1);
                        int rank = Integer.compare(rightRank, leftRank);
                        return rank != 0 ? rank : Double.compare(right.getQualityValue(), left.getQualityValue());
                    }).findFirst();
            if (accepted.isEmpty() || accepted.get().getQualityValue() == 0) throw invalid();
        } catch (IllegalArgumentException malformed) { throw invalid(); }
    }
    private static Failure invalid() { return new Failure(Code.VALIDATION_FAILED); }
    @Override public String toString() { return "OperationsOrderReadRequest[REDACTED]"; }
}
