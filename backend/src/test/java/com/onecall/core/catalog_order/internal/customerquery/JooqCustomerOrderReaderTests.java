package com.onecall.core.catalog_order.internal.customerquery;

import static org.assertj.core.api.Assertions.*;
import com.onecall.core.catalog_order.customerquery.CustomerOrderReader.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

class JooqCustomerOrderReaderTests {
    final List<String> statements = new ArrayList<>();
    final List<Object[]> bindings = new ArrayList<>();
    final JooqCustomerOrderReader reader = new JooqCustomerOrderReader(DSL.using(new MockConnection(execution -> {
        statements.add(execution.sql()); bindings.add(execution.bindings());
        return new MockResult[]{new MockResult(0, DSL.using(SQLDialect.POSTGRES).newResult(DSL.field("id", UUID.class)))};
    }), SQLDialect.POSTGRES));
    final UUID account = UUID.randomUUID(), organization = UUID.randomUUID(), order = UUID.randomUUID();
    @Test void ownershipPredicatesAndBoundParametersAreExact() {
        reader.fetchOrders(new Scope(account, null), Optional.empty(), 21);
        assertThat(statements.getLast()).contains("o.account_id = CAST(? AS uuid) AND o.owner_organization_id IS NULL");
        assertThat(bindings.getLast()).containsExactly(account, 21);
        reader.fetchOrders(new Scope(account, organization), Optional.empty(), 21);
        assertThat(statements.getLast()).contains("o.account_id = CAST(? AS uuid) AND o.owner_organization_id = CAST(? AS uuid)");
        assertThat(bindings.getLast()).containsExactly(account, organization, 21);
        assertThat(statements).allSatisfy(statement -> assertThat(statement).doesNotContain(account.toString(), organization.toString()));
    }
    @Test void pageUsesLimitPlusOneAndPostgresOrder() {
        Anchor anchor = new Anchor(Instant.parse("2026-11-25T00:00:00Z"), order);
        reader.fetchOrders(new Scope(account, null), Optional.of(anchor), 101);
        assertThat(statements.getLast()).contains("(o.created_at, o.id) < (CAST(? AS timestamptz), CAST(? AS uuid))",
                "ORDER BY o.created_at DESC, o.id DESC LIMIT ?");
        assertThat(bindings.getLast()).containsExactly(account, "2026-11-25 00:00:00+00:00", order, 101);
        for (int limit : List.of(0, 1, 102))
            assertThatThrownBy(() -> reader.fetchOrders(new Scope(account, null), Optional.empty(), limit)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void issuanceQueryReturnsRawStatusesForOnlyRequestedOrders() {
        reader.fetchIssuances(List.of(order));
        assertThat(statements.getLast()).isEqualTo("SELECT order_id, status FROM entitlement_metering.entitlement_issuance WHERE order_id IN (CAST(? AS uuid))");
        assertThat(bindings.getLast()).containsExactly(order);
        assertThatThrownBy(() -> reader.fetchIssuances(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reader.fetchIssuances(java.util.Collections.nCopies(101, order))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reader.fetchIssuances(List.of(order, order))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void detailUsesTheSameAccountWorkspacePredicate() {
        assertThat(reader.fetchOrder(new Scope(account, null), order)).isEmpty();
        assertThat(statements.getLast()).contains("o.owner_organization_id IS NULL AND o.id = CAST(? AS uuid)",
                "o.snapshot::text AS snapshot, o.snapshot_sha256, o.purchase_consent_id, o.paid_at, o.closed_at");
        assertThat(bindings.getLast()).containsExactly(account, order);
        reader.fetchOrder(new Scope(account, organization), order);
        assertThat(bindings.getLast()).containsExactly(account, organization, order);
    }
    @Test void queriesUseOnlyApprovedColumnsAndNoWrites() {
        reader.fetchOrders(new Scope(account, null), Optional.empty(), 21);
        assertThat(statements.getLast()).doesNotContain("snapshot", "purchase_consent_id", "paid_at", "closed_at");
        reader.fetchOrder(new Scope(account, organization), order); reader.fetchIssuances(List.of(order));
        assertThat(statements).allSatisfy(statement -> assertThat(statement.toUpperCase(java.util.Locale.ROOT))
                .startsWith("SELECT ").doesNotContain("SELECT *", "FOR UPDATE", "FOR SHARE", "INSERT", "UPDATE", "DELETE", "COUNT(", "OFFSET", "ENTITLEMENT_GRANT"));
    }
}
