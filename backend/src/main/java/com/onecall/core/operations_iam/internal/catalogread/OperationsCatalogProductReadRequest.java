package com.onecall.core.operations_iam.internal.catalogread;

import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService.RawListRequest;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService.RawDetailRequest;
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

public record OperationsCatalogProductReadRequest(int limit, String cursor) {
    static final String PURPOSE = "onecall:operations-catalog-product-list:cursor:v1";
    static final String ENDPOINT = "GET /api/v1/operations/catalog/products";
    static final String SORT = "productCode asc, productId asc";

    public static OperationsCatalogProductReadRequest parse(RawListRequest raw) {
        if (raw == null) throw invalid();
        shape(raw.query(), Set.of("cursor", "limit"));
        String text = value(raw.query(), "limit");
        int limit = 20;
        if (text != null) {
            if (!text.matches("[0-9]{1,3}")) throw invalid();
            limit = Integer.parseInt(text);
            if (limit < 1 || limit > 100) throw invalid();
        }
        accept(raw.accept());
        String cursor = value(raw.query(), "cursor");
        if (cursor != null && cursor.isBlank()) throw new Failure(Code.CURSOR_INVALID);
        return new OperationsCatalogProductReadRequest(limit, cursor);
    }
    public static UUID detail(RawDetailRequest raw) {
        if (raw == null) throw invalid();
        shape(raw.query(), Set.of());
        accept(raw.accept());
        if (raw.productId() == null || !raw.productId().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) throw invalid();
        return UUID.fromString(raw.productId());
    }
    public byte[] digest() {
        var bytes = new ByteArrayOutputStream();
        field(bytes, "purpose", PURPOSE.getBytes(StandardCharsets.UTF_8));
        field(bytes, "endpoint", ENDPOINT.getBytes(StandardCharsets.UTF_8));
        field(bytes, "sort", SORT.getBytes(StandardCharsets.UTF_8));
        field(bytes, "limit", ByteBuffer.allocate(4).putInt(limit).array());
        try { return MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()); }
        catch (NoSuchAlgorithmException impossible) { throw new Failure(Code.INTERNAL); }
    }
    private static void field(ByteArrayOutputStream bytes, String name, byte[] value) {
        bytes.writeBytes(lp(name)); bytes.write(1);
        bytes.writeBytes(ByteBuffer.allocate(4).putInt(value.length).array()); bytes.writeBytes(value);
    }
    static byte[] lp(String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        return ByteBuffer.allocate(4 + encoded.length).putInt(encoded.length).put(encoded).array();
    }
    private static void shape(Map<String, List<String>> query, Set<String> keys) {
        if (query == null) throw invalid();
        for (var entry : query.entrySet()) {
            if (entry.getKey() == null || !keys.contains(entry.getKey()) || entry.getValue() == null
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
    @Override public String toString() { return "OperationsCatalogProductReadRequest[REDACTED]"; }
}
