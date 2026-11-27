package com.onecall.core.entitlement_metering.internal.operationsread;

import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("operationsOrderIssuanceReader")
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public final class JooqOperationsOrderIssuanceReader implements IssuanceReader {
    private final DSLContext dsl;
    private final OperationsReadContext context;

    public JooqOperationsOrderIssuanceReader(@Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context) {
        this.dsl = dsl; this.context = context;
    }
    @Override public List<IssuanceFacts> fetchIssuances(List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty() || orderIds.size() > 100 || orderIds.stream().anyMatch(Objects::isNull)
                || orderIds.stream().distinct().count() != orderIds.size())
            throw new ContractRejectionException("Invalid order batch");
        return context.scoped(Role.TRIAL, () -> dsl.fetch(
                "SELECT order_id, status FROM entitlement_metering.entitlement_issuance WHERE order_id IN ("
                + String.join(",", Collections.nCopies(orderIds.size(), "CAST(? AS uuid)")) + ")", orderIds.toArray())
                .map(row -> new IssuanceFacts(row.get("order_id", UUID.class), row.get("status", String.class))));
    }
}
