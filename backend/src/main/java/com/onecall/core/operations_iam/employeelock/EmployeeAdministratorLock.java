package com.onecall.core.operations_iam.employeelock;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeAdministratorLock {
    Optional<EmployeeFacts> lockEmployeeAdministrator(UUID employeeId);
    record EmployeeFacts(UUID employeeId, String status, String role, int revision) {
        @Override public String toString() { return "EmployeeFacts[REDACTED]"; }
    }
    final class Failure extends RuntimeException {
        public enum Kind { UNAVAILABLE, INTEGRITY }
        private final Kind kind;
        public Failure(Kind kind) { super("Employee authority unavailable", null, true, false); this.kind = kind; }
        public Kind kind() { return kind; }
        @Override public String toString() { return "Failure[REDACTED]"; }
    }
}
