package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.CustomerSubjectDetailQueryService;
import com.onecall.core.customer_iam.internal.persistence.CustomerPersistenceConfiguration;
import com.onecall.core.customer_iam.internal.web.CustomerSubjectsController;
import com.onecall.core.evidence.internal.customerquery.CustomerSubjectEvidenceCountQueryConfiguration;
import com.onecall.core.catalog_order.internal.customerquery.CustomerSubjectOrderCountQueryConfiguration;
import com.onecall.core.subject_number.internal.customerquery.CustomerBusinessNumberQueryConfiguration;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerSubjectDetailConfigurationTests {
    private static final List<Class<?>> CONFIGURATIONS = List.of(CustomerSubjectEvidenceCountQueryConfiguration.class,
            CustomerSubjectOrderCountQueryConfiguration.class, CustomerBusinessNumberQueryConfiguration.class);

    private org.springframework.context.ConfigurableApplicationContext graph(boolean web, String profile) {
        return new SpringApplicationBuilder(OneCallApplication.class).web(web ? WebApplicationType.SERVLET : WebApplicationType.NONE)
                .profiles(profile).properties("server.port=0", "spring.main.banner-mode=off")
                .run("--spring.main.web-application-type=" + (web ? "servlet" : "none"));
    }
    private HttpResponse<String> request(org.springframework.context.ConfigurableApplicationContext context, String value, String token) throws Exception {
        int port = ((org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext) context).getWebServer().getPort();
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/customer/subjects/" + value));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        try (var client = HttpClient.newHttpClient()) { return client.send(builder.build(), HttpResponse.BodyHandlers.ofString()); }
    }

    @Test
    void defaultAndNonWebProfilesRemainInert() {
        for (String profile : List.of("default", "web", "unrelated")) try (var context = graph(false, profile)) {
            assertThat(context.getBeansOfType(CustomerSubjectsController.class)).isEmpty();
            assertThat(context.getBeanFactory().containsSingleton("customerSubjectDetailQueryService")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
            if (!profile.equals("web")) assertThat(context.getBeansOfType(CustomerSubjectDetailQueryService.class)).isEmpty();
        }
    }

    @Test
    void webWiringIsLazyAndSubjectValidationDoesNotCreatePools() throws Exception {
        try (var context = graph(true, "web")) {
            for (String name : List.of("customerSubjectDetailQueryService", "customerSubjectDetailTransactionTemplate", "customerSubjectAccessReader",
                    "customerSubjectEvidenceCountReader", "customerSubjectOrderCountReader")) {
                assertThat(context.getBeanFactory().getBeanDefinition(name).isLazyInit()).isTrue();
                assertThat(context.getBeanFactory().containsSingleton(name)).isFalse();
            }
            assertThat(request(context, "bad", null).statusCode()).isEqualTo(401);
            assertThat(request(context, UUID.randomUUID().toString(), "opaque").statusCode()).isEqualTo(503);
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
            assertThat(context.getBeansOfType(CustomerSubjectsController.class)).hasSize(1);
        }
    }

    @Test
    void subjectTemplateHasIndependentReadOnlyRepeatableNewTransactionSettings() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("web");
            context.registerBean("customerQueryTransactionManager", DataSourceTransactionManager.class,
                    () -> mock(DataSourceTransactionManager.class));
            context.register(CustomerPersistenceConfiguration.class); context.refresh();
            var subject = context.getBean("customerSubjectDetailTransactionTemplate", TransactionTemplate.class);
            var business = context.getBean("customerBusinessNumberTransactionTemplate", TransactionTemplate.class);
            var me = context.getBean("customerQueryTransactionTemplate", TransactionTemplate.class);
            assertThat(subject).isNotSameAs(business).isNotSameAs(me);
            assertThat(subject.isReadOnly()).isTrue();
            assertThat(subject.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThat(subject.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            assertThat(me.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
        }
    }

    @Test
    void ownerReadersShareExactlyTheQualifiedCustomerDsl() throws Exception {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("web");
            DSLContext customer = mock(DSLContext.class);
            context.registerBean("customerQueryDsl", DSLContext.class, () -> customer);
            context.registerBean("unrelatedDsl", DSLContext.class, () -> mock(DSLContext.class));
            CONFIGURATIONS.forEach(context::register); context.refresh();
            for (String bean : List.of("customerSubjectAccessReader", "customerSubjectEvidenceCountReader", "customerSubjectOrderCountReader")) {
                Object reader = context.getBean(bean);
                var field = reader.getClass().getDeclaredField("context"); field.setAccessible(true);
                assertThat(field.get(reader)).isSameAs(customer);
            }
            for (Class<?> configuration : CONFIGURATIONS) {
                assertThat(configuration.getAnnotation(Profile.class).value()).containsExactly("web");
                assertThat(configuration.getAnnotation(Configuration.class).proxyBeanMethods()).isFalse();
                for (var method : configuration.getDeclaredMethods()) {
                    assertThat(method.isAnnotationPresent(Bean.class)).isTrue();
                    assertThat(method.isAnnotationPresent(Lazy.class)).isTrue();
                    assertThat(method.isAnnotationPresent(Primary.class)).isFalse();
                    assertThat(method.getParameters()[0].getAnnotation(Qualifier.class).value()).isEqualTo("customerQueryDsl");
                }
            }
        }
    }

    @Test
    void controllerUsesOnlyQualifiedSubjectServiceProviderAndProblemWriter() {
        var parameters = CustomerSubjectsController.class.getConstructors()[0].getParameters();
        assertThat(parameters).hasSize(2);
        assertThat(parameters[0].getParameterizedType().getTypeName()).isEqualTo(
                "org.springframework.beans.factory.ObjectProvider<com.onecall.core.customer_iam.internal.application.CustomerSubjectDetailQueryService>");
        assertThat(parameters[0].getAnnotation(Qualifier.class).value()).isEqualTo("customerSubjectDetailQueryService");
        assertThat(parameters[1].getType()).isEqualTo(com.onecall.core.customer_iam.internal.web.CustomerProblemWriter.class);
        assertThat(CustomerSubjectsController.class.getAnnotation(Profile.class).value()).containsExactly("web");
    }

    @Test
    void missingResourcesFailClosedWithoutCursorEvidenceMediaOrCommandResources() throws Exception {
        try (var context = graph(true, "web")) {
            var response = request(context, UUID.randomUUID().toString(), "opaque");
            assertThat(response.statusCode()).isEqualTo(503);
            assertThat(response.body()).contains("SERVICE_UNAVAILABLE").doesNotContain("jdbc:", "password", "Exception");
            assertThat(context.getBeanFactory().containsSingleton("customerBusinessNumberCursorCodec")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("evidenceMediaReadDataSource")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("sharingCommandDataSource")).isFalse();
            assertThat(context.getBeanFactory().getSingletonNames()).doesNotContain("customerQueryDataSource");
        }
    }
}
