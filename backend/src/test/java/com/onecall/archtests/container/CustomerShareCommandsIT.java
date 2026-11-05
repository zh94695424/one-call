package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.archtests.sharing.DeterministicShareCommandProviders;
import com.onecall.core.evidence.internal.shareprojection.ShareProjectionReaderConfiguration;
import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import com.onecall.core.sharing.internal.application.ShareSummary.VerificationMode;
import com.onecall.core.sharing.internal.command.*;
import com.onecall.core.sharing.internal.commandservice.DefaultCustomerShareCommandService;
import com.onecall.core.sharing.internal.persistence.*;
import com.onecall.core.sharing.internal.persistence.SharingCommandRepository.ShareGrant;
import com.onecall.core.sharing.internal.provider.*;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class CustomerShareCommandsIT {
    private static final SharingQueryFixture base = new SharingQueryFixture();
    private AnnotationConfigApplicationContext context;
    private DSLContext dsl;
    private SharingCommandRepository repository;
    private CustomerShareCommandService service;
    private UUID subject;
    private UUID binding;
    private UUID organizationEvidence;
    private String login;
    private Instant expiry;
    private final List<Execution> executions = new CopyOnWriteArrayList<>();
    private final AtomicInteger tokens = new AtomicInteger();
    private final AtomicInteger codes = new AtomicInteger();
    private volatile ProviderResult<ShareCreationPolicyProvider.CreationPolicyDecision> policy = new ProviderResult.Allowed<>(ShareCreationPolicyProvider.CreationPolicyDecision.ALLOW);
    private volatile String failOn;

    record Execution(String sql, String role, int pid, List<Object> bindings) { }

    @BeforeAll
    static void startDatabase() throws Exception { base.start(); }

    @AfterAll
    static void stopDatabase() { base.close(); }

    @BeforeEach
    void provisionCommandProbe() throws Exception {
        base.reset();
        login = "command_probe_" + UUID.randomUUID().toString().replace('-', '_');
        binding = UUID.randomUUID();
        try (Connection admin = base.admin()) {
            admin.setAutoCommit(false);
            SharingQueryFixture.execute(admin, "CREATE ROLE " + login + " LOGIN NOINHERIT NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + base.password + "'");
            SharingQueryFixture.execute(admin, "GRANT sharing_command_role, evidence_role TO " + login);
            try (var statement = admin.prepareStatement("SELECT subject_id FROM evidence.evidence_record WHERE id = ?")) {
                statement.setObject(1, base.evidence);
                try (ResultSet rows = statement.executeQuery()) { rows.next(); subject = rows.getObject(1, UUID.class); }
            }
            SharingQueryFixture.execute(admin, "UPDATE subject_number.business_subject SET owner_account_id = ?, revision = revision + 1 WHERE id = ?", base.account, subject);
            SharingQueryFixture.execute(admin, "INSERT INTO subject_number.caller_number_binding(id, subject_id, number_type, number_ciphertext, number_hmac, number_masked, verified_at) "
                    + "VALUES (?, ?, 'MOBILE', 'v1.test.payload', ?, 'synthetic', clock_timestamp())", binding, subject, DeterministicShareCommandProviders.digest(binding.toString()));
            UUID media = UUID.randomUUID();
            SharingQueryFixture.execute(admin, "INSERT INTO evidence.media_object(id, storage_provider, bucket, object_key, object_version_id, sha256, size_bytes, mime_type, crypto_key_ref) "
                    + "VALUES (?, 'COS', 'fixture', ?, 'v1', ?, 20, 'audio/wav', 'test-key')", media, "private-" + media, "a".repeat(64));
            SharingQueryFixture.execute(admin, "INSERT INTO evidence.evidence_artifact(id, evidence_id, media_object_id, artifact_role) VALUES (?, ?, ?, 'ORIGINAL')", UUID.randomUUID(), base.evidence, media);
            UUID organizationSubject = UUID.randomUUID();
            UUID organizationBinding = UUID.randomUUID();
            organizationEvidence = UUID.randomUUID();
            SharingQueryFixture.execute(admin, "INSERT INTO subject_number.business_subject(id, owner_organization_id, source) VALUES (?, ?, 'WEB')", organizationSubject, base.org);
            seedBinding(admin, organizationSubject, organizationBinding);
            seedEvidence(admin, organizationEvidence, organizationSubject, null);
            admin.commit();
        }
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("persistence");
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("command-test", java.util.Map.of(
                "onecall.sharing-command.enabled", "true", "onecall.sharing-command.datasource.url", base.url,
                "onecall.sharing-command.datasource.username", login, "onecall.sharing-command.datasource.password", base.password)));
        context.registerBean("shareCreationPolicyProvider", ShareCreationPolicyProvider.class, () -> request -> policy);
        context.registerBean("capabilityTokenProvider", CapabilityTokenProvider.class, () -> grantId -> {
            tokens.incrementAndGet(); return DeterministicShareCommandProviders.tokens().issue(grantId);
        });
        context.registerBean("shareExtractionCodeProvider", ShareExtractionCodeProvider.class, () -> (grantId, generation) -> {
            codes.incrementAndGet(); return DeterministicShareCommandProviders.codes().generate(grantId, generation);
        });
        context.registerBean("designatedMobileBindingProvider", DesignatedMobileBindingProvider.class, DeterministicShareCommandProviders::mobiles);
        context.register(SharingCommandPersistenceConfiguration.class, ShareProjectionReaderConfiguration.class);
        context.refresh();
        context.getBean("sharingCommandDataSource", HikariDataSource.class).setMaximumPoolSize(4);
        dsl = context.getBean("sharingCommandDsl", DSLContext.class);
        dsl.configuration().set(new DefaultExecuteListenerProvider(new DefaultExecuteListener() {
            @Override
            public void executeStart(ExecuteContext execution) {
                try (Statement statement = execution.connection().createStatement(); ResultSet rows = statement.executeQuery("SELECT current_user, pg_backend_pid()")) {
                    rows.next(); executions.add(new Execution(execution.sql(), rows.getString(1), rows.getInt(2),
                            execution.query() == null ? List.of() : new java.util.ArrayList<>(execution.query().getBindValues())));
                } catch (java.sql.SQLException failure) { throw new IllegalStateException("Probe failed", failure); }
                if (failOn != null && execution.sql().contains(failOn)) throw new IllegalStateException("TEST_INTERNAL_CANARY");
            }
        }));
        repository = context.getBean(SharingCommandRepository.class);
        service = context.getBean(CustomerShareCommandService.class);
        expiry = Instant.now().plusSeconds(86400).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusNanos(123456000);
    }

    @AfterEach
    void closeCommandContext() { if (context != null) context.close(); }

    @Test
    void commandRoleIsAssertedBeforeFirstCommandRead() throws Exception {
        create("first", VerificationMode.NONE, Optional.empty());
        assertThat(executions.getFirst().sql()).isEqualTo("SET LOCAL ROLE sharing_command_role");
        assertThat(executions.get(1).sql()).isEqualTo("SELECT current_user");
        assertThat(executions.get(1).role()).isEqualTo("sharing_command_role");
        assertThat(executions.stream().filter(execution -> execution.sql().contains("customer_iam.customer_account")))
                .allMatch(execution -> execution.role().equals("sharing_command_role"));
        assertThat(context.getBean("sharingCommandDataSource")).isNotSameAs(base.pool);
        try (Connection connection = context.getBean("sharingCommandDataSource", HikariDataSource.class).getConnection(); Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT current_user, rolinherit FROM pg_roles WHERE rolname = current_user")) {
            rows.next(); assertThat(rows.getString(1)).isEqualTo(login); assertThat(rows.getBoolean(2)).isFalse();
        }
    }

    @Test
    void queryRoleCannotAssumeCommandRole() {
        var template = base.context.getBean(TransactionTemplate.class);
        DSLContext query = base.context.getBean(DSLContext.class);
        fails(() -> template.execute(status -> { repository.assertCommandRole(query); return null; }), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        assertThat(base.pool).isNotSameAs(context.getBean("sharingCommandDataSource"));
        assertThat(template.isReadOnly()).isTrue();
    }

    @Test
    void createPersistsGrantAuditAndOutboxAtomicallyWithoutAccessEvent() throws Exception {
        for (VerificationMode mode : VerificationMode.values()) {
            UUID trace = UUID.randomUUID();
            var created = (CustomerShareCommandResult.Created) create(mode.name(), mode, Optional.of(trace), true, 3, false);
            String shareId = created.state().shareId();
            try (Connection admin = base.admin()) {
                DSLContext facts = DSL.using(admin, org.jooq.SQLDialect.POSTGRES);
                var grant = facts.fetchOne("SELECT id, evidence_id, owner_organization_id, capability_token_hash, status, admissions_used, revision, revocation_generation, verification_mode, max_admissions, download_allowed, code_salt, code_hash, code_generation, valid_from, expires_at, content_snapshot::text AS snapshot FROM sharing.evidence_share_grant WHERE share_id = ?", shareId);
                UUID id = grant.get("id", UUID.class);
                assertThat(created.state().status()).isEqualTo(com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.ACTIVE);
                assertThat(created.state().evidenceId()).isEqualTo(mode == VerificationMode.ORGANIZATION_MEMBER ? organizationEvidence : base.evidence);
                assertThat(created.state().verificationMode()).isEqualTo(mode);
                assertThat(created.state().maxAdmissions()).isEqualTo(3);
                assertThat(created.state().downloadAllowed()).isTrue();
                assertThat(created.state().admissionsUsed()).isZero();
                assertThat(grant.get("evidence_id", UUID.class)).isEqualTo(created.state().evidenceId());
                assertThat(grant.get("owner_organization_id", UUID.class)).isEqualTo(mode == VerificationMode.ORGANIZATION_MEMBER ? base.org : null);
                assertThat(grant.get("status", String.class)).isEqualTo("ACTIVE");
                assertThat(grant.get("admissions_used", Integer.class)).isZero();
                assertThat(grant.get("revision", Integer.class)).isEqualTo(1);
                assertThat(grant.get("revocation_generation", Integer.class)).isZero();
                assertThat(grant.get("verification_mode", String.class)).isEqualTo(mode.name());
                assertThat(grant.get("max_admissions", Integer.class)).isEqualTo(3);
                assertThat(grant.get("download_allowed", Boolean.class)).isTrue();
                assertThat(grant.get("capability_token_hash", String.class)).isEqualTo(DeterministicShareCommandProviders.digest(created.capabilityToken()));
                var storedSnapshot = JsonMapper.builder().build().readTree(grant.get("snapshot", String.class));
                assertThat(storedSnapshot.path("creation").path("maxAdmissions").asInt()).isEqualTo(3);
                assertThat(storedSnapshot.path("creation").path("downloadAllowed").asBoolean()).isTrue();
                assertThat(storedSnapshot.path("creation").path("creationFingerprint").asString()).isNotBlank();
                assertThat(grant.get("expires_at", java.time.OffsetDateTime.class).toInstant()).isEqualTo(expiry);
                assertThat(grant.get("code_generation", Integer.class)).isEqualTo(mode == VerificationMode.EXTRACTION_CODE ? 1 : 0);
                assertThat(grant.get("code_salt", String.class) != null).isEqualTo(mode == VerificationMode.EXTRACTION_CODE);
                var outbox = facts.fetchOne("SELECT event_domain, dedupe_key, aggregate_type, aggregate_id, event_type, payload_redacted::text AS payload, payload_sha256, payload_ciphertext FROM platform.outbox_event WHERE aggregate_id = ?", id);
                assertThat(outbox.get("event_domain")).isEqualTo("SHARE_ACCESS");
                assertThat(outbox.get("aggregate_type")).isEqualTo("EVIDENCE_SHARE_GRANT");
                assertThat(outbox.get("event_type")).isEqualTo("SHARE_CREATED");
                assertThat(outbox.get("dedupe_key")).isEqualTo("share-created:" + id);
                assertThat(outbox.get("payload_ciphertext")).isNull();
                var payload = JsonMapper.builder().build().readTree(outbox.get("payload", String.class));
                assertThat(payload.properties().stream().map(java.util.Map.Entry::getKey)).containsExactlyInAnyOrder(
                        "shareId", "grantId", "status", "admissionsUsed", "codeGeneration", "revocationGeneration", "revision", "creationFingerprint", "occurredAt");
                assertThat(outbox.get("payload_sha256")).isEqualTo(DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(payload)));
                assertThat(payload.path("occurredAt").asString()).isEqualTo(DefaultCustomerShareCommandService.utc(created.state().validFrom()));
                assertThat(payload.path("shareId").asString()).isEqualTo(shareId);
                assertThat(payload.path("grantId").asString()).isEqualTo(id.toString());
                assertThat(payload.path("status").asString()).isEqualTo("ACTIVE");
                assertThat(payload.path("admissionsUsed").asInt()).isZero();
                assertThat(payload.path("revision").asInt()).isEqualTo(1);
                assertThat(payload.path("creationFingerprint").asString()).isEqualTo(storedSnapshot.path("creation").path("creationFingerprint").asString());
                assertThat(payload.path("revocationGeneration").asInt()).isZero();
                var audit = facts.fetchOne("SELECT actor_type, actor_id, action_code, reason_code, object_type, object_id, before_snapshot, after_snapshot::text AS after, request_id, trace_id, occurred_at FROM audit.audit_fact WHERE object_id = ?", id);
                assertThat(audit.get("actor_type")).isEqualTo("ACCOUNT");
                assertThat(audit.get("actor_id", UUID.class)).isEqualTo(base.account);
                assertThat(audit.get("action_code")).isEqualTo("SHARE_GRANT_CREATE");
                assertThat(audit.get("reason_code", String.class)).isNull();
                assertThat(audit.get("object_type")).isEqualTo("EVIDENCE_SHARE_GRANT");
                assertThat(audit.get("before_snapshot")).isNull();
                var after = JsonMapper.builder().build().readTree(audit.get("after", String.class));
                assertThat(after.path("status").asString()).isEqualTo("ACTIVE");
                assertThat(after.path("revision").asInt()).isEqualTo(1);
                assertThat(audit.get("trace_id")).isEqualTo(trace.toString());
                assertThat(audit.get("request_id")).isEqualTo(DeterministicShareCommandProviders.digest(mode.name()));
                assertThat(audit.get("occurred_at", java.time.OffsetDateTime.class).toInstant()).isEqualTo(created.state().validFrom());
                assertThat(count(facts, "sharing.share_access_event", "share_grant_id", id)).isZero();
                assertThat(grant.get("snapshot", String.class)).doesNotContain("test-token", "test-code", "13800138000", "private-");
            }
        }
        expiry = expiry.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        var organizationWorkspace = (CustomerShareCommandResult.Created) create("organization-workspace", VerificationMode.NONE, Optional.empty(), true, 2, true);
        try (Connection admin = base.admin()) {
            var owner = DSL.using(admin).fetchOne("SELECT owner_organization_id FROM sharing.evidence_share_grant WHERE share_id = ?", organizationWorkspace.state().shareId());
            assertThat(owner.get("owner_organization_id", UUID.class)).isEqualTo(base.org);
        }
        var direct = (CustomerShareCommandResult.Created) create("null-trace", VerificationMode.NONE, Optional.empty());
        try (Connection admin = base.admin()) {
            assertThat(DSL.using(admin).fetchOne("SELECT a.trace_id FROM audit.audit_fact a JOIN sharing.evidence_share_grant g ON g.id = a.object_id WHERE g.share_id = ?", direct.state().shareId()).get(0)).isNull();
            assertThat(DSL.using(admin).fetchOne("SELECT content_snapshot->'creation'->>'expiresAt' FROM sharing.evidence_share_grant WHERE share_id = ?", direct.state().shareId()).get(0, String.class))
                    .isEqualTo(expiry.toString());
        }
        var writes = executions.stream().map(Execution::sql).filter(sql -> sql.startsWith("INSERT INTO sharing.evidence_share_grant")
                || sql.contains("platform.outbox_append") || sql.contains("audit.append_fact")).toList();
        for (int index = 0; index < writes.size(); index += 3) {
            assertThat(writes.get(index)).startsWith("INSERT INTO sharing.evidence_share_grant");
            assertThat(writes.get(index + 1)).contains("platform.outbox_append");
            assertThat(writes.get(index + 2)).contains("audit.append_fact");
        }
    }

    @Test
    void createReplayReturnsMetadataAndDoesNotPersistOrReturnSecrets() {
        for (VerificationMode mode : VerificationMode.values()) {
            var first = (CustomerShareCommandResult.Created) create(mode.name(), mode, Optional.empty(), true, 3, false);
            int tokenCount = tokens.get();
            int codeCount = codes.get();
            executions.clear();
            var replay = (CustomerShareCommandResult.CreationReplayed) create(mode.name(), mode, Optional.of(UUID.randomUUID()), true, 3, false);
            assertThat(replay.state()).isEqualTo(first.state());
            assertThat(replay.state().maxAdmissions()).isEqualTo(3);
            assertThat(replay.state().downloadAllowed()).isTrue();
            assertThat(tokens).hasValue(tokenCount);
            assertThat(codes).hasValue(codeCount);
            assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT") || execution.sql().contains("outbox_append") || execution.sql().contains("append_fact"));
            assertThatThrownBy(() -> create(mode.name(), mode, Optional.empty(), false, 3, false))
                    .isInstanceOfSatisfying(ShareCommandException.class, exception -> assertThat(exception.code()).isEqualTo(ShareCommandException.Code.SHARE_CREATION_CONFLICT));
            assertThatThrownBy(() -> create(mode.name(), mode, Optional.empty(), true, 4, false))
                    .isInstanceOfSatisfying(ShareCommandException.class, exception -> assertThat(exception.code()).isEqualTo(ShareCommandException.Code.SHARE_CREATION_CONFLICT));
        }
    }

    @Test
    void createRiskAndPolicyChecksPrecedeReplay() throws Exception {
        UUID ambiguousBinding = UUID.randomUUID();
        try (Connection admin = base.admin()) { seedBinding(admin, subject, ambiguousBinding); }
        executions.clear();
        fails(() -> create("ambiguous-binding", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.SHARE_RISK_BLOCKED);
        assertNoFacts();
        assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT")
                || execution.sql().contains("outbox_append") || execution.sql().contains("append_fact") || execution.sql().contains("WHERE share_id"));
        try (Connection admin = base.admin()) {
            SharingQueryFixture.execute(admin, "UPDATE subject_number.caller_number_binding SET status = 'RETIRED', revision = revision + 1 WHERE id = ?", ambiguousBinding);
        }
        UUID blockedCapturedEvidence = UUID.randomUUID();
        try (Connection admin = base.admin()) {
            admin.setAutoCommit(false);
            seedEvidence(admin, blockedCapturedEvidence, subject, ambiguousBinding.toString());
            SharingQueryFixture.execute(admin, "INSERT INTO subject_number.subject_risk_state(id, caller_number_binding_id, risk_type, state, source) VALUES (?, ?, 'CONFLICT', 'OPEN', 'OPERATIONS')",
                    UUID.randomUUID(), ambiguousBinding);
            admin.commit();
        }
        executions.clear();
        fails(() -> executeEvidence("captured-open-risk", blockedCapturedEvidence), ShareCommandException.Code.SHARE_RISK_BLOCKED);
        assertNoFacts();
        assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT")
                || execution.sql().contains("outbox_append") || execution.sql().contains("append_fact") || execution.sql().contains("WHERE share_id"));
        assertThat(executions.stream().filter(execution -> execution.sql().contains("subject_number.subject_risk_state")))
                .isNotEmpty().allMatch(execution -> execution.role().equals("sharing_command_role"));
        create("replay", VerificationMode.NONE, Optional.empty());
        executions.clear();
        policy = new ProviderResult.Denied<>(ShareCommandException.Code.FORBIDDEN);
        fails(() -> create("replay", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.SHARE_CREATION_CONFLICT);
        policy = new ProviderResult.Unavailable<>(ShareCommandException.Code.SERVICE_UNAVAILABLE);
        fails(() -> create("replay", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.SHARE_ACCESS_POLICY_UNAVAILABLE);
        policy = new ProviderResult.Allowed<>(ShareCreationPolicyProvider.CreationPolicyDecision.ALLOW);
        try (Connection admin = base.admin()) {
            SharingQueryFixture.execute(admin, "INSERT INTO subject_number.subject_risk_state(id, caller_number_binding_id, risk_type, state, source) VALUES (?, ?, 'CONFLICT', 'OPEN', 'OPERATIONS')", UUID.randomUUID(), binding);
        }
        fails(() -> create("replay", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.SHARE_RISK_BLOCKED);
        assertThat(executions).noneMatch(execution -> execution.sql().contains("WHERE share_id"));
        var missing = new CustomerShareCommand.Create("missing", UUID.randomUUID(), VerificationMode.NONE, expiry, false, Optional.empty(), null, null);
        fails(() -> service.execute(missing, caller()), ShareCommandException.Code.FORBIDDEN);
        UUID captured = UUID.randomUUID();
        UUID capturedEvidence = UUID.randomUUID();
        try (Connection admin = base.admin()) {
            admin.setAutoCommit(false);
            seedBinding(admin, subject, captured);
            SharingQueryFixture.execute(admin, "UPDATE subject_number.caller_number_binding SET status = 'RETIRED', revision = revision + 1 WHERE id = ?", captured);
            seedEvidence(admin, capturedEvidence, subject, captured.toString());
            admin.commit();
        }
        assertThat(executeEvidence("captured", capturedEvidence)).isInstanceOf(CustomerShareCommandResult.Created.class);
        try (Connection admin = base.admin()) {
            SharingQueryFixture.execute(admin, "UPDATE subject_number.caller_number_binding SET status = 'DISABLED', revision = revision + 1 WHERE id = ?", binding);
        }
        assertThat(executeEvidence("captured", capturedEvidence)).isInstanceOf(CustomerShareCommandResult.CreationReplayed.class);
        fails(() -> create("replay", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.SHARE_RISK_BLOCKED);
        for (String invalid : List.of("not-a-uuid", UUID.randomUUID().toString())) {
            UUID invalidEvidence = UUID.randomUUID();
            try (Connection admin = base.admin()) {
                admin.setAutoCommit(false);
                seedEvidence(admin, invalidEvidence, subject, invalid);
                admin.commit();
            }
            fails(() -> executeEvidence("invalid-capture", invalidEvidence), ShareCommandException.Code.SHARE_RISK_BLOCKED);
        }
        for (boolean owned : List.of(false, true)) {
            UUID otherSubject = UUID.randomUUID();
            UUID otherEvidence = UUID.randomUUID();
            try (Connection admin = base.admin()) {
                admin.setAutoCommit(false);
                SharingQueryFixture.execute(admin, "INSERT INTO subject_number.business_subject(id, owner_account_id, source) VALUES (?, ?, 'WEB')",
                        otherSubject, owned ? base.other : null);
                seedBinding(admin, otherSubject, UUID.randomUUID());
                seedEvidence(admin, otherEvidence, otherSubject, null);
                admin.commit();
            }
            fails(() -> executeEvidence("owner-check", otherEvidence), owned ? ShareCommandException.Code.FORBIDDEN : ShareCommandException.Code.SHARE_RISK_BLOCKED);
        }
        failOn = "subject_number.business_subject";
        fails(() -> executeEvidence("risk-sql", capturedEvidence), ShareCommandException.Code.SHARE_RISK_BLOCKED);
        failOn = "evidence.media_object";
        fails(() -> executeEvidence("projection-sql", capturedEvidence), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        failOn = null;
        UUID pendingEvidence = UUID.randomUUID();
        String pendingEvent = UUID.randomUUID().toString();
        try (Connection admin = base.admin()) {
            admin.setAutoCommit(false);
            SharingQueryFixture.execute(admin, "INSERT INTO evidence.evidence_record(id, subject_id, source_type, source_ref_id, source_event_domain, source_event_id, "
                    + "identity_at_capture, consent_version, snapshot, snapshot_sha256, call_id, session_id, status) "
                    + "SELECT ?, subject_id, source_type, source_ref_id, source_event_domain, ?, identity_at_capture, consent_version, "
                    + "jsonb_set(snapshot, '{sourceEventId}', to_jsonb(?::text)), snapshot_sha256, call_id, session_id, 'PENDING' "
                    + "FROM evidence.evidence_record WHERE id = ?", pendingEvidence, pendingEvent, pendingEvent, capturedEvidence);
            SharingQueryFixture.execute(admin, "INSERT INTO evidence.evidence_event(id, evidence_id, event_domain, event_id, event_type, payload_redacted, payload_sha256) "
                    + "VALUES (?, ?, 'EVIDENCE', ?, 'EVIDENCE_CREATED', '{}'::jsonb, repeat('a',64))", UUID.randomUUID(), pendingEvidence, pendingEvent);
            admin.commit();
        }
        executions.clear();
        fails(() -> executeEvidence("pending-evidence", pendingEvidence), ShareCommandException.Code.FORBIDDEN);
        assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT")
                || execution.sql().contains("outbox_append") || execution.sql().contains("append_fact"));
    }

    @Test
    void createConcurrentSameAndDifferentIdentities() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> create("concurrent-mobile", VerificationMode.DESIGNATED_MOBILE, Optional.empty()));
            var second = executor.submit(() -> create("concurrent-mobile", VerificationMode.DESIGNATED_MOBILE, Optional.empty()));
            assertThat(List.of(first.get(), second.get()).stream().filter(CustomerShareCommandResult.Created.class::isInstance).count()).isEqualTo(1);
            assertThat(tokens).hasValue(1);
            var different = executor.submit(() -> create("different", VerificationMode.NONE, Optional.empty()));
            assertThat(different.get()).isInstanceOf(CustomerShareCommandResult.Created.class);
        }
        assertThat(executions.stream().filter(execution -> execution.sql().contains("pg_advisory_xact_lock"))).hasSize(3);
        assertThat(executions.stream().filter(execution -> execution.sql().contains("pg_advisory_xact_lock"))).allMatch(execution -> execution.role().equals("sharing_command_role"));
        var locks = executions.stream().filter(execution -> execution.sql().contains("pg_advisory_xact_lock")).toList();
        assertThat(locks).allMatch(execution -> execution.bindings().equals(List.of("customer_iam.account:" + base.account)));
        executions.clear();
        create("organization", VerificationMode.ORGANIZATION_MEMBER, Optional.empty());
        assertThat(executions.stream().filter(execution -> execution.sql().contains("pg_advisory_xact_lock")).map(Execution::bindings))
                .containsExactly(List.of("organization_access.organization:" + base.org),
                        List.of("organization_access.membership:" + base.org + ":" + base.account), List.of("customer_iam.account:" + base.account));
        try (Connection admin = base.admin()) {
            SharingQueryFixture.execute(admin, "UPDATE organization_access.organization_membership SET status = 'REVOKED', revision = revision + 1 WHERE id = ?", base.membership);
        }
        executions.clear();
        fails(() -> create("revoked-only", VerificationMode.ORGANIZATION_MEMBER, Optional.empty()), ShareCommandException.Code.FORBIDDEN);
        assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT")
                || execution.sql().contains("outbox_append") || execution.sql().contains("append_fact"));
        try (Connection admin = base.admin()) {
            SharingQueryFixture.execute(admin, "INSERT INTO organization_access.organization_membership(id, organization_id, account_id, role) VALUES (?, ?, ?, 'MEMBER')",
                    UUID.randomUUID(), base.org, base.account);
            assertThat(DSL.using(admin).fetch("SELECT status FROM organization_access.organization_membership WHERE organization_id = ? AND account_id = ?", base.org, base.account)
                    .getValues("status", String.class)).containsExactlyInAnyOrder("REVOKED", "ACTIVE");
        }
        var regranted = (CustomerShareCommandResult.Created) create("regranted", VerificationMode.ORGANIZATION_MEMBER, Optional.empty());
        try (Connection admin = base.admin()) {
            var grant = DSL.using(admin).fetchOne("SELECT created_by_account_id, owner_organization_id FROM sharing.evidence_share_grant WHERE share_id = ?", regranted.state().shareId());
            assertThat(grant.get("created_by_account_id", UUID.class)).isEqualTo(base.account);
            assertThat(grant.get("owner_organization_id", UUID.class)).isEqualTo(base.org);
        }
    }

    @Test
    void evidenceProjectionUsesCommandTransactionConnection() throws Exception {
        var reader = context.getBean(ShareableEvidenceProjectionReader.class);
        assertThatThrownBy(() -> reader.readShareableProjection(dsl, base.evidence)).isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
        var first = (CustomerShareCommandResult.Created) create("canonical", VerificationMode.NONE, Optional.empty());
        assertThat(executions.stream().map(Execution::pid).distinct()).hasSize(1);
        assertThat(executions.stream().filter(execution -> execution.sql().contains("evidence.media_object"))).allMatch(execution -> execution.role().equals("evidence_role"));
        assertThat(executions.stream().filter(execution -> execution.sql().contains("subject_number.business_subject"))).allMatch(execution -> execution.role().equals("sharing_command_role"));
        assertThat(executions.stream().filter(execution -> execution.sql().equals("SELECT clock_timestamp()"))).hasSize(1);
        try (Connection admin = base.admin()) {
            var row = DSL.using(admin).fetchOne("SELECT content_snapshot::text AS snapshot, content_sha256, jsonb_typeof(content_snapshot->'content') AS content_type, "
                    + "jsonb_typeof(content_snapshot->'verification') AS verification_type, jsonb_typeof(content_snapshot->'creation') AS creation_type FROM sharing.evidence_share_grant WHERE share_id = ?", first.state().shareId());
            for (String type : List.of("content_type", "verification_type", "creation_type")) assertThat(row.get(type)).isEqualTo("object");
            var mapper = JsonMapper.builder().build();
            var snapshot = mapper.readTree(row.get("snapshot", String.class));
            ObjectNode scope = mapper.createObjectNode();
            scope.set("content", snapshot.path("content")); scope.set("verification", snapshot.path("verification"));
            assertThat(snapshot.path("creation").path("scopeDigest").asString()).isEqualTo(DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(scope)));
            scope.set("creation", snapshot.path("creation"));
            assertThat(row.get("content_sha256")).isEqualTo(DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(scope)));
            ObjectNode creation = (ObjectNode) snapshot.path("creation").deepCopy();
            String fingerprint = creation.remove("creationFingerprint").asString();
            assertThat(fingerprint).isEqualTo(DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(creation)));
            assertThat(creation.path("expiresAt").asString()).isEqualTo(DefaultCustomerShareCommandService.utc(expiry));
            assertThat(snapshot.path("content").path("artifacts").get(0).path("artifactRole").asString()).isEqualTo("ORIGINAL");
        }
        failOn = "evidence.media_object";
        fails(() -> create("reader-failure", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        assertThat(executions.getLast().sql()).isEqualTo("SELECT current_user");
        assertThat(executions.getLast().role()).isEqualTo("sharing_command_role");
        failOn = null;
        var inconsistentRepository = spy(repository);
        doAnswer(invocation -> {
            var reference = repository.readEvidenceReference(invocation.getArgument(0), invocation.getArgument(1)).orElseThrow();
            return Optional.of(new SharingCommandRepository.EvidenceReference(reference.evidenceId(), UUID.randomUUID(),
                    reference.evidenceStatus(), reference.identityAtCapture()));
        }).when(inconsistentRepository).readEvidenceReference(any(), eq(base.evidence));
        var inconsistentService = service(context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class), inconsistentRepository);
        executions.clear();
        fails(() -> inconsistentService.execute(new CustomerShareCommand.Create("inconsistent-reference", base.evidence,
                VerificationMode.NONE, expiry, false, Optional.empty(), null, null), caller()), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT")
                || execution.sql().contains("outbox_append") || execution.sql().contains("append_fact"));
        UUID ambiguousMedia = UUID.randomUUID();
        try (Connection admin = base.admin()) {
            admin.setAutoCommit(false);
            SharingQueryFixture.execute(admin, "INSERT INTO evidence.media_object(id, storage_provider, bucket, object_key, object_version_id, sha256, size_bytes, mime_type, crypto_key_ref) "
                    + "SELECT ?, m.storage_provider, m.bucket, ?, m.object_version_id, m.sha256, m.size_bytes, m.mime_type, m.crypto_key_ref "
                    + "FROM evidence.evidence_artifact a JOIN evidence.media_object m ON m.id = a.media_object_id WHERE a.evidence_id = ? AND a.artifact_role = 'DERIVED'",
                    ambiguousMedia, "private-" + ambiguousMedia, base.evidence);
            SharingQueryFixture.execute(admin, "INSERT INTO evidence.evidence_artifact(id, evidence_id, media_object_id, artifact_role) "
                    + "VALUES (?, ?, ?, 'DERIVED')", UUID.randomUUID(), base.evidence, ambiguousMedia);
            admin.commit();
        }
        executions.clear();
        fails(() -> create("ambiguous-projection", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        assertThat(executions.getLast().sql()).isEqualTo("SELECT current_user");
        assertThat(executions.getLast().role()).isEqualTo("sharing_command_role");
        assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT")
                || execution.sql().contains("outbox_append") || execution.sql().contains("append_fact"));
    }

    @Test
    void createAppendFailureRollsBackGrantAuditAndOutbox() throws Exception {
        for (String boundary : List.of("INSERT INTO sharing.evidence_share_grant", "platform.outbox_append", "audit.append_fact")) {
            failOn = boundary;
            fails(() -> create(boundary, VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.INTERNAL);
            assertNoFacts();
        }
        failOn = null;
        TransactionTemplate normal = context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class);
        var invalidMaterial = spy(repository);
        doAnswer(invocation -> {
            ShareGrant grant = invocation.getArgument(1);
            var invalid = new ShareGrant(grant.id(), grant.shareId(), grant.capabilityTokenHash(), grant.evidenceId(),
                    grant.createdByAccountId(), grant.grantOwnerOrganizationId(), grant.verificationMode(), grant.codeSalt(),
                    grant.codeHash(), grant.codeGeneration(), grant.validFrom(), grant.expiresAt(), 0, grant.admissionsUsed(),
                    grant.downloadAllowed(), grant.contentSnapshot(), grant.contentSha256(), grant.status(), grant.revocationGeneration(), grant.revision());
            return repository.insertGrantReturning(invocation.getArgument(0), invalid);
        }).when(invalidMaterial).insertGrantReturning(any(), any());
        service = service(normal, invalidMaterial);
        fails(() -> create("check-material", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        assertNoFacts();
        for (String guard : List.of("SHARE_GRANT_IMMUTABLE", "SHARE_GRANT_EXPIRY_DECREASE", "SHARE_GRANT_GENERATION_INVALID",
                "SHARE_GRANT_CODE_ROTATION_INVALID", "SHARE_GRANT_STATUS_TERMINAL", "SHARE_GRANT_STATUS_TRANSITION",
                "SHARE_GRANT_EXPIRY_INVALID", "SHARE_GRANT_EXTENSION_INVALID", "SHARE_GRANT_REVISION_INVALID",
                "SHARE_ADMISSION_REFUSED", "SHARING_ROW_APPEND_ONLY", "TEST_UNEXPECTED_CANARY")) {
            var guarded = spy(repository);
            doAnswer(invocation -> {
                invocation.callRealMethod();
                if (guard.equals("SHARE_GRANT_EXPIRY_DECREASE")) {
                    dsl.execute("UPDATE sharing.evidence_share_grant SET expires_at = expires_at - interval '1 second', revision = revision + 1 WHERE created_by_account_id = ?", base.account);
                    throw new AssertionError("Expected V009 guard");
                }
                throw new org.jooq.exception.DataAccessException("TEST_DATABASE_CANARY",
                        new java.sql.SQLException("ERROR: " + guard + "\n  Where: TEST_SQL_CANARY", "P0001"));
            }).when(guarded).appendAudit(any(), any());
            service = service(normal, guarded);
            fails(() -> create(guard, VerificationMode.NONE, Optional.empty()), guard.equals("TEST_UNEXPECTED_CANARY")
                    ? ShareCommandException.Code.INTERNAL : ShareCommandException.Code.SERVICE_UNAVAILABLE);
            assertNoFacts();
        }
        for (String state : List.of("23514", "23505", "23503", "XX000")) {
            var failed = spy(repository);
            doAnswer(invocation -> {
                invocation.callRealMethod();
                var sql = new java.sql.SQLException("TEST_DATABASE_CANARY", state);
                if (state.equals("23505")) sql = new java.sql.SQLException("uq_share_grant_token_hash TEST_TOKEN_CANARY", state);
                var wrapper = new java.sql.SQLException("TEST_WRAPPER_CANARY");
                wrapper.setNextException(sql);
                throw new org.jooq.exception.DataAccessException("TEST_SQL_CANARY", wrapper);
            }).when(failed).appendAudit(any(), any());
            service = service(normal, failed);
            fails(() -> create(state, VerificationMode.NONE, Optional.empty()), state.equals("23514")
                    ? ShareCommandException.Code.SERVICE_UNAVAILABLE : ShareCommandException.Code.INTERNAL);
            assertNoFacts();
        }
        HikariDataSource pool = context.getBean("sharingCommandDataSource", HikariDataSource.class);
        var manager = context.getBean("sharingCommandTransactionManager", DataSourceTransactionManager.class);
        assertThat(normal.getTransactionManager()).isSameAs(manager);
        assertThat(manager.isRollbackOnCommitFailure()).isTrue();
        AtomicInteger commits = new AtomicInteger();
        AtomicInteger rollbacks = new AtomicInteger();
        var failingDataSource = new org.springframework.jdbc.datasource.DelegatingDataSource(pool) {
            @Override
            public Connection getConnection() throws java.sql.SQLException {
                Connection connection = super.getConnection();
                return (Connection) java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                        (proxy, method, arguments) -> {
                            if (method.getName().equals("commit")) {
                                commits.incrementAndGet();
                                throw new java.sql.SQLException("TEST_COMMIT_CANARY", "08006");
                            }
                            if (method.getName().equals("rollback")) rollbacks.incrementAndGet();
                            try { return method.invoke(connection, arguments); }
                            catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                        });
            }
        };
        var connectionProvider = dsl.configuration().connectionProvider();
        manager.setDataSource(failingDataSource);
        dsl.configuration().set(new org.jooq.impl.DataSourceConnectionProvider(
                new org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy(failingDataSource)));
        service = service(normal, repository);
        executions.clear();
        fails(() -> create("commit", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.INTERNAL);
        assertThat(commits).hasValue(1);
        assertThat(rollbacks).hasValue(1);
        assertThat(executions).anyMatch(execution -> execution.sql().contains("audit.append_fact"));
        assertNoFacts();
        manager.setDataSource(pool);
        dsl.configuration().set(connectionProvider);
        for (boolean changed : List.of(false, true)) {
            SharingCommandRepository raced = spy(repository);
            doAnswer(invocation -> {
                ShareGrant proposed = invocation.getArgument(1);
                ShareGrant concurrent = competing(proposed, changed, false);
                try (Connection admin = base.admin()) { repository.insertGrantReturning(DSL.using(admin, org.jooq.SQLDialect.POSTGRES), concurrent); }
                return invocation.callRealMethod();
            }).when(raced).insertGrantReturning(eq(dsl), any());
            service = service(normal, raced);
            if (changed) fails(() -> create("zero-row-changed", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.SHARE_CREATION_CONFLICT);
            else assertThat(create("zero-row-exact", VerificationMode.NONE, Optional.empty())).isInstanceOf(CustomerShareCommandResult.CreationReplayed.class);
            verify(raced, times(2)).findCreationIdentity(eq(dsl), anyString());
            verify(raced, never()).appendOutbox(any(), any());
            verify(raced, never()).appendAudit(any(), any());
        }
        SharingCommandRepository tokenRace = spy(repository);
        doAnswer(invocation -> {
            ShareGrant proposed = invocation.getArgument(1);
            try (Connection admin = base.admin()) { repository.insertGrantReturning(DSL.using(admin, org.jooq.SQLDialect.POSTGRES), competing(proposed, false, true)); }
            return invocation.callRealMethod();
        }).when(tokenRace).insertGrantReturning(eq(dsl), any());
        service = service(normal, tokenRace);
        fails(() -> create("unique-token-race", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.INTERNAL);
        verify(tokenRace, never()).appendOutbox(any(), any());
        verify(tokenRace, never()).appendAudit(any(), any());
        var token = new CapabilityTokenProvider.CapabilityTokenMaterial("fixed-test-token", DeterministicShareCommandProviders.digest("fixed-test-token"));
        service = service(normal, repository, grantId -> new ProviderResult.Allowed<>(token));
        create("token-precheck-first", VerificationMode.NONE, Optional.empty());
        int writes = (int) executions.stream().filter(execution -> execution.sql().startsWith("INSERT INTO sharing.evidence_share_grant")).count();
        fails(() -> create("token-precheck-collision", VerificationMode.NONE, Optional.empty()), ShareCommandException.Code.INTERNAL);
        assertThat(executions.stream().filter(execution -> execution.sql().startsWith("INSERT INTO sharing.evidence_share_grant"))).hasSize(writes);
        assertThat(executions.stream().filter(execution -> execution.sql().startsWith("INSERT INTO sharing.evidence_share_grant")))
                .allMatch(execution -> execution.sql().contains("ON CONFLICT (share_id) DO NOTHING RETURNING")
                        && execution.sql().contains("?::timestamptz, ?::timestamptz"));
    }

    private CustomerShareCommandService service(TransactionTemplate template, SharingCommandRepository selectedRepository) {
        return service(template, selectedRepository, context.getBean(CapabilityTokenProvider.class));
    }

    private CustomerShareCommandService service(TransactionTemplate template, SharingCommandRepository selectedRepository, CapabilityTokenProvider selectedTokens) {
        return new DefaultCustomerShareCommandService(template, dsl, selectedRepository, context.getBean(ShareableEvidenceProjectionReader.class),
                context.getBean(ShareCreationPolicyProvider.class), selectedTokens, context.getBean(ShareExtractionCodeProvider.class),
                context.getBean(DesignatedMobileBindingProvider.class), context.getBean(Sha256Provider.class));
    }

    private static ShareGrant competing(ShareGrant grant, boolean changed, boolean tokenRace) {
        var mapper = JsonMapper.builder().build();
        ObjectNode snapshot = (ObjectNode) mapper.readTree(grant.contentSnapshot().data());
        Instant expiry = changed ? grant.expiresAt().plusSeconds(1) : grant.expiresAt();
        if (changed) {
            ObjectNode creation = (ObjectNode) snapshot.path("creation");
            creation.put("expiresAt", DefaultCustomerShareCommandService.utc(expiry));
            creation.remove("creationFingerprint");
            creation.put("creationFingerprint", DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(creation)));
        }
        ObjectNode complete = mapper.createObjectNode();
        complete.set("content", snapshot.path("content")); complete.set("verification", snapshot.path("verification"));
        complete.set("creation", snapshot.path("creation"));
        return new ShareGrant(UUID.randomUUID(), tokenRace ? "share:competitor-" + UUID.randomUUID() : grant.shareId(),
                tokenRace ? grant.capabilityTokenHash() : DeterministicShareCommandProviders.digest(UUID.randomUUID().toString()),
                grant.evidenceId(), grant.createdByAccountId(), grant.grantOwnerOrganizationId(), grant.verificationMode(),
                grant.codeSalt(), grant.codeHash(), grant.codeGeneration(), grant.validFrom(), expiry, grant.maxAdmissions(),
                grant.admissionsUsed(), grant.downloadAllowed(), JSONB.valueOf(snapshot.toString()),
                DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(complete)), grant.status(), grant.revocationGeneration(), grant.revision());
    }

    private CustomerShareCommandResult create(String key, VerificationMode mode, Optional<UUID> trace) {
        return create(key, mode, trace, false, null, mode == VerificationMode.ORGANIZATION_MEMBER);
    }

    private CustomerShareCommandResult create(String key, VerificationMode mode, Optional<UUID> trace, boolean downloadAllowed, Integer maxAdmissions, boolean organizationWorkspace) {
        return service.execute(new CustomerShareCommand.Create(key, organizationWorkspace || mode == VerificationMode.ORGANIZATION_MEMBER ? organizationEvidence : base.evidence,
                mode, expiry, downloadAllowed, trace, maxAdmissions, mode == VerificationMode.DESIGNATED_MOBILE ? "13800138000" : null),
                new ShareCommandCaller(base.account, organizationWorkspace || mode == VerificationMode.ORGANIZATION_MEMBER ? Optional.of(base.org) : Optional.empty()));
    }

    private CustomerShareCommandResult executeEvidence(String key, UUID evidenceId) {
        return service.execute(new CustomerShareCommand.Create(key, evidenceId, VerificationMode.NONE, expiry, false, Optional.empty(), null, null), caller());
    }

    private ShareCommandCaller caller() { return new ShareCommandCaller(base.account, Optional.empty()); }

    private static void seedBinding(Connection admin, UUID selectedSubject, UUID selectedBinding) throws Exception {
        SharingQueryFixture.execute(admin, "INSERT INTO subject_number.caller_number_binding(id, subject_id, number_type, number_ciphertext, number_hmac, number_masked, verified_at) "
                + "VALUES (?, ?, 'MOBILE', 'v1.test.payload', ?, 'synthetic', clock_timestamp())", selectedBinding, selectedSubject,
                DeterministicShareCommandProviders.digest(selectedBinding.toString()));
    }

    private static void seedEvidence(Connection admin, UUID selectedEvidence, UUID selectedSubject, String capturedBinding) throws Exception {
        String event = UUID.randomUUID().toString();
        var identity = JsonMapper.builder().build().createObjectNode().put("subjectId", selectedSubject.toString())
                .put("capturedAt", "2026-11-04T10:00:00Z").put("sourceChannel", "IVR");
        if (capturedBinding != null) identity.put("bindingId", capturedBinding);
        SharingQueryFixture.execute(admin, "INSERT INTO evidence.evidence_record(id, subject_id, source_type, source_ref_id, source_event_domain, source_event_id, "
                + "identity_at_capture, consent_version, snapshot, snapshot_sha256, call_id, session_id, status, solidified_at) "
                + "SELECT ?, ?, source_type, source_ref_id, source_event_domain, ?, ?::jsonb, consent_version, "
                + "jsonb_set(jsonb_set(snapshot, '{source,subjectId}', to_jsonb(?::text)), '{sourceEventId}', to_jsonb(?::text)), "
                + "snapshot_sha256, call_id, session_id, status, solidified_at FROM evidence.evidence_record WHERE id = ?",
                selectedEvidence, selectedSubject, event, identity.toString(), selectedSubject.toString(), event, base.evidence);
        SharingQueryFixture.execute(admin, "INSERT INTO evidence.evidence_event(id, evidence_id, event_domain, event_id, event_type, payload_redacted, payload_sha256) "
                + "VALUES (?, ?, 'EVIDENCE', ?, 'EVIDENCE_CREATED', '{}'::jsonb, repeat('a',64))", UUID.randomUUID(), selectedEvidence, event);
        SharingQueryFixture.execute(admin, "INSERT INTO evidence.evidence_artifact(id, evidence_id, media_object_id, artifact_role) "
                + "SELECT gen_random_uuid(), ?, media_object_id, artifact_role FROM evidence.evidence_artifact WHERE evidence_id = ?", selectedEvidence, base.evidence);
    }

    private void assertNoFacts() throws Exception {
        try (Connection admin = base.admin()) {
            DSLContext facts = DSL.using(admin);
            assertThat(count(facts, "sharing.evidence_share_grant", "created_by_account_id", base.account)).isZero();
            assertThat(count(facts, "audit.audit_fact", "actor_id", base.account)).isZero();
            assertThat(facts.fetchOne("SELECT count(*) FROM platform.outbox_event o JOIN sharing.evidence_share_grant g ON o.aggregate_id = g.id WHERE g.created_by_account_id = ?", base.account).get(0, Integer.class)).isZero();
            for (Execution execution : executions) {
                if (execution.sql().startsWith("INSERT INTO sharing.evidence_share_grant")) {
                    assertThat(count(facts, "platform.outbox_event", "aggregate_id", (UUID) execution.bindings().getFirst())).isZero();
                }
            }
        }
    }

    private static int count(DSLContext facts, String table, String column, UUID id) {
        return facts.fetchOne("SELECT count(*) FROM " + table + " WHERE " + column + " = ?", id).get(0, Integer.class);
    }

    private static void fails(Runnable action, ShareCommandException.Code code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ShareCommandException.class, failure -> assertThat(failure.code()).isEqualTo(code))
                .hasMessage(code.name()).hasNoCause();
    }
}
