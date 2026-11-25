package com.onecall.core.customer_iam.internal.web;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class CustomerOrderJsonBinder {
    private static final Set<String> KEYS = Set.of("subjectId", "catalogOfferId", "channel", "decision", "policyCode", "policyVersion", "promptVersion", "contentSha256", "expectedVersionToken");
    private final JsonMapper mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    public CustomerOrderDtos.OrderRequest bind(byte[] body) {
        try {
            if (body.length > 8192) throw invalid();
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(body));
            JsonNode root = mapper.readTree(body);
            if (root == null || !root.isObject() || root.size() < 8 || root.size() > 9
                    || root.properties().stream().anyMatch(entry -> !KEYS.contains(entry.getKey()))) throw invalid();
            String channel = text(root, "channel", 7);
            String action = text(root, "decision", 6);
            String digest = text(root, "contentSha256", 64);
            if (!("WEB".equals(channel) || "MINIAPP".equals(channel)) || !"ACCEPT".equals(action) || !digest.matches("[0-9a-f]{64}")) throw invalid();
            return new CustomerOrderDtos.OrderRequest(uuid(root, "subjectId"), uuid(root, "catalogOfferId"), channel, action,
                    text(root, "policyCode", 64), text(root, "policyVersion", 64), text(root, "promptVersion", 64), digest,
                    root.has("expectedVersionToken") ? text(root, "expectedVersionToken", 128) : null);
        } catch (Exception failure) { throw invalid(); }
    }
    private static UUID uuid(JsonNode root, String key) {
        String text = text(root, key, 36);
        if (!text.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) throw invalid();
        return UUID.fromString(text);
    }
    private static String text(JsonNode root, String key, int limit) {
        JsonNode node = root.get(key);
        if (node == null || !node.isString()) throw invalid();
        String value = node.asString();
        if (value.isBlank() || value.codePointCount(0, value.length()) > limit || value.indexOf('\0') >= 0
                || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) throw invalid();
        return value;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid order body"); }
}
