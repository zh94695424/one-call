package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadPersistenceConfiguration;
import com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleConfiguration;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class OperationsCatalogVersionConfigurationTests {
    Object invoke(String name, Class<?> parameter, Object argument) throws Exception {
        Method method = OperationsSubjectReadPersistenceConfiguration.class.getDeclaredMethod(name, parameter); method.setAccessible(true);
        return method.invoke(new OperationsSubjectReadPersistenceConfiguration(), argument);
    }
    @Test void reusesExistingOperationsDatasource() throws Exception {
        DataSource source = mock(DataSource.class);
        var manager = (DataSourceTransactionManager) invoke("operationsSubjectReadCatalogSaleTransactionManager", DataSource.class, source);
        assertThat(manager.getDataSource()).isSameAs(source); assertThat(manager.isRollbackOnCommitFailure()).isTrue(); assertThat(manager.isEnforceReadOnly()).isFalse();
    }
    @Test void usesNewWritableRequiresNewReadCommittedTemplate() throws Exception {
        var manager = new DataSourceTransactionManager(mock(DataSource.class));
        var template = (TransactionTemplate) invoke("operationsSubjectReadCatalogSaleTransactionTemplate", DataSourceTransactionManager.class, manager);
        assertThat(template.getTransactionManager()).isSameAs(manager); assertThat(template.isReadOnly()).isFalse(); assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW); assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED); assertThat(template.getTimeout()).isEqualTo(5);
        var read = (TransactionTemplate) invoke("operationsSubjectReadTransactionTemplate", DataSourceTransactionManager.class, manager); assertThat(read.isReadOnly()).isTrue(); assertThat(read.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }
    @Test void reusesSamePhysicalDslBinding() throws Exception {
        for (Method method : OperationsSubjectReadPersistenceConfiguration.class.getDeclaredMethods()) for (Parameter parameter : method.getParameters()) {
            if (Set.of(DataSource.class, DataSourceTransactionManager.class, org.jooq.DSLContext.class).contains(parameter.getType())) assertThat(parameter.getAnnotation(Qualifier.class).value()).startsWith("operationsSubjectRead");
        }
        assertThat(Files.readString(Path.of("src/main/java/com/onecall/core/catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java"))).contains("@Qualifier(\"operationsSubjectReadDataSource\")", "@Qualifier(\"operationsSubjectReadDsl\")");
    }
    @Test void doesNotCreateSecondPoolOrCredentialFamily() throws Exception {
        for (String path : List.of("catalog_order/internal/operationscommand/CatalogSaleLifecycleConfiguration.java", "catalog_order/internal/operationscommand/JooqCatalogSaleLifecycleRepository.java", "operations_iam/internal/employeeauthority/DefaultEmployeeAuthorityReadPort.java")) assertThat(Files.readString(Path.of("src/main/java/com/onecall/core/" + path))).doesNotContain("new HikariDataSource", "operationsLifecycle", "datasource.password");
    }
    Object provider(DefaultListableBeanFactory beans) throws Throwable {
        var controller = new OperationsCatalogVersionController(beans.getBeanProvider(OperationsCatalogSaleCommand.class), new OperationsProblemWriter());
        Method method = OperationsCatalogVersionController.class.getDeclaredMethod("command"); method.setAccessible(true);
        try { return method.invoke(controller); } catch (InvocationTargetException failure) { throw failure.getCause(); }
    }
    @Test void resolvesExactlyOneCommandProvider() throws Throwable {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("persistence");
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("enabled", Map.of("onecall.operations-subject-read.enabled", "true")));
            var manager = new DataSourceTransactionManager(mock(DataSource.class));
            var template = (TransactionTemplate) invoke("operationsSubjectReadCatalogSaleTransactionTemplate", DataSourceTransactionManager.class, manager);
            context.registerBean("operationsSubjectReadCatalogSaleTransactionTemplate", TransactionTemplate.class, () -> template);
            context.registerBean(EmployeeAuthorityReadPort.class, () -> mock(EmployeeAuthorityReadPort.class));
            context.registerBean("operationsSubjectReadDataSource", DataSource.class, () -> mock(DataSource.class));
            context.registerBean("operationsSubjectReadDsl", org.jooq.DSLContext.class, () -> mock(org.jooq.DSLContext.class));
            context.register(Class.forName("com.onecall.core.catalog_order.internal.operationscommand.JooqCatalogSaleLifecycleRepository"));
            context.register(CatalogSaleLifecycleConfiguration.class); context.refresh();
            assertThat(context.getBeanNamesForType(OperationsCatalogSaleCommand.class)).containsExactly("operationsCatalogSaleCommand");
            assertThat(provider(context.getDefaultListableBeanFactory())).isSameAs(context.getBean(OperationsCatalogSaleCommand.class));
            context.registerBean("second", OperationsCatalogSaleCommand.class, () -> mock(OperationsCatalogSaleCommand.class));
            assertThatThrownBy(() -> provider(context.getDefaultListableBeanFactory())).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
        }
    }
    @Test void missingProviderFailsClosed() {
        assertThatThrownBy(() -> provider(new DefaultListableBeanFactory())).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean("unavailable", OperationsCatalogSaleCommand.class, () -> { throw new IllegalStateException("PRIVATE SQL password catalog_order_role"); }, definition -> definition.setLazyInit(true)); context.refresh();
            assertThatThrownBy(() -> provider(context.getDefaultListableBeanFactory())).isInstanceOfSatisfying(Failure.class, failure -> { assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE); assertThat(failure.toString()).doesNotContain("PRIVATE", "SQL", "password", "catalog_order_role"); assertThat(failure.getCause()).isNull(); });
        }
    }
    @Test void ambiguousProviderFailsClosed() { var beans = new DefaultListableBeanFactory(); beans.registerSingleton("one", mock(OperationsCatalogSaleCommand.class)); beans.registerSingleton("two", mock(OperationsCatalogSaleCommand.class)); assertThatThrownBy(() -> provider(beans)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE)); }
    @Test void defaultRuntimeHasNoWriteBeans() {
        for (String enabled : List.of("absent", "false", "true")) for (String profile : List.of("default", "web", "persistence")) try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(profile);
            if (!enabled.equals("absent")) context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("enabled", Map.of("onecall.operations-subject-read.enabled", enabled)));
            context.register(OperationsSubjectReadPersistenceConfiguration.class, CatalogSaleLifecycleConfiguration.class); context.refresh();
            boolean expected = enabled.equals("true") && profile.equals("persistence");
            for (String name : List.of("operationsSubjectReadTransactionManager", "operationsSubjectReadTransactionTemplate", "operationsSubjectReadCatalogSaleTransactionManager", "operationsSubjectReadCatalogSaleTransactionTemplate", "operationsCatalogSaleCommand")) {
                assertThat(context.containsBeanDefinition(name)).as("%s/%s/%s", enabled, profile, name).isEqualTo(expected);
                assertThat(context.getBeanFactory().containsSingleton(name)).isFalse();
            }
        }
    }
}
