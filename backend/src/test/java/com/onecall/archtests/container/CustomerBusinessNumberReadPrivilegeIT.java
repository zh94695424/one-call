package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;

class CustomerBusinessNumberReadPrivilegeIT {
    private CustomerBusinessNumbersFixture fixture;
    private static final Path SCRIPT = Path.of("src/main/resources/db/migration/V015__customer_business_number_read_privileges.sql");
    private static final Map<String, List<String>> APPROVED = Map.ofEntries(
            Map.entry("customer_iam.customer_account", List.of("id", "display_name", "status")),
            Map.entry("organization_access.organization", List.of("id", "display_name", "status")),
            Map.entry("organization_access.organization_membership", List.of("account_id", "organization_id", "role", "status")),
            Map.entry("subject_number.business_subject", List.of("id", "owner_account_id", "owner_organization_id", "real_name_status", "created_at")),
            Map.entry("subject_number.caller_number_binding", List.of("id", "subject_id", "number_masked", "number_type", "status")),
            Map.entry("activation_consent.service_activation", List.of("subject_id", "status")),
            Map.entry("catalog_order.catalog_product", List.of("id", "product_code")),
            Map.entry("catalog_order.catalog_version", List.of("id", "product_id", "version_no", "sale_status", "offer_type", "audience", "price_cents", "currency", "billing_cycle", "installments")),
            Map.entry("catalog_order.catalog_offer", List.of("id", "catalog_version_id", "channel", "offer_status", "version_token")),
            Map.entry("catalog_order.catalog_benefit_line", List.of("catalog_version_id", "entitlement_type", "quantity", "validity_days", "grant_interval_months")));
    private static final String ACL = """
            WITH objects AS (
                SELECT 'schema' AS kind,n.oid,0 AS subid,n.nspname AS identity,'' AS column_name,n.nspacl AS acl FROM pg_namespace n WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema'
                UNION ALL
                SELECT CASE WHEN c.relkind='S' THEN 'sequence' ELSE 'table' END,c.oid,0,n.nspname||'.'||c.relname,'',c.relacl FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema'
                UNION ALL
                SELECT 'column',c.oid,a.attnum,n.nspname||'.'||c.relname,a.attname,a.attacl FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE a.attnum>0 AND NOT a.attisdropped AND n.nspname !~ '^pg_' AND n.nspname<>'information_schema'
                UNION ALL
                SELECT 'routine',p.oid,0,p.oid::regprocedure::text,'',p.proacl FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema'
            )
            SELECT grantee::regrole::text||'|'||kind||'|'||identity||'|'||column_name||'|'||privilege_type||'|'||grantor::regrole::text||'|'||is_grantable
            FROM objects CROSS JOIN LATERAL aclexplode(acl) grants
            WHERE grantee IN ('sharing_role'::regrole,'customer_iam_role'::regrole,'sharing_command_role'::regrole,'evidence_role'::regrole,'sharing_public_read_role'::regrole,'sharing_admission_role'::regrole,'evidence_media_reader_role'::regrole) ORDER BY 1
            """;

    @BeforeEach void start() throws Exception { fixture = new CustomerBusinessNumbersFixture(); fixture.start("014"); }
    @AfterEach void stop() { if (fixture != null) fixture.close(); }
    private Flyway current() { return Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").target("015").load(); }
    private void upgrade() { assertThat(current().migrate().migrationsExecuted).isEqualTo(1); current().validate(); }
    private void latest() {
        var latest = Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").load();
        assertThat(latest.migrate().migrationsExecuted).isEqualTo(4); latest.validate(); assertThat(latest.info().applied()).hasSize(19);
    }
    private List<String> strings(Connection connection, String sql) throws SQLException {
        List<String> values = new ArrayList<>(); try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) { while (rows.next()) values.add(rows.getString(1)); } return values;
    }
    private Set<String> acls() throws Exception { try (var connection = fixture.admin()) { return new TreeSet<>(strings(connection, ACL)); } }
    private String catalog(Connection connection) throws Exception {
        return one(connection, """
                SELECT jsonb_build_array(
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_authid value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_auth_members value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY objoid,classoid) FROM pg_shdescription value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_default_acl value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY setdatabase,setrole) FROM pg_db_role_setting value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_namespace value),
                  (SELECT jsonb_agg(to_jsonb(value)-'relpages'-'reltuples'-'relallvisible'-'relfrozenxid'-'relminmxid' ORDER BY oid) FROM pg_class value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY attrelid,attnum) FROM pg_attribute value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_proc value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_constraint value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_trigger value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY indexrelid) FROM pg_index value))::text
                """);
    }
    private void denied(Connection connection, String sql) throws Exception {
        var savepoint = connection.setSavepoint();
        try { assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("42501")); }
        finally { connection.rollback(savepoint); }
    }
    private void rejects(String mutation, String category) throws Exception {
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false); execute(connection, mutation);
            String role = one(connection, "SELECT quote_ident(current_user)"); execute(connection, "RESET ROLE");
            String before = catalog(connection); execute(connection, "SET LOCAL ROLE " + role); var savepoint = connection.setSavepoint();
            assertThatThrownBy(() -> execute(connection, Files.readString(SCRIPT))).isInstanceOfSatisfying(SQLException.class,
                    error -> { assertThat(error.getSQLState()).isEqualTo("P0001"); assertThat(error.getMessage()).contains(category).doesNotContain(fixture.password); });
            connection.rollback(savepoint); execute(connection, "RESET ROLE"); assertThat(catalog(connection).equals(before)).isTrue(); connection.rollback();
        }
    }
    private void frozenGraph() throws Exception {
        try (var connection = fixture.admin()) {
            var names = List.of("pa27_query", "pa27_customer", "pa27_command"); var slots = List.of("sharing_query", "customer_context", "sharing_command");
            for (int index = 0; index < 3; index++) {
                String name = names.get(index);
                execute(connection, "CREATE ROLE " + name + " LOGIN NOINHERIT PASSWORD '" + fixture.password + "'");
                String marker = one(connection, "SELECT 'onecall:pa25:v1:db='||(SELECT oid FROM pg_database WHERE datname=current_database())||':role='||oid||':slot=" + slots.get(index) + ":name='||rolname FROM pg_roles WHERE rolname='" + name + "'");
                execute(connection, "COMMENT ON ROLE " + name + " IS '" + marker + "'");
            }
            execute(connection, "GRANT sharing_role TO pa27_query WITH ADMIN FALSE, INHERIT FALSE, SET TRUE; GRANT customer_iam_role TO pa27_customer WITH ADMIN FALSE, INHERIT FALSE, SET TRUE; GRANT sharing_command_role,evidence_role TO pa27_command WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
        }
    }

    @Test
    void v014CustomerCannotReadSubjectBindingOrActivationWith42501() throws Exception {
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            for (String sql : List.of("SELECT id FROM subject_number.business_subject", "SELECT number_masked FROM subject_number.caller_number_binding", "SELECT status FROM activation_consent.service_activation")) denied(connection, sql);
            connection.rollback();
        }
    }

    @Test
    void v015FreshAndUpgradeHaveExactFifteenHistoryEntriesAndFrozenBytes() throws Exception {
        Map<String, byte[]> frozen = new TreeMap<>();
        try (var files = Files.list(SCRIPT.getParent())) { for (var file : files.filter(path -> !path.equals(SCRIPT) && !path.getFileName().toString().startsWith("V016__") && !path.getFileName().toString().startsWith("V017__") && !path.getFileName().toString().startsWith("V018__") && !path.getFileName().toString().equals("V019__customer_catalog_offer_read_privileges.sql")).toList()) frozen.put(file.toString(), Files.readAllBytes(file)); }
        upgrade(); assertThat(current().info().applied()).hasSize(15); assertThat(current().info().pending()).isEmpty();
        for (var entry : frozen.entrySet()) assertThat(Files.readAllBytes(Path.of(entry.getKey()))).isEqualTo(entry.getValue());
        try (var connection = fixture.admin()) {
            assertThat(strings(connection, "SELECT version||'|'||script||'|'||checksum FROM public.flyway_schema_history WHERE installed_rank=15")).containsExactly("015|V015__customer_business_number_read_privileges.sql|-1316980027");
        }
        fixture.close(); fixture = new CustomerBusinessNumbersFixture(); fixture.start("015");
        assertThat(current().info().applied()).hasSize(15);
        latest(); fixture.provision(); assertThat(fixture.psql("check").getExitCode()).isZero();
    }

    @Test
    void v015AddsExactlyFourteenOwnerGrantedTuplesAndNothingElse() throws Exception {
        Set<String> before = acls(); assertThat(before).hasSize(247);
        upgrade(); Set<String> after = acls(); assertThat(after).hasSize(261).containsAll(before);
        Set<String> delta = new TreeSet<>(after); delta.removeAll(before);
        assertThat(delta).containsExactlyInAnyOrder(
                "customer_iam_role|schema|subject_number||USAGE|subject_number_owner|false",
                "customer_iam_role|column|subject_number.business_subject|id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.business_subject|owner_account_id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.business_subject|owner_organization_id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.business_subject|real_name_status|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.business_subject|created_at|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.caller_number_binding|id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.caller_number_binding|subject_id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.caller_number_binding|number_masked|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.caller_number_binding|number_type|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.caller_number_binding|status|SELECT|subject_number_owner|false",
                "customer_iam_role|schema|activation_consent||USAGE|activation_consent_owner|false",
                "customer_iam_role|column|activation_consent.service_activation|subject_id|SELECT|activation_consent_owner|false",
                "customer_iam_role|column|activation_consent.service_activation|status|SELECT|activation_consent_owner|false"
        );
        assertThat(after.stream().filter(value -> value.startsWith("customer_iam_role|"))).hasSize(26);
        assertThat(after.stream().filter(value -> value.startsWith("evidence_role|"))).hasSize(16);
        assertThat(after.stream().filter(value -> !value.startsWith("customer_iam_role|") && !value.startsWith("evidence_role|"))).hasSize(219);
    }

    @Test
    void customerRoleReadsOnlyApprovedColumnsAndDeniesEveryOtherCapabilityWith42501() throws Exception {
        Set<String> catalogColumns=new TreeSet<>();
        for(var entry:APPROVED.entrySet()) if(Set.of("catalog_order.catalog_product","catalog_order.catalog_version","catalog_order.catalog_offer","catalog_order.catalog_benefit_line").contains(entry.getKey())) for(String column:entry.getValue()) catalogColumns.add(entry.getKey()+"|"+column);
        assertThat(catalogColumns).hasSize(22).isEqualTo(CustomerCatalogOfferReadPrivilegesIT.catalogDeltaOracle().stream().map(tuple -> { String[] fields=tuple.split("\\|",-1); return fields[2]+"|"+fields[3]; }).collect(java.util.stream.Collectors.toSet()));

        upgrade(); frozenGraph();
        for (boolean currentPhase : List.of(false, true)) {
            Map<String, List<String>> approved = new TreeMap<>(APPROVED);
            if (!currentPhase) approved.keySet().removeAll(Set.of("catalog_order.catalog_product", "catalog_order.catalog_version", "catalog_order.catalog_offer", "catalog_order.catalog_benefit_line"));
            else {
                latest(); fixture.provision();
                for (String tuple : EvidenceRoleIvrAssociationAclIT.currentOracle()) {
                    String[] fields = tuple.split("\\|", -1);
                    if (fields[0].equals("customer_iam_role") && fields[1].equals("column")) {
                        var projected = new ArrayList<>(approved.getOrDefault(fields[2], List.of()));
                        if (!projected.contains(fields[3])) projected.add(fields[3]);
                        approved.put(fields[2], List.copyOf(projected));
                    }
                }
                assertThat(approved.values().stream().mapToInt(List::size).sum()).isEqualTo(69);
            }


        Set<String> before = acls();
        try (var admin = fixture.admin(); var connection = fixture.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            assertThat(one(connection, "SHOW transaction_read_only")).isEqualTo("off");
            for (String table : strings(admin, "SELECT n.nspname||'.'||c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind IN ('r','p') AND n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema') ORDER BY 1")) {
                for (String column : strings(admin, "SELECT attname FROM pg_attribute WHERE attrelid='" + table + "'::regclass AND attnum>0 AND NOT attisdropped ORDER BY attnum")) {
                    if (approved.getOrDefault(table, List.of()).contains(column)) execute(connection, "SELECT " + column + " FROM " + table);
                    else denied(connection, "SELECT " + column + " FROM " + table);
                    denied(connection, "UPDATE " + table + " SET " + column + "=" + column + " WHERE false");
                    denied(connection, "INSERT INTO " + table + " (" + column + ") SELECT " + column + " FROM " + table + " WHERE false");
                }
                for (String sql : List.of("SELECT * FROM " + table, "DELETE FROM " + table + " WHERE false", "TRUNCATE " + table,
                        "CREATE TRIGGER pa27_probe BEFORE UPDATE ON " + table + " FOR EACH ROW EXECUTE FUNCTION customer_iam.guard_customer_account()")) denied(connection, sql);
                if (approved.containsKey(table)) {
                    for (String lock : List.of("UPDATE", "SHARE")) denied(connection, "SELECT " + approved.get(table).getFirst() + " FROM " + table + " FOR " + lock);
                }
            }
            execute(admin, "CREATE TABLE customer_iam.pa27_reference_probe(id uuid)");
            execute(admin, "ALTER TABLE customer_iam.pa27_reference_probe OWNER TO customer_iam_role");
            try {
                for (String target : List.of("customer_iam.customer_account(id)", "organization_access.organization(id)",
                        "organization_access.organization_membership(id)", "subject_number.business_subject(id)",
                        "subject_number.caller_number_binding(id)", "activation_consent.service_activation(subject_id)"))
                    denied(connection, "ALTER TABLE customer_iam.pa27_reference_probe ADD CONSTRAINT pa27_fk FOREIGN KEY (id) REFERENCES " + target);
            } finally { execute(admin, "DROP TABLE customer_iam.pa27_reference_probe"); }
            for (String schema : List.of("customer_iam", "organization_access", "subject_number", "activation_consent", "audit", "platform")) denied(connection, "CREATE TABLE " + schema + ".pa27_probe(id integer)");
            for (String role : List.of("subject_number_owner", "activation_consent_owner", "sharing_role", "evidence_role", "sharing_command_role", "evidence_media_reader_role")) denied(connection, "SET LOCAL ROLE " + role);
            for (String routine : strings(admin, "SELECT format('%I.%I',n.nspname,p.proname)||'('||coalesce((SELECT string_agg('NULL::'||argument::regtype::text,',') FROM unnest(p.proargtypes::oid[]) argument),'')||')' FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema') ORDER BY 1")) denied(connection, "SELECT " + routine);
            for (String sequence : strings(admin, "SELECT schemaname||'.'||sequencename FROM pg_sequences WHERE schemaname !~ '^pg_'")) denied(connection, "SELECT nextval('" + sequence + "')");
            execute(connection, "GRANT SELECT (id) ON subject_number.business_subject TO pa27_query");
            connection.rollback();
        }
        assertThat(acls()).containsExactlyInAnyOrderElementsOf(before);

        }
    }

    @Test
    void v015ReplayPreservesAclsGraphCredentialsDefinitionsAndBusinessData() throws Exception {
        frozenGraph(); upgrade(); fixture.seed();
        String before; try (var connection = fixture.admin()) { before = catalog(connection); }
        String business = fixture.businessSnapshot(); Set<String> acls = acls();
        for (int replay = 0; replay < 2; replay++) {
            try (var connection = fixture.admin()) { connection.setAutoCommit(false); execute(connection, Files.readString(SCRIPT)); connection.commit(); }
            try (var connection = fixture.admin()) { assertThat(catalog(connection).equals(before)).isTrue(); }
            assertThat(fixture.businessSnapshot().equals(business)).isTrue(); assertThat(acls()).containsExactlyInAnyOrderElementsOf(acls);
        }
        assertThat(current().migrate().migrationsExecuted).isZero();
    }

    @Test
    void v015RejectsPartialUnexpectedGrantorGrantOptionAndAuthorityDriftAtomically() throws Exception {
        for (var entry : Map.ofEntries(
                Map.entry("SET LOCAL ROLE subject_number_owner; GRANT USAGE ON SCHEMA subject_number TO customer_iam_role; RESET ROLE", "V015_ACL_REJECTED"),
                Map.entry("GRANT SELECT ON subject_number.business_subject TO customer_iam_role", "V015_ACL_REJECTED"),
                Map.entry("GRANT SELECT (number_hmac) ON subject_number.caller_number_binding TO customer_iam_role", "V015_ACL_REJECTED"),
                Map.entry("SET LOCAL ROLE subject_number_owner; GRANT SELECT (id) ON subject_number.business_subject TO customer_iam_role WITH GRANT OPTION; RESET ROLE", "V015_ACL_REJECTED"),
                Map.entry("GRANT SELECT ON subject_number.business_subject TO PUBLIC", "V015_ACL_REJECTED"),
                Map.entry("ALTER DEFAULT PRIVILEGES FOR ROLE subject_number_owner GRANT SELECT ON TABLES TO customer_iam_role", "V015_DEFAULT_ACL_REJECTED"),
                Map.entry("GRANT pg_read_all_data TO customer_iam_role", "V015_TOPOLOGY_REJECTED"),
                Map.entry("ALTER TABLE subject_number.business_subject OWNER TO customer_iam_owner", "V015_OWNERSHIP_REJECTED"),
                Map.entry("ALTER ROLE customer_iam_role SET search_path TO subject_number", "V015_SETTINGS_REJECTED"),
                Map.entry("CREATE ROLE pa27_partial LOGIN NOINHERIT; COMMENT ON ROLE pa27_partial IS 'onecall:pa25:v1:stale'", "V015_PROVENANCE_REJECTED"),
                Map.entry("SET LOCAL ROLE customer_iam_role", "V015_AUTHORITY_REJECTED")).entrySet()) rejects(entry.getKey(), entry.getValue());
        upgrade();
        rejects("UPDATE pg_attribute SET attacl=ARRAY(SELECT CASE WHEN grant_entry::text LIKE 'customer_iam_role=%' THEN replace(grant_entry::text,'/subject_number_owner','/fixture_admin')::aclitem ELSE grant_entry END FROM unnest(attacl) grant_entry) WHERE attrelid='subject_number.business_subject'::regclass AND attname='id'", "V015_ACL_REJECTED");
        frozenGraph(); rejects("GRANT customer_iam_role TO pa27_customer WITH INHERIT TRUE", "V015_TOPOLOGY_REJECTED");
    }

    @Test
    void pa25ApplyCheckRequireExactV015HistoryAndIndependent261TupleOracle() throws Exception {
        assertThat(fixture.psql("apply").getExitCode()).isNotZero(); assertThat(fixture.psql("check").getExitCode()).isNotZero();
        upgrade(); assertThat(acls()).hasSize(261);
        for (String mode : List.of("apply", "check")) assertThat(fixture.psql(mode).getExitCode()).isNotZero();
        latest(); fixture.provision();
        List<String[]> additions = new ArrayList<>();
        additions.add(new String[]{"subject_number", "", "subject_number_owner"}); additions.add(new String[]{"activation_consent", "", "activation_consent_owner"});
        for (var entry : APPROVED.entrySet()) if (entry.getKey().startsWith("subject_number.") || entry.getKey().startsWith("activation_consent."))
            for (String column : entry.getValue()) additions.add(new String[]{entry.getKey(), column, entry.getKey().startsWith("subject_number.") ? "subject_number_owner" : "activation_consent_owner"});
        for (String[] addition : additions) {
            String target = addition[1].isEmpty() ? "USAGE ON SCHEMA " + addition[0] : "SELECT (" + addition[1] + ") ON " + addition[0];
            try (var connection = fixture.admin()) { execute(connection, "SET ROLE " + addition[2]); execute(connection, "REVOKE " + target + " FROM customer_iam_role"); }
            Set<String> before = acls();
            try { for (String mode : List.of("apply", "check")) { assertThat(fixture.psql(mode).getExitCode()).isNotZero(); assertThat(acls()).containsExactlyInAnyOrderElementsOf(before); } }
            finally { try (var connection = fixture.admin()) { execute(connection, "SET ROLE " + addition[2]); execute(connection, "GRANT " + target + " TO customer_iam_role"); } }
        }
        Set<String> cleanAcls = acls();
        String cleanCatalog; try (var connection = fixture.admin()) { cleanCatalog = catalog(connection); }
        try {
            try (var connection = fixture.admin()) {
                execute(connection, "SET ROLE subject_number_owner");
                execute(connection, "GRANT SELECT (number_hmac) ON subject_number.caller_number_binding TO customer_iam_role");
            }
            Set<String> widenedAcls = acls();
            assertThat(widenedAcls).containsAll(cleanAcls).hasSize(313);
            assertThat(widenedAcls).contains("customer_iam_role|column|subject_number.caller_number_binding|number_hmac|SELECT|subject_number_owner|false");
            String widenedCatalog; try (var connection = fixture.admin()) { widenedCatalog = catalog(connection); }
            for (String mode : List.of("apply", "check")) {
                assertThat(fixture.psql(mode).getExitCode()).isNotZero();
                assertThat(acls()).containsExactlyInAnyOrderElementsOf(widenedAcls);
                try (var connection = fixture.admin()) { assertThat(catalog(connection)).isEqualTo(widenedCatalog); }
            }
        } finally {
            try (var connection = fixture.admin()) {
                execute(connection, "SET ROLE subject_number_owner");
                execute(connection, "REVOKE SELECT (number_hmac) ON subject_number.caller_number_binding FROM customer_iam_role");
            }
        }
        assertThat(acls()).containsExactlyInAnyOrderElementsOf(cleanAcls);
        try (var connection = fixture.admin()) { assertThat(catalog(connection)).isEqualTo(cleanCatalog); }
        assertThat(fixture.psql("check").getExitCode()).isZero();
        assertThat(acls()).containsExactlyInAnyOrderElementsOf(cleanAcls);
        try (var connection = fixture.admin()) { assertThat(catalog(connection)).isEqualTo(cleanCatalog); }
        for (String assignment : List.of("checksum=0", "script='wrong.sql'", "description='wrong'", "type='BASELINE'", "success=false", "version='15'", "installed_rank=1019")) {
            String history; try (var connection = fixture.admin()) { history = one(connection, "SELECT to_jsonb(value)::text FROM public.flyway_schema_history value WHERE installed_rank=15"); execute(connection, "UPDATE public.flyway_schema_history SET " + assignment + " WHERE installed_rank=15"); }
            try { for (String mode : List.of("apply", "check")) assertThat(fixture.psql(mode).getExitCode()).isNotZero(); }
            finally { try (var connection = fixture.admin()) { execute(connection, "DELETE FROM public.flyway_schema_history WHERE version IN ('015','15')"); execute(connection, "INSERT INTO public.flyway_schema_history SELECT * FROM jsonb_populate_record(NULL::public.flyway_schema_history,?::jsonb)", history); } }
        }
        assertThat(fixture.psql("apply").getExitCode()).isZero(); assertThat(fixture.psql("check").getExitCode()).isZero(); assertThat(acls()).hasSize(312);
    }

    @Test
    void allSixLoginBoundariesAndRolePoolRestorationRemainUnchanged() throws Exception {
        frozenGraph(); String identities; try (var connection = fixture.admin()) { identities = one(connection, "SELECT jsonb_agg(to_jsonb(value) ORDER BY oid)::text FROM pg_authid value"); }
        upgrade(); try (var connection = fixture.admin()) { assertThat(one(connection, "SELECT jsonb_agg(to_jsonb(value) ORDER BY oid)::text FROM pg_authid value").equals(identities)).isTrue(); }
        latest(); fixture.provision(); fixture.web(); fixture.seed(); fixture.json("personal", "", 200); fixture.cleanPool();
        fixture.json("org4", "", 403); fixture.cleanPool();
        var result = fixture.psql("check"); assertThat(result.getExitCode()).isZero(); assertThat(result.getStdout().strip()).isEqualTo("PA25_CHECK_OK logins=7 effective_roles=9 memberships=11");
    }
}
