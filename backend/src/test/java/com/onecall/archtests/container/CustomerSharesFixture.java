package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType;
import com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession;
import com.onecall.core.OneCallApplication;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

final class CustomerSharesFixture implements AutoCloseable {
    static final String SECRET = "PA12_SECRET_CANARY";
    final CustomerMeFixture base = new CustomerMeFixture();
    final UUID organization = UUID.randomUUID();
    final UUID organizationTwo = UUID.randomUUID();
    final List<String> shareIds = new ArrayList<>();
    final Map<String, HikariDataSource> pools = new java.util.concurrent.ConcurrentHashMap<>();
    final Map<String, java.util.concurrent.atomic.AtomicInteger> beanCheckouts = new java.util.concurrent.ConcurrentHashMap<>();
    final Map<Integer, String> checkoutOwners = new java.util.concurrent.ConcurrentHashMap<>();
    final ListAppender<ILoggingEvent> serviceLogs = new ListAppender<>();
    final ch.qos.logback.classic.Logger queryLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            com.onecall.core.sharing.internal.application.CustomerShareQueryService.class);
    volatile String mappingFailureOn;
    ConfigurableApplicationContext context;

    void start() throws Exception {
        base.start();
        base.reset();
        base.context.close();
        context = startSharesContext(true, base.sharingLogin);
        serviceLogs.start();
        queryLogger.addAppender(serviceLogs);
        sessions(WorkspaceType.PERSONAL, null);
    }

    ConfigurableApplicationContext startSharesContext(boolean persistence, String sharingLogin) {
        if (context != null) context.close();
        else if (base.context != null) base.context.close();
        CustomerMeFixture.SESSIONS.clear();
        context = new SpringApplicationBuilder(OneCallApplication.class, CustomerMeFixture.DualPoolTestBeans.class)
                .profiles(persistence ? new String[]{"web", "persistence"} : new String[]{"web"})
                .properties("server.port=0", "spring.main.banner-mode=off")
                .initializers(application -> {
                    application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa12-shares", Map.of(
                            "onecall.customer.persistence.url", base.url,
                            "onecall.customer.persistence.username", base.login,
                            "onecall.customer.persistence.password", base.password,
                            "onecall.persistence.url", base.url,
                            "onecall.persistence.username", sharingLogin,
                            "onecall.persistence.password", base.password)));
                    application.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
                        @Override
                        public Object postProcessAfterInitialization(Object bean, String beanName) {
                            if (!(bean instanceof HikariDataSource source)) return bean;
                            source.setMaximumPoolSize(1);
                            source.setConnectionTimeout(5000);
                            base.allPools.add(source);
                            pools.put(beanName, source);
                            return Proxy.newProxyInstance(DataSource.class.getClassLoader(),
                                    new Class<?>[]{DataSource.class, AutoCloseable.class}, (proxy, method, arguments) -> {
                                        try {
                                            Object result = method.invoke(source, arguments);
                                            if (result instanceof Connection connection && method.getName().equals("getConnection")) {
                                                int checkout = base.acquisitions.incrementAndGet();
                                                beanCheckouts.computeIfAbsent(beanName, ignored -> new java.util.concurrent.atomic.AtomicInteger()).incrementAndGet();
                                                checkoutOwners.put(checkout, beanName);
                                                try (var timeout = connection.createStatement()) { timeout.execute("SET statement_timeout = '20s'"); }
                                                return captureConnection(connection, checkout);
                                            }
                                            return result;
                                        } catch (InvocationTargetException failure) { throw failure.getCause(); }
                                    });
                        }
                    });
                }).run();
        base.context = context;
        return context;
    }

    void sessions(WorkspaceType type, UUID organizationId) {
        CustomerMeFixture.SESSIONS.clear();
        CustomerMeFixture.SESSIONS.put("first", new VerifiedCustomerSession(base.account,
                com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel.WEB,
                "onecall:customer-session:v1", new com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace(type, organizationId)));
        CustomerMeFixture.SESSIONS.put("second", new VerifiedCustomerSession(base.other,
                com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel.MINIAPP,
                "onecall:customer-session:v1", new com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace(type, organizationId)));
    }

    void reset() throws Exception {
        base.reset();
        mappingFailureOn = null;
        beanCheckouts.values().forEach(counter -> counter.set(0));
        checkoutOwners.clear();
        serviceLogs.list.clear();
        shareIds.clear();
        sessions(WorkspaceType.PERSONAL, null);
    }

    Map<String, HikariDataSource> capturePoolsByBeanName() {
        return Map.copyOf(new TreeMap<>(pools));
    }

    String checkoutOwner(int checkout) {
        return checkoutOwners.get(checkout);
    }

    void assertServiceLogsExclude(String... canaries) {
        for (ILoggingEvent event : serviceLogs.list) {
            String message = event.getFormattedMessage();
            String throwable = event.getThrowableProxy() == null ? "" : event.getThrowableProxy().getMessage();
            for (String canary : canaries) {
                assertThat(message).doesNotContain(canary);
                assertThat(throwable).doesNotContain(canary);
            }
        }
    }

    private Connection captureConnection(Connection connection, int checkout) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
            try {
                if (method.getName().equals("commit") && base.failCommit) {
                    base.failCommit = false;
                    throw new SQLException("COMMIT_CANARY", "08006");
                }
                if (method.getName().equals("commit")) base.commits.incrementAndGet();
                if (method.getName().equals("rollback")) base.rollbacks.incrementAndGet();
                Object result = method.invoke(connection, arguments);
                if (result instanceof PreparedStatement statement && method.getName().equals("prepareStatement"))
                    return captureStatement(connection, statement, (String) arguments[0], checkout);
                return result;
            } catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
    }

    private PreparedStatement captureStatement(Connection connection, PreparedStatement statement, String sql, int checkout) {
        Map<Integer, Object> values = new TreeMap<>();
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class}, (proxy, method, arguments) -> {
            try {
                if (method.getName().startsWith("set") && arguments != null && arguments.length >= 2 && arguments[0] instanceof Integer index)
                    values.put(index, arguments[1]);
                if (method.getName().startsWith("execute")) {
                    if (base.failOn != null && sql.toLowerCase(java.util.Locale.ROOT).contains(base.failOn))
                        throw new SQLException("JDBC_CANARY " + base.password, "08006");
                    if (base.disconnectNext && sql.toLowerCase(java.util.Locale.ROOT).contains("customer_account")) {
                        base.disconnectNext = false;
                        int backend;
                        try (var probe = connection.createStatement(); var rows = probe.executeQuery("select pg_backend_pid()")) {
                            rows.next();
                            backend = rows.getInt(1);
                        }
                        try (var owner = base.admin()) { CustomerMeFixture.execute(owner, "select pg_terminate_backend(?)", backend); }
                    }
                    try (var probe = connection.createStatement(); var facts = probe.executeQuery(
                            "select current_user, session_user, current_setting('transaction_read_only'), current_setting('transaction_isolation'), pg_backend_pid()")) {
                        facts.next();
                        base.executions.add(new CustomerMeFixture.Execution(sql, List.copyOf(values.values()), checkout,
                                facts.getString(1), facts.getString(2), facts.getString(3), facts.getString(4), facts.getInt(5)));
                    }
                }
                Object result = method.invoke(statement, arguments);
                if (result instanceof java.sql.ResultSet rows && mappingFailureOn != null
                        && sql.toLowerCase(java.util.Locale.ROOT).contains(mappingFailureOn.toLowerCase(java.util.Locale.ROOT))) {
                    return Proxy.newProxyInstance(java.sql.ResultSet.class.getClassLoader(), new Class<?>[]{java.sql.ResultSet.class}, (resultProxy, resultMethod, resultArguments) -> {
                        if (resultMethod.getName().equals("getString"))
                            throw new com.onecall.core.sharing.internal.application.ShareQueryException(
                                    com.onecall.core.sharing.internal.application.ShareQueryException.Code.INTERNAL);
                        try { return resultMethod.invoke(rows, resultArguments); }
                        catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
                }
                return result;
            } catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
    }

    String seedShare(UUID creator, UUID ownerOrganization, Instant from) throws Exception {
        return seedShare("pa12-" + UUID.randomUUID(), creator, ownerOrganization, from);
    }

    String seedShare(String shareId, UUID creator, UUID ownerOrganization, Instant from) throws Exception {
        shareIds.add(shareId);
        UUID evidence = UUID.randomUUID();
        UUID subject = UUID.randomUUID();
        UUID source = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        String callId = "pa12-" + UUID.randomUUID();
        String eventId = UUID.randomUUID().toString();
        try (Connection admin = base.admin()) {
            admin.setAutoCommit(false);
            CustomerMeFixture.execute(admin, "INSERT INTO subject_number.business_subject(id, source) VALUES (?, 'IVR')", subject);
            CustomerMeFixture.execute(admin, """
                    INSERT INTO ivr.inbound_session(id, call_id, subject_id, trusted_caller, access_number_masked, node_id, protocol_version, prompt_version)
                    VALUES (?, ?, ?, false, 'synthetic', 'fixture', 'v1', 'v1')
                    """, session, callId, subject);
            CustomerMeFixture.execute(admin, """
                    INSERT INTO ivr.call_record(id, call_id, session_id, subject_id, caller_ciphertext, caller_number_hmac,
                        caller_masked, access_number_masked, node_id, protocol_version)
                    VALUES (?, ?, ?, ?, 'v1.fixture.payload', ?, 'synthetic', 'synthetic', 'fixture', 'v1')
                    """, UUID.randomUUID(), callId, session, subject, UUID.randomUUID().toString().replace("-", "").repeat(2));
            CustomerMeFixture.execute(admin, """
                    INSERT INTO evidence.evidence_record(id, subject_id, source_type, source_ref_id, source_event_domain,
                        source_event_id, identity_at_capture, consent_version, snapshot, snapshot_sha256, call_id, session_id, status, solidified_at)
                    VALUES (?, ?, 'CALL_RECORDING', ?, 'EVIDENCE', ?,
                        jsonb_build_object('subjectId', ?::text, 'capturedAt', '2026-01-01T00:00:00Z', 'sourceChannel', 'IVR'), 'v1',
                        jsonb_build_object('capture', '{}'::jsonb, 'content', '{}'::jsonb, 'sourceEventDomain', 'EVIDENCE',
                            'sourceEventId', ?::text, 'source', jsonb_build_object('subjectId', ?::text, 'recordingId', ?::text,
                            'callId', ?::text, 'sessionId', ?::text)), repeat('a', 64), ?, ?, 'SOLIDIFIED', now())
                    """, evidence, subject, source, eventId, subject, eventId, subject, source, callId, session, callId, session);
            CustomerMeFixture.execute(admin, "INSERT INTO evidence.evidence_event(id, evidence_id, event_domain, event_id, event_type, payload_redacted, payload_sha256) VALUES (?, ?, 'EVIDENCE', ?, 'EVIDENCE_CREATED', '{}'::jsonb, repeat('b',64))",
                    UUID.randomUUID(), evidence, eventId);
            UUID media = UUID.randomUUID();
            CustomerMeFixture.execute(admin, "INSERT INTO evidence.media_object(id, storage_provider, bucket, object_key, object_version_id, sha256, size_bytes, mime_type, crypto_key_ref) VALUES (?, 'COS', 'fixture', ?, 'v1', repeat('c',64), 1, 'audio/wav', 'fixture-key')",
                    media, SECRET + media);
            CustomerMeFixture.execute(admin, "INSERT INTO evidence.evidence_artifact(id, evidence_id, media_object_id, artifact_role) VALUES (?, ?, ?, 'DERIVED')",
                    UUID.randomUUID(), evidence, media);
            CustomerMeFixture.execute(admin, """
                    INSERT INTO sharing.evidence_share_grant(id, share_id, capability_token_hash, evidence_id,
                        created_by_account_id, owner_organization_id, verification_mode, valid_from, expires_at,
                        content_snapshot, content_sha256, code_generation, status)
                    SELECT ?, ?, ?, ?, ?, ?, 'NONE', ?, ?,
                        jsonb_build_object('content', jsonb_build_object('evidenceId', ?::text, 'canary', ?::text),
                        'verification', jsonb_build_object('mode', 'NONE'),
                        'creation', jsonb_build_object('creatorAccountId', ?::text, 'evidenceId', ?::text,
                        'ownerOrganizationId', ?::text, 'scopeDigest', repeat('d',64),
                        'creationRequestDigest', repeat('e',64), 'creationFingerprint', repeat('f',64))),
                        repeat('a',64), 0, 'ACTIVE'
                    """, UUID.randomUUID(), shareId, UUID.randomUUID().toString().replace("-", "").repeat(2), evidence,
                    creator, ownerOrganization, from.atOffset(ZoneOffset.UTC), from.plusSeconds(86400).atOffset(ZoneOffset.UTC),
                    evidence, SECRET, creator, evidence, ownerOrganization);
            admin.commit();
        }
        return shareId;
    }

    void organizationMembership(UUID org, UUID account) throws Exception {
        base.organization(org, "ACTIVE", "MEMBER", "ACTIVE", account);
    }

    void membership(UUID org, UUID account) throws Exception {
        try (Connection admin = base.admin()) {
            CustomerMeFixture.execute(admin, "SET ROLE organization_access_owner");
            CustomerMeFixture.execute(admin,
                    "INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role,status) VALUES (?, ?, ?, 'MEMBER', 'ACTIVE')",
                    UUID.randomUUID(), org, account);
        }
    }

    HttpResponse<String> shares(String session, String query) throws Exception {
        int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/customer/shares" + query))
                .timeout(java.time.Duration.ofSeconds(20)).header("Authorization", "Bearer " + session).GET().build();
        return base.client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    String encodedCursor(String cursor) {
        return "%" + String.format(java.util.Locale.ROOT, "%02X", (int) cursor.charAt(0)) + cursor.substring(1);
    }

    List<String> orderedShareIds(UUID creator, UUID ownerOrganization) throws Exception {
        try (Connection admin = base.admin(); var statement = admin.prepareStatement("""
                SELECT share_id FROM sharing.evidence_share_grant
                WHERE created_by_account_id = ? AND owner_organization_id IS NOT DISTINCT FROM ?
                ORDER BY valid_from DESC, share_id DESC
                """)) {
            statement.setObject(1, creator);
            statement.setObject(2, ownerOrganization);
            try (var rows = statement.executeQuery()) {
                List<String> ids = new ArrayList<>();
                while (rows.next()) ids.add(rows.getString(1));
                return List.copyOf(ids);
            }
        }
    }

    Map<String, String> snapshotSharingAndEvidenceRows() throws Exception {
        Map<String, String> result = new TreeMap<>(base.snapshot());
        try (Connection admin = base.admin(); var statement = admin.createStatement()) {
            for (String table : List.of("sharing.evidence_share_grant", "sharing.share_admission", "sharing.share_access_event",
                    "evidence.media_object", "evidence.recording", "evidence.evidence_record", "evidence.evidence_artifact", "evidence.evidence_event")) {
                try (var rows = statement.executeQuery("select coalesce(jsonb_agg(to_jsonb(row_value) order by to_jsonb(row_value)::text)::text, '[]') from " + table + " row_value")) {
                    rows.next(); result.put(table, rows.getString(1));
                }
            }
        }
        assertThat(result.keySet()).contains("customer_iam.customer_account", "organization_access.organization",
                "platform.idempotency_record", "platform.outbox_event", "audit.audit_fact",
                "sharing.evidence_share_grant", "sharing.share_admission", "sharing.share_access_event",
                "evidence.media_object", "evidence.recording", "evidence.evidence_record", "evidence.evidence_artifact", "evidence.evidence_event");
        Map<String, String> hashes = new TreeMap<>();
        result.forEach((table, rows) -> {
            try {
                byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(rows.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                hashes.put(table, java.util.HexFormat.of().formatHex(digest));
            } catch (java.security.NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
        });
        return Map.copyOf(hashes);
    }

    void assertSharingReadOnlyWriteDenied(String shareId) throws Exception {
        Map<String, String> before = snapshotSharingAndEvidenceRows();
        try (Connection connection = DriverManager.getConnection(base.url, base.sharingLogin, base.password)) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            try {
                CustomerMeFixture.execute(connection, "SET LOCAL ROLE sharing_role");
                try (var statement = connection.createStatement(); var rows = statement.executeQuery("select current_user, current_setting('transaction_read_only')")) {
                    rows.next(); assertThat(rows.getString(1)).isEqualTo("sharing_role"); assertThat(rows.getString(2)).isEqualTo("on");
                }
                assertThatThrownBy(() -> CustomerMeFixture.execute(connection,
                        "UPDATE sharing.evidence_share_grant SET revision=revision+1 WHERE share_id=?", shareId))
                        .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("25006"));
            } finally {
                connection.rollback();
                connection.setReadOnly(false);
                connection.setAutoCommit(true);
                assertThat(connection.isReadOnly()).isFalse();
                assertThat(connection.getAutoCommit()).isTrue();
            }
        }
        try (Connection connection = DriverManager.getConnection(base.url, base.sharingLogin, base.password)) {
            connection.setAutoCommit(false);
            try {
                CustomerMeFixture.execute(connection, "SET LOCAL ROLE sharing_role");
                Savepoint probe = connection.setSavepoint();
                try {
                    assertThatThrownBy(() -> CustomerMeFixture.execute(connection,
                            "UPDATE sharing.evidence_share_grant SET revision=revision+1 WHERE share_id=?", shareId))
                            .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
                } finally {
                    connection.rollback(probe);
                }
            } finally {
                connection.rollback();
            }
        }
        assertThat(before).isEqualTo(snapshotSharingAndEvidenceRows());
    }

    @Override
    public void close() {
        queryLogger.detachAppender(serviceLogs);
        serviceLogs.stop();
        if (context != null) context.close();
        CustomerMeFixture.SESSIONS.clear();
        base.close();
    }
}
