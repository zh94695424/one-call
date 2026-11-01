CREATE ROLE sharing_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE sharing_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;

CREATE SCHEMA sharing AUTHORIZATION sharing_owner;
REVOKE ALL ON SCHEMA sharing FROM PUBLIC;
GRANT USAGE ON SCHEMA sharing TO sharing_role;
GRANT USAGE ON SCHEMA evidence TO sharing_role;

SET LOCAL ROLE sharing_owner;

CREATE TABLE sharing.evidence_share_grant (
    id uuid NOT NULL,
    share_id text NOT NULL,
    capability_token_hash text NOT NULL,
    evidence_id uuid NOT NULL,
    created_by_account_id uuid NOT NULL,
    owner_organization_id uuid,
    verification_mode text NOT NULL,
    code_salt text,
    code_hash text,
    code_generation integer NOT NULL DEFAULT 0,
    valid_from timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    max_admissions integer,
    admissions_used integer NOT NULL DEFAULT 0,
    download_allowed boolean NOT NULL DEFAULT false,
    content_snapshot jsonb NOT NULL,
    content_sha256 text NOT NULL,
    status text NOT NULL DEFAULT 'ACTIVE'::text,
    revocation_generation integer NOT NULL DEFAULT 0,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_evidence_share_grant PRIMARY KEY (id),
    CONSTRAINT uq_share_grant_share_id UNIQUE (share_id),
    CONSTRAINT uq_share_grant_token_hash UNIQUE (capability_token_hash),
    CONSTRAINT ck_share_grant_share_id
        CHECK (length(share_id) BETWEEN 1 AND 128 AND btrim(share_id) <> ''),
    CONSTRAINT ck_share_grant_token_hash
        CHECK (capability_token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_share_grant_verification_mode
        CHECK (verification_mode IN ('NONE','EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER')),
    CONSTRAINT ck_share_grant_code_salt
        CHECK (code_salt IS NULL OR (length(code_salt) BETWEEN 22 AND 256 AND code_salt ~ '^[A-Za-z0-9_-]+$')),
    CONSTRAINT ck_share_grant_code_hash
        CHECK (code_hash IS NULL OR code_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_share_grant_code_generation
        CHECK (code_generation >= 0),
    CONSTRAINT ck_share_grant_verification_shape
        CHECK ((verification_mode = 'EXTRACTION_CODE' AND code_salt IS NOT NULL AND code_hash IS NOT NULL AND code_generation >= 1) OR (verification_mode <> 'EXTRACTION_CODE' AND code_salt IS NULL AND code_hash IS NULL AND code_generation = 0)),
    CONSTRAINT ck_share_grant_org_member
        CHECK (verification_mode <> 'ORGANIZATION_MEMBER' OR owner_organization_id IS NOT NULL),
    CONSTRAINT ck_share_grant_validity
        CHECK (expires_at > valid_from),
    CONSTRAINT ck_share_grant_admissions
        CHECK (admissions_used >= 0 AND (max_admissions IS NULL OR (max_admissions >= 1 AND admissions_used <= max_admissions))),
    CONSTRAINT ck_share_grant_snapshot_shape
        CHECK (jsonb_typeof(content_snapshot) = 'object' AND content_snapshot ? 'content' AND content_snapshot ? 'verification' AND content_snapshot ? 'creation' AND jsonb_typeof(content_snapshot->'content') = 'object' AND jsonb_typeof(content_snapshot->'verification') = 'object' AND jsonb_typeof(content_snapshot->'creation') = 'object' AND content_snapshot->'content'->>'evidenceId' = evidence_id::text AND content_snapshot->'creation'->>'creatorAccountId' = created_by_account_id::text AND content_snapshot->'creation'->>'evidenceId' = evidence_id::text AND (content_snapshot->'creation'->>'ownerOrganizationId') IS NOT DISTINCT FROM owner_organization_id::text AND (content_snapshot->'creation'->>'scopeDigest') ~ '^[0-9a-f]{64}$' AND (content_snapshot->'creation'->>'creationRequestDigest') ~ '^[0-9a-f]{64}$' AND (content_snapshot->'creation'->>'creationFingerprint') ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_share_grant_snapshot_binding
        CHECK (content_snapshot->'verification'->>'mode' = verification_mode),
    CONSTRAINT ck_share_grant_mobile_binding
        CHECK ((verification_mode = 'DESIGNATED_MOBILE' AND (content_snapshot->'verification'->>'mobileHmac') ~ '^[0-9a-f]{64}$' AND length(content_snapshot->'verification'->>'keyVersion') BETWEEN 1 AND 64) OR (verification_mode <> 'DESIGNATED_MOBILE' AND NOT (content_snapshot->'verification' ? 'mobileHmac'))),
    CONSTRAINT ck_share_grant_content_sha256
        CHECK (content_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_share_grant_status
        CHECK (status IN ('ACTIVE','EXPIRED','REVOKED','LOCKED')),
    CONSTRAINT ck_share_grant_revocation_generation
        CHECK (revocation_generation >= 0),
    CONSTRAINT ck_share_grant_revision
        CHECK (revision >= 1)
);

CREATE INDEX ix_share_grant_evidence ON sharing.evidence_share_grant (evidence_id, status);
CREATE INDEX ix_share_grant_expiry ON sharing.evidence_share_grant (status, expires_at);

CREATE TABLE sharing.share_admission (
    id uuid NOT NULL,
    share_grant_id uuid NOT NULL,
    admission_key text NOT NULL,
    admission_no integer NOT NULL,
    verification_mode text NOT NULL,
    verification_result text NOT NULL,
    admitted_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_share_admission PRIMARY KEY (id),
    CONSTRAINT uq_share_admission_key UNIQUE (share_grant_id, admission_key),
    CONSTRAINT uq_share_admission_no UNIQUE (share_grant_id, admission_no),
    CONSTRAINT ck_share_admission_key
        CHECK (length(admission_key) BETWEEN 1 AND 128 AND btrim(admission_key) <> ''),
    CONSTRAINT ck_share_admission_no
        CHECK (admission_no >= 1),
    CONSTRAINT ck_share_admission_mode
        CHECK (verification_mode IN ('NONE','EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER')),
    CONSTRAINT ck_share_admission_result
        CHECK (verification_result IN ('VERIFIED','NOT_REQUIRED')),
    CONSTRAINT ck_share_admission_result_shape
        CHECK ((verification_mode = 'NONE' AND verification_result = 'NOT_REQUIRED') OR (verification_mode IN ('EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER') AND verification_result = 'VERIFIED'))
);

CREATE INDEX ix_share_admission_grant_time ON sharing.share_admission (share_grant_id, admitted_at);

CREATE TABLE sharing.share_access_event (
    id uuid NOT NULL,
    share_grant_id uuid NOT NULL,
    share_admission_id uuid,
    event_domain text NOT NULL,
    event_id text NOT NULL,
    event_type text NOT NULL,
    result text NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    risk_digest text,
    payload_redacted jsonb NOT NULL,
    payload_sha256 text NOT NULL,
    CONSTRAINT pk_share_access_event PRIMARY KEY (id),
    CONSTRAINT uq_share_access_event_domain_id UNIQUE (event_domain, event_id),
    CONSTRAINT ck_share_access_event_domain
        CHECK (event_domain = 'SHARE_ACCESS'),
    CONSTRAINT ck_share_access_event_id
        CHECK (length(event_id) BETWEEN 1 AND 128 AND btrim(event_id) <> ''),
    CONSTRAINT ck_share_access_event_type
        CHECK (event_type IN ('PAGE_OPENED','PREFETCH_IGNORED','VERIFICATION_FAILED','LOCKED','ADMISSION_GRANTED','TICKET_ISSUED','TICKET_FAILED','PLAYBACK_AUTHORIZED','SEEK_AUTHORIZED','RENEWED','EXTENDED','CODE_ROTATED','REVOKED','EXPIRED','FAILURE')),
    CONSTRAINT ck_share_access_event_result
        CHECK (result IN ('SUCCESS','DENIED','FAILED','IGNORED')),
    CONSTRAINT ck_share_access_event_risk_digest
        CHECK (risk_digest IS NULL OR risk_digest ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_share_access_event_payload
        CHECK (jsonb_typeof(payload_redacted) = 'object'),
    CONSTRAINT ck_share_access_event_payload_sha256
        CHECK (payload_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_share_access_event_admission_shape
        CHECK (event_type NOT IN ('ADMISSION_GRANTED','TICKET_ISSUED','TICKET_FAILED','PLAYBACK_AUTHORIZED','SEEK_AUTHORIZED','RENEWED') OR share_admission_id IS NOT NULL),
    CONSTRAINT ck_share_access_event_admission_facts
        CHECK (event_type <> 'ADMISSION_GRANTED' OR (payload_redacted ? 'verificationMode' AND payload_redacted->>'verificationMode' IN ('NONE','EXTRACTION_CODE','DESIGNATED_MOBILE','ORGANIZATION_MEMBER') AND payload_redacted ? 'revocationGeneration' AND payload_redacted ? 'codeGeneration' AND payload_redacted ? 'grantExpiresAt' AND payload_redacted ? 'admissionExpiresAt' AND payload_redacted ? 'sessionExpiresAt' AND (payload_redacted->>'verificationMode' <> 'ORGANIZATION_MEMBER' OR (payload_redacted ? 'recipientAccountId' AND payload_redacted ? 'recipientOrganizationId'))))
);

CREATE INDEX ix_share_access_event_grant ON sharing.share_access_event (share_grant_id, occurred_at DESC);
CREATE INDEX ix_share_access_event_admission ON sharing.share_access_event (share_admission_id, occurred_at);

RESET ROLE;

ALTER TABLE sharing.evidence_share_grant
    ADD CONSTRAINT fk_share_grant_evidence
    FOREIGN KEY (evidence_id) REFERENCES evidence.evidence_record(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE sharing.evidence_share_grant
    ADD CONSTRAINT fk_share_grant_creator_account
    FOREIGN KEY (created_by_account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE sharing.evidence_share_grant
    ADD CONSTRAINT fk_share_grant_owner_organization
    FOREIGN KEY (owner_organization_id) REFERENCES organization_access.organization(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE sharing.share_admission
    ADD CONSTRAINT fk_share_admission_grant
    FOREIGN KEY (share_grant_id) REFERENCES sharing.evidence_share_grant(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE sharing.share_access_event
    ADD CONSTRAINT fk_share_access_event_grant
    FOREIGN KEY (share_grant_id) REFERENCES sharing.evidence_share_grant(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE sharing.share_access_event
    ADD CONSTRAINT fk_share_access_event_admission
    FOREIGN KEY (share_admission_id) REFERENCES sharing.share_admission(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

SET LOCAL ROLE sharing_owner;

CREATE FUNCTION sharing.guard_share_grant() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = sharing, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.share_id IS DISTINCT FROM OLD.share_id
        OR NEW.capability_token_hash IS DISTINCT FROM OLD.capability_token_hash
        OR NEW.evidence_id IS DISTINCT FROM OLD.evidence_id
        OR NEW.created_by_account_id IS DISTINCT FROM OLD.created_by_account_id
        OR NEW.owner_organization_id IS DISTINCT FROM OLD.owner_organization_id
        OR NEW.verification_mode IS DISTINCT FROM OLD.verification_mode
        OR NEW.valid_from IS DISTINCT FROM OLD.valid_from
        OR NEW.max_admissions IS DISTINCT FROM OLD.max_admissions
        OR NEW.download_allowed IS DISTINCT FROM OLD.download_allowed
        OR NEW.content_snapshot IS DISTINCT FROM OLD.content_snapshot
        OR NEW.content_sha256 IS DISTINCT FROM OLD.content_sha256 THEN
        RAISE EXCEPTION 'SHARE_GRANT_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    IF NEW.expires_at < OLD.expires_at THEN
        RAISE EXCEPTION 'SHARE_GRANT_EXPIRY_DECREASE' USING ERRCODE = 'P0001';
    END IF;
    IF NEW.code_generation < OLD.code_generation
        OR NEW.revocation_generation < OLD.revocation_generation THEN
        RAISE EXCEPTION 'SHARE_GRANT_GENERATION_INVALID' USING ERRCODE = 'P0001';
    END IF;
    IF NEW.code_salt IS DISTINCT FROM OLD.code_salt
        OR NEW.code_hash IS DISTINCT FROM OLD.code_hash
        OR NEW.code_generation IS DISTINCT FROM OLD.code_generation THEN
        IF NEW.code_salt IS NOT DISTINCT FROM OLD.code_salt
            OR NEW.code_hash IS NOT DISTINCT FROM OLD.code_hash
            OR NEW.code_generation IS NOT DISTINCT FROM OLD.code_generation THEN
            RAISE EXCEPTION 'SHARE_GRANT_CODE_ROTATION_INVALID' USING ERRCODE = 'P0001';
        END IF;
        IF NEW.code_generation <> OLD.code_generation + 1
            OR NEW.revocation_generation <> OLD.revocation_generation + 1 THEN
            RAISE EXCEPTION 'SHARE_GRANT_GENERATION_INVALID' USING ERRCODE = 'P0001';
        END IF;
    END IF;
    IF NEW.status IS DISTINCT FROM OLD.status THEN
        IF OLD.status IN ('EXPIRED', 'REVOKED') THEN
            RAISE EXCEPTION 'SHARE_GRANT_STATUS_TERMINAL' USING ERRCODE = 'P0001';
        END IF;
        IF NOT ((OLD.status = 'ACTIVE' AND NEW.status IN ('LOCKED', 'REVOKED', 'EXPIRED'))
            OR (OLD.status = 'LOCKED' AND NEW.status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))) THEN
            RAISE EXCEPTION 'SHARE_GRANT_STATUS_TRANSITION' USING ERRCODE = 'P0001';
        END IF;
        IF OLD.status = 'LOCKED' AND NEW.status = 'ACTIVE'
            AND (OLD.verification_mode <> 'EXTRACTION_CODE'
                OR NEW.code_salt IS NOT DISTINCT FROM OLD.code_salt
                OR NEW.code_hash IS NOT DISTINCT FROM OLD.code_hash
                OR NEW.code_generation <> OLD.code_generation + 1
                OR NEW.revocation_generation <> OLD.revocation_generation + 1) THEN
            RAISE EXCEPTION 'SHARE_GRANT_STATUS_TRANSITION' USING ERRCODE = 'P0001';
        END IF;
        IF NEW.status IN ('REVOKED', 'EXPIRED')
            AND NEW.revocation_generation <> OLD.revocation_generation + 1 THEN
            RAISE EXCEPTION 'SHARE_GRANT_GENERATION_INVALID' USING ERRCODE = 'P0001';
        END IF;
        IF NEW.status = 'LOCKED'
            AND NEW.revocation_generation <> OLD.revocation_generation THEN
            RAISE EXCEPTION 'SHARE_GRANT_GENERATION_INVALID' USING ERRCODE = 'P0001';
        END IF;
        IF NEW.status = 'EXPIRED' AND clock_timestamp() < OLD.expires_at THEN
            RAISE EXCEPTION 'SHARE_GRANT_EXPIRY_INVALID' USING ERRCODE = 'P0001';
        END IF;
    END IF;
    IF NEW.expires_at > OLD.expires_at
        AND (OLD.status <> 'ACTIVE' OR clock_timestamp() >= OLD.expires_at) THEN
        RAISE EXCEPTION 'SHARE_GRANT_EXTENSION_INVALID' USING ERRCODE = 'P0001';
    END IF;
    IF (NEW.status IS DISTINCT FROM OLD.status
        OR NEW.expires_at IS DISTINCT FROM OLD.expires_at
        OR NEW.code_salt IS DISTINCT FROM OLD.code_salt
        OR NEW.code_hash IS DISTINCT FROM OLD.code_hash
        OR NEW.code_generation IS DISTINCT FROM OLD.code_generation
        OR NEW.revocation_generation IS DISTINCT FROM OLD.revocation_generation
        OR NEW.admissions_used IS DISTINCT FROM OLD.admissions_used)
        AND NEW.revision <> OLD.revision + 1 THEN
        RAISE EXCEPTION 'SHARE_GRANT_REVISION_INVALID' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION sharing.guard_share_admission() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = sharing, pg_temp
AS $fn$
DECLARE
    parent_grant sharing.evidence_share_grant%ROWTYPE;
BEGIN
    SELECT * INTO parent_grant
    FROM sharing.evidence_share_grant
    WHERE id = NEW.share_grant_id;
    IF NOT FOUND
        OR NEW.verification_mode IS DISTINCT FROM parent_grant.verification_mode
        OR parent_grant.status <> 'ACTIVE'
        OR clock_timestamp() < parent_grant.valid_from
        OR clock_timestamp() >= parent_grant.expires_at
        OR (parent_grant.max_admissions IS NOT NULL
            AND parent_grant.admissions_used >= parent_grant.max_admissions)
        OR NEW.admission_no IS DISTINCT FROM parent_grant.admissions_used + 1 THEN
        RAISE EXCEPTION 'SHARE_ADMISSION_REFUSED' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION sharing.reject_row_mutation() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = sharing, pg_temp
AS $fn$
BEGIN
    RAISE EXCEPTION 'SHARING_ROW_APPEND_ONLY' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE TRIGGER trg_share_grant_guard
BEFORE UPDATE ON sharing.evidence_share_grant
FOR EACH ROW EXECUTE FUNCTION sharing.guard_share_grant();

CREATE TRIGGER trg_share_admission_guard
BEFORE INSERT ON sharing.share_admission
FOR EACH ROW EXECUTE FUNCTION sharing.guard_share_admission();

CREATE TRIGGER trg_share_admission_immutable
BEFORE UPDATE OR DELETE ON sharing.share_admission
FOR EACH ROW EXECUTE FUNCTION sharing.reject_row_mutation();

CREATE TRIGGER trg_share_access_event_immutable
BEFORE UPDATE OR DELETE ON sharing.share_access_event
FOR EACH ROW EXECUTE FUNCTION sharing.reject_row_mutation();

REVOKE ALL ON TABLE sharing.evidence_share_grant FROM PUBLIC;
GRANT SELECT, INSERT ON TABLE sharing.evidence_share_grant TO sharing_role;
REVOKE ALL ON TABLE sharing.share_admission FROM PUBLIC;
GRANT SELECT, INSERT ON TABLE sharing.share_admission TO sharing_role;
REVOKE ALL ON TABLE sharing.share_access_event FROM PUBLIC;
GRANT SELECT, INSERT ON TABLE sharing.share_access_event TO sharing_role;
GRANT UPDATE (status, admissions_used, revocation_generation, revision, expires_at,
    code_salt, code_hash, code_generation)
    ON TABLE sharing.evidence_share_grant TO sharing_role;

REVOKE ALL ON FUNCTION sharing.guard_share_grant() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION sharing.guard_share_grant() TO sharing_role;
REVOKE ALL ON FUNCTION sharing.guard_share_admission() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION sharing.guard_share_admission() TO sharing_role;
REVOKE ALL ON FUNCTION sharing.reject_row_mutation() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION sharing.reject_row_mutation() TO sharing_role;

RESET ROLE;

SET LOCAL ROLE evidence_owner;

GRANT USAGE ON SCHEMA evidence TO sharing_role;
GRANT SELECT (id, subject_id, status, source_type, source_ref_id, call_id, session_id, reservation_id, identity_at_capture, created_at)
    ON TABLE evidence.evidence_record TO sharing_role;
GRANT SELECT (evidence_id, media_object_id, artifact_role)
    ON TABLE evidence.evidence_artifact TO sharing_role;

RESET ROLE;

SET LOCAL ROLE customer_iam_owner;

GRANT USAGE ON SCHEMA customer_iam TO sharing_role;
GRANT SELECT (id, status)
    ON TABLE customer_iam.customer_account TO sharing_role;

RESET ROLE;

SET LOCAL ROLE organization_access_owner;

GRANT USAGE ON SCHEMA organization_access TO sharing_role;
GRANT SELECT (id, status)
    ON TABLE organization_access.organization TO sharing_role;
GRANT SELECT (organization_id, account_id, role, status)
    ON TABLE organization_access.organization_membership TO sharing_role;

RESET ROLE;

SET LOCAL ROLE subject_number_owner;

GRANT USAGE ON SCHEMA subject_number TO sharing_role;
GRANT SELECT (id, owner_account_id, owner_organization_id, revision)
    ON TABLE subject_number.business_subject TO sharing_role;
GRANT SELECT (id, subject_id, status, verified_at, revision)
    ON TABLE subject_number.caller_number_binding TO sharing_role;
GRANT SELECT (caller_number_binding_id, risk_type, state, revision)
    ON TABLE subject_number.subject_risk_state TO sharing_role;

RESET ROLE;

SET LOCAL ROLE platform_reliability_owner;

GRANT USAGE ON SCHEMA platform TO sharing_role;
GRANT EXECUTE ON FUNCTION platform.outbox_append TO sharing_role;

RESET ROLE;

SET LOCAL ROLE audit_owner;

GRANT USAGE ON SCHEMA audit TO sharing_role;
GRANT EXECUTE ON FUNCTION audit.append_fact TO sharing_role;

RESET ROLE;
