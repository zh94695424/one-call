package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.security.CustomerBearerAuthenticator;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier;
import jakarta.servlet.DispatcherType;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
@Profile("web")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CustomerWebConfiguration {
    @Bean
    @org.springframework.context.annotation.Lazy
    com.onecall.core.customer_iam.internal.application.CustomerSubjectClaimCommandService customerSubjectClaimCommandService(
            @org.springframework.beans.factory.annotation.Qualifier("customerSubjectClaimService")
            ObjectProvider<com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService> owner) {
        return new com.onecall.core.customer_iam.internal.application.CustomerSubjectClaimCommandService(owner);
    }

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> customerEncodedSolidusHandlingCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector ->
                connector.setEncodedSolidusHandling(EncodedSolidusHandling.PASS_THROUGH.getValue()));
    }

    @Bean
    CustomerProblemWriter customerProblemWriter() { return new CustomerProblemWriter(); }

    @Bean
    FilterRegistrationBean<CustomerBoundaryFilter> customerBoundaryFilterRegistration(
            ObjectProvider<CustomerSessionVerifier> verifiers, CustomerProblemWriter problems) {
        var registration = new FilterRegistrationBean<>(
                new CustomerBoundaryFilter(new CustomerBearerAuthenticator(verifiers), problems));
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}
