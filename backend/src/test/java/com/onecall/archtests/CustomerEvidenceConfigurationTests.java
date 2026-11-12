package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.persistence.CustomerPersistenceConfiguration;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader;
import com.onecall.core.evidence.internal.customerevidencequery.CustomerEvidenceQueryConfiguration;
import com.onecall.core.subject_number.internal.customerquery.CustomerBusinessNumberQueryConfiguration;
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
import org.springframework.mock.web.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerEvidenceConfigurationTests {
    private static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID EVIDENCE = UUID.fromString("abcdefab-0000-0000-0000-000000000001");
    private static final String LIST = "/api/v1/customer/evidence";
    private static final List<String> LAZY = List.of("customerEvidenceQueryService", "customerEvidenceTransactionTemplate",
            "customerEvidenceReader", "customerSubjectAccessReader", "customerEvidenceCursorCodec", "customerQueryDsl", "customerQueryDataSource");

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
            assertThat(context.getBeansOfType(CustomerEvidenceController.class)).isEmpty();
            for (String bean : LAZY) assertThat(context.getBeanFactory().containsSingleton(bean)).as(bean).isFalse();
            if (!profile.equals("web")) {
                assertThat(context.getBeansOfType(CustomerEvidenceQueryService.class)).isEmpty();
                assertThat(context.getBeansOfType(CustomerEvidenceReader.class)).isEmpty();
            }
        }
        try (var context = graph(true, "unrelated", false)) {
            assertThat(context.getBeansOfType(CustomerEvidenceController.class)).isEmpty();
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
            problem(get(context, LIST + "?limit=bad", null), 401, "UNAUTHENTICATED");
            problem(get(context, LIST + "/bad", "opaque"), 422, "VALIDATION_FAILED");
            problem(get(context, LIST + "?limit=bad&cursor=bad", "opaque"), 422, "VALIDATION_FAILED");
            problem(get(context, LIST + "?cursor=bad", "opaque"), 400, "CURSOR_INVALID");
            problem(get(context, LIST + "?cursor=bad&cursor=bad", "opaque"), 400, "CURSOR_INVALID");
            for (String bean : LAZY) assertThat(context.getBeanFactory().containsSingleton(bean)).as(bean).isFalse();
        }
        var parameters = CustomerEvidenceController.class.getConstructors()[0].getParameters();
        assertThat(parameters).hasSize(3);
        for (int index = 0; index < 2; index++) {
            assertThat(parameters[index].getType()).isEqualTo(org.springframework.beans.factory.ObjectProvider.class);
            assertThat(parameters[index].getAnnotation(Qualifier.class).value()).isEqualTo(index == 0 ? "customerEvidenceQueryService" : "customerEvidenceCursorCodec");
        }
        assertThat(parameters[0].getParameterizedType().getTypeName()).endsWith("<com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryService>");
        assertThat(parameters[1].getParameterizedType().getTypeName()).endsWith("<com.onecall.core.customer_iam.internal.web.CustomerEvidenceCursorCodec>");
        assertThat(parameters[2].getType()).isEqualTo(CustomerProblemWriter.class);
        assertThat(CustomerEvidenceController.class.getAnnotation(Profile.class).value()).containsExactly("web");
        assertThat(CustomerEvidenceController.class.getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.class).type())
                .isEqualTo(org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET);
    }

    @Test
    void evidenceTemplateIsIndependentReadOnlyRepeatableRequiresNew() throws Exception {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("web");
            var manager = mock(DataSourceTransactionManager.class);
            var customerDsl = mock(DSLContext.class);
            var lifecycle = mock(com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository.class);
            var subjects = mock(com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader.class);
            var reader = mock(CustomerEvidenceReader.class);
            context.registerBean("customerQueryTransactionManager", DataSourceTransactionManager.class, () -> manager);
            context.registerBean("customerQueryDsl", DSLContext.class, () -> customerDsl);
            context.registerBean("customerBusinessNumberAccessRepository", com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository.class, () -> lifecycle);
            context.registerBean("customerSubjectAccessReader", com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader.class, () -> subjects);
            context.registerBean("customerEvidenceReader", CustomerEvidenceReader.class, () -> reader);
            context.register(CustomerPersistenceConfiguration.class); context.refresh();
            var evidence = context.getBean("customerEvidenceTransactionTemplate", TransactionTemplate.class);
            assertThat(evidence.getTransactionManager()).isSameAs(manager);
            assertThat(evidence.isReadOnly()).isTrue();
            assertThat(evidence.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThat(evidence.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            for (String name : List.of("customerQueryTransactionTemplate", "customerBusinessNumberTransactionTemplate", "customerSubjectDetailTransactionTemplate")) {
                var existing = context.getBean(name, TransactionTemplate.class);
                assertThat(evidence).isNotSameAs(existing);
                assertThat(existing.getTransactionManager()).isSameAs(manager);
                assertThat(existing.isReadOnly()).isTrue();
                assertThat(existing.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                assertThat(existing.getIsolationLevel()).isEqualTo(name.equals("customerQueryTransactionTemplate")
                        ? TransactionDefinition.ISOLATION_READ_COMMITTED : TransactionDefinition.ISOLATION_REPEATABLE_READ);
            }
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
            var service = context.getBean("customerEvidenceQueryService", CustomerEvidenceQueryService.class);
            for (var entry : Map.of("transaction", evidence, "context", customerDsl, "lifecycle", lifecycle, "subjects", subjects, "evidence", reader).entrySet()) {
                var field = CustomerEvidenceQueryService.class.getDeclaredField(entry.getKey()); field.setAccessible(true);
                assertThat(field.get(service)).as(entry.getKey()).isSameAs(entry.getValue());
            }
            verifyNoInteractions(customerDsl, lifecycle, subjects, reader);
            verify(manager).afterPropertiesSet();
            verifyNoMoreInteractions(manager);
        }
        var method = Arrays.stream(CustomerPersistenceConfiguration.class.getDeclaredMethods())
                .filter(value -> value.getName().equals("customerEvidenceQueryService")).findFirst().orElseThrow();
        assertThat(Arrays.stream(method.getParameters()).map(value -> value.getAnnotation(Qualifier.class).value()))
                .containsExactly("customerEvidenceTransactionTemplate", "customerQueryDsl", "customerBusinessNumberAccessRepository",
                        "customerSubjectAccessReader", "customerEvidenceReader");
        for (String name : List.of("customerEvidenceTransactionTemplate", "customerEvidenceQueryService", "customerEvidenceCursorCodec")) {
            var bean = Arrays.stream(CustomerPersistenceConfiguration.class.getDeclaredMethods()).filter(value -> value.getName().equals(name)).findFirst().orElseThrow();
            assertThat(bean.isAnnotationPresent(Lazy.class)).isTrue();
            assertThat(bean.isAnnotationPresent(Primary.class)).isFalse();
        }
    }

    @Test
    void ownerReadersUseOnlyTheQualifiedCustomerDsl() throws Exception {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("web");
            var customer = mock(DSLContext.class);
            var unrelated = mock(DSLContext.class);
            context.registerBean("customerQueryDsl", DSLContext.class, () -> customer);
            context.registerBean("unrelatedDsl", DSLContext.class, () -> unrelated);
            context.register(CustomerEvidenceQueryConfiguration.class, CustomerBusinessNumberQueryConfiguration.class); context.refresh();
            for (String bean : List.of("customerEvidenceReader", "customerSubjectAccessReader")) {
                var reader = context.getBean(bean);
                var field = reader.getClass().getDeclaredField("context"); field.setAccessible(true);
                assertThat(field.get(reader)).isSameAs(customer);
                assertThat(context.getBeanFactory().getBeanDefinition(bean).isLazyInit()).isTrue();
            }
            verifyNoInteractions(customer, unrelated);
            assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
            assertThat(context.getBeansOfType(org.springframework.transaction.PlatformTransactionManager.class)).isEmpty();
        }
        for (Class<?> configuration : List.of(CustomerEvidenceQueryConfiguration.class, CustomerBusinessNumberQueryConfiguration.class)) {
            assertThat(configuration.getAnnotation(Profile.class).value()).containsExactly("web");
            assertThat(configuration.getAnnotation(Configuration.class).proxyBeanMethods()).isFalse();
            for (var method : configuration.getDeclaredMethods()) {
                assertThat(method.isAnnotationPresent(Bean.class)).isTrue();
                assertThat(method.isAnnotationPresent(Lazy.class)).isTrue();
                assertThat(method.isAnnotationPresent(Primary.class)).isFalse();
                assertThat(method.getParameters()).hasSize(1);
                assertThat(method.getParameters()[0].getType()).isEqualTo(DSLContext.class);
                assertThat(method.getParameters()[0].getAnnotation(Qualifier.class).value()).isEqualTo("customerQueryDsl");
            }
        }
    }

    @Test
    void detailNeverResolvesCursorKeyMediaOrSharingResources() throws Exception {
        try (var context = graph(true, "web", true)) {
            var service = mock(CustomerEvidenceQueryService.class);
            context.getBeanFactory().registerSingleton("customerEvidenceQueryService", service);
            when(service.getCustomerEvidence(any(), eq(EVIDENCE))).thenReturn(Optional.of(new CustomerEvidenceQueryService.Detail(
                    EVIDENCE, ACCOUNT, "FAILED", "CALL_RECORDING", java.time.Instant.parse("2026-01-01T00:00:00Z"), "v1", null, List.of())));
            assertThat(get(context, LIST + "/" + EVIDENCE + "?cursor=bad&cursor=bad&limit=bad", "opaque").statusCode()).isEqualTo(200);
            when(service.getCustomerEvidence(any(), eq(EVIDENCE))).thenReturn(Optional.empty());
            problem(get(context, LIST + "/" + EVIDENCE, "opaque"), 404, "NOT_FOUND");
            for (String bean : context.getBeanDefinitionNames()) {
                String lower = bean.toLowerCase(Locale.ROOT);
                if ((lower.contains("media") || lower.contains("sharing")) && context.getBeanFactory().getBeanDefinition(bean).isLazyInit())
                    assertThat(context.getBeanFactory().containsSingleton(bean)).as(bean).isFalse();
            }
            for (String bean : List.of("customerEvidenceCursorCodec", "customerBusinessNumberCursorCodec", "customerQueryDataSource", "customerEvidenceReader"))
                assertThat(context.getBeanFactory().containsSingleton(bean)).as(bean).isFalse();
            verify(service, times(2)).getCustomerEvidence(any(), eq(EVIDENCE));
            verifyNoMoreInteractions(service);
        }
    }

    @Test
    void missingCredentialsReadersAndCursorKeysFailClosedWithoutFallback() throws Exception {
        try (var context = graph(true, "web", true)) {
            problem(get(context, LIST, "opaque"), 503, "SERVICE_UNAVAILABLE");
            var properties = new CustomerEvidenceCursorProperties();
            properties.setCursorHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
            String cursor = new CustomerEvidenceCursorCodec(properties).encode(
                    new com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope(ACCOUNT, null),
                    new CustomerEvidenceReader.Anchor(java.time.Instant.parse("2026-01-01T00:00:00Z"), EVIDENCE));
            problem(get(context, LIST + "?cursor=" + cursor, "opaque"), 503, "SERVICE_UNAVAILABLE");
            assertThat(context.getBeanFactory().containsSingleton("customerEvidenceQueryService")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
            problem(get(context, LIST + "/" + EVIDENCE, "opaque"), 503, "SERVICE_UNAVAILABLE");
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
        }
        try (var context = graph(true, "web", false)) {
            problem(get(context, LIST + "/bad", "opaque"), 503, "SERVICE_UNAVAILABLE");
            for (String bean : LAZY) assertThat(context.getBeanFactory().containsSingleton(bean)).as(bean).isFalse();
        }
        for (String key : List.of("", "CANARY", Base64.getEncoder().encodeToString(new byte[31]), Base64.getEncoder().encodeToString(new byte[33]))) {
            var properties = new CustomerEvidenceCursorProperties(); properties.setCursorHmacKey(key);
            assertThatThrownBy(() -> new CustomerEvidenceCursorCodec(properties)).isInstanceOfSatisfying(CustomerContextException.class,
                    failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.SERVICE_UNAVAILABLE));
        }
        var factory = new DefaultListableBeanFactory();
        var controller = new CustomerEvidenceController(factory.getBeanProvider(CustomerEvidenceQueryService.class),
                factory.getBeanProvider(CustomerEvidenceCursorCodec.class), new CustomerProblemWriter());
        var principal = new CustomerRequestPrincipal(new TrustedCustomerCaller(ACCOUNT, TrustedCustomerCaller.Channel.WEB,
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
        assertThatThrownBy(() -> controller.getCustomerEvidence(principal, EVIDENCE.toString(), new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isInstanceOfSatisfying(CustomerContextException.class, failure -> assertThat(failure.code()).isEqualTo(CustomerContextException.Code.SERVICE_UNAVAILABLE));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("web");
            context.registerBean("customerQueryDsl", DSLContext.class, () -> mock(DSLContext.class));
            context.registerBean("customerQueryTransactionManager", DataSourceTransactionManager.class, () -> mock(DataSourceTransactionManager.class));
            context.register(CustomerPersistenceConfiguration.class); context.refresh();
            assertThatThrownBy(() -> context.getBean("customerEvidenceQueryService"))
                    .isInstanceOf(org.springframework.beans.BeansException.class);
            assertThat(context.getBeanFactory().containsSingleton("customerEvidenceQueryService")).isFalse();
        }
    }
}
