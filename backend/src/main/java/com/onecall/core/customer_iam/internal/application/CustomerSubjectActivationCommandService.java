package com.onecall.core.customer_iam.internal.application;

import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService;
import com.onecall.core.activation_consent.customeractivation.CustomerSubjectActivationService.*;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException;
import com.onecall.core.activation_consent.customeractivation.SubjectActivationException.Code;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

public final class CustomerSubjectActivationCommandService {
    private final ObjectProvider<CustomerSubjectActivationService> owner;
    public CustomerSubjectActivationCommandService(ObjectProvider<CustomerSubjectActivationService> owner) { this.owner = owner; }
    public void requireContext(TrustedCustomerCaller caller) {
        if (caller == null || caller.accountId() == null || caller.channel() == null || caller.selectedWorkspace() == null
                || caller.selectedWorkspace().workspaceType() == null
                || (caller.selectedWorkspace().workspaceType() == CustomerContext.WorkspaceType.PERSONAL) != (caller.selectedWorkspace().organizationId() == null)) throw new SubjectActivationException(Code.SERVICE_UNAVAILABLE);
    }
    public ActivationResult activate(TrustedCustomerCaller caller, ActivationCommand command) {
        requireContext(caller);
        CustomerSubjectActivationService service;
        try { service = owner.getObject(); }
        catch (BeansException unavailable) { throw new SubjectActivationException(Code.SERVICE_UNAVAILABLE); }
        return service.activate(command, new ActivationCaller(caller.accountId(), Channel.valueOf(caller.channel().name()),
                WorkspaceType.valueOf(caller.selectedWorkspace().workspaceType().name()), caller.selectedWorkspace().organizationId()));
    }
}
