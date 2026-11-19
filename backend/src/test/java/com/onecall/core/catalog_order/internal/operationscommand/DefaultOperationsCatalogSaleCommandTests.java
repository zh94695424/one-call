package com.onecall.core.catalog_order.internal.operationscommand;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.TransactionWitness;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.EmployeeAuthoritySnapshot;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.SimpleTransactionStatus;

public final class DefaultOperationsCatalogSaleCommandTests {
    private final UUID version = new UUID(0, 38), employee = new UUID(1, 38), trace = new UUID(2, 38);
    private final Caller caller = new Caller(employee, trace);
    private final TransactionWitness witness = new TransactionWitness("login", 38, 39);
    private final CatalogSaleLifecycleRepository repository = mock(CatalogSaleLifecycleRepository.class);
    private final EmployeeAuthorityReadPort authority = mock(EmployeeAuthorityReadPort.class);
    private final TransactionTemplate template = mock(TransactionTemplate.class);
    private final java.util.ArrayList<SimpleTransactionStatus> statuses = new java.util.ArrayList<>();
    private final DefaultOperationsCatalogSaleCommand command = new DefaultOperationsCatalogSaleCommand(template, authority, repository);
    private final CatalogVersionState state = new CatalogVersionState(version, new UUID(3, 38), 1, "ACTIVE", 2, Instant.parse("2026-11-18T00:00:00Z"), version);

    @BeforeEach void setup() {
        when(template.execute(any())).thenAnswer(invocation -> {
            var status = new SimpleTransactionStatus(); statuses.add(status);
            return invocation.<org.springframework.transaction.support.TransactionCallback<?>>getArgument(0).doInTransaction(status);
        });
        when(repository.begin()).thenReturn(witness);
        when(authority.read(employee, witness)).thenReturn(new EmployeeAuthoritySnapshot(employee, "ACTIVE", "ADMIN", 1));
        when(repository.publish(version, null, 1, employee, trace)).thenReturn(state);
        when(repository.offsale(version, 1, employee, trace)).thenReturn(state);
    }
    private CatalogVersionState publish() { return command.publish(new PublishCommand(version, null, 1, caller)); }
    private void failure(Code code) { assertThatThrownBy(this::publish).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(code)); }
    @Test void requiresActiveAdminWitness() {
        assertThat(publish()).isSameAs(state);
        var order = inOrder(repository, authority);
        order.verify(repository).begin(); order.verify(authority).read(employee, witness); order.verify(repository).catalogRole(witness);
        order.verify(repository).publish(version, null, 1, employee, trace); order.verify(repository).restore(witness);
        assertThat(statuses.getFirst().isRollbackOnly()).isFalse();
    }
    @Test void opsMapsToForbidden() {
        when(authority.read(employee, witness)).thenReturn(new EmployeeAuthoritySnapshot(employee, "ACTIVE", "OPS", 1)); failure(Code.FORBIDDEN);
        verify(repository, never()).catalogRole(any()); verify(repository).restore(witness); assertThat(statuses.getFirst().isRollbackOnly()).isTrue();
    }
    @Test void disabledMapsToForbidden() {
        when(authority.read(employee, witness)).thenReturn(new EmployeeAuthoritySnapshot(employee, "DISABLED", "ADMIN", 1)); failure(Code.FORBIDDEN);
        verify(repository, never()).publish(any(), any(), anyInt(), any(), any());
    }
    @Test void missingAuthorityMapsToServiceUnavailable() {
        var authorityFailure = new EmployeeAuthorityReadPort.Failure(EmployeeAuthorityReadPort.Code.SERVICE_UNAVAILABLE, "42501");
        when(authority.read(employee, witness)).thenThrow(authorityFailure);
        assertThat(authorityFailure.sqlState()).isEqualTo("42501");
        assertThatThrownBy(this::publish).isInstanceOfSatisfying(Failure.class, failure -> { assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE); assertThat(failure.toString()).doesNotContain("42501", "SQL", "role", "password"); assertThat(failure.getCause()).isNull(); });
        verify(repository, never()).catalogRole(any()); assertThat(statuses.getFirst().isRollbackOnly()).isTrue();
    }
    @Test void mapsStaleExpectation() {
        when(repository.publish(any(), any(), anyInt(), any(), any())).thenThrow(new Failure(Code.PUBLICATION_STALE_VIEW)); failure(Code.PUBLICATION_STALE_VIEW);
        verify(repository, never()).recover(any(), any(), anyInt(), anyBoolean()); assertThat(statuses.getFirst().isRollbackOnly()).isTrue();
    }
    @Test void mapsUnmatchableStateToConflict() {
        for (String sqlState : List.of("55P02", "55P03")) {
            doThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", sqlState))).when(repository).publish(any(), any(), anyInt(), any(), any());
            failure(Code.CATALOG_PUBLICATION_CONFLICT);
        }
        verify(repository, never()).recover(any(), any(), anyInt(), anyBoolean());
        doThrow(new Failure(Code.CATALOG_PUBLICATION_CONFLICT)).when(repository).publish(any(), any(), anyInt(), any(), any()); failure(Code.CATALOG_PUBLICATION_CONFLICT);
    }
    @Test void replayPerformsZeroWritesAndFacts() {
        for (String sqlState : List.of("23505", "40001")) {
            clearInvocations(repository);
            doThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", sqlState))).when(repository).publish(any(), any(), anyInt(), any(), any());
            when(repository.recover(version, null, 1, true)).thenAnswer(invocation -> { assertThat(statuses.get(statuses.size() - 2).isRollbackOnly()).isTrue(); return state; });
            assertThat(publish()).isSameAs(state);
            verify(repository, times(2)).begin(); verify(repository).recover(version, null, 1, true);
            verify(repository, times(1)).publish(version, null, 1, employee, trace);
        }
    }
    @Test void rollsBackAllFactsOnRepositoryFailure() {
        when(repository.publish(any(), any(), anyInt(), any(), any())).thenThrow(new IllegalStateException("PRIVATE")); failure(Code.SERVICE_UNAVAILABLE);
        assertThat(statuses.getFirst().isRollbackOnly()).isTrue(); verify(repository).restore(witness);
        doThrow(new IllegalStateException("PRIVATE-cleanup")).when(repository).restore(witness); failure(Code.SERVICE_UNAVAILABLE);
    }
    @Test void publishCommandHasFrozenFields() { assertThat(Arrays.stream(PublishCommand.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("versionId", "expectedActiveVersionId", "expectedRevision", "caller"); }
    @Test void offsaleCommandHasFrozenFields() { assertThat(Arrays.stream(OffsaleCommand.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("versionId", "expectedRevision", "caller"); }
    @Test void returnsExactCatalogVersionState() {
        assertThat(Arrays.stream(CatalogVersionState.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).containsExactly("catalogVersionId", "productId", "versionNo", "saleStatus", "revision", "publishedAt", "currentActiveVersionId");
        for (UUID active : Arrays.asList(version, null, new UUID(9, 38))) {
            var expected = new CatalogVersionState(version, state.productId(), 1, active == version ? "ACTIVE" : "OFFSALE", 2, state.publishedAt(), active);
            when(repository.publish(version, null, 1, employee, trace)).thenReturn(expected); when(repository.offsale(version, 1, employee, trace)).thenReturn(expected);
            assertThat(publish()).isEqualTo(expected); assertThat(command.offsale(new OffsaleCommand(version, 1, caller))).isEqualTo(expected);
        }
    }
    @Test void usesClosedFailureCodes() {
        assertThat(Code.values()).extracting(Enum::name).containsExactly("UNAUTHENTICATED", "FORBIDDEN", "NOT_FOUND", "PUBLICATION_STALE_VIEW", "CATALOG_PUBLICATION_CONFLICT", "VALIDATION_FAILED", "RATE_LIMITED", "SERVICE_UNAVAILABLE", "INTERNAL");
        when(repository.publish(any(), any(), anyInt(), any(), any())).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", "57014"))); failure(Code.SERVICE_UNAVAILABLE);
        verify(repository, never()).recover(any(), any(), anyInt(), anyBoolean());
        when(repository.begin()).thenThrow(new org.springframework.jdbc.CannotGetJdbcConnectionException("PRIVATE")); failure(Code.SERVICE_UNAVAILABLE);
    }
    @Test void doesNotExposeRequestReason() {
        assertThat(new Failure(Code.SERVICE_UNAVAILABLE).toString()).doesNotContain("PRIVATE", "SQL", "role");
        for (Class<?> type : List.of(PublishCommand.class, OffsaleCommand.class, Caller.class)) assertThat(Arrays.stream(type.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName)).doesNotContain("reason", "role", "snapshot", "idempotencyKey");
    }
    @Test void doesNotExposeDatabaseTypes() {
        for (Class<?> type : List.of(PublishCommand.class, OffsaleCommand.class, Caller.class, CatalogVersionState.class)) assertThat(Arrays.stream(type.getRecordComponents()).map(component -> component.getType().getName())).noneMatch(name -> name.startsWith("org.springframework") || name.startsWith("org.jooq") || name.startsWith("java.sql"));
    }
    @Test void acceptsNullableExpectedActiveOnly() {
        assertThat(new PublishCommand(version, null, 1, caller).expectedActiveVersionId()).isNull();
        assertThatThrownBy(() -> new PublishCommand(null, null, 1, caller)).isInstanceOf(Failure.class);
        assertThatThrownBy(() -> new PublishCommand(version, null, 1, null)).isInstanceOf(Failure.class);
        assertThatThrownBy(() -> new Caller(employee, null)).isInstanceOf(Failure.class);
    }
    @Test void rejectsNonPositiveExpectedRevision() {
        for (int revision : List.of(0, -1, Integer.MIN_VALUE)) {
            assertThatThrownBy(() -> new PublishCommand(version, null, revision, caller)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED));
            assertThatThrownBy(() -> new OffsaleCommand(version, revision, caller)).isInstanceOf(Failure.class);
        }
    }
}
