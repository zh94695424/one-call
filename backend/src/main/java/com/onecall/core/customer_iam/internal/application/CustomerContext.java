package com.onecall.core.customer_iam.internal.application;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.UUID;

public record CustomerContext(AccountSummary account, List<WorkspaceSummary> workspaces,
        TrustedCustomerCaller.Channel channel) {
    public CustomerContext {
        workspaces = List.copyOf(workspaces);
    }

    public record AccountSummary(UUID accountId, String displayName, AccountStatus status) { }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record WorkspaceSummary(WorkspaceType workspaceType, UUID organizationId,
            String organizationDisplayName, OrganizationRole role, WorkspaceStatus status) { }

    public enum AccountStatus { ACTIVE, DISABLED }
    public enum WorkspaceType { PERSONAL, ORGANIZATION }
    public enum OrganizationRole { ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER, AUDITOR }
    public enum WorkspaceStatus { ACTIVE }
}
