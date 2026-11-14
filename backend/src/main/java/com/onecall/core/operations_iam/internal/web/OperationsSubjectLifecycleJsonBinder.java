package com.onecall.core.operations_iam.internal.web;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class OperationsSubjectLifecycleJsonBinder {
    private static final Set<String> KEYS = Set.of("reasonCode", "expectedActivationRevision");
    private final JsonMapper mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    public record TransitionRequest(String reasonCode, BigInteger expectedActivationRevision) {
        @Override public String toString() { return "TransitionRequest[REDACTED]"; }
    }
    public TransitionRequest bind(byte[] body) {
        try {
            if (body == null || body.length > 8192) throw new IllegalArgumentException();
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(body));
            JsonNode root = mapper.readTree(body);
            if (root == null || !root.isObject() || root.size() != 2 || root.properties().stream().anyMatch(entry -> !KEYS.contains(entry.getKey()))) throw new IllegalArgumentException();
            JsonNode reason = root.get("reasonCode"), revision = root.get("expectedActivationRevision");
            if (reason == null || !reason.isString() || !validText(reason.asString(), 64)
                    || revision == null || !revision.isIntegralNumber() || revision.bigIntegerValue().signum() < 1) throw new IllegalArgumentException();
            return new TransitionRequest(reason.asString(), revision.bigIntegerValue());
        } catch (Exception invalid) { throw new IllegalArgumentException("Invalid lifecycle input"); }
    }
    public static boolean validText(String text, int maximum) {
        return text != null && !text.isBlank() && text.codePointCount(0, text.length()) <= maximum && text.indexOf('\0') < 0
                && StandardCharsets.UTF_8.newEncoder().canEncode(text);
    }
}
