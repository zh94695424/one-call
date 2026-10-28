package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * PA1/PA2/PA3 S1 PostgreSQL 16 proof for the frozen V001, V002 and V003 contracts: V001-then-
 * V002-then-V003 migration, exact V001+V002+V003 inventory (including the F-PA2-PR-01 34-check
 * and F-PA3-PR-01 25-check normalized pg_get_constraintdef maps) and failure-visibility;
 * idempotency/Outbox first-result and transaction atomicity; audit append-only, reason/denylist,
 * fixed-search-path (V001 functions, the six V002 guards and the four subject_number functions)
 * and privilege boundaries; the PA2 S3 runtime/cross-module privilege matrix and last-enabled-admin
 * serialization protocol (transient SQL, no database function); the PA3 S1 exact V003
 * catalog/guard inventory and owner/PUBLIC/runtime ACL matrix; and the PA3 S2 V003 subject
 * owner/real-name, binding lifecycle/active-number, append-only claim, risk revision and
 * active-binding concurrency backstops. There is no skip path: an
 * unavailable Docker daemon, failed image pull, failed container start, JDBC failure, Flyway
 * failure or assertion failure fails the Maven build. The password is generated per JVM run and
 * never persisted.
 */
class PostgresContainerIT {

    private static final String DB_USER = "onecall";
    private static final String PASSWORD = UUID.randomUUID().toString();

    private static final String BROKEN_HISTORY_SCHEMA = "pa1_broken_history";
    private static final String BROKEN_HISTORY_TABLE = "flyway_schema_history_broken";
    private static final String BROKEN_MARKER_SCHEMA = "pa1_broken_marker";
    private static final String BROKEN_VERSION = "901";
    private static final String BROKEN_SENTINEL = "PA1_S1_BROKEN_MIGRATION_SENTINEL_2f4c9a7e";

    private static final List<String> S1_ROLES = List.of(
            "platform_reliability_owner",
            "platform_reliability_role",
            "platform_dispatcher_role",
            "audit_owner",
            "audit_reader_role",
            "customer_iam_owner",
            "customer_iam_role",
            "organization_access_owner",
            "organization_access_role",
            "operations_iam_owner",
            "operations_iam_role",
            "subject_number_owner",
            "subject_number_role",
            "subject_identity_verification_role",
            "operations_service_role");

    private static final List<String> DOMAIN_ROLES = List.of(
            "customer_iam_role",
            "organization_access_role",
            "operations_iam_role",
            "subject_number_role");

    private static final List<String> PLATFORM_USAGE_ROLES = List.of(
            "customer_iam_role",
            "organization_access_role",
            "operations_iam_role",
            "subject_number_role",
            "platform_reliability_role",
            "platform_dispatcher_role");

    private static final List<String> AUDIT_USAGE_ROLES = List.of(
            "customer_iam_role",
            "organization_access_role",
            "operations_iam_role",
            "subject_number_role",
            "subject_identity_verification_role",
            "operations_service_role",
            "audit_reader_role");

    private static final List<String> PLATFORM_FUNCTIONS = List.of(
            "platform.idempotency_begin(text,text,text)",
            "platform.idempotency_complete(uuid,integer,text,uuid,jsonb,text)",
            "platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)");

    private static final String AUDIT_FUNCTION =
            "audit.append_fact(timestamp with time zone,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)";

    private static final List<String> S1_TABLES = List.of(
            "platform.idempotency_record",
            "platform.outbox_event",
            "audit.audit_fact");

    private static final List<String> V002_TABLES = List.of(
            "customer_iam.customer_account",
            "customer_iam.auth_identity",
            "customer_iam.identity_profile",
            "organization_access.organization",
            "organization_access.organization_membership",
            "operations_iam.employee_account");

    private static final List<String> V003_TABLES = List.of(
            "subject_number.business_subject",
            "subject_number.caller_number_binding",
            "subject_number.subject_claim",
            "subject_number.subject_risk_state");

    private static final List<String> ALL_V001_V003_TABLES = List.of(
            "audit.audit_fact",
            "customer_iam.auth_identity",
            "customer_iam.customer_account",
            "customer_iam.identity_profile",
            "operations_iam.employee_account",
            "organization_access.organization",
            "organization_access.organization_membership",
            "platform.idempotency_record",
            "platform.outbox_event",
            "subject_number.business_subject",
            "subject_number.caller_number_binding",
            "subject_number.subject_claim",
            "subject_number.subject_risk_state");

    private static final List<String> V004_ROLES = List.of(
            "catalog_order_owner",
            "catalog_order_role",
            "ivr_owner",
            "ivr_role",
            "entitlement_metering_owner",
            "entitlement_metering_role",
            "activation_consent_owner",
            "activation_consent_role");

    private static final List<String> V004_TABLES = List.of(
            "catalog_order.catalog_benefit_line",
            "catalog_order.catalog_offer",
            "catalog_order.catalog_product",
            "catalog_order.catalog_version",
            "catalog_order.customer_order",
            "catalog_order.payment_event",
            "catalog_order.payment_order");

    private static final List<String> V005_TABLES = List.of(
            "ivr.call_event",
            "ivr.call_leg",
            "ivr.call_record",
            "ivr.inbound_session",
            "ivr.ivr_decision",
            "ivr.outbound_call_task");

    /**
     * F-PA4-PR-01: frozen mapping of bare table name -&gt; (check name -&gt; expected PostgreSQL 16
     * canonical {@code pg_get_constraintdef} output) for all 51 V004 named checks. Each literal
     * derives from the accepted plan section 8.2 PG16.15 deparse and is compared by
     * whole-definition equality after ASCII-whitespace-only normalization. Substring/contains/
     * prefix/regex/semantic equivalence is forbidden.
     */
    private static final Map<String, Map<String, String>> V004_CHECK_DEF_MAP;

    static {
        Map<String, Map<String, String>> map = new java.util.LinkedHashMap<>();
        Map<String, String> catalogProduct = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_catalog_product_code",
                        "CHECK ((product_code ~ '^[A-Z][A-Z0-9_-]{1,63}$'::text))"),
                java.util.Map.entry("ck_catalog_product_family",
                        "CHECK ((product_family ~ '^[A-Z][A-Z0-9_]{0,63}$'::text))")));
        map.put("catalog_product", java.util.Collections.unmodifiableMap(catalogProduct));
        Map<String, String> catalogVersion = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_catalog_version_audience",
                        "CHECK ((audience = ANY (ARRAY['PERSONAL'::text, 'ENTERPRISE'::text])))"),
                java.util.Map.entry("ck_catalog_version_audience_shape",
                        "CHECK (((audience = 'ENTERPRISE'::text) = (offer_type = 'QUOTE'::text)))"),
                java.util.Map.entry("ck_catalog_version_billing_matrix",
                        "CHECK ((((offer_type = 'SUBSCRIPTION'::text) AND (billing_cycle = ANY (ARRAY['MONTHLY'::text, 'QUARTERLY'::text, 'ANNUAL'::text])) AND (installments >= 1) AND (grant_interval_months >= 1)) OR ((offer_type = ANY (ARRAY['ONE_TIME'::text, 'ADD_ON'::text])) AND (billing_cycle = 'ONCE'::text) AND (installments = 1) AND (grant_interval_months = 0)) OR ((offer_type = 'QUOTE'::text) AND (billing_cycle = 'QUOTE'::text) AND (installments = 1) AND (grant_interval_months = 0) AND (price_cents IS NULL))))"),
                java.util.Map.entry("ck_catalog_version_currency",
                        "CHECK ((currency ~ '^[A-Z]{3}$'::text))"),
                java.util.Map.entry("ck_catalog_version_no",
                        "CHECK ((version_no >= 1))"),
                java.util.Map.entry("ck_catalog_version_offer_type",
                        "CHECK ((offer_type = ANY (ARRAY['SUBSCRIPTION'::text, 'ONE_TIME'::text, 'ADD_ON'::text, 'QUOTE'::text])))"),
                java.util.Map.entry("ck_catalog_version_price",
                        "CHECK ((((price_cents IS NULL) = (offer_type = 'QUOTE'::text)) AND ((price_cents IS NULL) OR (price_cents >= 0))))"),
                java.util.Map.entry("ck_catalog_version_published_shape",
                        "CHECK (((sale_status = 'DRAFT'::text) = (published_at IS NULL)))"),
                java.util.Map.entry("ck_catalog_version_revision",
                        "CHECK ((revision >= 1))"),
                java.util.Map.entry("ck_catalog_version_sale_status",
                        "CHECK ((sale_status = ANY (ARRAY['DRAFT'::text, 'ACTIVE'::text, 'OFFSALE'::text])))"),
                java.util.Map.entry("ck_catalog_version_snapshot_sha",
                        "CHECK ((snapshot_sha256 ~ '^[0-9a-f]{64}$'::text))"),
                java.util.Map.entry("ck_catalog_version_snapshot_type",
                        "CHECK ((jsonb_typeof(snapshot) = 'object'::text))")));
        map.put("catalog_version", java.util.Collections.unmodifiableMap(catalogVersion));
        Map<String, String> catalogOffer = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_catalog_offer_channel",
                        "CHECK ((channel = ANY (ARRAY['MINIAPP'::text, 'WEB'::text])))"),
                java.util.Map.entry("ck_catalog_offer_revision",
                        "CHECK ((revision >= 1))"),
                java.util.Map.entry("ck_catalog_offer_status",
                        "CHECK ((offer_status = ANY (ARRAY['ACTIVE'::text, 'WITHDRAWN'::text])))"),
                java.util.Map.entry("ck_catalog_offer_token",
                        "CHECK ((((length(version_token) >= 1) AND (length(version_token) <= 128)) AND (btrim(version_token) <> ''::text)))")));
        map.put("catalog_offer", java.util.Collections.unmodifiableMap(catalogOffer));
        Map<String, String> catalogBenefit = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_catalog_benefit_interval",
                        "CHECK ((grant_interval_months >= 0))"),
                java.util.Map.entry("ck_catalog_benefit_quantity",
                        "CHECK ((quantity >= 1))"),
                java.util.Map.entry("ck_catalog_benefit_type",
                        "CHECK ((entitlement_type = ANY (ARRAY['RECORDING_SECONDS'::text, 'AI_SECONDS'::text, 'EVIDENCE_COUNT'::text, 'STORAGE_BYTES'::text])))"),
                java.util.Map.entry("ck_catalog_benefit_validity",
                        "CHECK ((validity_days >= 1))")));
        map.put("catalog_benefit_line", java.util.Collections.unmodifiableMap(catalogBenefit));
        Map<String, String> customerOrder = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_customer_order_amount",
                        "CHECK ((amount_cents >= 0))"),
                java.util.Map.entry("ck_customer_order_channel",
                        "CHECK ((channel = ANY (ARRAY['MINIAPP'::text, 'WEB'::text])))"),
                java.util.Map.entry("ck_customer_order_currency",
                        "CHECK ((currency ~ '^[A-Z]{3}$'::text))"),
                java.util.Map.entry("ck_customer_order_fingerprint",
                        "CHECK ((request_fingerprint ~ '^[0-9a-f]{64}$'::text))"),
                java.util.Map.entry("ck_customer_order_idempotency",
                        "CHECK ((((length(idempotency_key) >= 1) AND (length(idempotency_key) <= 475)) AND (btrim(idempotency_key) <> ''::text)))"),
                java.util.Map.entry("ck_customer_order_no",
                        "CHECK ((((length(order_no) >= 1) AND (length(order_no) <= 64)) AND (btrim(order_no) <> ''::text)))"),
                java.util.Map.entry("ck_customer_order_payment_shape",
                        "CHECK ((((payment_status = 'PAID'::text) = (paid_at IS NOT NULL)) AND ((payment_status = 'CLOSED'::text) = (closed_at IS NOT NULL))))"),
                java.util.Map.entry("ck_customer_order_payment_status",
                        "CHECK ((payment_status = ANY (ARRAY['UNPAID'::text, 'UNKNOWN'::text, 'PAID'::text, 'CLOSED'::text])))"),
                java.util.Map.entry("ck_customer_order_revision",
                        "CHECK ((revision >= 1))"),
                java.util.Map.entry("ck_customer_order_snapshot_sha",
                        "CHECK ((snapshot_sha256 ~ '^[0-9a-f]{64}$'::text))"),
                java.util.Map.entry("ck_customer_order_snapshot_type",
                        "CHECK ((jsonb_typeof(snapshot) = 'object'::text))"),
                java.util.Map.entry("ck_customer_order_time_order",
                        "CHECK ((((paid_at IS NULL) OR (paid_at >= created_at)) AND ((closed_at IS NULL) OR (closed_at >= created_at))))"),
                java.util.Map.entry("ck_customer_order_version_token",
                        "CHECK ((((length(version_token) >= 1) AND (length(version_token) <= 128)) AND (btrim(version_token) <> ''::text)))")));
        map.put("customer_order", java.util.Collections.unmodifiableMap(customerOrder));
        Map<String, String> paymentOrder = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_payment_order_amount",
                        "CHECK ((amount_cents >= 0))"),
                java.util.Map.entry("ck_payment_order_currency",
                        "CHECK ((currency ~ '^[A-Z]{3}$'::text))"),
                java.util.Map.entry("ck_payment_order_merchant_no",
                        "CHECK ((((length(merchant_order_no) >= 1) AND (length(merchant_order_no) <= 64)) AND (btrim(merchant_order_no) <> ''::text)))"),
                java.util.Map.entry("ck_payment_order_paid_shape",
                        "CHECK (((status <> 'PAID'::text) OR (provider_transaction_id IS NOT NULL)))"),
                java.util.Map.entry("ck_payment_order_provider",
                        "CHECK ((provider = 'WECHAT'::text))"),
                java.util.Map.entry("ck_payment_order_revision",
                        "CHECK ((revision >= 1))"),
                java.util.Map.entry("ck_payment_order_status",
                        "CHECK ((status = ANY (ARRAY['UNPAID'::text, 'UNKNOWN'::text, 'PAID'::text, 'CLOSED'::text])))"),
                java.util.Map.entry("ck_payment_order_time_order",
                        "CHECK ((updated_at >= created_at))"),
                java.util.Map.entry("ck_payment_order_transaction",
                        "CHECK (((provider_transaction_id IS NULL) OR (((length(provider_transaction_id) >= 1) AND (length(provider_transaction_id) <= 128)) AND (btrim(provider_transaction_id) <> ''::text))))")));
        map.put("payment_order", java.util.Collections.unmodifiableMap(paymentOrder));
        Map<String, String> paymentEvent = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_payment_event_amount",
                        "CHECK ((amount_cents >= 0))"),
                java.util.Map.entry("ck_payment_event_event_id",
                        "CHECK ((((length(provider_event_id) >= 1) AND (length(provider_event_id) <= 128)) AND (btrim(provider_event_id) <> ''::text)))"),
                java.util.Map.entry("ck_payment_event_payload_ciphertext",
                        "CHECK ((payload_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text))"),
                java.util.Map.entry("ck_payment_event_payload_sha",
                        "CHECK ((payload_sha256 ~ '^[0-9a-f]{64}$'::text))"),
                java.util.Map.entry("ck_payment_event_provider",
                        "CHECK ((provider = 'WECHAT'::text))"),
                java.util.Map.entry("ck_payment_event_transaction",
                        "CHECK (((transaction_id IS NULL) OR (((length(transaction_id) >= 1) AND (length(transaction_id) <= 128)) AND (btrim(transaction_id) <> ''::text))))"),
                java.util.Map.entry("ck_payment_event_type",
                        "CHECK ((((length(event_type) >= 1) AND (length(event_type) <= 64)) AND (btrim(event_type) <> ''::text)))")));
        map.put("payment_event", java.util.Collections.unmodifiableMap(paymentEvent));
        V004_CHECK_DEF_MAP = java.util.Collections.unmodifiableMap(map);
    }

    /**
     * Frozen PK/unique constraint inventory: table|name|contype|definition|columns. The two
     * partial uniques (uq_catalog_version_active, uq_payment_order_transaction) are indexes,
     * not constraints, and appear only in V004_INDEX_DEFS.
     */
    private static final List<String> V004_PK_UNIQUE_DEFS = List.of(
            "catalog_benefit_line|pk_catalog_benefit_line|p|PRIMARY KEY (id)|id",
            "catalog_benefit_line|uq_catalog_benefit_version_type|u|UNIQUE (catalog_version_id, entitlement_type)|catalog_version_id,entitlement_type",
            "catalog_offer|pk_catalog_offer|p|PRIMARY KEY (id)|id",
            "catalog_offer|uq_catalog_offer_version_channel|u|UNIQUE (catalog_version_id, channel)|catalog_version_id,channel",
            "catalog_product|pk_catalog_product|p|PRIMARY KEY (id)|id",
            "catalog_product|uq_catalog_product_code|u|UNIQUE (product_code)|product_code",
            "catalog_version|pk_catalog_version|p|PRIMARY KEY (id)|id",
            "catalog_version|uq_catalog_version_no|u|UNIQUE (product_id, version_no)|product_id,version_no",
            "customer_order|pk_customer_order|p|PRIMARY KEY (id)|id",
            "customer_order|uq_customer_order_idempotency|u|UNIQUE (account_id, idempotency_key)|account_id,idempotency_key",
            "customer_order|uq_customer_order_no|u|UNIQUE (order_no)|order_no",
            "payment_event|pk_payment_event|p|PRIMARY KEY (id)|id",
            "payment_event|uq_payment_event_provider_event|u|UNIQUE (provider, provider_event_id)|provider,provider_event_id",
            "payment_order|pk_payment_order|p|PRIMARY KEY (id)|id",
            "payment_order|uq_payment_order_merchant|u|UNIQUE (merchant_order_no)|merchant_order_no",
            "payment_order|uq_payment_order_order|u|UNIQUE (order_id)|order_id");

    /** Frozen FK inventory: name|definition|rrs|false|false|true. */
    private static final List<String> V004_FK_DEFS = List.of(
            "fk_catalog_benefit_version|FOREIGN KEY (catalog_version_id) REFERENCES catalog_order.catalog_version(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_catalog_offer_version|FOREIGN KEY (catalog_version_id) REFERENCES catalog_order.catalog_version(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_catalog_version_employee|FOREIGN KEY (created_by_employee_id) REFERENCES operations_iam.employee_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_catalog_version_product|FOREIGN KEY (product_id) REFERENCES catalog_order.catalog_product(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_customer_order_account|FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_customer_order_offer|FOREIGN KEY (catalog_offer_id) REFERENCES catalog_order.catalog_offer(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_customer_order_organization|FOREIGN KEY (owner_organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_customer_order_subject|FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_customer_order_version|FOREIGN KEY (catalog_version_id) REFERENCES catalog_order.catalog_version(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_payment_event_order|FOREIGN KEY (payment_order_id) REFERENCES catalog_order.payment_order(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_payment_order_order|FOREIGN KEY (order_id) REFERENCES catalog_order.customer_order(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true");

    /** Frozen pg_indexes.indexdef values (28). */
    private static final List<String> V004_INDEX_DEFS = List.of(
            "CREATE INDEX ix_catalog_benefit_version ON catalog_order.catalog_benefit_line USING btree (catalog_version_id)",
            "CREATE UNIQUE INDEX pk_catalog_benefit_line ON catalog_order.catalog_benefit_line USING btree (id)",
            "CREATE UNIQUE INDEX uq_catalog_benefit_version_type ON catalog_order.catalog_benefit_line USING btree (catalog_version_id, entitlement_type)",
            "CREATE INDEX ix_catalog_offer_channel_status ON catalog_order.catalog_offer USING btree (channel, offer_status)",
            "CREATE UNIQUE INDEX pk_catalog_offer ON catalog_order.catalog_offer USING btree (id)",
            "CREATE UNIQUE INDEX uq_catalog_offer_version_channel ON catalog_order.catalog_offer USING btree (catalog_version_id, channel)",
            "CREATE INDEX ix_catalog_product_family ON catalog_order.catalog_product USING btree (product_family, created_at)",
            "CREATE UNIQUE INDEX pk_catalog_product ON catalog_order.catalog_product USING btree (id)",
            "CREATE UNIQUE INDEX uq_catalog_product_code ON catalog_order.catalog_product USING btree (product_code)",
            "CREATE INDEX ix_catalog_version_product_status ON catalog_order.catalog_version USING btree (product_id, sale_status, version_no DESC)",
            "CREATE UNIQUE INDEX pk_catalog_version ON catalog_order.catalog_version USING btree (id)",
            "CREATE UNIQUE INDEX uq_catalog_version_active ON catalog_order.catalog_version USING btree (product_id) WHERE (sale_status = 'ACTIVE'::text)",
            "CREATE UNIQUE INDEX uq_catalog_version_no ON catalog_order.catalog_version USING btree (product_id, version_no)",
            "CREATE INDEX ix_customer_order_organization ON catalog_order.customer_order USING btree (owner_organization_id, created_at DESC)",
            "CREATE INDEX ix_customer_order_subject_status ON catalog_order.customer_order USING btree (subject_id, payment_status, created_at DESC)",
            "CREATE INDEX ix_customer_order_version ON catalog_order.customer_order USING btree (catalog_version_id)",
            "CREATE UNIQUE INDEX pk_customer_order ON catalog_order.customer_order USING btree (id)",
            "CREATE UNIQUE INDEX uq_customer_order_idempotency ON catalog_order.customer_order USING btree (account_id, idempotency_key)",
            "CREATE UNIQUE INDEX uq_customer_order_no ON catalog_order.customer_order USING btree (order_no)",
            "CREATE INDEX ix_payment_event_order_time ON catalog_order.payment_event USING btree (payment_order_id, received_at DESC)",
            "CREATE INDEX ix_payment_event_verified ON catalog_order.payment_event USING btree (payment_order_id, received_at) WHERE signature_verified",
            "CREATE UNIQUE INDEX pk_payment_event ON catalog_order.payment_event USING btree (id)",
            "CREATE UNIQUE INDEX uq_payment_event_provider_event ON catalog_order.payment_event USING btree (provider, provider_event_id)",
            "CREATE INDEX ix_payment_order_status_time ON catalog_order.payment_order USING btree (status, updated_at)",
            "CREATE UNIQUE INDEX pk_payment_order ON catalog_order.payment_order USING btree (id)",
            "CREATE UNIQUE INDEX uq_payment_order_merchant ON catalog_order.payment_order USING btree (merchant_order_no)",
            "CREATE UNIQUE INDEX uq_payment_order_order ON catalog_order.payment_order USING btree (order_id)",
            "CREATE UNIQUE INDEX uq_payment_order_transaction ON catalog_order.payment_order USING btree (provider_transaction_id) WHERE (provider_transaction_id IS NOT NULL)");

    /**
     * PA5 V005: frozen mapping of bare table name -&gt; (check name -&gt; expected PostgreSQL 16
     * canonical {@code pg_get_constraintdef} output) for all 73 V005 named checks. Each literal
     * derives from the accepted plan / docs/14 frozen dictionaries and is compared by
     * whole-definition equality after ASCII-whitespace-only normalization. Substring/contains/
     * prefix/regex/semantic equivalence is forbidden.
     */
    private static final Map<String, Map<String, String>> V005_CHECK_DEF_MAP;

    static {
        Map<String, Map<String, String>> map = new java.util.LinkedHashMap<>();
        Map<String, String> outbound = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_outbound_task_business_type",
                        "CHECK ((((length(business_type) >= 1) AND (length(business_type) <= 64)) AND (btrim(business_type) <> ''::text)))"),
                java.util.Map.entry("ck_outbound_task_callee_ciphertext",
                        "CHECK ((callee_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text))"),
                java.util.Map.entry("ck_outbound_task_callee_masked",
                        "CHECK ((((length(callee_masked) >= 1) AND (length(callee_masked) <= 64)) AND (btrim(callee_masked) <> ''::text)))"),
                java.util.Map.entry("ck_outbound_task_caller_hmac",
                        "CHECK ((caller_number_hmac ~ '^[0-9a-f]{64}$'::text))"),
                java.util.Map.entry("ck_outbound_task_claimed_shape",
                        "CHECK (((claimed_call_id IS NULL) = (claimed_at IS NULL)))"),
                java.util.Map.entry("ck_outbound_task_expiry",
                        "CHECK ((expires_at > created_at))"),
                java.util.Map.entry("ck_outbound_task_fingerprint",
                        "CHECK ((request_fingerprint ~ '^[0-9a-f]{64}$'::text))"),
                java.util.Map.entry("ck_outbound_task_idempotency_key",
                        "CHECK ((((length(idempotency_key) >= 1) AND (length(idempotency_key) <= 512)) AND (btrim(idempotency_key) <> ''::text)))"),
                java.util.Map.entry("ck_outbound_task_reason",
                        "CHECK (((reason_code IS NULL) OR (((length(reason_code) >= 1) AND (length(reason_code) <= 64)) AND (btrim(reason_code) <> ''::text))))"),
                java.util.Map.entry("ck_outbound_task_revision",
                        "CHECK ((revision >= 1))"),
                java.util.Map.entry("ck_outbound_task_single_owner",
                        "CHECK ((num_nonnulls(owner_account_id, owner_organization_id) = 1))"),
                java.util.Map.entry("ck_outbound_task_status",
                        "CHECK ((status = ANY (ARRAY['PENDING'::text, 'CLAIMED'::text, 'REPLACED'::text, 'CANCELLED'::text, 'EXPIRED'::text, 'REJECTED'::text, 'COMPLETED'::text, 'FAILED'::text])))"),
                java.util.Map.entry("ck_outbound_task_task_id",
                        "CHECK ((((length(task_id) >= 1) AND (length(task_id) <= 64)) AND (btrim(task_id) <> ''::text)))"),
                java.util.Map.entry("ck_outbound_task_terminal_shape",
                        "CHECK (((status = ANY (ARRAY['PENDING'::text, 'CLAIMED'::text])) = (terminal_at IS NULL)))"),
                java.util.Map.entry("ck_outbound_task_time_order",
                        "CHECK (((updated_at >= created_at) AND ((terminal_at IS NULL) OR (terminal_at >= created_at))))"),
                java.util.Map.entry("ck_outbound_task_ttl",
                        "CHECK ((ttl_seconds >= 1))")));
        map.put("outbound_call_task", java.util.Collections.unmodifiableMap(outbound));
        Map<String, String> inbound = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_inbound_session_access_masked",
                        "CHECK ((((length(access_number_masked) >= 1) AND (length(access_number_masked) <= 64)) AND (btrim(access_number_masked) <> ''::text)))"),
                java.util.Map.entry("ck_inbound_session_call_id",
                        "CHECK ((((length(call_id) >= 1) AND (length(call_id) <= 128)) AND (btrim(call_id) <> ''::text)))"),
                java.util.Map.entry("ck_inbound_session_caller_ciphertext",
                        "CHECK (((caller_ciphertext IS NULL) OR (caller_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text)))"),
                java.util.Map.entry("ck_inbound_session_caller_hmac",
                        "CHECK (((caller_number_hmac IS NULL) OR (caller_number_hmac ~ '^[0-9a-f]{64}$'::text)))"),
                java.util.Map.entry("ck_inbound_session_caller_masked",
                        "CHECK (((caller_masked IS NULL) OR (((length(caller_masked) >= 1) AND (length(caller_masked) <= 64)) AND (btrim(caller_masked) <> ''::text))))"),
                java.util.Map.entry("ck_inbound_session_caller_shape",
                        "CHECK (((trusted_caller = false) OR ((caller_ciphertext IS NOT NULL) AND (caller_number_hmac IS NOT NULL) AND (caller_masked IS NOT NULL))))"),
                java.util.Map.entry("ck_inbound_session_confirmed_shape",
                        "CHECK ((((status = 'CONFIRMED'::text) = (confirmed_at IS NOT NULL)) AND ((status <> 'CONFIRMED'::text) OR ((trusted_caller = true) AND (confirmed_subject_id IS NOT NULL)))))"),
                java.util.Map.entry("ck_inbound_session_guidance",
                        "CHECK (((guidance_status IS NULL) OR (guidance_status = ANY (ARRAY['ACTIVATION_REQUIRED'::text, 'CONSENT_REQUIRED'::text]))))"),
                java.util.Map.entry("ck_inbound_session_node",
                        "CHECK ((((length(node_id) >= 1) AND (length(node_id) <= 64)) AND (btrim(node_id) <> ''::text)))"),
                java.util.Map.entry("ck_inbound_session_prompt",
                        "CHECK ((((length(prompt_version) >= 1) AND (length(prompt_version) <= 64)) AND (btrim(prompt_version) <> ''::text)))"),
                java.util.Map.entry("ck_inbound_session_protocol",
                        "CHECK ((((length(protocol_version) >= 1) AND (length(protocol_version) <= 32)) AND (btrim(protocol_version) <> ''::text)))"),
                java.util.Map.entry("ck_inbound_session_revision",
                        "CHECK ((revision >= 1))"),
                java.util.Map.entry("ck_inbound_session_status",
                        "CHECK ((status = ANY (ARRAY['RECEIVED'::text, 'CONFIRMED'::text, 'ABANDONED'::text, 'REJECTED'::text])))"),
                java.util.Map.entry("ck_inbound_session_time_order",
                        "CHECK (((confirmed_at IS NULL) OR (confirmed_at >= created_at)))")));
        map.put("inbound_session", java.util.Collections.unmodifiableMap(inbound));
        Map<String, String> callRecord = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_call_record_access_masked",
                        "CHECK ((((length(access_number_masked) >= 1) AND (length(access_number_masked) <= 64)) AND (btrim(access_number_masked) <> ''::text)))"),
                java.util.Map.entry("ck_call_record_authoritative_seconds",
                        "CHECK (((authoritative_seconds IS NULL) OR ((authoritative_seconds >= 0) AND (status = ANY (ARRAY['ENDED'::text, 'FAILED'::text])))))"),
                java.util.Map.entry("ck_call_record_callee_ciphertext",
                        "CHECK (((callee_ciphertext IS NULL) OR (callee_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text)))"),
                java.util.Map.entry("ck_call_record_callee_masked",
                        "CHECK (((callee_masked IS NULL) OR (((length(callee_masked) >= 1) AND (length(callee_masked) <= 64)) AND (btrim(callee_masked) <> ''::text))))"),
                java.util.Map.entry("ck_call_record_call_id",
                        "CHECK ((((length(call_id) >= 1) AND (length(call_id) <= 128)) AND (btrim(call_id) <> ''::text)))"),
                java.util.Map.entry("ck_call_record_caller_ciphertext",
                        "CHECK ((caller_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text))"),
                java.util.Map.entry("ck_call_record_caller_hmac",
                        "CHECK ((caller_number_hmac ~ '^[0-9a-f]{64}$'::text))"),
                java.util.Map.entry("ck_call_record_caller_masked",
                        "CHECK ((((length(caller_masked) >= 1) AND (length(caller_masked) <= 64)) AND (btrim(caller_masked) <> ''::text)))"),
                java.util.Map.entry("ck_call_record_end_reason",
                        "CHECK (((end_reason IS NULL) OR (((length(end_reason) >= 1) AND (length(end_reason) <= 64)) AND (btrim(end_reason) <> ''::text))))"),
                java.util.Map.entry("ck_call_record_node",
                        "CHECK ((((length(node_id) >= 1) AND (length(node_id) <= 64)) AND (btrim(node_id) <> ''::text)))"),
                java.util.Map.entry("ck_call_record_protocol",
                        "CHECK ((((length(protocol_version) >= 1) AND (length(protocol_version) <= 32)) AND (btrim(protocol_version) <> ''::text)))"),
                java.util.Map.entry("ck_call_record_revision",
                        "CHECK ((revision >= 1))"),
                java.util.Map.entry("ck_call_record_status",
                        "CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'ENDED'::text, 'FAILED'::text, 'UNKNOWN'::text, 'REJECTED'::text])))"),
                java.util.Map.entry("ck_call_record_terminal_shape",
                        "CHECK (((status = ANY (ARRAY['ACTIVE'::text, 'UNKNOWN'::text])) = (ended_at IS NULL)))"),
                java.util.Map.entry("ck_call_record_time_order",
                        "CHECK ((((started_at IS NULL) OR (ended_at IS NULL) OR (ended_at >= started_at)) AND ((started_at IS NULL) OR (bridged_at IS NULL) OR (bridged_at >= started_at)) AND ((started_at IS NULL) OR (recording_started_at IS NULL) OR (recording_started_at >= started_at)) AND ((recording_started_at IS NULL) OR (recording_ended_at IS NULL) OR (recording_ended_at >= recording_started_at))))")));
        map.put("call_record", java.util.Collections.unmodifiableMap(callRecord));
        Map<String, String> callLeg = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_call_leg_direction",
                        "CHECK ((((leg_type = 'CALLER'::text) AND (direction = 'INBOUND'::text)) OR ((leg_type = 'CALLEE'::text) AND (direction = 'OUTBOUND'::text))))"),
                java.util.Map.entry("ck_call_leg_end_reason",
                        "CHECK (((end_reason IS NULL) OR (((length(end_reason) >= 1) AND (length(end_reason) <= 64)) AND (btrim(end_reason) <> ''::text))))"),
                java.util.Map.entry("ck_call_leg_leg_id",
                        "CHECK ((((length(leg_id) >= 1) AND (length(leg_id) <= 64)) AND (btrim(leg_id) <> ''::text)))"),
                java.util.Map.entry("ck_call_leg_revision",
                        "CHECK ((revision >= 1))"),
                java.util.Map.entry("ck_call_leg_state",
                        "CHECK ((state = ANY (ARRAY['RINGING'::text, 'ANSWERED'::text, 'ENDED'::text, 'FAILED'::text])))"),
                java.util.Map.entry("ck_call_leg_time_order",
                        "CHECK ((((answered_at IS NULL) OR (answered_at >= started_at)) AND ((ended_at IS NULL) OR (ended_at >= started_at)) AND ((answered_at IS NULL) OR (ended_at IS NULL) OR (ended_at >= answered_at))))"),
                java.util.Map.entry("ck_call_leg_type",
                        "CHECK ((leg_type = ANY (ARRAY['CALLER'::text, 'CALLEE'::text])))")));
        map.put("call_leg", java.util.Collections.unmodifiableMap(callLeg));
        Map<String, String> callEvent = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_call_event_domain",
                        "CHECK ((event_domain = 'IVR_EVENT'::text))"),
                java.util.Map.entry("ck_call_event_event_id",
                        "CHECK ((((length(event_id) >= 1) AND (length(event_id) <= 128)) AND (btrim(event_id) <> ''::text)))"),
                java.util.Map.entry("ck_call_event_payload_ciphertext",
                        "CHECK ((payload_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text))"),
                java.util.Map.entry("ck_call_event_payload_redacted",
                        "CHECK (((jsonb_typeof(payload_redacted) = 'object'::text) AND (payload_redacted ? 'source'::text) AND (payload_redacted ? 'receipt'::text) AND (jsonb_typeof((payload_redacted -> 'source'::text)) = 'object'::text) AND (jsonb_typeof((payload_redacted -> 'receipt'::text)) = 'object'::text) AND (((payload_redacted - 'source'::text) - 'receipt'::text) = '{}'::jsonb) AND ((((((payload_redacted -> 'source'::text) - 'eventType'::text) - 'legId'::text) - 'sequenceNo'::text) - 'payload'::text) = '{}'::jsonb) AND ((((payload_redacted -> 'receipt'::text) - 'payloadSha256'::text) - 'eventFingerprint'::text) = '{}'::jsonb) AND ((payload_redacted -> 'source'::text) ? 'payload'::text) AND (jsonb_typeof(((payload_redacted -> 'source'::text) -> 'payload'::text)) = 'object'::text) AND ((payload_redacted -> 'receipt'::text) ? 'payloadSha256'::text) AND (jsonb_typeof(((payload_redacted -> 'receipt'::text) -> 'payloadSha256'::text)) = 'string'::text) AND ((payload_redacted -> 'receipt'::text) ? 'eventFingerprint'::text) AND (((payload_redacted -> 'receipt'::text) ->> 'payloadSha256'::text) = payload_sha256)))"),
                java.util.Map.entry("ck_call_event_payload_sha",
                        "CHECK ((payload_sha256 ~ '^[0-9a-f]{64}$'::text))"),
                java.util.Map.entry("ck_call_event_processing_status",
                        "CHECK ((processing_status = ANY (ARRAY['RECEIVED'::text, 'APPLIED'::text, 'IGNORED'::text, 'QUARANTINED'::text])))"),
                java.util.Map.entry("ck_call_event_reason",
                        "CHECK (((reason_code IS NULL) OR (((length(reason_code) >= 1) AND (length(reason_code) <= 64)) AND (btrim(reason_code) <> ''::text))))"),
                java.util.Map.entry("ck_call_event_revision",
                        "CHECK ((revision >= 1))"),
                java.util.Map.entry("ck_call_event_sequence",
                        "CHECK (((sequence_no IS NULL) OR (sequence_no >= 1)))"),
                java.util.Map.entry("ck_call_event_type",
                        "CHECK ((((length(event_type) >= 1) AND (length(event_type) <= 64)) AND (btrim(event_type) <> ''::text)))")));
        map.put("call_event", java.util.Collections.unmodifiableMap(callEvent));
        Map<String, String> decision = new java.util.LinkedHashMap<>(Map.ofEntries(
                java.util.Map.entry("ck_ivr_decision_action",
                        "CHECK (((action IS NULL) OR (action = ANY (ARRAY['GUIDE_ACTIVATION'::text, 'DIAL_TASK_CALLEE'::text, 'ENTER_CALLEE'::text, 'GUIDE_PURCHASE'::text, 'REJECT'::text, 'REJECT_SUSPENDED'::text, 'REJECT_CALL_IN_PROGRESS'::text]))))"),
                java.util.Map.entry("ck_ivr_decision_direct_reason",
                        "CHECK (((action IS NULL) = (reason_code = 'DIRECT_AUTHORIZED'::text)))"),
                java.util.Map.entry("ck_ivr_decision_fingerprint",
                        "CHECK ((request_fingerprint ~ '^[0-9a-f]{64}$'::text))"),
                java.util.Map.entry("ck_ivr_decision_key_version",
                        "CHECK ((key_version >= 1))"),
                java.util.Map.entry("ck_ivr_decision_payload",
                        "CHECK ((response_payload_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text))"),
                java.util.Map.entry("ck_ivr_decision_purpose",
                        "CHECK ((purpose = ANY (ARRAY['INBOUND_RESOLVE'::text, 'ACTIVATION_NEXT_ACTION'::text, 'DIRECT_AUTHORIZATION'::text])))"),
                java.util.Map.entry("ck_ivr_decision_reason",
                        "CHECK ((reason_code = ANY (ARRAY['ACTIVATION_REQUIRED'::text, 'CONSENT_REQUIRED'::text, 'CALLER_UNTRUSTED'::text, 'SESSION_IDENTITY_MISMATCH'::text, 'CALL_IN_PROGRESS'::text, 'CONFIRMATION_NOT_ALLOWED'::text, 'ACTIVATION_SUSPENDED'::text, 'CALLER_BINDING_REVOKED'::text, 'WORKSPACE_ACCESS_REVOKED'::text, 'TASK_ACCESS_REVOKED'::text, 'ORGANIZATION_INACTIVE'::text, 'ACCOUNT_DISABLED'::text, 'CALL_STATE_UNRESOLVED'::text, 'TASK_CONFLICT'::text, 'TASK_CLAIMED'::text, 'NO_AVAILABLE_ENTITLEMENT'::text, 'NO_VALID_TASK'::text, 'TASK_EXPIRED'::text, 'TASK_UNAVAILABLE'::text, 'DIRECT_AUTHORIZED'::text, 'DATA_INTEGRITY'::text])))"),
                java.util.Map.entry("ck_ivr_decision_sha",
                        "CHECK ((response_sha256 ~ '^[0-9a-f]{64}$'::text))"),
                java.util.Map.entry("ck_ivr_decision_shape",
                        "CHECK ((((purpose = ANY (ARRAY['INBOUND_RESOLVE'::text, 'ACTIVATION_NEXT_ACTION'::text])) AND (action IS NOT NULL) AND (((action = 'DIAL_TASK_CALLEE'::text) AND (reservation_id IS NOT NULL)) OR ((action <> 'DIAL_TASK_CALLEE'::text) AND (reservation_id IS NULL)))) OR ((purpose = 'DIRECT_AUTHORIZATION'::text) AND (((action IS NULL) AND (reservation_id IS NOT NULL)) OR ((action IS NOT NULL) AND (action = ANY (ARRAY['GUIDE_PURCHASE'::text, 'REJECT'::text, 'REJECT_SUSPENDED'::text, 'REJECT_CALL_IN_PROGRESS'::text])) AND (reservation_id IS NULL))))))"),
                java.util.Map.entry("ck_ivr_decision_snapshot",
                        "CHECK ((jsonb_typeof(response_snapshot) = 'object'::text))"),
                java.util.Map.entry("ck_ivr_decision_source",
                        "CHECK ((((purpose = 'INBOUND_RESOLVE'::text) AND (source_event_domain IS NULL) AND (source_event_id IS NULL)) OR ((purpose = 'ACTIVATION_NEXT_ACTION'::text) AND (source_event_domain IS NOT NULL) AND (source_event_id IS NOT NULL) AND (source_event_domain = 'IVR_CONFIRM'::text) AND ((length(source_event_id) >= 1) AND (length(source_event_id) <= 128)) AND (btrim(source_event_id) <> ''::text)) OR ((purpose = 'DIRECT_AUTHORIZATION'::text) AND (source_event_domain IS NOT NULL) AND (source_event_id IS NOT NULL) AND (source_event_domain = 'IVR_DIRECT_AUTH'::text) AND ((length(source_event_id) >= 1) AND (length(source_event_id) <= 128)) AND (btrim(source_event_id) <> ''::text))))")));
        map.put("ivr_decision", java.util.Collections.unmodifiableMap(decision));
        V005_CHECK_DEF_MAP = java.util.Collections.unmodifiableMap(map);
    }

    /** Frozen PK/unique constraint inventory for V005: table|name|contype|definition|columns. */
    private static final List<String> V005_PK_UNIQUE_DEFS = List.of(
            "call_event|pk_call_event|p|PRIMARY KEY (id)|id",
            "call_event|uq_call_event_domain_id|u|UNIQUE (event_domain, event_id)|event_domain,event_id",
            "call_leg|pk_call_leg|p|PRIMARY KEY (id)|id",
            "call_leg|uq_call_leg_id|u|UNIQUE (call_id, leg_id)|call_id,leg_id",
            "call_record|pk_call_record|p|PRIMARY KEY (id)|id",
            "call_record|uq_call_record_call|u|UNIQUE (call_id)|call_id",
            "call_record|uq_call_record_session|u|UNIQUE (session_id)|session_id",
            "inbound_session|pk_inbound_session|p|PRIMARY KEY (id)|id",
            "inbound_session|uq_inbound_session_call|u|UNIQUE (call_id)|call_id",
            "ivr_decision|pk_ivr_decision|p|PRIMARY KEY (id)|id",
            "ivr_decision|uq_ivr_decision_call_purpose|u|UNIQUE (call_id, purpose)|call_id,purpose",
            "outbound_call_task|pk_outbound_call_task|p|PRIMARY KEY (id)|id",
            "outbound_call_task|uq_outbound_call_task_task_id|u|UNIQUE (task_id)|task_id",
            "outbound_call_task|uq_outbound_task_idempotency|u|UNIQUE (created_by_account_id, idempotency_key)|created_by_account_id,idempotency_key");

    /** Frozen FK inventory for V005: name|definition|rrs|false|false|true. */
    private static final List<String> V005_FK_DEFS = List.of(
            "fk_call_event_call|FOREIGN KEY (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_call_leg_call|FOREIGN KEY (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_call_record_session|FOREIGN KEY (session_id) REFERENCES ivr.inbound_session(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_call_record_subject|FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_call_record_task|FOREIGN KEY (task_id) REFERENCES ivr.outbound_call_task(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_inbound_session_confirmed_subject|FOREIGN KEY (confirmed_subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_inbound_session_subject|FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_ivr_decision_call|FOREIGN KEY (call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_ivr_decision_session|FOREIGN KEY (session_id) REFERENCES ivr.inbound_session(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_outbound_task_binding|FOREIGN KEY (caller_binding_id) REFERENCES subject_number.caller_number_binding(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_outbound_task_claimed_call|FOREIGN KEY (claimed_call_id) REFERENCES ivr.call_record(call_id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_outbound_task_creator|FOREIGN KEY (created_by_account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_outbound_task_owner_account|FOREIGN KEY (owner_account_id) REFERENCES customer_iam.customer_account(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_outbound_task_owner_org|FOREIGN KEY (owner_organization_id) REFERENCES organization_access.organization(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true",
            "fk_outbound_task_subject|FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id) ON UPDATE RESTRICT ON DELETE RESTRICT|rrs|false|false|true");

    /** Frozen pg_indexes.indexdef values for V005 (31). */
    private static final List<String> V005_INDEX_DEFS = List.of(
            "CREATE UNIQUE INDEX pk_call_event ON ivr.call_event USING btree (id)",
            "CREATE INDEX ix_call_event_call_time ON ivr.call_event USING btree (call_id, occurred_at)",
            "CREATE INDEX ix_call_event_processing ON ivr.call_event USING btree (processing_status, received_at)",
            "CREATE UNIQUE INDEX uq_call_event_domain_id ON ivr.call_event USING btree (event_domain, event_id)",
            "CREATE UNIQUE INDEX uq_call_event_sequence ON ivr.call_event USING btree (call_id, sequence_no) WHERE (sequence_no IS NOT NULL)",
            "CREATE UNIQUE INDEX pk_call_leg ON ivr.call_leg USING btree (id)",
            "CREATE INDEX ix_call_leg_call_type ON ivr.call_leg USING btree (call_id, leg_type)",
            "CREATE UNIQUE INDEX uq_call_leg_id ON ivr.call_leg USING btree (call_id, leg_id)",
            "CREATE UNIQUE INDEX pk_call_record ON ivr.call_record USING btree (id)",
            "CREATE UNIQUE INDEX uq_call_record_active_caller ON ivr.call_record USING btree (caller_number_hmac) WHERE (status = ANY (ARRAY['ACTIVE'::text, 'UNKNOWN'::text]))",
            "CREATE UNIQUE INDEX uq_call_record_call ON ivr.call_record USING btree (call_id)",
            "CREATE UNIQUE INDEX uq_call_record_session ON ivr.call_record USING btree (session_id)",
            "CREATE UNIQUE INDEX uq_call_record_task ON ivr.call_record USING btree (task_id) WHERE (task_id IS NOT NULL)",
            "CREATE INDEX ix_call_record_subject_time ON ivr.call_record USING btree (subject_id, ended_at, created_at)",
            "CREATE INDEX ix_call_record_task ON ivr.call_record USING btree (task_id)",
            "CREATE UNIQUE INDEX pk_inbound_session ON ivr.inbound_session USING btree (id)",
            "CREATE UNIQUE INDEX uq_inbound_session_call ON ivr.inbound_session USING btree (call_id)",
            "CREATE INDEX ix_inbound_session_hmac_status ON ivr.inbound_session USING btree (caller_number_hmac, status, created_at DESC)",
            "CREATE INDEX ix_inbound_session_subject_time ON ivr.inbound_session USING btree (subject_id, created_at DESC)",
            "CREATE UNIQUE INDEX pk_ivr_decision ON ivr.ivr_decision USING btree (id)",
            "CREATE UNIQUE INDEX uq_ivr_decision_call_purpose ON ivr.ivr_decision USING btree (call_id, purpose)",
            "CREATE UNIQUE INDEX uq_ivr_decision_source ON ivr.ivr_decision USING btree (call_id, source_event_domain, source_event_id) WHERE (source_event_id IS NOT NULL)",
            "CREATE INDEX ix_ivr_decision_call_time ON ivr.ivr_decision USING btree (call_id, decided_at)",
            "CREATE UNIQUE INDEX pk_outbound_call_task ON ivr.outbound_call_task USING btree (id)",
            "CREATE UNIQUE INDEX uq_outbound_call_task_task_id ON ivr.outbound_call_task USING btree (task_id)",
            "CREATE UNIQUE INDEX uq_outbound_task_idempotency ON ivr.outbound_call_task USING btree (created_by_account_id, idempotency_key)",
            "CREATE UNIQUE INDEX uq_outbound_task_claimed_call ON ivr.outbound_call_task USING btree (claimed_call_id) WHERE (claimed_call_id IS NOT NULL)",
            "CREATE UNIQUE INDEX uq_outbound_task_live_caller ON ivr.outbound_call_task USING btree (caller_number_hmac) WHERE (status = ANY (ARRAY['PENDING'::text, 'CLAIMED'::text]))",
            "CREATE INDEX ix_outbound_task_creator_status ON ivr.outbound_call_task USING btree (created_by_account_id, status)",
            "CREATE INDEX ix_outbound_task_org_status ON ivr.outbound_call_task USING btree (owner_organization_id, status, created_at DESC)",
            "CREATE INDEX ix_outbound_task_subject_status ON ivr.outbound_call_task USING btree (subject_id, status, created_at DESC)");

    /**
     * PA4 V004 S2 coverage contract: every table|check_name pair that must receive a
     * {@code 23514}+exact-constraint-name probe. The runtime helpers assert that the probed
     * name set equals {@link #V004_CHECK_DEF_MAP}'s flattened name set (exactly 51 entries).
     */
    private static final List<String> V004_CHECK_PROBES = List.of(
            "catalog_product|ck_catalog_product_code",
            "catalog_product|ck_catalog_product_family",
            "catalog_version|ck_catalog_version_audience",
            "catalog_version|ck_catalog_version_audience_shape",
            "catalog_version|ck_catalog_version_billing_matrix",
            "catalog_version|ck_catalog_version_currency",
            "catalog_version|ck_catalog_version_no",
            "catalog_version|ck_catalog_version_offer_type",
            "catalog_version|ck_catalog_version_price",
            "catalog_version|ck_catalog_version_published_shape",
            "catalog_version|ck_catalog_version_revision",
            "catalog_version|ck_catalog_version_sale_status",
            "catalog_version|ck_catalog_version_snapshot_sha",
            "catalog_version|ck_catalog_version_snapshot_type",
            "catalog_offer|ck_catalog_offer_channel",
            "catalog_offer|ck_catalog_offer_revision",
            "catalog_offer|ck_catalog_offer_status",
            "catalog_offer|ck_catalog_offer_token",
            "catalog_benefit_line|ck_catalog_benefit_interval",
            "catalog_benefit_line|ck_catalog_benefit_quantity",
            "catalog_benefit_line|ck_catalog_benefit_type",
            "catalog_benefit_line|ck_catalog_benefit_validity",
            "customer_order|ck_customer_order_amount",
            "customer_order|ck_customer_order_channel",
            "customer_order|ck_customer_order_currency",
            "customer_order|ck_customer_order_fingerprint",
            "customer_order|ck_customer_order_idempotency",
            "customer_order|ck_customer_order_no",
            "customer_order|ck_customer_order_payment_shape",
            "customer_order|ck_customer_order_payment_status",
            "customer_order|ck_customer_order_revision",
            "customer_order|ck_customer_order_snapshot_sha",
            "customer_order|ck_customer_order_snapshot_type",
            "customer_order|ck_customer_order_time_order",
            "customer_order|ck_customer_order_version_token",
            "payment_event|ck_payment_event_amount",
            "payment_event|ck_payment_event_event_id",
            "payment_event|ck_payment_event_payload_ciphertext",
            "payment_event|ck_payment_event_payload_sha",
            "payment_event|ck_payment_event_provider",
            "payment_event|ck_payment_event_transaction",
            "payment_event|ck_payment_event_type",
            "payment_order|ck_payment_order_amount",
            "payment_order|ck_payment_order_currency",
            "payment_order|ck_payment_order_merchant_no",
            "payment_order|ck_payment_order_paid_shape",
            "payment_order|ck_payment_order_provider",
            "payment_order|ck_payment_order_revision",
            "payment_order|ck_payment_order_status",
            "payment_order|ck_payment_order_time_order",
            "payment_order|ck_payment_order_transaction");

    /**
     * F-PA2-PR-01: explicit frozen mapping of schema.table -> (check name -> expected PostgreSQL 16
     * canonical {@code pg_get_constraintdef} output) for all 34 V002 named checks (4/7/7/4/4/8).
     * Predicates derive from docs/14 4.6-4.11; each literal was captured from the real
     * {@code postgres:16} container after V002 applied and is compared by whole-definition equality
     * after ASCII-whitespace-only normalization. Substring/contains/prefix/regex/semantic
     * equivalence is forbidden; S2 behavior probes do not replace this inventory.
     */
    private static final Map<String, Map<String, String>> V002_CHECK_DEF_MAP;

    static {
        Map<String, Map<String, String>> map = new java.util.LinkedHashMap<>();
        map.put("customer_iam.customer_account", new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("ck_customer_account_display_name",
                        "CHECK ((((length(display_name) >= 1) AND (length(display_name) <= 200)) AND (btrim(display_name) <> ''::text)))"),
                Map.entry("ck_customer_account_status",
                        "CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text])))"),
                Map.entry("ck_customer_account_revision",
                        "CHECK ((revision >= 1))"),
                Map.entry("ck_customer_account_time_order",
                        "CHECK ((updated_at >= created_at))"))));
        map.put("customer_iam.auth_identity", new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("ck_auth_identity_provider",
                        "CHECK ((provider = ANY (ARRAY['WECHAT'::text, 'PHONE'::text])))"),
                Map.entry("ck_auth_identity_scope",
                        "CHECK ((((length(provider_scope) >= 1) AND (length(provider_scope) <= 128)) AND (btrim(provider_scope) <> ''::text)))"),
                Map.entry("ck_auth_identity_ciphertext",
                        "CHECK ((provider_subject_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text))"),
                Map.entry("ck_auth_identity_hmac",
                        "CHECK ((provider_subject_hmac ~ '^[0-9a-f]{64}$'::text))"),
                Map.entry("ck_auth_identity_status",
                        "CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text])))"),
                Map.entry("ck_auth_identity_key_version",
                        "CHECK ((key_version >= 1))"),
                Map.entry("ck_auth_identity_revision",
                        "CHECK ((revision >= 1))"))));
        map.put("customer_iam.identity_profile", new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("ck_identity_profile_status",
                        "CHECK ((real_name_status = ANY (ARRAY['UNVERIFIED'::text, 'VERIFIED'::text])))"),
                Map.entry("ck_identity_profile_legal_ciphertext",
                        "CHECK (((legal_name_ciphertext IS NULL) OR (legal_name_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text)))"),
                Map.entry("ck_identity_profile_id_ciphertext",
                        "CHECK (((id_number_ciphertext IS NULL) OR (id_number_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text)))"),
                Map.entry("ck_identity_profile_id_hmac",
                        "CHECK (((id_number_hmac IS NULL) OR (id_number_hmac ~ '^[0-9a-f]{64}$'::text)))"),
                Map.entry("ck_identity_profile_verification_ref",
                        "CHECK (((verification_ref_ciphertext IS NULL) OR (verification_ref_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text)))"),
                Map.entry("ck_identity_profile_state_shape",
                        "CHECK ((((real_name_status = 'UNVERIFIED'::text) AND (legal_name_ciphertext IS NULL) AND (id_number_ciphertext IS NULL) "
                                + "AND (id_number_hmac IS NULL) AND (verification_ref_ciphertext IS NULL) AND (verified_at IS NULL)) "
                                + "OR ((real_name_status = 'VERIFIED'::text) AND (legal_name_ciphertext IS NOT NULL) AND (id_number_ciphertext IS NOT NULL) "
                                + "AND (id_number_hmac IS NOT NULL) AND (verification_ref_ciphertext IS NOT NULL) AND (verified_at IS NOT NULL))))"),
                Map.entry("ck_identity_profile_revision",
                        "CHECK ((revision >= 1))"))));
        map.put("organization_access.organization", new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("ck_organization_display_name",
                        "CHECK ((((length(display_name) >= 1) AND (length(display_name) <= 200)) AND (btrim(display_name) <> ''::text)))"),
                Map.entry("ck_organization_status",
                        "CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'SUSPENDED'::text, 'DISABLED'::text])))"),
                Map.entry("ck_organization_revision",
                        "CHECK ((revision >= 1))"),
                Map.entry("ck_organization_time_order",
                        "CHECK ((updated_at >= created_at))"))));
        map.put("organization_access.organization_membership", new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("ck_membership_role",
                        "CHECK ((role = ANY (ARRAY['ADMINISTRATOR'::text, 'MEMBER'::text, 'EVIDENCE_MANAGER'::text, 'AUDITOR'::text])))"),
                Map.entry("ck_membership_department",
                        "CHECK (((department_label IS NULL) OR (((length(department_label) >= 1) AND (length(department_label) <= 200)) AND (btrim(department_label) <> ''::text))))"),
                Map.entry("ck_membership_status",
                        "CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'SUSPENDED'::text, 'REVOKED'::text])))"),
                Map.entry("ck_membership_revision",
                        "CHECK ((revision >= 1))"))));
        map.put("operations_iam.employee_account", new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("ck_employee_login_hmac",
                        "CHECK ((login_name_hmac ~ '^[0-9a-f]{64}$'::text))"),
                Map.entry("ck_employee_login_masked",
                        "CHECK ((((length(login_name_masked) >= 1) AND (length(login_name_masked) <= 128)) AND (btrim(login_name_masked) <> ''::text)))"),
                Map.entry("ck_employee_issuer",
                        "CHECK ((((length(auth_issuer) >= 1) AND (length(auth_issuer) <= 128)) AND (btrim(auth_issuer) <> ''::text)))"),
                Map.entry("ck_employee_subject_hmac",
                        "CHECK ((auth_subject_hmac ~ '^[0-9a-f]{64}$'::text))"),
                Map.entry("ck_employee_display_name",
                        "CHECK ((((length(display_name) >= 1) AND (length(display_name) <= 200)) AND (btrim(display_name) <> ''::text)))"),
                Map.entry("ck_employee_role",
                        "CHECK ((role = ANY (ARRAY['ADMIN'::text, 'OPS'::text])))"),
                Map.entry("ck_employee_status",
                        "CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text])))"),
                Map.entry("ck_employee_revision",
                        "CHECK ((revision >= 1))"))));
        V002_CHECK_DEF_MAP = java.util.Collections.unmodifiableMap(map);
    }

    /**
     * F-PA3-PR-01: explicit frozen mapping of table -> (check name -> expected PostgreSQL 16
     * canonical {@code pg_get_constraintdef} output) for all 25 V003 named checks (6/7/6/6).
     * Predicates derive from docs/14 4.12-4.15; each literal was captured from the real
     * {@code postgres:16} container after V001/V002/V003 applied and is compared by
     * whole-definition equality after ASCII-whitespace-only normalization.
     * Substring/contains/prefix/regex/semantic equivalence is forbidden; S2 behavior probes do
     * not replace this inventory.
     */
    private static final Map<String, Map<String, String>> V003_CHECK_DEF_MAP;

    static {
        Map<String, Map<String, String>> map = new java.util.LinkedHashMap<>();
        map.put("business_subject", new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("ck_subject_single_owner",
                        "CHECK ((num_nonnulls(owner_account_id, owner_organization_id) <= 1))"),
                Map.entry("ck_subject_source",
                        "CHECK ((source = ANY (ARRAY['IVR'::text, 'MINIAPP'::text, 'WEB'::text, 'OPERATIONS'::text])))"),
                Map.entry("ck_subject_real_name_status",
                        "CHECK ((real_name_status = ANY (ARRAY['UNVERIFIED'::text, 'VERIFIED'::text])))"),
                Map.entry("ck_subject_real_name_shape",
                        "CHECK (((real_name_status = 'VERIFIED'::text) = (real_name_verified_at IS NOT NULL)))"),
                Map.entry("ck_subject_revision",
                        "CHECK ((revision >= 1))"),
                Map.entry("ck_subject_time_order",
                        "CHECK ((updated_at >= created_at))"))));
        map.put("caller_number_binding", new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("ck_binding_number_type",
                        "CHECK ((number_type = ANY (ARRAY['MOBILE'::text, 'LANDLINE'::text])))"),
                Map.entry("ck_binding_number_ciphertext",
                        "CHECK ((number_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text))"),
                Map.entry("ck_binding_number_hmac",
                        "CHECK ((number_hmac ~ '^[0-9a-f]{64}$'::text))"),
                Map.entry("ck_binding_number_masked",
                        "CHECK ((((length(number_masked) >= 1) AND (length(number_masked) <= 64)) AND (btrim(number_masked) <> ''::text)))"),
                Map.entry("ck_binding_status",
                        "CHECK ((status = ANY (ARRAY['ACTIVE'::text, 'DISABLED'::text, 'RETIRED'::text])))"),
                Map.entry("ck_binding_verified_shape",
                        "CHECK (((status <> 'ACTIVE'::text) OR (verified_at IS NOT NULL)))"),
                Map.entry("ck_binding_revision",
                        "CHECK ((revision >= 1))"))));
        map.put("subject_claim", new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("ck_claim_verification_method",
                        "CHECK ((verification_method = ANY (ARRAY['SMS'::text, 'WECHAT_PHONE'::text, 'CALLER_CHALLENGE'::text, 'MANUAL_REVIEW'::text])))"),
                Map.entry("ck_claim_verification_ref",
                        "CHECK ((verification_ref_ciphertext ~ '^v1\\.[A-Za-z0-9._:/-]+\\..+$'::text))"),
                Map.entry("ck_claim_request_id",
                        "CHECK ((((length(request_id) >= 1) AND (length(request_id) <= 128)) AND (btrim(request_id) <> ''::text)))"),
                Map.entry("ck_claim_status",
                        "CHECK ((status = ANY (ARRAY['ACCEPTED'::text, 'REJECTED'::text, 'CONFLICT'::text])))"),
                Map.entry("ck_claim_verified_shape",
                        "CHECK (((status = 'ACCEPTED'::text) = (verified_at IS NOT NULL)))"),
                Map.entry("ck_claim_verified_after_risk",
                        "CHECK (((verified_at IS NULL) OR (verified_at >= risk_checked_at)))"))));
        map.put("subject_risk_state", new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("ck_risk_type",
                        "CHECK ((risk_type = ANY (ARRAY['RECYCLED'::text, 'CONFLICT'::text, 'MANUAL_REVIEW'::text])))"),
                Map.entry("ck_risk_state",
                        "CHECK ((state = ANY (ARRAY['OPEN'::text, 'CLEARED'::text])))"),
                Map.entry("ck_risk_source",
                        "CHECK ((source = ANY (ARRAY['CLAIM_CHECK'::text, 'OPERATIONS'::text, 'IMPORT'::text])))"),
                Map.entry("ck_risk_cleared_shape",
                        "CHECK (((state = 'CLEARED'::text) = (cleared_at IS NOT NULL)))"),
                Map.entry("ck_risk_cleared_after_observed",
                        "CHECK (((cleared_at IS NULL) OR (cleared_at >= observed_at)))"),
                Map.entry("ck_risk_revision",
                        "CHECK ((revision >= 1))"))));
        V003_CHECK_DEF_MAP = java.util.Collections.unmodifiableMap(map);
    }

    private static final List<String> DISPATCHER_UPDATE_COLUMNS = List.of(
            "status", "attempt_count", "available_at", "published_at", "revision");

    private static final List<String> DISPATCHER_UNDECLARED_COLUMNS = List.of(
            "id", "event_domain", "dedupe_key", "aggregate_type", "aggregate_id",
            "event_type", "payload_redacted", "payload_ciphertext", "payload_sha256");

    private static final Map<String, String> DISPATCHER_UNDECLARED_VALUES = Map.ofEntries(
            Map.entry("id", "pg_catalog.gen_random_uuid()"),
            Map.entry("event_domain", "'ACTIVATION'"),
            Map.entry("dedupe_key", "'decoy-dedupe-key'"),
            Map.entry("aggregate_type", "'DECOY'"),
            Map.entry("aggregate_id", "pg_catalog.gen_random_uuid()"),
            Map.entry("event_type", "'DECOY_EVENT'"),
            Map.entry("payload_redacted", "'{}'::jsonb"),
            Map.entry("payload_ciphertext", "NULL"),
            Map.entry("payload_sha256", "'" + "a".repeat(64) + "'"));

    private static final String RUNTIME_ROLE = "subject_number_role";
    private static final String LOSER_COMMIT_APPLICATION = "pa1-s2-race-commit";
    private static final String LOSER_ROLLBACK_APPLICATION = "pa1-s2-race-rollback";
    private static final Duration BLOCK_DETECTION_TIMEOUT = Duration.ofSeconds(15);
    private static final long FUTURE_TIMEOUT_SECONDS = 30;

    private static final GenericContainer<?> POSTGRES =
            new GenericContainer<>(DockerImageName.parse("postgres:16"))
                    .withEnv("POSTGRES_USER", DB_USER)
                    .withEnv("POSTGRES_DB", "onecall")
                    .withEnv("POSTGRES_PASSWORD", PASSWORD)
                    .withExposedPorts(5432)
                    .waitingFor(Wait.forLogMessage(
                            ".*database system is ready to accept connections.*\\n", 2))
                    .withStartupTimeout(Duration.ofMinutes(2));

    private static String url;
    private static Flyway flyway;
    private static MigrateResult initialMigration;

    @BeforeAll
    static void startContainerAndMigrate() throws Exception {
        POSTGRES.start();
        url = String.format(
                "jdbc:postgresql://%s:%d/onecall",
                POSTGRES.getHost(), POSTGRES.getMappedPort(5432));

        assertCleanBeforeMigration();

        flyway = Flyway.configure()
                .dataSource(url, DB_USER, PASSWORD)
                .locations("classpath:db/migration")
                .load();
        initialMigration = flyway.migrate();
    }

    @AfterAll
    static void stopContainer() {
        POSTGRES.stop();
    }

    @Test
    void v001ThroughV005MigrateCleanPostgres16WithoutSkipping() throws Exception {
        assertThat(initialMigration.migrationsExecuted).isEqualTo(5);

        try (Connection connection = openConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT version, description, type, script, success "
                                + "FROM public.flyway_schema_history ORDER BY installed_rank")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("version")).isEqualTo("001");
            assertThat(resultSet.getString("description"))
                    .isEqualTo("platform schemas roles idempotency outbox audit");
            assertThat(resultSet.getString("type")).isEqualTo("SQL");
            assertThat(resultSet.getString("script"))
                    .isEqualTo("V001__platform_schemas_roles_idempotency_outbox_audit.sql");
            assertThat(resultSet.getBoolean("success")).isTrue();

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("version")).isEqualTo("002");
            assertThat(resultSet.getString("description"))
                    .isEqualTo("identity organization and employee");
            assertThat(resultSet.getString("type")).isEqualTo("SQL");
            assertThat(resultSet.getString("script"))
                    .isEqualTo("V002__identity_organization_and_employee.sql");
            assertThat(resultSet.getBoolean("success")).isTrue();

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("version")).isEqualTo("003");
            assertThat(resultSet.getString("description"))
                    .isEqualTo("subject number and claim");
            assertThat(resultSet.getString("type")).isEqualTo("SQL");
            assertThat(resultSet.getString("script"))
                    .isEqualTo("V003__subject_number_and_claim.sql");
            assertThat(resultSet.getBoolean("success")).isTrue();

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("version")).isEqualTo("004");
            assertThat(resultSet.getString("description"))
                    .isEqualTo("catalog order and payment");
            assertThat(resultSet.getString("type")).isEqualTo("SQL");
            assertThat(resultSet.getString("script"))
                    .isEqualTo("V004__catalog_order_and_payment.sql");
            assertThat(resultSet.getBoolean("success")).isTrue();

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("version")).isEqualTo("005");
            assertThat(resultSet.getString("description"))
                    .isEqualTo("ivr task session call");
            assertThat(resultSet.getString("type")).isEqualTo("SQL");
            assertThat(resultSet.getString("script"))
                    .isEqualTo("V005__ivr_task_session_call.sql");
            assertThat(resultSet.getBoolean("success")).isTrue();
            assertThat(resultSet.next()).isFalse();
        }

        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();

        MigrateResult repeated = flyway.migrate();
        assertThat(repeated.migrationsExecuted).isZero();

        exerciseV001FunctionsAsGrantedRole();
        exerciseV002InsertsAsOwningRuntimeRolesAndRollBack();
        exerciseV003InsertsAsRuntimeRoleAndRollBack();
        exerciseV005IvrInsertsAsRuntimeRoleAndRollBack();
    }

    @Test
    void v001ThroughV003InventoryMatchesTheFrozenContract() throws Exception {
        try (Connection connection = openConnection()) {
            assertInventory(connection);
        }
    }

    @Test
    void brokenMigrationFailsVisibly() throws Exception {
        cleanupBrokenSchemas();

        try {
            try (Connection connection = openConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA " + BROKEN_HISTORY_SCHEMA);
            }

            Flyway broken = Flyway.configure()
                    .dataSource(url, DB_USER, PASSWORD)
                    .locations("classpath:db/migration-broken")
                    .schemas(BROKEN_HISTORY_SCHEMA)
                    .defaultSchema(BROKEN_HISTORY_SCHEMA)
                    .table(BROKEN_HISTORY_TABLE)
                    .load();

            try {
                broken.migrate();
                fail("the broken test migration must fail");
            } catch (FlywayException exception) {
                assertThat(causeMessages(exception))
                        .as("broken migration must fail from the intentional sentinel statement")
                        .anyMatch(message -> message.contains(BROKEN_SENTINEL));
            }

            try (Connection connection = openConnection()) {
                assertThat(toRegnamespace(connection, BROKEN_MARKER_SCHEMA)).isNull();
                assertThat(toRegclass(connection, BROKEN_MARKER_SCHEMA + ".first_valid_ddl")).isNull();

                long successfulBrokenRows = 0;
                if (toRegclass(connection, BROKEN_HISTORY_SCHEMA + "." + BROKEN_HISTORY_TABLE) != null) {
                    successfulBrokenRows = queryLong(
                            connection,
                            "SELECT count(*) FROM " + BROKEN_HISTORY_SCHEMA + "." + BROKEN_HISTORY_TABLE
                                    + " WHERE success AND version = ?",
                            BROKEN_VERSION);
                }
                assertThat(successfulBrokenRows).isZero();
            }
        } finally {
            cleanupBrokenSchemas();
        }
    }

    @Test
    void idempotencyNewReplayConflictAndFinalizeOnce() throws Exception {
        String scope = "PURCHASE";
        String key = "s2-idempotency-" + UUID.randomUUID();
        String fingerprint = randomFingerprint();
        UUID resultId = UUID.randomUUID();
        String resultSnapshot = "{\"order\":\"ok\"}";
        String resultSha256 = randomSha256();

        UUID recordId;
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            BeginResult created = callIdempotencyBegin(connection, scope, key, fingerprint);
            assertThat(created.outcome()).isEqualTo("NEW");
            assertThat(created.revision()).isEqualTo(1);
            assertThat(created.recordId()).isNotNull();
            assertThat(created.resultType()).isNull();
            assertThat(created.resultId()).isNull();
            assertThat(created.resultSnapshot()).isNull();
            assertThat(created.resultSha256()).isNull();
            recordId = created.recordId();

            assertThat(callIdempotencyComplete(
                    connection, recordId, 1, "PURCHASE_RESULT", resultId, resultSnapshot, resultSha256))
                    .as("complete finalizes the first NEW result exactly once")
                    .isEqualTo("COMPLETED");
            connection.commit();
        }

        StoredIdempotency completed = readIdempotency(scope, key);
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.revision()).isEqualTo(2);
        assertThat(completed.resultType()).isEqualTo("PURCHASE_RESULT");
        assertThat(completed.resultId()).isEqualTo(resultId);
        assertThat(completed.resultSha256()).isEqualTo(resultSha256);

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            BeginResult replay = callIdempotencyBegin(connection, scope, key, fingerprint);
            assertThat(replay.outcome()).isEqualTo("REPLAY");
            assertThat(replay.recordId()).isEqualTo(recordId);
            assertThat(replay.revision()).isEqualTo(2);
            assertThat(replay.resultType()).isEqualTo("PURCHASE_RESULT");
            assertThat(replay.resultId()).isEqualTo(resultId);
            assertThat(replay.resultSnapshot()).isEqualTo(completed.resultSnapshot());
            assertThat(replay.resultSha256()).isEqualTo(resultSha256);
            connection.rollback();
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            BeginResult conflict = callIdempotencyBegin(
                    connection, scope, key, randomFingerprint());
            assertThat(conflict.outcome()).isEqualTo("CONFLICT");
            assertThat(conflict.recordId()).isNull();
            assertThat(conflict.revision()).isNull();
            assertThat(conflict.resultType()).isNull();
            assertThat(conflict.resultId()).isNull();
            assertThat(conflict.resultSnapshot()).isNull();
            assertThat(conflict.resultSha256()).isNull();
            connection.rollback();
        }

        assertThat(readIdempotency(scope, key))
                .as("a changed fingerprint must not modify the stored first result")
                .isEqualTo(completed);

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            assertThat(callIdempotencyComplete(
                    connection, recordId, 2, "PURCHASE_RESULT", resultId, resultSnapshot, resultSha256))
                    .as("the same result hash is an idempotent completion, not a second write")
                    .isEqualTo("ALREADY_COMPLETED");
            connection.rollback();
        }

        assertThat(readIdempotency(scope, key)).isEqualTo(completed);

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            expectFailure(connection, "IDEMPOTENCY_CONFLICT", () -> callIdempotencyComplete(
                    connection, recordId, 2, "PURCHASE_RESULT", resultId, resultSnapshot, randomSha256()));
        }

        assertThat(readIdempotency(scope, key))
                .as("a different result hash must never overwrite the stored first result")
                .isEqualTo(completed);

        String staleKey = "s2-idempotency-stale-" + UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            BeginResult fresh = callIdempotencyBegin(connection, scope, staleKey, fingerprint);
            assertThat(fresh.outcome()).isEqualTo("NEW");
            assertThat(fresh.revision()).isEqualTo(1);

            expectFailure(connection, "IDEMPOTENCY_CONFLICT", () -> callIdempotencyComplete(
                    connection, fresh.recordId(), 2, "PURCHASE_RESULT", resultId, resultSnapshot, resultSha256));
        }

        assertThat(countIdempotencyRows(scope, staleKey))
                .as("the rolled-back stale-revision NEW row must leave no row")
                .isZero();
        assertThat(countIdempotencyRows(scope, key))
                .as("exactly one method-local idempotency row remains")
                .isEqualTo(1);
        assertThat(readIdempotency(scope, key)).isEqualTo(completed);
    }

    @Test
    void committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow() throws Exception {
        String scope = "IVR_CONFIRM";
        String key = "s2-committed-new-" + UUID.randomUUID();
        String fingerprint = randomFingerprint();

        UUID recordId;
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            BeginResult created = callIdempotencyBegin(connection, scope, key, fingerprint);
            assertThat(created.outcome()).isEqualTo("NEW");
            assertThat(created.revision()).isEqualTo(1);
            recordId = created.recordId();
            connection.commit();
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            expectFailure(connection, "IDEMPOTENCY_INCOMPLETE",
                    () -> callIdempotencyBegin(connection, scope, key, fingerprint));
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM platform.idempotency_record "
                            + "WHERE status = 'NEW' AND scope = ? AND idempotency_key = ?",
                    scope, key))
                    .as("the committed NEW row is visible to the alert predicate")
                    .isEqualTo(1);
            assertThat(queryString(
                    connection,
                    "SELECT id::text FROM platform.idempotency_record "
                            + "WHERE status = 'NEW' AND scope = ? AND idempotency_key = ?",
                    scope, key))
                    .isEqualTo(recordId.toString());
            assertThat(queryBoolean(
                    connection,
                    "SELECT indisvalid FROM pg_index WHERE indexrelid = "
                            + "'platform.ix_idempotency_new_alert'::regclass"))
                    .as("ix_idempotency_new_alert must be a valid index")
                    .isTrue();
            assertAlertIndexUsed(connection);
        }

        String beginOnlyKey = "s2-begin-only-rollback-" + UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            BeginResult created = callIdempotencyBegin(connection, scope, beginOnlyKey, fingerprint);
            assertThat(created.outcome()).isEqualTo("NEW");
            connection.rollback();
        }
        assertThat(countIdempotencyRows(scope, beginOnlyKey))
                .as("begin-only rollback must leave no idempotency row")
                .isZero();

        String beginCompleteKey = "s2-begin-complete-rollback-" + UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            BeginResult created = callIdempotencyBegin(connection, scope, beginCompleteKey, fingerprint);
            assertThat(created.outcome()).isEqualTo("NEW");
            assertThat(callIdempotencyComplete(
                    connection, created.recordId(), 1, "IVR_CONFIRM_RESULT",
                    UUID.randomUUID(), "{\"activation\":\"ok\"}", randomSha256()))
                    .isEqualTo("COMPLETED");
            connection.rollback();
        }
        assertThat(countIdempotencyRows(scope, beginCompleteKey))
                .as("begin-plus-complete rollback must leave no idempotency row")
                .isZero();
    }

    @Test
    void concurrentIdempotencyRetryObservesOneWinner() throws Exception {
        runConcurrentCommitRace();
        runConcurrentRollbackRace();
    }

    @Test
    void outboxReplayConflictRollbackAndDenylist() throws Exception {
        String domain = "IVR_EVENT";
        String key = "s2-outbox-" + UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        String aggregateType = "CALL";
        String eventType = "CALL_EVENT";
        String payload = "{\"eventType\":\"REPLAY\"}";
        String payloadSha256 = randomSha256();

        UUID firstId;
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            firstId = callOutboxAppend(
                    connection, domain, key, aggregateType, aggregateId, eventType, payload, null, payloadSha256);
            assertThat(firstId).isNotNull();
            connection.commit();
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            assertThat(callOutboxAppend(
                    connection, domain, key, aggregateType, aggregateId, eventType, payload, null, payloadSha256))
                    .as("same domain/dedupe key and hash returns the original outbox id")
                    .isEqualTo(firstId);
            connection.commit();
        }

        StoredOutbox stored = readOutbox(domain, key);
        assertThat(countOutboxRows(domain, key)).isEqualTo(1);

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            expectFailure(connection, "OUTBOX_DEDUPE_CONFLICT", () -> callOutboxAppend(
                    connection, domain, key, aggregateType, aggregateId, eventType, payload, null, randomSha256()));
        }
        assertThat(readOutbox(domain, key))
                .as("a changed payload hash must not modify the stored outbox row")
                .isEqualTo(stored);
        assertThat(countOutboxRows(domain, key)).isEqualTo(1);

        String rolledBackKey = "s2-outbox-rollback-" + UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            callOutboxAppend(
                    connection, domain, rolledBackKey, aggregateType, aggregateId, eventType, payload, null, payloadSha256);
            connection.rollback();
        }
        assertThat(countOutboxRows(domain, rolledBackKey))
                .as("a caller rollback must leave no outbox row")
                .isZero();

        assertOutboxRejected(domain, aggregateType, aggregateId, eventType,
                null, null, payloadSha256, "missing payload");
        assertOutboxRejected(domain, aggregateType, aggregateId, eventType,
                null, "not-a-v1-envelope", payloadSha256, "bad ciphertext envelope");
        assertOutboxRejected(domain, aggregateType, aggregateId, eventType,
                "{\"phone\":\"13800000000\"}", null, payloadSha256, "protected plaintext");
    }

    @Test
    void auditIsAppendOnlyAndRuntimeWritesAreDenied() throws Exception {
        UUID appendedObjectId = UUID.randomUUID();
        UUID directInsertObjectId = UUID.randomUUID();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);
            UUID auditId = callAppendFact(
                    connection, appendedObjectId, "SERVICE", "AUDIT_APPEND_GRANTED", null, null);
            assertThat(auditId).as("a granted runtime role appends an audit fact").isNotNull();

            setLocalRole(connection, "audit_reader_role");
            assertThat(countAuditRows(connection, appendedObjectId))
                    .as("audit_reader_role can read the appended row").isEqualTo(1);

            connection.rollback();
            assertThat(countAuditRows(connection, appendedObjectId))
                    .as("a caller rollback removes the audit row").isZero();
        }

        try (Connection connection = openConnection()) {
            setSessionRole(connection, "customer_iam_role");
            expectSqlState(connection, "42501", null, () -> execute(connection,
                    "INSERT INTO audit.audit_fact "
                            + "(id, occurred_at, actor_type, actor_id, action_code, object_type, object_id) "
                            + "VALUES (?, now(), 'SERVICE', ?, 'DIRECT_INSERT', 'ORDER', ?)",
                    UUID.randomUUID(), UUID.randomUUID(), directInsertObjectId));
        }
        assertThat(countAuditRows(directInsertObjectId))
                .as("a domain role cannot insert directly into audit.audit_fact").isZero();

        try (Connection connection = openConnection()) {
            setSessionRole(connection, "audit_reader_role");
            expectSqlState(connection, "42501", null, () -> executeUpdate(connection,
                    "UPDATE audit.audit_fact SET request_id = 'mutated' WHERE object_id = ?", appendedObjectId));
            expectSqlState(connection, "42501", null, () -> executeUpdate(connection,
                    "DELETE FROM audit.audit_fact WHERE object_id = ?", appendedObjectId));
            expectSqlState(connection, "42501", null,
                    () -> execute(connection, "TRUNCATE audit.audit_fact"));
        }

        UUID ownerUpdateObjectId = UUID.randomUUID();
        assertOwnerAuditMutationIsRejected(ownerUpdateObjectId, "UPDATE");
        UUID ownerDeleteObjectId = UUID.randomUUID();
        assertOwnerAuditMutationIsRejected(ownerDeleteObjectId, "DELETE");

        assertThat(countAuditRows(appendedObjectId))
                .as("no audit row survives the append/rollback path").isZero();
    }

    @Test
    void auditReasonAndDenylistRulesAreEnforced() throws Exception {
        assertAuditReasonRequired("EMPLOYEE", "CUSTOMER_VIEW", "employee actor");
        assertAuditReasonRequired("SERVICE", "RISK_STATE_CHANGE", "high-risk service action");

        UUID validReasonObjectId = UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);
            UUID auditId = callAppendFact(
                    connection, validReasonObjectId, "EMPLOYEE", "CUSTOMER_VIEW", "CUSTOMER_REQUEST", null);
            assertThat(auditId).as("an employee append with a reason succeeds").isNotNull();
            setLocalRole(connection, "audit_reader_role");
            assertThat(countAuditRows(connection, validReasonObjectId))
                    .as("the valid-reason row exists before rollback").isEqualTo(1);
            connection.rollback();
            assertThat(countAuditRows(connection, validReasonObjectId))
                    .as("the valid-reason rollback leaves no row").isZero();
        }

        UUID protectedKeyObjectId = UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);
            expectFailure(connection, "AUDIT_ARGUMENT_INVALID", () -> callAppendFact(
                    connection, protectedKeyObjectId, "SERVICE", "SNAPSHOT_WRITE", null,
                    "{\"phone\":\"13800000000\"}"));
        }
        assertThat(countAuditRows(protectedKeyObjectId))
                .as("a protected JSON key is rejected with no audit row").isZero();

        UUID maskedObjectId = UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);
            UUID auditId = callAppendFact(
                    connection, maskedObjectId, "SERVICE", "SNAPSHOT_WRITE", null,
                    "{\"calleeNumberMasked\":\"138****0000\",\"summary\":\"redacted\"}");
            assertThat(auditId).as("an approved masked snapshot is accepted").isNotNull();
            setLocalRole(connection, "audit_reader_role");
            assertThat(countAuditRows(connection, maskedObjectId))
                    .as("the masked-snapshot row exists before rollback").isEqualTo(1);
            connection.rollback();
            assertThat(countAuditRows(connection, maskedObjectId))
                    .as("the masked-snapshot rollback leaves no row").isZero();
        }
    }

    @Test
    void functionsHaveFixedSearchPathAndNoTransactionControl() throws Exception {
        try (Connection connection = openConnection()) {
            assertThat(queryStrings(
                    connection,
                    "SELECT n.nspname || '.' || p.proname || '|' || p.prosecdef || '|' "
                            + "|| pg_get_userbyid(p.proowner) || '|' "
                            + "|| coalesce(array_to_string(p.proconfig, ','), '') "
                            + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE (n.nspname = 'platform' AND p.proname IN "
                            + "('idempotency_begin','idempotency_complete','outbox_append')) "
                            + "OR (n.nspname = 'audit' AND p.proname = 'append_fact') ORDER BY 1"))
                    .containsExactly(
                            "audit.append_fact|true|audit_owner|search_path=audit, pg_temp",
                            "platform.idempotency_begin|true|platform_reliability_owner"
                                    + "|search_path=platform, pg_temp",
                            "platform.idempotency_complete|true|platform_reliability_owner"
                                    + "|search_path=platform, pg_temp",
                            "platform.outbox_append|true|platform_reliability_owner"
                                    + "|search_path=platform, pg_temp");

            assertThat(queryStrings(
                    connection,
                    "SELECT n.nspname || '.' || p.proname || '|' || p.prosecdef || '|' "
                            + "|| pg_get_userbyid(p.proowner) || '|' "
                            + "|| coalesce(array_to_string(p.proconfig, ','), '') "
                            + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                            + "AND p.proname LIKE 'guard_%' ORDER BY 1"))
                    .containsExactly(
                            "customer_iam.guard_auth_identity|false|customer_iam_owner"
                                    + "|search_path=customer_iam, pg_temp",
                            "customer_iam.guard_customer_account|false|customer_iam_owner"
                                    + "|search_path=customer_iam, pg_temp",
                            "customer_iam.guard_identity_profile|false|customer_iam_owner"
                                    + "|search_path=customer_iam, pg_temp",
                            "operations_iam.guard_employee_account|false|operations_iam_owner"
                                    + "|search_path=operations_iam, pg_temp",
                            "organization_access.guard_membership|false|organization_access_owner"
                                    + "|search_path=organization_access, pg_temp",
                            "organization_access.guard_organization|false|organization_access_owner"
                                    + "|search_path=organization_access, pg_temp");

            assertThat(queryStrings(
                    connection,
                    "SELECT n.nspname || '.' || p.proname || '|' || p.prosecdef || '|' "
                            + "|| pg_get_userbyid(p.proowner) || '|' "
                            + "|| coalesce(array_to_string(p.proconfig, ','), '') "
                            + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE n.nspname = 'subject_number' ORDER BY 1"))
                    .containsExactly(
                            "subject_number.guard_caller_number_binding|false|subject_number_owner"
                                    + "|search_path=subject_number, pg_temp",
                            "subject_number.guard_subject_owner|false|subject_number_owner"
                                    + "|search_path=subject_number, pg_temp",
                            "subject_number.guard_subject_risk_state|false|subject_number_owner"
                                    + "|search_path=subject_number, pg_temp",
                            "subject_number.reject_row_mutation|false|subject_number_owner"
                                    + "|search_path=subject_number, pg_temp");

            assertThat(queryStrings(
                    connection,
                    "SELECT n.nspname || '.' || p.proname || '|' || p.prosecdef || '|' "
                            + "|| pg_get_userbyid(p.proowner) || '|' "
                            + "|| coalesce(array_to_string(p.proconfig, ','), '') "
                            + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE n.nspname = 'catalog_order' ORDER BY 1"))
                    .containsExactly(
                            "catalog_order.guard_catalog_offer|false|catalog_order_owner"
                                    + "|search_path=catalog_order, pg_temp",
                            "catalog_order.guard_catalog_version_published|false|catalog_order_owner"
                                    + "|search_path=catalog_order, pg_temp",
                            "catalog_order.guard_customer_order_snapshot|false|catalog_order_owner"
                                    + "|search_path=catalog_order, pg_temp",
                            "catalog_order.guard_payment_order_identity|false|catalog_order_owner"
                                    + "|search_path=catalog_order, pg_temp",
                            "catalog_order.reject_row_mutation|false|catalog_order_owner"
                                    + "|search_path=catalog_order, pg_temp");

            assertThat(queryStrings(
                    connection,
                    "SELECT n.nspname || '.' || p.proname || '|' || p.prosecdef || '|' "
                            + "|| pg_get_userbyid(p.proowner) || '|' "
                            + "|| coalesce(array_to_string(p.proconfig, ','), '') "
                            + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE n.nspname = 'ivr' ORDER BY 1"))
                    .containsExactly(
                            "ivr.guard_call_leg|false|ivr_owner|search_path=ivr, pg_temp",
                            "ivr.guard_call_record_parties|false|ivr_owner|search_path=ivr, pg_temp",
                            "ivr.guard_inbound_session|false|ivr_owner|search_path=ivr, pg_temp",
                            "ivr.guard_outbound_call_task|false|ivr_owner|search_path=ivr, pg_temp",
                            "ivr.reject_row_mutation|false|ivr_owner|search_path=ivr, pg_temp");

            // A PL/pgSQL body uses ordinary BEGIN/END block delimiters, so this assertion
            // matches only transaction-control statements, never BEGIN/END themselves.
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE ((n.nspname = 'platform' AND p.proname IN "
                            + "('idempotency_begin','idempotency_complete','outbox_append')) "
                            + "OR (n.nspname = 'audit' AND p.proname = 'append_fact') "
                            + "OR (n.nspname IN ('customer_iam','organization_access','operations_iam') "
                            + "AND p.proname LIKE 'guard_%') "
                            + "OR (n.nspname = 'subject_number' AND p.proname IN "
                            + "('guard_subject_owner','guard_caller_number_binding',"
                            + "'guard_subject_risk_state','reject_row_mutation')) "
                            + "OR (n.nspname = 'catalog_order' AND p.proname IN "
                            + "('guard_catalog_version_published','guard_catalog_offer',"
                            + "'guard_customer_order_snapshot','guard_payment_order_identity',"
                            + "'reject_row_mutation')) "
                            + "OR (n.nspname = 'ivr' AND p.proname IN "
                            + "('guard_outbound_call_task','guard_inbound_session',"
                            + "'guard_call_record_parties','guard_call_leg','reject_row_mutation'))) "
                            + "AND p.prosrc ~* "
                            + "'\\m(commit|rollback|start\\s+transaction|set\\s+transaction)\\M'"))
                    .as("declared functions contain no transaction-control statement")
                    .isZero();

            assertThat(queryStrings(
                    connection,
                    "SELECT n.nspname || '.' || p.proname FROM pg_proc p "
                            + "JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE ((n.nspname = 'platform' AND p.proname IN "
                            + "('idempotency_begin','idempotency_complete','outbox_append')) "
                            + "OR (n.nspname = 'audit' AND p.proname = 'append_fact')) "
                            + "AND p.prosrc LIKE '%pg_catalog.gen_random_uuid()%' ORDER BY 1"))
                    .containsExactly(
                            "audit.append_fact",
                            "platform.idempotency_begin",
                            "platform.outbox_append");

            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                            + "AND p.proname LIKE 'guard_%' "
                            + "AND p.prosrc LIKE '%gen_random_uuid%'"))
                    .as("V002 guards never generate IDs")
                    .isZero();

            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE n.nspname = 'subject_number' "
                            + "AND p.prosrc LIKE '%gen_random_uuid%'"))
                    .as("V003 trigger functions never generate IDs")
                    .isZero();

            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE n.nspname = 'catalog_order' "
                            + "AND p.prosrc LIKE '%gen_random_uuid%'"))
                    .as("V004 trigger functions never generate IDs")
                    .isZero();

            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE n.nspname = 'ivr' "
                            + "AND p.prosrc LIKE '%gen_random_uuid%'"))
                    .as("V005 trigger functions never generate IDs")
                    .isZero();

            assertThat(queryLong(
                    connection, "SELECT count(*) FROM pg_proc WHERE proname = 'issue_first_trial'"))
                    .as("issue_first_trial is a V006 function absent from V001-V005")
                    .isZero();
            assertThat(queryStrings(
                    connection,
                    "SELECT n.nspname || '.' || p.proname FROM pg_proc p "
                            + "JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE p.proname IN "
                            + "('select_call_action','record_activation_next_action')"))
                    .as("no IVR SQL service function exists at V005")
                    .isEmpty();
        }
    }

    @Test
    void ownerPublicAndRoleExecutionBoundariesAreExact() throws Exception {
        String eventDomain = "IVR_EVENT";
        String dedupeKey = "s3-dispatcher-" + UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        String payloadSha256 = randomSha256();

        try {
            try (Connection connection = openConnection()) {
                assertThat(queryStrings(
                        connection,
                        "SELECT rolname || ':' || rolcanlogin || ':' || rolsuper || ':' "
                                + "|| rolcreatedb || ':' || rolcreaterole FROM pg_roles "
                                + "WHERE rolname IN ('platform_reliability_owner','audit_owner',"
                                + "'customer_iam_owner','organization_access_owner',"
                                + "'operations_iam_owner','subject_number_owner') ORDER BY rolname"))
                        .containsExactly(
                                "audit_owner:false:false:false:false",
                                "customer_iam_owner:false:false:false:false",
                                "operations_iam_owner:false:false:false:false",
                                "organization_access_owner:false:false:false:false",
                                "platform_reliability_owner:false:false:false:false",
                                "subject_number_owner:false:false:false:false");

                assertThat(queryLong(
                        connection,
                        "SELECT count(*) FROM pg_namespace n, "
                                + "aclexplode(coalesce(n.nspacl, acldefault('n', n.nspowner))) a "
                                + "WHERE n.nspname IN ('public','platform','audit','customer_iam',"
                                + "'organization_access','operations_iam','subject_number') "
                                + "AND a.grantee = 0"))
                        .as("PUBLIC has no schema USAGE or CREATE").isZero();
                assertThat(queryLong(
                        connection,
                        "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace, "
                                + "aclexplode(coalesce(c.relacl, acldefault('r', c.relowner))) a "
                                + "WHERE n.nspname IN ('platform','audit') "
                                + "AND c.relkind IN ('r','p') AND a.grantee = 0"))
                        .as("PUBLIC has no table privilege").isZero();
                assertThat(queryLong(
                        connection,
                        "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace, "
                                + "aclexplode(coalesce(p.proacl, acldefault('f', p.proowner))) a "
                                + "WHERE n.nspname IN ('platform','audit') AND a.grantee = 0"))
                        .as("PUBLIC has no function EXECUTE").isZero();

                for (String role : PLATFORM_USAGE_ROLES) {
                    assertThat(hasSchema(connection, role, "platform", "USAGE"))
                            .as("%s platform USAGE", role).isTrue();
                    assertThat(hasSchema(connection, role, "platform", "CREATE"))
                            .as("%s platform CREATE", role).isFalse();
                }
                for (String role : AUDIT_USAGE_ROLES) {
                    assertThat(hasSchema(connection, role, "audit", "USAGE"))
                            .as("%s audit USAGE", role).isTrue();
                    assertThat(hasSchema(connection, role, "audit", "CREATE"))
                            .as("%s audit CREATE", role).isFalse();
                }
                assertThat(hasSchema(connection, "platform_reliability_role", "audit", "USAGE")).isFalse();
                assertThat(hasSchema(connection, "platform_dispatcher_role", "audit", "USAGE")).isFalse();
                assertThat(hasSchema(connection, "audit_reader_role", "platform", "USAGE")).isFalse();
                assertThat(hasSchema(connection, "subject_identity_verification_role", "platform", "USAGE"))
                        .isFalse();
                assertThat(hasSchema(connection, "operations_service_role", "platform", "USAGE")).isFalse();

                for (String role : DOMAIN_ROLES) {
                    for (String function : PLATFORM_FUNCTIONS) {
                        assertThat(hasFunction(connection, role, function, "EXECUTE"))
                                .as("%s EXECUTE %s", role, function).isTrue();
                    }
                    assertThat(hasFunction(connection, role, AUDIT_FUNCTION, "EXECUTE"))
                            .as("%s EXECUTE audit.append_fact", role).isTrue();
                }
                for (String role : List.of(
                        "subject_identity_verification_role", "operations_service_role")) {
                    assertThat(hasFunction(connection, role, AUDIT_FUNCTION, "EXECUTE"))
                            .as("%s EXECUTE audit.append_fact", role).isTrue();
                    for (String function : PLATFORM_FUNCTIONS) {
                        assertThat(hasFunction(connection, role, function, "EXECUTE"))
                                .as("%s must not EXECUTE %s", role, function).isFalse();
                    }
                }
                for (String role : List.of(
                        "platform_reliability_role", "platform_dispatcher_role", "audit_reader_role")) {
                    for (String function : PLATFORM_FUNCTIONS) {
                        assertThat(hasFunction(connection, role, function, "EXECUTE"))
                                .as("%s must not EXECUTE %s", role, function).isFalse();
                    }
                    assertThat(hasFunction(connection, role, AUDIT_FUNCTION, "EXECUTE"))
                            .as("%s must not EXECUTE audit.append_fact", role).isFalse();
                }

                assertTablePrivileges(connection, "platform_reliability_role",
                        "platform.idempotency_record", true, false, false, false, false);
                assertTablePrivileges(connection, "platform_reliability_role",
                        "platform.outbox_event", true, false, false, false, false);
                assertTablePrivileges(connection, "platform_reliability_role",
                        "audit.audit_fact", false, false, false, false, false);
                assertTablePrivileges(connection, "platform_dispatcher_role",
                        "platform.outbox_event", true, false, false, false, false);
                assertTablePrivileges(connection, "platform_dispatcher_role",
                        "platform.idempotency_record", false, false, false, false, false);
                assertTablePrivileges(connection, "platform_dispatcher_role",
                        "audit.audit_fact", false, false, false, false, false);
                assertTablePrivileges(connection, "audit_reader_role",
                        "audit.audit_fact", true, false, false, false, false);
                assertTablePrivileges(connection, "audit_reader_role",
                        "platform.idempotency_record", false, false, false, false, false);
                assertTablePrivileges(connection, "audit_reader_role",
                        "platform.outbox_event", false, false, false, false, false);
                for (String role : List.of(
                        "customer_iam_role", "organization_access_role", "operations_iam_role",
                        "subject_number_role", "subject_identity_verification_role",
                        "operations_service_role")) {
                    for (String table : S1_TABLES) {
                        assertTablePrivileges(connection, role, table, false, false, false, false, false);
                    }
                }

                for (String column : DISPATCHER_UPDATE_COLUMNS) {
                    assertThat(hasColumn(
                            connection, "platform_dispatcher_role", "platform.outbox_event", column, "UPDATE"))
                            .as("dispatcher UPDATE %s", column).isTrue();
                }
                for (String column : DISPATCHER_UNDECLARED_COLUMNS) {
                    assertThat(hasColumn(
                            connection, "platform_dispatcher_role", "platform.outbox_event", column, "UPDATE"))
                            .as("dispatcher must not UPDATE %s", column).isFalse();
                }
                assertThat(hasColumn(
                        connection, "platform_reliability_role", "platform.outbox_event", "status", "UPDATE"))
                        .isFalse();
            }

            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                setLocalRole(connection, RUNTIME_ROLE);
                UUID outboxId = callOutboxAppend(
                        connection, eventDomain, dedupeKey, "CALL", aggregateId, "CALL_EVENT",
                        "{\"eventType\":\"DISPATCHER\"}", null, payloadSha256);
                assertThat(outboxId).isNotNull();
                connection.commit();
            }

            try (Connection connection = openConnection()) {
                setSessionRole(connection, "platform_dispatcher_role");
                assertThat(executeUpdate(
                        connection,
                        "UPDATE platform.outbox_event SET status = 'PUBLISHED', "
                                + "attempt_count = attempt_count + 1, available_at = now(), "
                                + "published_at = now(), revision = revision + 1 "
                                + "WHERE event_domain = ? AND dedupe_key = ?", eventDomain, dedupeKey))
                        .as("the dispatcher updates exactly the five declared columns").isEqualTo(1);
            }

            StoredOutbox published = readOutbox(eventDomain, dedupeKey);
            assertThat(published.status()).isEqualTo("PUBLISHED");
            assertThat(published.attemptCount()).isEqualTo(1);
            assertThat(published.publishedAt()).isNotNull();
            assertThat(published.revision()).isEqualTo(2);

            try (Connection connection = openConnection()) {
                setSessionRole(connection, "platform_dispatcher_role");
                for (String column : DISPATCHER_UNDECLARED_COLUMNS) {
                    String value = DISPATCHER_UNDECLARED_VALUES.get(column);
                    expectSqlState(connection, "42501", null, () -> execute(
                            connection,
                            "UPDATE platform.outbox_event SET " + column + " = " + value
                                    + " WHERE event_domain = ? AND dedupe_key = ?", eventDomain, dedupeKey));
                }
                expectSqlState(connection, "42501", null, () -> execute(
                        connection,
                        "DELETE FROM platform.outbox_event WHERE event_domain = ? AND dedupe_key = ?",
                        eventDomain, dedupeKey));
                expectSqlState(connection, "42501", null,
                        () -> execute(connection, "TRUNCATE platform.outbox_event"));
            }

            assertThat(readOutbox(eventDomain, dedupeKey))
                    .as("denied dispatcher mutations leave the row unchanged")
                    .isEqualTo(published);
        } finally {
            try (Connection connection = openConnection();
                    PreparedStatement prepared = connection.prepareStatement(
                            "DELETE FROM platform.outbox_event WHERE event_domain = ? AND dedupe_key = ?")) {
                prepared.setString(1, eventDomain);
                prepared.setString(2, dedupeKey);
                prepared.executeUpdate();
            }
        }
    }

    @Test
    void hostileSearchPathCannotRedirectFunctionEffects() throws Exception {
        String decoySchema = "pa1_decoy_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String scope = "PURCHASE";
        String key = "s3-hostile-search-path-" + UUID.randomUUID();
        String fingerprint = randomFingerprint();

        try {
            try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE SCHEMA " + decoySchema);
                statement.execute("CREATE TABLE " + decoySchema + ".idempotency_record ("
                        + "id uuid NOT NULL PRIMARY KEY, scope text NOT NULL, idempotency_key text NOT NULL)");
                statement.execute("CREATE FUNCTION " + decoySchema + ".idempotency_begin("
                        + "p_scope text, p_key text, p_fingerprint text) RETURNS TABLE ("
                        + "record_id uuid, revision integer, outcome text, result_type text, "
                        + "result_id uuid, result_snapshot jsonb, result_sha256 text) "
                        + "LANGUAGE plpgsql AS $decoy$ BEGIN "
                        + "INSERT INTO " + decoySchema + ".idempotency_record (id, scope, idempotency_key) "
                        + "VALUES (pg_catalog.gen_random_uuid(), p_scope, p_key); "
                        + "RETURN QUERY SELECT NULL::uuid, 666, 'DECOY'::text, NULL::text, "
                        + "NULL::uuid, NULL::jsonb, NULL::text; END; $decoy$");
            }

            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                setLocalRole(connection, RUNTIME_ROLE);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET LOCAL search_path = " + decoySchema + ", pg_catalog");
                }
                assertThat(queryString(connection, "SELECT current_setting('search_path')"))
                        .as("the caller-controlled search_path points at the decoy schema")
                        .contains(decoySchema);

                BeginResult result = callIdempotencyBegin(connection, scope, key, fingerprint);
                assertThat(result.outcome())
                        .as("the schema-qualified real function cannot be redirected to the decoy")
                        .isEqualTo("NEW");
                assertThat(result.revision()).isEqualTo(1);
                connection.commit();
            }

            assertThat(countIdempotencyRows(scope, key))
                    .as("the real platform.idempotency_record receives exactly one row")
                    .isEqualTo(1);
            try (Connection connection = openConnection()) {
                assertThat(queryLong(
                        connection, "SELECT count(*) FROM " + decoySchema + ".idempotency_record"))
                        .as("the decoy table remains untouched").isZero();
            }
        } finally {
            try (Connection connection = openConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("DROP SCHEMA IF EXISTS " + decoySchema + " CASCADE");
            }
            try (Connection connection = openConnection();
                    PreparedStatement prepared = connection.prepareStatement(
                            "DELETE FROM platform.idempotency_record WHERE scope = ? AND idempotency_key = ?")) {
                prepared.setString(1, scope);
                prepared.setString(2, key);
                prepared.executeUpdate();
            }
        }
    }

    @Test
    void mutableHeadsEnforceLifecycleRevisionAndNamedChecks() throws Exception {
        assertSixTableDefaultsAndConditionalRevisions();
        assertEveryV002NamedCheckRejectsAViolation();
        assertForeignKeyMissingParentAndParentDeleteRestrict();
        assertNoIdDefaultsOrSequences();
    }

    @Test
    void localImmutabilityGuardsRejectRelationAndRotationChanges() throws Exception {
        assertLocalImmutabilityGuardsAndAuthRotation();
    }

    @Test
    void identityCollisionAndMembershipRevokeRegrantSemantics() throws Exception {
        assertIdentityCollisionAndMembershipReplacementSemantics();
    }

    @Test
    void concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner() throws Exception {
        runConcurrentIdentityCollisionCommitRace();
        runConcurrentIdentityCollisionRollbackRace();
        runConcurrentMembershipCollisionCommitRace();
        runConcurrentMembershipCollisionRollbackRace();
    }

    @Test
    void runtimeAndCrossModulePrivilegesAreExactAndWritesDenied() throws Exception {
        assertRuntimeAndCrossModulePrivilegesAreExactAndWritesDenied();
    }

    @Test
    void lastEnabledAdminTransactionIsSerializedAndFailsClosed() throws Exception {
        assertOperationsIamRoleCanForUpdateWithColumnUpdate();
        assertLastEnabledAdminSequentialFailClosedAndRevisionIncrements();
        runConcurrentLastEnabledAdminMutationRace();
    }

    @Test
    void v003SubjectNumberCatalogAndGuardsAreExact() throws Exception {
        try (Connection connection = openConnection()) {
            assertV003TablesColumnsChecksIndexesFks(connection);
            assertV003CheckPredicateInventory(connection);
            assertV003FunctionsAndTriggersExact(connection);
            assertV003NoIdDefaultsOrSequences(connection);
        }
    }

    @Test
    void v003OwnerPublicAndRuntimeAclsAreExact() throws Exception {
        try (Connection connection = openConnection()) {
            assertV003SchemaUsageExactAndNoCreate(connection);
            assertV003OwnedAndCrossModuleGrants(connection);
            assertV003PublicAclIsZero(connection);
            assertV003GuardFunctionExecuteBoundaries(connection);
        }
    }

    // PA3 V003 S2: executable data, lifecycle and concurrency backstops. Methods 21-24 run in a
    // single rolled-back transaction (savepoints isolate expected failures), so no test row,
    // especially no append-only claim row, survives. The concurrency method commits only
    // deletable binding/subject rows and removes them FK-safely.
    @Test
    void subjectOwnerSetOnceAndRealNameSeparation() throws Exception {
        assertSubjectOwnerSetOnceAndRealNameSeparation();
    }

    @Test
    void bindingLifecycleTerminalityAndActiveNumberUniqueness() throws Exception {
        assertBindingLifecycleTerminalityAndActiveNumberUniqueness();
    }

    @Test
    void appendOnlyClaimAndForeignKeyBackstops() throws Exception {
        assertAppendOnlyClaimAndForeignKeyBackstops();
    }

    @Test
    void riskOpenClearReopenRevisionProtocol() throws Exception {
        assertRiskOpenClearReopenRevisionProtocol();
    }

    @Test
    void concurrentActiveBindingUniquenessHasOneWinner() throws Exception {
        runConcurrentActiveBindingCommitRace();
        runConcurrentActiveBindingRollbackRace();
    }

    // PA4 V004 S1: exact catalog/ACL inventory and guard boundary. No lifecycle/concurrency
    // behavior method belongs to S1; those arrive in S2.
    @Test
    void v004CatalogOrderInventoryAndGuardsAreExact() throws Exception {
        try (Connection connection = openConnection()) {
            assertV004TablesColumnsChecksIndexesFks(connection);
            assertV004CheckPredicateInventory(connection);
            assertV004FunctionsAndTriggersExact(connection);
            assertV004NoIdDefaultsOrSequences(connection);
        }
    }

    @Test
    void v004OwnerPublicRuntimeAndCrossModuleAclsAreExact() throws Exception {
        try (Connection connection = openConnection()) {
            assertV004SchemaUsageMatrix(connection);
            assertV004OwnedAndCrossModuleGrants(connection);
            assertV004PublicAclIsZero(connection);
            assertV004GuardFunctionExecuteBoundaries(connection);
        }
    }

    // PA4 V004 S2: executable lifecycle, guard, FK and bounded concurrency backstops. No
    // application service exists; these methods prove database-visible behavior only.
    @Test
    void v004CatalogVersionLifecycleAndGuardBackstops() throws Exception {
        assertV004CatalogVersionLifecycleAndGuardBackstops();
    }

    @Test
    void v004OrderPaymentLifecycleAndAppendOnlyBackstops() throws Exception {
        assertV004OrderPaymentLifecycleAndAppendOnlyBackstops();
    }

    @Test
    void v004ForeignKeyAndAbsentConsentFkBackstops() throws Exception {
        assertV004ForeignKeyAndAbsentConsentFkBackstops();
    }

    @Test
    void concurrentCatalogPublicationAndCommerceIdentityHasOneWinner() throws Exception {
        runConcurrentCatalogPublicationAndCommerceIdentity();
    }

    // PA5 V005 S1: exact IVR inventory/ACL and guard boundary. No lifecycle/concurrency
    // behavior method belongs to S1; those arrive in S2.
    @Test
    void v005IvrTaskSessionCallInventoryAndGuardShapesAreExact() throws Exception {
        try (Connection connection = openConnection()) {
            assertV005TablesColumnsChecksIndexesFks(connection);
            assertV005CheckPredicateInventory(connection);
            assertV005FunctionsAndTriggersExact(connection);
            assertV005NoIdDefaultsOrSequences(connection);
            assertNoV006PlusObjects(connection);
        }
        // PA5 S1 fix-1 (F-PA5-S1-01/02): bounded regression probes for the user-authorized
        // frozen CHECK errata, run inside the suite with a rolled-back fixture transaction.
        assertV005ErrataCheckRegressionProbes();
    }

    @Test
    void v005OwnerPublicRuntimeAndCrossModuleAclsAreExact() throws Exception {
        try (Connection connection = openConnection()) {
            assertV005SchemaUsageMatrix(connection);
            assertV005OwnedAndCrossModuleGrants(connection);
            assertV005PublicAclIsZero(connection);
            assertV005GuardFunctionExecuteBoundaries(connection);
        }
        // PA5 S1 fix-1 (F-PA5-S1-03): plan §6.2 item 9 executable runtime 42501 denial
        // evidence inside this existing method; no new @Test and no catalog weakening.
        assertV005RuntimeDenialsAndLegalUpdateControl();
    }

    // PA5 V005 S2: executable lifecycle, guard, FK, append-only and bounded concurrency
    // backstops. No application service exists; these methods prove database-visible
    // behavior only. Claim/reservation/decision orchestration, expiry housekeeping and
    // production Q-input decisions remain service-owned and are labeled as shapes only.
    @Test
    void v005TaskTtlLifecycleAndGuardBackstops() throws Exception {
        assertV005TaskTtlLifecycleAndGuardBackstops();
    }

    @Test
    void v005SessionCallCanonicalCallerAndReciprocalLinkBackstops() throws Exception {
        assertV005SessionCallCanonicalCallerAndReciprocalLinkBackstops();
    }

    @Test
    void v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops() throws Exception {
        assertV005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops();
    }

    @Test
    void v005ForeignKeyAndDeferredReservationFkBackstops() throws Exception {
        assertV005ForeignKeyAndDeferredReservationFkBackstops();
    }

    @Test
    void concurrentV005TaskClaimAndActiveCallHasOneWinner() throws Exception {
        runConcurrentV005TaskClaimAndActiveCall();
    }

    /**
     * S3 T-S1-09 last-enabled-admin protocol as exact transient SQL (no database function):
     * advisory lock, target row FOR UPDATE, enabled-admin count, then either a conditional
     * revision update or a stable LAST_ENABLED_ADMIN failure. The caller must already run in a
     * transactional connection with {@code SET LOCAL ROLE operations_iam_role}.
     */
    private static final String ADMIN_MUTATION_ADVISORY_LOCK_SQL =
            "SELECT pg_advisory_xact_lock(hashtextextended('operations_iam.admin_mutation', 0))";

    private enum AdminMutationAction {
        DISABLE, DEMOTE
    }

    private static void assertRuntimeAndCrossModulePrivilegesAreExactAndWritesDenied()
            throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                assertOwnedTableRuntimePrivileges(
                        connection, accountId, identityId, profileId,
                        organizationId, membershipId, employeeId);
                assertCrossModuleRuntimePrivileges(
                        connection, accountId, organizationId, membershipId, employeeId);
                assertSchemaCreateDeniedAtRuntime(connection);
                assertGuardFunctionRuntimeExecuteBoundaries(connection);
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertSchemaUsageExactAndNoCreate(connection);
            assertGuardFunctionCatalogExecuteBoundaries(connection);
        }
    }

    private static void assertOwnedTableRuntimePrivileges(
            Connection connection,
            UUID accountId,
            UUID identityId,
            UUID profileId,
            UUID organizationId,
            UUID membershipId,
            UUID employeeId) throws SQLException {
        switchLocalRole(connection, "customer_iam_role");
        assertThat(insertCustomerAccount(connection, accountId, "S3 Priv Customer")).isEqualTo(1);
        assertThat(insertAuthIdentity(connection, identityId, accountId, "PHONE",
                "s3-priv-scope", "v1.s3.priv.cipher", randomSha256())).isEqualTo(1);
        assertThat(insertIdentityProfile(connection, profileId, accountId)).isEqualTo(1);
        assertThat(queryLong(connection,
                "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", accountId))
                .isEqualTo(1);
        assertThat(queryLong(connection,
                "SELECT count(*) FROM customer_iam.auth_identity WHERE id = ?", identityId))
                .isEqualTo(1);
        assertThat(queryLong(connection,
                "SELECT count(*) FROM customer_iam.identity_profile WHERE id = ?", profileId))
                .isEqualTo(1);
        assertThat(executeUpdate(connection,
                "UPDATE customer_iam.customer_account SET display_name = ?, status = status, "
                        + "updated_at = now(), revision = revision + 1 WHERE id = ? AND revision = 1",
                "S3 Priv Customer Updated", accountId)).isEqualTo(1);
        assertThat(executeUpdate(connection,
                "UPDATE customer_iam.auth_identity SET status = status, verified_at = verified_at, "
                        + "key_version = key_version, revision = revision + 1 "
                        + "WHERE id = ? AND revision = 1", identityId)).isEqualTo(1);
        assertThat(executeUpdate(connection,
                "UPDATE customer_iam.identity_profile SET revision = revision + 1 "
                        + "WHERE id = ? AND revision = 1", profileId)).isEqualTo(1);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.customer_account SET id = ? WHERE id = ?",
                UUID.randomUUID(), accountId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.auth_identity SET account_id = ? WHERE id = ?",
                accountId, identityId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.identity_profile SET account_id = ? WHERE id = ?",
                accountId, profileId);
        expectSqlFailure(connection, "42501", null,
                "DELETE FROM customer_iam.customer_account WHERE id = ?", accountId);
        expectSqlFailure(connection, "42501", null,
                "TRUNCATE customer_iam.customer_account");
        expectSqlFailure(connection, "42501", null,
                "DELETE FROM customer_iam.auth_identity WHERE id = ?", identityId);
        expectSqlFailure(connection, "42501", null,
                "DELETE FROM customer_iam.identity_profile WHERE id = ?", profileId);

        switchLocalRole(connection, "organization_access_role");
        assertThat(insertOrganization(connection, organizationId, "S3 Priv Org")).isEqualTo(1);
        assertThat(insertMembership(connection, membershipId, organizationId, accountId, "MEMBER"))
                .isEqualTo(1);
        assertThat(queryLong(connection,
                "SELECT count(*) FROM organization_access.organization WHERE id = ?", organizationId))
                .isEqualTo(1);
        assertThat(queryLong(connection,
                "SELECT count(*) FROM organization_access.organization_membership WHERE id = ?",
                membershipId)).isEqualTo(1);
        assertThat(executeUpdate(connection,
                "UPDATE organization_access.organization SET display_name = ?, status = status, "
                        + "updated_at = now(), revision = revision + 1 WHERE id = ? AND revision = 1",
                "S3 Priv Org Updated", organizationId)).isEqualTo(1);
        assertThat(executeUpdate(connection,
                "UPDATE organization_access.organization_membership SET role = 'MEMBER', "
                        + "department_label = ?, status = status, revision = revision + 1 "
                        + "WHERE id = ? AND revision = 1", "S3 Priv Dept", membershipId))
                .isEqualTo(1);
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization SET id = ? WHERE id = ?",
                UUID.randomUUID(), organizationId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization_membership SET organization_id = ? WHERE id = ?",
                UUID.randomUUID(), membershipId);
        expectSqlFailure(connection, "42501", null,
                "DELETE FROM organization_access.organization WHERE id = ?", organizationId);
        expectSqlFailure(connection, "42501", null,
                "TRUNCATE organization_access.organization");
        expectSqlFailure(connection, "42501", null,
                "DELETE FROM organization_access.organization_membership WHERE id = ?", membershipId);

        switchLocalRole(connection, "operations_iam_role");
        assertThat(insertEmployee(connection, employeeId, randomSha256(), "s3***priv",
                "onecall-ops", randomSha256(), "S3 Priv Ops", "OPS")).isEqualTo(1);
        assertThat(queryLong(connection,
                "SELECT count(*) FROM operations_iam.employee_account WHERE id = ?", employeeId))
                .isEqualTo(1);
        assertThat(executeUpdate(connection,
                "UPDATE operations_iam.employee_account SET display_name = ?, status = status, "
                        + "role = role, revision = revision + 1 WHERE id = ? AND revision = 1",
                "S3 Priv Ops Updated", employeeId)).isEqualTo(1);
        expectSqlFailure(connection, "42501", null,
                "UPDATE operations_iam.employee_account SET login_name_hmac = ? WHERE id = ?",
                randomSha256(), employeeId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE operations_iam.employee_account SET id = ? WHERE id = ?",
                UUID.randomUUID(), employeeId);
        expectSqlFailure(connection, "42501", null,
                "DELETE FROM operations_iam.employee_account WHERE id = ?", employeeId);
        expectSqlFailure(connection, "42501", null,
                "TRUNCATE operations_iam.employee_account");
    }

    private static void assertCrossModuleRuntimePrivileges(
            Connection connection,
            UUID accountId,
            UUID organizationId,
            UUID membershipId,
            UUID employeeId) throws SQLException {
        switchLocalRole(connection, "customer_iam_role");
        assertThat(queryStrings(connection,
                "SELECT id, display_name, status FROM organization_access.organization WHERE id = ?",
                organizationId)).hasSize(1);
        assertThat(queryStrings(connection,
                "SELECT organization_id, account_id, role, status "
                        + "FROM organization_access.organization_membership WHERE account_id = ?",
                accountId)).hasSize(1);
        expectSqlQueryFailure(connection, "42501",
                "SELECT revision FROM organization_access.organization WHERE id = ?", organizationId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT id FROM organization_access.organization WHERE id = ? FOR UPDATE", organizationId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT id FROM organization_access.organization_membership WHERE account_id = ?",
                accountId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT account_id FROM organization_access.organization_membership "
                        + "WHERE account_id = ? FOR UPDATE",
                accountId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization SET status = 'DISABLED' WHERE id = ?",
                organizationId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization_membership SET status = 'SUSPENDED' WHERE id = ?",
                membershipId);
        expectSqlFailure(connection, "42501", null,
                "INSERT INTO organization_access.organization (id, display_name) VALUES (?, 'X')",
                UUID.randomUUID());
        expectSqlFailure(connection, "42501", null,
                "DELETE FROM organization_access.organization WHERE id = ?", organizationId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT id FROM operations_iam.employee_account WHERE id = ?", employeeId);

        switchLocalRole(connection, "organization_access_role");
        expectSqlQueryFailure(connection, "42501",
                "SELECT id FROM customer_iam.customer_account WHERE id = ?", accountId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT id FROM operations_iam.employee_account WHERE id = ?", employeeId);

        switchLocalRole(connection, "operations_iam_role");
        expectSqlQueryFailure(connection, "42501",
                "SELECT id FROM customer_iam.customer_account WHERE id = ?", accountId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT id FROM organization_access.organization WHERE id = ?", organizationId);

        switchLocalRole(connection, "subject_number_role");
        assertThat(queryStrings(connection,
                "SELECT id, status FROM customer_iam.customer_account WHERE id = ?", accountId))
                .hasSize(1);
        assertThat(queryStrings(connection,
                "SELECT id, status FROM organization_access.organization WHERE id = ?", organizationId))
                .hasSize(1);
        assertThat(queryStrings(connection,
                "SELECT organization_id, account_id, role, status "
                        + "FROM organization_access.organization_membership WHERE account_id = ?",
                accountId)).hasSize(1);
        expectSqlQueryFailure(connection, "42501",
                "SELECT display_name FROM customer_iam.customer_account WHERE id = ?", accountId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT id FROM customer_iam.customer_account WHERE id = ? FOR UPDATE", accountId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT display_name FROM organization_access.organization WHERE id = ?", organizationId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT id FROM organization_access.organization WHERE id = ? FOR UPDATE", organizationId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT id FROM organization_access.organization_membership WHERE account_id = ?",
                accountId);
        expectSqlQueryFailure(connection, "42501",
                "SELECT account_id FROM organization_access.organization_membership "
                        + "WHERE account_id = ? FOR UPDATE",
                accountId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.customer_account SET status = 'DISABLED' WHERE id = ?", accountId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization SET status = 'DISABLED' WHERE id = ?", organizationId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization_membership SET status = 'SUSPENDED' WHERE id = ?",
                membershipId);
    }

    private static void assertSchemaCreateDeniedAtRuntime(Connection connection) throws SQLException {
        switchLocalRole(connection, "customer_iam_role");
        expectSqlFailure(connection, "42501", null,
                "CREATE TABLE customer_iam.s3_runtime_probe_forbidden (x int)");
        switchLocalRole(connection, "organization_access_role");
        expectSqlFailure(connection, "42501", null,
                "CREATE TABLE organization_access.s3_runtime_probe_forbidden (x int)");
        switchLocalRole(connection, "operations_iam_role");
        expectSqlFailure(connection, "42501", null,
                "CREATE TABLE operations_iam.s3_runtime_probe_forbidden (x int)");
        try (Statement statement = connection.createStatement()) {
            statement.execute("RESET ROLE");
        }
        assertThat(queryLong(connection,
                "SELECT count(*) FROM pg_class WHERE relname = 's3_runtime_probe_forbidden'"))
                .isZero();
    }

    private static void assertGuardFunctionRuntimeExecuteBoundaries(Connection connection)
            throws SQLException {
        switchLocalRole(connection, "customer_iam_role");
        Savepoint owningGuard = connection.setSavepoint("s3-owning-guard");
        try {
            execute(connection, "SELECT customer_iam.guard_customer_account()");
            fail("expected the owning runtime guard invocation to fail outside trigger context");
        } catch (SQLException exception) {
            assertThat(exception.getSQLState())
                    .as("owning runtime role must pass the guard EXECUTE check")
                    .isNotEqualTo("42501");
            assertThat(exception.getMessage().toLowerCase(java.util.Locale.ROOT))
                    .as("owning runtime guard reaches the trigger-context guard")
                    .contains("trigger");
        } finally {
            connection.rollback(owningGuard);
        }

        switchLocalRole(connection, "subject_number_role");
        expectSqlQueryFailure(connection, "42501",
                "SELECT customer_iam.guard_customer_account()");
        switchLocalRole(connection, "operations_iam_role");
        expectSqlQueryFailure(connection, "42501",
                "SELECT customer_iam.guard_customer_account()");
        switchLocalRole(connection, "organization_access_role");
        expectSqlQueryFailure(connection, "42501",
                "SELECT operations_iam.guard_employee_account()");
        switchLocalRole(connection, "customer_iam_role");
        expectSqlQueryFailure(connection, "42501",
                "SELECT operations_iam.guard_employee_account()");
    }

    private static void assertSchemaUsageExactAndNoCreate(Connection connection) throws SQLException {
        Map<String, List<String>> schemaUsageRoles = new java.util.LinkedHashMap<>();
        schemaUsageRoles.put("customer_iam", List.of("customer_iam_role", "subject_number_role"));
        schemaUsageRoles.put("organization_access",
                List.of("organization_access_role", "customer_iam_role", "subject_number_role"));
        schemaUsageRoles.put("operations_iam", List.of("operations_iam_role"));
        List<String> runtimeRoles = List.of(
                "customer_iam_role", "organization_access_role", "operations_iam_role",
                "subject_number_role");
        for (Map.Entry<String, List<String>> entry : schemaUsageRoles.entrySet()) {
            String schema = entry.getKey();
            for (String role : runtimeRoles) {
                assertThat(hasSchema(connection, role, schema, "USAGE"))
                        .as("%s USAGE %s", role, schema)
                        .isEqualTo(entry.getValue().contains(role));
                assertThat(hasSchema(connection, role, schema, "CREATE"))
                        .as("%s must not CREATE in %s", role, schema)
                        .isFalse();
            }
            assertThat(hasSchema(connection, "public", schema, "USAGE"))
                    .as("PUBLIC USAGE %s", schema).isFalse();
            assertThat(hasSchema(connection, "public", schema, "CREATE"))
                    .as("PUBLIC CREATE %s", schema).isFalse();
        }
    }

    private static void assertGuardFunctionCatalogExecuteBoundaries(Connection connection)
            throws SQLException {
        Map<String, String> guardFunctionGrantRole = Map.of(
                "customer_iam.guard_customer_account()", "customer_iam_role",
                "customer_iam.guard_auth_identity()", "customer_iam_role",
                "customer_iam.guard_identity_profile()", "customer_iam_role",
                "organization_access.guard_organization()", "organization_access_role",
                "organization_access.guard_membership()", "organization_access_role",
                "operations_iam.guard_employee_account()", "operations_iam_role");
        List<String> runtimeRoles = List.of(
                "customer_iam_role", "organization_access_role", "operations_iam_role",
                "subject_number_role");
        for (Map.Entry<String, String> entry : guardFunctionGrantRole.entrySet()) {
            String function = entry.getKey();
            String grantedRole = entry.getValue();
            assertThat(hasFunction(connection, grantedRole, function, "EXECUTE"))
                    .as("%s EXECUTE %s", grantedRole, function).isTrue();
            assertThat(hasFunction(connection, "public", function, "EXECUTE"))
                    .as("PUBLIC must not EXECUTE %s", function).isFalse();
            for (String role : runtimeRoles) {
                if (role.equals(grantedRole)) {
                    continue;
                }
                assertThat(hasFunction(connection, role, function, "EXECUTE"))
                        .as("%s must not EXECUTE %s", role, function).isFalse();
            }
        }
    }

    private static void assertOperationsIamRoleCanForUpdateWithColumnUpdate() throws Exception {
        UUID employeeId = UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "operations_iam_role");
                assertThat(insertEmployee(connection, employeeId, randomSha256(), "s3***lock",
                        "onecall-ops", randomSha256(), "S3 Lock Probe", "OPS")).isEqualTo(1);
                assertThat(queryUuid(connection,
                        "SELECT id FROM operations_iam.employee_account WHERE id = ? FOR UPDATE",
                        employeeId)).isEqualTo(employeeId);
                assertThat(executeUpdate(connection,
                        "UPDATE operations_iam.employee_account SET display_name = ?, "
                                + "revision = revision + 1 WHERE id = ? AND revision = 1",
                        "S3 Lock Probe Updated", employeeId)).isEqualTo(1);
            } finally {
                connection.rollback();
            }
        }
    }

    private static void assertLastEnabledAdminSequentialFailClosedAndRevisionIncrements()
            throws Exception {
        List<UUID> allEmployeeIds = new ArrayList<>();
        assertNoLeftoverEnabledAdministrators();
        try {
            UUID keepAdmin = UUID.randomUUID();
            UUID disableTarget = UUID.randomUUID();
            allEmployeeIds.add(keepAdmin);
            allEmployeeIds.add(disableTarget);
            commitEmployees(List.of(keepAdmin, disableTarget));

            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                try {
                    setLocalRole(connection, "operations_iam_role");
                    runLastEnabledAdminMutation(connection, disableTarget, AdminMutationAction.DISABLE, 1);
                    connection.commit();
                } catch (SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
            try (Connection verify = openConnection()) {
                assertThat(queryString(verify,
                        "SELECT status || '|' || role || '|' || revision "
                                + "FROM operations_iam.employee_account WHERE id = ?", disableTarget))
                        .isEqualTo("DISABLED|ADMIN|2");
                assertThat(queryString(verify,
                        "SELECT status || '|' || role || '|' || revision "
                                + "FROM operations_iam.employee_account WHERE id = ?", keepAdmin))
                        .isEqualTo("ACTIVE|ADMIN|1");
            }

            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                try {
                    setLocalRole(connection, "operations_iam_role");
                    expectLastEnabledAdminFailure(connection, keepAdmin, AdminMutationAction.DISABLE, 1);
                    expectLastEnabledAdminFailure(connection, keepAdmin, AdminMutationAction.DEMOTE, 1);
                    connection.commit();
                } catch (SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
            try (Connection verify = openConnection()) {
                assertThat(queryString(verify,
                        "SELECT status || '|' || role || '|' || revision "
                                + "FROM operations_iam.employee_account WHERE id = ?", keepAdmin))
                        .as("failed last-admin mutations must change nothing")
                        .isEqualTo("ACTIVE|ADMIN|1");
            }
            cleanupCommittedRaceRows(
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of(keepAdmin, disableTarget));
            assertNoLeftoverEnabledAdministrators();

            UUID demoteKeepAdmin = UUID.randomUUID();
            UUID demoteTarget = UUID.randomUUID();
            allEmployeeIds.add(demoteKeepAdmin);
            allEmployeeIds.add(demoteTarget);
            commitEmployees(List.of(demoteKeepAdmin, demoteTarget));

            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                try {
                    setLocalRole(connection, "operations_iam_role");
                    runLastEnabledAdminMutation(connection, demoteTarget, AdminMutationAction.DEMOTE, 1);
                    connection.commit();
                } catch (SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
            try (Connection verify = openConnection()) {
                assertThat(queryString(verify,
                        "SELECT status || '|' || role || '|' || revision "
                                + "FROM operations_iam.employee_account WHERE id = ?", demoteTarget))
                        .isEqualTo("ACTIVE|OPS|2");
                assertThat(queryString(verify,
                        "SELECT status || '|' || role || '|' || revision "
                                + "FROM operations_iam.employee_account WHERE id = ?", demoteKeepAdmin))
                        .isEqualTo("ACTIVE|ADMIN|1");
            }

            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                try {
                    setLocalRole(connection, "operations_iam_role");
                    expectLastEnabledAdminFailure(connection, demoteKeepAdmin, AdminMutationAction.DEMOTE, 1);
                    expectLastEnabledAdminFailure(connection, demoteKeepAdmin, AdminMutationAction.DISABLE, 1);
                    connection.commit();
                } catch (SQLException | RuntimeException exception) {
                    connection.rollback();
                    throw exception;
                }
            }
            try (Connection verify = openConnection()) {
                assertThat(queryString(verify,
                        "SELECT status || '|' || role || '|' || revision "
                                + "FROM operations_iam.employee_account WHERE id = ?", demoteKeepAdmin))
                        .as("failed last-admin demote/disable must change nothing")
                        .isEqualTo("ACTIVE|ADMIN|1");
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM operations_iam.employee_account "
                                + "WHERE role = 'ADMIN' AND status = 'ACTIVE' AND id IN (?, ?)",
                        demoteKeepAdmin, demoteTarget))
                        .isEqualTo(1);
            }
        } finally {
            cleanupCommittedRaceRows(
                    List.of(), List.of(), List.of(), List.of(), List.of(), allEmployeeIds);
        }
    }

    private static void runConcurrentLastEnabledAdminMutationRace() throws Exception {
        UUID winnerTarget = UUID.randomUUID();
        UUID loserTarget = UUID.randomUUID();
        String winnerApplication = "pa2-s3-admin-race-winner";
        String loserApplication = "pa2-s3-admin-race-loser";
        List<UUID> employeeIds = List.of(winnerTarget, loserTarget);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertNoLeftoverEnabledAdministrators();
            commitEmployees(employeeIds);
            try (Connection winner = openConnection(); Connection loser = openConnection()) {
                winner.setAutoCommit(false);
                try (Statement statement = winner.createStatement()) {
                    statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
                }
                setLocalRole(winner, "operations_iam_role");
                runLastEnabledAdminMutation(winner, winnerTarget, AdminMutationAction.DISABLE, 1);

                loser.setAutoCommit(false);
                configureBlockedWriter(loser, "operations_iam_role", loserApplication);
                Future<Void> loserFuture = executor.submit(() -> {
                    try {
                        runLastEnabledAdminMutation(loser, loserTarget, AdminMutationAction.DISABLE, 1);
                        loser.rollback();
                        fail("expected LAST_ENABLED_ADMIN after the winner committed");
                        return null;
                    } catch (SQLException exception) {
                        loser.rollback();
                        assertThat(exception.getSQLState())
                                .as("concurrent last-admin SQLSTATE")
                                .isEqualTo("P0001");
                        assertThat(exception.getMessage())
                                .as("concurrent last-admin stable error")
                                .contains("LAST_ENABLED_ADMIN");
                        return null;
                    }
                });
                awaitBlockedBackend(loserApplication);
                winner.commit();
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);

                try (Connection verify = openConnection()) {
                    assertThat(queryString(verify,
                            "SELECT status || '|' || role || '|' || revision "
                                    + "FROM operations_iam.employee_account WHERE id = ?", winnerTarget))
                            .as("winner disable commits with revision+1")
                            .isEqualTo("DISABLED|ADMIN|2");
                    assertThat(queryString(verify,
                            "SELECT status || '|' || role || '|' || revision "
                                    + "FROM operations_iam.employee_account WHERE id = ?", loserTarget))
                            .as("loser observes the new count and fails closed")
                            .isEqualTo("ACTIVE|ADMIN|1");
                    assertThat(queryLong(verify,
                            "SELECT count(*) FROM operations_iam.employee_account "
                                    + "WHERE role = 'ADMIN' AND status = 'ACTIVE' AND id IN (?, ?)",
                            winnerTarget, loserTarget))
                            .as("final enabled-admin count stays at least one")
                            .isEqualTo(1);
                }
            }
        } finally {
            executor.shutdownNow();
            cleanupCommittedRaceRows(
                    List.of(), List.of(), List.of(), List.of(), List.of(), employeeIds);
        }
    }

    private static void assertNoLeftoverEnabledAdministrators() throws SQLException {
        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM operations_iam.employee_account "
                            + "WHERE role = 'ADMIN' AND status = 'ACTIVE'"))
                    .as("last-enabled-admin scenarios require an empty enabled-admin set")
                    .isZero();
        }
    }

    private static void commitEmployees(List<UUID> employeeIds) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                for (UUID id : employeeIds) {
                    assertThat(insertEmployee(connection, id, randomSha256(), "s3***adm",
                            "onecall-ops", randomSha256(), "S3 Admin " + id, "ADMIN"))
                            .isEqualTo(1);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void runLastEnabledAdminMutation(
            Connection connection, UUID targetId, AdminMutationAction action, int expectedRevision)
            throws SQLException {
        execute(connection, ADMIN_MUTATION_ADVISORY_LOCK_SQL);
        assertThat(queryString(connection,
                "SELECT role || '|' || status || '|' || revision "
                        + "FROM operations_iam.employee_account WHERE id = ? FOR UPDATE",
                targetId))
                .as("target employee FOR UPDATE under operations_iam_role")
                .isEqualTo("ADMIN|ACTIVE|" + expectedRevision);
        long enabledAdmins = queryLong(connection,
                "SELECT count(*) FROM operations_iam.employee_account "
                        + "WHERE role = 'ADMIN' AND status = 'ACTIVE'");
        assertThat(enabledAdmins)
                .as("locked target is an enabled administrator so the count is at least one")
                .isGreaterThanOrEqualTo(1);
        if (enabledAdmins == 1) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DO $s3_last_admin$ "
                        + "BEGIN RAISE EXCEPTION 'LAST_ENABLED_ADMIN' USING ERRCODE = 'P0001'; "
                        + "END $s3_last_admin$");
            }
        }
        int updated;
        if (action == AdminMutationAction.DISABLE) {
            updated = executeUpdate(connection,
                    "UPDATE operations_iam.employee_account "
                            + "SET status = 'DISABLED', revision = revision + 1 "
                            + "WHERE id = ? AND role = 'ADMIN' AND status = 'ACTIVE' AND revision = ?",
                    targetId, expectedRevision);
        } else {
            updated = executeUpdate(connection,
                    "UPDATE operations_iam.employee_account "
                            + "SET role = 'OPS', revision = revision + 1 "
                            + "WHERE id = ? AND role = 'ADMIN' AND status = 'ACTIVE' AND revision = ?",
                    targetId, expectedRevision);
        }
        assertThat(updated)
                .as("conditional last-admin update must affect exactly one row")
                .isEqualTo(1);
    }

    private static void expectLastEnabledAdminFailure(
            Connection connection, UUID targetId, AdminMutationAction action, int expectedRevision)
            throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        try {
            try {
                runLastEnabledAdminMutation(connection, targetId, action, expectedRevision);
                fail("expected LAST_ENABLED_ADMIN but the mutation succeeded");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState())
                        .as("LAST_ENABLED_ADMIN SQLSTATE")
                        .isEqualTo("P0001");
                assertThat(exception.getMessage())
                        .as("LAST_ENABLED_ADMIN stable error marker")
                        .contains("LAST_ENABLED_ADMIN");
            }
        } finally {
            connection.rollback(savepoint);
        }
    }

    private static void expectSqlQueryFailure(
            Connection connection, String expectedSqlState, String sql, Object... parameters)
            throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        try {
            try (PreparedStatement prepared = connection.prepareStatement(sql)) {
                bind(prepared, parameters);
                try (ResultSet resultSet = prepared.executeQuery()) {
                    while (resultSet.next()) {
                        // drain any unexpectedly returned row
                    }
                }
                fail("expected SQL failure " + expectedSqlState + " but the query succeeded");
            }
        } catch (SQLException exception) {
            assertThat(exception.getSQLState())
                    .as("SQLSTATE for expected query failure %s", expectedSqlState)
                    .isEqualTo(expectedSqlState);
        } finally {
            connection.rollback(savepoint);
        }
    }

    private static void assertOwnerAuditMutationIsRejected(UUID objectId, String operation) throws Exception {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, "audit_owner");
            execute(connection,
                    "INSERT INTO audit.audit_fact "
                            + "(id, occurred_at, actor_type, actor_id, action_code, object_type, object_id) "
                            + "VALUES (?, now(), 'SERVICE', ?, 'OWNER_MUTATION_PROBE', 'ORDER', ?)",
                    UUID.randomUUID(), UUID.randomUUID(), objectId);

            if ("UPDATE".equals(operation)) {
                expectFailure(connection, "AUDIT_APPEND_ONLY", () -> executeUpdate(connection,
                        "UPDATE audit.audit_fact SET request_id = 'owner-mutated' WHERE object_id = ?",
                        objectId));
            } else {
                expectFailure(connection, "AUDIT_APPEND_ONLY", () -> executeUpdate(connection,
                        "DELETE FROM audit.audit_fact WHERE object_id = ?", objectId));
            }
        }
        assertThat(countAuditRows(objectId))
                .as("the owner %s attempt is rejected and its probe row is rolled back", operation)
                .isZero();
    }

    private static void assertAuditReasonRequired(String actorType, String actionCode, String caseName)
            throws Exception {
        UUID objectId = UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);
            expectFailure(connection, "AUDIT_REASON_REQUIRED",
                    () -> callAppendFact(connection, objectId, actorType, actionCode, null, null));
        }
        assertThat(countAuditRows(objectId))
                .as("rejected audit append '%s' leaves no row", caseName)
                .isZero();
    }

    private static void runConcurrentCommitRace() throws Exception {
        String scope = "IVR_CONFIRM";
        String key = "s2-race-commit-" + UUID.randomUUID();
        String fingerprint = randomFingerprint();
        UUID resultId = UUID.randomUUID();
        String resultSnapshot = "{\"race\":\"commit\"}";
        String resultSha256 = randomSha256();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Connection winnerConnection = openConnection();
                Connection loserConnection = openConnection()) {
            winnerConnection.setAutoCommit(false);
            setLocalRole(winnerConnection, RUNTIME_ROLE);
            BeginResult winner = callIdempotencyBegin(winnerConnection, scope, key, fingerprint);
            assertThat(winner.outcome()).isEqualTo("NEW");
            assertThat(winner.revision()).isEqualTo(1);
            assertThat(callIdempotencyComplete(
                    winnerConnection, winner.recordId(), 1, "RACE_RESULT",
                    resultId, resultSnapshot, resultSha256))
                    .isEqualTo("COMPLETED");

            loserConnection.setAutoCommit(false);
            configureBlockedLoser(loserConnection, LOSER_COMMIT_APPLICATION);
            Future<BeginResult> loserFuture = executor.submit(
                    () -> callIdempotencyBegin(loserConnection, scope, key, fingerprint));
            awaitBlockedBackend(LOSER_COMMIT_APPLICATION);

            winnerConnection.commit();
            BeginResult loser = awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
            loserConnection.commit();

            assertThat(loser.outcome()).isEqualTo("REPLAY");
            assertThat(loser.recordId()).isEqualTo(winner.recordId());
            assertThat(loser.revision()).isEqualTo(2);
            assertThat(loser.resultType()).isEqualTo("RACE_RESULT");
            assertThat(loser.resultId()).isEqualTo(resultId);
            assertThat(loser.resultSha256()).isEqualTo(resultSha256);
            assertThat(List.of(winner.outcome(), loser.outcome()))
                    .as("exactly one contender observes NEW")
                    .containsExactly("NEW", "REPLAY");

            StoredIdempotency stored = readIdempotency(scope, key);
            assertThat(stored.id()).isEqualTo(winner.recordId());
            assertThat(stored.status()).isEqualTo("COMPLETED");
            assertThat(stored.revision()).isEqualTo(2);
            assertThat(stored.resultSha256()).isEqualTo(resultSha256);
            assertThat(loser.resultSnapshot()).isEqualTo(stored.resultSnapshot());
            assertThat(countIdempotencyRows(scope, key)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void runConcurrentRollbackRace() throws Exception {
        String scope = "IVR_CONFIRM";
        String key = "s2-race-rollback-" + UUID.randomUUID();
        String fingerprint = randomFingerprint();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Connection winnerConnection = openConnection();
                Connection loserConnection = openConnection()) {
            winnerConnection.setAutoCommit(false);
            setLocalRole(winnerConnection, RUNTIME_ROLE);
            BeginResult winner = callIdempotencyBegin(winnerConnection, scope, key, fingerprint);
            assertThat(winner.outcome()).isEqualTo("NEW");
            assertThat(winner.revision()).isEqualTo(1);

            loserConnection.setAutoCommit(false);
            configureBlockedLoser(loserConnection, LOSER_ROLLBACK_APPLICATION);
            Future<BeginResult> loserFuture = executor.submit(
                    () -> callIdempotencyBegin(loserConnection, scope, key, fingerprint));
            awaitBlockedBackend(LOSER_ROLLBACK_APPLICATION);

            winnerConnection.rollback();
            BeginResult loser = awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
            loserConnection.commit();

            assertThat(winner.outcome()).isEqualTo("NEW");
            assertThat(loser.outcome()).isEqualTo("NEW");
            assertThat(loser.revision()).isEqualTo(1);
            StoredIdempotency stored = readIdempotency(scope, key);
            assertThat(stored.id()).isEqualTo(loser.recordId());
            assertThat(stored.status()).isEqualTo("NEW");
            assertThat(stored.revision()).isEqualTo(1);
            assertThat(stored.resultType()).isNull();
            assertThat(countIdempotencyRows(scope, key)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void assertOutboxRejected(
            String domain,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payloadRedacted,
            String payloadCiphertext,
            String payloadSha256,
            String caseName) throws Exception {
        String key = "s2-outbox-invalid-" + UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            setLocalRole(connection, RUNTIME_ROLE);

            expectFailure(connection, "OUTBOX_PAYLOAD_INVALID", () -> callOutboxAppend(
                    connection, domain, key, aggregateType, aggregateId, eventType,
                    payloadRedacted, payloadCiphertext, payloadSha256));
        }
        assertThat(countOutboxRows(domain, key))
                .as("rejected outbox case '%s' must leave no row", caseName)
                .isZero();
    }

    private static void configureBlockedLoser(Connection connection, String applicationName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCAL statement_timeout = 20000");
            statement.execute("SET LOCAL application_name = '" + applicationName + "'");
            statement.execute("SET LOCAL ROLE " + RUNTIME_ROLE);
        }
        assertThat(queryString(connection, "SELECT current_user")).isEqualTo(RUNTIME_ROLE);
        assertThat(queryString(connection, "SELECT current_setting('application_name')"))
                .isEqualTo(applicationName);
    }

    private static void awaitBlockedBackend(String applicationName) throws Exception {
        long deadline = System.nanoTime() + BLOCK_DETECTION_TIMEOUT.toNanos();
        try (Connection monitor = openConnection()) {
            while (System.nanoTime() < deadline) {
                long blocked = queryLong(
                        monitor,
                        "SELECT count(*) FROM pg_stat_activity WHERE application_name = ? "
                                + "AND state = 'active' AND wait_event_type = 'Lock'",
                        applicationName);
                if (blocked > 0) {
                    return;
                }
                Thread.sleep(50L);
            }
        }
        fail("loser backend '" + applicationName + "' did not reach a lock wait within "
                + BLOCK_DETECTION_TIMEOUT.toSeconds() + "s");
    }

    private static <T> T awaitFuture(Future<T> future, long timeoutSeconds) throws Exception {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception causeException) {
                throw causeException;
            }
            throw exception;
        }
    }

    private static BeginResult callIdempotencyBegin(
            Connection connection, String scope, String key, String fingerprint) throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(
                "SELECT record_id, revision, outcome, result_type, result_id, result_snapshot, result_sha256 "
                        + "FROM platform.idempotency_begin(?, ?, ?)")) {
            prepared.setString(1, scope);
            prepared.setString(2, key);
            prepared.setString(3, fingerprint);
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                BeginResult result = new BeginResult(
                        (UUID) resultSet.getObject(1),
                        (Integer) resultSet.getObject(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        (UUID) resultSet.getObject(5),
                        resultSet.getString(6),
                        resultSet.getString(7));
                assertThat(resultSet.next()).as("idempotency_begin returns exactly one row").isFalse();
                return result;
            }
        }
    }

    private static String callIdempotencyComplete(
            Connection connection,
            UUID recordId,
            int expectedRevision,
            String resultType,
            UUID resultId,
            String resultSnapshot,
            String resultSha256) throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(
                "SELECT platform.idempotency_complete(?, ?, ?, ?, CAST(? AS jsonb), ?)")) {
            prepared.setObject(1, recordId);
            prepared.setInt(2, expectedRevision);
            prepared.setString(3, resultType);
            prepared.setObject(4, resultId);
            prepared.setString(5, resultSnapshot);
            prepared.setString(6, resultSha256);
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                String outcome = resultSet.getString(1);
                assertThat(resultSet.next()).isFalse();
                return outcome;
            }
        }
    }

    private static UUID callOutboxAppend(
            Connection connection,
            String eventDomain,
            String dedupeKey,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payloadRedacted,
            String payloadCiphertext,
            String payloadSha256) throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(
                "SELECT platform.outbox_append(?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?)")) {
            prepared.setString(1, eventDomain);
            prepared.setString(2, dedupeKey);
            prepared.setString(3, aggregateType);
            prepared.setObject(4, aggregateId);
            prepared.setString(5, eventType);
            prepared.setString(6, payloadRedacted);
            prepared.setString(7, payloadCiphertext);
            prepared.setString(8, payloadSha256);
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                UUID id = (UUID) resultSet.getObject(1);
                assertThat(resultSet.next()).isFalse();
                return id;
            }
        }
    }

    private static StoredIdempotency readIdempotency(String scope, String key) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement prepared = connection.prepareStatement(
                        "SELECT id, scope, idempotency_key, request_fingerprint, status, result_type, "
                                + "result_id, result_snapshot, result_sha256, created_at, completed_at, revision "
                                + "FROM platform.idempotency_record WHERE scope = ? AND idempotency_key = ?")) {
            prepared.setString(1, scope);
            prepared.setString(2, key);
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).as("exactly one method-local idempotency row").isTrue();
                StoredIdempotency stored = new StoredIdempotency(
                        (UUID) resultSet.getObject(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getString(5),
                        resultSet.getString(6),
                        (UUID) resultSet.getObject(7),
                        resultSet.getString(8),
                        resultSet.getString(9),
                        resultSet.getObject(10, OffsetDateTime.class),
                        resultSet.getObject(11, OffsetDateTime.class),
                        resultSet.getInt(12));
                assertThat(resultSet.next()).as("no second method-local idempotency row").isFalse();
                return stored;
            }
        }
    }

    private static StoredOutbox readOutbox(String eventDomain, String dedupeKey) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement prepared = connection.prepareStatement(
                        "SELECT id, event_domain, dedupe_key, aggregate_type, aggregate_id, event_type, "
                                + "payload_redacted, payload_ciphertext, payload_sha256, status, attempt_count, "
                                + "available_at, published_at, revision "
                                + "FROM platform.outbox_event WHERE event_domain = ? AND dedupe_key = ?")) {
            prepared.setString(1, eventDomain);
            prepared.setString(2, dedupeKey);
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).as("exactly one method-local outbox row").isTrue();
                StoredOutbox stored = new StoredOutbox(
                        (UUID) resultSet.getObject(1),
                        resultSet.getString(2),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        (UUID) resultSet.getObject(5),
                        resultSet.getString(6),
                        resultSet.getString(7),
                        resultSet.getString(8),
                        resultSet.getString(9),
                        resultSet.getString(10),
                        resultSet.getInt(11),
                        resultSet.getObject(12, OffsetDateTime.class),
                        resultSet.getObject(13, OffsetDateTime.class),
                        resultSet.getInt(14));
                assertThat(resultSet.next()).as("no second method-local outbox row").isFalse();
                return stored;
            }
        }
    }

    private static long countIdempotencyRows(String scope, String key) throws SQLException {
        try (Connection connection = openConnection()) {
            return queryLong(
                    connection,
                    "SELECT count(*) FROM platform.idempotency_record WHERE scope = ? AND idempotency_key = ?",
                    scope, key);
        }
    }

    private static long countOutboxRows(String eventDomain, String dedupeKey) throws SQLException {
        try (Connection connection = openConnection()) {
            return queryLong(
                    connection,
                    "SELECT count(*) FROM platform.outbox_event WHERE event_domain = ? AND dedupe_key = ?",
                    eventDomain, dedupeKey);
        }
    }

    private static long countAuditRows(Connection connection, UUID objectId) throws SQLException {
        return queryLong(connection, "SELECT count(*) FROM audit.audit_fact WHERE object_id = ?", objectId);
    }

    private static long countAuditRows(UUID objectId) throws SQLException {
        try (Connection connection = openConnection()) {
            return countAuditRows(connection, objectId);
        }
    }

    private static UUID callAppendFact(
            Connection connection,
            UUID objectId,
            String actorType,
            String actionCode,
            String reasonCode,
            String afterSnapshot) throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(
                "SELECT audit.append_fact(CAST(? AS timestamptz), ?, ?, ?, 'ORDER', ?, ?, NULL, "
                        + "CAST(? AS jsonb), NULL, NULL)")) {
            prepared.setObject(1, OffsetDateTime.now(ZoneOffset.UTC));
            prepared.setString(2, actorType);
            prepared.setObject(3, UUID.randomUUID());
            prepared.setString(4, actionCode);
            prepared.setObject(5, objectId);
            prepared.setString(6, reasonCode);
            prepared.setString(7, afterSnapshot);
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                UUID id = (UUID) resultSet.getObject(1);
                assertThat(resultSet.next()).isFalse();
                return id;
            }
        }
    }

    private static void setSessionRole(Connection connection, String role) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET ROLE " + role);
        }
        assertThat(queryString(connection, "SELECT current_user")).isEqualTo(role);
    }

    private static void execute(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(sql)) {
            bind(prepared, parameters);
            prepared.execute();
        }
    }

    private static int executeUpdate(Connection connection, String sql, Object... parameters)
            throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(sql)) {
            bind(prepared, parameters);
            return prepared.executeUpdate();
        }
    }

    private static void expectSqlState(
            Connection connection, String expectedSqlState, String messageFragment, SqlAction action)
            throws SQLException {
        try {
            action.run();
            fail("expected SQL failure " + expectedSqlState + " but the call succeeded");
        } catch (SQLException exception) {
            assertThat(exception.getSQLState())
                    .as("SQLSTATE for %s", expectedSqlState)
                    .isEqualTo(expectedSqlState);
            if (messageFragment != null) {
                assertThat(exception.getMessage())
                        .as("message for %s", expectedSqlState)
                        .contains(messageFragment);
            }
        }
    }

    private static void assertAlertIndexUsed(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL enable_seqscan = off");
            }
            String plan = queryString(
                    connection,
                    "EXPLAIN (FORMAT JSON) SELECT id FROM platform.idempotency_record "
                            + "WHERE status = 'NEW' ORDER BY created_at");
            assertThat(plan)
                    .as("the committed-NEW alert query must use ix_idempotency_new_alert")
                    .contains("ix_idempotency_new_alert");
        } finally {
            connection.rollback();
            connection.setAutoCommit(true);
        }
    }

    private static void setLocalRole(Connection connection, String role) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCAL ROLE " + role);
        }
        assertThat(queryString(connection, "SELECT current_user")).isEqualTo(role);
    }

    private static void expectFailure(Connection connection, String expectedCode, SqlAction action)
            throws SQLException {
        try {
            try {
                action.run();
                fail("expected SQL failure " + expectedCode + " but the call succeeded");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState())
                        .as("SQLSTATE for %s", expectedCode)
                        .isEqualTo("P0001");
                assertThat(exception.getMessage())
                        .as("stable error code for %s", expectedCode)
                        .contains(expectedCode);
            }
        } finally {
            connection.rollback();
        }
    }

    private static void expectSqlFailure(
            Connection connection,
            String expectedSqlState,
            String messageFragment,
            String sql,
            Object... parameters) throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        try {
            try (PreparedStatement prepared = connection.prepareStatement(sql)) {
                bind(prepared, parameters);
                prepared.executeUpdate();
            }
            fail("expected SQL failure " + expectedSqlState + " but the statement succeeded");
        } catch (SQLException exception) {
            assertThat(exception.getSQLState())
                    .as("SQLSTATE for expected failure %s", expectedSqlState)
                    .isEqualTo(expectedSqlState);
            if (messageFragment != null) {
                assertThat(exception.getMessage())
                        .as("exact error/constraint marker for %s", messageFragment)
                        .contains(messageFragment);
            }
        } finally {
            connection.rollback(savepoint);
        }
    }

    private static void switchLocalRole(Connection connection, String role) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("RESET ROLE");
        }
        setLocalRole(connection, role);
    }

    private static String authIdentityInsertSql() {
        return "INSERT INTO customer_iam.auth_identity (id, account_id, provider, provider_scope, "
                + "provider_subject_ciphertext, provider_subject_hmac, verified_at, key_version) "
                + "VALUES (?, ?, ?, ?, ?, ?, now(), 1)";
    }

    private static String employeeInsertSql() {
        return "INSERT INTO operations_iam.employee_account "
                + "(id, login_name_hmac, login_name_masked, auth_issuer, auth_subject_hmac, "
                + "display_name, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    private static String membershipInsertSql() {
        return "INSERT INTO organization_access.organization_membership "
                + "(id, organization_id, account_id, role) VALUES (?, ?, ?, ?)";
    }

    private static int insertCustomerAccount(Connection connection, UUID id, String displayName)
            throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO customer_iam.customer_account (id, display_name) VALUES (?, ?)",
                id, displayName);
    }

    private static int insertAuthIdentity(
            Connection connection,
            UUID id,
            UUID accountId,
            String provider,
            String providerScope,
            String ciphertext,
            String hmac) throws SQLException {
        return executeUpdate(connection, authIdentityInsertSql(),
                id, accountId, provider, providerScope, ciphertext, hmac);
    }

    private static int insertIdentityProfile(Connection connection, UUID id, UUID accountId)
            throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO customer_iam.identity_profile (id, account_id) VALUES (?, ?)",
                id, accountId);
    }

    private static int insertOrganization(Connection connection, UUID id, String displayName)
            throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO organization_access.organization (id, display_name) VALUES (?, ?)",
                id, displayName);
    }

    private static int insertMembership(
            Connection connection, UUID id, UUID organizationId, UUID accountId, String role)
            throws SQLException {
        return executeUpdate(connection, membershipInsertSql(), id, organizationId, accountId, role);
    }

    private static int insertEmployee(
            Connection connection,
            UUID id,
            String loginHmac,
            String loginMasked,
            String issuer,
            String subjectHmac,
            String displayName,
            String role) throws SQLException {
        return executeUpdate(connection, employeeInsertSql(),
                id, loginHmac, loginMasked, issuer, subjectHmac, displayName, role);
    }

    private static UUID queryUuid(Connection connection, String sql, Object... parameters)
            throws SQLException {
        return UUID.fromString(queryString(connection, sql, parameters));
    }

    private static void assertSixTableDefaultsAndConditionalRevisions() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "customer_iam_role");
                assertThat(insertCustomerAccount(connection, accountId, "Defaults Customer")).isEqualTo(1);
                assertThat(insertAuthIdentity(connection, identityId, accountId, "PHONE",
                        "defaults-scope", "v1.defaults.ciphertext", randomSha256())).isEqualTo(1);
                assertThat(insertIdentityProfile(connection, profileId, accountId)).isEqualTo(1);

                assertThat(queryString(connection,
                        "SELECT status FROM customer_iam.customer_account WHERE id = ?", accountId))
                        .isEqualTo("ACTIVE");
                assertThat(queryLong(connection,
                        "SELECT revision FROM customer_iam.customer_account WHERE id = ?", accountId))
                        .isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT created_at::text FROM customer_iam.customer_account WHERE id = ?", accountId))
                        .isNotNull();
                assertThat(queryString(connection,
                        "SELECT updated_at::text FROM customer_iam.customer_account WHERE id = ?", accountId))
                        .isNotNull();
                assertThat(queryString(connection,
                        "SELECT status FROM customer_iam.auth_identity WHERE id = ?", identityId))
                        .isEqualTo("ACTIVE");
                assertThat(queryLong(connection,
                        "SELECT revision FROM customer_iam.auth_identity WHERE id = ?", identityId))
                        .isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT key_version FROM customer_iam.auth_identity WHERE id = ?", identityId))
                        .isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT real_name_status FROM customer_iam.identity_profile WHERE id = ?", profileId))
                        .isEqualTo("UNVERIFIED");
                assertThat(queryLong(connection,
                        "SELECT revision FROM customer_iam.identity_profile WHERE id = ?", profileId))
                        .isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM customer_iam.identity_profile WHERE id = ? "
                                + "AND legal_name_ciphertext IS NULL AND id_number_ciphertext IS NULL "
                                + "AND id_number_hmac IS NULL AND verification_ref_ciphertext IS NULL "
                                + "AND verified_at IS NULL", profileId)).isEqualTo(1);

                assertThat(executeUpdate(connection,
                        "UPDATE customer_iam.customer_account SET display_name = ?, status = 'DISABLED', "
                                + "revision = revision + 1, updated_at = now() WHERE id = ? AND revision = 1",
                        "Updated Customer", accountId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE customer_iam.auth_identity SET status = 'DISABLED', verified_at = now(), "
                                + "key_version = key_version + 1, revision = revision + 1 "
                                + "WHERE id = ? AND revision = 1", identityId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE customer_iam.identity_profile SET real_name_status = 'VERIFIED', "
                                + "legal_name_ciphertext = ?, id_number_ciphertext = ?, id_number_hmac = ?, "
                                + "verification_ref_ciphertext = ?, verified_at = now(), "
                                + "revision = revision + 1 WHERE id = ? AND revision = 1",
                        "v1.profile.legal", "v1.profile.id", randomSha256(), "v1.profile.ref", profileId))
                        .isEqualTo(1);

                assertThat(queryString(connection,
                        "SELECT display_name FROM customer_iam.customer_account WHERE id = ?", accountId))
                        .isEqualTo("Updated Customer");
                assertThat(queryString(connection,
                        "SELECT status FROM customer_iam.customer_account WHERE id = ?", accountId))
                        .isEqualTo("DISABLED");
                assertThat(queryLong(connection,
                        "SELECT revision FROM customer_iam.customer_account WHERE id = ?", accountId))
                        .isEqualTo(2);
                assertThat(queryLong(connection,
                        "SELECT revision FROM customer_iam.auth_identity WHERE id = ?", identityId))
                        .isEqualTo(2);
                assertThat(queryLong(connection,
                        "SELECT key_version FROM customer_iam.auth_identity WHERE id = ?", identityId))
                        .isEqualTo(2);
                assertThat(queryString(connection,
                        "SELECT real_name_status FROM customer_iam.identity_profile WHERE id = ?", profileId))
                        .isEqualTo("VERIFIED");
                assertThat(queryLong(connection,
                        "SELECT revision FROM customer_iam.identity_profile WHERE id = ?", profileId))
                        .isEqualTo(2);

                assertThat(executeUpdate(connection,
                        "UPDATE customer_iam.customer_account SET display_name = 'Stale', status = 'ACTIVE', "
                                + "revision = revision + 1, updated_at = now() WHERE id = ? AND revision = 1",
                        accountId)).isZero();
                assertThat(executeUpdate(connection,
                        "UPDATE customer_iam.auth_identity SET status = 'ACTIVE', verified_at = now(), "
                                + "key_version = key_version + 1, revision = revision + 1 "
                                + "WHERE id = ? AND revision = 1", identityId)).isZero();
                assertThat(executeUpdate(connection,
                        "UPDATE customer_iam.identity_profile SET real_name_status = 'UNVERIFIED', "
                                + "legal_name_ciphertext = NULL, id_number_ciphertext = NULL, "
                                + "id_number_hmac = NULL, verification_ref_ciphertext = NULL, "
                                + "verified_at = NULL, revision = revision + 1 "
                                + "WHERE id = ? AND revision = 1", profileId)).isZero();

                assertThat(queryString(connection,
                        "SELECT display_name FROM customer_iam.customer_account WHERE id = ?", accountId))
                        .isEqualTo("Updated Customer");
                assertThat(queryLong(connection,
                        "SELECT revision FROM customer_iam.customer_account WHERE id = ?", accountId))
                        .isEqualTo(2);
                assertThat(queryLong(connection,
                        "SELECT key_version FROM customer_iam.auth_identity WHERE id = ?", identityId))
                        .isEqualTo(2);
                assertThat(queryString(connection,
                        "SELECT real_name_status FROM customer_iam.identity_profile WHERE id = ?", profileId))
                        .isEqualTo("VERIFIED");
                assertThat(queryLong(connection,
                        "SELECT revision FROM customer_iam.identity_profile WHERE id = ?", profileId))
                        .isEqualTo(2);

                switchLocalRole(connection, "organization_access_role");
                assertThat(insertOrganization(connection, organizationId, "Defaults Organization"))
                        .isEqualTo(1);
                assertThat(insertMembership(connection, membershipId, organizationId, accountId, "MEMBER"))
                        .isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT status FROM organization_access.organization WHERE id = ?", organizationId))
                        .isEqualTo("ACTIVE");
                assertThat(queryLong(connection,
                        "SELECT revision FROM organization_access.organization WHERE id = ?",
                        organizationId)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT created_at::text FROM organization_access.organization WHERE id = ?",
                        organizationId)).isNotNull();
                assertThat(queryString(connection,
                        "SELECT updated_at::text FROM organization_access.organization WHERE id = ?",
                        organizationId)).isNotNull();
                assertThat(queryString(connection,
                        "SELECT status FROM organization_access.organization_membership WHERE id = ?",
                        membershipId)).isEqualTo("ACTIVE");
                assertThat(queryLong(connection,
                        "SELECT revision FROM organization_access.organization_membership WHERE id = ?",
                        membershipId)).isEqualTo(1);

                assertThat(executeUpdate(connection,
                        "UPDATE organization_access.organization SET display_name = ?, status = 'SUSPENDED', "
                                + "revision = revision + 1, updated_at = now() "
                                + "WHERE id = ? AND revision = 1", "Updated Organization", organizationId))
                        .isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE organization_access.organization_membership SET role = 'ADMINISTRATOR', "
                                + "department_label = 'Operations', revision = revision + 1 "
                                + "WHERE id = ? AND revision = 1", membershipId)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT status FROM organization_access.organization WHERE id = ?", organizationId))
                        .isEqualTo("SUSPENDED");
                assertThat(queryLong(connection,
                        "SELECT revision FROM organization_access.organization WHERE id = ?",
                        organizationId)).isEqualTo(2);
                assertThat(queryString(connection,
                        "SELECT role FROM organization_access.organization_membership WHERE id = ?",
                        membershipId)).isEqualTo("ADMINISTRATOR");
                assertThat(queryLong(connection,
                        "SELECT revision FROM organization_access.organization_membership WHERE id = ?",
                        membershipId)).isEqualTo(2);

                assertThat(executeUpdate(connection,
                        "UPDATE organization_access.organization SET display_name = 'Stale', "
                                + "status = 'ACTIVE', revision = revision + 1, updated_at = now() "
                                + "WHERE id = ? AND revision = 1", organizationId)).isZero();
                assertThat(executeUpdate(connection,
                        "UPDATE organization_access.organization_membership SET role = 'AUDITOR', "
                                + "department_label = 'Stale', revision = revision + 1 "
                                + "WHERE id = ? AND revision = 1", membershipId)).isZero();
                assertThat(queryString(connection,
                        "SELECT status FROM organization_access.organization WHERE id = ?", organizationId))
                        .isEqualTo("SUSPENDED");
                assertThat(queryLong(connection,
                        "SELECT revision FROM organization_access.organization WHERE id = ?",
                        organizationId)).isEqualTo(2);
                assertThat(queryString(connection,
                        "SELECT role FROM organization_access.organization_membership WHERE id = ?",
                        membershipId)).isEqualTo("ADMINISTRATOR");
                assertThat(queryLong(connection,
                        "SELECT revision FROM organization_access.organization_membership WHERE id = ?",
                        membershipId)).isEqualTo(2);

                switchLocalRole(connection, "operations_iam_role");
                assertThat(insertEmployee(connection, employeeId, randomSha256(), "op***1",
                        "defaults-issuer", randomSha256(), "Defaults Employee", "OPS")).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT status FROM operations_iam.employee_account WHERE id = ?", employeeId))
                        .isEqualTo("ACTIVE");
                assertThat(queryLong(connection,
                        "SELECT revision FROM operations_iam.employee_account WHERE id = ?", employeeId))
                        .isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE operations_iam.employee_account SET display_name = ?, role = 'ADMIN', "
                                + "revision = revision + 1 WHERE id = ? AND revision = 1",
                        "Updated Employee", employeeId)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT display_name FROM operations_iam.employee_account WHERE id = ?", employeeId))
                        .isEqualTo("Updated Employee");
                assertThat(queryLong(connection,
                        "SELECT revision FROM operations_iam.employee_account WHERE id = ?", employeeId))
                        .isEqualTo(2);
                assertThat(executeUpdate(connection,
                        "UPDATE operations_iam.employee_account SET display_name = 'Stale', role = 'OPS', "
                                + "revision = revision + 1 WHERE id = ? AND revision = 1", employeeId))
                        .isZero();
                assertThat(queryString(connection,
                        "SELECT display_name FROM operations_iam.employee_account WHERE id = ?", employeeId))
                        .isEqualTo("Updated Employee");
                assertThat(queryLong(connection,
                        "SELECT revision FROM operations_iam.employee_account WHERE id = ?", employeeId))
                        .isEqualTo(2);
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", accountId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.auth_identity WHERE id = ?", identityId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.identity_profile WHERE id = ?", profileId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?", organizationId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization_membership WHERE id = ?",
                    membershipId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM operations_iam.employee_account WHERE id = ?", employeeId)).isZero();
        }
    }

    private record CheckProbe(String constraint, String sql, Object[] parameters) {
    }

    private static CheckProbe checkProbe(String constraint, String sql, Object... parameters) {
        return new CheckProbe(constraint, sql, parameters);
    }

    private static void assertEveryV002NamedCheckRejectsAViolation() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        OffsetDateTime verifiedAt = OffsetDateTime.now(ZoneOffset.UTC);

        String authProbeSql = "INSERT INTO customer_iam.auth_identity (id, account_id, provider, "
                + "provider_scope, provider_subject_ciphertext, provider_subject_hmac, status, "
                + "verified_at, key_version, revision) VALUES (?, ?, ?, ?, ?, ?, ?, now(), ?, ?)";
        String profileProbeSql = "INSERT INTO customer_iam.identity_profile (id, account_id, "
                + "real_name_status, legal_name_ciphertext, id_number_ciphertext, id_number_hmac, "
                + "verification_ref_ciphertext, verified_at, revision) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String membershipProbeSql = "INSERT INTO organization_access.organization_membership "
                + "(id, organization_id, account_id, role, department_label, status, revision) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String employeeProbeSql = "INSERT INTO operations_iam.employee_account (id, login_name_hmac, "
                + "login_name_masked, auth_issuer, auth_subject_hmac, display_name, role, status, revision) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        List<CheckProbe> customerProbes = List.of(
                checkProbe("ck_customer_account_display_name",
                        "INSERT INTO customer_iam.customer_account (id, display_name) VALUES (?, '')",
                        UUID.randomUUID()),
                checkProbe("ck_customer_account_status",
                        "INSERT INTO customer_iam.customer_account (id, display_name, status) "
                                + "VALUES (?, 'Probe', 'BAD')",
                        UUID.randomUUID()),
                checkProbe("ck_customer_account_revision",
                        "INSERT INTO customer_iam.customer_account (id, display_name, revision) "
                                + "VALUES (?, 'Probe', 0)",
                        UUID.randomUUID()),
                checkProbe("ck_customer_account_time_order",
                        "INSERT INTO customer_iam.customer_account "
                                + "(id, display_name, created_at, updated_at) "
                                + "VALUES (?, 'Probe', now(), now() - interval '1 second')",
                        UUID.randomUUID()),
                checkProbe("ck_auth_identity_provider", authProbeSql,
                        UUID.randomUUID(), accountId, "EMAIL", "probe-" + UUID.randomUUID(),
                        "v1.probe.ciphertext", randomSha256(), "ACTIVE", 1, 1),
                checkProbe("ck_auth_identity_scope", authProbeSql,
                        UUID.randomUUID(), accountId, "PHONE", "",
                        "v1.probe.ciphertext", randomSha256(), "ACTIVE", 1, 1),
                checkProbe("ck_auth_identity_ciphertext", authProbeSql,
                        UUID.randomUUID(), accountId, "PHONE", "probe-" + UUID.randomUUID(),
                        "plain", randomSha256(), "ACTIVE", 1, 1),
                checkProbe("ck_auth_identity_hmac", authProbeSql,
                        UUID.randomUUID(), accountId, "PHONE", "probe-" + UUID.randomUUID(),
                        "v1.probe.ciphertext", "A".repeat(64), "ACTIVE", 1, 1),
                checkProbe("ck_auth_identity_status", authProbeSql,
                        UUID.randomUUID(), accountId, "PHONE", "probe-" + UUID.randomUUID(),
                        "v1.probe.ciphertext", randomSha256(), "BAD", 1, 1),
                checkProbe("ck_auth_identity_key_version", authProbeSql,
                        UUID.randomUUID(), accountId, "PHONE", "probe-" + UUID.randomUUID(),
                        "v1.probe.ciphertext", randomSha256(), "ACTIVE", 0, 1),
                checkProbe("ck_auth_identity_revision", authProbeSql,
                        UUID.randomUUID(), accountId, "PHONE", "probe-" + UUID.randomUUID(),
                        "v1.probe.ciphertext", randomSha256(), "ACTIVE", 1, 0),
                checkProbe("ck_identity_profile_status", profileProbeSql,
                        UUID.randomUUID(), accountId, "BAD", null, null, null, null, null, 1),
                checkProbe("ck_identity_profile_legal_ciphertext", profileProbeSql,
                        UUID.randomUUID(), accountId, "VERIFIED", "plain", "v1.probe.id",
                        randomSha256(), "v1.probe.ref", verifiedAt, 1),
                checkProbe("ck_identity_profile_id_ciphertext", profileProbeSql,
                        UUID.randomUUID(), accountId, "VERIFIED", "v1.probe.legal", "plain",
                        randomSha256(), "v1.probe.ref", verifiedAt, 1),
                checkProbe("ck_identity_profile_id_hmac", profileProbeSql,
                        UUID.randomUUID(), accountId, "VERIFIED", "v1.probe.legal", "v1.probe.id",
                        "A".repeat(64), "v1.probe.ref", verifiedAt, 1),
                checkProbe("ck_identity_profile_verification_ref", profileProbeSql,
                        UUID.randomUUID(), accountId, "VERIFIED", "v1.probe.legal", "v1.probe.id",
                        randomSha256(), "plain", verifiedAt, 1),
                checkProbe("ck_identity_profile_state_shape", profileProbeSql,
                        UUID.randomUUID(), accountId, "UNVERIFIED", "v1.probe.legal",
                        null, null, null, null, 1),
                checkProbe("ck_identity_profile_revision", profileProbeSql,
                        UUID.randomUUID(), accountId, "VERIFIED", "v1.probe.legal", "v1.probe.id",
                        randomSha256(), "v1.probe.ref", verifiedAt, 0));

        List<CheckProbe> organizationProbes = List.of(
                checkProbe("ck_organization_display_name",
                        "INSERT INTO organization_access.organization (id, display_name) VALUES (?, '')",
                        UUID.randomUUID()),
                checkProbe("ck_organization_status",
                        "INSERT INTO organization_access.organization (id, display_name, status) "
                                + "VALUES (?, 'Probe', 'BAD')",
                        UUID.randomUUID()),
                checkProbe("ck_organization_revision",
                        "INSERT INTO organization_access.organization (id, display_name, revision) "
                                + "VALUES (?, 'Probe', 0)",
                        UUID.randomUUID()),
                checkProbe("ck_organization_time_order",
                        "INSERT INTO organization_access.organization "
                                + "(id, display_name, created_at, updated_at) "
                                + "VALUES (?, 'Probe', now(), now() - interval '1 second')",
                        UUID.randomUUID()),
                checkProbe("ck_membership_role", membershipProbeSql,
                        UUID.randomUUID(), organizationId, accountId, "BAD", null, "ACTIVE", 1),
                checkProbe("ck_membership_department", membershipProbeSql,
                        UUID.randomUUID(), organizationId, accountId, "MEMBER", "", "ACTIVE", 1),
                checkProbe("ck_membership_status", membershipProbeSql,
                        UUID.randomUUID(), organizationId, accountId, "MEMBER", null, "BAD", 1),
                checkProbe("ck_membership_revision", membershipProbeSql,
                        UUID.randomUUID(), organizationId, accountId, "MEMBER", null, "ACTIVE", 0));

        List<CheckProbe> employeeProbes = List.of(
                checkProbe("ck_employee_login_hmac", employeeProbeSql,
                        UUID.randomUUID(), "A".repeat(64), "op***probe", "probe-issuer-" + UUID.randomUUID(),
                        randomSha256(), "Probe", "OPS", "ACTIVE", 1),
                checkProbe("ck_employee_login_masked", employeeProbeSql,
                        UUID.randomUUID(), randomSha256(), "", "probe-issuer-" + UUID.randomUUID(),
                        randomSha256(), "Probe", "OPS", "ACTIVE", 1),
                checkProbe("ck_employee_issuer", employeeProbeSql,
                        UUID.randomUUID(), randomSha256(), "op***probe", "", randomSha256(),
                        "Probe", "OPS", "ACTIVE", 1),
                checkProbe("ck_employee_subject_hmac", employeeProbeSql,
                        UUID.randomUUID(), randomSha256(), "op***probe",
                        "probe-issuer-" + UUID.randomUUID(), "A".repeat(64), "Probe", "OPS", "ACTIVE", 1),
                checkProbe("ck_employee_display_name", employeeProbeSql,
                        UUID.randomUUID(), randomSha256(), "op***probe",
                        "probe-issuer-" + UUID.randomUUID(), randomSha256(), "", "OPS", "ACTIVE", 1),
                checkProbe("ck_employee_role", employeeProbeSql,
                        UUID.randomUUID(), randomSha256(), "op***probe",
                        "probe-issuer-" + UUID.randomUUID(), randomSha256(), "Probe", "BAD", "ACTIVE", 1),
                checkProbe("ck_employee_status", employeeProbeSql,
                        UUID.randomUUID(), randomSha256(), "op***probe",
                        "probe-issuer-" + UUID.randomUUID(), randomSha256(), "Probe", "OPS", "BAD", 1),
                checkProbe("ck_employee_revision", employeeProbeSql,
                        UUID.randomUUID(), randomSha256(), "op***probe",
                        "probe-issuer-" + UUID.randomUUID(), randomSha256(), "Probe", "OPS", "ACTIVE", 0));

        List<String> probedConstraints = new ArrayList<>();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "customer_iam_role");
                assertThat(insertCustomerAccount(connection, accountId, "Check Probe Account")).isEqualTo(1);
                for (CheckProbe probe : customerProbes) {
                    if ("ck_identity_profile_status".equals(probe.constraint())) {
                        expectProfileStatusViolationWithIsolatedConstraint(connection, probe);
                    } else {
                        expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                                probe.parameters());
                    }
                    probedConstraints.add(probe.constraint());
                }
                switchLocalRole(connection, "organization_access_role");
                assertThat(insertOrganization(connection, organizationId, "Check Probe Organization"))
                        .isEqualTo(1);
                for (CheckProbe probe : organizationProbes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(), probe.parameters());
                    probedConstraints.add(probe.constraint());
                }
                switchLocalRole(connection, "operations_iam_role");
                for (CheckProbe probe : employeeProbes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(), probe.parameters());
                    probedConstraints.add(probe.constraint());
                }
            } finally {
                connection.rollback();
            }
        }

        List<String> expectedConstraints = new ArrayList<>();
        V002_CHECK_DEF_MAP.values().forEach(definitions -> expectedConstraints.addAll(definitions.keySet()));
        assertThat(probedConstraints)
                .as("every frozen V002 named check has exactly one violating probe")
                .hasSize(34)
                .containsExactlyInAnyOrderElementsOf(expectedConstraints);

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", accountId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?", organizationId))
                    .isZero();
        }
    }

    /**
     * {@code ck_identity_profile_status} is logically subsumed by
     * {@code ck_identity_profile_state_shape}, so PostgreSQL reports only the latter for any invalid
     * status value. This bounded helper isolates the frozen status check by dropping the
     * state-shape constraint inside a savepoint, inserting the violating row (exact 23514 and exact
     * constraint name), then rolling back to the savepoint so the catalog is restored exactly.
     */
    private static void expectProfileStatusViolationWithIsolatedConstraint(
            Connection connection, CheckProbe probe) throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        try {
            switchLocalRole(connection, "customer_iam_owner");
            execute(connection,
                    "ALTER TABLE customer_iam.identity_profile DROP CONSTRAINT ck_identity_profile_state_shape");
            try (PreparedStatement prepared = connection.prepareStatement(probe.sql())) {
                bind(prepared, probe.parameters());
                prepared.executeUpdate();
            }
            fail("expected SQL failure 23514 for ck_identity_profile_status but the statement succeeded");
        } catch (SQLException exception) {
            assertThat(exception.getSQLState())
                    .as("SQLSTATE for ck_identity_profile_status").isEqualTo("23514");
            assertThat(exception.getMessage())
                    .as("exact constraint marker for ck_identity_profile_status")
                    .contains("ck_identity_profile_status");
        } finally {
            connection.rollback(savepoint);
        }
        assertThat(queryLong(connection,
                "SELECT count(*) FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'customer_iam' AND r.relname = 'identity_profile' "
                        + "AND c.conname IN ('ck_identity_profile_status','ck_identity_profile_state_shape')"))
                .as("the transaction-local state-shape isolation is fully rolled back")
                .isEqualTo(2);
        switchLocalRole(connection, "customer_iam_role");
    }

    /**
     * {@code ck_catalog_version_offer_type} is logically subsumed by
     * {@code ck_catalog_version_billing_matrix}: every value outside the offer-type allow-list also
     * fails the billing-matrix predicate, so PostgreSQL reports only the latter for any invalid
     * offer type. This bounded helper isolates the frozen offer-type check by dropping the
     * billing-matrix constraint inside a savepoint as {@code catalog_order_owner}, inserting the
     * violating row (exact 23514 and exact constraint name), then rolling back to the savepoint so
     * the dropped constraint and the failed row are both fully restored.
     */
    private static void expectCatalogVersionOfferTypeViolationWithIsolatedConstraint(
            Connection connection, CheckProbe probe) throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        try {
            switchLocalRole(connection, "catalog_order_owner");
            execute(connection,
                    "ALTER TABLE catalog_order.catalog_version "
                            + "DROP CONSTRAINT ck_catalog_version_billing_matrix");
            try (PreparedStatement prepared = connection.prepareStatement(probe.sql())) {
                bind(prepared, probe.parameters());
                prepared.executeUpdate();
            }
            fail("expected SQL failure 23514 for ck_catalog_version_offer_type "
                    + "but the statement succeeded");
        } catch (SQLException exception) {
            assertThat(exception.getSQLState())
                    .as("SQLSTATE for ck_catalog_version_offer_type").isEqualTo("23514");
            assertThat(exception.getMessage())
                    .as("exact constraint marker for ck_catalog_version_offer_type")
                    .contains("ck_catalog_version_offer_type");
        } finally {
            connection.rollback(savepoint);
        }
        assertThat(queryLong(connection,
                "SELECT count(*) FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND r.relname = 'catalog_version' "
                        + "AND c.conname IN ('ck_catalog_version_offer_type',"
                        + "'ck_catalog_version_billing_matrix')"))
                .as("the transaction-local billing-matrix isolation is fully rolled back")
                .isEqualTo(2);
        switchLocalRole(connection, "catalog_order_role");
    }

    /**
     * {@code ck_call_leg_type} is logically subsumed by {@code ck_call_leg_direction}: any
     * {@code leg_type} outside CALLER/CALLEE also fails the direction pairing predicate, so
     * PostgreSQL reports only the latter for an illegal type value. This bounded helper isolates
     * the frozen type check by dropping the direction constraint inside a savepoint as
     * {@code ivr_owner}, inserting the violating row (exact 23514 and exact constraint name), then
     * rolling back to the savepoint so the dropped constraint and the failed row are both fully
     * restored.
     */
    private static void expectCallLegTypeViolationWithIsolatedConstraint(
            Connection connection, CheckProbe probe) throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        try {
            switchLocalRole(connection, "ivr_owner");
            execute(connection,
                    "ALTER TABLE ivr.call_leg DROP CONSTRAINT ck_call_leg_direction");
            try (PreparedStatement prepared = connection.prepareStatement(probe.sql())) {
                bind(prepared, probe.parameters());
                prepared.executeUpdate();
            }
            fail("expected SQL failure 23514 for ck_call_leg_type but the statement succeeded");
        } catch (SQLException exception) {
            assertThat(exception.getSQLState())
                    .as("SQLSTATE for ck_call_leg_type").isEqualTo("23514");
            assertThat(exception.getMessage())
                    .as("exact constraint marker for ck_call_leg_type")
                    .contains("ck_call_leg_type");
        } finally {
            connection.rollback(savepoint);
        }
        assertThat(queryLong(connection,
                "SELECT count(*) FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND r.relname = 'call_leg' "
                        + "AND c.conname IN ('ck_call_leg_type','ck_call_leg_direction')"))
                .as("the transaction-local direction isolation is fully rolled back")
                .isEqualTo(2);
        switchLocalRole(connection, "ivr_role");
    }

    /**
     * The partial unique {@code uq_ivr_decision_source} on {@code (call_id, source_event_domain,
     * source_event_id) WHERE source_event_id IS NOT NULL} cannot fire while unisolated:
     * {@code ck_ivr_decision_source} binds the source tuple to the purpose and
     * {@code uq_ivr_decision_call_purpose} preempts same-purpose duplicates on the shared call.
     * This bounded helper isolates it by dropping only those two preempting frozen objects
     * inside a savepoint as {@code ivr_owner}, inserting two legal rows with distinct purposes
     * but the same non-null source tuple (exact {@code 23505} + {@code uq_ivr_decision_source}),
     * then rolling back to the savepoint so the dropped check and unique index are fully
     * restored.
     */
    private static void expectDecisionSourceUniqueViolationWithIsolatedConstraint(
            Connection connection, UUID sessionId, String callId, String fingerprint, String sha)
            throws SQLException {
        String sourceEventId = "evt-source-unique-" + UUID.randomUUID();
        Savepoint savepoint = connection.setSavepoint();
        try {
            switchLocalRole(connection, "ivr_owner");
            execute(connection,
                    "ALTER TABLE ivr.ivr_decision DROP CONSTRAINT ck_ivr_decision_source");
            execute(connection,
                    "ALTER TABLE ivr.ivr_decision DROP CONSTRAINT uq_ivr_decision_call_purpose");
            switchLocalRole(connection, "ivr_role");
            assertThat(executeUpdate(connection,
                    "INSERT INTO ivr.ivr_decision "
                            + "(id, session_id, call_id, purpose, source_event_domain, "
                            + "source_event_id, request_fingerprint, action, reason_code, "
                            + "response_snapshot, response_payload_ciphertext, "
                            + "response_sha256, key_version) "
                            + "VALUES (?, ?, ?, 'INBOUND_RESOLVE', 'IVR_CONFIRM', ?, "
                            + "?, 'GUIDE_ACTIVATION', 'ACTIVATION_REQUIRED', '{}'::jsonb, "
                            + "?, ?, 1)",
                    UUID.randomUUID(), sessionId, callId, sourceEventId, fingerprint,
                    v005Ciphertext(), sha))
                    .as("first legal distinct-purpose row with the shared source tuple")
                    .isEqualTo(1);
            expectSqlFailure(connection, "23505", "uq_ivr_decision_source",
                    "INSERT INTO ivr.ivr_decision "
                            + "(id, session_id, call_id, purpose, source_event_domain, "
                            + "source_event_id, request_fingerprint, action, reason_code, "
                            + "response_snapshot, response_payload_ciphertext, "
                            + "response_sha256, key_version) "
                            + "VALUES (?, ?, ?, 'ACTIVATION_NEXT_ACTION', 'IVR_CONFIRM', ?, "
                            + "?, 'GUIDE_ACTIVATION', 'ACTIVATION_REQUIRED', '{}'::jsonb, "
                            + "?, ?, 1)",
                    UUID.randomUUID(), sessionId, callId, sourceEventId, fingerprint,
                    v005Ciphertext(), sha);
        } finally {
            connection.rollback(savepoint);
        }
        assertThat(queryLong(connection,
                "SELECT count(*) FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND r.relname = 'ivr_decision' "
                        + "AND c.conname = 'ck_ivr_decision_source'"))
                .as("the transaction-local ck_ivr_decision_source drop is fully rolled back")
                .isEqualTo(1);
        assertThat(toRegclass(connection, "ivr.uq_ivr_decision_call_purpose"))
                .as("the transaction-local uq_ivr_decision_call_purpose drop is fully rolled back")
                .isNotNull();
        assertThat(toRegclass(connection, "ivr.uq_ivr_decision_source"))
                .as("uq_ivr_decision_source remains in place after rollback")
                .isNotNull();
        switchLocalRole(connection, "ivr_role");
    }

    private static void assertForeignKeyMissingParentAndParentDeleteRestrict() throws Exception {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "customer_iam_role");
                UUID validAccount = UUID.randomUUID();
                assertThat(insertCustomerAccount(connection, validAccount, "FK Probe Account")).isEqualTo(1);
                expectSqlFailure(connection, "23503", "fk_auth_identity_account", authIdentityInsertSql(),
                        UUID.randomUUID(), UUID.randomUUID(), "PHONE", "fk-scope",
                        "v1.fk.ciphertext", randomSha256());
                expectSqlFailure(connection, "23503", "fk_identity_profile_account",
                        "INSERT INTO customer_iam.identity_profile (id, account_id) VALUES (?, ?)",
                        UUID.randomUUID(), UUID.randomUUID());

                switchLocalRole(connection, "organization_access_role");
                UUID validOrganization = UUID.randomUUID();
                assertThat(insertOrganization(connection, validOrganization, "FK Probe Organization"))
                        .isEqualTo(1);
                expectSqlFailure(connection, "23503", "fk_membership_organization", membershipInsertSql(),
                        UUID.randomUUID(), UUID.randomUUID(), validAccount, "MEMBER");
                expectSqlFailure(connection, "23503", "fk_membership_account", membershipInsertSql(),
                        UUID.randomUUID(), validOrganization, UUID.randomUUID(), "MEMBER");
            } finally {
                connection.rollback();
            }
        }

        UUID identityParent = UUID.randomUUID();
        UUID identityChild = UUID.randomUUID();
        UUID membershipParent = UUID.randomUUID();
        UUID membershipChild = UUID.randomUUID();
        UUID organizationParent = UUID.randomUUID();
        UUID organizationChild = UUID.randomUUID();
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "customer_iam_role");
                assertThat(insertCustomerAccount(connection, identityParent, "Delete Restrict Identity Parent"))
                        .isEqualTo(1);
                assertThat(insertAuthIdentity(connection, identityChild, identityParent, "PHONE",
                        "delete-restrict-scope", "v1.delete.ciphertext", randomSha256())).isEqualTo(1);
                assertThat(insertCustomerAccount(connection, membershipParent,
                        "Delete Restrict Membership Parent")).isEqualTo(1);

                switchLocalRole(connection, "organization_access_role");
                assertThat(insertOrganization(connection, organizationParent, "Delete Restrict Organization"))
                        .isEqualTo(1);
                assertThat(insertMembership(connection, organizationChild, organizationParent,
                        membershipParent, "MEMBER")).isEqualTo(1);

                switchLocalRole(connection, "customer_iam_role");
                try (Statement statement = connection.createStatement()) {
                    statement.execute("RESET ROLE");
                }
                assertThat(queryString(connection, "SELECT current_user")).isEqualTo(DB_USER);
                expectSqlFailure(connection, "23503", "fk_auth_identity_account",
                        "DELETE FROM customer_iam.customer_account WHERE id = ?", identityParent);
                expectSqlFailure(connection, "23503", "fk_membership_account",
                        "DELETE FROM customer_iam.customer_account WHERE id = ?", membershipParent);
                expectSqlFailure(connection, "23503", "fk_membership_organization",
                        "DELETE FROM organization_access.organization WHERE id = ?", organizationParent);
            } finally {
                connection.rollback();
            }
        }
    }

    private static void assertNoIdDefaultsOrSequences() throws Exception {
        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM pg_attrdef d "
                            + "JOIN pg_attribute a ON a.attrelid = d.adrelid AND a.attnum = d.adnum "
                            + "JOIN pg_class c ON c.oid = a.attrelid "
                            + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                            + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                            + "AND c.relname IN ('customer_account','auth_identity','identity_profile',"
                            + "'organization','organization_membership','employee_account') "
                            + "AND a.attname = 'id'"))
                    .as("no V002 id column has a default").isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                            + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                            + "AND c.relkind = 'S'"))
                    .as("no V002 sequence exists").isZero();
        }
    }

    private static void assertLocalImmutabilityGuardsAndAuthRotation() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "customer_iam_role");
                assertThat(insertCustomerAccount(connection, accountId, "Guard Customer")).isEqualTo(1);
                assertThat(insertAuthIdentity(connection, identityId, accountId, "PHONE", "guard-scope",
                        "v1.guard.ciphertext", randomSha256())).isEqualTo(1);
                assertThat(insertIdentityProfile(connection, profileId, accountId)).isEqualTo(1);

                switchLocalRole(connection, "organization_access_role");
                assertThat(insertOrganization(connection, organizationId, "Guard Organization")).isEqualTo(1);
                assertThat(insertMembership(connection, membershipId, organizationId, accountId, "MEMBER"))
                        .isEqualTo(1);

                switchLocalRole(connection, "operations_iam_role");
                assertThat(insertEmployee(connection, employeeId, randomSha256(), "op***guard",
                        "guard-issuer", randomSha256(), "Guard Employee", "OPS")).isEqualTo(1);

                assertRuntimeImmutableColumnDenied(connection, accountId, identityId, profileId,
                        organizationId, membershipId, employeeId);
                assertAllowedMutableUpdatesSucceed(connection, accountId, identityId, profileId,
                        organizationId, membershipId, employeeId);
                assertOwnerImmutableColumnGuards(connection, accountId, identityId, profileId,
                        organizationId, membershipId, employeeId);
                assertOwnerAuthRotationGuards(connection, identityId);
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", accountId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.auth_identity WHERE id = ?", identityId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.identity_profile WHERE id = ?", profileId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?", organizationId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization_membership WHERE id = ?",
                    membershipId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM operations_iam.employee_account WHERE id = ?", employeeId)).isZero();
        }
    }

    private static void assertRuntimeImmutableColumnDenied(
            Connection connection,
            UUID accountId,
            UUID identityId,
            UUID profileId,
            UUID organizationId,
            UUID membershipId,
            UUID employeeId) throws SQLException {
        switchLocalRole(connection, "customer_iam_role");
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.customer_account SET id = ? WHERE id = ?",
                UUID.randomUUID(), accountId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.customer_account SET created_at = now() + interval '1 day' "
                        + "WHERE id = ?", accountId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.auth_identity SET id = ? WHERE id = ?",
                UUID.randomUUID(), identityId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.auth_identity SET account_id = ? WHERE id = ?",
                accountId, identityId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.auth_identity SET provider = 'WECHAT' WHERE id = ?", identityId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.auth_identity SET provider_scope = 'other-scope' WHERE id = ?",
                identityId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.auth_identity SET provider_subject_ciphertext = 'v1.other.value' "
                        + "WHERE id = ?", identityId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.auth_identity SET provider_subject_hmac = ? WHERE id = ?",
                randomSha256(), identityId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.identity_profile SET id = ? WHERE id = ?",
                UUID.randomUUID(), profileId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE customer_iam.identity_profile SET account_id = ? WHERE id = ?",
                UUID.randomUUID(), profileId);

        switchLocalRole(connection, "organization_access_role");
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization SET id = ? WHERE id = ?",
                UUID.randomUUID(), organizationId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization SET created_at = now() + interval '1 day' "
                        + "WHERE id = ?", organizationId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization_membership SET id = ? WHERE id = ?",
                UUID.randomUUID(), membershipId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization_membership SET organization_id = ? WHERE id = ?",
                UUID.randomUUID(), membershipId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE organization_access.organization_membership SET account_id = ? WHERE id = ?",
                UUID.randomUUID(), membershipId);

        switchLocalRole(connection, "operations_iam_role");
        expectSqlFailure(connection, "42501", null,
                "UPDATE operations_iam.employee_account SET id = ? WHERE id = ?",
                UUID.randomUUID(), employeeId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE operations_iam.employee_account SET login_name_hmac = ? WHERE id = ?",
                randomSha256(), employeeId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE operations_iam.employee_account SET auth_issuer = 'other-issuer' WHERE id = ?",
                employeeId);
        expectSqlFailure(connection, "42501", null,
                "UPDATE operations_iam.employee_account SET auth_subject_hmac = ? WHERE id = ?",
                randomSha256(), employeeId);
    }

    private static void assertAllowedMutableUpdatesSucceed(
            Connection connection,
            UUID accountId,
            UUID identityId,
            UUID profileId,
            UUID organizationId,
            UUID membershipId,
            UUID employeeId) throws SQLException {
        switchLocalRole(connection, "customer_iam_role");
        assertThat(executeUpdate(connection,
                "UPDATE customer_iam.customer_account SET display_name = ?, status = 'DISABLED', "
                        + "revision = revision + 1, updated_at = now() WHERE id = ?",
                "Guarded Customer", accountId)).isEqualTo(1);
        assertThat(executeUpdate(connection,
                "UPDATE customer_iam.auth_identity SET status = 'DISABLED', verified_at = now(), "
                        + "key_version = key_version + 1, revision = revision + 1 WHERE id = ?",
                identityId)).isEqualTo(1);
        assertThat(executeUpdate(connection,
                "UPDATE customer_iam.identity_profile SET real_name_status = 'VERIFIED', "
                        + "legal_name_ciphertext = ?, id_number_ciphertext = ?, id_number_hmac = ?, "
                        + "verification_ref_ciphertext = ?, verified_at = now(), revision = revision + 1 "
                        + "WHERE id = ?",
                "v1.guard.legal", "v1.guard.id", randomSha256(), "v1.guard.ref", profileId))
                .isEqualTo(1);

        switchLocalRole(connection, "organization_access_role");
        assertThat(executeUpdate(connection,
                "UPDATE organization_access.organization SET display_name = ?, status = 'SUSPENDED', "
                        + "revision = revision + 1, updated_at = now() WHERE id = ?",
                "Guarded Organization", organizationId)).isEqualTo(1);
        assertThat(executeUpdate(connection,
                "UPDATE organization_access.organization_membership SET role = 'AUDITOR', "
                        + "department_label = 'Guarded', status = 'SUSPENDED', revision = revision + 1 "
                        + "WHERE id = ?", membershipId)).isEqualTo(1);

        switchLocalRole(connection, "operations_iam_role");
        assertThat(executeUpdate(connection,
                "UPDATE operations_iam.employee_account SET display_name = ?, role = 'ADMIN', "
                        + "status = 'DISABLED', revision = revision + 1 WHERE id = ?",
                "Guarded Employee", employeeId)).isEqualTo(1);
    }

    private static void assertOwnerImmutableColumnGuards(
            Connection connection,
            UUID accountId,
            UUID identityId,
            UUID profileId,
            UUID organizationId,
            UUID membershipId,
            UUID employeeId) throws SQLException {
        switchLocalRole(connection, "customer_iam_owner");
        expectSqlFailure(connection, "P0001", "CUSTOMER_ACCOUNT_IMMUTABLE",
                "UPDATE customer_iam.customer_account SET id = ? WHERE id = ?",
                UUID.randomUUID(), accountId);
        expectSqlFailure(connection, "P0001", "CUSTOMER_ACCOUNT_IMMUTABLE",
                "UPDATE customer_iam.customer_account SET created_at = created_at + interval '1 day' "
                        + "WHERE id = ?", accountId);
        expectSqlFailure(connection, "P0001", "AUTH_IDENTITY_IMMUTABLE",
                "UPDATE customer_iam.auth_identity SET id = ? WHERE id = ?",
                UUID.randomUUID(), identityId);
        expectSqlFailure(connection, "P0001", "AUTH_IDENTITY_IMMUTABLE",
                "UPDATE customer_iam.auth_identity SET account_id = ? WHERE id = ?",
                UUID.randomUUID(), identityId);
        expectSqlFailure(connection, "P0001", "AUTH_IDENTITY_IMMUTABLE",
                "UPDATE customer_iam.auth_identity SET provider = 'WECHAT' WHERE id = ?", identityId);
        expectSqlFailure(connection, "P0001", "AUTH_IDENTITY_IMMUTABLE",
                "UPDATE customer_iam.auth_identity SET provider_scope = 'other-scope' WHERE id = ?",
                identityId);
        expectSqlFailure(connection, "P0001", "IDENTITY_PROFILE_IMMUTABLE",
                "UPDATE customer_iam.identity_profile SET id = ? WHERE id = ?",
                UUID.randomUUID(), profileId);
        expectSqlFailure(connection, "P0001", "IDENTITY_PROFILE_IMMUTABLE",
                "UPDATE customer_iam.identity_profile SET account_id = ? WHERE id = ?",
                UUID.randomUUID(), profileId);

        switchLocalRole(connection, "organization_access_owner");
        expectSqlFailure(connection, "P0001", "ORGANIZATION_IMMUTABLE",
                "UPDATE organization_access.organization SET id = ? WHERE id = ?",
                UUID.randomUUID(), organizationId);
        expectSqlFailure(connection, "P0001", "ORGANIZATION_IMMUTABLE",
                "UPDATE organization_access.organization SET created_at = created_at + interval '1 day' "
                        + "WHERE id = ?", organizationId);
        expectSqlFailure(connection, "P0001", "MEMBERSHIP_IMMUTABLE",
                "UPDATE organization_access.organization_membership SET id = ? WHERE id = ?",
                UUID.randomUUID(), membershipId);
        expectSqlFailure(connection, "P0001", "MEMBERSHIP_IMMUTABLE",
                "UPDATE organization_access.organization_membership SET organization_id = ? WHERE id = ?",
                UUID.randomUUID(), membershipId);
        expectSqlFailure(connection, "P0001", "MEMBERSHIP_IMMUTABLE",
                "UPDATE organization_access.organization_membership SET account_id = ? WHERE id = ?",
                UUID.randomUUID(), membershipId);

        switchLocalRole(connection, "operations_iam_owner");
        expectSqlFailure(connection, "P0001", "EMPLOYEE_ACCOUNT_IMMUTABLE",
                "UPDATE operations_iam.employee_account SET id = ? WHERE id = ?",
                UUID.randomUUID(), employeeId);
        expectSqlFailure(connection, "P0001", "EMPLOYEE_ACCOUNT_IMMUTABLE",
                "UPDATE operations_iam.employee_account SET login_name_hmac = ? WHERE id = ?",
                randomSha256(), employeeId);
        expectSqlFailure(connection, "P0001", "EMPLOYEE_ACCOUNT_IMMUTABLE",
                "UPDATE operations_iam.employee_account SET auth_issuer = 'other-issuer' WHERE id = ?",
                employeeId);
        expectSqlFailure(connection, "P0001", "EMPLOYEE_ACCOUNT_IMMUTABLE",
                "UPDATE operations_iam.employee_account SET auth_subject_hmac = ? WHERE id = ?",
                randomSha256(), employeeId);
    }

    private static void assertOwnerAuthRotationGuards(Connection connection, UUID identityId)
            throws SQLException {
        switchLocalRole(connection, "customer_iam_owner");
        String newCiphertext = "v1.rotated.ciphertext." + UUID.randomUUID();
        String newHmac = randomSha256();
        expectSqlFailure(connection, "P0001", "AUTH_IDENTITY_IMMUTABLE",
                "UPDATE customer_iam.auth_identity SET provider_subject_ciphertext = ? WHERE id = ?",
                newCiphertext, identityId);
        expectSqlFailure(connection, "P0001", "AUTH_IDENTITY_IMMUTABLE",
                "UPDATE customer_iam.auth_identity SET provider_subject_hmac = ? WHERE id = ?",
                newHmac, identityId);
        expectSqlFailure(connection, "P0001", "AUTH_IDENTITY_IMMUTABLE",
                "UPDATE customer_iam.auth_identity SET provider_subject_ciphertext = ?, "
                        + "provider_subject_hmac = ? WHERE id = ?",
                newCiphertext, newHmac, identityId);
        expectSqlFailure(connection, "P0001", "AUTH_IDENTITY_IMMUTABLE",
                "UPDATE customer_iam.auth_identity SET provider_subject_ciphertext = ?, "
                        + "provider_subject_hmac = ?, key_version = key_version + 2 WHERE id = ?",
                newCiphertext, newHmac, identityId);
        assertThat(executeUpdate(connection,
                "UPDATE customer_iam.auth_identity SET provider_subject_ciphertext = ?, "
                        + "provider_subject_hmac = ?, key_version = key_version + 1 WHERE id = ?",
                newCiphertext, newHmac, identityId))
                .as("both change plus exactly one key-version increment is atomic and accepted")
                .isEqualTo(1);
        assertThat(queryString(connection,
                "SELECT provider_subject_ciphertext FROM customer_iam.auth_identity WHERE id = ?",
                identityId)).isEqualTo(newCiphertext);
        assertThat(queryString(connection,
                "SELECT provider_subject_hmac FROM customer_iam.auth_identity WHERE id = ?", identityId))
                .isEqualTo(newHmac);
    }

    private static void assertIdentityCollisionAndMembershipReplacementSemantics() throws Exception {
        UUID accountA = UUID.randomUUID();
        UUID accountB = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID membershipOne = UUID.randomUUID();
        UUID membershipTwo = UUID.randomUUID();
        UUID membershipThree = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        String provider = "PHONE";
        String scope = "lifecycle-" + UUID.randomUUID();
        String ciphertext = "v1.lifecycle.ciphertext";
        String hmac = randomSha256();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "customer_iam_role");
                assertThat(insertCustomerAccount(connection, accountA, "Lifecycle Account A")).isEqualTo(1);
                assertThat(insertCustomerAccount(connection, accountB, "Lifecycle Account B")).isEqualTo(1);
                assertThat(insertAuthIdentity(connection, identityId, accountA, provider, scope,
                        ciphertext, hmac)).isEqualTo(1);
                assertThat(classifyIdentityInsert(connection, UUID.randomUUID(), accountA, provider,
                        scope, ciphertext, hmac))
                        .as("the same account on the winning identity tuple is an idempotent success")
                        .isEqualTo("IDEMPOTENT");
                assertThat(classifyIdentityInsert(connection, UUID.randomUUID(), accountB, provider,
                        scope, ciphertext, hmac))
                        .as("a different account on the winning identity tuple is IDENTITY_CONFLICT")
                        .isEqualTo("IDENTITY_CONFLICT");
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM customer_iam.auth_identity WHERE provider = ? "
                                + "AND provider_scope = ? AND provider_subject_hmac = ?",
                        provider, scope, hmac)).isEqualTo(1);

                assertThat(insertIdentityProfile(connection, profileId, accountA)).isEqualTo(1);
                expectSqlFailure(connection, "23505", "uq_identity_profile_account",
                        "INSERT INTO customer_iam.identity_profile (id, account_id) VALUES (?, ?)",
                        UUID.randomUUID(), accountA);

                switchLocalRole(connection, "operations_iam_role");
                String loginOne = randomSha256();
                String issuer = "lifecycle-issuer";
                String subjectOne = randomSha256();
                assertThat(insertEmployee(connection, employeeId, loginOne, "op***life", issuer,
                        subjectOne, "Lifecycle Employee", "OPS")).isEqualTo(1);
                expectSqlFailure(connection, "23505", "uq_employee_login_hmac", employeeInsertSql(),
                        UUID.randomUUID(), loginOne, "op***dup", "other-issuer", randomSha256(),
                        "Duplicate Login", "OPS");
                expectSqlFailure(connection, "23505", "uq_employee_issuer_subject", employeeInsertSql(),
                        UUID.randomUUID(), randomSha256(), "op***dup2", issuer, subjectOne,
                        "Duplicate Subject", "OPS");

                switchLocalRole(connection, "organization_access_role");
                assertThat(insertOrganization(connection, organizationId, "Lifecycle Organization"))
                        .isEqualTo(1);
                assertThat(insertMembership(connection, membershipOne, organizationId, accountA, "MEMBER"))
                        .isEqualTo(1);
                expectSqlFailure(connection, "23505", "uq_membership_live", membershipInsertSql(),
                        UUID.randomUUID(), organizationId, accountA, "MEMBER");

                assertThat(executeUpdate(connection,
                        "UPDATE organization_access.organization_membership SET status = 'REVOKED', "
                                + "revision = revision + 1 WHERE id = ? AND revision = 1", membershipOne))
                        .isEqualTo(1);
                assertThat(insertMembership(connection, membershipTwo, organizationId, accountA, "AUDITOR"))
                        .as("a new row re-grants membership after the prior row is revoked")
                        .isEqualTo(1);
                expectSqlFailure(connection, "23505", "uq_membership_live",
                        "UPDATE organization_access.organization_membership SET status = 'ACTIVE', "
                                + "revision = revision + 1 WHERE id = ?", membershipOne);
                assertThat(executeUpdate(connection,
                        "UPDATE organization_access.organization_membership SET status = 'REVOKED', "
                                + "revision = revision + 1 WHERE id = ? AND revision = 1", membershipTwo))
                        .isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM organization_access.organization_membership "
                                + "WHERE organization_id = ? AND account_id = ? AND status = 'REVOKED'",
                        organizationId, accountA))
                        .as("multiple revoked rows for the same pair are allowed")
                        .isEqualTo(2);
                assertThat(insertMembership(connection, membershipThree, organizationId, accountA,
                        "ADMINISTRATOR")).isEqualTo(1);
                expectSqlFailure(connection, "23505", "uq_membership_live",
                        "UPDATE organization_access.organization_membership SET status = 'ACTIVE', "
                                + "revision = revision + 1 WHERE id = ?", membershipOne);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM organization_access.organization_membership "
                                + "WHERE organization_id = ? AND account_id = ? AND status <> 'REVOKED'",
                        organizationId, accountA)).isEqualTo(1);

                switchLocalRole(connection, "organization_access_owner");
                expectSqlFailure(connection, "P0001", "MEMBERSHIP_IMMUTABLE",
                        "UPDATE organization_access.organization_membership SET organization_id = ? "
                                + "WHERE id = ?", UUID.randomUUID(), membershipThree);
                expectSqlFailure(connection, "P0001", "MEMBERSHIP_IMMUTABLE",
                        "UPDATE organization_access.organization_membership SET account_id = ? "
                                + "WHERE id = ?", UUID.randomUUID(), membershipThree);
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            for (UUID accountId : List.of(accountA, accountB)) {
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", accountId))
                        .isZero();
            }
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.auth_identity WHERE id = ?", identityId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.identity_profile WHERE id = ?", profileId)).isZero();
            for (UUID membershipId : List.of(membershipOne, membershipTwo, membershipThree)) {
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM organization_access.organization_membership WHERE id = ?",
                        membershipId)).isZero();
            }
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?", organizationId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM operations_iam.employee_account WHERE id = ?", employeeId)).isZero();
        }
    }

    private static String classifyIdentityInsert(
            Connection connection,
            UUID id,
            UUID accountId,
            String provider,
            String scope,
            String ciphertext,
            String hmac) throws SQLException {
        Savepoint savepoint = connection.setSavepoint();
        try {
            insertAuthIdentity(connection, id, accountId, provider, scope, ciphertext, hmac);
            return "NEW";
        } catch (SQLException exception) {
            assertThat(exception.getSQLState())
                    .as("identity tuple collision SQLSTATE").isEqualTo("23505");
            assertThat(exception.getMessage())
                    .as("identity tuple collision constraint").contains("uq_auth_identity_subject");
            connection.rollback(savepoint);
            UUID existingAccount = queryUuid(connection,
                    "SELECT account_id::text FROM customer_iam.auth_identity "
                            + "WHERE provider = ? AND provider_scope = ? AND provider_subject_hmac = ?",
                    provider, scope, hmac);
            return existingAccount.equals(accountId) ? "IDEMPOTENT" : "IDENTITY_CONFLICT";
        }
    }

    private static void configureBlockedWriter(Connection connection, String role, String applicationName)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCAL statement_timeout = 20000");
            statement.execute("SET LOCAL application_name = '" + applicationName + "'");
        }
        setLocalRole(connection, role);
        assertThat(queryString(connection, "SELECT current_setting('application_name')"))
                .isEqualTo(applicationName);
    }

    private static void commitCustomerAccounts(UUID... accountIds) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "customer_iam_role");
                for (UUID accountId : accountIds) {
                    assertThat(insertCustomerAccount(connection, accountId, "Race Account")).isEqualTo(1);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void commitOrganization(UUID organizationId) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "organization_access_role");
                assertThat(insertOrganization(connection, organizationId, "Race Organization")).isEqualTo(1);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void cleanupCommittedRaceRows(
            List<UUID> membershipIds,
            List<UUID> identityIds,
            List<UUID> profileIds,
            List<UUID> organizationIds,
            List<UUID> accountIds,
            List<UUID> employeeIds) throws SQLException {
        try (Connection connection = openConnection();
                Statement timeout = connection.createStatement()) {
            timeout.execute("SET statement_timeout = 20000");
            for (UUID id : membershipIds) {
                executeUpdate(connection,
                        "DELETE FROM organization_access.organization_membership WHERE id = ?", id);
            }
            for (UUID id : identityIds) {
                executeUpdate(connection, "DELETE FROM customer_iam.auth_identity WHERE id = ?", id);
            }
            for (UUID id : profileIds) {
                executeUpdate(connection, "DELETE FROM customer_iam.identity_profile WHERE id = ?", id);
            }
            for (UUID id : employeeIds) {
                executeUpdate(connection, "DELETE FROM operations_iam.employee_account WHERE id = ?", id);
            }
            for (UUID id : organizationIds) {
                executeUpdate(connection, "DELETE FROM organization_access.organization WHERE id = ?", id);
            }
            for (UUID id : accountIds) {
                executeUpdate(connection, "DELETE FROM customer_iam.customer_account WHERE id = ?", id);
            }
            for (UUID id : membershipIds) {
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM organization_access.organization_membership WHERE id = ?", id))
                        .isZero();
            }
            for (UUID id : identityIds) {
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM customer_iam.auth_identity WHERE id = ?", id)).isZero();
            }
            for (UUID id : profileIds) {
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM customer_iam.identity_profile WHERE id = ?", id)).isZero();
            }
            for (UUID id : employeeIds) {
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM operations_iam.employee_account WHERE id = ?", id)).isZero();
            }
            for (UUID id : organizationIds) {
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM organization_access.organization WHERE id = ?", id)).isZero();
            }
            for (UUID id : accountIds) {
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", id)).isZero();
            }
        }
    }

    private static void runConcurrentIdentityCollisionCommitRace() throws Exception {
        UUID winnerAccount = UUID.randomUUID();
        UUID loserAccount = UUID.randomUUID();
        UUID winnerIdentity = UUID.randomUUID();
        UUID loserIdentity = UUID.randomUUID();
        String provider = "PHONE";
        String scope = "race-identity-commit-" + UUID.randomUUID();
        String ciphertext = "v1.race.identity.commit";
        String hmac = randomSha256();
        String winnerApplication = "pa2-s2-identity-commit-winner";
        String loserApplication = "pa2-s2-identity-commit-loser";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            commitCustomerAccounts(winnerAccount, loserAccount);
            try (Connection winner = openConnection(); Connection loser = openConnection()) {
                winner.setAutoCommit(false);
                try (Statement statement = winner.createStatement()) {
                    statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
                }
                setLocalRole(winner, "customer_iam_role");
                assertThat(insertAuthIdentity(winner, winnerIdentity, winnerAccount, provider, scope,
                        ciphertext, hmac)).isEqualTo(1);

                loser.setAutoCommit(false);
                configureBlockedWriter(loser, "customer_iam_role", loserApplication);
                Future<Integer> loserFuture = executor.submit(() -> insertAuthIdentity(
                        loser, loserIdentity, loserAccount, provider, scope, ciphertext, hmac));
                awaitBlockedBackend(loserApplication);

                winner.commit();
                try {
                    awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                    fail("the identity unique loser must not commit a second row");
                } catch (SQLException exception) {
                    assertThat(exception.getSQLState()).isEqualTo("23505");
                    assertThat(exception.getMessage()).contains("uq_auth_identity_subject");
                }
                loser.rollback();
                setLocalRole(loser, "customer_iam_role");
                UUID existingAccount = queryUuid(loser,
                        "SELECT account_id::text FROM customer_iam.auth_identity "
                                + "WHERE provider = ? AND provider_scope = ? AND provider_subject_hmac = ?",
                        provider, scope, hmac);
                assertThat(existingAccount).isEqualTo(winnerAccount);
                String classification = existingAccount.equals(loserAccount)
                        ? "IDEMPOTENT" : "IDENTITY_CONFLICT";
                assertThat(classification)
                        .as("the loser reads the committed winner and maps a different account")
                        .isEqualTo("IDENTITY_CONFLICT");
                loser.rollback();
            }

            try (Connection verify = openConnection()) {
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM customer_iam.auth_identity WHERE provider = ? "
                                + "AND provider_scope = ? AND provider_subject_hmac = ?",
                        provider, scope, hmac)).isEqualTo(1);
                assertThat(queryString(verify,
                        "SELECT id::text FROM customer_iam.auth_identity WHERE provider = ? "
                                + "AND provider_scope = ? AND provider_subject_hmac = ?",
                        provider, scope, hmac)).isEqualTo(winnerIdentity.toString());
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM customer_iam.auth_identity WHERE id = ?", loserIdentity))
                        .isZero();
            }
        } finally {
            executor.shutdownNow();
            cleanupCommittedRaceRows(List.of(), List.of(winnerIdentity, loserIdentity), List.of(),
                    List.of(), List.of(winnerAccount, loserAccount), List.of());
        }
    }

    private static void runConcurrentIdentityCollisionRollbackRace() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID winnerIdentity = UUID.randomUUID();
        UUID loserIdentity = UUID.randomUUID();
        String provider = "PHONE";
        String scope = "race-identity-rollback-" + UUID.randomUUID();
        String ciphertext = "v1.race.identity.rollback";
        String hmac = randomSha256();
        String winnerApplication = "pa2-s2-identity-rollback-winner";
        String loserApplication = "pa2-s2-identity-rollback-loser";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            commitCustomerAccounts(accountId);
            try (Connection winner = openConnection(); Connection loser = openConnection()) {
                winner.setAutoCommit(false);
                try (Statement statement = winner.createStatement()) {
                    statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
                }
                setLocalRole(winner, "customer_iam_role");
                assertThat(insertAuthIdentity(winner, winnerIdentity, accountId, provider, scope,
                        ciphertext, hmac)).isEqualTo(1);

                loser.setAutoCommit(false);
                configureBlockedWriter(loser, "customer_iam_role", loserApplication);
                Future<Integer> loserFuture = executor.submit(() -> insertAuthIdentity(
                        loser, loserIdentity, accountId, provider, scope, ciphertext, hmac));
                awaitBlockedBackend(loserApplication);

                winner.rollback();
                assertThat(awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS))
                        .as("the loser inserts after the winner rolls back").isEqualTo(1);
                loser.commit();
            }

            try (Connection verify = openConnection()) {
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM customer_iam.auth_identity WHERE provider = ? "
                                + "AND provider_scope = ? AND provider_subject_hmac = ?",
                        provider, scope, hmac)).isEqualTo(1);
                assertThat(queryString(verify,
                        "SELECT id::text FROM customer_iam.auth_identity WHERE provider = ? "
                                + "AND provider_scope = ? AND provider_subject_hmac = ?",
                        provider, scope, hmac)).isEqualTo(loserIdentity.toString());
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM customer_iam.auth_identity WHERE id = ?", winnerIdentity))
                        .isZero();
            }
        } finally {
            executor.shutdownNow();
            cleanupCommittedRaceRows(List.of(), List.of(winnerIdentity, loserIdentity), List.of(),
                    List.of(), List.of(accountId), List.of());
        }
    }

    private static void runConcurrentMembershipCollisionCommitRace() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID winnerMembership = UUID.randomUUID();
        UUID loserMembership = UUID.randomUUID();
        String winnerApplication = "pa2-s2-membership-commit-winner";
        String loserApplication = "pa2-s2-membership-commit-loser";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            commitCustomerAccounts(accountId);
            commitOrganization(organizationId);
            try (Connection winner = openConnection(); Connection loser = openConnection()) {
                winner.setAutoCommit(false);
                try (Statement statement = winner.createStatement()) {
                    statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
                }
                setLocalRole(winner, "organization_access_role");
                assertThat(insertMembership(winner, winnerMembership, organizationId, accountId, "MEMBER"))
                        .isEqualTo(1);

                loser.setAutoCommit(false);
                configureBlockedWriter(loser, "organization_access_role", loserApplication);
                Future<Integer> loserFuture = executor.submit(() -> insertMembership(
                        loser, loserMembership, organizationId, accountId, "MEMBER"));
                awaitBlockedBackend(loserApplication);

                winner.commit();
                try {
                    awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                    fail("the live-membership unique loser must not commit a second live row");
                } catch (SQLException exception) {
                    assertThat(exception.getSQLState()).isEqualTo("23505");
                    assertThat(exception.getMessage()).contains("uq_membership_live");
                }
                loser.rollback();
                setLocalRole(loser, "organization_access_role");
                assertThat(queryUuid(loser,
                        "SELECT id::text FROM organization_access.organization_membership "
                                + "WHERE organization_id = ? AND account_id = ? AND status <> 'REVOKED'",
                        organizationId, accountId)).isEqualTo(winnerMembership);
                loser.rollback();
            }

            try (Connection verify = openConnection()) {
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM organization_access.organization_membership "
                                + "WHERE organization_id = ? AND account_id = ? AND status <> 'REVOKED'",
                        organizationId, accountId)).isEqualTo(1);
                assertThat(queryString(verify,
                        "SELECT id::text FROM organization_access.organization_membership "
                                + "WHERE organization_id = ? AND account_id = ? AND status <> 'REVOKED'",
                        organizationId, accountId)).isEqualTo(winnerMembership.toString());
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM organization_access.organization_membership WHERE id = ?",
                        loserMembership)).isZero();
            }
        } finally {
            executor.shutdownNow();
            cleanupCommittedRaceRows(List.of(winnerMembership, loserMembership), List.of(), List.of(),
                    List.of(organizationId), List.of(accountId), List.of());
        }
    }

    private static void runConcurrentMembershipCollisionRollbackRace() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID winnerMembership = UUID.randomUUID();
        UUID loserMembership = UUID.randomUUID();
        String winnerApplication = "pa2-s2-membership-rollback-winner";
        String loserApplication = "pa2-s2-membership-rollback-loser";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            commitCustomerAccounts(accountId);
            commitOrganization(organizationId);
            try (Connection winner = openConnection(); Connection loser = openConnection()) {
                winner.setAutoCommit(false);
                try (Statement statement = winner.createStatement()) {
                    statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
                }
                setLocalRole(winner, "organization_access_role");
                assertThat(insertMembership(winner, winnerMembership, organizationId, accountId, "MEMBER"))
                        .isEqualTo(1);

                loser.setAutoCommit(false);
                configureBlockedWriter(loser, "organization_access_role", loserApplication);
                Future<Integer> loserFuture = executor.submit(() -> insertMembership(
                        loser, loserMembership, organizationId, accountId, "MEMBER"));
                awaitBlockedBackend(loserApplication);

                winner.rollback();
                assertThat(awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS))
                        .as("the live-membership loser inserts after the winner rolls back").isEqualTo(1);
                loser.commit();
            }

            try (Connection verify = openConnection()) {
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM organization_access.organization_membership "
                                + "WHERE organization_id = ? AND account_id = ? AND status <> 'REVOKED'",
                        organizationId, accountId)).isEqualTo(1);
                assertThat(queryString(verify,
                        "SELECT id::text FROM organization_access.organization_membership "
                                + "WHERE organization_id = ? AND account_id = ? AND status <> 'REVOKED'",
                        organizationId, accountId)).isEqualTo(loserMembership.toString());
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM organization_access.organization_membership WHERE id = ?",
                        winnerMembership)).isZero();
            }
        } finally {
            executor.shutdownNow();
            cleanupCommittedRaceRows(List.of(winnerMembership, loserMembership), List.of(), List.of(),
                    List.of(organizationId), List.of(accountId), List.of());
        }
    }

    private static String randomSha256() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    private static String randomFingerprint() {
        return randomSha256();
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    private record BeginResult(
            UUID recordId,
            Integer revision,
            String outcome,
            String resultType,
            UUID resultId,
            String resultSnapshot,
            String resultSha256) {
    }

    private record StoredIdempotency(
            UUID id,
            String scope,
            String key,
            String fingerprint,
            String status,
            String resultType,
            UUID resultId,
            String resultSnapshot,
            String resultSha256,
            OffsetDateTime createdAt,
            OffsetDateTime completedAt,
            int revision) {
    }

    private record StoredOutbox(
            UUID id,
            String eventDomain,
            String dedupeKey,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payloadRedacted,
            String payloadCiphertext,
            String payloadSha256,
            String status,
            int attemptCount,
            OffsetDateTime availableAt,
            OffsetDateTime publishedAt,
            int revision) {
    }

    private static void assertCleanBeforeMigration() throws Exception {
        try (Connection connection = openConnection();
                Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT current_database(), current_user, current_setting('server_version_num')::int")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString(1)).isEqualTo("onecall");
                assertThat(resultSet.getString(2)).isEqualTo(DB_USER);
                assertThat(resultSet.getInt(3)).isGreaterThanOrEqualTo(160000);
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT count(*) FROM pg_namespace WHERE nspname IN "
                            + "('platform','audit','customer_iam','organization_access','operations_iam','subject_number')")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getLong(1)).isZero();
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT count(*) FROM information_schema.tables "
                            + "WHERE table_schema NOT IN ('pg_catalog','information_schema')")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getLong(1)).isZero();
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT to_regclass('public.flyway_schema_history') IS NULL")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean(1)).isTrue();
            }
        }
    }

    private static void exerciseV001FunctionsAsGrantedRole() throws Exception {
        String idempotencyKey = "s1-smoke-" + UUID.randomUUID();
        String fingerprint = "a".repeat(64);
        UUID resultId = UUID.randomUUID();
        String resultSha256 = "b".repeat(64);
        UUID aggregateId = UUID.randomUUID();
        String dedupeKey = "s1-outbox-" + UUID.randomUUID();
        String payloadSha256 = "c".repeat(64);
        UUID auditObjectId = UUID.randomUUID();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET LOCAL ROLE subject_number_role");
                }

                try (Statement statement = connection.createStatement();
                        ResultSet resultSet = statement.executeQuery("SELECT current_user")) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString(1)).isEqualTo("subject_number_role");
                }

                UUID recordId;
                try (PreparedStatement prepared = connection.prepareStatement(
                        "SELECT record_id, revision, outcome, result_type, result_id, result_snapshot, result_sha256 "
                                + "FROM platform.idempotency_begin(?, ?, ?)")) {
                    prepared.setString(1, "IVR_CONFIRM");
                    prepared.setString(2, idempotencyKey);
                    prepared.setString(3, fingerprint);
                    try (ResultSet resultSet = prepared.executeQuery()) {
                        assertThat(resultSet.next()).isTrue();
                        recordId = (UUID) resultSet.getObject(1);
                        assertThat(recordId).isNotNull();
                        assertThat(resultSet.getInt(2)).isEqualTo(1);
                        assertThat(resultSet.getString(3)).isEqualTo("NEW");
                        assertThat(resultSet.getObject(4)).isNull();
                        assertThat(resultSet.getObject(5)).isNull();
                        assertThat(resultSet.getObject(6)).isNull();
                        assertThat(resultSet.getObject(7)).isNull();
                        assertThat(resultSet.next()).isFalse();
                    }
                }

                try (PreparedStatement prepared = connection.prepareStatement(
                        "SELECT platform.idempotency_complete(?, ?, ?, ?, CAST(? AS jsonb), ?)")) {
                    prepared.setObject(1, recordId);
                    prepared.setInt(2, 1);
                    prepared.setString(3, "SMOKE_RESULT");
                    prepared.setObject(4, resultId);
                    prepared.setString(5, "{\"status\":\"ok\"}");
                    prepared.setString(6, resultSha256);
                    try (ResultSet resultSet = prepared.executeQuery()) {
                        assertThat(resultSet.next()).isTrue();
                        assertThat(resultSet.getString(1)).isEqualTo("COMPLETED");
                    }
                }

                try (PreparedStatement prepared = connection.prepareStatement(
                        "SELECT outcome, result_type, result_id, result_snapshot, result_sha256 "
                                + "FROM platform.idempotency_begin(?, ?, ?)")) {
                    prepared.setString(1, "IVR_CONFIRM");
                    prepared.setString(2, idempotencyKey);
                    prepared.setString(3, fingerprint);
                    try (ResultSet resultSet = prepared.executeQuery()) {
                        assertThat(resultSet.next()).isTrue();
                        assertThat(resultSet.getString(1)).isEqualTo("REPLAY");
                        assertThat(resultSet.getString(2)).isEqualTo("SMOKE_RESULT");
                        assertThat(resultSet.getObject(3)).isEqualTo(resultId);
                        assertThat(resultSet.getObject(4)).isNotNull();
                        assertThat(resultSet.getString(5)).isEqualTo(resultSha256);
                    }
                }

                try (PreparedStatement prepared = connection.prepareStatement(
                        "SELECT platform.outbox_append(?, ?, ?, ?, ?, CAST(? AS jsonb), NULL, ?)")) {
                    prepared.setString(1, "IVR_EVENT");
                    prepared.setString(2, dedupeKey);
                    prepared.setString(3, "CALL");
                    prepared.setObject(4, aggregateId);
                    prepared.setString(5, "CALL_EVENT");
                    prepared.setString(6, "{\"eventType\":\"TEST\"}");
                    prepared.setString(7, payloadSha256);
                    try (ResultSet resultSet = prepared.executeQuery()) {
                        assertThat(resultSet.next()).isTrue();
                        assertThat(resultSet.getObject(1)).isNotNull();
                    }
                }

                try (PreparedStatement prepared = connection.prepareStatement(
                        "SELECT audit.append_fact(CAST(? AS timestamptz), ?, ?, ?, ?, ?, NULL, "
                                + "CAST(? AS jsonb), NULL, ?, NULL)")) {
                    prepared.setObject(1, OffsetDateTime.now(ZoneOffset.UTC));
                    prepared.setString(2, "SERVICE");
                    prepared.setObject(3, UUID.randomUUID());
                    prepared.setString(4, "SMOKE_ACTION");
                    prepared.setString(5, "SMOKE_OBJECT");
                    prepared.setObject(6, auditObjectId);
                    prepared.setString(7, "{\"state\":\"ok\"}");
                    prepared.setString(8, "smoke-request");
                    try (ResultSet resultSet = prepared.executeQuery()) {
                        assertThat(resultSet.next()).isTrue();
                        assertThat(resultSet.getObject(1)).isNotNull();
                    }
                }
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM platform.idempotency_record WHERE scope = ? AND idempotency_key = ?",
                    "IVR_CONFIRM", idempotencyKey)).isZero();
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM platform.outbox_event WHERE event_domain = ? AND dedupe_key = ?",
                    "IVR_EVENT", dedupeKey)).isZero();
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM audit.audit_fact WHERE object_id = ?",
                    auditObjectId)).isZero();
        }
    }

    private static void exerciseV002InsertsAsOwningRuntimeRolesAndRollBack() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        String hmac = "d".repeat(64);
        String ciphertext = "v1.test-key.abc123payload";
        String masked = "op***1";

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET LOCAL ROLE customer_iam_role");
                }
                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO customer_iam.customer_account (id, display_name) VALUES (?, ?)")) {
                    prepared.setObject(1, accountId);
                    prepared.setString(2, "V002 Smoke Customer");
                    assertThat(prepared.executeUpdate()).isEqualTo(1);
                }
                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO customer_iam.auth_identity "
                                + "(id, account_id, provider, provider_scope, provider_subject_ciphertext, "
                                + "provider_subject_hmac, verified_at, key_version) "
                                + "VALUES (?, ?, 'PHONE', 'login-phone', ?, ?, now(), 1)")) {
                    prepared.setObject(1, identityId);
                    prepared.setObject(2, accountId);
                    prepared.setString(3, ciphertext);
                    prepared.setString(4, hmac);
                    assertThat(prepared.executeUpdate()).isEqualTo(1);
                }
                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO customer_iam.identity_profile (id, account_id) VALUES (?, ?)")) {
                    prepared.setObject(1, profileId);
                    prepared.setObject(2, accountId);
                    assertThat(prepared.executeUpdate()).isEqualTo(1);
                }
                try (Statement statement = connection.createStatement()) {
                    statement.execute("RESET ROLE");
                    statement.execute("SET LOCAL ROLE organization_access_role");
                }
                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO organization_access.organization (id, display_name) VALUES (?, ?)")) {
                    prepared.setObject(1, organizationId);
                    prepared.setString(2, "V002 Smoke Org");
                    assertThat(prepared.executeUpdate()).isEqualTo(1);
                }
                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO organization_access.organization_membership "
                                + "(id, organization_id, account_id, role) VALUES (?, ?, ?, 'MEMBER')")) {
                    prepared.setObject(1, membershipId);
                    prepared.setObject(2, organizationId);
                    prepared.setObject(3, accountId);
                    assertThat(prepared.executeUpdate()).isEqualTo(1);
                }
                try (Statement statement = connection.createStatement()) {
                    statement.execute("RESET ROLE");
                    statement.execute("SET LOCAL ROLE operations_iam_role");
                }
                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO operations_iam.employee_account "
                                + "(id, login_name_hmac, login_name_masked, auth_issuer, auth_subject_hmac, "
                                + "display_name, role) VALUES (?, ?, ?, 'onecall-ops', ?, ?, 'OPS')")) {
                    prepared.setObject(1, employeeId);
                    prepared.setString(2, hmac);
                    prepared.setString(3, masked);
                    prepared.setString(4, "e".repeat(64));
                    prepared.setString(5, "V002 Smoke Ops");
                    assertThat(prepared.executeUpdate()).isEqualTo(1);
                }
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", accountId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.auth_identity WHERE id = ?", identityId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.identity_profile WHERE id = ?", profileId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?", organizationId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization_membership WHERE id = ?", membershipId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM operations_iam.employee_account WHERE id = ?", employeeId)).isZero();
        }
    }

    private static void assertInventory(Connection connection) throws SQLException {
        assertSchemasAndRoles(connection);
        assertTablesColumnsChecksAndIndexes(connection);
        assertV002CheckPredicateInventory(connection);
        assertV003TablesColumnsChecksIndexesFks(connection);
        assertV003CheckPredicateInventory(connection);
        assertFunctionsAndTrigger(connection);
        assertGrants(connection);
    }

    private static void assertSchemasAndRoles(Connection connection) throws SQLException {
        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '=' || pg_get_userbyid(n.nspowner) FROM pg_namespace n "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access','operations_iam','subject_number',"
                        + "'catalog_order','ivr','entitlement_metering','activation_consent') "
                        + "ORDER BY n.nspname"))
                .containsExactly(
                        "activation_consent=activation_consent_owner",
                        "audit=audit_owner",
                        "catalog_order=catalog_order_owner",
                        "customer_iam=customer_iam_owner",
                        "entitlement_metering=entitlement_metering_owner",
                        "ivr=ivr_owner",
                        "operations_iam=operations_iam_owner",
                        "organization_access=organization_access_owner",
                        "platform=platform_reliability_owner",
                        "subject_number=subject_number_owner");

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_namespace WHERE nspname IN ('evidence','sharing')"))
                .isZero();

        assertThat(queryStrings(
                connection,
                "SELECT nspname FROM pg_namespace WHERE nspname <> 'public' "
                        + "AND nspname <> 'information_schema' AND nspname NOT LIKE 'pg%' ORDER BY nspname"))
                .containsExactly(
                        "activation_consent",
                        "audit",
                        "catalog_order",
                        "customer_iam",
                        "entitlement_metering",
                        "ivr",
                        "operations_iam",
                        "organization_access",
                        "platform",
                        "subject_number");

        List<String> expectedRoles = new ArrayList<>();
        for (String role : S1_ROLES) {
            expectedRoles.add(role + ":false:false:false:false");
        }
        for (String role : V004_ROLES) {
            expectedRoles.add(role + ":false:false:false:false");
        }
        expectedRoles.sort(String::compareTo);

        assertThat(queryStrings(
                connection,
                "SELECT rolname || ':' || rolcanlogin || ':' || rolsuper || ':' || rolcreatedb || ':' || rolcreaterole "
                        + "FROM pg_roles WHERE rolname <> '" + DB_USER + "' AND rolname NOT LIKE 'pg%' ORDER BY rolname"))
                .containsExactlyElementsOf(expectedRoles);

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_roles WHERE rolname IN "
                        + "('evidence_owner','evidence_role','sharing_owner','sharing_role')"))
                .isZero();
    }

    private static void assertTablesColumnsChecksAndIndexes(Connection connection) throws SQLException {
        assertThat(queryStrings(
                connection,
                "SELECT table_schema || '.' || table_name FROM information_schema.tables "
                        + "WHERE table_schema IN ('platform','audit','customer_iam','organization_access','operations_iam','subject_number') "
                        + "ORDER BY 1"))
                .containsExactlyElementsOf(ALL_V001_V003_TABLES);

        assertThat(queryStrings(
                connection,
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY 1"))
                .containsExactly("flyway_schema_history");

        assertThat(queryStrings(
                connection,
                "SELECT string_agg(column_name, ',' ORDER BY ordinal_position) FROM information_schema.columns "
                        + "WHERE table_schema = ? AND table_name = ?",
                "platform", "idempotency_record"))
                .containsExactly("id,scope,idempotency_key,request_fingerprint,status,result_type,result_id,"
                        + "result_snapshot,result_sha256,created_at,completed_at,revision");
        assertThat(queryStrings(
                connection,
                "SELECT string_agg(column_name, ',' ORDER BY ordinal_position) FROM information_schema.columns "
                        + "WHERE table_schema = ? AND table_name = ?",
                "platform", "outbox_event"))
                .containsExactly("id,event_domain,dedupe_key,aggregate_type,aggregate_id,event_type,"
                        + "payload_redacted,payload_ciphertext,payload_sha256,status,attempt_count,"
                        + "available_at,published_at,revision");
        assertThat(queryStrings(
                connection,
                "SELECT string_agg(column_name, ',' ORDER BY ordinal_position) FROM information_schema.columns "
                        + "WHERE table_schema = ? AND table_name = ?",
                "audit", "audit_fact"))
                .containsExactly("id,occurred_at,actor_type,actor_id,action_code,object_type,object_id,"
                        + "reason_code,before_snapshot,after_snapshot,request_id,trace_id");

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = ? AND table_name = ?",
                "platform", "idempotency_record")).isEqualTo(12);
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = ? AND table_name = ?",
                "platform", "outbox_event")).isEqualTo(14);
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = ? AND table_name = ?",
                "audit", "audit_fact")).isEqualTo(12);

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_attrdef d "
                        + "JOIN pg_attribute a ON a.attrelid = d.adrelid AND a.attnum = d.adnum "
                        + "JOIN pg_class r ON r.oid = d.adrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname IN ('platform','audit') "
                        + "AND r.relname IN ('idempotency_record','outbox_event','audit_fact') "
                        + "AND a.attname = 'id'")).isZero();

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access',"
                        + "'operations_iam','subject_number') AND c.relkind = 'S'")).isZero();

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_constraint c "
                        + "JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname IN ('platform','audit') AND c.contype = 'f'")).isZero();

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = ? AND r.relname = ? AND c.contype = 'c' ORDER BY conname",
                "platform", "idempotency_record"))
                .containsExactly(
                        "ck_idempotency_completed_after_created",
                        "ck_idempotency_fingerprint",
                        "ck_idempotency_key",
                        "ck_idempotency_result_sha256",
                        "ck_idempotency_result_snapshot_type",
                        "ck_idempotency_result_type",
                        "ck_idempotency_revision",
                        "ck_idempotency_scope",
                        "ck_idempotency_state_shape",
                        "ck_idempotency_status");

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = ? AND r.relname = ? AND c.contype = 'c' ORDER BY conname",
                "platform", "outbox_event"))
                .containsExactly(
                        "ck_outbox_aggregate_type",
                        "ck_outbox_attempt_count",
                        "ck_outbox_ciphertext_shape",
                        "ck_outbox_dedupe_key",
                        "ck_outbox_event_domain",
                        "ck_outbox_event_type",
                        "ck_outbox_payload_present",
                        "ck_outbox_payload_sha256",
                        "ck_outbox_published_after_available",
                        "ck_outbox_published_shape",
                        "ck_outbox_revision",
                        "ck_outbox_status");

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = ? AND r.relname = ? AND c.contype = 'c' ORDER BY conname",
                "audit", "audit_fact"))
                .containsExactly(
                        "ck_audit_action_code",
                        "ck_audit_actor_id",
                        "ck_audit_actor_type",
                        "ck_audit_after_type",
                        "ck_audit_before_type",
                        "ck_audit_object_id",
                        "ck_audit_object_type",
                        "ck_audit_reason_code",
                        "ck_audit_request_id",
                        "ck_audit_trace_id");

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = ? AND r.relname = ? AND c.contype IN ('p','u') ORDER BY conname",
                "platform", "idempotency_record"))
                .containsExactly("pk_idempotency_record", "uq_idempotency_scope_key");
        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = ? AND r.relname = ? AND c.contype IN ('p','u') ORDER BY conname",
                "platform", "outbox_event"))
                .containsExactly("pk_outbox_event", "uq_outbox_domain_dedupe");
        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = ? AND r.relname = ? AND c.contype IN ('p','u') ORDER BY conname",
                "audit", "audit_fact"))
                .containsExactly("pk_audit_fact");

        assertThat(queryStrings(
                connection,
                "SELECT schemaname || '.' || tablename || '.' || indexname FROM pg_indexes "
                        + "WHERE schemaname IN ('platform','audit') ORDER BY 1"))
                .containsExactly(
                        "audit.audit_fact.ix_audit_action_time",
                        "audit.audit_fact.ix_audit_actor_time",
                        "audit.audit_fact.ix_audit_object_time",
                        "audit.audit_fact.ix_audit_occurred_at",
                        "audit.audit_fact.pk_audit_fact",
                        "platform.idempotency_record.ix_idempotency_new_alert",
                        "platform.idempotency_record.pk_idempotency_record",
                        "platform.idempotency_record.uq_idempotency_scope_key",
                        "platform.outbox_event.ix_outbox_failed_available",
                        "platform.outbox_event.ix_outbox_pending_available",
                        "platform.outbox_event.pk_outbox_event",
                        "platform.outbox_event.uq_outbox_domain_dedupe");

        assertThat(queryString(
                connection,
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'platform' AND indexname = ?",
                "ix_idempotency_new_alert"))
                .isEqualTo("CREATE INDEX ix_idempotency_new_alert ON platform.idempotency_record "
                        + "USING btree (created_at) WHERE (status = 'NEW'::text)");
        assertThat(queryString(
                connection,
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'platform' AND indexname = ?",
                "ix_outbox_pending_available"))
                .isEqualTo("CREATE INDEX ix_outbox_pending_available ON platform.outbox_event "
                        + "USING btree (available_at, id) WHERE (status = 'PENDING'::text)");
        assertThat(queryString(
                connection,
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'platform' AND indexname = ?",
                "ix_outbox_failed_available"))
                .isEqualTo("CREATE INDEX ix_outbox_failed_available ON platform.outbox_event "
                        + "USING btree (available_at, id) WHERE (status = 'FAILED'::text)");

        Map<String, String> v002ColumnOrder = Map.of(
                "customer_account", "id,display_name,status,revision,created_at,updated_at",
                "auth_identity", "id,account_id,provider,provider_scope,provider_subject_ciphertext,"
                        + "provider_subject_hmac,status,verified_at,key_version,revision",
                "identity_profile", "id,account_id,real_name_status,legal_name_ciphertext,"
                        + "id_number_ciphertext,id_number_hmac,verification_ref_ciphertext,verified_at,revision",
                "organization", "id,display_name,status,revision,created_at,updated_at",
                "organization_membership", "id,organization_id,account_id,role,department_label,status,revision",
                "employee_account", "id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,"
                        + "display_name,role,status,revision");
        Map<String, String> v002SchemaByTable = Map.of(
                "customer_account", "customer_iam",
                "auth_identity", "customer_iam",
                "identity_profile", "customer_iam",
                "organization", "organization_access",
                "organization_membership", "organization_access",
                "employee_account", "operations_iam");
        for (Map.Entry<String, String> entry : v002ColumnOrder.entrySet()) {
            assertThat(queryStrings(
                    connection,
                    "SELECT string_agg(column_name, ',' ORDER BY ordinal_position) FROM information_schema.columns "
                            + "WHERE table_schema = ? AND table_name = ?",
                    v002SchemaByTable.get(entry.getKey()), entry.getKey()))
                    .as("columns of %s", entry.getKey())
                    .containsExactly(entry.getValue());
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM information_schema.columns WHERE table_schema = ? AND table_name = ?",
                    v002SchemaByTable.get(entry.getKey()), entry.getKey()))
                    .as("column count of %s", entry.getKey())
                    .isEqualTo(entry.getValue().split(",").length);
        }

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_attrdef d "
                        + "JOIN pg_attribute a ON a.attrelid = d.adrelid AND a.attnum = d.adnum "
                        + "JOIN pg_class r ON r.oid = d.adrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                        + "AND a.attname = 'id'")).isZero();

        // Exact per-column contract (S1 fix 1, finding 1): every ordered V002 column's exact
        // name, PostgreSQL type, nullability and default for all six tables (6/10/9/6/7/9).
        // Canonical PG16 representations: text defaults render 'ACTIVE'::text and
        // 'UNVERIFIED'::text, integer defaults render 1, timestamptz defaults render now(),
        // and absent defaults render <none> here. Exact row-set equality fails closed on a
        // same-named wrong type/nullability/default and on any missing, extra or reordered
        // column; substring/contains matching is not used.
        Map<String, List<String>> v002ColumnDetails = new java.util.LinkedHashMap<>();
        v002ColumnDetails.put("customer_account", List.of(
                "id|uuid|NO|<none>",
                "display_name|text|NO|<none>",
                "status|text|NO|'ACTIVE'::text",
                "revision|integer|NO|1",
                "created_at|timestamp with time zone|NO|now()",
                "updated_at|timestamp with time zone|NO|now()"));
        v002ColumnDetails.put("auth_identity", List.of(
                "id|uuid|NO|<none>",
                "account_id|uuid|NO|<none>",
                "provider|text|NO|<none>",
                "provider_scope|text|NO|<none>",
                "provider_subject_ciphertext|text|NO|<none>",
                "provider_subject_hmac|text|NO|<none>",
                "status|text|NO|'ACTIVE'::text",
                "verified_at|timestamp with time zone|NO|<none>",
                "key_version|integer|NO|<none>",
                "revision|integer|NO|1"));
        v002ColumnDetails.put("identity_profile", List.of(
                "id|uuid|NO|<none>",
                "account_id|uuid|NO|<none>",
                "real_name_status|text|NO|'UNVERIFIED'::text",
                "legal_name_ciphertext|text|YES|<none>",
                "id_number_ciphertext|text|YES|<none>",
                "id_number_hmac|text|YES|<none>",
                "verification_ref_ciphertext|text|YES|<none>",
                "verified_at|timestamp with time zone|YES|<none>",
                "revision|integer|NO|1"));
        v002ColumnDetails.put("organization", List.of(
                "id|uuid|NO|<none>",
                "display_name|text|NO|<none>",
                "status|text|NO|'ACTIVE'::text",
                "revision|integer|NO|1",
                "created_at|timestamp with time zone|NO|now()",
                "updated_at|timestamp with time zone|NO|now()"));
        v002ColumnDetails.put("organization_membership", List.of(
                "id|uuid|NO|<none>",
                "organization_id|uuid|NO|<none>",
                "account_id|uuid|NO|<none>",
                "role|text|NO|<none>",
                "department_label|text|YES|<none>",
                "status|text|NO|'ACTIVE'::text",
                "revision|integer|NO|1"));
        v002ColumnDetails.put("employee_account", List.of(
                "id|uuid|NO|<none>",
                "login_name_hmac|text|NO|<none>",
                "login_name_masked|text|NO|<none>",
                "auth_issuer|text|NO|<none>",
                "auth_subject_hmac|text|NO|<none>",
                "display_name|text|NO|<none>",
                "role|text|NO|<none>",
                "status|text|NO|'ACTIVE'::text",
                "revision|integer|NO|1"));
        for (Map.Entry<String, List<String>> entry : v002ColumnDetails.entrySet()) {
            String schema = v002SchemaByTable.get(entry.getKey());
            assertThat(queryStrings(
                    connection,
                    "SELECT column_name || '|' || data_type || '|' || is_nullable || '|' "
                            + "|| coalesce(column_default, '<none>') FROM information_schema.columns "
                            + "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position",
                    schema, entry.getKey()))
                    .as("exact column name/type/nullability/default rows of %s", entry.getKey())
                    .containsExactlyElementsOf(entry.getValue());
            long expectedDefaults = entry.getValue().stream()
                    .filter(column -> !column.endsWith("|<none>"))
                    .count();
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM pg_attrdef d "
                            + "JOIN pg_class r ON r.oid = d.adrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = ? AND r.relname = ?",
                    schema, entry.getKey()))
                    .as("exact defaulted-column count of %s", entry.getKey())
                    .isEqualTo(expectedDefaults);
        }
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_attrdef d "
                        + "JOIN pg_class r ON r.oid = d.adrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam')"))
                .as("total V002 defaulted columns (no unlisted default)")
                .isEqualTo(16);

        Map<String, List<String>> v002CheckNames = new java.util.LinkedHashMap<>();
        v002CheckNames.put("customer_account", List.of(
                "ck_customer_account_display_name",
                "ck_customer_account_revision",
                "ck_customer_account_status",
                "ck_customer_account_time_order"));
        v002CheckNames.put("auth_identity", List.of(
                "ck_auth_identity_ciphertext",
                "ck_auth_identity_hmac",
                "ck_auth_identity_key_version",
                "ck_auth_identity_provider",
                "ck_auth_identity_revision",
                "ck_auth_identity_scope",
                "ck_auth_identity_status"));
        v002CheckNames.put("identity_profile", List.of(
                "ck_identity_profile_id_ciphertext",
                "ck_identity_profile_id_hmac",
                "ck_identity_profile_legal_ciphertext",
                "ck_identity_profile_revision",
                "ck_identity_profile_state_shape",
                "ck_identity_profile_status",
                "ck_identity_profile_verification_ref"));
        v002CheckNames.put("organization", List.of(
                "ck_organization_display_name",
                "ck_organization_revision",
                "ck_organization_status",
                "ck_organization_time_order"));
        v002CheckNames.put("organization_membership", List.of(
                "ck_membership_department",
                "ck_membership_revision",
                "ck_membership_role",
                "ck_membership_status"));
        v002CheckNames.put("employee_account", List.of(
                "ck_employee_display_name",
                "ck_employee_issuer",
                "ck_employee_login_hmac",
                "ck_employee_login_masked",
                "ck_employee_revision",
                "ck_employee_role",
                "ck_employee_status",
                "ck_employee_subject_hmac"));
        for (Map.Entry<String, List<String>> entry : v002CheckNames.entrySet()) {
            assertThat(queryStrings(
                    connection,
                    "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = ? AND r.relname = ? AND c.contype = 'c' ORDER BY conname",
                    v002SchemaByTable.get(entry.getKey()), entry.getKey()))
                    .as("check names of %s", entry.getKey())
                    .containsExactlyElementsOf(entry.getValue());
        }

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                        + "AND c.contype = 'p' ORDER BY 1"))
                .containsExactly(
                        "pk_auth_identity",
                        "pk_customer_account",
                        "pk_employee_account",
                        "pk_identity_profile",
                        "pk_organization",
                        "pk_organization_membership");

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                        + "AND c.contype = 'u' ORDER BY 1"))
                .containsExactly(
                        "uq_auth_identity_subject",
                        "uq_employee_issuer_subject",
                        "uq_employee_login_hmac",
                        "uq_identity_profile_account");

        // Exact PK/unique contract (S1 fix 1, finding 1): the six PK and four unique
        // constraints must sit on the exact table, carry the exact uniqueness semantics
        // (contype p/u) and expose the exact ordered column sequence through PG16 whole
        // pg_get_constraintdef plus conkey-ordered column names. A same-named constraint on
        // the wrong table, wrong contype, wrong column set or wrong column order fails
        // closed; name-only matching is not sufficient.
        Map<String, String[]> v002PkUniqueDefs = new java.util.LinkedHashMap<>();
        v002PkUniqueDefs.put("customer_iam.customer_account.pk_customer_account", new String[] {
                "customer_iam", "customer_account", "pk_customer_account", "p",
                "PRIMARY KEY (id)", "id"});
        v002PkUniqueDefs.put("customer_iam.auth_identity.pk_auth_identity", new String[] {
                "customer_iam", "auth_identity", "pk_auth_identity", "p",
                "PRIMARY KEY (id)", "id"});
        v002PkUniqueDefs.put("customer_iam.auth_identity.uq_auth_identity_subject", new String[] {
                "customer_iam", "auth_identity", "uq_auth_identity_subject", "u",
                "UNIQUE (provider, provider_scope, provider_subject_hmac)",
                "provider,provider_scope,provider_subject_hmac"});
        v002PkUniqueDefs.put("customer_iam.identity_profile.pk_identity_profile", new String[] {
                "customer_iam", "identity_profile", "pk_identity_profile", "p",
                "PRIMARY KEY (id)", "id"});
        v002PkUniqueDefs.put("customer_iam.identity_profile.uq_identity_profile_account", new String[] {
                "customer_iam", "identity_profile", "uq_identity_profile_account", "u",
                "UNIQUE (account_id)", "account_id"});
        v002PkUniqueDefs.put("organization_access.organization.pk_organization", new String[] {
                "organization_access", "organization", "pk_organization", "p",
                "PRIMARY KEY (id)", "id"});
        v002PkUniqueDefs.put("organization_access.organization_membership.pk_organization_membership",
                new String[] {
                        "organization_access", "organization_membership", "pk_organization_membership", "p",
                        "PRIMARY KEY (id)", "id"});
        v002PkUniqueDefs.put("operations_iam.employee_account.pk_employee_account", new String[] {
                "operations_iam", "employee_account", "pk_employee_account", "p",
                "PRIMARY KEY (id)", "id"});
        v002PkUniqueDefs.put("operations_iam.employee_account.uq_employee_login_hmac", new String[] {
                "operations_iam", "employee_account", "uq_employee_login_hmac", "u",
                "UNIQUE (login_name_hmac)", "login_name_hmac"});
        v002PkUniqueDefs.put("operations_iam.employee_account.uq_employee_issuer_subject", new String[] {
                "operations_iam", "employee_account", "uq_employee_issuer_subject", "u",
                "UNIQUE (auth_issuer, auth_subject_hmac)", "auth_issuer,auth_subject_hmac"});
        assertThat(v002PkUniqueDefs).as("expected PK/unique map size").hasSize(10);
        for (Map.Entry<String, String[]> entry : v002PkUniqueDefs.entrySet()) {
            String[] expected = entry.getValue();
            List<String> actualDefs = queryStrings(
                    connection,
                    "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c "
                            + "JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = ? AND r.relname = ? AND c.conname = ? "
                            + "AND c.contype::text = ?",
                    expected[0], expected[1], expected[2], expected[3]);
            assertThat(actualDefs)
                    .as("exactly one %s row for %s", expected[3], entry.getKey())
                    .hasSize(1);
            assertThat(actualDefs.get(0))
                    .as("whole pg_get_constraintdef equality for %s", entry.getKey())
                    .isEqualTo(expected[4]);
            assertThat(queryStrings(
                    connection,
                    "SELECT a.attname FROM pg_constraint c "
                            + "JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "JOIN unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord) ON true "
                            + "JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum "
                            + "WHERE n.nspname = ? AND r.relname = ? AND c.conname = ? "
                            + "AND c.contype::text = ? ORDER BY k.ord",
                    expected[0], expected[1], expected[2], expected[3]))
                    .as("ordered key columns of %s", entry.getKey())
                    .containsExactlyElementsOf(List.of(expected[5].split(",")));
        }

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname IN ('customer_iam','organization_access') AND c.contype = 'f' "
                        + "ORDER BY conname"))
                .containsExactly(
                        "fk_auth_identity_account",
                        "fk_identity_profile_account",
                        "fk_membership_account",
                        "fk_membership_organization");

        Map<String, String> v002FkDefs = Map.of(
                "fk_auth_identity_account",
                        "FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id) "
                                + "ON UPDATE RESTRICT ON DELETE RESTRICT",
                "fk_identity_profile_account",
                        "FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id) "
                                + "ON UPDATE RESTRICT ON DELETE RESTRICT",
                "fk_membership_organization",
                        "FOREIGN KEY (organization_id) REFERENCES organization_access.organization(id) "
                                + "ON UPDATE RESTRICT ON DELETE RESTRICT",
                "fk_membership_account",
                        "FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id) "
                                + "ON UPDATE RESTRICT ON DELETE RESTRICT");
        for (Map.Entry<String, String> entry : v002FkDefs.entrySet()) {
            assertThat(queryString(
                    connection,
                    "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname IN ('customer_iam','organization_access') AND c.conname = ?",
                    entry.getKey()))
                    .as("FK def %s", entry.getKey())
                    .isEqualTo(entry.getValue());
        }

        assertThat(queryStrings(
                connection,
                "SELECT schemaname || '.' || tablename || '.' || indexname FROM pg_indexes "
                        + "WHERE schemaname IN ('customer_iam','organization_access','operations_iam') ORDER BY 1"))
                .containsExactly(
                        "customer_iam.auth_identity.ix_auth_identity_account_status",
                        "customer_iam.auth_identity.pk_auth_identity",
                        "customer_iam.auth_identity.uq_auth_identity_subject",
                        "customer_iam.customer_account.ix_customer_account_status",
                        "customer_iam.customer_account.pk_customer_account",
                        "customer_iam.identity_profile.pk_identity_profile",
                        "customer_iam.identity_profile.uq_identity_profile_account",
                        "operations_iam.employee_account.ix_employee_auth_issuer_status",
                        "operations_iam.employee_account.ix_employee_enabled_admin",
                        "operations_iam.employee_account.pk_employee_account",
                        "operations_iam.employee_account.uq_employee_issuer_subject",
                        "operations_iam.employee_account.uq_employee_login_hmac",
                        "organization_access.organization.ix_organization_status",
                        "organization_access.organization.pk_organization",
                        "organization_access.organization_membership.ix_membership_account_status",
                        "organization_access.organization_membership.ix_membership_organization_status_role",
                        "organization_access.organization_membership.pk_organization_membership",
                        "organization_access.organization_membership.uq_membership_live");

        // Exact index contract (S1 fix 1, finding 1): whole PG16 indexdef equality for all
        // 18 V002 indexes binds schema/table, name, ordered columns/expression, uniqueness
        // and predicate. The name-list check above fails on missing/extra indexes; these
        // whole-string assertions fail on column-order, uniqueness or predicate changes,
        // including for both partial indexes (uq_membership_live is a UNIQUE partial index).
        Map<String, String> v002IndexDefs = new java.util.LinkedHashMap<>();
        v002IndexDefs.put("customer_iam.customer_account.pk_customer_account",
                "CREATE UNIQUE INDEX pk_customer_account ON customer_iam.customer_account "
                        + "USING btree (id)");
        v002IndexDefs.put("customer_iam.customer_account.ix_customer_account_status",
                "CREATE INDEX ix_customer_account_status ON customer_iam.customer_account "
                        + "USING btree (status)");
        v002IndexDefs.put("customer_iam.auth_identity.pk_auth_identity",
                "CREATE UNIQUE INDEX pk_auth_identity ON customer_iam.auth_identity "
                        + "USING btree (id)");
        v002IndexDefs.put("customer_iam.auth_identity.uq_auth_identity_subject",
                "CREATE UNIQUE INDEX uq_auth_identity_subject ON customer_iam.auth_identity "
                        + "USING btree (provider, provider_scope, provider_subject_hmac)");
        v002IndexDefs.put("customer_iam.auth_identity.ix_auth_identity_account_status",
                "CREATE INDEX ix_auth_identity_account_status ON customer_iam.auth_identity "
                        + "USING btree (account_id, status)");
        v002IndexDefs.put("customer_iam.identity_profile.pk_identity_profile",
                "CREATE UNIQUE INDEX pk_identity_profile ON customer_iam.identity_profile "
                        + "USING btree (id)");
        v002IndexDefs.put("customer_iam.identity_profile.uq_identity_profile_account",
                "CREATE UNIQUE INDEX uq_identity_profile_account ON customer_iam.identity_profile "
                        + "USING btree (account_id)");
        v002IndexDefs.put("organization_access.organization.pk_organization",
                "CREATE UNIQUE INDEX pk_organization ON organization_access.organization "
                        + "USING btree (id)");
        v002IndexDefs.put("organization_access.organization.ix_organization_status",
                "CREATE INDEX ix_organization_status ON organization_access.organization "
                        + "USING btree (status)");
        v002IndexDefs.put("organization_access.organization_membership.pk_organization_membership",
                "CREATE UNIQUE INDEX pk_organization_membership "
                        + "ON organization_access.organization_membership USING btree (id)");
        v002IndexDefs.put("organization_access.organization_membership.uq_membership_live",
                "CREATE UNIQUE INDEX uq_membership_live ON organization_access.organization_membership "
                        + "USING btree (organization_id, account_id) WHERE (status <> 'REVOKED'::text)");
        v002IndexDefs.put("organization_access.organization_membership.ix_membership_account_status",
                "CREATE INDEX ix_membership_account_status "
                        + "ON organization_access.organization_membership USING btree (account_id, status)");
        v002IndexDefs.put(
                "organization_access.organization_membership.ix_membership_organization_status_role",
                "CREATE INDEX ix_membership_organization_status_role "
                        + "ON organization_access.organization_membership "
                        + "USING btree (organization_id, status, role)");
        v002IndexDefs.put("operations_iam.employee_account.pk_employee_account",
                "CREATE UNIQUE INDEX pk_employee_account ON operations_iam.employee_account "
                        + "USING btree (id)");
        v002IndexDefs.put("operations_iam.employee_account.uq_employee_login_hmac",
                "CREATE UNIQUE INDEX uq_employee_login_hmac ON operations_iam.employee_account "
                        + "USING btree (login_name_hmac)");
        v002IndexDefs.put("operations_iam.employee_account.uq_employee_issuer_subject",
                "CREATE UNIQUE INDEX uq_employee_issuer_subject ON operations_iam.employee_account "
                        + "USING btree (auth_issuer, auth_subject_hmac)");
        v002IndexDefs.put("operations_iam.employee_account.ix_employee_enabled_admin",
                "CREATE INDEX ix_employee_enabled_admin ON operations_iam.employee_account "
                        + "USING btree (id) WHERE ((role = 'ADMIN'::text) AND (status = 'ACTIVE'::text))");
        v002IndexDefs.put("operations_iam.employee_account.ix_employee_auth_issuer_status",
                "CREATE INDEX ix_employee_auth_issuer_status ON operations_iam.employee_account "
                        + "USING btree (auth_issuer, status)");
        assertThat(v002IndexDefs).as("expected index map size").hasSize(18);
        for (Map.Entry<String, String> entry : v002IndexDefs.entrySet()) {
            String[] parts = entry.getKey().split("\\.", 3);
            List<String> actualDefs = queryStrings(
                    connection,
                    "SELECT indexdef FROM pg_indexes WHERE schemaname = ? AND tablename = ? AND indexname = ?",
                    parts[0], parts[1], parts[2]);
            assertThat(actualDefs)
                    .as("exactly one indexdef row for %s", entry.getKey())
                    .hasSize(1);
            assertThat(actualDefs.get(0))
                    .as("whole indexdef equality for %s", entry.getKey())
                    .isEqualTo(entry.getValue());
        }

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                        + "AND c.relkind = 'S'")).isZero();

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                        + "AND c.relkind = 'r'")).isEqualTo(6);

        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || r.relname || '=' || pg_get_userbyid(r.relowner) "
                        + "FROM pg_class r JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                        + "AND r.relkind = 'r' ORDER BY 1"))
                .containsExactly(
                        "customer_iam.auth_identity=customer_iam_owner",
                        "customer_iam.customer_account=customer_iam_owner",
                        "customer_iam.identity_profile=customer_iam_owner",
                        "operations_iam.employee_account=operations_iam_owner",
                        "organization_access.organization=organization_access_owner",
                        "organization_access.organization_membership=organization_access_owner");
    }

    /**
     * F-PA2-PR-01 fail-closed inventory: exact 34-name set per V002 table (4/7/7/4/4/8), zero extra
     * checks, and whole-definition equality of normalized {@code pg_get_constraintdef} output
     * against {@link #V002_CHECK_DEF_MAP}. Name-only, missing/extra or substring matches fail.
     */
    private static void assertV002CheckPredicateInventory(Connection connection) throws SQLException {
        int expectedCount = V002_CHECK_DEF_MAP.values().stream()
                .mapToInt(Map::size)
                .sum();
        assertThat(expectedCount).as("expected map size").isEqualTo(34);

        long actualCount = queryLong(
                connection,
                "SELECT count(*) FROM pg_constraint c "
                        + "JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname IN ('customer_iam','organization_access','operations_iam') "
                        + "AND c.contype = 'c'");
        assertThat(actualCount).as("total V002 contype='c' rows").isEqualTo(34);

        for (Map.Entry<String, Map<String, String>> tableEntry : V002_CHECK_DEF_MAP.entrySet()) {
            String qualifiedTable = tableEntry.getKey();
            int dot = qualifiedTable.indexOf('.');
            String schema = qualifiedTable.substring(0, dot);
            String table = qualifiedTable.substring(dot + 1);

            List<String> actualNames = queryStrings(
                    connection,
                    "SELECT c.conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = ? AND r.relname = ? AND c.contype = 'c' ORDER BY c.conname",
                    schema, table);
            List<String> expectedNames = new ArrayList<>(tableEntry.getValue().keySet());
            expectedNames.sort(String::compareTo);
            assertThat(actualNames)
                    .as("exact check-name set for %s", qualifiedTable)
                    .containsExactlyElementsOf(expectedNames);

            for (Map.Entry<String, String> checkEntry : tableEntry.getValue().entrySet()) {
                String checkName = checkEntry.getKey();
                List<String> actualDefs = queryStrings(
                        connection,
                        "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c "
                                + "JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = ? AND r.relname = ? AND c.contype = 'c' AND c.conname = ?",
                        schema, table, checkName);
                assertThat(actualDefs)
                        .as("exactly one row for %s.%s", qualifiedTable, checkName)
                        .hasSize(1);
                String normalizedActual = normalizeCheckDef(actualDefs.get(0));
                String normalizedExpected = normalizeCheckDef(checkEntry.getValue());
                assertThat(normalizedActual)
                        .as("whole pg_get_constraintdef equality for %s.%s", qualifiedTable, checkName)
                        .isEqualTo(normalizedExpected);
            }
        }
    }

    /**
     * ASCII-whitespace-only normalizer permitted by plan §8.2.1 / R-12: trim leading/trailing ASCII
     * whitespace and collapse each run of {@code [ \t\r\n]+} to one space. No other character,
     * token, keyword, case or parenthesization change is allowed; substring/contains/prefix/regex/
     * semantic equivalence matching is forbidden.
     */
    private static String normalizeCheckDef(String definition) {
        return definition.trim().replaceAll("[ \\t\\r\\n]+", " ");
    }

    private static void assertFunctionsAndTrigger(Connection connection) throws SQLException {
        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || p.proname FROM pg_proc p "
                        + "JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname <> 'information_schema' AND n.nspname NOT LIKE 'pg%' ORDER BY 1"))
                .containsExactly(
                        "audit.append_fact",
                        "audit.reject_row_mutation",
                        "catalog_order.guard_catalog_offer",
                        "catalog_order.guard_catalog_version_published",
                        "catalog_order.guard_customer_order_snapshot",
                        "catalog_order.guard_payment_order_identity",
                        "catalog_order.reject_row_mutation",
                        "customer_iam.guard_auth_identity",
                        "customer_iam.guard_customer_account",
                        "customer_iam.guard_identity_profile",
                        "ivr.guard_call_leg",
                        "ivr.guard_call_record_parties",
                        "ivr.guard_inbound_session",
                        "ivr.guard_outbound_call_task",
                        "ivr.reject_row_mutation",
                        "operations_iam.guard_employee_account",
                        "organization_access.guard_membership",
                        "organization_access.guard_organization",
                        "platform.idempotency_begin",
                        "platform.idempotency_complete",
                        "platform.outbox_append",
                        "subject_number.guard_caller_number_binding",
                        "subject_number.guard_subject_owner",
                        "subject_number.guard_subject_risk_state",
                        "subject_number.reject_row_mutation");

        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || p.proname || '(' || pg_get_function_identity_arguments(p.oid) || ')' "
                        + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access',"
                        + "'operations_iam','subject_number') "
                        + "ORDER BY 1"))
                .containsExactly(
                        "audit.append_fact(p_occurred_at timestamp with time zone, p_actor_type text, "
                                + "p_actor_id uuid, p_action_code text, p_object_type text, p_object_id uuid, "
                                + "p_reason_code text, p_before_snapshot jsonb, p_after_snapshot jsonb, "
                                + "p_request_id text, p_trace_id text)",
                        "audit.reject_row_mutation()",
                        "customer_iam.guard_auth_identity()",
                        "customer_iam.guard_customer_account()",
                        "customer_iam.guard_identity_profile()",
                        "operations_iam.guard_employee_account()",
                        "organization_access.guard_membership()",
                        "organization_access.guard_organization()",
                        "platform.idempotency_begin(p_scope text, p_key text, p_fingerprint text)",
                        "platform.idempotency_complete(p_record_id uuid, p_expected_revision integer, "
                                + "p_result_type text, p_result_id uuid, p_result_snapshot jsonb, "
                                + "p_result_sha256 text)",
                        "platform.outbox_append(p_event_domain text, p_dedupe_key text, "
                                + "p_aggregate_type text, p_aggregate_id uuid, p_event_type text, "
                                + "p_payload_redacted jsonb, p_payload_ciphertext text, "
                                + "p_payload_sha256 text)",
                        "subject_number.guard_caller_number_binding()",
                        "subject_number.guard_subject_owner()",
                        "subject_number.guard_subject_risk_state()",
                        "subject_number.reject_row_mutation()");

        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || p.proname || '|' || p.prosecdef || '|' "
                        + "|| pg_get_userbyid(p.proowner) || '|' || coalesce(array_to_string(p.proconfig, ','), '') "
                        + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access',"
                        + "'operations_iam','subject_number') "
                        + "ORDER BY 1"))
                .containsExactly(
                        "audit.append_fact|true|audit_owner|search_path=audit, pg_temp",
                        "audit.reject_row_mutation|false|audit_owner|search_path=audit, pg_temp",
                        "customer_iam.guard_auth_identity|false|customer_iam_owner|search_path=customer_iam, pg_temp",
                        "customer_iam.guard_customer_account|false|customer_iam_owner|search_path=customer_iam, pg_temp",
                        "customer_iam.guard_identity_profile|false|customer_iam_owner|search_path=customer_iam, pg_temp",
                        "operations_iam.guard_employee_account|false|operations_iam_owner|search_path=operations_iam, pg_temp",
                        "organization_access.guard_membership|false|organization_access_owner|search_path=organization_access, pg_temp",
                        "organization_access.guard_organization|false|organization_access_owner|search_path=organization_access, pg_temp",
                        "platform.idempotency_begin|true|platform_reliability_owner|search_path=platform, pg_temp",
                        "platform.idempotency_complete|true|platform_reliability_owner|search_path=platform, pg_temp",
                        "platform.outbox_append|true|platform_reliability_owner|search_path=platform, pg_temp",
                        "subject_number.guard_caller_number_binding|false|subject_number_owner|search_path=subject_number, pg_temp",
                        "subject_number.guard_subject_owner|false|subject_number_owner|search_path=subject_number, pg_temp",
                        "subject_number.guard_subject_risk_state|false|subject_number_owner|search_path=subject_number, pg_temp",
                        "subject_number.reject_row_mutation|false|subject_number_owner|search_path=subject_number, pg_temp");

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access',"
                        + "'operations_iam','subject_number') "
                        + "AND EXISTS (SELECT 1 FROM unnest(p.proargnames) AS a WHERE a = 'p_id')")).isZero();

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit') "
                        + "AND p.prosrc LIKE '%pg_catalog.gen_random_uuid()%'")).isEqualTo(3);

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access',"
                        + "'operations_iam','subject_number') "
                        + "AND p.prosrc LIKE '%gen_random_uuid%'")).isEqualTo(3);

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access',"
                        + "'operations_iam','subject_number') "
                        + "AND p.prosrc ~* '\\m(commit|rollback|start\\s+transaction|set\\s+transaction)\\M'"))
                .isZero();

        assertThat(queryLong(connection, "SELECT count(*) FROM pg_proc WHERE proname = 'issue_first_trial'"))
                .isZero();

        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || c.relname || '.' || t.tgname FROM pg_trigger t "
                        + "JOIN pg_class c ON c.oid = t.tgrelid "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE NOT t.tgisinternal AND n.nspname <> 'information_schema' "
                        + "AND n.nspname NOT LIKE 'pg%' ORDER BY 1"))
                .containsExactly(
                        "audit.audit_fact.trg_audit_fact_immutable",
                        "catalog_order.catalog_benefit_line.trg_catalog_benefit_line_immutable",
                        "catalog_order.catalog_offer.trg_catalog_offer_guard",
                        "catalog_order.catalog_product.trg_catalog_product_immutable",
                        "catalog_order.catalog_version.trg_catalog_version_guard",
                        "catalog_order.customer_order.trg_customer_order_guard",
                        "catalog_order.payment_event.trg_payment_event_immutable",
                        "catalog_order.payment_order.trg_payment_order_guard",
                        "customer_iam.auth_identity.trg_auth_identity_guard",
                        "customer_iam.customer_account.trg_customer_account_guard",
                        "customer_iam.identity_profile.trg_identity_profile_guard",
                        "ivr.call_leg.trg_call_leg_guard",
                        "ivr.call_record.trg_call_record_guard",
                        "ivr.inbound_session.trg_inbound_session_guard",
                        "ivr.ivr_decision.trg_ivr_decision_immutable",
                        "ivr.outbound_call_task.trg_outbound_call_task_guard",
                        "operations_iam.employee_account.trg_employee_account_guard",
                        "organization_access.organization.trg_organization_guard",
                        "organization_access.organization_membership.trg_membership_guard",
                        "subject_number.business_subject.trg_business_subject_guard",
                        "subject_number.caller_number_binding.trg_caller_number_binding_guard",
                        "subject_number.subject_claim.trg_subject_claim_immutable",
                        "subject_number.subject_risk_state.trg_subject_risk_state_guard");

        Map<String, String> v002TriggerFunctionByTrigger = Map.of(
                "trg_customer_account_guard", "customer_iam.guard_customer_account()",
                "trg_auth_identity_guard", "customer_iam.guard_auth_identity()",
                "trg_identity_profile_guard", "customer_iam.guard_identity_profile()",
                "trg_organization_guard", "organization_access.guard_organization()",
                "trg_membership_guard", "organization_access.guard_membership()",
                "trg_employee_account_guard", "operations_iam.guard_employee_account()",
                "trg_business_subject_guard", "subject_number.guard_subject_owner()",
                "trg_caller_number_binding_guard", "subject_number.guard_caller_number_binding()",
                "trg_subject_risk_state_guard", "subject_number.guard_subject_risk_state()");
        for (Map.Entry<String, String> entry : v002TriggerFunctionByTrigger.entrySet()) {
            try (PreparedStatement prepared = connection.prepareStatement(
                    "SELECT (t.tgtype & 2) <> 0 AS is_before, "
                            + "(t.tgtype & 1) <> 0 AS is_row, "
                            + "(t.tgtype & 16) <> 0 AS has_update, "
                            + "(t.tgtype & 8) <> 0 AS has_delete, "
                            + "(t.tgtype & 4) <> 0 AS has_insert, "
                            + "(t.tgtype & 32) <> 0 AS has_truncate, "
                            + "t.tgfoid = ?::regprocedure AS calls_expected_function "
                            + "FROM pg_trigger t WHERE t.tgname = ? AND NOT t.tgisinternal")) {
                prepared.setString(1, entry.getValue());
                prepared.setString(2, entry.getKey());
                try (ResultSet resultSet = prepared.executeQuery()) {
                    assertThat(resultSet.next()).as("trigger %s exists", entry.getKey()).isTrue();
                    assertThat(resultSet.getBoolean("is_before")).as("%s BEFORE", entry.getKey()).isTrue();
                    assertThat(resultSet.getBoolean("is_row")).as("%s FOR EACH ROW", entry.getKey()).isTrue();
                    assertThat(resultSet.getBoolean("has_update")).as("%s fires on UPDATE", entry.getKey()).isTrue();
                    assertThat(resultSet.getBoolean("has_delete")).as("%s no DELETE", entry.getKey()).isFalse();
                    assertThat(resultSet.getBoolean("has_insert")).as("%s no INSERT", entry.getKey()).isFalse();
                    assertThat(resultSet.getBoolean("has_truncate")).as("%s no TRUNCATE", entry.getKey()).isFalse();
                    assertThat(resultSet.getBoolean("calls_expected_function"))
                            .as("%s calls %s", entry.getKey(), entry.getValue()).isTrue();
                    assertThat(resultSet.next()).isFalse();
                }
            }
        }

        try (PreparedStatement prepared = connection.prepareStatement(
                "SELECT t.tgrelid = 'audit.audit_fact'::regclass AS on_audit_fact, "
                        + "t.tgfoid = 'audit.reject_row_mutation()'::regprocedure AS calls_reject_row_mutation, "
                        + "(t.tgtype & 2) <> 0 AS is_before, "
                        + "(t.tgtype & 1) <> 0 AS is_row, "
                        + "(t.tgtype & 16) <> 0 AS has_update, "
                        + "(t.tgtype & 8) <> 0 AS has_delete, "
                        + "(t.tgtype & 4) <> 0 AS has_insert, "
                        + "(t.tgtype & 32) <> 0 AS has_truncate "
                        + "FROM pg_trigger t "
                        + "WHERE t.tgname = 'trg_audit_fact_immutable' AND NOT t.tgisinternal")) {
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).as("exactly one immutable audit trigger").isTrue();
                assertThat(resultSet.getBoolean("on_audit_fact"))
                        .as("trigger must be on audit.audit_fact").isTrue();
                assertThat(resultSet.getBoolean("calls_reject_row_mutation"))
                        .as("trigger must call audit.reject_row_mutation()").isTrue();
                assertThat(resultSet.getBoolean("is_before"))
                        .as("trigger must be BEFORE").isTrue();
                assertThat(resultSet.getBoolean("is_row"))
                        .as("trigger must be FOR EACH ROW").isTrue();
                assertThat(resultSet.getBoolean("has_update"))
                        .as("trigger must fire on UPDATE").isTrue();
                assertThat(resultSet.getBoolean("has_delete"))
                        .as("trigger must fire on DELETE").isTrue();
                assertThat(resultSet.getBoolean("has_insert"))
                        .as("trigger must not fire on INSERT").isFalse();
                assertThat(resultSet.getBoolean("has_truncate"))
                        .as("trigger must not fire on TRUNCATE").isFalse();
                assertThat(resultSet.next()).isFalse();
            }
        }

        try (PreparedStatement prepared = connection.prepareStatement(
                "SELECT t.tgrelid = 'subject_number.subject_claim'::regclass AS on_subject_claim, "
                        + "t.tgfoid = 'subject_number.reject_row_mutation()'::regprocedure "
                        + "AS calls_reject_row_mutation, "
                        + "(t.tgtype & 2) <> 0 AS is_before, "
                        + "(t.tgtype & 1) <> 0 AS is_row, "
                        + "(t.tgtype & 16) <> 0 AS has_update, "
                        + "(t.tgtype & 8) <> 0 AS has_delete, "
                        + "(t.tgtype & 4) <> 0 AS has_insert, "
                        + "(t.tgtype & 32) <> 0 AS has_truncate "
                        + "FROM pg_trigger t "
                        + "WHERE t.tgname = 'trg_subject_claim_immutable' AND NOT t.tgisinternal")) {
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).as("exactly one append-only claim trigger").isTrue();
                assertThat(resultSet.getBoolean("on_subject_claim"))
                        .as("trigger must be on subject_number.subject_claim").isTrue();
                assertThat(resultSet.getBoolean("calls_reject_row_mutation"))
                        .as("trigger must call subject_number.reject_row_mutation()").isTrue();
                assertThat(resultSet.getBoolean("is_before"))
                        .as("trigger must be BEFORE").isTrue();
                assertThat(resultSet.getBoolean("is_row"))
                        .as("trigger must be FOR EACH ROW").isTrue();
                assertThat(resultSet.getBoolean("has_update"))
                        .as("trigger must fire on UPDATE").isTrue();
                assertThat(resultSet.getBoolean("has_delete"))
                        .as("trigger must fire on DELETE").isTrue();
                assertThat(resultSet.getBoolean("has_insert"))
                        .as("trigger must not fire on INSERT").isFalse();
                assertThat(resultSet.getBoolean("has_truncate"))
                        .as("trigger must not fire on TRUNCATE").isFalse();
                assertThat(resultSet.next()).isFalse();
            }
        }
    }

    /**
     * F-PA3-PR-01 fail-closed inventory: exact 25-name set per V003 table (6/7/6/6), zero extra
     * checks, and whole-definition equality of normalized {@code pg_get_constraintdef} output
     * against {@link #V003_CHECK_DEF_MAP}. Name-only, missing/extra or substring matches fail.
     */
    private static void assertV003CheckPredicateInventory(Connection connection) throws SQLException {
        int expectedCount = V003_CHECK_DEF_MAP.values().stream()
                .mapToInt(Map::size)
                .sum();
        assertThat(expectedCount).as("expected V003 map size").isEqualTo(25);

        long actualCount = queryLong(
                connection,
                "SELECT count(*) FROM pg_constraint c "
                        + "JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'subject_number' AND c.contype = 'c'");
        assertThat(actualCount).as("total V003 contype='c' rows").isEqualTo(25);

        for (Map.Entry<String, Map<String, String>> tableEntry : V003_CHECK_DEF_MAP.entrySet()) {
            String table = tableEntry.getKey();
            List<String> actualNames = queryStrings(
                    connection,
                    "SELECT c.conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'subject_number' AND r.relname = ? AND c.contype = 'c' "
                            + "ORDER BY c.conname",
                    table);
            List<String> expectedNames = new ArrayList<>(tableEntry.getValue().keySet());
            expectedNames.sort(String::compareTo);
            assertThat(actualNames)
                    .as("exact check-name set for subject_number.%s", table)
                    .containsExactlyElementsOf(expectedNames);

            for (Map.Entry<String, String> checkEntry : tableEntry.getValue().entrySet()) {
                String checkName = checkEntry.getKey();
                List<String> actualDefs = queryStrings(
                        connection,
                        "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c "
                                + "JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = 'subject_number' AND r.relname = ? "
                                + "AND c.contype = 'c' AND c.conname = ?",
                        table, checkName);
                assertThat(actualDefs)
                        .as("exactly one row for subject_number.%s.%s", table, checkName)
                        .hasSize(1);
                assertThat(normalizeCheckDef(actualDefs.get(0)))
                        .as("whole pg_get_constraintdef equality for subject_number.%s.%s",
                                table, checkName)
                        .isEqualTo(normalizeCheckDef(checkEntry.getValue()));
            }
        }
    }

    private static void assertV003TablesColumnsChecksIndexesFks(Connection connection)
            throws SQLException {
        assertThat(queryStrings(
                connection,
                "SELECT table_schema || '.' || table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'subject_number' ORDER BY 1"))
                .containsExactlyElementsOf(V003_TABLES);

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'subject_number'"))
                .as("total V003 columns").isEqualTo(36);

        Map<String, String> v003ColumnOrder = new java.util.LinkedHashMap<>();
        v003ColumnOrder.put("business_subject",
                "id,owner_account_id,owner_organization_id,source,real_name_status,"
                        + "real_name_verified_at,revision,created_at,updated_at");
        v003ColumnOrder.put("caller_number_binding",
                "id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,"
                        + "status,verified_at,revision");
        v003ColumnOrder.put("subject_claim",
                "id,subject_id,account_id,organization_id,verification_method,"
                        + "verification_ref_ciphertext,request_id,status,risk_checked_at,verified_at");
        v003ColumnOrder.put("subject_risk_state",
                "id,caller_number_binding_id,risk_type,state,source,observed_at,cleared_at,revision");
        Map<String, List<String>> v003ColumnDetails = new java.util.LinkedHashMap<>();
        v003ColumnDetails.put("business_subject", List.of(
                "id|uuid|NO|<none>",
                "owner_account_id|uuid|YES|<none>",
                "owner_organization_id|uuid|YES|<none>",
                "source|text|NO|<none>",
                "real_name_status|text|NO|'UNVERIFIED'::text",
                "real_name_verified_at|timestamp with time zone|YES|<none>",
                "revision|integer|NO|1",
                "created_at|timestamp with time zone|NO|now()",
                "updated_at|timestamp with time zone|NO|now()"));
        v003ColumnDetails.put("caller_number_binding", List.of(
                "id|uuid|NO|<none>",
                "subject_id|uuid|NO|<none>",
                "number_type|text|NO|<none>",
                "number_ciphertext|text|NO|<none>",
                "number_hmac|text|NO|<none>",
                "number_masked|text|NO|<none>",
                "status|text|NO|'ACTIVE'::text",
                "verified_at|timestamp with time zone|YES|<none>",
                "revision|integer|NO|1"));
        v003ColumnDetails.put("subject_claim", List.of(
                "id|uuid|NO|<none>",
                "subject_id|uuid|NO|<none>",
                "account_id|uuid|NO|<none>",
                "organization_id|uuid|YES|<none>",
                "verification_method|text|NO|<none>",
                "verification_ref_ciphertext|text|NO|<none>",
                "request_id|text|NO|<none>",
                "status|text|NO|<none>",
                "risk_checked_at|timestamp with time zone|NO|<none>",
                "verified_at|timestamp with time zone|YES|<none>"));
        v003ColumnDetails.put("subject_risk_state", List.of(
                "id|uuid|NO|<none>",
                "caller_number_binding_id|uuid|NO|<none>",
                "risk_type|text|NO|<none>",
                "state|text|NO|'OPEN'::text",
                "source|text|NO|<none>",
                "observed_at|timestamp with time zone|NO|now()",
                "cleared_at|timestamp with time zone|YES|<none>",
                "revision|integer|NO|1"));
        for (Map.Entry<String, String> entry : v003ColumnOrder.entrySet()) {
            assertThat(queryStrings(
                    connection,
                    "SELECT string_agg(column_name, ',' ORDER BY ordinal_position) FROM information_schema.columns "
                            + "WHERE table_schema = 'subject_number' AND table_name = ?",
                    entry.getKey()))
                    .as("columns of subject_number.%s", entry.getKey())
                    .containsExactly(entry.getValue());
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM information_schema.columns "
                            + "WHERE table_schema = 'subject_number' AND table_name = ?",
                    entry.getKey()))
                    .as("column count of subject_number.%s", entry.getKey())
                    .isEqualTo(entry.getValue().split(",").length);
            assertThat(queryStrings(
                    connection,
                    "SELECT column_name || '|' || data_type || '|' || is_nullable || '|' "
                            + "|| coalesce(column_default, '<none>') FROM information_schema.columns "
                            + "WHERE table_schema = 'subject_number' AND table_name = ? ORDER BY ordinal_position",
                    entry.getKey()))
                    .as("exact column rows of subject_number.%s", entry.getKey())
                    .containsExactlyElementsOf(v003ColumnDetails.get(entry.getKey()));
        }
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_attrdef d "
                        + "JOIN pg_attribute a ON a.attrelid = d.adrelid AND a.attnum = d.adnum "
                        + "JOIN pg_class r ON r.oid = d.adrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'subject_number'"))
                .as("total V003 defaulted columns")
                .isEqualTo(9);

        Map<String, List<String>> v003CheckNames = new java.util.LinkedHashMap<>();
        v003CheckNames.put("business_subject", List.of(
                "ck_subject_real_name_shape",
                "ck_subject_real_name_status",
                "ck_subject_revision",
                "ck_subject_single_owner",
                "ck_subject_source",
                "ck_subject_time_order"));
        v003CheckNames.put("caller_number_binding", List.of(
                "ck_binding_number_ciphertext",
                "ck_binding_number_hmac",
                "ck_binding_number_masked",
                "ck_binding_number_type",
                "ck_binding_revision",
                "ck_binding_status",
                "ck_binding_verified_shape"));
        v003CheckNames.put("subject_claim", List.of(
                "ck_claim_request_id",
                "ck_claim_status",
                "ck_claim_verification_method",
                "ck_claim_verification_ref",
                "ck_claim_verified_after_risk",
                "ck_claim_verified_shape"));
        v003CheckNames.put("subject_risk_state", List.of(
                "ck_risk_cleared_after_observed",
                "ck_risk_cleared_shape",
                "ck_risk_revision",
                "ck_risk_source",
                "ck_risk_state",
                "ck_risk_type"));
        for (Map.Entry<String, List<String>> entry : v003CheckNames.entrySet()) {
            assertThat(queryStrings(
                    connection,
                    "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'subject_number' AND r.relname = ? AND c.contype = 'c' "
                            + "ORDER BY conname",
                    entry.getKey()))
                    .as("check names of subject_number.%s", entry.getKey())
                    .containsExactlyElementsOf(entry.getValue());
        }

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'subject_number' AND c.contype = 'p' ORDER BY 1"))
                .containsExactly(
                        "pk_business_subject",
                        "pk_caller_number_binding",
                        "pk_subject_claim",
                        "pk_subject_risk_state");
        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'subject_number' AND c.contype = 'u' ORDER BY 1"))
                .containsExactly(
                        "uq_binding_subject_number",
                        "uq_claim_request",
                        "uq_risk_binding_type");

        Map<String, String[]> v003PkUniqueDefs = new java.util.LinkedHashMap<>();
        v003PkUniqueDefs.put("business_subject.pk_business_subject", new String[] {
                "business_subject", "pk_business_subject", "p", "PRIMARY KEY (id)", "id"});
        v003PkUniqueDefs.put("caller_number_binding.pk_caller_number_binding", new String[] {
                "caller_number_binding", "pk_caller_number_binding", "p", "PRIMARY KEY (id)", "id"});
        v003PkUniqueDefs.put("caller_number_binding.uq_binding_subject_number", new String[] {
                "caller_number_binding", "uq_binding_subject_number", "u",
                "UNIQUE (subject_id, number_hmac)", "subject_id,number_hmac"});
        v003PkUniqueDefs.put("subject_claim.pk_subject_claim", new String[] {
                "subject_claim", "pk_subject_claim", "p", "PRIMARY KEY (id)", "id"});
        v003PkUniqueDefs.put("subject_claim.uq_claim_request", new String[] {
                "subject_claim", "uq_claim_request", "u", "UNIQUE (request_id)", "request_id"});
        v003PkUniqueDefs.put("subject_risk_state.pk_subject_risk_state", new String[] {
                "subject_risk_state", "pk_subject_risk_state", "p", "PRIMARY KEY (id)", "id"});
        v003PkUniqueDefs.put("subject_risk_state.uq_risk_binding_type", new String[] {
                "subject_risk_state", "uq_risk_binding_type", "u",
                "UNIQUE (caller_number_binding_id, risk_type)",
                "caller_number_binding_id,risk_type"});
        assertThat(v003PkUniqueDefs).as("expected V003 PK/unique map size").hasSize(7);
        for (Map.Entry<String, String[]> entry : v003PkUniqueDefs.entrySet()) {
            String[] expected = entry.getValue();
            List<String> actualDefs = queryStrings(
                    connection,
                    "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c "
                            + "JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'subject_number' AND r.relname = ? AND c.conname = ? "
                            + "AND c.contype::text = ?",
                    expected[0], expected[1], expected[2]);
            assertThat(actualDefs)
                    .as("exactly one %s row for subject_number.%s", expected[2], entry.getKey())
                    .hasSize(1);
            assertThat(actualDefs.get(0))
                    .as("whole pg_get_constraintdef equality for subject_number.%s", entry.getKey())
                    .isEqualTo(expected[3]);
            assertThat(queryStrings(
                    connection,
                    "SELECT a.attname FROM pg_constraint c "
                            + "JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "JOIN unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord) ON true "
                            + "JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum "
                            + "WHERE n.nspname = 'subject_number' AND r.relname = ? AND c.conname = ? "
                            + "AND c.contype::text = ? ORDER BY k.ord",
                    expected[0], expected[1], expected[2]))
                    .as("ordered key columns of subject_number.%s", entry.getKey())
                    .containsExactlyElementsOf(List.of(expected[4].split(",")));
        }

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'subject_number' AND c.contype = 'f' ORDER BY conname"))
                .containsExactly(
                        "fk_binding_subject",
                        "fk_claim_account",
                        "fk_claim_organization",
                        "fk_claim_subject",
                        "fk_risk_binding",
                        "fk_subject_owner_account",
                        "fk_subject_owner_organization");

        Map<String, String> v003FkDefs = new java.util.LinkedHashMap<>();
        v003FkDefs.put("fk_subject_owner_account",
                "FOREIGN KEY (owner_account_id) REFERENCES customer_iam.customer_account(id) "
                        + "ON UPDATE RESTRICT ON DELETE RESTRICT");
        v003FkDefs.put("fk_subject_owner_organization",
                "FOREIGN KEY (owner_organization_id) REFERENCES organization_access.organization(id) "
                        + "ON UPDATE RESTRICT ON DELETE RESTRICT");
        v003FkDefs.put("fk_binding_subject",
                "FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id) "
                        + "ON UPDATE RESTRICT ON DELETE RESTRICT");
        v003FkDefs.put("fk_claim_subject",
                "FOREIGN KEY (subject_id) REFERENCES subject_number.business_subject(id) "
                        + "ON UPDATE RESTRICT ON DELETE RESTRICT");
        v003FkDefs.put("fk_claim_account",
                "FOREIGN KEY (account_id) REFERENCES customer_iam.customer_account(id) "
                        + "ON UPDATE RESTRICT ON DELETE RESTRICT");
        v003FkDefs.put("fk_claim_organization",
                "FOREIGN KEY (organization_id) REFERENCES organization_access.organization(id) "
                        + "ON UPDATE RESTRICT ON DELETE RESTRICT");
        v003FkDefs.put("fk_risk_binding",
                "FOREIGN KEY (caller_number_binding_id) "
                        + "REFERENCES subject_number.caller_number_binding(id) "
                        + "ON UPDATE RESTRICT ON DELETE RESTRICT");
        for (Map.Entry<String, String> entry : v003FkDefs.entrySet()) {
            assertThat(queryString(
                    connection,
                    "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c "
                            + "JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'subject_number' AND c.conname = ?",
                    entry.getKey()))
                    .as("FK def %s", entry.getKey())
                    .isEqualTo(entry.getValue());
            assertThat(queryStrings(
                    connection,
                    "SELECT c.confupdtype::text || c.confdeltype::text || c.confmatchtype::text || '|' "
                            + "|| c.condeferrable::text || '|' || c.condeferred::text || '|' || c.convalidated::text "
                            + "FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'subject_number' AND c.conname = ?",
                    entry.getKey()))
                    .as("FK properties %s", entry.getKey())
                    .containsExactly("rrs|false|false|true");
        }

        assertThat(queryStrings(
                connection,
                "SELECT schemaname || '.' || tablename || '.' || indexname FROM pg_indexes "
                        + "WHERE schemaname = 'subject_number' ORDER BY 1"))
                .containsExactly(
                        "subject_number.business_subject.ix_subject_owner_account",
                        "subject_number.business_subject.ix_subject_owner_organization",
                        "subject_number.business_subject.ix_subject_source_status",
                        "subject_number.business_subject.pk_business_subject",
                        "subject_number.caller_number_binding.ix_binding_hmac_status",
                        "subject_number.caller_number_binding.ix_binding_subject_status",
                        "subject_number.caller_number_binding.pk_caller_number_binding",
                        "subject_number.caller_number_binding.uq_binding_active_number",
                        "subject_number.caller_number_binding.uq_binding_subject_number",
                        "subject_number.subject_claim.ix_claim_account_status",
                        "subject_number.subject_claim.ix_claim_subject_status_time",
                        "subject_number.subject_claim.pk_subject_claim",
                        "subject_number.subject_claim.uq_claim_accepted_subject_account",
                        "subject_number.subject_claim.uq_claim_request",
                        "subject_number.subject_risk_state.ix_risk_binding_state",
                        "subject_number.subject_risk_state.ix_risk_open",
                        "subject_number.subject_risk_state.pk_subject_risk_state",
                        "subject_number.subject_risk_state.uq_risk_binding_type");

        Map<String, String> v003IndexDefs = new java.util.LinkedHashMap<>();
        v003IndexDefs.put("business_subject.pk_business_subject",
                "CREATE UNIQUE INDEX pk_business_subject ON subject_number.business_subject "
                        + "USING btree (id)");
        v003IndexDefs.put("business_subject.ix_subject_owner_account",
                "CREATE INDEX ix_subject_owner_account ON subject_number.business_subject "
                        + "USING btree (owner_account_id)");
        v003IndexDefs.put("business_subject.ix_subject_owner_organization",
                "CREATE INDEX ix_subject_owner_organization ON subject_number.business_subject "
                        + "USING btree (owner_organization_id)");
        v003IndexDefs.put("business_subject.ix_subject_source_status",
                "CREATE INDEX ix_subject_source_status ON subject_number.business_subject "
                        + "USING btree (source, real_name_status)");
        v003IndexDefs.put("caller_number_binding.pk_caller_number_binding",
                "CREATE UNIQUE INDEX pk_caller_number_binding ON subject_number.caller_number_binding "
                        + "USING btree (id)");
        v003IndexDefs.put("caller_number_binding.uq_binding_subject_number",
                "CREATE UNIQUE INDEX uq_binding_subject_number ON subject_number.caller_number_binding "
                        + "USING btree (subject_id, number_hmac)");
        v003IndexDefs.put("caller_number_binding.uq_binding_active_number",
                "CREATE UNIQUE INDEX uq_binding_active_number ON subject_number.caller_number_binding "
                        + "USING btree (number_hmac) WHERE (status = 'ACTIVE'::text)");
        v003IndexDefs.put("caller_number_binding.ix_binding_subject_status",
                "CREATE INDEX ix_binding_subject_status ON subject_number.caller_number_binding "
                        + "USING btree (subject_id, status)");
        v003IndexDefs.put("caller_number_binding.ix_binding_hmac_status",
                "CREATE INDEX ix_binding_hmac_status ON subject_number.caller_number_binding "
                        + "USING btree (number_hmac, status)");
        v003IndexDefs.put("subject_claim.pk_subject_claim",
                "CREATE UNIQUE INDEX pk_subject_claim ON subject_number.subject_claim "
                        + "USING btree (id)");
        v003IndexDefs.put("subject_claim.uq_claim_request",
                "CREATE UNIQUE INDEX uq_claim_request ON subject_number.subject_claim "
                        + "USING btree (request_id)");
        v003IndexDefs.put("subject_claim.uq_claim_accepted_subject_account",
                "CREATE UNIQUE INDEX uq_claim_accepted_subject_account "
                        + "ON subject_number.subject_claim "
                        + "USING btree (subject_id, account_id) WHERE (status = 'ACCEPTED'::text)");
        v003IndexDefs.put("subject_claim.ix_claim_subject_status_time",
                "CREATE INDEX ix_claim_subject_status_time ON subject_number.subject_claim "
                        + "USING btree (subject_id, status, risk_checked_at DESC)");
        v003IndexDefs.put("subject_claim.ix_claim_account_status",
                "CREATE INDEX ix_claim_account_status ON subject_number.subject_claim "
                        + "USING btree (account_id, status)");
        v003IndexDefs.put("subject_risk_state.pk_subject_risk_state",
                "CREATE UNIQUE INDEX pk_subject_risk_state ON subject_number.subject_risk_state "
                        + "USING btree (id)");
        v003IndexDefs.put("subject_risk_state.uq_risk_binding_type",
                "CREATE UNIQUE INDEX uq_risk_binding_type ON subject_number.subject_risk_state "
                        + "USING btree (caller_number_binding_id, risk_type)");
        v003IndexDefs.put("subject_risk_state.ix_risk_open",
                "CREATE INDEX ix_risk_open ON subject_number.subject_risk_state "
                        + "USING btree (caller_number_binding_id, observed_at) "
                        + "WHERE (state = 'OPEN'::text)");
        v003IndexDefs.put("subject_risk_state.ix_risk_binding_state",
                "CREATE INDEX ix_risk_binding_state ON subject_number.subject_risk_state "
                        + "USING btree (caller_number_binding_id, state)");
        assertThat(v003IndexDefs).as("expected V003 index map size").hasSize(18);
        for (Map.Entry<String, String> entry : v003IndexDefs.entrySet()) {
            String[] parts = entry.getKey().split("\\.", 2);
            List<String> actualDefs = queryStrings(
                    connection,
                    "SELECT indexdef FROM pg_indexes WHERE schemaname = 'subject_number' "
                            + "AND tablename = ? AND indexname = ?",
                    parts[0], parts[1]);
            assertThat(actualDefs)
                    .as("exactly one indexdef row for subject_number.%s", entry.getKey())
                    .hasSize(1);
            assertThat(actualDefs.get(0))
                    .as("whole indexdef equality for subject_number.%s", entry.getKey())
                    .isEqualTo(entry.getValue());
        }

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'subject_number' AND c.relkind = 'S'"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'subject_number' AND c.relkind = 'r'"))
                .isEqualTo(4);

        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || r.relname || '=' || pg_get_userbyid(r.relowner) "
                        + "FROM pg_class r JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'subject_number' AND r.relkind = 'r' ORDER BY 1"))
                .containsExactly(
                        "subject_number.business_subject=subject_number_owner",
                        "subject_number.caller_number_binding=subject_number_owner",
                        "subject_number.subject_claim=subject_number_owner",
                        "subject_number.subject_risk_state=subject_number_owner");
    }

    private static void assertV003FunctionsAndTriggersExact(Connection connection) throws SQLException {
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'subject_number'"))
                .as("exactly four subject_number functions").isEqualTo(4);
        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || p.proname || '(' || pg_get_function_identity_arguments(p.oid) || ')' "
                        + "|| '|' || p.prosecdef || '|' || pg_get_userbyid(p.proowner) || '|' "
                        + "|| coalesce(array_to_string(p.proconfig, ','), '') "
                        + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'subject_number' ORDER BY 1"))
                .containsExactly(
                        "subject_number.guard_caller_number_binding()|false|subject_number_owner"
                                + "|search_path=subject_number, pg_temp",
                        "subject_number.guard_subject_owner()|false|subject_number_owner"
                                + "|search_path=subject_number, pg_temp",
                        "subject_number.guard_subject_risk_state()|false|subject_number_owner"
                                + "|search_path=subject_number, pg_temp",
                        "subject_number.reject_row_mutation()|false|subject_number_owner"
                                + "|search_path=subject_number, pg_temp");
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'subject_number' "
                        + "AND EXISTS (SELECT 1 FROM unnest(p.proargnames) AS a WHERE a = 'p_id')"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'subject_number' "
                        + "AND p.prosrc LIKE '%gen_random_uuid%'"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'subject_number' "
                        + "AND p.prosrc ~* '\\m(commit|rollback|start\\s+transaction|set\\s+transaction)\\M'"))
                .isZero();

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE NOT t.tgisinternal AND n.nspname = 'subject_number'"))
                .as("exactly four subject_number triggers").isEqualTo(4);

        Map<String, String[]> v003Triggers = new java.util.LinkedHashMap<>();
        // expected: [0]=table, [1]=function, [2]=before, [3]=row, [4]=update, [5]=delete
        v003Triggers.put("trg_business_subject_guard", new String[] {
                "subject_number.business_subject", "subject_number.guard_subject_owner()",
                "1", "1", "1", "0"});
        v003Triggers.put("trg_caller_number_binding_guard", new String[] {
                "subject_number.caller_number_binding", "subject_number.guard_caller_number_binding()",
                "1", "1", "1", "0"});
        v003Triggers.put("trg_subject_risk_state_guard", new String[] {
                "subject_number.subject_risk_state", "subject_number.guard_subject_risk_state()",
                "1", "1", "1", "0"});
        v003Triggers.put("trg_subject_claim_immutable", new String[] {
                "subject_number.subject_claim", "subject_number.reject_row_mutation()",
                "1", "1", "1", "1"});
        for (Map.Entry<String, String[]> entry : v003Triggers.entrySet()) {
            String[] expected = entry.getValue();
            try (PreparedStatement prepared = connection.prepareStatement(
                    "SELECT t.tgrelid = ?::regclass AS on_table, "
                            + "t.tgfoid = ?::regprocedure AS calls_function, "
                            + "(t.tgtype & 2) <> 0 AS is_before, "
                            + "(t.tgtype & 1) <> 0 AS is_row, "
                            + "(t.tgtype & 16) <> 0 AS has_update, "
                            + "(t.tgtype & 8) <> 0 AS has_delete, "
                            + "(t.tgtype & 4) <> 0 AS has_insert, "
                            + "(t.tgtype & 32) <> 0 AS has_truncate "
                            + "FROM pg_trigger t WHERE t.tgname = ? AND NOT t.tgisinternal")) {
                prepared.setString(1, expected[0]);
                prepared.setString(2, expected[1]);
                prepared.setString(3, entry.getKey());
                try (ResultSet resultSet = prepared.executeQuery()) {
                    assertThat(resultSet.next()).as("trigger %s exists", entry.getKey()).isTrue();
                    assertThat(resultSet.getBoolean("on_table"))
                            .as("%s on %s", entry.getKey(), expected[0]).isTrue();
                    assertThat(resultSet.getBoolean("calls_function"))
                            .as("%s calls %s", entry.getKey(), expected[1]).isTrue();
                    assertThat(resultSet.getBoolean("is_before"))
                            .as("%s BEFORE", entry.getKey())
                            .isEqualTo("1".equals(expected[2]));
                    assertThat(resultSet.getBoolean("is_row"))
                            .as("%s FOR EACH ROW", entry.getKey())
                            .isEqualTo("1".equals(expected[3]));
                    assertThat(resultSet.getBoolean("has_update"))
                            .as("%s fires on UPDATE", entry.getKey())
                            .isEqualTo("1".equals(expected[4]));
                    assertThat(resultSet.getBoolean("has_delete"))
                            .as("%s fires on DELETE", entry.getKey())
                            .isEqualTo("1".equals(expected[5]));
                    assertThat(resultSet.getBoolean("has_insert"))
                            .as("%s no INSERT", entry.getKey()).isFalse();
                    assertThat(resultSet.getBoolean("has_truncate"))
                            .as("%s no TRUNCATE", entry.getKey()).isFalse();
                    assertThat(resultSet.next()).isFalse();
                }
            }
        }
    }

    private static void assertV003NoIdDefaultsOrSequences(Connection connection) throws SQLException {
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_attrdef d "
                        + "JOIN pg_attribute a ON a.attrelid = d.adrelid AND a.attnum = d.adnum "
                        + "JOIN pg_class r ON r.oid = d.adrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'subject_number' AND a.attname = 'id'"))
                .as("no V003 id column has a default").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'subject_number' AND c.relkind = 'S'"))
                .as("no V003 sequence exists").isZero();
    }

    private static void assertNoV006PlusObjects(Connection connection) throws SQLException {
        assertThat(queryStrings(
                connection,
                "SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND c.relkind = 'r' ORDER BY 1"))
                .containsExactlyElementsOf(List.of(
                        "call_event", "call_leg", "call_record", "inbound_session",
                        "ivr_decision", "outbound_call_task"));
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname IN ('entitlement_metering','activation_consent') "
                        + "AND c.relkind = 'r'"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_namespace WHERE nspname IN ('evidence','sharing')"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_roles WHERE rolname IN "
                        + "('evidence_owner','evidence_role','sharing_owner','sharing_role')"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc WHERE proname = 'issue_first_trial'"))
                .isZero();
        assertThat(queryStrings(
                connection,
                "SELECT proname FROM pg_proc WHERE proname IN "
                        + "('select_call_action','record_activation_next_action')"))
                .isEmpty();
        assertThat(toRegclass(connection, "ivr.call_task")).isNull();
        assertThat(toRegclass(connection, "ivr.call_session")).isNull();
        for (String v006Name : List.of(
                "trial_policy_version", "entitlement_grant", "trial_grant",
                "entitlement_balance", "entitlement_issuance", "usage_reservation", "usage_fact",
                "consent_record", "service_activation", "service_activation_transition",
                "media_object", "recording", "evidence_record", "evidence_artifact",
                "evidence_event", "evidence_share_grant", "share_admission", "share_access_event")) {
            assertThat(toRegclass(connection, "activation_consent." + v006Name)).isNull();
            assertThat(toRegclass(connection, "entitlement_metering." + v006Name)).isNull();
            assertThat(toRegclass(connection, "evidence." + v006Name)).isNull();
            assertThat(toRegclass(connection, "sharing." + v006Name)).isNull();
        }
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_constraint c "
                        + "JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND c.conname = 'fk_ivr_decision_reservation'"))
                .isZero();
        assertThat(queryStrings(
                connection,
                "SELECT a.attname FROM pg_attribute a "
                        + "JOIN pg_class c ON c.oid = a.attrelid "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND c.relname = 'ivr_decision' "
                        + "AND a.attname = 'reservation_id' AND a.attnotnull"))
                .isEmpty();
    }

    private static void assertV005TablesColumnsChecksIndexesFks(Connection connection)
            throws SQLException {
        assertThat(queryStrings(
                connection,
                "SELECT table_schema || '.' || table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'ivr' ORDER BY 1"))
                .containsExactlyElementsOf(V005_TABLES);
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'ivr'"))
                .as("total V005 columns").isEqualTo(103);

        Map<String, String> v005ColumnOrder = new java.util.LinkedHashMap<>();
        v005ColumnOrder.put("outbound_call_task",
                "id,task_id,caller_binding_id,subject_id,owner_account_id,owner_organization_id,"
                        + "created_by_account_id,caller_number_hmac,callee_ciphertext,callee_masked,"
                        + "business_type,status,reason_code,ttl_seconds,expires_at,idempotency_key,"
                        + "request_fingerprint,claimed_call_id,claimed_at,terminal_at,created_at,"
                        + "updated_at,revision");
        v005ColumnOrder.put("inbound_session",
                "id,call_id,subject_id,trusted_caller,caller_ciphertext,caller_number_hmac,"
                        + "caller_masked,access_number_masked,node_id,protocol_version,prompt_version,"
                        + "status,confirmed_subject_id,guidance_status,created_at,confirmed_at,revision");
        v005ColumnOrder.put("call_record",
                "id,call_id,session_id,task_id,subject_id,caller_ciphertext,caller_number_hmac,"
                        + "caller_masked,callee_ciphertext,callee_masked,access_number_masked,status,"
                        + "node_id,protocol_version,started_at,bridged_at,recording_started_at,"
                        + "recording_ended_at,ended_at,authoritative_seconds,end_reason,created_at,revision");
        v005ColumnOrder.put("call_leg",
                "id,call_id,leg_id,leg_type,direction,state,started_at,answered_at,ended_at,"
                        + "end_reason,revision");
        v005ColumnOrder.put("call_event",
                "id,call_id,event_domain,event_id,sequence_no,event_type,occurred_at,received_at,"
                        + "payload_ciphertext,payload_redacted,payload_sha256,processing_status,"
                        + "reason_code,revision");
        v005ColumnOrder.put("ivr_decision",
                "id,session_id,call_id,purpose,source_event_domain,source_event_id,"
                        + "request_fingerprint,action,reason_code,response_snapshot,"
                        + "response_payload_ciphertext,response_sha256,key_version,reservation_id,decided_at");
        for (Map.Entry<String, String> entry : v005ColumnOrder.entrySet()) {
            assertThat(queryStrings(
                    connection,
                    "SELECT column_name FROM information_schema.columns "
                            + "WHERE table_schema = 'ivr' AND table_name = ? "
                            + "ORDER BY ordinal_position",
                    entry.getKey()))
                    .as("ordered columns for ivr.%s", entry.getKey())
                    .containsExactlyElementsOf(List.of(entry.getValue().split(",")));
        }

        Map<String, List<String>> v005ColumnDetails = new java.util.LinkedHashMap<>();
        v005ColumnDetails.put("outbound_call_task", List.of(
                "id|uuid|NO|<none>",
                "task_id|text|NO|<none>",
                "caller_binding_id|uuid|NO|<none>",
                "subject_id|uuid|NO|<none>",
                "owner_account_id|uuid|YES|<none>",
                "owner_organization_id|uuid|YES|<none>",
                "created_by_account_id|uuid|NO|<none>",
                "caller_number_hmac|text|NO|<none>",
                "callee_ciphertext|text|NO|<none>",
                "callee_masked|text|NO|<none>",
                "business_type|text|NO|<none>",
                "status|text|NO|'PENDING'::text",
                "reason_code|text|YES|<none>",
                "ttl_seconds|integer|NO|30",
                "expires_at|timestamp with time zone|NO|<none>",
                "idempotency_key|text|NO|<none>",
                "request_fingerprint|text|NO|<none>",
                "claimed_call_id|text|YES|<none>",
                "claimed_at|timestamp with time zone|YES|<none>",
                "terminal_at|timestamp with time zone|YES|<none>",
                "created_at|timestamp with time zone|NO|now()",
                "updated_at|timestamp with time zone|NO|now()",
                "revision|integer|NO|1"));
        v005ColumnDetails.put("inbound_session", List.of(
                "id|uuid|NO|<none>",
                "call_id|text|NO|<none>",
                "subject_id|uuid|YES|<none>",
                "trusted_caller|boolean|NO|<none>",
                "caller_ciphertext|text|YES|<none>",
                "caller_number_hmac|text|YES|<none>",
                "caller_masked|text|YES|<none>",
                "access_number_masked|text|NO|<none>",
                "node_id|text|NO|<none>",
                "protocol_version|text|NO|<none>",
                "prompt_version|text|NO|<none>",
                "status|text|NO|'RECEIVED'::text",
                "confirmed_subject_id|uuid|YES|<none>",
                "guidance_status|text|YES|<none>",
                "created_at|timestamp with time zone|NO|now()",
                "confirmed_at|timestamp with time zone|YES|<none>",
                "revision|integer|NO|1"));
        v005ColumnDetails.put("call_record", List.of(
                "id|uuid|NO|<none>",
                "call_id|text|NO|<none>",
                "session_id|uuid|NO|<none>",
                "task_id|uuid|YES|<none>",
                "subject_id|uuid|YES|<none>",
                "caller_ciphertext|text|NO|<none>",
                "caller_number_hmac|text|NO|<none>",
                "caller_masked|text|NO|<none>",
                "callee_ciphertext|text|YES|<none>",
                "callee_masked|text|YES|<none>",
                "access_number_masked|text|NO|<none>",
                "status|text|NO|'ACTIVE'::text",
                "node_id|text|NO|<none>",
                "protocol_version|text|NO|<none>",
                "started_at|timestamp with time zone|YES|<none>",
                "bridged_at|timestamp with time zone|YES|<none>",
                "recording_started_at|timestamp with time zone|YES|<none>",
                "recording_ended_at|timestamp with time zone|YES|<none>",
                "ended_at|timestamp with time zone|YES|<none>",
                "authoritative_seconds|integer|YES|<none>",
                "end_reason|text|YES|<none>",
                "created_at|timestamp with time zone|NO|now()",
                "revision|integer|NO|1"));
        v005ColumnDetails.put("call_leg", List.of(
                "id|uuid|NO|<none>",
                "call_id|text|NO|<none>",
                "leg_id|text|NO|<none>",
                "leg_type|text|NO|<none>",
                "direction|text|NO|<none>",
                "state|text|NO|<none>",
                "started_at|timestamp with time zone|NO|<none>",
                "answered_at|timestamp with time zone|YES|<none>",
                "ended_at|timestamp with time zone|YES|<none>",
                "end_reason|text|YES|<none>",
                "revision|integer|NO|1"));
        v005ColumnDetails.put("call_event", List.of(
                "id|uuid|NO|<none>",
                "call_id|text|NO|<none>",
                "event_domain|text|NO|'IVR_EVENT'::text",
                "event_id|text|NO|<none>",
                "sequence_no|integer|YES|<none>",
                "event_type|text|NO|<none>",
                "occurred_at|timestamp with time zone|NO|<none>",
                "received_at|timestamp with time zone|NO|now()",
                "payload_ciphertext|text|NO|<none>",
                "payload_redacted|jsonb|NO|<none>",
                "payload_sha256|text|NO|<none>",
                "processing_status|text|NO|'RECEIVED'::text",
                "reason_code|text|YES|<none>",
                "revision|integer|NO|1"));
        v005ColumnDetails.put("ivr_decision", List.of(
                "id|uuid|NO|<none>",
                "session_id|uuid|NO|<none>",
                "call_id|text|NO|<none>",
                "purpose|text|NO|<none>",
                "source_event_domain|text|YES|<none>",
                "source_event_id|text|YES|<none>",
                "request_fingerprint|text|NO|<none>",
                "action|text|YES|<none>",
                "reason_code|text|NO|<none>",
                "response_snapshot|jsonb|NO|<none>",
                "response_payload_ciphertext|text|NO|<none>",
                "response_sha256|text|NO|<none>",
                "key_version|integer|NO|<none>",
                "reservation_id|uuid|YES|<none>",
                "decided_at|timestamp with time zone|NO|now()"));
        for (Map.Entry<String, List<String>> entry : v005ColumnDetails.entrySet()) {
            assertThat(queryStrings(
                    connection,
                    "SELECT column_name || '|' || data_type || '|' || is_nullable || '|' "
                            + "|| coalesce(column_default, '<none>') "
                            + "FROM information_schema.columns "
                            + "WHERE table_schema = 'ivr' AND table_name = ? "
                            + "ORDER BY ordinal_position",
                    entry.getKey()))
                    .as("exact column details for ivr.%s", entry.getKey())
                    .containsExactlyElementsOf(entry.getValue());
        }

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.columns c "
                        + "JOIN pg_attrdef d ON d.adrelid = (c.table_schema || '.' || c.table_name)::regclass "
                        + "AND d.adnum = c.ordinal_position "
                        + "WHERE c.table_schema = 'ivr'"))
                .as("V005 defaulted columns").isEqualTo(17);
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'ivr' AND column_default IS NOT NULL "
                        + "AND column_name = 'id'"))
                .as("zero id defaults").isZero();

        List<String> checkNames = queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND c.contype = 'c' ORDER BY conname");
        assertThat(checkNames).hasSize(73);

        List<String> pkUniqueRows = queryStrings(
                connection,
                "SELECT r.relname || '|' || c.conname || '|' || c.contype::text || '|' "
                        + "|| pg_get_constraintdef(c.oid) || '|' "
                        + "|| (SELECT string_agg(a.attname, ',' ORDER BY x.ord) "
                        + "FROM unnest(c.conkey) WITH ORDINALITY AS x(attnum, ord) "
                        + "JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = x.attnum) "
                        + "FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND c.contype IN ('p','u') "
                        + "AND NOT EXISTS (SELECT 1 FROM pg_index i WHERE i.indexrelid = c.conindid AND i.indpred IS NOT NULL) "
                        + "ORDER BY 1");
        assertThat(pkUniqueRows).containsExactlyInAnyOrderElementsOf(V005_PK_UNIQUE_DEFS);
        assertThat(V005_PK_UNIQUE_DEFS).hasSize(14);

        List<String> fkRows = queryStrings(
                connection,
                "SELECT c.conname || '|' || pg_get_constraintdef(c.oid) || '|' "
                        + "|| CASE WHEN c.confdeltype = 'r' THEN 'rrs' ELSE c.confdeltype::text END "
                        + "|| '|' || c.condeferrable || '|' || c.condeferred || '|' || c.convalidated "
                        + "FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND c.contype = 'f' ORDER BY 1");
        assertThat(fkRows).containsExactlyInAnyOrderElementsOf(V005_FK_DEFS);
        assertThat(V005_FK_DEFS).hasSize(15);
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND c.conname = 'fk_ivr_decision_reservation'"))
                .isZero();

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND c.contype = 'f' "
                        + "AND ((r.relname = 'inbound_session' AND EXISTS ("
                        + "SELECT 1 FROM pg_attribute a WHERE a.attrelid = r.oid AND a.attname = 'call_id' "
                        + "AND a.attnum = ANY (c.conkey))) "
                        + "OR (r.relname = 'call_record' AND EXISTS ("
                        + "SELECT 1 FROM pg_attribute a WHERE a.attrelid = r.oid AND a.attname = 'call_id' "
                        + "AND a.attnum = ANY (c.conkey))))"))
                .as("no FK on either root call_id").isZero();

        List<String> indexDefs = queryStrings(
                connection,
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'ivr' ORDER BY 1");
        assertThat(indexDefs).containsExactlyInAnyOrderElementsOf(V005_INDEX_DEFS);
        assertThat(V005_INDEX_DEFS).hasSize(31);
    }

    private static void assertV005CheckPredicateInventory(Connection connection) throws SQLException {
        assertThat(V005_CHECK_DEF_MAP).as("expected V005 check map tables").hasSize(6);
        int totalChecks = 0;
        for (Map.Entry<String, Map<String, String>> tableEntry : V005_CHECK_DEF_MAP.entrySet()) {
            String table = tableEntry.getKey();
            totalChecks += tableEntry.getValue().size();
            List<String> actualNames = queryStrings(
                    connection,
                    "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'ivr' AND r.relname = ? AND c.contype = 'c' "
                            + "ORDER BY conname",
                    table);
            List<String> expectedNames = new ArrayList<>(tableEntry.getValue().keySet());
            expectedNames.sort(String::compareTo);
            assertThat(actualNames)
                    .as("exact check-name set for ivr.%s", table)
                    .containsExactlyElementsOf(expectedNames);
            for (Map.Entry<String, String> checkEntry : tableEntry.getValue().entrySet()) {
                String checkName = checkEntry.getKey();
                List<String> actualDefs = queryStrings(
                        connection,
                        "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c "
                                + "JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = 'ivr' AND r.relname = ? "
                                + "AND c.contype = 'c' AND c.conname = ?",
                        table, checkName);
                assertThat(actualDefs)
                        .as("exactly one row for ivr.%s.%s", table, checkName)
                        .hasSize(1);
                assertThat(normalizeCheckDef(actualDefs.get(0)))
                        .as("whole pg_get_constraintdef equality for ivr.%s.%s", table, checkName)
                        .isEqualTo(normalizeCheckDef(checkEntry.getValue()));
            }
        }
        assertThat(totalChecks).as("total V005 checks").isEqualTo(73);
    }

    private static void assertV005FunctionsAndTriggersExact(Connection connection) throws SQLException {
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'ivr'"))
                .as("exactly five ivr functions").isEqualTo(5);
        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || p.proname || '(' || pg_get_function_identity_arguments(p.oid) || ')' "
                        + "|| '|' || p.prosecdef || '|' || pg_get_userbyid(p.proowner) || '|' "
                        + "|| coalesce(array_to_string(p.proconfig, ','), '') "
                        + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'ivr' ORDER BY 1"))
                .containsExactly(
                        "ivr.guard_call_leg()|false|ivr_owner|search_path=ivr, pg_temp",
                        "ivr.guard_call_record_parties()|false|ivr_owner|search_path=ivr, pg_temp",
                        "ivr.guard_inbound_session()|false|ivr_owner|search_path=ivr, pg_temp",
                        "ivr.guard_outbound_call_task()|false|ivr_owner|search_path=ivr, pg_temp",
                        "ivr.reject_row_mutation()|false|ivr_owner|search_path=ivr, pg_temp");
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'ivr' AND p.prosrc LIKE '%gen_random_uuid%'"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'ivr' "
                        + "AND p.prosrc ~* '\\m(commit|rollback|start\\s+transaction|set\\s+transaction)\\M'"))
                .isZero();

        List<String> triggerRows = queryStrings(
                connection,
                "SELECT n.nspname || '.' || c.relname || '.' || t.tgname || '|' "
                        + "|| CASE WHEN (t.tgtype & 2) <> 0 THEN 'BEFORE' ELSE 'AFTER' END || '|' "
                        + "|| CASE WHEN (t.tgtype & 1) <> 0 THEN 'ROW' ELSE 'STATEMENT' END || '|' "
                        + "|| CASE WHEN (t.tgtype & 4) <> 0 THEN 'INSERT' ELSE '' END || '|' "
                        + "|| CASE WHEN (t.tgtype & 16) <> 0 THEN 'UPDATE' ELSE '' END || '|' "
                        + "|| CASE WHEN (t.tgtype & 8) <> 0 THEN 'DELETE' ELSE '' END || '|' "
                        + "|| CASE WHEN (t.tgtype & 32) <> 0 THEN 'TRUNCATE' ELSE '' END || '|' "
                        + "|| p.proname "
                        + "FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "JOIN pg_proc p ON p.oid = t.tgfoid "
                        + "WHERE NOT t.tgisinternal AND n.nspname = 'ivr' ORDER BY 1");
        assertThat(triggerRows).containsExactly(
                "ivr.call_leg.trg_call_leg_guard|BEFORE|ROW||UPDATE|||guard_call_leg",
                "ivr.call_record.trg_call_record_guard|BEFORE|ROW||UPDATE|||guard_call_record_parties",
                "ivr.inbound_session.trg_inbound_session_guard|BEFORE|ROW||UPDATE|||guard_inbound_session",
                "ivr.ivr_decision.trg_ivr_decision_immutable|BEFORE|ROW||UPDATE|DELETE||reject_row_mutation",
                "ivr.outbound_call_task.trg_outbound_call_task_guard|BEFORE|ROW||UPDATE|||guard_outbound_call_task");
    }

    private static void assertV005NoIdDefaultsOrSequences(Connection connection) throws SQLException {
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'ivr' AND column_name = 'id' "
                        + "AND column_default IS NOT NULL"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'ivr' AND c.relkind = 'S'"))
                .isZero();
    }

    private static void assertV005SchemaUsageMatrix(Connection connection) throws SQLException {
        Map<String, List<String>> usageRolesBySchema = new java.util.LinkedHashMap<>();
        usageRolesBySchema.put("ivr", List.of("ivr_owner", "ivr_role"));
        usageRolesBySchema.put("customer_iam", List.of(
                "customer_iam_owner", "customer_iam_role", "subject_number_role",
                "catalog_order_role", "ivr_role"));
        usageRolesBySchema.put("organization_access", List.of(
                "organization_access_owner", "organization_access_role", "customer_iam_role",
                "subject_number_role", "catalog_order_role", "ivr_role"));
        usageRolesBySchema.put("subject_number", List.of(
                "subject_number_owner", "subject_number_role",
                "subject_identity_verification_role", "catalog_order_role", "ivr_role"));
        Map<String, String> ownerBySchema = new java.util.LinkedHashMap<>();
        ownerBySchema.put("ivr", "ivr_owner");
        ownerBySchema.put("customer_iam", "customer_iam_owner");
        ownerBySchema.put("organization_access", "organization_access_owner");
        ownerBySchema.put("subject_number", "subject_number_owner");
        List<String> grantees = new ArrayList<>(S1_ROLES);
        grantees.addAll(V004_ROLES);
        grantees.add("public");
        for (Map.Entry<String, String> schemaEntry : ownerBySchema.entrySet()) {
            String schema = schemaEntry.getKey();
            String owner = schemaEntry.getValue();
            List<String> usageRoles = usageRolesBySchema.get(schema);
            for (String grantee : grantees) {
                assertThat(hasSchema(connection, grantee, schema, "USAGE"))
                        .as("%s schema USAGE %s", grantee, schema)
                        .isEqualTo(usageRoles.contains(grantee));
                assertThat(hasSchema(connection, grantee, schema, "CREATE"))
                        .as("%s schema CREATE %s", grantee, schema)
                        .isEqualTo(grantee.equals(owner));
            }
        }
        assertThat(hasSchema(connection, "ivr_role", "catalog_order", "USAGE"))
                .as("ivr_role catalog_order USAGE is not granted in V005 (§10.12)").isFalse();
        assertThat(hasSchema(connection, "activation_consent_role", "subject_number", "USAGE"))
                .as("activation_consent_role S1 USAGE is deferred to V007").isFalse();
        assertThat(hasSchema(connection, "entitlement_metering_role", "subject_number", "USAGE"))
                .as("entitlement_metering_role subject USAGE is deferred to V006").isFalse();
    }

    private static void assertV005OwnedAndCrossModuleGrants(Connection connection)
            throws SQLException {
        Map<String, List<String>> allColumnsByTable = new java.util.LinkedHashMap<>();
        allColumnsByTable.put("ivr.outbound_call_task", List.of(
                "id", "task_id", "caller_binding_id", "subject_id", "owner_account_id",
                "owner_organization_id", "created_by_account_id", "caller_number_hmac",
                "callee_ciphertext", "callee_masked", "business_type", "status", "reason_code",
                "ttl_seconds", "expires_at", "idempotency_key", "request_fingerprint",
                "claimed_call_id", "claimed_at", "terminal_at", "created_at", "updated_at",
                "revision"));
        allColumnsByTable.put("ivr.inbound_session", List.of(
                "id", "call_id", "subject_id", "trusted_caller", "caller_ciphertext",
                "caller_number_hmac", "caller_masked", "access_number_masked", "node_id",
                "protocol_version", "prompt_version", "status", "confirmed_subject_id",
                "guidance_status", "created_at", "confirmed_at", "revision"));
        allColumnsByTable.put("ivr.call_record", List.of(
                "id", "call_id", "session_id", "task_id", "subject_id", "caller_ciphertext",
                "caller_number_hmac", "caller_masked", "callee_ciphertext", "callee_masked",
                "access_number_masked", "status", "node_id", "protocol_version", "started_at",
                "bridged_at", "recording_started_at", "recording_ended_at", "ended_at",
                "authoritative_seconds", "end_reason", "created_at", "revision"));
        allColumnsByTable.put("ivr.call_leg", List.of(
                "id", "call_id", "leg_id", "leg_type", "direction", "state", "started_at",
                "answered_at", "ended_at", "end_reason", "revision"));
        allColumnsByTable.put("ivr.call_event", List.of(
                "id", "call_id", "event_domain", "event_id", "sequence_no", "event_type",
                "occurred_at", "received_at", "payload_ciphertext", "payload_redacted",
                "payload_sha256", "processing_status", "reason_code", "revision"));
        allColumnsByTable.put("ivr.ivr_decision", List.of(
                "id", "session_id", "call_id", "purpose", "source_event_domain", "source_event_id",
                "request_fingerprint", "action", "reason_code", "response_snapshot",
                "response_payload_ciphertext", "response_sha256", "key_version", "reservation_id",
                "decided_at"));

        Map<String, List<String>> runtimeUpdateColumns = new java.util.LinkedHashMap<>();
        runtimeUpdateColumns.put("ivr.outbound_call_task", List.of(
                "status", "reason_code", "claimed_call_id", "claimed_at", "terminal_at",
                "updated_at", "revision"));
        runtimeUpdateColumns.put("ivr.inbound_session", List.of(
                "status", "confirmed_subject_id", "guidance_status", "confirmed_at", "revision"));
        runtimeUpdateColumns.put("ivr.call_record", List.of(
                "task_id", "status", "started_at", "bridged_at", "recording_started_at",
                "recording_ended_at", "ended_at", "authoritative_seconds", "end_reason",
                "revision"));
        runtimeUpdateColumns.put("ivr.call_leg", List.of(
                "state", "answered_at", "ended_at", "end_reason", "revision"));
        runtimeUpdateColumns.put("ivr.call_event", List.of(
                "processing_status", "reason_code", "revision"));
        runtimeUpdateColumns.put("ivr.ivr_decision", List.of());

        List<String> matrixRoles = new ArrayList<>(S1_ROLES);
        matrixRoles.addAll(V004_ROLES);
        for (String table : V005_TABLES) {
            for (String role : matrixRoles) {
                boolean ownerRow = role.equals("ivr_owner");
                boolean runtimeRow = role.equals("ivr_role");
                assertThat(hasTable(connection, role, table, "SELECT"))
                        .as("matrix %s table SELECT %s", role, table)
                        .isEqualTo(ownerRow || runtimeRow);
                assertThat(hasTable(connection, role, table, "INSERT"))
                        .as("matrix %s table INSERT %s", role, table)
                        .isEqualTo(ownerRow || runtimeRow);
                assertThat(hasTable(connection, role, table, "UPDATE"))
                        .as("matrix %s table UPDATE %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "DELETE"))
                        .as("matrix %s table DELETE %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "TRUNCATE"))
                        .as("matrix %s table TRUNCATE %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "REFERENCES"))
                        .as("matrix %s table REFERENCES %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "TRIGGER"))
                        .as("matrix %s table TRIGGER %s", role, table)
                        .isEqualTo(ownerRow);
                for (String column : allColumnsByTable.get(table)) {
                    boolean expectColumnSelect = ownerRow || runtimeRow;
                    boolean expectColumnInsert = ownerRow || runtimeRow;
                    boolean expectColumnUpdate = ownerRow
                            || (runtimeRow && runtimeUpdateColumns.get(table).contains(column));
                    assertThat(hasColumn(connection, role, table, column, "SELECT"))
                            .as("matrix %s column SELECT %s.%s", role, table, column)
                            .isEqualTo(expectColumnSelect);
                    assertThat(hasColumn(connection, role, table, column, "INSERT"))
                            .as("matrix %s column INSERT %s.%s", role, table, column)
                            .isEqualTo(expectColumnInsert);
                    assertThat(hasColumn(connection, role, table, column, "UPDATE"))
                            .as("matrix %s column UPDATE %s.%s", role, table, column)
                            .isEqualTo(expectColumnUpdate);
                }
            }
        }

        Map<String, List<String>> ivrCrossSelect = new java.util.LinkedHashMap<>();
        ivrCrossSelect.put("customer_iam.customer_account", List.of("id", "status"));
        ivrCrossSelect.put("organization_access.organization", List.of("id", "status"));
        ivrCrossSelect.put("organization_access.organization_membership",
                List.of("organization_id", "account_id", "role", "status"));
        ivrCrossSelect.put("subject_number.business_subject",
                List.of("id", "owner_account_id", "owner_organization_id", "revision"));
        ivrCrossSelect.put("subject_number.caller_number_binding",
                List.of("id", "subject_id", "number_hmac", "status", "verified_at", "revision"));
        ivrCrossSelect.put("subject_number.subject_risk_state",
                List.of("caller_number_binding_id", "risk_type", "state", "revision"));
        for (Map.Entry<String, List<String>> entry : ivrCrossSelect.entrySet()) {
            String table = entry.getKey();
            List<String> columns = queryStrings(
                    connection,
                    "SELECT a.attname FROM pg_class c "
                            + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                            + "JOIN pg_attribute a ON a.attrelid = c.oid "
                            + "WHERE n.nspname || '.' || c.relname = ? "
                            + "AND a.attnum > 0 AND NOT a.attisdropped ORDER BY a.attnum",
                    table);
            assertThat(columns).isNotEmpty();
            for (String privilege : List.of("SELECT", "INSERT", "UPDATE", "DELETE",
                    "TRUNCATE", "REFERENCES", "TRIGGER")) {
                assertThat(hasTable(connection, "ivr_role", table, privilege))
                        .as("ivr_role must not hold table %s on %s", privilege, table)
                        .isFalse();
            }
            for (String column : columns) {
                assertThat(hasColumn(connection, "ivr_role", table, column, "SELECT"))
                        .as("ivr_role SELECT %s.%s", table, column)
                        .isEqualTo(entry.getValue().contains(column));
                assertThat(hasColumn(connection, "ivr_role", table, column, "INSERT"))
                        .as("ivr_role must not INSERT %s.%s", table, column).isFalse();
                assertThat(hasColumn(connection, "ivr_role", table, column, "UPDATE"))
                        .as("ivr_role must not UPDATE %s.%s", table, column).isFalse();
            }
        }
        assertThat(hasTable(connection, "ivr_role", "ivr.ivr_decision", "UPDATE"))
                .as("ivr_decision is append-only for runtime").isFalse();
        assertThat(hasColumn(connection, "ivr_role", "ivr.call_record", "created_at", "UPDATE"))
                .as("created_at is insert-only for runtime (option B)").isFalse();
        assertThat(hasColumn(connection, "ivr_role", "ivr.call_record", "subject_id", "UPDATE"))
                .as("subject_id is not runtime-updatable").isFalse();
    }

    private static void assertV005PublicAclIsZero(Connection connection) throws SQLException {
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_namespace n, "
                        + "aclexplode(coalesce(n.nspacl, acldefault('n', n.nspowner))) a "
                        + "WHERE n.nspname = 'ivr' AND a.grantee = 0"))
                .as("PUBLIC has no ivr schema privilege").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace, "
                        + "aclexplode(coalesce(c.relacl, acldefault('r', c.relowner))) a "
                        + "WHERE n.nspname = 'ivr' AND c.relkind IN ('r','p') AND a.grantee = 0"))
                .as("PUBLIC has no ivr table privilege").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace, "
                        + "aclexplode(coalesce(p.proacl, acldefault('f', p.proowner))) a "
                        + "WHERE n.nspname = 'ivr' AND a.grantee = 0"))
                .as("PUBLIC has no ivr function privilege").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "JOIN pg_attribute a ON a.attrelid = c.oid, "
                        + "aclexplode(coalesce(a.attacl, acldefault('c', c.relowner))) x "
                        + "WHERE n.nspname = 'ivr' AND c.relkind IN ('r','p') "
                        + "AND a.attnum > 0 AND NOT a.attisdropped AND x.grantee = 0"))
                .as("PUBLIC has no ivr column privilege").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "JOIN pg_attribute a ON a.attrelid = c.oid, "
                        + "aclexplode(coalesce(a.attacl, acldefault('c', c.relowner))) x "
                        + "WHERE ((n.nspname = 'customer_iam' AND c.relname = 'customer_account') "
                        + "OR (n.nspname = 'organization_access' AND c.relname IN "
                        + "('organization','organization_membership')) "
                        + "OR (n.nspname = 'subject_number' AND c.relname IN "
                        + "('business_subject','caller_number_binding','subject_risk_state'))) "
                        + "AND a.attnum > 0 AND NOT a.attisdropped AND x.grantee = 0"))
                .as("PUBLIC has no column ACL on V005-touched S1 tables").isZero();
    }

    private static void assertV005GuardFunctionExecuteBoundaries(Connection connection)
            throws SQLException {
        List<String> guardFunctions = List.of(
                "ivr.guard_outbound_call_task()",
                "ivr.guard_inbound_session()",
                "ivr.guard_call_record_parties()",
                "ivr.guard_call_leg()");
        List<String> functionMatrixRoles = new ArrayList<>(S1_ROLES);
        functionMatrixRoles.addAll(V004_ROLES);
        for (String function : guardFunctions) {
            assertThat(hasFunction(connection, "ivr_owner", function, "EXECUTE"))
                    .as("ivr_owner EXECUTE %s", function).isTrue();
            assertThat(hasFunction(connection, "ivr_role", function, "EXECUTE"))
                    .as("ivr_role EXECUTE %s", function).isTrue();
            for (String role : functionMatrixRoles) {
                if (role.equals("ivr_owner") || role.equals("ivr_role")) {
                    continue;
                }
                assertThat(hasFunction(connection, role, function, "EXECUTE"))
                        .as("%s must not EXECUTE %s", role, function).isFalse();
            }
            assertThat(hasFunction(connection, "public", function, "EXECUTE"))
                    .as("PUBLIC must not EXECUTE %s", function).isFalse();
        }
        assertThat(hasFunction(connection, "ivr_owner", "ivr.reject_row_mutation()", "EXECUTE"))
                .as("ivr_owner EXECUTE reject_row_mutation").isTrue();
        assertThat(hasFunction(connection, "ivr_role", "ivr.reject_row_mutation()", "EXECUTE"))
                .as("runtime must not EXECUTE reject_row_mutation").isFalse();
        for (String role : functionMatrixRoles) {
            if (role.equals("ivr_owner")) {
                continue;
            }
            assertThat(hasFunction(connection, role, "ivr.reject_row_mutation()", "EXECUTE"))
                    .as("%s must not EXECUTE reject_row_mutation", role).isFalse();
        }
        assertThat(hasFunction(connection, "public", "ivr.reject_row_mutation()", "EXECUTE"))
                .as("PUBLIC must not EXECUTE reject_row_mutation").isFalse();
    }

    private static void exerciseV005IvrInsertsAsRuntimeRoleAndRollBack() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID callRecordId = UUID.randomUUID();
        UUID legId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID decisionId = UUID.randomUUID();
        String callId = "pa5-s1-call-" + UUID.randomUUID();
        String hmac = randomSha256();
        String fingerprint = randomSha256();
        String ciphertext = "v1.s1.ivr.smoke.ciphertext";
        String masked = "137****0001";
        String taskIdText = "pa5-task-" + UUID.randomUUID();
        String idempotencyKey = "pa5-s1-" + UUID.randomUUID();
        String eventFingerprint = randomSha256();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "customer_iam_role");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO customer_iam.customer_account "
                                + "(id, display_name, status) VALUES (?, ?, 'ACTIVE')",
                        accountId, "pa5-s1-" + UUID.randomUUID())).isEqualTo(1);
                setLocalRole(connection, "organization_access_owner");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO organization_access.organization "
                                + "(id, display_name, status) VALUES (?, ?, 'ACTIVE')",
                        organizationId, "pa5-" + UUID.randomUUID())).isEqualTo(1);
                setLocalRole(connection, "subject_number_owner");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO subject_number.business_subject "
                                + "(id, source, owner_account_id) VALUES (?, 'IVR', ?)",
                        subjectId, accountId)).isEqualTo(1);
                setLocalRole(connection, "subject_number_role");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO subject_number.caller_number_binding "
                                + "(id, subject_id, number_hmac, number_ciphertext, number_masked, "
                                + "number_type, status, verified_at) VALUES (?, ?, ?, ?, ?, 'MOBILE', 'ACTIVE', now())",
                        bindingId, subjectId, hmac, ciphertext, masked)).isEqualTo(1);
                setLocalRole(connection, "ivr_role");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                                + "created_by_account_id, caller_number_hmac, callee_ciphertext, "
                                + "callee_masked, business_type, expires_at, idempotency_key, "
                                + "request_fingerprint) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                                + "now() + interval '30 seconds', ?, ?)",
                        taskId, taskIdText, bindingId, subjectId, accountId, accountId, hmac,
                        ciphertext, masked, idempotencyKey, fingerprint)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.inbound_session "
                                + "(id, call_id, subject_id, trusted_caller, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, prompt_version) "
                                + "VALUES (?, ?, ?, TRUE, ?, ?, ?, ?, 'node-1', '1.0', 'p1')",
                        sessionId, callId, subjectId, ciphertext, hmac, masked, masked)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.call_record "
                                + "(id, call_id, session_id, subject_id, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, started_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'node-1', '1.0', now())",
                        callRecordId, callId, sessionId, subjectId, ciphertext, hmac, masked,
                        masked)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.call_leg "
                                + "(id, call_id, leg_id, leg_type, direction, state, started_at) "
                                + "VALUES (?, ?, 'leg-1', 'CALLER', 'INBOUND', 'RINGING', now())",
                        legId, callId)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.call_event "
                                + "(id, call_id, event_id, sequence_no, event_type, occurred_at, "
                                + "payload_ciphertext, payload_redacted, payload_sha256) "
                                + "VALUES (?, ?, ?, 1, 'SESSION_START', now(), ?, ?::jsonb, ?)",
                        eventId, callId, "evt-" + UUID.randomUUID(), ciphertext,
                        "{\"source\":{\"eventType\":\"SESSION_START\",\"legId\":\"leg-1\","
                                + "\"sequenceNo\":1,\"payload\":{}},\"receipt\":{\"payloadSha256\":\""
                                + hmac + "\",\"eventFingerprint\":\"" + eventFingerprint + "\"}}",
                        hmac)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, request_fingerprint, action, "
                                + "reason_code, response_snapshot, response_payload_ciphertext, "
                                + "response_sha256, key_version) "
                                + "VALUES (?, ?, ?, 'INBOUND_RESOLVE', ?, 'GUIDE_ACTIVATION', "
                                + "'ACTIVATION_REQUIRED', '{}'::jsonb, ?, ?, 1)",
                        decisionId, sessionId, callId, fingerprint, ciphertext, hmac)).isEqualTo(1);
                assertThat(queryLong(
                        connection,
                        "SELECT count(*) FROM ivr.outbound_call_task WHERE id = ?", taskId))
                        .isEqualTo(1);
                assertThat(queryLong(
                        connection,
                        "SELECT count(*) FROM ivr.ivr_decision WHERE id = ?", decisionId))
                        .isEqualTo(1);
                connection.rollback();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM ivr.outbound_call_task WHERE id = ?", taskId))
                    .isZero();
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM ivr.inbound_session WHERE id = ?", sessionId))
                    .isZero();
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM ivr.call_record WHERE id = ?", callRecordId))
                    .isZero();
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM ivr.call_leg WHERE id = ?", legId))
                    .isZero();
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM ivr.call_event WHERE id = ?", eventId))
                    .isZero();
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM ivr.ivr_decision WHERE id = ?", decisionId))
                    .isZero();
        }
    }

    /**
     * PA5 S1 fix-1 (F-PA5-S1-03): plan §6.2 item 9 executable runtime evidence for
     * {@code v005OwnerPublicRuntimeAndCrossModuleAclsAreExact}. Builds a method-local
     * fixture, switches to {@code ivr_role}, proves one legal column UPDATE positive
     * control, then asserts precise {@code 42501} for an undeclared-column UPDATE,
     * DELETE, TRUNCATE, cross-module writes, cross-module {@code SELECT ... FOR UPDATE}
     * and the owner-only {@code ivr.reject_row_mutation()} invocation. Every failure
     * uses a bounded savepoint rollback and the whole fixture transaction is rolled
     * back, so no committed residue remains. Exact catalog ACL assertions are unchanged.
     */
    private static void assertV005RuntimeDenialsAndLegalUpdateControl() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID callRecordId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID legId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID decisionId = UUID.randomUUID();
        String callId = "pa5-s1fix1-acl-" + UUID.randomUUID();
        String hmac = randomSha256();
        String fingerprint = randomSha256();
        String ciphertext = "v1.s1fix1.ivr.acl.ciphertext";
        String masked = "137****0002";
        String taskIdText = "pa5-s1fix1-task-" + UUID.randomUUID();
        String idempotencyKey = "pa5-s1fix1-" + UUID.randomUUID();
        String eventFingerprint = randomSha256();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "customer_iam_role");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO customer_iam.customer_account "
                                + "(id, display_name, status) VALUES (?, ?, 'ACTIVE')",
                        accountId, "pa5-s1fix1-" + UUID.randomUUID())).isEqualTo(1);
                setLocalRole(connection, "subject_number_owner");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO subject_number.business_subject "
                                + "(id, source, owner_account_id) VALUES (?, 'IVR', ?)",
                        subjectId, accountId)).isEqualTo(1);
                setLocalRole(connection, "subject_number_role");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO subject_number.caller_number_binding "
                                + "(id, subject_id, number_hmac, number_ciphertext, number_masked, "
                                + "number_type, status, verified_at) VALUES (?, ?, ?, ?, ?, 'MOBILE', 'ACTIVE', now())",
                        bindingId, subjectId, hmac, ciphertext, masked)).isEqualTo(1);
                setLocalRole(connection, "ivr_owner");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                                + "created_by_account_id, caller_number_hmac, callee_ciphertext, "
                                + "callee_masked, business_type, expires_at, idempotency_key, "
                                + "request_fingerprint) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                                + "now() + interval '30 seconds', ?, ?)",
                        taskId, taskIdText, bindingId, subjectId, accountId, accountId, hmac,
                        ciphertext, masked, idempotencyKey, fingerprint)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.inbound_session "
                                + "(id, call_id, subject_id, trusted_caller, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, prompt_version) "
                                + "VALUES (?, ?, ?, TRUE, ?, ?, ?, ?, 'node-1', '1.0', 'p1')",
                        sessionId, callId, subjectId, ciphertext, hmac, masked, masked)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.call_record "
                                + "(id, call_id, session_id, subject_id, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, started_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'node-1', '1.0', now())",
                        callRecordId, callId, sessionId, subjectId, ciphertext, hmac, masked,
                        masked)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.call_leg "
                                + "(id, call_id, leg_id, leg_type, direction, state, started_at) "
                                + "VALUES (?, ?, 'leg-1', 'CALLER', 'INBOUND', 'RINGING', now())",
                        legId, callId)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.call_event "
                                + "(id, call_id, event_id, event_type, occurred_at, "
                                + "payload_ciphertext, payload_redacted, payload_sha256) "
                                + "VALUES (?, ?, ?, 'SESSION_START', now(), ?, ?::jsonb, ?)",
                        eventId, callId, "evt-" + UUID.randomUUID(), ciphertext,
                        "{\"source\":{\"eventType\":\"SESSION_START\",\"legId\":\"leg-1\","
                                + "\"sequenceNo\":1,\"payload\":{}},\"receipt\":{\"payloadSha256\":\""
                                + hmac + "\",\"eventFingerprint\":\"" + eventFingerprint + "\"}}",
                        hmac)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, request_fingerprint, action, "
                                + "reason_code, response_snapshot, response_payload_ciphertext, "
                                + "response_sha256, key_version) "
                                + "VALUES (?, ?, ?, 'INBOUND_RESOLVE', ?, 'GUIDE_ACTIVATION', "
                                + "'ACTIVATION_REQUIRED', '{}'::jsonb, ?, ?, 1)",
                        decisionId, sessionId, callId, fingerprint, ciphertext, hmac)).isEqualTo(1);

                setLocalRole(connection, "ivr_role");
                // Legal column UPDATE positive control: state/revision are granted and
                // pass guard_call_leg unchanged.
                assertThat(executeUpdate(
                        connection,
                        "UPDATE ivr.call_leg SET state = 'ANSWERED', revision = revision + 1 "
                                + "WHERE id = ? AND revision = 1",
                        legId)).isEqualTo(1);
                // Undeclared-column runtime UPDATE denials.
                expectSqlFailure(connection, "42501", null,
                        "UPDATE ivr.call_leg SET leg_type = 'CALLEE' WHERE id = ?", legId);
                expectSqlFailure(connection, "42501", null,
                        "UPDATE ivr.outbound_call_task SET caller_number_hmac = ? WHERE id = ?",
                        randomSha256(), taskId);
                expectSqlFailure(connection, "42501", null,
                        "UPDATE ivr.ivr_decision SET reason_code = reason_code WHERE id = ?",
                        decisionId);
                // DELETE and TRUNCATE denials.
                expectSqlFailure(connection, "42501", null,
                        "DELETE FROM ivr.call_event WHERE id = ?", eventId);
                expectSqlFailure(connection, "42501", null,
                        "TRUNCATE ivr.call_leg");
                // Cross-module write denials.
                expectSqlFailure(connection, "42501", null,
                        "UPDATE subject_number.business_subject SET revision = revision + 1 "
                                + "WHERE id = ?",
                        subjectId);
                expectSqlFailure(connection, "42501", null,
                        "DELETE FROM customer_iam.customer_account WHERE id = ?", accountId);
                // Cross-module SELECT ... FOR UPDATE denials.
                expectSqlQueryFailure(connection, "42501",
                        "SELECT id FROM subject_number.business_subject WHERE id = ? FOR UPDATE",
                        subjectId);
                expectSqlQueryFailure(connection, "42501",
                        "SELECT id FROM customer_iam.customer_account WHERE id = ? FOR UPDATE",
                        accountId);
                // Owner-only reject_row_mutation has no runtime EXECUTE grant.
                expectSqlQueryFailure(connection, "42501",
                        "SELECT ivr.reject_row_mutation()");
            } finally {
                connection.rollback();
            }
        }
    }

    /**
     * PA5 S1 fix-1 (F-PA5-S1-01/02): bounded in-suite regression probes for the
     * user-authorized frozen CHECK errata. Malformed decision shapes and malformed
     * {@code payload_redacted} shapes must fail with SQLSTATE 23514 and the exact
     * constraint name; the legal direct-success (reservation present), direct-refusal
     * (no reservation), resolve, activation and object-payload/matching-receipt shapes
     * pass. Each probe is savepoint-isolated inside one rolled-back fixture transaction,
     * so the {@code (call_id, purpose)} and event identities stay unique and no
     * committed residue remains. The V006 reservation FK stays absent, so a random UUID
     * reservation reference is legal.
     */
    private static void assertV005ErrataCheckRegressionProbes() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID callRecordId = UUID.randomUUID();
        String callId = "pa5-s1fix1-probe-" + UUID.randomUUID();
        String hmac = randomSha256();
        String fingerprint = randomSha256();
        String ciphertext = "v1.s1fix1.ivr.probe.ciphertext";
        String masked = "137****0003";
        String eventFingerprint = randomSha256();
        String payloadSha = randomSha256();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "customer_iam_role");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO customer_iam.customer_account "
                                + "(id, display_name, status) VALUES (?, ?, 'ACTIVE')",
                        accountId, "pa5-s1fix1-probe-" + UUID.randomUUID())).isEqualTo(1);
                setLocalRole(connection, "subject_number_owner");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO subject_number.business_subject "
                                + "(id, source, owner_account_id) VALUES (?, 'IVR', ?)",
                        subjectId, accountId)).isEqualTo(1);
                setLocalRole(connection, "subject_number_role");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO subject_number.caller_number_binding "
                                + "(id, subject_id, number_hmac, number_ciphertext, number_masked, "
                                + "number_type, status, verified_at) VALUES (?, ?, ?, ?, ?, 'MOBILE', 'ACTIVE', now())",
                        bindingId, subjectId, hmac, ciphertext, masked)).isEqualTo(1);
                setLocalRole(connection, "ivr_owner");
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.inbound_session "
                                + "(id, call_id, subject_id, trusted_caller, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, prompt_version) "
                                + "VALUES (?, ?, ?, TRUE, ?, ?, ?, ?, 'node-1', '1.0', 'p1')",
                        sessionId, callId, subjectId, ciphertext, hmac, masked, masked)).isEqualTo(1);
                assertThat(executeUpdate(
                        connection,
                        "INSERT INTO ivr.call_record "
                                + "(id, call_id, session_id, subject_id, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, started_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'node-1', '1.0', now())",
                        callRecordId, callId, sessionId, subjectId, ciphertext, hmac, masked,
                        masked)).isEqualTo(1);

                String decisionInsertPrefix =
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, source_event_domain, "
                                + "source_event_id, request_fingerprint, action, reason_code, "
                                + "response_snapshot, response_payload_ciphertext, response_sha256, "
                                + "key_version) VALUES (?, ?, ?, ";

                // F-PA5-S1-01 negative: a "successful" DIRECT_AUTHORIZATION without a
                // reservation must be rejected by ck_ivr_decision_shape only.
                expectSqlFailure(connection, "23514", "ck_ivr_decision_shape",
                        decisionInsertPrefix
                                + "'DIRECT_AUTHORIZATION', 'IVR_DIRECT_AUTH', ?, ?, NULL, "
                                + "'DIRECT_AUTHORIZED', '{}'::jsonb, ?, ?, 1)",
                        UUID.randomUUID(), sessionId, callId, "evt-direct-" + UUID.randomUUID(),
                        fingerprint, ciphertext, hmac);
                // F-PA5-S1-01 negative: ACTIVATION_NEXT_ACTION with NULL source_event_id.
                expectSqlFailure(connection, "23514", "ck_ivr_decision_source",
                        decisionInsertPrefix
                                + "'ACTIVATION_NEXT_ACTION', 'IVR_CONFIRM', NULL, ?, "
                                + "'GUIDE_PURCHASE', 'CONSENT_REQUIRED', '{}'::jsonb, ?, ?, 1)",
                        UUID.randomUUID(), sessionId, callId, fingerprint, ciphertext, hmac);
                // F-PA5-S1-01 negative: ACTIVATION_NEXT_ACTION with NULL domain and NULL id.
                expectSqlFailure(connection, "23514", "ck_ivr_decision_source",
                        decisionInsertPrefix
                                + "'ACTIVATION_NEXT_ACTION', NULL, NULL, ?, 'GUIDE_PURCHASE', "
                                + "'CONSENT_REQUIRED', '{}'::jsonb, ?, ?, 1)",
                        UUID.randomUUID(), sessionId, callId, fingerprint, ciphertext, hmac);
                // F-PA5-S1-01 negative: DIRECT_AUTHORIZATION refusal with NULL source_event_id.
                expectSqlFailure(connection, "23514", "ck_ivr_decision_source",
                        decisionInsertPrefix
                                + "'DIRECT_AUTHORIZATION', 'IVR_DIRECT_AUTH', NULL, ?, 'REJECT', "
                                + "'CALLER_UNTRUSTED', '{}'::jsonb, ?, ?, 1)",
                        UUID.randomUUID(), sessionId, callId, fingerprint, ciphertext, hmac);
                // F-PA5-S1-01 negative: DIRECT_AUTHORIZATION refusal with NULL source_event_domain.
                expectSqlFailure(connection, "23514", "ck_ivr_decision_source",
                        decisionInsertPrefix
                                + "'DIRECT_AUTHORIZATION', NULL, ?, ?, 'REJECT', "
                                + "'CALLER_UNTRUSTED', '{}'::jsonb, ?, ?, 1)",
                        UUID.randomUUID(), sessionId, callId, "evt-direct-" + UUID.randomUUID(),
                        fingerprint, ciphertext, hmac);

                // Legal decision shapes (savepoint-isolated success probes).
                Savepoint directSuccess = connection.setSavepoint("s1fix1-direct-success");
                try {
                    assertThat(executeUpdate(
                            connection,
                            "INSERT INTO ivr.ivr_decision "
                                    + "(id, session_id, call_id, purpose, source_event_domain, "
                                    + "source_event_id, request_fingerprint, action, reason_code, "
                                    + "response_snapshot, response_payload_ciphertext, "
                                    + "response_sha256, key_version, reservation_id) "
                                    + "VALUES (?, ?, ?, 'DIRECT_AUTHORIZATION', 'IVR_DIRECT_AUTH', "
                                    + "?, ?, NULL, 'DIRECT_AUTHORIZED', '{}'::jsonb, ?, ?, 1, ?)",
                            UUID.randomUUID(), sessionId, callId,
                            "evt-direct-" + UUID.randomUUID(), fingerprint, ciphertext, hmac,
                            UUID.randomUUID())).isEqualTo(1);
                } finally {
                    connection.rollback(directSuccess);
                }
                Savepoint directRefusal = connection.setSavepoint("s1fix1-direct-refusal");
                try {
                    assertThat(executeUpdate(
                            connection,
                            "INSERT INTO ivr.ivr_decision "
                                    + "(id, session_id, call_id, purpose, source_event_domain, "
                                    + "source_event_id, request_fingerprint, action, reason_code, "
                                    + "response_snapshot, response_payload_ciphertext, "
                                    + "response_sha256, key_version) "
                                    + "VALUES (?, ?, ?, 'DIRECT_AUTHORIZATION', 'IVR_DIRECT_AUTH', "
                                    + "?, ?, 'REJECT', 'CALLER_UNTRUSTED', '{}'::jsonb, ?, ?, 1)",
                            UUID.randomUUID(), sessionId, callId,
                            "evt-direct-" + UUID.randomUUID(), fingerprint, ciphertext, hmac))
                            .isEqualTo(1);
                } finally {
                    connection.rollback(directRefusal);
                }
                Savepoint inboundResolve = connection.setSavepoint("s1fix1-inbound-resolve");
                try {
                    assertThat(executeUpdate(
                            connection,
                            "INSERT INTO ivr.ivr_decision "
                                    + "(id, session_id, call_id, purpose, request_fingerprint, "
                                    + "action, reason_code, response_snapshot, "
                                    + "response_payload_ciphertext, response_sha256, key_version) "
                                    + "VALUES (?, ?, ?, 'INBOUND_RESOLVE', ?, 'GUIDE_ACTIVATION', "
                                    + "'ACTIVATION_REQUIRED', '{}'::jsonb, ?, ?, 1)",
                            UUID.randomUUID(), sessionId, callId, fingerprint, ciphertext, hmac))
                            .isEqualTo(1);
                } finally {
                    connection.rollback(inboundResolve);
                }
                Savepoint activationNext = connection.setSavepoint("s1fix1-activation-next");
                try {
                    assertThat(executeUpdate(
                            connection,
                            "INSERT INTO ivr.ivr_decision "
                                    + "(id, session_id, call_id, purpose, source_event_domain, "
                                    + "source_event_id, request_fingerprint, action, reason_code, "
                                    + "response_snapshot, response_payload_ciphertext, "
                                    + "response_sha256, key_version) "
                                    + "VALUES (?, ?, ?, 'ACTIVATION_NEXT_ACTION', 'IVR_CONFIRM', "
                                    + "?, ?, 'GUIDE_PURCHASE', 'CONSENT_REQUIRED', '{}'::jsonb, "
                                    + "?, ?, 1)",
                            UUID.randomUUID(), sessionId, callId, "evt-confirm-" + UUID.randomUUID(),
                            fingerprint, ciphertext, hmac)).isEqualTo(1);
                } finally {
                    connection.rollback(activationNext);
                }

                String eventInsertPrefix =
                        "INSERT INTO ivr.call_event "
                                + "(id, call_id, event_id, event_type, occurred_at, "
                                + "payload_ciphertext, payload_redacted, payload_sha256) "
                                + "VALUES (?, ?, ?, 'SESSION_START', now(), ?, ?::jsonb, ?)";

                // F-PA5-S1-02 negative: JSON-null source.payload with a matching receipt
                // hash must be rejected by ck_call_event_payload_redacted only.
                expectSqlFailure(connection, "23514", "ck_call_event_payload_redacted",
                        eventInsertPrefix,
                        UUID.randomUUID(), callId, "evt-null-payload-" + UUID.randomUUID(),
                        ciphertext,
                        "{\"source\":{\"eventType\":\"SESSION_START\",\"legId\":\"leg-1\","
                                + "\"sequenceNo\":1,\"payload\":null},\"receipt\":{"
                                + "\"payloadSha256\":\"" + payloadSha + "\","
                                + "\"eventFingerprint\":\"" + eventFingerprint + "\"}}",
                        payloadSha);
                // F-PA5-S1-02 negative: non-object (string) source.payload.
                expectSqlFailure(connection, "23514", "ck_call_event_payload_redacted",
                        eventInsertPrefix,
                        UUID.randomUUID(), callId, "evt-string-payload-" + UUID.randomUUID(),
                        ciphertext,
                        "{\"source\":{\"eventType\":\"SESSION_START\",\"legId\":\"leg-1\","
                                + "\"sequenceNo\":1,\"payload\":\"not-an-object\"},\"receipt\":{"
                                + "\"payloadSha256\":\"" + payloadSha + "\","
                                + "\"eventFingerprint\":\"" + eventFingerprint + "\"}}",
                        payloadSha);
                // F-PA5-S1-02 negative: JSON-null receipt.payloadSha256 beside a valid
                // 64-hex payload_sha256 column value.
                expectSqlFailure(connection, "23514", "ck_call_event_payload_redacted",
                        eventInsertPrefix,
                        UUID.randomUUID(), callId, "evt-null-hash-" + UUID.randomUUID(),
                        ciphertext,
                        "{\"source\":{\"eventType\":\"SESSION_START\",\"legId\":\"leg-1\","
                                + "\"sequenceNo\":1,\"payload\":{}},\"receipt\":{"
                                + "\"payloadSha256\":null,"
                                + "\"eventFingerprint\":\"" + eventFingerprint + "\"}}",
                        payloadSha);

                // Legal event shape: object payload with non-NULL matching receipt hash.
                Savepoint eventProbe = connection.setSavepoint("s1fix1-event-valid");
                try {
                    assertThat(executeUpdate(
                            connection,
                            eventInsertPrefix,
                            UUID.randomUUID(), callId, "evt-valid-" + UUID.randomUUID(),
                            ciphertext,
                            "{\"source\":{\"eventType\":\"SESSION_START\",\"legId\":\"leg-1\","
                                    + "\"sequenceNo\":1,\"payload\":{}},\"receipt\":{"
                                    + "\"payloadSha256\":\"" + payloadSha + "\","
                                    + "\"eventFingerprint\":\"" + eventFingerprint + "\"}}",
                            payloadSha)).isEqualTo(1);
                } finally {
                    connection.rollback(eventProbe);
                }
            } finally {
                connection.rollback();
            }
        }
    }

    private static void assertV004TablesColumnsChecksIndexesFks(Connection connection)
            throws SQLException {
        assertThat(queryStrings(
                connection,
                "SELECT table_schema || '.' || table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'catalog_order' ORDER BY 1"))
                .containsExactlyElementsOf(V004_TABLES);

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.columns WHERE table_schema = 'catalog_order'"))
                .as("total V004 columns").isEqualTo(78);

        Map<String, String> v004ColumnOrder = new java.util.LinkedHashMap<>();
        v004ColumnOrder.put("catalog_product",
                "id,product_code,product_family,created_at");
        v004ColumnOrder.put("catalog_version",
                "id,product_id,version_no,sale_status,offer_type,audience,price_cents,currency,"
                        + "billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,"
                        + "created_by_employee_id,created_at,published_at,revision");
        v004ColumnOrder.put("catalog_offer",
                "id,catalog_version_id,channel,offer_status,version_token,created_at,revision");
        v004ColumnOrder.put("catalog_benefit_line",
                "id,catalog_version_id,entitlement_type,quantity,validity_days,grant_interval_months");
        v004ColumnOrder.put("customer_order",
                "id,order_no,account_id,owner_organization_id,subject_id,channel,catalog_version_id,"
                        + "catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,"
                        + "snapshot,snapshot_sha256,version_token,idempotency_key,request_fingerprint,"
                        + "revision,created_at,paid_at,closed_at");
        v004ColumnOrder.put("payment_order",
                "id,order_id,provider,merchant_order_no,provider_transaction_id,status,amount_cents,"
                        + "currency,created_at,updated_at,revision");
        v004ColumnOrder.put("payment_event",
                "id,payment_order_id,provider,provider_event_id,event_type,signature_verified,"
                        + "payload_ciphertext,payload_sha256,transaction_id,amount_cents,occurred_at,"
                        + "received_at");
        Map<String, List<String>> v004ColumnDetails = new java.util.LinkedHashMap<>();
        v004ColumnDetails.put("catalog_product", List.of(
                "id|uuid|NO|<none>",
                "product_code|text|NO|<none>",
                "product_family|text|NO|<none>",
                "created_at|timestamp with time zone|NO|now()"));
        v004ColumnDetails.put("catalog_version", List.of(
                "id|uuid|NO|<none>",
                "product_id|uuid|NO|<none>",
                "version_no|integer|NO|<none>",
                "sale_status|text|NO|'DRAFT'::text",
                "offer_type|text|NO|<none>",
                "audience|text|NO|<none>",
                "price_cents|bigint|YES|<none>",
                "currency|text|NO|<none>",
                "billing_cycle|text|NO|<none>",
                "installments|integer|NO|<none>",
                "grant_interval_months|integer|NO|<none>",
                "snapshot|jsonb|NO|<none>",
                "snapshot_sha256|text|NO|<none>",
                "created_by_employee_id|uuid|NO|<none>",
                "created_at|timestamp with time zone|NO|now()",
                "published_at|timestamp with time zone|YES|<none>",
                "revision|integer|NO|1"));
        v004ColumnDetails.put("catalog_offer", List.of(
                "id|uuid|NO|<none>",
                "catalog_version_id|uuid|NO|<none>",
                "channel|text|NO|<none>",
                "offer_status|text|NO|'ACTIVE'::text",
                "version_token|text|NO|<none>",
                "created_at|timestamp with time zone|NO|now()",
                "revision|integer|NO|1"));
        v004ColumnDetails.put("catalog_benefit_line", List.of(
                "id|uuid|NO|<none>",
                "catalog_version_id|uuid|NO|<none>",
                "entitlement_type|text|NO|<none>",
                "quantity|bigint|NO|<none>",
                "validity_days|integer|NO|<none>",
                "grant_interval_months|integer|NO|<none>"));
        v004ColumnDetails.put("customer_order", List.of(
                "id|uuid|NO|<none>",
                "order_no|text|NO|<none>",
                "account_id|uuid|NO|<none>",
                "owner_organization_id|uuid|YES|<none>",
                "subject_id|uuid|NO|<none>",
                "channel|text|NO|<none>",
                "catalog_version_id|uuid|NO|<none>",
                "catalog_offer_id|uuid|NO|<none>",
                "purchase_consent_id|uuid|NO|<none>",
                "amount_cents|bigint|NO|<none>",
                "currency|text|NO|<none>",
                "payment_status|text|NO|'UNPAID'::text",
                "snapshot|jsonb|NO|<none>",
                "snapshot_sha256|text|NO|<none>",
                "version_token|text|NO|<none>",
                "idempotency_key|text|NO|<none>",
                "request_fingerprint|text|NO|<none>",
                "revision|integer|NO|1",
                "created_at|timestamp with time zone|NO|now()",
                "paid_at|timestamp with time zone|YES|<none>",
                "closed_at|timestamp with time zone|YES|<none>"));
        v004ColumnDetails.put("payment_order", List.of(
                "id|uuid|NO|<none>",
                "order_id|uuid|NO|<none>",
                "provider|text|NO|<none>",
                "merchant_order_no|text|NO|<none>",
                "provider_transaction_id|text|YES|<none>",
                "status|text|NO|'UNPAID'::text",
                "amount_cents|bigint|NO|<none>",
                "currency|text|NO|<none>",
                "created_at|timestamp with time zone|NO|now()",
                "updated_at|timestamp with time zone|NO|now()",
                "revision|integer|NO|1"));
        v004ColumnDetails.put("payment_event", List.of(
                "id|uuid|NO|<none>",
                "payment_order_id|uuid|NO|<none>",
                "provider|text|NO|<none>",
                "provider_event_id|text|NO|<none>",
                "event_type|text|NO|<none>",
                "signature_verified|boolean|NO|false",
                "payload_ciphertext|text|NO|<none>",
                "payload_sha256|text|NO|<none>",
                "transaction_id|text|YES|<none>",
                "amount_cents|bigint|NO|<none>",
                "occurred_at|timestamp with time zone|NO|<none>",
                "received_at|timestamp with time zone|NO|now()"));
        for (Map.Entry<String, String> entry : v004ColumnOrder.entrySet()) {
            assertThat(queryStrings(
                    connection,
                    "SELECT string_agg(column_name, ',' ORDER BY ordinal_position) FROM information_schema.columns "
                            + "WHERE table_schema = 'catalog_order' AND table_name = ?",
                    entry.getKey()))
                    .as("columns of catalog_order.%s", entry.getKey())
                    .containsExactly(entry.getValue());
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM information_schema.columns "
                            + "WHERE table_schema = 'catalog_order' AND table_name = ?",
                    entry.getKey()))
                    .as("column count of catalog_order.%s", entry.getKey())
                    .isEqualTo(entry.getValue().split(",").length);
            assertThat(queryStrings(
                    connection,
                    "SELECT column_name || '|' || data_type || '|' || is_nullable || '|' "
                            + "|| coalesce(column_default, '<none>') FROM information_schema.columns "
                            + "WHERE table_schema = 'catalog_order' AND table_name = ? ORDER BY ordinal_position",
                    entry.getKey()))
                    .as("exact column rows of catalog_order.%s", entry.getKey())
                    .containsExactlyElementsOf(v004ColumnDetails.get(entry.getKey()));
        }
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_attrdef d "
                        + "JOIN pg_attribute a ON a.attrelid = d.adrelid AND a.attnum = d.adnum "
                        + "JOIN pg_class r ON r.oid = d.adrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'catalog_order'"))
                .as("total V004 defaulted columns")
                .isEqualTo(16);

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND c.contype = 'c'"))
                .as("total V004 named checks").isEqualTo(51);

        Map<String, List<String>> v004CheckNames = new java.util.LinkedHashMap<>();
        v004CheckNames.put("catalog_product", List.of(
                "ck_catalog_product_code",
                "ck_catalog_product_family"));
        v004CheckNames.put("catalog_version", List.of(
                "ck_catalog_version_audience",
                "ck_catalog_version_audience_shape",
                "ck_catalog_version_billing_matrix",
                "ck_catalog_version_currency",
                "ck_catalog_version_no",
                "ck_catalog_version_offer_type",
                "ck_catalog_version_price",
                "ck_catalog_version_published_shape",
                "ck_catalog_version_revision",
                "ck_catalog_version_sale_status",
                "ck_catalog_version_snapshot_sha",
                "ck_catalog_version_snapshot_type"));
        v004CheckNames.put("catalog_offer", List.of(
                "ck_catalog_offer_channel",
                "ck_catalog_offer_revision",
                "ck_catalog_offer_status",
                "ck_catalog_offer_token"));
        v004CheckNames.put("catalog_benefit_line", List.of(
                "ck_catalog_benefit_interval",
                "ck_catalog_benefit_quantity",
                "ck_catalog_benefit_type",
                "ck_catalog_benefit_validity"));
        v004CheckNames.put("customer_order", List.of(
                "ck_customer_order_amount",
                "ck_customer_order_channel",
                "ck_customer_order_currency",
                "ck_customer_order_fingerprint",
                "ck_customer_order_idempotency",
                "ck_customer_order_no",
                "ck_customer_order_payment_shape",
                "ck_customer_order_payment_status",
                "ck_customer_order_revision",
                "ck_customer_order_snapshot_sha",
                "ck_customer_order_snapshot_type",
                "ck_customer_order_time_order",
                "ck_customer_order_version_token"));
        v004CheckNames.put("payment_order", List.of(
                "ck_payment_order_amount",
                "ck_payment_order_currency",
                "ck_payment_order_merchant_no",
                "ck_payment_order_paid_shape",
                "ck_payment_order_provider",
                "ck_payment_order_revision",
                "ck_payment_order_status",
                "ck_payment_order_time_order",
                "ck_payment_order_transaction"));
        v004CheckNames.put("payment_event", List.of(
                "ck_payment_event_amount",
                "ck_payment_event_event_id",
                "ck_payment_event_payload_ciphertext",
                "ck_payment_event_payload_sha",
                "ck_payment_event_provider",
                "ck_payment_event_transaction",
                "ck_payment_event_type"));
        for (Map.Entry<String, List<String>> entry : v004CheckNames.entrySet()) {
            assertThat(queryStrings(
                    connection,
                    "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'catalog_order' AND r.relname = ? AND c.contype = 'c' "
                            + "ORDER BY conname",
                    entry.getKey()))
                    .as("check names of catalog_order.%s", entry.getKey())
                    .containsExactlyElementsOf(entry.getValue());
        }

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND c.contype = 'p' ORDER BY 1"))
                .containsExactly(
                        "pk_catalog_benefit_line",
                        "pk_catalog_offer",
                        "pk_catalog_product",
                        "pk_catalog_version",
                        "pk_customer_order",
                        "pk_payment_event",
                        "pk_payment_order");
        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND c.contype = 'u' ORDER BY 1"))
                .containsExactly(
                        "uq_catalog_benefit_version_type",
                        "uq_catalog_offer_version_channel",
                        "uq_catalog_product_code",
                        "uq_catalog_version_no",
                        "uq_customer_order_idempotency",
                        "uq_customer_order_no",
                        "uq_payment_event_provider_event",
                        "uq_payment_order_merchant",
                        "uq_payment_order_order");

        assertThat(V004_PK_UNIQUE_DEFS).as("expected V004 PK/unique map size").hasSize(16);
        for (String row : V004_PK_UNIQUE_DEFS) {
            String[] expected = row.split("\\|", -1);
            List<String> actualDefs = queryStrings(
                    connection,
                    "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c "
                            + "JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'catalog_order' AND r.relname = ? AND c.conname = ? "
                            + "AND c.contype::text = ?",
                    expected[0], expected[1], expected[2]);
            assertThat(actualDefs)
                    .as("exactly one %s row for catalog_order.%s", expected[2], row)
                    .hasSize(1);
            assertThat(actualDefs.get(0))
                    .as("whole pg_get_constraintdef equality for catalog_order.%s", row)
                    .isEqualTo(expected[3]);
            assertThat(queryStrings(
                    connection,
                    "SELECT a.attname FROM pg_constraint c "
                            + "JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "JOIN unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord) ON true "
                            + "JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum "
                            + "WHERE n.nspname = 'catalog_order' AND r.relname = ? AND c.conname = ? "
                            + "AND c.contype::text = ? ORDER BY k.ord",
                    expected[0], expected[1], expected[2]))
                    .as("ordered key columns of catalog_order.%s", row)
                    .containsExactlyElementsOf(List.of(expected[4].split(",")));
        }

        assertThat(queryStrings(
                connection,
                "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND c.contype = 'f' ORDER BY conname"))
                .containsExactly(
                        "fk_catalog_benefit_version",
                        "fk_catalog_offer_version",
                        "fk_catalog_version_employee",
                        "fk_catalog_version_product",
                        "fk_customer_order_account",
                        "fk_customer_order_offer",
                        "fk_customer_order_organization",
                        "fk_customer_order_subject",
                        "fk_customer_order_version",
                        "fk_payment_event_order",
                        "fk_payment_order_order");

        assertThat(V004_FK_DEFS).as("expected V004 FK map size").hasSize(11);
        for (String row : V004_FK_DEFS) {
            String[] expected = row.split("\\|", -1);
            assertThat(queryString(
                    connection,
                    "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c "
                            + "JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'catalog_order' AND c.conname = ?",
                    expected[0]))
                    .as("FK def %s", expected[0])
                    .isEqualTo(expected[1]);
            assertThat(queryStrings(
                    connection,
                    "SELECT c.confupdtype::text || c.confdeltype::text || c.confmatchtype::text || '|' "
                            + "|| c.condeferrable::text || '|' || c.condeferred::text || '|' || c.convalidated::text "
                            + "FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'catalog_order' AND c.conname = ?",
                    expected[0]))
                    .as("FK properties %s", expected[0])
                    .containsExactly("rrs|false|false|true");
        }

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND c.conname = 'fk_customer_order_consent'"))
                .as("fk_customer_order_consent is deferred to V007")
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_schema = 'catalog_order' AND table_name = 'customer_order' "
                        + "AND column_name = 'purchase_consent_id' AND is_nullable = 'NO' "
                        + "AND column_default IS NULL"))
                .as("purchase_consent_id is NOT NULL with no default")
                .isEqualTo(1);
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM information_schema.key_column_usage k "
                        + "JOIN information_schema.table_constraints t "
                        + "ON t.constraint_name = k.constraint_name "
                        + "AND t.table_schema = k.table_schema AND t.table_name = k.table_name "
                        + "WHERE k.table_schema = 'catalog_order' AND k.table_name = 'customer_order' "
                        + "AND k.column_name = 'purchase_consent_id' "
                        + "AND t.constraint_type = 'FOREIGN KEY'"))
                .as("purchase_consent_id has no FK at V004")
                .isZero();

        assertThat(queryStrings(
                connection,
                "SELECT schemaname || '.' || tablename || '.' || indexname FROM pg_indexes "
                        + "WHERE schemaname = 'catalog_order' ORDER BY 1"))
                .containsExactly(
                        "catalog_order.catalog_benefit_line.ix_catalog_benefit_version",
                        "catalog_order.catalog_benefit_line.pk_catalog_benefit_line",
                        "catalog_order.catalog_benefit_line.uq_catalog_benefit_version_type",
                        "catalog_order.catalog_offer.ix_catalog_offer_channel_status",
                        "catalog_order.catalog_offer.pk_catalog_offer",
                        "catalog_order.catalog_offer.uq_catalog_offer_version_channel",
                        "catalog_order.catalog_product.ix_catalog_product_family",
                        "catalog_order.catalog_product.pk_catalog_product",
                        "catalog_order.catalog_product.uq_catalog_product_code",
                        "catalog_order.catalog_version.ix_catalog_version_product_status",
                        "catalog_order.catalog_version.pk_catalog_version",
                        "catalog_order.catalog_version.uq_catalog_version_active",
                        "catalog_order.catalog_version.uq_catalog_version_no",
                        "catalog_order.customer_order.ix_customer_order_organization",
                        "catalog_order.customer_order.ix_customer_order_subject_status",
                        "catalog_order.customer_order.ix_customer_order_version",
                        "catalog_order.customer_order.pk_customer_order",
                        "catalog_order.customer_order.uq_customer_order_idempotency",
                        "catalog_order.customer_order.uq_customer_order_no",
                        "catalog_order.payment_event.ix_payment_event_order_time",
                        "catalog_order.payment_event.ix_payment_event_verified",
                        "catalog_order.payment_event.pk_payment_event",
                        "catalog_order.payment_event.uq_payment_event_provider_event",
                        "catalog_order.payment_order.ix_payment_order_status_time",
                        "catalog_order.payment_order.pk_payment_order",
                        "catalog_order.payment_order.uq_payment_order_merchant",
                        "catalog_order.payment_order.uq_payment_order_order",
                        "catalog_order.payment_order.uq_payment_order_transaction");

        assertThat(V004_INDEX_DEFS).as("expected V004 indexdef list size").hasSize(28);
        List<String> actualIndexDefs = queryStrings(
                connection,
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'catalog_order' ORDER BY indexdef");
        assertThat(actualIndexDefs)
                .as("exact V004 indexdef set")
                .containsExactlyInAnyOrderElementsOf(V004_INDEX_DEFS);

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND c.relkind = 'S'"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND c.relkind = 'r'"))
                .isEqualTo(7);

        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || r.relname || '=' || pg_get_userbyid(r.relowner) "
                        + "FROM pg_class r JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND r.relkind = 'r' ORDER BY 1"))
                .containsExactly(
                        "catalog_order.catalog_benefit_line=catalog_order_owner",
                        "catalog_order.catalog_offer=catalog_order_owner",
                        "catalog_order.catalog_product=catalog_order_owner",
                        "catalog_order.catalog_version=catalog_order_owner",
                        "catalog_order.customer_order=catalog_order_owner",
                        "catalog_order.payment_event=catalog_order_owner",
                        "catalog_order.payment_order=catalog_order_owner");

        assertThat(queryStrings(
                connection,
                "SELECT rolname FROM pg_roles WHERE rolname IN "
                        + "('catalog_order_owner','catalog_order_role','ivr_owner','ivr_role',"
                        + "'entitlement_metering_owner','entitlement_metering_role',"
                        + "'activation_consent_owner','activation_consent_role') "
                        + "AND (rolcanlogin OR rolsuper OR rolcreatedb OR rolcreaterole)"))
                .as("all eight V004 roles are NOLOGIN/NOSUPERUSER/NOCREATEDB/NOCREATEROLE")
                .isEmpty();
        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '=' || pg_get_userbyid(n.nspowner) FROM pg_namespace n "
                        + "WHERE nspname IN ('catalog_order','ivr','entitlement_metering','activation_consent') "
                        + "ORDER BY nspname"))
                .containsExactly(
                        "activation_consent=activation_consent_owner",
                        "catalog_order=catalog_order_owner",
                        "entitlement_metering=entitlement_metering_owner",
                        "ivr=ivr_owner");
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_auth_members m "
                        + "JOIN pg_roles g ON g.oid = m.roleid "
                        + "JOIN pg_roles r ON r.oid = m.member "
                        + "WHERE g.rolname IN ('catalog_order_owner','catalog_order_role','ivr_owner','ivr_role',"
                        + "'entitlement_metering_owner','entitlement_metering_role',"
                        + "'activation_consent_owner','activation_consent_role') "
                        + "OR r.rolname IN ('catalog_order_owner','catalog_order_role','ivr_owner','ivr_role',"
                        + "'entitlement_metering_owner','entitlement_metering_role',"
                        + "'activation_consent_owner','activation_consent_role')"))
                .as("no role membership among the eight V004 roles")
                .isZero();
    }

    private static void assertV004CheckPredicateInventory(Connection connection) throws SQLException {
        assertThat(V004_CHECK_DEF_MAP).as("expected V004 check map tables").hasSize(7);
        int totalChecks = 0;
        for (Map.Entry<String, Map<String, String>> tableEntry : V004_CHECK_DEF_MAP.entrySet()) {
            String table = tableEntry.getKey();
            totalChecks += tableEntry.getValue().size();
            List<String> actualNames = queryStrings(
                    connection,
                    "SELECT conname FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                            + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                            + "WHERE n.nspname = 'catalog_order' AND r.relname = ? AND c.contype = 'c' "
                            + "ORDER BY conname",
                    table);
            List<String> expectedNames = new ArrayList<>(tableEntry.getValue().keySet());
            expectedNames.sort(String::compareTo);
            assertThat(actualNames)
                    .as("exact check-name set for catalog_order.%s", table)
                    .containsExactlyElementsOf(expectedNames);

            for (Map.Entry<String, String> checkEntry : tableEntry.getValue().entrySet()) {
                String checkName = checkEntry.getKey();
                List<String> actualDefs = queryStrings(
                        connection,
                        "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c "
                                + "JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = 'catalog_order' AND r.relname = ? "
                                + "AND c.contype = 'c' AND c.conname = ?",
                        table, checkName);
                assertThat(actualDefs)
                        .as("exactly one row for catalog_order.%s.%s", table, checkName)
                        .hasSize(1);
                assertThat(normalizeCheckDef(actualDefs.get(0)))
                        .as("whole pg_get_constraintdef equality for catalog_order.%s.%s",
                                table, checkName)
                        .isEqualTo(normalizeCheckDef(checkEntry.getValue()));
            }
        }
        assertThat(totalChecks).as("total V004 checks").isEqualTo(51);
    }

    private static void assertV004FunctionsAndTriggersExact(Connection connection) throws SQLException {
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'catalog_order'"))
                .as("exactly five catalog_order functions").isEqualTo(5);
        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || p.proname || '(' || pg_get_function_identity_arguments(p.oid) || ')' "
                        + "|| '|' || p.prosecdef || '|' || pg_get_userbyid(p.proowner) || '|' "
                        + "|| coalesce(array_to_string(p.proconfig, ','), '') "
                        + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'catalog_order' ORDER BY 1"))
                .containsExactly(
                        "catalog_order.guard_catalog_offer()|false|catalog_order_owner"
                                + "|search_path=catalog_order, pg_temp",
                        "catalog_order.guard_catalog_version_published()|false|catalog_order_owner"
                                + "|search_path=catalog_order, pg_temp",
                        "catalog_order.guard_customer_order_snapshot()|false|catalog_order_owner"
                                + "|search_path=catalog_order, pg_temp",
                        "catalog_order.guard_payment_order_identity()|false|catalog_order_owner"
                                + "|search_path=catalog_order, pg_temp",
                        "catalog_order.reject_row_mutation()|false|catalog_order_owner"
                                + "|search_path=catalog_order, pg_temp");
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'catalog_order' "
                        + "AND EXISTS (SELECT 1 FROM unnest(p.proargnames) AS a WHERE a = 'p_id')"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'catalog_order' "
                        + "AND p.prosrc LIKE '%gen_random_uuid%'"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname = 'catalog_order' "
                        + "AND p.prosrc ~* '\\m(commit|rollback|start\\s+transaction|set\\s+transaction)\\M'"))
                .isZero();

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE NOT t.tgisinternal AND n.nspname = 'catalog_order'"))
                .as("exactly seven catalog_order triggers").isEqualTo(7);

        Map<String, String[]> v004Triggers = new java.util.LinkedHashMap<>();
        // expected: [0]=table, [1]=function, [2]=before, [3]=row, [4]=update, [5]=delete
        v004Triggers.put("trg_catalog_product_immutable", new String[] {
                "catalog_order.catalog_product", "catalog_order.reject_row_mutation()",
                "1", "1", "1", "1"});
        v004Triggers.put("trg_catalog_version_guard", new String[] {
                "catalog_order.catalog_version", "catalog_order.guard_catalog_version_published()",
                "1", "1", "1", "0"});
        v004Triggers.put("trg_catalog_offer_guard", new String[] {
                "catalog_order.catalog_offer", "catalog_order.guard_catalog_offer()",
                "1", "1", "1", "0"});
        v004Triggers.put("trg_catalog_benefit_line_immutable", new String[] {
                "catalog_order.catalog_benefit_line", "catalog_order.reject_row_mutation()",
                "1", "1", "1", "1"});
        v004Triggers.put("trg_customer_order_guard", new String[] {
                "catalog_order.customer_order", "catalog_order.guard_customer_order_snapshot()",
                "1", "1", "1", "0"});
        v004Triggers.put("trg_payment_order_guard", new String[] {
                "catalog_order.payment_order", "catalog_order.guard_payment_order_identity()",
                "1", "1", "1", "0"});
        v004Triggers.put("trg_payment_event_immutable", new String[] {
                "catalog_order.payment_event", "catalog_order.reject_row_mutation()",
                "1", "1", "1", "1"});
        for (Map.Entry<String, String[]> entry : v004Triggers.entrySet()) {
            String[] expected = entry.getValue();
            try (PreparedStatement prepared = connection.prepareStatement(
                    "SELECT t.tgrelid = ?::regclass AS on_table, "
                            + "t.tgfoid = ?::regprocedure AS calls_function, "
                            + "(t.tgtype & 2) <> 0 AS is_before, "
                            + "(t.tgtype & 1) <> 0 AS is_row, "
                            + "(t.tgtype & 16) <> 0 AS has_update, "
                            + "(t.tgtype & 8) <> 0 AS has_delete, "
                            + "(t.tgtype & 4) <> 0 AS has_insert, "
                            + "(t.tgtype & 32) <> 0 AS has_truncate "
                            + "FROM pg_trigger t WHERE t.tgname = ? AND NOT t.tgisinternal")) {
                prepared.setString(1, expected[0]);
                prepared.setString(2, expected[1]);
                prepared.setString(3, entry.getKey());
                try (ResultSet resultSet = prepared.executeQuery()) {
                    assertThat(resultSet.next()).as("trigger %s exists", entry.getKey()).isTrue();
                    assertThat(resultSet.getBoolean("on_table"))
                            .as("%s on %s", entry.getKey(), expected[0]).isTrue();
                    assertThat(resultSet.getBoolean("calls_function"))
                            .as("%s calls %s", entry.getKey(), expected[1]).isTrue();
                    assertThat(resultSet.getBoolean("is_before"))
                            .as("%s BEFORE", entry.getKey())
                            .isEqualTo("1".equals(expected[2]));
                    assertThat(resultSet.getBoolean("is_row"))
                            .as("%s FOR EACH ROW", entry.getKey())
                            .isEqualTo("1".equals(expected[3]));
                    assertThat(resultSet.getBoolean("has_update"))
                            .as("%s fires on UPDATE", entry.getKey())
                            .isEqualTo("1".equals(expected[4]));
                    assertThat(resultSet.getBoolean("has_delete"))
                            .as("%s fires on DELETE", entry.getKey())
                            .isEqualTo("1".equals(expected[5]));
                    assertThat(resultSet.getBoolean("has_insert"))
                            .as("%s no INSERT", entry.getKey()).isFalse();
                    assertThat(resultSet.getBoolean("has_truncate"))
                            .as("%s no TRUNCATE", entry.getKey()).isFalse();
                    assertThat(resultSet.next()).isFalse();
                }
            }
        }
    }

    private static void assertV004NoIdDefaultsOrSequences(Connection connection) throws SQLException {
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_attrdef d "
                        + "JOIN pg_attribute a ON a.attrelid = d.adrelid AND a.attnum = d.adnum "
                        + "JOIN pg_class r ON r.oid = d.adrelid "
                        + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND a.attname = 'id'"))
                .as("no V004 id column has a default").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = 'catalog_order' AND c.relkind = 'S'"))
                .as("no V004 sequence exists").isZero();
    }

    private static void assertV004SchemaUsageMatrix(Connection connection) throws SQLException {
        // F-PA4-S1-01 fix 1: complete effective USAGE/CREATE matrix (accepted plan 6.6.1,
        // 6.6.5 and 8.3). Every S1 role, every V004 role and PUBLIC is checked against the
        // four S2 schemas and the five S1 schemas touched by the V004 wave. Owners hold
        // USAGE and CREATE only on their own schema (inherent); no runtime or foreign
        // owner role holds CREATE anywhere; every grantee not enumerated below has
        // USAGE false, so an unlisted GRANT USAGE/CREATE fails the suite closed.
        Map<String, String> ownerBySchema = new java.util.LinkedHashMap<>();
        ownerBySchema.put("catalog_order", "catalog_order_owner");
        ownerBySchema.put("ivr", "ivr_owner");
        ownerBySchema.put("entitlement_metering", "entitlement_metering_owner");
        ownerBySchema.put("activation_consent", "activation_consent_owner");
        ownerBySchema.put("customer_iam", "customer_iam_owner");
        ownerBySchema.put("organization_access", "organization_access_owner");
        ownerBySchema.put("subject_number", "subject_number_owner");
        ownerBySchema.put("platform", "platform_reliability_owner");
        ownerBySchema.put("audit", "audit_owner");
        Map<String, List<String>> usageRolesBySchema = new java.util.LinkedHashMap<>();
        usageRolesBySchema.put("catalog_order", List.of(
                "catalog_order_owner", "catalog_order_role", "entitlement_metering_role"));
        usageRolesBySchema.put("ivr", List.of("ivr_owner", "ivr_role"));
        usageRolesBySchema.put("entitlement_metering", List.of(
                "entitlement_metering_owner", "entitlement_metering_role",
                "activation_consent_role", "ivr_role"));
        usageRolesBySchema.put("activation_consent", List.of(
                "activation_consent_owner", "activation_consent_role", "ivr_role",
                "catalog_order_role"));
        usageRolesBySchema.put("customer_iam", List.of(
                "customer_iam_owner", "customer_iam_role", "subject_number_role",
                "catalog_order_role", "ivr_role"));
        usageRolesBySchema.put("organization_access", List.of(
                "organization_access_owner", "organization_access_role", "customer_iam_role",
                "subject_number_role", "catalog_order_role", "ivr_role"));
        usageRolesBySchema.put("subject_number", List.of(
                "subject_number_owner", "subject_number_role",
                "subject_identity_verification_role", "catalog_order_role", "ivr_role"));
        usageRolesBySchema.put("platform", List.of(
                "platform_reliability_owner", "platform_reliability_role",
                "platform_dispatcher_role", "customer_iam_role", "organization_access_role",
                "operations_iam_role", "subject_number_role", "catalog_order_role", "ivr_role",
                "entitlement_metering_role", "activation_consent_role"));
        usageRolesBySchema.put("audit", List.of(
                "audit_owner", "customer_iam_role", "organization_access_role",
                "operations_iam_role", "subject_number_role",
                "subject_identity_verification_role", "operations_service_role",
                "audit_reader_role", "catalog_order_role", "ivr_role",
                "entitlement_metering_role", "activation_consent_role"));
        List<String> schemaGrantees = new ArrayList<>(S1_ROLES);
        schemaGrantees.addAll(V004_ROLES);
        schemaGrantees.add("public");
        for (Map.Entry<String, String> schemaEntry : ownerBySchema.entrySet()) {
            String schema = schemaEntry.getKey();
            String owner = schemaEntry.getValue();
            List<String> usageRoles = usageRolesBySchema.get(schema);
            for (String grantee : schemaGrantees) {
                boolean expectUsage = usageRoles.contains(grantee);
                boolean expectCreate = grantee.equals(owner);
                assertThat(hasSchema(connection, grantee, schema, "USAGE"))
                        .as("%s schema USAGE %s", grantee, schema).isEqualTo(expectUsage);
                assertThat(hasSchema(connection, grantee, schema, "CREATE"))
                        .as("%s schema CREATE %s", grantee, schema).isEqualTo(expectCreate);
            }
        }
        // Accepted V004 wave rules kept explicit for auditability.
        assertThat(hasSchema(connection, "catalog_order_role", "activation_consent", "USAGE"))
                .as("catalog_order_role activation_consent USAGE is granted at V004").isTrue();
        assertThat(hasSchema(connection, "catalog_order_role", "entitlement_metering", "USAGE"))
                .as("catalog_order_role first entitlement_metering USAGE is V006").isFalse();
        assertThat(hasSchema(connection, "catalog_order_role", "ivr", "USAGE"))
                .as("catalog_order_role ivr USAGE is not granted at V004").isFalse();
        assertThat(hasSchema(connection, "ivr_role", "ivr", "USAGE"))
                .as("ivr_role ivr USAGE is granted at V005").isTrue();
        assertThat(hasSchema(connection, "ivr_role", "catalog_order", "USAGE"))
                .as("ivr_role catalog_order USAGE is not granted in V005 (§10.12)").isFalse();
        assertThat(hasSchema(connection, "activation_consent_role", "subject_number", "USAGE"))
                .as("activation_consent_role S1 USAGE is deferred to V007").isFalse();
        assertThat(hasSchema(connection, "entitlement_metering_role", "subject_number", "USAGE"))
                .as("entitlement_metering_role subject USAGE is deferred to V006").isFalse();
    }

    private static void assertV004OwnedAndCrossModuleGrants(Connection connection) throws SQLException {
        // F-PA4-S1-01 fix 1: complete effective table/column matrix (accepted plan 6.6.2,
        // 6.6.3, 6.6.5 and 8.3) across all 15 S1 roles and all 8 V004 roles on the seven
        // V004 owned tables and the five V004-touched S1 cross-module tables, including
        // REFERENCES/TRIGGER and column-level INSERT/UPDATE denials. F-PA4-AGG-01 adds
        // one exact bounded loop over all 13 S1 universe tables x the eight S2 roles.
        // Owner-inherent privileges are modeled explicitly; catalog_order_role holds
        // table SELECT/INSERT and only the exact column UPDATE sets;
        // entitlement_metering_role holds only the exact cross-module column SELECT sets;
        // every other out-of-matrix table or column privilege is false, so a future
        // out-of-matrix grant fails closed.
        Map<String, List<String>> allColumnsByTable = new java.util.LinkedHashMap<>();
        allColumnsByTable.put("catalog_order.catalog_product", List.of(
                "id", "product_code", "product_family", "created_at"));
        allColumnsByTable.put("catalog_order.catalog_version", List.of(
                "id", "product_id", "version_no", "sale_status", "offer_type", "audience",
                "price_cents", "currency", "billing_cycle", "installments", "grant_interval_months",
                "snapshot", "snapshot_sha256", "created_by_employee_id", "created_at",
                "published_at", "revision"));
        allColumnsByTable.put("catalog_order.catalog_offer", List.of(
                "id", "catalog_version_id", "channel", "offer_status", "version_token",
                "created_at", "revision"));
        allColumnsByTable.put("catalog_order.catalog_benefit_line", List.of(
                "id", "catalog_version_id", "entitlement_type", "quantity", "validity_days",
                "grant_interval_months"));
        allColumnsByTable.put("catalog_order.customer_order", List.of(
                "id", "order_no", "account_id", "owner_organization_id", "subject_id", "channel",
                "catalog_version_id", "catalog_offer_id", "purchase_consent_id", "amount_cents",
                "currency", "payment_status", "snapshot", "snapshot_sha256", "version_token",
                "idempotency_key", "request_fingerprint", "revision", "created_at", "paid_at",
                "closed_at"));
        allColumnsByTable.put("catalog_order.payment_order", List.of(
                "id", "order_id", "provider", "merchant_order_no", "provider_transaction_id",
                "status", "amount_cents", "currency", "created_at", "updated_at", "revision"));
        allColumnsByTable.put("catalog_order.payment_event", List.of(
                "id", "payment_order_id", "provider", "provider_event_id", "event_type",
                "signature_verified", "payload_ciphertext", "payload_sha256", "transaction_id",
                "amount_cents", "occurred_at", "received_at"));

        Map<String, List<String>> runtimeUpdateColumns = new java.util.LinkedHashMap<>();
        runtimeUpdateColumns.put("catalog_order.catalog_product", List.of());
        runtimeUpdateColumns.put("catalog_order.catalog_version", List.of(
                "sale_status", "offer_type", "audience", "price_cents", "currency",
                "billing_cycle", "installments", "grant_interval_months", "snapshot",
                "snapshot_sha256", "published_at", "revision"));
        runtimeUpdateColumns.put("catalog_order.catalog_offer", List.of(
                "offer_status", "version_token", "revision"));
        runtimeUpdateColumns.put("catalog_order.catalog_benefit_line", List.of());
        runtimeUpdateColumns.put("catalog_order.customer_order", List.of(
                "payment_status", "paid_at", "closed_at", "revision"));
        runtimeUpdateColumns.put("catalog_order.payment_order", List.of(
                "status", "provider_transaction_id", "updated_at", "revision"));
        runtimeUpdateColumns.put("catalog_order.payment_event", List.of());

        Map<String, List<String>> meteringCrossSelect = new java.util.LinkedHashMap<>();
        meteringCrossSelect.put("catalog_order.customer_order", List.of(
                "id", "account_id", "owner_organization_id", "subject_id", "catalog_version_id",
                "snapshot", "snapshot_sha256", "payment_status", "currency", "created_at"));
        meteringCrossSelect.put("catalog_order.payment_order", List.of(
                "id", "order_id", "status", "amount_cents", "currency", "updated_at"));
        meteringCrossSelect.put("catalog_order.payment_event", List.of(
                "id", "payment_order_id", "provider", "provider_event_id", "event_type",
                "signature_verified", "amount_cents", "occurred_at"));

        Map<String, List<String>> catalogCrossSelect = new java.util.LinkedHashMap<>();
        catalogCrossSelect.put("customer_iam.customer_account", List.of("id", "status"));
        catalogCrossSelect.put("customer_iam.identity_profile",
                List.of("account_id", "real_name_status"));
        catalogCrossSelect.put("organization_access.organization", List.of("id", "status"));
        catalogCrossSelect.put("organization_access.organization_membership",
                List.of("organization_id", "account_id", "role", "status"));
        catalogCrossSelect.put("subject_number.business_subject",
                List.of("id", "owner_account_id", "owner_organization_id", "revision"));

        Map<String, List<String>> ivrCrossSelect = new java.util.LinkedHashMap<>();
        ivrCrossSelect.put("customer_iam.customer_account", List.of("id", "status"));
        ivrCrossSelect.put("organization_access.organization", List.of("id", "status"));
        ivrCrossSelect.put("organization_access.organization_membership",
                List.of("organization_id", "account_id", "role", "status"));
        ivrCrossSelect.put("subject_number.business_subject",
                List.of("id", "owner_account_id", "owner_organization_id", "revision"));
        ivrCrossSelect.put("subject_number.caller_number_binding",
                List.of("id", "subject_id", "number_hmac", "status", "verified_at", "revision"));
        ivrCrossSelect.put("subject_number.subject_risk_state",
                List.of("caller_number_binding_id", "risk_type", "state", "revision"));

        assertThat(runtimeUpdateColumns.get("catalog_order.catalog_version")).hasSize(12);
        assertThat(runtimeUpdateColumns.get("catalog_order.catalog_offer")).hasSize(3);
        assertThat(runtimeUpdateColumns.get("catalog_order.customer_order")).hasSize(4);
        assertThat(runtimeUpdateColumns.get("catalog_order.payment_order")).hasSize(4);
        assertThat(meteringCrossSelect.get("catalog_order.customer_order")).hasSize(10);
        assertThat(meteringCrossSelect.get("catalog_order.payment_order")).hasSize(6);
        assertThat(meteringCrossSelect.get("catalog_order.payment_event")).hasSize(8);

        List<String> matrixRoles = new ArrayList<>(S1_ROLES);
        matrixRoles.addAll(V004_ROLES);
        for (String table : V004_TABLES) {
            for (String role : matrixRoles) {
                boolean ownerRow = role.equals("catalog_order_owner");
                boolean catalogRuntimeRow = role.equals("catalog_order_role");
                boolean meteringRow = role.equals("entitlement_metering_role");
                List<String> meteringColumns = meteringCrossSelect.getOrDefault(table, List.of());
                assertThat(hasTable(connection, role, table, "SELECT"))
                        .as("matrix %s table SELECT %s", role, table)
                        .isEqualTo(ownerRow || catalogRuntimeRow);
                assertThat(hasTable(connection, role, table, "INSERT"))
                        .as("matrix %s table INSERT %s", role, table)
                        .isEqualTo(ownerRow || catalogRuntimeRow);
                assertThat(hasTable(connection, role, table, "UPDATE"))
                        .as("matrix %s table UPDATE %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "DELETE"))
                        .as("matrix %s table DELETE %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "TRUNCATE"))
                        .as("matrix %s table TRUNCATE %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "REFERENCES"))
                        .as("matrix %s table REFERENCES %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "TRIGGER"))
                        .as("matrix %s table TRIGGER %s", role, table)
                        .isEqualTo(ownerRow);
                for (String column : allColumnsByTable.get(table)) {
                    boolean expectColumnSelect = ownerRow || catalogRuntimeRow
                            || (meteringRow && meteringColumns.contains(column));
                    boolean expectColumnInsert = ownerRow || catalogRuntimeRow;
                    boolean expectColumnUpdate = ownerRow
                            || (catalogRuntimeRow
                                    && runtimeUpdateColumns.get(table).contains(column));
                    assertThat(hasColumn(connection, role, table, column, "SELECT"))
                            .as("matrix %s column SELECT %s.%s", role, table, column)
                            .isEqualTo(expectColumnSelect);
                    assertThat(hasColumn(connection, role, table, column, "INSERT"))
                            .as("matrix %s column INSERT %s.%s", role, table, column)
                            .isEqualTo(expectColumnInsert);
                    assertThat(hasColumn(connection, role, table, column, "UPDATE"))
                            .as("matrix %s column UPDATE %s.%s", role, table, column)
                            .isEqualTo(expectColumnUpdate);
                }
            }
        }

        // F-PA4-AGG-01: one exact bounded loop over the full S1 universe
        // (S1_TABLES + V002_TABLES + V003_TABLES, exactly equal to ALL_V001_V003_TABLES,
        // 13 tables) crossed with the eight S2 roles (V004_ROLES). Every table privilege
        // (SELECT/INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER) is false for every
        // pair; every column SELECT/INSERT/UPDATE is false except catalog_order_role's
        // exact column SELECT reads on the five V004-touched S1 tables declared in
        // catalogCrossSelect (accepted plan 6.6.3, 6.6.5 and 8.3; includes the prior
        // RR-09 owner-role denials). Columns are enumerated from the live catalog and
        // must be non-empty, so a missing table or column fails closed instead of being
        // skipped; the expected table set is asserted exactly. No live over-grant exists
        // in V004, so this is fail-closed evidence only.
        List<String> s1UniverseTables = new ArrayList<>(S1_TABLES);
        s1UniverseTables.addAll(V002_TABLES);
        s1UniverseTables.addAll(V003_TABLES);
        assertThat(s1UniverseTables)
                .containsExactlyInAnyOrderElementsOf(ALL_V001_V003_TABLES);
        assertThat(s1UniverseTables).hasSize(13);
        for (String table : s1UniverseTables) {
            List<String> actualColumns = queryStrings(
                    connection,
                    "SELECT a.attname FROM pg_class c "
                            + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                            + "JOIN pg_attribute a ON a.attrelid = c.oid "
                            + "WHERE n.nspname || '.' || c.relname = ? "
                            + "AND a.attnum > 0 AND NOT a.attisdropped ORDER BY a.attnum",
                    table);
            assertThat(actualColumns)
                    .as("S1 universe table %s must exist with enumerated columns", table)
                    .isNotEmpty();
            List<String> catalogGrantedColumns =
                    catalogCrossSelect.getOrDefault(table, List.of());
            List<String> ivrGrantedColumns = ivrCrossSelect.getOrDefault(table, List.of());
            for (String role : V004_ROLES) {
                for (String privilege : List.of("SELECT", "INSERT", "UPDATE", "DELETE",
                        "TRUNCATE", "REFERENCES", "TRIGGER")) {
                    assertThat(hasTable(connection, role, table, privilege))
                            .as("%s must not hold table %s on S1 table %s",
                                    role, privilege, table)
                            .isFalse();
                }
                for (String column : actualColumns) {
                    for (String privilege : List.of("SELECT", "INSERT", "UPDATE")) {
                        boolean expectGrant = (role.equals("catalog_order_role")
                                && privilege.equals("SELECT")
                                && catalogGrantedColumns.contains(column))
                                || (role.equals("ivr_role")
                                        && privilege.equals("SELECT")
                                        && ivrGrantedColumns.contains(column));
                        assertThat(hasColumn(connection, role, table, column, privilege))
                                .as("%s %s %s.%s", role, privilege, table, column)
                                .isEqualTo(expectGrant);
                    }
                }
            }
        }
        // activation_consent/entitlement_metering tables do not exist at V004; the schema
        // USAGE matrix asserts the catalog_order_role wave boundary for those schemas.
    }

    private static void assertV004PublicAclIsZero(Connection connection) throws SQLException {
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_namespace n, "
                        + "aclexplode(coalesce(n.nspacl, acldefault('n', n.nspowner))) a "
                        + "WHERE n.nspname IN ('catalog_order','ivr','entitlement_metering','activation_consent') "
                        + "AND a.grantee = 0"))
                .as("PUBLIC has no V004 schema privilege").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace, "
                        + "aclexplode(coalesce(c.relacl, acldefault('r', c.relowner))) a "
                        + "WHERE n.nspname = 'catalog_order' AND c.relkind IN ('r','p') AND a.grantee = 0"))
                .as("PUBLIC has no V004 table privilege").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace, "
                        + "aclexplode(coalesce(p.proacl, acldefault('f', p.proowner))) a "
                        + "WHERE n.nspname = 'catalog_order' AND a.grantee = 0"))
                .as("PUBLIC has no V004 function privilege").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "JOIN pg_attribute a ON a.attrelid = c.oid, "
                        + "aclexplode(coalesce(a.attacl, acldefault('c', c.relowner))) x "
                        + "WHERE n.nspname = 'catalog_order' "
                        + "AND c.relkind IN ('r','p') "
                        + "AND a.attnum > 0 AND NOT a.attisdropped "
                        + "AND x.grantee = 0"))
                .as("PUBLIC has no V004 column privilege").isZero();
        // PA3 F-S1-01 pattern: touched S1 tables have zero PUBLIC column ACL after V004 grants
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "JOIN pg_attribute a ON a.attrelid = c.oid, "
                        + "aclexplode(coalesce(a.attacl, acldefault('c', c.relowner))) x "
                        + "WHERE ((n.nspname = 'customer_iam' AND c.relname IN "
                        + "('customer_account','identity_profile')) "
                        + "OR (n.nspname = 'organization_access' AND c.relname IN "
                        + "('organization','organization_membership')) "
                        + "OR (n.nspname = 'subject_number' AND c.relname = 'business_subject')) "
                        + "AND a.attnum > 0 AND NOT a.attisdropped "
                        + "AND x.grantee = 0"))
                .as("PUBLIC has no column ACL on V004-touched S1 tables").isZero();
    }

    private static void assertV004GuardFunctionExecuteBoundaries(Connection connection)
            throws SQLException {
        // F-PA4-S1-01 fix 1: complete EXECUTE matrix (accepted plan 6.6.4, 6.6.5 and 8.3).
        // catalog_order_owner holds owner-inherent EXECUTE on all five V004 functions;
        // catalog_order_role holds EXECUTE on the four guards only; every other S1 or V004
        // role and PUBLIC holds no EXECUTE on any guard or reject_row_mutation, so an
        // out-of-matrix guard or reject_row_mutation EXECUTE grant fails the suite closed.
        List<String> guardFunctions = List.of(
                "catalog_order.guard_catalog_version_published()",
                "catalog_order.guard_catalog_offer()",
                "catalog_order.guard_customer_order_snapshot()",
                "catalog_order.guard_payment_order_identity()");
        List<String> functionMatrixRoles = new ArrayList<>(S1_ROLES);
        functionMatrixRoles.addAll(V004_ROLES);
        for (String function : guardFunctions) {
            assertThat(hasFunction(connection, "catalog_order_owner", function, "EXECUTE"))
                    .as("catalog_order_owner EXECUTE %s", function).isTrue();
            assertThat(hasFunction(connection, "catalog_order_role", function, "EXECUTE"))
                    .as("catalog_order_role EXECUTE %s", function).isTrue();
            for (String role : functionMatrixRoles) {
                if (role.equals("catalog_order_owner") || role.equals("catalog_order_role")) {
                    continue;
                }
                assertThat(hasFunction(connection, role, function, "EXECUTE"))
                        .as("%s must not EXECUTE %s", role, function).isFalse();
            }
            assertThat(hasFunction(connection, "public", function, "EXECUTE"))
                    .as("PUBLIC must not EXECUTE %s", function).isFalse();
        }
        assertThat(hasFunction(connection, "catalog_order_owner",
                "catalog_order.reject_row_mutation()", "EXECUTE"))
                .as("catalog_order_owner EXECUTE reject_row_mutation").isTrue();
        assertThat(hasFunction(connection, "catalog_order_role",
                "catalog_order.reject_row_mutation()", "EXECUTE"))
                .as("runtime must not EXECUTE reject_row_mutation").isFalse();
        for (String role : functionMatrixRoles) {
            if (role.equals("catalog_order_owner")) {
                continue;
            }
            assertThat(hasFunction(connection, role,
                    "catalog_order.reject_row_mutation()", "EXECUTE"))
                    .as("%s must not EXECUTE reject_row_mutation", role).isFalse();
        }
        assertThat(hasFunction(connection, "public",
                "catalog_order.reject_row_mutation()", "EXECUTE"))
                .as("PUBLIC must not EXECUTE reject_row_mutation").isFalse();

        // four S1 functions granted to the four S2 runtime roles
        for (String role : List.of("catalog_order_role", "ivr_role", "entitlement_metering_role",
                "activation_consent_role")) {
            for (String function : PLATFORM_FUNCTIONS) {
                assertThat(hasFunction(connection, role, function, "EXECUTE"))
                        .as("%s EXECUTE %s", role, function).isTrue();
            }
            assertThat(hasFunction(connection, role, AUDIT_FUNCTION, "EXECUTE"))
                    .as("%s EXECUTE audit.append_fact", role).isTrue();
        }
        // F-PA4-AGG-01: the four S2 owner roles hold EXECUTE=false on all four S1
        // functions (accepted plan 6.6.4 "no owner receives a runtime EXECUTE grant",
        // 6.6.5 and 8.3). Signatures reuse the existing PLATFORM_FUNCTIONS and
        // AUDIT_FUNCTION constants whose positive runtime grants are asserted above;
        // no live over-grant exists in V004, so this is fail-closed evidence only.
        for (String role : List.of("catalog_order_owner", "ivr_owner",
                "entitlement_metering_owner", "activation_consent_owner")) {
            for (String function : PLATFORM_FUNCTIONS) {
                assertThat(hasFunction(connection, role, function, "EXECUTE"))
                        .as("%s must not EXECUTE %s", role, function).isFalse();
            }
            assertThat(hasFunction(connection, role, AUDIT_FUNCTION, "EXECUTE"))
                    .as("%s must not EXECUTE audit.append_fact", role).isFalse();
        }
        // entitlement_metering_owner has no platform capability
        assertThat(hasSchema(connection, "entitlement_metering_owner", "platform", "USAGE")).isFalse();
        assertThat(hasFunction(connection, "entitlement_metering_owner",
                "platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)", "EXECUTE"))
                .isFalse();
        assertThat(queryLong(
                connection, "SELECT count(*) FROM pg_proc WHERE proname = 'issue_first_trial'"))
                .as("no issue_first_trial at V004").isZero();
    }

    private static void assertV003SchemaUsageExactAndNoCreate(Connection connection) throws SQLException {
        assertThat(hasSchema(connection, "subject_number_owner", "subject_number", "USAGE")).isTrue();
        assertThat(hasSchema(connection, "subject_number_owner", "subject_number", "CREATE")).isTrue();
        assertThat(hasSchema(connection, "subject_number_role", "subject_number", "USAGE")).isTrue();
        assertThat(hasSchema(connection, "subject_number_role", "subject_number", "CREATE")).isFalse();
        assertThat(hasSchema(connection, "subject_identity_verification_role", "subject_number", "USAGE"))
                .isTrue();
        assertThat(hasSchema(connection, "subject_identity_verification_role", "subject_number", "CREATE"))
                .isFalse();
        for (String role : List.of(
                "platform_reliability_owner", "platform_reliability_role", "platform_dispatcher_role",
                "audit_owner", "audit_reader_role", "customer_iam_owner", "customer_iam_role",
                "organization_access_owner", "organization_access_role", "operations_iam_owner",
                "operations_iam_role", "operations_service_role")) {
            assertThat(hasSchema(connection, role, "subject_number", "USAGE"))
                    .as("%s must not have subject_number USAGE", role).isFalse();
            assertThat(hasSchema(connection, role, "subject_number", "CREATE"))
                    .as("%s must not have subject_number CREATE", role).isFalse();
        }
        assertThat(hasSchema(connection, "public", "subject_number", "USAGE")).isFalse();
        assertThat(hasSchema(connection, "public", "subject_number", "CREATE")).isFalse();
    }

    private static void assertV003OwnedAndCrossModuleGrants(Connection connection) throws SQLException {
        assertTablePrivileges(connection, "subject_number_role",
                "subject_number.business_subject", true, false, false, false, false);
        assertTablePrivileges(connection, "subject_number_role",
                "subject_number.caller_number_binding", true, true, false, false, false);
        assertTablePrivileges(connection, "subject_number_role",
                "subject_number.subject_claim", true, true, false, false, false);
        assertTablePrivileges(connection, "subject_number_role",
                "subject_number.subject_risk_state", true, true, false, false, false);
        assertTablePrivileges(connection, "subject_identity_verification_role",
                "subject_number.business_subject", false, false, false, false, false);
        assertTablePrivileges(connection, "subject_identity_verification_role",
                "subject_number.caller_number_binding", false, false, false, false, false);
        assertTablePrivileges(connection, "subject_identity_verification_role",
                "subject_number.subject_claim", false, false, false, false, false);
        assertTablePrivileges(connection, "subject_identity_verification_role",
                "subject_number.subject_risk_state", false, false, false, false, false);

        Map<String, List<String>> allColumnsByTable = new java.util.LinkedHashMap<>();
        allColumnsByTable.put("subject_number.business_subject", List.of(
                "id", "owner_account_id", "owner_organization_id", "source", "real_name_status",
                "real_name_verified_at", "revision", "created_at", "updated_at"));
        allColumnsByTable.put("subject_number.caller_number_binding", List.of(
                "id", "subject_id", "number_type", "number_ciphertext", "number_hmac",
                "number_masked", "status", "verified_at", "revision"));
        allColumnsByTable.put("subject_number.subject_claim", List.of(
                "id", "subject_id", "account_id", "organization_id", "verification_method",
                "verification_ref_ciphertext", "request_id", "status", "risk_checked_at", "verified_at"));
        allColumnsByTable.put("subject_number.subject_risk_state", List.of(
                "id", "caller_number_binding_id", "risk_type", "state", "source", "observed_at",
                "cleared_at", "revision"));

        Map<String, List<String>> runtimeInsertColumns = new java.util.LinkedHashMap<>();
        runtimeInsertColumns.put("subject_number.business_subject",
                List.of("id", "source", "revision", "created_at", "updated_at"));
        runtimeInsertColumns.put("subject_number.caller_number_binding",
                allColumnsByTable.get("subject_number.caller_number_binding"));
        runtimeInsertColumns.put("subject_number.subject_claim",
                allColumnsByTable.get("subject_number.subject_claim"));
        runtimeInsertColumns.put("subject_number.subject_risk_state",
                allColumnsByTable.get("subject_number.subject_risk_state"));

        Map<String, List<String>> runtimeUpdateColumns = new java.util.LinkedHashMap<>();
        runtimeUpdateColumns.put("subject_number.business_subject",
                List.of("owner_account_id", "revision", "updated_at"));
        runtimeUpdateColumns.put("subject_number.caller_number_binding",
                List.of("status", "verified_at", "revision"));
        runtimeUpdateColumns.put("subject_number.subject_claim", List.of());
        runtimeUpdateColumns.put("subject_number.subject_risk_state",
                List.of("state", "source", "observed_at", "cleared_at", "revision"));

        Map<String, List<String>> verificationSelectColumns = List.of(
                "id", "real_name_status", "real_name_verified_at", "revision", "updated_at")
                .stream().collect(java.util.stream.Collectors.toMap(
                        column -> "subject_number.business_subject|" + column,
                        column -> List.of(column)));
        // rebuild as map of table -> columns
        Map<String, List<String>> verificationSelectByTable = Map.of(
                "subject_number.business_subject",
                List.of("id", "real_name_status", "real_name_verified_at", "revision", "updated_at"));
        Map<String, List<String>> verificationUpdateByTable = Map.of(
                "subject_number.business_subject",
                List.of("real_name_status", "real_name_verified_at", "revision", "updated_at"));
        assertThat(verificationSelectColumns).isNotNull();

        for (Map.Entry<String, List<String>> entry : allColumnsByTable.entrySet()) {
            String table = entry.getKey();
            List<String> allColumns = entry.getValue();
            for (String column : runtimeInsertColumns.get(table)) {
                assertThat(hasColumn(connection, "subject_number_role", table, column, "INSERT"))
                        .as("%s INSERT %s", "subject_number_role", table + "." + column).isTrue();
            }
            for (String column : allColumns) {
                if (!runtimeInsertColumns.get(table).contains(column)) {
                    assertThat(hasColumn(connection, "subject_number_role", table, column, "INSERT"))
                            .as("subject_number_role must not INSERT %s.%s", table, column).isFalse();
                }
                if (runtimeUpdateColumns.get(table).contains(column)) {
                    assertThat(hasColumn(connection, "subject_number_role", table, column, "UPDATE"))
                            .as("subject_number_role UPDATE %s.%s", table, column).isTrue();
                } else {
                    assertThat(hasColumn(connection, "subject_number_role", table, column, "UPDATE"))
                            .as("subject_number_role must not UPDATE %s.%s", table, column).isFalse();
                }
                assertThat(hasColumn(connection, "subject_number_role", table, column, "SELECT"))
                        .as("subject_number_role SELECT %s.%s", table, column).isTrue();

                boolean verificationSelect = verificationSelectByTable
                        .getOrDefault(table, List.of()).contains(column);
                boolean verificationUpdate = verificationUpdateByTable
                        .getOrDefault(table, List.of()).contains(column);
                assertThat(hasColumn(connection, "subject_identity_verification_role",
                        table, column, "SELECT"))
                        .as("verification SELECT %s.%s", table, column)
                        .isEqualTo(verificationSelect);
                assertThat(hasColumn(connection, "subject_identity_verification_role",
                        table, column, "UPDATE"))
                        .as("verification UPDATE %s.%s", table, column)
                        .isEqualTo(verificationUpdate);
                assertThat(hasColumn(connection, "subject_identity_verification_role",
                        table, column, "INSERT"))
                        .as("verification must not INSERT %s.%s", table, column).isFalse();
            }
        }

        // Complete expected effective-privilege matrix for all 15 roles x 4 V003 tables.
        Map<String, String> owningOwnerByTable = Map.of(
                "subject_number.business_subject", "subject_number_owner",
                "subject_number.caller_number_binding", "subject_number_owner",
                "subject_number.subject_claim", "subject_number_owner",
                "subject_number.subject_risk_state", "subject_number_owner");
        for (String table : V003_TABLES) {
            String owner = owningOwnerByTable.get(table);
            boolean businessSubject = table.equals("subject_number.business_subject");
            for (String role : S1_ROLES) {
                boolean ownerRow = role.equals(owner);
                boolean runtimeRow = role.equals("subject_number_role");
                boolean verificationRow = role.equals("subject_identity_verification_role");
                boolean expectTableSelect = ownerRow || runtimeRow;
                boolean expectTableInsert = ownerRow || (runtimeRow && !businessSubject);
                boolean expectTableUpdate = ownerRow;
                assertThat(hasTable(connection, role, table, "SELECT"))
                        .as("matrix %s table SELECT %s", role, table)
                        .isEqualTo(expectTableSelect);
                assertThat(hasTable(connection, role, table, "INSERT"))
                        .as("matrix %s table INSERT %s", role, table)
                        .isEqualTo(expectTableInsert);
                assertThat(hasTable(connection, role, table, "UPDATE"))
                        .as("matrix %s table UPDATE %s", role, table)
                        .isEqualTo(expectTableUpdate);
                assertThat(hasTable(connection, role, table, "DELETE"))
                        .as("matrix %s table DELETE %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "TRUNCATE"))
                        .as("matrix %s table TRUNCATE %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "REFERENCES"))
                        .as("matrix %s table REFERENCES %s", role, table)
                        .isEqualTo(ownerRow);
                assertThat(hasTable(connection, role, table, "TRIGGER"))
                        .as("matrix %s table TRIGGER %s", role, table)
                        .isEqualTo(ownerRow);
                for (String column : allColumnsByTable.get(table)) {
                    boolean expectColumnSelect = expectTableSelect
                            || (verificationRow && verificationSelectByTable
                                    .getOrDefault(table, List.of()).contains(column));
                    boolean expectColumnInsert = ownerRow
                            || (runtimeRow && runtimeInsertColumns.get(table).contains(column));
                    boolean expectColumnUpdate = ownerRow
                            || (runtimeRow && runtimeUpdateColumns.get(table).contains(column))
                            || (verificationRow && verificationUpdateByTable
                                    .getOrDefault(table, List.of()).contains(column));
                    assertThat(hasColumn(connection, role, table, column, "SELECT"))
                            .as("matrix %s column SELECT %s.%s", role, table, column)
                            .isEqualTo(expectColumnSelect);
                    assertThat(hasColumn(connection, role, table, column, "INSERT"))
                            .as("matrix %s column INSERT %s.%s", role, table, column)
                            .isEqualTo(expectColumnInsert);
                    assertThat(hasColumn(connection, role, table, column, "UPDATE"))
                            .as("matrix %s column UPDATE %s.%s", role, table, column)
                            .isEqualTo(expectColumnUpdate);
                }
            }
        }

        // Verification role has no table-level SELECT/UPDATE/INSERT.
        assertThat(hasTable(connection, "subject_identity_verification_role",
                "subject_number.business_subject", "SELECT")).isFalse();
        assertThat(hasTable(connection, "subject_identity_verification_role",
                "subject_number.business_subject", "UPDATE")).isFalse();
        assertThat(hasTable(connection, "subject_identity_verification_role",
                "subject_number.business_subject", "INSERT")).isFalse();
    }

    private static void assertV003PublicAclIsZero(Connection connection) throws SQLException {
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_namespace n, "
                        + "aclexplode(coalesce(n.nspacl, acldefault('n', n.nspowner))) a "
                        + "WHERE n.nspname = 'subject_number' AND a.grantee = 0"))
                .as("PUBLIC has no subject_number schema privilege").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace, "
                        + "aclexplode(coalesce(c.relacl, acldefault('r', c.relowner))) a "
                        + "WHERE n.nspname = 'subject_number' AND c.relkind IN ('r','p') "
                        + "AND a.grantee = 0"))
                .as("PUBLIC has no subject_number table privilege").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace, "
                        + "aclexplode(coalesce(p.proacl, acldefault('f', p.proowner))) a "
                        + "WHERE n.nspname = 'subject_number' AND a.grantee = 0"))
                .as("PUBLIC has no subject_number function EXECUTE").isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "JOIN pg_attribute a ON a.attrelid = c.oid, "
                        + "aclexplode(coalesce(a.attacl, acldefault('c', c.relowner))) x "
                        + "WHERE n.nspname = 'subject_number' "
                        + "AND c.relkind IN ('r','p') "
                        + "AND c.relname IN ('business_subject','caller_number_binding',"
                        + "'subject_claim','subject_risk_state') "
                        + "AND a.attnum > 0 AND NOT a.attisdropped "
                        + "AND x.grantee = 0"))
                .as("PUBLIC has no subject_number column privilege").isZero();
    }

    private static void assertV003GuardFunctionExecuteBoundaries(Connection connection)
            throws SQLException {
        Map<String, List<String>> guardFunctionGrantRoles = Map.of(
                "subject_number.guard_subject_owner()",
                List.of("subject_number_role", "subject_identity_verification_role"),
                "subject_number.guard_caller_number_binding()",
                List.of("subject_number_role"),
                "subject_number.guard_subject_risk_state()",
                List.of("subject_number_role"));
        for (Map.Entry<String, List<String>> entry : guardFunctionGrantRoles.entrySet()) {
            String function = entry.getKey();
            for (String grantedRole : entry.getValue()) {
                assertThat(hasFunction(connection, grantedRole, function, "EXECUTE"))
                        .as("%s EXECUTE %s", grantedRole, function).isTrue();
            }
            assertThat(hasFunction(connection, "public", function, "EXECUTE"))
                    .as("PUBLIC must not EXECUTE %s", function).isFalse();
            for (String role : S1_ROLES) {
                if (entry.getValue().contains(role) || role.equals("subject_number_owner")) {
                    continue;
                }
                assertThat(hasFunction(connection, role, function, "EXECUTE"))
                        .as("%s must not EXECUTE %s", role, function).isFalse();
            }
            // verification role only on guard_subject_owner
            if (!function.equals("subject_number.guard_subject_owner()")) {
                assertThat(hasFunction(connection, "subject_identity_verification_role", function, "EXECUTE"))
                        .as("verification must not EXECUTE %s", function).isFalse();
            }
        }

        assertThat(hasFunction(connection, "subject_number_role",
                "subject_number.reject_row_mutation()", "EXECUTE"))
                .as("runtime must not EXECUTE reject_row_mutation").isFalse();
        assertThat(hasFunction(connection, "subject_identity_verification_role",
                "subject_number.reject_row_mutation()", "EXECUTE"))
                .as("verification must not EXECUTE reject_row_mutation").isFalse();
        assertThat(hasFunction(connection, "public",
                "subject_number.reject_row_mutation()", "EXECUTE"))
                .as("PUBLIC must not EXECUTE reject_row_mutation").isFalse();
        for (String role : S1_ROLES) {
            if (role.equals("subject_number_owner")) {
                continue;
            }
            assertThat(hasFunction(connection, role,
                    "subject_number.reject_row_mutation()", "EXECUTE"))
                    .as("%s must not EXECUTE reject_row_mutation", role).isFalse();
        }

        assertThat(hasFunction(connection, "subject_identity_verification_role",
                "subject_number.guard_caller_number_binding()", "EXECUTE")).isFalse();
        assertThat(hasFunction(connection, "subject_identity_verification_role",
                "subject_number.guard_subject_risk_state()", "EXECUTE")).isFalse();
    }

    private static void exerciseV003InsertsAsRuntimeRoleAndRollBack() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID claimId = UUID.randomUUID();
        UUID riskId = UUID.randomUUID();
        String hmac = "9".repeat(64);
        String ciphertext = "v1.v003-smoke.ciphertext-payload";
        String masked = "138****0000";
        String verificationRef = "v1.v003-smoke.ref-payload";

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SET LOCAL ROLE customer_iam_role");
                }
                assertThat(insertCustomerAccount(connection, accountId, "V003 Smoke Customer"))
                        .isEqualTo(1);

                try (Statement statement = connection.createStatement()) {
                    statement.execute("RESET ROLE");
                    statement.execute("SET LOCAL ROLE organization_access_role");
                }
                assertThat(insertOrganization(connection, organizationId, "V003 Smoke Org"))
                        .isEqualTo(1);

                try (Statement statement = connection.createStatement()) {
                    statement.execute("RESET ROLE");
                    statement.execute("SET LOCAL ROLE subject_number_role");
                }
                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO subject_number.business_subject "
                                + "(id, source, revision, created_at, updated_at) "
                                + "VALUES (?, 'IVR', 1, now(), now())")) {
                    prepared.setObject(1, subjectId);
                    assertThat(prepared.executeUpdate()).isEqualTo(1);
                }
                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO subject_number.caller_number_binding "
                                + "(id, subject_id, number_type, number_ciphertext, number_hmac, "
                                + "number_masked, status, verified_at, revision) "
                                + "VALUES (?, ?, 'MOBILE', ?, ?, ?, 'ACTIVE', now(), 1)")) {
                    prepared.setObject(1, bindingId);
                    prepared.setObject(2, subjectId);
                    prepared.setString(3, ciphertext);
                    prepared.setString(4, hmac);
                    prepared.setString(5, masked);
                    assertThat(prepared.executeUpdate()).isEqualTo(1);
                }
                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO subject_number.subject_claim "
                                + "(id, subject_id, account_id, verification_method, "
                                + "verification_ref_ciphertext, request_id, status, risk_checked_at, "
                                + "verified_at) "
                                + "VALUES (?, ?, ?, 'SMS', ?, ?, 'ACCEPTED', now(), now())")) {
                    prepared.setObject(1, claimId);
                    prepared.setObject(2, subjectId);
                    prepared.setObject(3, accountId);
                    prepared.setString(4, verificationRef);
                    prepared.setString(5, "v003-smoke-" + UUID.randomUUID());
                    assertThat(prepared.executeUpdate()).isEqualTo(1);
                }
                try (PreparedStatement prepared = connection.prepareStatement(
                        "INSERT INTO subject_number.subject_risk_state "
                                + "(id, caller_number_binding_id, risk_type, state, source, "
                                + "observed_at, revision) "
                                + "VALUES (?, ?, 'CONFLICT', 'OPEN', 'CLAIM_CHECK', now(), 1)")) {
                    prepared.setObject(1, riskId);
                    prepared.setObject(2, bindingId);
                    assertThat(prepared.executeUpdate()).isEqualTo(1);
                }
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM subject_number.business_subject WHERE id = ?",
                        subjectId)).isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM subject_number.caller_number_binding WHERE id = ?",
                        bindingId)).isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM subject_number.subject_claim WHERE id = ?",
                        claimId)).isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM subject_number.subject_risk_state WHERE id = ?",
                        riskId)).isEqualTo(1);
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.business_subject WHERE id = ?", subjectId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.caller_number_binding WHERE id = ?",
                    bindingId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.subject_claim WHERE id = ?", claimId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.subject_risk_state WHERE id = ?", riskId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", accountId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?",
                    organizationId)).isZero();
        }
    }

    private static void resetToMigrationIdentity(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("RESET ROLE");
        }
        assertThat(queryString(connection, "SELECT current_user")).isEqualTo(DB_USER);
    }

    private static int insertSubject(Connection connection, UUID id, String source)
            throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO subject_number.business_subject (id, source) VALUES (?, ?)",
                id, source);
    }

    private static int insertBinding(
            Connection connection,
            UUID id,
            UUID subjectId,
            String numberType,
            String ciphertext,
            String hmac,
            String masked,
            String status,
            OffsetDateTime verifiedAt,
            int revision) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO subject_number.caller_number_binding (id, subject_id, number_type, "
                        + "number_ciphertext, number_hmac, number_masked, status, verified_at, revision) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, subjectId, numberType, ciphertext, hmac, masked, status, verifiedAt, revision);
    }

    private static int insertClaim(
            Connection connection,
            UUID id,
            UUID subjectId,
            UUID accountId,
            UUID organizationId,
            String verificationMethod,
            String verificationRef,
            String requestId,
            String status,
            OffsetDateTime riskCheckedAt,
            OffsetDateTime verifiedAt) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO subject_number.subject_claim (id, subject_id, account_id, "
                        + "organization_id, verification_method, verification_ref_ciphertext, "
                        + "request_id, status, risk_checked_at, verified_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, subjectId, accountId, organizationId, verificationMethod, verificationRef,
                requestId, status, riskCheckedAt, verifiedAt);
    }

    private static int insertRisk(
            Connection connection,
            UUID id,
            UUID bindingId,
            String riskType,
            String state,
            String source,
            OffsetDateTime observedAt,
            OffsetDateTime clearedAt,
            int revision) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO subject_number.subject_risk_state (id, caller_number_binding_id, "
                        + "risk_type, state, source, observed_at, cleared_at, revision) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, bindingId, riskType, state, source, observedAt, clearedAt, revision);
    }

    private static void assertSubjectOwnerSetOnceAndRealNameSeparation() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        String hmac = randomSha256();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                switchLocalRole(connection, "customer_iam_role");
                assertThat(insertCustomerAccount(connection, accountId, "S2 Subject Customer"))
                        .isEqualTo(1);
                switchLocalRole(connection, "organization_access_role");
                assertThat(insertOrganization(connection, organizationId, "S2 Subject Organization"))
                        .isEqualTo(1);

                switchLocalRole(connection, "subject_number_role");
                assertThat(insertSubject(connection, subjectId, "IVR")).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT real_name_status FROM subject_number.business_subject WHERE id = ?",
                        subjectId)).isEqualTo("UNVERIFIED");
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM subject_number.business_subject WHERE id = ? "
                                + "AND owner_account_id IS NULL AND owner_organization_id IS NULL "
                                + "AND revision = 1", subjectId)).isEqualTo(1);

                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.business_subject SET owner_account_id = ?, "
                                + "revision = revision + 1, updated_at = now() "
                                + "WHERE id = ? AND revision = 1", accountId, subjectId)).isEqualTo(1);
                assertThat(queryUuid(connection,
                        "SELECT owner_account_id::text FROM subject_number.business_subject "
                                + "WHERE id = ?", subjectId)).isEqualTo(accountId);
                assertThat(queryLong(connection,
                        "SELECT revision FROM subject_number.business_subject WHERE id = ?", subjectId))
                        .isEqualTo(2);

                expectSqlFailure(connection, "P0001", "SUBJECT_OWNER_IMMUTABLE",
                        "UPDATE subject_number.business_subject SET owner_account_id = ?, "
                                + "revision = revision + 1, updated_at = now() WHERE id = ?",
                        UUID.randomUUID(), subjectId);
                expectSqlFailure(connection, "P0001", "SUBJECT_OWNER_IMMUTABLE",
                        "UPDATE subject_number.business_subject SET owner_account_id = NULL, "
                                + "revision = revision + 1, updated_at = now() WHERE id = ?",
                        subjectId);
                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE subject_number.business_subject SET owner_organization_id = ? "
                                + "WHERE id = ?", organizationId, subjectId);
                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE subject_number.business_subject SET real_name_status = 'VERIFIED', "
                                + "real_name_verified_at = now() WHERE id = ?", subjectId);
                expectSqlFailure(connection, "42501", "permission denied",
                        "INSERT INTO subject_number.business_subject (id, owner_account_id, source) "
                                + "VALUES (?, ?, 'IVR')", UUID.randomUUID(), accountId);

                assertThat(insertBinding(connection, bindingId, subjectId, "MOBILE",
                        "v1.s2.subject.ciphertext", hmac, "138****0001", "ACTIVE",
                        OffsetDateTime.now(ZoneOffset.UTC), 1)).isEqualTo(1);
                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE subject_number.caller_number_binding SET number_hmac = ? WHERE id = ?",
                        randomSha256(), bindingId);

                switchLocalRole(connection, "subject_identity_verification_role");
                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.business_subject SET real_name_status = 'VERIFIED', "
                                + "real_name_verified_at = now(), revision = revision + 1, "
                                + "updated_at = now() WHERE id = ? AND revision = 2", subjectId))
                        .isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT real_name_status FROM subject_number.business_subject WHERE id = ?",
                        subjectId)).isEqualTo("VERIFIED");
                assertThat(queryLong(connection,
                        "SELECT revision FROM subject_number.business_subject WHERE id = ?", subjectId))
                        .isEqualTo(3);
                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE subject_number.business_subject SET owner_account_id = ? WHERE id = ?",
                        UUID.randomUUID(), subjectId);
                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE subject_number.business_subject SET source = 'WEB' WHERE id = ?",
                        subjectId);
                expectSqlFailure(connection, "42501", "permission denied",
                        "INSERT INTO subject_number.business_subject (id, source) VALUES (?, 'IVR')",
                        UUID.randomUUID());

                switchLocalRole(connection, "subject_number_owner");
                expectSqlFailure(connection, "P0001", "SUBJECT_OWNER_IMMUTABLE",
                        "UPDATE subject_number.business_subject SET real_name_status = 'UNVERIFIED', "
                                + "real_name_verified_at = NULL, revision = revision + 1, "
                                + "updated_at = now() WHERE id = ?", subjectId);
                expectSqlFailure(connection, "P0001", "SUBJECT_OWNER_IMMUTABLE",
                        "UPDATE subject_number.business_subject SET source = 'WEB' WHERE id = ?",
                        subjectId);

                List<CheckProbe> probes = List.of(
                        checkProbe("ck_subject_single_owner",
                                "INSERT INTO subject_number.business_subject "
                                        + "(id, owner_account_id, owner_organization_id, source) "
                                        + "VALUES (?, ?, ?, 'IVR')",
                                UUID.randomUUID(), accountId, organizationId),
                        checkProbe("ck_subject_source",
                                "INSERT INTO subject_number.business_subject (id, source) "
                                        + "VALUES (?, 'BAD')", UUID.randomUUID()),
                        checkProbe("ck_subject_real_name_status",
                                "INSERT INTO subject_number.business_subject "
                                        + "(id, source, real_name_status) VALUES (?, 'IVR', 'BAD')",
                                UUID.randomUUID()),
                        checkProbe("ck_subject_real_name_shape",
                                "INSERT INTO subject_number.business_subject "
                                        + "(id, source, real_name_status) VALUES (?, 'IVR', 'VERIFIED')",
                                UUID.randomUUID()),
                        checkProbe("ck_subject_revision",
                                "INSERT INTO subject_number.business_subject "
                                        + "(id, source, revision) VALUES (?, 'IVR', 0)",
                                UUID.randomUUID()),
                        checkProbe("ck_subject_time_order",
                                "INSERT INTO subject_number.business_subject "
                                        + "(id, source, created_at, updated_at) "
                                        + "VALUES (?, 'IVR', now(), now() - interval '1 second')",
                                UUID.randomUUID()));
                List<String> probed = new ArrayList<>();
                for (CheckProbe probe : probes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                            probe.parameters());
                    probed.add(probe.constraint());
                }
                assertThat(probed)
                        .as("all six business_subject named checks have a violating probe")
                        .containsExactlyInAnyOrderElementsOf(
                                V003_CHECK_DEF_MAP.get("business_subject").keySet());
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.business_subject WHERE id = ?", subjectId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.caller_number_binding WHERE id = ?", bindingId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", accountId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?",
                    organizationId)).isZero();
        }
    }

    private static void assertBindingLifecycleTerminalityAndActiveNumberUniqueness() throws Exception {
        UUID subjectA = UUID.randomUUID();
        UUID subjectB = UUID.randomUUID();
        UUID activeBinding = UUID.randomUUID();
        String activeHmac = randomSha256();
        String ciphertext = "v1.s2.binding.ciphertext";
        String masked = "139****0002";

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "subject_number_role");
                assertThat(insertSubject(connection, subjectA, "MINIAPP")).isEqualTo(1);
                assertThat(insertSubject(connection, subjectB, "WEB")).isEqualTo(1);
                assertThat(insertBinding(connection, activeBinding, subjectA, "MOBILE", ciphertext,
                        activeHmac, masked, "ACTIVE", OffsetDateTime.now(ZoneOffset.UTC), 1))
                        .isEqualTo(1);
                assertThat(insertBinding(connection, UUID.randomUUID(), subjectB, "MOBILE", ciphertext,
                        randomSha256(), masked, "DISABLED", null, 1)).isEqualTo(1);

                String probeSql = "INSERT INTO subject_number.caller_number_binding "
                        + "(id, subject_id, number_type, number_ciphertext, number_hmac, "
                        + "number_masked, status, revision) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                List<CheckProbe> probes = List.of(
                        checkProbe("ck_binding_number_type", probeSql,
                                UUID.randomUUID(), subjectA, "BAD", ciphertext, randomSha256(), masked,
                                "DISABLED", 1),
                        checkProbe("ck_binding_number_ciphertext", probeSql,
                                UUID.randomUUID(), subjectA, "MOBILE", "plain", randomSha256(), masked,
                                "DISABLED", 1),
                        checkProbe("ck_binding_number_hmac", probeSql,
                                UUID.randomUUID(), subjectA, "MOBILE", ciphertext, "A".repeat(64),
                                masked, "DISABLED", 1),
                        checkProbe("ck_binding_number_masked", probeSql,
                                UUID.randomUUID(), subjectA, "MOBILE", ciphertext, randomSha256(), "",
                                "DISABLED", 1),
                        checkProbe("ck_binding_status", probeSql,
                                UUID.randomUUID(), subjectA, "MOBILE", ciphertext, randomSha256(), masked,
                                "BAD", 1),
                        checkProbe("ck_binding_verified_shape", probeSql,
                                UUID.randomUUID(), subjectA, "MOBILE", ciphertext, randomSha256(), masked,
                                "ACTIVE", 1),
                        checkProbe("ck_binding_revision", probeSql,
                                UUID.randomUUID(), subjectA, "MOBILE", ciphertext, randomSha256(), masked,
                                "DISABLED", 0));
                List<String> probed = new ArrayList<>();
                for (CheckProbe probe : probes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                            probe.parameters());
                    probed.add(probe.constraint());
                }
                assertThat(probed)
                        .as("all seven caller_number_binding named checks have a violating probe")
                        .containsExactlyInAnyOrderElementsOf(
                                V003_CHECK_DEF_MAP.get("caller_number_binding").keySet());

                expectSqlFailure(connection, "23505", "uq_binding_active_number",
                        "INSERT INTO subject_number.caller_number_binding (id, subject_id, "
                                + "number_type, number_ciphertext, number_hmac, number_masked, status, "
                                + "verified_at) VALUES (?, ?, 'MOBILE', ?, ?, ?, 'ACTIVE', now())",
                        UUID.randomUUID(), subjectB, ciphertext, activeHmac, masked);

                String subjectNumberHmac = randomSha256();
                assertThat(insertBinding(connection, UUID.randomUUID(), subjectA, "MOBILE", ciphertext,
                        subjectNumberHmac, masked, "DISABLED", null, 1)).isEqualTo(1);
                expectSqlFailure(connection, "23505", "uq_binding_subject_number",
                        "INSERT INTO subject_number.caller_number_binding (id, subject_id, "
                                + "number_type, number_ciphertext, number_hmac, number_masked, status) "
                                + "VALUES (?, ?, 'MOBILE', ?, ?, ?, 'DISABLED')",
                        UUID.randomUUID(), subjectA, ciphertext, subjectNumberHmac, masked);

                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.caller_number_binding SET status = 'DISABLED', "
                                + "revision = revision + 1 WHERE id = ? AND status = 'ACTIVE' "
                                + "AND revision = 1", activeBinding)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT status FROM subject_number.caller_number_binding WHERE id = ?",
                        activeBinding)).isEqualTo("DISABLED");
                assertThat(queryLong(connection,
                        "SELECT revision FROM subject_number.caller_number_binding WHERE id = ?",
                        activeBinding)).isEqualTo(2);

                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.caller_number_binding SET status = 'ACTIVE', "
                                + "verified_at = now(), revision = revision + 1 WHERE id = ? "
                                + "AND status = 'DISABLED' AND revision = 2", activeBinding))
                        .isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT status FROM subject_number.caller_number_binding WHERE id = ?",
                        activeBinding)).isEqualTo("ACTIVE");
                assertThat(queryLong(connection,
                        "SELECT revision FROM subject_number.caller_number_binding WHERE id = ?",
                        activeBinding)).isEqualTo(3);

                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.caller_number_binding SET status = 'RETIRED', "
                                + "revision = revision + 1 WHERE id = ? AND status = 'ACTIVE' "
                                + "AND revision = 3", activeBinding)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT status FROM subject_number.caller_number_binding WHERE id = ?",
                        activeBinding)).isEqualTo("RETIRED");
                assertThat(queryLong(connection,
                        "SELECT revision FROM subject_number.caller_number_binding WHERE id = ?",
                        activeBinding)).isEqualTo(4);

                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.caller_number_binding SET status = 'DISABLED', "
                                + "revision = revision + 1 WHERE id = ? AND revision = 1",
                        activeBinding)).isZero();
                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.caller_number_binding SET status = 'RETIRED', "
                                + "revision = revision + 1 WHERE id = ? AND status = 'ACTIVE' "
                                + "AND revision = 4", activeBinding)).isZero();

                switchLocalRole(connection, "subject_number_owner");
                expectSqlFailure(connection, "P0001", "CALLER_NUMBER_BINDING_IMMUTABLE",
                        "UPDATE subject_number.caller_number_binding SET number_hmac = ? WHERE id = ?",
                        randomSha256(), activeBinding);
                switchLocalRole(connection, "subject_number_role");
                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE subject_number.caller_number_binding SET number_hmac = ? WHERE id = ?",
                        randomSha256(), activeBinding);

                expectSqlFailure(connection, "23503", "fk_binding_subject",
                        "INSERT INTO subject_number.caller_number_binding (id, subject_id, "
                                + "number_type, number_ciphertext, number_hmac, number_masked, status) "
                                + "VALUES (?, ?, 'MOBILE', ?, ?, ?, 'DISABLED')",
                        UUID.randomUUID(), UUID.randomUUID(), ciphertext, randomSha256(), masked);

                resetToMigrationIdentity(connection);
                expectSqlFailure(connection, "23503", "fk_binding_subject",
                        "DELETE FROM subject_number.business_subject WHERE id = ?", subjectA);
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.caller_number_binding "
                            + "WHERE subject_id IN (?, ?)", subjectA, subjectB)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.business_subject WHERE id IN (?, ?)",
                    subjectA, subjectB)).isZero();
        }
    }

    private static void assertAppendOnlyClaimAndForeignKeyBackstops() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID acceptedClaim = UUID.randomUUID();
        String acceptedRequest = "s2-claim-" + UUID.randomUUID();
        OffsetDateTime riskCheckedAt = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime verifiedAt = riskCheckedAt.plusSeconds(1);

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                switchLocalRole(connection, "customer_iam_role");
                assertThat(insertCustomerAccount(connection, accountId, "S2 Claim Customer"))
                        .isEqualTo(1);
                switchLocalRole(connection, "organization_access_role");
                assertThat(insertOrganization(connection, organizationId, "S2 Claim Organization"))
                        .isEqualTo(1);

                switchLocalRole(connection, "subject_number_role");
                assertThat(insertSubject(connection, subjectId, "IVR")).isEqualTo(1);
                UUID probeSubject = UUID.randomUUID();
                assertThat(insertSubject(connection, probeSubject, "WEB")).isEqualTo(1);
                assertThat(insertClaim(connection, acceptedClaim, subjectId, accountId, null,
                        "SMS", "v1.s2.claim.ref", acceptedRequest, "ACCEPTED",
                        riskCheckedAt, verifiedAt)).isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM subject_number.subject_claim WHERE id = ? "
                                + "AND status = 'ACCEPTED' AND verified_at IS NOT NULL", acceptedClaim))
                        .isEqualTo(1);

                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE subject_number.subject_claim SET status = 'REJECTED' WHERE id = ?",
                        acceptedClaim);
                expectSqlFailure(connection, "42501", "permission denied",
                        "DELETE FROM subject_number.subject_claim WHERE id = ?", acceptedClaim);

                switchLocalRole(connection, "subject_number_owner");
                expectSqlFailure(connection, "P0001", "SUBJECT_CLAIM_APPEND_ONLY",
                        "UPDATE subject_number.subject_claim SET status = 'REJECTED' WHERE id = ?",
                        acceptedClaim);
                expectSqlFailure(connection, "P0001", "SUBJECT_CLAIM_APPEND_ONLY",
                        "DELETE FROM subject_number.subject_claim WHERE id = ?", acceptedClaim);
                switchLocalRole(connection, "subject_number_role");

                String probeSql = "INSERT INTO subject_number.subject_claim "
                        + "(id, subject_id, account_id, verification_method, "
                        + "verification_ref_ciphertext, request_id, status, risk_checked_at, "
                        + "verified_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                OffsetDateTime earlyVerifiedAt = riskCheckedAt.minusSeconds(3600);
                List<CheckProbe> probes = List.of(
                        checkProbe("ck_claim_verification_method", probeSql,
                                UUID.randomUUID(), probeSubject, accountId, "BAD", "v1.s2.claim.ref",
                                "s2-claim-method-" + UUID.randomUUID(), "REJECTED", riskCheckedAt,
                                null),
                        checkProbe("ck_claim_verification_ref", probeSql,
                                UUID.randomUUID(), probeSubject, accountId, "SMS", "plain",
                                "s2-claim-ref-" + UUID.randomUUID(), "REJECTED", riskCheckedAt, null),
                        checkProbe("ck_claim_request_id", probeSql,
                                UUID.randomUUID(), probeSubject, accountId, "SMS", "v1.s2.claim.ref",
                                "", "REJECTED", riskCheckedAt, null),
                        checkProbe("ck_claim_status", probeSql,
                                UUID.randomUUID(), probeSubject, accountId, "SMS", "v1.s2.claim.ref",
                                "s2-claim-status-" + UUID.randomUUID(), "BAD", riskCheckedAt, null),
                        checkProbe("ck_claim_verified_shape", probeSql,
                                UUID.randomUUID(), probeSubject, accountId, "SMS", "v1.s2.claim.ref",
                                "s2-claim-shape-" + UUID.randomUUID(), "ACCEPTED", riskCheckedAt,
                                null),
                        checkProbe("ck_claim_verified_after_risk", probeSql,
                                UUID.randomUUID(), probeSubject, accountId, "SMS", "v1.s2.claim.ref",
                                "s2-claim-order-" + UUID.randomUUID(), "ACCEPTED", riskCheckedAt,
                                earlyVerifiedAt));
                List<String> probed = new ArrayList<>();
                for (CheckProbe probe : probes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                            probe.parameters());
                    probed.add(probe.constraint());
                }
                assertThat(probed)
                        .as("all six subject_claim named checks have a violating probe")
                        .containsExactlyInAnyOrderElementsOf(
                                V003_CHECK_DEF_MAP.get("subject_claim").keySet());

                expectSqlFailure(connection, "23505", "uq_claim_accepted_subject_account",
                        "INSERT INTO subject_number.subject_claim (id, subject_id, account_id, "
                                + "verification_method, verification_ref_ciphertext, request_id, "
                                + "status, risk_checked_at, verified_at) "
                                + "VALUES (?, ?, ?, 'SMS', 'v1.s2.claim.ref', ?, 'ACCEPTED', ?, ?)",
                        UUID.randomUUID(), subjectId, accountId,
                        "s2-claim-dup-" + UUID.randomUUID(), riskCheckedAt, verifiedAt);

                expectSqlFailure(connection, "23505", "uq_claim_request",
                        "INSERT INTO subject_number.subject_claim (id, subject_id, account_id, "
                                + "verification_method, verification_ref_ciphertext, request_id, "
                                + "status, risk_checked_at) "
                                + "VALUES (?, ?, ?, 'SMS', 'v1.s2.claim.ref', ?, 'REJECTED', ?)",
                        UUID.randomUUID(), subjectId, accountId, acceptedRequest, riskCheckedAt);

                expectSqlFailure(connection, "23503", "fk_claim_subject",
                        "INSERT INTO subject_number.subject_claim (id, subject_id, account_id, "
                                + "verification_method, verification_ref_ciphertext, request_id, "
                                + "status, risk_checked_at) "
                                + "VALUES (?, ?, ?, 'SMS', 'v1.s2.claim.ref', ?, 'REJECTED', ?)",
                        UUID.randomUUID(), UUID.randomUUID(), accountId,
                        "s2-claim-subject-parent-" + UUID.randomUUID(), riskCheckedAt);
                expectSqlFailure(connection, "23503", "fk_claim_account",
                        "INSERT INTO subject_number.subject_claim (id, subject_id, account_id, "
                                + "verification_method, verification_ref_ciphertext, request_id, "
                                + "status, risk_checked_at) "
                                + "VALUES (?, ?, ?, 'SMS', 'v1.s2.claim.ref', ?, 'REJECTED', ?)",
                        UUID.randomUUID(), subjectId, UUID.randomUUID(),
                        "s2-claim-account-parent-" + UUID.randomUUID(), riskCheckedAt);
                expectSqlFailure(connection, "23503", "fk_claim_organization",
                        "INSERT INTO subject_number.subject_claim (id, subject_id, account_id, "
                                + "organization_id, verification_method, verification_ref_ciphertext, "
                                + "request_id, status, risk_checked_at) "
                                + "VALUES (?, ?, ?, ?, 'SMS', 'v1.s2.claim.ref', ?, 'REJECTED', ?)",
                        UUID.randomUUID(), subjectId, accountId, UUID.randomUUID(),
                        "s2-claim-org-parent-" + UUID.randomUUID(), riskCheckedAt);

                assertThat(insertClaim(connection, UUID.randomUUID(), subjectId, accountId,
                        organizationId, "MANUAL_REVIEW", "v1.s2.claim.org-ref",
                        "s2-claim-org-" + UUID.randomUUID(), "REJECTED", riskCheckedAt, null))
                        .isEqualTo(1);

                resetToMigrationIdentity(connection);
                expectSqlFailure(connection, "23503", "fk_claim_subject",
                        "DELETE FROM subject_number.business_subject WHERE id = ?", subjectId);
                expectSqlFailure(connection, "23503", "fk_claim_account",
                        "DELETE FROM customer_iam.customer_account WHERE id = ?", accountId);
                expectSqlFailure(connection, "23503", "fk_claim_organization",
                        "DELETE FROM organization_access.organization WHERE id = ?", organizationId);
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.subject_claim "
                            + "WHERE subject_id = ? OR account_id = ?", subjectId, accountId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.business_subject WHERE id = ?", subjectId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", accountId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?",
                    organizationId)).isZero();
        }
    }

    private static void assertRiskOpenClearReopenRevisionProtocol() throws Exception {
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID riskId = UUID.randomUUID();
        String hmac = randomSha256();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "subject_number_role");
                assertThat(insertSubject(connection, subjectId, "IVR")).isEqualTo(1);
                assertThat(insertBinding(connection, bindingId, subjectId, "MOBILE",
                        "v1.s2.risk.ciphertext", hmac, "138****0024", "ACTIVE",
                        OffsetDateTime.now(ZoneOffset.UTC), 1)).isEqualTo(1);
                assertThat(insertRisk(connection, riskId, bindingId, "CONFLICT", "OPEN",
                        "CLAIM_CHECK", OffsetDateTime.now(ZoneOffset.UTC), null, 1)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT state FROM subject_number.subject_risk_state WHERE id = ?", riskId))
                        .isEqualTo("OPEN");
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM subject_number.subject_risk_state WHERE id = ? "
                                + "AND cleared_at IS NULL AND revision = 1", riskId)).isEqualTo(1);

                String probeSql = "INSERT INTO subject_number.subject_risk_state "
                        + "(id, caller_number_binding_id, risk_type, state, source, observed_at, "
                        + "cleared_at, revision) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                OffsetDateTime observedAt = OffsetDateTime.now(ZoneOffset.UTC);
                List<CheckProbe> probes = List.of(
                        checkProbe("ck_risk_type", probeSql,
                                UUID.randomUUID(), bindingId, "BAD", "OPEN", "CLAIM_CHECK",
                                observedAt, null, 1),
                        checkProbe("ck_risk_state", probeSql,
                                UUID.randomUUID(), bindingId, "RECYCLED", "BAD", "CLAIM_CHECK",
                                observedAt, null, 1),
                        checkProbe("ck_risk_source", probeSql,
                                UUID.randomUUID(), bindingId, "RECYCLED", "OPEN", "BAD",
                                observedAt, null, 1),
                        checkProbe("ck_risk_cleared_shape", probeSql,
                                UUID.randomUUID(), bindingId, "RECYCLED", "CLEARED", "CLAIM_CHECK",
                                observedAt, null, 1),
                        checkProbe("ck_risk_cleared_after_observed", probeSql,
                                UUID.randomUUID(), bindingId, "RECYCLED", "CLEARED", "CLAIM_CHECK",
                                observedAt, observedAt.minusSeconds(60), 1),
                        checkProbe("ck_risk_revision", probeSql,
                                UUID.randomUUID(), bindingId, "RECYCLED", "OPEN", "CLAIM_CHECK",
                                observedAt, null, 0));
                List<String> probed = new ArrayList<>();
                for (CheckProbe probe : probes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                            probe.parameters());
                    probed.add(probe.constraint());
                }
                assertThat(probed)
                        .as("all six subject_risk_state named checks have a violating probe")
                        .containsExactlyInAnyOrderElementsOf(
                                V003_CHECK_DEF_MAP.get("subject_risk_state").keySet());

                expectSqlFailure(connection, "23505", "uq_risk_binding_type",
                        "INSERT INTO subject_number.subject_risk_state "
                                + "(id, caller_number_binding_id, risk_type, source) "
                                + "VALUES (?, ?, 'CONFLICT', 'OPERATIONS')",
                        UUID.randomUUID(), bindingId);

                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.subject_risk_state SET state = 'CLEARED', "
                                + "cleared_at = GREATEST(observed_at, clock_timestamp()), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND state = 'OPEN' AND revision = 1", riskId))
                        .isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT state FROM subject_number.subject_risk_state WHERE id = ?", riskId))
                        .isEqualTo("CLEARED");
                assertThat(queryLong(connection,
                        "SELECT revision FROM subject_number.subject_risk_state WHERE id = ?", riskId))
                        .isEqualTo(2);

                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.subject_risk_state SET state = 'OPEN', "
                                + "cleared_at = NULL, revision = revision + 1 "
                                + "WHERE id = ? AND state = 'CLEARED' AND revision = 2", riskId))
                        .isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT state FROM subject_number.subject_risk_state WHERE id = ?", riskId))
                        .isEqualTo("OPEN");
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM subject_number.subject_risk_state WHERE id = ? "
                                + "AND cleared_at IS NULL AND revision = 3", riskId)).isEqualTo(1);

                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.subject_risk_state SET state = 'CLEARED', "
                                + "cleared_at = now(), revision = revision + 1 "
                                + "WHERE id = ? AND revision = 1", riskId)).isZero();
                assertThat(executeUpdate(connection,
                        "UPDATE subject_number.subject_risk_state SET state = 'CLEARED', "
                                + "cleared_at = now(), revision = revision + 1 "
                                + "WHERE id = ? AND state = 'CLEARED' AND revision = 3", riskId))
                        .isZero();

                switchLocalRole(connection, "subject_number_owner");
                expectSqlFailure(connection, "P0001", "SUBJECT_RISK_STATE_IMMUTABLE",
                        "UPDATE subject_number.subject_risk_state SET risk_type = 'RECYCLED' "
                                + "WHERE id = ?", riskId);
                switchLocalRole(connection, "subject_number_role");
                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE subject_number.subject_risk_state SET risk_type = 'RECYCLED' "
                                + "WHERE id = ?", riskId);

                expectSqlFailure(connection, "23503", "fk_risk_binding",
                        "INSERT INTO subject_number.subject_risk_state "
                                + "(id, caller_number_binding_id, risk_type, source) "
                                + "VALUES (?, ?, 'RECYCLED', 'IMPORT')",
                        UUID.randomUUID(), UUID.randomUUID());

                resetToMigrationIdentity(connection);
                expectSqlFailure(connection, "23503", "fk_risk_binding",
                        "DELETE FROM subject_number.caller_number_binding WHERE id = ?", bindingId);

                assertThat(queryStrings(connection,
                        "SELECT n.nspname FROM pg_namespace n WHERE n.nspname IN "
                                + "('ivr','entitlement_metering','activation_consent') ORDER BY n.nspname"))
                        .as("S2 schemas exist")
                        .containsExactly("activation_consent", "entitlement_metering", "ivr");
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                                + "WHERE n.nspname IN ('entitlement_metering','activation_consent') "
                                + "AND c.relkind = 'r'"))
                        .as("V005 ivr tables exist; entitlement/consent tables absent")
                        .isZero();
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                                + "WHERE n.nspname = 'ivr' AND c.relkind = 'r'"))
                        .as("V005 ivr tables exist; entitlement/consent tables absent")
                        .isEqualTo(6);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM pg_namespace WHERE nspname IN ('evidence','sharing')"))
                        .as("no evidence/sharing schema")
                        .isZero();
                assertThat(toRegclass(connection, "ivr.call_task")).isNull();
                assertThat(toRegclass(connection, "ivr.call_session")).isNull();
                assertThat(toRegclass(connection, "ivr.outbound_call_task")).isNotNull();
                assertThat(toRegclass(connection, "ivr.inbound_session")).isNotNull();
                assertThat(toRegclass(connection, "ivr.call_record")).isNotNull();
                assertThat(toRegclass(connection, "ivr.call_leg")).isNotNull();
                assertThat(toRegclass(connection, "ivr.call_event")).isNotNull();
                assertThat(toRegclass(connection, "ivr.ivr_decision")).isNotNull();
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.subject_risk_state WHERE id = ?", riskId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.caller_number_binding WHERE id = ?", bindingId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.business_subject WHERE id = ?", subjectId))
                    .isZero();
        }
    }

    private static void commitSubjects(UUID... subjectIds) throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "subject_number_role");
                for (UUID subjectId : subjectIds) {
                    assertThat(insertSubject(connection, subjectId, "IVR")).isEqualTo(1);
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void cleanupCommittedBindingsAndSubjects(
            List<UUID> bindingIds, List<UUID> subjectIds) throws SQLException {
        try (Connection connection = openConnection();
                Statement timeout = connection.createStatement()) {
            timeout.execute("SET statement_timeout = 20000");
            for (UUID id : bindingIds) {
                executeUpdate(connection,
                        "DELETE FROM subject_number.caller_number_binding WHERE id = ?", id);
            }
            for (UUID id : subjectIds) {
                executeUpdate(connection,
                        "DELETE FROM subject_number.business_subject WHERE id = ?", id);
            }
            for (UUID id : bindingIds) {
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM subject_number.caller_number_binding WHERE id = ?", id))
                        .isZero();
            }
            for (UUID id : subjectIds) {
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM subject_number.business_subject WHERE id = ?", id))
                        .isZero();
            }
        }
    }

    private static void runConcurrentActiveBindingCommitRace() throws Exception {
        UUID subjectA = UUID.randomUUID();
        UUID subjectB = UUID.randomUUID();
        UUID winnerBinding = UUID.randomUUID();
        UUID loserBinding = UUID.randomUUID();
        String hmac = randomSha256();
        String ciphertext = "v1.s2.race.commit.ciphertext";
        String masked = "137****0025";
        String winnerApplication = "pa3-s2-binding-commit-winner";
        String loserApplication = "pa3-s2-binding-commit-loser";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<UUID> bindingIds = List.of(winnerBinding, loserBinding);
        List<UUID> subjectIds = List.of(subjectA, subjectB);
        try {
            commitSubjects(subjectA, subjectB);
            try (Connection winner = openConnection(); Connection loser = openConnection()) {
                winner.setAutoCommit(false);
                try (Statement statement = winner.createStatement()) {
                    statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
                }
                setLocalRole(winner, "subject_number_role");
                assertThat(insertBinding(winner, winnerBinding, subjectA, "MOBILE", ciphertext,
                        hmac, masked, "ACTIVE", OffsetDateTime.now(ZoneOffset.UTC), 1)).isEqualTo(1);

                loser.setAutoCommit(false);
                configureBlockedWriter(loser, "subject_number_role", loserApplication);
                Future<Integer> loserFuture = executor.submit(() -> insertBinding(
                        loser, loserBinding, subjectB, "MOBILE", ciphertext, hmac, masked, "ACTIVE",
                        OffsetDateTime.now(ZoneOffset.UTC), 1));
                awaitBlockedBackend(loserApplication);

                winner.commit();
                try {
                    awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                    fail("the active-number union loser must not commit a second ACTIVE row");
                } catch (SQLException exception) {
                    assertThat(exception.getSQLState()).isEqualTo("23505");
                    assertThat(exception.getMessage()).contains("uq_binding_active_number");
                }
                loser.rollback();
                setLocalRole(loser, "subject_number_role");
                assertThat(queryString(loser,
                        "SELECT id::text FROM subject_number.caller_number_binding "
                                + "WHERE number_hmac = ? AND status = 'ACTIVE'", hmac))
                        .isEqualTo(winnerBinding.toString());
                loser.rollback();
            }

            try (Connection verify = openConnection()) {
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM subject_number.caller_number_binding "
                                + "WHERE number_hmac = ? AND status = 'ACTIVE'", hmac)).isEqualTo(1);
                assertThat(queryString(verify,
                        "SELECT id::text FROM subject_number.caller_number_binding "
                                + "WHERE number_hmac = ? AND status = 'ACTIVE'", hmac))
                        .isEqualTo(winnerBinding.toString());
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM subject_number.caller_number_binding WHERE id = ?",
                        loserBinding)).isZero();
            }
        } finally {
            executor.shutdownNow();
            cleanupCommittedBindingsAndSubjects(bindingIds, subjectIds);
        }
    }

    private static void runConcurrentActiveBindingRollbackRace() throws Exception {
        UUID subjectA = UUID.randomUUID();
        UUID subjectB = UUID.randomUUID();
        UUID winnerBinding = UUID.randomUUID();
        UUID loserBinding = UUID.randomUUID();
        String hmac = randomSha256();
        String ciphertext = "v1.s2.race.rollback.ciphertext";
        String masked = "137****0026";
        String winnerApplication = "pa3-s2-binding-rollback-winner";
        String loserApplication = "pa3-s2-binding-rollback-loser";

        ExecutorService executor = Executors.newSingleThreadExecutor();
        List<UUID> bindingIds = List.of(winnerBinding, loserBinding);
        List<UUID> subjectIds = List.of(subjectA, subjectB);
        try {
            commitSubjects(subjectA, subjectB);
            try (Connection winner = openConnection(); Connection loser = openConnection()) {
                winner.setAutoCommit(false);
                try (Statement statement = winner.createStatement()) {
                    statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
                }
                setLocalRole(winner, "subject_number_role");
                assertThat(insertBinding(winner, winnerBinding, subjectA, "MOBILE", ciphertext,
                        hmac, masked, "ACTIVE", OffsetDateTime.now(ZoneOffset.UTC), 1)).isEqualTo(1);

                loser.setAutoCommit(false);
                configureBlockedWriter(loser, "subject_number_role", loserApplication);
                Future<Integer> loserFuture = executor.submit(() -> insertBinding(
                        loser, loserBinding, subjectB, "MOBILE", ciphertext, hmac, masked, "ACTIVE",
                        OffsetDateTime.now(ZoneOffset.UTC), 1));
                awaitBlockedBackend(loserApplication);

                winner.rollback();
                assertThat(awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS))
                        .as("the active-number loser inserts after the winner rolls back")
                        .isEqualTo(1);
                loser.commit();
            }

            try (Connection verify = openConnection()) {
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM subject_number.caller_number_binding "
                                + "WHERE number_hmac = ? AND status = 'ACTIVE'", hmac)).isEqualTo(1);
                assertThat(queryString(verify,
                        "SELECT id::text FROM subject_number.caller_number_binding "
                                + "WHERE number_hmac = ? AND status = 'ACTIVE'", hmac))
                        .isEqualTo(loserBinding.toString());
                assertThat(queryLong(verify,
                        "SELECT count(*) FROM subject_number.caller_number_binding WHERE id = ?",
                        winnerBinding)).isZero();
            }
        } finally {
            executor.shutdownNow();
            cleanupCommittedBindingsAndSubjects(bindingIds, subjectIds);
        }
    }

    private static void assertGrants(Connection connection) throws SQLException {
        for (String role : PLATFORM_USAGE_ROLES) {
            assertThat(hasSchema(connection, role, "platform", "USAGE"))
                    .as("%s platform USAGE", role).isTrue();
            assertThat(hasSchema(connection, role, "platform", "CREATE"))
                    .as("%s platform CREATE", role).isFalse();
        }
        for (String role : AUDIT_USAGE_ROLES) {
            assertThat(hasSchema(connection, role, "audit", "USAGE"))
                    .as("%s audit USAGE", role).isTrue();
            assertThat(hasSchema(connection, role, "audit", "CREATE"))
                    .as("%s audit CREATE", role).isFalse();
        }
        assertThat(hasSchema(connection, "platform_reliability_role", "audit", "USAGE")).isFalse();
        assertThat(hasSchema(connection, "platform_dispatcher_role", "audit", "USAGE")).isFalse();
        assertThat(hasSchema(connection, "audit_reader_role", "platform", "USAGE")).isFalse();
        assertThat(hasSchema(connection, "subject_identity_verification_role", "platform", "USAGE")).isFalse();
        assertThat(hasSchema(connection, "operations_service_role", "platform", "USAGE")).isFalse();

        assertThat(hasSchema(connection, "platform_reliability_owner", "platform", "USAGE")).isTrue();
        assertThat(hasSchema(connection, "platform_reliability_owner", "platform", "CREATE")).isTrue();
        assertThat(hasSchema(connection, "audit_owner", "audit", "USAGE")).isTrue();
        assertThat(hasSchema(connection, "audit_owner", "audit", "CREATE")).isTrue();

        Map<String, String> domainSchemaOwners = Map.of(
                "customer_iam", "customer_iam_owner",
                "organization_access", "organization_access_owner",
                "operations_iam", "operations_iam_owner",
                "subject_number", "subject_number_owner");
        Map<String, List<String>> domainSchemaUsageGrantees = Map.of(
                "customer_iam", List.of("customer_iam_owner", "customer_iam_role", "subject_number_role"),
                "organization_access", List.of("customer_iam_role", "organization_access_owner",
                        "organization_access_role", "subject_number_role"),
                "operations_iam", List.of("operations_iam_owner", "operations_iam_role"),
                "subject_number", List.of("subject_number_owner", "subject_number_role",
                        "subject_identity_verification_role"));
        for (Map.Entry<String, String> entry : domainSchemaOwners.entrySet()) {
            String schema = entry.getKey();
            List<String> usageGrantees = domainSchemaUsageGrantees.get(schema);
            for (String role : S1_ROLES) {
                assertThat(hasSchema(connection, role, schema, "USAGE"))
                        .as("%s %s USAGE", role, schema)
                        .isEqualTo(usageGrantees.contains(role));
                assertThat(hasSchema(connection, role, schema, "CREATE"))
                        .as("%s %s CREATE", role, schema)
                        .isEqualTo(role.equals(entry.getValue()));
            }
        }

        for (String role : S1_ROLES) {
            assertThat(hasSchema(connection, role, "public", "USAGE"))
                    .as("%s public USAGE", role).isFalse();
            assertThat(hasSchema(connection, role, "public", "CREATE"))
                    .as("%s public CREATE", role).isFalse();
        }

        assertV002OwnedAndCrossModuleGrants(connection);

        assertTablePrivileges(connection, "platform_reliability_role",
                "platform.idempotency_record", true, false, false, false, false);
        assertTablePrivileges(connection, "platform_reliability_role",
                "platform.outbox_event", true, false, false, false, false);
        assertTablePrivileges(connection, "platform_reliability_role",
                "audit.audit_fact", false, false, false, false, false);
        assertTablePrivileges(connection, "platform_dispatcher_role",
                "platform.outbox_event", true, false, false, false, false);
        assertTablePrivileges(connection, "platform_dispatcher_role",
                "platform.idempotency_record", false, false, false, false, false);
        assertTablePrivileges(connection, "platform_dispatcher_role",
                "audit.audit_fact", false, false, false, false, false);
        assertTablePrivileges(connection, "audit_reader_role",
                "audit.audit_fact", true, false, false, false, false);
        assertTablePrivileges(connection, "audit_reader_role",
                "platform.idempotency_record", false, false, false, false, false);
        assertTablePrivileges(connection, "audit_reader_role",
                "platform.outbox_event", false, false, false, false, false);

        for (String role : List.of(
                "customer_iam_role", "organization_access_role", "operations_iam_role",
                "subject_number_role", "subject_identity_verification_role", "operations_service_role")) {
            for (String table : S1_TABLES) {
                assertTablePrivileges(connection, role, table, false, false, false, false, false);
            }
        }

        for (String column : List.of("status", "attempt_count", "available_at", "published_at", "revision")) {
            assertThat(hasColumn(connection, "platform_dispatcher_role", "platform.outbox_event", column, "UPDATE"))
                    .as("dispatcher UPDATE %s", column).isTrue();
        }
        for (String column : List.of("id", "event_domain", "dedupe_key", "aggregate_type", "aggregate_id",
                "event_type", "payload_redacted", "payload_ciphertext", "payload_sha256")) {
            assertThat(hasColumn(connection, "platform_dispatcher_role", "platform.outbox_event", column, "UPDATE"))
                    .as("dispatcher must not UPDATE %s", column).isFalse();
        }
        assertThat(hasColumn(connection, "platform_reliability_role",
                "platform.outbox_event", "status", "UPDATE")).isFalse();

        for (String role : DOMAIN_ROLES) {
            for (String function : PLATFORM_FUNCTIONS) {
                assertThat(hasFunction(connection, role, function, "EXECUTE"))
                        .as("%s EXECUTE %s", role, function).isTrue();
            }
        }
        for (String role : List.of(
                "platform_reliability_role", "platform_dispatcher_role", "audit_reader_role",
                "subject_identity_verification_role", "operations_service_role")) {
            for (String function : PLATFORM_FUNCTIONS) {
                assertThat(hasFunction(connection, role, function, "EXECUTE"))
                        .as("%s must not EXECUTE %s", role, function).isFalse();
            }
        }

        for (String role : List.of(
                "customer_iam_role", "organization_access_role", "operations_iam_role",
                "subject_number_role", "subject_identity_verification_role", "operations_service_role")) {
            assertThat(hasFunction(connection, role, AUDIT_FUNCTION, "EXECUTE"))
                    .as("%s EXECUTE audit.append_fact", role).isTrue();
        }
        for (String role : List.of(
                "platform_reliability_role", "platform_dispatcher_role", "audit_reader_role")) {
            assertThat(hasFunction(connection, role, AUDIT_FUNCTION, "EXECUTE"))
                    .as("%s must not EXECUTE audit.append_fact", role).isFalse();
        }
        for (String role : S1_ROLES) {
            if (role.equals("audit_owner")) {
                continue;
            }
            assertThat(hasFunction(connection, role, "audit.reject_row_mutation()", "EXECUTE"))
                    .as("%s must not EXECUTE audit.reject_row_mutation", role).isFalse();
        }

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_namespace n, "
                        + "aclexplode(coalesce(n.nspacl, acldefault('n', n.nspowner))) a "
                        + "WHERE n.nspname IN ('public','platform','audit','customer_iam',"
                        + "'organization_access','operations_iam','subject_number') AND a.grantee = 0"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace, "
                        + "aclexplode(coalesce(c.relacl, acldefault('r', c.relowner))) a "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access',"
                        + "'operations_iam','subject_number') AND c.relkind IN ('r','p') AND a.grantee = 0"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace, "
                        + "aclexplode(coalesce(p.proacl, acldefault('f', p.proowner))) a "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access',"
                        + "'operations_iam','subject_number') "
                        + "AND a.grantee = 0"))
                .isZero();

        assertV003OwnedAndCrossModuleGrants(connection);
    }

    private static void assertV002OwnedAndCrossModuleGrants(Connection connection) throws SQLException {
        // Owned tables grant table-level SELECT+INSERT only; UPDATE is column-level only,
        // so table-level UPDATE is false while the exact UPDATE columns remain true below.
        assertTablePrivileges(connection, "customer_iam_role",
                "customer_iam.customer_account", true, true, false, false, false);
        assertTablePrivileges(connection, "customer_iam_role",
                "customer_iam.auth_identity", true, true, false, false, false);
        assertTablePrivileges(connection, "customer_iam_role",
                "customer_iam.identity_profile", true, true, false, false, false);
        assertTablePrivileges(connection, "organization_access_role",
                "organization_access.organization", true, true, false, false, false);
        assertTablePrivileges(connection, "organization_access_role",
                "organization_access.organization_membership", true, true, false, false, false);
        assertTablePrivileges(connection, "operations_iam_role",
                "operations_iam.employee_account", true, true, false, false, false);

        Map<String, List<String>> ownedUpdateColumns = new java.util.LinkedHashMap<>();
        ownedUpdateColumns.put("customer_iam.customer_account",
                List.of("display_name", "status", "revision", "updated_at"));
        ownedUpdateColumns.put("customer_iam.auth_identity",
                List.of("status", "verified_at", "key_version", "revision"));
        ownedUpdateColumns.put("customer_iam.identity_profile",
                List.of("real_name_status", "legal_name_ciphertext", "id_number_ciphertext",
                        "id_number_hmac", "verification_ref_ciphertext", "verified_at", "revision"));
        ownedUpdateColumns.put("organization_access.organization",
                List.of("display_name", "status", "revision", "updated_at"));
        ownedUpdateColumns.put("organization_access.organization_membership",
                List.of("role", "department_label", "status", "revision"));
        ownedUpdateColumns.put("operations_iam.employee_account",
                List.of("display_name", "role", "status", "revision"));

        Map<String, String> owningRoleByTable = Map.of(
                "customer_iam.customer_account", "customer_iam_role",
                "customer_iam.auth_identity", "customer_iam_role",
                "customer_iam.identity_profile", "customer_iam_role",
                "organization_access.organization", "organization_access_role",
                "organization_access.organization_membership", "organization_access_role",
                "operations_iam.employee_account", "operations_iam_role");

        Map<String, List<String>> allColumnsByTable = new java.util.LinkedHashMap<>();
        allColumnsByTable.put("customer_iam.customer_account",
                List.of("id", "display_name", "status", "revision", "created_at", "updated_at"));
        allColumnsByTable.put("customer_iam.auth_identity",
                List.of("id", "account_id", "provider", "provider_scope", "provider_subject_ciphertext",
                        "provider_subject_hmac", "status", "verified_at", "key_version", "revision"));
        allColumnsByTable.put("customer_iam.identity_profile",
                List.of("id", "account_id", "real_name_status", "legal_name_ciphertext",
                        "id_number_ciphertext", "id_number_hmac", "verification_ref_ciphertext",
                        "verified_at", "revision"));
        allColumnsByTable.put("organization_access.organization",
                List.of("id", "display_name", "status", "revision", "created_at", "updated_at"));
        allColumnsByTable.put("organization_access.organization_membership",
                List.of("id", "organization_id", "account_id", "role", "department_label", "status", "revision"));
        allColumnsByTable.put("operations_iam.employee_account",
                List.of("id", "login_name_hmac", "login_name_masked", "auth_issuer", "auth_subject_hmac",
                        "display_name", "role", "status", "revision"));

        for (Map.Entry<String, List<String>> entry : ownedUpdateColumns.entrySet()) {
            String table = entry.getKey();
            String role = owningRoleByTable.get(table);
            for (String column : entry.getValue()) {
                assertThat(hasColumn(connection, role, table, column, "UPDATE"))
                        .as("%s UPDATE %s.%s", role, table, column).isTrue();
            }
            for (String column : allColumnsByTable.get(table)) {
                if (entry.getValue().contains(column)) {
                    continue;
                }
                assertThat(hasColumn(connection, role, table, column, "UPDATE"))
                        .as("%s must not UPDATE %s.%s", role, table, column).isFalse();
            }
        }

        Map<String, List<String>> crossSelectColumns = new java.util.LinkedHashMap<>();
        crossSelectColumns.put("customer_iam_role|organization_access.organization",
                List.of("id", "display_name", "status"));
        crossSelectColumns.put("customer_iam_role|organization_access.organization_membership",
                List.of("organization_id", "account_id", "role", "status"));
        crossSelectColumns.put("subject_number_role|organization_access.organization",
                List.of("id", "status"));
        crossSelectColumns.put("subject_number_role|organization_access.organization_membership",
                List.of("organization_id", "account_id", "role", "status"));
        crossSelectColumns.put("subject_number_role|customer_iam.customer_account",
                List.of("id", "status"));
        for (Map.Entry<String, List<String>> entry : crossSelectColumns.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            String role = parts[0];
            String table = parts[1];
            assertThat(hasTable(connection, role, table, "SELECT"))
                    .as("%s must not hold table-level SELECT on %s", role, table).isFalse();
            for (String column : entry.getValue()) {
                assertThat(hasColumn(connection, role, table, column, "SELECT"))
                        .as("%s SELECT %s.%s", role, table, column).isTrue();
            }
            for (String column : allColumnsByTable.get(table)) {
                if (entry.getValue().contains(column)) {
                    continue;
                }
                assertThat(hasColumn(connection, role, table, column, "SELECT"))
                        .as("%s must not SELECT %s.%s", role, table, column).isFalse();
            }
            assertThat(hasTable(connection, role, table, "INSERT")).isFalse();
            assertThat(hasTable(connection, role, table, "UPDATE")).isFalse();
            assertThat(hasTable(connection, role, table, "DELETE")).isFalse();
            assertThat(hasTable(connection, role, table, "TRUNCATE")).isFalse();
        }

        assertThat(hasTable(connection, "subject_number_role", "customer_iam.customer_account", "SELECT"))
                .isFalse();
        assertThat(hasTable(connection, "customer_iam_role", "organization_access.organization", "SELECT"))
                .isFalse();
        assertThat(hasTable(connection, "customer_iam_role", "organization_access.organization_membership", "SELECT"))
                .isFalse();

        Map<String, String> owningOwnerByTable = Map.of(
                "customer_iam.customer_account", "customer_iam_owner",
                "customer_iam.auth_identity", "customer_iam_owner",
                "customer_iam.identity_profile", "customer_iam_owner",
                "organization_access.organization", "organization_access_owner",
                "organization_access.organization_membership", "organization_access_owner",
                "operations_iam.employee_account", "operations_iam_owner");
        for (String table : V002_TABLES) {
            for (String role : S1_ROLES) {
                if (role.equals(owningOwnerByTable.get(table))) {
                    continue;
                }
                assertThat(hasTable(connection, role, table, "DELETE"))
                        .as("%s DELETE %s", role, table).isFalse();
                assertThat(hasTable(connection, role, table, "TRUNCATE"))
                        .as("%s TRUNCATE %s", role, table).isFalse();
                assertThat(hasTable(connection, role, table, "REFERENCES"))
                        .as("%s REFERENCES %s", role, table).isFalse();
                assertThat(hasTable(connection, role, table, "TRIGGER"))
                        .as("%s TRIGGER %s", role, table).isFalse();
            }
        }

        // Complete expected effective-privilege matrix (S1 fix 1, finding 2): every one of
        // the 15 S1 roles against every one of the six V002 tables for table-level and
        // column-level SELECT/INSERT/UPDATE. Table owners model inherent privileges
        // separately from explicit ACL grants (owner: all true). Owning runtime roles hold
        // table-level SELECT/INSERT true and table-level UPDATE false because only exact
        // columns are granted UPDATE. The five cross-module pairs hold only their exact
        // column SELECT grants (table-level SELECT/INSERT/UPDATE all false). Every other
        // role/table pair must be all-false with no column-level leakage, so an undeclared
        // GRANT to operations_iam_role, a platform role or another owner-role on the wrong
        // table flips an expected false and fails closed.
        for (String table : V002_TABLES) {
            String owner = owningOwnerByTable.get(table);
            String runtimeRole = owningRoleByTable.get(table);
            List<String> allColumns = allColumnsByTable.get(table);
            List<String> allowedUpdateColumns = ownedUpdateColumns.get(table);
            for (String role : S1_ROLES) {
                boolean ownerRow = role.equals(owner);
                boolean runtimeRow = role.equals(runtimeRole);
                boolean expectTableSelect = ownerRow || runtimeRow;
                boolean expectTableInsert = ownerRow || runtimeRow;
                boolean expectTableUpdate = ownerRow;
                assertThat(hasTable(connection, role, table, "SELECT"))
                        .as("matrix %s table SELECT %s", role, table)
                        .isEqualTo(expectTableSelect);
                assertThat(hasTable(connection, role, table, "INSERT"))
                        .as("matrix %s table INSERT %s", role, table)
                        .isEqualTo(expectTableInsert);
                assertThat(hasTable(connection, role, table, "UPDATE"))
                        .as("matrix %s table UPDATE %s", role, table)
                        .isEqualTo(expectTableUpdate);
                List<String> crossColumns = crossSelectColumns.get(role + "|" + table);
                for (String column : allColumns) {
                    boolean expectColumnSelect = expectTableSelect
                            || (crossColumns != null && crossColumns.contains(column));
                    boolean expectColumnInsert = expectTableInsert;
                    boolean expectColumnUpdate = ownerRow
                            || (runtimeRow && allowedUpdateColumns.contains(column));
                    assertThat(hasColumn(connection, role, table, column, "SELECT"))
                            .as("matrix %s column SELECT %s.%s", role, table, column)
                            .isEqualTo(expectColumnSelect);
                    assertThat(hasColumn(connection, role, table, column, "INSERT"))
                            .as("matrix %s column INSERT %s.%s", role, table, column)
                            .isEqualTo(expectColumnInsert);
                    assertThat(hasColumn(connection, role, table, column, "UPDATE"))
                            .as("matrix %s column UPDATE %s.%s", role, table, column)
                            .isEqualTo(expectColumnUpdate);
                }
            }
        }

        Map<String, String> guardFunctionGrantRole = Map.of(
                "customer_iam.guard_customer_account()", "customer_iam_role",
                "customer_iam.guard_auth_identity()", "customer_iam_role",
                "customer_iam.guard_identity_profile()", "customer_iam_role",
                "organization_access.guard_organization()", "organization_access_role",
                "organization_access.guard_membership()", "organization_access_role",
                "operations_iam.guard_employee_account()", "operations_iam_role");
        Map<String, String> guardFunctionOwnerRole = Map.of(
                "customer_iam.guard_customer_account()", "customer_iam_owner",
                "customer_iam.guard_auth_identity()", "customer_iam_owner",
                "customer_iam.guard_identity_profile()", "customer_iam_owner",
                "organization_access.guard_organization()", "organization_access_owner",
                "organization_access.guard_membership()", "organization_access_owner",
                "operations_iam.guard_employee_account()", "operations_iam_owner");
        for (Map.Entry<String, String> entry : guardFunctionGrantRole.entrySet()) {
            String function = entry.getKey();
            String grantedRole = entry.getValue();
            assertThat(hasFunction(connection, grantedRole, function, "EXECUTE"))
                    .as("%s EXECUTE %s", grantedRole, function).isTrue();
            for (String role : S1_ROLES) {
                if (role.equals(grantedRole) || role.equals(guardFunctionOwnerRole.get(function))) {
                    continue;
                }
                assertThat(hasFunction(connection, role, function, "EXECUTE"))
                        .as("%s must not EXECUTE %s", role, function).isFalse();
            }
            assertThat(hasFunction(connection, "public", function, "EXECUTE"))
                    .as("PUBLIC must not EXECUTE %s", function).isFalse();
        }
    }

    private static void assertTablePrivileges(
            Connection connection,
            String role,
            String table,
            boolean select,
            boolean insert,
            boolean update,
            boolean delete,
            boolean truncate) throws SQLException {
        assertThat(hasTable(connection, role, table, "SELECT")).as("%s SELECT %s", role, table).isEqualTo(select);
        assertThat(hasTable(connection, role, table, "INSERT")).as("%s INSERT %s", role, table).isEqualTo(insert);
        assertThat(hasTable(connection, role, table, "UPDATE")).as("%s UPDATE %s", role, table).isEqualTo(update);
        assertThat(hasTable(connection, role, table, "DELETE")).as("%s DELETE %s", role, table).isEqualTo(delete);
        assertThat(hasTable(connection, role, table, "TRUNCATE")).as("%s TRUNCATE %s", role, table).isEqualTo(truncate);
    }

    private static boolean hasSchema(Connection connection, String role, String schema, String privilege)
            throws SQLException {
        return queryBoolean(connection, "SELECT has_schema_privilege(?, ?, ?)", role, schema, privilege);
    }

    private static boolean hasTable(Connection connection, String role, String table, String privilege)
            throws SQLException {
        return queryBoolean(connection, "SELECT has_table_privilege(?, ?, ?)", role, table, privilege);
    }

    private static boolean hasColumn(
            Connection connection, String role, String table, String column, String privilege) throws SQLException {
        return queryBoolean(connection, "SELECT has_column_privilege(?, ?, ?, ?)", role, table, column, privilege);
    }

    private static boolean hasFunction(Connection connection, String role, String function, String privilege)
            throws SQLException {
        return queryBoolean(connection, "SELECT has_function_privilege(?, ?, ?)", role, function, privilege);
    }

    private static void cleanupBrokenSchemas() throws SQLException {
        try (Connection connection = openConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + BROKEN_MARKER_SCHEMA + " CASCADE");
            statement.execute("DROP SCHEMA IF EXISTS " + BROKEN_HISTORY_SCHEMA + " CASCADE");
        }
    }

    private static List<String> causeMessages(Throwable throwable) {
        List<String> messages = new ArrayList<>();
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            messages.add(String.valueOf(current.getMessage()));
        }
        return messages;
    }

    private static String toRegclass(Connection connection, String name) throws SQLException {
        return queryString(connection, "SELECT to_regclass(?)::text", name);
    }

    private static String toRegnamespace(Connection connection, String name) throws SQLException {
        return queryString(connection, "SELECT to_regnamespace(?)::text", name);
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, DB_USER, PASSWORD);
    }

    private static List<String> queryStrings(Connection connection, String sql, Object... parameters)
            throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(sql)) {
            bind(prepared, parameters);
            try (ResultSet resultSet = prepared.executeQuery()) {
                List<String> values = new ArrayList<>();
                while (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
                return values;
            }
        }
    }

    private static String queryString(Connection connection, String sql, Object... parameters) throws SQLException {
        List<String> values = queryStrings(connection, sql, parameters);
        assertThat(values).hasSize(1);
        return values.get(0);
    }

    private static long queryLong(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(sql)) {
            bind(prepared, parameters);
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getLong(1);
            }
        }
    }

    private static boolean queryBoolean(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement prepared = connection.prepareStatement(sql)) {
            bind(prepared, parameters);
            try (ResultSet resultSet = prepared.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getBoolean(1);
            }
        }
    }

    private static void bind(PreparedStatement prepared, Object... parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            prepared.setObject(index + 1, parameters[index]);
        }
    }

    // ------------------------------------------------------------------
    // PA4 V004 S2: lifecycle, guard, FK, named-check and concurrency helpers
    // ------------------------------------------------------------------

    private static List<String> v004ExpectedCheckNames(String... tables) {
        assertThat(V004_CHECK_PROBES).as("V004_CHECK_PROBES has exactly 51 entries").hasSize(51);
        List<String> expectedAll = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : V004_CHECK_DEF_MAP.entrySet()) {
            for (String name : entry.getValue().keySet()) {
                expectedAll.add(entry.getKey() + "|" + name);
            }
        }
        assertThat(expectedAll)
                .as("V004_CHECK_PROBES equals flattened V004_CHECK_DEF_MAP name set")
                .containsExactlyInAnyOrderElementsOf(V004_CHECK_PROBES);
        List<String> subset = new ArrayList<>();
        for (String table : tables) {
            assertThat(V004_CHECK_DEF_MAP).as("check map for %s", table).containsKey(table);
            subset.addAll(V004_CHECK_DEF_MAP.get(table).keySet());
        }
        return subset;
    }

    private static String randomProductCode() {
        return "P" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

    private static int insertCatalogProduct(Connection connection, UUID id, String productCode)
            throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO catalog_order.catalog_product (id, product_code, product_family) "
                        + "VALUES (?, ?, 'CORE')",
                id, productCode);
    }

    private static int insertCatalogVersion(
            Connection connection,
            UUID id,
            UUID productId,
            int versionNo,
            String saleStatus,
            String offerType,
            String audience,
            Long priceCents,
            String currency,
            String billingCycle,
            int installments,
            int grantIntervalMonths,
            String snapshot,
            String snapshotSha256,
            UUID createdByEmployeeId,
            OffsetDateTime publishedAt,
            int revision) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO catalog_order.catalog_version "
                        + "(id, product_id, version_no, sale_status, offer_type, audience, "
                        + "price_cents, currency, billing_cycle, installments, "
                        + "grant_interval_months, snapshot, snapshot_sha256, "
                        + "created_by_employee_id, published_at, revision) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)",
                id, productId, versionNo, saleStatus, offerType, audience, priceCents, currency,
                billingCycle, installments, grantIntervalMonths, snapshot, snapshotSha256,
                createdByEmployeeId, publishedAt, revision);
    }

    private static int insertCatalogOffer(
            Connection connection, UUID id, UUID catalogVersionId, String channel, String versionToken)
            throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO catalog_order.catalog_offer "
                        + "(id, catalog_version_id, channel, offer_status, version_token, revision) "
                        + "VALUES (?, ?, ?, 'ACTIVE', ?, 1)",
                id, catalogVersionId, channel, versionToken);
    }

    private static int insertCatalogBenefitLine(
            Connection connection, UUID id, UUID catalogVersionId, String entitlementType,
            long quantity, int validityDays, int grantIntervalMonths) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO catalog_order.catalog_benefit_line "
                        + "(id, catalog_version_id, entitlement_type, quantity, validity_days, "
                        + "grant_interval_months) VALUES (?, ?, ?, ?, ?, ?)",
                id, catalogVersionId, entitlementType, quantity, validityDays, grantIntervalMonths);
    }

    private static int insertCustomerOrder(
            Connection connection,
            UUID id,
            String orderNo,
            UUID accountId,
            UUID organizationId,
            UUID subjectId,
            UUID catalogVersionId,
            UUID catalogOfferId,
            UUID purchaseConsentId,
            long amountCents,
            String currency,
            String paymentStatus,
            String snapshot,
            String snapshotSha256,
            String versionToken,
            String idempotencyKey,
            String requestFingerprint,
            OffsetDateTime createdAt,
            OffsetDateTime paidAt,
            OffsetDateTime closedAt,
            int revision) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO catalog_order.customer_order "
                        + "(id, order_no, account_id, owner_organization_id, subject_id, channel, "
                        + "catalog_version_id, catalog_offer_id, purchase_consent_id, amount_cents, "
                        + "currency, payment_status, snapshot, snapshot_sha256, version_token, "
                        + "idempotency_key, request_fingerprint, revision, created_at, paid_at, "
                        + "closed_at) "
                        + "VALUES (?, ?, ?, ?, ?, 'WEB', ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?)",
                id, orderNo, accountId, organizationId, subjectId, catalogVersionId,
                catalogOfferId, purchaseConsentId, amountCents, currency, paymentStatus, snapshot,
                snapshotSha256, versionToken, idempotencyKey, requestFingerprint, revision,
                createdAt, paidAt, closedAt);
    }

    private static int insertPaymentOrder(
            Connection connection,
            UUID id,
            UUID orderId,
            String merchantOrderNo,
            String providerTransactionId,
            String status,
            long amountCents,
            String currency,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            int revision) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO catalog_order.payment_order "
                        + "(id, order_id, provider, merchant_order_no, provider_transaction_id, "
                        + "status, amount_cents, currency, created_at, updated_at, revision) "
                        + "VALUES (?, ?, 'WECHAT', ?, ?, ?, ?, ?, ?, ?, ?)",
                id, orderId, merchantOrderNo, providerTransactionId, status, amountCents, currency,
                createdAt, updatedAt, revision);
    }

    private static int insertPaymentEvent(
            Connection connection,
            UUID id,
            UUID paymentOrderId,
            String providerEventId,
            String eventType,
            boolean signatureVerified,
            String payloadCiphertext,
            String payloadSha256,
            String transactionId,
            long amountCents,
            OffsetDateTime occurredAt) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO catalog_order.payment_event "
                        + "(id, payment_order_id, provider, provider_event_id, event_type, "
                        + "signature_verified, payload_ciphertext, payload_sha256, transaction_id, "
                        + "amount_cents, occurred_at) "
                        + "VALUES (?, ?, 'WECHAT', ?, ?, ?, ?, ?, ?, ?, ?)",
                id, paymentOrderId, providerEventId, eventType, signatureVerified,
                payloadCiphertext, payloadSha256, transactionId, amountCents, occurredAt);
    }

    private static void assertV004CatalogVersionLifecycleAndGuardBackstops() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID secondVersionId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID benefitId = UUID.randomUUID();
        String snapshotSha = randomSha256();
        String snapshot = "{\"plan\":\"pa4-s2\"}";

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                assertThat(insertEmployee(connection, employeeId, randomSha256(), "s2***cat",
                        "onecall-ops", randomSha256(), "PA4 S2 Catalog Employee", "ADMIN"))
                        .isEqualTo(1);
                setLocalRole(connection, "catalog_order_role");
                assertThat(insertCatalogProduct(connection, productId, randomProductCode()))
                        .isEqualTo(1);
                assertThat(insertCatalogVersion(connection, versionId, productId, 1, "DRAFT",
                        "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1, snapshot,
                        snapshotSha, employeeId, null, 1)).isEqualTo(1);
                assertThat(insertCatalogVersion(connection, secondVersionId, productId, 2, "DRAFT",
                        "ONE_TIME", "PERSONAL", 5000L, "CNY", "ONCE", 1, 0,
                        "{\"plan\":\"pa4-s2-v2\"}", randomSha256(), employeeId, null, 1))
                        .isEqualTo(1);
                assertThat(insertCatalogOffer(connection, offerId, versionId, "WEB", "s2-token-1"))
                        .isEqualTo(1);
                assertThat(insertCatalogBenefitLine(connection, benefitId, versionId,
                        "RECORDING_SECONDS", 100, 30, 1)).isEqualTo(1);

                // Valid conditional revision update and stale-revision zero row.
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_version SET revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 1",
                        versionId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_version SET revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 1",
                        versionId)).as("stale revision update affects zero rows").isZero();

                // Publication: DRAFT -> ACTIVE sets published_at and bumps revision.
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_version "
                                + "SET sale_status = 'ACTIVE', published_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 2",
                        versionId)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT sale_status || '|' || (published_at IS NOT NULL)::text "
                                + "|| '|' || revision::text "
                                + "FROM catalog_order.catalog_version WHERE id = ?",
                        versionId)).isEqualTo("ACTIVE|true|3");

                // Published content update is immutable (owner path; trigger fires first).
                switchLocalRole(connection, "catalog_order_owner");
                expectSqlFailure(connection, "P0001", "CATALOG_VERSION_PUBLISHED_IMMUTABLE",
                        "UPDATE catalog_order.catalog_version SET snapshot = "
                                + "'{\"tampered\":true}'::jsonb WHERE id = ?",
                        versionId);
                expectSqlFailure(connection, "P0001", "CATALOG_VERSION_PUBLISHED_IMMUTABLE",
                        "UPDATE catalog_order.catalog_version SET published_at = NULL WHERE id = ?",
                        versionId);

                // Offer mutable updates succeed under the runtime role; relation
                // changes raise the guard on the owner path.
                switchLocalRole(connection, "catalog_order_role");
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_offer "
                                + "SET version_token = 's2-token-2', revision = revision + 1 "
                                + "WHERE id = ? AND revision = 1",
                        offerId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_offer "
                                + "SET offer_status = 'WITHDRAWN', revision = revision + 1 "
                                + "WHERE id = ? AND revision = 2",
                        offerId)).isEqualTo(1);
                switchLocalRole(connection, "catalog_order_owner");
                expectSqlFailure(connection, "P0001", "CATALOG_OFFER_RELATION_IMMUTABLE",
                        "UPDATE catalog_order.catalog_offer SET channel = 'MINIAPP' WHERE id = ?",
                        offerId);
                expectSqlFailure(connection, "P0001", "CATALOG_OFFER_RELATION_IMMUTABLE",
                        "UPDATE catalog_order.catalog_offer SET catalog_version_id = ? WHERE id = ?",
                        UUID.randomUUID(), offerId);

                // Second ACTIVE on the same product fails the partial unique index
                // while the first published version remains ACTIVE.
                switchLocalRole(connection, "catalog_order_role");
                expectSqlFailure(connection, "23505", "uq_catalog_version_active",
                        "UPDATE catalog_order.catalog_version "
                                + "SET sale_status = 'ACTIVE', published_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 1",
                        secondVersionId);

                // Legally retire the first ACTIVE (ACTIVE -> OFFSALE, terminal per
                // docs/14 7.3) and then publish the second DRAFT. OFFSALE is never
                // reactivated: these are synthetic DB backstop probes only, and
                // service-owned terminality is not proven here.
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_version "
                                + "SET sale_status = 'OFFSALE', revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'ACTIVE' AND revision = 3",
                        versionId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_version "
                                + "SET sale_status = 'ACTIVE', published_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 1",
                        secondVersionId)).isEqualTo(1);
                // Retire-then-activate leaves exactly one ACTIVE.
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM catalog_order.catalog_version "
                                + "WHERE product_id = ? AND sale_status = 'ACTIVE'",
                        productId)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT id::text FROM catalog_order.catalog_version "
                                + "WHERE product_id = ? AND sale_status = 'ACTIVE'",
                        productId)).isEqualTo(secondVersionId.toString());

                // Product/benefit append-only: runtime 42501, owner P0001.
                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE catalog_order.catalog_product SET product_family = 'OTHER' WHERE id = ?",
                        productId);
                expectSqlFailure(connection, "42501", "permission denied",
                        "DELETE FROM catalog_order.catalog_product WHERE id = ?", productId);
                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE catalog_order.catalog_benefit_line SET quantity = 1 WHERE id = ?",
                        benefitId);
                expectSqlFailure(connection, "42501", "permission denied",
                        "DELETE FROM catalog_order.catalog_benefit_line WHERE id = ?", benefitId);
                switchLocalRole(connection, "catalog_order_owner");
                expectSqlFailure(connection, "P0001", "CATALOG_ROW_APPEND_ONLY",
                        "UPDATE catalog_order.catalog_product SET product_family = 'OTHER' WHERE id = ?",
                        productId);
                expectSqlFailure(connection, "P0001", "CATALOG_ROW_APPEND_ONLY",
                        "DELETE FROM catalog_order.catalog_product WHERE id = ?", productId);
                expectSqlFailure(connection, "P0001", "CATALOG_ROW_APPEND_ONLY",
                        "UPDATE catalog_order.catalog_benefit_line SET quantity = 1 WHERE id = ?",
                        benefitId);
                expectSqlFailure(connection, "P0001", "CATALOG_ROW_APPEND_ONLY",
                        "DELETE FROM catalog_order.catalog_benefit_line WHERE id = ?", benefitId);
                switchLocalRole(connection, "catalog_order_role");

                // One 23514+exact-name probe for each of the 22 product/version/offer/benefit
                // checks (plan 8.4).
                List<CheckProbe> probes = List.of(
                        checkProbe("ck_catalog_product_code",
                                "INSERT INTO catalog_order.catalog_product "
                                        + "(id, product_code, product_family) VALUES (?, ?, 'CORE')",
                                UUID.randomUUID(), "bad"),
                        checkProbe("ck_catalog_product_family",
                                "INSERT INTO catalog_order.catalog_product "
                                        + "(id, product_code, product_family) VALUES (?, ?, 'bad')",
                                UUID.randomUUID(), randomProductCode()),
                        checkProbe("ck_catalog_version_no",
                                versionProbeSql(), UUID.randomUUID(), productId, 0, "DRAFT",
                                "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                                snapshot, snapshotSha, employeeId, null, 1),
                        checkProbe("ck_catalog_version_sale_status",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "INVALID",
                                "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                                snapshot, snapshotSha, employeeId,
                                OffsetDateTime.now(ZoneOffset.UTC), 1),
                        checkProbe("ck_catalog_version_offer_type",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "DRAFT",
                                "INVALID", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                                snapshot, snapshotSha, employeeId, null, 1),
                        checkProbe("ck_catalog_version_audience",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "DRAFT",
                                "SUBSCRIPTION", "INVALID", 9900L, "CNY", "MONTHLY", 1, 1,
                                snapshot, snapshotSha, employeeId, null, 1),
                        checkProbe("ck_catalog_version_audience_shape",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "DRAFT",
                                "ONE_TIME", "ENTERPRISE", 9900L, "CNY", "ONCE", 1, 0,
                                snapshot, snapshotSha, employeeId, null, 1),
                        checkProbe("ck_catalog_version_price",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "DRAFT",
                                "ONE_TIME", "PERSONAL", null, "CNY", "ONCE", 1, 0,
                                snapshot, snapshotSha, employeeId, null, 1),
                        checkProbe("ck_catalog_version_currency",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "DRAFT",
                                "SUBSCRIPTION", "PERSONAL", 9900L, "CN", "MONTHLY", 1, 1,
                                snapshot, snapshotSha, employeeId, null, 1),
                        checkProbe("ck_catalog_version_billing_matrix",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "DRAFT",
                                "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "ONCE", 1, 1,
                                snapshot, snapshotSha, employeeId, null, 1),
                        checkProbe("ck_catalog_version_snapshot_type",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "DRAFT",
                                "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                                "[]", snapshotSha, employeeId, null, 1),
                        checkProbe("ck_catalog_version_snapshot_sha",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "DRAFT",
                                "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                                snapshot, "abc", employeeId, null, 1),
                        checkProbe("ck_catalog_version_published_shape",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "ACTIVE",
                                "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                                snapshot, snapshotSha, employeeId, null, 1),
                        checkProbe("ck_catalog_version_revision",
                                versionProbeSql(), UUID.randomUUID(), productId, 3, "DRAFT",
                                "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                                snapshot, snapshotSha, employeeId, null, 0),
                        checkProbe("ck_catalog_offer_channel",
                                "INSERT INTO catalog_order.catalog_offer "
                                        + "(id, catalog_version_id, channel, offer_status, "
                                        + "version_token, revision) VALUES (?, ?, ?, 'ACTIVE', "
                                        + "'s2-token', 1)",
                                UUID.randomUUID(), versionId, "IVR"),
                        checkProbe("ck_catalog_offer_status",
                                "INSERT INTO catalog_order.catalog_offer "
                                        + "(id, catalog_version_id, channel, offer_status, "
                                        + "version_token, revision) VALUES (?, ?, 'WEB', ?, "
                                        + "'s2-token', 1)",
                                UUID.randomUUID(), versionId, "RETIRED"),
                        checkProbe("ck_catalog_offer_token",
                                "INSERT INTO catalog_order.catalog_offer "
                                        + "(id, catalog_version_id, channel, offer_status, "
                                        + "version_token, revision) VALUES (?, ?, 'WEB', 'ACTIVE', "
                                        + "?, 1)",
                                UUID.randomUUID(), versionId, "   "),
                        checkProbe("ck_catalog_offer_revision",
                                "INSERT INTO catalog_order.catalog_offer "
                                        + "(id, catalog_version_id, channel, offer_status, "
                                        + "version_token, revision) VALUES (?, ?, 'WEB', 'ACTIVE', "
                                        + "'s2-token', 0)",
                                UUID.randomUUID(), versionId),
                        checkProbe("ck_catalog_benefit_type",
                                "INSERT INTO catalog_order.catalog_benefit_line "
                                        + "(id, catalog_version_id, entitlement_type, quantity, "
                                        + "validity_days, grant_interval_months) "
                                        + "VALUES (?, ?, ?, 1, 30, 1)",
                                UUID.randomUUID(), versionId, "INVALID"),
                        checkProbe("ck_catalog_benefit_quantity",
                                "INSERT INTO catalog_order.catalog_benefit_line "
                                        + "(id, catalog_version_id, entitlement_type, quantity, "
                                        + "validity_days, grant_interval_months) "
                                        + "VALUES (?, ?, 'AI_SECONDS', 0, 30, 1)",
                                UUID.randomUUID(), versionId),
                        checkProbe("ck_catalog_benefit_validity",
                                "INSERT INTO catalog_order.catalog_benefit_line "
                                        + "(id, catalog_version_id, entitlement_type, quantity, "
                                        + "validity_days, grant_interval_months) "
                                        + "VALUES (?, ?, 'EVIDENCE_COUNT', 1, 0, 1)",
                                UUID.randomUUID(), versionId),
                        checkProbe("ck_catalog_benefit_interval",
                                "INSERT INTO catalog_order.catalog_benefit_line "
                                        + "(id, catalog_version_id, entitlement_type, quantity, "
                                        + "validity_days, grant_interval_months) "
                                        + "VALUES (?, ?, 'STORAGE_BYTES', 1, 30, -1)",
                                UUID.randomUUID(), versionId));
                List<String> probed = new ArrayList<>();
                for (CheckProbe probe : probes) {
                    if ("ck_catalog_version_offer_type".equals(probe.constraint())) {
                        expectCatalogVersionOfferTypeViolationWithIsolatedConstraint(
                                connection, probe);
                    } else {
                        expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                                probe.parameters());
                    }
                    probed.add(probe.constraint());
                }
                assertThat(probed)
                        .as("all 22 product/version/offer/benefit named checks have a probe")
                        .containsExactlyInAnyOrderElementsOf(v004ExpectedCheckNames(
                                "catalog_product", "catalog_version", "catalog_offer",
                                "catalog_benefit_line"));
                assertThat(probed).hasSize(22);

                // All fixtures rolled back; no committed row.
                connection.rollback();
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.catalog_product WHERE id = ?", productId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.catalog_version WHERE id IN (?, ?)",
                    versionId, secondVersionId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.catalog_offer WHERE id = ?", offerId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.catalog_benefit_line WHERE id = ?",
                    benefitId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM operations_iam.employee_account WHERE id = ?",
                    employeeId)).isZero();
        }
    }

    private static String versionProbeSql() {
        return "INSERT INTO catalog_order.catalog_version "
                + "(id, product_id, version_no, sale_status, offer_type, audience, price_cents, "
                + "currency, billing_cycle, installments, grant_interval_months, snapshot, "
                + "snapshot_sha256, created_by_employee_id, published_at, revision) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)";
    }

    private static void assertV004OrderPaymentLifecycleAndAppendOnlyBackstops() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentOrderId = UUID.randomUUID();
        UUID closedOrderId = UUID.randomUUID();
        UUID closedPaymentOrderOrderId = UUID.randomUUID();
        UUID closedPaymentOrderId = UUID.randomUUID();
        UUID verifiedEventId = UUID.randomUUID();
        UUID unverifiedEventId = UUID.randomUUID();
        String orderNo = "S2ORD" + UUID.randomUUID().toString().replace("-", "");
        String idempotencyKey = "s2-idem-" + UUID.randomUUID();
        String fingerprint = randomSha256();
        String orderSnapshotSha = randomSha256();
        String merchantNo = "S2MER" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        // Anchor fixture timestamps to the database clock so now() based
        // paid_at/closed_at/updated_at probes compare against the same clock
        // even when the host JVM and the container clock drift slightly.
        OffsetDateTime createdAt;
        try (Connection connection = openConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT now()")) {
            assertThat(resultSet.next()).isTrue();
            createdAt = resultSet.getObject(1, OffsetDateTime.class);
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                assertThat(insertEmployee(connection, employeeId, randomSha256(), "s2***pay",
                        "onecall-ops", randomSha256(), "PA4 S2 Payment Employee", "ADMIN"))
                        .isEqualTo(1);
                switchLocalRole(connection, "customer_iam_role");
                assertThat(insertCustomerAccount(connection, accountId, "PA4 S2 Order Customer"))
                        .isEqualTo(1);
                switchLocalRole(connection, "organization_access_role");
                assertThat(insertOrganization(connection, organizationId, "PA4 S2 Order Org"))
                        .isEqualTo(1);
                switchLocalRole(connection, "subject_number_role");
                assertThat(insertSubject(connection, subjectId, "IVR")).isEqualTo(1);
                switchLocalRole(connection, "catalog_order_role");
                assertThat(insertCatalogProduct(connection, productId, randomProductCode()))
                        .isEqualTo(1);
                assertThat(insertCatalogVersion(connection, versionId, productId, 1, "DRAFT",
                        "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                        "{\"plan\":\"pa4-s2-order\"}", randomSha256(), employeeId, null, 1))
                        .isEqualTo(1);
                assertThat(insertCatalogOffer(connection, offerId, versionId, "WEB", "s2-order-token"))
                        .isEqualTo(1);

                // Valid order insert with a random purchase_consent_id (no parent) succeeds.
                assertThat(insertCustomerOrder(connection, orderId, orderNo, accountId,
                        organizationId, subjectId, versionId, offerId, UUID.randomUUID(), 9900L,
                        "CNY", "UNPAID", "{\"line\":1}", orderSnapshotSha, "s2-order-token",
                        idempotencyKey, fingerprint, createdAt, null, null, 1)).isEqualTo(1);

                // purchase_consent_id=NULL fails NOT NULL.
                expectSqlFailure(connection, "23502", "purchase_consent_id",
                        "INSERT INTO catalog_order.customer_order "
                                + "(id, order_no, account_id, owner_organization_id, subject_id, "
                                + "channel, catalog_version_id, catalog_offer_id, "
                                + "purchase_consent_id, amount_cents, currency, payment_status, "
                                + "snapshot, snapshot_sha256, version_token, idempotency_key, "
                                + "request_fingerprint, revision) "
                                + "VALUES (?, ?, ?, ?, ?, 'WEB', ?, ?, NULL, 9900, 'CNY', 'UNPAID', "
                                + "'{\"line\":1}', ?, 's2-order-token', ?, ?, 1)",
                        UUID.randomUUID(), "S2ORDNULL" + UUID.randomUUID().toString()
                                .replace("-", "").substring(0, 8),
                        accountId, organizationId, subjectId, versionId, offerId,
                        orderSnapshotSha, "s2-idem-null-" + UUID.randomUUID(), fingerprint);

                // Order immutable-column update raises the snapshot guard (owner path).
                switchLocalRole(connection, "catalog_order_owner");
                expectSqlFailure(connection, "P0001", "CUSTOMER_ORDER_SNAPSHOT_IMMUTABLE",
                        "UPDATE catalog_order.customer_order SET order_no = 'tampered' WHERE id = ?",
                        orderId);
                expectSqlFailure(connection, "P0001", "CUSTOMER_ORDER_SNAPSHOT_IMMUTABLE",
                        "UPDATE catalog_order.customer_order SET amount_cents = 1 WHERE id = ?",
                        orderId);
                expectSqlFailure(connection, "P0001", "CUSTOMER_ORDER_SNAPSHOT_IMMUTABLE",
                        "UPDATE catalog_order.customer_order SET purchase_consent_id = ? WHERE id = ?",
                        UUID.randomUUID(), orderId);

                // Legal UNPAID -> PAID with conditional revision; the PAID fixture
                // stays terminal (docs/14 7.3). The database owns only shapes, so
                // these are synthetic DB backstop probes and never prove
                // service-owned PAID -> CLOSED.
                switchLocalRole(connection, "catalog_order_role");
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.customer_order "
                                + "SET payment_status = 'PAID', paid_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND payment_status = 'UNPAID' AND revision = 1",
                        orderId)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT payment_status || '|' || (paid_at IS NULL)::text || '|' "
                                + "|| (closed_at IS NOT NULL)::text || '|' || revision::text "
                                + "FROM catalog_order.customer_order WHERE id = ?",
                        orderId)).isEqualTo("PAID|false|false|2");
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.customer_order "
                                + "SET payment_status = 'PAID', paid_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND payment_status = 'UNPAID' AND revision = 1",
                        orderId))
                        .as("stale order revision update affects zero rows").isZero();

                // The CLOSED shape is reached only through an independent legal
                // UNPAID -> CLOSED fixture; the PAID fixture is never closed.
                String closedOrderNo = "S2ORDCLS"
                        + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                assertThat(insertCustomerOrder(connection, closedOrderId, closedOrderNo,
                        accountId, organizationId, subjectId, versionId, offerId,
                        UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"line\":2}",
                        orderSnapshotSha, "s2-order-token",
                        "s2-idem-cls-" + UUID.randomUUID(), randomSha256(), createdAt,
                        null, null, 1)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.customer_order "
                                + "SET payment_status = 'CLOSED', closed_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND payment_status = 'UNPAID' AND revision = 1",
                        closedOrderId)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT payment_status || '|' || (paid_at IS NULL)::text || '|' "
                                + "|| (closed_at IS NOT NULL)::text || '|' || revision::text "
                                + "FROM catalog_order.customer_order WHERE id = ?",
                        closedOrderId)).isEqualTo("CLOSED|true|true|2");
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.customer_order "
                                + "SET payment_status = 'CLOSED', closed_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND payment_status = 'CLOSED' AND revision = 1",
                        closedOrderId))
                        .as("stale closed-order revision update affects zero rows").isZero();

                // Synthetic invalid-shape probe (expected failure, not a lifecycle):
                // marking the CLOSED fixture PAID without paid_at must fail the exact
                // shape check instead of succeeding as a zero-row no-op.
                expectSqlFailure(connection, "23514", "ck_customer_order_payment_shape",
                        "UPDATE catalog_order.customer_order "
                                + "SET payment_status = 'PAID', paid_at = NULL, "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND payment_status = 'CLOSED' AND revision = 2",
                        closedOrderId);

                // Payment-order insert and legal UNPAID -> PAID transition with a
                // provider transaction id; the PAID fixture stays terminal
                // (docs/14 7.3; synthetic DB backstop probe only).
                assertThat(insertPaymentOrder(connection, paymentOrderId, orderId, merchantNo,
                        null, "UNPAID", 9900L, "CNY", createdAt, createdAt, 1)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.payment_order "
                                + "SET status = 'PAID', provider_transaction_id = ?, "
                                + "updated_at = now(), revision = revision + 1 "
                                + "WHERE id = ? AND status = 'UNPAID' AND revision = 1",
                        "txn-s2-" + UUID.randomUUID(), paymentOrderId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.payment_order "
                                + "SET status = 'PAID', updated_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND status = 'PAID' AND revision = 1",
                        paymentOrderId))
                        .as("stale payment_order revision update affects zero rows").isZero();

                // CLOSED shape comes from an independent legal UNPAID -> CLOSED
                // fixture on its own order row (uq_payment_order_order); the PAID
                // fixture is never closed.
                assertThat(insertCustomerOrder(connection, closedPaymentOrderOrderId,
                        "S2ORDPCL" + UUID.randomUUID().toString().replace("-", "")
                                .substring(0, 8),
                        accountId, organizationId, subjectId, versionId, offerId,
                        UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"line\":3}",
                        orderSnapshotSha, "s2-order-token",
                        "s2-idem-pcls-" + UUID.randomUUID(), randomSha256(), createdAt,
                        null, null, 1)).isEqualTo(1);
                assertThat(insertPaymentOrder(connection, closedPaymentOrderId,
                        closedPaymentOrderOrderId,
                        "S2MERCLS" + UUID.randomUUID().toString().replace("-", "")
                                .substring(0, 16),
                        null, "UNPAID", 9900L, "CNY", createdAt, createdAt, 1)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.payment_order "
                                + "SET status = 'CLOSED', updated_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND status = 'UNPAID' AND revision = 1",
                        closedPaymentOrderId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.payment_order "
                                + "SET status = 'CLOSED', updated_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND status = 'CLOSED' AND revision = 1",
                        closedPaymentOrderId))
                        .as("stale closed payment_order revision update affects zero rows")
                        .isZero();

                // Payment-order identity guard rejects all six identity columns (owner path).
                switchLocalRole(connection, "catalog_order_owner");
                expectSqlFailure(connection, "P0001", "PAYMENT_ORDER_IDENTITY_IMMUTABLE",
                        "UPDATE catalog_order.payment_order SET order_id = ? WHERE id = ?",
                        UUID.randomUUID(), paymentOrderId);
                expectSqlFailure(connection, "P0001", "PAYMENT_ORDER_IDENTITY_IMMUTABLE",
                        "UPDATE catalog_order.payment_order SET provider = 'ALIPAY' WHERE id = ?",
                        paymentOrderId);
                expectSqlFailure(connection, "P0001", "PAYMENT_ORDER_IDENTITY_IMMUTABLE",
                        "UPDATE catalog_order.payment_order SET merchant_order_no = 'tampered' "
                                + "WHERE id = ?",
                        paymentOrderId);
                expectSqlFailure(connection, "P0001", "PAYMENT_ORDER_IDENTITY_IMMUTABLE",
                        "UPDATE catalog_order.payment_order SET amount_cents = 1 WHERE id = ?",
                        paymentOrderId);
                expectSqlFailure(connection, "P0001", "PAYMENT_ORDER_IDENTITY_IMMUTABLE",
                        "UPDATE catalog_order.payment_order SET currency = 'USD' WHERE id = ?",
                        paymentOrderId);
                expectSqlFailure(connection, "P0001", "PAYMENT_ORDER_IDENTITY_IMMUTABLE",
                        "UPDATE catalog_order.payment_order SET created_at = now() WHERE id = ?",
                        paymentOrderId);
                switchLocalRole(connection, "catalog_order_role");

                // Synthetic invalid-shape probe (expected failure, not a lifecycle):
                // marking the CLOSED fixture PAID without provider_transaction_id
                // must fail the exact paid-shape check instead of a zero-row no-op.
                expectSqlFailure(connection, "23514", "ck_payment_order_paid_shape",
                        "UPDATE catalog_order.payment_order "
                                + "SET status = 'PAID', provider_transaction_id = NULL, "
                                + "updated_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND status = 'CLOSED' AND revision = 2",
                        closedPaymentOrderId);

                // Payment events: verified/unverified retained; duplicate identity rejected.
                assertThat(insertPaymentEvent(connection, verifiedEventId, paymentOrderId,
                        "evt-s2-verified-" + UUID.randomUUID(), "TRANSACTION.SUCCESS", true,
                        "v1.s2.event.verified", randomSha256(), "txn-s2-verified", 9900L,
                        createdAt)).isEqualTo(1);
                assertThat(insertPaymentEvent(connection, unverifiedEventId, paymentOrderId,
                        "evt-s2-unverified-" + UUID.randomUUID(), "TRANSACTION.FAILED", false,
                        "v1.s2.event.unverified", randomSha256(), null, 9900L, createdAt))
                        .isEqualTo(1);
                String duplicateEventId = queryString(connection,
                        "SELECT provider_event_id FROM catalog_order.payment_event WHERE id = ?",
                        verifiedEventId);
                expectSqlFailure(connection, "23505", "uq_payment_event_provider_event",
                        "INSERT INTO catalog_order.payment_event "
                                + "(id, payment_order_id, provider, provider_event_id, event_type, "
                                + "signature_verified, payload_ciphertext, payload_sha256, "
                                + "transaction_id, amount_cents, occurred_at) "
                                + "VALUES (?, ?, 'WECHAT', ?, 'TRANSACTION.SUCCESS', true, "
                                + "'v1.s2.event.dup', ?, NULL, 9900, ?)",
                        UUID.randomUUID(), paymentOrderId, duplicateEventId, randomSha256(),
                        createdAt);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM catalog_order.payment_event "
                                + "WHERE payment_order_id = ?",
                        paymentOrderId)).isEqualTo(2);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM catalog_order.payment_event "
                                + "WHERE payment_order_id = ? AND signature_verified",
                        paymentOrderId)).isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM catalog_order.payment_event "
                                + "WHERE payment_order_id = ? AND NOT signature_verified",
                        paymentOrderId)).isEqualTo(1);

                // Event append-only: runtime 42501, owner P0001.
                expectSqlFailure(connection, "42501", "permission denied",
                        "UPDATE catalog_order.payment_event SET event_type = 'TAMPERED' WHERE id = ?",
                        verifiedEventId);
                expectSqlFailure(connection, "42501", "permission denied",
                        "DELETE FROM catalog_order.payment_event WHERE id = ?", verifiedEventId);
                switchLocalRole(connection, "catalog_order_owner");
                expectSqlFailure(connection, "P0001", "CATALOG_ROW_APPEND_ONLY",
                        "UPDATE catalog_order.payment_event SET event_type = 'TAMPERED' WHERE id = ?",
                        verifiedEventId);
                expectSqlFailure(connection, "P0001", "CATALOG_ROW_APPEND_ONLY",
                        "DELETE FROM catalog_order.payment_event WHERE id = ?", verifiedEventId);
                switchLocalRole(connection, "catalog_order_role");

                // One 23514+exact-name probe for each of the 29
                // customer_order/payment_order/payment_event checks (plan 8.4).
                List<CheckProbe> probes = List.of(
                        checkProbe("ck_customer_order_no",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "   ", accountId, organizationId, subjectId, "WEB", versionId,
                                offerId, UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"line\":1}",
                                orderSnapshotSha, "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 1, createdAt, null, null),
                        checkProbe("ck_customer_order_channel",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDCH" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "IVR", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"line\":1}",
                                orderSnapshotSha, "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 1, createdAt, null, null),
                        checkProbe("ck_customer_order_amount",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDAMT" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), -1L, "CNY", "UNPAID", "{\"line\":1}",
                                orderSnapshotSha, "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 1, createdAt, null, null),
                        checkProbe("ck_customer_order_currency",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDCUR" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CN", "UNPAID", "{\"line\":1}",
                                orderSnapshotSha, "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 1, createdAt, null, null),
                        checkProbe("ck_customer_order_payment_status",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDSTS" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CNY", "REFUNDED", "{\"line\":1}",
                                orderSnapshotSha, "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 1, createdAt, null, null),
                        checkProbe("ck_customer_order_payment_shape",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDSHP" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CNY", "PAID", "{\"line\":1}",
                                orderSnapshotSha, "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 1, createdAt, null, null),
                        checkProbe("ck_customer_order_time_order",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDTIM" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CNY", "PAID", "{\"line\":1}",
                                orderSnapshotSha, "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 1,
                                OffsetDateTime.parse("2026-01-02T00:00:00Z"),
                                OffsetDateTime.parse("2026-01-01T00:00:00Z"), null),
                        checkProbe("ck_customer_order_snapshot_type",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDSNT" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CNY", "UNPAID", "[]",
                                orderSnapshotSha, "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 1, createdAt, null, null),
                        checkProbe("ck_customer_order_snapshot_sha",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDSHS" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"line\":1}",
                                "abc", "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 1, createdAt, null, null),
                        checkProbe("ck_customer_order_version_token",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDVTK" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"line\":1}",
                                orderSnapshotSha, "   ", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 1, createdAt, null, null),
                        checkProbe("ck_customer_order_idempotency",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDIDM" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"line\":1}",
                                orderSnapshotSha, "s2-token", "x".repeat(476),
                                fingerprint, 1, createdAt, null, null),
                        checkProbe("ck_customer_order_fingerprint",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDFPR" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"line\":1}",
                                orderSnapshotSha, "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                "abc", 1, createdAt, null, null),
                        checkProbe("ck_customer_order_revision",
                                customerOrderProbeSql(), UUID.randomUUID(),
                                "S2ORDREV" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                accountId, organizationId, subjectId, "WEB", versionId, offerId,
                                UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"line\":1}",
                                orderSnapshotSha, "s2-token", "s2-idem-probe-" + UUID.randomUUID(),
                                fingerprint, 0, createdAt, null, null),
                        checkProbe("ck_payment_order_provider",
                                paymentOrderProbeSql(), UUID.randomUUID(), orderId, "ALIPAY",
                                "S2POPROV" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                null, "UNPAID", 9900L, "CNY", createdAt, createdAt, 1),
                        checkProbe("ck_payment_order_merchant_no",
                                paymentOrderProbeSql(), UUID.randomUUID(), orderId, "WECHAT",
                                "   ", null, "UNPAID", 9900L, "CNY", createdAt, createdAt, 1),
                        checkProbe("ck_payment_order_transaction",
                                paymentOrderProbeSql(), UUID.randomUUID(), orderId, "WECHAT",
                                "S2POTRX" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                "   ", "UNPAID", 9900L, "CNY", createdAt, createdAt, 1),
                        checkProbe("ck_payment_order_status",
                                paymentOrderProbeSql(), UUID.randomUUID(), orderId, "WECHAT",
                                "S2POSTS" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                null, "REFUNDED", 9900L, "CNY", createdAt, createdAt, 1),
                        checkProbe("ck_payment_order_paid_shape",
                                paymentOrderProbeSql(), UUID.randomUUID(), orderId, "WECHAT",
                                "S2POSHP" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                null, "PAID", 9900L, "CNY", createdAt, createdAt, 1),
                        checkProbe("ck_payment_order_amount",
                                paymentOrderProbeSql(), UUID.randomUUID(), orderId, "WECHAT",
                                "S2POAMT" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                null, "UNPAID", -1L, "CNY", createdAt, createdAt, 1),
                        checkProbe("ck_payment_order_currency",
                                paymentOrderProbeSql(), UUID.randomUUID(), orderId, "WECHAT",
                                "S2POCUR" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                null, "UNPAID", 9900L, "CN", createdAt, createdAt, 1),
                        checkProbe("ck_payment_order_time_order",
                                paymentOrderProbeSql(), UUID.randomUUID(), orderId, "WECHAT",
                                "S2POTIM" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                null, "UNPAID", 9900L, "CNY",
                                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                                OffsetDateTime.parse("2025-12-31T23:59:59Z"), 1),
                        checkProbe("ck_payment_order_revision",
                                paymentOrderProbeSql(), UUID.randomUUID(), orderId, "WECHAT",
                                "S2POREV" + UUID.randomUUID().toString().replace("-", "")
                                        .substring(0, 8),
                                null, "UNPAID", 9900L, "CNY", createdAt, createdAt, 0),
                        checkProbe("ck_payment_event_provider",
                                paymentEventProbeSql(), UUID.randomUUID(), paymentOrderId,
                                "ALIPAY", "evt-s2-probe-" + UUID.randomUUID(),
                                "TRANSACTION.SUCCESS", "v1.s2.event.probe", randomSha256(), null,
                                9900L, createdAt),
                        checkProbe("ck_payment_event_event_id",
                                paymentEventProbeSql(), UUID.randomUUID(), paymentOrderId,
                                "WECHAT", "   ", "TRANSACTION.SUCCESS", "v1.s2.event.probe",
                                randomSha256(), null, 9900L, createdAt),
                        checkProbe("ck_payment_event_type",
                                paymentEventProbeSql(), UUID.randomUUID(), paymentOrderId,
                                "WECHAT", "evt-s2-probe-" + UUID.randomUUID(), "   ",
                                "v1.s2.event.probe", randomSha256(), null, 9900L, createdAt),
                        checkProbe("ck_payment_event_payload_ciphertext",
                                paymentEventProbeSql(), UUID.randomUUID(), paymentOrderId,
                                "WECHAT", "evt-s2-probe-" + UUID.randomUUID(),
                                "TRANSACTION.SUCCESS", "plaintext", randomSha256(), null, 9900L,
                                createdAt),
                        checkProbe("ck_payment_event_payload_sha",
                                paymentEventProbeSql(), UUID.randomUUID(), paymentOrderId,
                                "WECHAT", "evt-s2-probe-" + UUID.randomUUID(),
                                "TRANSACTION.SUCCESS", "v1.s2.event.probe", "abc", null, 9900L,
                                createdAt),
                        checkProbe("ck_payment_event_transaction",
                                paymentEventProbeSql(), UUID.randomUUID(), paymentOrderId,
                                "WECHAT", "evt-s2-probe-" + UUID.randomUUID(),
                                "TRANSACTION.SUCCESS", "v1.s2.event.probe", randomSha256(), "   ",
                                9900L, createdAt),
                        checkProbe("ck_payment_event_amount",
                                paymentEventProbeSql(), UUID.randomUUID(), paymentOrderId,
                                "WECHAT", "evt-s2-probe-" + UUID.randomUUID(),
                                "TRANSACTION.SUCCESS", "v1.s2.event.probe", randomSha256(), null,
                                -1L, createdAt));
                List<String> probed = new ArrayList<>();
                for (CheckProbe probe : probes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                            probe.parameters());
                    probed.add(probe.constraint());
                }
                assertThat(probed)
                        .as("all 29 customer_order/payment_order/payment_event named checks "
                                + "have a probe")
                        .containsExactlyInAnyOrderElementsOf(v004ExpectedCheckNames(
                                "customer_order", "payment_order", "payment_event"));
                assertThat(probed).hasSize(29);

                // All fixtures rolled back; no committed append-only row.
                connection.rollback();
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.customer_order WHERE id = ? OR order_no = ?",
                    orderId, orderNo)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.customer_order WHERE id IN (?, ?)",
                    closedOrderId, closedPaymentOrderOrderId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.payment_order WHERE id IN (?, ?)",
                    paymentOrderId, closedPaymentOrderId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.payment_event WHERE id IN (?, ?)",
                    verifiedEventId, unverifiedEventId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?",
                    accountId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?",
                    organizationId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.business_subject WHERE id = ?",
                    subjectId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM operations_iam.employee_account WHERE id = ?",
                    employeeId)).isZero();
        }
    }

    private static String customerOrderProbeSql() {
        return "INSERT INTO catalog_order.customer_order "
                + "(id, order_no, account_id, owner_organization_id, subject_id, channel, "
                + "catalog_version_id, catalog_offer_id, purchase_consent_id, amount_cents, "
                + "currency, payment_status, snapshot, snapshot_sha256, version_token, "
                + "idempotency_key, request_fingerprint, revision, created_at, paid_at, closed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private static String paymentOrderProbeSql() {
        return "INSERT INTO catalog_order.payment_order "
                + "(id, order_id, provider, merchant_order_no, provider_transaction_id, status, "
                + "amount_cents, currency, created_at, updated_at, revision) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private static String paymentEventProbeSql() {
        return "INSERT INTO catalog_order.payment_event "
                + "(id, payment_order_id, provider, provider_event_id, event_type, "
                + "signature_verified, payload_ciphertext, payload_sha256, transaction_id, "
                + "amount_cents, occurred_at) "
                + "VALUES (?, ?, ?, ?, ?, false, ?, ?, ?, ?, ?)";
    }

    private static void assertV004ForeignKeyAndAbsentConsentFkBackstops() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID versionWithOffer = UUID.randomUUID();
        UUID versionWithBenefit = UUID.randomUUID();
        UUID versionForEmployeeDelete = UUID.randomUUID();
        UUID versionForOrder = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID benefitId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentOrderId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                assertThat(insertEmployee(connection, employeeId, randomSha256(), "s2***fk",
                        "onecall-ops", randomSha256(), "PA4 S2 FK Employee", "ADMIN"))
                        .isEqualTo(1);
                switchLocalRole(connection, "customer_iam_role");
                assertThat(insertCustomerAccount(connection, accountId, "PA4 S2 FK Customer"))
                        .isEqualTo(1);
                switchLocalRole(connection, "organization_access_role");
                assertThat(insertOrganization(connection, organizationId, "PA4 S2 FK Org"))
                        .isEqualTo(1);
                switchLocalRole(connection, "subject_number_role");
                assertThat(insertSubject(connection, subjectId, "IVR")).isEqualTo(1);
                switchLocalRole(connection, "catalog_order_role");
                assertThat(insertCatalogProduct(connection, productId, randomProductCode()))
                        .isEqualTo(1);
                assertThat(insertCatalogVersion(connection, versionWithOffer, productId, 1,
                        "DRAFT", "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                        "{\"p\":1}", randomSha256(), employeeId, null, 1)).isEqualTo(1);
                assertThat(insertCatalogVersion(connection, versionWithBenefit, productId, 2,
                        "DRAFT", "ONE_TIME", "PERSONAL", 5000L, "CNY", "ONCE", 1, 0,
                        "{\"p\":2}", randomSha256(), employeeId, null, 1)).isEqualTo(1);
                assertThat(insertCatalogVersion(connection, versionForEmployeeDelete, productId, 3,
                        "DRAFT", "ADD_ON", "PERSONAL", 100L, "CNY", "ONCE", 1, 0,
                        "{\"p\":3}", randomSha256(), employeeId, null, 1)).isEqualTo(1);
                assertThat(insertCatalogVersion(connection, versionForOrder, productId, 4, "DRAFT",
                        "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                        "{\"p\":4}", randomSha256(), employeeId, null, 1)).isEqualTo(1);
                assertThat(insertCatalogOffer(connection, offerId, versionWithOffer, "WEB",
                        "s2-fk-token")).isEqualTo(1);
                assertThat(insertCatalogBenefitLine(connection, benefitId, versionWithBenefit,
                        "AI_SECONDS", 10, 7, 0)).isEqualTo(1);
                assertThat(insertCustomerOrder(connection, orderId,
                        "S2ORDFK" + UUID.randomUUID().toString().replace("-", ""),
                        accountId, organizationId, subjectId, versionForOrder, offerId,
                        UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"line\":1}",
                        randomSha256(), "s2-fk-token", "s2-idem-fk-" + UUID.randomUUID(),
                        randomSha256(), createdAt, null, null, 1)).isEqualTo(1);
                assertThat(insertPaymentOrder(connection, paymentOrderId, orderId,
                        "S2MERFK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                        null, "UNPAID", 9900L, "CNY", createdAt, createdAt, 1)).isEqualTo(1);

                // Missing-parent 23503 + exact constraint name for all 11 FKs.
                expectSqlFailure(connection, "23503", "fk_catalog_version_product",
                        "INSERT INTO catalog_order.catalog_version "
                                + "(id, product_id, version_no, sale_status, offer_type, audience, "
                                + "price_cents, currency, billing_cycle, installments, "
                                + "grant_interval_months, snapshot, snapshot_sha256, "
                                + "created_by_employee_id, revision) "
                                + "VALUES (?, ?, 99, 'DRAFT', 'SUBSCRIPTION', 'PERSONAL', 100, "
                                + "'CNY', 'MONTHLY', 1, 1, '{\"p\":9}', ?, ?, 1)",
                        UUID.randomUUID(), UUID.randomUUID(), randomSha256(), employeeId);
                expectSqlFailure(connection, "23503", "fk_catalog_version_employee",
                        "INSERT INTO catalog_order.catalog_version "
                                + "(id, product_id, version_no, sale_status, offer_type, audience, "
                                + "price_cents, currency, billing_cycle, installments, "
                                + "grant_interval_months, snapshot, snapshot_sha256, "
                                + "created_by_employee_id, revision) "
                                + "VALUES (?, ?, 99, 'DRAFT', 'SUBSCRIPTION', 'PERSONAL', 100, "
                                + "'CNY', 'MONTHLY', 1, 1, '{\"p\":9}', ?, ?, 1)",
                        UUID.randomUUID(), productId, randomSha256(), UUID.randomUUID());
                expectSqlFailure(connection, "23503", "fk_catalog_offer_version",
                        "INSERT INTO catalog_order.catalog_offer "
                                + "(id, catalog_version_id, channel, offer_status, version_token, "
                                + "revision) VALUES (?, ?, 'MINIAPP', 'ACTIVE', 's2-fk', 1)",
                        UUID.randomUUID(), UUID.randomUUID());
                expectSqlFailure(connection, "23503", "fk_catalog_benefit_version",
                        "INSERT INTO catalog_order.catalog_benefit_line "
                                + "(id, catalog_version_id, entitlement_type, quantity, "
                                + "validity_days, grant_interval_months) "
                                + "VALUES (?, ?, 'STORAGE_BYTES', 1, 1, 0)",
                        UUID.randomUUID(), UUID.randomUUID());
                expectSqlFailure(connection, "23503", "fk_customer_order_account",
                        customerOrderMissingParentSql("fk_customer_order_account"),
                        UUID.randomUUID(),
                        "S2ORDPACC" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                        UUID.randomUUID(), organizationId, subjectId, versionForOrder, offerId,
                        UUID.randomUUID(), randomSha256(),
                        "s2-idem-pacc-" + UUID.randomUUID(), randomSha256());
                expectSqlFailure(connection, "23503", "fk_customer_order_organization",
                        customerOrderMissingParentSql("fk_customer_order_organization"),
                        UUID.randomUUID(),
                        "S2ORDPORG" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                        accountId, UUID.randomUUID(), subjectId, versionForOrder, offerId,
                        UUID.randomUUID(), randomSha256(),
                        "s2-idem-porg-" + UUID.randomUUID(), randomSha256());
                expectSqlFailure(connection, "23503", "fk_customer_order_subject",
                        customerOrderMissingParentSql("fk_customer_order_subject"),
                        UUID.randomUUID(),
                        "S2ORDPSUB" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                        accountId, organizationId, UUID.randomUUID(), versionForOrder, offerId,
                        UUID.randomUUID(), randomSha256(),
                        "s2-idem-psub-" + UUID.randomUUID(), randomSha256());
                expectSqlFailure(connection, "23503", "fk_customer_order_version",
                        customerOrderMissingParentSql("fk_customer_order_version"),
                        UUID.randomUUID(),
                        "S2ORDPVER" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                        accountId, organizationId, subjectId, UUID.randomUUID(), offerId,
                        UUID.randomUUID(), randomSha256(),
                        "s2-idem-pver-" + UUID.randomUUID(), randomSha256());
                expectSqlFailure(connection, "23503", "fk_customer_order_offer",
                        customerOrderMissingParentSql("fk_customer_order_offer"),
                        UUID.randomUUID(),
                        "S2ORDPOFF" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                        accountId, organizationId, subjectId, versionForOrder, UUID.randomUUID(),
                        UUID.randomUUID(), randomSha256(),
                        "s2-idem-poff-" + UUID.randomUUID(), randomSha256());
                expectSqlFailure(connection, "23503", "fk_payment_order_order",
                        "INSERT INTO catalog_order.payment_order "
                                + "(id, order_id, provider, merchant_order_no, status, "
                                + "amount_cents, currency, revision) "
                                + "VALUES (?, ?, 'WECHAT', ?, 'UNPAID', 100, 'CNY', 1)",
                        UUID.randomUUID(), UUID.randomUUID(),
                        "S2MERMISS" + UUID.randomUUID().toString().replace("-", "")
                                .substring(0, 8));
                expectSqlFailure(connection, "23503", "fk_payment_event_order",
                        "INSERT INTO catalog_order.payment_event "
                                + "(id, payment_order_id, provider, provider_event_id, event_type, "
                                + "signature_verified, payload_ciphertext, payload_sha256, "
                                + "amount_cents, occurred_at) "
                                + "VALUES (?, ?, 'WECHAT', ?, 'TRANSACTION.SUCCESS', false, "
                                + "'v1.s2.event.missing', ?, 100, ?)",
                        UUID.randomUUID(), UUID.randomUUID(),
                        "evt-s2-missing-" + UUID.randomUUID(), randomSha256(), createdAt);

                // Parent-delete RESTRICT 23503 where the parent has no append-only trigger.
                resetToMigrationIdentity(connection);
                // One valid payment-event child so the payment_order parent delete below
                // hits fk_payment_event_order RESTRICT (the missing-parent probe above
                // used a random parent id and left no child row).
                assertThat(insertPaymentEvent(connection, UUID.randomUUID(), paymentOrderId,
                        "evt-s2-fkparent-" + UUID.randomUUID(), "TRANSACTION.SUCCESS", false,
                        "v1.s2.event.fkparent", randomSha256(), null, 9900L, createdAt))
                        .isEqualTo(1);
                expectSqlFailure(connection, "23503", "fk_catalog_offer_version",
                        "DELETE FROM catalog_order.catalog_version WHERE id = ?",
                        versionWithOffer);
                expectSqlFailure(connection, "23503", "fk_catalog_benefit_version",
                        "DELETE FROM catalog_order.catalog_version WHERE id = ?",
                        versionWithBenefit);
                expectSqlFailure(connection, "23503", "fk_payment_order_order",
                        "DELETE FROM catalog_order.customer_order WHERE id = ?", orderId);
                expectSqlFailure(connection, "23503", "fk_payment_event_order",
                        "DELETE FROM catalog_order.payment_order WHERE id = ?", paymentOrderId);
                expectSqlFailure(connection, "23503", "fk_customer_order_account",
                        "DELETE FROM customer_iam.customer_account WHERE id = ?", accountId);
                expectSqlFailure(connection, "23503", "fk_customer_order_organization",
                        "DELETE FROM organization_access.organization WHERE id = ?",
                        organizationId);
                expectSqlFailure(connection, "23503", "fk_customer_order_subject",
                        "DELETE FROM subject_number.business_subject WHERE id = ?", subjectId);
                expectSqlFailure(connection, "23503", "fk_catalog_version_employee",
                        "DELETE FROM operations_iam.employee_account WHERE id = ?", employeeId);

                // catalog_product delete is shadowed by trg_catalog_product_immutable (P0001),
                // not a 23503; the FK metadata still records confdeltype='r'.
                expectSqlFailure(connection, "P0001", "CATALOG_ROW_APPEND_ONLY",
                        "DELETE FROM catalog_order.catalog_product WHERE id = ?", productId);
                assertThat(queryString(connection,
                        "SELECT c.confupdtype::text || c.confdeltype::text || "
                                + "c.confmatchtype::text || '|' || c.condeferrable::text || '|' "
                                + "|| c.condeferred::text || '|' || c.convalidated::text "
                                + "FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = 'catalog_order' "
                                + "AND c.conname = 'fk_catalog_version_product'"))
                        .isEqualTo("rrs|false|false|true");

                // Exact 11-row FK map; consent FK absent; purchase_consent_id properties.
                assertThat(V004_FK_DEFS).as("expected V004 FK map size").hasSize(11);
                for (String row : V004_FK_DEFS) {
                    String[] expected = row.split("\\|", -1);
                    assertThat(queryString(connection,
                            "SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c "
                                    + "JOIN pg_class r ON r.oid = c.conrelid "
                                    + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                    + "WHERE n.nspname = 'catalog_order' AND c.conname = ?",
                            expected[0])).as("FK def %s", expected[0]).isEqualTo(expected[1]);
                    assertThat(queryStrings(connection,
                            "SELECT c.confupdtype::text || c.confdeltype::text || "
                                    + "c.confmatchtype::text || '|' || c.condeferrable::text || "
                                    + "'|' || c.condeferred::text || '|' || c.convalidated::text "
                                    + "FROM pg_constraint c JOIN pg_class r ON r.oid = c.conrelid "
                                    + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                    + "WHERE n.nspname = 'catalog_order' AND c.conname = ?",
                            expected[0]))
                            .as("FK properties %s", expected[0])
                            .containsExactly("rrs|false|false|true");
                }
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM pg_constraint c "
                                + "JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = 'catalog_order' "
                                + "AND c.conname = 'fk_customer_order_consent'"))
                        .as("fk_customer_order_consent is deferred to V007").isZero();
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM information_schema.columns "
                                + "WHERE table_schema = 'catalog_order' "
                                + "AND table_name = 'customer_order' "
                                + "AND column_name = 'purchase_consent_id' "
                                + "AND is_nullable = 'NO' AND column_default IS NULL"))
                        .as("purchase_consent_id is NOT NULL with no default").isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM information_schema.key_column_usage k "
                                + "JOIN information_schema.table_constraints c "
                                + "ON k.constraint_name = c.constraint_name "
                                + "AND k.table_schema = c.table_schema "
                                + "WHERE k.table_schema = 'catalog_order' "
                                + "AND k.table_name = 'customer_order' "
                                + "AND k.column_name = 'purchase_consent_id' "
                                + "AND c.constraint_type = 'FOREIGN KEY'"))
                        .as("purchase_consent_id has no FK at V004").isZero();
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM information_schema.tables "
                                + "WHERE table_schema = 'activation_consent'"))
                        .as("activation_consent has zero tables at V004").isZero();

                // All fixtures rolled back.
                connection.rollback();
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.catalog_product WHERE id = ?",
                    productId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.catalog_version "
                            + "WHERE id IN (?, ?, ?, ?)",
                    versionWithOffer, versionWithBenefit, versionForEmployeeDelete,
                    versionForOrder)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.customer_order WHERE id = ?", orderId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?",
                    accountId)).isZero();
        }
    }

    private static String customerOrderMissingParentSql(String ignoredConstraint) {
        return "INSERT INTO catalog_order.customer_order "
                + "(id, order_no, account_id, owner_organization_id, subject_id, channel, "
                + "catalog_version_id, catalog_offer_id, purchase_consent_id, amount_cents, "
                + "currency, payment_status, snapshot, snapshot_sha256, version_token, "
                + "idempotency_key, request_fingerprint, revision) "
                + "VALUES (?, ?, ?, ?, ?, 'WEB', ?, ?, ?, 9900, 'CNY', 'UNPAID', '{\"line\":1}', "
                + "?, 's2-fk', ?, ?, 1)";
    }

    private static void runConcurrentCatalogPublicationAndCommerceIdentity() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            commitCustomerAccounts(accountId);
            commitOrganization(organizationId);
            commitSubjects(subjectId);
            commitEmployees(List.of(employeeId));

            runUnlockedPublicationRace(executor, employeeId);
            runAdvisoryLockPublicationProtocol(executor, employeeId);
            runOrderIdempotencyRace(executor, accountId, organizationId, subjectId, employeeId);
            runPaymentOrderIdentityRaces(executor, accountId, organizationId, subjectId,
                    employeeId);
            runPaymentEventIdentityRace(executor, accountId, organizationId, subjectId,
                    employeeId);
        } finally {
            executor.shutdownNow();
            cleanupV004ConcurrentFixtures(accountId, organizationId, subjectId, employeeId);
        }
    }

    private static void runUnlockedPublicationRace(ExecutorService executor, UUID employeeId)
            throws Exception {
        UUID productId = UUID.randomUUID();
        UUID winnerVersion = UUID.randomUUID();
        UUID loserVersion = UUID.randomUUID();
        String winnerApplication = "pa4-s2-publication-unlocked-winner";
        String loserApplication = "pa4-s2-publication-unlocked-loser";

        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "catalog_order_role");
                assertThat(insertCatalogProduct(setup, productId, randomProductCode()))
                        .isEqualTo(1);
                assertThat(insertCatalogVersion(setup, winnerVersion, productId, 1, "DRAFT",
                        "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                        "{\"race\":\"unlocked-w\"}", randomSha256(), employeeId, null, 1))
                        .isEqualTo(1);
                assertThat(insertCatalogVersion(setup, loserVersion, productId, 2, "DRAFT",
                        "ONE_TIME", "PERSONAL", 5000L, "CNY", "ONCE", 1, 0,
                        "{\"race\":\"unlocked-l\"}", randomSha256(), employeeId, null, 1))
                        .isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }

        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
            }
            setLocalRole(winner, "catalog_order_role");
            assertThat(executeUpdate(winner,
                    "UPDATE catalog_order.catalog_version "
                            + "SET sale_status = 'ACTIVE', published_at = now(), "
                            + "revision = revision + 1 "
                            + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 1",
                    winnerVersion)).isEqualTo(1);

            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "catalog_order_role", loserApplication);
            Future<Integer> loserFuture = executor.submit(() -> executeUpdate(loser,
                    "UPDATE catalog_order.catalog_version "
                            + "SET sale_status = 'ACTIVE', published_at = now(), "
                            + "revision = revision + 1 "
                            + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 1",
                    loserVersion));
            awaitBlockedBackend(loserApplication);
            winner.commit();
            try {
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                fail("the publication loser must not commit a second ACTIVE version");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23505");
                assertThat(exception.getMessage()).contains("uq_catalog_version_active");
            }
            loser.rollback();
            setLocalRole(loser, "catalog_order_role");
            assertThat(queryString(loser,
                    "SELECT id::text FROM catalog_order.catalog_version "
                            + "WHERE product_id = ? AND sale_status = 'ACTIVE'",
                    productId)).isEqualTo(winnerVersion.toString());
            loser.rollback();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM catalog_order.catalog_version "
                            + "WHERE product_id = ? AND sale_status = 'ACTIVE'",
                    productId)).isEqualTo(1);
            // The loser's UPDATE failed on uq_catalog_version_active and its transaction
            // rolled back, so the pre-existing loser row remains exactly as committed:
            // DRAFT with published_at still NULL (the query renders published_at IS NULL,
            // so the unchanged NULL timestamp reads as true) and revision 1.
            assertThat(queryString(verify,
                    "SELECT sale_status || '|' || (published_at IS NULL)::text || '|' "
                            + "|| revision::text "
                            + "FROM catalog_order.catalog_version WHERE id = ?",
                    loserVersion)).isEqualTo("DRAFT|true|1");
        }
    }

    private static void runAdvisoryLockPublicationProtocol(ExecutorService executor, UUID employeeId)
            throws Exception {
        UUID productId = UUID.randomUUID();
        UUID activeVersion = UUID.randomUUID();
        UUID candidateVersion = UUID.randomUUID();
        String lockKey = "catalog_order.publication:" + productId;
        String winnerApplication = "pa4-s2-publication-lock-winner";
        String loserApplication = "pa4-s2-publication-lock-loser";
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "catalog_order_role");
                assertThat(insertCatalogProduct(setup, productId, randomProductCode()))
                        .isEqualTo(1);
                assertThat(insertCatalogVersion(setup, activeVersion, productId, 1, "ACTIVE",
                        "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                        "{\"race\":\"protocol-active\"}", randomSha256(), employeeId,
                        createdAt, 2)).isEqualTo(1);
                assertThat(insertCatalogVersion(setup, candidateVersion, productId, 2, "DRAFT",
                        "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                        "{\"race\":\"protocol-candidate\"}", randomSha256(), employeeId, null, 1))
                        .isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }

        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
            }
            setLocalRole(winner, "catalog_order_role");
            execute(winner, "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", lockKey);
            assertThat(executeUpdate(winner,
                    "UPDATE catalog_order.catalog_version "
                            + "SET sale_status = 'OFFSALE', revision = revision + 1 "
                            + "WHERE id = ? AND sale_status = 'ACTIVE' AND revision = 2",
                    activeVersion)).isEqualTo(1);
            assertThat(executeUpdate(winner,
                    "UPDATE catalog_order.catalog_version "
                            + "SET sale_status = 'ACTIVE', published_at = now(), "
                            + "revision = revision + 1 "
                            + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 1",
                    candidateVersion)).isEqualTo(1);

            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "catalog_order_role", loserApplication);
            Future<Void> loserFuture = executor.submit(() -> {
                try {
                    execute(loser, "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                            lockKey);
                    String currentActive = queryString(loser,
                            "SELECT id::text FROM catalog_order.catalog_version "
                                    + "WHERE product_id = ? AND sale_status = 'ACTIVE'",
                            productId);
                    assertThat(currentActive)
                            .as("stale loser observes the winner's ACTIVE candidate")
                            .isEqualTo(candidateVersion.toString());
                    assertThat(executeUpdate(loser,
                            "UPDATE catalog_order.catalog_version "
                                    + "SET sale_status = 'OFFSALE', revision = revision + 1 "
                                    + "WHERE id = ? AND sale_status = 'ACTIVE' AND revision = 2",
                            activeVersion))
                            .as("stale retire of the previous ACTIVE affects zero rows")
                            .isZero();
                    assertThat(executeUpdate(loser,
                            "UPDATE catalog_order.catalog_version "
                                    + "SET sale_status = 'ACTIVE', published_at = now(), "
                                    + "revision = revision + 1 "
                                    + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 1",
                            candidateVersion))
                            .as("stale activate of the candidate affects zero rows")
                            .isZero();
                    loser.rollback();
                    return null;
                } catch (SQLException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            awaitBlockedBackend(loserApplication);
            winner.commit();
            awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
            winner.close();
            loser.close();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM catalog_order.catalog_version "
                            + "WHERE product_id = ? AND sale_status = 'ACTIVE'",
                    productId)).isEqualTo(1);
            assertThat(queryString(verify,
                    "SELECT id::text FROM catalog_order.catalog_version "
                            + "WHERE product_id = ? AND sale_status = 'ACTIVE'",
                    productId)).isEqualTo(candidateVersion.toString());
            assertThat(queryString(verify,
                    "SELECT sale_status || '|' || revision::text "
                            + "FROM catalog_order.catalog_version WHERE id = ?",
                    activeVersion)).isEqualTo("OFFSALE|3");
            assertThat(queryString(verify,
                    "SELECT sale_status || '|' || revision::text "
                            + "FROM catalog_order.catalog_version WHERE id = ?",
                    candidateVersion)).isEqualTo("ACTIVE|2");
        }
    }

    private static void runOrderIdempotencyRace(
            ExecutorService executor,
            UUID accountId,
            UUID organizationId,
            UUID subjectId,
            UUID employeeId) throws Exception {
        UUID productId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID winnerOrder = UUID.randomUUID();
        UUID loserOrder = UUID.randomUUID();
        String idempotencyKey = "s2-idem-race-" + UUID.randomUUID();
        String winnerApplication = "pa4-s2-order-idem-winner";
        String loserApplication = "pa4-s2-order-idem-loser";
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "catalog_order_role");
                assertThat(insertCatalogProduct(setup, productId, randomProductCode()))
                        .isEqualTo(1);
                assertThat(insertCatalogVersion(setup, versionId, productId, 1, "DRAFT",
                        "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                        "{\"race\":\"order\"}", randomSha256(), employeeId, null, 1))
                        .isEqualTo(1);
                assertThat(insertCatalogOffer(setup, offerId, versionId, "WEB", "s2-race-token"))
                        .isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }

        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
            }
            setLocalRole(winner, "catalog_order_role");
            assertThat(insertCustomerOrder(winner, winnerOrder,
                    "S2ORDW" + UUID.randomUUID().toString().replace("-", ""),
                    accountId, organizationId, subjectId, versionId, offerId,
                    UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"race\":1}", randomSha256(),
                    "s2-race-token", idempotencyKey, randomSha256(), createdAt, null, null, 1))
                    .isEqualTo(1);

            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "catalog_order_role", loserApplication);
            Future<Integer> loserFuture = executor.submit(() -> insertCustomerOrder(loser,
                    loserOrder,
                    "S2ORDL" + UUID.randomUUID().toString().replace("-", ""),
                    accountId, organizationId, subjectId, versionId, offerId,
                    UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"race\":1}", randomSha256(),
                    "s2-race-token", idempotencyKey, randomSha256(), createdAt, null, null, 1));
            awaitBlockedBackend(loserApplication);
            winner.commit();
            try {
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                fail("the idempotency loser must not commit a second order");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23505");
                assertThat(exception.getMessage()).contains("uq_customer_order_idempotency");
            }
            loser.rollback();
            setLocalRole(loser, "catalog_order_role");
            assertThat(queryString(loser,
                    "SELECT id::text FROM catalog_order.customer_order "
                            + "WHERE account_id = ? AND idempotency_key = ?",
                    accountId, idempotencyKey)).isEqualTo(winnerOrder.toString());
            loser.rollback();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM catalog_order.customer_order "
                            + "WHERE account_id = ? AND idempotency_key = ?",
                    accountId, idempotencyKey)).isEqualTo(1);
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM catalog_order.customer_order WHERE id = ?",
                    loserOrder)).isZero();
        }
    }

    private static void runPaymentOrderIdentityRaces(
            ExecutorService executor,
            UUID accountId,
            UUID organizationId,
            UUID subjectId,
            UUID employeeId) throws Exception {
        UUID productId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "catalog_order_role");
                assertThat(insertCatalogProduct(setup, productId, randomProductCode()))
                        .isEqualTo(1);
                assertThat(insertCatalogVersion(setup, versionId, productId, 1, "DRAFT",
                        "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                        "{\"race\":\"payment\"}", randomSha256(), employeeId, null, 1))
                        .isEqualTo(1);
                assertThat(insertCatalogOffer(setup, offerId, versionId, "WEB", "s2-pay-token"))
                        .isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }

        // Race 1: same order_id.
        UUID orderForIdRace = UUID.randomUUID();
        UUID winnerPayment = UUID.randomUUID();
        UUID loserPayment = UUID.randomUUID();
        String orderRaceWinnerApp = "pa4-s2-payment-order-winner";
        String orderRaceLoserApp = "pa4-s2-payment-order-loser";
        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "catalog_order_role");
                assertThat(insertCustomerOrder(setup, orderForIdRace,
                        "S2ORDPID" + UUID.randomUUID().toString().replace("-", ""),
                        accountId, organizationId, subjectId, versionId, offerId,
                        UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"race\":1}", randomSha256(),
                        "s2-pay-token", "s2-idem-pid-" + UUID.randomUUID(), randomSha256(),
                        createdAt, null, null, 1)).isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }
        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + orderRaceWinnerApp + "'");
            }
            setLocalRole(winner, "catalog_order_role");
            assertThat(insertPaymentOrder(winner, winnerPayment, orderForIdRace,
                    "S2MERW" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                    null, "UNPAID", 9900L, "CNY", createdAt, createdAt, 1)).isEqualTo(1);
            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "catalog_order_role", orderRaceLoserApp);
            Future<Integer> loserFuture = executor.submit(() -> insertPaymentOrder(loser,
                    loserPayment, orderForIdRace,
                    "S2MERL" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                    null, "UNPAID", 9900L, "CNY", createdAt, createdAt, 1));
            awaitBlockedBackend(orderRaceLoserApp);
            winner.commit();
            try {
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                fail("the payment_order order_id loser must not commit");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23505");
                assertThat(exception.getMessage()).contains("uq_payment_order_order");
            }
            loser.rollback();
            setLocalRole(loser, "catalog_order_role");
            assertThat(queryString(loser,
                    "SELECT id::text FROM catalog_order.payment_order WHERE order_id = ?",
                    orderForIdRace)).isEqualTo(winnerPayment.toString());
            loser.rollback();
        }

        // Race 2: same non-null provider_transaction_id on two orders.
        UUID orderTxnA = UUID.randomUUID();
        UUID orderTxnB = UUID.randomUUID();
        UUID winnerTxnPayment = UUID.randomUUID();
        UUID loserTxnPayment = UUID.randomUUID();
        String sharedTransaction = "txn-s2-race-" + UUID.randomUUID();
        String txnRaceWinnerApp = "pa4-s2-payment-txn-winner";
        String txnRaceLoserApp = "pa4-s2-payment-txn-loser";
        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "catalog_order_role");
                assertThat(insertCustomerOrder(setup, orderTxnA,
                        "S2ORDTXA" + UUID.randomUUID().toString().replace("-", ""),
                        accountId, organizationId, subjectId, versionId, offerId,
                        UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"race\":1}", randomSha256(),
                        "s2-pay-token", "s2-idem-txa-" + UUID.randomUUID(), randomSha256(),
                        createdAt, null, null, 1)).isEqualTo(1);
                assertThat(insertCustomerOrder(setup, orderTxnB,
                        "S2ORDTXB" + UUID.randomUUID().toString().replace("-", ""),
                        accountId, organizationId, subjectId, versionId, offerId,
                        UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"race\":1}", randomSha256(),
                        "s2-pay-token", "s2-idem-txb-" + UUID.randomUUID(), randomSha256(),
                        createdAt, null, null, 1)).isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }
        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + txnRaceWinnerApp + "'");
            }
            setLocalRole(winner, "catalog_order_role");
            assertThat(insertPaymentOrder(winner, winnerTxnPayment, orderTxnA,
                    "S2MERTXA" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                    sharedTransaction, "UNPAID", 9900L, "CNY", createdAt, createdAt, 1))
                    .isEqualTo(1);
            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "catalog_order_role", txnRaceLoserApp);
            Future<Integer> loserFuture = executor.submit(() -> insertPaymentOrder(loser,
                    loserTxnPayment, orderTxnB,
                    "S2MERTXB" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                    sharedTransaction, "UNPAID", 9900L, "CNY", createdAt, createdAt, 1));
            awaitBlockedBackend(txnRaceLoserApp);
            winner.commit();
            try {
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                fail("the provider_transaction_id loser must not commit");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23505");
                assertThat(exception.getMessage()).contains("uq_payment_order_transaction");
            }
            loser.rollback();
            setLocalRole(loser, "catalog_order_role");
            assertThat(queryString(loser,
                    "SELECT id::text FROM catalog_order.payment_order "
                            + "WHERE provider_transaction_id = ?",
                    sharedTransaction)).isEqualTo(winnerTxnPayment.toString());
            loser.rollback();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM catalog_order.payment_order WHERE order_id = ?",
                    orderForIdRace)).isEqualTo(1);
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM catalog_order.payment_order WHERE id IN (?, ?)",
                    winnerPayment, loserPayment)).isEqualTo(1);
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM catalog_order.payment_order "
                            + "WHERE provider_transaction_id = ?",
                    sharedTransaction)).isEqualTo(1);
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM catalog_order.payment_order WHERE id = ?",
                    loserTxnPayment)).isZero();
        }
    }

    private static void runPaymentEventIdentityRace(
            ExecutorService executor,
            UUID accountId,
            UUID organizationId,
            UUID subjectId,
            UUID employeeId) throws Exception {
        UUID productId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentOrderId = UUID.randomUUID();
        UUID winnerEvent = UUID.randomUUID();
        UUID loserEvent = UUID.randomUUID();
        String providerEventId = "evt-s2-race-" + UUID.randomUUID();
        String winnerApplication = "pa4-s2-event-identity-winner";
        String loserApplication = "pa4-s2-event-identity-loser";
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "catalog_order_role");
                assertThat(insertCatalogProduct(setup, productId, randomProductCode()))
                        .isEqualTo(1);
                assertThat(insertCatalogVersion(setup, versionId, productId, 1, "DRAFT",
                        "SUBSCRIPTION", "PERSONAL", 9900L, "CNY", "MONTHLY", 1, 1,
                        "{\"race\":\"event\"}", randomSha256(), employeeId, null, 1))
                        .isEqualTo(1);
                assertThat(insertCatalogOffer(setup, offerId, versionId, "WEB", "s2-evt-token"))
                        .isEqualTo(1);
                assertThat(insertCustomerOrder(setup, orderId,
                        "S2ORDEVT" + UUID.randomUUID().toString().replace("-", ""),
                        accountId, organizationId, subjectId, versionId, offerId,
                        UUID.randomUUID(), 9900L, "CNY", "UNPAID", "{\"race\":1}", randomSha256(),
                        "s2-evt-token", "s2-idem-evt-" + UUID.randomUUID(), randomSha256(),
                        createdAt, null, null, 1)).isEqualTo(1);
                assertThat(insertPaymentOrder(setup, paymentOrderId, orderId,
                        "S2MEREVT" + UUID.randomUUID().toString().replace("-", "").substring(0, 10),
                        null, "UNPAID", 9900L, "CNY", createdAt, createdAt, 1)).isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }

        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
            }
            setLocalRole(winner, "catalog_order_role");
            assertThat(insertPaymentEvent(winner, winnerEvent, paymentOrderId, providerEventId,
                    "TRANSACTION.SUCCESS", false, "v1.s2.event.race", randomSha256(), null,
                    9900L, createdAt)).isEqualTo(1);

            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "catalog_order_role", loserApplication);
            Future<Integer> loserFuture = executor.submit(() -> insertPaymentEvent(loser,
                    loserEvent, paymentOrderId, providerEventId, "TRANSACTION.SUCCESS", false,
                    "v1.s2.event.race", randomSha256(), null, 9900L, createdAt));
            awaitBlockedBackend(loserApplication);
            winner.commit();
            try {
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                fail("the payment_event identity loser must not commit");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23505");
                assertThat(exception.getMessage()).contains("uq_payment_event_provider_event");
            }
            loser.rollback();
            setLocalRole(loser, "catalog_order_role");
            assertThat(queryString(loser,
                    "SELECT id::text FROM catalog_order.payment_event "
                            + "WHERE provider = 'WECHAT' AND provider_event_id = ?",
                    providerEventId)).isEqualTo(winnerEvent.toString());
            loser.rollback();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM catalog_order.payment_event "
                            + "WHERE payment_order_id = ? AND provider = 'WECHAT' "
                            + "AND provider_event_id = ?",
                    paymentOrderId, providerEventId)).isEqualTo(1);
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM catalog_order.payment_event WHERE id = ?",
                    loserEvent)).isZero();
        }
    }

    private static void cleanupV004ConcurrentFixtures(
            UUID accountId, UUID organizationId, UUID subjectId, UUID employeeId)
            throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                // One TRUNCATE covering all seven catalog_order tables in a single statement:
                // the only way to remove committed append-only fixtures; test-fixture hygiene
                // only. Runtime TRUNCATE denial remains asserted by the S1 ACL matrix.
                execute(connection,
                        "TRUNCATE TABLE catalog_order.catalog_product, "
                                + "catalog_order.catalog_version, catalog_order.catalog_offer, "
                                + "catalog_order.catalog_benefit_line, "
                                + "catalog_order.customer_order, catalog_order.payment_order, "
                                + "catalog_order.payment_event");
                // FK-safe parent deletes.
                executeUpdate(connection,
                        "DELETE FROM subject_number.business_subject WHERE id = ?", subjectId);
                executeUpdate(connection,
                        "DELETE FROM organization_access.organization WHERE id = ?",
                        organizationId);
                executeUpdate(connection,
                        "DELETE FROM operations_iam.employee_account WHERE id = ?", employeeId);
                executeUpdate(connection,
                        "DELETE FROM customer_iam.customer_account WHERE id = ?", accountId);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }

        try (Connection connection = openConnection()) {
            // After the single migration-identity TRUNCATE, all seven catalog_order tables
            // are empty (this method is the only committed catalog writer; sibling methods
            // roll back), and every method-local V002/V003 parent UUID is gone.
            assertThat(queryLong(connection, "SELECT count(*) FROM catalog_order.catalog_product"))
                    .as("catalog_product empty after TRUNCATE").isZero();
            assertThat(queryLong(connection, "SELECT count(*) FROM catalog_order.catalog_version"))
                    .as("catalog_version empty after TRUNCATE").isZero();
            assertThat(queryLong(connection, "SELECT count(*) FROM catalog_order.catalog_offer"))
                    .as("catalog_offer empty after TRUNCATE").isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM catalog_order.catalog_benefit_line"))
                    .as("catalog_benefit_line empty after TRUNCATE").isZero();
            assertThat(queryLong(connection, "SELECT count(*) FROM catalog_order.customer_order"))
                    .as("customer_order empty after TRUNCATE").isZero();
            assertThat(queryLong(connection, "SELECT count(*) FROM catalog_order.payment_order"))
                    .as("payment_order empty after TRUNCATE").isZero();
            assertThat(queryLong(connection, "SELECT count(*) FROM catalog_order.payment_event"))
                    .as("payment_event empty after TRUNCATE").isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?",
                    accountId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?",
                    organizationId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.business_subject WHERE id = ?",
                    subjectId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM operations_iam.employee_account WHERE id = ?",
                    employeeId)).isZero();
        }
    }
    // ------------------------------------------------------------------
    // PA5 V005 S2: lifecycle, guard, FK, named-check and concurrency helpers
    // ------------------------------------------------------------------

    private static List<String> v005ExpectedCheckNames(String... tables) {
        int total = 0;
        for (Map.Entry<String, Map<String, String>> entry : V005_CHECK_DEF_MAP.entrySet()) {
            total += entry.getValue().size();
        }
        assertThat(total).as("V005_CHECK_DEF_MAP has exactly 73 named checks").isEqualTo(73);
        List<String> subset = new ArrayList<>();
        for (String table : tables) {
            assertThat(V005_CHECK_DEF_MAP).as("check map for %s", table).containsKey(table);
            subset.addAll(V005_CHECK_DEF_MAP.get(table).keySet());
        }
        return subset;
    }

    private static String v005Ciphertext() {
        return "v1.s2.ivr.ciphertext";
    }

    private static String v005Masked() {
        return "137****0009";
    }

    private static String v005TaskIdText() {
        return "pa5-s2-task-" + UUID.randomUUID();
    }

    private static String v005CallId() {
        return "pa5-s2-call-" + UUID.randomUUID();
    }

    private static String v005IdempotencyKey() {
        return "pa5-s2-idem-" + UUID.randomUUID();
    }

    private static String v005EventId() {
        return "evt-s2-" + UUID.randomUUID();
    }

    private static String v005EventRedactedJson(String payloadSha256, String eventFingerprint) {
        return "{\"source\":{\"eventType\":\"SESSION_START\",\"legId\":\"leg-1\","
                + "\"sequenceNo\":1,\"payload\":{}},\"receipt\":{\"payloadSha256\":\""
                + payloadSha256 + "\",\"eventFingerprint\":\"" + eventFingerprint + "\"}}";
    }

    private static void v005InsertAccount(Connection connection, UUID accountId)
            throws SQLException {
        setLocalRole(connection, "customer_iam_role");
        assertThat(executeUpdate(connection,
                "INSERT INTO customer_iam.customer_account "
                        + "(id, display_name, status) VALUES (?, ?, 'ACTIVE')",
                accountId, "pa5-s2-acct-" + UUID.randomUUID())).isEqualTo(1);
    }

    private static void v005InsertOrganization(Connection connection, UUID organizationId)
            throws SQLException {
        setLocalRole(connection, "organization_access_owner");
        assertThat(executeUpdate(connection,
                "INSERT INTO organization_access.organization "
                        + "(id, display_name, status) VALUES (?, ?, 'ACTIVE')",
                organizationId, "pa5-s2-org-" + UUID.randomUUID())).isEqualTo(1);
    }

    private static void v005InsertSubject(Connection connection, UUID subjectId, UUID accountId)
            throws SQLException {
        setLocalRole(connection, "subject_number_owner");
        assertThat(executeUpdate(connection,
                "INSERT INTO subject_number.business_subject "
                        + "(id, source, owner_account_id) VALUES (?, 'IVR', ?)",
                subjectId, accountId)).isEqualTo(1);
    }

    private static void v005InsertBinding(
            Connection connection, UUID bindingId, UUID subjectId, String hmac)
            throws SQLException {
        setLocalRole(connection, "subject_number_role");
        assertThat(executeUpdate(connection,
                "INSERT INTO subject_number.caller_number_binding "
                        + "(id, subject_id, number_hmac, number_ciphertext, number_masked, "
                        + "number_type, status, verified_at) "
                        + "VALUES (?, ?, ?, ?, ?, 'MOBILE', 'ACTIVE', now())",
                bindingId, subjectId, hmac, v005Ciphertext(), v005Masked())).isEqualTo(1);
    }

    /**
     * Inserts the shared V002/V003 parents for a method-local IVR fixture. Callers must
     * already run in a non-autocommit connection; role ends as migration identity.
     */
    private static void v005InsertParents(
            Connection connection,
            UUID accountId,
            UUID organizationId,
            UUID subjectId,
            UUID bindingId,
            String hmac) throws SQLException {
        v005InsertAccount(connection, accountId);
        v005InsertOrganization(connection, organizationId);
        v005InsertSubject(connection, subjectId, accountId);
        v005InsertBinding(connection, bindingId, subjectId, hmac);
        resetToMigrationIdentity(connection);
    }

    private static int v005InsertTask(
            Connection connection,
            UUID taskId,
            String taskIdText,
            UUID bindingId,
            UUID subjectId,
            UUID accountId,
            String hmac,
            String idempotencyKey,
            String fingerprint) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO ivr.outbound_call_task "
                        + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                        + "created_by_account_id, caller_number_hmac, callee_ciphertext, "
                        + "callee_masked, business_type, expires_at, idempotency_key, "
                        + "request_fingerprint) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                        + "now() + interval '30 seconds', ?, ?)",
                taskId, taskIdText, bindingId, subjectId, accountId, accountId, hmac,
                v005Ciphertext(), v005Masked(), idempotencyKey, fingerprint);
    }

    private static int v005InsertSession(
            Connection connection,
            UUID sessionId,
            String callId,
            UUID subjectId,
            String hmac) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO ivr.inbound_session "
                        + "(id, call_id, subject_id, trusted_caller, caller_ciphertext, "
                        + "caller_number_hmac, caller_masked, access_number_masked, "
                        + "node_id, protocol_version, prompt_version) "
                        + "VALUES (?, ?, ?, TRUE, ?, ?, ?, ?, 'node-1', '1.0', 'p1')",
                sessionId, callId, subjectId, v005Ciphertext(), hmac, v005Masked(), v005Masked());
    }

    private static int v005InsertCall(
            Connection connection,
            UUID callRecordId,
            String callId,
            UUID sessionId,
            UUID subjectId,
            String hmac) throws SQLException {
        return executeUpdate(connection,
                "INSERT INTO ivr.call_record "
                        + "(id, call_id, session_id, subject_id, caller_ciphertext, "
                        + "caller_number_hmac, caller_masked, access_number_masked, "
                        + "node_id, protocol_version, started_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'node-1', '1.0', now())",
                callRecordId, callId, sessionId, subjectId, v005Ciphertext(), hmac,
                v005Masked(), v005Masked());
    }

    private static String v005TaskProbeSql() {
        return "INSERT INTO ivr.outbound_call_task "
                + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                + "created_by_account_id, caller_number_hmac, callee_ciphertext, callee_masked, "
                + "business_type, status, reason_code, ttl_seconds, expires_at, "
                + "idempotency_key, request_fingerprint, claimed_call_id, claimed_at, "
                + "terminal_at, created_at, updated_at, revision) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private static void assertV005TaskTtlLifecycleAndGuardBackstops() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID secondTaskId = UUID.randomUUID();
        UUID replaceTaskId = UUID.randomUUID();
        String hmac = randomSha256();
        String fingerprint = randomSha256();
        String taskIdText = v005TaskIdText();
        String idempotencyKey = v005IdempotencyKey();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                v005InsertParents(connection, accountId, organizationId, subjectId, bindingId, hmac);
                setLocalRole(connection, "ivr_role");
                assertThat(v005InsertTask(connection, taskId, taskIdText, bindingId, subjectId,
                        accountId, hmac, idempotencyKey, fingerprint)).isEqualTo(1);

                // Frozen defaults: PENDING / ttl 30 / revision 1 / expires_at > created_at.
                assertThat(queryString(connection,
                        "SELECT status || '|' || ttl_seconds::text || '|' || revision::text "
                                + "|| '|' || (expires_at > created_at)::text "
                                + "|| '|' || (claimed_call_id IS NULL)::text "
                                + "|| '|' || (terminal_at IS NULL)::text "
                                + "FROM ivr.outbound_call_task WHERE id = ?",
                        taskId)).isEqualTo("PENDING|30|1|true|true|true");

                // Duplicate idempotency tuple fails 23505 and leaves the original TTL/expiry.
                String originalTtlExpiry = queryString(connection,
                        "SELECT ttl_seconds::text || '|' || expires_at::text "
                                + "FROM ivr.outbound_call_task WHERE id = ?",
                        taskId);
                expectSqlFailure(connection, "23505", "uq_outbound_task_idempotency",
                        v005TaskProbeSql(),
                        taskProbeParams(UUID.randomUUID(), v005TaskIdText(), bindingId,
                                subjectId, accountId, hmac, idempotencyKey, fingerprint));
                assertThat(queryString(connection,
                        "SELECT ttl_seconds::text || '|' || expires_at::text "
                                + "FROM ivr.outbound_call_task WHERE id = ?",
                        taskId)).as("duplicate idempotency leaves original TTL/expiry")
                        .isEqualTo(originalTtlExpiry);

                // Owner replay that tries to refresh ttl/expires_at fails the frozen guard.
                switchLocalRole(connection, "ivr_owner");
                expectSqlFailure(connection, "P0001", "OUTBOUND_CALL_TASK_IMMUTABLE",
                        "UPDATE ivr.outbound_call_task "
                                + "SET ttl_seconds = 5, expires_at = now() + interval '5 seconds' "
                                + "WHERE id = ?",
                        taskId);
                expectSqlFailure(connection, "P0001", "OUTBOUND_CALL_TASK_CLAIM_INVALID",
                        "UPDATE ivr.outbound_call_task "
                                + "SET claimed_call_id = ?, claimed_at = now() "
                                + "WHERE id = ?",
                        "no-such-call-" + UUID.randomUUID(), taskId);
                switchLocalRole(connection, "ivr_role");

                // Second live PENDING task for the same canonical caller is rejected.
                expectSqlFailure(connection, "23505", "uq_outbound_task_live_caller",
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                                + "created_by_account_id, caller_number_hmac, callee_ciphertext, "
                                + "callee_masked, business_type, expires_at, idempotency_key, "
                                + "request_fingerprint) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                                + "now() + interval '30 seconds', ?, ?)",
                        secondTaskId, v005TaskIdText(), bindingId, subjectId, accountId, accountId,
                        hmac, v005Ciphertext(), v005Masked(), v005IdempotencyKey(), fingerprint);

                // Conditional PENDING -> terminal succeeds once; stale revision is zero rows.
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.outbound_call_task "
                                + "SET status = 'COMPLETED', terminal_at = now(), "
                                + "reason_code = 'DIRECT_AUTHORIZED', updated_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND status = 'PENDING' AND revision = 1",
                        taskId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.outbound_call_task "
                                + "SET status = 'COMPLETED', terminal_at = now(), "
                                + "updated_at = now(), revision = revision + 1 "
                                + "WHERE id = ? AND status = 'PENDING' AND revision = 1",
                        taskId)).as("stale conditional terminal update affects zero rows").isZero();
                assertThat(queryString(connection,
                        "SELECT status || '|' || (terminal_at IS NOT NULL)::text || '|' "
                                + "|| revision::text FROM ivr.outbound_call_task WHERE id = ?",
                        taskId)).isEqualTo("COMPLETED|true|2");

                // Terminal -> PENDING is rejected by the frozen guard.
                switchLocalRole(connection, "ivr_owner");
                expectSqlFailure(connection, "P0001", "OUTBOUND_CALL_TASK_TERMINAL",
                        "UPDATE ivr.outbound_call_task SET status = 'PENDING' WHERE id = ?",
                        taskId);
                switchLocalRole(connection, "ivr_role");

                // After the first task is terminal, a new PENDING task for the same caller
                // succeeds (live-caller occupancy released). Synthetic DB backstop only;
                // expiry housekeeping and replace/cancel policy remain service-owned.
                assertThat(v005InsertTask(connection, replaceTaskId, v005TaskIdText(), bindingId,
                        subjectId, accountId, hmac, v005IdempotencyKey(), fingerprint))
                        .isEqualTo(1);

                // Runtime immutable-column UPDATE is 42501.
                expectSqlFailure(connection, "42501", null,
                        "UPDATE ivr.outbound_call_task SET caller_number_hmac = ? WHERE id = ?",
                        randomSha256(), taskId);

                // One isolated 23514 + exact-name probe for each of the 16 outbound checks.
                List<String> probed = new ArrayList<>();
                List<CheckProbe> probes = List.of(
                        checkProbe("ck_outbound_task_task_id",
                                v005TaskProbeSql(),
                                taskProbeParams(UUID.randomUUID(), "  ", bindingId, subjectId,
                                        accountId, hmac, v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_single_owner",
                                v005TaskProbeSql(),
                                v005TaskProbeParamsNullOwner(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_caller_hmac",
                                v005TaskProbeSql(),
                                taskProbeParams(UUID.randomUUID(), v005TaskIdText(), bindingId,
                                        subjectId, accountId, "not-an-hmac", v005IdempotencyKey(),
                                        fingerprint)),
                        checkProbe("ck_outbound_task_callee_ciphertext",
                                v005TaskProbeSql(),
                                taskProbeParamsBadCiphertext(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_callee_masked",
                                v005TaskProbeSql(),
                                taskProbeParamsBadMasked(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_business_type",
                                v005TaskProbeSql(),
                                taskProbeParamsBadBusinessType(UUID.randomUUID(),
                                        v005TaskIdText(), bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_status",
                                v005TaskProbeSql(),
                                taskProbeParamsBadStatus(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_reason",
                                v005TaskProbeSql(),
                                taskProbeParamsBadReason(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_ttl",
                                v005TaskProbeSql(),
                                taskProbeParamsBadTtl(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_expiry",
                                v005TaskProbeSql(),
                                taskProbeParamsBadExpiry(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_idempotency_key",
                                v005TaskProbeSql(),
                                taskProbeParamsBadIdempotency(UUID.randomUUID(),
                                        v005TaskIdText(), bindingId, subjectId, accountId, hmac,
                                        fingerprint)),
                        checkProbe("ck_outbound_task_fingerprint",
                                v005TaskProbeSql(),
                                taskProbeParams(UUID.randomUUID(), v005TaskIdText(), bindingId,
                                        subjectId, accountId, hmac, v005IdempotencyKey(),
                                        "not-a-fingerprint")),
                        checkProbe("ck_outbound_task_claimed_shape",
                                v005TaskProbeSql(),
                                taskProbeParamsClaimedShape(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_terminal_shape",
                                v005TaskProbeSql(),
                                taskProbeParamsTerminalShape(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_time_order",
                                v005TaskProbeSql(),
                                taskProbeParamsBadTimeOrder(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)),
                        checkProbe("ck_outbound_task_revision",
                                v005TaskProbeSql(),
                                taskProbeParamsBadRevision(UUID.randomUUID(), v005TaskIdText(),
                                        bindingId, subjectId, accountId, hmac,
                                        v005IdempotencyKey(), fingerprint)));
                for (CheckProbe probe : probes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                            probe.parameters());
                    probed.add(probe.constraint());
                }
                assertThat(probed)
                        .as("all 16 outbound named checks have an isolated 23514 probe")
                        .containsExactlyInAnyOrderElementsOf(
                                v005ExpectedCheckNames("outbound_call_task"));
                assertThat(probed).hasSize(16);

                connection.rollback();
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM ivr.outbound_call_task WHERE id IN (?, ?, ?)",
                    taskId, secondTaskId, replaceTaskId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.caller_number_binding WHERE id = ?",
                    bindingId)).isZero();
        }
    }

    /** Builds full 22-parameter task probe params with one intentional defect hook. */
    private static Object[] taskProbeParams(
            UUID taskId,
            String taskIdText,
            UUID bindingId,
            UUID subjectId,
            UUID accountId,
            String hmac,
            String idempotencyKey,
            String fingerprint) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Object[] {
                taskId, taskIdText, bindingId, subjectId, accountId, accountId, hmac,
                v005Ciphertext(), v005Masked(), "EVIDENCE_CALL", "PENDING", null, 30,
                now.plusSeconds(30), idempotencyKey, fingerprint, null, null, null, now, now, 1
        };
    }

    /**
     * Owner-null probe for ck_outbound_task_single_owner. created_by_account_id stays
     * NOT NULL; only owner_account_id is cleared (owner_organization_id is absent from
     * the probe INSERT and defaults to NULL) so num_nonnulls is 0 and only the named
     * CHECK can fire.
     */
    private static Object[] v005TaskProbeParamsNullOwner(
            UUID taskId,
            String taskIdText,
            UUID bindingId,
            UUID subjectId,
            UUID accountId,
            String hmac,
            String idempotencyKey,
            String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[4] = null;
        return params;
    }

    private static Object[] taskProbeParamsBadCiphertext(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[7] = "plaintext-callee";
        return params;
    }

    private static Object[] taskProbeParamsBadMasked(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[8] = "   ";
        return params;
    }

    private static Object[] taskProbeParamsBadBusinessType(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[9] = " ";
        return params;
    }

    private static Object[] taskProbeParamsBadStatus(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[10] = "NOT_A_STATUS";
        return params;
    }

    private static Object[] taskProbeParamsBadReason(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[11] = " ";
        return params;
    }

    private static Object[] taskProbeParamsBadTtl(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[12] = 0;
        return params;
    }

    private static Object[] taskProbeParamsBadExpiry(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[13] = params[19];
        return params;
    }

    private static Object[] taskProbeParamsBadIdempotency(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, " ", fingerprint);
        return params;
    }

    private static Object[] taskProbeParamsClaimedShape(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[16] = "no-parent-call";
        params[17] = null;
        return params;
    }

    private static Object[] taskProbeParamsTerminalShape(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[10] = "CANCELLED";
        params[18] = null;
        return params;
    }

    private static Object[] taskProbeParamsBadTimeOrder(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        OffsetDateTime created = OffsetDateTime.now(ZoneOffset.UTC);
        params[19] = created;
        params[20] = created.minusSeconds(5);
        return params;
    }

    private static Object[] taskProbeParamsBadRevision(
            UUID taskId, String taskIdText, UUID bindingId, UUID subjectId, UUID accountId,
            String hmac, String idempotencyKey, String fingerprint) {
        Object[] params = taskProbeParams(taskId, taskIdText, bindingId, subjectId, accountId,
                hmac, idempotencyKey, fingerprint);
        params[21] = 0;
        return params;
    }


    private static String v005SessionProbeSql() {
        return "INSERT INTO ivr.inbound_session "
                + "(id, call_id, subject_id, trusted_caller, caller_ciphertext, "
                + "caller_number_hmac, caller_masked, access_number_masked, node_id, "
                + "protocol_version, prompt_version, status, confirmed_subject_id, "
                + "guidance_status, created_at, confirmed_at, revision) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private static Object[] v005ValidSessionParams(
            UUID sessionId, String callId, UUID subjectId, String hmac) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Object[] {
                sessionId, callId, subjectId, true, v005Ciphertext(), hmac, v005Masked(),
                v005Masked(), "node-1", "1.0", "p1", "RECEIVED", null, null, now, null, 1
        };
    }

    private static String v005CallProbeSql() {
        return "INSERT INTO ivr.call_record "
                + "(id, call_id, session_id, task_id, subject_id, caller_ciphertext, "
                + "caller_number_hmac, caller_masked, callee_ciphertext, callee_masked, "
                + "access_number_masked, status, node_id, protocol_version, started_at, "
                + "bridged_at, recording_started_at, recording_ended_at, ended_at, "
                + "authoritative_seconds, end_reason, created_at, revision) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private static Object[] v005ValidCallParams(
            UUID callRecordId, String callId, UUID sessionId, UUID subjectId, String hmac) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Object[] {
                callRecordId, callId, sessionId, null, subjectId, v005Ciphertext(), hmac,
                v005Masked(), null, null, v005Masked(), "ACTIVE", "node-1", "1.0", now,
                null, null, null, null, null, null, now, 1
        };
    }

    private static Object[] mutate(Object[] params, int index, Object value) {
        Object[] copy = params.clone();
        copy[index] = value;
        return copy;
    }

    private static void assertV005SessionCallCanonicalCallerAndReciprocalLinkBackstops()
            throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID callRecordId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID rootSessionId = UUID.randomUUID();
        UUID rootCallId = UUID.randomUUID();
        UUID terminalSessionId = UUID.randomUUID();
        UUID terminalCallId = UUID.randomUUID();
        UUID occupancySessionId = UUID.randomUUID();
        UUID occupancyUnknownCallId = UUID.randomUUID();
        UUID occupancyActiveCallId = UUID.randomUUID();
        UUID secondTaskId = UUID.randomUUID();
        String hmac = randomSha256();
        String callId = v005CallId();
        String rootCallIdText = "pa5-s2-root-" + UUID.randomUUID();
        String occupancyCallId = v005CallId();
        String unknownCallId = v005CallId();
        String fingerprint = randomSha256();
        // Distinct caller HMACs: uq_call_record_active_caller allows only one ACTIVE/UNKNOWN
        // call per hmac, so secondary live calls must not share the primary call's hmac.
        String rootCallHmac = randomSha256();
        String occupancyHmac = randomSha256();
        String terminalHmac = randomSha256();
        String reverseCallHmac = randomSha256();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                v005InsertParents(connection, accountId, organizationId, subjectId, bindingId, hmac);
                setLocalRole(connection, "ivr_role");
                assertThat(v005InsertSession(connection, sessionId, callId, subjectId, hmac))
                        .isEqualTo(1);
                assertThat(v005InsertCall(connection, callRecordId, callId, sessionId, subjectId,
                        hmac)).isEqualTo(1);
                assertThat(v005InsertTask(connection, taskId, v005TaskIdText(), bindingId,
                        subjectId, accountId, hmac, v005IdempotencyKey(), fingerprint))
                        .isEqualTo(1);

                // Defaults: RECEIVED/revision 1; ACTIVE/revision 1; task_id NULL.
                assertThat(queryString(connection,
                        "SELECT status || '|' || revision::text FROM ivr.inbound_session "
                                + "WHERE id = ?",
                        sessionId)).isEqualTo("RECEIVED|1");
                assertThat(queryString(connection,
                        "SELECT status || '|' || revision::text "
                                + "|| '|' || (task_id IS NULL)::text "
                                + "|| '|' || (ended_at IS NULL)::text "
                                + "FROM ivr.call_record WHERE id = ?",
                        callRecordId)).isEqualTo("ACTIVE|1|true|true");

                // Unique session call_id.
                expectSqlFailure(connection, "23505", "uq_inbound_session_call",
                        "INSERT INTO ivr.inbound_session "
                                + "(id, call_id, subject_id, trusted_caller, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, prompt_version) "
                                + "VALUES (?, ?, ?, TRUE, ?, ?, ?, ?, 'node-1', '1.0', 'p1')",
                        UUID.randomUUID(), callId, subjectId, v005Ciphertext(), hmac,
                        v005Masked(), v005Masked());

                // Root NO_FK identities insert without any parent identity.
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.inbound_session "
                                + "(id, call_id, trusted_caller, access_number_masked, node_id, "
                                + "protocol_version, prompt_version) "
                                + "VALUES (?, ?, FALSE, ?, 'node-1', '1.0', 'p1')",
                        rootSessionId, rootCallIdText, v005Masked())).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.call_record "
                                + "(id, call_id, session_id, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, started_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'node-1', '1.0', now())",
                        rootCallId, "pa5-s2-rootcall-" + UUID.randomUUID(), rootSessionId,
                        v005Ciphertext(), rootCallHmac, v005Masked(), v005Masked())).isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM pg_constraint c "
                                + "JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = 'ivr' AND r.relname = 'inbound_session' "
                                + "AND c.contype = 'f' "
                                + "AND pg_get_constraintdef(c.oid) LIKE '%call_id%'"))
                        .as("inbound_session.call_id is an intentional NO_FK root").isZero();
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM pg_constraint c "
                                + "JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = 'ivr' AND r.relname = 'call_record' "
                                + "AND c.contype = 'f' "
                                + "AND pg_get_constraintdef(c.oid) LIKE '%call_id%'"))
                        .as("call_record.call_id is an intentional NO_FK root").isZero();

                // Active/UNKNOWN occupancy per canonical caller.
                assertThat(v005InsertSession(connection, occupancySessionId, occupancyCallId,
                        subjectId, hmac)).isEqualTo(1);
                assertThat(v005InsertCall(connection, occupancyUnknownCallId, occupancyCallId,
                        occupancySessionId, subjectId, occupancyHmac)).isEqualTo(1);
                switchLocalRole(connection, "ivr_role");
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.call_record SET status = 'UNKNOWN', revision = revision + 1 "
                                + "WHERE id = ? AND status = 'ACTIVE' AND revision = 1",
                        occupancyUnknownCallId)).isEqualTo(1);
                // Probe uses a fresh session: uq_call_record_session is one call per session
                // and would otherwise fire before the caller occupancy unique.
                UUID occupancyProbeSession = UUID.randomUUID();
                assertThat(v005InsertSession(connection, occupancyProbeSession, v005CallId(),
                        subjectId, hmac)).isEqualTo(1);
                expectSqlFailure(connection, "23505", "uq_call_record_active_caller",
                        "INSERT INTO ivr.call_record "
                                + "(id, call_id, session_id, subject_id, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, started_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'node-1', '1.0', now())",
                        occupancyActiveCallId, v005CallId(), occupancyProbeSession, subjectId,
                        v005Ciphertext(), occupancyHmac, v005Masked(), v005Masked());
                // Second session for the occupancy call is blocked by unique session call_id;
                // use a fresh session id with the same caller hmac for the second ACTIVE probe
                // after releasing occupancy via terminal transition.
                assertThat(v005InsertSession(connection, terminalSessionId, v005CallId(),
                        subjectId, hmac)).isEqualTo(1);
                assertThat(v005InsertCall(connection, terminalCallId, v005CallId(),
                        terminalSessionId, subjectId, terminalHmac)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.call_record "
                                + "SET status = 'ENDED', ended_at = now(), end_reason = 'COMPLETE', "
                                + "authoritative_seconds = 1, revision = revision + 1 "
                                + "WHERE id = ? AND status = 'ACTIVE' AND revision = 1",
                        terminalCallId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.call_record "
                                + "SET status = 'ENDED', ended_at = now(), end_reason = 'COMPLETE', "
                                + "authoritative_seconds = 1, revision = revision + 1 "
                                + "WHERE id = ? AND status = 'UNKNOWN' AND revision = 2",
                        occupancyUnknownCallId)).isEqualTo(1);
                // Post-terminal ACTIVE occupancy is proven by a fresh session+call pair.
                UUID postTerminalSession = UUID.randomUUID();
                String postTerminalCallId = v005CallId();
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.inbound_session "
                                + "(id, call_id, subject_id, trusted_caller, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, prompt_version) "
                                + "VALUES (?, ?, ?, TRUE, ?, ?, ?, ?, 'node-1', '1.0', 'p1')",
                        postTerminalSession, postTerminalCallId, subjectId, v005Ciphertext(),
                        hmac, v005Masked(), v005Masked())).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.call_record "
                                + "(id, call_id, session_id, subject_id, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, started_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'node-1', '1.0', now())",
                        UUID.randomUUID(), postTerminalCallId, postTerminalSession, subjectId,
                        v005Ciphertext(), terminalHmac, v005Masked(), v005Masked())).isEqualTo(1);

                // Immutable origin: owner P0001 and runtime 42501; started_at is granted
                // but guard-rejected with P0001 (frozen grant/guard inconsistency F-PA5-PLAN-04).
                // Use clock_timestamp() so the value is distinct from the row's now()
                // transaction timestamp (same now() would be a no-op for the guard).
                switchLocalRole(connection, "ivr_owner");
                expectSqlFailure(connection, "P0001", "INBOUND_SESSION_IMMUTABLE",
                        "UPDATE ivr.inbound_session SET call_id = ? WHERE id = ?",
                        "tampered-" + UUID.randomUUID(), sessionId);
                expectSqlFailure(connection, "P0001", "CALL_RECORD_PARTIES_IMMUTABLE",
                        "UPDATE ivr.call_record SET started_at = clock_timestamp() WHERE id = ?",
                        callRecordId);
                switchLocalRole(connection, "ivr_role");
                expectSqlFailure(connection, "42501", null,
                        "UPDATE ivr.inbound_session SET caller_number_hmac = ? WHERE id = ?",
                        randomSha256(), sessionId);
                expectSqlFailure(connection, "42501", null,
                        "UPDATE ivr.call_record SET caller_number_hmac = ? WHERE id = ?",
                        randomSha256(), callRecordId);
                expectSqlFailure(connection, "P0001", "CALL_RECORD_PARTIES_IMMUTABLE",
                        "UPDATE ivr.call_record SET started_at = clock_timestamp() WHERE id = ?",
                        callRecordId);

                // Reciprocal link order: claimed_call_id+claimed_at first, then call.task_id.
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.outbound_call_task "
                                + "SET claimed_call_id = ?, claimed_at = now(), status = 'CLAIMED', "
                                + "updated_at = now(), revision = revision + 1 "
                                + "WHERE id = ? AND status = 'PENDING' AND claimed_call_id IS NULL "
                                + "AND revision = 1",
                        callId, taskId)).isEqualTo(1);
                // Reverse order is guarded: task_id before claimed_call_id.
                UUID reverseTaskId = UUID.randomUUID();
                assertThat(v005InsertTask(connection, reverseTaskId, v005TaskIdText(), bindingId,
                        subjectId, accountId, randomSha256(), v005IdempotencyKey(), fingerprint))
                        .isEqualTo(1);
                UUID reverseSessionId = UUID.randomUUID();
                String reverseCallId = v005CallId();
                assertThat(v005InsertSession(connection, reverseSessionId, reverseCallId,
                        subjectId, hmac)).isEqualTo(1);
                UUID reverseCallRecordId = UUID.randomUUID();
                assertThat(v005InsertCall(connection, reverseCallRecordId, reverseCallId,
                        reverseSessionId, subjectId, reverseCallHmac)).isEqualTo(1);
                expectSqlFailure(connection, "P0001", "CALL_RECORD_PARTIES_IMMUTABLE",
                        "UPDATE ivr.call_record SET task_id = ? WHERE id = ?",
                        reverseTaskId, reverseCallRecordId);
                // Legal forward order for the reverse fixture, then link the primary pair.
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.call_record SET task_id = ?, revision = revision + 1 "
                                + "WHERE id = ? AND task_id IS NULL AND revision = 1",
                        taskId, callRecordId)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT t.claimed_call_id || '|' || c.task_id::text "
                                + "FROM ivr.outbound_call_task t "
                                + "JOIN ivr.call_record c ON c.call_id = t.claimed_call_id "
                                + "WHERE t.id = ?",
                        taskId)).as("both link directions are equal").isEqualTo(callId + "|" + taskId);

                // Relink/clear/second-task rejections.
                expectSqlFailure(connection, "P0001", "OUTBOUND_CALL_TASK_CLAIM_IMMUTABLE",
                        "UPDATE ivr.outbound_call_task SET claimed_call_id = ? WHERE id = ?",
                        reverseCallId, taskId);
                expectSqlFailure(connection, "P0001", "OUTBOUND_CALL_TASK_CLAIM_IMMUTABLE",
                        "UPDATE ivr.outbound_call_task SET claimed_call_id = NULL WHERE id = ?",
                        taskId);
                expectSqlFailure(connection, "P0001", "CALL_RECORD_PARTIES_IMMUTABLE",
                        "UPDATE ivr.call_record SET task_id = NULL WHERE id = ?", callRecordId);
                expectSqlFailure(connection, "P0001", "CALL_RECORD_PARTIES_IMMUTABLE",
                        "UPDATE ivr.call_record SET task_id = ? WHERE id = ?",
                        reverseTaskId, callRecordId);
                // Second task claiming the same call hits the partial unique index.
                expectSqlFailure(connection, "23505", "uq_outbound_task_claimed_call",
                        "UPDATE ivr.outbound_call_task "
                                + "SET claimed_call_id = ?, claimed_at = now(), status = 'CLAIMED', "
                                + "updated_at = now(), revision = revision + 1 "
                                + "WHERE id = ? AND status = 'PENDING' AND claimed_call_id IS NULL "
                                + "AND revision = 1",
                        callId, reverseTaskId);

                // Session + call_record check probes (14 + 15).
                List<String> probed = new ArrayList<>();
                List<CheckProbe> sessionProbes = List.of(
                        checkProbe("ck_inbound_session_call_id",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(), "  ", subjectId,
                                        hmac), 1, "  ")),
                        checkProbe("ck_inbound_session_caller_shape",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 5, null)),
                        checkProbe("ck_inbound_session_caller_ciphertext",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 4, "plain")),
                        checkProbe("ck_inbound_session_caller_hmac",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 5, "nothex")),
                        checkProbe("ck_inbound_session_caller_masked",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 6, " ")),
                        checkProbe("ck_inbound_session_access_masked",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 7, " ")),
                        checkProbe("ck_inbound_session_node",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 8, " ")),
                        checkProbe("ck_inbound_session_protocol",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 9, " ")),
                        checkProbe("ck_inbound_session_prompt",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 10, " ")),
                        checkProbe("ck_inbound_session_status",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 11, "BOGUS")),
                        checkProbe("ck_inbound_session_guidance",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 13, "BOGUS")),
                        checkProbe("ck_inbound_session_confirmed_shape",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 11, "CONFIRMED")),
                        checkProbe("ck_inbound_session_time_order", v005SessionProbeSql(),
                                sessionProbeBadTimeOrder(subjectId, hmac)),
                        checkProbe("ck_inbound_session_revision",
                                v005SessionProbeSql(),
                                mutate(v005ValidSessionParams(UUID.randomUUID(),
                                        v005CallId(), subjectId, hmac), 16, 0)));
                for (CheckProbe probe : sessionProbes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                            probe.parameters());
                    probed.add(probe.constraint());
                }
                assertThat(probed)
                        .as("all 14 inbound_session named checks have an isolated probe")
                        .containsExactlyInAnyOrderElementsOf(
                                v005ExpectedCheckNames("inbound_session"));
                assertThat(probed).hasSize(14);

                List<String> callProbed = new ArrayList<>();
                Object[] badCalleeCipher = callProbeBase(UUID.randomUUID(), v005CallId(),
                        sessionId, subjectId, hmac);
                badCalleeCipher[8] = "plain-callee";
                Object[] badCalleeMasked = callProbeBase(UUID.randomUUID(), v005CallId(),
                        sessionId, subjectId, hmac);
                badCalleeMasked[9] = " ";
                Object[] badAccess = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badAccess[10] = " ";
                Object[] badStatus = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badStatus[11] = "BOGUS";
                Object[] badNode = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badNode[12] = " ";
                Object[] badProtocol = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badProtocol[13] = " ";
                Object[] badTerminal = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badTerminal[11] = "ENDED";
                badTerminal[18] = null;
                Object[] badTime = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                OffsetDateTime started = OffsetDateTime.now(ZoneOffset.UTC);
                badTime[14] = started;
                badTime[15] = started.minusSeconds(10);
                Object[] badSeconds = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badSeconds[11] = "ACTIVE";
                badSeconds[19] = 10;
                Object[] badEndReason = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badEndReason[11] = "ENDED";
                badEndReason[18] = OffsetDateTime.now(ZoneOffset.UTC);
                badEndReason[20] = " ";
                Object[] badRevision = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badRevision[22] = 0;
                Object[] badCipher = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badCipher[5] = "plain";
                Object[] badHmac = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badHmac[6] = "nothex";
                Object[] badMasked = callProbeBase(UUID.randomUUID(), v005CallId(), sessionId,
                        subjectId, hmac);
                badMasked[7] = " ";
                Object[] badCallId = callProbeBase(UUID.randomUUID(), "  ", sessionId, subjectId,
                        hmac);
                List<Object[]> defective = List.of(
                        badCallId, badCipher, badHmac, badMasked, badCalleeCipher, badCalleeMasked,
                        badAccess, badStatus, badNode, badProtocol, badTerminal, badTime,
                        badSeconds, badEndReason, badRevision);
                List<String> callCheckOrder = List.of(
                        "ck_call_record_call_id", "ck_call_record_caller_ciphertext",
                        "ck_call_record_caller_hmac", "ck_call_record_caller_masked",
                        "ck_call_record_callee_ciphertext", "ck_call_record_callee_masked",
                        "ck_call_record_access_masked", "ck_call_record_status",
                        "ck_call_record_node", "ck_call_record_protocol",
                        "ck_call_record_terminal_shape", "ck_call_record_time_order",
                        "ck_call_record_authoritative_seconds", "ck_call_record_end_reason",
                        "ck_call_record_revision");
                for (int index = 0; index < callCheckOrder.size(); index++) {
                    String name = callCheckOrder.get(index);
                    expectSqlFailure(connection, "23514", name, v005CallProbeSql(),
                            defective.get(index));
                    callProbed.add(name);
                }
                assertThat(callProbed)
                        .as("all 15 call_record named checks have an isolated probe")
                        .containsExactlyInAnyOrderElementsOf(
                                v005ExpectedCheckNames("call_record"));
                assertThat(callProbed).hasSize(15);

                connection.rollback();
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM ivr.inbound_session WHERE id IN (?, ?, ?)",
                    sessionId, rootSessionId, terminalSessionId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM ivr.call_record WHERE id IN (?, ?, ?)",
                    callRecordId, rootCallId, terminalCallId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM ivr.outbound_call_task WHERE id IN (?, ?)",
                    taskId, secondTaskId)).isZero();
        }
    }

    /**
     * Builds a defective call_record probe base by mutating one column of the valid shape.
     * Index map matches {@link #v005CallProbeSql()} column order.
     */
    private static Object[] callProbeBase(
            UUID callRecordId, String callId, UUID sessionId, UUID subjectId, String hmac) {
        return v005ValidCallParams(callRecordId, callId, sessionId, subjectId, hmac);
    }


    private static String v005LegProbeSql() {
        return "INSERT INTO ivr.call_leg "
                + "(id, call_id, leg_id, leg_type, direction, state, started_at, "
                + "answered_at, ended_at, end_reason, revision) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private static Object[] v005ValidLegParams(UUID legId, String callId, String legIdText) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new Object[] {
                legId, callId, legIdText, "CALLER", "INBOUND", "RINGING", now, null, null, null, 1
        };
    }

    private static String v005EventProbeSql() {
        return "INSERT INTO ivr.call_event "
                + "(id, call_id, event_domain, event_id, sequence_no, event_type, occurred_at, "
                + "received_at, payload_ciphertext, payload_redacted, payload_sha256, "
                + "processing_status, reason_code, revision) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, now(), ?, ?::jsonb, ?, ?, ?, ?)";
    }

    private static Object[] v005ValidEventParams(
            UUID eventId, String callId, String eventIdText, String sequenceNo, String sha) {
        return new Object[] {
                eventId, callId, "IVR_EVENT", eventIdText, Integer.valueOf(sequenceNo),
                "SESSION_START", OffsetDateTime.now(ZoneOffset.UTC), v005Ciphertext(),
                v005EventRedactedJson(sha, randomSha256()), sha, "RECEIVED", null, 1
        };
    }

    private static String v005DecisionProbeSql() {
        return "INSERT INTO ivr.ivr_decision "
                + "(id, session_id, call_id, purpose, source_event_domain, source_event_id, "
                + "request_fingerprint, action, reason_code, response_snapshot, "
                + "response_payload_ciphertext, response_sha256, key_version, reservation_id, "
                + "decided_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?)";
    }

    private static Object[] v005ValidDecisionParams(
            UUID decisionId, UUID sessionId, String callId, String fingerprint, String sha) {
        return new Object[] {
                decisionId, sessionId, callId, "INBOUND_RESOLVE", null, null, fingerprint,
                "GUIDE_ACTIVATION", "ACTIVATION_REQUIRED", "{}", v005Ciphertext(), sha, 1, null,
                OffsetDateTime.now(ZoneOffset.UTC)
        };
    }

    private static void assertV005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops()
            throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID callRecordId = UUID.randomUUID();
        UUID callerLegId = UUID.randomUUID();
        UUID calleeLegId = UUID.randomUUID();
        UUID duplicateLegId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID duplicateDomainEventId = UUID.randomUUID();
        UUID duplicateSequenceEventId = UUID.randomUUID();
        UUID otherCallEventId = UUID.randomUUID();
        UUID inboundDecisionId = UUID.randomUUID();
        UUID activationDecisionId = UUID.randomUUID();
        UUID enterCalleeDecisionId = UUID.randomUUID();
        UUID dialDecisionId = UUID.randomUUID();
        UUID directSuccessDecisionId = UUID.randomUUID();
        UUID directRefusalDecisionId = UUID.randomUUID();
        UUID duplicatePurposeDecisionId = UUID.randomUUID();
        UUID secondSessionId = UUID.randomUUID();
        UUID secondCallRecordId = UUID.randomUUID();
        String hmac = randomSha256();
        // Second live call needs its own hmac: uq_call_record_active_caller blocks a second
        // ACTIVE/UNKNOWN row for the same canonical caller.
        String secondCallHmac = randomSha256();
        String fingerprint = randomSha256();
        String callId = v005CallId();
        String otherCallId = v005CallId();
        String eventIdText = v005EventId();
        String callerLegIdText = "leg-caller-" + UUID.randomUUID();
        String calleeLegIdText = "leg-callee-" + UUID.randomUUID();
        String sha = randomSha256();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                v005InsertParents(connection, accountId, organizationId, subjectId, bindingId, hmac);
                setLocalRole(connection, "ivr_role");
                assertThat(v005InsertSession(connection, sessionId, callId, subjectId, hmac))
                        .isEqualTo(1);
                assertThat(v005InsertCall(connection, callRecordId, callId, sessionId, subjectId,
                        hmac)).isEqualTo(1);
                assertThat(v005InsertSession(connection, secondSessionId, otherCallId, subjectId,
                        hmac)).isEqualTo(1);
                assertThat(v005InsertCall(connection, secondCallRecordId, otherCallId,
                        secondSessionId, subjectId, secondCallHmac)).isEqualTo(1);

                // Legal CALLER/INBOUND and CALLEE/OUTBOUND legs.
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.call_leg "
                                + "(id, call_id, leg_id, leg_type, direction, state, started_at) "
                                + "VALUES (?, ?, ?, 'CALLER', 'INBOUND', 'RINGING', now())",
                        callerLegId, callId, callerLegIdText)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.call_leg "
                                + "(id, call_id, leg_id, leg_type, direction, state, started_at) "
                                + "VALUES (?, ?, ?, 'CALLEE', 'OUTBOUND', 'RINGING', now())",
                        calleeLegId, callId, calleeLegIdText)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT leg_type || '|' || direction FROM ivr.call_leg WHERE id = ?",
                        callerLegId)).isEqualTo("CALLER|INBOUND");

                // Duplicate (call_id, leg_id) is exact 23505.
                expectSqlFailure(connection, "23505", "uq_call_leg_id",
                        "INSERT INTO ivr.call_leg "
                                + "(id, call_id, leg_id, leg_type, direction, state, started_at) "
                                + "VALUES (?, ?, ?, 'CALLER', 'INBOUND', 'RINGING', now())",
                        duplicateLegId, callId, callerLegIdText);

                // Allowed processing/state updates, then owner identity guard P0001.
                switchLocalRole(connection, "ivr_role");
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.call_leg SET state = 'ANSWERED', answered_at = now(), "
                                + "revision = revision + 1 WHERE id = ? AND state = 'RINGING'",
                        callerLegId)).isEqualTo(1);
                switchLocalRole(connection, "ivr_owner");
                expectSqlFailure(connection, "P0001", "CALL_LEG_IDENTITY_IMMUTABLE",
                        "UPDATE ivr.call_leg SET leg_id = ? WHERE id = ?",
                        "tampered-leg-" + UUID.randomUUID(), callerLegId);
                expectSqlFailure(connection, "P0001", "CALL_LEG_IDENTITY_IMMUTABLE",
                        "UPDATE ivr.call_leg SET direction = 'OUTBOUND' WHERE id = ?", callerLegId);
                switchLocalRole(connection, "ivr_role");
                expectSqlFailure(connection, "42501", null,
                        "UPDATE ivr.call_leg SET leg_type = 'CALLEE' WHERE id = ?", callerLegId);

                // Legal event with exact redacted shape and IVR_EVENT default.
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.call_event "
                                + "(id, call_id, event_id, sequence_no, event_type, occurred_at, "
                                + "payload_ciphertext, payload_redacted, payload_sha256) "
                                + "VALUES (?, ?, ?, 1, 'SESSION_START', now(), ?, ?::jsonb, ?)",
                        eventId, callId, eventIdText, v005Ciphertext(),
                        v005EventRedactedJson(sha, randomSha256()), sha)).isEqualTo(1);
                assertThat(queryString(connection,
                        "SELECT event_domain || '|' || processing_status || '|' || revision::text "
                                + "FROM ivr.call_event WHERE id = ?",
                        eventId)).isEqualTo("IVR_EVENT|RECEIVED|1");

                // Duplicate (event_domain, event_id) across calls is exact 23505.
                expectSqlFailure(connection, "23505", "uq_call_event_domain_id",
                        v005EventProbeSql(),
                        v005ValidEventParams(UUID.randomUUID(), otherCallId, eventIdText, "9",
                                randomSha256()));
                // Duplicate (call_id, sequence_no) is exact 23505.
                expectSqlFailure(connection, "23505", "uq_call_event_sequence",
                        v005EventProbeSql(),
                        v005ValidEventParams(duplicateSequenceEventId, callId,
                                v005EventId(), "1", randomSha256()));

                // Allowed processing transitions; forbidden runtime identity/payload 42501.
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.call_event SET processing_status = 'APPLIED', "
                                + "revision = revision + 1 WHERE id = ? AND processing_status = 'RECEIVED'",
                        eventId)).isEqualTo(1);
                expectSqlFailure(connection, "42501", null,
                        "UPDATE ivr.call_event SET payload_sha256 = ? WHERE id = ?",
                        randomSha256(), eventId);
                expectSqlFailure(connection, "42501", null,
                        "UPDATE ivr.call_event SET event_id = ? WHERE id = ?",
                        "tampered-evt-" + UUID.randomUUID(), eventId);

                // Legal decision shapes: inbound resolve, activation, ENTER_CALLEE,
                // DIAL_TASK_CALLEE + random reservationUUID (no FK), direct success and
                // refusal. DTMF/ENTER_CALLEE remain shapes only (service-owned input).
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, request_fingerprint, "
                                + "action, reason_code, response_snapshot, "
                                + "response_payload_ciphertext, response_sha256, key_version) "
                                + "VALUES (?, ?, ?, 'INBOUND_RESOLVE', ?, 'GUIDE_ACTIVATION', "
                                + "'ACTIVATION_REQUIRED', '{}'::jsonb, ?, ?, 1)",
                        inboundDecisionId, sessionId, callId, fingerprint, v005Ciphertext(),
                        sha)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, source_event_domain, "
                                + "source_event_id, request_fingerprint, action, reason_code, "
                                + "response_snapshot, response_payload_ciphertext, "
                                + "response_sha256, key_version) "
                                + "VALUES (?, ?, ?, 'ACTIVATION_NEXT_ACTION', 'IVR_CONFIRM', ?, "
                                + "?, 'GUIDE_ACTIVATION', 'ACTIVATION_REQUIRED', '{}'::jsonb, "
                                + "?, ?, 1)",
                        activationDecisionId, sessionId, callId, eventIdText, fingerprint,
                        v005Ciphertext(), sha)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, source_event_domain, "
                                + "source_event_id, request_fingerprint, action, reason_code, "
                                + "response_snapshot, response_payload_ciphertext, "
                                + "response_sha256, key_version) "
                                + "VALUES (?, ?, ?, 'INBOUND_RESOLVE', NULL, NULL, ?, "
                                + "'ENTER_CALLEE', 'CALL_STATE_UNRESOLVED', '{}'::jsonb, "
                                + "?, ?, 1)",
                        enterCalleeDecisionId, secondSessionId, otherCallId, fingerprint,
                        v005Ciphertext(), sha)).isEqualTo(1);
                UUID randomReservationId = UUID.randomUUID();
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, source_event_domain, "
                                + "source_event_id, request_fingerprint, action, reason_code, "
                                + "response_snapshot, response_payload_ciphertext, "
                                + "response_sha256, key_version, reservation_id) "
                                + "VALUES (?, ?, ?, 'ACTIVATION_NEXT_ACTION', 'IVR_CONFIRM', ?, "
                                + "?, 'DIAL_TASK_CALLEE', 'TASK_CLAIMED', '{}'::jsonb, "
                                + "?, ?, 1, ?)",
                        dialDecisionId, secondSessionId, otherCallId, "evt-dial-"
                                + UUID.randomUUID(), fingerprint, v005Ciphertext(), sha,
                        randomReservationId)).isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM ivr.ivr_decision WHERE id = ? AND reservation_id = ?",
                        dialDecisionId, randomReservationId))
                        .as("DIAL_TASK_CALLEE reservation UUID has no FK parent").isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM pg_constraint c "
                                + "JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = 'ivr' AND r.relname = 'ivr_decision' "
                                + "AND c.contype = 'f' "
                                + "AND pg_get_constraintdef(c.oid) LIKE '%reservation%'"))
                        .as("reservation_id is intentional noFK").isZero();
                UUID directReservationId = UUID.randomUUID();
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, source_event_domain, "
                                + "source_event_id, request_fingerprint, action, reason_code, "
                                + "response_snapshot, response_payload_ciphertext, "
                                + "response_sha256, key_version, reservation_id) "
                                + "VALUES (?, ?, ?, 'DIRECT_AUTHORIZATION', 'IVR_DIRECT_AUTH', ?, "
                                + "?, NULL, 'DIRECT_AUTHORIZED', '{}'::jsonb, ?, ?, 1, ?)",
                        directSuccessDecisionId, sessionId, callId,
                        "evt-direct-" + UUID.randomUUID(), fingerprint, v005Ciphertext(), sha,
                        directReservationId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, source_event_domain, "
                                + "source_event_id, request_fingerprint, action, reason_code, "
                                + "response_snapshot, response_payload_ciphertext, "
                                + "response_sha256, key_version) "
                                + "VALUES (?, ?, ?, 'DIRECT_AUTHORIZATION', 'IVR_DIRECT_AUTH', ?, "
                                + "?, 'REJECT', 'CALL_IN_PROGRESS', '{}'::jsonb, ?, ?, 1)",
                        directRefusalDecisionId, secondSessionId, otherCallId,
                        "evt-direct-refuse-" + UUID.randomUUID(), fingerprint, v005Ciphertext(),
                        sha)).isEqualTo(1);

                // Event-based source domain/id are explicitly nonNULL for activation.
                assertThat(queryString(connection,
                        "SELECT source_event_domain || '|' || (source_event_id IS NOT NULL)::text "
                                + "FROM ivr.ivr_decision WHERE id = ?",
                        activationDecisionId)).isEqualTo("IVR_CONFIRM|true");

                // Duplicate (call_id, purpose) + same source: PostgreSQL reports
                // uq_ivr_decision_call_purpose first (created with the table, lower OID).
                // While unisolated a solo uq_ivr_decision_source violation is unreachable:
                // ck_ivr_decision_source binds domain to purpose and the call_purpose unique
                // preempts. The bounded isolation helper below proves the exact 23505 + name.
                expectSqlFailure(connection, "23505", "uq_ivr_decision_call_purpose",
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, source_event_domain, "
                                + "source_event_id, request_fingerprint, action, reason_code, "
                                + "response_snapshot, response_payload_ciphertext, "
                                + "response_sha256, key_version) "
                                + "VALUES (?, ?, ?, 'ACTIVATION_NEXT_ACTION', 'IVR_CONFIRM', ?, "
                                + "?, 'GUIDE_ACTIVATION', 'ACTIVATION_REQUIRED', '{}'::jsonb, "
                                + "?, ?, 1)",
                        duplicatePurposeDecisionId, sessionId, callId, eventIdText, fingerprint,
                        v005Ciphertext(), sha);
                // Wrong domain for DIRECT_AUTHORIZATION with legal shape/action: only
                // ck_ivr_decision_source can fire (shape and direct_reason stay legal).
                expectSqlFailure(connection, "23514", "ck_ivr_decision_source",
                        v005DecisionProbeSql(),
                        decisionProbeWrongDirectDomain(sessionId, callId, fingerprint, sha));
                // Plan 6.3 item 3: exact 23505 + uq_ivr_decision_source, isolated because
                // frozen ck_ivr_decision_source and uq_ivr_decision_call_purpose otherwise
                // preempt the partial source unique.
                expectDecisionSourceUniqueViolationWithIsolatedConstraint(
                        connection, sessionId, callId, fingerprint, sha);

                // Owner append-only P0001 for UPDATE and DELETE; runtime has no UPDATE grant.
                switchLocalRole(connection, "ivr_owner");
                expectSqlFailure(connection, "P0001", "IVR_DECISION_APPEND_ONLY",
                        "UPDATE ivr.ivr_decision SET action = 'REJECT' WHERE id = ?",
                        inboundDecisionId);
                expectSqlFailure(connection, "P0001", "IVR_DECISION_APPEND_ONLY",
                        "DELETE FROM ivr.ivr_decision WHERE id = ?", inboundDecisionId);
                switchLocalRole(connection, "ivr_role");
                expectSqlFailure(connection, "42501", null,
                        "UPDATE ivr.ivr_decision SET action = 'REJECT' WHERE id = ?",
                        inboundDecisionId);
                expectSqlFailure(connection, "42501", null,
                        "DELETE FROM ivr.ivr_decision WHERE id = ?", inboundDecisionId);

                // One isolated 23514 + exact-name probe per named check (7 + 10 + 11).
                List<String> legProbed = new ArrayList<>();
                List<CheckProbe> legProbes = List.of(
                        checkProbe("ck_call_leg_leg_id", v005LegProbeSql(),
                                mutate(v005ValidLegParams(UUID.randomUUID(), callId, "  "), 2, "  ")),
                        // ck_call_leg_type is subsumed by ck_call_leg_direction under PG16;
                        // the loop below isolates it via expectCallLegTypeViolationWithIsolatedConstraint.
                        checkProbe("ck_call_leg_type", v005LegProbeSql(),
                                mutate(v005ValidLegParams(UUID.randomUUID(), callId,
                                        "leg-x-" + UUID.randomUUID()), 3, "BOGUS")),
                        checkProbe("ck_call_leg_direction", v005LegProbeSql(),
                                mutate(v005ValidLegParams(UUID.randomUUID(), callId,
                                        "leg-x-" + UUID.randomUUID()), 4, "OUTBOUND")),
                        checkProbe("ck_call_leg_state", v005LegProbeSql(),
                                mutate(v005ValidLegParams(UUID.randomUUID(), callId,
                                        "leg-x-" + UUID.randomUUID()), 5, "BOGUS")),
                        checkProbe("ck_call_leg_time_order", v005LegProbeSql(),
                                legProbeBadTimeOrder(callId)),
                        checkProbe("ck_call_leg_end_reason", v005LegProbeSql(),
                                legProbeBadEndReason(callId)),
                        checkProbe("ck_call_leg_revision", v005LegProbeSql(),
                                mutate(v005ValidLegParams(UUID.randomUUID(), callId,
                                        "leg-x-" + UUID.randomUUID()), 10, 0)));
                for (CheckProbe probe : legProbes) {
                    if ("ck_call_leg_type".equals(probe.constraint())) {
                        expectCallLegTypeViolationWithIsolatedConstraint(connection, probe);
                    } else {
                        expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                                probe.parameters());
                    }
                    legProbed.add(probe.constraint());
                }
                assertThat(legProbed)
                        .as("all 7 call_leg named checks have an isolated probe")
                        .containsExactlyInAnyOrderElementsOf(v005ExpectedCheckNames("call_leg"));
                assertThat(legProbed).hasSize(7);

                List<String> eventProbed = new ArrayList<>();
                List<CheckProbe> eventProbes = List.of(
                        checkProbe("ck_call_event_domain", v005EventProbeSql(),
                                eventProbeBadDomain(callId)),
                        checkProbe("ck_call_event_event_id", v005EventProbeSql(),
                                mutate(v005ValidEventParams(UUID.randomUUID(), callId, "  ", "88",
                                        randomSha256()), 3, "  ")),
                        checkProbe("ck_call_event_sequence", v005EventProbeSql(),
                                mutate(v005ValidEventParams(UUID.randomUUID(), callId,
                                        v005EventId(), "0", randomSha256()), 4, 0)),
                        checkProbe("ck_call_event_type", v005EventProbeSql(),
                                mutate(v005ValidEventParams(UUID.randomUUID(), callId,
                                        v005EventId(), "89", randomSha256()), 5, "  ")),
                        checkProbe("ck_call_event_payload_ciphertext", v005EventProbeSql(),
                                mutate(v005ValidEventParams(UUID.randomUUID(), callId,
                                        v005EventId(), "90", randomSha256()), 7, "plain")),
                        checkProbe("ck_call_event_payload_redacted", v005EventProbeSql(),
                                eventProbeRedactedMismatch(callId)),
                        checkProbe("ck_call_event_payload_sha", v005EventProbeSql(),
                                eventProbeBadSha(callId)),
                        checkProbe("ck_call_event_processing_status", v005EventProbeSql(),
                                mutate(v005ValidEventParams(UUID.randomUUID(), callId,
                                        v005EventId(), "91", randomSha256()), 10, "BOGUS")),
                        checkProbe("ck_call_event_reason", v005EventProbeSql(),
                                mutate(v005ValidEventParams(UUID.randomUUID(), callId,
                                        v005EventId(), "92", randomSha256()), 11, "  ")),
                        checkProbe("ck_call_event_revision", v005EventProbeSql(),
                                mutate(v005ValidEventParams(UUID.randomUUID(), callId,
                                        v005EventId(), "93", randomSha256()), 12, 0)));
                for (CheckProbe probe : eventProbes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                            probe.parameters());
                    eventProbed.add(probe.constraint());
                }
                assertThat(eventProbed)
                        .as("all 10 call_event named checks have an isolated probe")
                        .containsExactlyInAnyOrderElementsOf(v005ExpectedCheckNames("call_event"));
                assertThat(eventProbed).hasSize(10);

                List<String> decisionProbed = new ArrayList<>();
                List<CheckProbe> decisionProbes = List.of(
                        checkProbe("ck_ivr_decision_purpose", v005DecisionProbeSql(),
                                mutate(v005ValidDecisionParams(UUID.randomUUID(), sessionId,
                                        callId, fingerprint, sha), 3, "BOGUS")),
                        checkProbe("ck_ivr_decision_source", v005DecisionProbeSql(),
                                decisionProbeSourceNullDomain(sessionId, callId, fingerprint, sha)),
                        checkProbe("ck_ivr_decision_fingerprint", v005DecisionProbeSql(),
                                mutate(v005ValidDecisionParams(UUID.randomUUID(), sessionId,
                                        callId, "not-a-fingerprint", sha), 6, "not-a-fingerprint")),
                        checkProbe("ck_ivr_decision_action", v005DecisionProbeSql(),
                                mutate(v005ValidDecisionParams(UUID.randomUUID(), sessionId,
                                        callId, fingerprint, sha), 7, "BOGUS_ACTION")),
                        checkProbe("ck_ivr_decision_reason", v005DecisionProbeSql(),
                                mutate(v005ValidDecisionParams(UUID.randomUUID(), sessionId,
                                        callId, fingerprint, sha), 8, "BOGUS_REASON")),
                        checkProbe("ck_ivr_decision_direct_reason", v005DecisionProbeSql(),
                                decisionProbeDirectReason(sessionId, callId, fingerprint, sha)),
                        checkProbe("ck_ivr_decision_shape", v005DecisionProbeSql(),
                                decisionProbeShape(sessionId, callId, fingerprint, sha)),
                        checkProbe("ck_ivr_decision_snapshot", v005DecisionProbeSql(),
                                mutate(v005ValidDecisionParams(UUID.randomUUID(), sessionId,
                                        callId, fingerprint, sha), 9, "[]")),
                        checkProbe("ck_ivr_decision_payload", v005DecisionProbeSql(),
                                mutate(v005ValidDecisionParams(UUID.randomUUID(), sessionId,
                                        callId, fingerprint, sha), 10, "plain")),
                        checkProbe("ck_ivr_decision_sha", v005DecisionProbeSql(),
                                mutate(v005ValidDecisionParams(UUID.randomUUID(), sessionId,
                                        callId, fingerprint, "not-a-sha"), 11, "not-a-sha")),
                        checkProbe("ck_ivr_decision_key_version", v005DecisionProbeSql(),
                                mutate(v005ValidDecisionParams(UUID.randomUUID(), sessionId,
                                        callId, fingerprint, sha), 12, 0)));
                for (CheckProbe probe : decisionProbes) {
                    expectSqlFailure(connection, "23514", probe.constraint(), probe.sql(),
                            probe.parameters());
                    decisionProbed.add(probe.constraint());
                }
                assertThat(decisionProbed)
                        .as("all 11 ivr_decision named checks have an isolated probe")
                        .containsExactlyInAnyOrderElementsOf(
                                v005ExpectedCheckNames("ivr_decision"));
                assertThat(decisionProbed).hasSize(11);

                connection.rollback();
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM ivr.call_leg WHERE id IN (?, ?, ?)",
                    callerLegId, calleeLegId, duplicateLegId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM ivr.call_event WHERE id IN (?, ?, ?, ?)",
                    eventId, duplicateDomainEventId, duplicateSequenceEventId, otherCallEventId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM ivr.ivr_decision WHERE id IN (?, ?, ?, ?, ?, ?, ?)",
                    inboundDecisionId, activationDecisionId, enterCalleeDecisionId,
                    dialDecisionId, directSuccessDecisionId, directRefusalDecisionId,
                    duplicatePurposeDecisionId)).isZero();
        }
    }

    private static Object[] sessionProbeBadTimeOrder(UUID subjectId, String hmac) {
        // Legal CONFIRMED shape (trusted_caller + confirmed_subject_id) so only
        // ck_inbound_session_time_order fires: confirmed_at before created_at.
        Object[] params = v005ValidSessionParams(UUID.randomUUID(), v005CallId(), subjectId, hmac);
        OffsetDateTime created = OffsetDateTime.now(ZoneOffset.UTC);
        params[11] = "CONFIRMED";
        params[12] = subjectId;
        params[14] = created;
        params[15] = created.minusSeconds(60);
        return params;
    }

    private static Object[] legProbeBadTimeOrder(String callId) {
        Object[] params = v005ValidLegParams(UUID.randomUUID(), callId,
                "leg-x-" + UUID.randomUUID());
        OffsetDateTime started = OffsetDateTime.now(ZoneOffset.UTC);
        params[6] = started;
        params[7] = started.minusSeconds(5);
        return params;
    }

    private static Object[] legProbeBadEndReason(String callId) {
        Object[] params = v005ValidLegParams(UUID.randomUUID(), callId,
                "leg-x-" + UUID.randomUUID());
        params[5] = "ENDED";
        params[6] = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(10);
        params[8] = OffsetDateTime.now(ZoneOffset.UTC);
        params[9] = "  ";
        return params;
    }

    private static Object[] eventProbeBadDomain(String callId) {
        Object[] params = v005ValidEventParams(UUID.randomUUID(), callId, v005EventId(), "80",
                randomSha256());
        params[2] = "OTHER_DOMAIN";
        return params;
    }

    private static Object[] eventProbeRedactedMismatch(String callId) {
        // receipt.payloadSha256 deliberately != payload_sha256 column: only the redacted
        // shape CHECK fails (payload_sha regex still holds on the column value).
        Object[] params = v005ValidEventParams(UUID.randomUUID(), callId, v005EventId(), "81",
                randomSha256());
        params[8] = v005EventRedactedJson(randomSha256(), randomSha256());
        return params;
    }

    private static Object[] eventProbeBadSha(String callId) {
        // payload_sha256 is not hex while receipt.payloadSha256 mirrors it so only
        // ck_call_event_payload_sha fails.
        String badSha = "not-a-sha";
        Object[] params = v005ValidEventParams(UUID.randomUUID(), callId, v005EventId(), "82",
                badSha);
        params[8] = v005EventRedactedJson(badSha, randomSha256());
        params[9] = badSha;
        return params;
    }

    private static Object[] decisionProbeSourceNullDomain(
            UUID sessionId, String callId, String fingerprint, String sha) {
        // ACTIVATION_NEXT_ACTION requires nonNULL IVR_CONFIRM domain; shape/action stay legal.
        Object[] params = v005ValidDecisionParams(UUID.randomUUID(), sessionId, callId,
                fingerprint, sha);
        params[3] = "ACTIVATION_NEXT_ACTION";
        params[4] = null;
        params[5] = "evt-activation-" + UUID.randomUUID();
        return params;
    }

    private static Object[] decisionProbeWrongDirectDomain(
            UUID sessionId, String callId, String fingerprint, String sha) {
        // DIRECT_AUTHORIZATION with wrong domain and legal REJECT action: only
        // ck_ivr_decision_source can fail (shape allows REJECT without reservation).
        Object[] params = v005ValidDecisionParams(UUID.randomUUID(), sessionId, callId,
                fingerprint, sha);
        params[3] = "DIRECT_AUTHORIZATION";
        params[4] = "IVR_CONFIRM";
        params[5] = "evt-direct-wrong-domain-" + UUID.randomUUID();
        params[7] = "REJECT";
        params[8] = "CALL_IN_PROGRESS";
        return params;
    }

    private static Object[] decisionProbeDirectReason(
            UUID sessionId, String callId, String fingerprint, String sha) {
        // DIRECT_AUTHORIZATION action NULL + non-DIRECT reason violates only direct_reason
        // (reservation nonNULL keeps shape legal).
        Object[] params = v005ValidDecisionParams(UUID.randomUUID(), sessionId, callId,
                fingerprint, sha);
        params[3] = "DIRECT_AUTHORIZATION";
        params[4] = "IVR_DIRECT_AUTH";
        params[5] = "evt-direct-" + UUID.randomUUID();
        params[7] = null;
        params[8] = "CALL_IN_PROGRESS";
        params[13] = UUID.randomUUID();
        return params;
    }

    private static Object[] decisionProbeShape(
            UUID sessionId, String callId, String fingerprint, String sha) {
        // INBOUND_RESOLVE + action NULL violates shape only (direct_reason stays legal
        // because reason is DIRECT_AUTHORIZED, matching action IS NULL).
        Object[] params = v005ValidDecisionParams(UUID.randomUUID(), sessionId, callId,
                fingerprint, sha);
        params[7] = null;
        params[8] = "DIRECT_AUTHORIZED";
        return params;
    }

    private static void assertV005ForeignKeyAndDeferredReservationFkBackstops() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID callRecordId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        String hmac = randomSha256();
        String fingerprint = randomSha256();
        String callId = v005CallId();

        // Delete-fixture ids kept method-scoped for post-rollback zero checks.
        UUID delBindingId = UUID.randomUUID();
        UUID delBindingSubjectId = UUID.randomUUID();
        UUID delBindingAccountId = UUID.randomUUID();
        UUID delBindingTaskId = UUID.randomUUID();
        UUID delAccountChild = UUID.randomUUID();
        UUID otherAccount = UUID.randomUUID();
        UUID sharedSubject = UUID.randomUUID();
        UUID sharedBinding = UUID.randomUUID();
        UUID delTask = UUID.randomUUID();
        UUID delOrg = UUID.randomUUID();
        UUID orgTask = UUID.randomUUID();
        UUID delSubjectAccount = UUID.randomUUID();
        UUID delSubjectOnly = UUID.randomUUID();
        UUID delSubjectSession = UUID.randomUUID();
        UUID delSessionOnly = UUID.randomUUID();
        UUID delSessionCall = UUID.randomUUID();
        UUID delTaskOnly = UUID.randomUUID();
        UUID delTaskSession = UUID.randomUUID();
        UUID delTaskCall = UUID.randomUUID();
        UUID delLegSession = UUID.randomUUID();
        UUID delLegCall = UUID.randomUUID();
        UUID delLeg = UUID.randomUUID();
        UUID claimedTask = UUID.randomUUID();
        UUID claimedSession = UUID.randomUUID();
        UUID claimedCall = UUID.randomUUID();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                v005InsertParents(connection, accountId, organizationId, subjectId, bindingId, hmac);
                setLocalRole(connection, "ivr_role");
                assertThat(v005InsertSession(connection, sessionId, callId, subjectId, hmac))
                        .isEqualTo(1);
                assertThat(v005InsertCall(connection, callRecordId, callId, sessionId, subjectId,
                        hmac)).isEqualTo(1);
                assertThat(v005InsertTask(connection, taskId, v005TaskIdText(), bindingId,
                        subjectId, accountId, hmac, v005IdempotencyKey(), fingerprint))
                        .isEqualTo(1);

                // 15 missing-parent 23503 probes with exact constraint names. Every probe
                // passes all named CHECKs so only the FK can fire.
                List<String> fkProbed = new ArrayList<>();

                expectSqlFailure(connection, "23503", "fk_outbound_task_binding",
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                                + "created_by_account_id, caller_number_hmac, callee_ciphertext, "
                                + "callee_masked, business_type, expires_at, idempotency_key, "
                                + "request_fingerprint) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                                + "now() + interval '30 seconds', ?, ?)",
                        UUID.randomUUID(), v005TaskIdText(), UUID.randomUUID(), subjectId,
                        accountId, accountId, randomSha256(), v005Ciphertext(), v005Masked(),
                        v005IdempotencyKey(), fingerprint);
                fkProbed.add("fk_outbound_task_binding");

                expectSqlFailure(connection, "23503", "fk_outbound_task_subject",
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                                + "created_by_account_id, caller_number_hmac, callee_ciphertext, "
                                + "callee_masked, business_type, expires_at, idempotency_key, "
                                + "request_fingerprint) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                                + "now() + interval '30 seconds', ?, ?)",
                        UUID.randomUUID(), v005TaskIdText(), bindingId, UUID.randomUUID(),
                        accountId, accountId, randomSha256(), v005Ciphertext(), v005Masked(),
                        v005IdempotencyKey(), fingerprint);
                fkProbed.add("fk_outbound_task_subject");

                expectSqlFailure(connection, "23503", "fk_outbound_task_owner_account",
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                                + "created_by_account_id, caller_number_hmac, callee_ciphertext, "
                                + "callee_masked, business_type, expires_at, idempotency_key, "
                                + "request_fingerprint) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                                + "now() + interval '30 seconds', ?, ?)",
                        UUID.randomUUID(), v005TaskIdText(), bindingId, subjectId,
                        UUID.randomUUID(), accountId, randomSha256(), v005Ciphertext(), v005Masked(),
                        v005IdempotencyKey(), fingerprint);
                fkProbed.add("fk_outbound_task_owner_account");

                expectSqlFailure(connection, "23503", "fk_outbound_task_owner_org",
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, "
                                + "owner_organization_id, created_by_account_id, "
                                + "caller_number_hmac, callee_ciphertext, callee_masked, "
                                + "business_type, expires_at, idempotency_key, "
                                + "request_fingerprint) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                                + "now() + interval '30 seconds', ?, ?)",
                        UUID.randomUUID(), v005TaskIdText(), bindingId, subjectId,
                        UUID.randomUUID(), accountId, randomSha256(), v005Ciphertext(), v005Masked(),
                        v005IdempotencyKey(), fingerprint);
                fkProbed.add("fk_outbound_task_owner_org");

                expectSqlFailure(connection, "23503", "fk_outbound_task_creator",
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                                + "created_by_account_id, caller_number_hmac, callee_ciphertext, "
                                + "callee_masked, business_type, expires_at, idempotency_key, "
                                + "request_fingerprint) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                                + "now() + interval '30 seconds', ?, ?)",
                        UUID.randomUUID(), v005TaskIdText(), bindingId, subjectId, accountId,
                        UUID.randomUUID(), randomSha256(), v005Ciphertext(), v005Masked(),
                        v005IdempotencyKey(), fingerprint);
                fkProbed.add("fk_outbound_task_creator");

                expectSqlFailure(connection, "23503", "fk_outbound_task_claimed_call",
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                                + "created_by_account_id, caller_number_hmac, callee_ciphertext, "
                                + "callee_masked, business_type, status, ttl_seconds, expires_at, "
                                + "idempotency_key, request_fingerprint, claimed_call_id, "
                                + "claimed_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', 'CLAIMED', "
                                + "30, now() + interval '30 seconds', ?, ?, ?, now())",
                        UUID.randomUUID(), v005TaskIdText(), bindingId, subjectId, accountId,
                        accountId, randomSha256(), v005Ciphertext(), v005Masked(), v005IdempotencyKey(),
                        fingerprint, "missing-call-" + UUID.randomUUID());
                fkProbed.add("fk_outbound_task_claimed_call");

                expectSqlFailure(connection, "23503", "fk_inbound_session_subject",
                        "INSERT INTO ivr.inbound_session "
                                + "(id, call_id, subject_id, trusted_caller, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, prompt_version) "
                                + "VALUES (?, ?, ?, TRUE, ?, ?, ?, ?, 'node-1', '1.0', 'p1')",
                        UUID.randomUUID(), v005CallId(), UUID.randomUUID(), v005Ciphertext(),
                        randomSha256(), v005Masked(), v005Masked());
                fkProbed.add("fk_inbound_session_subject");

                expectSqlFailure(connection, "23503", "fk_inbound_session_confirmed_subject",
                        "INSERT INTO ivr.inbound_session "
                                + "(id, call_id, subject_id, trusted_caller, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, prompt_version, status, "
                                + "confirmed_subject_id, confirmed_at) "
                                + "VALUES (?, ?, ?, TRUE, ?, ?, ?, ?, 'node-1', '1.0', 'p1', "
                                + "'CONFIRMED', ?, now())",
                        UUID.randomUUID(), v005CallId(), subjectId, v005Ciphertext(), randomSha256(),
                        v005Masked(), v005Masked(), UUID.randomUUID());
                fkProbed.add("fk_inbound_session_confirmed_subject");

                expectSqlFailure(connection, "23503", "fk_call_record_session",
                        "INSERT INTO ivr.call_record "
                                + "(id, call_id, session_id, subject_id, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, started_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'node-1', '1.0', now())",
                        UUID.randomUUID(), v005CallId(), UUID.randomUUID(), subjectId,
                        v005Ciphertext(), randomSha256(), v005Masked(), v005Masked());
                fkProbed.add("fk_call_record_session");

                // Dedicated empty session: the fixture session already owns callRecordId and
                // uq_call_record_session would fire before fk_call_record_task.
                UUID fkTaskProbeSession = UUID.randomUUID();
                assertThat(v005InsertSession(connection, fkTaskProbeSession, v005CallId(),
                        subjectId, randomSha256())).isEqualTo(1);
                expectSqlFailure(connection, "23503", "fk_call_record_task",
                        "INSERT INTO ivr.call_record "
                                + "(id, call_id, session_id, task_id, subject_id, "
                                + "caller_ciphertext, caller_number_hmac, caller_masked, "
                                + "access_number_masked, node_id, protocol_version, started_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'node-1', '1.0', now())",
                        UUID.randomUUID(), v005CallId(), fkTaskProbeSession, UUID.randomUUID(),
                        subjectId, v005Ciphertext(), randomSha256(), v005Masked(), v005Masked());
                fkProbed.add("fk_call_record_task");

                // Dedicated empty session: uq_call_record_session would otherwise fire first.
                UUID fkSubjectProbeSession = UUID.randomUUID();
                assertThat(v005InsertSession(connection, fkSubjectProbeSession, v005CallId(),
                        subjectId, randomSha256())).isEqualTo(1);
                expectSqlFailure(connection, "23503", "fk_call_record_subject",
                        "INSERT INTO ivr.call_record "
                                + "(id, call_id, session_id, subject_id, caller_ciphertext, "
                                + "caller_number_hmac, caller_masked, access_number_masked, "
                                + "node_id, protocol_version, started_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'node-1', '1.0', now())",
                        UUID.randomUUID(), v005CallId(), fkSubjectProbeSession, UUID.randomUUID(),
                        v005Ciphertext(), randomSha256(), v005Masked(), v005Masked());
                fkProbed.add("fk_call_record_subject");

                expectSqlFailure(connection, "23503", "fk_call_leg_call",
                        "INSERT INTO ivr.call_leg "
                                + "(id, call_id, leg_id, leg_type, direction, state, started_at) "
                                + "VALUES (?, ?, 'leg-1', 'CALLER', 'INBOUND', 'RINGING', now())",
                        UUID.randomUUID(), "missing-call-" + UUID.randomUUID());
                fkProbed.add("fk_call_leg_call");

                // Matching sha keeps the payload_redacted CHECK legal so only the FK fires.
                String fkEventSha = randomSha256();
                expectSqlFailure(connection, "23503", "fk_call_event_call",
                        "INSERT INTO ivr.call_event "
                                + "(id, call_id, event_id, sequence_no, event_type, occurred_at, "
                                + "payload_ciphertext, payload_redacted, payload_sha256) "
                                + "VALUES (?, ?, ?, 1, 'SESSION_START', now(), ?, ?::jsonb, ?)",
                        UUID.randomUUID(), "missing-call-" + UUID.randomUUID(),
                        v005EventId(), v005Ciphertext(),
                        v005EventRedactedJson(fkEventSha, randomSha256()), fkEventSha);
                fkProbed.add("fk_call_event_call");

                // Existing call_id, only session_id missing: exactly fk_ivr_decision_session
                // can fire. The separate probe below keeps the existing session and a
                // nonexistent call for fk_ivr_decision_call.
                expectSqlFailure(connection, "23503", "fk_ivr_decision_session",
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, request_fingerprint, "
                                + "action, reason_code, response_snapshot, "
                                + "response_payload_ciphertext, response_sha256, key_version) "
                                + "VALUES (?, ?, ?, 'INBOUND_RESOLVE', ?, 'GUIDE_ACTIVATION', "
                                + "'ACTIVATION_REQUIRED', '{}'::jsonb, ?, ?, 1)",
                        UUID.randomUUID(), UUID.randomUUID(), callId, fingerprint,
                        v005Ciphertext(), randomSha256());
                fkProbed.add("fk_ivr_decision_session");

                expectSqlFailure(connection, "23503", "fk_ivr_decision_call",
                        "INSERT INTO ivr.ivr_decision "
                                + "(id, session_id, call_id, purpose, request_fingerprint, "
                                + "action, reason_code, response_snapshot, "
                                + "response_payload_ciphertext, response_sha256, key_version) "
                                + "VALUES (?, ?, ?, 'INBOUND_RESOLVE', ?, 'GUIDE_ACTIVATION', "
                                + "'ACTIVATION_REQUIRED', '{}'::jsonb, ?, ?, 1)",
                        UUID.randomUUID(), sessionId, "missing-call-" + UUID.randomUUID(),
                        fingerprint, v005Ciphertext(), randomSha256());
                fkProbed.add("fk_ivr_decision_call");

                assertThat(fkProbed)
                        .as("all 15 V005 foreign keys have an isolated missing-parent probe")
                        .containsExactlyInAnyOrderElementsOf(v005ExpectedFkNames());
                assertThat(fkProbed).hasSize(15);

                // Whole FK map recheck against the frozen V005_FK_DEFS inventory.
                List<String> fkRows = new ArrayList<>();
                try (PreparedStatement prepared = connection.prepareStatement(
                        "SELECT c.conname || '|' || pg_get_constraintdef(c.oid) || '|' "
                                + "|| CASE WHEN c.confdeltype = 'r' THEN 'rrs' ELSE "
                                + "c.confdeltype::text END "
                                + "|| '|' || c.condeferrable || '|' || c.condeferred || '|' "
                                + "|| c.convalidated "
                                + "FROM pg_constraint c "
                                + "JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = 'ivr' AND c.contype = 'f' "
                                + "ORDER BY c.conname")) {
                    try (ResultSet resultSet = prepared.executeQuery()) {
                        while (resultSet.next()) {
                            fkRows.add(resultSet.getString(1));
                        }
                    }
                }
                assertThat(fkRows).containsExactlyInAnyOrderElementsOf(V005_FK_DEFS);
                assertThat(V005_FK_DEFS).hasSize(15);

                // Reservation noFK and no entitlement/activation tables.
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM pg_constraint c "
                                + "JOIN pg_class r ON r.oid = c.conrelid "
                                + "JOIN pg_namespace n ON n.oid = r.relnamespace "
                                + "WHERE n.nspname = 'ivr' AND c.contype = 'f' "
                                + "AND pg_get_constraintdef(c.oid) LIKE '%reservation%'"))
                        .as("no reservation FK exists in V005").isZero();
                // Schema-level zero-table assertion (pg_class/pg_namespace), consistent with
                // assertNoV006PlusObjects: the entitlement_metering/activation_consent tables
                // would live in their own schemas, never inside 'ivr', so a table_schema='ivr'
                // filter would prove nothing.
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM pg_class c "
                                + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                                + "WHERE n.nspname IN ('entitlement_metering','activation_consent') "
                                + "AND c.relkind = 'r'"))
                        .as("no entitlement/activation tables in V005 (schema-level)")
                        .isZero();

                // Representative unambiguous parent-delete RESTRICT probes. Each fixture
                // attaches exactly one child FK so the reported constraint is exact.
                switchLocalRole(connection, "customer_iam_owner");
                v005InsertAccountAsOwner(connection, delBindingAccountId);
                switchLocalRole(connection, "subject_number_owner");
                v005InsertSubjectAsOwner(connection, delBindingSubjectId, delBindingAccountId);
                switchLocalRole(connection, "subject_number_role");
                assertThat(executeUpdate(connection,
                        "INSERT INTO subject_number.caller_number_binding "
                                + "(id, subject_id, number_hmac, number_ciphertext, number_masked, "
                                + "number_type, status, verified_at) "
                                + "VALUES (?, ?, ?, ?, ?, 'MOBILE', 'ACTIVE', now())",
                        delBindingId, delBindingSubjectId, randomSha256(), v005Ciphertext(),
                        v005Masked())).isEqualTo(1);
                resetToMigrationIdentity(connection);
                assertThat(v005InsertTask(connection, delBindingTaskId, v005TaskIdText(),
                        delBindingId, delBindingSubjectId, delBindingAccountId, randomSha256(),
                        v005IdempotencyKey(), fingerprint)).isEqualTo(1);
                expectSqlFailure(connection, "23503", "fk_outbound_task_binding",
                        "DELETE FROM subject_number.caller_number_binding WHERE id = ?",
                        delBindingId);

                // owner_account RESTRICT: only task.owner_account references delAccountChild.
                switchLocalRole(connection, "customer_iam_owner");
                v005InsertAccountAsOwner(connection, delAccountChild);
                v005InsertAccountAsOwner(connection, otherAccount);
                switchLocalRole(connection, "subject_number_owner");
                v005InsertSubjectAsOwner(connection, sharedSubject, otherAccount);
                switchLocalRole(connection, "subject_number_role");
                assertThat(executeUpdate(connection,
                        "INSERT INTO subject_number.caller_number_binding "
                                + "(id, subject_id, number_hmac, number_ciphertext, number_masked, "
                                + "number_type, status, verified_at) "
                                + "VALUES (?, ?, ?, ?, ?, 'MOBILE', 'ACTIVE', now())",
                        sharedBinding, sharedSubject, randomSha256(), v005Ciphertext(),
                        v005Masked())).isEqualTo(1);
                resetToMigrationIdentity(connection);
                setLocalRole(connection, "ivr_role");
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, owner_account_id, "
                                + "created_by_account_id, caller_number_hmac, callee_ciphertext, "
                                + "callee_masked, business_type, expires_at, idempotency_key, "
                                + "request_fingerprint) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                                + "now() + interval '30 seconds', ?, ?)",
                        delTask, v005TaskIdText(), sharedBinding, sharedSubject, delAccountChild,
                        otherAccount, randomSha256(), v005Ciphertext(), v005Masked(), v005IdempotencyKey(),
                        fingerprint)).isEqualTo(1);
                switchLocalRole(connection, "customer_iam_owner");
                expectSqlFailure(connection, "23503", "fk_outbound_task_owner_account",
                        "DELETE FROM customer_iam.customer_account WHERE id = ?", delAccountChild);

                // owner_org RESTRICT: task inserted directly with owner_organization_id (owner
                // columns are guard-immutable, so UPDATE cannot be used to attach the org).
                switchLocalRole(connection, "organization_access_owner");
                assertThat(executeUpdate(connection,
                        "INSERT INTO organization_access.organization "
                                + "(id, display_name, status) VALUES (?, ?, 'ACTIVE')",
                        delOrg, "pa5-s2-delorg-" + UUID.randomUUID())).isEqualTo(1);
                resetToMigrationIdentity(connection);
                setLocalRole(connection, "ivr_role");
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.outbound_call_task "
                                + "(id, task_id, caller_binding_id, subject_id, "
                                + "owner_organization_id, created_by_account_id, "
                                + "caller_number_hmac, callee_ciphertext, callee_masked, "
                                + "business_type, expires_at, idempotency_key, "
                                + "request_fingerprint) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'EVIDENCE_CALL', "
                                + "now() + interval '30 seconds', ?, ?)",
                        orgTask, v005TaskIdText(), bindingId, subjectId, delOrg, accountId,
                        randomSha256(), v005Ciphertext(), v005Masked(), v005IdempotencyKey(), fingerprint))
                        .isEqualTo(1);
                switchLocalRole(connection, "organization_access_owner");
                expectSqlFailure(connection, "23503", "fk_outbound_task_owner_org",
                        "DELETE FROM organization_access.organization WHERE id = ?", delOrg);

                // subject RESTRICT: session is the only child of a dedicated subject.
                switchLocalRole(connection, "customer_iam_owner");
                v005InsertAccountAsOwner(connection, delSubjectAccount);
                switchLocalRole(connection, "subject_number_owner");
                v005InsertSubjectAsOwner(connection, delSubjectOnly, delSubjectAccount);
                resetToMigrationIdentity(connection);
                setLocalRole(connection, "ivr_role");
                assertThat(v005InsertSession(connection, delSubjectSession, v005CallId(),
                        delSubjectOnly, randomSha256())).isEqualTo(1);
                switchLocalRole(connection, "subject_number_owner");
                expectSqlFailure(connection, "23503", "fk_inbound_session_subject",
                        "DELETE FROM subject_number.business_subject WHERE id = ?",
                        delSubjectOnly);

                // session RESTRICT: call is the only child of a dedicated session.
                setLocalRole(connection, "ivr_role");
                String delSessionCallId = v005CallId();
                assertThat(v005InsertSession(connection, delSessionOnly, delSessionCallId,
                        subjectId, randomSha256())).isEqualTo(1);
                assertThat(v005InsertCall(connection, delSessionCall, delSessionCallId,
                        delSessionOnly, subjectId, randomSha256())).isEqualTo(1);
                switchLocalRole(connection, "ivr_owner");
                expectSqlFailure(connection, "23503", "fk_call_record_session",
                        "DELETE FROM ivr.inbound_session WHERE id = ?", delSessionOnly);

                // task RESTRICT via call.task_id. The call_record guard requires the task
                // to already claim this call (matching claimed_call_id) before task_id
                // can be set; use the legal two-step link, then delete the task.
                setLocalRole(connection, "ivr_role");
                assertThat(v005InsertTask(connection, delTaskOnly, v005TaskIdText(), bindingId,
                        subjectId, accountId, randomSha256(), v005IdempotencyKey(), fingerprint))
                        .isEqualTo(1);
                String delTaskCallId = v005CallId();
                assertThat(v005InsertSession(connection, delTaskSession, delTaskCallId,
                        subjectId, randomSha256())).isEqualTo(1);
                assertThat(v005InsertCall(connection, delTaskCall, delTaskCallId, delTaskSession,
                        subjectId, randomSha256())).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.outbound_call_task "
                                + "SET claimed_call_id = ?, claimed_at = now(), status = 'CLAIMED', "
                                + "updated_at = now(), revision = revision + 1 "
                                + "WHERE id = ? AND status = 'PENDING' AND claimed_call_id IS NULL",
                        delTaskCallId, delTaskOnly)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.call_record SET task_id = ?, revision = revision + 1 "
                                + "WHERE id = ? AND task_id IS NULL",
                        delTaskOnly, delTaskCall)).isEqualTo(1);
                switchLocalRole(connection, "ivr_owner");
                expectSqlFailure(connection, "23503", "fk_call_record_task",
                        "DELETE FROM ivr.outbound_call_task WHERE id = ?", delTaskOnly);

                // leg RESTRICT: delete the parent call that only a leg references.
                setLocalRole(connection, "ivr_role");
                String delLegCallId = v005CallId();
                assertThat(v005InsertSession(connection, delLegSession, delLegCallId, subjectId,
                        randomSha256())).isEqualTo(1);
                assertThat(v005InsertCall(connection, delLegCall, delLegCallId, delLegSession,
                        subjectId, randomSha256())).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "INSERT INTO ivr.call_leg "
                                + "(id, call_id, leg_id, leg_type, direction, state, started_at) "
                                + "VALUES (?, ?, 'leg-del', 'CALLER', 'INBOUND', 'RINGING', now())",
                        delLeg, delLegCallId)).isEqualTo(1);
                switchLocalRole(connection, "ivr_owner");
                expectSqlFailure(connection, "23503", "fk_call_leg_call",
                        "DELETE FROM ivr.call_record WHERE id = ?", delLegCall);

                // claimed_call RESTRICT: legal claim shape then delete the claimed call.
                setLocalRole(connection, "ivr_role");
                assertThat(v005InsertTask(connection, claimedTask, v005TaskIdText(), bindingId,
                        subjectId, accountId, randomSha256(), v005IdempotencyKey(), fingerprint))
                        .isEqualTo(1);
                String claimedCallText = v005CallId();
                assertThat(v005InsertSession(connection, claimedSession, claimedCallText,
                        subjectId, randomSha256())).isEqualTo(1);
                assertThat(v005InsertCall(connection, claimedCall, claimedCallText,
                        claimedSession, subjectId, randomSha256())).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.outbound_call_task "
                                + "SET claimed_call_id = ?, claimed_at = now(), status = 'CLAIMED', "
                                + "updated_at = now(), revision = revision + 1 "
                                + "WHERE id = ? AND status = 'PENDING' AND claimed_call_id IS NULL",
                        claimedCallText, claimedTask)).isEqualTo(1);
                switchLocalRole(connection, "ivr_owner");
                expectSqlFailure(connection, "23503", "fk_outbound_task_claimed_call",
                        "DELETE FROM ivr.call_record WHERE id = ?", claimedCall);

                connection.rollback();
            } finally {
                connection.rollback();
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM ivr.outbound_call_task "
                            + "WHERE id IN (?, ?, ?, ?, ?, ?)",
                    taskId, delBindingTaskId, delTask, orgTask, delTaskOnly, claimedTask))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM ivr.inbound_session WHERE id IN (?, ?, ?, ?)",
                    sessionId, delSubjectSession, delSessionOnly, delTaskSession)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM ivr.call_record WHERE id IN (?, ?, ?, ?, ?)",
                    callRecordId, delSessionCall, delTaskCall, delLegCall, claimedCall))
                    .isZero();
        }
    }

    private static List<String> v005ExpectedFkNames() {
        List<String> names = new ArrayList<>();
        for (String row : V005_FK_DEFS) {
            names.add(row.substring(0, row.indexOf('|')));
        }
        return names;
    }

    private static void v005InsertAccountAsOwner(Connection connection, UUID accountId)
            throws SQLException {
        assertThat(executeUpdate(connection,
                "INSERT INTO customer_iam.customer_account "
                        + "(id, display_name, status) VALUES (?, ?, 'ACTIVE')",
                accountId, "pa5-s2-acct-" + UUID.randomUUID())).isEqualTo(1);
    }

    private static void v005InsertSubjectAsOwner(
            Connection connection, UUID subjectId, UUID accountId) throws SQLException {
        assertThat(executeUpdate(connection,
                "INSERT INTO subject_number.business_subject "
                        + "(id, source, owner_account_id) VALUES (?, 'IVR', ?)",
                subjectId, accountId)).isEqualTo(1);
    }

    private static void commitV005RaceParents(
            UUID accountId, UUID organizationId, UUID subjectId, UUID bindingId, String hmac)
            throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                v005InsertParents(connection, accountId, organizationId, subjectId, bindingId, hmac);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void runConcurrentV005TaskClaimAndActiveCall() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        String hmac = randomSha256();
        String fingerprint = randomSha256();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            commitV005RaceParents(accountId, organizationId, subjectId, bindingId, hmac);

            // Distinct canonical-caller HMACs per race: a live-caller winner from an
            // earlier race would otherwise block the next task race on
            // uq_outbound_task_live_caller, and an ACTIVE call winner would block the
            // next call race on uq_call_record_active_caller.
            String liveCallerHmac = randomSha256();
            String activeCallHmac = randomSha256();
            String claimHmac = randomSha256();
            String rollbackHmac = randomSha256();

            // Race 1: two live PENDING task inserts for the same canonical caller.
            raceV005LiveCallerTaskInsert(executor, bindingId, subjectId, accountId, liveCallerHmac,
                    fingerprint);
            // Race 2: two ACTIVE/UNKNOWN call inserts for the same canonical caller.
            raceV005ActiveCallerCallInsert(executor, subjectId, activeCallHmac);
            // Race 3: two conditional claims of the same PENDING task (one winner, loser
            // zero rows).
            raceV005ConditionalClaim(executor, bindingId, subjectId, accountId, claimHmac,
                    fingerprint);
            // Race 4: two call_event rows with the same (event_domain, event_id).
            // Each race uses a distinct caller HMAC so the committed ACTIVE call from a
            // prior race does not trip uq_call_record_active_caller on setup inserts.
            raceV005EventDomainIdUnique(executor, subjectId, randomSha256());
            // Race 5: two call_event rows with the same (call_id, sequence_no).
            raceV005EventSequenceUnique(executor, subjectId, randomSha256());
            // Race 6: two ivr_decision rows with the same (call_id, purpose).
            raceV005DecisionPurposeUnique(executor, subjectId, randomSha256());

            // Category-2: savepoint rollback clears BOTH reciprocal link assignments while
            // a pre-savepoint call row survives on the same connection.
            assertV005CategoryTwoSavepointClearsBothLinks(bindingId, subjectId, accountId,
                    rollbackHmac, fingerprint);
            // Category-3: error after both links then whole-transaction rollback leaves no
            // claim and no link.
            assertV005CategoryThreeWholeRollbackLeavesNoLink(bindingId, subjectId, accountId,
                    randomSha256(), fingerprint);
        } finally {
            executor.shutdownNow();
            cleanupV005ConcurrentFixtures(accountId, organizationId, subjectId, bindingId);
        }
    }

    private static void raceV005LiveCallerTaskInsert(
            ExecutorService executor,
            UUID bindingId,
            UUID subjectId,
            UUID accountId,
            String hmac,
            String fingerprint) throws Exception {
        UUID winnerTaskId = UUID.randomUUID();
        UUID loserTaskId = UUID.randomUUID();
        String winnerApplication = "pa5-s2-livecaller-winner";
        String loserApplication = "pa5-s2-livecaller-loser";

        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
            }
            setLocalRole(winner, "ivr_role");
            assertThat(v005InsertTask(winner, winnerTaskId, v005TaskIdText(), bindingId, subjectId,
                    accountId, hmac, v005IdempotencyKey(), fingerprint)).isEqualTo(1);

            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "ivr_role", loserApplication);
            Future<Integer> loserFuture = executor.submit(() -> v005InsertTask(loser, loserTaskId,
                    v005TaskIdText(), bindingId, subjectId, accountId, hmac,
                    v005IdempotencyKey(), fingerprint));
            awaitBlockedBackend(loserApplication);

            winner.commit();
            try {
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                fail("the live-caller unique loser must not commit a second PENDING task");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23505");
                assertThat(exception.getMessage()).contains("uq_outbound_task_live_caller");
            }
            loser.rollback();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.outbound_call_task WHERE caller_number_hmac = ? "
                            + "AND status IN ('PENDING','CLAIMED')",
                    hmac)).as("exactly one live task for the canonical caller").isEqualTo(1);
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.outbound_call_task WHERE id = ?", loserTaskId))
                    .isZero();
        }
    }

    private static void raceV005ActiveCallerCallInsert(
            ExecutorService executor, UUID subjectId, String hmac) throws Exception {
        UUID winnerSession = UUID.randomUUID();
        UUID loserSession = UUID.randomUUID();
        UUID winnerCall = UUID.randomUUID();
        UUID loserCall = UUID.randomUUID();
        String callId = v005CallId();
        String winnerApplication = "pa5-s2-activecall-winner";
        String loserApplication = "pa5-s2-activecall-loser";

        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "ivr_role");
                assertThat(v005InsertSession(setup, winnerSession, callId, subjectId, hmac))
                        .isEqualTo(1);
                assertThat(v005InsertSession(setup, loserSession, v005CallId(), subjectId, hmac))
                        .isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }

        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
            }
            setLocalRole(winner, "ivr_role");
            assertThat(v005InsertCall(winner, winnerCall, callId, winnerSession, subjectId, hmac))
                    .isEqualTo(1);

            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "ivr_role", loserApplication);
            Future<Integer> loserFuture = executor.submit(() -> v005InsertCall(loser, loserCall,
                    v005CallId(), loserSession, subjectId, hmac));
            awaitBlockedBackend(loserApplication);

            winner.commit();
            try {
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                fail("the active-caller unique loser must not commit a second ACTIVE call");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23505");
                assertThat(exception.getMessage()).contains("uq_call_record_active_caller");
            }
            loser.rollback();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.call_record WHERE caller_number_hmac = ? "
                            + "AND status IN ('ACTIVE','UNKNOWN')",
                    hmac)).as("exactly one live call for the canonical caller").isEqualTo(1);
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.call_record WHERE id = ?", loserCall)).isZero();
        }
    }

    private static void raceV005ConditionalClaim(
            ExecutorService executor,
            UUID bindingId,
            UUID subjectId,
            UUID accountId,
            String hmac,
            String fingerprint) throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID winnerCall = UUID.randomUUID();
        UUID loserCallRecordId = UUID.randomUUID();
        UUID winnerSession = UUID.randomUUID();
        UUID loserSession = UUID.randomUUID();
        String claimedCallId = v005CallId();
        String loserCallId = v005CallId();
        String winnerApplication = "pa5-s2-claim-winner";
        String loserApplication = "pa5-s2-claim-loser";

        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "ivr_role");
                assertThat(v005InsertTask(setup, taskId, v005TaskIdText(), bindingId, subjectId,
                        accountId, hmac, v005IdempotencyKey(), fingerprint)).isEqualTo(1);
                assertThat(v005InsertSession(setup, winnerSession, claimedCallId, subjectId, hmac))
                        .isEqualTo(1);
                assertThat(v005InsertCall(setup, winnerCall, claimedCallId, winnerSession,
                        subjectId, hmac)).isEqualTo(1);
                // Loser call fixture uses a distinct HMAC: the winner call is ACTIVE with
                // claimHmac, and uq_call_record_active_caller would block a second live row.
                assertThat(v005InsertSession(setup, loserSession, loserCallId, subjectId,
                        randomSha256())).isEqualTo(1);
                assertThat(v005InsertCall(setup, loserCallRecordId, loserCallId, loserSession,
                        subjectId, randomSha256())).isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }

        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
            }
            setLocalRole(winner, "ivr_role");
            assertThat(executeUpdate(winner,
                    "UPDATE ivr.outbound_call_task "
                            + "SET claimed_call_id = ?, claimed_at = now(), status = 'CLAIMED', "
                            + "updated_at = now(), revision = revision + 1 "
                            + "WHERE id = ? AND status = 'PENDING' AND claimed_call_id IS NULL "
                            + "AND revision = 1",
                    claimedCallId, taskId)).isEqualTo(1);
            assertThat(executeUpdate(winner,
                    "UPDATE ivr.call_record SET task_id = ?, revision = revision + 1 "
                            + "WHERE id = ? AND task_id IS NULL AND revision = 1",
                    taskId, winnerCall)).isEqualTo(1);

            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "ivr_role", loserApplication);
            Future<Integer> loserFuture = executor.submit(() -> executeUpdate(loser,
                    "UPDATE ivr.outbound_call_task "
                            + "SET claimed_call_id = ?, claimed_at = now(), status = 'CLAIMED', "
                            + "updated_at = now(), revision = revision + 1 "
                            + "WHERE id = ? AND status = 'PENDING' AND claimed_call_id IS NULL "
                            + "AND revision = 1",
                    loserCallId, taskId));
            awaitBlockedBackend(loserApplication);

            winner.commit();
            assertThat(awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS))
                    .as("conditional-claim loser affects zero rows after the winner commits")
                    .isZero();
            loser.rollback();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryString(verify,
                    "SELECT status || '|' || claimed_call_id || '|' || revision::text "
                            + "FROM ivr.outbound_call_task WHERE id = ?",
                    taskId)).as("one winner holds the claim").isEqualTo("CLAIMED|" + claimedCallId + "|2");
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.call_record WHERE task_id = ?", taskId))
                    .isEqualTo(1);
        }
    }

    private static void raceV005EventDomainIdUnique(
            ExecutorService executor, UUID subjectId, String hmac) throws Exception {
        UUID callRecordId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID winnerEvent = UUID.randomUUID();
        UUID loserEvent = UUID.randomUUID();
        String callId = v005CallId();
        String eventIdText = v005EventId();
        String sha = randomSha256();
        String winnerApplication = "pa5-s2-event-domain-winner";
        String loserApplication = "pa5-s2-event-domain-loser";

        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "ivr_role");
                assertThat(v005InsertSession(setup, sessionId, callId, subjectId, hmac))
                        .isEqualTo(1);
                assertThat(v005InsertCall(setup, callRecordId, callId, sessionId, subjectId, hmac))
                        .isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }

        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
            }
            setLocalRole(winner, "ivr_role");
            assertThat(executeUpdate(winner,
                    "INSERT INTO ivr.call_event "
                            + "(id, call_id, event_id, sequence_no, event_type, occurred_at, "
                            + "payload_ciphertext, payload_redacted, payload_sha256) "
                            + "VALUES (?, ?, ?, 1, 'SESSION_START', now(), ?, ?::jsonb, ?)",
                    winnerEvent, callId, eventIdText, v005Ciphertext(),
                    v005EventRedactedJson(sha, randomSha256()), sha)).isEqualTo(1);

            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "ivr_role", loserApplication);
            // Same sha in redacted receipt and payload_sha256 column so only the domain/id
            // unique can fire after the winner commits (mismatched sha is 23514 first).
            String loserSha = randomSha256();
            Future<Integer> loserFuture = executor.submit(() -> executeUpdate(loser,
                    "INSERT INTO ivr.call_event "
                            + "(id, call_id, event_id, sequence_no, event_type, occurred_at, "
                            + "payload_ciphertext, payload_redacted, payload_sha256) "
                            + "VALUES (?, ?, ?, 2, 'SESSION_START', now(), ?, ?::jsonb, ?)",
                    loserEvent, callId, eventIdText, v005Ciphertext(),
                    v005EventRedactedJson(loserSha, randomSha256()), loserSha));
            awaitBlockedBackend(loserApplication);

            winner.commit();
            try {
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                fail("the event domain/id unique loser must not commit");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23505");
                assertThat(exception.getMessage()).contains("uq_call_event_domain_id");
            }
            loser.rollback();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.call_event WHERE event_id = ?", eventIdText))
                    .isEqualTo(1);
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.call_event WHERE id = ?", loserEvent)).isZero();
        }
    }

    private static void raceV005EventSequenceUnique(
            ExecutorService executor, UUID subjectId, String hmac) throws Exception {
        UUID callRecordId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID winnerEvent = UUID.randomUUID();
        UUID loserEvent = UUID.randomUUID();
        String callId = v005CallId();
        String sha = randomSha256();
        String winnerApplication = "pa5-s2-event-seq-winner";
        String loserApplication = "pa5-s2-event-seq-loser";

        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "ivr_role");
                assertThat(v005InsertSession(setup, sessionId, callId, subjectId, hmac))
                        .isEqualTo(1);
                assertThat(v005InsertCall(setup, callRecordId, callId, sessionId, subjectId, hmac))
                        .isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }

        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
            }
            setLocalRole(winner, "ivr_role");
            assertThat(executeUpdate(winner,
                    "INSERT INTO ivr.call_event "
                            + "(id, call_id, event_id, sequence_no, event_type, occurred_at, "
                            + "payload_ciphertext, payload_redacted, payload_sha256) "
                            + "VALUES (?, ?, ?, 1, 'SESSION_START', now(), ?, ?::jsonb, ?)",
                    winnerEvent, callId, v005EventId(), v005Ciphertext(),
                    v005EventRedactedJson(sha, randomSha256()), sha)).isEqualTo(1);

            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "ivr_role", loserApplication);
            // Matching sha keeps payload CHECKs legal so only the sequence unique can fire.
            String loserSha = randomSha256();
            Future<Integer> loserFuture = executor.submit(() -> executeUpdate(loser,
                    "INSERT INTO ivr.call_event "
                            + "(id, call_id, event_id, sequence_no, event_type, occurred_at, "
                            + "payload_ciphertext, payload_redacted, payload_sha256) "
                            + "VALUES (?, ?, ?, 1, 'DTMF', now(), ?, ?::jsonb, ?)",
                    loserEvent, callId, v005EventId(), v005Ciphertext(),
                    v005EventRedactedJson(loserSha, randomSha256()), loserSha));
            awaitBlockedBackend(loserApplication);

            winner.commit();
            try {
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                fail("the event sequence unique loser must not commit");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23505");
                assertThat(exception.getMessage()).contains("uq_call_event_sequence");
            }
            loser.rollback();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.call_event WHERE call_id = ? AND sequence_no = 1",
                    callId)).isEqualTo(1);
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.call_event WHERE id = ?", loserEvent)).isZero();
        }
    }

    private static void raceV005DecisionPurposeUnique(
            ExecutorService executor, UUID subjectId, String hmac) throws Exception {
        UUID callRecordId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID winnerDecision = UUID.randomUUID();
        UUID loserDecision = UUID.randomUUID();
        String callId = v005CallId();
        String fingerprint = randomSha256();
        String sha = randomSha256();
        String winnerApplication = "pa5-s2-decision-winner";
        String loserApplication = "pa5-s2-decision-loser";

        try (Connection setup = openConnection()) {
            setup.setAutoCommit(false);
            try {
                setLocalRole(setup, "ivr_role");
                assertThat(v005InsertSession(setup, sessionId, callId, subjectId, hmac))
                        .isEqualTo(1);
                assertThat(v005InsertCall(setup, callRecordId, callId, sessionId, subjectId, hmac))
                        .isEqualTo(1);
                setup.commit();
            } catch (SQLException | RuntimeException exception) {
                setup.rollback();
                throw exception;
            }
        }

        try (Connection winner = openConnection(); Connection loser = openConnection()) {
            winner.setAutoCommit(false);
            try (Statement statement = winner.createStatement()) {
                statement.execute("SET LOCAL application_name = '" + winnerApplication + "'");
            }
            setLocalRole(winner, "ivr_role");
            assertThat(executeUpdate(winner,
                    "INSERT INTO ivr.ivr_decision "
                            + "(id, session_id, call_id, purpose, request_fingerprint, action, "
                            + "reason_code, response_snapshot, response_payload_ciphertext, "
                            + "response_sha256, key_version) "
                            + "VALUES (?, ?, ?, 'INBOUND_RESOLVE', ?, 'GUIDE_ACTIVATION', "
                            + "'ACTIVATION_REQUIRED', '{}'::jsonb, ?, ?, 1)",
                    winnerDecision, sessionId, callId, fingerprint, v005Ciphertext(), sha))
                    .isEqualTo(1);

            loser.setAutoCommit(false);
            configureBlockedWriter(loser, "ivr_role", loserApplication);
            Future<Integer> loserFuture = executor.submit(() -> executeUpdate(loser,
                    "INSERT INTO ivr.ivr_decision "
                            + "(id, session_id, call_id, purpose, request_fingerprint, action, "
                            + "reason_code, response_snapshot, response_payload_ciphertext, "
                            + "response_sha256, key_version) "
                            + "VALUES (?, ?, ?, 'INBOUND_RESOLVE', ?, 'GUIDE_ACTIVATION', "
                            + "'ACTIVATION_REQUIRED', '{}'::jsonb, ?, ?, 1)",
                    loserDecision, sessionId, callId, fingerprint, v005Ciphertext(),
                    randomSha256()));
            awaitBlockedBackend(loserApplication);

            winner.commit();
            try {
                awaitFuture(loserFuture, FUTURE_TIMEOUT_SECONDS);
                fail("the decision purpose unique loser must not commit");
            } catch (SQLException exception) {
                assertThat(exception.getSQLState()).isEqualTo("23505");
                assertThat(exception.getMessage()).contains("uq_ivr_decision_call_purpose");
            }
            loser.rollback();
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.ivr_decision WHERE call_id = ? "
                            + "AND purpose = 'INBOUND_RESOLVE'",
                    callId)).isEqualTo(1);
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.ivr_decision WHERE id = ?", loserDecision))
                    .isZero();
        }
    }

    private static void assertV005CategoryTwoSavepointClearsBothLinks(
            UUID bindingId, UUID subjectId, UUID accountId, String hmac, String fingerprint)
            throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID callRecordId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String callId = v005CallId();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "ivr_role");
                assertThat(v005InsertTask(connection, taskId, v005TaskIdText(), bindingId,
                        subjectId, accountId, hmac, v005IdempotencyKey(), fingerprint))
                        .isEqualTo(1);
                assertThat(v005InsertSession(connection, sessionId, callId, subjectId, hmac))
                        .isEqualTo(1);
                assertThat(v005InsertCall(connection, callRecordId, callId, sessionId, subjectId,
                        hmac)).isEqualTo(1);
                // Pre-savepoint committed-in-transaction call survives the savepoint rollback.
                Savepoint preClaim = connection.setSavepoint();
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.outbound_call_task "
                                + "SET claimed_call_id = ?, claimed_at = now(), status = 'CLAIMED', "
                                + "updated_at = now(), revision = revision + 1 "
                                + "WHERE id = ? AND status = 'PENDING' AND claimed_call_id IS NULL",
                        callId, taskId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.call_record SET task_id = ?, revision = revision + 1 "
                                + "WHERE id = ? AND task_id IS NULL",
                        taskId, callRecordId)).isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM ivr.outbound_call_task "
                                + "WHERE id = ? AND claimed_call_id = ?",
                        taskId, callId)).isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM ivr.call_record WHERE id = ? AND task_id = ?",
                        callRecordId, taskId)).isEqualTo(1);
                connection.rollback(preClaim);
                // Category-2: savepoint rollback clears BOTH link assignments.
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM ivr.outbound_call_task "
                                + "WHERE id = ? AND claimed_call_id IS NULL AND status = 'PENDING'",
                        taskId)).as("claim cleared by savepoint rollback").isEqualTo(1);
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM ivr.call_record "
                                + "WHERE id = ? AND task_id IS NULL",
                        callRecordId)).as("call.task_id cleared by savepoint rollback")
                        .isEqualTo(1);
                // Pre-savepoint call row still present on the same connection.
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM ivr.call_record WHERE id = ?", callRecordId))
                        .as("pre-savepoint call survives savepoint rollback").isEqualTo(1);
                connection.rollback();
            } finally {
                connection.rollback();
            }
        }
    }

    private static void assertV005CategoryThreeWholeRollbackLeavesNoLink(
            UUID bindingId, UUID subjectId, UUID accountId, String hmac, String fingerprint)
            throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID callRecordId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        String callId = v005CallId();

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                setLocalRole(connection, "ivr_role");
                assertThat(v005InsertTask(connection, taskId, v005TaskIdText(), bindingId,
                        subjectId, accountId, hmac, v005IdempotencyKey(), fingerprint))
                        .isEqualTo(1);
                assertThat(v005InsertSession(connection, sessionId, callId, subjectId, hmac))
                        .isEqualTo(1);
                assertThat(v005InsertCall(connection, callRecordId, callId, sessionId, subjectId,
                        hmac)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.outbound_call_task "
                                + "SET claimed_call_id = ?, claimed_at = now(), status = 'CLAIMED', "
                                + "updated_at = now(), revision = revision + 1 "
                                + "WHERE id = ? AND status = 'PENDING' AND claimed_call_id IS NULL",
                        callId, taskId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE ivr.call_record SET task_id = ?, revision = revision + 1 "
                                + "WHERE id = ? AND task_id IS NULL",
                        taskId, callRecordId)).isEqualTo(1);
                // Category-3: an error after both links are set, then whole rollback.
                expectSqlFailure(connection, "P0001", "CALL_RECORD_PARTIES_IMMUTABLE",
                        "UPDATE ivr.call_record SET task_id = NULL WHERE id = ?", callRecordId);
                connection.rollback();
            } finally {
                connection.rollback();
            }
        }

        try (Connection verify = openConnection()) {
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.outbound_call_task WHERE id = ?", taskId))
                    .as("category-3 whole rollback leaves no task claim").isZero();
            assertThat(queryLong(verify,
                    "SELECT count(*) FROM ivr.call_record WHERE id = ?", callRecordId))
                    .as("category-3 whole rollback leaves no call link").isZero();
        }
    }

    private static void cleanupV005ConcurrentFixtures(
            UUID accountId, UUID organizationId, UUID subjectId, UUID bindingId)
            throws SQLException {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                // One TRUNCATE covering all six ivr tables in a single statement: the only
                // way to remove committed append-only fixtures; test-fixture hygiene only.
                // Runtime TRUNCATE denial remains asserted by the S1 ACL matrix.
                execute(connection,
                        "TRUNCATE TABLE ivr.ivr_decision, ivr.call_event, ivr.call_leg, "
                                + "ivr.call_record, ivr.inbound_session, ivr.outbound_call_task");
                // FK-safe parent deletes (no CASCADE, no runtime grants).
                executeUpdate(connection,
                        "DELETE FROM subject_number.caller_number_binding WHERE id = ?", bindingId);
                executeUpdate(connection,
                        "DELETE FROM subject_number.business_subject WHERE id = ?", subjectId);
                executeUpdate(connection,
                        "DELETE FROM organization_access.organization WHERE id = ?",
                        organizationId);
                executeUpdate(connection,
                        "DELETE FROM customer_iam.customer_account WHERE id = ?", accountId);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }

        try (Connection connection = openConnection()) {
            assertThat(queryLong(connection, "SELECT count(*) FROM ivr.ivr_decision"))
                    .as("ivr_decision empty after TRUNCATE").isZero();
            assertThat(queryLong(connection, "SELECT count(*) FROM ivr.call_event"))
                    .as("call_event empty after TRUNCATE").isZero();
            assertThat(queryLong(connection, "SELECT count(*) FROM ivr.call_leg"))
                    .as("call_leg empty after TRUNCATE").isZero();
            assertThat(queryLong(connection, "SELECT count(*) FROM ivr.call_record"))
                    .as("call_record empty after TRUNCATE").isZero();
            assertThat(queryLong(connection, "SELECT count(*) FROM ivr.inbound_session"))
                    .as("inbound_session empty after TRUNCATE").isZero();
            assertThat(queryLong(connection, "SELECT count(*) FROM ivr.outbound_call_task"))
                    .as("outbound_call_task empty after TRUNCATE").isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM customer_iam.customer_account WHERE id = ?", accountId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM organization_access.organization WHERE id = ?",
                    organizationId)).isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.business_subject WHERE id = ?", subjectId))
                    .isZero();
            assertThat(queryLong(connection,
                    "SELECT count(*) FROM subject_number.caller_number_binding WHERE id = ?",
                    bindingId)).isZero();
        }
    }
}
