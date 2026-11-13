package com.onecall.core.activation_consent.internal.operations;

import com.onecall.core.activation_consent.internal.command.ActivationSemantics;
import com.onecall.core.activation_consent.internal.operations.OperationsLifecycleRepository.*;
import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class DefaultOperationsSubjectLifecycleService implements OperationsSubjectLifecycleService {
    private final TransactionTemplate transaction;
    private final DSLContext context;
    private final OperationsLifecycleRepository repository;
    private final ObjectProvider<EmployeeAdministratorLock> employees;
    public DefaultOperationsSubjectLifecycleService(TransactionTemplate transaction, DSLContext context, OperationsLifecycleRepository repository,
            ObjectProvider<EmployeeAdministratorLock> employees) {
        this.transaction = transaction; this.context = context; this.repository = repository; this.employees = employees;
    }
    @Override public ActivationTransition suspend(Command command, Caller caller) { return execute(command, caller, "ACTIVE", "SUSPENDED"); }
    @Override public ActivationTransition resume(Command command, Caller caller) { return execute(command, caller, "SUSPENDED", "ACTIVE"); }
    private ActivationTransition execute(Command command, Caller caller, String from, String target) {
        if (caller == null || caller.employeeId() == null || !"onecall:employee-session:v1".equals(caller.audience())) throw new Failure(Code.SERVICE_UNAVAILABLE);
        if (command == null || command.subjectId() == null || !OperationsLifecycleSemantics.text(command.actionEventId(), 128)
                || !OperationsLifecycleSemantics.text(command.reasonCode(), 64) || command.expectedActivationRevision() == null
                || command.expectedActivationRevision().signum() < 1) throw new Failure(Code.VALIDATION_FAILED);
        EmployeeAdministratorLock employee;
        try {
            var candidates = employees.stream().limit(2).toList();
            if (candidates.size() != 1) throw new EmployeeAdministratorLock.Failure(EmployeeAdministratorLock.Failure.Kind.UNAVAILABLE);
            employee = candidates.getFirst();
        } catch (RuntimeException unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
        try {
            return transaction.execute(status -> {
                try {
                    if (!TransactionSynchronizationManager.isActualTransactionActive()
                            || !(transaction.getTransactionManager() instanceof DataSourceTransactionManager manager) || manager.getDataSource() == null
                            || !TransactionSynchronizationManager.hasResource(manager.getDataSource())) throw new Failure(Code.SERVICE_UNAVAILABLE);
                    repository.enter(context);
                    repository.lockSubject(context, command.subjectId());
                    var current = repository.lockHead(context, command.subjectId());
                    boolean exists = repository.subjectExists(context, command.subjectId());
                    var facts = employee.lockEmployeeAdministrator(caller.employeeId());
                    repository.assertRole(context);
                    if (facts == null) throw new EmployeeAdministratorLock.Failure(EmployeeAdministratorLock.Failure.Kind.INTEGRITY);
                    if (facts.isEmpty()) throw new Failure(Code.UNAUTHENTICATED);
                    var authority = facts.get();
                    if (!caller.employeeId().equals(authority.employeeId()) || authority.status() == null || !Set.of("ACTIVE", "DISABLED").contains(authority.status())
                            || authority.role() == null || !Set.of("ADMIN", "OPS").contains(authority.role()) || authority.revision() < 1)
                        throw new EmployeeAdministratorLock.Failure(EmployeeAdministratorLock.Failure.Kind.INTEGRITY);
                    if (!"ACTIVE".equals(authority.status())) throw new Failure(Code.UNAUTHENTICATED);
                    if (!"OPS".equals(authority.role())) throw new Failure(Code.FORBIDDEN);
                    if (!exists || current.isEmpty()) throw new Failure(Code.NOT_FOUND);
                    Head head = current.get();
                    OperationsLifecycleSemantics.validateHead(head, command.subjectId());
                    var previous = repository.actionTransition(context, command.actionEventId());
                    if (previous.isPresent()) return OperationsLifecycleSemantics.replay(previous.get(), head, command, caller, from, target);
                    if (!from.equals(head.status()) || !BigInteger.valueOf(head.revision()).equals(command.expectedActivationRevision()) || head.revision() == Integer.MAX_VALUE)
                        throw new Failure(Code.ACTIVATION_STATE_CONFLICT);
                    OperationsLifecycleSemantics.witness(repository.latestTransition(context, head).orElseThrow(OperationsLifecycleSemantics::unavailable), head);
                    var time = repository.decisionTime(context);
                    if (time == null || time.isBefore(head.updatedAt())) throw new Failure(Code.SERVICE_UNAVAILABLE);
                    String statusEvent = ActivationSemantics.statusEvent(head.id(), head.revision() + 1);
                    var transition = new Transition(UUID.randomUUID(), head.id(), head.subjectId(), from, target, head.revision(), head.revision() + 1,
                            "OPERATIONS", "OPERATIONS", command.actionEventId(), "ACTIVATION_STATUS", statusEvent,
                            OperationsLifecycleSemantics.fingerprint(command, caller, target, statusEvent), null, caller.employeeId(), command.reasonCode(), null, time);
                    repository.assertRole(context);
                    int updated = repository.advance(context, head, target, time);
                    if (updated == 0) { repository.lockHead(context, command.subjectId()); throw new Failure(Code.ACTIVATION_STATE_CONFLICT); }
                    if (updated != 1) throw new Failure(Code.SERVICE_UNAVAILABLE);
                    repository.assertRole(context); repository.appendTransition(context, transition);
                    repository.assertRole(context); repository.appendOutbox(context, transition);
                    repository.assertRole(context); repository.appendAudit(context, transition);
                    repository.assertRole(context);
                    return OperationsLifecycleSemantics.result(transition);
                } catch (RuntimeException failure) { status.setRollbackOnly(); throw failure; }
            });
        } catch (Failure known) { throw known; }
        catch (EmployeeAdministratorLock.Failure unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
        catch (RuntimeException failure) { throw map(failure); }
    }
    private Failure map(RuntimeException failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql) {
                if ("23505".equals(sql.getSQLState()) && sql.getMessage() != null && Set.of("uq_activation_transition_action", "uq_activation_transition_revision", "uq_activation_transition_status_event")
                        .stream().anyMatch(sql.getMessage()::contains)) return new Failure(Code.ACTIVATION_STATE_CONFLICT);
                return new Failure(Code.SERVICE_UNAVAILABLE);
            }
        }
        if (failure instanceof org.springframework.transaction.TransactionException || failure instanceof org.springframework.dao.DataAccessException
                || failure instanceof org.jooq.exception.DataAccessException) return new Failure(Code.SERVICE_UNAVAILABLE);
        return new Failure(Code.INTERNAL);
    }
}
