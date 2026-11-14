package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.assertThat;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;

import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.OneCallApplication;
import com.onecall.core.operations_iam.internal.security.EmployeeBearerAuthenticator;
import com.onecall.core.operations_iam.internal.security.VerifiedEmployeeSession;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class OperationsSubjectReadFixture implements AutoCloseable {
    static final String BASE = "/api/v1/operations/subjects";
    static final List<String> ROLES = List.of("operations_iam_role", "subject_number_role", "activation_consent_role",
            "entitlement_metering_role", "evidence_role", "catalog_order_role");
    final CustomerBusinessNumbersFixture base = new CustomerBusinessNumbersFixture();
    final UUID employee = UUID.randomUUID();
    final UUID ops = UUID.randomUUID();
    final UUID disabled = UUID.randomUUID();
    final UUID missing = UUID.randomUUID();
    final String login = "pa33_read_" + UUID.randomUUID().toString().replace("-", "");
    final String password = UUID.randomUUID().toString();
    final Instant time = Instant.parse("2026-11-14T00:00:00.123456Z");
    final HttpClient client = HttpClient.newHttpClient();
    ConfigurableApplicationContext context;
    OperationsSubjectReadService service;
    HikariDataSource pool;
    DSLContext dsl;
    TransactionTemplate transaction;
    SyntheticEmployeeSessions sessions;
    int port;

    @TestConfiguration(proxyBeanMethods = false)
    static class SessionBeans {
        @Bean SyntheticEmployeeSessions syntheticEmployeeSessions() { return new SyntheticEmployeeSessions(); }
        @Bean com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> {
                throw new com.onecall.core.customer_iam.internal.security.CustomerSessionVerificationException(
                        com.onecall.core.customer_iam.internal.security.CustomerSessionVerificationException.Code.UNAUTHENTICATED);
            };
        }
    }

    void start() throws Exception {
        base.postgres.withEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF8 --lc-collate=C --lc-ctype=C");
        base.start(null);
        try (var connection = admin()) {
            execute(connection, "CREATE ROLE " + login + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '" + password + "'");
            for (String role : ROLES) execute(connection, "GRANT " + role + " TO " + login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
            execute(connection, "ALTER ROLE pa27_query PASSWORD '" + base.password + "'");
        }
        Map<String, Object> properties = new HashMap<>();
        String prefix = "onecall.operations-subject-read.";
        properties.put(prefix + "enabled", "true");
        properties.put(prefix + "datasource.url", base.url);
        properties.put(prefix + "datasource.username", login);
        properties.put(prefix + "datasource.password", password);
        properties.put(prefix + "cursor.active-key-id", "Read-2026_11");
        properties.put(prefix + "cursor.keys[0].id", "Read-2026_11");
        properties.put(prefix + "cursor.keys[0].material", Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]));
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
        context = new SpringApplicationBuilder(OneCallApplication.class, SessionBeans.class).profiles("web", "persistence")
                .initializers(application -> application.getEnvironment().getPropertySources()
                        .addFirst(new MapPropertySource("read-fixture", properties)))
                .run("--server.port=0", "--spring.main.banner-mode=off");
        port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        service = context.getBean(OperationsSubjectReadService.class);
        pool = context.getBean("operationsSubjectReadDataSource", HikariDataSource.class);
        dsl = context.getBean("operationsSubjectReadDsl", DSLContext.class);
        transaction = context.getBean("operationsSubjectReadTransactionTemplate", TransactionTemplate.class);
        sessions = context.getBean(SyntheticEmployeeSessions.class);
        reset();
    }

    Connection admin() throws Exception { return base.admin(); }
    Connection runtime() throws Exception { return DriverManager.getConnection(base.url, login, password); }

    void reset() throws Exception {
        try (var connection = admin()) {
            execute(connection, "TRUNCATE customer_iam.customer_account, organization_access.organization, subject_number.business_subject, operations_iam.employee_account, platform.outbox_event, audit.audit_fact CASCADE");
            int index = 1;
            for (UUID identifier : List.of(employee, ops, disabled)) {
                execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role,status) VALUES (?,?,'fixture','fixture',?,'fixture',?,?)",
                        identifier, "%064x".formatted(index), "%064x".formatted(index + 100), identifier.equals(ops) ? "OPS" : "ADMIN", identifier.equals(disabled) ? "DISABLED" : "ACTIVE");
                index++;
            }
        }
        sessions.sessions.clear();
        sessions.sessions.put("admin", new VerifiedEmployeeSession(employee, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("ops", new VerifiedEmployeeSession(ops, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("disabled", new VerifiedEmployeeSession(disabled, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("missing", new VerifiedEmployeeSession(missing, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("wrong", new VerifiedEmployeeSession(employee, "onecall:customer-session:v1"));
    }

    UUID subject() throws Exception { return subject(UUID.randomUUID(), "WEB", "UNVERIFIED", time); }

    void replaceCursor(com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.Cursor cursor) {
        var original = context.getBean(com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.class);
        var properties = new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties(true, original.datasource(), cursor);
        service = new com.onecall.core.operations_iam.internal.subjectread.DefaultOperationsSubjectReadService(transaction, dsl,
                context.getBean("operationsSubjectReadContext", com.onecall.core.operations_iam.subjectread.OperationsReadContext.class), properties,
                context.getBeanProvider(com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.SubjectReader.class),
                context.getBeanProvider(com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.ActivationReader.class),
                context.getBeanProvider(com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.TrialReader.class),
                context.getBeanProvider(com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.EvidenceCounter.class),
                context.getBeanProvider(com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.OrderCounter.class));
        var factory = (org.springframework.beans.factory.support.DefaultListableBeanFactory) context.getBeanFactory();
        factory.destroySingleton("operationsSubjectReadService");
        factory.registerSingleton("operationsSubjectReadService", service);
    }

    UUID binding(UUID subject, UUID identifier, String status, String mask) throws Exception {
        try (var connection = admin()) {
            execute(connection, "INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,status,verified_at) VALUES (?,?,'MOBILE','v1.fixture.PRIVATE_PHONE',?,?,?,CASE WHEN ?='ACTIVE' THEN now() END)",
                    identifier, subject, UUID.randomUUID().toString().replace("-", "").repeat(2), mask, status, status);
        }
        return identifier;
    }

    void risk(UUID binding, String state) throws Exception {
        try (var connection = admin()) {
            execute(connection, "INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source,cleared_at) VALUES (?,?,'MANUAL_REVIEW',?,'OPERATIONS',CASE WHEN ?='CLEARED' THEN now() END)", UUID.randomUUID(), binding, state, state);
        }
    }

    void activation(UUID subject, String state) throws Exception {
        try (var connection = admin()) {
            execute(connection, "INSERT INTO activation_consent.service_activation(id,subject_id,status) VALUES (?,?,?)", UUID.randomUUID(), subject, state);
        }
    }

    UUID claim(UUID subject, UUID identifier, String status, Instant checkedAt) throws Exception {
        UUID account = UUID.randomUUID();
        try (var connection = admin()) {
            execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'PRIVATE_ACCOUNT')", account);
            execute(connection, "INSERT INTO subject_number.subject_claim(id,subject_id,account_id,verification_method,verification_ref_ciphertext,request_id,status,risk_checked_at,verified_at) VALUES (?,?,?,'MANUAL_REVIEW','v1.fixture.PRIVATE_CLAIM',?,?,?::timestamptz,CASE WHEN ?='ACCEPTED' THEN ?::timestamptz END)",
                    identifier, subject, account, identifier.toString(), status, checkedAt.toString(), status, checkedAt.toString());
        }
        return identifier;
    }

    UUID trial(UUID subject, long reserved, long consumed, String status, Instant expiry, boolean balance, boolean mismatchedExpiry) throws Exception {
        UUID policy = UUID.randomUUID();
        UUID grant = UUID.randomUUID();
        UUID trial = UUID.randomUUID();
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, "INSERT INTO entitlement_metering.trial_policy_version(id,campaign_key,version_no,granted_seconds,valid_days,enabled,snapshot,snapshot_sha256,published_by_employee_id) VALUES (?,'FIRST-TRIAL',?,100,1,false,'{}',repeat('a',64),?)",
                    policy, Integer.parseInt(CustomerBusinessNumbersFixture.one(connection, "SELECT coalesce(max(version_no),0)+1 FROM entitlement_metering.trial_policy_version")), employee);
            execute(connection, "INSERT INTO entitlement_metering.entitlement_grant(id,subject_id,source_type,source_ref_id,entitlement_type,granted_units,valid_from,expires_at,snapshot,snapshot_sha256) VALUES (?,?,'TRIAL',?,'RECORDING_SECONDS',100,?::timestamptz,?::timestamptz,'{}',repeat('b',64))",
                    grant, subject, trial, expiry.minusSeconds(172800).toString(), (mismatchedExpiry ? expiry.plusSeconds(1) : expiry).toString());
            execute(connection, "INSERT INTO entitlement_metering.trial_grant(id,subject_id,campaign_key,trial_policy_version_id,entitlement_grant_id,source_action_domain,source_action_event_id,request_fingerprint,granted_at,expires_at) VALUES (?,?,'FIRST-TRIAL',?,?,'ACTIVATION',?,repeat('c',64),?::timestamptz,?::timestamptz)",
                    trial, subject, policy, grant, trial.toString(), expiry.minusSeconds(172800).toString(), expiry.toString());
            if (balance) execute(connection, "INSERT INTO entitlement_metering.entitlement_balance(id,grant_id,granted_units,reserved_units,consumed_units,status) VALUES (?,?,100,?,?,?)", UUID.randomUUID(), grant, reserved, consumed, status);
            connection.commit();
        }
        return grant;
    }

    Map<String, String> fingerprints() throws Exception {
        var result = new java.util.TreeMap<String, String>();
        try (var connection = admin(); var statement = connection.createStatement(); var tables = statement.executeQuery(
                "SELECT schemaname,tablename FROM pg_tables WHERE schemaname IN ('subject_number','activation_consent','entitlement_metering','evidence','catalog_order','audit','platform','operations_iam') ORDER BY schemaname,tablename")) {
            while (tables.next()) {
                String table = tables.getString(1) + "." + tables.getString(2);
                result.put(table, CustomerBusinessNumbersFixture.one(connection, "SELECT count(*)::text||':'||md5(coalesce(string_agg(to_jsonb(row)::text,E'\\n' ORDER BY to_jsonb(row)::text),'')) FROM " + table + " row"));
            }
        }
        return result;
    }

    void evidence(UUID subject, String status) throws Exception {
        UUID evidence = UUID.randomUUID();
        UUID recording = UUID.randomUUID();
        String event = UUID.randomUUID().toString();
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, """
                    INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,source_event_id,
                        identity_at_capture,consent_version,snapshot,snapshot_sha256,status,solidified_at)
                    VALUES (?,?,'CALL_RECORDING',?,'EVIDENCE',?,
                        jsonb_build_object('subjectId',?::text,'capturedAt','2026-01-01T00:00:00Z','sourceChannel','IVR'),'PRIVATE_CONSENT',
                        jsonb_build_object('capture','{}'::jsonb,'content',jsonb_build_object('secret','PRIVATE_CONTENT'),
                            'sourceEventDomain','EVIDENCE','sourceEventId',?::text,'source',jsonb_build_object('subjectId',?::text,'recordingId',?::text)),
                        repeat('a',64),?,CASE WHEN ?='SOLIDIFIED' THEN now() END)
                    """, evidence, subject, recording, event, subject, event, subject, recording, status, status);
            execute(connection, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) VALUES (?,?,'EVIDENCE',?,'EVIDENCE_CREATED','{}',repeat('b',64))", UUID.randomUUID(), evidence, event);
            connection.commit();
        }
    }

    void order(UUID subject, String status, UUID purchaser, UUID organization) throws Exception {
        UUID product = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        UUID offer = UUID.randomUUID();
        UUID consent = UUID.randomUUID();
        UUID order = UUID.randomUUID();
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'PRIVATE_PURCHASER') ON CONFLICT DO NOTHING", purchaser);
            if (organization != null) execute(connection, "INSERT INTO organization_access.organization(id,display_name) VALUES (?,'PRIVATE_ORGANIZATION') ON CONFLICT DO NOTHING", organization);
            execute(connection, "INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (?,?,'CORE')", product, "PA33_" + product.toString().replace("-", "").toUpperCase(java.util.Locale.ROOT));
            execute(connection, "INSERT INTO catalog_order.catalog_version(id,product_id,version_no,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,created_by_employee_id) VALUES (?,?,1,'ONE_TIME','PERSONAL',0,'CNY','ONCE',1,0,'{}',repeat('c',64),?)", version, product, employee);
            execute(connection, "INSERT INTO catalog_order.catalog_offer(id,catalog_version_id,channel,offer_status,version_token) VALUES (?,?,'WEB','ACTIVE','v1')", offer, version);
            execute(connection, "INSERT INTO activation_consent.consent_record(id,subject_id,purpose,channel,event_domain,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,snapshot,snapshot_sha256,event_id,request_fingerprint) VALUES (?,?,'PURCHASE','WEB','PURCHASE','fixture','v1','v1','ACCEPT','ACCOUNT',?,?,'{}',repeat('d',64),?,repeat('e',64))", consent, subject, consent.toString(), purchaser, consent.toString());
            execute(connection, "INSERT INTO catalog_order.customer_order(id,order_no,account_id,owner_organization_id,subject_id,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,version_token,idempotency_key,request_fingerprint,paid_at,closed_at) VALUES (?,?,?,?,?,'WEB',?,?,?,0,'CNY',?,jsonb_build_object('secret','PRIVATE_ORDER'),repeat('f',64),'v1',?,repeat('a',64),CASE WHEN ?='PAID' THEN now() END,CASE WHEN ?='CLOSED' THEN now() END)",
                    order, order.toString(), purchaser, organization, subject, version, offer, consent, status, order.toString(), status, status);
            connection.commit();
        }
    }

    UUID subject(UUID identifier, String source, String realName, Instant createdAt) throws Exception {
        try (var connection = admin()) {
            execute(connection, "INSERT INTO subject_number.business_subject(id,source,real_name_status,real_name_verified_at,created_at,updated_at) VALUES (?,?,?,?::timestamptz,?::timestamptz,?::timestamptz)",
                    identifier, source, realName, "VERIFIED".equals(realName) ? createdAt.toString() : null, createdAt.toString(), createdAt.toString());
        }
        return identifier;
    }

    HttpResponse<String> request(String method, String path, String token, String... accepts) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (token != null) request.header("Authorization", "Bearer " + token);
        for (String accept : accepts) request.header("Accept", accept);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode get(String path) throws Exception {
        var response = request("GET", path, "admin");
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return json(response);
    }

    static JsonNode json(HttpResponse<String> response) { return JsonMapper.builder().build().readTree(response.body()); }

    static void problem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        var body = json(response);
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(body.path("status").asInt()).isEqualTo(status);
        assertThat(body.path("code").asString()).isEqualTo(code);
        UUID.fromString(body.path("traceId").asString());
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
    }

    @Override public void close() {
        if (context != null) context.close();
        base.close();
    }
}
