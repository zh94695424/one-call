package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.one;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class OperationsCatalogDraftFixture implements AutoCloseable {
    static final String BASE = "/api/v1/operations/catalog/versions";
    static final String COMMERCIAL = "\"offerType\":\"ONE_TIME\",\"audience\":\"PERSONAL\",\"priceCents\":0,\"currency\":\"CNY\",\"billingCycle\":\"ONCE\",\"installments\":1,\"grantIntervalMonths\":0";
    static final String BENEFITS = "[{\"entitlementType\":\"AI_SECONDS\",\"quantity\":9007199254740993,\"validityDays\":31,\"grantIntervalMonths\":0},{\"entitlementType\":\"STORAGE_BYTES\",\"quantity\":9223372036854775807,\"validityDays\":1,\"grantIntervalMonths\":2}]";
    final OperationsCatalogVersionLifecycleFixture base = new OperationsCatalogVersionLifecycleFixture();
    final JsonMapper mapper = JsonMapper.builder().build();
    final List<OperationsCatalogVersionLifecycleFixture.Entry> entries = new CopyOnWriteArrayList<>();
    final List<String> states = new CopyOnWriteArrayList<>(), started = new CopyOnWriteArrayList<>();
    final AtomicBoolean armed = new AtomicBoolean();
    volatile String fault;
    volatile java.util.function.Consumer<ExecuteContext> after;
    final AtomicInteger holderPid = new AtomicInteger(), contenderPid = new AtomicInteger();
    void start() throws Exception { base.start(); }
    void reset() throws Exception {
        base.reset(); entries.clear(); states.clear(); started.clear(); armed.set(false); fault = null; after = null; holderPid.set(0); contenderPid.set(0);
        base.shared.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void prepareStart(ExecuteContext context) {
                if (armed.get() && "ZERO_UPDATE".equals(fault) && context.sql().startsWith("UPDATE catalog_order.catalog_version")) {
                    armed.set(false); context.sql(context.sql() + " AND false");
                }
            }
            @Override public void executeStart(ExecuteContext context) {
                String sql = context.sql(); started.add(sql);
                try {
                    if (sql.startsWith("SELECT pg_advisory_xact_lock")) contenderPid.set(Integer.parseInt(one(context.connection(), "SELECT pg_backend_pid()")));
                    if (armed.get() && "RESTORE".equals(fault) && sql.equals("SET LOCAL ROLE NONE")) { armed.set(false); throw new IllegalStateException("PRIVATE restore"); }
                    if (armed.get() && "TERMINATE".equals(fault) && sql.equals("SET LOCAL ROLE NONE")) { armed.set(false); try (var admin = admin()) { execute(admin, "SELECT pg_terminate_backend(?)", Integer.parseInt(one(context.connection(), "SELECT pg_backend_pid()"))); } }
                    if (armed.get() && ("RAW_23505".equals(fault) || "RAW_40001".equals(fault)) && (sql.contains("AND version_no = ? FOR UPDATE") || sql.equals("SELECT id, product_id FROM catalog_order.catalog_version WHERE id = ?"))) {
                        armed.set(false);
                        try (var statement = context.connection().createStatement()) {
                            statement.execute("RAW_23505".equals(fault) ? "INSERT INTO catalog_order.catalog_product SELECT * FROM catalog_order.catalog_product LIMIT 1" : "DO $$ BEGIN RAISE EXCEPTION 'PRIVATE' USING ERRCODE='40001'; END $$");
                        }
                    }
                } catch (SQLException failure) { states.add(failure.getSQLState()); throw new org.jooq.exception.DataAccessException("PRIVATE", failure); }
                catch (Exception failure) { if (failure instanceof RuntimeException runtime) throw runtime; throw new IllegalStateException(failure); }
            }
            @Override public void executeEnd(ExecuteContext context) {
                String sql = context.sql();
                try {
                    int pid = Integer.parseInt(one(context.connection(), "SELECT pg_backend_pid()"));
                    entries.add(new OperationsCatalogVersionLifecycleFixture.Entry(sql, context.query() == null ? List.of() : new ArrayList<>(context.query().getBindValues()), one(context.connection(), "SELECT current_user"), pid, Long.parseLong(one(context.connection(), "SELECT txid_current()"))));
                    var hook = after; if (hook != null) hook.accept(context);
                    String stage = sql.startsWith("INSERT INTO catalog_order.catalog_version") ? "VERSION" : sql.startsWith("INSERT INTO catalog_order.catalog_benefit_line") ? "BENEFIT" : sql.startsWith("INSERT INTO catalog_order.catalog_offer") ? "OFFER" : sql.startsWith("UPDATE catalog_order.catalog_version") ? "UPDATE" : sql.startsWith("UPDATE catalog_order.catalog_offer") ? "TOKEN" : sql.contains("audit.append_fact") ? "AUDIT" : "";
                    if (armed.get() && Objects.equals(fault, "ZERO_UPDATE") && stage.equals("UPDATE")) { armed.set(false); context.rows(0); }
                    else if (armed.get() && Objects.equals(fault, stage)) { armed.set(false); throw new IllegalStateException("PRIVATE after " + stage); }
                    else if (armed.get() && "STATEMENT_TIMEOUT".equals(fault) && stage.equals("AUDIT")) { armed.set(false); try (var statement = context.connection().createStatement()) { statement.execute("SELECT pg_sleep(3)"); } }
                } catch (SQLException failure) { states.add(failure.getSQLState()); throw new org.jooq.exception.DataAccessException("PRIVATE", failure); }
            }
            @Override public void exception(ExecuteContext context) { if (context.sqlException() != null) states.add(context.sqlException().getSQLState()); }
        });
    }
    UUID callerTrace() {
        var binds = entries.stream().filter(entry -> entry.sql().contains("audit.append_fact")).toList().getLast().binds();
        assertThat(binds).hasSize(8);
        assertThat(binds.get(6).toString()).startsWith("catalog_version:").contains(":draft:");
        return UUID.fromString(binds.get(7).toString());
    }
    Connection admin() throws Exception { return base.admin(); }
    UUID product() throws Exception { return base.product(); }
    void mutate(String sql, Object... values) throws Exception { base.mutate(sql, values); }
    String value(String sql, Object... values) throws Exception { return base.value(sql, values); }
    String createBody(UUID product, int version, boolean children) { return "{\"productId\":\"" + product + "\",\"versionNo\":" + version + "," + COMMERCIAL + ",\"benefitLines\":" + (children ? BENEFITS : "[]") + ",\"channels\":" + (children ? "[\"WEB\",\"MINIAPP\"]" : "[]") + "}"; }
    String replaceBody(int revision, long price) { return "{\"expectedRevision\":" + revision + "," + COMMERCIAL.replace("\"priceCents\":0", "\"priceCents\":" + price) + "}"; }
    HttpResponse<String> request(String method, String path, String body, String token) throws Exception {
        return base.shared.client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + base.shared.port + path)).timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + token).header("Content-Type", "application/json").header("Accept", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> create(UUID product, int version, boolean children) throws Exception { return request("POST", BASE, createBody(product, version, children), "admin"); }
    HttpResponse<String> replace(UUID version, int revision, long price) throws Exception { return request("PUT", BASE + "/" + version + "/draft", replaceBody(revision, price), "admin"); }
    UUID draft(boolean children) throws Exception { return UUID.fromString(success(create(product(), 1, children), 201).get("catalogVersionId").asString()); }
    JsonNode success(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as("%s%nstarted=%s states=%s", response.body(), started, states).isEqualTo(status); headers(response); return mapper.readTree(response.body());
    }
    void headers(HttpResponse<String> response) { assertThat(response.headers().firstValue("Cache-Control")).contains("no-store"); assertThat(response.headers().firstValue("Pragma")).contains("no-cache"); assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff"); assertThat(response.headers().firstValue("Content-Length")).contains(Integer.toString(response.body().getBytes(StandardCharsets.UTF_8).length)); }
    void problem(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as("%s%n%s%n%s", response.body(), started, states).isEqualTo(status); headers(response); JsonNode body = mapper.readTree(response.body()); assertThat(body.size()).isEqualTo(5); assertThat(body.get("code").asString()).isEqualTo(code);
        String title = switch (code) { case "CATALOG_DRAFT_CONFLICT" -> "Catalog draft conflict"; case "CATALOG_DRAFT_STALE_VIEW" -> "Catalog draft stale view"; case "FORBIDDEN" -> "Access denied"; case "NOT_FOUND" -> "Not found"; case "INTERNAL" -> "Internal error"; case "VALIDATION_FAILED" -> "Request validation failed"; default -> "Service unavailable"; };
        UUID.fromString(body.get("traceId").asString()); assertThat(response.body()).isEqualTo("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status + ",\"code\":\"" + code + "\",\"traceId\":\"" + body.get("traceId").asString() + "\"}");
    }
    String allFacts() throws Exception {
        StringBuilder result = new StringBuilder();
        for (String table : List.of("catalog_order.catalog_product", "catalog_order.catalog_version", "catalog_order.catalog_benefit_line", "catalog_order.catalog_offer", "audit.audit_fact", "platform.outbox_event", "platform.idempotency_record")) result.append(value("SELECT coalesce(jsonb_agg(to_jsonb(row) ORDER BY id)::text,'[]') FROM " + table + " row"));
        return result.toString();
    }
    String immutable(UUID version) throws Exception {
        return value("SELECT (to_jsonb(row) - ARRAY['offer_type','audience','price_cents','currency','billing_cycle','installments','grant_interval_months','snapshot','snapshot_sha256','revision'])::text FROM catalog_order.catalog_version row WHERE id=?", version)
                + value("SELECT coalesce(jsonb_agg(to_jsonb(row) ORDER BY id)::text,'[]') FROM catalog_order.catalog_benefit_line row WHERE catalog_version_id=?", version)
                + value("SELECT coalesce(jsonb_agg(to_jsonb(row) - ARRAY['version_token','revision'] ORDER BY id)::text,'[]') FROM catalog_order.catalog_offer row WHERE catalog_version_id=?", version);
    }
    void noPlatformWrites() throws Exception { assertThat(value("SELECT count(*) FROM platform.outbox_event")).isEqualTo("0"); assertThat(value("SELECT count(*) FROM platform.idempotency_record")).isEqualTo("0"); }
    List<OperationsCatalogVersionLifecycleFixture.Entry> calls(String token) { return entries.stream().filter(entry -> entry.sql().contains(token)).toList(); }
    void fail(String stage) { fault = stage; armed.set(true); }
    static String hash(String text) { try { return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); } catch (Exception failure) { throw new AssertionError(failure); } }
    String canonical(JsonNode response) {
        var projection = new TreeMap<String, Object>();
        for (String field : List.of("productId", "versionNo", "offerType", "audience", "priceCents", "currency", "billingCycle", "installments", "grantIntervalMonths")) {
            JsonNode node = response.get(field); projection.put(field, node.isNull() ? null : node.isIntegralNumber() ? node.bigIntegerValue() : node.asString());
        }
        List<Map<String, Object>> benefits = new ArrayList<>();
        for (JsonNode line : response.get("benefitLines")) { var item = new TreeMap<String, Object>(); for (var entry : line.properties()) item.put(entry.getKey(), entry.getValue().isIntegralNumber() ? entry.getValue().bigIntegerValue() : entry.getValue().asString()); benefits.add(item); }
        benefits.sort(Comparator.comparing(line -> line.get("entitlementType").toString())); projection.put("benefitLines", benefits);
        List<String> channels = new ArrayList<>(); response.get("channels").forEach(node -> channels.add(node.asString())); channels.sort(String::compareTo); projection.put("channels", channels);
        return mapper.writeValueAsString(projection);
    }
    void exactStored(HttpResponse<String> response) throws Exception {
        JsonNode body = mapper.readTree(response.body()); UUID version = UUID.fromString(body.get("catalogVersionId").asString());
        assertThat(body.size()).isEqualTo(17); String projection = canonical(body); assertThat(body.get("snapshotSha256").asString()).isEqualTo(hash(projection));
        assertThat(value("SELECT snapshot_sha256 FROM catalog_order.catalog_version WHERE id=?", version)).isEqualTo(hash(projection));
        assertThat(value("SELECT snapshot=?::jsonb FROM catalog_order.catalog_version WHERE id=?", projection, version)).isEqualTo("t");
        Instant created; try (var connection = admin(); var query = connection.prepareStatement("SELECT created_at FROM catalog_order.catalog_version WHERE id=?")) { query.setObject(1, version); try (var rows = query.executeQuery()) { assertThat(rows.next()).isTrue(); created = rows.getObject(1, java.time.OffsetDateTime.class).toInstant(); } }
        assertThat(created.getNano() % 1000).isZero(); assertThat(body.get("createdAt").asString()).isEqualTo(created.toString());
        String expectedBenefits = body.get("benefitLines").isEmpty() ? "[]" : BENEFITS;
        String expectedChannels = body.get("channels").isEmpty() ? "[]" : "[\"MINIAPP\",\"WEB\"]";
        String expected = "{\"catalogVersionId\":\"" + version + "\",\"productId\":\"" + body.get("productId").asString() + "\",\"versionNo\":" + body.get("versionNo").asInt() + ",\"saleStatus\":\"DRAFT\",\"offerType\":\"" + body.get("offerType").asString() + "\",\"audience\":\"" + body.get("audience").asString() + "\",\"priceCents\":" + body.get("priceCents") + ",\"currency\":\"CNY\",\"billingCycle\":\"" + body.get("billingCycle").asString() + "\",\"installments\":" + body.get("installments").asInt() + ",\"grantIntervalMonths\":" + body.get("grantIntervalMonths").asInt() + ",\"benefitLines\":" + expectedBenefits + ",\"channels\":" + expectedChannels + ",\"revision\":" + body.get("revision").asInt() + ",\"createdAt\":\"" + created + "\",\"publishedAt\":null,\"snapshotSha256\":\"" + hash(projection) + "\"}";
        assertThat(response.body().getBytes(StandardCharsets.UTF_8)).isEqualTo(expected.getBytes(StandardCharsets.UTF_8)); headers(response);
        assertThat(response.body()).doesNotContain("versionToken", "createdBy", "password", "offerId");
    }
    record Race(HttpResponse<String> winner, HttpResponse<String> contender, boolean expired) { }
    Race race(Callable<HttpResponse<String>> winner, Callable<HttpResponse<String>> contender, boolean expire, boolean rollback) throws Exception {
        states.clear(); holderPid.set(0); contenderPid.set(0);
        String before = allFacts(); var ready = new CountDownLatch(1); var release = new CountDownLatch(1); var observed = new CountDownLatch(1); var observerStarted = new CountDownLatch(1); var taken = new AtomicBoolean();
        after = context -> {
            if (context.sql().contains("audit.append_fact") && taken.compareAndSet(false, true)) {
                try { holderPid.set(Integer.parseInt(one(context.connection(), "SELECT pg_backend_pid()"))); ready.countDown(); if (!release.await(10, TimeUnit.SECONDS)) throw new AssertionError("holder release timeout"); if (rollback) throw new IllegalStateException("PRIVATE holder rollback"); }
                catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
                catch (SQLException failure) { throw new AssertionError(failure); }
            }
        };
        try (var executor = Executors.newFixedThreadPool(3)) {
            Future<HttpResponse<String>> winning = executor.submit(winner); assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> observer = executor.submit(() -> {
                try (var connection = admin()) {
                    observerStarted.countDown();
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
                    while (System.nanoTime() < deadline) {
                        int contenderBackend = contenderPid.get();
                        if (contenderBackend != holderPid.get() && contenderBackend > 0 && "t".equals(one(connection, "SELECT EXISTS(SELECT 1 FROM pg_stat_activity WHERE pid=? AND wait_event='advisory' AND ?=ANY(pg_blocking_pids(pid)))", contenderBackend, holderPid.get()))) { observed.countDown(); return; }
                        java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                    }
                    throw new AssertionError("no observed PostgreSQL advisory blocker");
                } catch (Exception failure) { throw new AssertionError(failure); }
            });
            assertThat(observerStarted.await(5, TimeUnit.SECONDS)).isTrue();
            Future<HttpResponse<String>> losing = executor.submit(contender);
            try {
                assertThat(observed.await(8, TimeUnit.SECONDS)).isTrue(); observer.get(2, TimeUnit.SECONDS);
                HttpResponse<String> loser;
                if (expire) { loser = losing.get(10, TimeUnit.SECONDS); problem(loser, 409, "CATALOG_DRAFT_CONFLICT"); assertThat(states).contains("55P03"); assertThat(allFacts()).isEqualTo(before); release.countDown(); }
                else { release.countDown(); loser = losing.get(10, TimeUnit.SECONDS); }
                HttpResponse<String> won = winning.get(10, TimeUnit.SECONDS);
                List<String> raceStates = List.copyOf(states);
                boolean expired = raceStates.contains("55P03");
                if (expired) problem(loser, 409, "CATALOG_DRAFT_CONFLICT");
                if (loser.statusCode() == 200) {
                    assertThat(expired).isFalse();
                    assertThat(entries).anyMatch(entry -> entry.pid() == contenderPid.get() && entry.sql().startsWith("SELECT pg_advisory_xact_lock"));
                }
                if (expire) assertThat(raceStates).contains("55P03");
                return new Race(won, loser, expired);
            } finally { release.countDown(); after = null; }
        }
    }
    void clean() throws Exception { base.clean(); }
    void provisioningOracle() throws Exception {
        try (var independent = new CustomerBusinessNumbersFixture()) {
            independent.start(null);
            var result = independent.psql("check");
            assertThat(result.getExitCode()).as(result.getStderr()).isZero();
            assertThat(result.getStdout()).contains("PA25_CHECK_OK logins=7 effective_roles=9 memberships=11");
        }
    }
    @Override public void close() { base.close(); }
}
