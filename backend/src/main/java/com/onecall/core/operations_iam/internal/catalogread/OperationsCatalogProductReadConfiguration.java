package com.onecall.core.operations_iam.internal.catalogread;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadPorts.CatalogProductReader;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public class OperationsCatalogProductReadConfiguration {
    @Bean
    @Lazy
    OperationsCatalogProductReadService operationsCatalogProductReadService(
            @Qualifier("operationsSubjectReadTransactionTemplate") TransactionTemplate transaction,
            @Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context,
            ObjectProvider<OperationsSubjectReadProperties> properties, ObjectProvider<CatalogProductReader> readers) {
        return new DefaultOperationsCatalogProductReadService(transaction, dsl, context, properties, readers);
    }
}
