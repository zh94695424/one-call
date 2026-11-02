package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.application.CustomerContextService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CustomerMeController {
    private final ObjectProvider<CustomerContextService> service;
    private final CustomerProblemWriter problems;

    public CustomerMeController(@Qualifier("customerContextService") ObjectProvider<CustomerContextService> service,
            CustomerProblemWriter problems) {
        this.service = service;
        this.problems = problems;
    }

    @GetMapping(path = "/api/v1/customer/me", produces = "application/json")
    public CustomerContext getCustomerMe(Principal principal) {
        if (!(principal instanceof CustomerRequestPrincipal customer)) {
            throw new CustomerContextException(CustomerContextException.Code.UNAUTHENTICATED);
        }
        try { return service.getObject().getCustomerMe(customer.caller()); }
        catch (org.springframework.beans.BeansException failure) {
            throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
        }
    }

    @ExceptionHandler(CustomerContextException.class)
    void known(CustomerContextException failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, failure.code(), trace(request));
    }

    @ExceptionHandler(Exception.class)
    void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, CustomerContextException.Code.INTERNAL, trace(request));
    }

    static UUID trace(HttpServletRequest request) {
        Object value = request.getAttribute(CustomerBoundaryFilter.TRACE_ATTRIBUTE);
        return value instanceof UUID traceId ? traceId : UUID.randomUUID();
    }
}
