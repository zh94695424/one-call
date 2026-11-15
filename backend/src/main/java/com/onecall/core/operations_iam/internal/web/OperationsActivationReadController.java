package com.onecall.core.operations_iam.internal.web;

import com.onecall.core.operations_iam.subjectread.OperationsActivationReadService;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadService.RawListRequest;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.cfg.DateTimeFeature;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OperationsActivationReadController {
    private final ObjectProvider<OperationsActivationReadService> services;
    private final OperationsProblemWriter problems;
    private final JsonMapper mapper = JsonMapper.builder().disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS).build();

    public OperationsActivationReadController(ObjectProvider<OperationsActivationReadService> services, OperationsProblemWriter problems) {
        this.services = services; this.problems = problems;
    }

    @GetMapping(path = "/api/v1/operations/activations")
    public void listOperationsActivations(Principal principal, HttpServletRequest request, HttpServletResponse response) throws IOException {
        var employee = employee(principal);
        var service = service();
        var result = service.list(employee.caller(), new RawListRequest(query(request), Collections.list(request.getHeaders("Accept"))));
        if (result == null) throw new Failure(Code.INTERNAL);
        success(result, response);
    }

    private OperationsActivationReadService service() {
        try {
            var candidates = services.stream().limit(2).toList();
            if (candidates.size() != 1) throw new Failure(Code.SERVICE_UNAVAILABLE);
            return candidates.getFirst();
        } catch (RuntimeException unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }

    private static OperationsRequestPrincipal employee(Principal principal) {
        if (!(principal instanceof OperationsRequestPrincipal employee)) throw new Failure(Code.UNAUTHENTICATED);
        return employee;
    }

    private static Map<String, List<String>> query(HttpServletRequest request) {
        var result = new LinkedHashMap<String, List<String>>();
        request.getParameterMap().forEach((key, values) -> result.put(key, values == null ? null : Arrays.asList(values.clone())));
        return result;
    }

    private void success(Object result, HttpServletResponse response) throws IOException {
        byte[] body = mapper.writeValueAsBytes(result);
        problems.headers(response);
        response.setStatus(200);
        response.setContentType("application/json");
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    @ExceptionHandler(Failure.class)
    public void known(Failure failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Code code = switch (failure.code()) {
            case CURSOR_INVALID, UNAUTHENTICATED, FORBIDDEN, VALIDATION_FAILED, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL -> failure.code();
            default -> Code.INTERNAL;
        };
        problems.write(response, code, OperationsBoundaryFilter.trace(request));
    }

    @ExceptionHandler(Exception.class)
    public void unexpected(Exception failure, HttpServletRequest request, HttpServletResponse response) throws IOException {
        problems.write(response, Code.INTERNAL, OperationsBoundaryFilter.trace(request));
    }
}
