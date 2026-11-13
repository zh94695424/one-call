package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.one;

import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.OneCallApplication;
import com.onecall.core.activation_consent.internal.command.ActivationSemantics;
import com.onecall.core.activation_consent.internal.operations.*;
import com.onecall.core.activation_consent.internal.operations.OperationsLifecycleRepository.*;
import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.*;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.jooq.*;
import org.jooq.impl.DefaultExecuteListener;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class OperationsSubjectLifecycleFixture implements AutoCloseable {
    final CustomerSubjectActivationFixture customer = new CustomerSubjectActivationFixture();
    final UUID employee = UUID.randomUUID(), secondEmployee = UUID.randomUUID(), disabled = UUID.randomUUID(), missing = UUID.randomUUID();
    final String login = "pa32_lifecycle_" + UUID.randomUUID().toString().replace("-", ""), password = UUID.randomUUID().toString();
    final List<StatementFact> statements = new CopyOnWriteArrayList<>();
    ConfigurableApplicationContext context;
    HikariDataSource pool;
    DSLContext dsl;
    TransactionTemplate transaction;
    OperationsLifecycleRepository repository;
    EmployeeAdministratorLock employeeLock;
    OperationsSubjectLifecycleService service;
    SyntheticEmployeeSessions sessions;
    UUID subject;
    int port;
    record StatementFact(String sql, String identity) { }
    @TestConfiguration(proxyBeanMethods = false)
    static class SessionBeans {
        @Bean SyntheticEmployeeSessions syntheticEmployeeSessions() { return new SyntheticEmployeeSessions(); }
    }
    void start() throws Exception {
        customer.base.postgres.withEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF8 --lc-collate=C --lc-ctype=C");
        customer.start();
        try (var connection = admin()) {
            int version = Integer.parseInt(one(connection, "SHOW server_version_num")); assertThat(version).isBetween(160000, 169999);
            assertThat(one(connection, "SHOW server_encoding")).isEqualTo("UTF8");
            execute(connection, "CREATE ROLE " + login + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '" + password + "'");
            for (String role : List.of("activation_consent_role", "operations_iam_role", "operations_service_role")) execute(connection, "GRANT " + role + " TO " + login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("onecall.operations-lifecycle.enabled", "true"); properties.put("onecall.operations-lifecycle.datasource.url", customer.base.url); properties.put("onecall.operations-lifecycle.datasource.username", login); properties.put("onecall.operations-lifecycle.datasource.password", password);
        properties.put("onecall.customer.persistence.url", customer.base.url); properties.put("onecall.customer.persistence.username", "pa27_customer"); properties.put("onecall.customer.persistence.password", customer.base.password);
        properties.put("onecall.persistence.url", customer.base.url); properties.put("onecall.persistence.username", "pa27_query"); properties.put("onecall.persistence.password", customer.base.password);
        properties.put("onecall.customer.business-numbers.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32])); properties.put("onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]));
        properties.put("onecall.sharing-command.enabled", "false"); properties.put("onecall.sharing-admission.datasource.enabled", "false"); properties.put("onecall.evidence-media-read.datasource.enabled", "false");
        context = new SpringApplicationBuilder(OneCallApplication.class, SessionBeans.class).profiles("web", "persistence").properties("server.port=0", "spring.main.banner-mode=off")
                .initializers(application -> application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa32", properties))).run();
        port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        pool = context.getBean("operationsLifecycleDataSource", HikariDataSource.class); pool.setMaximumPoolSize(2);
        dsl = context.getBean("operationsLifecycleDsl", DSLContext.class); transaction = context.getBean("operationsLifecycleTransactionTemplate", TransactionTemplate.class);
        repository = context.getBean("operationsLifecycleRepository", OperationsLifecycleRepository.class); service = context.getBean("operationsSubjectLifecycleService", OperationsSubjectLifecycleService.class);
        employeeLock = context.getBean("employeeAdministratorLock", EmployeeAdministratorLock.class); sessions = context.getBean(SyntheticEmployeeSessions.class);
        dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext execution) {
                try (var statement = execution.connection().createStatement(); var rows = statement.executeQuery("SELECT current_user||'|'||session_user||'|'||pg_backend_pid()||'|'||txid_current()")) {
                    rows.next(); statements.add(new StatementFact(execution.sql(), rows.getString(1)));
                } catch (SQLException unavailable) { throw new IllegalStateException("Fixture statement capture failed", unavailable); }
            }
        });
        reset();
    }
    void reset() throws Exception {
        customer.reset(); subject = customer.subject; customer.accept("original-" + UUID.randomUUID());
        try (var connection = admin()) {
            int index = 1;
            for (UUID identity : List.of(employee, secondEmployee, disabled)) {
                execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role,status) VALUES (?,?,'fixture','fixture',?,'fixture','OPS',?)", identity,
                        Integer.toHexString(index).repeat(64), Integer.toHexString(index + 3).repeat(64), identity.equals(disabled) ? "DISABLED" : "ACTIVE"); index++;
            }
        }
        sessions.sessions.clear(); sessions.failure = null;
        sessions.sessions.put("employee", new VerifiedEmployeeSession(employee, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("second", new VerifiedEmployeeSession(secondEmployee, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("disabled", new VerifiedEmployeeSession(disabled, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("missing", new VerifiedEmployeeSession(missing, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("admin", new VerifiedEmployeeSession(customer.employee, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("customer", new VerifiedEmployeeSession(employee, "onecall:customer-session:v1"));
        statements.clear();
    }
    Connection admin() throws SQLException { return customer.admin(); }
    Connection runtime() throws SQLException { return DriverManager.getConnection(customer.base.url, login, password); }
    void sql(String query, Object... values) throws Exception { try (var connection = admin()) { execute(connection, query, values); } }
    String scalar(String query) throws Exception { try (var connection = admin()) { return one(connection, query); } }
    String identity() { return dsl.fetchOne("SELECT current_user||'|'||session_user||'|'||pg_backend_pid()||'|'||txid_current()").get(0, String.class); }
    Command command(UUID target, String key, String reason, BigInteger revision) { return new Command(target, key, reason, revision); }
    Caller caller() { return new Caller(employee, EmployeeBearerAuthenticator.AUDIENCE); }
    HttpResponse<String> post(String token, UUID target, String route, String key, String reason, BigInteger revision) throws Exception {
        String body = JsonMapper.builder().build().writeValueAsString(Map.of("reasonCode", reason, "expectedActivationRevision", revision));
        return customer.base.client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/operations/subjects/" + target + "/" + route)).timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json").header("Idempotency-Key", key).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> suspend(String key) throws Exception { return post("employee", subject, "suspensions", key, "reason", BigInteger.TWO); }
    HttpResponse<String> resume(String key) throws Exception { return post("employee", subject, "resumptions", key, "reason", BigInteger.valueOf(3)); }
    JsonNode json(HttpResponse<String> response) { return JsonMapper.builder().build().readTree(response.body()); }
    void problem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status); assertThat(json(response).propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId"); assertThat(json(response).path("code").asString()).isEqualTo(code);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store"); assertThat(response.headers().firstValue("Set-Cookie")).isEmpty(); UUID.fromString(json(response).path("traceId").asString());
    }
    List<Long> effects() throws Exception {
        return List.of(Long.parseLong(scalar("SELECT count(*) FROM activation_consent.service_activation_transition WHERE source_action_domain='OPERATIONS'")), Long.parseLong(scalar("SELECT count(*) FROM platform.outbox_event WHERE event_type='ACTIVATION_TRANSITION_APPLIED' AND payload_redacted->>'sourceActionDomain'='OPERATIONS'")), Long.parseLong(scalar("SELECT count(*) FROM audit.audit_fact WHERE action_code IN ('SUBJECT_SUSPEND','SUBJECT_RESUME')")));
    }
    String allFacts() throws Exception {
        return scalar("SELECT jsonb_build_array((SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM activation_consent.service_activation r),(SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM activation_consent.service_activation_transition r),(SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM platform.outbox_event r),(SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM audit.audit_fact r))::text");
    }
    Map<String, String> business() throws Exception {
        var snapshot = new TreeMap<String, String>();
        try (var connection = admin(); var statement = connection.createStatement(); var rows = statement.executeQuery("SELECT n.nspname||'.'||c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind='r' AND n.nspname IN ('customer_iam','organization_access','operations_iam','subject_number','catalog_order','entitlement_metering','evidence','ivr','sharing','activation_consent') AND c.relname NOT IN ('service_activation','service_activation_transition') ORDER BY 1")) {
            while (rows.next()) { String table = rows.getString(1); snapshot.put(table, one(connection, "SELECT coalesce(jsonb_agg(to_jsonb(value) ORDER BY to_jsonb(value)::text),'[]'::jsonb)::text FROM " + table + " value")); }
        }
        return snapshot;
    }
    void assertFactSet(HttpResponse<String> response, String key, String from, String target, int revision) throws Exception {
        assertThat(response.statusCode()).isEqualTo(200); JsonNode body = json(response);
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("transitionId", "statusEventId", "fromStatus", "toStatus", "fromRevision", "toRevision", "sourceChannel", "sourceActionDomain", "reasonCode", "occurredAt");
        UUID transitionId = UUID.fromString(body.path("transitionId").asString());
        try (var connection = admin(); var statement = connection.prepareStatement("SELECT to_jsonb(t)::text FROM activation_consent.service_activation_transition t WHERE id=?")) {
            statement.setObject(1, transitionId); try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue(); JsonNode row = JsonMapper.builder().build().readTree(rows.getString(1)); UUID activation = UUID.fromString(row.path("activation_id").asString());
                assertThat(row.path("actor_employee_id").asString()).isEqualTo(employee.toString()); assertThat(row.path("actor_account_id").isNull()).isTrue(); assertThat(row.path("consent_record_id").isNull()).isTrue();
                assertThat(body.path("fromStatus").asString()).isEqualTo(from); assertThat(body.path("toStatus").asString()).isEqualTo(target); assertThat(body.path("fromRevision").asInt()).isEqualTo(revision); assertThat(body.path("toRevision").asInt()).isEqualTo(revision + 1);
                assertThat(row.path("source_channel").asString()).isEqualTo("OPERATIONS"); assertThat(row.path("source_action_domain").asString()).isEqualTo("OPERATIONS"); assertThat(row.path("status_event_domain").asString()).isEqualTo("ACTIVATION_STATUS"); assertThat(row.path("from_revision").asInt()).isEqualTo(revision);
                assertThat(OffsetDateTime.parse(row.path("occurred_at").asString()).toInstant()).isEqualTo(Instant.parse(body.path("occurredAt").asString()));
                assertThat(row.path("source_action_event_id").asString()).isEqualTo(key); assertThat(row.path("from_status").asString()).isEqualTo(from); assertThat(row.path("to_status").asString()).isEqualTo(target); assertThat(row.path("to_revision").asInt()).isEqualTo(revision + 1);
                assertThat(row.path("reason_code").asString()).isEqualTo(body.path("reasonCode").asString());
                String outboxText = scalar("SELECT payload_redacted::text FROM platform.outbox_event WHERE dedupe_key='activation_transition:" + transitionId + "' AND event_domain='ACTIVATION_STATUS' AND aggregate_type='SERVICE_ACTIVATION' AND aggregate_id='" + activation + "'");
                JsonNode payload = JsonMapper.builder().build().readTree(outboxText);
                assertThat(payload.propertyNames()).containsExactlyInAnyOrder("activationId", "transitionId", "subjectId", "fromStatus", "toStatus", "fromRevision", "toRevision", "sourceChannel", "sourceActionDomain", "sourceActionEventId", "statusEventDomain", "statusEventId", "occurredAt");
                assertThat(payload.path("occurredAt").asString()).isEqualTo(body.path("occurredAt").asString());
                assertThat(scalar("SELECT payload_sha256 FROM platform.outbox_event WHERE dedupe_key='activation_transition:" + transitionId + "'")).isEqualTo(ActivationSemantics.sha(ActivationSemantics.canonical(JsonMapper.builder().build().readValue(outboxText, Map.class))));
                String status = ActivationSemantics.statusEvent(activation, revision + 1); assertThat(row.path("status_event_id").asString()).isEqualTo(status); assertThat(body.path("statusEventId").asString()).isEqualTo(status);
                assertThat(row.path("request_fingerprint").asString()).isEqualTo(OperationsLifecycleSemantics.fingerprint(command(subject, key, body.path("reasonCode").asString(), BigInteger.valueOf(revision)), caller(), target, status));
                assertThat(scalar("SELECT count(*) FROM activation_consent.service_activation a JOIN activation_consent.service_activation_transition t ON t.activation_id=a.id WHERE t.id='" + transitionId + "' AND a.status=t.to_status AND a.revision=t.to_revision AND a.updated_at=t.occurred_at")).isEqualTo("1");
                JsonNode audit = JsonMapper.builder().build().readTree(scalar("SELECT to_jsonb(value)::text FROM audit.audit_fact value WHERE request_id='" + status + "'"));
                assertThat(audit.path("before_snapshot").propertyNames()).containsExactlyInAnyOrder("status", "revision", "reasonCode"); assertThat(audit.path("after_snapshot").propertyNames()).containsExactlyInAnyOrder("status", "revision", "reasonCode");
                assertThat(audit.path("before_snapshot").path("reasonCode").asString()).isEqualTo(body.path("reasonCode").asString()); assertThat(audit.path("after_snapshot").path("reasonCode").asString()).isEqualTo(body.path("reasonCode").asString());
                assertThat(audit.path("actor_type").asString()).isEqualTo("EMPLOYEE"); assertThat(audit.path("object_type").asString()).isEqualTo("SUBJECT"); assertThat(audit.path("reason_code").asString()).isEqualTo(body.path("reasonCode").asString()); UUID.fromString(audit.path("trace_id").asString());
                assertThat(scalar("SELECT count(*) FROM audit.audit_fact WHERE actor_id='" + employee + "' AND object_id='" + subject + "' AND action_code='" + (target.equals("SUSPENDED") ? "SUBJECT_SUSPEND" : "SUBJECT_RESUME") + "' AND request_id='" + status + "' AND before_snapshot->>'status'='" + from + "' AND before_snapshot->>'revision'='" + revision + "' AND after_snapshot->>'revision'='" + (revision + 1) + "' AND occurred_at=(SELECT occurred_at FROM activation_consent.service_activation_transition WHERE id='" + transitionId + "')")).isEqualTo("1");
            }
        }
    }
    OperationsSubjectLifecycleService with(OperationsLifecycleRepository replacement) { return new DefaultOperationsSubjectLifecycleService(transaction, dsl, replacement, context.getBeanProvider(EmployeeAdministratorLock.class)); }
    OperationsLifecycleRepository failAfter(String point) {
        return (OperationsLifecycleRepository) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {OperationsLifecycleRepository.class}, (proxy, method, arguments) -> {
            try { Object result = method.invoke(repository, arguments); if (method.getName().equals(point)) throw new IllegalStateException("FIXTURE_SECRET_FAILURE"); return result; }
            catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
        });
    }
    OperationsSubjectLifecycleService commitFailure() {
        var manager = new DataSourceTransactionManager(pool) {
            @Override protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) { throw new org.springframework.transaction.TransactionSystemException("FIXTURE_SECRET_COMMIT"); }
        };
        manager.setRollbackOnCommitFailure(true); var template = new TransactionTemplate(manager); template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return new DefaultOperationsSubjectLifecycleService(template, dsl, repository, context.getBeanProvider(EmployeeAdministratorLock.class));
    }
    List<HttpResponse<String>> concurrent(UUID leftSubject, String leftRoute, String leftKey, int leftRevision, UUID rightSubject, String rightRoute, String rightKey, int rightRevision) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor(); var blocker = admin()) {
            blocker.setAutoCommit(false);
            for (UUID target : java.util.stream.Stream.of(leftSubject, rightSubject).distinct().sorted().toList()) execute(blocker, "SELECT pg_advisory_xact_lock(hashtextextended(?,0))", "subject_number.subject:" + target);
            var barrier = new CyclicBarrier(3);
            var left = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return post("employee", leftSubject, leftRoute, leftKey, "reason", BigInteger.valueOf(leftRevision)); });
            var right = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return post("employee", rightSubject, rightRoute, rightKey, "reason", BigInteger.valueOf(rightRevision)); });
            barrier.await(10, TimeUnit.SECONDS); awaitWaiters(2, "advisory"); blocker.commit();
            return List.of(left.get(30, TimeUnit.SECONDS), right.get(30, TimeUnit.SECONDS));
        }
    }
    void awaitWaiters(int count, String lock) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15); boolean observed = false;
        while (System.nanoTime() < deadline) {
            if (Integer.parseInt(scalar("SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.usename='" + login + "' AND NOT l.granted" + (lock == null ? "" : " AND l.locktype='" + lock + "'"))) >= count) { observed = true; break; }
            Thread.onSpinWait();
        }
        assertThat(observed).as("real independent runtime backends waiting on PostgreSQL locks").isTrue();
    }
    void cleanPool() throws Exception {
        transaction.execute(status -> { assertThat(identity()).startsWith(login + "|" + login + "|"); return null; });
        assertThat(scalar("SELECT count(*) FROM pg_stat_activity WHERE usename='" + login + "' AND state='idle in transaction'")).isEqualTo("0");
        assertThat(scalar("SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.usename='" + login + "' AND l.locktype='advisory'")).isEqualTo("0");
    }
    @Override public void close() {
        try { if (context != null) context.close(); if (customer.base.url != null) try (var connection = admin()) { execute(connection, "DROP ROLE IF EXISTS " + login); } }
        catch (SQLException failure) { throw new IllegalStateException("Operations fixture cleanup failed"); }
        finally { customer.close(); }
    }
}
