package com.onecall.core.catalog_order.internal.operationscommand;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.onecall.core.catalog_order.internal.operationscommand.CatalogSaleLifecycleRepositoryHarness.*;
import com.onecall.core.operations_iam.operationscommand.OperationsCatalogSaleCommand.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

public final class CatalogSaleLifecycleSemanticsTests {
    @Test void canonicalLockKeyUsesUtf8ProductId() throws Exception {
        try (var harness = new CatalogSaleLifecycleRepositoryHarness()) {
            harness.initialize(); harness.publish(null, 1);
            String sql = "SELECT pg_advisory_xact_lock(hashtextextended(CAST(? AS text), 0))";
            verify(harness.dsl).fetch(sql, "catalog_order.publication:" + PRODUCT);
            assertThat(org.jooq.impl.DSL.using(org.jooq.SQLDialect.POSTGRES).query(sql, CatalogSaleLifecycleSemantics.lockKey(PRODUCT)).getSQL(org.jooq.conf.ParamType.INLINED))
                    .isEqualTo("SELECT pg_advisory_xact_lock(hashtextextended(CAST('catalog_order.publication:" + PRODUCT + "' AS text), 0))");
        }
    }
    @Test void dedupeKeysUseCanonicalVersionId() { assertThat(CatalogSaleLifecycleSemantics.dedupe(VERSION, true)).isEqualTo("catalog_version:" + VERSION + ":published"); assertThat(CatalogSaleLifecycleSemantics.dedupe(VERSION, false)).isEqualTo("catalog_version:" + VERSION + ":offsale"); }
    @Test void publicationReasonIsActionDerived() throws Exception {
        try (var harness = new CatalogSaleLifecycleRepositoryHarness()) {
            harness.initialize(); harness.publish(null, 1); exactAudit(harness, true);
            harness.exactOutbox(0, VERSION, 1, "ACTIVE", CatalogSaleLifecycleRepositoryHarness.NOW, HASH, true);
        }
    }
    @Test void offsaleReasonIsActionDerived() throws Exception {
        try (var harness = new CatalogSaleLifecycleRepositoryHarness()) {
            harness.initialize(); harness.active(); harness.repository.offsale(VERSION, 1, new UUID(2, 38), new UUID(3, 38)); exactAudit(harness, false);
            harness.exactOutbox(0, VERSION, 1, "OFFSALE", CatalogSaleLifecycleRepositoryHarness.NOW, HASH, false);
        }
    }
    void exactAudit(CatalogSaleLifecycleRepositoryHarness harness, boolean publication) {
        Object[] binds = {java.time.OffsetDateTime.ofInstant(CatalogSaleLifecycleRepositoryHarness.NOW, java.time.ZoneOffset.UTC), new UUID(2, 38), publication ? "CATALOG_VERSION_PUBLISH" : "CATALOG_VERSION_OFFSALE", VERSION,
                publication ? "{\"status\":\"DRAFT\",\"revision\":1,\"snapshotSha256\":\"" + HASH + "\"}" : "{\"status\":\"ACTIVE\",\"revision\":1}",
                publication ? "{\"status\":\"ACTIVE\",\"revision\":2,\"snapshotSha256\":\"" + HASH + "\"}" : "{\"status\":\"OFFSALE\",\"revision\":2}",
                "catalog_version:" + VERSION + (publication ? ":published" : ":offsale"), new UUID(3, 38).toString()};
        assertThat(harness.audits).hasSize(1); assertThat(harness.audits.getFirst()).containsExactly(binds);
        verify(harness.dsl).fetch("SELECT audit.append_fact(?::timestamptz, 'EMPLOYEE', ?, ?, 'CATALOG_VERSION', ?, 'ACTION_DERIVED', ?::jsonb, ?::jsonb, ?, ?)", binds);
        assertThat(harness.audits.getFirst().get(6)).isEqualTo(harness.outboxes.getFirst().getFirst());
    }
    @Test void publicationSnapshotsContainOnlyFrozenFields() {
        assertThat(CatalogSaleLifecycleSemantics.snapshot("DRAFT", 1, HASH)).isEqualTo("{\"status\":\"DRAFT\",\"revision\":1,\"snapshotSha256\":\"" + HASH + "\"}");
        assertThat(CatalogSaleLifecycleSemantics.snapshot("ACTIVE", 2, HASH)).isEqualTo("{\"status\":\"ACTIVE\",\"revision\":2,\"snapshotSha256\":\"" + HASH + "\"}");
    }
    @Test void offsaleSnapshotsContainOnlyFrozenFields() throws Exception {
        String payload = CatalogSaleLifecycleSemantics.payload(VERSION, PRODUCT, 1, "OFFSALE", Instant.parse("2026-11-18T00:00:00.123456Z"), HASH);
        assertThat(payload).isEqualTo("{\"catalogVersionId\":\"" + VERSION + "\",\"productId\":\"" + PRODUCT + "\",\"versionNo\":1,\"saleStatus\":\"OFFSALE\",\"publishedAt\":\"2026-11-18T00:00:00.123456Z\",\"snapshotSha256\":\"" + HASH + "\"}");
        assertThat(CatalogSaleLifecycleSemantics.sha256(payload)).isEqualTo(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8))));
        assertThat(JsonMapper.builder().build().readTree(CatalogSaleLifecycleSemantics.snapshot("ACTIVE", 1, null)).size()).isEqualTo(2);
    }
    @Test void rejectsMalformedPersistedState() {
        assertThatCode(() -> CatalogSaleLifecycleSemantics.validate(record(valid()))).doesNotThrowAnyException();
        for (String field : valid().keySet()) {
            var values = valid(); values.remove(field);
            assertThatThrownBy(() -> CatalogSaleLifecycleSemantics.validate(record(values))).as(field).isInstanceOf(Failure.class);
        }
        for (var mutation : Map.<String, Object>of("snapshot", "[]", "snapshot_sha256", "A".repeat(64), "revision", 0, "version_no", 0, "audience", "ENTERPRISE", "installments", 2, "currency", "cny", "sale_status", "UNKNOWN", "price_cents", -1L).entrySet()) {
            var values = valid(); values.put(mutation.getKey(), mutation.getValue());
            assertThatThrownBy(() -> CatalogSaleLifecycleSemantics.validate(record(values))).as(mutation.getKey()).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.SERVICE_UNAVAILABLE));
        }
        assertThatCode(() -> CatalogSaleLifecycleSemantics.validateBenefits(VERSION, List.of())).doesNotThrowAnyException();
        var benefit = new HashMap<String, Object>(Map.of("id", VERSION, "catalog_version_id", VERSION, "entitlement_type", "AI_SECONDS", "quantity", 1L, "validity_days", 1, "grant_interval_months", 0));
        CatalogSaleLifecycleSemantics.validateBenefits(VERSION, List.of(record(benefit)));
        assertThatThrownBy(() -> CatalogSaleLifecycleSemantics.validateBenefits(VERSION, List.of(record(benefit), record(benefit)))).isInstanceOf(Failure.class);
        benefit.put("quantity", 0L); assertThatThrownBy(() -> CatalogSaleLifecycleSemantics.validateBenefits(VERSION, List.of(record(benefit)))).isInstanceOf(Failure.class);
    }
    @Test void rejectsRevisionOverflow() { assertThat(CatalogSaleLifecycleSemantics.increment(Integer.MAX_VALUE - 1)).isEqualTo(Integer.MAX_VALUE); for (int revision : List.of(0, -1, Integer.MAX_VALUE)) assertThatThrownBy(() -> CatalogSaleLifecycleSemantics.increment(revision)).isInstanceOf(Failure.class); }
    @Test void nullActiveExpectationIsSyntaxOnlyForActiveIdentity() throws Exception {
        try (var fixture = new CatalogSaleLifecycleRepositoryHarness()) { fixture.initialize(); fixture.oldActive(); assertThatThrownBy(() -> fixture.publish(null, 1)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.PUBLICATION_STALE_VIEW)); assertThat(fixture.audits).isEmpty(); assertThat(fixture.outboxes).isEmpty(); }
    }
    @Test void preTransitionExpectationIsNotReplay() throws Exception {
        try (var fixture = new CatalogSaleLifecycleRepositoryHarness()) {
            fixture.initialize(); fixture.publish(null, 1);
            assertThatThrownBy(() -> fixture.publish(null, 1)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.PUBLICATION_STALE_VIEW));
            assertThatThrownBy(() -> fixture.publish(VERSION, 1)).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.PUBLICATION_STALE_VIEW));
            assertThat(fixture.publish(VERSION, 2).currentActiveVersionId()).isEqualTo(VERSION); assertThat(fixture.audits).hasSize(1); assertThat(fixture.outboxes).hasSize(1);
        }
    }
}
