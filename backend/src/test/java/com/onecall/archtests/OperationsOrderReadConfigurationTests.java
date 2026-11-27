package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.operations.OperationsOrderReadTestProviders.*;
import com.onecall.archtests.operations.OperationsOrderReadTestProviders;
import com.onecall.core.operations_iam.internal.orderread.OperationsOrderReadConfiguration;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import com.onecall.core.catalog_order.internal.operationsread.JooqOperationsOrderReader;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;

class OperationsOrderReadConfigurationTests {
    @Test void persistenceConfigurationIsLazyConditionalAndReusesExistingResources() {
        for (boolean enabled : java.util.List.of(false, true)) try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("persistence");
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("onecall.operations-subject-read.enabled", Boolean.toString(enabled))));
            context.register(OperationsOrderReadConfiguration.class);
            context.refresh();
            assertThat(context.getBeanNamesForType(OperationsOrderReadService.class)).hasSize(enabled ? 1 : 0);
            assertThat(context.getBeanFactory().getSingletonNames()).doesNotContain("operationsOrderReadService");
            assertThat(context.getBeanNamesForType(com.onecall.core.catalog_order.customerorder.CustomerOrderSnapshotReader.class)).isEmpty();
        }
    }
    @Test void detailWithoutCursorConfigurationUsesNoWebOrPurchaseGraph() throws Exception {
        var graph = new OperationsOrderReadTestProviders();
        var row = mock(org.jooq.Record.class);
        var root = order();
        when(row.get("id", java.util.UUID.class)).thenReturn(root.orderId());
        when(row.get("order_no", String.class)).thenReturn(root.orderNo());
        when(row.get("subject_id", java.util.UUID.class)).thenReturn(root.subjectId());
        when(row.get("channel", String.class)).thenReturn(root.channel());
        when(row.get("catalog_version_id", java.util.UUID.class)).thenReturn(root.catalogVersionId());
        when(row.get("catalog_offer_id", java.util.UUID.class)).thenReturn(root.catalogOfferId());
        when(row.get("amount_cents", Long.class)).thenReturn(root.amountCents());
        when(row.get("currency", String.class)).thenReturn(root.currency());
        when(row.get("payment_status", String.class)).thenReturn(root.paymentStatus());
        when(row.get("created_at", java.time.OffsetDateTime.class)).thenReturn(TIME.atOffset(java.time.ZoneOffset.UTC));
        when(row.get("purchase_consent_id", java.util.UUID.class)).thenReturn(facts().purchaseConsentId());
        String snapshot = """
                {"audience":"PERSONAL","benefitLines":[{"entitlementType":"AI_SECONDS","grantIntervalMonths":0,
                "lineKey":"00000000-0000-0000-0000-000000000001","quantity":100,"schedule":[{"entitlementType":"AI_SECONDS",
                "expiresAt":"2027-02-01T00:00:00Z","installmentNo":1,"units":100,"validFrom":"2027-01-01T00:00:00Z"}],
                "validityDays":30}],"billingCycle":"ONCE","catalogOfferId":"%s","catalogVersionId":"%s",
                "channel":"WEB","currency":"CNY","installments":1,"offerType":"ONE_TIME","priceCents":100,
                "productCode":"PA43","schemaVersion":1,"terms":{},"versionNo":1,"versionToken":"historical"}
                """.formatted(root.catalogOfferId(), root.catalogVersionId());
        String canonical = tools.jackson.databind.json.JsonMapper.builder().build().readTree(snapshot).toString();
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        when(row.get("snapshot", String.class)).thenReturn(snapshot);
        when(row.get("snapshot_sha256", String.class)).thenReturn(hash);
        when(graph.dsl.fetchOptional(anyString(), any(Object[].class))).thenReturn(java.util.Optional.of(row));
        when(graph.scope.scoped(eq(Role.ORDER), any())).thenAnswer(call -> ((java.util.function.Supplier<?>) call.getArgument(1)).get());
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("persistence");
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of("onecall.operations-subject-read.enabled", "true")));
            var beans = context.getBeanFactory();
            beans.registerSingleton("operationsSubjectReadTransactionTemplate", graph.transaction);
            beans.registerSingleton("operationsSubjectReadDsl", graph.dsl);
            beans.registerSingleton("operationsSubjectReadContext", graph.scope);
            beans.registerSingleton("issuances", graph.issuances);
            context.register(OperationsOrderReadConfiguration.class, JooqOperationsOrderReader.class);
            context.refresh();
            var service = context.getBean(OperationsOrderReadService.class);
            assertThat(service.detail(graph.caller, detail()).snapshotSha256()).isEqualTo(hash);
            assertThat(context.getBeanNamesForType(com.onecall.core.catalog_order.customerorder.CustomerOrderSnapshotReader.class)).isEmpty();
            assertThat(context.getBeanNamesForType(OrderReader.class)).hasSize(1);
            when(row.get("snapshot_sha256", String.class)).thenReturn("b".repeat(64));
            assertThatThrownBy(() -> service.detail(graph.caller, detail())).isInstanceOfSatisfying(Failure.class,
                    failure -> assertThat(failure.code()).isEqualTo(Code.NOT_FOUND));
            when(graph.dsl.fetchOptional(anyString(), any(Object[].class))).thenThrow(new org.jooq.exception.DataAccessException("PRIVATE"));
            assertThatThrownBy(() -> service.detail(graph.caller, detail())).isInstanceOfSatisfying(Failure.class,
                    failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
        }
        assertThat(graph.transaction.isReadOnly()).isTrue();
        assertThat(graph.transaction.getIsolationLevel()).isEqualTo(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThat(graph.transaction.getPropagationBehavior()).isEqualTo(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
}
