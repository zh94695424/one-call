package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;
import com.onecall.core.OneCallApplication;
import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class CustomerOrderCreationBoundaryTests {
    static final Path MAIN = Path.of("src/main/java/com/onecall/core");
    static final List<String> PRODUCTION = List.of("customer_iam/internal/application/CustomerOrderCommandService.java", "customer_iam/internal/web/CustomerOrdersController.java", "customer_iam/internal/web/CustomerOrderJsonBinder.java", "customer_iam/internal/web/CustomerOrderDtos.java", "catalog_order/customerorder/package-info.java", "catalog_order/customerorder/CustomerOrderCreationService.java", "catalog_order/customerorder/OrderCreationException.java", "catalog_order/internal/customerorder/CustomerOrderConfiguration.java", "catalog_order/internal/customerorder/DefaultCustomerOrderCreationService.java", "catalog_order/internal/customerorder/CustomerOrderRepository.java", "catalog_order/internal/customerorder/JooqCustomerOrderRepository.java", "catalog_order/internal/customerorder/CustomerOrderSemantics.java", "activation_consent/purchaseconsent/package-info.java", "activation_consent/purchaseconsent/PurchaseConsentService.java", "activation_consent/internal/purchase/PurchaseConsentConfiguration.java", "activation_consent/internal/purchase/DefaultPurchaseConsentService.java", "activation_consent/internal/purchase/JooqPurchaseConsentRepository.java", "activation_consent/internal/purchase/PurchaseConsentSemantics.java");
    static final List<String> CURRENT_PRODUCTION = java.util.stream.Stream.concat(PRODUCTION.stream(), java.util.stream.Stream.of(
            "catalog_order/customerorder/CustomerOrderSnapshotReader.java",
            "catalog_order/internal/customerorder/DefaultCustomerOrderSnapshotReader.java")).toList();
    static final Map<String, Set<String>> CUSTOMER_ADAPTER_FOREIGN_EXCEPTIONS = Map.of(
            "customer_iam/internal/web/CustomerBoundaryFilter.java", Set.of(),
            "customer_iam/internal/web/CustomerProblemWriter.java", Set.of(
                    "com.onecall.core.sharing.internal.command.ShareCommandException",
                    "com.onecall.core.sharing.internal.publicquery.PublicShareQueryException",
                    "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException"),
            "customer_iam/internal/web/CustomerWebConfiguration.java", Set.of());
    String owner(String relative) throws Exception { return Files.readString(MAIN.resolve(relative)); }
    String graph() throws Exception { StringBuilder source = new StringBuilder(); for (String path : PRODUCTION) source.append(owner(path)).append('\n'); return source.toString(); }
    @Test void newNamedInterfacesAndExactDependencyDeltasAreBounded() throws Exception {
        var modules = ApplicationModules.of(OneCallApplication.class); modules.verify(); assertThat(modules.stream().count()).isEqualTo(14);
        assertThat(modules.getModuleByName("customer_iam").orElseThrow().getAllowedDependencies(modules)).hasSize(15);
        assertThat(modules.getModuleByName("catalog_order").orElseThrow().getAllowedDependencies(modules)).hasSize(6);
        assertThat(modules.getModuleByName("catalog_order").orElseThrow().getNamedInterfaces().getByName("customer-order-command")).isPresent();
        assertThat(modules.getModuleByName("activation_consent").orElseThrow().getNamedInterfaces().getByName("purchase-consent")).isPresent();
        assertThat(CustomerOrderCreationService.class.getDeclaredMethods()).hasSize(1); assertThat(PurchaseConsentService.class.getDeclaredMethods()).hasSize(4);
        assertThat(modules.stream().mapToLong(module -> module.getDirectDependencies(modules).uniqueModules().count()).sum()).isEqualTo(14);
        assertThat(modules.getModuleByName("entitlement_metering").orElseThrow().getDirectDependencies(modules).uniqueModules().map(module -> module.getIdentifier().toString())).doesNotContain("catalog_order");
        for (String path : PRODUCTION) if (path.contains("purchase")) assertThat(owner(path)).doesNotContain("activationtrial", "activation-trial", "issue_first_trial");
    }
    @Test void customerOrderAdapterHasNoSqlOrTransactionAuthority() throws Exception {
        for (String path : List.copyOf(PRODUCTION)) if (path.startsWith("customer_iam/")) assertAdapterHasNoAuthority(path, Set.of());
        for (var adapter : CUSTOMER_ADAPTER_FOREIGN_EXCEPTIONS.entrySet()) assertAdapterHasNoAuthority(adapter.getKey(), adapter.getValue());
        try (var files = Files.list(MAIN.resolve("customer_iam/internal/web"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                String path = MAIN.relativize(file).toString().replace('\\', '/');
                assertNoAuthorityTokens(path, owner(path));
            }
        }
    }
    void assertAdapterHasNoAuthority(String path, Set<String> allowedForeignImports) throws Exception {
        String source = owner(path); assertNoAuthorityTokens(path, source);
        assertThat(foreignInternalReferences(source)).as(path).containsExactlyInAnyOrderElementsOf(allowedForeignImports);
        assertThat(sharingInternalReferences(source)).as(path).containsExactlyInAnyOrderElementsOf(allowedForeignImports);
        assertThat(Pattern.compile("(?m)^\\s*import\\s+static\\s+com\\.onecall\\.core\\.sharing\\.internal\\.").matcher(source).find()).as(path).isFalse();
    }
    void assertNoAuthorityTokens(String path, String source) {
        assertThat(source).as(path).doesNotContain("org.jooq", "java.sql", "javax.sql", "TransactionTemplate", "@Transactional", "DataSource", "JdbcTemplate", "NamedParameterJdbcTemplate", "JdbcClient", "JdbcOperations", "DataSourceUtils", "SimpleJdbc", "org.springframework.jdbc", "SET LOCAL ROLE", "SET ROLE ", "RESET ROLE");
        assertThat(Pattern.compile("(?i)\\b(?:SET\\s+(?:LOCAL\\s+|SESSION\\s+)?ROLE|RESET\\s+ROLE|COMMIT|ROLLBACK|SAVEPOINT|RELEASE\\s+SAVEPOINT|START\\s+TRANSACTION|BEGIN\\s+(?:TRANSACTION|WORK)|END\\s+(?:TRANSACTION|WORK)|ABORT)\\b|\\b(?:commit|rollback|setSavepoint|releaseSavepoint|rollbackToSavepoint)\\s*\\(").matcher(source).find()).as(path + " manual transaction/role authority").isFalse();
    }
    Set<String> foreignInternalReferences(String source) {
        Set<String> references = new LinkedHashSet<>();
        var matcher = Pattern.compile("com\\.onecall\\.core\\.(?!customer_iam)[A-Za-z0-9_]+\\.internal\\.[A-Za-z0-9_$.]+").matcher(source);
        while (matcher.find()) references.add(matcher.group());
        return references;
    }
    Set<String> sharingInternalReferences(String source) {
        Set<String> references = new LinkedHashSet<>();
        var matcher = Pattern.compile("com\\.onecall\\.core\\.sharing\\.internal\\.[A-Za-z0-9_$.]+").matcher(source);
        while (matcher.find()) references.add(matcher.group());
        return references;
    }
    @Test void checkoutUsesOnlyExistingLoResourcesAndLiteralRoles() throws Exception {
        String source = graph(); assertThat(source).contains("operationsSubjectReadCatalogSaleTransactionTemplate", "operationsSubjectReadCatalogSaleTransactionManager", "operationsSubjectReadDataSource", "operationsSubjectReadDsl", "Propagation.MANDATORY");
        assertThat(source).doesNotContain("new Hikari", "new DataSourceTransactionManager", "SET ROLE ", "RESET ROLE", "commit()", "rollback()", "setSavepoint", "operations_iam.internal", "activationCommandDataSource");
        assertThat(source).contains("SET LOCAL ROLE catalog_order_role", "SET LOCAL ROLE activation_consent_role", "SET LOCAL ROLE NONE");
    }
    @Test void ownerSqlUsesOnlyFrozenGrantedColumnsAndWrites() throws Exception {
        String source = graph();
        assertThat(Pattern.compile("(?i)(INSERT\\s+INTO|UPDATE|DELETE\\s+FROM|MERGE\\s+INTO|TRUNCATE)\\s+catalog_order\\.catalog_(product|version|offer|benefit_line)").matcher(source).find()).isFalse();
        String catalog = owner("catalog_order/internal/customerorder/JooqCustomerOrderRepository.java");
        assertThat(catalog).doesNotContain("FOR UPDATE", "FOR SHARE", "FOR KEY SHARE", "FOR NO KEY UPDATE", "SELECT *", "consent_record.snapshot");
        assertThat(catalog).contains("id,order_no,account_id,owner_organization_id,subject_id,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,version_token,idempotency_key,request_fingerprint,revision,created_at,paid_at,closed_at", "'UNPAID'", "transaction_timestamp(),NULL,NULL", "command.clientIdempotencyKey()");
        String command = owner("catalog_order/internal/customerorder/DefaultCustomerOrderCreationService.java");
        assertThat(command.indexOf("repository.authorize")).isLessThan(command.indexOf("repository.lockPublication")); assertThat(command.indexOf("repository.lockPublication")).isLessThan(command.indexOf("repository.beginIdempotency"));
        assertThat(command.substring(command.indexOf("repository.lockPublication"))).doesNotContain("repository.lockSubject", "lockServiceActivation(", "repository.authorize(");
    }
    @Test void frozenContractsMigrationsAndPa25RemainByteIdentical() throws Exception {
        new CustomerCatalogOfferReadAclBoundaryTests().frozenContractsAndV001ThroughV018RemainByteIdentical();
        try (var migrations = Files.list(Path.of("src/main/resources/db/migration"))) { assertThat(migrations.count()).isEqualTo(20); }
        var historical = Class.forName("com.onecall.archtests.container.CustomerCatalogOfferReadPrivilegesIT").getDeclaredMethod("historicalV019Pa25Script");
        historical.setAccessible(true);
        String pa25 = ((String) historical.invoke(null)).replace("\r\n", "\n");
        assertThat(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(pa25.getBytes(java.nio.charset.StandardCharsets.UTF_8)))).isEqualTo("ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47");
    }
    @Test void commandGraphHasNoPaymentEntitlementOrRemoteSideEffects() throws Exception {
        assertThat(graph()).doesNotContain("payment_order", "payment_event", "entitlement_grant", "entitlement_issuance", "trial_grant", "HttpClient", "RestClient", "WebClient", "@Scheduled", "java.net");
    }
    @Test void productionAndTestAllowlistIsExact() throws Exception {
        assertThat(PRODUCTION).hasSize(18).doesNotHaveDuplicates(); assertThat(CURRENT_PRODUCTION).hasSize(20).doesNotHaveDuplicates(); for (String path : CURRENT_PRODUCTION) assertThat(MAIN.resolve(path)).isRegularFile();
        try (var production = Files.walk(Path.of("src/main/java")); var tests = Files.walk(Path.of("src/test/java"))) {
            assertThat(production.filter(path -> path.toString().endsWith(".java")).count()).isEqualTo(332); assertThat(tests.filter(path -> path.toString().endsWith(".java")).count()).isEqualTo(216);
        }
        for (String directory : List.of("catalog_order/internal/customerorder", "activation_consent/internal/purchase", "catalog_order/customerorder", "activation_consent/purchaseconsent")) {
            try (var files = Files.list(MAIN.resolve(directory))) { assertThat(files.filter(Files::isRegularFile).map(path -> MAIN.relativize(path).toString().replace('\\', '/')).toList()).containsExactlyInAnyOrderElementsOf(CURRENT_PRODUCTION.stream().filter(path -> path.startsWith(directory + "/")).toList()); }
        }
    }
    @Test void sourceCiPomAndXmlInventoryIsExact() throws Exception {
        new CustomerCatalogOfferReadAclBoundaryTests().sourceCiPomAndXmlIdentitiesMatchExactly();
        String pom = Files.readString(Path.of("pom.xml")); assertThat(Pattern.compile("<include>\\*\\*/[^<]+IT.java</include>").matcher(pom).results().count()).isEqualTo(46);
        String ci = Files.readString(Path.of("../.github/workflows/ci.yml")); assertThat(ci).contains("pa41_start_totals == [1071, 701]", "pa41_start_line_scan_total == 1773", "method_totals == [1127, 738]", "line_scan_total == 1866");
    }
}
