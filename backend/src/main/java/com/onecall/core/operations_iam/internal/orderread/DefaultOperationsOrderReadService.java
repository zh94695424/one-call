package com.onecall.core.operations_iam.internal.orderread;

import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Code;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.Failure;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import org.jooq.DSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionTemplate;

public final class DefaultOperationsOrderReadService implements OperationsOrderReadService {
    private static final Set<String> EMPLOYEE_ROLES = Set.of("ADMIN", "OPS");
    private final TransactionTemplate transaction;
    private final DSLContext dsl;
    private final OperationsReadContext context;
    private final ObjectProvider<OperationsSubjectReadProperties> properties;
    private final ObjectProvider<OrderReader> orders;
    private final ObjectProvider<IssuanceReader> issuances;
    private final LongSupplier nanoTime;

    public DefaultOperationsOrderReadService(TransactionTemplate transaction, DSLContext dsl, OperationsReadContext context,
            ObjectProvider<OperationsSubjectReadProperties> properties, ObjectProvider<OrderReader> orders,
            ObjectProvider<IssuanceReader> issuances) {
        this(transaction, dsl, context, properties, orders, issuances, System::nanoTime);
    }
    DefaultOperationsOrderReadService(TransactionTemplate transaction, DSLContext dsl, OperationsReadContext context,
            ObjectProvider<OperationsSubjectReadProperties> properties, ObjectProvider<OrderReader> orders,
            ObjectProvider<IssuanceReader> issuances, LongSupplier nanoTime) {
        this.transaction = transaction; this.dsl = dsl; this.context = context; this.properties = properties;
        this.orders = orders; this.issuances = issuances; this.nanoTime = nanoTime;
    }
    @Override public OrderPage list(Caller caller, RawListRequest raw) {
        return execute(caller, deadline -> {
            var request = OperationsOrderReadRequest.parse(raw);
            budget(deadline);
            var codec = new OperationsOrderCursorCodec(one(properties));
            Optional<Anchor> anchor = request.cursor() == null ? Optional.empty() : Optional.of(codec.decode(request.cursor(), request));
            var reader = one(orders);
            budget(deadline);
            var roots = reader.fetchOrders(request.filters(), anchor, request.limit() + 1);
            budget(deadline);
            require(roots != null && roots.size() <= request.limit() + 1, Code.INTERNAL);
            var identities = new HashSet<UUID>();
            Anchor previous = anchor.orElse(null);
            for (var root : roots) {
                require(root != null && root.orderId() != null && OperationsOrderCursorCodec.validInstant(root.createdAt()), Code.INTERNAL);
                var current = new Anchor(root.createdAt(), root.orderId());
                require(identities.add(root.orderId()) && (previous == null || compare(previous, current) > 0), Code.INTERNAL);
                require((request.filters().subjectId() == null || request.filters().subjectId().equals(root.subjectId()))
                        && (request.filters().paymentStatus() == null || request.filters().paymentStatus().equals(root.paymentStatus())), Code.INTERNAL);
                previous = current;
            }
            boolean more = roots.size() > request.limit();
            var emitted = roots.subList(0, Math.min(roots.size(), request.limit()));
            if (emitted.isEmpty()) return new OrderPage(List.of(), null, false);
            emitted.forEach(root -> validate(root, Code.INTERNAL));
            var statuses = issuance(emitted.stream().map(OrderFacts::orderId).toList(), Code.INTERNAL, deadline);
            var items = emitted.stream().map(root -> summary(root, statuses.get(root.orderId()))).toList();
            String next = null;
            if (more) {
                var last = emitted.getLast();
                next = codec.encode(new Anchor(last.createdAt(), last.orderId()), request);
            }
            budget(deadline);
            return new OrderPage(items, next, more);
        });
    }
    @Override public Order detail(Caller caller, RawDetailRequest raw) {
        return execute(caller, deadline -> {
            UUID identity = OperationsOrderReadRequest.detail(raw);
            var reader = one(orders);
            budget(deadline);
            var found = reader.fetchOrder(identity);
            budget(deadline);
            require(found != null, Code.INTERNAL);
            var detail = found.orElseThrow(() -> new Failure(Code.NOT_FOUND));
            var root = detail.order();
            validate(root, Code.NOT_FOUND);
            require(identity.equals(root.orderId()) && detail.purchaseConsentId() != null && detail.snapshot() != null
                    && detail.snapshotSha256() != null && detail.snapshotSha256().matches("[0-9a-f]{64}"), Code.NOT_FOUND);
            var snapshot = detail.snapshot();
            require(root.catalogVersionId().equals(snapshot.catalogVersionId()) && root.catalogOfferId().equals(snapshot.catalogOfferId())
                    && root.channel().equals(snapshot.channel()) && root.amountCents() == snapshot.amountCents()
                    && root.currency().equals(snapshot.currency()) && detail.snapshotSha256().equals(snapshot.snapshotSha256()), Code.NOT_FOUND);
            require("PAID".equals(root.paymentStatus()) == (detail.paidAt() != null)
                    && "CLOSED".equals(root.paymentStatus()) == (detail.closedAt() != null)
                    && (detail.paidAt() == null || OperationsOrderCursorCodec.validInstant(detail.paidAt()) && !detail.paidAt().isBefore(root.createdAt()))
                    && (detail.closedAt() == null || OperationsOrderCursorCodec.validInstant(detail.closedAt()) && !detail.closedAt().isBefore(root.createdAt())), Code.NOT_FOUND);
            String issuance = issuance(List.of(identity), Code.NOT_FOUND, deadline).get(identity);
            return new Order(root.orderId(), root.orderNo(), root.subjectId(), root.channel(), root.catalogVersionId(), root.catalogOfferId(),
                    root.amountCents(), root.currency(), root.paymentStatus(), issuance, root.createdAt(), detail.snapshotSha256(),
                    snapshot, detail.purchaseConsentId(), detail.paidAt(), detail.closedAt());
        });
    }
    private Map<UUID, String> issuance(List<UUID> identities, Code invalid, long deadline) {
        var reader = one(issuances);
        budget(deadline);
        var facts = reader.fetchIssuances(identities);
        budget(deadline);
        require(facts != null, invalid);
        var grouped = new HashMap<UUID, Set<String>>();
        identities.forEach(identity -> grouped.put(identity, new HashSet<>()));
        for (var fact : facts) {
            require(fact != null && fact.orderId() != null && grouped.containsKey(fact.orderId())
                    && fact.status() != null && Set.of("PENDING", "ISSUED", "FAILED").contains(fact.status()), invalid);
            grouped.get(fact.orderId()).add(fact.status());
        }
        var result = new HashMap<UUID, String>();
        grouped.forEach((identity, statuses) -> result.put(identity,
                statuses.isEmpty() ? "NONE" : statuses.size() == 1 ? statuses.iterator().next() : "MIXED"));
        return result;
    }
    private <Result> Result execute(Caller caller, LongFunction<Result> operation) {
        long deadline = nanoTime.getAsLong() + 5_000_000_000L;
        try {
            var result = transaction.execute(status -> {
                try {
                    context.enter();
                    budget(deadline);
                    authority(caller);
                    budget(deadline);
                    return operation.apply(deadline);
                } finally { context.exit(); }
            });
            require(result != null, Code.INTERNAL);
            budget(deadline);
            return result;
        } catch (RuntimeException failure) {
            Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
                if (cause instanceof java.sql.SQLException || cause instanceof org.jooq.exception.DataAccessException
                        || cause instanceof org.springframework.dao.DataAccessException
                        || cause instanceof org.springframework.transaction.TransactionException
                        || cause instanceof org.springframework.beans.BeansException
                        || cause instanceof ContractRejectionException)
                    throw new Failure(Code.SERVICE_UNAVAILABLE);
            }
            if (failure instanceof Failure known) throw known;
            throw new Failure(Code.INTERNAL);
        }
    }
    private void authority(Caller caller) {
        require(caller != null && caller.employeeId() != null && caller.audience() != null && !caller.audience().isBlank(), Code.SERVICE_UNAVAILABLE);
        require("onecall:employee-session:v1".equals(caller.audience()), Code.UNAUTHENTICATED);
        var row = dsl.fetchOne("SELECT id, status, role, revision FROM operations_iam.employee_account WHERE id = CAST(? AS uuid)", caller.employeeId());
        require(row != null, Code.UNAUTHENTICATED);
        try {
            String status = row.get("status", String.class);
            String role = row.get("role", String.class);
            Integer revision = row.get("revision", Integer.class);
            require(caller.employeeId().equals(row.get("id", UUID.class)) && status != null && Set.of("ACTIVE", "DISABLED").contains(status)
                    && role != null && EMPLOYEE_ROLES.contains(role) && revision != null && revision >= 1, Code.SERVICE_UNAVAILABLE);
            require(!"DISABLED".equals(status), Code.FORBIDDEN);
        } catch (Failure known) { throw known; }
        catch (RuntimeException malformed) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }
    private static void validate(OrderFacts root, Code invalid) {
        require(root != null && root.orderId() != null && root.subjectId() != null && root.catalogVersionId() != null
                && root.catalogOfferId() != null && root.orderNo() != null && !root.orderNo().isBlank()
                && root.orderNo().codePointCount(0, root.orderNo().length()) <= 64 && root.orderNo().indexOf('\0') < 0
                && StandardCharsets.UTF_8.newEncoder().canEncode(root.orderNo()) && root.channel() != null
                && Set.of("WEB", "MINIAPP").contains(root.channel()) && root.amountCents() != null && root.amountCents() >= 0
                && root.amountCents() <= 9007199254740991L && "CNY".equals(root.currency()) && root.paymentStatus() != null
                && OperationsOrderReadPorts.PAYMENT_STATUSES.contains(root.paymentStatus())
                && OperationsOrderCursorCodec.validInstant(root.createdAt()), invalid);
    }
    private static OrderSummary summary(OrderFacts root, String issuance) {
        return new OrderSummary(root.orderId(), root.orderNo(), root.subjectId(), root.channel(), root.catalogVersionId(), root.catalogOfferId(),
                root.amountCents(), root.currency(), root.paymentStatus(), issuance, root.createdAt());
    }
    private static int compare(Anchor left, Anchor right) {
        int time = left.createdAt().compareTo(right.createdAt());
        if (time != 0) return time;
        int most = Long.compareUnsigned(left.orderId().getMostSignificantBits(), right.orderId().getMostSignificantBits());
        return most == 0 ? Long.compareUnsigned(left.orderId().getLeastSignificantBits(), right.orderId().getLeastSignificantBits()) : most;
    }
    private void budget(long deadline) { require(deadline - nanoTime.getAsLong() > 0, Code.SERVICE_UNAVAILABLE); }
    private static <Port> Port one(ObjectProvider<Port> provider) {
        try {
            var found = provider.stream().limit(2).toList();
            require(found.size() == 1 && found.getFirst() != null, Code.SERVICE_UNAVAILABLE);
            return found.getFirst();
        } catch (RuntimeException failure) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }
    private static void require(boolean valid, Code code) { if (!valid) throw new Failure(code); }
}
