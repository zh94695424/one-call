package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader;
import com.onecall.core.activation_consent.customerquery.CustomerActivationStatusReader;
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
        assertThat(modules.getModuleByName("customer_iam").orElseThrow().getAllowedDependencies(modules)).hasSize(6);
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
        assertThat(pa25).contains("V015__customer_business_number_read_privileges.sql", "expected_resolved) <> 261", "history) <> 15");
        Map<String, String> frozen = Map.ofEntries(
                Map.entry("../api/openapi/onecall-v1.openapi.yaml", "0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68"),
                Map.entry("src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql", "96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6"),
                Map.entry("src/main/resources/db/migration/V002__identity_organization_and_employee.sql", "4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505"),
                Map.entry("src/main/resources/db/migration/V003__subject_number_and_claim.sql", "4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7"),
                Map.entry("src/main/resources/db/migration/V004__catalog_order_and_payment.sql", "8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030"),
                Map.entry("src/main/resources/db/migration/V005__ivr_task_session_call.sql", "6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065"),
                Map.entry("src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql", "7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d"),
                Map.entry("src/main/resources/db/migration/V007__activation_and_consent.sql", "bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81"),
                Map.entry("src/main/resources/db/migration/V008__evidence_media.sql", "32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde"),
                Map.entry("src/main/resources/db/migration/V009__sharing.sql", "3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1"),
                Map.entry("src/main/resources/db/migration/V010__sharing_role_least_privilege.sql", "d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559"),
                Map.entry("src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql", "ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e"),
                Map.entry("src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql", "ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6"),
                Map.entry("src/main/resources/db/migration/V013__evidence_media_reader_role.sql", "1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e"),
                Map.entry("src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql", "bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8")
        );
        for (var entry : frozen.entrySet()) {
            assertThat(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(entry.getKey())))))
                    .as(entry.getKey()).isEqualTo(entry.getValue());
        }
    }
}
