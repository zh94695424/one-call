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
    private volatile ProviderResult<ShareRotationPolicyProvider.RotationPolicyDecision> rotationPolicy = new ProviderResult.Allowed<>(ShareRotationPolicyProvider.RotationPolicyDecision.ALLOW);
    private volatile String failOn;
    private volatile String rotationMiss;

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
        context.registerBean("shareRotationPolicyProvider", ShareRotationPolicyProvider.class, () -> request -> rotationPolicy);
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
                if (rotationMiss != null && execution.sql().startsWith("UPDATE sharing.evidence_share_grant SET status = ?, code_salt")) {
                    try {
                        if (rotationMiss.equals("revision")) execution.statement().setInt(8, Integer.MAX_VALUE);
                        if (rotationMiss.equals("status")) execution.statement().setString(9, "ACTIVE");
                        if (rotationMiss.equals("code")) execution.statement().setInt(10, 0);
                    } catch (java.sql.SQLException failure) { throw new IllegalStateException("Conditional probe failed", failure); }
                }
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
                context.getBean(ShareCreationPolicyProvider.class), context.getBean(ShareRotationPolicyProvider.class), selectedTokens, context.getBean(ShareExtractionCodeProvider.class),
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

    @Test
    void revokeWritesRegisteredEventAuditAndOutboxAtomically() throws Exception {
        for (VerificationMode mode : VerificationMode.values()) {
            for (boolean locked : List.of(false, true)) {
                var created = (CustomerShareCommandResult.Created) create("revoke-facts-" + mode + locked, mode, Optional.empty());
                String shareId = created.state().shareId();
                ShareCommandCaller selected = new ShareCommandCaller(base.account,
                        mode == VerificationMode.ORGANIZATION_MEMBER ? Optional.of(base.org) : Optional.empty());
                prepareGrant(shareId, locked ? "LOCKED" : "ACTIVE", locked ? 2 : 0);
                var before = grantSnapshot(shareId);
                UUID grantId = (UUID) before.get("id");
                UUID trace = UUID.randomUUID();
                String reason = locked ? " exact reason 😀 " : null;
                var command = new CustomerShareCommand.Revoke(shareId, "revoke,raw-key", locked ? Optional.of(trace) : Optional.empty(),
                        locked ? 2 : null, reason);
                int oldTokens = tokens.get();
                int oldCodes = codes.get();
                executions.clear();
                var result = (CustomerShareCommandResult.Revoked) service.execute(command, selected);
                assertThat(result.state().status().name()).isEqualTo("REVOKED");
                assertThat(result.state().revocationGeneration()).isEqualTo(locked ? 3 : 1);
                assertThat(tokens).hasValue(oldTokens);
                assertThat(codes).hasValue(oldCodes);
                assertRevokeHead(before, grantSnapshot(shareId));
                assertRevokeExecution(selected);
                try (Connection admin = base.admin()) {
                    DSLContext facts = DSL.using(admin);
                    var event = facts.fetchOne("SELECT id, share_grant_id, share_admission_id, event_domain, event_id, event_type, result, occurred_at,"
                            + " risk_digest, payload_redacted, payload_sha256 FROM sharing.share_access_event WHERE share_grant_id = ?", grantId);
                    var mapper = JsonMapper.builder().build();
                    String requestDigest = DeterministicShareCommandProviders.digest("revoke,raw-key");
                    var identity = mapper.createObjectNode().put("shareGrantId", grantId.toString()).put("actionType", "REVOKE");
                    identity.set("sourceIdentity", mapper.createObjectNode().put("kind", "REQUEST").put("actionRequestId", "revoke,raw-key"));
                    String eventId = "share-transition:" + DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(identity));
                    var semantics = mapper.createObjectNode().put("shareGrantId", grantId.toString()).put("actionType", "REVOKE")
                            .put("reasonCode", reason).putNull("newExpiresAt").put("actionRequestDigest", requestDigest);
                    String fingerprint = DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(semantics));
                    assertThat(event.get("id", UUID.class)).isNotNull();
                    assertThat(event.get("event_domain")).isEqualTo("SHARE_ACCESS");
                    assertThat(event.get("event_id")).isEqualTo(eventId);
                    assertThat(event.get("event_type")).isEqualTo("REVOKED");
                    assertThat(event.get("result")).isEqualTo("SUCCESS");
                    assertThat(event.get("share_admission_id")).isNull();
                    assertThat(event.get("risk_digest")).isNull();
                    var expected = mapper.createObjectNode().put("actionType", "REVOKE").put("actionRequestDigest", requestDigest)
                            .put("actionFingerprint", fingerprint).put("status", "REVOKED").put("codeGeneration", created.state().codeGeneration())
                            .put("revocationGeneration", locked ? 3 : 1).put("revision", ((Integer) before.get("revision")) + 1);
                    var payload = mapper.readTree(event.get("payload_redacted", JSONB.class).data());
                    assertThat(payload).isEqualTo(expected);
                    assertThat(event.get("payload_sha256")).isEqualTo(DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(expected)));
                    Instant occurred = event.get("occurred_at", java.time.OffsetDateTime.class).toInstant();
                    var outbox = facts.fetchOne("SELECT event_domain, aggregate_type, aggregate_id, event_type, dedupe_key, payload_redacted,"
                            + " payload_ciphertext, payload_sha256 FROM platform.outbox_event WHERE aggregate_id = ? AND event_type = 'SHARE_REVOKED'", grantId);
                    assertThat(outbox.get("event_domain")).isEqualTo("SHARE_ACCESS");
                    assertThat(outbox.get("aggregate_type")).isEqualTo("EVIDENCE_SHARE_GRANT");
                    assertThat(outbox.get("aggregate_id")).isEqualTo(grantId);
                    assertThat(outbox.get("event_type")).isEqualTo("SHARE_REVOKED");
                    assertThat(outbox.get("dedupe_key")).isEqualTo("share-transition:" + eventId);
                    assertThat(outbox.get("payload_ciphertext")).isNull();
                    var expectedOutbox = expected.deepCopy().put("shareId", shareId).put("grantId", grantId.toString())
                            .put("occurredAt", DefaultCustomerShareCommandService.utc(occurred));
                    assertThat(mapper.readTree(outbox.get("payload_redacted", JSONB.class).data())).isEqualTo(expectedOutbox);
                    assertThat(outbox.get("payload_sha256")).isEqualTo(DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(expectedOutbox)));
                    var audit = facts.fetchOne("SELECT occurred_at, actor_type, actor_id, action_code, object_type, object_id, reason_code,"
                            + " before_snapshot, after_snapshot, request_id, trace_id FROM audit.audit_fact WHERE object_id = ? AND action_code = 'SHARE_GRANT_REVOKE'", grantId);
                    assertThat(audit.get("occurred_at", java.time.OffsetDateTime.class).toInstant()).isEqualTo(occurred);
                    assertThat(audit.get("actor_type")).isEqualTo("ACCOUNT");
                    assertThat(audit.get("actor_id")).isEqualTo(base.account);
                    assertThat(audit.get("action_code")).isEqualTo("SHARE_GRANT_REVOKE");
                    assertThat(audit.get("object_type")).isEqualTo("EVIDENCE_SHARE_GRANT");
                    assertThat(audit.get("object_id")).isEqualTo(grantId);
                    assertThat(audit.get("reason_code")).isEqualTo(reason);
                    assertThat(audit.get("request_id")).isEqualTo(requestDigest);
                    assertThat(audit.get("trace_id")).isEqualTo(locked ? trace.toString() : null);
                    assertThat(mapper.readTree(audit.get("before_snapshot", JSONB.class).data())).isEqualTo(mapper.createObjectNode()
                            .put("status", locked ? "LOCKED" : "ACTIVE").put("revision", (Integer) before.get("revision")));
                    assertThat(mapper.readTree(audit.get("after_snapshot", JSONB.class).data())).isEqualTo(mapper.createObjectNode()
                            .put("status", "REVOKED").put("revision", ((Integer) before.get("revision")) + 1).put("reasonCode", reason));
                    assertThat(event.toString() + outbox).doesNotContain("revoke,raw-key", "exact reason", created.capabilityToken());
                }
                assertRevokeCounts(grantId, 1);
                var revokedHead = grantSnapshot(shareId);
                List<String> durable = revokeFacts(grantId);
                fails(() -> service.execute(new CustomerShareCommand.Revoke(shareId, "different-key", Optional.empty(), 99, null), selected),
                        ShareCommandException.Code.SHARE_REVOKED);
                assertThat(grantSnapshot(shareId)).isEqualTo(revokedHead);
                assertThat(revokeFacts(grantId)).isEqualTo(durable);
            }
        }
    }

    @Test
    void revokeExactReplayAddsNoFactsOrRevision() throws Exception {
        var created = (CustomerShareCommandResult.Created) create("replay-create", VerificationMode.EXTRACTION_CODE, Optional.empty());
        String shareId = created.state().shareId();
        var first = revoke(shareId, "replay-key", 0, " reason ");
        var original = grantSnapshot(shareId);
        UUID grantId = (UUID) original.get("id");
        List<String> durable = revokeFacts(grantId);
        var seam = spy(repository);
        doReturn(expiry.plusSeconds(1)).when(seam).sampleDatabaseTime(any());
        service = service(context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class), seam);
        int oldTokens = tokens.get();
        int oldCodes = codes.get();
        for (Integer witness : new Integer[] {null, 0, 1, 100}) {
            executions.clear();
            assertThat(revoke(shareId, "replay-key", witness, " reason ")).isEqualTo(first);
            assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT") || execution.sql().startsWith("UPDATE")
                    || execution.sql().contains("outbox_append") || execution.sql().contains("append_fact"));
            assertThat(grantSnapshot(shareId)).isEqualTo(original);
            assertThat(revokeFacts(grantId)).isEqualTo(durable);
        }
        assertThat(tokens).hasValue(oldTokens);
        assertThat(codes).hasValue(oldCodes);
        assertRevokeCounts(grantId, 1);
        fails(() -> service.execute(new CustomerShareCommand.Revoke(shareId, "replay-key", Optional.empty(), 0, " reason "),
                new ShareCommandCaller(base.account, Optional.of(base.org))), ShareCommandException.Code.NOT_FOUND);
        try (Connection admin = base.admin()) { SharingQueryFixture.execute(admin,
                "UPDATE customer_iam.customer_account SET status = 'DISABLED', revision = revision + 1 WHERE id = ?", base.account); }
        executions.clear();
        fails(() -> revoke(shareId, "replay-key", 0, " reason "), ShareCommandException.Code.FORBIDDEN);
        assertThat(executions).noneMatch(execution -> execution.sql().contains("FROM sharing.evidence_share_grant"));
        assertThat(grantSnapshot(shareId)).isEqualTo(original);
        assertThat(revokeFacts(grantId)).isEqualTo(durable);
    }

    @Test
    void revokeChangedReasonTerminalExpiryAndStaleWitnessConflict() throws Exception {
        var normal = context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class);
        CustomerShareCommandService real = service;
        for (String status : List.of("ACTIVE", "LOCKED", "REVOKED", "EXPIRED")) {
            for (boolean elapsed : List.of(false, true)) {
                for (Integer witness : new Integer[] {null, 0, 2, 3}) {
                    String key = status + elapsed + witness;
                    if (status.equals("EXPIRED")) {
                        var expired = spy(repository);
                        doAnswer(invocation -> {
                            ShareGrant proposed = invocation.getArgument(1);
                            return repository.insertGrantReturning(invocation.getArgument(0), new ShareGrant(proposed.id(), proposed.shareId(),
                                    proposed.capabilityTokenHash(), proposed.evidenceId(), proposed.createdByAccountId(), proposed.grantOwnerOrganizationId(),
                                    proposed.verificationMode(), proposed.codeSalt(), proposed.codeHash(), proposed.codeGeneration(),
                                    proposed.validFrom().minusSeconds(7200), proposed.validFrom().minusSeconds(3600), proposed.maxAdmissions(),
                                    proposed.admissionsUsed(), proposed.downloadAllowed(), proposed.contentSnapshot(), proposed.contentSha256(),
                                    proposed.status(), proposed.revocationGeneration(), proposed.revision()));
                        }).when(expired).insertGrantReturning(any(), any());
                        service = service(normal, expired);
                    }
                    var created = (CustomerShareCommandResult.Created) create("matrix-" + key, VerificationMode.NONE, Optional.empty());
                    String shareId = created.state().shareId();
                    prepareGrant(shareId, status, 2);
                    var before = grantSnapshot(shareId);
                    UUID grantId = (UUID) before.get("id");
                    var seam = spy(repository);
                    Instant expires = ((java.time.OffsetDateTime) before.get("expires_at")).toInstant();
                    doReturn(elapsed || status.equals("EXPIRED") ? expires : expires.minusSeconds(1)).when(seam).sampleDatabaseTime(any());
                    service = service(normal, seam);
                    var expected = status.equals("REVOKED") ? ShareCommandException.Code.SHARE_REVOKED
                            : status.equals("EXPIRED") || elapsed ? ShareCommandException.Code.SHARE_TRANSITION_CONFLICT
                            : witness != null && witness != 2 ? ShareCommandException.Code.SHARE_GENERATION_STALE : null;
                    if (expected == null) {
                        assertThat(revoke(shareId, key, witness, null)).isInstanceOf(CustomerShareCommandResult.Revoked.class);
                        assertRevokeHead(before, grantSnapshot(shareId));
                        assertRevokeCounts(grantId, 1);
                    } else {
                        fails(() -> revoke(shareId, key, witness, null), expected);
                        assertThat(grantSnapshot(shareId)).isEqualTo(before);
                        assertRevokeCounts(grantId, 0);
                    }
                    service = real;
                }
            }
        }
        var created = (CustomerShareCommandResult.Created) create("conflicts", VerificationMode.NONE, Optional.empty());
        String shareId = created.state().shareId();
        revoke(shareId, "original", null, " reason ");
        var terminal = grantSnapshot(shareId);
        List<String> durable = revokeFacts((UUID) terminal.get("id"));
        fails(() -> revoke(shareId, "original", 99, "reason"), ShareCommandException.Code.SHARE_TRANSITION_CONFLICT);
        fails(() -> revoke(shareId, "new", 99, null), ShareCommandException.Code.SHARE_REVOKED);
        for (ShareCommandCaller wrong : List.of(new ShareCommandCaller(base.account, Optional.of(base.org)),
                new ShareCommandCaller(base.other, Optional.empty()))) {
            fails(() -> service.execute(new CustomerShareCommand.Revoke(shareId, "original", Optional.empty(), null, " reason "), wrong),
                    ShareCommandException.Code.NOT_FOUND);
        }
        fails(() -> revoke("valid-absent", "x", null, null), ShareCommandException.Code.NOT_FOUND);
        for (String sql : List.of("customer_iam.customer_account", "FOR UPDATE", "FROM sharing.share_access_event")) {
            failOn = sql;
            fails(() -> revoke(shareId, "original", null, " reason "), ShareCommandException.Code.SERVICE_UNAVAILABLE);
            failOn = null;
        }
        assertThat(grantSnapshot(shareId)).isEqualTo(terminal);
        assertThat(revokeFacts((UUID) terminal.get("id"))).isEqualTo(durable);
        var organizationCreated = (CustomerShareCommandResult.Created) create("organization-denials", VerificationMode.ORGANIZATION_MEMBER, Optional.empty());
        String organizationShare = organizationCreated.state().shareId();
        var organizationHead = grantSnapshot(organizationShare);
        UUID organizationGrantId = (UUID) organizationHead.get("id");
        List<String> organizationFacts = revokeFacts(organizationGrantId);
        var organizationCommand = new CustomerShareCommand.Revoke(organizationShare, "denied", Optional.empty(), null, null);
        for (ShareCommandCaller wrong : List.of(caller(), new ShareCommandCaller(base.account, Optional.of(base.org2)),
                new ShareCommandCaller(base.other, Optional.of(base.org)))) {
            fails(() -> service.execute(organizationCommand, wrong), ShareCommandException.Code.NOT_FOUND);
            assertThat(grantSnapshot(organizationShare)).isEqualTo(organizationHead);
            assertThat(revokeFacts(organizationGrantId)).isEqualTo(organizationFacts);
        }
        for (ShareCommandCaller inactive : List.of(new ShareCommandCaller(UUID.randomUUID(), Optional.empty()),
                new ShareCommandCaller(base.account, Optional.of(UUID.randomUUID())), new ShareCommandCaller(base.other, Optional.of(base.org2)))) {
            executions.clear();
            fails(() -> service.execute(organizationCommand, inactive), ShareCommandException.Code.FORBIDDEN);
            assertThat(executions).noneMatch(execution -> execution.sql().contains("FROM sharing.evidence_share_grant"));
            assertThat(grantSnapshot(organizationShare)).isEqualTo(organizationHead);
            assertThat(revokeFacts(organizationGrantId)).isEqualTo(organizationFacts);
        }
        for (String boundary : List.of("account", "organization", "membership")) {
            try (Connection admin = base.admin()) {
                if (boundary.equals("account")) SharingQueryFixture.execute(admin,
                        "UPDATE customer_iam.customer_account SET status = 'DISABLED', revision = revision + 1 WHERE id = ?", base.account);
                if (boundary.equals("organization")) SharingQueryFixture.execute(admin,
                        "UPDATE organization_access.organization SET status = 'SUSPENDED', revision = revision + 1 WHERE id = ?", base.org);
                if (boundary.equals("membership")) SharingQueryFixture.execute(admin,
                        "UPDATE organization_access.organization_membership SET status = 'SUSPENDED', revision = revision + 1 WHERE id = ?", base.membership);
            }
            try {
                executions.clear();
                fails(() -> service.execute(organizationCommand, new ShareCommandCaller(base.account, Optional.of(base.org))), ShareCommandException.Code.FORBIDDEN);
                assertThat(executions).noneMatch(execution -> execution.sql().contains("FROM sharing.evidence_share_grant"));
                assertThat(grantSnapshot(organizationShare)).isEqualTo(organizationHead);
                assertThat(revokeFacts(organizationGrantId)).isEqualTo(organizationFacts);
            } finally {
                try (Connection admin = base.admin()) {
                    SharingQueryFixture.execute(admin, "UPDATE customer_iam.customer_account SET status = 'ACTIVE', revision = revision + 1 WHERE id = ?", base.account);
                    SharingQueryFixture.execute(admin, "UPDATE organization_access.organization SET status = 'ACTIVE', revision = revision + 1 WHERE id = ?", base.org);
                    SharingQueryFixture.execute(admin, "UPDATE organization_access.organization_membership SET status = 'ACTIVE', revision = revision + 1 WHERE id = ?", base.membership);
                }
            }
        }
        for (Integer witness : new Integer[] {null, 0}) {
            for (String reread : List.of("ACTIVE", "REVOKED", "EXPIRED", "STALE", "ABSENT")) {
                var fresh = (CustomerShareCommandResult.Created) create("not-updated-" + witness + reread, VerificationMode.NONE, Optional.empty());
                var before = grantSnapshot(fresh.state().shareId());
                var seam = spy(repository);
                AtomicInteger reads = new AtomicInteger();
                doAnswer(invocation -> {
                    Optional<ShareGrant> current = repository.lockGrant(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
                    if (reads.incrementAndGet() == 1 || reread.equals("ACTIVE")) return current;
                    if (reread.equals("ABSENT")) return Optional.empty();
                    ShareGrant grant = current.orElseThrow();
                    return Optional.of(new ShareGrant(grant.id(), grant.shareId(), grant.capabilityTokenHash(), grant.evidenceId(), grant.createdByAccountId(),
                            grant.grantOwnerOrganizationId(), grant.verificationMode(), grant.codeSalt(), grant.codeHash(), grant.codeGeneration(),
                            grant.validFrom(), grant.expiresAt(), grant.maxAdmissions(), grant.admissionsUsed(), grant.downloadAllowed(),
                            grant.contentSnapshot(), grant.contentSha256(), reread.equals("STALE") ? grant.status()
                                    : com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.valueOf(reread),
                            reread.equals("STALE") ? grant.revocationGeneration() + 1 : grant.revocationGeneration(), grant.revision()));
                }).when(seam).lockGrant(any(), eq(fresh.state().shareId()), eq(caller()));
                doAnswer(invocation -> repository.transitionGrant(invocation.getArgument(0), invocation.getArgument(1),
                        invocation.<Integer>getArgument(2) + 1, invocation.getArgument(3), invocation.getArgument(4), invocation.getArgument(5),
                        invocation.getArgument(6), invocation.getArgument(7), invocation.getArgument(8), invocation.getArgument(9), invocation.getArgument(10)))
                        .when(seam).transitionGrant(any(), any(), anyInt(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), any());
                service = service(normal, seam);
                executions.clear();
                var expected = reread.equals("REVOKED") ? ShareCommandException.Code.SHARE_REVOKED
                        : reread.equals("STALE") && witness != null ? ShareCommandException.Code.SHARE_GENERATION_STALE
                        : ShareCommandException.Code.SHARE_TRANSITION_CONFLICT;
                fails(() -> revoke(fresh.state().shareId(), "zero-row", witness, null), expected);
                assertThat(reads).hasValue(2);
                verify(seam, times(2)).lockGrant(any(), eq(fresh.state().shareId()), eq(caller()));
                verify(seam).transitionGrant(any(), eq((UUID) before.get("id")), anyInt(), any(), eq(Optional.ofNullable(witness)), eq(Optional.empty()),
                        any(), anyInt(), anyInt(), anyInt(), eq(Optional.empty()));
                verify(seam, never()).appendOutbox(any(), any());
                verify(seam, never()).appendAudit(any(), any());
                verify(seam).appendAccessEvent(any(), any());
                verify(seam).findTransition(any(), any(), any());
                assertThat(executions.stream().filter(execution -> execution.sql().startsWith("UPDATE sharing.evidence_share_grant")))
                        .singleElement().satisfies(execution -> assertThat(execution.sql().contains("AND revocation_generation = ?")).isEqualTo(witness != null));
                assertThat(grantSnapshot(fresh.state().shareId())).isEqualTo(before);
                assertRevokeCounts((UUID) before.get("id"), 0);
                service = real;
            }
        }
    }

    @Test
    void revokeConcurrentActionsProduceOneTransition() throws Exception {
        for (boolean organization : List.of(false, true)) {
            for (boolean sameKey : List.of(false, true)) {
                var created = (CustomerShareCommandResult.Created) create("concurrent-" + organization + sameKey,
                        VerificationMode.NONE, Optional.empty(), false, null, organization);
                String shareId = created.state().shareId();
                var before = grantSnapshot(shareId);
                ShareCommandCaller selected = new ShareCommandCaller(base.account, organization ? Optional.of(base.org) : Optional.empty());
                var barrier = new java.util.concurrent.CyclicBarrier(2);
                var seam = spy(repository);
                doAnswer(invocation -> {
                    barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
                    return invocation.callRealMethod();
                }).when(seam).acquireCreatorWorkspaceLocks(any(), eq(selected));
                var concurrent = service(context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class), seam);
                var executor = Executors.newFixedThreadPool(2);
                executions.clear();
                try {
                    var first = executor.submit(() -> revokeOutcome(concurrent, shareId, "first", selected));
                    var second = executor.submit(() -> revokeOutcome(concurrent, shareId, sameKey ? "first" : "second", selected));
                    Object firstResult = first.get(20, java.util.concurrent.TimeUnit.SECONDS);
                    Object secondResult = second.get(20, java.util.concurrent.TimeUnit.SECONDS);
                    if (sameKey) {
                        assertThat(firstResult).isInstanceOf(CustomerShareCommandResult.Revoked.class).isEqualTo(secondResult);
                    } else {
                        assertThat(List.of(firstResult, secondResult).stream().filter(CustomerShareCommandResult.Revoked.class::isInstance)).hasSize(1);
                        assertThat(List.of(firstResult, secondResult)).contains(ShareCommandException.Code.SHARE_REVOKED);
                    }
                } finally {
                    executor.shutdownNow();
                    assertThat(executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                }
                for (var operation : executions.stream().collect(java.util.stream.Collectors.groupingBy(Execution::pid)).values()) {
                    assertRevokeExecution(selected, operation, operation.stream().anyMatch(execution -> execution.sql().startsWith("INSERT")));
                }
                assertThat(executions.stream().map(Execution::pid).distinct()).hasSize(2);
                assertThat(executions.stream().filter(execution -> execution.sql().startsWith("UPDATE sharing.evidence_share_grant"))).hasSize(1);
                assertRevokeHead(before, grantSnapshot(shareId));
                assertRevokeCounts((UUID) before.get("id"), 1);
                String winner;
                try (Connection admin = base.admin()) {
                    String digest = JsonMapper.builder().build().readTree(DSL.using(admin).fetchOne(
                            "SELECT payload_redacted FROM sharing.share_access_event WHERE share_grant_id = ?", before.get("id"))
                            .get(0, JSONB.class).data()).path("actionRequestDigest").asString();
                    winner = digest.equals(DeterministicShareCommandProviders.digest("first")) ? "first" : "second";
                }
                fails(() -> service.execute(new CustomerShareCommand.Revoke(shareId, winner, Optional.empty(), null, "changed"), selected),
                        ShareCommandException.Code.SHARE_TRANSITION_CONFLICT);
                assertRevokeCounts((UUID) before.get("id"), 1);
            }
        }
    }

    @Test
    void revokeFailureAtEachFactAppendRollsBackAllEffects() throws Exception {
        var normal = context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class);
        CustomerShareCommandService real = service;
        for (String boundary : List.of("before-event", "after-event", "before-update", "after-update", "before-outbox", "after-outbox",
                "before-audit", "after-audit", "commit")) {
            var created = (CustomerShareCommandResult.Created) create("rollback-" + boundary, VerificationMode.NONE, Optional.empty());
            String shareId = created.state().shareId();
            var before = grantSnapshot(shareId);
            UUID grantId = (UUID) before.get("id");
            List<String> durable = revokeFacts(grantId);
            var seam = spy(repository);
            if (boundary.contains("event")) doAnswer(invocation -> {
                if (boundary.equals("after-event")) invocation.callRealMethod();
                throw new IllegalStateException("TEST_INTERNAL_CANARY");
            }).when(seam).appendAccessEvent(any(), any());
            if (boundary.endsWith("update")) doAnswer(invocation -> {
                if (boundary.equals("after-update")) invocation.callRealMethod();
                throw new IllegalStateException("TEST_INTERNAL_CANARY");
            }).when(seam).transitionGrant(any(), any(), anyInt(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), any());
            if (boundary.endsWith("outbox")) doAnswer(invocation -> {
                if (boundary.equals("after-outbox")) invocation.callRealMethod();
                throw new IllegalStateException("TEST_INTERNAL_CANARY");
            }).when(seam).appendOutbox(any(), any());
            if (boundary.endsWith("audit")) doAnswer(invocation -> {
                if (boundary.equals("after-audit")) invocation.callRealMethod();
                throw new IllegalStateException("TEST_INTERNAL_CANARY");
            }).when(seam).appendAudit(any(), any());
            var manager = context.getBean("sharingCommandTransactionManager", DataSourceTransactionManager.class);
            var pool = context.getBean("sharingCommandDataSource", HikariDataSource.class);
            var connectionProvider = dsl.configuration().connectionProvider();
            AtomicInteger commits = new AtomicInteger();
            AtomicInteger rollbacks = new AtomicInteger();
            if (boundary.equals("commit")) {
                var failing = new org.springframework.jdbc.datasource.DelegatingDataSource(pool) {
                    @Override
                    public Connection getConnection() throws java.sql.SQLException {
                        Connection connection = super.getConnection();
                        return (Connection) java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                                (proxy, method, arguments) -> {
                                    if (method.getName().equals("commit")) {
                                        commits.incrementAndGet(); throw new java.sql.SQLException("TEST_COMMIT_CANARY", "08006");
                                    }
                                    if (method.getName().equals("rollback")) rollbacks.incrementAndGet();
                                    try { return method.invoke(connection, arguments); }
                                    catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                                });
                    }
                };
                manager.setDataSource(failing);
                dsl.configuration().set(new org.jooq.impl.DataSourceConnectionProvider(
                        new org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy(failing)));
            }
            service = service(normal, seam);
            executions.clear();
            try {
                fails(() -> revoke(shareId, "retry-key", null, null), ShareCommandException.Code.INTERNAL);
                if (boundary.equals("commit")) {
                    assertThat(commits).hasValue(1);
                    assertThat(rollbacks).hasValue(1);
                    assertThat(executions).anyMatch(execution -> execution.sql().contains("audit.append_fact"));
                }
            } finally {
                manager.setDataSource(pool);
                dsl.configuration().set(connectionProvider);
                service = real;
            }
            assertThat(grantSnapshot(shareId)).isEqualTo(before);
            assertThat(revokeFacts(grantId)).isEqualTo(durable);
            assertRevokeCounts(grantId, 0);
            revoke(shareId, "retry-key", null, null);
            assertRevokeHead(before, grantSnapshot(shareId));
            assertRevokeCounts(grantId, 1);
        }
    }

    @Test
    void rotateWritesRegisteredEventAuditAndOutboxAtomically() throws Exception {
        try (Connection admin = base.admin(); Statement statement = admin.createStatement();
                ResultSet version = statement.executeQuery("SELECT version(), current_setting('server_version')")) {
            assertThat(version.next()).isTrue();
            assertThat(version.getString(2)).startsWith("16.");
            System.out.println("PA14 PostgreSQL version: " + version.getString(1) + "; server_version=" + version.getString(2));
        }
        CustomerShareCommandService real = service;
        for (Integer witness : new Integer[] {null, 1}) {
            var created = (CustomerShareCommandResult.Created) create("rotate-facts-" + witness, VerificationMode.EXTRACTION_CODE, Optional.empty());
            String shareId = created.state().shareId();
            prepareGrant(shareId, "LOCKED", 4);
            var before = grantSnapshot(shareId);
            UUID grantId = (UUID) before.get("id");
            UUID trace = UUID.randomUUID();
            Optional<UUID> selectedTrace = witness == null ? Optional.empty() : Optional.of(trace);
            var sampled = new java.util.concurrent.atomic.AtomicReference<Instant>();
            var seam = spy(repository);
            doAnswer(invocation -> {
                Instant value = (Instant) invocation.callRealMethod();
                sampled.set(value);
                return value;
            }).when(seam).sampleDatabaseTime(any());
            var policyProbe = mock(ShareRotationPolicyProvider.class);
            when(policyProbe.evaluate(any())).thenAnswer(invocation -> {
                assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT") || execution.sql().startsWith("UPDATE"));
                assertThat(invocation.getArgument(0, ShareRotationPolicyProvider.RotationPolicyRequest.class)).isEqualTo(
                        new ShareRotationPolicyProvider.RotationPolicyRequest(caller(), grantId, VerificationMode.EXTRACTION_CODE,
                                com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.LOCKED, 1, 4));
                return DeterministicShareCommandProviders.rotationPolicy().evaluate(invocation.getArgument(0));
            });
            var codeProbe = mock(ShareExtractionCodeProvider.class);
            when(codeProbe.generate(any(), anyInt())).thenAnswer(invocation -> {
                verify(policyProbe).evaluate(any());
                assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT") || execution.sql().startsWith("UPDATE"));
                return context.getBean(ShareExtractionCodeProvider.class).generate(invocation.getArgument(0), invocation.getArgument(1));
            });
            service = rotationService(seam, policyProbe, codeProbe);
            int oldTokens = tokens.get();
            int oldCodes = codes.get();
            executions.clear();
            var rotated = (CustomerShareCommandResult.Rotated) service.execute(new CustomerShareCommand.Rotate(shareId, " raw ключ,😀 ", selectedTrace, witness), caller());
            assertThat(rotated.status().name()).isEqualTo("ACTIVE");
            assertThat(rotated.codeGeneration()).isEqualTo(2);
            assertThat(rotated.revocationGeneration()).isEqualTo(5);
            assertThat(rotated.extractionCode()).isNotEqualTo(created.extractionCode().orElseThrow());
            assertThat(tokens).hasValue(oldTokens);
            assertThat(codes).hasValue(oldCodes + 1);
            var after = grantSnapshot(shareId);
            assertRotateHead(before, after);
            assertRevokeExecution(caller());
            var update = executions.stream().filter(execution -> execution.sql().startsWith("UPDATE sharing.evidence_share_grant")).findFirst().orElseThrow();
            assertThat(update.sql().contains("AND code_generation = ?")).isEqualTo(witness != null);
            assertThat(update.bindings().subList(0, 9)).containsExactly("ACTIVE", after.get("code_salt"), after.get("code_hash"), 2, 5,
                    after.get("revision"), grantId, before.get("revision"), "LOCKED");
            assertRotationFacts(before, " raw ключ,😀 ", selectedTrace, sampled.get(), rotated.extractionCode(), created.capabilityToken());
            assertRevokeCounts(grantId, 1);
            service = real;
        }
        var created = (CustomerShareCommandResult.Created) create("rotate-refusal", VerificationMode.EXTRACTION_CODE, Optional.empty());
        String shareId = created.state().shareId();
        prepareGrant(shareId, "LOCKED", 0);
        var before = grantSnapshot(shareId);
        UUID grantId = (UUID) before.get("id");
        List<String> durable = revokeFacts(grantId);
        var policyProbe = mock(ShareRotationPolicyProvider.class);
        var codeProbe = mock(ShareExtractionCodeProvider.class);
        service = rotationService(repository, policyProbe, codeProbe);
        for (var outcome : java.util.Arrays.<ProviderResult<ShareRotationPolicyProvider.RotationPolicyDecision>>asList(
                new ProviderResult.Denied<>(ShareCommandException.Code.INTERNAL),
                new ProviderResult.Allowed<>(ShareRotationPolicyProvider.RotationPolicyDecision.DENY),
                new ProviderResult.Unavailable<>(ShareCommandException.Code.INTERNAL), null,
                mock(ProviderResult.Allowed.class))) {
            doReturn(outcome).when(policyProbe).evaluate(any());
            fails(() -> rotate(shareId, "retry-key", null), outcome instanceof ProviderResult.Denied<?>
                    || outcome instanceof ProviderResult.Allowed<?> allowed && allowed.value() == ShareRotationPolicyProvider.RotationPolicyDecision.DENY
                    ? ShareCommandException.Code.FORBIDDEN : ShareCommandException.Code.SERVICE_UNAVAILABLE);
            assertThat(grantSnapshot(shareId)).isEqualTo(before);
            assertThat(revokeFacts(grantId)).isEqualTo(durable);
        }
        when(policyProbe.evaluate(any())).thenThrow(new IllegalStateException("POLICY_CANARY"));
        fails(() -> rotate(shareId, "retry-key", null), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        verifyNoInteractions(codeProbe);
        doReturn(new ProviderResult.Allowed<>(ShareRotationPolicyProvider.RotationPolicyDecision.ALLOW)).when(policyProbe).evaluate(any());
        for (var material : List.of(new ExtractionCodeMaterial("", "valid_salt_abcdefghijklmnop", "b".repeat(64)),
                new ExtractionCodeMaterial("code", "short", "b".repeat(64)),
                new ExtractionCodeMaterial("code", "valid_salt_abcdefghijklmnop", "INVALID"),
                new ExtractionCodeMaterial("code", (String) before.get("code_salt"), "b".repeat(64)),
                new ExtractionCodeMaterial("code", "valid_salt_abcdefghijklmnop", (String) before.get("code_hash")))) {
            when(codeProbe.generate(any(), anyInt())).thenReturn(new ProviderResult.Allowed<>(material));
            executions.clear();
            fails(() -> rotate(shareId, "retry-key", null), ShareCommandException.Code.SERVICE_UNAVAILABLE);
            assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT") || execution.sql().startsWith("UPDATE"));
            assertThat(grantSnapshot(shareId)).isEqualTo(before);
            assertThat(revokeFacts(grantId)).isEqualTo(durable);
        }
        for (var outcome : java.util.Arrays.<ProviderResult<ExtractionCodeMaterial>>asList(null,
                new ProviderResult.Denied<>(ShareCommandException.Code.FORBIDDEN), new ProviderResult.Unavailable<>(ShareCommandException.Code.INTERNAL),
                mock(ProviderResult.Allowed.class))) {
            doReturn(outcome).when(codeProbe).generate(any(), anyInt());
            fails(() -> rotate(shareId, "retry-key", null), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        }
        when(codeProbe.generate(any(), anyInt())).thenThrow(new IllegalStateException("CODE_CANARY"));
        fails(() -> rotate(shareId, "retry-key", null), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        assertThat(grantSnapshot(shareId)).isEqualTo(before);
        assertThat(revokeFacts(grantId)).isEqualTo(durable);
        try (var missingPolicy = new AnnotationConfigApplicationContext()) {
            missingPolicy.getEnvironment().setActiveProfiles("persistence");
            missingPolicy.getEnvironment().getPropertySources().addFirst(new MapPropertySource("missing-policy", java.util.Map.of(
                    "onecall.sharing-command.enabled", "true", "onecall.sharing-command.datasource.url", base.url,
                    "onecall.sharing-command.datasource.username", login, "onecall.sharing-command.datasource.password", base.password)));
            missingPolicy.register(SharingCommandPersistenceConfiguration.class, ShareProjectionReaderConfiguration.class);
            missingPolicy.refresh();
            fails(() -> missingPolicy.getBean(CustomerShareCommandService.class).execute(new CustomerShareCommand.Rotate(
                    shareId, "retry-key", Optional.empty(), null), caller()), ShareCommandException.Code.SERVICE_UNAVAILABLE);
            assertThat(grantSnapshot(shareId)).isEqualTo(before);
            assertThat(revokeFacts(grantId)).isEqualTo(durable);
        }
        service = real;
        assertThat(rotate(shareId, "retry-key", null)).isInstanceOf(CustomerShareCommandResult.Rotated.class);
        service = rotationService(repository, policyProbe, codeProbe);
        doReturn(new ProviderResult.Unavailable<>(ShareCommandException.Code.SERVICE_UNAVAILABLE)).when(policyProbe).evaluate(any());
        clearInvocations(policyProbe, codeProbe);
        assertThat(rotate(shareId, "retry-key", 99)).isInstanceOf(CustomerShareCommandResult.RotationReplayed.class);
        verifyNoInteractions(policyProbe, codeProbe);
        assertRevokeCounts(grantId, 1);
    }

    @Test
    void rotateReplayReturnsNoCodeOrCapabilityToken() throws Exception {
        var created = (CustomerShareCommandResult.Created) create("rotate-replay", VerificationMode.EXTRACTION_CODE, Optional.empty());
        String shareId = created.state().shareId();
        prepareGrant(shareId, "LOCKED", 0);
        var rotated = (CustomerShareCommandResult.Rotated) rotate(shareId, "original-key", 1);
        var normal = context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class);
        CustomerShareCommandService real = service;
        for (String later : List.of("ACTIVE", "LOCKED", "ROTATED", "REVOKED", "ELAPSED")) {
            service = real;
            if (later.equals("LOCKED")) prepareGrant(shareId, "LOCKED", 1);
            if (later.equals("ROTATED")) rotate(shareId, "later-key", 2);
            if (later.equals("REVOKED")) revoke(shareId, "later-revoke", null, null);
            var seam = spy(repository);
            if (later.equals("ELAPSED")) doReturn(expiry.plusSeconds(1)).when(seam).sampleDatabaseTime(any());
            service = service(normal, seam);
            var before = grantSnapshot(shareId);
            UUID grantId = (UUID) before.get("id");
            List<String> durable = revokeFacts(grantId);
            int oldTokens = tokens.get();
            int oldCodes = codes.get();
            rotationPolicy = new ProviderResult.Denied<>(ShareCommandException.Code.FORBIDDEN);
            for (Integer witness : new Integer[] {null, 0, 1, 2, 99}) {
                executions.clear();
                var replay = (CustomerShareCommandResult.RotationReplayed) service.execute(new CustomerShareCommand.Rotate(
                        shareId, "original-key", Optional.of(UUID.randomUUID()), witness), caller());
                assertThat(replay).isEqualTo(new CustomerShareCommandResult.RotationReplayed(rotated.shareId(), rotated.verificationMode(),
                        rotated.status(), rotated.codeGeneration(), rotated.revocationGeneration()));
                assertThat(JsonMapper.builder().build().writeValueAsString(replay)).doesNotContain("extractionCode", "capabilityToken", rotated.extractionCode());
                assertThat(grantSnapshot(shareId)).isEqualTo(before);
                assertThat(revokeFacts(grantId)).isEqualTo(durable);
                assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT") || execution.sql().startsWith("UPDATE")
                        || execution.sql().contains("outbox_append") || execution.sql().contains("append_fact"));
            }
            assertThat(tokens).hasValue(oldTokens);
            assertThat(codes).hasValue(oldCodes);
            rotationPolicy = new ProviderResult.Allowed<>(ShareRotationPolicyProvider.RotationPolicyDecision.ALLOW);
        }
        fails(() -> service.execute(new CustomerShareCommand.Rotate(shareId, "original-key", Optional.empty(), null),
                new ShareCommandCaller(base.account, Optional.of(base.org))), ShareCommandException.Code.NOT_FOUND);
        var conflict = spy(repository);
        doAnswer(invocation -> {
            var persisted = repository.findTransition(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)).orElseThrow();
            return Optional.of(new SharingCommandRepository.PersistedTransition(persisted.shareGrantId(), persisted.eventId(), persisted.actionType(),
                    persisted.actionRequestDigest(), "a".repeat(64), persisted.status(), persisted.codeGeneration(), persisted.revocationGeneration(),
                    persisted.revision(), persisted.occurredAt()));
        }).when(conflict).findTransition(any(), any(), any());
        service = service(normal, conflict);
        var beforeConflict = grantSnapshot(shareId);
        var factsBeforeConflict = revokeFacts((UUID) beforeConflict.get("id"));
        fails(() -> rotate(shareId, "original-key", null), ShareCommandException.Code.SHARE_TRANSITION_CONFLICT);
        assertThat(grantSnapshot(shareId)).isEqualTo(beforeConflict);
        assertThat(revokeFacts((UUID) beforeConflict.get("id"))).isEqualTo(factsBeforeConflict);
        for (String corruption : List.of("hash", "extra", "action", "status", "counter", "digest", "fraction", "event")) {
            service = real;
            String key = "corrupt-" + corruption;
            var candidate = (CustomerShareCommandResult.Created) create(key, VerificationMode.EXTRACTION_CODE, Optional.empty());
            prepareGrant(candidate.state().shareId(), "LOCKED", 0);
            var before = grantSnapshot(candidate.state().shareId());
            UUID grantId = (UUID) before.get("id");
            var mapper = JsonMapper.builder().build();
            var identity = mapper.createObjectNode().put("shareGrantId", grantId.toString()).put("actionType", "CODE_ROTATE");
            identity.set("sourceIdentity", mapper.createObjectNode().put("kind", "REQUEST").put("actionRequestId", key));
            String eventId = "share-transition:" + DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(identity));
            var payload = mapper.createObjectNode().put("actionType", "CODE_ROTATE").put("actionRequestDigest", "b".repeat(64))
                    .put("actionFingerprint", "c".repeat(64)).put("status", "ACTIVE").put("codeGeneration", 2).put("revocationGeneration", 1).put("revision", 3);
            switch (corruption) {
                case "extra" -> payload.put("extra", "x");
                case "action" -> payload.put("actionType", "REVOKE");
                case "status" -> payload.put("status", "LOCKED");
                case "counter" -> payload.put("codeGeneration", 0);
                case "digest" -> payload.put("actionRequestDigest", "INVALID");
                case "fraction" -> payload.put("revision", 1.5);
                default -> { }
            }
            var durable = revokeFacts(grantId);
            var malformed = spy(repository);
            doAnswer(invocation -> {
                DSLContext transaction = invocation.getArgument(0);
                repository.appendAccessEvent(transaction, new SharingCommandRepository.AccessEvent(UUID.randomUUID(), grantId, Optional.empty(),
                        "SHARE_ACCESS", eventId, corruption.equals("event") ? "REVOKED" : "CODE_ROTATED", "SUCCESS", base.timestamp,
                        Optional.empty(), JSONB.valueOf(payload.toString()), corruption.equals("hash") ? "a".repeat(64)
                                : DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(payload))));
                return repository.findTransition(transaction, grantId, eventId);
            }).when(malformed).findTransition(any(), eq(grantId), eq(eventId));
            service = service(normal, malformed);
            int oldCodes = codes.get();
            fails(() -> rotate(candidate.state().shareId(), key, null), ShareCommandException.Code.SERVICE_UNAVAILABLE);
            assertThat(grantSnapshot(candidate.state().shareId())).isEqualTo(before);
            assertThat(revokeFacts(grantId)).isEqualTo(durable);
            assertThat(codes).hasValue(oldCodes);
        }
    }

    @Test
    void rotateFailureAtEachFactAppendRollsBackAllEffects() throws Exception {
        var normal = context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class);
        CustomerShareCommandService real = service;
        for (String boundary : List.of("before-event", "after-event", "before-update", "after-update", "before-outbox", "after-outbox",
                "before-audit", "after-audit", "commit")) {
            var created = (CustomerShareCommandResult.Created) create("rollback-" + boundary, VerificationMode.EXTRACTION_CODE, Optional.empty());
            String shareId = created.state().shareId();
            prepareGrant(shareId, "LOCKED", 2);
            var before = grantSnapshot(shareId);
            UUID grantId = (UUID) before.get("id");
            List<String> durable = revokeFacts(grantId);
            var seam = spy(repository);
            if (boundary.contains("event")) doAnswer(invocation -> {
                if (boundary.equals("after-event")) invocation.callRealMethod();
                throw new IllegalStateException("TEST_INTERNAL_CANARY");
            }).when(seam).appendAccessEvent(any(), any());
            if (boundary.endsWith("update")) doAnswer(invocation -> {
                if (boundary.equals("after-update")) invocation.callRealMethod();
                throw new IllegalStateException("TEST_INTERNAL_CANARY");
            }).when(seam).transitionGrant(any(), any(), anyInt(), any(), any(), any(), any(), anyInt(), anyInt(), anyInt(), any());
            if (boundary.endsWith("outbox")) doAnswer(invocation -> {
                if (boundary.equals("after-outbox")) invocation.callRealMethod();
                throw new IllegalStateException("TEST_INTERNAL_CANARY");
            }).when(seam).appendOutbox(any(), any());
            if (boundary.endsWith("audit")) doAnswer(invocation -> {
                if (boundary.equals("after-audit")) invocation.callRealMethod();
                throw new IllegalStateException("TEST_INTERNAL_CANARY");
            }).when(seam).appendAudit(any(), any());
            var manager = context.getBean("sharingCommandTransactionManager", DataSourceTransactionManager.class);
            var pool = context.getBean("sharingCommandDataSource", HikariDataSource.class);
            var connectionProvider = dsl.configuration().connectionProvider();
            AtomicInteger commits = new AtomicInteger();
            AtomicInteger rollbacks = new AtomicInteger();
            if (boundary.equals("commit")) {
                var failing = new org.springframework.jdbc.datasource.DelegatingDataSource(pool) {
                    @Override
                    public Connection getConnection() throws java.sql.SQLException {
                        Connection connection = super.getConnection();
                        return (Connection) java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                                (proxy, method, arguments) -> {
                                    if (method.getName().equals("commit")) {
                                        commits.incrementAndGet(); throw new java.sql.SQLException("TEST_COMMIT_CANARY", "08006");
                                    }
                                    if (method.getName().equals("rollback")) rollbacks.incrementAndGet();
                                    try { return method.invoke(connection, arguments); }
                                    catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                                });
                    }
                };
                manager.setDataSource(failing);
                dsl.configuration().set(new org.jooq.impl.DataSourceConnectionProvider(
                        new org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy(failing)));
            }
            service = service(normal, seam);
            executions.clear();
            try {
                fails(() -> rotate(shareId, "retry-key", null), ShareCommandException.Code.INTERNAL);
                if (boundary.equals("commit")) {
                    assertThat(commits).hasValue(1);
                    assertThat(rollbacks).hasValue(1);
                    assertThat(executions).anyMatch(execution -> execution.sql().contains("audit.append_fact"));
                }
            } finally {
                manager.setDataSource(pool);
                dsl.configuration().set(connectionProvider);
                service = real;
            }
            assertThat(grantSnapshot(shareId)).isEqualTo(before);
            assertThat(revokeFacts(grantId)).isEqualTo(durable);
            assertRevokeCounts(grantId, 0);
            rotate(shareId, "retry-key", null);
            assertRotateHead(before, grantSnapshot(shareId));
            assertRevokeCounts(grantId, 1);
            assertThat(rotate(shareId, "retry-key", 99)).isInstanceOf(CustomerShareCommandResult.RotationReplayed.class);
            assertRevokeCounts(grantId, 1);
        }
    }

    @Test
    void rotateConcurrentActionsProduceOneTransition() throws Exception {
        for (boolean organization : List.of(false, true)) {
            for (boolean sameKey : List.of(false, true)) {
                var created = (CustomerShareCommandResult.Created) create("concurrent-" + organization + sameKey,
                        VerificationMode.EXTRACTION_CODE, Optional.empty(), false, null, organization);
                String shareId = created.state().shareId();
                prepareGrant(shareId, "LOCKED", 2);
                var before = grantSnapshot(shareId);
                ShareCommandCaller selected = new ShareCommandCaller(base.account, organization ? Optional.of(base.org) : Optional.empty());
                var barrier = new java.util.concurrent.CyclicBarrier(2);
                var seam = spy(repository);
                doAnswer(invocation -> {
                    invocation.callRealMethod();
                    invocation.<DSLContext>getArgument(0).connection(connection -> {
                        try (Statement statement = connection.createStatement()) {
                            statement.execute("SET LOCAL lock_timeout = '10s'");
                            statement.execute("SET LOCAL statement_timeout = '15s'");
                        }
                    });
                    return null;
                }).when(seam).assertCommandRole(any());
                doAnswer(invocation -> {
                    barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
                    return invocation.callRealMethod();
                }).when(seam).acquireCreatorWorkspaceLocks(any(), eq(selected));
                var concurrent = service(context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class), seam);
                var executor = Executors.newFixedThreadPool(2);
                executions.clear();
                try {
                    var first = executor.submit(() -> rotateOutcome(concurrent, shareId, "first", selected));
                    var second = executor.submit(() -> rotateOutcome(concurrent, shareId, sameKey ? "first" : "second", selected));
                    Object firstResult = first.get(20, java.util.concurrent.TimeUnit.SECONDS);
                    Object secondResult = second.get(20, java.util.concurrent.TimeUnit.SECONDS);
                    if (sameKey) {
                        assertThat(List.of(firstResult, secondResult).stream().filter(CustomerShareCommandResult.Rotated.class::isInstance)).hasSize(1);
                        assertThat(List.of(firstResult, secondResult).stream().filter(CustomerShareCommandResult.RotationReplayed.class::isInstance)).hasSize(1);
                    } else {
                        assertThat(List.of(firstResult, secondResult).stream().filter(CustomerShareCommandResult.Rotated.class::isInstance)).hasSize(1);
                        assertThat(List.of(firstResult, secondResult)).contains(ShareCommandException.Code.SHARE_TRANSITION_CONFLICT);
                    }
                } finally {
                    executor.shutdownNow();
                    assertThat(executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                }
                for (var operation : executions.stream().collect(java.util.stream.Collectors.groupingBy(Execution::pid)).values()) {
                    assertRevokeExecution(selected, operation, operation.stream().anyMatch(execution -> execution.sql().startsWith("INSERT")));
                }
                assertThat(executions.stream().map(Execution::pid).distinct()).hasSize(2);
                assertThat(executions.stream().filter(execution -> execution.sql().startsWith("UPDATE sharing.evidence_share_grant"))).hasSize(1);
                assertRotateHead(before, grantSnapshot(shareId));
                assertRevokeCounts((UUID) before.get("id"), 1);
            }
        }
    }

    @Test
    void rotateRequiresExactLockedStateAndGenerationWitness() throws Exception {
        var normal = context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class);
        CustomerShareCommandService real = service;
        var policyProbe = mock(ShareRotationPolicyProvider.class);
        var codeProbe = mock(ShareExtractionCodeProvider.class);
        when(policyProbe.evaluate(any())).thenAnswer(invocation -> DeterministicShareCommandProviders.rotationPolicy().evaluate(invocation.getArgument(0)));
        when(codeProbe.generate(any(), anyInt())).thenAnswer(invocation -> DeterministicShareCommandProviders.codes().generate(invocation.getArgument(0), invocation.getArgument(1)));
        for (VerificationMode mode : VerificationMode.values()) {
            for (String status : List.of("ACTIVE", "LOCKED", "REVOKED", "EXPIRED")) {
                for (int time : new int[] {-1, 0, 1}) {
                    for (Integer witness : new Integer[] {null, 1, 0}) {
                        String key = mode + status + time + witness;
                        service = real;
                        if (status.equals("EXPIRED")) {
                            var expired = spy(repository);
                            doAnswer(invocation -> {
                                ShareGrant proposed = invocation.getArgument(1);
                                return repository.insertGrantReturning(invocation.getArgument(0), new ShareGrant(proposed.id(), proposed.shareId(),
                                        proposed.capabilityTokenHash(), proposed.evidenceId(), proposed.createdByAccountId(), proposed.grantOwnerOrganizationId(),
                                        proposed.verificationMode(), proposed.codeSalt(), proposed.codeHash(), proposed.codeGeneration(),
                                        proposed.validFrom().minusSeconds(7200), proposed.validFrom().minusSeconds(3600), proposed.maxAdmissions(),
                                        proposed.admissionsUsed(), proposed.downloadAllowed(), proposed.contentSnapshot(), proposed.contentSha256(),
                                        proposed.status(), proposed.revocationGeneration(), proposed.revision()));
                            }).when(expired).insertGrantReturning(any(), any());
                            service = service(normal, expired);
                        }
                        var created = (CustomerShareCommandResult.Created) create("rotate-matrix-" + key, mode, Optional.empty());
                        String shareId = created.state().shareId();
                        prepareGrant(shareId, status, 3);
                        var before = grantSnapshot(shareId);
                        var seam = spy(repository);
                        Instant expires = ((java.time.OffsetDateTime) before.get("expires_at")).toInstant();
                        doReturn(expires.plusSeconds(time)).when(seam).sampleDatabaseTime(any());
                        service = rotationService(seam, policyProbe, codeProbe);
                        var selected = new ShareCommandCaller(base.account, mode == VerificationMode.ORGANIZATION_MEMBER ? Optional.of(base.org) : Optional.empty());
                        var command = new CustomerShareCommand.Rotate(shareId, key, Optional.empty(), witness);
                        clearInvocations(policyProbe, codeProbe);
                        executions.clear();
                        if (mode == VerificationMode.EXTRACTION_CODE && status.equals("LOCKED") && time < 0 && (witness == null || witness == 1)) {
                            assertThat(service.execute(command, selected)).isInstanceOf(CustomerShareCommandResult.Rotated.class);
                            assertRotateHead(before, grantSnapshot(shareId));
                            assertRevokeCounts((UUID) before.get("id"), 1);
                        } else {
                            fails(() -> service.execute(command, selected), ShareCommandException.Code.SHARE_TRANSITION_CONFLICT);
                            verifyNoInteractions(policyProbe, codeProbe);
                            assertThat(grantSnapshot(shareId)).isEqualTo(before);
                            assertRevokeCounts((UUID) before.get("id"), 0);
                            assertThat(executions).noneMatch(execution -> execution.sql().startsWith("INSERT") || execution.sql().startsWith("UPDATE"));
                        }
                    }
                }
            }
        }
        service = real;
        var created = (CustomerShareCommandResult.Created) create("rotate-faults", VerificationMode.EXTRACTION_CODE, Optional.empty());
        String shareId = created.state().shareId();
        prepareGrant(shareId, "LOCKED", 0);
        var head = grantSnapshot(shareId);
        var durable = revokeFacts((UUID) head.get("id"));
        for (String sql : List.of("SET LOCAL ROLE", "SELECT current_user", "pg_advisory_xact_lock", "customer_iam.customer_account", "FOR UPDATE",
                "SELECT clock_timestamp()", "FROM sharing.share_access_event")) {
            failOn = sql;
            try { fails(() -> rotate(shareId, "fault", null), ShareCommandException.Code.SERVICE_UNAVAILABLE); }
            finally { failOn = null; }
            assertThat(grantSnapshot(shareId)).isEqualTo(head);
            assertThat(revokeFacts((UUID) head.get("id"))).isEqualTo(durable);
        }
        for (Instant badClock : new Instant[] {null, Instant.parse("2026-11-05T00:00:00.123456789Z")}) {
            var seam = spy(repository);
            doReturn(badClock).when(seam).sampleDatabaseTime(any());
            service = service(normal, seam);
            fails(() -> rotate(shareId, "fault", null), ShareCommandException.Code.SERVICE_UNAVAILABLE);
        }
        service = real;
        fails(() -> rotate("opaque-absent", "fault", null), ShareCommandException.Code.NOT_FOUND);
        for (ShareCommandCaller wrong : List.of(new ShareCommandCaller(base.other, Optional.empty()), new ShareCommandCaller(base.account, Optional.of(base.org))))
            fails(() -> service.execute(new CustomerShareCommand.Rotate(shareId, "fault", Optional.empty(), null), wrong), ShareCommandException.Code.NOT_FOUND);
        for (ShareCommandCaller inactive : List.of(new ShareCommandCaller(UUID.randomUUID(), Optional.empty()),
                new ShareCommandCaller(base.account, Optional.of(UUID.randomUUID())), new ShareCommandCaller(base.other, Optional.of(base.org2)))) {
            executions.clear();
            fails(() -> service.execute(new CustomerShareCommand.Rotate(shareId, "fault", Optional.empty(), null), inactive), ShareCommandException.Code.FORBIDDEN);
            assertThat(executions).noneMatch(execution -> execution.sql().contains("FROM sharing.evidence_share_grant"));
        }
        for (Integer witness : new Integer[] {null, 1}) {
            for (String miss : List.of("revision", "status", "code")) {
                if (witness == null && miss.equals("code")) continue;
                for (String reread : List.of("same", "changed", "absent")) {
                    var seam = spy(repository);
                    AtomicInteger reads = new AtomicInteger();
                    doAnswer(invocation -> {
                        Optional<ShareGrant> found = repository.lockGrant(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
                        if (reads.incrementAndGet() == 1 || reread.equals("same")) return found;
                        if (reread.equals("absent")) return Optional.empty();
                        ShareGrant grant = found.orElseThrow();
                        return Optional.of(new ShareGrant(grant.id(), grant.shareId(), grant.capabilityTokenHash(), grant.evidenceId(), grant.createdByAccountId(),
                                grant.grantOwnerOrganizationId(), grant.verificationMode(), grant.codeSalt(), grant.codeHash(), grant.codeGeneration() + 1,
                                grant.validFrom(), grant.expiresAt(), grant.maxAdmissions(), grant.admissionsUsed(), grant.downloadAllowed(), grant.contentSnapshot(),
                                grant.contentSha256(), com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.REVOKED, grant.revocationGeneration() + 1, grant.revision() + 1));
                    }).when(seam).lockGrant(any(), eq(shareId), eq(caller()));
                    service = service(normal, seam);
                    executions.clear();
                    rotationMiss = miss;
                    try { fails(() -> rotate(shareId, "zero-row", witness), ShareCommandException.Code.SHARE_TRANSITION_CONFLICT); }
                    finally { rotationMiss = null; }
                    assertThat(reads).hasValue(2);
                    verify(seam).transitionGrant(any(), eq((UUID) head.get("id")), eq((Integer) head.get("revision")),
                            eq(com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.LOCKED), eq(Optional.empty()), eq(Optional.ofNullable(witness)),
                            eq(com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.ACTIVE), eq(((Integer) head.get("revision")) + 1),
                            eq(2), eq(1), any());
                    verify(seam).appendAccessEvent(any(), any());
                    verify(seam, never()).appendOutbox(any(), any());
                    verify(seam, never()).appendAudit(any(), any());
                    verify(seam).findTransition(any(), any(), any());
                    assertThat(executions.stream().filter(execution -> execution.sql().startsWith("UPDATE sharing.evidence_share_grant"))).hasSize(1);
                    assertThat(grantSnapshot(shareId)).isEqualTo(head);
                    assertThat(revokeFacts((UUID) head.get("id"))).isEqualTo(durable);
                }
            }
        }
        service = real;
        var organizationCreated = (CustomerShareCommandResult.Created) create("rotate-organization", VerificationMode.EXTRACTION_CODE, Optional.empty(), false, null, true);
        String organizationShare = organizationCreated.state().shareId();
        prepareGrant(organizationShare, "LOCKED", 0);
        for (ShareCommandCaller wrong : List.of(caller(), new ShareCommandCaller(base.account, Optional.of(base.org2)), new ShareCommandCaller(base.other, Optional.of(base.org))))
            fails(() -> service.execute(new CustomerShareCommand.Rotate(organizationShare, "fault", Optional.empty(), null), wrong), ShareCommandException.Code.NOT_FOUND);
        for (String boundary : List.of("account", "organization", "membership")) {
            try (Connection admin = base.admin()) {
                if (boundary.equals("account")) SharingQueryFixture.execute(admin,
                        "UPDATE customer_iam.customer_account SET status = 'DISABLED', revision = revision + 1 WHERE id = ?", base.account);
                if (boundary.equals("organization")) SharingQueryFixture.execute(admin,
                        "UPDATE organization_access.organization SET status = 'SUSPENDED', revision = revision + 1 WHERE id = ?", base.org);
                if (boundary.equals("membership")) SharingQueryFixture.execute(admin,
                        "UPDATE organization_access.organization_membership SET status = 'SUSPENDED', revision = revision + 1 WHERE id = ?", base.membership);
            }
            try {
                executions.clear();
                fails(() -> service.execute(new CustomerShareCommand.Rotate(organizationShare, "fault", Optional.empty(), null),
                        new ShareCommandCaller(base.account, Optional.of(base.org))), ShareCommandException.Code.FORBIDDEN);
                assertThat(executions).noneMatch(execution -> execution.sql().contains("FROM sharing.evidence_share_grant"));
            } finally {
                try (Connection admin = base.admin()) {
                    SharingQueryFixture.execute(admin, "UPDATE customer_iam.customer_account SET status = 'ACTIVE', revision = revision + 1 WHERE id = ?", base.account);
                    SharingQueryFixture.execute(admin, "UPDATE organization_access.organization SET status = 'ACTIVE', revision = revision + 1 WHERE id = ?", base.org);
                    SharingQueryFixture.execute(admin, "UPDATE organization_access.organization_membership SET status = 'ACTIVE', revision = revision + 1 WHERE id = ?", base.membership);
                }
            }
        }
        assertThat(grantSnapshot(shareId)).isEqualTo(head);
        assertThat(revokeFacts((UUID) head.get("id"))).isEqualTo(durable);
    }

    private Object rotateOutcome(CustomerShareCommandService selectedService, String shareId, String key, ShareCommandCaller selected) {
        try { return selectedService.execute(new CustomerShareCommand.Rotate(shareId, key, Optional.empty(), null), selected); }
        catch (ShareCommandException failure) { return failure.code(); }
    }

    private CustomerShareCommandResult rotate(String shareId, String key, Integer witness) {
        return service.execute(new CustomerShareCommand.Rotate(shareId, key, Optional.empty(), witness), caller());
    }

    private CustomerShareCommandService rotationService(SharingCommandRepository selectedRepository,
            ShareRotationPolicyProvider selectedPolicy, ShareExtractionCodeProvider selectedCodes) {
        return new DefaultCustomerShareCommandService(context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class),
                dsl, selectedRepository, context.getBean(ShareableEvidenceProjectionReader.class), context.getBean(ShareCreationPolicyProvider.class),
                selectedPolicy, context.getBean(CapabilityTokenProvider.class), selectedCodes,
                context.getBean(DesignatedMobileBindingProvider.class), context.getBean(Sha256Provider.class));
    }

    private void assertRotateHead(java.util.Map<String, Object> before, java.util.Map<String, Object> after) {
        var expected = new java.util.HashMap<>(before);
        expected.put("status", "ACTIVE");
        expected.put("revision", ((Integer) before.get("revision")) + 1);
        expected.put("code_generation", ((Integer) before.get("code_generation")) + 1);
        expected.put("revocation_generation", ((Integer) before.get("revocation_generation")) + 1);
        expected.put("code_salt", after.get("code_salt"));
        expected.put("code_hash", after.get("code_hash"));
        assertThat(after.get("code_salt")).isNotNull().isNotEqualTo(before.get("code_salt"));
        assertThat(after.get("code_hash")).isNotNull().isNotEqualTo(before.get("code_hash"));
        assertThat(after).hasSize(20).isEqualTo(expected);
    }

    private void assertRotationFacts(java.util.Map<String, Object> before, String rawKey, Optional<UUID> trace, Instant occurred,
            String code, String token) throws Exception {
        UUID grantId = (UUID) before.get("id");
        try (Connection admin = base.admin()) {
            DSLContext facts = DSL.using(admin);
            var mapper = JsonMapper.builder().build();
            String requestDigest = DeterministicShareCommandProviders.digest(rawKey);
            var identity = mapper.createObjectNode().put("shareGrantId", grantId.toString()).put("actionType", "CODE_ROTATE");
            identity.set("sourceIdentity", mapper.createObjectNode().put("kind", "REQUEST").put("actionRequestId", rawKey));
            String eventId = "share-transition:" + DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(identity));
            var semantics = mapper.createObjectNode().put("shareGrantId", grantId.toString()).put("actionType", "CODE_ROTATE")
                    .putNull("reasonCode").putNull("newExpiresAt").put("actionRequestDigest", requestDigest);
            String fingerprint = DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(semantics));
            var expected = mapper.createObjectNode().put("actionType", "CODE_ROTATE").put("actionRequestDigest", requestDigest)
                    .put("actionFingerprint", fingerprint).put("status", "ACTIVE").put("codeGeneration", ((Integer) before.get("code_generation")) + 1)
                    .put("revocationGeneration", ((Integer) before.get("revocation_generation")) + 1).put("revision", ((Integer) before.get("revision")) + 1);
            var event = facts.fetchOne("SELECT id, share_grant_id, share_admission_id, event_domain, event_id, event_type, result, occurred_at,"
                    + " risk_digest, payload_redacted, payload_sha256 FROM sharing.share_access_event WHERE share_grant_id = ?", grantId);
            assertThat(event.get("id", UUID.class)).isNotNull();
            assertThat(event.get("share_grant_id")).isEqualTo(grantId);
            assertThat(event.get("event_domain")).isEqualTo("SHARE_ACCESS");
            assertThat(event.get("event_id")).isEqualTo(eventId);
            assertThat(event.get("event_type")).isEqualTo("CODE_ROTATED");
            assertThat(event.get("result")).isEqualTo("SUCCESS");
            assertThat(event.get("share_admission_id")).isNull();
            assertThat(event.get("risk_digest")).isNull();
            assertThat(event.get("occurred_at", java.time.OffsetDateTime.class).toInstant()).isEqualTo(occurred);
            assertThat(occurred.getNano() % 1000).isZero();
            assertThat(mapper.readTree(event.get("payload_redacted", JSONB.class).data())).isEqualTo(expected);
            assertThat(event.get("payload_sha256")).isEqualTo(DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(expected)));
            var outbox = facts.fetchOne("SELECT event_domain, aggregate_type, aggregate_id, event_type, dedupe_key, payload_redacted, payload_ciphertext, payload_sha256"
                    + " FROM platform.outbox_event WHERE aggregate_id = ? AND event_type = 'SHARE_CODE_ROTATED'", grantId);
            assertThat(outbox.get("event_domain")).isEqualTo("SHARE_ACCESS");
            assertThat(outbox.get("aggregate_type")).isEqualTo("EVIDENCE_SHARE_GRANT");
            assertThat(outbox.get("aggregate_id")).isEqualTo(grantId);
            assertThat(outbox.get("event_type")).isEqualTo("SHARE_CODE_ROTATED");
            assertThat(outbox.get("dedupe_key")).isEqualTo("share-transition:" + eventId);
            assertThat(outbox.get("payload_ciphertext")).isNull();
            var expectedOutbox = expected.deepCopy().put("shareId", (String) before.get("share_id")).put("grantId", grantId.toString())
                    .put("occurredAt", DefaultCustomerShareCommandService.utc(occurred));
            assertThat(mapper.readTree(outbox.get("payload_redacted", JSONB.class).data())).isEqualTo(expectedOutbox);
            assertThat(outbox.get("payload_sha256")).isEqualTo(DeterministicShareCommandProviders.digest(DefaultCustomerShareCommandService.canonical(expectedOutbox)));
            var audit = facts.fetchOne("SELECT occurred_at, actor_type, actor_id, action_code, object_type, object_id, reason_code, before_snapshot,"
                    + " after_snapshot, request_id, trace_id FROM audit.audit_fact WHERE object_id = ? AND action_code = 'SHARE_GRANT_CODE_ROTATE'", grantId);
            assertThat(audit.get("occurred_at", java.time.OffsetDateTime.class).toInstant()).isEqualTo(occurred);
            assertThat(audit.get("actor_type")).isEqualTo("ACCOUNT");
            assertThat(audit.get("actor_id")).isEqualTo(base.account);
            assertThat(audit.get("action_code")).isEqualTo("SHARE_GRANT_CODE_ROTATE");
            assertThat(audit.get("object_type")).isEqualTo("EVIDENCE_SHARE_GRANT");
            assertThat(audit.get("object_id")).isEqualTo(grantId);
            assertThat(audit.get("reason_code")).isNull();
            assertThat(audit.get("request_id")).isEqualTo(requestDigest);
            assertThat(audit.get("trace_id")).isEqualTo(trace.map(UUID::toString).orElse(null));
            assertThat(mapper.readTree(audit.get("before_snapshot", JSONB.class).data())).isEqualTo(mapper.createObjectNode()
                    .put("status", "LOCKED").put("revision", (Integer) before.get("revision")));
            assertThat(mapper.readTree(audit.get("after_snapshot", JSONB.class).data())).isEqualTo(mapper.createObjectNode()
                    .put("status", "ACTIVE").put("revision", ((Integer) before.get("revision")) + 1));
            assertThat(revokeFacts(grantId).toString()).doesNotContain(code, token, rawKey, (String) before.get("code_salt"), (String) before.get("code_hash"));
            assertThat(facts.fetchOne("SELECT row_to_json(g)::text FROM sharing.evidence_share_grant g WHERE id = ?", grantId).get(0, String.class))
                    .doesNotContain(code, token, rawKey);
        }
    }

    private CustomerShareCommandResult revoke(String shareId, String key, Integer witness, String reason) {
        return service.execute(new CustomerShareCommand.Revoke(shareId, key, Optional.empty(), witness, reason), caller());
    }

    private Object revokeOutcome(CustomerShareCommandService selectedService, String shareId, String key, ShareCommandCaller selected) {
        try { return selectedService.execute(new CustomerShareCommand.Revoke(shareId, key, Optional.empty(), null, null), selected); }
        catch (ShareCommandException failure) { return failure.code(); }
    }

    private java.util.Map<String, Object> grantSnapshot(String shareId) throws Exception {
        try (Connection admin = base.admin()) {
            return DSL.using(admin).fetchOne("SELECT id, share_id, capability_token_hash, evidence_id, created_by_account_id, owner_organization_id,"
                    + " verification_mode, code_salt, code_hash, code_generation, valid_from, expires_at, max_admissions, admissions_used, download_allowed,"
                    + " content_snapshot, content_sha256, status, revocation_generation, revision FROM sharing.evidence_share_grant WHERE share_id = ?", shareId).intoMap();
        }
    }

    private void prepareGrant(String shareId, String status, int generation) throws Exception {
        try (Connection admin = base.admin()) {
            DSLContext facts = DSL.using(admin);
            facts.execute("UPDATE sharing.evidence_share_grant SET revocation_generation = ?, revision = revision + 1 WHERE share_id = ?",
                    status.equals("EXPIRED") || status.equals("REVOKED") ? generation - 1 : generation, shareId);
            if (!status.equals("ACTIVE")) {
                facts.execute("UPDATE sharing.evidence_share_grant SET status = ?, revocation_generation = revocation_generation + ?,"
                        + " revision = revision + 1 WHERE share_id = ?", status, status.equals("REVOKED") || status.equals("EXPIRED") ? 1 : 0, shareId);
            }
        }
    }

    private void assertRevokeHead(java.util.Map<String, Object> before, java.util.Map<String, Object> after) {
        var expected = new java.util.HashMap<>(before);
        expected.put("status", "REVOKED");
        expected.put("revision", ((Integer) before.get("revision")) + 1);
        expected.put("revocation_generation", ((Integer) before.get("revocation_generation")) + 1);
        assertThat(after).hasSize(20).isEqualTo(expected);
    }

    private List<String> revokeFacts(UUID grantId) throws Exception {
        try (Connection admin = base.admin()) {
            DSLContext facts = DSL.using(admin);
            return List.of(facts.fetch("SELECT row_to_json(e)::text FROM sharing.share_access_event e WHERE share_grant_id = ? ORDER BY id", grantId).formatJSON(),
                    facts.fetch("SELECT row_to_json(o)::text FROM platform.outbox_event o WHERE aggregate_id = ? ORDER BY id", grantId).formatJSON(),
                    facts.fetch("SELECT row_to_json(a)::text FROM audit.audit_fact a WHERE object_id = ? ORDER BY id", grantId).formatJSON());
        }
    }

    private void assertRevokeCounts(UUID grantId, int transitions) throws Exception {
        try (Connection admin = base.admin()) {
            DSLContext facts = DSL.using(admin);
            assertThat(count(facts, "sharing.share_access_event", "share_grant_id", grantId)).isEqualTo(transitions);
            assertThat(count(facts, "platform.outbox_event", "aggregate_id", grantId)).isEqualTo(1 + transitions);
            assertThat(count(facts, "audit.audit_fact", "object_id", grantId)).isEqualTo(1 + transitions);
            assertThat(count(facts, "sharing.share_admission", "share_grant_id", grantId)).isZero();
        }
    }

    private void assertRevokeExecution(ShareCommandCaller selected) {
        assertRevokeExecution(selected, executions, true);
    }

    private void assertRevokeExecution(ShareCommandCaller selected, List<Execution> operation, boolean writes) {
        assertThat(operation.getFirst().sql()).isEqualTo("SET LOCAL ROLE sharing_command_role");
        assertThat(operation.get(1).sql()).isEqualTo("SELECT current_user");
        assertThat(operation.stream().skip(1)).allMatch(execution -> execution.role().equals("sharing_command_role"));
        assertThat(operation.stream().map(Execution::pid).distinct()).hasSize(1);
        var locks = operation.stream().filter(execution -> execution.sql().contains("pg_advisory_xact_lock")).map(execution -> execution.bindings().getFirst()).toList();
        if (selected.organizationId().isPresent()) assertThat(locks).containsExactly("organization_access.organization:" + base.org,
                "organization_access.membership:" + base.org + ":" + base.account, "customer_iam.account:" + base.account);
        else assertThat(locks).containsExactly("customer_iam.account:" + base.account);
        assertThat(operation.stream().filter(execution -> execution.sql().equals("SELECT clock_timestamp()"))).hasSize(1);
        assertThat(operation).noneMatch(execution -> execution.sql().contains("SET LOCAL ROLE evidence_role") || execution.sql().contains("evidence.evidence_record"));
        int contextIndex = java.util.stream.IntStream.range(0, operation.size()).filter(index -> operation.get(index).sql().contains("customer_iam.customer_account")).findFirst().orElseThrow();
        int grantIndex = java.util.stream.IntStream.range(0, operation.size()).filter(index -> operation.get(index).sql().endsWith("FOR UPDATE")).findFirst().orElseThrow();
        assertThat(grantIndex).isGreaterThan(contextIndex);
        int lastLock = java.util.stream.IntStream.range(0, operation.size())
                .filter(index -> operation.get(index).sql().contains("pg_advisory_xact_lock")).max().orElseThrow();
        assertThat(contextIndex).isGreaterThan(lastLock);
        assertThat(operation.stream().filter(execution -> execution.sql().contains("FOR UPDATE"))).hasSize(1);
        if (writes) {
            var writeOrder = operation.stream().map(Execution::sql).filter(sql -> sql.startsWith("INSERT") || sql.startsWith("UPDATE")
                    || sql.contains("outbox_append") || sql.contains("append_fact")).toList();
            assertThat(writeOrder).hasSize(4);
            assertThat(writeOrder.get(0)).startsWith("INSERT INTO sharing.share_access_event");
            assertThat(writeOrder.get(1)).startsWith("UPDATE sharing.evidence_share_grant");
            assertThat(writeOrder.get(2)).contains("platform.outbox_append");
            assertThat(writeOrder.get(3)).contains("audit.append_fact");
        }
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
