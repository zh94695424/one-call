package com.onecall.core.customer_iam.internal.application;

import java.util.UUID;

public record SelectedCustomerWorkspace(CustomerContext.WorkspaceType workspaceType, UUID organizationId) { }
