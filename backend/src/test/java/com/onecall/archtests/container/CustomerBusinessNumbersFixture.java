package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.*;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.flywaydb.core.Flyway;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class CustomerBusinessNumbersFixture implements AutoCloseable {
    final String password = UUID.randomUUID().toString();
    final GenericContainer<?> postgres = new GenericContainer<>(DockerImageName.parse("postgres:16"))
            .withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", password).withEnv("POSTGRES_DB", "pa27")
            .withEnv("POSTGRES_INITDB_ARGS", "--lc-collate=C --lc-ctype=C").withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));
    static final Map<String, VerifiedCustomerSession> SESSIONS = new ConcurrentHashMap<>();
    final UUID account = UUID.fromString("10000000-0000-0000-0000-000000000001");
    final UUID other = UUID.fromString("10000000-0000-0000-0000-000000000002");
    final List<UUID> organizations = java.util.stream.IntStream.rangeClosed(1, 5)
            .mapToObj(index -> UUID.fromString("20000000-0000-0000-0000-%012d".formatted(index))).toList();
    final Map<UUID, List<UUID>> expected = new LinkedHashMap<>();
    final Instant time = Instant.parse("2026-11-11T00:00:00.123456Z");
    final HttpClient client = HttpClient.newHttpClient();
    String url;
    ConfigurableApplicationContext context;
    int sequence;

    @TestConfiguration(proxyBeanMethods = false)
    static class SessionBeans {
        @Bean
        CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> {
                var session = SESSIONS.get(bearer.value());
                if (session == null) throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                return session;
            };
        }
    }

    void start(String target) throws Exception {
        postgres.start();
        url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/pa27";
        var configuration = Flyway.configure().dataSource(url, "fixture_admin", password).locations("classpath:db/migration");
        if (target != null) configuration.target(target);
        var flyway = configuration.load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(target == null ? 19 : Integer.parseInt(target));
        flyway.validate();
        postgres.copyFileToContainer(MountableFile.forHostPath(Path.of("ops/postgresql/pa25-runtime-identities.sql").toAbsolutePath()), "/tmp/pa27-identities.sql");
        if (target == null) provision();
    }

    void provision() throws Exception {
        assertThat(psql("apply").getExitCode()).isZero();
        assertThat(psql("check").getExitCode()).isZero();
        try (var connection = admin()) { execute(connection, "ALTER ROLE pa27_customer PASSWORD '" + password + "'"); }
    }

    org.testcontainers.containers.Container.ExecResult psql(String mode) throws Exception {
        return postgres.execInContainer("psql", "-X", "-w", "-U", "fixture_admin", "-d", "pa27", "-v", "mode=" + mode,
                "-v", "sharing_login=pa27_query", "-v", "customer_login=pa27_customer", "-v", "command_login=pa27_command", "-v", "operations_login=pa36_operations", "-f", "/tmp/pa27-identities.sql");
    }

    void web() {
        context = new SpringApplicationBuilder(OneCallApplication.class, SessionBeans.class).profiles("web")
                .properties("server.port=0", "spring.main.banner-mode=off")
                .initializers(application -> application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa27", Map.of(
                        "onecall.customer.persistence.url", url, "onecall.customer.persistence.username", "pa27_customer",
                        "onecall.customer.persistence.password", password,
                        "onecall.customer.business-numbers.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]),
                        "onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]))))).run();
        context.getBean("customerQueryDataSource", HikariDataSource.class).setMaximumPoolSize(1);
    }

    void seed() throws Exception {
        sequence = 0; expected.clear(); SESSIONS.clear();
        try (var connection = admin()) {
            execute(connection, "TRUNCATE customer_iam.customer_account, organization_access.organization, subject_number.business_subject CASCADE");
            execute(connection, "SET ROLE customer_iam_owner");
            execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?, 'Reader'), (?, 'Other')", account, other);
            execute(connection, "SET ROLE organization_access_owner");
            var roles = List.of("ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR");
            for (int index = 0; index < organizations.size(); index++) {
                UUID organization = organizations.get(index);
                execute(connection, "INSERT INTO organization_access.organization(id,display_name) VALUES (?, 'Org')", organization);
                if (index < 4) execute(connection, "INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role,status) VALUES (?, ?, ?, ?, 'ACTIVE')", UUID.randomUUID(), organization, account, roles.get(index));
                execute(connection, "INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role,status) VALUES (?, ?, ?, 'MEMBER', 'ACTIVE')", UUID.randomUUID(), organization, other);
            }
        }
        session("personal", account, null);
        for (int index = 0; index < 5; index++) session("org" + index, account, organizations.get(index));
        for (UUID organization : organizations.subList(0, 4)) matrix(organization);
        matrix(null);
        seedRow(other, null, "ACTIVE", "ACTIVE", "VERIFIED", "MOBILE", time, UUID.randomUUID(), "FOREIGN_MASK");
        seedRow(null, organizations.get(4), "ACTIVE", "ACTIVE", "VERIFIED", "MOBILE", time, UUID.randomUUID(), "FOREIGN_ORG_MASK");
        seedRow(null, null, "ACTIVE", "ACTIVE", "VERIFIED", "MOBILE", time, UUID.randomUUID(), "UNOWNED_MASK");
    }

    void session(String token, UUID customer, UUID organization) {
        SESSIONS.put(token, new VerifiedCustomerSession(customer, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1",
                new SelectedCustomerWorkspace(organization == null ? CustomerContext.WorkspaceType.PERSONAL : CustomerContext.WorkspaceType.ORGANIZATION, organization)));
    }

    void matrix(UUID organization) throws Exception {
        List<UUID> ids = new ArrayList<>();
        for (String binding : List.of("ACTIVE", "DISABLED", "RETIRED")) for (String activation : List.of("MISSING", "INACTIVE", "ACTIVE", "SUSPENDED"))
            for (String realName : List.of("UNVERIFIED", "VERIFIED")) for (String type : List.of("MOBILE", "LANDLINE")) {
                UUID id = new UUID(sequence % 2 == 0 ? 0x7fffffff00000000L : 0x8000000000000000L, ++sequence);
                seedRow(organization == null ? account : null, organization, binding, activation, realName, type, time, id, "138****" + sequence);
                ids.add(id);
            }
        ids.sort(Comparator.comparing(UUID::toString)); expected.put(organization == null ? account : organization, List.copyOf(ids));
    }

    UUID seedRow(UUID owner, UUID organization, String binding, String activation, String realName, String type, Instant createdAt, UUID bindingId, String mask) throws Exception {
        UUID subject = UUID.randomUUID();
        try (var connection = admin()) {
            execute(connection, "SET ROLE subject_number_owner");
            execute(connection, "INSERT INTO subject_number.business_subject(id,owner_account_id,owner_organization_id,source,real_name_status,real_name_verified_at,created_at,updated_at) VALUES (?, ?, ?, 'WEB', ?, ?, ?, ?)",
                    subject, owner, organization, realName, realName.equals("VERIFIED") ? createdAt.atOffset(ZoneOffset.UTC) : null, createdAt.atOffset(ZoneOffset.UTC), createdAt.atOffset(ZoneOffset.UTC));
            execute(connection, "INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,status,verified_at) VALUES (?, ?, ?, 'v1.fixture.CIPHERTEXT_CANARY', ?, ?, ?, ?)",
                    bindingId, subject, type, UUID.randomUUID().toString().replace("-", "").repeat(2), mask, binding, binding.equals("ACTIVE") ? createdAt.atOffset(ZoneOffset.UTC) : null);
            if (!activation.equals("MISSING")) {
                execute(connection, "SET ROLE activation_consent_owner");
                execute(connection, "INSERT INTO activation_consent.service_activation(id,subject_id,status) VALUES (?, ?, ?)", UUID.randomUUID(), subject, activation);
            }
        }
        return subject;
    }

    HttpResponse<String> get(String token, String query) throws Exception {
        int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/customer/business-numbers" + query))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode json(String token, String query, int status) throws Exception {
        var response = get(token, query);
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.body()).doesNotContain(password, "CANARY", "FOREIGN_MASK", "FOREIGN_ORG_MASK", "UNOWNED_MASK", "ownerAccountId", "ownerOrganizationId", "createdAt", "numberHmac");
        var json = JsonMapper.builder().build().readTree(response.body());
        assertThat(json.propertyNames()).containsExactlyInAnyOrderElementsOf(status == 200 ? List.of("items", "nextCursor", "hasMore") : List.of("type", "title", "status", "code", "traceId"));
        return json;
    }

    List<UUID> ids(JsonNode page) {
        List<UUID> result = new ArrayList<>();
        for (JsonNode item : page.path("items")) {
            assertThat(item.propertyNames()).containsExactlyInAnyOrder("bindingId", "subjectId", "numberMasked", "numberType", "bindingStatus", "activationStatus", "realNameStatus");
            result.add(UUID.fromString(item.path("bindingId").asString()));
        }
        return result;
    }

    Connection admin() throws SQLException { return DriverManager.getConnection(url, "fixture_admin", password); }
    Connection runtime() throws SQLException { return DriverManager.getConnection(url, "pa27_customer", password); }
    static void execute(Connection connection, String sql, Object... parameters) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) statement.setObject(index + 1, parameters[index]);
            statement.execute();
        }
    }
    static String one(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) { assertThat(rows.next()).isTrue(); return rows.getString(1); }
    }
    String businessSnapshot() throws Exception {
        StringBuilder result = new StringBuilder();
        try (var connection = admin(); var statement = connection.createStatement(); var tables = statement.executeQuery("SELECT n.nspname||'.'||c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind='r' AND n.nspname NOT IN ('public','information_schema') AND n.nspname !~ '^pg_' ORDER BY 1")) {
            while (tables.next()) result.append(tables.getString(1)).append(one(connection, "SELECT coalesce(jsonb_agg(to_jsonb(value) ORDER BY to_jsonb(value)::text),'[]'::jsonb)::text FROM " + tables.getString(1) + " value"));
            result.append(one(connection, "SELECT coalesce(jsonb_agg(to_jsonb(value) ORDER BY schemaname,sequencename),'[]'::jsonb)::text FROM pg_sequences value WHERE schemaname !~ '^pg_'"));
        }
        return result.toString();
    }
    void cleanPool() throws Exception {
        var pool = context.getBean("customerQueryDataSource", HikariDataSource.class);
        try (var connection = pool.getConnection()) {
            assertThat(one(connection, "SELECT current_user||'|'||session_user||'|'||current_setting('transaction_read_only')||'|'||current_setting('transaction_isolation')")).isEqualTo("pa27_customer|pa27_customer|off|read committed");
            assertThat(connection.getAutoCommit()).isTrue(); assertThat(connection.isReadOnly()).isFalse();
        }
        assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
    }
    @Override
    public void close() { if (context != null) context.close(); client.close(); SESSIONS.clear(); postgres.stop(); }
}
