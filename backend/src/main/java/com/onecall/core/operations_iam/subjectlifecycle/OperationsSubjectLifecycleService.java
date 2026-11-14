package com.onecall.core.operations_iam.subjectlifecycle;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

public interface OperationsSubjectLifecycleService {
    ActivationTransition suspend(Command command, Caller caller);
    ActivationTransition resume(Command command, Caller caller);
    record Command(UUID subjectId, String actionEventId, String reasonCode, BigInteger expectedActivationRevision) {
        @Override public String toString() { return "Command[REDACTED]"; }
    }
    record Caller(UUID employeeId, String audience) {
        @Override public String toString() { return "Caller[REDACTED]"; }
    }
    record ActivationTransition(UUID transitionId, String statusEventId, String fromStatus, String toStatus,
            int fromRevision, int toRevision, String sourceChannel, String sourceActionDomain, String reasonCode, Instant occurredAt) { }
    enum Code { UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, VALIDATION_FAILED, ACTIVATION_STATE_CONFLICT, RATE_LIMITED, SERVICE_UNAVAILABLE, INTERNAL }
    final class Failure extends RuntimeException {
        private final Code code;
        public Failure(Code code) { super("Operations lifecycle failed", null, false, false); this.code = code; }
        public Code code() { return code; }
        @Override public String toString() { return "Failure[REDACTED]"; }
    }
}
