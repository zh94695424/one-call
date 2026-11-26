package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CustomerCatalogOfferReadAclBoundaryTests {
    private static final String V019 = "V019__customer_catalog_offer_read_privileges.sql";
    static final Set<String> MIGRATIONS = Set.of(
            "V001__platform_schemas_roles_idempotency_outbox_audit.sql",
            "V002__identity_organization_and_employee.sql",
            "V003__subject_number_and_claim.sql",
            "V004__catalog_order_and_payment.sql",
            "V005__ivr_task_session_call.sql",
            "V006__trial_entitlement_issuance_reservation_usage.sql",
            "V007__activation_and_consent.sql",
            "V008__evidence_media.sql",
            "V009__sharing.sql",
            "V010__sharing_role_least_privilege.sql",
            "V011__sharing_public_read_least_privilege.sql",
            "V012__sharing_admission_least_privilege.sql",
            "V013__evidence_media_reader_role.sql",
            "V014__database_runtime_acl_rbac_hardening.sql",
            "V015__customer_business_number_read_privileges.sql",
            "V016__customer_subject_detail_read_privileges.sql",
            "V017__customer_evidence_read_privileges.sql",
            "V018__evidence_role_ivr_association_read_privileges.sql",
            "V019__customer_catalog_offer_read_privileges.sql",
            "V020__customer_order_read_privileges.sql");
    private static final Path ROOT = Path.of("..");
    private static String hash(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static byte[] lf(byte[] bytes) { return new String(bytes, StandardCharsets.UTF_8).replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8); }
    @SuppressWarnings("unchecked")
    static Set<String> oracle(String name) throws Exception {
        var method = Class.forName("com.onecall.archtests.container.CustomerCatalogOfferReadPrivilegesIT").getDeclaredMethod(name);
        method.setAccessible(true); return (Set<String>) method.invoke(null);
    }
    static String historicalPa25() throws Exception {
        var method = Class.forName("com.onecall.archtests.container.CustomerCatalogOfferReadPrivilegesIT").getDeclaredMethod("historicalV018Pa25Script");
        method.setAccessible(true); return (String) method.invoke(null);
    }
    static void assertCurrentMigrationNames(Collection<String> names) {
        assertThat(names).hasSize(20); assertThat(new HashSet<>(names)).hasSize(20).isEqualTo(MIGRATIONS);
    }
    static void currentMigrationProof() throws Exception {
        try (var files = Files.list(Path.of("src/main/resources/db/migration"))) { assertCurrentMigrationNames(files.map(path -> path.getFileName().toString()).toList()); }
        var historical = new TreeSet<>(MIGRATIONS); historical.remove("V020__customer_order_read_privileges.sql"); assertThat(historical).hasSize(19);
        historical.remove(V019); assertThat(historical).hasSize(18).noneMatch(name -> name.startsWith("V019"));
        for (String extra : List.of("V020__unexpected.sql", "V019__unexpected.sql")) { var hostile = new ArrayList<>(MIGRATIONS); hostile.add(extra); assertThatThrownBy(() -> assertCurrentMigrationNames(hostile)).isInstanceOf(AssertionError.class); }
        var missing = new ArrayList<>(historical); assertThatThrownBy(() -> assertCurrentMigrationNames(missing)).isInstanceOf(AssertionError.class);
        missing.add("V019__unexpected.sql"); assertThatThrownBy(() -> assertCurrentMigrationNames(missing)).isInstanceOf(AssertionError.class);
        var duplicate = new ArrayList<>(MIGRATIONS); duplicate.set(0, duplicate.get(1)); assertThatThrownBy(() -> assertCurrentMigrationNames(duplicate)).isInstanceOf(AssertionError.class);
    }
    private static String sql() throws Exception { return Files.readString(Path.of("src/main/resources/db/migration").resolve(V019)); }
    private static tools.jackson.databind.JsonNode tagged(String text, String tag) {
        String marker = "$"+tag+"$"; int start = text.indexOf(marker)+marker.length();
        return JsonMapper.builder().build().readTree(text.substring(start, text.indexOf(marker,start)));
    }
    private static Set<String> tuples(String text, String tag) {
        var rows = tagged(text, tag); Set<String> result = new TreeSet<>();
        for (var row : rows) { List<String> fields = new ArrayList<>(); for (int index=0;index<6;index++) fields.add(row.get(index).asString()); fields.add("false"); assertThat(result.add(String.join("|",fields))).isTrue(); }
        assertThat(result).hasSize(rows.size()); return result;
    }
    @Test void v019GrantsExactlyTwentyTwoOwnerIssuedColumns() throws Exception {
        Set<String> old = tuples(sql(),"old"), complete = tuples(sql(),"final");
        assertThat(old).hasSize(53); assertThat(complete).hasSize(75).containsAll(old);
        Set<String> delta = new TreeSet<>(complete); delta.removeAll(old); assertThat(delta).isEqualTo(oracle("catalogDeltaOracle"));
        assertThat(delta).allMatch(value -> value.startsWith("customer_iam_role|column|catalog_order.") && value.endsWith("|SELECT|catalog_order_owner|false"));
        assertThat(sql()).contains("SET LOCAL ROLE catalog_order_owner;", "EXECUTE format('SET LOCAL ROLE %I', session_user)").doesNotContain( "REVOKE ", "CREATE ROLE", "INSERT INTO", "UPDATE catalog_order", "DELETE FROM catalog_order");
        var grants=Pattern.compile("(?s)GRANT SELECT \\(.*?;").matcher(sql());
        while(grants.find()) assertThat(grants.group()).doesNotContain("WITH GRANT OPTION");
        assertThat(Pattern.compile("(?m)^                GRANT SELECT ").matcher(sql()).results().count()).isEqualTo(4);
    }
    @Test void v019RejectsDriftAndAcceptsOnlyExactReplayStates() throws Exception {
        String source = sql();
        for (String category : List.of("AUTHORITY","PROVENANCE","HISTORY","SETTINGS","ATTRIBUTES","TOPOLOGY","OWNERSHIP","ACL","DATABASE","DEFAULT_ACL","DEPENDENCY","PRESERVATION","RECONCILIATION")) assertThat(source).contains("V019_"+category+"_REJECTED");
        assertThat(source).contains("NOT IN (18, 19)", "NOT IN (53, 75)", "FOR pass IN 1..2 LOOP", "expected_acls = old_acls", "installed_rank = 19", "725025, 1", "ERRCODE = 'P0001'", "MESSAGE = failure_category");
        assertThat(tagged(source,"history").size()).isEqualTo(18);
        assertThat(source).doesNotContain("marked_count = 4 AND NOT EXISTS");
    }
    @Test void frozenContractsAndV001ThroughV018RemainByteIdentical() throws Exception {
        var manifest = new StringBuilder(); int count=0;
        for (String directory : List.of("src/main/java", "src/main/resources")) try (var paths = Files.walk(Path.of(directory))) {
            for (Path path : paths.filter(Files::isRegularFile).filter(path -> !path.getFileName().toString().equals(V019) && !path.getFileName().toString().equals("V020__customer_order_read_privileges.sql")).sorted().toList()) { manifest.append(path.toString().replace('\\','/')).append(' ').append(hash(lf(Files.readAllBytes(path)))).append('\n'); count++; }
        }
        assertThat(count).isEqualTo(353);
        var lines = manifest.toString().lines().sorted().toList();
        assertThat(hash((String.join("\n",lines)+"\n").getBytes(StandardCharsets.UTF_8))).isEqualTo("93764e87e85a318753f98a20dfaaa46bde76a0e6e4113cd856ca1172b03cdbae");
        currentMigrationProof();
    }
    @Test void pa25PinsNineteenHistoryAndIndependent432TupleOracle() throws Exception {
        String source = Files.readString(Path.of("ops/postgresql/pa25-runtime-identities.sql"));
        assertThat(tuples(source,"frozen")).hasSize(449);
        var historicalMethod = Class.forName("com.onecall.archtests.container.CustomerCatalogOfferReadPrivilegesIT").getDeclaredMethod("historicalV019Pa25Script");
        historicalMethod.setAccessible(true); source = (String) historicalMethod.invoke(null);
        assertThat(tuples(source,"frozen")).hasSize(432).isEqualTo(oracle("current432Oracle"));
        var history = tagged(source,"history"); assertThat(history.size()).isEqualTo(19);
        CRC32 crc = new CRC32(); for (String line : sql().lines().toList()) crc.update(line.getBytes(StandardCharsets.UTF_8));
        assertThat(history.get(18).get(0).asString()).isEqualTo("019"); assertThat(history.get(18).get(1).asString()).isEqualTo(V019);
        assertThat(history.get(18).get(2).asInt()).isEqualTo((int)crc.getValue());
        assertThat(source).contains("expected_resolved) <> 432", "history) <> 19");
        historicalPa25();
    }
    @Test void existingSevenLoginElevenEdgeGraphRemainsUnchanged() throws Exception {
        String source = Files.readString(Path.of("ops/postgresql/pa25-runtime-identities.sql")).replace("\r\n","\n");
        for (String marker : List.of("PA25_APPLY_OK logins=7 effective_roles=9 memberships=11", "PA25_CHECK_OK logins=7 effective_roles=9 memberships=11", "edge_count <> 11", "membership.admin_option OR membership.inherit_option OR NOT membership.set_option")) assertThat(source).contains(marker);
        String old = historicalPa25();
        assertThat(source.substring(source.indexOf("external_names text[]"),source.indexOf("expected_acls jsonb"))).isEqualTo(old.replace("\r\n","\n").substring(old.replace("\r\n","\n").indexOf("external_names text[]"),old.replace("\r\n","\n").indexOf("expected_acls jsonb")));
    }
    @Test void currentDocsPreserveHistoricalBoundaries() throws Exception {
        for (String name : List.of("backend/README.md", "docs/operations/pa25-production-database-identity-provisioning-runbook.md")) {
            String text = Files.readString(ROOT.resolve(name)); int marker = text.indexOf("\n<!-- PA36 HISTORICAL BODY -->");
            assertThat(text.substring(0,marker)).contains("PA36 Current Runtime Boundary", "PA42 Current Provisioning Boundary", "**20**", "**449**", "**92**", "seven schema USAGE", "85 column SELECT", "NOT EXECUTED", "not RLS", "operations_login");
            new EvidenceRoleIvrAssociationAclBoundaryTests().currentSecurityAndRunbookBoundariesAgreeWithoutRewritingHistory();
        }
    }
    @Test void migrationOnlyAddsOnePackagedResource() throws Exception {
        currentMigrationProof();
        try (var source=Files.walk(Path.of("src/main/java")); var resources=Files.walk(Path.of("src/main/resources"))) { assertThat(source.filter(path -> path.toString().endsWith(".java")).count()).isEqualTo(332); assertThat(resources.filter(Files::isRegularFile).count()).isEqualTo(23); }
        String ci = Files.readString(ROOT.resolve(".github/workflows/ci.yml")); assertThat(ci).contains("entries.count('BOOT-INF/classes/db/migration/"+V019+"') == 1");
        List<String> manifest=new ArrayList<>();
        for(String directory:List.of("src/main/java","src/main/resources")) try(var files=Files.walk(Path.of(directory))) { for(Path file:files.filter(Files::isRegularFile).toList()) manifest.add(file.toString().replace('\\','/')+" "+hash(lf(Files.readAllBytes(file)))); }
        Collections.sort(manifest); assertThat(manifest).hasSize(355); assertThat(hash((String.join("\n",manifest)+"\n").getBytes(StandardCharsets.UTF_8))).isEqualTo("7d7346e43c1c6d744ec803e4746d9b33b42ca33aa7e89c8beff317cc0cf3910d");
    }
    @Test void sourceCiPomAndXmlIdentitiesMatchExactly() throws Exception {
        Set<String> identities = new TreeSet<>(); int annotations=0, unit=0, integration=0;
        try (var paths=Files.walk(Path.of("src/test/java"))) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source=Files.readString(path); String fqcn=path.toString().replace('\\','/').substring("src/test/java/".length()).replace('/','.').replace(".java","");
                annotations+=(int)source.lines().filter(line -> Pattern.compile("@"+"Test\\b").matcher(line).find()).count();
                var matcher=Pattern.compile("@"+"Test\\s+(?:public\\s+)?void\\s+(\\w+)\\s*\\(").matcher(source);
                while(matcher.find()) { assertThat(identities.add(fqcn+"#"+matcher.group(1))).isTrue(); if(path.getFileName().toString().endsWith("IT.java")) integration++;else unit++; }
            }
        }
        assertThat(unit).isEqualTo(1164); assertThat(integration).isEqualTo(764); assertThat(annotations).isEqualTo(1929);
        assertThat(hash((String.join("\n",identities)+"\n").getBytes(StandardCharsets.UTF_8))).isEqualTo("0af27da2f85f6239d0e04a3a694fd4bbde7b325208a7771fbf359db80c056ae8");
        String ci=Files.readString(ROOT.resolve(".github/workflows/ci.yml")); assertThat(ci).contains("pa40a_unit_additions", "pa40a_container_additions", "method_totals == [1127, 738]", "line_scan_total == 1866", "current_method_totals == [1164, 764]", "current_line_scan_total == 1929");
        assertThat(Files.readString(Path.of("pom.xml"))).contains("<include>**/CustomerCatalogOfferReadPrivilegesIT.java</include>", "<include>**/CustomerCatalogOffersIT.java</include>");
    }
}
