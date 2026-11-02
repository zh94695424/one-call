package com.onecall.core.customer_iam.internal.application;

import static com.onecall.core.customer_iam.internal.application.CustomerContextException.Code.*;

import com.onecall.core.customer_iam.internal.persistence.CustomerContextRepository;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

public final class CustomerContextService {
    private final TransactionTemplate transaction;
    private final DSLContext context;
    private final CustomerContextRepository repository;

    public CustomerContextService(@Qualifier("customerQueryTransactionTemplate") TransactionTemplate transaction,
            @Qualifier("customerQueryDsl") DSLContext context,
            @Qualifier("customerContextRepository") CustomerContextRepository repository) {
        this.transaction = transaction;
        this.context = context;
        this.repository = repository;
    }

    public CustomerContext getCustomerMe(TrustedCustomerCaller caller) {
        try {
            if (caller == null || caller.accountId() == null || caller.channel() == null) {
                throw new CustomerContextException(INTERNAL);
            }
            CustomerContext result = transaction.execute(status -> {
                context.execute("SET LOCAL ROLE customer_iam_role");
                var role = context.fetchOne("select current_user");
                if (role == null || !"customer_iam_role".equals(role.get(0, String.class))) {
                    throw new CustomerContextException(SERVICE_UNAVAILABLE);
                }
                CustomerContext projection = repository.findActiveContext(caller)
                        .orElseThrow(() -> new CustomerContextException(UNAUTHENTICATED));
                validate(projection, caller);
                return projection;
            });
            if (result == null) { throw new CustomerContextException(INTERNAL); }
            return result;
        } catch (RuntimeException failure) {
            if (failure instanceof CustomerContextException known) {
                throw new CustomerContextException(known.code());
            }
            throw new CustomerContextException(unavailable(failure) ? SERVICE_UNAVAILABLE : INTERNAL);
        }
    }

    private static void validate(CustomerContext projection, TrustedCustomerCaller caller) {
        if (projection == null || projection.account() == null) { throw new CustomerContextException(INTERNAL); }
        var account = projection.account();
        if (account.status() == CustomerContext.AccountStatus.DISABLED) {
            throw new CustomerContextException(UNAUTHENTICATED);
        }
        if (account.status() != CustomerContext.AccountStatus.ACTIVE || account.displayName() == null
                || !caller.accountId().equals(account.accountId()) || projection.channel() != caller.channel()
                || projection.workspaces().isEmpty()) {
            throw new CustomerContextException(INTERNAL);
        }
        Set<UUID> organizations = new HashSet<>();
        for (int index = 0; index < projection.workspaces().size(); index++) {
            var workspace = projection.workspaces().get(index);
            if (workspace.status() != CustomerContext.WorkspaceStatus.ACTIVE) {
                throw new CustomerContextException(INTERNAL);
            }
            if (index == 0) {
                if (workspace.workspaceType() != CustomerContext.WorkspaceType.PERSONAL
                        || workspace.organizationId() != null || workspace.organizationDisplayName() != null
                        || workspace.role() != null) { throw new CustomerContextException(INTERNAL); }
            } else if (workspace.workspaceType() != CustomerContext.WorkspaceType.ORGANIZATION
                    || workspace.organizationId() == null || workspace.organizationDisplayName() == null
                    || workspace.role() == null || !organizations.add(workspace.organizationId())) {
                throw new CustomerContextException(INTERNAL);
            }
        }
    }

    private static boolean unavailable(Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof SQLException || cause instanceof org.springframework.dao.DataAccessException
                    || cause instanceof org.jooq.exception.DataAccessException
                    || cause instanceof org.springframework.transaction.TransactionException
                    || cause instanceof org.springframework.beans.BeansException) { return true; }
        }
        return false;
    }
}
