package com.onecall.core.operations_iam.internal.subjectread;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.onecall.core.operations_iam.internal.web.OperationsProblemWriter;
import com.onecall.core.operations_iam.internal.web.OperationsRequestPrincipal;
import com.onecall.core.operations_iam.internal.web.OperationsSubjectReadController;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.ActivationReader;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.EvidenceCounter;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.OrderCounter;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.SubjectReader;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.TrialReader;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Base64;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

class OperationsSubjectReadServiceTests {
    private static final String CANARY = "PRIVATE-database-password-and-subject-canary";
    private static final UUID EMPLOYEE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SUBJECT = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void trustedCallerAndCurrentEmployeePrecedeInputAndSubjectReads() throws Exception {
        for (String status : List.of("DISABLED", "BROKEN", "ACTIVE")) {
            var fixture = new ServiceFixture();
            when(fixture.employee.get("status", String.class)).thenReturn(status);
            if ("ACTIVE".equals(status)) when(fixture.employee.get("revision", Integer.class)).thenReturn(0);
            Code expected = "DISABLED".equals(status) ? Code.FORBIDDEN : Code.SERVICE_UNAVAILABLE;
            fixture.failure(expected, () -> fixture.service.list(fixture.caller, null));
            verifyNoInteractions(fixture.subjects);
            verify(fixture.connection).rollback();
        }
        var fixture = new ServiceFixture();
        fixture.failure(Code.UNAUTHENTICATED, () -> fixture.service.list(new Caller(EMPLOYEE, "customer"), null));
        verifyNoInteractions(fixture.subjects);
        verify(fixture.dsl, never()).fetchOne(anyString(), any(Object[].class));
        for (String corrupt : List.of("unknownRole", "nullRole", "nullStatus", "nullRevision", "negativeRevision", "wrongIdentity", "missingEmployee")) {
            var invalid = new ServiceFixture();
            switch (corrupt) {
                case "unknownRole" -> when(invalid.employee.get("role", String.class)).thenReturn("CUSTOMER");
                case "nullRole" -> when(invalid.employee.get("role", String.class)).thenReturn(null);
                case "nullStatus" -> when(invalid.employee.get("status", String.class)).thenReturn(null);
                case "nullRevision" -> when(invalid.employee.get("revision", Integer.class)).thenReturn(null);
                case "negativeRevision" -> when(invalid.employee.get("revision", Integer.class)).thenReturn(-1);
                case "wrongIdentity" -> when(invalid.employee.get("id", UUID.class)).thenReturn(SUBJECT);
                case "missingEmployee" -> when(invalid.dsl.fetchOne(anyString(), any(Object[].class))).thenReturn(null);
                default -> throw new AssertionError(corrupt);
            }
            invalid.failure("missingEmployee".equals(corrupt) ? Code.UNAUTHENTICATED : Code.SERVICE_UNAVAILABLE,
                    () -> invalid.service.list(invalid.caller, invalid.raw(Map.of())));
            verifyNoInteractions(invalid.subjects);
            verify(invalid.connection).rollback();
        }
    }

    @Test
    void oneReadOnlyRepeatableNewTransactionOwnsEveryProjection() throws Exception {
        var fixture = new ServiceFixture();
        when(fixture.subjects.detail(SUBJECT, 101)).thenAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isTrue();
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.getResource(fixture.source)).isNotNull();
            return Optional.of(fixture.detail(List.of(), List.of()));
        });
        fixture.service.detail(fixture.caller, fixture.rawDetail());
        verify(fixture.connection).setReadOnly(true);
        verify(fixture.connection).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        verify(fixture.connection).commit();
        verify(fixture.connection).close();
        assertThat(fixture.transaction.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(fixture.transaction.getTimeout()).isEqualTo(5);
    }

    @Test
    void zeroTwoAndUncreatableOwnerPortsAreUnavailable() throws Exception {
        for (int count : List.of(0, 2, 3)) {
            var fixture = new ServiceFixture();
            fixture.beans.destroySingleton("subjects");
            if (count == 2) {
                fixture.beans.registerSingleton("first", fixture.subjects);
                fixture.beans.registerSingleton("second", mock(SubjectReader.class));
            }
            if (count == 3) {
                var definition = new org.springframework.beans.factory.support.RootBeanDefinition(SubjectReader.class);
                definition.setInstanceSupplier(() -> { throw new BeanCreationException(CANARY); });
                fixture.beans.registerBeanDefinition("broken", definition);
            }
            fixture.failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller, fixture.raw(Map.of())));
            verifyNoInteractions(fixture.subjects);
            verify(fixture.connection).rollback();
        }
        for (String name : List.of("activations", "trials", "evidence", "orders")) {
            for (int count : List.of(0, 2, 3)) {
                var fixture = new ServiceFixture();
                Object original = fixture.beans.getBean(name);
                Class<?> port = original.getClass().getInterfaces()[0];
                if ("activations".equals(name)) port = ActivationReader.class;
                if ("trials".equals(name)) port = TrialReader.class;
                if ("evidence".equals(name)) port = EvidenceCounter.class;
                if ("orders".equals(name)) port = OrderCounter.class;
                fixture.beans.destroySingleton(name);
                if (count == 2) {
                    fixture.beans.registerSingleton("first", original);
                    fixture.beans.registerSingleton("second", mock(port));
                }
                if (count == 3) {
                    var definition = new org.springframework.beans.factory.support.RootBeanDefinition(port);
                    definition.setInstanceSupplier(() -> { throw new BeanCreationException(CANARY); });
                    fixture.beans.registerBeanDefinition("broken", definition);
                }
                fixture.failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.detail(fixture.caller, fixture.rawDetail()));
                verify(fixture.connection).rollback();
            }
        }
    }

    @Test
    void ownerHandoffsAssertAndRestoreLiteralRolesOnOneResource() throws Exception {
        var fixture = new ServiceFixture();
        fixture.service.detail(fixture.caller, fixture.rawDetail());
        var ordered = inOrder(fixture.context, fixture.dsl, fixture.subjects, fixture.activations,
                fixture.trials, fixture.evidence, fixture.orders);
        ordered.verify(fixture.context).enter();
        ordered.verify(fixture.dsl).fetchOne(anyString(), any(Object[].class));
        ordered.verify(fixture.dsl).fetchSingle("SELECT transaction_timestamp() AS now");
        ordered.verify(fixture.subjects).detail(SUBJECT, 101);
        ordered.verify(fixture.activations).find(Set.of(SUBJECT));
        ordered.verify(fixture.trials).find(SUBJECT, ServiceFixture.NOW);
        ordered.verify(fixture.evidence).count(SUBJECT);
        ordered.verify(fixture.orders).count(SUBJECT);
        ordered.verify(fixture.context).exit();
        assertThat(OperationsReadContext.Role.values()).containsExactly(OperationsReadContext.Role.SUBJECT,
                OperationsReadContext.Role.ACTIVATION, OperationsReadContext.Role.TRIAL,
                OperationsReadContext.Role.EVIDENCE, OperationsReadContext.Role.ORDER);
    }

    @Test
    void listUsesDescendingSeekAndLimitPlusOneWithoutOffset() throws Exception {
        var fixture = new ServiceFixture();
        var first = fixture.candidate(9, false);
        var second = fixture.candidate(8, false);
        when(fixture.subjects.fetchCandidates(any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(first, second)).thenReturn(List.of(second));
        var page = fixture.service.list(fixture.caller, fixture.raw(Map.of("limit", List.of("1"))));
        assertThat(page.items()).extracting(SubjectSummary::subjectId).containsExactly(first.subject().subjectId());
        assertThat(page.hasMore()).isTrue();
        var tail = fixture.service.list(fixture.caller, fixture.raw(Map.of("limit", List.of("1"), "cursor", List.of(page.nextCursor()))));
        assertThat(tail.items()).extracting(SubjectSummary::subjectId).containsExactly(second.subject().subjectId());
        assertThat(tail.hasMore()).isFalse();
        assertThat(tail.nextCursor()).isNull();
        verify(fixture.subjects).fetchCandidates(any(), org.mockito.ArgumentMatchers.eq(Optional.of(
                new Anchor(first.subject().createdAt(), first.subject().subjectId()))), org.mockito.ArgumentMatchers.eq(2));
    }

    @Test
    void activationFilterBatchesCandidatesWithoutSkippingMatchingRows() throws Exception {
        var fixture = new ServiceFixture();
        var first = new ArrayList<Candidate>();
        for (int index = 300; index > 199; index--) first.add(fixture.candidate(index, false));
        var matched = fixture.candidate(198, false);
        when(fixture.subjects.fetchCandidates(any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(first).thenReturn(List.of(matched));
        when(fixture.activations.find(any())).thenReturn(Map.of()).thenReturn(Map.of(matched.subject().subjectId(), new ActivationFacts("ACTIVE", 1)));
        var page = fixture.service.list(fixture.caller, fixture.raw(Map.of("activationStatus", List.of("ACTIVE"))));
        assertThat(page.items()).extracting(SubjectSummary::subjectId).containsExactly(matched.subject().subjectId());
        assertThat(page.hasMore()).isFalse();
        verify(fixture.subjects).fetchCandidates(any(), org.mockito.ArgumentMatchers.eq(Optional.of(
                new Anchor(ServiceFixture.NOW, first.getLast().subject().subjectId()))), org.mockito.ArgumentMatchers.eq(101));
    }

    @Test
    void candidateBudgetExhaustionNeverInventsTailOrContinuation() throws Exception {
        var fixture = new ServiceFixture();
        var batch = new java.util.concurrent.atomic.AtomicInteger();
        when(fixture.subjects.fetchCandidates(any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer(invocation -> {
            int offset = batch.getAndIncrement() * 101;
            var result = new ArrayList<Candidate>();
            for (int index = 20000 - offset; index > 20000 - offset - 101; index--) result.add(fixture.candidate(index, false));
            return result;
        });
        fixture.failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller,
                fixture.raw(Map.of("activationStatus", List.of("ACTIVE")))));
        assertThat(batch.get()).isEqualTo(100);
        verify(fixture.connection).rollback();
    }

    @Test
    void missingActivationAndAnyBindingRiskProjectExactly() throws Exception {
        for (boolean risk : List.of(false, true)) {
            var fixture = new ServiceFixture();
            when(fixture.subjects.fetchCandidates(any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                    .thenReturn(List.of(fixture.candidate(4, risk)));
            var item = fixture.service.list(fixture.caller, fixture.raw(Map.of())).items().getFirst();
            assertThat(item.activationStatus()).isEqualTo("INACTIVE");
            assertThat(item.riskState()).isEqualTo(risk ? "OPEN" : "CLEARED");
        }
    }

    @Test
    void latestClaimSelectionPrecedesClaimStatusFiltering() throws Exception {
        var fixture = new ServiceFixture();
        var facts = new SubjectFacts(SUBJECT, "IVR", null, "REJECTED", "UNVERIFIED", false, ServiceFixture.NOW);
        when(fixture.subjects.fetchCandidates(any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(new Candidate(facts)));
        fixture.failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.list(fixture.caller,
                fixture.raw(Map.of("claimStatus", List.of("ACCEPTED")))));
        verifyNoInteractions(fixture.activations);
    }

    @Test
    void displayBindingAndOptionalProjectionOrderingAreDeterministic() throws Exception {
        var fixture = new ServiceFixture();
        var binding = new BindingFacts(UUID.randomUUID(), "+86 138****0000", "MOBILE", "ACTIVE", ServiceFixture.NOW);
        var facts = new SubjectFacts(SUBJECT, "WEB", binding.numberMasked(), null, "VERIFIED", false, ServiceFixture.NOW);
        when(fixture.subjects.detail(SUBJECT, 101)).thenReturn(Optional.of(new DetailFacts(facts, List.of(binding), List.of())));
        var detail = fixture.service.detail(fixture.caller, fixture.rawDetail());
        assertThat(detail.maskedNumber()).isEqualTo(binding.numberMasked());
        assertThat(detail.bindings()).extracting(BindingSummary::bindingId).containsExactly(binding.bindingId());
        assertThat(detail.activation()).isNull();
        assertThat(detail.trial()).isNull();
    }

    @Test
    void detailHistoriesAreCompleteWithinCapAndOverflowFailsClosed() throws Exception {
        for (int size : List.of(0, 100, 101)) {
            var fixture = new ServiceFixture();
            var bindings = new ArrayList<BindingFacts>();
            for (int index = 1; index <= size; index++) bindings.add(new BindingFacts(new UUID(0, index), "***", "MOBILE", "RETIRED", null));
            var facts = new SubjectFacts(SUBJECT, "WEB", size == 0 ? null : "***", null, "UNVERIFIED", false, ServiceFixture.NOW);
            when(fixture.subjects.detail(SUBJECT, 101)).thenReturn(Optional.of(new DetailFacts(facts, bindings, List.of())));
            if (size > 100) fixture.failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.detail(fixture.caller, fixture.rawDetail()));
            else assertThat(fixture.service.detail(fixture.caller, fixture.rawDetail()).bindings()).hasSize(size);
        }
    }

    @Test
    void trialUsesOriginalGrantBalanceAndDatabaseDecisionTime() throws Exception {
        for (long reserved : List.of(0L, 25L, 100L)) {
            var fixture = new ServiceFixture();
            UUID grant = UUID.randomUUID();
            when(fixture.trials.find(SUBJECT, ServiceFixture.NOW)).thenReturn(Optional.of(new TrialFacts(UUID.randomUUID(), SUBJECT,
                    "FIRST-TRIAL", grant, ServiceFixture.NOW.plusSeconds(60), grant, SUBJECT, "TRIAL", "RECORDING_SECONDS",
                    100L, ServiceFixture.NOW.plusSeconds(60), UUID.randomUUID(), grant, 100L, reserved, 0L, "AVAILABLE")));
            var result = fixture.service.detail(fixture.caller, fixture.rawDetail()).trial();
            assertThat(result.status()).isEqualTo("GRANTED");
            assertThat(result.grantedSeconds()).isEqualTo(100);
            assertThat(result.remainingSeconds()).isEqualTo(100 - reserved);
            assertThat(result.expiresAt()).isEqualTo(ServiceFixture.NOW.plusSeconds(60));
        }
        for (String state : List.of("EXHAUSTED", "EXPIRED", "AT_EXPIRY", "TIME_EXPIRED", "MISMATCH")) {
            var fixture = new ServiceFixture();
            UUID grant = UUID.randomUUID();
            Instant expiry = "AT_EXPIRY".equals(state) ? ServiceFixture.NOW
                    : "TIME_EXPIRED".equals(state) ? ServiceFixture.NOW.minusSeconds(1) : ServiceFixture.NOW.plusSeconds(60);
            String balanceStatus = Set.of("EXHAUSTED", "EXPIRED").contains(state) ? state : "AVAILABLE";
            when(fixture.trials.find(SUBJECT, ServiceFixture.NOW)).thenReturn(Optional.of(new TrialFacts(UUID.randomUUID(), SUBJECT,
                    "FIRST-TRIAL", grant, expiry, grant, SUBJECT, "TRIAL", "RECORDING_SECONDS", 100L,
                    "MISMATCH".equals(state) ? expiry.plusSeconds(1) : expiry, UUID.randomUUID(), grant, 100L, 0L,
                    "EXHAUSTED".equals(state) ? 100L : 0L, balanceStatus)));
            if ("MISMATCH".equals(state)) fixture.failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.detail(fixture.caller, fixture.rawDetail()));
            else assertThat(fixture.service.detail(fixture.caller, fixture.rawDetail()).trial().status())
                    .isEqualTo("EXHAUSTED".equals(state) ? "EXHAUSTED" : "EXPIRED");
        }
    }

    @Test
    void countsRemainLongExactAndIndependentOfHistoryJoins() throws Exception {
        var fixture = new ServiceFixture();
        when(fixture.evidence.count(SUBJECT)).thenReturn(Long.MAX_VALUE);
        when(fixture.orders.count(SUBJECT)).thenReturn(2147483648L);
        var result = fixture.service.detail(fixture.caller, fixture.rawDetail());
        assertThat(result.evidenceCount()).isEqualTo(Long.MAX_VALUE);
        assertThat(result.orderCount()).isEqualTo(2147483648L);
        verify(fixture.evidence).count(SUBJECT);
        verify(fixture.orders).count(SUBJECT);
    }

    @Test
    void invalidOwnerFactsAndRoleRestorationFailuresAreUnavailable() throws Exception {
        var fixture = new ServiceFixture();
        when(fixture.activations.find(any())).thenReturn(Map.of(SUBJECT, new ActivationFacts("BROKEN", 0)));
        fixture.failure(Code.SERVICE_UNAVAILABLE, () -> fixture.service.detail(fixture.caller, fixture.rawDetail()));
        verify(fixture.connection).rollback();
        var restoration = new ServiceFixture();
        doThrow(new Failure(Code.SERVICE_UNAVAILABLE)).when(restoration.context).exit();
        restoration.failure(Code.SERVICE_UNAVAILABLE, () -> restoration.service.detail(restoration.caller, restoration.rawDetail()));
        verify(restoration.connection).rollback();
    }

    @Test
    void allSuccessDenialAndFailurePathsHaveZeroWriteAuditAndOutboxCalls() throws Exception {
        for (String status : List.of("ACTIVE", "DISABLED", "BROKEN")) {
            var fixture = new ServiceFixture();
            when(fixture.employee.get("status", String.class)).thenReturn(status);
            if ("ACTIVE".equals(status)) fixture.service.detail(fixture.caller, fixture.rawDetail());
            else fixture.failure("DISABLED".equals(status) ? Code.FORBIDDEN : Code.SERVICE_UNAVAILABLE,
                    () -> fixture.service.detail(fixture.caller, fixture.rawDetail()));
            verify(fixture.dsl, never()).execute(anyString());
            verify(fixture.dsl, never()).execute(anyString(), any(Object[].class));
            verify(fixture.context).exit();
        }
    }

    private static final class ServiceFixture {
        static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
        final Connection connection = mock(Connection.class);
        final DataSource source = mock(DataSource.class);
        final DSLContext dsl = mock(DSLContext.class);
        final org.jooq.Record employee = mock(org.jooq.Record.class);
        final OperationsReadContext context = mock(OperationsReadContext.class);
        final SubjectReader subjects = mock(SubjectReader.class);
        final ActivationReader activations = mock(ActivationReader.class);
        final TrialReader trials = mock(TrialReader.class);
        final EvidenceCounter evidence = mock(EvidenceCounter.class);
        final OrderCounter orders = mock(OrderCounter.class);
        final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        final Caller caller = new Caller(EMPLOYEE, "onecall:employee-session:v1");
        final TransactionTemplate transaction;
        final DefaultOperationsSubjectReadService service;

        ServiceFixture() throws Exception {
            when(connection.getAutoCommit()).thenReturn(true);
            when(source.getConnection()).thenReturn(connection);
            transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
            transaction.setReadOnly(true);
            transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.setTimeout(5);
            when(employee.get("id", UUID.class)).thenReturn(EMPLOYEE);
            when(employee.get("status", String.class)).thenReturn("ACTIVE");
            when(employee.get("role", String.class)).thenReturn("OPS");
            when(employee.get("revision", Integer.class)).thenReturn(1);
            when(dsl.fetchOne(anyString(), any(Object[].class))).thenReturn(employee);
            var time = mock(org.jooq.Record.class);
            when(time.get("now", OffsetDateTime.class)).thenReturn(NOW.atOffset(ZoneOffset.UTC));
            when(dsl.fetchSingle("SELECT transaction_timestamp() AS now")).thenReturn(time);
            beans.registerSingleton("subjects", subjects);
            beans.registerSingleton("activations", activations);
            beans.registerSingleton("trials", trials);
            beans.registerSingleton("evidence", evidence);
            beans.registerSingleton("orders", orders);
            when(subjects.fetchCandidates(any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
            when(subjects.detail(SUBJECT, 101)).thenReturn(Optional.of(detail(List.of(), List.of())));
            when(activations.find(any())).thenReturn(Map.of());
            when(trials.find(any(), any())).thenReturn(Optional.empty());
            var key = new OperationsSubjectReadProperties.KeyEntry("unit-key", Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]));
            service = new DefaultOperationsSubjectReadService(transaction, dsl, context,
                    new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor("unit-key", List.of(key))),
                    beans.getBeanProvider(SubjectReader.class), beans.getBeanProvider(ActivationReader.class),
                    beans.getBeanProvider(TrialReader.class), beans.getBeanProvider(EvidenceCounter.class), beans.getBeanProvider(OrderCounter.class));
        }

        Candidate candidate(long identifier, boolean risk) {
            return new Candidate(new SubjectFacts(new UUID(0, identifier), "WEB", null, null, "UNVERIFIED", risk, NOW));
        }

        DetailFacts detail(List<BindingFacts> bindings, List<ClaimFacts> claims) {
            return new DetailFacts(new SubjectFacts(SUBJECT, "WEB", null, null, "UNVERIFIED", false, NOW), bindings, claims);
        }

        RawListRequest raw(Map<String, List<String>> query) { return new RawListRequest(query, List.of()); }
        RawDetailRequest rawDetail() { return new RawDetailRequest(SUBJECT.toString(), Map.of(), List.of()); }

        void failure(Code code, Runnable action) {
            assertThatThrownBy(action::run).isInstanceOfSatisfying(Failure.class,
                    failure -> assertThat(failure.code()).isEqualTo(code)).hasMessage("Operations read failed");
        }
    }

    @Test
    void sqlBeanTransactionAndUnknownFailuresMapWithoutLeakingCauses() throws Exception {
        var failures = List.of(
                new org.jooq.exception.DataAccessException(CANARY, new SQLException(CANARY, "42501")),
                new org.springframework.dao.DataAccessResourceFailureException(CANARY),
                new CannotCreateTransactionException(CANARY),
                new BeanCreationException("privateReadDependency", CANARY),
                new IllegalStateException(CANARY));
        for (RuntimeException failure : failures) {
            for (String path : List.of("/api/v1/operations/subjects", "/api/v1/operations/subjects/" + SUBJECT)) {
                var connection = mock(Connection.class);
                when(connection.getAutoCommit()).thenReturn(true);
                var source = mock(DataSource.class);
                when(source.getConnection()).thenReturn(connection);
                var manager = new DataSourceTransactionManager(source);
                manager.setRollbackOnCommitFailure(true);
                var transaction = new TransactionTemplate(manager);
                transaction.setReadOnly(true);
                transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
                transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                transaction.setTimeout(5);
                var dsl = mock(DSLContext.class);
                when(dsl.fetchOne(anyString(), any(Object[].class))).thenThrow(failure);
                var context = mock(OperationsReadContext.class);
                var beans = new DefaultListableBeanFactory();
                var service = new DefaultOperationsSubjectReadService(transaction, dsl, context,
                        new OperationsSubjectReadProperties(true, null, null),
                        beans.getBeanProvider(SubjectReader.class), beans.getBeanProvider(ActivationReader.class),
                        beans.getBeanProvider(TrialReader.class), beans.getBeanProvider(EvidenceCounter.class),
                        beans.getBeanProvider(OrderCounter.class));
                beans.registerSingleton("operationsSubjectReadService", service);
                var mvc = MockMvcBuilders.standaloneSetup(new OperationsSubjectReadController(
                        beans.getBeanProvider(OperationsSubjectReadService.class), new OperationsProblemWriter())).build();
                var principal = new OperationsRequestPrincipal(new Caller(EMPLOYEE, "onecall:employee-session:v1"));
                var response = mvc.perform(get(path).principal(principal)).andReturn().getResponse();
                boolean unknown = failure instanceof IllegalStateException;
                assertThat(response.getStatus()).as("%s on %s", failure.getClass().getSimpleName(), path)
                        .isEqualTo(unknown ? 500 : 503);
                assertThat(response.getContentType()).startsWith("application/problem+json");
                var problem = JsonMapper.builder().build().readTree(response.getContentAsByteArray());
                assertThat(problem.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
                assertThat(problem.path("code").asString()).isEqualTo(unknown ? "INTERNAL" : "SERVICE_UNAVAILABLE");
                assertThat(problem.path("status").asInt()).isEqualTo(unknown ? 500 : 503);
                UUID.fromString(problem.path("traceId").asString());
                assertThat(response.getContentAsString()).doesNotContain(CANARY, "privateReadDependency", "42501", "SQLException");
                assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
                assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
                assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
                assertThat(response.getHeaders("Set-Cookie")).isEmpty();
                verify(context).enter();
                verify(context).exit();
                verify(connection).rollback();
                verify(connection).close();
            }
        }
    }
}
