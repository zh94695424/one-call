package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.one;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.*;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class CustomerOrdersFixture implements AutoCloseable {
    static final String ROUTE = "/api/v1/customer/orders", CONTENT = "a".repeat(64);
    static final List<String> TABLES = List.of("catalog_order.catalog_product", "catalog_order.catalog_version", "catalog_order.catalog_offer", "catalog_order.catalog_benefit_line",
            "catalog_order.customer_order", "activation_consent.consent_record", "platform.idempotency_record", "platform.outbox_event", "audit.audit_fact",
            "catalog_order.payment_order", "catalog_order.payment_event", "entitlement_metering.entitlement_issuance", "entitlement_metering.entitlement_grant", "entitlement_metering.entitlement_balance", "entitlement_metering.trial_grant", "activation_consent.service_activation", "activation_consent.service_activation_transition");
    static final Map<String, VerifiedCustomerSession> SESSIONS = new java.util.concurrent.ConcurrentHashMap<>();
    final CustomerBusinessNumbersFixture base = new CustomerBusinessNumbersFixture();
    final UUID account = UUID.randomUUID(), subject = UUID.randomUUID(), secondSubject = UUID.randomUUID(), employee = UUID.randomUUID();
    final UUID product = UUID.randomUUID(), version = UUID.randomUUID(), offer = UUID.randomUUID(), line = UUID.randomUUID();
    final JsonMapper mapper = JsonMapper.builder().build();
    final HttpClient client = HttpClient.newHttpClient();
    final List<Entry> entries = new CopyOnWriteArrayList<>();
    volatile java.util.function.Consumer<ExecuteContext> before, after;
    ConfigurableApplicationContext context;
    DSLContext dsl;
    HikariDataSource pool;
    int port;
    record Entry(String sql, List<Object> binds, String login, String role, int pid, long txid) {}
    @TestConfiguration(proxyBeanMethods = false)
    static class SessionBeans {
        @Bean CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> {
                var session = SESSIONS.get(bearer.value());
                if (session == null) throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                return session;
            };
        }
    }
    void start() throws Exception {
        base.postgres.withEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF8 --lc-collate=C --lc-ctype=C");
        base.start(null);
        try (var connection = admin()) {
            execute(connection, "ALTER ROLE pa36_operations PASSWORD '" + base.password + "'");
            execute(connection, "ALTER ROLE pa27_query PASSWORD '" + base.password + "'");
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("onecall.operations-subject-read.enabled", "true");
        properties.put("onecall.operations-subject-read.datasource.url", base.url);
        properties.put("onecall.operations-subject-read.datasource.username", "pa36_operations");
        properties.put("onecall.operations-subject-read.datasource.password", base.password);
        properties.put("onecall.operations-subject-read.cursor.active-key-id", "PA41");
        properties.put("onecall.operations-subject-read.cursor.keys[0].id", "PA41");
        properties.put("onecall.operations-subject-read.cursor.keys[0].material", Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]));
        properties.put("onecall.customer.persistence.url", base.url);
        properties.put("onecall.customer.persistence.username", "pa27_customer");
        properties.put("onecall.customer.persistence.password", base.password);
        properties.put("onecall.persistence.url", base.url);
        properties.put("onecall.persistence.username", "pa27_query");
        properties.put("onecall.persistence.password", base.password);
        properties.put("onecall.customer.business-numbers.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]));
        properties.put("onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]));
        properties.put("onecall.sharing-command.enabled", "false");
        properties.put("onecall.sharing-admission.datasource.enabled", "false");
        properties.put("onecall.evidence-media-read.datasource.enabled", "false");
        properties.put("onecall.purchase-consent.current.policy-code", "SYNTHETIC_PURCHASE");
        properties.put("onecall.purchase-consent.current.policy-version", "1");
        properties.put("onecall.purchase-consent.current.prompt-version", "1");
        properties.put("onecall.purchase-consent.current.content-sha256", CONTENT);
        context = new SpringApplicationBuilder(OneCallApplication.class, SessionBeans.class).profiles("web", "persistence")
                .initializers(application -> application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa41-synthetic", properties)))
                .run("--server.port=0", "--spring.main.banner-mode=off");
        port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        dsl = context.getBean("operationsSubjectReadDsl", DSLContext.class);
        pool = context.getBean("operationsSubjectReadDataSource", HikariDataSource.class);
    }
    void reset() throws Exception {
        before = null; after = null; entries.clear(); SESSIONS.clear();
        try (var connection = admin()) {
            execute(connection, "TRUNCATE customer_iam.customer_account,organization_access.organization,subject_number.business_subject,catalog_order.catalog_product,operations_iam.employee_account,platform.idempotency_record,platform.outbox_event,audit.audit_fact CASCADE");
            execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'PA41 synthetic')", account);
            execute(connection, "INSERT INTO customer_iam.identity_profile(id,account_id,real_name_status,legal_name_ciphertext,id_number_ciphertext,id_number_hmac,verification_ref_ciphertext,verified_at) VALUES (?,?,'VERIFIED','v1.fixture.PRIVATE','v1.fixture.PRIVATE',repeat('a',64),'v1.fixture.PRIVATE',now())", UUID.randomUUID(), account);
            execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role) VALUES (?,repeat('e',64),'synthetic','synthetic',repeat('f',64),'PA41 synthetic','ADMIN')", employee);
            for (UUID subjectId : List.of(subject, secondSubject)) {
                execute(connection, "INSERT INTO subject_number.business_subject(id,owner_account_id,source) VALUES (?,?,'WEB')", subjectId, account);
                execute(connection, "INSERT INTO activation_consent.service_activation(id,subject_id,status) VALUES (?,?,'ACTIVE')", UUID.randomUUID(), subjectId);
            }
        }
        seedCatalog("ONE_TIME", "PERSONAL", 123L, "WEB", "ACTIVE", "ACTIVE", "{}");
        session("web", "WEB", null); session("miniapp", "MINIAPP", null);
        dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext execution) { var hook = before; if (hook != null) hook.accept(execution); }
            @Override public void executeEnd(ExecuteContext execution) {
                try {
                    entries.add(new Entry(execution.sql(), execution.query() == null ? List.of() : new ArrayList<>(execution.query().getBindValues()), one(execution.connection(), "SELECT session_user"), one(execution.connection(), "SELECT current_user"), Integer.parseInt(one(execution.connection(), "SELECT pg_backend_pid()")), Long.parseLong(one(execution.connection(), "SELECT txid_current()"))));
                } catch (SQLException failure) { throw new org.jooq.exception.DataAccessException("Synthetic observer failure", failure); }
                var hook = after; if (hook != null) hook.accept(execution);
            }
        });
    }
    UUID organization(String role) throws Exception {
        UUID organization = UUID.randomUUID();
        sql("INSERT INTO organization_access.organization(id,display_name) VALUES (?,'PA41 synthetic')", organization);
        sql("INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role) VALUES (?,?,?,?)", UUID.randomUUID(), organization, account, role);
        sql("DELETE FROM activation_consent.service_activation WHERE subject_id IN (?,?)", subject, secondSubject);
        sql("DELETE FROM subject_number.business_subject WHERE id IN (?,?)", subject, secondSubject);
        for (UUID subjectId : List.of(subject, secondSubject)) {
            sql("INSERT INTO subject_number.business_subject(id,owner_organization_id,source) VALUES (?,?,'WEB')", subjectId, organization);
            sql("INSERT INTO activation_consent.service_activation(id,subject_id,status) VALUES (?,?,'ACTIVE')", UUID.randomUUID(), subjectId);
        }
        session("web", "WEB", organization); session("miniapp", "MINIAPP", organization);
        return organization;
    }
    void session(String token, String channel, UUID organization) {
        SESSIONS.put(token, new VerifiedCustomerSession(account, TrustedCustomerCaller.Channel.valueOf(channel), "onecall:customer-session:v1",
                new SelectedCustomerWorkspace(organization == null ? CustomerContext.WorkspaceType.PERSONAL : CustomerContext.WorkspaceType.ORGANIZATION, organization)));
    }
    void seedCatalog(String type, String audience, Long price, String channel, String versionStatus, String offerStatus, String terms) throws Exception {
        seedCatalog(type, audience, price, channel, versionStatus, offerStatus, terms, null);
    }
    void seedCatalog(String type, String audience, Long price, String channel, String versionStatus, String offerStatus, String terms, String approvedSnapshot) throws Exception {
        boolean subscription = "SUBSCRIPTION".equals(type), quote = "QUOTE".equals(type);
        String cycle = quote ? "QUOTE" : subscription ? "MONTHLY" : "ONCE";
        int interval = subscription ? 1 : 0;
        String source = "{\"schemaVersion\":1,\"catalogVersionId\":\"" + version + "\",\"productCode\":\"PA41_SYNTHETIC\",\"versionNo\":1,\"offerType\":\"" + type + "\",\"audience\":\"" + audience + "\",\"priceCents\":" + price + ",\"currency\":\"CNY\",\"billingCycle\":\"" + cycle + "\",\"installments\":1,\"benefitLines\":[{\"lineKey\":\"" + line + "\",\"entitlementType\":\"AI_SECONDS\",\"quantity\":100,\"validityDays\":30,\"grantIntervalMonths\":" + interval + ",\"schedule\":[{\"installmentNo\":1,\"entitlementType\":\"AI_SECONDS\",\"units\":100,\"validFrom\":\"2027-01-01T00:00:00Z\",\"expiresAt\":\"2027-02-01T00:00:00Z\"}]}],\"terms\":" + terms + "}";
        var approved = mapper.readTree(approvedSnapshot == null ? source : approvedSnapshot);
        int installments = approved.has("installments") ? approved.get("installments").intValue() : 1;
        if (approved.has("benefitLines") && !approved.get("benefitLines").isEmpty()) interval = approved.get("benefitLines").get(0).get("grantIntervalMonths").intValue();
        source = canonical(approved);
        try (var connection = admin()) {
            execute(connection, "TRUNCATE catalog_order.catalog_product CASCADE");
            execute(connection, "INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (?,'PA41_SYNTHETIC','STANDARD')", product);
            execute(connection, "INSERT INTO catalog_order.catalog_version(id,product_id,version_no,sale_status,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,created_by_employee_id,published_at) VALUES (?,?,1,?,?,?,?,'CNY',?,?,?,?::jsonb,?,?,CASE WHEN ?='DRAFT' THEN NULL ELSE now() END)", version, product, versionStatus, type, audience, price, cycle, installments, interval, source, sha(source), employee, versionStatus);
            execute(connection, "INSERT INTO catalog_order.catalog_benefit_line(id,catalog_version_id,entitlement_type,quantity,validity_days,grant_interval_months) VALUES (?,?,'AI_SECONDS',100,30,?)", line, version, interval);
            execute(connection, "INSERT INTO catalog_order.catalog_offer(id,catalog_version_id,channel,offer_status,version_token) VALUES (?,?,?,?, 'Pa41-Exact')", offer, version, channel, offerStatus);
        }
    }
    String body() { return body(subject, offer, "WEB"); }
    String body(UUID subjectId, UUID offerId, String channel) {
        return "{\"subjectId\":\"" + subjectId + "\",\"catalogOfferId\":\"" + offerId + "\",\"channel\":\"" + channel + "\",\"decision\":\"ACCEPT\",\"policyCode\":\"SYNTHETIC_PURCHASE\",\"policyVersion\":\"1\",\"promptVersion\":\"1\",\"contentSha256\":\"" + CONTENT + "\"}";
    }
    HttpResponse<String> post(String key) throws Exception { return post("web", key, body()); }
    HttpResponse<String> post(String token, String key, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ROUTE)).timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + token).header("Content-Type", "application/json");
        if (key != null) request.header("Idempotency-Key", key);
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode success(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        JsonNode node = mapper.readTree(response.body());
        assertThat(node.propertyNames()).containsExactlyInAnyOrder("orderId", "orderNo", "subjectId", "channel", "catalogVersionId", "catalogOfferId", "amountCents", "currency", "paymentStatus", "issuanceStatus", "createdAt", "snapshotSha256", "snapshot", "purchaseConsentId");
        assertThat(node.get("paymentStatus").asString()).isEqualTo("UNPAID"); assertThat(node.get("issuanceStatus").asString()).isEqualTo("NONE");
        return node;
    }
    void problem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        JsonNode node = mapper.readTree(response.body());
        assertThat(node.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(node.get("code").asString()).isEqualTo(code);
        assertThat(response.body()).doesNotContain(base.password, "PRIVATE", "SQLState", "SQLException", "fixture_admin", "pa36_operations");
    }
    Map<String, String> rows() throws Exception {
        Map<String, String> result = new TreeMap<>();
        for (String table : TABLES) result.put(table, value("SELECT coalesce(jsonb_agg(to_jsonb(row) ORDER BY id)::text,'[]') FROM " + table + " row"));
        return result;
    }
    void bundle(Map<String, String> before) throws Exception {
        Map<String, String> current = rows();
        Map<String, Integer> changes = Map.of("catalog_order.customer_order", 1, "activation_consent.consent_record", 1, "platform.idempotency_record", 1, "platform.outbox_event", 2, "audit.audit_fact", 2);
        for (String table : TABLES) {
            if (!changes.containsKey(table)) assertThat(current.get(table)).as(table).isEqualTo(before.get(table));
            else {
                JsonNode previous = mapper.readTree(before.get(table)), actual = mapper.readTree(current.get(table));
                assertThat(actual.size() - previous.size()).as(table).isEqualTo(changes.get(table));
                previous.forEach(row -> assertThat(actual).as("unchanged original " + table).contains(row));
            }
        }
        assertThat(value("SELECT count(*) FROM platform.idempotency_record WHERE scope='PURCHASE' AND status='COMPLETED'")).isEqualTo("1");
        assertThat(value("SELECT count(*) FROM catalog_order.customer_order o JOIN activation_consent.consent_record c ON c.id=o.purchase_consent_id JOIN platform.idempotency_record i ON i.result_id=o.id WHERE o.payment_status='UNPAID' AND c.action='ACCEPT' AND c.account_id=o.account_id AND c.subject_id=o.subject_id AND c.channel=o.channel AND c.purpose='PURCHASE' AND i.result_type='ORDER' AND i.result_sha256=o.snapshot_sha256 AND i.idempotency_key=o.account_id::text||':'||o.idempotency_key")).isEqualTo("1");
        for (String event : List.of("ORDER_CREATED", "PURCHASE_CONSENT_RECORDED")) {
            JsonNode fact = mapper.readTree(value("SELECT to_jsonb(row)::text FROM platform.outbox_event row WHERE event_type=?", event));
            JsonNode payload = fact.get("payload_redacted");
            assertThat(fact.get("event_domain").asString()).isEqualTo("PURCHASE");
            assertThat(fact.get("payload_sha256").asString()).isEqualTo(sha(canonical(payload)));
            assertThat(fact.get("dedupe_key").asString()).isEqualTo((event.equals("ORDER_CREATED") ? "customer_order:" : "consent_record:") + fact.get("aggregate_id").asString());
            JsonNode audit = mapper.readTree(value("SELECT to_jsonb(row)::text FROM audit.audit_fact row WHERE action_code=?", event));
            assertThat(audit.get("actor_type").asString()).isEqualTo("ACCOUNT");
            assertThat(audit.get("actor_id").asString()).isEqualTo(account.toString());
            assertThat(audit.get("object_id")).isEqualTo(fact.get("aggregate_id"));
            assertThat(audit.get("object_type")).isEqualTo(fact.get("aggregate_type"));
            assertThat(audit.get("after_snapshot")).isEqualTo(payload);
            for (String empty : List.of("before_snapshot", "reason_code", "request_id")) assertThat(audit.get(empty).isNull()).isTrue();
            assertThat(UUID.fromString(audit.get("trace_id").asString())).isNotNull();
        }
    }
    Object policy(Object replacement) throws Exception {
        Object owner = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(context.getBean(com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService.class));
        var field = owner.getClass().getDeclaredField("policy"); field.setAccessible(true); Object previous = field.get(owner); field.set(owner, replacement); return previous;
    }
    void consentFixture(String change) throws Exception {
        if (change.equals("CONSUMED")) {
            success(post("consent"), 201);
            sql("WITH original AS (DELETE FROM catalog_order.customer_order RETURNING *) INSERT INTO catalog_order.customer_order SELECT (jsonb_populate_record(NULL::catalog_order.customer_order,to_jsonb(original)||'{\"idempotency_key\":\"consumed-original\"}'::jsonb)).* FROM original");
            sql("DELETE FROM platform.idempotency_record"); return;
        }
        String event = "purchase-consent:" + sha("{\"accountId\":\"" + account + "\",\"clientIdempotencyKey\":\"consent\"}");
        var snapshot = mapper.createObjectNode(); snapshot.put("schemaVersion", 1); snapshot.put("accountId", account.toString()); snapshot.put("subjectId", subject.toString());
        for (var entry : Map.of("purpose", "PURCHASE", "channel", "WEB", "eventDomain", "PURCHASE", "eventId", event, "sourceType", "ACCOUNT", "sourceRef", event, "action", "ACCEPT", "policyCode", "SYNTHETIC_PURCHASE", "policyVersion", "1", "promptVersion", "1").entrySet()) snapshot.put(entry.getKey(), entry.getValue());
        snapshot.put("contentSha256", CONTENT);
        if (change.equals("DECLINE")) snapshot.put("action", "DECLINE");
        if (change.equals("SUBJECT")) snapshot.put("subjectId", secondSubject.toString());
        if (change.equals("SOURCE")) snapshot.put("sourceRef", "other-source");
        var fingerprint = mapper.createObjectNode();
        for (String field : List.of("accountId", "action", "channel", "eventDomain", "eventId", "policyCode", "policyVersion", "promptVersion", "purpose", "sourceType", "subjectId")) fingerprint.set(field, snapshot.get(field));
        String bytes = canonical(snapshot);
        sql("INSERT INTO activation_consent.consent_record(id,subject_id,purpose,channel,event_domain,event_id,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,snapshot,snapshot_sha256,request_fingerprint) VALUES (?,?,'PURCHASE','WEB','PURCHASE',?,'SYNTHETIC_PURCHASE','1','1',?,'ACCOUNT',?,?,?::jsonb,?,?)", UUID.randomUUID(), UUID.fromString(snapshot.get("subjectId").asString()), event, snapshot.get("action").asString(), snapshot.get("sourceRef").asString(), account, bytes, sha(bytes), sha(canonical(fingerprint)));
    }
    void clean() throws Exception {
        for (int index = 0; index < 4; index++) try (var connection = pool.getConnection()) {
            assertThat(one(connection, "SELECT current_user=session_user")).isEqualTo("t");
            assertThat(connection.getAutoCommit()).isTrue(); assertThat(connection.isReadOnly()).isFalse();
            assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
            for (String setting : List.of("statement_timeout", "lock_timeout", "idle_in_transaction_session_timeout")) assertThat(one(connection, "SHOW " + setting)).isEqualTo("0");
        }
    }
    void sql(String sql, Object... values) throws Exception { try (var connection = admin()) { execute(connection, sql, values); } }
    String value(String sql, Object... values) throws Exception { try (var connection = admin()) { return one(connection, sql, values); } }
    Connection admin() throws SQLException { return base.admin(); }
    Connection runtime() throws SQLException { return DriverManager.getConnection(base.url, "pa36_operations", base.password); }
    static String canonical(JsonNode node) {
        if (node.isObject()) {
            TreeMap<String, JsonNode> sorted = new TreeMap<>(); node.properties().forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
            List<String> entries = new ArrayList<>(); sorted.forEach((key, value) -> entries.add(JsonMapper.builder().build().writeValueAsString(key) + ":" + canonical(value)));
            return "{" + String.join(",", entries) + "}";
        }
        if (node.isArray()) { List<String> entries = new ArrayList<>(); node.forEach(value -> entries.add(canonical(value))); return "[" + String.join(",", entries) + "]"; }
        return node.toString();
    }
    static String sha(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception failure) { throw new AssertionError(failure); }
    }
    @Override public void close() { if (context != null) context.close(); base.close(); }
}
