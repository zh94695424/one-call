package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.catalog_order.customercatalogquery.CustomerCatalogOfferReader;
import com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryService;
import com.onecall.core.customer_iam.internal.application.CustomerCatalogOfferQueryService;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;
import java.util.Set;
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
public class CustomerCatalogOffersController {
    private final ObjectProvider<CustomerCatalogOfferQueryService> service;
    private final ObjectProvider<CustomerCatalogOfferCursorCodec> cursors;
    private final CustomerProblemWriter problems;

    public CustomerCatalogOffersController(
            @Qualifier("customerCatalogOfferQueryService") ObjectProvider<CustomerCatalogOfferQueryService> service,
            @Qualifier("customerCatalogOfferCursorCodec") ObjectProvider<CustomerCatalogOfferCursorCodec> cursors,
            CustomerProblemWriter problems) {
        this.service = service;
        this.cursors = cursors;
        this.problems = problems;
    }

    @GetMapping(path = "/api/v1/customer/catalog/offers", produces = "application/json")
    public CustomerCatalogOfferPage listCustomerCatalogOffers(Principal principal, HttpServletRequest request) {
        if (!(principal instanceof CustomerRequestPrincipal customer))
            throw new CustomerContextException(CustomerContextException.Code.UNAUTHENTICATED);
        var caller = customer.caller();
        var scope = CustomerBusinessNumberQueryService.requireScope(caller);
        if (caller.channel() == null) throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
        Optional<Integer> limit = parseLimit(request.getParameterValues("limit"));
        Optional<String> channel = parseEnum(request.getParameterValues("channel"), Set.of("MINIAPP", "WEB"));
        Optional<String> audience = parseEnum(request.getParameterValues("audience"), Set.of("PERSONAL", "ENTERPRISE"));
        Optional<String> offerType = parseEnum(request.getParameterValues("offerType"), Set.of("SUBSCRIPTION", "ONE_TIME", "ADD_ON", "QUOTE"));
        String trustedChannel = caller.channel().name();
        String trustedAudience = caller.selectedWorkspace().workspaceType()
                == com.onecall.core.customer_iam.internal.application.CustomerContext.WorkspaceType.PERSONAL ? "PERSONAL" : "ENTERPRISE";
        if (channel.isPresent() && !channel.get().equals(trustedChannel)
                || audience.isPresent() && !audience.get().equals(trustedAudience))
            throw new CustomerContextException(CustomerContextException.Code.FORBIDDEN);
        String[] values = request.getParameterValues("cursor");
        if (values != null) {
            if (values.length != 1) throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
            CustomerCatalogOfferCursorCodec.validateStructure(values[0]);
        }
        try {
            var codec = cursors.getObject();
            var binding = new CustomerCatalogOfferCursorCodec.Binding(caller.accountId(), scope.organizationId(),
                    trustedChannel, trustedAudience, offerType);
            Optional<CustomerCatalogOfferReader.Anchor> anchor = Optional.empty();
            if (values != null) {
                var decoded = codec.decode(binding, values[0]);
                anchor = Optional.of(new CustomerCatalogOfferReader.Anchor(decoded.productCode(), decoded.versionNo(), decoded.catalogOfferId()));
            }
            var page = service.getObject().listCustomerCatalogOffers(caller,
                    new CustomerCatalogOfferQueryService.Request(anchor, limit, channel, audience, offerType));
            String nextCursor = page.nextAnchor().map(value -> codec.encode(binding,
                    new CustomerCatalogOfferCursorCodec.Anchor(value.productCode(), value.versionNo(), value.catalogOfferId()))).orElse(null);
            return CustomerCatalogOfferPage.from(page, nextCursor);
        } catch (BeansException failure) {
            throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
        }
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

    private static Optional<String> parseEnum(String[] values, Set<String> allowed) {
        if (values == null) return Optional.empty();
        if (values.length != 1 || values[0] == null || !allowed.contains(values[0]))
            throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED);
        return Optional.of(values[0]);
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
