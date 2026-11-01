package com.onecall.archtests;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.sharing.internal.application.CustomerShareQueryService;
import com.onecall.core.sharing.internal.persistence.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SharingPersistenceConfigurationTests {
    @Test
    void defaultProfileCreatesNoPersistenceBeans() {
        new ApplicationContextRunner().withUserConfiguration(SharingPersistenceConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            for (Class<?> type : List.of(javax.sql.DataSource.class, org.jooq.DSLContext.class,
                    org.springframework.transaction.PlatformTransactionManager.class,
                    org.springframework.transaction.support.TransactionTemplate.class,
                    CustomerShareQueryService.class, ShareQueryRepository.class, com.zaxxer.hikari.HikariDataSource.class)) {
                assertThat(context.getBeansOfType(type)).isEmpty();
            }
        });
    }

    @Test
    void persistenceRequiresExplicitQueryCredentials() throws Exception {
        for (String missing : List.of("url", "username", "password")) {
            for (String blank : List.of("ABSENT", "", " ")) {
                java.util.ArrayList<String> properties = new java.util.ArrayList<>();
                properties.add("spring.profiles.active=persistence");
                for (String property : List.of("url", "username", "password")) {
                    if (!property.equals(missing)) {
                        properties.add("onecall.persistence." + property + "="
                                + (property.equals("url") ? "jdbc:postgresql://invalid/test" : "CANARY"));
                    }
                }
                if (!blank.equals("ABSENT")) { properties.add("onecall.persistence." + missing + "=" + blank); }
                new ApplicationContextRunner().withUserConfiguration(SharingPersistenceConfiguration.class)
                        .withPropertyValues(properties.toArray(String[]::new))
                        .run(context -> {
                            assertThat(context).hasFailed();
                            Throwable root = context.getStartupFailure();
                            while (root.getCause() != null) { root = root.getCause(); }
                            assertThat(root).isInstanceOf(IllegalArgumentException.class)
                                    .hasMessage("Explicit query credentials are required");
                        });
            }
        }
        String resources = Files.readString(Path.of("src/main/resources/application-persistence.yml"));
        assertThat(resources).contains("${ONECALL_DB_URL:}", "${ONECALL_DB_USERNAME:}", "${ONECALL_DB_PASSWORD:}");
        assertThat(java.util.Arrays.stream(SharingPersistenceProperties.class.getDeclaredFields()).map(java.lang.reflect.Field::getName))
                .containsExactlyInAnyOrder("url", "username", "password");
        try (var paths = Files.walk(Path.of("src/main/java/com/onecall/core/sharing/internal"))) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                assertThat(Files.readString(path)).doesNotContain("org.flywaydb", "spring.datasource", "ONECALL_MIGRATION");
            }
        }
        assertThat(resources).doesNotContain("flyway", "migration", "spring.datasource", "jdbc:h2");
    }
}
