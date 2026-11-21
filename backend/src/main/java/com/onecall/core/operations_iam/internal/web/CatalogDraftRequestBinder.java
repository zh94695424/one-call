package com.onecall.core.operations_iam.internal.web;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;

import tools.jackson.core.StreamReadFeature;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class CatalogDraftRequestBinder {
    private static final Set<String> CREATE = Set.of("productId", "versionNo", "offerType", "audience", "priceCents", "currency", "billingCycle", "installments", "grantIntervalMonths", "benefitLines", "channels");
    private static final Set<String> REPLACE = Set.of("expectedRevision", "offerType", "audience", "priceCents", "currency", "billingCycle", "installments", "grantIntervalMonths");
    private static final Set<String> BENEFIT = Set.of("entitlementType", "quantity", "validityDays", "grantIntervalMonths");
    private final JsonMapper mapper = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    ParsedCreate create(byte[] body) {
        try {
            JsonNode root = object(parse(body), CREATE);
            return new ParsedCreate(canonicalUuid(text(root.get("productId"))), positiveInt(root.get("versionNo")),
                    commercial(root), benefits(root.get("benefitLines")), channels(root.get("channels")));
        } catch (Exception invalid) { throw new Failure(Code.VALIDATION_FAILED); }
    }
    ParsedReplace replace(String versionId, byte[] body) {
        try {
            UUID identity = canonicalUuid(versionId);
            JsonNode root = object(parse(body), REPLACE);
            return new ParsedReplace(identity, positiveInt(root.get("expectedRevision")), commercial(root));
        } catch (Exception invalid) { throw new Failure(Code.VALIDATION_FAILED); }
    }
    private JsonNode parse(byte[] body) throws Exception {
        if (body == null || body.length == 0 || body.length > 8192 || body[0] == (byte) 0xef) throw new IllegalArgumentException();
        StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(body));
        return mapper.readTree(body);
    }
    private static UUID canonicalUuid(String text) {
        UUID identity = UUID.fromString(text);
        if (!identity.toString().equals(text)) throw new IllegalArgumentException();
        return identity;
    }
    private static int positiveInt(JsonNode node) { int value = nonNegativeInt(node); if (value == 0) throw new IllegalArgumentException(); return value; }
    private static int nonNegativeInt(JsonNode node) {
        BigInteger value = integer(node);
        if (value.signum() < 0 || value.bitLength() > 31) throw new IllegalArgumentException();
        return value.intValueExact();
    }
    private static long positiveLong(JsonNode node) { long value = nonNegativeLong(node); if (value == 0) throw new IllegalArgumentException(); return value; }
    private static long nonNegativeLong(JsonNode node) {
        BigInteger value = integer(node);
        if (value.signum() < 0 || value.bitLength() > 63) throw new IllegalArgumentException();
        return value.longValueExact();
    }
    private static BigInteger integer(JsonNode node) { if (node == null || !node.isIntegralNumber()) throw new IllegalArgumentException(); return node.bigIntegerValue(); }
    private static String text(JsonNode node) { if (node == null || !node.isString()) throw new IllegalArgumentException(); return node.asString(); }
    private static String enumeration(JsonNode node, Set<String> allowed) {
        String value = text(node); if (!allowed.contains(value)) throw new IllegalArgumentException(); return value;
    }
    private static Commercial commercial(JsonNode root) {
        return new Commercial(enumeration(root.get("offerType"), Set.of("SUBSCRIPTION", "ONE_TIME", "ADD_ON", "QUOTE")),
                enumeration(root.get("audience"), Set.of("PERSONAL", "ENTERPRISE")),
                root.get("priceCents").isNull() ? null : nonNegativeLong(root.get("priceCents")),
                enumeration(root.get("currency"), Set.of("CNY")),
                enumeration(root.get("billingCycle"), Set.of("MONTHLY", "QUARTERLY", "ANNUAL", "ONCE", "QUOTE")),
                positiveInt(root.get("installments")), nonNegativeInt(root.get("grantIntervalMonths")));
    }
    private static List<BenefitLine> benefits(JsonNode node) {
        if (node == null || !node.isArray()) throw new IllegalArgumentException();
        List<BenefitLine> result = new ArrayList<>(); Set<String> identities = new HashSet<>();
        for (JsonNode entry : node) {
            object(entry, BENEFIT);
            String type = enumeration(entry.get("entitlementType"), Set.of("AI_SECONDS", "EVIDENCE_COUNT", "RECORDING_SECONDS", "STORAGE_BYTES"));
            if (!identities.add(type)) throw new IllegalArgumentException();
            result.add(new BenefitLine(type, positiveLong(entry.get("quantity")), positiveInt(entry.get("validityDays")), nonNegativeInt(entry.get("grantIntervalMonths"))));
        }
        return List.copyOf(result);
    }
    private static List<String> channels(JsonNode node) {
        if (node == null || !node.isArray()) throw new IllegalArgumentException();
        List<String> result = new ArrayList<>(); Set<String> identities = new HashSet<>();
        for (JsonNode entry : node) {
            String channel = enumeration(entry, Set.of("MINIAPP", "WEB"));
            if (!identities.add(channel)) throw new IllegalArgumentException();
            result.add(channel);
        }
        return List.copyOf(result);
    }
    private static JsonNode object(JsonNode node, Set<String> fields) {
        if (node == null || !node.isObject() || node.size() != fields.size()) throw new IllegalArgumentException();
        for (String field : fields) if (!node.has(field)) throw new IllegalArgumentException();
        return node;
    }
    record ParsedCreate(UUID productId, int versionNo, Commercial commercial, List<BenefitLine> benefitLines, List<String> channels) {
        ParsedCreate { benefitLines = List.copyOf(benefitLines); channels = List.copyOf(channels); }
    }
    record ParsedReplace(UUID versionId, int expectedRevision, Commercial commercial) { }
}
