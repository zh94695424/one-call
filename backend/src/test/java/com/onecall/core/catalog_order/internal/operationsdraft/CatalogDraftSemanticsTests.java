package com.onecall.core.catalog_order.internal.operationsdraft;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;
import static com.onecall.core.catalog_order.internal.operationsdraft.JooqCatalogDraftRepository.*;
import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CatalogDraftSemanticsTests {
    static final UUID PRODUCT = new UUID(0, 1), VERSION = new UUID(0, 2), EMPLOYEE = new UUID(0, 3);
    static final Commercial COMMERCIAL = new Commercial("ONE_TIME", "PERSONAL", 0L, "CNY", "ONCE", 1, 0);
    static final Instant NOW = Instant.parse("2026-11-20T00:00:00.123456Z");
    static final String GOLDEN = "{\"audience\":\"PERSONAL\",\"benefitLines\":[],\"billingCycle\":\"ONCE\",\"channels\":[],\"currency\":\"CNY\",\"grantIntervalMonths\":0,\"installments\":1,\"offerType\":\"ONE_TIME\",\"priceCents\":0,\"productId\":\"00000000-0000-0000-0000-000000000001\",\"versionNo\":1}";
    static final String HASH = "eecbe2e59df56e661efa42fb92262cc6a8d507646ecf964b7aa1820f6ed7a35a";
    static StoredDraft stored(String snapshot, String hash, int revision, Instant time) { return new StoredDraft(VERSION, PRODUCT, 1, "DRAFT", COMMERCIAL, snapshot, hash, EMPLOYEE, time, null, revision, List.of(), List.of()); }
    static StoredDraft valid() { return stored(GOLDEN, HASH, 1, NOW); }
    static void failure(Runnable action, Code code) { assertThatThrownBy(action::run).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(code)); }
    String projection(Commercial commercial) { return CatalogDraftSemantics.projection(PRODUCT, 1, commercial, List.of(), List.of()); }
    String wire(CatalogVersionDraft response) { return JsonMapper.builder().build().writeValueAsString(response); }

    @Test void projectionHasRecursivelySortedKeysExactTypesAndNulls() {
        assertThat(projection(COMMERCIAL)).isEqualTo(GOLDEN); assertThat(projection(new Commercial("QUOTE", "ENTERPRISE", null, "CNY", "QUOTE", 1, 0))).contains("\"priceCents\":null");
        assertThat(new String(CatalogDraftSemantics.canonicalBytes("{\"z\":{\"b\":1,\"a\":2},\"a\":null}"), StandardCharsets.UTF_8)).isEqualTo("{\"a\":null,\"z\":{\"a\":2,\"b\":1}}");
    }
    @Test void projectionOrdersBenefitsAndChannelsIndependentlyOfInput() {
        var line = new BenefitLine("AI_SECONDS", 1, 1, 0);
        String projected = CatalogDraftSemantics.projection(PRODUCT, 1, COMMERCIAL, List.of(line), List.of("WEB", "MINIAPP"));
        assertThat(projected.getBytes(StandardCharsets.UTF_8)).hasSize(339); assertThat(CatalogDraftSemantics.sha256(projected)).isEqualTo("88a88dbd9303d10a4e58fb39778feccc19d0cf5bcf2d176bd7c725ec4b8cdec0");
        var other = new BenefitLine("STORAGE_BYTES", 3, 5, 2);
        assertThat(CatalogDraftSemantics.projection(PRODUCT, 1, COMMERCIAL, List.of(other, line), List.of("WEB", "MINIAPP")))
                .isEqualTo(CatalogDraftSemantics.projection(PRODUCT, 1, COMMERCIAL, List.of(line, other), List.of("MINIAPP", "WEB")));
    }
    @Test void projectionExcludesAllGeneratedAndHiddenMetadata() { assertThat(projection(COMMERCIAL)).doesNotContain("catalogVersionId", "revision", "createdAt", "publishedAt", "snapshotSha256", "token", "creator", "saleStatus"); assertThat(JsonMapper.builder().build().readTree(GOLDEN).size()).isEqualTo(11); }
    @Test void snapshotHashUsesExactUtf8BytesWithoutNewline() {
        assertThat(GOLDEN.getBytes(StandardCharsets.UTF_8)).hasSize(238); assertThat(CatalogDraftSemantics.sha256(CatalogDraftSemantics.canonicalBytes(GOLDEN))).isEqualTo(HASH); assertThat(CatalogDraftSemantics.sha256(GOLDEN + "\n")).isNotEqualTo(HASH);
        for (long value : List.of(9007199254740992L, 9007199254740993L, Long.MAX_VALUE)) {
            String expected = GOLDEN.replace("\"priceCents\":0", "\"priceCents\":" + value);
            String actual = projection(new Commercial("ONE_TIME", "PERSONAL", value, "CNY", "ONCE", 1, 0));
            assertThat(actual).isEqualTo(expected); assertThat(CatalogDraftSemantics.canonicalBytes(actual)).isEqualTo(expected.getBytes(StandardCharsets.UTF_8));
            assertThat(CatalogDraftSemantics.sha256(actual)).isEqualTo(independentHash(expected));
            var benefit = new BenefitLine("AI_SECONDS", value, 1, 0);
            String withBenefit = CatalogDraftSemantics.projection(PRODUCT, 1, COMMERCIAL, List.of(benefit), List.of());
            assertThat(withBenefit).contains("\"quantity\":" + value); assertThat(CatalogDraftSemantics.canonicalBytes(withBenefit)).isEqualTo(withBenefit.getBytes(StandardCharsets.UTF_8));
        }
        assertThat(projection(new Commercial("ONE_TIME", "PERSONAL", 9007199254740993L, "CNY", "ONCE", 1, 0))).isNotEqualTo(projection(new Commercial("ONE_TIME", "PERSONAL", 9007199254740992L, "CNY", "ONCE", 1, 0)));
    }
    static String independentHash(String value) { try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception failure) { throw new AssertionError(failure); } }
    @Test void reconstructsJsonbWithoutHashingDatabaseText() {
        String reordered = "{\"versionNo\":1," + GOLDEN.substring(1, GOLDEN.length() - ",\"versionNo\":1}".length()) + "}";
        assertThat(CatalogDraftSemantics.validateStored(stored(reordered, HASH, 1, NOW))).isEqualTo(GOLDEN);
        failure(() -> CatalogDraftSemantics.validateStored(stored(reordered, independentHash(reordered), 1, NOW)), Code.SERVICE_UNAVAILABLE);
    }
    @Test void requiresBytesAsWellAsHashForProjectionEquality() { assertThat(CatalogDraftSemantics.sameProjection(GOLDEN, GOLDEN.replace("\"priceCents\":0", "\"priceCents\":1"))).isFalse(); failure(() -> CatalogDraftSemantics.validateStored(stored(GOLDEN.replace("\"priceCents\":0", "\"priceCents\":1"), HASH, 1, NOW)), Code.SERVICE_UNAVAILABLE); }
    @Test void validatesCommercialMatrixAndClassifiesLegacyAndCorruptStoredFacts() {
        for (String type : List.of("ONE_TIME", "ADD_ON", "SUBSCRIPTION", "QUOTE")) for (String audience : List.of("PERSONAL", "ENTERPRISE")) for (String cycle : List.of("ONCE", "MONTHLY", "QUARTERLY", "ANNUAL", "QUOTE")) {
            boolean subscription = type.equals("SUBSCRIPTION"), quote = type.equals("QUOTE");
            var commercial = new Commercial(type, audience, quote ? null : 1L, "CNY", cycle, 1, subscription ? 1 : 0);
            boolean valid = audience.equals(quote ? "ENTERPRISE" : "PERSONAL") && (subscription ? List.of("MONTHLY", "QUARTERLY", "ANNUAL").contains(cycle) : cycle.equals(quote ? "QUOTE" : "ONCE"));
            if (valid) CatalogDraftSemantics.validateCommercial(commercial); else failure(() -> CatalogDraftSemantics.validateCommercial(commercial), Code.VALIDATION_FAILED);
        }
        failure(() -> CatalogDraftSemantics.validateStored(stored("{}", HASH, 1, NOW)), Code.CATALOG_DRAFT_CONFLICT);
        for (String snapshot : List.of("[]", GOLDEN.replace("\"priceCents\":0", "\"priceCents\":\"0\""), GOLDEN.replace("\"channels\":[]", "\"channels\":[\"WEB\",\"WEB\"]"))) failure(() -> CatalogDraftSemantics.validateStored(stored(snapshot, HASH, 1, NOW)), Code.SERVICE_UNAVAILABLE);
        failure(() -> CatalogDraftSemantics.validateStored(new StoredDraft(VERSION, PRODUCT, 1, "ACTIVE", COMMERCIAL, "{}", HASH, EMPLOYEE, NOW, NOW, 2, List.of(), List.of())), Code.CATALOG_DRAFT_CONFLICT);
    }
    @Test void responseContainsExactlySeventeenFrozenFields() { String encoded = wire(CatalogDraftSemantics.response(valid())); assertThat(JsonMapper.builder().build().readTree(encoded).size()).isEqualTo(17); assertThat(encoded.getBytes(StandardCharsets.UTF_8)).hasSize(475); assertThat(independentHash(encoded)).isEqualTo("7f759e65fa0db683941cf207f7d3aee6a15a3b9a7a70cb9459bf799f0a3868e9"); }
    @Test void responsePreservesNullPriceAndPublishedAtAndEmptyArrays() { var quote = new Commercial("QUOTE", "ENTERPRISE", null, "CNY", "QUOTE", 1, 0); String snapshot = projection(quote); var draft = new StoredDraft(VERSION, PRODUCT, 1, "DRAFT", quote, snapshot, independentHash(snapshot), EMPLOYEE, NOW, null, 1, List.of(), List.of()); assertThat(wire(CatalogDraftSemantics.response(draft))).contains("\"priceCents\":null", "\"publishedAt\":null", "\"benefitLines\":[]", "\"channels\":[]"); }
    @Test void responseUsesStoredUtcMicrosecondTimestamp() {
        String[] times = {"2026-11-20T00:00:00Z", "2026-11-20T00:00:00.123Z", "2026-11-20T00:00:00.123400Z"};
        String[] hashes = {"f749c92277d39f199d689fa931477c17f410c283858d247603fcbebbed7370b0", "f432f913c3ef690f0f7ddbe431fb9d43c0d90b902678be89c7f2cf1d783d6f6f", "c39ceb166d38ca7cd5da855ea25dd26d77d91089faa2db5df8e53866e47f14f6"};
        int[] sizes = {468, 472, 475};
        for (int index = 0; index < times.length; index++) { String response = wire(CatalogDraftSemantics.response(stored(GOLDEN, HASH, 1, Instant.parse(times[index])))); assertThat(response.getBytes(StandardCharsets.UTF_8)).hasSize(sizes[index]); assertThat(independentHash(response)).isEqualTo(hashes[index]); }
        failure(() -> CatalogDraftSemantics.response(stored(GOLDEN, HASH, 1, Instant.parse("2026-11-20T00:00:00.123456789Z"))), Code.SERVICE_UNAVAILABLE);
    }
    @Test void tokensBindVersionChannelRevisionAndProjectionHash() { String expected = independentHash("catalog-draft-token:v1\n" + VERSION + "\nWEB\n1\n" + HASH); assertThat(CatalogDraftSemantics.token(VERSION, "WEB", 1, HASH)).isEqualTo(expected); assertThat(List.of(expected, CatalogDraftSemantics.token(VERSION, "MINIAPP", 1, HASH), CatalogDraftSemantics.token(VERSION, "WEB", 2, HASH), CatalogDraftSemantics.token(PRODUCT, "WEB", 1, HASH))).doesNotHaveDuplicates(); }
    @Test void revisionArithmeticRejectsOverflowWithoutWrapping() { assertThat(CatalogDraftSemantics.increment(Integer.MAX_VALUE - 1)).isEqualTo(Integer.MAX_VALUE); for (int revision : List.of(-1, 0, Integer.MAX_VALUE)) failure(() -> CatalogDraftSemantics.increment(revision), Code.SERVICE_UNAVAILABLE); }
    @Test void replayRequiresExactSuccessorRevisionAndProjection() { assertThat(CatalogDraftSemantics.response(stored(GOLDEN, HASH, 2, NOW)).revision()).isEqualTo(2); assertThat(CatalogDraftSemantics.sameProjection(GOLDEN, GOLDEN)).isTrue(); failure(() -> CatalogDraftSemantics.validateStored(stored(GOLDEN, "f".repeat(64), 2, NOW)), Code.SERVICE_UNAVAILABLE); }
    @Test void auditSnapshotsAndRequestIdsAreActionDerivedAndRedacted() { assertThat(CatalogDraftSemantics.auditSnapshot(PRODUCT, 1, 2, HASH)).isEqualTo("{\"productId\":\"" + PRODUCT + "\",\"versionNo\":1,\"saleStatus\":\"DRAFT\",\"revision\":2,\"snapshotSha256\":\"" + HASH + "\"}"); assertThat(CatalogDraftSemantics.auditRequestId(VERSION, true, 1)).isEqualTo("catalog_version:" + VERSION + ":draft:create"); assertThat(CatalogDraftSemantics.auditRequestId(VERSION, false, 2)).isEqualTo("catalog_version:" + VERSION + ":draft:replace:2"); }
}
