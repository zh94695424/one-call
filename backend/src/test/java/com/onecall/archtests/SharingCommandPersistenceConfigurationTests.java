package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.evidence.internal.shareprojection.JooqShareableEvidenceProjectionReader;
import com.onecall.core.evidence.internal.shareprojection.ShareProjectionReaderConfiguration;
import com.onecall.core.sharing.internal.persistence.SharingCommandPersistenceConfiguration;
import com.onecall.core.sharing.internal.persistence.SharingCommandRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

class SharingCommandPersistenceConfigurationTests {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SharingCommandPersistenceConfiguration.class, ShareProjectionReaderConfiguration.class);

    @Test
    void defaultAndWebProfilesCreateNoCommandDatasource() {
        for (String profile : List.of("default", "web", "persistence")) {
            runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
                assertThat(context.getBeansOfType(SharingCommandRepository.class)).isEmpty();
                assertThat(context.getBeansOfType(com.onecall.core.sharing.internal.provider.ShareRotationPolicyProvider.class)).isEmpty();
            });
        }
        runner.withPropertyValues("spring.profiles.active=web", "onecall.sharing-command.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
        });
        for (String missing : List.of("url", "username", "password")) {
            var properties = new java.util.ArrayList<String>(List.of("spring.profiles.active=persistence", "onecall.sharing-command.enabled=true"));
            for (String key : List.of("url", "username", "password")) properties.add("onecall.sharing-command.datasource." + key + "=" + (key.equals(missing) ? " " : "test"));
            runner.withPropertyValues(properties.toArray(String[]::new)).run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    void commandProfileUsesDistinctPoolAndResetsLocalRole() throws Exception {
        try (var construction = mockConstruction(HikariDataSource.class, (pool, captured) -> {
            HikariConfig configuration = (HikariConfig) captured.arguments().getFirst();
            assertThat(configuration.getJdbcUrl()).isEqualTo("jdbc:postgresql://unused/command");
            assertThat(configuration.getUsername()).isEqualTo("command-test");
        })) {
            runner.withPropertyValues("spring.profiles.active=persistence", "onecall.sharing-command.enabled=true",
                    "onecall.sharing-command.datasource.url=jdbc:postgresql://unused/command",
                    "onecall.sharing-command.datasource.username=command-test", "onecall.sharing-command.datasource.password=test-only")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(construction.constructed()).hasSize(1);
                        assertThat(context.containsBean("sharingQueryDataSource")).isFalse();
                        TransactionTemplate template = context.getBean("sharingCommandTransactionTemplate", TransactionTemplate.class);
                        assertThat(template.isReadOnly()).isFalse();
                        assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
                        assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                        assertThat(context.getBean("sharingCommandDataSource")).isSameAs(construction.constructed().getFirst());
                        var policy = context.getBean("shareRotationPolicyProvider", com.onecall.core.sharing.internal.provider.ShareRotationPolicyProvider.class);
                        var result = policy.evaluate(new com.onecall.core.sharing.internal.provider.ShareRotationPolicyProvider.RotationPolicyRequest(
                                new com.onecall.core.sharing.internal.command.ShareCommandCaller(java.util.UUID.randomUUID(), java.util.Optional.empty()),
                                java.util.UUID.randomUUID(), com.onecall.core.sharing.internal.application.ShareSummary.VerificationMode.EXTRACTION_CODE,
                                com.onecall.core.sharing.internal.application.ShareSummary.ShareStatus.LOCKED, 1, 0));
                        assertThat(result).isEqualTo(new com.onecall.core.sharing.internal.provider.ProviderResult.Unavailable<>(
                                com.onecall.core.sharing.internal.command.ShareCommandException.Code.SERVICE_UNAVAILABLE));
                    });
            verify(construction.constructed().getFirst()).close();
            var override = com.onecall.archtests.sharing.DeterministicShareCommandProviders.rotationPolicy();
            runner.withBean("shareRotationPolicyProvider", com.onecall.core.sharing.internal.provider.ShareRotationPolicyProvider.class, () -> override)
                    .withPropertyValues("spring.profiles.active=persistence", "onecall.sharing-command.enabled=true",
                            "onecall.sharing-command.datasource.url=jdbc:postgresql://unused/command",
                            "onecall.sharing-command.datasource.username=command-test", "onecall.sharing-command.datasource.password=test-only")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBeansOfType(com.onecall.core.sharing.internal.provider.ShareRotationPolicyProvider.class))
                                .containsOnlyKeys("shareRotationPolicyProvider");
                        assertThat(context.getBean("shareRotationPolicyProvider")).isSameAs(override);
                    });
        }
        var annotation = JooqShareableEvidenceProjectionReader.class.getMethod("readShareableProjection", DSLContext.class, java.util.UUID.class)
                .getAnnotation(Transactional.class);
        assertThat(annotation.propagation()).isEqualTo(Propagation.MANDATORY);
        assertThat(annotation.transactionManager()).isEqualTo("sharingCommandTransactionManager");
        for (String method : List.of("assertCommandRole", "switchToEvidenceRole", "restoreAndAssertCommandRole", "sampleDatabaseTime"))
            assertThat(SharingCommandRepository.class.getMethod(method, DSLContext.class)).isNotNull();
    }
}
