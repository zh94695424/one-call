package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.CustomerSubjectDetailQueryService;
import com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader;
import com.onecall.core.evidence.customerquery.CustomerSubjectEvidenceCountReader;
import com.onecall.core.catalog_order.customerquery.CustomerSubjectOrderCountReader;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import tools.jackson.databind.json.JsonMapper;

class CustomerSubjectDetailBoundaryTests {
    private static final Path ROOT = Path.of("src/main/java/com/onecall/core");
    private static final Set<String> DELTA = Set.of(
            "customer_iam_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner",
            "customer_iam_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner",
            "customer_iam_role|schema|evidence||USAGE|evidence_owner",
            "customer_iam_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner",
            "customer_iam_role|schema|catalog_order||USAGE|catalog_order_owner",
            "customer_iam_role|column|catalog_order.customer_order|subject_id|SELECT|catalog_order_owner",
            "customer_iam_role|column|catalog_order.customer_order|account_id|SELECT|catalog_order_owner",
            "customer_iam_role|column|catalog_order.customer_order|owner_organization_id|SELECT|catalog_order_owner");

    @Test
    void subjectEvidenceAndOrderExposeOnlyMinimalNamedQueryInterfaces() throws Exception {
        var modules = ApplicationModules.of(OneCallApplication.class); modules.verify();
        for (Class<?> seam : List.of(CustomerSubjectAccessReader.class, CustomerSubjectEvidenceCountReader.class, CustomerSubjectOrderCountReader.class)) {
            assertThat(seam.isInterface()).isTrue();
            for (var method : seam.getDeclaredMethods()) {
                if (seam == CustomerSubjectAccessReader.class && method.getName().equals("hasOpenRisk")
                        && Arrays.equals(method.getParameterTypes(), new Class<?>[] {Set.class})) {
                    assertThat(method.getGenericParameterTypes()[0].getTypeName()).isEqualTo("java.util.Set<java.util.UUID>");
                } else {
                    assertThat(method.getParameterTypes()).allMatch(type -> type == UUID.class);
                }
                assertThat(method.getGenericReturnType().getTypeName()).doesNotContain("org.jooq", "org.springframework", ".internal.", "java.sql");
            }
        }
        assertThat(CustomerSubjectAccessReader.class.getDeclaredMethods()).hasSize(4);
        assertThat(CustomerSubjectAccessReader.class.getMethod("findOwnedSubjectIds", UUID.class, UUID.class).getGenericReturnType().getTypeName())
                .isEqualTo("java.util.Set<java.util.UUID>");
        assertThat(CustomerSubjectAccessReader.class.getMethod("hasOpenRisk", Set.class).getReturnType()).isEqualTo(boolean.class);
        assertThat(CustomerSubjectAccessReader.class.getMethod("findOwnedSubject", UUID.class, UUID.class, UUID.class).getGenericReturnType().getTypeName()).isEqualTo("java.util.Optional<java.util.UUID>");
        assertThat(CustomerSubjectAccessReader.class.getMethod("hasOpenRisk", UUID.class).getReturnType()).isEqualTo(boolean.class);
        assertThat(CustomerSubjectEvidenceCountReader.class.getDeclaredMethods()).hasSize(1);
        assertThat(CustomerSubjectEvidenceCountReader.class.getMethod("countForSubject", UUID.class).getReturnType()).isEqualTo(long.class);
        assertThat(CustomerSubjectOrderCountReader.class.getDeclaredMethods()).hasSize(1);
        assertThat(CustomerSubjectOrderCountReader.class.getMethod("countForWorkspace", UUID.class, UUID.class, UUID.class).getReturnType()).isEqualTo(long.class);
        for (var entry : Map.of("subject_number", "customer-subject-query", "evidence", "customer-subject-count-query", "catalog_order", "customer-subject-count-query").entrySet())
            assertThat(modules.getModuleByName(entry.getKey()).orElseThrow().getNamedInterfaces().getByName(entry.getValue())).isPresent();
        assertThat(modules.getModuleByName("customer_iam").orElseThrow().getAllowedDependencies(modules)).hasSize(10);
    }

    @Test
    void customerCompositionContainsNoForeignSqlOrInternalImports() throws Exception {
        String service = Files.readString(ROOT.resolve("customer_iam/internal/application/CustomerSubjectDetailQueryService.java"));
        assertThat(service).doesNotContain("FROM ", "JOIN ", "FOR UPDATE", "FOR SHARE", "INSERT ", "UPDATE ", "DELETE ", "cursor", "customer_iam_role;");
        assertThat(service.lines().filter(line -> line.startsWith("import com.onecall.core.") && !line.contains(".customer_iam.")))
                .noneMatch(line -> line.contains(".internal."));
        assertThat(Arrays.stream(CustomerSubjectDetailQueryService.Detail.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("subjectId", "activationStatus", "evidenceCount", "orderCount");
        String controller = Files.readString(ROOT.resolve("customer_iam/internal/web/CustomerSubjectsController.java"));
        assertThat(controller).contains("/api/v1/customer/subjects/{subjectId}", "problems.notFound(response, CustomerBoundaryFilter.trace(request))")
                .doesNotContain("getParameter", "getHeader", "getInputStream", "getReader", "RequestBody", "RequestParam", "Cursor", "DataSource", "DSLContext");
        for (String path : List.of("subject_number/internal/customerquery/JooqCustomerSubjectAccessReader.java",
                "evidence/internal/customerquery/JooqCustomerSubjectEvidenceCountReader.java", "catalog_order/internal/customerquery/JooqCustomerSubjectOrderCountReader.java")) {
            String owner = Files.readString(ROOT.resolve(path));
            assertThat(owner).doesNotContain("customer_iam.internal", "TransactionTemplate", "SET LOCAL ROLE", "SELECT *", "FOR UPDATE", "FOR SHARE", "INSERT ", "UPDATE ", "DELETE ");
        }
    }

    @Test
    void v016AddsOnlyTheEightRequiredPrivilegeTuplesAndPa25Uses269AndSixteen() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql"));
        assertThat(migration).doesNotContain("REVOKE ", "CREATE TABLE", "ALTER TABLE", "CREATE ROLE", "ALTER ROLE", "DELETE FROM", "INSERT INTO", "UPDATE ");
        Set<String> old = tuples(migration, "old"); Set<String> complete = tuples(migration, "final");
        assertThat(old).hasSize(42); assertThat(complete).hasSize(50);
        var added = new HashSet<>(complete); added.removeAll(old); assertThat(added).isEqualTo(DELTA);
        assertThat(complete).containsAll(old);
        assertThat(migration).contains("namespace.nspname = 'evidence'", "NOT IN (15, 16)", "installed_rank <= 15", "V016_RECONCILIATION_REJECTED");
        String pa25 = Files.readString(Path.of("ops/postgresql/pa25-runtime-identities.sql"));
        assertThat(pa25).contains("expected_resolved) <> 288", "history) <> 17", "V017__customer_evidence_read_privileges.sql");
    }
    private Set<String> tuples(String source, String tag) {
        String marker = "$" + tag + "$";
        String json = source.substring(source.indexOf(marker) + marker.length(), source.indexOf(marker, source.indexOf(marker) + marker.length()));
        Set<String> result = new HashSet<>();
        for (var row : JsonMapper.builder().build().readTree(json)) {
            assertThat(row.size()).isEqualTo(6);
            List<String> fields = new ArrayList<>(); row.forEach(value -> fields.add(value.asString()));
            assertThat(result.add(String.join("|", fields))).isTrue();
        }
        return result;
    }

    @Test
    void openApiAndV001ThroughV015RemainFrozenAcrossNewlineRepresentations() throws Exception {
        Map<String, String> frozen = Map.ofEntries(
                Map.entry("../api/openapi/onecall-v1.openapi.yaml", "ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30"),
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
                Map.entry("src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql", "1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3")
        );
        for (var entry : frozen.entrySet()) {
            byte[] bytes = Files.readAllBytes(Path.of(entry.getKey()));
            String lf = new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n");
            for (String variant : List.of(lf, lf.replace("\n", "\r\n"), lf.replace("\n", "\r"))) {
                assertThat(hash(variant)).as(entry.getKey()).isEqualTo(entry.getValue());
                assertThat(hash("X" + variant)).isNotEqualTo(entry.getValue());
            }
        }
    }
    private String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                value.replace("\r\n", "\n").replace("\r", "\n").getBytes(StandardCharsets.UTF_8)));
    }
}
