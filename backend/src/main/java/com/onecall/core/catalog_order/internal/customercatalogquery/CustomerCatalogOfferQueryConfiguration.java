package com.onecall.core.catalog_order.internal.customercatalogquery;

import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("web")
public class CustomerCatalogOfferQueryConfiguration {
    @Bean
    @Lazy
    CustomerCatalogOfferReader customerCatalogOfferReader(@Qualifier("customerQueryDsl") DSLContext context) {
        return new JooqCustomerCatalogOfferReader(context);
    }
}
