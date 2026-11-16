package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryService;
import com.onecall.core.customer_iam.internal.web.CustomerEvidenceDtos;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader;
import com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CustomerEvidenceBoundaryTests {
    private static final Path ROOT = Path.of("src/main/java/com/onecall/core");
    private static final String MIGRATION = "src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql";
    private static final int V017_CHECKSUM = -41822162;
    private static final Set<String> DELTA = Set.of(
            "customer_iam_role|column|evidence.evidence_record|id|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.evidence_record|status|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.evidence_record|consent_version|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.evidence_record|created_at|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.evidence_record|solidified_at|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.media_object|id|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.media_object|sha256|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.media_object|mime_type|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.recording|id|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.recording|media_object_id|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.recording|status|SELECT|evidence_owner",
            "customer_iam_role|column|evidence.recording|validated_duration|SELECT|evidence_owner");

    @Test
    void namedOwnerInterfacesAndTenCustomerDependenciesAreExact() throws Exception {
        var modules = ApplicationModules.of(OneCallApplication.class); modules.verify();
        assertThat(modules.getModuleByName("customer_iam").orElseThrow().getAllowedDependencies(modules).stream()
                .map(dependency -> dependency.getTargetModule().getIdentifier() + "::" + dependency.getTargetNamedInterface().getName()))
                .containsExactlyInAnyOrder("sharing::customer-query", "sharing::customer-command", "sharing::public-query", "sharing::public-admission",
                        "subject_number::customer-business-number-query", "activation_consent::customer-activation-query",
                        "subject_number::customer-subject-query", "evidence::customer-subject-count-query",
                        "catalog_order::customer-subject-count-query", "evidence::customer-evidence-query", "subject_number::customer-subject-claim-command", "activation_consent::customer-subject-activation-command");
        assertThat(CustomerSubjectAccessReader.class.getDeclaredMethods()).hasSize(4);
        signature(CustomerSubjectAccessReader.class, "findOwnedSubject", "java.util.Optional<java.util.UUID>", "java.util.UUID", "java.util.UUID", "java.util.UUID");
        signature(CustomerSubjectAccessReader.class, "hasOpenRisk", "boolean", "java.util.UUID");
        signature(CustomerSubjectAccessReader.class, "findOwnedSubjectIds", "java.util.Set<java.util.UUID>", "java.util.UUID", "java.util.UUID");
        signature(CustomerSubjectAccessReader.class, "hasOpenRisk", "boolean", "java.util.Set<java.util.UUID>");
        String owner = CustomerEvidenceReader.class.getName();
        assertThat(CustomerEvidenceReader.class.getDeclaredMethods()).hasSize(5);
        signature(CustomerEvidenceReader.class, "findSubjectIdsWithEvidence", "java.util.Set<java.util.UUID>", "java.util.Set<java.util.UUID>");
        signature(CustomerEvidenceReader.class, "fetchPage", "java.util.List<" + owner + "$Header>", "java.util.Set<java.util.UUID>", "java.util.Optional<" + owner + "$Anchor>", "int");
        signature(CustomerEvidenceReader.class, "findArtifactRoles", "java.util.Map<java.util.UUID, java.util.List<java.lang.String>>", "java.util.Set<java.util.UUID>");
        signature(CustomerEvidenceReader.class, "findHeader", "java.util.Optional<" + owner + "$Header>", "java.util.UUID");
        signature(CustomerEvidenceReader.class, "findArtifacts", "java.util.List<" + owner + "$Artifact>", "java.util.UUID");
        for (Class<?> seam : List.of(CustomerEvidenceReader.class, CustomerSubjectAccessReader.class)) {
            assertThat(seam.isInterface()).isTrue();
            for (var method : seam.getDeclaredMethods()) assertThat(method.toGenericString())
                    .doesNotContain("org.jooq", "org.springframework", "java.sql", ".internal.", "DataSource", "Principal");
        }
        components(CustomerEvidenceReader.Anchor.class, "createdAt", "evidenceId");
        components(CustomerEvidenceReader.Header.class, "evidenceId", "subjectId", "status", "sourceType", "capturedAtText", "consentVersion", "createdAt", "solidifiedAt");
        components(CustomerEvidenceReader.Artifact.class, "artifactRole", "mimeType", "sizeBytes", "validatedDurationSeconds", "sha256");
        assertThat(Arrays.stream(CustomerEvidenceReader.Anchor.class.getRecordComponents()).map(value -> value.getType().getName()))
                .containsExactly("java.time.Instant", "java.util.UUID");
        assertThat(Arrays.stream(CustomerEvidenceReader.Header.class.getRecordComponents()).map(value -> value.getType().getName()))
                .containsExactly("java.util.UUID", "java.util.UUID", "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String", "java.time.Instant", "java.time.Instant");
        assertThat(Arrays.stream(CustomerEvidenceReader.Artifact.class.getRecordComponents()).map(value -> value.getType().getName()))
                .containsExactly("java.lang.String", "java.lang.String", "java.lang.Long", "java.lang.Integer", "java.lang.String");
        for (var entry : Map.of("subject_number", "customer-subject-query", "evidence", "customer-evidence-query").entrySet()) {
            var named = modules.getModuleByName(entry.getKey()).orElseThrow().getNamedInterfaces().getByName(entry.getValue()).orElseThrow();
            assertThat(named.asJavaClasses().map(value -> value.getName())).noneMatch(value -> value.contains(".internal."));
        }
        assertThat(modules.getModuleByName("subject_number").orElseThrow().getAllowedDependencies(modules).stream()
                .map(dependency -> dependency.getTargetModule().getIdentifier() + "::" + dependency.getTargetNamedInterface().getName()))
                .containsExactly("operations_iam::operations-subject-read");
        assertThat(modules.getModuleByName("evidence").orElseThrow().getAllowedDependencies(modules).stream()
                .map(dependency -> dependency.getTargetModule().getIdentifier()
                        + (dependency.getTargetNamedInterface().isUnnamed() ? "" : "::" + dependency.getTargetNamedInterface().getName())))
                .containsExactlyInAnyOrder("entitlement_metering", "operations_iam::operations-subject-read");
    }

    private void signature(Class<?> owner, String name, String result, String... parameters) {
        var candidates = Arrays.stream(owner.getDeclaredMethods()).filter(method -> method.getName().equals(name))
                .filter(method -> Arrays.equals(Arrays.stream(method.getGenericParameterTypes()).map(Type::getTypeName).toArray(String[]::new), parameters)).toList();
        assertThat(candidates).hasSize(1);
        assertThat(candidates.getFirst().getGenericReturnType().getTypeName()).isEqualTo(result);
    }

    private void components(Class<?> record, String... names) {
        assertThat(record.isRecord()).isTrue();
        assertThat(Arrays.stream(record.getRecordComponents()).map(RecordComponent::getName)).containsExactly(names);
    }

    @Test
    void customerCompositionAndOwnerSqlRespectMinimumReadBoundaries() throws Exception {
        try (var files = Files.walk(ROOT.resolve("customer_iam"))) {
            for (var file : files.filter(Files::isRegularFile).toList()) {
                String source = Files.readString(file);
                assertThat(source).doesNotContain("evidence.internal", "subject_number.internal", "FROM evidence.", "JOIN evidence.",
                        "FROM subject_number.", "JOIN subject_number.");
            }
        }
        String service = Files.readString(ROOT.resolve("customer_iam/internal/application/CustomerEvidenceQueryService.java"));
        assertThat(service).doesNotContain("FROM ", "JOIN ", "INSERT ", "UPDATE ", "DELETE ", "FOR UPDATE", "FOR SHARE", "snapshot", "mediaread", "sharing");
        assertThat(service.lines().filter(value -> value.startsWith("import com.onecall.core.") && !value.contains(".customer_iam.")))
                .noneMatch(value -> value.contains(".internal."));
        for (String path : List.of("evidence/internal/customerevidencequery/JooqCustomerEvidenceReader.java", "subject_number/internal/customerquery/JooqCustomerSubjectAccessReader.java")) {
            String source = Files.readString(ROOT.resolve(path));
            assertThat(source.toUpperCase(Locale.ROOT)).doesNotContain("SELECT *", "INSERT ", "UPDATE ", "DELETE ", "TRUNCATE ", "FOR UPDATE", "FOR SHARE", "BEGIN ", "COMMIT", "SET LOCAL ROLE");
            assertThat(source).doesNotContain("TransactionTemplate", "DataSource", "customer_iam.internal", "sharing.", "mediaread", "ByteSource", "snapshot", "evidence_event");
            if (path.startsWith("evidence")) assertThat(source).doesNotContain("subject_number.", "organization_access.", "customer_iam.", "owner_account_id", "owner_organization_id");
            else assertThat(source).doesNotContain("evidence.", "organization_access.", "customer_iam.");
        }
        String reader = Files.readString(ROOT.resolve("evidence/internal/customerevidencequery/JooqCustomerEvidenceReader.java"));
        assertThat(reader).contains("identity_at_capture->>'capturedAt'", "LEFT JOIN evidence.media_object", "r.id = e.source_ref_id",
                "a.artifact_role = 'ORIGINAL'", "r.media_object_id = a.media_object_id", "r.status = 'VALIDATED'", "e.created_at DESC, e.id DESC");
        assertThat(reader).doesNotContain("object_key", "bucket", "crypto_key_ref", "object_version_id", "declared_duration", "OFFSET");
        String subjects = Files.readString(ROOT.resolve("subject_number/internal/customerquery/JooqCustomerSubjectAccessReader.java"));
        assertThat(subjects).contains("s.owner_account_id = ?", "s.owner_organization_id = ?", "caller_number_binding", "subject_risk_state")
                .doesNotContain("b.status", "number_hmac", "number_ciphertext");
        components(CustomerEvidenceDtos.EvidencePage.class, "items", "nextCursor", "hasMore");
        components(CustomerEvidenceDtos.EvidenceSummary.class, "evidenceId", "subjectId", "status", "sourceType", "capturedAt", "consentVersion", "createdAt", "artifactRoles");
        components(CustomerEvidenceDtos.EvidenceDetail.class, "evidenceId", "subjectId", "status", "sourceType", "capturedAt", "consentVersion", "solidifiedAt", "artifacts");
        components(CustomerEvidenceDtos.EvidenceArtifact.class, "artifactRole", "mimeType", "sizeBytes", "validatedDurationSeconds", "sha256");
        components(CustomerEvidenceQueryService.Request.class, "anchor", "limit");
        components(CustomerEvidenceQueryService.Page.class, "items", "nextAnchor", "hasMore");
        components(CustomerEvidenceQueryService.Summary.class, "evidenceId", "subjectId", "status", "sourceType", "capturedAt", "consentVersion", "createdAt", "artifactRoles");
        components(CustomerEvidenceQueryService.Detail.class, "evidenceId", "subjectId", "status", "sourceType", "capturedAt", "consentVersion", "solidifiedAt", "artifacts");
    }

    private Set<String> oldTuples() {
        Set<String> expected = new HashSet<>();
        for (String schema : List.of("customer_iam", "organization_access", "subject_number", "activation_consent", "evidence", "catalog_order"))
            expected.add("customer_iam_role|schema|" + schema + "||USAGE|" + schema + "_owner");
        expected.add("evidence_role|schema|evidence||USAGE|evidence_owner");
        columns(expected, "customer_iam_role", "customer_iam.customer_account", "id", "display_name", "status");
        columns(expected, "customer_iam_role", "organization_access.organization", "id", "display_name", "status");
        columns(expected, "customer_iam_role", "organization_access.organization_membership", "organization_id", "account_id", "role", "status");
        columns(expected, "customer_iam_role", "subject_number.business_subject", "id", "owner_account_id", "owner_organization_id", "real_name_status", "created_at");
        columns(expected, "customer_iam_role", "subject_number.caller_number_binding", "id", "subject_id", "number_masked", "number_type", "status");
        columns(expected, "customer_iam_role", "activation_consent.service_activation", "subject_id", "status");
        columns(expected, "customer_iam_role", "subject_number.subject_risk_state", "caller_number_binding_id", "state");
        columns(expected, "customer_iam_role", "evidence.evidence_record", "subject_id");
        columns(expected, "customer_iam_role", "catalog_order.customer_order", "subject_id", "account_id", "owner_organization_id");
        columns(expected, "evidence_role", "evidence.evidence_record", "id", "subject_id", "status", "source_type", "source_ref_id", "identity_at_capture");
        columns(expected, "evidence_role", "evidence.evidence_artifact", "evidence_id", "media_object_id", "artifact_role");
        columns(expected, "evidence_role", "evidence.media_object", "id", "sha256", "size_bytes", "mime_type");
        columns(expected, "evidence_role", "evidence.recording", "media_object_id", "validated_duration");
        return expected;
    }

    private void columns(Set<String> result, String role, String table, String... names) {
        for (String name : names) assertThat(result.add(role + "|column|" + table + "|" + name + "|SELECT|" + table.substring(0, table.indexOf('.')) + "_owner")).isTrue();
    }

    @Test
    void v017AddsOnlyNineteenColumnsAndPa25Uses288AndSeventeen() throws Exception {
        String migration = Files.readString(Path.of(MIGRATION));
        Set<String> old = tuples(migration, "old");
        Set<String> complete = tuples(migration, "final");
        assertThat(old).hasSize(50).isEqualTo(oldTuples());
        Set<String> expected = new HashSet<>(oldTuples()); expected.addAll(DELTA);
        assertThat(complete).hasSize(69).isEqualTo(expected);
        Set<String> delta = new HashSet<>(complete); delta.removeAll(old);
        assertThat(delta).hasSize(19).isEqualTo(DELTA);
        assertThat(complete.stream().filter(value -> value.startsWith("customer_iam_role|"))).hasSize(53);
        assertThat(complete.stream().filter(value -> value.startsWith("evidence_role|"))).hasSize(16);
        assertThat(migration).doesNotContain("REVOKE ", "CREATE TABLE", "ALTER TABLE", "CREATE ROLE", "ALTER ROLE", "INSERT INTO", "DELETE FROM", "UPDATE ", "TRUNCATE ");
        assertThat(migration.lines().map(String::strip).filter(value -> value.startsWith("EXECUTE format('GRANT")))
                .containsExactly("EXECUTE format('GRANT USAGE ON SCHEMA %s TO %I', fact.item->>2, fact.item->>0);",
                        "EXECUTE format('GRANT SELECT (%I) ON TABLE %s TO %I', fact.item->>3, fact.item->>2, fact.item->>0);");
        assertThat(migration).contains("NOT IN (16, 17)", "installed_rank <= 16", "installed_rank <> 17", "version IS DISTINCT FROM '017'");
        for (String category : List.of("AUTHORITY", "PROVENANCE", "HISTORY", "SETTINGS", "ATTRIBUTES", "TOPOLOGY", "OWNERSHIP", "ACL", "DATABASE", "DEFAULT_ACL", "DEPENDENCY", "PRESERVATION", "RECONCILIATION"))
            assertThat(migration).contains("V017_" + category + "_REJECTED");
        var checksum = new CRC32();
        for (String line : migration.replace("\uFEFF", "").lines().toList()) checksum.update(line.getBytes(StandardCharsets.UTF_8));
        assertThat((int) checksum.getValue()).isEqualTo(V017_CHECKSUM);
        String pa25 = Files.readString(Path.of("ops/postgresql/pa25-runtime-identities.sql"));
        assertThat(pa25).contains("expected_resolved) <> 410", "history) <> 18");
        Set<String> provisioned = tuples(pa25, "frozen");
        assertThat(provisioned).hasSize(410).containsAll(expected);
        assertThat(provisioned.stream().filter(value -> value.startsWith("customer_iam_role|"))).hasSize(53);
        assertThat(provisioned.stream().filter(value -> value.startsWith("evidence_role|"))).hasSize(18);
        assertThat(provisioned.stream().filter(value -> !value.startsWith("customer_iam_role|") && !value.startsWith("evidence_role|") && !value.startsWith("operations_iam_role|") && !value.startsWith("ivr_role|"))).hasSize(219);
        JsonNode history = tagged(pa25, "history");
        assertThat(history.size()).isEqualTo(18);
        assertThat(history.get(17).get(0).asString()).isEqualTo("018");
        assertThat(history.get(17).get(2).asInt()).isEqualTo(1606293398);
        assertThat(history.get(15).get(2).asInt()).isEqualTo(1669606497);
        JsonNode last = history.get(16);
        assertThat(last.size()).isEqualTo(4);
        assertThat(last.get(0).asString()).isEqualTo("017");
        assertThat(last.get(1).asString()).isEqualTo("V017__customer_evidence_read_privileges.sql");
        assertThat(last.get(2).isIntegralNumber()).isTrue();
        assertThat(last.get(2).asInt()).isEqualTo(V017_CHECKSUM);
        assertThat(last.get(3).asString()).isEqualTo("customer evidence read privileges");
    }

    private JsonNode tagged(String source, String tag) {
        String marker = "$" + tag + "$";
        int start = source.indexOf(marker); int end = source.indexOf(marker, start + marker.length());
        assertThat(start).isNotNegative(); assertThat(end).isGreaterThan(start);
        return JsonMapper.builder().build().readTree(source.substring(start + marker.length(), end));
    }

    private Set<String> tuples(String source, String tag) {
        Set<String> result = new HashSet<>();
        for (var row : tagged(source, tag)) {
            assertThat(row.size()).isEqualTo(tag.equals("frozen") && row.size() == 7 ? 7 : 6);
            if (row.size() == 7) {
                assertThat(row.get(6).isBoolean()).isTrue();
                assertThat(row.get(6).asBoolean()).isFalse();
            }
            assertThat(result.add(String.join("|", row.valueStream().limit(6).map(JsonNode::asString).toList()))).isTrue();
        }
        return result;
    }

    @Test
    void openApiAndV001ThroughV016RemainFrozenWithoutContractExpansion() throws Exception {
        Map<String, String> frozen = Map.ofEntries(
                Map.entry("../api/openapi/onecall-v1.openapi.yaml", "ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30"),
                Map.entry("../api/openapi/media-gateway-v1.openapi.yaml", "7f169b083125b890129e5b640d904034d49bd64ad063300c1459cde596213b65"),
                Map.entry("src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql", "946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045"),
                Map.entry("src/main/resources/db/migration/V002__identity_organization_and_employee.sql", "534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5"),
                Map.entry("src/main/resources/db/migration/V003__subject_number_and_claim.sql", "1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1"),
                Map.entry("src/main/resources/db/migration/V004__catalog_order_and_payment.sql", "a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc"),
                Map.entry("src/main/resources/db/migration/V005__ivr_task_session_call.sql", "2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310"),
                Map.entry("src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql", "830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475"),
                Map.entry("src/main/resources/db/migration/V007__activation_and_consent.sql", "bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659"),
                Map.entry("src/main/resources/db/migration/V008__evidence_media.sql", "2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e"),
                Map.entry("src/main/resources/db/migration/V009__sharing.sql", "ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae"),
                Map.entry("src/main/resources/db/migration/V010__sharing_role_least_privilege.sql", "fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343"),
                Map.entry("src/main/resources/db/migration/V011__sharing_public_read_least_privilege.sql", "f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b"),
                Map.entry("src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql", "13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828"),
                Map.entry("src/main/resources/db/migration/V013__evidence_media_reader_role.sql", "691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6"),
                Map.entry("src/main/resources/db/migration/V014__database_runtime_acl_rbac_hardening.sql", "e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a"),
                Map.entry("src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql", "1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3"),
                Map.entry("src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql", "988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f"));
        assertThat(frozen).hasSize(18);
        for (var entry : frozen.entrySet()) {
            String lf = Files.readString(Path.of(entry.getKey())).replace("\r\n", "\n").replace("\r", "\n");
            for (String variant : List.of(lf, lf.replace("\n", "\r\n"), lf.replace("\n", "\r"))) {
                assertThat(hash(variant)).as(entry.getKey()).isEqualTo(entry.getValue());
                assertThat(hash("X" + variant)).isNotEqualTo(entry.getValue());
            }
        }
    }

    private String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.replace("\r\n", "\n").replace("\r", "\n").getBytes(StandardCharsets.UTF_8)));
    }
}
