package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import com.onecall.archtests.container.EvidenceRoleIvrAssociationAclIT;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class EvidenceRoleIvrAssociationAclBoundaryTests {
    private static final String V018 = "V018__evidence_role_ivr_association_read_privileges.sql";
    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
    private static final int CHECKSUM = 1606293398;
    private static final List<String> UNIT = List.of("v018NamesOnlyTheTwoOwnerIssuedAssociationColumns", "v018GuardsAuthorityHistoryProvenanceAndExactReplayStates", "v001ThroughV017AndPublicContractsRemainByteFrozen", "pa25UsesExactEighteenHistoryAndIndependent410TupleOracle", "pa25DefinesOnlyTheElevenApprovedNonInheritingEdges", "currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory", "pa36HasNoApplicationApiPoolOrDependencyExpansion", "ciPomAndOrdinaryMethodInventoryMatchExactly");
    private static final List<String> CONTAINER = List.of("v017DeniesBothAssociationColumnsWith42501", "freshAndUpgradeHaveExactEighteenHistoryAndFrozenPredecessors", "v018AddsExactlyTwoOwnerIssuedTuplesAndMatches410Oracle", "evidenceRoleReadsAllEightColumnsAndDeniesEveryUngrantableColumn", "v018ReplaysOnlyExactCleanOrCompleteStatesWithoutDrift", "v018RejectsPartialExtraWrongGrantorAndGrantOptionStatesAtomically", "v018RejectsAuthorityHistoryAndProvenanceDriftAtomically", "v018RejectsTopologyAttributesOwnershipSettingsAndDefaultDrift", "pa25RequiresV018AndExact410OracleInBothModes", "pa25UpgradesThreeSlotsToFourAndPreservesExistingCredentialBytes", "pa25RejectsInvalidOperationsInputAndCollidingProvenance", "pa25ApplyAndCheckEnforceExactElevenEdgeTopology", "operationsLoginSetsExactlySevenRolesAndRestoresSessionContext", "allOtherLoginAndDirectRoleBoundariesRemainExact", "pa25FailuresAreAtomicSanitizedAndNeverAdoptOrRestamp", "v018PreservesBusinessDefinitionsOwnersAndAllUnrelatedAcls");
    private static final String HISTORY = """
            001|V001__platform_schemas_roles_idempotency_outbox_audit.sql|-709123726|platform schemas roles idempotency outbox audit
            002|V002__identity_organization_and_employee.sql|-1229975166|identity organization and employee
            003|V003__subject_number_and_claim.sql|-1355776370|subject number and claim
            004|V004__catalog_order_and_payment.sql|-665258487|catalog order and payment
            005|V005__ivr_task_session_call.sql|-1005314773|ivr task session call
            006|V006__trial_entitlement_issuance_reservation_usage.sql|366834714|trial entitlement issuance reservation usage
            007|V007__activation_and_consent.sql|-1281725572|activation and consent
            008|V008__evidence_media.sql|-245169007|evidence media
            009|V009__sharing.sql|-59961153|sharing
            010|V010__sharing_role_least_privilege.sql|247431460|sharing role least privilege
            011|V011__sharing_public_read_least_privilege.sql|492686454|sharing public read least privilege
            012|V012__sharing_admission_least_privilege.sql|-291628548|sharing admission least privilege
            013|V013__evidence_media_reader_role.sql|-611381791|evidence media reader role
            014|V014__database_runtime_acl_rbac_hardening.sql|-340359494|database runtime acl rbac hardening
            015|V015__customer_business_number_read_privileges.sql|-1316980027|customer business number read privileges
            016|V016__customer_subject_detail_read_privileges.sql|1669606497|customer subject detail read privileges
            017|V017__customer_evidence_read_privileges.sql|-41822162|customer evidence read privileges
            """;
    private static final String FROZEN = """
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
            api/openapi/media-gateway-v1.openapi.yaml 26434f8bd231fb9ea45724b5887b653e8dc233c42d8b9319f8257f951c16a789 7f169b083125b890129e5b640d904034d49bd64ad063300c1459cde596213b65
            api/openapi/onecall-v1.openapi.yaml 0ef64251846ce6f23e136bdcaa39e45a6cc9d54d5f86470b8589d8d14fc94c68 ce8de66d9bbc3c3e2fbbc6469f146e6d7e9b58223d3fd7c2187e95c26a6e7e30
            docs/12-technology-stack-and-system-architecture.md 9faadb2fed8dfac33fe52349074852a967ef31ea6f4c1609fc0c4320d8bc40db 772ccaee7b188f68fcc694f3e8c00983ad9de028547f8e0b0c8638a8d8cae435
            docs/14-production-data-model-and-flyway-schema-spec.md 0a65833d202e566e56f8c11bcd27734812a62a27e8c9c5342b42301720a17b31 bd898f2978c186cf1ccc41c83807f226c6b5aeb7121bc07486748e16453c8c58
            """;
    private String migration() throws Exception { return Files.readString(MIGRATIONS.resolve(V018)); }
    private String pa25() throws Exception { return Files.readString(Path.of("ops/postgresql/pa25-runtime-identities.sql")); }
    private JsonNode tagged(String source, String tag) { String marker = "$" + tag + "$"; int start = source.indexOf(marker); assertThat(start).isGreaterThan(0); return JsonMapper.builder().build().readTree(source.substring(start + marker.length(), source.indexOf(marker, start + marker.length()))); }
    private Set<String> tuples(String source, String tag) {
        Set<String> result = new TreeSet<>(); var array = tagged(source, tag);
        for (var row : array) { List<String> fields = new ArrayList<>(); for (int index = 0; index < 6; index++) fields.add(row.get(index).asString()); fields.add("false"); assertThat(result.add(String.join("|", fields))).isTrue(); }
        assertThat(result).hasSize(array.size()); return result;
    }
    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static byte[] lf(byte[] bytes) { return new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8); }
    private static Set<String> methods(String source) { Set<String> names = new TreeSet<>(); var matcher = Pattern.compile("@" + "Test\\s+(?:public\\s+)?void\\s+(\\w+)\\s*\\(").matcher(source); while (matcher.find()) assertThat(names.add(matcher.group(1))).isTrue(); return names; }

    @Test
    void v018NamesOnlyTheTwoOwnerIssuedAssociationColumns() throws Exception {
        String source = migration(); Set<String> old = tuples(source, "old"), complete = tuples(source, "final");
        assertThat(old).hasSize(16); assertThat(complete).hasSize(18); Set<String> delta = new TreeSet<>(complete); delta.removeAll(old);
        assertThat(delta).containsExactlyInAnyOrder("evidence_role|column|evidence.evidence_record|session_id|SELECT|evidence_owner|false", "evidence_role|column|evidence.evidence_record|call_id|SELECT|evidence_owner|false");
        assertThat(complete).containsAll(old).allMatch(value -> value.startsWith("evidence_role|") && value.endsWith("|evidence_owner|false"));
        assertThat(source).contains("SET LOCAL ROLE evidence_owner;", "GRANT SELECT (session_id, call_id)", "ON TABLE evidence.evidence_record TO evidence_role;", "EXECUTE format('SET LOCAL ROLE %I', session_user)", "NOT IN (16, 18)");
        assertThat(source.lines().map(String::strip).filter(line -> line.startsWith("GRANT "))).hasSize(1);
        assertThat(source).doesNotContain("408", "410", "REVOKE ", "CREATE TABLE", "ALTER TABLE", "CREATE ROLE", "ALTER ROLE", "COMMENT ON", "INSERT INTO", "DELETE FROM", "TRUNCATE ");
    }
    @Test
    void v018GuardsAuthorityHistoryProvenanceAndExactReplayStates() throws Exception {
        String source = migration();
        for (String category : List.of("AUTHORITY", "PROVENANCE", "HISTORY", "SETTINGS", "ATTRIBUTES", "TOPOLOGY", "OWNERSHIP", "ACL", "DATABASE", "DEFAULT_ACL", "DEPENDENCY", "PRESERVATION", "RECONCILIATION")) assertThat(source).contains("V018_" + category + "_REJECTED");
        assertThat(source).contains("160000 AND 169999", "current_user <> session_user", "pg_advisory_xact_lock(725025, 1)", "set_config('search_path', 'pg_catalog', true)", "FOR pass IN 1..2", "EXCEPTION WHEN OTHERS", "ERRCODE = 'P0001'", "NOT IN (17, 18)", "installed_rank <= 17", "installed_rank <> 18", "checksum IS NULL", "marked_count NOT IN (0, 3, 4)", "marked_count = 4 AND NOT EXISTS", "expected_acls = old_acls AND EXISTS", "convert_to(actual_marker, 'UTF8')", "invariant_after IS DISTINCT FROM invariant_before", "grantee = ANY(boundary_oids)", "grantor = ANY(managed_oids)", "defaults.defaclrole = ANY(managed_oids)");
        var history = tagged(source, "history"); assertThat(history.size()).isEqualTo(17);
        int index = 0; for (String line : HISTORY.strip().split("\n")) { assertThat(history.get(index).get(0).asString()).isEqualTo(line.split("\\|")[0]); assertThat(history.get(index).get(2).asInt()).isEqualTo(Integer.parseInt(line.split("\\|")[2])); index++; }
        CRC32 crc = new CRC32(); source.lines().forEach(line -> crc.update(line.getBytes(StandardCharsets.UTF_8))); assertThat((int) crc.getValue()).isEqualTo(CHECKSUM);
        assertThat(hash(lf(Files.readAllBytes(MIGRATIONS.resolve(V018))))).isEqualTo("892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817");
    }
    @Test
    void v001ThroughV017AndPublicContractsRemainByteFrozen() throws Exception {
        for (String line : FROZEN.strip().split("\n")) { String[] fields = line.split(" "); byte[] bytes = Files.readAllBytes(Path.of("..").resolve(fields[0])); assertThat(hash(bytes)).as(fields[0]).isIn(fields[1], fields[2]); assertThat(hash(lf(bytes))).isEqualTo(fields[2]); }
        List<Path> files = new ArrayList<>(); for (String root : List.of("src/main/java", "src/main/resources")) try (var paths = Files.walk(Path.of(root))) { files.addAll(paths.filter(Files::isRegularFile).filter(path -> !path.getFileName().toString().equals(V018) && !path.getFileName().toString().equals("V019__customer_catalog_offer_read_privileges.sql") && !path.getFileName().toString().equals("V020__customer_order_read_privileges.sql")).toList()); }
        List<String> manifest = new ArrayList<>(); for (Path file : files) manifest.add(file.toString().replace('\\', '/') + " " + hash(lf(Files.readAllBytes(file)))); Collections.sort(manifest);
        assertThat(manifest).hasSize(361); assertThat(hash((String.join("\n", manifest) + "\n").getBytes(StandardCharsets.UTF_8))).isEqualTo("e23efe75af8b2398079d91cb71686ca812514d4fb07a846dffac7aa6a334b1c3");
    }
    @Test
    void pa25UsesExactEighteenHistoryAndIndependent410TupleOracle() throws Exception {
        String current = pa25(); String source = CustomerCatalogOfferReadAclBoundaryTests.historicalPa25(); Set<String> expected = EvidenceRoleIvrAssociationAclIT.currentOracle(); assertThat(tuples(source, "frozen")).hasSize(410).isEqualTo(expected);
        Set<String> seven = new TreeSet<>(expected); seven.removeIf(value -> value.startsWith("operations_iam_role|") || value.startsWith("ivr_role|")); assertThat(seven).hasSize(290); seven.removeAll(EvidenceRoleIvrAssociationAclIT.associationDelta()); assertThat(seven).hasSize(288);
        assertThat(source).contains("expected_resolved) <> 410", "history) <> 18"); var history = tagged(source, "history"); assertThat(history.size()).isEqualTo(18);
        int index = 0; for (String line : HISTORY.strip().split("\n")) { List<String> actual = new ArrayList<>(); for (var value : history.get(index)) actual.add(value.isIntegralNumber() ? Integer.toString(value.asInt()) : value.asString()); assertThat(String.join("|", actual)).isEqualTo(line); index++; }
        assertThat(history.get(17).get(0).asString()).isEqualTo("018"); assertThat(history.get(17).get(1).asString()).isEqualTo(V018); assertThat(history.get(17).get(2).asInt()).isEqualTo(CHECKSUM); assertThat(history.get(17).get(3).asString()).isEqualTo("evidence role ivr association read privileges");
        Set<String> currentExpected = CustomerOrderReadBoundaryTests.oracle("current449Oracle");
        assertThat(tuples(current, "frozen")).hasSize(449).isEqualTo(currentExpected);
        Set<String> recovered = new TreeSet<>(currentExpected); recovered.removeAll(CustomerOrderReadBoundaryTests.oracle("orderReadDeltaOracle")); assertThat(recovered).isEqualTo(CustomerCatalogOfferReadAclBoundaryTests.oracle("current432Oracle")); recovered.removeAll(CustomerCatalogOfferReadAclBoundaryTests.oracle("catalogDeltaOracle")); assertThat(recovered).isEqualTo(expected);
        assertThat(tagged(current,"history").size()).isEqualTo(20);
    }
    @Test
    void pa25DefinesOnlyTheElevenApprovedNonInheritingEdges() throws Exception {
        String source = pa25();
        for (String input : List.of("sharing_login", "customer_login", "command_login", "operations_login")) assertThat(source).contains("\\if :{?" + input + "}", "pa25." + input);
        assertThat(source).contains("operations_ivr_read", "FOR slot_index IN 1..4", "edge_count <> 11", "membership.admin_option OR membership.inherit_option OR NOT membership.set_option", "NOT grantor.rolsuper", "boundary_names := managed_names || shared_context_targets", "setrole = ANY(boundary_oids)", "ARRAY boundary_names", "'operations_iam_role', 'ivr_role') OR role_name = ANY(shared_context_targets)", "CREATE ROLE %I LOGIN NOINHERIT", "PA25_APPLY_OK logins=7 effective_roles=9 memberships=11", "PA25_CHECK_OK logins=7 effective_roles=9 memberships=11");
        String pairs = source.substring(source.indexOf("FOR object_row IN SELECT * FROM (VALUES"), source.indexOf("edges(target_name, member_name)"));
        var matcher = Pattern.compile("\\('([^']+)', external_names\\[(\\d)\\]\\)").matcher(pairs); Set<String> actual = new TreeSet<>(); while (matcher.find()) assertThat(actual.add(matcher.group(1) + "|" + matcher.group(2))).isTrue();
        assertThat(actual).containsExactlyInAnyOrder("sharing_role|1", "customer_iam_role|2", "sharing_command_role|3", "evidence_role|3", "operations_iam_role|4", "subject_number_role|4", "activation_consent_role|4", "entitlement_metering_role|4", "evidence_role|4", "catalog_order_role|4", "ivr_role|4");
    }
    @Test
    void currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory() throws Exception {
        for (String row : List.of("docs/17-production-security-design-spec.md 8f96a69ce5309319a8c8fab1cc6cab419e79e90781945ddcd6fd2b41a311d38f", "docs/operations/pa25-production-database-identity-provisioning-runbook.md 8b2daec18d9d0ab5498a50d63392f0a253b82eff68822d2a04860981c80c242d", "backend/README.md fb6ab282e38c7a308ad963b9481a856682dd488ff30cc8eb412ab80f55fb3beb")) {
            String[] fields = row.split(" "); byte[] bytes = Files.readAllBytes(Path.of("..").resolve(fields[0])); String text = new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n"); String marker = "\n<!-- PA36 HISTORICAL BODY -->\n\n"; int offset = text.indexOf(marker); assertThat(offset).isGreaterThan(0); byte[] historical = text.substring(offset + marker.length()).getBytes(StandardCharsets.UTF_8); assertThat(hash(historical)).isEqualTo(fields[1]);
            String current = text.substring(0, offset); assertThat(current).contains("PA36 Current Runtime Boundary (V018)", "16→18", "408→410", "288→290", "operations_login", "operations_ivr_read", "effective_roles=9 means exact-ACL-monitored roles", "capability boundary13", "full boundary17", "role-wide", "explicit SET is not a read-only privilege sandbox", "JooqOperationsEvidenceCounter", "NOT EXECUTED", "PA35 code is added", Integer.toString(CHECKSUM), "892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817");
            assertThat(current).contains("signed Flyway checksum **1606293398**", "repository-canonical LF SHA256 **892c607787c7bfeb0e8eed0a6235f14563fab515872ee6558263e2feada84817**", "a standard CRLF-materialized checkout has raw SHA256 **ea2911ea92fa4b3d043f7fe63777d8cf35fb0733202784952886ac7978a4b37c**", "Checkout bytes depend on the line-ending representation.");
            var paths = Pattern.compile("`(backend/src/main/java/[^`]+\\.java)`").matcher(current); int consumers = 0; while (paths.find()) { Path path = Path.of("..").resolve(paths.group(1)); assertThat(path).exists(); assertThat(Files.readString(path)).contains(path.getFileName().toString().replace(".java", "")); consumers++; } assertThat(consumers).isEqualTo(4);
            if (!fields[0].equals("docs/17-production-security-design-spec.md"))
                assertThat(current).contains("PA42 Current Provisioning Boundary", "**20**", "**449**", "**92**", "seven schema USAGE", "85 column SELECT");
        }
        byte[] docs = Files.readAllBytes(Path.of("../docs/17-production-security-design-spec.md")); assertThat(hash(lf(docs))).isEqualTo("4e7df589380f70ba7e9e1a5b7e9c1542ca6ed89313a775dacfc91e109852ceb6");
    }
    @Test
    void pa36HasNoApplicationApiPoolOrDependencyExpansion() throws Exception {
        String pom = Files.readString(Path.of("pom.xml")).replace("\r\n", "\n"); var pattern = Pattern.compile("(?s)<dependencies>.*?</dependencies>").matcher(pom); List<String> dependencies = new ArrayList<>(); while (pattern.find()) dependencies.add(pattern.group()); assertThat(hash(String.join("\n", dependencies).getBytes(StandardCharsets.UTF_8))).isEqualTo("e2e2547a79447555b7bf4c42ca4bf3c967a84124987eea62c3435112c3ee4445");
        try (var paths = Files.list(MIGRATIONS)) { assertThat(paths.map(path -> path.getFileName().toString()).toList()).hasSize(20).containsExactlyInAnyOrderElementsOf(java.util.stream.Stream.concat(FROZEN.lines().filter(line -> line.contains("/migration/")).map(line -> Path.of(line.split(" ")[0]).getFileName().toString()), java.util.stream.Stream.of(V018, "V019__customer_catalog_offer_read_privileges.sql", "V020__customer_order_read_privileges.sql")).toList()); }
        String ci = Files.readString(Path.of("../.github/workflows/ci.yml")); assertThat(ci).contains("entries.count('BOOT-INF/classes/db/migration/" + V018 + "') == 1", "pa25-runtime-identities", "{probes} adversarial probes");
    }
    @Test
    void ciPomAndOrdinaryMethodInventoryMatchExactly() throws Exception {
        String ci = Files.readString(Path.of("../.github/workflows/ci.yml")); String pom = Files.readString(Path.of("pom.xml"));
        assertThat(ci).contains("pa36_unit_additions", "pa36_container_additions", "[788, 556]", "pa36_start_totals == [704, 504]", "pa36_start_line_scan_total == 1209", "pa35_start_totals == [712, 520]", "pa35_start_line_scan_total == 1233", "== 1345", "len(unit_expected) == 130", "len(container_expected) == 48", "len(includes) == 48", "pa38_unit_additions", "pa38_container_additions", "pa38_start_totals == [848, 592]", "pa38_start_line_scan_total == 1441", "method_totals == [1127, 738]", "line_scan_total == 1866", "current_method_totals == [1202, 775]", "current_line_scan_total == 1978");
        String include = "<include>**/EvidenceRoleIvrAssociationAclIT.java</include>"; assertThat(pom.indexOf(include)).isGreaterThan(0); assertThat(pom.indexOf(include, pom.indexOf(include) + 1)).isEqualTo(-1);
        for (var entry : Map.of("architecture/EvidenceRoleIvrAssociationAclBoundaryTests.java", UNIT, "container/EvidenceRoleIvrAssociationAclIT.java", CONTAINER).entrySet()) {
            String source = Files.readString(Path.of("src/test/java/com/onecall/archtests").resolve(entry.getKey())); assertThat(methods(source)).hasSize(entry.getValue().size()).containsExactlyInAnyOrderElementsOf(entry.getValue()); assertThat(source).doesNotContain("@" + "ParameterizedTest", "@" + "TestFactory", "@" + "Disabled", "Assumptions" + ".", "@" + "RepeatedTest"); for (String name : entry.getValue()) assertThat(ci).contains("'" + name + "'");
        }
    }
}
