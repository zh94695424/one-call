package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.one;

import com.onecall.core.OneCallApplication;
import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService;
import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.*;
import com.onecall.core.activation_consent.internal.command.*;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService;
import com.onecall.core.entitlement_metering.internal.activation.DefaultActivationTrialService;
import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.jooq.DSLContext;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

final class CustomerSubjectActivationFixture implements AutoCloseable {
    final CustomerBusinessNumbersFixture base = new CustomerBusinessNumbersFixture();
    final UUID account = base.account, other = base.other, organization = base.organizations.getFirst(), otherOrganization = UUID.randomUUID(), employee = UUID.randomUUID();
    final String login = "pa31_activation_" + UUID.randomUUID().toString().replace("-", ""), password = UUID.randomUUID().toString();
    ConfigurableApplicationContext context;
    HikariDataSource pool;
    DSLContext dsl;
    TransactionTemplate transaction;
    CustomerSubjectActivationService service;
    ActivationTrialService trials;
    SubjectActivationRepository repository;
    UUID subject, policy;
    int version, port;
    void start() throws Exception {
        base.start(null);
        try (var connection = admin()) {
            execute(connection, "CREATE ROLE " + login + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '" + password + "'");
            execute(connection, "GRANT activation_consent_role TO " + login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
            execute(connection, "GRANT entitlement_metering_role TO " + login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
            execute(connection, "ALTER ROLE pa27_query PASSWORD '" + base.password + "'");
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("onecall.activation-command.enabled", "true"); properties.put("onecall.activation-command.datasource.url", base.url); properties.put("onecall.activation-command.datasource.username", login); properties.put("onecall.activation-command.datasource.password", password);
        properties.put("onecall.customer.persistence.url", base.url); properties.put("onecall.customer.persistence.username", "pa27_customer"); properties.put("onecall.customer.persistence.password", base.password);
        properties.put("onecall.persistence.url", base.url); properties.put("onecall.persistence.username", "pa27_query"); properties.put("onecall.persistence.password", base.password);
        properties.put("onecall.customer.business-numbers.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32])); properties.put("onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]));
        properties.put("onecall.sharing-command.enabled", "false"); properties.put("onecall.sharing-admission.datasource.enabled", "false"); properties.put("onecall.evidence-media-read.datasource.enabled", "false");
        context = new SpringApplicationBuilder(OneCallApplication.class, CustomerBusinessNumbersFixture.SessionBeans.class).profiles("web", "persistence").properties("server.port=0", "spring.main.banner-mode=off")
                .initializers(application -> application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa31", properties))).run();
        port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        pool = context.getBean("activationCommandDataSource", HikariDataSource.class); pool.setMaximumPoolSize(1); pool.getHikariPoolMXBean().softEvictConnections();
        dsl = context.getBean("activationCommandDsl", DSLContext.class); transaction = context.getBean("activationCommandTransactionTemplate", TransactionTemplate.class);
        repository = context.getBean("subjectActivationRepository", SubjectActivationRepository.class); trials = context.getBean("activationTrialService", ActivationTrialService.class); service = context.getBean("customerSubjectActivationService", CustomerSubjectActivationService.class);
        assertThat(service).isInstanceOf(DefaultCustomerSubjectActivationService.class); assertThat(repository).isInstanceOf(JooqSubjectActivationRepository.class); assertThat(trials).isInstanceOf(DefaultActivationTrialService.class);
        assertThat(context.getBean("activationCommandTransactionManager", DataSourceTransactionManager.class).getDataSource()).isSameAs(pool);
        assertThat(transaction.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED); assertThat(transaction.isReadOnly()).isFalse();
        transaction.execute(status -> { repository.enter(dsl); String before = identity(); trials.readCurrentPolicy(); assertThat(identity()).isEqualTo(before); assertThat(dsl.fetchOne("SHOW TimeZone").get(0, String.class)).isEqualTo("UTC"); return null; });
        reset();
    }
    String identity() { return dsl.fetchOne("SELECT current_user||'|'||session_user||'|'||pg_backend_pid()||'|'||txid_current()").get(0, String.class); }
    Connection admin() throws SQLException { return base.admin(); }
    Connection runtime() throws SQLException { return DriverManager.getConnection(base.url, login, password); }
    void reset() throws Exception {
        try (var connection = admin()) {
            execute(connection, "TRUNCATE customer_iam.customer_account,organization_access.organization,subject_number.business_subject,operations_iam.employee_account,platform.outbox_event,audit.audit_fact CASCADE");
            execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'Customer'),(?,'Other')", account, other);
            execute(connection, "INSERT INTO organization_access.organization(id,display_name) VALUES (?,'Organization'),(?,'Other organization')", organization, otherOrganization);
            execute(connection, "INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role,status) VALUES (?,?,?,'MEMBER','ACTIVE')", UUID.randomUUID(), organization, account);
            execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role) VALUES (?,?,'fixture','fixture',?,'fixture','ADMIN')", employee, "a".repeat(64), "b".repeat(64));
        }
        version = 0; policy("valid", true, true); subject = subject(account, null); base.session("personal", account, null); base.session("organization", account, organization);
    }
    UUID subject(UUID owner, UUID organizationId) throws Exception {
        UUID id = UUID.randomUUID(); try (var connection = admin()) { execute(connection, "INSERT INTO subject_number.business_subject(id,owner_account_id,owner_organization_id,source) VALUES (?,?,?,'WEB')", id, owner, organizationId); } return id;
    }
    UUID policy(String shape, boolean enabled, boolean published) throws Exception {
        version++; UUID id = UUID.randomUUID(); String snapshot = Set.of("valid", "larger").contains(shape) ? "{\"policyCode\":\"FIRST-TRIAL\",\"promptVersion\":\"1\",\"contentSha256\":\"" + "a".repeat(64) + "\"}" : "{}";
        try (var connection = admin()) {
            execute(connection, "UPDATE entitlement_metering.trial_policy_version SET enabled=false,revision=revision+1 WHERE enabled");
            execute(connection, "INSERT INTO entitlement_metering.trial_policy_version(id,campaign_key,version_no,granted_seconds,valid_days,enabled,snapshot,snapshot_sha256,published_by_employee_id,published_at) VALUES (?,'FIRST-TRIAL',?,?,?,?,?::jsonb,?,?,?)", id, version, shape.equals("larger") ? 3600 : 600, shape.equals("larger") ? 30 : 1, enabled, snapshot, "b".repeat(64), employee, published ? OffsetDateTime.now(ZoneOffset.UTC) : null);
        }
        policy = id; return id;
    }
    ActivationCommand command(String event, Decision decision) { return command(subject, event, decision); }
    ActivationCommand command(UUID subjectId, String event, Decision decision) { return new ActivationCommand(subjectId, event, decision, "FIRST-TRIAL", Integer.toString(version), "1", "a".repeat(64), UUID.randomUUID()); }
    ActivationCaller caller() { return new ActivationCaller(account, Channel.WEB, WorkspaceType.PERSONAL, null); }
    ActivationResult accept(String event) { return service.activate(command(event, Decision.ACCEPT), caller()); }
    long count(String table) throws Exception { try (var connection = admin()) { return Long.parseLong(one(connection, "SELECT count(*) FROM " + table)); } }
    List<Long> counts() throws Exception {
        List<Long> result = new ArrayList<>(); for (String table : List.of("activation_consent.consent_record", "activation_consent.service_activation", "activation_consent.service_activation_transition", "entitlement_metering.trial_grant", "entitlement_metering.entitlement_grant", "entitlement_metering.entitlement_balance", "platform.outbox_event", "audit.audit_fact")) result.add(count(table)); return result;
    }
    void sql(String sql, Object... parameters) throws Exception { try (var connection = admin()) { execute(connection, sql, parameters); } }
    String scalar(String sql) throws Exception { try (var connection = admin()) { return one(connection, sql); } }
    HttpResponse<String> post(String token, UUID subjectId, String body) throws Exception {
        return post(token, subjectId, HttpRequest.BodyPublishers.ofString(body));
    }
    HttpResponse<String> post(String token, UUID subjectId, HttpRequest.BodyPublisher body) throws Exception {
        return base.client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/customer/subjects/" + subjectId + "/activations")).timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + token).header("Content-Type", "application/json").POST(body).build(), HttpResponse.BodyHandlers.ofString());
    }
    String body(String event, Decision decision) { return JsonMapper.builder().build().writeValueAsString(Map.of("activationEventId", event, "decision", decision.name(), "policyCode", "FIRST-TRIAL", "policyVersion", Integer.toString(version), "promptVersion", "1", "contentSha256", "a".repeat(64))); }
    SubjectActivationRepository decorated(String point) {
        return (SubjectActivationRepository) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {SubjectActivationRepository.class}, (proxy, method, arguments) -> {
            try { Object result = method.invoke(repository, arguments); if (method.getName().equals(point)) throw new IllegalStateException("FIXTURE_FAULT"); return result; }
            catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
        });
    }
    CustomerSubjectActivationService with(SubjectActivationRepository replacement, ActivationTrialService owner) { return new DefaultCustomerSubjectActivationService(transaction, dsl, replacement, owner); }
    CustomerSubjectActivationService commitFailure() {
        var manager = new DataSourceTransactionManager(pool) {
            @Override protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) { throw new org.springframework.transaction.TransactionSystemException("FIXTURE_COMMIT_FAULT"); }
        };
        manager.setRollbackOnCommitFailure(true); var template = new TransactionTemplate(manager); template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return new DefaultCustomerSubjectActivationService(template, dsl, repository, new DefaultActivationTrialService(manager, dsl));
    }
    void move(String destination) throws Exception {
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            UUID activation;
            int revision;
            String status;
            try (var statement = connection.prepareStatement("SELECT id,revision,status FROM activation_consent.service_activation WHERE subject_id=?")) {
                statement.setObject(1, subject); try (var row = statement.executeQuery()) { assertThat(row.next()).isTrue(); activation = row.getObject(1, UUID.class); revision = row.getInt(2); status = row.getString(3); }
            }
            int next = revision + 1;
            execute(connection, "UPDATE activation_consent.service_activation SET status=?,revision=?,updated_at=clock_timestamp() WHERE id=?", destination, next, activation);
            execute(connection, "INSERT INTO activation_consent.service_activation_transition(id,activation_id,subject_id,from_status,to_status,from_revision,to_revision,source_channel,source_action_domain,source_action_event_id,status_event_domain,status_event_id,request_fingerprint,actor_employee_id,reason_code) VALUES (?,?,?,?,?,?,?,'OPERATIONS','OPERATIONS',?,'ACTIVATION_STATUS',?,?,?,'FIXTURE_OPERATION')", UUID.randomUUID(), activation, subject, status, destination, revision, next, UUID.randomUUID().toString(), ActivationSemantics.statusEvent(activation, next), "d".repeat(64), employee);
            connection.commit();
        }
    }
    List<Object> concurrent(ActivationCommand first, ActivationCommand second) throws Exception {
        return concurrent(first, caller(), second, caller());
    }
    List<Object> concurrent(ActivationCommand first, ActivationCaller firstCaller, ActivationCommand second, ActivationCaller secondCaller) throws Exception {
        pool.setMaximumPoolSize(2);
        var executor = Executors.newFixedThreadPool(2);
        try (var blocker = admin()) {
            blocker.setAutoCommit(false);
            for (UUID locked : java.util.stream.Stream.of(first.subjectId(), second.subjectId()).distinct().sorted().toList()) execute(blocker, "SELECT pg_advisory_xact_lock(hashtextextended('subject_number.subject:'||?::text,0))", locked);
            CountDownLatch started = new CountDownLatch(2);
            java.util.function.BiFunction<ActivationCommand, ActivationCaller, Callable<Object>> task = (command, caller) -> () -> { started.countDown(); try { return service.activate(command, caller); } catch (RuntimeException failure) { return failure; } };
            Future<Object> left = executor.submit(task.apply(first, firstCaller)), right = executor.submit(task.apply(second, secondCaller)); assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15); boolean observed = false;
            while (System.nanoTime() < deadline) { if (Long.parseLong(scalar("SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.usename='" + login + "' AND l.locktype='advisory' AND NOT l.granted")) >= 2) { observed = true; break; } Thread.onSpinWait(); }
            assertThat(observed).as("two independent runtime backends blocked on real subject advisory").isTrue(); blocker.commit();
            return List.of(left.get(30, TimeUnit.SECONDS), right.get(30, TimeUnit.SECONDS));
        } finally { executor.shutdownNow(); assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue(); pool.setMaximumPoolSize(1); }
    }
    void revokeWhileWaiting(boolean membership) throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var blocker = admin()) {
            blocker.setAutoCommit(false);
            String key = membership ? "organization_access.membership:" + organization + ":" + account : "customer_iam.account:" + account;
            execute(blocker, "SELECT pg_advisory_xact_lock(hashtextextended(?,0))", key);
            ActivationCaller caller = membership ? new ActivationCaller(account, Channel.WEB, WorkspaceType.ORGANIZATION, organization) : caller();
            CountDownLatch started = new CountDownLatch(1);
            Future<Object> pending = executor.submit(() -> { started.countDown(); try { return service.activate(command("waiting", Decision.ACCEPT), caller); } catch (RuntimeException failure) { return failure; } });
            assertThat(started.await(10, TimeUnit.SECONDS)).isTrue(); long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15); boolean observed = false;
            while (System.nanoTime() < deadline) { if (Long.parseLong(scalar("SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.usename='" + login + "' AND l.locktype='advisory' AND NOT l.granted")) == 1) { observed = true; break; } Thread.onSpinWait(); }
            assertThat(observed).isTrue();
            if (membership) execute(blocker, "UPDATE organization_access.organization_membership SET status='REVOKED',revision=revision+1 WHERE organization_id=? AND account_id=? AND status='ACTIVE'", organization, account);
            else execute(blocker, "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", account);
            blocker.commit();
            assertThat(pending.get(30, TimeUnit.SECONDS)).isInstanceOfSatisfying(com.onecall.core.activation_consent.customeractivation.SubjectActivationException.class,
                    failure -> assertThat(failure.code()).isEqualTo(com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code.FORBIDDEN));
        } finally { executor.shutdownNow(); assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue(); }
    }
    @Override public void close() {
        try {
            if (context != null) {
                context.close();
                try (var connection = admin()) {
                    execute(connection, "TRUNCATE customer_iam.customer_account,organization_access.organization,subject_number.business_subject,operations_iam.employee_account,platform.outbox_event,audit.audit_fact CASCADE");
                    execute(connection, "DROP ROLE IF EXISTS " + login);
                } catch (SQLException failure) { throw new IllegalStateException("Activation fixture cleanup failed"); }
            }
        } finally { base.close(); }
    }
}
