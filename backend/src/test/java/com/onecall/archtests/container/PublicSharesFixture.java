package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import com.onecall.archtests.sharing.DeterministicShareCommandProviders;
import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.sharing.internal.application.ShareSummary.VerificationMode;
import com.onecall.core.sharing.internal.command.*;
import com.onecall.core.sharing.internal.persistence.*;
import com.onecall.core.sharing.internal.provider.*;
import com.onecall.core.sharing.internal.publicquery.*;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class PublicSharesFixture implements AutoCloseable {
    final SharingQueryFixture base = new SharingQueryFixture();
    final JsonMapper mapper = JsonMapper.builder().build();
    final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    final List<Execution> executions = new CopyOnWriteArrayList<>();
    final AtomicInteger verifications = new AtomicInteger();
    final AtomicInteger digests = new AtomicInteger();
    final AtomicInteger policies = new AtomicInteger();
    final AtomicInteger tokenIssuances = new AtomicInteger();
    final AtomicInteger extractionCodes = new AtomicInteger();
    final AtomicInteger designatedMobiles = new AtomicInteger();
    final AtomicInteger rotationPolicies = new AtomicInteger();
    final Map<String, VerifiedCustomerSession> sessions = new HashMap<>();
    ConfigurableApplicationContext context;
    DSLContext dsl;
    HikariDataSource pool;
    CustomerShareCommandService commands;
    UUID subject;
    UUID binding;
    String commandLogin;
    String customerLogin;
    String writeState;
    volatile String failOn;
    volatile boolean tryWrite;
    volatile boolean failRestore;
    final Map<VerificationMode, CustomerShareCommandResult.Created> created = new EnumMap<>(VerificationMode.class);

    record Execution(String sql, String role, String readOnly, int pid, boolean prepared, int bindings) { }

    void start() throws Exception {
        base.start();
        try (var admin = base.admin(); var rows = admin.createStatement().executeQuery("SELECT current_setting('server_version'), current_setting('server_version_num')::int")) {
            rows.next();
            assertThat(rows.getInt(2)).isBetween(160000, 169999);
            System.out.println("PA15 PostgreSQL version=" + rows.getString(1));
        }
    }

    void reset() throws Exception {
        if (context != null) context.close();
        context = null;
        base.reset();
        commandLogin = "public_command_" + UUID.randomUUID().toString().replace("-", "");
        customerLogin = "public_customer_" + UUID.randomUUID().toString().replace("-", "");
        binding = UUID.randomUUID();
        try (var admin = base.admin()) {
            admin.setAutoCommit(false);
            SharingQueryFixture.execute(admin, "CREATE ROLE " + commandLogin + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + base.password + "'");
            SharingQueryFixture.execute(admin, "GRANT sharing_command_role, evidence_role TO " + commandLogin);
            SharingQueryFixture.execute(admin, "CREATE ROLE " + customerLogin + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + base.password + "'");
            SharingQueryFixture.execute(admin, "GRANT customer_iam_role TO " + customerLogin);
            var evidence = DSL.using(admin).fetchOne("SELECT subject_id, source_ref_id, call_id FROM evidence.evidence_record WHERE id = ?", base.evidence);
            subject = evidence.get("subject_id", UUID.class);
            SharingQueryFixture.execute(admin, "UPDATE subject_number.business_subject SET owner_organization_id = ?, revision = revision + 1 WHERE id = ?", base.org, subject);
            SharingQueryFixture.execute(admin, "INSERT INTO subject_number.caller_number_binding(id, subject_id, number_type, number_ciphertext, number_hmac, number_masked, verified_at) VALUES (?, ?, 'MOBILE', 'v1.synthetic.payload', ?, 'synthetic', clock_timestamp())",
                    binding, subject, DeterministicShareCommandProviders.digest(binding.toString()));
            UUID media = UUID.randomUUID();
            SharingQueryFixture.execute(admin, "INSERT INTO evidence.media_object(id, storage_provider, bucket, object_key, object_version_id, sha256, size_bytes, mime_type, crypto_key_ref) VALUES (?, 'COS', 'fixture', ?, 'v1', repeat('a',64), 20, 'audio/wav', 'test-key')", media, "PRIVATE_OBJECT_CANARY_" + media);
            SharingQueryFixture.execute(admin, "INSERT INTO evidence.evidence_artifact(id, evidence_id, media_object_id, artifact_role) VALUES (?, ?, ?, 'ORIGINAL')", UUID.randomUUID(), base.evidence, media);
            SharingQueryFixture.execute(admin, "INSERT INTO evidence.recording(id, call_id, recording_id, media_object_id, declared_format, declared_size, declared_duration, declared_sha256, validated_duration, validated_sha256, status, upload_expires_at, validated_at) VALUES (?, ?, ?, ?, 'wav', 20, 12, repeat('a',64), 12, repeat('a',64), 'VALIDATED', clock_timestamp() + interval '1 hour', clock_timestamp())",
                    evidence.get("source_ref_id", UUID.class), evidence.get("call_id", String.class), "recording-" + UUID.randomUUID(), media);
            admin.commit();
        }
        sessions.clear();
        sessions.put("member", new VerifiedCustomerSession(base.account, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1"));
        sessions.put("personal", new VerifiedCustomerSession(base.account, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1",
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
        sessions.put("different", new VerifiedCustomerSession(base.account, TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1",
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, base.org2)));
        context = new SpringApplicationBuilder(OneCallApplication.class).profiles("web", "persistence")
                .properties("server.port=0", "spring.main.banner-mode=off").initializers(application -> {
                    application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("public-fixture", Map.ofEntries(
                            Map.entry("onecall.customer.persistence.url", base.url), Map.entry("onecall.customer.persistence.username", customerLogin),
                            Map.entry("onecall.customer.persistence.password", base.password), Map.entry("onecall.persistence.url", base.url),
                            Map.entry("onecall.persistence.username", base.login), Map.entry("onecall.persistence.password", base.password),
                            Map.entry("onecall.sharing-command.enabled", "true"), Map.entry("onecall.sharing-command.datasource.url", base.url),
                            Map.entry("onecall.sharing-command.datasource.username", commandLogin), Map.entry("onecall.sharing-command.datasource.password", base.password))));
                    var generic = (GenericApplicationContext) application;
                    generic.registerBean("customerSessionVerifier", CustomerSessionVerifier.class, () -> (bearer, audience) -> {
                        verifications.incrementAndGet();
                        if (bearer.value().equals("outage")) throw new IllegalStateException("VERIFIER_CANARY");
                        var session = sessions.get(bearer.value());
                        if (session == null) throw new CustomerSessionVerificationException(CustomerSessionVerificationException.Code.UNAUTHENTICATED);
                        return session;
                    });
                    generic.registerBean("shareCreationPolicyProvider", ShareCreationPolicyProvider.class, () -> request -> {
                        policies.incrementAndGet(); return DeterministicShareCommandProviders.policy().evaluate(request);
                    });
                    generic.registerBean("shareRotationPolicyProvider", ShareRotationPolicyProvider.class, () -> request -> {
                        rotationPolicies.incrementAndGet(); return DeterministicShareCommandProviders.rotationPolicy().evaluate(request);
                    });
                    generic.registerBean("capabilityTokenProvider", CapabilityTokenProvider.class, () -> grantId -> {
                        tokenIssuances.incrementAndGet(); return DeterministicShareCommandProviders.tokens().issue(grantId);
                    });
                    generic.registerBean("shareExtractionCodeProvider", ShareExtractionCodeProvider.class, () -> (grantId, generation) -> {
                        extractionCodes.incrementAndGet(); return DeterministicShareCommandProviders.codes().generate(grantId, generation);
                    });
                    generic.registerBean("designatedMobileBindingProvider", DesignatedMobileBindingProvider.class, () -> mobile -> {
                        designatedMobiles.incrementAndGet(); return DeterministicShareCommandProviders.mobiles().bind(mobile);
                    });
                    generic.registerBean("sha256Provider", Sha256Provider.class, () -> bytes -> {
                        digests.incrementAndGet(); return DeterministicShareCommandProviders.sha256().digest(bytes);
                    });
                }).run();
        pool = context.getBean("sharingCommandDataSource", HikariDataSource.class);
        pool.setMaximumPoolSize(1);
        pool.setMinimumIdle(1);
        pool.setConnectionTimeout(5000);
        dsl = context.getBean("sharingCommandDsl", DSLContext.class);
        dsl.configuration().set(new DefaultExecuteListenerProvider(new DefaultExecuteListener() {
            @Override
            public void executeStart(ExecuteContext execution) {
                if (failRestore && execution.sql().equals("SET LOCAL ROLE sharing_command_role")
                        && executions.stream().anyMatch(observed -> observed.role().equals("evidence_role"))) throw new IllegalStateException("RESTORE_CANARY");
                if (failOn != null && execution.sql().contains(failOn)) throw new IllegalStateException("DEPENDENCY_CANARY");
                if (tryWrite && execution.sql().contains("WHERE capability_token_hash = ?")) {
                    try {
                        Savepoint savepoint = execution.connection().setSavepoint();
                        try (var statement = execution.connection().createStatement()) {
                            statement.executeUpdate("UPDATE sharing.evidence_share_grant SET admissions_used = admissions_used WHERE false");
                            fail("Read-only probe unexpectedly wrote");
                        } catch (SQLException denied) {
                            writeState = denied.getSQLState();
                            execution.connection().rollback(savepoint);
                        } finally { execution.connection().releaseSavepoint(savepoint); }
                    } catch (SQLException failure) { throw new IllegalStateException("Probe failed"); }
                }
            }
            @Override
            public void executeEnd(ExecuteContext execution) {
                try (var statement = execution.connection().createStatement(); var rows = statement.executeQuery(
                        "SELECT current_user, current_setting('transaction_read_only'), pg_backend_pid()")) {
                    rows.next();
                    executions.add(new Execution(execution.sql(), rows.getString(1), rows.getString(2), rows.getInt(3),
                            execution.statement() instanceof PreparedStatement, execution.query() == null ? 0 : execution.query().getBindValues().size()));
                } catch (SQLException failure) { throw new IllegalStateException("Observation failed"); }
            }
        }));
        commands = context.getBean(CustomerShareCommandService.class);
        created.clear();
        for (VerificationMode mode : VerificationMode.values()) created.put(mode, create(mode, "seed-" + mode));
        clearObservations();
    }

    CustomerShareCommandResult.Created create(VerificationMode mode, String key) {
        return (CustomerShareCommandResult.Created) commands.execute(command(mode, key), caller());
    }
    CustomerShareCommand.Create command(VerificationMode mode, String key) {
        return new CustomerShareCommand.Create(key, base.evidence, mode, Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
                false, Optional.empty(), 1, mode == VerificationMode.DESIGNATED_MOBILE ? "13800138000" : null);
    }
    ShareCommandCaller caller() { return new ShareCommandCaller(base.account, Optional.of(base.org)); }
    void clearObservations() {
        executions.clear(); verifications.set(0); digests.set(0); policies.set(0);
        tokenIssuances.set(0); extractionCodes.set(0); designatedMobiles.set(0); rotationPolicies.set(0);
        failOn = null; tryWrite = false; failRestore = false; writeState = null;
    }
    HttpResponse<String> get(String token, String session, String... headers) throws Exception {
        return request("GET", "/api/v1/public/shares/" + token, session, headers);
    }
    HttpResponse<String> request(String method, String path, String session, String... headers) throws Exception {
        int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(20))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (session != null) request.header("Authorization", "Bearer " + session);
        for (int index = 0; index < headers.length; index += 2) request.header(headers[index], headers[index + 1]);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode json(HttpResponse<String> response) { return mapper.readTree(response.body()); }
    void sql(String sql, Object... bindings) throws Exception { try (var admin = base.admin()) { SharingQueryFixture.execute(admin, sql, bindings); } }
    Map<String, String> snapshot() throws Exception {
        var snapshot = new TreeMap<String, String>();
        try (var admin = base.admin()) {
            for (String table : List.of("sharing.evidence_share_grant", "sharing.share_admission", "sharing.share_access_event",
                    "evidence.evidence_record", "evidence.evidence_artifact", "evidence.media_object", "evidence.recording", "evidence.evidence_event",
                    "audit.audit_fact", "platform.outbox_event", "platform.idempotency_record")) {
                var row = DSL.using(admin).fetchOne("SELECT coalesce(string_agg(to_jsonb(facts)::text, chr(10) ORDER BY to_jsonb(facts)::text), '') FROM " + table + " facts");
                snapshot.put(table, DeterministicShareCommandProviders.digest(row.get(0, String.class)));
            }
        }
        return snapshot;
    }
    @Override
    public void close() {
        if (context != null) context.close();
        client.close();
        base.close();
    }
}
