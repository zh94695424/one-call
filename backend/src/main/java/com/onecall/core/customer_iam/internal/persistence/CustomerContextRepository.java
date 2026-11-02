package com.onecall.core.customer_iam.internal.persistence;

import com.onecall.core.customer_iam.internal.application.CustomerContext;
import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller;
import java.util.Optional;

public interface CustomerContextRepository {
    Optional<CustomerContext> findActiveContext(TrustedCustomerCaller caller);
}
