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
    private static final java.util.Set<String> GATEWAY_FILES = java.util.Set.of("gateway/MediaGatewayFilter.java",
            "gateway/MediaGatewayConfiguration.java", "gateway/SingleByteRange.java", "gateway/MediaGatewayProblemWriter.java");
    private static final java.util.Set<String> PLAYBACK_FILES = java.util.Set.of("OpaquePlaybackCredential.java", "VerifiedMediaClaim.java",
            "MediaCredentialVerifier.java", "PlaybackStateReader.java", "SharingPlaybackStateReader.java", "ControlledMediaReader.java",
            "MediaPlaybackService.java", "MediaPlaybackConfiguration.java");
    private static final java.util.Set<String> PUBLIC_ADMISSION_FILES = java.util.Set.of("publicadmission/package-info.java", "publicadmission/ShareAdmissionCapabilityToken.java", "publicadmission/ShareAdmissionRequest.java", "publicadmission/ShareAdmissionView.java", "publicadmission/ShareAdmissionException.java", "publicadmission/TrustedShareAdmissionCaller.java", "publicadmission/ShareAdmissionService.java", "publicadmission/ShareAdmissionJsonBinder.java", "publicadmission/MediaAuthorizationRequest.java", "publicadmission/ShareMediaAuthorizationService.java");
    private static final java.util.Set<String> ADMISSION_IMPLEMENTATION_FILES = java.util.Set.of("admission/ShareAdmissionRepository.java", "admission/JooqShareAdmissionRepository.java", "admission/DefaultShareAdmissionService.java", "admission/ShareAdmissionTicketProvider.java", "admission/ShareMediaCredentialProvider.java", "admission/ShareAdmissionPolicyProvider.java", "admission/ShareAdmissionCodeVerifier.java", "admission/DesignatedMobileProofVerifier.java", "admission/ShareAdmissionSha256Provider.java", "persistence/ShareAdmissionPersistenceConfiguration.java", "admission/DefaultShareMediaAuthorizationService.java");
    private static final java.util.Set<String> PUBLIC_READ_FILES = java.util.Set.of(
            "publicquery/package-info.java", "publicquery/OpaqueCapabilityToken.java", "publicquery/PublicShareQueryException.java",
            "publicquery/PublicShareView.java", "publicquery/PublicShareQueryService.java", "persistence/PublicShareQueryRepository.java",
            "persistence/JooqPublicShareQueryRepository.java", "persistence/PublicSharePersistenceConfiguration.java");
    private static final java.util.Set<String> QUERY_FILES = java.util.Set.of(
            "application/CustomerShareQueryService.java", "application/TrustedShareCaller.java", "application/SharePageRequest.java",
            "application/ShareCursor.java", "application/ShareSummary.java", "application/SharePage.java", "application/ShareQueryException.java",
            "application/ShareQueryDiagnostic.java", "application/package-info.java", "persistence/ShareQueryRepository.java",
            "persistence/JooqShareQueryRepository.java", "persistence/SharingPersistenceConfiguration.java", "persistence/SharingPersistenceProperties.java");

    private static boolean queryFile(Path path) {
        return QUERY_FILES.contains(Path.of("src/main/java/com/onecall/core/sharing/internal").relativize(path).toString().replace('\\', '/'));
    }
    private static boolean publicReadFile(Path path) {
        return PUBLIC_READ_FILES.contains(Path.of("src/main/java/com/onecall/core/sharing/internal").relativize(path).toString().replace('\\', '/'));
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
                .containsExactlyInAnyOrder("customer-query", "customer-command", "public-query", "public-admission");
        Path admissionRoot = Path.of("src/main/java/com/onecall/core/sharing/internal");
        try (var files = Files.walk(admissionRoot)) {
            var discovered = files.filter(Files::isRegularFile).map(admissionRoot::relativize).map(Path::toString).map(value -> value.replace('\\', '/'))
                    .filter(value -> value.startsWith("admission/") || value.startsWith("publicadmission/") || value.equals("persistence/ShareAdmissionPersistenceConfiguration.java")).collect(java.util.stream.Collectors.toSet());
            var expected = new java.util.HashSet<>(PUBLIC_ADMISSION_FILES); expected.addAll(ADMISSION_IMPLEMENTATION_FILES);
            assertThat(discovered).containsExactlyInAnyOrderElementsOf(expected);
            for (String relative : discovered) {
                String source = Files.readString(admissionRoot.resolve(relative));
                assertThat(source).doesNotContain("com.onecall.core.customer_iam", "Logger", "System.out", "printStackTrace", "SET ROLE");
                if (!relative.equals("admission/JooqShareAdmissionRepository.java"))
                    assertThat(source).doesNotContain("FOR UPDATE", "FOR SHARE", "pg_advisory", "INSERT INTO", "UPDATE sharing.", "SELECT code_hash");
                assertThat(queryFile(admissionRoot.resolve(relative))).isFalse();
                assertThat(publicReadFile(admissionRoot.resolve(relative))).isFalse();
            }
        }
        String admissionRepository = Files.readString(admissionRoot.resolve("admission/JooqShareAdmissionRepository.java"));
        String preliminaryLookup = admissionRepository.substring(admissionRepository.indexOf("Optional<GrantSnapshot> findGrantByTokenDigest"), admissionRepository.indexOf("Optional<GrantSnapshot> lockGrant"));
        String preliminaryAdmission = admissionRepository.substring(admissionRepository.indexOf("Optional<AdmissionRow> findAdmission"), admissionRepository.indexOf("private AdmissionRow admission"));
        for (String preliminary : List.of(preliminaryLookup, preliminaryAdmission))
            assertThat(preliminary).doesNotContain("FOR UPDATE", "FOR SHARE", "pg_advisory", "INSERT INTO", "UPDATE sharing.");
        String admissionService = Files.readString(admissionRoot.resolve("admission/DefaultShareAdmissionService.java"));
        assertThat(admissionService).contains("TransactionSynchronizationManager.isActualTransactionActive()", "if (durable.replay()) policy = policy(", "validateWitness(", "proof == null || policy == null || proof.unavailable()");
        assertThat(admissionService.indexOf("facts = facts(grant, caller)")).isLessThan(admissionService.indexOf("Optional<AdmissionRow> existing"));
        assertThat(admissionService).contains("grant.verificationMode().equals(witness.verificationMode())", "witness.codeGeneration()", "witness.revocationGeneration()",
                "facts.open()", "SHARE_RISK_BLOCKED", "eventId = \"admission-granted:\"", "ADMISSION_GRANTED", "TICKET_FAILED",
                "ShareAdmissionPolicyProvider.ApprovedTicketPolicy policy = null", "if (durable.replay()) policy = policy(");
        assertThat(admissionService.indexOf("lifecycle(original, caller)")).isLessThan(admissionService.indexOf("repository.lockGrant(original.id())"));
        assertThat(admissionService).doesNotContain("Logger", "System.out", "System.err", "printStackTrace", "LoggerFactory");
        assertThat(Arrays.stream(com.onecall.core.sharing.internal.publicadmission.ShareAdmissionCapabilityToken.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())).map(java.lang.reflect.Method::getName))
                .containsExactlyInAnyOrder("utf8", "toString");
        var admissionApi = namedInterfaces.getByName("public-admission").orElseThrow();
        assertThat(admissionApi.asJavaClasses().filter(type -> !type.getName().contains("$")).map(com.tngtech.archunit.core.domain.JavaClass::getSimpleName))
                .containsExactlyInAnyOrder("ShareAdmissionCapabilityToken", "ShareAdmissionRequest", "ShareAdmissionView", "ShareAdmissionException", "TrustedShareAdmissionCaller", "ShareAdmissionService", "ShareAdmissionJsonBinder", "MediaAuthorizationRequest", "ShareMediaAuthorizationService");
        assertThat(admissionApi.asJavaClasses().map(com.tngtech.archunit.core.domain.JavaClass::getName).filter(name -> name.contains("$")))
                .containsExactlyInAnyOrder("com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest$Verification", "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest$Verification$None", "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest$Verification$ExtractionCode", "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest$Verification$DesignatedMobile", "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest$Verification$OrganizationMember", "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionView$MediaAuthorization", "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException$Code", "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService$AdmissionOutcome", "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService$AdmissionCommitted", "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService$ReplayCommitted", "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService$DeniedCommitted", "com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest$Purpose", "com.onecall.core.sharing.internal.publicadmission.ShareMediaAuthorizationService$CallerResolver");
        assertThat(modules.getModuleByName("customer_iam").orElseThrow().getAllowedDependencies(modules)
                .stream().map(dependency -> dependency.getTargetModule().getIdentifier()+"::"+dependency.getTargetNamedInterface().getName()))
                .containsExactlyInAnyOrder("sharing::customer-query", "sharing::customer-command", "sharing::public-query", "sharing::public-admission",
                        "subject_number::customer-business-number-query", "activation_consent::customer-activation-query", "subject_number::customer-subject-query", "evidence::customer-subject-count-query", "catalog_order::customer-subject-count-query", "evidence::customer-evidence-query", "subject_number::customer-subject-claim-command", "activation_consent::customer-subject-activation-command", "catalog_order::customer-catalog-offer-query", "catalog_order::customer-order-command", "catalog_order::customer-order-query");
        var admissionMethod = com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService.class.getMethod("admit",
                com.onecall.core.sharing.internal.publicadmission.ShareAdmissionCapabilityToken.class,
                com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest.class, java.util.Optional.class);
        assertThat(admissionMethod.getGenericParameterTypes()[2].getTypeName()).isEqualTo("java.util.Optional<com.onecall.core.sharing.internal.publicadmission.TrustedShareAdmissionCaller>");
        assertThat(com.onecall.core.sharing.internal.publicadmission.ShareAdmissionJsonBinder.class.getMethod("bind", byte[].class)).isNotNull();
        assertThat(com.onecall.core.sharing.internal.publicadmission.ShareAdmissionJsonBinder.class.getMethod("bindVerification", tools.jackson.databind.JsonNode.class)).isNotNull();
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
            var commandPaths = paths.filter(Files::isRegularFile).filter(path -> !queryFile(path) && !publicReadFile(path))
                    .filter(path -> !PUBLIC_ADMISSION_FILES.contains(internal.relativize(path).toString().replace('\\', '/')) && !ADMISSION_IMPLEMENTATION_FILES.contains(internal.relativize(path).toString().replace('\\', '/')))
                    .filter(path -> !internal.relativize(path).toString().replace('\\', '/').startsWith("playback/"))
                    .filter(path -> !GATEWAY_FILES.contains(internal.relativize(path).toString().replace('\\', '/'))).toList();
            assertThat(commandPaths.stream().map(path -> internal.relativize(path).toString().replace('\\', '/')))
                    .containsExactlyInAnyOrder("command/package-info.java", "command/CustomerShareCommand.java", "command/CustomerShareCommandResult.java",
                            "command/CustomerShareCommandService.java", "command/ShareCommandCaller.java", "command/ShareCommandException.java",
                            "commandservice/DefaultCustomerShareCommandService.java", "persistence/SharingCommandRepository.java",
                            "persistence/JooqSharingCommandRepository.java", "persistence/SharingCommandPersistenceConfiguration.java",
                            "persistence/SharingCommandPersistenceProperties.java", "provider/ProviderResult.java", "provider/ShareCreationPolicyProvider.java",
                            "provider/CapabilityTokenProvider.java", "provider/ShareExtractionCodeProvider.java", "provider/DesignatedMobileBindingProvider.java",
                            "provider/Sha256Provider.java", "provider/ExtractionCodeMaterial.java", "provider/ShareRotationPolicyProvider.java",
                            "credential/MediaPlaybackSigningKeyProvider.java", "credential/MediaPlaybackCredentialAuthority.java", "credential/MediaPlaybackCredentialCodec.java",
                            "keysource/MediaPlaybackKeyBundleParser.java", "keysource/FileMediaPlaybackSigningKeyProvider.java", "keysource/MediaPlaybackKeySourceConfiguration.java",
                            "keysource/TencentSsmMediaPlaybackSigningKeyProvider.java", "keysource/TencentSsmSecretStringClient.java");
            for (Path path : commandPaths) {
                String source = Files.readString(path);
                assertThat(source).doesNotContain("org.flywaydb", "spring.datasource", "ONECALL_MIGRATION", "jdbc:", "SELECT *",
                        "implements CustomerSessionVerifier", "@TestConfiguration", "@Profile(\"test\")", "@Profile(\"dev\")",
                        "Logger", "System.out", "printStackTrace", "com.onecall.core.customer_iam", "com.onecall.core.organization_access", "com.onecall.core.subject_number");
                if (!path.getParent().endsWith("persistence")) assertThat(source).doesNotContain("content_snapshot", "code_salt", "code_hash",
                        "share_access_event", "FOR UPDATE", "pg_advisory", "selectFrom(");
            }
        }
        try (var files = Files.list(internal.resolve("credential"))) {
            var credentials = files.toList();
            assertThat(credentials.stream().map(path -> path.getFileName().toString())).containsExactlyInAnyOrder(
                    "MediaPlaybackSigningKeyProvider.java", "MediaPlaybackCredentialAuthority.java", "MediaPlaybackCredentialCodec.java");
            for (var file : credentials) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("NamedInterface", "com.onecall.core.evidence", "shareprojection", "org.jooq", "DataSource", "Transaction",
                        "Repository", "StateReader", "EvidenceMediaObjectReader", "ControlledMedia", "org.springframework.web", "jakarta.servlet", "Redis", "COS",
                        "KMS", "SecretManager", "java.net", "java.sql", "SELECT ", "INSERT ", "UPDATE ", "DELETE ",
                        "Logger", "System.out", "System.err", "printStackTrace", "@Value", "getProperty(", "getenv(");
                assertThat(source.lines().filter(line -> line.startsWith("import "))).allMatch(line -> line.startsWith("import java.nio.")
                        || line.startsWith("import java.security.") || line.startsWith("import java.time.") || line.startsWith("import java.util.")
                        || line.startsWith("import javax.crypto.") || line.equals("import com.fasterxml.jackson.annotation.JsonValue;")
                        || line.equals("import org.springframework.beans.factory.ObjectProvider;")
                        || line.equals("import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider;")
                        || line.equals("import com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose;")
                        || line.matches("import com\\.onecall\\.core\\.sharing\\.internal\\.playback\\.(MediaCredentialVerifier|OpaquePlaybackCredential|VerifiedMediaClaim|VerifiedMediaClaim\\.PlaybackWindow);"));
            }
        }
        try (var files = Files.walk(internal)) { assertThat(files.filter(Files::isRegularFile).count()).isEqualTo(81); }
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
        var publicApi = namedInterfaces.getByName("public-query").orElseThrow();
        assertThat(publicApi.asJavaClasses().filter(type -> !type.getName().contains("$"))
                .map(com.tngtech.archunit.core.domain.JavaClass::getSimpleName))
                .containsExactlyInAnyOrder("OpaqueCapabilityToken", "PublicShareQueryException", "PublicShareQueryService", "PublicShareView");
        assertThat(publicApi.asJavaClasses().filter(type -> type.getName().contains("$"))
                .map(com.tngtech.archunit.core.domain.JavaClass::getName))
                .containsExactlyInAnyOrder("com.onecall.core.sharing.internal.publicquery.PublicShareQueryException$Code",
                        "com.onecall.core.sharing.internal.publicquery.PublicShareQueryService$MemberAccess",
                        "com.onecall.core.sharing.internal.publicquery.PublicShareQueryService$MemberMatch",
                        "com.onecall.core.sharing.internal.publicquery.PublicShareView$Content",
                        "com.onecall.core.sharing.internal.publicquery.PublicShareView$Artifact",
                        "com.onecall.core.sharing.internal.publicquery.PublicShareView$VerificationChallenge",
                        "com.onecall.core.sharing.internal.publicquery.PublicShareView$VerificationMode",
                        "com.onecall.core.sharing.internal.publicquery.PublicShareView$Status",
                        "com.onecall.core.sharing.internal.publicquery.PublicShareView$ArtifactRole");
        var publicService = com.onecall.core.sharing.internal.publicquery.PublicShareQueryService.class;
        assertThat(Arrays.stream(publicService.getDeclaredMethods()).filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(java.lang.reflect.Method::getName)).containsExactly("getPublicShare");
        assertThat(publicService.getMethod("getPublicShare", com.onecall.core.sharing.internal.publicquery.OpaqueCapabilityToken.class,
                com.onecall.core.sharing.internal.publicquery.PublicShareQueryService.MemberAccess.class).getReturnType())
                .isEqualTo(com.onecall.core.sharing.internal.publicquery.PublicShareView.class);
        assertThat(Arrays.stream(PublicShareQueryRepository.class.getDeclaredMethods()).map(java.lang.reflect.Method::getName))
                .containsExactlyInAnyOrder("findByTokenDigest", "sampleDatabaseTime");
        assertThat(Arrays.stream(PublicShareQueryRepository.class.getDeclaredMethods()).flatMap(method -> Arrays.stream(method.getParameterTypes())))
                .doesNotContain(org.jooq.DSLContext.class);
        components(PublicShareQueryRepository.PublicGrant.class, "evidenceId", "ownerOrganizationId", "verificationMode", "status",
                "validFrom", "expiresAt", "downloadAllowed", "contentSnapshot");
        assertThat(PublicShareQueryRepository.PublicGrant.class.getRecordComponents()[7].getType()).isIn(String.class, tools.jackson.databind.JsonNode.class);
        components(com.onecall.core.sharing.internal.publicquery.PublicShareView.class, "verificationMode", "status", "expiresAt", "downloadAllowed", "content", "verificationChallenge");
        components(com.onecall.core.sharing.internal.publicquery.PublicShareView.Content.class, "capturedAt", "validatedDurationSeconds", "artifacts");
        components(com.onecall.core.sharing.internal.publicquery.PublicShareView.Artifact.class, "artifactRole", "mimeType", "sizeBytes", "sha256");
        components(com.onecall.core.sharing.internal.publicquery.PublicShareView.VerificationChallenge.class, "customerSessionRequired");
        assertThat(com.onecall.core.sharing.internal.publicquery.PublicShareQueryException.Code.values()).extracting(Enum::name)
                .containsExactly("UNAUTHENTICATED", "FORBIDDEN", "VALIDATION_FAILED", "SHARE_TOKEN_INVALID", "SHARE_REVOKED", "SHARE_EXPIRED", "RATE_LIMITED", "SERVICE_UNAVAILABLE", "INTERNAL");
        assertThat(com.onecall.core.sharing.internal.publicquery.PublicShareQueryService.MemberMatch.values()).extracting(Enum::name)
                .containsExactly("MATCH", "NONMATCH", "UNAUTHENTICATED", "UNAVAILABLE", "INTERNAL");
        assertThat(com.onecall.core.sharing.internal.publicquery.PublicShareView.VerificationMode.values()).extracting(Enum::name)
                .containsExactly("NONE", "EXTRACTION_CODE", "DESIGNATED_MOBILE", "ORGANIZATION_MEMBER");
        assertThat(com.onecall.core.sharing.internal.publicquery.PublicShareView.Status.values()).extracting(Enum::name)
                .containsExactly("ACTIVE", "EXPIRED", "REVOKED", "LOCKED");
        assertThat(com.onecall.core.sharing.internal.publicquery.PublicShareView.ArtifactRole.values()).extracting(Enum::name)
                .containsExactly("ORIGINAL", "DERIVED");
        assertThat(Modifier.isFinal(com.onecall.core.sharing.internal.publicquery.OpaqueCapabilityToken.class.getModifiers())).isTrue();
        assertThat(com.onecall.core.sharing.internal.publicquery.OpaqueCapabilityToken.class.isRecord()).isFalse();
        var member = com.onecall.core.sharing.internal.publicquery.PublicShareQueryService.MemberAccess.class;
        assertThat(member.getDeclaredMethods()).hasSize(1);
        assertThat(member.getMethod("check", java.util.UUID.class).getReturnType()).isEqualTo(com.onecall.core.sharing.internal.publicquery.PublicShareQueryService.MemberMatch.class);
        try (var paths = Files.walk(internal)) {
            var publicPaths = paths.filter(Files::isRegularFile).filter(SharingQueryBoundaryTests::publicReadFile).toList();
            assertThat(publicPaths.stream().map(path -> internal.relativize(path).toString().replace('\\', '/')))
                    .containsExactlyInAnyOrderElementsOf(PUBLIC_READ_FILES);
            for (Path path : publicPaths) {
                String source = Files.readString(path);
                assertThat(source).doesNotContain("Logger", "System.out", "System.err", "printStackTrace", "ExecuteListener",
                        "SELECT *", "selectFrom(", "forUpdate", "forShare", "pg_advisory", "share_access_event", "Outbox",
                        "CustomerShareCommandService", "DefaultCustomerShareCommandService", "SharingCommandRepository", "JooqSharingCommandRepository",
                        "com.onecall.core.customer_iam", "com.onecall.core.organization_access");
                assertThat(source).doesNotMatch("(?is).*\\b(insert|update|delete|merge|truncate|create|alter|drop)\\s+(into|from|table|schema|role|function|sharing\\.|evidence\\.).*");
                assertThat(source).doesNotMatch("(?is).*\\bfor\\s+(update|share)\\b.*");
                assertThat(source).doesNotMatch("(?is).*\\b(insert|update|delete|merge|truncate|create|alter|drop)\\b.*");
                assertThat(source).doesNotContain(".insertInto(", ".update(", ".deleteFrom(", ".mergeInto(", ".truncate(",
                        ".createTable(", ".alterTable(", ".dropTable(", "appendAudit", "appendOutbox", "appendAccess", "WITH ");
                if (!path.endsWith("JooqPublicShareQueryRepository.java")) assertThat(source).doesNotContain("capability_token_hash", "content_snapshot");
                if (path.getParent().endsWith("publicquery")) {
                    assertThat(source).doesNotContain("java.sql", "javax.sql", "DataSource", "Hikari");
                    if (!path.endsWith("PublicShareQueryService.java")) assertThat(source).doesNotContain("org.jooq", "DSLContext");
                    else assertThat(source.lines().filter(line -> line.startsWith("import org.jooq"))).containsExactly("import org.jooq.DSLContext;");
                }
            }
        }
    }

    private void components(Class<?> type, String... expected) {
        assertThat(Arrays.stream(type.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly(expected);
        assertThat(Arrays.stream(type.getRecordComponents()).map(java.lang.reflect.RecordComponent::getType))
                .noneMatch(component -> component == java.util.Map.class || component == org.jooq.Record.class);
    }
    @Test void mediaAuthorizationSurfacePreservesNamedInterfaceAndNoForeignWriteBoundary() throws Exception {
        Path sharingRoot = Path.of("src/main/java/com/onecall/core/sharing/internal");
        try (var paths = Files.walk(sharingRoot)) {
            var discovered = paths.filter(Files::isRegularFile).map(sharingRoot::relativize).map(Path::toString)
                    .map(value -> value.replace('\\', '/'))
                    .filter(value -> value.startsWith("publicadmission/") || value.startsWith("admission/")
                            || value.equals("persistence/ShareAdmissionPersistenceConfiguration.java"))
                    .collect(java.util.stream.Collectors.toSet());
            var expected = new java.util.HashSet<>(PUBLIC_ADMISSION_FILES);
            expected.addAll(ADMISSION_IMPLEMENTATION_FILES);
            assertThat(discovered).containsExactlyInAnyOrderElementsOf(expected);
        }
        var modules = ApplicationModules.of(com.onecall.core.OneCallApplication.class);
        var admissionApi = modules.getModuleByName("sharing").orElseThrow().getNamedInterfaces()
                .getByName("public-admission").orElseThrow();
        assertThat(admissionApi.asJavaClasses().filter(type -> !type.getName().contains("$"))
                .map(com.tngtech.archunit.core.domain.JavaClass::getSimpleName))
                .containsExactlyInAnyOrder("ShareAdmissionCapabilityToken", "ShareAdmissionRequest", "ShareAdmissionView",
                        "ShareAdmissionException", "TrustedShareAdmissionCaller", "ShareAdmissionService", "ShareAdmissionJsonBinder",
                        "MediaAuthorizationRequest", "ShareMediaAuthorizationService");
        assertThat(admissionApi.asJavaClasses().map(com.tngtech.archunit.core.domain.JavaClass::getName)
                .filter(name -> name.contains("$")))
                .containsExactlyInAnyOrder("com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest$Verification",
                        "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest$Verification$None",
                        "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest$Verification$ExtractionCode",
                        "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest$Verification$DesignatedMobile",
                        "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest$Verification$OrganizationMember",
                        "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionView$MediaAuthorization",
                        "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException$Code",
                        "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService$AdmissionOutcome",
                        "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService$AdmissionCommitted",
                        "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService$ReplayCommitted",
                        "com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService$DeniedCommitted",
                        "com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest$Purpose",
                        "com.onecall.core.sharing.internal.publicadmission.ShareMediaAuthorizationService$CallerResolver");
        var credentialProvider = com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.class;
        assertThat(List.of(com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.MediaAuthorizationCredentialRequest.class,
                com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.class,
                com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued.class,
                com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Unavailable.class,
                com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Malformed.class,
                com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.AuthorizedMediaFacts.class,
                com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.ReplayAnchor.class)
                .stream().map(Class::getName))
                .containsExactly("com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$MediaAuthorizationCredentialRequest",
                        "com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$MediaAuthorizationCredentialResult",
                        "com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$MediaAuthorizationCredentialResult$Issued",
                        "com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$MediaAuthorizationCredentialResult$Unavailable",
                        "com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$MediaAuthorizationCredentialResult$Malformed",
                        "com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$AuthorizedMediaFacts",
                        "com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider$ReplayAnchor");
        components(com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.MediaAuthorizationCredentialRequest.class,
                "shareGrantId", "shareAdmissionId", "purpose", "eventType", "requestKeyDigest", "codeGeneration", "revocationGeneration", "grantExpiresAt",
                "admissionExpiresAt", "sessionExpiresAt", "credentialExpiresAt", "preProviderFingerprint", "eventId", "mediaFacts", "priorReplayAnchor");
        components(com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.AuthorizedMediaFacts.class,
                "evidenceId", "mediaObjectId", "sha256", "sizeBytes", "mimeType", "durationSeconds", "windowStartNanos", "windowEndNanos");
        components(com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.ReplayAnchor.class,"issuedAtMicros","notBeforeMicros","kid");
        components(com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider.MediaAuthorizationCredentialResult.Issued.class,"credential","expiresAt","replayAnchor");
        Path controller = Path.of("src/main/java/com/onecall/core/customer_iam/internal/web/PublicSharesController.java");
        for (Path surface : List.of(controller, sharingRoot.resolve("publicadmission/MediaAuthorizationRequest.java"),
                sharingRoot.resolve("publicadmission/ShareMediaAuthorizationService.java"))) {
            String source = Files.readString(surface);
            assertThat(source).doesNotContain(".internal.persistence", ".internal.admission", "org.jooq", "DSLContext",
                    "TransactionTemplate", "PlatformTransactionManager", "ObjectStorage", "Redis", "COS", "GenericMedia",
                    "internal.command", "CustomerShareCommand", "CustomerShareCommandService");
        }
        assertThat(Files.readString(Path.of("../.github/workflows/ci.yml")))
                .contains("\"mediaAuthorizationSurfacePreservesNamedInterfaceAndNoForeignWriteBoundary\"");
        Path admissionRoot = sharingRoot;
        try (var paths = Files.walk(admissionRoot.resolve("publicadmission"))) {
            for (Path surface : paths.filter(Files::isRegularFile).toList()) {
                String source = Files.readString(surface);
                assertThat(source).doesNotContain(".internal.persistence", ".internal.admission", "org.jooq", "DSLContext",
                        "TransactionTemplate", "PlatformTransactionManager", "ObjectStorage", "Redis", "COS", "GenericMedia",
                        "internal.command", "CustomerShareCommand", "CustomerShareCommandService");
                assertThat(source).doesNotMatch("(?is).*\\b(insert|update|delete|merge|truncate|create|alter|drop)\\s+(into|from|table|schema|role|function|sharing\\.|evidence\\.).*");
            }
        }
    }

    @Test void gatewayUsesExactOwnedFilesAndEvidenceNamedInterface() throws Exception {
        Path internal = Path.of("src/main/java/com/onecall/core/sharing/internal");
        try (var files = Files.list(internal.resolve("gateway"))) {
            assertThat(files.map(path -> "gateway/" + path.getFileName())).containsExactlyInAnyOrderElementsOf(GATEWAY_FILES);
        }
        for (String file : GATEWAY_FILES) {
            String source = Files.readString(internal.resolve(file));
            assertThat(source).doesNotContain("evidence.internal", "customer_iam", "NamedInterface", "organization_access");
            assertThat(source.lines().filter(line -> line.startsWith("import com.onecall.core.evidence.")))
                    .allMatch(line -> line.equals("import com.onecall.core.evidence.mediaread.ControlledMediaByteSource;")
                            || line.equals("import com.onecall.core.evidence.mediaread.ControlledMediaByteSource.ByteInterval;"));
        }
        var interval = com.onecall.core.evidence.mediaread.ControlledMediaByteSource.ByteInterval.class;
        var byteApi = com.onecall.core.evidence.mediaread.ControlledMediaByteSource.class;
        assertThat(byteApi.getDeclaredMethods()).extracting(java.lang.reflect.Method::getName).containsExactly("open");
        assertThat(byteApi.getMethod("open", com.onecall.core.evidence.mediaread.ControlledMediaByteSource.OpenRequest.class).getReturnType())
                .isEqualTo(com.onecall.core.evidence.mediaread.ControlledMediaByteSource.Result.class);
        assertThat(com.onecall.core.evidence.mediaread.ControlledMediaByteSource.ReadSession.class.getDeclaredMethods())
                .extracting(java.lang.reflect.Method::getName).containsExactlyInAnyOrder("request", "read", "abort", "close", "redacted");
        assertThat(com.onecall.core.sharing.internal.gateway.SingleByteRange.Resolution.Selected.class.getRecordComponents()[0].getType()).isEqualTo(interval);
        assertThat(com.onecall.core.evidence.mediaread.ControlledMediaByteSource.OpenRequest.class.getRecordComponents()[1].getType()).isEqualTo(interval);
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            var definitions = paths.filter(path -> path.toString().endsWith(".java")).filter(path -> {
                try { return Files.readString(path).contains("record ByteInterval("); }
                catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            }).toList();
            assertThat(definitions).containsExactly(Path.of("src/main/java/com/onecall/core/evidence/mediaread/ControlledMediaByteSource.java"));
        }
        String api = Files.readString(Path.of("src/main/java/com/onecall/core/evidence/mediaread/ControlledMediaByteSource.java"));
        assertThat(api).contains("Result open(OpenRequest request) throws IOException", "interface ReadSession extends AutoCloseable", "@JsonValue")
                .doesNotContain("com.onecall.core.sharing", "InputStream", "bucket", "object_key");
    }

    @Test void gatewayTransfersWithoutTransactionsProjectionOrPublicContractChanges() throws Exception {
        Path internal = Path.of("src/main/java/com/onecall/core/sharing/internal");
        for (String file : GATEWAY_FILES) {
            String source = Files.readString(internal.resolve(file));
            assertThat(source).doesNotContain("@Transactional", "TransactionTemplate", "DataSource", "org.jooq", "java.sql", "FOR UPDATE", "FOR SHARE",
                    "pg_advisory", "INSERT INTO", "SELECT ", "ShareProjection", "insertAccessEvent", "issueOrReplay", "readAllBytes", "transferTo", ".available(",
                    "Executor", "CompletableFuture", "Logger", "System.out", "System.err", "printStackTrace", "getUserPrincipal", "getCookies");
        }
        String filter = Files.readString(internal.resolve("gateway/MediaGatewayFilter.java"));
        assertThat(filter).contains("/internal/media-gateway/v1/playback", "Purpose.PLAYBACK", "Purpose.SEEK", "new byte[8192]", "isSynchronizationActive()", "getResourceMap().isEmpty()");
        assertThat(filter.split("service\\.authorizeAndOpen\\(", -1)).hasSize(2);
        String transfer = filter.substring(filter.indexOf("private void transfer("), filter.indexOf("private void safeProblem("));
        assertThat(transfer).doesNotContain("service.", "clock.", "verifier.", "state.", "evidence.");
        byte[] publicContract = Files.readString(Path.of("../api/openapi/onecall-v1.openapi.yaml"), java.nio.charset.StandardCharsets.UTF_8)
                .replace("\r\n", "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(publicContract)))
                .isEqualTo("ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30");
        String contract = Files.readString(Path.of("../api/openapi/media-gateway-v1.openapi.yaml"));
        assertThat(contract).contains("/internal/media-gateway/v1/playback:", "operationId: readAuthorizedMedia", "MEDIA_FORBIDDEN", "MEDIA_UNAVAILABLE", "Content-Range", "WWW-Authenticate");
        assertThat(contract).doesNotContain("<<:").contains("'206':", "'401':", "'405':", "'416':", "Cache-Control:", "Pragma:",
                "X-Content-Type-Options:", "Content-Length:", "Accept-Ranges:", "Content-Range:", "WWW-Authenticate:", "Allow:");
        assertThat(contract).doesNotContain("onecall-v1.openapi.yaml", "https://", "bucket", "object_key");
    }

    @Test void playbackUsesOnlyNamedEvidenceReadAndPrivateTypedScope() throws Exception {
        Path root = Path.of("src/main/java/com/onecall/core/sharing/internal/playback");
        try (var paths = Files.list(root)) {
            assertThat(paths.map(path -> path.getFileName().toString())).containsExactlyInAnyOrderElementsOf(PLAYBACK_FILES);
        }
        for (String file : PLAYBACK_FILES) {
            String source = Files.readString(root.resolve(file));
            assertThat(source.lines().filter(line -> line.startsWith("import com.onecall.core.sharing.internal.credential.")))
                    .allMatch(line -> file.equals("MediaPlaybackConfiguration.java") && (line.equals("import com.onecall.core.sharing.internal.credential.MediaPlaybackCredentialAuthority;")
                            || line.equals("import com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider;")));
            assertThat(source).doesNotContain("NamedInterface", "evidence.internal", "evidence.shareprojection", "org.jooq", "byte[]", "InputStream",
                    "org.springframework.web", "bucket", "object_key", "object_version", "https://", "Logger", "System.out", "System.err",
                    "Supplier<", "Callable<", "Redis", "COS", "com.onecall.core.customer_iam", "com.onecall.core.organization_access");
            assertThat(source.lines().filter(line -> line.startsWith("import com.onecall.core.evidence.")))
                    .allMatch(line -> line.equals("import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;")
                            || line.equals("import com.onecall.core.evidence.mediaread.ResolvedMediaObject;"));
        }
        var service = com.onecall.core.sharing.internal.playback.MediaPlaybackService.class;
        assertThat(Arrays.stream(service.getDeclaredMethods()).filter(method -> Modifier.isPublic(method.getModifiers())).map(java.lang.reflect.Method::getName))
                .containsExactly("authorizeAndOpen");
        assertThat(service.getMethod("authorizeAndOpen", com.onecall.core.sharing.internal.playback.OpaquePlaybackCredential.class,
                com.onecall.core.sharing.internal.publicadmission.MediaAuthorizationRequest.Purpose.class).getReturnType())
                .isEqualTo(com.onecall.core.sharing.internal.playback.MediaPlaybackService.Result.class);
        assertThat(Arrays.stream(com.onecall.core.sharing.internal.playback.ControlledMediaReader.ReadScope.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)).containsExactly("media", "purpose", "window", "expiresAt");
        assertThat(Arrays.stream(com.onecall.core.sharing.internal.playback.ControlledMediaReader.ControlledReadHandle.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)).containsExactly("scope");
        for (Class<?> result : List.of(com.onecall.core.sharing.internal.playback.MediaCredentialVerifier.Result.class,
                com.onecall.core.sharing.internal.playback.PlaybackStateReader.Result.class,
                com.onecall.core.sharing.internal.playback.ControlledMediaReader.Result.class,
                com.onecall.core.sharing.internal.playback.MediaPlaybackService.Result.class)) {
            assertThat(result.isSealed()).isTrue();
            assertThat(Arrays.stream(result.getPermittedSubclasses()).map(Class::getSimpleName)).contains("Unavailable");
        }
        var modules = ApplicationModules.of(com.onecall.core.OneCallApplication.class);
        assertThat(modules.getModuleByName("sharing").orElseThrow().getAllowedDependencies(modules).stream()
                .map(dependency -> dependency.getTargetModule().getIdentifier() + "::" + dependency.getTargetNamedInterface().getName()))
                .containsExactlyInAnyOrder("evidence::share-projection", "evidence::media-read");
    }

    @Test void keySourceUsesOnlyLocalReadAndExistingProviderBoundary() throws Exception {
        Path root = Path.of("src/main/java/com/onecall/core/sharing/internal/keysource");
        var expected = java.util.Set.of("MediaPlaybackKeyBundleParser.java", "FileMediaPlaybackSigningKeyProvider.java", "MediaPlaybackKeySourceConfiguration.java",
                "TencentSsmMediaPlaybackSigningKeyProvider.java", "TencentSsmSecretStringClient.java");
        try (var files = Files.walk(root)) {
            assertThat(files.filter(Files::isRegularFile).map(path -> root.relativize(path).toString())).containsExactlyInAnyOrderElementsOf(expected);
        }
        var parserImports = java.util.Set.of("com.fasterxml.jackson.annotation.JsonValue",
                "com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Entry",
                "com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Result",
                "com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.Role",
                "java.nio.ByteBuffer", "java.nio.charset.CharacterCodingException", "java.nio.charset.CodingErrorAction", "java.nio.charset.StandardCharsets",
                "java.util.ArrayList", "java.util.Arrays", "java.util.Comparator", "java.util.HashSet", "java.util.HexFormat", "java.util.Set",
                "tools.jackson.core.StreamReadConstraints", "tools.jackson.core.StreamReadFeature", "tools.jackson.core.json.JsonFactory",
                "tools.jackson.databind.DeserializationFeature", "tools.jackson.databind.JsonNode", "tools.jackson.databind.json.JsonMapper");
        var fileImports = java.util.Set.of("com.fasterxml.jackson.annotation.JsonValue",
                "com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider", "java.io.IOException", "java.nio.ByteBuffer",
                "java.nio.file.Files", "java.nio.file.LinkOption", "java.nio.file.Path", "java.nio.file.StandardOpenOption",
                "java.nio.file.attribute.BasicFileAttributes", "java.util.Arrays", "java.util.Objects", "java.util.Set");
        var configImports = java.util.Set.of("com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider",
                "org.springframework.context.annotation.Bean", "org.springframework.context.annotation.Condition", "org.springframework.context.annotation.ConditionContext",
                "org.springframework.context.annotation.Conditional", "org.springframework.context.annotation.Configuration", "org.springframework.core.env.Environment",
                "org.springframework.core.type.AnnotatedTypeMetadata");
        for (String file : java.util.Set.of("MediaPlaybackKeyBundleParser.java", "FileMediaPlaybackSigningKeyProvider.java", "MediaPlaybackKeySourceConfiguration.java")) {
            String source = Files.readString(root.resolve(file));
            assertThat(source.lines().filter(line -> line.startsWith("import ")).map(line -> line.substring(7, line.length() - 1)))
                    .containsExactlyInAnyOrderElementsOf(file.startsWith("File") ? fileImports : file.contains("Configuration") ? configImports : parserImports);
            assertThat(source).doesNotContain("NamedInterface", "Serializable", "Logger", "System.out", "System.err", "printStackTrace", "getenv(",
                    "System.getProperty", "getResource", "classpath:", "java.net", "java.sql", "SecretManager", "KMS", "COS", "@Scheduled", "WatchService",
                    "cache", "lastGood", "DataSource", "Repository", "Evidence", "Authorization", "MediaPlaybackCredentialAuthority", "issueOrReplay",
                    "StandardOpenOption.WRITE", "StandardOpenOption.CREATE", "StandardOpenOption.APPEND", "Files.write", "Files.copy", "Files.move", "Files.delete",
                    "Files.list", "Files.walk", "readAllBytes", "newOutputStream", "@ConditionalOnMissingBean", "@Primary", "@Order", "@Profile");
            if (!file.contains("Configuration")) assertThat(source).contains("@JsonValue", "[REDACTED]", "String toString()", "new Result.Unavailable()")
                    .doesNotContain("getProperty(", "Environment", "org.springframework", "throw new IOException(\"", "throw new IllegalArgumentException(\"");
        }
        String parser = Files.readString(root.resolve("MediaPlaybackKeyBundleParser.java"));
        assertThat(parser).contains("65_536", "maxNestingDepth(8)", "STRICT_DUPLICATE_DETECTION", "FAIL_ON_TRAILING_TOKENS",
                "CodingErrorAction.REPORT", "mapper.readTree(json)", "'\\ufeff'", "Set.of(\"schemaVersion\", \"keys\")",
                "Set.of(\"kid\", \"role\", \"keyHex\")", "isIntegralNumber()", "version.asInt() != 1", "isArray()", "keys.isEmpty()",
                "[A-Za-z0-9_-]{1,32}", "!kids.add(kid)", "[0-9a-f]{64}", "Role.valueOf(role)", "material.length != 32",
                "if (active != null)", "if (active == null)", "Comparator.comparing(Entry::kid)", "new Result.Available(active, verifyOnly)", "Arrays.fill(material");
        String adapter = Files.readString(root.resolve("FileMediaPlaybackSigningKeyProvider.java"));
        assertThat(adapter).contains("implements MediaPlaybackSigningKeyProvider", "Result currentKeys()", "path.isAbsolute()", "path.equals(path.normalize())",
                "source = readBoundedSource(path)", "return parser.parse(source)", "byte[] readBoundedSource(Path path)", "isRegularFile()", "isSymbolicLink()",
                "Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)", "try (var channel", "MAX_SOURCE_BYTES + 1", "channel.read(target)",
                "target.position() != before.size()", "before.fileKey()", "before.lastModifiedTime()", "IOException | RuntimeException", "Arrays.fill(source", "Arrays.fill(buffer");
        String configuration = Files.readString(root.resolve("MediaPlaybackKeySourceConfiguration.java"));
        assertThat(configuration).contains("@Configuration(proxyBeanMethods = false)", "\"onecall.sharing.media-playback.signing-key-source.\"",
                "@Conditional(FileSelection.class)", "@Conditional(TencentSelection.class)", "onecall.sharing.media-playback.signing-key-source.bundle-path",
                "\"file\".equals(context.getEnvironment().getProperty(PREFIX + \"provider\"))",
                "\"tencent-ssm\".equals(context.getEnvironment().getProperty(PREFIX + \"provider\"))",
                "!context.getEnvironment().containsProperty(PREFIX + \"bundle-path\")",
                "catch (RuntimeException failure) { path = null; }", "return new FileMediaPlaybackSigningKeyProvider(path);");
        String fileCondition = configuration.substring(configuration.indexOf("static final class FileSelection"), configuration.indexOf("static final class TencentSelection"));
        assertThat(fileCondition).doesNotContain("tencent.", "containsProperty", "bundle-path", "Settings", "trim(", "equalsIgnoreCase");
        assertThat(configuration.lines().filter(line -> line.strip().equals("@Bean")).count()).isEqualTo(2);
        assertThat(Arrays.stream(com.onecall.core.sharing.internal.keysource.MediaPlaybackKeySourceConfiguration.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(org.springframework.context.annotation.Bean.class)).map(java.lang.reflect.Method::getReturnType))
                .containsExactly(com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.class,
                        com.onecall.core.sharing.internal.credential.MediaPlaybackSigningKeyProvider.class);
    }

    @Test void tencentKeySourceConfinesSdkAndPreservesExistingAuthority() throws Exception {
        Path production = Path.of("src/main/java");
        Path root = production.resolve("com/onecall/core/sharing/internal/keysource");
        Path clientPath = root.resolve("TencentSsmSecretStringClient.java");
        var sdkImports = java.util.Set.of("com.tencentcloudapi.common.exception.TencentCloudSDKException",
                "com.tencentcloudapi.common.profile.ClientProfile", "com.tencentcloudapi.common.profile.HttpProfile",
                "com.tencentcloudapi.common.provider.CvmRoleCredential", "com.tencentcloudapi.ssm.v20190923.SsmClient",
                "com.tencentcloudapi.ssm.v20190923.models.GetSecretValueRequest", "com.tencentcloudapi.ssm.v20190923.models.GetSecretValueResponse");
        try (var paths = Files.walk(production)) {
            for (Path path : paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                if (!path.equals(clientPath)) assertThat(source).doesNotContain("com.tencentcloudapi", "okhttp3", "okio.");
            }
        }
        String client = Files.readString(clientPath);
        assertThat(client.lines().filter(line -> line.startsWith("import com.tencentcloudapi.")).map(line -> line.substring(7, line.length() - 1)))
                .containsExactlyInAnyOrderElementsOf(sdkImports);
        assertThat(client).contains("private synchronized SsmClient client()", "if (client == null) client = factory.create(settings)",
                "new SsmClient(new CvmRoleCredential(), settings.region, profile)", "new HttpProfile()", "HttpProfile.REQ_POST", "HttpProfile.REQ_HTTPS",
                "ssm.tencentcloudapi.com", "http.setConnTimeout(settings.connectSeconds)", "http.setReadTimeout(settings.readSeconds)",
                "http.setWriteTimeout(settings.connectSeconds)", "new ClientProfile(ClientProfile.SIGN_TC3_256, http)",
                "profile.setDebug(false)", "profile.setUnsignedPayload(false)", "request.setSecretName(settings.secretName)", "request.setVersionId(settings.versionId)",
                "response == null", "!settings.secretName.equals(response.getSecretName())", "!settings.versionId.equals(response.getVersionId())",
                "response.getSecretBinary()", "response.getSecretString()", "!binary.isEmpty()", "secret.isBlank()", "TencentCloudSDKException | RuntimeException");
        assertThat(client.split("\\.GetSecretValue\\(", -1)).hasSize(2);
        String provider = Files.readString(root.resolve("TencentSsmMediaPlaybackSigningKeyProvider.java"));
        assertThat(provider).contains("implements MediaPlaybackSigningKeyProvider", "Result currentKeys()", "settings == null || reader == null",
                "String secret = reader.read()", "secret.length() > MediaPlaybackKeyBundleParser.MAX_SOURCE_BYTES", "CodingErrorAction.REPORT",
                "target.position() > MediaPlaybackKeyBundleParser.MAX_SOURCE_BYTES", "return parser.parse(source)", "Arrays.fill(source", "Arrays.fill(buffer",
                "connect == null ? 2", "read == null ? 3", "1_024", "256", "[a-z0-9]+(?:-[a-z0-9]+)+");
        for (String source : List.of(client, provider)) {
            assertThat(source).contains("@JsonValue", "[REDACTED]", "String toString()")
                    .doesNotContain("NamedInterface", "Serializable", "Logger", "System.out", "System.err", "printStackTrace", "getMessage(", "getCause(", "getRequestId(",
                            "DefaultCredentialsProvider", "EnvironmentVariable", "ProfileCredentials", "OIDC", "new Credential(", "SecretId", "SecretKey", "getSnapshot(",
                            "getenv(", "System.getProperty", "java.net", "metadata.tencentyun", "okhttp3", "okio.", "Base64", "getResource", "Files.",
                            "FileMediaPlayback", "Repository", "DataSource", "java.sql", "org.springframework.web", "jakarta.servlet", "MediaPlaybackCredentialAuthority",
                            "issueOrReplay", "@Scheduled", "Executor", "Thread", "Timer", "cache", "lastGood", "TTL", "ttl", "retry(", "sleep(", "fallback",
                            "CreateSecret", "UpdateSecret", "DeleteSecret", "ListSecrets", "DescribeSecret", "PutSecret", "EnableSecret", "DisableSecret",
                            "setEncryptionPublicKey", "setEncryptionAlgorithm", "setProxy", "setHttpClient", "setSsl", "setHostname", "setRootDomain", "setApigwEndpoint");
        }
        String configuration = Files.readString(root.resolve("MediaPlaybackKeySourceConfiguration.java"));
        assertThat(configuration).doesNotContain("@Primary", "@Order", "@ConditionalOnMissingBean", "@Profile", "trim(", "equalsIgnoreCase", "new SsmClient", "CvmRoleCredential");
        String authority = Files.readString(production.resolve("com/onecall/core/sharing/internal/credential/MediaPlaybackCredentialAuthority.java"));
        assertThat(authority.indexOf("providers.size() != 1")).isLessThan(authority.indexOf("currentKeys()"));
    }

    @Test void playbackStateAndOpenPreserveReadOnlyNoProjectionBoundary() throws Exception {
        Path root = Path.of("src/main/java/com/onecall/core/sharing/internal/playback");
        for (String file : PLAYBACK_FILES) assertThat(Files.readString(root.resolve(file)))
                .doesNotContain("FOR UPDATE", "FOR SHARE", "pg_advisory", "INSERT INTO", "UPDATE sharing.", "DELETE FROM",
                        "lockGrant(", "acquireOrganization", "insertAccessEvent", "insertAdmission", "expireGrant(", "updateGrantAdmissionCount",
                        "ShareMediaCredentialProvider", "issueOrReplay", "ShareProjection", "@Transactional");
        String state = Files.readString(root.resolve("SharingPlaybackStateReader.java"));
        assertThat(state).contains("setReadOnly(true)", "ISOLATION_REPEATABLE_READ", "PROPAGATION_REQUIRES_NEW", "findGrantById(claim.shareGrantId())");
        String repository = Files.readString(Path.of("src/main/java/com/onecall/core/sharing/internal/admission/JooqShareAdmissionRepository.java"));
        String query = repository.substring(repository.indexOf("@Override public Optional<GrantSnapshot> findGrantById"), repository.indexOf("@Override public Optional<GrantSnapshot> findGrantByTokenDigest"));
        assertThat(query).contains("principal();", "SELECT \" + GRANT_COLUMNS", "JOIN evidence.evidence_record evidence_row ON evidence_row.id=grant_row.evidence_id WHERE grant_row.id=?\", grantId)")
                .doesNotContain("FOR UPDATE", "FOR SHARE", "capability_token_hash", "media_object", "evidence_artifact");
        String preliminary = repository.substring(repository.indexOf("@Override public Optional<GrantSnapshot> findGrantByTokenDigest"), repository.indexOf("@Override public Optional<GrantSnapshot> lockGrant"));
        assertThat(preliminary).contains("WHERE grant_row.capability_token_hash=?\", digest)").doesNotContain("principal();");
        String service = Files.readString(root.resolve("MediaPlaybackService.java"));
        String afterOpen = service.substring(service.indexOf("var opened = reader.open(scope);"), service.indexOf("static boolean validShape"));
        assertThat(afterOpen).doesNotContain("clock.", "state.", "evidence.", "verifier.", "repository.", "transactions.");
    }
}
