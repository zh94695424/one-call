package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;

class CustomerEvidenceReadPrivilegeIT {
    private CustomerBusinessNumbersFixture fixture;
    private static final Path SCRIPT = Path.of("src/main/resources/db/migration/V017__customer_evidence_read_privileges.sql");
    private static final Map<String, List<String>> APPROVED = Map.ofEntries(
            Map.entry("customer_iam.customer_account", List.of("id", "display_name", "status")),
            Map.entry("organization_access.organization", List.of("id", "display_name", "status")),
            Map.entry("organization_access.organization_membership", List.of("account_id", "organization_id", "role", "status")),
            Map.entry("subject_number.business_subject", List.of("id", "owner_account_id", "owner_organization_id", "real_name_status", "created_at")),
            Map.entry("subject_number.caller_number_binding", List.of("id", "subject_id", "number_masked", "number_type", "status")),
            Map.entry("activation_consent.service_activation", List.of("subject_id", "status")),
            Map.entry("subject_number.subject_risk_state", List.of("caller_number_binding_id", "state")),
            Map.entry("evidence.evidence_record", List.of("subject_id", "id", "status", "source_type", "source_ref_id", "identity_at_capture", "consent_version", "created_at", "solidified_at")),
            Map.entry("catalog_order.customer_order", List.of("subject_id", "account_id", "owner_organization_id")),
            Map.entry("evidence.evidence_artifact", List.of("evidence_id", "media_object_id", "artifact_role")),
            Map.entry("evidence.media_object", List.of("id", "sha256", "size_bytes", "mime_type")),
            Map.entry("evidence.recording", List.of("id", "media_object_id", "status", "validated_duration")));
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

    @BeforeEach void start() throws Exception { fixture = new CustomerBusinessNumbersFixture(); fixture.start("016"); }
    @AfterEach void stop() { if (fixture != null) fixture.close(); }
    private Flyway current() { return Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").target("017").load(); }
    private void upgrade() { assertThat(current().migrate().migrationsExecuted).isEqualTo(1); current().validate(); }
    private List<String> strings(Connection connection, String sql) throws SQLException {
        List<String> values = new ArrayList<>(); try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) { while (rows.next()) values.add(rows.getString(1)); } return values;
    }
    private Set<String> acls() throws Exception { try (var connection = fixture.admin()) { return new TreeSet<>(strings(connection, ACL)); } }
    private String catalog(Connection connection) throws Exception {
        return one(connection, """
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
    }
    private void denied(Connection connection, String sql) throws Exception {
        var savepoint = connection.setSavepoint();
        try { assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("42501")); }
        finally { connection.rollback(savepoint); }
    }
    private void rejects(String mutation, String category) throws Exception {
        String business = fixture.businessSnapshot();
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false); execute(connection, mutation);
            String role = one(connection, "SELECT quote_ident(current_user)"); execute(connection, "RESET ROLE");
            String before = catalog(connection); execute(connection, "SET LOCAL ROLE " + role); var savepoint = connection.setSavepoint();
            assertThatThrownBy(() -> execute(connection, Files.readString(SCRIPT))).as(mutation).isInstanceOfSatisfying(SQLException.class,
                    error -> { assertThat(error.getSQLState()).isEqualTo("P0001"); assertThat(error.getMessage()).contains(category).doesNotContain(fixture.password, "PASSWORD_CANARY", "IDENTITY_CANARY"); });
            connection.rollback(savepoint); execute(connection, "RESET ROLE"); assertThat(catalog(connection)).isEqualTo(before); connection.rollback();
        }
        assertThat(fixture.businessSnapshot()).isEqualTo(business);
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
    private List<String[]> additions() {
        return List.of(
                new String[]{"evidence.evidence_record", "id", "evidence_owner"},
                new String[]{"evidence.evidence_record", "status", "evidence_owner"},
                new String[]{"evidence.evidence_record", "source_type", "evidence_owner"},
                new String[]{"evidence.evidence_record", "source_ref_id", "evidence_owner"},
                new String[]{"evidence.evidence_record", "identity_at_capture", "evidence_owner"},
                new String[]{"evidence.evidence_record", "consent_version", "evidence_owner"},
                new String[]{"evidence.evidence_record", "created_at", "evidence_owner"},
                new String[]{"evidence.evidence_record", "solidified_at", "evidence_owner"},
                new String[]{"evidence.evidence_artifact", "evidence_id", "evidence_owner"},
                new String[]{"evidence.evidence_artifact", "media_object_id", "evidence_owner"},
                new String[]{"evidence.evidence_artifact", "artifact_role", "evidence_owner"},
                new String[]{"evidence.media_object", "id", "evidence_owner"},
                new String[]{"evidence.media_object", "sha256", "evidence_owner"},
                new String[]{"evidence.media_object", "size_bytes", "evidence_owner"},
                new String[]{"evidence.media_object", "mime_type", "evidence_owner"},
                new String[]{"evidence.recording", "id", "evidence_owner"},
                new String[]{"evidence.recording", "media_object_id", "evidence_owner"},
                new String[]{"evidence.recording", "status", "evidence_owner"},
                new String[]{"evidence.recording", "validated_duration", "evidence_owner"});
    }
    private String capability(String[] addition) {
        return "SELECT (" + addition[1] + ") ON " + addition[0];
    }
    private Set<String> expectedDelta() {
        Set<String> result = new TreeSet<>();
        for (var addition : additions()) result.add("customer_iam_role|column|" + addition[0] + "|" + addition[1] + "|SELECT|evidence_owner|false");
        assertThat(result).hasSize(19);
        return result;
    }
    private Set<String> currentOracle() {
        Set<String> result = new TreeSet<>(oracle()); result.addAll(expectedDelta());
        assertThat(result).hasSize(288); return result;
    }
    private void history() throws Exception {
        current().validate(); assertThat(current().info().applied()).hasSize(17); assertThat(current().info().pending()).isEmpty();
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
                            "016|V016__customer_subject_detail_read_privileges.sql|1669606497|customer subject detail read privileges|16|SQL|true",
                            "017|V017__customer_evidence_read_privileges.sql|-41822162|customer evidence read privileges|17|SQL|true");
        }
        assertThat(current().info().applied()[15].getChecksum()).isEqualTo(1669606497);
        assertThat(current().info().applied()[16].getChecksum()).isEqualTo(-41822162);
    }

    private Map<Path, byte[]> frozenFiles() throws Exception {
        Map<Path, byte[]> result = new TreeMap<>();
        Map<String, String> hashes = Map.ofEntries(
                Map.entry("001", "946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045"),
                Map.entry("002", "534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5"),
                Map.entry("003", "1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1"),
                Map.entry("004", "a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc"),
                Map.entry("005", "2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310"),
                Map.entry("006", "830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475"),
                Map.entry("007", "bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659"),
                Map.entry("008", "2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e"),
                Map.entry("009", "ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae"),
                Map.entry("010", "fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343"),
                Map.entry("011", "f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b"),
                Map.entry("012", "13e6ae056134cd48b2fa8d840119a3268efe8bfc178d4fee24e7e3feac46f828"),
                Map.entry("013", "691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6"),
                Map.entry("014", "e0e97abcc86a9e9db354e44f33925f336b3266b267cf7107e821e2420668b85a"),
                Map.entry("015", "1697a69fd39a61a9f8f7e0c1d2d699deda965840b58f7ff9937308f5670191c3"),
                Map.entry("016", "988a9876943ae07a32210bc0380128294fc823817f5b6c3d4a44a28dfdd4696f"));
        try (var files = Files.list(SCRIPT.getParent())) {
            for (Path path : files.filter(path -> hashes.containsKey(path.getFileName().toString().substring(1, 4))).toList()) {
                byte[] bytes = Files.readAllBytes(path);
                String normalized = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).replace("\r\n", "\n");
                assertThat(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                        .as(path.toString()).isEqualTo(hashes.get(path.getFileName().toString().substring(1, 4)));
                result.put(path, bytes);
            }
        }
        assertThat(result).hasSize(16); return result;
    }
    @Test
    void v016DeniesEveryNineteenMissingColumnCapabilityWith42501() throws Exception {
        assertThat(additions()).hasSize(19);
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            for (var addition : additions()) denied(connection, "SELECT " + addition[1] + " FROM " + addition[0]);
            for (String sql : List.of("SELECT subject_id FROM evidence.evidence_record", "SELECT id FROM subject_number.business_subject",
                    "SELECT caller_number_binding_id,state FROM subject_number.subject_risk_state", "SELECT subject_id,account_id,owner_organization_id FROM catalog_order.customer_order")) execute(connection, sql);
            connection.rollback();
        }
        assertThat(acls()).hasSize(269).isEqualTo(oracle());
    }

    @Test
    void v017FreshAndUpgradeHaveExactSeventeenHistoryAndFrozenPredecessors() throws Exception {
        Map<Path, byte[]> before = frozenFiles();
        upgrade(); history();
        for (var entry : before.entrySet()) assertThat(Files.readAllBytes(entry.getKey())).isEqualTo(entry.getValue());
        fixture.close(); fixture = new CustomerBusinessNumbersFixture(); fixture.start(null); history();
        assertThat(current().migrate().migrationsExecuted).isZero();
        for (var entry : before.entrySet()) assertThat(Files.readAllBytes(entry.getKey())).isEqualTo(entry.getValue());
        assertThat(acls()).isEqualTo(currentOracle());
    }

    @Test
    void v017AddsExactlyNineteenOwnerIssuedTuplesAndMatchesIndependent288Oracle() throws Exception {
        Set<String> before = acls(); assertThat(before).hasSize(269).isEqualTo(oracle());
        upgrade(); Set<String> after = acls(); assertThat(after).hasSize(288).isEqualTo(currentOracle());
        Set<String> delta = new TreeSet<>(after); delta.removeAll(before);
        assertThat(delta).hasSize(19).isEqualTo(expectedDelta());
        assertThat(after).containsAll(before);
        assertThat(after.stream().filter(value -> value.startsWith("customer_iam_role|"))).hasSize(53);
        assertThat(after.stream().filter(value -> value.startsWith("evidence_role|"))).hasSize(16);
        assertThat(after.stream().filter(value -> !value.startsWith("customer_iam_role|") && !value.startsWith("evidence_role|"))).hasSize(219);
        assertThat(after).allMatch(value -> value.endsWith("|false"));
    }
    @Test
    void customerRoleReadsOnlyApprovedFactsAndDeniesAllSensitiveCapabilitiesWith42501() throws Exception {
        upgrade(); fixture.provision(); fixture.seed(); seedEvidence();
        Set<String> before = acls();
        assertThat(before).isEqualTo(currentOracle());
        try (var admin = fixture.admin(); var connection = fixture.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            assertThat(one(connection, "SHOW transaction_read_only")).isEqualTo("off");
            assertThat(one(connection, "SELECT identity_at_capture->>'identity' FROM evidence.evidence_record")).isEqualTo("IDENTITY_CANARY");
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
    void v017ReplayPreservesRolesCredentialsDefinitionsAclsAndBusinessRows() throws Exception {
        for (boolean credentialed : List.of(false, true)) {
            if (credentialed) { stop(); start(); frozenGraph(); }
            upgrade(); fixture.seed(); seedEvidence();
            String before; try (var connection = fixture.admin()) { before = catalog(connection); }
            String business = fixture.businessSnapshot(); Set<String> beforeAcls = acls();
            for (int replay = 0; replay < 2; replay++) {
                try (var connection = fixture.admin()) { connection.setAutoCommit(false); execute(connection, Files.readString(SCRIPT)); connection.commit(); }
                try (var connection = fixture.admin()) { assertThat(catalog(connection)).isEqualTo(before); }
                assertThat(fixture.businessSnapshot()).isEqualTo(business);
                assertThat(acls()).isEqualTo(beforeAcls).isEqualTo(currentOracle());
            }
            assertThat(current().migrate().migrationsExecuted).isZero(); history();
        }
    }

    @Test
    void v017RejectsPartialWidenedWrongGrantorAndGrantOptionStatesAtomically() throws Exception {
        fixture.seed(); seedEvidence();
        rejects("INSERT INTO public.flyway_schema_history SELECT 17,'017','customer evidence read privileges',"
                + "'SQL','V017__customer_evidence_read_privileges.sql'," + current().info().pending()[0].getChecksum()
                + ",installed_by,installed_on,execution_time,true FROM public.flyway_schema_history WHERE installed_rank=16",
                "V017_HISTORY_REJECTED");
        for (var addition : additions()) rejects("SET LOCAL ROLE evidence_owner; GRANT " + capability(addition) + " TO customer_iam_role; RESET ROLE", "V017_ACL_REJECTED");
        for (String mutation : List.of("REVOKE SELECT (subject_id) ON evidence.evidence_record FROM customer_iam_role",
                "GRANT SELECT ON evidence.evidence_record TO customer_iam_role",
                "GRANT SELECT (snapshot) ON evidence.evidence_record TO customer_iam_role",
                "ALTER TABLE evidence.evidence_record OWNER TO customer_iam_owner",
                "SET LOCAL ROLE evidence_owner; GRANT SELECT (subject_id) ON evidence.evidence_record TO customer_iam_role WITH GRANT OPTION; RESET ROLE"))
            rejects(mutation, mutation.startsWith("ALTER") ? "V017_OWNERSHIP_REJECTED" : "V017_ACL_REJECTED");
        upgrade();
        rejects("DELETE FROM public.flyway_schema_history WHERE installed_rank=17", "V017_HISTORY_REJECTED");
        rejects("DELETE FROM public.flyway_schema_history WHERE installed_rank=17; INSERT INTO public.flyway_schema_history(installed_rank,version,description,type,script,checksum,installed_by,installed_on,execution_time,success) VALUES (0,'baseline','baseline','BASELINE','baseline',0,'fixture_admin',now(),0,true)", "V017_HISTORY_REJECTED");
        for (var addition : additions()) {
            rejects("SET LOCAL ROLE evidence_owner; REVOKE " + capability(addition) + " FROM customer_iam_role; RESET ROLE", "V017_ACL_REJECTED");
            rejects("SET LOCAL ROLE evidence_owner; GRANT " + capability(addition) + " TO customer_iam_role WITH GRANT OPTION; RESET ROLE", "V017_ACL_REJECTED");
            rejects("UPDATE pg_attribute SET attacl=ARRAY(SELECT CASE WHEN grant_entry::text LIKE 'customer_iam_role=%' THEN replace(grant_entry::text,'/evidence_owner','/fixture_admin')::aclitem ELSE grant_entry END FROM unnest(attacl) grant_entry) WHERE attrelid='" + addition[0] + "'::regclass AND attname='" + addition[1] + "'", "V017_ACL_REJECTED");
        }
        assertThat(acls()).isEqualTo(currentOracle());
    }

    private String eventTrigger(String body) {
        return "CREATE FUNCTION public.pa29_grant_probe() RETURNS event_trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $probe$ BEGIN " + body
                + " END $probe$; REVOKE ALL ON FUNCTION public.pa29_grant_probe() FROM PUBLIC; CREATE EVENT TRIGGER pa29_grant_probe ON ddl_command_end WHEN TAG IN ('GRANT') EXECUTE FUNCTION public.pa29_grant_probe()";
    }

    @Test
    void v017RejectsAuthorityHistoryProvenanceAndCatalogDriftAtomically() throws Exception {
        for (String category : List.of("AUTHORITY", "PROVENANCE", "HISTORY", "SETTINGS", "ATTRIBUTES", "TOPOLOGY", "OWNERSHIP", "ACL", "DATABASE", "DEFAULT_ACL", "DEPENDENCY", "PRESERVATION", "RECONCILIATION"))
            assertThat(Files.readString(SCRIPT)).contains("V017_" + category + "_REJECTED");
        for (var entry : Map.ofEntries(
                Map.entry("SET LOCAL ROLE customer_iam_role", "AUTHORITY"),
                Map.entry("ALTER ROLE customer_iam_role LOGIN", "ATTRIBUTES"),
                Map.entry("ALTER ROLE customer_iam_role SUPERUSER", "ATTRIBUTES"),
                Map.entry("ALTER ROLE customer_iam_role NOINHERIT", "ATTRIBUTES"),
                Map.entry("ALTER ROLE customer_iam_role SET search_path TO evidence", "SETTINGS"),
                Map.entry("ALTER DATABASE pa27 SET search_path TO evidence", "SETTINGS"),
                Map.entry("GRANT pg_read_all_data TO customer_iam_role", "TOPOLOGY"),
                Map.entry("ALTER TABLE evidence.evidence_record OWNER TO customer_iam_owner", "OWNERSHIP"),
                Map.entry("GRANT SELECT ON evidence.evidence_event TO PUBLIC", "ACL"),
                Map.entry("GRANT CREATE ON DATABASE pa27 TO PUBLIC", "DATABASE"),
                Map.entry("ALTER DEFAULT PRIVILEGES FOR ROLE evidence_owner GRANT SELECT ON TABLES TO customer_iam_role", "DEFAULT_ACL"),
                Map.entry("CREATE ROLE pa29_partial LOGIN NOINHERIT; COMMENT ON ROLE pa29_partial IS 'onecall:pa25:v1:stale'", "PROVENANCE"),
                Map.entry("ALTER EXTENSION plpgsql ADD TABLE evidence.evidence_record", "DEPENDENCY"),
                Map.entry(eventTrigger("RAISE EXCEPTION 'PASSWORD_CANARY';"), "RECONCILIATION"),
                Map.entry(eventTrigger("COMMENT ON ROLE fixture_admin IS 'pa29-preservation-probe';"), "PRESERVATION")
        ).entrySet()) rejects(entry.getKey(), "V017_" + entry.getValue() + "_REJECTED");
        for (int rank = 1; rank <= 16; rank++) {
            for (String assignment : List.of("checksum=0", "description='wrong'", "script='wrong.sql'", "version='wrong'", "type='BASELINE'", "success=false", "installed_rank=1000"))
                rejects("UPDATE public.flyway_schema_history SET " + assignment + " WHERE installed_rank=" + rank, "V017_HISTORY_REJECTED");
            rejects("DELETE FROM public.flyway_schema_history WHERE installed_rank=" + rank, "V017_HISTORY_REJECTED");
        }
        rejects("INSERT INTO public.flyway_schema_history SELECT 1016,'018',description,type,script,checksum,installed_by,installed_on,execution_time,success FROM public.flyway_schema_history WHERE installed_rank=16", "V017_HISTORY_REJECTED");
        frozenGraph();
        for (String option : List.of("ADMIN TRUE", "INHERIT TRUE", "SET FALSE")) rejects("GRANT customer_iam_role TO pa27_customer WITH " + option, "V017_TOPOLOGY_REJECTED");
        rejects("COMMENT ON ROLE pa27_customer IS 'onecall:pa25:v1:stale'", "V017_PROVENANCE_REJECTED");
    }

    private void pa25RejectedWithoutRepair() throws Exception {
        String before; try (var connection = fixture.admin()) { before = catalog(connection); }
        String business = fixture.businessSnapshot(); Set<String> beforeAcls = acls();
        for (String mode : List.of("apply", "check")) {
            var result = fixture.psql(mode);
            assertThat(result.getExitCode()).as(mode + ": " + result.getStderr()).isNotZero();
            assertThat(result.getStdout() + result.getStderr()).doesNotContain(fixture.password);
            try (var connection = fixture.admin()) { assertThat(catalog(connection)).isEqualTo(before); }
            assertThat(acls()).isEqualTo(beforeAcls); assertThat(fixture.businessSnapshot()).isEqualTo(business);
        }
    }

    @Test
    void pa25RequiresExactV017HistoryAndIndependent288OracleWithoutRepair() throws Exception {
        pa25RejectedWithoutRepair(); upgrade(); fixture.provision(); fixture.seed();
        for (var addition : additions()) {
            try (var connection = fixture.admin()) { execute(connection, "SET ROLE evidence_owner; REVOKE " + capability(addition) + " FROM customer_iam_role"); }
            try { pa25RejectedWithoutRepair(); }
            finally { try (var connection = fixture.admin()) { execute(connection, "SET ROLE evidence_owner; GRANT " + capability(addition) + " TO customer_iam_role"); } }
        }
        for (int rank : List.of(16, 17)) {
            for (String mutation : List.of("checksum=0", "version='" + rank + "'", "installed_rank=18", "type='BASELINE'", "success=false", "description='wrong'", "script='wrong.sql'", "DELETE")) {
                String row;
                try (var connection = fixture.admin()) {
                    row = one(connection, "SELECT to_jsonb(value)::text FROM public.flyway_schema_history value WHERE installed_rank=" + rank);
                    execute(connection, mutation.equals("DELETE") ? "DELETE FROM public.flyway_schema_history WHERE installed_rank=" + rank : "UPDATE public.flyway_schema_history SET " + mutation + " WHERE installed_rank=" + rank);
                }
                try { pa25RejectedWithoutRepair(); }
                finally { try (var connection = fixture.admin()) {
                    execute(connection, "DELETE FROM public.flyway_schema_history WHERE version IN ('0" + rank + "','" + rank + "')");
                    execute(connection, "INSERT INTO public.flyway_schema_history SELECT * FROM jsonb_populate_record(NULL::public.flyway_schema_history,?::jsonb)", row);
                } }
            }
        }
        for (String version : List.of("017", "018")) {
            try (var connection = fixture.admin()) { execute(connection, "INSERT INTO public.flyway_schema_history SELECT 1017,?,description,type,script,checksum,installed_by,installed_on,execution_time,success FROM public.flyway_schema_history WHERE installed_rank=17", version); }
            try { pa25RejectedWithoutRepair(); }
            finally { try (var connection = fixture.admin()) { execute(connection, "DELETE FROM public.flyway_schema_history WHERE installed_rank=1017"); } }
        }
        String before; try (var connection = fixture.admin()) { before = catalog(connection); }
        for (int replay = 0; replay < 2; replay++) for (String mode : List.of("apply", "check")) {
            assertThat(fixture.psql(mode).getExitCode()).isZero();
            try (var connection = fixture.admin()) { assertThat(catalog(connection)).isEqualTo(before); }
            assertThat(acls()).isEqualTo(currentOracle());
        }
        history();
    }

    @Test
    void sixLoginsSevenEffectiveRolesAndFourMembershipEdgesRemainExact() throws Exception {
        frozenGraph(); upgrade(); fixture.provision();
        List<String> roles = List.of("sharing_role", "customer_iam_role", "sharing_command_role", "evidence_role", "sharing_public_read_role", "sharing_admission_role", "evidence_media_reader_role");
        List<String> logins = List.of("pa27_query", "pa27_customer", "pa27_command", "sharing_public_read_role", "sharing_admission_role", "evidence_media_reader_role");
        Map<String, List<String>> targets = Map.of("pa27_query", List.of("sharing_role"), "pa27_customer", List.of("customer_iam_role"), "pa27_command", List.of("sharing_command_role", "evidence_role"));
        try (var admin = fixture.admin()) {
            assertThat(strings(admin, "SELECT member::regrole::text||'|'||roleid::regrole::text||'|'||admin_option||'|'||inherit_option||'|'||set_option FROM pg_auth_members WHERE member IN ('pa27_customer'::regrole,'pa27_query'::regrole,'pa27_command'::regrole) ORDER BY 1"))
                    .containsExactly("pa27_command|evidence_role|false|false|true", "pa27_command|sharing_command_role|false|false|true", "pa27_customer|customer_iam_role|false|false|true", "pa27_query|sharing_role|false|false|true");
            assertThat(one(admin, "SELECT count(*) FROM pg_roles WHERE rolcanlogin AND rolname <> 'fixture_admin' AND rolname !~ '^pg_' AND rolname <> 'postgres'")).isEqualTo("6");
            for (String login : logins) execute(admin, "ALTER ROLE " + login + " PASSWORD '" + fixture.password + "'");
            for (String role : roles) {
                Set<String> expected = new TreeSet<>();
                for (String tuple : currentOracle()) {
                    String[] fact = tuple.split("\\|", -1);
                    if (!fact[0].equals(role) || !fact[4].equals("SELECT")) continue;
                    if (fact[1].equals("column")) expected.add(fact[2] + "|" + fact[3]);
                    if (fact[1].equals("table")) for (String column : strings(admin, "SELECT attname FROM pg_attribute WHERE attrelid='" + fact[2] + "'::regclass AND attnum>0 AND NOT attisdropped")) expected.add(fact[2] + "|" + column);
                }
                assertThat(new TreeSet<>(strings(admin, "SELECT n.nspname||'.'||c.relname||'|'||a.attname FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE a.attnum>0 AND NOT a.attisdropped AND c.relkind IN ('r','p') AND n.nspname !~ '^pg_' AND n.nspname NOT IN ('public','information_schema') AND has_column_privilege('" + role + "',c.oid,a.attnum,'SELECT')")))
                        .as(role + " effective SELECT matrix").isEqualTo(expected);
            }
        }
        for (String login : logins) try (var connection = DriverManager.getConnection(fixture.url, login, fixture.password)) {
            connection.setAutoCommit(false);
            if (targets.containsKey(login)) denied(connection, "SELECT id FROM customer_iam.customer_account");
            for (String role : roles) {
                if (role.equals(login) || targets.getOrDefault(login, List.of()).contains(role)) { execute(connection, "SET LOCAL ROLE " + role); assertThat(one(connection, "SELECT current_user")).isEqualTo(role); execute(connection, "RESET ROLE"); }
                else denied(connection, "SET LOCAL ROLE " + role);
            }
            for (String role : List.of("evidence_owner", "subject_number_owner", "customer_iam_owner", "pg_read_all_data", "pg_write_all_data", "fixture_admin")) denied(connection, "SET LOCAL ROLE " + role);
            connection.rollback();
        }
        assertThat(acls()).isEqualTo(currentOracle());
    }

    private UUID seedEvidence() throws Exception {
        UUID evidence = UUID.randomUUID(); UUID source = UUID.randomUUID(); String event = UUID.randomUUID().toString();
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false);
            UUID subject = UUID.fromString(one(connection, "SELECT id FROM subject_number.business_subject WHERE owner_account_id='" + fixture.account + "' ORDER BY id LIMIT 1"));
            execute(connection, """
                    INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,source_event_id,
                        identity_at_capture,consent_version,snapshot,snapshot_sha256,status,created_at)
                    VALUES (?,?,'CALL_RECORDING',?,'EVIDENCE',?,
                        jsonb_build_object('subjectId',?::text,'capturedAt','2026-11-11T00:00:00Z','sourceChannel','IVR','identity','IDENTITY_CANARY'),
                        'consent-v1',jsonb_build_object('capture',jsonb_build_object('capturedAt','2026-11-11T00:00:00Z'),'content','SNAPSHOT_CANARY','sourceEventDomain','EVIDENCE','sourceEventId',?::text,
                            'source',jsonb_build_object('subjectId',?::text,'recordingId',?::text)),repeat('a',64),'PENDING',?::timestamptz)
                    """, evidence, subject, source, event, subject, event, subject, source, fixture.time.toString());
            execute(connection, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) VALUES (?,?,'EVIDENCE',?,'EVIDENCE_CREATED',jsonb_build_object('secret','EVENT_CANARY'),repeat('b',64))", UUID.randomUUID(), evidence, event);
            connection.commit();
        }
        return evidence;
    }

    @Test
    void realCustomerPoolRestoresRoleAndTransactionAfterSuccessAndFailure() throws Exception {
        upgrade(); fixture.provision(); fixture.seed(); UUID evidence = seedEvidence(); fixture.web();
        var service = fixture.context.getBean("customerEvidenceQueryService", com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryService.class);
        var caller = new com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller(fixture.account,
                com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel.WEB,
                new com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace(com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.PERSONAL, null));
        var request = new com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryService.Request(Optional.empty(), Optional.empty());
        List<String> states = new ArrayList<>();
        fixture.context.getBean("customerQueryDsl", org.jooq.DSLContext.class).configuration().set(new org.jooq.impl.DefaultExecuteListener() {
            @Override public void executeEnd(org.jooq.ExecuteContext execution) {
                if (!execution.sql().stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT")) return;
                try { states.add(one(execution.connection(), "SELECT current_user||'|'||session_user||'|'||current_setting('transaction_read_only')||'|'||current_setting('transaction_isolation')||'|'||pg_backend_pid()")); }
                catch (SQLException failure) { throw new IllegalStateException(failure); }
            }
        });
        assertThat(service.listCustomerEvidence(caller, request).items()).hasSize(1); fixture.cleanPool();
        assertThat(states).isNotEmpty().allMatch(state -> state.startsWith("customer_iam_role|pa27_customer|on|repeatable read|"));
        assertThat(new HashSet<>(states)).hasSize(1); states.clear();
        assertThat(service.getCustomerEvidence(caller, evidence)).isPresent(); fixture.cleanPool();
        assertThat(service.getCustomerEvidence(caller, UUID.randomUUID())).isEmpty(); fixture.cleanPool();
        try (var connection = fixture.admin()) { execute(connection, "SET ROLE evidence_owner; REVOKE SELECT (id) ON evidence.evidence_record FROM customer_iam_role"); }
        try { assertThatThrownBy(() -> service.listCustomerEvidence(caller, request)).isInstanceOfSatisfying(
                com.onecall.core.customer_iam.internal.application.CustomerContextException.class, error -> assertThat(error.code()).isEqualTo(com.onecall.core.customer_iam.internal.application.CustomerContextException.Code.SERVICE_UNAVAILABLE)); }
        finally { try (var connection = fixture.admin()) { execute(connection, "SET ROLE evidence_owner; GRANT SELECT (id) ON evidence.evidence_record TO customer_iam_role"); } }
        fixture.cleanPool(); assertThat(service.listCustomerEvidence(caller, request).items()).hasSize(1); fixture.cleanPool();
        assertThat(acls()).isEqualTo(currentOracle());
    }
}
