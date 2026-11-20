package com.onecall.core.catalog_order.internal.operationsdraft;

import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public class CatalogDraftConfiguration {
    @Bean
    @Lazy
    OperationsCatalogDraftCommand operationsCatalogDraftCommand(
            @Qualifier("operationsSubjectReadCatalogSaleTransactionTemplate") TransactionTemplate transaction,
            ObjectProvider<EmployeeAuthorityReadPort> authorities, ObjectProvider<CatalogDraftRepository> repositories) {
        if (transaction.isReadOnly() || transaction.getPropagationBehavior() != TransactionDefinition.PROPAGATION_REQUIRES_NEW
                || transaction.getIsolationLevel() != TransactionDefinition.ISOLATION_READ_COMMITTED || transaction.getTimeout() != 5)
            throw new OperationsCatalogDraftCommand.Failure(OperationsCatalogDraftCommand.Code.SERVICE_UNAVAILABLE);
        try {
            var authority = authorities.stream().limit(2).toList();
            var repository = repositories.stream().limit(2).toList();
            if (authority.size() == 1 && repository.size() == 1) return new DefaultOperationsCatalogDraftCommand(transaction, authority.getFirst(), repository.getFirst());
        } catch (RuntimeException unavailable) { throw new OperationsCatalogDraftCommand.Failure(OperationsCatalogDraftCommand.Code.SERVICE_UNAVAILABLE); }
        throw new OperationsCatalogDraftCommand.Failure(OperationsCatalogDraftCommand.Code.SERVICE_UNAVAILABLE);
    }
}
