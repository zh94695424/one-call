package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.OperationsOrderReadFixture.BASE;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.problem;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.internal.security.EmployeeBearerAuthenticator;
import java.util.*;
import java.time.Instant;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.*;
import tools.jackson.databind.JsonNode;

class OperationsOrdersReadIT {
    static final OperationsOrderReadFixture fixture = new OperationsOrderReadFixture();
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @AfterEach void clean() throws Exception { fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]); fixture.clean(); }

    private JsonNode success(String path, String token) throws Exception {
        String before = fixture.inventory();
        var response = fixture.request("GET", path, token);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(fixture.inventory()).isEqualTo(before);
        return fixture.mapper.readTree(response.body());
    }

    @Test void traceProbeSelectsExactErrorDispatchVariant() throws Exception {
        fixture.witness.drain();
        var business = new java.util.concurrent.atomic.AtomicInteger();
        fixture.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(org.jooq.ExecuteContext context) { business.incrementAndGet(); }
        });
        String detail = BASE + "/00000000-0000-0000-0000-000000000001";
        var family = List.of(BASE, detail, BASE + "/", BASE + "//", BASE + "//x", BASE + ";x", BASE + "/x;y",
                BASE + "/%2fx", BASE + "/x%25", BASE + "/first/second", BASE + "/a/b/c", BASE + "/x/");
        for (String path : family) {
            var response = fixture.request("TRACE", path, null);
            System.out.println("PA43 TRACE family method=TRACE rawURI=" + path + " contextPath= status=" + response.statusCode()
                    + " headers=" + response.headers().map() + " body=" + response.body());
            problem(response, 404, "NOT_FOUND");
            witnessedErrorDispatch("TRACE", path, 405);
        }
        for (String path : List.of(BASE, detail, BASE + "/", BASE + "/first/second"))
            for (String method : List.of("POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")) {
                var response = fixture.request(method, path, null);
                assertThat(response.statusCode()).as(method + " " + path).isEqualTo(404);
                assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
                assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
                if (method.equals("HEAD")) assertThat(response.body()).as("HEAD carries no body").isEmpty();
                else problem(response, 404, "NOT_FOUND");
                assertThat(fixture.witness.drain()).as("REQUEST-dispatch rejection never ERROR-dispatches " + method + " " + path).isEmpty();
            }
        for (String path : List.of(BASE + "/", BASE + "//x", BASE + "/x;y", BASE + "/%2fx", BASE + "/first/second"))
            for (String token : java.util.Arrays.asList("admin", "wrong", null)) {
                var response = fixture.request("GET", path, token);
                assertThat(response.statusCode()).as("alias GET rejected before auth " + path).isEqualTo(404);
                assertThat(fixture.witness.drain()).as("alias GET rejected on REQUEST dispatch " + path).isEmpty();
            }
        for (String path : List.of(BASE, detail))
            for (String token : java.util.Arrays.asList("admin", "wrong", null)) {
                problem(fixture.request("TRACE", path, token), 404, "NOT_FOUND");
                witnessedErrorDispatch("TRACE", path, 405);
            }
        assertThat(business.get()).as("zero business/employee SQL on rejected routes").isZero();
        fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]);
        var activations = fixture.request("TRACE", "/api/v1/operations/activations", null);
        System.out.println("PA43 TRACE sibling activations status=" + activations.statusCode() + " body=" + activations.body());
        problem(activations, 404, "NOT_FOUND");
        witnessedErrorDispatch("TRACE", "/api/v1/operations/activations", 405);
        for (String path : List.of("/api/v1/operations/subjects", "/api/v1/operations/ivr-sessions",
                "/api/v1/operations/catalog/products", BASE + "foo", BASE + "-x",
                "/api/v1/operations/./orders", "/api/v1/operations/%6frders", "/api/v1/operations/ORDERS")) {
            var response = fixture.request("TRACE", path, null);
            System.out.println("PA43 TRACE outside-family method=TRACE rawURI=" + path + " status=" + response.statusCode()
                    + " headers=" + response.headers().map() + " body=" + response.body());
            assertThat(response.statusCode()).as("outside bounded family preserves existing sanitized500 " + path).isEqualTo(500);
            assertThat(fixture.mapper.readTree(response.body()).get("code").asString()).isEqualTo("INTERNAL");
            witnessedErrorDispatch("TRACE", path, 405);
        }
    }
    private static void witnessedErrorDispatch(String method, String uri, int status) {
        var observations = fixture.witness.drain();
        assertThat(observations).as("one native ERROR dispatch witnessed for " + method + " " + uri).hasSize(1);
        var observation = observations.getFirst();
        assertThat(observation.get("method")).as("ERROR_METHOD for " + uri).isEqualTo(method);
        assertThat(observation.get("uri")).as("ERROR_REQUEST_URI for " + uri).isEqualTo(uri);
        assertThat(observation.get("status")).as("ERROR_STATUS_CODE for " + uri).isEqualTo(status);
        assertThat(observation.get("contextPath")).as("ERROR contextPath for " + uri).isEqualTo("");
    }
    @Test void bothEmployeeRolesReadExactHistoricalProjectionAndCurrentIndependentStatuses() throws Exception {
        fixture.seed(); UUID identity = fixture.order(1, "UNKNOWN");
        fixture.issuance(identity, 1, "PENDING"); fixture.issuance(identity, 2, "FAILED");
        for (String token : List.of("admin", "ops")) {
            var page = success(BASE, token);
            assertThat(page.propertyNames()).containsExactlyInAnyOrder("items", "nextCursor", "hasMore");
            var summary = page.get("items").get(0);
            assertThat(summary.propertyNames()).containsExactlyInAnyOrder("orderId", "orderNo", "subjectId", "channel", "catalogVersionId",
                    "catalogOfferId", "amountCents", "currency", "paymentStatus", "issuanceStatus", "createdAt");
            assertThat(summary.get("paymentStatus").asString()).isEqualTo("UNKNOWN");
            assertThat(summary.get("issuanceStatus").asString()).isEqualTo("MIXED");
            var detail = success(BASE + "/" + identity, token);
            assertThat(detail.propertyNames()).hasSize(16);
            assertThat(detail.get("snapshot").get("versionToken").asString()).isEqualTo("PA43-historical");
            assertThat(detail.get("snapshotSha256").asString()).isEqualTo(OperationsOrderReadFixture.sha(fixture.snapshot()));
            assertThat(detail.get("paidAt").isNull()).isTrue();
        }
        try (var connection = fixture.admin()) {
            execute(connection, "UPDATE catalog_order.customer_order SET payment_status='PAID',paid_at=?::timestamptz,revision=revision+1 WHERE id=?",
                    fixture.time.toString(), identity);
        }
        assertThat(success(BASE + "/" + identity, "admin").get("paymentStatus").asString()).isEqualTo("PAID");
    }
    @Test void structuredFiltersAndCursorPagingRemainStableAcrossLimitAndEmployeeChanges() throws Exception {
        fixture.seed();
        var expected = new ArrayList<UUID>();
        for (int index = 1; index <= 25; index++) expected.add(fixture.order(index, index % 2 == 0 ? "UNKNOWN" : "UNPAID"));
        expected.sort((left, right) -> {
            int leftSecond = (int) left.getLeastSignificantBits() / 10, rightSecond = (int) right.getLeastSignificantBits() / 10;
            return leftSecond == rightSecond ? right.toString().compareTo(left.toString()) : Integer.compare(leftSecond, rightSecond);
        });
        var collected = new ArrayList<String>();
        String next = BASE + "?limit=1";
        boolean first = true;
        while (next != null) {
            var page = success(next, first ? "admin" : "ops");
            page.get("items").forEach(item -> collected.add(item.get("orderId").asString()));
            next = page.get("hasMore").asBoolean() ? BASE + "?limit=7&cursor=" + page.get("nextCursor").asString() : null;
            first = false;
        }
        assertThat(collected).containsExactlyElementsOf(expected.stream().map(UUID::toString).toList());
        for (String filter : List.of("subjectId=" + fixture.subject, "paymentStatus=UNKNOWN", "subjectId=" + fixture.subject + "&paymentStatus=UNKNOWN")) {
            var page = success(BASE + "?limit=1&" + filter, "admin");
            String cursor = page.get("nextCursor").asString();
            problem(fixture.request("GET", BASE + "?cursor=" + cursor, "admin"), 400, "CURSOR_FILTER_MISMATCH");
            assertThat(success(BASE + "?limit=100&" + filter + "&cursor=" + cursor, "ops").get("items").size()).isPositive();
        }
    }
    @Test void authorityValidationAndDependencyFailuresAreSanitizedAndWriteNothing() throws Exception {
        fixture.seed(); fixture.order(1, "UNKNOWN");
        String before = fixture.inventory();
        for (String path : List.of(BASE, BASE + "/not-uuid")) {
            problem(fixture.request("GET", path, null), 401, "UNAUTHENTICATED");
            problem(fixture.request("GET", path, "wrong"), 401, "UNAUTHENTICATED");
            problem(fixture.request("GET", path, "missing"), 401, "UNAUTHENTICATED");
            problem(fixture.request("GET", path, "disabled"), 403, "FORBIDDEN");
        }
        for (String query : List.of("limit=0", "limit=101", "limit=1&limit=1", "subjectId=x", "paymentStatus=paid", "role=ADMIN"))
            problem(fixture.request("GET", BASE + "?" + query, "admin"), 422, "VALIDATION_FAILED");
        problem(fixture.request("GET", BASE + "?cursor=!", "admin"), 400, "CURSOR_INVALID");
        problem(fixture.request("GET", BASE + "/" + UUID.randomUUID(), "admin"), 404, "NOT_FOUND");
        fixture.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(org.jooq.ExecuteContext context) {
                if (context.sql().contains("FROM catalog_order.customer_order")) throw new org.jooq.exception.DataAccessException("PRIVATE SQLSTATE");
            }
        });
        problem(fixture.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE");
        fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]);
        assertThat(fixture.inventory()).isEqualTo(before);
    }
    @Test void corruptDetailIs404ButUnrepresentableListIs500() throws Exception {
        fixture.seed();
        UUID corrupted = UUID.randomUUID();
        fixture.insertOrder(corrupted, fixture.subject, "UNKNOWN", fixture.time, 123, fixture.snapshot(), "b".repeat(64));
        problem(fixture.request("GET", BASE + "/" + corrupted, "admin"), 404, "NOT_FOUND");
        assertThat(success(BASE, "admin").get("items").size()).isEqualTo(1);
        UUID overflow = UUID.randomUUID();
        fixture.insertOrder(overflow, fixture.subject, "UNPAID", fixture.time, 9007199254740992L, fixture.snapshot(), null);
        String before = fixture.inventory();
        problem(fixture.request("GET", BASE, "admin"), 500, "INTERNAL");
        problem(fixture.request("GET", BASE + "/" + overflow, "admin"), 404, "NOT_FOUND");
        assertThat(fixture.inventory()).isEqualTo(before);
    }
    @Test void snapshotIsolationHidesConcurrentCommitUntilNextRequest() throws Exception {
        fixture.seed(); UUID identity = fixture.order(1, "UNKNOWN");
        var rootRead = new java.util.concurrent.CountDownLatch(1);
        var committed = new java.util.concurrent.CountDownLatch(1);
        var writer = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var future = writer.submit(() -> {
                try {
                    assertThat(rootRead.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                    try (var connection = fixture.admin()) {
                        execute(connection, "UPDATE catalog_order.customer_order SET payment_status='PAID',paid_at=?::timestamptz,revision=revision+1 WHERE id=?",
                                fixture.time.toString(), identity);
                    }
                    fixture.issuance(identity, 1, "PENDING");
                } catch (Exception failure) { throw new IllegalStateException(failure); }
                finally { committed.countDown(); }
            });
            fixture.dsl.configuration().set(new DefaultExecuteListener() {
                @Override public void executeEnd(org.jooq.ExecuteContext context) {
                    if (!context.sql().contains("FROM catalog_order.customer_order")) return;
                    rootRead.countDown();
                    try { assertThat(committed.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
                    catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
                }
            });
            var page = fixture.service.list(new Caller(fixture.employee, EmployeeBearerAuthenticator.AUDIENCE), new RawListRequest(Map.of(), List.of()));
            assertThat(page.items().getFirst().paymentStatus()).isEqualTo("UNKNOWN");
            assertThat(page.items().getFirst().issuanceStatus()).isEqualTo("NONE");
            future.get(15, java.util.concurrent.TimeUnit.SECONDS);
            fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]);
            var current = success(BASE, "admin").get("items").get(0);
            assertThat(current.get("paymentStatus").asString()).isEqualTo("PAID");
            assertThat(current.get("issuanceStatus").asString()).isEqualTo("PENDING");
        } finally { writer.shutdownNow(); }
    }
    @Test void explainsActualPreparedQueriesWithDensePostgres16Fixtures() throws Exception {
        fixture.bulk();
        record Query(String sql, List<Object> parameters, com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role role) { }
        var queries = new ArrayList<Query>();
        fixture.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(org.jooq.ExecuteContext context) {
                if (context.sql().contains("FROM catalog_order.customer_order") || context.sql().contains("FROM entitlement_metering.entitlement_issuance"))
                    queries.add(new Query(context.sql(), new ArrayList<>(context.query().getBindValues()),
                            context.sql().contains("FROM catalog_order") ? com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role.ORDER
                                    : com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role.TRIAL));
            }
        });
        var caller = new Caller(fixture.employee, EmployeeBearerAuthenticator.AUDIENCE);
        for (boolean subject : List.of(false, true)) for (boolean payment : List.of(false, true)) {
            var query = new HashMap<String, List<String>>();
            query.put("limit", List.of("100"));
            if (subject) query.put("subjectId", List.of(fixture.subject.toString()));
            if (payment) query.put("paymentStatus", List.of("UNKNOWN"));
            var page = fixture.service.list(caller, new RawListRequest(query, List.of()));
            assertThat(page.items()).hasSize(100);
            query.put("cursor", List.of(page.nextCursor()));
            assertThat(fixture.service.list(caller, new RawListRequest(query, List.of())).items()).hasSize(100);
        }
        fixture.service.detail(caller, new RawDetailRequest(new UUID(44, 1).toString(), Map.of(), List.of()));
        assertThatThrownBy(() -> fixture.service.detail(caller, new RawDetailRequest(UUID.randomUUID().toString(), Map.of(), List.of())))
                .isInstanceOf(com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure.class);
        int beforeEmpty = queries.size();
        fixture.service.list(caller, new RawListRequest(Map.of("subjectId", List.of(UUID.randomUUID().toString())), List.of()));
        assertThat(queries.size()).isEqualTo(beforeEmpty + 1);
        fixture.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]);
        String before = fixture.inventory();
        for (var query : queries) fixture.transaction.execute(status -> {
            fixture.scope.enter();
            try {
                return fixture.scope.scoped(query.role(), () -> {
                    var plan = fixture.dsl.fetch("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + query.sql(), query.parameters().toArray());
                    assertThat(plan).hasSize(1);
                    System.out.println("PA43 EXPLAIN role=" + query.role() + " preparedSQL=" + query.sql() + " plan=" + plan.getFirst().get(0));
                    return null;
                });
            } finally { fixture.scope.exit(); }
        });
        assertThat(fixture.inventory()).isEqualTo(before);
        assertThat(queries.stream().filter(query -> query.role() == com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role.ORDER)).hasSize(11);
    }
}
