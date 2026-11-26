package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.one;

import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import tools.jackson.databind.JsonNode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CustomerOrdersReadIT {
    CustomerOrdersFixture fixture;
    final List<String> queries = new java.util.concurrent.CopyOnWriteArrayList<>();
    volatile java.util.function.Consumer<ExecuteContext> beforeRead, afterRead;
    @BeforeAll void start() throws Exception { fixture = new CustomerOrdersFixture(); fixture.start(); }
    @AfterAll void stop() { if (fixture != null) fixture.close(); }
    @BeforeEach void reset() throws Exception {
        fixture.reset(); queries.clear(); beforeRead = null; afterRead = null;
        fixture.context.getBean("customerQueryDsl", DSLContext.class).configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext execution) {
                var hook = beforeRead; if (hook != null) hook.accept(execution);
            }
            @Override public void executeEnd(ExecuteContext execution) {
                queries.add(execution.sql());
                try {
                    assertThat(one(execution.connection(), "SELECT current_user||'|'||session_user"))
                            .isEqualTo("customer_iam_role|pa27_customer");
                    assertThat(one(execution.connection(), "SHOW transaction_isolation")).isEqualTo("repeatable read");
                    assertThat(one(execution.connection(), "SHOW transaction_read_only")).isEqualTo("on");
                } catch (Exception failure) { throw new AssertionError(failure); }
                var hook = afterRead; if (hook != null) hook.accept(execution);
            }
        });
    }
    HttpResponse<String> get(String suffix, String token) throws Exception {
        return get(fixture, suffix, token);
    }
    HttpResponse<String> get(CustomerOrdersFixture target, String suffix, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + target.port + CustomerOrdersFixture.ROUTE + suffix));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return target.client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    JsonNode success(String suffix) throws Exception {
        var response = get(suffix, "web");
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        return fixture.mapper.readTree(response.body());
    }
    String create(String key) throws Exception { return fixture.success(fixture.post(key), 201).get("orderId").asString(); }
    String replaceFixtureOrder(String original, String identity, String amount, String hash, String created) throws Exception {
        fixture.sql("""
                INSERT INTO catalog_order.customer_order(id,order_no,account_id,owner_organization_id,subject_id,channel,
                    catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,
                    snapshot_sha256,version_token,idempotency_key,request_fingerprint,created_at)
                SELECT ?::uuid,'FIXTURE-'||?::text,account_id,owner_organization_id,subject_id,channel,
                    catalog_version_id,catalog_offer_id,purchase_consent_id,%s,currency,payment_status,snapshot,%s,
                    version_token,?::text,request_fingerprint,%s FROM catalog_order.customer_order WHERE id=?::uuid
                """.formatted(amount, hash, created), identity, identity, identity, original);
        fixture.sql("DELETE FROM catalog_order.customer_order WHERE id=?::uuid", original);
        return identity;
    }
    void clean() throws Exception {
        var pool = fixture.context.getBean("customerQueryDataSource", HikariDataSource.class);
        for (int attempt = 0; attempt < 4; attempt++) try (var connection = pool.getConnection()) {
            assertThat(one(connection, "SELECT current_user||'|'||session_user")).isEqualTo("pa27_customer|pa27_customer");
            assertThat(connection.getAutoCommit()).isTrue();
            assertThat(connection.isReadOnly()).isFalse();
            assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
        }
    }
    @Test void postgres16FullChainAndCustomerRuntimeRoleAreUsed() throws Exception {
        assertThat(fixture.value("SELECT version()")).contains("PostgreSQL 16.");
        assertThat(fixture.value("SELECT string_agg(version,',' ORDER BY installed_rank) FROM flyway_schema_history WHERE success"))
                .isEqualTo(java.util.stream.IntStream.rangeClosed(1, 20).mapToObj(index -> "%03d".formatted(index))
                        .collect(java.util.stream.Collectors.joining(",")));
        var before = fixture.databaseInventory();
        var page = success("");
        assertThat(page.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
        assertThat(page.get("items").size()).isZero();
        assertThat(page.get("hasMore").asBoolean()).isFalse();
        assertThat(page.get("nextCursor").isNull()).isTrue();
        assertThat(queries.stream().filter(sql -> sql.contains("entitlement_issuance"))).isEmpty();
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
    }
    @Test void catalogMutationCannotChangeStoredSnapshotProjection() throws Exception {
        String identity = create("unknown");
        fixture.sql("UPDATE catalog_order.customer_order SET payment_status='UNKNOWN' WHERE id=?::uuid", identity);
        var before = fixture.databaseInventory();
        var page = success("");
        var item = page.get("items").get(0);
        assertThat(item.propertyNames()).containsExactlyInAnyOrder("orderId", "orderNo", "subjectId", "channel",
                "catalogVersionId", "catalogOfferId", "amountCents", "currency", "paymentStatus", "issuanceStatus", "createdAt");
        assertThat(item.get("paymentStatus").asString()).isEqualTo("UNKNOWN");
        var detail = success("/" + identity);
        assertThat(detail.get("snapshot").propertyNames()).containsExactlyInAnyOrder("schemaVersion", "catalogVersionId",
                "catalogOfferId", "channel", "versionToken", "productCode", "offerType", "audience", "amountCents",
                "currency", "billingCycle", "installments", "benefitLines", "snapshotSha256");
        assertThat(detail.get("paymentStatus").asString()).isEqualTo("UNKNOWN");
        assertThat(detail.get("issuanceStatus").asString()).isEqualTo("NONE");
        assertThat(detail.get("paidAt").isNull()).isTrue(); assertThat(detail.get("closedAt").isNull()).isTrue();
        var catalogOrderSelects = queries.stream()
                .filter(sql -> sql.startsWith("SELECT ") && sql.contains("catalog_order.")).toList();
        assertThat(catalogOrderSelects).isNotEmpty()
                .allSatisfy(sql -> assertThat(sql).doesNotContain("catalog_product", "catalog_version ", "catalog_offer ", "payment_order"));
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
        fixture.sql("UPDATE catalog_order.catalog_offer SET version_token='PA42_MUTATED' WHERE id=?", fixture.offer);
        before = fixture.databaseInventory();
        assertThat(success("/" + identity).get("snapshot")).isEqualTo(detail.get("snapshot"));
        assertThat(fixture.databaseInventory()).isEqualTo(before);
    }
    @Test void stablePostgresUuidOrderAndPagingBoundariesAreExact() throws Exception {
        List<String> identities = new ArrayList<>();
        for (int index = 0; index < 5; index++) identities.add(create("page-" + index));
        var before = fixture.databaseInventory();
        List<String> seen = new ArrayList<>();
        String suffix = "?limit=2";
        do {
            queries.clear();
            var page = success(suffix);
            page.get("items").forEach(item -> seen.add(item.get("orderId").asString()));
            assertThat(page.get("items").size()).isBetween(1, 2);
            assertThat(queries.stream().filter(sql -> sql.contains("FROM catalog_order.customer_order"))).hasSize(1);
            assertThat(queries.stream().filter(sql -> sql.contains("FROM entitlement_metering.entitlement_issuance"))).hasSize(1);
            if (!page.get("hasMore").asBoolean()) break;
            suffix = "?limit=2&cursor=" + page.get("nextCursor").asString();
        } while (seen.size() <= 5);
        assertThat(seen).containsExactlyInAnyOrderElementsOf(identities).doesNotHaveDuplicates();
        List<String> expected = java.util.Arrays.asList(fixture.value("""
                SELECT string_agg(id::text, ',' ORDER BY created_at DESC, id DESC) FROM catalog_order.customer_order
                WHERE account_id=?::uuid AND owner_organization_id IS NULL""", fixture.account).split(","));
        assertThat(expected).containsExactlyInAnyOrderElementsOf(identities);
        List<String> limitChanged = new ArrayList<>();
        queries.clear();
        var opened = success("?limit=2");
        assertThat(opened.get("items").size()).isEqualTo(2);
        assertThat(opened.get("hasMore").asBoolean()).isTrue();
        assertThat(opened.get("nextCursor").isNull()).isFalse();
        assertThat(queries.stream().filter(sql -> sql.contains("FROM catalog_order.customer_order"))).hasSize(1)
                .allSatisfy(sql -> assertThat(sql).doesNotContain("(o.created_at, o.id) <")
                        .contains("ORDER BY o.created_at DESC, o.id DESC LIMIT ?"));
        opened.get("items").forEach(item -> limitChanged.add(item.get("orderId").asString()));
        assertThat(limitChanged).containsExactlyElementsOf(expected.subList(0, 2));
        queries.clear();
        var closed = success("?limit=4&cursor=" + opened.get("nextCursor").asString());
        assertThat(closed.get("items").size()).isEqualTo(3);
        assertThat(closed.get("hasMore").asBoolean()).isFalse();
        assertThat(closed.get("nextCursor").isNull()).isTrue();
        assertThat(queries.stream().filter(sql -> sql.contains("FROM catalog_order.customer_order"))).hasSize(1)
                .allSatisfy(sql -> assertThat(sql)
                        .contains("(o.created_at, o.id) < (CAST(? AS timestamptz), CAST(? AS uuid))").doesNotContain("OFFSET"));
        assertThat(queries.stream().filter(sql -> sql.contains("FROM entitlement_metering.entitlement_issuance"))).hasSize(1);
        closed.get("items").forEach(item -> limitChanged.add(item.get("orderId").asString()));
        assertThat(limitChanged).doesNotHaveDuplicates().containsExactlyElementsOf(expected);
        List<String> anchorMutated = new ArrayList<>();
        var head = success("?limit=2");
        head.get("items").forEach(item -> anchorMutated.add(item.get("orderId").asString()));
        assertThat(anchorMutated).containsExactlyElementsOf(expected.subList(0, 2));
        String anchor = anchorMutated.get(1);
        String inserted = UUID.randomUUID().toString();
        fixture.sql("""
                INSERT INTO catalog_order.customer_order(id,order_no,account_id,owner_organization_id,subject_id,channel,
                    catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,
                    snapshot_sha256,version_token,idempotency_key,request_fingerprint,created_at)
                SELECT ?::uuid,'PA42-INSERT-'||?::text,account_id,owner_organization_id,subject_id,channel,
                    catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,
                    snapshot_sha256,version_token,'pa42-inserted-'||?::text,request_fingerprint,
                    '2025-01-01T00:00:00Z'::timestamptz FROM catalog_order.customer_order WHERE id=?::uuid
                """, inserted, inserted, inserted, anchor);
        fixture.sql("DELETE FROM catalog_order.customer_order WHERE id=?::uuid", anchor);
        assertThat(fixture.value("SELECT count(*)::int FROM catalog_order.customer_order WHERE id=?::uuid", anchor)).isEqualTo("0");
        queries.clear();
        var middle = success("?limit=2&cursor=" + head.get("nextCursor").asString());
        assertThat(middle.get("items").size()).isEqualTo(2);
        assertThat(middle.get("hasMore").asBoolean()).isTrue();
        assertThat(queries.stream().filter(sql -> sql.contains("FROM catalog_order.customer_order"))).hasSize(1)
                .allSatisfy(sql -> assertThat(sql)
                        .contains("(o.created_at, o.id) < (CAST(? AS timestamptz), CAST(? AS uuid))").doesNotContain("OFFSET"));
        middle.get("items").forEach(item -> anchorMutated.add(item.get("orderId").asString()));
        assertThat(middle.get("items").get(0).get("orderId").asString()).isEqualTo(expected.get(2));
        assertThat(middle.get("items").get(1).get("orderId").asString()).isEqualTo(expected.get(3));
        var tail = success("?limit=2&cursor=" + middle.get("nextCursor").asString());
        assertThat(tail.get("items").size()).isEqualTo(2);
        assertThat(tail.get("hasMore").asBoolean()).isFalse();
        assertThat(tail.get("nextCursor").isNull()).isTrue();
        tail.get("items").forEach(item -> anchorMutated.add(item.get("orderId").asString()));
        assertThat(tail.get("items").get(0).get("orderId").asString()).isEqualTo(expected.get(4));
        assertThat(tail.get("items").get(1).get("orderId").asString()).isEqualTo(inserted);
        assertThat(anchorMutated).doesNotHaveDuplicates().hasSize(6)
                .containsExactly(expected.get(0), anchor, expected.get(2), expected.get(3), expected.get(4), inserted);
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
    }
    @Test void missingAndForeignDetailsAreIndistinguishable() throws Exception {
        String identity = create("ownership");
        var before = fixture.databaseInventory();
        fixture.problem(get("/" + UUID.randomUUID(), "web"), 404, "NOT_FOUND");
        UUID other = UUID.randomUUID();
        fixture.sql("INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'other')", other);
        CustomerOrdersFixture.SESSIONS.put("other", new VerifiedCustomerSession(other, TrustedCustomerCaller.Channel.WEB,
                "onecall:customer-session:v1", new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
        fixture.problem(get("/" + identity, "other"), 404, "NOT_FOUND");
        assertThat(fixture.mapper.readTree(get("", "other").body()).get("items").size()).isZero();
        fixture.sql("DELETE FROM customer_iam.customer_account WHERE id=?", other);
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
    }
    @Test void activeLifecycleIsRecheckedForBothEndpoints() throws Exception {
        String identity = create("active");
        fixture.sql("UPDATE customer_iam.customer_account SET status='DISABLED' WHERE id=?", fixture.account);
        var before = fixture.databaseInventory();
        fixture.problem(get("", null), 401, "UNAUTHENTICATED");
        fixture.problem(get("", "web"), 403, "FORBIDDEN");
        fixture.problem(get("/" + identity, "web"), 403, "FORBIDDEN");
        CustomerOrdersFixture.SESSIONS.put("missing-account", new VerifiedCustomerSession(UUID.randomUUID(),
                TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1",
                new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)));
        fixture.problem(get("", "missing-account"), 403, "FORBIDDEN");
        fixture.problem(get("/" + identity, "missing-account"), 403, "FORBIDDEN");
        assertThat(fixture.databaseInventory()).isEqualTo(before);
        fixture.sql("UPDATE customer_iam.customer_account SET status='ACTIVE' WHERE id=?", fixture.account);
        fixture.session("missing-organization", "WEB", UUID.randomUUID());
        before = fixture.databaseInventory();
        fixture.problem(get("", "missing-organization"), 403, "FORBIDDEN");
        fixture.problem(get("/" + identity, "missing-organization"), 403, "FORBIDDEN");
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
    }
    @Test void cursorKeyAndLifecyclePrecedenceIsExact() throws Exception {
        var before = fixture.databaseInventory();
        fixture.problem(get("?limit=0&cursor=bad", "web"), 422, "VALIDATION_FAILED");
        fixture.problem(get("?cursor=bad", "web"), 400, "CURSOR_INVALID");
        fixture.problem(get("?limit=2&limit=3", "web"), 422, "VALIDATION_FAILED");
        fixture.problem(get("/not-a-uuid", "web"), 422, "VALIDATION_FAILED");
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
        for (String key : java.util.Arrays.asList(null, "invalid")) {
            try (var missingKey = new CustomerOrdersFixture()) {
                missingKey.start(java.util.Collections.singletonMap("onecall.customer.catalog-offers.cursor-hmac-key", key));
                missingKey.reset();
                String identity = missingKey.success(missingKey.post("missing-key"), 201).get("orderId").asString();
                var queriesStarted = new java.util.concurrent.atomic.AtomicInteger();
                missingKey.context.getBean("customerQueryDsl", DSLContext.class).configuration().set(new DefaultExecuteListener() {
                    @Override public void executeStart(ExecuteContext execution) { queriesStarted.incrementAndGet(); }
                });
                var properties = new com.onecall.core.customer_iam.internal.web.CustomerCatalogOfferCursorProperties();
                properties.setCursorHmacKey(java.util.Base64.getEncoder().encodeToString(new byte[32]));
                String cursor = new com.onecall.core.customer_iam.internal.web.CustomerOrderCursorCodec(properties).encode(
                        new com.onecall.core.catalog_order.customerquery.CustomerOrderReader.Scope(missingKey.account, null),
                        new com.onecall.core.catalog_order.customerquery.CustomerOrderReader.Anchor(java.time.Instant.parse("2026-01-01T00:00:00Z"), UUID.fromString(identity)));
                var inventory = missingKey.databaseInventory();
                missingKey.problem(get(missingKey, "?limit=0&cursor=bad", "web"), 422, "VALIDATION_FAILED");
                missingKey.problem(get(missingKey, "?cursor=bad", "web"), 400, "CURSOR_INVALID");
                missingKey.problem(get(missingKey, "?cursor=" + cursor, "web"), 503, "SERVICE_UNAVAILABLE");
                missingKey.problem(get(missingKey, "", "web"), 503, "SERVICE_UNAVAILABLE");
                assertThat(queriesStarted).hasValue(0);
                assertThat(get(missingKey, "/" + identity, "web").statusCode()).isEqualTo(200);
                assertThat(queriesStarted.get()).isPositive();
                assertThat(missingKey.databaseInventory()).isEqualTo(inventory);
                try (var connection = missingKey.context.getBean("customerQueryDataSource", HikariDataSource.class).getConnection()) {
                    assertThat(one(connection, "SELECT current_user||'|'||session_user")).isEqualTo("pa27_customer|pa27_customer");
                    assertThat(connection.getAutoCommit()).isTrue();
                    assertThat(connection.isReadOnly()).isFalse();
                    assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
                }
            }
        }
    }
    @Test void issuanceStatusesCoverNoneHomogeneousAndMixed() throws Exception {
        String identity = create("issuance");
        var empty = fixture.databaseInventory();
        assertThat(success("").get("items").get(0).get("issuanceStatus").asString()).isEqualTo("NONE");
        assertThat(success("/" + identity).get("issuanceStatus").asString()).isEqualTo("NONE");
        assertThat(fixture.databaseInventory()).isEqualTo(empty);
        fixture.sql("INSERT INTO entitlement_metering.entitlement_issuance(id,order_id,installment_no,entitlement_type,status,source_event_domain,source_event_id) VALUES (?,?::uuid,1,'AI_SECONDS','PENDING','PAYMENT','synthetic')", UUID.randomUUID(), identity);
        var before = fixture.databaseInventory();
        assertThat(success("/" + identity).get("issuanceStatus").asString()).isEqualTo("PENDING");
        assertThat(fixture.databaseInventory()).isEqualTo(before);
        fixture.sql("INSERT INTO entitlement_metering.entitlement_issuance(id,order_id,installment_no,entitlement_type,status,last_error_code,source_event_domain,source_event_id) VALUES (?,?::uuid,2,'AI_SECONDS','FAILED','SYNTHETIC','PAYMENT','synthetic-2')", UUID.randomUUID(), identity);
        before = fixture.databaseInventory();
        assertThat(success("").get("items").get(0).get("issuanceStatus").asString()).isEqualTo("MIXED");
        assertThat(success("/" + identity).get("issuanceStatus").asString()).isEqualTo("MIXED");
        assertThat(fixture.databaseInventory()).isEqualTo(before);
        fixture.sql("""
                INSERT INTO entitlement_metering.entitlement_grant(id,subject_id,source_type,source_ref_id,
                    entitlement_type,granted_units,valid_from,expires_at,order_id,installment_no,snapshot,snapshot_sha256)
                SELECT md5('pa42-grant-'||issuance.id)::uuid,orders.subject_id,'PURCHASE',orders.id,
                    issuance.entitlement_type,1,now(),now()+interval '1 day',orders.id,issuance.installment_no,
                    '{}'::jsonb,repeat('a',64)
                FROM entitlement_metering.entitlement_issuance issuance
                JOIN catalog_order.customer_order orders ON orders.id=issuance.order_id
                """);
        for (String status : List.of("PENDING", "ISSUED", "FAILED")) {
            fixture.sql("""
                    UPDATE entitlement_metering.entitlement_issuance SET status=?,
                        last_error_code=CASE WHEN ?='FAILED' THEN 'SYNTHETIC' ELSE NULL END,
                        grant_id=CASE WHEN ?='ISSUED' THEN md5('pa42-grant-'||id)::uuid ELSE NULL END,
                        issued_at=CASE WHEN ?='ISSUED' THEN now() ELSE NULL END
                    """, status, status, status, status);
            before = fixture.databaseInventory();
            assertThat(success("").get("items").get(0).get("issuanceStatus").asString()).isEqualTo(status);
            assertThat(success("/" + identity).get("issuanceStatus").asString()).isEqualTo(status);
            assertThat(fixture.databaseInventory()).isEqualTo(before);
        }
        clean();
    }
    @Test void unknownAndAllPaymentStatesRemainVisible() throws Exception {
        String identity = create("payment");
        for (String state : List.of("UNPAID", "UNKNOWN", "PAID", "CLOSED")) {
            fixture.sql("UPDATE catalog_order.customer_order SET payment_status=?,paid_at=CASE WHEN ?='PAID' THEN now() ELSE NULL END,closed_at=CASE WHEN ?='CLOSED' THEN now() ELSE NULL END WHERE id=?::uuid",
                    state, state, state, identity);
            var before = fixture.databaseInventory();
            var detail = success("/" + identity);
            assertThat(detail.propertyNames()).containsExactlyInAnyOrder("orderId", "orderNo", "subjectId", "channel",
                    "catalogVersionId", "catalogOfferId", "amountCents", "currency", "paymentStatus", "issuanceStatus",
                    "createdAt", "snapshotSha256", "snapshot", "purchaseConsentId", "paidAt", "closedAt");
            assertThat(detail.get("paymentStatus").asString()).isEqualTo(state);
            assertThat(detail.get("paidAt").isNull()).isEqualTo(!state.equals("PAID"));
            assertThat(detail.get("closedAt").isNull()).isEqualTo(!state.equals("CLOSED"));
            assertThat(success("").get("items").get(0).get("paymentStatus").asString()).isEqualTo(state);
            assertThat(fixture.databaseInventory()).isEqualTo(before);
        }
        clean();
    }
    @Test void personalAndOrganizationPagesAreExactlyScoped() throws Exception {
        String original = create("organization");
        String identity = UUID.randomUUID().toString();
        UUID organization = UUID.randomUUID();
        fixture.sql("INSERT INTO organization_access.organization(id,display_name) VALUES (?,'synthetic org')", organization);
        fixture.sql("INSERT INTO organization_access.organization_membership(id,account_id,organization_id,role) VALUES (?,?,?,'MEMBER')",
                UUID.randomUUID(), fixture.account, organization);
        fixture.sql("""
                INSERT INTO catalog_order.customer_order(id,order_no,account_id,owner_organization_id,subject_id,channel,
                    catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,
                    snapshot_sha256,version_token,idempotency_key,request_fingerprint,created_at)
                SELECT ?::uuid,'PA42-ORG',account_id,?,subject_id,channel,catalog_version_id,catalog_offer_id,
                    purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,version_token,
                    'pa42-org',request_fingerprint,created_at FROM catalog_order.customer_order WHERE id=?::uuid
                """, identity, organization, original);
        fixture.session("organization", "WEB", organization);
        for (String role : List.of("ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR")) {
            fixture.sql("UPDATE organization_access.organization_membership SET role=? WHERE account_id=?", role, fixture.account);
            var before = fixture.databaseInventory();
            var response = get("/" + identity, "organization");
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            assertThat(fixture.mapper.readTree(get("", "organization").body()).get("items").size()).isEqualTo(1);
            fixture.problem(get("/" + identity, "web"), 404, "NOT_FOUND");
            assertThat(fixture.databaseInventory()).isEqualTo(before);
        }
        UUID other = UUID.randomUUID();
        fixture.sql("INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'other org member')", other);
        fixture.sql("INSERT INTO organization_access.organization_membership(id,account_id,organization_id,role) VALUES (?,?,?,'MEMBER')",
                UUID.randomUUID(), other, organization);
        CustomerOrdersFixture.SESSIONS.put("same-org-other-account", new VerifiedCustomerSession(other, TrustedCustomerCaller.Channel.WEB,
                "onecall:customer-session:v1", new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.ORGANIZATION, organization)));
        var before = fixture.databaseInventory();
        fixture.problem(get("/" + identity, "same-org-other-account"), 404, "NOT_FOUND");
        assertThat(fixture.mapper.readTree(get("", "same-org-other-account").body()).get("items").size()).isZero();
        assertThat(fixture.databaseInventory()).isEqualTo(before);
        UUID otherOrganization = UUID.randomUUID();
        fixture.sql("INSERT INTO organization_access.organization(id,display_name) VALUES (?,'other workspace')", otherOrganization);
        fixture.sql("INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role) VALUES (?,?,?,'MEMBER')",
                UUID.randomUUID(), otherOrganization, fixture.account);
        fixture.session("other-organization", "WEB", otherOrganization);
        before = fixture.databaseInventory();
        fixture.problem(get("/" + identity, "other-organization"), 404, "NOT_FOUND");
        assertThat(fixture.mapper.readTree(get("", "other-organization").body()).get("items").size()).isZero();
        assertThat(fixture.databaseInventory()).isEqualTo(before);
        fixture.sql("UPDATE organization_access.organization_membership SET status='SUSPENDED' WHERE account_id=?", fixture.account);
        before = fixture.databaseInventory();
        fixture.problem(get("", "organization"), 403, "FORBIDDEN");
        fixture.problem(get("/" + identity, "organization"), 403, "FORBIDDEN");
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
    }
    @Test void corruptSummaryIs500AndCorruptDetailIs404WithNoWrites() throws Exception {
        String identity = replaceFixtureOrder(create("corrupt"), UUID.randomUUID().toString(),
                "amount_cents", "repeat('b',64)", "created_at");
        var before = fixture.databaseInventory();
        fixture.problem(get("/" + identity, "web"), 404, "NOT_FOUND");
        assertThat(success("").get("items").size()).isEqualTo(1);
        assertThat(fixture.databaseInventory()).isEqualTo(before);
        identity = replaceFixtureOrder(identity, UUID.randomUUID().toString(),
                "9007199254740992", "snapshot_sha256", "created_at");
        before = fixture.databaseInventory();
        fixture.problem(get("", "web"), 500, "INTERNAL");
        fixture.problem(get("/" + identity, "web"), 404, "NOT_FOUND");
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
        String agreement = create("agreement");
        fixture.sql("DELETE FROM catalog_order.customer_order WHERE id=?::uuid", identity);
        var consistent = success("/" + agreement);
        assertThat(consistent.get("amountCents").asLong()).isEqualTo(123L);
        assertThat(consistent.get("snapshot").get("amountCents").asLong()).isEqualTo(123L);
        String storedFacts = fixture.value("""
                SELECT md5(snapshot::text)||'|'||snapshot_sha256 FROM catalog_order.customer_order WHERE id=?::uuid""", agreement);
        before = fixture.databaseInventory();
        identity = replaceFixtureOrder(agreement, UUID.randomUUID().toString(),
                "(snapshot->>'priceCents')::bigint+1", "snapshot_sha256", "created_at");
        assertThat(fixture.value("SELECT md5(snapshot::text)||'|'||snapshot_sha256 FROM catalog_order.customer_order WHERE id=?::uuid", identity))
                .isEqualTo(storedFacts);
        assertThat(fixture.value("SELECT amount_cents::text||'|'||(snapshot->>'priceCents') FROM catalog_order.customer_order WHERE id=?::uuid", identity))
                .isEqualTo("124|123");
        assertThat(fixture.value("SELECT count(*)::int FROM catalog_order.customer_order WHERE account_id=?::uuid AND owner_organization_id IS NULL", fixture.account)).isEqualTo("1");
        fixture.problem(get("/" + identity, "web"), 404, "NOT_FOUND");
        var page = success("");
        assertThat(page.get("items").size()).isEqualTo(1);
        assertThat(page.get("items").get(0).get("orderId").asString()).isEqualTo(identity);
        assertThat(page.get("items").get(0).get("amountCents").asLong()).isEqualTo(124L);
        assertThat(fixture.databaseInventory()).isEqualTo(before);
        clean();
    }
    @Test void readsAndInTransactionFailuresHaveZeroWriteDelta() throws Exception {
        create("dependency");
        var before = fixture.databaseInventory();
        beforeRead = execution -> {
            if (execution.sql().contains("FROM catalog_order.customer_order"))
                throw new org.jooq.exception.DataAccessException("PRIVATE synthetic");
        };
        fixture.problem(get("", "web"), 503, "SERVICE_UNAVAILABLE");
        fixture.problem(get("/" + fixture.value("SELECT id FROM catalog_order.customer_order"), "web"), 503, "SERVICE_UNAVAILABLE");
        beforeRead = null;
        assertThat(success("").get("items").size()).isEqualTo(1);
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
    }
    @Test void summaryAndIssuanceShareSnapshotAcrossCommittedConcurrentChange() throws Exception {
        String identity = create("snapshot");
        var changed = new java.util.concurrent.atomic.AtomicBoolean();
        afterRead = execution -> {
            if (execution.sql().contains("FROM catalog_order.customer_order") && changed.compareAndSet(false, true)) {
                try {
                    fixture.sql("INSERT INTO entitlement_metering.entitlement_issuance(id,order_id,installment_no,entitlement_type,status,source_event_domain,source_event_id) VALUES (?,?::uuid,1,'AI_SECONDS','PENDING','PAYMENT','concurrent')", UUID.randomUUID(), identity);
                } catch (Exception failure) { throw new AssertionError(failure); }
            }
        };
        assertThat(success("").get("items").get(0).get("issuanceStatus").asString()).isEqualTo("NONE");
        assertThat(changed).isTrue(); afterRead = null;
        assertThat(success("/" + identity).get("issuanceStatus").asString()).isEqualTo("PENDING");
        clean();
    }
    @Test void tiedTimestampsUseUnsignedPostgresUuidOrderAcrossPages() throws Exception {
        List<String> identities = List.of("00000000-0000-0000-0000-000000000001", "7fffffff-ffff-ffff-ffff-ffffffffffff",
                "80000000-0000-0000-0000-000000000000");
        for (int index = 0; index < identities.size(); index++) {
            String original = create("tied-" + index);
            replaceFixtureOrder(original, identities.get(index), "amount_cents", "snapshot_sha256",
                    "'2026-01-01T00:00:00.123456Z'::timestamptz");
        }
        var before = fixture.databaseInventory();
        String suffix = "?limit=1";
        for (String expected : identities.reversed()) {
            var page = success(suffix);
            assertThat(page.get("items").get(0).get("orderId").asString()).isEqualTo(expected);
            assertThat(page.get("items").get(0).get("createdAt").asString()).isEqualTo("2026-01-01T00:00:00.123456Z");
            suffix = "?limit=1&cursor=" + page.get("nextCursor").asString();
        }
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
    }
    @Test void crossAccountAndCrossWorkspaceRowsNeverAppear() throws Exception {
        String identity = create("workspace-isolation");
        UUID organization = UUID.randomUUID();
        fixture.sql("INSERT INTO organization_access.organization(id,display_name) VALUES (?,'isolated workspace')", organization);
        fixture.sql("INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role) VALUES (?,?,?,'MEMBER')",
                UUID.randomUUID(), organization, fixture.account);
        fixture.session("organization", "WEB", organization);
        var before = fixture.databaseInventory();
        fixture.problem(get("/" + identity, "organization"), 404, "NOT_FOUND");
        assertThat(fixture.mapper.readTree(get("", "organization").body()).get("items").size()).isZero();
        assertThat(fixture.databaseInventory()).isEqualTo(before);
        for (String mutation : List.of(
                "UPDATE organization_access.organization SET status='DISABLED'",
                "UPDATE organization_access.organization SET status='SUSPENDED'",
                "UPDATE organization_access.organization_membership SET status='SUSPENDED'",
                "UPDATE organization_access.organization_membership SET status='REVOKED'",
                "DELETE FROM organization_access.organization_membership")) {
            fixture.sql("UPDATE organization_access.organization SET status='ACTIVE'");
            fixture.sql("UPDATE organization_access.organization_membership SET status='ACTIVE'");
            fixture.sql(mutation);
            before = fixture.databaseInventory();
            fixture.problem(get("", "organization"), 403, "FORBIDDEN");
            fixture.problem(get("/" + identity, "organization"), 403, "FORBIDDEN");
            assertThat(fixture.databaseInventory()).isEqualTo(before);
        }
        clean();
    }
    @Test void postgresConstraintsRejectInvalidIssuanceStatuses() throws Exception {
        String identity = create("issuance-constraints");
        var before = fixture.databaseInventory();
        try (var connection = fixture.base.admin()) {
            connection.setAutoCommit(false);
            for (String status : java.util.Arrays.asList(null, "UNKNOWN", "MIXED", "NONE")) {
                var savepoint = connection.setSavepoint();
                assertThatThrownBy(() -> execute(connection, """
                        INSERT INTO entitlement_metering.entitlement_issuance(id,order_id,installment_no,
                            entitlement_type,status,source_event_domain,source_event_id)
                        VALUES (?,?::uuid,1,'AI_SECONDS',?,'PAYMENT','constraint')
                        """, UUID.randomUUID(), identity, status)).isInstanceOfSatisfying(java.sql.SQLException.class,
                                failure -> assertThat(failure.getSQLState()).isIn("23502", "23514"));
                connection.rollback(savepoint);
            }
            execute(connection, """
                    INSERT INTO entitlement_metering.entitlement_issuance(id,order_id,installment_no,
                        entitlement_type,status,source_event_domain,source_event_id)
                    VALUES (?,?::uuid,1,'AI_SECONDS','PENDING','PAYMENT','constraint')
                    """, UUID.randomUUID(), identity);
            for (String status : java.util.Arrays.asList(null, "UNKNOWN")) {
                var savepoint = connection.setSavepoint();
                assertThatThrownBy(() -> execute(connection,
                        "UPDATE entitlement_metering.entitlement_issuance SET status=?", status))
                        .isInstanceOfSatisfying(java.sql.SQLException.class,
                                failure -> assertThat(failure.getSQLState()).isIn("23502", "23514"));
                connection.rollback(savepoint);
            }
            connection.rollback();
        }
        assertThat(fixture.databaseInventory()).isEqualTo(before); clean();
    }
    @Test void representativePersonalAndOrganizationExplainEvidenceIsCaptured() throws Exception {
        String root = create("explain");
        fixture.sql("INSERT INTO customer_iam.customer_account(id,display_name) SELECT md5('pa42-account-'||series)::uuid,'plan account' FROM generate_series(1,9) series");
        fixture.sql("INSERT INTO organization_access.organization(id,display_name) SELECT md5('pa42-org-'||series)::uuid,'plan organization' FROM generate_series(1,2) series");
        fixture.sql("""
                INSERT INTO catalog_order.customer_order(id,order_no,account_id,owner_organization_id,subject_id,channel,
                    catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,
                    snapshot_sha256,version_token,idempotency_key,request_fingerprint,created_at)
                SELECT md5('pa42-order-'||series)::uuid,'PLAN-'||series,
                    CASE WHEN series<=8000 OR series%10=0 THEN original.account_id ELSE md5('pa42-account-'||(series%10))::uuid END,
                    CASE WHEN series%3=0 THEN NULL ELSE md5('pa42-org-'||(series%3))::uuid END,
                    subject_id,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,
                    payment_status,snapshot,snapshot_sha256,version_token,'plan-'||series,request_fingerprint,
                    '2026-01-01T00:00:00Z'::timestamptz+(series%1000)*interval '1 second'
                FROM catalog_order.customer_order original CROSS JOIN generate_series(1,10000) series WHERE original.id=?::uuid
                """, root);
        fixture.sql("""
                INSERT INTO entitlement_metering.entitlement_issuance(id,order_id,installment_no,entitlement_type,status,source_event_domain,source_event_id)
                SELECT md5('pa42-issuance-'||series||'-'||installment)::uuid,md5('pa42-order-'||series)::uuid,
                    installment,'AI_SECONDS','PENDING','PAYMENT','plan-'||series
                FROM generate_series(1,10000) series CROSS JOIN LATERAL generate_series(1,series%3) installment
                """);
        fixture.sql("ANALYZE catalog_order.customer_order; ANALYZE entitlement_metering.entitlement_issuance");
        assertThat(fixture.value("SELECT count(*)||'|'||count(DISTINCT account_id)||'|'||count(DISTINCT owner_organization_id) FROM catalog_order.customer_order")).isEqualTo("10001|10|2");
        System.out.println("PA42 EXPLAIN version=" + fixture.value("SELECT version()") + " orders=10001 accounts=10 organizations=2 dense-account=8201 repeated-timestamps=1000 issuance=" + fixture.value("SELECT count(*) FROM entitlement_metering.entitlement_issuance"));
        var statements = new ArrayList<String>();
        var bindings = new ArrayList<Object[]>();
        var owner = new com.onecall.core.catalog_order.internal.customerquery.JooqCustomerOrderReader(org.jooq.impl.DSL.using(
                new org.jooq.tools.jdbc.MockConnection(execution -> {
                    statements.add(execution.sql()); bindings.add(execution.bindings());
                    return new org.jooq.tools.jdbc.MockResult[]{new org.jooq.tools.jdbc.MockResult(0,
                            org.jooq.impl.DSL.using(org.jooq.SQLDialect.POSTGRES).newResult(org.jooq.impl.DSL.field("id", UUID.class)))};
                }), org.jooq.SQLDialect.POSTGRES));
        for (UUID organization : java.util.Arrays.asList(null, UUID.fromString(fixture.value("SELECT md5('pa42-org-1')::uuid")))) {
            var scope = new com.onecall.core.catalog_order.customerquery.CustomerOrderReader.Scope(fixture.account, organization);
            for (int limit : List.of(20, 100)) {
                owner.fetchOrders(scope, java.util.Optional.empty(), limit + 1);
                owner.fetchOrders(scope, java.util.Optional.of(new com.onecall.core.catalog_order.customerquery.CustomerOrderReader.Anchor(
                        java.time.Instant.parse("2026-01-01T00:05:00Z"), UUID.fromString("80000000-0000-0000-0000-000000000000"))), limit + 1);
            }
            owner.fetchOrder(scope, organization == null ? UUID.fromString(root)
                    : UUID.fromString(fixture.value("SELECT md5('pa42-order-1')::uuid")));
        }
        owner.fetchIssuances(java.util.stream.IntStream.rangeClosed(1, 100).mapToObj(index -> UUID.fromString(
                fixtureValue("SELECT md5('pa42-order-" + index + "')::uuid"))).toList());
        try (var connection = fixture.base.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            for (int index = 0; index < statements.size(); index++) {
                String sql = statements.get(index);
                assertThat(sql).doesNotContain("OFFSET", "COUNT(", "SELECT *");
                try (var statement = connection.prepareStatement("EXPLAIN (ANALYZE, BUFFERS) " + sql)) {
                    Object[] values = bindings.get(index);
                    for (int parameter = 0; parameter < values.length; parameter++) statement.setObject(parameter + 1, values[parameter]);
                    try (var rows = statement.executeQuery()) {
                        List<String> plan = new ArrayList<>(); while (rows.next()) plan.add(rows.getString(1));
                        assertThat(plan).anyMatch(line -> line.contains("Execution Time"));
                        System.out.println("PA42 EXPLAIN statement=" + index + " binds=" + values.length
                                + " sql=" + sql + " values=" + java.util.Arrays.toString(values)
                                + "\n" + String.join("\n", plan));
                    }
                }
            }
            connection.rollback();
        }
        clean();
    }
    String fixtureValue(String sql) {
        try { return fixture.value(sql); } catch (Exception failure) { throw new AssertionError(failure); }
    }
}
