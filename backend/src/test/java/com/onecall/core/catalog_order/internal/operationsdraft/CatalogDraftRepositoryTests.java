package com.onecall.core.catalog_order.internal.operationsdraft;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.TransactionWitness;
import java.sql.Connection;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CatalogDraftRepositoryTests {
    final DataSource source = mock(DataSource.class);
    final Connection connection = mock(Connection.class);
    final DSLContext dsl = mock(DSLContext.class);
    final JooqCatalogDraftRepository repository = new JooqCatalogDraftRepository(source, dsl);
    final List<String> sql = new ArrayList<>();
    final List<List<Object>> audits = new ArrayList<>();
    final List<Map<String, Object>> benefits = new ArrayList<>(), offers = new ArrayList<>();
    final Caller caller = new Caller(CatalogDraftSemanticsTests.EMPLOYEE, new UUID(0, 5));
    final CreateCommand create = new CreateCommand(CatalogDraftSemanticsTests.PRODUCT, 1, CatalogDraftSemanticsTests.COMMERCIAL, List.of(new BenefitLine("AI_SECONDS", 1, 1, 0)), List.of("WEB", "MINIAPP"), caller);
    Map<String, Object> row;
    String role = "login";
    boolean zeroInsert, zeroUpdate;
    java.util.function.Consumer<Map<String, Object>> zeroUpdateOutcome = current -> {};
    int auditRows = 1;
    TransactionWitness witness;
    @BeforeEach void setup() throws Exception {
        TransactionSynchronizationManager.setActualTransactionActive(true); TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.bindResource(source, new ConnectionHolder(connection)); when(connection.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        when(dsl.execute(anyString())).thenAnswer(invocation -> { String statement = invocation.getArgument(0); sql.add(statement); if (statement.startsWith("SET LOCAL ROLE ")) role = statement.endsWith("NONE") ? "login" : statement.substring(15); return 0; });
        when(dsl.fetchSingle(anyString())).thenAnswer(invocation -> { String statement = invocation.getArgument(0); sql.add(statement); if (statement.contains("clock_timestamp")) return record(Map.of("decision", CatalogDraftSemanticsTests.NOW)); if (statement.contains("AS statement")) return record(Map.of("statement", "2s", "lock", "250ms", "idle", "5s")); return record(Map.of("login", "login", "role", role, "backend", 1, "transaction", 2L, "readonly", "off")); });
        when(dsl.fetchOne(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String statement = invocation.getArgument(0); Object[] binds = (Object[]) invocation.getRawArguments()[1]; sql.add(statement);
            if (statement.contains("FROM catalog_order.catalog_product")) return record(Map.of("id", create.productId()));
            if (statement.startsWith("INSERT")) {
                row = new HashMap<>(); String[] fields = {"id", "product_id", "version_no", "offer_type", "audience", "price_cents", "currency", "billing_cycle", "installments", "grant_interval_months", "snapshot", "snapshot_sha256", "created_by_employee_id", "created_at"};
                for (int index = 0; index < fields.length; index++) row.put(fields[index], binds[index]); row.put("sale_status", "DRAFT"); row.put("revision", 1);
                return zeroInsert ? null : record(row);
            }
            return row == null ? null : record(row);
        });
        when(dsl.fetch(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String statement = invocation.getArgument(0); Object[] binds = (Object[]) invocation.getRawArguments()[1]; sql.add(statement);
            if (statement.contains("catalog_benefit_line")) return result(benefits);
            if (statement.contains("catalog_offer")) return result(offers);
            if (statement.contains("audit.append_fact")) { audits.add(Arrays.asList(binds)); return result(Collections.nCopies(auditRows, Map.of("id", UUID.randomUUID()))); }
            return result(List.of());
        });
        when(dsl.execute(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String statement = invocation.getArgument(0); Object[] binds = (Object[]) invocation.getRawArguments()[1]; sql.add(statement);
            if (statement.startsWith("INSERT INTO catalog_order.catalog_benefit_line")) benefits.add(values(new String[] {"id", "catalog_version_id", "entitlement_type", "quantity", "validity_days", "grant_interval_months"}, binds));
            else if (statement.startsWith("INSERT INTO catalog_order.catalog_offer")) { var offer = values(new String[] {"id", "catalog_version_id", "channel", "version_token", "created_at"}, binds); offer.put("offer_status", "ACTIVE"); offer.put("revision", 1); offers.add(offer); }
            else if (statement.startsWith("UPDATE catalog_order.catalog_version")) { if (zeroUpdate) { zeroUpdateOutcome.accept(row); return 0; } String[] fields = {"offer_type", "audience", "price_cents", "currency", "billing_cycle", "installments", "grant_interval_months", "snapshot", "snapshot_sha256"}; for (int index = 0; index < fields.length; index++) row.put(fields[index], binds[index]); row.put("revision", (int) row.get("revision") + 1); }
            else if (statement.startsWith("UPDATE catalog_order.catalog_offer")) { var offer = offers.stream().filter(value -> value.get("id").equals(binds[1])).findFirst().orElseThrow(); offer.put("version_token", binds[0]); offer.put("revision", (int) offer.get("revision") + 1); }
            return 1;
        });
        witness = repository.begin(); role = "operations_iam_role"; repository.catalogRole(witness); sql.clear();
    }
    static Map<String, Object> values(String[] fields, Object[] binds) { var result = new HashMap<String, Object>(); for (int index = 0; index < fields.length; index++) result.put(fields[index], binds[index]); return result; }
    static Record record(Map<String, Object> values) { return mock(Record.class, invocation -> invocation.getMethod().getName().equals("get") && invocation.getArgument(0) instanceof String name ? values.get(name) : org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation)); }
    @SuppressWarnings("unchecked") static Result<Record> result(List<Map<String, Object>> values) {
        Result<Record> result = mock(Result.class);
        when(result.size()).thenReturn(values.size());
        when(result.map(any(RecordMapper.class))).thenAnswer(invocation -> { RecordMapper<Record, Object> mapper = invocation.getArgument(0); return values.stream().map(value -> mapper.map(record(value))).toList(); });
        return result;
    }
    ReplaceCommand replacement() { return new ReplaceCommand((UUID) row.get("id"), 1, new Commercial("ADD_ON", "PERSONAL", 9L, "CNY", "ONCE", 1, 0), caller); }
    @AfterEach void clear() { TransactionSynchronizationManager.unbindResourceIfPossible(source); TransactionSynchronizationManager.clear(); }
    @Test void requiresWritableSharedPhysicalTransactionWitness() { TransactionSynchronizationManager.setCurrentTransactionReadOnly(true); CatalogDraftSemanticsTests.failure(repository::begin, Code.SERVICE_UNAVAILABLE); TransactionSynchronizationManager.setCurrentTransactionReadOnly(false); TransactionSynchronizationManager.setActualTransactionActive(false); CatalogDraftSemanticsTests.failure(repository::begin, Code.SERVICE_UNAVAILABLE); }
    @Test void assertsExactRoleSequenceAndLocalTimeouts() { repository.restore(witness); sql.clear(); witness = repository.begin(); role = "operations_iam_role"; repository.catalogRole(witness); repository.restore(witness); assertThat(sql).containsSubsequence("SET LOCAL statement_timeout = '2000ms'", "SET LOCAL lock_timeout = '250ms'", "SET LOCAL idle_in_transaction_session_timeout = '5000ms'", "SET LOCAL ROLE catalog_order_role", "SET LOCAL ROLE NONE"); assertThat(role).isEqualTo("login"); }
    @Test void locksExistingPublicationKeyBeforeCatalogDecision() { repository.create(create); int lock = sql.indexOf("SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))"); assertThat(lock).isGreaterThanOrEqualTo(0); assertThat(sql.get(lock - 1)).contains("catalog_product"); assertThat(sql.get(lock + 1)).contains("product_id = ? AND version_no = ? FOR UPDATE"); verify(dsl).fetch("SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))", "catalog_order.publication:" + create.productId()); }
    @Test void createUsesNaturalConstraintAndExplicitInsertColumns() { assertThat(repository.create(create).created()).isTrue(); assertThat(sql.stream().filter(statement -> statement.startsWith("INSERT INTO catalog_order.catalog_version"))).singleElement().satisfies(statement -> assertThat(statement).contains("created_by_employee_id, created_at, published_at, revision", "ON CONFLICT ON CONSTRAINT uq_catalog_version_no DO NOTHING RETURNING id")); assertThat(row.get("created_by_employee_id")).isEqualTo(caller.employeeId()); }
    @Test void createInsertsCanonicalChildrenExactlyOnce() { repository.create(create); assertThat(benefits).hasSize(1); assertThat(offers).extracting(value -> value.get("channel")).containsExactly("MINIAPP", "WEB"); assertThat(offers).allSatisfy(value -> { assertThat(value.get("revision")).isEqualTo(1); assertThat(value.get("version_token").toString()).matches("[0-9a-f]{64}"); }); }
    @Test void createReplayAndConflictUseNoDmlOrAudit() { repository.create(create); sql.clear(); assertThat(repository.create(create).created()).isFalse(); CatalogDraftSemanticsTests.failure(() -> repository.create(new CreateCommand(create.productId(), 1, new Commercial("ADD_ON", "PERSONAL", 1L, "CNY", "ONCE", 1, 0), create.benefitLines(), create.channels(), caller)), Code.CATALOG_DRAFT_CONFLICT); assertThat(sql).noneMatch(statement -> statement.startsWith("INSERT") || statement.startsWith("UPDATE") || statement.contains("append_fact")); assertThat(audits).hasSize(1); }
    @Test void replaceSetsOnlyAllowedVersionColumnsConditionally() { repository.create(create); sql.clear(); assertThat(repository.replace(replacement()).revision()).isEqualTo(2); assertThat(sql.stream().filter(statement -> statement.startsWith("UPDATE catalog_order.catalog_version"))).singleElement().satisfies(statement -> assertThat(statement).contains("offer_type=?, audience=?, price_cents=?, currency=?, billing_cycle=?, installments=?, grant_interval_months=?, snapshot=?::jsonb, snapshot_sha256=?, revision=revision+1", "WHERE id=? AND product_id=? AND sale_status='DRAFT' AND published_at IS NULL AND revision=?").doesNotContain("SET product_id", "created_at=")); }
    @Test void replaceOnlyRegeneratesExistingOfferTokensAndRevisions() { repository.create(create); var before = offers.stream().map(Map::copyOf).toList(); repository.replace(replacement()); for (int index = 0; index < offers.size(); index++) { var offer = offers.get(index); assertThat(offer.get("id")).isEqualTo(before.get(index).get("id")); assertThat(offer.get("channel")).isEqualTo(before.get(index).get("channel")); assertThat(offer.get("revision")).isEqualTo(2); assertThat(offer.get("version_token")).isNotEqualTo(before.get(index).get("version_token")); } }
    @Test void neverRewritesBenefitsOrImmutableOfferFacts() { repository.create(create); String before = benefits.toString(); sql.clear(); repository.replace(replacement()); assertThat(benefits.toString()).isEqualTo(before); assertThat(sql).noneMatch(statement -> statement.startsWith("DELETE") || statement.startsWith("INSERT") || statement.startsWith("UPDATE catalog_order.catalog_benefit_line")); }
    @Test void appendsExactlyOneActionDerivedAuditAndNoOutbox() { repository.create(create); repository.replace(replacement()); assertThat(audits).hasSize(2); assertThat(audits.getFirst().get(2)).isEqualTo("CATALOG_DRAFT_CREATE"); assertThat(audits.getFirst().get(4)).isNull(); assertThat(audits.getLast().get(2)).isEqualTo("CATALOG_DRAFT_REPLACE"); assertThat(audits.getLast().get(6)).isEqualTo("catalog_version:" + row.get("id") + ":draft:replace:2"); assertThat(audits.getLast().get(7)).isEqualTo(caller.traceId().toString()); assertThat(sql).noneMatch(statement -> statement.contains("outbox") || statement.contains("idempotency"));
        for (int count : List.of(0, 2)) { auditRows = count; CatalogDraftSemanticsTests.failure(() -> repository.replace(new ReplaceCommand((UUID) row.get("id"), (int) row.get("revision"), create.commercial(), caller)), Code.SERVICE_UNAVAILABLE); }
    }
    @Test void zeroRowsAndRawUniqueRacesUseBoundedReread() { zeroInsert = true; var empty = new CreateCommand(create.productId(), 1, create.commercial(), List.of(), List.of(), caller); assertThat(repository.create(empty).created()).isFalse(); assertThat(audits).isEmpty(); assertThat(sql.stream().filter(statement -> statement.contains("product_id = ? AND version_no = ? FOR UPDATE"))).hasSize(2); sql.clear(); assertThat(repository.recoverCreate(empty).created()).isFalse(); assertThat(sql).noneMatch(statement -> statement.startsWith("INSERT") || statement.startsWith("UPDATE") || statement.contains("append_fact"));
        zeroUpdate = true; sql.clear(); String before = row.toString();
        CatalogDraftSemanticsTests.failure(() -> repository.replace(replacement()), Code.SERVICE_UNAVAILABLE);
        assertThat(row.toString()).isEqualTo(before); assertThat(audits).isEmpty();
        assertThat(sql.stream().filter(statement -> statement.startsWith("UPDATE catalog_order.catalog_version"))).hasSize(1);
        assertThat(sql.stream().filter(statement -> statement.contains("WHERE id = ? FOR UPDATE"))).hasSize(2);
        assertThat(sql).noneMatch(statement -> statement.startsWith("UPDATE catalog_order.catalog_offer") || statement.contains("append_fact"));
        var original = new HashMap<>(row);
        record Outcome(String name, java.util.function.Consumer<Map<String, Object>> reread, Commercial requested, Code code) {}
        for (Outcome outcome : List.of(
                new Outcome("unchanged", current -> {}, empty.commercial(), Code.SERVICE_UNAVAILABLE),
                new Outcome("equal successor", current -> current.put("revision", 2), empty.commercial(), null),
                new Outcome("different successor", current -> current.put("revision", 2), replacement().commercial(), Code.CATALOG_DRAFT_STALE_VIEW),
                new Outcome("other revision", current -> current.put("revision", 3), empty.commercial(), Code.CATALOG_DRAFT_STALE_VIEW),
                new Outcome("non-DRAFT before stale", current -> { current.put("revision", 3); current.put("sale_status", "ACTIVE"); current.put("published_at", CatalogDraftSemanticsTests.NOW); }, empty.commercial(), Code.CATALOG_DRAFT_CONFLICT),
                new Outcome("legacy before stale", current -> { current.put("revision", 3); current.put("snapshot", "{}"); }, empty.commercial(), Code.CATALOG_DRAFT_CONFLICT),
                new Outcome("corrupt before stale", current -> { current.put("revision", 3); current.put("snapshot_sha256", "0".repeat(64)); }, empty.commercial(), Code.SERVICE_UNAVAILABLE),
                new Outcome("corrupt before non-DRAFT", current -> { current.put("sale_status", "ACTIVE"); current.put("published_at", CatalogDraftSemanticsTests.NOW); current.put("snapshot_sha256", "invalid"); }, empty.commercial(), Code.SERVICE_UNAVAILABLE))) {
            row = new HashMap<>(original); zeroUpdateOutcome = outcome.reread(); sql.clear();
            var request = new ReplaceCommand((UUID) row.get("id"), 1, outcome.requested(), caller);
            if (outcome.code() == null) assertThat(repository.replace(request).revision()).as(outcome.name()).isEqualTo(2);
            else CatalogDraftSemanticsTests.failure(() -> repository.replace(request), outcome.code());
            assertThat(sql.stream().filter(statement -> statement.startsWith("UPDATE catalog_order.catalog_version"))).as(outcome.name()).hasSize(1);
            assertThat(sql.stream().filter(statement -> statement.contains("WHERE id = ? FOR UPDATE"))).as(outcome.name()).hasSize(2);
            assertThat(sql).noneMatch(statement -> statement.startsWith("UPDATE catalog_order.catalog_offer") || statement.contains("append_fact"));
            assertThat(audits).isEmpty(); assertThat(offers).isEmpty(); assertThat(benefits).isEmpty();
            var expected = new HashMap<>(original); outcome.reread().accept(expected); assertThat(row).isEqualTo(expected);
        }
    }
    @Test void restoresRoleAndDiscardsWitnessOnEveryExit() { repository.restore(witness); assertThat(role).isEqualTo("login"); CatalogDraftSemanticsTests.failure(() -> repository.catalogRole(witness), Code.SERVICE_UNAVAILABLE); witness = repository.begin(); role = "operations_iam_role"; repository.catalogRole(witness); role = "login"; sql.clear();
        repository.restore(witness); assertThat(sql).doesNotContain("SET LOCAL ROLE NONE");
        CatalogDraftSemanticsTests.failure(() -> repository.catalogRole(witness), Code.SERVICE_UNAVAILABLE);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(2); TransactionSynchronizationManager.getSynchronizations().getFirst().afterCompletion(0); CatalogDraftSemanticsTests.failure(() -> repository.catalogRole(witness), Code.SERVICE_UNAVAILABLE); }
}
