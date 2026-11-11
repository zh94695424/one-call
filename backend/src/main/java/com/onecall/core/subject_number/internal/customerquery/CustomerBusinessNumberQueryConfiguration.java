package com.onecall.core.subject_number.internal.customerquery;

import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("web")
public class CustomerBusinessNumberQueryConfiguration {
    @Bean
    @Lazy
    CustomerBusinessNumberReader customerBusinessNumberReader(@Qualifier("customerQueryDsl") DSLContext context) {
        return new JooqCustomerBusinessNumberReader(context);
    }
}
