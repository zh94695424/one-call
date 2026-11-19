package com.onecall.core.operations_iam.internal.employeeauthority;

import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort;
import java.util.UUID;
import java.sql.Connection;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("persistence")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class DefaultEmployeeAuthorityReadPort implements EmployeeAuthorityReadPort {
    private final DSLContext dsl;
    private final DataSource dataSource;
    public DefaultEmployeeAuthorityReadPort(@Qualifier("operationsSubjectReadDataSource") DataSource dataSource,
            @Qualifier("operationsSubjectReadDsl") DSLContext dsl) { this.dataSource = dataSource; this.dsl = dsl; }
    @Override public EmployeeAuthoritySnapshot read(UUID employeeId, TransactionWitness witness) {
        try {
            Connection connection = connection();
            assertIdentity(connection, witness, witness.login());
            dsl.execute("SET LOCAL ROLE operations_iam_role");
            assertIdentity(connection, witness, "operations_iam_role");
            var rows = dsl.fetch("SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = ?", employeeId);
            if (rows.size() != 1) throw new Failure(Code.SERVICE_UNAVAILABLE);
            var row = rows.getFirst();
            if (!employeeId.equals(row.get("id", UUID.class)) || row.get("revision", Integer.class) == null
                    || row.get("revision", Integer.class) < 1) throw new Failure(Code.SERVICE_UNAVAILABLE);
            assertIdentity(connection, witness, "operations_iam_role");
            return new EmployeeAuthoritySnapshot(employeeId, row.get("status", String.class), row.get("role", String.class), row.get("revision", Integer.class));
        } catch (Exception failure) {
            if (TransactionSynchronizationManager.getResource(dataSource) instanceof ConnectionHolder holder) holder.setRollbackOnly();
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof java.sql.SQLException sql) throw new Failure(Code.SERVICE_UNAVAILABLE, sql.getSQLState());
            }
            throw new Failure(Code.SERVICE_UNAVAILABLE);
        }
    }
    private Connection connection() {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !(TransactionSynchronizationManager.getResource(dataSource) instanceof ConnectionHolder holder)) throw new Failure(Code.SERVICE_UNAVAILABLE);
        return holder.getConnection();
    }
    private void assertIdentity(Connection expected, TransactionWitness witness, String role) throws Exception {
        Connection current = connection();
        if (current != expected || current.isReadOnly() || current.getAutoCommit()
                || current.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED || witness == null) throw new Failure(Code.SERVICE_UNAVAILABLE);
        var identity = dsl.fetchSingle("SELECT session_user AS login, current_user AS role, pg_backend_pid() AS backend, txid_current() AS transaction, current_setting('transaction_read_only') AS readonly");
        if (!witness.login().equals(identity.get("login", String.class)) || !role.equals(identity.get("role", String.class))
                || witness.backendPid() != identity.get("backend", Integer.class) || witness.transactionId() != identity.get("transaction", Long.class)
                || !"off".equals(identity.get("readonly", String.class))) throw new Failure(Code.SERVICE_UNAVAILABLE);
    }
}
