package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.subject_number.customerclaim.*;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.*;
import com.onecall.core.customer_iam.internal.web.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class CustomerSubjectClaimBoundaryTests {
    private static final Path OWNER = Path.of("src/main/java/com/onecall/core/subject_number");
    private static final Path CUSTOMER = Path.of("src/main/java/com/onecall/core/customer_iam/internal");
    private static String source(Path path) throws Exception { return Files.readString(path); }
    @Test
    void namedCommandExportsOnlyFrozenOwnerTypesAndPersonalCaller() throws Exception {
        assertThat(source(OWNER.resolve("customerclaim/package-info.java"))).contains("@org.springframework.modulith.NamedInterface(\"customer-subject-claim-command\")");
        try (var paths = Files.list(OWNER.resolve("customerclaim"))) { assertThat(paths.map(path -> path.getFileName().toString())).containsExactlyInAnyOrder("package-info.java", "CustomerSubjectClaimService.java", "SubjectClaimException.java"); }
        assertThat(Arrays.stream(PersonalClaimCaller.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("accountId");
        assertThat(CustomerSubjectClaimService.class.getDeclaredMethods()).hasSize(1);
        assertThat(Arrays.stream(CustomerSubjectClaimService.class.getDeclaredClasses()).map(Class::getSimpleName)).containsExactlyInAnyOrder("PersonalClaimCaller", "ClaimCommand", "VerificationMethod", "ClaimStatus", "ClaimResult");
    }
    @Test
    void controllerBinderAndProblemSurfaceMatchFrozenOperation() throws Exception {
        var method = CustomerSubjectClaimsController.class.getMethod("createCustomerSubjectClaim", java.security.Principal.class, jakarta.servlet.http.HttpServletRequest.class);
        assertThat(method.getReturnType()).isEqualTo(CustomerSubjectClaimDtos.ClaimResult.class);
        var mapping = method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
        assertThat(mapping.path()).containsExactly("/api/v1/customer/subject-claims"); assertThat(mapping.consumes()).isEmpty(); assertThat(mapping.produces()).containsExactly("application/json");
        assertThat(Arrays.stream(CustomerSubjectClaimDtos.ClaimRequest.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("subjectId", "bindingId", "verificationMethod", "verificationProofId");
        assertThat(Arrays.stream(CustomerSubjectClaimDtos.ClaimResult.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("claimId", "status", "subjectId", "verifiedAt");
        assertThat(VerificationMethod.values()).containsExactly(VerificationMethod.SMS, VerificationMethod.WECHAT_PHONE, VerificationMethod.CALLER_CHALLENGE, VerificationMethod.MANUAL_REVIEW);
        assertThat(Arrays.stream(SubjectClaimException.Code.values()).map(Enum::name)).containsExactly("UNAUTHENTICATED", "FORBIDDEN", "VALIDATION_FAILED", "CLAIM_RISK_BLOCKED", "CLAIM_REPLAY_CONFLICT", "CLAIM_CONTEXT_CONFLICT", "RATE_LIMITED", "SERVICE_UNAVAILABLE", "INTERNAL");
        assertThat(source(CUSTOMER.resolve("web/CustomerSubjectClaimJsonBinder.java"))).contains("STRICT_DUPLICATE_DETECTION", "FAIL_ON_TRAILING_TOKENS", "root.size() != 4");
    }
    @Test
    void customerCompositionHasNoSqlOrClientAuthorityEscape() throws Exception {
        for (String name : List.of("application/CustomerSubjectClaimCommandService.java", "web/CustomerSubjectClaimsController.java", "web/CustomerSubjectClaimJsonBinder.java", "web/CustomerSubjectClaimDtos.java")) {
            String code = source(CUSTOMER.resolve(name));
            assertThat(code).doesNotContain("DSLContext", "DataSource", "SELECT ", "INSERT ", "UPDATE ", "subject_number.internal", "getCustomerMe", "Class.forName", "getSession(", "getDeclaredField", "SessionVerifier");
            assertThat(code.lines().filter(line -> line.startsWith("import com.onecall.core.subject_number."))).allMatch(line -> line.startsWith("import com.onecall.core.subject_number.customerclaim."));
        }
    }
    @Test
    void ownerSqlUsesOnlyApprovedColumnsLocksAndAuditFunction() throws Exception {
        String code = source(OWNER.resolve("internal/claim/JooqSubjectClaimRepository.java"));
        assertThat(code).contains("SET LOCAL ROLE subject_number_role", "SELECT current_user", "ORDER BY id FOR SHARE", "WHERE id=? FOR UPDATE", "customer_iam.customer_account WHERE id=?", "audit.append_fact", "owner_account_id=?,revision=revision+1,updated_at=?", "owner_organization_id IS NULL AND revision=?");
        assertThat(code).doesNotContain("SELECT *", "DELETE ", "TRUNCATE ", "SAVEPOINT", "ON CONFLICT", "outbox", "activation_consent", "evidence.", "sharing.", "SET ROLE ");
        for (var method : com.onecall.core.subject_number.internal.claim.SubjectClaimRepository.class.getDeclaredMethods()) assertThat(method.getParameterTypes()[0]).isEqualTo(org.jooq.DSLContext.class);
    }
    @Test
    void singleAuthoritativeTransactionAndLocalSpisPreserveBoundary() throws Exception {
        String code = source(OWNER.resolve("internal/claim/DefaultCustomerSubjectClaimService.java"));
        assertThat(code.split("transaction.execute", -1)).hasSize(2); assertThat(code).contains("attempt < 3", "isActualTransactionActive()", "subject_number.number:", "subject_number.subject:", "customer_iam.account:");
        assertThat(code).doesNotContain("savepoint", "PreparationChanged", "HttpClient", "java.net", "Thread.sleep", "REQUIRES_NESTED", "preparedState", "Map<String, Payload>");
        assertThat(code.indexOf("risks.stream()")).isLessThan(code.indexOf("var request = repository.findByRequestId"));
        assertThat(code.indexOf("if (!rowIdentity(row, command, caller))")).isLessThan(code.indexOf("stored = stored(row)"));
    }
    @Test
    void openApiMigrationsAndProductionProviderAbsenceRemainFrozen() throws Exception {
        String[] frozen = FROZEN.strip().split("\n"); assertThat(frozen).hasSize(20);
        Set<String> paths = new HashSet<>();
        for (String line : frozen) {
            String[] fields = line.split(" "); paths.add(fields[0]); byte[] bytes = Files.readAllBytes(Path.of("..").resolve(fields[0]));
            assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))).isIn(fields[1], fields[2]);
            byte[] canonical = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).replace("\r\n", "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical))).isEqualTo(fields[2]);
        }
        assertThat(paths).hasSize(20); assertThat(paths.stream().filter(path -> path.contains("/migration/"))).hasSize(17);
        try (var migrations = Files.list(Path.of("src/main/resources/db/migration"))) {
            assertThat(migrations.filter(Files::isRegularFile).map(path -> "backend/" + path.toString()))
                    .containsExactlyInAnyOrderElementsOf(java.util.stream.Stream.concat(paths.stream().filter(path -> path.contains("/migration/")), java.util.stream.Stream.of("backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql")).toList());
        }
        try (var files = Files.walk(OWNER.resolve("internal/claim"))) {
            for (var path : files.filter(Files::isRegularFile).toList()) assertThat(source(path)).doesNotContain("javax.crypto", "KmsClient", "SmsClient", "HttpClient", "@Profile(\"test\")");
        }
    }
    private static final String FROZEN = """
api/openapi/onecall-v1.openapi.yaml 0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68 ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30
docs/14-production-data-model-and-flyway-schema-spec.md 0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31 bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58
docs/15-production-openapi-surface-spec.md db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0 0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02
backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql 96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6 946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045
backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql 4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505 534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5
backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql 4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7 1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1
backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql 8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030 a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc
backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql 6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065 2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310
backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql 7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d 830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475
backend/src/main/resources/db/migration/V007__activation_and_consent.sql bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81 bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659
backend/src/main/resources/db/migration/V008__evidence_media.sql 32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde 2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e
backend/src/main/resources/db/migration/V009__sharing.sql 3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1 ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae
backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559 fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343
backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b
backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6 13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828
backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql 1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e 691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6
backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8 e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a
backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc 1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3
backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c 988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f
backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql 74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3 57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad
""";
}
