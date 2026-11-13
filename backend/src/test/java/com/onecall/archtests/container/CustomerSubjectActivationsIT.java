package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService;
import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.*;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code;
import com.onecall.core.activation_consent.internal.command.ActivationSemantics;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService.*;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.customer_iam.internal.security.VerifiedCustomerSession;
import java.util.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerSubjectActivationsIT {
    CustomerSubjectActivationFixture fixture;
    @BeforeAll void start() throws Exception { fixture = new CustomerSubjectActivationFixture(); fixture.start(); }
    @AfterAll void stop() { if (fixture != null) fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    static void denied(Runnable action, Code code) { assertThatThrownBy(action::run).isInstanceOfSatisfying(SubjectActivationException.class, failure -> assertThat(failure.code()).isEqualTo(code)); }
    void exact(List<Long> expected) throws Exception { assertThat(fixture.counts()).isEqualTo(expected); }
    @Test
    void personalAcceptCommitsExactActivationTrialAndResult() throws Exception {
        var result = fixture.accept("event"); assertThat(result.activationStatus()).isEqualTo("ACTIVE"); assertThat(result.activationRevision()).isEqualTo(2); assertThat(result.trialOutcome()).isEqualTo("GRANTED"); assertThat(result.trial().grantedSeconds()).isEqualTo(600); exact(List.of(1L, 1L, 1L, 1L, 1L, 1L, 2L, 0L));
    }
    @Test
    void existingInactiveHeadAdvancesExactlyOneRevision() throws Exception {
        fixture.sql("INSERT INTO activation_consent.service_activation(id,subject_id,status,revision) VALUES (?,?,'INACTIVE',7)", UUID.randomUUID(), fixture.subject); assertThat(fixture.accept("event").activationRevision()).isEqualTo(8); exact(List.of(1L, 1L, 1L, 1L, 1L, 1L, 2L, 0L));
        fixture.reset(); fixture.sql("SELECT entitlement_metering.issue_first_trial(?,'FIRST-TRIAL',?,'IVR_CONFIRM','previous',?)", fixture.subject, fixture.policy, "c".repeat(64)); long outbox = fixture.count("platform.outbox_event");
        assertThat(fixture.accept("later").trialOutcome()).isEqualTo("ALREADY_GRANTED"); assertThat(fixture.count("platform.outbox_event") - outbox).isEqualTo(1); exact(List.of(1L, 1L, 1L, 1L, 1L, 1L, 2L, 0L));
    }
    @Test
    void webAndMiniappUseTrustedChannelAndClosedRequest() throws Exception {
        for (TrustedCustomerCaller.Channel channel : TrustedCustomerCaller.Channel.values()) {
            fixture.reset(); CustomerBusinessNumbersFixture.SESSIONS.put("personal", new VerifiedCustomerSession(fixture.account, channel, "onecall:customer-session:v1", new com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace(com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.PERSONAL, null)));
            String oversized = fixture.body("oversized", Decision.ACCEPT) + " ".repeat(8192);
            for (java.net.http.HttpRequest.BodyPublisher body : List.of(java.net.http.HttpRequest.BodyPublishers.ofString(oversized), java.net.http.HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream(oversized.getBytes(java.nio.charset.StandardCharsets.UTF_8))))) {
                var rejected = fixture.post("personal", fixture.subject, body);
                assertThat(rejected.statusCode()).isEqualTo(422);
                var problem = JsonMapper.builder().build().readTree(rejected.body());
                assertThat(problem.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
                assertThat(problem.path("code").asString()).isEqualTo("VALIDATION_FAILED");
                assertThat(rejected.headers().firstValue("Cache-Control")).contains("no-store");
                assertThat(rejected.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
                exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
            }
            var response = fixture.post("personal", fixture.subject, fixture.body("event", Decision.ACCEPT)); assertThat(response.statusCode()).isEqualTo(200); assertThat(fixture.scalar("SELECT channel FROM activation_consent.consent_record")).isEqualTo(channel.name());
            assertThat(fixture.post("personal", fixture.subject, fixture.body("bad", Decision.ACCEPT).replace("{", "{\"accountId\":\"spoof\",")).statusCode()).isEqualTo(422);
        }
    }
    @Test
    void organizationAllAllowedRolesCanActivateAccessibleSubject() throws Exception {
        for (String role : List.of("ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR")) {
            fixture.reset(); UUID subject = fixture.subject(null, fixture.organization); fixture.sql("UPDATE organization_access.organization_membership SET role=?,revision=revision+1", role);
            var response = fixture.post("organization", subject, fixture.body("event", Decision.ACCEPT));
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(JsonMapper.builder().build().readTree(response.body()).path("activationStatus").asString()).isEqualTo("ACTIVE");
            exact(List.of(1L, 1L, 1L, 1L, 1L, 1L, 2L, 0L));
        }
    }
    @Test
    void inactiveOrganizationMembershipOrDisallowedRoleIsForbidden() throws Exception {
        for (String sql : List.of("UPDATE organization_access.organization SET status='DISABLED',revision=revision+1", "UPDATE organization_access.organization_membership SET status='REVOKED',revision=revision+1")) {
            fixture.reset(); fixture.sql(sql); List<Long> before = fixture.counts(); denied(() -> fixture.service.activate(fixture.command("event", Decision.ACCEPT), new ActivationCaller(fixture.account, Channel.WEB, WorkspaceType.ORGANIZATION, fixture.organization)), Code.FORBIDDEN); exact(before);
        }
        fixture.reset(); fixture.subject = fixture.subject(null, fixture.organization); fixture.revokeWhileWaiting(true); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
    }
    @Test
    void inactiveAccountIsForbiddenBeforeMissingSubjectDisclosure() throws Exception {
        fixture.sql("UPDATE customer_iam.customer_account SET status='DISABLED',revision=revision+1 WHERE id=?", fixture.account); denied(() -> fixture.service.activate(fixture.command(UUID.randomUUID(), "event", Decision.ACCEPT), fixture.caller()), Code.FORBIDDEN); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
        fixture.reset(); fixture.revokeWhileWaiting(false); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
    }
    @Test
    void foreignPersonalAndOrganizationSubjectsAreNotFound() throws Exception {
        for (UUID subject : List.of(fixture.subject(fixture.other, null), fixture.subject(null, fixture.organization), UUID.randomUUID())) denied(() -> fixture.service.activate(fixture.command(subject, "event", Decision.ACCEPT), fixture.caller()), Code.NOT_FOUND); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
        UUID foreignSubject = fixture.subject(null, fixture.otherOrganization);
        denied(() -> fixture.service.activate(fixture.command(foreignSubject, "foreign", Decision.ACCEPT), new ActivationCaller(fixture.account, Channel.WEB, WorkspaceType.ORGANIZATION, fixture.organization)), Code.NOT_FOUND);
        for (UUID subject : List.of(foreignSubject, UUID.randomUUID())) {
            var response = fixture.post("organization", subject, fixture.body("foreign", Decision.ACCEPT));
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.headers().firstValue("Content-Type").orElseThrow()).startsWith("application/problem+json");
            assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
            assertThat(response.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
            var problem = JsonMapper.builder().build().readTree(response.body());
            assertThat(problem.propertyNames()).containsExactlyInAnyOrder("type", "title", "status", "code", "traceId");
            assertThat(problem.path("code").asString()).isEqualTo("NOT_FOUND");
            assertThat(problem.path("status").asInt()).isEqualTo(404);
            assertThat(response.body()).doesNotContain(subject.toString(), fixture.organization.toString(), fixture.otherOrganization.toString(), fixture.account.toString());
            exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
        }
    }
    @Test
    void malformedContextAndMissingGraphAreServiceUnavailable() throws Exception {
        denied(() -> fixture.service.activate(fixture.command("event", Decision.ACCEPT), new ActivationCaller(fixture.account, Channel.WEB, WorkspaceType.PERSONAL, fixture.organization)), Code.SERVICE_UNAVAILABLE);
        var beans = new org.springframework.beans.factory.support.DefaultListableBeanFactory(); var composer = new com.onecall.core.customer_iam.internal.application.CustomerSubjectActivationCommandService(beans.getBeanProvider(CustomerSubjectActivationService.class)); denied(() -> composer.activate(new com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller(fixture.account, TrustedCustomerCaller.Channel.WEB, new com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace(com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.PERSONAL, null)), fixture.command("event", Decision.ACCEPT)), Code.SERVICE_UNAVAILABLE); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
    }
    @Test
    void missingDisabledUnpublishedOrMalformedPolicyHasNoEffects() throws Exception {
        for (int state = 0; state < 4; state++) {
            fixture.reset(); if (state == 0) fixture.sql("TRUNCATE entitlement_metering.trial_policy_version CASCADE"); else fixture.policy(state == 3 ? "invalid" : "valid", state != 1, state != 2);
            denied(() -> fixture.accept("event"), Code.CONSENT_REQUIRED); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
        }
    }
    @Test
    void stalePolicyPromptOrContentHasNoConsentOrGrant() throws Exception {
        var base = fixture.command("event", Decision.ACCEPT);
        for (int field = 0; field < 3; field++) { var changed = new ActivationCommand(base.subjectId(), base.activationEventId(), base.decision(), base.policyCode(), field == 0 ? "0" : base.policyVersion(), field == 1 ? "2" : base.promptVersion(), field == 2 ? "b".repeat(64) : base.contentSha256(), base.traceId()); denied(() -> fixture.service.activate(changed, fixture.caller()), Code.CONSENT_REQUIRED); }
        exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
    }
    @Test
    void declineCommitsOneRefusalAndReplaysSameProblem() throws Exception {
        fixture.sql("REVOKE EXECUTE ON FUNCTION audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text) FROM activation_consent_role");
        try { for (int replay = 0; replay < 2; replay++) denied(() -> fixture.service.activate(fixture.command("event", Decision.DECLINE), fixture.caller()), Code.ACTIVATION_CONSENT_DECLINED); exact(List.of(1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)); }
        finally { fixture.sql("GRANT EXECUTE ON FUNCTION audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text) TO activation_consent_role"); }
        String snapshot = fixture.scalar("SELECT snapshot::text FROM activation_consent.consent_record"); String hash = fixture.scalar("SELECT snapshot_sha256 FROM activation_consent.consent_record"); assertThat(ActivationSemantics.decode(snapshot, hash, "FIRST-TRIAL", "1", "1", false).revision()).isNull();
    }
    @Test
    void acceptReplayReturnsOriginalIdsOutcomeUnitsAndExpiry() throws Exception {
        var first = fixture.accept("event"); List<Long> before = fixture.counts(); assertThat(fixture.accept("event")).isEqualTo(first); exact(before);
        String snapshot = fixture.scalar("SELECT snapshot::text FROM activation_consent.consent_record"); String hash = fixture.scalar("SELECT snapshot_sha256 FROM activation_consent.consent_record"); var descriptor = ActivationSemantics.decode(snapshot, hash, "FIRST-TRIAL", "1", "1", true); assertThat(descriptor.transitionId()).isEqualTo(first.transitionId()); assertThat(descriptor.policyId()).isEqualTo(fixture.policy); assertThat(descriptor.outcome()).isEqualTo("GRANTED");
    }
    @Test
    void sameEventSemanticMismatchHasNoAdditionalEffects() throws Exception {
        fixture.accept("event"); List<Long> before = fixture.counts(); denied(() -> fixture.service.activate(fixture.command("event", Decision.DECLINE), fixture.caller()), Code.CONSENT_REQUIRED); exact(before);
    }
    @Test
    void sameEventAcrossAccountsOrSubjectsFailsWithoutDisclosure() throws Exception {
        fixture.accept("event"); UUID otherSubject = fixture.subject(fixture.account, null); List<Long> before = fixture.counts(); denied(() -> fixture.service.activate(fixture.command(otherSubject, "event", Decision.ACCEPT), fixture.caller()), Code.CONSENT_REQUIRED); exact(before);
        fixture.reset(); UUID foreign = fixture.subject(fixture.other, null); var results = fixture.concurrent(fixture.command("collision", Decision.ACCEPT), fixture.caller(), fixture.command(foreign, "collision", Decision.ACCEPT), new ActivationCaller(fixture.other, Channel.WEB, WorkspaceType.PERSONAL, null));
        assertThat(results.stream().filter(result -> result instanceof ActivationResult)).hasSize(1); assertThat(results.stream().filter(result -> result instanceof SubjectActivationException)).singleElement().isInstanceOfSatisfying(SubjectActivationException.class, failure -> assertThat(failure.code()).isEqualTo(Code.CONSENT_REQUIRED)); exact(List.of(1L, 1L, 1L, 1L, 1L, 1L, 2L, 0L));
    }
    @Test
    void laterActiveEventUsesLatestAnchorWithoutTransitionOrTrialDuplication() throws Exception {
        var first = fixture.accept("first"); fixture.move("SUSPENDED"); fixture.move("ACTIVE"); var laterCommand = fixture.command("later", Decision.ACCEPT); var later = fixture.service.activate(laterCommand, fixture.caller()); assertThat(later.transitionId()).isNotEqualTo(first.transitionId()); assertThat(later.activationRevision()).isEqualTo(4); assertThat(later.trial()).isEqualTo(first.trial()); assertThat(later.trialOutcome()).isEqualTo("ALREADY_GRANTED");
        fixture.move("SUSPENDED"); fixture.move("ACTIVE"); fixture.policy("valid", true, true); List<Long> before = fixture.counts(); assertThat(fixture.service.activate(laterCommand, fixture.caller())).isEqualTo(later); exact(before); assertThat(fixture.count("platform.outbox_event")).isEqualTo(2);
    }
    @Test
    void changedPolicyNeverChangesPermanentTrialUnitsOrExpiry() throws Exception {
        var first = fixture.accept("first"); fixture.policy("larger", true, true); fixture.sql("UPDATE entitlement_metering.entitlement_balance SET consumed_units=granted_units,status='EXHAUSTED',revision=revision+1,updated_at=clock_timestamp()"); var later = fixture.accept("later"); assertThat(later.trial()).isEqualTo(first.trial()); assertThat(later.trial().grantedSeconds()).isEqualTo(600); exact(List.of(2L, 1L, 1L, 1L, 1L, 1L, 2L, 0L));
    }
    @Test
    void suspendedCannotBeReactivatedByNewCustomerEvent() throws Exception {
        fixture.accept("first"); fixture.move("SUSPENDED"); List<Long> before = fixture.counts(); denied(() -> fixture.accept("new"), Code.FORBIDDEN); exact(before); denied(() -> fixture.service.activate(fixture.command("refusal", Decision.DECLINE), fixture.caller()), Code.ACTIVATION_CONSENT_DECLINED); assertThat(fixture.count("activation_consent.consent_record")).isEqualTo(2); assertThat(fixture.scalar("SELECT status FROM activation_consent.service_activation")).isEqualTo("SUSPENDED");
    }
    @Test
    void historicalReplayAfterPolicyChangeOrSuspensionHasNoMutation() throws Exception {
        var command = fixture.command("event", Decision.ACCEPT); var first = fixture.service.activate(command, fixture.caller()); fixture.move("SUSPENDED"); fixture.policy("valid", true, true); List<Long> before = fixture.counts(); assertThat(fixture.service.activate(command, fixture.caller())).isEqualTo(first); exact(before);
        for (int corruption = 0; corruption < 11; corruption++) {
            fixture.reset(); fixture.accept("event"); String snapshot = fixture.scalar("SELECT snapshot::text FROM activation_consent.consent_record");
            @SuppressWarnings("unchecked") Map<String, Object> value = JsonMapper.builder().build().readValue(snapshot, Map.class);
            if (corruption == 0) value.put("extra", "not-allowed"); if (corruption == 1) value.put("schemaVersion", 2); if (corruption == 2) value.put("activationResult", null);
            if (corruption == 4) value.remove("promptVersion"); if (corruption == 5) value.put("schemaVersion", "1");
            if (corruption >= 6 && corruption <= 9) {
                @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) value.get("activationResult");
                if (corruption == 6) result.put("extra", "not-allowed"); if (corruption == 7) result.put("activationRevision", "2");
                if (corruption == 8) result.put("transitionId", "not-a-uuid"); if (corruption == 9) result.put("trialOutcome", "UNKNOWN");
            }
            if (corruption == 10) value.put("contentSha256", "A".repeat(64));
            String modified = ActivationSemantics.canonical(value), hash = corruption == 3 ? "0".repeat(64) : ActivationSemantics.sha(modified);
            fixture.sql("ALTER TABLE activation_consent.consent_record DISABLE TRIGGER trg_consent_record_immutable");
            try { fixture.sql("UPDATE activation_consent.consent_record SET snapshot=?::jsonb,snapshot_sha256=?", modified, hash); }
            finally { fixture.sql("ALTER TABLE activation_consent.consent_record ENABLE TRIGGER trg_consent_record_immutable"); }
            List<Long> frozen = fixture.counts(); denied(() -> fixture.accept("event"), Code.INTERNAL); exact(frozen);
        }
        fixture.reset(); denied(() -> fixture.service.activate(fixture.command("refusal", Decision.DECLINE), fixture.caller()), Code.ACTIVATION_CONSENT_DECLINED);
        @SuppressWarnings("unchecked") Map<String, Object> refusal = JsonMapper.builder().build().readValue(fixture.scalar("SELECT snapshot::text FROM activation_consent.consent_record"), Map.class);
        refusal.put("activationResult", Map.of("activationStatus", "ACTIVE", "activationRevision", 2, "transitionId", UUID.randomUUID().toString(), "trialOutcome", "GRANTED", "trialPolicyVersionId", fixture.policy.toString()));
        String invalidRefusal = ActivationSemantics.canonical(refusal);
        fixture.sql("ALTER TABLE activation_consent.consent_record DISABLE TRIGGER trg_consent_record_immutable");
        try { fixture.sql("UPDATE activation_consent.consent_record SET snapshot=?::jsonb,snapshot_sha256=?", invalidRefusal, ActivationSemantics.sha(invalidRefusal)); }
        finally { fixture.sql("ALTER TABLE activation_consent.consent_record ENABLE TRIGGER trg_consent_record_immutable"); }
        List<Long> refused = fixture.counts(); denied(() -> fixture.service.activate(fixture.command("refusal", Decision.DECLINE), fixture.caller()), Code.INTERNAL); exact(refused);
        fixture.reset(); fixture.accept("event"); fixture.sql("DELETE FROM entitlement_metering.entitlement_balance"); List<Long> incomplete = fixture.counts(); denied(() -> fixture.accept("event"), Code.INTERNAL); exact(incomplete);
        fixture.reset(); fixture.accept("event"); fixture.sql("ALTER TABLE activation_consent.service_activation_transition DISABLE TRIGGER trg_activation_transition_immutable");
        try { fixture.sql("DELETE FROM activation_consent.service_activation_transition"); }
        finally { fixture.sql("ALTER TABLE activation_consent.service_activation_transition ENABLE TRIGGER trg_activation_transition_immutable"); }
        List<Long> missingAnchor = fixture.counts(); denied(() -> fixture.accept("event"), Code.INTERNAL); exact(missingAnchor);
    }
    @Test
    void concurrentSameEventAcceptsConvergeOnOneFrozenResult() throws Exception {
        var results = fixture.concurrent(fixture.command("event", Decision.ACCEPT), fixture.command("event", Decision.ACCEPT)); assertThat(results.getFirst()).isInstanceOf(ActivationResult.class).isEqualTo(results.getLast()); exact(List.of(1L, 1L, 1L, 1L, 1L, 1L, 2L, 0L));
    }
    @Test
    void concurrentDifferentEventsShareOneTransitionAndPermanentTrial() throws Exception {
        var results = fixture.concurrent(fixture.command("left", Decision.ACCEPT), fixture.command("right", Decision.ACCEPT)); assertThat(results).allMatch(result -> result instanceof ActivationResult); var left = (ActivationResult) results.getFirst(); var right = (ActivationResult) results.getLast(); assertThat(left.transitionId()).isEqualTo(right.transitionId()); assertThat(left.trial()).isEqualTo(right.trial()); exact(List.of(2L, 1L, 1L, 1L, 1L, 1L, 2L, 0L));
    }
    @Test
    void concurrentDeclineReplayAndSemanticConflictNeverAccept() throws Exception {
        var results = fixture.concurrent(fixture.command("event", Decision.DECLINE), fixture.command("event", Decision.DECLINE)); assertThat(results).allSatisfy(result -> assertThat(result).isInstanceOfSatisfying(SubjectActivationException.class, failure -> assertThat(failure.code()).isEqualTo(Code.ACTIVATION_CONSENT_DECLINED)));
        denied(() -> fixture.accept("event"), Code.CONSENT_REQUIRED); exact(List.of(1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
    }
    @Test
    void trialOutboxAndCommitFaultsRespectExactDecisionWriteSets() throws Exception {
        for (String point : List.of("appendConsent", "advance", "appendTransition", "appendOutbox")) { fixture.reset(); var service = fixture.with(fixture.decorated(point), fixture.trials); denied(() -> service.activate(fixture.command("event", Decision.ACCEPT), fixture.caller()), Code.INTERNAL); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)); }
        for (Decision decision : Decision.values()) { fixture.reset(); denied(() -> fixture.commitFailure().activate(fixture.command("event", decision), fixture.caller()), Code.INTERNAL); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)); }
        fixture.reset(); var consentFault = fixture.with(fixture.decorated("appendConsent"), fixture.trials); denied(() -> consentFault.activate(fixture.command("refusal-fault", Decision.DECLINE), fixture.caller()), Code.INTERNAL); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
        ActivationTrialService afterFunction = new ActivationTrialService() {
            public PolicyResult readCurrentPolicy() { return fixture.trials.readCurrentPolicy(); }
            public Optional<OriginalTrial> readOriginalTrial(UUID subject) { return fixture.trials.readOriginalTrial(subject); }
            public TrialIssueResult issueFirstTrial(TrialIssueCommand command) { fixture.trials.issueFirstTrial(command); throw new IllegalStateException("AFTER_REAL_TRIAL_OUTBOX"); }
        };
        var trialFault = fixture.with(fixture.repository, afterFunction); denied(() -> trialFault.activate(fixture.command("trial-fault", Decision.ACCEPT), fixture.caller()), Code.INTERNAL); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
        var refusal = fixture.with(fixture.decorated("appendOutbox"), fixture.trials); denied(() -> refusal.activate(fixture.command("refusal", Decision.DECLINE), fixture.caller()), Code.ACTIVATION_CONSENT_DECLINED); exact(List.of(1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L));
    }
    @Test
    void policyDisableAroundFunctionSelectionHasFrozenLinearization() throws Exception {
        for (boolean before : List.of(true, false)) {
            fixture.reset(); ActivationTrialService owner = new ActivationTrialService() {
                public PolicyResult readCurrentPolicy() { return fixture.trials.readCurrentPolicy(); }
                public Optional<OriginalTrial> readOriginalTrial(UUID subject) { return fixture.trials.readOriginalTrial(subject); }
                public TrialIssueResult issueFirstTrial(TrialIssueCommand command) {
                    try { if (before) fixture.sql("UPDATE entitlement_metering.trial_policy_version SET enabled=false,revision=revision+1 WHERE enabled"); var result = fixture.trials.issueFirstTrial(command); if (!before) fixture.sql("UPDATE entitlement_metering.trial_policy_version SET enabled=false,revision=revision+1 WHERE enabled"); return result; }
                    catch (java.sql.SQLException failure) { throw new IllegalStateException(failure); } catch (RuntimeException failure) { throw failure; } catch (Exception failure) { throw new IllegalStateException(failure); }
                }
            };
            var service = fixture.with(fixture.repository, owner); if (before) { denied(() -> service.activate(fixture.command("event", Decision.ACCEPT), fixture.caller()), Code.CONSENT_REQUIRED); exact(List.of(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L)); } else { assertThat(service.activate(fixture.command("event", Decision.ACCEPT), fixture.caller()).trialOutcome()).isEqualTo("GRANTED"); exact(List.of(1L, 1L, 1L, 1L, 1L, 1L, 2L, 0L)); }
        }
    }
    @Test
    void storedResultsProblemsAuditOutboxAndLogsAreRedacted() throws Exception {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var captured = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>(); captured.start(); logger.addAppender(captured);
        try {
            var response = fixture.post("personal", fixture.subject, fixture.body("event", Decision.ACCEPT)); assertThat(response.statusCode()).isEqualTo(200); var root = JsonMapper.builder().build().readTree(response.body()); assertThat(root.propertyNames()).containsExactlyInAnyOrder("activationStatus", "activationRevision", "transitionId", "consentId", "trialOutcome", "trial"); assertThat(fixture.count("audit.audit_fact")).isZero();
            assertThat(root.path("activationStatus").isString()).isTrue();
            assertThat(root.path("activationStatus").asString()).isEqualTo("ACTIVE");
            assertThat(root.path("activationRevision").isIntegralNumber()).isTrue();
            assertThat(root.path("activationRevision").asLong()).isPositive();
            for (String field : List.of("transitionId", "consentId")) {
                assertThat(root.path(field).isString()).isTrue();
                assertThat(UUID.fromString(root.path(field).asString()).toString()).isEqualTo(root.path(field).asString());
            }
            assertThat(root.path("trialOutcome").isString()).isTrue();
            assertThat(root.path("trialOutcome").asString()).isEqualTo("GRANTED");
            assertThat(root.path("trial").propertyNames()).containsExactlyInAnyOrder("grantedSeconds", "expiresAt");
            assertThat(root.path("trial").path("grantedSeconds").isIntegralNumber()).isTrue();
            assertThat(root.path("trial").path("grantedSeconds").asLong()).isPositive();
            assertThat(root.path("trial").path("expiresAt").isString()).isTrue();
            String expiresAt = root.path("trial").path("expiresAt").asString();
            assertThat(java.time.Instant.parse(expiresAt).toString()).isEqualTo(expiresAt);
            var problem = fixture.post("personal", fixture.subject, fixture.body("refusal", Decision.DECLINE)); assertThat(problem.statusCode()).isEqualTo(409);
            var payload = JsonMapper.builder().build().readTree(fixture.scalar("SELECT payload_redacted::text FROM platform.outbox_event WHERE event_domain='ACTIVATION_STATUS'")); assertThat(payload.propertyNames()).containsExactlyInAnyOrder("activationId", "transitionId", "subjectId", "fromStatus", "toStatus", "fromRevision", "toRevision", "sourceChannel", "sourceActionDomain", "sourceActionEventId", "statusEventDomain", "statusEventId", "occurredAt");
            String logs = captured.list.stream().map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage).collect(java.util.stream.Collectors.joining("\n"));
            for (String canary : List.of(fixture.password, fixture.account.toString(), fixture.policy.toString(), "requestFingerprint", "snapshotSha256")) assertThat((response.body() + problem.body() + logs).contains(canary)).isFalse();
            var descriptor = JsonMapper.builder().build().readTree(fixture.scalar("SELECT snapshot::text FROM activation_consent.consent_record WHERE event_id='event'")); assertThat(descriptor.propertyNames()).containsExactlyInAnyOrder("schemaVersion", "policyCode", "policyVersion", "promptVersion", "contentSha256", "activationResult"); assertThat(fixture.count("audit.audit_fact")).isZero();
        } finally { logger.detachAppender(captured); captured.stop(); }
    }
}
