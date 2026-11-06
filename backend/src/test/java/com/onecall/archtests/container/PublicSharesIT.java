package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import com.onecall.archtests.sharing.DeterministicShareCommandProviders;
import com.onecall.core.evidence.shareprojection.*;
import com.onecall.core.sharing.internal.application.ShareSummary.VerificationMode;
import com.onecall.core.sharing.internal.command.*;
import com.onecall.core.sharing.internal.persistence.PublicShareQueryRepository;
import com.onecall.core.sharing.internal.persistence.PublicShareQueryRepository.PublicGrant;
import com.onecall.core.sharing.internal.publicquery.*;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionTemplate;

class PublicSharesIT {
    private static final PublicSharesFixture fixture = new PublicSharesFixture();

    @BeforeAll
    static void startDatabase() throws Exception { fixture.start(); }
    @AfterAll
    static void stopDatabase() { fixture.close(); }
    @BeforeEach
    void seed() throws Exception { fixture.reset(); }

    @Test
    void publicGetResolvesOnlyExactTokenDigestAndRedactsEveryResponse() throws Exception {
        var before = fixture.snapshot();
        for (var mode : VerificationMode.values()) {
            var response = fixture.get(token(mode), mode == VerificationMode.ORGANIZATION_MEMBER ? "member" : null);
            assertThat(response.statusCode()).isEqualTo(200);
            var json = fixture.json(response);
            assertThat(json.properties().stream().map(Map.Entry::getKey)).containsExactlyInAnyOrder("verificationMode", "status", "expiresAt", "downloadAllowed", "content");
            assertThat(json.path("content").properties().stream().map(Map.Entry::getKey)).containsExactlyInAnyOrder("capturedAt", "validatedDurationSeconds", "artifacts");
            for (var artifact : json.path("content").path("artifacts")) assertThat(artifact.properties().stream().map(Map.Entry::getKey))
                    .containsExactlyInAnyOrder("artifactRole", "mimeType", "sizeBytes", "sha256");
            assertThat(response.body()).doesNotContain(token(mode), fixture.base.account.toString(), fixture.base.evidence.toString(), fixture.base.org.toString(),
                    fixture.subject.toString(), "PRIVATE_OBJECT_CANARY", "PA10_SECRET_CANARY", "test-code", "sourceRefId", "identityAtCapture", "capabilityToken");
            assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
            assertThat(response.headers().firstValue("Location")).isEmpty();
        }
        for (String token : List.of("unknown", token(VerificationMode.NONE).toUpperCase(Locale.ROOT), "retired"))
            problem(fixture.get(token, null), 404, "SHARE_TOKEN_INVALID");
        fixture.clearObservations();
        for (String path : List.of("/api/v1/public/shares/a%2Fb", "/api/v1/public/shares/a%2fb", "/api/v1/public/shares/a%252Fb",
                "/api/v1/public/shares/a%20b", "/api/v1/public/shares/a;b", "/api/v1/public//shares/a", "/api/v1/public/shares/a/admissions")) {
            problem(fixture.request("GET", path, "outage", "Accept", "text/html"), 404, "NOT_FOUND");
        }
        assertThat(fixture.verifications.get()).isZero();
        assertThat(fixture.digests.get()).isZero();
        assertThat(fixture.executions).isEmpty();
        var head = fixture.request("HEAD", "/api/v1/public/shares/unknown", "outage");
        assertThat(head.statusCode()).isEqualTo(404);
        assertThat(head.body()).isEmpty();
        assertThat(fixture.snapshot()).isEqualTo(before);
    }

    @Test
    void publicGetKeepsRevokedExpiredAndLockedPageSemantics() throws Exception {
        fixture.sql("UPDATE sharing.evidence_share_grant SET status = 'LOCKED', admissions_used = max_admissions, revision = revision + 1 WHERE share_id = ?",
                fixture.created.get(VerificationMode.EXTRACTION_CODE).state().shareId());
        assertThat(fixture.get(token(VerificationMode.EXTRACTION_CODE), null).statusCode()).isEqualTo(200);
        var before = fixture.snapshot();
        for (var mode : List.of(VerificationMode.NONE, VerificationMode.EXTRACTION_CODE)) {
            var state = fixture.created.get(mode).state();
            for (Instant now : List.of(state.validFrom().minusNanos(1), state.expiresAt(), state.expiresAt().plusSeconds(1))) {
                fixture.clearObservations();
                var service = service(grant -> grant, now, null);
                denied(() -> service.getPublicShare(new OpaqueCapabilityToken(token(mode)), organization -> { throw new AssertionError("Member called"); }),
                        now.isBefore(state.validFrom()) ? PublicShareQueryException.Code.SHARE_TOKEN_INVALID : PublicShareQueryException.Code.SHARE_EXPIRED);
                assertThat(fixture.executions).noneMatch(execution -> execution.sql().contains("evidence.evidence_record"));
            }
        }
        var expired = service(grant -> new PublicGrant(grant.evidenceId(), grant.ownerOrganizationId(), grant.verificationMode(), PublicShareView.Status.EXPIRED,
                grant.validFrom(), grant.expiresAt(), grant.downloadAllowed(), grant.contentSnapshot()), Instant.now(), null);
        denied(() -> expired.getPublicShare(new OpaqueCapabilityToken(token(VerificationMode.NONE)), organization -> { throw new AssertionError(); }), PublicShareQueryException.Code.SHARE_EXPIRED);
        assertThat(fixture.snapshot()).isEqualTo(before);
        String expiredToken = "synthetic-expired-" + UUID.randomUUID();
        fixture.sql("INSERT INTO sharing.evidence_share_grant(id, share_id, capability_token_hash, evidence_id, created_by_account_id, owner_organization_id, verification_mode, "
                + "code_salt, code_hash, code_generation, valid_from, expires_at, max_admissions, admissions_used, download_allowed, content_snapshot, content_sha256, status, revocation_generation, revision) "
                + "SELECT ?, ?, ?, evidence_id, created_by_account_id, owner_organization_id, verification_mode, code_salt, code_hash, code_generation, valid_from, expires_at, max_admissions, admissions_used, "
                + "download_allowed, content_snapshot, content_sha256, 'EXPIRED', 1, 1 FROM sharing.evidence_share_grant WHERE share_id = ?",
                UUID.randomUUID(), "synthetic-expired-" + UUID.randomUUID(), DeterministicShareCommandProviders.digest(expiredToken), fixture.created.get(VerificationMode.NONE).state().shareId());
        before = fixture.snapshot();
        problem(fixture.get(expiredToken, null), 410, "SHARE_EXPIRED");
        assertThat(fixture.snapshot()).isEqualTo(before);
        fixture.commands.execute(new CustomerShareCommand.Revoke(fixture.created.get(VerificationMode.NONE).state().shareId(), "revoke", Optional.empty(), 0, "Synthetic"), fixture.caller());
        before = fixture.snapshot();
        problem(fixture.get(token(VerificationMode.NONE), null), 410, "SHARE_REVOKED");
        var revokedAfterExpiry = service(grant -> grant, fixture.created.get(VerificationMode.NONE).state().expiresAt().plusSeconds(1), null);
        denied(() -> revokedAfterExpiry.getPublicShare(new OpaqueCapabilityToken(token(VerificationMode.NONE)), organization -> { throw new AssertionError(); }), PublicShareQueryException.Code.SHARE_REVOKED);
        assertThat(fixture.snapshot()).isEqualTo(before);
    }

    @Test
    void organizationPageRechecksCurrentAccountTenantAndMembership() throws Exception {
        for (String session : List.of("member", "personal", "different")) {
            var response = fixture.get(token(VerificationMode.ORGANIZATION_MEMBER), session, "X-Organization-Id", UUID.randomUUID().toString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(fixture.json(response).has("content")).isTrue();
        }
        metadata(fixture.get(token(VerificationMode.ORGANIZATION_MEMBER), null));
        UUID outsider = UUID.randomUUID();
        fixture.sql("INSERT INTO customer_iam.customer_account(id, display_name) VALUES (?, 'Synthetic nonmember')", outsider);
        fixture.sessions.put("nonmember", new com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession(outsider,
                com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1"));
        metadata(fixture.get(token(VerificationMode.ORGANIZATION_MEMBER), "nonmember"));
        fixture.sessions.put("missing", new com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession(UUID.randomUUID(),
                com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1"));
        problem(fixture.get(token(VerificationMode.ORGANIZATION_MEMBER), "missing"), 401, "UNAUTHENTICATED");
        fixture.sql("UPDATE organization_access.organization SET status = 'SUSPENDED', revision = revision + 1 WHERE id = ?", fixture.base.org);
        metadata(fixture.get(token(VerificationMode.ORGANIZATION_MEMBER), "member"));
        fixture.sql("UPDATE organization_access.organization SET status = 'ACTIVE', revision = revision + 1 WHERE id = ?", fixture.base.org);
        fixture.sql("UPDATE organization_access.organization_membership SET status = 'SUSPENDED', revision = revision + 1 WHERE id = ?", fixture.base.membership);
        metadata(fixture.get(token(VerificationMode.ORGANIZATION_MEMBER), "member"));
        fixture.sql("UPDATE organization_access.organization_membership SET status = 'REVOKED', revision = revision + 1 WHERE id = ?", fixture.base.membership);
        metadata(fixture.get(token(VerificationMode.ORGANIZATION_MEMBER), "member"));
        fixture.sql("UPDATE customer_iam.customer_account SET status = 'DISABLED', revision = revision + 1 WHERE id = ?", fixture.base.account);
        problem(fixture.get(token(VerificationMode.ORGANIZATION_MEMBER), "member"), 401, "UNAUTHENTICATED");
        problem(fixture.get(token(VerificationMode.ORGANIZATION_MEMBER), "outage"), 503, "SERVICE_UNAVAILABLE");
        problem(fixture.get(token(VerificationMode.ORGANIZATION_MEMBER), "unknown"), 401, "UNAUTHENTICATED");
    }

    @Test
    void publicProjectionUsesDedicatedRoleOnSameReadOnlyConnection() throws Exception {
        var before = fixture.snapshot();
        var response = fixture.get(token(VerificationMode.NONE), null);
        assertThat(response.statusCode()).isEqualTo(200);
        var content = fixture.json(response).path("content");
        assertThat(content.path("capturedAt").asString()).isEqualTo("2026-01-01T00:00:00Z");
        assertThat(content.path("validatedDurationSeconds").asInt()).isEqualTo(12);
        assertThat(content.path("artifacts").get(0).path("artifactRole").asString()).isEqualTo("ORIGINAL");
        assertThat(content.path("artifacts").get(1).path("artifactRole").asString()).isEqualTo("DERIVED");
        assertThat(fixture.executions.stream().map(PublicSharesFixture.Execution::pid).distinct()).hasSize(1);
        assertThat(fixture.executions).allMatch(execution -> execution.readOnly().equals("on")
                && execution.role().equals("sharing_public_read_role") && execution.sessionUser().equals("sharing_public_read_role"));
        assertThat(fixture.executions).noneMatch(execution -> execution.sql().contains("SET ROLE") || execution.sql().contains("SET LOCAL ROLE"));
        assertThat(fixture.executions.stream().filter(execution -> execution.sql().contains("FROM sharing.evidence_share_grant")))
                .hasSize(1).allMatch(execution -> execution.role().equals("sharing_public_read_role") && execution.prepared() && execution.bindings() == 1);
        assertThat(fixture.executions.stream().filter(execution -> execution.sql().contains("FROM evidence.")))
                .hasSize(2).allMatch(execution -> execution.role().equals("sharing_public_read_role"));
        var publicReader = fixture.context.getBean("publicShareableEvidenceProjectionReader", ShareableEvidenceProjectionReader.class);
        assertThat(fixture.context.getBeansOfType(ShareableEvidenceProjectionReader.class).keySet())
                .containsExactlyInAnyOrder("shareableEvidenceProjectionReader", "publicShareableEvidenceProjectionReader");
        assertThat(org.springframework.aop.support.AopUtils.isAopProxy(publicReader)).isTrue();
        var untouched = org.mockito.Mockito.mock(org.jooq.DSLContext.class);
        int commandConnections = fixture.commandPool.getHikariPoolMXBean().getActiveConnections();
        assertThatThrownBy(() -> publicReader.readShareableProjection(untouched, fixture.base.evidence))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
        assertThat(fixture.commandPool.getHikariPoolMXBean().getActiveConnections()).isEqualTo(commandConnections);
        fixture.context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class).execute(status -> {
            assertThatThrownBy(() -> publicReader.readShareableProjection(untouched, fixture.base.evidence))
                    .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
            return null;
        });
        org.mockito.Mockito.verifyNoInteractions(untouched);
        assertThat(fixture.snapshot()).isEqualTo(before);
        System.out.println("PA16 same-connection pid=" + fixture.executions.getFirst().pid() + " readOnly=on current=session=sharing_public_read_role noWrite=true");
    }

    @Test
    void publicProjectionAndDependencyFailuresFailClosedWithoutPartialContent() throws Exception {
        var before = fixture.snapshot();
        assertThat(fixture.get(token(VerificationMode.NONE), null).statusCode()).isEqualTo(200);
        var missingReader = new PublicShareQueryService(
                fixture.context.getBean("publicShareQueryTransactionTemplate", TransactionTemplate.class), fixture.dsl,
                fixture.context.getBean(PublicShareQueryRepository.class), null, DeterministicShareCommandProviders.sha256());
        denied(() -> missingReader.getPublicShare(new OpaqueCapabilityToken(token(VerificationMode.NONE)),
                organization -> PublicShareQueryService.MemberMatch.MATCH), PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        assertThat(fixture.snapshot()).isEqualTo(before);
        for (String failing : List.of("SELECT current_user, session_user", "FROM evidence.evidence_record", "SELECT clock_timestamp()")) {
            fixture.failOn = failing;
            problem(fixture.get(token(VerificationMode.NONE), null), 503, "SERVICE_UNAVAILABLE");
        }
        fixture.failOn = null;
        fixture.sql("REVOKE evidence_role FROM " + fixture.commandLogin);
        try { assertThat(fixture.get(token(VerificationMode.NONE), null).statusCode()).isEqualTo(200); }
        finally { fixture.sql("GRANT evidence_role TO " + fixture.commandLogin); }
        fixture.clearObservations();
        fixture.sql("REVOKE SELECT (identity_at_capture) ON evidence.evidence_record FROM sharing_public_read_role");
        try { problem(fixture.get(token(VerificationMode.NONE), null), 503, "SERVICE_UNAVAILABLE"); }
        finally { fixture.sql("SET ROLE evidence_owner; GRANT SELECT (identity_at_capture) ON evidence.evidence_record TO sharing_public_read_role; RESET ROLE"); }
        fixture.failIdentity = true;
        problem(fixture.get(token(VerificationMode.NONE), null), 503, "SERVICE_UNAVAILABLE");
        fixture.clearObservations();
        assertThat(fixture.get(token(VerificationMode.NONE), null).statusCode()).isEqualTo(200);
        for (String malformed : List.of("{}", "null", "{", "{\"content\":null}")) {
            var service = service(grant -> new PublicGrant(grant.evidenceId(), grant.ownerOrganizationId(), grant.verificationMode(), grant.status(),
                    grant.validFrom(), grant.expiresAt(), grant.downloadAllowed(), malformed), null, null);
            denied(() -> service.getPublicShare(new OpaqueCapabilityToken(token(VerificationMode.NONE)), organization -> PublicShareQueryService.MemberMatch.MATCH), PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        }
        for (var result : List.of(new ShareableEvidenceProjectionReader.ReadResult.Missing(), new ShareableEvidenceProjectionReader.ReadResult.Unavailable())) {
            var service = service(grant -> grant, null, (dsl, evidence) -> result);
            denied(() -> service.getPublicShare(new OpaqueCapabilityToken(token(VerificationMode.NONE)), organization -> PublicShareQueryService.MemberMatch.MATCH),
                    result instanceof ShareableEvidenceProjectionReader.ReadResult.Missing ? PublicShareQueryException.Code.FORBIDDEN : PublicShareQueryException.Code.SERVICE_UNAVAILABLE);
        }
        assertThat(fixture.snapshot()).isEqualTo(before);
        var ownerReader = fixture.context.getBean("publicShareableEvidenceProjectionReader", ShareableEvidenceProjectionReader.class);
        var nonSolidified = service(grant -> grant, null, (dsl, evidence) -> {
            var found = (ShareableEvidenceProjectionReader.ReadResult.Found) ownerReader.readShareableProjection(dsl, evidence);
            var projection = found.projection();
            return new ShareableEvidenceProjectionReader.ReadResult.Found(new ShareableEvidenceProjection(projection.evidenceId(), projection.subjectId(),
                    "QUARANTINED", projection.sourceType(), projection.sourceRefId(), projection.identityAtCapture(), projection.artifacts()));
        });
        denied(() -> nonSolidified.getPublicShare(new OpaqueCapabilityToken(token(VerificationMode.NONE)), organization -> PublicShareQueryService.MemberMatch.MATCH), PublicShareQueryException.Code.FORBIDDEN);
        assertThat(fixture.snapshot()).isEqualTo(before);
        UUID extra = UUID.randomUUID();
        fixture.sql("INSERT INTO evidence.media_object(id, storage_provider, bucket, object_key, object_version_id, sha256, size_bytes, mime_type, crypto_key_ref) VALUES (?, 'COS', 'fixture', ?, 'v1', repeat('f',64), 5, 'audio/wav', 'test-key')", extra, "UNSHARED_CANARY_" + extra);
        fixture.sql("INSERT INTO evidence.evidence_artifact(id, evidence_id, media_object_id, artifact_role) VALUES (?, ?, ?, 'DERIVED')", UUID.randomUUID(), fixture.base.evidence, extra);
        before = fixture.snapshot();
        problem(fixture.get(token(VerificationMode.NONE), null), 503, "SERVICE_UNAVAILABLE");
        assertThat(fixture.snapshot()).isEqualTo(before);
    }

    @Test
    void repeatedPageAndPrefetchLeaveAllPersistentFactsUnchanged() throws Exception {
        var before = fixture.snapshot();
        var envelopes = new HashMap<String, String>();
        for (var mode : VerificationMode.values()) {
            for (String session : new String[] {null, "member"}) {
                var response = fixture.get(token(mode), session);
                assertThat(response.statusCode()).isEqualTo(200);
                envelopes.put(mode + ":" + session, response.body());
                for (String purpose : List.of("prefetch", "prefetch;prerender", "navigate")) {
                    assertThat(fixture.get(token(mode), session, "Purpose", purpose, "Sec-Purpose", purpose, "Idempotency-Key", "CLIENT_CANARY").body()).isEqualTo(response.body());
                }
            }
        }
        fixture.sql("INSERT INTO subject_number.subject_risk_state(id, caller_number_binding_id, risk_type, source) VALUES (?, ?, 'RECYCLED', 'OPERATIONS')", UUID.randomUUID(), fixture.binding);
        for (var mode : VerificationMode.values()) for (String session : new String[] {null, "member"})
            assertThat(fixture.get(token(mode), session).body()).isEqualTo(envelopes.get(mode + ":" + session));
        assertThat(fixture.policies.get()).isZero();
        assertThat(fixture.tokenIssuances.get()).isZero();
        assertThat(fixture.extractionCodes.get()).isZero();
        assertThat(fixture.designatedMobiles.get()).isZero();
        assertThat(fixture.rotationPolicies.get()).isZero();
        assertThat(fixture.executions).noneMatch(execution -> execution.sql().contains("subject_number") || execution.sql().matches("(?is).*\\b(insert|update|delete)\\b.*"));
        assertThat(fixture.snapshot()).isEqualTo(before);
        System.out.println("PA15 no-write snapshots: 11 complete table digests equal across repeats/prefetch/CLEAR/OPEN");
    }

    @Test
    void publicReadIsPostgresEnforcedAndRestoresPoolRoleAndTransactionState() throws Exception {
        var before = fixture.snapshot();
        fixture.tryWrite = true;
        assertThat(fixture.get(token(VerificationMode.NONE), null).statusCode()).isEqualTo(200);
        assertThat(fixture.writeState).isEqualTo("25006");
        assertThat(fixture.executions.getFirst().sql()).isEqualTo("SET TRANSACTION READ ONLY");
        int pid = fixture.executions.getFirst().pid();
        try (var connection = fixture.pool.getConnection(); var rows = connection.createStatement().executeQuery("SELECT current_user, session_user, current_setting('transaction_read_only'), pg_backend_pid()")) {
            rows.next();
            assertThat(rows.getString(1)).isEqualTo("sharing_public_read_role");
            assertThat(rows.getString(2)).isEqualTo("sharing_public_read_role");
            assertThat(rows.getString(3)).isEqualTo("off");
            assertThat(rows.getInt(4)).isEqualTo(pid);
            assertThat(connection.isReadOnly()).isFalse();
            assertThat(connection.getAutoCommit()).isTrue();
        }
        for (String login : List.of(fixture.base.login, fixture.commandLogin)) {
            try (var connection = DriverManager.getConnection(fixture.base.url, login, fixture.base.password)) {
                assertThatThrownBy(() -> connection.createStatement().executeQuery("SELECT capability_token_hash FROM sharing.evidence_share_grant"))
                        .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
                if (login.equals(fixture.base.login)) assertThatThrownBy(() -> connection.createStatement().execute("SET ROLE sharing_command_role"))
                        .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
            }
        }
        fixture.tryWrite = false;
        assertConcurrentPoolEvidence();
        try (var executor = Executors.newFixedThreadPool(3)) {
            var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int index = 0; index < 6; index++) futures.add(executor.submit(() -> fixture.get(token(VerificationMode.NONE), null).statusCode()));
            for (var future : futures) assertThat(future.get(30, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertPublicPoolReset();
        assertThat(fixture.pool.getHikariPoolMXBean().getTotalConnections()).isEqualTo(1);
        assertThat(fixture.executions).isNotEmpty().allMatch(execution -> execution.role().equals("sharing_public_read_role")
                && execution.sessionUser().equals("sharing_public_read_role") && execution.readOnly().equals("on"));
        assertThat(fixture.executions).noneMatch(execution -> execution.sql().matches("(?is).*\\bSET\\s+(LOCAL\\s+|SESSION\\s+)?ROLE\\b.*"));
        assertThat(fixture.snapshot()).isEqualTo(before);
        var publicExecutions = List.copyOf(fixture.executions);
        fixture.commandExecutions.clear();
        var command = fixture.create(VerificationMode.NONE, "after-public");
        assertThat(command).isNotNull();
        assertThat(fixture.commandExecutions.stream().filter(execution -> execution.sql().startsWith("INSERT INTO sharing.evidence_share_grant")))
                .isNotEmpty().allMatch(execution -> execution.readOnly().equals("off")
                        && execution.role().equals("sharing_command_role") && execution.sessionUser().equals(fixture.commandLogin));
        assertThat(fixture.executions).containsExactlyElementsOf(publicExecutions);
        assertThat(fixture.pool).isNotSameAs(fixture.context.getBean("sharingCommandDataSource"));
        System.out.println("PA16 SQLSTATE=25006 pidReuse=" + pid + " poolResetIdentity=true transaction_read_only=off positiveCommandINSERT=true commandReadOnly=off publicStreamUnchanged=true");
    }

    private static void assertConcurrentPoolEvidence() throws Exception {
        int maximum = fixture.pool.getMaximumPoolSize();
        var executor = Executors.newFixedThreadPool(2);
        var futures = new ArrayList<Future<PublicSharesFixture.Execution>>();
        try {
            fixture.pool.setMaximumPoolSize(2);
            int observationStart = fixture.executions.size();
            var publicBarrier = new CyclicBarrier(2);
            var first = executor.submit(() -> fixture.heldPublicRead(publicBarrier));
            futures.add(first);
            var second = executor.submit(() -> fixture.heldPublicRead(publicBarrier));
            futures.add(second);
            var firstIdentity = first.get(40, TimeUnit.SECONDS);
            var secondIdentity = second.get(40, TimeUnit.SECONDS);
            assertThat(firstIdentity.pid()).isNotEqualTo(secondIdentity.pid());
            var publicPids = Set.of(firstIdentity.pid(), secondIdentity.pid());
            var concurrentExecutions = List.copyOf(fixture.executions.subList(observationStart, fixture.executions.size()));
            assertThat(concurrentExecutions).isNotEmpty().allMatch(execution -> publicPids.contains(execution.pid())
                    && execution.role().equals("sharing_public_read_role") && execution.sessionUser().equals("sharing_public_read_role")
                    && execution.readOnly().equals("on"));
            assertThat(concurrentExecutions).noneMatch(execution -> execution.sql().matches("(?is).*\\bSET\\s+(LOCAL\\s+|SESSION\\s+)?ROLE\\b.*"));
            for (int publicPid : publicPids) {
                assertThat(concurrentExecutions.stream().filter(execution -> execution.pid() == publicPid
                        && execution.sql().contains("FROM sharing.evidence_share_grant"))).hasSize(1);
                assertThat(concurrentExecutions.stream().filter(execution -> execution.pid() == publicPid
                        && execution.sql().contains("FROM evidence."))).hasSize(2);
            }
            try (var firstReset = fixture.pool.getConnection(); var secondReset = fixture.pool.getConnection()) {
                assertThat(Set.of(fixture.observe(firstReset).pid(), fixture.observe(secondReset).pid())).isEqualTo(publicPids);
                for (var connection : List.of(firstReset, secondReset)) {
                    var identity = fixture.observe(connection);
                    assertThat(identity.role()).isEqualTo("sharing_public_read_role");
                    assertThat(identity.sessionUser()).isEqualTo("sharing_public_read_role");
                    assertThat(identity.readOnly()).isEqualTo("off");
                    assertThat(connection.getAutoCommit()).isTrue();
                    assertThat(connection.isReadOnly()).isFalse();
                }
            }
            System.out.println("PA16 twoPublicBarrier=passed pids=" + publicPids
                    + " distinct=true current=session=sharing_public_read_role readOnly=on projections=2 noRoleTransitions=true bothReset=true");
            var dualPoolBarrier = new CyclicBarrier(2);
            var publicRead = executor.submit(() -> fixture.heldPublicRead(dualPoolBarrier));
            futures.add(publicRead);
            var commandRead = executor.submit(() -> fixture.heldCommandRead(dualPoolBarrier));
            futures.add(commandRead);
            var publicIdentity = publicRead.get(40, TimeUnit.SECONDS);
            var commandIdentity = commandRead.get(40, TimeUnit.SECONDS);
            assertThat(publicIdentity.pid()).isNotEqualTo(commandIdentity.pid());
            try (var publicReset = fixture.pool.getConnection(); var commandReset = fixture.commandPool.getConnection()) {
                var publicState = fixture.observe(publicReset);
                assertThat(publicState.role()).isEqualTo("sharing_public_read_role");
                assertThat(publicState.sessionUser()).isEqualTo("sharing_public_read_role");
                assertThat(publicState.readOnly()).isEqualTo("off");
                var commandState = fixture.observe(commandReset);
                assertThat(commandState.role()).isEqualTo(fixture.commandLogin);
                assertThat(commandState.sessionUser()).isEqualTo(fixture.commandLogin);
                assertThat(commandState.readOnly()).isEqualTo("off");
                assertThat(publicState.pid()).isNotEqualTo(commandState.pid());
                for (var connection : List.of(publicReset, commandReset)) {
                    assertThat(connection.getAutoCommit()).isTrue();
                    assertThat(connection.isReadOnly()).isFalse();
                }
            }
            System.out.println("PA16 simultaneousPoolBarrier=passed publicPid=" + publicIdentity.pid() + " commandPid=" + commandIdentity.pid()
                    + " distinct=true publicReadOnly=on commandReadOnly=off commandRole=sharing_command_role commandSession=login bothReset=true");
        } finally {
            futures.forEach(future -> future.cancel(true));
            executor.shutdownNow();
            try { assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue(); }
            finally {
                fixture.pool.setMaximumPoolSize(maximum);
                fixture.pool.getHikariPoolMXBean().softEvictConnections();
            }
        }
        assertThat(fixture.pool.getMaximumPoolSize()).isEqualTo(maximum);
    }

    @Test
    void publicReadPreservesCreateRevokeRotateListAndMeBehavior() throws Exception {
        var command = fixture.command(VerificationMode.NONE, "regression");
        var created = (CustomerShareCommandResult.Created) fixture.commands.execute(command, fixture.caller());
        var replay = fixture.commands.execute(command, fixture.caller());
        assertThat(replay).isInstanceOf(CustomerShareCommandResult.CreationReplayed.class);
        var before = fixture.snapshot();
        assertThat(fixture.get(created.capabilityToken(), null).statusCode()).isEqualTo(200);
        assertThat(fixture.snapshot()).isEqualTo(before);
        fixture.commands.execute(new CustomerShareCommand.Revoke(created.state().shareId(), "regression-revoke", Optional.empty(), 0, "Synthetic"), fixture.caller());
        before = fixture.snapshot();
        problem(fixture.get(created.capabilityToken(), null), 410, "SHARE_REVOKED");
        assertThat(fixture.snapshot()).isEqualTo(before);
        var extraction = fixture.created.get(VerificationMode.EXTRACTION_CODE);
        fixture.sql("UPDATE sharing.evidence_share_grant SET status = 'LOCKED', revision = revision + 1 WHERE share_id = ?", extraction.state().shareId());
        var rotation = new CustomerShareCommand.Rotate(extraction.state().shareId(), "regression-rotate", Optional.empty(), 1);
        assertThat(fixture.commands.execute(rotation, fixture.caller())).isInstanceOf(CustomerShareCommandResult.Rotated.class);
        assertThat(fixture.commands.execute(rotation, fixture.caller())).isInstanceOf(CustomerShareCommandResult.RotationReplayed.class);
        before = fixture.snapshot();
        assertThat(fixture.get(extraction.capabilityToken(), null).statusCode()).isEqualTo(200);
        assertThat(fixture.request("GET", "/api/v1/customer/me", "member").statusCode()).isEqualTo(200);
        assertThat(fixture.request("GET", "/api/v1/customer/shares", "different").statusCode()).isEqualTo(200);
        assertThat(fixture.request("GET", "/api/v1/customer/shares", "member").statusCode()).isEqualTo(503);
        assertThat(fixture.snapshot()).isEqualTo(before);
    }

    private static PublicShareQueryService service(UnaryOperator<PublicGrant> transform, Instant time, ShareableEvidenceProjectionReader reader) {
        var repository = fixture.context.getBean(PublicShareQueryRepository.class);
        var seam = new PublicShareQueryRepository() {
            public Optional<PublicGrant> findByTokenDigest(String digest) { return repository.findByTokenDigest(digest).map(transform); }
            public Instant sampleDatabaseTime() { return time == null ? repository.sampleDatabaseTime() : time; }
        };
        return new PublicShareQueryService(fixture.context.getBean("publicShareQueryTransactionTemplate", TransactionTemplate.class), fixture.dsl, seam,
                reader == null ? fixture.context.getBean("publicShareableEvidenceProjectionReader", ShareableEvidenceProjectionReader.class) : reader, DeterministicShareCommandProviders.sha256());
    }
    private static String token(VerificationMode mode) { return fixture.created.get(mode).capabilityToken(); }
    private static void denied(Runnable action, PublicShareQueryException.Code code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(PublicShareQueryException.class, failure -> assertThat(failure.code()).isEqualTo(code)).hasMessage(code.name()).hasNoCause();
    }
    private static void metadata(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(200);
        var json = fixture.json(response);
        assertThat(json.properties().stream().map(Map.Entry::getKey)).containsExactlyInAnyOrder("verificationMode", "status", "expiresAt", "downloadAllowed", "verificationChallenge");
        assertThat(json.path("verificationChallenge").path("customerSessionRequired").asBoolean()).isTrue();
        assertThat(json.has("content")).isFalse();
    }
    private static void problem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/problem+json");
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).isEqualTo("no-store");
        var json = fixture.json(response);
        assertThat(json.properties().stream().map(Map.Entry::getKey)).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(json.path("code").asString()).isEqualTo(code);
        assertThat(UUID.fromString(json.path("traceId").asString())).isNotNull();
        assertThat(response.body()).doesNotContain("CANARY", "test-token-", fixture.base.account.toString(), "/api/", "<html");
        assertPublicPoolReset();
    }

    private static void assertPublicPoolReset() {
        try (var connection = fixture.pool.getConnection(); var statement = connection.createStatement(); var rows = statement.executeQuery(
                "SELECT current_user, session_user, current_setting('transaction_read_only'), pg_backend_pid()")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("sharing_public_read_role");
            assertThat(rows.getString(2)).isEqualTo("sharing_public_read_role");
            assertThat(rows.getString(3)).isEqualTo("off");
            if (!fixture.executions.isEmpty()) assertThat(rows.getInt(4)).isEqualTo(fixture.executions.getLast().pid());
            assertThat(connection.isReadOnly()).isFalse();
            assertThat(connection.getAutoCommit()).isTrue();
        } catch (SQLException failure) { throw new AssertionError("Public pool reset observation failed"); }
    }
}
