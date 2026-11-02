package com.onecall.archtests.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

class BootstrapDependencyBoundaryTests {

    private static final List<String> FORBIDDEN = List.of(
            "org.flywaydb.core.Flyway",
            "org.springframework.security.config.annotation.web.builders.HttpSecurity",
            "jakarta.persistence.EntityManager");

    @Test
    void approvedPersistenceFrameworksAndForbiddenStacksAreExact() {
        for (String type : List.of("org.jooq.DSLContext", "com.zaxxer.hikari.HikariDataSource",
                "org.springframework.jdbc.core.JdbcTemplate", "org.springframework.transaction.support.TransactionTemplate",
                "org.postgresql.Driver", "org.springframework.web.context.WebApplicationContext",
                "org.springframework.web.servlet.DispatcherServlet", "org.apache.catalina.startup.Tomcat",
                "tools.jackson.databind.json.JsonMapper", "com.fasterxml.jackson.annotation.JsonInclude")) {
            assertThat(ClassUtils.isPresent(type, getClass().getClassLoader())).as(type).isTrue();
        }
        for (String type : FORBIDDEN) {
            assertThat(ClassUtils.isPresent(type, getClass().getClassLoader()))
                    .as(type + " must not be on the bootstrap classpath")
                    .isFalse();
        }
    }
}
