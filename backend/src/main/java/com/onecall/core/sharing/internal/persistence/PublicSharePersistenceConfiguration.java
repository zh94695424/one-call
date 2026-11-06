package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import com.onecall.core.sharing.internal.provider.Sha256Provider;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DataSourceConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.sharing-command", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PublicSharePersistenceConfiguration.SharingPublicReadPersistenceProperties.class)
public class PublicSharePersistenceConfiguration {
    @ConfigurationProperties("onecall.sharing-public-read.datasource")
    public static record SharingPublicReadPersistenceProperties(String url, String username, String password) {
        public SharingPublicReadPersistenceProperties {
            if (url == null || url.isBlank() || username == null || username.isBlank()
                    || password == null || password.isBlank() || !username.equals("sharing_public_read_role")) {
                throw new IllegalArgumentException("Explicit dedicated public-read credentials are required");
            }
        }

        @Override
        public String toString() {
            return "[REDACTED]";
        }
    }

    @Bean(destroyMethod = "close")
    HikariDataSource publicShareQueryDataSource(SharingPublicReadPersistenceProperties properties) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.url());
        configuration.setUsername(properties.username());
        configuration.setPassword(properties.password());
        return new HikariDataSource(configuration);
    }

    @Bean
    DataSourceTransactionManager publicShareQueryTransactionManager(
            @Qualifier("publicShareQueryDataSource") DataSource dataSource) {
        DataSourceTransactionManager manager = new DataSourceTransactionManager(dataSource);
        manager.setRollbackOnCommitFailure(true);
        manager.setEnforceReadOnly(true);
        return manager;
    }

    @Bean
    TransactionTemplate publicShareQueryTransactionTemplate(
            @Qualifier("publicShareQueryTransactionManager") DataSourceTransactionManager manager) {
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(true);
        return template;
    }

    @Bean
    DSLContext publicShareQueryDsl(@Qualifier("publicShareQueryDataSource") DataSource dataSource) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(new Settings().withExecuteLogging(false).withParamType(ParamType.INDEXED)
                .withStatementType(StatementType.PREPARED_STATEMENT));
        configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)));
        return DSL.using(configuration);
    }

    @Bean
    PublicShareQueryRepository publicShareQueryRepository(@Qualifier("publicShareQueryDsl") DSLContext context) {
        return new JooqPublicShareQueryRepository(context);
    }

    @Bean
    PublicShareQueryService publicShareQueryService(
            @Qualifier("publicShareQueryTransactionTemplate") TransactionTemplate template,
            @Qualifier("publicShareQueryDsl") DSLContext context,
            @Qualifier("publicShareQueryRepository") PublicShareQueryRepository repository,
            @Qualifier("publicShareableEvidenceProjectionReader") ShareableEvidenceProjectionReader reader,
            @Qualifier("sha256Provider") Sha256Provider sha256) {
        return new PublicShareQueryService(template, context, repository, reader, sha256);
    }
}
