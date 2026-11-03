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
            assertThat(paths.filter(path -> path.toString().endsWith(".java")).map(path -> path.getFileName().toString()))
                    .containsExactlyInAnyOrder("CustomerShareQueryService.java", "TrustedShareCaller.java", "SharePageRequest.java", "ShareCursor.java",
                            "ShareSummary.java", "SharePage.java", "ShareQueryException.java", "ShareQueryDiagnostic.java", "ShareQueryRepository.java",
                            "JooqShareQueryRepository.java", "SharingPersistenceConfiguration.java", "SharingPersistenceProperties.java",
                            "package-info.java");
        }
        var modules = ApplicationModules.of(com.onecall.core.OneCallApplication.class);
        var namedInterfaces = modules.getModuleByName("sharing").orElseThrow().getNamedInterfaces();
        assertThat(namedInterfaces.stream().filter(NamedInterface::isNamed).map(NamedInterface::getName))
                .containsExactly("customer-query");
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
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
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
    }

    private void components(Class<?> type, String... expected) {
        assertThat(Arrays.stream(type.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly(expected);
        assertThat(Arrays.stream(type.getRecordComponents()).map(java.lang.reflect.RecordComponent::getType))
                .noneMatch(component -> component == java.util.Map.class || component == org.jooq.Record.class);
    }
}
