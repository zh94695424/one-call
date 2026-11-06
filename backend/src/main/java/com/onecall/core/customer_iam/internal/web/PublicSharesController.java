package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.application.CustomerContextService;
import com.onecall.core.sharing.internal.publicquery.OpaqueCapabilityToken;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryException;
import com.onecall.core.sharing.internal.publicquery.PublicShareQueryService;
import com.onecall.core.sharing.internal.publicquery.PublicShareView;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionCapabilityToken;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionJsonBinder;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionRequest;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionService;
import com.onecall.core.sharing.internal.publicadmission.ShareAdmissionView;
import com.onecall.core.sharing.internal.publicadmission.TrustedShareAdmissionCaller;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PublicSharesController {
    private final ObjectProvider<PublicShareQueryService> service;
    private final ObjectProvider<CustomerContextService> customerContexts;
    private final CustomerProblemWriter problems;
    private final ObjectProvider<ShareAdmissionService> admissions;
    private final ShareAdmissionJsonBinder admissionBinder = new ShareAdmissionJsonBinder();

    public PublicSharesController(@Qualifier("publicShareQueryService") ObjectProvider<PublicShareQueryService> service,
            @Qualifier("customerContextService") ObjectProvider<CustomerContextService> customerContexts, CustomerProblemWriter problems,
            @Qualifier("shareAdmissionService") ObjectProvider<ShareAdmissionService> admissions) {
        this.service = service;
        this.customerContexts = customerContexts;
        this.problems = problems;
        this.admissions = admissions;
    }

    @PostMapping(path = "/api/v1/public/shares/{capabilityToken}/admissions", produces = "application/json")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ShareAdmissionView admit(@PathVariable("capabilityToken") String capabilityToken, Principal principal,
            HttpServletRequest servletRequest) throws IOException {
        ShareAdmissionRequest request = admissionBinder.bind(servletRequest.getInputStream().readAllBytes());
        var token = new ShareAdmissionCapabilityToken(capabilityToken);
        java.util.Optional<TrustedShareAdmissionCaller> caller = java.util.Optional.empty();
        if (request.verification() instanceof ShareAdmissionRequest.Verification.OrganizationMember) {
            if (!(principal instanceof CustomerRequestPrincipal customer)) throw new ShareAdmissionException(ShareAdmissionException.Code.UNAUTHENTICATED);
            try {
                var current = customerContexts.getObject().getCustomerMe(customer.caller());
                var organizations = current.workspaces().stream().filter(workspace -> workspace.workspaceType() == CustomerContext.WorkspaceType.ORGANIZATION
                        && workspace.status() == CustomerContext.WorkspaceStatus.ACTIVE).toList();
                if (!current.account().accountId().equals(customer.caller().accountId())) throw new ShareAdmissionException(ShareAdmissionException.Code.SHARE_WRONG_TENANT);
                if (current.account().status() != CustomerContext.AccountStatus.ACTIVE) throw new ShareAdmissionException(ShareAdmissionException.Code.FORBIDDEN);
                var selected = customer.caller().selectedWorkspace();
                UUID organization;
                if (selected != null) {
                    if (selected.workspaceType() != CustomerContext.WorkspaceType.ORGANIZATION || selected.organizationId() == null
                            || organizations.stream().noneMatch(workspace -> selected.organizationId().equals(workspace.organizationId())))
                        throw new ShareAdmissionException(ShareAdmissionException.Code.SHARE_WRONG_TENANT);
                    organization = selected.organizationId();
                } else {
                    if (organizations.size() != 1) throw new ShareAdmissionException(ShareAdmissionException.Code.FORBIDDEN);
                    organization = organizations.getFirst().organizationId();
                }
                caller = java.util.Optional.of(new TrustedShareAdmissionCaller(current.account().accountId(), organization));
            } catch (ShareAdmissionException known) { throw known; }
            catch (CustomerContextException known) {
                throw new ShareAdmissionException(known.code() == CustomerContextException.Code.UNAUTHENTICATED
                        ? ShareAdmissionException.Code.UNAUTHENTICATED : ShareAdmissionException.Code.SERVICE_UNAVAILABLE);
            } catch (RuntimeException unavailable) { throw new ShareAdmissionException(ShareAdmissionException.Code.SERVICE_UNAVAILABLE); }
        }
        final ShareAdmissionService service;
        try { service = java.util.Objects.requireNonNull(admissions.getObject()); }
        catch (RuntimeException unavailable) { throw new ShareAdmissionException(ShareAdmissionException.Code.SERVICE_UNAVAILABLE); }
        var outcome = service.admit(token, request, caller);
        if (outcome instanceof ShareAdmissionService.AdmissionCommitted committed) return committed.view();
        if (outcome instanceof ShareAdmissionService.ReplayCommitted committed) return committed.view();
        throw new ShareAdmissionException(((ShareAdmissionService.DeniedCommitted) outcome).code());
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

    @ExceptionHandler(ShareAdmissionException.class)
    void admissionFailure(ShareAdmissionException failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, failure.code(), CustomerBoundaryFilter.trace(request));
    }

    @ExceptionHandler(Exception.class)
    void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, PublicShareQueryException.Code.INTERNAL, CustomerBoundaryFilter.trace(request));
    }
}
