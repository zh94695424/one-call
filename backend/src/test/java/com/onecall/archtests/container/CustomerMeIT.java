package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.sharing.internal.application.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.*;

class CustomerMeIT {
    private static CustomerMeFixture fixture;

    @BeforeAll
    static void start() throws Exception {
        fixture = new CustomerMeFixture();
        try { fixture.start(); } catch (Exception | AssertionError failure) { fixture.close(); throw failure; }
    }

    @AfterAll
    static void close() { if (fixture != null) { fixture.close(); } }

    @BeforeEach
    void reset() throws Exception { fixture.reset(); }

    private void oneTransaction() {
        assertThat(fixture.acquisitions).hasValue(1);
        assertThat(fixture.commits).hasValue(1);
        assertThat(fixture.executions).hasSize(3);
        assertThat(fixture.executions.get(0).sql()).isEqualTo("SET LOCAL ROLE customer_iam_role");
        assertThat(fixture.executions.get(1).sql()).isEqualTo("select current_user");
        var query = fixture.executions.getLast();
        assertThat(query.role()).isEqualTo("customer_iam_role");
        assertThat(query.session()).isEqualTo(fixture.login);
        assertThat(query.readOnly()).isEqualTo("on");
        assertThat(query.isolation()).isEqualTo("read committed");
        assertThat(query.bindings()).containsExactly("ACTIVE", "ACTIVE", fixture.account, "ACTIVE");
        assertThat(fixture.executions.stream().map(CustomerMeFixture.Execution::checkout).distinct()).hasSize(1);
        assertThat(fixture.executions.stream().map(CustomerMeFixture.Execution::pid).distinct()).hasSize(1);
    }

    @Test
    void webRuntimeReadsMigratedDatabaseUnderCustomerRole() throws Exception {
        var json = fixture.json(fixture.get("first"), 200);
        assertThat(json.get("account").get("accountId").asString()).isEqualTo(fixture.account.toString());
        oneTransaction();
        try (var connection = fixture.runtime(); var statement = connection.createStatement();
                var facts = statement.executeQuery("select session_user, current_user, rolsuper, rolinherit from pg_roles where rolname=session_user")) {
            facts.next();
            assertThat(facts.getString(1)).isEqualTo(fixture.login);
            assertThat(facts.getString(2)).isEqualTo(fixture.login);
            assertThat(facts.getBoolean(3)).isFalse();
            assertThat(facts.getBoolean(4)).isFalse();
        }
    }

    @Test
    void personalContextRequiresCurrentActiveAccount() throws Exception {
        var json = fixture.json(fixture.get("first"), 200);
        assertThat(json.get("workspaces").size()).isEqualTo(1);
        assertThat(json.get("workspaces").get(0).propertyNames()).containsExactlyInAnyOrder("workspaceType", "status");
        fixture.update("customer_iam_owner", "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account);
        assertThat(fixture.json(fixture.get("first"), 401).get("code").asString()).isEqualTo("UNAUTHENTICATED");
        CustomerMeFixture.SESSIONS.put("missing", new com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession(UUID.randomUUID(),
                com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel.WEB, "onecall:customer-session:v1"));
        fixture.json(fixture.get("missing"), 401);
    }

    @Test
    void onlyActiveMembershipsUnderActiveOrganizationsAreProjected() throws Exception {
        for (String member : List.of("ACTIVE", "SUSPENDED", "REVOKED")) {
            for (String organization : List.of("ACTIVE", "SUSPENDED", "DISABLED")) {
                fixture.organization(UUID.randomUUID(), organization, "MEMBER", member, fixture.account);
            }
        }
        for (String role : List.of("ADMINISTRATOR", "EVIDENCE_MANAGER", "AUDITOR")) {
            fixture.organization(UUID.randomUUID(), "ACTIVE", role, "ACTIVE", fixture.account);
        }
        fixture.organization(UUID.randomUUID(), "ACTIVE", "ADMINISTRATOR", "ACTIVE", fixture.other);
        var rows = fixture.json(fixture.get("first"), 200).get("workspaces");
        assertThat(rows.size()).isEqualTo(5);
        assertThat(java.util.stream.IntStream.range(1, rows.size()).mapToObj(index -> rows.get(index).get("role").asString()))
                .containsExactlyInAnyOrder("MEMBER", "ADMINISTRATOR", "EVIDENCE_MANAGER", "AUDITOR");
        UUID history = UUID.randomUUID();
        fixture.organization(history, "ACTIVE", "MEMBER", "REVOKED", fixture.account);
        fixture.update("organization_access_owner", "INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role) VALUES (?, ?, ?, 'AUDITOR')",
                UUID.randomUUID(), history, fixture.account);
        assertThat(fixture.json(fixture.get("first"), 200).get("workspaces").size()).isEqualTo(6);
    }

    @Test
    void workspaceOrderingIsUuidDeterministicAndPersonalFirst() throws Exception {
        var ordered = List.of("7fffffff-ffff-ffff-7fff-ffffffffffff", "7fffffff-ffff-ffff-8000-000000000000",
                "80000000-0000-0000-0000-000000000000", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        var shuffled = new java.util.ArrayList<>(ordered);
        java.util.Collections.shuffle(shuffled);
        for (String id : shuffled) {
            fixture.organization(UUID.fromString(id), "ACTIVE", "MEMBER", "ACTIVE", fixture.account);
            fixture.update("organization_access_owner", "UPDATE organization_access.organization SET display_name=?,revision=revision+1 WHERE id=?",
                    "Name-" + (ordered.size() - ordered.indexOf(id)), UUID.fromString(id));
        }
        for (int iteration = 0; iteration < 3; iteration++) {
            var rows = fixture.json(fixture.get("first"), 200).get("workspaces");
            assertThat(rows.get(0).get("workspaceType").asString()).isEqualTo("PERSONAL");
            assertThat(java.util.stream.IntStream.range(1, rows.size()).mapToObj(index -> rows.get(index).get("organizationId").asString()))
                    .containsExactlyElementsOf(ordered);
        }
    }

    @Test
    void exactGrantsPermitProjectionAndRejectForeignColumnsAndLocks() throws Exception {
        fixture.organization(UUID.randomUUID(), "ACTIVE", "MEMBER", "ACTIVE", fixture.account);
        try (Connection connection = fixture.runtime()) {
            CustomerMeFixture.execute(connection, "SET ROLE customer_iam_role");
            for (String sql : List.of("SELECT id,display_name,status FROM organization_access.organization",
                    "SELECT organization_id,account_id,role,status FROM organization_access.organization_membership")) {
                CustomerMeFixture.execute(connection, sql);
            }
            for (String sql : List.of("SELECT revision FROM organization_access.organization", "SELECT created_at FROM organization_access.organization",
                    "SELECT id FROM organization_access.organization_membership", "SELECT department_label FROM organization_access.organization_membership",
                    "SELECT revision FROM organization_access.organization_membership", "UPDATE organization_access.organization SET revision=revision+1",
                    "DELETE FROM organization_access.organization", "UPDATE organization_access.organization_membership SET role='AUDITOR'",
                    "SELECT id FROM organization_access.organization FOR UPDATE", "SELECT account_id FROM organization_access.organization_membership FOR SHARE")) {
                assertThatThrownBy(() -> CustomerMeFixture.execute(connection, sql)).isInstanceOfSatisfying(SQLException.class,
                        denied -> assertThat(denied.getSQLState()).isEqualTo("42501"));
            }
        }
        try (var connection = fixture.admin()) {
            CustomerMeFixture.execute(connection, "REVOKE SELECT (display_name) ON organization_access.organization FROM customer_iam_role");
            try { fixture.json(fixture.get("first"), 503); }
            finally { CustomerMeFixture.execute(connection, "GRANT SELECT (display_name) ON organization_access.organization TO customer_iam_role"); }
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "select has_column_privilege('customer_iam_role','organization_access.organization','display_name','SELECT'), has_column_privilege('customer_iam_role','organization_access.organization','revision','SELECT')")) {
                rows.next(); assertThat(rows.getBoolean(1)).isTrue(); assertThat(rows.getBoolean(2)).isFalse();
            }
            for (String table : List.of("organization", "organization_membership")) {
                var permitted = new java.util.HashSet<String>();
                try (var statement = connection.prepareStatement("select attname from pg_attribute where attrelid=cast(? as regclass) and attnum>0 and not attisdropped and has_column_privilege('customer_iam_role',attrelid,attname,'SELECT')")) {
                    statement.setString(1, "organization_access." + table);
                    try (var rows = statement.executeQuery()) { while (rows.next()) { permitted.add(rows.getString(1)); } }
                }
                assertThat(permitted).containsExactlyInAnyOrderElementsOf(table.equals("organization")
                        ? List.of("id", "display_name", "status") : List.of("organization_id", "account_id", "role", "status"));
            }
        }
        fixture.json(fixture.get("first"), 200);
    }

    @Test
    void committedLifecycleChangesAreObservedOnNextRequest() throws Exception {
        UUID organization = UUID.randomUUID();
        fixture.organization(organization, "ACTIVE", "MEMBER", "ACTIVE", fixture.account);
        assertThat(fixture.json(fixture.get("first"), 200).get("workspaces").size()).isEqualTo(2);
        for (String status : List.of("SUSPENDED", "ACTIVE", "REVOKED")) {
            fixture.update("organization_access_owner", "UPDATE organization_access.organization_membership SET status=?,revision=revision+1 WHERE organization_id=?", status, organization);
            assertThat(fixture.json(fixture.get("first"), 200).get("workspaces").size()).isEqualTo(status.equals("ACTIVE") ? 2 : 1);
        }
        fixture.update("organization_access_owner", "INSERT INTO organization_access.organization_membership(id,organization_id,account_id,role) VALUES (?, ?, ?, 'MEMBER')",
                UUID.randomUUID(), organization, fixture.account);
        fixture.update("organization_access_owner", "UPDATE organization_access.organization SET status='DISABLED',revision=revision+1 WHERE id=?", organization);
        assertThat(fixture.json(fixture.get("first"), 200).get("workspaces").size()).isEqualTo(1);
        fixture.update("customer_iam_owner", "UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account);
        fixture.json(fixture.get("first"), 401);
    }

    @Test
    void requestsCannotSelectAnotherAccountOrganizationRoleOrChannel() throws Exception {
        for (String session : List.of("first", "second")) {
            fixture.clearCapture();
            UUID expected = session.equals("first") ? fixture.account : fixture.other;
            var json = fixture.json(fixture.get(fixture.context, session, "?accountId=" + fixture.other
                    + "&organizationId=" + UUID.randomUUID() + "&workspace=ORGANIZATION&role=ADMINISTRATOR&channel=IVR", true), 200);
            assertThat(json.get("account").get("accountId").asString()).isEqualTo(expected.toString());
            assertThat(json.get("channel").asString()).isEqualTo(session.equals("first") ? "WEB" : "MINIAPP");
            assertThat(fixture.executions.getLast().bindings()).containsExactly("ACTIVE", "ACTIVE", expected, "ACTIVE");
        }
    }

    @Test
    void successAndFailuresDoNotWriteAndRestorePoolState() throws Exception {
        fixture.organization(UUID.randomUUID(), "ACTIVE", "MEMBER", "ACTIVE", fixture.account);
        var before = fixture.snapshot();
        fixture.json(fixture.get("first"), 200);
        int backend = fixture.executions.getLast().pid();
        var pool = fixture.pools.get("customerQueryDataSource");
        fixture.assertPoolClean(pool, fixture.login);
        for (String failure : List.of("customer_account", "set local role")) {
            fixture.failOn = failure;
            fixture.json(fixture.get("first"), 503);
            fixture.failOn = null;
            fixture.assertPoolClean(pool, fixture.login);
            fixture.json(fixture.get("first"), 200);
            assertThat(fixture.executions.getLast().pid()).isEqualTo(backend);
        }
        fixture.writeProbe = true;
        try { fixture.json(fixture.get("first"), 503); assertThat(fixture.writeState).isEqualTo("25006"); }
        finally { fixture.writeProbe = false; }
        fixture.assertPoolClean(pool, fixture.login);
        fixture.failCommit = true;
        try { fixture.json(fixture.get("first"), 503); }
        finally { fixture.failCommit = false; }
        fixture.assertPoolClean(pool, fixture.login);
        fixture.json(fixture.get("first"), 200);
        assertThat(fixture.executions.getLast().pid()).isEqualTo(backend);
        assertThat(fixture.snapshot()).isEqualTo(before);
        assertThat(fixture.executions).allSatisfy(execution -> assertThat(execution.sql())
                .satisfiesAnyOf(sql -> assertThat(sql).isEqualTo("SET LOCAL ROLE customer_iam_role"),
                        sql -> assertThat(sql).isEqualTo("select current_user"), sql -> assertThat(sql).startsWith("SELECT a.id")));
        assertThat(fixture.rollbacks.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void webAndPa10PersistenceCoexistWithoutBeanOrRoleLeakage() throws Exception {
        try (var dual = fixture.startContext(true, fixture.login)) {
            fixture.json(fixture.get(dual, "first", "", false), 200);
            var sharing = dual.getBean("customerShareQueryService", CustomerShareQueryService.class);
            var page = sharing.listCustomerShares(new TrustedShareCaller(fixture.account, Optional.empty()),
                    new SharePageRequest(Optional.empty(), Optional.empty()));
            assertThat(page.items()).isEmpty();
            assertThat(dual.getBean("customerQueryDsl")).isNotSameAs(dual.getBean("sharingQueryDsl"));
            assertThat(dual.getBean("customerQueryTransactionTemplate")).isNotSameAs(dual.getBean("sharingQueryTransactionTemplate"));
            fixture.assertPoolClean(dual.getBean("customerQueryDataSource", javax.sql.DataSource.class).unwrap(com.zaxxer.hikari.HikariDataSource.class), fixture.login);
            fixture.assertPoolClean(dual.getBean("sharingQueryDataSource", javax.sql.DataSource.class).unwrap(com.zaxxer.hikari.HikariDataSource.class), fixture.sharingLogin);
            assertThat(fixture.executions).anySatisfy(execution -> assertThat(execution.role()).isEqualTo("sharing_role"));
            try (var customer = fixture.runtime()) {
                assertThatThrownBy(() -> CustomerMeFixture.execute(customer, "SET ROLE sharing_role"))
                        .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
            }
            try (var sharingConnection = java.sql.DriverManager.getConnection(fixture.url, fixture.sharingLogin, fixture.password)) {
                assertThatThrownBy(() -> CustomerMeFixture.execute(sharingConnection, "SET ROLE customer_iam_role"))
                        .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
            }
            try (var connection = fixture.admin(); var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "select pg_has_role('" + fixture.login + "','sharing_role','MEMBER'), pg_has_role('" + fixture.sharingLogin + "','customer_iam_role','MEMBER')")) {
                rows.next(); assertThat(rows.getBoolean(1)).isFalse(); assertThat(rows.getBoolean(2)).isFalse();
            }
        }
    }

    @Test
    void verifierOutageAndDatabaseFailureExposeNoSecrets() throws Exception {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            CustomerMeFixture.verifierOutage = true;
            fixture.json(fixture.get("first"), 503);
            assertThat(fixture.acquisitions).hasValue(0);
            CustomerMeFixture.verifierOutage = false;
            try (var denied = fixture.startContext(false, fixture.deniedLogin)) {
                fixture.json(fixture.get(denied, "first", "", false), 503);
            }
            fixture.disconnectNext = true;
            fixture.json(fixture.get("first"), 503);
            fixture.json(fixture.get("first"), 200);
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain("CANARY", fixture.password, "Bearer", "Authorization");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            CustomerMeFixture.verifierOutage = false; fixture.failOn = null;
            logger.detachAppender(appender); appender.stop();
        }
    }
}
