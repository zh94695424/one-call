package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CustomerApiErrorController implements ErrorController {
    private final CustomerProblemWriter problems;

    public CustomerApiErrorController(CustomerProblemWriter problems) { this.problems = problems; }

    @RequestMapping("${server.error.path:/error}")
    public void error(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getDispatcherType() != DispatcherType.ERROR) {
            problems.notFound(response, CustomerMeController.trace(request));
            return;
        }
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (Integer.valueOf(404).equals(value)) {
            problems.notFound(response, CustomerMeController.trace(request));
        } else {
            problems.write(response, Integer.valueOf(401).equals(value) ? CustomerContextException.Code.UNAUTHENTICATED
                    : Integer.valueOf(503).equals(value) ? CustomerContextException.Code.SERVICE_UNAVAILABLE
                    : CustomerContextException.Code.INTERNAL, CustomerMeController.trace(request));
        }
    }
}
