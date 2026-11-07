package com.onecall.core.sharing.internal.publicadmission;

import java.util.Optional;
import java.util.UUID;

public interface ShareMediaAuthorizationService {
    ShareAdmissionView.MediaAuthorization authorize(ShareAdmissionCapabilityToken capabilityToken, UUID admissionId,
            MediaAuthorizationRequest request, CallerResolver callerResolver) throws ShareAdmissionException;

    @FunctionalInterface
    interface CallerResolver {
        Optional<TrustedShareAdmissionCaller> resolve() throws ShareAdmissionException;
    }
}
