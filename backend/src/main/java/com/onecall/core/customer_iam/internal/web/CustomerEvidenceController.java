package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerBusinessNumberQueryService;
import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.application.CustomerEvidenceQueryService;
import com.onecall.core.evidence.customerevidencequery.CustomerEvidenceReader.Anchor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;
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
public class CustomerEvidenceController {
    private final ObjectProvider<CustomerEvidenceQueryService> service;
    private final ObjectProvider<CustomerEvidenceCursorCodec> cursors;
    private final CustomerProblemWriter problems;

    public CustomerEvidenceController(
            @Qualifier("customerEvidenceQueryService") ObjectProvider<CustomerEvidenceQueryService> service,
            @Qualifier("customerEvidenceCursorCodec") ObjectProvider<CustomerEvidenceCursorCodec> cursors,
            CustomerProblemWriter problems) {
        this.service = service;
        this.cursors = cursors;
        this.problems = problems;
    }

    @GetMapping(path = "/api/v1/customer/evidence", produces = "application/json")
    public CustomerEvidenceDtos.EvidencePage listCustomerEvidence(Principal principal, HttpServletRequest request) {
        if (!(principal instanceof CustomerRequestPrincipal customer))
            throw new CustomerContextException(CustomerContextException.Code.UNAUTHENTICATED);
        var caller = customer.caller();
        var scope = CustomerBusinessNumberQueryService.requireScope(caller);
        Optional<Integer> limit = parseLimit(request.getParameterValues("limit"));
        String[] values = request.getParameterValues("cursor");
        if (values != null) {
            if (values.length != 1) throw new CustomerContextException(CustomerContextException.Code.CURSOR_INVALID);
            CustomerEvidenceCursorCodec.validateStructure(values[0]);
        }
        try {
            var codec = cursors.getObject();
            Optional<Anchor> anchor = values == null ? Optional.empty() : Optional.of(codec.decode(scope, values[0]));
            var page = service.getObject().listCustomerEvidence(caller, new CustomerEvidenceQueryService.Request(anchor, limit));
            if (page == null) throw new CustomerContextException(CustomerContextException.Code.INTERNAL);
            return CustomerEvidenceDtos.EvidencePage.from(page, page.nextAnchor().map(value -> codec.encode(scope, value)).orElse(null));
        } catch (BeansException failure) {
            throw new CustomerContextException(CustomerContextException.Code.SERVICE_UNAVAILABLE);
        }
    }

    @GetMapping(path = "/api/v1/customer/evidence/{evidenceId}", produces = "application/json")
    public CustomerEvidenceDtos.EvidenceDetail getCustomerEvidence(Principal principal,
            @PathVariable("evidenceId") String evidenceId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!(principal instanceof CustomerRequestPrincipal customer))
            throw new CustomerContextException(CustomerContextException.Code.UNAUTHENTICATED);
        var caller = customer.caller();
        CustomerBusinessNumberQueryService.requireScope(caller);
        if (evidenceId == null || !evidenceId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw new CustomerContextException(CustomerContextException.Code.VALIDATION_FAILED);
        try {
            var result = service.getObject().getCustomerEvidence(caller, UUID.fromString(evidenceId));
            if (result == null) throw new CustomerContextException(CustomerContextException.Code.INTERNAL);
            if (result.isPresent()) return CustomerEvidenceDtos.EvidenceDetail.from(result.get());
            problems.notFound(response, CustomerBoundaryFilter.trace(request));
            return null;
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

    @ExceptionHandler(CustomerContextException.class)
    public void known(CustomerContextException failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, failure.code(), CustomerBoundaryFilter.trace(request));
    }

    @ExceptionHandler(Exception.class)
    public void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, CustomerContextException.Code.INTERNAL, CustomerBoundaryFilter.trace(request));
    }
}
