package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders.*;
import com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders;
import com.onecall.core.operations_iam.internal.ivrread.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;


class OperationsIvrSessionReadBoundaryTests {
    private String source(String path) throws Exception { return java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/onecall/core/" + path)); }
    private String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static final String FROZEN = """
            api/openapi/onecall-v1.openapi.yaml 0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68 ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30
            docs/12-technology-stack-and-system-architecture.md 9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db 772ccaee7b188f68fcc694f3e8c00983ad9de028547f8e0b0c8638a8d8cae435
            docs/14-production-data-model-and-flyway-schema-spec.md 0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31 bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58
            docs/15-production-openapi-surface-spec.md db2f9b5a548288b71be4566ce1b820b9a9ffd0118978c24f92bdcc69d6aab5a0 0b3b996e3e4a4acc3cb38211290be5ae874d66e7c63d670958b8bc23f67b9b02
            docs/17-production-security-design-spec.md b531046014f80889bba6e83992814c77fd5a51f564d511ad6bfe4fd6b8122205 4e7df589380f70ba7e9e1a5b7e9c1542ca6ed89313a775dacfc91e109852ceb6
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
            backend/src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c 892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817
            backend/ops/postgresql/pa25-runtime-identities.sql b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9 8f02d4d4842e734f10eac0c15b431afa16eb062790773b8696ae0de67d63b451
            docs/operations/pa25-production-database-identity-provisioning-runbook.md 44ced2245fc1c89175eeb386bc6cc6fa036302e368829cdd163f3da022425423 792a32f72d6657b59a220a42ec7c11f94361078c8c0d491ff52d777fe22f08ab
            backend/README.md ca9d8aa4a92066f6e9a3f44fb9ad5c66f156a569fcc5ab478d05410f987627b5 0816cc1c1ef622eaec9d4d7545e1ee8c6dc0f35813d4748b41d44dc9d4cec20b
            """;
    @Test
    void publishesOnlyTwoReadMethodsAndExactFrozenRecords() throws Exception {
        assertThat(Arrays.stream(OperationsIvrSessionReadService.class.getDeclaredMethods()).filter(method -> !java.lang.reflect.Modifier.isPrivate(method.getModifiers())).map(java.lang.reflect.Method::getName)).containsExactlyInAnyOrder("list", "detail");
        assertThat(Arrays.stream(IvrSessionSummary.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("sessionId", "callId", "subjectId", "status", "nodeId", "protocolVersion", "promptVersion", "guidanceStatus", "callerMasked", "accessNumberMasked", "createdAt", "confirmedAt");
        assertThat(Arrays.stream(IvrSessionDetail.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("sessionId", "callId", "subjectId", "status", "nodeId", "protocolVersion", "promptVersion", "guidanceStatus", "callerMasked", "accessNumberMasked", "createdAt", "confirmedAt", "call", "decisions", "legs", "callEvents", "evidence");
    }
    @Test
    void operationsRoutesAndControllerMappingsAreExact() throws Exception {
        var mappings = Arrays.stream(com.onecall.core.operations_iam.internal.web.OperationsIvrSessionReadController.class.getDeclaredMethods()).map(method -> method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class)).filter(Objects::nonNull).flatMap(mapping -> Arrays.stream(mapping.path())).toList(); assertThat(mappings).containsExactlyInAnyOrder(BASE, BASE + "/{sessionId}");
        for (String file : List.of("operations_iam/internal/web/OperationsBoundaryFilter.java", "customer_iam/internal/web/CustomerBoundaryFilter.java")) assertThat(source(file)).contains(BASE, BASE + "/[^/;%]+", "getContextPath()");
    }
    @Test
    void ivrSqlUsesOnlyExplicitApprovedColumns() throws Exception {
        String authority = source("operations_iam/internal/ivrread/DefaultOperationsIvrSessionReadService.java");
        var projections = java.util.regex.Pattern.compile("SELECT\\s+[^\"]+?FROM\\s+operations_iam\\.employee_account[^\"]*").matcher(authority);
        var actual = new ArrayList<String>();
        while (projections.find()) actual.add(projections.group());
        assertThat(actual).containsExactly("SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)");
        String sql = source("ivr/internal/operationsread/JooqOperationsIvrSessionReader.java"); assertThat(sql).contains("SELECT s.id, s.call_id, s.subject_id, s.status, s.node_id,", "s.protocol_version, s.prompt_version, s.guidance_status,", "s.caller_masked, s.access_number_masked, s.created_at, s.confirmed_at", "WHERE c.session_id = s.id AND t.task_id = ?", "ORDER BY s.created_at DESC, s.id DESC", "LIMIT ?", "Role.IVR"); assertThat(sql).doesNotContain("SELECT *", "trusted_caller", "confirmed_subject_id", "ciphertext", "hmac", "response_snapshot", "payload_redacted", "source_event", "FOR UPDATE", "OFFSET");
    }
    @Test
    void evidenceSqlNeverReadsMediaRecordingOrProtectedColumns() throws Exception {
        String sql = source("evidence/internal/operationsread/JooqOperationsIvrEvidenceReader.java"); assertThat(sql).contains("SELECT id, status, session_id, call_id", "FROM evidence.evidence_record", "SELECT evidence_id, artifact_role FROM evidence.evidence_artifact", "GROUP BY evidence_id, artifact_role ORDER BY evidence_id ASC", "start += 1000", "Math.min(start + 1000, ids.size())", "Role.EVIDENCE", "allowed.contains(id)"); assertThat(sql).doesNotContain("SELECT *", "source_ref_id", "media_object", "recording", "snapshot", "sha256", "bucket", "object_key", "ivr.");
    }
    @Test
    void applicationAndControllerContainNoForeignSql() throws Exception {
        for (String file : List.of("operations_iam/internal/ivrread/DefaultOperationsIvrSessionReadService.java", "operations_iam/internal/web/OperationsIvrSessionReadController.java")) assertThat(source(file)).doesNotContain("FROM ivr.", "FROM evidence.", "JooqOperationsIvr", "@Resource", "getBean(");
    }
    @Test
    void readPathHasNoWritesAuditOutboxLocksOrCursorStorage() throws Exception {
        for (String file : List.of("operations_iam/internal/ivrread/DefaultOperationsIvrSessionReadService.java", "ivr/internal/operationsread/JooqOperationsIvrSessionReader.java", "evidence/internal/operationsread/JooqOperationsIvrEvidenceReader.java")) assertThat(source(file)).doesNotContain("INSERT ", "UPDATE ", "DELETE ", "TRUNCATE ", "FOR UPDATE", "FOR SHARE", "pg_advisory", "Outbox", "Audit", "CREATE TABLE", "GRANT ");
    }
    @Test
    void namedInterfaceAndExactThirteenModuleEdgesArePreserved() throws Exception {
        var exercised = EnumSet.noneOf(OperationsReadContext.Role.class);
        for (String file : List.of("OperationsSubjectReadRoleLeastPrivilegeIT.java", "OperationsIvrSessionReadRoleLeastPrivilegeIT.java")) {
            String handoff = java.nio.file.Files.readString(java.nio.file.Path.of("src/test/java/com/onecall/archtests/container").resolve(file));
            var loops = java.util.regex.Pattern.compile("for \\(var role : List\\.of\\(([^)]+)\\)\\)").matcher(handoff);
            assertThat(loops.find()).as(file).isTrue();
            for (String role : loops.group(1).split(",\\s*")) exercised.add(OperationsReadContext.Role.valueOf(role.substring("OperationsReadContext.Role.".length())));
            assertThat(loops.find()).as(file).isFalse();
        }
        assertThat(exercised).containsExactlyInAnyOrder(OperationsReadContext.Role.values());
        var modules = org.springframework.modulith.core.ApplicationModules.of(com.onecall.core.OneCallApplication.class); modules.verify();
        assertThat(source("ivr/package-info.java")).contains("operations_iam::operations-subject-read"); assertThat(source("operations_iam/package-info.java")).doesNotContain("ivr", "evidence");
        int count = modules.stream().mapToInt(module -> (int) module.getDirectDependencies(modules).uniqueModules().count()).sum(); assertThat(count).isEqualTo(13);
    }
    @Test
    void frozenInputsEighteenMigrationsAndIndependentVerifierRemainExact() throws Exception {
        String frozen = FROZEN;
        var charset = java.nio.charset.StandardCharsets.UTF_8;
        String planPath = "docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md";
        String expectedDigest = "afda283795031bd226b3d48db7b9384ffacc5592798cd410769c9ec7ff8bc761";
        java.util.function.Function<byte[], String> sha256 = bytes -> {
            try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
        };
        java.util.function.Function<String, byte[]> historicalBytes = path -> {
            try {
                if (path.equals("backend/ops/postgresql/pa25-runtime-identities.sql")) return CustomerCatalogOfferReadAclBoundaryTests.historicalPa25().replace("\r\n","\n").getBytes(charset);
                String text = java.nio.file.Files.readString(java.nio.file.Path.of("..").resolve(path), charset).replace("\r\n","\n");
                if (path.equals("backend/README.md") || path.equals("docs/operations/pa25-production-database-identity-provisioning-runbook.md")) {
                    int addendum = text.indexOf("## PA40A Current Provisioning Boundary");
                    int marker = text.indexOf("<!-- PA36 HISTORICAL BODY -->", addendum);
                    assertThat(addendum).isPositive(); assertThat(marker).isGreaterThan(addendum);
                    text = text.substring(0, addendum) + text.substring(marker);
                }
                return text.getBytes(charset);
            } catch (Exception failure) { throw new IllegalStateException(failure); }
        };
        java.util.function.BiConsumer<java.nio.file.Path, String> verifyFrozen = (root, independent) -> {
            try {
                var map = new TreeMap<String, List<String>>((left, right) -> Arrays.compareUnsigned(left.getBytes(charset), right.getBytes(charset)));
                for (String row : independent.strip().lines().toList()) {
                    String[] fields = row.split(" ");
                    assertThat(fields).hasSize(3);
                    assertThat(map.put(fields[0], List.of(fields[1], fields[2]))).isNull();
                }
                assertThat(map).hasSize(26);
                var historical = new TreeMap<>(map);
                historical.put("backend/README.md", List.of("fdcf813e4886d0fa68f7170207944457d331a51b79a0ae664485240f37ee5d64", "bf152e7284aeeb5bc3cd9457c433607a8b91be848e9946d8549051cff1507177"));
                String plan = java.nio.file.Files.readString(root.resolve(planPath), charset).replace("\r\n","\n");
                var table = new TreeMap<String, List<String>>();
                var tableRows = java.util.regex.Pattern.compile("(?m)^\\| `([^`]+)` \\| `([0-9a-f]{64})` \\| `([0-9a-f]{64})` \\|$").matcher(plan);
                int tableCount = 0;
                while (tableRows.find()) {
                    tableCount++;
                    assertThat(table.put(tableRows.group(1), List.of(tableRows.group(2), tableRows.group(3)))).isNull();
                }
                var bare = new TreeMap<String, String>();
                var bareRows = java.util.regex.Pattern.compile("(?m)^([0-9a-f]{64})  (.+)$").matcher(plan);
                int bareCount = 0;
                while (bareRows.find()) {
                    bareCount++;
                    assertThat(bare.put(bareRows.group(2), bareRows.group(1))).isNull();
                }
                assertThat(tableCount).isEqualTo(26);
                assertThat(bareCount).isEqualTo(26);
                assertThat(table.keySet()).isEqualTo(map.keySet());
                assertThat(bare.keySet()).isEqualTo(map.keySet());
                assertThat(table).isEqualTo(historical);
                var historicalBinary = new java.io.ByteArrayOutputStream();
                for (var entry : historical.entrySet()) {
                    assertThat(bare.get(entry.getKey())).isEqualTo(entry.getValue().get(0));
                    historicalBinary.writeBytes(entry.getKey().getBytes(charset)); historicalBinary.write(0);
                    historicalBinary.writeBytes(HexFormat.of().parseHex(entry.getValue().get(0)));
                    historicalBinary.writeBytes(HexFormat.of().parseHex(entry.getValue().get(1)));
                }
                assertThat(sha256.apply(historicalBinary.toByteArray())).isEqualTo("ed03b90fe748039ca7d1d674c32cf0e0c56b828901116294a1ea5dd011954409");
                var binary = new java.io.ByteArrayOutputStream();
                var hexText = new java.io.ByteArrayOutputStream();
                var extraDelimiter = new java.io.ByteArrayOutputStream();
                var rowTerminator = new java.io.ByteArrayOutputStream();
                for (var entry : map.entrySet()) {
                    String path = entry.getKey();
                    String raw = entry.getValue().get(0), canonical = entry.getValue().get(1);
                    byte[] bytes = root.equals(java.nio.file.Path.of("..")) ? historicalBytes.apply(path) : java.nio.file.Files.readAllBytes(root.resolve(path));
                    String actualRaw = sha256.apply(bytes);
                    String actualLf = sha256.apply(new String(bytes, charset).replace("\r\n", "\n").getBytes(charset));
                    assertThat(actualRaw.equals(raw) || actualRaw.equals(canonical) || actualLf.equals(canonical)).isTrue();
                    assertThat(actualLf).as(path).isEqualTo(canonical);
                    byte[] pathBytes = path.getBytes(charset), rawBytes = HexFormat.of().parseHex(raw), lfBytes = HexFormat.of().parseHex(canonical);
                    binary.writeBytes(pathBytes); binary.write(0); binary.writeBytes(rawBytes); binary.writeBytes(lfBytes);
                    hexText.writeBytes(pathBytes); hexText.write(0); hexText.writeBytes(raw.getBytes(charset)); hexText.writeBytes(canonical.getBytes(charset));
                    extraDelimiter.writeBytes(pathBytes); extraDelimiter.write(0); extraDelimiter.writeBytes(rawBytes); extraDelimiter.write(0); extraDelimiter.writeBytes(lfBytes);
                    rowTerminator.writeBytes(pathBytes); rowTerminator.write(0); rowTerminator.writeBytes(rawBytes); rowTerminator.writeBytes(lfBytes); rowTerminator.write('\n');
                }
                assertThat(sha256.apply(binary.toByteArray())).isEqualTo(expectedDigest);
                for (var incorrect : List.of(hexText, extraDelimiter, rowTerminator)) assertThat(sha256.apply(incorrect.toByteArray())).isNotEqualTo(expectedDigest);
                var migrations = map.keySet().stream().filter(path -> path.startsWith("backend/src/main/resources/db/migration/")).map(path -> java.nio.file.Path.of(path).getFileName().toString()).sorted().toList();
                assertThat(migrations).hasSize(18);
                for (int index = 0; index < 18; index++) assertThat(migrations.get(index)).startsWith("V%03d__".formatted(index + 1));
                try (var files = java.nio.file.Files.list(root.resolve("backend/src/main/resources/db/migration"))) {
                    assertThat(files.map(path -> path.getFileName().toString()).filter(name -> !root.equals(java.nio.file.Path.of("..")) || !name.equals("V019__customer_catalog_offer_read_privileges.sql")).sorted().toList()).isEqualTo(migrations);
                }
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        };
        verifyFrozen.accept(java.nio.file.Path.of(".."), frozen);
        var temporary = java.nio.file.Files.createTempDirectory("pa35-frozen-oracle-");
        try {
            String originalPlan = java.nio.file.Files.readString(java.nio.file.Path.of("..").resolve(planPath), charset).replace("\r\n","\n");
            var planCopy = temporary.resolve(planPath);
            java.nio.file.Files.createDirectories(planCopy.getParent());
            java.nio.file.Files.writeString(planCopy, originalPlan, charset);
            for (boolean crlf : List.of(false, true)) {
                for (String row : frozen.strip().lines().toList()) {
                    String path = row.split(" ")[0];
                    String canonical = new String(historicalBytes.apply(path), charset).replace("\r\n", "\n");
                    var destination = temporary.resolve(path);
                    java.nio.file.Files.createDirectories(destination.getParent());
                    java.nio.file.Files.writeString(destination, crlf ? canonical.replace("\n", "\r\n") : canonical, charset);
                }
                verifyFrozen.accept(temporary, frozen);
            }
            String[] first = frozen.strip().lines().findFirst().orElseThrow().split(" ");
            var input = temporary.resolve(first[0]);
            byte[] original = java.nio.file.Files.readAllBytes(input);
            byte[] mutated = original.clone(); mutated[0] ^= 1;
            java.nio.file.Files.write(input, mutated);
            assertThatThrownBy(() -> verifyFrozen.accept(temporary, frozen)).isInstanceOf(AssertionError.class);
            var bom = new java.io.ByteArrayOutputStream(); bom.writeBytes(new byte[] {(byte) 0xef, (byte) 0xbb, (byte) 0xbf}); bom.writeBytes(original);
            java.nio.file.Files.write(input, bom.toByteArray());
            assertThatThrownBy(() -> verifyFrozen.accept(temporary, frozen)).isInstanceOf(AssertionError.class);
            java.nio.file.Files.write(input, original);
            String changedHash = "0".repeat(64);
            String[] readme = frozen.strip().lines().filter(row -> row.startsWith("backend/README.md ")).findFirst().orElseThrow().split(" ");
            for (int pin : List.of(1, 2)) {
                assertThatThrownBy(() -> verifyFrozen.accept(temporary, frozen.replace(readme[pin], changedHash))).isInstanceOf(AssertionError.class);
            }
            String historicalReadmeRaw = "fdcf813e4886d0fa68f7170207944457d331a51b79a0ae664485240f37ee5d64";
            String historicalReadmeLf = "bf152e7284aeeb5bc3cd9457c433607a8b91be848e9946d8549051cff1507177";
            String readmeTable = "| `backend/README.md` | `" + historicalReadmeRaw + "` | `" + historicalReadmeLf + "` |";
            for (String drift : List.of(
                    originalPlan.replace(readmeTable, readmeTable.replace(historicalReadmeRaw, changedHash)),
                    originalPlan.replace(readmeTable, readmeTable.replace(historicalReadmeLf, changedHash)),
                    originalPlan.replace(historicalReadmeRaw + "  backend/README.md", changedHash + "  backend/README.md"))) {
                java.nio.file.Files.writeString(planCopy, drift, charset);
                assertThatThrownBy(() -> verifyFrozen.accept(temporary, frozen)).isInstanceOf(AssertionError.class);
            }
            java.nio.file.Files.writeString(planCopy, originalPlan, charset);
            assertThatThrownBy(() -> verifyFrozen.accept(temporary, frozen.replace(first[1], changedHash))).isInstanceOf(AssertionError.class);
            String tableLine = "| `" + first[0] + "` | `" + first[1] + "` | `" + first[2] + "` |";
            String bareLine = first[1] + "  " + first[0];
            for (String drift : List.of(
                    originalPlan.replace(tableLine, tableLine.replace(first[1], changedHash)),
                    originalPlan.replace(bareLine, changedHash + "  " + first[0]),
                    originalPlan + "\n" + changedHash + "  extra-path\n",
                    originalPlan.replace(bareLine + "\n", ""),
                    originalPlan + "\n" + bareLine + "\n",
                    originalPlan + "\n" + tableLine + "\n",
                    originalPlan.replace(tableLine + "\n", ""),
                    originalPlan + "\n| `extra-path` | `" + changedHash + "` | `" + changedHash + "` |\n")) {
                java.nio.file.Files.writeString(planCopy, drift, charset);
                assertThatThrownBy(() -> verifyFrozen.accept(temporary, frozen)).isInstanceOf(AssertionError.class);
            }
            java.nio.file.Files.writeString(planCopy, originalPlan, charset);
            for (String drift : List.of(frozen + "\nextra-path " + first[1] + " " + first[2], frozen.replace(frozen.strip().lines().findFirst().orElseThrow(), ""), frozen + "\n" + frozen.strip().lines().findFirst().orElseThrow())) {
                assertThatThrownBy(() -> verifyFrozen.accept(temporary, drift)).isInstanceOf(AssertionError.class);
            }
            java.nio.file.Files.delete(input);
            assertThatThrownBy(() -> verifyFrozen.accept(temporary, frozen)).isInstanceOf(java.io.UncheckedIOException.class);
            java.nio.file.Files.write(input, original);
            java.nio.file.Files.writeString(temporary.resolve("backend/src/main/resources/db/migration/V019__unexpected.sql"), "unexpected", charset);
            assertThatThrownBy(() -> verifyFrozen.accept(temporary, frozen)).isInstanceOf(AssertionError.class);
            java.nio.file.Files.delete(temporary.resolve("backend/src/main/resources/db/migration/V019__unexpected.sql"));
            verifyFrozen.accept(temporary, frozen);
        } finally {
            try (var paths = java.nio.file.Files.walk(temporary)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) java.nio.file.Files.delete(path);
            }
        }
        try (var paths = java.nio.file.Files.walk(java.nio.file.Path.of("src/main/java"))) {
            for (var path : paths.filter(path -> path.toString().endsWith(".java")).toList()) assertThat(java.nio.file.Files.readString(path)).as(path.toString()).doesNotContain("implements EmployeeSessionVerifier");
        }
        assertThat(source("operations_iam/internal/web/OperationsBoundaryFilter.java")).contains("authenticator.authenticate(request.getHeaders(\"Authorization\"))");

        var currentPins = new TreeMap<String, List<String>>((left,right) -> Arrays.compareUnsigned(left.getBytes(charset),right.getBytes(charset)));
        for (String row : frozen.strip().lines().toList()) { String[] fields = row.split(" "); assertThat(currentPins.put(fields[0],List.of(fields[2],fields[2]))).isNull(); }
        currentPins.put("backend/ops/postgresql/pa25-runtime-identities.sql", List.of("ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47", "ae0261bf90f916323bd387ca5dc314ab425576b6a17560408e9bf9fee2db7e47"));
        currentPins.put("docs/operations/pa25-production-database-identity-provisioning-runbook.md", List.of("a74827f87821353365cf48b0f5ebb794a902ca0ee4710229f057078a0f2d7430", "a74827f87821353365cf48b0f5ebb794a902ca0ee4710229f057078a0f2d7430"));
        currentPins.put("backend/README.md", List.of("9c094a00f691fd8849363355a12c174cc289ce57841a63d0f25d04f8a30faf7b", "9c094a00f691fd8849363355a12c174cc289ce57841a63d0f25d04f8a30faf7b"));
        currentPins.put("backend/src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql", List.of("32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e", "32fc494347cb88acca8b8f3d882cef5d357d76195e501df45cad056f9b50659e"));
        assertThat(currentPins).hasSize(27);
        StringBuilder currentTable = new StringBuilder(), currentBare = new StringBuilder();
        for (var entry : currentPins.entrySet()) {
            currentTable.append("| `").append(entry.getKey()).append("` | `").append(entry.getValue().get(0)).append("` | `").append(entry.getValue().get(1)).append("` |\n");
            currentBare.append(entry.getValue().get(0)).append("  ").append(entry.getKey()).append('\n');
        }
        java.util.function.BiConsumer<String,String> verifyCurrentRepresentations = (tableText,bareText) -> {
            var table = new TreeMap<String,List<String>>(); var bare = new TreeMap<String,String>();
            int currentTableCount = 0, currentBareCount = 0;
            var tableRows = java.util.regex.Pattern.compile("(?m)^\\| `([^`]+)` \\| `([0-9a-f]{64})` \\| `([0-9a-f]{64})` \\|$").matcher(tableText);
            while(tableRows.find()) { currentTableCount++; assertThat(table.put(tableRows.group(1),List.of(tableRows.group(2),tableRows.group(3)))).isNull(); }
            var bareRows = java.util.regex.Pattern.compile("(?m)^([0-9a-f]{64})  (.+)$").matcher(bareText);
            while(bareRows.find()) { currentBareCount++; assertThat(bare.put(bareRows.group(2),bareRows.group(1))).isNull(); }
            assertThat(currentTableCount).isEqualTo(27); assertThat(currentBareCount).isEqualTo(27);
            assertThat(table.keySet()).isEqualTo(currentPins.keySet()); assertThat(bare.keySet()).isEqualTo(currentPins.keySet()); assertThat(table).isEqualTo(currentPins);
            for(var entry:currentPins.entrySet()) assertThat(bare.get(entry.getKey())).isEqualTo(entry.getValue().get(0));
        };
        verifyCurrentRepresentations.accept(currentTable.toString(),currentBare.toString());
        for(String text:List.of(currentTable+currentTable.toString().lines().findFirst().orElseThrow()+"\n", currentTable.toString().substring(currentTable.indexOf("\n")+1), currentTable+"| `fake-path` | `"+"0".repeat(64)+"` | `"+"0".repeat(64)+"` |\n", currentTable.toString().replaceFirst("[0-9a-f]{64}","0".repeat(64)))) assertThatThrownBy(() -> verifyCurrentRepresentations.accept(text,currentBare.toString())).isInstanceOf(AssertionError.class);
        for(String text:List.of(currentBare+currentBare.toString().lines().findFirst().orElseThrow()+"\n", currentBare.toString().substring(currentBare.indexOf("\n")+1), currentBare+"0".repeat(64)+"  fake-path\n", currentBare.toString().replaceFirst("[0-9a-f]{64}","0".repeat(64)))) assertThatThrownBy(() -> verifyCurrentRepresentations.accept(currentTable.toString(),text)).isInstanceOf(AssertionError.class);
        var currentBinary = new java.io.ByteArrayOutputStream();
        for(var entry:currentPins.entrySet()) { currentBinary.writeBytes(entry.getKey().getBytes(charset)); currentBinary.write(0); currentBinary.writeBytes(HexFormat.of().parseHex(entry.getValue().get(0))); currentBinary.writeBytes(HexFormat.of().parseHex(entry.getValue().get(1))); }
        assertThat(sha256.apply(currentBinary.toByteArray())).isEqualTo("58161cad19014b05911bb068d8cd120a7f4a3630c9f6c2c98b7a3883132cef98");
        java.util.function.Consumer<java.nio.file.Path> verifyCurrentBytes = root -> {
            try {
                for(var entry:currentPins.entrySet()) {
                    byte[] bytes=java.nio.file.Files.readAllBytes(root.resolve(entry.getKey()));
                    String actualLf=sha256.apply(new String(bytes,charset).replace("\r\n","\n").getBytes(charset));
                    assertThat(actualLf).as(entry.getKey()).isEqualTo(entry.getValue().get(0)).isEqualTo(entry.getValue().get(1));
                }
                var expectedNames=currentPins.keySet().stream().filter(name -> name.startsWith("backend/src/main/resources/db/migration/")).map(name -> java.nio.file.Path.of(name).getFileName().toString()).sorted().toList();
                assertThat(expectedNames).hasSize(19);
                try(var paths=java.nio.file.Files.list(root.resolve("backend/src/main/resources/db/migration"))) { assertThat(paths.map(path -> path.getFileName().toString()).sorted().toList()).isEqualTo(expectedNames); }
            } catch(java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        };
        verifyCurrentBytes.accept(java.nio.file.Path.of(".."));
        var currentProjection=java.nio.file.Files.createTempDirectory("pa40a-clean-checkout27-");
        try {
            for(boolean crlf:List.of(false,true)) {
                for(var entry:currentPins.entrySet()) { var destination=currentProjection.resolve(entry.getKey()); java.nio.file.Files.createDirectories(destination.getParent()); String text=java.nio.file.Files.readString(java.nio.file.Path.of("..").resolve(entry.getKey()),charset).replace("\r\n","\n"); java.nio.file.Files.writeString(destination,crlf?text.replace("\n","\r\n"):text,charset); }
                verifyCurrentBytes.accept(currentProjection);
            }
            for(String changed:currentPins.keySet()) {
                var destination=currentProjection.resolve(changed); byte[] original=java.nio.file.Files.readAllBytes(destination); byte[] corruption=original.clone(); corruption[0]^=1; java.nio.file.Files.write(destination,corruption); assertThatThrownBy(() -> verifyCurrentBytes.accept(currentProjection)).isInstanceOf(AssertionError.class); java.nio.file.Files.write(destination,original);
            }
            var migration=currentProjection.resolve("backend/src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql"); byte[] original=java.nio.file.Files.readAllBytes(migration);
            java.nio.file.Files.delete(migration); assertThatThrownBy(() -> verifyCurrentBytes.accept(currentProjection)).isInstanceOf(java.io.UncheckedIOException.class); java.nio.file.Files.write(migration,original);
            for(String name:List.of("V020__unexpected.sql","V019__unexpected.sql")) { var hostile=migration.resolveSibling(name); java.nio.file.Files.writeString(hostile,"unexpected"); assertThatThrownBy(() -> verifyCurrentBytes.accept(currentProjection)).isInstanceOf(AssertionError.class); java.nio.file.Files.delete(hostile); }
            java.nio.file.Files.move(migration,migration.resolveSibling("V019__unexpected.sql")); assertThatThrownBy(() -> verifyCurrentBytes.accept(currentProjection)).isInstanceOf(java.io.UncheckedIOException.class);
        } finally { try(var paths=java.nio.file.Files.walk(currentProjection)) { for(var path:paths.sorted(Comparator.reverseOrder()).toList()) java.nio.file.Files.delete(path); } }
    }
}
