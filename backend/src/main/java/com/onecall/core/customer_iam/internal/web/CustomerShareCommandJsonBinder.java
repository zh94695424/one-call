package com.onecall.core.customer_iam.internal.web;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public final class CustomerShareCommandJsonBinder {
    private static final Set<String> CREATE_KEYS = Set.of("creationRequestId", "evidenceId", "verificationMode",
            "expiresAt", "downloadAllowed", "maxAdmissions", "designatedMobile");
    private final JsonMapper mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    public CustomerShareCommandDtos.CreateRequest create(byte[] body) {
        try {
            JsonNode parsed = mapper.readTree(body);
            if (parsed == null || !parsed.isObject()) throw invalid();
            ObjectNode root = (ObjectNode) parsed;
            if (root.properties().stream().anyMatch(entry -> !CREATE_KEYS.contains(entry.getKey()))) throw invalid();
            String key = text(root, "creationRequestId", 128);
            String id = text(root, "evidenceId", 36);
            if (!id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) throw invalid();
            var mode = CustomerShareCommandDtos.VerificationMode.valueOf(text(root, "verificationMode", 32));
            String expires = text(root, "expiresAt", 40);
            if (!expires.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}[Tt][0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,6})?([Zz]|[+-][0-9]{2}:[0-9]{2})")) throw invalid();
            var instant = OffsetDateTime.parse(expires).toInstant();
            JsonNode download = root.get("downloadAllowed");
            if (download == null || !download.isBoolean()) throw invalid();
            Integer maximum = null;
            JsonNode max = root.get("maxAdmissions");
            if (max != null && !max.isNull()) {
                if (!max.isIntegralNumber() || !max.canConvertToInt() || max.asInt() < 1) throw invalid();
                maximum = max.asInt();
            }
            String mobile = root.has("designatedMobile") ? text(root, "designatedMobile", 32) : null;
            if ((mode == CustomerShareCommandDtos.VerificationMode.DESIGNATED_MOBILE) != (mobile != null)) throw invalid();
            return new CustomerShareCommandDtos.CreateRequest(key, UUID.fromString(id), mode, instant, download.asBoolean(), maximum, mobile);
        } catch (RuntimeException failure) { throw invalid(); }
    }

    private static String text(JsonNode root, String key, int maximum) {
        JsonNode value = root.get(key);
        if (value == null || !value.isString()) throw invalid();
        String text = value.asString();
        if (text.isBlank() || text.codePointCount(0, text.length()) > maximum || text.indexOf('\0') >= 0
                || !StandardCharsets.UTF_8.newEncoder().canEncode(text)) throw invalid();
        return text;
    }

    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid command body"); }
}
