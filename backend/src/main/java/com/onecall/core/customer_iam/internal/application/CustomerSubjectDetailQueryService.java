package com.onecall.core.customer_iam.internal.application;

import static com.onecall.core.customer_iam.internal.application.CustomerContextException.Code.*;

import com.onecall.core.activation_consent.customerquery.CustomerActivationStatusReader;
import com.onecall.core.catalog_order.customerquery.CustomerSubjectOrderCountReader;
import com.onecall.core.customer_iam.internal.persistence.JooqCustomerBusinessNumberAccessRepository;
import com.onecall.core.evidence.customerquery.CustomerSubjectEvidenceCountReader;
import com.onecall.core.subject_number.customersubjectquery.CustomerSubjectAccessReader;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.transaction.support.TransactionTemplate;

public final class CustomerSubjectDetailQueryService {
    private final TransactionTemplate transaction;
    private final DSLContext context;
    private final JooqCustomerBusinessNumberAccessRepository repository;
    private final CustomerSubjectAccessReader subjectReader;
    private final CustomerActivationStatusReader activationReader;
    private final CustomerSubjectEvidenceCountReader evidenceReader;
    private final CustomerSubjectOrderCountReader orderReader;

    public CustomerSubjectDetailQueryService(TransactionTemplate transaction, DSLContext context,
            JooqCustomerBusinessNumberAccessRepository repository, CustomerSubjectAccessReader subjectReader,
            CustomerActivationStatusReader activationReader, CustomerSubjectEvidenceCountReader evidenceReader,
            CustomerSubjectOrderCountReader orderReader) {
        this.transaction = transaction;
        this.context = context;
        this.repository = repository;
        this.subjectReader = subjectReader;
        this.activationReader = activationReader;
        this.evidenceReader = evidenceReader;
        this.orderReader = orderReader;
    }

    public record Detail(UUID subjectId, String activationStatus, long evidenceCount, long orderCount) { }

    public Optional<Detail> getCustomerSubject(TrustedCustomerCaller caller, UUID subjectId) {
        var scope = CustomerBusinessNumberQueryService.requireScope(caller);
        if (subjectId == null) throw new CustomerContextException(VALIDATION_FAILED);
        try {
            Optional<Detail> result = transaction.execute(status -> {
                try {
                    context.execute("SET LOCAL ROLE customer_iam_role");
                    var role = context.fetchOne("select current_user");
                    if (role == null || !"customer_iam_role".equals(role.get(0, String.class)))
                        throw new CustomerContextException(SERVICE_UNAVAILABLE);
                } catch (RuntimeException failure) {
                    throw new CustomerContextException(SERVICE_UNAVAILABLE);
                }
                repository.requireActiveContext(scope);
                var owned = subjectReader.findOwnedSubject(subjectId, scope.accountId(), scope.organizationId());
                if (owned == null) throw new CustomerContextException(INTERNAL);
                if (owned.isEmpty()) return Optional.empty();
                if (!subjectId.equals(owned.get())) throw new CustomerContextException(INTERNAL);
                if (subjectReader.hasOpenRisk(subjectId)) throw new CustomerContextException(FORBIDDEN);
                var statuses = activationReader.findStatuses(Set.of(subjectId));
                if (statuses == null || !Set.of(subjectId).containsAll(statuses.keySet())
                        || statuses.values().stream().anyMatch(value -> value == null
                            || !Set.of("INACTIVE", "ACTIVE", "SUSPENDED").contains(value)))
                    throw new CustomerContextException(INTERNAL);
                long evidenceCount = evidenceReader.countForSubject(subjectId);
                long orderCount = orderReader.countForWorkspace(subjectId, scope.accountId(), scope.organizationId());
                if (evidenceCount < 0 || orderCount < 0) throw new CustomerContextException(INTERNAL);
                return Optional.of(new Detail(subjectId, statuses.getOrDefault(subjectId, "INACTIVE"), evidenceCount, orderCount));
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
}
