package com.onecall.core.catalog_order.internal.operationscommand;

import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public class CatalogSaleLifecycleConfiguration {
    @Bean OperationsCatalogSaleCommand operationsCatalogSaleCommand(
            @Qualifier("operationsSubjectReadCatalogSaleTransactionTemplate") TransactionTemplate transaction,
            EmployeeAuthorityReadPort authority, CatalogSaleLifecycleRepository repository) {
        if (transaction.isReadOnly() || transaction.getPropagationBehavior() != org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
                || transaction.getIsolationLevel() != org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED || transaction.getTimeout() != 5) throw new IllegalStateException("Catalog command unavailable");
        return new DefaultOperationsCatalogSaleCommand(transaction, authority, repository);
    }
}
