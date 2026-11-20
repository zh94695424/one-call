package com.onecall.core.catalog_order.internal.operationsdraft;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.EmployeeAuthoritySnapshot;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.TransactionWitness;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;

class DefaultOperationsCatalogDraftCommandTests {
    final TransactionTemplate transaction = mock(TransactionTemplate.class);
    final EmployeeAuthorityReadPort authority = mock(EmployeeAuthorityReadPort.class);
    final CatalogDraftRepository repository = mock(CatalogDraftRepository.class);
    final UUID employee = new UUID(0, 3);
    final Caller caller = new Caller(employee, new UUID(0, 4));
    final TransactionWitness witness = new TransactionWitness("login", 1, 1);
    final Commercial commercial = CatalogDraftSemanticsTests.COMMERCIAL;
    final CreateCommand create = new CreateCommand(CatalogDraftSemanticsTests.PRODUCT, 1, commercial, List.of(), List.of(), caller);
    final ReplaceCommand replace = new ReplaceCommand(CatalogDraftSemanticsTests.VERSION, 1, commercial, caller);
    final CatalogVersionDraft draft = CatalogDraftSemantics.response(CatalogDraftSemanticsTests.valid());
    final DefaultOperationsCatalogDraftCommand command = new DefaultOperationsCatalogDraftCommand(transaction, authority, repository);
    final java.util.ArrayList<SimpleTransactionStatus> statuses = new java.util.ArrayList<>();
    @BeforeEach void setup() {
        when(transaction.execute(any())).thenAnswer(invocation -> { var status = new SimpleTransactionStatus(); statuses.add(status); TransactionCallback<?> callback = invocation.getArgument(0); return callback.doInTransaction(status); });
        when(repository.begin()).thenReturn(witness); when(authority.read(employee, witness)).thenReturn(new EmployeeAuthoritySnapshot(employee, "ACTIVE", "ADMIN", 1));
        when(repository.create(create)).thenReturn(new CreateResult(draft, true)); when(repository.replace(replace)).thenReturn(draft);
    }
    void failure(Runnable action, Code code) { CatalogDraftSemanticsTests.failure(action, code); }
    @Test void requiresCurrentActiveAdminForBothCommands() { command.create(create); command.replace(replace); verify(authority, times(2)).read(employee, witness); var order = inOrder(repository, authority); order.verify(repository).begin(); order.verify(authority).read(employee, witness); order.verify(repository).catalogRole(witness); order.verify(repository).create(create); order.verify(repository).restore(witness); }
    @Test void opsAndDisabledEmployeesAreForbidden() { for (String[] pair : List.of(new String[] {"ACTIVE", "OPS"}, new String[] {"DISABLED", "ADMIN"}, new String[] {"DISABLED", "OPS"})) { when(authority.read(employee, witness)).thenReturn(new EmployeeAuthoritySnapshot(employee, pair[0], pair[1], 1)); failure(() -> command.create(create), Code.FORBIDDEN); failure(() -> command.replace(replace), Code.FORBIDDEN); } verify(repository, never()).create(any()); verify(repository, never()).replace(any()); assertThat(statuses).allSatisfy(status -> assertThat(status.isRollbackOnly()).isTrue()); }
    @Test void missingMalformedAndUnavailableAuthorityFailClosed() { for (EmployeeAuthoritySnapshot snapshot : java.util.Arrays.asList(null, new EmployeeAuthoritySnapshot(employee, "UNKNOWN", "ADMIN", 1), new EmployeeAuthoritySnapshot(employee, "ACTIVE", "OWNER", 1), new EmployeeAuthoritySnapshot(employee, "ACTIVE", "ADMIN", 0), new EmployeeAuthoritySnapshot(UUID.randomUUID(), "ACTIVE", "ADMIN", 1))) { when(authority.read(employee, witness)).thenReturn(snapshot); failure(() -> command.create(create), Code.SERVICE_UNAVAILABLE); } when(authority.read(employee, witness)).thenThrow(new EmployeeAuthorityReadPort.Failure(EmployeeAuthorityReadPort.Code.SERVICE_UNAVAILABLE)); failure(() -> command.replace(replace), Code.SERVICE_UNAVAILABLE); verify(repository, never()).catalogRole(any()); }
    @Test void createReturnsCreatedOrReplayStatusWithoutDuplicateFacts() { assertThat(command.create(create).created()).isTrue(); when(repository.create(create)).thenReturn(new CreateResult(draft, false)); assertThat(command.create(create).created()).isFalse(); verify(repository, times(2)).create(create); verify(repository, never()).recoverCreate(any()); }
    @Test void changedNaturalProjectionAndNonDraftCreateConflict() { when(repository.create(create)).thenThrow(new Failure(Code.CATALOG_DRAFT_CONFLICT)); failure(() -> command.create(create), Code.CATALOG_DRAFT_CONFLICT); verify(repository, never()).recoverCreate(any()); assertThat(statuses.getFirst().isRollbackOnly()).isTrue(); }
    @Test void missingProductAndVersionAreNotFound() { when(repository.create(create)).thenThrow(new Failure(Code.NOT_FOUND)); when(repository.replace(replace)).thenThrow(new Failure(Code.NOT_FOUND)); failure(() -> command.create(create), Code.NOT_FOUND); failure(() -> command.replace(replace), Code.NOT_FOUND); }
    @Test void replaceAppliesOnlyAtExpectedRevision() { assertThat(command.replace(replace)).isSameAs(draft); verify(repository).replace(argThat(value -> value.expectedRevision() == 1 && value.commercial().equals(commercial))); verify(repository, never()).recoverReplace(any()); }
    @Test void sameContentAtExpectedRevisionStillIncrementsOnce() { var updated = CatalogDraftSemantics.response(CatalogDraftSemanticsTests.stored(CatalogDraftSemanticsTests.GOLDEN, CatalogDraftSemanticsTests.HASH, 2, CatalogDraftSemanticsTests.NOW)); when(repository.replace(replace)).thenReturn(updated); assertThat(command.replace(replace).revision()).isEqualTo(2); verify(repository, times(1)).replace(replace); }
    @Test void exactSuccessorProjectionReplaysWithoutWrites() { var updated = CatalogDraftSemantics.response(CatalogDraftSemanticsTests.stored(CatalogDraftSemanticsTests.GOLDEN, CatalogDraftSemanticsTests.HASH, 2, CatalogDraftSemanticsTests.NOW)); when(repository.replace(replace)).thenReturn(updated); assertThat(command.replace(replace)).isSameAs(updated); verify(repository, never()).recoverReplace(any()); }
    @Test void allOtherRevisionRelationsAreStale() { for (int revision : List.of(1, 2, Integer.MAX_VALUE)) { var request = new ReplaceCommand(replace.versionId(), revision, commercial, caller); when(repository.replace(request)).thenThrow(new Failure(Code.CATALOG_DRAFT_STALE_VIEW)); failure(() -> command.replace(request), Code.CATALOG_DRAFT_STALE_VIEW); } verify(repository, never()).recoverReplace(any()); }
    @Test void nonDraftConflictPrecedesStaleAndReplay() { when(repository.replace(replace)).thenThrow(new Failure(Code.CATALOG_DRAFT_CONFLICT)); failure(() -> command.replace(replace), Code.CATALOG_DRAFT_CONFLICT); verify(repository, never()).recoverReplace(any()); }
    @Test void auditMutationAndCleanupFailuresRollbackEverything() { when(repository.create(create)).thenThrow(new IllegalStateException("PRIVATE")); failure(() -> command.create(create), Code.INTERNAL); doThrow(new IllegalStateException("PRIVATE cleanup")).when(repository).restore(witness); failure(() -> command.replace(replace), Code.SERVICE_UNAVAILABLE); assertThat(statuses).allSatisfy(status -> assertThat(status.isRollbackOnly()).isTrue()); }
    @Test void recoveryReauthenticatesOnceAndNeverRetriesWrites() {
        for (String wrapperState : new String[] {null, "00000"}) {
            for (String nestedState : List.of("55P02", "55P03", "23505", "40001")) {
                reset(repository); when(repository.begin()).thenReturn(witness);
                var failure = new org.jooq.exception.DataAccessException("PRIVATE wrapper", new SQLException("PRIVATE", nestedState)) {
                    @Override public String sqlState() { return wrapperState; }
                };
                when(repository.create(create)).thenThrow(failure);
                if (nestedState.startsWith("55")) {
                    failure(() -> command.create(create), Code.CATALOG_DRAFT_CONFLICT);
                    verify(repository, never()).recoverCreate(any()); verify(repository, never()).restore(any());
                } else {
                    when(repository.recoverCreate(create)).thenReturn(new CreateResult(draft, false));
                    assertThat(command.create(create).created()).isFalse();
                    verify(repository, times(1)).recoverCreate(create); verify(repository, times(1)).restore(witness);
                }
                reset(repository); when(repository.begin()).thenReturn(witness);
                when(repository.create(create)).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", "40001")));
                when(repository.recoverCreate(create)).thenThrow(failure);
                failure(() -> command.create(create), nestedState.startsWith("55") ? Code.CATALOG_DRAFT_CONFLICT : Code.SERVICE_UNAVAILABLE);
                verify(repository, times(1)).recoverCreate(create); verify(repository, never()).restore(any());
            }
        }
        for (RuntimeException unknown : List.of(new org.jooq.exception.DataAccessException("PRIVATE"),
                new RuntimeException(new SQLException("PRIVATE")), new RuntimeException(new SQLException("PRIVATE", "00000")),
                new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", "XX000")), new IllegalStateException("PRIVATE"))) {
            boolean sql = !(unknown instanceof IllegalStateException);
            reset(repository); when(repository.begin()).thenReturn(witness); when(repository.create(create)).thenThrow(unknown);
            failure(() -> command.create(create), sql ? Code.SERVICE_UNAVAILABLE : Code.INTERNAL);
            verify(repository, never()).recoverCreate(any()); verify(repository, times(sql ? 0 : 1)).restore(witness);
            reset(repository); when(repository.begin()).thenReturn(witness);
            when(repository.create(create)).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", "40001")));
            when(repository.recoverCreate(create)).thenThrow(unknown);
            failure(() -> command.create(create), sql ? Code.SERVICE_UNAVAILABLE : Code.INTERNAL);
            verify(repository, times(1)).recoverCreate(create); verify(repository, times(sql ? 0 : 1)).restore(witness);
        }
        for (String state : List.of("23505", "40001")) { reset(repository, authority); when(repository.begin()).thenReturn(witness); when(authority.read(employee, witness)).thenReturn(new EmployeeAuthoritySnapshot(employee, "ACTIVE", "ADMIN", 1)); when(repository.create(create)).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", state))); when(repository.recoverCreate(create)).thenReturn(new CreateResult(draft, false)); assertThat(command.create(create).created()).isFalse(); verify(repository, times(1)).create(create); verify(repository, times(1)).recoverCreate(create); verify(authority, times(2)).read(employee, witness); }
        for (String state : List.of("55P02", "55P03", "40001")) {
            reset(repository, authority); when(repository.begin()).thenReturn(witness);
            when(authority.read(employee, witness)).thenThrow(new EmployeeAuthorityReadPort.Failure(EmployeeAuthorityReadPort.Code.SERVICE_UNAVAILABLE, state));
            failure(() -> command.create(create), Code.SERVICE_UNAVAILABLE);
            verify(repository, never()).catalogRole(any()); verify(repository, never()).create(any()); verify(repository, never()).recoverCreate(any());
            verify(authority, times(1)).read(employee, witness);
        }
        for (String state : List.of("55P02", "55P03", "40001")) {
            reset(repository, authority); when(repository.begin()).thenReturn(witness);
            when(authority.read(employee, witness)).thenReturn(new EmployeeAuthoritySnapshot(employee, "ACTIVE", "ADMIN", 1)).thenThrow(new EmployeeAuthorityReadPort.Failure(EmployeeAuthorityReadPort.Code.SERVICE_UNAVAILABLE, state));
            when(repository.create(create)).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", "40001")));
            failure(() -> command.create(create), Code.SERVICE_UNAVAILABLE);
            verify(repository, times(1)).create(create); verify(repository, never()).recoverCreate(any());
            verify(authority, times(2)).read(employee, witness);
        }
        reset(authority); when(authority.read(employee, witness)).thenReturn(new EmployeeAuthoritySnapshot(employee, "ACTIVE", "ADMIN", 1));
        for (String state : List.of("55P02", "55P03")) {
            reset(repository); when(repository.begin()).thenReturn(witness); when(repository.create(create)).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", state))); failure(() -> command.create(create), Code.CATALOG_DRAFT_CONFLICT); verify(repository, never()).recoverCreate(any());
            doThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", "40001"))).when(repository).create(create); when(repository.recoverCreate(create)).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", state))); failure(() -> command.create(create), Code.CATALOG_DRAFT_CONFLICT);
        }
        for (String state : List.of("23505", "40001")) {
            reset(repository); when(repository.begin()).thenReturn(witness);
            when(repository.replace(replace)).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", state)));
            when(repository.recoverReplace(replace)).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", state)));
            failure(() -> command.replace(replace), Code.SERVICE_UNAVAILABLE);
            verify(repository, times(1)).replace(replace); verify(repository, times(1)).recoverReplace(replace);
            reset(repository); when(repository.begin()).thenReturn(witness);
            when(repository.replace(replace)).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", state)));
            when(repository.recoverReplace(replace)).thenThrow(new IllegalStateException("PRIVATE recovery"));
            failure(() -> command.replace(replace), Code.INTERNAL);
            verify(repository, times(1)).replace(replace); verify(repository, times(1)).recoverReplace(replace);
        }
        reset(repository); when(repository.begin()).thenReturn(witness); when(repository.replace(replace)).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", "40001"))); when(repository.recoverReplace(replace)).thenThrow(new Failure(Code.FORBIDDEN)); failure(() -> command.replace(replace), Code.FORBIDDEN); verify(repository, times(1)).replace(replace); verify(repository, times(1)).recoverReplace(replace);
    }
    @Test void portCraftedInvalidMatrixReturns422BeforeTransactionOrAuthority() { for (Commercial invalid : List.of(new Commercial("ONE_TIME", "ENTERPRISE", 0L, "CNY", "ONCE", 1, 0), new Commercial("SUBSCRIPTION", "PERSONAL", 0L, "CNY", "ONCE", 1, 1))) { failure(() -> command.create(new CreateCommand(create.productId(), 1, invalid, List.of(), List.of(), caller)), Code.VALIDATION_FAILED); failure(() -> command.replace(new ReplaceCommand(replace.versionId(), 1, invalid, caller)), Code.VALIDATION_FAILED); } verifyNoInteractions(transaction, authority, repository); }
}
