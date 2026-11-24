package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

public final class OperationsCatalogVersionBoundaryTests {
    String source(String path) throws Exception { return Files.readString(Path.of("src/main/java/com/onecall/core/" + path)); }
    @Test void scansExactProductionFiles() throws Exception { try (var java = Files.walk(Path.of("src/main/java")); var tests = Files.walk(Path.of("src/test/java"))) { assertThat(java.filter(path -> path.toString().endsWith(".java")).count()).isEqualTo(307); assertThat(tests.filter(path -> path.toString().endsWith(".java")).count()).isEqualTo(199); } }
    @Test void commandPortIsOperationsIamOwned() throws Exception { assertThat(source("operations_iam/operationscommand/package-info.java")).contains("operations-catalog-sale-command"); assertThat(source("operations_iam/internal/web/OperationsCatalogVersionController.java")).doesNotContain("com.onecall.core.catalog_order"); }
    @Test void catalogOrderImplementsNamedPortWithoutReverseEdge() throws Exception { assertThat(source("catalog_order/package-info.java")).contains("operations_iam::operations-catalog-sale-command", "operations_iam::employee-authority-read", "operations_iam::operations-subject-read"); assertThat(source("operations_iam/package-info.java")).doesNotContain("catalog_order").contains("operations_audit"); }
    @Test void employeeAuthorityPortIsOperationsIamOwned() throws Exception { assertThat(source("operations_iam/employeeauthority/package-info.java")).contains("employee-authority-read"); assertThat(source("operations_iam/employeeauthority/EmployeeAuthorityReadPort.java")).contains("TransactionWitness", "revision").doesNotContain("java.sql", "org.jooq", "org.springframework"); }
    @Test void catalogSqlNeverReadsEmployeeAccount() throws Exception { try (var paths = Files.list(Path.of("src/main/java/com/onecall/core/catalog_order/internal/operationscommand"))) { for (Path path : paths.toList()) assertThat(Files.readString(path)).doesNotContain("employee_account"); } assertThat(source("operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java")).contains("SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = ?").doesNotContain("FOR UPDATE", "advisory", "REQUIRES_NEW"); }
    @Test void rejectsSelectStarForbiddenTablesAndContentWrites() throws Exception { String sql = source("catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java"); assertThat(sql).doesNotContain("SELECT *", "INSERT INTO", "DELETE FROM", "SET snapshot", "SET price_cents", "UPDATE catalog_order.catalog_benefit_line", "UPDATE catalog_order.catalog_offer", "UPDATE catalog_order.customer_order"); assertThat(sql).contains("audit.append_fact", "platform.outbox_append", "revision=revision+1"); }
    @Test void requiresOneTransactionAndPublicationOrder() throws Exception {
        String sql = source("catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java"); assertThat(sql.indexOf("pg_advisory_xact_lock")).isLessThan(sql.indexOf("SELECT clock_timestamp()")); assertThat(sql.indexOf("SET sale_status='OFFSALE'")).isLessThan(sql.indexOf("SET published_at=?"));
        String filter = source("operations_iam/internal/web/OperationsBoundaryFilter.java"); assertThat(filter).contains("[^/;%]+/(publications|offsales)", "[^/;%]+/(suspensions|resumptions)", "!(request.getContextPath() + path).equals(request.getRequestURI())", "authenticator.authenticate(request.getHeaders(\"Authorization\"))");
        assertThat(source("customer_iam/internal/web/CustomerBoundaryFilter.java")).contains("[^/;%]+/(publications|offsales)", "(request.getContextPath() + operationsPath).equals(request.getRequestURI())");
    }
    @Test void preservesFrozenInputsAndNoV019() throws Exception {
        var pins = Map.of("../api/openapi/onecall-v1.openapi.yaml", "0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68", "ops/postgresql/pa25-runtime-identities.sql", "b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9");
        for (var pin : pins.entrySet()) assertThat(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((pin.getKey().equals("ops/postgresql/pa25-runtime-identities.sql") ? CustomerCatalogOfferReadAclBoundaryTests.historicalPa25().getBytes(java.nio.charset.StandardCharsets.UTF_8) : Files.readAllBytes(Path.of(pin.getKey())))))).isEqualTo(pin.getValue());
        CustomerCatalogOfferReadAclBoundaryTests.currentMigrationProof();
        byte[] current = Files.readAllBytes(Path.of("ops/postgresql/pa25-runtime-identities.sql"));
        assertThat(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(new String(current,java.nio.charset.StandardCharsets.UTF_8).replace("\r\n","\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)))).isEqualTo("ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47");
    }
}
