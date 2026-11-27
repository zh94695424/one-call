package com.onecall.core.entitlement_metering.internal.operationsread;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.ContractRejectionException;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import java.util.*;
import java.util.function.Supplier;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.*;
import org.junit.jupiter.api.Test;

class JooqOperationsOrderIssuanceReaderTests {
    @Test void completeRawStatusBatchUsesOnlyTwoColumnsAndTrialScope() {
        UUID identity = new UUID(1, 1);
        var scope = mock(OperationsReadContext.class);
        when(scope.scoped(eq(Role.TRIAL), any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(1)).get());
        var dsl = DSL.using(new MockConnection(query -> {
            assertThat(query.sql()).isEqualTo("SELECT order_id, status FROM entitlement_metering.entitlement_issuance WHERE order_id IN (CAST(? AS uuid))");
            assertThat(query.bindings()).containsExactly(identity);
            var local = DSL.using(SQLDialect.POSTGRES);
            var result = local.newResult(DSL.field("order_id", UUID.class), DSL.field("status", String.class));
            for (String status : List.of("PENDING", "PENDING", "FAILED")) {
                var row = local.newRecord(DSL.field("order_id", UUID.class), DSL.field("status", String.class));
                row.values(identity, status); result.add(row);
            }
            return new MockResult[] { new MockResult(3, result) };
        }), SQLDialect.POSTGRES);
        assertThat(new JooqOperationsOrderIssuanceReader(dsl, scope).fetchIssuances(List.of(identity))).hasSize(3);
        verify(scope).scoped(eq(Role.TRIAL), any());
    }
    @Test void nullEmptyDuplicateAndOverLimitBatchesNeverQuery() {
        var scope = mock(OperationsReadContext.class);
        var dsl = mock(org.jooq.DSLContext.class);
        var reader = new JooqOperationsOrderIssuanceReader(dsl, scope);
        UUID identity = UUID.randomUUID();
        for (List<UUID> batch : Arrays.asList(null, List.<UUID>of(), List.of(identity, identity),
                Arrays.asList(identity, null), java.util.stream.IntStream.range(0, 101).mapToObj(index -> new UUID(0, index)).toList()))
            assertThatThrownBy(() -> reader.fetchIssuances(batch)).isExactlyInstanceOf(ContractRejectionException.class);
        verifyNoInteractions(scope, dsl);
    }
}
