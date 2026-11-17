package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.OneCallApplication;
import com.onecall.core.operations_iam.internal.ivrread.*;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
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

final class OperationsIvrSessionReadFixture implements AutoCloseable {
    static final String BASE = "/api/v1/operations/ivr-sessions";
    static final List<String> ROLES = List.of("operations_iam_role", "ivr_role", "evidence_role");
    final CustomerBusinessNumbersFixture base = new CustomerBusinessNumbersFixture();
    final UUID employee = UUID.randomUUID(), ops = UUID.randomUUID(), disabled = UUID.randomUUID(), missing = UUID.randomUUID();
    final String login = "pa35_read_" + UUID.randomUUID().toString().replace("-", "");
    final String password = UUID.randomUUID().toString();
    final Instant time = Instant.parse("2026-11-15T00:00:00.123456Z");
    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    ConfigurableApplicationContext context;
    HikariDataSource pool;
    DSLContext dsl;
    TransactionTemplate transaction;
    OperationsReadContext scope;
    OperationsIvrSessionReadService service;
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
            settings.put(prefix + "enabled", "true"); settings.put(prefix + "datasource.url", base.url); settings.put(prefix + "datasource.username", login); settings.put(prefix + "datasource.password", password);
            var key = com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders.properties().cursor().keys().getFirst();
            settings.put(prefix + "cursor.active-key-id", key.id()); settings.put(prefix + "cursor.keys[0].id", key.id()); settings.put(prefix + "cursor.keys[0].material", key.material());
            settings.put("onecall.customer.persistence.url", base.url); settings.put("onecall.customer.persistence.username", "pa27_customer"); settings.put("onecall.customer.persistence.password", base.password);
            settings.put("onecall.persistence.url", base.url); settings.put("onecall.persistence.username", "pa27_query"); settings.put("onecall.persistence.password", base.password);
            settings.put("onecall.customer.business-numbers.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32])); settings.put("onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]));
            for (String name : List.of("onecall.sharing-command.enabled", "onecall.sharing-admission.datasource.enabled", "onecall.evidence-media-read.datasource.enabled")) settings.put(name, "false");
            context = new SpringApplicationBuilder(OneCallApplication.class, SessionBeans.class).profiles("web", "persistence").initializers(application -> application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa35-fixture", settings))).run("--server.port=0", "--spring.main.banner-mode=off");
            port = ((ServletWebServerApplicationContext) context).getWebServer().getPort(); service = context.getBean(OperationsIvrSessionReadService.class); pool = context.getBean("operationsSubjectReadDataSource", HikariDataSource.class); dsl = context.getBean("operationsSubjectReadDsl", DSLContext.class); transaction = context.getBean("operationsSubjectReadTransactionTemplate", TransactionTemplate.class); scope = context.getBean("operationsSubjectReadContext", OperationsReadContext.class); sessions = context.getBean(SyntheticEmployeeSessions.class); reset();
        } catch (Exception | Error failure) { close(); throw failure; }
    }
    static String one(Connection connection, String sql, Object... binds) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < binds.length; index++) statement.setObject(index + 1, binds[index]);
            try (var rows = statement.executeQuery()) { if (!rows.next()) throw new SQLException("fixture result missing"); return rows.getString(1); }
        }
    }
    Connection admin() throws Exception { return base.admin(); }
    Connection runtime() throws Exception { return DriverManager.getConnection(base.url, login, password); }
    void reset() throws Exception {
        if (pool != null) pool.setMaximumPoolSize(4);
        try (var connection = admin()) {
            execute(connection, "TRUNCATE customer_iam.customer_account, organization_access.organization, subject_number.business_subject, operations_iam.employee_account, platform.outbox_event, audit.audit_fact CASCADE"); int index = 1;
            for (UUID identifier : List.of(employee, ops, disabled)) { execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role,status) VALUES (?,?,'fixture','fixture',?,'PRIVATE_EMPLOYEE',?,?)", identifier, "%064x".formatted(index), "%064x".formatted(index + 100), identifier.equals(ops) ? "OPS" : "ADMIN", identifier.equals(disabled) ? "DISABLED" : "ACTIVE"); index++; }
        }
        sessions.sessions.clear(); for (var entry : Map.of("admin", employee, "ops", ops, "disabled", disabled, "missing", missing).entrySet()) sessions.sessions.put(entry.getKey(), new VerifiedEmployeeSession(entry.getValue(), EmployeeBearerAuthenticator.AUDIENCE)); sessions.sessions.put("wrong", new VerifiedEmployeeSession(employee, "onecall:customer-session:v1"));
    }
    UUID subject() throws Exception { UUID identifier = UUID.randomUUID(); try (var connection = admin()) { execute(connection, "INSERT INTO subject_number.business_subject(id,source) VALUES (?,'WEB')", identifier); } return identifier; }
    UUID session(int sequence) throws Exception { return session(new UUID(0, sequence), time.minusSeconds(sequence), null, "RECEIVED", true); }
    UUID session(UUID id, Instant created, UUID subject, String status, boolean call) throws Exception {
        String callId = "call-" + id;
        try (var connection = admin()) {
            execute(connection, "INSERT INTO ivr.inbound_session(id,call_id,subject_id,status,trusted_caller,caller_ciphertext,caller_number_hmac,caller_masked,access_number_masked,node_id,protocol_version,prompt_version,created_at,confirmed_at,confirmed_subject_id) VALUES (?,?,?,?,?,?,?,?,'***123','node','v1','v1',?::timestamptz,CASE WHEN ?='CONFIRMED' THEN ?::timestamptz END,CASE WHEN ?='CONFIRMED' THEN ?::uuid END)", id, callId, subject, status, status.equals("CONFIRMED"), status.equals("CONFIRMED") ? "v1.fixture.PRIVATE_CALLER" : null, status.equals("CONFIRMED") ? id.toString().replace("-", "").repeat(2) : null, status.equals("CONFIRMED") ? "***456" : null, created.toString(), status, created.toString(), status, subject);
            if (call) execute(connection, "INSERT INTO ivr.call_record(id,session_id,call_id,subject_id,caller_ciphertext,caller_number_hmac,caller_masked,access_number_masked,node_id,protocol_version) VALUES (?,?,?,?,'v1.fixture.PRIVATE_CALLER',?,'***456','***123','node','v1')", UUID.randomUUID(), id, callId, subject, id.toString().replace("-", "").repeat(2));
        }
        return id;
    }
    void mutate(String sql, Object... values) throws Exception { try (var connection = admin()) { execute(connection, sql, values); } }
    void task(UUID sessionId, String publicId) throws Exception {
        UUID subject = subject(), account = UUID.randomUUID(), binding = UUID.randomUUID(), internal = UUID.randomUUID();
        try (var connection = admin()) {
            execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'PRIVATE_ACCOUNT')", account);
            execute(connection, "INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,status,verified_at) VALUES (?,?,'MOBILE','v1.fixture.PRIVATE',?,'***789','ACTIVE',now())", binding, subject, binding.toString().replace("-", "").repeat(2));
            execute(connection, "INSERT INTO ivr.outbound_call_task(id,task_id,caller_binding_id,subject_id,owner_account_id,created_by_account_id,caller_number_hmac,callee_ciphertext,callee_masked,business_type,expires_at,idempotency_key,request_fingerprint) VALUES (?,?,?,?,?,?,?,'v1.fixture.PRIVATE_CALLEE','***789','fixture',now()+interval '1 hour',?,repeat('a',64))", internal, publicId, binding, subject, account, account, binding.toString().replace("-", "").repeat(2), internal.toString());
            execute(connection, "UPDATE ivr.outbound_call_task SET status='CLAIMED',claimed_call_id=?,claimed_at=now(),revision=revision+1 WHERE id=?", "call-" + sessionId, internal);
            execute(connection, "UPDATE ivr.call_record SET task_id=?,revision=revision+1 WHERE session_id=?", internal, sessionId);
        }
    }
    UUID evidence(UUID sessionId, UUID id) throws Exception { return evidence(sessionId, id, "call-" + sessionId); }
    UUID evidence(UUID sessionId, UUID id, String call) throws Exception {
        UUID subject = subject(), source = UUID.randomUUID(); String event = id.toString();
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, """
                    INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,source_event_id,
                        call_id,session_id,identity_at_capture,consent_version,snapshot,snapshot_sha256)
                    VALUES (?,?,'CALL_RECORDING',?,'EVIDENCE',?,?,?,
                        jsonb_build_object('subjectId',?::text,'capturedAt','2026-11-17T00:00:00Z','sourceChannel','IVR'), 'v1',
                        jsonb_build_object('capture','PRIVATE_CAPTURE','content','PRIVATE_CONTENT','sourceEventDomain','EVIDENCE',
                        'sourceEventId',?::text,'source',jsonb_build_object('subjectId',?::text,'recordingId',?::text,'callId',?::text,'sessionId',?::text)),repeat('a',64))
                    """, id, subject, source, event, call, sessionId, subject, event, subject, source, call, sessionId);
            execute(connection, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) VALUES (?,?,'EVIDENCE',?,'EVIDENCE_CREATED','{}',repeat('b',64))", UUID.randomUUID(), id, event);
            connection.commit();
        } return id;
    }
    void bulkEvidence(UUID session, int count) throws Exception {
        UUID subject = subject(), recording = UUID.randomUUID();
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, "TRUNCATE evidence.evidence_record CASCADE");
            execute(connection, """
                    INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,source_event_id,
                        call_id,session_id,identity_at_capture,consent_version,snapshot,snapshot_sha256)
                    SELECT md5('pa35-evidence-'||n)::uuid,?::uuid,'CALL_RECORDING',?::uuid,'EVIDENCE','bulk-'||n,?,?::uuid,
                        jsonb_build_object('subjectId',?::text,'capturedAt','2026-11-17T00:00:00Z','sourceChannel','IVR'),'v1',
                        jsonb_build_object('capture','PRIVATE_CAPTURE','content','PRIVATE_CONTENT','sourceEventDomain','EVIDENCE',
                        'sourceEventId','bulk-'||n,'source',jsonb_build_object('subjectId',?::text,'recordingId',?::text,'callId',?::text,'sessionId',?::text)),repeat('a',64)
                    FROM generate_series(1,?::integer) n
                    """, subject, recording, "call-" + session, session, subject, subject, recording, "call-" + session, session, count);
            execute(connection, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) SELECT md5('pa35-event-'||source_event_id)::uuid,id,'EVIDENCE',source_event_id,'EVIDENCE_CREATED','{}',repeat('b',64) FROM evidence.evidence_record");
            connection.commit();
        }
    }
    void artifact(UUID evidence, String role) throws Exception {
        UUID media = UUID.randomUUID();
        mutate("INSERT INTO evidence.media_object(id,storage_provider,bucket,object_key,object_version_id,sha256,size_bytes,mime_type,crypto_key_ref) VALUES (?,'COS','PRIVATE_BUCKET',?,'PRIVATE_VERSION',repeat('a',64),1,'audio/wav','PRIVATE_KEY')", media, "PRIVATE_OBJECT_" + media);
        mutate("INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) VALUES (?,?,?,?)", UUID.randomUUID(), evidence, media, role);
    }
    void directDecision(UUID session) throws Exception {
        UUID subject = subject(), grant = UUID.randomUUID(), balance = UUID.randomUUID(), reservation = UUID.randomUUID();
        mutate("INSERT INTO entitlement_metering.entitlement_grant(id,subject_id,source_type,source_ref_id,entitlement_type,granted_units,valid_from,expires_at,snapshot,snapshot_sha256) VALUES (?,?,'TRIAL',?,'RECORDING_SECONDS',100,now(),now()+interval '1 day','{}',repeat('a',64))", grant, subject, UUID.randomUUID());
        mutate("INSERT INTO entitlement_metering.entitlement_balance(id,grant_id,granted_units,reserved_units) VALUES (?,?,100,10)", balance, grant);
        mutate("INSERT INTO entitlement_metering.usage_reservation(id,call_id,balance_id,subject_id,authorized_units,authorization_expires_at) VALUES (?,?,?,?,10,now()+interval '1 hour')", reservation, "call-" + session, balance, subject);
        mutate("INSERT INTO ivr.ivr_decision(id,session_id,call_id,purpose,source_event_domain,source_event_id,request_fingerprint,action,reason_code,reservation_id,response_snapshot,response_payload_ciphertext,response_sha256,key_version) VALUES (?,?,?,'DIRECT_AUTHORIZATION','IVR_DIRECT_AUTH',?,repeat('a',64),NULL,'DIRECT_AUTHORIZED',?,'{}','v1.fixture.PRIVATE_DIRECT',repeat('b',64),1)", UUID.randomUUID(), session, "call-" + session, UUID.randomUUID().toString(), reservation);
    }
    void decision(UUID session) throws Exception {
        mutate("INSERT INTO ivr.ivr_decision(id,session_id,call_id,purpose,request_fingerprint,action,reason_code,response_snapshot,response_payload_ciphertext,response_sha256,key_version) VALUES (?,?,?,'INBOUND_RESOLVE',repeat('a',64),'GUIDE_ACTIVATION','ACTIVATION_REQUIRED','{}','v1.fixture.PRIVATE_DECISION',repeat('b',64),1)", UUID.randomUUID(), session, "call-" + session);
    }
    void leg(UUID session, String id, Instant time) throws Exception {
        mutate("INSERT INTO ivr.call_leg(id,call_id,leg_id,leg_type,direction,state,started_at) VALUES (?,?,?,'CALLER','INBOUND','RINGING',?::timestamptz)", UUID.randomUUID(), "call-" + session, id, time.toString());
    }
    void event(UUID session, String id, Integer sequence) throws Exception {
        mutate("""
                INSERT INTO ivr.call_event(id,call_id,event_id,sequence_no,event_type,occurred_at,payload_ciphertext,payload_redacted,payload_sha256)
                VALUES (?,?,?,?,'RINGING',?::timestamptz,'v1.fixture.PRIVATE_EVENT',
                  jsonb_build_object('source',jsonb_build_object('eventType','RINGING','legId',null,'sequenceNo',?::integer,'payload','{}'::jsonb),
                    'receipt',jsonb_build_object('payloadSha256',repeat('a',64),'eventFingerprint',repeat('b',64))),repeat('a',64))
                """, UUID.randomUUID(), "call-" + session, id, sequence, time.toString(), sequence);
    }
    void clean() throws Exception {
        for (int index = 0; index < 4; index++) try (var connection = pool.getConnection()) {
            assertThat(one(connection, "SELECT session_user=current_user")).isEqualTo("t");
            assertThat(connection.getAutoCommit()).isTrue(); assertThat(connection.isReadOnly()).isFalse();
            assertThat(one(connection, "SHOW statement_timeout")).isEqualTo("0"); assertThat(one(connection, "SHOW lock_timeout")).isEqualTo("0");
        }
        try (var connection = admin()) { assertThat(one(connection, "SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.usename='" + login + "' AND l.mode IN ('RowShareLock','RowExclusiveLock','ExclusiveLock','AccessExclusiveLock')")).isEqualTo("0"); }
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
        replaceService(new DefaultOperationsIvrSessionReadService(transaction, dsl, scope, factory.getBeanProvider(OperationsSubjectReadProperties.class), context.getBeanProvider(IvrSessionReader.class), context.getBeanProvider(IvrEvidenceReader.class)));
    }
    void replaceService(OperationsIvrSessionReadService replacement) { var factory = (DefaultListableBeanFactory) context.getBeanFactory(); factory.destroySingleton("operationsIvrSessionReadService"); factory.registerSingleton("operationsIvrSessionReadService", replacement); service = replacement; }
    @Override public void close() { try { if (context != null) context.close(); } finally { try { client.close(); } finally { base.close(); } } }
}
