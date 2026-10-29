-- PA6 V006: trial, entitlement, issuance, reservation and usage foundation.
-- Creates the seven frozen entitlement_metering tables (84 columns, 50 named
-- checks, 12 unique keys, 29 index objects), one SECURITY DEFINER service
-- function issue_first_trial, seven local SECURITY INVOKER trigger functions,
-- seven triggers and the exact owner/runtime/cross-module grants. Fifteen
-- ordinary immediate RESTRICT FKs are added by the migration identity after
-- RESET ROLE; the sixteenth fk_ivr_decision_reservation is the deliberate
-- late ALTER_FK on ivr.ivr_decision.reservation_id now that usage_reservation
-- exists. No schema/role creation, no service function beyond
-- issue_first_trial, no V007+ object, no application service.

SET LOCAL ROLE entitlement_metering_owner;

CREATE TABLE entitlement_metering.trial_policy_version (
    id uuid NOT NULL,
    campaign_key text NOT NULL,
    version_no integer NOT NULL,
    granted_seconds integer NOT NULL,
    valid_days integer NOT NULL,
    enabled boolean NOT NULL DEFAULT true,
    snapshot jsonb NOT NULL,
    snapshot_sha256 text NOT NULL,
    published_by_employee_id uuid NOT NULL,
    published_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_trial_policy_version PRIMARY KEY (id),
    CONSTRAINT uq_trial_policy_campaign_version UNIQUE (campaign_key, version_no),
    CONSTRAINT ck_trial_policy_campaign
        CHECK (campaign_key = 'FIRST-TRIAL'),
    CONSTRAINT ck_trial_policy_version_no
        CHECK (version_no >= 1),
    CONSTRAINT ck_trial_policy_granted_seconds
        CHECK (granted_seconds >= 1),
    CONSTRAINT ck_trial_policy_valid_days
        CHECK (valid_days >= 1),
    CONSTRAINT ck_trial_policy_snapshot_type
        CHECK (jsonb_typeof(snapshot) = 'object'),
    CONSTRAINT ck_trial_policy_snapshot_sha
        CHECK (snapshot_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_trial_policy_revision
        CHECK (revision >= 1)
);

CREATE UNIQUE INDEX uq_trial_policy_enabled
    ON entitlement_metering.trial_policy_version (campaign_key)
    WHERE enabled AND published_at IS NOT NULL;

CREATE INDEX ix_trial_policy_enabled
    ON entitlement_metering.trial_policy_version (enabled, published_at);

CREATE TABLE entitlement_metering.entitlement_grant (
    id uuid NOT NULL,
    subject_id uuid NOT NULL,
    source_type text NOT NULL,
    source_ref_id uuid NOT NULL,
    entitlement_type text NOT NULL,
    granted_units bigint NOT NULL,
    valid_from timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    order_id uuid,
    installment_no integer,
    snapshot jsonb NOT NULL,
    snapshot_sha256 text NOT NULL,
    granted_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_entitlement_grant PRIMARY KEY (id),
    CONSTRAINT ck_entitlement_grant_source
        CHECK (source_type IN ('TRIAL','PURCHASE')),
    CONSTRAINT ck_entitlement_grant_source_shape
        CHECK ((source_type = 'TRIAL' AND order_id IS NULL AND installment_no IS NULL)
            OR (source_type = 'PURCHASE' AND order_id IS NOT NULL AND installment_no IS NOT NULL
                AND installment_no >= 1 AND source_ref_id = order_id)),
    CONSTRAINT ck_entitlement_grant_type
        CHECK (entitlement_type IN ('RECORDING_SECONDS','AI_SECONDS','EVIDENCE_COUNT','STORAGE_BYTES')),
    CONSTRAINT ck_entitlement_grant_units
        CHECK (granted_units >= 1),
    CONSTRAINT ck_entitlement_grant_expiry
        CHECK (expires_at > valid_from),
    CONSTRAINT ck_entitlement_grant_installment
        CHECK (installment_no IS NULL OR installment_no >= 1),
    CONSTRAINT ck_entitlement_grant_snapshot_type
        CHECK (jsonb_typeof(snapshot) = 'object'),
    CONSTRAINT ck_entitlement_grant_snapshot_sha
        CHECK (snapshot_sha256 ~ '^[0-9a-f]{64}$')
);

CREATE UNIQUE INDEX uq_entitlement_grant_paid
    ON entitlement_metering.entitlement_grant (order_id, installment_no, entitlement_type)
    WHERE source_type = 'PURCHASE';

CREATE INDEX ix_entitlement_grant_subject_type
    ON entitlement_metering.entitlement_grant (subject_id, entitlement_type, expires_at);

CREATE INDEX ix_entitlement_grant_order
    ON entitlement_metering.entitlement_grant (order_id, installment_no);

CREATE TABLE entitlement_metering.trial_grant (
    id uuid NOT NULL,
    subject_id uuid NOT NULL,
    campaign_key text NOT NULL,
    trial_policy_version_id uuid NOT NULL,
    entitlement_grant_id uuid NOT NULL,
    source_action_domain text NOT NULL,
    source_action_event_id text NOT NULL,
    request_fingerprint text NOT NULL,
    granted_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    CONSTRAINT pk_trial_grant PRIMARY KEY (id),
    CONSTRAINT uq_trial_grant_subject UNIQUE (subject_id),
    CONSTRAINT uq_trial_grant_grant UNIQUE (entitlement_grant_id),
    CONSTRAINT uq_trial_grant_source UNIQUE (source_action_domain, source_action_event_id),
    CONSTRAINT ck_trial_grant_campaign
        CHECK (campaign_key = 'FIRST-TRIAL'),
    CONSTRAINT ck_trial_grant_action_domain
        CHECK (source_action_domain IN ('IVR_CONFIRM','ACTIVATION')),
    CONSTRAINT ck_trial_grant_event_id
        CHECK (length(source_action_event_id) BETWEEN 1 AND 128 AND btrim(source_action_event_id) <> ''),
    CONSTRAINT ck_trial_grant_fingerprint
        CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_trial_grant_expiry
        CHECK (expires_at > granted_at)
);

CREATE INDEX ix_trial_grant_campaign_time
    ON entitlement_metering.trial_grant (campaign_key, granted_at);

CREATE TABLE entitlement_metering.entitlement_balance (
    id uuid NOT NULL,
    grant_id uuid NOT NULL,
    granted_units bigint NOT NULL,
    reserved_units bigint NOT NULL DEFAULT 0,
    consumed_units bigint NOT NULL DEFAULT 0,
    status text NOT NULL DEFAULT 'AVAILABLE',
    revision integer NOT NULL DEFAULT 1,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_entitlement_balance PRIMARY KEY (id),
    CONSTRAINT uq_entitlement_balance_grant UNIQUE (grant_id),
    CONSTRAINT ck_entitlement_balance_granted
        CHECK (granted_units >= 1),
    CONSTRAINT ck_entitlement_balance_units
        CHECK (reserved_units >= 0 AND consumed_units >= 0
            AND reserved_units + consumed_units <= granted_units),
    CONSTRAINT ck_entitlement_balance_status
        CHECK (status IN ('AVAILABLE','EXHAUSTED','EXPIRED')),
    CONSTRAINT ck_entitlement_balance_status_shape
        CHECK ((status = 'EXHAUSTED') = (consumed_units = granted_units)),
    CONSTRAINT ck_entitlement_balance_revision
        CHECK (revision >= 1)
);

CREATE INDEX ix_entitlement_balance_status
    ON entitlement_metering.entitlement_balance (status);

CREATE FUNCTION entitlement_metering.issue_first_trial(
    p_subject_id uuid,
    p_campaign_key text,
    p_trial_policy_version_id uuid,
    p_action_domain text,
    p_action_event_id text,
    p_request_fingerprint text
) RETURNS TABLE (
    trial_grant_id uuid,
    entitlement_grant_id uuid,
    entitlement_balance_id uuid,
    outcome text,
    first_granted_at timestamptz
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = entitlement_metering, platform, pg_temp
AS $fn$
#variable_conflict use_column
DECLARE
    v_trial_id uuid;
    v_grant_id uuid;
    v_balance_id uuid;
    v_granted_at timestamptz;
    v_expires_at timestamptz;
    v_policy RECORD;
    v_trial RECORD;
    v_grant RECORD;
    v_balance RECORD;
    v_payload jsonb;
    v_canonical text;
    v_sha text;
BEGIN
    IF p_subject_id IS NULL
        OR p_trial_policy_version_id IS NULL
        OR p_action_domain IS NULL
        OR p_action_event_id IS NULL
        OR p_request_fingerprint IS NULL
        OR p_action_domain NOT IN ('IVR_CONFIRM','ACTIVATION')
        OR length(p_action_event_id) NOT BETWEEN 1 AND 128
        OR btrim(p_action_event_id) = ''
        OR p_request_fingerprint !~ '^[0-9a-f]{64}$' THEN
        RAISE EXCEPTION 'TRIAL_ARGUMENT_INVALID' USING ERRCODE = 'P0001';
    END IF;
    IF p_campaign_key IS NULL THEN
        RAISE EXCEPTION 'TRIAL_ARGUMENT_INVALID' USING ERRCODE = 'P0001';
    END IF;
    IF p_campaign_key <> 'FIRST-TRIAL' THEN
        RAISE EXCEPTION 'TRIAL_CAMPAIGN_INVALID' USING ERRCODE = 'P0001';
    END IF;

    PERFORM pg_advisory_xact_lock(
        hashtextextended('subject_number.subject:' || p_subject_id::text, 0));

    SELECT * INTO v_trial
    FROM entitlement_metering.trial_grant AS tg
    WHERE tg.subject_id = p_subject_id;

    IF FOUND THEN
        SELECT * INTO v_grant
        FROM entitlement_metering.entitlement_grant AS g
        WHERE g.id = v_trial.entitlement_grant_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'TRIAL_DATA_INTEGRITY' USING ERRCODE = 'P0001';
        END IF;
        SELECT * INTO v_balance
        FROM entitlement_metering.entitlement_balance AS b
        WHERE b.grant_id = v_grant.id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'TRIAL_DATA_INTEGRITY' USING ERRCODE = 'P0001';
        END IF;
        IF v_grant.subject_id IS DISTINCT FROM v_trial.subject_id
            OR v_grant.source_type IS DISTINCT FROM 'TRIAL'
            OR v_grant.source_ref_id IS DISTINCT FROM v_trial.trial_policy_version_id
            OR v_grant.entitlement_type IS DISTINCT FROM 'RECORDING_SECONDS'
            OR v_grant.order_id IS NOT NULL
            OR v_grant.installment_no IS NOT NULL
            OR v_grant.granted_units IS DISTINCT FROM v_balance.granted_units
            OR v_grant.valid_from IS DISTINCT FROM v_grant.granted_at
            OR v_grant.granted_at IS DISTINCT FROM v_trial.granted_at
            OR v_grant.expires_at IS DISTINCT FROM v_trial.expires_at
            OR v_balance.grant_id IS DISTINCT FROM v_grant.id THEN
            RAISE EXCEPTION 'TRIAL_DATA_INTEGRITY' USING ERRCODE = 'P0001';
        END IF;
        IF EXISTS (
            SELECT 1 FROM entitlement_metering.trial_grant AS tg2
            WHERE tg2.source_action_domain = p_action_domain
              AND tg2.source_action_event_id = p_action_event_id
              AND tg2.subject_id <> p_subject_id
        ) THEN
            RAISE EXCEPTION 'TRIAL_DATA_INTEGRITY' USING ERRCODE = 'P0001';
        END IF;
        IF v_trial.source_action_domain = p_action_domain
            AND v_trial.source_action_event_id = p_action_event_id THEN
            IF v_trial.request_fingerprint IS DISTINCT FROM p_request_fingerprint THEN
                RAISE EXCEPTION 'TRIAL_ACTION_CONFLICT' USING ERRCODE = 'P0001';
            END IF;
            RETURN QUERY SELECT v_trial.id, v_trial.entitlement_grant_id, v_balance.id,
                'REPLAY'::text, v_trial.granted_at;
            RETURN;
        END IF;
        RETURN QUERY SELECT v_trial.id, v_trial.entitlement_grant_id, v_balance.id,
            'ALREADY_GRANTED'::text, v_trial.granted_at;
        RETURN;
    END IF;

    SELECT * INTO v_policy
    FROM entitlement_metering.trial_policy_version AS tpv
    WHERE tpv.campaign_key = 'FIRST-TRIAL'
      AND tpv.enabled
      AND tpv.published_at IS NOT NULL
      AND tpv.id = p_trial_policy_version_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'TRIAL_POLICY_NOT_FOUND' USING ERRCODE = 'P0001';
    END IF;

    v_granted_at := clock_timestamp();
    v_expires_at := v_granted_at + (v_policy.valid_days * interval '1 day');
    v_grant_id := pg_catalog.gen_random_uuid();
    v_trial_id := pg_catalog.gen_random_uuid();
    v_balance_id := pg_catalog.gen_random_uuid();

    INSERT INTO entitlement_metering.entitlement_grant AS eg
        (id, subject_id, source_type, source_ref_id, entitlement_type, granted_units,
         valid_from, expires_at, order_id, installment_no, snapshot, snapshot_sha256, granted_at)
    VALUES
        (v_grant_id, p_subject_id, 'TRIAL', p_trial_policy_version_id,
         'RECORDING_SECONDS', v_policy.granted_seconds, v_granted_at, v_expires_at,
         NULL, NULL, v_policy.snapshot, v_policy.snapshot_sha256, v_granted_at);

    INSERT INTO entitlement_metering.trial_grant AS tg
        (id, subject_id, campaign_key, trial_policy_version_id, entitlement_grant_id,
         source_action_domain, source_action_event_id, request_fingerprint, granted_at, expires_at)
    VALUES
        (v_trial_id, p_subject_id, 'FIRST-TRIAL', p_trial_policy_version_id, v_grant_id,
         p_action_domain, p_action_event_id, p_request_fingerprint, v_granted_at, v_expires_at);

    INSERT INTO entitlement_metering.entitlement_balance AS eb
        (id, grant_id, granted_units, reserved_units, consumed_units, status, revision, updated_at)
    VALUES
        (v_balance_id, v_grant_id, v_policy.granted_seconds, 0, 0, 'AVAILABLE', 1, v_granted_at);

    v_payload := jsonb_build_object(
        'trialGrantId', v_trial_id::text,
        'entitlementGrantId', v_grant_id::text,
        'entitlementBalanceId', v_balance_id::text,
        'subjectId', p_subject_id::text,
        'campaignKey', 'FIRST-TRIAL',
        'trialPolicyVersionId', p_trial_policy_version_id::text,
        'grantedUnits', v_policy.granted_seconds,
        'validFrom', to_char(v_granted_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
        'expiresAt', to_char(v_expires_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
        'grantedAt', to_char(v_granted_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US"Z"')
    );
    v_canonical := '{"campaignKey":' || (v_payload -> 'campaignKey')::text
        || ',"entitlementBalanceId":' || (v_payload -> 'entitlementBalanceId')::text
        || ',"entitlementGrantId":' || (v_payload -> 'entitlementGrantId')::text
        || ',"expiresAt":' || (v_payload -> 'expiresAt')::text
        || ',"grantedAt":' || (v_payload -> 'grantedAt')::text
        || ',"grantedUnits":' || (v_payload -> 'grantedUnits')::text
        || ',"subjectId":' || (v_payload -> 'subjectId')::text
        || ',"trialGrantId":' || (v_payload -> 'trialGrantId')::text
        || ',"trialPolicyVersionId":' || (v_payload -> 'trialPolicyVersionId')::text
        || ',"validFrom":' || (v_payload -> 'validFrom')::text
        || '}';
    v_sha := encode(pg_catalog.sha256(convert_to(v_canonical, 'UTF8')), 'hex');

    PERFORM platform.outbox_append(
        'ACTIVATION',
        'trial_grant:' || v_trial_id::text,
        'TRIAL_GRANT',
        v_trial_id,
        'FIRST_TRIAL_GRANTED',
        v_payload,
        NULL,
        v_sha
    );

    RETURN QUERY SELECT v_trial_id, v_grant_id, v_balance_id,
        'GRANTED'::text, v_granted_at;
END;
$fn$;

CREATE TABLE entitlement_metering.entitlement_issuance (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    installment_no integer NOT NULL,
    entitlement_type text NOT NULL,
    status text NOT NULL DEFAULT 'PENDING',
    attempt_count integer NOT NULL DEFAULT 0,
    revision integer NOT NULL DEFAULT 1,
    last_error_code text,
    grant_id uuid,
    source_event_domain text NOT NULL,
    source_event_id text NOT NULL,
    first_attempt_at timestamptz,
    issued_at timestamptz,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_entitlement_issuance PRIMARY KEY (id),
    CONSTRAINT uq_issuance_line UNIQUE (order_id, installment_no, entitlement_type),
    CONSTRAINT ck_issuance_installment
        CHECK (installment_no >= 1),
    CONSTRAINT ck_issuance_type
        CHECK (entitlement_type IN ('RECORDING_SECONDS','AI_SECONDS','EVIDENCE_COUNT','STORAGE_BYTES')),
    CONSTRAINT ck_issuance_status
        CHECK (status IN ('PENDING','ISSUED','FAILED')),
    CONSTRAINT ck_issuance_state_shape
        CHECK ((status = 'ISSUED') = (grant_id IS NOT NULL AND issued_at IS NOT NULL)
            AND (status = 'FAILED') = (last_error_code IS NOT NULL)),
    CONSTRAINT ck_issuance_attempt_count
        CHECK (attempt_count >= 0),
    CONSTRAINT ck_issuance_revision
        CHECK (revision >= 1),
    CONSTRAINT ck_issuance_last_error
        CHECK (last_error_code IS NULL
            OR (length(last_error_code) BETWEEN 1 AND 64 AND btrim(last_error_code) <> '')),
    CONSTRAINT ck_issuance_event_domain
        CHECK (source_event_domain = 'PAYMENT'),
    CONSTRAINT ck_issuance_event_id
        CHECK (length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> ''),
    CONSTRAINT ck_issuance_time_order
        CHECK (issued_at IS NULL OR first_attempt_at IS NULL OR issued_at >= first_attempt_at)
);

CREATE UNIQUE INDEX uq_issuance_grant
    ON entitlement_metering.entitlement_issuance (grant_id)
    WHERE grant_id IS NOT NULL;

CREATE INDEX ix_issuance_order_status
    ON entitlement_metering.entitlement_issuance (order_id, status);

CREATE INDEX ix_issuance_event
    ON entitlement_metering.entitlement_issuance (source_event_domain, source_event_id);

CREATE TABLE entitlement_metering.usage_reservation (
    id uuid NOT NULL,
    call_id text NOT NULL,
    balance_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    authorized_units bigint NOT NULL,
    status text NOT NULL DEFAULT 'HELD',
    authorization_expires_at timestamptz NOT NULL,
    settled_units bigint NOT NULL DEFAULT 0,
    reconciliation_due_at timestamptz,
    reconciliation_attempt_count integer NOT NULL DEFAULT 0,
    last_reconciled_at timestamptz,
    authority_source text,
    last_error_code text,
    created_at timestamptz NOT NULL DEFAULT now(),
    terminal_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_usage_reservation PRIMARY KEY (id),
    CONSTRAINT uq_usage_reservation_call UNIQUE (call_id),
    CONSTRAINT ck_usage_reservation_authorized
        CHECK (authorized_units >= 1),
    CONSTRAINT ck_usage_reservation_status
        CHECK (status IN ('HELD','RECONCILING','SETTLED','RELEASED')),
    CONSTRAINT ck_usage_reservation_settled
        CHECK (settled_units >= 0 AND settled_units <= authorized_units),
    CONSTRAINT ck_usage_reservation_state_shape
        CHECK ((status = 'HELD' AND terminal_at IS NULL AND settled_units = 0
                    AND reconciliation_due_at IS NULL)
            OR (status = 'RECONCILING' AND terminal_at IS NULL AND settled_units = 0
                    AND reconciliation_due_at IS NOT NULL)
            OR (status = 'SETTLED' AND terminal_at IS NOT NULL AND settled_units > 0
                    AND authority_source IS NOT NULL)
            OR (status = 'RELEASED' AND terminal_at IS NOT NULL AND settled_units = 0
                    AND authority_source IS NOT NULL)),
    CONSTRAINT ck_usage_reservation_reconciliation_attempts
        CHECK (reconciliation_attempt_count >= 0),
    CONSTRAINT ck_usage_reservation_authority
        CHECK (authority_source IS NULL OR authority_source IN ('IVR','OPERATIONS')),
    CONSTRAINT ck_usage_reservation_last_error
        CHECK (last_error_code IS NULL
            OR (length(last_error_code) BETWEEN 1 AND 64 AND btrim(last_error_code) <> '')),
    CONSTRAINT ck_usage_reservation_time_order
        CHECK (terminal_at IS NULL OR terminal_at >= created_at),
    CONSTRAINT ck_usage_reservation_revision
        CHECK (revision >= 1)
);

CREATE INDEX ix_usage_reservation_balance_status
    ON entitlement_metering.usage_reservation (balance_id, status);

CREATE INDEX ix_usage_reservation_reconcile
    ON entitlement_metering.usage_reservation (status, reconciliation_due_at);

CREATE TABLE entitlement_metering.usage_fact (
    id uuid NOT NULL,
    reservation_id uuid NOT NULL,
    balance_id uuid NOT NULL,
    call_id text NOT NULL,
    usage_type text NOT NULL,
    units bigint NOT NULL,
    source_event_domain text NOT NULL,
    source_event_id text NOT NULL,
    occurred_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    snapshot jsonb NOT NULL,
    snapshot_sha256 text NOT NULL,
    CONSTRAINT pk_usage_fact PRIMARY KEY (id),
    CONSTRAINT uq_usage_fact_reservation_type UNIQUE (reservation_id, usage_type),
    CONSTRAINT uq_usage_fact_source UNIQUE (source_event_domain, source_event_id),
    CONSTRAINT ck_usage_fact_type
        CHECK (usage_type IN ('RECORDING_SECONDS')),
    CONSTRAINT ck_usage_fact_units
        CHECK (units >= 1),
    CONSTRAINT ck_usage_fact_event_domain
        CHECK (source_event_domain = 'IVR_EVENT'),
    CONSTRAINT ck_usage_fact_event_id
        CHECK (length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> ''),
    CONSTRAINT ck_usage_fact_snapshot_type
        CHECK (jsonb_typeof(snapshot) = 'object'),
    CONSTRAINT ck_usage_fact_snapshot_sha
        CHECK (snapshot_sha256 ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_usage_fact_call
    ON entitlement_metering.usage_fact (call_id);

CREATE FUNCTION entitlement_metering.guard_trial_policy_version() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = entitlement_metering, pg_temp
AS $fn$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'TRIAL_POLICY_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.campaign_key IS DISTINCT FROM OLD.campaign_key
        OR NEW.version_no IS DISTINCT FROM OLD.version_no
        OR NEW.granted_seconds IS DISTINCT FROM OLD.granted_seconds
        OR NEW.valid_days IS DISTINCT FROM OLD.valid_days
        OR NEW.snapshot IS DISTINCT FROM OLD.snapshot
        OR NEW.snapshot_sha256 IS DISTINCT FROM OLD.snapshot_sha256 THEN
        RAISE EXCEPTION 'TRIAL_POLICY_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    IF OLD.published_at IS NULL AND NEW.published_at IS NOT NULL THEN
        IF NEW.published_by_employee_id IS NULL
            OR NEW.enabled IS DISTINCT FROM TRUE
            OR NEW.revision IS DISTINCT FROM OLD.revision + 1 THEN
            RAISE EXCEPTION 'TRIAL_POLICY_PUBLICATION_INVALID' USING ERRCODE = 'P0001';
        END IF;
        RETURN NEW;
    END IF;
    IF OLD.published_at IS NOT NULL THEN
        IF NEW.published_by_employee_id IS DISTINCT FROM OLD.published_by_employee_id
            OR NEW.published_at IS DISTINCT FROM OLD.published_at THEN
            RAISE EXCEPTION 'TRIAL_POLICY_IMMUTABLE' USING ERRCODE = 'P0001';
        END IF;
        RETURN NEW;
    END IF;
    IF NEW.published_at IS NOT NULL THEN
        RAISE EXCEPTION 'TRIAL_POLICY_PUBLICATION_INVALID' USING ERRCODE = 'P0001';
    END IF;
    RAISE EXCEPTION 'TRIAL_POLICY_IMMUTABLE' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE FUNCTION entitlement_metering.guard_entitlement_grant() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = entitlement_metering, pg_temp
AS $fn$
BEGIN
    RAISE EXCEPTION 'ENTITLEMENT_GRANT_IMMUTABLE' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE FUNCTION entitlement_metering.guard_trial_grant() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = entitlement_metering, pg_temp
AS $fn$
BEGIN
    RAISE EXCEPTION 'TRIAL_GRANT_IMMUTABLE' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE FUNCTION entitlement_metering.guard_entitlement_balance() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = entitlement_metering, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.grant_id IS DISTINCT FROM OLD.grant_id
        OR NEW.granted_units IS DISTINCT FROM OLD.granted_units THEN
        RAISE EXCEPTION 'ENTITLEMENT_BALANCE_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION entitlement_metering.guard_entitlement_issuance() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = entitlement_metering, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.order_id IS DISTINCT FROM OLD.order_id
        OR NEW.installment_no IS DISTINCT FROM OLD.installment_no
        OR NEW.entitlement_type IS DISTINCT FROM OLD.entitlement_type
        OR NEW.source_event_domain IS DISTINCT FROM OLD.source_event_domain
        OR NEW.source_event_id IS DISTINCT FROM OLD.source_event_id THEN
        RAISE EXCEPTION 'ENTITLEMENT_ISSUANCE_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION entitlement_metering.guard_usage_reservation() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = entitlement_metering, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.call_id IS DISTINCT FROM OLD.call_id
        OR NEW.balance_id IS DISTINCT FROM OLD.balance_id
        OR NEW.subject_id IS DISTINCT FROM OLD.subject_id
        OR NEW.authorized_units IS DISTINCT FROM OLD.authorized_units
        OR NEW.authorization_expires_at IS DISTINCT FROM OLD.authorization_expires_at
        OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'USAGE_RESERVATION_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION entitlement_metering.reject_row_mutation() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = entitlement_metering, pg_temp
AS $fn$
BEGIN
    RAISE EXCEPTION 'USAGE_FACT_APPEND_ONLY' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE TRIGGER trg_trial_policy_version_guard
BEFORE UPDATE ON entitlement_metering.trial_policy_version
FOR EACH ROW EXECUTE FUNCTION entitlement_metering.guard_trial_policy_version();

CREATE TRIGGER trg_entitlement_grant_immutable
BEFORE UPDATE OR DELETE ON entitlement_metering.entitlement_grant
FOR EACH ROW EXECUTE FUNCTION entitlement_metering.guard_entitlement_grant();

CREATE TRIGGER trg_trial_grant_immutable
BEFORE UPDATE OR DELETE ON entitlement_metering.trial_grant
FOR EACH ROW EXECUTE FUNCTION entitlement_metering.guard_trial_grant();

CREATE TRIGGER trg_entitlement_balance_guard
BEFORE UPDATE ON entitlement_metering.entitlement_balance
FOR EACH ROW EXECUTE FUNCTION entitlement_metering.guard_entitlement_balance();

CREATE TRIGGER trg_entitlement_issuance_guard
BEFORE UPDATE ON entitlement_metering.entitlement_issuance
FOR EACH ROW EXECUTE FUNCTION entitlement_metering.guard_entitlement_issuance();

CREATE TRIGGER trg_usage_reservation_guard
BEFORE UPDATE ON entitlement_metering.usage_reservation
FOR EACH ROW EXECUTE FUNCTION entitlement_metering.guard_usage_reservation();

CREATE TRIGGER trg_usage_fact_immutable
BEFORE UPDATE OR DELETE ON entitlement_metering.usage_fact
FOR EACH ROW EXECUTE FUNCTION entitlement_metering.reject_row_mutation();

REVOKE ALL ON TABLE entitlement_metering.trial_policy_version FROM PUBLIC;
REVOKE ALL ON TABLE entitlement_metering.entitlement_grant FROM PUBLIC;
REVOKE ALL ON TABLE entitlement_metering.trial_grant FROM PUBLIC;
REVOKE ALL ON TABLE entitlement_metering.entitlement_balance FROM PUBLIC;
REVOKE ALL ON TABLE entitlement_metering.entitlement_issuance FROM PUBLIC;
REVOKE ALL ON TABLE entitlement_metering.usage_reservation FROM PUBLIC;
REVOKE ALL ON TABLE entitlement_metering.usage_fact FROM PUBLIC;
REVOKE ALL ON FUNCTION entitlement_metering.issue_first_trial(uuid, text, uuid, text, text, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION entitlement_metering.guard_trial_policy_version() FROM PUBLIC;
REVOKE ALL ON FUNCTION entitlement_metering.guard_entitlement_grant() FROM PUBLIC;
REVOKE ALL ON FUNCTION entitlement_metering.guard_trial_grant() FROM PUBLIC;
REVOKE ALL ON FUNCTION entitlement_metering.guard_entitlement_balance() FROM PUBLIC;
REVOKE ALL ON FUNCTION entitlement_metering.guard_entitlement_issuance() FROM PUBLIC;
REVOKE ALL ON FUNCTION entitlement_metering.guard_usage_reservation() FROM PUBLIC;
REVOKE ALL ON FUNCTION entitlement_metering.reject_row_mutation() FROM PUBLIC;

GRANT USAGE ON SCHEMA entitlement_metering TO catalog_order_role;

GRANT SELECT, INSERT ON TABLE entitlement_metering.trial_policy_version TO entitlement_metering_role;
GRANT UPDATE (published_by_employee_id, enabled, published_at, revision)
    ON TABLE entitlement_metering.trial_policy_version TO entitlement_metering_role;
GRANT SELECT, INSERT ON TABLE entitlement_metering.entitlement_grant TO entitlement_metering_role;
GRANT SELECT ON TABLE entitlement_metering.trial_grant TO entitlement_metering_role;
GRANT SELECT, INSERT ON TABLE entitlement_metering.entitlement_balance TO entitlement_metering_role;
GRANT UPDATE (reserved_units, consumed_units, status, revision, updated_at)
    ON TABLE entitlement_metering.entitlement_balance TO entitlement_metering_role;
GRANT SELECT, INSERT ON TABLE entitlement_metering.entitlement_issuance TO entitlement_metering_role;
GRANT UPDATE (status, attempt_count, last_error_code, grant_id, first_attempt_at, issued_at, updated_at, revision)
    ON TABLE entitlement_metering.entitlement_issuance TO entitlement_metering_role;
GRANT SELECT, INSERT ON TABLE entitlement_metering.usage_reservation TO entitlement_metering_role;
GRANT UPDATE (status, settled_units, reconciliation_due_at, reconciliation_attempt_count,
    last_reconciled_at, authority_source, last_error_code, terminal_at, revision)
    ON TABLE entitlement_metering.usage_reservation TO entitlement_metering_role;
GRANT SELECT, INSERT ON TABLE entitlement_metering.usage_fact TO entitlement_metering_role;

GRANT EXECUTE ON FUNCTION entitlement_metering.guard_trial_policy_version() TO entitlement_metering_role;
GRANT EXECUTE ON FUNCTION entitlement_metering.guard_entitlement_grant() TO entitlement_metering_role;
GRANT EXECUTE ON FUNCTION entitlement_metering.guard_trial_grant() TO entitlement_metering_role;
GRANT EXECUTE ON FUNCTION entitlement_metering.guard_entitlement_balance() TO entitlement_metering_role;
GRANT EXECUTE ON FUNCTION entitlement_metering.guard_entitlement_issuance() TO entitlement_metering_role;
GRANT EXECUTE ON FUNCTION entitlement_metering.guard_usage_reservation() TO entitlement_metering_role;
GRANT EXECUTE ON FUNCTION entitlement_metering.issue_first_trial(uuid, text, uuid, text, text, text)
    TO activation_consent_role, entitlement_metering_role;

GRANT SELECT (id, grant_id, granted_units, reserved_units, consumed_units, status)
    ON TABLE entitlement_metering.entitlement_balance TO ivr_role;
GRANT SELECT (id, subject_id, entitlement_type, granted_units, valid_from, expires_at)
    ON TABLE entitlement_metering.entitlement_grant TO ivr_role;
GRANT SELECT (order_id, installment_no, entitlement_type, status, grant_id, issued_at, last_error_code)
    ON TABLE entitlement_metering.entitlement_issuance TO catalog_order_role;
GRANT SELECT (id, subject_id, entitlement_type, granted_units, valid_from, expires_at)
    ON TABLE entitlement_metering.entitlement_grant TO catalog_order_role;

RESET ROLE;

SET LOCAL ROLE platform_reliability_owner;

GRANT USAGE ON SCHEMA platform TO entitlement_metering_owner;
GRANT EXECUTE ON FUNCTION platform.outbox_append(text, text, text, uuid, text, jsonb, text, text)
    TO entitlement_metering_owner;

RESET ROLE;

SET LOCAL ROLE subject_number_owner;

GRANT USAGE ON SCHEMA subject_number TO entitlement_metering_role;
GRANT SELECT (id, owner_account_id, owner_organization_id, revision)
    ON TABLE subject_number.business_subject TO entitlement_metering_role;

RESET ROLE;

ALTER TABLE entitlement_metering.trial_policy_version
    ADD CONSTRAINT fk_trial_policy_employee
    FOREIGN KEY (published_by_employee_id) REFERENCES operations_iam.employee_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.entitlement_grant
    ADD CONSTRAINT fk_entitlement_grant_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.entitlement_grant
    ADD CONSTRAINT fk_entitlement_grant_order
    FOREIGN KEY (order_id) REFERENCES catalog_order.customer_order(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.trial_grant
    ADD CONSTRAINT fk_trial_grant_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.trial_grant
    ADD CONSTRAINT fk_trial_grant_policy
    FOREIGN KEY (trial_policy_version_id) REFERENCES entitlement_metering.trial_policy_version(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.trial_grant
    ADD CONSTRAINT fk_trial_grant_grant
    FOREIGN KEY (entitlement_grant_id) REFERENCES entitlement_metering.entitlement_grant(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.entitlement_balance
    ADD CONSTRAINT fk_entitlement_balance_grant
    FOREIGN KEY (grant_id) REFERENCES entitlement_metering.entitlement_grant(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.entitlement_issuance
    ADD CONSTRAINT fk_issuance_order
    FOREIGN KEY (order_id) REFERENCES catalog_order.customer_order(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.entitlement_issuance
    ADD CONSTRAINT fk_issuance_grant
    FOREIGN KEY (grant_id) REFERENCES entitlement_metering.entitlement_grant(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.usage_reservation
    ADD CONSTRAINT fk_usage_reservation_call
    FOREIGN KEY (call_id) REFERENCES ivr.call_record(call_id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.usage_reservation
    ADD CONSTRAINT fk_usage_reservation_balance
    FOREIGN KEY (balance_id) REFERENCES entitlement_metering.entitlement_balance(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.usage_reservation
    ADD CONSTRAINT fk_usage_reservation_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.usage_fact
    ADD CONSTRAINT fk_usage_fact_reservation
    FOREIGN KEY (reservation_id) REFERENCES entitlement_metering.usage_reservation(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.usage_fact
    ADD CONSTRAINT fk_usage_fact_balance
    FOREIGN KEY (balance_id) REFERENCES entitlement_metering.entitlement_balance(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE entitlement_metering.usage_fact
    ADD CONSTRAINT fk_usage_fact_call
    FOREIGN KEY (call_id) REFERENCES ivr.call_record(call_id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.ivr_decision
    ADD CONSTRAINT fk_ivr_decision_reservation
    FOREIGN KEY (reservation_id) REFERENCES entitlement_metering.usage_reservation(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
