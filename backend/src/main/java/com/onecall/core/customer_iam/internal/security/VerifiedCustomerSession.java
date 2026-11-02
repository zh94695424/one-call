package com.onecall.core.customer_iam.internal.security;

import com.onecall.core.customer_iam.internal.application.TrustedCustomerCaller.Channel;
import java.util.UUID;

public record VerifiedCustomerSession(UUID accountId, Channel channel, String audience) { }
