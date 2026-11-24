package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerCatalogOffersFixture.*;

import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader;
import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader.*;
import com.onecall.core.catalog_order.internal.customercatalogquery.JooqCustomerCatalogOfferReader;
import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class CustomerCatalogOffersIT {
    private final CustomerCatalogOffersFixture fixture = new CustomerCatalogOffersFixture();
    private org.springframework.boot.test.system.CapturedOutput output;
    @BeforeAll void start() throws Exception { fixture.start(); fixture.web(); }
    @BeforeEach void seed(org.springframework.boot.test.system.CapturedOutput capturedOutput) throws Exception { output = capturedOutput; fixture.seed(); }
    @AfterAll void stop() { fixture.close(); }

    private List<UUID> walk(String token, int limit) throws Exception {
        List<UUID> result = new ArrayList<>(); String cursor = null;
        do {
            var page = fixture.json(token, "?limit=" + limit + (cursor == null ? "" : "&cursor=" + cursor), 200);
            result.addAll(fixture.ids(page));
            boolean more = page.path("hasMore").asBoolean();
            assertThat(page.path("nextCursor").isNull()).isEqualTo(!more);
            cursor = more ? page.path("nextCursor").asString() : null;
            assertThat(result.size()).isLessThan(1000);
        } while (cursor != null);
        assertThat(new HashSet<>(result)).hasSize(result.size());
        return result;
    }
    private TrustedCustomerCaller caller() { return new TrustedCustomerCaller(fixture.account, TrustedCustomerCaller.Channel.WEB, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)); }
    private CustomerCatalogOfferQueryService.Request request() { return new CustomerCatalogOfferQueryService.Request(Optional.empty(), Optional.of(100), Optional.empty(), Optional.empty(), Optional.empty()); }
    private DSLContext dsl() { return fixture.context.getBean("customerQueryDsl", DSLContext.class); }

    @Test
    void postgres16FullChainAndCustomerRuntimeRoleAreUsed() throws Exception {
        try (var connection = fixture.admin()) {
            assertThat(Integer.parseInt(one(connection, "SHOW server_version_num"))).isBetween(160000, 169999);
            assertThat(one(connection, "SELECT count(*) FROM public.flyway_schema_history WHERE success")).isEqualTo("19");
            assertThat(one(connection, "SELECT rolinherit FROM pg_roles WHERE rolname='pa27_customer'")).isEqualTo("f");
        }
        fixture.json("personal", "", 200); fixture.cleanPool();
        assertThat(fixture.psql("check").getExitCode()).isZero();
    }

    @Test
    void personalScopeReturnsOnlyActivePersonalChannelOffers() throws Exception {
        assertThat(walk("personal", 4)).containsExactlyElementsOf(fixture.expected("PERSONAL"));
        var page = fixture.json("personal", "?limit=100", 200);
        for (JsonNode item : page.path("items")) {
            assertThat(item.path("audience").asString()).isEqualTo("PERSONAL");
            assertThat(item.path("channel").asString()).isEqualTo("WEB");
            assertThat(item.path("offerType").asString()).isNotEqualTo("QUOTE");
        }
    }

    @Test
    void organizationScopeReturnsOnlyActiveEnterpriseQuoteOffers() throws Exception {
        assertThat(walk("enterprise", 3)).containsExactlyElementsOf(fixture.expected("ENTERPRISE"));
        var page = fixture.json("enterprise", "?limit=100", 200);
        for (JsonNode item : page.path("items")) {
            assertThat(item.path("offerType").asString()).isEqualTo("QUOTE");
            assertThat(item.path("audience").asString()).isEqualTo("ENTERPRISE");
            assertThat(item.path("priceCents").isNull()).isTrue();
        }
    }

    @Test
    void draftOffsaleWithdrawnWrongChannelAndWrongAudienceFactsNeverAppear() throws Exception {
        var personal = walk("personal", 100);
        var enterprise = walk("enterprise", 100);
        for (String code : List.of("JULIETT", "KILO", "LIMA", "MIKE", "QUEBEC")) {
            assertThat(personal).doesNotContain(fixture.seedByCode(code).offerId());
            assertThat(enterprise).doesNotContain(fixture.seedByCode(code).offerId());
        }
        assertThat(personal).doesNotContainAnyElementsOf(fixture.expected("ENTERPRISE"));
        assertThat(enterprise).doesNotContainAnyElementsOf(fixture.expected("PERSONAL"));
    }

    @Test
    void offerTypeNarrowingAndEmptyIntersectionAreExact() throws Exception {
        assertThat(fixture.ids(fixture.json("personal", "?offerType=SUBSCRIPTION&limit=100", 200)))
                .containsExactly(fixture.seedByCode("ALPHA").offerId(), fixture.seedByCode("DELTA").offerId());
        assertThat(fixture.ids(fixture.json("personal", "?offerType=ONE_TIME&limit=100", 200)))
                .containsExactly(fixture.seedByCode("BRAVO").offerId(), fixture.seedByCode("ECHO").offerId());
        assertThat(fixture.ids(fixture.json("personal", "?offerType=ADD_ON&limit=100", 200)))
                .containsExactly(fixture.seedByCode("CHARLIE").offerId(), fixture.seedByCode("FOXTROT").offerId());
        assertThat(fixture.ids(fixture.json("personal", "?offerType=QUOTE&limit=100", 200))).isEmpty();
        assertThat(fixture.ids(fixture.json("enterprise", "?offerType=SUBSCRIPTION&limit=100", 200))).isEmpty();
        assertThat(fixture.ids(fixture.json("enterprise", "?offerType=QUOTE&limit=100", 200)))
                .containsExactlyElementsOf(fixture.expected("ENTERPRISE"));
    }

    @Test
    void quotePriceIsNullAndCurrencyIsCnyAndTokenIsStored() throws Exception {
        var clean = fixture.json("enterprise", "?offerType=QUOTE&limit=100", 200);
        for (JsonNode item : clean.path("items")) {
            assertThat(item.path("priceCents").isNull()).isTrue();
            assertThat(item.path("currency").asString()).isEqualTo("CNY");
            assertThat(item.path("versionToken").asString()).startsWith("stored-");
        }
        UUID canaryProduct = UUID.randomUUID(); UUID canaryVersion = UUID.randomUUID(); UUID canaryOffer = UUID.randomUUID();
        try (var connection = fixture.admin()) {
            execute(connection, "SET ROLE catalog_order_owner");
            execute(connection, "INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (?,'ZZ_CANARY_NONCNY','STANDARD')", canaryProduct);
            execute(connection, "INSERT INTO catalog_order.catalog_version(id,product_id,version_no,sale_status,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,created_by_employee_id,published_at) VALUES (?,?,'1','ACTIVE','QUOTE','ENTERPRISE',NULL,'USD','QUOTE',1,0,'{}',repeat('a',64),?,'2026-11-23T00:00:00Z')", canaryVersion, canaryProduct, fixture.employee);
            execute(connection, "INSERT INTO catalog_order.catalog_offer(id,catalog_version_id,channel,offer_status,version_token) VALUES (?,?,'WEB','ACTIVE','canary-token')", canaryOffer, canaryVersion);
        }
        String baseline = fixture.businessSnapshot();
        try {
            assertThat(fixture.json("enterprise", "?offerType=QUOTE&limit=100", 500).path("code").asString()).isEqualTo("INTERNAL");
            assertThat(fixture.businessSnapshot()).isEqualTo(baseline);
        } finally {
            try (var connection = fixture.admin()) {
                execute(connection, "ALTER TABLE catalog_order.catalog_product DISABLE TRIGGER trg_catalog_product_immutable");
                try {
                    execute(connection, "DELETE FROM catalog_order.catalog_offer WHERE id=?", canaryOffer);
                    execute(connection, "DELETE FROM catalog_order.catalog_version WHERE id=?", canaryVersion);
                    execute(connection, "DELETE FROM catalog_order.catalog_product WHERE id=?", canaryProduct);
                } finally {
                    execute(connection, "ALTER TABLE catalog_order.catalog_product ENABLE TRIGGER trg_catalog_product_immutable");
                }
            }
        }
        fixture.cleanPool();
    }

    @Test
    void benefitLinesAreCanonicalAndEmptyWhenAbsent() throws Exception {
        var page = fixture.json("personal", "?limit=100", 200);
        for (JsonNode item : page.path("items")) {
            String code = item.path("productCode").asString();
            var lines = item.path("benefitLines");
            List<String> types = new ArrayList<>();
            for (JsonNode line : lines) {
                assertThat(line.propertyNames()).containsExactly("entitlementType", "quantity", "validityDays", "grantIntervalMonths");
                types.add(line.path("entitlementType").asString());
            }
            assertThat(types).isSortedAccordingTo(String::compareTo);
            if (code.equals("BRAVO") || code.equals("FOXTROT")) assertThat(lines).isEmpty();
            if (code.equals("ALPHA")) assertThat(types).containsExactly("AI_SECONDS", "EVIDENCE_COUNT");
        }
    }

    @Test
    void productCodeAscVersionNoDescCatalogOfferIdAscTiesAreExact() throws Exception {
        var page = fixture.json("personal", "?limit=100", 200);
        assertThat(fixture.ids(page)).containsExactlyElementsOf(fixture.expected("PERSONAL"));
        List<String> codes = new ArrayList<>();
        for (JsonNode item : page.path("items")) codes.add(item.path("productCode").asString());
        assertThat(codes).isSortedAccordingTo(String::compareTo);
        assertThat(page.path("items").get(0).path("versionNo").asInt()).isEqualTo(2);
        assertThat(page.path("items").get(2).path("versionNo").asInt()).isEqualTo(3);
        String cursor = fixture.json("personal", "?limit=2", 200).path("nextCursor").asString();
        var continuation = fixture.json("personal", "?limit=100&cursor=" + cursor, 200);
        assertThat(fixture.ids(continuation)).containsExactlyElementsOf(fixture.expected("PERSONAL").subList(2, 6));
    }

    @Test
    void emptySingleAndExactLimitPagesHaveExactTailSemantics() throws Exception {
        var empty = fixture.json("personal", "?offerType=QUOTE", 200);
        assertThat(empty.path("items")).isEmpty();
        assertThat(empty.path("nextCursor").isNull()).isTrue();
        assertThat(empty.path("hasMore").asBoolean()).isFalse();
        var single = fixture.json("personal", "?limit=100", 200);
        assertThat(fixture.ids(single)).hasSize(6);
        assertThat(single.path("hasMore").asBoolean()).isFalse();
        assertThat(single.path("nextCursor").isNull()).isTrue();
        var exact = fixture.json("personal", "?limit=6", 200);
        assertThat(fixture.ids(exact)).hasSize(6);
        assertThat(exact.path("hasMore").asBoolean()).isFalse();
        var over = fixture.json("personal", "?limit=5", 200);
        assertThat(fixture.ids(over)).hasSize(5);
        assertThat(over.path("hasMore").asBoolean()).isTrue();
    }

    @Test
    void firstMiddleAndLastPagesEnumerateEveryOfferOnce() throws Exception {
        var expected = fixture.expected("PERSONAL");
        var first = fixture.json("personal", "?limit=2", 200);
        assertThat(fixture.ids(first)).containsExactlyElementsOf(expected.subList(0, 2));
        var middle = fixture.json("personal", "?limit=2&cursor=" + first.path("nextCursor").asString(), 200);
        assertThat(fixture.ids(middle)).containsExactlyElementsOf(expected.subList(2, 4));
        var last = fixture.json("personal", "?limit=2&cursor=" + middle.path("nextCursor").asString(), 200);
        assertThat(fixture.ids(last)).containsExactlyElementsOf(expected.subList(4, 6));
        assertThat(last.path("hasMore").asBoolean()).isFalse();
        assertThat(last.path("nextCursor").isNull()).isTrue();
        assertThat(walk("personal", 2)).containsExactlyElementsOf(expected);
    }

    @Test
    void defaultOneAndHundredLimitsAndChangedContinuationLimitAreExact() throws Exception {
        assertThat(fixture.ids(fixture.json("personal", "", 200))).containsExactlyElementsOf(fixture.expected("PERSONAL"));
        var one = fixture.json("personal", "?limit=1", 200);
        assertThat(fixture.ids(one)).hasSize(1);
        assertThat(one.path("hasMore").asBoolean()).isTrue();
        assertThat(fixture.ids(fixture.json("personal", "?limit=100", 200))).hasSize(6);
        var continuation = fixture.json("personal", "?limit=100&cursor=" + one.path("nextCursor").asString(), 200);
        assertThat(fixture.ids(continuation)).containsExactlyElementsOf(fixture.expected("PERSONAL").subList(1, 6));
        assertThat(continuation.path("hasMore").asBoolean()).isFalse();
    }

    @Test
    void malformedTamperedAndCrossScopeCursorsCannotWidenResults() throws Exception {
        String cursor = fixture.json("personal", "?limit=1", 200).path("nextCursor").asString();
        byte[] bytes = Base64.getUrlDecoder().decode(cursor); bytes[bytes.length - 1] ^= 1;
        for (String value : List.of("bad", "", cursor + "&cursor=" + cursor, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)))
            assertThat(fixture.json("personal", "?cursor=" + value, 400).path("code").asString()).isEqualTo("CURSOR_INVALID");
        fixture.json("enterprise", "?cursor=" + cursor, 400);
        fixture.json("other", "?cursor=" + cursor, 400);
    }

    @Test
    void normalizedFilterChangeIsCursorFilterMismatch() throws Exception {
        String cursor = fixture.json("personal", "?limit=1", 200).path("nextCursor").asString();
        assertThat(fixture.json("personal", "?limit=1&offerType=SUBSCRIPTION&cursor=" + cursor, 400).path("code").asString()).isEqualTo("CURSOR_FILTER_MISMATCH");
        String narrowed = fixture.json("personal", "?limit=1&offerType=SUBSCRIPTION", 200).path("nextCursor").asString();
        assertThat(fixture.json("personal", "?limit=1&cursor=" + narrowed, 400).path("code").asString()).isEqualTo("CURSOR_FILTER_MISMATCH");
    }

    @Test
    void committedLifecycleChangesAreRecheckedForEveryPage() throws Exception {
        String cursor = fixture.json("personal", "?limit=1", 200).path("nextCursor").asString();
        try (var connection = fixture.admin()) {
            execute(connection, "SET ROLE customer_iam_owner");
            execute(connection, "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account);
        }
        assertThat(fixture.json("personal", "?cursor=" + cursor, 403).path("code").asString()).isEqualTo("FORBIDDEN");
    }

    @Test
    void lifecyclePageAndBenefitsShareOneRepeatableReadSnapshot() throws Exception {
        var initial = fixture.json("personal", "?limit=100", 200);
        UUID targetVersion = null; long original = 0; String targetType = null;
        for (JsonNode item : initial.path("items")) {
            if (item.path("benefitLines").size() > 0) {
                targetVersion = UUID.fromString(item.path("catalogVersionId").asString());
                var line = item.path("benefitLines").get(0);
                original = line.path("quantity").asLong();
                targetType = line.path("entitlementType").asString();
                break;
            }
        }
        assertThat(targetVersion).isNotNull();
        var entered = new CountDownLatch(1); var resume = new CountDownLatch(1);
        var facts = new CopyOnWriteArrayList<String>();
        Runnable capture = () -> facts.add(dsl().fetchOne("SELECT current_user||'|'||current_setting('transaction_read_only')||'|'||current_setting('transaction_isolation')||'|'||pg_backend_pid()||'|'||pg_current_snapshot()").get(0, String.class));
        var real = new JooqCustomerCatalogOfferReader(dsl());
        CustomerCatalogOfferReader delegating = new CustomerCatalogOfferReader() {
            @Override public List<OfferRow> fetchOffers(Scope scope, Optional<Anchor> anchor, int fetchSize) { capture.run(); return real.fetchOffers(scope, anchor, fetchSize); }
            @Override public Map<UUID, List<BenefitRow>> fetchBenefits(Set<UUID> ids) {
                capture.run(); entered.countDown();
                try { if (!resume.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out"); }
                catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(); }
                return real.fetchBenefits(ids);
            }
        };
        var service = new CustomerCatalogOfferQueryService(fixture.context.getBean("customerCatalogOfferTransactionTemplate", TransactionTemplate.class), dsl(),
                fixture.context.getBean("customerBusinessNumberAccessRepository", JooqCustomerBusinessNumberAccessRepository.class), delegating);
        UUID mutationVersion = targetVersion; String mutationType = targetType;
        long expectedQuantity = original;
        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(() -> service.listCustomerCatalogOffers(caller(), request()));
            try {
                assertThat(entered.await(30, TimeUnit.SECONDS)).isTrue();
                try (var connection = fixture.admin()) {
                    execute(connection, "ALTER TABLE catalog_order.catalog_benefit_line DISABLE TRIGGER trg_catalog_benefit_line_immutable");
                    try {
                        execute(connection, "SET ROLE catalog_order_owner");
                        execute(connection, "UPDATE catalog_order.catalog_benefit_line SET quantity=quantity+1 WHERE catalog_version_id=? AND entitlement_type=?", mutationVersion, mutationType);
                    } finally {
                        execute(connection, "RESET ROLE");
                        execute(connection, "ALTER TABLE catalog_order.catalog_benefit_line ENABLE TRIGGER trg_catalog_benefit_line_immutable");
                    }
                }
            } finally { resume.countDown(); }
            var page = future.get(30, TimeUnit.SECONDS);
            for (var item : page.items()) {
                if (item.catalogVersionId().equals(mutationVersion)) {
                    assertThat(item.benefitLines()).anyMatch(line -> line.entitlementType().equals(mutationType) && line.quantity() == expectedQuantity);
                }
            }
        }
        assertThat(facts).hasSize(2).allMatch(value -> value.startsWith("customer_iam_role|on|repeatable read|"));
        assertThat(new HashSet<>(facts)).hasSize(1);
        var fresh = fixture.json("personal", "?limit=100", 200);
        boolean sawMutation = false;
        for (JsonNode item : fresh.path("items")) {
            if (UUID.fromString(item.path("catalogVersionId").asString()).equals(mutationVersion)) {
                for (JsonNode line : item.path("benefitLines"))
                    if (line.path("entitlementType").asString().equals(mutationType) && line.path("quantity").asLong() == original + 1) sawMutation = true;
            }
        }
        assertThat(sawMutation).isTrue();
        fixture.cleanPool();
    }

    @Test
    void concurrentInsertAndDeletedAnchorFollowDocumentedKeysetSemantics() throws Exception {
        List<UUID> before = fixture.expected("PERSONAL");
        var first = fixture.json("personal", "?limit=1", 200);
        UUID anchor = fixture.ids(first).getFirst(); String cursor = first.path("nextCursor").asString();
        try (var connection = fixture.admin()) {
            execute(connection, "SET ROLE catalog_order_owner");
            execute(connection, "DELETE FROM catalog_order.catalog_offer WHERE id=?", anchor);
        }
        fixture.insert("AAA", 1, "ACTIVE", "SUBSCRIPTION", "PERSONAL", "WEB", "ACTIVE", 100L, "CNY", "MONTHLY", 1, List.of());
        fixture.insert("ZZZ", 1, "ACTIVE", "SUBSCRIPTION", "PERSONAL", "WEB", "ACTIVE", 100L, "CNY", "MONTHLY", 1, List.of());
        var continuation = fixture.ids(fixture.json("personal", "?limit=100&cursor=" + cursor, 200));
        assertThat(continuation).doesNotContain(anchor, fixture.seedByCode("AAA").offerId()).contains(fixture.seedByCode("ZZZ").offerId());
        List<UUID> expectedContinuation = new ArrayList<>(before);
        expectedContinuation.remove(anchor);
        expectedContinuation.add(fixture.seedByCode("ZZZ").offerId());
        assertThat(continuation).containsExactlyElementsOf(expectedContinuation);
    }

    @Test
    void successDenialAndFailurePerformZeroBusinessWrites() throws Exception {
        String before = fixture.businessSnapshot();
        fixture.json("personal", "", 200);
        fixture.session("missing", UUID.randomUUID(), null);
        assertThat(fixture.json("missing", "", 403).path("code").asString()).isEqualTo("FORBIDDEN");
        fixture.json("personal", "?cursor=bad", 400);
        var service = new CustomerCatalogOfferQueryService(fixture.context.getBean("customerCatalogOfferTransactionTemplate", TransactionTemplate.class), dsl(),
                fixture.context.getBean("customerBusinessNumberAccessRepository", JooqCustomerBusinessNumberAccessRepository.class),
                new CustomerCatalogOfferReader() {
                    @Override public List<OfferRow> fetchOffers(Scope scope, Optional<Anchor> anchor, int fetchSize) { throw new org.jooq.exception.DataAccessException("DEPENDENCY_CANARY"); }
                    @Override public Map<UUID, List<BenefitRow>> fetchBenefits(Set<UUID> ids) { return Map.of(); }
                });
        assertThatThrownBy(() -> service.listCustomerCatalogOffers(caller(), request())).isInstanceOf(CustomerContextException.class);
        assertThat(fixture.businessSnapshot()).isEqualTo(before);
        fixture.cleanPool();
    }

    @Test
    void roleIsolationReadOnlyAndAutoCommitRestoreOnPooledConnectionReuse() throws Exception {
        for (int index = 0; index < 3; index++) {
            fixture.json("personal", "", 200); fixture.cleanPool();
            fixture.session("missing", UUID.randomUUID(), null);
            fixture.json("missing", "", 403); fixture.cleanPool();
        }
        var service = new CustomerCatalogOfferQueryService(fixture.context.getBean("customerCatalogOfferTransactionTemplate", TransactionTemplate.class), dsl(),
                fixture.context.getBean("customerBusinessNumberAccessRepository", JooqCustomerBusinessNumberAccessRepository.class),
                new CustomerCatalogOfferReader() {
                    @Override public List<OfferRow> fetchOffers(Scope scope, Optional<Anchor> anchor, int fetchSize) { throw new IllegalStateException("FAILURE_CANARY"); }
                    @Override public Map<UUID, List<BenefitRow>> fetchBenefits(Set<UUID> ids) { return Map.of(); }
                });
        assertThatThrownBy(() -> service.listCustomerCatalogOffers(caller(), request())).isInstanceOf(CustomerContextException.class);
        fixture.cleanPool(); fixture.json("personal", "", 200); fixture.cleanPool();
    }

    @Test
    void responsesAndCapturedLogsNeverContainSecretOrOwnershipSentinels() throws Exception {
        for (String token : List.of("personal", "enterprise")) {
            var response = fixture.get(token, "?limit=100"); assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).doesNotContain(fixture.password, "CANARY", fixture.account.toString(), fixture.other.toString(),
                    "created_by_employee_id", "product_family", "snapshot_sha256", "published_at");
        }
        String web = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/application-web.yml"));
        assertThat(web).contains("org.jooq: 'OFF'", "com.zaxxer.hikari: 'OFF'", "org.springframework.jdbc: 'OFF'", "log-request-details: false");
        assertThat(output.getAll()).doesNotContain(fixture.password, "CANARY", fixture.account.toString(), fixture.other.toString());
    }
}
