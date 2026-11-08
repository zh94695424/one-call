package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.evidence.internal.mediaread.EvidenceMediaReadPersistenceConfiguration;
import com.onecall.core.evidence.internal.mediaread.EvidenceMediaReadPersistenceProperties;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class EvidenceMediaReadPersistenceConfigurationTests {
    private static final String PREFIX = "onecall.evidence-media-read.datasource.";
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(EvidenceMediaReadPersistenceConfiguration.class);

    @Test
    void missingDisabledAndNonPersistenceProfilesAreInert() {
        for (String profile : List.of("default", "web", "persistence")) {
            for (String enablement : List.of("ABSENT", "false")) {
                var candidate = runner.withPropertyValues("spring.profiles.active=" + profile);
                if (!enablement.equals("ABSENT")) candidate = candidate.withPropertyValues(PREFIX + "enabled=" + enablement);
                candidate.run(context -> {
                    assertThat(context).hasNotFailed();
                    for (Class<?> type : List.of(DataSource.class, DSLContext.class, PlatformTransactionManager.class,
                            TransactionTemplate.class, EvidenceMediaObjectReader.class, EvidenceMediaReadPersistenceProperties.class)) {
                        assertThat(context.getBeansOfType(type)).isEmpty();
                    }
                });
            }
        }
        runner.withPropertyValues(PREFIX + "enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(EvidenceMediaObjectReader.class)).isEmpty();
        });
    }

    @Test
    void enabledInvalidCredentialsFailClosedWithSanitizedRootCause() {
        String[] valid = {"jdbc:postgresql://localhost:5432/synthetic", "evidence_media_reader_role", "synthetic-password"};
        List<String[]> invalid = new ArrayList<>();
        for (int index = 0; index < valid.length; index++) {
            for (String value : new String[] {null, "", " "}) {
                String[] candidate = valid.clone();
                candidate[index] = value;
                invalid.add(candidate);
            }
        }
        invalid.add(new String[] {valid[0], "evidence_owner", valid[2]});
        for (String url : List.of("jdbc:h2:mem:synthetic", "not-a-url", "jdbc:postgresql://localhost/",
                "jdbc:postgresql://user:secret@localhost/synthetic", "jdbc:postgresql://localhost/synthetic?password=secret")) {
            invalid.add(new String[] {url, valid[1], valid[2]});
        }
        for (String[] candidate : invalid) {
            var properties = new ArrayList<>(List.of("spring.profiles.active=persistence", PREFIX + "enabled=true"));
            String[] names = {"url", "username", "password"};
            for (int index = 0; index < names.length; index++) {
                if (candidate[index] != null) properties.add(PREFIX + names[index] + "=" + candidate[index]);
            }
            runner.withPropertyValues(properties.toArray(String[]::new)).run(context -> {
                assertThat(context).hasFailed();
                Throwable root = context.getStartupFailure();
                while (root.getCause() != null) root = root.getCause();
                assertThat(root).isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Valid explicit evidence media reader credentials are required").hasNoCause();
            });
        }
    }

    @Test
    void validExplicitCredentialsWireOnlyQualifiedReadOnlyResources() {
        runner.withPropertyValues("spring.profiles.active=persistence", PREFIX + "enabled=true",
                PREFIX + "url=jdbc:postgresql://localhost:5432/synthetic", PREFIX + "username=evidence_media_reader_role",
                PREFIX + "password=synthetic-password").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(EvidenceMediaObjectReader.class).hasSingleBean(DataSource.class)
                    .hasSingleBean(DSLContext.class).hasSingleBean(PlatformTransactionManager.class);
            var source = context.getBean("evidenceMediaReadDataSource", HikariDataSource.class);
            var manager = context.getBean("evidenceMediaReadTransactionManager", DataSourceTransactionManager.class);
            var template = context.getBean("evidenceMediaReadTransactionTemplate", TransactionTemplate.class);
            assertThat(source.isReadOnly()).isTrue();
            assertThat(source.getHikariPoolMXBean()).isNull();
            assertThat(manager.getDataSource()).isSameAs(source);
            assertThat(manager.isEnforceReadOnly()).isTrue();
            assertThat(template.getTransactionManager()).isSameAs(manager);
            assertThat(template.isReadOnly()).isTrue();
            assertThat(template.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThat(template.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
            assertThat(context.getBean("evidenceMediaReadDsl", DSLContext.class).settings().isExecuteLogging()).isFalse();
            for (var method : EvidenceMediaReadPersistenceConfiguration.class.getDeclaredMethods()) {
                for (var parameter : method.getParameters()) {
                    if (parameter.getType() != EvidenceMediaReadPersistenceProperties.class) {
                        assertThat(parameter.getAnnotation(Qualifier.class)).isNotNull();
                        assertThat(parameter.getAnnotation(Qualifier.class).value()).startsWith("evidenceMediaRead");
                    }
                }
            }
        });
    }

    @Test
    void propertiesConfigurationAndDatasourceStringFormsAreRedacted() {
        var properties = new EvidenceMediaReadPersistenceProperties(true, "jdbc:postgresql://localhost:5432/synthetic",
                "evidence_media_reader_role", "synthetic-password");
        assertThat(properties.toString()).isEqualTo("EvidenceMediaReadPersistenceProperties[REDACTED]");
        assertThat(new EvidenceMediaReadPersistenceConfiguration().toString()).endsWith("[REDACTED]");
        assertThat(Arrays.stream(EvidenceMediaReadPersistenceProperties.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("enabled", "url", "username", "password");
        runner.withPropertyValues("spring.profiles.active=persistence", PREFIX + "enabled=true",
                PREFIX + "url=" + properties.url(), PREFIX + "username=" + properties.username(), PREFIX + "password=" + properties.password())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(DataSource.class).toString()).isEqualTo("EvidenceMediaReadDataSource[REDACTED]");
                    assertThat(context.getBean(EvidenceMediaObjectReader.class).toString()).isEqualTo("JooqEvidenceMediaObjectReader[REDACTED]");
                });
    }
}
