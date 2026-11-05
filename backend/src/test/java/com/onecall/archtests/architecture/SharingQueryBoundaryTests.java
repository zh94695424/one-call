package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.sharing.internal.application.*;
import com.onecall.core.sharing.internal.persistence.*;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.NamedInterface;

class SharingQueryBoundaryTests {
    private static final java.util.Set<String> QUERY_FILES = java.util.Set.of(
            "application/CustomerShareQueryService.java", "application/TrustedShareCaller.java", "application/SharePageRequest.java",
            "application/ShareCursor.java", "application/ShareSummary.java", "application/SharePage.java", "application/ShareQueryException.java",
            "application/ShareQueryDiagnostic.java", "application/package-info.java", "persistence/ShareQueryRepository.java",
            "persistence/JooqShareQueryRepository.java", "persistence/SharingPersistenceConfiguration.java", "persistence/SharingPersistenceProperties.java");

    private static boolean queryFile(Path path) {
        return QUERY_FILES.contains(Path.of("src/main/java/com/onecall/core/sharing/internal").relativize(path).toString().replace('\\', '/'));
    }
    @Test
    void sharingQueryApiAndProjectionAreExact() throws Exception {
        assertThat(Arrays.stream(CustomerShareQueryService.class.getDeclaredMethods()).filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(java.lang.reflect.Method::getName)).containsExactly("listCustomerShares");
        assertThat(ShareQueryRepository.class.getDeclaredMethods()).extracting(java.lang.reflect.Method::getName)
                .containsExactlyInAnyOrder("requireActiveCreatorContext", "fetch");
        assertThat(ShareQueryRepository.class.getMethod("requireActiveCreatorContext", TrustedShareCaller.class).getReturnType()).isEqualTo(void.class);
        assertThat(ShareQueryRepository.class.getMethod("fetch", TrustedShareCaller.class, SharePageRequest.class, int.class)
                .getGenericReturnType().getTypeName()).isEqualTo("java.util.List<" + ShareSummary.class.getName() + ">");
        components(TrustedShareCaller.class, "accountId", "organizationId");
        components(SharePageRequest.class, "cursor", "limit");
        components(ShareCursor.class, "accountId", "organizationId", "validFrom", "shareId");
        components(SharePage.class, "items", "nextCursor", "hasMore");
        components(ShareSummary.class, "shareId", "evidenceId", "verificationMode", "status", "validFrom", "expiresAt",
                "maxAdmissions", "admissionsUsed", "downloadAllowed", "codeGeneration", "revocationGeneration");
        components(ShareQueryDiagnostic.class, "operation", "errorCode", "exceptionClass", "sqlState", "category", "roleAssertionPassed");
        assertThat(Arrays.stream(TrustedShareCaller.class.getRecordComponents()).map(component -> component.getGenericType().getTypeName()))
                .containsExactly("java.util.UUID", "java.util.Optional<java.util.UUID>");
        assertThat(Arrays.stream(SharePageRequest.class.getRecordComponents()).map(component -> component.getGenericType().getTypeName()))
                .containsExactly("java.util.Optional<" + ShareCursor.class.getName() + ">", "java.util.Optional<java.lang.Integer>");
        assertThat(Arrays.stream(ShareCursor.class.getRecordComponents()).map(component -> component.getGenericType().getTypeName()))
                .containsExactly("java.util.UUID", "java.util.Optional<java.util.UUID>", "java.time.Instant", "java.lang.String");
        assertThat(Arrays.stream(SharePage.class.getRecordComponents()).map(component -> component.getGenericType().getTypeName()))
                .containsExactly("java.util.List<" + ShareSummary.class.getName() + ">", "java.util.Optional<" + ShareCursor.class.getName() + ">", "boolean");
        assertThat(Arrays.stream(ShareQueryDiagnostic.class.getRecordComponents()).map(component -> component.getGenericType().getTypeName()))
                .containsExactly("java.lang.String", ShareQueryException.Code.class.getName(), "java.lang.String", "java.util.Optional<java.lang.String>", "java.lang.String", "boolean");
        try (var paths = Files.walk(Path.of("src/main/java/com/onecall/core/sharing/internal"))) {
            assertThat(paths.filter(path -> path.toString().endsWith(".java")).filter(SharingQueryBoundaryTests::queryFile).map(path -> path.getFileName().toString()))
                    .containsExactlyInAnyOrder("CustomerShareQueryService.java", "TrustedShareCaller.java", "SharePageRequest.java", "ShareCursor.java",
                            "ShareSummary.java", "SharePage.java", "ShareQueryException.java", "ShareQueryDiagnostic.java", "ShareQueryRepository.java",
                            "JooqShareQueryRepository.java", "SharingPersistenceConfiguration.java", "SharingPersistenceProperties.java",
                            "package-info.java");
        }
        var modules = ApplicationModules.of(com.onecall.core.OneCallApplication.class);
        var namedInterfaces = modules.getModuleByName("sharing").orElseThrow().getNamedInterfaces();
        assertThat(namedInterfaces.stream().filter(NamedInterface::isNamed).map(NamedInterface::getName))
                .containsExactlyInAnyOrder("customer-query", "customer-command");
        var customerQuery = namedInterfaces.getByName("customer-query").orElseThrow();
        assertThat(customerQuery.asJavaClasses()
                .filter(type -> !type.getName().contains("$"))
                .map(com.tngtech.archunit.core.domain.JavaClass::getSimpleName))
                .containsExactlyInAnyOrder("CustomerShareQueryService", "TrustedShareCaller", "SharePageRequest",
                        "ShareCursor", "SharePage", "ShareSummary", "ShareQueryException", "ShareQueryDiagnostic");
        assertThat(customerQuery.asJavaClasses().map(com.tngtech.archunit.core.domain.JavaClass::getName)
                .filter(name -> name.contains("$")).map(name -> name.substring(name.lastIndexOf('$') + 1)))
                .containsExactlyInAnyOrder("VerificationMode", "ShareStatus", "Code");
        assertThat(namedInterfaces.getByName("customer-qurey")).isEmpty();
        assertThat(CustomerShareQueryService.class.getDeclaredMethods())
                .filteredOn(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .extracting(java.lang.reflect.Method::getName).containsExactly("listCustomerShares");
        assertThat(Files.readString(Path.of("src/main/java/com/onecall/core/sharing/internal/application/package-info.java")))
                .contains("@org.springframework.modulith.NamedInterface(\"customer-query\")")
                .doesNotContain("NamedInterface(\"customer-query\", \"", "NamedInterface(\"customer-query\")\n@org");
        assertThat(CustomerShareQueryService.class.getMethod("listCustomerShares", TrustedShareCaller.class, SharePageRequest.class).getReturnType())
                .isEqualTo(SharePage.class);
        String repository = Files.readString(Path.of("src/main/java/com/onecall/core/sharing/internal/persistence/JooqShareQueryRepository.java"));
        assertThat(repository).contains("context.select(SHARE_ID, EVIDENCE_ID, MODE, STATUS, VALID_FROM, EXPIRES_AT, MAX, USED, DOWNLOAD, CODE, REVOCATION)",
                "SQLDataType.TIMESTAMPWITHTIMEZONE", "ZoneOffset.UTC", "creator.eq(caller.accountId())", "organization::isNull",
                "VALID_FROM.lt(anchor).or(VALID_FROM.eq(anchor).and(SHARE_ID.lt(cursor.shareId())))");
        try (var paths = Files.walk(Path.of("src/main/java/com/onecall/core/sharing/internal"))) {
            for (Path path : paths.filter(Files::isRegularFile).filter(SharingQueryBoundaryTests::queryFile).toList()) {
                String source = Files.readString(path);
                assertThat(source).doesNotContain("org.flywaydb", "org.springframework.security", "jakarta.persistence", "org.springframework.web",
                        "com.onecall.core.customer_iam", "com.onecall.core.organization_access", "com.onecall.core.evidence",
                        "ExecuteListener", "SELECT *", "selectFrom(", "forUpdate", "forShare", "pg_advisory", "FOR UPDATE", "FOR SHARE",
                        "content_snapshot", "capability_token_hash", "code_salt", "code_hash", "share_access_event", "LocalDateTime", "COLLATE", "offset(");
                if (!path.getFileName().toString().equals("CustomerShareQueryService.java")) {
                    assertThat(source).doesNotContain("Logger", "System.out", "printStackTrace");
                }
            }
        }
        Path internal = Path.of("src/main/java/com/onecall/core/sharing/internal");
        try (var paths = Files.walk(internal)) {
            var commandPaths = paths.filter(Files::isRegularFile).filter(path -> !queryFile(path)).toList();
            assertThat(commandPaths.stream().map(path -> internal.relativize(path).toString().replace('\\', '/')))
                    .containsExactlyInAnyOrder("command/package-info.java", "command/CustomerShareCommand.java", "command/CustomerShareCommandResult.java",
                            "command/CustomerShareCommandService.java", "command/ShareCommandCaller.java", "command/ShareCommandException.java",
                            "commandservice/DefaultCustomerShareCommandService.java", "persistence/SharingCommandRepository.java",
                            "persistence/JooqSharingCommandRepository.java", "persistence/SharingCommandPersistenceConfiguration.java",
                            "persistence/SharingCommandPersistenceProperties.java", "provider/ProviderResult.java", "provider/ShareCreationPolicyProvider.java",
                            "provider/CapabilityTokenProvider.java", "provider/ShareExtractionCodeProvider.java", "provider/DesignatedMobileBindingProvider.java",
                            "provider/Sha256Provider.java", "provider/ExtractionCodeMaterial.java", "provider/ShareRotationPolicyProvider.java");
            for (Path path : commandPaths) {
                String source = Files.readString(path);
                assertThat(source).doesNotContain("org.flywaydb", "spring.datasource", "ONECALL_MIGRATION", "jdbc:", "SELECT *",
                        "implements CustomerSessionVerifier", "@TestConfiguration", "@Profile(\"test\")", "@Profile(\"dev\")",
                        "Logger", "System.out", "printStackTrace", "com.onecall.core.customer_iam", "com.onecall.core.organization_access", "com.onecall.core.subject_number");
                if (!path.getParent().endsWith("persistence")) assertThat(source).doesNotContain("content_snapshot", "code_salt", "code_hash",
                        "share_access_event", "FOR UPDATE", "pg_advisory", "selectFrom(");
            }
        }
        var commandApi = namedInterfaces.getByName("customer-command").orElseThrow();
        components(com.onecall.core.sharing.internal.provider.ShareRotationPolicyProvider.RotationPolicyRequest.class,
                "caller", "grantId", "verificationMode", "status", "codeGeneration", "revocationGeneration");
        components(com.onecall.core.sharing.internal.command.CustomerShareCommandResult.Rotated.class,
                "shareId", "verificationMode", "status", "codeGeneration", "revocationGeneration", "extractionCode");
        components(com.onecall.core.sharing.internal.command.CustomerShareCommandResult.RotationReplayed.class,
                "shareId", "verificationMode", "status", "codeGeneration", "revocationGeneration");
        assertThat(com.onecall.core.sharing.internal.provider.ShareRotationPolicyProvider.RotationPolicyDecision.values())
                .extracting(Enum::name).containsExactly("ALLOW", "DENY");
        assertThat(commandApi.asJavaClasses().filter(type -> !type.getName().contains("$")).map(com.tngtech.archunit.core.domain.JavaClass::getSimpleName))
                .containsExactlyInAnyOrder("CustomerShareCommandService", "CustomerShareCommand", "CustomerShareCommandResult", "ShareCommandCaller", "ShareCommandException");
    }

    private void components(Class<?> type, String... expected) {
        assertThat(Arrays.stream(type.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly(expected);
        assertThat(Arrays.stream(type.getRecordComponents()).map(java.lang.reflect.RecordComponent::getType))
                .noneMatch(component -> component == java.util.Map.class || component == org.jooq.Record.class);
    }
}
