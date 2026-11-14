package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import com.onecall.core.activation_consent.internal.operations.*;
import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.*;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OperationsSubjectLifecycleIT {
    OperationsSubjectLifecycleFixture fixture;
    @BeforeAll void start() throws Exception { fixture = new OperationsSubjectLifecycleFixture(); fixture.start(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @AfterEach void clean() throws Exception { fixture.cleanPool(); }
    @AfterAll void stop() { if (fixture != null) fixture.close(); }
    void oneSet() throws Exception { assertThat(fixture.effects()).containsExactly(1L, 1L, 1L); }
    void denied(Runnable work, Code code) { assertThatThrownBy(work::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(code)); }

    @Test
    void suspendActiveSubjectCommitsOneRevisionTransitionOutboxAndAudit() throws Exception {
        var business = fixture.business(); var response = fixture.suspend("suspend"); fixture.assertFactSet(response, "suspend", "ACTIVE", "SUSPENDED", 2); oneSet(); assertThat(fixture.business()).isEqualTo(business);
    }
    @Test
    void resumeSuspendedSubjectCommitsOneRevisionTransitionOutboxAndAudit() throws Exception {
        fixture.suspend("suspend"); var business = fixture.business(); var response = fixture.resume("resume"); fixture.assertFactSet(response, "resume", "SUSPENDED", "ACTIVE", 3); assertThat(fixture.effects()).containsExactly(2L, 2L, 2L); assertThat(fixture.business()).isEqualTo(business);
    }
    @Test
    void sameSuspendActionReplaysOriginalWithoutAnyWrite() throws Exception {
        var first = fixture.suspend("same"); String facts = fixture.allFacts(); var business = fixture.business(); fixture.statements.clear(); var replay = fixture.suspend("same"); assertThat(fixture.json(replay)).isEqualTo(fixture.json(first)); assertThat(fixture.allFacts()).isEqualTo(facts); assertThat(fixture.business()).isEqualTo(business); oneSet(); assertThat(fixture.statements.stream().map(OperationsSubjectLifecycleFixture.StatementFact::sql)).noneMatch(sql -> sql.startsWith("UPDATE ") || sql.startsWith("INSERT ") || sql.contains("outbox_append") || sql.contains("append_fact"));
    }
    @Test
    void sameResumeActionReplaysOriginalWithoutAnyWrite() throws Exception {
        fixture.suspend("suspend"); var first = fixture.resume("same"); String facts = fixture.allFacts(); var business = fixture.business(); var replay = fixture.resume("same"); assertThat(fixture.json(replay)).isEqualTo(fixture.json(first)); assertThat(fixture.allFacts()).isEqualTo(facts); assertThat(fixture.business()).isEqualTo(business); assertThat(fixture.effects()).containsExactly(2L, 2L, 2L);
    }
    @Test
    void historicalReplayAfterOppositeTransitionReturnsOriginalResult() throws Exception {
        var first = fixture.suspend("first"); fixture.resume("opposite"); String before = fixture.allFacts(); assertThat(fixture.json(fixture.suspend("first"))).isEqualTo(fixture.json(first)); assertThat(fixture.allFacts()).isEqualTo(before);
    }
    @Test
    void changedReasonAndExpectedRevisionConflictWithoutDuplicateFacts() throws Exception {
        fixture.suspend("same"); String facts = fixture.allFacts(); fixture.problem(fixture.post("employee", fixture.subject, "suspensions", "same", "changed", BigInteger.TWO), 409, "ACTIVATION_STATE_CONFLICT"); fixture.problem(fixture.post("employee", fixture.subject, "suspensions", "same", "reason", BigInteger.valueOf(3)), 409, "ACTIVATION_STATE_CONFLICT"); assertThat(fixture.allFacts()).isEqualTo(facts); oneSet();
    }
    @Test
    void changedActorSubjectAndRouteUnderSameKeyConflict() throws Exception {
        fixture.suspend("same"); UUID other = fixture.customer.subject(fixture.customer.other, null); fixture.customer.service.activate(fixture.customer.command(other, "other-original", com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.Decision.ACCEPT), new com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.ActivationCaller(fixture.customer.other, com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.Channel.WEB, com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.WorkspaceType.PERSONAL, null));
        String before = fixture.allFacts();
        fixture.problem(fixture.post("second", fixture.subject, "suspensions", "same", "reason", BigInteger.TWO), 409, "ACTIVATION_STATE_CONFLICT");
        fixture.problem(fixture.post("employee", other, "suspensions", "same", "reason", BigInteger.TWO), 409, "ACTIVATION_STATE_CONFLICT");
        fixture.problem(fixture.post("employee", fixture.subject, "resumptions", "same", "reason", BigInteger.TWO), 409, "ACTIVATION_STATE_CONFLICT"); assertThat(fixture.allFacts()).isEqualTo(before); oneSet();
    }
    @Test
    void missingSubjectAndMissingActivationAreNotFound() throws Exception {
        for (UUID subject : List.of(UUID.randomUUID(), fixture.customer.subject(fixture.customer.account, null))) fixture.problem(fixture.post("employee", subject, "suspensions", "missing", "reason", BigInteger.ONE), 404, "NOT_FOUND"); assertThat(fixture.effects()).containsOnly(0L);
    }
    @Test
    void inactiveAndWrongCurrentStatesConflict() throws Exception {
        fixture.problem(fixture.resume("resume"), 409, "ACTIVATION_STATE_CONFLICT"); UUID inactive = fixture.customer.subject(fixture.customer.account, null); fixture.sql("INSERT INTO activation_consent.service_activation(id,subject_id,status,revision) VALUES (?,?,'INACTIVE',1)", UUID.randomUUID(), inactive);
        fixture.problem(fixture.post("employee", inactive, "suspensions", "inactive", "reason", BigInteger.ONE), 409, "ACTIVATION_STATE_CONFLICT"); fixture.suspend("first"); String before = fixture.allFacts(); fixture.problem(fixture.suspend("second"), 409, "ACTIVATION_STATE_CONFLICT"); assertThat(fixture.allFacts()).isEqualTo(before); oneSet();
    }
    @Test
    void staleAndUnrepresentableExpectedRevisionsConflict() throws Exception {
        String before = fixture.allFacts(); for (BigInteger revision : List.of(BigInteger.ONE, BigInteger.valueOf(Integer.MAX_VALUE), new BigInteger("99999999999999999999999"))) fixture.problem(fixture.post("employee", fixture.subject, "suspensions", "stale", "reason", revision), 409, "ACTIVATION_STATE_CONFLICT"); assertThat(fixture.allFacts()).isEqualTo(before);
    }
    @Test
    void missingDisabledAndNonOpsEmployeesCannotMutateOrReplay() throws Exception {
        fixture.suspend("same"); String before = fixture.allFacts(); for (String token : List.of("missing", "disabled", "admin")) { fixture.problem(fixture.post(token, fixture.subject, "suspensions", "same", "reason", BigInteger.TWO), token.equals("admin") ? 403 : 401, token.equals("admin") ? "FORBIDDEN" : "UNAUTHENTICATED"); fixture.problem(fixture.post(token, UUID.randomUUID(), "suspensions", "new", "reason", BigInteger.TWO), token.equals("admin") ? 403 : 401, token.equals("admin") ? "FORBIDDEN" : "UNAUTHENTICATED"); } assertThat(fixture.allFacts()).isEqualTo(before);
    }
    @Test
    void wrongAudienceAndCustomerCredentialNeverReachOwnerWrites() throws Exception {
        for (String token : List.of("customer", "personal", "invalid", "expired", "revoked")) fixture.problem(fixture.post(token, fixture.subject, "suspensions", "denied", "reason", BigInteger.TWO), 401, "UNAUTHENTICATED"); assertThat(fixture.effects()).containsOnly(0L); assertThat(fixture.statements).isEmpty();
    }
    @Test
    void absentAmbiguousAndUnusableVerifierFailClosed() throws Exception {
        var beans = (DefaultListableBeanFactory) fixture.context.getBeanFactory(); beans.destroySingleton("syntheticEmployeeSessions"); var definition = beans.getBeanDefinition("syntheticEmployeeSessions"); beans.removeBeanDefinition("syntheticEmployeeSessions");
        try {
            fixture.problem(fixture.suspend("absent"), 503, "SERVICE_UNAVAILABLE");
            beans.registerSingleton("syntheticEmployeeSessions", fixture.sessions); beans.registerSingleton("secondVerifier", new com.onecall.archtests.support.SyntheticEmployeeSessions());
            fixture.problem(fixture.suspend("ambiguous"), 503, "SERVICE_UNAVAILABLE"); beans.destroySingleton("secondVerifier"); fixture.sessions.failure = new IllegalStateException("SECRET SQL"); fixture.problem(fixture.suspend("unusable"), 503, "SERVICE_UNAVAILABLE");
        } finally { beans.destroySingleton("secondVerifier"); beans.destroySingleton("syntheticEmployeeSessions"); beans.registerBeanDefinition("syntheticEmployeeSessions", definition); beans.registerSingleton("syntheticEmployeeSessions", fixture.sessions); fixture.sessions.failure = null; }
        assertThat(fixture.effects()).containsOnly(0L);
    }
    @Test
    void maximumActionAndReasonRoundTripWithoutTruncation() throws Exception {
        String key = "k".repeat(128), reason = "原因😀".repeat(21) + "x"; var response = fixture.post("employee", fixture.subject, "suspensions", key, reason, BigInteger.TWO); fixture.assertFactSet(response, key, "ACTIVE", "SUSPENDED", 2); assertThat(fixture.json(response).path("reasonCode").asString()).isEqualTo(reason); oneSet();
    }
    @Test
    void concurrentSameSuspendActionConvergesOnOneFactSet() throws Exception {
        var results = fixture.concurrent(fixture.subject, "suspensions", "same", 2, fixture.subject, "suspensions", "same", 2); assertThat(results).extracting(java.net.http.HttpResponse::statusCode).containsOnly(200); assertThat(fixture.json(results.getFirst())).isEqualTo(fixture.json(results.getLast())); oneSet();
    }
    @Test
    void concurrentSameResumeActionConvergesOnOneFactSet() throws Exception {
        fixture.suspend("suspend"); var results = fixture.concurrent(fixture.subject, "resumptions", "same", 3, fixture.subject, "resumptions", "same", 3); assertThat(results).extracting(java.net.http.HttpResponse::statusCode).containsOnly(200); assertThat(fixture.json(results.getFirst())).isEqualTo(fixture.json(results.getLast())); assertThat(fixture.effects()).containsExactly(2L, 2L, 2L);
    }
    @Test
    void concurrentDistinctActionsAtOneRevisionHaveOneWinner() throws Exception {
        var results = fixture.concurrent(fixture.subject, "suspensions", "left", 2, fixture.subject, "suspensions", "right", 2); assertThat(results).extracting(java.net.http.HttpResponse::statusCode).containsExactlyInAnyOrder(200, 409); oneSet();
    }
    @Test
    void concurrentCrossSubjectSameKeyHasOneWinnerAndOneConflict() throws Exception {
        UUID other = fixture.customer.subject(fixture.customer.account, null); fixture.customer.service.activate(fixture.customer.command(other, "other-original", com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.Decision.ACCEPT), fixture.customer.caller());
        var results = fixture.concurrent(fixture.subject, "suspensions", "collision", 2, other, "suspensions", "collision", 2); assertThat(results).extracting(java.net.http.HttpResponse::statusCode).containsExactlyInAnyOrder(200, 409); oneSet();
    }
    @Test
    void employeeRevocationSerializesWithLockedCurrentAuthority() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor(); var blocker = fixture.admin()) {
            blocker.setAutoCommit(false); execute(blocker, "SELECT pg_advisory_xact_lock(hashtextextended('operations_iam.admin_mutation',0))"); execute(blocker, "SELECT id FROM operations_iam.employee_account WHERE id=? FOR UPDATE", fixture.employee);
            var pending = executor.submit(() -> fixture.suspend("revoked")); fixture.awaitWaiters(1, "advisory"); execute(blocker, "UPDATE operations_iam.employee_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.employee); blocker.commit(); fixture.problem(pending.get(30, TimeUnit.SECONDS), 401, "UNAUTHENTICATED");
        } assertThat(fixture.effects()).containsOnly(0L);
        fixture.sql("UPDATE operations_iam.employee_account SET status='ACTIVE',revision=revision+1 WHERE id=?", fixture.employee);
        String before = fixture.allFacts();
        var employeeRead = new CountDownLatch(1); var release = new CountDownLatch(1);
        var operationsBackend = new java.util.concurrent.atomic.AtomicReference<String>();
        var listeners = fixture.dsl.configuration().executeListenerProviders();
        var pause = new org.jooq.impl.DefaultExecuteListener() {
            @Override public void resultEnd(org.jooq.ExecuteContext execution) {
                if (execution.sql().contains("employee_account WHERE id=? FOR UPDATE")) {
                    assertThat(execution.result()).hasSize(1);
                    assertThat(execution.result().getFirst().get("status", String.class)).isEqualTo("ACTIVE");
                    assertThat(execution.result().getFirst().get("role", String.class)).isEqualTo("OPS");
                    try {
                        operationsBackend.set(CustomerBusinessNumbersFixture.one(execution.connection(), "SELECT pg_backend_pid()"));
                        employeeRead.countDown(); assertThat(release.await(30, TimeUnit.SECONDS)).isTrue();
                    } catch (SQLException failure) { throw new IllegalStateException("Fixture backend capture failed", failure); }
                    catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException("Fixture employee read interrupted", failure); }
                }
            }
        };
        var pauseListeners = Arrays.copyOf(listeners, listeners.length + 1); pauseListeners[listeners.length] = new org.jooq.impl.DefaultExecuteListenerProvider(pause);
        fixture.dsl.configuration().set(pauseListeners);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor(); var disable = fixture.admin()) {
            try {
                disable.setAutoCommit(false); String disableBackend = CustomerBusinessNumbersFixture.one(disable, "SELECT pg_backend_pid()");
                var pending = executor.submit(() -> fixture.suspend("operations-first"));
                assertThat(employeeRead.await(10, TimeUnit.SECONDS)).as("Operations read current employee facts under all owner locks").isTrue();
                assertThat(fixture.allFacts()).isEqualTo(before); assertThat(fixture.effects()).containsOnly(0L);
                try (var probe = fixture.admin()) {
                    probe.setAutoCommit(false);
                    assertThat(CustomerBusinessNumbersFixture.one(probe, "SELECT pg_try_advisory_xact_lock(hashtextextended('subject_number.subject:" + fixture.subject + "',0))")).isEqualTo("f");
                    assertThatThrownBy(() -> execute(probe, "SELECT id FROM activation_consent.service_activation WHERE subject_id=? FOR UPDATE NOWAIT", fixture.subject))
                            .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("55P03"));
                    probe.rollback();
                    assertThatThrownBy(() -> execute(probe, "SELECT id FROM operations_iam.employee_account WHERE id=? FOR UPDATE NOWAIT", fixture.employee))
                            .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("55P03"));
                    probe.rollback();
                }
                var revocation = executor.submit(() -> {
                    execute(disable, "SELECT pg_advisory_xact_lock(hashtextextended('operations_iam.admin_mutation',0))");
                    execute(disable, "SELECT id FROM operations_iam.employee_account WHERE id=? FOR UPDATE", fixture.employee);
                    execute(disable, "UPDATE operations_iam.employee_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.employee);
                    return null;
                });
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15); boolean waiting = false;
                while (System.nanoTime() < deadline) {
                    if (fixture.scalar("SELECT count(*) FROM pg_locks WHERE pid=" + disableBackend + " AND NOT granted AND locktype='advisory' AND " + operationsBackend.get() + "=ANY(pg_blocking_pids(pid))").equals("1")) { waiting = true; break; }
                    Thread.onSpinWait();
                }
                assertThat(waiting).as("disable waits on Operations administrator lock on the identified backend").isTrue();
                assertThat(revocation.isDone()).isFalse(); assertThat(fixture.allFacts()).isEqualTo(before);
                assertThat(fixture.scalar("SELECT status FROM operations_iam.employee_account WHERE id='" + fixture.employee + "'")).isEqualTo("ACTIVE");
                release.countDown();
                fixture.assertFactSet(pending.get(30, TimeUnit.SECONDS), "operations-first", "ACTIVE", "SUSPENDED", 2); oneSet();
                revocation.get(30, TimeUnit.SECONDS); disable.commit();
            } finally { release.countDown(); fixture.dsl.configuration().set(listeners); }
        }
        assertThat(fixture.scalar("SELECT status FROM operations_iam.employee_account WHERE id='" + fixture.employee + "'")).isEqualTo("DISABLED");
        String committed = fixture.allFacts(); var business = fixture.business();
        fixture.problem(fixture.suspend("operations-first"), 401, "UNAUTHENTICATED");
        assertThat(fixture.allFacts()).isEqualTo(committed); assertThat(fixture.business()).isEqualTo(business); oneSet();
    }
    @Test
    void activationAndCustomerCommandRacePreservesOneAuthoritativeHead() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor(); var blocker = fixture.admin()) {
            blocker.setAutoCommit(false); execute(blocker, "SELECT pg_advisory_xact_lock(hashtextextended(?,0))", "subject_number.subject:" + fixture.subject);
            var start = new CyclicBarrier(3); var operations = executor.submit(() -> { start.await(10, TimeUnit.SECONDS); return fixture.suspend("race"); });
            var customer = executor.submit(() -> { start.await(10, TimeUnit.SECONDS); try { return (Object) fixture.customer.accept("customer-race"); } catch (RuntimeException failure) { return failure; } });
            start.await(10, TimeUnit.SECONDS); long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15); boolean observed = false;
            while (System.nanoTime() < deadline) { if (Integer.parseInt(fixture.scalar("SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.usename IN ('" + fixture.login + "','" + fixture.customer.login + "') AND NOT l.granted AND l.locktype='advisory'")) == 2) { observed = true; break; } Thread.onSpinWait(); }
            assertThat(observed).isTrue(); blocker.commit(); assertThat(operations.get(30, TimeUnit.SECONDS).statusCode()).isEqualTo(200); Object result = customer.get(30, TimeUnit.SECONDS);
            if (result instanceof com.onecall.core.activation_consent.customeractivation.SubjectActivationException failure) assertThat(failure.code()).isEqualTo(com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code.FORBIDDEN);
            else assertThat(result).isInstanceOf(com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.ActivationResult.class);
        } assertThat(fixture.scalar("SELECT count(*) FROM activation_consent.service_activation WHERE subject_id='" + fixture.subject + "' AND status='SUSPENDED' AND revision=3")).isEqualTo("1"); oneSet();
    }
    @Test
    void resumeAndSuspendNeverChangeTrialConsentOrOtherBusinessFacts() throws Exception {
        var original = fixture.business(); fixture.suspend("suspend"); fixture.resume("resume"); fixture.suspend("suspend"); assertThat(fixture.business()).isEqualTo(original);
        UUID noTrial = fixture.customer.subject(fixture.customer.account, null), activation = UUID.randomUUID();
        String status = com.onecall.core.activation_consent.internal.command.ActivationSemantics.statusEvent(activation, 3);
        String fingerprint = OperationsLifecycleSemantics.fingerprint(fixture.command(noTrial, "seed-no-trial", "reason", BigInteger.TWO), fixture.caller(), "SUSPENDED", status);
        fixture.sql("WITH head AS (INSERT INTO activation_consent.service_activation(id,subject_id,status,revision) VALUES (?,?,'SUSPENDED',3) RETURNING updated_at) INSERT INTO activation_consent.service_activation_transition(id,activation_id,subject_id,from_status,to_status,from_revision,to_revision,source_channel,source_action_domain,source_action_event_id,status_event_domain,status_event_id,request_fingerprint,actor_employee_id,reason_code,occurred_at) SELECT ?,?,?,'ACTIVE','SUSPENDED',2,3,'OPERATIONS','OPERATIONS','seed-no-trial','ACTIVATION_STATUS',?,?,?,'reason',updated_at FROM head", activation, noTrial, UUID.randomUUID(), activation, noTrial, status, fingerprint, fixture.employee);
        var noTrialFacts = fixture.business();
        assertThat(fixture.post("employee", noTrial, "resumptions", "no-trial-resume", "reason", BigInteger.valueOf(3)).statusCode()).isEqualTo(200); assertThat(fixture.post("employee", noTrial, "suspensions", "no-trial-suspend", "reason", BigInteger.valueOf(4)).statusCode()).isEqualTo(200); assertThat(fixture.business()).isEqualTo(noTrialFacts);
        assertThat(fixture.statements.stream().map(OperationsSubjectLifecycleFixture.StatementFact::sql)).noneMatch(sql -> sql.contains("issue_first_trial") || sql.contains("entitlement_metering."));
    }
    @Test
    void auditFailureRollsBackHeadTransitionAndOutbox() throws Exception {
        String before = fixture.allFacts(); denied(() -> fixture.with(fixture.failAfter("appendAudit")).suspend(fixture.command(fixture.subject, "fault", "reason", BigInteger.TWO), fixture.caller()), Code.INTERNAL); assertThat(fixture.allFacts()).isEqualTo(before); assertThat(fixture.effects()).containsOnly(0L);
    }
    @Test
    void transitionOutboxAndCommitFailuresCannotPartiallySucceed() throws Exception {
        String before = fixture.allFacts(); for (String stage : List.of("advance", "appendTransition", "appendOutbox")) { denied(() -> fixture.with(fixture.failAfter(stage)).suspend(fixture.command(fixture.subject, stage, "reason", BigInteger.TWO), fixture.caller()), Code.INTERNAL); assertThat(fixture.allFacts()).isEqualTo(before); }
        denied(() -> fixture.commitFailure().suspend(fixture.command(fixture.subject, "commit", "reason", BigInteger.TWO), fixture.caller()), Code.SERVICE_UNAVAILABLE); assertThat(fixture.allFacts()).isEqualTo(before);
        fixture.suspend("lost-response"); String committed = fixture.allFacts(); assertThat(fixture.suspend("lost-response").statusCode()).isEqualTo(200); assertThat(fixture.allFacts()).isEqualTo(committed); oneSet();
    }
    @Test
    void responsesProblemsAndEventsRespectSensitiveOutputBoundary() throws Exception {
        var success = fixture.suspend("safe"); fixture.assertFactSet(success, "safe", "ACTIVE", "SUSPENDED", 2); fixture.sessions.failure = new IllegalStateException("SECRET SQL PASSWORD_CANARY"); var error = fixture.suspend("fail"); fixture.problem(error, 503, "SERVICE_UNAVAILABLE"); assertThat(success.body() + error.body()).doesNotContain(fixture.password, "SECRET", "SQL", "PASSWORD_CANARY", "actor_employee_id", "request_fingerprint", "<html");
        String payload = fixture.scalar("SELECT payload_redacted::text FROM platform.outbox_event WHERE payload_redacted->>'sourceActionEventId'='safe'"); var json = tools.jackson.databind.json.JsonMapper.builder().build().readTree(payload); assertThat(json.propertyNames()).containsExactlyInAnyOrder("activationId", "transitionId", "subjectId", "fromStatus", "toStatus", "fromRevision", "toRevision", "sourceChannel", "sourceActionDomain", "sourceActionEventId", "statusEventDomain", "statusEventId", "occurredAt"); assertThat(payload).doesNotContain("employeeId", "reasonCode", fixture.password);
    }
}
