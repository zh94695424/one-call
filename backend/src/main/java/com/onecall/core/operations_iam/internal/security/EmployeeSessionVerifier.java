package com.onecall.core.operations_iam.internal.security;

public interface EmployeeSessionVerifier {
    VerifiedEmployeeSession verify(OpaqueEmployeeBearer bearer, String expectedAudience);
}
