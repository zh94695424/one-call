package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.customer_iam.internal.application.CustomerSubjectClaimCommandService;
import com.onecall.core.customer_iam.internal.web.CustomerSubjectClaimsController;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService;
import com.onecall.core.subject_number.internal.claim.*;
import com.onecall.archtests.subjectnumber.ControlledSubjectClaimProviders;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class SubjectClaimPersistenceConfigurationTests {
    private final SubjectClaimPersistenceConfiguration configuration = new SubjectClaimPersistenceConfiguration();
    private Object bean(String name, Object... arguments) throws Exception {
        Method method = Arrays.stream(configuration.getClass().getDeclaredMethods()).filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
        method.setAccessible(true); return method.invoke(configuration, arguments);
    }
    @Test
    void defaultWebAndDisabledProfilesCreateNoCommandPool() {
        for (String profile : List.of("default", "web", "persistence")) {
            new ApplicationContextRunner().withUserConfiguration(SubjectClaimPersistenceConfiguration.class)
                    .withPropertyValues("spring.profiles.active=" + profile, "onecall.subject-claim-command.enabled=false")
                    .run(context -> { assertThat(context).hasNotFailed(); assertThat(context).doesNotHaveBean("subjectClaimCommandDataSource"); assertThat(context).doesNotHaveBean(CustomerSubjectClaimService.class); });
        }
        new ApplicationContextRunner().withUserConfiguration(SubjectClaimPersistenceConfiguration.class)
                .withPropertyValues("onecall.subject-claim-command.enabled=true").run(context -> assertThat(context).doesNotHaveBean("subjectClaimCommandDataSource"));
    }
    @Test
    void explicitEnablementRequiresAllDedicatedCredentials() {
        for (String missing : List.of("url", "username", "password")) for (String blank : List.of("", " ")) {
            var runner = new ApplicationContextRunner().withUserConfiguration(SubjectClaimPersistenceConfiguration.class)
                    .withPropertyValues("spring.profiles.active=persistence", "onecall.subject-claim-command.enabled=true",
                            "onecall.persistence.url=jdbc:other", "onecall.persistence.username=OTHER", "onecall.persistence.password=OTHER",
                            "onecall.customer.persistence.url=jdbc:other", "onecall.customer.persistence.username=OTHER", "onecall.customer.persistence.password=OTHER");
            for (String field : List.of("url", "username", "password")) runner = runner.withPropertyValues("onecall.subject-claim-command.datasource." + field + "=" + (field.equals(missing) ? blank : "SYNTHETIC"));
            runner.run(context -> {
                assertThat(context).hasFailed(); Throwable root = context.getStartupFailure(); while (root.getCause() != null) root = root.getCause();
                assertThat(root.getMessage().equals("Subject claim command credentials required")).isTrue();
            });
        }
    }
    @Test
    void commandBeansUseExactQualifiersAndOneDedicatedDatasource() throws Exception {
        DataSource source = mock(DataSource.class);
        var manager = (DataSourceTransactionManager) bean("subjectClaimCommandTransactionManager", source);
        assertThat(manager.getDataSource()).isSameAs(source);
        DSLContext dsl = (DSLContext) bean("subjectClaimCommandDsl", source);
        var connection = mock(java.sql.Connection.class); when(source.getConnection()).thenReturn(connection);
        var acquired = dsl.configuration().connectionProvider().acquire(); assertThat(acquired).isNotNull(); acquired.getAutoCommit();
        dsl.configuration().connectionProvider().release(acquired); verify(source).getConnection();
        assertThat(bean("subjectClaimRepository")).isInstanceOf(JooqSubjectClaimRepository.class);
        var owner = Arrays.stream(configuration.getClass().getDeclaredMethods()).filter(method -> method.getName().equals("customerSubjectClaimService")).findFirst().orElseThrow();
        assertThat(Arrays.stream(owner.getParameters()).limit(5).map(parameter -> parameter.getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value()))
                .containsExactly("subjectClaimCommandTransactionTemplate", "subjectClaimCommandDsl", "subjectClaimRepository", "subjectClaimProofVerifier", "subjectClaimReferenceCipher");
    }
    @Test
    void writeTemplateUsesReadCommittedRequiresNewAndRollbackCleanup() throws Exception {
        var manager = (DataSourceTransactionManager) bean("subjectClaimCommandTransactionManager", mock(DataSource.class));
        var template = (TransactionTemplate) bean("subjectClaimCommandTransactionTemplate", manager);
        assertThat(template.getTransactionManager()).isSameAs(manager); assertThat(manager.isRollbackOnCommitFailure()).isTrue();
        assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW); assertThat(template.isReadOnly()).isFalse();
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/subject_number/internal/claim/SubjectClaimPersistenceConfiguration.java"));
        assertThat(source).contains("new TransactionAwareDataSourceProxy(dataSource)", "withExecuteLogging(false)", "ParamType.INDEXED", "StatementType.PREPARED_STATEMENT");
    }
    @Test
    void defaultProvidersAreUnavailableAndControlledReplacementsAreUnique() throws Exception {
        var runner = new ApplicationContextRunner().withAllowBeanDefinitionOverriding(false)
                .withPropertyValues("spring.profiles.active=web,persistence", "onecall.subject-claim-command.enabled=true",
                        "onecall.subject-claim-command.datasource.url=jdbc:synthetic", "onecall.subject-claim-command.datasource.username=SYNTHETIC",
                        "onecall.subject-claim-command.datasource.password=SYNTHETIC")
                .withInitializer(context -> context.addBeanFactoryPostProcessor(factory ->
                        ((AbstractBeanDefinition) factory.getBeanDefinition("subjectClaimCommandDataSource"))
                                .setInstanceSupplier(() -> mock(HikariDataSource.class))));
        runner.withUserConfiguration(SubjectClaimPersistenceConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(SubjectClaimProofVerifier.class).hasSingleBean(SubjectClaimReferenceCipher.class)
                    .hasSingleBean(CustomerSubjectClaimService.class);
            var proofs = context.getBean("subjectClaimProofVerifier", SubjectClaimProofVerifier.class);
            var cipher = context.getBean("subjectClaimReferenceCipher", SubjectClaimReferenceCipher.class);
            assertThat(proofs.verify(null)).isInstanceOf(SubjectClaimProviderResult.Unavailable.class);
            assertThat(cipher.seal(new byte[0])).isInstanceOf(SubjectClaimProviderResult.Unavailable.class);
            assertThat(cipher.open(null)).isInstanceOf(SubjectClaimProviderResult.Unavailable.class);
            var beans = new DefaultListableBeanFactory(); beans.registerSingleton("proof", proofs); beans.registerSingleton("cipher", cipher);
            var manager = new DataSourceTransactionManager(mock(DataSource.class));
            Object owner = bean("customerSubjectClaimService", new TransactionTemplate(manager), mock(DSLContext.class), mock(SubjectClaimRepository.class), proofs, cipher,
                    beans.getBeanProvider(SubjectClaimProofVerifier.class), beans.getBeanProvider(SubjectClaimReferenceCipher.class));
            assertThat(owner).isInstanceOf(DefaultCustomerSubjectClaimService.class);
            beans.registerSingleton("second", (SubjectClaimProofVerifier) request -> new SubjectClaimProviderResult.Unavailable<>());
            assertThatThrownBy(() -> bean("customerSubjectClaimService", new TransactionTemplate(manager), mock(DSLContext.class), mock(SubjectClaimRepository.class), proofs, cipher,
                    beans.getBeanProvider(SubjectClaimProofVerifier.class), beans.getBeanProvider(SubjectClaimReferenceCipher.class))).hasCauseInstanceOf(com.onecall.core.subject_number.customerclaim.SubjectClaimException.class);
            beans.destroySingleton("second"); beans.registerSingleton("secondCipher", mock(SubjectClaimReferenceCipher.class));
            assertThatThrownBy(() -> bean("customerSubjectClaimService", new TransactionTemplate(manager), mock(DSLContext.class), mock(SubjectClaimRepository.class), proofs, cipher,
                    beans.getBeanProvider(SubjectClaimProofVerifier.class), beans.getBeanProvider(SubjectClaimReferenceCipher.class))).hasCauseInstanceOf(com.onecall.core.subject_number.customerclaim.SubjectClaimException.class);
        });
        Class<?> controlled = Class.forName("com.onecall.archtests.container.CustomerSubjectClaimFixture$ControlledBeans");
        for (Class<?>[] sources : List.of(new Class<?>[] {SubjectClaimPersistenceConfiguration.class, controlled},
                new Class<?>[] {controlled, SubjectClaimPersistenceConfiguration.class})) {
            runner.withUserConfiguration(sources).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(SubjectClaimProofVerifier.class).hasSingleBean(SubjectClaimReferenceCipher.class)
                        .hasSingleBean(CustomerSubjectClaimService.class);
                assertThat(context.getBean("subjectClaimProofVerifier")).isInstanceOf(ControlledSubjectClaimProviders.class)
                        .isSameAs(context.getBean("subjectClaimReferenceCipher"));
                assertThat(((DefaultListableBeanFactory) context.getBeanFactory()).isAllowBeanDefinitionOverriding()).isFalse();
                assertThat(context.getBeansOfType(SubjectClaimProofVerifier.class)).containsOnlyKeys("subjectClaimProofVerifier");
                assertThat(context.getBeansOfType(SubjectClaimReferenceCipher.class)).containsOnlyKeys("subjectClaimProofVerifier");
                assertThat(context.getAliases("subjectClaimProofVerifier")).containsExactly("subjectClaimReferenceCipher");
            });
        }
    }
    @Test
    void customerComposerAndControllerUsePublishedOwnerProviderOnly() {
        var constructor = CustomerSubjectClaimsController.class.getConstructors()[0];
        assertThat(constructor.getParameters()[0].getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value()).isEqualTo("customerSubjectClaimCommandService");
        assertThat(CustomerSubjectClaimCommandService.class.getConstructors()[0].getGenericParameterTypes()[0].getTypeName()).contains("ObjectProvider<com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService>");
        assertThat(CustomerSubjectClaimsController.class.getAnnotation(org.springframework.context.annotation.Profile.class).value()).containsExactly("web");
    }
    @Test
    void roleAssertionFailureNeverFallsBackToAnotherPrincipal() {
        DSLContext context = mock(DSLContext.class); var row = mock(org.jooq.Record.class);
        when(context.fetchOne("SELECT current_user")).thenReturn(row); when(row.get(0, String.class)).thenReturn("other");
        assertThatThrownBy(() -> new JooqSubjectClaimRepository().assertRole(context)).isInstanceOf(com.onecall.core.subject_number.customerclaim.SubjectClaimException.class);
        var order = inOrder(context); order.verify(context).execute("SET LOCAL ROLE subject_number_role"); order.verify(context).fetchOne("SELECT current_user"); verifyNoMoreInteractions(context);
    }
    @Test
    void datasourceAndProviderDiagnosticsNeverExposeCredentialsOrReferences() {
        String canary = "PRIVATE_CREDENTIAL_CANARY";
        assertThat(new SubjectClaimPersistenceProperties("jdbc:synthetic", canary, canary).toString().contains(canary)).isFalse();
        try { new SubjectClaimPersistenceProperties("", canary, canary); fail("Expected credential validation"); }
        catch (IllegalArgumentException failure) { assertThat(failure.toString().contains(canary)).isFalse(); assertThat(failure.getCause()).isNull(); }
    }
}
