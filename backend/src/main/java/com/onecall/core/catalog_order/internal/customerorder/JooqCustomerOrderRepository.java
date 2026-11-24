package com.onecall.core.catalog_order.internal.customerorder;

import com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService.*;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException.Code;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class JooqCustomerOrderRepository implements CustomerOrderRepository {
    private static final String ORDER_COLUMNS = "id,order_no,account_id,owner_organization_id,subject_id,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,version_token,idempotency_key,request_fingerprint,revision,created_at,paid_at,closed_at";
    private final DataSource source;
    private final DSLContext dsl;
    private final ThreadLocal<Connection> connections = new ThreadLocal<>();
    JooqCustomerOrderRepository(DataSource source, DSLContext dsl) { this.source = source; this.dsl = dsl; }
    @Override public TransactionWitness begin() {
        connections.remove();
        Connection connection = connection();
        var identity = identity();
        String login = identity.get("login", String.class);
        if (login == null || !login.equals(identity.get("role", String.class))) throw unavailable();
        TransactionWitness witness = new TransactionWitness(login, identity.get("backend", Integer.class), identity.get("transaction", Long.class));
        connections.set(connection);
        TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCompletion(int status) { connections.remove(); }
        });
        assertIdentity(witness, login);
        dsl.execute("SET LOCAL statement_timeout = '2000ms'");
        dsl.execute("SET LOCAL lock_timeout = '250ms'");
        dsl.execute("SET LOCAL idle_in_transaction_session_timeout = '5000ms'");
        dsl.execute("SET LOCAL ROLE catalog_order_role");
        assertIdentity(witness, "catalog_order_role");
        return witness;
    }
    @Override public void restore(TransactionWitness witness) {
        try { dsl.execute("SET LOCAL ROLE NONE"); assertIdentity(witness, witness.login()); }
        finally { connections.remove(); }
    }
    private Connection connection() {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                || !(TransactionSynchronizationManager.getResource(source) instanceof ConnectionHolder holder)) throw unavailable();
        Connection connection = holder.getConnection();
        try { if (connection.getAutoCommit() || connection.isReadOnly() || connection.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED) throw unavailable(); }
        catch (java.sql.SQLException failure) { throw unavailable(); }
        return connection;
    }
    private Record identity() { return dsl.fetchSingle("SELECT session_user AS login,current_user AS role,pg_backend_pid() AS backend,txid_current() AS transaction"); }
    private void assertIdentity(TransactionWitness witness, String role) {
        if (connection() != connections.get()) throw unavailable();
        var row = identity();
        if (!witness.login().equals(row.get("login", String.class)) || !role.equals(row.get("role", String.class))
                || witness.backendPid() != row.get("backend", Integer.class) || witness.transactionId() != row.get("transaction", Long.class)) throw unavailable();
    }
    private void lock(String key) { dsl.fetch("SELECT pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))", key); }
    @Override public void lockSubject(UUID subjectId) { lock("subject_number.subject:" + subjectId); }
    private Record subject(UUID subjectId) { return dsl.fetchOne("SELECT id,owner_account_id,owner_organization_id,revision FROM subject_number.business_subject WHERE id=?", subjectId); }
    @Override public void authorize(Command command, Caller caller) {
        Record subject = subject(command.subjectId());
        boolean personal = "PERSONAL".equals(caller.workspaceType());
        if (!personal && !"ORGANIZATION".equals(caller.workspaceType()) || personal != (caller.organizationId() == null)) throw unavailable();
        if (subject == null) throw forbidden();
        UUID owner = subject.get("owner_account_id", UUID.class), organization = subject.get("owner_organization_id", UUID.class);
        if (owner != null && organization != null) throw CustomerOrderSemantics.integrity();
        if (personal ? !caller.accountId().equals(owner) || organization != null : !caller.organizationId().equals(organization) || owner != null) throw forbidden();
        if (!personal) {
            lock("organization_access.organization:" + caller.organizationId());
            var organizationRow = dsl.fetchOne("SELECT id,status FROM organization_access.organization WHERE id=?", caller.organizationId());
            if (organizationRow == null || !"ACTIVE".equals(organizationRow.get("status", String.class))) throw forbidden();
            lock("organization_access.membership:" + caller.organizationId() + ":" + caller.accountId());
            var membership = dsl.fetchOne("SELECT organization_id,account_id,role,status FROM organization_access.organization_membership WHERE organization_id=? AND account_id=? AND status<>'REVOKED'", caller.organizationId(), caller.accountId());
            if (membership == null || !"ACTIVE".equals(membership.get("status", String.class))
                    || !Set.of("ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR").contains(membership.get("role", String.class))) throw forbidden();
        }
        lock("customer_iam.account:" + caller.accountId());
        var account = dsl.fetchOne("SELECT id,status FROM customer_iam.customer_account WHERE id=?", caller.accountId());
        if (account == null || !"ACTIVE".equals(account.get("status", String.class))) throw forbidden();
        var current = subject(command.subjectId());
        if (current == null || !Objects.equals(owner, current.get("owner_account_id", UUID.class)) || !Objects.equals(organization, current.get("owner_organization_id", UUID.class))
                || !subject.get("revision", Integer.class).equals(current.get("revision", Integer.class))) throw forbidden();
        if (personal) {
            var identity = dsl.fetchOne("SELECT account_id,real_name_status FROM customer_iam.identity_profile WHERE account_id=?", caller.accountId());
            if (identity == null || !"VERIFIED".equals(identity.get("real_name_status", String.class))) throw new OrderCreationException(Code.REAL_NAME_REQUIRED);
        }
    }
    @Override public Catalog catalog(UUID offerId) {
        Record row = dsl.fetchOne("SELECT o.id AS offer_id,o.catalog_version_id,o.channel,o.offer_status,o.version_token,v.product_id,v.version_no,v.sale_status,v.offer_type,v.audience,v.price_cents,v.currency,v.billing_cycle,v.installments,v.grant_interval_months,v.snapshot,v.snapshot_sha256,p.product_code FROM catalog_order.catalog_offer o JOIN catalog_order.catalog_version v ON v.id=o.catalog_version_id JOIN catalog_order.catalog_product p ON p.id=v.product_id WHERE o.id=?", offerId);
        return row == null ? null : new Catalog(row.get("offer_id", UUID.class), row.get("catalog_version_id", UUID.class), row.get("product_id", UUID.class), row.get("product_code", String.class), row.get("version_no", Integer.class), row.get("channel", String.class), row.get("offer_status", String.class), row.get("sale_status", String.class), row.get("version_token", String.class), row.get("offer_type", String.class), row.get("audience", String.class), row.get("price_cents", Long.class), row.get("currency", String.class), row.get("billing_cycle", String.class), row.get("installments", Integer.class), row.get("grant_interval_months", Integer.class), row.get("snapshot", String.class), row.get("snapshot_sha256", String.class));
    }
    @Override public void lockPublication(UUID productId) { lock("catalog_order.publication:" + productId); }
    @Override public List<BenefitRow> benefits(UUID versionId) {
        return dsl.fetch("SELECT id,entitlement_type,quantity,validity_days,grant_interval_months FROM catalog_order.catalog_benefit_line WHERE catalog_version_id=? ORDER BY entitlement_type,id", versionId)
                .map(row -> new BenefitRow(row.get("id", UUID.class), row.get("entitlement_type", String.class), row.get("quantity", Long.class), row.get("validity_days", Integer.class), row.get("grant_interval_months", Integer.class)));
    }
    @Override public StoredOrder existing(Caller caller, String key) {
        Record row = dsl.fetchOne("SELECT " + ORDER_COLUMNS + " FROM catalog_order.customer_order WHERE account_id=? AND idempotency_key=?", caller.accountId(), key);
        return row == null ? null : stored(row);
    }
    private StoredOrder stored(Record row) {
        String snapshot = row.get("snapshot", String.class), hash = row.get("snapshot_sha256", String.class);
        CustomerOrderSemantics.storedSnapshot(snapshot);
        if (!CustomerOrderSemantics.sha(CustomerOrderSemantics.canonical(CustomerOrderSemantics.parse(snapshot))).equals(hash)) throw CustomerOrderSemantics.integrity();
        Snapshot projection = CustomerOrderSemantics.projection(snapshot, hash);
        UUID id = row.get("id", UUID.class);
        if (!CustomerOrderSemantics.orderNo(id).equals(row.get("order_no", String.class)) || !projection.catalogVersionId().equals(row.get("catalog_version_id", UUID.class))
                || !projection.catalogOfferId().equals(row.get("catalog_offer_id", UUID.class)) || !projection.channel().equals(row.get("channel", String.class))
                || !projection.versionToken().equals(row.get("version_token", String.class)) || projection.amountCents() != row.get("amount_cents", Long.class)
                || !projection.currency().equals(row.get("currency", String.class))) throw CustomerOrderSemantics.integrity();
        Order order = new Order(id, row.get("order_no", String.class), row.get("subject_id", UUID.class), row.get("channel", String.class), row.get("catalog_version_id", UUID.class),
                row.get("catalog_offer_id", UUID.class), row.get("amount_cents", Long.class), row.get("currency", String.class), "UNPAID", "NONE", row.get("created_at", OffsetDateTime.class).toInstant(), hash, projection, row.get("purchase_consent_id", UUID.class));
        return new StoredOrder(order, row.get("account_id", UUID.class), row.get("owner_organization_id", UUID.class), row.get("request_fingerprint", String.class), snapshot);
    }
    @Override public Idempotency beginIdempotency(String key, String fingerprint) {
        try {
            var row = dsl.fetchSingle("SELECT record_id,revision,outcome,result_type,result_id,result_snapshot,result_sha256 FROM platform.idempotency_begin('PURCHASE',?,?)", key, fingerprint);
            Integer revision = row.get("revision", Integer.class);
            return new Idempotency(row.get("record_id", UUID.class), revision == null ? 0 : revision, row.get("outcome", String.class), row.get("result_type", String.class), row.get("result_id", UUID.class), row.get("result_snapshot", String.class), row.get("result_sha256", String.class));
        } catch (RuntimeException failure) { throw translate(failure, true); }
    }
    @Override public boolean unknown(UUID subjectId, UUID offerId) { return !dsl.fetch("SELECT id FROM catalog_order.customer_order WHERE subject_id=? AND catalog_offer_id=? AND payment_status='UNKNOWN'", subjectId, offerId).isEmpty(); }
    @Override public void verifyConsent(ConsentDescriptor consent) {
        Record row = dsl.fetchOne("SELECT id,subject_id,purpose,channel,event_domain,event_id,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,request_fingerprint,accepted_at FROM activation_consent.consent_record WHERE id=?", consent.id());
        if (row == null || !consent.subjectId().equals(row.get("subject_id", UUID.class)) || !consent.accountId().equals(row.get("account_id", UUID.class))
                || !consent.channel().equals(row.get("channel", String.class)) || !consent.eventId().equals(row.get("event_id", String.class)) || !consent.sourceRef().equals(row.get("source_ref", String.class))
                || !"PURCHASE".equals(row.get("purpose", String.class)) || !"PURCHASE".equals(row.get("event_domain", String.class)) || !"ACCOUNT".equals(row.get("source_type", String.class)) || !"ACCEPT".equals(row.get("action", String.class))
                || !consent.policyCode().equals(row.get("policy_code", String.class)) || !consent.policyVersion().equals(row.get("policy_version", String.class)) || !consent.promptVersion().equals(row.get("prompt_version", String.class))
                || !consent.requestFingerprint().equals(row.get("request_fingerprint", String.class)) || !consent.acceptedAt().equals(row.get("accepted_at", OffsetDateTime.class).toInstant())) throw new OrderCreationException(Code.PURCHASE_CONSENT_REQUIRED);
        if (!dsl.fetch("SELECT id FROM catalog_order.customer_order WHERE purchase_consent_id=?", consent.id()).isEmpty()) throw new OrderCreationException(Code.PURCHASE_CONSENT_REQUIRED);
    }
    @Override public StoredOrder insert(Command command, Caller caller, Catalog catalog, ConsentDescriptor consent, String snapshot, String fingerprint) {
        UUID id = UUID.randomUUID();
        String hash = CustomerOrderSemantics.sha(snapshot);
        Record row = dsl.fetchSingle("INSERT INTO catalog_order.customer_order (" + ORDER_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,'UNPAID',?::jsonb,?,?,?,?,1,transaction_timestamp(),NULL,NULL) RETURNING " + ORDER_COLUMNS,
                id, CustomerOrderSemantics.orderNo(id), caller.accountId(), caller.organizationId(), command.subjectId(), caller.channel(), catalog.versionId(), catalog.offerId(), consent.id(), catalog.priceCents(), catalog.currency(), snapshot, hash, catalog.versionToken(), command.clientIdempotencyKey(), fingerprint);
        return stored(row);
    }
    @Override public void complete(Idempotency record, Order order, String snapshotHash) {
        String outcome = dsl.fetchSingle("SELECT platform.idempotency_complete(?,?,'ORDER',?,?::jsonb,?)", record.id(), record.revision(), order.orderId(), CustomerOrderSemantics.canonical(order), snapshotHash).get(0, String.class);
        if (!"COMPLETED".equals(outcome)) throw CustomerOrderSemantics.integrity();
    }
    private String payload(StoredOrder stored, Caller caller) {
        Order order = stored.order();
        return CustomerOrderSemantics.canonical(CustomerOrderSemantics.fields("orderId", order.orderId(), "orderNo", order.orderNo(), "accountId", caller.accountId(), "ownerOrganizationId", caller.organizationId(),
                "subjectId", order.subjectId(), "channel", order.channel(), "catalogVersionId", order.catalogVersionId(), "catalogOfferId", order.catalogOfferId(), "purchaseConsentId", order.purchaseConsentId(), "amountCents", order.amountCents(),
                "currency", order.currency(), "versionToken", order.snapshot().versionToken(), "snapshotSha256", order.snapshotSha256(), "createdAt", order.createdAt().toString()));
    }
    @Override public void outbox(StoredOrder stored, Caller caller) {
        String payload = payload(stored, caller);
        dsl.fetch("SELECT platform.outbox_append('PURCHASE',?,'CUSTOMER_ORDER',?,'ORDER_CREATED',?::jsonb,NULL,?)", "customer_order:" + stored.order().orderId(), stored.order().orderId(), payload, CustomerOrderSemantics.sha(payload));
    }
    @Override public void audit(StoredOrder stored, Caller caller, UUID traceId) {
        dsl.fetch("SELECT audit.append_fact(?::timestamptz,'ACCOUNT',?,'ORDER_CREATED','CUSTOMER_ORDER',?,NULL,NULL,?::jsonb,NULL,?)", stored.order().createdAt().atOffset(java.time.ZoneOffset.UTC), caller.accountId(), stored.order().orderId(), payload(stored, caller), traceId.toString());
    }
    static OrderCreationException translate(Throwable failure, boolean begin) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure) return unavailable();
            if (cause instanceof org.springframework.transaction.TransactionSystemException || cause instanceof org.springframework.transaction.CannotCreateTransactionException || cause instanceof org.springframework.transaction.TransactionTimedOutException
                    || cause instanceof org.springframework.jdbc.CannotGetJdbcConnectionException) return unavailable();
            if (cause instanceof java.sql.SQLException sql) {
                String state = sql.getSQLState();
                if (state != null && (state.startsWith("08") || Set.of("55P03", "57014", "40P01", "42501", "57P01").contains(state))) return unavailable();
                if ("P0001".equals(state) && begin && "IDEMPOTENCY_INCOMPLETE".equals(serverField(sql, "getMessage"))) return unavailable();
                if ("23505".equals(state) && Set.of("uq_customer_order_idempotency", "uq_consent_event").contains(Objects.toString(serverField(sql, "getConstraint"), ""))) return new OrderCreationException(Code.IDEMPOTENCY_CONFLICT);
                return CustomerOrderSemantics.integrity();
            }
        }
        return CustomerOrderSemantics.integrity();
    }
    private static OrderCreationException forbidden() { return new OrderCreationException(Code.FORBIDDEN); }
    private static String serverField(java.sql.SQLException failure, String field) {
        try {
            Object detail = failure.getClass().getMethod("getServerErrorMessage").invoke(failure);
            return detail == null ? null : (String) detail.getClass().getMethod(field).invoke(detail);
        } catch (ReflectiveOperationException unavailable) { return null; }
    }
    private static OrderCreationException unavailable() { return new OrderCreationException(Code.SERVICE_UNAVAILABLE); }
}
