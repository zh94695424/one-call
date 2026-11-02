package com.onecall.core.customer_iam.internal.persistence;

import com.onecall.core.customer_iam.internal.application.CustomerContextService;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("web")
@EnableConfigurationProperties(CustomerPersistenceProperties.class)
public class CustomerPersistenceConfiguration {
    @Bean(destroyMethod = "close")
    @Lazy
    HikariDataSource customerQueryDataSource(CustomerPersistenceProperties properties) {
        properties.validate();
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.getUrl());
        configuration.setUsername(properties.getUsername());
        configuration.setPassword(properties.getPassword());
        return new HikariDataSource(configuration);
    }

    @Bean
    @Lazy
    DataSourceTransactionManager customerQueryTransactionManager(@Qualifier("customerQueryDataSource") DataSource source) {
        return new DataSourceTransactionManager(source);
    }

    @Bean
    @Lazy
    TransactionTemplate customerQueryTransactionTemplate(
            @Qualifier("customerQueryTransactionManager") DataSourceTransactionManager manager) {
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(true);
        return template;
    }

    @Bean
    @Lazy
    DSLContext customerQueryDsl(@Qualifier("customerQueryDataSource") DataSource source) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(new Settings().withExecuteLogging(false).withParamType(ParamType.INDEXED)
                .withStatementType(StatementType.PREPARED_STATEMENT));
        configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(source)));
        return DSL.using(configuration);
    }

    @Bean
    @Lazy
    CustomerContextRepository customerContextRepository(@Qualifier("customerQueryDsl") DSLContext context) {
        return new JooqCustomerContextRepository(context);
    }

    @Bean
    @Lazy
    CustomerContextService customerContextService(@Qualifier("customerQueryTransactionTemplate") TransactionTemplate template,
            @Qualifier("customerQueryDsl") DSLContext context,
            @Qualifier("customerContextRepository") CustomerContextRepository repository) {
        return new CustomerContextService(template, context, repository);
    }
}
