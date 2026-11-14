package com.onecall.core.catalog_order.internal.operationsread;

import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.OrderCounter;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("operationsOrderCounter")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class JooqOperationsOrderCounter implements OrderCounter {
    private final DSLContext dsl;
    private final OperationsReadContext context;
    public JooqOperationsOrderCounter(@Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context) { this.dsl = dsl; this.context = context; }
    @Override public long count(UUID subjectId) {
        if (subjectId == null) throw new Failure(Code.SERVICE_UNAVAILABLE);
        return context.scoped(Role.ORDER, () -> {
            Long count = dsl.fetchSingle("SELECT count(subject_id) AS order_count FROM catalog_order.customer_order WHERE subject_id = CAST(? AS uuid)", subjectId)
                    .get("order_count", Long.class);
            if (count == null || count < 0) throw new Failure(Code.SERVICE_UNAVAILABLE);
            return count;
        });
    }
}
