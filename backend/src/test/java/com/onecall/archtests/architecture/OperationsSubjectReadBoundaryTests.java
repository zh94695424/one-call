package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class OperationsSubjectReadBoundaryTests {
    private static final Path ROOT = Path.of("src/main/java/com/onecall/core");
    private static final List<String> OWNERS = List.of("subject_number", "activation_consent", "entitlement_metering", "evidence", "catalog_order");
    private static final List<String> ADAPTERS = List.of("JooqOperationsSubjectReader", "JooqOperationsActivationReader", "JooqOperationsTrialReader", "JooqOperationsEvidenceCounter", "JooqOperationsOrderCounter");
    private static String source(String path) throws Exception { return Files.readString(ROOT.resolve(path)); }

    @Test
    void operationsPublishesOnlyExactReadContractsAndInboundPorts() {
        var module = ApplicationModules.of(OneCallApplication.class).getModuleByName("operations_iam").orElseThrow();
        assertThat(module.getNamedInterfaces().getByName("operations-subject-read")).isPresent();
        assertThat(Arrays.stream(OperationsSubjectReadPorts.class.getDeclaredClasses()).filter(Class::isInterface).map(Class::getSimpleName))
                .containsExactlyInAnyOrder("SubjectReader", "ActivationReader", "TrialReader", "EvidenceCounter", "OrderCounter");
        assertThat(Arrays.stream(OperationsSubjectReadService.class.getDeclaredMethods()).filter(method -> Modifier.isPublic(method.getModifiers())).map(java.lang.reflect.Method::getName))
                .containsExactlyInAnyOrder("list", "detail");
    }

    @Test
    void fiveOwnersImplementInboundPortsWithoutReverseModuleCycles() throws Exception {
        List<Class<?>> ports = List.of(SubjectReader.class, ActivationReader.class, TrialReader.class, EvidenceCounter.class, OrderCounter.class);
        for (int index = 0; index < OWNERS.size(); index++) {
            var adapter = Class.forName("com.onecall.core." + OWNERS.get(index) + ".internal.operationsread." + ADAPTERS.get(index));
            assertThat(adapter.getInterfaces()).containsExactly(ports.get(index));
        }
        ApplicationModules.of(OneCallApplication.class).verify();
    }

    @Test
    void controllerAndApplicationContainNoForeignBaseTableSql() throws Exception {
        for (String path : List.of("operations_iam/internal/web/OperationsSubjectReadController.java",
                "operations_iam/internal/subjectread/DefaultOperationsSubjectReadService.java")) {
            assertThat(source(path)).doesNotContain("subject_number.", "activation_consent.", "entitlement_metering.", "evidence.", "catalog_order.");
        }
    }

    @Test
    void ownerSqlSelectsOnlyApprovedSafeColumnsAndCounts() throws Exception {
        for (int index = 0; index < OWNERS.size(); index++) {
            String text = source(OWNERS.get(index) + "/internal/operationsread/" + ADAPTERS.get(index) + ".java");
            assertThat(text).contains("SELECT ", "context.scoped(Role.").doesNotContain("SELECT *", "number_ciphertext", "number_hmac", "verification_ref_ciphertext", "recording_url", "playback", "object_key");
            var references = java.util.regex.Pattern.compile("\\b(?:FROM|JOIN)\\s+(?!LATERAL\\b)([a-z_][a-z0-9_]*(?:\\.[a-z_][a-z0-9_]*)?)\\b",
                    java.util.regex.Pattern.CASE_INSENSITIVE).matcher(text).results().map(match -> match.group(1)).toList();
            assertThat(references).as(ADAPTERS.get(index)).isNotEmpty();
            String owner = OWNERS.get(index);
            assertThat(references).as(ADAPTERS.get(index)).anyMatch(reference -> reference.startsWith(owner + "."))
                    .allSatisfy(reference -> {
                        if ("subject_number".equals(owner) && "safe_subjects".equals(reference)) return;
                        assertThat(reference).startsWith(owner + ".");
                    });
        }
        assertThat(source("evidence/internal/operationsread/JooqOperationsEvidenceCounter.java")).contains("count(subject_id)", "WHERE subject_id = CAST(? AS uuid)").doesNotContain("JOIN");
        assertThat(source("catalog_order/internal/operationsread/JooqOperationsOrderCounter.java")).contains("count(subject_id)", "WHERE subject_id = CAST(? AS uuid)").doesNotContain("JOIN");
    }

    @Test
    void readPathHasNoLocksWritesAuditOutboxOrCommandCalls() throws Exception {
        var sources = new java.util.ArrayList<String>();
        sources.add(source("operations_iam/internal/subjectread/DefaultOperationsSubjectReadService.java"));
        for (int index = 0; index < OWNERS.size(); index++) sources.add(source(OWNERS.get(index) + "/internal/operationsread/" + ADAPTERS.get(index) + ".java"));
        for (String text : sources) assertThat(text).doesNotContain("INSERT ", "UPDATE ", "DELETE ", "FOR UPDATE", "FOR SHARE", "SKIP LOCKED", "audit_fact", "outbox_event", "pg_advisory", "subjectLifecycleService");
        assertThat(source("operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java"))
                .contains("setReadOnly(true)", "ISOLATION_REPEATABLE_READ", "PROPAGATION_REQUIRES_NEW", "setTimeout(5)");
    }

    @Test
    void employeeVerifierIsReusedWithoutPackagedProductionImplementation() throws Exception {
        try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                assertThat(Files.readString(path)).as(path.toString()).doesNotContain("implements EmployeeSessionVerifier");
            }
        }
        assertThat(source("operations_iam/internal/web/OperationsBoundaryFilter.java")).contains("authenticator.authenticate(request.getHeaders(\"Authorization\"))");
    }

    @Test
    void v001ThroughV017AndFrozenOpenApiAndDesignDocsRemainUnchanged() throws Exception {
        new OperationsLifecycleBoundaryTests().frozenOpenApiMigrationsAndPolicyBytesAreUnchanged();
        try (var paths = Files.list(Path.of("src/main/resources/db/migration"))) {
            assertThat(paths.filter(path -> path.toString().endsWith(".sql")).map(path -> path.getFileName().toString().substring(0, 4)).collect(Collectors.toSet()))
                    .containsExactlyInAnyOrderElementsOf(java.util.stream.IntStream.rangeClosed(1, 17).mapToObj(index -> "V%03d".formatted(index)).toList());
        }
    }

    @Test
    void allowlistedRoutesProjectionTypesAndDependencyGraphAreExact() throws Exception {
        String controller = source("operations_iam/internal/web/OperationsSubjectReadController.java");
        assertThat(controller).contains("@GetMapping(path = \"/api/v1/operations/subjects\")", "@GetMapping(path = \"/api/v1/operations/subjects/{subjectId}\")")
                .doesNotContain("@PostMapping", "@DeleteMapping", "@PutMapping");
        assertThat(Arrays.stream(SubjectSummary.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("subjectId", "source", "maskedNumber", "claimStatus", "activationStatus", "realNameStatus", "riskState", "createdAt");
        assertThat(Arrays.stream(SubjectPage.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("items", "nextCursor", "hasMore");
        var modules = ApplicationModules.of(OneCallApplication.class);
        for (String owner : OWNERS) assertThat(modules.getModuleByName(owner).orElseThrow().getAllowedDependencies(modules))
                .anySatisfy(dependency -> {
                    assertThat(dependency.getTargetModule().getIdentifier().toString()).isEqualTo("operations_iam");
                    assertThat(dependency.getTargetNamedInterface().getName()).isEqualTo("operations-subject-read");
                });
    }
}
