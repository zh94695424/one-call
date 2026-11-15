package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.OneCallApplication;
import com.onecall.core.operations_iam.internal.activationread.*;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.*;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.jooq.DSLContext;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class OperationsActivationReadFixture implements AutoCloseable {
    static final String BASE = "/api/v1/operations/activations";
    static final List<String> ROLES = List.of("operations_iam_role", "subject_number_role", "activation_consent_role", "entitlement_metering_role", "evidence_role", "catalog_order_role");
    final CustomerBusinessNumbersFixture base = new CustomerBusinessNumbersFixture();
    final UUID employee = UUID.randomUUID(), ops = UUID.randomUUID(), disabled = UUID.randomUUID(), missing = UUID.randomUUID();
    final String login = "pa34_read_" + UUID.randomUUID().toString().replace("-", "");
    final String password = UUID.randomUUID().toString();
    final Instant time = Instant.parse("2026-11-15T00:00:00.123456Z");
    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    ConfigurableApplicationContext context;
    HikariDataSource pool;
    DSLContext dsl;
    TransactionTemplate transaction;
    OperationsReadContext scope;
    OperationsActivationReadService service;
    SyntheticEmployeeSessions sessions;
    int port;

    @TestConfiguration(proxyBeanMethods = false)
    static class SessionBeans {
        @Bean SyntheticEmployeeSessions syntheticEmployeeSessions() { return new SyntheticEmployeeSessions(); }
        @Bean com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> { throw new com.onecall.core.customer_iam.internal.security.CustomerSessionVerificationException(com.onecall.core.customer_iam.internal.security.CustomerSessionVerificationException.Code.UNAUTHENTICATED); };
        }
    }
    void start() throws Exception {
        try {
            base.postgres.withEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF8 --lc-collate=C --lc-ctype=C"); base.start(null);
            try (var connection = admin()) {
                execute(connection, "CREATE ROLE " + login + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '" + password + "'");
                for (String role : ROLES) execute(connection, "GRANT " + role + " TO " + login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
                execute(connection, "ALTER ROLE pa27_query PASSWORD '" + base.password + "'");
            }
            var settings = new HashMap<String, Object>(); String prefix = "onecall.operations-subject-read.";
            settings.put("server.error.path", "/internal/pa34-error");
            settings.put("spring.web.error.path", "/internal/pa34-error");
            settings.put(prefix + "enabled", "true"); settings.put(prefix + "datasource.url", base.url); settings.put(prefix + "datasource.username", login); settings.put(prefix + "datasource.password", password);
            var key = com.onecall.archtests.operations.OperationsActivationReadTestProviders.properties().cursor().keys().getFirst();
            settings.put(prefix + "cursor.active-key-id", key.id()); settings.put(prefix + "cursor.keys[0].id", key.id()); settings.put(prefix + "cursor.keys[0].material", key.material());
            settings.put("onecall.customer.persistence.url", base.url); settings.put("onecall.customer.persistence.username", "pa27_customer"); settings.put("onecall.customer.persistence.password", base.password);
            settings.put("onecall.persistence.url", base.url); settings.put("onecall.persistence.username", "pa27_query"); settings.put("onecall.persistence.password", base.password);
            settings.put("onecall.customer.business-numbers.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32])); settings.put("onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]));
            for (String name : List.of("onecall.sharing-command.enabled", "onecall.sharing-admission.datasource.enabled", "onecall.evidence-media-read.datasource.enabled")) settings.put(name, "false");
            context = new SpringApplicationBuilder(OneCallApplication.class, SessionBeans.class).profiles("web", "persistence").initializers(application -> application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa34-fixture", settings))).run("--server.port=0", "--spring.main.banner-mode=off");
            port = ((ServletWebServerApplicationContext) context).getWebServer().getPort(); service = context.getBean(OperationsActivationReadService.class); pool = context.getBean("operationsSubjectReadDataSource", HikariDataSource.class); dsl = context.getBean("operationsSubjectReadDsl", DSLContext.class); transaction = context.getBean("operationsSubjectReadTransactionTemplate", TransactionTemplate.class); scope = context.getBean("operationsSubjectReadContext", OperationsReadContext.class); sessions = context.getBean(SyntheticEmployeeSessions.class); reset();
        } catch (Exception | Error failure) { close(); throw failure; }
    }
    Connection admin() throws Exception { return base.admin(); }
    Connection runtime() throws Exception { return DriverManager.getConnection(base.url, login, password); }
    void reset() throws Exception {
        try (var connection = admin()) {
            execute(connection, "TRUNCATE customer_iam.customer_account, organization_access.organization, subject_number.business_subject, operations_iam.employee_account, platform.outbox_event, audit.audit_fact CASCADE"); int index = 1;
            for (UUID identifier : List.of(employee, ops, disabled)) { execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role,status) VALUES (?,?,'fixture','fixture',?,'PRIVATE_EMPLOYEE',?,?)", identifier, "%064x".formatted(index), "%064x".formatted(index + 100), identifier.equals(ops) ? "OPS" : "ADMIN", identifier.equals(disabled) ? "DISABLED" : "ACTIVE"); index++; }
        }
        sessions.sessions.clear(); for (var entry : Map.of("admin", employee, "ops", ops, "disabled", disabled, "missing", missing).entrySet()) sessions.sessions.put(entry.getKey(), new VerifiedEmployeeSession(entry.getValue(), EmployeeBearerAuthenticator.AUDIENCE)); sessions.sessions.put("wrong", new VerifiedEmployeeSession(employee, "onecall:customer-session:v1"));
    }
    UUID subject() throws Exception { UUID identifier = UUID.randomUUID(); try (var connection = admin()) { execute(connection, "INSERT INTO subject_number.business_subject(id,source) VALUES (?,'WEB')", identifier); } return identifier; }
    UUID transition(int sequence) throws Exception { return transition(new UUID(0, sequence), time.minusSeconds(sequence), "INACTIVE", "WEB", "ACTIVATED"); }
    UUID transition(UUID identifier, Instant occurredAt, String from, String channel, String reason) throws Exception {
        UUID subject = subject(), activation = UUID.randomUUID(), consent = "OPERATIONS".equals(channel) ? null : UUID.randomUUID(), account = consent == null ? null : UUID.randomUUID();
        String domain = "OPERATIONS".equals(channel) ? "OPERATIONS" : "IVR".equals(channel) ? "IVR_CONFIRM" : "ACTIVATION"; String to = "ACTIVE".equals(from) ? "SUSPENDED" : "ACTIVE";
        try (var connection = admin()) {
            connection.setAutoCommit(false); execute(connection, "INSERT INTO activation_consent.service_activation(id,subject_id,status,revision) VALUES (?,?,?,2)", activation, subject, to);
            if (consent != null) {
                execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'PRIVATE_ACCOUNT')", account); UUID session = null;
                if ("IVR".equals(channel)) { session = UUID.randomUUID(); execute(connection, "INSERT INTO ivr.inbound_session(id,call_id,subject_id,trusted_caller,access_number_masked,node_id,protocol_version,prompt_version) VALUES (?,?,?,false,'PRIVATE_ACCESS','fixture','v1','v1')", session, session.toString(), subject); }
                execute(connection, "INSERT INTO activation_consent.consent_record(id,subject_id,purpose,channel,event_domain,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,ivr_session_id,snapshot,snapshot_sha256,event_id,request_fingerprint) VALUES (?,?,'SERVICE_ACTIVATION',?,?,'fixture','v1','v1','ACCEPT',?,?,?,?,jsonb_build_object('secret','PRIVATE_CONSENT'),repeat('a',64),?,repeat('b',64))", consent, subject, channel, domain, session == null ? "ACCOUNT" : "IVR_SESSION", consent.toString(), session == null ? account : null, session, consent.toString());
            }
            execute(connection, "INSERT INTO activation_consent.service_activation_transition(id,activation_id,subject_id,from_status,to_status,from_revision,to_revision,source_channel,source_action_domain,source_action_event_id,status_event_domain,status_event_id,request_fingerprint,actor_account_id,actor_employee_id,reason_code,consent_record_id,occurred_at) VALUES (?,?,?,?,?,1,2,?,?,?,'ACTIVATION_STATUS',?,repeat('c',64),?,?,?,?,?::timestamptz)", identifier, activation, subject, from, to, channel, domain, "PRIVATE_ACTION_" + identifier, "event-" + identifier, account, consent == null ? employee : null, reason, consent, occurredAt.toString()); connection.commit();
        }
        return identifier;
    }
    HttpResponse<String> request(String method, String path, String token, String... accepts) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(15)).method(method, HttpRequest.BodyPublishers.noBody());
        if (token != null) request.header("Authorization", "Bearer " + token); for (String accept : accepts) request.header("Accept", accept); return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode get(String path) throws Exception { var response = request("GET", path, "admin"); assertThat(response.statusCode()).as(response.body()).isEqualTo(200); return json(response); }
    static JsonNode json(HttpResponse<String> response) { return JsonMapper.builder().build().readTree(response.body()); }
    static void problem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status); assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json"); var body = json(response); assertThat(body.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId"); assertThat(body.path("status").asInt()).isEqualTo(status); assertThat(body.path("code").asString()).isEqualTo(code); UUID.fromString(body.path("traceId").asString()); assertThat(response.headers().firstValue("Cache-Control")).contains("no-store"); assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
    }
    Map<String, String> fingerprints() throws Exception {
        var result = new TreeMap<String, String>(); try (var connection = admin(); var statement = connection.createStatement(); var tables = statement.executeQuery("SELECT schemaname,tablename FROM pg_tables WHERE schemaname IN ('customer_iam','organization_access','subject_number','activation_consent','entitlement_metering','evidence','catalog_order','ivr','audit','platform','operations_iam') ORDER BY schemaname,tablename")) {
            while (tables.next()) { String table = tables.getString(1) + "." + tables.getString(2); result.put(table, one(connection, "SELECT count(*)::text||':'||md5(coalesce(string_agg(to_jsonb(row)::text,E'\\n' ORDER BY to_jsonb(row)::text),'')) FROM " + table + " row")); }
        } return result;
    }
    void replaceCursor(OperationsSubjectReadProperties.Cursor cursor) {
        var factory = new DefaultListableBeanFactory(); var original = context.getBean(OperationsSubjectReadProperties.class); factory.registerSingleton("properties", new OperationsSubjectReadProperties(true, original.datasource(), cursor));
        replaceService(new DefaultOperationsActivationReadService(transaction, dsl, scope, factory.getBeanProvider(OperationsSubjectReadProperties.class), context.getBeanProvider(ActivationHistoryReader.class)));
    }
    void replaceService(OperationsActivationReadService replacement) { var factory = (DefaultListableBeanFactory) context.getBeanFactory(); factory.destroySingleton("operationsActivationReadService"); factory.registerSingleton("operationsActivationReadService", replacement); service = replacement; }
    @Override public void close() { try { if (context != null) context.close(); } finally { try { client.close(); } finally { base.close(); } } }
}
