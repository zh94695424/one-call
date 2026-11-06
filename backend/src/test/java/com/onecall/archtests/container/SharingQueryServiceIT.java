package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.onecall.core.sharing.internal.application.*;
import com.onecall.core.sharing.internal.persistence.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

class SharingQueryServiceIT {
    private static SharingQueryFixture fixture;

    @BeforeAll
    static void start() throws Exception {
        ((Logger) LoggerFactory.getLogger("org.jooq")).setLevel(ch.qos.logback.classic.Level.OFF);
        ((Logger) LoggerFactory.getLogger("com.zaxxer.hikari")).setLevel(ch.qos.logback.classic.Level.OFF);
        fixture = new SharingQueryFixture();
        fixture.start();
    }

    @AfterAll
    static void stop() { if (fixture != null) { fixture.close(); } }

    @BeforeEach
    void seed() throws Exception { fixture.reset(); }

    @Test
    void queryRuntimeStartsAfterFixtureProvisioning() throws Exception {
        assertThat(fixture.flyway.info().applied()).hasSize(12);
        fixture.flyway.validate();
        assertThat(fixture.context.getBeansOfType(org.jooq.DSLContext.class)).hasSize(1);
        assertThat(fixture.context.getBeansOfType(javax.sql.DataSource.class)).hasSize(1);
        assertThat(fixture.context.getBeansOfType(TransactionTemplate.class)).hasSize(1);
        assertThat(fixture.context.getBeansOfType(ShareQueryRepository.class)).hasSize(1);
        assertThat(fixture.context.getBeansOfType(org.flywaydb.core.Flyway.class)).isEmpty();
        assertThat(fixture.pool).isInstanceOf(com.zaxxer.hikari.HikariDataSource.class);
        TransactionTemplate template = fixture.context.getBean(TransactionTemplate.class);
        assertThat(template.isReadOnly()).isTrue();
        assertThat(template.getIsolationLevel()).isEqualTo(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThat(template.getPropagationBehavior()).isEqualTo(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try (Connection admin = fixture.admin(); Statement statement = admin.createStatement(); ResultSet facts = statement.executeQuery(
                "select version(), datcollate, datctype, datistemplate, current_setting('TimeZone'), current_setting('server_version_num')::integer from pg_database where datname = current_database()")) {
            facts.next();
            assertThat(facts.getInt(6)).isBetween(160000, 169999);
            assertThat(facts.getString(2)).isEqualTo("C");
            assertThat(facts.getString(3)).isEqualTo("C");
            assertThat(facts.getBoolean(4)).isFalse();
            System.out.println("PA10 database facts: " + facts.getString(1) + "; collation=" + facts.getString(2)
                    + "; ctype=" + facts.getString(3) + "; template=false; sessionZone=" + facts.getString(5)
                    + "; JVM=" + TimeZone.getDefault().getID() + "; image=" + fixture.postgres.getDockerImageName()
                    + "; imageId=" + fixture.postgres.getContainerInfo().getImageId());
        }
        try (Connection connection = fixture.pool.getConnection(); Statement statement = connection.createStatement(); ResultSet facts = statement.executeQuery(
                "select session_user, current_user, rolsuper from pg_roles where rolname = session_user")) {
            facts.next();
            assertThat(facts.getString(1)).isEqualTo(fixture.login).isNotEqualTo("fixture_admin");
            assertThat(facts.getString(2)).isEqualTo(fixture.login);
            assertThat(facts.getBoolean(3)).isFalse();
        }
        fixture.page(fixture.personal(), null, null);
        assertOneTransaction();
        fixture.executions.clear(); fixture.acquisitions.set(0);
        fixture.page(fixture.organization(), null, null);
        assertOneTransaction();
    }

    @Test
    void personalListingIsCreatorScoped() throws Exception {
        fixture.grant("scope-A-personal", fixture.account, null, fixture.timestamp);
        fixture.grant("A-org", fixture.account, fixture.org, fixture.timestamp);
        fixture.grant("B-personal", fixture.other, null, fixture.timestamp);
        fixture.grant("B-org", fixture.other, fixture.org, fixture.timestamp);
        assertThat(fixture.page(fixture.personal(), null, null).items()).extracting(ShareSummary::shareId).containsExactly("scope-A-personal");
        assertOneTransaction();
    }

    @Test
    void organizationListingIsCreatorAndWorkspaceScoped() throws Exception {
        fixture.grant("A-O1", fixture.account, fixture.org, fixture.timestamp);
        fixture.grant("B-O1", fixture.other, fixture.org, fixture.timestamp);
        fixture.grant("A-O2", fixture.account, fixture.org2, fixture.timestamp);
        fixture.grant("A-personal", fixture.account, null, fixture.timestamp);
        for (String role : List.of("ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR")) {
            fixture.update("organization_access.organization_membership", fixture.membership, "role", role);
            assertThat(fixture.page(fixture.organization(), null, null).items()).extracting(ShareSummary::shareId).containsExactly("A-O1");
        }
    }

    @Test
    void inactiveAndMissingCreatorContextsFailClosed() throws Exception {
        denied(new TrustedShareCaller(UUID.randomUUID(), Optional.empty()));
        denied(new TrustedShareCaller(fixture.account, Optional.of(UUID.randomUUID())));
        for (String state : List.of("SUSPENDED", "DISABLED")) {
            fixture.update("organization_access.organization", fixture.org, "status", state);
            denied(fixture.organization());
            fixture.page(fixture.personal(), null, null);
            fixture.update("organization_access.organization", fixture.org, "status", "ACTIVE");
        }
        fixture.update("customer_iam.customer_account", fixture.account, "status", "DISABLED");
        denied(fixture.personal()); denied(fixture.organization());
        fixture.update("customer_iam.customer_account", fixture.account, "status", "ACTIVE");
        UUID absentOrg;
        try (Connection admin = fixture.admin()) {
            absentOrg = UUID.randomUUID();
            SharingQueryFixture.execute(admin, "INSERT INTO organization_access.organization(id, display_name) VALUES (?, 'absent member')", absentOrg);
        }
        denied(new TrustedShareCaller(fixture.account, Optional.of(absentOrg)));
        for (String state : List.of("SUSPENDED", "REVOKED")) {
            fixture.update("organization_access.organization_membership", fixture.membership, "status", state);
            denied(fixture.organization());
            if (state.equals("SUSPENDED")) {
                fixture.update("organization_access.organization_membership", fixture.membership, "status", "ACTIVE");
            }
        }
        try (Connection admin = fixture.admin()) {
            SharingQueryFixture.execute(admin, "INSERT INTO organization_access.organization_membership(id, organization_id, account_id, role) VALUES (?, ?, ?, 'AUDITOR')",
                    UUID.randomUUID(), fixture.org, fixture.account);
        }
        fixture.page(fixture.organization(), null, null);
    }

    @Test
    void keysetTiesAndConcurrentInsertRemainStable() throws Exception {
        for (String id : List.of("Z", "a", "A", "z", "t-10", "t-2", "é", "Ω")) {
            fixture.grant(id, fixture.account, null, fixture.timestamp);
        }
        fixture.grant("micro-newer", fixture.account, null, fixture.timestamp.plusNanos(1000));
        fixture.grant("micro-older", fixture.account, null, fixture.timestamp.minusNanos(1000));
        List<String> baseline = fixture.baseline(fixture.account, null);
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai")); fixture.sessionZone = "America/New_York";
            List<ShareCursor> nonUtcAnchors = new ArrayList<>();
            assertThat(traverse(fixture.personal(), 2, null, nonUtcAnchors)).containsExactlyElementsOf(baseline);
            TimeZone.setDefault(TimeZone.getTimeZone("UTC")); fixture.sessionZone = "UTC";
            List<ShareCursor> utcAnchors = new ArrayList<>();
            assertThat(traverse(fixture.personal(), 2, null, utcAnchors)).containsExactlyElementsOf(baseline);
            assertThat(utcAnchors).containsExactlyElementsOf(nonUtcAnchors);
            assertThat(utcAnchors).extracting(ShareCursor::validFrom).contains(fixture.timestamp);
            System.out.println("PA10 ordering: C database-native baseline=" + baseline.size()
                    + "; microsecond anchors exact; JVM Asia/Shanghai/session America/New_York equals UTC/UTC");
        } finally { TimeZone.setDefault(original); fixture.sessionZone = "UTC"; }
        SharePage first = fixture.page(fixture.personal(), 2, null);
        ShareCursor anchor = first.nextCursor().orElseThrow();
        fixture.grant("newer-between-pages", fixture.account, null, fixture.timestamp.plusSeconds(1));
        List<String> candidates;
        try (Connection admin = fixture.admin(); PreparedStatement statement = admin.prepareStatement(
                "select value from (values (?::text), ('~~~~'), ('0000'), ('AAAA')) candidates(value) where value > ? order by value desc")) {
            statement.setString(1, anchor.shareId() + "-above");
            statement.setString(2, anchor.shareId());
            try (ResultSet rows = statement.executeQuery()) {
                candidates = new ArrayList<>(); while (rows.next()) { candidates.add(rows.getString(1)); }
            }
        }
        assertThat(candidates).isNotEmpty();
        fixture.grant(candidates.getFirst(), fixture.account, null, anchor.validFrom());
        List<String> traversed = new ArrayList<>(first.items().stream().map(ShareSummary::shareId).toList());
        traversed.addAll(traverse(fixture.personal(), 2, anchor, new ArrayList<>()));
        assertThat(traversed).containsExactlyElementsOf(baseline).doesNotHaveDuplicates();
    }

    @Test
    void pageLimitsEmptyAndTailStatesAreExact() throws Exception {
        SharePage empty = fixture.page(fixture.personal(), null, null);
        assertThat(empty.items()).isEmpty(); assertThat(empty.nextCursor()).isEmpty(); assertThat(empty.hasMore()).isFalse();
        for (int index = 0; index < 101; index++) { fixture.grant("limit-" + index, fixture.account, null, fixture.timestamp); }
        SharePage defaults = fixture.page(fixture.personal(), null, null);
        assertThat(defaults.items()).hasSize(20); assertThat(defaults.hasMore()).isTrue();
        SharePage one = fixture.page(fixture.personal(), 1, null);
        assertThat(one.items()).hasSize(1); assertThat(one.hasMore()).isTrue();
        SharePage hundred = fixture.page(fixture.personal(), 100, null);
        assertThat(hundred.items()).hasSize(100); assertThat(hundred.hasMore()).isTrue();
        SharePage tail = fixture.page(fixture.personal(), 1, hundred.nextCursor().orElseThrow());
        assertThat(tail.items()).hasSize(1); assertThat(tail.hasMore()).isFalse(); assertThat(tail.nextCursor()).isEmpty();
        assertThat(traverse(fixture.personal(), 20, null, new ArrayList<>())).hasSize(101).doesNotHaveDuplicates();
        for (int invalid : List.of(0, -1, 101)) {
            fixture.executions.clear();
            failure(ShareQueryException.Code.VALIDATION_FAILED, () -> fixture.page(fixture.personal(), invalid, null));
            assertThat(fixture.executions).isEmpty();
        }
    }

    @Test
    void invalidAndCrossContextCursorsNeverWidenScope() throws Exception {
        fixture.grant("legal-'?;--", fixture.account, null, fixture.timestamp);
        fixture.grant("older", fixture.account, null, fixture.timestamp.minusSeconds(1));
        ShareCursor anchor = fixture.page(fixture.personal(), 1, null).nextCursor().orElseThrow();
        for (TrustedShareCaller caller : List.of(fixture.organization(), new TrustedShareCaller(fixture.other, Optional.empty()),
                new TrustedShareCaller(fixture.account, Optional.of(fixture.org2)))) {
            fixture.executions.clear();
            failure(ShareQueryException.Code.CURSOR_INVALID, () -> fixture.page(caller, 1, anchor));
            assertThat(fixture.executions).isEmpty();
        }
        fixture.executions.clear();
        for (ShareCursor invalid : List.of(new ShareCursor(fixture.account, Optional.empty(), null, "id"),
                new ShareCursor(fixture.account, Optional.empty(), fixture.timestamp.plusNanos(1), "id"),
                new ShareCursor(fixture.account, Optional.empty(), Instant.MAX, "id"),
                new ShareCursor(fixture.account, Optional.empty(), fixture.timestamp, " "),
                new ShareCursor(fixture.account, Optional.empty(), fixture.timestamp, "x".repeat(129)))) {
            failure(ShareQueryException.Code.CURSOR_INVALID, () -> fixture.page(fixture.personal(), 1, invalid));
        }
        assertThat(fixture.executions).isEmpty();
        assertThat(fixture.page(fixture.personal(), 1, anchor).items()).extracting(ShareSummary::shareId).containsExactly("older");
        ShareCursor missing = new ShareCursor(fixture.account, Optional.empty(), fixture.timestamp.plusSeconds(1), "nonexistent");
        assertThat(fixture.page(fixture.personal(), 100, missing).items()).hasSize(2);
        assertThat(fixture.executions).allSatisfy(execution -> assertThat(execution.sql()).doesNotContain("legal-'?;--", "nonexistent"));
        ShareCursor orgAnchor = new ShareCursor(fixture.account, Optional.of(fixture.org), fixture.timestamp, "id");
        failure(ShareQueryException.Code.CURSOR_INVALID, () -> fixture.page(fixture.personal(), 1, orgAnchor));
    }

    @Test
    void listingProjectionNeverReadsOrReturnsSecrets() throws Exception {
        for (int index = 0; index < 4; index++) {
            String mode = ShareSummary.VerificationMode.values()[index].name();
            String status = ShareSummary.ShareStatus.values()[index].name();
            fixture.grant("org-mode-" + index, fixture.account, fixture.org, fixture.timestamp, mode, status, index % 2 == 0 ? null : 7);
            fixture.grant("personal-mode-" + index, fixture.account, null, fixture.timestamp, "NONE", status, null);
        }
        List<String> before = fingerprints();
        fixture.executions.clear();
        SharePage personal = fixture.page(fixture.personal(), 1, null);
        fixture.page(fixture.personal(), 1, personal.nextCursor().orElseThrow());
        SharePage organization = fixture.page(fixture.organization(), 1, null);
        fixture.page(fixture.organization(), 1, organization.nextCursor().orElseThrow());
        List<SharingQueryFixture.Execution> grants = fixture.executions.stream().filter(execution -> execution.sql().contains("evidence_share_grant")).toList();
        assertThat(grants).extracting(SharingQueryFixture.Execution::parameterCount).containsExactly(2, 5, 3, 6);
        for (var execution : grants) {
            String normalized = execution.sql().replace("\"", "").toLowerCase(Locale.ROOT);
            assertThat(normalized.substring(0, normalized.indexOf(" from "))).isEqualTo("select " + SharingQueryFixture.COLUMNS);
            assertThat(normalized).contains("from sharing.evidence_share_grant", "created_by_account_id =", "order by valid_from desc, share_id desc");
            assertThat(execution.indexes()).containsExactlyInAnyOrderElementsOf(java.util.stream.IntStream.rangeClosed(1, execution.parameterCount()).boxed().toList());
        }
        for (var execution : fixture.executions) {
            String normalized = execution.sql().replace("\"", "").toLowerCase(Locale.ROOT);
            assertThat(normalized).doesNotContain("*", "snapshot", "hash", "salt", "evidence.evidence", "share_access_event", "for update", "for share", "advisory", " offset ", " join ");
            assertThat(normalized.startsWith("select " + SharingQueryFixture.COLUMNS + " from sharing.evidence_share_grant where ")
                    || normalized.startsWith("select id, status from customer_iam.customer_account where ")
                    || normalized.startsWith("select id, status from organization_access.organization where ")
                    || normalized.startsWith("select organization_id, account_id, status from organization_access.organization_membership where ")
                    || normalized.equals("set local role sharing_role") || normalized.equals("select current_user"))
                    .as("executed statements stay within the exact role/lifecycle/projection allowlist").isTrue();
        }
        SharePage all = fixture.page(fixture.organization(), 100, null);
        assertThat(all.items()).extracting(ShareSummary::verificationMode).containsExactlyInAnyOrder(ShareSummary.VerificationMode.values());
        assertThat(all.items()).extracting(ShareSummary::status).containsExactlyInAnyOrder(ShareSummary.ShareStatus.values());
        assertThat(all.items()).extracting(ShareSummary::maxAdmissions).containsNull().contains(7);
        assertThat(all.toString()).doesNotContain(SharingQueryFixture.SECRET, fixture.password, fixture.account.toString(), fixture.org.toString(), "b".repeat(64));
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>(); appender.start(); logger.addAppender(appender);
        try {
            fixture.failOn = "set local role"; fixture.failState = "42501";
            failure(ShareQueryException.Code.SERVICE_UNAVAILABLE, () -> fixture.page(fixture.personal(), null, null));
            fixture.failOn = "evidence_share_grant"; fixture.failState = "08006";
            failure(ShareQueryException.Code.SERVICE_UNAVAILABLE, () -> fixture.page(fixture.personal(), null, null));
            fixture.failOn = null;
            fixture.corruptMapping = true;
            failure(ShareQueryException.Code.INTERNAL, () -> fixture.page(fixture.personal(), null, null));
            fixture.nullMapping = true;
            failure(ShareQueryException.Code.INTERNAL, () -> fixture.page(fixture.personal(), null, null));
            assertThat(appender.list).hasSize(4);
            assertThat((ShareQueryDiagnostic) appender.list.get(0).getArgumentArray()[0])
                    .extracting(ShareQueryDiagnostic::sqlState, ShareQueryDiagnostic::category, ShareQueryDiagnostic::roleAssertionPassed)
                    .containsExactly(Optional.of("42501"), "42", false);
            assertThat((ShareQueryDiagnostic) appender.list.get(1).getArgumentArray()[0])
                    .extracting(ShareQueryDiagnostic::sqlState, ShareQueryDiagnostic::category, ShareQueryDiagnostic::roleAssertionPassed)
                    .containsExactly(Optional.of("08006"), "08", true);
            for (var event : appender.list) {
                assertThat(event.getThrowableProxy()).isNull();
                assertThat(event.getFormattedMessage()).doesNotContain(SharingQueryFixture.SECRET, fixture.password, fixture.account.toString(), "select ", "password");
                ShareQueryDiagnostic diagnostic = (ShareQueryDiagnostic) event.getArgumentArray()[0];
                assertThat(diagnostic.operation()).isEqualTo("listCustomerShares");
                assertThat(diagnostic.exceptionClass()).isNotBlank();
            }
        } finally { fixture.failOn = null; fixture.corruptMapping = false; fixture.nullMapping = false;
            logger.detachAppender(appender); appender.stop(); }
        assertThat(fingerprints()).containsExactlyElementsOf(before);
        System.out.println("PA10 executed projection: exact 11 ordered columns; personal/org first/anchor parameters=2/3/5/6; no values captured");
    }

    @Test
    void runtimeRoleAndPoolCleanupHoldOnSuccessAndFailure() throws Exception {
        fixture.grant("role", fixture.account, null, fixture.timestamp);
        try (Connection denied = DriverManager.getConnection(fixture.url, fixture.deniedLogin, fixture.password)) {
            denied.setAutoCommit(false);
            assertSqlState("42501", () -> SharingQueryFixture.execute(denied, "SET LOCAL ROLE sharing_role"));
        }
        try (Connection admin = fixture.admin()) {
            SharingQueryFixture.execute(admin, "REVOKE sharing_role FROM " + fixture.login);
            try {
                failure(ShareQueryException.Code.SERVICE_UNAVAILABLE, () -> fixture.page(fixture.personal(), 1, null));
                assertThat(grantExecutions()).isEmpty();
            } finally { SharingQueryFixture.execute(admin, "GRANT sharing_role TO " + fixture.login); }
        }
        for (String probe : List.of("select display_name from customer_iam.customer_account",
                "update customer_iam.customer_account set display_name = 'blocked'", "create table sharing.unapproved(id int)",
                "select id from customer_iam.customer_account for share", "select id from customer_iam.customer_account for update",
                "select snapshot from evidence.evidence_record",
                "update sharing.evidence_share_grant set revision = revision + 1")) {
            try (Connection connection = fixture.pool.getConnection()) {
                connection.setAutoCommit(false);
                SharingQueryFixture.execute(connection, "SET LOCAL ROLE sharing_role");
                assertSqlState("42501", () -> SharingQueryFixture.execute(connection, probe));
                connection.rollback();
            }
        }
        int commitsBefore = fixture.commits.get();
        fixture.page(fixture.personal(), 1, null);
        assertThat(fixture.commits.get()).isEqualTo(commitsBefore + 1);
        int pid = grantExecutions().getLast().pid();
        int rollbacksBefore = fixture.rollbacks.get();
        fixture.tryWrite = true;
        failure(ShareQueryException.Code.SERVICE_UNAVAILABLE, () -> fixture.page(fixture.personal(), 1, null));
        assertThat(fixture.writeState).isEqualTo("25006");
        assertThat(fixture.rollbacks.get()).isEqualTo(rollbacksBefore + 1);
        assertThat(fixture.commits.get()).isEqualTo(commitsBefore + 1);
        fixture.tryWrite = false;
        fixture.page(fixture.personal(), 1, null);
        assertThat(grantExecutions()).allSatisfy(execution -> {
            assertThat(execution.effectiveRole()).isEqualTo("sharing_role"); assertThat(execution.readOnly()).isEqualTo("on");
            assertThat(execution.isolation()).isEqualTo("read committed"); assertThat(execution.pid()).isEqualTo(pid);
        });
        assertCleanPool(pid);
        TransactionTemplate outer = new TransactionTemplate(fixture.context.getBean(org.springframework.jdbc.datasource.DataSourceTransactionManager.class));
        int acquisitionsBefore = fixture.acquisitions.get();
        fixture.pool.setMaximumPoolSize(2);
        try {
            outer.executeWithoutResult(status -> {
                try {
                    org.jooq.DSLContext context = fixture.context.getBean(org.jooq.DSLContext.class);
                    context.execute("SET LOCAL ROLE sharing_role");
                    assertThat(context.fetchOne("select current_setting('transaction_read_only')").get(0, String.class)).isEqualTo("off");
                    int outerPid = context.fetchOne("select pg_backend_pid()").get(0, Integer.class);
                    assertThat(fixture.acquisitions.get()).isEqualTo(acquisitionsBefore + 1);
                    int listingsBefore = grantExecutions().size();
                    fixture.page(fixture.personal(), 1, null);
                    assertThat(grantExecutions().subList(listingsBefore, grantExecutions().size())).singleElement().satisfies(execution -> {
                        assertThat(execution.effectiveRole()).isEqualTo("sharing_role");
                        assertThat(execution.readOnly()).isEqualTo("on");
                        assertThat(execution.isolation()).isEqualTo("read committed");
                        assertThat(execution.pid()).isNotEqualTo(outerPid);
                        assertThat(execution.checkout()).isEqualTo(acquisitionsBefore + 2);
                    });
                    assertThat(fixture.acquisitions.get()).isEqualTo(acquisitionsBefore + 2);
                    assertThat(context.fetchOne("select current_setting('transaction_read_only')").get(0, String.class)).isEqualTo("off");
                } finally { status.setRollbackOnly(); }
            });
        } finally { fixture.pool.setMaximumPoolSize(1); }
    }

    @Test
    void committedLifecycleChangesDenyNextQueryAndFailuresRollBack() throws Exception {
        fixture.grant("lifecycle", fixture.account, fixture.org, fixture.timestamp);
        List<Object[]> variants = List.of(new Object[]{"customer_iam.customer_account", fixture.account, "DISABLED"},
                new Object[]{"organization_access.organization", fixture.org, "SUSPENDED"},
                new Object[]{"organization_access.organization_membership", fixture.membership, "SUSPENDED"});
        for (Object[] variant : variants) {
            fixture.page(fixture.organization(), null, null);
            fixture.update((String) variant[0], (UUID) variant[1], "status", (String) variant[2]);
            denied(fixture.organization());
            fixture.update((String) variant[0], (UUID) variant[1], "status", "ACTIVE");
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CountDownLatch updated = new CountDownLatch(1); CountDownLatch release = new CountDownLatch(1);
            try {
                Future<?> writer = executor.submit(() -> {
                    try (Connection admin = fixture.admin()) {
                        admin.setAutoCommit(false);
                        fixture.update(admin, (String) variant[0], (UUID) variant[1], "status", (String) variant[2]);
                        updated.countDown();
                        if (!release.await(10, TimeUnit.SECONDS)) { throw new AssertionError("commit barrier deadline"); }
                        admin.commit();
                    } catch (Exception failure) { throw new RuntimeException(failure); }
                });
                assertThat(updated.await(10, TimeUnit.SECONDS)).isTrue();
                assertThat(fixture.page(fixture.organization(), null, null).items()).hasSize(1);
                release.countDown(); writer.get(10, TimeUnit.SECONDS);
                denied(fixture.organization());
            } finally { release.countDown(); executor.shutdownNow(); }
            fixture.update((String) variant[0], (UUID) variant[1], "status", "ACTIVE");
            try (Connection admin = fixture.admin()) {
                admin.setAutoCommit(false);
                fixture.update(admin, (String) variant[0], (UUID) variant[1], "status", (String) variant[2]);
                fixture.page(fixture.organization(), null, null);
                admin.rollback();
            }
            fixture.page(fixture.organization(), null, null);
        }
        List<String> before = fingerprints();
        int rollbacksBefore = fixture.rollbacks.get();
        fixture.failOn = "evidence_share_grant"; fixture.failState = "08006";
        failure(ShareQueryException.Code.SERVICE_UNAVAILABLE, () -> fixture.page(fixture.organization(), null, null));
        assertThat(fixture.rollbacks.get()).isEqualTo(rollbacksBefore + 1);
        fixture.failOn = null;
        fixture.page(fixture.organization(), null, null);
        assertCleanPool(grantExecutions().getLast().pid());
        assertThat(fingerprints()).containsExactlyElementsOf(before);
    }

    private List<SharingQueryFixture.Execution> grantExecutions() {
        return fixture.executions.stream().filter(execution -> execution.sql().contains("evidence_share_grant")).toList();
    }

    private void assertOneTransaction() {
        assertThat(fixture.acquisitions.get()).isEqualTo(1);
        assertThat(fixture.executions).isNotEmpty();
        assertThat(fixture.executions.getFirst().sql()).isEqualTo("SET LOCAL ROLE sharing_role");
        assertThat(fixture.executions.stream().map(SharingQueryFixture.Execution::checkout).distinct()).hasSize(1);
        assertThat(grantExecutions()).allSatisfy(execution -> {
            assertThat(execution.effectiveRole()).isEqualTo("sharing_role"); assertThat(execution.readOnly()).isEqualTo("on");
            assertThat(execution.isolation()).isEqualTo("read committed");
        });
    }

    private void assertCleanPool(int pid) throws Exception {
        try (Connection connection = fixture.pool.getConnection(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(
                "select current_user, current_setting('transaction_read_only'), pg_backend_pid()")) {
            rows.next(); assertThat(rows.getString(1)).isEqualTo(fixture.login);
            assertThat(rows.getString(2)).isEqualTo("off"); assertThat(rows.getInt(3)).isEqualTo(pid);
        }
    }

    private List<String> traverse(TrustedShareCaller caller, int size, ShareCursor start, List<ShareCursor> anchors) {
        List<String> result = new ArrayList<>(); ShareCursor cursor = start;
        for (int pageNumber = 0; pageNumber < 200; pageNumber++) {
            SharePage page = fixture.page(caller, size, cursor);
            result.addAll(page.items().stream().map(ShareSummary::shareId).toList());
            if (!page.hasMore()) { return result; }
            cursor = page.nextCursor().orElseThrow(); anchors.add(cursor);
        }
        throw new AssertionError("Non-terminating keyset traversal");
    }

    private void denied(TrustedShareCaller caller) {
        fixture.executions.clear();
        failure(ShareQueryException.Code.FORBIDDEN, () -> fixture.page(caller, null, null));
        assertThat(grantExecutions()).isEmpty();
    }

    private void failure(ShareQueryException.Code code, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ShareQueryException.class, exception -> {
            assertThat(exception.code()).isEqualTo(code); assertThat(exception.getCause()).isNull();
            assertThat(exception.getMessage()).isEqualTo(new ShareQueryException(code).getMessage())
                    .doesNotContain(SharingQueryFixture.SECRET, fixture.password);
        });
    }

    private void assertSqlState(String state, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo(state));
    }

    private List<String> fingerprints() throws Exception {
        List<String> result = new ArrayList<>();
        try (Connection admin = fixture.admin(); Statement statement = admin.createStatement()) {
            for (String table : List.of("sharing.evidence_share_grant", "sharing.share_admission", "sharing.share_access_event",
                    "customer_iam.customer_account", "organization_access.organization", "organization_access.organization_membership",
                    "evidence.evidence_record", "evidence.evidence_event", "platform.outbox_event", "audit.audit_fact")) {
                try (ResultSet rows = statement.executeQuery("select md5(coalesce(string_agg(row_to_json(facts)::text, ',' order by row_to_json(facts)::text), '')) from " + table + " facts")) {
                    rows.next(); result.add(table + ":" + rows.getString(1));
                }
            }
        }
        return result;
    }
}
