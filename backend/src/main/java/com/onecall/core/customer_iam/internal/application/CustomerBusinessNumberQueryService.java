package com.onecall.core.customer_iam.internal.application;

import static com.onecall.core.customer_iam.internal.application.CustomerContextException.Code.*;

import com.onecall.core.activation_consent.customerquery.CustomerActivationStatusReader;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Anchor;
import com.onecall.core.subject_number.customerquery.CustomerBusinessNumberReader.Scope;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.transaction.support.TransactionTemplate;

public final class CustomerBusinessNumberQueryService {
    private final TransactionTemplate transaction;
    private final DSLContext context;
    private final JooqCustomerBusinessNumberAccessRepository repository;
    private final CustomerBusinessNumberReader reader;
    private final CustomerActivationStatusReader activationReader;

    public CustomerBusinessNumberQueryService(TransactionTemplate transaction, DSLContext context,
            JooqCustomerBusinessNumberAccessRepository repository, CustomerBusinessNumberReader reader,
            CustomerActivationStatusReader activationReader) {
        this.transaction = transaction;
        this.context = context;
        this.repository = repository;
        this.reader = reader;
        this.activationReader = activationReader;
    }

    public record Request(Optional<Anchor> anchor, Optional<Integer> limit) {
        public Request {
            if (anchor == null || limit == null || limit.filter(value -> value < 1 || value > 100).isPresent())
                throw new CustomerContextException(VALIDATION_FAILED);
        }
        public int effectiveLimit() { return limit.orElse(20); }
    }

    public record Item(UUID bindingId, UUID subjectId, String numberMasked, String numberType,
            String bindingStatus, String activationStatus, String realNameStatus) { }
    public record Page(List<Item> items, Optional<Anchor> nextAnchor, boolean hasMore) {
        public Page { items = List.copyOf(items); }
    }

    public Page listCustomerBusinessNumbers(TrustedCustomerCaller caller, Request request) {
        Scope scope = requireScope(caller);
        if (request == null) throw new CustomerContextException(VALIDATION_FAILED);
        try {
            Page page = transaction.execute(status -> {
                try {
                    context.execute("SET LOCAL ROLE customer_iam_role");
                    var role = context.fetchOne("select current_user");
                    if (role == null || !"customer_iam_role".equals(role.get(0, String.class)))
                        throw new CustomerContextException(SERVICE_UNAVAILABLE);
                } catch (RuntimeException failure) {
                    throw new CustomerContextException(SERVICE_UNAVAILABLE);
                }
                repository.requireActiveContext(scope);
                return compose(scope, request);
            });
            if (page == null) throw new CustomerContextException(INTERNAL);
            return page;
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

    public static Scope requireScope(TrustedCustomerCaller caller) {
        if (caller == null || caller.accountId() == null || caller.selectedWorkspace() == null)
            throw new CustomerContextException(SERVICE_UNAVAILABLE);
        var selected = caller.selectedWorkspace();
        if (selected.workspaceType() == CustomerContext.WorkspaceType.PERSONAL && selected.organizationId() == null
                || selected.workspaceType() == CustomerContext.WorkspaceType.ORGANIZATION && selected.organizationId() != null)
            return new Scope(caller.accountId(), selected.organizationId());
        throw new CustomerContextException(SERVICE_UNAVAILABLE);
    }

    private Page compose(Scope scope, Request request) {
        var rows = reader.fetch(scope, request.anchor(), request.effectiveLimit() + 1);
        if (rows == null || rows.size() > request.effectiveLimit() + 1) throw new CustomerContextException(INTERNAL);
        Set<UUID> subjects = new HashSet<>();
        Set<UUID> bindings = new HashSet<>();
        Anchor previous = request.anchor().orElse(null);
        for (var row : rows) {
            if (row == null || row.bindingId() == null || row.subjectId() == null || row.subjectCreatedAt() == null
                    || row.subjectCreatedAt().getNano() % 1000 != 0 || !bindings.add(row.bindingId())
                    || row.numberMasked() == null || row.numberMasked().chars().allMatch(value -> value == ' ') || row.numberMasked().codePointCount(0, row.numberMasked().length()) > 64
                    || row.numberMasked().indexOf('\0') >= 0 || !java.nio.charset.StandardCharsets.UTF_8.newEncoder().canEncode(row.numberMasked())
                    || !Set.of("MOBILE", "LANDLINE").contains(row.numberType())
                    || !Set.of("ACTIVE", "DISABLED", "RETIRED").contains(row.bindingStatus())
                    || !Set.of("UNVERIFIED", "VERIFIED").contains(row.realNameStatus())) throw new CustomerContextException(INTERNAL);
            Anchor current = new Anchor(row.subjectCreatedAt(), row.bindingId());
            if (previous != null && (current.createdAt().isAfter(previous.createdAt())
                    || current.createdAt().equals(previous.createdAt())
                    && current.bindingId().toString().compareTo(previous.bindingId().toString()) <= 0))
                throw new CustomerContextException(INTERNAL);
            previous = current;
            subjects.add(row.subjectId());
        }
        var statuses = activationReader.findStatuses(Set.copyOf(subjects));
        if (statuses == null || !subjects.containsAll(statuses.keySet())
                || statuses.values().stream().anyMatch(value -> value == null || !Set.of("INACTIVE", "ACTIVE", "SUSPENDED").contains(value)))
            throw new CustomerContextException(INTERNAL);
        boolean more = rows.size() > request.effectiveLimit();
        var returned = rows.subList(0, Math.min(rows.size(), request.effectiveLimit()));
        List<Item> items = new ArrayList<>();
        for (var row : returned) items.add(new Item(row.bindingId(), row.subjectId(), row.numberMasked(), row.numberType(),
                row.bindingStatus(), statuses.getOrDefault(row.subjectId(), "INACTIVE"), row.realNameStatus()));
        Optional<Anchor> next = more ? Optional.of(new Anchor(returned.getLast().subjectCreatedAt(), returned.getLast().bindingId())) : Optional.empty();
        return new Page(items, next, more);
    }
}
