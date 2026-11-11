package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryService;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.application.CustomerSubjectDetailQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.UUID;
import org.springframework.beans.BeansException;
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
public class CustomerSubjectsController {
    private final ObjectProvider<CustomerSubjectDetailQueryService> service;
    private final CustomerProblemWriter problems;

    public CustomerSubjectsController(
            @Qualifier("customerSubjectDetailQueryService") ObjectProvider<CustomerSubjectDetailQueryService> service,
            CustomerProblemWriter problems) {
        this.service = service;
        this.problems = problems;
    }

    @GetMapping(path = "/api/v1/customer/subjects/{subjectId}", produces = "application/json")
    public CustomerSubjectDetailQueryService.Detail getCustomerSubject(Principal principal,
            @PathVariable("subjectId") String subjectId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!(principal instanceof CustomerRequestPrincipal customer))
            throw new CustomerContextException(CustomerContextException.Code.UNAUTHENTICATED);
        var caller = customer.caller();
        CustomerBusinessNumberQueryService.requireScope(caller);
        if (subjectId == null || !subjectId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED);
        try {
            var result = service.getObject().getCustomerSubject(caller, UUID.fromString(subjectId));
            if (result == null) throw new CustomerContextException(CustomerContextException.Code.INTERNAL);
            if (result.isPresent()) return result.get();
            problems.notFound(response, CustomerBoundaryFilter.trace(request));
            return null;
        } catch (BeansException failure) {
            throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
        }
    }

    @ExceptionHandler(CustomerContextException.class)
    public void known(CustomerContextException failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, failure.code(), CustomerBoundaryFilter.trace(request));
    }

    @ExceptionHandler(Exception.class)
    public void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, CustomerContextException.Code.INTERNAL, CustomerBoundaryFilter.trace(request));
    }
}
