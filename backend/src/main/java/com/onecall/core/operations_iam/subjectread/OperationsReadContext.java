package com.onecall.core.operations_iam.subjectread;

import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class OperationsReadContext {
    public enum Role {
        SUBJECT("subject_number_role"), ACTIVATION("activation_consent_role"), TRIAL("entitlement_metering_role"),
        EVIDENCE("evidence_role"), ORDER("catalog_order_role");

        private final String databaseRole;

        Role(String databaseRole) { this.databaseRole = databaseRole; }
    }
    private record State(Connection connection, String login, int backend) { }
    private final DataSource dataSource;
    private final DSLContext dsl;
    private final ThreadLocal<State> state = new ThreadLocal<>();

    public OperationsReadContext(DataSource dataSource, DSLContext dsl) { this.dataSource = dataSource; this.dsl = dsl; }

    public void enter() {
        if (state.get() != null) throw unavailable();
        Connection connection = connection();
        try {
            if (!connection.isReadOnly() || connection.getTransactionIsolation() != Connection.TRANSACTION_REPEATABLE_READ
                    || connection.getAutoCommit()) throw unavailable();
            var identity = dsl.fetchSingle("SELECT session_user AS login, current_user AS role, pg_backend_pid() AS backend, current_setting('transaction_read_only') AS readonly");
            String login = identity.get("login", String.class);
            if (login == null || !login.equals(identity.get("role", String.class)) || !"on".equals(identity.get("readonly", String.class))) throw unavailable();
            state.set(new State(connection, login, identity.get("backend", Integer.class)));
            dsl.execute("SET LOCAL statement_timeout = '2000ms'");
            dsl.execute("SET LOCAL lock_timeout = '250ms'");
            dsl.execute("SET LOCAL idle_in_transaction_session_timeout = '5000ms'");
            dsl.execute("SET LOCAL ROLE operations_iam_role");
            assertRole("operations_iam_role");
        } catch (SQLException failure) { throw unavailable(); }
    }

    public <Result> Result scoped(Role role, Supplier<Result> operation) {
        try {
            assertRole("operations_iam_role");
            dsl.execute("SET LOCAL ROLE " + role.databaseRole);
            assertRole(role.databaseRole);
            return operation.get();
        } finally {
            try {
                dsl.execute("SET LOCAL ROLE operations_iam_role");
                assertRole("operations_iam_role");
            } catch (RuntimeException cleanup) { rollbackOnly(); throw unavailable(); }
        }
    }

    public void exit() {
        State expected = state.get();
        try {
            if (expected == null) throw unavailable();
            dsl.execute("SET LOCAL ROLE NONE");
            assertRole(expected.login());
        } catch (RuntimeException cleanup) { rollbackOnly(); throw unavailable(); }
        finally { state.remove(); }
    }

    private Connection connection() {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || !TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !(TransactionSynchronizationManager.getResource(dataSource) instanceof ConnectionHolder holder)) throw unavailable();
        return holder.getConnection();
    }

    private void assertRole(String role) {
        State expected = state.get();
        Connection connection = connection();
        try {
            if (expected == null || connection != expected.connection() || !connection.isReadOnly()
                    || connection.getAutoCommit() || connection.getTransactionIsolation() != Connection.TRANSACTION_REPEATABLE_READ) throw unavailable();
            var identity = dsl.fetchSingle("SELECT session_user AS login, current_user AS role, pg_backend_pid() AS backend, current_setting('transaction_read_only') AS readonly");
            if (!expected.login().equals(identity.get("login", String.class)) || !role.equals(identity.get("role", String.class))
                    || expected.backend() != identity.get("backend", Integer.class) || !"on".equals(identity.get("readonly", String.class))) throw unavailable();
        } catch (SQLException failure) { throw unavailable(); }
    }

    private void rollbackOnly() {
        if (TransactionSynchronizationManager.getResource(dataSource) instanceof ConnectionHolder holder) holder.setRollbackOnly();
    }
    private static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
}
