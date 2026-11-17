package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.*;
import com.onecall.core.operations_iam.internal.ivrread.*;
import com.onecall.core.operations_iam.internal.subjectread.*;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.time.*;
import java.util.*;
import java.sql.*;
import java.util.concurrent.atomic.*;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.*;


class OperationsIvrSessionReadRoleLeastPrivilegeIT {
    static final OperationsIvrSessionReadFixture fixture = new OperationsIvrSessionReadFixture();
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @AfterEach void clean() throws Exception { fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]); fixture.clean(); }
    private String detail(UUID session) { return BASE + "/" + session; }
    private List<String> ids(tools.jackson.databind.JsonNode page) { var result = new ArrayList<String>(); page.path("items").forEach(item -> result.add(item.path("sessionId").asString())); return result; }
    private void listen(java.util.function.Consumer<ExecuteContext> callback) { fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeEnd(ExecuteContext context) { callback.accept(context); } }); }
    private void denied(String role, String query, String state) throws Exception {
        try (var connection = fixture.runtime()) { connection.setReadOnly(true); connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE " + role);
            assertThatThrownBy(() -> execute(connection, query)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo(state)); connection.rollback(); }
    }
    private String catalog(Connection connection) throws Exception {
        return one(connection, "SELECT md5(string_agg(oid::text||rolname||rolsuper::text||rolinherit::text,',' ORDER BY oid)) FROM pg_roles")
            + one(connection, "SELECT md5(string_agg(oid::text||coalesce(proacl::text,'')||prosrc,',' ORDER BY oid)) FROM pg_proc")
            + one(connection, "SELECT md5(string_agg(oid::text||coalesce(relacl::text,''),',' ORDER BY oid)) FROM pg_class")
            + one(connection, "SELECT md5(string_agg(roleid::text||':'||member::text||':'||grantor::text||':'||admin_option::text||':'||inherit_option::text||':'||set_option::text,',' ORDER BY roleid,member,grantor)) FROM pg_auth_members")
            + one(connection, "SELECT md5(string_agg(attrelid::text||':'||attnum::text||':'||coalesce(attacl::text,''),',' ORDER BY attrelid,attnum)) FROM pg_attribute");
    }
    @Test
    void disposableLoginHasExactlyThreeExistingNonInheritedSetMemberships() throws Exception {
        try (var connection = fixture.admin()) { assertThat(one(connection, "SELECT count(*) FROM pg_auth_members m JOIN pg_roles member ON member.oid=m.member WHERE member.rolname=?", fixture.login)).isEqualTo("3"); assertThat(one(connection, "SELECT count(*) FROM pg_auth_members m JOIN pg_roles member ON member.oid=m.member WHERE member.rolname=? AND (admin_option OR inherit_option OR NOT set_option)", fixture.login)).isEqualTo("0"); assertThat(one(connection, "SELECT rolinherit OR rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls FROM pg_roles WHERE rolname=?", fixture.login)).isEqualTo("f"); }
    }
    @Test
    void operationsRoleCannotReadIvrButScopedIvrRoleCan() throws Exception {
        fixture.session(1); denied("operations_iam_role", "SELECT id FROM ivr.inbound_session", "42501"); try (var connection = fixture.runtime()) { connection.setReadOnly(true); connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE ivr_role"); assertThat(one(connection, "SELECT count(*) FROM ivr.inbound_session")).isEqualTo("1"); connection.rollback(); } assertThat(fixture.get(BASE).path("items")).hasSize(1);
    }
    @Test
    void evidenceScopeReadsOnlySafeAssociationAndArtifactProjection() throws Exception {
        UUID session = fixture.session(1); UUID id = fixture.evidence(session, new UUID(0, 1)); fixture.artifact(id, "DERIVED"); fixture.artifact(id, "ORIGINAL"); fixture.artifact(id, "DERIVED"); var body = fixture.get(detail(session)); assertThat(body.path("evidence").get(0).path("artifactRoles").get(0).asString()).isEqualTo("ORIGINAL"); assertThat(body.path("evidence").get(0).path("artifactRoles")).hasSize(2);
        try (var connection = fixture.runtime()) { connection.setReadOnly(true); connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE evidence_role"); assertThat(one(connection, "SELECT id FROM evidence.evidence_record WHERE session_id=? OR call_id=?", session, "call-" + session)).isEqualTo(id.toString()); execute(connection, "SELECT id,status,session_id,call_id FROM evidence.evidence_record"); connection.rollback(); }
        denied("evidence_role", "SELECT snapshot FROM evidence.evidence_record", "42501"); denied("evidence_role", "SELECT * FROM evidence.evidence_record", "42501");
        for (int count : List.of(0, 1, 1000, 1001, 2000)) {
            fixture.bulkEvidence(session, count); AtomicInteger queries = new AtomicInteger(); var snapshots = new HashSet<String>();
            listen(execution -> { if (execution.sql().contains("FROM evidence.evidence_artifact")) {
                queries.incrementAndGet(); try { snapshots.add(one(execution.connection(), "SELECT pg_current_snapshot()::text")); } catch (Exception failure) { throw new IllegalStateException(failure); }
            } });
            var batch = fixture.get(detail(session)); assertThat(batch.path("evidence")).hasSize(count);
            assertThat(queries.get()).isEqualTo((count + 999) / 1000); assertThat(snapshots.size()).isEqualTo(count == 0 ? 0 : 1);
            fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]);
        }
        AtomicInteger chunks = new AtomicInteger(); fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(ExecuteContext execution) { if (execution.sql().contains("FROM evidence.evidence_artifact") && chunks.incrementAndGet() == 2) throw new org.jooq.exception.DataAccessException("PRIVATE_CHUNK"); } });
        problem(fixture.request("GET", detail(session), "admin"), 503, "SERVICE_UNAVAILABLE");
        fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]);

    }
    @Test
    void missingIvrMembershipFailsClosedWithoutRepair() throws Exception {
        fixture.session(1); try (var connection = fixture.admin()) { execute(connection, "REVOKE ivr_role FROM " + fixture.login); } try { problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); } finally { try (var connection = fixture.admin()) { execute(connection, "GRANT ivr_role TO " + fixture.login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE"); } }
        var factory = (org.springframework.beans.factory.support.DefaultListableBeanFactory) fixture.context.getBeanFactory(); Object reader = fixture.context.getBean(IvrSessionReader.class); factory.destroySingleton("operationsIvrSessionReader"); var definition = factory.getBeanDefinition("operationsIvrSessionReader"); factory.removeBeanDefinition("operationsIvrSessionReader"); try { problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); } finally { factory.registerBeanDefinition("operationsIvrSessionReader", definition); factory.registerSingleton("operationsIvrSessionReader", reader); }
    }
    @Test
    void missingEvidenceMembershipFailsDetailWithoutRepair() throws Exception {
        UUID session = fixture.session(1); try (var connection = fixture.admin()) { execute(connection, "REVOKE evidence_role FROM " + fixture.login); } try { problem(fixture.request("GET", detail(session), "admin"), 503, "SERVICE_UNAVAILABLE"); assertThat(fixture.get(BASE).path("items")).hasSize(1); } finally { try (var connection = fixture.admin()) { execute(connection, "GRANT evidence_role TO " + fixture.login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE"); } }
        var factory = (org.springframework.beans.factory.support.DefaultListableBeanFactory) fixture.context.getBeanFactory(); Object reader = fixture.context.getBean(IvrEvidenceReader.class); factory.destroySingleton("operationsIvrEvidenceReader"); var definition = factory.getBeanDefinition("operationsIvrEvidenceReader"); factory.removeBeanDefinition("operationsIvrEvidenceReader"); try { problem(fixture.request("GET", detail(session), "admin"), 503, "SERVICE_UNAVAILABLE"); } finally { factory.registerBeanDefinition("operationsIvrEvidenceReader", definition); factory.registerSingleton("operationsIvrEvidenceReader", reader); }
    }
    @Test
    void missingOperationsMembershipFailsClosedBeforeParsing() throws Exception {
        try (var connection = fixture.admin()) { execute(connection, "REVOKE operations_iam_role FROM " + fixture.login); } try { problem(fixture.request("GET", BASE + "?limit=bad", "admin"), 503, "SERVICE_UNAVAILABLE"); } finally { try (var connection = fixture.admin()) { execute(connection, "GRANT operations_iam_role TO " + fixture.login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE"); } }
    }
    @Test
    void handoffsAssertAndRestoreSameConnectionBackendAndSnapshot() throws Exception {
        fixture.transaction.execute(status -> { fixture.scope.enter(); try { int backend = fixture.dsl.fetchSingle("SELECT pg_backend_pid()").get(0, Integer.class); String snapshot = fixture.dsl.fetchSingle("SELECT pg_current_snapshot()::text").get(0, String.class); for (var role : List.of(OperationsReadContext.Role.IVR, OperationsReadContext.Role.EVIDENCE)) { fixture.scope.scoped(role, () -> { assertThat(fixture.dsl.fetchSingle("SELECT pg_backend_pid()").get(0, Integer.class)).isEqualTo(backend); assertThat(fixture.dsl.fetchSingle("SELECT pg_current_snapshot()::text").get(0, String.class)).isEqualTo(snapshot); assertThat(fixture.dsl.fetchSingle("SHOW transaction_read_only").get(0, String.class)).isEqualTo("on"); return null; }); assertThat(fixture.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo("operations_iam_role"); assertThatThrownBy(() -> fixture.scope.scoped(role, () -> { throw new IllegalStateException("PRIVATE"); })).isInstanceOf(IllegalStateException.class); assertThat(fixture.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo("operations_iam_role"); } } finally { fixture.scope.exit(); } return null; });
    }
    @Test
    void wrongEntryResourceAndTransactionStateFailClosed() throws Exception {
        for (String mode : List.of("read-write", "read-committed", "role", "resource")) { var template = new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(fixture.pool)); template.setReadOnly(!mode.equals("read-write")); template.setIsolationLevel(mode.equals("read-committed") ? Connection.TRANSACTION_READ_COMMITTED : Connection.TRANSACTION_REPEATABLE_READ); assertThatThrownBy(() -> template.execute(status -> { if (mode.equals("resource")) new OperationsReadContext(mock(javax.sql.DataSource.class), fixture.dsl).enter(); else { if (mode.equals("role")) fixture.dsl.execute("SET LOCAL ROLE ivr_role"); fixture.scope.enter(); fixture.scope.exit(); } return null; })).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE)); }
    }
    @Test
    void readOnlyTransactionRejectsAllDmlAndLockingQueries() throws Exception {
        fixture.session(1); for (String sql : List.of("INSERT INTO ivr.inbound_session(id) VALUES (gen_random_uuid())", "UPDATE ivr.inbound_session SET status='RECEIVED'", "DELETE FROM ivr.inbound_session", "SELECT id FROM ivr.inbound_session FOR UPDATE")) denied("ivr_role", sql, "25006"); denied("evidence_role", "UPDATE evidence.recording SET revision=revision+1", "25006");
    }
    @Test
    void forbiddenQueriesStayOutsideProductionSqlWithoutPretendingOwnerColumnAcls() throws Exception {
        denied("operations_iam_role", "SELECT id FROM evidence.evidence_record", "42501"); denied("operations_iam_role", "SELECT id FROM ivr.call_record", "42501"); denied("evidence_role", "SELECT snapshot_sha256 FROM evidence.evidence_record", "42501");
        String sql = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/java/com/onecall/core/ivr/internal/operationsread/JooqOperationsIvrSessionReader.java")); assertThat(sql).doesNotContain("ciphertext", "payload_", "hmac", "snapshot");
    }
    @Test
    void failedRestorationRollsBackAndEvictsDirtyConnections() throws Exception {
        fixture.pool.setMaximumPoolSize(1); fixture.pool.getHikariPoolMXBean().softEvictConnections(); fixture.session(1); AtomicBoolean armed = new AtomicBoolean(true);
        fixture.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(ExecuteContext context) { if (context.sql().equals("SET LOCAL ROLE NONE") && armed.getAndSet(false)) throw new Failure(Code.SERVICE_UNAVAILABLE); } }); problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]); fixture.get(BASE);
        var backends = new ArrayList<String>(); armed.set(true); listen(execution -> { if (execution.sql().contains("FROM ivr.inbound_session") && armed.getAndSet(false)) try { backends.add(one(execution.connection(), "SELECT pg_backend_pid()")); execute(execution.connection(), "SET statement_timeout='1234ms'"); } catch (Exception failure) { throw new IllegalStateException(failure); } }); problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]); fixture.get(BASE); try (var connection = fixture.pool.getConnection()) { assertThat(one(connection, "SELECT pg_backend_pid()")).isNotEqualTo(backends.getFirst()); }
    }
    @Test
    void poolRolesSettingsLocksCatalogsAndFactsAreCleanAfterEveryExit() throws Exception {
        fixture.session(1); String before; try (var connection = fixture.admin()) { before = catalog(connection); } var facts = fixture.fingerprints(); fixture.get(BASE); problem(fixture.request("GET", BASE + "?cursor=bad", "admin"), 400, "CURSOR_INVALID"); assertThat(fixture.fingerprints()).isEqualTo(facts); try (var connection = fixture.admin()) { assertThat(catalog(connection)).isEqualTo(before); } fixture.clean(); assertThat(fixture.pool.getMaximumPoolSize()).isEqualTo(4); assertThat(fixture.pool.getConnectionTimeout()).isEqualTo(3000); assertThat(fixture.transaction.getTimeout()).isEqualTo(5); fixture.transaction.execute(status -> { fixture.scope.enter(); try { assertThat(fixture.dsl.fetchSingle("SHOW statement_timeout").get(0, String.class)).isEqualTo("2s"); assertThat(fixture.dsl.fetchSingle("SHOW lock_timeout").get(0, String.class)).isEqualTo("250ms"); assertThat(fixture.dsl.fetchSingle("SHOW idle_in_transaction_session_timeout").get(0, String.class)).isEqualTo("5s"); } finally { fixture.scope.exit(); } return null; });
    }
}
