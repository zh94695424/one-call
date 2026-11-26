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

final class CustomerCatalogOffersFixture implements AutoCloseable {
    final String password = UUID.randomUUID().toString();
    final GenericContainer<?> postgres = new GenericContainer<>(DockerImageName.parse("postgres:16"))
            .withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", password).withEnv("POSTGRES_DB", "pa27")
            .withEnv("POSTGRES_INITDB_ARGS", "--lc-collate=C --lc-ctype=C").withExposedPorts(5432)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2));
    static final Map<String, VerifiedCustomerSession> SESSIONS = new ConcurrentHashMap<>();
    final UUID account = UUID.fromString("10000000-0000-0000-0000-000000000001");
    final UUID other = UUID.fromString("10000000-0000-0000-0000-000000000002");
    final UUID organization = UUID.fromString("20000000-0000-0000-0000-000000000001");
    final HttpClient client = HttpClient.newHttpClient();
    final List<Seed> seeds = new ArrayList<>();
    String url;
    ConfigurableApplicationContext context;
    UUID employee;

    record Seed(UUID productId, UUID versionId, UUID offerId, String code, int versionNo, String saleStatus, String type,
            String audience, String channel, String offerStatus, Long price, String currency, String cycle, int installments,
            String token, List<Benefit> benefits) { }
    record Benefit(String type, long quantity, int validityDays, int interval) { }

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

    void start() throws Exception {
        postgres.start();
        url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/pa27";
        var flyway = Flyway.configure().dataSource(url, "fixture_admin", password).locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(20);
        flyway.validate();
        postgres.copyFileToContainer(MountableFile.forHostPath(Path.of("ops/postgresql/pa25-runtime-identities.sql").toAbsolutePath()), "/tmp/pa27-identities.sql");
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
                        "onecall.customer.catalog-offers.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]),
                        "onecall.customer.business-numbers.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]),
                        "onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]))))).run();
        context.getBean("customerQueryDataSource", HikariDataSource.class).setMaximumPoolSize(1);
    }

    void seed() throws Exception {
        seeds.clear(); SESSIONS.clear();
        try (var connection = admin()) {
            execute(connection, "TRUNCATE customer_iam.customer_account, organization_access.organization, catalog_order.catalog_product, operations_iam.employee_account CASCADE");
            execute(connection, "SET ROLE customer_iam_owner");
            execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?, 'Reader'), (?, 'Other')", account, other);
            execute(connection, "SET ROLE organization_access_owner");
            execute(connection, "INSERT INTO organization_access.organization(id,display_name) VALUES (?, 'Org')", organization);
            execute(connection, "INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role,status) VALUES (?, ?, ?, 'ADMINISTRATOR', 'ACTIVE')", UUID.randomUUID(), organization, account);
            execute(connection, "SET ROLE operations_iam_owner");
            employee = UUID.randomUUID();
            execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role,status) VALUES (?,repeat('a',64),'fixture','fixture',repeat('b',64),'PRIVATE_EMPLOYEE','ADMIN','ACTIVE')", employee);
        }
        session("personal", account, null);
        session("enterprise", account, organization);
        session("other", other, null);
        personal("ALPHA", 2, "SUBSCRIPTION", 1200L, "MONTHLY", List.of(new Benefit("AI_SECONDS", 10, 30, 1), new Benefit("EVIDENCE_COUNT", 3, 30, 2)));
        personal("BRAVO", 1, "ONE_TIME", 500L, "ONCE", List.of());
        personal("CHARLIE", 3, "ADD_ON", 300L, "ONCE", List.of(new Benefit("STORAGE_BYTES", 1024, 30, 0)));
        personal("DELTA", 1, "SUBSCRIPTION", 9000L, "ANNUAL", List.of(new Benefit("RECORDING_SECONDS", 60, 30, 1)));
        personal("ECHO", 1, "ONE_TIME", 0L, "ONCE", List.of(new Benefit("EVIDENCE_COUNT", 5, 30, 0), new Benefit("RECORDING_SECONDS", 120, 30, 1)));
        personal("FOXTROT", 1, "ADD_ON", 100L, "ONCE", List.of());
        enterprise("GOLF", 1, List.of(new Benefit("AI_SECONDS", 20, 30, 1)));
        enterprise("HOTEL", 2, List.of());
        enterprise("INDIA", 1, List.of(new Benefit("EVIDENCE_COUNT", 8, 30, 2), new Benefit("STORAGE_BYTES", 2048, 30, 0)));
        enterprise("NOVEMBER", 1, List.of(new Benefit("RECORDING_SECONDS", 30, 30, 1)));
        enterprise("PAPA", 1, List.of());
        hidden("JULIETT", "DRAFT", "PERSONAL", "SUBSCRIPTION", "WEB", "ACTIVE", 1200L);
        hidden("KILO", "OFFSALE", "PERSONAL", "SUBSCRIPTION", "WEB", "ACTIVE", 1200L);
        hidden("LIMA", "ACTIVE", "PERSONAL", "SUBSCRIPTION", "WEB", "WITHDRAWN", 1200L);
        hidden("MIKE", "ACTIVE", "PERSONAL", "SUBSCRIPTION", "MINIAPP", "ACTIVE", 1200L);
        hidden("QUEBEC", "ACTIVE", "PERSONAL", "SUBSCRIPTION", "MINIAPP", "ACTIVE", 700L);
        try (var connection = admin()) {
            execute(connection, "SET ROLE catalog_order_owner");
            execute(connection, "INSERT INTO catalog_order.catalog_offer(id,catalog_version_id,channel,offer_status,version_token) VALUES (?,?,'MINIAPP','ACTIVE','lima-miniapp')",
                    UUID.randomUUID(), seedByCode("LIMA").versionId());
        }
    }

    Seed seedByCode(String code) { return seeds.stream().filter(seed -> seed.code().equals(code)).findFirst().orElseThrow(); }

    private void personal(String code, int versionNo, String type, Long price, String cycle, List<Benefit> benefits) throws Exception {
        insert(code, versionNo, "ACTIVE", type, "PERSONAL", "WEB", "ACTIVE", price, "CNY", cycle, 1, benefits);
    }
    private void enterprise(String code, int versionNo, List<Benefit> benefits) throws Exception {
        insert(code, versionNo, "ACTIVE", "QUOTE", "ENTERPRISE", "WEB", "ACTIVE", null, "CNY", "QUOTE", 1, benefits);
    }
    private void hidden(String code, String saleStatus, String audience, String type, String channel, String offerStatus, Long price) throws Exception {
        insert(code, 1, saleStatus, type, audience, channel, offerStatus, price, "CNY", type.equals("SUBSCRIPTION") ? "MONTHLY" : "ONCE", 1, List.of());
    }

    void insert(String code, int versionNo, String saleStatus, String type, String audience, String channel, String offerStatus,
            Long price, String currency, String cycle, int installments, List<Benefit> benefits) throws Exception {
        UUID productId = UUID.randomUUID(); UUID versionId = UUID.randomUUID(); UUID offerId = UUID.randomUUID();
        String token = "stored-" + code.toLowerCase(Locale.ROOT) + "-" + channel.toLowerCase(Locale.ROOT);
        try (var connection = admin()) {
            execute(connection, "SET ROLE catalog_order_owner");
            execute(connection, "INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (?,?,'STANDARD')", productId, code);
            execute(connection, "INSERT INTO catalog_order.catalog_version(id,product_id,version_no,sale_status,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,created_by_employee_id,published_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,'{}',repeat('a',64),?,?::timestamptz)",
                    versionId, productId, versionNo, saleStatus, type, audience, price, currency, cycle, installments,
                    type.equals("SUBSCRIPTION") ? 1 : 0, employee, saleStatus.equals("DRAFT") ? null : "2026-11-23T00:00:00Z");
            execute(connection, "INSERT INTO catalog_order.catalog_offer(id,catalog_version_id,channel,offer_status,version_token) VALUES (?,?,?,?,?)", offerId, versionId, channel, offerStatus, token);
            for (Benefit benefit : benefits)
                execute(connection, "INSERT INTO catalog_order.catalog_benefit_line(id,catalog_version_id,entitlement_type,quantity,validity_days,grant_interval_months) VALUES (?,?,?,?,?,?)",
                        UUID.randomUUID(), versionId, benefit.type(), benefit.quantity(), benefit.validityDays(), benefit.interval());
        }
        seeds.add(new Seed(productId, versionId, offerId, code, versionNo, saleStatus, type, audience, channel, offerStatus, price, currency, cycle, installments, token, benefits));
    }

    List<UUID> expected(String audience) {
        return seeds.stream().filter(seed -> seed.saleStatus().equals("ACTIVE") && seed.offerStatus().equals("ACTIVE")
                && seed.channel().equals("WEB") && seed.audience().equals(audience))
                .sorted(Comparator.comparing(Seed::code).thenComparing(Comparator.comparingInt(Seed::versionNo).reversed())
                        .thenComparing(seed -> seed.offerId().toString()))
                .map(Seed::offerId).toList();
    }

    void session(String token, UUID customer, UUID organizationId) {
        SESSIONS.put(token, new VerifiedCustomerSession(customer, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1",
                new SelectedCustomerWorkspace(organizationId == null ? CustomerContext.WorkspaceType.PERSONAL : CustomerContext.WorkspaceType.ORGANIZATION, organizationId)));
    }

    HttpResponse<String> get(String token, String query) throws Exception {
        int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/customer/catalog/offers" + query))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode json(String token, String query, int status) throws Exception {
        var response = get(token, query);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.body()).doesNotContain(password, "CANARY", "snapshot", "revision", "created_by_employee_id", "product_family");
        var json = JsonMapper.builder().build().readTree(response.body());
        assertThat(json.propertyNames()).containsExactlyInAnyOrderElementsOf(status == 200 ? List.of("items", "nextCursor", "hasMore") : List.of("type", "title", "status", "code", "traceId"));
        return json;
    }

    List<UUID> ids(JsonNode page) {
        List<UUID> result = new ArrayList<>();
        for (JsonNode item : page.path("items")) {
            assertThat(item.propertyNames()).containsExactlyInAnyOrder("productCode", "catalogVersionId", "catalogOfferId", "channel",
                    "versionNo", "offerType", "audience", "priceCents", "currency", "billingCycle", "installments", "benefitLines", "versionToken");
            result.add(UUID.fromString(item.path("catalogOfferId").asString()));
        }
        return result;
    }

    Connection admin() throws SQLException { return DriverManager.getConnection(url, "fixture_admin", password); }
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
