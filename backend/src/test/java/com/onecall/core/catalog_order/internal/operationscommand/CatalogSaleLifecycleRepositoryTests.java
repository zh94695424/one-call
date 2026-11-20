package com.onecall.core.catalog_order.internal.operationscommand;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.TransactionWitness;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand.*;
import java.sql.Connection;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class CatalogSaleLifecycleRepositoryTests extends CatalogSaleLifecycleRepositoryHarness {
    @BeforeEach void setup() throws Exception { initialize(); }
    @AfterEach void cleanup() { close(); }
    @Test void readsAuthorityWithoutLocking() {
        // Ordered authority-port delegation is owned by DefaultOperationsCatalogSaleCommandTests.requiresActiveAdminWitness.
        publish(null, 1); assertThat(statements).isNotEmpty(); assertThat(statements).allSatisfy(sql -> assertThat(sql.toUpperCase(Locale.ROOT)).doesNotContain("EMPLOYEE_ACCOUNT", "OPERATIONS_IAM.", "FOR UPDATE"));
    }
    @Test void entersExactOperationsIamRole() { role = "wrong"; assertThatThrownBy(() -> repository.catalogRole(witness)).isInstanceOf(Failure.class); assertThat(statements).doesNotContain("SET LOCAL ROLE wrong"); }
    @Test void entersExactCatalogOrderRoleForMutation() { publish(null, 1); assertThat(role).isEqualTo("catalog_order_role"); assertThat(statements.indexOf("SET LOCAL ROLE catalog_order_role")).isLessThan(first("UPDATE ")); }
    @Test void acquiresProductLockBeforeDecisionTime() { publish(null, 1); assertThat(statements.subList(0, 3)).containsExactly("SET LOCAL statement_timeout = '2000ms'", "SET LOCAL lock_timeout = '250ms'", "SET LOCAL idle_in_transaction_session_timeout = '5000ms'"); assertThat(first("pg_advisory_xact_lock")).isLessThan(first("clock_timestamp")); assertThat(first("clock_timestamp")).isLessThan(first("UPDATE ")); }
    @Test void publicationReadsCandidateAndCurrentActiveUnderLock() { assertThat(publish(null, 1).currentActiveVersionId()).isEqualTo(VERSION); assertThat(first("pg_advisory_xact_lock")).isLessThan(first("offer_type, audience")); assertThat(first("catalog_benefit_line")).isLessThan(first("UPDATE ")); assertThat(statements.getLast()).contains("sale_status = 'ACTIVE'"); }
    @Test void retiresOldBeforeActivatingCandidate() { oldActive(); assertThat(publish((UUID) old.get("id"), 1).currentActiveVersionId()).isEqualTo(VERSION); assertThat(first("SET sale_status='OFFSALE'")).isLessThan(first("SET published_at=?")); assertThat(old.get("revision")).isEqualTo(2); assertThat(target.get("revision")).isEqualTo(2); }
    @Test void offsaleUsesConditionalRevisionUpdate() { active(); assertThat(repository.offsale(VERSION, 1, PRODUCT, VERSION).currentActiveVersionId()).isNull(); assertThat(statements).anyMatch(sql -> sql.contains("WHERE id=? AND sale_status='ACTIVE' AND revision=?")); assertThat(repository.offsale(VERSION, 2, PRODUCT, VERSION).revision()).isEqualTo(2); assertThat(audits).hasSize(1); }
    @Test void updatesOnlyLifecycleColumns() { oldActive(); publish((UUID) old.get("id"), 1); assertThat(statements.stream().filter(sql -> sql.startsWith("UPDATE"))).allSatisfy(sql -> assertThat(sql).doesNotContain("snapshot=", "price_cents=", "catalog_offer", "benefit_line")); }
    @Test void usesOnePublishAudit() { publish(null, 1); assertThat(audits).hasSize(1); var binds = audits.getFirst(); assertThat(binds.get(1)).isEqualTo(new UUID(2, 38)); assertThat(binds.get(2)).isEqualTo("CATALOG_VERSION_PUBLISH"); assertThat(binds.get(3)).isEqualTo(VERSION); assertThat(binds.get(6)).isEqualTo("catalog_version:" + VERSION + ":published"); assertThat(binds.get(7)).isEqualTo(new UUID(3, 38).toString()); }
    @Test void usesTwoPublishOutboxesWhenOldActiveExists() { oldActive(); old.put("version_no", 7); old.put("snapshot_sha256", "b".repeat(64)); publish((UUID) old.get("id"), 1); assertThat(outboxes).hasSize(2); exactOutbox(0, (UUID) old.get("id"), 7, "OFFSALE", NOW.minusSeconds(60), "b".repeat(64), false); exactOutbox(1, VERSION, 1, "ACTIVE", NOW, HASH, true); }
    @Test void usesOneOffsaleAuditAndOutbox() { active(); repository.offsale(VERSION, 1, PRODUCT, VERSION); assertThat(audits).hasSize(1); assertThat(outboxes).hasSize(1); assertThat(audits.getFirst().get(2)).isEqualTo("CATALOG_VERSION_OFFSALE"); exactOutbox(0, VERSION, 1, "OFFSALE", NOW, HASH, false); assertThat(audits.getFirst().get(6)).isEqualTo(outboxes.getFirst().getFirst()); }
    @Test void restoresRoleOnSuccessAndFailure() { publish(null, 1); repository.restore(witness); assertThat(role).isEqualTo("login"); assertThat(statements.get(statements.size() - 2)).isEqualTo("SET LOCAL ROLE NONE"); doThrow(new IllegalStateException("PRIVATE")).when(dsl).execute("SET LOCAL ROLE NONE"); assertThatThrownBy(() -> repository.restore(witness)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE)); }
}

class CatalogSaleLifecycleRepositoryHarness implements AutoCloseable {
    void exactOutbox(int index, UUID version, int number, String status, Instant published, String hash, boolean publication) {
        String payload = "{\"catalogVersionId\":\"" + version + "\",\"productId\":\"" + PRODUCT + "\",\"versionNo\":" + number + ",\"saleStatus\":\"" + status + "\",\"publishedAt\":\"" + published + "\",\"snapshotSha256\":\"" + hash + "\"}";
        assertThat(outboxes.get(index)).containsExactly("catalog_version:" + version + (publication ? ":published" : ":offsale"), version, publication ? "CATALOG_VERSION_PUBLISHED" : "CATALOG_VERSION_OFFSALE", payload, independentHash(payload));
        verify(dsl, atLeastOnce()).fetch(eq("SELECT platform.outbox_append('OPERATIONS', ?, 'CATALOG_VERSION', ?, ?, ?::jsonb, NULL, ?)"), eq(outboxes.get(index).toArray()));
    }
    static String independentHash(String value) {
        try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
    }
    static final UUID VERSION = new UUID(0, 38), PRODUCT = new UUID(1, 38);
    static final String HASH = "a".repeat(64);
    static final Instant NOW = Instant.parse("2026-11-18T00:00:00.123456Z");
    final DataSource source = mock(DataSource.class);
    final Connection connection = mock(Connection.class);
    final DSLContext dsl = mock(DSLContext.class);
    final Map<String, Object> target = valid();
    Map<String, Object> old;
    final List<String> statements = new ArrayList<>();
    final List<List<Object>> audits = new ArrayList<>(), outboxes = new ArrayList<>();
    String role = "login";
    final JooqCatalogSaleLifecycleRepository repository = new JooqCatalogSaleLifecycleRepository(source, dsl);
    TransactionWitness witness;

    void initialize() throws Exception {
        TransactionSynchronizationManager.setActualTransactionActive(true); TransactionSynchronizationManager.bindResource(source, new ConnectionHolder(connection));
        when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        when(dsl.fetchSingle(anyString())).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0); statements.add(sql);
            if (sql.contains("current_setting('statement_timeout')")) return record(Map.of("statement", "2s", "lock", "250ms", "idle", "5s"));
            if (sql.contains("clock_timestamp")) return record(Map.of("decision_time", NOW));
            return record(Map.of("login", "login", "role", role, "backend", 38, "transaction", 39L, "readonly", "off"));
        });
        when(dsl.execute(anyString())).thenAnswer(invocation -> { String sql = invocation.getArgument(0); statements.add(sql); if (sql.startsWith("SET LOCAL ROLE ")) role = sql.endsWith("NONE") ? "login" : sql.substring("SET LOCAL ROLE ".length()); return 0; });
        when(dsl.fetchOne(anyString(), any(Object[].class))).thenAnswer(invocation -> { String sql = invocation.getArgument(0); statements.add(sql); return record(target); });
        when(dsl.fetch(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0); Object[] binds = (Object[]) invocation.getRawArguments()[1]; statements.add(sql);
            if (sql.contains("sale_status = 'ACTIVE'")) {
                if ("ACTIVE".equals(target.get("sale_status"))) return result(List.of(record(target)));
                if (old != null && "ACTIVE".equals(old.get("sale_status"))) return result(List.of(record(old)));
            }
            if (sql.contains("audit.append_fact")) audits.add(Arrays.asList(binds));
            if (sql.contains("outbox_append")) outboxes.add(Arrays.asList(binds));
            return result(List.of());
        });
        when(dsl.execute(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0); Object[] binds = (Object[]) invocation.getRawArguments()[1]; statements.add(sql);
            if (sql.contains("SET published_at=?")) { target.put("published_at", NOW); target.put("sale_status", "ACTIVE"); target.put("revision", (int) target.get("revision") + 1); }
            else { Map<String, Object> row = old != null && old.get("id").equals(binds[0]) ? old : target; row.put("sale_status", "OFFSALE"); row.put("revision", (int) row.get("revision") + 1); }
            return 1;
        });
        witness = repository.begin(); role = "operations_iam_role"; repository.catalogRole(witness);
    }
    static Map<String, Object> valid() {
        var values = new HashMap<String, Object>();
        values.put("id", VERSION); values.put("product_id", PRODUCT); values.put("version_no", 1); values.put("sale_status", "DRAFT");
        values.put("offer_type", "ONE_TIME"); values.put("audience", "PERSONAL"); values.put("price_cents", 100L); values.put("currency", "CNY"); values.put("billing_cycle", "ONCE");
        values.put("installments", 1); values.put("grant_interval_months", 0); values.put("snapshot", "{}"); values.put("snapshot_sha256", HASH);
        values.put("created_by_employee_id", new UUID(2, 38)); values.put("created_at", Instant.parse("2026-11-18T00:00:00Z")); values.put("revision", 1);
        return values;
    }
    static Record record(Map<String, Object> values) {
        return mock(Record.class, invocation -> {
            if (invocation.getMethod().getName().equals("get") && invocation.getArgument(0) instanceof String field) return values.get(field);
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }
    static Result<Record> result(List<Record> rows) { Result<Record> result = mock(Result.class); when(result.size()).thenReturn(rows.size()); when(result.isEmpty()).thenReturn(rows.isEmpty()); when(result.getFirst()).thenAnswer(invocation -> rows.getFirst()); when(result.iterator()).thenAnswer(invocation -> rows.iterator()); return result; }
    @Override public void close() { TransactionSynchronizationManager.unbindResourceIfPossible(source); TransactionSynchronizationManager.clear(); }
    CatalogVersionState publish(UUID active, int revision) { return repository.publish(VERSION, active, revision, new UUID(2, 38), new UUID(3, 38)); }
    void oldActive() { old = valid(); old.put("id", new UUID(9, 38)); old.put("sale_status", "ACTIVE"); old.put("published_at", NOW.minusSeconds(60)); }
    void active() { target.put("sale_status", "ACTIVE"); target.put("published_at", NOW); }
    int first(String token) { for (int index = 0; index < statements.size(); index++) if (statements.get(index).contains(token)) return index; return -1; }
}
