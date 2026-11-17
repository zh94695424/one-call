package com.onecall.core.operations_iam.internal.ivrread;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService;
import com.onecall.core.operations_iam.subjectread.OperationsIvrSessionReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

public final class DefaultOperationsIvrSessionReadService implements OperationsIvrSessionReadService {
    private static final Set<String> SESSION_STATUSES = Set.of("RECEIVED", "CONFIRMED", "ABANDONED", "REJECTED");
    private static final Set<String> ACTIONS = Set.of("GUIDE_ACTIVATION", "DIAL_TASK_CALLEE", "ENTER_CALLEE", "GUIDE_PURCHASE",
            "REJECT", "REJECT_SUSPENDED", "REJECT_CALL_IN_PROGRESS");
    private static final Set<String> REASONS = Set.of("ACTIVATION_REQUIRED", "CONSENT_REQUIRED", "CALLER_UNTRUSTED",
            "SESSION_IDENTITY_MISMATCH", "CALL_IN_PROGRESS", "CONFIRMATION_NOT_ALLOWED", "ACTIVATION_SUSPENDED",
            "CALLER_BINDING_REVOKED", "WORKSPACE_ACCESS_REVOKED", "TASK_ACCESS_REVOKED", "ORGANIZATION_INACTIVE",
            "ACCOUNT_DISABLED", "CALL_STATE_UNRESOLVED", "TASK_CONFLICT", "TASK_CLAIMED", "NO_AVAILABLE_ENTITLEMENT",
            "NO_VALID_TASK", "TASK_EXPIRED", "TASK_UNAVAILABLE", "DIRECT_AUTHORIZED", "DATA_INTEGRITY");
    private final TransactionTemplate transaction;
    private final DSLContext dsl;
    private final OperationsReadContext context;
    private final ObjectProvider<OperationsSubjectReadProperties> properties;
    private final ObjectProvider<IvrSessionReader> readers;
    private final ObjectProvider<IvrEvidenceReader> evidence;
    private final LongSupplier nanoTime;

    public DefaultOperationsIvrSessionReadService(TransactionTemplate transaction, DSLContext dsl, OperationsReadContext context,
            ObjectProvider<OperationsSubjectReadProperties> properties, ObjectProvider<IvrSessionReader> readers,
            ObjectProvider<IvrEvidenceReader> evidence) {
        this(transaction, dsl, context, properties, readers, evidence, System::nanoTime);
    }
    DefaultOperationsIvrSessionReadService(TransactionTemplate transaction, DSLContext dsl, OperationsReadContext context,
            ObjectProvider<OperationsSubjectReadProperties> properties, ObjectProvider<IvrSessionReader> readers,
            ObjectProvider<IvrEvidenceReader> evidence, LongSupplier nanoTime) {
        this.transaction = transaction; this.dsl = dsl; this.context = context; this.properties = properties;
        this.readers = readers; this.evidence = evidence; this.nanoTime = nanoTime;
    }
    @Override public IvrSessionPage list(Caller caller, RawListRequest raw) {
        return execute(caller, deadline -> {
            var request = OperationsIvrSessionReadRequest.parse(raw);
            checkBudget(deadline);
            var codec = new OperationsIvrSessionCursorCodec(one(properties));
            var anchor = request.cursor() == null ? Optional.<Anchor>empty() : Optional.of(codec.decode(request.cursor(), request.digest()));
            var reader = one(readers);
            checkBudget(deadline);
            var facts = reader.fetch(request.filters(), anchor, request.limit() + 1);
            checkBudget(deadline);
            require(facts != null && facts.size() <= request.limit() + 1);
            var items = new ArrayList<IvrSessionSummary>();
            var seen = new HashSet<UUID>();
            Anchor previous = anchor.orElse(null);
            for (var fact : facts) {
                validateSession(fact);
                require(seen.add(fact.sessionId()));
                Anchor current = new Anchor(fact.createdAt(), fact.sessionId());
                require(previous == null || compare(current, previous) < 0);
                previous = current;
                if (items.size() < request.limit()) items.add(fact);
            }
            boolean more = facts.size() > request.limit();
            String cursor = more ? codec.encode(new Anchor(items.getLast().createdAt(), items.getLast().sessionId()), request.digest()) : null;
            return new IvrSessionPage(items, cursor, more);
        });
    }
    @Override public IvrSessionDetail detail(Caller caller, RawDetailRequest raw) {
        return execute(caller, deadline -> {
            UUID sessionId = OperationsIvrSessionReadRequest.parseDetail(raw);
            var reader = one(readers);
            checkBudget(deadline);
            var result = reader.detail(sessionId);
            checkBudget(deadline);
            require(result != null);
            var facts = result.orElseThrow(() -> new Failure(Code.NOT_FOUND));
            var session = facts.session();
            validateSession(session);
            require(sessionId.equals(session.sessionId()) && facts.calls().size() == 1);
            var call = facts.calls().getFirst();
            require(call != null && sessionId.equals(call.sessionId()) && session.callId().equals(call.callId()));
            validateCall(call.call());
            var decisions = new ArrayList<IvrDecisionSummary>();
            var decisionIds = new HashSet<UUID>();
            IvrDecisionSummary previousDecision = null;
            for (var fact : facts.decisions()) {
                require(fact != null && sessionId.equals(fact.sessionId()) && session.callId().equals(fact.callId()));
                var decision = fact.decision();
                validateDecision(decision);
                require(decisionIds.add(decision.decisionId()) && (previousDecision == null ||
                        compare(new Anchor(previousDecision.decidedAt(), previousDecision.decisionId()),
                                new Anchor(decision.decidedAt(), decision.decisionId())) < 0));
                previousDecision = decision;
                decisions.add(decision);
            }
            validateLegs(facts.legs());
            validateEvents(facts.callEvents());
            var evidenceReader = one(evidence);
            checkBudget(deadline);
            var evidenceFacts = evidenceReader.find(sessionId, session.callId());
            checkBudget(deadline);
            require(evidenceFacts != null);
            var summaries = new ArrayList<IvrEvidenceSummary>();
            UUID previousEvidence = null;
            for (var fact : evidenceFacts) {
                require(fact != null && fact.evidenceId() != null && member(fact.status(), Set.of("PENDING", "SOLIDIFIED", "QUARANTINED", "FAILED"))
                        && session.callId().equals(fact.callId()) && (fact.sessionId() == null || sessionId.equals(fact.sessionId()))
                        && (previousEvidence == null || uuidCompare(previousEvidence, fact.evidenceId()) < 0) && fact.artifactRoles() != null);
                require(fact.artifactRoles().equals(List.of()) || fact.artifactRoles().equals(List.of("ORIGINAL"))
                        || fact.artifactRoles().equals(List.of("DERIVED")) || fact.artifactRoles().equals(List.of("ORIGINAL", "DERIVED")));
                previousEvidence = fact.evidenceId();
                summaries.add(new IvrEvidenceSummary(fact.evidenceId(), fact.status(), fact.artifactRoles()));
            }
            return new IvrSessionDetail(session.sessionId(), session.callId(), session.subjectId(), session.status(), session.nodeId(),
                    session.protocolVersion(), session.promptVersion(), session.guidanceStatus(), session.callerMasked(), session.accessNumberMasked(),
                    session.createdAt(), session.confirmedAt(), call.call(), decisions, facts.legs(), facts.callEvents(), summaries);
        });
    }
    private <Result> Result execute(Caller caller, java.util.function.LongFunction<Result> read) {
        long deadline = nanoTime.getAsLong() + TimeUnit.SECONDS.toNanos(5);
        try {
            Result result = transaction.execute(status -> {
                try {
                    checkBudget(deadline);
                    context.enter();
                    checkBudget(deadline);
                    authority(caller);
                    checkBudget(deadline);
                    return read.apply(deadline);
                } finally { context.exit(); }
            });
            checkBudget(deadline);
            require(result != null);
            return result;
        } catch (Failure known) { throw known; }
        catch (org.jooq.exception.DataAccessException | org.springframework.dao.DataAccessException | TransactionException | BeansException failure) {
            throw unavailable();
        } catch (RuntimeException unexpected) { throw new Failure(Code.INTERNAL); }
    }
    private void authority(Caller caller) {
        if (caller == null || caller.employeeId() == null || caller.audience() == null || caller.audience().isBlank()) throw unavailable();
        if (!"onecall:employee-session:v1".equals(caller.audience())) throw new Failure(Code.UNAUTHENTICATED);
        var row = dsl.fetchOne("SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)", caller.employeeId());
        if (row == null) throw new Failure(Code.UNAUTHENTICATED);
        String status = row.get("status", String.class);
        String role = row.get("role", String.class);
        Integer revision = row.get("revision", Integer.class);
        require(caller.employeeId().equals(row.get("id", UUID.class)) && status != null && Set.of("ACTIVE", "DISABLED").contains(status)
                && role != null && Set.of("ADMIN", "OPS").contains(role) && revision != null && revision >= 1);
        if ("DISABLED".equals(status)) throw new Failure(Code.FORBIDDEN);
    }

    private static <Port> Port one(ObjectProvider<Port> provider) {
        try {
            var candidates = provider.stream().limit(2).toList();
            if (candidates.size() != 1 || candidates.getFirst() == null) throw unavailable();
            return candidates.getFirst();
        } catch (RuntimeException failure) { throw unavailable(); }
    }


    private void checkBudget(long deadline) { if (deadline - nanoTime.getAsLong() <= 0) throw unavailable(); }
    private static void validateSession(IvrSessionSummary fact) {
        require(fact != null && fact.sessionId() != null && text(fact.callId(), 128) && member(fact.status(), SESSION_STATUSES)
                && text(fact.nodeId(), 64) && text(fact.protocolVersion(), 32) && text(fact.promptVersion(), 64)
                && optionalText(fact.callerMasked(), 64) && text(fact.accessNumberMasked(), 64) && instant(fact.createdAt())
                && (fact.guidanceStatus() == null || member(fact.guidanceStatus(), Set.of("ACTIVATION_REQUIRED", "CONSENT_REQUIRED")))
                && ("CONFIRMED".equals(fact.status()) == (fact.confirmedAt() != null))
                && ordered(fact.createdAt(), fact.confirmedAt()));
    }
    private static void validateCall(IvrCallSummary call) {
        require(call != null && member(call.status(), Set.of("ACTIVE", "ENDED", "FAILED", "UNKNOWN", "REJECTED"))
                && (Set.of("ACTIVE", "UNKNOWN").contains(call.status()) == (call.endedAt() == null))
                && optionalInstant(call.startedAt()) && optionalInstant(call.endedAt()) && ordered(call.startedAt(), call.endedAt())
                && (call.authoritativeSeconds() == null || call.authoritativeSeconds() >= 0 && Set.of("ENDED", "FAILED").contains(call.status())));
    }
    private static void validateDecision(IvrDecisionSummary fact) {
        require(fact != null && fact.decisionId() != null && member(fact.purpose(), Set.of("INBOUND_RESOLVE", "ACTIVATION_NEXT_ACTION", "DIRECT_AUTHORIZATION"))
                && (fact.action() == null || member(fact.action(), ACTIONS)) && member(fact.reasonCode(), REASONS) && instant(fact.decidedAt()));
        if ("DIRECT_AUTHORIZATION".equals(fact.purpose())) {
            require(fact.action() == null && fact.reservationId() != null || member(fact.action(), Set.of("GUIDE_PURCHASE", "REJECT", "REJECT_SUSPENDED", "REJECT_CALL_IN_PROGRESS")) && fact.reservationId() == null);
        } else require(fact.action() != null && ("DIAL_TASK_CALLEE".equals(fact.action()) == (fact.reservationId() != null)));
        require((fact.action() == null) == "DIRECT_AUTHORIZED".equals(fact.reasonCode()));
    }
    private static void validateLegs(List<IvrLegSummary> legs) {
        var ids = new HashSet<String>();
        IvrLegSummary previous = null;
        for (var fact : legs) {
            require(fact != null && text(fact.legId(), 64) && ids.add(fact.legId())
                    && ("CALLER".equals(fact.legType()) && "INBOUND".equals(fact.direction()) || "CALLEE".equals(fact.legType()) && "OUTBOUND".equals(fact.direction()))
                    && member(fact.state(), Set.of("RINGING", "ANSWERED", "ENDED", "FAILED")) && instant(fact.startedAt())
                    && optionalInstant(fact.answeredAt()) && optionalInstant(fact.endedAt()) && ordered(fact.startedAt(), fact.answeredAt())
                    && ordered(fact.startedAt(), fact.endedAt()) && ordered(fact.answeredAt(), fact.endedAt()) && optionalText(fact.endReason(), 64));
            require(previous == null || previous.startedAt().isBefore(fact.startedAt()) || previous.startedAt().equals(fact.startedAt()) && cCompare(previous.legId(), fact.legId()) < 0);
            previous = fact;
        }
    }
    private static void validateEvents(List<IvrCallEventSummary> events) {
        var ids = new HashSet<String>(); var sequences = new HashSet<Long>();
        IvrCallEventSummary previous = null;
        for (var fact : events) {
            require(fact != null && text(fact.eventId(), 128) && ids.add(fact.eventId()) && text(fact.eventType(), 64)
                    && instant(fact.occurredAt()) && member(fact.processingStatus(), Set.of("RECEIVED", "APPLIED", "IGNORED", "QUARANTINED"))
                    && optionalText(fact.reasonCode(), 64) && (fact.sequenceNo() == null || fact.sequenceNo() >= 1 && sequences.add(fact.sequenceNo())));
            if (previous != null) {
                int sequence = Comparator.nullsLast(Long::compareTo).compare(previous.sequenceNo(), fact.sequenceNo());
                require(sequence < 0 || sequence == 0 && (previous.occurredAt().isBefore(fact.occurredAt())
                        || previous.occurredAt().equals(fact.occurredAt()) && cCompare(previous.eventId(), fact.eventId()) < 0));
            }
            previous = fact;
        }
    }
    private static int cCompare(String left, String right) { return Arrays.compareUnsigned(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8)); }
    private static int compare(Anchor left, Anchor right) { int time = left.createdAt().compareTo(right.createdAt()); return time != 0 ? time : uuidCompare(left.sessionId(), right.sessionId()); }
    private static int uuidCompare(UUID left, UUID right) {
        int most = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
        return most != 0 ? most : Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
    }
    private static boolean member(String value, Set<String> values) { return value != null && values.contains(value); }
    private static boolean text(String value, int maximum) { return value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= maximum; }
    private static boolean optionalText(String value, int maximum) { return value == null || text(value, maximum); }
    private static boolean instant(Instant value) { return value != null && value.getNano() % 1000 == 0; }
    private static boolean optionalInstant(Instant value) { return value == null || instant(value); }
    private static boolean ordered(Instant start, Instant end) { return optionalInstant(end) && (start == null || end == null || !end.isBefore(start)); }
    private static void require(boolean valid) { if (!valid) throw unavailable(); }
    private static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
}
