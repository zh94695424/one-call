package com.onecall.core.catalog_order.internal.operationsdraft;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;

import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.transaction.support.TransactionTemplate;

final class DefaultOperationsCatalogDraftCommand implements OperationsCatalogDraftCommand {
    private final TransactionTemplate transaction;
    private final EmployeeAuthorityReadPort authority;
    private final CatalogDraftRepository repository;

    DefaultOperationsCatalogDraftCommand(TransactionTemplate transaction, EmployeeAuthorityReadPort authority, CatalogDraftRepository repository) {
        this.transaction = transaction; this.authority = authority; this.repository = repository;
    }
    @Override public CreateResult create(CreateCommand command) {
        CatalogDraftSemantics.validateCommercial(command.commercial());
        CatalogDraftSemantics.projection(command.productId(), command.versionNo(), command.commercial(), command.benefitLines(), command.channels());
        return execute(command.caller(), () -> repository.create(command), () -> repository.recoverCreate(command));
    }
    @Override public CatalogVersionDraft replace(ReplaceCommand command) {
        CatalogDraftSemantics.validateCommercial(command.commercial());
        if (command.expectedRevision() < 1) throw new Failure(Code.VALIDATION_FAILED);
        return execute(command.caller(), () -> repository.replace(command), () -> repository.recoverReplace(command));
    }
    private <Result> Result execute(Caller caller, Supplier<Result> action, Supplier<Result> recovery) {
        try { return attempt(caller, action); }
        catch (RuntimeException failure) {
            if (failure instanceof Failure known) throw known;
            if (authorityFailure(failure)) throw new Failure(Code.SERVICE_UNAVAILABLE);
            String state = sqlState(failure);
            if ("55P02".equals(state) || "55P03".equals(state)) throw new Failure(Code.CATALOG_DRAFT_CONFLICT);
            if ("23505".equals(state) || "40001".equals(state)) return recover(caller, recovery);
            throw new Failure(sqlRelated(failure) ? Code.SERVICE_UNAVAILABLE : Code.INTERNAL);
        }
    }
    private <Result> Result recover(Caller caller, Supplier<Result> recovery) {
        try { return attempt(caller, recovery); }
        catch (RuntimeException failure) {
            if (failure instanceof Failure known) throw known;
            if (authorityFailure(failure)) throw new Failure(Code.SERVICE_UNAVAILABLE);
            String state = sqlState(failure);
            if ("55P02".equals(state) || "55P03".equals(state)) throw new Failure(Code.CATALOG_DRAFT_CONFLICT);
            throw new Failure(sqlRelated(failure) ? Code.SERVICE_UNAVAILABLE : Code.INTERNAL);
        }
    }
    private <Result> Result attempt(Caller caller, Supplier<Result> action) {
        return transaction.execute(status -> {
            var witness = repository.begin();
            try {
                requireAdmin(caller, authority.read(caller.employeeId(), witness));
                repository.catalogRole(witness);
                Result result = action.get();
                try { repository.restore(witness); }
                catch (RuntimeException cleanup) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
                return result;
            } catch (RuntimeException failure) {
                status.setRollbackOnly();
                if (!sqlRelated(failure)) {
                    try { repository.restore(witness); }
                    catch (RuntimeException cleanup) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
                }
                throw failure;
            }
        });
    }
    private static void requireAdmin(Caller caller, EmployeeAuthorityReadPort.EmployeeAuthoritySnapshot snapshot) {
        if (snapshot == null || !caller.employeeId().equals(snapshot.employeeId()) || snapshot.revision() < 1
                || snapshot.status() == null || !Set.of("ACTIVE", "DISABLED").contains(snapshot.status())
                || snapshot.role() == null || !Set.of("ADMIN", "OPS").contains(snapshot.role())) throw new Failure(Code.SERVICE_UNAVAILABLE);
        if (!"ACTIVE".equals(snapshot.status()) || !"ADMIN".equals(snapshot.role())) throw new Failure(Code.FORBIDDEN);
    }
    private static boolean authorityFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof EmployeeAuthorityReadPort.Failure) return true;
        }
        return false;
    }
    private static String sqlState(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String state = cause instanceof EmployeeAuthorityReadPort.Failure authority ? authority.sqlState()
                    : cause instanceof java.sql.SQLException sql ? sql.getSQLState()
                    : cause instanceof org.jooq.exception.DataAccessException data ? data.sqlState() : null;
            if (state != null && !state.isBlank() && !"00000".equals(state)) return state;
        }
        return null;
    }
    private static boolean sqlRelated(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.sql.SQLException || cause instanceof org.jooq.exception.DataAccessException) return true;
        }
        return false;
    }
}
