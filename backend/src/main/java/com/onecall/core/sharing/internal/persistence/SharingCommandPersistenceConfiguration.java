package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import com.onecall.core.sharing.internal.command.CustomerShareCommandService;
import com.onecall.core.sharing.internal.command.ShareCommandException.Code;
import com.onecall.core.sharing.internal.commandservice.DefaultCustomerShareCommandService;
import com.onecall.core.sharing.internal.provider.CapabilityTokenProvider;
import com.onecall.core.sharing.internal.provider.DesignatedMobileBindingProvider;
import com.onecall.core.sharing.internal.provider.ProviderResult;
import com.onecall.core.sharing.internal.provider.Sha256Provider;
import com.onecall.core.sharing.internal.provider.ShareCreationPolicyProvider;
import com.onecall.core.sharing.internal.provider.ShareExtractionCodeProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@ConditionalOnProperty(prefix = "onecall.sharing-command", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SharingCommandPersistenceProperties.class)
public class SharingCommandPersistenceConfiguration {
    @Bean(destroyMethod = "close")
    HikariDataSource sharingCommandDataSource(SharingCommandPersistenceProperties properties) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.url());
        configuration.setUsername(properties.username());
        configuration.setPassword(properties.password());
        return new HikariDataSource(configuration);
    }

    @Bean
    DataSourceTransactionManager sharingCommandTransactionManager(@Qualifier("sharingCommandDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    TransactionTemplate sharingCommandTransactionTemplate(@Qualifier("sharingCommandTransactionManager") DataSourceTransactionManager manager) {
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(false);
        return template;
    }

    @Bean
    DSLContext sharingCommandDsl(@Qualifier("sharingCommandDataSource") DataSource dataSource) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(new Settings().withExecuteLogging(false).withParamType(ParamType.INDEXED)
                .withStatementType(StatementType.PREPARED_STATEMENT));
        configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(dataSource)));
        return DSL.using(configuration);
    }

    @Bean
    SharingCommandRepository sharingCommandRepository(@Qualifier("sharingCommandDsl") DSLContext context) {
        return new JooqSharingCommandRepository(context);
    }

    @Bean
    CustomerShareCommandService customerShareCommandService(
            @Qualifier("sharingCommandTransactionTemplate") TransactionTemplate template,
            @Qualifier("sharingCommandDsl") DSLContext context,
            @Qualifier("sharingCommandRepository") SharingCommandRepository repository,
            @Qualifier("shareableEvidenceProjectionReader") ShareableEvidenceProjectionReader reader,
            @Qualifier("shareCreationPolicyProvider") ShareCreationPolicyProvider policy,
            @Qualifier("capabilityTokenProvider") CapabilityTokenProvider tokens,
            @Qualifier("shareExtractionCodeProvider") ShareExtractionCodeProvider codes,
            @Qualifier("designatedMobileBindingProvider") DesignatedMobileBindingProvider mobiles,
            @Qualifier("sha256Provider") Sha256Provider sha256) {
        return new DefaultCustomerShareCommandService(template, context, repository, reader, policy, tokens, codes, mobiles, sha256);
    }

    @Bean
    @ConditionalOnMissingBean(ShareCreationPolicyProvider.class)
    ShareCreationPolicyProvider shareCreationPolicyProvider() {
        return request -> new ProviderResult.Unavailable<>(Code.SHARE_ACCESS_POLICY_UNAVAILABLE);
    }

    @Bean
    @ConditionalOnMissingBean(CapabilityTokenProvider.class)
    CapabilityTokenProvider capabilityTokenProvider() {
        return grantId -> new ProviderResult.Unavailable<>(Code.SERVICE_UNAVAILABLE);
    }

    @Bean
    @ConditionalOnMissingBean(ShareExtractionCodeProvider.class)
    ShareExtractionCodeProvider shareExtractionCodeProvider() {
        return (grantId, generation) -> new ProviderResult.Unavailable<>(Code.SERVICE_UNAVAILABLE);
    }

    @Bean
    @ConditionalOnMissingBean(DesignatedMobileBindingProvider.class)
    DesignatedMobileBindingProvider designatedMobileBindingProvider() {
        return mobile -> new ProviderResult.Unavailable<>(Code.SERVICE_UNAVAILABLE);
    }

    @Bean
    @ConditionalOnMissingBean(Sha256Provider.class)
    Sha256Provider sha256Provider() {
        return bytes -> {
            try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (NoSuchAlgorithmException failure) { throw new IllegalStateException("Digest unavailable"); }
        };
    }
}
