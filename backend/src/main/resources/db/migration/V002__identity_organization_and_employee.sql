-- PA2 V002: identity, organization and employee foundation.
-- Creates the six frozen V002 tables, four ordinary immediate FKs, six local
-- immutability guards and the exact owned/cross-module grants. No V003+ object,
-- no role/schema, no application service function, no issue_first_trial.

SET LOCAL ROLE customer_iam_owner;

CREATE TABLE customer_iam.customer_account (
    id uuid NOT NULL,
    display_name text NOT NULL,
    status text NOT NULL DEFAULT 'ACTIVE',
    revision integer NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_customer_account PRIMARY KEY (id),
    CONSTRAINT ck_customer_account_display_name CHECK (length(display_name) BETWEEN 1 AND 200 AND btrim(display_name) <> ''),
    CONSTRAINT ck_customer_account_status CHECK (status IN ('ACTIVE','DISABLED')),
    CONSTRAINT ck_customer_account_revision CHECK (revision >= 1),
    CONSTRAINT ck_customer_account_time_order CHECK (updated_at >= created_at)
);

CREATE INDEX ix_customer_account_status ON customer_iam.customer_account (status);

CREATE TABLE customer_iam.auth_identity (
    id uuid NOT NULL,
    account_id uuid NOT NULL,
    provider text NOT NULL,
    provider_scope text NOT NULL,
    provider_subject_ciphertext text NOT NULL,
    provider_subject_hmac text NOT NULL,
    status text NOT NULL DEFAULT 'ACTIVE',
    verified_at timestamptz NOT NULL,
    key_version integer NOT NULL,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_auth_identity PRIMARY KEY (id),
    CONSTRAINT uq_auth_identity_subject UNIQUE (provider, provider_scope, provider_subject_hmac),
    CONSTRAINT ck_auth_identity_provider CHECK (provider IN ('WECHAT','PHONE')),
    CONSTRAINT ck_auth_identity_scope CHECK (length(provider_scope) BETWEEN 1 AND 128 AND btrim(provider_scope) <> ''),
    CONSTRAINT ck_auth_identity_ciphertext CHECK (provider_subject_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_auth_identity_hmac CHECK (provider_subject_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_auth_identity_status CHECK (status IN ('ACTIVE','DISABLED')),
    CONSTRAINT ck_auth_identity_key_version CHECK (key_version >= 1),
    CONSTRAINT ck_auth_identity_revision CHECK (revision >= 1)
);

CREATE INDEX ix_auth_identity_account_status ON customer_iam.auth_identity (account_id, status);

-- docs/14 4.8 lists the canonical named check as ck_identity_profile_state_shape.
-- The verified_at table row abbreviates it as ck_identity_profile_verified_shape;
-- that alias is not a separate constraint.
CREATE TABLE customer_iam.identity_profile (
    id uuid NOT NULL,
    account_id uuid NOT NULL,
    real_name_status text NOT NULL DEFAULT 'UNVERIFIED',
    legal_name_ciphertext text,
    id_number_ciphertext text,
    id_number_hmac text,
    verification_ref_ciphertext text,
    verified_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_identity_profile PRIMARY KEY (id),
    CONSTRAINT uq_identity_profile_account UNIQUE (account_id),
    CONSTRAINT ck_identity_profile_status CHECK (real_name_status IN ('UNVERIFIED','VERIFIED')),
    CONSTRAINT ck_identity_profile_legal_ciphertext CHECK (legal_name_ciphertext IS NULL OR legal_name_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_identity_profile_id_ciphertext CHECK (id_number_ciphertext IS NULL OR id_number_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_identity_profile_id_hmac CHECK (id_number_hmac IS NULL OR id_number_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_identity_profile_verification_ref CHECK (verification_ref_ciphertext IS NULL OR verification_ref_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_identity_profile_state_shape CHECK (
        (real_name_status = 'UNVERIFIED' AND legal_name_ciphertext IS NULL AND id_number_ciphertext IS NULL
            AND id_number_hmac IS NULL AND verification_ref_ciphertext IS NULL AND verified_at IS NULL)
        OR
        (real_name_status = 'VERIFIED' AND legal_name_ciphertext IS NOT NULL AND id_number_ciphertext IS NOT NULL
            AND id_number_hmac IS NOT NULL AND verification_ref_ciphertext IS NOT NULL AND verified_at IS NOT NULL)
    ),
    CONSTRAINT ck_identity_profile_revision CHECK (revision >= 1)
);

RESET ROLE;

SET LOCAL ROLE organization_access_owner;

CREATE TABLE organization_access.organization (
    id uuid NOT NULL,
    display_name text NOT NULL,
    status text NOT NULL DEFAULT 'ACTIVE',
    revision integer NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_organization PRIMARY KEY (id),
    CONSTRAINT ck_organization_display_name CHECK (length(display_name) BETWEEN 1 AND 200 AND btrim(display_name) <> ''),
    CONSTRAINT ck_organization_status CHECK (status IN ('ACTIVE','SUSPENDED','DISABLED')),
    CONSTRAINT ck_organization_revision CHECK (revision >= 1),
    CONSTRAINT ck_organization_time_order CHECK (updated_at >= created_at)
);

CREATE INDEX ix_organization_status ON organization_access.organization (status);

CREATE TABLE organization_access.organization_membership (
    id uuid NOT NULL,
    organization_id uuid NOT NULL,
    account_id uuid NOT NULL,
    role text NOT NULL,
    department_label text,
    status text NOT NULL DEFAULT 'ACTIVE',
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_organization_membership PRIMARY KEY (id),
    CONSTRAINT ck_membership_role CHECK (role IN ('ADMINISTRATOR','MEMBER','EVIDENCE_MANAGER','AUDITOR')),
    CONSTRAINT ck_membership_department CHECK (department_label IS NULL OR (length(department_label) BETWEEN 1 AND 200 AND btrim(department_label) <> '')),
    CONSTRAINT ck_membership_status CHECK (status IN ('ACTIVE','SUSPENDED','REVOKED')),
    CONSTRAINT ck_membership_revision CHECK (revision >= 1)
);

CREATE UNIQUE INDEX uq_membership_live ON organization_access.organization_membership (organization_id, account_id) WHERE status <> 'REVOKED';
CREATE INDEX ix_membership_account_status ON organization_access.organization_membership (account_id, status);
CREATE INDEX ix_membership_organization_status_role ON organization_access.organization_membership (organization_id, status, role);

RESET ROLE;

SET LOCAL ROLE operations_iam_owner;

CREATE TABLE operations_iam.employee_account (
    id uuid NOT NULL,
    login_name_hmac text NOT NULL,
    login_name_masked text NOT NULL,
    auth_issuer text NOT NULL,
    auth_subject_hmac text NOT NULL,
    display_name text NOT NULL,
    role text NOT NULL,
    status text NOT NULL DEFAULT 'ACTIVE',
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_employee_account PRIMARY KEY (id),
    CONSTRAINT uq_employee_login_hmac UNIQUE (login_name_hmac),
    CONSTRAINT uq_employee_issuer_subject UNIQUE (auth_issuer, auth_subject_hmac),
    CONSTRAINT ck_employee_login_hmac CHECK (login_name_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_employee_login_masked CHECK (length(login_name_masked) BETWEEN 1 AND 128 AND btrim(login_name_masked) <> ''),
    CONSTRAINT ck_employee_issuer CHECK (length(auth_issuer) BETWEEN 1 AND 128 AND btrim(auth_issuer) <> ''),
    CONSTRAINT ck_employee_subject_hmac CHECK (auth_subject_hmac ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_employee_display_name CHECK (length(display_name) BETWEEN 1 AND 200 AND btrim(display_name) <> ''),
    CONSTRAINT ck_employee_role CHECK (role IN ('ADMIN','OPS')),
    CONSTRAINT ck_employee_status CHECK (status IN ('ACTIVE','DISABLED')),
    CONSTRAINT ck_employee_revision CHECK (revision >= 1)
);

CREATE INDEX ix_employee_enabled_admin ON operations_iam.employee_account (id) WHERE role = 'ADMIN' AND status = 'ACTIVE';
CREATE INDEX ix_employee_auth_issuer_status ON operations_iam.employee_account (auth_issuer, status);

RESET ROLE;

ALTER TABLE customer_iam.auth_identity
    ADD CONSTRAINT fk_auth_identity_account
    FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE customer_iam.identity_profile
    ADD CONSTRAINT fk_identity_profile_account
    FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE organization_access.organization_membership
    ADD CONSTRAINT fk_membership_organization
    FOREIGN KEY (organization_id) REFERENCES organization_access.organization(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE organization_access.organization_membership
    ADD CONSTRAINT fk_membership_account
    FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

SET LOCAL ROLE customer_iam_owner;

CREATE FUNCTION customer_iam.guard_customer_account() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = customer_iam, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'CUSTOMER_ACCOUNT_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION customer_iam.guard_auth_identity() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = customer_iam, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.account_id IS DISTINCT FROM OLD.account_id
        OR NEW.provider IS DISTINCT FROM OLD.provider
        OR NEW.provider_scope IS DISTINCT FROM OLD.provider_scope THEN
        RAISE EXCEPTION 'AUTH_IDENTITY_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    IF (NEW.provider_subject_ciphertext IS DISTINCT FROM OLD.provider_subject_ciphertext
            OR NEW.provider_subject_hmac IS DISTINCT FROM OLD.provider_subject_hmac)
        AND NOT (
            NEW.provider_subject_ciphertext IS DISTINCT FROM OLD.provider_subject_ciphertext
            AND NEW.provider_subject_hmac IS DISTINCT FROM OLD.provider_subject_hmac
            AND NEW.key_version = OLD.key_version + 1
        ) THEN
        RAISE EXCEPTION 'AUTH_IDENTITY_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION customer_iam.guard_identity_profile() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = customer_iam, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id OR NEW.account_id IS DISTINCT FROM OLD.account_id THEN
        RAISE EXCEPTION 'IDENTITY_PROFILE_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE TRIGGER trg_customer_account_guard
BEFORE UPDATE ON customer_iam.customer_account
FOR EACH ROW EXECUTE FUNCTION customer_iam.guard_customer_account();

CREATE TRIGGER trg_auth_identity_guard
BEFORE UPDATE ON customer_iam.auth_identity
FOR EACH ROW EXECUTE FUNCTION customer_iam.guard_auth_identity();

CREATE TRIGGER trg_identity_profile_guard
BEFORE UPDATE ON customer_iam.identity_profile
FOR EACH ROW EXECUTE FUNCTION customer_iam.guard_identity_profile();

REVOKE ALL ON TABLE customer_iam.customer_account FROM PUBLIC;
REVOKE ALL ON TABLE customer_iam.auth_identity FROM PUBLIC;
REVOKE ALL ON TABLE customer_iam.identity_profile FROM PUBLIC;
REVOKE ALL ON FUNCTION customer_iam.guard_customer_account() FROM PUBLIC;
REVOKE ALL ON FUNCTION customer_iam.guard_auth_identity() FROM PUBLIC;
REVOKE ALL ON FUNCTION customer_iam.guard_identity_profile() FROM PUBLIC;

GRANT USAGE ON SCHEMA customer_iam TO customer_iam_role, subject_number_role;

GRANT SELECT, INSERT ON TABLE customer_iam.customer_account TO customer_iam_role;
GRANT UPDATE (display_name, status, revision, updated_at) ON TABLE customer_iam.customer_account TO customer_iam_role;
GRANT SELECT (id, status) ON TABLE customer_iam.customer_account TO subject_number_role;

GRANT SELECT, INSERT ON TABLE customer_iam.auth_identity TO customer_iam_role;
GRANT UPDATE (status, verified_at, key_version, revision) ON TABLE customer_iam.auth_identity TO customer_iam_role;

GRANT SELECT, INSERT ON TABLE customer_iam.identity_profile TO customer_iam_role;
GRANT UPDATE (real_name_status, legal_name_ciphertext, id_number_ciphertext, id_number_hmac,
    verification_ref_ciphertext, verified_at, revision) ON TABLE customer_iam.identity_profile TO customer_iam_role;

GRANT EXECUTE ON FUNCTION customer_iam.guard_customer_account() TO customer_iam_role;
GRANT EXECUTE ON FUNCTION customer_iam.guard_auth_identity() TO customer_iam_role;
GRANT EXECUTE ON FUNCTION customer_iam.guard_identity_profile() TO customer_iam_role;

RESET ROLE;

SET LOCAL ROLE organization_access_owner;

CREATE FUNCTION organization_access.guard_organization() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = organization_access, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'ORGANIZATION_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION organization_access.guard_membership() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = organization_access, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.organization_id IS DISTINCT FROM OLD.organization_id
        OR NEW.account_id IS DISTINCT FROM OLD.account_id THEN
        RAISE EXCEPTION 'MEMBERSHIP_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE TRIGGER trg_organization_guard
BEFORE UPDATE ON organization_access.organization
FOR EACH ROW EXECUTE FUNCTION organization_access.guard_organization();

CREATE TRIGGER trg_membership_guard
BEFORE UPDATE ON organization_access.organization_membership
FOR EACH ROW EXECUTE FUNCTION organization_access.guard_membership();

REVOKE ALL ON TABLE organization_access.organization FROM PUBLIC;
REVOKE ALL ON TABLE organization_access.organization_membership FROM PUBLIC;
REVOKE ALL ON FUNCTION organization_access.guard_organization() FROM PUBLIC;
REVOKE ALL ON FUNCTION organization_access.guard_membership() FROM PUBLIC;

GRANT USAGE ON SCHEMA organization_access TO organization_access_role, customer_iam_role, subject_number_role;

GRANT SELECT, INSERT ON TABLE organization_access.organization TO organization_access_role;
GRANT UPDATE (display_name, status, revision, updated_at) ON TABLE organization_access.organization TO organization_access_role;
GRANT SELECT (id, display_name, status) ON TABLE organization_access.organization TO customer_iam_role;
GRANT SELECT (id, status) ON TABLE organization_access.organization TO subject_number_role;

GRANT SELECT, INSERT ON TABLE organization_access.organization_membership TO organization_access_role;
GRANT UPDATE (role, department_label, status, revision) ON TABLE organization_access.organization_membership TO organization_access_role;
GRANT SELECT (organization_id, account_id, role, status) ON TABLE organization_access.organization_membership TO customer_iam_role;
GRANT SELECT (organization_id, account_id, role, status) ON TABLE organization_access.organization_membership TO subject_number_role;

GRANT EXECUTE ON FUNCTION organization_access.guard_organization() TO organization_access_role;
GRANT EXECUTE ON FUNCTION organization_access.guard_membership() TO organization_access_role;

RESET ROLE;

SET LOCAL ROLE operations_iam_owner;

CREATE FUNCTION operations_iam.guard_employee_account() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = operations_iam, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.login_name_hmac IS DISTINCT FROM OLD.login_name_hmac
        OR NEW.auth_issuer IS DISTINCT FROM OLD.auth_issuer
        OR NEW.auth_subject_hmac IS DISTINCT FROM OLD.auth_subject_hmac THEN
        RAISE EXCEPTION 'EMPLOYEE_ACCOUNT_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE TRIGGER trg_employee_account_guard
BEFORE UPDATE ON operations_iam.employee_account
FOR EACH ROW EXECUTE FUNCTION operations_iam.guard_employee_account();

REVOKE ALL ON TABLE operations_iam.employee_account FROM PUBLIC;
REVOKE ALL ON FUNCTION operations_iam.guard_employee_account() FROM PUBLIC;

GRANT USAGE ON SCHEMA operations_iam TO operations_iam_role;

GRANT SELECT, INSERT ON TABLE operations_iam.employee_account TO operations_iam_role;
GRANT UPDATE (display_name, role, status, revision) ON TABLE operations_iam.employee_account TO operations_iam_role;

GRANT EXECUTE ON FUNCTION operations_iam.guard_employee_account() TO operations_iam_role;

RESET ROLE;
