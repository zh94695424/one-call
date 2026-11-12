package com.onecall.core.subject_number.internal.claim;

import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.VerificationMethod;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class SubjectClaimVerificationReference {
    private static final Set<String> KEYS = Set.of("bindingId", "numberHmac", "proofRef", "schemaVersion", "subjectId", "verificationMethod");
    private final JsonMapper mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    public record Payload(int schemaVersion, UUID subjectId, UUID bindingId, String numberHmac,
                          VerificationMethod verificationMethod, String proofRef) {
        public Payload {
            if (schemaVersion != 1 || subjectId == null || bindingId == null || verificationMethod == null
                    || numberHmac == null || !numberHmac.matches("[0-9a-f]{64}") || !opaque(proofRef)) throw invalid();
        }
        @Override public String toString() { return "Payload[REDACTED]"; }
    }

    public byte[] encode(Payload payload) {
        if (payload == null) throw invalid();
        String canonical = "{\"bindingId\":" + quoted(payload.bindingId().toString()) + ",\"numberHmac\":" + quoted(payload.numberHmac())
                + ",\"proofRef\":" + quoted(payload.proofRef()) + ",\"schemaVersion\":1,\"subjectId\":" + quoted(payload.subjectId().toString())
                + ",\"verificationMethod\":" + quoted(payload.verificationMethod().name()) + "}";
        return canonical.getBytes(StandardCharsets.UTF_8);
    }

    public Payload decode(byte[] canonicalJson) {
        try {
            if (canonicalJson == null) throw invalid();
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(canonicalJson));
            JsonNode root = mapper.readTree(canonicalJson);
            if (root == null || !root.isObject() || root.size() != 6
                    || root.properties().stream().anyMatch(entry -> !KEYS.contains(entry.getKey()))
                    || !root.path("schemaVersion").isIntegralNumber() || !root.path("schemaVersion").canConvertToInt()
                    || root.path("schemaVersion").asInt() != 1) throw invalid();
            Payload payload = new Payload(1, uuid(text(root, "subjectId")), uuid(text(root, "bindingId")),
                    text(root, "numberHmac"), VerificationMethod.valueOf(text(root, "verificationMethod")), text(root, "proofRef"));
            if (!Arrays.equals(canonicalJson, encode(payload))) throw invalid();
            return payload;
        } catch (Exception failure) { throw invalid(); }
    }

    public static boolean opaque(String value) {
        return value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= 128
                && value.indexOf('\0') < 0 && StandardCharsets.UTF_8.newEncoder().canEncode(value);
    }

    public static boolean envelope(String value) {
        return value != null && value.matches("v1\\.[A-Za-z0-9._:/-]+\\.[A-Za-z0-9_-]+");
    }

    private static String quoted(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\t' -> escaped.append("\\t");
                case '\n' -> escaped.append("\\n");
                case '\f' -> escaped.append("\\f");
                case '\r' -> escaped.append("\\r");
                default -> {
                    if (character < 0x20) escaped.append("\\u%04x".formatted((int) character));
                    else escaped.append(character);
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static String text(JsonNode root, String key) {
        if (!root.path(key).isString()) throw invalid();
        return root.path(key).asString();
    }
    private static UUID uuid(String value) {
        if (!value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) throw invalid();
        return UUID.fromString(value);
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid verification reference"); }
}
