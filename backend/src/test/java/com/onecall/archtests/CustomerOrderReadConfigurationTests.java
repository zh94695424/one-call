package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.customer_iam.internal.persistence.CustomerPersistenceConfiguration;
import com.onecall.core.customer_iam.internal.web.CustomerCatalogOfferCursorProperties;
import com.onecall.core.customer_iam.internal.web.CustomerOrderCursorCodec;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerOrderReadConfigurationTests {
    static Method factory(String name) {
        Method method = Arrays.stream(CustomerPersistenceConfiguration.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
        method.setAccessible(true);
        return method;
    }
    @Test void readTemplateIsReadOnlyRequiresNewRepeatableRead() throws Exception {
        var manager = new DataSourceTransactionManager(mock(DataSource.class));
        var template = (TransactionTemplate) factory("customerOrderTransactionTemplate")
                .invoke(new CustomerPersistenceConfiguration(), manager);
        assertThat(template.getTransactionManager()).isSameAs(manager);
        assertThat(template.isReadOnly()).isTrue();
        assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Test void keyAndServicesResolveLazily() {
        for (String name : List.of("customerOrderTransactionTemplate", "customerOrderQueryService", "customerOrderCursorCodec"))
            assertThat(factory(name).isAnnotationPresent(Lazy.class)).isTrue();
        assertThat(Arrays.stream(factory("customerOrderQueryService").getParameters())
                .map(parameter -> parameter.getAnnotation(Qualifier.class).value()).toList())
                .containsExactly("customerOrderTransactionTemplate", "customerQueryDsl", "customerBusinessNumberAccessRepository",
                        "customerOrderReader", "customerOrderSnapshotReader");
    }
    @Test void readGraphDoesNotActivateCommandPersistence() {
        assertThat(CustomerOrderCursorCodec.class.getConstructors()).hasSize(1);
        assertThat(CustomerOrderCursorCodec.class.getConstructors()[0].getParameterTypes())
                .containsExactly(CustomerCatalogOfferCursorProperties.class);
        assertThatThrownBy(() -> new CustomerOrderCursorCodec(new CustomerCatalogOfferCursorProperties()))
                .isInstanceOf(com.onecall.core.customer_iam.internal.application.CustomerContextException.class);
        assertThat(factory("customerOrderQueryService").getParameterTypes())
                .doesNotContain(com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.class);
    }
    @Test void ownerBeansUseOnlyCustomerDslAndSnapshotSeam() throws Exception {
        Class<?> configuration = Class.forName("com.onecall.core.catalog_order.internal.customerquery.CustomerOrderQueryConfiguration");
        assertThat(Arrays.stream(configuration.getDeclaredMethods()).map(Method::getName).toList())
                .containsExactlyInAnyOrder("customerOrderReader", "customerOrderSnapshotReader");
        assertThat(configuration.getAnnotation(org.springframework.context.annotation.Profile.class).value()).containsExactly("web");
        for (Method method : configuration.getDeclaredMethods()) assertThat(method.isAnnotationPresent(Lazy.class)).isTrue();
    }
}
