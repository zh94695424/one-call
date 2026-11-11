package com.onecall.core.activation_consent.internal.customerquery;

import com.onecall.core.activation_consent.customerquery.CustomerActivationStatusReader;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("web")
public class CustomerActivationQueryConfiguration {
    @Bean
    @Lazy
    CustomerActivationStatusReader customerActivationStatusReader(@Qualifier("customerQueryDsl") DSLContext context) {
        return new JooqCustomerActivationStatusReader(context);
    }
}
