REVOKE ALL ON SCHEMA public FROM PUBLIC;

CREATE ROLE platform_reliability_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE platform_reliability_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE platform_dispatcher_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE audit_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE audit_reader_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE customer_iam_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE customer_iam_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE organization_access_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE organization_access_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE operations_iam_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE operations_iam_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE subject_number_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE subject_number_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE subject_identity_verification_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE operations_service_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;

CREATE SCHEMA platform AUTHORIZATION platform_reliability_owner;
CREATE SCHEMA audit AUTHORIZATION audit_owner;
CREATE SCHEMA customer_iam AUTHORIZATION customer_iam_owner;
CREATE SCHEMA organization_access AUTHORIZATION organization_access_owner;
CREATE SCHEMA operations_iam AUTHORIZATION operations_iam_owner;
CREATE SCHEMA subject_number AUTHORIZATION subject_number_owner;

REVOKE ALL ON SCHEMA platform, audit, customer_iam, organization_access, operations_iam, subject_number FROM PUBLIC;

SET LOCAL ROLE platform_reliability_owner;

CREATE TABLE platform.idempotency_record (
    id uuid NOT NULL,
    scope text NOT NULL,
    idempotency_key text NOT NULL,
    request_fingerprint text NOT NULL,
    status text NOT NULL DEFAULT 'NEW',
    result_type text,
    result_id uuid,
    result_snapshot jsonb,
    result_sha256 text,
    created_at timestamptz NOT NULL DEFAULT now(),
    completed_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_idempotency_record PRIMARY KEY (id),
    CONSTRAINT uq_idempotency_scope_key UNIQUE (scope, idempotency_key),
    CONSTRAINT ck_idempotency_scope CHECK (scope IN ('IVR_CONFIRM','PURCHASE')),
    CONSTRAINT ck_idempotency_key CHECK (length(idempotency_key) BETWEEN 1 AND 512 AND btrim(idempotency_key) <> ''),
    CONSTRAINT ck_idempotency_fingerprint CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_idempotency_status CHECK (status IN ('NEW','COMPLETED')),
    CONSTRAINT ck_idempotency_result_type CHECK (result_type IS NULL OR (length(result_type) BETWEEN 1 AND 64 AND btrim(result_type) <> '')),
    CONSTRAINT ck_idempotency_result_snapshot_type CHECK (result_snapshot IS NULL OR jsonb_typeof(result_snapshot) IN ('object','array')),
    CONSTRAINT ck_idempotency_result_sha256 CHECK (result_sha256 IS NULL OR result_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_idempotency_revision CHECK (revision >= 1),
    CONSTRAINT ck_idempotency_state_shape CHECK (
        (status = 'NEW' AND result_type IS NULL AND result_id IS NULL AND result_snapshot IS NULL
            AND result_sha256 IS NULL AND completed_at IS NULL AND revision = 1)
        OR
        (status = 'COMPLETED' AND result_type IS NOT NULL AND result_id IS NOT NULL
            AND result_snapshot IS NOT NULL AND result_sha256 IS NOT NULL AND completed_at IS NOT NULL
            AND revision = 2)
    ),
    CONSTRAINT ck_idempotency_completed_after_created CHECK (completed_at IS NULL OR completed_at >= created_at)
);

CREATE INDEX ix_idempotency_new_alert ON platform.idempotency_record (created_at) WHERE status = 'NEW';

CREATE TABLE platform.outbox_event (
    id uuid NOT NULL,
    event_domain text NOT NULL,
    dedupe_key text NOT NULL,
    aggregate_type text NOT NULL,
    aggregate_id uuid NOT NULL,
    event_type text NOT NULL,
    payload_redacted jsonb,
    payload_ciphertext text,
    payload_sha256 text NOT NULL,
    status text NOT NULL DEFAULT 'PENDING',
    attempt_count integer NOT NULL DEFAULT 0,
    available_at timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_outbox_event PRIMARY KEY (id),
    CONSTRAINT uq_outbox_domain_dedupe UNIQUE (event_domain, dedupe_key),
    CONSTRAINT ck_outbox_event_domain CHECK (event_domain IN ('ACTIVATION','IVR_CONFIRM','ACTIVATION_STATUS','OPERATIONS','IVR_EVENT','IVR_DIRECT_AUTH','EVIDENCE','PAYMENT','SHARE_ACCESS','PURCHASE','SUBJECT_CLAIM')),
    CONSTRAINT ck_outbox_dedupe_key CHECK (length(dedupe_key) BETWEEN 1 AND 512 AND btrim(dedupe_key) <> ''),
    CONSTRAINT ck_outbox_aggregate_type CHECK (length(aggregate_type) BETWEEN 1 AND 128 AND btrim(aggregate_type) <> ''),
    CONSTRAINT ck_outbox_event_type CHECK (length(event_type) BETWEEN 1 AND 128 AND btrim(event_type) <> ''),
    CONSTRAINT ck_outbox_payload_present CHECK (payload_redacted IS NOT NULL OR payload_ciphertext IS NOT NULL),
    CONSTRAINT ck_outbox_ciphertext_shape CHECK (payload_ciphertext IS NULL OR payload_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_outbox_payload_sha256 CHECK (payload_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING','PUBLISHED','FAILED')),
    CONSTRAINT ck_outbox_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_outbox_published_shape CHECK ((status = 'PUBLISHED') = (published_at IS NOT NULL)),
    CONSTRAINT ck_outbox_published_after_available CHECK (published_at IS NULL OR published_at >= available_at),
    CONSTRAINT ck_outbox_revision CHECK (revision >= 1)
);

CREATE INDEX ix_outbox_pending_available ON platform.outbox_event (available_at, id) WHERE status = 'PENDING';
CREATE INDEX ix_outbox_failed_available ON platform.outbox_event (available_at, id) WHERE status = 'FAILED';

CREATE FUNCTION platform.idempotency_begin(
    p_scope text,
    p_key text,
    p_fingerprint text
) RETURNS TABLE (
    record_id uuid,
    revision integer,
    outcome text,
    result_type text,
    result_id uuid,
    result_snapshot jsonb,
    result_sha256 text
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = platform, pg_temp
AS $fn$
#variable_conflict use_column
DECLARE
    v_record_id uuid;
    v_revision integer;
    v_status text;
    v_fingerprint text;
    v_result_type text;
    v_result_id uuid;
    v_result_snapshot jsonb;
    v_result_sha256 text;
BEGIN
    IF p_scope IS NULL OR p_scope NOT IN ('IVR_CONFIRM','PURCHASE')
        OR p_key IS NULL OR length(p_key) NOT BETWEEN 1 AND 512 OR btrim(p_key) = ''
        OR p_fingerprint IS NULL OR p_fingerprint !~ '^[0-9a-f]{64}$' THEN
        RAISE EXCEPTION 'IDEMPOTENCY_ARGUMENT_INVALID' USING ERRCODE = 'P0001';
    END IF;

    INSERT INTO platform.idempotency_record AS r
        (id, scope, idempotency_key, request_fingerprint, status, revision)
    VALUES
        (pg_catalog.gen_random_uuid(), p_scope, p_key, p_fingerprint, 'NEW', 1)
    ON CONFLICT (scope, idempotency_key) DO NOTHING
    RETURNING r.id, r.revision INTO v_record_id, v_revision;

    IF FOUND THEN
        RETURN QUERY SELECT v_record_id, v_revision, 'NEW'::text, NULL::text, NULL::uuid, NULL::jsonb, NULL::text;
        RETURN;
    END IF;

    SELECT r.id, r.revision, r.status, r.request_fingerprint, r.result_type, r.result_id,
           r.result_snapshot, r.result_sha256
    INTO v_record_id, v_revision, v_status, v_fingerprint, v_result_type, v_result_id,
         v_result_snapshot, v_result_sha256
    FROM platform.idempotency_record AS r
    WHERE r.scope = p_scope AND r.idempotency_key = p_key
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'IDEMPOTENCY_INCOMPLETE' USING ERRCODE = 'P0001';
    END IF;

    IF v_status = 'COMPLETED' THEN
        IF v_fingerprint = p_fingerprint THEN
            RETURN QUERY SELECT v_record_id, v_revision, 'REPLAY'::text, v_result_type, v_result_id,
                                v_result_snapshot, v_result_sha256;
        ELSE
            RETURN QUERY SELECT NULL::uuid, NULL::integer, 'CONFLICT'::text, NULL::text, NULL::uuid,
                                NULL::jsonb, NULL::text;
        END IF;
        RETURN;
    END IF;

    RAISE EXCEPTION 'IDEMPOTENCY_INCOMPLETE' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE FUNCTION platform.idempotency_complete(
    p_record_id uuid,
    p_expected_revision integer,
    p_result_type text,
    p_result_id uuid,
    p_result_snapshot jsonb,
    p_result_sha256 text
) RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = platform, pg_temp
AS $fn$
#variable_conflict use_column
DECLARE
    v_status text;
    v_revision integer;
    v_result_sha256 text;
BEGIN
    IF p_record_id IS NULL
        OR p_expected_revision IS NULL OR p_expected_revision < 1
        OR p_result_type IS NULL OR length(p_result_type) NOT BETWEEN 1 AND 64 OR btrim(p_result_type) = ''
        OR p_result_id IS NULL
        OR p_result_snapshot IS NULL OR jsonb_typeof(p_result_snapshot) NOT IN ('object','array')
        OR p_result_sha256 IS NULL OR p_result_sha256 !~ '^[0-9a-f]{64}$' THEN
        RAISE EXCEPTION 'IDEMPOTENCY_ARGUMENT_INVALID' USING ERRCODE = 'P0001';
    END IF;

    IF EXISTS (
        WITH RECURSIVE walk(value, key_name) AS (
            SELECT p_result_snapshot, NULL::text
            UNION ALL
            SELECT child.value, child.key_name
            FROM walk AS w
            CROSS JOIN LATERAL (
                SELECT e.value, e.key AS key_name
                FROM jsonb_each(CASE WHEN jsonb_typeof(w.value) = 'object' THEN w.value ELSE '{}'::jsonb END) AS e
                UNION ALL
                SELECT a.value, NULL::text AS key_name
                FROM jsonb_array_elements(CASE WHEN jsonb_typeof(w.value) = 'array' THEN w.value ELSE '[]'::jsonb END) AS a
            ) AS child
        )
        SELECT 1
        FROM walk
        WHERE (key_name IS NOT NULL AND lower(key_name) = ANY (ARRAY[
                'phone','mobile','callernumber','calleenumber','providersubject','identitynumber',
                'idnumber','legalname','sharecode','otp','objectkey','objectversionid','bucket',
                'providertransactionid','providereventid','secret','token','password','hmac',
                'ciphertext','keyref','kmskey']::text[]))
            OR (jsonb_typeof(value) = 'object' AND value ? 'enc'
                AND ((SELECT count(*) FROM jsonb_each(value)) <> 1
                    OR jsonb_typeof(value -> 'enc') <> 'string'
                    OR (value ->> 'enc') !~ '^v1\.[A-Za-z0-9._:/-]+\..+$'))
    ) THEN
        RAISE EXCEPTION 'IDEMPOTENCY_ARGUMENT_INVALID' USING ERRCODE = 'P0001';
    END IF;

    UPDATE platform.idempotency_record AS r
    SET status = 'COMPLETED',
        result_type = p_result_type,
        result_id = p_result_id,
        result_snapshot = p_result_snapshot,
        result_sha256 = p_result_sha256,
        completed_at = pg_catalog.clock_timestamp(),
        revision = r.revision + 1
    WHERE r.id = p_record_id AND r.status = 'NEW' AND r.revision = p_expected_revision;

    IF FOUND THEN
        RETURN 'COMPLETED';
    END IF;

    SELECT r.status, r.revision, r.result_sha256
    INTO v_status, v_revision, v_result_sha256
    FROM platform.idempotency_record AS r
    WHERE r.id = p_record_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'IDEMPOTENCY_NOT_FOUND' USING ERRCODE = 'P0001';
    END IF;

    IF v_status = 'COMPLETED' AND v_result_sha256 = p_result_sha256 THEN
        RETURN 'ALREADY_COMPLETED';
    END IF;

    IF v_status = 'COMPLETED' THEN
        RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE = 'P0001';
    END IF;

    IF v_status = 'NEW' AND v_revision <> p_expected_revision THEN
        RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE = 'P0001';
    END IF;

    IF v_status = 'NEW' AND v_revision = p_expected_revision THEN
        UPDATE platform.idempotency_record AS r
        SET status = 'COMPLETED',
            result_type = p_result_type,
            result_id = p_result_id,
            result_snapshot = p_result_snapshot,
            result_sha256 = p_result_sha256,
            completed_at = pg_catalog.clock_timestamp(),
            revision = r.revision + 1
        WHERE r.id = p_record_id AND r.status = 'NEW' AND r.revision = p_expected_revision;

        IF FOUND THEN
            RETURN 'COMPLETED';
        END IF;
    END IF;

    RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE FUNCTION platform.outbox_append(
    p_event_domain text,
    p_dedupe_key text,
    p_aggregate_type text,
    p_aggregate_id uuid,
    p_event_type text,
    p_payload_redacted jsonb,
    p_payload_ciphertext text,
    p_payload_sha256 text
) RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = platform, pg_temp
AS $fn$
#variable_conflict use_column
DECLARE
    v_id uuid;
    v_payload_sha256 text;
BEGIN
    IF p_event_domain IS NULL OR p_event_domain NOT IN ('ACTIVATION','IVR_CONFIRM','ACTIVATION_STATUS','OPERATIONS','IVR_EVENT','IVR_DIRECT_AUTH','EVIDENCE','PAYMENT','SHARE_ACCESS','PURCHASE','SUBJECT_CLAIM')
        OR p_dedupe_key IS NULL OR length(p_dedupe_key) NOT BETWEEN 1 AND 512 OR btrim(p_dedupe_key) = ''
        OR p_aggregate_type IS NULL OR length(p_aggregate_type) NOT BETWEEN 1 AND 128 OR btrim(p_aggregate_type) = ''
        OR p_aggregate_id IS NULL
        OR p_event_type IS NULL OR length(p_event_type) NOT BETWEEN 1 AND 128 OR btrim(p_event_type) = ''
        OR (p_payload_redacted IS NULL AND p_payload_ciphertext IS NULL)
        OR (p_payload_ciphertext IS NOT NULL AND p_payload_ciphertext !~ '^v1\.[A-Za-z0-9._:/-]+\..+$')
        OR p_payload_sha256 IS NULL OR p_payload_sha256 !~ '^[0-9a-f]{64}$' THEN
        RAISE EXCEPTION 'OUTBOX_PAYLOAD_INVALID' USING ERRCODE = 'P0001';
    END IF;

    IF p_payload_redacted IS NOT NULL AND EXISTS (
        WITH RECURSIVE walk(value, key_name) AS (
            SELECT p_payload_redacted, NULL::text
            UNION ALL
            SELECT child.value, child.key_name
            FROM walk AS w
            CROSS JOIN LATERAL (
                SELECT e.value, e.key AS key_name
                FROM jsonb_each(CASE WHEN jsonb_typeof(w.value) = 'object' THEN w.value ELSE '{}'::jsonb END) AS e
                UNION ALL
                SELECT a.value, NULL::text AS key_name
                FROM jsonb_array_elements(CASE WHEN jsonb_typeof(w.value) = 'array' THEN w.value ELSE '[]'::jsonb END) AS a
            ) AS child
        )
        SELECT 1
        FROM walk
        WHERE (key_name IS NOT NULL AND lower(key_name) = ANY (ARRAY[
                'phone','mobile','callernumber','calleenumber','providersubject','identitynumber',
                'idnumber','legalname','sharecode','otp','objectkey','objectversionid','bucket',
                'providertransactionid','providereventid','secret','token','password','hmac',
                'ciphertext','keyref','kmskey']::text[]))
            OR (jsonb_typeof(value) = 'object' AND value ? 'enc'
                AND ((SELECT count(*) FROM jsonb_each(value)) <> 1
                    OR jsonb_typeof(value -> 'enc') <> 'string'
                    OR (value ->> 'enc') !~ '^v1\.[A-Za-z0-9._:/-]+\..+$'))
    ) THEN
        RAISE EXCEPTION 'OUTBOX_PAYLOAD_INVALID' USING ERRCODE = 'P0001';
    END IF;

    INSERT INTO platform.outbox_event AS o
        (id, event_domain, dedupe_key, aggregate_type, aggregate_id, event_type,
         payload_redacted, payload_ciphertext, payload_sha256, status, attempt_count, available_at, revision)
    VALUES
        (pg_catalog.gen_random_uuid(), p_event_domain, p_dedupe_key, p_aggregate_type, p_aggregate_id,
         p_event_type, p_payload_redacted, p_payload_ciphertext, p_payload_sha256, 'PENDING', 0, pg_catalog.now(), 1)
    ON CONFLICT (event_domain, dedupe_key) DO NOTHING
    RETURNING o.id INTO v_id;

    IF FOUND THEN
        RETURN v_id;
    END IF;

    SELECT o.id, o.payload_sha256
    INTO v_id, v_payload_sha256
    FROM platform.outbox_event AS o
    WHERE o.event_domain = p_event_domain AND o.dedupe_key = p_dedupe_key
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'OUTBOX_DEDUPE_CONFLICT' USING ERRCODE = 'P0001';
    END IF;

    IF v_payload_sha256 = p_payload_sha256 THEN
        RETURN v_id;
    END IF;

    RAISE EXCEPTION 'OUTBOX_DEDUPE_CONFLICT' USING ERRCODE = 'P0001';
END;
$fn$;

REVOKE ALL ON TABLE platform.idempotency_record FROM PUBLIC;
REVOKE ALL ON TABLE platform.outbox_event FROM PUBLIC;
REVOKE ALL ON FUNCTION platform.idempotency_begin(text, text, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION platform.idempotency_complete(uuid, integer, text, uuid, jsonb, text) FROM PUBLIC;
REVOKE ALL ON FUNCTION platform.outbox_append(text, text, text, uuid, text, jsonb, text, text) FROM PUBLIC;

GRANT USAGE ON SCHEMA platform TO platform_reliability_role, platform_dispatcher_role, customer_iam_role, organization_access_role, operations_iam_role, subject_number_role;
GRANT SELECT ON TABLE platform.idempotency_record TO platform_reliability_role;
GRANT SELECT ON TABLE platform.outbox_event TO platform_reliability_role;
GRANT SELECT ON TABLE platform.outbox_event TO platform_dispatcher_role;
GRANT UPDATE (status, attempt_count, available_at, published_at, revision) ON TABLE platform.outbox_event TO platform_dispatcher_role;
GRANT EXECUTE ON FUNCTION platform.idempotency_begin(text, text, text) TO customer_iam_role, organization_access_role, operations_iam_role, subject_number_role;
GRANT EXECUTE ON FUNCTION platform.idempotency_complete(uuid, integer, text, uuid, jsonb, text) TO customer_iam_role, organization_access_role, operations_iam_role, subject_number_role;
GRANT EXECUTE ON FUNCTION platform.outbox_append(text, text, text, uuid, text, jsonb, text, text) TO customer_iam_role, organization_access_role, operations_iam_role, subject_number_role;

RESET ROLE;

SET LOCAL ROLE audit_owner;

CREATE TABLE audit.audit_fact (
    id uuid NOT NULL,
    occurred_at timestamptz NOT NULL,
    actor_type text NOT NULL,
    actor_id uuid,
    action_code text NOT NULL,
    object_type text NOT NULL,
    object_id uuid,
    reason_code text,
    before_snapshot jsonb,
    after_snapshot jsonb,
    request_id text,
    trace_id text,
    CONSTRAINT pk_audit_fact PRIMARY KEY (id),
    CONSTRAINT ck_audit_actor_type CHECK (actor_type IN ('ACCOUNT','EMPLOYEE','SERVICE','SYSTEM')),
    CONSTRAINT ck_audit_actor_id CHECK ((actor_type = 'SYSTEM') OR actor_id IS NOT NULL),
    CONSTRAINT ck_audit_action_code CHECK (action_code ~ '^[A-Z][A-Z0-9_]{1,127}$'),
    CONSTRAINT ck_audit_object_type CHECK (length(object_type) BETWEEN 1 AND 64 AND btrim(object_type) <> ''),
    CONSTRAINT ck_audit_object_id CHECK (object_id IS NOT NULL OR object_type = 'SYSTEM'),
    CONSTRAINT ck_audit_reason_code CHECK (reason_code IS NULL OR (length(reason_code) BETWEEN 1 AND 128 AND btrim(reason_code) <> '')),
    CONSTRAINT ck_audit_before_type CHECK (before_snapshot IS NULL OR jsonb_typeof(before_snapshot) IN ('object','array')),
    CONSTRAINT ck_audit_after_type CHECK (after_snapshot IS NULL OR jsonb_typeof(after_snapshot) IN ('object','array')),
    CONSTRAINT ck_audit_request_id CHECK (request_id IS NULL OR length(request_id) BETWEEN 1 AND 128),
    CONSTRAINT ck_audit_trace_id CHECK (trace_id IS NULL OR length(trace_id) BETWEEN 1 AND 128)
);

CREATE INDEX ix_audit_object_time ON audit.audit_fact (object_type, object_id, occurred_at DESC);
CREATE INDEX ix_audit_actor_time ON audit.audit_fact (actor_type, actor_id, occurred_at DESC);
CREATE INDEX ix_audit_action_time ON audit.audit_fact (action_code, occurred_at DESC);
CREATE INDEX ix_audit_occurred_at ON audit.audit_fact (occurred_at DESC);

CREATE FUNCTION audit.reject_row_mutation() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = audit, pg_temp
AS $fn$
BEGIN
    RAISE EXCEPTION 'AUDIT_APPEND_ONLY' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE TRIGGER trg_audit_fact_immutable
BEFORE UPDATE OR DELETE ON audit.audit_fact
FOR EACH ROW EXECUTE FUNCTION audit.reject_row_mutation();

CREATE FUNCTION audit.append_fact(
    p_occurred_at timestamptz,
    p_actor_type text,
    p_actor_id uuid,
    p_action_code text,
    p_object_type text,
    p_object_id uuid,
    p_reason_code text,
    p_before_snapshot jsonb,
    p_after_snapshot jsonb,
    p_request_id text,
    p_trace_id text
) RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = audit, pg_temp
AS $fn$
#variable_conflict use_column
DECLARE
    v_id uuid;
BEGIN
    IF p_occurred_at IS NULL
        OR p_actor_type IS NULL OR p_actor_type NOT IN ('ACCOUNT','EMPLOYEE','SERVICE','SYSTEM')
        OR (p_actor_type <> 'SYSTEM' AND p_actor_id IS NULL)
        OR p_action_code IS NULL OR p_action_code !~ '^[A-Z][A-Z0-9_]{1,127}$'
        OR p_object_type IS NULL OR length(p_object_type) NOT BETWEEN 1 AND 64 OR btrim(p_object_type) = ''
        OR (p_object_id IS NULL AND p_object_type <> 'SYSTEM')
        OR (p_before_snapshot IS NOT NULL AND jsonb_typeof(p_before_snapshot) NOT IN ('object','array'))
        OR (p_after_snapshot IS NOT NULL AND jsonb_typeof(p_after_snapshot) NOT IN ('object','array'))
        OR (p_request_id IS NOT NULL AND length(p_request_id) NOT BETWEEN 1 AND 128)
        OR (p_trace_id IS NOT NULL AND length(p_trace_id) NOT BETWEEN 1 AND 128) THEN
        RAISE EXCEPTION 'AUDIT_ARGUMENT_INVALID' USING ERRCODE = 'P0001';
    END IF;

    IF (p_actor_type = 'EMPLOYEE' OR p_action_code IN (
            'IDENTITY_PROFILE_VERIFY','SUBJECT_OWNER_SET','RISK_STATE_CHANGE','EMPLOYEE_ACCOUNT_CHANGE',
            'ORGANIZATION_STATUS_CHANGE','MEMBERSHIP_CHANGE','IDEMPOTENCY_INCOMPLETE_ALERT'))
        AND (p_reason_code IS NULL OR btrim(p_reason_code) = '') THEN
        RAISE EXCEPTION 'AUDIT_REASON_REQUIRED' USING ERRCODE = 'P0001';
    END IF;

    IF p_reason_code IS NOT NULL
        AND (length(p_reason_code) NOT BETWEEN 1 AND 128 OR btrim(p_reason_code) = '') THEN
        RAISE EXCEPTION 'AUDIT_ARGUMENT_INVALID' USING ERRCODE = 'P0001';
    END IF;

    IF EXISTS (
        WITH RECURSIVE walk(value, key_name) AS (
            SELECT s.value, NULL::text
            FROM (VALUES (p_before_snapshot), (p_after_snapshot)) AS s(value)
            UNION ALL
            SELECT child.value, child.key_name
            FROM walk AS w
            CROSS JOIN LATERAL (
                SELECT e.value, e.key AS key_name
                FROM jsonb_each(CASE WHEN jsonb_typeof(w.value) = 'object' THEN w.value ELSE '{}'::jsonb END) AS e
                UNION ALL
                SELECT a.value, NULL::text AS key_name
                FROM jsonb_array_elements(CASE WHEN jsonb_typeof(w.value) = 'array' THEN w.value ELSE '[]'::jsonb END) AS a
            ) AS child
        )
        SELECT 1
        FROM walk
        WHERE (key_name IS NOT NULL AND lower(key_name) = ANY (ARRAY[
                'phone','mobile','callernumber','calleenumber','providersubject','identitynumber',
                'idnumber','legalname','sharecode','otp','objectkey','objectversionid','bucket',
                'providertransactionid','providereventid','secret','token','password','hmac',
                'ciphertext','keyref','kmskey']::text[]))
            OR (jsonb_typeof(value) = 'object' AND value ? 'enc'
                AND ((SELECT count(*) FROM jsonb_each(value)) <> 1
                    OR jsonb_typeof(value -> 'enc') <> 'string'
                    OR (value ->> 'enc') !~ '^v1\.[A-Za-z0-9._:/-]+\..+$'))
    ) THEN
        RAISE EXCEPTION 'AUDIT_ARGUMENT_INVALID' USING ERRCODE = 'P0001';
    END IF;

    INSERT INTO audit.audit_fact AS a
        (id, occurred_at, actor_type, actor_id, action_code, object_type, object_id, reason_code,
         before_snapshot, after_snapshot, request_id, trace_id)
    VALUES
        (pg_catalog.gen_random_uuid(), p_occurred_at, p_actor_type, p_actor_id, p_action_code,
         p_object_type, p_object_id, p_reason_code, p_before_snapshot, p_after_snapshot,
         p_request_id, p_trace_id)
    RETURNING a.id INTO v_id;

    RETURN v_id;
END;
$fn$;

REVOKE ALL ON TABLE audit.audit_fact FROM PUBLIC;
REVOKE ALL ON FUNCTION audit.reject_row_mutation() FROM PUBLIC;
REVOKE ALL ON FUNCTION audit.append_fact(timestamptz, text, uuid, text, text, uuid, text, jsonb, jsonb, text, text) FROM PUBLIC;

GRANT USAGE ON SCHEMA audit TO customer_iam_role, organization_access_role, operations_iam_role, subject_number_role, subject_identity_verification_role, operations_service_role, audit_reader_role;
GRANT SELECT ON TABLE audit.audit_fact TO audit_reader_role;
GRANT EXECUTE ON FUNCTION audit.append_fact(timestamptz, text, uuid, text, text, uuid, text, jsonb, jsonb, text, text) TO customer_iam_role, organization_access_role, operations_iam_role, subject_number_role, subject_identity_verification_role, operations_service_role;

RESET ROLE;
