package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.application.CustomerContextService;
import com.onecall.core.sharing.internal.publicquery.OpaqueCapabilityToken;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryException;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryService;
import com.onecall.core.sharing.internal.publicquery.PublicShareView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PublicSharesController {
    private final ObjectProvider<PublicShareQueryService> service;
    private final ObjectProvider<CustomerContextService> customerContexts;
    private final CustomerProblemWriter problems;

    public PublicSharesController(@Qualifier("publicShareQueryService") ObjectProvider<PublicShareQueryService> service,
            @Qualifier("customerContextService") ObjectProvider<CustomerContextService> customerContexts, CustomerProblemWriter problems) {
        this.service = service;
        this.customerContexts = customerContexts;
        this.problems = problems;
    }

    @GetMapping(path = "/api/v1/public/shares/{capabilityToken}", produces = "application/json")
    public PublicShareView getPublicShare(@PathVariable("capabilityToken") String capabilityToken, Principal principal) {
        var token = new OpaqueCapabilityToken(capabilityToken);
        final PublicShareQueryService query;
        try { query = java.util.Objects.requireNonNull(service.getObject()); }
        catch (RuntimeException failure) { throw new PublicShareQueryException(PublicShareQueryException.Code.SERVICE_UNAVAILABLE); }
        return query.getPublicShare(token, organization -> member(principal, organization));
    }

    private PublicShareQueryService.MemberMatch member(Principal principal, UUID organization) {
        if (!(principal instanceof CustomerRequestPrincipal customer)) return PublicShareQueryService.MemberMatch.NONMATCH;
        final CustomerContextService contexts;
        try { contexts = java.util.Objects.requireNonNull(customerContexts.getObject()); }
        catch (RuntimeException failure) { return PublicShareQueryService.MemberMatch.UNAVAILABLE; }
        try {
            var current = contexts.getCustomerMe(customer.caller());
            return current.workspaces().stream().anyMatch(workspace -> workspace.workspaceType() == CustomerContext.WorkspaceType.ORGANIZATION
                    && organization.equals(workspace.organizationId()) && workspace.status() == CustomerContext.WorkspaceStatus.ACTIVE)
                    ? PublicShareQueryService.MemberMatch.MATCH : PublicShareQueryService.MemberMatch.NONMATCH;
        } catch (CustomerContextException known) {
            return switch (known.code()) {
                case UNAUTHENTICATED -> PublicShareQueryService.MemberMatch.UNAUTHENTICATED;
                case SERVICE_UNAVAILABLE -> PublicShareQueryService.MemberMatch.UNAVAILABLE;
                default -> PublicShareQueryService.MemberMatch.INTERNAL;
            };
        } catch (RuntimeException failure) { return PublicShareQueryService.MemberMatch.INTERNAL; }
    }

    @ExceptionHandler(PublicShareQueryException.class)
    void known(PublicShareQueryException failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, failure.code(), CustomerBoundaryFilter.trace(request));
    }

    @ExceptionHandler(Exception.class)
    void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, PublicShareQueryException.Code.INTERNAL, CustomerBoundaryFilter.trace(request));
    }
}
