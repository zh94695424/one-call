package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.onecall.archtests.operations.OperationsActivationReadTestProviders.*;
import com.onecall.archtests.operations.OperationsActivationReadTestProviders;
import com.onecall.core.activation_consent.internal.operationsread.JooqOperationsActivationHistoryReader;
import com.onecall.core.operations_iam.internal.activationread.*;
import com.onecall.core.operations_iam.internal.subjectread.*;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;

class OperationsActivationReadConfigurationTests {
    private AnnotationConfigApplicationContext context(String profile, boolean enabled, Map<String, Object> properties) {
        var context = new AnnotationConfigApplicationContext(); context.getEnvironment().setActiveProfiles(profile);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("enabled", Map.of("onecall.operations-subject-read.enabled", "" + enabled)));
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("keys", properties));
        context.register(OperationsSubjectReadPersistenceConfiguration.class, OperationsActivationReadConfiguration.class, JooqOperationsActivationHistoryReader.class); context.refresh(); return context;
    }
    private void unavailable(Runnable operation) { assertThatThrownBy(operation::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE)); }
    private Map<String, Object> keys() {
        return Map.of("onecall.operations-subject-read.cursor.active-key-id", "Test-34", "onecall.operations-subject-read.cursor.keys[0].id", "Test-34", "onecall.operations-subject-read.cursor.keys[0].material", properties().cursor().keys().getFirst().material());
    }
    @Test
    void defaultWebAndDisabledProfilesCreateNoActivationReadResources() {
        for (String profile : List.of("default", "web", "persistence")) for (boolean enabled : List.of(false, true)) {
            if (enabled && "persistence".equals(profile)) continue;
            try (var context = context(profile, enabled, Map.of())) { assertThat(context.getBeanNamesForType(OperationsActivationReadService.class)).isEmpty(); assertThat(context.getBeanNamesForType(ActivationHistoryReader.class)).isEmpty(); assertThat(context.getBeanNamesForType(javax.sql.DataSource.class)).isEmpty(); }
        }
        for (Map<String, Object> configured : List.of(
                Map.<String, Object>of(),
                Map.<String, Object>of("spring.web.error.path", "/internal/failure-34"),
                Map.<String, Object>of("spring.web.error.path", "/internal/failure-34", "server.error.path", "/legacy/failure-34"),
                Map.<String, Object>of("server.error.path", "/legacy/failure-34"),
                Map.<String, Object>of("server.error.path", "legacy-invalid-secret-34"))) {
            try (var context = new org.springframework.web.context.support.AnnotationConfigWebApplicationContext()) {
                context.setServletContext(new org.springframework.mock.web.MockServletContext()); context.getEnvironment().setActiveProfiles("web");
                context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("error-path", configured));
                context.register(com.onecall.core.operations_iam.internal.web.OperationsWebConfiguration.class); context.refresh();
                String expected = (String) configured.getOrDefault("spring.web.error.path", "/error");
                org.springframework.boot.web.servlet.FilterRegistrationBean<?> errors = context.getBean("operationsErrorBoundaryFilterRegistration", org.springframework.boot.web.servlet.FilterRegistrationBean.class);
                assertThat(errors.getUrlPatterns()).containsExactly(expected); assertThat(errors.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
                var servlet = mock(jakarta.servlet.ServletContext.class); var mapping = mock(jakarta.servlet.FilterRegistration.Dynamic.class);
                when(servlet.addFilter(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(jakarta.servlet.Filter.class))).thenReturn(mapping);
                errors.onStartup(servlet);
                verify(mapping).addMappingForUrlPatterns(EnumSet.of(jakarta.servlet.DispatcherType.ERROR), false, expected);
                org.springframework.boot.web.servlet.FilterRegistrationBean<?> requests = context.getBean("operationsBoundaryFilterRegistration", org.springframework.boot.web.servlet.FilterRegistrationBean.class);
                assertThat(requests.getUrlPatterns()).containsExactly("/api/v1/operations/*"); assertThat(requests.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
                clearInvocations(mapping); requests.onStartup(servlet);
                verify(mapping).addMappingForUrlPatterns(EnumSet.of(jakarta.servlet.DispatcherType.REQUEST), false, "/api/v1/operations/*");
            } catch (jakarta.servlet.ServletException failure) { throw new AssertionError(failure); }
        }
        for (String malformed : List.of("", "relative", "//error", "/error/*", "/error?x", "/error#x", "/error%2f", "/../error", "/error/./nested", "/error;path", "/error\\path", "/error\n")) {
            try (var context = new org.springframework.web.context.support.AnnotationConfigWebApplicationContext()) {
                context.setServletContext(new org.springframework.mock.web.MockServletContext()); context.getEnvironment().setActiveProfiles("web");
                context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("error-path", Map.of("spring.web.error.path", malformed, "server.error.path", "/legacy/fallback-34")));
                context.register(com.onecall.core.operations_iam.internal.web.OperationsWebConfiguration.class);
                assertThatThrownBy(context::refresh).hasRootCauseInstanceOf(IllegalStateException.class)
                        .rootCause().hasMessage("Invalid error dispatch path");
                assertThat(context.getBeanFactory().containsSingleton("operationsErrorBoundaryFilterRegistration")).isFalse();
            }
        }
    }
    @Test
    void enabledPersistenceGraphIsLazyAndReusesQualifiedReadInfrastructure() {
        try (var context = context("persistence", true, keys())) {
            for (String name : List.of("operationsActivationReadService", "operationsActivationHistoryReader", "operationsSubjectReadDataSource", "operationsSubjectReadDsl", "operationsSubjectReadContext")) { assertThat(context.getBeanFactory().getBeanDefinition(name).isLazyInit()).isTrue(); assertThat(context.getBeanFactory().containsSingleton(name)).isFalse(); }
            var datasource = mock(javax.sql.DataSource.class);
            context.getBeanFactory().registerSingleton("operationsSubjectReadDataSource", datasource);
            context.getBeanFactory().registerSingleton("unrelatedDataSource", mock(javax.sql.DataSource.class));
            var manager = context.getBean("operationsSubjectReadTransactionManager", org.springframework.jdbc.datasource.DataSourceTransactionManager.class);
            assertThat(manager.getDataSource()).isSameAs(datasource);
            assertThat(manager.isRollbackOnCommitFailure()).isTrue();
            assertThat(manager.isEnforceReadOnly()).isTrue();
            var transaction = context.getBean("operationsSubjectReadTransactionTemplate", org.springframework.transaction.support.TransactionTemplate.class);
            assertThat(transaction.getTransactionManager()).isSameAs(manager);
            assertThat(transaction.isReadOnly()).isTrue();
            assertThat(transaction.getIsolationLevel()).isEqualTo(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
            assertThat(transaction.getPropagationBehavior()).isEqualTo(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThat(transaction.getTimeout()).isEqualTo(5);
            assertThat(context.getBean("operationsSubjectReadDataSource")).isSameAs(datasource);
            verifyNoInteractions(datasource);
        }
        var method = Arrays.stream(OperationsActivationReadConfiguration.class.getDeclaredMethods()).filter(candidate -> candidate.getName().equals("operationsActivationReadService")).findFirst().orElseThrow();
        assertThat(Arrays.stream(method.getParameters()).limit(3).map(parameter -> parameter.getAnnotation(Qualifier.class).value())).containsExactly("operationsSubjectReadTransactionTemplate", "operationsSubjectReadDsl", "operationsSubjectReadContext");
    }
    @Test
    void activationKeysUseOnlyExactEndpointSpecificProperties() {
        try (var context = context("persistence", true, keys())) { assertThat(new OperationsActivationCursorCodec(context.getBean(OperationsSubjectReadProperties.class))).isNotNull(); }
        try (var context = context("persistence", true, Map.of("onecall.operations-activation-read.cursor.active-key-id", "Test-34"))) { unavailable(() -> new OperationsActivationCursorCodec(context.getBean(OperationsSubjectReadProperties.class))); }
        var settings = new HashMap<>(keys()); byte[] otherMaterial = new byte[32]; Arrays.fill(otherMaterial, (byte) 9);
        var otherKey = new OperationsSubjectReadProperties.KeyEntry("Activation-34", Base64.getUrlEncoder().withoutPadding().encodeToString(otherMaterial));
        settings.put("onecall.operations-activation-read.cursor.active-key-id", otherKey.id());
        settings.put("onecall.operations-activation-read.cursor.keys[0].id", otherKey.id());
        settings.put("onecall.operations-activation-read.cursor.keys[0].material", otherKey.material());
        var expectedAnchor = new Anchor(TIME, EMPLOYEE); var sharedCodec = new OperationsActivationCursorCodec(properties());
        var activationOnlyCodec = new OperationsActivationCursorCodec(new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor(otherKey.id(), List.of(otherKey))));
        try (var context = context("persistence", true, settings)) {
            assertThat(context.getBeanNamesForType(OperationsSubjectReadProperties.class)).containsExactly("operationsSubjectReadProperties");
            var configured = context.getBean(OperationsSubjectReadProperties.class);
            assertThat(configured.cursor()).isEqualTo(properties().cursor());
            var configuredCodec = new OperationsActivationCursorCodec(configured);
            assertThat(sharedCodec.decode(configuredCodec.encode(expectedAnchor))).isEqualTo(expectedAnchor);
            assertThat(configuredCodec.decode(sharedCodec.encode(expectedAnchor))).isEqualTo(expectedAnchor);
            assertThatThrownBy(() -> configuredCodec.decode(activationOnlyCodec.encode(expectedAnchor))).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.CURSOR_INVALID));
        }
        settings.put("onecall.operations-subject-read.cursor.active-key-id", "Missing-34");
        try (var context = context("persistence", true, settings)) { unavailable(() -> new OperationsActivationCursorCodec(context.getBean(OperationsSubjectReadProperties.class))); }
        var anchor = new Anchor(TIME, EMPLOYEE); var codec = new OperationsActivationCursorCodec(properties()); var subject = new OperationsSubjectCursorCodec(properties());
        assertThatThrownBy(() -> subject.decode(codec.encode(anchor), new byte[32])).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.CURSOR_INVALID));
        assertThatThrownBy(() -> codec.decode(subject.encode(new OperationsSubjectReadPorts.Anchor(TIME, EMPLOYEE), new byte[32]))).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.CURSOR_INVALID));
    }
    @Test
    void malformedIndexedKeyPropertiesFailClosedWithoutFallback() {
        for (String malformed : List.of("keys[1].id", "keys[3].id", "keys[0].unknown", "keys[00].id", "keys[0].material")) {
            var settings = new HashMap<>(keys()); settings.put("onecall.operations-subject-read.cursor." + malformed, "PRIVATE");
            try (var context = context("persistence", true, settings)) { unavailable(() -> new OperationsActivationCursorCodec(context.getBean(OperationsSubjectReadProperties.class))); }
        }
    }
    @Test
    void missingInvalidAndAmbiguousActivationDependenciesAreUnavailable() throws Exception {
        for (String dependency : List.of("properties", "reader")) for (int count : List.of(0, 2, 3)) {
            var fixture = new OperationsActivationReadTestProviders(); Object original = fixture.beans.getBean(dependency); fixture.beans.destroySingleton(dependency);
            if (count == 2) { fixture.beans.registerSingleton("first", original); fixture.beans.registerSingleton("second", original); }
            if (count == 3) {
                var definition = new org.springframework.beans.factory.support.RootBeanDefinition("properties".equals(dependency) ? OperationsSubjectReadProperties.class : ActivationHistoryReader.class);
                definition.setInstanceSupplier(() -> { throw new IllegalStateException("PRIVATE"); }); fixture.beans.registerBeanDefinition("broken", definition);
            }
            unavailable(() -> fixture.service.list(fixture.caller, raw(Map.of())));
        }
    }
    @Test
    void propertiesKeysRequestsAndCodecsRedactToString() {
        for (Object value : List.of(properties(), properties().cursor(), properties().cursor().keys().getFirst(), new OperationsSubjectReadProperties.Datasource("PRIVATE", "PRIVATE", "PRIVATE"), raw(Map.of("cursor", List.of("PRIVATE"))), new OperationsActivationReadRequest(20, "PRIVATE"), new Anchor(TIME, EMPLOYEE), fact(1), new OperationsActivationCursorCodec(properties()))) assertThat(value.toString()).contains("REDACTED").doesNotContain("PRIVATE", EMPLOYEE.toString(), TIME.toString(), properties().cursor().keys().getFirst().material());
    }
    @Test
    void ownerReaderUsesOnlyExistingActivationReadRoleAndPort() throws Exception {
        var owner = JooqOperationsActivationHistoryReader.class; assertThat(owner.getInterfaces()).containsExactly(ActivationHistoryReader.class); assertThat(owner.getAnnotation(Lazy.class)).isNotNull(); assertThat(owner.getAnnotation(Profile.class).value()).containsExactly("persistence");
        assertThat(Arrays.stream(owner.getConstructors()[0].getParameters()).map(parameter -> parameter.getAnnotation(Qualifier.class).value())).containsExactly("operationsSubjectReadDsl", "operationsSubjectReadContext");
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/activation_consent/internal/operationsread/JooqOperationsActivationHistoryReader.java")); assertThat(source).contains("@Component(\"operationsActivationHistoryReader\")", "context.scoped(Role.ACTIVATION").doesNotContain("GRANT ", "CREATE ROLE", "@Bean");
    }
    @Test
    void configurationAddsNoDependencyPoolOrProductionVerifier() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/operations_iam/internal/activationread/OperationsActivationReadConfiguration.java")); assertThat(source).doesNotContain("DataSource", "EmployeeSessionVerifier", "new OperationsSubjectReadProperties", "JooqOperationsActivationHistoryReader");
        assertThat(Arrays.stream(OperationsActivationReadConfiguration.class.getDeclaredMethods()).filter(method -> method.isAnnotationPresent(Bean.class)).map(java.lang.reflect.Method::getName)).containsExactly("operationsActivationReadService");
        assertThat(Files.readString(Path.of("src/main/java/com/onecall/core/operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java"))).contains("setReadOnly(true)", "ISOLATION_REPEATABLE_READ", "PROPAGATION_REQUIRES_NEW", "setTimeout(5)");
    }
}
