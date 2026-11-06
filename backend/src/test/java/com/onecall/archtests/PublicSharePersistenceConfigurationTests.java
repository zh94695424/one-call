package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.sharing.internal.persistence.PublicSharePersistenceConfiguration;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryService;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class PublicSharePersistenceConfigurationTests {
    @Test
    void defaultWebAndDisabledCommandProfilesStayDatabaseFree() {
        for (String profile : List.of("default", "web", "persistence")) {
            new ApplicationContextRunner().withUserConfiguration(PublicSharePersistenceConfiguration.class)
                    .withPropertyValues("spring.profiles.active=" + profile, "onecall.sharing-command.enabled=false")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBeansOfType(PublicShareQueryService.class)).isEmpty();
                        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
                        assertThat(context.getBeansOfType(TransactionTemplate.class)).isEmpty();
                    });
        }
    }

    @Test
    void publicReadUsesExistingQualifiedManagerAndEnforcedReadOnlyTemplate() throws Exception {
        var configuration = PublicSharePersistenceConfiguration.class;
        var method = configuration.getDeclaredMethod("publicShareQueryTransactionTemplate", DataSourceTransactionManager.class);
        method.setAccessible(true);
        var manager = mock(DataSourceTransactionManager.class);
        var template = (TransactionTemplate) method.invoke(new PublicSharePersistenceConfiguration(), manager);
        assertThat(template.getTransactionManager()).isSameAs(manager);
        assertThat(template.isReadOnly()).isTrue();
        assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThat(method.getParameters()[0].getAnnotation(Qualifier.class).value()).isEqualTo("sharingCommandTransactionManager");
        var service = configuration.getDeclaredMethods();
        assertThat(Arrays.stream(service).map(java.lang.reflect.Method::getName)).containsExactlyInAnyOrder(
                "publicShareQueryTransactionTemplate", "publicShareQueryRepository", "publicShareQueryService");
        var factory = Arrays.stream(service).filter(candidate -> candidate.getName().equals("publicShareQueryService")).findFirst().orElseThrow();
        assertThat(Arrays.stream(factory.getParameters()).map(parameter -> parameter.getAnnotation(Qualifier.class).value()))
                .containsExactly("publicShareQueryTransactionTemplate", "sharingCommandDsl", "publicShareQueryRepository", "shareableEvidenceProjectionReader", "sha256Provider");
        String source = Files.readString(Path.of("src/main/java/com/onecall/core/sharing/internal/publicquery/PublicShareQueryService.java"));
        assertThat(source.indexOf("context.execute(\"SET TRANSACTION READ ONLY\")")).isLessThan(source.indexOf("repository.findByTokenDigest"));
        assertThat(source).doesNotContain("READ WRITE", "new Hikari", "Logger", "System.out");
        var reader = com.onecall.core.evidence.internal.shareprojection.JooqShareableEvidenceProjectionReader.class
                .getMethod("readShareableProjection", org.jooq.DSLContext.class, java.util.UUID.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(reader.propagation()).isEqualTo(org.springframework.transaction.annotation.Propagation.MANDATORY);
        assertThat(reader.transactionManager()).isEqualTo("sharingCommandTransactionManager");
        String command = Files.readString(Path.of("src/main/java/com/onecall/core/sharing/internal/persistence/SharingCommandPersistenceConfiguration.java"));
        assertThat(command).contains("template.setReadOnly(false)", "StatementType.PREPARED_STATEMENT", "ParamType.INDEXED", "withExecuteLogging(false)");
    }
}
