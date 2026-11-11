package com.onecall.core.evidence.internal.customerquery;

import com.onecall.core.evidence.customerquery.CustomerSubjectEvidenceCountReader;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("web")
public class CustomerSubjectEvidenceCountQueryConfiguration {
    @Bean
    @Lazy
    CustomerSubjectEvidenceCountReader customerSubjectEvidenceCountReader(@Qualifier("customerQueryDsl") DSLContext context) {
        return new JooqCustomerSubjectEvidenceCountReader(context);
    }
}
