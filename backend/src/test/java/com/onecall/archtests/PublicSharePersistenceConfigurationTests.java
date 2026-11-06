package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.sharing.internal.persistence.PublicSharePersistenceConfiguration;
import com.onecall.core.sharing.internal.persistence.PublicSharePersistenceConfiguration.SharingPublicReadPersistenceProperties;
import com.onecall.core.evidence.internal.shareprojection.ShareProjectionReaderConfiguration;
import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryService;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class PublicSharePersistenceConfigurationTests {
    @Test
    void defaultWebAndDisabledCommandProfilesStayDatabaseFree() {
        for (String profile : List.of("default", "web", "persistence")) {
            new ApplicationContextRunner().withUserConfiguration(PublicSharePersistenceConfiguration.class)
                    .withPropertyValues("spring.profiles.active=" + profile, "onecall.sharing-command.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBeansOfType(PublicShareQueryService.class)).isEmpty();
                        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
                        assertThat(context.getBeansOfType(TransactionTemplate.class)).isEmpty();
                    });
        }
        var binding = new ApplicationContextRunner().withUserConfiguration(PublicReadPropertiesBindingOnly.class);
        String[] valid = {"synthetic-url-canary", "sharing_public_read_role", "synthetic-password-canary"};
        var invalid = new java.util.ArrayList<String[]>();
        invalid.add(new String[] {null, null, null});
        for (int index = 0; index < 3; index++) {
            for (String value : new String[] {null, "", "   "}) {
                var values = valid.clone();
                values[index] = value;
                invalid.add(values);
            }
        }
        invalid.add(new String[] {valid[0], "wrong-user-canary", valid[2]});
        String[] names = {"url", "username", "password"};
        for (String[] values : invalid) {
            assertThatThrownBy(() -> new SharingPublicReadPersistenceProperties(values[0], values[1], values[2]))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Explicit dedicated public-read credentials are required").hasNoCause();
            var properties = new java.util.ArrayList<String>();
            for (int index = 0; index < 3; index++) {
                if (values[index] != null) properties.add("onecall.sharing-public-read.datasource." + names[index] + "=" + values[index]);
            }
            binding.withPropertyValues(properties.toArray(String[]::new)).run(context -> {
                Throwable failure = context.getStartupFailure();
                assertThat(failure != null).isTrue();
                while (failure.getCause() != null) failure = failure.getCause();
                assertThat(failure.getClass()).isEqualTo(IllegalArgumentException.class);
                assertThat(failure.getMessage()).isEqualTo("Explicit dedicated public-read credentials are required")
                        .doesNotContain(valid[0], valid[1], valid[2], "wrong-user-canary");
                assertThat(failure.getCause()).isNull();
            });
        }
        binding.withPropertyValues("onecall.sharing-public-read.datasource.url=" + valid[0],
                "onecall.sharing-public-read.datasource.username=" + valid[1],
                "onecall.sharing-public-read.datasource.password=" + valid[2]).run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(SharingPublicReadPersistenceProperties.class);
                    assertThat(properties.url()).isEqualTo(valid[0]);
                    assertThat(properties.username()).isEqualTo(valid[1]);
                    assertThat(properties.password()).isEqualTo(valid[2]);
                    assertThat(properties.toString()).isEqualTo("[REDACTED]").doesNotContain(valid);
                });
        var propertiesType = SharingPublicReadPersistenceProperties.class;
        assertThat(Modifier.isPublic(propertiesType.getModifiers())).isTrue();
        assertThat(Modifier.isStatic(propertiesType.getModifiers())).isTrue();
        assertThat(propertiesType.getConstructors()).hasSize(1);
        assertThat(propertiesType.getAnnotation(ConfigurationProperties.class).value()).isEqualTo("onecall.sharing-public-read.datasource");
        assertThat(PublicSharePersistenceConfiguration.class.getAnnotation(EnableConfigurationProperties.class).value()).containsExactly(propertiesType);
    }

    @Test
    void publicReadUsesIsolatedQualifiedManagerAndEnforcedReadOnlyTemplate() throws Exception {
        var configuration = PublicSharePersistenceConfiguration.class;
        var method = configuration.getDeclaredMethod("publicShareQueryTransactionTemplate", DataSourceTransactionManager.class);
        method.setAccessible(true);
        var manager = mock(DataSourceTransactionManager.class);
        var template = (TransactionTemplate) method.invoke(new PublicSharePersistenceConfiguration(), manager);
        assertThat(template.getTransactionManager()).isSameAs(manager);
        assertThat(template.isReadOnly()).isTrue();
        assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThat(method.getParameters()[0].getAnnotation(Qualifier.class).value()).isEqualTo("publicShareQueryTransactionManager");
        var service = configuration.getDeclaredMethods();
        assertThat(Arrays.stream(service).map(java.lang.reflect.Method::getName)).containsExactlyInAnyOrder(
                "publicShareQueryDataSource", "publicShareQueryTransactionManager", "publicShareQueryDsl",
                "publicShareQueryTransactionTemplate", "publicShareQueryRepository", "publicShareQueryService");
        var factory = Arrays.stream(service).filter(candidate -> candidate.getName().equals("publicShareQueryService")).findFirst().orElseThrow();
        assertThat(Arrays.stream(factory.getParameters()).map(parameter -> parameter.getAnnotation(Qualifier.class).value()))
                .containsExactly("publicShareQueryTransactionTemplate", "publicShareQueryDsl", "publicShareQueryRepository", "publicShareableEvidenceProjectionReader", "sha256Provider");
        for (String name : List.of("publicShareQueryTransactionManager", "publicShareQueryDsl")) {
            var factoryMethod = configuration.getDeclaredMethod(name, javax.sql.DataSource.class);
            assertThat(factoryMethod.getParameters()[0].getAnnotation(Qualifier.class).value()).isEqualTo("publicShareQueryDataSource");
        }
        assertThat(configuration.getDeclaredMethod("publicShareQueryRepository", org.jooq.DSLContext.class)
                .getParameters()[0].getAnnotation(Qualifier.class).value()).isEqualTo("publicShareQueryDsl");
        assertThat(configuration.getDeclaredMethod("publicShareQueryDataSource", SharingPublicReadPersistenceProperties.class)
                .getAnnotation(Bean.class).destroyMethod()).isEqualTo("close");
        var dataSource = mock(javax.sql.DataSource.class);
        var managerFactory = configuration.getDeclaredMethod("publicShareQueryTransactionManager", javax.sql.DataSource.class);
        managerFactory.setAccessible(true);
        var actualManager = (DataSourceTransactionManager) managerFactory.invoke(new PublicSharePersistenceConfiguration(), dataSource);
        assertThat(actualManager.getDataSource()).isSameAs(dataSource);
        assertThat(actualManager.isEnforceReadOnly()).isTrue();
        assertThat(actualManager.isRollbackOnCommitFailure()).isTrue();
        var dslFactory = configuration.getDeclaredMethod("publicShareQueryDsl", javax.sql.DataSource.class);
        dslFactory.setAccessible(true);
        var publicDsl = (org.jooq.DSLContext) dslFactory.invoke(new PublicSharePersistenceConfiguration(), dataSource);
        assertThat(publicDsl.dialect()).isEqualTo(org.jooq.SQLDialect.POSTGRES);
        assertThat(publicDsl.settings().isExecuteLogging()).isFalse();
        assertThat(publicDsl.settings().getParamType()).isEqualTo(org.jooq.conf.ParamType.INDEXED);
        assertThat(publicDsl.settings().getStatementType()).isEqualTo(org.jooq.conf.StatementType.PREPARED_STATEMENT);
        verifyNoInteractions(dataSource);
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryService.java"));
        assertThat(source.indexOf("context.execute(\"SET TRANSACTION READ ONLY\")")).isLessThan(source.indexOf("repository.findByTokenDigest"));
        assertThat(source).doesNotContain("READ WRITE", "SET ROLE", "SET LOCAL ROLE", "sharing_command_role", "evidence_role", "new Hikari", "Logger", "System.out");
        var reader = com.onecall.core.evidence.internal.shareprojection.JooqShareableEvidenceProjectionReader.class
                .getMethod("readShareableProjection", org.jooq.DSLContext.class, java.util.UUID.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(reader.propagation()).isEqualTo(org.springframework.transaction.annotation.Propagation.MANDATORY);
        assertThat(reader.transactionManager()).isEqualTo("sharingCommandTransactionManager");
        String command = Files.readString(Path.of("src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandPersistenceConfiguration.java"));
        assertThat(command).contains("template.setReadOnly(false)", "StatementType.PREPARED_STATEMENT", "ParamType.INDEXED", "withExecuteLogging(false)");
        var publicMethod = ShareProjectionReaderConfiguration.class.getDeclaredMethod("publicShareableEvidenceProjectionReader");
        var condition = publicMethod.getAnnotation(ConditionalOnProperty.class);
        assertThat(condition.prefix()).isEqualTo("onecall.sharing-public-read.datasource");
        assertThat(condition.name()).containsExactly("url");
        assertThat(condition.matchIfMissing()).isFalse();
        assertThat(condition.havingValue()).isEmpty();
        assertThat(publicMethod.isAnnotationPresent(Primary.class)).isFalse();
        assertThat(ShareProjectionReaderConfiguration.class.isAnnotationPresent(Primary.class)).isFalse();
        assertThat(ShareProjectionReaderConfiguration.class.getDeclaredMethod("shareableEvidenceProjectionReader").isAnnotationPresent(Primary.class)).isFalse();
        var readers = new ApplicationContextRunner().withUserConfiguration(ShareProjectionReaderConfiguration.class)
                .withPropertyValues("spring.profiles.active=persistence", "onecall.sharing-command.enabled=true")
                .withBean("sharingCommandTransactionManager", DataSourceTransactionManager.class,
                        () -> new DataSourceTransactionManager(mock(javax.sql.DataSource.class)));
        readers.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(ShareableEvidenceProjectionReader.class).keySet()).containsExactly("shareableEvidenceProjectionReader");
            assertThat(context.getBean(ShareableEvidenceProjectionReader.class)).isSameAs(context.getBean("shareableEvidenceProjectionReader"));
            assertReader(context.getBean("shareableEvidenceProjectionReader"), "sharingCommandTransactionManager");
        });
        var publicReaders = readers.withPropertyValues("onecall.sharing-public-read.datasource.url=synthetic-url");
        publicReaders.withBean("publicShareQueryTransactionManager", DataSourceTransactionManager.class,
                () -> new DataSourceTransactionManager(mock(javax.sql.DataSource.class))).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(ShareableEvidenceProjectionReader.class).keySet())
                    .containsExactlyInAnyOrder("shareableEvidenceProjectionReader", "publicShareableEvidenceProjectionReader");
            assertThat(context.getBean("sharingCommandTransactionManager")).isNotSameAs(context.getBean("publicShareQueryTransactionManager"));
            var publicReader = context.getBean("publicShareableEvidenceProjectionReader", ShareableEvidenceProjectionReader.class);
            assertThat(publicReader).isNotSameAs(context.getBean("shareableEvidenceProjectionReader"));
            assertReader(publicReader, "publicShareQueryTransactionManager");
            assertReader(context.getBean("shareableEvidenceProjectionReader"), "sharingCommandTransactionManager");
            var dsl = mock(org.jooq.DSLContext.class);
            assertThatThrownBy(() -> publicReader.readShareableProjection(dsl, java.util.UUID.randomUUID()))
                    .isInstanceOf(IllegalTransactionStateException.class);
            verifyNoInteractions(dsl);
        });
        publicReaders.run(context -> {
            assertThat(context).hasNotFailed();
            var dsl = mock(org.jooq.DSLContext.class);
            var publicReader = context.getBean("publicShareableEvidenceProjectionReader", ShareableEvidenceProjectionReader.class);
            assertThatThrownBy(() -> publicReader.readShareableProjection(dsl, java.util.UUID.randomUUID()))
                    .isInstanceOf(NoSuchBeanDefinitionException.class).hasMessageContaining("publicShareQueryTransactionManager");
            verifyNoInteractions(dsl);
        });
    }

    private static void assertReader(Object reader, String manager) throws Exception {
        assertThat(AopUtils.isAopProxy(reader)).isTrue();
        var annotation = AopUtils.getTargetClass(reader).getMethod("readShareableProjection", org.jooq.DSLContext.class, java.util.UUID.class)
                .getAnnotation(Transactional.class);
        assertThat(annotation.propagation()).isEqualTo(Propagation.MANDATORY);
        assertThat(annotation.transactionManager()).isEqualTo(manager);
        assertThat(AopUtils.getTargetClass(reader).isAnnotationPresent(Primary.class)).isFalse();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SharingPublicReadPersistenceProperties.class)
    static class PublicReadPropertiesBindingOnly { }
}
