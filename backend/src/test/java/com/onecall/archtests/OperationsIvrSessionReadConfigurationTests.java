package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders.*;
import com.onecall.archtests.operations.OperationsIvrSessionReadTestProviders;
import com.onecall.core.operations_iam.internal.ivrread.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import com.onecall.core.operations_iam.internal.subjectread.*;
import com.onecall.core.ivr.internal.operationsread.JooqOperationsIvrSessionReader;
import com.onecall.core.evidence.internal.operationsread.JooqOperationsIvrEvidenceReader;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;


class OperationsIvrSessionReadConfigurationTests {
    private AnnotationConfigApplicationContext context(String profile, boolean enabled) {
        var context = new AnnotationConfigApplicationContext(); context.getEnvironment().setActiveProfiles(profile);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("enabled", Map.of("onecall.operations-subject-read.enabled", "" + enabled)));
        context.register(OperationsSubjectReadPersistenceConfiguration.class, OperationsIvrSessionReadConfiguration.class, JooqOperationsIvrSessionReader.class, JooqOperationsIvrEvidenceReader.class);
        context.refresh(); return context;
    }
    private String source(String file) throws Exception { return Files.readString(Path.of("src/main/java/com/onecall/core/" + file)); }
    private void unavailable(Runnable operation) { assertThatThrownBy(operation::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE)); }
    @Test
    void defaultWebAndDisabledPersistenceCreateNoIvrReadResources() throws Exception {
        for (String profile : List.of("default", "web", "persistence")) for (boolean enabled : List.of(false, true)) { if (enabled && profile.equals("persistence")) continue; try (var context = context(profile, enabled)) { assertThat(context.getBeanNamesForType(OperationsIvrSessionReadService.class)).isEmpty(); assertThat(context.getBeanNamesForType(IvrSessionReader.class)).isEmpty(); assertThat(context.getBeanNamesForType(IvrEvidenceReader.class)).isEmpty(); assertThat(context.getBeanNamesForType(javax.sql.DataSource.class)).isEmpty(); } }
    }
    @Test
    void enabledGraphLazilyReusesAllQualifiedSubjectReadBeans() throws Exception {
        try (var context = context("persistence", true)) { var factory = context.getBeanFactory(); assertThat(factory.containsSingleton("operationsSubjectReadDataSource")).isFalse(); assertThat(factory.containsSingleton("operationsIvrSessionReadService")).isFalse(); assertThat(context.getBeanNamesForType(IvrSessionReader.class)).containsExactly("operationsIvrSessionReader"); assertThat(context.getBeanNamesForType(IvrEvidenceReader.class)).containsExactly("operationsIvrEvidenceReader"); }
        String source = source("operations_iam/internal/ivrread/OperationsIvrSessionReadConfiguration.java"); for (String qualifier : List.of("operationsSubjectReadTransactionTemplate", "operationsSubjectReadDsl", "operationsSubjectReadContext")) assertThat(source).contains("@Qualifier(\"" + qualifier + "\")");
    }
    @Test
    void serviceConfigurationCreatesNoPoolDslContextOrKeyRing() throws Exception {
        String text = source("operations_iam/internal/ivrread/OperationsIvrSessionReadConfiguration.java"); assertThat(text).doesNotContain("new Hikari", "new TransactionTemplate", "DSL.using", "new OperationsSubjectReadProperties"); assertThat(text).contains("new DefaultOperationsIvrSessionReadService", "ObjectProvider<IvrSessionReader>", "ObjectProvider<IvrEvidenceReader>");
    }
    @Test
    void transactionRetainsFiveSecondReadOnlyRepeatableReadSettings() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); assertThat(fixture.transaction.getTimeout()).isEqualTo(5); assertThat(fixture.transaction.isReadOnly()).isTrue(); assertThat(fixture.transaction.getIsolationLevel()).isEqualTo(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ); assertThat(fixture.transaction.getPropagationBehavior()).isEqualTo(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(source("operations_iam/internal/subjectread/OperationsSubjectReadPersistenceConfiguration.java")).contains("setMaximumPoolSize(4)", "setConnectionTimeout(3000)", "operations-subject-read"); assertThat(source("operations_iam/subjectread/OperationsReadContext.java")).contains("2000ms", "250ms", "5000ms");
    }
    @Test
    void onlyExistingSubjectReadPrefixConfiguresKeysAndCredentials() throws Exception {
        assertThat(OperationsSubjectReadProperties.class.getAnnotation(org.springframework.boot.context.properties.ConfigurationProperties.class).value()).isEqualTo("onecall.operations-subject-read"); for (String path : List.of("operations_iam/internal/ivrread/OperationsIvrSessionReadConfiguration.java", "ivr/internal/operationsread/JooqOperationsIvrSessionReader.java", "evidence/internal/operationsread/JooqOperationsIvrEvidenceReader.java")) { assertThat(source(path)).contains("onecall.operations-subject-read").doesNotContain("onecall.operations-ivr"); }
    }
    @Test
    void missingInvalidAmbiguousAndUncreatableResourcesFailClosed() throws Exception {
        for (Class<?> type : List.of(IvrSessionReader.class, IvrEvidenceReader.class)) for (int mode = 0; mode < 3; mode++) {
        var fixture = new OperationsIvrSessionReadTestProviders(); String name = type == IvrSessionReader.class ? "reader" : "evidenceReader";
        if (mode == 0) fixture.beans.destroySingleton(name);
        if (mode == 1) { var definition = new org.springframework.beans.factory.support.RootBeanDefinition(type); definition.setPrimary(true); definition.setInstanceSupplier(() -> mock(type)); fixture.beans.registerBeanDefinition("extra", definition); }
        if (mode == 2) { fixture.beans.destroySingleton(name); var definition = new org.springframework.beans.factory.support.RootBeanDefinition(type); definition.setInstanceSupplier(() -> { throw new IllegalStateException("PRIVATE"); }); fixture.beans.registerBeanDefinition(name, definition); }
        if (type == IvrSessionReader.class) unavailable(() -> fixture.service.list(fixture.caller, raw(Map.of()))); else unavailable(() -> fixture.service.detail(fixture.caller, new RawDetailRequest(fact(1).sessionId().toString(), Map.of(), List.of()))); }
        var fixture = new OperationsIvrSessionReadTestProviders(); fixture.beans.destroySingleton("properties"); unavailable(() -> fixture.service.list(fixture.caller, raw(Map.of())));
    }
    @Test
    void detailDoesNotResolveCursorKeyMaterial() throws Exception {
        var fixture = new OperationsIvrSessionReadTestProviders(); fixture.beans.destroySingleton("properties"); assertThat(fixture.service.detail(fixture.caller, new RawDetailRequest(fact(1).sessionId().toString(), Map.of(), List.of())).sessionId()).isEqualTo(fact(1).sessionId());
    }
    @Test
    void ownerReadersRegisterExactlyOnceWithoutReverseModuleEdges() throws Exception {
        assertThat(JooqOperationsIvrSessionReader.class.getInterfaces()).containsExactly(IvrSessionReader.class); assertThat(JooqOperationsIvrEvidenceReader.class.getInterfaces()).containsExactly(IvrEvidenceReader.class);
        var fixture = new OperationsIvrSessionReadTestProviders(); try (var context = new AnnotationConfigApplicationContext()) { context.registerBean("arbitraryIvr", IvrSessionReader.class, () -> fixture.reader); context.registerBean("arbitraryEvidence", IvrEvidenceReader.class, () -> fixture.evidence); context.registerBean("keys", OperationsSubjectReadProperties.class, () -> properties()); context.refresh();
        var service = new DefaultOperationsIvrSessionReadService(fixture.transaction, fixture.dsl, fixture.scope, context.getBeanProvider(OperationsSubjectReadProperties.class), context.getBeanProvider(IvrSessionReader.class), context.getBeanProvider(IvrEvidenceReader.class)); assertThat(service.list(fixture.caller, raw(Map.of())).items()).hasSize(1); assertThat(service.detail(fixture.caller, new RawDetailRequest(fact(1).sessionId().toString(), Map.of(), List.of())).call().status()).isEqualTo("ACTIVE"); }
        assertThat(source("operations_iam/internal/ivrread/OperationsIvrSessionReadConfiguration.java")).doesNotContain("JooqOperations", "@Qualifier(\"operationsIvr");
    }
    @Test
    void requestsPropertiesKeysAndFailuresHaveRedactedRepresentations() throws Exception {
        for (Object value : List.of(raw(Map.of("callId", List.of("PRIVATE"))), new RawDetailRequest("PRIVATE", Map.of(), List.of()), OperationsIvrSessionReadRequest.parse(raw(Map.of("callId", List.of("PRIVATE")))), properties(), properties().cursor(), properties().cursor().keys().getFirst(), new Failure(Code.INTERNAL))) assertThat(value.toString()).doesNotContain("PRIVATE", properties().cursor().keys().getFirst().material());
    }
    @Test
    void noProductionEmployeeVerifierOrNewDependencyIsRegistered() throws Exception {
        try (var paths = Files.walk(Path.of("src/main/java"))) { for (var path : paths.filter(Files::isRegularFile).toList()) assertThat(Files.readString(path)).doesNotContain("implements EmployeeSessionVerifier", "new SyntheticEmployeeSessions"); }
        assertThat(source("operations_iam/internal/ivrread/OperationsIvrSessionReadConfiguration.java")).doesNotContain("EmployeeSessionVerifier", "DataSource");
    }
}
