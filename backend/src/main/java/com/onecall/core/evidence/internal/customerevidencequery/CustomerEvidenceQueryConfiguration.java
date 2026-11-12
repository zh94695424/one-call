package com.onecall.core.evidence.internal.customerevidencequery;

import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("web")
public class CustomerEvidenceQueryConfiguration {
    @Bean
    @Lazy
    CustomerEvidenceReader customerEvidenceReader(@Qualifier("customerQueryDsl") DSLContext context) {
        return new JooqCustomerEvidenceReader(context);
    }
}
