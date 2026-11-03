package com.onecall.core.customer_iam.internal.security;

import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel;
import com.onecall.core.customer_iam.internal.application.SelectedCustomerWorkspace;
import java.util.UUID;

public record VerifiedCustomerSession(UUID accountId, Channel channel, String audience,
        SelectedCustomerWorkspace selectedWorkspace) {
    public VerifiedCustomerSession(UUID accountId, Channel channel, String audience) {
        this(accountId, channel, audience, null);
    }
}
