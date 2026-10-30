-- PA8 V008: evidence media and evidence creation cycle.
-- Creates the evidence schema and two NOLOGIN roles, the five frozen evidence
-- tables (56 columns, 39 named checks, 8 ordinary immediate RESTRICT FKs and
-- 2 DEFERRABLE INITIALLY DEFERRED creation-cycle FKs), five local SECURITY
-- INVOKER trigger functions, seven triggers and the exact owner/runtime/
-- cross-module grants. No SQL service function, no sharing/V009 object, no
-- application service, no S1/S2 table altered.

CREATE ROLE evidence_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE evidence_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;

CREATE SCHEMA evidence AUTHORIZATION evidence_owner;

REVOKE ALL ON SCHEMA evidence FROM PUBLIC;
GRANT USAGE ON SCHEMA evidence TO evidence_role;

SET LOCAL ROLE evidence_owner;

CREATE TABLE evidence.media_object (
    id uuid NOT NULL,
    storage_provider text NOT NULL,
    bucket text NOT NULL,
    object_key text NOT NULL,
    object_version_id text NOT NULL,
    sha256 text NOT NULL,
    size_bytes bigint NOT NULL,
    mime_type text NOT NULL,
    crypto_key_ref text NOT NULL,
    worm_status text NOT NULL DEFAULT 'PENDING',
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_media_object PRIMARY KEY (id),
    CONSTRAINT uq_media_object_version UNIQUE (bucket, object_key, object_version_id),
    CONSTRAINT ck_media_object_provider
        CHECK (storage_provider = 'COS'),
    CONSTRAINT ck_media_object_bucket
        CHECK (length(bucket) BETWEEN 1 AND 255 AND btrim(bucket) <> ''),
    CONSTRAINT ck_media_object_key
        CHECK (length(object_key) BETWEEN 1 AND 1024 AND btrim(object_key) <> ''),
    CONSTRAINT ck_media_object_version
        CHECK (length(object_version_id) BETWEEN 1 AND 1024 AND btrim(object_version_id) <> ''),
    CONSTRAINT ck_media_object_sha256
        CHECK (sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_media_object_size
        CHECK (size_bytes > 0),
    CONSTRAINT ck_media_object_mime
        CHECK (length(mime_type) BETWEEN 1 AND 255 AND btrim(mime_type) <> ''),
    CONSTRAINT ck_media_object_key_ref
        CHECK (length(crypto_key_ref) BETWEEN 1 AND 128 AND btrim(crypto_key_ref) <> ''),
    CONSTRAINT ck_media_object_worm_status
        CHECK (worm_status IN ('PENDING','CONFIRMED','UNSUPPORTED','FAILED'))
);

CREATE INDEX ix_media_object_sha256
    ON evidence.media_object (sha256);

CREATE TABLE evidence.recording (
    id uuid NOT NULL,
    call_id text NOT NULL,
    recording_id text NOT NULL,
    media_object_id uuid,
    declared_format text NOT NULL,
    declared_size bigint NOT NULL,
    declared_duration integer NOT NULL,
    declared_sha256 text NOT NULL,
    validated_duration integer,
    validated_sha256 text,
    status text NOT NULL DEFAULT 'DECLARED',
    upload_expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    validated_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_recording PRIMARY KEY (id),
    CONSTRAINT uq_recording_protocol_id UNIQUE (recording_id),
    CONSTRAINT ck_recording_protocol_id
        CHECK (length(recording_id) BETWEEN 1 AND 128 AND btrim(recording_id) <> ''),
    CONSTRAINT ck_recording_declared_format
        CHECK (length(declared_format) BETWEEN 1 AND 64 AND btrim(declared_format) <> ''),
    CONSTRAINT ck_recording_declared_size
        CHECK (declared_size > 0),
    CONSTRAINT ck_recording_declared_duration
        CHECK (declared_duration > 0),
    CONSTRAINT ck_recording_declared_sha256
        CHECK (declared_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_recording_validated_duration
        CHECK (validated_duration IS NULL OR validated_duration > 0),
    CONSTRAINT ck_recording_validated_sha256
        CHECK (validated_sha256 IS NULL OR validated_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_recording_status
        CHECK (status IN ('DECLARED','UPLOADED','VALIDATED','QUARANTINED')),
    CONSTRAINT ck_recording_state_shape
        CHECK ((status = 'DECLARED' AND media_object_id IS NULL AND validated_duration IS NULL
                    AND validated_sha256 IS NULL AND validated_at IS NULL)
            OR (status = 'UPLOADED' AND media_object_id IS NOT NULL AND validated_duration IS NULL
                    AND validated_sha256 IS NULL AND validated_at IS NULL)
            OR (status = 'VALIDATED' AND media_object_id IS NOT NULL AND validated_duration IS NOT NULL
                    AND validated_sha256 IS NOT NULL AND validated_at IS NOT NULL)
            OR (status = 'QUARANTINED' AND validated_at IS NOT NULL)),
    CONSTRAINT ck_recording_upload_expiry
        CHECK (upload_expires_at > created_at),
    CONSTRAINT ck_recording_revision
        CHECK (revision >= 1)
);

CREATE INDEX ix_recording_call
    ON evidence.recording (call_id, created_at DESC);

CREATE INDEX ix_recording_pending
    ON evidence.recording (status, upload_expires_at);

CREATE TABLE evidence.evidence_record (
    id uuid NOT NULL,
    subject_id uuid NOT NULL,
    source_type text NOT NULL,
    source_ref_id uuid NOT NULL,
    source_event_domain text NOT NULL,
    source_event_id text NOT NULL,
    call_id text,
    session_id uuid,
    reservation_id uuid,
    identity_at_capture jsonb NOT NULL,
    consent_version text NOT NULL,
    status text NOT NULL DEFAULT 'PENDING',
    revision integer NOT NULL DEFAULT 1,
    snapshot jsonb NOT NULL,
    snapshot_sha256 text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    solidified_at timestamptz,
    CONSTRAINT pk_evidence_record PRIMARY KEY (id),
    CONSTRAINT uq_evidence_source_event UNIQUE (source_event_domain, source_event_id),
    CONSTRAINT ck_evidence_source_type
        CHECK (source_type = 'CALL_RECORDING'),
    CONSTRAINT ck_evidence_source_ref
        CHECK (source_ref_id IS NOT NULL),
    CONSTRAINT ck_evidence_source_domain
        CHECK (source_event_domain = 'EVIDENCE'),
    CONSTRAINT ck_evidence_source_event_id
        CHECK (length(source_event_id) BETWEEN 1 AND 128 AND btrim(source_event_id) <> ''),
    CONSTRAINT ck_evidence_capture_shape
        CHECK (jsonb_typeof(identity_at_capture) = 'object'
            AND identity_at_capture ? 'subjectId'
            AND identity_at_capture ? 'capturedAt'
            AND identity_at_capture ? 'sourceChannel'),
    CONSTRAINT ck_evidence_capture_subject
        CHECK (identity_at_capture->>'subjectId' = subject_id::text),
    CONSTRAINT ck_evidence_consent_version
        CHECK (length(consent_version) BETWEEN 1 AND 64 AND btrim(consent_version) <> ''),
    CONSTRAINT ck_evidence_status
        CHECK (status IN ('PENDING','SOLIDIFIED','QUARANTINED','FAILED')),
    CONSTRAINT ck_evidence_state_shape
        CHECK ((status = 'PENDING' AND solidified_at IS NULL)
            OR (status = 'SOLIDIFIED' AND solidified_at IS NOT NULL)
            OR (status IN ('QUARANTINED','FAILED') AND solidified_at IS NULL)),
    CONSTRAINT ck_evidence_revision
        CHECK (revision >= 1),
    CONSTRAINT ck_evidence_snapshot_shape
        CHECK (jsonb_typeof(snapshot) = 'object'
            AND snapshot ? 'capture'
            AND snapshot ? 'content'
            AND jsonb_typeof(snapshot->'source') = 'object'
            AND snapshot->>'sourceEventDomain' = 'EVIDENCE'
            AND snapshot->>'sourceEventId' = source_event_id
            AND (snapshot->'source'->>'callId') IS NOT DISTINCT FROM call_id
            AND (snapshot->'source'->>'sessionId') IS NOT DISTINCT FROM session_id::text
            AND (snapshot->'source'->>'subjectId') IS NOT DISTINCT FROM subject_id::text
            AND (snapshot->'source'->>'reservationId') IS NOT DISTINCT FROM reservation_id::text
            AND (snapshot->'source'->>'recordingId') IS NOT DISTINCT FROM source_ref_id::text),
    CONSTRAINT ck_evidence_snapshot_sha256
        CHECK (snapshot_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_evidence_link_shape
        CHECK (session_id IS NULL OR call_id IS NOT NULL)
);

CREATE INDEX ix_evidence_subject
    ON evidence.evidence_record (subject_id, created_at DESC);

CREATE INDEX ix_evidence_call
    ON evidence.evidence_record (call_id);

CREATE INDEX ix_evidence_status
    ON evidence.evidence_record (status, created_at);

CREATE TABLE evidence.evidence_artifact (
    id uuid NOT NULL,
    evidence_id uuid NOT NULL,
    media_object_id uuid NOT NULL,
    artifact_role text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_evidence_artifact PRIMARY KEY (id),
    CONSTRAINT uq_evidence_artifact UNIQUE (evidence_id, artifact_role, media_object_id),
    CONSTRAINT ck_evidence_artifact_role
        CHECK (artifact_role IN ('ORIGINAL','DERIVED'))
);

CREATE UNIQUE INDEX uq_evidence_original_artifact
    ON evidence.evidence_artifact (evidence_id)
    WHERE artifact_role = 'ORIGINAL';

CREATE INDEX ix_evidence_artifact_media
    ON evidence.evidence_artifact (media_object_id);

CREATE TABLE evidence.evidence_event (
    id uuid NOT NULL,
    evidence_id uuid NOT NULL,
    event_domain text NOT NULL,
    event_id text NOT NULL,
    event_type text NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    payload_redacted jsonb NOT NULL,
    payload_sha256 text NOT NULL,
    CONSTRAINT pk_evidence_event PRIMARY KEY (id),
    CONSTRAINT uq_evidence_event_domain_id UNIQUE (event_domain, event_id),
    CONSTRAINT ck_evidence_event_domain
        CHECK (event_domain = 'EVIDENCE'),
    CONSTRAINT ck_evidence_event_id
        CHECK (length(event_id) BETWEEN 1 AND 128 AND btrim(event_id) <> ''),
    CONSTRAINT ck_evidence_event_type
        CHECK (event_type IN ('EVIDENCE_CREATED','EVIDENCE_SOLIDIFIED','EVIDENCE_QUARANTINED','EVIDENCE_FAILED')),
    CONSTRAINT ck_evidence_event_payload
        CHECK (jsonb_typeof(payload_redacted) = 'object'),
    CONSTRAINT ck_evidence_event_payload_sha256
        CHECK (payload_sha256 ~ '^[0-9a-f]{64}$')
);

CREATE UNIQUE INDEX uq_evidence_created_event
    ON evidence.evidence_event (evidence_id)
    WHERE event_type = 'EVIDENCE_CREATED';

CREATE INDEX ix_evidence_event_record
    ON evidence.evidence_event (evidence_id, occurred_at);

RESET ROLE;

ALTER TABLE evidence.recording
    ADD CONSTRAINT fk_recording_call
    FOREIGN KEY (call_id) REFERENCES ivr.call_record(call_id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE evidence.recording
    ADD CONSTRAINT fk_recording_media_object
    FOREIGN KEY (media_object_id) REFERENCES evidence.media_object(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE evidence.evidence_record
    ADD CONSTRAINT fk_evidence_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE evidence.evidence_record
    ADD CONSTRAINT fk_evidence_call
    FOREIGN KEY (call_id) REFERENCES ivr.call_record(call_id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE evidence.evidence_record
    ADD CONSTRAINT fk_evidence_session
    FOREIGN KEY (session_id) REFERENCES ivr.inbound_session(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE evidence.evidence_record
    ADD CONSTRAINT fk_evidence_reservation
    FOREIGN KEY (reservation_id) REFERENCES entitlement_metering.usage_reservation(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE evidence.evidence_artifact
    ADD CONSTRAINT fk_evidence_artifact_evidence
    FOREIGN KEY (evidence_id) REFERENCES evidence.evidence_record(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE evidence.evidence_artifact
    ADD CONSTRAINT fk_evidence_artifact_media
    FOREIGN KEY (media_object_id) REFERENCES evidence.media_object(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE evidence.evidence_record
    ADD CONSTRAINT fk_evidence_source_event
    FOREIGN KEY (source_event_domain, source_event_id)
    REFERENCES evidence.evidence_event(event_domain, event_id)
    DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE evidence.evidence_event
    ADD CONSTRAINT fk_evidence_event_record
    FOREIGN KEY (evidence_id)
    REFERENCES evidence.evidence_record(id)
    DEFERRABLE INITIALLY DEFERRED;

SET LOCAL ROLE evidence_owner;

CREATE FUNCTION evidence.reject_row_mutation() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = evidence, pg_temp
AS $fn$
BEGIN
    RAISE EXCEPTION 'EVIDENCE_APPEND_ONLY' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE FUNCTION evidence.guard_recording() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = evidence, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.call_id IS DISTINCT FROM OLD.call_id
        OR NEW.recording_id IS DISTINCT FROM OLD.recording_id
        OR NEW.declared_format IS DISTINCT FROM OLD.declared_format
        OR NEW.declared_size IS DISTINCT FROM OLD.declared_size
        OR NEW.declared_duration IS DISTINCT FROM OLD.declared_duration
        OR NEW.declared_sha256 IS DISTINCT FROM OLD.declared_sha256
        OR NEW.upload_expires_at IS DISTINCT FROM OLD.upload_expires_at
        OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'RECORDING_HEAD_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;

    IF OLD.status IN ('VALIDATED','QUARANTINED') AND NEW.status IS DISTINCT FROM OLD.status THEN
        RAISE EXCEPTION 'RECORDING_STATUS_TERMINAL' USING ERRCODE = 'P0001';
    END IF;

    IF OLD.media_object_id IS NOT NULL AND NEW.media_object_id IS DISTINCT FROM OLD.media_object_id THEN
        RAISE EXCEPTION 'RECORDING_MEDIA_OBJECT_LOCKED' USING ERRCODE = 'P0001';
    END IF;

    IF OLD.validated_duration IS NOT NULL AND NEW.validated_duration IS DISTINCT FROM OLD.validated_duration THEN
        RAISE EXCEPTION 'RECORDING_VALIDATION_LOCKED' USING ERRCODE = 'P0001';
    END IF;

    IF OLD.validated_sha256 IS NOT NULL AND NEW.validated_sha256 IS DISTINCT FROM OLD.validated_sha256 THEN
        RAISE EXCEPTION 'RECORDING_VALIDATION_LOCKED' USING ERRCODE = 'P0001';
    END IF;

    IF OLD.validated_at IS NOT NULL AND NEW.validated_at IS DISTINCT FROM OLD.validated_at THEN
        RAISE EXCEPTION 'RECORDING_VALIDATION_LOCKED' USING ERRCODE = 'P0001';
    END IF;

    IF NEW.status IS DISTINCT FROM OLD.status THEN
        IF NOT ((OLD.status = 'DECLARED' AND NEW.status = 'UPLOADED')
            OR (OLD.status = 'UPLOADED' AND NEW.status IN ('VALIDATED','QUARANTINED'))
            OR (OLD.status = 'DECLARED' AND NEW.status = 'QUARANTINED')) THEN
            RAISE EXCEPTION 'RECORDING_STATUS_TRANSITION' USING ERRCODE = 'P0001';
        END IF;
        IF NEW.revision <> OLD.revision + 1 THEN
            RAISE EXCEPTION 'RECORDING_REVISION_INVALID' USING ERRCODE = 'P0001';
        END IF;
    END IF;

    RETURN NEW;
END;
$fn$;

CREATE FUNCTION evidence.guard_evidence_record() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = evidence, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.subject_id IS DISTINCT FROM OLD.subject_id
        OR NEW.source_type IS DISTINCT FROM OLD.source_type
        OR NEW.source_ref_id IS DISTINCT FROM OLD.source_ref_id
        OR NEW.source_event_domain IS DISTINCT FROM OLD.source_event_domain
        OR NEW.source_event_id IS DISTINCT FROM OLD.source_event_id
        OR NEW.call_id IS DISTINCT FROM OLD.call_id
        OR NEW.session_id IS DISTINCT FROM OLD.session_id
        OR NEW.reservation_id IS DISTINCT FROM OLD.reservation_id
        OR NEW.identity_at_capture IS DISTINCT FROM OLD.identity_at_capture
        OR NEW.consent_version IS DISTINCT FROM OLD.consent_version
        OR NEW.snapshot IS DISTINCT FROM OLD.snapshot
        OR NEW.snapshot_sha256 IS DISTINCT FROM OLD.snapshot_sha256
        OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'EVIDENCE_RECORD_HEAD_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;

    IF OLD.status = 'PENDING' AND NEW.status IS DISTINCT FROM OLD.status THEN
        IF NEW.revision <> OLD.revision + 1 THEN
            RAISE EXCEPTION 'EVIDENCE_RECORD_REVISION_INVALID' USING ERRCODE = 'P0001';
        END IF;
    ELSIF OLD.status <> 'PENDING' AND NEW.status IS DISTINCT FROM OLD.status THEN
        RAISE EXCEPTION 'EVIDENCE_RECORD_STATUS_TERMINAL' USING ERRCODE = 'P0001';
    END IF;

    RETURN NEW;
END;
$fn$;

CREATE FUNCTION evidence.guard_evidence_record_creation_cycle() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = evidence, pg_temp
AS $fn$
DECLARE
    v_count integer;
BEGIN
    SELECT count(*) INTO v_count
    FROM evidence.evidence_event e
    WHERE e.evidence_id = NEW.id
      AND e.event_type = 'EVIDENCE_CREATED'
      AND e.event_domain = NEW.source_event_domain
      AND e.event_id = NEW.source_event_id;
    IF v_count <> 1 THEN
        RAISE EXCEPTION 'EVIDENCE_CREATION_CYCLE_VIOLATION' USING ERRCODE = 'P0001';
    END IF;
    RETURN NULL;
END;
$fn$;

CREATE FUNCTION evidence.guard_evidence_event_creation_cycle() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = evidence, pg_temp
AS $fn$
DECLARE
    v_record evidence.evidence_record%ROWTYPE;
    v_count integer;
BEGIN
    IF NEW.event_type <> 'EVIDENCE_CREATED' THEN
        RETURN NULL;
    END IF;
    SELECT * INTO v_record
    FROM evidence.evidence_record r
    WHERE r.id = NEW.evidence_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'EVIDENCE_CREATION_CYCLE_VIOLATION' USING ERRCODE = 'P0001';
    END IF;
    IF v_record.source_event_domain IS DISTINCT FROM NEW.event_domain
        OR v_record.source_event_id IS DISTINCT FROM NEW.event_id THEN
        RAISE EXCEPTION 'EVIDENCE_CREATION_CYCLE_VIOLATION' USING ERRCODE = 'P0001';
    END IF;
    SELECT count(*) INTO v_count
    FROM evidence.evidence_event e
    WHERE e.evidence_id = NEW.evidence_id
      AND e.event_type = 'EVIDENCE_CREATED';
    IF v_count <> 1 THEN
        RAISE EXCEPTION 'EVIDENCE_CREATION_CYCLE_VIOLATION' USING ERRCODE = 'P0001';
    END IF;
    RETURN NULL;
END;
$fn$;

CREATE TRIGGER trg_media_object_immutable
BEFORE UPDATE OR DELETE ON evidence.media_object
FOR EACH ROW EXECUTE FUNCTION evidence.reject_row_mutation();

CREATE TRIGGER trg_recording_guard
BEFORE UPDATE ON evidence.recording
FOR EACH ROW EXECUTE FUNCTION evidence.guard_recording();

CREATE TRIGGER trg_evidence_record_guard
BEFORE UPDATE ON evidence.evidence_record
FOR EACH ROW EXECUTE FUNCTION evidence.guard_evidence_record();

CREATE TRIGGER trg_evidence_artifact_immutable
BEFORE UPDATE OR DELETE ON evidence.evidence_artifact
FOR EACH ROW EXECUTE FUNCTION evidence.reject_row_mutation();

CREATE TRIGGER trg_evidence_event_immutable
BEFORE UPDATE OR DELETE ON evidence.evidence_event
FOR EACH ROW EXECUTE FUNCTION evidence.reject_row_mutation();

CREATE CONSTRAINT TRIGGER trg_evidence_record_creation_cycle
AFTER INSERT OR UPDATE OF source_event_domain, source_event_id, status
ON evidence.evidence_record
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION evidence.guard_evidence_record_creation_cycle();

CREATE CONSTRAINT TRIGGER trg_evidence_event_creation_cycle
AFTER INSERT OR UPDATE OF evidence_id, event_domain, event_id, event_type
ON evidence.evidence_event
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION evidence.guard_evidence_event_creation_cycle();

REVOKE ALL ON TABLE evidence.media_object FROM PUBLIC;
REVOKE ALL ON TABLE evidence.recording FROM PUBLIC;
REVOKE ALL ON TABLE evidence.evidence_record FROM PUBLIC;
REVOKE ALL ON TABLE evidence.evidence_artifact FROM PUBLIC;
REVOKE ALL ON TABLE evidence.evidence_event FROM PUBLIC;
REVOKE ALL ON FUNCTION evidence.reject_row_mutation() FROM PUBLIC;
REVOKE ALL ON FUNCTION evidence.guard_recording() FROM PUBLIC;
REVOKE ALL ON FUNCTION evidence.guard_evidence_record() FROM PUBLIC;
REVOKE ALL ON FUNCTION evidence.guard_evidence_record_creation_cycle() FROM PUBLIC;
REVOKE ALL ON FUNCTION evidence.guard_evidence_event_creation_cycle() FROM PUBLIC;

GRANT SELECT, INSERT ON TABLE evidence.media_object TO evidence_role;
GRANT SELECT, INSERT ON TABLE evidence.recording TO evidence_role;
GRANT UPDATE (media_object_id, validated_duration, validated_sha256, status, validated_at, revision)
    ON TABLE evidence.recording TO evidence_role;
GRANT SELECT, INSERT ON TABLE evidence.evidence_record TO evidence_role;
GRANT UPDATE (status, solidified_at, revision)
    ON TABLE evidence.evidence_record TO evidence_role;
GRANT SELECT, INSERT ON TABLE evidence.evidence_artifact TO evidence_role;
GRANT SELECT, INSERT ON TABLE evidence.evidence_event TO evidence_role;

GRANT EXECUTE ON FUNCTION evidence.reject_row_mutation()
    TO evidence_role;
GRANT EXECUTE ON FUNCTION evidence.guard_recording()
    TO evidence_role;
GRANT EXECUTE ON FUNCTION evidence.guard_evidence_record()
    TO evidence_role;
GRANT EXECUTE ON FUNCTION evidence.guard_evidence_record_creation_cycle()
    TO evidence_role;
GRANT EXECUTE ON FUNCTION evidence.guard_evidence_event_creation_cycle()
    TO evidence_role;

RESET ROLE;

SET LOCAL ROLE subject_number_owner;

GRANT USAGE ON SCHEMA subject_number TO evidence_role;
GRANT SELECT (id)
    ON TABLE subject_number.business_subject TO evidence_role;

RESET ROLE;

SET LOCAL ROLE ivr_owner;

GRANT USAGE ON SCHEMA ivr TO evidence_role;
GRANT SELECT (call_id, session_id, subject_id)
    ON TABLE ivr.call_record TO evidence_role;
GRANT SELECT (id, call_id, subject_id)
    ON TABLE ivr.inbound_session TO evidence_role;

RESET ROLE;

SET LOCAL ROLE entitlement_metering_owner;

GRANT USAGE ON SCHEMA entitlement_metering TO evidence_role;
GRANT SELECT (id, call_id, subject_id)
    ON TABLE entitlement_metering.usage_reservation TO evidence_role;

RESET ROLE;

SET LOCAL ROLE platform_reliability_owner;

GRANT USAGE ON SCHEMA platform TO evidence_role;
GRANT EXECUTE ON FUNCTION platform.outbox_append
    TO evidence_role;

RESET ROLE;

SET LOCAL ROLE audit_owner;

GRANT USAGE ON SCHEMA audit TO evidence_role;
GRANT EXECUTE ON FUNCTION audit.append_fact
    TO evidence_role;

RESET ROLE;
