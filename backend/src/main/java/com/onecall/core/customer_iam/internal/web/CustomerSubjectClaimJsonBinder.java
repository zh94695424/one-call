package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.VerificationMethod;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class CustomerSubjectClaimJsonBinder {
    private static final Set<String> KEYS = Set.of("subjectId", "bindingId", "verificationMethod", "verificationProofId");
    private final JsonMapper mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    public CustomerSubjectClaimDtos.ClaimRequest bind(byte[] body) {
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(body));
            JsonNode root = mapper.readTree(body);
            if (root == null || !root.isObject() || root.size() != 4 || root.properties().stream().anyMatch(entry -> !KEYS.contains(entry.getKey()))) throw invalid();
            return new CustomerSubjectClaimDtos.ClaimRequest(uuid(text(root, "subjectId")), uuid(text(root, "bindingId")),
                    VerificationMethod.valueOf(text(root, "verificationMethod")), text(root, "verificationProofId"));
        } catch (Exception failure) { throw invalid(); }
    }
    private static String text(JsonNode root, String key) {
        JsonNode value = root.get(key);
        if (value == null || !value.isString()) throw invalid();
        String text = value.asString();
        if (text.isBlank() || text.codePointCount(0, text.length()) > 128 || text.indexOf('\0') >= 0
                || !StandardCharsets.UTF_8.newEncoder().canEncode(text)) throw invalid();
        return text;
    }
    private static UUID uuid(String value) {
        if (!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) throw invalid();
        return UUID.fromString(value);
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid claim body"); }
}
