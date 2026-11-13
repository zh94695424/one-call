package com.onecall.core.operations_iam.internal.web;

import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OperationsSubjectsController {
    private final ObjectProvider<OperationsSubjectLifecycleService> services;
    private final OperationsProblemWriter problems;
    private final OperationsSubjectLifecycleJsonBinder binder = new OperationsSubjectLifecycleJsonBinder();
    public OperationsSubjectsController(@Qualifier("operationsSubjectLifecycleService") ObjectProvider<OperationsSubjectLifecycleService> services, OperationsProblemWriter problems) {
        this.services = services; this.problems = problems;
    }
    @PostMapping(path = "/api/v1/operations/subjects/{subjectId}/suspensions", produces = "application/json")
    public ActivationTransition suspendOperationsSubject(@PathVariable String subjectId, Principal principal, HttpServletRequest request) {
        return execute(subjectId, principal, request, true);
    }
    @PostMapping(path = "/api/v1/operations/subjects/{subjectId}/resumptions", produces = "application/json")
    public ActivationTransition resumeOperationsSubject(@PathVariable String subjectId, Principal principal, HttpServletRequest request) {
        return execute(subjectId, principal, request, false);
    }
    private ActivationTransition execute(String subjectId, Principal principal, HttpServletRequest request, boolean suspend) {
        if (!(principal instanceof OperationsRequestPrincipal employee)) throw new Failure(Code.UNAUTHENTICATED);
        Command command;
        try {
            UUID subject = UUID.fromString(subjectId);
            if (!subject.toString().equals(subjectId)) throw new IllegalArgumentException();
            var body = binder.bind(request.getInputStream().readNBytes(8193));
            command = new Command(subject, OperationsBoundaryFilter.idempotencyKey(request), body.reasonCode(), body.expectedActivationRevision());
        } catch (IOException | IllegalArgumentException invalid) { throw new Failure(Code.VALIDATION_FAILED); }
        OperationsSubjectLifecycleService service;
        try {
            var candidates = services.stream().limit(2).toList();
            if (candidates.size() != 1) throw new Failure(Code.SERVICE_UNAVAILABLE);
            service = candidates.getFirst();
        } catch (RuntimeException unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
        return suspend ? service.suspend(command, employee.caller()) : service.resume(command, employee.caller());
    }
    @ExceptionHandler(Failure.class)
    public void known(Failure failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, failure.code(), OperationsBoundaryFilter.trace(request)); }
    @ExceptionHandler(Exception.class)
    public void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException { problems.write(response, Code.INTERNAL, OperationsBoundaryFilter.trace(request)); }
}
