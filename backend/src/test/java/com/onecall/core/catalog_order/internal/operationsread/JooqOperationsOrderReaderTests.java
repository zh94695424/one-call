package com.onecall.core.catalog_order.internal.operationsread;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.*;
import org.junit.jupiter.api.Test;

class JooqOperationsOrderReaderTests {
    @Test void allPredicatesAreBoundAndOwnerScopeIsRestoredByContext() {
        var captured = new ArrayList<MockExecuteContext>();
        var dsl = DSL.using(new MockConnection(context -> {
            captured.add(context);
            return new MockResult[] { new MockResult(0, DSL.using(SQLDialect.POSTGRES).newResult(DSL.field("id", UUID.class))) };
        }), SQLDialect.POSTGRES);
        var scope = mock(OperationsReadContext.class);
        when(scope.scoped(eq(Role.ORDER), any())).thenAnswer(call -> ((Supplier<?>) call.getArgument(1)).get());
        var reader = new JooqOperationsOrderReader(dsl, scope);
        for (boolean subject : List.of(false, true)) for (boolean payment : List.of(false, true)) for (boolean seek : List.of(false, true))
            reader.fetchOrders(new Filters(subject ? new UUID(1, 1) : null, payment ? "UNKNOWN" : null),
                    seek ? Optional.of(new Anchor(Instant.parse("2026-11-27T00:00:00Z"), new UUID(2, 1))) : Optional.empty(), 21);
        assertThat(captured).hasSize(8);
        for (var query : captured) {
            assertThat(query.sql()).startsWith("SELECT o.id, o.order_no, o.subject_id, o.channel, o.catalog_version_id,")
                    .contains("ORDER BY o.created_at DESC, o.id DESC LIMIT ?")
                    .doesNotContain("account_id", "payment_order", "snapshot", "COUNT", "OFFSET", "FOR UPDATE", "SELECT *");
            assertThat(query.bindings()[query.bindings().length - 1]).isEqualTo(21);
        }
        reader.fetchOrder(new UUID(1, 1));
        assertThat(captured.getLast().sql()).contains("o.snapshot::text AS snapshot", "WHERE o.id = CAST(? AS uuid)")
                .doesNotContain("account_id", "JOIN");
        verify(scope, times(9)).scoped(eq(Role.ORDER), any());
    }
    @Test void invalidPortInputsNeverEnterOwnerScope() {
        var scope = mock(OperationsReadContext.class);
        var reader = new JooqOperationsOrderReader(mock(org.jooq.DSLContext.class), scope);
        for (int limit : List.of(0, 1, 102)) assertThatThrownBy(() -> reader.fetchOrders(new Filters(null, null), Optional.empty(), limit))
                .isExactlyInstanceOf(ContractRejectionException.class);
        assertThatThrownBy(() -> reader.fetchOrders(new Filters(null, "INVALID"), Optional.empty(), 21)).isExactlyInstanceOf(ContractRejectionException.class);
        assertThatThrownBy(() -> reader.fetchOrder(null)).isExactlyInstanceOf(ContractRejectionException.class);
        verifyNoInteractions(scope);
    }
}
