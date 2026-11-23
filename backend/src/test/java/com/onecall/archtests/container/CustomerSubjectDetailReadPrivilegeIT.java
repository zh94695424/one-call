package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;

class CustomerSubjectDetailReadPrivilegeIT {
    private CustomerBusinessNumbersFixture fixture;
    private static final Path SCRIPT = Path.of("src/main/resources/db/migration/V016__customer_subject_detail_read_privileges.sql");
    private static final Map<String, List<String>> APPROVED = Map.ofEntries(
            Map.entry("customer_iam.customer_account", List.of("id", "display_name", "status")),
            Map.entry("organization_access.organization", List.of("id", "display_name", "status")),
            Map.entry("organization_access.organization_membership", List.of("account_id", "organization_id", "role", "status")),
            Map.entry("subject_number.business_subject", List.of("id", "owner_account_id", "owner_organization_id", "real_name_status", "created_at")),
            Map.entry("subject_number.caller_number_binding", List.of("id", "subject_id", "number_masked", "number_type", "status")),
            Map.entry("activation_consent.service_activation", List.of("subject_id", "status")),
            Map.entry("subject_number.subject_risk_state", List.of("caller_number_binding_id", "state")),
            Map.entry("evidence.evidence_record", List.of("id", "subject_id", "status", "source_type", "source_ref_id", "identity_at_capture", "consent_version", "created_at", "solidified_at")),
            Map.entry("evidence.evidence_artifact", List.of("evidence_id", "media_object_id", "artifact_role")),
            Map.entry("evidence.media_object", List.of("id", "sha256", "size_bytes", "mime_type")),
            Map.entry("evidence.recording", List.of("id", "media_object_id", "status", "validated_duration")),
            Map.entry("catalog_order.customer_order", List.of("subject_id", "account_id", "owner_organization_id")),
            Map.entry("catalog_order.catalog_product", List.of("id", "product_code")),
            Map.entry("catalog_order.catalog_version", List.of("id", "product_id", "version_no", "sale_status", "offer_type", "audience", "price_cents", "currency", "billing_cycle", "installments")),
            Map.entry("catalog_order.catalog_offer", List.of("id", "catalog_version_id", "channel", "offer_status", "version_token")),
            Map.entry("catalog_order.catalog_benefit_line", List.of("catalog_version_id", "entitlement_type", "quantity", "validity_days", "grant_interval_months")));
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
            WHERE grantee IN ('sharing_role'::regrole,'customer_iam_role'::regrole,'sharing_command_role'::regrole,'evidence_role'::regrole,'sharing_public_read_role'::regrole,'sharing_admission_role'::regrole,'evidence_media_reader_role'::regrole) ORDER BY 1
            """;

    @BeforeEach void start() throws Exception { fixture = new CustomerBusinessNumbersFixture(); fixture.start("015"); }
    @AfterEach void stop() { if (fixture != null) fixture.close(); }
    private Flyway current() { return Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").target("016").load(); }
    private Flyway latest() { return Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").target("019").load(); }
    private void upgrade() { assertThat(current().migrate().migrationsExecuted).isEqualTo(1); current().validate(); }
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
            assertThatThrownBy(() -> execute(connection, Files.readString(SCRIPT))).as(mutation).isInstanceOfSatisfying(SQLException.class,
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


    private Set<String> oracle() { return Set.of(
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
                "customer_iam_role|column|activation_consent.service_activation|status|SELECT|activation_consent_owner|false",

                "customer_iam_role|column|customer_iam.customer_account|display_name|SELECT|customer_iam_owner|false",
                "customer_iam_role|column|customer_iam.customer_account|id|SELECT|customer_iam_owner|false",
                "customer_iam_role|column|customer_iam.customer_account|status|SELECT|customer_iam_owner|false",
                "customer_iam_role|column|organization_access.organization|display_name|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization|id|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization|status|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization_membership|account_id|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization_membership|organization_id|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization_membership|role|SELECT|organization_access_owner|false",
                "customer_iam_role|column|organization_access.organization_membership|status|SELECT|organization_access_owner|false",
                "customer_iam_role|schema|customer_iam||USAGE|customer_iam_owner|false",
                "customer_iam_role|schema|organization_access||USAGE|organization_access_owner|false",
                "evidence_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.media_object|id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.media_object|mime_type|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.media_object|sha256|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.recording|media_object_id|SELECT|evidence_owner|false",
                "evidence_role|column|evidence.recording|validated_duration|SELECT|evidence_owner|false",
                "evidence_role|schema|evidence||USAGE|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.media_object|id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.media_object|mime_type|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.media_object|sha256|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.recording|id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.recording|media_object_id|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.recording|status|SELECT|evidence_owner|false",
                "evidence_media_reader_role|column|evidence.recording|validated_duration|SELECT|evidence_owner|false",
                "evidence_media_reader_role|schema|evidence||USAGE|evidence_owner|false",
                "sharing_admission_role|column|customer_iam.customer_account|id|SELECT|customer_iam_owner|false",
                "sharing_admission_role|column|customer_iam.customer_account|status|SELECT|customer_iam_owner|false",
                "sharing_admission_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
                "sharing_admission_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false",
                "sharing_admission_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
                "sharing_admission_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "sharing_admission_role|column|organization_access.organization|id|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization|status|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|account_id|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|id|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|organization_id|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|revision|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|role|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|organization_access.organization_membership|status|SELECT|organization_access_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|admissions_used|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|admissions_used|UPDATE|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|capability_token_hash|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|code_generation|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|code_hash|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|code_salt|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|content_sha256|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|content_snapshot|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|created_by_account_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|download_allowed|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|evidence_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|expires_at|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|max_admissions|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|owner_organization_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|revision|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|revision|UPDATE|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|revocation_generation|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|revocation_generation|UPDATE|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|share_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|status|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|status|UPDATE|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|valid_from|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.evidence_share_grant|verification_mode|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_domain|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_domain|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_type|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|event_type|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|occurred_at|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|occurred_at|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|payload_redacted|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|payload_redacted|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|payload_sha256|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|payload_sha256|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|result|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|result|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|risk_digest|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|risk_digest|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|share_admission_id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|share_admission_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|share_grant_id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_access_event|share_grant_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admission_key|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admission_key|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admission_no|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admission_no|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admitted_at|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|admitted_at|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|share_grant_id|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|share_grant_id|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|verification_mode|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|verification_mode|SELECT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|verification_result|INSERT|sharing_owner|false",
                "sharing_admission_role|column|sharing.share_admission|verification_result|SELECT|sharing_owner|false",
                "sharing_admission_role|column|subject_number.business_subject|id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.business_subject|owner_account_id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.business_subject|owner_organization_id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.business_subject|revision|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|number_hmac|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|number_type|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|revision|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|status|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|subject_id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.caller_number_binding|verified_at|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|cleared_at|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|id|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|observed_at|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|revision|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|risk_type|SELECT|subject_number_owner|false",
                "sharing_admission_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false",
                "sharing_admission_role|schema|customer_iam||USAGE|customer_iam_owner|false",
                "sharing_admission_role|schema|evidence||USAGE|evidence_owner|false",
                "sharing_admission_role|schema|organization_access||USAGE|organization_access_owner|false",
                "sharing_admission_role|schema|sharing||USAGE|sharing_owner|false",
                "sharing_admission_role|schema|subject_number||USAGE|subject_number_owner|false",
                "sharing_command_role|column|customer_iam.customer_account|id|SELECT|customer_iam_owner|false",
                "sharing_command_role|column|customer_iam.customer_account|status|SELECT|customer_iam_owner|false",
                "sharing_command_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|call_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|created_at|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|reservation_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|session_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
                "sharing_command_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "sharing_command_role|column|organization_access.organization|id|SELECT|organization_access_owner|false",
                "sharing_command_role|column|organization_access.organization|status|SELECT|organization_access_owner|false",
                "sharing_command_role|column|organization_access.organization_membership|account_id|SELECT|organization_access_owner|false",
                "sharing_command_role|column|organization_access.organization_membership|organization_id|SELECT|organization_access_owner|false",
                "sharing_command_role|column|organization_access.organization_membership|role|SELECT|organization_access_owner|false",
                "sharing_command_role|column|organization_access.organization_membership|status|SELECT|organization_access_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|admissions_used|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|code_generation|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|code_hash|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|code_salt|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|expires_at|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|revision|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|revocation_generation|UPDATE|sharing_owner|false",
                "sharing_command_role|column|sharing.evidence_share_grant|status|UPDATE|sharing_owner|false",
                "sharing_command_role|column|subject_number.business_subject|id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.business_subject|owner_account_id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.business_subject|owner_organization_id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.business_subject|revision|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.caller_number_binding|id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.caller_number_binding|revision|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.caller_number_binding|status|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.caller_number_binding|subject_id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.caller_number_binding|verified_at|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.subject_risk_state|revision|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.subject_risk_state|risk_type|SELECT|subject_number_owner|false",
                "sharing_command_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false",
                "sharing_command_role|routine|audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)||EXECUTE|audit_owner|false",
                "sharing_command_role|routine|platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)||EXECUTE|platform_reliability_owner|false",
                "sharing_command_role|schema|audit||USAGE|audit_owner|false",
                "sharing_command_role|schema|customer_iam||USAGE|customer_iam_owner|false",
                "sharing_command_role|schema|evidence||USAGE|evidence_owner|false",
                "sharing_command_role|schema|organization_access||USAGE|organization_access_owner|false",
                "sharing_command_role|schema|platform||USAGE|platform_reliability_owner|false",
                "sharing_command_role|schema|sharing||USAGE|sharing_owner|false",
                "sharing_command_role|schema|subject_number||USAGE|subject_number_owner|false",
                "sharing_command_role|table|sharing.evidence_share_grant||INSERT|sharing_owner|false",
                "sharing_command_role|table|sharing.evidence_share_grant||SELECT|sharing_owner|false",
                "sharing_command_role|table|sharing.share_access_event||INSERT|sharing_owner|false",
                "sharing_command_role|table|sharing.share_access_event||SELECT|sharing_owner|false",
                "sharing_command_role|table|sharing.share_admission||INSERT|sharing_owner|false",
                "sharing_command_role|table|sharing.share_admission||SELECT|sharing_owner|false",
                "sharing_public_read_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.media_object|id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.media_object|mime_type|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.media_object|sha256|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.recording|media_object_id|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|evidence.recording|validated_duration|SELECT|evidence_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|capability_token_hash|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|content_snapshot|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|download_allowed|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|evidence_id|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|expires_at|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|owner_organization_id|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|status|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|valid_from|SELECT|sharing_owner|false",
                "sharing_public_read_role|column|sharing.evidence_share_grant|verification_mode|SELECT|sharing_owner|false",
                "sharing_public_read_role|schema|evidence||USAGE|evidence_owner|false",
                "sharing_public_read_role|schema|sharing||USAGE|sharing_owner|false",
                "customer_iam_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false",
                "customer_iam_role|schema|evidence||USAGE|evidence_owner|false",
                "customer_iam_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "customer_iam_role|schema|catalog_order||USAGE|catalog_order_owner|false",
                "customer_iam_role|column|catalog_order.customer_order|subject_id|SELECT|catalog_order_owner|false",
                "customer_iam_role|column|catalog_order.customer_order|account_id|SELECT|catalog_order_owner|false",
                "customer_iam_role|column|catalog_order.customer_order|owner_organization_id|SELECT|catalog_order_owner|false",
                "sharing_role|column|customer_iam.customer_account|id|SELECT|customer_iam_owner|false",
                "sharing_role|column|customer_iam.customer_account|status|SELECT|customer_iam_owner|false",
                "sharing_role|column|organization_access.organization|id|SELECT|organization_access_owner|false",
                "sharing_role|column|organization_access.organization|status|SELECT|organization_access_owner|false",
                "sharing_role|column|organization_access.organization_membership|account_id|SELECT|organization_access_owner|false",
                "sharing_role|column|organization_access.organization_membership|organization_id|SELECT|organization_access_owner|false",
                "sharing_role|column|organization_access.organization_membership|status|SELECT|organization_access_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|admissions_used|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|code_generation|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|created_by_account_id|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|download_allowed|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|evidence_id|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|expires_at|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|max_admissions|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|owner_organization_id|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|revocation_generation|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|share_id|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|status|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|valid_from|SELECT|sharing_owner|false",
                "sharing_role|column|sharing.evidence_share_grant|verification_mode|SELECT|sharing_owner|false",
                "sharing_role|schema|customer_iam||USAGE|customer_iam_owner|false",
                "sharing_role|schema|organization_access||USAGE|organization_access_owner|false",
                "sharing_role|schema|sharing||USAGE|sharing_owner|false"); }
    private Set<String> currentOracle() { Set<String> expected = new TreeSet<>(oracle()); expected.addAll(Set.of(
            "customer_iam_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.evidence_record|consent_version|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.evidence_record|created_at|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.evidence_record|solidified_at|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.media_object|id|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.media_object|sha256|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.media_object|mime_type|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.recording|id|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.recording|media_object_id|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.recording|status|SELECT|evidence_owner|false", "customer_iam_role|column|evidence.recording|validated_duration|SELECT|evidence_owner|false")); expected.addAll(EvidenceRoleIvrAssociationAclIT.associationDelta()); expected.addAll(CustomerCatalogOfferReadPrivilegesIT.catalogDeltaOracle()); return expected; }
    private void rejectsPa25WithoutMutation() throws Exception { Set<String> before = acls(); for (String mode : List.of("apply", "check")) assertThat(fixture.psql(mode).getExitCode()).isNotZero(); assertThat(acls()).isEqualTo(before); }
    private void currentHistory() throws Exception { try (var connection = fixture.admin()) { assertThat(current().info().applied()).hasSize(19); assertThat(one(connection, "SELECT version||'|'||script||'|'||checksum||'|'||installed_rank||'|'||success FROM public.flyway_schema_history WHERE installed_rank=18")).isEqualTo("018|V018__evidence_role_ivr_association_read_privileges.sql|1606293398|18|true"); assertThat(one(connection, "SELECT count(*) FROM public.flyway_schema_history WHERE installed_rank=19 AND version='019' AND success AND script='V019__customer_catalog_offer_read_privileges.sql'")).isEqualTo("1"); } }
    private List<String[]> additions() {
        return List.of(
                new String[]{"subject_number.subject_risk_state", "caller_number_binding_id", "subject_number_owner"},
                new String[]{"subject_number.subject_risk_state", "state", "subject_number_owner"},
                new String[]{"evidence", "", "evidence_owner"},
                new String[]{"evidence.evidence_record", "subject_id", "evidence_owner"},
                new String[]{"catalog_order", "", "catalog_order_owner"},
                new String[]{"catalog_order.customer_order", "subject_id", "catalog_order_owner"},
                new String[]{"catalog_order.customer_order", "account_id", "catalog_order_owner"},
                new String[]{"catalog_order.customer_order", "owner_organization_id", "catalog_order_owner"});
    }
    private String capability(String[] addition) {
        return addition[1].isEmpty() ? "USAGE ON SCHEMA " + addition[0] : "SELECT (" + addition[1] + ") ON " + addition[0];
    }

    @Test
    void v015DeniesEveryMissingRiskEvidenceAndOrderCapabilityWith42501() throws Exception {
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            for (var addition : additions()) if (!addition[1].isEmpty()) denied(connection, "SELECT " + addition[1] + " FROM " + addition[0]);
            for (String sql : List.of("SELECT id FROM subject_number.business_subject", "SELECT id,subject_id FROM subject_number.caller_number_binding",
                    "SELECT subject_id,status FROM activation_consent.service_activation", "SELECT id,status FROM customer_iam.customer_account")) execute(connection, sql);
            connection.rollback();
        }
        assertThat(acls()).hasSize(261);
    }

    @Test
    void v016FreshAndUpgradeHaveExactSixteenHistoryEntriesAndFrozenPredecessors() throws Exception {
        Map<Path, byte[]> before = new TreeMap<>();
        try (var files = Files.list(SCRIPT.getParent())) {
            for (Path path : files.filter(path -> !path.equals(SCRIPT)).toList()) before.put(path, Files.readAllBytes(path));
        }
        upgrade(); history();
        for (var entry : before.entrySet()) assertThat(Files.readAllBytes(entry.getKey())).isEqualTo(entry.getValue());
        fixture.close(); fixture = new CustomerBusinessNumbersFixture(); fixture.start("016"); history();
        assertThat(current().migrate().migrationsExecuted).isZero();
    }
    private void history() throws Exception {
        current().validate(); assertThat(current().info().applied()).hasSize(16); assertThat(current().info().pending()).isEmpty();
        try (var connection = fixture.admin()) {
            assertThat(strings(connection, "SELECT version||'|'||script||'|'||checksum||'|'||description||'|'||installed_rank||'|'||type||'|'||success FROM public.flyway_schema_history ORDER BY installed_rank"))
                    .containsExactly("001|V001__platform_schemas_roles_idempotency_outbox_audit.sql|-709123726|platform schemas roles idempotency outbox audit|1|SQL|true",
                            "002|V002__identity_organization_and_employee.sql|-1229975166|identity organization and employee|2|SQL|true",
                            "003|V003__subject_number_and_claim.sql|-1355776370|subject number and claim|3|SQL|true",
                            "004|V004__catalog_order_and_payment.sql|-665258487|catalog order and payment|4|SQL|true",
                            "005|V005__ivr_task_session_call.sql|-1005314773|ivr task session call|5|SQL|true",
                            "006|V006__trial_entitlement_issuance_reservation_usage.sql|366834714|trial entitlement issuance reservation usage|6|SQL|true",
                            "007|V007__activation_and_consent.sql|-1281725572|activation and consent|7|SQL|true",
                            "008|V008__evidence_media.sql|-245169007|evidence media|8|SQL|true",
                            "009|V009__sharing.sql|-59961153|sharing|9|SQL|true",
                            "010|V010__sharing_role_least_privilege.sql|247431460|sharing role least privilege|10|SQL|true",
                            "011|V011__sharing_public_read_least_privilege.sql|492686454|sharing public read least privilege|11|SQL|true",
                            "012|V012__sharing_admission_least_privilege.sql|-291628548|sharing admission least privilege|12|SQL|true",
                            "013|V013__evidence_media_reader_role.sql|-611381791|evidence media reader role|13|SQL|true",
                            "014|V014__database_runtime_acl_rbac_hardening.sql|-340359494|database runtime acl rbac hardening|14|SQL|true",
                            "015|V015__customer_business_number_read_privileges.sql|-1316980027|customer business number read privileges|15|SQL|true",
                            "016|V016__customer_subject_detail_read_privileges.sql|1669606497|customer subject detail read privileges|16|SQL|true");
        }
        assertThat(current().info().applied()[15].getChecksum()).isEqualTo(1669606497);
    }

    @Test
    void v016AddsExactlyEightOwnerIssuedTuplesAndMatchesIndependent269Oracle() throws Exception {
        Set<String> before = acls(); assertThat(before).hasSize(261);
        upgrade(); Set<String> after = acls(); assertThat(after).hasSize(269);
        assertThat(after).isEqualTo(oracle());
        var delta = new TreeSet<>(after); delta.removeAll(before);
        assertThat(delta).containsExactlyInAnyOrder(
                "customer_iam_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false",
                "customer_iam_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false",
                "customer_iam_role|schema|evidence||USAGE|evidence_owner|false",
                "customer_iam_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
                "customer_iam_role|schema|catalog_order||USAGE|catalog_order_owner|false",
                "customer_iam_role|column|catalog_order.customer_order|subject_id|SELECT|catalog_order_owner|false",
                "customer_iam_role|column|catalog_order.customer_order|account_id|SELECT|catalog_order_owner|false",
                "customer_iam_role|column|catalog_order.customer_order|owner_organization_id|SELECT|catalog_order_owner|false");
        assertThat(after.stream().filter(value -> value.startsWith("customer_iam_role|"))).hasSize(34);
        assertThat(after.stream().filter(value -> value.startsWith("evidence_role|"))).hasSize(16);
        assertThat(after.stream().filter(value -> !value.startsWith("customer_iam_role|") && !value.startsWith("evidence_role|"))).hasSize(219);
        assertThat(after).allMatch(value -> value.endsWith("|false"));
    }
    @Test
    void customerRoleReadsOnlyApprovedColumnsAndDeniesAllOtherCapabilitiesWith42501() throws Exception {
        Set<String> catalogColumns=new TreeSet<>();
        for(var entry:APPROVED.entrySet()) if(Set.of("catalog_order.catalog_product","catalog_order.catalog_version","catalog_order.catalog_offer","catalog_order.catalog_benefit_line").contains(entry.getKey())) for(String column:entry.getValue()) catalogColumns.add(entry.getKey()+"|"+column);
        assertThat(catalogColumns).hasSize(22).isEqualTo(CustomerCatalogOfferReadPrivilegesIT.catalogDeltaOracle().stream().map(tuple -> { String[] fields=tuple.split("\\|",-1); return fields[2]+"|"+fields[3]; }).collect(java.util.stream.Collectors.toSet()));

        assertThat(latest().migrate().migrationsExecuted).isEqualTo(4); fixture.provision(); currentHistory();
        Set<String> before = acls();
        try (var admin = fixture.admin(); var connection = fixture.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            assertThat(one(connection, "SHOW transaction_read_only")).isEqualTo("off");
            for (String table : strings(admin, "SELECT n.nspname||'.'||c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind IN ('r','p') AND n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema') ORDER BY 1")) {
                for (String column : strings(admin, "SELECT attname FROM pg_attribute WHERE attrelid='" + table + "'::regclass AND attnum>0 AND NOT attisdropped ORDER BY attnum")) {
                    if (APPROVED.getOrDefault(table, List.of()).contains(column)) execute(connection, "SELECT " + column + " FROM " + table);
                    else denied(connection, "SELECT " + column + " FROM " + table);
                    denied(connection, "UPDATE " + table + " SET " + column + "=" + column + " WHERE false");
                    denied(connection, "INSERT INTO " + table + " (" + column + ") SELECT " + column + " FROM " + table + " WHERE false");
                }
                for (String sql : List.of("SELECT * FROM " + table, "DELETE FROM " + table + " WHERE false", "TRUNCATE " + table,
                        "CREATE TRIGGER pa27_probe BEFORE UPDATE ON " + table + " FOR EACH ROW EXECUTE FUNCTION customer_iam.guard_customer_account()")) denied(connection, sql);
                if (APPROVED.containsKey(table)) {
                    for (String lock : List.of("UPDATE", "SHARE")) denied(connection, "SELECT " + APPROVED.get(table).getFirst() + " FROM " + table + " FOR " + lock);
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
            for (String schema : List.of("customer_iam", "organization_access", "subject_number", "activation_consent", "evidence", "catalog_order", "audit", "platform")) denied(connection, "CREATE TABLE " + schema + ".pa27_probe(id integer)");
            for (String role : List.of("subject_number_owner", "activation_consent_owner", "sharing_role", "evidence_role", "sharing_command_role", "evidence_media_reader_role")) denied(connection, "SET LOCAL ROLE " + role);
            for (String routine : strings(admin, "SELECT format('%I.%I',n.nspname,p.proname)||'('||coalesce((SELECT string_agg('NULL::'||argument::regtype::text,',') FROM unnest(p.proargtypes::oid[]) argument),'')||')' FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema') ORDER BY 1")) denied(connection, "SELECT " + routine);
            for (String sequence : strings(admin, "SELECT schemaname||'.'||sequencename FROM pg_sequences WHERE schemaname !~ '^pg_'")) denied(connection, "SELECT nextval('" + sequence + "')");
            execute(connection, "GRANT SELECT (id) ON subject_number.business_subject TO pa27_query");
            connection.rollback();
        }
        assertThat(acls()).containsExactlyInAnyOrderElementsOf(before);
    }


    @Test
    void v016ReplayPreservesAclsRolesCredentialsDefinitionsAndBusinessRows() throws Exception {
        for (boolean credentialed : List.of(false, true)) {
            if (credentialed) { stop(); start(); frozenGraph(); }
            upgrade(); fixture.seed();
            String before; try (var connection = fixture.admin()) { before = catalog(connection); }
            String business = fixture.businessSnapshot();
            for (int replay = 0; replay < 2; replay++) {
                try (var connection = fixture.admin()) { connection.setAutoCommit(false); execute(connection, Files.readString(SCRIPT)); connection.commit(); }
                try (var connection = fixture.admin()) { assertThat(catalog(connection)).isEqualTo(before); }
                assertThat(fixture.businessSnapshot()).isEqualTo(business); assertThat(acls()).isEqualTo(oracle());
            }
            assertThat(current().migrate().migrationsExecuted).isZero(); current().validate();
        }
    }

    @Test
    void v016RejectsPartialWidenedWrongGrantorAndGrantOptionStatesAtomically() throws Exception {
        for (var addition : additions()) rejects("SET LOCAL ROLE " + addition[2] + "; GRANT " + capability(addition) + " TO customer_iam_role; RESET ROLE", "V016_ACL_REJECTED");
        for (String mutation : List.of("REVOKE SELECT (id) ON subject_number.business_subject FROM customer_iam_role",
                "GRANT SELECT ON evidence.evidence_record TO customer_iam_role",
                "GRANT SELECT (snapshot) ON catalog_order.customer_order TO customer_iam_role",
                "SET LOCAL ROLE evidence_owner; GRANT USAGE ON SCHEMA evidence TO customer_iam_role WITH GRANT OPTION; RESET ROLE")) rejects(mutation, "V016_ACL_REJECTED");
        upgrade();
        rejects("UPDATE pg_attribute SET attacl=ARRAY(SELECT CASE WHEN grant_entry::text LIKE 'customer_iam_role=%' THEN replace(grant_entry::text,'/evidence_owner','/fixture_admin')::aclitem ELSE grant_entry END FROM unnest(attacl) grant_entry) WHERE attrelid='evidence.evidence_record'::regclass AND attname='subject_id'", "V016_ACL_REJECTED");
    }

    @Test
    void v016RejectsAuthorityRoleGraphOwnershipSettingsPublicAndDefaultDriftAtomically() throws Exception {
        for (var entry : Map.ofEntries(
                Map.entry("SET LOCAL ROLE customer_iam_role", "AUTHORITY"),
                Map.entry("ALTER ROLE customer_iam_role LOGIN", "ATTRIBUTES"),
                Map.entry("ALTER ROLE customer_iam_role SUPERUSER", "ATTRIBUTES"),
                Map.entry("ALTER ROLE customer_iam_role NOINHERIT", "ATTRIBUTES"),
                Map.entry("ALTER ROLE customer_iam_role SET search_path TO evidence", "SETTINGS"),
                Map.entry("GRANT pg_read_all_data TO customer_iam_role", "TOPOLOGY"),
                Map.entry("ALTER TABLE evidence.evidence_record OWNER TO customer_iam_owner", "OWNERSHIP"),
                Map.entry("GRANT SELECT ON evidence.evidence_record TO PUBLIC", "ACL"),
                Map.entry("ALTER DEFAULT PRIVILEGES FOR ROLE evidence_owner GRANT SELECT ON TABLES TO customer_iam_role", "DEFAULT_ACL"),
                Map.entry("CREATE ROLE pa28_partial LOGIN NOINHERIT; COMMENT ON ROLE pa28_partial IS 'onecall:pa25:v1:stale'", "PROVENANCE"),
                Map.entry("CREATE TABLE evidence.pa28_dependency(id uuid); ALTER TABLE evidence.pa28_dependency OWNER TO customer_iam_role", "OWNERSHIP")
        ).entrySet()) rejects(entry.getKey(), "V016_" + entry.getValue() + "_REJECTED");
        frozenGraph(); rejects("GRANT customer_iam_role TO pa27_customer WITH INHERIT TRUE", "V016_TOPOLOGY_REJECTED");
    }

    @Test
    void pa25RequiresExactV016HistoryAndIndependent269TupleOracleWithoutRepair() throws Exception {
        rejectsPa25WithoutMutation();
        upgrade(); history(); assertThat(acls()).isEqualTo(oracle());
        rejectsPa25WithoutMutation();
        assertThat(latest().migrate().migrationsExecuted).isEqualTo(3); fixture.provision(); currentHistory(); assertThat(acls()).isEqualTo(currentOracle());
        for (var addition : additions()) {
            try (var connection = fixture.admin()) { execute(connection, "SET ROLE " + addition[2]); execute(connection, "REVOKE " + capability(addition) + " FROM customer_iam_role"); }
            Set<String> before = acls();
            try { for (String mode : List.of("apply", "check")) { assertThat(fixture.psql(mode).getExitCode()).isNotZero(); assertThat(acls()).isEqualTo(before); } }
            finally { try (var connection = fixture.admin()) { execute(connection, "SET ROLE " + addition[2]); execute(connection, "GRANT " + capability(addition) + " TO customer_iam_role"); } }
        }
        for (String mutation : List.of("checksum=0", "version='16'", "installed_rank=20", "type='BASELINE'", "success=false", "description='wrong'", "script='wrong.sql'")) {
            String row;
            try (var connection = fixture.admin()) { row = one(connection, "SELECT to_jsonb(value)::text FROM public.flyway_schema_history value WHERE installed_rank=16"); execute(connection, "UPDATE public.flyway_schema_history SET " + mutation + " WHERE installed_rank=16"); }
            try { for (String mode : List.of("apply", "check")) assertThat(fixture.psql(mode).getExitCode()).isNotZero(); }
            finally { try (var connection = fixture.admin()) { execute(connection, "DELETE FROM public.flyway_schema_history WHERE version IN ('016','16')"); execute(connection, "INSERT INTO public.flyway_schema_history SELECT * FROM jsonb_populate_record(NULL::public.flyway_schema_history,?::jsonb)", row); } }
        }
        assertThat(acls()).isEqualTo(currentOracle()); assertThat(fixture.psql("check").getExitCode()).isZero();
    }

    @Test
    void allSixLoginsAndSevenEffectiveRolesRetainExactMembershipBoundaries() throws Exception {
        frozenGraph(); assertThat(latest().migrate().migrationsExecuted).isEqualTo(4); fixture.provision(); currentHistory();
        try (var admin = fixture.admin()) {
            assertThat(one(admin, "SELECT count(*) FROM pg_auth_members WHERE member IN ('pa27_customer'::regrole,'pa27_query'::regrole,'pa27_command'::regrole) AND NOT admin_option AND NOT inherit_option AND set_option")).isEqualTo("4");
        }
        for (String login : List.of("pa27_customer", "pa27_query", "pa27_command", "sharing_public_read_role", "sharing_admission_role", "evidence_media_reader_role")) {
            try (var admin = fixture.admin()) { execute(admin, "ALTER ROLE " + login + " PASSWORD '" + fixture.password + "'"); }
            try (var connection = DriverManager.getConnection(fixture.url, login, fixture.password)) {
                connection.setAutoCommit(false);
                if (login.equals("pa27_customer")) {
                    denied(connection, "SELECT id FROM customer_iam.customer_account");
                    execute(connection, "SET LOCAL ROLE customer_iam_role"); execute(connection, "SELECT subject_id FROM evidence.evidence_record");
                    for (String role : List.of("evidence_role", "sharing_command_role", "evidence_owner", "catalog_order_owner", "subject_number_owner")) denied(connection, "SET LOCAL ROLE " + role);
                } else denied(connection, "SET LOCAL ROLE customer_iam_role");
                connection.rollback();
            }
        }
        assertThat(acls()).isEqualTo(currentOracle());
    }

    @Test
    void realCustomerPoolRestoresRoleAndTransactionAfterSuccessAndFailure() throws Exception {
        assertThat(latest().migrate().migrationsExecuted).isEqualTo(4); fixture.provision(); currentHistory(); fixture.seed(); fixture.web();
        var service = fixture.context.getBean("customerSubjectDetailQueryService", com.onecall.core.customer_iam.internal.application.CustomerSubjectDetailQueryService.class);
        UUID subject; try (var connection = fixture.admin()) { subject = UUID.fromString(one(connection, "SELECT id FROM subject_number.business_subject WHERE owner_account_id='" + fixture.account + "' LIMIT 1")); }
        var caller = new com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller(fixture.account,
                com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel.WEB,
                new com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace(com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.PERSONAL, null));
        assertThat(service.getCustomerSubject(caller, subject)).isPresent(); fixture.cleanPool();
        assertThat(service.getCustomerSubject(caller, UUID.randomUUID())).isEmpty(); fixture.cleanPool();
        try (var connection = fixture.admin()) { execute(connection, "REVOKE SELECT (subject_id) ON evidence.evidence_record FROM customer_iam_role"); }
        try { assertThatThrownBy(() -> service.getCustomerSubject(caller, subject)).isInstanceOfSatisfying(
                com.onecall.core.customer_iam.internal.application.CustomerContextException.class, error -> assertThat(error.code()).isEqualTo(com.onecall.core.customer_iam.internal.application.CustomerContextException.Code.SERVICE_UNAVAILABLE)); }
        finally { try (var connection = fixture.admin()) { execute(connection, "SET ROLE evidence_owner; GRANT SELECT (subject_id) ON evidence.evidence_record TO customer_iam_role"); } }
        fixture.cleanPool(); assertThat(service.getCustomerSubject(caller, subject)).isPresent(); fixture.cleanPool();
    }
}
