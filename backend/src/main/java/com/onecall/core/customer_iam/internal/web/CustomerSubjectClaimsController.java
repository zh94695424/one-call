package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerSubjectClaimCommandService;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.ClaimCommand;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException.Code;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CustomerSubjectClaimsController {
    private final ObjectProvider<CustomerSubjectClaimCommandService> commands;
    private final CustomerProblemWriter problems;
    private final CustomerSubjectClaimJsonBinder binder = new CustomerSubjectClaimJsonBinder();
    public CustomerSubjectClaimsController(@Qualifier("customerSubjectClaimCommandService") ObjectProvider<CustomerSubjectClaimCommandService> commands,
            CustomerProblemWriter problems) {
        this.commands = commands;
        this.problems = problems;
    }
    @PostMapping(path = "/api/v1/customer/subject-claims", produces = "application/json")
    public CustomerSubjectClaimDtos.ClaimResult createCustomerSubjectClaim(Principal principal, HttpServletRequest request) {
        if (!(principal instanceof CustomerRequestPrincipal customer)) throw new SubjectClaimException(Code.UNAUTHENTICATED);
        CustomerSubjectClaimCommandService service;
        try { service = commands.getObject(); }
        catch (BeansException unavailable) { throw new SubjectClaimException(Code.SERVICE_UNAVAILABLE); }
        service.requirePersonal(customer.caller());
        CustomerSubjectClaimDtos.ClaimRequest body;
        try { body = binder.bind(request.getInputStream().readAllBytes()); }
        catch (IOException | IllegalArgumentException invalid) { throw new SubjectClaimException(Code.VALIDATION_FAILED); }
        var result = service.create(customer.caller(), new ClaimCommand(body.subjectId(), body.bindingId(), body.verificationMethod(),
                body.verificationProofId(), CustomerBoundaryFilter.idempotencyKey(request), CustomerBoundaryFilter.trace(request)));
        return new CustomerSubjectClaimDtos.ClaimResult(result.claimId(), result.status(), result.subjectId(), result.verifiedAt());
    }
    @ExceptionHandler(SubjectClaimException.class)
    public void known(SubjectClaimException failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, failure.code(), CustomerBoundaryFilter.trace(request));
    }
    @ExceptionHandler(Exception.class)
    public void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, Code.INTERNAL, CustomerBoundaryFilter.trace(request));
    }
}
