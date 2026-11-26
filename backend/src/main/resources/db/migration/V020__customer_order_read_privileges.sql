DO $migration$
DECLARE
    failure_category text := 'V020_AUTHORITY_REJECTED';
    slots text[] := ARRAY['sharing_query', 'customer_context', 'sharing_command', 'operations_ivr_read'];
    external_names text[] := ARRAY[]::text[];
    effective_names text[] := ARRAY['sharing_role', 'customer_iam_role', 'sharing_command_role',
        'evidence_role', 'sharing_public_read_role', 'sharing_admission_role', 'evidence_media_reader_role', 'operations_iam_role', 'ivr_role'];
    target_names text[] := ARRAY['customer_iam_role'];
    shared_context_targets text[] := ARRAY['subject_number_role', 'activation_consent_role', 'entitlement_metering_role', 'catalog_order_role'];
    old_acls jsonb := $old$[
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
    "customer_iam_role",
    "column",
    "catalog_order.catalog_benefit_line",
    "catalog_version_id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_benefit_line",
    "entitlement_type",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_benefit_line",
    "grant_interval_months",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_benefit_line",
    "quantity",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_benefit_line",
    "validity_days",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_offer",
    "catalog_version_id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_offer",
    "channel",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_offer",
    "id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_offer",
    "offer_status",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_offer",
    "version_token",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_product",
    "id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_product",
    "product_code",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "audience",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "billing_cycle",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "currency",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "installments",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "offer_type",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "price_cents",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "product_id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "sale_status",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "version_no",
    "SELECT",
    "catalog_order_owner",
    false
  ]
]$old$::jsonb;
    final_acls jsonb := $final$[
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
    "customer_iam_role",
    "column",
    "catalog_order.catalog_benefit_line",
    "catalog_version_id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_benefit_line",
    "entitlement_type",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_benefit_line",
    "grant_interval_months",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_benefit_line",
    "quantity",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_benefit_line",
    "validity_days",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_offer",
    "catalog_version_id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_offer",
    "channel",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_offer",
    "id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_offer",
    "offer_status",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_offer",
    "version_token",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_product",
    "id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_product",
    "product_code",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "audience",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "billing_cycle",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "currency",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "installments",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "offer_type",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "price_cents",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "product_id",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "sale_status",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.catalog_version",
    "version_no",
    "SELECT",
    "catalog_order_owner",
    false
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "id",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "order_no",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "channel",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "catalog_version_id",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "catalog_offer_id",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "purchase_consent_id",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "amount_cents",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "currency",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "payment_status",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "snapshot",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "snapshot_sha256",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "created_at",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "paid_at",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "catalog_order.customer_order",
    "closed_at",
    "SELECT",
    "catalog_order_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "entitlement_metering.entitlement_issuance",
    "order_id",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "customer_iam_role",
    "column",
    "entitlement_metering.entitlement_issuance",
    "status",
    "SELECT",
    "entitlement_metering_owner"
  ],
  [
    "customer_iam_role",
    "schema",
    "entitlement_metering",
    "",
    "USAGE",
    "entitlement_metering_owner"
  ]
]$final$::jsonb;
    expected_acls jsonb;
    expected_owners jsonb := $owners$
{
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
  ],
  [
    "019",
    "V019__customer_catalog_offer_read_privileges.sql",
    -1794072022,
    "customer catalog offer read privileges"
  ]
]$history$::jsonb;
    expected_routines jsonb := $routines$
{
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
    target_oids oid[];
    boundary_oids oid[];
    managed_names text[];
    login_names text[];
    marked_count integer;
    invariant_before jsonb;
    invariant_after jsonb;
    actual_value boolean;
    privilege_name text;
BEGIN
    IF current_setting('server_version_num')::integer NOT BETWEEN 160000 AND 169999
       OR current_user <> session_user
       OR NOT (SELECT rolsuper FROM pg_roles WHERE rolname = current_user) THEN
        RAISE EXCEPTION 'rejected';
    END IF;
    IF session_user = ANY(effective_names || shared_context_targets)
        OR EXISTS (SELECT 1 FROM pg_roles WHERE rolname = session_user
            AND shobj_description(oid, 'pg_authid') LIKE 'onecall:pa25%') THEN
        RAISE EXCEPTION 'rejected';
    END IF;
    SELECT oid INTO STRICT database_oid FROM pg_database WHERE datname = current_database();
    PERFORM pg_advisory_xact_lock(725025, 1);
    PERFORM set_config('search_path', 'pg_catalog', true);
    failure_category := 'V020_PROVENANCE_REJECTED';
    SELECT count(*) INTO marked_count FROM pg_roles
        WHERE shobj_description(oid, 'pg_authid') LIKE 'onecall:pa25%';
    IF marked_count NOT IN (0, 3, 4) THEN RAISE EXCEPTION 'rejected'; END IF;
    IF marked_count > 0 THEN
        FOR slot_index IN 1..marked_count LOOP
            SELECT count(*), min(rolname::text) INTO edge_count, role_name FROM pg_roles
            WHERE convert_to(shobj_description(oid, 'pg_authid'), 'UTF8') = convert_to(
                format('onecall:pa25:v1:db=%s:role=%s:slot=%s:name=%s',
                    database_oid, oid, slots[slot_index], rolname), 'UTF8');
            IF edge_count <> 1 THEN RAISE EXCEPTION 'rejected'; END IF;
            external_names := array_append(external_names, role_name);
        END LOOP;
    END IF;
    managed_names := external_names || effective_names || shared_context_targets;
    login_names := external_names || ARRAY['sharing_public_read_role', 'sharing_admission_role', 'evidence_media_reader_role'];
    SELECT array_agg(oid) INTO target_oids FROM pg_roles WHERE rolname = ANY(target_names);
    SELECT array_agg(oid) INTO external_oids FROM pg_roles WHERE rolname = ANY(external_names);
    boundary_oids := target_oids || coalesce(external_oids, ARRAY[]::oid[]);
    IF cardinality(target_oids) <> 1 THEN RAISE EXCEPTION 'rejected'; END IF;
    failure_category := 'V020_ACL_REJECTED';
    IF jsonb_array_length(old_acls) <> 75 OR jsonb_array_length(final_acls) <> 92
        OR (SELECT count(DISTINCT value) FROM jsonb_array_elements(old_acls)) <> 75
        OR (SELECT count(DISTINCT value) FROM jsonb_array_elements(final_acls)) <> 92
        OR EXISTS (SELECT value FROM jsonb_array_elements(old_acls)
            EXCEPT SELECT value FROM jsonb_array_elements(final_acls))
        OR (SELECT count(*) FROM (SELECT value FROM jsonb_array_elements(final_acls)
            EXCEPT SELECT value FROM jsonb_array_elements(old_acls)) delta) <> 17 THEN
        RAISE EXCEPTION 'rejected';
    END IF;
    FOR pass IN 1..2 LOOP
        SELECT array_agg(oid) INTO managed_oids FROM pg_roles WHERE rolname = ANY(managed_names);
        SELECT array_agg(oid) INTO external_oids FROM pg_roles WHERE rolname = ANY(external_names);
        failure_category := 'V020_PROVENANCE_REJECTED';
        FOR slot_index IN 1..marked_count LOOP
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
        failure_category := 'V020_HISTORY_REJECTED';
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
                WHERE type = 'SQL' AND success AND installed_rank <= 19
            )
            SELECT 1 FROM ((SELECT * FROM expected EXCEPT SELECT * FROM actual)
                UNION ALL (SELECT * FROM actual EXCEPT SELECT * FROM expected)) difference
        ) OR (SELECT count(*) FROM public.flyway_schema_history) NOT IN (19, 20)
           OR EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE installed_rank < 1)
           OR EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE installed_rank > 19
               AND (installed_rank <> 20 OR version IS DISTINCT FROM '020' OR type IS DISTINCT FROM 'SQL'
                   OR NOT success OR script IS DISTINCT FROM 'V020__customer_order_read_privileges.sql'
                   OR description IS DISTINCT FROM 'customer order read privileges' OR checksum IS NULL)) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'V020_SETTINGS_REJECTED';
        IF EXISTS (SELECT 1 FROM pg_db_role_setting WHERE setrole = ANY(managed_oids)
            OR (setrole = 0 AND setdatabase IN (0, database_oid))) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'V020_ATTRIBUTES_REJECTED';
        FOREACH role_name IN ARRAY managed_names LOOP
            SELECT auth.rolcanlogin, auth.rolinherit, auth.rolsuper, auth.rolcreatedb, auth.rolcreaterole,
                auth.rolreplication, auth.rolbypassrls, auth.rolconnlimit, auth.rolvaliduntil,
                auth.rolpassword IS NULL AS password_is_null, roles.rolconfig INTO role_row FROM pg_authid auth
                JOIN pg_roles roles ON roles.oid = auth.oid WHERE auth.rolname = role_name;
            IF NOT FOUND THEN RAISE EXCEPTION 'rejected'; END IF;
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
        failure_category := 'V020_TOPOLOGY_REJECTED';
        IF EXISTS (
            SELECT 1 FROM pg_auth_members membership
            JOIN pg_roles target ON target.oid = membership.roleid
            JOIN pg_roles member_role ON member_role.oid = membership.member
            JOIN pg_roles grantor ON grantor.oid = membership.grantor
            WHERE (membership.roleid = ANY(managed_oids) OR membership.member = ANY(managed_oids)
                OR membership.grantor = ANY(managed_oids))
              AND (((target.rolname = 'sharing_role' AND member_role.rolname = external_names[1])
                  OR (target.rolname = 'customer_iam_role' AND member_role.rolname = external_names[2])
                  OR (target.rolname IN ('sharing_command_role', 'evidence_role') AND member_role.rolname = external_names[3])
                  OR (target.rolname = ANY(ARRAY['operations_iam_role', 'subject_number_role', 'activation_consent_role', 'entitlement_metering_role', 'evidence_role', 'catalog_order_role', 'ivr_role']) AND member_role.rolname = external_names[4])) IS NOT TRUE
                  OR membership.admin_option OR membership.inherit_option OR NOT membership.set_option
                  OR NOT grantor.rolsuper OR grantor.oid = ANY(managed_oids))
        ) OR EXISTS (
            SELECT 1 FROM pg_auth_members WHERE roleid = ANY(managed_oids) OR member = ANY(managed_oids)
            GROUP BY roleid, member HAVING count(*) <> 1
        ) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        SELECT count(*) INTO edge_count FROM pg_auth_members
            WHERE roleid = ANY(managed_oids) OR member = ANY(managed_oids);
        IF edge_count <> (CASE marked_count WHEN 0 THEN 0 WHEN 3 THEN 4 ELSE 11 END) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'V020_OWNERSHIP_REJECTED';
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
        failure_category := 'V020_ACL_REJECTED';
        expected_acls := final_acls;
        IF pass = 1 AND NOT EXISTS (SELECT 1 FROM pg_attribute attribute
            CROSS JOIN LATERAL aclexplode(attribute.attacl) acl
            WHERE attribute.attrelid = 'catalog_order.customer_order'::regclass
                AND attribute.attname = 'id' AND acl.grantee = 'customer_iam_role'::regrole) THEN
            expected_acls := old_acls;
        END IF;
        IF expected_acls = old_acls AND EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE installed_rank = 20) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        SELECT jsonb_agg(jsonb_build_array(item->>0, item->>1,
            CASE item->>1 WHEN 'schema' THEN to_regnamespace(item->>2)::oid
                WHEN 'routine' THEN to_regprocedure(item->>2)::oid
                ELSE to_regclass(item->>2)::oid END,
            CASE WHEN item->>1 = 'column' THEN (SELECT attnum FROM pg_attribute
                WHERE attrelid = to_regclass(item->>2) AND attname = item->>3 AND NOT attisdropped) ELSE 0 END,
            item->>4, to_regrole(item->>5)::oid, false)) INTO expected_resolved
        FROM jsonb_array_elements(expected_acls) item;
        IF jsonb_array_length(expected_resolved) NOT IN (75, 92)
            OR (SELECT count(DISTINCT value) FROM jsonb_array_elements(expected_resolved)) <> jsonb_array_length(expected_resolved) OR EXISTS (
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
        FROM expanded WHERE grantee = ANY(boundary_oids) OR grantee = 0 OR grantor = ANY(managed_oids);
        IF jsonb_array_length(coalesce(actual_acls, '[]'::jsonb)) <> jsonb_array_length(expected_resolved) OR EXISTS (
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
            WHERE runtime.oid = ANY(target_oids) AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
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
            WHERE runtime.oid = ANY(target_oids) AND relation.relkind IN ('r','p','v','m','f')
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
            WHERE runtime.oid = ANY(target_oids) AND relation.relkind IN ('r','p','v','m','f')
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
            WHERE runtime.oid = ANY(target_oids) AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
                AND has_function_privilege(runtime.oid, routine.oid, 'EXECUTE') IS DISTINCT FROM EXISTS (
                    SELECT 1 FROM expected WHERE grantee_name = runtime.rolname AND kind = 'routine' AND object_oid = routine.oid)
        ) OR EXISTS (
            SELECT 1 FROM pg_roles runtime CROSS JOIN pg_class relation
                JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
            WHERE runtime.oid = ANY(target_oids) AND relation.relkind = 'S'
                AND namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'
                AND has_sequence_privilege(runtime.oid, relation.oid, 'USAGE,SELECT,UPDATE')
        ) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'V020_DATABASE_REJECTED';
        IF EXISTS (SELECT 1 FROM pg_database database_row CROSS JOIN LATERAL aclexplode(
                coalesce(database_row.datacl, acldefault('d', database_row.datdba))) acl
            WHERE acl.grantee = ANY(managed_oids) OR acl.grantor = ANY(managed_oids)
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
        failure_category := 'V020_DEFAULT_ACL_REJECTED';
        IF EXISTS (SELECT 1 FROM pg_default_acl defaults
            WHERE defaults.defaclrole = ANY(managed_oids))
            OR EXISTS (SELECT 1 FROM pg_default_acl defaults
                CROSS JOIN LATERAL aclexplode(defaults.defaclacl) acl
            WHERE acl.grantee = 0 OR acl.grantee = ANY(managed_oids)
                OR acl.grantor = ANY(managed_oids)) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        failure_category := 'V020_DEPENDENCY_REJECTED';
        IF EXISTS (
            SELECT 1 FROM pg_shdepend dependency
            WHERE dependency.refclassid = 'pg_authid'::regclass AND dependency.refobjid = ANY(managed_oids)
                AND NOT (dependency.dbid = database_oid AND dependency.deptype = 'a'
                    AND dependency.refobjid <> ALL(coalesce(external_oids, ARRAY[]::oid[]))
                    AND (dependency.refobjid <> ALL(target_oids)
                        OR EXISTS (SELECT 1 FROM jsonb_array_elements(expected_resolved) item
                        WHERE to_regrole(item->>0)::oid = dependency.refobjid AND (item->>2)::oid = dependency.objid
                            AND (item->>3)::integer = dependency.objsubid AND dependency.classid = CASE item->>1
                                WHEN 'schema' THEN 'pg_namespace'::regclass
                                WHEN 'routine' THEN 'pg_proc'::regclass ELSE 'pg_class'::regclass END)))
        ) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        IF EXISTS (SELECT 1 FROM pg_depend dependency
            WHERE dependency.deptype = 'e' AND (EXISTS (SELECT 1 FROM pg_shdepend shared
                WHERE shared.refclassid = 'pg_authid'::regclass AND shared.refobjid = ANY(managed_oids)
                    AND shared.dbid = database_oid AND shared.classid = dependency.classid
                    AND shared.objid = dependency.objid) OR EXISTS (
                SELECT 1 FROM jsonb_array_elements(expected_resolved) item
                WHERE (item->>2)::oid = dependency.objid AND dependency.classid = CASE item->>1
                    WHEN 'schema' THEN 'pg_namespace'::regclass
                    WHEN 'routine' THEN 'pg_proc'::regclass ELSE 'pg_class'::regclass END))) THEN
            RAISE EXCEPTION 'rejected';
        END IF;
        SELECT jsonb_build_object(
            'roles', (SELECT jsonb_agg(to_jsonb(auth) ORDER BY oid) FROM pg_authid auth),
            'comments', (SELECT jsonb_agg(to_jsonb(comment_row) ORDER BY objoid, classoid) FROM pg_shdescription comment_row),
            'memberships', (SELECT jsonb_agg(to_jsonb(membership) ORDER BY oid) FROM pg_auth_members membership),
            'settings', (SELECT jsonb_agg(to_jsonb(setting_row) ORDER BY setdatabase, setrole) FROM pg_db_role_setting setting_row),
            'defaults', (SELECT jsonb_agg(to_jsonb(default_row) ORDER BY oid) FROM pg_default_acl default_row),
            'databases', (SELECT jsonb_agg(to_jsonb(database_row) ORDER BY oid) FROM pg_database database_row),
            'definitions', (SELECT jsonb_agg(to_jsonb(routine) ORDER BY oid) FROM pg_proc routine),
            'constraints', (SELECT jsonb_agg(to_jsonb(constraint_row) ORDER BY oid) FROM pg_constraint constraint_row),
            'indexes', (SELECT jsonb_agg(to_jsonb(index_row) ORDER BY indexrelid) FROM pg_index index_row),
            'triggers', (SELECT jsonb_agg(to_jsonb(trigger_row) ORDER BY oid) FROM pg_trigger trigger_row),
            'relations', (SELECT jsonb_agg(to_jsonb(relation) - 'relacl' ORDER BY relation.oid)
                FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
                WHERE namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'),
            'columns', (SELECT jsonb_agg(to_jsonb(attribute) - 'attacl' ORDER BY attribute.attrelid, attribute.attnum)
                FROM pg_attribute attribute JOIN pg_class relation ON relation.oid = attribute.attrelid
                JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
                WHERE namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'),
            'types', (SELECT jsonb_agg(to_jsonb(object_type) ORDER BY object_type.oid)
                FROM pg_type object_type JOIN pg_namespace namespace ON namespace.oid = object_type.typnamespace
                WHERE namespace.nspname !~ '^pg_' AND namespace.nspname <> 'information_schema'),
            'dependencies', (SELECT jsonb_agg(to_jsonb(dependency) ORDER BY dependency.dbid,
                dependency.classid, dependency.objid, dependency.objsubid, dependency.refobjid, dependency.deptype)
                FROM pg_shdepend dependency WHERE NOT (dependency.dbid = database_oid
                    AND dependency.refclassid = 'pg_authid'::regclass
                    AND dependency.refobjid = 'customer_iam_role'::regrole AND dependency.deptype = 'a'
                    AND EXISTS (
                        SELECT 1 FROM jsonb_array_elements(final_acls) item
                        WHERE NOT old_acls @> jsonb_build_array(item)
                        AND dependency.classid = CASE item->>1 WHEN 'schema' THEN 'pg_namespace'::regclass ELSE 'pg_class'::regclass END
                        AND CASE item->>1 WHEN 'schema' THEN to_regnamespace(item->>2)::oid ELSE to_regclass(item->>2)::oid END = dependency.objid
                        AND CASE item->>1 WHEN 'schema' THEN 0 ELSE (SELECT attnum FROM pg_attribute
                            WHERE attrelid = dependency.objid AND attname = item->>3 AND NOT attisdropped) END = dependency.objsubid))),
            'owners', (SELECT jsonb_agg(jsonb_build_array(kind, oid, owner) ORDER BY kind, oid) FROM (
                SELECT 'schema' AS kind, oid, nspowner AS owner FROM pg_namespace
                UNION ALL SELECT 'relation', oid, relowner FROM pg_class
                UNION ALL SELECT 'routine', oid, proowner FROM pg_proc
                UNION ALL SELECT 'type', oid, typowner FROM pg_type) ownership),
            'other_acls', (SELECT jsonb_agg(jsonb_build_array(kind, oid, subid, grantee, grantor, privilege_type, is_grantable)
                    ORDER BY kind, oid, subid, grantee, grantor, privilege_type, is_grantable)
                FROM (
                    SELECT 'schema' AS kind, oid, 0 AS subid, nspacl AS acl FROM pg_namespace
                    UNION ALL SELECT 'relation', oid, 0, relacl FROM pg_class
                    UNION ALL SELECT 'column', attrelid, attnum, attacl FROM pg_attribute
                    UNION ALL SELECT 'routine', oid, 0, proacl FROM pg_proc
                ) objects CROSS JOIN LATERAL aclexplode(objects.acl) acl
                WHERE NOT (acl.grantee = 'customer_iam_role'::regrole
                    AND NOT acl.is_grantable AND EXISTS (SELECT 1 FROM jsonb_array_elements(final_acls) item
                        WHERE NOT old_acls @> jsonb_build_array(item)
                        AND kind = item->>1 AND acl.privilege_type = item->>4
                        AND acl.grantor = to_regrole(item->>5)::oid
                        AND CASE item->>1 WHEN 'schema' THEN to_regnamespace(item->>2)::oid ELSE to_regclass(item->>2)::oid END = objects.oid
                        AND CASE item->>1 WHEN 'schema' THEN 0 ELSE (SELECT attnum FROM pg_attribute
                            WHERE attrelid = objects.oid AND attname = item->>3 AND NOT attisdropped) END = objects.subid)))
        ) INTO invariant_after;
        IF pass = 1 THEN invariant_before := invariant_after;
        ELSIF invariant_after IS DISTINCT FROM invariant_before THEN
            failure_category := 'V020_PRESERVATION_REJECTED';
            RAISE EXCEPTION 'rejected';
        END IF;
        IF pass = 1 THEN
            failure_category := 'V020_RECONCILIATION_REJECTED';
            IF expected_acls = old_acls THEN
                SET LOCAL ROLE catalog_order_owner;
                GRANT SELECT (id, order_no, channel, catalog_version_id, catalog_offer_id,
                    purchase_consent_id, amount_cents, currency, payment_status, snapshot,
                    snapshot_sha256, created_at, paid_at, closed_at)
                    ON TABLE catalog_order.customer_order TO customer_iam_role;
                EXECUTE format('SET LOCAL ROLE %I', session_user);
                SET LOCAL ROLE entitlement_metering_owner;
                GRANT USAGE ON SCHEMA entitlement_metering TO customer_iam_role;
                GRANT SELECT (order_id, status)
                    ON TABLE entitlement_metering.entitlement_issuance TO customer_iam_role;
                EXECUTE format('SET LOCAL ROLE %I', session_user);
            END IF;
        END IF;
    END LOOP;
EXCEPTION WHEN OTHERS THEN
    RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = failure_category;
END;
$migration$;
