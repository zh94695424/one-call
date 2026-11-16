package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.assertThat;
import static com.onecall.archtests.container.OperationsSubjectReadFixture.problem;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.one;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashSet;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OperationsSubjectReadsIT {
    static final OperationsSubjectReadFixture fixture = new OperationsSubjectReadFixture();

    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }

    @Test
    void postgres16AppliesExactlyFrozenSeventeenMigrations() throws Exception {
        try (var connection = fixture.admin()) {
            assertThat(Integer.parseInt(one(connection, "SHOW server_version_num"))).isBetween(160000, 169999);
            assertThat(one(connection, "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL")).isEqualTo("18");
            assertThat(one(connection, "SHOW server_encoding")).isEqualTo("UTF8");
        }
    }

    @Test
    void activeAdminAndOpsReadWithIndependentEmployeeSessions() throws Exception {
        var subject = fixture.subject();
        for (String token : List.of("admin", "ops")) {
            for (String path : List.of(OperationsSubjectReadFixture.BASE, OperationsSubjectReadFixture.BASE + "/" + subject)) {
                var response = fixture.request("GET", path, token);
                assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
                assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
                assertThat(response.body()).contains(subject.toString()).doesNotContain(fixture.password, fixture.login);
            }
        }
    }

    @Test
    void disabledMissingAndWrongAudienceEmployeesFailBeforeDisclosure() throws Exception {
        for (String path : List.of(OperationsSubjectReadFixture.BASE + "?limit=bad", OperationsSubjectReadFixture.BASE + "/bad")) {
            problem(fixture.request("GET", path, "disabled", "text/html"), 403, "FORBIDDEN");
            for (String token : List.of("missing", "wrong")) problem(fixture.request("GET", path, token), 401, "UNAUTHENTICATED");
            for (String accept : List.of("text/html", "application/json;q=0", "application/json;q=broken", "application/json;q=0, */*;q=1")) {
                for (String token : List.of("admin", "ops")) problem(fixture.request("GET", path, token, accept), 422, "VALIDATION_FAILED");
            }
        }
    }

    @Test
    void listDefaultsAndEveryStructuredFilterAreExact() throws Exception {
        for (String source : List.of("IVR", "MINIAPP", "WEB", "OPERATIONS")) {
            for (String realName : List.of("UNVERIFIED", "VERIFIED")) {
                for (String risk : List.of("OPEN", "CLEARED")) {
                    UUID subject = fixture.subject(UUID.randomUUID(), source, realName, fixture.time);
                    fixture.risk(fixture.binding(subject, UUID.randomUUID(), "ACTIVE", "138****0000"), risk);
                }
            }
        }
        assertThat(fixture.get(OperationsSubjectReadFixture.BASE).path("items").size()).isEqualTo(16);
        for (String source : List.of("IVR", "MINIAPP", "WEB", "OPERATIONS")) {
            assertThat(fixture.get(OperationsSubjectReadFixture.BASE + "?source=" + source).path("items").size()).isEqualTo(4);
            for (String realName : List.of("UNVERIFIED", "VERIFIED")) {
                for (String risk : List.of("OPEN", "CLEARED")) {
                    var page = fixture.get(OperationsSubjectReadFixture.BASE + "?source=" + source + "&realNameStatus=" + realName + "&riskState=" + risk);
                    assertThat(page.path("items").size()).isEqualTo(1);
                    assertThat(page.path("items").get(0).path("riskState").asString()).isEqualTo(risk);
                }
            }
        }
        for (String limit : List.of("1", "20", "100")) assertThat(fixture.get(OperationsSubjectReadFixture.BASE + "?limit=" + limit).path("items").size()).isEqualTo(Math.min(16, Integer.parseInt(limit)));
    }

    @Test
    void latestClaimFilterDoesNotMatchOlderHistoricalStatus() throws Exception {
        UUID subject = fixture.subject();
        fixture.claim(subject, UUID.randomUUID(), "ACCEPTED", fixture.time);
        fixture.claim(subject, UUID.randomUUID(), "REJECTED", fixture.time.plusSeconds(1));
        assertThat(fixture.get(OperationsSubjectReadFixture.BASE + "?claimStatus=ACCEPTED").path("items").size()).isZero();
        var page = fixture.get(OperationsSubjectReadFixture.BASE + "?claimStatus=REJECTED");
        assertThat(page.path("items").size()).isEqualTo(1);
        assertThat(page.path("items").get(0).path("claimStatus").asString()).isEqualTo("REJECTED");
    }

    @Test
    void allBindingRiskStatesAndAbsentActivationHaveExactProjection() throws Exception {
        UUID active = fixture.subject();
        fixture.risk(fixture.binding(active, UUID.randomUUID(), "ACTIVE", "111****0000"), "CLEARED");
        UUID disabled = fixture.subject();
        fixture.risk(fixture.binding(disabled, new UUID(1, 1), "DISABLED", "222****0000"), "CLEARED");
        fixture.risk(fixture.binding(disabled, new UUID(1, 2), "DISABLED", "999****0000"), "OPEN");
        UUID retired = fixture.subject();
        fixture.risk(fixture.binding(retired, UUID.randomUUID(), "RETIRED", "333****0000"), "CLEARED");
        UUID withoutRisk = fixture.subject();
        fixture.binding(withoutRisk, UUID.randomUUID(), "DISABLED", "444****0000");
        UUID allCleared = fixture.subject();
        fixture.risk(fixture.binding(allCleared, UUID.randomUUID(), "ACTIVE", "555****0000"), "CLEARED");
        fixture.risk(fixture.binding(allCleared, UUID.randomUUID(), "DISABLED", "888****0000"), "CLEARED");
        fixture.risk(fixture.binding(allCleared, UUID.randomUUID(), "RETIRED", "777****0000"), "CLEARED");
        UUID mixed = fixture.subject();
        fixture.risk(fixture.binding(mixed, UUID.randomUUID(), "ACTIVE", "666****0000"), "CLEARED");
        fixture.risk(fixture.binding(mixed, UUID.randomUUID(), "RETIRED", "999****0000"), "OPEN");
        var expected = Map.of(active, List.of("CLEARED", "111****0000"), disabled, List.of("OPEN", "222****0000"),
                retired, List.of("CLEARED", "333****0000"), withoutRisk, List.of("CLEARED", "444****0000"),
                allCleared, List.of("CLEARED", "555****0000"), mixed, List.of("OPEN", "666****0000"));
        for (var entry : expected.entrySet()) {
            var items = fixture.get(OperationsSubjectReadFixture.BASE + "?subjectId=" + entry.getKey()).path("items");
            assertThat(items.size()).isEqualTo(1);
            var item = items.get(0);
            var detail = fixture.get(OperationsSubjectReadFixture.BASE + "/" + entry.getKey());
            assertThat(item.path("subjectId").asString()).isEqualTo(entry.getKey().toString());
            assertThat(item.path("riskState").asString()).isEqualTo(entry.getValue().get(0));
            assertThat(detail.path("riskState").asString()).isEqualTo(entry.getValue().get(0));
            assertThat(item.path("maskedNumber").asString()).isEqualTo(entry.getValue().get(1));
            assertThat(detail.path("maskedNumber").asString()).isEqualTo(entry.getValue().get(1));
            assertThat(item.path("activationStatus").asString()).isEqualTo("INACTIVE");
            assertThat(detail.has("activation")).isFalse();
        }
    }

    @Test
    void stableTimestampUuidTiesSeekWithoutDuplicatesOrOffset() throws Exception {
        var expected = List.of(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), UUID.fromString("80000000-0000-0000-0000-000000000000"),
                UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffffff"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        for (UUID identifier : expected) fixture.subject(identifier, "WEB", "UNVERIFIED", fixture.time);
        var found = new ArrayList<UUID>();
        String path = OperationsSubjectReadFixture.BASE + "?limit=1";
        while (true) {
            var page = fixture.get(path);
            found.add(UUID.fromString(page.path("items").get(0).path("subjectId").asString()));
            if (!page.path("hasMore").asBoolean()) break;
            path = OperationsSubjectReadFixture.BASE + "?limit=1&cursor=" + page.path("nextCursor").asString();
        }
        assertThat(found).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
    }

    @Test
    void limitPlusOneEmptyAndTailPagesUseLastReturnedAnchor() throws Exception {
        var empty = fixture.get(OperationsSubjectReadFixture.BASE);
        assertThat(empty.path("items").size()).isZero();
        assertThat(empty.path("nextCursor").isNull()).isTrue();
        for (int index = 1; index <= 3; index++) fixture.subject(new UUID(0, index), "WEB", "UNVERIFIED", fixture.time);
        var first = fixture.get(OperationsSubjectReadFixture.BASE + "?limit=2");
        assertThat(first.path("items").size()).isEqualTo(2);
        assertThat(first.path("hasMore").asBoolean()).isTrue();
        var last = fixture.get(OperationsSubjectReadFixture.BASE + "?limit=2&cursor=" + first.path("nextCursor").asString());
        assertThat(last.path("items").size()).isEqualTo(1);
        assertThat(last.path("items").get(0).path("subjectId").asString()).isEqualTo(new UUID(0, 1).toString());
        assertThat(last.path("nextCursor").isNull()).isTrue();
        assertThat(last.path("hasMore").asBoolean()).isFalse();
    }

    @Test
    void normalizedFiltersBindCursorButPageSizeDoesNot() throws Exception {
        for (int index = 1; index <= 3; index++) fixture.subject(new UUID(0, index), "WEB", "UNVERIFIED", fixture.time);
        String cursor = fixture.get(OperationsSubjectReadFixture.BASE + "?limit=1&source=WEB").path("nextCursor").asString();
        assertThat(fixture.get(OperationsSubjectReadFixture.BASE + "?source=WEB&cursor=" + cursor + "&limit=100").path("items").size()).isEqualTo(2);
        problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "?source=IVR&cursor=" + cursor, "admin"), 400, "CURSOR_FILTER_MISMATCH");
    }

    @Test
    void tamperedWrongKeyAndRotatedCursorsHaveExactProblems() throws Exception {
        for (int index = 1; index <= 2; index++) fixture.subject(new UUID(0, index), "WEB", "UNVERIFIED", fixture.time);
        String cursor = fixture.get(OperationsSubjectReadFixture.BASE + "?limit=1").path("nextCursor").asString();
        byte[] envelope = java.util.Base64.getUrlDecoder().decode(cursor);
        for (int position : List.of(0, 5, envelope.length - 1)) {
            byte[] tampered = envelope.clone();
            tampered[position] ^= 1;
            problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "?cursor=" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(tampered), "admin"), 400, "CURSOR_INVALID");
        }
        var properties = fixture.context.getBean(com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.class);
        var codec = new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectCursorCodec(properties);
        var digest = com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadRequest.list(
                new com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.RawListRequest(Map.of(), List.of())).digest();
        assertThat(codec.decode(cursor, digest).subjectId()).isEqualTo(new UUID(0, 2));
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, (byte) 71);
        var nextKey = new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.KeyEntry(
                "Rotated-Key_2026", java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        var wrongKey = new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.KeyEntry(
                properties.cursor().activeKeyId(), nextKey.material());
        var wrongProperties = new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties(true, null,
                new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.Cursor(wrongKey.id(), List.of(wrongKey)));
        String wrongToken = new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectCursorCodec(wrongProperties)
                .encode(codec.decode(cursor, digest), digest);
        problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "?cursor=" + wrongToken, "admin"), 400, "CURSOR_INVALID");
        try {
            fixture.replaceCursor(new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.Cursor(
                    nextKey.id(), List.of(nextKey, properties.cursor().keys().getFirst())));
            assertThat(fixture.get(OperationsSubjectReadFixture.BASE + "?cursor=" + cursor).path("items").size()).isEqualTo(1);
            String rotated = fixture.get(OperationsSubjectReadFixture.BASE + "?limit=1").path("nextCursor").asString();
            assertThat(java.util.Base64.getUrlDecoder().decode(rotated)[2]).isEqualTo((byte) 'R');
            fixture.replaceCursor(new com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties.Cursor(nextKey.id(), List.of(nextKey)));
            problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "?cursor=" + cursor, "admin"), 400, "CURSOR_INVALID");
            assertThat(fixture.get(OperationsSubjectReadFixture.BASE + "?cursor=" + rotated).path("items").size()).isEqualTo(1);
        } finally { fixture.replaceCursor(properties.cursor()); }
    }

    @Test
    void strictInputsAndDuplicatesHaveFrozenErrorPrecedence() throws Exception {
        UUID subject = fixture.subject();
        for (String query : List.of("limit=1&limit=1", "cursor=bad&cursor=bad", "unknown=x", "source=web", "limit=101", "subjectId=ABCDEF01-2345-6789-ABCD-0123456789AB", "cursor=bad&limit=bad")) {
            problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "?" + query, "admin"), 422, "VALIDATION_FAILED");
        }
        for (String path : List.of(OperationsSubjectReadFixture.BASE, OperationsSubjectReadFixture.BASE + "/" + subject)) {
            problem(fixture.request("GET", path, "disabled", "text/html"), 403, "FORBIDDEN");
            for (String accept : List.of("text/html", "application/json;q=0", "application/json;q=broken", "application/json;q=0, */*;q=1")) {
                problem(fixture.request("GET", path, "admin", accept), 422, "VALIDATION_FAILED");
            }
        }
    }

    @Test
    void detailReturnsOnlySafeFieldsAndOmissionRules() throws Exception {
        UUID subject = fixture.subject();
        var body = fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
        assertThat(body.propertyNames()).containsExactlyInAnyOrder("subjectId", "source", "realNameStatus", "riskState", "bindings", "claims", "evidenceCount", "orderCount", "createdAt");
        assertThat(body.path("bindings").size()).isZero();
        assertThat(body.path("claims").size()).isZero();
        assertThat(body.path("evidenceCount").asLong()).isZero();
        assertThat(body.path("orderCount").asLong()).isZero();
    }

    @Test
    void displayBindingAndClaimOrderingAreDeterministic() throws Exception {
        UUID subject = fixture.subject();
        fixture.binding(subject, new UUID(0, 1), "RETIRED", "111****0000");
        fixture.binding(subject, new UUID(0, 2), "ACTIVE", "222****0000");
        fixture.binding(subject, new UUID(0, 3), "ACTIVE", "333****0000");
        fixture.claim(subject, new UUID(0, 4), "CONFLICT", fixture.time);
        fixture.claim(subject, new UUID(0, 5), "REJECTED", fixture.time);
        var body = fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
        assertThat(body.path("maskedNumber").asString()).isEqualTo("222****0000");
        assertThat(body.path("bindings").get(0).path("bindingId").asString()).isEqualTo(new UUID(0, 1).toString());
        assertThat(body.path("claims").get(0).path("claimId").asString()).isEqualTo(new UUID(0, 5).toString());
    }

    @Test
    void detailHistoryZeroOneHundredAndOverflowAreBounded() throws Exception {
        for (var sizes : List.of(List.of(0, 0), List.of(1, 1), List.of(100, 100),
                List.of(101, 1), List.of(1, 101), List.of(101, 101))) {
            int bindingCount = sizes.get(0);
            int claimCount = sizes.get(1);
            UUID subject = fixture.subject();
            for (int index = 1; index <= bindingCount; index++) {
                fixture.binding(subject, UUID.randomUUID(), "RETIRED", "***");
            }
            for (int index = 1; index <= claimCount; index++) {
                fixture.claim(subject, UUID.randomUUID(), "REJECTED", fixture.time);
            }
            if (bindingCount > 100 || claimCount > 100) {
                var response = fixture.request("GET", OperationsSubjectReadFixture.BASE + "/" + subject, "admin");
                problem(response, 503, "SERVICE_UNAVAILABLE");
                assertThat(response.body()).doesNotContain("bindings", "claims", "maskedNumber", subject.toString(), "PRIVATE_", "SQL", "42501");
            } else {
                var body = fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
                assertThat(body.path("bindings").size()).isEqualTo(bindingCount);
                assertThat(body.path("claims").size()).isEqualTo(claimCount);
            }
        }
    }

    @Test
    void trialAbsentOriginalExpiredExhaustedAndReservedRulesAreExact() throws Exception {
        assertThat(fixture.get(OperationsSubjectReadFixture.BASE + "/" + fixture.subject()).has("trial")).isFalse();
        Instant future = Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        for (long reserved : List.of(0L, 25L, 100L)) {
            UUID subject = fixture.subject();
            fixture.trial(subject, reserved, 0, "AVAILABLE", future, true, false);
            var trial = fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject).path("trial");
            assertThat(trial.path("status").asString()).isEqualTo("GRANTED");
            assertThat(trial.path("grantedSeconds").asLong()).isEqualTo(100);
            assertThat(trial.path("remainingSeconds").asLong()).isEqualTo(100 - reserved);
            assertThat(Instant.parse(trial.path("expiresAt").asString())).isEqualTo(future);
        }
        for (String status : List.of("EXHAUSTED", "EXPIRED", "AVAILABLE")) {
            UUID subject = fixture.subject();
            fixture.trial(subject, 0, "EXHAUSTED".equals(status) ? 100 : 0, status,
                    "AVAILABLE".equals(status) ? future.minusSeconds(7200) : future, true, false);
            assertThat(fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject).path("trial").path("status").asString())
                    .isEqualTo("EXHAUSTED".equals(status) ? "EXHAUSTED" : "EXPIRED");
        }
    }

    @Test
    void missingOrInconsistentTrialRelationsFailWithoutRepair() throws Exception {
        for (boolean mismatch : List.of(false, true)) {
            UUID subject = fixture.subject();
            fixture.trial(subject, 0, 0, "AVAILABLE", Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS), mismatch, mismatch);
            var before = fixture.fingerprints();
            problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "/" + subject, "admin"), 503, "SERVICE_UNAVAILABLE");
            assertThat(fixture.fingerprints()).isEqualTo(before);
        }
    }

    @Test
    void evidenceAndOrderCountsIncludeAllSubjectStatusesAndPurchasers() throws Exception {
        UUID subject = fixture.subject();
        UUID unrelated = fixture.subject();
        for (String status : List.of("PENDING", "SOLIDIFIED", "FAILED")) fixture.evidence(subject, status);
        fixture.evidence(unrelated, "PENDING");
        for (String status : List.of("UNPAID", "UNKNOWN", "PAID", "CLOSED")) fixture.order(subject, status, UUID.randomUUID(), "PAID".equals(status) ? UUID.randomUUID() : null);
        fixture.order(unrelated, "UNPAID", UUID.randomUUID(), null);
        var body = fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
        assertThat(body.path("evidenceCount").asLong()).isEqualTo(3);
        assertThat(body.path("orderCount").asLong()).isEqualTo(4);
    }

    @Test
    void activationFilteredCandidateBatchesRemainCompleteWithinBudget() throws Exception {
        candidates(205);
        try (var connection = fixture.admin()) {
            execute(connection, "INSERT INTO activation_consent.service_activation(id,subject_id,status) SELECT id,id,'ACTIVE' FROM subject_number.business_subject ORDER BY created_at DESC,id DESC OFFSET 150 LIMIT 2");
        }
        var first = fixture.get(OperationsSubjectReadFixture.BASE + "?activationStatus=ACTIVE&limit=1");
        assertThat(first.path("items").size()).isEqualTo(1);
        assertThat(first.path("hasMore").asBoolean()).isTrue();
        var last = fixture.get(OperationsSubjectReadFixture.BASE + "?activationStatus=ACTIVE&limit=1&cursor=" + first.path("nextCursor").asString());
        assertThat(last.path("items").size()).isEqualTo(1);
        assertThat(last.path("items").get(0).path("subjectId").asString()).isNotEqualTo(first.path("items").get(0).path("subjectId").asString());
        assertThat(last.path("hasMore").asBoolean()).isFalse();
    }

    @Test
    void candidateBudgetAndDatabaseTimeoutsNeverReturnPartialSuccess() throws Exception {
        candidates(10100);
        var before = fixture.fingerprints();
        problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "?activationStatus=ACTIVE", "admin"), 503, "SERVICE_UNAVAILABLE");
        assertThat(fixture.fingerprints()).isEqualTo(before);
        var providers = fixture.dsl.configuration().executeListenerProviders();
        var armed = new AtomicBoolean(true);
        fixture.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeStart(ExecuteContext context) {
                if (context.sql().contains("WITH safe_subjects") && armed.getAndSet(false)) {
                    try (var statement = context.connection().createStatement()) { statement.execute("SELECT pg_sleep(3)"); }
                    catch (java.sql.SQLException failure) {
                        assertThat(failure.getSQLState()).isEqualTo("57014");
                        throw new org.jooq.exception.DataAccessException("PRIVATE_TIMEOUT", failure);
                    }
                }
            }
        });
        try { problem(fixture.request("GET", OperationsSubjectReadFixture.BASE, "admin"), 503, "SERVICE_UNAVAILABLE"); }
        finally { fixture.dsl.configuration().set(providers); }
        assertThat(armed.get()).isFalse();
        assertThat(fixture.fingerprints()).isEqualTo(before);
    }

    @Test
    void authoritySubjectActivationTrialAndCountsShareOneSnapshot() throws Exception {
        UUID subject = fixture.subject();
        var established = new CountDownLatch(1);
        var committed = new CountDownLatch(1);
        var armed = new AtomicBoolean(true);
        var providers = fixture.dsl.configuration().executeListenerProviders();
        fixture.dsl.configuration().set(new DefaultExecuteListener() {
            @Override public void executeEnd(ExecuteContext context) {
                if (context.sql().contains("FROM operations_iam.employee_account") && armed.getAndSet(false)) {
                    established.countDown();
                    try { assertThat(committed.await(10, TimeUnit.SECONDS)).isTrue(); }
                    catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
                }
            }
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var response = executor.submit(() -> fixture.request("GET", OperationsSubjectReadFixture.BASE + "/" + subject, "admin"));
            assertThat(established.await(10, TimeUnit.SECONDS)).isTrue();
            fixture.activation(subject, "ACTIVE");
            fixture.trial(subject, 25, 0, "AVAILABLE", Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MICROS), true, false);
            fixture.evidence(subject, "PENDING");
            fixture.order(subject, "UNPAID", UUID.randomUUID(), null);
            try (var connection = fixture.admin()) {
                connection.setAutoCommit(false);
                execute(connection, "UPDATE operations_iam.employee_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.employee);
                execute(connection, "SET LOCAL ROLE subject_identity_verification_role");
                execute(connection, "UPDATE subject_number.business_subject SET real_name_status='VERIFIED',real_name_verified_at=now(),revision=revision+1 WHERE id=?", subject);
                connection.commit();
            }
            committed.countDown();
            var current = response.get(10, TimeUnit.SECONDS);
            assertThat(current.statusCode()).as(current.body()).isEqualTo(200);
            assertThat(OperationsSubjectReadFixture.json(current).path("realNameStatus").asString()).isEqualTo("UNVERIFIED");
            var original = OperationsSubjectReadFixture.json(current);
            assertThat(original.has("activation")).isFalse();
            assertThat(original.has("trial")).isFalse();
            assertThat(original.path("evidenceCount").asLong()).isZero();
            assertThat(original.path("orderCount").asLong()).isZero();
            problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "/" + subject, "admin"), 403, "FORBIDDEN");
            var next = OperationsSubjectReadFixture.json(fixture.request("GET", OperationsSubjectReadFixture.BASE + "/" + subject, "ops"));
            assertThat(next.path("realNameStatus").asString()).isEqualTo("VERIFIED");
            assertThat(next.path("activation").path("status").asString()).isEqualTo("ACTIVE");
            assertThat(next.path("trial").path("remainingSeconds").asLong()).isEqualTo(75);
            assertThat(next.path("evidenceCount").asLong()).isEqualTo(1);
            assertThat(next.path("orderCount").asLong()).isEqualTo(1);
        } finally { committed.countDown(); fixture.dsl.configuration().set(providers); }
    }

    @Test
    void committedEmployeeAndDomainChangesAreVisibleOnNextRequest() throws Exception {
        UUID subject = fixture.subject();
        assertThat(fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject).path("realNameStatus").asString()).isEqualTo("UNVERIFIED");
        try (var connection = fixture.admin()) {
            connection.setAutoCommit(false);
            execute(connection, "SET LOCAL ROLE subject_identity_verification_role");
            execute(connection, "UPDATE subject_number.business_subject SET real_name_status='VERIFIED',real_name_verified_at=now(),revision=revision+1 WHERE id=?", subject);
            connection.commit();
        }
        assertThat(fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject).path("realNameStatus").asString()).isEqualTo("VERIFIED");
        try (var connection = fixture.admin()) { execute(connection, "UPDATE operations_iam.employee_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.employee); }
        problem(fixture.request("GET", OperationsSubjectReadFixture.BASE, "admin"), 403, "FORBIDDEN");
    }

    @Test
    void successDenialNotFoundAndFaultsPerformZeroBusinessWrites() throws Exception {
        UUID subject = fixture.subject();
        var before = fixture.fingerprints();
        fixture.get(OperationsSubjectReadFixture.BASE);
        fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
        problem(fixture.request("GET", OperationsSubjectReadFixture.BASE, "disabled"), 403, "FORBIDDEN");
        problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "/" + UUID.randomUUID(), "admin"), 404, "NOT_FOUND");
        problem(fixture.request("GET", OperationsSubjectReadFixture.BASE + "?cursor=bad", "admin"), 400, "CURSOR_INVALID");
        assertThat(fixture.fingerprints()).isEqualTo(before);
    }

    @Test
    void readEndpointsAndExistingCommandCustomerPublicRoutesStayIsolated() throws Exception {
        UUID subject = fixture.subject();
        fixture.get(OperationsSubjectReadFixture.BASE);
        fixture.get(OperationsSubjectReadFixture.BASE + "/" + subject);
        problem(fixture.request("POST", OperationsSubjectReadFixture.BASE, "admin"), 404, "NOT_FOUND");
        problem(fixture.request("DELETE", OperationsSubjectReadFixture.BASE + "/" + subject, "admin"), 404, "NOT_FOUND");
        for (String path : List.of("/api/v1/operations/employees", OperationsSubjectReadFixture.BASE + "/" + subject + "/employees", OperationsSubjectReadFixture.BASE + "/%61", OperationsSubjectReadFixture.BASE + "/" + subject + ";x", OperationsSubjectReadFixture.BASE + "//hidden")) {
            problem(fixture.request("GET", path, "admin"), 404, "NOT_FOUND");
        }
        assertThat(fixture.request("GET", "/api/v1/customer/me", "admin").statusCode()).isEqualTo(401);
        assertThat(fixture.request("POST", OperationsSubjectReadFixture.BASE + "/" + subject + "/suspensions", null).statusCode()).isEqualTo(401);
    }

    @Test
    void responsesProblemsAndCapturedLogsNeverContainSensitiveCanaries() throws Exception {
        UUID subject = fixture.subject();
        fixture.binding(subject, UUID.randomUUID(), "ACTIVE", "138****0000");
        fixture.claim(subject, UUID.randomUUID(), "ACCEPTED", fixture.time);
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var capture = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        capture.start();
        logger.addAppender(capture);
        try {
            for (String path : List.of(OperationsSubjectReadFixture.BASE, OperationsSubjectReadFixture.BASE + "/" + subject)) {
                var success = fixture.request("GET", path, "admin");
                assertThat(success.statusCode()).isEqualTo(200);
                assertThat(success.body()).doesNotContain("PRIVATE_PHONE", "PRIVATE_CLAIM", "PRIVATE_ACCOUNT", fixture.password, fixture.login, "Bearer admin");
                var failure = fixture.request("GET", path, "disabled");
                problem(failure, 403, "FORBIDDEN");
                assertThat(failure.body()).doesNotContain(subject.toString(), fixture.employee.toString(), "PRIVATE");
            }
            assertThat(capture.list).allSatisfy(event -> assertThat(event.getFormattedMessage()).doesNotContain("PRIVATE_PHONE", "PRIVATE_CLAIM", fixture.password, "Bearer admin"));
        } finally { logger.detachAppender(capture); capture.stop(); }
    }

    private void candidates(int count) throws Exception {
        try (var connection = fixture.admin()) {
            execute(connection, "INSERT INTO subject_number.business_subject(id,source,created_at,updated_at) SELECT md5('pa33-candidate-'||candidate)::uuid,'IVR',?::timestamptz,?::timestamptz FROM generate_series(1,?) candidate", fixture.time.toString(), fixture.time.toString(), count);
        }
    }
}
