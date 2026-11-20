package com.onecall.core.catalog_order.internal.operationscommand;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand.*;

import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.TransactionWitness;
import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("persistence")
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
final class JooqCatalogSaleLifecycleRepository implements CatalogSaleLifecycleRepository {
    private static final String RESPONSE = "id, product_id, version_no, sale_status, published_at, revision, snapshot_sha256";
    private static final String CONTENT = "id, product_id, version_no, sale_status, offer_type, audience, price_cents, currency, billing_cycle, installments, grant_interval_months, snapshot, snapshot_sha256, created_by_employee_id, created_at, published_at, revision";
    private final DSLContext dsl;
    private final DataSource dataSource;
    private final ThreadLocal<Connection> connections = new ThreadLocal<>();

    JooqCatalogSaleLifecycleRepository(@Qualifier("operationsSubjectReadDataSource") DataSource dataSource,
            @Qualifier("operationsSubjectReadDsl") DSLContext dsl) { this.dataSource = dataSource; this.dsl = dsl; }

    @Override public TransactionWitness begin() {
        connections.remove();
        Connection connection = connection();
        dsl.execute("SET LOCAL statement_timeout = '2000ms'");
        dsl.execute("SET LOCAL lock_timeout = '250ms'");
        dsl.execute("SET LOCAL idle_in_transaction_session_timeout = '5000ms'");
        var identity = identity();
        String login = identity.get("login", String.class);
        if (login == null || !login.equals(identity.get("role", String.class))) throw unavailable();
        var witness = new TransactionWitness(login, identity.get("backend", Integer.class), identity.get("transaction", Long.class));
        connections.set(connection);
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCompletion(int status) { connections.remove(); }
        });
        assertIdentity(witness, login);
        var limits = dsl.fetchSingle("SELECT current_setting('statement_timeout') AS statement, current_setting('lock_timeout') AS lock, current_setting('idle_in_transaction_session_timeout') AS idle");
        if (!"2s".equals(limits.get("statement", String.class)) || !"250ms".equals(limits.get("lock", String.class))
                || !"5s".equals(limits.get("idle", String.class))) throw unavailable();
        return witness;
    }

    @Override public void catalogRole(TransactionWitness witness) {
        assertIdentity(witness, "operations_iam_role");
        dsl.execute("SET LOCAL ROLE catalog_order_role");
        assertIdentity(witness, "catalog_order_role");
    }

    @Override public void restore(TransactionWitness witness) {
        try {
            dsl.execute("SET LOCAL ROLE NONE");
            assertIdentity(witness, witness.login());
        } catch (RuntimeException cleanup) { throw unavailable(); }
        finally { connections.remove(); }
    }
    @Override public void recoveryRole(TransactionWitness witness) {
        assertIdentity(witness, witness.login());
        dsl.execute("SET LOCAL ROLE catalog_order_role");
        assertIdentity(witness, "catalog_order_role");
    }

    private Connection connection() {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !(TransactionSynchronizationManager.getResource(dataSource) instanceof ConnectionHolder holder)) throw unavailable();
        Connection connection = holder.getConnection();
        try {
            if (connection.getAutoCommit() || connection.isReadOnly() || connection.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED) throw unavailable();
        } catch (java.sql.SQLException failure) { throw unavailable(); }
        return connection;
    }

    private Record identity() {
        return dsl.fetchSingle("SELECT session_user AS login, current_user AS role, pg_backend_pid() AS backend, txid_current() AS transaction, current_setting('transaction_read_only') AS readonly");
    }

    private void assertIdentity(TransactionWitness witness, String role) {
        if (connection() != connections.get()) throw unavailable();
        var identity = identity();
        if (!witness.login().equals(identity.get("login", String.class)) || !role.equals(identity.get("role", String.class))
                || witness.backendPid() != identity.get("backend", Integer.class) || witness.transactionId() != identity.get("transaction", Long.class)
                || !"off".equals(identity.get("readonly", String.class))) throw unavailable();
    }

    @Override public CatalogVersionState publish(UUID versionId, UUID expectedActiveVersionId, int expectedRevision, UUID employeeId, UUID traceId) {
        return transition(versionId, expectedRevision, employeeId, traceId, true, expectedActiveVersionId);
    }

    @Override public CatalogVersionState offsale(UUID versionId, int expectedRevision, UUID employeeId, UUID traceId) {
        return transition(versionId, expectedRevision, employeeId, traceId, false, null);
    }

    private CatalogVersionState transition(UUID versionId, int expectedRevision, UUID employeeId, UUID traceId, boolean publication, UUID expectedActive) {
        Record preliminary = dsl.fetchOne("SELECT id, product_id FROM catalog_order.catalog_version WHERE id = ?", versionId);
        if (preliminary == null) throw new Failure(Code.NOT_FOUND);
        UUID productId = preliminary.get("product_id", UUID.class);
        if (productId == null) throw unavailable();
        dsl.fetch("SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))", CatalogSaleLifecycleSemantics.lockKey(productId));
        Record target = dsl.fetchOne("SELECT " + CONTENT + " FROM catalog_order.catalog_version WHERE id = ?", versionId);
        Record active = active(productId);
        Instant decision = instant(dsl.fetchSingle("SELECT clock_timestamp() AS decision_time").get("decision_time"));
        CatalogSaleLifecycleSemantics.validate(target);
        if (!versionId.equals(target.get("id", UUID.class)) || !productId.equals(target.get("product_id", UUID.class))) throw unavailable();
        String status = target.get("sale_status", String.class);
        int revision = target.get("revision", Integer.class);
        UUID activeId = active == null ? null : active.get("id", UUID.class);
        if ("ACTIVE".equals(status) && !versionId.equals(activeId)) throw unavailable();
        if (publication && "OFFSALE".equals(status) || !publication && "DRAFT".equals(status)) throw new Failure(Code.CATALOG_PUBLICATION_CONFLICT);
        if (publication && !Objects.equals(expectedActive, activeId) || revision != expectedRevision) throw new Failure(Code.PUBLICATION_STALE_VIEW);
        if (publication && "ACTIVE".equals(status) || !publication && "OFFSALE".equals(status)) return state(target, activeId);
        CatalogSaleLifecycleSemantics.increment(revision);
        if (publication) {
            var benefits = dsl.fetch("SELECT id, catalog_version_id, entitlement_type, quantity, validity_days, grant_interval_months FROM catalog_order.catalog_benefit_line WHERE catalog_version_id = ? ORDER BY id", versionId);
            CatalogSaleLifecycleSemantics.validateBenefits(versionId, benefits);
            if (active != null) {
                CatalogSaleLifecycleSemantics.increment(active.get("revision", Integer.class));
                exactlyOne(dsl.execute("UPDATE catalog_order.catalog_version SET sale_status='OFFSALE', revision=revision+1 WHERE id=? AND product_id=? AND sale_status='ACTIVE' AND revision=?", activeId, productId, active.get("revision", Integer.class)));
            }
            exactlyOne(dsl.execute("UPDATE catalog_order.catalog_version SET published_at=?::timestamptz, sale_status='ACTIVE', revision=revision+1 WHERE id=? AND sale_status='DRAFT' AND published_at IS NULL AND revision=?", OffsetDateTime.ofInstant(decision, java.time.ZoneOffset.UTC), versionId, revision));
            String hash = target.get("snapshot_sha256", String.class);
            appendAudit(decision, employeeId, versionId, traceId, true, CatalogSaleLifecycleSemantics.snapshot("DRAFT", revision, hash), CatalogSaleLifecycleSemantics.snapshot("ACTIVE", revision + 1, hash));
            if (active != null) appendOutbox(active, "OFFSALE", false, "CATALOG_VERSION_OFFSALE", instant(active.get("published_at")));
            appendOutbox(target, "ACTIVE", true, "CATALOG_VERSION_PUBLISHED", decision);
        } else {
            exactlyOne(dsl.execute("UPDATE catalog_order.catalog_version SET sale_status='OFFSALE', revision=revision+1 WHERE id=? AND sale_status='ACTIVE' AND revision=?", versionId, revision));
            appendAudit(decision, employeeId, versionId, traceId, false, CatalogSaleLifecycleSemantics.snapshot("ACTIVE", revision, null), CatalogSaleLifecycleSemantics.snapshot("OFFSALE", revision + 1, null));
            appendOutbox(target, "OFFSALE", false, "CATALOG_VERSION_OFFSALE", instant(target.get("published_at")));
        }
        Record updated = dsl.fetchOne("SELECT " + RESPONSE + " FROM catalog_order.catalog_version WHERE id = ?", versionId);
        Record current = active(productId);
        return state(updated, current == null ? null : current.get("id", UUID.class));
    }

    @Override public CatalogVersionState recover(UUID versionId, UUID expectedActiveVersionId, int expectedRevision, boolean publication) {
        Record target = dsl.fetchOne("SELECT " + RESPONSE + " FROM catalog_order.catalog_version WHERE id = ?", versionId);
        if (target == null) throw new Failure(Code.CATALOG_PUBLICATION_CONFLICT);
        Record current = active(target.get("product_id", UUID.class));
        UUID activeId = current == null ? null : current.get("id", UUID.class);
        if (expectedRevision != target.get("revision", Integer.class)
                || publication && (!"ACTIVE".equals(target.get("sale_status", String.class)) || !versionId.equals(activeId) || !Objects.equals(expectedActiveVersionId, activeId))
                || !publication && !"OFFSALE".equals(target.get("sale_status", String.class))) throw new Failure(Code.CATALOG_PUBLICATION_CONFLICT);
        return state(target, activeId);
    }

    private Record active(UUID productId) {
        var rows = dsl.fetch("SELECT " + RESPONSE + " FROM catalog_order.catalog_version WHERE product_id = ? AND sale_status = 'ACTIVE'", productId);
        if (rows.size() > 1) throw unavailable();
        if (rows.isEmpty()) return null;
        Record row = rows.getFirst();
        if (!productId.equals(row.get("product_id", UUID.class)) || row.get("id", UUID.class) == null || row.get("revision", Integer.class) == null
                || row.get("revision", Integer.class) < 1 || row.get("published_at") == null) throw unavailable();
        return row;
    }

    private static void exactlyOne(int count) { if (count != 1) throw unavailable(); }
    private static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
    static Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime offset) return offset.toInstant();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        throw unavailable();
    }
    private static CatalogVersionState state(Record row, UUID active) {
        if (row == null) throw unavailable();
        return new CatalogVersionState(row.get("id", UUID.class), row.get("product_id", UUID.class), row.get("version_no", Integer.class), row.get("sale_status", String.class), row.get("revision", Integer.class), instant(row.get("published_at")), active);
    }
    private void appendAudit(Instant decision, UUID employeeId, UUID versionId, UUID traceId, boolean publication, String before, String after) {
        dsl.fetch("SELECT audit.append_fact(?::timestamptz, 'EMPLOYEE', ?, ?, 'CATALOG_VERSION', ?, 'ACTION_DERIVED', ?::jsonb, ?::jsonb, ?, ?)", OffsetDateTime.ofInstant(decision, java.time.ZoneOffset.UTC), employeeId,
                publication ? "CATALOG_VERSION_PUBLISH" : "CATALOG_VERSION_OFFSALE", versionId, before, after, CatalogSaleLifecycleSemantics.dedupe(versionId, publication), traceId.toString());
    }
    private void appendOutbox(Record row, String status, boolean publication, String event, Instant published) {
        UUID version = row.get("id", UUID.class);
        String payload = CatalogSaleLifecycleSemantics.payload(version, row.get("product_id", UUID.class), row.get("version_no", Integer.class), status, published, row.get("snapshot_sha256", String.class));
        dsl.fetch("SELECT platform.outbox_append('OPERATIONS', ?, 'CATALOG_VERSION', ?, ?, ?::jsonb, NULL, ?)", CatalogSaleLifecycleSemantics.dedupe(version, publication), version, event, payload, CatalogSaleLifecycleSemantics.sha256(payload));
    }
}
