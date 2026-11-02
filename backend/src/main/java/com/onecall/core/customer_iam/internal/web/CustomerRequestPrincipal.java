package com.onecall.core.customer_iam.internal.web;

import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import java.security.Principal;

public record CustomerRequestPrincipal(TrustedCustomerCaller caller) implements Principal {
    @Override
    public String getName() { return caller.accountId().toString(); }
}
