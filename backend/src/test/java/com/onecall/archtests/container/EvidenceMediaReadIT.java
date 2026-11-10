package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.evidence.internal.mediaread.EvidenceMediaReadPersistenceConfiguration;
import com.onecall.core.evidence.internal.mediaread.JooqEvidenceMediaObjectReader;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader;
import com.onecall.core.evidence.mediaread.EvidenceMediaObjectReader.Result;
import com.onecall.core.evidence.mediaread.ResolvedMediaObject;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.json.JsonMapper;

class EvidenceMediaReadIT {
    private static final Fixture BASE = new Fixture();

    @BeforeAll static void start() throws Exception { BASE.start(); }
    @AfterAll static void stop() { BASE.close(); }
    @BeforeEach void seed() throws Exception { BASE.seed(); }

    @Test
    void dedicatedQualifiedReaderAuthenticatesAndResolvesInOneReadOnlyQuery() throws Exception {
        BASE.password("evidence_media_reader_role", BASE.password);
        try {
            List<String> queries = new CopyOnWriteArrayList<>();
            List<String> facts = new CopyOnWriteArrayList<>();
            new ApplicationContextRunner().withUserConfiguration(EvidenceMediaReadPersistenceConfiguration.class)
                    .withPropertyValues("spring.profiles.active=persistence", "onecall.evidence-media-read.datasource.enabled=true",
                            "onecall.evidence-media-read.datasource.url=" + BASE.url,
                            "onecall.evidence-media-read.datasource.username=evidence_media_reader_role",
                            "onecall.evidence-media-read.datasource.password=" + BASE.password)
                    .withInitializer(context -> context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
                        @Override public Object postProcessAfterInitialization(Object bean, String name) {
                            if (!name.equals("evidenceMediaReadDataSource")) return bean;
                            var source = (HikariDataSource) bean;
                            return Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class<?>[] {DataSource.class, AutoCloseable.class},
                                    (proxy, method, arguments) -> {
                                        try {
                                            Object result = method.invoke(source, arguments);
                                            if (result instanceof Connection connection && method.getName().equals("getConnection")) {
                                                return capture(connection, queries, facts);
                                            }
                                            return result;
                                        } catch (InvocationTargetException failure) { throw failure.getCause(); }
                                    });
                        }
                    })).run(context -> {
                        assertThat(context).hasNotFailed();
                        Result result = context.getBean(EvidenceMediaObjectReader.class).resolve(BASE.evidence);
                        assertThat(result).isEqualTo(new Result.Found(new ResolvedMediaObject(BASE.media, "a".repeat(64), 123, "audio/wav", 10)));
                        assertThat(queries).hasSize(1);
                        assertThat(queries.getFirst()).contains("WHERE er.id = ?", "LEFT JOIN evidence.evidence_artifact", "LEFT JOIN evidence.recording")
                                .doesNotContain("SELECT *", "LIMIT", "bucket", "object_key", "object_version_id");
                        assertThat(facts).containsExactly("evidence_media_reader_role|evidence_media_reader_role|on|read committed");
                        var json = JsonMapper.builder().build().readTree(JsonMapper.builder().build().writeValueAsString(((Result.Found) result).media()));
                        assertThat(json.propertyNames()).containsExactlyInAnyOrder("mediaObjectId", "sha256", "sizeBytes", "mimeType", "durationSeconds");
                    });
        } finally { BASE.password("evidence_media_reader_role", null); }
    }

    @Test
    void onlyAbsentEvidenceReturnsMissing() throws Exception {
        BASE.withReader(reader -> {
            assertThat(reader.resolve(UUID.randomUUID())).isEqualTo(new Result.Missing());
            assertThatNullPointerException().isThrownBy(() -> reader.resolve(null));
            assertThat(reader.resolve(BASE.evidence)).isInstanceOf(Result.Found.class);
        });
    }

    @Test
    void missingOriginalMediaAndRecordingRemainUnavailable() throws Exception {
        for (String table : List.of("evidence_artifact", "media_object", "recording")) {
            BASE.seed();
            BASE.corrupt("DELETE FROM evidence." + table);
            BASE.withReader(reader -> assertThat(reader.resolve(BASE.evidence)).isEqualTo(new Result.Unavailable()));
        }
    }

    @Test
    void pendingQuarantinedAndUnvalidatedFactsAreUnavailable() throws Exception {
        for (String status : List.of("PENDING", "QUARANTINED", "FAILED")) {
            BASE.seed();
            BASE.corrupt("UPDATE evidence.evidence_record SET status='" + status + "', solidified_at=NULL");
            BASE.withReader(reader -> assertThat(reader.resolve(BASE.evidence)).isEqualTo(new Result.Unavailable()));
        }
        BASE.seed();
        BASE.corrupt("UPDATE evidence.recording SET status='UPLOADED', validated_duration=NULL, validated_sha256=NULL, validated_at=NULL");
        BASE.withReader(reader -> assertThat(reader.resolve(BASE.evidence)).isEqualTo(new Result.Unavailable()));
    }

    @Test
    void mismatchedRecordingReferenceMediaAndDerivedArtifactFailClosed() throws Exception {
        UUID differentRecording = UUID.randomUUID();
        for (String mutation : List.of("UPDATE evidence.evidence_record SET source_ref_id='" + differentRecording
                        + "', snapshot=jsonb_set(snapshot,'{source,recordingId}',to_jsonb('" + differentRecording + "'::text))",
                "UPDATE evidence.recording SET media_object_id='" + UUID.randomUUID() + "'",
                "UPDATE evidence.evidence_artifact SET artifact_role='DERIVED'")) {
            BASE.seed();
            BASE.corrupt(mutation);
            BASE.withReader(reader -> assertThat(reader.resolve(BASE.evidence)).isEqualTo(new Result.Unavailable()));
        }
    }

    @Test
    void corruptedMetadataIsUnavailableAndOptionalDurationIsSupported() throws Exception {
        for (String mutation : List.of("sha256=NULL", "sha256='invalid'", "size_bytes=0", "size_bytes=NULL", "mime_type=' '", "mime_type=NULL")) {
            BASE.seed();
            Map<String, String> checks;
            try (Connection connection = BASE.admin()) {
                checks = new java.util.LinkedHashMap<>();
                try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                        "SELECT conname,pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='evidence.media_object'::regclass AND contype='c'")) {
                    while (rows.next()) checks.put(rows.getString(1), rows.getString(2));
                }
                for (String name : checks.keySet()) Fixture.execute(connection, "ALTER TABLE evidence.media_object DROP CONSTRAINT " + name);
                for (String name : List.of("sha256", "size_bytes", "mime_type")) Fixture.execute(connection, "ALTER TABLE evidence.media_object ALTER COLUMN " + name + " DROP NOT NULL");
            }
            try {
                BASE.corrupt("UPDATE evidence.media_object SET " + mutation);
                BASE.withReader(reader -> assertThat(reader.resolve(BASE.evidence)).isEqualTo(new Result.Unavailable()));
            } finally {
                BASE.corrupt("UPDATE evidence.media_object SET sha256=repeat('a',64), size_bytes=123, mime_type='audio/wav'");
                try (Connection connection = BASE.admin()) {
                    for (String name : List.of("sha256", "size_bytes", "mime_type")) Fixture.execute(connection, "ALTER TABLE evidence.media_object ALTER COLUMN " + name + " SET NOT NULL");
                    for (var check : checks.entrySet()) Fixture.execute(connection, "ALTER TABLE evidence.media_object ADD CONSTRAINT " + check.getKey() + " " + check.getValue());
                }
            }
        }
        BASE.seed();
        try (Connection connection = BASE.admin()) { Fixture.execute(connection, "ALTER TABLE evidence.recording DROP CONSTRAINT ck_recording_state_shape"); }
        try {
            BASE.corrupt("UPDATE evidence.recording SET validated_duration=NULL");
            BASE.withReader(reader -> assertThat(((Result.Found) reader.resolve(BASE.evidence)).media().durationSeconds()).isNull());
        } finally {
            BASE.corrupt("UPDATE evidence.recording SET validated_duration=10");
            try (Connection connection = BASE.admin()) {
                String migration = Files.readString(Path.of("src/main/resources/db/migration/V008__evidence_media.sql"));
                String check = migration.substring(migration.indexOf("CONSTRAINT ck_recording_state_shape"), migration.indexOf("CONSTRAINT ck_recording_upload_expiry")).strip();
                Fixture.execute(connection, "ALTER TABLE evidence.recording ADD " + check.substring(0, check.length() - 1));
            }
        }
    }

    @Test
    void duplicateOriginalArtifactsAreUnavailableWithoutLimitOrAggregation() throws Exception {
        try (Connection connection = BASE.admin()) { Fixture.execute(connection, "DROP INDEX evidence.uq_evidence_original_artifact"); }
        UUID secondMedia = UUID.randomUUID();
        try {
            try (Connection connection = BASE.admin()) {
                Fixture.execute(connection, "INSERT INTO evidence.media_object SELECT ?,storage_provider,bucket,?,object_version_id,sha256,size_bytes,mime_type,crypto_key_ref,worm_status,created_at FROM evidence.media_object WHERE id=?",
                        secondMedia, UUID.randomUUID().toString(), BASE.media);
                Fixture.execute(connection, "INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) VALUES (?,?,?,'ORIGINAL')", UUID.randomUUID(), BASE.evidence, secondMedia);
            }
            BASE.withReader(reader -> assertThat(reader.resolve(BASE.evidence)).isEqualTo(new Result.Unavailable()));
        } finally {
            BASE.corrupt("DELETE FROM evidence.evidence_artifact WHERE media_object_id='" + secondMedia + "'");
            try (Connection connection = BASE.admin()) { Fixture.execute(connection, "CREATE UNIQUE INDEX uq_evidence_original_artifact ON evidence.evidence_artifact(evidence_id) WHERE artifact_role='ORIGINAL'"); }
        }
    }

    @Test
    void connectionAndPermissionFaultsReturnOnlyUnavailable() throws Exception {
        String role = "denied_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = BASE.admin()) { Fixture.execute(connection, "CREATE ROLE " + role + " LOGIN PASSWORD '" + BASE.password + "'"); }
        var source = new DriverManagerDataSource(BASE.url, role, BASE.password);
        assertThat(Fixture.reader(source).resolve(BASE.evidence)).isEqualTo(new Result.Unavailable());
        source.setPassword(UUID.randomUUID().toString());
        assertThat(Fixture.reader(source).resolve(BASE.evidence)).isEqualTo(new Result.Unavailable());
        BASE.withReader(reader -> assertThat(reader.resolve(BASE.evidence)).isInstanceOf(Result.Found.class));
    }

    private static Connection capture(Connection connection, List<String> queries, List<String> facts) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class}, (proxy, method, arguments) -> {
            try {
                Object result = method.invoke(connection, arguments);
                if (method.getName().equals("prepareStatement") && result instanceof PreparedStatement statement && ((String) arguments[0]).contains("evidence.evidence_record er")) {
                    String sql = (String) arguments[0];
                    return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[] {PreparedStatement.class}, (statementProxy, statementMethod, statementArguments) -> {
                        try {
                            if (statementMethod.getName().startsWith("execute")) {
                                queries.add(sql);
                                try (Statement probe = connection.createStatement(); var rows = probe.executeQuery("SELECT current_user||'|'||session_user||'|'||current_setting('transaction_read_only')||'|'||current_setting('transaction_isolation')")) {
                                    assertThat(rows.next()).isTrue(); facts.add(rows.getString(1));
                                }
                                assertThat(statement.getParameterMetaData().getParameterCount()).isEqualTo(1);
                            }
                            return statementMethod.invoke(statement, statementArguments);
                        } catch (InvocationTargetException failure) { throw failure.getCause(); }
                    });
                }
                return result;
            } catch (InvocationTargetException failure) { throw failure.getCause(); }
        });
    }

    static final class Fixture implements AutoCloseable {
        final String password = UUID.randomUUID().toString();
        final GenericContainer<?> postgres = new GenericContainer<>(DockerImageName.parse("postgres:16"))
                .withEnv("POSTGRES_DB", "evidence_media_read").withEnv("POSTGRES_USER", "fixture_admin").withEnv("POSTGRES_PASSWORD", password)
                .withExposedPorts(5432).waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2))
                .withStartupTimeout(Duration.ofMinutes(2));
        Flyway flyway;
        String url;
        UUID evidence;
        UUID media;
        UUID recording;

        void start() {
            postgres.start();
            url = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/evidence_media_read";
            flyway = Flyway.configure().dataSource(url, "fixture_admin", password).locations("classpath:db/migration").load();
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(14);
            flyway.validate();
        }

        Connection admin() throws SQLException { return DriverManager.getConnection(url, "fixture_admin", password); }
        Connection login(String role) throws SQLException { return DriverManager.getConnection(url, role, password); }

        void password(String role, String value) throws SQLException {
            try (Connection connection = admin()) { execute(connection, "ALTER ROLE " + role + " PASSWORD " + (value == null ? "NULL" : "'" + value + "'")); }
        }

        void withReader(java.util.function.Consumer<EvidenceMediaObjectReader> action) throws Exception {
            password("evidence_media_reader_role", password);
            try { action.accept(reader(new DriverManagerDataSource(url, "evidence_media_reader_role", password))); }
            finally { password("evidence_media_reader_role", null); }
        }

        static EvidenceMediaObjectReader reader(DataSource source) {
            var manager = new DataSourceTransactionManager(source);
            manager.setEnforceReadOnly(true);
            var transactions = new TransactionTemplate(manager);
            transactions.setReadOnly(true);
            transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            var database = DSL.using(new TransactionAwareDataSourceProxy(source), SQLDialect.POSTGRES,
                    new Settings().withExecuteLogging(false));
            return new JooqEvidenceMediaObjectReader(database, transactions);
        }

        void corrupt(String sql) throws SQLException {
            try (Connection connection = admin()) {
                connection.setAutoCommit(false);
                execute(connection, "SET LOCAL session_replication_role=replica");
                execute(connection, sql);
                connection.commit();
            }
        }

        void seed() throws Exception {
            evidence = UUID.randomUUID(); media = UUID.randomUUID(); recording = UUID.randomUUID();
            UUID subject = UUID.randomUUID(); UUID session = UUID.randomUUID();
            String call = UUID.randomUUID().toString(); String event = UUID.randomUUID().toString();
            try (Connection connection = admin()) {
                connection.setAutoCommit(false);
                execute(connection, "TRUNCATE subject_number.business_subject CASCADE");
                execute(connection, "TRUNCATE evidence.media_object, evidence.recording, evidence.evidence_record, evidence.evidence_artifact, evidence.evidence_event CASCADE");
                execute(connection, "INSERT INTO subject_number.business_subject(id,source) VALUES (?,'IVR')", subject);
                execute(connection, "INSERT INTO ivr.inbound_session(id,call_id,subject_id,trusted_caller,access_number_masked,node_id,protocol_version,prompt_version) VALUES (?,?,?,false,'synthetic','fixture','v1','v1')", session, call, subject);
                execute(connection, "INSERT INTO ivr.call_record(id,call_id,session_id,subject_id,caller_ciphertext,caller_number_hmac,caller_masked,access_number_masked,node_id,protocol_version) VALUES (?,?,?,?,'v1.fixture.payload',repeat('b',64),'synthetic','synthetic','fixture','v1')", UUID.randomUUID(), call, session, subject);
                execute(connection, "INSERT INTO evidence.media_object(id,storage_provider,bucket,object_key,object_version_id,sha256,size_bytes,mime_type,crypto_key_ref) VALUES (?,'COS','synthetic',?,'v1',repeat('a',64),123,'audio/wav','fixture-key')", media, UUID.randomUUID().toString());
                execute(connection, "INSERT INTO evidence.recording(id,call_id,recording_id,media_object_id,declared_format,declared_size,declared_duration,declared_sha256,validated_duration,validated_sha256,status,upload_expires_at,validated_at) VALUES (?,?,?,?,'wav',123,10,repeat('a',64),10,repeat('a',64),'VALIDATED',now()+interval '1 hour',now())", recording, call, UUID.randomUUID().toString(), media);
                execute(connection, """
                        INSERT INTO evidence.evidence_record(id,subject_id,source_type,source_ref_id,source_event_domain,source_event_id,
                            identity_at_capture,consent_version,snapshot,snapshot_sha256,call_id,session_id,status,solidified_at)
                        VALUES (?,?,'CALL_RECORDING',?,'EVIDENCE',?,
                            jsonb_build_object('subjectId',?::text,'capturedAt','2026-01-01T00:00:00Z','sourceChannel','IVR'),'v1',
                            jsonb_build_object('capture','{}'::jsonb,'content','{}'::jsonb,'sourceEventDomain','EVIDENCE','sourceEventId',?::text,
                                'source',jsonb_build_object('subjectId',?::text,'recordingId',?::text,'callId',?::text,'sessionId',?::text)),
                            repeat('a',64),?,?,'SOLIDIFIED',now())
                        """, evidence, subject, recording, event, subject, event, subject, recording, call, session, call, session);
                execute(connection, "INSERT INTO evidence.evidence_event(id,evidence_id,event_domain,event_id,event_type,payload_redacted,payload_sha256) VALUES (?,?,'EVIDENCE',?,'EVIDENCE_CREATED','{}'::jsonb,repeat('a',64))", UUID.randomUUID(), evidence, event);
                execute(connection, "INSERT INTO evidence.evidence_artifact(id,evidence_id,media_object_id,artifact_role) VALUES (?,?,?,'ORIGINAL')", UUID.randomUUID(), evidence, media);
                connection.commit();
            }
        }

        static void execute(Connection connection, String sql, Object... arguments) throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < arguments.length; index++) statement.setObject(index + 1, arguments[index]);
                statement.execute();
            }
        }

        static List<String> strings(Connection connection, String sql) throws SQLException {
            List<String> result = new ArrayList<>();
            try (Statement statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
                while (rows.next()) result.add(rows.getString(1));
            }
            return result;
        }

        static String migration() throws Exception { return Files.readString(Path.of("src/main/resources/db/migration/V013__evidence_media_reader_role.sql")); }
        @Override public void close() { postgres.stop(); }
    }
}
