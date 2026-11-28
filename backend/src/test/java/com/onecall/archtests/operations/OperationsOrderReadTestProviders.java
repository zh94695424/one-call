package com.onecall.archtests.operations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.operations_iam.internal.orderread.*;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService;
import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import java.sql.Connection;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class OperationsOrderReadTestProviders {
    public static final String BASE = "/api/v1/operations/orders";
    public static final UUID EMPLOYEE = new UUID(1, 1), ORDER = new UUID(2, 1), SUBJECT = new UUID(3, 1);
    public static final Instant TIME = Instant.parse("2026-11-27T00:00:00.123456Z");
    public final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    public final OrderReader orders = mock(OrderReader.class);
    public final IssuanceReader issuances = mock(IssuanceReader.class);
    public final DSLContext dsl = mock(DSLContext.class);
    public final Record employee = mock(Record.class);
    public final OperationsReadContext scope = mock(OperationsReadContext.class);
    public final DataSource source = mock(DataSource.class);
    public final Connection connection = mock(Connection.class);
    public final TransactionTemplate transaction;
    public final DefaultOperationsOrderReadService service;
    public final Caller caller = new Caller(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE);
    public final SyntheticEmployeeSessions sessions = new SyntheticEmployeeSessions();
    public final MockMvc mvc;

    public OperationsOrderReadTestProviders() throws Exception {
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
        when(orders.fetchOrders(any(), any(), anyInt())).thenReturn(List.of(order()));
        when(orders.fetchOrder(ORDER)).thenReturn(Optional.of(facts()));
        when(issuances.fetchIssuances(anyList())).thenReturn(List.of());
        beans.registerSingleton("orders", orders);
        beans.registerSingleton("issuances", issuances);
        beans.registerSingleton("properties", properties());
        service = new DefaultOperationsOrderReadService(transaction, dsl, scope, beans.getBeanProvider(OperationsSubjectReadProperties.class),
                beans.getBeanProvider(OrderReader.class), beans.getBeanProvider(IssuanceReader.class));
        beans.registerSingleton("service", service);
        sessions.sessions.put("valid", new VerifiedEmployeeSession(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("wrong", new VerifiedEmployeeSession(EMPLOYEE, "customer"));
        beans.registerSingleton("verifier", sessions);
        var problems = new OperationsProblemWriter();
        mvc = MockMvcBuilders.standaloneSetup(new OperationsOrderReadController(beans.getBeanProvider(OperationsOrderReadService.class), problems))
                .addFilters(new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(beans.getBeanProvider(EmployeeSessionVerifier.class)), problems),
                        new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter())).build();
    }
    public static OperationsSubjectReadProperties properties() {
        var key = new OperationsSubjectReadProperties.KeyEntry("Test-43", Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]));
        return new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor(key.id(), List.of(key)));
    }
    public static RawListRequest raw(Map<String, List<String>> query) { return new RawListRequest(query, List.of()); }
    public static RawDetailRequest detail() { return new RawDetailRequest(ORDER.toString(), Map.of(), List.of()); }
    public static OrderFacts order() {
        return new OrderFacts(ORDER, "PA43-1", SUBJECT, "WEB", new UUID(4, 1), new UUID(5, 1), 100L, "CNY", "UNKNOWN", TIME);
    }
    public static DetailFacts facts() {
        var root = order();
        return new DetailFacts(root, "a".repeat(64), new Snapshot(1, root.catalogVersionId(), root.catalogOfferId(), "WEB",
                "v1." + "a".repeat(64), "PA43", "ONE_TIME", "PERSONAL", 100L, "CNY", "ONCE", 1,
                List.of(new BenefitLine("AI_SECONDS", 100, 30, 0)), "a".repeat(64)), new UUID(6, 1), null, null);
    }
}
