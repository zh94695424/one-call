package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerSubjectActivationCommandService;
import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.ActivationCommand;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code;
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
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CustomerSubjectActivationsController {
    private static final int MAX_BODY_BYTES = 8 * 1024;
    private final ObjectProvider<CustomerSubjectActivationCommandService> commands;
    private final CustomerProblemWriter problems;
    private final CustomerSubjectActivationJsonBinder binder = new CustomerSubjectActivationJsonBinder();
    public CustomerSubjectActivationsController(@Qualifier("customerSubjectActivationCommandService") ObjectProvider<CustomerSubjectActivationCommandService> commands, CustomerProblemWriter problems) {
        this.commands = commands;
        this.problems = problems;
    }
    @PostMapping(path = "/api/v1/customer/subjects/{subjectId}/activations", produces = "application/json")
    public CustomerSubjectActivationDtos.ActivationResult createCustomerSubjectActivation(@PathVariable String subjectId, Principal principal, HttpServletRequest request) {
        if (!(principal instanceof CustomerRequestPrincipal customer)) throw new SubjectActivationException(Code.UNAUTHENTICATED);
        CustomerSubjectActivationCommandService service;
        try { service = commands.getObject(); }
        catch (BeansException unavailable) { throw new SubjectActivationException(Code.SERVICE_UNAVAILABLE); }
        service.requireContext(customer.caller());
        UUID subject;
        CustomerSubjectActivationDtos.ActivationRequest body;
        try {
            if (!subjectId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) throw new IllegalArgumentException();
            subject = UUID.fromString(subjectId);
            byte[] bodyBytes = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
            if (bodyBytes.length > MAX_BODY_BYTES) throw new IllegalArgumentException();
            body = binder.bind(bodyBytes);
        } catch (IOException | IllegalArgumentException invalid) { throw new SubjectActivationException(Code.VALIDATION_FAILED); }
        var result = service.activate(customer.caller(), new ActivationCommand(subject, body.activationEventId(), body.decision(), body.policyCode(), body.policyVersion(), body.promptVersion(), body.contentSha256(), CustomerBoundaryFilter.trace(request)));
        return new CustomerSubjectActivationDtos.ActivationResult(result.activationStatus(), result.activationRevision(), result.transitionId(), result.consentId(), result.trialOutcome(),
                new CustomerSubjectActivationDtos.ActivationTrial(result.trial().grantedSeconds(), result.trial().expiresAt()));
    }
    @ExceptionHandler(SubjectActivationException.class)
    public void known(SubjectActivationException failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, failure.code(), CustomerBoundaryFilter.trace(request)); }
    @ExceptionHandler(Exception.class)
    public void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, Code.INTERNAL, CustomerBoundaryFilter.trace(request)); }
}
