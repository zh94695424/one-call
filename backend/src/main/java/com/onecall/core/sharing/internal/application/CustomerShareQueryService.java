package com.onecall.core.sharing.internal.application;

import static com.onecall.core.sharing.internal.application.ShareQueryException.Code.*;

import com.onecall.core.sharing.internal.persistence.ShareQueryRepository;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

public final class CustomerShareQueryService {
    private static final Logger LOG = LoggerFactory.getLogger(CustomerShareQueryService.class);
    private final TransactionTemplate transaction;
    private final DSLContext context;
    private final ShareQueryRepository repository;

    public CustomerShareQueryService(TransactionTemplate transaction, DSLContext context, ShareQueryRepository repository) {
        this.transaction = transaction;
        this.context = context;
        this.repository = repository;
    }

    public SharePage listCustomerShares(TrustedShareCaller caller, SharePageRequest request) {
        boolean[] rolePassed = {false};
        try {
            int limit = validate(caller, request);
            return transaction.execute(status -> {
                context.execute("SET LOCAL ROLE sharing_role");
                if (!"sharing_role".equals(context.fetchOne("select current_user").get(0, String.class))) {
                    throw new ShareQueryException(SERVICE_UNAVAILABLE);
                }
                rolePassed[0] = true;
                repository.requireActiveCreatorContext(caller);
                List<ShareSummary> rows = repository.fetch(caller, request, limit + 1);
                if (rows.size() > limit + 1) {
                    throw new IllegalStateException("Invalid query result");
                }
                boolean more = rows.size() > limit;
                List<ShareSummary> items = List.copyOf(rows.subList(0, Math.min(rows.size(), limit)));
                Optional<ShareCursor> next = Optional.empty();
                if (more) {
                    ShareSummary tail = items.getLast();
                    next = Optional.of(new ShareCursor(caller.accountId(), caller.organizationId(), tail.validFrom(), tail.shareId()));
                }
                return new SharePage(items, next, more);
            });
        } catch (RuntimeException failure) {
            Optional<String> state = sqlState(failure);
            ShareQueryException.Code code = failure instanceof ShareQueryException known ? known.code()
                    : state.isPresent() || failure instanceof org.springframework.dao.DataAccessException
                            || failure instanceof org.springframework.transaction.TransactionException
                            ? SERVICE_UNAVAILABLE : INTERNAL;
            LOG.warn("{}", new ShareQueryDiagnostic("listCustomerShares", code, failure.getClass().getName(),
                    state, state.map(value -> value.substring(0, 2)).orElse("NON_SQL"), rolePassed[0]));
            throw new ShareQueryException(code);
        }
    }

    private static int validate(TrustedShareCaller caller, SharePageRequest request) {
        if (caller == null || caller.accountId() == null || caller.organizationId() == null) {
            throw new ShareQueryException(INTERNAL);
        }
        if (request == null || request.cursor() == null || request.limit() == null) {
            throw new ShareQueryException(VALIDATION_FAILED);
        }
        int limit = request.limit().orElse(20);
        if (limit < 1 || limit > 100) {
            throw new ShareQueryException(VALIDATION_FAILED);
        }
        request.cursor().ifPresent(cursor -> {
            if (!caller.accountId().equals(cursor.accountId()) || !caller.organizationId().equals(cursor.organizationId())
                    || cursor.validFrom() == null || cursor.shareId() == null
                    || cursor.validFrom().getNano() % 1000 != 0
                    || cursor.shareId().codePointCount(0, cursor.shareId().length()) < 1
                    || cursor.shareId().codePointCount(0, cursor.shareId().length()) > 128
                    || cursor.shareId().replace(" ", "").isEmpty() || cursor.shareId().indexOf('\0') >= 0
                    || !java.nio.charset.StandardCharsets.UTF_8.newEncoder().canEncode(cursor.shareId())) {
                throw new ShareQueryException(CURSOR_INVALID);
            }
            try {
                OffsetDateTime anchor = cursor.validFrom().atOffset(ZoneOffset.UTC);
                if (anchor.getYear() < -4712 || anchor.getYear() > 294276) {
                    throw new ShareQueryException(CURSOR_INVALID);
                }
            } catch (java.time.DateTimeException invalid) {
                throw new ShareQueryException(CURSOR_INVALID);
            }
        });
        return limit;
    }

    private static Optional<String> sqlState(Throwable failure) {
        java.util.Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState() != null
                    && sql.getSQLState().matches("[0-9A-Z]{5}")) {
                return Optional.of(sql.getSQLState());
            }
        }
        return Optional.empty();
    }
}
