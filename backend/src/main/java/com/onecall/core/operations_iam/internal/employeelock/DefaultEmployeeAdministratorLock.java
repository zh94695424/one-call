package com.onecall.core.operations_iam.internal.employeelock;

import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock;
import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock.Failure.Kind;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component("employeeAdministratorLock")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-lifecycle", name = "enabled", havingValue = "true")
public final class DefaultEmployeeAdministratorLock implements EmployeeAdministratorLock {
    private final DataSourceTransactionManager manager;
    private final DSLContext context;
    private final TransactionTemplate mandatory;
    public DefaultEmployeeAdministratorLock(@Qualifier("operationsLifecycleTransactionManager") DataSourceTransactionManager manager,
            @Qualifier("operationsLifecycleDsl") DSLContext context) {
        this.manager = manager; this.context = context;
        mandatory = new TransactionTemplate(manager);
        mandatory.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
    }
    @Override public Optional<EmployeeFacts> lockEmployeeAdministrator(UUID employeeId) {
        if (employeeId == null) throw new Failure(Kind.INTEGRITY);
        if (!TransactionSynchronizationManager.isActualTransactionActive() || manager.getDataSource() == null
                || !TransactionSynchronizationManager.hasResource(manager.getDataSource())) throw new Failure(Kind.UNAVAILABLE);
        try {
            return mandatory.execute(status -> {
                assertRole("activation_consent_role");
                Failure primary = null;
                try {
                    context.execute("SET LOCAL ROLE operations_iam_role");
                    assertRole("operations_iam_role");
                    context.fetch("SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('operations_iam.admin_mutation',0))");
                    var rows = context.fetch("SELECT id,status,role,revision FROM operations_iam.employee_account WHERE id=? FOR UPDATE", employeeId);
                    if (rows.isEmpty()) return Optional.empty();
                    if (rows.size() != 1) throw new Failure(Kind.INTEGRITY);
                    var row = rows.getFirst();
                    UUID identity = row.get("id", UUID.class);
                    String state = row.get("status", String.class), role = row.get("role", String.class);
                    Integer revision = row.get("revision", Integer.class);
                    if (!employeeId.equals(identity) || state == null || !Set.of("ACTIVE", "DISABLED").contains(state)
                            || role == null || !Set.of("ADMIN", "OPS").contains(role) || revision == null || revision < 1) throw new Failure(Kind.INTEGRITY);
                    return Optional.of(new EmployeeFacts(identity, state, role, revision));
                } catch (RuntimeException failure) {
                    status.setRollbackOnly();
                    primary = failure instanceof Failure typed ? typed : new Failure(Kind.UNAVAILABLE);
                    throw primary;
                } finally {
                    try { context.execute("SET LOCAL ROLE activation_consent_role"); assertRole("activation_consent_role"); }
                    catch (RuntimeException restoration) {
                        status.setRollbackOnly();
                        if (primary == null) throw new Failure(Kind.UNAVAILABLE);
                        primary.addSuppressed(new Failure(Kind.UNAVAILABLE));
                    }
                }
            });
        } catch (Failure typed) { throw typed; }
        catch (RuntimeException unavailable) { throw new Failure(Kind.UNAVAILABLE); }
    }
    private void assertRole(String expected) {
        var row = context.fetchOne("SELECT current_user");
        if (row == null || !expected.equals(row.get(0, String.class))) throw new Failure(Kind.UNAVAILABLE);
    }
}
