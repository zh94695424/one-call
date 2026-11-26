package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class CustomerCatalogOfferBoundaryTests {
    @Test
    void catalogOrderExposesOnlyTheNamedCustomerCatalogOfferQueryInterface() {
        var modules = ApplicationModules.of(OneCallApplication.class);
        var interfaces = modules.getModuleByName("catalog_order").orElseThrow().getNamedInterfaces();
        assertThat(interfaces.getByName("customer-catalog-offer-query")).isPresent();
        assertThat(interfaces.getByName("customer-subject-count-query")).isPresent();
        assertThat(interfaces.stream().flatMap(named -> named.asJavaClasses()).map(type -> type.getName()))
                .noneMatch(name -> name.contains(".internal.customercatalogquery."));
        assertThat(CustomerCatalogOfferReader.class.getDeclaredMethods()).extracting(java.lang.reflect.Method::getName)
                .containsExactlyInAnyOrder("fetchOffers", "fetchBenefits");
        for (var method : CustomerCatalogOfferReader.class.getDeclaredMethods())
            assertThat(method.toGenericString()).doesNotContain("org.jooq", "java.sql", "customer_iam.internal");
        assertThat(Arrays.stream(CustomerCatalogOfferReader.Scope.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("channel", "audience", "offerType");
        assertThat(Arrays.stream(CustomerCatalogOfferReader.Anchor.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("productCode", "versionNo", "catalogOfferId");
        assertThat(Arrays.stream(CustomerCatalogOfferReader.OfferRow.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("productCode", "catalogVersionId", "catalogOfferId", "channel", "versionNo", "offerType", "audience",
                        "priceCents", "currency", "billingCycle", "installments", "versionToken");
        assertThat(Arrays.stream(CustomerCatalogOfferReader.BenefitRow.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("catalogVersionId", "entitlementType", "quantity", "validityDays", "grantIntervalMonths");
        modules.verify();
    }

    @Test
    void customerImportsOnlyThePublicCatalogSeamAndNoForeignInternalImplementation() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/com/onecall/core/customer_iam"))) {
            for (var file : files.filter(Files::isRegularFile).toList()) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("catalog_order.internal", "catalog_order_owner");
            }
        }
        var modules = ApplicationModules.of(OneCallApplication.class);
        assertThat(modules.getModuleByName("customer_iam").orElseThrow().getAllowedDependencies(modules)).hasSize(15);
        modules.verify();
    }

    @Test
    void controllerServiceAndOwnerSqlStayInsideTheirSemanticOwners() throws Exception {
        Path root = Path.of("src/main/java/com/onecall/core");
        String controller = Files.readString(root.resolve("customer_iam/internal/web/CustomerCatalogOffersController.java"));
        assertThat(controller).contains("ObjectProvider<CustomerCatalogOfferQueryService>", "ObjectProvider<CustomerCatalogOfferCursorCodec>")
                .doesNotContain("DSLContext", "DataSource", "Repository", "getAttribute(", "getSession(");
        String service = Files.readString(root.resolve("customer_iam/internal/application/CustomerCatalogOfferQueryService.java"));
        assertThat(service).doesNotContain("FROM catalog_order.", "INSERT ", "UPDATE ", "DELETE ");
        String reader = Files.readString(root.resolve("catalog_order/internal/customercatalogquery/JooqCustomerCatalogOfferReader.java"));
        assertThat(reader).contains("v.sale_status = 'ACTIVE'", "o.offer_status = 'ACTIVE'", "o.channel = ?", "v.audience = ?",
                "ORDER BY p.product_code COLLATE \"C\" ASC, v.version_no DESC, o.id ASC", "LIMIT ?")
                .doesNotContain("customer_iam.internal", "TransactionTemplate", "SET LOCAL ROLE", "OFFSET", "SELECT *", "FOR UPDATE", "FOR SHARE");
    }

    @Test
    void readerProjectsOnlyTheTwentyTwoApprovedColumns() throws Exception {
        String reader = Files.readString(Path.of("src/main/java/com/onecall/core/catalog_order/internal/customercatalogquery/JooqCustomerCatalogOfferReader.java"));
        String offerQuery = reader.substring(reader.indexOf("fetchOffers"), reader.indexOf("fetchBenefits"));
        assertThat(offerQuery).doesNotContain("grant_interval_months");
        assertThat(reader).contains("grant_interval_months");
        for (String column : List.of("product_code", "catalog_version_id", "catalog_offer_id", "channel", "version_no", "offer_type",
                "audience", "price_cents", "currency", "billing_cycle", "installments", "version_token", "entitlement_type", "quantity",
                "validity_days", "grant_interval_months", "sale_status", "offer_status", "product_id"))
            assertThat(reader).as(column).contains(column);
        for (String adjacent : List.of("product_family", "created_at", "snapshot", "snapshot_sha256", "created_by_employee_id", "published_at", "revision"))
            assertThat(reader).as(adjacent).doesNotContain(adjacent);
        assertThat(reader).doesNotContain("SELECT *", "OFFSET", "FOR UPDATE", "FOR SHARE");
    }

    @Test
    void openApiAndV001ThroughV019RemainFrozenWithoutContractExpansion() throws Exception {
        Map<String, String> frozen = Map.ofEntries(
                Map.entry("../api/openapi/onecall-v1.openapi.yaml", "ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30"),
                Map.entry("../api/openapi/media-gateway-v1.openapi.yaml", "7f169b083125b890129e5b640d904034d49bd64ad063300c1459cde596213b65"),
                Map.entry("src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql", "946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045"),
                Map.entry("src/main/resources/db/migration/V002__identity_organization_and_employee.sql", "534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5"),
                Map.entry("src/main/resources/db/migration/V003__subject_number_and_claim.sql", "1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1"),
                Map.entry("src/main/resources/db/migration/V004__catalog_order_and_payment.sql", "a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc"),
                Map.entry("src/main/resources/db/migration/V005__ivr_task_session_call.sql", "2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310"),
                Map.entry("src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql", "830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475"),
                Map.entry("src/main/resources/db/migration/V007__activation_and_consent.sql", "bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659"),
                Map.entry("src/main/resources/db/migration/V008__evidence_media.sql", "2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e"),
                Map.entry("src/main/resources/db/migration/V009__sharing.sql", "ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae"),
                Map.entry("src/main/resources/db/migration/V010__sharing_role_least_privilege.sql", "fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343"),
                Map.entry("src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql", "f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b"),
                Map.entry("src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql", "13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828"),
                Map.entry("src/main/resources/db/migration/V013__evidence_media_reader_role.sql", "691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6"),
                Map.entry("src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql", "e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a"),
                Map.entry("src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql", "1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3"),
                Map.entry("src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql", "988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f"),
                Map.entry("src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql", "57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad"),
                Map.entry("src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql", "892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817"),
                Map.entry("src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql", "32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e"));
        assertThat(frozen).hasSize(21);
        for (var entry : frozen.entrySet()) {
            byte[] bytes = Files.readAllBytes(Path.of(entry.getKey()));
            assertFrozenBytes(bytes, entry.getValue());
            String lf = new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n");
            for (String variant : List.of(lf, lf.replace("\n", "\r\n"))) {
                assertFrozenBytes(variant.getBytes(StandardCharsets.UTF_8), entry.getValue());
                byte[] changed = ("X" + variant).getBytes(StandardCharsets.UTF_8);
                assertThat(hash(changed)).isNotEqualTo(entry.getValue());
                assertThatThrownBy(() -> assertFrozenBytes(changed, entry.getValue())).isInstanceOf(AssertionError.class);
            }
            byte[] crOnly = lf.replace("\n", "\r").getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> assertFrozenBytes(crOnly, entry.getValue())).isInstanceOf(AssertionError.class);
        }
    }

    private void assertFrozenBytes(byte[] bytes, String expectedLf) throws Exception {
        String lf = new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n");
        String lfHash = hash(lf.getBytes(StandardCharsets.UTF_8));
        assertThat(lfHash).isEqualTo(expectedLf);
        assertThat(hash(bytes)).isIn(lfHash, hash(lf.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8)));
    }

    private String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
