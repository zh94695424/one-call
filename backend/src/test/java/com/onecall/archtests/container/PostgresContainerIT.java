package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Real PostgreSQL 16 container smoke for PA0. There is no skip path: an unavailable Docker
 * daemon, failed image pull, failed container start, JDBC failure or failed assertion fails the
 * Maven build. The password is generated per JVM run and never persisted.
 */
class PostgresContainerIT {

    private static final String PASSWORD = UUID.randomUUID().toString();

    private static final GenericContainer<?> POSTGRES =
            new GenericContainer<>(DockerImageName.parse("postgres:16"))
                    .withEnv("POSTGRES_USER", "onecall")
                    .withEnv("POSTGRES_DB", "onecall")
                    .withEnv("POSTGRES_PASSWORD", PASSWORD)
                    .withExposedPorts(5432)
                    .waitingFor(Wait.forLogMessage(
                            ".*database system is ready to accept connections.*\\n", 2))
                    .withStartupTimeout(Duration.ofMinutes(2));

    @BeforeAll
    static void startContainer() {
        POSTGRES.start();
    }

    @AfterAll
    static void stopContainer() {
        POSTGRES.stop();
    }

    @Test
    void connectsToPostgresAndFindsNoApplicationSchema() throws Exception {
        String url = String.format(
                "jdbc:postgresql://%s:%d/onecall",
                POSTGRES.getHost(), POSTGRES.getMappedPort(5432));

        try (Connection connection = DriverManager.getConnection(url, "onecall", PASSWORD);
                Statement statement = connection.createStatement()) {

            try (ResultSet resultSet =
                    statement.executeQuery("SELECT current_database(), current_user")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString(1)).isEqualTo("onecall");
                assertThat(resultSet.getString(2)).isEqualTo("onecall");
            }

            try (ResultSet resultSet = statement.executeQuery("SHOW server_version_num")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(Integer.parseInt(resultSet.getString(1)))
                        .isGreaterThanOrEqualTo(160000);
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT count(*) FROM information_schema.tables "
                            + "WHERE table_schema NOT IN ('pg_catalog','information_schema')")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getLong(1)).isZero();
            }

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT to_regclass('public.flyway_schema_history') IS NULL")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean(1)).isTrue();
            }
        }
    }
}
