package com.onecall.core.customer_iam.internal.security;

import static com.onecall.core.customer_iam.internal.application.CustomerContextException.Code.*;

import com.onecall.core.customer_iam.internal.application.CustomerContextException;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;

public final class CustomerBearerAuthenticator {
    private static final Pattern BEARER = Pattern.compile("(?i:Bearer) +([A-Za-z0-9._~+/-]+=*)");
    private final ObjectProvider<CustomerSessionVerifier> verifiers;

    public CustomerBearerAuthenticator(ObjectProvider<CustomerSessionVerifier> verifiers) {
        this.verifiers = verifiers;
    }

    public TrustedCustomerCaller authenticate(Enumeration<String> headers) {
        if (headers == null || !headers.hasMoreElements()) { throw new CustomerContextException(UNAUTHENTICATED); }
        String header = headers.nextElement();
        if (header == null || headers.hasMoreElements()) { throw new CustomerContextException(UNAUTHENTICATED); }
        Matcher match = BEARER.matcher(header);
        if (!match.matches()) { throw new CustomerContextException(UNAUTHENTICATED); }
        final java.util.List<CustomerSessionVerifier> candidates;
        try {
            candidates = verifiers.stream().limit(2).toList();
        } catch (RuntimeException failure) {
            throw new CustomerContextException(SERVICE_UNAVAILABLE);
        }
        if (candidates.size() != 1) { throw new CustomerContextException(SERVICE_UNAVAILABLE); }
        VerifiedCustomerSession session;
        try {
            session = candidates.getFirst().verify(new OpaqueCustomerBearer(match.group(1)),
                    "onecall:customer-session:v1");
        } catch (CustomerSessionVerificationException known) {
            throw new CustomerContextException(known.code() == CustomerSessionVerificationException.Code.UNAUTHENTICATED
                    ? UNAUTHENTICATED : SERVICE_UNAVAILABLE);
        } catch (RuntimeException failure) {
            throw new CustomerContextException(SERVICE_UNAVAILABLE);
        }
        if (session == null || session.accountId() == null || session.channel() == null || session.audience() == null) {
            throw new CustomerContextException(SERVICE_UNAVAILABLE);
        }
        if (!"onecall:customer-session:v1".equals(session.audience())) {
            throw new CustomerContextException(UNAUTHENTICATED);
        }
        return new TrustedCustomerCaller(session.accountId(), session.channel());
    }
}
