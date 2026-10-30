-- PA7 V007: activation and consent foundation.
-- Creates the three frozen activation_consent tables (43 columns, 31 named
-- checks, 5 ordinary unique keys, 12 index objects), two local SECURITY
-- INVOKER trigger functions, three triggers and the exact owner/runtime/
-- cross-module grants. Ten ordinary immediate RESTRICT FKs are added by the
-- migration identity after RESET ROLE; the eleventh fk_customer_order_consent
-- is the deliberate late ALTER_FK on catalog_order.customer_order.
-- purchase_consent_id now that consent_record exists. No schema/role
-- creation, no SQL service function, no V008+ object, no application
-- service.

SET LOCAL ROLE activation_consent_owner;

CREATE TABLE activation_consent.consent_record (
    id uuid NOT NULL,
    subject_id uuid NOT NULL,
    purpose text NOT NULL,
    channel text NOT NULL,
    event_domain text NOT NULL,
    policy_code text NOT NULL,
    policy_version text NOT NULL,
    prompt_version text NOT NULL,
    action text NOT NULL,
    source_type text NOT NULL,
    source_ref text NOT NULL,
    ivr_session_id uuid,
    account_id uuid,
    employee_id uuid,
    snapshot jsonb NOT NULL,
    snapshot_sha256 text NOT NULL,
    event_id text NOT NULL,
    request_fingerprint text NOT NULL,
    accepted_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_consent_record PRIMARY KEY (id),
    CONSTRAINT uq_consent_event UNIQUE (event_domain, event_id),
    CONSTRAINT ck_consent_purpose
        CHECK (purpose IN ('SERVICE_ACTIVATION','EVIDENCE_RECORDING','PURCHASE')),
    CONSTRAINT ck_consent_channel
        CHECK (channel IN ('IVR','MINIAPP','WEB','OPERATIONS')),
    CONSTRAINT ck_consent_event_domain
        CHECK (event_domain IN ('IVR_CONFIRM','ACTIVATION','PURCHASE','OPERATIONS')),
    CONSTRAINT ck_consent_policy_code
        CHECK (length(policy_code) BETWEEN 1 AND 64 AND btrim(policy_code) <> ''),
    CONSTRAINT ck_consent_policy_version
        CHECK (length(policy_version) BETWEEN 1 AND 64 AND btrim(policy_version) <> ''),
    CONSTRAINT ck_consent_prompt_version
        CHECK (length(prompt_version) BETWEEN 1 AND 64 AND btrim(prompt_version) <> ''),
    CONSTRAINT ck_consent_action
        CHECK (action IN ('ACCEPT','DECLINE')),
    CONSTRAINT ck_consent_source_type
        CHECK (source_type IN ('IVR_SESSION','ACCOUNT','EMPLOYEE')),
    CONSTRAINT ck_consent_source_ref
        CHECK (length(source_ref) BETWEEN 1 AND 128 AND btrim(source_ref) <> ''),
    CONSTRAINT ck_consent_source_shape
        CHECK ((source_type = 'IVR_SESSION' AND ivr_session_id IS NOT NULL
                    AND account_id IS NULL AND employee_id IS NULL)
            OR (source_type = 'ACCOUNT' AND account_id IS NOT NULL
                    AND ivr_session_id IS NULL AND employee_id IS NULL)
            OR (source_type = 'EMPLOYEE' AND employee_id IS NOT NULL
                    AND ivr_session_id IS NULL AND account_id IS NULL)),
    CONSTRAINT ck_consent_channel_shape
        CHECK ((channel = 'IVR' AND source_type = 'IVR_SESSION')
            OR (channel IN ('MINIAPP','WEB') AND source_type = 'ACCOUNT')
            OR (channel = 'OPERATIONS' AND source_type = 'EMPLOYEE')),
    CONSTRAINT ck_consent_snapshot_type
        CHECK (jsonb_typeof(snapshot) = 'object'),
    CONSTRAINT ck_consent_snapshot_sha
        CHECK (snapshot_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_consent_event_id
        CHECK (length(event_id) BETWEEN 1 AND 128 AND btrim(event_id) <> ''),
    CONSTRAINT ck_consent_fingerprint
        CHECK (request_fingerprint ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_consent_subject_purpose
    ON activation_consent.consent_record (subject_id, purpose, accepted_at DESC);

CREATE INDEX ix_consent_session
    ON activation_consent.consent_record (ivr_session_id);

CREATE TABLE activation_consent.service_activation (
    id uuid NOT NULL,
    subject_id uuid NOT NULL,
    status text NOT NULL DEFAULT 'INACTIVE',
    revision integer NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_service_activation PRIMARY KEY (id),
    CONSTRAINT uq_service_activation_subject UNIQUE (subject_id),
    CONSTRAINT ck_service_activation_status
        CHECK (status IN ('INACTIVE','ACTIVE','SUSPENDED')),
    CONSTRAINT ck_service_activation_revision
        CHECK (revision >= 1),
    CONSTRAINT ck_service_activation_time_order
        CHECK (updated_at >= created_at)
);

CREATE INDEX ix_service_activation_status
    ON activation_consent.service_activation (status);

CREATE TABLE activation_consent.service_activation_transition (
    id uuid NOT NULL,
    activation_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    from_status text NOT NULL,
    to_status text NOT NULL,
    from_revision integer NOT NULL,
    to_revision integer NOT NULL,
    source_channel text NOT NULL,
    source_action_domain text NOT NULL,
    source_action_event_id text NOT NULL,
    status_event_domain text NOT NULL,
    status_event_id text NOT NULL,
    request_fingerprint text NOT NULL,
    actor_account_id uuid,
    actor_employee_id uuid,
    reason_code text NOT NULL,
    consent_record_id uuid,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_service_activation_transition PRIMARY KEY (id),
    CONSTRAINT uq_activation_transition_revision UNIQUE (activation_id, to_revision),
    CONSTRAINT uq_activation_transition_action UNIQUE (source_action_domain, source_action_event_id),
    CONSTRAINT uq_activation_transition_status_event UNIQUE (status_event_domain, status_event_id),
    CONSTRAINT ck_activation_transition_status
        CHECK (from_status IN ('INACTIVE','ACTIVE','SUSPENDED')
            AND to_status IN ('INACTIVE','ACTIVE','SUSPENDED')),
    CONSTRAINT ck_activation_transition_matrix
        CHECK ((from_status = 'INACTIVE' AND to_status = 'ACTIVE')
            OR (from_status = 'ACTIVE' AND to_status = 'SUSPENDED')
            OR (from_status = 'SUSPENDED' AND to_status = 'ACTIVE'
                AND source_action_domain = 'OPERATIONS')),
    CONSTRAINT ck_activation_transition_from_revision
        CHECK (from_revision >= 0),
    CONSTRAINT ck_activation_transition_to_revision
        CHECK (to_revision = from_revision + 1 AND to_revision >= 1),
    CONSTRAINT ck_activation_transition_channel
        CHECK (source_channel IN ('IVR','MINIAPP','WEB','OPERATIONS')),
    CONSTRAINT ck_activation_transition_channel_shape
        CHECK ((source_channel = 'OPERATIONS' AND source_action_domain = 'OPERATIONS')
            OR (source_channel = 'IVR' AND source_action_domain = 'IVR_CONFIRM')
            OR (source_channel IN ('MINIAPP','WEB') AND source_action_domain = 'ACTIVATION')),
    CONSTRAINT ck_activation_transition_action_domain
        CHECK (source_action_domain IN ('ACTIVATION','IVR_CONFIRM','OPERATIONS')),
    CONSTRAINT ck_activation_transition_action_event
        CHECK (length(source_action_event_id) BETWEEN 1 AND 128
            AND btrim(source_action_event_id) <> ''),
    CONSTRAINT ck_activation_transition_status_domain
        CHECK (status_event_domain = 'ACTIVATION_STATUS'),
    CONSTRAINT ck_activation_transition_status_event_id
        CHECK (length(status_event_id) BETWEEN 1 AND 128 AND btrim(status_event_id) <> ''),
    CONSTRAINT ck_activation_transition_fingerprint
        CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_activation_transition_actor_shape
        CHECK ((source_action_domain = 'OPERATIONS' AND actor_employee_id IS NOT NULL
                    AND consent_record_id IS NULL)
            OR (source_action_domain <> 'OPERATIONS' AND consent_record_id IS NOT NULL)),
    CONSTRAINT ck_activation_transition_reason
        CHECK (length(reason_code) BETWEEN 1 AND 64 AND btrim(reason_code) <> '')
);

CREATE INDEX ix_activation_transition_occurred
    ON activation_consent.service_activation_transition (subject_id, occurred_at DESC);

CREATE FUNCTION activation_consent.reject_row_mutation() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = activation_consent, pg_temp
AS $fn$
BEGIN
    RAISE EXCEPTION 'CONSENT_TRANSITION_APPEND_ONLY' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE FUNCTION activation_consent.guard_service_activation() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = activation_consent, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.subject_id IS DISTINCT FROM OLD.subject_id
        OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'SERVICE_ACTIVATION_HEAD_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE TRIGGER trg_consent_record_immutable
BEFORE UPDATE OR DELETE ON activation_consent.consent_record
FOR EACH ROW EXECUTE FUNCTION activation_consent.reject_row_mutation();

CREATE TRIGGER trg_service_activation_guard
BEFORE UPDATE ON activation_consent.service_activation
FOR EACH ROW EXECUTE FUNCTION activation_consent.guard_service_activation();

CREATE TRIGGER trg_activation_transition_immutable
BEFORE UPDATE OR DELETE ON activation_consent.service_activation_transition
FOR EACH ROW EXECUTE FUNCTION activation_consent.reject_row_mutation();

REVOKE ALL ON TABLE activation_consent.consent_record FROM PUBLIC;
REVOKE ALL ON TABLE activation_consent.service_activation FROM PUBLIC;
REVOKE ALL ON TABLE activation_consent.service_activation_transition FROM PUBLIC;
REVOKE ALL ON FUNCTION activation_consent.reject_row_mutation() FROM PUBLIC;
REVOKE ALL ON FUNCTION activation_consent.guard_service_activation() FROM PUBLIC;

GRANT SELECT, INSERT ON TABLE activation_consent.consent_record TO activation_consent_role;
GRANT SELECT, INSERT ON TABLE activation_consent.service_activation TO activation_consent_role;
GRANT UPDATE (status, revision, updated_at)
    ON TABLE activation_consent.service_activation TO activation_consent_role;
GRANT SELECT, INSERT ON TABLE activation_consent.service_activation_transition
    TO activation_consent_role;

GRANT EXECUTE ON FUNCTION activation_consent.guard_service_activation()
    TO activation_consent_role;

GRANT SELECT (id, subject_id, status, revision)
    ON TABLE activation_consent.service_activation TO ivr_role;
GRANT SELECT (subject_id, purpose, channel, policy_code, policy_version, prompt_version,
              action, event_domain, event_id, accepted_at)
    ON TABLE activation_consent.consent_record TO ivr_role;

GRANT SELECT (id, subject_id, status, revision)
    ON TABLE activation_consent.service_activation TO catalog_order_role;
GRANT SELECT (id, subject_id, purpose, channel, event_domain, event_id, policy_code,
              policy_version, prompt_version, action, source_type, source_ref, account_id,
              request_fingerprint, accepted_at)
    ON TABLE activation_consent.consent_record TO catalog_order_role;

RESET ROLE;

SET LOCAL ROLE subject_number_owner;

GRANT USAGE ON SCHEMA subject_number TO activation_consent_role;
GRANT SELECT (id, owner_account_id, owner_organization_id, revision)
    ON TABLE subject_number.business_subject TO activation_consent_role;

RESET ROLE;

SET LOCAL ROLE customer_iam_owner;

GRANT USAGE ON SCHEMA customer_iam TO activation_consent_role;
GRANT SELECT (id, status)
    ON TABLE customer_iam.customer_account TO activation_consent_role;

RESET ROLE;

SET LOCAL ROLE organization_access_owner;

GRANT USAGE ON SCHEMA organization_access TO activation_consent_role;
GRANT SELECT (id, status)
    ON TABLE organization_access.organization TO activation_consent_role;
GRANT SELECT (organization_id, account_id, role, status)
    ON TABLE organization_access.organization_membership TO activation_consent_role;

RESET ROLE;

ALTER TABLE activation_consent.consent_record
    ADD CONSTRAINT fk_consent_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE activation_consent.consent_record
    ADD CONSTRAINT fk_consent_session
    FOREIGN KEY (ivr_session_id) REFERENCES ivr.inbound_session(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE activation_consent.consent_record
    ADD CONSTRAINT fk_consent_account
    FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE activation_consent.consent_record
    ADD CONSTRAINT fk_consent_employee
    FOREIGN KEY (employee_id) REFERENCES operations_iam.employee_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE activation_consent.service_activation
    ADD CONSTRAINT fk_service_activation_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE activation_consent.service_activation_transition
    ADD CONSTRAINT fk_activation_transition_activation
    FOREIGN KEY (activation_id) REFERENCES activation_consent.service_activation(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE activation_consent.service_activation_transition
    ADD CONSTRAINT fk_activation_transition_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE activation_consent.service_activation_transition
    ADD CONSTRAINT fk_activation_transition_actor_account
    FOREIGN KEY (actor_account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE activation_consent.service_activation_transition
    ADD CONSTRAINT fk_activation_transition_actor_employee
    FOREIGN KEY (actor_employee_id) REFERENCES operations_iam.employee_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE activation_consent.service_activation_transition
    ADD CONSTRAINT fk_activation_transition_consent
    FOREIGN KEY (consent_record_id) REFERENCES activation_consent.consent_record(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.customer_order
    ADD CONSTRAINT fk_customer_order_consent
    FOREIGN KEY (purchase_consent_id)
    REFERENCES activation_consent.consent_record(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
