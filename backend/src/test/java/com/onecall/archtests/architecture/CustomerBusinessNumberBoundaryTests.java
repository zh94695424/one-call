package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader;
import com.onecall.core.activation_consent.customerquery.CustomerActivationStatusReader;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class CustomerBusinessNumberBoundaryTests {
    @Test
    void subjectAndActivationExposeOnlyTheTwoOwnedNamedQueryInterfaces() {
        var modules = ApplicationModules.of(OneCallApplication.class);
        for (var entry : Map.of("subject_number", "customer-business-number-query", "activation_consent", "customer-activation-query").entrySet()) {
            var interfaces = modules.getModuleByName(entry.getKey()).orElseThrow().getNamedInterfaces();
            assertThat(interfaces.getByName(entry.getValue())).isPresent();
            assertThat(interfaces.stream().flatMap(named -> named.asJavaClasses()).map(type -> type.getName()))
                    .noneMatch(name -> name.contains(".internal.customerquery."));
        }
        assertThat(CustomerBusinessNumberReader.class.getDeclaredMethods()).extracting(java.lang.reflect.Method::getName).containsExactly("fetch");
        assertThat(CustomerActivationStatusReader.class.getDeclaredMethods()).extracting(java.lang.reflect.Method::getName).containsExactly("findStatuses");
        for (Class<?> type : List.of(CustomerBusinessNumberReader.class, CustomerActivationStatusReader.class)) {
            for (var method : type.getDeclaredMethods()) assertThat(method.toGenericString()).doesNotContain("org.jooq", "java.sql", "customer_iam.internal");
        }
        assertThat(Arrays.stream(CustomerBusinessNumberReader.Row.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("bindingId", "subjectId", "numberMasked", "numberType", "bindingStatus", "realNameStatus", "subjectCreatedAt");
        modules.verify();
    }

    @Test
    void customerImportsOnlyPublicQuerySeamsAndNoForeignInternalImplementation() throws Exception {
        try (var files = Files.walk(Path.of("src/main/java/com/onecall/core/customer_iam"))) {
            for (var file : files.filter(Files::isRegularFile).toList()) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("subject_number.internal", "activation_consent.internal", "subject_number_owner", "activation_consent_owner");
            }
        }
        var modules = ApplicationModules.of(OneCallApplication.class);
        assertThat(modules.getModuleByName("customer_iam").orElseThrow().getAllowedDependencies(modules)).hasSize(12);
        modules.verify();
    }

    @Test
    void controllerSqlAndProjectionStayInsideTheirSemanticOwners() throws Exception {
        Path root = Path.of("src/main/java/com/onecall/core");
        String controller = Files.readString(root.resolve("customer_iam/internal/web/CustomerBusinessNumbersController.java"));
        assertThat(controller).contains("ObjectProvider<CustomerBusinessNumberQueryService>", "ObjectProvider<CustomerBusinessNumberCursorCodec>")
                .doesNotContain("DSLContext", "DataSource", "Repository", "getAttribute(", "getSession(");
        for (String path : List.of("customer_iam/internal/application/CustomerBusinessNumberQueryService.java", "customer_iam/internal/persistence/JooqCustomerBusinessNumberAccessRepository.java")) {
            assertThat(Files.readString(root.resolve(path))).doesNotContain("FROM subject_number.", "FROM activation_consent.", "INSERT ", "UPDATE ", "DELETE ");
        }
        String subject = Files.readString(root.resolve("subject_number/internal/customerquery/JooqCustomerBusinessNumberReader.java"));
        assertThat(subject).contains("s.owner_account_id = ?", "s.owner_organization_id = ?", "s.created_at DESC, b.id ASC LIMIT ?", "b.id > ?")
                .doesNotContain("number_ciphertext", "number_hmac", "OFFSET", "SELECT *", "FOR UPDATE", "FOR SHARE");
        String activation = Files.readString(root.resolve("activation_consent/internal/customerquery/JooqCustomerActivationStatusReader.java"));
        assertThat(activation).contains("SELECT subject_id, status FROM activation_consent.service_activation", "subjectIds.isEmpty()")
                .doesNotContain("customer_iam.internal", "TransactionTemplate", "SET LOCAL ROLE");
    }

    @Test
    void v015AndPa25DeltasAreReadOnlyAndFrozenInputsRemainByteIdentical() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql"));
        assertThat(migration).doesNotContain("REVOKE ", "CREATE TABLE", "ALTER TABLE", "CREATE ROLE", "ALTER ROLE", "DELETE FROM", "INSERT INTO", "UPDATE ");
        assertThat(migration.lines().filter(line -> line.contains("EXECUTE format('GRANT")))
                .hasSize(2).noneMatch(line -> line.contains("WITH GRANT OPTION"));
        assertThat(migration).contains("V015_AUTHORITY_REJECTED", "V015_ACL_REJECTED", "SET LOCAL ROLE %I", "GRANT SELECT (%I)");
        String pa25 = Files.readString(Path.of("ops/postgresql/pa25-runtime-identities.sql"));
        assertThat(pa25).contains("V015__customer_business_number_read_privileges.sql", "expected_resolved) <> 410", "history) <> 18");
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
                Map.entry("src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql", "e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a")
        );
        for (var entry : frozen.entrySet()) {
            byte[] content = Files.readAllBytes(Path.of(entry.getKey()));
            assertThat(canonicalLfSha256(content))
                    .as(entry.getKey()).isEqualTo(entry.getValue());
            byte[] lf = canonicalLf(content);
            var crlf = new ByteArrayOutputStream();
            byte[] cr = lf.clone();
            for (int index = 0; index < lf.length; index++) {
                if (lf[index] == '\n') {
                    crlf.write('\r');
                    cr[index] = '\r';
                }
                crlf.write(lf[index]);
            }
            for (byte[] variant : List.of(lf, crlf.toByteArray(), cr)) {
                assertThat(canonicalLfSha256(variant)).as("%s newline variant", entry.getKey()).isEqualTo(entry.getValue());
                byte[] mutated = variant.clone();
                mutated[0] ^= 1;
                assertThat(canonicalLfSha256(mutated)).as("%s content mutation", entry.getKey()).isNotEqualTo(entry.getValue());
            }
        }
    }

    private static byte[] canonicalLf(byte[] content) {
        var normalized = new ByteArrayOutputStream(content.length);
        for (int index = 0; index < content.length; index++) {
            if (content[index] == '\r') {
                normalized.write('\n');
                if (index + 1 < content.length && content[index + 1] == '\n') index++;
            } else {
                normalized.write(content[index]);
            }
        }
        return normalized.toByteArray();
    }

    private static String canonicalLfSha256(byte[] content) throws java.security.NoSuchAlgorithmException {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(canonicalLf(content)));
    }
}
