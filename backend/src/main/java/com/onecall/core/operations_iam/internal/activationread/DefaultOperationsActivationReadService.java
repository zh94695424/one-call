package com.onecall.core.operations_iam.internal.activationread;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadService;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import org.jooq.DSLContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

public final class DefaultOperationsActivationReadService implements OperationsActivationReadService {
    private static final long REQUEST_BUDGET_NANOS = TimeUnit.SECONDS.toNanos(5);
    private final TransactionTemplate transaction;
    private final DSLContext dsl;
    private final OperationsReadContext context;
    private final ObjectProvider<OperationsSubjectReadProperties> properties;
    private final ObjectProvider<ActivationHistoryReader> readers;
    private final LongSupplier nanoTime;

    public DefaultOperationsActivationReadService(TransactionTemplate transaction, DSLContext dsl, OperationsReadContext context,
            ObjectProvider<OperationsSubjectReadProperties> properties, ObjectProvider<ActivationHistoryReader> readers) {
        this(transaction, dsl, context, properties, readers, System::nanoTime);
    }

    DefaultOperationsActivationReadService(TransactionTemplate transaction, DSLContext dsl, OperationsReadContext context,
            ObjectProvider<OperationsSubjectReadProperties> properties, ObjectProvider<ActivationHistoryReader> readers, LongSupplier nanoTime) {
        this.transaction = transaction; this.dsl = dsl; this.context = context;
        this.properties = properties; this.readers = readers; this.nanoTime = nanoTime;
    }

    @Override public ActivationPage list(Caller caller, RawListRequest raw) {
        long deadline = nanoTime.getAsLong() + REQUEST_BUDGET_NANOS;
        try {
            ActivationPage page = transaction.execute(status -> {
                try {
                    checkBudget(deadline);
                    context.enter();
                    checkBudget(deadline);
                    authority(caller);
                    checkBudget(deadline);
                    return read(raw, deadline);
                } finally { context.exit(); }
            });
            checkBudget(deadline);
            if (page == null) throw unavailable();
            return page;
        } catch (Failure known) {
            throw switch (known.code()) {
                case CURSOR_INVALID, UNAUTHENTICATED, FORBIDDEN, VALIDATION_FAILED, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL -> known;
                default -> new Failure(Code.INTERNAL);
            };
        } catch (org.jooq.exception.DataAccessException | org.springframework.dao.DataAccessException | TransactionException | BeansException failure) {
            throw unavailable();
        } catch (RuntimeException unexpected) { throw new Failure(Code.INTERNAL); }
    }

    private ActivationPage read(RawListRequest raw, long deadline) {
        var request = OperationsActivationReadRequest.parse(raw);
        checkBudget(deadline);
        var codec = new OperationsActivationCursorCodec(one(properties));
        checkBudget(deadline);
        Optional<Anchor> anchor = request.cursor() == null ? Optional.empty() : Optional.of(codec.decode(request.cursor()));
        checkBudget(deadline);
        var reader = one(readers);
        checkBudget(deadline);
        var facts = reader.fetch(anchor, request.limit() + 1);
        checkBudget(deadline);
        require(facts != null && facts.size() <= request.limit() + 1);
        var items = new ArrayList<ActivationSummary>();
        var seen = new HashSet<UUID>();
        Anchor previous = anchor.orElse(null);
        for (var fact : facts) {
            validate(fact);
            require(seen.add(fact.transitionId()));
            Anchor current = new Anchor(fact.occurredAt(), fact.transitionId());
            if (previous != null) require(before(current, previous));
            previous = current;
            if (items.size() < request.limit()) {
                items.add(new ActivationSummary(fact.transitionId(), fact.subjectId(), fact.statusEventId(), fact.fromStatus(), fact.toStatus(),
                        fact.fromRevision(), fact.toRevision(), fact.sourceChannel(), fact.sourceActionDomain(), fact.reasonCode(), fact.occurredAt()));
            }
        }
        boolean hasMore = facts.size() > request.limit();
        String cursor = hasMore ? codec.encode(new Anchor(items.getLast().occurredAt(), items.getLast().transitionId())) : null;
        checkBudget(deadline);
        return new ActivationPage(items, cursor, hasMore);
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

    private void checkBudget(long deadline) {
        if (deadline - nanoTime.getAsLong() <= 0) throw unavailable();
    }

    private static void validate(TransitionFacts fact) {
        require(fact != null && fact.transitionId() != null && fact.subjectId() != null && text(fact.statusEventId(), 128)
                && text(fact.reasonCode(), 64) && fact.occurredAt() != null && fact.occurredAt().getNano() % 1000 == 0
                && fact.fromRevision() != null && fact.toRevision() != null && fact.fromRevision() >= 0 && fact.toRevision() >= 1
                && (long) fact.toRevision() == (long) fact.fromRevision() + 1);
        boolean initial = "INACTIVE".equals(fact.fromStatus()) && "ACTIVE".equals(fact.toStatus());
        boolean operations = "OPERATIONS".equals(fact.sourceChannel()) && "OPERATIONS".equals(fact.sourceActionDomain());
        boolean activation = ("MINIAPP".equals(fact.sourceChannel()) || "WEB".equals(fact.sourceChannel()))
                && "ACTIVATION".equals(fact.sourceActionDomain())
                || "IVR".equals(fact.sourceChannel()) && "IVR_CONFIRM".equals(fact.sourceActionDomain());
        require(initial && activation || operations && ("ACTIVE".equals(fact.fromStatus()) && "SUSPENDED".equals(fact.toStatus())
                || "SUSPENDED".equals(fact.fromStatus()) && "ACTIVE".equals(fact.toStatus())));
    }

    private static boolean text(String value, int maximum) {
        return value != null && !value.replace(" ", "").isEmpty() && value.codePointCount(0, value.length()) <= maximum;
    }

    private static boolean before(Anchor current, Anchor previous) {
        int time = current.occurredAt().compareTo(previous.occurredAt());
        int most = Long.compareUnsigned(current.transitionId().getMostSignificantBits(), previous.transitionId().getMostSignificantBits());
        int least = Long.compareUnsigned(current.transitionId().getLeastSignificantBits(), previous.transitionId().getLeastSignificantBits());
        return time < 0 || time == 0 && (most < 0 || most == 0 && least < 0);
    }

    private static void require(boolean valid) { if (!valid) throw unavailable(); }
    private static Failure unavailable() { return new Failure(Code.SERVICE_UNAVAILABLE); }
}
