package com.onecall.archtests.operations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.customer_iam.internal.security.*;
import com.onecall.core.customer_iam.internal.web.*;
import com.onecall.core.operations_iam.internal.catalogread.*;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.core.operations_iam.internal.subjectread.OperationsSubjectReadProperties;
import com.onecall.core.operations_iam.internal.web.*;
import com.onecall.core.operations_iam.subjectlifecycle.OperationsSubjectLifecycleService.Caller;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService.*;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import java.sql.Connection;
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

public final class OperationsCatalogProductReadTestProviders {
    public static final String BASE = "/api/v1/operations/catalog/products";
    public static final UUID EMPLOYEE = new UUID(1, 1), PRODUCT = new UUID(2, 1), VERSION = new UUID(3, 1);
    public final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    public final CatalogProductReader reader = mock(CatalogProductReader.class);
    public final DSLContext dsl = mock(DSLContext.class);
    public final Record employee = mock(Record.class);
    public final OperationsReadContext scope = mock(OperationsReadContext.class);
    public final DataSource source = mock(DataSource.class);
    public final Connection connection = mock(Connection.class);
    public final TransactionTemplate transaction;
    public final DefaultOperationsCatalogProductReadService service;
    public final Caller caller = new Caller(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE);
    public final MockMvc mvc;

    public OperationsCatalogProductReadTestProviders() throws Exception {
        when(source.getConnection()).thenReturn(connection); when(connection.getAutoCommit()).thenReturn(true);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.setReadOnly(true); transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); transaction.setTimeout(5);
        when(employee.get("id", UUID.class)).thenReturn(EMPLOYEE); when(employee.get("status", String.class)).thenReturn("ACTIVE");
        when(employee.get("role", String.class)).thenReturn("ADMIN"); when(employee.get("revision", Integer.class)).thenReturn(1);
        when(dsl.fetchOne(anyString(), any(Object[].class))).thenReturn(employee);
        when(reader.roots(any(), anyInt())).thenReturn(List.of(product())); when(reader.product(PRODUCT)).thenReturn(product());
        when(reader.summaries(anyList())).thenReturn(List.of(version())); when(reader.versions(PRODUCT)).thenReturn(List.of(version()));
        when(reader.benefits(anyList())).thenReturn(List.of());
        beans.registerSingleton("reader", reader); beans.registerSingleton("properties", properties());
        service = new DefaultOperationsCatalogProductReadService(transaction, dsl, scope,
                beans.getBeanProvider(OperationsSubjectReadProperties.class), beans.getBeanProvider(CatalogProductReader.class));
        beans.registerSingleton("service", service);
        var sessions = new SyntheticEmployeeSessions();
        sessions.sessions.put("valid", new VerifiedEmployeeSession(EMPLOYEE, EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("wrong", new VerifiedEmployeeSession(EMPLOYEE, "customer")); beans.registerSingleton("verifier", sessions);
        var problems = new OperationsProblemWriter();
        mvc = MockMvcBuilders.standaloneSetup(new OperationsCatalogProductReadController(beans.getBeanProvider(OperationsCatalogProductReadService.class), problems))
                .addFilters(new OperationsBoundaryFilter(new EmployeeBearerAuthenticator(beans.getBeanProvider(EmployeeSessionVerifier.class)), problems),
                        new CustomerBoundaryFilter(new CustomerBearerAuthenticator(beans.getBeanProvider(CustomerSessionVerifier.class)), new CustomerProblemWriter())).build();
    }
    public static OperationsSubjectReadProperties properties() {
        var key = new OperationsSubjectReadProperties.KeyEntry("Test-37", Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]));
        return new OperationsSubjectReadProperties(true, null, new OperationsSubjectReadProperties.Cursor(key.id(), List.of(key)));
    }
    public static RawListRequest raw(Map<String, List<String>> query) { return new RawListRequest(query, List.of()); }
    public static RawDetailRequest detail() { return new RawDetailRequest(PRODUCT.toString(), Map.of(), List.of()); }
    public static ProductFacts product() { return new ProductFacts(PRODUCT, "AB", "STANDARD"); }
    public static VersionFacts version() { return new VersionFacts(PRODUCT, VERSION, 1, "DRAFT", "ONE_TIME", "PERSONAL", 100L, "CNY", "ONCE", 1, 0, null, 1); }
    public static VersionFacts with(VersionFacts original, String field, Object value) {
        try {
            var fields = VersionFacts.class.getRecordComponents(); Object[] values = new Object[fields.length];
            for (int index = 0; index < fields.length; index++) values[index] = fields[index].getName().equals(field) ? value : fields[index].getAccessor().invoke(original);
            return VersionFacts.class.getDeclaredConstructors()[0].newInstance(values) instanceof VersionFacts result ? result : null;
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
}
