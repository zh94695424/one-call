package com.onecall.core.activation_consent.internal.command;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.ActivationCaller;
import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.ActivationCommand;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

public final class ActivationSemantics {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Set<String> ROOT = Set.of("schemaVersion", "policyCode", "policyVersion", "promptVersion", "contentSha256", "activationResult");
    private static final Set<String> RESULT = Set.of("activationStatus", "activationRevision", "transitionId", "trialOutcome", "trialPolicyVersionId");
    private ActivationSemantics() { }
    public static String canonical(Object value) {
        if (value == null) return "null";
        if (value instanceof String text) {
            if (!StandardCharsets.UTF_8.newEncoder().canEncode(text)) throw integrity();
            StringBuilder encoded = new StringBuilder("\"");
            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                switch (character) {
                    case '"' -> encoded.append("\\\"");
                    case '\\' -> encoded.append("\\\\");
                    case '\b' -> encoded.append("\\b");
                    case '\f' -> encoded.append("\\f");
                    case '\n' -> encoded.append("\\n");
                    case '\r' -> encoded.append("\\r");
                    case '\t' -> encoded.append("\\t");
                    default -> { if (character < 32) encoded.append(String.format("\\u%04x", (int) character)); else encoded.append(character); }
                }
            }
            return encoded.append('"').toString();
        }
        if (value instanceof UUID identity) return canonical(identity.toString());
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            long number = ((Number) value).longValue();
            if (number < -9007199254740991L || number > 9007199254740991L) throw integrity();
            return Long.toString(number);
        }
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            map.forEach((key, entry) -> { if (!(key instanceof String)) throw integrity(); sorted.put((String) key, entry); });
            return "{" + sorted.entrySet().stream().map(entry -> canonical(entry.getKey()) + ":" + canonical(entry.getValue()))
                    .collect(java.util.stream.Collectors.joining(",")) + "}";
        }
        throw integrity();
    }
    public static String sha(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException unavailable) { throw integrity(); }
    }
    public static Map<String, Object> fields(Object... fields) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < fields.length; index += 2) result.put((String) fields[index], fields[index + 1]);
        return result;
    }
    public static String sourceRef(ActivationCommand command, ActivationCaller caller) {
        return "activation:" + sha(canonical(fields("accountId", caller.accountId(), "activationEventId", command.activationEventId())));
    }
    public static String consentFingerprint(ActivationCommand command, ActivationCaller caller) {
        return sha(canonical(fields("accountId", caller.accountId(), "action", command.decision().name(), "channel", caller.channel().name(),
                "eventDomain", "ACTIVATION", "policyCode", command.policyCode(), "policyVersion", command.policyVersion(), "promptVersion", command.promptVersion(),
                "purpose", "SERVICE_ACTIVATION", "sourceRef", sourceRef(command, caller), "sourceType", "ACCOUNT", "subjectId", command.subjectId())));
    }
    public static String trialFingerprint(ActivationCommand command, ActivationCaller caller, String consentFingerprint, UUID policyId) {
        return sha(canonical(fields("accountId", caller.accountId(), "activationEventId", command.activationEventId(), "channel", caller.channel().name(),
                "consentRequestFingerprint", consentFingerprint, "policyVersionId", policyId, "sourceActionDomain", "ACTIVATION", "subjectId", command.subjectId())));
    }
    public static String statusEvent(UUID activationId, int revision) { return "activation-status:" + activationId + ":" + revision; }
    public static String transitionFingerprint(ActivationCommand command, ActivationCaller caller, String consentFingerprint, String statusEvent) {
        return sha(canonical(fields("accountId", caller.accountId(), "actorEmployeeId", null, "channel", caller.channel().name(), "consentRequestFingerprint", consentFingerprint,
                "reasonCode", "CLIENT_ACTIVATION_ACCEPTED", "sourceActionDomain", "ACTIVATION", "sourceActionEventId", command.activationEventId(),
                "statusEventDomain", "ACTIVATION_STATUS", "statusEventId", statusEvent, "subjectId", command.subjectId(), "toStatus", "ACTIVE")));
    }
    public record Descriptor(Integer revision, UUID transitionId, String outcome, UUID policyId, String contentSha256) {
        @Override public String toString() { return "Descriptor[REDACTED]"; }
    }
    public static String descriptor(ActivationCommand command, Descriptor result) {
        return canonical(fields("schemaVersion", 1, "policyCode", command.policyCode(), "policyVersion", command.policyVersion(), "promptVersion", command.promptVersion(),
                "contentSha256", command.contentSha256(), "activationResult", result == null ? null : fields("activationStatus", "ACTIVE", "activationRevision", result.revision(),
                        "transitionId", result.transitionId(), "trialOutcome", result.outcome(), "trialPolicyVersionId", result.policyId())));
    }
    public static Descriptor decode(String snapshot, String hash, String code, String version, String prompt, boolean accepted) {
        try {
            var root = MAPPER.readTree(snapshot);
            if (root == null || !root.isObject() || !root.propertyNames().equals(ROOT) || !root.get("schemaVersion").isIntegralNumber() || !root.get("schemaVersion").canConvertToInt()
                    || root.get("schemaVersion").asInt() != 1) throw integrity();
            if (!text(root.get("policyCode"), 64).equals(code) || !text(root.get("policyVersion"), 64).equals(version)
                    || !text(root.get("promptVersion"), 64).equals(prompt)) throw integrity();
            String content = text(root.get("contentSha256"), 64);
            if (!content.matches("[0-9a-f]{64}")) throw integrity();
            Object result = null;
            Descriptor descriptor = new Descriptor(null, null, null, null, content);
            var nested = root.get("activationResult");
            if (accepted) {
                if (nested == null || !nested.isObject() || !nested.propertyNames().equals(RESULT)
                        || !"ACTIVE".equals(text(nested.get("activationStatus"), 64)) || !nested.get("activationRevision").isIntegralNumber()
                        || !nested.get("activationRevision").canConvertToInt() || nested.get("activationRevision").asInt() < 1) throw integrity();
                int revision = nested.get("activationRevision").asInt();
                UUID transition = uuid(text(nested.get("transitionId"), 36));
                UUID policy = uuid(text(nested.get("trialPolicyVersionId"), 36));
                String outcome = text(nested.get("trialOutcome"), 64);
                if (!Set.of("GRANTED", "REPLAY", "ALREADY_GRANTED").contains(outcome)) throw integrity();
                descriptor = new Descriptor(revision, transition, outcome, policy, content);
                result = fields("activationStatus", "ACTIVE", "activationRevision", revision, "transitionId", transition, "trialOutcome", outcome, "trialPolicyVersionId", policy);
            } else if (nested == null || !nested.isNull()) throw integrity();
            String canonical = canonical(fields("schemaVersion", 1, "policyCode", code, "policyVersion", version, "promptVersion", prompt,
                    "contentSha256", content, "activationResult", result));
            if (!sha(canonical).equals(hash)) throw integrity();
            return descriptor;
        } catch (RuntimeException invalid) { throw integrity(); }
    }
    private static String text(tools.jackson.databind.JsonNode node, int limit) {
        if (node == null || !node.isString()) throw integrity();
        String value = node.asString();
        if (value.isBlank() || value.codePointCount(0, value.length()) > limit || value.indexOf('\0') >= 0
                || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) throw integrity();
        return value;
    }
    public static UUID uuid(String value) {
        UUID identity = UUID.fromString(value);
        if (!identity.toString().equals(value)) throw integrity();
        return identity;
    }
    private static SubjectActivationException integrity() { return new SubjectActivationException(Code.INTERNAL); }
}
