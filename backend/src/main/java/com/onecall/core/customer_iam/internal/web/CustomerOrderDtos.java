package com.onecall.core.customer_iam.internal.web;

import java.util.UUID;

public final class CustomerOrderDtos {
    private CustomerOrderDtos() {}
    public record OrderRequest(UUID subjectId, UUID catalogOfferId, String channel, String decision,
            String policyCode, String policyVersion, String promptVersion, String contentSha256, String expectedVersionToken) {}
}
