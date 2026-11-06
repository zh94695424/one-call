package com.onecall.core.evidence.internal.shareprojection;

import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.sharing-command", name = "enabled", havingValue = "true")
@EnableTransactionManagement
public class ShareProjectionReaderConfiguration {
    @Bean
    ShareableEvidenceProjectionReader shareableEvidenceProjectionReader() {
        return new JooqShareableEvidenceProjectionReader();
    }

    @Bean
    @ConditionalOnProperty(prefix = "onecall.sharing-public-read.datasource", name = "url", matchIfMissing = false)
    ShareableEvidenceProjectionReader publicShareableEvidenceProjectionReader() {
        return new PublicReadProjectionReader();
    }

    public static class PublicReadProjectionReader extends JooqShareableEvidenceProjectionReader {
        @Override
        @Transactional(transactionManager = "publicShareQueryTransactionManager", propagation = Propagation.MANDATORY)
        public ReadResult readShareableProjection(DSLContext transactionDsl, UUID evidenceId) {
            return super.readShareableProjection(transactionDsl, evidenceId);
        }
    }
}
