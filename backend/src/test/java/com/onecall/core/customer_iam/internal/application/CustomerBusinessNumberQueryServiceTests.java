package com.onecall.core.customer_iam.internal.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.onecall.core.activation_consent.customerquery.CustomerActivationStatusReader;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerBusinessNumberQueryServiceTests {
    private final UUID account = UUID.randomUUID();
    private final TrustedCustomerCaller caller = new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB,
            new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null));
    private final DSLContext context = mock(DSLContext.class);
    private final TransactionTemplate transaction = mock(TransactionTemplate.class);
    private final JooqCustomerBusinessNumberAccessRepository access = mock(JooqCustomerBusinessNumberAccessRepository.class);
    private final CustomerBusinessNumberReader reader = mock(CustomerBusinessNumberReader.class);
    private final CustomerActivationStatusReader activations = mock(CustomerActivationStatusReader.class);
    private CustomerBusinessNumberQueryService service;

    @BeforeEach
    void setup() {
        when(transaction.execute(any())).thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0)).doInTransaction(mock(org.springframework.transaction.TransactionStatus.class)));
        var role = mock(org.jooq.Record.class); when(role.get(0, String.class)).thenReturn("customer_iam_role");
        when(context.fetchOne("select current_user")).thenReturn(role);
        when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of());
        when(activations.findStatuses(anySet())).thenReturn(Map.of());
        service = new CustomerBusinessNumberQueryService(transaction, context, access, reader, activations);
    }

    private CustomerBusinessNumberQueryService.Request request(Integer limit) {
        return new CustomerBusinessNumberQueryService.Request(Optional.empty(), Optional.ofNullable(limit));
    }
    private Row row(String binding, String realName, String numberType) {
        return new Row(UUID.randomUUID(), UUID.randomUUID(), "138****0000", numberType, binding, realName, Instant.parse("2026-11-11T00:00:00.000001Z"));
    }
    private void failure(CustomerContextException.Code expected, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(CustomerContextException.class,
                error -> { assertThat(error.code()).isEqualTo(expected); assertThat(error.getCause()).isNull(); });
    }

    @Test
    void defaultBoundsAndLookaheadRequestAreExact() {
        for (Integer limit : new Integer[]{null, 1, 20, 100}) {
            service.listCustomerBusinessNumbers(caller, request(limit));
            verify(reader).fetch(new Scope(account, null), Optional.empty(), (limit == null ? 20 : limit) + 1);
            clearInvocations(reader);
        }
        for (int limit : new int[]{-1, 0, 101, Integer.MAX_VALUE}) failure(CustomerContextException.Code.VALIDATION_FAILED, () -> request(limit));
    }

    @Test
    void trustedWorkspaceAndCurrentLifecycleAreRequiredBeforeReaders() {
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.listCustomerBusinessNumbers(new TrustedCustomerCaller(account, TrustedCustomerCaller.Channel.WEB), request(1)));
        doThrow(new CustomerContextException(CustomerContextException.Code.FORBIDDEN)).when(access).requireActiveContext(any());
        failure(CustomerContextException.Code.FORBIDDEN, () -> service.listCustomerBusinessNumbers(caller, request(1)));
        verifyNoInteractions(reader, activations);
        var order = inOrder(context, access); order.verify(context).execute("SET LOCAL ROLE customer_iam_role"); order.verify(context).fetchOne("select current_user"); order.verify(access).requireActiveContext(new Scope(account, null));
    }

    @Test
    void allBindingActivationAndRealNameStatesComposeWithoutFiltering() {
        for (String binding : List.of("ACTIVE", "DISABLED", "RETIRED")) for (String activation : List.of("MISSING", "INACTIVE", "ACTIVE", "SUSPENDED"))
            for (String realName : List.of("UNVERIFIED", "VERIFIED")) for (String type : List.of("MOBILE", "LANDLINE")) {
                var row = row(binding, realName, type); when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(row));
                when(activations.findStatuses(anySet())).thenReturn(activation.equals("MISSING") ? Map.of() : Map.of(row.subjectId(), activation));
                var item = service.listCustomerBusinessNumbers(caller, request(1)).items().getFirst();
                assertThat(item).isEqualTo(new CustomerBusinessNumberQueryService.Item(row.bindingId(), row.subjectId(), row.numberMasked(), type, binding, activation.equals("MISSING") ? "INACTIVE" : activation, realName));
            }
    }

    @Test
    void missingActivationDefaultsToInactiveAndForeignProjectionFailsClosed() {
        var row = row("ACTIVE", "VERIFIED", "MOBILE"); when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(row));
        assertThat(service.listCustomerBusinessNumbers(caller, request(1)).items().getFirst().activationStatus()).isEqualTo("INACTIVE");
        verify(activations).findStatuses(Set.of(row.subjectId()));
        for (var statuses : List.of(Map.of(UUID.randomUUID(), "ACTIVE"), Map.of(row.subjectId(), "CORRUPT"))) {
            when(activations.findStatuses(anySet())).thenReturn(statuses);
            failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerBusinessNumbers(caller, request(1)));
        }
        var nullStatus = new java.util.HashMap<UUID, String>(); nullStatus.put(row.subjectId(), null);
        var nullSubject = new java.util.HashMap<UUID, String>(); nullSubject.put(null, "ACTIVE");
        for (var statuses : java.util.Arrays.asList(null, nullStatus, nullSubject)) {
            when(activations.findStatuses(anySet())).thenReturn(statuses);
            failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerBusinessNumbers(caller, request(1)));
        }
        var second = new Row(UUID.randomUUID(), row.subjectId(), row.numberMasked(), row.numberType(), row.bindingStatus(), row.realNameStatus(), row.subjectCreatedAt().minusSeconds(1));
        when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(row, second));
        when(activations.findStatuses(anySet())).thenReturn(Map.of(row.subjectId(), "ACTIVE"));
        clearInvocations(activations);
        assertThat(service.listCustomerBusinessNumbers(caller, request(1)).items()).hasSize(1);
        verify(activations, times(1)).findStatuses(Set.of(row.subjectId()));
        var activationContext = mock(DSLContext.class);
        var activationRows = mock(org.jooq.Result.class);
        var activationRow = mock(org.jooq.Record.class);
        when(activationRow.get("subject_id", UUID.class)).thenReturn(row.subjectId());
        when(activationRow.get("status", String.class)).thenReturn("ACTIVE");
        when(activationRows.iterator()).thenAnswer(invocation -> List.of(activationRow, activationRow).iterator());
        when(activationContext.fetch(anyString(), any(Object[].class))).thenReturn(activationRows);
        var duplicateActivationService = new CustomerBusinessNumberQueryService(transaction, context, access, reader,
                new com.onecall.core.activation_consent.internal.customerquery.JooqCustomerActivationStatusReader(activationContext));
        failure(CustomerContextException.Code.INTERNAL, () -> duplicateActivationService.listCustomerBusinessNumbers(caller, request(1)));
        verify(activationContext, times(1)).fetch("SELECT subject_id, status FROM activation_consent.service_activation WHERE subject_id IN (?)", row.subjectId());
    }

    @Test
    void lookaheadUsesLastReturnedRowAndNullCursorOnTail() {
        var first = row("RETIRED", "VERIFIED", "MOBILE");
        var second = new Row(UUID.randomUUID(), UUID.randomUUID(), "010****0000", "LANDLINE", "DISABLED", "UNVERIFIED", first.subjectCreatedAt().minusSeconds(1));
        when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(first, second));
        var page = service.listCustomerBusinessNumbers(caller, request(1));
        assertThat(page.items()).hasSize(1); assertThat(page.hasMore()).isTrue();
        assertThat(page.nextAnchor()).contains(new Anchor(first.subjectCreatedAt(), first.bindingId()));
        verify(activations).findStatuses(Set.of(first.subjectId(), second.subjectId()));
        when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(second));
        page = service.listCustomerBusinessNumbers(caller, request(1));
        assertThat(page.hasMore()).isFalse(); assertThat(page.nextAnchor()).isEmpty();
        var maximumRows = new java.util.ArrayList<Row>();
        for (int index = 0; index < 101; index++) maximumRows.add(new Row(UUID.randomUUID(), UUID.randomUUID(), first.numberMasked(), first.numberType(), first.bindingStatus(), first.realNameStatus(), first.subjectCreatedAt().minusSeconds(index)));
        when(reader.fetch(any(), any(), anyInt())).thenReturn(maximumRows);
        clearInvocations(activations);
        page = service.listCustomerBusinessNumbers(caller, request(100));
        assertThat(page.items()).hasSize(100); assertThat(page.hasMore()).isTrue();
        assertThat(page.nextAnchor()).contains(new Anchor(maximumRows.get(99).subjectCreatedAt(), maximumRows.get(99).bindingId()));
        verify(activations, times(1)).findStatuses(maximumRows.stream().map(Row::subjectId).collect(java.util.stream.Collectors.toSet()));
        verifyNoMoreInteractions(activations);
    }

    @Test
    void emptySingleAndMultiPageProjectionIsExact() {
        var empty = service.listCustomerBusinessNumbers(caller, request(null));
        assertThat(empty.items()).isEmpty(); assertThat(empty.nextAnchor()).isEmpty(); assertThat(empty.hasMore()).isFalse();
        var row = row("ACTIVE", "VERIFIED", "MOBILE"); when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(row));
        var page = service.listCustomerBusinessNumbers(caller, request(20));
        assertThat(page.items()).hasSize(1); assertThat(page.items().getFirst().bindingId()).isEqualTo(row.bindingId());
        assertThatThrownBy(() -> page.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(row, row));
        failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerBusinessNumbers(caller, request(20)));
    }

    @Test
    void technicalAndProjectionFailuresReturnNoPartialOrSensitivePage() {
        var valid = row("ACTIVE", "VERIFIED", "MOBILE");
        var invalidRows = new java.util.ArrayList<Row>();
        invalidRows.add(null);
        invalidRows.add(new Row(null, valid.subjectId(), valid.numberMasked(), valid.numberType(), valid.bindingStatus(), valid.realNameStatus(), valid.subjectCreatedAt()));
        invalidRows.add(new Row(valid.bindingId(), null, valid.numberMasked(), valid.numberType(), valid.bindingStatus(), valid.realNameStatus(), valid.subjectCreatedAt()));
        for (Instant time : java.util.Arrays.asList(null, valid.subjectCreatedAt().plusNanos(1)))
            invalidRows.add(new Row(valid.bindingId(), valid.subjectId(), valid.numberMasked(), valid.numberType(), valid.bindingStatus(), valid.realNameStatus(), time));
        for (String mask : java.util.Arrays.asList(null, "", "   ", "a".repeat(65), "\ud83d\ude00".repeat(65), "MASK\0CANARY", "\ud800", "\udc00"))
            invalidRows.add(new Row(valid.bindingId(), valid.subjectId(), mask, valid.numberType(), valid.bindingStatus(), valid.realNameStatus(), valid.subjectCreatedAt()));
        for (String value : java.util.Arrays.asList(null, "CORRUPT_CANARY")) {
            invalidRows.add(new Row(valid.bindingId(), valid.subjectId(), valid.numberMasked(), value, valid.bindingStatus(), valid.realNameStatus(), valid.subjectCreatedAt()));
            invalidRows.add(new Row(valid.bindingId(), valid.subjectId(), valid.numberMasked(), valid.numberType(), value, valid.realNameStatus(), valid.subjectCreatedAt()));
            invalidRows.add(new Row(valid.bindingId(), valid.subjectId(), valid.numberMasked(), valid.numberType(), valid.bindingStatus(), value, valid.subjectCreatedAt()));
        }
        for (Row invalid : invalidRows) {
            when(reader.fetch(any(), any(), anyInt())).thenReturn(java.util.Arrays.asList(invalid));
            failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerBusinessNumbers(caller, request(1)));
        }
        when(reader.fetch(any(), any(), anyInt())).thenReturn(null);
        failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerBusinessNumbers(caller, request(1)));
        var older = new Row(UUID.randomUUID(), UUID.randomUUID(), valid.numberMasked(), valid.numberType(), valid.bindingStatus(), valid.realNameStatus(), valid.subjectCreatedAt().minusSeconds(1));
        var oldest = new Row(UUID.randomUUID(), UUID.randomUUID(), valid.numberMasked(), valid.numberType(), valid.bindingStatus(), valid.realNameStatus(), valid.subjectCreatedAt().minusSeconds(2));
        when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(valid, older, oldest));
        failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerBusinessNumbers(caller, request(1)));
        var duplicateBinding = new Row(valid.bindingId(), older.subjectId(), older.numberMasked(), older.numberType(), older.bindingStatus(), older.realNameStatus(), older.subjectCreatedAt());
        var low = new Row(UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff"), valid.subjectId(), valid.numberMasked(), valid.numberType(), valid.bindingStatus(), valid.realNameStatus(), valid.subjectCreatedAt());
        var high = new Row(UUID.fromString("80000000-0000-0000-0000-000000000000"), valid.subjectId(), valid.numberMasked(), valid.numberType(), valid.bindingStatus(), valid.realNameStatus(), valid.subjectCreatedAt());
        for (var rows : List.of(List.of(valid, duplicateBinding), List.of(older, valid), List.of(high, low))) {
            when(reader.fetch(any(), any(), anyInt())).thenReturn(rows);
            failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerBusinessNumbers(caller, request(1)));
        }
        when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(valid));
        for (Anchor anchor : List.of(new Anchor(valid.subjectCreatedAt().minusSeconds(1), valid.bindingId()),
                new Anchor(valid.subjectCreatedAt(), valid.bindingId()), new Anchor(high.subjectCreatedAt(), high.bindingId()))) {
            if (anchor.bindingId().equals(high.bindingId())) when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(low));
            failure(CustomerContextException.Code.INTERNAL, () -> service.listCustomerBusinessNumbers(caller,
                    new CustomerBusinessNumberQueryService.Request(Optional.of(anchor), Optional.of(1))));
        }
        verifyNoInteractions(activations);
        var maximumMask = new Row(valid.bindingId(), valid.subjectId(), "\ud83d\ude00".repeat(64), valid.numberType(), valid.bindingStatus(), valid.realNameStatus(), valid.subjectCreatedAt());
        when(reader.fetch(any(), any(), anyInt())).thenReturn(List.of(maximumMask));
        assertThat(service.listCustomerBusinessNumbers(caller, request(1)).items().getFirst().numberMasked()).isEqualTo(maximumMask.numberMasked());
        for (RuntimeException exception : List.of(new org.jooq.exception.DataAccessException("PASSWORD_SQL_CANARY"), new IllegalStateException("CIPHERTEXT_CANARY"))) {
            doThrow(exception).when(reader).fetch(any(), any(), anyInt());
            failure(exception instanceof org.jooq.exception.DataAccessException ? CustomerContextException.Code.SERVICE_UNAVAILABLE : CustomerContextException.Code.INTERNAL,
                    () -> service.listCustomerBusinessNumbers(caller, request(1)));
        }
        when(context.fetchOne("select current_user")).thenReturn(null);
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.listCustomerBusinessNumbers(caller, request(1)));
        when(context.execute("SET LOCAL ROLE customer_iam_role")).thenThrow(new IllegalStateException("SQL_CANARY"));
        failure(CustomerContextException.Code.SERVICE_UNAVAILABLE, () -> service.listCustomerBusinessNumbers(caller, request(1)));
    }

    @Test
    void webLazyConfigurationAndSeparateReadOnlyRepeatableReadTemplateAreExact() throws Exception {
        var configuration = Class.forName("com.onecall.core.customer_iam.internal.persistence.CustomerPersistenceConfiguration");
        var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager();
        Object instance = configuration.getConstructor().newInstance();
        for (String name : List.of("customerBusinessNumberTransactionTemplate", "customerQueryTransactionTemplate")) {
            var method = configuration.getDeclaredMethod(name, org.springframework.jdbc.datasource.DataSourceTransactionManager.class); method.setAccessible(true);
            var template = (TransactionTemplate) method.invoke(instance, manager);
            assertThat(template.isReadOnly()).isTrue(); assertThat(template.getPropagationBehavior()).isEqualTo(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThat(template.getIsolationLevel()).isEqualTo(name.startsWith("customerBusiness") ? org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ : org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        }
        assertThat(configuration.getDeclaredMethods()).hasSize(15);
        for (var method : configuration.getDeclaredMethods()) {
            assertThat(method.isAnnotationPresent(org.springframework.context.annotation.Lazy.class)).isTrue();
            assertThat(method.isAnnotationPresent(org.springframework.context.annotation.Bean.class)).isTrue();
        }
        var dslMethod = configuration.getDeclaredMethod("customerQueryDsl", javax.sql.DataSource.class); dslMethod.setAccessible(true);
        var settings = ((DSLContext) dslMethod.invoke(instance, mock(javax.sql.DataSource.class))).settings();
        assertThat(settings.getStatementType()).isEqualTo(org.jooq.conf.StatementType.PREPARED_STATEMENT);
        assertThat(settings.isExecuteLogging()).isFalse();
        assertThat(settings.getParamType()).isEqualTo(org.jooq.conf.ParamType.INDEXED);
        var serviceMethod = configuration.getDeclaredMethod("customerBusinessNumberQueryService", TransactionTemplate.class, DSLContext.class,
                JooqCustomerBusinessNumberAccessRepository.class, CustomerBusinessNumberReader.class, CustomerActivationStatusReader.class);
        assertThat(serviceMethod.getParameters()[1].getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value()).isEqualTo("customerQueryDsl");
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withInitializer(application -> application.getEnvironment().setActiveProfiles("web"))
                .withUserConfiguration(configuration).run(application -> {
                    assertThat(application).hasNotFailed();
                    for (String name : List.of("customerQueryDataSource", "customerBusinessNumberQueryService", "customerBusinessNumberCursorCodec")) {
                        assertThat(application.containsBean(name)).isTrue();
                        assertThat(application.getBeanFactory().containsSingleton(name)).isFalse();
                    }
                });
    }
}
