package com.onecall.core.catalog_order.internal.operationsdraft;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;

import com.onecall.core.catalog_order.internal.operationsdraft.JooqCatalogDraftRepository.StoredDraft;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class CatalogDraftSemantics {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Set<String> ROOT = Set.of("productId", "versionNo", "offerType", "audience", "priceCents", "currency", "billingCycle", "installments", "grantIntervalMonths", "benefitLines", "channels");
    private static final Set<String> BENEFIT = Set.of("entitlementType", "quantity", "validityDays", "grantIntervalMonths");
    private static final Set<String> ENTITLEMENTS = Set.of("AI_SECONDS", "EVIDENCE_COUNT", "RECORDING_SECONDS", "STORAGE_BYTES");
    private static final Set<String> CHANNELS = Set.of("MINIAPP", "WEB");

    static void validateCommercial(Commercial commercial) {
        if (commercial == null || commercial.billingCycle() == null || !"CNY".equals(commercial.currency()) || commercial.installments() < 1 || commercial.grantIntervalMonths() < 0
                || commercial.priceCents() != null && commercial.priceCents() < 0) throw new Failure(Code.VALIDATION_FAILED);
        boolean valid = switch (commercial.offerType() == null ? "" : commercial.offerType()) {
            case "SUBSCRIPTION" -> "PERSONAL".equals(commercial.audience()) && commercial.priceCents() != null
                    && List.of("MONTHLY", "QUARTERLY", "ANNUAL").contains(commercial.billingCycle()) && commercial.grantIntervalMonths() > 0;
            case "ONE_TIME", "ADD_ON" -> "PERSONAL".equals(commercial.audience()) && commercial.priceCents() != null
                    && "ONCE".equals(commercial.billingCycle()) && commercial.installments() == 1 && commercial.grantIntervalMonths() == 0;
            case "QUOTE" -> "ENTERPRISE".equals(commercial.audience()) && commercial.priceCents() == null
                    && "QUOTE".equals(commercial.billingCycle()) && commercial.installments() == 1 && commercial.grantIntervalMonths() == 0;
            default -> false;
        };
        if (!valid) throw new Failure(Code.VALIDATION_FAILED);
    }
    static String projection(UUID productId, int versionNo, Commercial commercial, List<BenefitLine> benefits, List<String> channels) {
        validateCommercial(commercial);
        if (productId == null || versionNo < 1 || benefits == null || channels == null) throw new Failure(Code.VALIDATION_FAILED);
        Set<String> identities = new HashSet<>(); List<Object> lines = new ArrayList<>();
        for (BenefitLine benefit : benefits) {
            if (benefit == null || benefit.entitlementType() == null || !ENTITLEMENTS.contains(benefit.entitlementType()) || !identities.add(benefit.entitlementType())
                    || benefit.quantity() < 1 || benefit.validityDays() < 1 || benefit.grantIntervalMonths() < 0) throw new Failure(Code.VALIDATION_FAILED);
            lines.add(new TreeMap<>(Map.of("entitlementType", benefit.entitlementType(), "quantity", benefit.quantity(), "validityDays", benefit.validityDays(), "grantIntervalMonths", benefit.grantIntervalMonths())));
        }
        lines.sort(Comparator.comparing(line -> ((Map<?, ?>) line).get("entitlementType").toString()));
        identities.clear();
        for (String channel : channels) if (channel == null || !CHANNELS.contains(channel) || !identities.add(channel)) throw new Failure(Code.VALIDATION_FAILED);
        var result = new TreeMap<String, Object>();
        result.put("productId", productId.toString()); result.put("versionNo", versionNo); result.put("offerType", commercial.offerType()); result.put("audience", commercial.audience());
        result.put("priceCents", commercial.priceCents()); result.put("currency", commercial.currency()); result.put("billingCycle", commercial.billingCycle());
        result.put("installments", commercial.installments()); result.put("grantIntervalMonths", commercial.grantIntervalMonths()); result.put("benefitLines", lines);
        result.put("channels", channels.stream().sorted().toList());
        return MAPPER.writeValueAsString(result);
    }
    static byte[] canonicalBytes(String json) { return MAPPER.writeValueAsBytes(canonical(MAPPER.readTree(json))); }
    private static Object canonical(JsonNode node) {
        if (node.isObject()) {
            var result = new TreeMap<String, Object>();
            for (var entry : node.properties()) result.put(entry.getKey(), canonical(entry.getValue()));
            return result;
        }
        if (node.isArray()) { var result = new ArrayList<>(); for (JsonNode entry : node) result.add(canonical(entry)); return result; }
        if (node.isNull()) return null;
        if (node.isString()) return node.asString();
        if (node.isIntegralNumber()) return node.bigIntegerValue();
        if (node.isBoolean()) return node.booleanValue();
        throw new Failure(Code.SERVICE_UNAVAILABLE);
    }
    static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException("Digest unavailable"); }
    }
    static String sha256(String json) { return sha256(json.getBytes(StandardCharsets.UTF_8)); }
    static boolean sameProjection(String left, String right) { return Arrays.equals(canonicalBytes(left), canonicalBytes(right)); }
    static String validateStored(StoredDraft draft) {
        try {
            if (draft == null || draft.id() == null || draft.productId() == null || draft.creator() == null || draft.versionNo() < 1 || draft.revision() < 1
                    || draft.createdAt() == null || draft.createdAt().getNano() % 1000 != 0 || draft.hash() == null || !draft.hash().matches("[0-9a-f]{64}")
                    || draft.saleStatus() == null || !Set.of("DRAFT", "ACTIVE", "OFFSALE").contains(draft.saleStatus())
                    || "DRAFT".equals(draft.saleStatus()) != (draft.publishedAt() == null)) throw new Failure(Code.SERVICE_UNAVAILABLE);
            validateCommercial(draft.commercial());
            Set<UUID> identities = new HashSet<>(); List<BenefitLine> lines = new ArrayList<>();
            for (var benefit : draft.benefits()) {
                if (benefit.id() == null || !identities.add(benefit.id()) || !draft.id().equals(benefit.versionId())) throw new Failure(Code.SERVICE_UNAVAILABLE);
                lines.add(benefit.line());
            }
            List<String> channels = new ArrayList<>();
            for (var offer : draft.offers()) {
                if (offer.id() == null || !identities.add(offer.id()) || !draft.id().equals(offer.versionId()) || offer.createdAt() == null
                        || offer.revision() < 1 || offer.status() == null || !Set.of("ACTIVE", "WITHDRAWN").contains(offer.status())
                        || offer.token() == null || !offer.token().matches("[0-9a-f]{64}")) throw new Failure(Code.SERVICE_UNAVAILABLE);
                channels.add(offer.channel());
            }
            String expected = projection(draft.productId(), draft.versionNo(), draft.commercial(), lines, channels);
            JsonNode snapshot = MAPPER.readTree(draft.snapshot());
            if (snapshot == null || !snapshot.isObject()) throw new Failure(Code.SERVICE_UNAVAILABLE);
            if (!"DRAFT".equals(draft.saleStatus())) throw new Failure(Code.CATALOG_DRAFT_CONFLICT);
            if (!members(snapshot, ROOT)) throw new Failure(Code.CATALOG_DRAFT_CONFLICT);
            if (!snapshot.get("benefitLines").isArray()) throw new Failure(Code.SERVICE_UNAVAILABLE);
            for (JsonNode line : snapshot.get("benefitLines")) {
                if (!line.isObject()) throw new Failure(Code.SERVICE_UNAVAILABLE);
                if (!members(line, BENEFIT)) throw new Failure(Code.CATALOG_DRAFT_CONFLICT);
            }
            String actual = storedProjection(snapshot);
            if (!sameProjection(expected, actual) || !sha256(canonicalBytes(actual)).equals(draft.hash())) throw new Failure(Code.SERVICE_UNAVAILABLE);
            if (draft.offers().stream().anyMatch(offer -> !"ACTIVE".equals(offer.status()))) throw new Failure(Code.CATALOG_DRAFT_CONFLICT);
            return expected;
        } catch (Failure known) { if (known.code() == Code.VALIDATION_FAILED) throw new Failure(Code.SERVICE_UNAVAILABLE); throw known; }
        catch (RuntimeException malformed) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }
    private static String storedProjection(JsonNode root) {
        String product = string(root.get("productId")); UUID productId = UUID.fromString(product);
        if (!productId.toString().equals(product)) throw new Failure(Code.SERVICE_UNAVAILABLE);
        JsonNode price = root.get("priceCents");
        Commercial commercial = new Commercial(string(root.get("offerType")), string(root.get("audience")), price.isNull() ? null : number(price),
                string(root.get("currency")), string(root.get("billingCycle")), Math.toIntExact(number(root.get("installments"))), Math.toIntExact(number(root.get("grantIntervalMonths"))));
        List<BenefitLine> benefits = new ArrayList<>();
        for (JsonNode line : root.get("benefitLines")) benefits.add(new BenefitLine(string(line.get("entitlementType")), number(line.get("quantity")),
                Math.toIntExact(number(line.get("validityDays"))), Math.toIntExact(number(line.get("grantIntervalMonths")))));
        if (!root.get("channels").isArray()) throw new Failure(Code.SERVICE_UNAVAILABLE);
        List<String> channels = new ArrayList<>(); for (JsonNode channel : root.get("channels")) channels.add(string(channel));
        return projection(productId, Math.toIntExact(number(root.get("versionNo"))), commercial, benefits, channels);
    }
    private static String string(JsonNode node) { if (node == null || !node.isString()) throw new Failure(Code.SERVICE_UNAVAILABLE); return node.asString(); }
    private static long number(JsonNode node) { if (node == null || !node.isIntegralNumber()) throw new Failure(Code.SERVICE_UNAVAILABLE); return node.bigIntegerValue().longValueExact(); }
    private static boolean members(JsonNode node, Set<String> fields) { return node.size() == fields.size() && fields.stream().allMatch(node::has); }
    static CatalogVersionDraft response(StoredDraft draft) {
        validateStored(draft);
        Commercial commercial = draft.commercial();
        return new CatalogVersionDraft(draft.id(), draft.productId(), draft.versionNo(), "DRAFT", commercial.offerType(), commercial.audience(), commercial.priceCents(), commercial.currency(),
                commercial.billingCycle(), commercial.installments(), commercial.grantIntervalMonths(), draft.benefits().stream().map(benefit -> benefit.line()).sorted(Comparator.comparing(BenefitLine::entitlementType)).toList(),
                draft.offers().stream().map(offer -> offer.channel()).sorted().toList(), draft.revision(), draft.createdAt(), null, draft.hash());
    }
    static String token(UUID versionId, String channel, int revision, String hash) { return sha256("catalog-draft-token:v1\n" + versionId + "\n" + channel + "\n" + revision + "\n" + hash); }
    static int increment(int revision) { if (revision < 1 || revision == Integer.MAX_VALUE) throw new Failure(Code.SERVICE_UNAVAILABLE); return revision + 1; }
    static String auditSnapshot(UUID productId, int versionNo, int revision, String hash) {
        var result = new LinkedHashMap<String, Object>(); result.put("productId", productId.toString()); result.put("versionNo", versionNo);
        result.put("saleStatus", "DRAFT"); result.put("revision", revision); result.put("snapshotSha256", hash); return MAPPER.writeValueAsString(result);
    }
    static String auditRequestId(UUID versionId, boolean create, int revision) { return "catalog_version:" + versionId + (create ? ":draft:create" : ":draft:replace:" + revision); }
}
