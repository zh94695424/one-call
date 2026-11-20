package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

public final class OperationsCatalogVersionLifecycleIT {
    static final OperationsCatalogVersionLifecycleFixture fixture = new OperationsCatalogVersionLifecycleFixture();
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @AfterEach void clean() throws Exception { fixture.invariant(); fixture.clean(); }
    UUID draft() throws Exception { return fixture.draft(); }
    @Test void adminPublishesWithoutPreviousActive() throws Exception {
        UUID version = draft(); String content = fixture.content(); var state = fixture.success(fixture.request(version, true, null, 1)); assertThat(state.get("currentActiveVersionId").asString()).isEqualTo(version.toString()); assertThat(fixture.content()).isEqualTo(content); fixture.facts(1, 1);
        fixture.reset(); version = draft(); fixture.catalog.benefit(version, 1, "AI_SECONDS"); content = fixture.content(); state = fixture.success(fixture.request(version, true, null, 1)); assertThat(state.get("currentActiveVersionId").asString()).isEqualTo(version.toString()); assertThat(fixture.content()).isEqualTo(content); fixture.facts(1, 1);
        fixture.problem(fixture.request(version, true, version, 2, "ops"), 403, "FORBIDDEN"); fixture.problem(fixture.request(version, true, version, 2, "disabled"), 403, "FORBIDDEN"); fixture.problem(fixture.request(version, true, version, 2, "missing"), 503, "SERVICE_UNAVAILABLE"); fixture.facts(1, 1);
    }
    UUID[] pair() throws Exception { UUID product = fixture.product(); return new UUID[] { fixture.version(product, 1, "ACTIVE"), fixture.version(product, 2, "DRAFT") }; }
    @Test void adminPublishesAndRetiresPreviousActive() throws Exception { var pair = pair(); var state = fixture.success(fixture.request(pair[1], true, pair[0], 1)); assertThat(state.get("currentActiveVersionId").asString()).isEqualTo(pair[1].toString()); assertThat(fixture.value("SELECT sale_status FROM catalog_order.catalog_version WHERE id=?", pair[0])).isEqualTo("OFFSALE"); fixture.facts(1, 2); }
    @Test void retirementStatementPrecedesActivation() throws Exception { var pair = pair(); fixture.success(fixture.request(pair[1], true, pair[0], 1)); var updates = fixture.calls("UPDATE catalog_order.catalog_version"); assertThat(updates).hasSize(2); assertThat(updates.getFirst().sql()).contains("SET sale_status='OFFSALE'"); assertThat(updates.getLast().sql()).contains("SET published_at=?"); }
    @Test void publicationIncrementsBothRevisions() throws Exception { var pair = pair(); fixture.success(fixture.request(pair[1], true, pair[0], 1)); for (UUID version : pair) assertThat(fixture.value("SELECT revision FROM catalog_order.catalog_version WHERE id=?", version)).isEqualTo("2"); }
    @Test void publicationUsesOneDecisionTimestamp() throws Exception { UUID version = draft(); fixture.success(fixture.request(version, true, null, 1)); assertThat(fixture.calls("SELECT clock_timestamp()")).hasSize(1); assertThat(fixture.value("SELECT occurred_at=published_at FROM audit.audit_fact a JOIN catalog_order.catalog_version v ON a.object_id=v.id")).isEqualTo("t"); var lock = fixture.calls("pg_advisory_xact_lock").getFirst(); assertThat(lock.sql()).isEqualTo("SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))"); assertThat(lock.binds()).containsExactly("catalog_order.publication:" + fixture.value("SELECT product_id FROM catalog_order.catalog_version WHERE id=?", version)); }
    @Test void publicationAuditArgumentsAreExact() throws Exception {
        UUID version = draft(); fixture.success(fixture.request(version, true, null, 1)); var call = fixture.calls("audit.append_fact").getFirst(); assertThat(call.binds()).hasSize(8); assertThat(call.binds().get(1)).isEqualTo(fixture.shared.employee); assertThat(call.binds().get(2)).isEqualTo("CATALOG_VERSION_PUBLISH"); assertThat(call.binds().get(3)).isEqualTo(version); assertThat(call.binds().get(6)).isEqualTo("catalog_version:" + version + ":published"); assertThat(UUID.fromString((String) call.binds().get(7))).isNotNull();
        assertThat(fixture.mapper.readTree(call.binds().get(4).toString())).isEqualTo(fixture.mapper.readTree("{\"status\":\"DRAFT\",\"revision\":1,\"snapshotSha256\":\"" + "a".repeat(64) + "\"}")); fixture.facts(1, 1);
    }
    void outboxArguments(int index, UUID version, UUID product, int number, String status, java.time.Instant published, boolean publication) throws Exception {
        var call = fixture.calls("platform.outbox_append").get(index);
        String payload = "{\"catalogVersionId\":\"" + version + "\",\"productId\":\"" + product + "\",\"versionNo\":" + number + ",\"saleStatus\":\"" + status + "\",\"publishedAt\":\"" + published + "\",\"snapshotSha256\":\"" + "a".repeat(64) + "\"}";
        assertThat(call.sql()).isEqualTo("SELECT platform.outbox_append('OPERATIONS', ?, 'CATALOG_VERSION', ?, ?, ?::jsonb, NULL, ?)");
        assertThat(call.binds()).containsExactly("catalog_version:" + version + (publication ? ":published" : ":offsale"), version, publication ? "CATALOG_VERSION_PUBLISHED" : "CATALOG_VERSION_OFFSALE", payload, HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }
    @Test void publicationOutboxArgumentsAreExact() throws Exception { UUID product = fixture.product(), version = fixture.version(product, 1, "DRAFT"); var state = fixture.success(fixture.request(version, true, null, 1)); fixture.facts(1, 1); outboxArguments(0, version, product, 1, "ACTIVE", java.time.Instant.parse(state.get("publishedAt").asString()), true); }
    @Test void publicationWithOldActiveHasOneAuditAndTwoOutboxes() throws Exception { UUID product = fixture.product(), old = fixture.version(product, 1, "ACTIVE"), candidate = fixture.version(product, 2, "DRAFT"); var previousPublished = fixture.published(old); var state = fixture.success(fixture.request(candidate, true, old, 1)); fixture.facts(1, 2); outboxArguments(0, old, product, 1, "OFFSALE", previousPublished, false); outboxArguments(1, candidate, product, 2, "ACTIVE", java.time.Instant.parse(state.get("publishedAt").asString()), true); assertThat(fixture.calls("audit.append_fact").getFirst().binds().get(6)).isEqualTo(fixture.calls("platform.outbox_append").getLast().binds().getFirst()); }
    @Test void offsaleTransitionsActive() throws Exception { UUID version = fixture.version(fixture.product(), 1, "ACTIVE"); var state = fixture.success(fixture.request(version, false, null, 1)); assertThat(state.get("saleStatus").asString()).isEqualTo("OFFSALE"); assertThat(state.get("revision").asInt()).isEqualTo(2); assertThat(state.get("currentActiveVersionId").isNull()).isTrue(); fixture.facts(1, 1); }
    @Test void offsaleAuditAndOutboxArgumentsAreExact() throws Exception { UUID product = fixture.product(), version = fixture.version(product, 1, "ACTIVE"); var previousPublished = fixture.published(version); fixture.success(fixture.request(version, false, null, 1)); outboxArguments(0, version, product, 1, "OFFSALE", previousPublished, false); var binds = fixture.calls("audit.append_fact").getFirst().binds(); assertThat(binds.get(2)).isEqualTo("CATALOG_VERSION_OFFSALE"); assertThat(binds.get(4)).isEqualTo("{\"status\":\"ACTIVE\",\"revision\":1}"); assertThat(binds.get(5)).isEqualTo("{\"status\":\"OFFSALE\",\"revision\":2}"); assertThat(binds.get(6)).isEqualTo("catalog_version:" + version + ":offsale"); assertThat(binds.get(6)).isEqualTo(fixture.calls("platform.outbox_append").getFirst().binds().getFirst()); fixture.facts(1, 1); }
    @Test void publicationCurrentStateReplayIsZeroWrite() throws Exception { UUID version = draft(); fixture.success(fixture.request(version, true, null, 1)); String before = fixture.lifecycle(); assertThat(fixture.success(fixture.request(version, true, version, 2)).get("currentActiveVersionId").asString()).isEqualTo(version.toString()); assertThat(fixture.lifecycle()).isEqualTo(before); fixture.facts(1, 1); fixture.problem(fixture.request(version, true, version, 1), 409, "PUBLICATION_STALE_VIEW"); }
    @Test void offsaleCurrentStateReplayIsZeroWrite() throws Exception { UUID product = fixture.product(), version = fixture.version(product, 1, "OFFSALE"); assertThat(fixture.success(fixture.request(version, false, null, 1)).get("currentActiveVersionId").isNull()).isTrue(); UUID other = fixture.version(product, 2, "ACTIVE"); assertThat(fixture.success(fixture.request(version, false, null, 1)).get("currentActiveVersionId").asString()).isEqualTo(other.toString()); fixture.facts(0, 0); }
    @Test void preTransitionPublicationRetryIsStale() throws Exception { UUID version = draft(); fixture.success(fixture.request(version, true, null, 1)); fixture.problem(fixture.request(version, true, null, 1), 409, "PUBLICATION_STALE_VIEW"); fixture.facts(1, 1); }
    @Test void preTransitionOffsaleRetryIsStale() throws Exception { UUID version = fixture.version(fixture.product(), 1, "ACTIVE"); fixture.success(fixture.request(version, false, null, 1)); fixture.problem(fixture.request(version, false, null, 1), 409, "PUBLICATION_STALE_VIEW"); fixture.facts(1, 1); }
    @Test void stalePublicationMakesNoChange() throws Exception { UUID version = draft(); String before = fixture.lifecycle(); fixture.problem(fixture.request(version, true, new UUID(0, 99), 1), 409, "PUBLICATION_STALE_VIEW"); fixture.problem(fixture.request(version, true, null, 2), 409, "PUBLICATION_STALE_VIEW"); assertThat(fixture.lifecycle()).isEqualTo(before); fixture.facts(0, 0); }
    @Test void staleOffsaleMakesNoChange() throws Exception { UUID version = fixture.version(fixture.product(), 1, "ACTIVE"); String before = fixture.lifecycle(); fixture.problem(fixture.request(version, false, null, 2), 409, "PUBLICATION_STALE_VIEW"); assertThat(fixture.lifecycle()).isEqualTo(before); fixture.facts(0, 0); }
    @Test void unexpectedStatesMapToConflict() throws Exception { UUID product = fixture.product(), draft = fixture.version(product, 1, "DRAFT"), offsale = fixture.version(product, 2, "OFFSALE"); fixture.problem(fixture.request(draft, false, null, 99), 409, "CATALOG_PUBLICATION_CONFLICT"); fixture.problem(fixture.request(offsale, true, null, 99), 409, "CATALOG_PUBLICATION_CONFLICT"); fixture.problem(fixture.request(UUID.randomUUID(), true, null, 1), 404, "NOT_FOUND"); fixture.facts(0, 0); }
    @Test void publishedContentAndHistoryRemainByteIdentical() throws Exception { var pair = pair(); fixture.catalog.benefit(pair[1], 1, "AI_SECONDS"); String content = fixture.content(); fixture.success(fixture.request(pair[1], true, pair[0], 1)); fixture.success(fixture.request(pair[1], false, null, 2)); assertThat(fixture.content()).isEqualTo(content); }
    @Test void auditFailureRollsBackLifecycle() throws Exception {
        UUID version = draft(); String before = fixture.lifecycle(); for (String stage : List.of("AFTER_AUDIT_APPEND", "STATEMENT_TIMEOUT")) { fixture.fail(stage); fixture.problem(fixture.request(version, true, null, 1), 503, "SERVICE_UNAVAILABLE"); assertThat(fixture.lifecycle()).isEqualTo(before); fixture.facts(0, 0); fixture.readWitness(UUID.fromString(fixture.value("SELECT product_id FROM catalog_order.catalog_version WHERE id=?", version))); }
        assertThat(fixture.states).contains("57014");
    }
    @Test void outboxFailureRollsBackLifecycle() throws Exception { var pair = pair(); String before = fixture.lifecycle(); for (String stage : List.of("AFTER_RETIREMENT_OUTBOX", "AFTER_PUBLISH_OUTBOX")) { fixture.fail(stage); fixture.problem(fixture.request(pair[1], true, pair[0], 1), 503, "SERVICE_UNAVAILABLE"); assertThat(fixture.lifecycle()).isEqualTo(before); fixture.facts(0, 0); } fixture.fail("AFTER_OFFSALE_OUTBOX"); fixture.problem(fixture.request(pair[0], false, null, 1), 503, "SERVICE_UNAVAILABLE"); assertThat(fixture.lifecycle()).isEqualTo(before); fixture.facts(0, 0); }
    @Test void concurrentPublishersYieldOneActiveWinner() throws Exception {
        UUID product = fixture.product(), first = fixture.version(product, 1, "DRAFT"), second = fixture.version(product, 2, "DRAFT");
        UUID firstCandidate = first;
        try (var executor = Executors.newFixedThreadPool(2)) { var start = new CountDownLatch(1); var left = executor.submit(() -> { start.await(); return fixture.request(firstCandidate, true, null, 1); }); var right = executor.submit(() -> { start.await(); return fixture.request(second, true, null, 1); }); start.countDown(); assertThat(List.of(left.get(10, TimeUnit.SECONDS).statusCode(), right.get(10, TimeUnit.SECONDS).statusCode())).containsExactlyInAnyOrder(200, 409); } fixture.facts(1, 1);
        fixture.reset(); product = fixture.product(); first = fixture.version(product, 1, "DRAFT"); UUID competitor = fixture.version(product, 2, "DRAFT");
        fixture.activation = context -> { try { fixture.mutate("UPDATE catalog_order.catalog_version SET sale_status='ACTIVE',published_at=now(),revision=revision+1 WHERE id=?", competitor); } catch (Exception failure) { throw new IllegalStateException(failure); } };
        fixture.problem(fixture.request(first, true, null, 1), 409, "CATALOG_PUBLICATION_CONFLICT"); assertThat(fixture.states).contains("23505"); fixture.facts(0, 0); fixture.recoveryWitness(product, competitor); fixture.clean();
        fixture.reset(); product = fixture.product(); UUID candidate = fixture.version(product, 1, "DRAFT"), serialCompetitor = fixture.version(product, 2, "DRAFT");
        fixture.activation = context -> { try { fixture.mutate("UPDATE catalog_order.catalog_version SET sale_status='ACTIVE',published_at=now(),revision=revision+1 WHERE id=?", serialCompetitor); } catch (Exception failure) { throw new IllegalStateException(failure); } };
        fixture.fail("SERIALIZATION_AT_ACTIVATION"); fixture.problem(fixture.request(candidate, true, null, 1), 409, "CATALOG_PUBLICATION_CONFLICT"); fixture.facts(0, 0); fixture.recoveryWitness(product, serialCompetitor); fixture.clean();
        for (boolean publication : List.of(true, false)) {
            fixture.reset(); UUID current = fixture.version(fixture.product(), 1, publication ? "ACTIVE" : "OFFSALE");
            fixture.fail("SERIALIZATION_AT_READ"); fixture.success(fixture.request(current, publication, publication ? current : null, 1));
            fixture.facts(0, 0); assertThat(fixture.calls("SET LOCAL statement_timeout")).hasSize(2);
            assertThat(fixture.calls("SET LOCAL statement_timeout")).extracting(OperationsCatalogVersionLifecycleFixture.Entry::transaction).doesNotHaveDuplicates();
        }
        fixture.reset(); product = fixture.product(); candidate = fixture.version(product, 1, "DRAFT"); UUID lockedProduct = product, lockedCandidate = candidate;
        fixture.readWitness(product); fixture.entries.clear();
        String beforeLock = fixture.lifecycle();
        try (var admin = fixture.admin(); var executor = Executors.newFixedThreadPool(2)) {
            admin.setAutoCommit(false); execute(admin, "SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text),0))", com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleSemantics.lockKey(product));
            var observerStarted = new CountDownLatch(1);
            var waitObserved = new CountDownLatch(1);
            var observed = executor.submit(() -> {
                try (var observer = fixture.admin(); var statement = observer.prepareStatement("SELECT count(*) FROM pg_stat_activity WHERE pid = ? AND wait_event_type = 'Lock'")) {
                    observerStarted.countDown();
                    assertThat(fixture.lockAttempt.await(5, TimeUnit.SECONDS)).isTrue();
                    assertThat(fixture.lockAttemptPid).isPositive();
                    statement.setInt(1, fixture.lockAttemptPid); long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    while (System.nanoTime() < deadline) {
                        try (var rows = statement.executeQuery()) { rows.next(); if (rows.getInt(1) > 0) { waitObserved.countDown(); return true; } }
                    }
                    return false;
                }
            });
            assertThat(observerStarted.await(5, TimeUnit.SECONDS)).isTrue();
            var pending = executor.submit(() -> fixture.request(lockedCandidate, true, null, 1));
            assertThat(waitObserved.await(6, TimeUnit.SECONDS)).isTrue();
            assertThat(observed.get(6, TimeUnit.SECONDS)).isTrue(); fixture.readWitness(lockedProduct); fixture.problem(pending.get(10, TimeUnit.SECONDS), 409, "CATALOG_PUBLICATION_CONFLICT"); admin.rollback();
        }
        assertThat(fixture.states).contains("55P03"); fixture.facts(0, 0); assertThat(fixture.lifecycle()).isEqualTo(beforeLock); fixture.readWitness(product); fixture.clean();
    }
    @Test void concurrentOffsalesYieldOneWinner() throws Exception { UUID version = fixture.version(fixture.product(), 1, "ACTIVE"); try (var executor = Executors.newFixedThreadPool(2)) { var start = new CountDownLatch(1); var left = executor.submit(() -> { start.await(); return fixture.request(version, false, null, 1); }); var right = executor.submit(() -> { start.await(); return fixture.request(version, false, null, 1); }); start.countDown(); assertThat(List.of(left.get(10, TimeUnit.SECONDS).statusCode(), right.get(10, TimeUnit.SECONDS).statusCode())).containsExactlyInAnyOrder(200, 409); } fixture.facts(1, 1); }
    @Test void oneActiveInvariantHoldsAfterEveryCommit() throws Exception { UUID product = fixture.product(), active = null; for (int number = 1; number <= 4; number++) { UUID version = fixture.version(product, number, "DRAFT"); fixture.success(fixture.request(version, true, active, 1)); fixture.invariant(); active = version; } fixture.facts(4, 7); }
    @Test void routeMutationOutsideTwoTemplatesIsConcealed() throws Exception { String path = "/api/v1/operations/catalog/versions/" + draft() + "/publications;x"; fixture.problem(fixture.shared.request("POST", path, null), 404, "NOT_FOUND"); fixture.problem(fixture.shared.request("DELETE", path, "admin"), 404, "NOT_FOUND"); fixture.facts(0, 0); }
}
