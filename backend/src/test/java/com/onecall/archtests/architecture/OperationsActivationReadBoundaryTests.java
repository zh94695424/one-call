package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;
import com.onecall.core.OneCallApplication;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadService.*;
import com.onecall.core.operations_iam.internal.web.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class OperationsActivationReadBoundaryTests {
    private static final Path ROOT = Path.of("src/main/java/com/onecall/core");
    private static final List<String> APPLICATION = List.of("operations_iam/subjectread/OperationsActivationReadService.java", "operations_iam/subjectread/OperationsActivationReadPorts.java", "operations_iam/internal/activationread/OperationsActivationReadRequest.java", "operations_iam/internal/activationread/OperationsActivationCursorCodec.java", "operations_iam/internal/activationread/OperationsActivationReadConfiguration.java", "operations_iam/internal/activationread/DefaultOperationsActivationReadService.java", "operations_iam/internal/web/OperationsActivationReadController.java", "activation_consent/internal/operationsread/JooqOperationsActivationHistoryReader.java");
    private String source(String path) throws Exception { return Files.readString(ROOT.resolve(path)); }
    @Test
    void publishesOnlyExactActivationReadServicePortAndRecords() {
        assertThat(Arrays.stream(OperationsActivationReadService.class.getDeclaredMethods()).filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers())).map(java.lang.reflect.Method::getName)).containsExactly("list");
        assertThat(Arrays.stream(OperationsActivationReadPorts.class.getDeclaredClasses()).filter(Class::isInterface)).containsExactly(ActivationHistoryReader.class);
        var expected = List.of("transitionId", "subjectId", "statusEventId", "fromStatus", "toStatus", "fromRevision", "toRevision", "sourceChannel", "sourceActionDomain", "reasonCode", "occurredAt");
        assertThat(Arrays.stream(TransitionFacts.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactlyElementsOf(expected); assertThat(Arrays.stream(ActivationSummary.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactlyElementsOf(expected);
        assertThat(TransitionFacts.class.getRecordComponents()[5].getType()).isEqualTo(Integer.class); assertThat(TransitionFacts.class.getRecordComponents()[6].getType()).isEqualTo(Integer.class); assertThat(ActivationSummary.class.getRecordComponents()[5].getType()).isEqualTo(int.class); assertThat(ActivationSummary.class.getRecordComponents()[6].getType()).isEqualTo(int.class);
        assertThat(Arrays.stream(ActivationPage.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("items", "nextCursor", "hasMore");
        assertThat(Arrays.stream(ActivationPage.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getType)).containsExactly(List.class, String.class, boolean.class);
        assertThat(ActivationPage.class.getRecordComponents()[0].getGenericType().getTypeName()).isEqualTo("java.util.List<" + ActivationSummary.class.getName() + ">");
        assertThat(Arrays.stream(Anchor.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("occurredAt", "transitionId");
        assertThat(Arrays.stream(Anchor.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getType)).containsExactly(java.time.Instant.class, UUID.class);
    }
    @Test
    void operationsRouteAndControllerFileMapsRemainExact() throws Exception {
        var routes = new HashSet<String>();
        for (Class<?> controller : List.of(OperationsSubjectsController.class, OperationsSubjectReadController.class, OperationsActivationReadController.class)) for (var method : controller.getDeclaredMethods()) {
            var get = method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class); var post = method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
            if (get != null) for (String path : get.path()) routes.add("GET " + path); if (post != null) for (String path : post.path()) routes.add("POST " + path);
        }
        assertThat(routes).containsExactlyInAnyOrder("GET /api/v1/operations/activations", "GET /api/v1/operations/subjects", "GET /api/v1/operations/subjects/{subjectId}", "POST /api/v1/operations/subjects/{subjectId}/suspensions", "POST /api/v1/operations/subjects/{subjectId}/resumptions");
        try (var paths = Files.list(ROOT.resolve("operations_iam/internal/activationread"))) { assertThat(paths.map(path -> path.getFileName().toString())).hasSize(4).containsExactlyInAnyOrder("OperationsActivationReadRequest.java", "OperationsActivationCursorCodec.java", "OperationsActivationReadConfiguration.java", "DefaultOperationsActivationReadService.java"); }
        assertThat(APPLICATION.stream().filter(path -> Files.isRegularFile(ROOT.resolve(path)))).hasSize(8);
    }
    @Test
    void ownerReaderSelectsOnlyElevenApprovedTransitionColumns() throws Exception {
        String reader = source(APPLICATION.getLast());
        var projections = java.util.regex.Pattern.compile("(?s)\\bSELECT\\s+(.*?)\\s+FROM\\b").matcher(reader).results().map(match -> match.group(1)).toList();
        assertThat(projections).hasSize(2);
        for (String projection : projections) assertThat(Arrays.stream(projection.split(",", -1)).map(String::strip)).containsExactly("id", "subject_id", "status_event_id", "from_status", "to_status", "from_revision", "to_revision", "source_channel", "source_action_domain", "reason_code", "occurred_at");
        assertThat(reader).contains("WHERE (occurred_at, id) < (CAST(? AS timestamptz), CAST(? AS uuid))", "ORDER BY occurred_at DESC, id DESC", "LIMIT ?", "context.scoped(Role.ACTIVATION");
        assertThat(java.util.regex.Pattern.compile("FROM\\s+(\\w+\\.\\w+)").matcher(reader).results().map(match -> match.group(1))).containsExactly("activation_consent.service_activation_transition", "activation_consent.service_activation_transition");
        assertThat(reader).doesNotContain("SELECT *", "activation_id", "consent_record_id", "actor_employee_id", "request_fingerprint", "new ActivationSummary");
    }
    @Test
    void applicationAndControllerHaveNoForeignSqlOrCommandCalls() throws Exception {
        for (String path : APPLICATION.subList(0, 7)) assertThat(source(path)).doesNotContain("activation_consent.", "subject_number.", "catalog_order.", "evidence.", "entitlement_metering.", "subjectLifecycleService", "CommandService");
    }
    @Test
    void readPathHasNoWritesAuditOutboxLocksOrCursorStorage() throws Exception {
        for (String path : APPLICATION) assertThat(source(path)).doesNotContain("INSERT ", "UPDATE ", "DELETE ", "FOR UPDATE", "FOR SHARE", "audit_fact", "outbox_event", "pg_advisory", "CREATE TABLE", "CREATE ROLE", "GRANT ");
        assertThat(source("operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java")).contains("setReadOnly(true)", "ISOLATION_REPEATABLE_READ", "PROPAGATION_REQUIRES_NEW");
    }
    @Test
    void existingNamedInterfaceAndModuleEdgeMultisetAreUnchanged() throws Exception {
        assertThat(source("operations_iam/subjectread/package-info.java")).contains("operations-subject-read"); assertThat(source("activation_consent/package-info.java")).contains("operations_iam::operations-subject-read");
        assertThat(source("operations_iam/package-info.java")).doesNotContain("activation_consent"); ApplicationModules.of(OneCallApplication.class).verify(); new ModuleBoundaryTests().assertsExactAllowedDependencyMapFromPlan62(); new ModuleBoundaryTests().discoveredProductionEdgesStayInsideTheConfiguredMap();
    }
    @Test
    void frozenInputsAndSeventeenMigrationInventoryRemainByteIdentical() throws Exception {
        var plan = Path.of("../docs/gateflow/pa34-operations-activation-history-read-foundation-plan.md");
        assertThat(plan).isRegularFile();
        var entries = java.util.regex.Pattern.compile("(?m)^([0-9a-f]{64})  (.+)$").matcher(Files.readString(plan)).results().toList(); assertThat(entries).hasSize(21);
        var hashes = Map.ofEntries(
            Map.entry("api/openapi/onecall-v1.openapi.yaml", new String[] {"0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68", "ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30"}),
            Map.entry("docs/14-production-data-model-and-flyway-schema-spec.md", new String[] {"0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31", "bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58"}),
            Map.entry("docs/15-production-openapi-surface-spec.md", new String[] {"db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0", "0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02"}),
            Map.entry("docs/17-production-security-design-spec.md", new String[] {"7d1de5bb6ec83d09364dad37c030e59e2084bd149fe3b90083c6337ff048446e", "8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f"}),
            Map.entry("backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql", new String[] {"96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6", "946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045"}),
            Map.entry("backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql", new String[] {"4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505", "534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5"}),
            Map.entry("backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql", new String[] {"4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7", "1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1"}),
            Map.entry("backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql", new String[] {"8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030", "a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc"}),
            Map.entry("backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql", new String[] {"6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065", "2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310"}),
            Map.entry("backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql", new String[] {"7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d", "830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475"}),
            Map.entry("backend/src/main/resources/db/migration/V007__activation_and_consent.sql", new String[] {"bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81", "bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659"}),
            Map.entry("backend/src/main/resources/db/migration/V008__evidence_media.sql", new String[] {"32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde", "2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e"}),
            Map.entry("backend/src/main/resources/db/migration/V009__sharing.sql", new String[] {"3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1", "ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae"}),
            Map.entry("backend/src/main/resources/db/migration/V010__sharing_role_least_privilege.sql", new String[] {"d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559", "fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343"}),
            Map.entry("backend/src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql", new String[] {"ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e", "f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b"}),
            Map.entry("backend/src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql", new String[] {"ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6", "13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828"}),
            Map.entry("backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql", new String[] {"1143f4a5a87b6727edc1878cb86b3eadb034d485add81e3a33558150f0a5db8e", "691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6"}),
            Map.entry("backend/src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql", new String[] {"bc9f9240f25820a78510aa6512009053bd066689abd13fdcd9fe101de941aea8", "e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a"}),
            Map.entry("backend/src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql", new String[] {"c069ea5393138d208c885b3fff852882412d3e9fd418ad0626ffff90717544fc", "1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3"}),
            Map.entry("backend/src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql", new String[] {"a9913a9fad57fdba5a07b8be20cee5e4c81c4d113120d1c19851c276145eea5c", "988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f"}),
            Map.entry("backend/src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql", new String[] {"74f0b860c6da0d2ebc4d03b9baa94010f2ece5c1e4a1a4512a34ffe64044dcc3", "57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad"}));
        assertThat(hashes).hasSize(21);
        assertThat(entries.stream().map(entry -> entry.group(2))).containsExactlyInAnyOrderElementsOf(hashes.keySet());
        for (var entry : entries) {
            assertThat(entry.group(1)).isEqualTo(hashes.get(entry.group(2))[0]);
            byte[] bytes = historicalBytes(entry.group(2), Files.readAllBytes(Path.of("..").resolve(entry.group(2))));
            assertThat(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))).as(entry.group(2)).isIn((Object[]) hashes.get(entry.group(2)));
        }
        try (var files = Files.list(Path.of("src/main/resources/db/migration"))) { assertThat(files.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet())).hasSize(19).containsExactlyInAnyOrderElementsOf(java.util.stream.Stream.concat(entries.stream().map(entry -> entry.group(2)).filter(path -> path.contains("/migration/")).map(path -> Path.of(path).getFileName().toString()), java.util.stream.Stream.of("V018__evidence_role_ivr_association_read_privileges.sql", "V019__customer_catalog_offer_read_privileges.sql")).toList()); }
    }
    @Test
    void employeeVerifierAndFilterLockstepStayIndependentAndExact() throws Exception {
        for (String filter : List.of("operations_iam/internal/web/OperationsBoundaryFilter.java", "customer_iam/internal/web/CustomerBoundaryFilter.java")) assertThat(source(filter)).contains("\"/api/v1/operations/activations\".equals(", "\"GET\".equals(", ".equals(request.getRequestURI())").doesNotContain("startsWith(\"/api/v1/operations/activations");
        try (var paths = Files.walk(ROOT)) { for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) assertThat(Files.readString(path)).doesNotContain("implements EmployeeSessionVerifier"); }
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
