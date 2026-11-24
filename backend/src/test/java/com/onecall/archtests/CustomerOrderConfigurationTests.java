package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException.Code;
import com.onecall.core.customer_iam.internal.application.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerOrderConfigurationTests {
    static Method factory() throws Exception { Method method = Class.forName("com.onecall.core.catalog_order.internal.customerorder.CustomerOrderConfiguration").getDeclaredMethods()[0]; method.setAccessible(true); return method; }
    static TrustedCustomerCaller caller() { return new TrustedCustomerCaller(UUID.randomUUID(), TrustedCustomerCaller.Channel.WEB, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)); }
    @Test void reusesOnlyExistingLoPoolManagerAndDsl() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/catalog_order/internal/customerorder/CustomerOrderConfiguration.java"));
        assertThat(source).contains("operationsSubjectReadCatalogSaleTransactionTemplate", "operationsSubjectReadCatalogSaleTransactionManager", "operationsSubjectReadDataSource", "operationsSubjectReadDsl").doesNotContain("new Hikari", "new DataSourceTransactionManager");
        String existing = Files.readString(Path.of("src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java")); assertThat(existing).contains("setMaximumPoolSize(4)", "setConnectionTimeout(3000)");
    }
    @Test void requiresWritableReadCommittedRequiresNewTemplate() throws Exception {
        DataSource source = mock(DataSource.class); var manager = new DataSourceTransactionManager(source); var template = new TransactionTemplate(manager);
        var method = factory(); Object configuration = method.getDeclaringClass().getConstructor().newInstance();
        for (int variant = 0; variant < 5; variant++) {
            template.setReadOnly(false); template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED); template.setTimeout(5);
            switch (variant) { case 0 -> template.setReadOnly(true); case 1 -> template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED); case 2 -> template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ); case 3 -> template.setTimeout(6); default -> template.setTransactionManager(new DataSourceTransactionManager(source)); }
            assertThatThrownBy(() -> method.invoke(configuration, template, manager, source, mock(DSLContext.class), mock(com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService.class))).hasCauseInstanceOf(IllegalStateException.class);
        }
    }
    @Test void ownerHandoffIsMandatoryOnSameResource() throws Exception {
        var owner = Class.forName("com.onecall.core.activation_consent.internal.purchase.DefaultPurchaseConsentService");
        var annotation = owner.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(annotation.propagation()).isEqualTo(org.springframework.transaction.annotation.Propagation.MANDATORY); assertThat(annotation.transactionManager()).isEqualTo("operationsSubjectReadCatalogSaleTransactionManager");
    }
    @Test void missingPersistenceDependenciesFailClosedLazily() {
        var beans = new DefaultListableBeanFactory(); var service = new CustomerOrderCommandService(beans.getBeanProvider(CustomerOrderCreationService.class));
        assertThatThrownBy(() -> service.create(caller(), null)).isInstanceOfSatisfying(OrderCreationException.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
    }
    @Test void currentPurchasePolicyHasNoDemoOrTrialFallback() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/activation_consent/internal/purchase/PurchaseConsentConfiguration.java"));
        assertThat(source).contains("onecall.purchase-consent.current").doesNotContain("demo", "trialPolicy", "@Value");
        assertThat(Class.forName("com.onecall.core.activation_consent.internal.purchase.PurchaseConsentConfiguration$CurrentPolicy").getRecordComponents()).extracting(java.lang.reflect.RecordComponent::getName).containsExactly("policyCode", "policyVersion", "promptVersion", "contentSha256");
    }
    @Test void customerAdapterUsesOnlyTrustedPrincipalValues() {
        var beans = new DefaultListableBeanFactory(); List<Caller> received = new ArrayList<>();
        beans.registerSingleton("owner", (CustomerOrderCreationService) (command, caller) -> { received.add(caller); return null; });
        var adapter = new CustomerOrderCommandService(beans.getBeanProvider(CustomerOrderCreationService.class)); var caller = caller();
        adapter.create(caller, new Command(UUID.randomUUID(), UUID.randomUUID(), "WEB", "ACCEPT", "SYNTHETIC", "1", "1", "a".repeat(64), null, "key", UUID.randomUUID()));
        assertThat(received).containsExactly(new Caller(caller.accountId(), "WEB", "PERSONAL", null));
    }
    @Test void duplicateOrAmbiguousOwnerBeansFailClosed() {
        var beans = new DefaultListableBeanFactory(); beans.registerSingleton("first", mock(CustomerOrderCreationService.class)); beans.registerSingleton("second", mock(CustomerOrderCreationService.class));
        var adapter = new CustomerOrderCommandService(beans.getBeanProvider(CustomerOrderCreationService.class));
        assertThatThrownBy(() -> adapter.create(caller(), null)).isInstanceOfSatisfying(OrderCreationException.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
    }
    @Test void legacyCustomerConfigurationsRemainUnchanged() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/customer_iam/internal/persistence/CustomerPersistenceConfiguration.java"));
        assertThat(source).contains("customerQueryTransactionTemplate", "setReadOnly(true)", "PROPAGATION_REQUIRES_NEW");
        String activation = Files.readString(Path.of("src/main/java/com/onecall/core/activation_consent/internal/command/ActivationCommandPersistenceConfiguration.java")); assertThat(activation).contains("activation");
    }
}
