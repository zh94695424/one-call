package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.onecall.core.activation_consent.internal.operations.*;
import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.*;
import com.onecall.core.operations_iam.internal.employeelock.DefaultEmployeeAdministratorLock;
import com.onecall.core.operations_iam.internal.web.OperationsWebConfiguration;
import com.onecall.core.operations_iam.internal.security.EmployeeSessionVerifier;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class OperationsLifecycleConfigurationTests {
    static String source(String file) throws Exception { return Files.readString(Path.of("src/main/java/com/onecall/core").resolve(file)); }
    static Object invoke(String name, Class<?> type, Object argument) throws Exception {
        var method = OperationsLifecyclePersistenceConfiguration.class.getDeclaredMethod(name, type); method.setAccessible(true); return method.invoke(new OperationsLifecyclePersistenceConfiguration(), argument);
    }
    static void unavailable(DefaultOperationsSubjectLifecycleService service) {
        assertThatThrownBy(() -> service.suspend(new Command(UUID.randomUUID(), "action", "reason", BigInteger.ONE), new Caller(UUID.randomUUID(), "onecall:employee-session:v1")))
                .isInstanceOfSatisfying(Failure.class, failure -> { assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE); assertThat(failure.getCause()).isNull(); });
    }

    @Test
    void defaultWebAndDisabledProfilesCreateNoLifecyclePool() throws Exception {
        for (String profile : List.of("default", "web", "persistence")) new ApplicationContextRunner().withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
            .withUserConfiguration(OperationsLifecyclePersistenceConfiguration.class, DefaultEmployeeAdministratorLock.class).run(context -> { assertThat(context).hasNotFailed(); for (String name : List.of("operationsLifecycleDataSource", "operationsLifecycleTransactionManager", "operationsLifecycleDsl", "operationsSubjectLifecycleService", "employeeAdministratorLock")) assertThat(context).doesNotHaveBean(name); });
        new ApplicationContextRunner().withInitializer(context -> context.getEnvironment().setActiveProfiles("web"))
            .withPropertyValues("onecall.operations-lifecycle.enabled=true")
            .withUserConfiguration(OperationsLifecyclePersistenceConfiguration.class, DefaultEmployeeAdministratorLock.class)
            .run(context -> { assertThat(context).hasNotFailed(); for (String name : List.of("operationsLifecycleDataSource", "operationsLifecycleTransactionManager", "operationsLifecycleDsl", "operationsSubjectLifecycleService", "employeeAdministratorLock")) assertThat(context).doesNotHaveBean(name); });
    }
    @Test
    void explicitEnablementRequiresDedicatedNonblankCredentials() throws Exception {
        for (String[] values : new String[][] {{null, "user", "password"}, {"url", " ", "password"}, {"url", "user", ""}}) assertThatThrownBy(() -> new OperationsLifecyclePersistenceProperties(values[0], values[1], values[2])).hasMessage("Operations lifecycle credentials required");
        new ApplicationContextRunner().withInitializer(context -> context.getEnvironment().setActiveProfiles("persistence")).withUserConfiguration(OperationsLifecyclePersistenceConfiguration.class).withPropertyValues("onecall.operations-lifecycle.enabled=true").run(context -> assertThat(context).hasFailed());
    }
    @Test
    void ownerBeansUseExactQualifiersAndOneDatasource() throws Exception {
        var source = mock(DataSource.class); var manager = (DataSourceTransactionManager) invoke("operationsLifecycleTransactionManager", DataSource.class, source); assertThat(manager.getDataSource()).isSameAs(source);
        String code = source("activation_consent/internal/operations/OperationsLifecyclePersistenceConfiguration.java"); assertThat(code).contains("@Qualifier(\"operationsLifecycleDataSource\")", "@Qualifier(\"operationsLifecycleDsl\")", "@Qualifier(\"operationsLifecycleRepository\")", "TransactionAwareDataSourceProxy(dataSource)", "SET TIME ZONE 'UTC'", "@Bean(destroyMethod = \"close\")");
    }
    @Test
    void writeTemplateUsesRequiresNewReadCommittedAndRollbackOnCommitFailure() throws Exception {
        var manager = (DataSourceTransactionManager) invoke("operationsLifecycleTransactionManager", DataSource.class, mock(DataSource.class));
        var template = (TransactionTemplate) invoke("operationsLifecycleTransactionTemplate", DataSourceTransactionManager.class, manager);
        assertThat(template.getTransactionManager()).isSameAs(manager); assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW); assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED); assertThat(template.isReadOnly()).isFalse(); assertThat(manager.isRollbackOnCommitFailure()).isTrue();
    }
    @Test
    void employeeCollaboratorIsOperationsOwnedAndUsesExactQualifiedManagerAndDsl() throws Exception {
        var parameters = DefaultEmployeeAdministratorLock.class.getConstructors()[0].getParameters();
        assertThat(Arrays.stream(parameters).map(parameter -> parameter.getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value())).containsExactly("operationsLifecycleTransactionManager", "operationsLifecycleDsl");
        assertThat(DefaultEmployeeAdministratorLock.class.getAnnotation(org.springframework.stereotype.Component.class).value()).isEqualTo("employeeAdministratorLock");
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("persistence"); context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("enabled", Map.of("onecall.operations-lifecycle.enabled", "true")));
            context.registerBean("operationsLifecycleTransactionManager", DataSourceTransactionManager.class, () -> new DataSourceTransactionManager(mock(DataSource.class)));
            context.registerBean("operationsLifecycleDsl", DSLContext.class, () -> mock(DSLContext.class)); context.register(DefaultEmployeeAdministratorLock.class); context.refresh();
            assertThat(context.getBeanNamesForType(EmployeeAdministratorLock.class)).containsExactly("employeeAdministratorLock"); assertThat(context.containsBean("defaultEmployeeAdministratorLock")).isFalse();
        }
        assertThat(source("activation_consent/internal/operations/OperationsLifecyclePersistenceConfiguration.java")).doesNotContain("internal.employeelock", "getIfAvailable", "@Primary", "candidates.stream()");
    }
    @Test
    void missingAndAmbiguousOwnerCollaboratorsFailClosed() throws Exception {
        for (int count : List.of(0, 2)) {
            try (var context = new AnnotationConfigApplicationContext()) {
                context.getEnvironment().setActiveProfiles("persistence");
                context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("enabled", Map.of(
                        "onecall.operations-lifecycle.enabled", "true",
                        "onecall.operations-lifecycle.datasource.url", "jdbc:postgresql://localhost/operations",
                        "onecall.operations-lifecycle.datasource.username", "operations",
                        "onecall.operations-lifecycle.datasource.password", "operations")));
                ((DefaultListableBeanFactory) context.getBeanFactory()).setAllowBeanDefinitionOverriding(true);
                context.register(OperationsLifecyclePersistenceConfiguration.class);
                var transaction = mock(TransactionTemplate.class); var repository = mock(OperationsLifecycleRepository.class); var dataSource = mock(DataSource.class);
                context.registerBean("operationsLifecycleDataSource", DataSource.class, () -> dataSource);
                context.registerBean("operationsLifecycleTransactionTemplate", TransactionTemplate.class, () -> transaction);
                context.registerBean("operationsLifecycleDsl", DSLContext.class, () -> mock(DSLContext.class));
                context.registerBean("operationsLifecycleRepository", OperationsLifecycleRepository.class, () -> repository);
                for (int index = 0; index < count; index++) context.registerBean("employee" + index, EmployeeAdministratorLock.class, () -> mock(EmployeeAdministratorLock.class));
                context.refresh();
                verify(transaction).afterPropertiesSet(); clearInvocations(transaction);
                unavailable(context.getBean("operationsSubjectLifecycleService", DefaultOperationsSubjectLifecycleService.class)); verifyNoInteractions(transaction, repository);
            }
        }
        var beans = new DefaultListableBeanFactory(); var definition = new RootBeanDefinition(EmployeeAdministratorLock.class, () -> { throw new IllegalStateException("SECRET"); }); definition.setLazyInit(true); beans.registerBeanDefinition("broken", definition);
        var transaction = mock(TransactionTemplate.class); var repository = mock(OperationsLifecycleRepository.class); var service = new DefaultOperationsSubjectLifecycleService(transaction, mock(DSLContext.class), repository, beans.getBeanProvider(EmployeeAdministratorLock.class)); unavailable(service); verifyNoInteractions(transaction, repository);
    }
    @Test
    void webBoundaryUsesIndependentVerifierAndExactFilterOrder() throws Exception {
        var config = source("operations_iam/internal/web/OperationsWebConfiguration.java"); assertThat(config).contains("@Profile(\"web\")", "DispatcherType.REQUEST", "Ordered.HIGHEST_PRECEDENCE)", "EmployeeSessionVerifier", "addUrlPatterns(\"/api/v1/operations/*\")").doesNotContain("CustomerSession", "DataSource");
        assertThat(source("customer_iam/internal/web/CustomerWebConfiguration.java")).contains("Ordered.HIGHEST_PRECEDENCE + 1");
    }
    @Test
    void configurationAndStartupFailuresAreRedacted() throws Exception {
        assertThat(new OperationsLifecyclePersistenceProperties("SECRET_URL", "SECRET_USER", "SECRET_PASSWORD").toString()).isEqualTo("OperationsLifecyclePersistenceProperties[REDACTED]");
        assertThat(source("activation_consent/internal/operations/OperationsLifecyclePersistenceConfiguration.java")).contains("throw new IllegalStateException(\"Operations lifecycle pool unavailable\")").doesNotContain("getMessage()", "printStackTrace");
        var transaction = mock(TransactionTemplate.class); var beans = new DefaultListableBeanFactory(); unavailable(new DefaultOperationsSubjectLifecycleService(transaction, mock(DSLContext.class), mock(OperationsLifecycleRepository.class), beans.getBeanProvider(EmployeeAdministratorLock.class))); verifyNoInteractions(transaction);
    }
}
