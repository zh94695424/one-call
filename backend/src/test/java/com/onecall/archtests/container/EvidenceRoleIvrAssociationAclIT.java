package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadPersistenceConfiguration;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.jooq.DSLContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

public class EvidenceRoleIvrAssociationAclIT {
    private static final Path SCRIPT = Path.of("src/main/resources/db/migration/V018__evidence_role_ivr_association_read_privileges.sql");
    private static final List<String> OPERATIONS = List.of("operations_iam_role", "subject_number_role", "activation_consent_role", "entitlement_metering_role", "evidence_role", "catalog_order_role", "ivr_role");
    private static final List<String> LOGINS = List.of("pa27_query", "pa27_customer", "pa27_command", "pa36_operations", "sharing_public_read_role", "sharing_admission_role", "evidence_media_reader_role");
    private static final List<String> RECORD_COLUMNS = List.of("id", "subject_id", "status", "source_type", "source_ref_id", "identity_at_capture", "session_id", "call_id");
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
    private final Map<String, String> passwords = new LinkedHashMap<>();
    private CustomerBusinessNumbersFixture fixture;

    @BeforeEach void start() throws Exception { fixture = new CustomerBusinessNumbersFixture(); fixture.start("017"); }
    @AfterEach void stop() { if (fixture != null) fixture.close(); }
    private Flyway latest() { return Flyway.configure().dataSource(fixture.url, "fixture_admin", fixture.password).locations("classpath:db/migration").target("018").load(); }
    private void upgrade() { assertThat(latest().migrate().migrationsExecuted).isEqualTo(1); latest().validate(); }
    public static Set<String> associationDelta() { return Set.of(
            "evidence_role|column|evidence.evidence_record|session_id|SELECT|evidence_owner|false",
            "evidence_role|column|evidence.evidence_record|call_id|SELECT|evidence_owner|false"); }
    public static Set<String> currentOracle() { return Set.of(
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
            "sharing_role|schema|sharing||USAGE|sharing_owner|false",
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
            "customer_iam_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false",
            "customer_iam_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false",
            "customer_iam_role|schema|evidence||USAGE|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_record|subject_id|SELECT|evidence_owner|false",
            "customer_iam_role|schema|catalog_order||USAGE|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.customer_order|subject_id|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.customer_order|account_id|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|catalog_order.customer_order|owner_organization_id|SELECT|catalog_order_owner|false",
            "customer_iam_role|column|evidence.evidence_record|id|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_record|status|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_record|source_type|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_record|source_ref_id|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_record|identity_at_capture|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_record|consent_version|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_record|created_at|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_record|solidified_at|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_artifact|evidence_id|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_artifact|media_object_id|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.evidence_artifact|artifact_role|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.media_object|id|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.media_object|sha256|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.media_object|size_bytes|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.media_object|mime_type|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.recording|id|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.recording|media_object_id|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.recording|status|SELECT|evidence_owner|false",
            "customer_iam_role|column|evidence.recording|validated_duration|SELECT|evidence_owner|false",
            "evidence_role|column|evidence.evidence_record|session_id|SELECT|evidence_owner|false",
            "evidence_role|column|evidence.evidence_record|call_id|SELECT|evidence_owner|false",
            "operations_iam_role|schema|platform||USAGE|platform_reliability_owner|false",
            "operations_iam_role|routine|platform.idempotency_begin(text,text,text)||EXECUTE|platform_reliability_owner|false",
            "operations_iam_role|routine|platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)||EXECUTE|platform_reliability_owner|false",
            "operations_iam_role|routine|platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)||EXECUTE|platform_reliability_owner|false",
            "operations_iam_role|schema|audit||USAGE|audit_owner|false",
            "operations_iam_role|routine|audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)||EXECUTE|audit_owner|false",
            "operations_iam_role|schema|operations_iam||USAGE|operations_iam_owner|false",
            "operations_iam_role|table|operations_iam.employee_account||SELECT|operations_iam_owner|false",
            "operations_iam_role|table|operations_iam.employee_account||INSERT|operations_iam_owner|false",
            "operations_iam_role|column|operations_iam.employee_account|display_name|UPDATE|operations_iam_owner|false",
            "operations_iam_role|column|operations_iam.employee_account|role|UPDATE|operations_iam_owner|false",
            "operations_iam_role|column|operations_iam.employee_account|status|UPDATE|operations_iam_owner|false",
            "operations_iam_role|column|operations_iam.employee_account|revision|UPDATE|operations_iam_owner|false",
            "operations_iam_role|routine|operations_iam.guard_employee_account()||EXECUTE|operations_iam_owner|false",
            "ivr_role|schema|platform||USAGE|platform_reliability_owner|false",
            "ivr_role|routine|platform.idempotency_begin(text,text,text)||EXECUTE|platform_reliability_owner|false",
            "ivr_role|routine|platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)||EXECUTE|platform_reliability_owner|false",
            "ivr_role|routine|platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)||EXECUTE|platform_reliability_owner|false",
            "ivr_role|schema|audit||USAGE|audit_owner|false",
            "ivr_role|routine|audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)||EXECUTE|audit_owner|false",
            "ivr_role|schema|activation_consent||USAGE|activation_consent_owner|false",
            "ivr_role|schema|entitlement_metering||USAGE|entitlement_metering_owner|false",
            "ivr_role|schema|ivr||USAGE|ivr_owner|false",
            "ivr_role|table|ivr.outbound_call_task||SELECT|ivr_owner|false",
            "ivr_role|table|ivr.outbound_call_task||INSERT|ivr_owner|false",
            "ivr_role|column|ivr.outbound_call_task|status|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.outbound_call_task|reason_code|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.outbound_call_task|claimed_call_id|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.outbound_call_task|claimed_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.outbound_call_task|terminal_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.outbound_call_task|updated_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.outbound_call_task|revision|UPDATE|ivr_owner|false",
            "ivr_role|table|ivr.inbound_session||SELECT|ivr_owner|false",
            "ivr_role|table|ivr.inbound_session||INSERT|ivr_owner|false",
            "ivr_role|column|ivr.inbound_session|status|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.inbound_session|confirmed_subject_id|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.inbound_session|guidance_status|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.inbound_session|confirmed_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.inbound_session|revision|UPDATE|ivr_owner|false",
            "ivr_role|table|ivr.call_record||SELECT|ivr_owner|false",
            "ivr_role|table|ivr.call_record||INSERT|ivr_owner|false",
            "ivr_role|column|ivr.call_record|task_id|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_record|status|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_record|started_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_record|bridged_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_record|recording_started_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_record|recording_ended_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_record|ended_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_record|authoritative_seconds|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_record|end_reason|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_record|revision|UPDATE|ivr_owner|false",
            "ivr_role|table|ivr.call_leg||SELECT|ivr_owner|false",
            "ivr_role|table|ivr.call_leg||INSERT|ivr_owner|false",
            "ivr_role|column|ivr.call_leg|state|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_leg|answered_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_leg|ended_at|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_leg|end_reason|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_leg|revision|UPDATE|ivr_owner|false",
            "ivr_role|table|ivr.call_event||SELECT|ivr_owner|false",
            "ivr_role|table|ivr.call_event||INSERT|ivr_owner|false",
            "ivr_role|column|ivr.call_event|processing_status|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_event|reason_code|UPDATE|ivr_owner|false",
            "ivr_role|column|ivr.call_event|revision|UPDATE|ivr_owner|false",
            "ivr_role|table|ivr.ivr_decision||SELECT|ivr_owner|false",
            "ivr_role|table|ivr.ivr_decision||INSERT|ivr_owner|false",
            "ivr_role|routine|ivr.guard_outbound_call_task()||EXECUTE|ivr_owner|false",
            "ivr_role|routine|ivr.guard_inbound_session()||EXECUTE|ivr_owner|false",
            "ivr_role|routine|ivr.guard_call_record_parties()||EXECUTE|ivr_owner|false",
            "ivr_role|routine|ivr.guard_call_leg()||EXECUTE|ivr_owner|false",
            "ivr_role|schema|customer_iam||USAGE|customer_iam_owner|false",
            "ivr_role|column|customer_iam.customer_account|id|SELECT|customer_iam_owner|false",
            "ivr_role|column|customer_iam.customer_account|status|SELECT|customer_iam_owner|false",
            "ivr_role|schema|organization_access||USAGE|organization_access_owner|false",
            "ivr_role|column|organization_access.organization|id|SELECT|organization_access_owner|false",
            "ivr_role|column|organization_access.organization|status|SELECT|organization_access_owner|false",
            "ivr_role|column|organization_access.organization_membership|organization_id|SELECT|organization_access_owner|false",
            "ivr_role|column|organization_access.organization_membership|account_id|SELECT|organization_access_owner|false",
            "ivr_role|column|organization_access.organization_membership|role|SELECT|organization_access_owner|false",
            "ivr_role|column|organization_access.organization_membership|status|SELECT|organization_access_owner|false",
            "ivr_role|schema|subject_number||USAGE|subject_number_owner|false",
            "ivr_role|column|subject_number.business_subject|id|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.business_subject|owner_account_id|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.business_subject|owner_organization_id|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.business_subject|revision|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.caller_number_binding|id|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.caller_number_binding|subject_id|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.caller_number_binding|number_hmac|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.caller_number_binding|status|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.caller_number_binding|verified_at|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.caller_number_binding|revision|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.subject_risk_state|caller_number_binding_id|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.subject_risk_state|risk_type|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.subject_risk_state|state|SELECT|subject_number_owner|false",
            "ivr_role|column|subject_number.subject_risk_state|revision|SELECT|subject_number_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_balance|id|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_balance|grant_id|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_balance|granted_units|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_balance|reserved_units|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_balance|consumed_units|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_balance|status|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_grant|id|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_grant|subject_id|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_grant|entitlement_type|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_grant|granted_units|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_grant|valid_from|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|entitlement_metering.entitlement_grant|expires_at|SELECT|entitlement_metering_owner|false",
            "ivr_role|column|activation_consent.service_activation|id|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.service_activation|subject_id|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.service_activation|status|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.service_activation|revision|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.consent_record|subject_id|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.consent_record|purpose|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.consent_record|channel|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.consent_record|policy_code|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.consent_record|policy_version|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.consent_record|prompt_version|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.consent_record|action|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.consent_record|event_domain|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.consent_record|event_id|SELECT|activation_consent_owner|false",
            "ivr_role|column|activation_consent.consent_record|accepted_at|SELECT|activation_consent_owner|false"
    ); }
    static Set<String> additionalOracle() { Set<String> result = new TreeSet<>(currentOracle()); result.removeIf(value -> !value.startsWith("operations_iam_role|") && !value.startsWith("ivr_role|") && !associationDelta().contains(value)); return result; }
    private static List<String> strings(Connection connection, String sql) throws SQLException {
        List<String> result = new ArrayList<>(); try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) { while (rows.next()) result.add(rows.getString(1)); } return result;
    }
    private Set<String> acls() throws Exception { try (var connection = fixture.admin()) { return new TreeSet<>(strings(connection, ACL)); } }
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
    private String preserved(Connection connection) throws Exception {
        return one(connection, """
                SELECT encode(sha256(convert_to(jsonb_build_array(
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_authid value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_auth_members value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY objoid,classoid) FROM pg_shdescription value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_default_acl value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY setdatabase,setrole) FROM pg_db_role_setting value),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY oid) FROM pg_namespace value),
                  (SELECT jsonb_agg(to_jsonb(value)-'relpages'-'reltuples'-'relallvisible'-'relfrozenxid'-'relminmxid' ORDER BY oid) FROM pg_class value),
                  (SELECT jsonb_agg(to_jsonb(value)-'attacl' ORDER BY attrelid,attnum) FROM pg_attribute value),
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
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY dbid,classid,objid,objsubid,refclassid,refobjid,deptype) FROM pg_shdepend value WHERE refobjid<>'evidence_role'::regrole),
                  (SELECT jsonb_agg(to_jsonb(value) ORDER BY installed_rank) FROM public.flyway_schema_history value WHERE installed_rank<=17))::text, 'UTF8')), 'hex')
                """) + fixture.businessSnapshot();
    }
    private void denied(Connection connection, String sql) throws Exception {
        var savepoint = connection.setSavepoint();
        try { assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class, error -> assertThat(error.getSQLState()).isEqualTo("42501")); }
        finally { connection.rollback(savepoint); }
    }
    private void rejects(String mutation) throws Exception {
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false); execute(connection, mutation);
            String role = one(connection, "SELECT quote_ident(current_user)"); execute(connection, "RESET ROLE");
            String before = snapshot(connection); execute(connection, "SET LOCAL ROLE " + role); var savepoint = connection.setSavepoint();
            assertThatThrownBy(() -> execute(connection, Files.readString(SCRIPT))).isInstanceOfSatisfying(SQLException.class, error -> {
                assertThat(error.getSQLState()).isEqualTo("P0001");
                assertThat(error.getMessage()).matches("(?s).*V018_(AUTHORITY|PROVENANCE|HISTORY|SETTINGS|ATTRIBUTES|TOPOLOGY|OWNERSHIP|ACL|DATABASE|DEFAULT_ACL|DEPENDENCY|PRESERVATION|RECONCILIATION)_REJECTED.*").doesNotContain(fixture.password, "PA36_SECRET_CANARY");
            });
            connection.rollback(savepoint); execute(connection, "RESET ROLE"); assertThat(snapshot(connection).equals(before)).isTrue(); connection.rollback();
        }
    }
    private org.testcontainers.containers.Container.ExecResult psql(String mode, String operations) throws Exception {
        return fixture.postgres.execInContainer("psql", "-X", "-w", "-U", "fixture_admin", "-d", "pa27", "-v", "mode=" + mode, "-v", "sharing_login=pa27_query", "-v", "customer_login=pa27_customer", "-v", "command_login=pa27_command", "-v", "operations_login=" + operations, "-f", "/tmp/pa27-identities.sql");
    }
    private void success(String mode) throws Exception {
        var result = psql(mode, "pa36_operations"); assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        assertThat(result.getStdout().strip()).isEqualTo("PA25_" + mode.toUpperCase(Locale.ROOT) + "_OK logins=7 effective_roles=9 memberships=11"); assertThat(result.getStderr()).isEmpty();
    }
    private void pa25Rejects() throws Exception {
        pa25Rejects("PA25_");
    }
    private void pa25Rejects(String category) throws Exception {
        String before; try (var connection = fixture.admin()) { before = snapshot(connection); }
        for (String mode : List.of("apply", "check")) {
            var result = psql(mode, "pa36_operations"); assertThat(result.getExitCode()).isNotZero();
            assertThat(result.getStdout() + result.getStderr()).contains(category).doesNotContain("PA25_APPLY_OK", "PA25_CHECK_OK", fixture.password, "PA36_SECRET_CANARY");
            try (var connection = fixture.admin()) { assertThat(snapshot(connection).equals(before)).isTrue(); }
        }
    }
    private void contaminate(String mutation, String undo) throws Exception {
        try (var connection = fixture.admin()) { execute(connection, mutation); }
        try { pa25Rejects(); } finally { try (var connection = fixture.admin()) { execute(connection, undo); } }
    }
    private void provision() throws Exception {
        success("apply"); success("check");
        try (var connection = fixture.admin()) { for (String login : LOGINS) { String password = UUID.randomUUID().toString(); passwords.put(login, password); execute(connection, "ALTER ROLE " + login + " PASSWORD '" + password + "'"); } }
    }
    private Connection login(String name) throws Exception { return DriverManager.getConnection(fixture.url, name, passwords.get(name)); }
    private void oldGraph() throws Exception {
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
    private void seed() throws Exception {
        fixture.seed();
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false);
            String subject = one(connection, "SELECT id FROM subject_number.business_subject ORDER BY id LIMIT 1");
            for (int index = 0; index < 3; index++) {
                UUID evidence = UUID.randomUUID(); UUID session = UUID.randomUUID(); String call = "pa36-" + UUID.randomUUID(); UUID recording = UUID.randomUUID(); String event = UUID.randomUUID().toString();
                if (index > 0) {
                    execute(connection, "INSERT INTO ivr.inbound_session(id,call_id,trusted_caller,access_number_masked,node_id,protocol_version,prompt_version) VALUES (?, ?, false, '***', 'node', '1', '1')", session, call);
                    String callerHmac = "%064x".formatted(index + 1);
                    execute(connection, "INSERT INTO ivr.call_record(id,call_id,session_id,caller_ciphertext,caller_number_hmac,caller_masked,access_number_masked,node_id,protocol_version) VALUES (?, ?, ?, 'v1.key.cipher', ?, '***', '***', 'node', '1')", UUID.randomUUID(), call, session, callerHmac);
                }
                execute(connection, """
                        INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,source_event_id,
                          identity_at_capture,consent_version,snapshot,snapshot_sha256,status,call_id,session_id)
                        VALUES (?,?::uuid,'CALL_RECORDING',?,'EVIDENCE',?,
                          jsonb_build_object('subjectId',?::text,'capturedAt','2026-11-16T00:00:00Z','sourceChannel','IVR'),
                          'v1',jsonb_build_object('capture',jsonb_build_object('capturedAt','2026-11-16T00:00:00Z'),'content','synthetic','sourceEventDomain','EVIDENCE','sourceEventId',?::text,
                          'source',jsonb_build_object('subjectId',?::text,'recordingId',?::text,'callId',?::text,'sessionId',?::text)),repeat('b',64),'PENDING',?,?)
                        """, evidence, subject, recording, event, subject, event, subject, recording,
                        index == 0 ? null : call, index == 2 ? session : null, index == 0 ? null : call, index == 2 ? session : null);
                execute(connection, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) VALUES (?, ?, 'EVIDENCE', ?, 'EVIDENCE_CREATED', '{}'::jsonb, repeat('a',64))", UUID.randomUUID(), evidence, event);
            }
            connection.commit();
        }
    }

    @Test
    void v017DeniesBothAssociationColumnsWith42501() throws Exception {
        Set<String> observed = acls(); assertThat(observed).hasSize(408);
        assertThat(observed.stream().filter(value -> value.startsWith("evidence_role|"))).hasSize(16);
        assertThat(observed.stream().filter(value -> !value.startsWith("operations_iam_role|") && !value.startsWith("ivr_role|"))).hasSize(288);
        oldGraph(); try (var connection = DriverManager.getConnection(fixture.url, "pa27_command", fixture.password)) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE evidence_role");
            for (String column : List.of("session_id", "call_id")) denied(connection, "SELECT " + column + " FROM evidence.evidence_record");
            connection.rollback();
        }
    }
    @Test
    void freshAndUpgradeHaveExactEighteenHistoryAndFrozenPredecessors() throws Exception {
        List<String> before; try (var connection = fixture.admin()) { before = strings(connection, "SELECT (to_jsonb(fact)-'installed_on')::text FROM public.flyway_schema_history fact ORDER BY installed_rank"); }
        assertThat(latest().info().pending()[0].getChecksum()).isEqualTo(1606293398); upgrade();
        try (var connection = fixture.admin()) { assertThat(strings(connection, "SELECT (to_jsonb(fact)-'installed_on')::text FROM public.flyway_schema_history fact WHERE installed_rank<=17 ORDER BY installed_rank")).isEqualTo(before); }
        assertThat(latest().info().applied()).hasSize(18); assertThat(latest().migrate().migrationsExecuted).isZero();
        fixture.close(); fixture = new CustomerBusinessNumbersFixture(); fixture.start(null); latest().validate(); assertThat(latest().info().applied()).hasSize(18); assertThat(acls()).isEqualTo(currentOracle());
    }
    @Test
    void v018AddsExactlyTwoOwnerIssuedTuplesAndMatches410Oracle() throws Exception {
        Set<String> before = acls(); upgrade(); Set<String> after = acls(); assertThat(after).hasSize(410).isEqualTo(currentOracle());
        Set<String> additions = new TreeSet<>(after); additions.removeAll(before); assertThat(additions).isEqualTo(associationDelta());
        Set<String> removals = new TreeSet<>(before); removals.removeAll(after); assertThat(removals).isEmpty();
        assertThat(after.stream().filter(value -> value.startsWith("customer_iam_role|"))).hasSize(53);
        assertThat(after.stream().filter(value -> value.startsWith("evidence_role|"))).hasSize(18);
        assertThat(after.stream().filter(value -> value.startsWith("operations_iam_role|"))).hasSize(14);
        assertThat(after.stream().filter(value -> value.startsWith("ivr_role|"))).hasSize(106);
        assertThat(after.stream().filter(value -> !value.startsWith("operations_iam_role|") && !value.startsWith("ivr_role|"))).hasSize(290);
    }
    @Test
    void evidenceRoleReadsAllEightColumnsAndDeniesEveryUngrantableColumn() throws Exception {
        upgrade(); provision(); seed();
        try (var admin = fixture.admin(); var connection = login("pa27_command")) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE evidence_role");
            assertThat(one(connection, "SELECT count(*) FROM (SELECT " + String.join(",", RECORD_COLUMNS) + " FROM evidence.evidence_record) facts")).isEqualTo("3");
            for (String column : RECORD_COLUMNS) execute(connection, "SELECT " + column + " FROM evidence.evidence_record");
            assertThat(one(connection, "SELECT count(session_id)||'|'||count(call_id) FROM evidence.evidence_record")).isEqualTo("1|2");
            for (String column : strings(admin, "SELECT attname FROM pg_attribute WHERE attrelid='evidence.evidence_record'::regclass AND attnum>0 AND NOT attisdropped")) if (!RECORD_COLUMNS.contains(column)) denied(connection, "SELECT " + column + " FROM evidence.evidence_record");
            for (String sql : List.of("SELECT * FROM evidence.evidence_record", "UPDATE evidence.evidence_record SET status=status", "DELETE FROM evidence.evidence_record", "INSERT INTO evidence.evidence_record DEFAULT VALUES", "SELECT id FROM evidence.evidence_record FOR UPDATE", "SELECT id FROM evidence.evidence_record FOR SHARE", "CREATE TABLE evidence.pa36_forbidden(id integer)", "SELECT object_key FROM evidence.media_object", "SELECT snapshot FROM evidence.evidence_record", "SELECT evidence.guard_evidence_record()")) denied(connection, sql);
            var savepoint = connection.setSavepoint();
            try {
                execute(connection, "GRANT SELECT (call_id) ON evidence.evidence_record TO customer_iam_role");
                try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT has_column_privilege('customer_iam_role', 'evidence.evidence_record', 'call_id', 'SELECT')")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject(1, Boolean.class)).isEqualTo(false);
                }
            } finally { connection.rollback(savepoint); }
            connection.rollback();
        }
        assertThat(acls()).isEqualTo(currentOracle());
    }
    @Test
    void v018ReplaysOnlyExactCleanOrCompleteStatesWithoutDrift() throws Exception {
        try (var connection = fixture.admin()) { execute(connection, Files.readString(SCRIPT)); String before = snapshot(connection); execute(connection, Files.readString(SCRIPT)); assertThat(snapshot(connection).equals(before)).isTrue(); }
        upgrade();
        for (int topology = 0; topology < 3; topology++) {
            if (topology == 1) oldGraph(); if (topology == 2) success("apply");
            try (var connection = fixture.admin()) { String before = snapshot(connection); execute(connection, Files.readString(SCRIPT)); assertThat(snapshot(connection).equals(before)).isTrue(); }
            if (topology == 1) try (var connection = fixture.admin()) {
                connection.setAutoCommit(false); execute(connection, "DELETE FROM public.flyway_schema_history WHERE installed_rank=18"); String before = snapshot(connection);
                execute(connection, Files.readString(SCRIPT)); assertThat(snapshot(connection).equals(before)).isTrue(); connection.rollback();
            }
        }
        assertThat(latest().migrate().migrationsExecuted).isZero(); assertThat(acls()).isEqualTo(currentOracle());
    }
    @Test
    void v018RejectsPartialExtraWrongGrantorAndGrantOptionStatesAtomically() throws Exception {
        for (String column : List.of("session_id", "call_id")) rejects("SET LOCAL ROLE evidence_owner; GRANT SELECT (" + column + ") ON evidence.evidence_record TO evidence_role; RESET ROLE");
        for (String capability : List.of("SELECT (snapshot)", "SELECT")) rejects("SET LOCAL ROLE evidence_owner; GRANT " + capability + " ON evidence.evidence_record TO evidence_role; RESET ROLE");
        upgrade();
        for (String column : List.of("session_id", "call_id")) {
            rejects("SET LOCAL ROLE evidence_owner; REVOKE SELECT (" + column + ") ON evidence.evidence_record FROM evidence_role; RESET ROLE");
            rejects("SET LOCAL ROLE evidence_owner; GRANT SELECT (" + column + ") ON evidence.evidence_record TO evidence_role WITH GRANT OPTION; RESET ROLE");
            rejects("UPDATE pg_attribute SET attacl=ARRAY(SELECT replace(entry::text,'/evidence_owner','/fixture_admin')::aclitem FROM unnest(attacl) entry) WHERE attrelid='evidence.evidence_record'::regclass AND attname='" + column + "'");
        }
        rejects("SET LOCAL ROLE evidence_owner; GRANT SELECT (snapshot) ON evidence.evidence_record TO evidence_role; RESET ROLE");
    }
    @Test
    void v018RejectsAuthorityHistoryAndProvenanceDriftAtomically() throws Exception {
        assertThat(Files.readString(SCRIPT)).contains("160000 AND 169999", "current_user <> session_user");
        rejects("SET LOCAL ROLE evidence_role");
        for (int rank = 1; rank <= 17; rank++) {
            for (String assignment : List.of("checksum=0", "version='wrong'", "installed_rank=99", "type='BASELINE'", "success=false", "description='wrong'", "script='wrong.sql'")) rejects("UPDATE public.flyway_schema_history SET " + assignment + " WHERE installed_rank=" + rank);
            rejects("DELETE FROM public.flyway_schema_history WHERE installed_rank=" + rank);
        }
        rejects("INSERT INTO public.flyway_schema_history SELECT 18,'018','evidence role ivr association read privileges','SQL','V018__evidence_role_ivr_association_read_privileges.sql',1,installed_by,installed_on,0,true FROM public.flyway_schema_history WHERE installed_rank=17");
        rejects("INSERT INTO public.flyway_schema_history SELECT 99,version,description,type,script,checksum,installed_by,installed_on,execution_time,success FROM public.flyway_schema_history WHERE installed_rank=17");
        oldGraph();
        for (String marker : List.of("onecall:pa25:v1:stale", "onecall:pa25:v1:db=0:role=0:slot=operations_ivr_read:name=PA36_SECRET_CANARY")) rejects("COMMENT ON ROLE pa27_customer IS '" + marker + "'");
        rejects("COMMENT ON ROLE pa27_customer IS NULL"); rejects("ALTER ROLE pa27_customer RENAME TO pa36_renamed");
        try (var connection = DriverManager.getConnection(fixture.url, "pa27_command", fixture.password)) {
            assertThatThrownBy(() -> execute(connection, Files.readString(SCRIPT))).isInstanceOfSatisfying(SQLException.class, failure -> { assertThat(failure.getSQLState()).isEqualTo("P0001"); assertThat(failure.getMessage()).contains("V018_AUTHORITY_REJECTED").doesNotContain(fixture.password); });
        }
        rejects("CREATE ROLE pa36_operations LOGIN NOINHERIT; DO $$ BEGIN EXECUTE format('COMMENT ON ROLE pa36_operations IS %L',format('onecall:pa25:v1:db=%s:role=%s:slot=operations_ivr_read:name=pa36_operations',(SELECT oid FROM pg_database WHERE datname=current_database()),'pa36_operations'::regrole::oid)); END $$; GRANT " + String.join(",", OPERATIONS) + " TO pa36_operations WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
        rejects("CREATE ROLE pa36_copied; DO $$ BEGIN EXECUTE format('COMMENT ON ROLE pa36_copied IS %L',shobj_description('pa27_customer'::regrole,'pg_authid')); END $$");
    }
    @Test
    void v018RejectsTopologyAttributesOwnershipSettingsAndDefaultDrift() throws Exception {
        oldGraph();
        for (String option : List.of("ADMIN TRUE", "INHERIT TRUE", "SET FALSE")) rejects("GRANT evidence_role TO pa27_command WITH " + option);
        for (String sql : List.of("REVOKE evidence_role FROM pa27_command", "GRANT ivr_role TO pa27_command WITH INHERIT FALSE", "GRANT evidence_role TO sharing_role WITH INHERIT FALSE", "ALTER ROLE ivr_role SUPERUSER", "ALTER ROLE subject_number_role PASSWORD 'PA36_SECRET_CANARY'", "ALTER ROLE evidence_role SET search_path='evidence'", "ALTER ROLE catalog_order_role SET statement_timeout='1s'", "ALTER TABLE evidence.evidence_record OWNER TO fixture_admin", "GRANT SELECT (snapshot) ON evidence.evidence_record TO PUBLIC", "GRANT CREATE ON DATABASE pa27 TO evidence_role", "ALTER DEFAULT PRIVILEGES FOR ROLE evidence_owner GRANT SELECT ON TABLES TO evidence_role", "ALTER DEFAULT PRIVILEGES FOR ROLE evidence_role REVOKE ALL ON TABLES FROM evidence_role", "ALTER EXTENSION plpgsql ADD TABLE evidence.evidence_record", "UPDATE pg_type SET typowner='fixture_admin'::regrole WHERE oid='evidence.evidence_record'::regtype", "UPDATE pg_auth_members SET grantor='evidence_role'::regrole WHERE member='pa27_command'::regrole AND roleid='evidence_role'::regrole", "GRANT CONNECT ON DATABASE postgres TO evidence_role", "ALTER DATABASE pa27 SET statement_timeout='1s'")) rejects(sql);
    }
    @Test
    void pa25RequiresV018AndExact410OracleInBothModes() throws Exception {
        pa25Rejects(); upgrade(); provision(); assertThat(acls()).isEqualTo(currentOracle());
        for (String tuple : currentOracle()) {
            if (!associationDelta().contains(tuple) && !tuple.equals("ivr_role|routine|ivr.guard_call_leg()||EXECUTE|ivr_owner|false") && !tuple.equals("operations_iam_role|table|operations_iam.employee_account||SELECT|operations_iam_owner|false")) continue;
            String[] fields = tuple.split("\\|", -1); String capability = switch (fields[1]) {
                case "routine" -> "EXECUTE ON FUNCTION " + fields[2];
                case "table" -> fields[4] + " ON TABLE " + fields[2];
                default -> fields[4] + " (" + fields[3] + ") ON " + fields[2];
            };
            contaminate("SET ROLE " + fields[5] + "; REVOKE " + capability + " FROM " + fields[0], "SET ROLE " + fields[5] + "; GRANT " + capability + " TO " + fields[0]);
        }
        for (String column : List.of("session_id", "call_id")) contaminate("SET ROLE evidence_owner; GRANT SELECT (" + column + ") ON evidence.evidence_record TO evidence_role WITH GRANT OPTION", "SET ROLE evidence_owner; REVOKE GRANT OPTION FOR SELECT (" + column + ") ON evidence.evidence_record FROM evidence_role");
        for (String assignment : List.of("checksum=0", "description='wrong'", "script='wrong.sql'")) {
            String row; try (var connection = fixture.admin()) { row = one(connection, "SELECT to_jsonb(fact)::text FROM public.flyway_schema_history fact WHERE installed_rank=18"); execute(connection, "UPDATE public.flyway_schema_history SET " + assignment + " WHERE installed_rank=18"); }
            try {
                pa25Rejects("PA25_HISTORY_REJECTED");
                if (!assignment.startsWith("script=")) assertThatThrownBy(() -> latest().validate()).isInstanceOf(org.flywaydb.core.api.FlywayException.class);
            }
            finally { try (var connection = fixture.admin()) { execute(connection, "DELETE FROM public.flyway_schema_history WHERE installed_rank=18; INSERT INTO public.flyway_schema_history SELECT * FROM jsonb_populate_record(NULL::public.flyway_schema_history,?::jsonb)", row); } }
        }
    }
    @Test
    void pa25UpgradesThreeSlotsToFourAndPreservesExistingCredentialBytes() throws Exception {
        oldGraph(); upgrade();
        String before; try (var connection = fixture.admin()) { before = one(connection, "SELECT jsonb_agg(to_jsonb(fact) ORDER BY oid)::text FROM pg_authid fact WHERE rolname IN ('pa27_query','pa27_customer','pa27_command')"); }
        success("apply"); success("check"); success("apply");
        try (var connection = fixture.admin()) {
            assertThat(one(connection, "SELECT jsonb_agg(to_jsonb(fact) ORDER BY oid)::text FROM pg_authid fact WHERE rolname IN ('pa27_query','pa27_customer','pa27_command')")).isEqualTo(before);
            assertThat(one(connection, "SELECT rolpassword IS NULL FROM pg_authid WHERE rolname='pa36_operations'")).isEqualTo("t");
            assertThat(one(connection, "SELECT shobj_description(oid,'pg_authid')=format('onecall:pa25:v1:db=%s:role=%s:slot=operations_ivr_read:name=%s',(SELECT oid FROM pg_database WHERE datname=current_database()),oid,rolname) FROM pg_roles WHERE rolname='pa36_operations'")).isEqualTo("t");
            assertThat(one(connection, "SELECT count(*) FROM pg_auth_members WHERE member='pa36_operations'::regrole AND NOT admin_option AND NOT inherit_option AND set_option")).isEqualTo("7");
        }
    }
    @Test
    void pa25RejectsInvalidOperationsInputAndCollidingProvenance() throws Exception {
        upgrade();
        for (String mode : List.of("apply", "check")) {
            var missing = fixture.postgres.execInContainer("psql", "-X", "-w", "-U", "fixture_admin", "-d", "pa27", "-v", "mode=" + mode, "-v", "sharing_login=pa27_query", "-v", "customer_login=pa27_customer", "-v", "command_login=pa27_command", "-f", "/tmp/pa27-identities.sql");
            assertThat(missing.getExitCode()).isNotZero(); assertThat(missing.getStdout() + missing.getStderr()).contains("PA25_INPUT_REJECTED").doesNotContain(fixture.password);
        }
        for (String input : List.of("", "pa27_query", "pg_bad", "public", "ivr_role", "operations_iam_role", "subject_number_role", "activation_consent_role", "entitlement_metering_role", "catalog_order_role", "evidence_owner", "evidence", "a".repeat(64), "PA36_SECRET_CANARY'; SELECT 1")) {
            for (String mode : List.of("apply", "check")) { String before; try (var connection = fixture.admin()) { before = snapshot(connection); } var result = psql(mode, input); assertThat(result.getExitCode()).isNotZero(); assertThat(result.getStdout() + result.getStderr()).contains("PA25_INPUT_REJECTED").doesNotContain(input.isEmpty() ? "PA36_SECRET_CANARY" : input); try (var connection = fixture.admin()) { assertThat(snapshot(connection).equals(before)).isTrue(); } }
        }
        try (var connection = fixture.admin()) { execute(connection, "CREATE ROLE pa36_operations LOGIN NOINHERIT"); } pa25Rejects();
        try (var connection = fixture.admin()) { execute(connection, "COMMENT ON ROLE pa36_operations IS 'onecall:pa25:v1:stale'"); } pa25Rejects();
    }
    @Test
    void pa25ApplyAndCheckEnforceExactElevenEdgeTopology() throws Exception {
        upgrade(); provision();
        for (String role : OPERATIONS) {
            try (var connection = fixture.admin()) { execute(connection, "REVOKE " + role + " FROM pa36_operations"); }
            var result = psql("check", "pa36_operations"); assertThat(result.getExitCode()).isNotZero(); success("apply"); success("check");
            for (String option : List.of("ADMIN TRUE", "INHERIT TRUE", "SET FALSE")) contaminate("GRANT " + role + " TO pa36_operations WITH " + option, "GRANT " + role + " TO pa36_operations WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
        }
        contaminate("GRANT sharing_role TO pa36_operations WITH INHERIT FALSE", "REVOKE sharing_role FROM pa36_operations");
        contaminate("GRANT ivr_role TO pa27_customer WITH INHERIT FALSE", "REVOKE ivr_role FROM pa27_customer");
        contaminate("GRANT ivr_role TO evidence_role WITH INHERIT FALSE", "REVOKE ivr_role FROM evidence_role");
        contaminate("GRANT pa27_customer TO pa36_operations WITH INHERIT FALSE", "REVOKE pa27_customer FROM pa36_operations");
        try (var connection = fixture.admin()) { execute(connection, "CREATE ROLE pa36_other_admin SUPERUSER NOLOGIN"); }
        try {
            contaminate("INSERT INTO pg_auth_members(oid,roleid,member,grantor,admin_option,inherit_option,set_option) SELECT (SELECT (max(oid)::bigint+1)::oid FROM pg_auth_members),roleid,member,'pa36_other_admin'::regrole,false,false,true FROM pg_auth_members WHERE member='pa36_operations'::regrole AND roleid='ivr_role'::regrole", "DELETE FROM pg_auth_members WHERE grantor='pa36_other_admin'::regrole");
            contaminate("UPDATE pg_auth_members SET grantor='evidence_role'::regrole WHERE member='pa36_operations'::regrole AND roleid='ivr_role'::regrole", "UPDATE pg_auth_members SET grantor='fixture_admin'::regrole WHERE member='pa36_operations'::regrole AND roleid='ivr_role'::regrole");
        } finally { try (var connection = fixture.admin()) { execute(connection, "DROP ROLE pa36_other_admin"); } }
    }
    @Test
    void operationsLoginSetsExactlySevenRolesAndRestoresSessionContext() throws Exception {
        upgrade(); provision();
        try (var application = new AnnotationConfigApplicationContext()) {
            application.getEnvironment().setActiveProfiles("persistence");
            application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa36", Map.of("onecall.operations-subject-read.enabled", "true", "onecall.operations-subject-read.datasource.url", fixture.url, "onecall.operations-subject-read.datasource.username", "pa36_operations", "onecall.operations-subject-read.datasource.password", passwords.get("pa36_operations"))));
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
    @Test
    void allOtherLoginAndDirectRoleBoundariesRemainExact() throws Exception {
        upgrade(); provision(); seed();
        List<String> targets; try (var connection = fixture.admin()) {
            targets = strings(connection, "SELECT rolname FROM pg_roles ORDER BY 1");
            assertThat(one(connection, "SELECT has_schema_privilege('pa36_operations','evidence','USAGE')")).isEqualTo("f");
            assertThat(one(connection, "SELECT has_schema_privilege('evidence_role','evidence','USAGE')")).isEqualTo("t");
            for (String column : RECORD_COLUMNS) assertThat(one(connection, "SELECT has_column_privilege('evidence_role','evidence.evidence_record','" + column + "','SELECT')")).isEqualTo("t");
            assertThat(one(connection, "SELECT count(*) FROM pg_roles WHERE rolname IN ('operations_iam_role','ivr_role','subject_number_role','activation_consent_role','entitlement_metering_role','catalog_order_role') AND rolinherit AND NOT rolcanlogin")).isEqualTo("6");
        }
        for (String name : LOGINS) try (var connection = login(name)) {
            connection.setAutoCommit(false);
            for (String target : targets) {
                if (target.equals(name) || name.equals("pa36_operations") && OPERATIONS.contains(target) || name.equals("pa27_command") && List.of("sharing_command_role", "evidence_role").contains(target) || name.equals("pa27_query") && target.equals("sharing_role") || name.equals("pa27_customer") && target.equals("customer_iam_role")) continue;
                denied(connection, "SET LOCAL ROLE \"" + target.replace("\"", "\"\"") + "\"");
            }
            if (name.equals("pa36_operations") || name.equals("pa27_command")) {
                denied(connection, "SELECT call_id FROM evidence.evidence_record"); execute(connection, "SET LOCAL ROLE evidence_role"); assertThat(one(connection, "SELECT count(session_id)||'|'||count(call_id) FROM evidence.evidence_record")).isEqualTo("1|2");
            }
            connection.rollback(); assertThat(one(connection, "SELECT current_user::text")).isEqualTo(name); connection.commit();
        }
    }
    @Test
    void pa25FailuresAreAtomicSanitizedAndNeverAdoptOrRestamp() throws Exception {
        upgrade();
        String verifier = Files.readString(Path.of("ops/postgresql/pa25-runtime-identities.sql")).replace("\r\n", "\n");
        String anchor = "    END LOOP;\nEXCEPTION WHEN OTHERS";
        assertThat(verifier.indexOf(anchor)).isGreaterThan(0);
        String faulted = verifier.replace(anchor, "        IF pass = 2 THEN RAISE EXCEPTION 'PA36_SECRET_CANARY'; END IF;\n" + anchor);
        fixture.postgres.copyFileToContainer(org.testcontainers.images.builder.Transferable.of(faulted.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "/tmp/pa27-identities.sql");
        try {
            var result = psql("apply", "pa36_operations"); assertThat(result.getExitCode()).isNotZero(); assertThat(result.getStdout() + result.getStderr()).contains("PA25_DEPENDENCY_REJECTED").doesNotContain("PA36_SECRET_CANARY", fixture.password);
            try (var connection = fixture.admin()) { assertThat(one(connection, "SELECT count(*) FROM pg_roles WHERE rolname IN ('pa27_query','pa27_customer','pa27_command','pa36_operations')")).isEqualTo("0"); }
        } finally { fixture.postgres.copyFileToContainer(org.testcontainers.images.builder.Transferable.of(verifier.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "/tmp/pa27-identities.sql"); }
        provision(); String before; try (var connection = fixture.admin()) { before = snapshot(connection); } success("check"); success("apply"); try (var connection = fixture.admin()) { assertThat(snapshot(connection).equals(before)).isTrue(); }
    }
    @Test
    void v018PreservesBusinessDefinitionsOwnersAndAllUnrelatedAcls() throws Exception {
        seed(); Set<String> before = acls();
        try (var connection = fixture.admin()) { execute(connection, "SET ROLE ivr_owner; REVOKE EXECUTE ON FUNCTION ivr.guard_call_leg() FROM ivr_role"); }
        String invariant; try (var connection = fixture.admin()) { invariant = preserved(connection); }
        upgrade(); try (var connection = fixture.admin()) { assertThat(preserved(connection).equals(invariant)).isTrue(); } Set<String> expected = new TreeSet<>(before); expected.addAll(associationDelta()); expected.remove("ivr_role|routine|ivr.guard_call_leg()||EXECUTE|ivr_owner|false"); assertThat(acls()).isEqualTo(expected); pa25Rejects();
        try (var connection = fixture.admin()) { String snapshot = snapshot(connection); execute(connection, Files.readString(SCRIPT)); assertThat(snapshot(connection).equals(snapshot)).isTrue(); }
        rejects("SET LOCAL ROLE evidence_owner; GRANT SELECT(snapshot) ON evidence.evidence_record TO evidence_role; RESET ROLE");
    }
}
