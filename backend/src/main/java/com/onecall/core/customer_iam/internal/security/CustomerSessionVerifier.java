package com.onecall.core.customer_iam.internal.security;

public interface CustomerSessionVerifier {
    VerifiedCustomerSession verify(OpaqueCustomerBearer bearer, String audience);
}
