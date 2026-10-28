-- PA4 V004: catalog order and payment foundation.
-- Creates the four frozen S2 schemas and eight NOLOGIN owner/runtime roles,
-- the seven catalog_order tables (78 columns, 51 named checks, 11 unique keys,
-- 28 indexes), five local SECURITY INVOKER trigger functions, seven triggers,
-- and the exact owner/runtime/cross-module grants. Eleven ordinary immediate
-- RESTRICT FKs are added by the migration identity after RESET ROLE.
-- customer_order.purchase_consent_id is NOT NULL with no default and no FK
-- until V007. No V005+ object, no issue_first_trial, no application service.

CREATE ROLE catalog_order_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE catalog_order_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE ivr_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE ivr_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE entitlement_metering_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE entitlement_metering_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE activation_consent_owner NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE activation_consent_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;

CREATE SCHEMA catalog_order AUTHORIZATION catalog_order_owner;
CREATE SCHEMA ivr AUTHORIZATION ivr_owner;
CREATE SCHEMA entitlement_metering AUTHORIZATION entitlement_metering_owner;
CREATE SCHEMA activation_consent AUTHORIZATION activation_consent_owner;

REVOKE ALL ON SCHEMA catalog_order, ivr, entitlement_metering, activation_consent FROM PUBLIC;

SET LOCAL ROLE platform_reliability_owner;

GRANT USAGE ON SCHEMA platform
    TO catalog_order_role, ivr_role, entitlement_metering_role, activation_consent_role;
GRANT EXECUTE ON FUNCTION platform.idempotency_begin(text, text, text)
    TO catalog_order_role, ivr_role, entitlement_metering_role, activation_consent_role;
GRANT EXECUTE ON FUNCTION platform.idempotency_complete(uuid, integer, text, uuid, jsonb, text)
    TO catalog_order_role, ivr_role, entitlement_metering_role, activation_consent_role;
GRANT EXECUTE ON FUNCTION platform.outbox_append(text, text, text, uuid, text, jsonb, text, text)
    TO catalog_order_role, ivr_role, entitlement_metering_role, activation_consent_role;

RESET ROLE;

SET LOCAL ROLE audit_owner;

GRANT USAGE ON SCHEMA audit
    TO catalog_order_role, ivr_role, entitlement_metering_role, activation_consent_role;
GRANT EXECUTE ON FUNCTION audit.append_fact(timestamp with time zone, text, uuid, text, text, uuid, text, jsonb, jsonb, text, text)
    TO catalog_order_role, ivr_role, entitlement_metering_role, activation_consent_role;

RESET ROLE;

SET LOCAL ROLE catalog_order_owner;

CREATE TABLE catalog_order.catalog_product (
    id uuid NOT NULL,
    product_code text NOT NULL,
    product_family text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_catalog_product PRIMARY KEY (id),
    CONSTRAINT uq_catalog_product_code UNIQUE (product_code),
    CONSTRAINT ck_catalog_product_code CHECK (product_code ~ '^[A-Z][A-Z0-9_-]{1,63}$'),
    CONSTRAINT ck_catalog_product_family CHECK (product_family ~ '^[A-Z][A-Z0-9_]{0,63}$')
);

CREATE INDEX ix_catalog_product_family ON catalog_order.catalog_product (product_family, created_at);

CREATE TABLE catalog_order.catalog_version (
    id uuid NOT NULL,
    product_id uuid NOT NULL,
    version_no integer NOT NULL,
    sale_status text NOT NULL DEFAULT 'DRAFT',
    offer_type text NOT NULL,
    audience text NOT NULL,
    price_cents bigint,
    currency text NOT NULL,
    billing_cycle text NOT NULL,
    installments integer NOT NULL,
    grant_interval_months integer NOT NULL,
    snapshot jsonb NOT NULL,
    snapshot_sha256 text NOT NULL,
    created_by_employee_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz,
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_catalog_version PRIMARY KEY (id),
    CONSTRAINT uq_catalog_version_no UNIQUE (product_id, version_no),
    CONSTRAINT ck_catalog_version_no CHECK (version_no >= 1),
    CONSTRAINT ck_catalog_version_sale_status CHECK (sale_status IN ('DRAFT', 'ACTIVE', 'OFFSALE')),
    CONSTRAINT ck_catalog_version_offer_type CHECK (offer_type IN ('SUBSCRIPTION', 'ONE_TIME', 'ADD_ON', 'QUOTE')),
    CONSTRAINT ck_catalog_version_audience CHECK (audience IN ('PERSONAL', 'ENTERPRISE')),
    CONSTRAINT ck_catalog_version_audience_shape CHECK ((audience = 'ENTERPRISE') = (offer_type = 'QUOTE')),
    CONSTRAINT ck_catalog_version_price CHECK ((price_cents IS NULL) = (offer_type = 'QUOTE')
        AND (price_cents IS NULL OR price_cents >= 0)),
    CONSTRAINT ck_catalog_version_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_catalog_version_billing_matrix CHECK (
        (offer_type = 'SUBSCRIPTION'
            AND billing_cycle IN ('MONTHLY', 'QUARTERLY', 'ANNUAL')
            AND installments >= 1
            AND grant_interval_months >= 1)
        OR (offer_type IN ('ONE_TIME', 'ADD_ON')
            AND billing_cycle = 'ONCE'
            AND installments = 1
            AND grant_interval_months = 0)
        OR (offer_type = 'QUOTE'
            AND billing_cycle = 'QUOTE'
            AND installments = 1
            AND grant_interval_months = 0
            AND price_cents IS NULL)
    ),
    CONSTRAINT ck_catalog_version_snapshot_type CHECK (jsonb_typeof(snapshot) = 'object'),
    CONSTRAINT ck_catalog_version_snapshot_sha CHECK (snapshot_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_catalog_version_published_shape CHECK ((sale_status = 'DRAFT') = (published_at IS NULL)),
    CONSTRAINT ck_catalog_version_revision CHECK (revision >= 1)
);

CREATE UNIQUE INDEX uq_catalog_version_active ON catalog_order.catalog_version (product_id)
    WHERE sale_status = 'ACTIVE';
CREATE INDEX ix_catalog_version_product_status
    ON catalog_order.catalog_version (product_id, sale_status, version_no DESC);

CREATE TABLE catalog_order.catalog_offer (
    id uuid NOT NULL,
    catalog_version_id uuid NOT NULL,
    channel text NOT NULL,
    offer_status text NOT NULL DEFAULT 'ACTIVE',
    version_token text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_catalog_offer PRIMARY KEY (id),
    CONSTRAINT uq_catalog_offer_version_channel UNIQUE (catalog_version_id, channel),
    CONSTRAINT ck_catalog_offer_channel CHECK (channel IN ('MINIAPP', 'WEB')),
    CONSTRAINT ck_catalog_offer_status CHECK (offer_status IN ('ACTIVE', 'WITHDRAWN')),
    CONSTRAINT ck_catalog_offer_token CHECK (length(version_token) BETWEEN 1 AND 128
        AND btrim(version_token) <> ''),
    CONSTRAINT ck_catalog_offer_revision CHECK (revision >= 1)
);

CREATE INDEX ix_catalog_offer_channel_status
    ON catalog_order.catalog_offer (channel, offer_status);

CREATE TABLE catalog_order.catalog_benefit_line (
    id uuid NOT NULL,
    catalog_version_id uuid NOT NULL,
    entitlement_type text NOT NULL,
    quantity bigint NOT NULL,
    validity_days integer NOT NULL,
    grant_interval_months integer NOT NULL,
    CONSTRAINT pk_catalog_benefit_line PRIMARY KEY (id),
    CONSTRAINT uq_catalog_benefit_version_type UNIQUE (catalog_version_id, entitlement_type),
    CONSTRAINT ck_catalog_benefit_type CHECK (entitlement_type IN
        ('RECORDING_SECONDS', 'AI_SECONDS', 'EVIDENCE_COUNT', 'STORAGE_BYTES')),
    CONSTRAINT ck_catalog_benefit_quantity CHECK (quantity >= 1),
    CONSTRAINT ck_catalog_benefit_validity CHECK (validity_days >= 1),
    CONSTRAINT ck_catalog_benefit_interval CHECK (grant_interval_months >= 0)
);

CREATE INDEX ix_catalog_benefit_version
    ON catalog_order.catalog_benefit_line (catalog_version_id);

CREATE TABLE catalog_order.customer_order (
    id uuid NOT NULL,
    order_no text NOT NULL,
    account_id uuid NOT NULL,
    owner_organization_id uuid,
    subject_id uuid NOT NULL,
    channel text NOT NULL,
    catalog_version_id uuid NOT NULL,
    catalog_offer_id uuid NOT NULL,
    purchase_consent_id uuid NOT NULL,
    amount_cents bigint NOT NULL,
    currency text NOT NULL,
    payment_status text NOT NULL DEFAULT 'UNPAID',
    snapshot jsonb NOT NULL,
    snapshot_sha256 text NOT NULL,
    version_token text NOT NULL,
    idempotency_key text NOT NULL,
    request_fingerprint text NOT NULL,
    revision integer NOT NULL DEFAULT 1,
    created_at timestamptz NOT NULL DEFAULT now(),
    paid_at timestamptz,
    closed_at timestamptz,
    CONSTRAINT pk_customer_order PRIMARY KEY (id),
    CONSTRAINT uq_customer_order_no UNIQUE (order_no),
    CONSTRAINT uq_customer_order_idempotency UNIQUE (account_id, idempotency_key),
    CONSTRAINT ck_customer_order_no CHECK (length(order_no) BETWEEN 1 AND 64
        AND btrim(order_no) <> ''),
    CONSTRAINT ck_customer_order_channel CHECK (channel IN ('MINIAPP', 'WEB')),
    CONSTRAINT ck_customer_order_amount CHECK (amount_cents >= 0),
    CONSTRAINT ck_customer_order_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_customer_order_payment_status CHECK
        (payment_status IN ('UNPAID', 'UNKNOWN', 'PAID', 'CLOSED')),
    CONSTRAINT ck_customer_order_snapshot_type CHECK (jsonb_typeof(snapshot) = 'object'),
    CONSTRAINT ck_customer_order_snapshot_sha CHECK (snapshot_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_customer_order_version_token CHECK (length(version_token) BETWEEN 1 AND 128
        AND btrim(version_token) <> ''),
    CONSTRAINT ck_customer_order_idempotency CHECK (length(idempotency_key) BETWEEN 1 AND 475
        AND btrim(idempotency_key) <> ''),
    CONSTRAINT ck_customer_order_fingerprint CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_customer_order_revision CHECK (revision >= 1),
    CONSTRAINT ck_customer_order_payment_shape CHECK
        ((payment_status = 'PAID') = (paid_at IS NOT NULL)
            AND (payment_status = 'CLOSED') = (closed_at IS NOT NULL)),
    CONSTRAINT ck_customer_order_time_order CHECK
        ((paid_at IS NULL OR paid_at >= created_at)
            AND (closed_at IS NULL OR closed_at >= created_at))
);

CREATE INDEX ix_customer_order_subject_status
    ON catalog_order.customer_order (subject_id, payment_status, created_at DESC);
CREATE INDEX ix_customer_order_organization
    ON catalog_order.customer_order (owner_organization_id, created_at DESC);
CREATE INDEX ix_customer_order_version
    ON catalog_order.customer_order (catalog_version_id);

CREATE TABLE catalog_order.payment_order (
    id uuid NOT NULL,
    order_id uuid NOT NULL,
    provider text NOT NULL,
    merchant_order_no text NOT NULL,
    provider_transaction_id text,
    status text NOT NULL DEFAULT 'UNPAID',
    amount_cents bigint NOT NULL,
    currency text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    revision integer NOT NULL DEFAULT 1,
    CONSTRAINT pk_payment_order PRIMARY KEY (id),
    CONSTRAINT uq_payment_order_order UNIQUE (order_id),
    CONSTRAINT uq_payment_order_merchant UNIQUE (merchant_order_no),
    CONSTRAINT ck_payment_order_provider CHECK (provider = 'WECHAT'),
    CONSTRAINT ck_payment_order_merchant_no CHECK (length(merchant_order_no) BETWEEN 1 AND 64
        AND btrim(merchant_order_no) <> ''),
    CONSTRAINT ck_payment_order_transaction CHECK (provider_transaction_id IS NULL
        OR (length(provider_transaction_id) BETWEEN 1 AND 128
            AND btrim(provider_transaction_id) <> '')),
    CONSTRAINT ck_payment_order_status CHECK
        (status IN ('UNPAID', 'UNKNOWN', 'PAID', 'CLOSED')),
    CONSTRAINT ck_payment_order_paid_shape CHECK (status <> 'PAID'
        OR provider_transaction_id IS NOT NULL),
    CONSTRAINT ck_payment_order_amount CHECK (amount_cents >= 0),
    CONSTRAINT ck_payment_order_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_payment_order_time_order CHECK (updated_at >= created_at),
    CONSTRAINT ck_payment_order_revision CHECK (revision >= 1)
);

CREATE UNIQUE INDEX uq_payment_order_transaction
    ON catalog_order.payment_order (provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;
CREATE INDEX ix_payment_order_status_time
    ON catalog_order.payment_order (status, updated_at);

CREATE TABLE catalog_order.payment_event (
    id uuid NOT NULL,
    payment_order_id uuid NOT NULL,
    provider text NOT NULL,
    provider_event_id text NOT NULL,
    event_type text NOT NULL,
    signature_verified boolean NOT NULL DEFAULT false,
    payload_ciphertext text NOT NULL,
    payload_sha256 text NOT NULL,
    transaction_id text,
    amount_cents bigint NOT NULL,
    occurred_at timestamptz NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_payment_event PRIMARY KEY (id),
    CONSTRAINT uq_payment_event_provider_event UNIQUE (provider, provider_event_id),
    CONSTRAINT ck_payment_event_provider CHECK (provider = 'WECHAT'),
    CONSTRAINT ck_payment_event_event_id CHECK (length(provider_event_id) BETWEEN 1 AND 128
        AND btrim(provider_event_id) <> ''),
    CONSTRAINT ck_payment_event_type CHECK (length(event_type) BETWEEN 1 AND 64
        AND btrim(event_type) <> ''),
    CONSTRAINT ck_payment_event_payload_ciphertext CHECK
        (payload_ciphertext ~ '^v1\.[A-Za-z0-9._:/-]+\..+$'),
    CONSTRAINT ck_payment_event_payload_sha CHECK (payload_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_payment_event_transaction CHECK (transaction_id IS NULL
        OR (length(transaction_id) BETWEEN 1 AND 128 AND btrim(transaction_id) <> '')),
    CONSTRAINT ck_payment_event_amount CHECK (amount_cents >= 0)
);

CREATE INDEX ix_payment_event_order_time
    ON catalog_order.payment_event (payment_order_id, received_at DESC);
CREATE INDEX ix_payment_event_verified
    ON catalog_order.payment_event (payment_order_id, received_at) WHERE signature_verified;

CREATE FUNCTION catalog_order.reject_row_mutation() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = catalog_order, pg_temp
AS $fn$
BEGIN
    RAISE EXCEPTION 'CATALOG_ROW_APPEND_ONLY' USING ERRCODE = 'P0001';
END;
$fn$;

CREATE FUNCTION catalog_order.guard_catalog_version_published() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = catalog_order, pg_temp
AS $fn$
BEGIN
    IF OLD.published_at IS NOT NULL THEN
        IF NEW.product_id IS DISTINCT FROM OLD.product_id
            OR NEW.version_no IS DISTINCT FROM OLD.version_no
            OR NEW.offer_type IS DISTINCT FROM OLD.offer_type
            OR NEW.audience IS DISTINCT FROM OLD.audience
            OR NEW.price_cents IS DISTINCT FROM OLD.price_cents
            OR NEW.currency IS DISTINCT FROM OLD.currency
            OR NEW.billing_cycle IS DISTINCT FROM OLD.billing_cycle
            OR NEW.installments IS DISTINCT FROM OLD.installments
            OR NEW.grant_interval_months IS DISTINCT FROM OLD.grant_interval_months
            OR NEW.snapshot IS DISTINCT FROM OLD.snapshot
            OR NEW.snapshot_sha256 IS DISTINCT FROM OLD.snapshot_sha256
            OR NEW.created_by_employee_id IS DISTINCT FROM OLD.created_by_employee_id
            OR NEW.created_at IS DISTINCT FROM OLD.created_at
            OR NEW.published_at IS DISTINCT FROM OLD.published_at THEN
            RAISE EXCEPTION 'CATALOG_VERSION_PUBLISHED_IMMUTABLE' USING ERRCODE = 'P0001';
        END IF;
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION catalog_order.guard_catalog_offer() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = catalog_order, pg_temp
AS $fn$
BEGIN
    IF NEW.catalog_version_id IS DISTINCT FROM OLD.catalog_version_id
        OR NEW.channel IS DISTINCT FROM OLD.channel THEN
        RAISE EXCEPTION 'CATALOG_OFFER_RELATION_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION catalog_order.guard_customer_order_snapshot() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = catalog_order, pg_temp
AS $fn$
BEGIN
    IF NEW.id IS DISTINCT FROM OLD.id
        OR NEW.order_no IS DISTINCT FROM OLD.order_no
        OR NEW.account_id IS DISTINCT FROM OLD.account_id
        OR NEW.owner_organization_id IS DISTINCT FROM OLD.owner_organization_id
        OR NEW.subject_id IS DISTINCT FROM OLD.subject_id
        OR NEW.channel IS DISTINCT FROM OLD.channel
        OR NEW.catalog_version_id IS DISTINCT FROM OLD.catalog_version_id
        OR NEW.catalog_offer_id IS DISTINCT FROM OLD.catalog_offer_id
        OR NEW.purchase_consent_id IS DISTINCT FROM OLD.purchase_consent_id
        OR NEW.amount_cents IS DISTINCT FROM OLD.amount_cents
        OR NEW.currency IS DISTINCT FROM OLD.currency
        OR NEW.snapshot IS DISTINCT FROM OLD.snapshot
        OR NEW.snapshot_sha256 IS DISTINCT FROM OLD.snapshot_sha256
        OR NEW.version_token IS DISTINCT FROM OLD.version_token
        OR NEW.idempotency_key IS DISTINCT FROM OLD.idempotency_key
        OR NEW.request_fingerprint IS DISTINCT FROM OLD.request_fingerprint
        OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'CUSTOMER_ORDER_SNAPSHOT_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE FUNCTION catalog_order.guard_payment_order_identity() RETURNS trigger
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = catalog_order, pg_temp
AS $fn$
BEGIN
    IF NEW.order_id IS DISTINCT FROM OLD.order_id
        OR NEW.provider IS DISTINCT FROM OLD.provider
        OR NEW.merchant_order_no IS DISTINCT FROM OLD.merchant_order_no
        OR NEW.amount_cents IS DISTINCT FROM OLD.amount_cents
        OR NEW.currency IS DISTINCT FROM OLD.currency
        OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'PAYMENT_ORDER_IDENTITY_IMMUTABLE' USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$fn$;

CREATE TRIGGER trg_catalog_product_immutable
BEFORE UPDATE OR DELETE ON catalog_order.catalog_product
FOR EACH ROW EXECUTE FUNCTION catalog_order.reject_row_mutation();

CREATE TRIGGER trg_catalog_version_guard
BEFORE UPDATE ON catalog_order.catalog_version
FOR EACH ROW EXECUTE FUNCTION catalog_order.guard_catalog_version_published();

CREATE TRIGGER trg_catalog_offer_guard
BEFORE UPDATE ON catalog_order.catalog_offer
FOR EACH ROW EXECUTE FUNCTION catalog_order.guard_catalog_offer();

CREATE TRIGGER trg_catalog_benefit_line_immutable
BEFORE UPDATE OR DELETE ON catalog_order.catalog_benefit_line
FOR EACH ROW EXECUTE FUNCTION catalog_order.reject_row_mutation();

CREATE TRIGGER trg_customer_order_guard
BEFORE UPDATE ON catalog_order.customer_order
FOR EACH ROW EXECUTE FUNCTION catalog_order.guard_customer_order_snapshot();

CREATE TRIGGER trg_payment_order_guard
BEFORE UPDATE ON catalog_order.payment_order
FOR EACH ROW EXECUTE FUNCTION catalog_order.guard_payment_order_identity();

CREATE TRIGGER trg_payment_event_immutable
BEFORE UPDATE OR DELETE ON catalog_order.payment_event
FOR EACH ROW EXECUTE FUNCTION catalog_order.reject_row_mutation();

REVOKE ALL ON TABLE catalog_order.catalog_product FROM PUBLIC;
REVOKE ALL ON TABLE catalog_order.catalog_version FROM PUBLIC;
REVOKE ALL ON TABLE catalog_order.catalog_offer FROM PUBLIC;
REVOKE ALL ON TABLE catalog_order.catalog_benefit_line FROM PUBLIC;
REVOKE ALL ON TABLE catalog_order.customer_order FROM PUBLIC;
REVOKE ALL ON TABLE catalog_order.payment_order FROM PUBLIC;
REVOKE ALL ON TABLE catalog_order.payment_event FROM PUBLIC;
REVOKE ALL ON FUNCTION catalog_order.reject_row_mutation() FROM PUBLIC;
REVOKE ALL ON FUNCTION catalog_order.guard_catalog_version_published() FROM PUBLIC;
REVOKE ALL ON FUNCTION catalog_order.guard_catalog_offer() FROM PUBLIC;
REVOKE ALL ON FUNCTION catalog_order.guard_customer_order_snapshot() FROM PUBLIC;
REVOKE ALL ON FUNCTION catalog_order.guard_payment_order_identity() FROM PUBLIC;

GRANT USAGE ON SCHEMA catalog_order
    TO catalog_order_owner, catalog_order_role, entitlement_metering_role;

GRANT SELECT, INSERT ON TABLE catalog_order.catalog_product TO catalog_order_role;
GRANT SELECT, INSERT ON TABLE catalog_order.catalog_version TO catalog_order_role;
GRANT UPDATE (sale_status, offer_type, audience, price_cents, currency, billing_cycle,
        installments, grant_interval_months, snapshot, snapshot_sha256, published_at, revision)
    ON TABLE catalog_order.catalog_version TO catalog_order_role;
GRANT SELECT, INSERT ON TABLE catalog_order.catalog_offer TO catalog_order_role;
GRANT UPDATE (offer_status, version_token, revision)
    ON TABLE catalog_order.catalog_offer TO catalog_order_role;
GRANT SELECT, INSERT ON TABLE catalog_order.catalog_benefit_line TO catalog_order_role;
GRANT SELECT, INSERT ON TABLE catalog_order.customer_order TO catalog_order_role;
GRANT UPDATE (payment_status, paid_at, closed_at, revision)
    ON TABLE catalog_order.customer_order TO catalog_order_role;
GRANT SELECT, INSERT ON TABLE catalog_order.payment_order TO catalog_order_role;
GRANT UPDATE (status, provider_transaction_id, updated_at, revision)
    ON TABLE catalog_order.payment_order TO catalog_order_role;
GRANT SELECT, INSERT ON TABLE catalog_order.payment_event TO catalog_order_role;

GRANT EXECUTE ON FUNCTION catalog_order.guard_catalog_version_published() TO catalog_order_role;
GRANT EXECUTE ON FUNCTION catalog_order.guard_catalog_offer() TO catalog_order_role;
GRANT EXECUTE ON FUNCTION catalog_order.guard_customer_order_snapshot() TO catalog_order_role;
GRANT EXECUTE ON FUNCTION catalog_order.guard_payment_order_identity() TO catalog_order_role;

GRANT SELECT (id, account_id, owner_organization_id, subject_id, catalog_version_id,
        snapshot, snapshot_sha256, payment_status, currency, created_at)
    ON TABLE catalog_order.customer_order TO entitlement_metering_role;
GRANT SELECT (id, order_id, status, amount_cents, currency, updated_at)
    ON TABLE catalog_order.payment_order TO entitlement_metering_role;
GRANT SELECT (id, payment_order_id, provider, provider_event_id, event_type,
        signature_verified, amount_cents, occurred_at)
    ON TABLE catalog_order.payment_event TO entitlement_metering_role;

RESET ROLE;

SET LOCAL ROLE customer_iam_owner;

GRANT USAGE ON SCHEMA customer_iam TO catalog_order_role;
GRANT SELECT (id, status) ON TABLE customer_iam.customer_account TO catalog_order_role;
GRANT SELECT (account_id, real_name_status) ON TABLE customer_iam.identity_profile
    TO catalog_order_role;

RESET ROLE;

SET LOCAL ROLE organization_access_owner;

GRANT USAGE ON SCHEMA organization_access TO catalog_order_role;
GRANT SELECT (id, status) ON TABLE organization_access.organization TO catalog_order_role;
GRANT SELECT (organization_id, account_id, role, status)
    ON TABLE organization_access.organization_membership TO catalog_order_role;

RESET ROLE;

SET LOCAL ROLE subject_number_owner;

GRANT USAGE ON SCHEMA subject_number TO catalog_order_role;
GRANT SELECT (id, owner_account_id, owner_organization_id, revision)
    ON TABLE subject_number.business_subject TO catalog_order_role;

RESET ROLE;

SET LOCAL ROLE activation_consent_owner;

GRANT USAGE ON SCHEMA activation_consent
    TO activation_consent_owner, activation_consent_role, ivr_role, catalog_order_role;

RESET ROLE;

SET LOCAL ROLE entitlement_metering_owner;

GRANT USAGE ON SCHEMA entitlement_metering
    TO entitlement_metering_owner, entitlement_metering_role, activation_consent_role, ivr_role;

RESET ROLE;

ALTER TABLE catalog_order.catalog_version
    ADD CONSTRAINT fk_catalog_version_product
    FOREIGN KEY (product_id) REFERENCES catalog_order.catalog_product(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.catalog_version
    ADD CONSTRAINT fk_catalog_version_employee
    FOREIGN KEY (created_by_employee_id) REFERENCES operations_iam.employee_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.catalog_offer
    ADD CONSTRAINT fk_catalog_offer_version
    FOREIGN KEY (catalog_version_id) REFERENCES catalog_order.catalog_version(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.catalog_benefit_line
    ADD CONSTRAINT fk_catalog_benefit_version
    FOREIGN KEY (catalog_version_id) REFERENCES catalog_order.catalog_version(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.customer_order
    ADD CONSTRAINT fk_customer_order_account
    FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.customer_order
    ADD CONSTRAINT fk_customer_order_organization
    FOREIGN KEY (owner_organization_id) REFERENCES organization_access.organization(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.customer_order
    ADD CONSTRAINT fk_customer_order_subject
    FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.customer_order
    ADD CONSTRAINT fk_customer_order_version
    FOREIGN KEY (catalog_version_id) REFERENCES catalog_order.catalog_version(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.customer_order
    ADD CONSTRAINT fk_customer_order_offer
    FOREIGN KEY (catalog_offer_id) REFERENCES catalog_order.catalog_offer(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.payment_order
    ADD CONSTRAINT fk_payment_order_order
    FOREIGN KEY (order_id) REFERENCES catalog_order.customer_order(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE catalog_order.payment_event
    ADD CONSTRAINT fk_payment_event_order
    FOREIGN KEY (payment_order_id) REFERENCES catalog_order.payment_order(id)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
