-- PA3 V003: subject number and claim foundation.
-- Creates the four frozen V003 tables, seven ordinary immediate FKs (added by
-- the migration identity after RESET ROLE), three local immutability guards,
-- one append-only claim control and the exact owned/cross-role grants. No
-- later-version object, no application service function, no role/schema creation.

SET LOCAL ROLE subject_number_owner;

CREATE TABLE subject_number.business_subject (
    id uuid NOT NULL,
    owner_account_id uuid,
    owner_organization_id uuid,
    source text NOT NULL,
    real_name_status text NOT NULL DEFAULT 'UNVERIFIED',
    real_name_verified_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_business_subject PRIMARY KEY (id),
    CONSTRAINT ck_subject_single_owner CHECK (num_nonnulls(owner_account_id, owner_organization_id) <= 1),
    CONSTRAINT ck_subject_source CHECK (source IN ('IVR', 'MINIAPP', 'WEB', 'OPERATIONS')),
    CONSTRAINT ck_subject_real_name_status CHECK (real_name_status IN ('UNVERIFIED', 'VERIFIED')),
    CONSTRAINT ck_subject_real_name_shape CHECK ((real_name_status = 'VERIFIED') = (real_name_verified_at IS NOT NULL)),
    CONSTRAINT ck_subject_revision CHECK (revision >= 1),
    CONSTRAINT ck_subject_time_order CHECK (updated_at >= created_at)
);

CREATE INDEX ix_subject_owner_account ON subject_number.business_subject (owner_account_id);
CREATE INDEX ix_subject_owner_organization ON subject_number.business_subject (owner_organization_id);
CREATE INDEX ix_subject_source_status ON subject_number.business_subject (source, real_name_status);

CREATE TABLE subject_number.caller_number_binding (
    id uuid NOT NULL,
    subject_id uuid NOT NULL,
    number_type text NOT NULL,
    number_ciphertext text NOT NULL,
    number_hmac text NOT NULL,
    number_masked text NOT NULL,
    status text NOT NULL DEFAULT 'ACTIVE',
    verified_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_caller_number_binding PRIMARY KEY (id),
    CONSTRAINT uq_binding_subject_number UNIQUE (subject_id, number_hmac),
    CONSTRAINT ck_binding_number_type CHECK (number_type IN ('MOBILE', 'LANDLINE')),
    CONSTRAINT ck_binding_number_ciphertext CHECK (number_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_binding_number_hmac CHECK (number_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_binding_number_masked CHECK (length(number_masked) BETWEEN 1 AND 64 AND btrim(number_masked) <> ''),
    CONSTRAINT ck_binding_status CHECK (status IN ('ACTIVE', 'DISABLED', 'RETIRED')),
    CONSTRAINT ck_binding_verified_shape CHECK (status <> 'ACTIVE' OR verified_at IS NOT NULL),
    CONSTRAINT ck_binding_revision CHECK (revision >= 1)
);

CREATE INDEX ix_binding_subject_status ON subject_number.caller_number_binding (subject_id, status);
CREATE INDEX ix_binding_hmac_status ON subject_number.caller_number_binding (number_hmac, status);
CREATE UNIQUE INDEX uq_binding_active_number ON subject_number.caller_number_binding (number_hmac) WHERE status = 'ACTIVE';

CREATE TABLE subject_number.subject_claim (
    id uuid NOT NULL,
    subject_id uuid NOT NULL,
    account_id uuid NOT NULL,
    organization_id uuid,
    verification_method text NOT NULL,
    verification_ref_ciphertext text NOT NULL,
    request_id text NOT NULL,
    status text NOT NULL,
    risk_checked_at timestamptz NOT NULL,
    verified_at timestamptz,
    CONSTRAINT pk_subject_claim PRIMARY KEY (id),
    CONSTRAINT uq_claim_request UNIQUE (request_id),
    CONSTRAINT ck_claim_verification_method CHECK (verification_method IN ('SMS', 'WECHAT_PHONE', 'CALLER_CHALLENGE', 'MANUAL_REVIEW')),
    CONSTRAINT ck_claim_verification_ref CHECK (verification_ref_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_claim_request_id CHECK (length(request_id) BETWEEN 1 AND 128 AND btrim(request_id) <> ''),
    CONSTRAINT ck_claim_status CHECK (status IN ('ACCEPTED', 'REJECTED', 'CONFLICT')),
    CONSTRAINT ck_claim_verified_shape CHECK ((status = 'ACCEPTED') = (verified_at IS NOT NULL)),
    CONSTRAINT ck_claim_verified_after_risk CHECK (verified_at IS NULL OR verified_at >= risk_checked_at)
);

CREATE INDEX ix_claim_subject_status_time ON subject_number.subject_claim (subject_id, status, risk_checked_at DESC);
CREATE INDEX ix_claim_account_status ON subject_number.subject_claim (account_id, status);
CREATE UNIQUE INDEX uq_claim_accepted_subject_account ON subject_number.subject_claim (subject_id, account_id) WHERE status = 'ACCEPTED';

CREATE TABLE subject_number.subject_risk_state (
    id uuid NOT NULL,
    caller_number_binding_id uuid NOT NULL,
    risk_type text NOT NULL,
    state text NOT NULL DEFAULT 'OPEN',
    source text NOT NULL,
    observed_at timestamptz NOT NULL DEFAULT now(),
    cleared_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_subject_risk_state PRIMARY KEY (id),
    CONSTRAINT uq_risk_binding_type UNIQUE (caller_number_binding_id, risk_type),
    CONSTRAINT ck_risk_type CHECK (risk_type IN ('RECYCLED', 'CONFLICT', 'MANUAL_REVIEW')),
    CONSTRAINT ck_risk_state CHECK (state IN ('OPEN', 'CLEARED')),
    CONSTRAINT ck_risk_source CHECK (source IN ('CLAIM_CHECK', 'OPERATIONS', 'IMPORT')),
    CONSTRAINT ck_risk_cleared_shape CHECK ((state = 'CLEARED') = (cleared_at IS NOT NULL)),
    CONSTRAINT ck_risk_cleared_after_observed CHECK (cleared_at IS NULL OR cleared_at >= observed_at),
    CONSTRAINT ck_risk_revision CHECK (revision >= 1)
);

CREATE INDEX ix_risk_open ON subject_number.subject_risk_state (caller_number_binding_id, observed_at) WHERE state = 'OPEN';
CREATE INDEX ix_risk_binding_state ON subject_number.subject_risk_state (caller_number_binding_id, state);

CREATE FUNCTION subject_number.reject_row_mutation() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = subject_number, pg_temp
AS $fn$
BEGIN
    RAISE EXCEPTION 'SUBJECT_CLAIM_APPEND_ONLY' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE FUNCTION subject_number.guard_subject_owner() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = subject_number, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.source IS DISTINCT FROM OLD.source
        OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'SUBJECT_OWNER_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    IF OLD.owner_account_id IS NOT NULL
        AND NEW.owner_account_id IS DISTINCT FROM OLD.owner_account_id THEN
        RAISE EXCEPTION 'SUBJECT_OWNER_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    IF OLD.owner_organization_id IS NOT NULL
        AND NEW.owner_organization_id IS DISTINCT FROM OLD.owner_organization_id THEN
        RAISE EXCEPTION 'SUBJECT_OWNER_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    IF (NEW.real_name_status IS DISTINCT FROM OLD.real_name_status
            OR NEW.real_name_verified_at IS DISTINCT FROM OLD.real_name_verified_at)
        AND current_user <> 'subject_identity_verification_role' THEN
        RAISE EXCEPTION 'SUBJECT_OWNER_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION subject_number.guard_caller_number_binding() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = subject_number, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.subject_id IS DISTINCT FROM OLD.subject_id
        OR NEW.number_type IS DISTINCT FROM OLD.number_type
        OR NEW.number_ciphertext IS DISTINCT FROM OLD.number_ciphertext
        OR NEW.number_hmac IS DISTINCT FROM OLD.number_hmac
        OR NEW.number_masked IS DISTINCT FROM OLD.number_masked THEN
        RAISE EXCEPTION 'CALLER_NUMBER_BINDING_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION subject_number.guard_subject_risk_state() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = subject_number, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.caller_number_binding_id IS DISTINCT FROM OLD.caller_number_binding_id
        OR NEW.risk_type IS DISTINCT FROM OLD.risk_type THEN
        RAISE EXCEPTION 'SUBJECT_RISK_STATE_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE TRIGGER trg_business_subject_guard
BEFORE UPDATE ON subject_number.business_subject
FOR EACH ROW EXECUTE FUNCTION subject_number.guard_subject_owner();

CREATE TRIGGER trg_caller_number_binding_guard
BEFORE UPDATE ON subject_number.caller_number_binding
FOR EACH ROW EXECUTE FUNCTION subject_number.guard_caller_number_binding();

CREATE TRIGGER trg_subject_risk_state_guard
BEFORE UPDATE ON subject_number.subject_risk_state
FOR EACH ROW EXECUTE FUNCTION subject_number.guard_subject_risk_state();

CREATE TRIGGER trg_subject_claim_immutable
BEFORE UPDATE OR DELETE ON subject_number.subject_claim
FOR EACH ROW EXECUTE FUNCTION subject_number.reject_row_mutation();

REVOKE ALL ON TABLE subject_number.business_subject FROM PUBLIC;
REVOKE ALL ON TABLE subject_number.caller_number_binding FROM PUBLIC;
REVOKE ALL ON TABLE subject_number.subject_claim FROM PUBLIC;
REVOKE ALL ON TABLE subject_number.subject_risk_state FROM PUBLIC;
REVOKE ALL ON FUNCTION subject_number.guard_subject_owner() FROM PUBLIC;
REVOKE ALL ON FUNCTION subject_number.guard_caller_number_binding() FROM PUBLIC;
REVOKE ALL ON FUNCTION subject_number.guard_subject_risk_state() FROM PUBLIC;
REVOKE ALL ON FUNCTION subject_number.reject_row_mutation() FROM PUBLIC;

GRANT USAGE ON SCHEMA subject_number TO subject_number_role, subject_identity_verification_role;

GRANT SELECT ON TABLE subject_number.business_subject TO subject_number_role;
GRANT INSERT (id, source, revision, created_at, updated_at) ON TABLE subject_number.business_subject TO subject_number_role;
GRANT UPDATE (owner_account_id, revision, updated_at) ON TABLE subject_number.business_subject TO subject_number_role;

GRANT SELECT (id, real_name_status, real_name_verified_at, revision, updated_at)
    ON TABLE subject_number.business_subject TO subject_identity_verification_role;
GRANT UPDATE (real_name_status, real_name_verified_at, revision, updated_at)
    ON TABLE subject_number.business_subject TO subject_identity_verification_role;

GRANT SELECT, INSERT ON TABLE subject_number.caller_number_binding TO subject_number_role;
GRANT UPDATE (status, verified_at, revision) ON TABLE subject_number.caller_number_binding TO subject_number_role;

GRANT SELECT, INSERT ON TABLE subject_number.subject_claim TO subject_number_role;

GRANT SELECT, INSERT ON TABLE subject_number.subject_risk_state TO subject_number_role;
GRANT UPDATE (state, source, observed_at, cleared_at, revision) ON TABLE subject_number.subject_risk_state TO subject_number_role;

GRANT EXECUTE ON FUNCTION subject_number.guard_subject_owner()
    TO subject_number_role, subject_identity_verification_role;
GRANT EXECUTE ON FUNCTION subject_number.guard_caller_number_binding() TO subject_number_role;
GRANT EXECUTE ON FUNCTION subject_number.guard_subject_risk_state() TO subject_number_role;

RESET ROLE;

ALTER TABLE subject_number.business_subject
    ADD CONSTRAINT fk_subject_owner_account
    FOREIGN KEY (owner_account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE subject_number.business_subject
    ADD CONSTRAINT fk_subject_owner_organization
    FOREIGN KEY (owner_organization_id) REFERENCES organization_access.organization(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE subject_number.caller_number_binding
    ADD CONSTRAINT fk_binding_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE subject_number.subject_claim
    ADD CONSTRAINT fk_claim_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE subject_number.subject_claim
    ADD CONSTRAINT fk_claim_account
    FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE subject_number.subject_claim
    ADD CONSTRAINT fk_claim_organization
    FOREIGN KEY (organization_id) REFERENCES organization_access.organization(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE subject_number.subject_risk_state
    ADD CONSTRAINT fk_risk_binding
    FOREIGN KEY (caller_number_binding_id) REFERENCES subject_number.caller_number_binding(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
