package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.activation_consent.internal.command.*;
import com.onecall.core.entitlement_metering.internal.activation.DefaultActivationTrialService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerSubjectActivationConfigurationTests {
    static final Path ROOT = Path.of("src/main/java/com/onecall/core");
    static String source(String path) throws Exception { return Files.readString(ROOT.resolve(path)); }
    static Object invoke(String method, Class<?> parameter, Object value) throws Exception {
        var declared = ActivationCommandPersistenceConfiguration.class.getDeclaredMethod(method, parameter); declared.setAccessible(true); return declared.invoke(new ActivationCommandPersistenceConfiguration(), value);
    }
    @Test
    void disabledActivationCreatesNoPoolOrOwnerGraph() {
        new ApplicationContextRunner().withInitializer(context -> context.getEnvironment().setActiveProfiles("persistence"))
                .withUserConfiguration(ActivationCommandPersistenceConfiguration.class, DefaultActivationTrialService.class).run(context -> { assertThat(context).hasNotFailed(); assertThat(context).doesNotHaveBean("activationCommandDataSource"); assertThat(context).doesNotHaveBean("activationTrialService"); });
    }
    @Test
    void enabledActivationRequiresExplicitDedicatedCredentials() {
        for (String[] values : new String[][] {{null, "name", "password"}, {"url", "", "password"}, {"url", "name", " "}}) assertThatThrownBy(() -> new ActivationCommandPersistenceProperties(values[0], values[1], values[2])).hasMessage("Activation command credentials required");
        new ApplicationContextRunner().withInitializer(context -> context.getEnvironment().setActiveProfiles("persistence")).withUserConfiguration(ActivationCommandPersistenceConfiguration.class).withPropertyValues("onecall.activation-command.enabled=true").run(context -> assertThat(context).hasFailed());
    }
    @Test
    void activationUsesQualifiedWritableReadCommittedTransaction() throws Exception {
        var manager = (DataSourceTransactionManager) invoke("activationCommandTransactionManager", DataSource.class, mock(DataSource.class));
        var template = (TransactionTemplate) invoke("activationCommandTransactionTemplate", DataSourceTransactionManager.class, manager);
        assertThat(template.getTransactionManager()).isSameAs(manager); assertThat(template.isReadOnly()).isFalse(); assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED); assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW); assertThat(manager.isRollbackOnCommitFailure()).isTrue();
    }
    @Test
    void entitlementMandatoryTemplateUsesSameManagerAndDsl() throws Exception {
        var parameters = DefaultActivationTrialService.class.getConstructors()[0].getParameters(); assertThat(Arrays.stream(parameters).map(parameter -> parameter.getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value())).containsExactly("activationCommandTransactionManager", "activationCommandDsl");
        assertThat(source("entitlement_metering/internal/activation/DefaultActivationTrialService.java")).contains("PROPAGATION_MANDATORY", "hasResource(manager.getDataSource())").doesNotContain("new Hikari", "new DataSourceTransactionManager");
    }
    @Test
    void ambiguousOrMissingOwnerGraphFailsClosed() throws Exception {
        assertThat(source("activation_consent/internal/command/ActivationCommandPersistenceConfiguration.java")).contains("candidates.stream().count() != 1", "@Qualifier(\"activationTrialService\")").doesNotContain("@Primary", "@ConditionalOnMissingBean");
        var beans = new org.springframework.beans.factory.support.DefaultListableBeanFactory(); var composer = new com.onecall.core.customer_iam.internal.application.CustomerSubjectActivationCommandService(beans.getBeanProvider(com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.class));
        var caller = new com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller(java.util.UUID.randomUUID(), com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel.WEB, new com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace(com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.PERSONAL, null));
        assertThatThrownBy(() -> composer.activate(caller, null)).isInstanceOfSatisfying(com.onecall.core.activation_consent.customeractivation.SubjectActivationException.class, failure -> assertThat(failure.code()).isEqualTo(com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code.SERVICE_UNAVAILABLE));
    }
    @Test
    void propertiesAndInitializationFailuresRedactCredentials() throws Exception {
        assertThat(new ActivationCommandPersistenceProperties("PRIVATE_URL", "PRIVATE_USER", "PRIVATE_PASSWORD").toString()).isEqualTo("ActivationCommandPersistenceProperties[REDACTED]");
        assertThat(source("activation_consent/internal/command/ActivationCommandPersistenceConfiguration.java")).contains("throw new IllegalStateException(\"Activation command pool unavailable\")").doesNotContain("printStackTrace", "getMessage()");
    }
    @Test
    void webWithoutPersistenceRemainsAuthenticatedFailClosed() throws Exception {
        assertThat(source("customer_iam/internal/web/CustomerWebConfiguration.java")).contains("@org.springframework.context.annotation.Lazy", "customerSubjectActivationCommandService");
        assertThat(source("customer_iam/internal/application/CustomerSubjectActivationCommandService.java")).contains("catch (BeansException unavailable)", "Code.SERVICE_UNAVAILABLE");
        assertThat(source("customer_iam/internal/web/CustomerSubjectActivationsController.java").indexOf("Code.UNAUTHENTICATED")).isLessThan(source("customer_iam/internal/web/CustomerSubjectActivationsController.java").indexOf("commands.getObject()"));
    }
    @Test
    void utcInitializationAndPoolClosePreserveLifecycle() throws Exception {
        String configuration = source("activation_consent/internal/command/ActivationCommandPersistenceConfiguration.java"); assertThat(configuration).contains("@Bean(destroyMethod = \"close\")", "SET TIME ZONE 'UTC'", "withExecuteLogging(false)", "ParamType.INDEXED", "StatementType.PREPARED_STATEMENT", "TransactionAwareDataSourceProxy");
        String properties = Files.readString(Path.of("src/main/resources/application-persistence.yml")); assertThat(properties).contains("ONECALL_ACTIVATION_COMMAND_ENABLED:false", "ONECALL_ACTIVATION_COMMAND_DB_URL:", "ONECALL_ACTIVATION_COMMAND_DB_USERNAME:", "ONECALL_ACTIVATION_COMMAND_DB_PASSWORD:");
    }
}
