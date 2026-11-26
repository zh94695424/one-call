package com.onecall.core.customer_iam.internal.application;

import static com.onecall.core.customer_iam.internal.application.CustomerContextException.Code.*;

import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.Snapshot;
import com.onecall.core.catalog_order.customerorder.CustomerOrderSnapshotReader;
import com.onecall.core.catalog_order.customerquery.CustomerOrderReader;
import com.onecall.core.catalog_order.customerquery.CustomerOrderReader.*;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.jooq.DSLContext;
import org.springframework.transaction.support.TransactionTemplate;

public final class CustomerOrderQueryService {
    private final TransactionTemplate transaction;
    private final DSLContext context;
    private final JooqCustomerBusinessNumberAccessRepository lifecycle;
    private final CustomerOrderReader reader;
    private final CustomerOrderSnapshotReader snapshots;

    public CustomerOrderQueryService(TransactionTemplate transaction, DSLContext context,
            JooqCustomerBusinessNumberAccessRepository lifecycle, CustomerOrderReader reader,
            CustomerOrderSnapshotReader snapshots) {
        this.transaction = transaction;
        this.context = context;
        this.lifecycle = lifecycle;
        this.reader = reader;
        this.snapshots = snapshots;
    }

    public record Request(Optional<Anchor> anchor, Optional<Integer> limit) {
        public Request {
            if (anchor == null || limit == null || limit.filter(value -> value < 1 || value > 100).isPresent())
                throw new CustomerContextException(VALIDATION_FAILED);
        }
        public int effectiveLimit() { return limit.orElse(20); }
    }
    public record Summary(UUID orderId, String orderNo, UUID subjectId, String channel, UUID catalogVersionId,
            UUID catalogOfferId, long amountCents, String currency, String paymentStatus, String issuanceStatus,
            Instant createdAt) {}
    public record Detail(Summary summary, String snapshotSha256, Snapshot snapshot, UUID purchaseConsentId,
            Instant paidAt, Instant closedAt) {}
    public record Page(List<Summary> items, Optional<Anchor> nextAnchor, boolean hasMore) {
        public Page { items = List.copyOf(items); }
    }

    public static Scope requireScope(TrustedCustomerCaller caller) {
        if (caller == null || caller.accountId() == null || caller.selectedWorkspace() == null)
            throw new CustomerContextException(FORBIDDEN);
        var workspace = caller.selectedWorkspace();
        if (workspace.workspaceType() == CustomerContext.WorkspaceType.PERSONAL && workspace.organizationId() == null
                || workspace.workspaceType() == CustomerContext.WorkspaceType.ORGANIZATION && workspace.organizationId() != null)
            return new Scope(caller.accountId(), workspace.organizationId());
        throw new CustomerContextException(FORBIDDEN);
    }

    public Page listCustomerOrders(TrustedCustomerCaller caller, Request request) {
        Scope scope = requireScope(caller);
        if (request == null) throw new CustomerContextException(VALIDATION_FAILED);
        return execute(scope, () -> page(scope, request));
    }

    public Optional<Detail> getCustomerOrder(TrustedCustomerCaller caller, UUID orderId) {
        Scope scope = requireScope(caller);
        if (orderId == null) throw new CustomerContextException(VALIDATION_FAILED);
        return execute(scope, () -> {
            Optional<DetailRow> found = reader.fetchOrder(scope, orderId);
            if (found == null) throw new CustomerContextException(INTERNAL);
            if (found.isEmpty()) return Optional.empty();
            var row = found.get();
            try {
                validate(row.summary());
                if (!orderId.equals(row.summary().orderId()) || row.purchaseConsentId() == null
                        || !paymentShape(row)) throw new CustomerContextException(INTERNAL);
                var statuses = issuance(List.of(orderId));
                Snapshot snapshot = snapshots.read(row.snapshot(), row.snapshotSha256());
                if (snapshot == null || !row.summary().catalogVersionId().equals(snapshot.catalogVersionId())
                        || !row.summary().catalogOfferId().equals(snapshot.catalogOfferId())
                        || !row.summary().channel().equals(snapshot.channel())
                        || row.summary().amountCents() != snapshot.amountCents()
                        || !row.summary().currency().equals(snapshot.currency())
                        || !java.util.Objects.equals(row.snapshotSha256(), snapshot.snapshotSha256()))
                    throw new CustomerContextException(INTERNAL);
                return Optional.of(new Detail(project(row.summary(), statuses.get(orderId)), row.snapshotSha256(),
                        snapshot, row.purchaseConsentId(), row.paidAt(), row.closedAt()));
            } catch (CustomerOrderSnapshotReader.InvalidSnapshotException corrupt) {
                return Optional.empty();
            } catch (CustomerContextException corrupt) {
                if (corrupt.code() == INTERNAL) return Optional.empty();
                throw corrupt;
            }
        });
    }

    private Page page(Scope scope, Request request) {
        var rows = reader.fetchOrders(scope, request.anchor(), request.effectiveLimit() + 1);
        if (rows == null || rows.size() > request.effectiveLimit() + 1) throw new CustomerContextException(INTERNAL);
        Set<UUID> identities = new HashSet<>();
        Anchor previous = request.anchor().orElse(null);
        for (var row : rows) {
            if (row == null || row.orderId() == null || !validInstant(row.createdAt()) || !identities.add(row.orderId()))
                throw new CustomerContextException(INTERNAL);
            Anchor current = new Anchor(row.createdAt(), row.orderId());
            if (previous != null && (current.createdAt().isAfter(previous.createdAt())
                    || current.createdAt().equals(previous.createdAt())
                    && current.orderId().toString().compareTo(previous.orderId().toString()) >= 0))
                throw new CustomerContextException(INTERNAL);
            previous = current;
        }
        boolean more = rows.size() > request.effectiveLimit();
        var returned = rows.subList(0, Math.min(rows.size(), request.effectiveLimit()));
        if (returned.isEmpty()) return new Page(List.of(), Optional.empty(), false);
        returned.forEach(CustomerOrderQueryService::validate);
        Map<UUID, String> statuses = issuance(returned.stream().map(SummaryRow::orderId).toList());
        List<Summary> items = new ArrayList<>();
        returned.forEach(row -> items.add(project(row, statuses.get(row.orderId()))));
        var last = returned.getLast();
        return new Page(items, more ? Optional.of(new Anchor(last.createdAt(), last.orderId())) : Optional.empty(), more);
    }

    private Map<UUID, String> issuance(List<UUID> identities) {
        var rows = reader.fetchIssuances(identities);
        if (rows == null) throw new CustomerContextException(INTERNAL);
        Map<UUID, Set<String>> statuses = new HashMap<>();
        identities.forEach(identity -> statuses.put(identity, new HashSet<>()));
        for (var row : rows) {
            if (row == null || row.orderId() == null || !statuses.containsKey(row.orderId()) || row.status() == null
                    || !Set.of("PENDING", "ISSUED", "FAILED").contains(row.status()))
                throw new CustomerContextException(INTERNAL);
            statuses.get(row.orderId()).add(row.status());
        }
        Map<UUID, String> result = new HashMap<>();
        statuses.forEach((identity, values) -> result.put(identity,
                values.isEmpty() ? "NONE" : values.size() == 1 ? values.iterator().next() : "MIXED"));
        return result;
    }

    private <Result> Result execute(Scope scope, Supplier<Result> action) {
        try {
            Result result = transaction.execute(status -> {
                try {
                    context.execute("SET LOCAL ROLE customer_iam_role");
                    var role = context.fetchOne("select current_user");
                    if (role == null || !"customer_iam_role".equals(role.get(0, String.class)))
                        throw new CustomerContextException(SERVICE_UNAVAILABLE);
                } catch (RuntimeException failure) { throw new CustomerContextException(SERVICE_UNAVAILABLE); }
                lifecycle.requireActiveContext(new com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope(
                        scope.accountId(), scope.organizationId()));
                return action.get();
            });
            if (result == null) throw new CustomerContextException(INTERNAL);
            return result;
        } catch (RuntimeException failure) {
            if (failure instanceof CustomerContextException known) throw new CustomerContextException(known.code());
            Set<Throwable> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
                if (cause instanceof java.sql.SQLException || cause instanceof org.springframework.dao.DataAccessException
                        || cause instanceof org.jooq.exception.DataAccessException
                        || cause instanceof org.springframework.transaction.TransactionException
                        || cause instanceof org.springframework.beans.BeansException)
                    throw new CustomerContextException(SERVICE_UNAVAILABLE);
            }
            throw new CustomerContextException(INTERNAL);
        }
    }

    private static void validate(SummaryRow row) {
        if (row == null || row.orderId() == null || row.subjectId() == null || row.catalogVersionId() == null
                || row.catalogOfferId() == null || row.orderNo() == null || row.orderNo().isBlank()
                || row.orderNo().codePointCount(0, row.orderNo().length()) > 64 || row.orderNo().indexOf('\0') >= 0
                || !StandardCharsets.UTF_8.newEncoder().canEncode(row.orderNo()) || row.channel() == null
                || !Set.of("WEB", "MINIAPP").contains(row.channel()) || row.amountCents() == null
                || row.amountCents() < 0 || row.amountCents() > 9007199254740991L || !"CNY".equals(row.currency())
                || row.paymentStatus() == null || !Set.of("UNPAID", "UNKNOWN", "PAID", "CLOSED").contains(row.paymentStatus())
                || !validInstant(row.createdAt())) throw new CustomerContextException(INTERNAL);
    }

    private static boolean validInstant(Instant value) {
        return value != null && value.getNano() % 1000 == 0
                && !value.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
                && !value.isAfter(Instant.parse("9999-12-31T23:59:59.999999Z"));
    }

    private static boolean paymentShape(DetailRow row) {
        return "PAID".equals(row.summary().paymentStatus()) == (row.paidAt() != null)
                && "CLOSED".equals(row.summary().paymentStatus()) == (row.closedAt() != null)
                && (row.paidAt() == null || validInstant(row.paidAt()) && !row.paidAt().isBefore(row.summary().createdAt()))
                && (row.closedAt() == null || validInstant(row.closedAt()) && !row.closedAt().isBefore(row.summary().createdAt()));
    }

    private static Summary project(SummaryRow row, String status) {
        return new Summary(row.orderId(), row.orderNo(), row.subjectId(), row.channel(), row.catalogVersionId(), row.catalogOfferId(),
                row.amountCents(), row.currency(), row.paymentStatus(), status, row.createdAt());
    }
}
