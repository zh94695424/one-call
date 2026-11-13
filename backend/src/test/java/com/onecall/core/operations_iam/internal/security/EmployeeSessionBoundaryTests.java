package com.onecall.core.operations_iam.internal.security;

import static org.assertj.core.api.Assertions.*;
import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class EmployeeSessionBoundaryTests {
    static EmployeeBearerAuthenticator auth(EmployeeSessionVerifier... providers) {
        var beans = new DefaultListableBeanFactory();
        for (int index = 0; index < providers.length; index++) beans.registerSingleton("verifier" + index, providers[index]);
        return new EmployeeBearerAuthenticator(beans.getBeanProvider(EmployeeSessionVerifier.class));
    }
    static Enumeration<String> headers(String... values) { return Collections.enumeration(Arrays.asList(values)); }
    static void denied(EmployeeBearerAuthenticator auth, Code code, String... values) {
        assertThatThrownBy(() -> auth.authenticate(headers(values))).isInstanceOfSatisfying(Failure.class, failure -> assertThat(failure.code()).isEqualTo(code));
    }

    @Test
    void missingMalformedAndDuplicateBearerAreUnauthenticated() throws Exception {
        var auth = auth((bearer, audience) -> { throw new AssertionError("Provider must not run"); });
        for (String[] values : new String[][] {{}, {""}, {"Bearer"}, {"Basic token"}, {"Bearer a,b"}, {"Bearer a", "Bearer a"}, {"Bearer a\n"}}) denied(auth, Code.UNAUTHENTICATED, values);
    }
    @Test
    void employeeAudienceIsPassedAndIndependentlyValidated() throws Exception {
        UUID employee = UUID.randomUUID();
        var auth = auth((bearer, audience) -> { assertThat(audience).isEqualTo("onecall:employee-session:v1"); return new VerifiedEmployeeSession(employee, audience); });
        assertThat(auth.authenticate(headers("bEaReR   opaque==")).employeeId()).isEqualTo(employee);
        denied(auth((bearer, audience) -> new VerifiedEmployeeSession(employee, "onecall:customer-session:v1")), Code.UNAUTHENTICATED, "Bearer opaque");
    }
    @Test
    void customerTokensAndClientIdentityNeverAuthenticateEmployee() throws Exception {
        var sessions = new SyntheticEmployeeSessions();
        sessions.sessions.put("employee", new VerifiedEmployeeSession(UUID.randomUUID(), EmployeeBearerAuthenticator.AUDIENCE));
        denied(auth(sessions), Code.UNAUTHENTICATED, "Bearer customer");
        assertThat(auth(sessions).authenticate(headers("Bearer employee")).employeeId()).isEqualTo(sessions.sessions.get("employee").employeeId());
    }
    @Test
    void absentAndAmbiguousVerifierFailClosed() throws Exception {
        denied(auth(), Code.SERVICE_UNAVAILABLE, "Bearer opaque");
        var provider = new SyntheticEmployeeSessions(); denied(auth(provider, provider), Code.SERVICE_UNAVAILABLE, "Bearer opaque");
    }
    @Test
    void unusableVerifierAndMalformedAttestationFailClosed() throws Exception {
        for (VerifiedEmployeeSession session : Arrays.asList(null, new VerifiedEmployeeSession(null, EmployeeBearerAuthenticator.AUDIENCE), new VerifiedEmployeeSession(UUID.randomUUID(), null))) denied(auth((bearer, audience) -> session), Code.SERVICE_UNAVAILABLE, "Bearer opaque");
        denied(auth((bearer, audience) -> { throw new IllegalStateException("SECRET"); }), Code.SERVICE_UNAVAILABLE, "Bearer opaque");
        var beans = new DefaultListableBeanFactory();
        var definition = new org.springframework.beans.factory.support.RootBeanDefinition(EmployeeSessionVerifier.class, () -> { throw new IllegalStateException("SECRET"); });
        beans.registerBeanDefinition("broken", definition);
        denied(new EmployeeBearerAuthenticator(beans.getBeanProvider(EmployeeSessionVerifier.class)), Code.SERVICE_UNAVAILABLE, "Bearer opaque");
    }
    @Test
    void invalidExpiredAndRevokedSessionAreUnauthenticated() throws Exception {
        for (String token : List.of("invalid", "expired", "revoked")) denied(auth(new SyntheticEmployeeSessions()), Code.UNAUTHENTICATED, "Bearer " + token);
        denied(auth((bearer, audience) -> { throw new EmployeeSessionVerificationException(EmployeeSessionVerificationException.Code.SERVICE_UNAVAILABLE); }), Code.SERVICE_UNAVAILABLE, "Bearer opaque");
    }
    @Test
    void opaqueValuesAndProviderFailuresHaveRedactedRepresentations() throws Exception {
        assertThat(new OpaqueEmployeeBearer("SECRET").toString()).doesNotContain("SECRET");
        assertThat(new VerifiedEmployeeSession(UUID.randomUUID(), "SECRET").toString()).doesNotContain("SECRET");
        for (var code : EmployeeSessionVerificationException.Code.values()) { var failure = new EmployeeSessionVerificationException(code); assertThat(failure.toString() + failure.getMessage()).doesNotContain("SECRET"); assertThat(failure.getCause()).isNull(); }
        assertThat(new Caller(UUID.randomUUID(), "SECRET").toString()).doesNotContain("SECRET");
    }
    @Test
    void concurrentSessionsNeverShareTrustedCallerState() throws Exception {
        var sessions = new SyntheticEmployeeSessions();
        for (int index = 0; index < 32; index++) sessions.sessions.put("token" + index, new VerifiedEmployeeSession(UUID.randomUUID(), EmployeeBearerAuthenticator.AUDIENCE));
        var auth = auth(sessions);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = sessions.sessions.entrySet().stream().map(entry -> executor.submit(() -> { assertThat(auth.authenticate(headers("Bearer " + entry.getKey())).employeeId()).isEqualTo(entry.getValue().employeeId()); })).toList();
            for (var future : futures) future.get(10, TimeUnit.SECONDS);
        }
    }
}
