package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.sharing.internal.application.CustomerShareQueryService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@EnableConfigurationProperties(SharingPersistenceProperties.class)
public class SharingPersistenceConfiguration {
    @Bean(destroyMethod = "close")
    HikariDataSource sharingQueryDataSource(SharingPersistenceProperties properties) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.getUrl());
        configuration.setUsername(properties.getUsername());
        configuration.setPassword(properties.getPassword());
        return new HikariDataSource(configuration);
    }

    @Bean
    DataSourceTransactionManager sharingQueryTransactionManager(@Qualifier("sharingQueryDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    TransactionTemplate sharingQueryTransactionTemplate(@Qualifier("sharingQueryTransactionManager") DataSourceTransactionManager manager) {
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(true);
        return template;
    }

    @Bean
    DSLContext sharingQueryDsl(@Qualifier("sharingQueryDataSource") DataSource dataSource) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(new Settings().withExecuteLogging(false).withParamType(ParamType.INDEXED)
                .withStatementType(StatementType.PREPARED_STATEMENT));
        configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)));
        return DSL.using(configuration);
    }

    @Bean
    ShareQueryRepository sharingQueryRepository(@Qualifier("sharingQueryDsl") DSLContext context) {
        return new JooqShareQueryRepository(context);
    }

    @Bean
    CustomerShareQueryService customerShareQueryService(@Qualifier("sharingQueryTransactionTemplate") TransactionTemplate template,
            @Qualifier("sharingQueryDsl") DSLContext context, @Qualifier("sharingQueryRepository") ShareQueryRepository repository) {
        return new CustomerShareQueryService(template, context, repository);
    }
}
