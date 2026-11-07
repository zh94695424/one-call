package com.onecall.core.sharing.internal.publicadmission;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class ShareAdmissionJsonBinder {
    private final JsonMapper mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    public ShareAdmissionRequest bind(byte[] body) {
        try {
            JsonNode root = mapper.readTree(body);
            closed(root, Set.of("admissionRequestId", "verification"));
            return new ShareAdmissionRequest(text(root, "admissionRequestId", 128), bindVerification(root.get("verification")));
        } catch (RuntimeException failure) { throw invalid(); }
    }

    public MediaAuthorizationRequest bindMediaAuthorization(byte[] body) {
        try {
            try { StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(body)); }
            catch (java.nio.charset.CharacterCodingException invalidUtf8) { throw invalid(); }
            JsonNode root = mapper.readTree(body);
            closed(root, Set.of("authorizationRequestId", "purpose"));
            String purpose = text(root, "purpose", 16);
            return new MediaAuthorizationRequest(text(root, "authorizationRequestId", 128),
                    MediaAuthorizationRequest.Purpose.valueOf(purpose));
        } catch (RuntimeException failure) { throw invalid(); }
    }

    public ShareAdmissionRequest.Verification bindVerification(JsonNode verification) {
        try {
            String mode = text(verification, "mode", 32);
            return switch (mode) {
                case "NONE" -> {
                    closed(verification, Set.of("mode"));
                    yield new ShareAdmissionRequest.Verification.None();
                }
                case "EXTRACTION_CODE" -> {
                    closed(verification, Set.of("mode", "code"));
                    yield new ShareAdmissionRequest.Verification.ExtractionCode(text(verification, "code", 128));
                }
                case "DESIGNATED_MOBILE" -> {
                    closed(verification, Set.of("mode", "verificationProofId"));
                    yield new ShareAdmissionRequest.Verification.DesignatedMobile(text(verification, "verificationProofId", 128));
                }
                case "ORGANIZATION_MEMBER" -> {
                    closed(verification, Set.of("mode"));
                    yield new ShareAdmissionRequest.Verification.OrganizationMember();
                }
                default -> throw invalid();
            };
        } catch (RuntimeException failure) { throw invalid(); }
    }

    private static void closed(JsonNode root, Set<String> keys) {
        if (root == null || !root.isObject() || root.size() != keys.size()
                || root.properties().stream().anyMatch(entry -> !keys.contains(entry.getKey()))) throw invalid();
    }

    private static String text(JsonNode root, String key, int maximum) {
        if (root == null || !root.isObject() || !root.path(key).isString()) throw invalid();
        String value = root.path(key).asString();
        if (value.isBlank() || value.codePointCount(0, value.length()) > maximum || value.indexOf('\0') >= 0
                || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) throw invalid();
        return value;
    }

    private static ShareAdmissionException invalid() {
        return new ShareAdmissionException(ShareAdmissionException.Code.VALIDATION_FAILED);
    }
}
