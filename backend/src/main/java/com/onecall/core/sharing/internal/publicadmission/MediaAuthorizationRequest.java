package com.onecall.core.sharing.internal.publicadmission;

public record MediaAuthorizationRequest(String authorizationRequestId, Purpose purpose) {
    public MediaAuthorizationRequest {
        if (authorizationRequestId == null || purpose == null) throw new ShareAdmissionException(ShareAdmissionException.Code.VALIDATION_FAILED);
    }
    public enum Purpose { PLAYBACK, SEEK, RENEW }
    @Override public String toString() { return "[REDACTED]"; }
}
