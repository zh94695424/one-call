package com.onecall.core.catalog_order.internal.operationscommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.time.Instant;
import org.jooq.Record;
import tools.jackson.databind.json.JsonMapper;
import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand.*;

public final class CatalogSaleLifecycleSemantics {
    private CatalogSaleLifecycleSemantics() { }
    public static String lockKey(UUID productId) { return "catalog_order.publication:" + productId; }
    public static String hex(byte[] bytes) {
        try { var digest = MessageDigest.getInstance("SHA-256").digest(bytes); var result = new StringBuilder(64); for (byte value : digest) result.append(String.format("%02x", value)); return result.toString(); }
        catch (Exception failure) { throw new IllegalStateException(failure); }
    }
    public static String sha256(String value) { return hex(value.getBytes(StandardCharsets.UTF_8)); }
    static String dedupe(UUID version, boolean publication) { return "catalog_version:" + version + (publication ? ":published" : ":offsale"); }
    static int increment(int revision) { if (revision < 1 || revision == Integer.MAX_VALUE) throw unavailable(); return revision + 1; }
    static String snapshot(String status, int revision, String hash) { return "{\"status\":\"" + status + "\",\"revision\":" + revision + (hash == null ? "}" : ",\"snapshotSha256\":\"" + hash + "\"}"); }
    static String payload(UUID version, UUID product, int number, String status, Instant published, String hash) {
        return "{\"catalogVersionId\":\"" + version + "\",\"productId\":\"" + product + "\",\"versionNo\":" + number + ",\"saleStatus\":\"" + status + "\",\"publishedAt\":" + (published == null ? "null" : "\"" + published + "\"") + ",\"snapshotSha256\":\"" + hash + "\"}";
    }
    static void validate(Record row) {
        try {
            if (row == null) throw new Failure(Code.NOT_FOUND);
            for (String field : Set.of("id", "product_id", "version_no", "sale_status", "offer_type", "audience", "currency", "billing_cycle", "installments", "grant_interval_months", "snapshot", "snapshot_sha256", "created_by_employee_id", "created_at", "revision")) if (row.get(field) == null) throw unavailable();
            String status = row.get("sale_status", String.class), offer = row.get("offer_type", String.class), audience = row.get("audience", String.class), cycle = row.get("billing_cycle", String.class);
            Long price = row.get("price_cents", Long.class);
            if (!Set.of("DRAFT", "ACTIVE", "OFFSALE").contains(status) || !Set.of("SUBSCRIPTION", "ONE_TIME", "ADD_ON", "QUOTE").contains(offer)
                    || !Set.of("PERSONAL", "ENTERPRISE").contains(audience) || "ENTERPRISE".equals(audience) != "QUOTE".equals(offer)
                    || (price == null) != "QUOTE".equals(offer) || price != null && price < 0
                    || !row.get("currency", String.class).matches("[A-Z]{3}") || row.get("version_no", Integer.class) < 1 || row.get("revision", Integer.class) < 1
                    || "DRAFT".equals(status) != (row.get("published_at") == null) || !row.get("snapshot_sha256", String.class).matches("[0-9a-f]{64}")) throw unavailable();
            int installments = row.get("installments", Integer.class), interval = row.get("grant_interval_months", Integer.class);
            if ("SUBSCRIPTION".equals(offer)) {
                if (!Set.of("MONTHLY", "QUARTERLY", "ANNUAL").contains(cycle) || installments < 1 || interval < 1) throw unavailable();
            } else if (!cycle.equals("QUOTE".equals(offer) ? "QUOTE" : "ONCE") || installments != 1 || interval != 0) throw unavailable();
            if (!JsonMapper.builder().build().readTree(row.get("snapshot").toString()).isObject()) throw unavailable();
        } catch (Failure failure) { throw failure; }
        catch (RuntimeException failure) { throw unavailable(); }
    }
    static void validateBenefits(UUID version, Iterable<? extends Record> benefits) {
        var identities = new HashSet<UUID>();
        var types = new HashSet<String>();
        try {
            for (Record row : benefits) {
                UUID id = row.get("id", UUID.class);
                String type = row.get("entitlement_type", String.class);
                if (id == null || !identities.add(id) || !version.equals(row.get("catalog_version_id", UUID.class))
                        || type == null || !Set.of("RECORDING_SECONDS", "AI_SECONDS", "EVIDENCE_COUNT", "STORAGE_BYTES").contains(type) || !types.add(type)
                        || row.get("quantity", Long.class) < 1 || row.get("validity_days", Integer.class) < 1 || row.get("grant_interval_months", Integer.class) < 0) throw unavailable();
            }
        } catch (RuntimeException failure) { throw unavailable(); }
    }
    private static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
}
