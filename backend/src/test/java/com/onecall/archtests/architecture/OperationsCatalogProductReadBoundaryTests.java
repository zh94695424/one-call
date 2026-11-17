package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class OperationsCatalogProductReadBoundaryTests {
    private List<Path> files() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java"))) { return files.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().contains("OperationsCatalogProduct")).sorted().toList(); }
    }
    private String source() throws Exception { var result = new StringBuilder(); for (Path file : files()) result.append(Files.readString(file)).append('\n'); return result.toString(); }
    private String reader() throws Exception { return Files.readString(Path.of("src/main/java/com/onecall/core/catalog_order/internal/operationsread/JooqOperationsCatalogProductReader.java")); }
    private List<String> sql() throws Exception { return Pattern.compile("\"(SELECT [^\\n]+?)\"").matcher(reader()).results().map(match -> match.group(1)).toList(); }
    @Test void scansAllEightProductionFiles() throws Exception { assertThat(files()).hasSize(8); assertThat(files().stream().map(path -> path.getFileName().toString())).containsExactlyInAnyOrder("OperationsCatalogProductReadController.java", "OperationsCatalogProductReadService.java", "OperationsCatalogProductReadPorts.java", "OperationsCatalogProductReadRequest.java", "OperationsCatalogProductCursorCodec.java", "DefaultOperationsCatalogProductReadService.java", "OperationsCatalogProductReadConfiguration.java", "JooqOperationsCatalogProductReader.java"); }
    @Test void rejectsSelectStarAndForbiddenTokens() throws Exception { assertThat(source()).doesNotContain("SELECT *", "ciphertext", "snapshot_sha256", "version_token", "created_by_employee_id", "payment_status", "media_url"); }
    @Test void rejectsWritesAndLocks() throws Exception { for (String sql : sql()) assertThat(sql.toUpperCase(Locale.ROOT)).doesNotContain("INSERT ", "UPDATE ", "DELETE ", "CREATE ", "ALTER ", "DROP ", "FOR UPDATE", "ADVISORY", "LOCK ", "OUTBOX", "AUDIT"); }
    @Test void rejectsOfferReads() throws Exception { assertThat(source()).doesNotContain("catalog_offer", "catalogOffer", " JOIN ", " EXISTS "); }
    @Test void rejectsUnapprovedTablesAndColumns() throws Exception {
        var tables = Pattern.compile("FROM ([a-z_]+\\.[a-z_]+)").matcher(reader()).results().map(match -> match.group(1)).collect(java.util.stream.Collectors.toSet());
        assertThat(tables).containsExactlyInAnyOrder("catalog_order.catalog_product", "catalog_order.catalog_version", "catalog_order.catalog_benefit_line");
        Set<String> approved = Set.of("id", "product_code", "product_family", "product_id", "version_no", "sale_status", "offer_type", "audience", "price_cents", "currency", "billing_cycle", "installments", "grant_interval_months", "published_at", "revision", "catalog_version_id", "entitlement_type", "quantity", "validity_days");
        var selections = Pattern.compile("SELECT ([a-z_, ]+) FROM").matcher(reader()).results().map(match -> match.group(1)).toList(); assertThat(selections).hasSize(6);
        for (String selection : selections) assertThat(Arrays.asList(selection.split(", "))).isSubsetOf(approved);
    }
    @Test void assertsApprovedProjection() throws Exception { assertThat(reader()).contains("SELECT id, product_code, product_family FROM", "SELECT product_id, id, version_no, sale_status, offer_type, audience FROM", "SELECT id, catalog_version_id, entitlement_type, quantity, validity_days, grant_interval_months FROM", "COLLATE \\\"C\\\"", "ORDER BY product_id, version_no, id", "ORDER BY catalog_version_id, id", "context.scoped(Role.ORDER"); }
    @Test void assertsNoNewResourceConstruction() throws Exception {
        assertThat(source()).doesNotContain("new Hikari", "new TransactionTemplate", "new OperationsReadContext", "DSL.using", "DriverManager");
        for (Path file : files()) if (file.toString().contains("operations_iam")) assertThat(Files.readString(file)).doesNotContain("com.onecall.core.catalog_order.internal");
    }
    @Test void preservesFrozenOpenApiReference() throws Exception {
        byte[] bytes = Files.readString(Path.of("../api/openapi/onecall-v1.openapi.yaml")).replace("\r\n", "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))).isEqualTo("ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30");
    }
}
