package com.onecall.core.customer_iam.internal.application;

import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService;
import com.onecall.core.catalog_order.customerorder.CustomerOrderCreationService.*;
import com.onecall.core.catalog_order.customerorder.OrderCreationException;
import com.onecall.core.catalog_order.customerorder.OrderCreationException.Code;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

public final class CustomerOrderCommandService {
    private final ObjectProvider<CustomerOrderCreationService> owners;
    public CustomerOrderCommandService(ObjectProvider<CustomerOrderCreationService> owners) { this.owners = owners; }
    public void requireContext(TrustedCustomerCaller caller) {
        if (caller == null || caller.accountId() == null || caller.channel() == null || caller.selectedWorkspace() == null
                || caller.selectedWorkspace().workspaceType() == null
                || (caller.selectedWorkspace().workspaceType() == CustomerContext.WorkspaceType.PERSONAL)
                != (caller.selectedWorkspace().organizationId() == null)) throw new OrderCreationException(Code.SERVICE_UNAVAILABLE);
    }
    public Result create(TrustedCustomerCaller caller, Command command) {
        requireContext(caller);
        CustomerOrderCreationService owner;
        try { owner = owners.getObject(); }
        catch (BeansException unavailable) { throw new OrderCreationException(Code.SERVICE_UNAVAILABLE); }
        if (!caller.channel().name().equals(command.channel())) throw new OrderCreationException(Code.VALIDATION_FAILED);
        return owner.create(command, new Caller(caller.accountId(), caller.channel().name(),
                caller.selectedWorkspace().workspaceType().name(), caller.selectedWorkspace().organizationId()));
    }
}
