package com.onecall.core.catalog_order.internal.customerorder;

import static org.assertj.core.api.Assertions.*;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException.Code;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class CustomerOrderSemanticsTests {
    static final UUID ACCOUNT = UUID.fromString("10000000-0000-0000-0000-000000000001"), SUBJECT = UUID.randomUUID(), VERSION = UUID.randomUUID(), OFFER = UUID.randomUUID(), PRODUCT = UUID.randomUUID(), LINE = UUID.randomUUID(), SECOND_LINE = UUID.randomUUID();
    static Caller caller() { return new Caller(ACCOUNT, "WEB", "PERSONAL", null); }
    static Command command(String key, String token) { return new Command(SUBJECT, OFFER, "WEB", "ACCEPT", "SYNTHETIC", "1", "1", "a".repeat(64), token, key, UUID.randomUUID()); }
    static String source(String terms) {
        return "{\"schemaVersion\":1,\"catalogVersionId\":\"" + VERSION + "\",\"productCode\":\"SYNTHETIC\",\"versionNo\":1,\"offerType\":\"ONE_TIME\",\"audience\":\"PERSONAL\",\"priceCents\":123,\"currency\":\"CNY\",\"billingCycle\":\"ONCE\",\"installments\":1,\"benefitLines\":[{\"lineKey\":\"" + LINE + "\",\"entitlementType\":\"AI_SECONDS\",\"quantity\":100,\"validityDays\":30,\"grantIntervalMonths\":0,\"schedule\":[{\"installmentNo\":1,\"entitlementType\":\"AI_SECONDS\",\"units\":100,\"validFrom\":\"2027-01-01T00:00:00Z\",\"expiresAt\":\"2027-02-01T00:00:00Z\"}]}],\"terms\":" + terms + "}";
    }
    static CustomerOrderRepository.Catalog catalog(String source) {
        return new CustomerOrderRepository.Catalog(OFFER, VERSION, PRODUCT, "SYNTHETIC", 1, "WEB", "ACTIVE", "ACTIVE", "Exact", "ONE_TIME", "PERSONAL", 123L, "CNY", "ONCE", 1, 0, source, CustomerOrderSemantics.sha(CustomerOrderSemantics.canonical(CustomerOrderSemantics.parse(source))));
    }
    static List<CustomerOrderRepository.BenefitRow> benefits() { return List.of(new CustomerOrderRepository.BenefitRow(LINE, "AI_SECONDS", 100, 30, 0)); }
    static List<CustomerOrderRepository.BenefitRow> multiBenefits() { return List.of(new CustomerOrderRepository.BenefitRow(LINE, "AI_SECONDS", 100, 30, 0), new CustomerOrderRepository.BenefitRow(SECOND_LINE, "STORAGE_BYTES", 50, 60, 0)); }
    static void code(Runnable action, Code code) { assertThatThrownBy(action::run).isInstanceOfSatisfying(OrderCreationException.class, failure -> assertThat(failure.code()).isEqualTo(code)); }
    @Test void consentEventIdentityMatchesCanonical81CharacterVectors() throws Exception {
        String bytes = "{\"accountId\":\"10000000-0000-0000-0000-000000000001\",\"clientIdempotencyKey\":\"key\"}";
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.getBytes(StandardCharsets.UTF_8)));
        assertThat(CustomerOrderSemantics.event(ACCOUNT, "key")).isEqualTo("purchase-consent:" + digest).hasSize(81);
        assertThat(CustomerOrderSemantics.canonical(CustomerOrderSemantics.fields("clientIdempotencyKey", "key", "accountId", ACCOUNT))).isEqualTo(bytes);
    }
    @Test void unicodeAndCaseKeysRemainDistinctWithoutNormalization() {
        assertThat(List.of("é", "e\u0301", "Key", "key", " key", "key ", "😀").stream().map(key -> CustomerOrderSemantics.event(ACCOUNT, key))).doesNotHaveDuplicates();
    }
    @Test void platformPurchaseKeyMatches512CharacterBoundary() {
        assertThat(CustomerOrderSemantics.platformKey(ACCOUNT, "😀".repeat(475)).codePointCount(0, 987)).isEqualTo(512);
        for (String key : List.of("", " ", "a".repeat(476), "\0", "\ud800")) code(() -> CustomerOrderSemantics.platformKey(ACCOUNT, key), Code.VALIDATION_FAILED);
    }
    @Test void orderFingerprintUsesOnlyFrozenTenSemanticFields() {
        var catalog = catalog(source("{}")); Command command = command("key", null);
        String expected = CustomerOrderSemantics.canonical(CustomerOrderSemantics.fields("accountId", ACCOUNT, "amountCents", 123L, "catalogOfferId", OFFER, "catalogVersionId", VERSION, "channel", "WEB", "currency", "CNY", "ownerOrganizationId", null, "purchaseConsentEventId", CustomerOrderSemantics.event(ACCOUNT, "key"), "subjectId", SUBJECT, "versionToken", "Exact"));
        assertThat(CustomerOrderSemantics.fingerprint(command, caller(), catalog)).isEqualTo(CustomerOrderSemantics.sha(expected));
        assertThat(CustomerOrderSemantics.fingerprint(command("key", "Exact"), caller(), catalog)).isEqualTo(CustomerOrderSemantics.fingerprint(command, caller(), catalog));
        assertThat(CustomerOrderSemantics.fingerprint(command("other", null), caller(), catalog)).isNotEqualTo(CustomerOrderSemantics.fingerprint(command, caller(), catalog));
    }
    @Test void consentFingerprintUsesOnlyFrozenElevenSemanticFields() throws Exception {
        var input = new com.onecall.core.activation_consent.purchaseconsent.PurchaseConsentService.PurchaseConsentInput(SUBJECT, ACCOUNT, "WEB", CustomerOrderSemantics.event(ACCOUNT, "key"), "ACCEPT", "SYNTHETIC", "1", "1", "a".repeat(64));
        Class<?> owner = Class.forName("com.onecall.core.activation_consent.internal.purchase.PurchaseConsentSemantics");
        var fingerprint = owner.getDeclaredMethod("fingerprint", input.getClass()); fingerprint.setAccessible(true);
        String bytes = CustomerOrderSemantics.canonical(CustomerOrderSemantics.fields("accountId", ACCOUNT, "action", "ACCEPT", "channel", "WEB", "eventDomain", "PURCHASE", "eventId", input.eventId(), "policyCode", "SYNTHETIC", "policyVersion", "1", "promptVersion", "1", "purpose", "PURCHASE", "sourceType", "ACCOUNT", "subjectId", SUBJECT));
        assertThat(fingerprint.invoke(null, input)).isEqualTo(CustomerOrderSemantics.sha(bytes));
        assertThat(CustomerOrderSemantics.parse(bytes).propertyNames()).hasSize(11).doesNotContain("contentSha256");
    }
    @Test void snapshotCopiesAllApprovedCommerceAndScheduleFields() {
        String full = CustomerOrderSemantics.snapshot(catalog(source("{\"approved\":true}")), benefits());
        var node = CustomerOrderSemantics.parse(full); assertThat(node.propertyNames()).hasSize(15).contains("terms", "versionNo");
        assertThat(node.get("benefitLines").get(0).get("lineKey").asString()).isEqualTo(LINE.toString());
        for (String terms : List.of("null", "[]", "{\"value\":1.0}", "{\"value\":1e0}", "{\"value\":9007199254740992}", "{\"value\":\"\\u0000\"}", "{\"value\":\"\\ud800\"}", "{\"text\":\"" + "x".repeat(1025) + "\"}")) {
            code(() -> CustomerOrderSemantics.terms(CustomerOrderSemantics.parse(terms)), Code.VALIDATION_FAILED);
        }
        CustomerOrderSemantics.terms(CustomerOrderSemantics.parse("{\"text\":\"" + "x".repeat(1024) + "\"}"));
        for (int count : List.of(255, 256)) {
            var array = CustomerOrderSemantics.JSON.createArrayNode(); for (int index = 0; index < count - 1; index++) array.add(true);
            var terms = CustomerOrderSemantics.JSON.createObjectNode().set("values", array);
            if (count == 255) CustomerOrderSemantics.terms(terms); else code(() -> CustomerOrderSemantics.terms(terms), Code.VALIDATION_FAILED);
        }
        code(() -> CustomerOrderSemantics.terms(CustomerOrderSemantics.parse("null")), Code.VALIDATION_FAILED);
        for (int depth : List.of(8, 9)) {
            String nested = "{\"a\":".repeat(depth - 1) + "true" + "}".repeat(depth - 1);
            if (depth == 8) CustomerOrderSemantics.terms(CustomerOrderSemantics.parse(nested));
            else code(() -> CustomerOrderSemantics.terms(CustomerOrderSemantics.parse(nested)), Code.VALIDATION_FAILED);
        }
        for (int bytes : List.of(8192, 8193)) {
            var bounded = CustomerOrderSemantics.JSON.createObjectNode();
            for (int index = 0; index < 8; index++) bounded.put(Character.toString('a' + index), "x".repeat(index < 7 ? 1024 : bytes - 57 - 7168));
            assertThat(CustomerOrderSemantics.canonical(bounded).getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(bytes);
            if (bytes == 8192) CustomerOrderSemantics.terms(bounded); else code(() -> CustomerOrderSemantics.terms(bounded), Code.VALIDATION_FAILED);
        }
        CustomerOrderSemantics.terms(CustomerOrderSemantics.parse("{\"min\":-9007199254740991,\"max\":9007199254740991}"));
        code(() -> CustomerOrderSemantics.terms(CustomerOrderSemantics.parse("{\"min\":-9007199254740992}")), Code.VALIDATION_FAILED);
        assertThatThrownBy(() -> CustomerOrderSemantics.JSON.readTree("{\"duplicate\":1,\"duplicate\":2}")).isInstanceOf(RuntimeException.class);
        var binary = CustomerOrderSemantics.JSON.createObjectNode().put("binary", new byte[]{1, 2});
        code(() -> CustomerOrderSemantics.terms(binary), Code.VALIDATION_FAILED);
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(CustomerOrderSemantics.class);
        var captured = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>(); captured.start(); logger.addAppender(captured);
        try {
            code(() -> CustomerOrderSemantics.terms(CustomerOrderSemantics.parse("{\"PRIVATE_KEY\":\"" + "PRIVATE_VALUE".repeat(100) + "\"}")), Code.VALIDATION_FAILED);
            assertThat(captured.list).hasSize(1);
            assertThat(captured.list.get(0).getFormattedMessage()).isEqualTo("Checkout terms rejected reason=TERMS_TEXT_LIMIT dimension=STRING limit=1024 count=1300").doesNotContain("PRIVATE_KEY", "PRIVATE_VALUE");
        } finally { logger.detachAppender(captured); captured.stop(); }
    }
    @Test void snapshotHashUsesCanonicalFullSnapshotNotProjection() {
        Map<String, String> vectors = Map.of("{}", "44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a", "{\"a\":[null,true,false,-9007199254740991,9007199254740991],\"z\":\"é\"}", "625ae4c671502c27a5448d9c889ef115851e4cdc208dafbdfc0cfdf18c383d53", "{\"a\":1,\"b\":[2,1]}", "e9d26fb0100c3f9ef569c38ced9811ec35059c3c999b8683fde59ed24e6e66e8");
        vectors.forEach((bytes, hash) -> assertThat(CustomerOrderSemantics.sha(CustomerOrderSemantics.canonical(CustomerOrderSemantics.parse(bytes)))).isEqualTo(hash));
        String full = CustomerOrderSemantics.snapshot(catalog(source("{}")), benefits()), hash = CustomerOrderSemantics.sha(full);
        assertThat(CustomerOrderSemantics.sha(CustomerOrderSemantics.canonical(CustomerOrderSemantics.projection(full, hash)))).isNotEqualTo(hash);
        var multi = (tools.jackson.databind.node.ObjectNode) CustomerOrderSemantics.parse(source("{}"));
        var lines = (tools.jackson.databind.node.ArrayNode) multi.get("benefitLines");
        var second = CustomerOrderSemantics.JSON.createObjectNode().put("lineKey", SECOND_LINE.toString()).put("entitlementType", "STORAGE_BYTES").put("quantity", 50).put("validityDays", 60).put("grantIntervalMonths", 0);
        second.set("schedule", CustomerOrderSemantics.parse("[{\"installmentNo\":1,\"entitlementType\":\"STORAGE_BYTES\",\"units\":50,\"validFrom\":\"2027-01-01T00:00:00Z\",\"expiresAt\":\"2027-03-01T00:00:00Z\"}]"));
        lines.insert(0, second); String multiSnapshot = CustomerOrderSemantics.snapshot(catalog(CustomerOrderSemantics.canonical(multi)), multiBenefits());
        var stored = CustomerOrderSemantics.parse(multiSnapshot).get("benefitLines");
        assertThat(stored).hasSize(2); assertThat(stored.get(0).get("entitlementType").asString()).isEqualTo("AI_SECONDS"); assertThat(stored.get(1).get("lineKey").asString()).isEqualTo(SECOND_LINE.toString());
        var duplicate = (tools.jackson.databind.node.ObjectNode) CustomerOrderSemantics.parse(CustomerOrderSemantics.canonical(multi)); ((tools.jackson.databind.node.ObjectNode) duplicate.withArray("benefitLines").get(1)).put("lineKey", SECOND_LINE.toString());
        code(() -> CustomerOrderSemantics.snapshot(catalog(CustomerOrderSemantics.canonical(duplicate)), multiBenefits()), Code.VALIDATION_FAILED);
        var missing = (tools.jackson.databind.node.ObjectNode) CustomerOrderSemantics.parse(CustomerOrderSemantics.canonical(multi)); missing.withArray("benefitLines").remove(1);
        code(() -> CustomerOrderSemantics.snapshot(catalog(CustomerOrderSemantics.canonical(missing)), multiBenefits()), Code.VALIDATION_FAILED);
        var extra = (tools.jackson.databind.node.ObjectNode) CustomerOrderSemantics.parse(CustomerOrderSemantics.canonical(multi)); ((tools.jackson.databind.node.ObjectNode) extra.withArray("benefitLines").get(1)).put("lineKey", UUID.randomUUID().toString());
        code(() -> CustomerOrderSemantics.snapshot(catalog(CustomerOrderSemantics.canonical(extra)), multiBenefits()), Code.INTERNAL);
        var mismatched = (tools.jackson.databind.node.ObjectNode) CustomerOrderSemantics.parse(CustomerOrderSemantics.canonical(multi)); ((tools.jackson.databind.node.ObjectNode) mismatched.withArray("benefitLines").get(1)).put("quantity", 51);
        code(() -> CustomerOrderSemantics.snapshot(catalog(CustomerOrderSemantics.canonical(mismatched)), multiBenefits()), Code.INTERNAL);
    }
    @Test void personalPayableMatrixAndIntegerAmountAreExact() {
        CustomerOrderSemantics.payable(catalog(source("{}")), command("key", null), caller());
        for (String numeric : List.of("1.0", "1e0", "9007199254740992", "9223372036854775808")) code(() -> CustomerOrderSemantics.integer(CustomerOrderSemantics.parse(numeric)), Code.VALIDATION_FAILED);
        assertThat(CustomerOrderSemantics.integer(CustomerOrderSemantics.parse("9007199254740991"))).isEqualTo(9007199254740991L);
    }
    @Test void enterpriseQuoteAndNonPersonalCatalogFactsFailClosed() {
        var original = catalog(source("{}"));
        var quote = new CustomerOrderRepository.Catalog(OFFER, VERSION, PRODUCT, "SYNTHETIC", 1, "WEB", "ACTIVE", "ACTIVE", "Exact", "QUOTE", "ENTERPRISE", null, "CNY", "QUOTE", 1, 0, original.snapshot(), original.snapshotSha256());
        code(() -> CustomerOrderSemantics.payable(quote, command("key", null), caller()), Code.VALIDATION_FAILED);
    }
    @Test void approvedSchedulesMustMatchEveryLineAndInstallment() {
        for (String source : List.of(source("{}").replace("\"units\":100", "\"units\":99"), source("{}").replace("\"installmentNo\":1", "\"installmentNo\":2"), source("{}").replace("2027-02-01", "2026-02-01"))) code(() -> CustomerOrderSemantics.snapshot(catalog(source), benefits()), Code.VALIDATION_FAILED);
        for (String value : List.of("5", "true", "null", "{}", "[]")) {
            for (String field : List.of("lineKey", "entitlementType", "quantity", "validityDays", "grantIntervalMonths", "schedule", "installmentNo", "schedule.entitlementType", "units", "validFrom", "expiresAt")) {
                var source = CustomerOrderSemantics.parse(source("{}"));
                var line = (tools.jackson.databind.node.ObjectNode) source.get("benefitLines").get(0);
                String name = field.replace("schedule.", "");
                var target = line.has(field) ? line : (tools.jackson.databind.node.ObjectNode) line.get("schedule").get(0);
                if (target.get(name).isIntegralNumber() && value.equals("5")) continue;
                target.set(name, CustomerOrderSemantics.parse(value));
                code(() -> CustomerOrderSemantics.snapshot(catalog(CustomerOrderSemantics.canonical(source)), benefits()), Code.VALIDATION_FAILED);
            }
        }
        code(() -> CustomerOrderSemantics.snapshot(catalog(source("{}").replace(LINE.toString(), "not-a-uuid")), benefits()), Code.VALIDATION_FAILED);
        for (String field : List.of("quantity", "validityDays", "grantIntervalMonths", "installmentNo", "units")) {
            var source = CustomerOrderSemantics.parse(source("{}"));
            var line = (tools.jackson.databind.node.ObjectNode) source.get("benefitLines").get(0);
            var target = line.has(field) ? line : (tools.jackson.databind.node.ObjectNode) line.get("schedule").get(0);
            target.put(field, "5");
            code(() -> CustomerOrderSemantics.snapshot(catalog(CustomerOrderSemantics.canonical(source)), benefits()), Code.VALIDATION_FAILED);
        }
        code(() -> CustomerOrderSemantics.snapshot(catalog(source("{}").replace("\"quantity\":100", "\"quantity\":99")), benefits()), Code.INTERNAL);
    }
    @Test void versionTokenAndOptionalExpectationAreComparedExactly() {
        var catalog = catalog(source("{}")); CustomerOrderSemantics.payable(catalog, command("key", "Exact"), caller());
        code(() -> CustomerOrderSemantics.payable(catalog, command("key", "exact"), caller()), Code.VALIDATION_FAILED);
    }
    @Test void responseProjectionAndOrderNumberAreDeterministic() {
        String snapshot = CustomerOrderSemantics.snapshot(catalog(source("{}")), benefits()), hash = CustomerOrderSemantics.sha(snapshot);
        var projection = CustomerOrderSemantics.projection(snapshot, hash); assertThat(projection.amountCents()).isEqualTo(123); assertThat(projection.snapshotSha256()).isEqualTo(hash);
        assertThat(CustomerOrderSemantics.orderNo(ACCOUNT)).isEqualTo("OC-10000000000000000000000000000001").hasSize(35);
    }
}
