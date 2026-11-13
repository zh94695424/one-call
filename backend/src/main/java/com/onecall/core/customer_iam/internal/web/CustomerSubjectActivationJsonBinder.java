package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.Decision;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class CustomerSubjectActivationJsonBinder {
    private static final Set<String> KEYS = Set.of("activationEventId", "decision", "policyCode", "policyVersion", "promptVersion", "contentSha256");
    private final JsonMapper mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    public CustomerSubjectActivationDtos.ActivationRequest bind(byte[] body) {
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(body));
            JsonNode root = mapper.readTree(body);
            if (root == null || !root.isObject() || root.size() != 6 || root.properties().stream().anyMatch(entry -> !KEYS.contains(entry.getKey()))) throw invalid();
            String content = text(root, "contentSha256", 64);
            if (!content.matches("[0-9a-f]{64}")) throw invalid();
            return new CustomerSubjectActivationDtos.ActivationRequest(text(root, "activationEventId", 128), Decision.valueOf(text(root, "decision", 64)),
                    text(root, "policyCode", 64), text(root, "policyVersion", 64), text(root, "promptVersion", 64), content);
        } catch (Exception failure) { throw invalid(); }
    }
    private static String text(JsonNode root, String key, int limit) {
        JsonNode value = root.get(key);
        if (value == null || !value.isString()) throw invalid();
        String text = value.asString();
        if (text.isBlank() || text.codePointCount(0, text.length()) > limit || text.indexOf('\0') >= 0 || !StandardCharsets.UTF_8.newEncoder().canEncode(text)) throw invalid();
        return text;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid activation body"); }
}
