package com.onecall.core.operations_iam.employeeauthority;

import java.util.UUID;

public interface EmployeeAuthorityReadPort {
    EmployeeAuthoritySnapshot read(UUID employeeId, TransactionWitness witness);
    record TransactionWitness(String login, int backendPid, long transactionId) { }
    record EmployeeAuthoritySnapshot(UUID employeeId, String status, String role, int revision) { }
    enum Code { SERVICE_UNAVAILABLE }
    final class Failure extends RuntimeException {
        private final Code code;
        private final String sqlState;
        public Failure(Code code) { this(code, null); }
        public Failure(Code code, String sqlState) { super("Employee authority unavailable", null, false, false); this.code = code; this.sqlState = sqlState != null && sqlState.matches("[0-9A-Z]{5}") ? sqlState : null; }
        public Code code() { return code; }
        public String sqlState() { return sqlState; }
    }
}
