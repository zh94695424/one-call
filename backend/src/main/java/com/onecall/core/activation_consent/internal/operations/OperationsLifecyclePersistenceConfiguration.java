package com.onecall.core.activation_consent.internal.operations;

import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService;
import com.onecall.core.operations_iam.employeelock.EmployeeAdministratorLock;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.*;
import org.jooq.impl.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "onecall.operations-lifecycle", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OperationsLifecyclePersistenceProperties.class)
public class OperationsLifecyclePersistenceConfiguration {
    @Bean(destroyMethod = "close")
    HikariDataSource operationsLifecycleDataSource(OperationsLifecyclePersistenceProperties properties) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.url());
        configuration.setUsername(properties.username());
        configuration.setPassword(properties.password());
        configuration.setConnectionInitSql("SET TIME ZONE 'UTC'");
        try { return new HikariDataSource(configuration); }
        catch (RuntimeException failure) { throw new IllegalStateException("Operations lifecycle pool unavailable"); }
    }
    @Bean
    DataSourceTransactionManager operationsLifecycleTransactionManager(@Qualifier("operationsLifecycleDataSource") DataSource dataSource) {
        var manager = new DataSourceTransactionManager(dataSource);
        manager.setRollbackOnCommitFailure(true);
        return manager;
    }
    @Bean
    TransactionTemplate operationsLifecycleTransactionTemplate(@Qualifier("operationsLifecycleTransactionManager") DataSourceTransactionManager manager) {
        var template = new TransactionTemplate(manager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setReadOnly(false);
        return template;
    }
    @Bean
    DSLContext operationsLifecycleDsl(@Qualifier("operationsLifecycleDataSource") DataSource dataSource) {
        var configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(new Settings().withExecuteLogging(false).withParamType(ParamType.INDEXED).withStatementType(StatementType.PREPARED_STATEMENT));
        configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)));
        return DSL.using(configuration);
    }
    @Bean
    OperationsLifecycleRepository operationsLifecycleRepository() { return new JooqOperationsLifecycleRepository(); }
    @Bean
    OperationsSubjectLifecycleService operationsSubjectLifecycleService(@Qualifier("operationsLifecycleTransactionTemplate") TransactionTemplate transaction,
            @Qualifier("operationsLifecycleDsl") DSLContext context, @Qualifier("operationsLifecycleRepository") OperationsLifecycleRepository repository,
            ObjectProvider<EmployeeAdministratorLock> candidates) {
        return new DefaultOperationsSubjectLifecycleService(transaction, context, repository, candidates);
    }
}
