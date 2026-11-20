package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.one;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService;
import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class OperationsCatalogVersionLifecycleFixture implements AutoCloseable {
    final OperationsCatalogProductReadFixture catalog = new OperationsCatalogProductReadFixture();
    final OperationsIvrSessionReadFixture shared = catalog.shared;
    final JsonMapper mapper = JsonMapper.builder().build();
    final List<Entry> entries = new CopyOnWriteArrayList<>();
    final List<String> states = new CopyOnWriteArrayList<>();
    final List<String> started = new CopyOnWriteArrayList<>();
    final AtomicBoolean armed = new AtomicBoolean();
    CountDownLatch lockAttempt;
    volatile int lockAttemptPid;
    volatile String fault;
    volatile java.util.function.Consumer<ExecuteContext> activation;
    OperationsCatalogSaleCommand command;
    record Entry(String sql, List<Object> binds, String role, int pid, long transaction) { }

    void start() throws Exception {
        catalog.start();
        try (var connection = admin()) { for (String role : List.of("ivr_role", "evidence_role")) execute(connection, "REVOKE " + role + " FROM " + shared.login); }
        command = shared.context.getBean(OperationsCatalogSaleCommand.class);
    }
    void reset() throws Exception {
        catalog.reset(); entries.clear(); states.clear(); started.clear(); fault = null; armed.set(false); activation = null;
        lockAttempt = new CountDownLatch(1); lockAttemptPid = 0;
        shared.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext context) {
                String sql = context.sql();
                started.add(sql);
                if (sql.startsWith("SELECT pg_advisory_xact_lock(")) {
                    try { lockAttemptPid = Integer.parseInt(one(context.connection(), "SELECT pg_backend_pid()")); }
                    catch (SQLException failure) { throw new IllegalStateException(failure); }
                    lockAttempt.countDown();
                }
                if (sql.equals("SELECT id, product_id FROM catalog_order.catalog_version WHERE id = ?") && "SERIALIZATION_AT_READ".equals(fault) && armed.getAndSet(false)) throw new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", "40001"));
                if (sql.startsWith("UPDATE catalog_order.catalog_version SET published_at")) {
                    var hook = activation; if (hook != null) { activation = null; hook.accept(context); }
                    if ("SERIALIZATION_AT_ACTIVATION".equals(fault) && armed.getAndSet(false)) throw new org.jooq.exception.DataAccessException("PRIVATE", new SQLException("PRIVATE", "40001"));
                }
                if ("SET LOCAL ROLE NONE".equals(sql) && armed.get() && ("BEFORE_ROLE_RESTORE".equals(fault) || "TERMINATE_BEFORE_ROLE_RESTORE".equals(fault))) {
                    armed.set(false);
                    if ("TERMINATE_BEFORE_ROLE_RESTORE".equals(fault)) try (var admin = admin()) { execute(admin, "SELECT pg_terminate_backend(?)", Integer.parseInt(one(context.connection(), "SELECT pg_backend_pid()"))); } catch (Exception failure) { throw new IllegalStateException(failure); }
                    else throw new IllegalStateException("PRIVATE-role-restore");
                }
            }
            @Override public void executeEnd(ExecuteContext context) {
                String sql = context.sql();
                try {
                    String role = one(context.connection(), "SELECT current_user");
                    int pid = Integer.parseInt(one(context.connection(), "SELECT pg_backend_pid()"));
                    long transaction = Long.parseLong(one(context.connection(), "SELECT txid_current()"));
                    entries.add(new Entry(sql, context.query() == null ? List.of() : new ArrayList<>(context.query().getBindValues()), role, pid, transaction));
                    String stage = null;
                    if (sql.contains("audit.append_fact")) stage = "AFTER_AUDIT_APPEND";
                    if (sql.contains("platform.outbox_append")) {
                        var binds = context.query().getBindValues();
                        if (binds.contains("CATALOG_VERSION_PUBLISHED")) stage = "AFTER_PUBLISH_OUTBOX";
                        else stage = entries.stream().anyMatch(entry -> entry.transaction() == transaction && entry.sql().contains("SET published_at=?")) ? "AFTER_RETIREMENT_OUTBOX" : "AFTER_OFFSALE_OUTBOX";
                    }
                    if (armed.get() && (Objects.equals(stage, fault) || stage != null && stage.equals("AFTER_AUDIT_APPEND") && "STATEMENT_TIMEOUT".equals(fault))) {
                        armed.set(false);
                        if ("STATEMENT_TIMEOUT".equals(fault)) {
                            try (var statement = context.connection().createStatement()) { statement.execute("SELECT pg_sleep(3)"); }
                            catch (SQLException failure) { states.add(failure.getSQLState()); throw new org.jooq.exception.DataAccessException("PRIVATE", failure); }
                        }
                        throw new IllegalStateException("PRIVATE-after-real-fact");
                    }
                } catch (SQLException failure) { throw new IllegalStateException(failure); }
            }
            @Override public void exception(ExecuteContext context) { if (context.sqlException() != null) states.add(context.sqlException().getSQLState()); }
        });
    }
    void fail(String stage) { fault = stage; armed.set(true); }
    Connection admin() throws Exception { return catalog.admin(); }
    UUID product() throws Exception { return catalog.product(1, "AB"); }
    UUID version(UUID product, int number, String status) throws Exception { return catalog.version(product, number, status, false); }
    UUID draft() throws Exception { return version(product(), 1, "DRAFT"); }
    void mutate(String sql, Object... values) throws Exception { catalog.mutate(sql, values); }
    HttpResponse<String> request(UUID version, boolean publication, UUID active, int revision) throws Exception { return request(version, publication, active, revision, "admin"); }
    HttpResponse<String> request(UUID version, boolean publication, UUID active, int revision, String token) throws Exception {
        String body = publication ? "{\"expectedActiveVersionId\":" + (active == null ? "null" : "\"" + active + "\"") + ",\"expectedRevision\":" + revision + "}" : "{\"expectedRevision\":" + revision + "}";
        return shared.client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + shared.port + "/api/v1/operations/catalog/versions/" + version + (publication ? "/publications" : "/offsales"))).timeout(java.time.Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json").header("Accept", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode success(HttpResponse<String> response) {
        assertThat(response.statusCode()).as("%s%nSQL trace: %s%nSQLSTATE: %s", response.body(), entries, states).isEqualTo(200); assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        return mapper.readTree(response.body());
    }
    void problem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status); var body = mapper.readTree(response.body()); assertThat(body.size()).isEqualTo(5); assertThat(body.get("code").asString()).isEqualTo(code); assertThat(response.body()).doesNotContain("PRIVATE", "catalog_order_role", "SQL", "password");
    }
    String value(String sql, Object... arguments) throws Exception {
        try (var connection = admin(); var statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < arguments.length; index++) statement.setObject(index + 1, arguments[index]);
            try (var rows = statement.executeQuery()) { assertThat(rows.next()).isTrue(); return rows.getString(1); }
        }
    }
    String lifecycle() throws Exception { return value("SELECT coalesce(jsonb_agg(to_jsonb(row) ORDER BY id)::text,'[]') FROM catalog_order.catalog_version row"); }
    java.time.Instant published(UUID version) throws Exception {
        try (var connection = admin(); var statement = connection.prepareStatement("SELECT published_at FROM catalog_order.catalog_version WHERE id=?")) {
            statement.setObject(1, version); try (var rows = statement.executeQuery()) { assertThat(rows.next()).isTrue(); return rows.getObject(1, java.time.OffsetDateTime.class).toInstant(); }
        }
    }
    String content() throws Exception { return value("SELECT coalesce(jsonb_agg(to_jsonb(row) - 'sale_status' - 'revision' - 'published_at' ORDER BY id)::text,'[]') FROM catalog_order.catalog_version row") + value("SELECT coalesce(jsonb_agg(to_jsonb(row) ORDER BY id)::text,'[]') FROM catalog_order.catalog_benefit_line row"); }
    long audits() throws Exception { return Long.parseLong(value("SELECT count(*) FROM audit.audit_fact")); }
    long outboxes() throws Exception { return Long.parseLong(value("SELECT count(*) FROM platform.outbox_event")); }
    void facts(long audit, long outbox) throws Exception { assertThat(audits()).isEqualTo(audit); assertThat(outboxes()).isEqualTo(outbox); }
    void invariant() throws Exception { assertThat(value("SELECT count(*) FROM (SELECT product_id FROM catalog_order.catalog_version WHERE sale_status='ACTIVE' GROUP BY product_id HAVING count(*)>1) row")).isEqualTo("0"); }
    void clean() throws Exception {
        shared.clean();
        try (var connection = shared.pool.getConnection()) {
            assertThat(one(connection, "SHOW idle_in_transaction_session_timeout")).isEqualTo("0");
            assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
        }
    }
    List<Entry> calls(String token) { return entries.stream().filter(entry -> entry.sql().contains(token)).toList(); }
    void recoveryWitness(UUID product, UUID competitor) throws Exception {
        var starts = calls("SET LOCAL statement_timeout"); assertThat(starts).hasSize(2);
        assertThat(starts).extracting(Entry::transaction).doesNotHaveDuplicates();
        long recovery = starts.getLast().transaction();
        var recovered = entries.stream().filter(entry -> entry.transaction() == recovery).toList();
        assertThat(recovered.stream().filter(entry -> entry.sql().startsWith("SET LOCAL ROLE"))).extracting(Entry::sql).containsExactly("SET LOCAL ROLE catalog_order_role", "SET LOCAL ROLE NONE");
        assertThat(recovered.stream().filter(entry -> entry.sql().equals("SET LOCAL ROLE catalog_order_role"))).extracting(Entry::role).containsExactly("catalog_order_role");
        assertThat(recovered.getFirst().role()).isEqualTo(shared.login); assertThat(recovered.getLast().role()).isEqualTo(shared.login);
        assertThat(recovered).allSatisfy(entry -> assertThat(entry.sql()).doesNotContain("employee_account", "UPDATE ", "pg_advisory_xact_lock", "append_fact", "outbox_append"));
        assertThat(started.stream().filter(sql -> sql.contains("FROM operations_iam.employee_account"))).hasSize(1);
        assertThat(started.stream().filter(sql -> sql.startsWith("UPDATE catalog_order.catalog_version SET published_at"))).hasSize(1);
        assertThat(calls("pg_advisory_xact_lock")).hasSize(1);
        assertThat(value("SELECT count(*) FROM catalog_order.catalog_version WHERE product_id=? AND sale_status='ACTIVE'", product)).isEqualTo("1");
        assertThat(value("SELECT id FROM catalog_order.catalog_version WHERE product_id=? AND sale_status='ACTIVE'", product)).isEqualTo(competitor.toString());
    }
    void readWitness(UUID product) throws Exception { assertThat(catalog.get(OperationsCatalogProductReadFixture.BASE + "/" + product).get("productId").asString()).isEqualTo(product.toString()); }
    @Override public void close() { catalog.close(); }
}
