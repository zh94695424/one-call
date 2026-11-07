package com.onecall.core.sharing.internal.publicadmission;

import java.util.Optional;

public interface ShareAdmissionService {
    AdmissionOutcome admit(ShareAdmissionCapabilityToken token, ShareAdmissionRequest request,
            Optional<TrustedShareAdmissionCaller> caller);

    sealed interface AdmissionOutcome permits AdmissionCommitted, ReplayCommitted, DeniedCommitted { }
    record AdmissionCommitted(ShareAdmissionView view) implements AdmissionOutcome { }
    record ReplayCommitted(ShareAdmissionView view) implements AdmissionOutcome { }
    record DeniedCommitted(ShareAdmissionException.Code code) implements AdmissionOutcome { }
}
