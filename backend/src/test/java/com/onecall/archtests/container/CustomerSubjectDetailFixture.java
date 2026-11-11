package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.*;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.core.env.MapPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class CustomerSubjectDetailFixture implements AutoCloseable {
    final CustomerBusinessNumbersFixture base = new CustomerBusinessNumbersFixture();
    final List<String> queries = new CopyOnWriteArrayList<>();
    final List<String> states = new CopyOnWriteArrayList<>();
    UUID subject;
    UUID foreign;
    UUID unowned;
    volatile String failOn;
    volatile Runnable afterOwnership;
    volatile Runnable afterLifecycle;

    void start() throws Exception {
        base.start(null);
        base.context = new SpringApplicationBuilder(OneCallApplication.class, CustomerBusinessNumbersFixture.SessionBeans.class)
                .profiles("web").properties("server.port=0", "spring.main.banner-mode=off")
                .initializers(application -> application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa28", Map.of(
                        "onecall.customer.persistence.url", base.url, "onecall.customer.persistence.username", "pa27_customer",
                        "onecall.customer.persistence.password", base.password)))).run();
        base.context.getBean("customerQueryDataSource", HikariDataSource.class).setMaximumPoolSize(1);
        dsl().configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext execution) {
                String sql = execution.sql(); queries.add(sql);
                if (failOn != null && sql.contains(failOn)) throw new org.jooq.exception.DataAccessException("SQL_CANARY",
                        new SQLException("PASSWORD_CANARY", "42501"));
            }
            @Override public void executeEnd(ExecuteContext execution) {
                if (!execution.sql().stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT")) return;
                try { states.add(one(execution.connection(), "SELECT current_user||'|'||session_user||'|'||current_setting('transaction_read_only')||'|'||current_setting('transaction_isolation')||'|'||pg_backend_pid()")); }
                catch (SQLException failure) { throw new IllegalStateException("Invalid fixture state"); }
                if (execution.sql().contains("FROM subject_number.business_subject") && afterOwnership != null) {
                    Runnable hook = afterOwnership; afterOwnership = null; hook.run();
                }
                if (execution.sql().contains("FROM customer_iam.customer_account") && afterLifecycle != null) {
                    Runnable hook = afterLifecycle; afterLifecycle = null; hook.run();
                }
            }
        });
    }
    void seed() throws Exception {
        base.seed();
        subject = subject(base.account, null); foreign = subject(base.other, null); unowned = subject(null, null);
        queries.clear(); states.clear(); failOn = null; afterOwnership = null; afterLifecycle = null;
    }
    UUID subject(UUID owner, UUID organization) throws Exception {
        UUID id = UUID.randomUUID();
        try (var connection = admin()) {
            execute(connection, "INSERT INTO subject_number.business_subject(id,owner_account_id,owner_organization_id,source) VALUES (?,?,?,'WEB')", id, owner, organization);
        }
        return id;
    }
    UUID binding(UUID id, String status) throws Exception {
        UUID binding = UUID.randomUUID();
        try (var connection = admin()) {
            execute(connection, "INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,status,verified_at) VALUES (?,?,'MOBILE','v1.fixture.CIPHERTEXT_CANARY',?,'138****0000',?,CASE WHEN ?='ACTIVE' THEN now() END)",
                    binding, id, UUID.randomUUID().toString().replace("-", "").repeat(2), status, status);
        }
        return binding;
    }
    void risk(UUID binding, String type, String state) throws Exception {
        try (var connection = admin()) {
            execute(connection, "INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source,cleared_at) VALUES (?,?,?,?,'IMPORT',CASE WHEN ?='CLEARED' THEN now() END)",
                    UUID.randomUUID(), binding, type, state, state);
        }
    }
    void clearRisk() throws Exception {
        try (var connection = admin()) { execute(connection, "UPDATE subject_number.subject_risk_state SET state='CLEARED',cleared_at=now(),revision=revision+1 WHERE state='OPEN'"); }
    }
    void activation(UUID id, String status) throws Exception {
        try (var connection = admin()) {
            execute(connection, "INSERT INTO activation_consent.service_activation(id,subject_id,status) VALUES (?,?,?) ON CONFLICT(subject_id) DO UPDATE SET status=excluded.status,revision=service_activation.revision+1,updated_at=now()",
                    UUID.randomUUID(), id, status);
        }
    }
    void evidence(UUID id, String status) throws Exception {
        UUID evidence = UUID.randomUUID(); UUID recording = UUID.randomUUID(); String event = UUID.randomUUID().toString();
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, """
                    INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,source_event_id,
                        identity_at_capture,consent_version,snapshot,snapshot_sha256,status,solidified_at)
                    VALUES (?,?,'CALL_RECORDING',?,'EVIDENCE',?,
                        jsonb_build_object('subjectId',?::text,'capturedAt','2026-01-01T00:00:00Z','sourceChannel','IVR'),'CONSENT_CANARY',
                        jsonb_build_object('capture','{}'::jsonb,'content',jsonb_build_object('secret','CONTENT_CANARY'),
                            'sourceEventDomain','EVIDENCE','sourceEventId',?::text,'source',jsonb_build_object('subjectId',?::text,'recordingId',?::text)),
                        repeat('a',64),?,CASE WHEN ?='SOLIDIFIED' THEN now() END)
                    """, evidence, id, recording, event, id, event, id, recording, status, status);
            execute(connection, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) VALUES (?,?,'EVIDENCE',?,'EVIDENCE_CREATED','{}'::jsonb,repeat('b',64))", UUID.randomUUID(), evidence, event);
            connection.commit();
        }
    }
    void order(UUID id, UUID purchaser, UUID organization, String status) throws Exception {
        UUID employee = UUID.randomUUID(); UUID product = UUID.randomUUID(); UUID version = UUID.randomUUID();
        UUID offer = UUID.randomUUID(); UUID consent = UUID.randomUUID(); UUID order = UUID.randomUUID();
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role) VALUES (?,?,'fixture','fixture',?,'fixture','ADMIN')", employee,
                    UUID.randomUUID().toString().replace("-", "").repeat(2), UUID.randomUUID().toString().replace("-", "").repeat(2));
            execute(connection, "INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (?,?,'CORE')", product, "PA28_" + product.toString().replace("-", "").toUpperCase(Locale.ROOT));
            execute(connection, "INSERT INTO catalog_order.catalog_version(id,product_id,version_no,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,created_by_employee_id) VALUES (?,?,1,'ONE_TIME','PERSONAL',0,'CNY','ONCE',1,0,'{}'::jsonb,repeat('c',64),?)", version, product, employee);
            execute(connection, "INSERT INTO catalog_order.catalog_offer(id,catalog_version_id,channel,offer_status,version_token) VALUES (?,?,'WEB','ACTIVE','v1')", offer, version);
            execute(connection, "INSERT INTO activation_consent.consent_record(id,subject_id,purpose,channel,event_domain,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,snapshot,snapshot_sha256,event_id,request_fingerprint) VALUES (?,?,'PURCHASE','WEB','PURCHASE','fixture','v1','v1','ACCEPT','ACCOUNT',?,?,'{}'::jsonb,repeat('d',64),?,repeat('e',64))", consent, id, consent.toString(), purchaser, consent.toString());
            execute(connection, "INSERT INTO catalog_order.customer_order(id,order_no,account_id,owner_organization_id,subject_id,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,version_token,idempotency_key,request_fingerprint,paid_at,closed_at) VALUES (?,?,?,?,?,'WEB',?,?,?,0,'CNY',?,jsonb_build_object('secret','ORDER_CANARY'),repeat('f',64),'v1',?,repeat('a',64),CASE WHEN ?='PAID' THEN now() END,CASE WHEN ?='CLOSED' THEN now() END)",
                    order, order.toString(), purchaser, organization, id, version, offer, consent, status, order.toString(), status, status);
            connection.commit();
        }
    }
    HttpResponse<String> request(String method, String value, String token, HttpRequest.BodyPublisher body) throws Exception {
        int port = ((ServletWebServerApplicationContext) base.context).getWebServer().getPort();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/customer/subjects/" + value))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "text/plain").method(method, body);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return base.client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode get(UUID id, String token, int status) throws Exception { return check(request("GET", id.toString(), token, HttpRequest.BodyPublishers.noBody()), status); }
    JsonNode check(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.body()).doesNotContain(base.password, "CANARY", "ownerAccountId", "ownerOrganizationId", "numberMasked", "claimState", "consentRequirements", "trial");
        var json = JsonMapper.builder().build().readTree(response.body());
        assertThat(json.propertyNames()).containsExactlyInAnyOrderElementsOf(status == 200
                ? List.of("subjectId", "activationStatus", "evidenceCount", "orderCount") : List.of("type", "title", "status", "code", "traceId"));
        if (status != 200) assertThat(json.path("code").asString()).isEqualTo(switch (status) {
            case 401 -> "UNAUTHENTICATED"; case 403 -> "FORBIDDEN"; case 404 -> "NOT_FOUND"; case 422 -> "VALIDATION_FAILED"; case 503 -> "SERVICE_UNAVAILABLE"; default -> "INTERNAL";
        });
        return json;
    }
    TrustedCustomerCaller caller() { return new TrustedCustomerCaller(base.account, TrustedCustomerCaller.Channel.WEB, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)); }
    CustomerSubjectDetailQueryService service() { return base.context.getBean("customerSubjectDetailQueryService", CustomerSubjectDetailQueryService.class); }
    DSLContext dsl() { return base.context.getBean("customerQueryDsl", DSLContext.class); }
    Connection admin() throws SQLException { return base.admin(); }
    void mutate(String sql, Object... values) throws SQLException { try (var connection = admin()) { execute(connection, sql, values); } }
    String snapshot() throws Exception { return base.businessSnapshot(); }
    void cleanPool() throws Exception { base.cleanPool(); }
    @Override public void close() { base.close(); }
}
