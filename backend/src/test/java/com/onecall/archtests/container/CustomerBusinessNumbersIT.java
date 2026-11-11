package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;

import com.onecall.core.customer_iam.internal.application.*;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.*;
import com.onecall.core.activation_consent.customerquery.CustomerActivationStatusReader;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.jooq.DSLContext;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class CustomerBusinessNumbersIT {
    private final CustomerBusinessNumbersFixture fixture = new CustomerBusinessNumbersFixture();
    private org.springframework.boot.test.system.CapturedOutput output;
    @BeforeAll void start() throws Exception { fixture.start(null); fixture.web(); }
    @BeforeEach void seed(org.springframework.boot.test.system.CapturedOutput capturedOutput) throws Exception { output = capturedOutput; fixture.seed(); }
    @AfterAll void stop() { fixture.close(); }

    private List<UUID> walk(String token, int limit) throws Exception {
        List<UUID> result = new ArrayList<>(); String cursor = null;
        do {
            var page = fixture.json(token, "?limit=" + limit + (cursor == null ? "" : "&cursor=" + cursor), 200);
            var ids = fixture.ids(page); result.addAll(ids);
            boolean more = page.path("hasMore").asBoolean();
            assertThat(page.path("nextCursor").isNull()).isEqualTo(!more);
            cursor = more ? page.path("nextCursor").asString() : null;
            assertThat(result.size()).isLessThan(1000);
        } while (cursor != null);
        assertThat(new HashSet<>(result)).hasSize(result.size());
        return result;
    }
    private void forbidden(String token) throws Exception { assertThat(fixture.json(token, "", 403).path("code").asString()).isEqualTo("FORBIDDEN"); }
    private TrustedCustomerCaller caller() { return new TrustedCustomerCaller(fixture.account, TrustedCustomerCaller.Channel.WEB, new SelectedCustomerWorkspace(CustomerContext.WorkspaceType.PERSONAL, null)); }
    private CustomerBusinessNumberQueryService.Request request() { return new CustomerBusinessNumberQueryService.Request(Optional.empty(), Optional.of(100)); }
    private DSLContext dsl() { return fixture.context.getBean("customerQueryDsl", DSLContext.class); }

    @Test
    void postgres16FullChainAndCustomerRuntimeRoleAreUsed() throws Exception {
        try (var connection = fixture.admin()) {
            assertThat(Integer.parseInt(one(connection, "SHOW server_version_num"))).isBetween(160000, 169999);
            assertThat(one(connection, "SELECT count(*) FROM public.flyway_schema_history WHERE success")).isEqualTo("16");
            assertThat(one(connection, "SELECT rolinherit FROM pg_roles WHERE rolname='pa27_customer'")).isEqualTo("f");
        }
        fixture.json("personal", "", 200); fixture.cleanPool();
        assertThat(fixture.psql("check").getExitCode()).isZero();
    }

    @Test
    void personalScopeReturnsOnlyEveryOwnedBinding() throws Exception { assertThat(walk("personal", 7)).containsExactlyElementsOf(fixture.expected.get(fixture.account)); }

    @Test
    void organizationScopeIncludesEveryActiveRoleAndOnlyOrganizationOwnedSubjects() throws Exception {
        for (int index = 0; index < 4; index++) assertThat(walk("org" + index, 17)).containsExactlyElementsOf(fixture.expected.get(fixture.organizations.get(index)));
    }

    @Test
    void allBindingActivationMissingActivationAndRealNameCombinationsAreVisible() throws Exception {
        for (String token : List.of("personal", "org0", "org1", "org2", "org3")) {
            var page = fixture.json(token, "?limit=100", 200);
            Map<String, Integer> counts = new HashMap<>();
            for (JsonNode item : page.path("items")) counts.merge(item.path("bindingStatus").asString() + "|" + item.path("activationStatus").asString() + "|" + item.path("realNameStatus").asString() + "|" + item.path("numberType").asString(), 1, Integer::sum);
            assertThat(fixture.ids(page)).hasSize(48);
            for (String binding : List.of("ACTIVE", "DISABLED", "RETIRED")) for (String activation : List.of("INACTIVE", "ACTIVE", "SUSPENDED"))
                for (String realName : List.of("UNVERIFIED", "VERIFIED")) for (String type : List.of("MOBILE", "LANDLINE"))
                    assertThat(counts.get(binding + "|" + activation + "|" + realName + "|" + type)).isEqualTo(activation.equals("INACTIVE") ? 2 : 1);
        }
    }

    @Test
    void inactiveAndMissingAccountsAreForbiddenWithoutPageReads() throws Exception {
        fixture.session("missing", UUID.randomUUID(), null); forbidden("missing");
        try (var connection = fixture.admin()) { execute(connection, "SET ROLE customer_iam_owner"); execute(connection, "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account); }
        for (String token : List.of("personal", "org0", "org1", "org2", "org3")) forbidden(token);
    }

    @Test
    void inactiveMissingAndForeignOrganizationsOrMembershipsAreForbidden() throws Exception {
        forbidden("org4"); fixture.session("missing", fixture.account, UUID.randomUUID()); forbidden("missing");
        for (int index = 0; index < 4; index++) {
            UUID organization = fixture.organizations.get(index); String token = "org" + index;
            for (String status : List.of("SUSPENDED", "DISABLED")) {
                try (var connection = fixture.admin()) { execute(connection, "SET ROLE organization_access_owner"); execute(connection, "UPDATE organization_access.organization SET status=?,revision=revision+1 WHERE id=?", status, organization); }
                forbidden(token);
                try (var connection = fixture.admin()) { execute(connection, "SET ROLE organization_access_owner"); execute(connection, "UPDATE organization_access.organization SET status='ACTIVE',revision=revision+1 WHERE id=?", organization); }
            }
            for (String status : List.of("SUSPENDED", "REVOKED")) {
                try (var connection = fixture.admin()) { execute(connection, "SET ROLE organization_access_owner"); execute(connection, "UPDATE organization_access.organization_membership SET status=?,revision=revision+1 WHERE organization_id=? AND account_id=?", status, organization, fixture.account); }
                forbidden(token);
                if (!status.equals("REVOKED")) try (var connection = fixture.admin()) { execute(connection, "SET ROLE organization_access_owner"); execute(connection, "UPDATE organization_access.organization_membership SET status='ACTIVE',revision=revision+1 WHERE organization_id=? AND account_id=?", organization, fixture.account); }
            }
        }
    }

    @Test
    void unownedForeignAndOtherSelectedWorkspaceSubjectsNeverAppear() throws Exception {
        for (String token : List.of("personal", "org0", "org1", "org2", "org3")) {
            var ids = walk(token, 100);
            UUID key = token.equals("personal") ? fixture.account : fixture.organizations.get(Integer.parseInt(token.substring(3)));
            assertThat(ids).containsExactlyElementsOf(fixture.expected.get(key));
        }
    }

    @Test
    void subjectCreatedAtDescendingAndNativeUuidAscendingTiesAreExact() throws Exception {
        UUID newest = UUID.randomUUID();
        UUID subject = fixture.seedRow(fixture.account, null, "RETIRED", "MISSING", "UNVERIFIED", "LANDLINE", fixture.time.plusSeconds(1), newest, "NEW_MASK");
        UUID second = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        try (var connection = fixture.admin()) { execute(connection, "SET ROLE subject_number_owner"); execute(connection, "INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,status) VALUES (?,?,'MOBILE','v1.fixture.SECRET',?,'SECOND_MASK','DISABLED')", second, subject, "e".repeat(64)); }
        var expected = new ArrayList<UUID>(); expected.add(newest); expected.add(second); expected.addAll(fixture.expected.get(fixture.account));
        assertThat(walk("personal", 1)).containsExactlyElementsOf(expected);
    }

    @Test
    void emptySingleAndExactLimitPagesHaveExactTailSemantics() throws Exception {
        try (var connection = fixture.admin()) { execute(connection, "TRUNCATE subject_number.business_subject CASCADE"); }
        var empty = fixture.json("personal", "", 200); assertThat(fixture.ids(empty)).isEmpty(); assertThat(empty.path("nextCursor").isNull()).isTrue();
        fixture.seedRow(fixture.account, null, "DISABLED", "MISSING", "UNVERIFIED", "MOBILE", fixture.time, UUID.randomUUID(), "SINGLE");
        var single = fixture.json("personal", "?limit=1", 200); assertThat(fixture.ids(single)).hasSize(1); assertThat(single.path("hasMore").asBoolean()).isFalse();
        fixture.seedRow(fixture.account, null, "RETIRED", "SUSPENDED", "VERIFIED", "LANDLINE", fixture.time, UUID.randomUUID(), "SECOND");
        var exact = fixture.json("personal", "?limit=2", 200); assertThat(fixture.ids(exact)).hasSize(2); assertThat(exact.path("nextCursor").isNull()).isTrue();
        var first = fixture.json("personal", "?limit=1", 200);
        assertThat(first.path("hasMore").asBoolean()).isTrue();
        UUID anchor = fixture.ids(first).getFirst();
        String cursor = first.path("nextCursor").asString();
        try (var connection = fixture.admin()) { execute(connection, "DELETE FROM subject_number.caller_number_binding WHERE id<>?", anchor); }
        try (var connection = fixture.admin()) {
            try (var statement = connection.prepareStatement("SELECT count(*) FROM subject_number.caller_number_binding WHERE id=?")) {
                statement.setObject(1, anchor);
                try (var rows = statement.executeQuery()) { assertThat(rows.next()).isTrue(); assertThat(rows.getInt(1)).isEqualTo(1); }
            }
        }
        var emptyTail = fixture.json("personal", "?limit=1&cursor=" + cursor, 200);
        assertThat(fixture.ids(emptyTail)).isEmpty();
        assertThat(emptyTail.has("nextCursor")).isTrue();
        assertThat(emptyTail.path("nextCursor").isNull()).isTrue();
        assertThat(emptyTail.path("hasMore").asBoolean()).isFalse();
    }

    @Test
    void firstMiddleAndLastPagesEnumerateEveryBindingOnce() throws Exception { assertThat(walk("personal", 5)).containsExactlyElementsOf(fixture.expected.get(fixture.account)); }

    @Test
    void defaultOneAndHundredLimitsAndChangedContinuationLimitAreExact() throws Exception {
        for (int index = 0; index < 60; index++) fixture.seedRow(fixture.account, null, "RETIRED", "MISSING", "UNVERIFIED", "MOBILE", fixture.time.minusSeconds(1), UUID.randomUUID(), "EXTRA" + index);
        assertThat(fixture.ids(fixture.json("personal", "", 200))).hasSize(20);
        var first = fixture.json("personal", "?limit=1", 200); assertThat(fixture.ids(first)).hasSize(1);
        var second = fixture.json("personal", "?limit=100&cursor=" + first.path("nextCursor").asString(), 200); assertThat(fixture.ids(second)).hasSize(100); assertThat(second.path("hasMore").asBoolean()).isTrue();
        var last = fixture.json("personal", "?limit=20&cursor=" + second.path("nextCursor").asString(), 200); assertThat(fixture.ids(last)).hasSize(7); assertThat(last.path("nextCursor").isNull()).isTrue();
    }

    @Test
    void malformedTamperedAndCrossScopeCursorsCannotWidenResults() throws Exception {
        String cursor = fixture.json("personal", "?limit=1", 200).path("nextCursor").asString();
        byte[] bytes = Base64.getUrlDecoder().decode(cursor); bytes[92] ^= 1;
        for (String value : List.of("bad", "", cursor + "&cursor=" + cursor, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)))
            assertThat(fixture.json("personal", "?cursor=" + value, 400).path("code").asString()).isEqualTo("CURSOR_INVALID");
        for (String token : List.of("org0", "org1", "org2", "org3")) fixture.json(token, "?cursor=" + cursor, 400);
        fixture.session("other", fixture.other, null); fixture.json("other", "?cursor=" + cursor, 400);
    }

    @Test
    void committedLifecycleChangesAreRecheckedForEveryPage() throws Exception {
        String cursor = fixture.json("personal", "?limit=1", 200).path("nextCursor").asString();
        try (var connection = fixture.admin()) { execute(connection, "SET ROLE customer_iam_owner"); execute(connection, "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account); }
        assertThat(fixture.json("personal", "?cursor=" + cursor, 403).path("code").asString()).isEqualTo("FORBIDDEN");
    }

    @Test
    void lifecyclePageAndActivationShareOneRepeatableReadSnapshot() throws Exception {
        var entered = new CountDownLatch(1); var resume = new CountDownLatch(1);
        var facts = new CopyOnWriteArrayList<String>();
        Runnable capture = () -> facts.add(dsl().fetchOne("SELECT current_user||'|'||current_setting('transaction_read_only')||'|'||current_setting('transaction_isolation')||'|'||pg_backend_pid()||'|'||pg_current_snapshot()").get(0, String.class));
        var access = new JooqCustomerBusinessNumberAccessRepository(dsl()) {
            @Override public void requireActiveContext(Scope scope) {
                super.requireActiveContext(scope); capture.run(); entered.countDown();
                try { if (!resume.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out"); }
                catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(); }
            }
        };
        var reader = fixture.context.getBean("customerBusinessNumberReader", CustomerBusinessNumberReader.class);
        var activation = fixture.context.getBean("customerActivationStatusReader", CustomerActivationStatusReader.class);
        var service = new CustomerBusinessNumberQueryService(fixture.context.getBean("customerBusinessNumberTransactionTemplate", TransactionTemplate.class), dsl(), access,
                (scope, anchor, size) -> { capture.run(); return reader.fetch(scope, anchor, size); }, subjects -> { capture.run(); return activation.findStatuses(subjects); });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var future = executor.submit(() -> service.listCustomerBusinessNumbers(caller(), request()));
            try {
                assertThat(entered.await(30, TimeUnit.SECONDS)).isTrue();
                try (var connection = fixture.admin()) {
                    execute(connection, "SET ROLE customer_iam_owner"); execute(connection, "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account);
                    execute(connection, "SET ROLE activation_consent_owner"); execute(connection, "UPDATE activation_consent.service_activation SET status='SUSPENDED',revision=revision+1 WHERE status='ACTIVE'");
                }
            } finally { resume.countDown(); }
            var page = future.get(30, TimeUnit.SECONDS); assertThat(page.items()).hasSize(48); assertThat(page.items()).anyMatch(item -> item.activationStatus().equals("ACTIVE"));
        }
        assertThat(facts).hasSize(3).allMatch(value -> value.startsWith("customer_iam_role|on|repeatable read|"));
        assertThat(new HashSet<>(facts)).hasSize(1); forbidden("personal"); fixture.cleanPool();
    }

    @Test
    void concurrentInsertAndDeletedAnchorFollowDocumentedKeysetSemantics() throws Exception {
        var first = fixture.json("personal", "?limit=1", 200); UUID anchor = fixture.ids(first).getFirst(); String cursor = first.path("nextCursor").asString();
        try (var connection = fixture.admin()) { execute(connection, "DELETE FROM subject_number.caller_number_binding WHERE id=?", anchor); }
        UUID before = UUID.randomUUID(); fixture.seedRow(fixture.account, null, "RETIRED", "MISSING", "UNVERIFIED", "MOBILE", fixture.time.plusSeconds(1), before, "BEFORE");
        UUID after = UUID.randomUUID(); fixture.seedRow(fixture.account, null, "DISABLED", "MISSING", "UNVERIFIED", "MOBILE", fixture.time.minusSeconds(1), after, "AFTER");
        var ids = fixture.ids(fixture.json("personal", "?limit=100&cursor=" + cursor, 200));
        assertThat(ids).doesNotContain(anchor, before).contains(after); assertThat(ids.subList(0, 47)).containsExactlyElementsOf(fixture.expected.get(fixture.account).subList(1, 48));
    }

    @Test
    void successDenialAndFailurePerformZeroBusinessWrites() throws Exception {
        String before = fixture.businessSnapshot(); fixture.json("personal", "", 200); forbidden("org4"); fixture.json("personal", "?cursor=bad", 400);
        var service = new CustomerBusinessNumberQueryService(fixture.context.getBean("customerBusinessNumberTransactionTemplate", TransactionTemplate.class), dsl(),
                fixture.context.getBean("customerBusinessNumberAccessRepository", JooqCustomerBusinessNumberAccessRepository.class),
                (scope, anchor, size) -> { throw new org.jooq.exception.DataAccessException("DEPENDENCY_CANARY"); }, subjects -> Map.of());
        assertThatThrownBy(() -> service.listCustomerBusinessNumbers(caller(), request())).isInstanceOf(CustomerContextException.class);
        assertThat(fixture.businessSnapshot().equals(before)).isTrue(); fixture.cleanPool();
    }

    @Test
    void roleIsolationReadOnlyAndAutoCommitRestoreOnPooledConnectionReuse() throws Exception {
        for (int index = 0; index < 3; index++) { fixture.json("personal", "", 200); fixture.cleanPool(); forbidden("org4"); fixture.cleanPool(); }
        var service = new CustomerBusinessNumberQueryService(fixture.context.getBean("customerBusinessNumberTransactionTemplate", TransactionTemplate.class), dsl(),
                fixture.context.getBean("customerBusinessNumberAccessRepository", JooqCustomerBusinessNumberAccessRepository.class),
                (scope, anchor, size) -> { throw new IllegalStateException("FAILURE_CANARY"); }, subjects -> Map.of());
        assertThatThrownBy(() -> service.listCustomerBusinessNumbers(caller(), request())).isInstanceOf(CustomerContextException.class);
        fixture.cleanPool(); fixture.json("personal", "", 200); fixture.cleanPool();
    }

    @Test
    void responsesAndCapturedLogsNeverContainSecretOrOwnershipSentinels() throws Exception {
        for (String token : List.of("personal", "org0", "org1", "org2", "org3")) {
            var response = fixture.get(token, "?limit=100"); assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).doesNotContain(fixture.password, "CANARY", fixture.account.toString(), fixture.other.toString(), "FOREIGN", "UNOWNED", "number_hmac", "number_ciphertext");
        }
        String web = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/application-web.yml"));
        assertThat(web).contains("org.jooq: 'OFF'", "com.zaxxer.hikari: 'OFF'", "org.springframework.jdbc: 'OFF'", "log-request-details: false");
        assertThat(output.getAll()).doesNotContain(fixture.password, "CIPHERTEXT_CANARY", "number_hmac", "number_ciphertext", fixture.account.toString(), fixture.other.toString());
    }
}
