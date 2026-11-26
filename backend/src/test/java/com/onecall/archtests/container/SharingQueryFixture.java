package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.sharing.internal.application.*;
import com.onecall.core.sharing.internal.persistence.SharingPersistenceConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import com.onecall.core.OneCallApplication;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

final class SharingQueryFixture implements AutoCloseable {
    static final String COLUMNS = "share_id, evidence_id, verification_mode, status, valid_from, expires_at, max_admissions, admissions_used, download_allowed, code_generation, revocation_generation";
    static final String SECRET = "PA10_SECRET_CANARY";
    final String password = UUID.randomUUID().toString();
    final String login = "query_" + UUID.randomUUID().toString().replace("-", "");
    final String deniedLogin = "denied_" + UUID.randomUUID().toString().replace("-", "");
    final GenericContainer<?> postgres = new GenericContainer<>(DockerImageName.parse("postgres:16"))
            .withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", password)
            .withEnv("POSTGRES_DB", "sharing_query").withEnv("POSTGRES_INITDB_ARGS", "--lc-collate=C --lc-ctype=C")
            .withExposedPorts(5432).waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
            .withStartupTimeout(Duration.ofMinutes(2));
    final List<Execution> executions = new CopyOnWriteArrayList<>();
    final AtomicInteger acquisitions = new AtomicInteger();
    final AtomicInteger commits = new AtomicInteger();
    final AtomicInteger rollbacks = new AtomicInteger();
    ConfigurableApplicationContext context;
    HikariDataSource pool;
    Flyway flyway;
    String url;
    volatile String sessionZone = "UTC";
    volatile String failOn;
    volatile String failState;
    volatile boolean tryWrite;
    volatile String writeState;
    volatile boolean corruptMapping;
    volatile boolean nullMapping;
    UUID account;
    UUID other;
    UUID org;
    UUID org2;
    UUID membership;
    UUID evidence;
    final Instant timestamp = Instant.parse("2026-01-02T03:04:05.123456Z");

    record Execution(String sql, int parameterCount, Set<Integer> indexes, int checkout,
            String effectiveRole, String readOnly, String isolation, int pid) {}

    void start() throws Exception {
        postgres.start();
        url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/sharing_query";
        flyway = Flyway.configure().dataSource(url, "fixture_admin", password).locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(20);
        flyway.validate();
        assertThat(flyway.info().applied()).hasSize(20);
        try (Connection admin = admin()) {
            execute(admin, "CREATE ROLE " + login + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + password + "'");
            execute(admin, "GRANT sharing_role TO " + login);
            execute(admin, "CREATE ROLE " + deniedLogin + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + password + "'");
        }
        context = new SpringApplicationBuilder(OneCallApplication.class).web(WebApplicationType.NONE)
                .profiles("persistence").initializers(application -> {
        application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("query-only", Map.of(
                "onecall.persistence.url", url, "onecall.persistence.username", login, "onecall.persistence.password", password)));
        application.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String name) {
                if (bean instanceof HikariDataSource source) {
                    pool = source;
                    source.setMaximumPoolSize(1);
                    return Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class, AutoCloseable.class},
                            (proxy, method, arguments) -> {
                                try {
                                    Object result = method.invoke(source, arguments);
                                    if (result instanceof Connection connection && method.getName().equals("getConnection")) {
                                        int checkout = acquisitions.incrementAndGet();
                                        try (Statement statement = connection.createStatement()) {
                                            statement.execute("SET TIME ZONE '" + sessionZone + "'");
                                            statement.execute("SET statement_timeout = '5s'");
                                        }
                                        return capture(connection, checkout);
                                    }
                                    return result;
                                } catch (InvocationTargetException failure) { throw failure.getCause(); }
                            });
                }
                return bean;
            }
        });
        }).run();
    }

    Connection admin() throws SQLException { return DriverManager.getConnection(url, "fixture_admin", password); }
    CustomerShareQueryService service() { return context.getBean(CustomerShareQueryService.class); }
    TrustedShareCaller personal() { return new TrustedShareCaller(account, Optional.empty()); }
    TrustedShareCaller organization() { return new TrustedShareCaller(account, Optional.of(org)); }
    SharePage page(TrustedShareCaller caller, Integer limit, ShareCursor cursor) {
        return service().listCustomerShares(caller, new SharePageRequest(Optional.ofNullable(cursor), Optional.ofNullable(limit)));
    }

    void reset() throws Exception {
        failOn = null;
        failState = null;
        tryWrite = false;
        writeState = null;
        corruptMapping = false;
        nullMapping = false;
        sessionZone = "UTC";
        account = UUID.randomUUID(); other = UUID.randomUUID(); org = UUID.randomUUID(); org2 = UUID.randomUUID();
        membership = UUID.randomUUID(); evidence = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        String call = "pa10-" + UUID.randomUUID();
        String event = UUID.randomUUID().toString();
        try (Connection admin = admin()) {
            admin.setAutoCommit(false);
            execute(admin, "INSERT INTO customer_iam.customer_account(id, display_name) VALUES (?, 'A'), (?, 'B')", account, other);
            execute(admin, "INSERT INTO organization_access.organization(id, display_name) VALUES (?, 'O1'), (?, 'O2')", org, org2);
            execute(admin, "INSERT INTO organization_access.organization_membership(id, organization_id, account_id, role) VALUES (?, ?, ?, 'MEMBER'), (?, ?, ?, 'ADMINISTRATOR'), (?, ?, ?, 'MEMBER')",
                    membership, org, account, UUID.randomUUID(), org, other, UUID.randomUUID(), org2, account);
            execute(admin, "INSERT INTO subject_number.business_subject(id, source) VALUES (?, 'IVR')", subject);
            execute(admin, """
                    INSERT INTO ivr.inbound_session(id, call_id, subject_id, trusted_caller, access_number_masked, node_id, protocol_version, prompt_version)
                    VALUES (?, ?, ?, false, 'synthetic', 'fixture', 'v1', 'v1')
                    """, session, call, subject);
            execute(admin, """
                    INSERT INTO ivr.call_record(id, call_id, session_id, subject_id, caller_ciphertext, caller_number_hmac,
                        caller_masked, access_number_masked, node_id, protocol_version)
                    VALUES (?, ?, ?, ?, 'v1.fixture.payload', ?, 'synthetic', 'synthetic', 'fixture', 'v1')
                    """, UUID.randomUUID(), call, session, subject, UUID.randomUUID().toString().replace("-", "").repeat(2));
            execute(admin, """
                    INSERT INTO evidence.evidence_record(id, subject_id, source_type, source_ref_id, source_event_domain,
                        source_event_id, identity_at_capture, consent_version, snapshot, snapshot_sha256, call_id, session_id, status, solidified_at)
                    VALUES (?, ?, 'CALL_RECORDING', ?, 'EVIDENCE', ?,
                        jsonb_build_object('subjectId', ?::text, 'capturedAt', '2026-01-01T00:00:00Z', 'sourceChannel', 'IVR'), 'v1',
                        jsonb_build_object('capture', '{}'::jsonb, 'content', '{}'::jsonb, 'sourceEventDomain', 'EVIDENCE',
                            'sourceEventId', ?::text, 'source', jsonb_build_object('subjectId', ?::text, 'recordingId', ?::text,
                            'callId', ?::text, 'sessionId', ?::text)), repeat('a', 64), ?, ?, 'SOLIDIFIED', now())
                    """, evidence, subject, source, event, subject, event, subject, source, call, session, call, session);
            execute(admin, "INSERT INTO evidence.evidence_event(id, evidence_id, event_domain, event_id, event_type, payload_redacted, payload_sha256) VALUES (?, ?, 'EVIDENCE', ?, 'EVIDENCE_CREATED', '{}'::jsonb, repeat('b',64))",
                    UUID.randomUUID(), evidence, event);
            UUID media = UUID.randomUUID();
            execute(admin, """
                    INSERT INTO evidence.media_object(id, storage_provider, bucket, object_key, object_version_id, sha256, size_bytes, mime_type, crypto_key_ref)
                    VALUES (?, 'COS', 'fixture', ?, 'v1', repeat('c',64), 1, 'audio/wav', 'fixture-key')
                    """, media, SECRET + media);
            execute(admin, "INSERT INTO evidence.evidence_artifact(id, evidence_id, media_object_id, artifact_role) VALUES (?, ?, ?, 'DERIVED')",
                    UUID.randomUUID(), evidence, media);
            admin.commit();
        }
        executions.clear(); acquisitions.set(0); commits.set(0); rollbacks.set(0);
    }

    void grant(String id, UUID creator, UUID organization, Instant from) throws Exception {
        grant(id, creator, organization, from, "NONE", "ACTIVE", null);
    }

    void grant(String id, UUID creator, UUID organization, Instant from, String mode, String status, Integer maximum) throws Exception {
        try (Connection admin = admin()) {
            execute(admin, """
                    INSERT INTO sharing.evidence_share_grant(id, share_id, capability_token_hash, evidence_id,
                        created_by_account_id, owner_organization_id, verification_mode, valid_from, expires_at, max_admissions,
                        content_snapshot, content_sha256, code_salt, code_hash, code_generation, status)
                    SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                        jsonb_build_object('content', jsonb_build_object('evidenceId', ?::text, 'canary', ?::text),
                        'verification', jsonb_build_object('mode', ?::text) || CASE WHEN ? = 'DESIGNATED_MOBILE'
                        THEN jsonb_build_object('mobileHmac', repeat('c',64), 'keyVersion', 'canary-key') ELSE '{}'::jsonb END,
                        'creation', jsonb_build_object('creatorAccountId', ?::text, 'evidenceId', ?::text,
                        'ownerOrganizationId', ?::text, 'scopeDigest', repeat('d',64), 'creationRequestDigest', repeat('e',64), 'creationFingerprint', repeat('f',64))),
                        repeat('a',64), CASE WHEN ? = 'EXTRACTION_CODE' THEN repeat('S',22) END,
                        CASE WHEN ? = 'EXTRACTION_CODE' THEN repeat('b',64) END,
                        CASE WHEN ? = 'EXTRACTION_CODE' THEN 1 ELSE 0 END, ?
                    """, UUID.randomUUID(), id, UUID.randomUUID().toString().replace("-", "").repeat(2), evidence,
                    creator, organization, mode, from.atOffset(ZoneOffset.UTC), from.plusSeconds(86400).atOffset(ZoneOffset.UTC), maximum,
                    evidence, SECRET, mode, mode, creator, evidence, organization, mode, mode, mode, status);
        }
    }

    void update(String table, UUID id, String field, String value) throws Exception {
        try (Connection admin = admin()) {
            admin.setAutoCommit(false);
            update(admin, table, id, field, value);
            admin.commit();
        }
    }

    void update(Connection connection, String table, UUID id, String field, String value) throws SQLException {
        String owner = switch (table) {
            case "customer_iam.customer_account" -> "customer_iam_owner";
            case "organization_access.organization", "organization_access.organization_membership" -> "organization_access_owner";
            default -> throw new IllegalArgumentException("Unsupported lifecycle fixture");
        };
        execute(connection, "SET LOCAL ROLE " + owner);
        execute(connection, "UPDATE " + table + " SET " + field + " = ?, revision = revision + 1 WHERE id = ?", value, id);
    }

    List<String> baseline(UUID creator, UUID organization) throws Exception {
        try (Connection admin = admin(); PreparedStatement statement = admin.prepareStatement(
                "SELECT share_id FROM sharing.evidence_share_grant WHERE created_by_account_id = ? AND owner_organization_id IS NOT DISTINCT FROM ? ORDER BY valid_from DESC, share_id DESC")) {
            statement.setObject(1, creator); statement.setObject(2, organization);
            try (ResultSet rows = statement.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rows.next()) { result.add(rows.getString(1)); }
                return List.copyOf(result);
            }
        }
    }

    static void execute(Connection connection, String sql, Object... arguments) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < arguments.length; index++) { statement.setObject(index + 1, arguments[index]); }
            statement.execute();
        }
    }

    private Connection capture(Connection connection, int checkout) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
            try {
                if (method.getName().equals("commit")) { commits.incrementAndGet(); }
                if (method.getName().equals("rollback")) { rollbacks.incrementAndGet(); }
                Object result = method.invoke(connection, arguments);
                if (method.getName().equals("prepareStatement") && result instanceof PreparedStatement statement) {
                    return captureStatement(connection, statement, (String) arguments[0], checkout);
                }
                return result;
            } catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
    }

    private PreparedStatement captureStatement(Connection connection, PreparedStatement statement, String sql, int checkout) {
        Set<Integer> indexes = new TreeSet<>();
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                (proxy, method, arguments) -> {
                    try {
                        if (method.getName().startsWith("set") && arguments != null && arguments.length >= 2 && arguments[0] instanceof Integer index) {
                            indexes.add(index);
                        }
                        if (method.getName().startsWith("execute")) {
                            if (failOn != null && sql.toLowerCase(Locale.ROOT).contains(failOn)) {
                                throw new SQLException(SECRET + " SQL password " + password, failState);
                            }
                            if (sql.contains("evidence_share_grant")) {
                                try (Statement probe = connection.createStatement(); ResultSet facts = probe.executeQuery(
                                        "select current_user, current_setting('transaction_read_only'), current_setting('transaction_isolation'), pg_backend_pid()")) {
                                    facts.next();
                                    executions.add(new Execution(sql, statement.getParameterMetaData().getParameterCount(), Set.copyOf(indexes), checkout,
                                            facts.getString(1), facts.getString(2), facts.getString(3), facts.getInt(4)));
                                }
                                if (tryWrite) {
                                    try (Statement probe = connection.createStatement()) {
                                        probe.executeUpdate("UPDATE sharing.evidence_share_grant SET revision = revision + 1");
                                    } catch (SQLException denied) { writeState = denied.getSQLState(); throw denied; }
                                }
                            } else {
                                executions.add(new Execution(sql, statement.getParameterMetaData().getParameterCount(), Set.copyOf(indexes), checkout, "", "", "", 0));
                            }
                        }
                        Object result = method.invoke(statement, arguments);
                        if (corruptMapping && sql.contains("evidence_share_grant") && result instanceof ResultSet rows) {
                            return Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                                    (rowProxy, rowMethod, rowArguments) -> {
                                        if (rowMethod.getName().equals("getString") && Objects.equals(rowArguments[0], 3)) { return nullMapping ? null : SECRET; }
                                        try { return rowMethod.invoke(rows, rowArguments); }
                                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                                    });
                        }
                        return result;
                    } catch (InvocationTargetException failure) { throw failure.getCause(); }
                });
    }

    @Override
    public void close() {
        if (context != null) { context.close(); }
        if (pool != null) { pool.close(); }
        postgres.stop();
    }
}
