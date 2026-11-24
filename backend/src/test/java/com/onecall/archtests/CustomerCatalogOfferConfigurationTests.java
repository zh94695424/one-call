package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader;
import com.onecall.core.catalog_order.internal.customercatalogquery.CustomerCatalogOfferQueryConfiguration;
import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.persistence.CustomerPersistenceConfiguration;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerCatalogOfferConfigurationTests {
    private static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID OFFER = UUID.fromString("80000000-0000-0000-0000-000000000001");
    private static final String PATH = "/api/v1/customer/catalog/offers";
    private static final List<String> LAZY = List.of("customerCatalogOfferQueryService", "customerCatalogOfferTransactionTemplate",
            "customerCatalogOfferReader", "customerCatalogOfferCursorCodec", "customerQueryDsl", "customerQueryDataSource");

    private ConfigurableApplicationContext graph(boolean web, String profile, boolean verifier) {
        var builder = new SpringApplicationBuilder(OneCallApplication.class).web(web ? WebApplicationType.SERVLET : WebApplicationType.NONE)
                .profiles(profile).properties("server.port=0", "spring.main.banner-mode=off");
        if (verifier) builder.initializers(context -> context.getBeanFactory().registerSingleton("testVerifier",
                (CustomerSessionVerifier) (bearer, audience) -> new VerifiedCustomerSession(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                        audience, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null))));
        return builder.run("--spring.main.web-application-type=" + (web ? "servlet" : "none"));
    }

    private HttpResponse<String> get(ConfigurableApplicationContext context, String path, String token) throws Exception {
        int port = ((org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext) context).getWebServer().getPort();
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(java.time.Duration.ofSeconds(20));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        try (var client = HttpClient.newHttpClient()) { return client.send(builder.build(), HttpResponse.BodyHandlers.ofString()); }
    }

    private void problem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        var json = tools.jackson.databind.json.JsonMapper.builder().build().readTree(response.body());
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
        assertThat(json.path("code").asString()).isEqualTo(code);
        assertThat(response.body()).doesNotContain("jdbc:", "password", "Exception", "CANARY");
    }

    @Test
    void defaultAndNonWebProfilesRemainDatabaseFree() {
        for (String profile : List.of("default", "web", "unrelated")) try (var context = graph(false, profile, false)) {
            assertThat(context.getBeansOfType(CustomerCatalogOffersController.class)).isEmpty();
            for (String bean : LAZY) assertThat(context.getBeanFactory().containsSingleton(bean)).as(bean).isFalse();
            if (!profile.equals("web")) {
                assertThat(context.getBeansOfType(CustomerCatalogOfferQueryService.class)).isEmpty();
                assertThat(context.getBeansOfType(CustomerCatalogOfferReader.class)).isEmpty();
            }
        }
        try (var context = graph(true, "unrelated", false)) {
            assertThat(context.getBeansOfType(CustomerCatalogOffersController.class)).isEmpty();
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
        }
    }

    @Test
    void webWiringIsLazyAndInvalidRequestsDoNotCreatePools() throws Exception {
        try (var context = graph(true, "web", true)) {
            for (String bean : LAZY) {
                assertThat(context.getBeanFactory().getBeanDefinition(bean).isLazyInit()).as(bean).isTrue();
                assertThat(context.getBeanFactory().containsSingleton(bean)).as(bean).isFalse();
            }
            problem(get(context, PATH, null), 401, "UNAUTHENTICATED");
            problem(get(context, PATH + "?limit=bad", "opaque"), 422, "VALIDATION_FAILED");
            problem(get(context, PATH + "?cursor=bad", "opaque"), 400, "CURSOR_INVALID");
            problem(get(context, PATH + "?cursor=bad&cursor=bad", "opaque"), 400, "CURSOR_INVALID");
            for (String bean : LAZY) assertThat(context.getBeanFactory().containsSingleton(bean)).as(bean).isFalse();
        }
        var parameters = CustomerCatalogOffersController.class.getConstructors()[0].getParameters();
        assertThat(parameters).hasSize(3);
        for (int index = 0; index < 2; index++) {
            assertThat(parameters[index].getType()).isEqualTo(org.springframework.beans.factory.ObjectProvider.class);
            assertThat(parameters[index].getAnnotation(Qualifier.class).value()).isEqualTo(index == 0 ? "customerCatalogOfferQueryService" : "customerCatalogOfferCursorCodec");
        }
        assertThat(parameters[0].getParameterizedType().getTypeName()).endsWith("<com.onecall.core.customer_iam.internal.application.CustomerCatalogOfferQueryService>");
        assertThat(parameters[1].getParameterizedType().getTypeName()).endsWith("<com.onecall.core.customer_iam.internal.web.CustomerCatalogOfferCursorCodec>");
        assertThat(parameters[2].getType()).isEqualTo(CustomerProblemWriter.class);
        assertThat(CustomerCatalogOffersController.class.getAnnotation(Profile.class).value()).containsExactly("web");
        assertThat(CustomerCatalogOffersController.class.getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.class).type())
                .isEqualTo(org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET);
    }

    @Test
    void catalogOfferTemplateIsIndependentReadOnlyRepeatableRequiresNew() throws Exception {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("web");
            var manager = mock(DataSourceTransactionManager.class);
            var customerDsl = mock(DSLContext.class);
            var lifecycle = mock(JooqCustomerBusinessNumberAccessRepository.class);
            var reader = mock(CustomerCatalogOfferReader.class);
            context.registerBean("customerQueryTransactionManager", DataSourceTransactionManager.class, () -> manager);
            context.registerBean("customerQueryDsl", DSLContext.class, () -> customerDsl);
            context.registerBean("customerBusinessNumberAccessRepository", JooqCustomerBusinessNumberAccessRepository.class, () -> lifecycle);
            context.registerBean("customerCatalogOfferReader", CustomerCatalogOfferReader.class, () -> reader);
            context.register(CustomerPersistenceConfiguration.class); context.refresh();
            var template = context.getBean("customerCatalogOfferTransactionTemplate", TransactionTemplate.class);
            assertThat(template.getTransactionManager()).isSameAs(manager);
            assertThat(template.isReadOnly()).isTrue();
            assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            for (String name : List.of("customerQueryTransactionTemplate", "customerBusinessNumberTransactionTemplate", "customerSubjectDetailTransactionTemplate", "customerEvidenceTransactionTemplate")) {
                var existing = context.getBean(name, TransactionTemplate.class);
                assertThat(template).isNotSameAs(existing);
                assertThat(existing.getTransactionManager()).isSameAs(manager);
                assertThat(existing.isReadOnly()).isTrue();
            }
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
            var service = context.getBean("customerCatalogOfferQueryService", CustomerCatalogOfferQueryService.class);
            for (var entry : Map.of("transaction", template, "context", customerDsl, "lifecycle", lifecycle, "reader", reader).entrySet()) {
                var field = CustomerCatalogOfferQueryService.class.getDeclaredField(entry.getKey()); field.setAccessible(true);
                assertThat(field.get(service)).as(entry.getKey()).isSameAs(entry.getValue());
            }
            verifyNoInteractions(customerDsl, lifecycle, reader);
        }
        var method = Arrays.stream(CustomerPersistenceConfiguration.class.getDeclaredMethods())
                .filter(value -> value.getName().equals("customerCatalogOfferQueryService")).findFirst().orElseThrow();
        assertThat(Arrays.stream(method.getParameters()).map(value -> value.getAnnotation(Qualifier.class).value()))
                .containsExactly("customerCatalogOfferTransactionTemplate", "customerQueryDsl", "customerBusinessNumberAccessRepository", "customerCatalogOfferReader");
        for (String name : List.of("customerCatalogOfferTransactionTemplate", "customerCatalogOfferQueryService", "customerCatalogOfferCursorCodec")) {
            var bean = Arrays.stream(CustomerPersistenceConfiguration.class.getDeclaredMethods()).filter(value -> value.getName().equals(name)).findFirst().orElseThrow();
            assertThat(bean.isAnnotationPresent(Lazy.class)).isTrue();
            assertThat(bean.isAnnotationPresent(Bean.class)).isTrue();
            assertThat(bean.isAnnotationPresent(Primary.class)).isFalse();
        }
    }

    @Test
    void ownerReaderUsesOnlyTheQualifiedCustomerDsl() throws Exception {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("web");
            var customer = mock(DSLContext.class);
            var unrelated = mock(DSLContext.class);
            context.registerBean("customerQueryDsl", DSLContext.class, () -> customer);
            context.registerBean("unrelatedDsl", DSLContext.class, () -> unrelated);
            context.register(CustomerCatalogOfferQueryConfiguration.class); context.refresh();
            var reader = context.getBean("customerCatalogOfferReader");
            var field = reader.getClass().getDeclaredField("context"); field.setAccessible(true);
            assertThat(field.get(reader)).isSameAs(customer);
            assertThat(context.getBeanFactory().getBeanDefinition("customerCatalogOfferReader").isLazyInit()).isTrue();
            verifyNoInteractions(customer, unrelated);
            assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
            assertThat(context.getBeansOfType(org.springframework.transaction.PlatformTransactionManager.class)).isEmpty();
        }
        assertThat(CustomerCatalogOfferQueryConfiguration.class.getAnnotation(Profile.class).value()).containsExactly("web");
        assertThat(CustomerCatalogOfferQueryConfiguration.class.getAnnotation(Configuration.class).proxyBeanMethods()).isFalse();
        for (var method : CustomerCatalogOfferQueryConfiguration.class.getDeclaredMethods()) {
            assertThat(method.isAnnotationPresent(Bean.class)).isTrue();
            assertThat(method.isAnnotationPresent(Lazy.class)).isTrue();
            assertThat(method.isAnnotationPresent(Primary.class)).isFalse();
            assertThat(method.getParameters()).hasSize(1);
            assertThat(method.getParameters()[0].getType()).isEqualTo(DSLContext.class);
            assertThat(method.getParameters()[0].getAnnotation(Qualifier.class).value()).isEqualTo("customerQueryDsl");
        }
    }

    @Test
    void missingCredentialsReaderAndCursorKeyFailClosedWithoutFallback() throws Exception {
        try (var context = graph(true, "web", true)) {
            problem(get(context, PATH, "opaque"), 503, "SERVICE_UNAVAILABLE");
            var properties = new CustomerCatalogOfferCursorProperties();
            properties.setCursorHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
            String cursor = new CustomerCatalogOfferCursorCodec(properties).encode(
                    new CustomerCatalogOfferCursorCodec.Binding(ACCOUNT, null, "WEB", "PERSONAL", Optional.empty()),
                    new CustomerCatalogOfferCursorCodec.Anchor("ALPHA", 1, OFFER));
            problem(get(context, PATH + "?cursor=" + cursor, "opaque"), 503, "SERVICE_UNAVAILABLE");
            assertThat(context.getBeanFactory().containsSingleton("customerCatalogOfferQueryService")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
        }
        try (var context = graph(true, "web", false)) {
            problem(get(context, PATH, "opaque"), 503, "SERVICE_UNAVAILABLE");
            for (String bean : LAZY) assertThat(context.getBeanFactory().containsSingleton(bean)).as(bean).isFalse();
        }
        for (String key : List.of("", "CANARY", Base64.getEncoder().encodeToString(new byte[31]), Base64.getEncoder().encodeToString(new byte[33]))) {
            var properties = new CustomerCatalogOfferCursorProperties(); properties.setCursorHmacKey(key);
            assertThatThrownBy(() -> new CustomerCatalogOfferCursorCodec(properties)).isInstanceOfSatisfying(CustomerContextException.class,
                    failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.SERVICE_UNAVAILABLE));
        }
        var factory = new DefaultListableBeanFactory();
        var controller = new CustomerCatalogOffersController(factory.getBeanProvider(CustomerCatalogOfferQueryService.class),
                factory.getBeanProvider(CustomerCatalogOfferCursorCodec.class), new CustomerProblemWriter());
        var principal = new CustomerRequestPrincipal(new TrustedCustomerCaller(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
        assertThatThrownBy(() -> controller.listCustomerCatalogOffers(principal, new org.springframework.mock.web.MockHttpServletRequest()))
                .isInstanceOfSatisfying(CustomerContextException.class, failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.SERVICE_UNAVAILABLE));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("web");
            context.registerBean("customerQueryDsl", DSLContext.class, () -> mock(DSLContext.class));
            context.registerBean("customerQueryTransactionManager", DataSourceTransactionManager.class, () -> mock(DataSourceTransactionManager.class));
            context.register(CustomerPersistenceConfiguration.class); context.refresh();
            assertThatThrownBy(() -> context.getBean("customerCatalogOfferQueryService"))
                    .isInstanceOf(org.springframework.beans.BeansException.class);
            assertThat(context.getBeanFactory().containsSingleton("customerCatalogOfferQueryService")).isFalse();
        }
    }

    @Test
    void cursorKeyResolutionStaysAfterStructuralInputValidation() throws Exception {
        try (var context = graph(true, "web", true)) {
            problem(get(context, PATH + "?cursor=bad", "opaque"), 400, "CURSOR_INVALID");
            assertThat(context.getBeanFactory().containsSingleton("customerCatalogOfferCursorCodec")).isFalse();
            var properties = new CustomerCatalogOfferCursorProperties();
            properties.setCursorHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
            String cursor = new CustomerCatalogOfferCursorCodec(properties).encode(
                    new CustomerCatalogOfferCursorCodec.Binding(ACCOUNT, null, "WEB", "PERSONAL", Optional.empty()),
                    new CustomerCatalogOfferCursorCodec.Anchor("ALPHA", 1, OFFER));
            problem(get(context, PATH + "?cursor=" + cursor, "opaque"), 503, "SERVICE_UNAVAILABLE");
            problem(get(context, PATH, "opaque"), 503, "SERVICE_UNAVAILABLE");
        }
    }
}
