package com.onecall.core.operations_iam.internal.security;

import java.util.UUID;

public record VerifiedEmployeeSession(UUID employeeId, String audience) {
    @Override public String toString() { return "VerifiedEmployeeSession[REDACTED]"; }
}
