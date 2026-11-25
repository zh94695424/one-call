package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException.Code;
import com.onecall.core.customer_iam.internal.application.CustomerOrderCommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CustomerOrdersController {
    private final ObjectProvider<CustomerOrderCommandService> commands;
    private final CustomerProblemWriter problems;
    private final CustomerOrderJsonBinder binder = new CustomerOrderJsonBinder();
    public CustomerOrdersController(@Qualifier("customerOrderCommandService") ObjectProvider<CustomerOrderCommandService> commands, CustomerProblemWriter problems) {
        this.commands = commands; this.problems = problems;
    }
    @PostMapping(path = "/api/v1/customer/orders", produces = "application/json")
    public Order createCustomerOrder(Principal principal, HttpServletRequest request, HttpServletResponse response) {
        if (!(principal instanceof CustomerRequestPrincipal customer)) throw new OrderCreationException(Code.UNAUTHENTICATED);
        CustomerOrderCommandService service;
        try { service = commands.getObject(); }
        catch (BeansException unavailable) { throw new OrderCreationException(Code.SERVICE_UNAVAILABLE); }
        service.requireContext(customer.caller());
        CustomerOrderDtos.OrderRequest body;
        try { body = binder.bind(request.getInputStream().readNBytes(8193)); }
        catch (IllegalArgumentException invalid) { throw new OrderCreationException(Code.VALIDATION_FAILED); }
        catch (IOException unavailable) { throw new OrderCreationException(Code.SERVICE_UNAVAILABLE); }
        var result = service.create(customer.caller(), new Command(body.subjectId(), body.catalogOfferId(), body.channel(), body.decision(),
                body.policyCode(), body.policyVersion(), body.promptVersion(), body.contentSha256(), body.expectedVersionToken(),
                CustomerBoundaryFilter.idempotencyKey(request), CustomerBoundaryFilter.trace(request)));
        response.setStatus(result.replay() ? 200 : 201);
        return result.order();
    }
    @ExceptionHandler(OrderCreationException.class)
    public void known(OrderCreationException failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, failure.code(), CustomerBoundaryFilter.trace(request)); }
    @ExceptionHandler(Exception.class)
    public void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, Code.INTERNAL, CustomerBoundaryFilter.trace(request)); }
}
