package com.onecall.archtests.operations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.operations_iam.internal.activationread.*;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectread.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsActivationReadService.RawListRequest;
import java.sql.Connection;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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

public final class OperationsActivationReadTestProviders {
    public static final String BASE = "/api/v1/operations/activations";
    public static final UUID EMPLOYEE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final Instant TIME = Instant.parse("2026-11-15T00:00:00.123456Z");
    public final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    public final SyntheticEmployeeSessions sessions = spy(new SyntheticEmployeeSessions());
    public final ActivationHistoryReader reader = mock(ActivationHistoryReader.class);
    public final DSLContext dsl = mock(DSLContext.class);
    public final Record employee = mock(Record.class);
    public final OperationsReadContext scope = mock(OperationsReadContext.class);
    public final DataSource source = mock(DataSource.class);
    public final Connection connection = mock(Connection.class);
    public final TransactionTemplate transaction;
    public final DefaultOperationsActivationReadService service;
    public final Caller caller = new Caller(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE);
    public final MockMvc mvc;

    public OperationsActivationReadTestProviders() throws Exception {
        when(source.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.setReadOnly(true);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(5);
        when(employee.get("id", UUID.class)).thenReturn(EMPLOYEE);
        when(employee.get("status", String.class)).thenReturn("ACTIVE");
        when(employee.get("role", String.class)).thenReturn("ADMIN");
        when(employee.get("revision", Integer.class)).thenReturn(1);
        when(dsl.fetchOne(anyString(), any(Object[].class))).thenReturn(employee);
        when(reader.fetch(any(), anyInt())).thenReturn(List.of(fact(1)));
        beans.registerSingleton("reader", reader);
        beans.registerSingleton("properties", properties());
        service = new DefaultOperationsActivationReadService(transaction, dsl, scope,
                beans.getBeanProvider(OperationsSubjectReadProperties.class), beans.getBeanProvider(ActivationHistoryReader.class));
        beans.registerSingleton("service", service);
        sessions.sessions.put("valid", new VerifiedEmployeeSession(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("wrong", new VerifiedEmployeeSession(EMPLOYEE, "onecall:customer-session:v1"));
        beans.registerSingleton("verifier", sessions);
        mvc = mvc();
    }

    public MockMvc mvc(Object... additionalControllers) {
        var controllers = new java.util.ArrayList<Object>();
        var problems = new OperationsProblemWriter();
        controllers.add(new OperationsActivationReadController(beans.getBeanProvider(OperationsActivationReadService.class), problems));
        controllers.addAll(List.of(additionalControllers));
        return MockMvcBuilders.standaloneSetup(controllers.toArray())
                .addFilters(new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(beans.getBeanProvider(EmployeeSessionVerifier.class)), problems),
                        new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter()))
                .build();
    }

    public static OperationsSubjectReadProperties properties() {
        var key = new OperationsSubjectReadProperties.KeyEntry("Test-34", Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]));
        return new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor(key.id(), List.of(key)));
    }

    public static RawListRequest raw(Map<String, List<String>> query) { return new RawListRequest(query, List.of()); }
    public static TransitionFacts fact(int sequence) {
        return new TransitionFacts(new UUID(0, sequence), new UUID(1, sequence), "event-" + sequence, "INACTIVE", "ACTIVE", 0, 1,
                "WEB", "ACTIVATION", "ACTIVATED", TIME.minusSeconds(sequence));
    }
}
