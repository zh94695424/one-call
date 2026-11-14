package com.onecall.archtests.operations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.customer_iam.internal.security.CustomerBearerAuthenticator;
import com.onecall.core.customer_iam.internal.security.CustomerSessionVerifier;
import com.onecall.core.customer_iam.internal.web.CustomerBoundaryFilter;
import com.onecall.core.customer_iam.internal.web.CustomerProblemWriter;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.subjectread.*;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.*;
import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class OperationsSubjectReadTestProviders {
    public static final String BASE = "/api/v1/operations/subjects";
    public static final UUID SUBJECT = UUID.fromString("abcdef01-2345-6789-abcd-0123456789ab");
    public static final UUID EMPLOYEE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final Instant NOW = Instant.parse("2026-11-14T00:00:00.123456Z");
    public final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    public final SyntheticEmployeeSessions sessions = new SyntheticEmployeeSessions();
    public final SubjectReader subjects = mock(SubjectReader.class);
    public final ActivationReader activations = mock(ActivationReader.class);
    public final TrialReader trials = mock(TrialReader.class);
    public final EvidenceCounter evidence = mock(EvidenceCounter.class);
    public final OrderCounter orders = mock(OrderCounter.class);
    public final Record employee = mock(Record.class);
    public final DSLContext dsl = mock(DSLContext.class);
    public final OperationsReadContext scope = mock(OperationsReadContext.class);
    public final Connection connection = mock(Connection.class);
    public final DefaultOperationsSubjectReadService service;
    public final MockMvc mvc;

    public OperationsSubjectReadTestProviders() throws Exception {
        var source = mock(DataSource.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.setReadOnly(true);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(5);
        when(employee.get("id", UUID.class)).thenReturn(EMPLOYEE);
        when(employee.get("status", String.class)).thenReturn("ACTIVE");
        when(employee.get("role", String.class)).thenReturn("ADMIN");
        when(employee.get("revision", Integer.class)).thenReturn(1);
        when(dsl.fetchOne(anyString(), any(Object[].class))).thenReturn(employee);
        var time = mock(Record.class);
        when(time.get("now", OffsetDateTime.class)).thenReturn(NOW.atOffset(ZoneOffset.UTC));
        when(dsl.fetchSingle("SELECT transaction_timestamp() AS now")).thenReturn(time);
        beans.registerSingleton("subjectReader", subjects);
        beans.registerSingleton("activationReader", activations);
        beans.registerSingleton("trialReader", trials);
        beans.registerSingleton("evidenceCounter", evidence);
        beans.registerSingleton("orderCounter", orders);
        var key = new OperationsSubjectReadProperties.KeyEntry("Web-2026_11", Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]));
        service = new DefaultOperationsSubjectReadService(transaction, dsl, scope,
                new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor(key.id(), List.of(key))),
                beans.getBeanProvider(SubjectReader.class), beans.getBeanProvider(ActivationReader.class),
                beans.getBeanProvider(TrialReader.class), beans.getBeanProvider(EvidenceCounter.class), beans.getBeanProvider(OrderCounter.class));
        var facts = new SubjectFacts(SUBJECT, "WEB", null, null, "UNVERIFIED", false, NOW);
        when(subjects.fetchCandidates(any(), any(), anyInt())).thenReturn(List.of(new Candidate(facts)));
        when(subjects.detail(SUBJECT, 101)).thenReturn(Optional.of(new DetailFacts(facts, List.of(), List.of())));
        when(activations.find(any())).thenReturn(Map.of());
        when(trials.find(any(), any())).thenReturn(Optional.empty());
        beans.registerSingleton("readService", service);
        sessions.sessions.put("valid", new VerifiedEmployeeSession(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE));
        beans.registerSingleton("verifier", sessions);
        var problems = new OperationsProblemWriter();
        mvc = MockMvcBuilders.standaloneSetup(new OperationsSubjectReadController(beans.getBeanProvider(OperationsSubjectReadService.class), problems))
                .addFilters(new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(beans.getBeanProvider(EmployeeSessionVerifier.class)), problems),
                        new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter()))
                .build();
    }
}
