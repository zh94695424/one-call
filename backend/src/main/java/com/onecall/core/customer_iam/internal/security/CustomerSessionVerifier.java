package com.onecall.core.customer_iam.internal.security;

/** Verifies opaque sessions and attests selected workspace from trusted server-side session state. */
public interface CustomerSessionVerifier {
    VerifiedCustomerSession verify(OpaqueCustomerBearer bearer, String audience);
}
