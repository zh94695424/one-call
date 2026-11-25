package com.onecall.core.activation_consent.internal.purchase;

import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class PurchaseConsentSemantics {
    static final JsonMapper JSON = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private PurchaseConsentSemantics() {}
    static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> fields = new TreeMap<>();
        for (int index = 0; index < pairs.length; index += 2) fields.put((String) pairs[index], pairs[index + 1]);
        return fields;
    }
    static String canonical(Object value) { return encode(value instanceof JsonNode node ? node : JSON.valueToTree(value)); }
    private static String encode(JsonNode node) {
        if (node.isNull()) return "null";
        if (node.isString()) {
            String value = node.asString();
            if (value.indexOf('\0') >= 0 || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) throw new Failure(Kind.INTEGRITY);
            return JSON.writeValueAsString(value);
        }
        if (node.isBoolean()) return node.asBoolean() ? "true" : "false";
        if (node.isIntegralNumber() && node.canConvertToLong() && node.longValue() >= -9007199254740991L && node.longValue() <= 9007199254740991L) return Long.toString(node.longValue());
        if (node.isArray()) {
            List<String> elements = new ArrayList<>(); node.forEach(child -> elements.add(encode(child)));
            return "[" + String.join(",", elements) + "]";
        }
        if (node.isObject()) {
            Map<String, JsonNode> sorted = new TreeMap<>(); node.properties().forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
            List<String> elements = new ArrayList<>(); sorted.forEach((key, child) -> elements.add(JSON.writeValueAsString(key) + ":" + encode(child)));
            return "{" + String.join(",", elements) + "}";
        }
        throw new Failure(Kind.INTEGRITY);
    }
    static String sha(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException failure) { throw new Failure(Kind.INTEGRITY); }
    }
    static String fingerprint(PurchaseConsentInput input) {
        return sha(canonical(fields("accountId", input.accountId(), "action", input.action(), "channel", input.channel(), "eventDomain", "PURCHASE", "eventId", input.eventId(),
                "policyCode", input.policyCode(), "policyVersion", input.policyVersion(), "promptVersion", input.promptVersion(), "purpose", "PURCHASE", "sourceType", "ACCOUNT", "subjectId", input.subjectId())));
    }
    static String snapshot(PurchaseConsentInput input) {
        return canonical(fields("schemaVersion", 1, "accountId", input.accountId(), "subjectId", input.subjectId(), "purpose", "PURCHASE", "channel", input.channel(),
                "eventDomain", "PURCHASE", "eventId", input.eventId(), "sourceType", "ACCOUNT", "sourceRef", input.eventId(), "action", input.action(), "policyCode", input.policyCode(),
                "policyVersion", input.policyVersion(), "promptVersion", input.promptVersion(), "contentSha256", input.contentSha256()));
    }
    static String payload(ConsentDescriptor consent) {
        return canonical(fields("consentId", consent.id(), "subjectId", consent.subjectId(), "accountId", consent.accountId(), "channel", consent.channel(), "purpose", consent.purpose(),
                "eventId", consent.eventId(), "policyCode", consent.policyCode(), "policyVersion", consent.policyVersion(), "promptVersion", consent.promptVersion(), "action", consent.action(),
                "snapshotSha256", consent.snapshotSha256(), "acceptedAt", consent.acceptedAt().toString()));
    }
    static boolean validText(String value) { return value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= 64 && value.indexOf('\0') < 0 && StandardCharsets.UTF_8.newEncoder().canEncode(value); }
}
