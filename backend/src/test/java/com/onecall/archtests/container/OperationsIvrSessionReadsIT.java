package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.*;
import com.onecall.core.operations_iam.internal.ivrread.*;
import com.onecall.core.operations_iam.internal.subjectread.*;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.time.*;
import java.util.*;
import java.sql.*;
import java.util.concurrent.atomic.*;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.*;


class OperationsIvrSessionReadsIT {
    static final OperationsIvrSessionReadFixture fixture = new OperationsIvrSessionReadFixture();
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @AfterEach void clean() throws Exception { fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]); fixture.clean(); }
    private String detail(UUID session) { return BASE + "/" + session; }
    private List<String> ids(tools.jackson.databind.JsonNode page) { var result = new ArrayList<String>(); page.path("items").forEach(item -> result.add(item.path("sessionId").asString())); return result; }
    private void explains(UUID session) throws Exception {
        try (var connection = fixture.admin()) {
            for (String sql : List.of(
                    "SELECT s.id FROM ivr.inbound_session s ORDER BY s.created_at DESC,s.id DESC LIMIT 21",
                    "SELECT s.id FROM ivr.inbound_session s WHERE s.status='RECEIVED' ORDER BY s.created_at DESC,s.id DESC LIMIT 21",
                    "SELECT s.id FROM ivr.inbound_session s WHERE s.subject_id='00000000-0000-0000-0000-000000000001' ORDER BY s.created_at DESC,s.id DESC LIMIT 21",
                    "SELECT id FROM ivr.ivr_decision WHERE session_id='" + session + "' OR call_id='call-" + session + "' ORDER BY decided_at,id",
                    "SELECT id FROM evidence.evidence_record WHERE session_id='" + session + "' OR call_id='call-" + session + "' ORDER BY id")) {
                var rows = new ArrayList<String>(); try (var statement = connection.createStatement(); var result = statement.executeQuery("EXPLAIN " + sql)) { while (result.next()) rows.add(result.getString(1)); }
                assertThat(rows).isNotEmpty(); System.out.println("PA35 EXPLAIN " + String.join(" | ", rows));
            }
        }
    }

    private void listen(java.util.function.Consumer<ExecuteContext> callback) { fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeEnd(ExecuteContext context) { callback.accept(context); } }); }
    @Test
    void postgres16UsesExactlyFrozenEighteenMigrations() throws Exception {
        String frozen = """
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
        var charset = java.nio.charset.StandardCharsets.UTF_8;
        String planPath = "docs/gateflow/pa35-operations-ivr-session-read-foundation-plan.md";
        String expectedDigest = "afda283795031bd226b3d48db7b9384ffacc5592798cd410769c9ec7ff8bc761";
        java.util.function.Function<byte[], String> sha256 = bytes -> {
            try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
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
                String plan = java.nio.file.Files.readString(root.resolve(planPath), charset);
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
                    byte[] bytes = java.nio.file.Files.readAllBytes(root.resolve(path));
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
                    assertThat(files.map(path -> path.getFileName().toString()).sorted().toList()).isEqualTo(migrations);
                }
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        };
        verifyFrozen.accept(java.nio.file.Path.of(".."), frozen);
        var temporary = java.nio.file.Files.createTempDirectory("pa35-frozen-oracle-");
        try {
            String originalPlan = java.nio.file.Files.readString(java.nio.file.Path.of("..").resolve(planPath), charset);
            var planCopy = temporary.resolve(planPath);
            java.nio.file.Files.createDirectories(planCopy.getParent());
            java.nio.file.Files.writeString(planCopy, originalPlan, charset);
            for (boolean crlf : List.of(false, true)) {
                for (String row : frozen.strip().lines().toList()) {
                    String path = row.split(" ")[0];
                    String canonical = java.nio.file.Files.readString(java.nio.file.Path.of("..").resolve(path), charset).replace("\r\n", "\n");
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
        try (var connection = fixture.admin()) {
            assertThat(Integer.parseInt(one(connection, "SHOW server_version_num"))).isBetween(160000, 169999);
            assertThat(one(connection, "SHOW server_encoding")).isEqualTo("UTF8");
            assertThat(one(connection, "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL")).isEqualTo("18");
            assertThat(one(connection, "SELECT checksum FROM flyway_schema_history WHERE version='018' AND success")).isEqualTo("1606293398");
            var migrations = frozen.strip().lines().map(row -> row.split(" ")[0]).filter(path -> path.startsWith("backend/src/main/resources/db/migration/")).sorted().toList();
            try (var statement = connection.createStatement(); var history = statement.executeQuery("SELECT installed_rank, version, description, type, script, checksum, success FROM flyway_schema_history ORDER BY installed_rank")) {
                for (int index = 0; index < 18; index++) {
                    assertThat(history.next()).isTrue();
                    var migration = java.nio.file.Path.of("..").resolve(migrations.get(index));
                    String name = migration.getFileName().toString();
                    var checksum = new java.util.zip.CRC32();
                    for (String line : java.nio.file.Files.readAllLines(migration, charset)) checksum.update(line.getBytes(charset));
                    assertThat(history.getInt("installed_rank")).isEqualTo(index + 1);
                    assertThat(history.getString("version")).isEqualTo("%03d".formatted(index + 1));
                    assertThat(history.getString("description")).isEqualTo(name.substring(6, name.length() - 4).replace('_', ' '));
                    assertThat(history.getString("type")).isEqualTo("SQL");
                    assertThat(history.getString("script")).isEqualTo(name);
                    assertThat(history.getInt("checksum")).isEqualTo((int) checksum.getValue());
                    assertThat(history.getBoolean("success")).isTrue();
                }
                assertThat(history.next()).isFalse();
            }
        }
    }
    @Test
    void realHttpActiveAdminAndOpsReturnExactList() throws Exception {
        UUID session = fixture.session(1); for (String token : List.of("admin", "ops")) { var response = fixture.request("GET", BASE, token); assertThat(response.statusCode()).isEqualTo(200); assertThat(ids(json(response))).containsExactly(session.toString()); assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore"); }
    }
    @Test
    void realHttpDetailReturnsExactSafeChain() throws Exception {
        UUID session = fixture.session(1); fixture.decision(session); fixture.leg(session, "leg", fixture.time); fixture.event(session, "event", 1); UUID evidence = fixture.evidence(session, new UUID(0, 1)); fixture.artifact(evidence, "ORIGINAL"); var body = fixture.get(detail(session)); assertThat(body.propertyNames()).containsExactlyInAnyOrder("sessionId", "callId", "status", "nodeId", "protocolVersion", "promptVersion", "accessNumberMasked", "createdAt", "call", "decisions", "legs", "callEvents", "evidence"); assertThat(body.path("decisions")).hasSize(1); assertThat(body.path("evidence").get(0).path("artifactRoles").get(0).asString()).isEqualTo("ORIGINAL"); assertThat(body.toString()).doesNotContain("PRIVATE", "ciphertext", "snapshot", "objectKey");
    }
    @Test
    void realHttpAuthenticationAuthorityAndAudiencePrecedenceAreExact() throws Exception {
        for (String path : List.of(BASE + "?limit=bad", BASE + "/bad")) { problem(fixture.request("GET", path, null), 401, "UNAUTHENTICATED"); problem(fixture.request("GET", path, "wrong"), 401, "UNAUTHENTICATED"); problem(fixture.request("GET", path, "missing"), 401, "UNAUTHENTICATED"); problem(fixture.request("GET", path, "disabled"), 403, "FORBIDDEN"); }
        fixture.mutate("ALTER TABLE operations_iam.employee_account DROP CONSTRAINT ck_employee_revision"); try { fixture.mutate("UPDATE operations_iam.employee_account SET revision=0 WHERE id=?", fixture.disabled); problem(fixture.request("GET", BASE + "?limit=bad", "disabled"), 503, "SERVICE_UNAVAILABLE"); } finally { fixture.mutate("UPDATE operations_iam.employee_account SET revision=1 WHERE id=?", fixture.disabled); fixture.mutate("ALTER TABLE operations_iam.employee_account ADD CONSTRAINT ck_employee_revision CHECK (revision>=1)"); }
    }
    @Test
    void realHttpMethodsAndNoncanonicalVariantsStayConcealed() throws Exception {
        for (String path : List.of(BASE, detail(new UUID(0, 1)))) { for (String method : List.of("POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")) { var response = fixture.request(method, path, null); if (method.equals("HEAD")) assertThat(response.statusCode()).isEqualTo(404); else { assertThat(response.statusCode()).as(method + " " + path + " " + response.body()).isEqualTo(404); problem(response, 404, "NOT_FOUND"); } } problem(fixture.request("TRACE", path, null), 500, "INTERNAL"); }
        for (String path : List.of(BASE + "/", BASE + "/x/y", BASE + ";x", BASE + "/%31", BASE + "%2f", "/api//v1/operations/ivr-sessions")) { var response = fixture.request("GET", path, null); assertThat(response.statusCode()).as(path + " " + response.body()).isEqualTo(404); problem(response, 404, "NOT_FOUND"); }
    }
    @Test
    void realHttpParameterDefaultsDuplicatesAndBoundsAreExact() throws Exception {
        fixture.session(1); assertThat(fixture.get(BASE).path("items")).hasSize(1); for (String query : List.of("limit=0", "limit=101", "limit=bad", "limit=1&limit=1", "unknown=x", "subjectId=bad", "status=received")) problem(fixture.request("GET", BASE + "?" + query, "admin"), 422, "VALIDATION_FAILED"); assertThat(fixture.get(BASE + "?limit=001").path("items")).hasSize(1);
    }
    @Test
    void subjectFilterMatchesOnlyExactSessionSubject() throws Exception {
        UUID subject = fixture.subject(); UUID expected = fixture.session(new UUID(0, 1), fixture.time, subject, "RECEIVED", true); fixture.session(2); assertThat(ids(fixture.get(BASE + "?subjectId=" + subject))).containsExactly(expected.toString());
    }
    @Test
    void callFilterMatchesOnlyExactCallIdentity() throws Exception {
        UUID expected = fixture.session(1); fixture.session(2); assertThat(ids(fixture.get(BASE + "?callId=call-" + expected))).containsExactly(expected.toString()); assertThat(ids(fixture.get(BASE + "?callId=CALL-" + expected))).isEmpty();
    }
    @Test
    void taskFilterRequiresPersistedAssociationAndNeverSilentlyDropsPredicate() throws Exception {
        UUID expected = fixture.session(1); fixture.task(expected, "public-task-not-a-uuid"); explains(expected); fixture.session(2); fixture.session(new UUID(0, 3), fixture.time.minusSeconds(3), null, "RECEIVED", false);
        assertThat(ids(fixture.get(BASE))).hasSize(3); assertThat(ids(fixture.get(BASE + "?taskId=public-task-not-a-uuid"))).containsExactly(expected.toString()); assertThat(ids(fixture.get(BASE + "?taskId=not-matching"))).isEmpty();
        try (var connection = fixture.admin()) { String explain = one(connection, "EXPLAIN SELECT s.id FROM ivr.inbound_session s WHERE EXISTS (SELECT 1 FROM ivr.call_record c JOIN ivr.outbound_call_task t ON t.id=c.task_id WHERE c.session_id=s.id AND t.task_id='public-task-not-a-uuid') ORDER BY s.created_at DESC,s.id DESC LIMIT 21"); assertThat(explain).isNotBlank(); System.out.println("PA35 task EXPLAIN: " + explain); }
    }
    @Test
    void statusFilterMatchesAllFourFrozenStatuses() throws Exception {
        int index = 1; for (String status : List.of("RECEIVED", "CONFIRMED", "ABANDONED", "REJECTED")) { UUID id = fixture.session(new UUID(0, index++), fixture.time, fixture.subject(), status, true); assertThat(ids(fixture.get(BASE + "?status=" + status))).containsExactly(id.toString()); }
    }
    @Test
    void combinedFiltersAndPresenceAwareDigestRemainExact() throws Exception {
        UUID subject = fixture.subject(); UUID session = fixture.session(new UUID(0, 1), fixture.time, subject, "RECEIVED", true); fixture.task(session, "task-exact"); fixture.session(2); String query = "subjectId=" + subject + "&callId=call-" + session + "&taskId=task-exact&status=RECEIVED"; assertThat(ids(fixture.get(BASE + "?" + query))).containsExactly(session.toString()); assertThat(ids(fixture.get(BASE + "?" + query.replace("task-exact", "other")))).isEmpty();
    }
    @Test
    void emptySingleAndExactLimitPagesHaveNullCursor() throws Exception {
        assertThat(fixture.get(BASE).path("nextCursor").isNull()).isTrue(); fixture.session(1); assertThat(fixture.get(BASE + "?limit=1").path("nextCursor").isNull()).isTrue(); fixture.session(2); var page = fixture.get(BASE + "?limit=2"); assertThat(page.path("hasMore").asBoolean()).isFalse(); assertThat(page.path("nextCursor").isNull()).isTrue();
    }
    @Test
    void lookaheadAndTailUseOnlyLastReturnedAnchor() throws Exception {
        UUID first = fixture.session(1), second = fixture.session(2), third = fixture.session(3); var page = fixture.get(BASE + "?limit=2"); assertThat(ids(page)).containsExactly(first.toString(), second.toString()); assertThat(page.path("hasMore").asBoolean()).isTrue(); var tail = fixture.get(BASE + "?limit=2&cursor=" + page.path("nextCursor").asString()); assertThat(ids(tail)).containsExactly(third.toString()); assertThat(tail.path("nextCursor").isNull()).isTrue();
    }
    @Test
    void timestampTiesAndHighBitUuidsContinueWithoutDuplicates() throws Exception {
        var expected = List.of(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), UUID.fromString("80000000-0000-0000-0000-000000000000"), new UUID(0, 1)); for (UUID id : expected) fixture.session(id, fixture.time, null, "RECEIVED", true); var actual = new ArrayList<String>(); String path = BASE + "?limit=1"; while (true) { var page = fixture.get(path); actual.addAll(ids(page)); if (!page.path("hasMore").asBoolean()) break; path = BASE + "?limit=1&cursor=" + page.path("nextCursor").asString(); } assertThat(actual).containsExactlyElementsOf(expected.stream().map(UUID::toString).toList());
    }
    @Test
    void changingLimitReusesCursorWithoutMismatch() throws Exception {
        fixture.session(1); UUID second = fixture.session(2), third = fixture.session(3); String cursor = fixture.get(BASE + "?limit=1").path("nextCursor").asString(); assertThat(ids(fixture.get(BASE + "?limit=100&cursor=" + cursor))).containsExactly(second.toString(), third.toString());
    }
    @Test
    void realHttpMalformedTamperedWrongPurposeAndFilterMismatchAre400() throws Exception {
        fixture.session(1); fixture.session(2); String cursor = fixture.get(BASE + "?limit=1").path("nextCursor").asString(); for (String token : List.of("!", "", cursor.substring(0, cursor.length() - 2))) problem(fixture.request("GET", BASE + "?cursor=" + token, "admin"), 400, "CURSOR_INVALID"); problem(fixture.request("GET", BASE + "?cursor=" + cursor + "&status=REJECTED", "admin"), 400, "CURSOR_FILTER_MISMATCH"); String subjectCursor = new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectCursorCodec(com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders.properties()).encode(new OperationsSubjectReadPorts.Anchor(fixture.time, fixture.employee), new byte[32]); problem(fixture.request("GET", BASE + "?cursor=" + subjectCursor, "admin"), 400, "CURSOR_INVALID");
    }
    @Test
    void sessionOptionalNullsAreOmittedAndGuidanceIsNotInferred() throws Exception {
        UUID session = fixture.session(1); fixture.decision(session); var body = fixture.get(detail(session)); for (String key : List.of("subjectId", "guidanceStatus", "callerMasked", "confirmedAt")) assertThat(body.has(key)).isFalse(); fixture.mutate("UPDATE ivr.inbound_session SET guidance_status='CONSENT_REQUIRED',revision=revision+1 WHERE id=?", session); assertThat(fixture.get(detail(session)).path("guidanceStatus").asString()).isEqualTo("CONSENT_REQUIRED");
    }
    @Test
    void callAndDecisionRequiredNullableAndOptionalFieldsAreExact() throws Exception {
        UUID session = fixture.session(1); var call = fixture.get(detail(session)).path("call"); assertThat(call.propertyNames()).containsExactly("status"); fixture.decision(session); var decision = fixture.get(detail(session)).path("decisions").get(0); assertThat(decision.has("action")).isTrue(); assertThat(decision.has("reservationId")).isFalse(); fixture.directDecision(session); var direct = fixture.get(detail(session)).path("decisions").get(1); assertThat(direct.path("action").isNull()).isTrue(); assertThat(direct.has("action")).isTrue(); assertThat(direct.has("reservationId")).isTrue(); fixture.mutate("UPDATE ivr.call_record SET status='ENDED',ended_at=?::timestamptz,authoritative_seconds=0,revision=revision+1 WHERE session_id=?", fixture.time.toString(), session); assertThat(fixture.get(detail(session)).path("call").path("authoritativeSeconds").asInt()).isZero();
    }
    @Test
    void legsEventsAndEvidenceHaveDeterministicOrdering() throws Exception {
        UUID session = fixture.session(1); fixture.leg(session, "z", fixture.time); fixture.leg(session, "a", fixture.time); fixture.event(session, "z", null); fixture.event(session, "b", 2); fixture.event(session, "a", 1); UUID second = fixture.evidence(session, new UUID(0, 2)), first = fixture.evidence(session, new UUID(0, 1)); fixture.artifact(first, "DERIVED"); fixture.artifact(first, "ORIGINAL"); fixture.artifact(first, "DERIVED"); var body = fixture.get(detail(session)); assertThat(body.path("legs").get(0).path("legId").asString()).isEqualTo("a"); assertThat(body.path("callEvents").get(2).path("eventId").asString()).isEqualTo("z"); assertThat(body.path("evidence").get(0).path("evidenceId").asString()).isEqualTo(first.toString()); assertThat(body.path("evidence").get(0).path("artifactRoles")).hasSize(2); assertThat(body.path("evidence").get(1).path("artifactRoles")).isEmpty();
        for (int count : List.of(0, 1, 1000, 1001, 2000)) {
            fixture.bulkEvidence(session, count); AtomicInteger chunks = new AtomicInteger();
            listen(execution -> { if (execution.sql().contains("FROM evidence.evidence_artifact")) chunks.incrementAndGet(); });
            assertThat(fixture.get(detail(session)).path("evidence")).hasSize(count); assertThat(chunks.get()).isEqualTo((count + 999) / 1000);
            fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]);
        }
    }
    @Test
    void missingRootIs404ButMissingCallAndInconsistentLinksAre503() throws Exception {
        problem(fixture.request("GET", detail(UUID.randomUUID()), "admin"), 404, "NOT_FOUND"); UUID session = fixture.session(new UUID(0, 1), fixture.time, null, "RECEIVED", false); problem(fixture.request("GET", detail(session), "admin"), 503, "SERVICE_UNAVAILABLE"); assertThat(ids(fixture.get(BASE))).containsExactly(session.toString());
        UUID first = fixture.session(2), second = fixture.session(3); fixture.evidence(second, new UUID(0, 1), "call-" + first);
        problem(fixture.request("GET", detail(first), "admin"), 503, "SERVICE_UNAVAILABLE");
        fixture.mutate("TRUNCATE evidence.evidence_record CASCADE");
        fixture.mutate("INSERT INTO ivr.ivr_decision(id,session_id,call_id,purpose,request_fingerprint,action,reason_code,response_snapshot,response_payload_ciphertext,response_sha256,key_version) VALUES (?,?,?,'INBOUND_RESOLVE',repeat('a',64),'REJECT','DATA_INTEGRITY','{}','v1.fixture.PRIVATE_CONFLICT',repeat('b',64),1)", UUID.randomUUID(), second, "call-" + first);
        problem(fixture.request("GET", detail(first), "admin"), 503, "SERVICE_UNAVAILABLE");
    }
    @Test
    void concurrentDomainChangesRemainInvisibleWithinSnapshot() throws Exception {
        UUID session = fixture.session(1); AtomicBoolean changed = new AtomicBoolean(); listen(execution -> { if (execution.sql().contains("FROM ivr.inbound_session") && changed.compareAndSet(false, true)) try { fixture.session(2); } catch (Exception failure) { throw new IllegalStateException(failure); } }); assertThat(ids(fixture.get(BASE))).containsExactly(session.toString()); assertThat(ids(fixture.get(BASE))).hasSize(2); assertThat(changed).isTrue();
    }
    @Test
    void concurrentEmployeeDisableUsesSnapshotThenDeniesNextRequest() throws Exception {
        fixture.session(1); AtomicBoolean changed = new AtomicBoolean(); listen(execution -> { if (execution.sql().contains("FROM operations_iam.employee_account") && changed.compareAndSet(false, true)) try { fixture.mutate("UPDATE operations_iam.employee_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.employee); } catch (Exception failure) { throw new IllegalStateException(failure); } }); assertThat(fixture.request("GET", BASE, "admin").statusCode()).isEqualTo(200); problem(fixture.request("GET", BASE, "admin"), 403, "FORBIDDEN");
    }
    @Test
    void dependencyTimeoutCleanupAndUnknownFailuresAreSanitized() throws Exception {
        fixture.session(1); var original = fixture.service; try { for (Code code : Code.values()) { var replacement = mock(OperationsIvrSessionReadService.class); when(replacement.list(any(), any())).thenThrow(new Failure(code)); fixture.replaceService(replacement); var response = fixture.request("GET", BASE, "admin"); assertThat(json(response).path("code").asString()).isEqualTo(code.name()); } } finally { fixture.replaceService(original); }
        for (String mode : List.of("dependency", "unknown", "timeout")) {
            fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(ExecuteContext execution) {
                if (!execution.sql().contains("FROM ivr.inbound_session")) return;
                if (mode.equals("dependency")) throw new org.jooq.exception.DataAccessException("PRIVATE jdbc:");
                if (mode.equals("unknown")) throw new IllegalStateException("PRIVATE_UNKNOWN");
                try (var statement = execution.connection().createStatement()) { statement.execute("SELECT pg_sleep(3)"); }
                catch (SQLException failure) { throw new org.jooq.exception.DataAccessException("PRIVATE_TIMEOUT", failure); }
            } });
            problem(fixture.request("GET", BASE, "admin"), mode.equals("unknown") ? 500 : 503, mode.equals("unknown") ? "INTERNAL" : "SERVICE_UNAVAILABLE");
            fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]); fixture.get(BASE);
        }
    }
    @Test
    void allResponsesLogsAndBusinessAuditOutboxFingerprintsStaySafe() throws Exception {
        UUID session = fixture.session(1); fixture.decision(session); fixture.evidence(session, new UUID(0, 1)); var before = fixture.fingerprints();
        var root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var captured = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>(); captured.start(); root.addAppender(captured);
        try {
            for (String path : List.of(BASE, detail(session), BASE + "?limit=bad", detail(UUID.randomUUID()))) {
                var response = fixture.request("GET", path, "admin"); assertThat(response.body()).doesNotContain("PRIVATE", "ciphertext", "snapshot", "objectKey", "jdbc:", fixture.password, fixture.employee.toString());
            }
            assertThat(captured.list).allSatisfy(event -> assertThat(event.getFormattedMessage()).doesNotContain("PRIVATE", "jdbc:", fixture.password, fixture.employee.toString()));
        } finally { root.detachAppender(captured); captured.stop(); }
        assertThat(fixture.fingerprints()).isEqualTo(before);
    }
}
