package com.onecall.core.catalog_order.internal.operationscommand;

import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import java.util.function.Supplier;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.TransactionWitness;

final class DefaultOperationsCatalogSaleCommand implements OperationsCatalogSaleCommand {
    private final TransactionTemplate transaction;
    private final EmployeeAuthorityReadPort authority;
    private final CatalogSaleLifecycleRepository repository;
    DefaultOperationsCatalogSaleCommand(TransactionTemplate transaction, EmployeeAuthorityReadPort authority, CatalogSaleLifecycleRepository repository) { this.transaction = transaction; this.authority = authority; this.repository = repository; }
    @Override public CatalogVersionState publish(PublishCommand command) {
        return execute(command.caller(), command.versionId(), command.expectedActiveVersionId(), command.expectedRevision(), true,
                () -> repository.publish(command.versionId(), command.expectedActiveVersionId(), command.expectedRevision(), command.caller().employeeId(), command.caller().traceId()));
    }
    @Override public CatalogVersionState offsale(OffsaleCommand command) {
        return execute(command.caller(), command.versionId(), null, command.expectedRevision(), false,
                () -> repository.offsale(command.versionId(), command.expectedRevision(), command.caller().employeeId(), command.caller().traceId()));
    }
    private CatalogVersionState execute(Caller caller, UUID versionId, UUID active, int revision, boolean publication, Supplier<CatalogVersionState> action) {
        try { return attempt(caller, action); }
        catch (RuntimeException failure) {
            String state = sqlState(failure);
            if ("23505".equals(state) || "40001".equals(state)) {
                try { return attempt(null, () -> repository.recover(versionId, active, revision, publication), true); }
                catch (Failure recovery) { if (recovery.code() == Code.CATALOG_PUBLICATION_CONFLICT) throw recovery; throw new Failure(Code.SERVICE_UNAVAILABLE); }
                catch (RuntimeException recovery) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
            }
            if (failure instanceof Failure known) throw known;
            if ("55P02".equals(state) || "55P03".equals(state)) throw new Failure(Code.CATALOG_PUBLICATION_CONFLICT);
            throw new Failure(Code.SERVICE_UNAVAILABLE);
        }
    }
    private CatalogVersionState attempt(Caller caller, Supplier<CatalogVersionState> action) {
        return attempt(caller, action, false);
    }
    private CatalogVersionState attempt(Caller caller, Supplier<CatalogVersionState> action, boolean recovery) {
        return transaction.execute(status -> {
            TransactionWitness witness;
            try { witness = repository.begin(); }
            catch (RuntimeException failure) { status.setRollbackOnly(); throw new Failure(Code.SERVICE_UNAVAILABLE); }
            try {
                if (recovery) repository.recoveryRole(witness);
                else {
                    var snapshot = authority.read(caller.employeeId(), witness);
                    if (snapshot == null || !caller.employeeId().equals(snapshot.employeeId()) || snapshot.revision() < 1) throw new Failure(Code.SERVICE_UNAVAILABLE);
                    if (!"ACTIVE".equals(snapshot.status()) || !"ADMIN".equals(snapshot.role())) throw new Failure(Code.FORBIDDEN);
                    repository.catalogRole(witness);
                }
                var result = action.get();
                repository.restore(witness);
                return result;
            } catch (RuntimeException failure) {
                status.setRollbackOnly();
                if (sqlState(failure) == null) {
                    try { repository.restore(witness); }
                    catch (RuntimeException cleanup) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
                }
                if (failure instanceof EmployeeAuthorityReadPort.Failure) throw new Failure(Code.SERVICE_UNAVAILABLE);
                throw failure;
            }
        });
    }
    private static String sqlState(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException sql) return sql.getSQLState();
            if (cause instanceof org.jooq.exception.DataAccessException data && data.sqlState() != null && !"00000".equals(data.sqlState())) return data.sqlState();
        }
        return null;
    }
}
