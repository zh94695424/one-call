package com.onecall.core.customer_iam.internal.application;

import java.util.UUID;

public record TrustedCustomerCaller(UUID accountId, Channel channel, SelectedCustomerWorkspace selectedWorkspace) {
    public TrustedCustomerCaller(UUID accountId, Channel channel) {
        this(accountId, channel, null);
    }

    public enum Channel { MINIAPP, WEB }
}
