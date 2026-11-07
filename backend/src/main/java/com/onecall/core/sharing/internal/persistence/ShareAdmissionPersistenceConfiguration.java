package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.sharing.internal.admission.DefaultShareAdmissionService;
import com.onecall.core.sharing.internal.admission.DefaultShareMediaAuthorizationService;
import com.onecall.core.sharing.internal.admission.DesignatedMobileProofVerifier;
import com.onecall.core.sharing.internal.admission.JooqShareAdmissionRepository;
import com.onecall.core.sharing.internal.admission.ShareAdmissionCodeVerifier;
import com.onecall.core.sharing.internal.admission.ShareAdmissionPolicyProvider;
import com.onecall.core.sharing.internal.admission.ShareAdmissionRepository;
import com.onecall.core.sharing.internal.admission.ShareAdmissionSha256Provider;
import com.onecall.core.sharing.internal.admission.ShareAdmissionTicketProvider;
import com.onecall.core.sharing.internal.admission.ShareMediaCredentialProvider;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService;
import com.onecall.core.sharing.internal.publicadmission.ShareMediaAuthorizationService;
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
@ConditionalOnProperty(prefix = "onecall.sharing-admission.datasource", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ShareAdmissionPersistenceConfiguration.Properties.class)
public class ShareAdmissionPersistenceConfiguration {
    @ConfigurationProperties("onecall.sharing-admission.datasource")
    public record Properties(String url, String username, String password) {
        public Properties {
            if (url == null || url.isBlank() || !"sharing_admission_role".equals(username) || password == null || password.isBlank())
                throw new IllegalArgumentException("Explicit dedicated admission credentials are required");
        }
        @Override public String toString() { return "[REDACTED]"; }
    }
    @Bean(destroyMethod = "close")
    HikariDataSource sharingAdmissionDataSource(Properties properties) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.url());
        configuration.setUsername(properties.username());
        configuration.setPassword(properties.password());
        return new HikariDataSource(configuration);
    }
    @Bean
    DataSourceTransactionManager sharingAdmissionTransactionManager(@Qualifier("sharingAdmissionDataSource") DataSource source) {
        var manager = new DataSourceTransactionManager(source);
        manager.setRollbackOnCommitFailure(true);
        manager.setEnforceReadOnly(true);
        return manager;
    }
    @Bean
    TransactionTemplate sharingAdmissionTransactionTemplate(@Qualifier("sharingAdmissionTransactionManager") DataSourceTransactionManager manager) {
        var template = new TransactionTemplate(manager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(false);
        return template;
    }
    @Bean
    DSLContext sharingAdmissionDsl(@Qualifier("sharingAdmissionDataSource") DataSource source) {
        var configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(new Settings().withExecuteLogging(false).withParamType(ParamType.INDEXED).withStatementType(StatementType.PREPARED_STATEMENT));
        configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(source)));
        return DSL.using(configuration);
    }
    @Bean ShareAdmissionRepository shareAdmissionRepository(@Qualifier("sharingAdmissionDsl") DSLContext database) { return new JooqShareAdmissionRepository(database); }
    @Bean("sharingAdmissionSha256Provider") ShareAdmissionSha256Provider sharingAdmissionSha256Provider() {
        return bytes -> {
            try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (NoSuchAlgorithmException unavailable) { throw new ShareAdmissionException(ShareAdmissionException.Code.DEPENDENCY_UNAVAILABLE); }
        };
    }
    @Bean @ConditionalOnMissingBean(ShareAdmissionPolicyProvider.class)
    ShareAdmissionPolicyProvider unavailableShareAdmissionPolicyProvider() { return new UnavailablePolicyProvider(); }
    @Bean @ConditionalOnMissingBean(ShareAdmissionCodeVerifier.class)
    ShareAdmissionCodeVerifier unavailableShareAdmissionCodeVerifier() { return new UnavailableCodeVerifier(); }
    @Bean @ConditionalOnMissingBean(DesignatedMobileProofVerifier.class)
    DesignatedMobileProofVerifier unavailableDesignatedMobileProofVerifier() { return new UnavailableMobileProofVerifier(); }
    @Bean @ConditionalOnMissingBean(ShareMediaCredentialProvider.class)
    ShareMediaCredentialProvider unavailableShareMediaCredentialProvider() { return new UnavailableMediaCredentialProvider(); }
    @Bean @ConditionalOnMissingBean(ShareAdmissionTicketProvider.class)
    ShareAdmissionTicketProvider unavailableShareAdmissionTicketProvider() { return new UnavailableTicketProvider(); }
    @Bean
    ShareAdmissionService shareAdmissionService(@Qualifier("sharingAdmissionTransactionTemplate") TransactionTemplate transactions,
            ShareAdmissionRepository repository, @Qualifier("sharingAdmissionSha256Provider") ShareAdmissionSha256Provider sha256,
            ShareAdmissionPolicyProvider policies, ShareAdmissionCodeVerifier codes, DesignatedMobileProofVerifier mobiles,
            ShareMediaCredentialProvider credentials, ShareAdmissionTicketProvider tickets) {
        return new DefaultShareAdmissionService(transactions, repository, sha256, policies, codes, mobiles, credentials, tickets);
    }
    @Bean
    ShareMediaAuthorizationService shareMediaAuthorizationService(@Qualifier("sharingAdmissionTransactionTemplate") TransactionTemplate transactions,
            ShareAdmissionRepository repository, @Qualifier("sharingAdmissionSha256Provider") ShareAdmissionSha256Provider sha256,
            ShareMediaCredentialProvider credentials) {
        return new DefaultShareMediaAuthorizationService(transactions, repository, sha256, credentials);
    }
    static final class UnavailablePolicyProvider implements ShareAdmissionPolicyProvider {
        @Override public Result ticketPolicy(SharePolicyRequest request) { return new Unavailable(); }
    }
    static final class UnavailableCodeVerifier implements ShareAdmissionCodeVerifier {
        @Override public Result verify(CodeVerificationRequest request) { return new Unavailable(); }
    }
    static final class UnavailableMobileProofVerifier implements DesignatedMobileProofVerifier {
        @Override public Result verify(MobileProofRequest request) { return new Unavailable(); }
    }
    static final class UnavailableMediaCredentialProvider implements ShareMediaCredentialProvider {
        @Override public Result issue(MediaCredentialRequest request) { return new Unavailable(); }
    }
    static final class UnavailableTicketProvider implements ShareAdmissionTicketProvider {
        @Override public Result write(TicketWriteRequest request) { return new Unavailable(); }
    }
}
