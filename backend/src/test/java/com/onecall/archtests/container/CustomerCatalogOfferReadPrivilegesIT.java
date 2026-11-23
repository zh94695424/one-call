package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadPersistenceConfiguration;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.images.builder.Transferable;
import org.springframework.transaction.support.TransactionTemplate;

public class CustomerCatalogOfferReadPrivilegesIT {
    private static final Path SCRIPT = Path.of("src/main/resources/db/migration/V019__customer_catalog_offer_read_privileges.sql");
    private static final String ACL = """
            WITH objects AS (
                SELECT 'schema' AS kind,n.oid,0 AS subid,n.nspname::text AS identity,'' AS column_name,n.nspacl AS acl FROM pg_namespace n WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema'
                UNION ALL
                SELECT CASE WHEN c.relkind='S' THEN 'sequence' ELSE 'table' END,c.oid,0,n.nspname||'.'||c.relname,'',c.relacl FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema'
                UNION ALL
                SELECT 'column',c.oid,a.attnum,n.nspname||'.'||c.relname,a.attname,a.attacl FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE a.attnum>0 AND NOT a.attisdropped AND n.nspname !~ '^pg_' AND n.nspname<>'information_schema'
                UNION ALL
                SELECT 'routine',p.oid,0,p.oid::regprocedure::text,'',p.proacl FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema'
            )
            SELECT grantee::regrole::text||'|'||kind||'|'||identity||'|'||column_name||'|'||privilege_type||'|'||grantor::regrole::text||'|'||is_grantable
            FROM objects CROSS JOIN LATERAL aclexplode(acl) grants
            WHERE grantee IN ('sharing_role'::regrole,'customer_iam_role'::regrole,'sharing_command_role'::regrole,'evidence_role'::regrole,'sharing_public_read_role'::regrole,'sharing_admission_role'::regrole,'evidence_media_reader_role'::regrole,'operations_iam_role'::regrole,'ivr_role'::regrole) ORDER BY 1
            """;
    private CustomerBusinessNumbersFixture fixture;
    private CustomerBusinessNumbersFixture seededFixture;
    static Set<String> catalogDeltaOracle() {
        return Set.of(
            "customer_iam_role|column|catalog_order.catalog_benefit_line|catalog_version_id|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_benefit_line|entitlement_type|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_benefit_line|grant_interval_months|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_benefit_line|quantity|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_benefit_line|validity_days|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_offer|catalog_version_id|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_offer|channel|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_offer|id|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_offer|offer_status|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_offer|version_token|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_product|id|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_product|product_code|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_version|audience|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_version|billing_cycle|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_version|currency|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_version|id|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_version|installments|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_version|offer_type|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_version|price_cents|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_version|product_id|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_version|sale_status|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.catalog_version|version_no|SELECT|catalog_order_owner|false");
    }
    static Set<String> current432Oracle() {
        Set<String> baseline = EvidenceRoleIvrAssociationAclIT.currentOracle();
        Set<String> delta = catalogDeltaOracle();
        assertThat(baseline).hasSize(410);
        assertThat(delta).hasSize(22).doesNotContainAnyElementsOf(baseline);
        Set<String> result = new TreeSet<>(baseline);
        result.addAll(delta);
        assertThat(result).hasSize(432);
        assertThat(result.stream().filter(value -> value.startsWith("customer_iam_role|"))).hasSize(75);
        assertThat(result.stream().filter(value -> !value.startsWith("customer_iam_role|"))).hasSize(357);
        Set<String> recovered = new TreeSet<>(result); recovered.removeAll(delta);
        assertThat(recovered).isEqualTo(baseline);
        return Set.copyOf(result);
    }
    static String historicalV018Pa25Script() throws Exception {
        String current = Files.readString(Path.of("ops/postgresql/pa25-runtime-identities.sql")).replace("\r\n", "\n");
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        int frozenStart = current.indexOf("$frozen$") + "$frozen$".length();
        int frozenEnd = current.indexOf("$frozen$", frozenStart);
        String frozen = current.substring(frozenStart, frozenEnd);
        var rows = mapper.readTree(frozen);
        assertThat(rows.size()).isEqualTo(432);
        Set<String> appended = new TreeSet<>();
        for (int index = 410; index < 432; index++) {
            var row = rows.get(index);
            appended.add(row.get(0).asString()+"|"+row.get(1).asString()+"|"+row.get(2).asString()+"|"+row.get(3).asString()+"|"+row.get(4).asString()+"|"+row.get(5).asString()+"|"+row.get(6).asBoolean());
        }
        assertThat(appended).isEqualTo(catalogDeltaOracle());
        int deltaStart = frozen.indexOf(",\n  [\n    \"customer_iam_role\",\n    \"column\",\n    \"catalog_order.catalog_benefit_line\"");
        assertThat(deltaStart).isPositive();
        String historicalFrozen = frozen.substring(0, deltaStart) + "\n]";
        assertThat(mapper.readTree(historicalFrozen).size()).isEqualTo(410);
        String historical = current.substring(0, frozenStart) + historicalFrozen + current.substring(frozenEnd);
        int historyStart = historical.indexOf("$history$") + "$history$".length();
        int historyEnd = historical.indexOf("$history$", historyStart);
        String history = historical.substring(historyStart, historyEnd);
        var historyRows = mapper.readTree(history);
        assertThat(historyRows.size()).isEqualTo(19);
        assertThat(historyRows.get(18).get(0).asString()).isEqualTo("019");
        assertThat(historyRows.get(18).get(1).asString()).isEqualTo(SCRIPT.getFileName().toString());
        int selfStart = history.indexOf(",\n  [\n    \"019\",");
        assertThat(selfStart).isPositive();
        historical = historical.substring(0, historyStart) + history.substring(0, selfStart) + "\n]" + historical.substring(historyEnd);
        for (String token : List.of("jsonb_array_length(expected_resolved) <> 432", "count(DISTINCT value) FROM jsonb_array_elements(expected_resolved)) <> 432", "jsonb_array_length(coalesce(actual_acls, '[]'::jsonb)) <> 432", "count(*) FROM public.flyway_schema_history) <> 19")) {
            assertThat(historical.indexOf(token)).isEqualTo(historical.lastIndexOf(token)).isNotNegative();
            historical = historical.replace(token, token.endsWith("432") ? token.substring(0, token.length()-3)+"410" : token.substring(0, token.length()-2)+"18");
        }
        assertThat(sha(historical.getBytes(StandardCharsets.UTF_8))).isEqualTo("8f02d4d4842e734f10eac0c15b431afa16eb062790773b8696ae0de67d63b451");
        String raw = historical.replace("\n", "\r\n");
        assertThat(sha(raw.getBytes(StandardCharsets.UTF_8))).isEqualTo("b5e4bbdea2830effbbd244f15f00d2f69a950e625cae1a4cf51559bb29eddcc9");
        return raw;
    }
    private static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static List<String> strings(Connection connection, String sql) throws Exception {
        List<String> result = new ArrayList<>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) { while (rows.next()) result.add(rows.getString(1)); }
        return result;
    }
    private Set<String> acls() throws Exception { try (var connection = fixture.admin()) { return new TreeSet<>(strings(connection, ACL)); } }
    @BeforeEach void start() throws Exception { fixture = new CustomerBusinessNumbersFixture(); fixture.start("018"); seed(); }
    @AfterEach void stop() { if (fixture != null) fixture.close(); }

    private void upgrade() throws Exception { assertThat(Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").target("019").load().migrate().migrationsExecuted).isEqualTo(1); Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").target("019").load().validate(); assertThat(acls()).isEqualTo(current432Oracle()); }
    private void provisionHistorical() throws Exception {
        fixture.postgres.copyFileToContainer(Transferable.of(historicalV018Pa25Script().getBytes(StandardCharsets.UTF_8)), "/tmp/pa40a-old.sql");
        for (String mode : List.of("apply", "check")) assertThat(fixture.postgres.execInContainer("psql", "-X", "-w", "-v", "ON_ERROR_STOP=1", "-U", "fixture_admin", "-d", "pa27", "-v", "mode="+mode, "-v", "sharing_login=pa27_query", "-v", "customer_login=pa27_customer", "-v", "command_login=pa27_command", "-v", "operations_login=pa36_operations", "-f", "/tmp/pa40a-old.sql").getExitCode()).isZero();
        try (var connection = fixture.admin()) { execute(connection, "ALTER ROLE pa27_customer PASSWORD '"+fixture.password+"'"); }
    }
    private Connection customer() throws Exception {
        Connection connection = fixture.runtime(); connection.setAutoCommit(false);
        assertThat(one(connection, "SELECT current_user||'|'||session_user")).isEqualTo("pa27_customer|pa27_customer");
        execute(connection, "SET LOCAL ROLE customer_iam_role"); return connection;
    }
    private void denied(Connection connection, String sql) throws Exception {
        var savepoint = connection.setSavepoint();
        try { assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501")); }
        finally { connection.rollback(savepoint); }
    }
    private String snapshot(Connection connection) throws Exception {
        String catalog = one(connection, """
                SELECT encode(sha256(convert_to(jsonb_build_array(
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
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY indexrelid) FROM pg_index value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_database value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_type value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_attrdef value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_rewrite value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_event_trigger value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY classid,objid,objsubid,refclassid,refobjid,refobjsubid,deptype) FROM pg_depend value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY dbid,classid,objid,objsubid,refclassid,refobjid,deptype) FROM pg_shdepend value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY installed_rank) FROM public.flyway_schema_history value))::text, 'UTF8')), 'hex')
                """);
        List<String> business = new ArrayList<>();
        for (String table : strings(connection, "SELECT quote_ident(n.nspname)||'.'||quote_ident(c.relname) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind='r' AND n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema') ORDER BY 1")) business.addAll(strings(connection, "SELECT row_to_json(fact)::text FROM " + table + " fact ORDER BY row_to_json(fact)::text COLLATE \"C\""));
        return catalog + business;
    }
    private String invariantSnapshot(Connection connection) throws Exception {
        String catalog = one(connection, """
                SELECT encode(sha256(convert_to(jsonb_build_array(
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_authid value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_auth_members value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY objoid,classoid) FROM pg_shdescription value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_default_acl value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY setdatabase,setrole) FROM pg_db_role_setting value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_namespace value),
                  (SELECT jsonb_agg(to_jsonb(value)-'relpages'-'reltuples'-'relallvisible'-'relfrozenxid'-'relminmxid' ORDER BY oid) FROM pg_class value),
                  (SELECT jsonb_agg(to_jsonb(value)-'attacl' ORDER BY attrelid,attnum) FROM pg_attribute value),
                  (SELECT jsonb_agg(jsonb_build_array(attribute.attrelid,attribute.attnum,acl.grantee,acl.grantor,acl.privilege_type,acl.is_grantable) ORDER BY attribute.attrelid,attribute.attnum,acl.grantee,acl.grantor,acl.privilege_type,acl.is_grantable)
                    FROM pg_attribute attribute CROSS JOIN LATERAL aclexplode(attribute.attacl) acl
                    WHERE NOT (acl.grantee='customer_iam_role'::regrole AND acl.grantor='catalog_order_owner'::regrole AND acl.privilege_type='SELECT' AND NOT acl.is_grantable
                      AND EXISTS(SELECT 1 FROM jsonb_array_elements('%s'::jsonb) expected WHERE to_regclass(expected->>2)=attribute.attrelid AND expected->>3=attribute.attname))),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_proc value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_constraint value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_trigger value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY indexrelid) FROM pg_index value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_database value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_type value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_attrdef value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_rewrite value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_event_trigger value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY classid,objid,objsubid,refclassid,refobjid,refobjsubid,deptype) FROM pg_depend value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY dbid,classid,objid,objsubid,refclassid,refobjid,deptype) FROM pg_shdepend value WHERE NOT (refclassid='pg_authid'::regclass AND refobjid='customer_iam_role'::regrole AND deptype='a' AND classid='pg_class'::regclass
                    AND EXISTS(SELECT 1 FROM jsonb_array_elements('%s'::jsonb) expected WHERE to_regclass(expected->>2)=value.objid AND (SELECT attnum FROM pg_attribute WHERE attrelid=value.objid AND attname=expected->>3)=value.objsubid))),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY installed_rank) FROM public.flyway_schema_history value WHERE installed_rank<=18))::text, 'UTF8')), 'hex')
                """.formatted(deltaJson(),deltaJson()));
        List<String> business = new ArrayList<>();
        for (String table : strings(connection, "SELECT quote_ident(n.nspname)||'.'||quote_ident(c.relname) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind='r' AND n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema') ORDER BY 1")) business.addAll(strings(connection, "SELECT row_to_json(fact)::text FROM " + table + " fact ORDER BY row_to_json(fact)::text COLLATE \"C\""));
        return catalog + business;
    }
    private String deltaJson() {
        var rows=catalogDeltaOracle().stream().sorted().map(tuple -> Arrays.asList(tuple.split("\\|",-1))).toList();
        return tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(rows);
    }
    private void injectedFailure(String script,String category) throws Exception {
        try(var connection=fixture.admin()) {
            connection.setAutoCommit(false); String before=snapshot(connection); var savepoint=connection.setSavepoint();
            assertThatThrownBy(() -> execute(connection,script)).isInstanceOfSatisfying(SQLException.class,failure -> { assertThat(failure.getSQLState()).isEqualTo("P0001"); assertThat(failure.getMessage()).contains("V019_"+category+"_REJECTED").doesNotContain("PRIVATE_CANARY",fixture.password); });
            connection.rollback(savepoint); assertThat(snapshot(connection).equals(before)).isTrue(); connection.rollback();
        }
    }


    private void rejects(String mutation, String category) throws Exception {
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false); execute(connection, mutation);
            String role = one(connection, "SELECT quote_ident(current_user)"); execute(connection, "RESET ROLE");
            String before = snapshot(connection); execute(connection, "SET LOCAL ROLE "+role);
            var savepoint = connection.setSavepoint();
            assertThatThrownBy(() -> execute(connection, Files.readString(SCRIPT))).isInstanceOfSatisfying(SQLException.class, failure -> {
                assertThat(failure.getSQLState()).isEqualTo("P0001");
                assertThat(failure.getMessage()).contains("V019_"+category+"_REJECTED").doesNotContain(fixture.password);
            });
            connection.rollback(savepoint); execute(connection, "RESET ROLE");
            assertThat(snapshot(connection).equals(before)).isTrue(); connection.rollback();
        }
    }
    private void replay(Connection connection) throws Exception { execute(connection, Files.readString(SCRIPT)); }
    private Map<String, Set<String>> approved() {
        Map<String, Set<String>> result = new TreeMap<>();
        for (String tuple : catalogDeltaOracle()) { String[] fields = tuple.split("\\|", -1); result.computeIfAbsent(fields[2], key -> new TreeSet<>()).add(fields[3]); }
        return result;
    }
    private void seed() throws Exception {
        if(seededFixture==fixture) return;
        seededFixture=fixture;
        fixture.seed();
        UUID employee = UUID.randomUUID();
        try (var connection = fixture.admin()) {
            execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role,status) VALUES (?,repeat('a',64),'fixture','fixture',repeat('b',64),'PRIVATE_EMPLOYEE','ADMIN','ACTIVE')", employee);
            execute(connection, "INSERT INTO platform.idempotency_record(id,scope,idempotency_key,request_fingerprint) VALUES (?,'PURCHASE',?,repeat('a',64))", UUID.randomUUID(), UUID.randomUUID().toString());
            execute(connection, "SELECT platform.outbox_append('OPERATIONS',?,'CATALOG',?,'PA40A_CANARY','{}'::jsonb,NULL,repeat('a',64))", UUID.randomUUID().toString(), UUID.randomUUID());
            execute(connection, "SELECT audit.append_fact(now(),'SYSTEM',NULL,'PA40A_CANARY','SYSTEM',NULL,NULL,'{}'::jsonb,'{}'::jsonb,NULL,NULL)");
            int index = 0;
            for (String type : List.of("SUBSCRIPTION", "ONE_TIME", "ADD_ON", "QUOTE")) for (String status : List.of("ACTIVE", "DRAFT", "OFFSALE")) {
                UUID product = UUID.randomUUID(), version = UUID.randomUUID();
                execute(connection, "INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (?,?,'STANDARD')", product, "PA40A_"+(++index));
                execute(connection, "INSERT INTO catalog_order.catalog_version(id,product_id,version_no,sale_status,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,created_by_employee_id,published_at) VALUES (?,?,1,?,?,?,?,'CNY',?,1,?,'{}',repeat('a',64),?,?::timestamptz)", version, product, status, type, type.equals("QUOTE") ? "ENTERPRISE" : "PERSONAL", type.equals("QUOTE") ? null : 1200L, type.equals("QUOTE") ? "QUOTE" : type.equals("SUBSCRIPTION") ? "MONTHLY" : "ONCE", type.equals("SUBSCRIPTION") ? 1 : 0, employee, status.equals("DRAFT") ? null : "2026-11-23T00:00:00Z");
                for (String channel : List.of("MINIAPP", "WEB")) execute(connection, "INSERT INTO catalog_order.catalog_offer(id,catalog_version_id,channel,offer_status,version_token) VALUES (?,?,?,?,?)", UUID.randomUUID(), version, channel, channel.equals("WEB") ? "WITHDRAWN" : "ACTIVE", "stored-"+index+"-"+channel);
                if (index != 12) execute(connection, "INSERT INTO catalog_order.catalog_benefit_line(id,catalog_version_id,entitlement_type,quantity,validity_days,grant_interval_months) VALUES (?,?,'EVIDENCE_COUNT',3,30,2)", UUID.randomUUID(), version);
            }
        }
    }
    @Test void v018CustomerLoginDeniesAllTwentyTwoCatalogColumns() throws Exception {
        provisionHistorical();
        try (var connection = customer()) { for (var table : approved().entrySet()) for (String column : table.getValue()) denied(connection, "SELECT "+column+" FROM "+table.getKey()); }
    }
    @Test void freshAndUpgradeReachExactlyNineteenSuccessfulMigrations() throws Exception {
        provisionHistorical(); upgrade();
        Set<String> upgraded = acls(); assertThat(Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").target("019").load().info().applied()).hasSize(19);
        assertThat(Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").target("019").load().migrate().migrationsExecuted).isZero();
        fixture.close(); fixture = new CustomerBusinessNumbersFixture(); fixture.start(null);
        assertThat(Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").target("019").load().info().applied()).hasSize(19); assertThat(acls()).isEqualTo(upgraded);
    }
    @Test void v019AddsOnlyTwentyTwoOwnerIssuedTuplesAndMatches432Oracle() throws Exception {
        Set<String> before = acls(); assertThat(before).isEqualTo(EvidenceRoleIvrAssociationAclIT.currentOracle());
        upgrade(); Set<String> delta = new TreeSet<>(acls()); delta.removeAll(before);
        assertThat(delta).isEqualTo(catalogDeltaOracle()); assertThat(acls()).containsAll(before);
    }
    @Test void customerLoginReadsEveryApprovedColumnAndExactStoredProjection() throws Exception {
        upgrade(); fixture.provision(); seed();
        String projection="""
            SELECT row_to_json(projection)::text FROM (
                SELECT product.id AS product_id,product.product_code,version.id AS version_id,
                    version.product_id AS version_product_id,version.version_no,version.sale_status,
                    version.offer_type,version.audience,version.price_cents,version.currency,
                    version.billing_cycle,version.installments,offer.id AS offer_id,
                    offer.catalog_version_id AS offer_version_id,offer.channel,offer.offer_status,offer.version_token,
                    benefit.catalog_version_id AS benefit_version_id,benefit.entitlement_type,
                    benefit.quantity,benefit.validity_days,benefit.grant_interval_months
                FROM catalog_order.catalog_product product
                JOIN catalog_order.catalog_version version ON version.product_id=product.id
                JOIN catalog_order.catalog_offer offer ON offer.catalog_version_id=version.id
                LEFT JOIN catalog_order.catalog_benefit_line benefit ON benefit.catalog_version_id=version.id
                ORDER BY product.product_code ASC,version.version_no DESC,offer.id ASC,benefit.entitlement_type ASC
            ) projection
            """;
        try (var connection = customer()) {
            try(var admin=fixture.admin()) {
                var expected=strings(admin,projection); var actual=strings(connection,projection);
                assertThat(actual.equals(expected)).isTrue(); assertThat(actual).hasSize(24);
                assertThat(actual.stream().filter(row -> row.contains("\"offer_type\":\"QUOTE\"") && row.contains("\"price_cents\":null"))).hasSize(6);
                assertThat(actual.stream().filter(row -> row.contains("\"benefit_version_id\":null"))).hasSize(2);
            }
            for (var table : approved().entrySet()) for (String column : table.getValue()) execute(connection, "SELECT "+column+" FROM "+table.getKey());
            assertThat(one(connection, "SELECT count(*) FROM catalog_order.catalog_product product JOIN catalog_order.catalog_version version ON version.product_id=product.id JOIN catalog_order.catalog_offer offer ON offer.catalog_version_id=version.id LEFT JOIN catalog_order.catalog_benefit_line benefit ON benefit.catalog_version_id=version.id WHERE version.sale_status='ACTIVE' AND offer.offer_status='ACTIVE' AND offer.channel='MINIAPP' AND version.currency='CNY' AND offer.version_token LIKE 'stored-%' AND (benefit.catalog_version_id IS NULL OR (benefit.quantity=3 AND benefit.validity_days=30 AND benefit.grant_interval_months=2))")).isEqualTo("4");
        }
    }
    @Test void customerLoginDeniesEveryAdjacentColumnWith42501() throws Exception {
        upgrade(); fixture.provision(); int allowed = 0, denied = 0;
        try (var connection = customer(); var admin = fixture.admin()) {
            for (var table : approved().entrySet()) {
                assertThat(one(admin, "SELECT has_table_privilege('customer_iam_role','"+table.getKey()+"','SELECT')")).isEqualTo("f");
                for (String column : strings(admin, "SELECT attname FROM pg_attribute WHERE attrelid='"+table.getKey()+"'::regclass AND attnum>0 AND NOT attisdropped ORDER BY attnum")) {
                    if (table.getValue().contains(column)) { execute(connection, "SELECT "+column+" FROM "+table.getKey()); allowed++; }
                    else { denied(connection, "SELECT "+column+" FROM "+table.getKey()); denied++; }
                }
                denied(connection, "SELECT * FROM "+table.getKey()); denied(connection, "SELECT fact FROM "+table.getKey()+" fact");
            }
        }
        assertThat(allowed).isEqualTo(22); assertThat(denied).isEqualTo(12);
    }
    @Test void customerLoginDeniesWritesDdlSequencesRoutinesAndUnrelatedObjects() throws Exception {
        upgrade(); fixture.provision(); seed();
        String business = fixture.businessSnapshot(); String invariants; try(var admin=fixture.admin()) { invariants=invariantSnapshot(admin); }
        try (var admin = fixture.admin()) { execute(admin, "SET ROLE catalog_order_owner; CREATE SEQUENCE catalog_order.pa40a_sequence; RESET ROLE"); }
        try (var connection = customer()) {
            assertThat(connection.isReadOnly()).isFalse();
            for (String table : approved().keySet()) {
                try(var admin=fixture.admin()) {
                    for(String column:strings(admin,"SELECT attname FROM pg_attribute WHERE attrelid='"+table+"'::regclass AND attnum>0 AND NOT attisdropped")) {
                        denied(connection,"UPDATE "+table+" SET "+column+"="+column);
                        denied(connection,"INSERT INTO "+table+" ("+column+") SELECT "+column+" FROM "+table+" WHERE false");
                    }
                }
                denied(connection,"CREATE TRIGGER pa40a_probe BEFORE UPDATE ON "+table+" FOR EACH ROW EXECUTE FUNCTION catalog_order.reject_row_mutation()");
                for(String lock:List.of("UPDATE","SHARE")) denied(connection,"SELECT "+approved().get(table).iterator().next()+" FROM "+table+" FOR "+lock);
                for (String sql : List.of("INSERT INTO "+table+" DEFAULT VALUES", "UPDATE "+table+" SET "+approved().get(table).iterator().next()+"="+approved().get(table).iterator().next(), "DELETE FROM "+table, "DELETE FROM "+table+" RETURNING *", "TRUNCATE "+table, "ALTER TABLE "+table+" ADD COLUMN pa40a integer", "DROP TABLE "+table, "CREATE INDEX pa40a_index ON "+table+" ("+approved().get(table).iterator().next()+")")) denied(connection, sql);
            }
            for (String sql : List.of("CREATE TABLE catalog_order.pa40a_table(id integer)", "SELECT nextval('catalog_order.pa40a_sequence')", "SELECT currval('catalog_order.pa40a_sequence')", "SELECT setval('catalog_order.pa40a_sequence',1)", "SELECT * FROM catalog_order.payment_order", "SELECT * FROM catalog_order.payment_event", "SELECT * FROM operations_iam.employee_account", "SELECT * FROM platform.idempotency_record", "SELECT * FROM platform.outbox_event", "SELECT * FROM audit.audit_fact", "SELECT pg_read_file('postgresql.conf')")) denied(connection, sql);
        }
        try(var admin=fixture.admin();var connection=customer()) {
            execute(admin,"CREATE TABLE catalog_order.pa40a_reference(id uuid); ALTER TABLE catalog_order.pa40a_reference OWNER TO customer_iam_role");
            try { for(String target:List.of("catalog_order.catalog_product(id)","catalog_order.catalog_version(id)","catalog_order.catalog_offer(id)","catalog_order.catalog_benefit_line(id)")) denied(connection,"ALTER TABLE catalog_order.pa40a_reference ADD CONSTRAINT pa40a_fk FOREIGN KEY(id) REFERENCES "+target); }
            finally { execute(admin,"DROP TABLE catalog_order.pa40a_reference"); }
            for(String routine:strings(admin,"SELECT p.oid::regprocedure::text FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema')")) assertThat(one(admin,"SELECT has_function_privilege('customer_iam_role','"+routine+"','EXECUTE')")).isEqualTo("f");
            for(String routine:strings(admin,"SELECT format('%I.%I',n.nspname,p.proname)||'('||coalesce((SELECT string_agg('NULL::'||argument::regtype::text,',') FROM unnest(p.proargtypes::oid[]) argument),'')||')' FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema') AND p.prorettype<>'trigger'::regtype")) denied(connection,"SELECT "+routine);
        }
        try(var admin=fixture.admin()) { execute(admin,"DROP SEQUENCE catalog_order.pa40a_sequence"); }
        assertThat(fixture.businessSnapshot().equals(business)).isTrue(); try(var admin=fixture.admin()) { assertThat(invariantSnapshot(admin).equals(invariants)).isTrue(); }
    }
    @Test void customerLoginCannotEscalateOrGrantPrivileges() throws Exception {
        upgrade(); fixture.provision();
        try (var connection = fixture.runtime()) { connection.setAutoCommit(false); for (var table : approved().entrySet()) denied(connection, "SELECT "+table.getValue().iterator().next()+" FROM "+table.getKey()); }
        try (var connection = customer()) {
            try(var admin=fixture.admin()) { for(String role:strings(admin,"SELECT quote_ident(rolname) FROM pg_roles WHERE rolname NOT IN ('pa27_customer','customer_iam_role') ORDER BY rolname")) denied(connection,"SET LOCAL ROLE "+role); }
            denied(connection,"GRANT catalog_order_role TO pa27_query");
            denied(connection, "CREATE ROLE pa40a_escalation");
            for (var table : approved().entrySet()) {
                var savepoint=connection.setSavepoint(); execute(connection,"GRANT SELECT ("+table.getValue().iterator().next()+") ON "+table.getKey()+" TO pa27_query");
                try(var admin=fixture.admin()) { assertThat(one(admin,"SELECT has_column_privilege('pa27_query','"+table.getKey()+"','"+table.getValue().iterator().next()+"','SELECT')")).isEqualTo("f"); }
                connection.rollback(savepoint);
            }
        }
        assertThat(acls()).isEqualTo(current432Oracle());
    }
    @Test void v019ReplaysCleanAndCompleteStatesWithoutFurtherChanges() throws Exception {
        for(int topology=0;topology<3;topology++) {
            if(topology>0) { fixture.close(); fixture=new CustomerBusinessNumbersFixture(); fixture.start("018"); seed(); }

        if(topology==1) historicalThreeSlotGraph();
        if(topology==2) provisionHistorical();
        seed(); String invariant; try(var connection=fixture.admin()) { invariant=invariantSnapshot(connection); }
        try (var connection = fixture.admin()) { connection.setAutoCommit(false); replay(connection); connection.commit(); }
        try(var connection=fixture.admin()) { assertThat(invariantSnapshot(connection).equals(invariant)).isTrue(); }
        assertThat(acls()).isEqualTo(current432Oracle());
        for (int phase = 0; phase < 2; phase++) {
            if (phase == 1) upgrade();
            try (var connection = fixture.admin()) { String before = snapshot(connection); connection.setAutoCommit(false); replay(connection); connection.commit(); assertThat(snapshot(connection).equals(before)).isTrue(); }
        }

        }
    }
    @Test void v019RejectsPartialExtraWrongGrantorAndGrantOptionStatesAtomically() throws Exception {
        List<String> ordered = catalogDeltaOracle().stream().sorted().toList();
        for(int size=1;size<22;size++) {
            StringBuilder partial=new StringBuilder("SET LOCAL ROLE catalog_order_owner;");
            for(String tuple:ordered.subList(0,size)) { String[] fields=tuple.split("\\|",-1); partial.append(" GRANT SELECT (").append(fields[3]).append(") ON ").append(fields[2]).append(" TO customer_iam_role;"); }
            partial.append(" RESET ROLE"); rejects(partial.toString(),"ACL");
        }

        for (String mutation : List.of("SET LOCAL ROLE catalog_order_owner; GRANT SELECT(id) ON catalog_order.catalog_product TO customer_iam_role; RESET ROLE", "GRANT SELECT ON catalog_order.catalog_product TO customer_iam_role", "SET LOCAL ROLE catalog_order_owner; GRANT SELECT(product_family) ON catalog_order.catalog_product TO customer_iam_role; RESET ROLE", "GRANT SELECT(id) ON catalog_order.catalog_product TO customer_iam_role WITH GRANT OPTION")) rejects(mutation, "ACL");
        upgrade();
        rejects("SET LOCAL ROLE catalog_order_owner; REVOKE SELECT(id) ON catalog_order.catalog_product FROM customer_iam_role; RESET ROLE", "ACL");
        rejects("UPDATE pg_attribute SET attacl=ARRAY(SELECT CASE WHEN entry::text LIKE 'customer_iam_role=%' THEN replace(entry::text,'/catalog_order_owner','/fixture_admin')::aclitem ELSE entry END FROM unnest(attacl) entry) WHERE attrelid='catalog_order.catalog_product'::regclass AND attname='id'", "ACL");
        StringBuilder old=new StringBuilder("SET LOCAL ROLE catalog_order_owner;");
        for(var table:approved().entrySet()) old.append(" REVOKE SELECT (").append(String.join(",",table.getValue())).append(") ON ").append(table.getKey()).append(" FROM customer_iam_role;");
        old.append(" RESET ROLE"); rejects(old.toString(),"ACL");
    }
    @Test void v019RejectsAuthorityHistoryProvenanceAndTopologyDrift() throws Exception {
        rejects("SET LOCAL ROLE customer_iam_role", "AUTHORITY");
        for (int rank = 1; rank <= 18; rank++) rejects("DELETE FROM public.flyway_schema_history WHERE installed_rank="+rank, "HISTORY");
        rejects("CREATE ROLE pa40a_forged LOGIN; COMMENT ON ROLE pa40a_forged IS 'onecall:pa25:forged'", "PROVENANCE");
        rejects("GRANT pg_read_all_data TO customer_iam_role", "TOPOLOGY");
        rejects("ALTER ROLE customer_iam_role BYPASSRLS", "ATTRIBUTES");
        upgrade(); rejects("UPDATE public.flyway_schema_history SET version='020' WHERE installed_rank=19", "HISTORY");
        rejects("UPDATE public.flyway_schema_history SET installed_rank=1019 WHERE installed_rank=19", "HISTORY");
        for(String version:List.of("019","020")) rejects("INSERT INTO public.flyway_schema_history SELECT 1019,'"+version+"',description,type,script,checksum,installed_by,installed_on,execution_time,success FROM public.flyway_schema_history WHERE installed_rank=19","HISTORY");
    }
    @Test void v019RejectsOwnerSettingsPublicDefaultsAndDependencyDrift() throws Exception {
        rejects("ALTER TABLE catalog_order.catalog_product OWNER TO customer_iam_owner", "OWNERSHIP");
        rejects("ALTER ROLE customer_iam_role SET search_path TO catalog_order", "SETTINGS");
        rejects("GRANT SELECT ON catalog_order.catalog_product TO PUBLIC", "ACL");
        rejects("ALTER DEFAULT PRIVILEGES FOR ROLE catalog_order_owner GRANT SELECT ON TABLES TO customer_iam_role", "DEFAULT_ACL");
        rejects("GRANT CREATE ON DATABASE pa27 TO customer_iam_role", "DATABASE");
        rejects("INSERT INTO pg_shdepend(dbid,classid,objid,objsubid,refclassid,refobjid,deptype) SELECT oid,'pg_class'::regclass,'catalog_order.catalog_product'::regclass,0,'pg_authid'::regclass,'customer_iam_role'::regrole,'o' FROM pg_database WHERE datname=current_database()", "DEPENDENCY");
    }
    @Test void pa25ApplyAndCheckRequireNineteenHistoryAndExact432Oracle() throws Exception {
        for (String mode : List.of("apply", "check")) assertThat(fixture.psql(mode).getExitCode()).isNotZero();
        upgrade(); fixture.provision();
        for (String tuple : current432Oracle()) {
            String[] fields = tuple.split("\\|", -1);
            String target = switch (fields[1]) { case "schema" -> "USAGE ON SCHEMA "+fields[2]; case "column" -> fields[4]+" ("+fields[3]+") ON "+fields[2]; case "routine" -> "EXECUTE ON FUNCTION "+fields[2]; default -> fields[4]+" ON "+fields[2]; };
            try (var connection = fixture.admin()) { execute(connection, "SET ROLE "+fields[5]); execute(connection, "REVOKE "+target+" FROM "+fields[0]); }
            try { for (String mode : List.of("apply", "check")) { String before; try (var connection = fixture.admin()) { before = snapshot(connection); } assertThat(fixture.psql(mode).getExitCode()).isNotZero(); try (var connection = fixture.admin()) { assertThat(snapshot(connection).equals(before)).isTrue(); } } }
            finally { try (var connection = fixture.admin()) { execute(connection, "SET ROLE "+fields[5]); execute(connection, "GRANT "+target+" TO "+fields[0]); } }
        }
        assertThat(acls()).isEqualTo(current432Oracle());
    }
    @Test void allSevenLoginsElevenMembershipEdgesAndCredentialsRemainUnchanged() throws Exception {
        provisionHistorical(); String before;
        try (var connection = fixture.admin()) { before = one(connection, "SELECT jsonb_agg(to_jsonb(role) ORDER BY oid)::text FROM pg_authid role")+one(connection, "SELECT jsonb_agg(to_jsonb(edge) ORDER BY oid)::text FROM pg_auth_members edge"); }
        upgrade();
        for (String mode : List.of("apply", "check", "apply", "check")) { var result = fixture.psql(mode); assertThat(result.getExitCode()).isZero(); assertThat(result.getStdout().strip()).isEqualTo("PA25_"+mode.toUpperCase(Locale.ROOT)+"_OK logins=7 effective_roles=9 memberships=11"); }
        try (var connection = fixture.admin()) { assertThat((one(connection, "SELECT jsonb_agg(to_jsonb(role) ORDER BY oid)::text FROM pg_authid role")+one(connection, "SELECT jsonb_agg(to_jsonb(edge) ORDER BY oid)::text FROM pg_auth_members edge")).equals(before)).isTrue(); }
    }
    @Test void customerPoolRestoresRoleTransactionAndSettingsOnSuccessAndFailure() throws Exception {
        upgrade(); fixture.provision(); fixture.web();
        var transaction = fixture.context.getBean("customerQueryTransactionTemplate", TransactionTemplate.class);
        for (boolean fail : List.of(false, true)) {
            try { transaction.execute(status -> { var pool=fixture.context.getBean("customerQueryDataSource",com.zaxxer.hikari.HikariDataSource.class); var connection=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(pool); try { execute(connection,"SET LOCAL ROLE customer_iam_role"); assertThat(one(connection,"SELECT current_user||'|'||session_user")).isEqualTo("customer_iam_role|pa27_customer"); execute(connection,"SET LOCAL statement_timeout='2s'; SET LOCAL lock_timeout='1s'"); } catch(Exception failure) { throw new IllegalStateException(failure); } finally { org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection(connection,pool); } if (fail) throw new IllegalStateException("synthetic"); return null; }); }
            catch (IllegalStateException failure) { assertThat(fail).isTrue(); }
            fixture.cleanPool();
        }
    }
    private Set<String> catalogMatrix(Connection connection) throws Exception {
        return new TreeSet<>(strings(connection, """
                WITH objects AS (
                    SELECT 'table' kind,c.relname identity,'' column_name,c.relacl acl FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='catalog_order' AND c.relkind='r'
                    UNION ALL SELECT 'column',c.relname,a.attname,a.attacl FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='catalog_order' AND a.attnum>0 AND NOT a.attisdropped
                    UNION ALL SELECT 'routine',p.proname,'',p.proacl FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='catalog_order'
                    UNION ALL SELECT 'schema',nspname,'',nspacl FROM pg_namespace WHERE nspname='catalog_order'
                ) SELECT kind||'|'||identity||'|'||column_name||'|'||privilege_type||'|'||grantor::regrole::text||'|'||is_grantable FROM objects CROSS JOIN LATERAL aclexplode(acl) grants WHERE grantee='catalog_order_role'::regrole ORDER BY 1
                """));
    }
    @Test void catalogRoleAndOperationsIdentityRetainExactCapabilitiesAndPool() throws Exception {
        provisionHistorical(); Set<String> catalog;
        var expected = new TreeSet<String>(); String suffix = "|catalog_order_owner|false";
        for (String table : List.of("catalog_product", "catalog_version", "catalog_offer", "catalog_benefit_line", "customer_order", "payment_order", "payment_event")) for (String privilege : List.of("SELECT", "INSERT")) expected.add("table|" + table + "||" + privilege + suffix);
        var updates = Map.of("catalog_version", List.of("sale_status", "offer_type", "audience", "price_cents", "currency", "billing_cycle", "installments", "grant_interval_months", "snapshot", "snapshot_sha256", "published_at", "revision"), "catalog_offer", List.of("offer_status", "version_token", "revision"), "customer_order", List.of("payment_status", "paid_at", "closed_at", "revision"), "payment_order", List.of("status", "provider_transaction_id", "updated_at", "revision"));
        updates.forEach((table, columns) -> columns.forEach(column -> expected.add("column|" + table + "|" + column + "|UPDATE" + suffix)));
        for (String function : List.of("guard_catalog_version_published", "guard_catalog_offer", "guard_customer_order_snapshot", "guard_payment_order_identity")) expected.add("routine|" + function + "||EXECUTE" + suffix);
        expected.add("schema|catalog_order||USAGE" + suffix);
        try (var connection = fixture.admin()) { catalog = catalogMatrix(connection); }
        assertThat(catalog).hasSize(42).isEqualTo(expected); upgrade();
        try (var connection = fixture.admin()) {
            assertThat(catalogMatrix(connection)).hasSize(42).isEqualTo(expected).isEqualTo(catalog);
            assertThat(one(connection, "SELECT count(*) FROM pg_auth_members WHERE member='pa36_operations'::regrole AND set_option AND NOT inherit_option AND NOT admin_option")).isEqualTo("7");
        }
        assertThat(acls().stream().filter(value -> value.startsWith("operations_iam_role|"))).hasSize(14);
        assertThat(acls().stream().filter(value -> value.startsWith("ivr_role|"))).hasSize(106);
        operationsPoolProof();
    }
    @Test void migrationReplayAndRejectedStatesPreserveAllBusinessAndInfrastructureRows() throws Exception {
        provisionHistorical(); seed(); String business = fixture.businessSnapshot();
        String invariants; try(var connection=fixture.admin()) { invariants=invariantSnapshot(connection); }
        String source=Files.readString(SCRIPT);
        injectedFailure(source.replace("IF pass = 1 THEN invariant_before := invariant_after;", "IF pass = 2 THEN invariant_after := invariant_after || '{\"fault\":true}'::jsonb; END IF; IF pass = 1 THEN invariant_before := invariant_after;"),"PRESERVATION");
        injectedFailure(source.replace("EXECUTE format('SET LOCAL ROLE %I', session_user);", "EXECUTE format('SET LOCAL ROLE %I', session_user); RAISE EXCEPTION 'PRIVATE_CANARY';"),"RECONCILIATION");
        upgrade(); try(var connection=fixture.admin()) { assertThat(invariantSnapshot(connection).equals(invariants)).isTrue(); for(String table:List.of("platform.idempotency_record","platform.outbox_event","audit.audit_fact","catalog_order.catalog_product","operations_iam.employee_account")) assertThat(Integer.parseInt(one(connection,"SELECT count(*) FROM "+table))).isPositive(); }
        assertThat(fixture.businessSnapshot().equals(business)).isTrue();
        try (var connection = fixture.admin()) { assertThat(strings(connection, "SELECT n.nspname||'.'||c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind='r' AND n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema')")).hasSize(44); String before = snapshot(connection); connection.setAutoCommit(false); replay(connection); connection.commit(); assertThat(snapshot(connection).equals(before)).isTrue(); }
        rejects("GRANT SELECT ON catalog_order.catalog_product TO customer_iam_role", "ACL"); assertThat(fixture.businessSnapshot().equals(business)).isTrue();
    }
    private void operationsPoolProof() throws Exception {
        try(var connection=fixture.admin()) { execute(connection,"ALTER ROLE pa36_operations PASSWORD '"+fixture.password+"'"); }
        try (var application = new AnnotationConfigApplicationContext()) {
            application.getEnvironment().setActiveProfiles("persistence");
            application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa36", Map.of("onecall.operations-subject-read.enabled", "true", "onecall.operations-subject-read.datasource.url", fixture.url, "onecall.operations-subject-read.datasource.username", "pa36_operations", "onecall.operations-subject-read.datasource.password", fixture.password)));
            application.register(OperationsSubjectReadPersistenceConfiguration.class); application.refresh();
            var pool = application.getBean("operationsSubjectReadDataSource", HikariDataSource.class); var dsl = application.getBean("operationsSubjectReadDsl", DSLContext.class); var transaction = application.getBean("operationsSubjectReadTransactionTemplate", TransactionTemplate.class); var context = application.getBean("operationsSubjectReadContext", OperationsReadContext.class);
            pool.setMaximumPoolSize(1);
            assertThat(application.getBeansOfType(HikariDataSource.class)).hasSize(1);
            for (boolean throwing : List.of(false, true)) {
                Runnable action = () -> transaction.executeWithoutResult(status -> {
                    String pid = dsl.fetchSingle("SELECT pg_backend_pid()::text").get(0, String.class); context.enter();
                    try {
                        for (var role : OperationsReadContext.Role.values()) {
                            context.scoped(role, () -> { assertThat(dsl.fetchSingle("SELECT session_user::text").get(0, String.class)).isEqualTo("pa36_operations"); return null; });
                            assertThat(dsl.fetchSingle("SELECT current_user::text").get(0, String.class)).isEqualTo("operations_iam_role");
                        }
                        dsl.execute("SET LOCAL ROLE ivr_role"); assertThat(dsl.fetchSingle("SELECT current_user::text").get(0, String.class)).isEqualTo("ivr_role"); dsl.execute("SET LOCAL ROLE operations_iam_role");
                        assertThat(dsl.fetchSingle("SELECT pg_backend_pid()::text").get(0, String.class)).isEqualTo(pid);
                        assertThat(dsl.fetchSingle("SHOW transaction_read_only").get(0, String.class)).isEqualTo("on");
                        assertThat(dsl.fetchSingle("SHOW transaction_isolation").get(0, String.class)).isEqualTo("repeatable read");
                        if (throwing) context.scoped(OperationsReadContext.Role.EVIDENCE, () -> { throw new IllegalStateException("synthetic"); });
                    } finally { context.exit(); }
                });
                if (throwing) assertThatThrownBy(action::run).isInstanceOf(IllegalStateException.class); else action.run();
                try (var connection = pool.getConnection()) { assertThat(one(connection, "SELECT current_user||'|'||session_user")).isEqualTo("pa36_operations|pa36_operations"); assertThat(connection.getAutoCommit()).isTrue(); assertThat(connection.isReadOnly()).isFalse(); }
            }
            for (String fault : List.of("cleanup", "rollback", "commit")) {
                assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                    context.enter();
                    String pid = dsl.fetchSingle("SELECT pg_backend_pid()::text").get(0, String.class);
                    if (!fault.equals("cleanup")) context.exit();
                    if (fault.equals("rollback")) status.setRollbackOnly();
                    try (var admin = fixture.admin()) { execute(admin, "SELECT pg_terminate_backend(" + Integer.parseInt(pid) + ")"); }
                    catch (Exception failure) { throw new IllegalStateException(failure); }
                    if (fault.equals("cleanup")) context.exit();
                })).isInstanceOf(RuntimeException.class);
                transaction.executeWithoutResult(status -> { context.enter(); context.scoped(OperationsReadContext.Role.EVIDENCE, () -> null); context.exit(); });
                try (var connection = pool.getConnection()) { assertThat(one(connection, "SELECT current_user||'|'||session_user")).isEqualTo("pa36_operations|pa36_operations"); assertThat(connection.getAutoCommit()).isTrue(); assertThat(connection.isReadOnly()).isFalse(); }
            }
        }
        }
    private void historicalThreeSlotGraph() throws Exception {
        var names = List.of("pa27_query", "pa27_customer", "pa27_command"); var slots = List.of("sharing_query", "customer_context", "sharing_command");
        try (var connection = fixture.admin()) {
            for (int index = 0; index < 3; index++) {
                String name = names.get(index); execute(connection, "CREATE ROLE " + name + " LOGIN NOINHERIT PASSWORD '" + fixture.password + "'");
                String marker = one(connection, "SELECT 'onecall:pa25:v1:db='||(SELECT oid FROM pg_database WHERE datname=current_database())||':role='||oid||':slot=" + slots.get(index) + ":name='||rolname FROM pg_roles WHERE rolname='" + name + "'");
                execute(connection, "COMMENT ON ROLE " + name + " IS '" + marker + "'");
            }
            execute(connection, "GRANT sharing_role TO pa27_query WITH ADMIN FALSE, INHERIT FALSE, SET TRUE; GRANT customer_iam_role TO pa27_customer WITH ADMIN FALSE, INHERIT FALSE, SET TRUE; GRANT sharing_command_role,evidence_role TO pa27_command WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
        }
    }
}
