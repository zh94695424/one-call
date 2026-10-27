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
 * PA1/PA2 PostgreSQL 16 proof for the frozen V001 and V002 contracts: V001-then-V002 migration,
 * exact V001+V002 inventory (including the F-PA2-PR-01 34-check normalized pg_get_constraintdef
 * map) and failure-visibility; idempotency/Outbox first-result and transaction atomicity; and
 * audit append-only, reason/denylist, fixed-search-path (V001 functions plus the six V002 guards)
 * and privilege boundaries. There is no skip path: an unavailable Docker daemon, failed image
 * pull, failed container start, JDBC failure, Flyway failure or assertion failure fails the Maven
 * build. The password is generated per JVM run and never persisted.
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

    private static final List<String> ALL_V001_V002_TABLES = List.of(
            "audit.audit_fact",
            "customer_iam.auth_identity",
            "customer_iam.customer_account",
            "customer_iam.identity_profile",
            "operations_iam.employee_account",
            "organization_access.organization",
            "organization_access.organization_membership",
            "platform.idempotency_record",
            "platform.outbox_event");

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
    void v001ThenV002MigrateCleanPostgres16WithoutSkipping() throws Exception {
        assertThat(initialMigration.migrationsExecuted).isEqualTo(2);

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
            assertThat(resultSet.next()).isFalse();
        }

        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();

        MigrateResult repeated = flyway.migrate();
        assertThat(repeated.migrationsExecuted).isZero();

        exerciseV001FunctionsAsGrantedRole();
        exerciseV002InsertsAsOwningRuntimeRolesAndRollBack();
    }

    @Test
    void v001AndV002InventoryMatchesTheFrozenContract() throws Exception {
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

            // A PL/pgSQL body uses ordinary BEGIN/END block delimiters, so this assertion
            // matches only transaction-control statements, never BEGIN/END themselves.
            assertThat(queryLong(
                    connection,
                    "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                            + "WHERE ((n.nspname = 'platform' AND p.proname IN "
                            + "('idempotency_begin','idempotency_complete','outbox_append')) "
                            + "OR (n.nspname = 'audit' AND p.proname = 'append_fact') "
                            + "OR (n.nspname IN ('customer_iam','organization_access','operations_iam') "
                            + "AND p.proname LIKE 'guard_%')) "
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
                    connection, "SELECT count(*) FROM pg_proc WHERE proname = 'issue_first_trial'"))
                    .as("issue_first_trial is a V006 function absent from V001/V002")
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
        assertFunctionsAndTrigger(connection);
        assertGrants(connection);
    }

    private static void assertSchemasAndRoles(Connection connection) throws SQLException {
        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '=' || pg_get_userbyid(n.nspowner) FROM pg_namespace n "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access','operations_iam','subject_number') "
                        + "ORDER BY n.nspname"))
                .containsExactly(
                        "audit=audit_owner",
                        "customer_iam=customer_iam_owner",
                        "operations_iam=operations_iam_owner",
                        "organization_access=organization_access_owner",
                        "platform=platform_reliability_owner",
                        "subject_number=subject_number_owner");

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_namespace WHERE nspname IN "
                        + "('catalog_order','ivr','entitlement_metering','activation_consent','evidence','sharing')"))
                .isZero();

        assertThat(queryStrings(
                connection,
                "SELECT nspname FROM pg_namespace WHERE nspname <> 'public' "
                        + "AND nspname <> 'information_schema' AND nspname NOT LIKE 'pg%' ORDER BY nspname"))
                .containsExactly(
                        "audit",
                        "customer_iam",
                        "operations_iam",
                        "organization_access",
                        "platform",
                        "subject_number");

        List<String> expectedRoles = new ArrayList<>();
        for (String role : S1_ROLES) {
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
                        + "('catalog_order_owner','catalog_order_role','ivr_owner','ivr_role',"
                        + "'entitlement_metering_owner','entitlement_metering_role',"
                        + "'activation_consent_owner','activation_consent_role','evidence_owner',"
                        + "'evidence_role','sharing_owner','sharing_role')"))
                .isZero();
    }

    private static void assertTablesColumnsChecksAndIndexes(Connection connection) throws SQLException {
        assertThat(queryStrings(
                connection,
                "SELECT table_schema || '.' || table_name FROM information_schema.tables "
                        + "WHERE table_schema NOT IN ('pg_catalog','information_schema') "
                        + "AND NOT (table_schema = 'public' AND table_name = 'flyway_schema_history') ORDER BY 1"))
                .containsExactlyElementsOf(ALL_V001_V002_TABLES);

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
                        "customer_iam.guard_auth_identity",
                        "customer_iam.guard_customer_account",
                        "customer_iam.guard_identity_profile",
                        "operations_iam.guard_employee_account",
                        "organization_access.guard_membership",
                        "organization_access.guard_organization",
                        "platform.idempotency_begin",
                        "platform.idempotency_complete",
                        "platform.outbox_append");

        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || p.proname || '(' || pg_get_function_identity_arguments(p.oid) || ')' "
                        + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access','operations_iam') "
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
                                + "p_payload_sha256 text)");

        assertThat(queryStrings(
                connection,
                "SELECT n.nspname || '.' || p.proname || '|' || p.prosecdef || '|' "
                        + "|| pg_get_userbyid(p.proowner) || '|' || coalesce(array_to_string(p.proconfig, ','), '') "
                        + "FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access','operations_iam') "
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
                        "platform.outbox_append|true|platform_reliability_owner|search_path=platform, pg_temp");

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access','operations_iam') "
                        + "AND EXISTS (SELECT 1 FROM unnest(p.proargnames) AS a WHERE a = 'p_id')")).isZero();

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit') "
                        + "AND p.prosrc LIKE '%pg_catalog.gen_random_uuid()%'")).isEqualTo(3);

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access','operations_iam') "
                        + "AND p.prosrc LIKE '%gen_random_uuid%'")).isEqualTo(3);

        assertThat(queryLong(
                connection,
                "SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace "
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access','operations_iam') "
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
                        "customer_iam.auth_identity.trg_auth_identity_guard",
                        "customer_iam.customer_account.trg_customer_account_guard",
                        "customer_iam.identity_profile.trg_identity_profile_guard",
                        "operations_iam.employee_account.trg_employee_account_guard",
                        "organization_access.organization.trg_organization_guard",
                        "organization_access.organization_membership.trg_membership_guard");

        Map<String, String> v002TriggerFunctionByTrigger = Map.of(
                "trg_customer_account_guard", "customer_iam.guard_customer_account()",
                "trg_auth_identity_guard", "customer_iam.guard_auth_identity()",
                "trg_identity_profile_guard", "customer_iam.guard_identity_profile()",
                "trg_organization_guard", "organization_access.guard_organization()",
                "trg_membership_guard", "organization_access.guard_membership()",
                "trg_employee_account_guard", "operations_iam.guard_employee_account()");
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
                "subject_number", List.of("subject_number_owner"));
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
                        + "WHERE n.nspname IN ('platform','audit','customer_iam','organization_access','operations_iam') "
                        + "AND a.grantee = 0"))
                .isZero();
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
}
