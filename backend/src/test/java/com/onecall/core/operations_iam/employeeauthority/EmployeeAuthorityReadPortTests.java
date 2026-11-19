package com.onecall.core.operations_iam.employeeauthority;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.*;
import com.onecall.core.operations_iam.internal.employeeauthority.DefaultEmployeeAuthorityReadPort;
import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class EmployeeAuthorityReadPortTests {
    private final DataSource source = mock(DataSource.class);
    private final Connection connection = mock(Connection.class);
    private final ConnectionHolder holder = new ConnectionHolder(connection);
    private final DSLContext dsl = mock(DSLContext.class);
    private final Record identity = mock(Record.class), employee = mock(Record.class);
    private final Result<Record> rows = mock(Result.class);
    private final UUID employeeId = new UUID(0, 38);
    private final TransactionWitness witness = new TransactionWitness("login", 38, 39);
    private final DefaultEmployeeAuthorityReadPort port = new DefaultEmployeeAuthorityReadPort(source, dsl);
    private final java.util.concurrent.atomic.AtomicReference<String> role = new java.util.concurrent.atomic.AtomicReference<>("login");

    @BeforeEach void setup() throws Exception {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        TransactionSynchronizationManager.bindResource(source, holder);
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        when(identity.get("login", String.class)).thenReturn("login"); when(identity.get("role", String.class)).thenAnswer(invocation -> role.get());
        when(identity.get("backend", Integer.class)).thenReturn(38); when(identity.get("transaction", Long.class)).thenReturn(39L); when(identity.get("readonly", String.class)).thenReturn("off");
        when(dsl.fetchSingle(anyString())).thenReturn(identity);
        when(dsl.execute("SET LOCAL ROLE operations_iam_role")).thenAnswer(invocation -> { role.set("operations_iam_role"); return 0; });
        when(dsl.fetch(anyString(), any(Object[].class))).thenReturn(rows); when(rows.size()).thenReturn(1); when(rows.getFirst()).thenReturn(employee);
        when(employee.get("id", UUID.class)).thenReturn(employeeId); when(employee.get("status", String.class)).thenReturn("ACTIVE"); when(employee.get("role", String.class)).thenReturn("ADMIN"); when(employee.get("revision", Integer.class)).thenReturn(7);
    }
    @AfterEach void cleanup() { TransactionSynchronizationManager.unbindResourceIfPossible(source); TransactionSynchronizationManager.clear(); }
    private void unavailable() { assertThatThrownBy(() -> port.read(employeeId, witness)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE)); }
    @Test void readsOnlyStatusRoleRevision() { assertThat(port.read(employeeId, witness).revision()).isEqualTo(7); verify(dsl).fetch("SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = ?", employeeId); }
    @Test void isTransactionMandatory() throws Exception {
        TransactionSynchronizationManager.setActualTransactionActive(false); unavailable(); verifyNoInteractions(dsl);
        TransactionSynchronizationManager.setActualTransactionActive(true); TransactionSynchronizationManager.setCurrentTransactionReadOnly(true); unavailable();
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false); when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_REPEATABLE_READ); unavailable();
        verifyNoInteractions(dsl);
    }
    @Test void usesOperationsIamRole() { port.read(employeeId, witness); var order = inOrder(dsl); order.verify(dsl).fetchSingle(anyString()); order.verify(dsl).execute("SET LOCAL ROLE operations_iam_role"); order.verify(dsl).fetchSingle(anyString()); order.verify(dsl).fetch(anyString(), eq(employeeId)); }
    @Test void doesNotUseForUpdateOrAdvisoryLock() {
        port.read(employeeId, witness);
        assertThat(mockingDetails(dsl).getInvocations()).allSatisfy(invocation -> { if (invocation.getArguments().length > 0 && invocation.getArgument(0) instanceof String sql) assertThat(sql.toUpperCase()).doesNotContain("FOR UPDATE", "ADVISORY", "INSERT", "UPDATE ", "DELETE"); });
    }
    @Test void preservesCommandConnectionOnSuccess() { port.read(employeeId, witness); assertThat(holder.getConnection()).isSameAs(connection); assertThat(role.get()).isEqualTo("operations_iam_role"); verify(dsl, never()).execute("SET LOCAL ROLE NONE"); assertThat(holder.isRollbackOnly()).isFalse(); }
    @Test void marksOuterTransactionRollbackOnlyOnFailure() {
        for (RuntimeException cause : java.util.List.of(new IllegalStateException("PRIVATE"), new org.jooq.exception.DataAccessException("PRIVATE SQL catalog_order_role password", new java.sql.SQLException("PRIVATE credential", "42501")))) {
            role.set("login"); when(dsl.fetch(anyString(), any(Object[].class))).thenThrow(cause);
            assertThatThrownBy(() -> port.read(employeeId, witness)).isInstanceOfSatisfying(Failure.class, failure -> {
                assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE); assertThat(failure.sqlState()).isEqualTo(cause instanceof org.jooq.exception.DataAccessException ? "42501" : null);
                assertThat(failure.toString()).doesNotContain("PRIVATE", "SQL", "catalog_order_role", "password", "credential", "42501"); assertThat(failure.getCause()).isNull();
            });
            assertThat(holder.isRollbackOnly()).isTrue(); verify(dsl, never()).execute("SET LOCAL ROLE NONE");
        }
    }
    @Test void mapsMissingEmployeeToUnavailable() { for (int size : new int[] {0, 2}) { when(rows.size()).thenReturn(size); role.set("login"); unavailable(); } }
    @Test void returnsImmutableSnapshot() { var snapshot = port.read(employeeId, witness); assertThat(snapshot).isEqualTo(new EmployeeAuthoritySnapshot(employeeId, "ACTIVE", "ADMIN", 7)); assertThat(EmployeeAuthoritySnapshot.class.isRecord()).isTrue(); assertThat(java.util.Arrays.stream(EmployeeAuthoritySnapshot.class.getDeclaredFields())).allSatisfy(field -> assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers())).isTrue()); }
}
