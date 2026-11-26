package com.onecall.core.catalog_order.internal.customerquery;

import com.onecall.core.catalog_order.customerorder.CustomerOrderSnapshotReader;
import com.onecall.core.catalog_order.customerquery.CustomerOrderReader;
import com.onecall.core.catalog_order.internal.customerorder.DefaultCustomerOrderSnapshotReader;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("web")
public class CustomerOrderQueryConfiguration {
    @Bean
    @Lazy
    CustomerOrderReader customerOrderReader(@Qualifier("customerQueryDsl") DSLContext context) {
        return new JooqCustomerOrderReader(context);
    }

    @Bean
    @Lazy
    CustomerOrderSnapshotReader customerOrderSnapshotReader() {
        return new DefaultCustomerOrderSnapshotReader();
    }
}
