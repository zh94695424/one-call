package com.onecall.core.entitlement_metering.internal.activation;

import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService.CurrentPolicy;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService.PolicyResult;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class ActivationTrialPolicySnapshot {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private ActivationTrialPolicySnapshot() { }
    public static PolicyResult parse(UUID id, int versionNo, String snapshot, String snapshotSha256) {
        try {
            JsonNode root = MAPPER.readTree(snapshot);
            if (id == null || versionNo < 1 || snapshotSha256 == null || !snapshotSha256.matches("[0-9a-f]{64}")
                    || root == null || !root.isObject()) return new PolicyResult(null);
            String version = Integer.toString(versionNo);
            if (root.has("policyVersion") && (!root.get("policyVersion").isString()
                    || !version.equals(root.get("policyVersion").asString()))) return new PolicyResult(null);
            String code = text(root, "policyCode");
            String prompt = text(root, "promptVersion");
            String content = text(root, "contentSha256");
            if (!content.matches("[0-9a-f]{64}")) return new PolicyResult(null);
            return new PolicyResult(new CurrentPolicy(id, versionNo, code, version, prompt, content, snapshotSha256));
        } catch (RuntimeException invalid) { return new PolicyResult(null); }
    }
    private static String text(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || !node.isString()) throw new IllegalArgumentException();
        String value = node.asString();
        if (value.isBlank() || value.codePointCount(0, value.length()) > 64 || value.indexOf('\0') >= 0
                || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) throw new IllegalArgumentException();
        return value;
    }
}
