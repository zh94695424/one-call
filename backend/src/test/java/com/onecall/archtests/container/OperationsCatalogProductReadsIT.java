package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.OperationsCatalogProductReadFixture.BASE;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.problem;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.jooq.impl.DefaultExecuteListener;
import org.junit.jupiter.api.*;

class OperationsCatalogProductReadsIT {
    static final OperationsCatalogProductReadFixture fixture = new OperationsCatalogProductReadFixture();
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @AfterEach void clean() throws Exception { fixture.unchanged(); }
    private UUID seed() throws Exception { UUID product = fixture.product(1, "AB"); fixture.version(product, 1, "DRAFT", false); return product; }
    @Test void readsAdminAndOps() throws Exception { seed(); for (String token : List.of("admin", "ops")) assertThat(fixture.shared.request("GET", BASE, token).statusCode()).isEqualTo(200); }
    @Test void returnsAllVersionStates() throws Exception { UUID id = seed(); fixture.version(id, 2, "ACTIVE", false); fixture.version(id, 3, "OFFSALE", false); assertThat(fixture.get(BASE).get("items").get(0).get("versions").valueStream().map(row -> row.get("saleStatus").asString()).toList()).containsExactly("DRAFT", "ACTIVE", "OFFSALE"); }
    @Test void retainsOfferlessDraft() throws Exception { UUID id = seed(); assertThat(fixture.get(BASE).get("items").get(0).get("versions")).hasSize(1); assertThat(fixture.get(BASE + "/" + id).get("versions")).hasSize(1); }
    @Test void ignoresMultiChannelOffersForMembership() throws Exception {
        UUID id = fixture.product(1, "AB"), version = fixture.version(id, 1, "ACTIVE", false); String before = fixture.get(BASE).toString();
        for (String channel : List.of("MINIAPP", "WEB")) fixture.mutate("INSERT INTO catalog_order.catalog_offer(id,catalog_version_id,channel,version_token) VALUES (?,?,?,'PRIVATE')", UUID.randomUUID(), version, channel);
        assertThat(fixture.get(BASE).toString()).isEqualTo(before);
    }
    @Test void returnsZeroVersionProduct() throws Exception { UUID id = fixture.product(1, "AB"); assertThat(fixture.get(BASE).get("items").get(0).get("versions")).isEmpty(); assertThat(fixture.get(BASE + "/" + id).get("versions")).isEmpty(); }
    @Test void ordersRootsWithCCol() throws Exception { int index = 0; for (String code : List.of("AB", "A-1", "A_1", "A0")) fixture.product(++index, code); assertThat(fixture.get(BASE).get("items").valueStream().map(row -> row.get("productCode").asString()).toList()).containsExactly("A-1", "A0", "AB", "A_1"); }
    @Test void pagesLimitOne() throws Exception { seed(); fixture.product(2, "AC"); var page = fixture.get(BASE + "?limit=1"); assertThat(page.get("items")).hasSize(1); assertThat(page.get("hasMore").asBoolean()).isTrue(); assertThat(page.get("nextCursor").asString().length()).isLessThanOrEqualTo(512); assertThat(fixture.get(BASE + "?limit=1&cursor=" + page.get("nextCursor").asString()).get("items").get(0).get("productCode").asString()).isEqualTo("AC"); }
    @Test void pagesLimitOneHundred() throws Exception { for (int index = 1; index <= 101; index++) fixture.product(index, "A%03d".formatted(index)); var page = fixture.get(BASE + "?limit=100"); assertThat(page.get("items")).hasSize(100); assertThat(page.get("hasMore").asBoolean()).isTrue(); }
    @Test void pagesExactBoundary() throws Exception { seed(); var page = fixture.get(BASE + "?limit=1"); assertThat(page.get("hasMore").asBoolean()).isFalse(); assertThat(page.get("nextCursor").isNull()).isTrue(); }
    @Test void pagesLastPage() throws Exception { var empty = fixture.get(BASE); assertThat(empty.get("items")).isEmpty(); seed(); fixture.product(2, "AC"); String cursor = fixture.get(BASE + "?limit=1").get("nextCursor").asString(); var tail = fixture.get(BASE + "?limit=1&cursor=" + cursor); assertThat(tail.get("items")).hasSize(1); assertThat(tail.get("nextCursor").isNull()).isTrue(); }
    @Test void doesNotDuplicateOrSkipRoots() throws Exception {
        int index = 0; for (String code : List.of("AB", "A-1", "A_1", "A0")) { UUID id = fixture.product(++index, code); for (int number = 1; number <= 4; number++) fixture.version(id, number, "DRAFT", false); }
        var seen = new ArrayList<String>(); String cursor = null;
        do { var page = fixture.get(BASE + "?limit=1" + (cursor == null ? "" : "&cursor=" + cursor)); seen.add(page.get("items").get(0).get("productCode").asString()); assertThat(page.get("items").get(0).get("versions")).hasSize(4); cursor = page.get("nextCursor").isNull() ? null : page.get("nextCursor").asString(); } while (cursor != null && seen.size() < 6);
        assertThat(seen).containsExactly("A-1", "A0", "AB", "A_1");
    }
    @Test void ordersVersions() throws Exception { UUID id = fixture.product(1, "AB"); fixture.version(id, 3, "DRAFT", false); fixture.version(id, 1, "DRAFT", false); fixture.version(id, 2, "DRAFT", false); assertThat(fixture.get(BASE + "/" + id).get("versions").valueStream().map(row -> row.get("versionNo").asInt()).toList()).containsExactly(1, 2, 3); }
    @Test void ordersBenefits() throws Exception { UUID id = fixture.product(1, "AB"), version = fixture.version(id, 1, "DRAFT", false); fixture.benefit(version, 2, "AI_SECONDS"); fixture.benefit(version, 1, "RECORDING_SECONDS"); assertThat(fixture.get(BASE + "/" + id).get("versions").get(0).get("benefitLines").valueStream().map(row -> row.get("quantity").asLong()).toList()).containsExactly(1L, 2L); }
    @Test void returnsDeclaredPublishedAtNull() throws Exception { UUID id = seed(); var version = fixture.get(BASE + "/" + id).get("versions").get(0); assertThat(version.has("publishedAt")).isTrue(); assertThat(version.get("publishedAt").isNull()).isTrue(); }
    @Test void returnsQuotePriceNull() throws Exception { UUID id = fixture.product(1, "AB"); fixture.version(id, 1, "DRAFT", true); var version = fixture.get(BASE + "/" + id).get("versions").get(0); assertThat(version.has("priceCents")).isTrue(); assertThat(version.get("priceCents").isNull()).isTrue(); }
    @Test void rejectsNonCnyCurrency() throws Exception { UUID id = seed(); fixture.mutate("UPDATE catalog_order.catalog_version SET currency='USD',revision=revision+1 WHERE product_id=?", id); problem(fixture.shared.request("GET", BASE + "/" + id, "admin"), 503, "SERVICE_UNAVAILABLE"); }
    @Test void returnsDetailHistory() throws Exception { UUID id = seed(); fixture.version(id, 2, "OFFSALE", false); fixture.version(id, 3, "ACTIVE", false); var history = fixture.get(BASE + "/" + id).get("versions"); assertThat(history).hasSize(3); assertThat(history.get(1).get("publishedAt").asString()).isEqualTo(fixture.shared.time.toString()); assertThat(history.get(2).get("saleStatus").asString()).isEqualTo("ACTIVE"); }
    @Test void returnsEmptyBenefits() throws Exception { UUID id = seed(); assertThat(fixture.get(BASE + "/" + id).get("versions").get(0).get("benefitLines")).isEmpty(); }
    @Test void rejectsMalformedFacts() throws Exception {
        seed(); fixture.shared.dsl.configuration().set(new DefaultExecuteListener() { @Override public void fetchEnd(org.jooq.ExecuteContext context) { if (context.sql().contains("FROM catalog_order.catalog_product") && context.result() != null && !context.result().isEmpty()) context.result().get(0).set(org.jooq.impl.DSL.field("product_code", String.class), "bad"); } });
        problem(fixture.shared.request("GET", BASE, "admin"), 503, "SERVICE_UNAVAILABLE");
    }
    @Test void keepsRepeatableReadSnapshot() throws Exception {
        UUID id = seed(); var entered = new CountDownLatch(1); var changed = new CountDownLatch(1); var armed = new AtomicBoolean(true);
        fixture.shared.dsl.configuration().set(new DefaultExecuteListener() { @Override public void executeStart(org.jooq.ExecuteContext context) { if (context.sql().contains("FROM catalog_order.catalog_version") && armed.getAndSet(false)) { entered.countDown(); try { assertThat(changed.await(10, TimeUnit.SECONDS)).isTrue(); } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); } } } });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var result = executor.submit(() -> fixture.shared.get(BASE)); assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue(); fixture.version(id, 2, "DRAFT", false); changed.countDown();
            assertThat(result.get(10, TimeUnit.SECONDS).get("items").get(0).get("versions")).hasSize(1);
        } finally { changed.countDown(); fixture.shared.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]); }
        assertThat(fixture.get(BASE).get("items").get(0).get("versions")).hasSize(2);
    }
}
