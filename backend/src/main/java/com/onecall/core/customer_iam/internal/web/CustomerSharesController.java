package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import com.onecall.core.sharing.internal.application.CustomerShareQueryService;
import com.onecall.core.sharing.internal.application.ShareCursor;
import com.onecall.core.sharing.internal.application.SharePage;
import com.onecall.core.sharing.internal.application.SharePageRequest;
import com.onecall.core.sharing.internal.application.ShareQueryException;
import com.onecall.core.sharing.internal.application.TrustedShareCaller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CustomerSharesController {
    private final ObjectProvider<CustomerShareQueryService> service;
    private final CustomerProblemWriter problems;
    private final CustomerShareCursorCodec cursors = new CustomerShareCursorCodec();

    public CustomerSharesController(@Qualifier("customerShareQueryService") ObjectProvider<CustomerShareQueryService> service,
            CustomerProblemWriter problems) {
        this.service = service;
        this.problems = problems;
    }

    @GetMapping(path = "/api/v1/customer/shares", produces = "application/json")
    public CustomerSharePage getCustomerShares(Principal principal, HttpServletRequest request) {
        if (!(principal instanceof CustomerRequestPrincipal customer))
            throw new CustomerContextException(CustomerContextException.Code.UNAUTHENTICATED);
        TrustedCustomerCaller caller = customer.caller();
        SelectedCustomerWorkspace selected = requireSelectedWorkspace(caller);
        Optional<Integer> limit = parseLimit(request.getParameterValues("limit"));
        Optional<ShareCursor> cursor = parseCursor(caller, selected, request.getParameterValues("cursor"));
        TrustedShareCaller shareCaller = new TrustedShareCaller(caller.accountId(),
                Optional.ofNullable(selected.organizationId()));
        SharePageRequest pageRequest = new SharePageRequest(cursor, limit);
        try {
            SharePage page = service.getObject().listCustomerShares(shareCaller, pageRequest);
            String next = page.nextCursor().map(value -> cursors.encode(shareCaller, value)).orElse(null);
            return CustomerSharePage.from(page, next);
        } catch (ShareQueryException failure) {
            throw switch (failure.code()) {
                case FORBIDDEN -> new CustomerContextException(CustomerContextException.Code.FORBIDDEN);
                case VALIDATION_FAILED -> new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED);
                case CURSOR_INVALID -> new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
                case SERVICE_UNAVAILABLE -> new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
                case INTERNAL -> new CustomerContextException(CustomerContextException.Code.INTERNAL);
            };
        } catch (BeansException failure) {
            throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
        }
    }

    private static SelectedCustomerWorkspace requireSelectedWorkspace(TrustedCustomerCaller caller) {
        if (caller == null || caller.selectedWorkspace() == null)
            throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
        SelectedCustomerWorkspace selected = caller.selectedWorkspace();
        if (selected.workspaceType() == WorkspaceType.PERSONAL && selected.organizationId() == null) return selected;
        if (selected.workspaceType() == WorkspaceType.ORGANIZATION && selected.organizationId() != null) return selected;
        throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
    }

    private static Optional<Integer> parseLimit(String[] values) {
        if (values == null) return Optional.empty();
        if (values.length != 1 || values[0] == null || !values[0].matches("[0-9]+"))
            throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED);
        try {
            int value = Integer.parseInt(values[0]);
            if (value < 1 || value > 100) throw new NumberFormatException();
            return Optional.of(value);
        } catch (NumberFormatException failure) {
            throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED);
        }
    }

    private Optional<ShareCursor> parseCursor(TrustedCustomerCaller caller, SelectedCustomerWorkspace selected, String[] values) {
        if (values == null) return Optional.empty();
        if (values.length != 1) throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
        TrustedShareCaller shareCaller = new TrustedShareCaller(caller.accountId(), Optional.ofNullable(selected.organizationId()));
        try { return Optional.of(cursors.decode(shareCaller, values[0])); }
        catch (IllegalArgumentException failure) {
            throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
        }
    }

    @ExceptionHandler(CustomerContextException.class)
    void known(CustomerContextException failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, failure.code(), CustomerBoundaryFilter.trace(request));
    }

    @ExceptionHandler(Exception.class)
    void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, CustomerContextException.Code.INTERNAL, CustomerBoundaryFilter.trace(request));
    }
}
