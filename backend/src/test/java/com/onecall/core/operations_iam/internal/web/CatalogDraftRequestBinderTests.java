package com.onecall.core.operations_iam.internal.web;

import static com.onecall.core.operations_iam.operationscommand.OperationsCatalogDraftCommand.*;
import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogDraftRequestBinderTests {
    static final String ID = "00000000-0000-0000-0000-000000000001";
    static final String COMMERCIAL = "\"offerType\":\"ONE_TIME\",\"audience\":\"PERSONAL\",\"priceCents\":0,\"currency\":\"CNY\",\"billingCycle\":\"ONCE\",\"installments\":1,\"grantIntervalMonths\":0";
    static final String CREATE = "{\"productId\":\"" + ID + "\",\"versionNo\":1," + COMMERCIAL + ",\"benefitLines\":[],\"channels\":[]}";
    static final String REPLACE = "{\"expectedRevision\":1," + COMMERCIAL + "}";
    final CatalogDraftRequestBinder binder = new CatalogDraftRequestBinder();
    byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    void invalidCreate(String text) { assertThatThrownBy(() -> binder.create(bytes(text))).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED)); }
    void invalidReplace(String text) { assertThatThrownBy(() -> binder.replace(ID, bytes(text))).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(Code.VALIDATION_FAILED)); }

    @Test void createAcceptsExactlyFrozenFields() {
        var parsed = binder.create(bytes(CREATE)); assertThat(parsed.productId().toString()).isEqualTo(ID); assertThat(parsed.versionNo()).isEqualTo(1);
        assertThat(parsed.commercial()).isEqualTo(new Commercial("ONE_TIME", "PERSONAL", 0L, "CNY", "ONCE", 1, 0));
    }
    @Test void replaceAcceptsExactlyFrozenFields() { var parsed = binder.replace(ID, bytes(REPLACE)); assertThat(parsed.versionId().toString()).isEqualTo(ID); assertThat(parsed.expectedRevision()).isEqualTo(1); assertThat(parsed.commercial().priceCents()).isZero(); }
    @Test void rejectsUnknownDuplicateAndMissingFieldsAtEveryDepth() {
        invalidCreate(CREATE.replace("\"versionNo\":1", "\"versionNo\":1,\"versionNo\":1")); invalidReplace(REPLACE.replace("\"priceCents\":0,", "")); invalidCreate(CREATE.replace("\"channels\":[]", "\"extra\":[]"));
        String benefit = "{\"entitlementType\":\"AI_SECONDS\",\"quantity\":1,\"validityDays\":1,\"grantIntervalMonths\":0}";
        for (String broken : List.of(benefit.replace("\"quantity\":1", "\"quantity\":1,\"quantity\":1"), benefit.replace("\"quantity\":1,", ""), benefit.replace("\"quantity\":1", "\"other\":1"))) invalidCreate(CREATE.replace("\"benefitLines\":[]", "\"benefitLines\":[" + broken + "]"));
    }
    @Test void rejectsInvalidUtf8TrailingTokensAndNonObjects() {
        for (String text : List.of("[]", "null", "1", "{} {}", CREATE + " true", "\ufeff" + CREATE, "{", "")) invalidCreate(text);
        assertThatThrownBy(() -> binder.create(new byte[] {(byte) 0xc3, 0x28})).isInstanceOf(Failure.class);
    }
    @Test void requiresCanonicalProductAndVersionIds() {
        for (String identity : List.of("1-1-1-1-1", ID.toUpperCase().replace("000001", "00000A"), " " + ID, "{" + ID + "}", "bad")) {
            invalidCreate(CREATE.replace(ID, identity)); assertThatThrownBy(() -> binder.replace(identity, bytes(REPLACE))).isInstanceOf(Failure.class);
        }
    }
    @Test void enforcesIntegralPositiveAndStorageBounds() {
        for (String value : List.of("0", "-1", "2147483648", "1.0", "1e0", "\"1\"", "true", "null")) { invalidCreate(CREATE.replace("\"versionNo\":1", "\"versionNo\":" + value)); invalidReplace(REPLACE.replace("\"expectedRevision\":1", "\"expectedRevision\":" + value)); }
        for (String value : List.of("-1", "9223372036854775808", "1.0", "\"1\"")) invalidCreate(CREATE.replace("\"priceCents\":0", "\"priceCents\":" + value));
        for (long value : List.of(9007199254740993L, Long.MAX_VALUE)) assertThat(binder.create(bytes(CREATE.replace("\"priceCents\":0", "\"priceCents\":" + value))).commercial().priceCents()).isEqualTo(value);
        assertThat(binder.create(bytes(CREATE.replace("\"versionNo\":1", "\"versionNo\":2147483647"))).versionNo()).isEqualTo(Integer.MAX_VALUE);
    }
    @Test void passesGrammarValidMatrixCombinationsToOwnerWithoutPolicy() {
        for (String text : List.of(CREATE.replace("PERSONAL", "ENTERPRISE"), CREATE.replace("ONE_TIME", "SUBSCRIPTION"), CREATE.replace("\"priceCents\":0", "\"priceCents\":null"))) assertThat(binder.create(bytes(text))).isNotNull();
    }
    @Test void requiresCnyAndRejectsUnknownEnumValues() {
        for (String value : List.of("CNY", "ONE_TIME", "PERSONAL", "ONCE")) for (String replacement : List.of(value.toLowerCase(), "UNKNOWN")) invalidCreate(CREATE.replace(value, replacement));
        invalidCreate(CREATE.replace("CNY", "USD"));
    }
    @Test void acceptsUniqueBenefitLinesAndChannelsInAnyOrder() {
        String benefit = "{\"entitlementType\":\"STORAGE_BYTES\",\"quantity\":9223372036854775807,\"validityDays\":1,\"grantIntervalMonths\":0}";
        String body = CREATE.replace("\"benefitLines\":[]", "\"benefitLines\":[" + benefit + "]").replace("\"channels\":[]", "\"channels\":[\"WEB\",\"MINIAPP\"]");
        assertThat(binder.create(bytes(body)).channels()).containsExactly("WEB", "MINIAPP"); assertThat(binder.create(bytes(body)).benefitLines().getFirst().quantity()).isEqualTo(Long.MAX_VALUE);
        invalidCreate(body.replace("[\"WEB\",\"MINIAPP\"]", "[\"WEB\",\"WEB\"]")); invalidCreate(body.replace("[" + benefit + "]", "[" + benefit + "," + benefit + "]"));
    }
    @Test void acceptsEmptyBenefitsAndChannels() { var parsed = binder.create(bytes(CREATE)); assertThat(parsed.benefitLines()).isEmpty(); assertThat(parsed.channels()).isEmpty(); assertThatThrownBy(() -> parsed.channels().add("WEB")).isInstanceOf(UnsupportedOperationException.class); }
    @Test void rejectsAllClientOwnedMetadataAndImmutableReplaceFields() {
        for (String field : List.of("productId", "versionNo", "benefitLines", "channels", "saleStatus", "status", "publishedAt", "createdAt", "versionToken", "createdByEmployeeId", "snapshot", "snapshotSha256", "revision", "versionId", "catalogVersionId", "other"))
            for (String value : List.of("null", "1", "\"DRAFT\"", "[]")) invalidReplace(REPLACE.replaceFirst("\\{", "{\"" + field + "\":" + value + ","));
        for (String field : List.of("revision", "catalogVersionId", "snapshotSha256", "createdAt")) invalidCreate(CREATE.replaceFirst("\\{", "{\"" + field + "\":null,"));
    }
    @Test void rejectsOversizedEmptyAndUnreadableBodies() {
        assertThatThrownBy(() -> binder.create(null)).isInstanceOf(Failure.class); invalidCreate(" ".repeat(8193)); invalidCreate(CREATE.substring(0, CREATE.length() - 1)); invalidReplace("");
        assertThat(binder.create(bytes(CREATE + " ".repeat(8192 - CREATE.length())))).isNotNull();
    }
}
