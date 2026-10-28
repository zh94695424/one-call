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
    void v001ThroughV004MigrateCleanPostgres16WithoutSkipping() throws Exception {
        assertThat(initialMigration.migrationsExecuted).isEqualTo(4);

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
            assertThat(resultSet.next()).isFalse();
        }

        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();

        MigrateResult repeated = flyway.migrate();
        assertThat(repeated.migrationsExecuted).isZero();

        exerciseV001FunctionsAsGrantedRole();
        exerciseV002InsertsAsOwningRuntimeRolesAndRollBack();
        exerciseV003InsertsAsRuntimeRoleAndRollBack();
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
                            + "'reject_row_mutation'))) "
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
                    connection, "SELECT count(*) FROM pg_proc WHERE proname = 'issue_first_trial'"))
                    .as("issue_first_trial is a V006 function absent from V001-V005")
                    .isZero();
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
            assertNoV005PlusObjects(connection);
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

    private static void assertNoV005PlusObjects(Connection connection) throws SQLException {
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_namespace WHERE nspname IN ('evidence','sharing')"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname IN ('ivr','entitlement_metering','activation_consent') "
                        + "AND c.relkind = 'r'"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc WHERE proname = 'issue_first_trial'"))
                .isZero();
        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_roles WHERE rolname IN "
                        + "('evidence_owner','evidence_role','sharing_owner','sharing_role')"))
                .isZero();
        assertThat(toRegclass(connection, "ivr.call_task")).isNull();
        assertThat(toRegclass(connection, "ivr.call_session")).isNull();
        assertThat(toRegclass(connection, "ivr.outbound_call_task")).isNull();
        assertThat(toRegclass(connection, "ivr.inbound_session")).isNull();
        assertThat(toRegclass(connection, "ivr.call_record")).isNull();
        assertThat(toRegclass(connection, "ivr.call_leg")).isNull();
        assertThat(toRegclass(connection, "ivr.call_event")).isNull();
        assertThat(toRegclass(connection, "ivr.ivr_decision")).isNull();
        assertThat(toRegclass(connection, "activation_consent.consent_record")).isNull();
        assertThat(toRegclass(connection, "activation_consent.service_activation")).isNull();
        assertThat(toRegclass(connection, "activation_consent.service_activation_transition")).isNull();
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
        usageRolesBySchema.put("ivr", List.of("ivr_owner"));
        usageRolesBySchema.put("entitlement_metering", List.of(
                "entitlement_metering_owner", "entitlement_metering_role",
                "activation_consent_role", "ivr_role"));
        usageRolesBySchema.put("activation_consent", List.of(
                "activation_consent_owner", "activation_consent_role", "ivr_role",
                "catalog_order_role"));
        usageRolesBySchema.put("customer_iam", List.of(
                "customer_iam_owner", "customer_iam_role", "subject_number_role",
                "catalog_order_role"));
        usageRolesBySchema.put("organization_access", List.of(
                "organization_access_owner", "organization_access_role", "customer_iam_role",
                "subject_number_role", "catalog_order_role"));
        usageRolesBySchema.put("subject_number", List.of(
                "subject_number_owner", "subject_number_role",
                "subject_identity_verification_role", "catalog_order_role"));
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
                .as("ivr_role ivr USAGE is deferred to V005").isFalse();
        assertThat(hasSchema(connection, "ivr_role", "catalog_order", "USAGE"))
                .as("ivr_role catalog_order USAGE is deferred to V005").isFalse();
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
                        boolean expectGrant = role.equals("catalog_order_role")
                                && privilege.equals("SELECT")
                                && catalogGrantedColumns.contains(column);
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
                                + "WHERE n.nspname IN ('ivr','entitlement_metering','activation_consent') "
                                + "AND c.relkind = 'r'"))
                        .as("no IVR/entitlement/consent table")
                        .isZero();
                assertThat(queryLong(connection,
                        "SELECT count(*) FROM pg_namespace WHERE nspname IN ('evidence','sharing')"))
                        .as("no evidence/sharing schema")
                        .isZero();
                assertThat(toRegclass(connection, "ivr.call_task")).isNull();
                assertThat(toRegclass(connection, "ivr.call_session")).isNull();
                assertThat(toRegclass(connection, "ivr.outbound_call_task")).isNull();
                assertThat(toRegclass(connection, "ivr.inbound_session")).isNull();
                assertThat(toRegclass(connection, "ivr.call_record")).isNull();
                assertThat(toRegclass(connection, "ivr.call_leg")).isNull();
                assertThat(toRegclass(connection, "ivr.call_event")).isNull();
                assertThat(toRegclass(connection, "ivr.ivr_decision")).isNull();
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

                // ACTIVE -> OFFSALE succeeds with revision+1.
                switchLocalRole(connection, "catalog_order_role");
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_version "
                                + "SET sale_status = 'OFFSALE', revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'ACTIVE' AND revision = 3",
                        versionId)).isEqualTo(1);

                // Re-activate for guard/unique probes: retire path then second ACTIVE.
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_version "
                                + "SET sale_status = 'ACTIVE', published_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'OFFSALE' AND revision = 4",
                        versionId)).isEqualTo(1);

                // Offer mutable updates succeed; relation changes raise the guard.
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

                // Second ACTIVE on the same product fails the partial unique index.
                switchLocalRole(connection, "catalog_order_role");
                expectSqlFailure(connection, "23505", "uq_catalog_version_active",
                        "UPDATE catalog_order.catalog_version "
                                + "SET sale_status = 'ACTIVE', published_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 1",
                        secondVersionId);

                // Retire-then-activate leaves exactly one ACTIVE.
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_version "
                                + "SET sale_status = 'OFFSALE', revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'ACTIVE' AND revision = 5",
                        versionId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.catalog_version "
                                + "SET sale_status = 'ACTIVE', published_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND sale_status = 'DRAFT' AND revision = 1",
                        secondVersionId)).isEqualTo(1);
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
        UUID verifiedEventId = UUID.randomUUID();
        UUID unverifiedEventId = UUID.randomUUID();
        String orderNo = "S2ORD" + UUID.randomUUID().toString().replace("-", "");
        String idempotencyKey = "s2-idem-" + UUID.randomUUID();
        String fingerprint = randomSha256();
        String orderSnapshotSha = randomSha256();
        String merchantNo = "S2MER" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);

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

                // Valid payment transitions with conditional revision updates.
                switchLocalRole(connection, "catalog_order_role");
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.customer_order "
                                + "SET payment_status = 'PAID', paid_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND payment_status = 'UNPAID' AND revision = 1",
                        orderId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.customer_order "
                                + "SET payment_status = 'CLOSED', paid_at = NULL, closed_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND payment_status = 'PAID' AND revision = 2",
                        orderId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.customer_order "
                                + "SET payment_status = 'CLOSED', closed_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND payment_status = 'PAID' AND revision = 2",
                        orderId))
                        .as("stale order revision update affects zero rows").isZero();
                assertThat(queryString(connection,
                        "SELECT payment_status || '|' || (paid_at IS NULL)::text || '|' "
                                + "|| (closed_at IS NOT NULL)::text || '|' || revision::text "
                                + "FROM catalog_order.customer_order WHERE id = ?",
                        orderId)).isEqualTo("CLOSED|true|true|3");

                // Invalid payment shapes fail 23514 with the exact check name.
                expectSqlFailure(connection, "23514", "ck_customer_order_payment_shape",
                        "UPDATE catalog_order.customer_order "
                                + "SET payment_status = 'PAID', paid_at = NULL, "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND payment_status = 'CLOSED' AND revision = 3",
                        orderId);

                // Payment-order insert and valid status/transaction transitions.
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
                                + "SET status = 'CLOSED', updated_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND status = 'PAID' AND revision = 2",
                        paymentOrderId)).isEqualTo(1);
                assertThat(executeUpdate(connection,
                        "UPDATE catalog_order.payment_order "
                                + "SET status = 'PAID', updated_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND status = 'UNPAID' AND revision = 1",
                        paymentOrderId))
                        .as("stale payment_order revision update affects zero rows").isZero();

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

                // Runtime PAID without provider_transaction_id fails the paid-shape check.
                expectSqlFailure(connection, "23514", "ck_payment_order_paid_shape",
                        "UPDATE catalog_order.payment_order "
                                + "SET status = 'PAID', provider_transaction_id = NULL, "
                                + "updated_at = now(), "
                                + "revision = revision + 1 "
                                + "WHERE id = ? AND status = 'CLOSED' AND revision = 3",
                        paymentOrderId);

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
                    "SELECT count(*) FROM catalog_order.payment_order WHERE id = ?",
                    paymentOrderId)).isZero();
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
}
