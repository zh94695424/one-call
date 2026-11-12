package com.onecall.core.customer_iam.internal.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.onecall.core.activation_consent.customerquery.CustomerActivationStatusReader;
import com.onecall.core.catalog_order.customerquery.CustomerSubjectOrderCountReader;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import com.onecall.core.evidence.customerquery.CustomerSubjectEvidenceCountReader;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope;
import com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader;
import java.util.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerSubjectDetailQueryServiceTests {
    private final UUID account = UUID.randomUUID();
    private final UUID subject = UUID.randomUUID();
    private final TrustedCustomerCaller caller = new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB,
            new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null));
    private final TransactionTemplate transaction = mock(TransactionTemplate.class);
    private final DSLContext context = mock(DSLContext.class);
    private final JooqCustomerBusinessNumberAccessRepository repository = mock(JooqCustomerBusinessNumberAccessRepository.class);
    private final CustomerSubjectAccessReader subjects = mock(CustomerSubjectAccessReader.class);
    private final CustomerActivationStatusReader activations = mock(CustomerActivationStatusReader.class);
    private final CustomerSubjectEvidenceCountReader evidence = mock(CustomerSubjectEvidenceCountReader.class);
    private final CustomerSubjectOrderCountReader orders = mock(CustomerSubjectOrderCountReader.class);
    private CustomerSubjectDetailQueryService service;

    @BeforeEach
    void setup() {
        when(transaction.execute(any())).thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0))
                .doInTransaction(mock(org.springframework.transaction.TransactionStatus.class)));
        var role = mock(org.jooq.Record.class);
        when(role.get(0, String.class)).thenReturn("customer_iam_role");
        when(context.fetchOne("select current_user")).thenReturn(role);
        when(subjects.findOwnedSubject(subject, account, null)).thenReturn(Optional.of(subject));
        when(activations.findStatuses(Set.of(subject))).thenReturn(Map.of());
        service = new CustomerSubjectDetailQueryService(transaction, context, repository, subjects, activations, evidence, orders);
    }

    private Optional<CustomerSubjectDetailQueryService.Detail> read() { return service.getCustomerSubject(caller, subject); }
    private void failure(CustomerContextException.Code code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomerContextException.class, error -> {
            assertThat(error.code()).isEqualTo(code);
            assertThat(error.getCause()).isNull();
            assertThat(error.toString()).doesNotContain("CANARY", "jdbc:");
        });
    }

    @Test
    void personalAndOrganizationScopesUseOnlyVerifiedCaller() {
        assertThat(read()).isPresent();
        verify(repository).requireActiveContext(new Scope(account, null));
        verify(subjects).findOwnedSubject(subject, account, null);
        verify(orders).countForWorkspace(subject, account, null);
        UUID organization = UUID.randomUUID();
        when(subjects.findOwnedSubject(subject, account, organization)).thenReturn(Optional.of(subject));
        var organizationCaller = new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.MINIAPP,
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, organization));
        assertThat(service.getCustomerSubject(organizationCaller, subject)).isPresent();
        verify(repository).requireActiveContext(new Scope(account, organization));
        verify(subjects).findOwnedSubject(subject, account, organization);
        verify(orders).countForWorkspace(subject, account, organization);
    }

    @Test
    void nullCallerWorkspaceAndSubjectFailClosedBeforeQueries() {
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.getCustomerSubject(null, subject));
        for (var invalid : List.of(new TrustedCustomerCaller(null, null, null), new TrustedCustomerCaller(account, null, null),
                new TrustedCustomerCaller(account, null, new SelectedCustomerWorkspace(null, null)),
                new TrustedCustomerCaller(account, null, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, null)),
                new TrustedCustomerCaller(account, null, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, UUID.randomUUID()))))
            failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.getCustomerSubject(invalid, null));
        failure(CustomerContextException.Code.VALIDATION_FAILED, () -> service.getCustomerSubject(caller, null));
        verifyNoInteractions(transaction, context, repository, subjects, activations, evidence, orders);
    }

    @Test
    void roleIsAssumedAndAssertedBeforeLifecycleAndSubjectQueries() {
        read();
        var ordered = inOrder(context, repository, subjects);
        ordered.verify(context).execute("SET LOCAL ROLE customer_iam_role");
        ordered.verify(context).fetchOne("select current_user");
        ordered.verify(repository).requireActiveContext(new Scope(account, null));
        ordered.verify(subjects).findOwnedSubject(subject, account, null);
        ordered.verify(subjects).hasOpenRisk(subject);
    }

    @Test
    void roleAssumptionOrAssertionFailureIsUnavailableWithoutOwnerReads() {
        when(context.execute("SET LOCAL ROLE customer_iam_role")).thenThrow(new IllegalStateException("CANARY"));
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, this::read);
        reset(context);
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, this::read);
        var role = mock(org.jooq.Record.class);
        when(role.get(0, String.class)).thenReturn("fixture_admin");
        when(context.fetchOne("select current_user")).thenReturn(role);
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, this::read);
        verifyNoInteractions(repository, subjects, activations, evidence, orders);
    }

    @Test
    void inactiveContextPrecedesSubjectAndRiskReads() {
        doThrow(new CustomerContextException(CustomerContextException.Code.FORBIDDEN)).when(repository).requireActiveContext(any());
        failure(CustomerContextException.Code.FORBIDDEN, this::read);
        verifyNoInteractions(subjects, activations, evidence, orders);
    }

    @Test
    void missingOwnedSubjectIsEmptyAndInvalidProjectionIsInternal() {
        when(subjects.findOwnedSubject(subject, account, null)).thenReturn(Optional.empty());
        assertThat(read()).isEmpty();
        when(subjects.findOwnedSubject(subject, account, null)).thenReturn(null);
        failure(CustomerContextException.Code.INTERNAL, this::read);
        when(subjects.findOwnedSubject(subject, account, null)).thenReturn(Optional.of(UUID.randomUUID()));
        failure(CustomerContextException.Code.INTERNAL, this::read);
        verify(subjects, never()).hasOpenRisk(org.mockito.ArgumentMatchers.<UUID>any());
        verifyNoInteractions(activations, evidence, orders);
    }

    @Test
    void openRiskPrecedesActivationAndBothCounts() {
        when(subjects.hasOpenRisk(subject)).thenReturn(true);
        failure(CustomerContextException.Code.FORBIDDEN, this::read);
        verifyNoInteractions(activations, evidence, orders);
    }

    @Test
    void missingActivationDefaultsInactiveAndPresentStatesAreExact() {
        assertThat(read().orElseThrow().activationStatus()).isEqualTo("INACTIVE");
        for (String status : List.of("INACTIVE", "ACTIVE", "SUSPENDED")) {
            when(activations.findStatuses(Set.of(subject))).thenReturn(Map.of(subject, status));
            assertThat(read().orElseThrow().activationStatus()).isEqualTo(status);
        }
    }

    @Test
    void nullExtraUnknownAndMisboundActivationProjectionsAreInternal() {
        Map<UUID, String> nullStatus = new HashMap<>(); nullStatus.put(subject, null);
        Map<UUID, String> nullKey = new HashMap<>(); nullKey.put(null, "ACTIVE");
        for (Map<UUID, String> invalid : Arrays.asList(null, nullStatus, nullKey,
                Map.of(subject, "CANARY"), Map.of(UUID.randomUUID(), "ACTIVE"),
                Map.of(subject, "ACTIVE", UUID.randomUUID(), "INACTIVE"))) {
            when(activations.findStatuses(Set.of(subject))).thenReturn(invalid);
            failure(CustomerContextException.Code.INTERNAL, this::read);
        }
        verifyNoInteractions(evidence, orders);
    }

    @Test
    void zeroAndLongCountsAreExactAndNegativeCountsFailClosed() {
        assertThat(read()).contains(new CustomerSubjectDetailQueryService.Detail(subject, "INACTIVE", 0, 0));
        when(evidence.countForSubject(subject)).thenReturn((long) Integer.MAX_VALUE + 123);
        when(orders.countForWorkspace(subject, account, null)).thenReturn(Long.MAX_VALUE);
        assertThat(read()).contains(new CustomerSubjectDetailQueryService.Detail(subject, "INACTIVE", (long) Integer.MAX_VALUE + 123, Long.MAX_VALUE));
        when(evidence.countForSubject(subject)).thenReturn(-1L);
        failure(CustomerContextException.Code.INTERNAL, this::read);
        when(evidence.countForSubject(subject)).thenReturn(0L);
        when(orders.countForWorkspace(subject, account, null)).thenReturn(-1L);
        failure(CustomerContextException.Code.INTERNAL, this::read);
    }

    @Test
    void sqlTransactionAndBeanFailuresAreRedactedAndUnknownFaultsAreInternal() {
        var cyclic = new RuntimeException("CANARY");
        var sql = new RuntimeException("jdbc:CANARY", new java.sql.SQLException("CANARY"));
        cyclic.initCause(sql);
        for (RuntimeException error : List.of(cyclic, new org.jooq.exception.DataAccessException("CANARY"),
                new org.springframework.transaction.TransactionSystemException("CANARY"),
                new org.springframework.beans.factory.BeanCreationException("CANARY"))) {
            doThrow(error).when(transaction).execute(any());
            failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, this::read);
        }
        doReturn(null).when(transaction).execute(any());
        failure(CustomerContextException.Code.INTERNAL, this::read);
        doThrow(new IllegalStateException("CANARY")).when(transaction).execute(any());
        failure(CustomerContextException.Code.INTERNAL, this::read);
        doAnswer(invocation -> {
            ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
            throw new org.springframework.transaction.TransactionSystemException("CANARY");
        }).when(transaction).execute(any());
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, this::read);
        when(subjects.findOwnedSubject(subject, account, null)).thenReturn(Optional.empty());
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, this::read);
    }

    @Test
    void oneReadOnlyRepeatableTransactionContainsOrderedOwnerReadsAndNoWrites() {
        assertThat(read()).contains(new CustomerSubjectDetailQueryService.Detail(subject, "INACTIVE", 0, 0));
        var ordered = inOrder(transaction, context, repository, subjects, activations, evidence, orders);
        ordered.verify(transaction).execute(any());
        ordered.verify(context).execute("SET LOCAL ROLE customer_iam_role");
        ordered.verify(context).fetchOne("select current_user");
        ordered.verify(repository).requireActiveContext(new Scope(account, null));
        ordered.verify(subjects).findOwnedSubject(subject, account, null);
        ordered.verify(subjects).hasOpenRisk(subject);
        ordered.verify(activations).findStatuses(Set.of(subject));
        ordered.verify(evidence).countForSubject(subject);
        ordered.verify(orders).countForWorkspace(subject, account, null);
        ordered.verifyNoMoreInteractions();
        assertThat(Arrays.stream(CustomerSubjectDetailQueryService.Detail.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .containsExactly("subjectId", "activationStatus", "evidenceCount", "orderCount");
        verify(transaction, never()).setReadOnly(anyBoolean());
        verify(transaction, never()).setIsolationLevel(anyInt());
        verify(transaction, never()).setPropagationBehavior(anyInt());
    }
}
