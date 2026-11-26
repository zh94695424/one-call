package com.onecall.core.catalog_order.internal.customerorder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.onecall.core.catalog_order.customerorder.CustomerOrderSnapshotReader.InvalidSnapshotException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CustomerOrderSnapshotReaderTests {
    final DefaultCustomerOrderSnapshotReader reader = new DefaultCustomerOrderSnapshotReader();
    static String stored() {
        return CustomerOrderSemantics.snapshot(CustomerOrderSemanticsTests.catalog(CustomerOrderSemanticsTests.source("{}")),
                CustomerOrderSemanticsTests.benefits());
    }
    static String hash(String value) { return CustomerOrderSemantics.sha(CustomerOrderSemantics.canonical(CustomerOrderSemantics.parse(value))); }
    @Test void validatesStoredSemanticsHashThenProjection() {
        String value = stored();
        var result = reader.read(value, hash(value));
        assertThat(result.amountCents()).isEqualTo(123);
        assertThat(result.snapshotSha256()).isEqualTo(hash(value));
        assertThat(reader.read(" \n" + value, hash(value))).isEqualTo(result);
        assertThatThrownBy(() -> reader.read(value, "0".repeat(64))).isInstanceOf(InvalidSnapshotException.class);
        var semanticFailure = new OrderCreationException(OrderCreationException.Code.INTERNAL);
        try (var semantics = mockStatic(CustomerOrderSemantics.class, CALLS_REAL_METHODS)) {
            semantics.when(() -> CustomerOrderSemantics.storedSnapshot("semantic")).thenThrow(semanticFailure);
            assertThatThrownBy(() -> reader.read("semantic", "a".repeat(64)))
                    .isInstanceOfSatisfying(InvalidSnapshotException.class, failure -> assertThat(failure.getCause()).isSameAs(semanticFailure));
        }
        var runtimeCanary = new IllegalStateException("private runtime canary");
        try (var semantics = mockStatic(CustomerOrderSemantics.class, CALLS_REAL_METHODS)) {
            semantics.when(() -> CustomerOrderSemantics.storedSnapshot("runtime")).thenThrow(runtimeCanary);
            assertThatThrownBy(() -> reader.read("runtime", "a".repeat(64))).isSameAs(runtimeCanary)
                    .isNotInstanceOf(InvalidSnapshotException.class);
        }
    }
    @Test void storedAndApiKeySetsAreExact() {
        String value = stored();
        assertThat(CustomerOrderSemantics.parse(value).propertyNames()).containsExactlyInAnyOrderElementsOf(Set.of(
                "schemaVersion", "catalogVersionId", "catalogOfferId", "channel", "versionToken", "productCode", "versionNo",
                "offerType", "audience", "priceCents", "currency", "billingCycle", "installments", "benefitLines", "terms"));
        assertThat(CustomerOrderSemantics.JSON.valueToTree(reader.read(value, hash(value))).propertyNames()).containsExactlyInAnyOrder(
                "schemaVersion", "catalogVersionId", "catalogOfferId", "channel", "versionToken", "productCode", "offerType",
                "audience", "amountCents", "currency", "billingCycle", "installments", "benefitLines", "snapshotSha256");
    }
    @Test void benefitProjectionHasExactlyFourKeys() {
        String value = stored();
        var benefit = CustomerOrderSemantics.JSON.valueToTree(reader.read(value, hash(value))).get("benefitLines").get(0);
        assertThat(benefit.propertyNames()).containsExactlyInAnyOrder("entitlementType", "quantity", "validityDays", "grantIntervalMonths");
        assertThat(benefit.toString()).doesNotContain("lineKey", "schedule");
    }
    @Test void missingExtraMalformedAndMismatchedFactsAreRejected() {
        String value = stored();
        for (String corrupt : java.util.List.of("{}", "[]", "{", value.replace("\"schemaVersion\":1", "\"schemaVersion\":2"),
                value.replace("\"terms\":{}", "\"terms\":null"), value.replace("\"terms\":{}", "\"terms\":{},\"extra\":true"),
                value.replace("\"quantity\":100", "\"quantity\":-1"), value.replace("\"currency\":\"CNY\"", "\"currency\":\"USD\""))) {
            assertThatThrownBy(() -> reader.read(corrupt, hash(value))).isInstanceOf(InvalidSnapshotException.class);
        }
        for (String corrupt : java.util.List.of("", "F".repeat(64), "a".repeat(63)))
            assertThatThrownBy(() -> reader.read(value, corrupt)).isInstanceOf(InvalidSnapshotException.class);
        assertThatThrownBy(() -> reader.read(null, null)).isInstanceOf(InvalidSnapshotException.class);
    }
    @Test void safeIntegerBoundariesAreEnforced() {
        String maximum = stored().replace("\"priceCents\":123", "\"priceCents\":9007199254740991");
        assertThat(reader.read(maximum, hash(maximum)).amountCents()).isEqualTo(9007199254740991L);
        String unsafe = maximum.replace("9007199254740991", "9007199254740992");
        assertThatThrownBy(() -> reader.read(unsafe, CustomerOrderSemantics.sha(unsafe))).isInstanceOf(InvalidSnapshotException.class);
        String unsafeBenefit = stored().replace("\"quantity\":100", "\"quantity\":9007199254740992");
        assertThatThrownBy(() -> reader.read(unsafeBenefit, CustomerOrderSemantics.sha(unsafeBenefit))).isInstanceOf(InvalidSnapshotException.class);
    }
}
