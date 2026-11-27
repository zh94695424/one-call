package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService;
import com.onecall.core.catalog_order.customerorder.CustomerOrderSnapshotReader;
import com.onecall.core.catalog_order.customerquery.CustomerOrderReader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CustomerOrderReadBoundaryTests {
    static final Path MAIN = Path.of("src/main/java/com/onecall/core");
    @SuppressWarnings("unchecked")
    static java.util.Set<String> oracle(String name) throws Exception {
        Method method = Class.forName("com.onecall.archtests.container.CustomerOrderReadPrivilegesIT").getDeclaredMethod(name);
        method.setAccessible(true);
        return (java.util.Set<String>) method.invoke(null);
    }
    static String source(String path) throws Exception { return Files.readString(MAIN.resolve(path)); }
    @Test void ownerPortNamedInterfaceAndSqlBoundaryAreExact() {
        assertThat(Arrays.stream(CustomerOrderReader.class.getDeclaredMethods()).map(Method::getName).toList())
                .containsExactlyInAnyOrder("fetchOrders", "fetchOrder", "fetchIssuances");
        assertThat(Arrays.stream(CustomerOrderSnapshotReader.class.getDeclaredMethods()).map(Method::getName).toList()).containsExactly("read");
        assertThat(Arrays.stream(CustomerOrderCreationService.class.getDeclaredMethods()).map(Method::getName).toList()).containsExactly("create");
    }
    @Test void snapshotSeamValidatesWithoutDuplicatingProjection() throws Exception {
        String service = source("customer_iam/internal/application/CustomerOrderQueryService.java");
        assertThat(service).contains("SET LOCAL ROLE customer_iam_role", "requireActiveContext", "transaction.execute");
        assertThat(service).doesNotContain("FROM catalog_order.", "FROM entitlement_metering.", "new Hikari", "INSERT ", "UPDATE ", "DELETE ");
        String owner = source("catalog_order/internal/customerquery/JooqCustomerOrderReader.java");
        assertThat(owner).contains("o.account_id = CAST(? AS uuid)", "o.owner_organization_id", "ORDER BY o.created_at DESC, o.id DESC LIMIT ?");
        assertThat(owner).doesNotContain("FOR UPDATE", "FOR SHARE", "SELECT *", "payment_order", "entitlement_grant", "OFFSET", "COUNT(");
        String seam = source("catalog_order/internal/customerorder/DefaultCustomerOrderSnapshotReader.java");
        assertThat(seam).contains("storedSnapshot", "canonical", "projection", "sha");
    }
    @Test void v020HasOnlyApprovedGrantActionsAndFailureCategories() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V020__customer_order_read_privileges.sql"));
        for (String category : List.of("AUTHORITY", "PROVENANCE", "HISTORY", "SETTINGS", "ATTRIBUTES", "TOPOLOGY",
                "OWNERSHIP", "ACL", "DATABASE", "DEFAULT_ACL", "DEPENDENCY", "PRESERVATION", "RECONCILIATION"))
            assertThat(sql).contains("V020_" + category + "_REJECTED");
        assertThat(sql).contains("DO $migration$", "725025, 1", "FOR pass IN 1..2 LOOP", "NOT IN (19, 20)",
                "expected_acls = old_acls", "SET LOCAL ROLE catalog_order_owner", "SET LOCAL ROLE entitlement_metering_owner");
        assertThat(sql).doesNotContain("CREATE TABLE", "CREATE INDEX", "CREATE ROLE", "REVOKE ", "INSERT INTO", "DELETE FROM");
        assertThat(java.util.regex.Pattern.compile("(?ms)^\\s+GRANT\\s+(?:SELECT|USAGE)\\b.*?;").matcher(sql).results()
                .map(java.util.regex.MatchResult::group).toList()).hasSize(3)
                .allSatisfy(grant -> assertThat(grant).doesNotContain("WITH GRANT OPTION"));
        CustomerCatalogOfferReadAclBoundaryTests.currentMigrationProof();
    }
    @Test void frozenContractsAndV001ThroughV019RemainByteIdentical() throws Exception {
        new CustomerCatalogOfferBoundaryTests().openApiAndV001ThroughV019RemainFrozenWithoutContractExpansion();
        java.util.List<String> manifest = new java.util.ArrayList<>();
        try (var files = Files.list(Path.of("src/main/resources/db/migration"))) {
            for (Path file : files.filter(path -> !path.getFileName().toString().equals("V020__customer_order_read_privileges.sql")).toList()) {
                byte[] bytes = Files.readString(file).replace("\r\n", "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                manifest.add(file.getFileName() + " " + java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)));
            }
        }
        assertThat(manifest).hasSize(19);
        java.util.Collections.sort(manifest);
        assertThat(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest((String.join("\n", manifest) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .isEqualTo("943375ab52e9b214c90ad8d00ce6bfcfadf7ab4ad6e72791a96428e12cfe7d86");
        assertThat(source("catalog_order/customerquery/package-info.java"))
                .contains("\"customer-subject-count-query\"", "\"customer-order-query\"");
    }
    @Test void finalSourceCiPomXmlAndJarInventoriesAreExact() throws Exception {
        String ci = Files.readString(Path.of("../.github/workflows/ci.yml"));
        String pom = Files.readString(Path.of("pom.xml"));
        assertThat(ci).contains("current_method_totals == [1202, 775]", "current_line_scan_total == 1978",
                "exact_inventory(\"target/surefire-reports/TEST-*.xml\", unit_expected, 1202)",
                "exact_inventory(\"target/failsafe-reports/TEST-*.xml\", container_expected, 775)",
                "V020__customer_order_read_privileges.sql", "len(unit_expected) == 130",
                "len(container_expected) == 48", "len(includes) == 48");
        assertThat(pom).contains("<include>**/CustomerOrdersReadIT.java</include>",
                "<include>**/CustomerOrderReadPrivilegesIT.java</include>");
        assertThat(ci).contains("source/CI inventory mismatch", "XML/CI inventory mismatch");
        try (var files = Files.walk(Path.of("src/main/java"))) {
            assertThat(files.filter(path -> path.toString().endsWith(".java")).count()).isEqualTo(341);
        }
        try (var files = Files.walk(Path.of("src/test/java"))) {
            assertThat(files.filter(path -> path.toString().endsWith(".java")).count()).isEqualTo(228);
        }
    }
}
