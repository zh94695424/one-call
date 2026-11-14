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
    private AnnotationConfigApplicationContext context(String profile, boolean enabled) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("read", Map.of(
                "onecall.operations-subject-read.enabled", Boolean.toString(enabled))));
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
    void missingCredentialsAndCursorKeysFailAtAffectedRequestNotBoot() {
        try (var context = context("persistence", true)) {
            assertThat(context.isActive()).isTrue();
            var properties = context.getBean(OperationsSubjectReadProperties.class);
            assertThatThrownBy(properties.datasource()::validate).isInstanceOf(Failure.class);
            assertThatThrownBy(() -> new OperationsSubjectCursorCodec(properties))
                    .isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
            assertThat(context.getBeanFactory().containsSingleton("operationsSubjectReadDataSource")).isFalse();
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
