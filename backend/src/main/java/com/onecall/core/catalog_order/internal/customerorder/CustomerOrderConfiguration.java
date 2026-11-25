package com.onecall.core.catalog_order.internal.customerorder;

import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public class CustomerOrderConfiguration {
    @Bean
    CustomerOrderCreationService customerOrderCreationService(
            @Qualifier("operationsSubjectReadCatalogSaleTransactionTemplate") TransactionTemplate transaction,
            @Qualifier("operationsSubjectReadCatalogSaleTransactionManager") DataSourceTransactionManager manager,
            @Qualifier("operationsSubjectReadDataSource") DataSource source,
            @Qualifier("operationsSubjectReadDsl") DSLContext dsl, PurchaseConsentService consent) {
        if (transaction.getTransactionManager() != manager || manager.getDataSource() != source || transaction.isReadOnly()
                || transaction.getPropagationBehavior() != TransactionDefinition.PROPAGATION_REQUIRES_NEW
                || transaction.getIsolationLevel() != TransactionDefinition.ISOLATION_READ_COMMITTED || transaction.getTimeout() != 5)
            throw new IllegalStateException("Order creation unavailable");
        return new DefaultCustomerOrderCreationService(transaction, new JooqCustomerOrderRepository(source, dsl), consent);
    }
}
