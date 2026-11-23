package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;

import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.*;
import com.onecall.core.subject_number.internal.claim.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimReferenceCipher.SealedReference;
import com.onecall.core.subject_number.internal.claim.SubjectClaimVerificationReference.Payload;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.*;

@Timeout(30)
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class CustomerSubjectClaimsIT {
    private org.springframework.boot.test.system.CapturedOutput output;
    @BeforeEach void captureOutput(org.springframework.boot.test.system.CapturedOutput captured) { output = captured; }
    static CustomerSubjectClaimFixture fixture;
    @BeforeAll static void start() throws Exception { fixture = new CustomerSubjectClaimFixture(); fixture.start(); }
    @AfterAll static void stop() { if (fixture != null) fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    private static Payload payload(CustomerSubjectClaimFixture.Seed seed, VerificationMethod method, String reference) { return new Payload(1, seed.subject(), seed.binding(), seed.hmac(), method, reference); }
    private static void code(tools.jackson.databind.JsonNode response, String code) { assertThat(response.path("code").asString()).isEqualTo(code); }
    private static String acceptedHistory(CustomerSubjectClaimFixture.Seed seed, UUID id, UUID claimant, UUID organization, Payload payload) throws Exception {
        Runnable probe = fixture.providers.probe; fixture.providers.probe = () -> { };
        String envelope; try { envelope = fixture.providers.sealPayload(payload); } finally { fixture.providers.probe = probe; }
        try (var connection = fixture.admin()) {
            execute(connection, "INSERT INTO subject_number.subject_claim(id,subject_id,account_id,organization_id,verification_method,verification_ref_ciphertext,request_id,status,risk_checked_at,verified_at) VALUES (?,?,?,?,?,?,?,'ACCEPTED',clock_timestamp(),clock_timestamp()+interval '1 second')",
                    id, seed.subject(), claimant, organization, payload.verificationMethod().name(), envelope, "history-" + id);
        }
        return envelope;
    }

    @Test
    void postgres16FrozenSeventeenMigrationChainAndDedicatedLoginAreUsed() throws Exception {
        fixture.preflight(); assertThat(fixture.value("SHOW server_version")).startsWith("16.");
        fixture.flyway().validate(); assertThat(fixture.flyway().info().applied()).hasSize(19);
        assertThat(fixture.value("SELECT count(*) FROM pg_auth_members WHERE member=(SELECT oid FROM pg_roles WHERE rolname=?)", fixture.login)).isEqualTo("1");
        fixture.cleanPool();
    }
    @Test
    void roleLocksReadsWritesAndAuditShareOneConnection() throws Exception {
        fixture.preflight(); List<Integer> pids = new CopyOnWriteArrayList<>(); List<String> roles = new CopyOnWriteArrayList<>();
        var previous = fixture.dsl.configuration().executeListenerProviders();
        fixture.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeEnd(ExecuteContext context) {
                try (var statement = context.connection().createStatement(); var rows = statement.executeQuery("SELECT pg_backend_pid(),current_user")) {
                    assertThat(rows.next()).isTrue(); pids.add(rows.getInt(1)); roles.add(rows.getString(2));
                } catch (SQLException failure) { throw new IllegalStateException("Connection probe failed"); }
            }
        });
        try { fixture.post(fixture.seed(), "same-pid", 200); }
        finally { fixture.dsl.configuration().set(previous); }
        assertThat(new HashSet<>(pids)).hasSize(1); assertThat(roles).allMatch(role -> role.equals("subject_number_role"));
        assertThat(fixture.providerPids).isNotEmpty(); assertThat(new HashSet<>(fixture.providerPids)).isEqualTo(new HashSet<>(pids));
        fixture.audit("CLAIM_ACCEPTED", "ACCEPTED", "NO_OPEN"); fixture.cleanPool();
    }
    @Test
    void freshUnownedPersonalClaimCommitsOneOwnerAndAcceptedRow() throws Exception {
        var seed = fixture.seed(); String before = fixture.value("SELECT updated_at::text FROM subject_number.business_subject WHERE id=?", seed.subject());
        var result = fixture.post(seed, "fresh", 200); assertThat(result.path("status").asString()).isEqualTo("ACCEPTED");
        assertThat(fixture.owner(seed)).isEqualTo(fixture.account + "|null|2"); assertThat(fixture.count("subject_number.subject_claim")).isEqualTo(1);
        assertThat(fixture.value("SELECT verified_at>=risk_checked_at FROM subject_number.subject_claim")).isEqualTo("t");
        assertThat(fixture.value("SELECT updated_at::text FROM subject_number.business_subject WHERE id=?", seed.subject()).equals(before)).isFalse();
        fixture.audit("CLAIM_ACCEPTED", "ACCEPTED", "NO_OPEN");
    }
    @Test
    void inactiveAccountAndOrganizationWorkspaceAreForbidden() throws Exception {
        var seed = fixture.seed(); String body = fixture.body(seed, VerificationMethod.SMS, fixture.approve(seed, fixture.account, VerificationMethod.SMS, "server-reference"));
        code(fixture.checked(fixture.post("organization", "organization", body), 403), "FORBIDDEN");
        code(fixture.checked(fixture.post("missing", "missing", body), 422), "VALIDATION_FAILED");
        assertThat(fixture.count("subject_number.subject_claim")).isZero(); assertThat(fixture.count("audit.audit_fact")).isZero();
        UUID absent = CustomerBusinessNumbersFixture.SESSIONS.get("missing").accountId();
        body = fixture.body(seed, VerificationMethod.SMS, fixture.approve(seed, absent, VerificationMethod.SMS, "missing-reference"));
        code(fixture.checked(fixture.post("missing", "missing-current-account", body), 403), "FORBIDDEN");
        assertThat(fixture.count("audit.audit_fact")).isZero();
        try (var connection = fixture.admin()) { execute(connection, "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account); }
        code(fixture.post(seed, "inactive", 403), "FORBIDDEN"); fixture.audit("CLAIM_ACCOUNT_INACTIVE", "DENIED", "NOT_REQUIRED");
        assertThat(fixture.owner(seed)).isEqualTo("null|null|1");
    }
    @Test
    void proofInvalidUnavailableAndUnresolvableReferencesFailClosed() throws Exception {
        var seed = fixture.seed();
        code(fixture.checked(fixture.post("personal", "invalid", fixture.body(seed, VerificationMethod.SMS, "unknown")), 422), "VALIDATION_FAILED");
        fixture.providers.unavailable = true; code(fixture.post(seed, "unavailable", 503), "SERVICE_UNAVAILABLE"); fixture.providers.unavailable = false;
        fixture.historyEnvelope(seed, fixture.account, VerificationMethod.SMS, "stored", ClaimStatus.REJECTED, "v1.test.AAAA");
        code(fixture.post(seed, "stored", 422), "VALIDATION_FAILED"); fixture.audit("CLAIM_REPLAY_UNRESOLVABLE", "DENIED", "NO_OPEN");
        assertThat(fixture.owner(seed)).isEqualTo("null|null|1");
    }
    @Test
    void missingSubjectBindingAndBindingMismatchUseUniformValidation() throws Exception {
        var seed = fixture.seed(); var missingBinding = new CustomerSubjectClaimFixture.Seed(seed.subject(), UUID.randomUUID(), seed.hmac());
        code(fixture.post(missingBinding, "missing-binding", 422), "VALIDATION_FAILED");
        var missingSubject = new CustomerSubjectClaimFixture.Seed(UUID.randomUUID(), seed.binding(), seed.hmac());
        code(fixture.post(missingSubject, "missing-subject", 422), "VALIDATION_FAILED"); assertThat(fixture.count("audit.audit_fact")).isZero();
        var second = fixture.seed(); var mismatch = new CustomerSubjectClaimFixture.Seed(second.subject(), seed.binding(), seed.hmac());
        code(fixture.post(mismatch, "mismatch", 422), "VALIDATION_FAILED");
        assertThat(fixture.count("subject_number.subject_claim")).isEqualTo(1); fixture.audit("CLAIM_SUBJECT_BINDING_MISMATCH", "DENIED", "NOT_REQUIRED");
    }
    @Test
    void inactiveAndUnverifiedBindingsPersistOnlyRejectedHistory() throws Exception {
        for (boolean verified : List.of(false, true)) {
            var seed = fixture.seed(null, null, "DISABLED", verified); code(fixture.post(seed, UUID.randomUUID().toString(), 422), "VALIDATION_FAILED");
            fixture.audit("CLAIM_BINDING_NOT_ACTIVE", "DENIED", "NOT_REQUIRED"); assertThat(fixture.owner(seed)).isEqualTo("null|null|1");
        }
        assertThat(fixture.value("SELECT count(*) FROM subject_number.subject_claim WHERE status='REJECTED'")).isEqualTo("2");
    }
    @Test
    void openRiskRejectsFreshRequestAndPreemptsAcceptedReplay() throws Exception {
        var seed = fixture.seed(); var accepted = fixture.post(seed, "accepted", 200); fixture.risk(seed, "OPEN");
        code(fixture.post(seed, "accepted", 409), "CLAIM_RISK_BLOCKED"); code(fixture.post(seed, "fresh-risk", 409), "CLAIM_RISK_BLOCKED");
        assertThat(fixture.value("SELECT id::text FROM subject_number.subject_claim WHERE request_id='accepted'")).isEqualTo(accepted.path("claimId").asString());
        assertThat(fixture.value("SELECT status FROM subject_number.subject_claim WHERE request_id='fresh-risk'")).isEqualTo("REJECTED");
        fixture.audit("CLAIM_RISK_BLOCKED", "DENIED", "OPEN"); assertThat(fixture.owner(seed)).isEqualTo(fixture.account + "|null|2");
    }
    @Test
    void openRiskPreemptsSameAccountDuplicateAndStoredReferenceErrors() throws Exception {
        for (boolean unavailable : List.of(false, true)) {
            fixture.reset(); var seed = fixture.seed(fixture.account, null, "ACTIVE", true); String envelope = "v1.stored." + UUID.randomUUID().toString().replace("-", "");
            fixture.historyEnvelope(seed, fixture.account, VerificationMethod.SMS, "stored", ClaimStatus.ACCEPTED, envelope);
            fixture.providers.force(envelope, unavailable ? new SubjectClaimProviderResult.Unavailable<>() : new SubjectClaimProviderResult.Invalid<>());
            fixture.risk(seed, "OPEN"); int before = fixture.providers.opens(envelope);
            code(fixture.post(seed, "stored", 409), "CLAIM_RISK_BLOCKED"); code(fixture.post(seed, "duplicate", 409), "CLAIM_RISK_BLOCKED");
            assertThat(fixture.providers.opens(envelope)).isEqualTo(before); fixture.audit("CLAIM_RISK_BLOCKED", "DENIED", "OPEN");
        }
    }
    @Test
    void clearedRiskReevaluatesAndReplaysFirstStoredOutcome() throws Exception {
        var seed = fixture.seed(); fixture.risk(seed, "OPEN"); fixture.post(seed, "risk", 409); fixture.risk(seed, "CLEARED");
        var replay = fixture.post(seed, "risk", 200); assertThat(replay.path("status").asString()).isEqualTo("REJECTED");
        fixture.audit("CLAIM_REPLAYED", "REPLAYED", "NO_OPEN"); assertThat(fixture.owner(seed)).isEqualTo("null|null|1");
        var accepted = fixture.post(seed, "accepted", 200); fixture.risk(seed, "OPEN"); fixture.post(seed, "accepted", 409); fixture.risk(seed, "CLEARED");
        assertThat(fixture.post(seed, "accepted", 200).equals(accepted)).isTrue();
    }
    @Test
    void exactReplayUsesFirstIdTimeAndRandomizedReferenceEquality() throws Exception {
        var seed = fixture.seed(); var first = fixture.post(seed, "same", 200); var replay = fixture.post(seed, "same", 200);
        assertThat(first.equals(replay)).isTrue(); assertThat(fixture.count("subject_number.subject_claim")).isEqualTo(1);
        assertThat(fixture.owner(seed)).isEqualTo(fixture.account + "|null|2"); fixture.audit("CLAIM_REPLAYED", "REPLAYED", "NO_OPEN");
        assertThat(fixture.providers.seals.get()).isGreaterThanOrEqualTo(2);
    }
    @Test
    void changedReplayIdentityReturnsConflictWithoutStoredDisclosure() throws Exception {
        for (int dimension = 0; dimension < 3; dimension++) for (boolean unavailable : List.of(false, true)) {
            fixture.reset(); var seed = fixture.seed(); var foreignSubject = dimension == 0 ? fixture.seed() : seed;
            String envelope = "v1.foreign." + UUID.randomUUID().toString().replace("-", "");
            fixture.historyEnvelope(foreignSubject, dimension == 1 ? fixture.other : fixture.account,
                    dimension == 2 ? VerificationMethod.MANUAL_REVIEW : VerificationMethod.SMS, "foreign", ClaimStatus.REJECTED, envelope);
            fixture.providers.force(envelope, unavailable ? new SubjectClaimProviderResult.Unavailable<>() : new SubjectClaimProviderResult.Invalid<>());
            code(fixture.post(seed, "foreign", 409), "CLAIM_REPLAY_CONFLICT"); assertThat(fixture.providers.opens(envelope)).isZero();
            fixture.audit("CLAIM_REPLAY_CONFLICT", "CONFLICT", "NO_OPEN"); assertThat(fixture.count("subject_number.subject_claim")).isEqualTo(1);
        }
        fixture.reset(); var seed = fixture.seed(); fixture.post(seed, "logical", 200);
        var attempt = fixture.approve(seed, fixture.account, VerificationMethod.SMS, "changed-server-reference");
        code(fixture.checked(fixture.post("personal", "logical", fixture.body(seed, VerificationMethod.SMS, attempt)), 409), "CLAIM_REPLAY_CONFLICT");
    }
    @Test
    void exactDuplicateWithNewKeyReturnsFirstAcceptedWithoutInsert() throws Exception {
        var seed = fixture.seed(); var first = fixture.post(seed, "first", 200); assertThat(fixture.post(seed, "unused", 200).equals(first)).isTrue();
        assertThat(fixture.value("SELECT count(*) FROM subject_number.subject_claim WHERE request_id='unused'")).isEqualTo("0");
        assertThat(fixture.owner(seed)).isEqualTo(fixture.account + "|null|2"); fixture.audit("CLAIM_DUPLICATE", "DUPLICATE", "NO_OPEN");
    }
    @Test
    void changedDuplicateContextAppendsOneConflictAndNeverUpdatesOwner() throws Exception {
        for (String dimension : List.of("proof", "method", "binding", "hmac")) {
            fixture.reset(); var seed = fixture.seed(fixture.account, null, "ACTIVE", true); UUID accepted = UUID.randomUUID();
            String envelope = acceptedHistory(seed, accepted, fixture.account, null, new Payload(1, seed.subject(),
                    dimension.equals("binding") ? UUID.randomUUID() : seed.binding(),
                    dimension.equals("hmac") ? "b".repeat(64) : seed.hmac(), VerificationMethod.SMS, "server-reference"));
            String owner = fixture.owner(seed), updated = fixture.value("SELECT updated_at::text FROM subject_number.business_subject WHERE id=?", seed.subject());
            String stored = fixture.value("SELECT row_to_json(claim)::text FROM subject_number.subject_claim claim WHERE id=?", accepted);
            VerificationMethod method = dimension.equals("method") ? VerificationMethod.MANUAL_REVIEW : VerificationMethod.SMS;
            var attempt = fixture.approve(seed, fixture.account, method, dimension.equals("proof") ? "different-reference" : "server-reference");
            String body = fixture.body(seed, method, attempt);
            if (dimension.equals("method")) fixture.providers.force(envelope, new SubjectClaimProviderResult.Unavailable<>());
            AtomicInteger metadataReads = new AtomicInteger(), ownerWrites = new AtomicInteger();
            var previous = fixture.dsl.configuration().executeListenerProviders();
            fixture.dsl.configuration().set(new DefaultExecuteListener() {
                @Override public void executeStart(ExecuteContext context) {
                    if (metadataReads.get() == 0) assertThat(fixture.providers.opens(envelope)).isZero();
                    if (context.sql().startsWith("UPDATE subject_number.business_subject")) ownerWrites.incrementAndGet();
                }
                @Override public void executeEnd(ExecuteContext context) {
                    if (context.sql().contains("FROM subject_number.subject_claim WHERE subject_id=? AND account_id=? AND status='ACCEPTED'")) metadataReads.incrementAndGet();
                }
            });
            try {
                var response = fixture.post("personal", "changed", body);
                code(fixture.checked(response, 409), "CLAIM_CONTEXT_CONFLICT");
                assertThat(response.body().contains(accepted.toString())).isFalse(); assertThat(response.body().contains("verifiedAt")).isFalse();
                assertThat(metadataReads.get()).isPositive(); assertThat(fixture.providers.opens(envelope)).isEqualTo(dimension.equals("method") ? 0 : 1);
                fixture.audit("CLAIM_CONTEXT_CONFLICT", "CONFLICT", "NO_OPEN");
                assertThat(fixture.value("SELECT count(*) FROM subject_number.subject_claim WHERE request_id='changed' AND status='CONFLICT' AND verified_at IS NULL")).isEqualTo("1");
                assertThat(fixture.checked(fixture.post("personal", "changed", body), 200).path("status").asString()).isEqualTo("CONFLICT");
                assertThat(ownerWrites).hasValue(0);
            } finally { fixture.dsl.configuration().set(previous); }
            assertThat(fixture.count("subject_number.subject_claim")).isEqualTo(2); assertThat(fixture.owner(seed)).isEqualTo(owner);
            assertThat(fixture.value("SELECT updated_at::text FROM subject_number.business_subject WHERE id=?", seed.subject())).isEqualTo(updated);
            assertThat(fixture.value("SELECT row_to_json(claim)::text FROM subject_number.subject_claim claim WHERE id=?", accepted).equals(stored)).isTrue();
        }
    }
    @Test
    void identicalRejectedAndConflictReplayReturnsFrozenStoredShape() throws Exception {
        for (var status : List.of(ClaimStatus.REJECTED, ClaimStatus.CONFLICT)) {
            var seed = fixture.seed(); String key = UUID.randomUUID().toString();
            UUID id = fixture.history(seed, fixture.account, VerificationMethod.SMS, key, status, payload(seed, VerificationMethod.SMS, "server-reference"));
            var result = fixture.post(seed, key, 200); assertThat(result.path("claimId").asString()).isEqualTo(id.toString());
            assertThat(result.path("status").asString()).isEqualTo(status.name()); assertThat(result.has("verifiedAt")).isFalse(); fixture.audit("CLAIM_REPLAYED", "REPLAYED", "NO_OPEN");
        }
    }
    @Test
    void otherAccountAndOrganizationOwnersAreNeverTransferred() throws Exception {
        for (boolean organization : List.of(false, true)) {
            var seed = fixture.seed(organization ? null : fixture.other, organization ? fixture.base.organizations.getFirst() : null, "ACTIVE", true);
            String before = fixture.owner(seed); code(fixture.post(seed, UUID.randomUUID().toString(), 403), "FORBIDDEN");
            assertThat(fixture.owner(seed)).isEqualTo(before); fixture.audit("CLAIM_OWNER_CONFLICT", "CONFLICT", "NO_OPEN");
        }
    }
    @Test
    void corruptAcceptedOwnerRelationsFailWithoutRepair() throws Exception {
        for (String corruption : List.of("personal", "unowned", "later", "organization-missing", "organization-different")) {
            fixture.reset(); UUID organization = fixture.base.organizations.getFirst(), different = UUID.randomUUID();
            boolean organizationOwned = corruption.startsWith("organization");
            var seed = fixture.seed(organizationOwned || corruption.equals("unowned") ? null : corruption.equals("later") ? fixture.account : fixture.other,
                    organizationOwned ? organization : null, "ACTIVE", true);
            if (corruption.equals("organization-different")) {
                try (var connection = fixture.admin()) { execute(connection, "INSERT INTO organization_access.organization(id,display_name) VALUES (?,'Different')", different); }
            }
            UUID earlier = UUID.fromString("00000000-0000-0000-0000-000000000001"), later = UUID.fromString("00000000-0000-0000-0000-000000000002");
            List<String> envelopes = new ArrayList<>();
            envelopes.add(acceptedHistory(seed, earlier, fixture.account, corruption.equals("organization-different") ? different : null, payload(seed, VerificationMethod.SMS, "server-reference")));
            if (corruption.equals("later")) {
                envelopes.add(acceptedHistory(seed, later, fixture.other, null, payload(seed, VerificationMethod.SMS, "other-reference")));
                assertThat(fixture.value("SELECT string_agg(id::text,',' ORDER BY id) FROM subject_number.subject_claim WHERE status='ACCEPTED'")).isEqualTo(earlier + "," + later);
            }
            String before = fixture.owner(seed), updated = fixture.value("SELECT updated_at::text FROM subject_number.business_subject WHERE id=?", seed.subject());
            String claims = fixture.value("SELECT jsonb_agg(to_jsonb(claim) ORDER BY id)::text FROM subject_number.subject_claim claim");
            AtomicInteger writes = new AtomicInteger(); var previous = fixture.dsl.configuration().executeListenerProviders();
            fixture.dsl.configuration().set(new DefaultExecuteListener() {
                @Override public void executeStart(ExecuteContext context) {
                    if (context.sql().startsWith("UPDATE subject_number.") || context.sql().startsWith("INSERT INTO subject_number.") || context.sql().startsWith("DELETE FROM subject_number.")) writes.incrementAndGet();
                }
            });
            try { code(fixture.post(seed, "new-" + corruption, 500), "INTERNAL"); }
            finally { fixture.dsl.configuration().set(previous); }
            assertThat(writes).hasValue(0); assertThat(fixture.owner(seed)).isEqualTo(before);
            assertThat(fixture.value("SELECT updated_at::text FROM subject_number.business_subject WHERE id=?", seed.subject())).isEqualTo(updated);
            assertThat(fixture.value("SELECT jsonb_agg(to_jsonb(claim) ORDER BY id)::text FROM subject_number.subject_claim claim").equals(claims)).isTrue();
            assertThat(fixture.count("subject_number.subject_claim")).isEqualTo(corruption.equals("later") ? 2 : 1);
            for (String envelope : envelopes) assertThat(fixture.providers.opens(envelope)).isZero();
            assertThat(fixture.count("audit.audit_fact")).isEqualTo(1); fixture.audit("CLAIM_DATA_INTEGRITY", "DENIED", "NO_OPEN");
        }
    }
    @Test
    void concurrentExactKeyCallsHaveOneAcceptedWinner() throws Exception {
        var seed = fixture.seed(); var results = race(() -> fixture.post(seed, "race", 200), () -> fixture.post(seed, "race", 200));
        assertThat(results.get(0).equals(results.get(1))).isTrue(); assertThat(fixture.count("subject_number.subject_claim")).isEqualTo(1);
        assertThat(fixture.owner(seed)).isEqualTo(fixture.account + "|null|2"); fixture.cleanPool();
    }
    @Test
    void crossSubjectRequestKeyLoserRollsBackBeforeReplayConflict() throws Exception {
        var first = fixture.seed(); var second = fixture.seed(); var barrier = new CyclicBarrier(2); AtomicInteger insertAttempts = new AtomicInteger(), uniqueFailures = new AtomicInteger();
        var previous = fixture.dsl.configuration().executeListenerProviders();
        fixture.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext context) {
                if (context.sql().startsWith("INSERT INTO subject_number.subject_claim") && insertAttempts.incrementAndGet() <= 2) {
                    try { barrier.await(10, TimeUnit.SECONDS); } catch (Exception failure) { throw new IllegalStateException("Insert barrier failed"); }
                }
            }
            @Override public void exception(ExecuteContext context) { if (context.sqlException() != null && "23505".equals(context.sqlException().getSQLState())) uniqueFailures.incrementAndGet(); }
        });
        try {
            String firstBody = fixture.body(first, VerificationMethod.SMS, fixture.approve(first, fixture.account, VerificationMethod.SMS, "server-reference"));
            String secondBody = fixture.body(second, VerificationMethod.SMS, fixture.approve(second, fixture.other, VerificationMethod.SMS, "server-reference"));
            var responses = race(() -> fixture.post("personal", "global-race", firstBody), () -> fixture.post("other", "global-race", secondBody));
            assertThat(responses.stream().map(java.net.http.HttpResponse::statusCode)).containsExactlyInAnyOrder(200, 409); assertThat(uniqueFailures).hasValue(1);
            assertThat(fixture.count("subject_number.subject_claim")).isEqualTo(1);
            if (responses.getFirst().statusCode() == 200) {
                assertThat(fixture.owner(first)).isEqualTo(fixture.account + "|null|2");
                assertThat(fixture.owner(second)).isEqualTo("null|null|1");
                code(fixture.checked(responses.getLast(), 409), "CLAIM_REPLAY_CONFLICT");
            } else {
                assertThat(fixture.owner(second)).isEqualTo(fixture.other + "|null|2");
                assertThat(fixture.owner(first)).isEqualTo("null|null|1");
                code(fixture.checked(responses.getFirst(), 409), "CLAIM_REPLAY_CONFLICT");
            }
        } finally { fixture.dsl.configuration().set(previous); }
        fixture.cleanPool();
    }
    @Test
    void concurrentSameAccountDuplicateAndChangedContextSerialize() throws Exception {
        var seed = fixture.seed(); var results = race(() -> fixture.post(seed, "duplicate-a", 200), () -> fixture.post(seed, "duplicate-b", 200));
        assertThat(results.getFirst().equals(results.getLast())).isTrue(); assertThat(fixture.count("subject_number.subject_claim")).isEqualTo(1);
        String changed = fixture.body(seed, VerificationMethod.MANUAL_REVIEW, fixture.approve(seed, fixture.account, VerificationMethod.MANUAL_REVIEW, "changed"));
        var responses = race(() -> fixture.post("personal", "changed-a", changed), () -> fixture.post("personal", "changed-b", changed));
        assertThat(responses.stream().map(java.net.http.HttpResponse::statusCode)).containsExactly(409, 409);
        assertThat(fixture.value("SELECT count(*) FROM subject_number.subject_claim WHERE status='CONFLICT'")).isEqualTo("2"); assertThat(fixture.owner(seed)).isEqualTo(fixture.account + "|null|2");
    }
    @Test
    void twoAccountsClaimingUnownedSubjectHaveOneOwnerWinner() throws Exception {
        var seed = fixture.seed(); String personal = fixture.body(seed, VerificationMethod.SMS, fixture.approve(seed, fixture.account, VerificationMethod.SMS, "account-one"));
        String other = fixture.body(seed, VerificationMethod.SMS, fixture.approve(seed, fixture.other, VerificationMethod.SMS, "account-two"));
        var responses = race(() -> fixture.post("personal", "account-one", personal), () -> fixture.post("other", "account-two", other));
        assertThat(responses.stream().map(java.net.http.HttpResponse::statusCode)).containsExactlyInAnyOrder(200, 403);
        assertThat(fixture.value("SELECT count(*) FROM subject_number.subject_claim WHERE status='ACCEPTED'")).isEqualTo("1");
        assertThat(fixture.value("SELECT count(*) FROM subject_number.subject_claim WHERE status='CONFLICT'")).isEqualTo("1");
        assertThat(fixture.value("SELECT revision FROM subject_number.business_subject WHERE id=?", seed.subject())).isEqualTo("2");
    }
    @Test
    void riskBindingAndAccountWritersRespectBothSerialOrders() throws Exception {
        for (String mutation : List.of("risk", "binding", "account")) {
            fixture.reset(); var seed = fixture.seed();
            try (var connection = fixture.admin(); var executor = Executors.newSingleThreadExecutor()) {
                connection.setAutoCommit(false);
                String key = mutation.equals("account") ? "customer_iam.account:" + fixture.account : "subject_number.number:" + seed.hmac();
                execute(connection, "SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))", key);
                var future = executor.submit(() -> fixture.post(seed, "writer-first", mutation.equals("risk") ? 409 : mutation.equals("account") ? 403 : 422));
                awaitWaiter(); assertThat(future.isDone()).isFalse();
                if (mutation.equals("risk")) execute(connection, "INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source) VALUES (?,?,'RECYCLED','OPEN','CLAIM_CHECK')", UUID.randomUUID(), seed.binding());
                if (mutation.equals("binding")) execute(connection, "UPDATE subject_number.caller_number_binding SET status='DISABLED',revision=revision+1 WHERE id=?", seed.binding());
                if (mutation.equals("account")) execute(connection, "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account);
                connection.commit(); future.get(10, TimeUnit.SECONDS); assertThat(fixture.owner(seed)).isEqualTo("null|null|1");
            }
        }
        for (String mutation : List.of("risk", "binding", "account")) {
            fixture.reset(); var seed = fixture.seed(); fixture.post(seed, "claim-first", 200);
            if (mutation.equals("risk")) fixture.risk(seed, "OPEN");
            else try (var connection = fixture.admin()) {
                connection.setAutoCommit(false);
                execute(connection, "SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))", mutation.equals("account") ? "customer_iam.account:" + fixture.account : "subject_number.number:" + seed.hmac());
                if (mutation.equals("binding")) execute(connection, "UPDATE subject_number.caller_number_binding SET status='DISABLED',revision=revision+1 WHERE id=?", seed.binding());
                else execute(connection, "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account);
                connection.commit();
            }
            fixture.post(seed, "claim-first", mutation.equals("risk") ? 409 : mutation.equals("account") ? 403 : 422);
            assertThat(fixture.owner(seed)).isEqualTo(fixture.account + "|null|2"); assertThat(fixture.value("SELECT status FROM subject_number.subject_claim WHERE request_id='claim-first'")).isEqualTo("ACCEPTED");
        }
    }
    @Test
    void auditFailureAndConditionalOwnerMissRollBackWithoutSecretLeakage() throws Exception {
        for (boolean audit : List.of(false, true)) {
            fixture.reset(); var seed = fixture.seed(); String table = audit ? "audit.audit_fact" : "subject_number.business_subject";
            try (var connection = fixture.admin()) {
                execute(connection, "CREATE FUNCTION public.pa30_fail() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN " + (audit ? "RAISE EXCEPTION 'SECRET_CANARY' USING ERRCODE='23514';" : "RETURN NULL;") + " END $$");
                execute(connection, "CREATE TRIGGER pa30_failure BEFORE " + (audit ? "INSERT" : "UPDATE") + " ON " + table + " FOR EACH ROW EXECUTE FUNCTION public.pa30_fail()");
            }
            try {
                var response = fixture.post(seed, "SECRET_CANARY", audit ? 503 : 500); code(response, audit ? "SERVICE_UNAVAILABLE" : "INTERNAL");
                assertThat(fixture.count("subject_number.subject_claim")).isZero(); assertThat(fixture.count("audit.audit_fact")).isZero(); assertThat(fixture.owner(seed)).isEqualTo("null|null|1"); fixture.cleanPool();
            } finally { try (var connection = fixture.admin()) { execute(connection, "DROP TRIGGER pa30_failure ON " + table); execute(connection, "DROP FUNCTION public.pa30_fail()"); } }
        }
        assertThat(output.getAll().contains("SECRET_CANARY")).isFalse();
    }
    @Test
    void boundedWholeTransactionRetriesRestorePoolAfterFailure() throws Exception {
        for (String failure : List.of("40001", "40P01", "23505:uq_claim_request", "23505:uq_claim_accepted_subject_account")) {
            fixture.reset(); var seed = fixture.seed(); int before = fixture.providers.verifications.get();
            String state = failure.split(":")[0];
            String constraint = failure.contains(":") ? ",CONSTRAINT='" + failure.split(":")[1] + "'" : "";
            try (var connection = fixture.admin()) {
                execute(connection, "CREATE SEQUENCE public.pa30_attempts");
                execute(connection, "CREATE FUNCTION public.pa30_retry() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER AS $$ BEGIN PERFORM nextval('public.pa30_attempts'); RAISE EXCEPTION 'SECRET_CANARY' USING ERRCODE='" + state + "'" + constraint + "; END $$");
                execute(connection, "CREATE TRIGGER pa30_retry BEFORE INSERT ON subject_number.subject_claim FOR EACH ROW EXECUTE FUNCTION public.pa30_retry()");
            }
            try {
                code(fixture.post(seed, "retry", 503), "SERVICE_UNAVAILABLE"); assertThat(fixture.value("SELECT last_value FROM public.pa30_attempts")).isEqualTo("3");
                assertThat(fixture.providers.verifications.get() - before).isEqualTo(3); assertThat(fixture.count("subject_number.subject_claim")).isZero(); assertThat(fixture.count("audit.audit_fact")).isZero();
                assertThat(fixture.owner(seed)).isEqualTo("null|null|1"); fixture.cleanPool();
            } finally { try (var connection = fixture.admin()) { execute(connection, "DROP TRIGGER pa30_retry ON subject_number.subject_claim"); execute(connection, "DROP FUNCTION public.pa30_retry()"); execute(connection, "DROP SEQUENCE public.pa30_attempts"); } }
            fixture.post(seed, "retry", 200); fixture.cleanPool();
        }
    }
    private static <T> List<T> race(Callable<T> first, Callable<T> second) throws Exception {
        fixture.pool.setMaximumPoolSize(2); var gate = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> { assertThat(gate.await(10, TimeUnit.SECONDS)).isTrue(); return first.call(); });
            var two = executor.submit(() -> { assertThat(gate.await(10, TimeUnit.SECONDS)).isTrue(); return second.call(); });
            gate.countDown(); return List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS));
        } finally { fixture.pool.setMaximumPoolSize(1); }
    }
    private static void awaitWaiter() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (Integer.parseInt(fixture.value("SELECT count(*) FROM pg_stat_activity WHERE usename=? AND wait_event='advisory'", fixture.login)) > 0) return;
            Thread.onSpinWait();
        }
        fail("Expected transaction advisory waiter");
    }
}
