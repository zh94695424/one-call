package com.onecall.core.sharing.internal.persistence;

import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import com.onecall.core.sharing.internal.provider.Sha256Provider;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryService;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.sharing-command", name = "enabled", havingValue = "true")
public class PublicSharePersistenceConfiguration {
    @Bean
    TransactionTemplate publicShareQueryTransactionTemplate(
            @Qualifier("sharingCommandTransactionManager") DataSourceTransactionManager manager) {
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(true);
        return template;
    }

    @Bean
    PublicShareQueryRepository publicShareQueryRepository(@Qualifier("sharingCommandDsl") DSLContext context) {
        return new JooqPublicShareQueryRepository(context);
    }

    @Bean
    PublicShareQueryService publicShareQueryService(
            @Qualifier("publicShareQueryTransactionTemplate") TransactionTemplate template,
            @Qualifier("sharingCommandDsl") DSLContext context,
            @Qualifier("publicShareQueryRepository") PublicShareQueryRepository repository,
            @Qualifier("shareableEvidenceProjectionReader") ShareableEvidenceProjectionReader reader,
            @Qualifier("sha256Provider") Sha256Provider sha256) {
        return new PublicShareQueryService(template, context, repository, reader, sha256);
    }
}
