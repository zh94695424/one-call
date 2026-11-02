package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CustomerSharesIT {
    private static CustomerSharesFixture fixture;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @BeforeAll
    static void start() throws Exception {
        fixture = new CustomerSharesFixture();
        fixture.start();
    }

    @AfterAll
    static void close() {
        if (fixture != null) fixture.close();
    }

    @BeforeEach
    void reset() throws Exception {
        fixture.reset();
        fixture.base.failOn = null;
        fixture.base.verifierOutage = false;
    }

    @Test
    void authenticatedHttpListsPersonalAndOrganizationCreatorMetadata() throws Exception {
        Instant timestamp = Instant.parse("2026-01-02T03:04:05.123456Z");
        String personal = fixture.seedShare(fixture.base.account, null, timestamp);
        fixture.base.organization(fixture.organization, "ACTIVE", "MEMBER", "ACTIVE", fixture.base.account);
        fixture.membership(fixture.organization, fixture.base.other);
        fixture.base.organization(fixture.organizationTwo, "ACTIVE", "MEMBER", "ACTIVE", fixture.base.account);
        String organization = fixture.seedShare(fixture.base.account, fixture.organization, timestamp);
        fixture.seedShare(fixture.base.other, fixture.organization, timestamp.plusSeconds(1));
        String otherWorkspace = fixture.seedShare(fixture.base.account, fixture.organizationTwo, timestamp);
        var personalResponse = fixture.shares("first", "");
        assertThat(personalResponse.statusCode()).isEqualTo(200);
        assertThat(itemIds(personalResponse.body())).containsExactly(personal);
        fixture.sessions(WorkspaceType.ORGANIZATION, fixture.organization);
        var organizationResponse = fixture.shares("first", "");
        assertThat(organizationResponse.statusCode()).isEqualTo(200);
        assertThat(itemIds(organizationResponse.body())).containsExactly(organization);
        fixture.sessions(WorkspaceType.ORGANIZATION, fixture.organizationTwo);
        assertThat(itemIds(fixture.shares("first", "").body())).containsExactly(otherWorkspace);
    }

    @Test
    void httpPaginationPreservesTiesAndConcurrentInsertOrder() throws Exception {
        Instant tie = Instant.parse("2026-01-02T03:04:05.123456Z");
        fixture.seedShare("同時刻-é", fixture.base.account, null, tie);
        fixture.seedShare("同時刻-雪", fixture.base.account, null, tie);
        fixture.seedShare("older", fixture.base.account, null, tie.minusSeconds(1));
        List<String> ordered = fixture.orderedShareIds(fixture.base.account, null);
        var pageOne = fixture.shares("first", "?limit=1");
        assertThat(pageOne.statusCode()).isEqualTo(200);
        JsonNode firstPage = mapper.readTree(pageOne.body());
        assertThat(firstPage.get("items").size()).isEqualTo(1);
        assertThat(firstPage.get("nextCursor").isTextual()).isTrue();
        String nextCursor = firstPage.get("nextCursor").asString();
        fixture.seedShare(fixture.base.account, null, tie.plusSeconds(10));
        var pageTwo = fixture.shares("first", "?limit=1&cursor=" + fixture.encodedCursor(nextCursor));
        assertThat(pageTwo.statusCode()).isEqualTo(200);
        String returned = itemIds(pageTwo.body()).getFirst();
        assertThat(ordered).contains(firstPage.get("items").get(0).get("shareId").asString(), returned);
        assertThat(ordered.indexOf(firstPage.get("items").get(0).get("shareId").asString())).isZero();
        assertThat(ordered.indexOf(returned)).isEqualTo(1);
        assertThat(returned).isNotEqualTo(firstPage.get("items").get(0).get("shareId").asString());
        JsonNode tail = mapper.readTree(fixture.shares("first", "?limit=100&cursor=" + fixture.encodedCursor(nextCursor)).body());
        assertThat(itemIds(tail.toString())).containsExactlyElementsOf(ordered.subList(1, ordered.size()));
        assertThat(tail.get("nextCursor").isNull()).isTrue();
        assertThat(mapper.readTree(fixture.shares("first", "?limit=100").body()).get("items").size()).isEqualTo(4);
    }

    @Test
    void httpCursorsCannotCrossTrustedAccountOrWorkspace() throws Exception {
        fixture.seedShare(fixture.base.account, null, Instant.parse("2026-01-02T03:04:05Z"));
        fixture.seedShare(fixture.base.account, null, Instant.parse("2026-01-01T03:04:05Z"));
        String cursor = mapper.readTree(fixture.shares("first", "?limit=1").body()).get("nextCursor").asString();
        int beforeAccountMismatch = fixture.base.acquisitions.get();
        assertThat(fixture.shares("second", "?cursor=" + fixture.encodedCursor(cursor)).statusCode()).isEqualTo(400);
        assertThat(fixture.base.acquisitions).hasValue(beforeAccountMismatch);
        fixture.base.organization(fixture.organization, "ACTIVE", "MEMBER", "ACTIVE", fixture.base.account);
        fixture.sessions(WorkspaceType.ORGANIZATION, fixture.organization);
        int beforeInvalid = fixture.base.acquisitions.get();
        assertThat(fixture.shares("first", "?cursor=" + fixture.encodedCursor(cursor)).statusCode()).isEqualTo(400);
        assertThat(fixture.base.acquisitions).hasValue(beforeInvalid);
        String tampered = cursor.substring(0, cursor.length() - 1) + (cursor.endsWith("A") ? "B" : "A");
        assertThat(fixture.shares("first", "?cursor=" + fixture.encodedCursor(tampered)).statusCode()).isEqualTo(400);
        assertThat(fixture.base.acquisitions).hasValue(beforeInvalid);
        fixture.seedShare(fixture.base.account, fixture.organization, Instant.parse("2026-01-03T03:04:05Z"));
        fixture.seedShare(fixture.base.account, fixture.organization, Instant.parse("2026-01-02T03:04:05Z"));
        String organizationCursor = mapper.readTree(fixture.shares("first", "?limit=1").body()).get("nextCursor").asString();
        fixture.base.organization(fixture.organizationTwo, "ACTIVE", "MEMBER", "ACTIVE", fixture.base.account);
        fixture.sessions(WorkspaceType.ORGANIZATION, fixture.organizationTwo);
        int beforeOrganizationMismatch = fixture.base.acquisitions.get();
        assertThat(fixture.shares("first", "?cursor=" + fixture.encodedCursor(organizationCursor)).statusCode()).isEqualTo(400);
        assertThat(fixture.base.acquisitions).hasValue(beforeOrganizationMismatch);
        int acquisitions = fixture.base.acquisitions.get();
        assertThat(fixture.shares("first", "?limit=0").statusCode()).isEqualTo(422);
        assertThat(fixture.base.acquisitions).hasValue(acquisitions);
    }

    @Test
    void committedAccountMembershipAndOrganizationChangesDenyNextPage() throws Exception {
        fixture.seedShare(fixture.base.account, null, Instant.parse("2026-01-02T03:04:05Z"));
        fixture.seedShare(fixture.base.account, null, Instant.parse("2026-01-01T03:04:05Z"));
        String cursor = mapper.readTree(fixture.shares("first", "?limit=1").body()).get("nextCursor").asString();
        assertThat(itemIds(fixture.shares("first", "?cursor=" + fixture.encodedCursor(cursor)).body())).isNotEmpty();
        cursor = mapper.readTree(fixture.shares("first", "?limit=1").body()).get("nextCursor").asString();
        fixture.base.update("customer_iam_owner",
                "UPDATE customer_iam.customer_account SET status='DISABLED', revision=revision+1 WHERE id=?", fixture.base.account);
        assertThat(fixture.shares("first", "?cursor=" + cursor).statusCode()).isEqualTo(403);
        fixture.base.update("customer_iam_owner",
                "UPDATE customer_iam.customer_account SET status='ACTIVE', revision=revision+1 WHERE id=?", fixture.base.account);
        fixture.base.organization(fixture.organization, "ACTIVE", "MEMBER", "ACTIVE", fixture.base.account);
        fixture.sessions(WorkspaceType.ORGANIZATION, fixture.organization);
        fixture.seedShare(fixture.base.account, fixture.organization, Instant.parse("2026-01-02T03:04:05Z"));
        fixture.seedShare(fixture.base.account, fixture.organization, Instant.parse("2026-01-01T03:04:05Z"));
        String organizationCursor = mapper.readTree(fixture.shares("first", "?limit=1").body()).get("nextCursor").asString();
        fixture.base.update("organization_access_owner",
                "UPDATE organization_access.organization_membership SET status='SUSPENDED', revision=revision+1 WHERE organization_id=? AND account_id=?",
                fixture.organization, fixture.base.account);
        assertThat(fixture.shares("first", "?cursor=" + organizationCursor).statusCode()).isEqualTo(403);
        fixture.base.update("organization_access_owner",
                "UPDATE organization_access.organization_membership SET status='ACTIVE', revision=revision+1 WHERE organization_id=? AND account_id=?",
                fixture.organization, fixture.base.account);
        fixture.base.update("organization_access_owner",
                "UPDATE organization_access.organization_membership SET status='REVOKED', revision=revision+1 WHERE organization_id=? AND account_id=?",
                fixture.organization, fixture.base.account);
        assertThat(fixture.shares("first", "?cursor=" + fixture.encodedCursor(organizationCursor)).statusCode()).isEqualTo(403);
        fixture.base.update("organization_access_owner",
                "UPDATE organization_access.organization_membership SET status='ACTIVE', revision=revision+1 WHERE organization_id=? AND account_id=?",
                fixture.organization, fixture.base.account);
        fixture.base.update("organization_access_owner",
                "UPDATE organization_access.organization SET status='DISABLED', revision=revision+1 WHERE id=?", fixture.organization);
        assertThat(fixture.shares("first", "?limit=0").statusCode()).isEqualTo(422);
        assertThat(fixture.shares("first", "?cursor=" + fixture.encodedCursor(organizationCursor)).statusCode()).isEqualTo(403);
    }

    @Test
    void customerAndSharingConnectionsRemainSeparatelyOwned() throws Exception {
        fixture.seedShare(fixture.base.account, null, Instant.parse("2026-01-02T03:04:05Z"));
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(200);
        assertThat(fixture.base.get("first").statusCode()).isEqualTo(200);
        List<com.onecall.archtests.container.CustomerMeFixture.Execution> shareExecutions = fixture.base.executions.stream()
                .filter(execution -> execution.sql().toLowerCase().contains("evidence_share_grant")).toList();
        List<com.onecall.archtests.container.CustomerMeFixture.Execution> customerExecutions = fixture.base.executions.stream()
                .filter(execution -> execution.session().equals(fixture.base.login)
                        && execution.role().equals("customer_iam_role")
                        && execution.sql().startsWith("SELECT a.id")).toList();
        List<com.onecall.archtests.container.CustomerMeFixture.Execution> sharingLifecycleReads = fixture.base.executions.stream()
                .filter(execution -> execution.session().equals(fixture.base.sharingLogin)
                        && execution.sql().toLowerCase().contains("customer_account")).toList();
        assertThat(shareExecutions).isNotEmpty().allSatisfy(execution -> {
            assertThat(execution.role()).isEqualTo("sharing_role");
            assertThat(execution.session()).isEqualTo(fixture.base.sharingLogin);
            assertThat(execution.readOnly()).isEqualTo("on");
            assertThat(execution.isolation()).isEqualTo("read committed");
        });
        assertThat(customerExecutions).isNotEmpty().allSatisfy(execution -> {
            assertThat(execution.role()).isEqualTo("customer_iam_role");
            assertThat(execution.session()).isEqualTo(fixture.base.login);
            assertThat(fixture.checkoutOwner(execution.checkout())).isEqualTo("customerQueryDataSource");
        });
        assertThat(sharingLifecycleReads).isNotEmpty().allSatisfy(execution -> {
            assertThat(execution.role()).isEqualTo("sharing_role");
            assertThat(execution.readOnly()).isEqualTo("on");
            assertThat(fixture.checkoutOwner(execution.checkout())).isEqualTo("sharingQueryDataSource");
        });
        assertThat(shareExecutions).allSatisfy(execution ->
                assertThat(fixture.checkoutOwner(execution.checkout())).isEqualTo("sharingQueryDataSource"));
        Map<String, HikariDataSource> pools = fixture.capturePoolsByBeanName();
        assertThat(pools.keySet()).containsExactlyInAnyOrder("customerQueryDataSource", "sharingQueryDataSource");
        fixture.base.assertPoolClean(pools.get("customerQueryDataSource"), fixture.base.login);
        fixture.base.assertPoolClean(pools.get("sharingQueryDataSource"), fixture.base.sharingLogin);
        try (var connection = fixture.base.admin(); var statement = connection.prepareStatement("""
                SELECT rolname, rolsuper, rolcreatedb, rolcreaterole, rolcanlogin
                FROM pg_roles WHERE rolname IN (?, ?, ?)
                """)) {
            statement.setString(1, fixture.base.login);
            statement.setString(2, fixture.base.sharingLogin);
            statement.setString(3, fixture.base.deniedLogin);
            try (var rows = statement.executeQuery()) {
                int checked = 0;
                while (rows.next()) {
                    checked++;
                    assertThat(rows.getBoolean("rolsuper")).isFalse();
                    assertThat(rows.getBoolean("rolcreatedb")).isFalse();
                    assertThat(rows.getBoolean("rolcreaterole")).isFalse();
                    assertThat(rows.getBoolean("rolcanlogin")).isTrue();
                }
                assertThat(checked).isEqualTo(3);
            }
        }
        assertThat(fixture.beanCheckouts.get("sharingQueryDataSource").get()).isPositive();
        assertThat(fixture.beanCheckouts.get("customerQueryDataSource").get()).isPositive();
    }

    @Test
    void existingAclsAndReadOnlyTransactionsForbidWritesAndForeignReads() throws Exception {
        String shareId = fixture.seedShare(fixture.base.account, null, Instant.parse("2026-01-02T03:04:05Z"));
        fixture.assertSharingReadOnlyWriteDenied(shareId);
        try (Connection sharing = java.sql.DriverManager.getConnection(fixture.base.url, fixture.base.sharingLogin, fixture.base.password)) {
            assertThatThrownBy(() -> CustomerMeFixture.execute(sharing, "SELECT capability_token_hash FROM sharing.evidence_share_grant"))
                    .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
            assertThatThrownBy(() -> CustomerMeFixture.execute(sharing, "SET ROLE customer_iam_role"))
                    .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
            assertThatThrownBy(() -> CustomerMeFixture.execute(sharing, "SELECT id_number_ciphertext FROM customer_iam.identity_profile"))
                    .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
            assertThatThrownBy(() -> CustomerMeFixture.execute(sharing, "UPDATE evidence.evidence_record SET status='REVOKED'"))
                    .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
            assertThatThrownBy(() -> CustomerMeFixture.execute(sharing, "SELECT * FROM customer_iam.customer_account FOR UPDATE"))
                    .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
        }
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(200);
        assertThat(fixture.base.executions)
                .filteredOn(execution -> execution.sql().toLowerCase().contains("evidence_share_grant"))
                .anySatisfy(execution -> {
                    assertThat(execution.readOnly()).isEqualTo("on");
                    assertThat(execution.role()).isEqualTo("sharing_role");
                });
    }

    @Test
    void successfulAndFailedHttpPagesNeverWriteOrProjectSecrets() throws Exception {
        fixture.seedShare(fixture.base.account, null, Instant.parse("2026-01-02T03:04:05Z"));
        Map<String, String> before = fixture.snapshotSharingAndEvidenceRows();
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(200);
        assertThat(before).isEqualTo(fixture.snapshotSharingAndEvidenceRows());
        Map<String, String> beforeValidation = fixture.snapshotSharingAndEvidenceRows();
        assertThat(fixture.shares("first", "?limit=0").statusCode()).isEqualTo(422);
        assertThat(beforeValidation).isEqualTo(fixture.snapshotSharingAndEvidenceRows());
        Map<String, String> beforeUnauthenticated = fixture.snapshotSharingAndEvidenceRows();
        assertThat(fixture.shares("unknown", "").statusCode()).isEqualTo(401);
        assertThat(beforeUnauthenticated).isEqualTo(fixture.snapshotSharingAndEvidenceRows());
        Map<String, String> beforeBadCursor = fixture.snapshotSharingAndEvidenceRows();
        assertThat(fixture.shares("first", "?cursor=invalid").statusCode()).isEqualTo(400);
        assertThat(beforeBadCursor).isEqualTo(fixture.snapshotSharingAndEvidenceRows());
        fixture.base.update("customer_iam_owner",
                "UPDATE customer_iam.customer_account SET status='DISABLED', revision=revision+1 WHERE id=?", fixture.base.account);
        Map<String, String> beforeForbidden = fixture.snapshotSharingAndEvidenceRows();
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(403);
        assertThat(beforeForbidden).isEqualTo(fixture.snapshotSharingAndEvidenceRows());
        fixture.base.update("customer_iam_owner",
                "UPDATE customer_iam.customer_account SET status='ACTIVE', revision=revision+1 WHERE id=?", fixture.base.account);
        fixture.mappingFailureOn = "evidence_share_grant";
        Map<String, String> beforeInternal = fixture.snapshotSharingAndEvidenceRows();
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(500);
        assertThat(beforeInternal).isEqualTo(fixture.snapshotSharingAndEvidenceRows());
        fixture.mappingFailureOn = null;
        fixture.base.failOn = "evidence_share_grant";
        Map<String, String> beforeUnavailable = fixture.snapshotSharingAndEvidenceRows();
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(503);
        assertThat(beforeUnavailable).isEqualTo(fixture.snapshotSharingAndEvidenceRows());
        fixture.base.failOn = null;
        assertThat(fixture.shares("first", "").body()).doesNotContain(CustomerSharesFixture.SECRET,
                "capability_token_hash", "code_hash", "object_key");
        fixture.sessions(null, null);
        assertThat(fixture.shares("first", "?limit=0").statusCode()).isEqualTo(503);
        fixture.sessions(WorkspaceType.PERSONAL, null);
        fixture.base.failOn = "evidence_share_grant";
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(503);
        fixture.base.failOn = null;
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(200);
        assertThat(fixture.shares("first", "?limit=999").statusCode()).isEqualTo(422);
        fixture.base.failOn = "evidence_share_grant";
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(503);
        fixture.base.failOn = null;
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(200);
        for (String canary : List.of(CustomerSharesFixture.SECRET, fixture.base.password, "capability_token_hash", "code_hash",
                "salt", "object_key", "media_uri", "permanent_url", "identity_profile", "ID_CANARY", "NAME_CANARY")) {
            assertThat(fixture.shares("first", "").body()).doesNotContain(canary);
        }
        fixture.assertServiceLogsExclude(CustomerSharesFixture.SECRET, fixture.base.password, "capability_token_hash", "code_hash",
                "salt", "object_key", "media_uri", "permanent_url", "identity_profile", "ID_CANARY", "NAME_CANARY");
    }

    @Test
    void missingRolesVerifierAndDatabaseFailuresStaySanitized() throws Exception {
        fixture.seedShare(fixture.base.account, null, Instant.parse("2026-01-02T03:04:05Z"));
        try (var webOnly = fixture.startSharesContext(false, fixture.base.sharingLogin)) {
            fixture.sessions(WorkspaceType.PERSONAL, null);
            assertThat(fixture.shares("first", "").statusCode()).isEqualTo(503);
        }
        fixture.startSharesContext(true, fixture.base.sharingLogin);
        fixture.sessions(WorkspaceType.PERSONAL, null);
        try (var denied = fixture.startSharesContext(true, fixture.base.deniedLogin)) {
            fixture.sessions(WorkspaceType.PERSONAL, null);
            var deniedResponse = fixture.shares("first", "");
            assertThat(deniedResponse.statusCode()).isEqualTo(503);
            assertThat(deniedResponse.body()).doesNotContain(fixture.base.password, "jdbc:", "SQLException");
        }
        fixture.startSharesContext(true, fixture.base.sharingLogin);
        fixture.sessions(WorkspaceType.PERSONAL, null);
        fixture.base.verifierOutage = true;
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(503);
        fixture.base.verifierOutage = false;
        fixture.base.failOn = "evidence_share_grant";
        var meBeforeSharingFault = fixture.base.get("first");
        assertThat(meBeforeSharingFault.statusCode()).isEqualTo(200);
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(503);
        fixture.base.failOn = null;
        var meAfterSharingFault = fixture.base.get("first");
        assertThat(meAfterSharingFault.statusCode()).isEqualTo(200);
        assertThat(meAfterSharingFault.body()).isEqualTo(meBeforeSharingFault.body());
        fixture.mappingFailureOn = "evidence_share_grant";
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(500);
        fixture.mappingFailureOn = null;
        fixture.base.failCommit = true;
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(503);
        fixture.base.disconnectNext = true;
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(503);
        fixture.base.failOn = null;
        fixture.mappingFailureOn = null;
        fixture.base.failCommit = false;
        fixture.base.disconnectNext = false;
        assertThat(fixture.shares("first", "").statusCode()).isEqualTo(200);
        Map<String, HikariDataSource> pools = fixture.capturePoolsByBeanName();
        fixture.base.assertPoolClean(pools.get("sharingQueryDataSource"), fixture.base.sharingLogin);
        Map<String, String> rows = fixture.snapshotSharingAndEvidenceRows();
        assertThat(fixture.base.get("first").statusCode()).isEqualTo(200);
        assertThat(rows).isEqualTo(fixture.snapshotSharingAndEvidenceRows());
        for (var pool : pools.values()) fixture.base.assertPoolClean(pool,
                pool == pools.get("customerQueryDataSource") ? fixture.base.login : fixture.base.sharingLogin);
        for (var execution : fixture.base.executions) {
            for (String canary : List.of("capability_token_hash", "code_hash", "salt", "object_key", "media_uri",
                    "permanent_url", "private_identity", "ID_CANARY", "NAME_CANARY", fixture.base.password)) {
                assertThat(execution.sql()).doesNotContain(canary);
                assertThat(execution.bindings().toString()).doesNotContain(canary);
            }
        }
        fixture.assertServiceLogsExclude(CustomerSharesFixture.SECRET, fixture.base.password, "COMMIT_CANARY", "JDBC_CANARY",
                "MAPPING_CANARY", "capability_token_hash", "code_hash", "salt", "object_key", "media_uri", "permanent_url");
    }

    private List<String> itemIds(String body) {
        return mapper.readTree(body).get("items").valueStream().map(item -> item.get("shareId").asString()).toList();
    }
}
