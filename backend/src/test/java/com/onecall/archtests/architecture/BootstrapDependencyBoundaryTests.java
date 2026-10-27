package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

class BootstrapDependencyBoundaryTests {

    private static final List<String> FORBIDDEN = List.of(
            "org.flywaydb.core.Flyway",
            "org.springframework.security.config.annotation.web.builders.HttpSecurity",
            "org.jooq.DSLContext",
            "com.zaxxer.hikari.HikariDataSource");

    @Test
    void forbiddenFrameworksAreAbsentFromTheBootstrapClasspath() {
        for (String type : FORBIDDEN) {
            assertThat(ClassUtils.isPresent(type, getClass().getClassLoader()))
                    .as(type + " must not be on the PA0 classpath")
                    .isFalse();
        }
    }
}
