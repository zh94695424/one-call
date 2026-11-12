package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;

import com.onecall.archtests.subjectnumber.ControlledSubjectClaimProviders;
import com.onecall.core.OneCallApplication;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.*;
import com.onecall.core.subject_number.internal.claim.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimProofVerifier.ApprovedProof;
import com.onecall.core.subject_number.internal.claim.SubjectClaimVerificationReference.Payload;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class CustomerSubjectClaimFixture implements AutoCloseable {
    final CustomerBusinessNumbersFixture base = new CustomerBusinessNumbersFixture();
    final String login = "pa30_subject_claim_" + UUID.randomUUID().toString().replace("-", "");
    final String password = UUID.randomUUID().toString();
    final UUID account = base.account, other = base.other;
    final HttpClient client = HttpClient.newHttpClient();
    final List<Integer> providerPids = new CopyOnWriteArrayList<>();
    ConfigurableApplicationContext context;
    ControlledSubjectClaimProviders providers;
    HikariDataSource pool;
    DSLContext dsl;
    TransactionTemplate transaction;
    int port;

    static final class ProviderHolder { final ControlledSubjectClaimProviders value = new ControlledSubjectClaimProviders(); }
    @TestConfiguration(proxyBeanMethods = false)
    static class ControlledBeans {
        @Bean ProviderHolder claimProviderHolder() { return new ProviderHolder(); }
        @Bean(name = {"subjectClaimProofVerifier", "subjectClaimReferenceCipher"})
        ControlledSubjectClaimProviders subjectClaimProofVerifier(ProviderHolder holder) { return holder.value; }
    }
    void start() throws Exception {
        base.start(null);
        try (var connection = admin()) {
            execute(connection, "CREATE ROLE " + login + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '" + password + "'");
            execute(connection, "GRANT subject_number_role TO " + login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
            execute(connection, "ALTER ROLE pa27_query PASSWORD '" + base.password + "'");
        }
        Map<String, Object> properties = new HashMap<>();
        properties.put("onecall.customer.persistence.url", base.url); properties.put("onecall.customer.persistence.username", "pa27_customer"); properties.put("onecall.customer.persistence.password", base.password);
        properties.put("onecall.customer.business-numbers.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]));
        properties.put("onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]));
        properties.put("onecall.subject-claim-command.enabled", "true"); properties.put("onecall.subject-claim-command.datasource.url", base.url);
        properties.put("onecall.subject-claim-command.datasource.username", login); properties.put("onecall.subject-claim-command.datasource.password", password);
        properties.put("onecall.persistence.url", base.url); properties.put("onecall.persistence.username", "pa27_query"); properties.put("onecall.persistence.password", base.password);
        properties.put("onecall.sharing-command.enabled", "false"); properties.put("onecall.sharing-admission.datasource.enabled", "false"); properties.put("onecall.evidence-media-read.datasource.enabled", "false");
        context = new SpringApplicationBuilder(OneCallApplication.class, CustomerBusinessNumbersFixture.SessionBeans.class, ControlledBeans.class)
                .profiles("web", "persistence").properties("server.port=0", "spring.main.banner-mode=off")
                .initializers(application -> application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa30-claim", properties))).run();
        port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        providers = context.getBean(ProviderHolder.class).value;
        pool = context.getBean("subjectClaimCommandDataSource", HikariDataSource.class); pool.setMaximumPoolSize(1);
        dsl = context.getBean("subjectClaimCommandDsl", DSLContext.class); transaction = context.getBean("subjectClaimCommandTransactionTemplate", TransactionTemplate.class);
        preflight();
        providers.probe = () -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(dsl.fetchOne("SELECT current_user").get(0, String.class)).isEqualTo("subject_number_role");
            providerPids.add(dsl.fetchOne("SELECT pg_backend_pid()").get(0, Integer.class));
        };
        reset();
    }
    void preflight() {
        assertThat(context.getEnvironment().getActiveProfiles()).contains("web", "persistence");
        assertThat(context.getBean("customerSubjectClaimService")).isInstanceOf(DefaultCustomerSubjectClaimService.class);
        assertThat(context.getBean("subjectClaimRepository")).isInstanceOf(JooqSubjectClaimRepository.class);
        assertThat(context.getBeansOfType(SubjectClaimProofVerifier.class)).hasSize(1); assertThat(context.getBeansOfType(SubjectClaimReferenceCipher.class)).hasSize(1);
        assertThat(context.getBean("subjectClaimProofVerifier")).isSameAs(providers); assertThat(context.getBean("subjectClaimReferenceCipher")).isSameAs(providers);
        assertThat(context.getBean("subjectClaimCommandTransactionManager", DataSourceTransactionManager.class).getDataSource()).isSameAs(pool);
        assertThat(context.getBean("customerQueryDataSource")).isNotSameAs(pool); assertThat(context.getBean("sharingQueryDataSource")).isNotSameAs(pool);
        assertThat(pool.getUsername().equals(login)).isTrue();
        transaction.execute(status -> {
            dsl.execute("SET LOCAL ROLE subject_number_role");
            assertThat(dsl.fetchOne("SELECT current_user").get(0, String.class)).isEqualTo("subject_number_role");
            assertThat(dsl.fetchOne("SELECT current_setting('transaction_isolation')").get(0, String.class)).isEqualTo("read committed");
            assertThat(dsl.fetchOne("SELECT current_setting('transaction_read_only')").get(0, String.class)).isEqualTo("off");
            assertThat(dsl.fetchOne("SELECT pg_backend_pid()").get(0, Integer.class)).isPositive(); return null;
        });
    }
    void reset() throws Exception {
        try (var connection = admin()) {
            execute(connection, "TRUNCATE subject_number.business_subject,customer_iam.customer_account,organization_access.organization,audit.audit_fact CASCADE");
            execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'Claimant'),(?,'Other')", account, other);
            execute(connection, "INSERT INTO organization_access.organization(id,display_name) VALUES (?,'Organization')", base.organizations.getFirst());
        }
        CustomerBusinessNumbersFixture.SESSIONS.clear(); base.session("personal", account, null); base.session("other", other, null);
        base.session("organization", account, base.organizations.getFirst()); base.session("missing", UUID.randomUUID(), null);
        providerPids.clear(); providers.unavailable = false;
    }
    record Seed(UUID subject, UUID binding, String hmac) {
        @Override public String toString() { return "Seed[REDACTED]"; }
    }
    Seed seed(UUID owner, UUID organization, String status, boolean verified) throws Exception {
        UUID subject = UUID.randomUUID(), binding = UUID.randomUUID(); String hmac = UUID.randomUUID().toString().replace("-", "").repeat(2);
        try (var connection = admin()) {
            execute(connection, "INSERT INTO subject_number.business_subject(id,owner_account_id,owner_organization_id,source) VALUES (?,?,?,'WEB')", subject, owner, organization);
            execute(connection, "INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,status,verified_at) VALUES (?,?,'MOBILE','v1.test.AAAA',?,'masked',?,?)",
                    binding, subject, hmac, status, verified ? OffsetDateTime.now(ZoneOffset.UTC) : null);
        }
        return new Seed(subject, binding, hmac);
    }
    Seed seed() throws Exception { return seed(null, null, "ACTIVE", true); }
    String approve(Seed seed, UUID claimant, VerificationMethod method, String reference) {
        String attempt = UUID.randomUUID().toString(); providers.approve(attempt, new ApprovedProof(claimant, seed.subject(), seed.binding(), method, reference)); return attempt;
    }
    String body(Seed seed, VerificationMethod method, String attempt) {
        return "{\"subjectId\":\"" + seed.subject() + "\",\"bindingId\":\"" + seed.binding() + "\",\"verificationMethod\":\"" + method.name() + "\",\"verificationProofId\":\"" + attempt + "\"}";
    }
    HttpResponse<String> post(String token, String key, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/customer/subject-claims"))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + token).header("Idempotency-Key", key)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode post(Seed seed, String key, int status) throws Exception {
        return checked(post("personal", key, body(seed, VerificationMethod.SMS, approve(seed, account, VerificationMethod.SMS, "server-reference"))), status);
    }
    JsonNode checked(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).isEqualTo(status); assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        for (String canary : List.of(password, base.password, "SECRET_CANARY", "numberHmac", "verificationProofId", "verification_ref_ciphertext")) assertThat(response.body().contains(canary)).isFalse();
        JsonNode root = JsonMapper.builder().build().readTree(response.body());
        if (status == 200) assertThat(root.propertyNames()).containsExactlyInAnyOrderElementsOf(root.path("status").asString().equals("ACCEPTED") ? List.of("claimId", "status", "subjectId", "verifiedAt") : List.of("claimId", "status", "subjectId"));
        else assertThat(root.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        return root;
    }
    void risk(Seed seed, String state) throws Exception {
        try (var connection = admin()) {
            connection.setAutoCommit(false);
            execute(connection, "SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))", "subject_number.number:" + seed.hmac());
            execute(connection, "INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source,cleared_at) VALUES (?,?,'RECYCLED',?,'CLAIM_CHECK',?) ON CONFLICT (caller_number_binding_id,risk_type) DO UPDATE SET state=excluded.state,cleared_at=excluded.cleared_at,revision=subject_number.subject_risk_state.revision+1",
                    UUID.randomUUID(), seed.binding(), state, state.equals("CLEARED") ? OffsetDateTime.now(ZoneOffset.UTC) : null);
            connection.commit();
        }
    }
    UUID history(Seed seed, UUID claimant, VerificationMethod method, String key, ClaimStatus status, Payload payload) throws Exception {
        Runnable probe = providers.probe; providers.probe = () -> { };
        String envelope; try { envelope = providers.sealPayload(payload); } finally { providers.probe = probe; }
        return historyEnvelope(seed, claimant, method, key, status, envelope);
    }
    UUID historyEnvelope(Seed seed, UUID claimant, VerificationMethod method, String key, ClaimStatus status, String envelope) throws Exception {
        UUID id = UUID.randomUUID();
        try (var connection = admin()) {
            execute(connection, "INSERT INTO subject_number.subject_claim(id,subject_id,account_id,verification_method,verification_ref_ciphertext,request_id,status,risk_checked_at,verified_at) VALUES (?,?,?,?,?,?,?,clock_timestamp(),?)",
                    id, seed.subject(), claimant, method.name(), envelope, key, status.name(), status == ClaimStatus.ACCEPTED ? OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(1) : null);
        }
        return id;
    }
    long count(String table) throws Exception { return Long.parseLong(value("SELECT count(*) FROM " + table)); }
    String value(String sql, Object... parameters) throws Exception {
        try (var connection = admin(); var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < parameters.length; index++) statement.setObject(index + 1, parameters[index]);
            try (var rows = statement.executeQuery()) { assertThat(rows.next()).isTrue(); return rows.getString(1); }
        }
    }
    String owner(Seed seed) throws Exception { return value("SELECT coalesce(owner_account_id::text,'null')||'|'||coalesce(owner_organization_id::text,'null')||'|'||revision FROM subject_number.business_subject WHERE id=?", seed.subject()); }
    void audit(String reason, String outcome, String risk) throws Exception {
        JsonNode snapshot = JsonMapper.builder().build().readTree(value("SELECT after_snapshot::text FROM audit.audit_fact ORDER BY occurred_at DESC,id DESC LIMIT 1"));
        assertThat(snapshot.propertyNames()).containsExactlyInAnyOrder("reason", "outcome", "riskEvaluation");
        assertThat(snapshot.path("reason").asString()).isEqualTo(reason); assertThat(snapshot.path("outcome").asString()).isEqualTo(outcome); assertThat(snapshot.path("riskEvaluation").asString()).isEqualTo(risk);
        assertThat(value("SELECT reason_code FROM audit.audit_fact ORDER BY occurred_at DESC,id DESC LIMIT 1")).isEqualTo(reason);
    }
    void cleanPool() throws Exception {
        try (var connection = pool.getConnection()) {
            assertThat(CustomerBusinessNumbersFixture.one(connection, "SELECT current_user=session_user")).isEqualTo("t");
            assertThat(CustomerBusinessNumbersFixture.one(connection, "SELECT current_user").equals(login)).isTrue();
            assertThat(connection.getAutoCommit()).isTrue(); assertThat(connection.isReadOnly()).isFalse();
            assertThat(CustomerBusinessNumbersFixture.one(connection, "SELECT current_setting('transaction_isolation')")).isEqualTo("read committed");
            assertThat(CustomerBusinessNumbersFixture.one(connection, "SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid() AND locktype='advisory'")).isEqualTo("0");
        }
        assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
    }
    Connection admin() throws SQLException { return base.admin(); }
    Connection runtime() throws SQLException { return DriverManager.getConnection(base.url, login, password); }
    Flyway flyway() { return Flyway.configure().dataSource(base.url, "fixture_admin", base.password).locations("classpath:db/migration").load(); }
    @Override public void close() { if (context != null) context.close(); client.close(); base.close(); }
}
