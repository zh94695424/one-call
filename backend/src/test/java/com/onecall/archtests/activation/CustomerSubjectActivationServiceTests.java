package com.onecall.archtests.activation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.*;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code;
import com.onecall.core.activation_consent.internal.command.*;
import com.onecall.core.activation_consent.internal.command.SubjectActivationRepository.*;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService;
import com.onecall.core.entitlement_metering.activationtrial.ActivationTrialService.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

class CustomerSubjectActivationServiceTests {
    static final UUID ACCOUNT = UUID.randomUUID(), SUBJECT = UUID.randomUUID(), POLICY = UUID.randomUUID();
    static final ActivationCaller PERSONAL = new ActivationCaller(ACCOUNT, Channel.WEB, WorkspaceType.PERSONAL, null);
    static ActivationCommand command(String event, Decision decision) { return new ActivationCommand(SUBJECT, event, decision, "FIRST-TRIAL", "1", "1", "a".repeat(64), UUID.randomUUID()); }
    static final class Harness {
        final DSLContext context = mock(DSLContext.class);
        final SubjectActivationRepository repository = mock(SubjectActivationRepository.class);
        final ActivationTrialService trials = mock(ActivationTrialService.class);
        final Connection connection = mock(Connection.class);
        final Map<String, Consent> consents = new HashMap<>();
        final Map<UUID, Transition> transitions = new HashMap<>();
        final List<String> locks = new ArrayList<>();
        final DefaultCustomerSubjectActivationService service;
        Head head;
        Subject subject = new Subject(SUBJECT, ACCOUNT, null, 1);
        Access access = new Access("ACTIVE", null, null, null);
        OriginalTrial original;
        int outbox;
        Harness() throws Exception {
            DataSource source = mock(DataSource.class);
            when(source.getConnection()).thenReturn(connection);
            when(connection.getAutoCommit()).thenReturn(true);
            var manager = new DataSourceTransactionManager(source);
            manager.setRollbackOnCommitFailure(true);
            var transaction = new TransactionTemplate(manager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            when(repository.lockHead(context, SUBJECT)).thenAnswer(call -> Optional.ofNullable(head));
            when(repository.subject(context, SUBJECT)).thenAnswer(call -> Optional.ofNullable(subject));
            when(repository.access(eq(context), any())).thenAnswer(call -> access);
            when(repository.consent(eq(context), anyString())).thenAnswer(call -> Optional.ofNullable(consents.get(call.getArgument(1))));
            when(repository.actionTransition(eq(context), anyString())).thenAnswer(call -> transitions.values().stream().filter(row -> row.actionEvent().equals(call.getArgument(1))).findFirst());
            when(repository.transition(eq(context), any())).thenAnswer(call -> Optional.ofNullable(transitions.get(call.getArgument(1))));
            when(repository.latestTransition(eq(context), any())).thenAnswer(call -> transitions.values().stream().filter(row -> row.toRevision() == head.revision()).findFirst());
            doAnswer(call -> { locks.add(call.getArgument(1)); return null; }).when(repository).lock(eq(context), anyString());
            doAnswer(call -> { Consent consent = call.getArgument(1); consents.put(consent.eventId(), consent); return null; }).when(repository).appendConsent(eq(context), any());
            doAnswer(call -> { head = call.getArgument(1); return null; }).when(repository).createHead(eq(context), any(), any());
            when(repository.advance(eq(context), any(), any())).thenAnswer(call -> { Head before = call.getArgument(1); head = new Head(before.id(), before.subjectId(), "ACTIVE", before.revision() + 1); return true; });
            doAnswer(call -> { Transition transition = call.getArgument(1); transitions.put(transition.id(), transition); return null; }).when(repository).appendTransition(eq(context), any());
            doAnswer(call -> { outbox++; return null; }).when(repository).appendOutbox(eq(context), any());
            when(trials.readCurrentPolicy()).thenReturn(new PolicyResult(new CurrentPolicy(POLICY, 1, "FIRST-TRIAL", "1", "1", "a".repeat(64), "b".repeat(64))));
            when(trials.readOriginalTrial(SUBJECT)).thenAnswer(call -> Optional.ofNullable(original));
            when(trials.issueFirstTrial(any())).thenAnswer(call -> {
                TrialIssueCommand issue = call.getArgument(0);
                if (original != null) return new TrialIssueResult("ALREADY_GRANTED", original);
                Instant time = Instant.parse("2026-11-13T00:00:00Z");
                original = new OriginalTrial(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SUBJECT, issue.policyVersionId(), "ACTIVATION", issue.activationEventId(), issue.requestFingerprint(), 600, time, time.plusSeconds(86400));
                return new TrialIssueResult("GRANTED", original);
            });
            service = new DefaultCustomerSubjectActivationService(transaction, context, repository, trials);
        }
        ActivationResult accept(String event) { return service.activate(command(event, Decision.ACCEPT), PERSONAL); }
        void denied(ActivationCommand command, ActivationCaller caller, Code code) {
            assertThatThrownBy(() -> service.activate(command, caller)).isInstanceOfSatisfying(SubjectActivationException.class, failure -> assertThat(failure.code()).isEqualTo(code));
        }
        void noWrites() { assertThat(consents).isEmpty(); assertThat(transitions).isEmpty(); assertThat(outbox).isZero(); verify(trials, never()).issueFirstTrial(any()); }
    }
    @Test
    void personalAccessRequiresActiveAccountAndMatchingOwner() throws Exception {
        var harness = new Harness(); harness.access = new Access("DISABLED", null, null, null); harness.denied(command("event", Decision.ACCEPT), PERSONAL, Code.FORBIDDEN); harness.noWrites();
        harness.access = new Access("ACTIVE", null, null, null); assertThat(harness.accept("event").activationStatus()).isEqualTo("ACTIVE");
    }
    @Test
    void organizationAccessRequiresActiveWorkspaceMembershipAndAllowedRole() throws Exception {
        UUID organization = UUID.randomUUID(); var caller = new ActivationCaller(ACCOUNT, Channel.MINIAPP, WorkspaceType.ORGANIZATION, organization);
        for (String role : List.of("ADMINISTRATOR", "MEMBER", "EVIDENCE_MANAGER", "AUDITOR", "OWNER")) {
            var harness = new Harness(); harness.subject = new Subject(SUBJECT, null, organization, 1); harness.access = new Access("ACTIVE", "ACTIVE", "ACTIVE", role);
            if (role.equals("OWNER")) { harness.denied(command("event", Decision.ACCEPT), caller, Code.FORBIDDEN); harness.noWrites(); }
            else assertThat(harness.service.activate(command("event", Decision.ACCEPT), caller).activationStatus()).isEqualTo("ACTIVE");
        }
        for (Access invalid : List.of(new Access("ACTIVE", "DISABLED", "ACTIVE", "MEMBER"), new Access("ACTIVE", "ACTIVE", "REVOKED", "MEMBER"))) {
            var harness = new Harness(); harness.access = invalid; harness.denied(command("event", Decision.ACCEPT), caller, Code.FORBIDDEN); harness.noWrites();
        }
    }
    @Test
    void lifecycleFailurePrecedesNonDisclosingSubjectLookup() throws Exception {
        var harness = new Harness(); harness.subject = null; harness.access = new Access(null, null, null, null); harness.denied(command("event", Decision.ACCEPT), PERSONAL, Code.FORBIDDEN); harness.noWrites();
    }
    @Test
    void missingAndForeignSubjectsReturnNotFoundWithoutEffects() throws Exception {
        for (Subject subject : Arrays.asList(null, new Subject(SUBJECT, UUID.randomUUID(), null, 1), new Subject(SUBJECT, null, UUID.randomUUID(), 1))) {
            var harness = new Harness(); harness.subject = subject; harness.denied(command("event", Decision.ACCEPT), PERSONAL, Code.NOT_FOUND); harness.noWrites();
        }
    }
    @Test
    void currentPolicyPromptAndContentMustMatchExactly() throws Exception {
        for (Decision decision : Decision.values()) for (int field = 0; field < 5; field++) {
            var harness = new Harness(); var base = command("event", decision);
            if (field == 4) when(harness.trials.readCurrentPolicy()).thenReturn(new PolicyResult(null));
            var changed = new ActivationCommand(SUBJECT, "event", decision, field == 0 ? "other" : base.policyCode(), field == 1 ? "01" : base.policyVersion(), field == 2 ? "other" : base.promptVersion(), field == 3 ? "b".repeat(64) : base.contentSha256(), base.traceId());
            harness.denied(changed, PERSONAL, Code.CONSENT_REQUIRED); harness.noWrites();
        }
    }
    @Test
    void acceptCreatesOneRevisionTransitionAndIssuesTrialAtomically() throws Exception {
        for (int revision : List.of(0, 7)) {
            var harness = new Harness(); if (revision > 0) harness.head = new Head(UUID.randomUUID(), SUBJECT, "INACTIVE", revision);
            var result = harness.accept("event"); assertThat(result.activationRevision()).isEqualTo(revision == 0 ? 2 : 8); assertThat(result.trialOutcome()).isEqualTo("GRANTED");
            assertThat(harness.consents).hasSize(1); assertThat(harness.transitions).hasSize(1); assertThat(harness.outbox).isEqualTo(1); verify(harness.connection).commit();
        }
    }
    @Test
    void declineCommitsRefusalBeforeExceptionMapping() throws Exception {
        var harness = new Harness(); harness.denied(command("event", Decision.DECLINE), PERSONAL, Code.ACTIVATION_CONSENT_DECLINED);
        verify(harness.connection).commit(); verify(harness.connection, never()).rollback(); assertThat(harness.head).isNull(); assertThat(harness.transitions).isEmpty(); assertThat(harness.outbox).isZero();
        verify(harness.trials, never()).readOriginalTrial(any()); verify(harness.trials, never()).issueFirstTrial(any());
        var consent = harness.consents.get("event"); assertThat(consent.snapshot()).contains("\"activationResult\":null"); assertThat(ActivationSemantics.sha(consent.snapshot())).isEqualTo(consent.snapshotSha256());
    }
    @Test
    void exactAcceptReplayReturnsFirstFrozenResultWithoutWrites() throws Exception {
        var harness = new Harness(); var first = harness.accept("event"); when(harness.trials.readCurrentPolicy()).thenThrow(new AssertionError("must not consult current policy"));
        assertThat(harness.accept("event")).isEqualTo(first); assertThat(harness.consents).hasSize(1); assertThat(harness.transitions).hasSize(1); assertThat(harness.outbox).isEqualTo(1); verify(harness.trials, times(1)).issueFirstTrial(any());
    }
    @Test
    void exactDeclineReplayReturnsSameRefusalWithoutWrites() throws Exception {
        var harness = new Harness(); harness.denied(command("event", Decision.DECLINE), PERSONAL, Code.ACTIVATION_CONSENT_DECLINED);
        harness.denied(command("event", Decision.DECLINE), PERSONAL, Code.ACTIVATION_CONSENT_DECLINED); assertThat(harness.consents).hasSize(1); verify(harness.trials, never()).readOriginalTrial(any()); verify(harness.trials, times(1)).readCurrentPolicy();
    }
    @Test
    void sameEventSemanticOrContentMismatchReturnsConsentRequired() throws Exception {
        for (int field = 0; field < 5; field++) {
            var harness = new Harness(); harness.accept("event"); var base = command("event", Decision.ACCEPT);
            var changed = new ActivationCommand(SUBJECT, "event", field == 0 ? Decision.DECLINE : Decision.ACCEPT, field == 1 ? "other" : base.policyCode(), field == 2 ? "2" : base.policyVersion(), field == 3 ? "2" : base.promptVersion(), field == 4 ? "b".repeat(64) : base.contentSha256(), base.traceId());
            harness.denied(changed, PERSONAL, Code.CONSENT_REQUIRED); assertThat(harness.consents).hasSize(1);
        }
    }
    @Test
    void laterActiveEventFreezesLatestAnchorAndOriginalTrial() throws Exception {
        var harness = new Harness(); var first = harness.accept("first"); var later = harness.accept("later");
        assertThat(later.transitionId()).isEqualTo(first.transitionId()); assertThat(later.trial()).isEqualTo(first.trial()); assertThat(later.trialOutcome()).isEqualTo("ALREADY_GRANTED");
        harness.head = new Head(harness.head.id(), SUBJECT, "SUSPENDED", 3); assertThat(harness.accept("later")).isEqualTo(later); assertThat(harness.consents).hasSize(2); assertThat(harness.transitions).hasSize(1); assertThat(harness.outbox).isEqualTo(1);
    }
    @Test
    void suspendedRejectsNewAcceptButNeverMutatesOnHistoricalReplay() throws Exception {
        var harness = new Harness(); var first = harness.accept("first"); harness.head = new Head(harness.head.id(), SUBJECT, "SUSPENDED", 3);
        harness.denied(command("new", Decision.ACCEPT), PERSONAL, Code.FORBIDDEN); assertThat(harness.accept("first")).isEqualTo(first);
        harness.denied(command("refusal", Decision.DECLINE), PERSONAL, Code.ACTIVATION_CONSENT_DECLINED); assertThat(harness.consents).hasSize(2); assertThat(harness.transitions).hasSize(1);
    }
    @Test
    void missingAnchorTrialOrCorruptReplayDescriptorFailsInternal() throws Exception {
        for (int corruption = 0; corruption < 4; corruption++) {
            var harness = new Harness(); harness.accept("event"); var consent = harness.consents.get("event");
            if (corruption == 0) harness.transitions.clear();
            if (corruption == 1) harness.original = null;
            if (corruption >= 2) harness.consents.put("event", new Consent(consent.id(), consent.subjectId(), consent.purpose(), consent.channel(), consent.eventDomain(), consent.eventId(), consent.policyCode(), consent.policyVersion(), consent.promptVersion(), consent.action(), consent.sourceType(), consent.sourceRef(), consent.accountId(), null, null, corruption == 2 ? "{}" : consent.snapshot(), "0".repeat(64), consent.fingerprint()));
            harness.denied(command("event", Decision.ACCEPT), PERSONAL, Code.INTERNAL); assertThat(harness.consents).hasSize(1);
        }
    }
    @Test
    void roleAndLockOrderIsExactBeforeRereadsAndWrites() throws Exception {
        var harness = new Harness(); UUID organization = UUID.randomUUID(); harness.subject = new Subject(SUBJECT, null, organization, 1); harness.access = new Access("ACTIVE", "ACTIVE", "ACTIVE", "AUDITOR");
        harness.service.activate(command("event", Decision.ACCEPT), new ActivationCaller(ACCOUNT, Channel.WEB, WorkspaceType.ORGANIZATION, organization));
        assertThat(harness.locks).containsExactly("subject_number.subject:" + SUBJECT, "organization_access.organization:" + organization, "organization_access.membership:" + organization + ":" + ACCOUNT, "customer_iam.account:" + ACCOUNT);
        var ordered = inOrder(harness.repository, harness.trials); ordered.verify(harness.repository).enter(harness.context); ordered.verify(harness.repository).lock(harness.context, "subject_number.subject:" + SUBJECT); ordered.verify(harness.repository).lockHead(harness.context, SUBJECT); ordered.verify(harness.repository).subject(harness.context, SUBJECT);
        ordered.verify(harness.repository).access(harness.context, new ActivationCaller(ACCOUNT, Channel.WEB, WorkspaceType.ORGANIZATION, organization)); ordered.verify(harness.trials).readCurrentPolicy(); ordered.verify(harness.repository).assertRole(harness.context);
    }
    @Test
    void canonicalFingerprintsAndSourceIdentitiesMatchFrozenVectors() {
        var command = command("event", Decision.DECLINE); String refusal = ActivationSemantics.descriptor(command, null); assertThat(ActivationSemantics.sha(refusal)).isEqualTo("c3ce0a81bbe9da0c148e6ab01d9d5cb7ddeca47cb4c0218475ac012423bddc65");
        String accepted = ActivationSemantics.descriptor(command, new ActivationSemantics.Descriptor(2, UUID.fromString("11111111-1111-1111-1111-111111111111"), "GRANTED", UUID.fromString("22222222-2222-2222-2222-222222222222"), "a".repeat(64)));
        assertThat(ActivationSemantics.sha(accepted)).isEqualTo("9673ed6bb19b601880627c428ee20d5b66cf7ad119a0fcf1a55d5f92d3709a22");
        assertThat(ActivationSemantics.canonical(ActivationSemantics.fields("\ue000", 1, "😀", "雪\n\u0001", "nullable", null))).isEqualTo("{\"nullable\":null,\"😀\":\"雪\\n\\u0001\",\"\ue000\":1}");
        assertThat(ActivationSemantics.sourceRef(command, PERSONAL)).hasSize(75); assertThat(ActivationSemantics.consentFingerprint(command, PERSONAL)).matches("[0-9a-f]{64}");
    }
    @Test
    void trialOutboxAndCommitFailuresRespectExactDecisionWriteSets() throws Exception {
        var harness = new Harness(); doThrow(new RuntimeException("PRIVATE")).when(harness.trials).issueFirstTrial(any()); harness.denied(command("event", Decision.ACCEPT), PERSONAL, Code.INTERNAL); verify(harness.connection).rollback(); verify(harness.connection, never()).commit();
        var refusal = new Harness(); doThrow(new SQLException("PRIVATE", "08006")).when(refusal.connection).commit(); refusal.denied(command("refusal", Decision.DECLINE), PERSONAL, Code.SERVICE_UNAVAILABLE); verify(refusal.connection).rollback(); verify(refusal.trials, never()).issueFirstTrial(any());
        for (String reason : List.of("TRIAL_POLICY_NOT_FOUND", "TRIAL_ACTION_CONFLICT")) {
            for (String message : List.of(reason, "ERROR: " + reason + "\n  Where: PRIVATE_CONTEXT", "SQL [PRIVATE];\nERROR: " + reason + "\n  Where: PRIVATE_CONTEXT")) {
                var database = new SQLException(message, "P0001");
                for (SQLException cause : List.of(database, new SQLException("PRIVATE_WRAPPER", database))) {
                    var rejected = new Harness();
                    doThrow(new org.jooq.exception.DataAccessException("PRIVATE_SQL", cause)).when(rejected.trials).issueFirstTrial(any());
                    rejected.denied(command("event", Decision.ACCEPT), PERSONAL, Code.CONSENT_REQUIRED);
                    verify(rejected.connection).rollback(); verify(rejected.connection, never()).commit(); verify(rejected.repository, never()).appendOutbox(any(), any());
                }
            }
        }
        for (String reason : List.of("TRIAL_DATA_INTEGRITY", "TRIAL_ARGUMENT_INVALID", "TRIAL_CAMPAIGN_INVALID", "TRIAL_POLICY_NOT_FOUND_EXTRA", "PRIVATE TRIAL_POLICY_NOT_FOUND", "PRIVATE\n  Where: TRIAL_POLICY_NOT_FOUND")) {
            var rejected = new Harness();
            doThrow(new org.jooq.exception.DataAccessException("PRIVATE_SQL", new SQLException("ERROR: " + reason, "P0001"))).when(rejected.trials).issueFirstTrial(any());
            rejected.denied(command("event", Decision.ACCEPT), PERSONAL, Code.INTERNAL); verify(rejected.connection).rollback();
        }
        for (String state : List.of("42501", "08006", "28P01", "57014", "55P03", "XX000")) {
            var rejected = new Harness();
            doThrow(new org.jooq.exception.DataAccessException("PRIVATE_SQL", new SQLException("ERROR: TRIAL_POLICY_NOT_FOUND", state))).when(rejected.trials).issueFirstTrial(any());
            rejected.denied(command("event", Decision.ACCEPT), PERSONAL, "XX000".equals(state) ? Code.INTERNAL : Code.SERVICE_UNAVAILABLE);
            verify(rejected.connection).rollback();
        }
    }
    @Test
    void retryableFailuresRestartWholeAttemptAndExhaustAtThree() throws Exception {
        for (String state : List.of("40001", "40P01", "23505")) {
            var harness = new Harness(); doThrow(new RuntimeException(new SQLException("constraint \"uq_consent_event\"", state))).when(harness.repository).enter(harness.context);
            harness.denied(command("event", Decision.ACCEPT), PERSONAL, Code.SERVICE_UNAVAILABLE); verify(harness.connection, times(3)).rollback(); verify(harness.repository, times(3)).enter(harness.context); harness.noWrites();
        }
        var unknown = new Harness(); doThrow(new RuntimeException(new SQLException("constraint \"unapproved\"", "23505"))).when(unknown.repository).enter(unknown.context); unknown.denied(command("event", Decision.ACCEPT), PERSONAL, Code.INTERNAL); verify(unknown.connection).rollback();
    }
    @Test
    void unknownFailuresAndDomainStringsNeverEscapePublicProblems() throws Exception {
        var harness = new Harness(); doThrow(new RuntimeException("PRIVATE_SQL_PASSWORD")).when(harness.repository).enter(harness.context);
        assertThatThrownBy(() -> harness.accept("event")).isInstanceOf(SubjectActivationException.class).hasMessage("Subject activation failed").hasNoCause(); assertThat(command("PRIVATE", Decision.ACCEPT).toString()).doesNotContain("PRIVATE"); assertThat(PERSONAL.toString()).doesNotContain(ACCOUNT.toString());
    }
}
