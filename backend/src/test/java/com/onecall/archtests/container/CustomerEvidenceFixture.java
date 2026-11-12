package com.onecall.archtests.container;

import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.onecall.core.customer_iam.internal.application.*;
import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class CustomerEvidenceFixture implements AutoCloseable {
    final CustomerBusinessNumbersFixture base = new CustomerBusinessNumbersFixture();
    final List<String> queries = new CopyOnWriteArrayList<>();
    final List<String> states = new CopyOnWriteArrayList<>();
    final Instant capturedAt = Instant.parse("2025-01-02T03:04:05Z");
    UUID subject;
    UUID foreign;
    UUID unowned;
    UUID evidence;
    volatile String failOn;
    volatile Runnable afterOwnership;
    volatile Runnable afterLifecycle;
    volatile boolean probeWrite;
    volatile String writeSqlState;

    void start() throws Exception {
        base.start(null);
        base.web();
        dsl().configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext execution) {
                queries.add(execution.sql());
                if (failOn != null && execution.sql().contains(failOn))
                    throw new org.jooq.exception.DataAccessException("SQL_CANARY", new SQLException("PASSWORD_CANARY", "42501"));
            }
            @Override public void executeEnd(ExecuteContext execution) {
                if (!execution.sql().stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT")) return;
                try {
                    states.add(one(execution.connection(), "SELECT current_user||'|'||session_user||'|'||current_setting('transaction_read_only')||'|'||current_setting('transaction_isolation')||'|'||pg_backend_pid()"));
                    if (probeWrite) {
                        probeWrite = false;
                        try (var statement = execution.connection().createStatement()) {
                            statement.executeUpdate("UPDATE customer_iam.customer_account SET display_name=display_name");
                            throw new AssertionError("Read-only customer connection accepted a write");
                        } catch (SQLException denied) { writeSqlState = denied.getSQLState(); }
                    }
                } catch (SQLException failure) { throw new IllegalStateException("Fixture connection inspection failed", failure); }
                if (execution.sql().contains("subject_number.business_subject") && afterOwnership != null) {
                    Runnable hook = afterOwnership; afterOwnership = null; hook.run();
                }
                if (execution.sql().contains("customer_iam.customer_account") && afterLifecycle != null) {
                    Runnable hook = afterLifecycle; afterLifecycle = null; hook.run();
                }
            }
        });
    }

    void seed() throws Exception {
        base.seed();
        base.session("other", base.other, null);
        subject = subject(base.account, null); foreign = subject(base.other, null); unowned = subject(null, null);
        evidence = evidence(subject, "PENDING");
        resetObservations();
    }

    void resetObservations() {
        queries.clear(); states.clear(); failOn = null; afterOwnership = null; afterLifecycle = null;
        probeWrite = false; writeSqlState = null;
    }

    UUID subject(UUID account, UUID organization) throws SQLException {
        UUID id = UUID.randomUUID();
        mutate("INSERT INTO subject_number.business_subject(id,owner_account_id,owner_organization_id,source) VALUES (?,?,?,'WEB')", id, account, organization);
        return id;
    }

    UUID binding(UUID subjectId, String status) throws SQLException {
        UUID id = UUID.randomUUID();
        mutate("INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,status,verified_at) VALUES (?,?,'MOBILE','v1.fixture.CIPHERTEXT_CANARY',?,'MASK_CANARY',?,CASE WHEN ?='ACTIVE' THEN now() END)",
                id, subjectId, UUID.randomUUID().toString().replace("-", "").repeat(2), status, status);
        return id;
    }

    void risk(UUID binding, String state) throws SQLException {
        mutate("INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source,cleared_at) VALUES (?,?,'CONFLICT',?,'IMPORT',CASE WHEN ?='CLEARED' THEN now() END)",
                UUID.randomUUID(), binding, state, state);
    }

    void clearRisk() throws SQLException {
        mutate("UPDATE subject_number.subject_risk_state SET state='CLEARED',cleared_at=now(),revision=revision+1 WHERE state='OPEN'");
    }

    UUID evidence(UUID subjectId, String status) throws SQLException {
        return evidence(subjectId, status, UUID.randomUUID(), base.time, UUID.randomUUID(), "\"" + capturedAt + "\"");
    }

    UUID evidence(UUID subjectId, String status, UUID id, Instant createdAt, UUID source, String captureJson) throws SQLException {
        String event = UUID.randomUUID().toString();
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, """
                    INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,source_event_id,
                        identity_at_capture,consent_version,snapshot,snapshot_sha256,status,created_at,solidified_at)
                    VALUES (?,?,'CALL_RECORDING',?,'EVIDENCE',?,
                        jsonb_build_object('subjectId',?::text,'capturedAt',?::jsonb,'sourceChannel','IVR',
                            'bindingId','BINDING_CANARY','identity','IDENTITY_CANARY','number','NUMBER_CANARY'), 'consent-v1',
                        jsonb_build_object('capture',jsonb_build_object('capturedAt','2099-01-01T00:00:00Z','identity','SNAPSHOT_CANARY'),
                            'content','CONTENT_CANARY','status','FAILED','consentVersion','SNAPSHOT_CONSENT_CANARY',
                            'sourceEventDomain','EVIDENCE','sourceEventId',?::text,
                            'source',jsonb_build_object('subjectId',?::text,'recordingId',?::text)),
                        repeat('a',64),?,?,CASE WHEN ?='SOLIDIFIED' THEN ?::timestamptz END)
                    """, id, subjectId, source, event, subjectId, captureJson, event, subjectId, source,
                    status, createdAt.atOffset(ZoneOffset.UTC), status, createdAt.atOffset(ZoneOffset.UTC));
            execute(connection, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) VALUES (?,?,'EVIDENCE',?,'EVIDENCE_CREATED',jsonb_build_object('secret','EVENT_CANARY'),repeat('b',64))",
                    UUID.randomUUID(), id, event);
            connection.commit();
        }
        return id;
    }

    UUID media(String hash, long size, String mime) throws SQLException {
        UUID id = UUID.randomUUID();
        mutate("INSERT INTO evidence.media_object(id,storage_provider,bucket,object_key,object_version_id,sha256,size_bytes,mime_type,crypto_key_ref) VALUES (?,'COS','BUCKET_CANARY',?,'VERSION_CANARY',?,?,?,'CRYPTO_CANARY')",
                id, "OBJECT_CANARY_" + id, hash, size, mime);
        return id;
    }

    void artifact(UUID evidenceId, UUID media, String role) throws SQLException {
        mutate("INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) VALUES (?,?,?,?)", UUID.randomUUID(), evidenceId, media, role);
    }

    void recording(UUID id, UUID media, String status, int duration) throws SQLException {
        UUID session = UUID.randomUUID(); String call = "CALL_CANARY_" + id;
        mutate("INSERT INTO ivr.inbound_session(id,call_id,subject_id,trusted_caller,access_number_masked,node_id,protocol_version,prompt_version) VALUES (?,?,?,false,'MASK_CANARY','fixture','v1','v1')", session, call, subject);
        mutate("INSERT INTO ivr.call_record(id,call_id,session_id,subject_id,caller_ciphertext,caller_number_hmac,caller_masked,access_number_masked,node_id,protocol_version) VALUES (?,?,?,?,'v1.fixture.CALLER_CANARY',?,'MASK_CANARY','MASK_CANARY','fixture','v1')", UUID.randomUUID(), call, session, subject, UUID.randomUUID().toString().replace("-", "").repeat(2));
        mutate("""
                INSERT INTO evidence.recording(id,call_id,recording_id,media_object_id,declared_format,declared_size,
                    declared_duration,declared_sha256,validated_duration,validated_sha256,status,upload_expires_at,validated_at)
                VALUES (?,?,?,?,'DECLARED_CANARY',999,999,repeat('f',64),
                    CASE WHEN ?='VALIDATED' THEN ?::integer END,CASE WHEN ?='VALIDATED' THEN repeat('a',64) END,?,
                    now()+interval '1 day',CASE WHEN ? IN ('VALIDATED','QUARANTINED') THEN now() END)
                """, id, call, id.toString(), media, status, duration, status, status, status);
    }

    void transition(UUID id, String status) throws SQLException {
        mutate("UPDATE evidence.evidence_record SET status=?,revision=revision+1,solidified_at=CASE WHEN ?='SOLIDIFIED' THEN ?::timestamptz END WHERE id=?",
                status, status, base.time.plusSeconds(60).atOffset(ZoneOffset.UTC), id);
    }

    void order(UUID subjectId, UUID purchaser, UUID organization) throws SQLException {
        UUID employee = UUID.randomUUID(); UUID product = UUID.randomUUID(); UUID version = UUID.randomUUID();
        UUID offer = UUID.randomUUID(); UUID consent = UUID.randomUUID(); UUID order = UUID.randomUUID();
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role) VALUES (?,?,'fixture','fixture',?,'fixture','ADMIN')", employee,
                    UUID.randomUUID().toString().replace("-", "").repeat(2), UUID.randomUUID().toString().replace("-", "").repeat(2));
            execute(connection, "INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (?,?,'CORE')", product, "PA29_" + product.toString().replace("-", "").toUpperCase(Locale.ROOT));
            execute(connection, "INSERT INTO catalog_order.catalog_version(id,product_id,version_no,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,created_by_employee_id) VALUES (?,?,1,'ONE_TIME','PERSONAL',0,'CNY','ONCE',1,0,'{}'::jsonb,repeat('c',64),?)", version, product, employee);
            execute(connection, "INSERT INTO catalog_order.catalog_offer(id,catalog_version_id,channel,offer_status,version_token) VALUES (?,?,'WEB','ACTIVE','v1')", offer, version);
            execute(connection, "INSERT INTO activation_consent.consent_record(id,subject_id,purpose,channel,event_domain,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,snapshot,snapshot_sha256,event_id,request_fingerprint) VALUES (?,?,'PURCHASE','WEB','PURCHASE','fixture','v1','v1','ACCEPT','ACCOUNT',?,?,'{}'::jsonb,repeat('d',64),?,repeat('e',64))", consent, subjectId, consent.toString(), purchaser, consent.toString());
            execute(connection, "INSERT INTO catalog_order.customer_order(id,order_no,account_id,owner_organization_id,subject_id,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,version_token,idempotency_key,request_fingerprint) VALUES (?,?,?,?,?,'WEB',?,?,?,0,'CNY','UNPAID',jsonb_build_object('secret','ORDER_CANARY'),repeat('f',64),'v1',?,repeat('a',64))",
                    order, order.toString(), purchaser, organization, subjectId, version, offer, consent, order.toString());
            connection.commit();
        }
    }

    HttpResponse<String> request(String method, String suffix, String token, HttpRequest.BodyPublisher body) throws Exception {
        int port = ((ServletWebServerApplicationContext) base.context).getWebServer().getPort();
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/customer/evidence" + suffix))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "text/plain").method(method, body);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return base.client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    JsonNode list(String token, String query, int status) throws Exception { return check(request("GET", query, token, HttpRequest.BodyPublishers.noBody()), status); }
    JsonNode detail(UUID id, String token, int status) throws Exception { return check(request("GET", "/" + id, token, HttpRequest.BodyPublishers.noBody()), status); }

    JsonNode check(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(response.headers().firstValue("Pragma")).contains("no-cache");
        assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        assertThat(response.body()).doesNotContain(base.password, "CANARY", "ownerAccountId", "ownerOrganizationId", "identityAtCapture",
                "snapshot", "sourceRefId", "sourceEvent", "mediaObjectId", "objectKey", "bucket", "cryptoKeyRef", "shareToken", "shareCode",
                "playback", "download", "evidenceEvent", "validatedSha256", "declaredDuration", "jdbc:", "SQLException");
        var json = JsonMapper.builder().build().readTree(response.body());
        if (status != 200) {
            keys(json, "type", "title", "status", "code", "traceId");
            assertThat(json.path("status").asInt()).isEqualTo(status);
            assertThat(json.path("code").asString()).isEqualTo(switch (status) {
                case 400 -> "CURSOR_INVALID"; case 401 -> "UNAUTHENTICATED"; case 403 -> "FORBIDDEN";
                case 404 -> "NOT_FOUND"; case 422 -> "VALIDATION_FAILED"; case 503 -> "SERVICE_UNAVAILABLE"; default -> "INTERNAL";
            });
            assertThat(UUID.fromString(json.path("traceId").asString())).isNotNull();
        }
        return json;
    }

    static void keys(JsonNode value, String... names) { assertThat(value.propertyNames()).containsExactlyInAnyOrder(names); }

    List<UUID> ids(JsonNode page) {
        keys(page, "items", "nextCursor", "hasMore");
        List<UUID> result = new ArrayList<>();
        for (var item : page.path("items")) {
            keys(item, "evidenceId", "subjectId", "status", "sourceType", "capturedAt", "consentVersion", "createdAt", "artifactRoles");
            result.add(UUID.fromString(item.path("evidenceId").asString()));
        }
        return result;
    }

    TrustedCustomerCaller caller() { return new TrustedCustomerCaller(base.account, TrustedCustomerCaller.Channel.WEB, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)); }
    CustomerEvidenceQueryService service() { return base.context.getBean("customerEvidenceQueryService", CustomerEvidenceQueryService.class); }
    DSLContext dsl() { return base.context.getBean("customerQueryDsl", DSLContext.class); }
    Connection admin() throws SQLException { return base.admin(); }
    void mutate(String sql, Object... values) throws SQLException { try (var connection = admin()) { execute(connection, sql, values); } }
    String snapshot() throws Exception { return base.businessSnapshot(); }
    void cleanPool() throws Exception { base.cleanPool(); }
    @Override public void close() { base.close(); }
}
