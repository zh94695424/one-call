package com.onecall.core.customer_iam.internal.application;

import static com.onecall.core.customer_iam.internal.application.CustomerContext.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.core.customer_iam.internal.persistence.CustomerContextRepository;
import com.onecall.core.customer_iam.internal.persistence.CustomerPersistenceConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

class CustomerContextServiceTests {
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final DSLContext context = mock(DSLContext.class);
    private final CustomerContextRepository repository = mock(CustomerContextRepository.class);
    private final TransactionTemplate transaction = new TransactionTemplate(manager);
    private final TrustedCustomerCaller caller = new TrustedCustomerCaller(UUID.randomUUID(), TrustedCustomerCaller.Channel.WEB);
    private final CustomerContextService service = new CustomerContextService(transaction, context, repository);

    @BeforeEach
    void setup() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var role = mock(org.jooq.Record.class);
        when(role.get(0, String.class)).thenReturn("customer_iam_role");
        when(context.fetchOne("select current_user")).thenReturn(role);
        when(repository.findActiveContext(caller)).thenReturn(Optional.of(projection(List.of(personal()))));
    }

    CustomerContext projection(List<WorkspaceSummary> workspaces) {
        return new CustomerContext(new AccountSummary(caller.accountId(), "Name", AccountStatus.ACTIVE), workspaces, caller.channel());
    }

    WorkspaceSummary personal() { return new WorkspaceSummary(WorkspaceType.PERSONAL, null, null, null, WorkspaceStatus.ACTIVE); }
    WorkspaceSummary organization(UUID id) {
        return new WorkspaceSummary(WorkspaceType.ORGANIZATION, id, "Organization", OrganizationRole.AUDITOR, WorkspaceStatus.ACTIVE);
    }

    @Test
    void trustedCallerIsTheOnlyAccountAndChannelSource() {
        assertThat(service.getCustomerMe(caller).channel()).isEqualTo(caller.channel());
        verify(repository).findActiveContext(same(caller));
        assertThat(Arrays.stream(CustomerContextService.class.getDeclaredMethods()).filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .map(java.lang.reflect.Method::getName)).containsExactly("getCustomerMe");
        for (TrustedCustomerCaller invalid : Arrays.asList(null, new TrustedCustomerCaller(null, caller.channel()),
                new TrustedCustomerCaller(caller.accountId(), null))) {
            expect(CustomerContextException.Code.INTERNAL, () -> service.getCustomerMe(invalid));
        }
    }

    @Test
    void activeContextHasExactPersonalAndOrganizationProjection() {
        var ordered = new ArrayList<>(List.of(personal(), organization(UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff")),
                organization(UUID.fromString("80000000-0000-0000-0000-000000000000")),
                organization(UUID.fromString("80000000-0000-0000-8000-000000000000"))));
        var projection = projection(ordered);
        when(repository.findActiveContext(caller)).thenReturn(Optional.of(projection));
        assertThat(service.getCustomerMe(caller).workspaces()).containsExactlyElementsOf(ordered);
        ordered.clear();
        assertThat(projection.workspaces()).hasSize(4);
        assertThatThrownBy(() -> projection.workspaces().clear()).isInstanceOf(UnsupportedOperationException.class);
        var json = JsonMapper.builder().build().valueToTree(projection);
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("account", "workspaces", "channel");
        assertThat(json.get("workspaces").get(0).propertyNames()).containsExactlyInAnyOrder("workspaceType", "status");
        assertThat(OrganizationRole.values()).extracting(Enum::name).containsExactly("ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR");
        assertThat(AccountStatus.values()).extracting(Enum::name).containsExactly("ACTIVE", "DISABLED");
        assertThat(WorkspaceType.values()).extracting(Enum::name).containsExactly("PERSONAL", "ORGANIZATION");
        assertThat(WorkspaceStatus.values()).extracting(Enum::name).containsExactly("ACTIVE");
    }

    @Test
    void missingAndDisabledAccountsAreUnauthenticated() {
        when(repository.findActiveContext(caller)).thenReturn(Optional.empty());
        expect(CustomerContextException.Code.UNAUTHENTICATED, () -> service.getCustomerMe(caller));
        when(repository.findActiveContext(caller)).thenReturn(Optional.of(new CustomerContext(
                new AccountSummary(null, null, AccountStatus.DISABLED), List.of(), null)));
        expect(CustomerContextException.Code.UNAUTHENTICATED, () -> service.getCustomerMe(caller));
    }

    @Test
    void invalidProjectionNeverReturnsPartialContext() {
        var organization = organization(UUID.randomUUID());
        var invalid = new ArrayList<CustomerContext>();
        for (List<WorkspaceSummary> rows : List.of(List.<WorkspaceSummary>of(), List.of(organization),
                List.of(personal(), personal()), List.of(organization, personal()), List.of(personal(), organization, organization),
                List.of(personal(), new WorkspaceSummary(WorkspaceType.ORGANIZATION, UUID.randomUUID(), "Name", null, WorkspaceStatus.ACTIVE)),
                List.of(new WorkspaceSummary(WorkspaceType.PERSONAL, UUID.randomUUID(), null, null, WorkspaceStatus.ACTIVE)),
                List.of(personal(), new WorkspaceSummary(WorkspaceType.ORGANIZATION, null, "Name", OrganizationRole.MEMBER, WorkspaceStatus.ACTIVE)),
                List.of(personal(), new WorkspaceSummary(WorkspaceType.ORGANIZATION, UUID.randomUUID(), null, OrganizationRole.MEMBER, WorkspaceStatus.ACTIVE)))) {
            invalid.add(projection(rows));
        }
        invalid.add(new CustomerContext(new AccountSummary(caller.accountId(), null, AccountStatus.ACTIVE), List.of(personal()), caller.channel()));
        invalid.add(new CustomerContext(new AccountSummary(caller.accountId(), "Name", null), List.of(personal()), caller.channel()));
        invalid.add(new CustomerContext(null, List.of(personal()), caller.channel()));
        for (CustomerContext result : invalid) {
            when(repository.findActiveContext(caller)).thenReturn(Optional.of(result));
            expect(CustomerContextException.Code.INTERNAL, () -> service.getCustomerMe(caller));
        }
        assertThatThrownBy(() -> AccountStatus.valueOf("UNKNOWN")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrganizationRole.valueOf("OWNER")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roleAssertionPrecedesRepositoryAndTransactionIsReadOnly() throws Exception {
        var configuration = CustomerPersistenceConfiguration.class.getDeclaredMethod("customerQueryTransactionTemplate",
                org.springframework.jdbc.datasource.DataSourceTransactionManager.class);
        configuration.setAccessible(true);
        var template = (TransactionTemplate) configuration.invoke(new CustomerPersistenceConfiguration(),
                new org.springframework.jdbc.datasource.DataSourceTransactionManager());
        assertThat(template.isReadOnly()).isTrue();
        assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        service.getCustomerMe(caller);
        var order = inOrder(context, repository, manager);
        order.verify(context).execute("SET LOCAL ROLE customer_iam_role");
        order.verify(context).fetchOne("select current_user");
        order.verify(repository).findActiveContext(caller);
        order.verify(manager).commit(any());
        clearInvocations(repository);
        when(context.fetchOne("select current_user")).thenReturn(null);
        expect(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.getCustomerMe(caller));
        verifyNoInteractions(repository);
        verify(manager).rollback(any());
        clearInvocations(context, repository, manager);
        var wrongRole = mock(org.jooq.Record.class);
        when(wrongRole.get(0, String.class)).thenReturn("postgres");
        when(context.fetchOne("select current_user")).thenReturn(wrongRole);
        expect(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.getCustomerMe(caller));
        verifyNoInteractions(repository);
        verify(manager).rollback(any());
    }

    @Test
    void technicalAndProgrammingFailuresAreSanitized() {
        for (RuntimeException failure : List.of(new org.jooq.exception.DataAccessException("CANARY", new java.sql.SQLException("CANARY", "42501")),
                new org.springframework.dao.DataAccessResourceFailureException("CANARY"),
                new org.springframework.transaction.CannotCreateTransactionException("CANARY"), new IllegalStateException("CANARY"))) {
            reset(repository);
            when(repository.findActiveContext(caller)).thenThrow(failure);
            expect(failure instanceof IllegalStateException ? CustomerContextException.Code.INTERNAL
                    : CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.getCustomerMe(caller));
            verify(repository, times(1)).findActiveContext(caller);
        }
        verify(manager, times(4)).rollback(any());
    }

    private void expect(CustomerContextException.Code code, Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(CustomerContextException.class, failure -> {
            assertThat(failure.code()).isEqualTo(code);
            assertThat(failure.getCause()).isNull();
            assertThat(failure.toString()).doesNotContain("CANARY");
        });
    }
}
