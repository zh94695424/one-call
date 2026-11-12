package com.onecall.core.subject_number.internal.claim;

import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.subject-claim-command", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SubjectClaimPersistenceProperties.class)
@Import(SubjectClaimPersistenceConfiguration.UnavailableProvidersImport.class)
public class SubjectClaimPersistenceConfiguration {
    static class UnavailableProvidersImport implements DeferredImportSelector {
        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            return new String[] {SubjectClaimUnavailableProviders.class.getName()};
        }
    }

    @Bean(destroyMethod = "close")
    HikariDataSource subjectClaimCommandDataSource(SubjectClaimPersistenceProperties properties) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.url());
        configuration.setUsername(properties.username());
        configuration.setPassword(properties.password());
        try { return new HikariDataSource(configuration); }
        catch (RuntimeException failure) { throw new IllegalStateException("Subject claim command pool unavailable"); }
    }
    @Bean
    DataSourceTransactionManager subjectClaimCommandTransactionManager(@Qualifier("subjectClaimCommandDataSource") DataSource dataSource) {
        var manager = new DataSourceTransactionManager(dataSource);
        manager.setRollbackOnCommitFailure(true);
        return manager;
    }
    @Bean
    TransactionTemplate subjectClaimCommandTransactionTemplate(@Qualifier("subjectClaimCommandTransactionManager") DataSourceTransactionManager manager) {
        var template = new TransactionTemplate(manager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(false);
        return template;
    }
    @Bean
    DSLContext subjectClaimCommandDsl(@Qualifier("subjectClaimCommandDataSource") DataSource dataSource) {
        var configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(new Settings().withExecuteLogging(false).withParamType(ParamType.INDEXED).withStatementType(StatementType.PREPARED_STATEMENT));
        configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)));
        return DSL.using(configuration);
    }
    @Bean
    SubjectClaimRepository subjectClaimRepository() { return new JooqSubjectClaimRepository(); }
    @Bean
    CustomerSubjectClaimService customerSubjectClaimService(
            @Qualifier("subjectClaimCommandTransactionTemplate") TransactionTemplate transaction,
            @Qualifier("subjectClaimCommandDsl") DSLContext context,
            @Qualifier("subjectClaimRepository") SubjectClaimRepository repository,
            @Qualifier("subjectClaimProofVerifier") SubjectClaimProofVerifier proofs,
            @Qualifier("subjectClaimReferenceCipher") SubjectClaimReferenceCipher cipher,
            ObjectProvider<SubjectClaimProofVerifier> proofCandidates, ObjectProvider<SubjectClaimReferenceCipher> cipherCandidates) {
        if (proofCandidates.stream().count() != 1 || cipherCandidates.stream().count() != 1)
            throw new SubjectClaimException(SubjectClaimException.Code.SERVICE_UNAVAILABLE);
        return new DefaultCustomerSubjectClaimService(transaction, context, repository, proofs, cipher);
    }
}

class SubjectClaimUnavailableProviders {
    @Bean
    @ConditionalOnMissingBean(SubjectClaimProofVerifier.class)
    SubjectClaimProofVerifier subjectClaimProofVerifier() { return request -> new SubjectClaimProviderResult.Unavailable<>(); }
    @Bean
    @ConditionalOnMissingBean(SubjectClaimReferenceCipher.class)
    SubjectClaimReferenceCipher subjectClaimReferenceCipher() {
        return new SubjectClaimReferenceCipher() {
            @Override public SubjectClaimProviderResult<SealedReference> seal(byte[] payload) { return new SubjectClaimProviderResult.Unavailable<>(); }
            @Override public SubjectClaimProviderResult<byte[]> open(SealedReference reference) { return new SubjectClaimProviderResult.Unavailable<>(); }
        };
    }
}
