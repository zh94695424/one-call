package com.onecall.core.activation_consent.internal.command;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code;
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
@ConditionalOnProperty(prefix = "onecall.activation-command", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ActivationCommandPersistenceProperties.class)
public class ActivationCommandPersistenceConfiguration {
    @Bean(destroyMethod = "close")
    HikariDataSource activationCommandDataSource(ActivationCommandPersistenceProperties properties) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.url());
        configuration.setUsername(properties.username());
        configuration.setPassword(properties.password());
        configuration.setConnectionInitSql("SET TIME ZONE 'UTC'");
        try { return new HikariDataSource(configuration); }
        catch (RuntimeException failure) { throw new IllegalStateException("Activation command pool unavailable"); }
    }
    @Bean
    DataSourceTransactionManager activationCommandTransactionManager(@Qualifier("activationCommandDataSource") DataSource dataSource) {
        var manager = new DataSourceTransactionManager(dataSource);
        manager.setRollbackOnCommitFailure(true);
        return manager;
    }
    @Bean
    TransactionTemplate activationCommandTransactionTemplate(@Qualifier("activationCommandTransactionManager") DataSourceTransactionManager manager) {
        var template = new TransactionTemplate(manager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setReadOnly(false);
        return template;
    }
    @Bean
    DSLContext activationCommandDsl(@Qualifier("activationCommandDataSource") DataSource dataSource) {
        var configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(new Settings().withExecuteLogging(false).withParamType(ParamType.INDEXED).withStatementType(StatementType.PREPARED_STATEMENT));
        configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)));
        return DSL.using(configuration);
    }
    @Bean
    SubjectActivationRepository subjectActivationRepository() { return new JooqSubjectActivationRepository(); }
    @Bean
    CustomerSubjectActivationService customerSubjectActivationService(@Qualifier("activationCommandTransactionTemplate") TransactionTemplate transaction,
            @Qualifier("activationCommandDsl") DSLContext context, @Qualifier("subjectActivationRepository") SubjectActivationRepository repository,
            @Qualifier("activationTrialService") ObjectProvider<ActivationTrialService> owner, ObjectProvider<ActivationTrialService> candidates) {
        if (candidates.stream().count() != 1) throw new SubjectActivationException(Code.SERVICE_UNAVAILABLE);
        return new DefaultCustomerSubjectActivationService(transaction, context, repository, owner.getObject());
    }
}
