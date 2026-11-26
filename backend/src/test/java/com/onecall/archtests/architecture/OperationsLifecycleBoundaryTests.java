package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.*;
import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock;
import com.onecall.core.activation_consent.internal.operations.OperationsLifecycleSemantics;
import com.onecall.core.activation_consent.internal.command.ActivationSemantics;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class OperationsLifecycleBoundaryTests {
    static final Path ROOT = Path.of("src/main/java/com/onecall/core");
    static String source(String file) throws Exception { return Files.readString(ROOT.resolve(file)); }

    @Test
    void operationsPortsAndNamedInterfacesAreExactAndAcyclic() throws Exception {
        assertThat(Arrays.stream(OperationsSubjectLifecycleService.class.getDeclaredMethods()).map(java.lang.reflect.Method::getName)).containsExactlyInAnyOrder("suspend", "resume");
        assertThat(EmployeeAdministratorLock.class.getDeclaredMethods()).hasSize(1);
        assertThat(Arrays.stream(Command.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("subjectId", "actionEventId", "reasonCode", "expectedActivationRevision");
        assertThat(EmployeeAdministratorLock.Failure.Kind.values()).extracting(Enum::name).containsExactly("UNAVAILABLE", "INTEGRITY");
        assertThat(source("operations_iam/subjectlifecycle/package-info.java")).contains("subject-lifecycle-command"); assertThat(source("operations_iam/employeelock/package-info.java")).contains("employee-administrator-lock");
        for (var kind : EmployeeAdministratorLock.Failure.Kind.values()) { var failure = new EmployeeAdministratorLock.Failure(kind); assertThat(failure.kind()).isEqualTo(kind); assertThat(failure.getCause()).isNull(); assertThat(failure.toString()).isEqualTo("Failure[REDACTED]"); }
        String service = source("activation_consent/internal/operations/DefaultOperationsSubjectLifecycleService.java"); assertThat(service).contains("catch (EmployeeAdministratorLock.Failure unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }", "return new Failure(Code.INTERNAL)");
    }
    @Test
    void activationImportsOnlyPublishedOperationsInterfaces() throws Exception {
        try (var paths = Files.walk(ROOT.resolve("activation_consent/internal/operations"))) { for (var file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
            String code = Files.readString(file); assertThat(code).doesNotContain("operations_iam.internal", "entitlement_metering");
            assertThat(code.lines().filter(line -> line.startsWith("import com.onecall.core.operations_iam."))).allMatch(line -> line.startsWith("import com.onecall.core.operations_iam.subjectlifecycle.") || line.startsWith("import com.onecall.core.operations_iam.employeelock."));
        } }
        assertThat(source("operations_iam/package-info.java")).doesNotContain("activation_consent");
    }
    @Test
    void employeeSessionIsIndependentAndHasNoProductionVerifierImplementation() throws Exception {
        try (var paths = Files.walk(ROOT.resolve("operations_iam"))) { for (var file : paths.filter(path -> path.toString().endsWith(".java")).toList()) assertThat(Files.readString(file)).doesNotContain("implements EmployeeSessionVerifier", "CustomerSessionVerifier", "JwtDecoder", "RedisTemplate"); }
        assertThat(Files.readString(Path.of("src/test/java/com/onecall/archtests/support/SyntheticEmployeeSessions.java"))).contains("implements EmployeeSessionVerifier");
        String authenticator = source("operations_iam/internal/security/EmployeeBearerAuthenticator.java"); assertThat(authenticator).contains("onecall:employee-session:v1", "stream().limit(2)", "!AUDIENCE.equals(session.audience())");
    }
    @Test
    void employeeOwnerSqlIsLockOnlyAndUsesMandatoryLiteralRoles() throws Exception {
        String code = source("operations_iam/internal/employeelock/DefaultEmployeeAdministratorLock.java"); assertThat(code).contains("PROPAGATION_MANDATORY", "hasResource(manager.getDataSource())", "SET LOCAL ROLE operations_iam_role", "SET LOCAL ROLE activation_consent_role", "finally", "status.setRollbackOnly()", "operations_iam.admin_mutation", "WHERE id=? FOR UPDATE").doesNotContain("INSERT INTO", "UPDATE operations_iam", "DELETE FROM", "HttpClient", "REQUIRES_NEW", "commit(");
    }
    @Test
    void lifecycleSqlHasNoForeignBaseWritesOrTrialCalls() throws Exception {
        String code = source("activation_consent/internal/operations/JooqOperationsLifecycleRepository.java"); assertThat(code).contains("source_action_domain='OPERATIONS' AND source_action_event_id=?", "WHERE id=? AND subject_id=? AND status=? AND revision=?", "SELECT id FROM subject_number.business_subject WHERE id=?").doesNotContain("issue_first_trial", "consent_record SET", "entitlement_metering.", "operations_iam.employee_account", "SELECT *", "ON CONFLICT", "DELETE FROM", "UPDATE subject_number");
    }
    @Test
    void routesBinderProblemsAndCustomerPassThroughStayExact() throws Exception {
        String controller = source("operations_iam/internal/web/OperationsSubjectsController.java"); assertThat(controller.split("@PostMapping", -1)).hasSize(3); assertThat(controller).doesNotContain("@GetMapping", "getMessage()", "getCause()", "employeeId\"");
        String filter = source("operations_iam/internal/web/OperationsBoundaryFilter.java"); assertThat(filter).contains("[^/;%]+/(suspensions|resumptions)").doesNotContain("CustomerBearer");
        String binder = source("operations_iam/internal/web/OperationsSubjectLifecycleJsonBinder.java"); assertThat(binder).contains("STRICT_DUPLICATE_DETECTION", "FAIL_ON_TRAILING_TOKENS", "isIntegralNumber()", "8192");
        String customer = source("customer_iam/internal/web/CustomerBoundaryFilter.java"); assertThat(customer.indexOf("operationsPath.matches")).isLessThan(customer.indexOf("UUID traceId = UUID.randomUUID()"));
    }
    @Test
    void frozenOpenApiMigrationsAndPolicyBytesAreUnchanged() throws Exception {
        Map<String, String[]> hashes = Map.ofEntries(
            Map.entry("../api/openapi/onecall-v1.openapi.yaml", new String[] {"0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68", "ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30"}),
            Map.entry("../docs/12-technology-stack-and-system-architecture.md", new String[] {"9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db", "772ccaee7b188f68fcc694f3e8c00983ad9de028547f8e0b0c8638a8d8cae435"}),
            Map.entry("../docs/14-production-data-model-and-flyway-schema-spec.md", new String[] {"0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31", "bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58"}),
            Map.entry("../docs/15-production-openapi-surface-spec.md", new String[] {"db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0", "0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02"}),
            Map.entry("../docs/17-production-security-design-spec.md", new String[] {"7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e", "8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f"}),
            Map.entry("../backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql", new String[] {"96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6", "946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045"}),
            Map.entry("../backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql", new String[] {"4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505", "534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5"}),
            Map.entry("../backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql", new String[] {"4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7", "1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1"}),
            Map.entry("../backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql", new String[] {"8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030", "a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc"}),
            Map.entry("../backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql", new String[] {"6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065", "2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310"}),
            Map.entry("../backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql", new String[] {"7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d", "830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475"}),
            Map.entry("../backend/src/main/resources/db/migration/V007__activation_and_consent.sql", new String[] {"bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81", "bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659"}),
            Map.entry("../backend/src/main/resources/db/migration/V008__evidence_media.sql", new String[] {"32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde", "2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e"}),
            Map.entry("../backend/src/main/resources/db/migration/V009__sharing.sql", new String[] {"3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1", "ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae"}),
            Map.entry("../backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql", new String[] {"d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559", "fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343"}),
            Map.entry("../backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql", new String[] {"ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e", "f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b"}),
            Map.entry("../backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql", new String[] {"ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6", "13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828"}),
            Map.entry("../backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql", new String[] {"1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e", "691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6"}),
            Map.entry("../backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql", new String[] {"bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8", "e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a"}),
            Map.entry("../backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql", new String[] {"c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc", "1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3"}),
            Map.entry("../backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql", new String[] {"a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c", "988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f"}),
            Map.entry("../backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql", new String[] {"74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3", "57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad"}));
        for (var entry : hashes.entrySet()) { byte[] bytes = historicalBytes(entry.getKey(), Files.readAllBytes(Path.of(entry.getKey()))); String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); assertThat(actual).as(entry.getKey()).isIn((Object[]) entry.getValue()); }
        try (var paths = Files.list(Path.of("src/main/resources/db/migration"))) { assertThat(paths.filter(path -> path.toString().endsWith(".sql")).count()).isEqualTo(20); }
    }
    @Test
    void auditOutboxFingerprintAndResponseFieldSetsAreExact() throws Exception {
        assertThat(Arrays.stream(ActivationTransition.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("transitionId", "statusEventId", "fromStatus", "toStatus", "fromRevision", "toRevision", "sourceChannel", "sourceActionDomain", "reasonCode", "occurredAt");
        var fields = OperationsLifecycleSemantics.fingerprintFields(new Command(UUID.randomUUID(), "action", "reason", java.math.BigInteger.ONE), new Caller(UUID.randomUUID(), "onecall:employee-session:v1"), "ACTIVE", "status"); assertThat(fields).hasSize(11).doesNotContainKeys("expectedActivationRevision", "traceId", "occurredAt");
        String sql = source("activation_consent/internal/operations/JooqOperationsLifecycleRepository.java"); assertThat(sql).contains("\"status\", row.fromStatus(), \"revision\", row.fromRevision(), \"reasonCode\", row.reason()", "row.statusEvent(), UUID.randomUUID().toString()");
        String outbox = sql.substring(sql.indexOf("String payload ="), sql.indexOf("var result = context.fetchOne(\"SELECT platform")); assertThat(outbox).doesNotContain("employeeId", "reason()", "password", "bearer"); assertThat(outbox.split("\"[a-zA-Z]+\",", -1).length - 1).isEqualTo(13);
    }
    private static byte[] historicalBytes(String name, byte[] bytes) throws Exception {
        if (!name.endsWith("docs/17-production-security-design-spec.md")) return bytes;
        String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).replace("\r\n", "\n");
        String lf = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThat(lf).isEqualTo("4e7df589380f70ba7e9e1a5b7e9c1542ca6ed89313a775dacfc91e109852ceb6");
        String marker = "\n<!-- PA36 HISTORICAL BODY -->\n\n";
        int offset = text.indexOf(marker); assertThat(offset).isGreaterThan(0); assertThat(text.indexOf(marker, offset + 1)).isEqualTo(-1);
        return text.substring(offset + marker.length()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

}
