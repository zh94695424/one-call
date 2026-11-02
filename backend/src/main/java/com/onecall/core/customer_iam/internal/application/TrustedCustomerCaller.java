package com.onecall.core.customer_iam.internal.application;

import java.util.UUID;

public record TrustedCustomerCaller(UUID accountId, Channel channel) {
    public enum Channel { MINIAPP, WEB }
}
