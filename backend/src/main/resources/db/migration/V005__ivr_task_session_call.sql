-- PA5 V005: IVR task, session and call foundation.
-- Creates the six frozen ivr tables (103 columns, 73 named checks, 14 unique
-- keys, 31 indexes), five local SECURITY INVOKER trigger functions, five
-- triggers, and the exact owner/runtime/cross-module grants. Fifteen ordinary
-- immediate RESTRICT FKs are added by the migration identity after RESET
-- ROLE (fourteen in the FK block plus same-wave late fk_outbound_task_claimed_call).
-- inbound_session.call_id and call_record.call_id are intentional NO_FK roots.
-- ivr_decision.reservation_id is a nullable V005 column with no FK until V006.
-- No schema/role creation, no service function, no V006+ object, no
-- issue_first_trial, no application service.

SET LOCAL ROLE ivr_owner;

CREATE TABLE ivr.outbound_call_task (
    id uuid NOT NULL,
    task_id text NOT NULL,
    caller_binding_id uuid NOT NULL,
    subject_id uuid NOT NULL,
    owner_account_id uuid,
    owner_organization_id uuid,
    created_by_account_id uuid NOT NULL,
    caller_number_hmac text NOT NULL,
    callee_ciphertext text NOT NULL,
    callee_masked text NOT NULL,
    business_type text NOT NULL,
    status text NOT NULL DEFAULT 'PENDING',
    reason_code text,
    ttl_seconds integer NOT NULL DEFAULT 30,
    expires_at timestamptz NOT NULL,
    idempotency_key text NOT NULL,
    request_fingerprint text NOT NULL,
    claimed_call_id text,
    claimed_at timestamptz,
    terminal_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_outbound_call_task PRIMARY KEY (id),
    CONSTRAINT uq_outbound_call_task_task_id UNIQUE (task_id),
    CONSTRAINT uq_outbound_task_idempotency UNIQUE (created_by_account_id, idempotency_key),
    CONSTRAINT ck_outbound_task_task_id
        CHECK (length(task_id) BETWEEN 1 AND 64 AND btrim(task_id) <> ''),
    CONSTRAINT ck_outbound_task_single_owner
        CHECK (num_nonnulls(owner_account_id, owner_organization_id) = 1),
    CONSTRAINT ck_outbound_task_caller_hmac
        CHECK (caller_number_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_outbound_task_callee_ciphertext
        CHECK (callee_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_outbound_task_callee_masked
        CHECK (length(callee_masked) BETWEEN 1 AND 64 AND btrim(callee_masked) <> ''),
    CONSTRAINT ck_outbound_task_business_type
        CHECK (length(business_type) BETWEEN 1 AND 64 AND btrim(business_type) <> ''),
    CONSTRAINT ck_outbound_task_status
        CHECK (status IN ('PENDING','CLAIMED','REPLACED','CANCELLED','EXPIRED','REJECTED','COMPLETED','FAILED')),
    CONSTRAINT ck_outbound_task_reason
        CHECK (reason_code IS NULL OR (length(reason_code) BETWEEN 1 AND 64 AND btrim(reason_code) <> '')),
    CONSTRAINT ck_outbound_task_ttl
        CHECK (ttl_seconds >= 1),
    CONSTRAINT ck_outbound_task_expiry
        CHECK (expires_at > created_at),
    CONSTRAINT ck_outbound_task_idempotency_key
        CHECK (length(idempotency_key) BETWEEN 1 AND 512 AND btrim(idempotency_key) <> ''),
    CONSTRAINT ck_outbound_task_fingerprint
        CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_outbound_task_claimed_shape
        CHECK ((claimed_call_id IS NULL) = (claimed_at IS NULL)),
    CONSTRAINT ck_outbound_task_terminal_shape
        CHECK ((status IN ('PENDING','CLAIMED')) = (terminal_at IS NULL)),
    CONSTRAINT ck_outbound_task_time_order
        CHECK (updated_at >= created_at AND (terminal_at IS NULL OR terminal_at >= created_at)),
    CONSTRAINT ck_outbound_task_revision
        CHECK (revision >= 1)
);

CREATE UNIQUE INDEX uq_outbound_task_live_caller
    ON ivr.outbound_call_task (caller_number_hmac)
    WHERE status IN ('PENDING','CLAIMED');

CREATE UNIQUE INDEX uq_outbound_task_claimed_call
    ON ivr.outbound_call_task (claimed_call_id)
    WHERE claimed_call_id IS NOT NULL;

CREATE INDEX ix_outbound_task_subject_status
    ON ivr.outbound_call_task (subject_id, status, created_at DESC);

CREATE INDEX ix_outbound_task_org_status
    ON ivr.outbound_call_task (owner_organization_id, status, created_at DESC);

CREATE INDEX ix_outbound_task_creator_status
    ON ivr.outbound_call_task (created_by_account_id, status);

CREATE TABLE ivr.inbound_session (
    id uuid NOT NULL,
    call_id text NOT NULL,
    subject_id uuid,
    trusted_caller boolean NOT NULL,
    caller_ciphertext text,
    caller_number_hmac text,
    caller_masked text,
    access_number_masked text NOT NULL,
    node_id text NOT NULL,
    protocol_version text NOT NULL,
    prompt_version text NOT NULL,
    status text NOT NULL DEFAULT 'RECEIVED',
    confirmed_subject_id uuid,
    guidance_status text,
    created_at timestamptz NOT NULL DEFAULT now(),
    confirmed_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_inbound_session PRIMARY KEY (id),
    CONSTRAINT uq_inbound_session_call UNIQUE (call_id),
    CONSTRAINT ck_inbound_session_call_id
        CHECK (length(call_id) BETWEEN 1 AND 128 AND btrim(call_id) <> ''),
    CONSTRAINT ck_inbound_session_caller_shape
        CHECK (trusted_caller = FALSE OR (caller_ciphertext IS NOT NULL AND caller_number_hmac IS NOT NULL AND caller_masked IS NOT NULL)),
    CONSTRAINT ck_inbound_session_caller_ciphertext
        CHECK (caller_ciphertext IS NULL OR caller_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_inbound_session_caller_hmac
        CHECK (caller_number_hmac IS NULL OR caller_number_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_inbound_session_caller_masked
        CHECK (caller_masked IS NULL OR (length(caller_masked) BETWEEN 1 AND 64 AND btrim(caller_masked) <> '')),
    CONSTRAINT ck_inbound_session_access_masked
        CHECK (length(access_number_masked) BETWEEN 1 AND 64 AND btrim(access_number_masked) <> ''),
    CONSTRAINT ck_inbound_session_node
        CHECK (length(node_id) BETWEEN 1 AND 64 AND btrim(node_id) <> ''),
    CONSTRAINT ck_inbound_session_protocol
        CHECK (length(protocol_version) BETWEEN 1 AND 32 AND btrim(protocol_version) <> ''),
    CONSTRAINT ck_inbound_session_prompt
        CHECK (length(prompt_version) BETWEEN 1 AND 64 AND btrim(prompt_version) <> ''),
    CONSTRAINT ck_inbound_session_status
        CHECK (status IN ('RECEIVED','CONFIRMED','ABANDONED','REJECTED')),
    CONSTRAINT ck_inbound_session_guidance
        CHECK (guidance_status IS NULL OR guidance_status IN ('ACTIVATION_REQUIRED','CONSENT_REQUIRED')),
    CONSTRAINT ck_inbound_session_confirmed_shape
        CHECK (((status = 'CONFIRMED') = (confirmed_at IS NOT NULL)) AND (status <> 'CONFIRMED' OR (trusted_caller = TRUE AND confirmed_subject_id IS NOT NULL))),
    CONSTRAINT ck_inbound_session_time_order
        CHECK (confirmed_at IS NULL OR confirmed_at >= created_at),
    CONSTRAINT ck_inbound_session_revision
        CHECK (revision >= 1)
);

CREATE INDEX ix_inbound_session_hmac_status
    ON ivr.inbound_session (caller_number_hmac, status, created_at DESC);

CREATE INDEX ix_inbound_session_subject_time
    ON ivr.inbound_session (subject_id, created_at DESC);

CREATE TABLE ivr.call_record (
    id uuid NOT NULL,
    call_id text NOT NULL,
    session_id uuid NOT NULL,
    task_id uuid,
    subject_id uuid,
    caller_ciphertext text NOT NULL,
    caller_number_hmac text NOT NULL,
    caller_masked text NOT NULL,
    callee_ciphertext text,
    callee_masked text,
    access_number_masked text NOT NULL,
    status text NOT NULL DEFAULT 'ACTIVE',
    node_id text NOT NULL,
    protocol_version text NOT NULL,
    started_at timestamptz,
    bridged_at timestamptz,
    recording_started_at timestamptz,
    recording_ended_at timestamptz,
    ended_at timestamptz,
    authoritative_seconds integer,
    end_reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_call_record PRIMARY KEY (id),
    CONSTRAINT uq_call_record_call UNIQUE (call_id),
    CONSTRAINT uq_call_record_session UNIQUE (session_id),
    CONSTRAINT ck_call_record_call_id
        CHECK (length(call_id) BETWEEN 1 AND 128 AND btrim(call_id) <> ''),
    CONSTRAINT ck_call_record_caller_ciphertext
        CHECK (caller_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_call_record_caller_hmac
        CHECK (caller_number_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_call_record_caller_masked
        CHECK (length(caller_masked) BETWEEN 1 AND 64 AND btrim(caller_masked) <> ''),
    CONSTRAINT ck_call_record_callee_ciphertext
        CHECK (callee_ciphertext IS NULL OR callee_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_call_record_callee_masked
        CHECK (callee_masked IS NULL OR (length(callee_masked) BETWEEN 1 AND 64 AND btrim(callee_masked) <> '')),
    CONSTRAINT ck_call_record_access_masked
        CHECK (length(access_number_masked) BETWEEN 1 AND 64 AND btrim(access_number_masked) <> ''),
    CONSTRAINT ck_call_record_status
        CHECK (status IN ('ACTIVE','ENDED','FAILED','UNKNOWN','REJECTED')),
    CONSTRAINT ck_call_record_node
        CHECK (length(node_id) BETWEEN 1 AND 64 AND btrim(node_id) <> ''),
    CONSTRAINT ck_call_record_protocol
        CHECK (length(protocol_version) BETWEEN 1 AND 32 AND btrim(protocol_version) <> ''),
    CONSTRAINT ck_call_record_terminal_shape
        CHECK ((status IN ('ACTIVE','UNKNOWN')) = (ended_at IS NULL)),
    CONSTRAINT ck_call_record_time_order
        CHECK ((started_at IS NULL OR ended_at IS NULL OR ended_at >= started_at)
            AND (started_at IS NULL OR bridged_at IS NULL OR bridged_at >= started_at)
            AND (started_at IS NULL OR recording_started_at IS NULL OR recording_started_at >= started_at)
            AND (recording_started_at IS NULL OR recording_ended_at IS NULL OR recording_ended_at >= recording_started_at)),
    CONSTRAINT ck_call_record_authoritative_seconds
        CHECK (authoritative_seconds IS NULL OR (authoritative_seconds >= 0 AND status IN ('ENDED','FAILED'))),
    CONSTRAINT ck_call_record_end_reason
        CHECK (end_reason IS NULL OR (length(end_reason) BETWEEN 1 AND 64 AND btrim(end_reason) <> '')),
    CONSTRAINT ck_call_record_revision
        CHECK (revision >= 1)
);

CREATE UNIQUE INDEX uq_call_record_task
    ON ivr.call_record (task_id)
    WHERE task_id IS NOT NULL;

CREATE UNIQUE INDEX uq_call_record_active_caller
    ON ivr.call_record (caller_number_hmac)
    WHERE status IN ('ACTIVE','UNKNOWN');

CREATE INDEX ix_call_record_subject_time
    ON ivr.call_record (subject_id, ended_at, created_at);

CREATE INDEX ix_call_record_task
    ON ivr.call_record (task_id);

CREATE TABLE ivr.call_leg (
    id uuid NOT NULL,
    call_id text NOT NULL,
    leg_id text NOT NULL,
    leg_type text NOT NULL,
    direction text NOT NULL,
    state text NOT NULL,
    started_at timestamptz NOT NULL,
    answered_at timestamptz,
    ended_at timestamptz,
    end_reason text,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_call_leg PRIMARY KEY (id),
    CONSTRAINT uq_call_leg_id UNIQUE (call_id, leg_id),
    CONSTRAINT ck_call_leg_leg_id
        CHECK (length(leg_id) BETWEEN 1 AND 64 AND btrim(leg_id) <> ''),
    CONSTRAINT ck_call_leg_type
        CHECK (leg_type IN ('CALLER','CALLEE')),
    CONSTRAINT ck_call_leg_direction
        CHECK ((leg_type = 'CALLER' AND direction = 'INBOUND') OR (leg_type = 'CALLEE' AND direction = 'OUTBOUND')),
    CONSTRAINT ck_call_leg_state
        CHECK (state IN ('RINGING','ANSWERED','ENDED','FAILED')),
    CONSTRAINT ck_call_leg_time_order
        CHECK ((answered_at IS NULL OR answered_at >= started_at)
            AND (ended_at IS NULL OR ended_at >= started_at)
            AND (answered_at IS NULL OR ended_at IS NULL OR ended_at >= answered_at)),
    CONSTRAINT ck_call_leg_end_reason
        CHECK (end_reason IS NULL OR (length(end_reason) BETWEEN 1 AND 64 AND btrim(end_reason) <> '')),
    CONSTRAINT ck_call_leg_revision
        CHECK (revision >= 1)
);

CREATE INDEX ix_call_leg_call_type
    ON ivr.call_leg (call_id, leg_type);

CREATE TABLE ivr.call_event (
    id uuid NOT NULL,
    call_id text NOT NULL,
    event_domain text NOT NULL DEFAULT 'IVR_EVENT',
    event_id text NOT NULL,
    sequence_no integer,
    event_type text NOT NULL,
    occurred_at timestamptz NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    payload_ciphertext text NOT NULL,
    payload_redacted jsonb NOT NULL,
    payload_sha256 text NOT NULL,
    processing_status text NOT NULL DEFAULT 'RECEIVED',
    reason_code text,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_call_event PRIMARY KEY (id),
    CONSTRAINT uq_call_event_domain_id UNIQUE (event_domain, event_id),
    CONSTRAINT ck_call_event_domain
        CHECK (event_domain = 'IVR_EVENT'),
    CONSTRAINT ck_call_event_event_id
        CHECK (length(event_id) BETWEEN 1 AND 128 AND btrim(event_id) <> ''),
    CONSTRAINT ck_call_event_sequence
        CHECK (sequence_no IS NULL OR sequence_no >= 1),
    CONSTRAINT ck_call_event_type
        CHECK (length(event_type) BETWEEN 1 AND 64 AND btrim(event_type) <> ''),
    CONSTRAINT ck_call_event_payload_ciphertext
        CHECK (payload_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_call_event_payload_redacted
        CHECK (jsonb_typeof(payload_redacted) = 'object'
            AND payload_redacted ? 'source'
            AND payload_redacted ? 'receipt'
            AND jsonb_typeof(payload_redacted->'source') = 'object'
            AND jsonb_typeof(payload_redacted->'receipt') = 'object'
            AND ((payload_redacted - 'source') - 'receipt') = '{}'::jsonb
            AND ((((payload_redacted->'source') - 'eventType') - 'legId') - 'sequenceNo' - 'payload') = '{}'::jsonb
            AND (((payload_redacted->'receipt') - 'payloadSha256') - 'eventFingerprint') = '{}'::jsonb
            AND (payload_redacted->'source') ? 'payload'
            AND jsonb_typeof((payload_redacted->'source')->'payload') = 'object'
            AND (payload_redacted->'receipt') ? 'payloadSha256'
            AND jsonb_typeof((payload_redacted->'receipt')->'payloadSha256') = 'string'
            AND (payload_redacted->'receipt') ? 'eventFingerprint'
            AND ((payload_redacted->'receipt')->>'payloadSha256') = payload_sha256),
    CONSTRAINT ck_call_event_payload_sha
        CHECK (payload_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_call_event_processing_status
        CHECK (processing_status IN ('RECEIVED','APPLIED','IGNORED','QUARANTINED')),
    CONSTRAINT ck_call_event_reason
        CHECK (reason_code IS NULL OR (length(reason_code) BETWEEN 1 AND 64 AND btrim(reason_code) <> '')),
    CONSTRAINT ck_call_event_revision
        CHECK (revision >= 1)
);

CREATE UNIQUE INDEX uq_call_event_sequence
    ON ivr.call_event (call_id, sequence_no)
    WHERE sequence_no IS NOT NULL;

CREATE INDEX ix_call_event_call_time
    ON ivr.call_event (call_id, occurred_at);

CREATE INDEX ix_call_event_processing
    ON ivr.call_event (processing_status, received_at);

CREATE TABLE ivr.ivr_decision (
    id uuid NOT NULL,
    session_id uuid NOT NULL,
    call_id text NOT NULL,
    purpose text NOT NULL,
    source_event_domain text,
    source_event_id text,
    request_fingerprint text NOT NULL,
    action text,
    reason_code text NOT NULL,
    response_snapshot jsonb NOT NULL,
    response_payload_ciphertext text NOT NULL,
    response_sha256 text NOT NULL,
    key_version integer NOT NULL,
    reservation_id uuid,
    decided_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_ivr_decision PRIMARY KEY (id),
    CONSTRAINT uq_ivr_decision_call_purpose UNIQUE (call_id, purpose),
    CONSTRAINT ck_ivr_decision_purpose
        CHECK (purpose IN ('INBOUND_RESOLVE','ACTIVATION_NEXT_ACTION','DIRECT_AUTHORIZATION')),
    CONSTRAINT ck_ivr_decision_source
        CHECK ((purpose = 'INBOUND_RESOLVE' AND source_event_domain IS NULL AND source_event_id IS NULL)
            OR (purpose = 'ACTIVATION_NEXT_ACTION' AND source_event_domain IS NOT NULL AND source_event_id IS NOT NULL AND source_event_domain = 'IVR_CONFIRM' AND length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> '')
            OR (purpose = 'DIRECT_AUTHORIZATION' AND source_event_domain IS NOT NULL AND source_event_id IS NOT NULL AND source_event_domain = 'IVR_DIRECT_AUTH' AND length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> '')),
    CONSTRAINT ck_ivr_decision_fingerprint
        CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_ivr_decision_action
        CHECK (action IS NULL OR action IN ('GUIDE_ACTIVATION','DIAL_TASK_CALLEE','ENTER_CALLEE','GUIDE_PURCHASE','REJECT','REJECT_SUSPENDED','REJECT_CALL_IN_PROGRESS')),
    CONSTRAINT ck_ivr_decision_reason
        CHECK (reason_code IN ('ACTIVATION_REQUIRED','CONSENT_REQUIRED','CALLER_UNTRUSTED','SESSION_IDENTITY_MISMATCH','CALL_IN_PROGRESS','CONFIRMATION_NOT_ALLOWED','ACTIVATION_SUSPENDED','CALLER_BINDING_REVOKED','WORKSPACE_ACCESS_REVOKED','TASK_ACCESS_REVOKED','ORGANIZATION_INACTIVE','ACCOUNT_DISABLED','CALL_STATE_UNRESOLVED','TASK_CONFLICT','TASK_CLAIMED','NO_AVAILABLE_ENTITLEMENT','NO_VALID_TASK','TASK_EXPIRED','TASK_UNAVAILABLE','DIRECT_AUTHORIZED','DATA_INTEGRITY')),
    CONSTRAINT ck_ivr_decision_direct_reason
        CHECK ((action IS NULL) = (reason_code = 'DIRECT_AUTHORIZED')),
    CONSTRAINT ck_ivr_decision_shape
        CHECK ((purpose IN ('INBOUND_RESOLVE','ACTIVATION_NEXT_ACTION') AND action IS NOT NULL AND ((action = 'DIAL_TASK_CALLEE' AND reservation_id IS NOT NULL) OR (action <> 'DIAL_TASK_CALLEE' AND reservation_id IS NULL)))
            OR (purpose = 'DIRECT_AUTHORIZATION' AND ((action IS NULL AND reservation_id IS NOT NULL) OR (action IS NOT NULL AND action IN ('GUIDE_PURCHASE','REJECT','REJECT_SUSPENDED','REJECT_CALL_IN_PROGRESS') AND reservation_id IS NULL)))),
    CONSTRAINT ck_ivr_decision_snapshot
        CHECK (jsonb_typeof(response_snapshot) = 'object'),
    CONSTRAINT ck_ivr_decision_payload
        CHECK (response_payload_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_ivr_decision_sha
        CHECK (response_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_ivr_decision_key_version
        CHECK (key_version >= 1)
);

CREATE UNIQUE INDEX uq_ivr_decision_source
    ON ivr.ivr_decision (call_id, source_event_domain, source_event_id)
    WHERE source_event_id IS NOT NULL;

CREATE INDEX ix_ivr_decision_call_time
    ON ivr.ivr_decision (call_id, decided_at);

CREATE FUNCTION ivr.reject_row_mutation() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ivr, pg_temp
AS $fn$
BEGIN
    RAISE EXCEPTION 'IVR_DECISION_APPEND_ONLY' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE FUNCTION ivr.guard_outbound_call_task() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ivr, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.task_id IS DISTINCT FROM OLD.task_id
        OR NEW.caller_binding_id IS DISTINCT FROM OLD.caller_binding_id
        OR NEW.subject_id IS DISTINCT FROM OLD.subject_id
        OR NEW.owner_account_id IS DISTINCT FROM OLD.owner_account_id
        OR NEW.owner_organization_id IS DISTINCT FROM OLD.owner_organization_id
        OR NEW.created_by_account_id IS DISTINCT FROM OLD.created_by_account_id
        OR NEW.caller_number_hmac IS DISTINCT FROM OLD.caller_number_hmac
        OR NEW.callee_ciphertext IS DISTINCT FROM OLD.callee_ciphertext
        OR NEW.callee_masked IS DISTINCT FROM OLD.callee_masked
        OR NEW.business_type IS DISTINCT FROM OLD.business_type
        OR NEW.ttl_seconds IS DISTINCT FROM OLD.ttl_seconds
        OR NEW.expires_at IS DISTINCT FROM OLD.expires_at
        OR NEW.idempotency_key IS DISTINCT FROM OLD.idempotency_key
        OR NEW.request_fingerprint IS DISTINCT FROM OLD.request_fingerprint
        OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'OUTBOUND_CALL_TASK_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    IF OLD.status NOT IN ('PENDING','CLAIMED') AND NEW.status IN ('PENDING','CLAIMED') THEN
        RAISE EXCEPTION 'OUTBOUND_CALL_TASK_TERMINAL' USING ERRCODE = 'P0001';
    END IF;
    IF NEW.claimed_call_id IS DISTINCT FROM OLD.claimed_call_id THEN
        IF OLD.claimed_call_id IS NOT NULL OR NEW.claimed_call_id IS NULL THEN
            RAISE EXCEPTION 'OUTBOUND_CALL_TASK_CLAIM_IMMUTABLE' USING ERRCODE = 'P0001';
        END IF;
        IF NOT EXISTS (SELECT 1 FROM ivr.call_record c WHERE c.call_id = NEW.claimed_call_id) THEN
            RAISE EXCEPTION 'OUTBOUND_CALL_TASK_CLAIM_INVALID' USING ERRCODE = 'P0001';
        END IF;
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION ivr.guard_inbound_session() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ivr, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.call_id IS DISTINCT FROM OLD.call_id
        OR NEW.subject_id IS DISTINCT FROM OLD.subject_id
        OR NEW.trusted_caller IS DISTINCT FROM OLD.trusted_caller
        OR NEW.caller_ciphertext IS DISTINCT FROM OLD.caller_ciphertext
        OR NEW.caller_number_hmac IS DISTINCT FROM OLD.caller_number_hmac
        OR NEW.caller_masked IS DISTINCT FROM OLD.caller_masked
        OR NEW.access_number_masked IS DISTINCT FROM OLD.access_number_masked
        OR NEW.node_id IS DISTINCT FROM OLD.node_id
        OR NEW.protocol_version IS DISTINCT FROM OLD.protocol_version
        OR NEW.prompt_version IS DISTINCT FROM OLD.prompt_version
        OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'INBOUND_SESSION_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION ivr.guard_call_record_parties() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ivr, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.call_id IS DISTINCT FROM OLD.call_id
        OR NEW.session_id IS DISTINCT FROM OLD.session_id
        OR NEW.caller_ciphertext IS DISTINCT FROM OLD.caller_ciphertext
        OR NEW.caller_number_hmac IS DISTINCT FROM OLD.caller_number_hmac
        OR NEW.caller_masked IS DISTINCT FROM OLD.caller_masked
        OR NEW.callee_ciphertext IS DISTINCT FROM OLD.callee_ciphertext
        OR NEW.callee_masked IS DISTINCT FROM OLD.callee_masked
        OR NEW.access_number_masked IS DISTINCT FROM OLD.access_number_masked
        OR NEW.node_id IS DISTINCT FROM OLD.node_id
        OR NEW.protocol_version IS DISTINCT FROM OLD.protocol_version
        OR NEW.started_at IS DISTINCT FROM OLD.started_at THEN
        RAISE EXCEPTION 'CALL_RECORD_PARTIES_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    IF NEW.task_id IS DISTINCT FROM OLD.task_id THEN
        IF OLD.task_id IS NOT NULL OR NEW.task_id IS NULL
            OR NOT EXISTS (
                SELECT 1 FROM ivr.outbound_call_task t
                WHERE t.id = NEW.task_id AND t.claimed_call_id = NEW.call_id
            ) THEN
            RAISE EXCEPTION 'CALL_RECORD_PARTIES_IMMUTABLE' USING ERRCODE = 'P0001';
        END IF;
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION ivr.guard_call_leg() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = ivr, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.call_id IS DISTINCT FROM OLD.call_id
        OR NEW.leg_id IS DISTINCT FROM OLD.leg_id
        OR NEW.leg_type IS DISTINCT FROM OLD.leg_type
        OR NEW.direction IS DISTINCT FROM OLD.direction
        OR NEW.started_at IS DISTINCT FROM OLD.started_at THEN
        RAISE EXCEPTION 'CALL_LEG_IDENTITY_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE TRIGGER trg_outbound_call_task_guard
BEFORE UPDATE ON ivr.outbound_call_task
FOR EACH ROW EXECUTE FUNCTION ivr.guard_outbound_call_task();

CREATE TRIGGER trg_inbound_session_guard
BEFORE UPDATE ON ivr.inbound_session
FOR EACH ROW EXECUTE FUNCTION ivr.guard_inbound_session();

CREATE TRIGGER trg_call_record_guard
BEFORE UPDATE ON ivr.call_record
FOR EACH ROW EXECUTE FUNCTION ivr.guard_call_record_parties();

CREATE TRIGGER trg_call_leg_guard
BEFORE UPDATE ON ivr.call_leg
FOR EACH ROW EXECUTE FUNCTION ivr.guard_call_leg();

CREATE TRIGGER trg_ivr_decision_immutable
BEFORE UPDATE OR DELETE ON ivr.ivr_decision
FOR EACH ROW EXECUTE FUNCTION ivr.reject_row_mutation();

REVOKE ALL ON TABLE ivr.outbound_call_task FROM PUBLIC;
REVOKE ALL ON TABLE ivr.inbound_session FROM PUBLIC;
REVOKE ALL ON TABLE ivr.call_record FROM PUBLIC;
REVOKE ALL ON TABLE ivr.call_leg FROM PUBLIC;
REVOKE ALL ON TABLE ivr.call_event FROM PUBLIC;
REVOKE ALL ON TABLE ivr.ivr_decision FROM PUBLIC;
REVOKE ALL ON FUNCTION ivr.reject_row_mutation() FROM PUBLIC;
REVOKE ALL ON FUNCTION ivr.guard_outbound_call_task() FROM PUBLIC;
REVOKE ALL ON FUNCTION ivr.guard_inbound_session() FROM PUBLIC;
REVOKE ALL ON FUNCTION ivr.guard_call_record_parties() FROM PUBLIC;
REVOKE ALL ON FUNCTION ivr.guard_call_leg() FROM PUBLIC;

GRANT USAGE ON SCHEMA ivr TO ivr_role;

GRANT SELECT, INSERT ON TABLE ivr.outbound_call_task TO ivr_role;
GRANT UPDATE (status, reason_code, claimed_call_id, claimed_at, terminal_at, updated_at, revision)
    ON TABLE ivr.outbound_call_task TO ivr_role;
GRANT SELECT, INSERT ON TABLE ivr.inbound_session TO ivr_role;
GRANT UPDATE (status, confirmed_subject_id, guidance_status, confirmed_at, revision)
    ON TABLE ivr.inbound_session TO ivr_role;
GRANT SELECT, INSERT ON TABLE ivr.call_record TO ivr_role;
GRANT UPDATE (task_id, status, started_at, bridged_at, recording_started_at, recording_ended_at, ended_at, authoritative_seconds, end_reason, revision)
    ON TABLE ivr.call_record TO ivr_role;
GRANT SELECT, INSERT ON TABLE ivr.call_leg TO ivr_role;
GRANT UPDATE (state, answered_at, ended_at, end_reason, revision)
    ON TABLE ivr.call_leg TO ivr_role;
GRANT SELECT, INSERT ON TABLE ivr.call_event TO ivr_role;
GRANT UPDATE (processing_status, reason_code, revision)
    ON TABLE ivr.call_event TO ivr_role;
GRANT SELECT, INSERT ON TABLE ivr.ivr_decision TO ivr_role;

GRANT EXECUTE ON FUNCTION ivr.guard_outbound_call_task() TO ivr_role;
GRANT EXECUTE ON FUNCTION ivr.guard_inbound_session() TO ivr_role;
GRANT EXECUTE ON FUNCTION ivr.guard_call_record_parties() TO ivr_role;
GRANT EXECUTE ON FUNCTION ivr.guard_call_leg() TO ivr_role;

RESET ROLE;

SET LOCAL ROLE customer_iam_owner;

GRANT USAGE ON SCHEMA customer_iam TO ivr_role;
GRANT SELECT (id, status) ON TABLE customer_iam.customer_account TO ivr_role;

RESET ROLE;

SET LOCAL ROLE organization_access_owner;

GRANT USAGE ON SCHEMA organization_access TO ivr_role;
GRANT SELECT (id, status) ON TABLE organization_access.organization TO ivr_role;
GRANT SELECT (organization_id, account_id, role, status)
    ON TABLE organization_access.organization_membership TO ivr_role;

RESET ROLE;

SET LOCAL ROLE subject_number_owner;

GRANT USAGE ON SCHEMA subject_number TO ivr_role;
GRANT SELECT (id, owner_account_id, owner_organization_id, revision)
    ON TABLE subject_number.business_subject TO ivr_role;
GRANT SELECT (id, subject_id, number_hmac, status, verified_at, revision)
    ON TABLE subject_number.caller_number_binding TO ivr_role;
GRANT SELECT (caller_number_binding_id, risk_type, state, revision)
    ON TABLE subject_number.subject_risk_state TO ivr_role;

RESET ROLE;

ALTER TABLE ivr.outbound_call_task
    ADD CONSTRAINT fk_outbound_task_binding
    FOREIGN KEY (caller_binding_id) REFERENCES subject_number.caller_number_binding(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.outbound_call_task
    ADD CONSTRAINT fk_outbound_task_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.outbound_call_task
    ADD CONSTRAINT fk_outbound_task_owner_account
    FOREIGN KEY (owner_account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.outbound_call_task
    ADD CONSTRAINT fk_outbound_task_owner_org
    FOREIGN KEY (owner_organization_id) REFERENCES organization_access.organization(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.outbound_call_task
    ADD CONSTRAINT fk_outbound_task_creator
    FOREIGN KEY (created_by_account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.inbound_session
    ADD CONSTRAINT fk_inbound_session_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.inbound_session
    ADD CONSTRAINT fk_inbound_session_confirmed_subject
    FOREIGN KEY (confirmed_subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.call_record
    ADD CONSTRAINT fk_call_record_session
    FOREIGN KEY (session_id) REFERENCES ivr.inbound_session(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.call_record
    ADD CONSTRAINT fk_call_record_task
    FOREIGN KEY (task_id) REFERENCES ivr.outbound_call_task(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.call_record
    ADD CONSTRAINT fk_call_record_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.call_leg
    ADD CONSTRAINT fk_call_leg_call
    FOREIGN KEY (call_id) REFERENCES ivr.call_record(call_id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.call_event
    ADD CONSTRAINT fk_call_event_call
    FOREIGN KEY (call_id) REFERENCES ivr.call_record(call_id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.ivr_decision
    ADD CONSTRAINT fk_ivr_decision_session
    FOREIGN KEY (session_id) REFERENCES ivr.inbound_session(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.ivr_decision
    ADD CONSTRAINT fk_ivr_decision_call
    FOREIGN KEY (call_id) REFERENCES ivr.call_record(call_id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ivr.outbound_call_task
    ADD CONSTRAINT fk_outbound_task_claimed_call
    FOREIGN KEY (claimed_call_id) REFERENCES ivr.call_record(call_id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
