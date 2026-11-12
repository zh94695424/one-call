package com.onecall.archtests.subjectnumber;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.core.subject_number.customerclaim.*;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.*;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException.Code;
import com.onecall.core.subject_number.internal.claim.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimRepository.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimProofVerifier.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimVerificationReference.Payload;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class CustomerSubjectClaimServiceTests {
    private org.springframework.boot.test.system.CapturedOutput output;
    @org.junit.jupiter.api.BeforeEach
    void captureOutput(org.springframework.boot.test.system.CapturedOutput captured) { output = captured; }
    static final class Harness {
        final UUID account = UUID.randomUUID(), subject = UUID.randomUUID(), binding = UUID.randomUUID();
        final String hmac = "a".repeat(64);
        final Instant time = Instant.parse("2026-11-13T00:00:00Z");
        final DSLContext context = mock(DSLContext.class);
        final SubjectClaimRepository repository = mock(SubjectClaimRepository.class);
        final ControlledSubjectClaimProviders providers = new ControlledSubjectClaimProviders();
        final List<NewClaim> rows = new ArrayList<>();
        final List<AuditDecision> audits = new ArrayList<>();
        final List<String> locks = new ArrayList<>();
        final AtomicInteger commits = new AtomicInteger(), rollbacks = new AtomicInteger(), begins = new AtomicInteger();
        final TransactionTemplate transaction;
        CustomerSubjectClaimService service;
        ClaimCommand command = command("request", VerificationMethod.SMS, "attempt");

        Harness() {
            transaction = new TransactionTemplate(new AbstractPlatformTransactionManager() {
                @Override protected Object doGetTransaction() { return new Object(); }
                @Override protected void doBegin(Object tx, TransactionDefinition definition) { begins.incrementAndGet(); }
                @Override protected void doCommit(DefaultTransactionStatus status) { commits.incrementAndGet(); }
                @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks.incrementAndGet(); rows.clear(); audits.clear(); }
            });
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            when(repository.findBindingIdentity(context, binding)).thenReturn(Optional.of(new BindingIdentity(binding, subject, hmac)));
            when(repository.lockSubject(context, subject)).thenReturn(Optional.of(new SubjectRow(subject, null, null, 7)));
            when(repository.lockBinding(context, binding)).thenReturn(Optional.of(new BindingRow(binding, subject, hmac, "ACTIVE", time.minusSeconds(10))));
            when(repository.lockRisks(context, binding)).thenReturn(List.of());
            when(repository.readAccount(context, account)).thenReturn(Optional.of(new AccountRow(account, "ACTIVE")));
            when(repository.findByRequestId(eq(context), anyString())).thenReturn(Optional.empty());
            when(repository.findAcceptedForAccount(context, subject, account)).thenReturn(Optional.empty());
            when(repository.readAcceptedOwners(context, subject)).thenReturn(List.of());
            when(repository.databaseTime(context)).thenReturn(time, time.plusMillis(1), time.plusMillis(2));
            when(repository.setUnownedOwner(eq(context), eq(subject), eq(account), eq(7), any())).thenReturn(1);
            doAnswer(call -> { rows.add(call.getArgument(1)); return null; }).when(repository).insertClaim(eq(context), any());
            doAnswer(call -> { audits.add(call.getArgument(1)); return null; }).when(repository).appendAudit(eq(context), any());
            doAnswer(call -> { locks.add(call.getArgument(1)); return null; }).when(repository).lock(eq(context), anyString());
            approve("attempt", VerificationMethod.SMS, "server-proof");
            providers.probe = () -> assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            service = new DefaultCustomerSubjectClaimService(transaction, context, repository, providers, providers);
        }
        ClaimCommand command(String key, VerificationMethod method, String proof) { return new ClaimCommand(subject, binding, method, proof, key, UUID.randomUUID()); }
        void approve(String attempt, VerificationMethod method, String reference) { providers.approve(attempt, new ApprovedProof(account, subject, binding, method, reference)); }
        Payload payload(String reference) { return new Payload(1, subject, binding, hmac, VerificationMethod.SMS, reference); }
        ClaimRow stored(ClaimStatus status) {
            providers.probe = () -> { };
            String envelope = providers.sealPayload(payload("server-proof"));
            providers.probe = () -> assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            return new ClaimRow(UUID.randomUUID(), subject, account, null, VerificationMethod.SMS, envelope, command.requestId(), status, time,
                    status == ClaimStatus.ACCEPTED ? time.plusMillis(1) : null);
        }
        void owner(UUID owner, UUID organization) { when(repository.lockSubject(context, subject)).thenReturn(Optional.of(new SubjectRow(subject, owner, organization, 7))); }
        void accepted(ClaimRow row) {
            owner(account, null);
            when(repository.findAcceptedForAccount(context, subject, account)).thenReturn(Optional.of(row));
            when(repository.readAcceptedOwners(context, subject)).thenReturn(List.of(new AcceptedOwnerRow(row.id(), account, null)));
        }
        ClaimResult run() { return service.create(command, new PersonalClaimCaller(account)); }
        void fails(Code code) { assertThatThrownBy(this::run).isInstanceOfSatisfying(SubjectClaimException.class, failure -> assertThat(failure.code()).isEqualTo(code)); }
        void audit(Reason reason, Outcome outcome, RiskEvaluation risk) {
            assertThat(audits).hasSize(1);
            var audit = audits.getFirst();
            assertThat(audit.reason()).isEqualTo(reason); assertThat(audit.outcome()).isEqualTo(outcome); assertThat(audit.riskEvaluation()).isEqualTo(risk);
            assertThat(commits).hasValue(1); assertThat(rollbacks).hasValue(0);
        }
    }

    @Test
    void personalCallerAndCurrentActiveAccountAreMandatory() {
        var harness = new Harness();
        assertThatThrownBy(() -> harness.service.create(harness.command, null)).isInstanceOf(SubjectClaimException.class);
        assertThatThrownBy(() -> harness.service.create(null, new PersonalClaimCaller(harness.account))).isInstanceOf(SubjectClaimException.class);
        when(harness.repository.readAccount(harness.context, harness.account)).thenReturn(Optional.empty());
        harness.fails(Code.FORBIDDEN); assertThat(harness.rows).isEmpty(); assertThat(harness.audits).isEmpty();
        var inactive = new Harness();
        when(inactive.repository.readAccount(inactive.context, inactive.account)).thenReturn(Optional.of(new AccountRow(inactive.account, "DISABLED")));
        inactive.fails(Code.FORBIDDEN); assertThat(inactive.rows).singleElement().extracting(NewClaim::status).isEqualTo(ClaimStatus.REJECTED);
    }
    @Test
    void zeroInitialBindingStopsBeforeLocksAndWrites() {
        var harness = new Harness(); when(harness.repository.findBindingIdentity(harness.context, harness.binding)).thenReturn(Optional.empty());
        harness.fails(Code.VALIDATION_FAILED); assertThat(harness.locks).isEmpty(); assertThat(harness.rows).isEmpty(); assertThat(harness.audits).isEmpty();
        assertThat(harness.providers.verifications).hasValue(0);
    }
    @Test
    void proofApprovalMustBindEveryTrustedDimension() {
        for (int dimension = 0; dimension < 5; dimension++) {
            var harness = new Harness(); int selected = dimension;
            SubjectClaimProofVerifier broken = request -> new SubjectClaimProviderResult.Allowed<>(new ApprovedProof(
                    selected == 0 ? UUID.randomUUID() : harness.account, selected == 1 ? UUID.randomUUID() : harness.subject,
                    selected == 2 ? UUID.randomUUID() : harness.binding, selected == 3 ? VerificationMethod.MANUAL_REVIEW : VerificationMethod.SMS,
                    selected == 4 ? "" : "reference"));
            harness.service = new DefaultCustomerSubjectClaimService(harness.transaction, harness.context, harness.repository, broken, harness.providers);
            harness.fails(Code.SERVICE_UNAVAILABLE); assertThat(harness.rows).isEmpty();
        }
        for (SubjectClaimProofVerifier broken : List.<SubjectClaimProofVerifier>of(request -> null, request -> { throw new IllegalStateException("CANARY"); })) {
            var harness = new Harness(); harness.service = new DefaultCustomerSubjectClaimService(harness.transaction, harness.context, harness.repository, broken, harness.providers);
            harness.fails(Code.SERVICE_UNAVAILABLE);
        }
        var harness = new Harness(); harness.command = harness.command("request", VerificationMethod.SMS, "unknown"); harness.fails(Code.VALIDATION_FAILED);
    }
    @Test
    void unavailableDefaultProofAndCipherNeverAccept() {
        for (var method : VerificationMethod.values()) {
            var harness = new Harness(); harness.providers.unavailable = true; harness.command = harness.command("request", method, "attempt");
            harness.fails(Code.SERVICE_UNAVAILABLE); assertThat(harness.rows).isEmpty(); assertThat(harness.audits).isEmpty();
        }
    }
    @Test
    void locksUseFrozenNamespacesAndCurrentRereads() {
        var harness = new Harness(); harness.run();
        assertThat(harness.locks.equals(List.of("subject_number.number:" + harness.hmac, "subject_number.subject:" + harness.subject, "customer_iam.account:" + harness.account))).isTrue();
        var order = inOrder(harness.repository);
        order.verify(harness.repository).assertRole(harness.context);
        order.verify(harness.repository).findBindingIdentity(harness.context, harness.binding);
        order.verify(harness.repository).lock(harness.context, harness.locks.get(0));
        order.verify(harness.repository).lock(harness.context, harness.locks.get(1));
        order.verify(harness.repository).lockSubject(harness.context, harness.subject);
        order.verify(harness.repository).lockBinding(harness.context, harness.binding);
        order.verify(harness.repository).lockRisks(harness.context, harness.binding);
        order.verify(harness.repository).lock(harness.context, harness.locks.get(2));
        order.verify(harness.repository).readAccount(harness.context, harness.account);
        assertThat(harness.begins).hasValue(1);
        harness.transaction.execute(status -> { harness.fails(Code.SERVICE_UNAVAILABLE); return null; });
    }
    @Test
    void currentPredicateFailuresPersistOnlyResolvableFreshRejections() {
        for (int condition = 0; condition < 3; condition++) {
            var harness = new Harness();
            if (condition == 0) {
                UUID actualSubject = UUID.randomUUID();
                when(harness.repository.findBindingIdentity(harness.context, harness.binding)).thenReturn(Optional.of(new BindingIdentity(harness.binding, actualSubject, harness.hmac)));
                when(harness.repository.lockBinding(harness.context, harness.binding)).thenReturn(Optional.of(new BindingRow(harness.binding, actualSubject, harness.hmac, "ACTIVE", harness.time)));
            }
            if (condition == 1) when(harness.repository.lockBinding(harness.context, harness.binding)).thenReturn(Optional.of(new BindingRow(harness.binding, harness.subject, harness.hmac, "DISABLED", null)));
            if (condition == 2) when(harness.repository.readAccount(harness.context, harness.account)).thenReturn(Optional.of(new AccountRow(harness.account, "DISABLED")));
            harness.fails(condition == 2 ? Code.FORBIDDEN : Code.VALIDATION_FAILED);
            harness.audit(condition == 2 ? Reason.CLAIM_ACCOUNT_INACTIVE : condition == 0 ? Reason.CLAIM_SUBJECT_BINDING_MISMATCH : Reason.CLAIM_BINDING_NOT_ACTIVE, Outcome.DENIED, RiskEvaluation.NOT_REQUIRED);
            assertThat(harness.rows).hasSize(1);
        }
        var harness = new Harness(); when(harness.repository.lockSubject(harness.context, harness.subject)).thenReturn(Optional.empty());
        harness.fails(Code.VALIDATION_FAILED); assertThat(harness.audits).isEmpty();
    }
    @Test
    void openRiskPreemptsEveryReplayAndDuplicateOutcome() {
        for (var status : ClaimStatus.values()) for (boolean existing : List.of(false, true)) {
            var harness = new Harness(); var stored = harness.stored(status);
            if (existing) when(harness.repository.findByRequestId(harness.context, harness.command.requestId())).thenReturn(Optional.of(stored));
            harness.providers.force(stored.verificationRef(), new SubjectClaimProviderResult.Unavailable<>());
            when(harness.repository.lockRisks(harness.context, harness.binding)).thenReturn(List.of(new RiskRow(UUID.randomUUID(), "OPEN")));
            harness.fails(Code.CLAIM_RISK_BLOCKED); assertThat(harness.providers.opens(stored.verificationRef())).isZero();
            assertThat(harness.rows).hasSize(existing ? 0 : 1); harness.audit(Reason.CLAIM_RISK_BLOCKED, Outcome.DENIED, RiskEvaluation.OPEN);
        }
    }
    @Test
    void riskTimesAndAcceptedTimesComeFromDatabaseDecisionClock() {
        var harness = new Harness(); var result = harness.run(); var row = harness.rows.getFirst();
        assertThat(row.riskCheckedAt()).isEqualTo(harness.time); assertThat(row.verifiedAt()).isEqualTo(harness.time.plusMillis(1));
        assertThat(result.verifiedAt()).isEqualTo(row.verifiedAt()); assertThat(row.verifiedAt()).isAfterOrEqualTo(row.riskCheckedAt());
    }
    @Test
    void exactRequestReplayUsesLogicalIdentityAndFirstOutcome() {
        for (var status : ClaimStatus.values()) {
            var harness = new Harness(); var row = harness.stored(status);
            when(harness.repository.findByRequestId(harness.context, harness.command.requestId())).thenReturn(Optional.of(row));
            if (status == ClaimStatus.ACCEPTED) harness.accepted(row);
            var result = harness.run(); assertThat(result.claimId()).isEqualTo(row.id()); assertThat(result.status()).isEqualTo(status);
            assertThat(result.verifiedAt()).isEqualTo(row.verifiedAt()); assertThat(harness.rows).isEmpty();
            harness.audit(Reason.CLAIM_REPLAYED, Outcome.REPLAYED, RiskEvaluation.NO_OPEN);
        }
    }
    @Test
    void changedRequestIdentityNeverDisclosesStoredOutcome() {
        for (int dimension = 0; dimension < 4; dimension++) for (boolean unavailable : List.of(false, true)) {
            var harness = new Harness(); var original = harness.stored(ClaimStatus.ACCEPTED);
            var row = new ClaimRow(original.id(), dimension == 0 ? UUID.randomUUID() : harness.subject,
                    dimension == 1 ? UUID.randomUUID() : harness.account, dimension == 2 ? UUID.randomUUID() : null,
                    dimension == 3 ? VerificationMethod.MANUAL_REVIEW : VerificationMethod.SMS, original.verificationRef(), "request", ClaimStatus.ACCEPTED, harness.time, harness.time);
            harness.providers.force(row.verificationRef(), unavailable ? new SubjectClaimProviderResult.Unavailable<>() : new SubjectClaimProviderResult.Invalid<>());
            when(harness.repository.findByRequestId(harness.context, "request")).thenReturn(Optional.of(row));
            harness.fails(Code.CLAIM_REPLAY_CONFLICT); assertThat(harness.providers.opens(row.verificationRef())).isZero(); assertThat(harness.rows).isEmpty();
            harness.audit(Reason.CLAIM_REPLAY_CONFLICT, Outcome.CONFLICT, RiskEvaluation.NO_OPEN);
        }
    }
    @Test
    void exactAcceptedDuplicateReturnsFirstResultWithoutInsert() {
        var harness = new Harness(); var row = harness.stored(ClaimStatus.ACCEPTED); harness.accepted(row);
        var result = harness.run(); assertThat(result.claimId()).isEqualTo(row.id()); assertThat(harness.rows).isEmpty();
        verify(harness.repository, never()).setUnownedOwner(any(), any(), any(), anyInt(), any());
        harness.audit(Reason.CLAIM_DUPLICATE, Outcome.DUPLICATE, RiskEvaluation.NO_OPEN);
    }
    @Test
    void changedAcceptedContextAppendsExactlyOneConflict() {
        for (String dimension : List.of("proof", "method", "binding", "hmac")) {
            var harness = new Harness(); var original = harness.stored(ClaimStatus.ACCEPTED);
            harness.providers.probe = () -> { };
            String envelope = harness.providers.sealPayload(new Payload(1, harness.subject,
                    dimension.equals("binding") ? UUID.randomUUID() : harness.binding,
                    dimension.equals("hmac") ? "b".repeat(64) : harness.hmac, VerificationMethod.SMS, "server-proof"));
            harness.providers.probe = () -> assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            var row = new ClaimRow(original.id(), original.subjectId(), original.accountId(), null, VerificationMethod.SMS,
                    envelope, original.requestId(), original.status(), original.riskCheckedAt(), original.verifiedAt());
            harness.accepted(row);
            VerificationMethod method = dimension.equals("method") ? VerificationMethod.MANUAL_REVIEW : VerificationMethod.SMS;
            harness.approve("changed", method, dimension.equals("proof") ? "changed-proof" : "server-proof");
            harness.command = harness.command("fresh", method, "changed");
            if (dimension.equals("method")) harness.providers.force(envelope, new SubjectClaimProviderResult.Unavailable<>());
            var cipher = spy(harness.providers);
            harness.service = new DefaultCustomerSubjectClaimService(harness.transaction, harness.context, harness.repository, harness.providers, cipher);
            var owner = harness.repository.lockSubject(harness.context, harness.subject).orElseThrow();
            harness.fails(Code.CLAIM_CONTEXT_CONFLICT); assertThat(harness.rows).singleElement().extracting(NewClaim::status).isEqualTo(ClaimStatus.CONFLICT);
            var conflict = harness.rows.getFirst();
            assertThat(conflict.requestId()).isEqualTo("fresh"); assertThat(conflict.id()).isNotEqualTo(row.id()); assertThat(conflict.verifiedAt()).isNull();
            verify(harness.repository, times(1)).insertClaim(eq(harness.context), any());
            verify(harness.repository, never()).setUnownedOwner(any(), any(), any(), anyInt(), any());
            assertThat(harness.repository.lockSubject(harness.context, harness.subject).orElseThrow()).isEqualTo(owner);
            var order = inOrder(harness.repository, cipher);
            order.verify(harness.repository).findAcceptedForAccount(harness.context, harness.subject, harness.account);
            if (!dimension.equals("method")) order.verify(cipher).open(argThat(reference -> envelope.equals(reference.envelope())));
            assertThat(harness.providers.opens(envelope)).isEqualTo(dimension.equals("method") ? 0 : 1);
            harness.audit(Reason.CLAIM_CONTEXT_CONFLICT, Outcome.CONFLICT, RiskEvaluation.NO_OPEN);
        }
    }
    @Test
    void freshAcceptanceSetsOnlyNullOwnerWithExpectedRevision() {
        var harness = new Harness(); harness.run();
        verify(harness.repository).setUnownedOwner(harness.context, harness.subject, harness.account, 7, harness.rows.getFirst().verifiedAt());
        harness.audit(Reason.CLAIM_ACCEPTED, Outcome.ACCEPTED, RiskEvaluation.NO_OPEN);
        harness = new Harness(); harness.owner(harness.account, null); harness.run();
        verify(harness.repository, never()).setUnownedOwner(any(), any(), any(), anyInt(), any()); assertThat(harness.rows).hasSize(1);
    }
    @Test
    void differentOwnerRefusesWithoutTransferOrRepair() {
        for (boolean organization : List.of(false, true)) {
            var harness = new Harness(); harness.owner(organization ? null : UUID.randomUUID(), organization ? UUID.randomUUID() : null);
            harness.fails(Code.FORBIDDEN); assertThat(harness.rows).singleElement().extracting(NewClaim::status).isEqualTo(ClaimStatus.CONFLICT);
            verify(harness.repository, never()).setUnownedOwner(any(), any(), any(), anyInt(), any());
            harness.audit(Reason.CLAIM_OWNER_CONFLICT, Outcome.CONFLICT, RiskEvaluation.NO_OPEN);
        }
    }
    @Test
    void corruptAcceptedOwnerRelationAndUpdateMissFailClosed() {
        for (String corruption : List.of("personal", "unowned", "later", "organization-missing", "organization-different")) {
            UUID earlier = UUID.fromString("00000000-0000-0000-0000-000000000001"), later = UUID.fromString("00000000-0000-0000-0000-000000000002");
            var scenario = new Harness(); var original = scenario.stored(ClaimStatus.ACCEPTED);
            UUID relationOrganization = corruption.equals("organization-different") ? UUID.randomUUID() : null;
            var stored = new ClaimRow(earlier, original.subjectId(), original.accountId(), relationOrganization, original.verificationMethod(),
                    original.verificationRef(), original.requestId(), original.status(), original.riskCheckedAt(), original.verifiedAt());
            scenario.accepted(stored);
            UUID organization = UUID.randomUUID();
            if (corruption.equals("personal")) scenario.owner(UUID.randomUUID(), null);
            else if (corruption.equals("unowned")) scenario.owner(null, null);
            else if (corruption.startsWith("organization")) scenario.owner(null, organization);
            List<AcceptedOwnerRow> relations = corruption.equals("later")
                    ? List.of(new AcceptedOwnerRow(earlier, scenario.account, null), new AcceptedOwnerRow(later, UUID.randomUUID(), null))
                    : List.of(new AcceptedOwnerRow(earlier, scenario.account, relationOrganization));
            when(scenario.repository.readAcceptedOwners(scenario.context, scenario.subject)).thenReturn(relations);
            var owner = scenario.repository.lockSubject(scenario.context, scenario.subject).orElseThrow();
            scenario.fails(Code.INTERNAL); assertThat(scenario.rows).isEmpty();
            verify(scenario.repository, never()).insertClaim(any(), any());
            verify(scenario.repository, never()).setUnownedOwner(any(), any(), any(), anyInt(), any());
            assertThat(scenario.repository.lockSubject(scenario.context, scenario.subject).orElseThrow()).isEqualTo(owner);
            assertThat(scenario.providers.opens(stored.verificationRef())).isZero();
            scenario.audit(Reason.CLAIM_DATA_INTEGRITY, Outcome.DENIED, RiskEvaluation.NO_OPEN);
        }
        var harness = new Harness(); when(harness.repository.setUnownedOwner(any(), any(), any(), anyInt(), any())).thenReturn(0);
        harness.fails(Code.INTERNAL); assertThat(harness.rollbacks).hasValue(1); assertThat(harness.rows).isEmpty(); assertThat(harness.audits).isEmpty();
    }
    @Test
    void uniqueLoserRetriesOnlyAfterWholeTransactionCleanup() {
        for (String state : List.of("40001", "40P01")) {
            var harness = new Harness(); AtomicInteger attempts = new AtomicInteger();
            doAnswer(call -> {
                assertThat(harness.rollbacks.get()).isEqualTo(attempts.get()); attempts.incrementAndGet();
                throw new RuntimeException(new SQLException("CANARY", state));
            }).when(harness.repository).assertRole(harness.context);
            harness.fails(Code.SERVICE_UNAVAILABLE); assertThat(attempts).hasValue(3); assertThat(harness.rollbacks).hasValue(3); assertThat(harness.commits).hasValue(0);
        }
        for (String constraint : List.of("uq_claim_request", "uq_claim_accepted_subject_account", "other_constraint")) {
            var harness = new Harness();
            var server = new org.postgresql.util.ServerErrorMessage("SERROR\0C23505\0n" + constraint + "\0MCANARY\0\0");
            doThrow(new RuntimeException(new org.postgresql.util.PSQLException(server))).when(harness.repository).assertRole(harness.context);
            harness.fails(constraint.equals("other_constraint") ? Code.INTERNAL : Code.SERVICE_UNAVAILABLE);
            assertThat(harness.begins).hasValue(constraint.equals("other_constraint") ? 1 : 3);
        }
    }
    @Test
    void exactAuditMappingsAndFailuresNeverLeakOrPartiallyCommit() {
        for (var reason : Reason.values()) {
            var scenario = new Harness(); Code code = null;
            Outcome expected = switch (reason) {
                case CLAIM_ACCEPTED -> Outcome.ACCEPTED;
                case CLAIM_REPLAYED -> Outcome.REPLAYED;
                case CLAIM_DUPLICATE -> Outcome.DUPLICATE;
                case CLAIM_REPLAY_CONFLICT, CLAIM_CONTEXT_CONFLICT, CLAIM_OWNER_CONFLICT -> Outcome.CONFLICT;
                default -> Outcome.DENIED;
            };
            switch (reason) {
                case CLAIM_ACCEPTED -> { }
                case CLAIM_REPLAYED -> {
                    var row = scenario.stored(ClaimStatus.REJECTED);
                    when(scenario.repository.findByRequestId(scenario.context, "request")).thenReturn(Optional.of(row));
                }
                case CLAIM_DUPLICATE -> scenario.accepted(scenario.stored(ClaimStatus.ACCEPTED));
                case CLAIM_RISK_BLOCKED -> {
                    when(scenario.repository.lockRisks(scenario.context, scenario.binding)).thenReturn(List.of(new RiskRow(UUID.randomUUID(), "OPEN")));
                    code = Code.CLAIM_RISK_BLOCKED;
                }
                case CLAIM_REPLAY_CONFLICT -> {
                    var row = scenario.stored(ClaimStatus.REJECTED);
                    when(scenario.repository.findByRequestId(scenario.context, "request")).thenReturn(Optional.of(new ClaimRow(row.id(), row.subjectId(), UUID.randomUUID(), null,
                            row.verificationMethod(), row.verificationRef(), row.requestId(), row.status(), row.riskCheckedAt(), null)));
                    code = Code.CLAIM_REPLAY_CONFLICT;
                }
                case CLAIM_CONTEXT_CONFLICT -> {
                    scenario.accepted(scenario.stored(ClaimStatus.ACCEPTED)); scenario.approve("changed", VerificationMethod.SMS, "changed-reference");
                    scenario.command = scenario.command("request", VerificationMethod.SMS, "changed"); code = Code.CLAIM_CONTEXT_CONFLICT;
                }
                case CLAIM_OWNER_CONFLICT -> { scenario.owner(UUID.randomUUID(), null); code = Code.FORBIDDEN; }
                case CLAIM_SUBJECT_BINDING_MISMATCH -> {
                    UUID actual = UUID.randomUUID();
                    when(scenario.repository.findBindingIdentity(scenario.context, scenario.binding)).thenReturn(Optional.of(new BindingIdentity(scenario.binding, actual, scenario.hmac)));
                    when(scenario.repository.lockBinding(scenario.context, scenario.binding)).thenReturn(Optional.of(new BindingRow(scenario.binding, actual, scenario.hmac, "ACTIVE", scenario.time)));
                    code = Code.VALIDATION_FAILED;
                }
                case CLAIM_BINDING_NOT_ACTIVE -> {
                    when(scenario.repository.lockBinding(scenario.context, scenario.binding)).thenReturn(Optional.of(new BindingRow(scenario.binding, scenario.subject, scenario.hmac, "DISABLED", null)));
                    code = Code.VALIDATION_FAILED;
                }
                case CLAIM_ACCOUNT_INACTIVE -> { when(scenario.repository.readAccount(scenario.context, scenario.account)).thenReturn(Optional.of(new AccountRow(scenario.account, "DISABLED"))); code = Code.FORBIDDEN; }
                case CLAIM_REPLAY_UNRESOLVABLE -> {
                    var row = scenario.stored(ClaimStatus.REJECTED); when(scenario.repository.findByRequestId(scenario.context, "request")).thenReturn(Optional.of(row));
                    scenario.providers.force(row.verificationRef(), new SubjectClaimProviderResult.Invalid<>()); code = Code.VALIDATION_FAILED;
                }
                case CLAIM_DATA_INTEGRITY -> {
                    when(scenario.repository.readAcceptedOwners(scenario.context, scenario.subject)).thenReturn(List.of(new AcceptedOwnerRow(UUID.randomUUID(), scenario.account, null)));
                    code = Code.INTERNAL;
                }
            }
            if (code == null) scenario.run(); else scenario.fails(code);
            RiskEvaluation risk = switch (reason) {
                case CLAIM_RISK_BLOCKED -> RiskEvaluation.OPEN;
                case CLAIM_SUBJECT_BINDING_MISMATCH, CLAIM_BINDING_NOT_ACTIVE, CLAIM_ACCOUNT_INACTIVE -> RiskEvaluation.NOT_REQUIRED;
                default -> RiskEvaluation.NO_OPEN;
            };
            scenario.audit(reason, expected, risk);
            assertThat(scenario.audits.getFirst().toString().contains("server-proof")).isFalse();
        }
        var harness = new Harness(); doThrow(new IllegalStateException("CANARY")).when(harness.repository).appendAudit(any(), any());
        harness.fails(Code.SERVICE_UNAVAILABLE); assertThat(harness.rows).isEmpty(); assertThat(harness.audits).isEmpty(); assertThat(harness.rollbacks).hasValue(1);
        try { harness.run(); } catch (SubjectClaimException failure) {
            assertThat(failure.toString().contains("CANARY")).isFalse(); assertThat(failure.getCause()).isNull();
        }
        assertThat(harness.command.toString().contains("attempt")).isFalse();
        assertThat(output.getAll().contains("CANARY")).isFalse();
    }
}
