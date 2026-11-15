package com.onecall.core.operations_iam.internal.web;

import com.onecall.core.operations_iam.internal.security.EmployeeBearerAuthenticator;
import com.onecall.core.operations_iam.internal.security.EmployeeSessionVerifier;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OperationsWebConfiguration {
    @Bean
    OperationsProblemWriter operationsProblemWriter() { return new OperationsProblemWriter(); }
    @Bean
    FilterRegistrationBean<OperationsBoundaryFilter> operationsBoundaryFilterRegistration(ObjectProvider<EmployeeSessionVerifier> verifiers, OperationsProblemWriter problems) {
        var registration = new FilterRegistrationBean<>(new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(verifiers), problems));
        registration.addUrlPatterns("/api/v1/operations/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
    @Bean
    FilterRegistrationBean<OperationsBoundaryFilter> operationsErrorBoundaryFilterRegistration(ObjectProvider<EmployeeSessionVerifier> verifiers, OperationsProblemWriter problems, Environment environment) {
        String path = environment.getProperty("spring.web.error.path", "/error");
        if (!path.matches("/[A-Za-z0-9_./~-]*") || path.contains("//")
                || java.util.Arrays.stream(path.split("/")).anyMatch(segment -> ".".equals(segment) || "..".equals(segment))) {
            throw new IllegalStateException("Invalid error dispatch path");
        }
        var registration = new FilterRegistrationBean<>(new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(verifiers), problems));
        registration.addUrlPatterns(path);
        registration.setDispatcherTypes(DispatcherType.ERROR);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
