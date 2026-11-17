package com.onecall.core.operations_iam.internal.ivrread;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService;
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
public class OperationsIvrSessionReadConfiguration {
    @Bean
    @Lazy
    OperationsIvrSessionReadService operationsIvrSessionReadService(
            @Qualifier("operationsSubjectReadTransactionTemplate") TransactionTemplate transaction,
            @Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context,
            ObjectProvider<OperationsSubjectReadProperties> properties, ObjectProvider<IvrSessionReader> readers, ObjectProvider<IvrEvidenceReader> evidence) {
        return new DefaultOperationsIvrSessionReadService(transaction, dsl, context, properties, readers, evidence);
    }
}
