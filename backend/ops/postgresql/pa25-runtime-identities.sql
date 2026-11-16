\set ON_ERROR_STOP on
\set ECHO none
\set QUIET on
\set VERBOSITY terse
\set SHOW_CONTEXT never
\pset tuples_only on
\pset format unaligned
\if :{?mode}
\else
\set mode ''
\endif
\if :{?sharing_login}
\else
\set sharing_login ''
\endif
\if :{?customer_login}
\else
\set customer_login ''
\endif
\if :{?command_login}
\else
\set command_login ''
\endif
\if :{?operations_login}
\else
\set operations_login ''
\endif
BEGIN;
\o /dev/null
SELECT set_config('pa25.mode', :'mode', true),
       set_config('pa25.sharing_login', :'sharing_login', true),
       set_config('pa25.customer_login', :'customer_login', true),
       set_config('pa25.command_login', :'command_login', true),
       set_config('pa25.operations_login', :'operations_login', true);
DO $pa25$
DECLARE
    failure_category text := 'PA25_INPUT_REJECTED';
    requested_mode text := current_setting('pa25.mode');
    external_names text[] := ARRAY[current_setting('pa25.sharing_login'),
        current_setting('pa25.customer_login'), current_setting('pa25.command_login'), current_setting('pa25.operations_login')];
    slots text[] := ARRAY['sharing_query', 'customer_context', 'sharing_command', 'operations_ivr_read'];
    effective_names text[] := ARRAY['sharing_role', 'customer_iam_role', 'sharing_command_role',
        'evidence_role', 'sharing_public_read_role', 'sharing_admission_role', 'evidence_media_reader_role', 'operations_iam_role', 'ivr_role'];
    shared_context_targets text[] := ARRAY['subject_number_role', 'activation_consent_role', 'entitlement_metering_role', 'catalog_order_role'];
    boundary_names text[];
    boundary_oids oid[];
    managed_names text[];
    login_names text[];
    expected_acls jsonb := $frozen$[
  [
    "customer_iam_role",
    "column",
    "customer_iam.customer_account",
    "display_name",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "customer_iam.customer_account",
    "id",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "customer_iam.customer_account",
    "status",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "organization_access.organization",
    "display_name",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "organization_access.organization",
    "id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "organization_access.organization",
    "status",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "organization_access.organization_membership",
    "account_id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "organization_access.organization_membership",
    "organization_id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "organization_access.organization_membership",
    "role",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "organization_access.organization_membership",
    "status",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "customer_iam_role",
    "schema",
    "customer_iam",
    "",
    "USAGE",
    "customer_iam_owner"
  ],
  [
    "customer_iam_role",
    "schema",
    "organization_access",
    "",
    "USAGE",
    "organization_access_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.evidence_artifact",
    "artifact_role",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.evidence_artifact",
    "evidence_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.evidence_artifact",
    "media_object_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.evidence_record",
    "id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.evidence_record",
    "source_ref_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.evidence_record",
    "source_type",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.evidence_record",
    "status",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.media_object",
    "id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.media_object",
    "mime_type",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.media_object",
    "sha256",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.media_object",
    "size_bytes",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.recording",
    "id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.recording",
    "media_object_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.recording",
    "status",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "column",
    "evidence.recording",
    "validated_duration",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_media_reader_role",
    "schema",
    "evidence",
    "",
    "USAGE",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_artifact",
    "artifact_role",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_artifact",
    "evidence_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_artifact",
    "media_object_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_record",
    "id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_record",
    "identity_at_capture",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_record",
    "source_ref_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_record",
    "source_type",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_record",
    "status",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_record",
    "subject_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.media_object",
    "id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.media_object",
    "mime_type",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.media_object",
    "sha256",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.media_object",
    "size_bytes",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.recording",
    "media_object_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.recording",
    "validated_duration",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "schema",
    "evidence",
    "",
    "USAGE",
    "evidence_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "customer_iam.customer_account",
    "id",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "customer_iam.customer_account",
    "status",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "evidence.evidence_record",
    "id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "evidence.evidence_record",
    "identity_at_capture",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "evidence.evidence_record",
    "status",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "evidence.evidence_record",
    "subject_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "organization_access.organization",
    "id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "organization_access.organization",
    "status",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "organization_access.organization_membership",
    "account_id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "organization_access.organization_membership",
    "id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "organization_access.organization_membership",
    "organization_id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "organization_access.organization_membership",
    "revision",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "organization_access.organization_membership",
    "role",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "organization_access.organization_membership",
    "status",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "admissions_used",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "admissions_used",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "capability_token_hash",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "code_generation",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "code_hash",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "code_salt",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "content_sha256",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "content_snapshot",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "created_by_account_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "download_allowed",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "evidence_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "expires_at",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "max_admissions",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "owner_organization_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "revision",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "revision",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "revocation_generation",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "revocation_generation",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "share_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "status",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "status",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "valid_from",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.evidence_share_grant",
    "verification_mode",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "event_domain",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "event_domain",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "event_id",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "event_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "event_type",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "event_type",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "id",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "occurred_at",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "occurred_at",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "payload_redacted",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "payload_redacted",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "payload_sha256",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "payload_sha256",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "result",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "result",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "risk_digest",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "risk_digest",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "share_admission_id",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "share_admission_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "share_grant_id",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_access_event",
    "share_grant_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "admission_key",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "admission_key",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "admission_no",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "admission_no",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "admitted_at",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "admitted_at",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "id",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "share_grant_id",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "share_grant_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "verification_mode",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "verification_mode",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "verification_result",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "sharing.share_admission",
    "verification_result",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.business_subject",
    "id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.business_subject",
    "owner_account_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.business_subject",
    "owner_organization_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.business_subject",
    "revision",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.caller_number_binding",
    "id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.caller_number_binding",
    "number_hmac",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.caller_number_binding",
    "number_type",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.caller_number_binding",
    "revision",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.caller_number_binding",
    "status",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.caller_number_binding",
    "subject_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.caller_number_binding",
    "verified_at",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.subject_risk_state",
    "caller_number_binding_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.subject_risk_state",
    "cleared_at",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.subject_risk_state",
    "id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.subject_risk_state",
    "observed_at",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.subject_risk_state",
    "revision",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.subject_risk_state",
    "risk_type",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "column",
    "subject_number.subject_risk_state",
    "state",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_admission_role",
    "schema",
    "customer_iam",
    "",
    "USAGE",
    "customer_iam_owner"
  ],
  [
    "sharing_admission_role",
    "schema",
    "evidence",
    "",
    "USAGE",
    "evidence_owner"
  ],
  [
    "sharing_admission_role",
    "schema",
    "organization_access",
    "",
    "USAGE",
    "organization_access_owner"
  ],
  [
    "sharing_admission_role",
    "schema",
    "sharing",
    "",
    "USAGE",
    "sharing_owner"
  ],
  [
    "sharing_admission_role",
    "schema",
    "subject_number",
    "",
    "USAGE",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "customer_iam.customer_account",
    "id",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "customer_iam.customer_account",
    "status",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_artifact",
    "artifact_role",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_artifact",
    "evidence_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_artifact",
    "media_object_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_record",
    "call_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_record",
    "created_at",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_record",
    "id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_record",
    "identity_at_capture",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_record",
    "reservation_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_record",
    "session_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_record",
    "source_ref_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_record",
    "source_type",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_record",
    "status",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "evidence.evidence_record",
    "subject_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "organization_access.organization",
    "id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "organization_access.organization",
    "status",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "organization_access.organization_membership",
    "account_id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "organization_access.organization_membership",
    "organization_id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "organization_access.organization_membership",
    "role",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "organization_access.organization_membership",
    "status",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "sharing.evidence_share_grant",
    "admissions_used",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "sharing.evidence_share_grant",
    "code_generation",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "sharing.evidence_share_grant",
    "code_hash",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "sharing.evidence_share_grant",
    "code_salt",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "sharing.evidence_share_grant",
    "expires_at",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "sharing.evidence_share_grant",
    "revision",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "sharing.evidence_share_grant",
    "revocation_generation",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "sharing.evidence_share_grant",
    "status",
    "UPDATE",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.business_subject",
    "id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.business_subject",
    "owner_account_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.business_subject",
    "owner_organization_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.business_subject",
    "revision",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.caller_number_binding",
    "id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.caller_number_binding",
    "revision",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.caller_number_binding",
    "status",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.caller_number_binding",
    "subject_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.caller_number_binding",
    "verified_at",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.subject_risk_state",
    "caller_number_binding_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.subject_risk_state",
    "revision",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.subject_risk_state",
    "risk_type",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "column",
    "subject_number.subject_risk_state",
    "state",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "routine",
    "audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)",
    "",
    "EXECUTE",
    "audit_owner"
  ],
  [
    "sharing_command_role",
    "routine",
    "platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)",
    "",
    "EXECUTE",
    "platform_reliability_owner"
  ],
  [
    "sharing_command_role",
    "schema",
    "audit",
    "",
    "USAGE",
    "audit_owner"
  ],
  [
    "sharing_command_role",
    "schema",
    "customer_iam",
    "",
    "USAGE",
    "customer_iam_owner"
  ],
  [
    "sharing_command_role",
    "schema",
    "evidence",
    "",
    "USAGE",
    "evidence_owner"
  ],
  [
    "sharing_command_role",
    "schema",
    "organization_access",
    "",
    "USAGE",
    "organization_access_owner"
  ],
  [
    "sharing_command_role",
    "schema",
    "platform",
    "",
    "USAGE",
    "platform_reliability_owner"
  ],
  [
    "sharing_command_role",
    "schema",
    "sharing",
    "",
    "USAGE",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "schema",
    "subject_number",
    "",
    "USAGE",
    "subject_number_owner"
  ],
  [
    "sharing_command_role",
    "table",
    "sharing.evidence_share_grant",
    "",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "table",
    "sharing.evidence_share_grant",
    "",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "table",
    "sharing.share_access_event",
    "",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "table",
    "sharing.share_access_event",
    "",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "table",
    "sharing.share_admission",
    "",
    "INSERT",
    "sharing_owner"
  ],
  [
    "sharing_command_role",
    "table",
    "sharing.share_admission",
    "",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.evidence_artifact",
    "artifact_role",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.evidence_artifact",
    "evidence_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.evidence_artifact",
    "media_object_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.evidence_record",
    "id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.evidence_record",
    "identity_at_capture",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.evidence_record",
    "source_ref_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.evidence_record",
    "source_type",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.evidence_record",
    "status",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.evidence_record",
    "subject_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.media_object",
    "id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.media_object",
    "mime_type",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.media_object",
    "sha256",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.media_object",
    "size_bytes",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.recording",
    "media_object_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "evidence.recording",
    "validated_duration",
    "SELECT",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "sharing.evidence_share_grant",
    "capability_token_hash",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "sharing.evidence_share_grant",
    "content_snapshot",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "sharing.evidence_share_grant",
    "download_allowed",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "sharing.evidence_share_grant",
    "evidence_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "sharing.evidence_share_grant",
    "expires_at",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "sharing.evidence_share_grant",
    "owner_organization_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "sharing.evidence_share_grant",
    "status",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "sharing.evidence_share_grant",
    "valid_from",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_public_read_role",
    "column",
    "sharing.evidence_share_grant",
    "verification_mode",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_public_read_role",
    "schema",
    "evidence",
    "",
    "USAGE",
    "evidence_owner"
  ],
  [
    "sharing_public_read_role",
    "schema",
    "sharing",
    "",
    "USAGE",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "customer_iam.customer_account",
    "id",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "sharing_role",
    "column",
    "customer_iam.customer_account",
    "status",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "sharing_role",
    "column",
    "organization_access.organization",
    "id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_role",
    "column",
    "organization_access.organization",
    "status",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_role",
    "column",
    "organization_access.organization_membership",
    "account_id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_role",
    "column",
    "organization_access.organization_membership",
    "organization_id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_role",
    "column",
    "organization_access.organization_membership",
    "status",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "admissions_used",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "code_generation",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "created_by_account_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "download_allowed",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "evidence_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "expires_at",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "max_admissions",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "owner_organization_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "revocation_generation",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "share_id",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "status",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "valid_from",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "column",
    "sharing.evidence_share_grant",
    "verification_mode",
    "SELECT",
    "sharing_owner"
  ],
  [
    "sharing_role",
    "schema",
    "customer_iam",
    "",
    "USAGE",
    "customer_iam_owner"
  ],
  [
    "sharing_role",
    "schema",
    "organization_access",
    "",
    "USAGE",
    "organization_access_owner"
  ],
  [
    "sharing_role",
    "schema",
    "sharing",
    "",
    "USAGE",
    "sharing_owner"
  ],
  [
    "customer_iam_role",
    "schema",
    "subject_number",
    "",
    "USAGE",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.business_subject",
    "id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.business_subject",
    "owner_account_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.business_subject",
    "owner_organization_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.business_subject",
    "real_name_status",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.business_subject",
    "created_at",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.caller_number_binding",
    "id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.caller_number_binding",
    "subject_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.caller_number_binding",
    "number_masked",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.caller_number_binding",
    "number_type",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.caller_number_binding",
    "status",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "schema",
    "activation_consent",
    "",
    "USAGE",
    "activation_consent_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "activation_consent.service_activation",
    "subject_id",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "activation_consent.service_activation",
    "status",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.subject_risk_state",
    "caller_number_binding_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "subject_number.subject_risk_state",
    "state",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "customer_iam_role",
    "schema",
    "evidence",
    "",
    "USAGE",
    "evidence_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_record",
    "subject_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "customer_iam_role",
    "schema",
    "catalog_order",
    "",
    "USAGE",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "subject_id",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "account_id",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "owner_organization_id",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_record",
    "id",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_record",
    "status",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_record",
    "source_type",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_record",
    "source_ref_id",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_record",
    "identity_at_capture",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_record",
    "consent_version",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_record",
    "created_at",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_record",
    "solidified_at",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_artifact",
    "evidence_id",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_artifact",
    "media_object_id",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.evidence_artifact",
    "artifact_role",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.media_object",
    "id",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.media_object",
    "sha256",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.media_object",
    "size_bytes",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.media_object",
    "mime_type",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.recording",
    "id",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.recording",
    "media_object_id",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.recording",
    "status",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "evidence.recording",
    "validated_duration",
    "SELECT",
    "evidence_owner",
    false
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_record",
    "session_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "evidence_role",
    "column",
    "evidence.evidence_record",
    "call_id",
    "SELECT",
    "evidence_owner"
  ],
  [
    "operations_iam_role",
    "schema",
    "platform",
    "",
    "USAGE",
    "platform_reliability_owner"
  ],
  [
    "operations_iam_role",
    "routine",
    "platform.idempotency_begin(text,text,text)",
    "",
    "EXECUTE",
    "platform_reliability_owner"
  ],
  [
    "operations_iam_role",
    "routine",
    "platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)",
    "",
    "EXECUTE",
    "platform_reliability_owner"
  ],
  [
    "operations_iam_role",
    "routine",
    "platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)",
    "",
    "EXECUTE",
    "platform_reliability_owner"
  ],
  [
    "operations_iam_role",
    "schema",
    "audit",
    "",
    "USAGE",
    "audit_owner"
  ],
  [
    "operations_iam_role",
    "routine",
    "audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)",
    "",
    "EXECUTE",
    "audit_owner"
  ],
  [
    "operations_iam_role",
    "schema",
    "operations_iam",
    "",
    "USAGE",
    "operations_iam_owner"
  ],
  [
    "operations_iam_role",
    "table",
    "operations_iam.employee_account",
    "",
    "SELECT",
    "operations_iam_owner"
  ],
  [
    "operations_iam_role",
    "table",
    "operations_iam.employee_account",
    "",
    "INSERT",
    "operations_iam_owner"
  ],
  [
    "operations_iam_role",
    "column",
    "operations_iam.employee_account",
    "display_name",
    "UPDATE",
    "operations_iam_owner"
  ],
  [
    "operations_iam_role",
    "column",
    "operations_iam.employee_account",
    "role",
    "UPDATE",
    "operations_iam_owner"
  ],
  [
    "operations_iam_role",
    "column",
    "operations_iam.employee_account",
    "status",
    "UPDATE",
    "operations_iam_owner"
  ],
  [
    "operations_iam_role",
    "column",
    "operations_iam.employee_account",
    "revision",
    "UPDATE",
    "operations_iam_owner"
  ],
  [
    "operations_iam_role",
    "routine",
    "operations_iam.guard_employee_account()",
    "",
    "EXECUTE",
    "operations_iam_owner"
  ],
  [
    "ivr_role",
    "schema",
    "platform",
    "",
    "USAGE",
    "platform_reliability_owner"
  ],
  [
    "ivr_role",
    "routine",
    "platform.idempotency_begin(text,text,text)",
    "",
    "EXECUTE",
    "platform_reliability_owner"
  ],
  [
    "ivr_role",
    "routine",
    "platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)",
    "",
    "EXECUTE",
    "platform_reliability_owner"
  ],
  [
    "ivr_role",
    "routine",
    "platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)",
    "",
    "EXECUTE",
    "platform_reliability_owner"
  ],
  [
    "ivr_role",
    "schema",
    "audit",
    "",
    "USAGE",
    "audit_owner"
  ],
  [
    "ivr_role",
    "routine",
    "audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)",
    "",
    "EXECUTE",
    "audit_owner"
  ],
  [
    "ivr_role",
    "schema",
    "activation_consent",
    "",
    "USAGE",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "schema",
    "entitlement_metering",
    "",
    "USAGE",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "schema",
    "ivr",
    "",
    "USAGE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.outbound_call_task",
    "",
    "SELECT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.outbound_call_task",
    "",
    "INSERT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.outbound_call_task",
    "status",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.outbound_call_task",
    "reason_code",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.outbound_call_task",
    "claimed_call_id",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.outbound_call_task",
    "claimed_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.outbound_call_task",
    "terminal_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.outbound_call_task",
    "updated_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.outbound_call_task",
    "revision",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.inbound_session",
    "",
    "SELECT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.inbound_session",
    "",
    "INSERT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.inbound_session",
    "status",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.inbound_session",
    "confirmed_subject_id",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.inbound_session",
    "guidance_status",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.inbound_session",
    "confirmed_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.inbound_session",
    "revision",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.call_record",
    "",
    "SELECT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.call_record",
    "",
    "INSERT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_record",
    "task_id",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_record",
    "status",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_record",
    "started_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_record",
    "bridged_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_record",
    "recording_started_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_record",
    "recording_ended_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_record",
    "ended_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_record",
    "authoritative_seconds",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_record",
    "end_reason",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_record",
    "revision",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.call_leg",
    "",
    "SELECT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.call_leg",
    "",
    "INSERT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_leg",
    "state",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_leg",
    "answered_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_leg",
    "ended_at",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_leg",
    "end_reason",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_leg",
    "revision",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.call_event",
    "",
    "SELECT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.call_event",
    "",
    "INSERT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_event",
    "processing_status",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_event",
    "reason_code",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "column",
    "ivr.call_event",
    "revision",
    "UPDATE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.ivr_decision",
    "",
    "SELECT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "table",
    "ivr.ivr_decision",
    "",
    "INSERT",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "routine",
    "ivr.guard_outbound_call_task()",
    "",
    "EXECUTE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "routine",
    "ivr.guard_inbound_session()",
    "",
    "EXECUTE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "routine",
    "ivr.guard_call_record_parties()",
    "",
    "EXECUTE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "routine",
    "ivr.guard_call_leg()",
    "",
    "EXECUTE",
    "ivr_owner"
  ],
  [
    "ivr_role",
    "schema",
    "customer_iam",
    "",
    "USAGE",
    "customer_iam_owner"
  ],
  [
    "ivr_role",
    "column",
    "customer_iam.customer_account",
    "id",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "ivr_role",
    "column",
    "customer_iam.customer_account",
    "status",
    "SELECT",
    "customer_iam_owner"
  ],
  [
    "ivr_role",
    "schema",
    "organization_access",
    "",
    "USAGE",
    "organization_access_owner"
  ],
  [
    "ivr_role",
    "column",
    "organization_access.organization",
    "id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "ivr_role",
    "column",
    "organization_access.organization",
    "status",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "ivr_role",
    "column",
    "organization_access.organization_membership",
    "organization_id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "ivr_role",
    "column",
    "organization_access.organization_membership",
    "account_id",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "ivr_role",
    "column",
    "organization_access.organization_membership",
    "role",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "ivr_role",
    "column",
    "organization_access.organization_membership",
    "status",
    "SELECT",
    "organization_access_owner"
  ],
  [
    "ivr_role",
    "schema",
    "subject_number",
    "",
    "USAGE",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.business_subject",
    "id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.business_subject",
    "owner_account_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.business_subject",
    "owner_organization_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.business_subject",
    "revision",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.caller_number_binding",
    "id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.caller_number_binding",
    "subject_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.caller_number_binding",
    "number_hmac",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.caller_number_binding",
    "status",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.caller_number_binding",
    "verified_at",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.caller_number_binding",
    "revision",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.subject_risk_state",
    "caller_number_binding_id",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.subject_risk_state",
    "risk_type",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.subject_risk_state",
    "state",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "subject_number.subject_risk_state",
    "revision",
    "SELECT",
    "subject_number_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_balance",
    "id",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_balance",
    "grant_id",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_balance",
    "granted_units",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_balance",
    "reserved_units",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_balance",
    "consumed_units",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_balance",
    "status",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_grant",
    "id",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_grant",
    "subject_id",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_grant",
    "entitlement_type",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_grant",
    "granted_units",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_grant",
    "valid_from",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "entitlement_metering.entitlement_grant",
    "expires_at",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.service_activation",
    "id",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.service_activation",
    "subject_id",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.service_activation",
    "status",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.service_activation",
    "revision",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.consent_record",
    "subject_id",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.consent_record",
    "purpose",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.consent_record",
    "channel",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.consent_record",
    "policy_code",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.consent_record",
    "policy_version",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.consent_record",
    "prompt_version",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.consent_record",
    "action",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.consent_record",
    "event_domain",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.consent_record",
    "event_id",
    "SELECT",
    "activation_consent_owner"
  ],
  [
    "ivr_role",
    "column",
    "activation_consent.consent_record",
    "accepted_at",
    "SELECT",
    "activation_consent_owner"
  ]
]$frozen$::jsonb;
    expected_owners jsonb := $owners${
  "platform": "platform_reliability_owner",
  "audit": "audit_owner",
  "customer_iam": "customer_iam_owner",
  "organization_access": "organization_access_owner",
  "operations_iam": "operations_iam_owner",
  "subject_number": "subject_number_owner",
  "catalog_order": "catalog_order_owner",
  "ivr": "ivr_owner",
  "entitlement_metering": "entitlement_metering_owner",
  "activation_consent": "activation_consent_owner",
  "evidence": "evidence_owner",
  "sharing": "sharing_owner"
}$owners$::jsonb;
    expected_history jsonb := $history$[
  [
    "001",
    "V001__platform_schemas_roles_idempotency_outbox_audit.sql",
    -709123726,
    "platform schemas roles idempotency outbox audit"
  ],
  [
    "002",
    "V002__identity_organization_and_employee.sql",
    -1229975166,
    "identity organization and employee"
  ],
  [
    "003",
    "V003__subject_number_and_claim.sql",
    -1355776370,
    "subject number and claim"
  ],
  [
    "004",
    "V004__catalog_order_and_payment.sql",
    -665258487,
    "catalog order and payment"
  ],
  [
    "005",
    "V005__ivr_task_session_call.sql",
    -1005314773,
    "ivr task session call"
  ],
  [
    "006",
    "V006__trial_entitlement_issuance_reservation_usage.sql",
    366834714,
    "trial entitlement issuance reservation usage"
  ],
  [
    "007",
    "V007__activation_and_consent.sql",
    -1281725572,
    "activation and consent"
  ],
  [
    "008",
    "V008__evidence_media.sql",
    -245169007,
    "evidence media"
  ],
  [
    "009",
    "V009__sharing.sql",
    -59961153,
    "sharing"
  ],
  [
    "010",
    "V010__sharing_role_least_privilege.sql",
    247431460,
    "sharing role least privilege"
  ],
  [
    "011",
    "V011__sharing_public_read_least_privilege.sql",
    492686454,
    "sharing public read least privilege"
  ],
  [
    "012",
    "V012__sharing_admission_least_privilege.sql",
    -291628548,
    "sharing admission least privilege"
  ],
  [
    "013",
    "V013__evidence_media_reader_role.sql",
    -611381791,
    "evidence media reader role"
  ],
  [
    "014",
    "V014__database_runtime_acl_rbac_hardening.sql",
    -340359494,
    "database runtime acl rbac hardening"
  ],
  [
    "015",
    "V015__customer_business_number_read_privileges.sql",
    -1316980027,
    "customer business number read privileges"
  ],
  [
    "016",
    "V016__customer_subject_detail_read_privileges.sql",
    1669606497,
    "customer subject detail read privileges"
  ],
  [
    "017",
    "V017__customer_evidence_read_privileges.sql",
    -41822162,
    "customer evidence read privileges"
  ],
  [
    "018",
    "V018__evidence_role_ivr_association_read_privileges.sql",
    1606293398,
    "evidence role ivr association read privileges"
  ]
]$history$::jsonb;
    expected_routines jsonb := $routines${
  "platform.idempotency_begin": {
    "security": true,
    "config": [
      "search_path=platform, pg_temp"
    ]
  },
  "platform.idempotency_complete": {
    "security": true,
    "config": [
      "search_path=platform, pg_temp"
    ]
  },
  "platform.outbox_append": {
    "security": true,
    "config": [
      "search_path=platform, pg_temp"
    ]
  },
  "audit.reject_row_mutation": {
    "security": false,
    "config": [
      "search_path=audit, pg_temp"
    ]
  },
  "audit.append_fact": {
    "security": true,
    "config": [
      "search_path=audit, pg_temp"
    ]
  },
  "customer_iam.guard_customer_account": {
    "security": false,
    "config": [
      "search_path=customer_iam, pg_temp"
    ]
  },
  "customer_iam.guard_auth_identity": {
    "security": false,
    "config": [
      "search_path=customer_iam, pg_temp"
    ]
  },
  "customer_iam.guard_identity_profile": {
    "security": false,
    "config": [
      "search_path=customer_iam, pg_temp"
    ]
  },
  "organization_access.guard_organization": {
    "security": false,
    "config": [
      "search_path=organization_access, pg_temp"
    ]
  },
  "organization_access.guard_membership": {
    "security": false,
    "config": [
      "search_path=organization_access, pg_temp"
    ]
  },
  "operations_iam.guard_employee_account": {
    "security": false,
    "config": [
      "search_path=operations_iam, pg_temp"
    ]
  },
  "subject_number.reject_row_mutation": {
    "security": false,
    "config": [
      "search_path=subject_number, pg_temp"
    ]
  },
  "subject_number.guard_subject_owner": {
    "security": false,
    "config": [
      "search_path=subject_number, pg_temp"
    ]
  },
  "subject_number.guard_caller_number_binding": {
    "security": false,
    "config": [
      "search_path=subject_number, pg_temp"
    ]
  },
  "subject_number.guard_subject_risk_state": {
    "security": false,
    "config": [
      "search_path=subject_number, pg_temp"
    ]
  },
  "catalog_order.reject_row_mutation": {
    "security": false,
    "config": [
      "search_path=catalog_order, pg_temp"
    ]
  },
  "catalog_order.guard_catalog_version_published": {
    "security": false,
    "config": [
      "search_path=catalog_order, pg_temp"
    ]
  },
  "catalog_order.guard_catalog_offer": {
    "security": false,
    "config": [
      "search_path=catalog_order, pg_temp"
    ]
  },
  "catalog_order.guard_customer_order_snapshot": {
    "security": false,
    "config": [
      "search_path=catalog_order, pg_temp"
    ]
  },
  "catalog_order.guard_payment_order_identity": {
    "security": false,
    "config": [
      "search_path=catalog_order, pg_temp"
    ]
  },
  "ivr.reject_row_mutation": {
    "security": false,
    "config": [
      "search_path=ivr, pg_temp"
    ]
  },
  "ivr.guard_outbound_call_task": {
    "security": false,
    "config": [
      "search_path=ivr, pg_temp"
    ]
  },
  "ivr.guard_inbound_session": {
    "security": false,
    "config": [
      "search_path=ivr, pg_temp"
    ]
  },
  "ivr.guard_call_record_parties": {
    "security": false,
    "config": [
      "search_path=ivr, pg_temp"
    ]
  },
  "ivr.guard_call_leg": {
    "security": false,
    "config": [
      "search_path=ivr, pg_temp"
    ]
  },
  "entitlement_metering.issue_first_trial": {
    "security": true,
    "config": [
      "search_path=entitlement_metering, platform, pg_temp"
    ]
  },
  "entitlement_metering.guard_trial_policy_version": {
    "security": false,
    "config": [
      "search_path=entitlement_metering, pg_temp"
    ]
  },
  "entitlement_metering.guard_entitlement_grant": {
    "security": false,
    "config": [
      "search_path=entitlement_metering, pg_temp"
    ]
  },
  "entitlement_metering.guard_trial_grant": {
    "security": false,
    "config": [
      "search_path=entitlement_metering, pg_temp"
    ]
  },
  "entitlement_metering.guard_entitlement_balance": {
    "security": false,
    "config": [
      "search_path=entitlement_metering, pg_temp"
    ]
  },
  "entitlement_metering.guard_entitlement_issuance": {
    "security": false,
    "config": [
      "search_path=entitlement_metering, pg_temp"
    ]
  },
  "entitlement_metering.guard_usage_reservation": {
    "security": false,
    "config": [
      "search_path=entitlement_metering, pg_temp"
    ]
  },
  "entitlement_metering.reject_row_mutation": {
    "security": false,
    "config": [
      "search_path=entitlement_metering, pg_temp"
    ]
  },
  "activation_consent.reject_row_mutation": {
    "security": false,
    "config": [
      "search_path=activation_consent, pg_temp"
    ]
  },
  "activation_consent.guard_service_activation": {
    "security": false,
    "config": [
      "search_path=activation_consent, pg_temp"
    ]
  },
  "evidence.reject_row_mutation": {
    "security": false,
    "config": [
      "search_path=evidence, pg_temp"
    ]
  },
  "evidence.guard_recording": {
    "security": false,
    "config": [
      "search_path=evidence, pg_temp"
    ]
  },
  "evidence.guard_evidence_record": {
    "security": false,
    "config": [
      "search_path=evidence, pg_temp"
    ]
  },
  "evidence.guard_evidence_record_creation_cycle": {
    "security": false,
    "config": [
      "search_path=evidence, pg_temp"
    ]
  },
  "evidence.guard_evidence_event_creation_cycle": {
    "security": false,
    "config": [
      "search_path=evidence, pg_temp"
    ]
  },
  "sharing.guard_share_grant": {
    "security": false,
    "config": [
      "search_path=sharing, pg_temp"
    ]
  },
  "sharing.guard_share_admission": {
    "security": false,
    "config": [
      "search_path=sharing, pg_temp"
    ]
  },
  "sharing.reject_row_mutation": {
    "security": false,
    "config": [
      "search_path=sharing, pg_temp"
    ]
  }
}$routines$::jsonb;
    database_oid oid;
    managed_oids oid[];
    external_oids oid[];
    role_row record;
    object_row record;
    fact record;
    role_name text;
    expected_marker text;
    actual_marker text;
    slot_index integer;
    pass integer;
    edge_count integer;
    expected_login boolean;
    expected_inherit boolean;
    expected_object oid;
    expected_subid integer;
    actual_acls jsonb;
    expected_resolved jsonb;
    actual_value boolean;
    privilege_name text;
BEGIN
    IF requested_mode NOT IN ('apply', 'check') THEN
        RAISE EXCEPTION 'rejected';
    END IF;
    IF (SELECT count(DISTINCT name COLLATE "C") FROM unnest(external_names) name) <> 4 THEN
        RAISE EXCEPTION 'rejected';
    END IF;
    FOREACH role_name IN ARRAY external_names LOOP
        IF role_name COLLATE "C" !~ '^[a-z_][a-z0-9_]{0,62}$'
           OR octet_length(role_name) NOT BETWEEN 1 AND 63
           OR role_name COLLATE "C" ~ '[^a-z0-9_]'
           OR role_name LIKE 'pg\_%' ESCAPE '\'
           OR role_name = ANY(effective_names)
           OR role_name = ANY(shared_context_targets)
           OR role_name = ANY(ARRAY(SELECT key FROM jsonb_each_text(expected_owners)))
           OR role_name = ANY(ARRAY(SELECT value FROM jsonb_each_text(expected_owners)))
           OR role_name IN (current_user, session_user, 'public', 'none')
           OR role_name IN (SELECT rolname FROM pg_roles WHERE NOT rolcanlogin AND rolname <> ALL(effective_names)) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
    END LOOP;
    failure_category := 'PA25_AUTHORITY_REJECTED';
    IF current_setting('server_version_num')::integer NOT BETWEEN 160000 AND 169999
       OR current_user <> session_user
       OR NOT (SELECT rolsuper FROM pg_roles WHERE rolname = current_user) THEN
        RAISE EXCEPTION 'rejected';
    END IF;
    SELECT oid INTO STRICT database_oid FROM pg_database WHERE datname = current_database();
    PERFORM pg_advisory_xact_lock(725025, 1);
    PERFORM set_config('search_path', 'pg_catalog', true);
    managed_names := external_names || effective_names;
    login_names := external_names || ARRAY['sharing_public_read_role', 'sharing_admission_role', 'evidence_media_reader_role'];
    boundary_names := managed_names || shared_context_targets;
    FOR pass IN 1..2 LOOP
        SELECT array_agg(oid) INTO managed_oids FROM pg_roles WHERE rolname = ANY(managed_names);
        SELECT array_agg(oid) INTO boundary_oids FROM pg_roles WHERE rolname = ANY(boundary_names);
        SELECT array_agg(oid) INTO external_oids FROM pg_roles WHERE rolname = ANY(external_names);
        failure_category := 'PA25_PROVENANCE_REJECTED';
        FOR slot_index IN 1..4 LOOP
            SELECT oid, rolname INTO role_row FROM pg_roles WHERE rolname = external_names[slot_index];
            IF FOUND THEN
                expected_marker := format('onecall:pa25:v1:db=%s:role=%s:slot=%s:name=%s',
                    database_oid, role_row.oid, slots[slot_index], external_names[slot_index]);
                actual_marker := shobj_description(role_row.oid, 'pg_authid');
                IF convert_to(actual_marker, 'UTF8') IS DISTINCT FROM convert_to(expected_marker, 'UTF8') THEN
                    RAISE EXCEPTION 'rejected';
                END IF;
            END IF;
        END LOOP;
        failure_category := 'PA25_HISTORY_REJECTED';
        IF to_regclass('public.flyway_schema_history') IS NULL THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        IF EXISTS (
            WITH expected AS (
                SELECT item->>0 AS version, item->>1 AS script, (item->>2)::integer AS checksum,
                    ordinality::integer AS installed_rank, item->>3 AS description
                FROM jsonb_array_elements(expected_history) WITH ORDINALITY entries(item, ordinality)
            ), actual AS (
                SELECT version, script, checksum, installed_rank, description FROM public.flyway_schema_history
                WHERE type = 'SQL' AND success
            )
            SELECT 1 FROM ((SELECT * FROM expected EXCEPT SELECT * FROM actual)
                UNION ALL (SELECT * FROM actual EXCEPT SELECT * FROM expected)) difference
        ) OR (SELECT count(*) FROM public.flyway_schema_history) <> 18 THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'PA25_SETTINGS_REJECTED';
        IF EXISTS (SELECT 1 FROM pg_db_role_setting WHERE setrole = ANY(boundary_oids)
            OR (setrole = 0 AND setdatabase IN (0, database_oid))) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'PA25_ATTRIBUTES_REJECTED';
        FOREACH role_name IN ARRAY boundary_names LOOP
            SELECT auth.rolcanlogin, auth.rolinherit, auth.rolsuper, auth.rolcreatedb, auth.rolcreaterole,
                auth.rolreplication, auth.rolbypassrls, auth.rolconnlimit, auth.rolvaliduntil,
                auth.rolpassword IS NULL AS password_is_null, roles.rolconfig INTO role_row FROM pg_authid auth
                JOIN pg_roles roles ON roles.oid = auth.oid WHERE auth.rolname = role_name;
            IF NOT FOUND THEN
                IF role_name = ANY(effective_names) OR role_name = ANY(shared_context_targets) OR requested_mode = 'check' OR pass = 2 THEN
                    RAISE EXCEPTION 'rejected';
                END IF;
                CONTINUE;
            END IF;
            expected_login := role_name = ANY(login_names);
            expected_inherit := role_name IN ('sharing_role', 'customer_iam_role', 'evidence_role',
                'operations_iam_role', 'ivr_role') OR role_name = ANY(shared_context_targets);
            IF role_row.rolcanlogin IS DISTINCT FROM expected_login
               OR role_row.rolinherit IS DISTINCT FROM expected_inherit
               OR role_row.rolsuper OR role_row.rolcreatedb OR role_row.rolcreaterole
               OR role_row.rolreplication OR role_row.rolbypassrls
               OR role_row.rolconnlimit <> -1 OR role_row.rolconfig IS NOT NULL
               OR (NOT expected_login AND NOT role_row.password_is_null)
               OR (role_name = 'evidence_media_reader_role' AND role_row.rolvaliduntil IS DISTINCT FROM 'infinity'::timestamptz)
               OR (role_name <> 'evidence_media_reader_role' AND role_row.rolvaliduntil IS NOT NULL) THEN
                RAISE EXCEPTION 'rejected';
            END IF;
        END LOOP;
        failure_category := 'PA25_TOPOLOGY_REJECTED';
        IF EXISTS (
            SELECT 1 FROM pg_auth_members membership
            JOIN pg_roles target ON target.oid = membership.roleid
            JOIN pg_roles member_role ON member_role.oid = membership.member
            JOIN pg_roles grantor ON grantor.oid = membership.grantor
            WHERE (membership.roleid = ANY(boundary_oids) OR membership.member = ANY(boundary_oids)
                OR membership.grantor = ANY(boundary_oids))
              AND (NOT ((target.rolname = 'sharing_role' AND member_role.rolname = external_names[1])
                  OR (target.rolname = 'customer_iam_role' AND member_role.rolname = external_names[2])
                  OR (target.rolname IN ('sharing_command_role', 'evidence_role') AND member_role.rolname = external_names[3])
                  OR (target.rolname = ANY(ARRAY['operations_iam_role', 'subject_number_role', 'activation_consent_role', 'entitlement_metering_role', 'evidence_role', 'catalog_order_role', 'ivr_role']) AND member_role.rolname = external_names[4]))
                  OR membership.admin_option OR membership.inherit_option OR NOT membership.set_option
                  OR NOT grantor.rolsuper OR grantor.oid = ANY(boundary_oids))
        ) OR EXISTS (
            SELECT 1 FROM pg_auth_members WHERE roleid = ANY(boundary_oids) OR member = ANY(boundary_oids)
            GROUP BY roleid, member HAVING count(*) <> 1
        ) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        SELECT count(*) INTO edge_count FROM pg_auth_members
            WHERE roleid = ANY(boundary_oids) OR member = ANY(boundary_oids);
        IF (requested_mode = 'check' OR pass = 2) AND edge_count <> 11 THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'PA25_OWNERSHIP_REJECTED';
        FOR object_row IN SELECT key AS schema_name, value AS role_name FROM jsonb_each_text(expected_owners) LOOP
            SELECT auth.oid, auth.rolcanlogin, auth.rolinherit, auth.rolsuper, auth.rolcreatedb, auth.rolcreaterole,
                auth.rolreplication, auth.rolbypassrls, auth.rolconnlimit, auth.rolvaliduntil,
                auth.rolpassword IS NULL AS password_is_null, roles.rolconfig INTO role_row FROM pg_authid auth
                JOIN pg_roles roles ON roles.oid = auth.oid WHERE auth.rolname = object_row.role_name;
            IF NOT FOUND OR role_row.rolcanlogin OR role_row.rolsuper OR role_row.rolcreatedb
                OR role_row.rolcreaterole OR role_row.rolreplication OR role_row.rolbypassrls
                OR NOT role_row.rolinherit OR NOT role_row.password_is_null
                OR role_row.rolconfig IS NOT NULL OR role_row.rolconnlimit <> -1 OR role_row.rolvaliduntil IS NOT NULL
                OR NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = object_row.schema_name AND nspowner = role_row.oid)
                OR EXISTS (SELECT 1 FROM pg_auth_members WHERE roleid = role_row.oid OR member = role_row.oid)
                OR EXISTS (SELECT 1 FROM pg_db_role_setting WHERE setrole = role_row.oid) THEN
                RAISE EXCEPTION 'rejected';
            END IF;
        END LOOP;
        IF EXISTS (SELECT 1 FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
                WHERE expected_owners ? namespace.nspname AND relation.relowner <> namespace.nspowner)
            OR EXISTS (SELECT 1 FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid = routine.pronamespace
                WHERE expected_owners ? namespace.nspname AND (routine.proowner <> namespace.nspowner
                    OR NOT (expected_routines ? (namespace.nspname || '.' || routine.proname))
                    OR routine.prosecdef IS DISTINCT FROM (expected_routines->(namespace.nspname || '.' || routine.proname)->>'security')::boolean
                    OR to_jsonb(routine.proconfig) IS DISTINCT FROM expected_routines->(namespace.nspname || '.' || routine.proname)->'config'))
            OR EXISTS (SELECT 1 FROM pg_type object_type JOIN pg_namespace namespace ON namespace.oid = object_type.typnamespace
                WHERE expected_owners ? namespace.nspname AND object_type.typowner <> namespace.nspowner) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'PA25_ACL_REJECTED';
        SELECT jsonb_agg(jsonb_build_array(item->>0, item->>1,
            CASE item->>1 WHEN 'schema' THEN to_regnamespace(item->>2)::oid
                WHEN 'routine' THEN to_regprocedure(item->>2)::oid
                ELSE to_regclass(item->>2)::oid END,
            CASE WHEN item->>1 = 'column' THEN (SELECT attnum FROM pg_attribute
                WHERE attrelid = to_regclass(item->>2) AND attname = item->>3 AND NOT attisdropped) ELSE 0 END,
            item->>4, to_regrole(item->>5)::oid, false)) INTO expected_resolved
        FROM jsonb_array_elements(expected_acls) item;
        IF jsonb_array_length(expected_resolved) <> 410
            OR (SELECT count(DISTINCT value) FROM jsonb_array_elements(expected_resolved)) <> 410 OR EXISTS (
            SELECT 1 FROM jsonb_array_elements(expected_resolved) item WHERE item->>2 IS NULL OR item->>3 IS NULL OR item->>5 IS NULL
        ) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        WITH objects AS (
            SELECT 'schema' AS kind, namespace.oid, 0 AS subid, namespace.nspowner AS owner,
                coalesce(namespace.nspacl, acldefault('n', namespace.nspowner)) AS acl
            FROM pg_namespace namespace WHERE namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
            UNION ALL
            SELECT CASE WHEN relation.relkind = 'S' THEN 'sequence' ELSE 'table' END, relation.oid, 0, relation.relowner,
                coalesce(relation.relacl, acldefault(CASE WHEN relation.relkind = 'S' THEN 's'::"char" ELSE 'r'::"char" END, relation.relowner))
            FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
            WHERE namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
            UNION ALL
            SELECT 'column', relation.oid, attribute.attnum, relation.relowner, attribute.attacl
            FROM pg_attribute attribute JOIN pg_class relation ON relation.oid = attribute.attrelid
            JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
            WHERE attribute.attnum > 0 AND NOT attribute.attisdropped AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
            UNION ALL
            SELECT 'routine', routine.oid, 0, routine.proowner, coalesce(routine.proacl, acldefault('f', routine.proowner))
            FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid = routine.pronamespace
            WHERE namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
        ), expanded AS (
            SELECT objects.*, acl.grantee, acl.grantor, acl.privilege_type, acl.is_grantable
            FROM objects CROSS JOIN LATERAL aclexplode(objects.acl) acl
        )
        SELECT jsonb_agg(jsonb_build_array(coalesce(grantee::regrole::text, 'public'), kind, oid, subid,
            privilege_type, grantor, is_grantable)) INTO actual_acls
        FROM expanded WHERE grantee = ANY(managed_oids) OR grantee = 0 OR grantor = ANY(boundary_oids);
        IF jsonb_array_length(coalesce(actual_acls, '[]'::jsonb)) <> 410 OR EXISTS (
            SELECT 1 FROM ((SELECT value FROM jsonb_array_elements(expected_resolved)
                    EXCEPT SELECT value FROM jsonb_array_elements(coalesce(actual_acls, '[]'::jsonb)))
                UNION ALL (SELECT value FROM jsonb_array_elements(coalesce(actual_acls, '[]'::jsonb))
                    EXCEPT SELECT value FROM jsonb_array_elements(expected_resolved))) difference
        ) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        FOR fact IN SELECT value AS item FROM jsonb_array_elements(expected_resolved) LOOP
            role_name := fact.item->>0;
            expected_object := (fact.item->>2)::oid;
            expected_subid := (fact.item->>3)::integer;
            privilege_name := fact.item->>4;
            actual_value := CASE fact.item->>1
                WHEN 'schema' THEN has_schema_privilege(role_name, expected_object, privilege_name)
                WHEN 'routine' THEN has_function_privilege(role_name, expected_object, privilege_name)
                WHEN 'column' THEN has_column_privilege(role_name, expected_object, expected_subid::smallint, privilege_name)
                ELSE has_table_privilege(role_name, expected_object, privilege_name) END;
            IF actual_value IS DISTINCT FROM true THEN RAISE EXCEPTION 'rejected'; END IF;
        END LOOP;
        IF EXISTS (
            WITH expected AS MATERIALIZED (
                SELECT item->>0 AS grantee_name, item->>1 AS kind, (item->>2)::oid AS object_oid,
                    (item->>3)::integer AS subid, item->>4 AS privilege
                FROM jsonb_array_elements(expected_resolved) item
            )
            SELECT 1 FROM pg_roles runtime CROSS JOIN pg_namespace namespace
                CROSS JOIN (VALUES ('USAGE'), ('CREATE')) privileges(privilege)
            WHERE runtime.oid = ANY(managed_oids) AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
                AND has_schema_privilege(runtime.oid, namespace.oid, privileges.privilege) IS DISTINCT FROM EXISTS (
                    SELECT 1 FROM expected WHERE grantee_name = runtime.rolname AND kind = 'schema'
                        AND object_oid = namespace.oid AND privilege = privileges.privilege)
        ) OR EXISTS (
            WITH expected AS MATERIALIZED (
                SELECT item->>0 AS grantee_name, item->>1 AS kind, (item->>2)::oid AS object_oid,
                    (item->>3)::integer AS subid, item->>4 AS privilege
                FROM jsonb_array_elements(expected_resolved) item
            )
            SELECT 1 FROM pg_roles runtime CROSS JOIN pg_class relation
                JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
                CROSS JOIN (VALUES ('SELECT'), ('INSERT'), ('UPDATE'), ('DELETE'), ('TRUNCATE'), ('REFERENCES'), ('TRIGGER')) privileges(privilege)
            WHERE runtime.oid = ANY(managed_oids) AND relation.relkind IN ('r','p','v','m','f')
                AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
                AND has_table_privilege(runtime.oid, relation.oid, privileges.privilege) IS DISTINCT FROM EXISTS (
                    SELECT 1 FROM expected WHERE grantee_name = runtime.rolname AND kind = 'table'
                        AND object_oid = relation.oid AND privilege = privileges.privilege)
        ) OR EXISTS (
            WITH expected AS MATERIALIZED (
                SELECT item->>0 AS grantee_name, item->>1 AS kind, (item->>2)::oid AS object_oid,
                    (item->>3)::integer AS subid, item->>4 AS privilege
                FROM jsonb_array_elements(expected_resolved) item
            )
            SELECT 1 FROM pg_roles runtime CROSS JOIN pg_attribute attribute
                JOIN pg_class relation ON relation.oid = attribute.attrelid
                JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
                CROSS JOIN (VALUES ('SELECT'), ('INSERT'), ('UPDATE'), ('REFERENCES')) privileges(privilege)
            WHERE runtime.oid = ANY(managed_oids) AND relation.relkind IN ('r','p','v','m','f')
                AND attribute.attnum > 0 AND NOT attribute.attisdropped
                AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
                AND (has_column_privilege(runtime.oid, relation.oid, attribute.attnum, privileges.privilege) IS DISTINCT FROM EXISTS (
                    SELECT 1 FROM expected WHERE grantee_name = runtime.rolname AND kind IN ('table','column')
                        AND object_oid = relation.oid AND (kind = 'table' OR subid = attribute.attnum) AND privilege = privileges.privilege)
                    OR has_column_privilege(runtime.oid, relation.oid, attribute.attnum, privileges.privilege || ' WITH GRANT OPTION'))
        ) OR EXISTS (
            WITH expected AS MATERIALIZED (
                SELECT item->>0 AS grantee_name, item->>1 AS kind, (item->>2)::oid AS object_oid
                FROM jsonb_array_elements(expected_resolved) item
            )
            SELECT 1 FROM pg_roles runtime CROSS JOIN pg_proc routine
                JOIN pg_namespace namespace ON namespace.oid = routine.pronamespace
            WHERE runtime.oid = ANY(managed_oids) AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
                AND has_function_privilege(runtime.oid, routine.oid, 'EXECUTE') IS DISTINCT FROM EXISTS (
                    SELECT 1 FROM expected WHERE grantee_name = runtime.rolname AND kind = 'routine' AND object_oid = routine.oid)
        ) OR EXISTS (
            SELECT 1 FROM pg_roles runtime CROSS JOIN pg_class relation
                JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
            WHERE runtime.oid = ANY(managed_oids) AND relation.relkind = 'S'
                AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
                AND has_sequence_privilege(runtime.oid, relation.oid, 'USAGE,SELECT,UPDATE')
        ) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'PA25_DATABASE_REJECTED';
        IF EXISTS (SELECT 1 FROM pg_database database_row CROSS JOIN LATERAL aclexplode(
                coalesce(database_row.datacl, acldefault('d', database_row.datdba))) acl
            WHERE acl.grantee = ANY(boundary_oids) OR acl.grantor = ANY(boundary_oids)
                OR (database_row.oid = database_oid AND acl.grantee = 0
                    AND (acl.privilege_type <> 'CONNECT' OR acl.is_grantable)))
           OR NOT EXISTS (SELECT 1 FROM pg_database database_row CROSS JOIN LATERAL aclexplode(
                coalesce(database_row.datacl, acldefault('d', database_row.datdba))) acl
                WHERE database_row.oid = database_oid AND acl.grantee = 0 AND acl.privilege_type = 'CONNECT') THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        FOREACH role_name IN ARRAY login_names LOOP
            IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
                IF NOT has_database_privilege(role_name, database_oid, 'CONNECT')
                   OR has_database_privilege(role_name, database_oid, 'CREATE,TEMPORARY') THEN
                    RAISE EXCEPTION 'rejected';
                END IF;
            END IF;
        END LOOP;
        failure_category := 'PA25_DEFAULT_ACL_REJECTED';
        IF EXISTS (SELECT 1 FROM pg_default_acl defaults
            WHERE defaults.defaclrole = ANY(boundary_oids))
            OR EXISTS (SELECT 1 FROM pg_default_acl defaults
                CROSS JOIN LATERAL aclexplode(defaults.defaclacl) acl
            WHERE acl.grantee = 0 OR acl.grantee = ANY(boundary_oids)
                OR acl.grantor = ANY(boundary_oids)) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'PA25_DEPENDENCY_REJECTED';
        IF EXISTS (
            SELECT 1 FROM pg_shdepend dependency
            WHERE dependency.refclassid = 'pg_authid'::regclass AND dependency.refobjid = ANY(boundary_oids)
                AND NOT (dependency.dbid = database_oid AND dependency.deptype = 'a'
                    AND dependency.refobjid <> ALL(coalesce(external_oids, ARRAY[]::oid[]))
                    AND (dependency.refobjid = ANY(ARRAY(SELECT oid FROM pg_roles WHERE rolname = ANY(shared_context_targets)))
                    OR EXISTS (SELECT 1 FROM jsonb_array_elements(expected_resolved) item
                        WHERE to_regrole(item->>0)::oid = dependency.refobjid AND (item->>2)::oid = dependency.objid
                            AND (item->>3)::integer = dependency.objsubid AND dependency.classid = CASE item->>1
                                WHEN 'schema' THEN 'pg_namespace'::regclass
                                WHEN 'routine' THEN 'pg_proc'::regclass ELSE 'pg_class'::regclass END)))
        ) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        IF EXISTS (SELECT 1 FROM pg_depend dependency
            WHERE dependency.deptype = 'e' AND EXISTS (
                SELECT 1 FROM jsonb_array_elements(expected_resolved) item
                WHERE (item->>2)::oid = dependency.objid AND dependency.classid = CASE item->>1
                    WHEN 'schema' THEN 'pg_namespace'::regclass
                    WHEN 'routine' THEN 'pg_proc'::regclass ELSE 'pg_class'::regclass END)) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        IF pass = 1 AND requested_mode = 'apply' THEN
            failure_category := 'PA25_APPLY_REJECTED';
            FOR slot_index IN 1..4 LOOP
                role_name := external_names[slot_index];
                IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = role_name) THEN
                    EXECUTE format('CREATE ROLE %I LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS CONNECTION LIMIT -1 PASSWORD NULL', role_name);
                    SELECT oid INTO STRICT expected_object FROM pg_roles WHERE rolname = role_name;
                    expected_marker := format('onecall:pa25:v1:db=%s:role=%s:slot=%s:name=%s',
                        database_oid, expected_object, slots[slot_index], role_name);
                    EXECUTE format('COMMENT ON ROLE %I IS %L', role_name, expected_marker);
                ELSE
                    EXECUTE format('ALTER ROLE %I LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS CONNECTION LIMIT -1', role_name);
                END IF;
            END LOOP;
            FOR object_row IN SELECT * FROM (VALUES
                ('sharing_role', external_names[1]), ('customer_iam_role', external_names[2]),
                ('sharing_command_role', external_names[3]), ('evidence_role', external_names[3]),
                ('operations_iam_role', external_names[4]),
                ('subject_number_role', external_names[4]),
                ('activation_consent_role', external_names[4]),
                ('entitlement_metering_role', external_names[4]),
                ('evidence_role', external_names[4]),
                ('catalog_order_role', external_names[4]),
                ('ivr_role', external_names[4])) edges(target_name, member_name) LOOP
                IF NOT EXISTS (SELECT 1 FROM pg_auth_members WHERE roleid = to_regrole(object_row.target_name)
                    AND member = to_regrole(object_row.member_name)) THEN
                    EXECUTE format('GRANT %I TO %I WITH ADMIN FALSE, INHERIT FALSE, SET TRUE', object_row.target_name, object_row.member_name);
                END IF;
            END LOOP;
        END IF;
    END LOOP;
EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = failure_category;
END;
$pa25$;
SELECT current_setting('pa25.mode') = 'apply' AS is_apply \gset
COMMIT;
\o
\if :is_apply
\echo PA25_APPLY_OK logins=7 effective_roles=9 memberships=11
\else
\echo PA25_CHECK_OK logins=7 effective_roles=9 memberships=11
\endif
