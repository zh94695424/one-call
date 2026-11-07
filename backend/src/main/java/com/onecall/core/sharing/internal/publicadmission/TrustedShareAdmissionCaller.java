package com.onecall.core.sharing.internal.publicadmission;

import java.util.UUID;

public record TrustedShareAdmissionCaller(UUID accountId, UUID organizationId) {
    @Override
    public String toString() { return "[REDACTED]"; }
}
