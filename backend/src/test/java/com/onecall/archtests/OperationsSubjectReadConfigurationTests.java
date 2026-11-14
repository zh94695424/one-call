package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.operations_iam.internal.subjectread.*;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.subject_number.internal.operationsread.JooqOperationsSubjectReader;
import com.onecall.core.activation_consent.internal.operationsread.JooqOperationsActivationReader;
import com.onecall.core.entitlement_metering.internal.operationsread.JooqOperationsTrialReader;
import com.onecall.core.evidence.internal.operationsread.JooqOperationsEvidenceCounter;
import com.onecall.core.catalog_order.internal.operationsread.JooqOperationsOrderCounter;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class OperationsSubjectReadConfigurationTests {
    @SafeVarargs
    private AnnotationConfigApplicationContext context(String profile, boolean enabled, Map<String, Object>... cursorProperties) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("read", Map.of(
                "onecall.operations-subject-read.enabled", Boolean.toString(enabled))));
        for (int index = 0; index < cursorProperties.length; index++) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("cursor-shape-" + index, cursorProperties[index]));
        }
        context.register(OperationsSubjectReadPersistenceConfiguration.class);
        context.refresh();
        return context;
    }

    @Test
    void defaultWebAndDisabledProfilesCreateNoReadPool() {
        for (String profile : List.of("default", "web", "persistence")) {
            for (boolean enabled : List.of(false, true)) {
                if (enabled && "persistence".equals(profile)) continue;
                try (var context = context(profile, enabled)) {
                    assertThat(context.getBeanNamesForType(HikariDataSource.class)).isEmpty();
                    assertThat(context.getBeanNamesForType(OperationsSubjectReadService.class)).isEmpty();
                }
            }
        }
    }

    @Test
    void persistenceEnabledGraphIsLazyAndUsesDedicatedCredentials() {
        try (var context = context("persistence", true)) {
            for (String name : List.of("operationsSubjectReadDataSource", "operationsSubjectReadDsl", "operationsSubjectReadContext", "operationsSubjectReadService")) {
                assertThat(context.getBeanFactory().getBeanDefinition(name).isLazyInit()).as(name).isTrue();
                assertThat(context.getBeanFactory().containsSingleton(name)).isFalse();
            }
            var properties = context.getBean(OperationsSubjectReadProperties.class);
            assertThat(properties.datasource().url()).isNull();
            assertThat(properties.datasource().username()).isNull();
            assertThat(properties.datasource().password()).isNull();
        }
    }

    @Test
    void everyReadBeanUsesExactQualifiedDatasourceDslAndContext() {
        for (var method : OperationsSubjectReadPersistenceConfiguration.class.getDeclaredMethods()) {
            for (var parameter : method.getParameters()) {
                if (List.of(DataSource.class, DSLContext.class, OperationsReadContext.class, TransactionTemplate.class,
                        DataSourceTransactionManager.class).contains(parameter.getType())) {
                    assertThat(parameter.getAnnotation(Qualifier.class)).as(method.getName()).isNotNull();
                    assertThat(parameter.getAnnotation(Qualifier.class).value()).startsWith("operationsSubjectRead");
                }
            }
        }
        for (Class<?> adapter : adapters()) {
            assertThat(Arrays.stream(adapter.getConstructors()[0].getParameters())
                    .map(parameter -> parameter.getAnnotation(Qualifier.class).value()))
                    .containsExactly("operationsSubjectReadDsl", "operationsSubjectReadContext");
        }
    }

    @Test
    void templateIsReadOnlyRepeatableRequiresNewWithBoundedTimeouts() {
        try (var context = context("persistence", true)) {
            var manager = new DataSourceTransactionManager(mock(DataSource.class));
            context.getBeanFactory().registerSingleton("operationsSubjectReadTransactionManager", manager);
            var template = context.getBean("operationsSubjectReadTransactionTemplate", TransactionTemplate.class);
            assertThat(template.isReadOnly()).isTrue();
            assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThat(template.getTimeout()).isEqualTo(5);
        }
    }

    @Test
    void ownerAdaptersAreConditionalAndImplementOnlyInboundReadPorts() {
        var ports = List.of(SubjectReader.class, ActivationReader.class, TrialReader.class, EvidenceCounter.class, OrderCounter.class);
        for (int index = 0; index < adapters().size(); index++) {
            var adapter = adapters().get(index);
            assertThat(adapter.getInterfaces()).containsExactly(ports.get(index));
            assertThat(adapter.getAnnotation(Lazy.class)).isNotNull();
            var conditional = adapter.getAnnotation(ConditionalOnProperty.class);
            assertThat(conditional.prefix()).isEqualTo("onecall.operations-subject-read");
            assertThat(conditional.name()).containsExactly("enabled");
            assertThat(conditional.havingValue()).isEqualTo("true");
        }
    }

    @Test
    void missingCredentialsAndCursorKeysFailAtAffectedRequestNotBoot() throws Exception {
        try (var context = context("persistence", true)) {
            assertThat(context.isActive()).isTrue();
            var properties = context.getBean(OperationsSubjectReadProperties.class);
            assertThatThrownBy(properties.datasource()::validate).isInstanceOf(Failure.class);
            assertThatThrownBy(() -> new OperationsSubjectCursorCodec(properties))
                    .isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
            assertThat(context.getBeanFactory().containsSingleton("operationsSubjectReadDataSource")).isFalse();
        }
        String prefix = "onecall.operations-subject-read.cursor.";
        String material = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
        byte[] rotated = new byte[32];
        Arrays.fill(rotated, (byte) 71);
        String secondMaterial = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rotated);
        var shapes = List.<Map<String, Object>>of(
                Map.of(prefix + "active-key-id", "first", prefix + "keys[1].id", "first", prefix + "keys[1].material", material),
                Map.of(prefix + "active-key-id", "first", prefix + "keys[0].id", "first", prefix + "keys[0].material", material,
                        prefix + "keys[3].id", "second", prefix + "keys[3].material", secondMaterial),
                Map.of(prefix + "active-key-id", "first", prefix + "keys[0].id", "first"),
                Map.of(prefix + "active-key-id", "first", prefix + "keys[0].id", "first", prefix + "keys[0].material", material,
                        prefix + "keys[0].comment", "unexpected"),
                Map.of(prefix + "active-key-id", "first", prefix + "keys[0].id", "first", prefix + "keys[0].material", material),
                Map.of(prefix + "active-key-id", "second", prefix + "keys[0].id", "first", prefix + "keys[0].material", material,
                        prefix + "keys[1].id", "second", prefix + "keys[1].material", secondMaterial));
        for (int index = 0; index < shapes.size(); index++) {
            var shape = shapes.get(index);
            for (String profile : List.of("default", "web")) {
                try (var context = context(profile, true, shape)) {
                    assertThat(context.isActive()).isTrue();
                    assertThat(context.getBeanNamesForType(HikariDataSource.class)).isEmpty();
                    assertThat(context.getBeanNamesForType(OperationsSubjectReadService.class)).isEmpty();
                }
            }
            try (var context = context("persistence", true, shape)) {
                assertThat(context.isActive()).isTrue();
                var properties = context.getBean(OperationsSubjectReadProperties.class);
                assertThat(context.getBeanFactory().containsSingleton("operationsSubjectReadDataSource")).isFalse();
                UUID employeeId = UUID.randomUUID();
                UUID subjectId = UUID.randomUUID();
                var now = java.time.OffsetDateTime.parse("2026-11-14T00:00:00Z");
                var datasource = mock(DataSource.class);
                var connection = mock(java.sql.Connection.class);
                when(datasource.getConnection()).thenReturn(connection);
                when(connection.getAutoCommit()).thenReturn(true);
                when(connection.createStatement()).thenReturn(mock(java.sql.Statement.class));
                var dsl = mock(DSLContext.class);
                var employee = mock(org.jooq.Record.class);
                when(employee.get("id", UUID.class)).thenReturn(employeeId);
                when(employee.get("status", String.class)).thenReturn("ACTIVE");
                when(employee.get("role", String.class)).thenReturn("OPS");
                when(employee.get("revision", Integer.class)).thenReturn(1);
                when(dsl.fetchOne(anyString(), any(Object[].class))).thenReturn(employee);
                var time = mock(org.jooq.Record.class);
                when(time.get("now", java.time.OffsetDateTime.class)).thenReturn(now);
                when(dsl.fetchSingle("SELECT transaction_timestamp() AS now")).thenReturn(time);
                var subjects = mock(SubjectReader.class);
                when(subjects.detail(subjectId, 101)).thenReturn(Optional.of(new DetailFacts(
                        new SubjectFacts(subjectId, "WEB", null, null, "UNVERIFIED", false, now.toInstant()), List.of(), List.of())));
                when(subjects.fetchCandidates(any(), any(), anyInt())).thenReturn(List.of());
                var activations = mock(ActivationReader.class);
                when(activations.find(any())).thenReturn(Map.of());
                var trials = mock(TrialReader.class);
                when(trials.find(any(), any())).thenReturn(Optional.empty());
                var beans = context.getBeanFactory();
                beans.registerSingleton("operationsSubjectReadDataSource", datasource);
                beans.registerSingleton("operationsSubjectReadDsl", dsl);
                beans.registerSingleton("operationsSubjectReadContext", mock(OperationsReadContext.class));
                beans.registerSingleton("subjects", subjects);
                beans.registerSingleton("activations", activations);
                beans.registerSingleton("trials", trials);
                beans.registerSingleton("evidence", mock(EvidenceCounter.class));
                beans.registerSingleton("orders", mock(OrderCounter.class));
                var service = context.getBean(OperationsSubjectReadService.class);
                var caller = new Caller(employeeId, "onecall:employee-session:v1");
                assertThat(service.detail(caller, new RawDetailRequest(subjectId.toString(), Map.of(), List.of())).subjectId()).isEqualTo(subjectId);
                clearInvocations(subjects);
                assertThatThrownBy(() -> service.list(caller, new RawListRequest(Map.of("unexpected", List.of("value")), List.of())))
                        .isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED));
                var request = new RawListRequest(Map.of(), List.of());
                if (index < 4) {
                    assertThatThrownBy(() -> new OperationsSubjectCursorCodec(properties)).isInstanceOfSatisfying(Failure.class,
                            failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE)).hasMessage("Operations read failed");
                    assertThatThrownBy(() -> service.list(caller, request)).isInstanceOfSatisfying(Failure.class,
                            failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE)).hasMessage("Operations read failed");
                    verifyNoInteractions(subjects);
                } else {
                    assertThat(properties.cursor().validatedKeys()).hasSize(index - 3);
                    var codec = new OperationsSubjectCursorCodec(properties);
                    var anchor = new Anchor(now.toInstant(), subjectId);
                    byte[] digest = OperationsSubjectReadRequest.list(request).digest();
                    assertThat(codec.decode(codec.encode(anchor, digest), digest)).isEqualTo(anchor);
                    assertThat(service.list(caller, request).items()).isEmpty();
                    verify(subjects).fetchCandidates(any(), any(), anyInt());
                }
                when(employee.get("status", String.class)).thenReturn("DISABLED");
                assertThatThrownBy(() -> service.list(caller, request)).isInstanceOfSatisfying(Failure.class,
                        failure -> assertThat(failure.code()).isEqualTo(Code.FORBIDDEN));
            }
        }
    }

    @Test
    void detailDoesNotRequireKeysOrAnyCommandResource() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/operations_iam/internal/subjectread/DefaultOperationsSubjectReadService.java"));
        String detail = source.split("@Override public OperationsSubjectDetail detail", 2)[1].split("private <Result>", 2)[0];
        assertThat(detail).contains("OperationsSubjectReadRequest.detail", "subjectReader.detail(subjectId, 101)")
                .doesNotContain("CursorCodec", "properties", "Command", "Outbox", "audit", "Idempotency");
        assertThat(Arrays.stream(OperationsSubjectReadService.class.getDeclaredMethods())
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers())).map(java.lang.reflect.Method::getName))
                .containsExactlyInAnyOrder("list", "detail");
    }

    @Test
    void startupPropertiesProvidersAndPoolFailuresAreRedacted() {
        var credentials = new OperationsSubjectReadProperties.Datasource("PRIVATE_URL", "PRIVATE_USER", "PRIVATE_PASSWORD");
        var properties = new OperationsSubjectReadProperties(true, credentials,
                new OperationsSubjectReadProperties.Cursor("PRIVATE_ID", List.of(new OperationsSubjectReadProperties.KeyEntry("PRIVATE_ID", "PRIVATE_KEY"))));
        assertThat(properties.toString()).isEqualTo("OperationsSubjectReadProperties[REDACTED]");
        assertThat(credentials.toString()).isEqualTo("Datasource[REDACTED]");
        assertThat(properties.cursor().toString()).isEqualTo("Cursor[REDACTED]");
        assertThat(properties.cursor().keys().getFirst().toString()).isEqualTo("KeyEntry[REDACTED]");
        assertThatThrownBy(properties.cursor()::validatedKeys).isInstanceOf(Failure.class).hasMessage("Operations read failed");
    }

    private static List<Class<?>> adapters() {
        return List.of(JooqOperationsSubjectReader.class, JooqOperationsActivationReader.class, JooqOperationsTrialReader.class,
                JooqOperationsEvidenceCounter.class, JooqOperationsOrderCounter.class);
    }
}
