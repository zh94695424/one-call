package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.JsonNode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerOrdersIT {
    CustomerOrdersFixture fixture;
    @BeforeAll void start() throws Exception { fixture = new CustomerOrdersFixture(); fixture.start(); }
    @AfterAll void stop() { if (fixture != null) fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    @Test void personalSubscriptionCheckoutCreatesExactUnpaidOrder() throws Exception {
        fixture.seedCatalog("SUBSCRIPTION", "PERSONAL", 9900L, "WEB", "ACTIVE", "ACTIVE", "{\"approved\":true}");
        var before = fixture.rows(); var result = fixture.success(fixture.post("first"), 201); fixture.bundle(before);
        assertThat(result.get("amountCents").asLong()).isEqualTo(9900);
        String stored = fixture.value("SELECT snapshot::text FROM catalog_order.customer_order");
        assertThat(CustomerOrdersFixture.sha(CustomerOrdersFixture.canonical(fixture.mapper.readTree(stored)))).isEqualTo(result.get("snapshotSha256").asString());
        assertThat(fixture.value("SELECT result_sha256 FROM platform.idempotency_record")).isEqualTo(result.get("snapshotSha256").asString());
        assertThat(fixture.mapper.readTree(stored).propertyNames()).contains("terms", "versionNo", "benefitLines");
        assertThat(result.get("snapshot").propertyNames()).doesNotContain("terms", "versionNo");
        assertThat(result.get("orderNo").asString()).isEqualTo("OC-" + result.get("orderId").asString().replace("-", ""));
        fixture.clean();
    }
    @Test void personalOneTimeAndAddOnCheckoutUseServerPrices() throws Exception {
        for (String type : List.of("ONE_TIME", "ADD_ON")) for (long price : List.of(0L, 456L)) {
            fixture.reset(); fixture.seedCatalog(type, "PERSONAL", price, "WEB", "ACTIVE", "ACTIVE", "{}");
            var before = fixture.rows(); assertThat(fixture.success(fixture.post("price"), 201).get("amountCents").asLong()).isEqualTo(price); fixture.bundle(before);
        }
    }
    @Test void miniappAndWebUseVerifierAttestedChannels() throws Exception {
        for (String channel : List.of("WEB", "MINIAPP")) {
            fixture.reset(); fixture.seedCatalog("ONE_TIME", "PERSONAL", 123L, channel, "ACTIVE", "ACTIVE", "{}");
            var before = fixture.rows();
            fixture.problem(fixture.post(channel.toLowerCase(Locale.ROOT), "forged", fixture.body(fixture.subject, fixture.offer, channel.equals("WEB") ? "MINIAPP" : "WEB")), 422, "VALIDATION_FAILED");
            assertThat(fixture.rows()).isEqualTo(before);
            assertThat(fixture.success(fixture.post(channel.toLowerCase(Locale.ROOT), "channel", fixture.body(fixture.subject, fixture.offer, channel)), 201).get("channel").asString()).isEqualTo(channel); fixture.bundle(before);
        }
    }
    @Test void identicalRetryReturnsFirstImmutableOrderWith200() throws Exception {
        var result = fixture.success(fixture.post("replay"), 201); var before = fixture.rows();
        assertThat(fixture.success(fixture.post("replay"), 200)).isEqualTo(result); assertThat(fixture.rows()).isEqualTo(before);
    }
    @Test void changedPurchaseSemanticsConflictWithZeroDelta() throws Exception {
        fixture.success(fixture.post("conflict"), 201); var before = fixture.rows();
        fixture.problem(fixture.post("web", "conflict", fixture.body(fixture.secondSubject, fixture.offer, "WEB")), 409, "IDEMPOTENCY_CONFLICT");
        assertThat(fixture.rows()).isEqualTo(before);
        fixture.success(fixture.post("web", "conflict", fixture.body().replace("}", ",\"expectedVersionToken\":\"Pa41-Exact\"}")), 200);
        assertThat(fixture.rows()).isEqualTo(before);
    }
    @Test void changedConsentPolicyPromptOrContentConflictsWithZeroDelta() throws Exception {
        fixture.success(fixture.post("policy"), 201); var before = fixture.rows();
        for (String body : List.of(fixture.body().replace("SYNTHETIC_PURCHASE", "OTHER"), fixture.body().replace("\"policyVersion\":\"1\"", "\"policyVersion\":\"2\""), fixture.body().replace("\"promptVersion\":\"1\"", "\"promptVersion\":\"2\""), fixture.body().replace(CustomerOrdersFixture.CONTENT, "b".repeat(64)))) {
            fixture.problem(fixture.post("web", "policy", body), 409, "IDEMPOTENCY_CONFLICT"); assertThat(fixture.rows()).isEqualTo(before);
        }
    }
    private List<HttpResponse<String>> race(boolean changed) throws Exception {
        var before = fixture.rows();
        CountDownLatch winnerReady = new CountDownLatch(1), loserReady = new CountDownLatch(1), releaseWinner = new CountDownLatch(1), releaseLoser = new CountDownLatch(1);
        AtomicLong winnerThread = new AtomicLong();
        fixture.before = execution -> {
            if (execution.sql().startsWith("SELECT pg_advisory_xact_lock")) {
                winnerThread.compareAndSet(0, Thread.currentThread().threadId());
                if (winnerThread.get() != Thread.currentThread().threadId()) { loserReady.countDown(); await(releaseLoser); }
            }
        };
        fixture.after = execution -> {
            if (execution.sql().startsWith("INSERT INTO catalog_order.customer_order") && winnerThread.get() == Thread.currentThread().threadId()) { winnerReady.countDown(); await(releaseWinner); }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var winner = executor.submit(() -> fixture.post("race")); assertThat(winnerReady.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(fixture.value("SELECT count(*) FROM pg_locks WHERE locktype='advisory' AND granted AND pid IN (SELECT pid FROM pg_stat_activity WHERE usename='pa36_operations')")).isNotEqualTo("0");
            var loser = executor.submit(() -> fixture.post("web", "race", fixture.body(changed ? fixture.secondSubject : fixture.subject, fixture.offer, "WEB")));
            assertThat(loserReady.await(3, TimeUnit.SECONDS)).isTrue(); releaseWinner.countDown();
            var won = winner.get(3, TimeUnit.SECONDS); fixture.success(won, 201);
            releaseLoser.countDown(); var lost = loser.get(3, TimeUnit.SECONDS);
            if (changed) fixture.problem(lost, 409, "IDEMPOTENCY_CONFLICT"); else assertThat(fixture.success(lost, 200)).isEqualTo(fixture.mapper.readTree(won.body()));
            fixture.bundle(before); return List.of(won, lost);
        } finally { releaseWinner.countDown(); releaseLoser.countDown(); fixture.before = null; fixture.after = null; }
    }
    static void await(CountDownLatch latch) {
        try { if (!latch.await(3, TimeUnit.SECONDS)) throw new AssertionError("PA41 deterministic barrier exceeded"); }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
    }
    private void timeout(String key, String body) throws Exception {
        var before = fixture.rows();
        try (var holder = fixture.admin()) {
            holder.setAutoCommit(false);
            execute(holder, "SELECT pg_advisory_xact_lock(hashtextextended(?,0))", key);
            fixture.problem(fixture.post("web", "timeout", body), 503, "SERVICE_UNAVAILABLE");
            assertThat(fixture.rows()).isEqualTo(before); holder.rollback();
        }
        fixture.clean();
    }
    @Test void concurrentSameKeyHasOneCreateAndOneReplay() throws Exception {
        race(false); var before = fixture.rows(); timeout("subject_number.subject:" + fixture.subject, fixture.body());
        fixture.success(fixture.post("race"), 200); assertThat(fixture.rows()).isEqualTo(before);
    }
    @Test void concurrentChangedKeyPayloadHasOneWinnerAndConflict() throws Exception {
        race(true); timeout("customer_iam.account:" + fixture.account, fixture.body(fixture.secondSubject, fixture.offer, "WEB"));
        var before = fixture.rows(); fixture.problem(fixture.post("web", "race", fixture.body(fixture.secondSubject, fixture.offer, "WEB")), 409, "IDEMPOTENCY_CONFLICT"); assertThat(fixture.rows()).isEqualTo(before);
    }
    @Test void unknownOrderBlocksNewCheckoutButNotCreationReplay() throws Exception {
        var created = fixture.success(fixture.post("original"), 201);
        fixture.sql("UPDATE catalog_order.customer_order SET payment_status='UNKNOWN',revision=revision+1 WHERE id=?::uuid", created.get("orderId").asString());
        var before = fixture.rows(); fixture.problem(fixture.post("new"), 409, "ORDER_UNKNOWN_PAYMENT_PENDING");
        assertThat(fixture.success(fixture.post("original"), 200)).isEqualTo(created); assertThat(fixture.rows()).isEqualTo(before);
    }
    @Test void purchaseConsentIdentityAndLongKeysMatchCanonicalBytes() throws Exception {
        for (int length : List.of(74, 75, 128, 475)) {
            fixture.reset(); String key = "k".repeat(length); fixture.success(fixture.post(key), 201);
            assertThat(fixture.value("SELECT idempotency_key FROM catalog_order.customer_order")).isEqualTo(key);
            assertThat(fixture.value("SELECT idempotency_key FROM platform.idempotency_record")).isEqualTo(fixture.account + ":" + key);
            String bytes = "{\"accountId\":\"" + fixture.account + "\",\"clientIdempotencyKey\":\"" + key + "\"}";
            assertThat(fixture.value("SELECT event_id FROM activation_consent.consent_record")).isEqualTo("purchase-consent:" + CustomerOrdersFixture.sha(bytes)).hasSize(81);
        }
        fixture.reset(); AtomicReference<String> received = new AtomicReference<>();
        fixture.before = execution -> {
            if (execution.sql().contains("idempotency_begin")) received.set(((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest().getHeader("Idempotency-Key"));
        };
        var original = fixture.success(fixture.post(" left,right "), 201); fixture.before = null;
        assertThat(received.get()).isEqualTo("left,right");
        assertThat(fixture.value("SELECT idempotency_key FROM catalog_order.customer_order")).isEqualTo(received.get());
        assertThat(fixture.value("SELECT idempotency_key FROM platform.idempotency_record")).isEqualTo(fixture.account + ":" + received.get());
        var unchanged = fixture.rows(); assertThat(fixture.success(fixture.post(" left,right "), 200)).isEqualTo(original); assertThat(fixture.rows()).isEqualTo(unchanged);
    }
    @Test void invalidKeysAndForgedBodiesNeverWrite() throws Exception {
        var before = fixture.rows();
        for (String key : List.of(" ", "x".repeat(476), "x".repeat(512))) fixture.problem(fixture.post(key), 422, "VALIDATION_FAILED");
        fixture.problem(fixture.post(null), 422, "VALIDATION_FAILED");
        for (String key : List.of("amountCents", "currency", "paymentStatus", "snapshot", "accountId", "catalogVersionId", "purchaseConsentId", "eventId")) fixture.problem(fixture.post("web", "body", fixture.body().replace("{", "{\"" + key + "\":\"forged\",")), 422, "VALIDATION_FAILED");
        assertThat(fixture.rows()).isEqualTo(before);
    }
    @Test void ordinaryClientRequiresCurrentVerifiedRealName() throws Exception {
        fixture.sql("DELETE FROM customer_iam.identity_profile"); var before = fixture.rows();
        fixture.problem(fixture.post("real"), 422, "REAL_NAME_REQUIRED"); assertThat(fixture.rows()).isEqualTo(before);
        fixture.sql("INSERT INTO customer_iam.identity_profile(id,account_id) VALUES (?,?)", UUID.randomUUID(), fixture.account);
        before = fixture.rows(); fixture.problem(fixture.post("real"), 422, "REAL_NAME_REQUIRED"); assertThat(fixture.rows()).isEqualTo(before);
    }
    @Test void inactiveMissingOrSuspendedActivationDeniesBeforeReplay() throws Exception {
        fixture.success(fixture.post("active"), 201);
        for (String status : List.of("INACTIVE", "SUSPENDED")) {
            fixture.sql("UPDATE activation_consent.service_activation SET status=?,revision=revision+1 WHERE subject_id=?", status, fixture.subject);
            var before = fixture.rows(); fixture.problem(fixture.post("active"), 403, "ACTIVATION_NOT_ACTIVE"); assertThat(fixture.rows()).isEqualTo(before);
        }
    }
    @Test void currentAccountWorkspaceMembershipAndOwnershipAreReread() throws Exception {
        fixture.sql("UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account);
        var before = fixture.rows(); fixture.problem(fixture.post("authority"), 403, "FORBIDDEN"); assertThat(fixture.rows()).isEqualTo(before);
        fixture.reset(); before = fixture.rows(); fixture.problem(fixture.post("web", "missing", fixture.body(UUID.randomUUID(), fixture.offer, "WEB")), 403, "FORBIDDEN"); assertThat(fixture.rows()).isEqualTo(before);
        fixture.session("web", "WEB", UUID.randomUUID()); before = fixture.rows(); fixture.problem(fixture.post("tenant"), 403, "FORBIDDEN"); assertThat(fixture.rows()).isEqualTo(before);
        for (String role : List.of("ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR")) {
            fixture.reset(); UUID organization = fixture.organization(role); fixture.sql("DELETE FROM customer_iam.identity_profile");
            before = fixture.rows(); fixture.success(fixture.post("organization"), 201); fixture.bundle(before);
            assertThat(fixture.value("SELECT owner_organization_id::text FROM catalog_order.customer_order")).isEqualTo(organization.toString());
            fixture.sql("UPDATE organization_access.organization_membership SET status='REVOKED',revision=revision+1"); before = fixture.rows();
            fixture.problem(fixture.post("organization"), 403, "FORBIDDEN"); assertThat(fixture.rows()).isEqualTo(before);
        }
    }
    @Test void enterpriseQuoteCheckoutFailsClosedWithZeroDelta() throws Exception {
        for (String channel : List.of("WEB", "MINIAPP")) for (String workspace : List.of("PERSONAL", "ORGANIZATION")) {
            fixture.reset(); if (workspace.equals("ORGANIZATION")) fixture.organization("MEMBER");
            fixture.seedCatalog("QUOTE", "ENTERPRISE", null, channel, "ACTIVE", "ACTIVE", "{}"); var before = fixture.rows();
            fixture.problem(fixture.post(channel.toLowerCase(Locale.ROOT), "quote", fixture.body(fixture.subject, fixture.offer, channel)), 422, "VALIDATION_FAILED"); assertThat(fixture.rows()).isEqualTo(before);
        }
    }
    @Test void inactiveAndWrongChannelCatalogOffersFailClosed() throws Exception {
        for (String status : List.of("DRAFT", "OFFSALE")) {
            fixture.reset(); fixture.seedCatalog("ONE_TIME", "PERSONAL", 123L, "WEB", status, "ACTIVE", "{}"); var before = fixture.rows();
            fixture.problem(fixture.post("inactive"), 422, "VALIDATION_FAILED"); assertThat(fixture.rows()).isEqualTo(before);
        }
        fixture.reset(); fixture.sql("UPDATE catalog_order.catalog_offer SET offer_status='WITHDRAWN',revision=revision+1"); var before = fixture.rows(); fixture.problem(fixture.post("withdrawn"), 422, "VALIDATION_FAILED"); assertThat(fixture.rows()).isEqualTo(before);
        fixture.reset(); fixture.seedCatalog("ONE_TIME", "PERSONAL", 123L, "MINIAPP", "ACTIVE", "ACTIVE", "{}"); before = fixture.rows(); fixture.problem(fixture.post("wrong"), 422, "VALIDATION_FAILED"); assertThat(fixture.rows()).isEqualTo(before);
    }
    @Test void staleVersionAndNullOrNonpayableFactsCannotCheckout() throws Exception {
        var before = fixture.rows(); fixture.problem(fixture.post("web", "stale", fixture.body().replace("}", ",\"expectedVersionToken\":\"pa41-exact\"}")), 422, "VALIDATION_FAILED"); assertThat(fixture.rows()).isEqualTo(before);
        fixture.seedCatalog("QUOTE", "ENTERPRISE", null, "WEB", "ACTIVE", "ACTIVE", "{}"); before = fixture.rows(); fixture.problem(fixture.post("quote"), 422, "VALIDATION_FAILED"); assertThat(fixture.rows()).isEqualTo(before);
    }
    @Test void completeApprovedScheduleIsFrozenAndNeverComputed() throws Exception {
        fixture.success(fixture.post("schedule"), 201);
        JsonNode stored = fixture.mapper.readTree(fixture.value("SELECT snapshot::text FROM catalog_order.customer_order"));
        assertThat(stored.get("benefitLines").get(0).get("schedule").get(0).get("validFrom").asString()).isEqualTo("2027-01-01T00:00:00Z");
        fixture.reset(); fixture.seedCatalog("SUBSCRIPTION", "PERSONAL", 9900L, "WEB", "ACTIVE", "ACTIVE", "{\"approved\":true}");
        var custom = (tools.jackson.databind.node.ObjectNode) fixture.mapper.readTree(fixture.value("SELECT snapshot::text FROM catalog_order.catalog_version")); custom.put("installments", 3);
        var customLine = (tools.jackson.databind.node.ObjectNode) custom.get("benefitLines").get(0); customLine.put("grantIntervalMonths", 2);
        var schedule = fixture.mapper.createArrayNode();
        List<String> starts = List.of("2027-01-03T04:05:06Z", "2027-06-17T08:09:10Z", "2027-12-02T12:13:14Z");
        List<String> ends = List.of("2027-02-12T04:05:06Z", "2027-08-03T08:09:10Z", "2028-03-11T12:13:14Z");
        for (int index = 0; index < 3; index++) schedule.add(fixture.mapper.createObjectNode().put("installmentNo", index + 1).put("entitlementType", "AI_SECONDS").put("units", 100).put("validFrom", starts.get(index)).put("expiresAt", ends.get(index)));
        customLine.set("schedule", schedule);
        UUID secondLineId = UUID.randomUUID();
        var secondLine = fixture.mapper.createObjectNode().put("lineKey", secondLineId.toString()).put("entitlementType", "STORAGE_BYTES").put("quantity", 50).put("validityDays", 60).put("grantIntervalMonths", 2);
        var secondSchedule = fixture.mapper.createArrayNode();
        for (int index = 0; index < 3; index++) secondSchedule.add(fixture.mapper.createObjectNode().put("installmentNo", index + 1).put("entitlementType", "STORAGE_BYTES").put("units", 50).put("validFrom", starts.get(index)).put("expiresAt", ends.get(index)));
        secondLine.set("schedule", secondSchedule); custom.withArray("benefitLines").insert(0, secondLine);
        fixture.seedCatalog("SUBSCRIPTION", "PERSONAL", 9900L, "WEB", "ACTIVE", "ACTIVE", "{}", CustomerOrdersFixture.canonical(custom));
        var customBefore = fixture.rows(); fixture.success(fixture.post("custom-schedule"), 201); fixture.bundle(customBefore);
        var frozen = fixture.mapper.readTree(fixture.value("SELECT snapshot::text FROM catalog_order.customer_order"));
        assertThat(frozen.get("installments").asInt()).isEqualTo(3); assertThat(frozen.get("benefitLines").get(0)).isEqualTo(customLine); assertThat(frozen.get("benefitLines").get(1)).isEqualTo(secondLine);
        List<String> unsupported = new ArrayList<>(List.of("null", "[]", "{\"text\":\"" + "PRIVATE_VALUE".repeat(100) + "\"}", "{\"decimal\":1.5}", "{\"unsafe\":9007199254740992}", "{\"a\":".repeat(8) + "true" + "}".repeat(8), "{\"nodes\":[" + String.join(",", Collections.nCopies(255, "true")) + "]}"));
        var bytes = fixture.mapper.createObjectNode(); for (int index = 0; index < 8; index++) bytes.put(Character.toString('a' + index), "x".repeat(index < 7 ? 1024 : 968)); unsupported.add(CustomerOrdersFixture.canonical(bytes));
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("com.onecall.core.catalog_order.internal.customerorder.CustomerOrderSemantics");
        var captured = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>(); captured.start(); logger.addAppender(captured);
        try {
            for (String terms : unsupported) {
                fixture.reset(); fixture.seedCatalog("ONE_TIME", "PERSONAL", 123L, "WEB", "ACTIVE", "ACTIVE", terms); var before = fixture.rows();
                var response = fixture.post("invalid-terms"); fixture.problem(response, 422, "VALIDATION_FAILED"); assertThat(response.body()).doesNotContain("TERMS_", "PRIVATE_VALUE"); assertThat(fixture.rows()).isEqualTo(before);
            }
            assertThat(captured.list).extracting(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage).anyMatch(message -> message.contains("TERMS_DEPTH_LIMIT")).anyMatch(message -> message.contains("TERMS_NODE_LIMIT")).anyMatch(message -> message.contains("TERMS_TEXT_LIMIT")).anyMatch(message -> message.contains("TERMS_UTF8_LIMIT")).allSatisfy(message -> assertThat(message).doesNotContain("PRIVATE_VALUE", "invalid-terms"));
        } finally { logger.detachAppender(captured); captured.stop(); }
        fixture.reset(); fixture.seedCatalog("ONE_TIME", "PERSONAL", 123L, "WEB", "ACTIVE", "ACTIVE", "{}", "{}");
        var legacy = fixture.rows(); fixture.problem(fixture.post("legacy"), 422, "VALIDATION_FAILED"); assertThat(fixture.rows()).isEqualTo(legacy);
    }
    @Test void missingStaleDeclinedOrMismatchedConsentRollsBack() throws Exception {
        var before = fixture.rows(); fixture.problem(fixture.post("web", "consent", fixture.body().replace("SYNTHETIC_PURCHASE", "NOT_CURRENT")), 409, "PURCHASE_CONSENT_STALE"); assertThat(fixture.rows()).isEqualTo(before);
        Object policy = fixture.policy(null);
        try { before = fixture.rows(); fixture.problem(fixture.post("missing-policy"), 409, "PURCHASE_CONSENT_REQUIRED"); assertThat(fixture.rows()).isEqualTo(before); }
        finally { fixture.policy(policy); }
        for (String change : List.of("DECLINE", "SUBJECT", "SOURCE", "CONSUMED")) {
            fixture.reset(); fixture.consentFixture(change); before = fixture.rows();
            fixture.problem(fixture.post("consent"), 409, change.equals("DECLINE") ? "PURCHASE_CONSENT_DECLINED" : "PURCHASE_CONSENT_REQUIRED"); assertThat(fixture.rows()).as(change).isEqualTo(before);
        }
    }
    void ownerRace(String key, String mutation, int denialStatus, String denialCode, boolean checkoutFirst) throws Exception {
        var baseline = fixture.rows(); CountDownLatch ready = new CountDownLatch(1), release = new CountDownLatch(1), writerAcquired = new CountDownLatch(1), mutate = new CountDownLatch(1);
        AtomicBoolean armed = new AtomicBoolean(true);
        try (var writer = fixture.admin(); var executor = Executors.newFixedThreadPool(2)) {
            writer.setAutoCommit(false); int writerPid = Integer.parseInt(com.onecall.archtests.container.OperationsIvrSessionReadFixture.one(writer, "SELECT pg_backend_pid()"));
            if (checkoutFirst) {
                fixture.after = execution -> { if (execution.sql().startsWith("INSERT INTO catalog_order.customer_order") && armed.getAndSet(false)) { ready.countDown(); await(release); } };
                var checkout = executor.submit(() -> fixture.post("serialized")); assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
                var lifecycle = executor.submit(() -> { ownerLock(writer, key); writerAcquired.countDown(); await(mutate); execute(writer, mutation); writer.commit(); return true; });
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                boolean blocked = false;
                while (System.nanoTime() < deadline) {
                    if (fixture.value("SELECT count(*) FROM pg_stat_activity WHERE pid=? AND wait_event_type='Lock'", writerPid).equals("1")) { blocked = true; break; }
                    Thread.sleep(5);
                }
                assertThat(blocked).as("owner writer waits behind checkout " + key).isTrue(); release.countDown();
                fixture.success(checkout.get(3, TimeUnit.SECONDS), 201); assertThat(writerAcquired.await(3, TimeUnit.SECONDS)).isTrue(); fixture.bundle(baseline);
                mutate.countDown(); assertThat(lifecycle.get(3, TimeUnit.SECONDS)).isTrue();
            } else {
                ownerLock(writer, key); execute(writer, mutation);
                fixture.before = execution -> {
                    boolean target = key.equals("ACTIVATION_ROW") ? execution.sql().contains("FOR UPDATE") : execution.query() != null && execution.query().getBindValues().contains(key);
                    if (target && armed.getAndSet(false)) { ready.countDown(); await(release); }
                };
                var checkout = executor.submit(() -> fixture.post("serialized")); assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
                assertThat(fixture.value("SELECT count(*) FROM pg_locks WHERE pid=? AND granted", writerPid)).isNotEqualTo("0");
                writer.commit(); var afterOwner = fixture.rows(); release.countDown();
                fixture.problem(checkout.get(3, TimeUnit.SECONDS), denialStatus, denialCode); assertThat(fixture.rows()).isEqualTo(afterOwner);
            }
            fixture.before = null; fixture.after = null;
            var afterOwner = fixture.rows(); fixture.problem(fixture.post("serialized"), denialStatus, denialCode); assertThat(fixture.rows()).isEqualTo(afterOwner);
        } finally { release.countDown(); mutate.countDown(); fixture.before = null; fixture.after = null; }
        fixture.clean();
    }
    void ownerLock(java.sql.Connection writer, String key) throws java.sql.SQLException {
        if (key.equals("ACTIVATION_ROW")) execute(writer, "SELECT id FROM activation_consent.service_activation WHERE subject_id=? FOR UPDATE", fixture.subject);
        else execute(writer, "SELECT pg_advisory_xact_lock(hashtextextended(?,0))", key);
    }
    @Test void publicationAndOffsaleSerializeWithCheckout() throws Exception {
        timeout("catalog_order.publication:" + fixture.product, fixture.body());
        for (boolean checkoutFirst : List.of(false, true)) {
            fixture.reset(); ownerRace("catalog_order.publication:" + fixture.product, "UPDATE catalog_order.catalog_version SET sale_status='OFFSALE',revision=revision+1", 422, "VALIDATION_FAILED", checkoutFirst);
        }
    }
    @Test void activationAndLifecycleMutationsSerializeWithCheckout() throws Exception {
        timeout("subject_number.subject:" + fixture.subject, fixture.body());
        for (boolean checkoutFirst : List.of(false, true)) for (String rank : List.of("SUBJECT", "ACTIVATION", "ORGANIZATION", "MEMBERSHIP")) {
            fixture.reset(); String key, mutation, code = "FORBIDDEN";
            if (rank.equals("SUBJECT")) { key = "subject_number.subject:" + fixture.subject; mutation = "UPDATE activation_consent.service_activation SET status='SUSPENDED',revision=revision+1"; code = "ACTIVATION_NOT_ACTIVE"; }
            else if (rank.equals("ACTIVATION")) { key = "ACTIVATION_ROW"; mutation = "UPDATE activation_consent.service_activation SET status='SUSPENDED',revision=revision+1"; code = "ACTIVATION_NOT_ACTIVE"; }
            else {
                UUID organization = fixture.organization("MEMBER");
                key = rank.equals("ORGANIZATION") ? "organization_access.organization:" + organization : "organization_access.membership:" + organization + ":" + fixture.account;
                mutation = rank.equals("ORGANIZATION") ? "UPDATE organization_access.organization SET status='DISABLED',revision=revision+1" : "UPDATE organization_access.organization_membership SET status='REVOKED',revision=revision+1";
            }
            ownerRace(key, mutation, 403, code, checkoutFirst);
        }
    }
    @Test void personalAccountDisableSerializesWithCheckout() throws Exception {
        timeout("customer_iam.account:" + fixture.account, fixture.body());
        for (boolean checkoutFirst : List.of(false, true)) {
            fixture.reset(); ownerRace("customer_iam.account:" + fixture.account, "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1", 403, "FORBIDDEN", checkoutFirst);
            assertThat(fixture.entries).noneMatch(entry -> entry.binds().stream().anyMatch(bind -> Objects.toString(bind).startsWith("organization_access.")));
        }
    }
    @Test void injectedFailureAtEveryWriteStageLeavesZeroPartialRows() throws Exception {
        for (String stage : List.of("idempotency_begin", "INSERT INTO activation_consent.consent_record", "INSERT INTO catalog_order.customer_order", "idempotency_complete", "outbox_append", "append_fact", "SET LOCAL ROLE NONE")) for (int occurrence = 1; occurrence <= (stage.equals("outbox_append") || stage.equals("append_fact") ? 2 : 1); occurrence++) {
            fixture.reset(); var before = fixture.rows(); AtomicBoolean armed = new AtomicBoolean(true);
            AtomicInteger seen = new AtomicInteger(); int target = occurrence;
            fixture.after = execution -> { if (execution.sql().contains(stage) && seen.incrementAndGet() == target && armed.getAndSet(false)) throw new IllegalStateException("PRIVATE fault"); };
            fixture.problem(fixture.post("fault"), stage.equals("SET LOCAL ROLE NONE") ? 503 : 500, stage.equals("SET LOCAL ROLE NONE") ? "SERVICE_UNAVAILABLE" : "INTERNAL"); assertThat(armed).as(stage + "/" + target).isFalse(); fixture.after = null; assertThat(fixture.rows()).isEqualTo(before); fixture.clean();
        }
        sqlFault("deadlock/40P01", "idempotency_complete", 1, false, "40P01", null, 503, "SERVICE_UNAVAILABLE", execution -> {
            try (var blocker = fixture.admin(); var executor = Executors.newSingleThreadExecutor()) {
                blocker.setAutoCommit(false); execute(blocker, "SET LOCAL deadlock_timeout = '4s'");
                execute(execution.connection(), "SELECT pg_advisory_xact_lock(hashtextextended('pa41-fault-first',0))");
                execute(blocker, "SELECT pg_advisory_xact_lock(hashtextextended('pa41-fault-second',0))");
                int backend = Integer.parseInt(com.onecall.archtests.container.OperationsIvrSessionReadFixture.one(blocker, "SELECT pg_backend_pid()"));
                var waiting = executor.submit(() -> { execute(blocker, "SELECT pg_advisory_xact_lock(hashtextextended('pa41-fault-first',0))"); return true; });
                try (var observer = fixture.admin()) {
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1); boolean blocked = false;
                    while (System.nanoTime() < deadline) if (com.onecall.archtests.container.OperationsIvrSessionReadFixture.one(observer, "SELECT count(*) FROM pg_stat_activity WHERE pid=? AND wait_event_type='Lock'", backend).equals("1")) { blocked = true; break; }
                    assertThat(blocked).as("real deadlock counterpart is blocked").isTrue();
                }
                try { execute(execution.connection(), "SELECT pg_advisory_xact_lock(hashtextextended('pa41-fault-second',0))"); }
                finally { blocker.rollback(); }
            }
        });
        sqlFault("statement timeout/57014", "idempotency_complete", 1, false, "57014", null, 503, "SERVICE_UNAVAILABLE", execution -> execute(execution.connection(), "SELECT pg_sleep(3)"));
        sqlFault("begin/IDEMPOTENCY_ARGUMENT_INVALID", "idempotency_begin", 1, false, "P0001", "IDEMPOTENCY_ARGUMENT_INVALID", 500, "INTERNAL", execution -> execute(execution.connection(), "SELECT platform.idempotency_begin(NULL,NULL,NULL)"));
        sqlFault("complete/IDEMPOTENCY_ARGUMENT_INVALID", "idempotency_complete", 1, false, "P0001", "IDEMPOTENCY_ARGUMENT_INVALID", 500, "INTERNAL", execution -> execute(execution.connection(), "SELECT platform.idempotency_complete(NULL,NULL,NULL,NULL,NULL,NULL)"));
        sqlFault("complete/IDEMPOTENCY_NOT_FOUND", "idempotency_complete", 1, false, "P0001", "IDEMPOTENCY_NOT_FOUND", 500, "INTERNAL", execution -> execute(execution.connection(), "SELECT platform.idempotency_complete(?,1,'ORDER',?,'{}'::jsonb,repeat('a',64))", UUID.randomUUID(), UUID.randomUUID()));
        sqlFault("complete/IDEMPOTENCY_CONFLICT", "idempotency_complete", 1, false, "P0001", "IDEMPOTENCY_CONFLICT", 500, "INTERNAL", execution -> {
            var values = execution.query().getBindValues();
            execute(execution.connection(), "SELECT platform.idempotency_complete(?,?,'ORDER',?,'{}'::jsonb,repeat('a',64))", values.get(0), values.get(1), values.get(2));
            execute(execution.connection(), "SELECT platform.idempotency_complete(?,?,'ORDER',?,'{}'::jsonb,repeat('b',64))", values.get(0), values.get(1), values.get(2));
        });
        for (int occurrence : List.of(1, 2)) {
            String owner = occurrence == 1 ? "consent" : "order";
            sqlFault(owner + "/OUTBOX_PAYLOAD_INVALID", "outbox_append", occurrence, false, "P0001", "OUTBOX_PAYLOAD_INVALID", 500, "INTERNAL", execution -> execute(execution.connection(), "SELECT platform.outbox_append('PURCHASE','fault','CUSTOMER_ORDER',?,'ORDER_CREATED','{\"password\":\"PRIVATE\"}'::jsonb,NULL,repeat('a',64))", UUID.randomUUID()));
            sqlFault(owner + "/OUTBOX_DEDUPE_CONFLICT", "outbox_append", occurrence, false, "P0001", "OUTBOX_DEDUPE_CONFLICT", 500, "INTERNAL", execution -> {
                var values = execution.query().getBindValues();
                execute(execution.connection(), "SELECT platform.outbox_append('PURCHASE',?,'CUSTOMER_ORDER',?,'ORDER_CREATED','{}'::jsonb,NULL,repeat('a',64))", values.get(0), values.get(1));
                execute(execution.connection(), "SELECT platform.outbox_append('PURCHASE',?,'CUSTOMER_ORDER',?,'ORDER_CREATED','{}'::jsonb,NULL,repeat('b',64))", values.get(0), values.get(1));
            });
            sqlFault(owner + "/AUDIT_ARGUMENT_INVALID", "append_fact", occurrence, false, "P0001", "AUDIT_ARGUMENT_INVALID", 500, "INTERNAL", execution -> execute(execution.connection(), "SELECT audit.append_fact(NULL,'ACCOUNT',?,'ORDER_CREATED','CUSTOMER_ORDER',?,NULL,NULL,'{}'::jsonb,NULL,NULL)", fixture.account, UUID.randomUUID()));
            sqlFault(owner + "/AUDIT_REASON_REQUIRED", "append_fact", occurrence, false, "P0001", "AUDIT_REASON_REQUIRED", 500, "INTERNAL", execution -> execute(execution.connection(), "SELECT audit.append_fact(now(),'EMPLOYEE',?,'ORDER_CREATED','CUSTOMER_ORDER',?,NULL,NULL,'{}'::jsonb,NULL,NULL)", fixture.employee, UUID.randomUUID()));
        }
        fixture.reset();
        String event = "purchase-consent:" + CustomerOrdersFixture.sha(CustomerOrdersFixture.canonical(fixture.mapper.readTree("{\"accountId\":\"" + fixture.account + "\",\"clientIdempotencyKey\":\"incomplete\"}")));
        String fingerprint = CustomerOrdersFixture.sha(CustomerOrdersFixture.canonical(fixture.mapper.readTree("{\"accountId\":\"" + fixture.account + "\",\"amountCents\":123,\"catalogOfferId\":\"" + fixture.offer + "\",\"catalogVersionId\":\"" + fixture.version + "\",\"channel\":\"WEB\",\"currency\":\"CNY\",\"ownerOrganizationId\":null,\"purchaseConsentEventId\":\"" + event + "\",\"subjectId\":\"" + fixture.subject + "\",\"versionToken\":\"Pa41-Exact\"}")));
        fixture.sql("SELECT platform.idempotency_begin('PURCHASE',?,?)", fixture.account + ":incomplete", fingerprint);
        var durable = fixture.rows(); fixture.problem(fixture.post("incomplete"), 503, "SERVICE_UNAVAILABLE"); assertThat(fixture.rows()).as("begin/IDEMPOTENCY_INCOMPLETE durable NEW").isEqualTo(durable); fixture.clean();
        for (String collision : List.of("uq_customer_order_idempotency", "pk_customer_order", "uq_customer_order_no")) {
            sqlFault(collision, "INSERT INTO catalog_order.customer_order", 1, true, "23505", null, collision.equals("uq_customer_order_idempotency") ? 409 : 500, collision.equals("uq_customer_order_idempotency") ? "IDEMPOTENCY_CONFLICT" : "INTERNAL", execution -> {
                String replacement = collision.equals("uq_customer_order_idempotency") ? "{\"id\":\"" + UUID.randomUUID() + "\",\"order_no\":\"collision-other\"}" : collision.equals("uq_customer_order_no") ? "{\"id\":\"" + UUID.randomUUID() + "\"}" : "{}";
                execute(execution.connection(), "INSERT INTO catalog_order.customer_order SELECT (jsonb_populate_record(NULL::catalog_order.customer_order,to_jsonb(original)||?::jsonb)).* FROM catalog_order.customer_order original", replacement);
            });
        }
        for (String collision : List.of("uq_consent_event", "pk_consent_record")) sqlFault(collision, "INSERT INTO activation_consent.consent_record", 1, true, "23505", null, collision.equals("uq_consent_event") ? 409 : 500, collision.equals("uq_consent_event") ? "IDEMPOTENCY_CONFLICT" : "INTERNAL", execution -> execute(execution.connection(), "INSERT INTO activation_consent.consent_record SELECT (jsonb_populate_record(NULL::activation_consent.consent_record,to_jsonb(original)||?::jsonb)).* FROM activation_consent.consent_record original", collision.equals("uq_consent_event") ? "{\"id\":\"" + UUID.randomUUID() + "\"}" : "{}"));
        sqlFault("23514/order amount", "INSERT INTO catalog_order.customer_order", 1, false, "23514", null, 500, "INTERNAL", execution -> { Object[] values = execution.query().getBindValues().toArray(); values[9] = -1L; execute(execution.connection(), execution.sql(), values); });
        for (int foreignKey : List.of(2, 3, 4, 6, 7, 8)) sqlFault("23503/order FK column " + foreignKey, "INSERT INTO catalog_order.customer_order", 1, false, "23503", null, 500, "INTERNAL", execution -> { Object[] values = execution.query().getBindValues().toArray(); values[foreignKey] = UUID.randomUUID(); execute(execution.connection(), execution.sql(), values); });
        assertThat(fixture.value("SELECT pg_get_functiondef('audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text)'::regprocedure)")).contains("INSERT INTO audit.audit_fact").doesNotContain("UPDATE audit.audit_fact", "DELETE FROM audit.audit_fact", "AUDIT_APPEND_ONLY");
    }
    @FunctionalInterface interface SqlFault { void run(org.jooq.ExecuteContext execution) throws java.sql.SQLException; }
    void sqlFault(String label, String site, int occurrence, boolean after, String state, String token, int status, String code, SqlFault action) throws Exception {
        fixture.reset(); var baseline = fixture.rows(); AtomicInteger seen = new AtomicInteger(); AtomicBoolean armed = new AtomicBoolean(true);
        java.util.function.Consumer<org.jooq.ExecuteContext> hook = execution -> {
            if (!execution.sql().contains(site) || seen.incrementAndGet() != occurrence || !armed.getAndSet(false)) return;
            try { action.run(execution); throw new AssertionError("Expected PostgreSQL failure: " + label); }
            catch (java.sql.SQLException failure) {
                assertThat(failure.getSQLState()).as(label).isEqualTo(state);
                if (token != null) assertThat(((org.postgresql.util.PSQLException) failure).getServerErrorMessage().getMessage()).as(label).isEqualTo(token);
                throw new org.jooq.exception.DataAccessException("Synthetic labelled fault", failure);
            }
        };
        if (after) fixture.after = hook; else fixture.before = hook;
        var response = fixture.post("fault"); assertThat(response.statusCode()).as(label + ": " + response.body()).isEqualTo(status); fixture.problem(response, status, code); assertThat(armed).as(label).isFalse();
        fixture.before = null; fixture.after = null; assertThat(fixture.rows()).as(label).isEqualTo(baseline); fixture.clean();
    }
    @Test void replayAfterCatalogOrPolicyChangeFollowsExactPrecedence() throws Exception {
        var original = fixture.success(fixture.post("original"), 201);
        var replacement = new com.onecall.core.activation_consent.internal.purchase.PurchaseConsentConfiguration.CurrentPolicy("SYNTHETIC_DEPLOYMENT_2", "2", "2", "b".repeat(64));
        Object policy = fixture.policy(replacement);
        try { var unchanged = fixture.rows(); assertThat(fixture.success(fixture.post("original"), 200)).isEqualTo(original); assertThat(fixture.rows()).isEqualTo(unchanged); }
        finally { fixture.policy(policy); }
        fixture.sql("UPDATE catalog_order.customer_order SET payment_status='PAID',paid_at=now(),revision=revision+1");
        var paid = fixture.rows(); assertThat(fixture.success(fixture.post("original"), 200)).isEqualTo(original); assertThat(fixture.rows()).isEqualTo(paid);
        fixture.sql("UPDATE catalog_order.catalog_offer SET version_token='changed',revision=revision+1"); var before = fixture.rows(); fixture.problem(fixture.post("original"), 409, "IDEMPOTENCY_CONFLICT"); assertThat(fixture.rows()).isEqualTo(before);
        fixture.sql("UPDATE catalog_order.catalog_offer SET offer_status='WITHDRAWN',revision=revision+1"); before = fixture.rows(); fixture.problem(fixture.post("original"), 422, "VALIDATION_FAILED"); assertThat(fixture.rows()).isEqualTo(before); assertThat(original.get("paymentStatus").asString()).isEqualTo("UNPAID");
    }
    @Test void allPathsAvoidPaymentIssuanceAndProviderCalls() throws Exception {
        var before = fixture.rows(); fixture.success(fixture.post("only-order"), 201); fixture.bundle(before);
        var complete = fixture.rows(); fixture.success(fixture.post("only-order"), 200); assertThat(fixture.rows()).isEqualTo(complete);
        assertThat(fixture.entries).noneMatch(entry -> entry.sql().matches("(?is).*(INSERT|UPDATE|DELETE|MERGE|TRUNCATE)\\s+(INTO\\s+)?catalog_order\\.catalog_(product|version|offer|benefit_line).*"));
        assertThat(fixture.entries).noneMatch(entry -> entry.sql().contains("entitlement_metering.") || entry.sql().contains("payment_order") || entry.sql().contains("payment_event"));
    }
}
