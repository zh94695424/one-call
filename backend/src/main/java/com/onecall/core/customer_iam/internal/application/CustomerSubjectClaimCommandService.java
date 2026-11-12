package com.onecall.core.customer_iam.internal.application;

import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.ClaimCommand;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.ClaimResult;
import com.onecall.core.subject_number.customerclaim.CustomerSubjectClaimService.PersonalClaimCaller;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException;
import com.onecall.core.subject_number.customerclaim.SubjectClaimException.Code;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

public final class CustomerSubjectClaimCommandService {
    private final ObjectProvider<CustomerSubjectClaimService> owner;
    public CustomerSubjectClaimCommandService(ObjectProvider<CustomerSubjectClaimService> owner) { this.owner = owner; }

    public void requirePersonal(TrustedCustomerCaller caller) {
        if (caller == null || caller.accountId() == null || caller.channel() == null || caller.selectedWorkspace() == null
                || caller.selectedWorkspace().workspaceType() == null) throw new SubjectClaimException(Code.SERVICE_UNAVAILABLE);
        if (caller.selectedWorkspace().workspaceType() != CustomerContext.WorkspaceType.PERSONAL
                || caller.selectedWorkspace().organizationId() != null) throw new SubjectClaimException(Code.FORBIDDEN);
    }

    public ClaimResult create(TrustedCustomerCaller caller, ClaimCommand command) {
        requirePersonal(caller);
        CustomerSubjectClaimService service;
        try { service = owner.getObject(); }
        catch (BeansException unavailable) { throw new SubjectClaimException(Code.SERVICE_UNAVAILABLE); }
        return service.create(command, new PersonalClaimCaller(caller.accountId()));
    }
}
