package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException.Code;
import com.onecall.core.customer_iam.internal.application.CustomerOrderCommandService;
import com.onecall.core.customer_iam.internal.application.CustomerOrderQueryService;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import java.util.Optional;
import java.util.UUID;
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
    private final ObjectProvider<CustomerOrderQueryService> queries;
    private final ObjectProvider<CustomerOrderCursorCodec> cursors;
    private final CustomerProblemWriter problems;
    private final CustomerOrderJsonBinder binder = new CustomerOrderJsonBinder();
    public CustomerOrdersController(@Qualifier("customerOrderCommandService") ObjectProvider<CustomerOrderCommandService> commands,
            @Qualifier("customerOrderQueryService") ObjectProvider<CustomerOrderQueryService> queries,
            @Qualifier("customerOrderCursorCodec") ObjectProvider<CustomerOrderCursorCodec> cursors, CustomerProblemWriter problems) {
        this.commands = commands; this.queries = queries; this.cursors = cursors; this.problems = problems;
    }
    @GetMapping(path = "/api/v1/customer/orders", produces = "application/json")
    public CustomerOrderDtos.Page listCustomerOrders(Principal principal, HttpServletRequest request) {
        if (!(principal instanceof CustomerRequestPrincipal customer))
            throw new CustomerContextException(CustomerContextException.Code.UNAUTHENTICATED);
        var scope = CustomerOrderQueryService.requireScope(customer.caller());
        Optional<Integer> limit = parseLimit(request.getParameterValues("limit"));
        String[] values = request.getParameterValues("cursor");
        if (values != null) {
            if (values.length != 1) throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
            CustomerOrderCursorCodec.validateStructure(values[0]);
        }
        try {
            var codec = cursors.getObject();
            var anchor = values == null ? Optional.<com.onecall.core.catalog_order.customerquery.CustomerOrderReader.Anchor>empty()
                    : Optional.of(codec.decode(scope, values[0]));
            var page = queries.getObject().listCustomerOrders(customer.caller(), new CustomerOrderQueryService.Request(anchor, limit));
            String nextCursor = page.nextAnchor().map(value -> codec.encode(scope, value)).orElse(null);
            return new CustomerOrderDtos.Page(page.items().stream().map(CustomerOrderDtos.Summary::from).toList(), nextCursor, page.hasMore());
        } catch (BeansException unavailable) { throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE); }
    }
    @GetMapping(path = "/api/v1/customer/orders/{orderId}", produces = "application/json")
    public CustomerOrderDtos.Detail getCustomerOrder(@PathVariable("orderId") String orderId, Principal principal,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!(principal instanceof CustomerRequestPrincipal customer))
            throw new CustomerContextException(CustomerContextException.Code.UNAUTHENTICATED);
        CustomerOrderQueryService.requireScope(customer.caller());
        UUID identity;
        try {
            identity = UUID.fromString(orderId);
            if (!identity.toString().equals(orderId)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException invalid) { throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED); }
        try {
            var detail = queries.getObject().getCustomerOrder(customer.caller(), identity);
            if (detail.isPresent()) return CustomerOrderDtos.Detail.from(detail.get());
            problems.notFound(response, CustomerBoundaryFilter.trace(request));
            return null;
        } catch (BeansException unavailable) { throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE); }
    }
    private static Optional<Integer> parseLimit(String[] values) {
        if (values == null) return Optional.empty();
        if (values.length != 1 || values[0] == null || !values[0].matches("[0-9]+"))
            throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED);
        try {
            int value = Integer.parseInt(values[0]);
            if (value < 1 || value > 100) throw new NumberFormatException();
            return Optional.of(value);
        } catch (NumberFormatException invalid) { throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED); }
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
    @ExceptionHandler(CustomerContextException.class)
    public void knownContext(CustomerContextException failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, failure.code(), CustomerBoundaryFilter.trace(request));
    }
    @ExceptionHandler(Exception.class)
    public void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, Code.INTERNAL, CustomerBoundaryFilter.trace(request)); }
}
