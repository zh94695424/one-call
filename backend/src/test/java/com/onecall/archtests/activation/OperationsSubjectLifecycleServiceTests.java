package com.onecall.archtests.activation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.onecall.core.activation_consent.internal.command.ActivationSemantics;
import com.onecall.core.activation_consent.internal.operations.*;
import com.onecall.core.activation_consent.internal.operations.OperationsLifecycleRepository.*;
import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock;
import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock.EmployeeFacts;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.*;
import java.math.BigInteger;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class OperationsSubjectLifecycleServiceTests {
    static final UUID SUBJECT = UUID.randomUUID(), EMPLOYEE = UUID.randomUUID(), ACTIVATION = UUID.randomUUID();
    static final Caller CALLER = new Caller(EMPLOYEE, "onecall:employee-session:v1");
    static final Instant TIME = Instant.parse("2026-11-13T00:00:00Z");
    static Command command(String action, int revision) { return new Command(SUBJECT, action, "reason", BigInteger.valueOf(revision)); }
    static Transition persistedWitness(String channel, String actionDomain, String statusEvent, String fingerprint, Instant occurredAt) {
        if ("OPERATIONS".equals(actionDomain)) return new Transition(UUID.randomUUID(), ACTIVATION, SUBJECT, "SUSPENDED", "ACTIVE", 1, 2, channel, actionDomain, "original", "ACTIVATION_STATUS", statusEvent, fingerprint, null, EMPLOYEE, "CLIENT_ACTIVATION_ACCEPTED", null, occurredAt);
        return new Transition(UUID.randomUUID(), ACTIVATION, SUBJECT, "INACTIVE", "ACTIVE", 1, 2, channel, actionDomain, "original", "ACTIVATION_STATUS", statusEvent, fingerprint, UUID.randomUUID(), null, "CLIENT_ACTIVATION_ACCEPTED", UUID.randomUUID(), occurredAt);
    }
    static final class Harness {
        final DSLContext context = mock(DSLContext.class);
        final OperationsLifecycleRepository repository = mock(OperationsLifecycleRepository.class);
        final EmployeeAdministratorLock employee = mock(EmployeeAdministratorLock.class);
        final Connection connection = mock(Connection.class);
        final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        final DefaultOperationsSubjectLifecycleService service;
        final List<Transition> appended = new ArrayList<>();
        final Map<String, Transition> actions = new HashMap<>();
        final List<String> order = new ArrayList<>();
        Head head = new Head(ACTIVATION, SUBJECT, "ACTIVE", 2, TIME.minusSeconds(10), TIME.minusSeconds(1));
        Transition witness = new Transition(UUID.randomUUID(), ACTIVATION, SUBJECT, "INACTIVE", "ACTIVE", 1, 2, "WEB", "ACTIVATION", "original", "ACTIVATION_STATUS", ActivationSemantics.statusEvent(ACTIVATION, 2), "a".repeat(64), UUID.randomUUID(), null, "CLIENT_ACTIVATION_ACCEPTED", UUID.randomUUID(), TIME.minusSeconds(1));
        boolean exists = true;
        int count = 1, outbox, audit;
        Harness() throws Exception {
            var source = mock(DataSource.class); when(source.getConnection()).thenReturn(connection); when(connection.getAutoCommit()).thenReturn(true);
            var manager = new DataSourceTransactionManager(source); manager.setRollbackOnCommitFailure(true);
            var transaction = new TransactionTemplate(manager); transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            beans.registerSingleton("employee", employee);
            when(employee.lockEmployeeAdministrator(EMPLOYEE)).thenAnswer(call -> { order.add("employee"); return Optional.of(new EmployeeFacts(EMPLOYEE, "ACTIVE", "OPS", 1)); });
            doAnswer(call -> { order.add("enter"); return null; }).when(repository).enter(context);
            doAnswer(call -> { order.add("role"); return null; }).when(repository).assertRole(context);
            doAnswer(call -> { order.add("subject"); return null; }).when(repository).lockSubject(context, SUBJECT);
            when(repository.lockHead(context, SUBJECT)).thenAnswer(call -> { order.add("head"); return Optional.ofNullable(head); });
            when(repository.subjectExists(context, SUBJECT)).thenAnswer(call -> { order.add("exists"); return exists; });
            when(repository.actionTransition(eq(context), anyString())).thenAnswer(call -> Optional.ofNullable(actions.get(call.getArgument(1))));
            when(repository.latestTransition(eq(context), any())).thenAnswer(call -> Optional.ofNullable(witness));
            when(repository.decisionTime(context)).thenReturn(TIME);
            when(repository.advance(eq(context), any(), anyString(), any())).thenAnswer(call -> { order.add("advance"); Head before = call.getArgument(1); if (count == 1) head = new Head(before.id(), before.subjectId(), call.getArgument(2), before.revision() + 1, before.createdAt(), call.getArgument(3)); return count; });
            doAnswer(call -> { order.add("transition"); Transition row = call.getArgument(1); appended.add(row); actions.put(row.actionEvent(), row); witness = row; return null; }).when(repository).appendTransition(eq(context), any());
            doAnswer(call -> { order.add("outbox"); outbox++; return null; }).when(repository).appendOutbox(eq(context), any());
            doAnswer(call -> { order.add("audit"); audit++; return null; }).when(repository).appendAudit(eq(context), any());
            service = new DefaultOperationsSubjectLifecycleService(transaction, context, repository, beans.getBeanProvider(EmployeeAdministratorLock.class));
        }
        void suspended() { head = new Head(ACTIVATION, SUBJECT, "SUSPENDED", 3, head.createdAt(), TIME.minusSeconds(1)); String status = ActivationSemantics.statusEvent(ACTIVATION, 3); witness = new Transition(UUID.randomUUID(), ACTIVATION, SUBJECT, "ACTIVE", "SUSPENDED", 2, 3, "OPERATIONS", "OPERATIONS", "prior", "ACTIVATION_STATUS", status, OperationsLifecycleSemantics.fingerprint(command("prior", 2), CALLER, "SUSPENDED", status), null, EMPLOYEE, "reason", null, head.updatedAt()); }
        void denied(Command command, Caller caller, Code code) { assertThatThrownBy(() -> service.suspend(command, caller)).isInstanceOfSatisfying(Failure.class, failure -> { assertThat(failure.code()).isEqualTo(code); assertThat(failure.getCause()).isNull(); }); }
        void noWrites() { verify(repository, never()).advance(any(), any(), anyString(), any()); verify(repository, never()).appendTransition(any(), any()); verify(repository, never()).appendOutbox(any(), any()); verify(repository, never()).appendAudit(any(), any()); }
    }

    @Test
    void suspendAdvancesOnlyActiveHeadAndAppendsOneFactSet() throws Exception {
        var harness = new Harness(); var result = harness.service.suspend(command("action", 2), CALLER);
        assertThat(result.fromStatus()).isEqualTo("ACTIVE"); assertThat(result.toStatus()).isEqualTo("SUSPENDED"); assertThat(result.fromRevision()).isEqualTo(2); assertThat(result.toRevision()).isEqualTo(3);
        assertThat(harness.appended).hasSize(1); assertThat(harness.outbox).isEqualTo(1); assertThat(harness.audit).isEqualTo(1); verify(harness.connection).commit();
    }
    @Test
    void resumeAdvancesOnlySuspendedHeadAndAppendsOneFactSet() throws Exception {
        var harness = new Harness(); harness.suspended(); var result = harness.service.resume(command("resume", 3), CALLER); assertThat(result.toStatus()).isEqualTo("ACTIVE"); assertThat(result.toRevision()).isEqualTo(4); assertThat(harness.appended).hasSize(1); assertThat(harness.outbox).isEqualTo(1); assertThat(harness.audit).isEqualTo(1);
    }
    @Test
    void currentActiveOpsIsRequiredBeforeExistenceOrReplay() throws Exception {
        for (var facts : Arrays.asList(null, new EmployeeFacts(EMPLOYEE, "DISABLED", "OPS", 1), new EmployeeFacts(EMPLOYEE, "ACTIVE", "ADMIN", 1), new EmployeeFacts(UUID.randomUUID(), "ACTIVE", "OPS", 1), new EmployeeFacts(EMPLOYEE, "UNKNOWN", "OPS", 1), new EmployeeFacts(EMPLOYEE, "ACTIVE", "BAD", 1), new EmployeeFacts(EMPLOYEE, "ACTIVE", "OPS", 0))) {
            var harness = new Harness(); harness.exists = false; when(harness.employee.lockEmployeeAdministrator(EMPLOYEE)).thenReturn(Optional.ofNullable(facts));
            Code code = facts == null || "DISABLED".equals(facts.status()) ? Code.UNAUTHENTICATED : "ADMIN".equals(facts.role()) ? Code.FORBIDDEN : Code.SERVICE_UNAVAILABLE;
            harness.denied(command("action", 2), CALLER, code); harness.noWrites(); verify(harness.repository, never()).actionTransition(any(), anyString());
        }
    }
    @Test
    void missingSubjectOrActivationReturnsNotFoundWithoutCreation() throws Exception {
        for (boolean missingHead : List.of(false, true)) { var harness = new Harness(); if (missingHead) harness.head = null; else harness.exists = false; harness.denied(command("action", 2), CALLER, Code.NOT_FOUND); harness.noWrites(); verify(harness.employee).lockEmployeeAdministrator(EMPLOYEE); }
    }
    @Test
    void wrongStateAndStaleRevisionConflictWithoutWrites() throws Exception {
        for (int revision : List.of(1, 3, Integer.MAX_VALUE)) { var harness = new Harness(); harness.denied(command("action", revision), CALLER, Code.ACTIVATION_STATE_CONFLICT); harness.noWrites(); }
        var harness = new Harness(); harness.head = new Head(ACTIVATION, SUBJECT, "INACTIVE", 1, TIME, TIME); harness.denied(command("action", 1), CALLER, Code.ACTIVATION_STATE_CONFLICT); harness.noWrites();
        harness = new Harness(); harness.denied(new Command(SUBJECT, "action", "reason", new BigInteger("999999999999999999999")), CALLER, Code.ACTIVATION_STATE_CONFLICT); harness.noWrites();
    }
    @Test
    void exactActionReplayReturnsOriginalBeforeCurrentPredicate() throws Exception {
        var harness = new Harness(); var original = harness.service.suspend(command("action", 2), CALLER); var replay = harness.service.suspend(command("action", 2), CALLER); assertThat(replay).isEqualTo(original); assertThat(harness.appended).hasSize(1); assertThat(harness.outbox).isEqualTo(1); assertThat(harness.audit).isEqualTo(1); verify(harness.repository, times(1)).advance(any(), any(), anyString(), any());
    }
    @Test
    void changedActorSubjectRouteReasonOrExpectedRevisionConflicts() throws Exception {
        var harness = new Harness(); harness.service.suspend(command("action", 2), CALLER);
        harness.denied(new Command(SUBJECT, "action", "changed", BigInteger.TWO), CALLER, Code.ACTIVATION_STATE_CONFLICT);
        harness.denied(command("action", 3), CALLER, Code.ACTIVATION_STATE_CONFLICT);
        assertThatThrownBy(() -> harness.service.resume(command("action", 2), CALLER)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.ACTIVATION_STATE_CONFLICT));
        var row = harness.actions.get("action");
        assertThatThrownBy(() -> OperationsLifecycleSemantics.replay(row, harness.head, command("action", 2), new Caller(UUID.randomUUID(), CALLER.audience()), "ACTIVE", "SUSPENDED")).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.ACTIVATION_STATE_CONFLICT));
        assertThatThrownBy(() -> OperationsLifecycleSemantics.replay(row, harness.head, new Command(UUID.randomUUID(), "action", "reason", BigInteger.TWO), CALLER, "ACTIVE", "SUSPENDED")).isInstanceOf(Failure.class);
        assertThat(harness.appended).hasSize(1);
    }
    @Test
    void frozenFingerprintHasExactKeysNullsAndCanonicalUtf8() throws Exception {
        var command = new Command(SUBJECT, " action ", "原因😀", BigInteger.TWO); String status = ActivationSemantics.statusEvent(ACTIVATION, 3);
        var fields = OperationsLifecycleSemantics.fingerprintFields(command, CALLER, "SUSPENDED", status);
        assertThat(fields.keySet()).containsExactlyInAnyOrder("accountId", "actorEmployeeId", "channel", "consentRequestFingerprint", "reasonCode", "sourceActionDomain", "sourceActionEventId", "statusEventDomain", "statusEventId", "subjectId", "toStatus"); assertThat(fields.get("accountId")).isNull(); assertThat(fields.get("consentRequestFingerprint")).isNull();
        assertThat(OperationsLifecycleSemantics.fingerprint(command, CALLER, "SUSPENDED", status)).isEqualTo(ActivationSemantics.sha(ActivationSemantics.canonical(fields)));
        assertThat(OperationsLifecycleSemantics.fingerprint(new Command(SUBJECT, command.actionEventId(), command.reasonCode(), BigInteger.TEN), CALLER, "SUSPENDED", status)).isEqualTo(OperationsLifecycleSemantics.fingerprint(command, CALLER, "SUSPENDED", status));
    }
    @Test
    void maximumActionIdentityIsStoredWithoutTruncation() throws Exception {
        var harness = new Harness(); String key = "😀".repeat(128); harness.service.suspend(command(key, 2), CALLER); assertThat(harness.appended.getFirst().actionEvent()).isEqualTo(key);
        for (String reason : List.of("原因😀".repeat(21) + "x", "原因😊".repeat(21) + "x")) {
            harness = new Harness();
            var bounded = new Command(SUBJECT, "k".repeat(128), reason, BigInteger.TWO);
            assertThat(reason.codePointCount(0, reason.length())).isEqualTo(64);
            var result = harness.service.suspend(bounded, CALLER);
            assertThat(result.reasonCode()).isEqualTo(reason);
            assertThat(harness.appended.getFirst().actionEvent()).isEqualTo(bounded.actionEventId());
            assertThat(harness.appended.getFirst().reason()).isEqualTo(reason);
            assertThat(harness.service.suspend(bounded, CALLER)).isEqualTo(result);
            assertThat(harness.appended).hasSize(1); assertThat(harness.outbox).isEqualTo(1); assertThat(harness.audit).isEqualTo(1);
        }
    }
    @Test
    void statusIdentityUsesActivationAndNextRevisionWithoutOverflow() throws Exception {
        assertThat(ActivationSemantics.statusEvent(ACTIVATION, Integer.MAX_VALUE)).isEqualTo("activation-status:" + ACTIVATION + ":2147483647");
        var harness = new Harness(); harness.head = new Head(ACTIVATION, SUBJECT, "ACTIVE", Integer.MAX_VALUE, TIME, TIME); harness.denied(command("action", Integer.MAX_VALUE), CALLER, Code.ACTIVATION_STATE_CONFLICT); harness.noWrites();
    }
    @Test
    void lockOrderAndOwnerAssertionsPrecedeEveryLocalWrite() throws Exception {
        var harness = new Harness(); harness.service.suspend(command("action", 2), CALLER);
        assertThat(harness.order).containsSubsequence("enter", "subject", "head", "exists", "employee", "role", "advance", "role", "transition", "role", "outbox", "role", "audit", "role");
    }
    @Test
    void unusableHeadTransitionOrEmployeeContextFailsClosed() throws Exception {
        var harness = new Harness(); harness.head = new Head(ACTIVATION, SUBJECT, "UNKNOWN", 2, TIME, TIME); harness.denied(command("action", 2), CALLER, Code.SERVICE_UNAVAILABLE); harness.noWrites();
        harness = new Harness(); harness.witness = null; harness.denied(command("action", 2), CALLER, Code.SERVICE_UNAVAILABLE); harness.noWrites();
        for (var malformed : List.of(
                persistedWitness("WEB", "ACTIVATION", ActivationSemantics.statusEvent(ACTIVATION, 2), "invalid-fingerprint", TIME.minusSeconds(1)),
                persistedWitness("WEB", "OPERATIONS", ActivationSemantics.statusEvent(ACTIVATION, 2), "a".repeat(64), TIME.minusSeconds(1)),
                persistedWitness("OPERATIONS", "ACTIVATION", ActivationSemantics.statusEvent(ACTIVATION, 2), "a".repeat(64), TIME.minusSeconds(1)),
                persistedWitness("WEB", "ACTIVATION", "wrong-status-event", "a".repeat(64), TIME.minusSeconds(1)),
                persistedWitness("OPERATIONS", "OPERATIONS", ActivationSemantics.statusEvent(ACTIVATION, 2), "b".repeat(64), TIME.minusSeconds(1)),
                persistedWitness("WEB", "ACTIVATION", ActivationSemantics.statusEvent(ACTIVATION, 2), "a".repeat(64), TIME.minusSeconds(2)))) {
            harness = new Harness(); harness.witness = malformed; harness.denied(command("action", 2), CALLER, Code.SERVICE_UNAVAILABLE); harness.noWrites();
        }
        for (var kind : EmployeeAdministratorLock.Failure.Kind.values()) { harness = new Harness(); when(harness.employee.lockEmployeeAdministrator(EMPLOYEE)).thenThrow(new EmployeeAdministratorLock.Failure(kind)); harness.denied(command("action", 2), CALLER, Code.SERVICE_UNAVAILABLE); harness.noWrites(); }
        harness = new Harness(); when(harness.employee.lockEmployeeAdministrator(EMPLOYEE)).thenThrow(new IllegalStateException("SECRET")); harness.denied(command("action", 2), CALLER, Code.INTERNAL); harness.noWrites();
        harness = new Harness(); harness.denied(command("action", 2), new Caller(EMPLOYEE, "wrong"), Code.SERVICE_UNAVAILABLE); harness.noWrites();
    }
    @Test
    void conditionalUpdateMissRereadsOnceAndConflicts() throws Exception {
        var harness = new Harness(); harness.count = 0; harness.denied(command("action", 2), CALLER, Code.ACTIVATION_STATE_CONFLICT); verify(harness.repository, times(2)).lockHead(harness.context, SUBJECT); assertThat(harness.appended).isEmpty(); verify(harness.connection).rollback();
    }
    @Test
    void auditAndOutboxMappingsAreExactAndContainNoSecrets() throws Exception {
        var harness = new Harness(); harness.service.suspend(command("action", 2), CALLER); var row = harness.appended.getFirst();
        verify(harness.repository).appendAudit(harness.context, row); verify(harness.repository).appendOutbox(harness.context, row);
        assertThat(row.accountId()).isNull(); assertThat(row.consentId()).isNull(); assertThat(row.employeeId()).isEqualTo(EMPLOYEE); assertThat(row.occurredAt()).isEqualTo(TIME);
        String sql = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/onecall/core/activation_consent/internal/operations/JooqOperationsLifecycleRepository.java")); assertThat(sql).contains("SUBJECT_SUSPEND", "SUBJECT_RESUME", "row.statusEvent(), UUID.randomUUID().toString()", "\"reasonCode\", row.reason()");
    }
    @Test
    void auditOutboxAndTransitionFailuresRollBackWholeDecision() throws Exception {
        for (String stage : List.of("transition", "outbox", "audit")) { var harness = new Harness();
            if (stage.equals("transition")) doThrow(new Failure(Code.SERVICE_UNAVAILABLE)).when(harness.repository).appendTransition(any(), any());
            if (stage.equals("outbox")) doThrow(new Failure(Code.SERVICE_UNAVAILABLE)).when(harness.repository).appendOutbox(any(), any());
            if (stage.equals("audit")) doThrow(new Failure(Code.SERVICE_UNAVAILABLE)).when(harness.repository).appendAudit(any(), any());
            harness.denied(command("action", 2), CALLER, Code.SERVICE_UNAVAILABLE); verify(harness.connection).rollback(); verify(harness.connection, never()).commit(); assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        }
    }
    @Test
    void resumeHasNoTrialConsentOrForeignMutationCollaborator() throws Exception {
        assertThat(Arrays.stream(DefaultOperationsSubjectLifecycleService.class.getConstructors()[0].getParameterTypes()).map(Class::getName)).containsExactly(TransactionTemplate.class.getName(), DSLContext.class.getName(), OperationsLifecycleRepository.class.getName(), org.springframework.beans.factory.ObjectProvider.class.getName());
        String source = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/onecall/core/activation_consent/internal/operations/DefaultOperationsSubjectLifecycleService.java")); assertThat(source).doesNotContain("issue_first_trial", "ActivationTrialService", "appendConsent", "entitlement_metering");
    }
    @Test
    void uniqueAndTechnicalFailuresMapOnlyAfterTransactionCleanup() throws Exception {
        for (String state : List.of("23505", "40001", "40P01", "42501", "08006")) { var harness = new Harness();
            doThrow(new org.jooq.exception.DataAccessException("SQL SECRET", new SQLException("uq_activation_transition_action", state))).when(harness.repository).appendTransition(any(), any());
            harness.denied(command("action", 2), CALLER, state.equals("23505") ? Code.ACTIVATION_STATE_CONFLICT : Code.SERVICE_UNAVAILABLE); verify(harness.connection).rollback(); assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        }
        for (var kind : EmployeeAdministratorLock.Failure.Kind.values()) { var harness = new Harness(); when(harness.employee.lockEmployeeAdministrator(EMPLOYEE)).thenThrow(new EmployeeAdministratorLock.Failure(kind)); harness.denied(command("action", 2), CALLER, Code.SERVICE_UNAVAILABLE); verify(harness.connection).rollback(); }
        for (String state : List.of("22012", "25P02")) {
            var harness = new Harness();
            when(harness.employee.lockEmployeeAdministrator(EMPLOYEE)).thenThrow(new IllegalStateException("Fixture statement capture failed", new SQLException("SQL SECRET", state)));
            harness.denied(command("action", 2), CALLER, Code.SERVICE_UNAVAILABLE);
            harness.noWrites(); verify(harness.connection).rollback(); verify(harness.connection, never()).commit();
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        }
    }
    @Test
    void replayAfterLaterTransitionReturnsFirstImmutableResult() throws Exception {
        var harness = new Harness(); var first = harness.service.suspend(command("action", 2), CALLER); harness.service.resume(command("resume", 3), CALLER); assertThat(harness.service.suspend(command("action", 2), CALLER)).isEqualTo(first); assertThat(harness.appended).hasSize(2); assertThat(harness.head.revision()).isEqualTo(4); assertThat(harness.outbox).isEqualTo(2); assertThat(harness.audit).isEqualTo(2);
    }
}
