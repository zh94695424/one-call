package com.onecall.core.catalog_order.internal.customerorder;

import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException.Code;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class CustomerOrderSemantics {
    static final long MAX_INTEGER = 9007199254740991L;
    static final JsonMapper JSON = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CustomerOrderSemantics.class);
    private CustomerOrderSemantics() {}
    static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> result = new TreeMap<>();
        for (int index = 0; index < pairs.length; index += 2) result.put((String) pairs[index], pairs[index + 1]);
        return result;
    }
    static String canonical(Object value) { return encode(value instanceof JsonNode node ? node : JSON.valueToTree(value)); }
    private static String encode(JsonNode node) {
        if (node.isNull()) return "null";
        if (node.isString()) { scalar(node.asString()); return JSON.writeValueAsString(node.asString()); }
        if (node.isBoolean()) return node.asBoolean() ? "true" : "false";
        if (node.isIntegralNumber() && node.canConvertToLong() && Math.abs(node.longValue()) <= MAX_INTEGER
                && node.longValue() != Long.MIN_VALUE) return Long.toString(node.longValue());
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(child -> values.add(encode(child)));
            return "[" + String.join(",", values) + "]";
        }
        if (node.isObject()) {
            Map<String, JsonNode> sorted = new TreeMap<>();
            node.properties().forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
            List<String> values = new ArrayList<>();
            sorted.forEach((key, child) -> { scalar(key); values.add(JSON.writeValueAsString(key) + ":" + encode(child)); });
            return "{" + String.join(",", values) + "}";
        }
        throw invalid();
    }
    static void scalar(String value) {
        if (value.indexOf('\0') >= 0 || !StandardCharsets.UTF_8.newEncoder().canEncode(value)) throw invalid();
    }
    static String sha(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException("Digest unavailable"); }
    }
    static JsonNode parse(String value) {
        try { return JSON.readTree(value); }
        catch (RuntimeException failure) { throw new OrderCreationException(Code.INTERNAL); }
    }
    static void key(String key) {
        if (key == null || key.isBlank() || key.codePointCount(0, key.length()) > 475) throw invalid();
        scalar(key);
    }
    static String event(UUID accountId, String key) {
        key(key);
        return "purchase-consent:" + sha(canonical(fields("accountId", accountId.toString(), "clientIdempotencyKey", key)));
    }
    static String platformKey(UUID accountId, String key) { key(key); return accountId + ":" + key; }
    static void arguments(Command command, Caller caller) {
        if (caller == null || caller.accountId() == null || !Set.of("WEB", "MINIAPP").contains(Objects.toString(caller.channel(), ""))
                || !Set.of("PERSONAL", "ORGANIZATION").contains(Objects.toString(caller.workspaceType(), ""))
                || "PERSONAL".equals(caller.workspaceType()) != (caller.organizationId() == null)) throw new OrderCreationException(Code.SERVICE_UNAVAILABLE);
        if (command == null || command.subjectId() == null || command.catalogOfferId() == null || command.traceId() == null
                || !caller.channel().equals(command.channel()) || !"ACCEPT".equals(command.decision())
                || !text(command.policyCode(), 64) || !text(command.policyVersion(), 64) || !text(command.promptVersion(), 64)
                || command.contentSha256() == null || !command.contentSha256().matches("[0-9a-f]{64}")
                || command.expectedVersionToken() != null && !text(command.expectedVersionToken(), 128)) throw invalid();
        key(command.clientIdempotencyKey());
    }
    private static boolean text(String value, int bound) {
        return value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= bound && value.indexOf('\0') < 0 && StandardCharsets.UTF_8.newEncoder().canEncode(value);
    }
    static String fingerprint(Command command, Caller caller, CustomerOrderRepository.Catalog catalog) {
        return sha(canonical(fields("accountId", caller.accountId(), "amountCents", catalog.priceCents(),
                "catalogOfferId", catalog.offerId(), "catalogVersionId", catalog.versionId(), "channel", caller.channel(),
                "currency", catalog.currency(), "ownerOrganizationId", caller.organizationId(), "purchaseConsentEventId", event(caller.accountId(), command.clientIdempotencyKey()),
                "subjectId", command.subjectId(), "versionToken", catalog.versionToken())));
    }
    static void payable(CustomerOrderRepository.Catalog catalog, Command command, Caller caller) {
        if (catalog == null || !"ACTIVE".equals(catalog.versionStatus()) || !"ACTIVE".equals(catalog.offerStatus())
                || !"PERSONAL".equals(catalog.audience()) || !Set.of("SUBSCRIPTION", "ONE_TIME", "ADD_ON").contains(catalog.offerType())
                || !caller.channel().equals(catalog.channel()) || !caller.channel().equals(command.channel())
                || catalog.priceCents() == null || catalog.priceCents() < 0 || catalog.priceCents() > MAX_INTEGER || !"CNY".equals(catalog.currency())
                || catalog.installments() < 1 || catalog.versionToken() == null || catalog.versionToken().isBlank()
                || command.expectedVersionToken() != null && !command.expectedVersionToken().equals(catalog.versionToken())) throw invalid();
        if ("SUBSCRIPTION".equals(catalog.offerType())) {
            if (!Set.of("MONTHLY", "QUARTERLY", "ANNUAL").contains(catalog.billingCycle()) || catalog.grantIntervalMonths() < 1) throw invalid();
        } else if (!"ONCE".equals(catalog.billingCycle()) || catalog.installments() != 1 || catalog.grantIntervalMonths() != 0) throw invalid();
    }
    static String snapshot(CustomerOrderRepository.Catalog catalog, List<CustomerOrderRepository.BenefitRow> benefits) {
        JsonNode source;
        try { source = JSON.readTree(catalog.snapshot()); }
        catch (RuntimeException unsupported) { throw invalid(); }
        if (source == null || !source.isObject()) throw invalid();
        if (!source.has("terms") || !source.has("benefitLines")) throw invalid();
        terms(source.get("terms"));
        if (!sha(canonical(source)).equals(catalog.snapshotSha256())) throw integrity();
        Map<String, Object> expected = fields("schemaVersion", 1, "catalogVersionId", catalog.versionId().toString(),
                "productCode", catalog.productCode(), "versionNo", catalog.versionNo(), "offerType", catalog.offerType(), "audience", catalog.audience(),
                "priceCents", catalog.priceCents(), "currency", catalog.currency(), "billingCycle", catalog.billingCycle(), "installments", catalog.installments());
        expected.forEach((key, value) -> {
            if (!source.has(key) || !canonical(source.get(key)).equals(canonical(value))) throw integrity();
        });
        JsonNode lines = source.get("benefitLines");
        if (!lines.isArray() || lines.size() != benefits.size() || benefits.isEmpty()) throw invalid();
        List<JsonNode> sorted = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (JsonNode line : lines) {
            exact(line, Set.of("lineKey", "entitlementType", "quantity", "validityDays", "grantIntervalMonths", "schedule"));
            if (!line.get("lineKey").isTextual() || !line.get("entitlementType").isTextual()) throw invalid();
            UUID lineId;
            try { lineId = UUID.fromString(line.get("lineKey").asString()); } catch (RuntimeException failure) { throw invalid(); }
            var benefit = benefits.stream().filter(candidate -> candidate.id().equals(lineId)).findFirst().orElseThrow(CustomerOrderSemantics::integrity);
            if (!seen.add(lineId)) throw invalid();
            if (!Objects.equals(line.get("entitlementType").asString(), benefit.entitlementType())
                    || integer(line.get("quantity")) != benefit.quantity() || integer(line.get("validityDays")) != benefit.validityDays()
                    || integer(line.get("grantIntervalMonths")) != benefit.grantIntervalMonths()) throw integrity();
            if (benefit.quantity() < 1 || benefit.validityDays() < 1 || benefit.grantIntervalMonths() != catalog.grantIntervalMonths()) throw invalid();
            JsonNode schedule = line.get("schedule");
            if (!schedule.isArray() || schedule.size() != catalog.installments()) throw invalid();
            for (int index = 0; index < schedule.size(); index++) {
                JsonNode window = schedule.get(index);
                exact(window, Set.of("installmentNo", "entitlementType", "units", "validFrom", "expiresAt"));
                if (!window.get("entitlementType").isTextual() || !window.get("validFrom").isTextual() || !window.get("expiresAt").isTextual()) throw invalid();
                if (integer(window.get("installmentNo")) != index + 1 || !benefit.entitlementType().equals(window.get("entitlementType").asString())
                        || integer(window.get("units")) != benefit.quantity()) throw invalid();
                try {
                    String start = window.get("validFrom").asString(), end = window.get("expiresAt").asString();
                    if (!start.endsWith("Z") || !end.endsWith("Z") || !Instant.parse(end).isAfter(Instant.parse(start))) throw invalid();
                } catch (RuntimeException failure) { throw invalid(); }
            }
            sorted.add(line);
        }
        sorted.sort(Comparator.comparing((JsonNode line) -> line.get("entitlementType").asString()).thenComparing(line -> line.get("lineKey").asString()));
        expected.put("benefitLines", sorted); expected.put("terms", source.get("terms"));
        expected.put("catalogOfferId", catalog.offerId()); expected.put("channel", catalog.channel()); expected.put("versionToken", catalog.versionToken());
        Set<String> allowedSource = new HashSet<>(expected.keySet());
        allowedSource.removeAll(Set.of("catalogOfferId", "channel", "versionToken"));
        if (!source.propertyNames().equals(allowedSource)) throw invalid();
        return canonical(expected);
    }
    static void terms(JsonNode terms) {
        if (terms == null || !terms.isObject()) throw invalid();
        int[] nodes = {0};
        bound(terms, 1, nodes);
        if (canonical(terms).getBytes(StandardCharsets.UTF_8).length > 8192) throw boundFailure("BYTES", 8192, canonical(terms).getBytes(StandardCharsets.UTF_8).length);
    }
    private static void bound(JsonNode node, int depth, int[] nodes) {
        if (depth > 8) throw boundFailure("DEPTH", 8, depth);
        if (++nodes[0] > 256) throw boundFailure("NODES", 256, nodes[0]);
        if (node.isString()) boundedString(node.asString());
        else if (node.isObject()) node.properties().forEach(entry -> { boundedString(entry.getKey()); bound(entry.getValue(), depth + 1, nodes); });
        else if (node.isArray()) node.forEach(child -> bound(child, depth + 1, nodes));
        else if (!node.isNull() && !node.isBoolean()) integer(node);
    }
    private static void boundedString(String value) {
        scalar(value);
        int count = value.codePointCount(0, value.length());
        if (count > 1024) throw boundFailure("STRING", 1024, count);
    }
    private static OrderCreationException boundFailure(String dimension, int limit, int count) {
        String reason = switch (dimension) { case "DEPTH" -> "TERMS_DEPTH_LIMIT"; case "NODES" -> "TERMS_NODE_LIMIT"; case "STRING" -> "TERMS_TEXT_LIMIT"; default -> "TERMS_UTF8_LIMIT"; };
        LOG.warn("Checkout terms rejected reason={} dimension={} limit={} count={}", reason, dimension, limit, count);
        return invalid();
    }
    static long integer(JsonNode node) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToLong() || node.longValue() < -MAX_INTEGER || node.longValue() > MAX_INTEGER) throw invalid();
        return node.longValue();
    }
    static void exact(JsonNode node, Set<String> keys) { if (!node.isObject() || !node.propertyNames().equals(keys)) throw invalid(); }
    static void storedSnapshot(String full) {
        try {
            var node = parse(full);
            exact(node, Set.of("schemaVersion", "catalogVersionId", "catalogOfferId", "channel", "versionToken", "productCode", "versionNo", "offerType", "audience", "priceCents", "currency", "billingCycle", "installments", "benefitLines", "terms"));
            terms(node.get("terms"));
            List<CustomerOrderRepository.BenefitRow> benefits = new ArrayList<>();
            for (JsonNode line : node.get("benefitLines")) benefits.add(new CustomerOrderRepository.BenefitRow(UUID.fromString(line.get("lineKey").asString()), line.get("entitlementType").asString(), integer(line.get("quantity")), Math.toIntExact(integer(line.get("validityDays"))), Math.toIntExact(integer(line.get("grantIntervalMonths")))));
            if (benefits.isEmpty()) throw integrity();
            var source = ((tools.jackson.databind.node.ObjectNode) node).deepCopy();
            source.remove("catalogOfferId"); source.remove("channel"); source.remove("versionToken");
            String bytes = canonical(source), channel = node.get("channel").asString();
            var catalog = new CustomerOrderRepository.Catalog(UUID.fromString(node.get("catalogOfferId").asString()), UUID.fromString(node.get("catalogVersionId").asString()), null, node.get("productCode").asString(), Math.toIntExact(integer(node.get("versionNo"))), channel, "ACTIVE", "ACTIVE", node.get("versionToken").asString(), node.get("offerType").asString(), node.get("audience").asString(), integer(node.get("priceCents")), node.get("currency").asString(), node.get("billingCycle").asString(), Math.toIntExact(integer(node.get("installments"))), benefits.getFirst().grantIntervalMonths(), bytes, sha(bytes));
            payable(catalog, new Command(null, null, channel, null, null, null, null, null, null, null, null), new Caller(null, channel, null, null));
            if (integer(node.get("schemaVersion")) != 1 || catalog.versionNo() < 1 || !text(catalog.productCode(), 64) || !text(catalog.versionToken(), 128)
                    || !Set.of("WEB", "MINIAPP").contains(channel) || !snapshot(catalog, benefits).equals(canonical(node))) throw integrity();
        } catch (RuntimeException corrupt) { throw integrity(); }
    }
    static Snapshot projection(String full, String hash) {
        JsonNode node = parse(full);
        List<Benefit> benefits = new ArrayList<>();
        node.get("benefitLines").forEach(line -> benefits.add(new Benefit(line.get("entitlementType").asString(), integer(line.get("quantity")), (int) integer(line.get("validityDays")), (int) integer(line.get("grantIntervalMonths")))));
        return new Snapshot(1, UUID.fromString(node.get("catalogVersionId").asString()), UUID.fromString(node.get("catalogOfferId").asString()), node.get("channel").asString(),
                node.get("versionToken").asString(), node.get("productCode").asString(), node.get("offerType").asString(), node.get("audience").asString(), integer(node.get("priceCents")),
                node.get("currency").asString(), node.get("billingCycle").asString(), (int) integer(node.get("installments")), benefits, hash);
    }
    static String orderNo(UUID id) { return "OC-" + id.toString().replace("-", ""); }
    static OrderCreationException invalid() { return new OrderCreationException(Code.VALIDATION_FAILED); }
    static OrderCreationException integrity() { return new OrderCreationException(Code.INTERNAL); }
}
