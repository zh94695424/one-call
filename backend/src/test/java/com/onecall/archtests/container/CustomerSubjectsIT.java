package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;

import com.onecall.core.customer_iam.internal.application.*;
import java.net.http.HttpRequest;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(OutputCaptureExtension.class)
class CustomerSubjectsIT {
    private static CustomerSubjectDetailFixture fixture;
    private CapturedOutput output;
    @BeforeAll static void start() throws Exception { fixture = new CustomerSubjectDetailFixture(); fixture.start(); }
    @AfterAll static void stop() { if (fixture != null) fixture.close(); }
    @BeforeEach void seed(CapturedOutput output) throws Exception { this.output = output; fixture.seed(); }

    private void counts(UUID subject, String token, String activation, long evidence, long orders) throws Exception {
        var json = fixture.get(subject, token, 200);
        assertThat(json.path("subjectId").asString()).isEqualTo(subject.toString());
        assertThat(json.path("activationStatus").asString()).isEqualTo(activation);
        assertThat(json.path("evidenceCount").asLong()).isEqualTo(evidence);
        assertThat(json.path("orderCount").asLong()).isEqualTo(orders);
    }
    private void account(String status) throws Exception {
        fixture.mutate("UPDATE customer_iam.customer_account SET status=?,revision=revision+1,updated_at=now() WHERE id=?", status, fixture.base.account);
    }
    private void organization(UUID id, String status) throws Exception {
        fixture.mutate("UPDATE organization_access.organization SET status=?,revision=revision+1,updated_at=now() WHERE id=?", status, id);
    }
    private void membership(UUID organization, String status) throws Exception {
        fixture.mutate("UPDATE organization_access.organization_membership SET status=?,revision=revision+1 WHERE account_id=? AND organization_id=?", status, fixture.base.account, organization);
    }
    private void noProjectionReads() {
        assertThat(fixture.queries).noneMatch(sql -> sql.contains("subject_risk_state") || sql.contains("service_activation")
                || sql.contains("evidence_record") || sql.contains("customer_order"));
    }
    private CustomerSubjectDetailQueryService completionFailureService() {
        var manager = fixture.base.context.getBean("customerQueryTransactionManager", DataSourceTransactionManager.class);
        var failing = new PlatformTransactionManager() {
            @Override public TransactionStatus getTransaction(TransactionDefinition definition) { return manager.getTransaction(definition); }
            @Override public void commit(TransactionStatus status) { manager.commit(status); throw new TransactionSystemException("COMPLETION_CANARY"); }
            @Override public void rollback(TransactionStatus status) { manager.rollback(status); }
        };
        var template = new TransactionTemplate(failing);
        template.setReadOnly(true); template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new CustomerSubjectDetailQueryService(template, fixture.dsl(),
                fixture.base.context.getBean("customerBusinessNumberAccessRepository", com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository.class),
                fixture.base.context.getBean("customerSubjectAccessReader", com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader.class),
                fixture.base.context.getBean("customerActivationStatusReader", com.onecall.core.activation_consent.customerquery.CustomerActivationStatusReader.class),
                fixture.base.context.getBean("customerSubjectEvidenceCountReader", com.onecall.core.evidence.customerquery.CustomerSubjectEvidenceCountReader.class),
                fixture.base.context.getBean("customerSubjectOrderCountReader", com.onecall.core.catalog_order.customerquery.CustomerSubjectOrderCountReader.class));
    }
    private void replaceService(CustomerSubjectDetailQueryService service) {
        var controller = fixture.base.context.getBean(com.onecall.core.customer_iam.internal.web.CustomerSubjectsController.class);
        var factory = new org.springframework.beans.factory.support.StaticListableBeanFactory(Map.of("customerSubjectDetailQueryService", service));
        var provider = factory.getBeanProvider(CustomerSubjectDetailQueryService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "service", provider);
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(controller, "service")).isSameAs(provider);
        assertThat(provider.getObject()).isSameAs(service);
    }

    @Test
    void postgres16LatestChainAndCustomerRuntimeRoleAreUsed() throws Exception {
        try (var connection = fixture.admin()) {
            assertThat(Integer.parseInt(one(connection, "SHOW server_version_num"))).isBetween(160000, 169999);
            assertThat(one(connection, "SELECT count(*) FROM public.flyway_schema_history WHERE success AND type='SQL'")).isEqualTo("16");
        }
        counts(fixture.subject, "personal", "INACTIVE", 0, 0);
        assertThat(fixture.states).isNotEmpty().allMatch(value -> value.startsWith("customer_iam_role|pa27_customer|on|repeatable read|"));
        assertThat(fixture.states.stream().map(value -> value.substring(value.lastIndexOf('|') + 1)).distinct()).hasSize(1);
        fixture.cleanPool();
    }

    @Test
    void personalOwnerReadsExactlyFourRequiredFields() throws Exception {
        String before = fixture.snapshot(); counts(fixture.subject, "personal", "INACTIVE", 0, 0);
        assertThat(fixture.snapshot()).isEqualTo(before);
        assertThat(fixture.queries).anyMatch(sql -> sql.contains("s.owner_account_id = ?"));
    }

    @Test
    void allFourActiveOrganizationRolesReadOnlySelectedOrganizationSubjects() throws Exception {
        var subjects = new ArrayList<UUID>();
        for (int index = 0; index < 4; index++) subjects.add(fixture.subject(null, fixture.base.organizations.get(index)));
        for (int index = 0; index < 4; index++) {
            counts(subjects.get(index), "org" + index, "INACTIVE", 0, 0);
            fixture.get(subjects.get((index + 1) % 4), "org" + index, 404);
            fixture.get(fixture.subject, "org" + index, 404);
        }
    }

    @Test
    void organizationCountsIncludeOtherPurchasersButNotOtherWorkspaces() throws Exception {
        UUID organization = fixture.base.organizations.getFirst(); UUID subject = fixture.subject(null, organization);
        fixture.order(subject, fixture.base.account, organization, "UNPAID");
        fixture.order(subject, fixture.base.other, organization, "PAID");
        fixture.order(subject, fixture.base.account, fixture.base.organizations.get(1), "UNKNOWN");
        fixture.order(subject, fixture.base.account, null, "CLOSED");
        counts(subject, "org0", "INACTIVE", 0, 2);
        fixture.get(subject, "personal", 404);
    }

    @Test
    void missingAndInactiveAccountsAreForbiddenBeforeSubjectLookup() throws Exception {
        fixture.base.session("missing", UUID.randomUUID(), null);
        for (String token : List.of("missing", "personal")) {
            if (token.equals("personal")) account("DISABLED");
            fixture.queries.clear(); fixture.get(fixture.foreign, token, 403);
            assertThat(fixture.queries).noneMatch(sql -> sql.contains("business_subject")); noProjectionReads();
        }
    }

    @Test
    void missingInactiveAndForeignSelectedOrganizationsAreForbidden() throws Exception {
        UUID unknown = UUID.randomUUID(); fixture.base.session("missing-org", fixture.base.account, unknown);
        fixture.get(fixture.subject, "missing-org", 403); fixture.get(fixture.subject, "org4", 403);
        for (String status : List.of("SUSPENDED", "DISABLED")) {
            organization(fixture.base.organizations.getFirst(), status); fixture.queries.clear();
            fixture.get(fixture.foreign, "org0", 403); noProjectionReads();
        }
    }

    @Test
    void missingInactiveAndRemovedMembershipsAreForbiddenForEveryRole() throws Exception {
        for (int index = 0; index < 4; index++) {
            UUID organization = fixture.base.organizations.get(index); UUID subject = fixture.subject(null, organization);
            for (String status : List.of("SUSPENDED", "REVOKED")) {
                membership(organization, status); fixture.queries.clear(); fixture.get(subject, "org" + index, 403); noProjectionReads();
            }
            fixture.mutate("DELETE FROM organization_access.organization_membership WHERE account_id=? AND organization_id=?", fixture.base.account, organization);
            fixture.get(subject, "org" + index, 403);
        }
    }

    @Test
    void missingUnownedForeignAccountAndForeignWorkspaceSubjectsAreUniformNotFound() throws Exception {
        fixture.risk(fixture.binding(fixture.foreign, "RETIRED"), "MANUAL_REVIEW", "OPEN");
        var missing = new ArrayList<>(List.of(UUID.randomUUID(), fixture.unowned, fixture.foreign,
                fixture.subject(null, fixture.base.organizations.getFirst())));
        for (UUID subject : missing) {
            fixture.queries.clear(); var problem = fixture.get(subject, "personal", 404);
            assertThat(problem.path("title").asString()).isEqualTo("Not found"); noProjectionReads();
            assertThat(fixture.queries).anyMatch(sql -> sql.contains("business_subject"));
        }
        var original = fixture.service(); replaceService(completionFailureService());
        try { fixture.get(fixture.foreign, "personal", 503); } finally { replaceService(original); }
        fixture.cleanPool();
    }

    @Test
    void openRiskOnAnyActiveDisabledOrRetiredBindingIsForbidden() throws Exception {
        for (String bindingStatus : List.of("ACTIVE", "DISABLED", "RETIRED")) for (String type : List.of("RECYCLED", "CONFLICT", "MANUAL_REVIEW")) {
            UUID subject = fixture.subject(fixture.base.account, null);
            fixture.binding(subject, "ACTIVE"); fixture.risk(fixture.binding(subject, bindingStatus), type, "OPEN");
            fixture.queries.clear(); fixture.get(subject, "personal", 403);
            assertThat(fixture.queries).noneMatch(sql -> sql.contains("service_activation") || sql.contains("evidence_record") || sql.contains("customer_order"));
        }
    }

    @Test
    void noBindingsNoRiskAndClearedRiskDoNotInventDenial() throws Exception {
        counts(fixture.subject, "personal", "INACTIVE", 0, 0);
        fixture.binding(fixture.subject, "DISABLED"); counts(fixture.subject, "personal", "INACTIVE", 0, 0);
        fixture.risk(fixture.binding(fixture.subject, "RETIRED"), "RECYCLED", "CLEARED");
        counts(fixture.subject, "personal", "INACTIVE", 0, 0);
    }

    @Test
    void missingActivationDefaultsInactiveAndEveryStoredStatusIsReturned() throws Exception {
        counts(fixture.subject, "personal", "INACTIVE", 0, 0);
        for (String status : List.of("INACTIVE", "ACTIVE", "SUSPENDED")) {
            UUID subject = fixture.subject(fixture.base.account, null); fixture.activation(subject, status);
            counts(subject, "personal", status, 0, 0);
        }
        try (var connection = fixture.admin()) { assertThat(one(connection, "SELECT count(*) FROM activation_consent.service_activation WHERE subject_id='" + fixture.subject + "'")).isEqualTo("0"); }
    }

    @Test
    void evidenceCountsEveryStatusForOnlyTheAccessibleSubject() throws Exception {
        for (String status : List.of("PENDING", "SOLIDIFIED", "QUARANTINED", "FAILED")) fixture.evidence(fixture.subject, status);
        fixture.evidence(fixture.foreign, "PENDING"); fixture.queries.clear();
        counts(fixture.subject, "personal", "INACTIVE", 4, 0);
        assertThat(fixture.queries).noneMatch(sql -> sql.contains("snapshot") || sql.contains("media_object") || sql.contains("evidence_event"));
    }

    @Test
    void personalOrderCountsAllPaymentStatesOnlyInPersonalWorkspace() throws Exception {
        for (String status : List.of("UNPAID", "UNKNOWN", "PAID", "CLOSED")) fixture.order(fixture.subject, fixture.base.account, null, status);
        fixture.order(fixture.subject, fixture.base.other, null, "PAID");
        fixture.order(fixture.subject, fixture.base.account, fixture.base.organizations.getFirst(), "PAID");
        fixture.order(fixture.foreign, fixture.base.account, null, "PAID");
        counts(fixture.subject, "personal", "INACTIVE", 0, 4);
    }

    @Test
    void emptyAndMultipleCountsNeverMultiplyAcrossBindingsOrActivation() throws Exception {
        counts(fixture.subject, "personal", "INACTIVE", 0, 0);
        for (String status : List.of("ACTIVE", "DISABLED", "RETIRED")) fixture.binding(fixture.subject, status);
        fixture.activation(fixture.subject, "SUSPENDED");
        fixture.evidence(fixture.subject, "PENDING"); fixture.evidence(fixture.subject, "FAILED");
        for (int index = 0; index < 3; index++) fixture.order(fixture.subject, fixture.base.account, null, "UNPAID");
        counts(fixture.subject, "personal", "SUSPENDED", 2, 3);
    }

    @Test
    void lifecycleRiskActivationAndCountsShareOneRepeatableReadSnapshot() throws Exception {
        UUID binding = fixture.binding(fixture.subject, "DISABLED");
        CountDownLatch reached = new CountDownLatch(1); CountDownLatch resume = new CountDownLatch(1);
        fixture.afterOwnership = () -> { reached.countDown(); await(resume); };
        try (var executor = Executors.newSingleThreadExecutor()) {
            var response = executor.submit(() -> fixture.get(fixture.subject, "personal", 200));
            try {
                assertThat(reached.await(15, TimeUnit.SECONDS)).isTrue();
                fixture.risk(binding, "RECYCLED", "OPEN"); fixture.activation(fixture.subject, "ACTIVE");
                fixture.evidence(fixture.subject, "PENDING"); fixture.order(fixture.subject, fixture.base.account, null, "UNPAID");
            } finally { resume.countDown(); }
            var json = response.get(20, TimeUnit.SECONDS);
            assertThat(json.path("activationStatus").asString()).isEqualTo("INACTIVE");
            assertThat(json.path("evidenceCount").asLong()).isZero(); assertThat(json.path("orderCount").asLong()).isZero();
        }
        fixture.get(fixture.subject, "personal", 403); fixture.clearRisk(); counts(fixture.subject, "personal", "ACTIVE", 1, 1);
        CountDownLatch lifecycleReached = new CountDownLatch(1); CountDownLatch lifecycleResume = new CountDownLatch(1);
        fixture.afterLifecycle = () -> { lifecycleReached.countDown(); await(lifecycleResume); };
        try (var executor = Executors.newSingleThreadExecutor()) {
            var response = executor.submit(() -> fixture.get(fixture.subject, "personal", 200));
            try { assertThat(lifecycleReached.await(15, TimeUnit.SECONDS)).isTrue(); account("DISABLED"); }
            finally { lifecycleResume.countDown(); }
            assertThat(response.get(20, TimeUnit.SECONDS).path("activationStatus").asString()).isEqualTo("ACTIVE");
        }
        fixture.get(fixture.subject, "personal", 403);
    }
    private static void await(CountDownLatch latch) {
        try { if (!latch.await(20, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for fixture"); }
        catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
    }

    @Test
    void committedLifecycleAndRiskChangesAreRecheckedOnNextRequest() throws Exception {
        counts(fixture.subject, "personal", "INACTIVE", 0, 0); account("DISABLED"); fixture.get(fixture.subject, "personal", 403);
        account("ACTIVE"); counts(fixture.subject, "personal", "INACTIVE", 0, 0);
        UUID organization = fixture.base.organizations.getFirst(); UUID subject = fixture.subject(null, organization);
        counts(subject, "org0", "INACTIVE", 0, 0); organization(organization, "SUSPENDED"); fixture.get(subject, "org0", 403);
        organization(organization, "ACTIVE"); membership(organization, "SUSPENDED"); fixture.get(subject, "org0", 403);
        membership(organization, "ACTIVE"); counts(subject, "org0", "INACTIVE", 0, 0);
        fixture.risk(fixture.binding(subject, "RETIRED"), "CONFLICT", "OPEN"); fixture.get(subject, "org0", 403);
        fixture.clearRisk(); counts(subject, "org0", "INACTIVE", 0, 0);
    }

    @Test
    void successDenialAndDependencyFailurePerformZeroBusinessWrites() throws Exception {
        UUID risky = fixture.subject(fixture.base.account, null); fixture.risk(fixture.binding(risky, "RETIRED"), "CONFLICT", "OPEN");
        String before = fixture.snapshot(); fixture.queries.clear();
        fixture.get(fixture.subject, "personal", 200); fixture.get(risky, "personal", 403); fixture.get(fixture.foreign, "personal", 404);
        fixture.check(fixture.request("GET", "bad", "personal", HttpRequest.BodyPublishers.noBody()), 422);
        fixture.failOn = "count(subject_id)";
        try { fixture.get(fixture.subject, "personal", 503); } finally { fixture.failOn = null; }
        assertThat(fixture.snapshot()).isEqualTo(before);
        assertThat(fixture.queries).allMatch(sql -> sql.stripLeading().toUpperCase(Locale.ROOT).startsWith("SELECT") || sql.equals("SET LOCAL ROLE customer_iam_role"));
    }

    @Test
    void pooledRoleReadOnlyIsolationAutoCommitAndAmbientTransactionsRestore() throws Exception {
        fixture.get(fixture.subject, "personal", 200); fixture.cleanPool(); fixture.get(fixture.foreign, "personal", 404); fixture.cleanPool();
        account("DISABLED"); fixture.get(fixture.subject, "personal", 403); fixture.cleanPool(); account("ACTIVE");
        for (String failure : List.of("SET LOCAL ROLE", "select current_user", "evidence_record")) {
            fixture.failOn = failure;
            try { fixture.get(fixture.subject, "personal", 503); } finally { fixture.failOn = null; }
            fixture.cleanPool();
        }
        var original = fixture.service(); replaceService(completionFailureService());
        try { fixture.get(fixture.subject, "personal", 503); fixture.get(fixture.foreign, "personal", 503); }
        finally { replaceService(original); }
        fixture.cleanPool();
        var pool = fixture.base.context.getBean("customerQueryDataSource", com.zaxxer.hikari.HikariDataSource.class); pool.setMaximumPoolSize(2);
        var manager = fixture.base.context.getBean("customerQueryTransactionManager", DataSourceTransactionManager.class);
        try {
            new TransactionTemplate(manager).execute(status -> {
                String before = fixture.dsl().fetchOne("SELECT current_user||'|'||pg_backend_pid()").get(0, String.class);
                assertThat(original.getCustomerSubject(fixture.caller(), fixture.subject)).isPresent();
                assertThat(fixture.dsl().fetchOne("SELECT current_user||'|'||pg_backend_pid()").get(0, String.class)).isEqualTo(before);
                return null;
            });
        } finally { pool.setMaximumPoolSize(1); }
        fixture.cleanPool();
    }

    @Test
    void responsesProblemsAndCapturedLogsNeverDiscloseSecretsOrRowContents() throws Exception {
        fixture.binding(fixture.subject, "ACTIVE"); fixture.evidence(fixture.subject, "PENDING"); fixture.order(fixture.subject, fixture.base.account, null, "UNPAID");
        int offset = output.getAll().length();
        fixture.get(fixture.subject, "personal", 200); fixture.get(fixture.foreign, "personal", 404);
        fixture.failOn = "evidence_record";
        try { fixture.get(fixture.subject, "personal", 503); } finally { fixture.failOn = null; }
        assertThat(output.getAll().substring(offset)).doesNotContain("CANARY", fixture.base.password, fixture.base.account.toString(), fixture.foreign.toString(), "Bearer ", "SCRAM-SHA-256", "jdbc:");
    }

    @Test
    void cursorlessUuidValidationAndIgnoredInputsPreserveExistingCustomerEndpoints() throws Exception {
        for (String value : List.of("bad", "1-1-1-1-1", fixture.subject + ".json")) {
            fixture.queries.clear(); fixture.check(fixture.request("GET", value, "personal", HttpRequest.BodyPublishers.noBody()), 422); assertThat(fixture.queries).isEmpty();
        }
        for (String value : List.of(fixture.subject + "/", fixture.subject + "/json")) fixture.check(fixture.request("GET", value, "personal", HttpRequest.BodyPublishers.noBody()), 404);
        for (String method : List.of("POST", "PUT", "DELETE", "OPTIONS", "HEAD")) assertThat(fixture.request(method, fixture.subject.toString(), "personal", HttpRequest.BodyPublishers.noBody()).statusCode()).isEqualTo(404);
        String query = "?ownerAccountId=foreign&accountId=bad&accountId=other&workspace=bad&role=bad&cursor=bad&cursor=bad";
        for (boolean chunked : List.of(false, true)) {
            var body = chunked ? HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream("CANARY".getBytes(java.nio.charset.StandardCharsets.UTF_8))) : HttpRequest.BodyPublishers.ofString("{\"owner\":\"CANARY\"}");
            fixture.check(fixture.request("GET", fixture.subject + query, "personal", body), 200);
            fixture.check(fixture.request("GET", fixture.foreign + query, "personal", HttpRequest.BodyPublishers.ofString("CANARY")), 404);
        }
        fixture.base.json("personal", "", 503);
        assertThat(fixture.base.context.getBeanFactory().containsSingleton("customerBusinessNumberCursorCodec")).isFalse();
        fixture.get(fixture.subject, "personal", 200);
    }
}
