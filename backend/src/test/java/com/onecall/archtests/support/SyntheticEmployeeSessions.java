package com.onecall.archtests.support;

import com.onecall.core.operations_iam.internal.security.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SyntheticEmployeeSessions implements EmployeeSessionVerifier {
    public final Map<String, VerifiedEmployeeSession> sessions = new ConcurrentHashMap<>();
    public volatile RuntimeException failure;
    @Override public VerifiedEmployeeSession verify(OpaqueEmployeeBearer bearer, String expectedAudience) {
        if (failure != null) throw failure;
        var session = sessions.get(bearer.value());
        if (session == null) throw new EmployeeSessionVerificationException(EmployeeSessionVerificationException.Code.UNAUTHENTICATED);
        return session;
    }
}
