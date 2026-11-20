package com.onecall.core.catalog_order.internal.operationsdraft;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;

import com.onecall.core.operations_iam.employeeauthority.EmployeeAuthorityReadPort.TransactionWitness;
import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
final class JooqCatalogDraftRepository implements CatalogDraftRepository {
    private static final String COLUMNS = "id, product_id, version_no, sale_status, offer_type, audience, price_cents, currency, billing_cycle, installments, grant_interval_months, snapshot, snapshot_sha256, created_by_employee_id, created_at, published_at, revision";
    private final DataSource dataSource;
    private final DSLContext dsl;
    private final ThreadLocal<Connection> connections = new ThreadLocal<>();

    JooqCatalogDraftRepository(@Qualifier("operationsSubjectReadDataSource") DataSource dataSource,
            @Qualifier("operationsSubjectReadDsl") DSLContext dsl) { this.dataSource = dataSource; this.dsl = dsl; }

    @Override public TransactionWitness begin() {
        connections.remove();
        Connection connection = connection();
        dsl.execute("SET LOCAL statement_timeout = '2000ms'");
        dsl.execute("SET LOCAL lock_timeout = '250ms'");
        dsl.execute("SET LOCAL idle_in_transaction_session_timeout = '5000ms'");
        Record identity = identity(); String login = identity.get("login", String.class);
        if (login == null || !login.equals(identity.get("role", String.class))) throw unavailable();
        var witness = new TransactionWitness(login, identity.get("backend", Integer.class), identity.get("transaction", Long.class));
        connections.set(connection);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { connections.remove(); }
        });
        assertIdentity(witness, login);
        var limits = dsl.fetchSingle("SELECT current_setting('statement_timeout') AS statement, current_setting('lock_timeout') AS lock, current_setting('idle_in_transaction_session_timeout') AS idle");
        if (!"2s".equals(limits.get("statement", String.class)) || !"250ms".equals(limits.get("lock", String.class)) || !"5s".equals(limits.get("idle", String.class))) throw unavailable();
        return witness;
    }
    @Override public void catalogRole(TransactionWitness witness) {
        assertIdentity(witness, "operations_iam_role");
        dsl.execute("SET LOCAL ROLE catalog_order_role");
        assertIdentity(witness, "catalog_order_role");
    }
    @Override public void restore(TransactionWitness witness) {
        try {
            String role = identity().get("role", String.class);
            if (witness.login().equals(role)) { assertIdentity(witness, role); return; }
            if (!"operations_iam_role".equals(role) && !"catalog_order_role".equals(role)) throw unavailable();
            assertIdentity(witness, role);
            dsl.execute("SET LOCAL ROLE NONE");
            assertIdentity(witness, witness.login());
        } catch (RuntimeException cleanup) { throw unavailable(); }
        finally { connections.remove(); }
    }
    private Connection connection() {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !(TransactionSynchronizationManager.getResource(dataSource) instanceof ConnectionHolder holder)) throw unavailable();
        Connection connection = holder.getConnection();
        try { if (connection.getAutoCommit() || connection.isReadOnly() || connection.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED) throw unavailable(); }
        catch (java.sql.SQLException failure) { throw unavailable(); }
        return connection;
    }
    private Record identity() { return dsl.fetchSingle("SELECT session_user AS login, current_user AS role, pg_backend_pid() AS backend, txid_current() AS transaction, current_setting('transaction_read_only') AS readonly"); }
    private void assertIdentity(TransactionWitness witness, String role) {
        if (connection() != connections.get()) throw unavailable();
        Record identity = identity();
        if (!witness.login().equals(identity.get("login", String.class)) || !role.equals(identity.get("role", String.class))
                || witness.backendPid() != identity.get("backend", Integer.class) || witness.transactionId() != identity.get("transaction", Long.class)
                || !"off".equals(identity.get("readonly", String.class))) throw unavailable();
    }
    private void lockProduct(UUID productId) { dsl.fetch("SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))", "catalog_order.publication:" + productId); }
    private boolean readProduct(UUID productId) {
        Record row = dsl.fetchOne("SELECT id FROM catalog_order.catalog_product WHERE id = ?", productId);
        if (row == null) return false;
        if (!productId.equals(row.get("id", UUID.class))) throw unavailable();
        return true;
    }
    private StoredDraft readNaturalDraft(UUID productId, int versionNo) {
        Record row = dsl.fetchOne("SELECT " + COLUMNS + " FROM catalog_order.catalog_version WHERE product_id = ? AND version_no = ? FOR UPDATE", productId, versionNo);
        if (row == null) return null;
        StoredDraft draft = stored(row);
        if (!productId.equals(draft.productId()) || versionNo != draft.versionNo()) throw unavailable();
        return draft;
    }
    private StoredDraft readDraft(UUID versionId, UUID productId) {
        Record row = dsl.fetchOne("SELECT " + COLUMNS + " FROM catalog_order.catalog_version WHERE id = ? FOR UPDATE", versionId);
        if (row == null) throw new Failure(Code.NOT_FOUND);
        StoredDraft draft = stored(row);
        if (!versionId.equals(draft.id()) || !productId.equals(draft.productId())) throw unavailable();
        return draft;
    }
    private StoredDraft stored(Record row) {
        UUID versionId = row.get("id", UUID.class);
        return new StoredDraft(versionId, row.get("product_id", UUID.class), row.get("version_no", Integer.class), row.get("sale_status", String.class),
                new Commercial(row.get("offer_type", String.class), row.get("audience", String.class), row.get("price_cents", Long.class), row.get("currency", String.class),
                        row.get("billing_cycle", String.class), row.get("installments", Integer.class), row.get("grant_interval_months", Integer.class)),
                row.get("snapshot", String.class), row.get("snapshot_sha256", String.class), row.get("created_by_employee_id", UUID.class), instant(row.get("created_at")),
                instant(row.get("published_at")), row.get("revision", Integer.class), readBenefits(versionId), readOffers(versionId));
    }
    private List<StoredBenefit> readBenefits(UUID versionId) {
        return dsl.fetch("SELECT id, catalog_version_id, entitlement_type, quantity, validity_days, grant_interval_months FROM catalog_order.catalog_benefit_line WHERE catalog_version_id = ? ORDER BY entitlement_type COLLATE \"C\"", versionId)
                .map(row -> new StoredBenefit(row.get("id", UUID.class), row.get("catalog_version_id", UUID.class), new BenefitLine(row.get("entitlement_type", String.class),
                        row.get("quantity", Long.class), row.get("validity_days", Integer.class), row.get("grant_interval_months", Integer.class))));
    }
    private List<StoredOffer> readOffers(UUID versionId) {
        return dsl.fetch("SELECT id, catalog_version_id, channel, offer_status, version_token, created_at, revision FROM catalog_order.catalog_offer WHERE catalog_version_id = ? ORDER BY channel COLLATE \"C\" FOR UPDATE", versionId)
                .map(row -> new StoredOffer(row.get("id", UUID.class), row.get("catalog_version_id", UUID.class), row.get("channel", String.class), row.get("offer_status", String.class),
                        row.get("version_token", String.class), instant(row.get("created_at")), row.get("revision", Integer.class)));
    }
    @Override public CreateResult create(CreateCommand command) {
        if (!readProduct(command.productId())) throw new Failure(Code.NOT_FOUND);
        lockProduct(command.productId());
        StoredDraft existing = readNaturalDraft(command.productId(), command.versionNo());
        String projection = CatalogDraftSemantics.projection(command.productId(), command.versionNo(), command.commercial(), command.benefitLines(), command.channels());
        if (existing != null) return replayCreate(existing, projection);
        UUID versionId = UUID.randomUUID();
        Instant decision = instant(dsl.fetchSingle("SELECT clock_timestamp() AS decision").get("decision"));
        String hash = CatalogDraftSemantics.sha256(CatalogDraftSemantics.canonicalBytes(projection));
        if (!insertDraft(versionId, command, projection, hash, decision)) {
            existing = readNaturalDraft(command.productId(), command.versionNo());
            if (existing == null) throw unavailable();
            return replayCreate(existing, projection);
        }
        insertBenefits(versionId, command.benefitLines());
        insertOffers(versionId, command.channels(), hash, decision);
        StoredDraft inserted = readDraft(versionId, command.productId());
        CatalogVersionDraft response = CatalogDraftSemantics.response(inserted);
        appendAudit(decision, command.caller(), inserted, true, null);
        return new CreateResult(response, true);
    }
    private CreateResult replayCreate(StoredDraft draft, String projection) {
        String current = CatalogDraftSemantics.validateStored(draft);
        if (!CatalogDraftSemantics.sameProjection(current, projection)) throw new Failure(Code.CATALOG_DRAFT_CONFLICT);
        return new CreateResult(CatalogDraftSemantics.response(draft), false);
    }
    @Override public CatalogVersionDraft replace(ReplaceCommand command) { return replace(command, false); }
    @Override public CatalogVersionDraft recoverReplace(ReplaceCommand command) { return replace(command, true); }
    private CatalogVersionDraft replace(ReplaceCommand command, boolean recovery) {
        Record preliminary = dsl.fetchOne("SELECT id, product_id FROM catalog_order.catalog_version WHERE id = ?", command.versionId());
        if (preliminary == null) throw new Failure(Code.NOT_FOUND);
        UUID productId = preliminary.get("product_id", UUID.class);
        if (productId == null || !command.versionId().equals(preliminary.get("id", UUID.class))) throw unavailable();
        lockProduct(productId);
        StoredDraft current = readDraft(command.versionId(), productId);
        String before = CatalogDraftSemantics.validateStored(current);
        String projection = CatalogDraftSemantics.projection(productId, current.versionNo(), command.commercial(), current.benefits().stream().map(StoredBenefit::line).toList(), current.offers().stream().map(StoredOffer::channel).toList());
        if ((long) current.revision() == (long) command.expectedRevision() + 1 && CatalogDraftSemantics.sameProjection(before, projection)) return CatalogDraftSemantics.response(current);
        if (current.revision() != command.expectedRevision()) throw new Failure(Code.CATALOG_DRAFT_STALE_VIEW);
        if (recovery) throw unavailable();
        int revision = CatalogDraftSemantics.increment(current.revision());
        for (StoredOffer offer : current.offers()) CatalogDraftSemantics.increment(offer.revision());
        String hash = CatalogDraftSemantics.sha256(CatalogDraftSemantics.canonicalBytes(projection));
        Instant decision = instant(dsl.fetchSingle("SELECT clock_timestamp() AS decision").get("decision"));
        int changed = updateDraft(current, command.commercial(), projection, hash);
        if (changed == 0) {
            StoredDraft reread = readDraft(command.versionId(), productId);
            String persisted = CatalogDraftSemantics.validateStored(reread);
            String requested = CatalogDraftSemantics.projection(productId, reread.versionNo(), command.commercial(), reread.benefits().stream().map(StoredBenefit::line).toList(), reread.offers().stream().map(StoredOffer::channel).toList());
            if ((long) reread.revision() == (long) command.expectedRevision() + 1 && CatalogDraftSemantics.sameProjection(persisted, requested)) return CatalogDraftSemantics.response(reread);
            if (reread.revision() != command.expectedRevision()) throw new Failure(Code.CATALOG_DRAFT_STALE_VIEW);
            throw unavailable();
        }
        exactlyOne(changed);
        regenerateTokens(current, hash, revision);
        StoredDraft updated = readDraft(command.versionId(), productId);
        CatalogVersionDraft response = CatalogDraftSemantics.response(updated);
        appendAudit(decision, command.caller(), updated, false, CatalogDraftSemantics.auditSnapshot(productId, current.versionNo(), current.revision(), current.hash()));
        return response;
    }
    @Override public CreateResult recoverCreate(CreateCommand command) {
        lockProduct(command.productId());
        StoredDraft current = readNaturalDraft(command.productId(), command.versionNo());
        if (current == null) throw new Failure(Code.CATALOG_DRAFT_CONFLICT);
        return replayCreate(current, CatalogDraftSemantics.projection(command.productId(), command.versionNo(), command.commercial(), command.benefitLines(), command.channels()));
    }
    private boolean insertDraft(UUID versionId, CreateCommand command, String snapshot, String hash, Instant createdAt) {
        Commercial commercial = command.commercial();
        return dsl.fetchOne("INSERT INTO catalog_order.catalog_version (id, product_id, version_no, sale_status, offer_type, audience, price_cents, currency, billing_cycle, installments, grant_interval_months, snapshot, snapshot_sha256, created_by_employee_id, created_at, published_at, revision) VALUES (?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?::timestamptz, NULL, 1) ON CONFLICT ON CONSTRAINT uq_catalog_version_no DO NOTHING RETURNING id",
                versionId, command.productId(), command.versionNo(), commercial.offerType(), commercial.audience(), commercial.priceCents(), commercial.currency(), commercial.billingCycle(), commercial.installments(), commercial.grantIntervalMonths(),
                snapshot, hash, command.caller().employeeId(), OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC)) != null;
    }
    private void insertBenefits(UUID versionId, List<BenefitLine> benefits) {
        for (BenefitLine benefit : benefits.stream().sorted(Comparator.comparing(BenefitLine::entitlementType)).toList()) exactlyOne(dsl.execute("INSERT INTO catalog_order.catalog_benefit_line (id, catalog_version_id, entitlement_type, quantity, validity_days, grant_interval_months) VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), versionId, benefit.entitlementType(), benefit.quantity(), benefit.validityDays(), benefit.grantIntervalMonths()));
    }
    private void insertOffers(UUID versionId, List<String> channels, String hash, Instant createdAt) {
        for (String channel : channels.stream().sorted().toList()) exactlyOne(dsl.execute("INSERT INTO catalog_order.catalog_offer (id, catalog_version_id, channel, offer_status, version_token, created_at, revision) VALUES (?, ?, ?, 'ACTIVE', ?, ?::timestamptz, 1)",
                UUID.randomUUID(), versionId, channel, CatalogDraftSemantics.token(versionId, channel, 1, hash), OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC)));
    }
    private int updateDraft(StoredDraft current, Commercial commercial, String snapshot, String hash) {
        return dsl.execute("UPDATE catalog_order.catalog_version SET offer_type=?, audience=?, price_cents=?, currency=?, billing_cycle=?, installments=?, grant_interval_months=?, snapshot=?::jsonb, snapshot_sha256=?, revision=revision+1 WHERE id=? AND product_id=? AND sale_status='DRAFT' AND published_at IS NULL AND revision=?",
                commercial.offerType(), commercial.audience(), commercial.priceCents(), commercial.currency(), commercial.billingCycle(), commercial.installments(), commercial.grantIntervalMonths(), snapshot, hash, current.id(), current.productId(), current.revision());
    }
    private void regenerateTokens(StoredDraft current, String hash, int revision) {
        for (StoredOffer offer : current.offers()) exactlyOne(dsl.execute("UPDATE catalog_order.catalog_offer SET version_token=?, revision=revision+1 WHERE id=? AND catalog_version_id=? AND channel=? AND offer_status='ACTIVE' AND revision=?",
                CatalogDraftSemantics.token(current.id(), offer.channel(), revision, hash), offer.id(), current.id(), offer.channel(), offer.revision()));
    }
    private void appendAudit(Instant decision, Caller caller, StoredDraft draft, boolean create, String before) {
        exactlyOne(dsl.fetch("SELECT audit.append_fact(?::timestamptz, 'EMPLOYEE', ?, ?, 'CATALOG_VERSION', ?, 'ACTION_DERIVED', ?::jsonb, ?::jsonb, ?, ?)",
                OffsetDateTime.ofInstant(decision, ZoneOffset.UTC), caller.employeeId(), create ? "CATALOG_DRAFT_CREATE" : "CATALOG_DRAFT_REPLACE", draft.id(), before,
                CatalogDraftSemantics.auditSnapshot(draft.productId(), draft.versionNo(), draft.revision(), draft.hash()), CatalogDraftSemantics.auditRequestId(draft.id(), create, draft.revision()), caller.traceId().toString()).size());
    }
    private static void exactlyOne(int count) { if (count != 1) throw unavailable(); }
    private static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
    private static Instant instant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime offset) return offset.toInstant();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        throw unavailable();
    }
    record StoredDraft(UUID id, UUID productId, int versionNo, String saleStatus, Commercial commercial, String snapshot, String hash,
            UUID creator, Instant createdAt, Instant publishedAt, int revision, List<StoredBenefit> benefits, List<StoredOffer> offers) {
        StoredDraft { benefits = List.copyOf(benefits); offers = List.copyOf(offers); }
    }
    record StoredBenefit(UUID id, UUID versionId, BenefitLine line) { }
    record StoredOffer(UUID id, UUID versionId, String channel, String status, String token, Instant createdAt, int revision) { }
}
