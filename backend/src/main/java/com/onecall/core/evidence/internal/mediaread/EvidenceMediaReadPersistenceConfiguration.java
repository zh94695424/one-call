package com.onecall.core.evidence.internal.mediaread;

import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
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
@ConditionalOnProperty(prefix = "onecall.evidence-media-read.datasource", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(EvidenceMediaReadPersistenceProperties.class)
public class EvidenceMediaReadPersistenceConfiguration {
    @Bean(destroyMethod = "close")
    HikariDataSource evidenceMediaReadDataSource(EvidenceMediaReadPersistenceProperties properties) {
        HikariDataSource source = new HikariDataSource() {
            @Override public String toString() { return "EvidenceMediaReadDataSource[REDACTED]"; }
        };
        source.setJdbcUrl(properties.url());
        source.setUsername(properties.username());
        source.setPassword(properties.password());
        source.setReadOnly(true);
        source.setPoolName("evidence-media-read");
        source.setMaximumPoolSize(2);
        return source;
    }

    @Bean
    DataSourceTransactionManager evidenceMediaReadTransactionManager(
            @Qualifier("evidenceMediaReadDataSource") DataSource dataSource) {
        var manager = new DataSourceTransactionManager(dataSource);
        manager.setEnforceReadOnly(true);
        manager.setRollbackOnCommitFailure(true);
        return manager;
    }

    @Bean
    TransactionTemplate evidenceMediaReadTransactionTemplate(
            @Qualifier("evidenceMediaReadTransactionManager") DataSourceTransactionManager manager) {
        var template = new TransactionTemplate(manager);
        template.setReadOnly(true);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    @Bean
    DSLContext evidenceMediaReadDsl(@Qualifier("evidenceMediaReadDataSource") DataSource dataSource) {
        DefaultConfiguration config = new DefaultConfiguration();
        config.set(SQLDialect.POSTGRES);
        config.set(new Settings().withExecuteLogging(false).withParamType(ParamType.INDEXED)
                .withStatementType(StatementType.PREPARED_STATEMENT));
        config.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)));
        return DSL.using(config);
    }

    @Bean
    EvidenceMediaObjectReader evidenceMediaObjectReader(@Qualifier("evidenceMediaReadDsl") DSLContext dsl,
            @Qualifier("evidenceMediaReadTransactionTemplate") TransactionTemplate transactions) {
        return new JooqEvidenceMediaObjectReader(dsl, transactions);
    }

    @Override public String toString() { return "EvidenceMediaReadPersistenceConfiguration[REDACTED]"; }
}
