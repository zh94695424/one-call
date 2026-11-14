package com.onecall.core.operations_iam.internal.security;

import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Code;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Failure;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;

public final class EmployeeBearerAuthenticator {
    public static final String AUDIENCE = "onecall:employee-session:v1";
    private static final Pattern BEARER = Pattern.compile("(?i:Bearer) +([A-Za-z0-9._~+/-]+=*)");
    private final ObjectProvider<EmployeeSessionVerifier> verifiers;
    public EmployeeBearerAuthenticator(ObjectProvider<EmployeeSessionVerifier> verifiers) { this.verifiers = verifiers; }
    public Caller authenticate(Enumeration<String> headers) {
        if (headers == null || !headers.hasMoreElements()) throw new Failure(Code.UNAUTHENTICATED);
        String header = headers.nextElement();
        if (header == null || headers.hasMoreElements()) throw new Failure(Code.UNAUTHENTICATED);
        var match = BEARER.matcher(header);
        if (!match.matches()) throw new Failure(Code.UNAUTHENTICATED);
        List<EmployeeSessionVerifier> candidates;
        try { candidates = verifiers.stream().limit(2).toList(); }
        catch (RuntimeException unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
        if (candidates.size() != 1) throw new Failure(Code.SERVICE_UNAVAILABLE);
        VerifiedEmployeeSession session;
        try { session = candidates.getFirst().verify(new OpaqueEmployeeBearer(match.group(1)), AUDIENCE); }
        catch (EmployeeSessionVerificationException known) {
            throw new Failure(known.code() == EmployeeSessionVerificationException.Code.UNAUTHENTICATED ? Code.UNAUTHENTICATED : Code.SERVICE_UNAVAILABLE);
        } catch (RuntimeException unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
        if (session == null || session.employeeId() == null || session.audience() == null || session.audience().isBlank()) throw new Failure(Code.SERVICE_UNAVAILABLE);
        if (!AUDIENCE.equals(session.audience())) throw new Failure(Code.UNAUTHENTICATED);
        return new Caller(session.employeeId(), session.audience());
    }
}
