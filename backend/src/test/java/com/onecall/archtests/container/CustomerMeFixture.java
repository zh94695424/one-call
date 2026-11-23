package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel;
import com.onecall.core.customer_iam.internal.security.*;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class CustomerMeFixture implements AutoCloseable {
    final String password = UUID.randomUUID().toString();
    final String login = "customer_" + UUID.randomUUID().toString().replace("-", "");
    final String sharingLogin = "sharing_" + UUID.randomUUID().toString().replace("-", "");
    final String deniedLogin = "denied_" + UUID.randomUUID().toString().replace("-", "");
    final GenericContainer<?> postgres = new GenericContainer<>(DockerImageName.parse("postgres:16"))
            .withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", password)
            .withEnv("POSTGRES_DB", "customer_me").withEnv("POSTGRES_INITDB_ARGS", "--lc-collate=C --lc-ctype=C")
            .withExposedPorts(5432).waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
            .withStartupTimeout(Duration.ofMinutes(2));
    static final Map<String, VerifiedCustomerSession> SESSIONS = new ConcurrentHashMap<>();
    static volatile boolean verifierOutage;
    final List<Execution> executions = new CopyOnWriteArrayList<>();
    final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();
    final List<HikariDataSource> allPools = new CopyOnWriteArrayList<>();
    volatile boolean disconnectNext;
    volatile boolean failCommit;
    final AtomicInteger acquisitions = new AtomicInteger();
    final AtomicInteger commits = new AtomicInteger();
    final AtomicInteger rollbacks = new AtomicInteger();
    final HttpClient client = HttpClient.newHttpClient();
    final UUID account = UUID.randomUUID();
    final UUID other = UUID.randomUUID();
    String url;
    ConfigurableApplicationContext context;
    volatile String failOn;
    volatile boolean writeProbe;
    volatile String writeState;
    record Execution(String sql, List<Object> bindings, int checkout, String role, String session,
            String readOnly, String isolation, int pid) { }

    @TestConfiguration(proxyBeanMethods = false)
    static class DualPoolTestBeans {
        @Bean
        CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> {
                if (verifierOutage) { throw new IllegalStateException("VERIFIER_CANARY"); }
                VerifiedCustomerSession session = SESSIONS.get(bearer.value());
                if (session == null) {
                    throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                }
                return session;
            };
        }
    }

    void start() throws Exception {
        long start = System.nanoTime();
        postgres.start();
        url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/customer_me";
        Flyway flyway = Flyway.configure().dataSource(url, "fixture_admin", password).locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(19);
        flyway.validate();
        assertThat(flyway.info().applied()).hasSize(19);
        try (Connection connection = admin()) {
            for (String identity : List.of(login, sharingLogin, deniedLogin)) {
                execute(connection, "CREATE ROLE " + identity + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + password + "'");
            }
            execute(connection, "GRANT customer_iam_role TO " + login);
            execute(connection, "GRANT sharing_role TO " + sharingLogin);
            try (var statement = connection.createStatement(); var facts = statement.executeQuery(
                    "select current_setting('server_version'), current_setting('server_version_num')::int, (select datcollate from pg_database where datname=current_database()), current_setting('TimeZone')")) {
                facts.next();
                assertThat(facts.getInt(2)).isBetween(160000, 169999);
                System.out.println("PA11 PostgreSQL version=" + facts.getString(1) + " collation=" + facts.getString(3)
                        + " zone=" + facts.getString(4) + " image=" + postgres.getDockerClient().inspectImageCmd("postgres:16").exec().getId()
                        + " startupMs=" + (System.nanoTime() - start) / 1_000_000);
            }
        }
        context = startContext(false, login);
    }

    ConfigurableApplicationContext startContext(boolean dual, String customerLogin) {
        return new SpringApplicationBuilder(OneCallApplication.class, DualPoolTestBeans.class)
                .profiles(dual ? new String[]{"web", "persistence"} : new String[]{"web"})
                .properties("server.port=0", "spring.main.banner-mode=off")
                .initializers(application -> {
                    application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("fixture-query", Map.of(
                            "onecall.customer.persistence.url", url, "onecall.customer.persistence.username", customerLogin,
                            "onecall.customer.persistence.password", password, "onecall.persistence.url", url,
                            "onecall.persistence.username", sharingLogin, "onecall.persistence.password", password)));
                    application.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
                        @Override
                        public Object postProcessAfterInitialization(Object bean, String name) {
                            if (!(bean instanceof HikariDataSource source)) { return bean; }
                            source.setMaximumPoolSize(1);
                            source.setConnectionTimeout(5000);
                            allPools.add(source);
                            if (!dual && customerLogin.equals(login)) { pools.put(name, source); }
                            return Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class, AutoCloseable.class},
                                    (proxy, method, arguments) -> {
                                        try {
                                            Object result = method.invoke(source, arguments);
                                            if (result instanceof Connection connection && method.getName().equals("getConnection")) {
                                                int checkout = acquisitions.incrementAndGet();
                                                try (var statement = connection.createStatement()) { statement.execute("SET statement_timeout = '20s'"); }
                                                return captureConnection(connection, checkout);
                                            }
                                            return result;
                                        } catch (InvocationTargetException failure) { throw failure.getCause(); }
                                    });
                        }
                    });
                }).run();
    }

    void reset() throws Exception {
        failOn = null;
        disconnectNext = false;
        failCommit = false;
        writeProbe = false;
        writeState = null;
        verifierOutage = false;
        SESSIONS.clear();
        SESSIONS.put("first", new VerifiedCustomerSession(account, Channel.WEB, "onecall:customer-session:v1"));
        SESSIONS.put("second", new VerifiedCustomerSession(other, Channel.MINIAPP, "onecall:customer-session:v1"));
        try (var connection = admin()) {
            execute(connection, "TRUNCATE customer_iam.customer_account, organization_access.organization CASCADE");
            execute(connection, "SET ROLE customer_iam_owner");
            execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?, 'First'), (?, 'Second')", account, other);
            execute(connection, "INSERT INTO customer_iam.auth_identity(id,account_id,provider,provider_scope,provider_subject_ciphertext,provider_subject_hmac,verified_at,key_version) VALUES (?,?,'PHONE','fixture','v1.fixture.IDENTITY_CANARY',?,now(),1)",
                    UUID.randomUUID(), account, "a".repeat(64));
            execute(connection, "INSERT INTO customer_iam.identity_profile(id,account_id,real_name_status,legal_name_ciphertext,id_number_ciphertext,id_number_hmac,verification_ref_ciphertext,verified_at) VALUES (?,?,'VERIFIED','v1.fixture.NAME_CANARY','v1.fixture.ID_CANARY',?,'v1.fixture.VERIFICATION_CANARY',now())",
                    UUID.randomUUID(), account, "b".repeat(64));
            execute(connection, "RESET ROLE");
        }
        clearCapture();
    }

    void clearCapture() { executions.clear(); acquisitions.set(0); commits.set(0); rollbacks.set(0); }
    Connection admin() throws SQLException { return DriverManager.getConnection(url, "fixture_admin", password); }
    Connection runtime() throws SQLException { return DriverManager.getConnection(url, login, password); }

    void organization(UUID organization, String status, String role, String membershipStatus, UUID customer) throws Exception {
        try (var connection = admin()) {
            execute(connection, "SET ROLE organization_access_owner");
            execute(connection, "INSERT INTO organization_access.organization(id,display_name,status) VALUES (?, ?, ?)", organization, "Org-" + organization, status);
            execute(connection, "INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role,status) VALUES (?, ?, ?, ?, ?)",
                    UUID.randomUUID(), organization, customer, role, membershipStatus);
        }
    }

    void update(String owner, String sql, Object... values) throws Exception {
        try (var connection = admin()) { execute(connection, "SET ROLE " + owner); execute(connection, sql, values); }
    }

    HttpResponse<String> get(String session) throws Exception { return get(context, session, "", false); }
    HttpResponse<String> get(ConfigurableApplicationContext application, String session, String query, boolean spoof) throws Exception {
        int port = ((ServletWebServerApplicationContext) application).getWebServer().getPort();
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/customer/me" + query))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + session);
        if (spoof) {
            builder.header("X-Account-Id", other.toString()).header("X-Organization-Id", UUID.randomUUID().toString())
                    .header("X-Role", "ADMINISTRATOR").header("X-Channel", "IVR").header("X-Forwarded-User", "HTTP_CANARY")
                    .header("Cookie", "accountId=HTTP_CANARY").method("GET", HttpRequest.BodyPublishers.ofString("{\"accountId\":\"HTTP_CANARY\"}"));
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode json(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(response.body()).doesNotContain(password, "CANARY", "jdbc:", "timestamp", "exception");
        var json = JsonMapper.builder().build().readTree(response.body());
        if (status != 200) {
            assertThat(json.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
            assertThat(json.get("status").asInt()).isEqualTo(status);
        }
        return json;
    }

    Map<String, String> snapshot() throws Exception {
        Map<String, String> snapshot = new TreeMap<>();
        try (var connection = admin()) {
            for (String table : List.of("customer_iam.customer_account", "customer_iam.auth_identity", "customer_iam.identity_profile",
                    "organization_access.organization", "organization_access.organization_membership", "platform.idempotency_record",
                    "platform.outbox_event", "audit.audit_fact")) {
                try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                        "select coalesce(jsonb_agg(to_jsonb(row_value) order by to_jsonb(row_value)::text)::text, '[]') from " + table + " row_value")) {
                    rows.next(); snapshot.put(table, rows.getString(1));
                }
            }
        }
        return snapshot;
    }

    void assertPoolClean(HikariDataSource source, String expectedLogin) throws Exception {
        try (var connection = source.getConnection(); var statement = connection.createStatement();
                var rows = statement.executeQuery("select current_user, session_user, current_setting('transaction_read_only'), current_setting('transaction_isolation')")) {
            rows.next();
            assertThat(rows.getString(1)).isEqualTo(expectedLogin);
            assertThat(rows.getString(2)).isEqualTo(expectedLogin);
            assertThat(rows.getString(3)).isEqualTo("off");
            assertThat(rows.getString(4)).isEqualTo("read committed");
            assertThat(connection.getAutoCommit()).isTrue();
        }
        assertThat(source.getHikariPoolMXBean().getActiveConnections()).isZero();
    }

    private Connection captureConnection(Connection connection, int checkout) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
            try {
                if (method.getName().equals("commit")) {
                    commits.incrementAndGet();
                    if (failCommit) { throw new SQLException("COMMIT_CANARY", "08006"); }
                }
                if (method.getName().equals("rollback")) { rollbacks.incrementAndGet(); }
                Object result = method.invoke(connection, arguments);
                if (result instanceof PreparedStatement statement && method.getName().equals("prepareStatement")) {
                    return captureStatement(connection, statement, (String) arguments[0], checkout);
                }
                return result;
            } catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
    }

    private PreparedStatement captureStatement(Connection connection, PreparedStatement statement, String sql, int checkout) {
        Map<Integer, Object> values = new TreeMap<>();
        return (PreparedStatement) Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class}, (proxy, method, arguments) -> {
            try {
                if (method.getName().startsWith("set") && arguments != null && arguments.length >= 2 && arguments[0] instanceof Integer index) {
                    values.put(index, arguments[1]);
                }
                if (method.getName().startsWith("execute")) {
                    if (failOn != null && sql.toLowerCase(Locale.ROOT).contains(failOn)) { throw new SQLException("JDBC_CANARY " + password, "08006"); }
                    if (disconnectNext && sql.contains("customer_account")) {
                        disconnectNext = false;
                        int backend;
                        try (var probe = connection.createStatement(); var rows = probe.executeQuery("select pg_backend_pid()")) {
                            rows.next(); backend = rows.getInt(1);
                        }
                        try (var owner = admin()) { execute(owner, "select pg_terminate_backend(?)", backend); }
                    }
                    try (var probe = connection.createStatement(); var facts = probe.executeQuery(
                            "select current_user, session_user, current_setting('transaction_read_only'), current_setting('transaction_isolation'), pg_backend_pid()")) {
                        facts.next();
                        executions.add(new Execution(sql, List.copyOf(values.values()), checkout, facts.getString(1), facts.getString(2),
                                facts.getString(3), facts.getString(4), facts.getInt(5)));
                    }
                    if (writeProbe && sql.contains("customer_account")) {
                        try (var probe = connection.createStatement()) {
                            probe.executeUpdate("UPDATE customer_iam.customer_account SET revision=revision+1");
                        } catch (SQLException denied) { writeState = denied.getSQLState(); throw denied; }
                    }
                }
                return method.invoke(statement, arguments);
            } catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
    }

    static void execute(Connection connection, String sql, Object... arguments) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < arguments.length; index++) { statement.setObject(index + 1, arguments[index]); }
            statement.execute();
        }
    }

    @Override
    public void close() {
        try {
            if (context != null) { context.close(); }
            allPools.forEach(HikariDataSource::close);
            assertThat(allPools).allSatisfy(pool -> assertThat(pool.isClosed()).isTrue());
        } finally { client.close(); SESSIONS.clear(); postgres.stop(); }
    }
}
