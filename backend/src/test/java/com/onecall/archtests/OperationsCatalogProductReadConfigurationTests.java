package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders.*;
import com.onecall.archtests.operations.OperationsCatalogProductReadTestProviders;
import com.onecall.core.catalog_order.internal.operationsread.JooqOperationsCatalogProductReader;
import com.onecall.core.operations_iam.internal.catalogread.*;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadPorts.CatalogProductReader;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;

class OperationsCatalogProductReadConfigurationTests {
    private AnnotationConfigApplicationContext context(String profile, boolean enabled) {
        var context = new AnnotationConfigApplicationContext(); context.getEnvironment().setActiveProfiles(profile);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of("onecall.operations-subject-read.enabled", enabled)));
        context.register(OperationsCatalogProductReadConfiguration.class, JooqOperationsCatalogProductReader.class); context.refresh(); return context;
    }
    private java.lang.reflect.Method factory() { return Arrays.stream(OperationsCatalogProductReadConfiguration.class.getDeclaredMethods()).filter(method -> method.getName().equals("operationsCatalogProductReadService")).findFirst().orElseThrow(); }
    private void unavailable(Runnable action) { assertThatThrownBy(action::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE)); }
    @Test void reusesSharedDatasource() throws Exception {
        var fixture = new OperationsCatalogProductReadTestProviders(); assertThat(((org.springframework.jdbc.datasource.DataSourceTransactionManager) fixture.transaction.getTransactionManager()).getDataSource()).isSameAs(fixture.source);
        assertThat(Arrays.stream(JooqOperationsCatalogProductReader.class.getConstructors()[0].getParameters()).map(parameter -> parameter.getAnnotation(Qualifier.class).value())).containsExactly("operationsSubjectReadDsl", "operationsSubjectReadContext");
    }
    @Test void reusesSharedTransactionTemplate() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); assertThat(factory().getParameters()[0].getAnnotation(Qualifier.class).value()).isEqualTo("operationsSubjectReadTransactionTemplate"); assertThat(fixture.transaction.isReadOnly()).isTrue(); assertThat(fixture.transaction.getIsolationLevel()).isEqualTo(4); assertThat(fixture.transaction.getPropagationBehavior()).isEqualTo(3); }
    @Test void reusesOrderRoleContext() throws Exception { assertThat(factory().getParameters()[2].getAnnotation(Qualifier.class).value()).isEqualTo("operationsSubjectReadContext"); assertThat(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readString(Path.of("src/main/java/com/onecall/core/operations_iam/subjectread/OperationsReadContext.java")).replace("\r\n", "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)))).isEqualTo("734e82e2bfc1f33a1d0c2e3f1cce4cf634bf15796503f82338253e99aff4289d"); assertThat(OperationsReadContext.Role.valueOf("ORDER")).isNotNull(); }
    @Test void reusesSharedCursorKeyRing() throws Exception { var fixture = new OperationsCatalogProductReadTestProviders(); assertThat(factory().getParameters()[3].getParameterizedType().getTypeName()).contains("OperationsSubjectReadProperties"); assertThat(fixture.service.list(fixture.caller, raw(Map.of())).items()).hasSize(1); }
    @Test void resolvesProvidersLazily() {
        for (String profile : List.of("persistence", "web")) for (boolean enabled : List.of(true, false)) try (var context = context(profile, enabled)) {
            boolean present = enabled && profile.equals("persistence");
            assertThat(context.containsBeanDefinition("operationsCatalogProductReader")).isEqualTo(present); assertThat(context.containsBeanDefinition("operationsCatalogProductReadService")).isEqualTo(present);
            if (present) { assertThat(context.getBeanFactory().containsSingleton("operationsCatalogProductReader")).isFalse(); assertThat(context.getBeanFactory().containsSingleton("operationsCatalogProductReadService")).isFalse(); }
        }
        assertThat(OperationsCatalogProductReadConfiguration.class.getAnnotation(Lazy.class).value()).isTrue(); assertThat(factory().getAnnotation(Lazy.class).value()).isTrue();
    }
    @Test void rejectsMissingProvider() throws Exception {
        for (String name : List.of("properties", "reader")) { var fixture = new OperationsCatalogProductReadTestProviders(); fixture.beans.destroySingleton(name); unavailable(() -> fixture.service.list(fixture.caller, raw(Map.of()))); }
    }
    @Test void rejectsAmbiguousProvider() throws Exception {
        for (String name : List.of("properties", "reader")) { var fixture = new OperationsCatalogProductReadTestProviders(); Object original = fixture.beans.getBean(name); fixture.beans.registerSingleton("duplicate", original); unavailable(() -> fixture.service.list(fixture.caller, raw(Map.of()))); }
    }
    @Test void doesNotConstructNewResources() throws Exception {
        assertThat(OperationsCatalogProductReadConfiguration.class.getDeclaredMethods()).hasSize(1);
        try (var context = context("persistence", true)) {
            var fixture = new OperationsCatalogProductReadTestProviders(); var beans = context.getBeanFactory();
            beans.registerSingleton("operationsSubjectReadTransactionTemplate", fixture.transaction); beans.registerSingleton("operationsSubjectReadDsl", fixture.dsl); beans.registerSingleton("operationsSubjectReadContext", fixture.scope);
            assertThat(context.getBean(OperationsCatalogProductReadService.class)).isNotNull(); verifyNoInteractions(fixture.dsl, fixture.scope, fixture.source);
        }
    }
}
