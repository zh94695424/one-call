package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.OneCallApplication;
import com.onecall.core.customer_iam.internal.persistence.CustomerPersistenceConfiguration;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier;
import com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel;
import com.onecall.core.customer_iam.internal.web.CustomerApiErrorController;
import com.onecall.core.customer_iam.internal.web.CustomerBoundaryFilter;
import com.onecall.core.customer_iam.internal.web.CustomerMeController;
import com.onecall.core.customer_iam.internal.web.CustomerProblemWriter;
import com.onecall.core.sharing.internal.persistence.SharingPersistenceConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

class CustomerWebConfigurationTests {
    @TestConfiguration(proxyBeanMethods = false)
    static class VerifierBeans {
        @Bean
        CustomerSessionVerifier customerSessionVerifier() {
            return (bearer, audience) -> new VerifiedCustomerSession(UUID.randomUUID(), Channel.WEB, audience);
        }
    }

    private ConfigurableApplicationContext start(String profiles, boolean verifier, Map<String, Object> properties) {
        var builder = new SpringApplicationBuilder(OneCallApplication.class).profiles(profiles.split(","))
                .properties("server.port=0", "spring.main.banner-mode=off")
                .initializers(context -> context.getEnvironment().getPropertySources()
                        .addFirst(new MapPropertySource("customer-test", properties)));
        if (verifier) { builder.sources(VerifierBeans.class); }
        return builder.run();
    }

    private HttpResponse<String> get(ConfigurableApplicationContext context, boolean bearer) throws Exception {
        int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/customer/me"))
                    .timeout(Duration.ofSeconds(30));
            if (bearer) { request.header("Authorization", "Bearer configuration-probe"); }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private HttpResponse<String> getShares(ConfigurableApplicationContext context) throws Exception {
        int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/customer/shares"))
                    .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer configuration-probe").GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void defaultRuntimeHasNoCustomerOrServletBeans() {
        try (var context = start("default", false, Map.of("ONECALL_CUSTOMER_DB_URL", "jdbc:postgresql://invalid/test",
                "ONECALL_CUSTOMER_DB_USERNAME", "CANARY", "ONECALL_CUSTOMER_DB_PASSWORD", "CANARY"))) {
            assertThat(context).isNotInstanceOf(ServletWebServerApplicationContext.class);
            for (String name : List.of("customerQueryDataSource", "customerContextService", "customerMeController",
                    "customerBoundaryFilterRegistration", "customerPersistenceConfiguration")) {
                assertThat(context.containsBean(name)).as(name).isFalse();
            }
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("sharingQueryDataSource")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("customerQueryTransactionManager")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("sharingQueryTransactionManager")).isFalse();
            assertThat(context.getBeansOfType(CustomerSessionVerifier.class)).isEmpty();
        }
    }

    @Test
    void webProfileIsExplicitAndMissingAdapterHasNoDatabaseEffects() throws Exception {
        try (var context = start("web", false, Map.of())) {
            assertThat(get(context, false).statusCode()).isEqualTo(401);
            assertThat(get(context, true).statusCode()).isEqualTo(503);
            assertThat(getShares(context).statusCode()).isEqualTo(503);
            for (String name : List.of("customerContextService", "customerContextRepository", "customerQueryDataSource")) {
                assertThat(context.getBeanFactory().containsSingleton(name)).as(name).isFalse();
                assertThat(context.containsBeanDefinition(name)).isTrue();
            }
            assertThat(context.getBeansOfType(CustomerProblemWriter.class)).hasSize(1);
            assertThat(context.getBeansOfType(CustomerApiErrorController.class)).hasSize(1);
            assertThat(context.getBeansOfType(CustomerMeController.class)).hasSize(1);
            assertThat(context.getBeansOfType(FilterRegistrationBean.class).values().stream()
                    .filter(registration -> registration.getFilter() instanceof CustomerBoundaryFilter)).hasSize(1);
            assertThat(context.containsBean("customerApiErrorController")).isTrue();
            assertThat(context.containsBean("basicErrorController")).isFalse();
            assertThat(context.getBeansOfType(CustomerSessionVerifier.class)).isEmpty();
            assertThat(context.getBeansOfType(com.onecall.core.sharing.internal.application.CustomerShareQueryService.class)).isEmpty();
            assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("sharingQueryDataSource")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("customerQueryTransactionManager")).isFalse();
            assertThat(context.getBeanFactory().containsSingleton("sharingQueryTransactionManager")).isFalse();
            var mappings = context.getBean(RequestMappingHandlerMapping.class).getHandlerMethods();
            assertThat(mappings.entrySet().stream().filter(entry -> entry.getKey().getMethodsCondition().getMethods().contains(RequestMethod.GET)
                    && entry.getKey().getPatternValues().contains("/api/v1/customer/me"))).hasSize(1);
            assertThat(mappings.entrySet().stream().filter(entry -> entry.getKey().getPatternValues().contains("/error"))).hasSize(1);
            assertThat(mappings.values()).noneMatch(method -> method.getBeanType().equals(
                    org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController.class));
        }
    }

    @Test
    void customerCredentialsAreExplicitAndValidatedBeforeConnection() throws Exception {
        for (String missing : List.of("url", "username", "password")) {
            for (String blank : List.of("", " ")) {
                var properties = new java.util.HashMap<String, Object>();
                for (String name : List.of("url", "username", "password")) {
                    properties.put("onecall.customer.persistence." + name, name.equals(missing) ? blank
                            : name.equals("url") ? "jdbc:postgresql://invalid/test" : "CREDENTIAL_CANARY");
                }
                try (var context = start("web", true, properties)) {
                    var response = get(context, true);
                    assertThat(response.statusCode()).isEqualTo(503);
                    assertThat(response.body()).contains("SERVICE_UNAVAILABLE").doesNotContain("CANARY", "invalid", "jdbc:");
                    assertThat(context.getBeanFactory().containsSingleton("customerQueryDataSource")).isFalse();
                }
            }
        }
        try (var context = start("web", true, Map.of())) {
            assertThat(get(context, true).statusCode()).isEqualTo(503);
        }
    }

    @Test
    void customerAndSharingGraphsUseExactQualifiers() {
        for (Class<?> configuration : List.of(CustomerPersistenceConfiguration.class, SharingPersistenceConfiguration.class,
                com.onecall.core.sharing.internal.persistence.SharingCommandPersistenceConfiguration.class)) {
            Map<String, List<String>> expected = configuration == CustomerPersistenceConfiguration.class
                    ? Map.of("customerQueryDataSource", List.of(), "customerQueryTransactionManager", List.of("customerQueryDataSource"),
                            "customerQueryTransactionTemplate", List.of("customerQueryTransactionManager"),
                            "customerQueryDsl", List.of("customerQueryDataSource"), "customerContextRepository", List.of("customerQueryDsl"),
                            "customerContextService", List.of("customerQueryTransactionTemplate", "customerQueryDsl", "customerContextRepository"))
                    : configuration == SharingPersistenceConfiguration.class
                    ? Map.of("sharingQueryDataSource", List.of(), "sharingQueryTransactionManager", List.of("sharingQueryDataSource"),
                            "sharingQueryTransactionTemplate", List.of("sharingQueryTransactionManager"),
                            "sharingQueryDsl", List.of("sharingQueryDataSource"), "sharingQueryRepository", List.of("sharingQueryDsl"),
                            "customerShareQueryService", List.of("sharingQueryTransactionTemplate", "sharingQueryDsl", "sharingQueryRepository"))
                    : Map.of("sharingCommandDataSource", List.of(), "sharingCommandTransactionManager", List.of("sharingCommandDataSource"),
                            "sharingCommandTransactionTemplate", List.of("sharingCommandTransactionManager"),
                            "sharingCommandDsl", List.of("sharingCommandDataSource"), "sharingCommandRepository", List.of("sharingCommandDsl"),
                            "customerShareCommandService", List.of("sharingCommandTransactionTemplate", "sharingCommandDsl", "sharingCommandRepository",
                                    "shareableEvidenceProjectionReader", "shareCreationPolicyProvider", "shareRotationPolicyProvider", "capabilityTokenProvider",
                                    "shareExtractionCodeProvider", "designatedMobileBindingProvider", "sha256Provider"));
            for (var method : configuration.getDeclaredMethods()) {
                if (configuration == com.onecall.core.sharing.internal.persistence.SharingCommandPersistenceConfiguration.class
                        && !expected.containsKey(method.getName())) continue;
                assertThat(method.isAnnotationPresent(org.springframework.context.annotation.Primary.class)).isFalse();
                if (configuration == CustomerPersistenceConfiguration.class) { assertThat(method.isAnnotationPresent(Lazy.class)).isTrue(); }
                var expectedQualifiers = expected.get(method.getName());
                int qualifierIndex = 0;
                for (var parameter : method.getParameters()) {
                    if (parameter.getType().getSimpleName().endsWith("Properties")) { continue; }
                    var qualifier = parameter.getAnnotation(Qualifier.class);
                    assertThat(qualifier).as(method.getName()).isNotNull();
                    assertThat(qualifier.value()).isEqualTo(expectedQualifiers.get(qualifierIndex++));
                }
                assertThat(qualifierIndex).isEqualTo(expectedQualifiers.size());
            }
            assertThat(expected.keySet()).containsExactlyInAnyOrderElementsOf(Arrays.stream(configuration.getDeclaredMethods())
                    .filter(method -> configuration != com.onecall.core.sharing.internal.persistence.SharingCommandPersistenceConfiguration.class
                            || expected.containsKey(method.getName()))
                    .map(java.lang.reflect.Method::getName).toList());
            assertThat(configuration.getAnnotation(org.springframework.context.annotation.Profile.class).value())
                    .containsExactly(configuration == CustomerPersistenceConfiguration.class ? "web" : "persistence");
        }
        var parameters = com.onecall.core.customer_iam.internal.web.CustomerSharesController.class.getDeclaredConstructors()[0].getParameters();
        assertThat(parameters).hasSize(3);
        for (var entry : Map.of("CustomerShareQueryService", "customerShareQueryService", "CustomerShareCommandService", "customerShareCommandService").entrySet()) {
            var serviceParameter = Arrays.stream(parameters).filter(parameter -> parameter.getParameterizedType().getTypeName()
                    .equals("org.springframework.beans.factory.ObjectProvider<com.onecall.core.sharing.internal."
                            + (entry.getKey().equals("CustomerShareQueryService") ? "application." : "command.") + entry.getKey() + ">"))
                    .findFirst().orElseThrow();
            assertThat(serviceParameter.getAnnotation(Qualifier.class).value()).isEqualTo(entry.getValue());
        }
        var condition = com.onecall.core.sharing.internal.persistence.SharingCommandPersistenceConfiguration.class
                .getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class);
        assertThat(condition.prefix()).isEqualTo("onecall.sharing-command");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(parameters[0].getAnnotation(Qualifier.class).value()).isEqualTo("customerShareQueryService");
        assertThat(parameters[1].getAnnotation(Qualifier.class).value()).isEqualTo("customerShareCommandService");
    }

    @Test
    void testProfileAndProductClasspathCannotEnableVerifierBypass() {
        for (String profiles : List.of("test", "dev", "web,test", "web,dev")) {
            try (var context = start(profiles, false, Map.of())) {
                assertThat(context.getBeansOfType(CustomerSessionVerifier.class)).isEmpty();
                assertThat(context.containsBean("springSecurityFilterChain")).isFalse();
            }
        }
    }

    @Test
    void webSettingsDisableUnsafeLoggingAndSessionBehavior() throws Exception {
        String web = Files.readString(Path.of("src/main/resources/application-web.yml"));
        assertThat(web).contains("web-application-type: servlet", "${ONECALL_CUSTOMER_DB_URL:}",
                "${ONECALL_CUSTOMER_DB_USERNAME:}", "${ONECALL_CUSTOMER_DB_PASSWORD:}", "log-request-details: false",
                "org.jooq: 'OFF'", "com.zaxxer.hikari: 'OFF'", "org.springframework.transaction: 'OFF'",
                "org.springframework.jdbc: 'OFF'", "org.springframework.web: WARN", "enabled: false");
        assertThat(web).doesNotContain("session", "cookie", "redis", "flyway", "default-property-inclusion", "maximum-pool", "connection-timeout");
        assertThat(Files.readString(Path.of("src/main/resources/application.yml"))).contains("web-application-type: none");
        assertThat(Arrays.stream(com.onecall.core.customer_iam.internal.persistence.CustomerPersistenceProperties.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)).containsExactly("url", "username", "password");
    }
}
