package com.onecall.core.evidence.internal.shareprojection;

import com.onecall.core.evidence.shareprojection.ShareableEvidenceProjectionReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.sharing-command", name = "enabled", havingValue = "true")
@EnableTransactionManagement
public class ShareProjectionReaderConfiguration {
    @Bean
    ShareableEvidenceProjectionReader shareableEvidenceProjectionReader() {
        return new JooqShareableEvidenceProjectionReader();
    }
}
