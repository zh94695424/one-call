package com.onecall.core.catalog_order.internal.customerquery;

import com.onecall.core.catalog_order.customerquery.CustomerSubjectOrderCountReader;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("web")
public class CustomerSubjectOrderCountQueryConfiguration {
    @Bean
    @Lazy
    CustomerSubjectOrderCountReader customerSubjectOrderCountReader(@Qualifier("customerQueryDsl") DSLContext context) {
        return new JooqCustomerSubjectOrderCountReader(context);
    }
}
