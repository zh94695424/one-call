package com.onecall.core.subject_number.internal.claim;

import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException.Code;
import com.onecall.core.subject_number.internal.claim.SubjectClaimProofVerifier.ApprovedProof;
import com.onecall.core.subject_number.internal.claim.SubjectClaimProofVerifier.ProofRequest;
import com.onecall.core.subject_number.internal.claim.SubjectClaimReferenceCipher.SealedReference;
import com.onecall.core.subject_number.internal.claim.SubjectClaimRepository.*;
import com.onecall.core.subject_number.internal.claim.SubjectClaimVerificationReference.Payload;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class DefaultCustomerSubjectClaimService implements CustomerSubjectClaimService {
    private final TransactionTemplate transaction;
    private final DSLContext context;
    private final SubjectClaimRepository repository;
    private final SubjectClaimProofVerifier proofs;
    private final SubjectClaimReferenceCipher cipher;
    private final SubjectClaimVerificationReference references = new SubjectClaimVerificationReference();

    public DefaultCustomerSubjectClaimService(TransactionTemplate transaction, DSLContext context,
            SubjectClaimRepository repository, SubjectClaimProofVerifier proofs, SubjectClaimReferenceCipher cipher) {
        this.transaction = Objects.requireNonNull(transaction);
        this.context = Objects.requireNonNull(context);
        this.repository = Objects.requireNonNull(repository);
        this.proofs = Objects.requireNonNull(proofs);
        this.cipher = Objects.requireNonNull(cipher);
    }

    @Override public ClaimResult create(ClaimCommand command, PersonalClaimCaller caller) {
        if (caller == null) throw error(Code.UNAUTHENTICATED);
        if (command == null) throw error(Code.VALIDATION_FAILED);
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw error(Code.SERVICE_UNAVAILABLE);
        for (int attempt = 0; attempt < 3; attempt++) {
            ClaimDecision decision;
            try {
                decision = transaction.execute(status -> decide(command, caller));
            } catch (SubjectClaimException known) { throw known; }
            catch (RuntimeException failure) {
                SqlFailure sql = classify(failure);
                if (sql == SqlFailure.RETRY) continue;
                throw error(sql == SqlFailure.INTEGRITY ? Code.INTERNAL : Code.SERVICE_UNAVAILABLE);
            }
            if (decision == null) throw error(Code.SERVICE_UNAVAILABLE);
            if (decision.error() != null) throw error(decision.error());
            return decision.result();
        }
        throw error(Code.SERVICE_UNAVAILABLE);
    }

    private ClaimDecision decide(ClaimCommand command, PersonalClaimCaller caller) {
        repository.assertRole(context);
        BindingIdentity identity = repository.findBindingIdentity(context, command.bindingId())
                .orElseThrow(() -> error(Code.VALIDATION_FAILED));
        ApprovedProof approval = allowed(() -> proofs.verify(new ProofRequest(caller.accountId(), command.subjectId(),
                command.bindingId(), command.verificationMethod(), command.verificationProofId())));
        if (!caller.accountId().equals(approval.accountId()) || !command.subjectId().equals(approval.subjectId())
                || !command.bindingId().equals(approval.bindingId()) || command.verificationMethod() != approval.method()
                || !SubjectClaimVerificationReference.opaque(approval.proofRef())) throw error(Code.SERVICE_UNAVAILABLE);
        Payload input;
        try { input = new Payload(1, command.subjectId(), command.bindingId(), identity.numberHmac(), command.verificationMethod(), approval.proofRef()); }
        catch (RuntimeException invalid) { throw error(Code.VALIDATION_FAILED); }
        SealedReference sealed = allowed(() -> cipher.seal(references.encode(input)));
        if (!SubjectClaimVerificationReference.envelope(sealed.envelope())) throw error(Code.SERVICE_UNAVAILABLE);
        Payload roundTrip = decode(allowed(() -> cipher.open(sealed)));
        if (!input.equals(roundTrip)) throw error(Code.SERVICE_UNAVAILABLE);

        repository.lock(context, "subject_number.number:" + identity.numberHmac());
        repository.lock(context, "subject_number.subject:" + command.subjectId());
        SubjectRow subject = repository.lockSubject(context, command.subjectId()).orElseThrow(() -> error(Code.VALIDATION_FAILED));
        BindingRow binding = repository.lockBinding(context, command.bindingId()).orElseThrow(() -> error(Code.VALIDATION_FAILED));
        var risks = repository.lockRisks(context, command.bindingId());
        repository.lock(context, "customer_iam.account:" + caller.accountId());
        AccountRow account = repository.readAccount(context, caller.accountId()).orElseThrow(() -> error(Code.FORBIDDEN));
        if (!identity.id().equals(binding.id()) || !identity.subjectId().equals(binding.subjectId())
                || !identity.numberHmac().equals(binding.numberHmac())) throw error(Code.INTERNAL);
        if (!command.subjectId().equals(binding.subjectId()))
            return deny(command, caller, sealed, Code.VALIDATION_FAILED, Reason.CLAIM_SUBJECT_BINDING_MISMATCH, ClaimStatus.REJECTED, RiskEvaluation.NOT_REQUIRED);
        if (!"ACTIVE".equals(binding.status()) || binding.verifiedAt() == null)
            return deny(command, caller, sealed, Code.VALIDATION_FAILED, Reason.CLAIM_BINDING_NOT_ACTIVE, ClaimStatus.REJECTED, RiskEvaluation.NOT_REQUIRED);
        if (!"ACTIVE".equals(account.status()))
            return deny(command, caller, sealed, Code.FORBIDDEN, Reason.CLAIM_ACCOUNT_INACTIVE, ClaimStatus.REJECTED, RiskEvaluation.NOT_REQUIRED);
        Instant riskTime = repository.databaseTime(context);
        if (risks.stream().anyMatch(risk -> "OPEN".equals(risk.state())))
            return deny(command, caller, sealed, Code.CLAIM_RISK_BLOCKED, Reason.CLAIM_RISK_BLOCKED, ClaimStatus.REJECTED, RiskEvaluation.OPEN);

        var request = repository.findByRequestId(context, command.requestId());
        if (request.isPresent()) {
            ClaimRow row = request.get();
            if (!rowIdentity(row, command, caller))
                return audited(command, caller, null, Code.CLAIM_REPLAY_CONFLICT, Reason.CLAIM_REPLAY_CONFLICT, Outcome.CONFLICT, RiskEvaluation.NO_OPEN);
            Payload stored;
            try { stored = stored(row); }
            catch (SubjectClaimException failure) {
                if (failure.code() != Code.VALIDATION_FAILED) throw failure;
                return audited(command, caller, null, Code.VALIDATION_FAILED, Reason.CLAIM_REPLAY_UNRESOLVABLE, Outcome.DENIED, RiskEvaluation.NO_OPEN);
            }
            if (!input.equals(stored))
                return audited(command, caller, null, Code.CLAIM_REPLAY_CONFLICT, Reason.CLAIM_REPLAY_CONFLICT, Outcome.CONFLICT, RiskEvaluation.NO_OPEN);
            repository.findByRequestId(context, command.requestId());
            repository.findAcceptedForAccount(context, command.subjectId(), caller.accountId());
            if (!integrity(subject)) return corrupt(command, caller);
            return audited(command, caller, result(row), null, Reason.CLAIM_REPLAYED, Outcome.REPLAYED, RiskEvaluation.NO_OPEN);
        }
        var accepted = repository.findAcceptedForAccount(context, command.subjectId(), caller.accountId());
        if (!integrity(subject)) return corrupt(command, caller);
        if (accepted.isPresent()) {
            ClaimRow row = accepted.get();
            boolean same = rowIdentity(row, command, caller);
            if (same) {
                try { same = input.equals(stored(row)); }
                catch (SubjectClaimException failure) {
                    if (failure.code() != Code.VALIDATION_FAILED) throw failure;
                    return audited(command, caller, null, Code.VALIDATION_FAILED, Reason.CLAIM_REPLAY_UNRESOLVABLE, Outcome.DENIED, RiskEvaluation.NO_OPEN);
                }
            }
            repository.findByRequestId(context, command.requestId());
            repository.findAcceptedForAccount(context, command.subjectId(), caller.accountId());
            if (same) return audited(command, caller, result(row), null, Reason.CLAIM_DUPLICATE, Outcome.DUPLICATE, RiskEvaluation.NO_OPEN);
            return deny(command, caller, sealed, Code.CLAIM_CONTEXT_CONFLICT, Reason.CLAIM_CONTEXT_CONFLICT, ClaimStatus.CONFLICT, RiskEvaluation.NO_OPEN);
        }
        if (subject.ownerOrganizationId() != null || subject.ownerAccountId() != null && !caller.accountId().equals(subject.ownerAccountId()))
            return deny(command, caller, sealed, Code.FORBIDDEN, Reason.CLAIM_OWNER_CONFLICT, ClaimStatus.CONFLICT, RiskEvaluation.NO_OPEN);
        Instant verifiedAt = repository.databaseTime(context);
        NewClaim fresh = new NewClaim(UUID.randomUUID(), command.subjectId(), caller.accountId(), null, command.verificationMethod(),
                sealed.envelope(), command.requestId(), ClaimStatus.ACCEPTED, riskTime, verifiedAt);
        repository.insertClaim(context, fresh);
        if (subject.ownerAccountId() == null && repository.setUnownedOwner(context, command.subjectId(), caller.accountId(), subject.revision(), verifiedAt) != 1) {
            repository.lockSubject(context, command.subjectId());
            throw error(Code.INTERNAL);
        }
        return audited(command, caller, new ClaimResult(fresh.id(), ClaimStatus.ACCEPTED, command.subjectId(), verifiedAt), null,
                Reason.CLAIM_ACCEPTED, Outcome.ACCEPTED, RiskEvaluation.NO_OPEN);
    }

    private boolean integrity(SubjectRow subject) {
        if (subject.ownerAccountId() != null && subject.ownerOrganizationId() != null) return false;
        for (AcceptedOwnerRow row : repository.readAcceptedOwners(context, subject.id())) {
            if (subject.ownerAccountId() != null) {
                if (!subject.ownerAccountId().equals(row.accountId()) || row.organizationId() != null) return false;
            } else if (subject.ownerOrganizationId() == null || !subject.ownerOrganizationId().equals(row.organizationId())) return false;
        }
        return true;
    }
    private ClaimDecision corrupt(ClaimCommand command, PersonalClaimCaller caller) {
        return audited(command, caller, null, Code.INTERNAL, Reason.CLAIM_DATA_INTEGRITY, Outcome.DENIED, RiskEvaluation.NO_OPEN);
    }
    private ClaimDecision deny(ClaimCommand command, PersonalClaimCaller caller, SealedReference sealed, Code code,
            Reason reason, ClaimStatus status, RiskEvaluation risk) {
        Instant time = repository.databaseTime(context);
        if (repository.findByRequestId(context, command.requestId()).isEmpty())
            repository.insertClaim(context, new NewClaim(UUID.randomUUID(), command.subjectId(), caller.accountId(), null,
                    command.verificationMethod(), sealed.envelope(), command.requestId(), status, time, null));
        return audited(command, caller, null, code, reason, status == ClaimStatus.CONFLICT ? Outcome.CONFLICT : Outcome.DENIED, risk);
    }
    private ClaimDecision audited(ClaimCommand command, PersonalClaimCaller caller, ClaimResult result, Code code,
            Reason reason, Outcome outcome, RiskEvaluation risk) {
        try {
            repository.appendAudit(context, new AuditDecision(repository.databaseTime(context), caller.accountId(), command.subjectId(),
                    command.requestId(), command.traceId(), reason, outcome, risk));
        } catch (RuntimeException failure) { throw error(Code.SERVICE_UNAVAILABLE); }
        return new ClaimDecision(result, code);
    }
    private boolean rowIdentity(ClaimRow row, ClaimCommand command, PersonalClaimCaller caller) {
        return command.subjectId().equals(row.subjectId()) && caller.accountId().equals(row.accountId())
                && row.organizationId() == null && command.verificationMethod() == row.verificationMethod();
    }
    private Payload stored(ClaimRow row) {
        if (!SubjectClaimVerificationReference.envelope(row.verificationRef())) throw error(Code.VALIDATION_FAILED);
        Payload payload = decode(allowed(() -> cipher.open(new SealedReference(row.verificationRef()))));
        if (!row.subjectId().equals(payload.subjectId()) || row.verificationMethod() != payload.verificationMethod()) throw error(Code.VALIDATION_FAILED);
        return payload;
    }
    private Payload decode(byte[] bytes) {
        try { return references.decode(bytes); }
        catch (RuntimeException invalid) { throw error(Code.VALIDATION_FAILED); }
    }
    private static <T> T allowed(Supplier<SubjectClaimProviderResult<T>> operation) {
        SubjectClaimProviderResult<T> result;
        try { result = operation.get(); }
        catch (RuntimeException failure) { throw error(Code.SERVICE_UNAVAILABLE); }
        if (result instanceof SubjectClaimProviderResult.Invalid<T>) throw error(Code.VALIDATION_FAILED);
        if (!(result instanceof SubjectClaimProviderResult.Allowed<T> approved) || approved.value() == null) throw error(Code.SERVICE_UNAVAILABLE);
        return approved.value();
    }
    private static ClaimResult result(ClaimRow row) { return new ClaimResult(row.id(), row.status(), row.subjectId(), row.verifiedAt()); }
    private static SubjectClaimException error(Code code) { return new SubjectClaimException(code); }
    private enum SqlFailure { RETRY, INTEGRITY, UNAVAILABLE }
    private static SqlFailure classify(Throwable failure) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int depth = 0; failure != null && depth < 32 && seen.add(failure); depth++, failure = failure.getCause()) {
            if (failure instanceof SQLException sql) {
                String state = sql.getSQLState();
                if ("40001".equals(state) || "40P01".equals(state)) return SqlFailure.RETRY;
                if ("23505".equals(state) && retryConstraint(sql)) return SqlFailure.RETRY;
                if (state != null && (state.startsWith("23") || "P0001".equals(state))) return SqlFailure.INTEGRITY;
            }
        }
        return SqlFailure.UNAVAILABLE;
    }
    private static boolean retryConstraint(SQLException failure) {
        try {
            if (!"org.postgresql.util.PSQLException".equals(failure.getClass().getName())) return false;
            Object server = failure.getClass().getMethod("getServerErrorMessage").invoke(failure);
            if (server == null) return false;
            Object constraint = server.getClass().getMethod("getConstraint").invoke(server);
            return "uq_claim_request".equals(constraint) || "uq_claim_accepted_subject_account".equals(constraint);
        } catch (ReflectiveOperationException unavailable) { return false; }
    }
    private record ClaimDecision(ClaimResult result, Code error) {
        @Override public String toString() { return "ClaimDecision[REDACTED]"; }
    }
}
