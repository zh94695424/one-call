package com.onecall.core.operations_iam.internal.subjectread;

import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadPorts.*;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService;
import com.onecall.core.operations_iam.subjectread.OperationsSubjectReadService.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.*;
import org.jooq.impl.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("persistence")
@ConditionalOnProperty(prefix = "onecall.operations-subject-read", name = "enabled", havingValue = "true")
@Lazy
public class OperationsSubjectReadPersistenceConfiguration {
    @Bean
    @Lazy
    OperationsSubjectReadProperties operationsSubjectReadProperties(ConfigurableEnvironment environment) {
        String prefix = "onecall.operations-subject-read.";
        var datasource = new OperationsSubjectReadProperties.Datasource(environment.getProperty(prefix + "datasource.url"),
                environment.getProperty(prefix + "datasource.username"), environment.getProperty(prefix + "datasource.password"));
        var keys = new ArrayList<OperationsSubjectReadProperties.KeyEntry>();
        boolean invalid = false;
        for (var source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    if (name.startsWith(prefix + "cursor.keys[")
                            && !name.matches("onecall\\.operations-subject-read\\.cursor\\.keys\\[[0-2]\\]\\.(id|material)")) invalid = true;
                }
            }
        }
        boolean gap = false;
        for (int index = 0; index < 3; index++) {
            String identifier = environment.getProperty(prefix + "cursor.keys[" + index + "].id");
            String material = environment.getProperty(prefix + "cursor.keys[" + index + "].material");
            if (identifier == null && material == null) gap = true;
            else {
                if (gap) invalid = true;
                keys.add(new OperationsSubjectReadProperties.KeyEntry(identifier, material));
            }
        }
        var cursor = new OperationsSubjectReadProperties.Cursor(environment.getProperty(prefix + "cursor.active-key-id"), invalid ? List.of() : keys);
        return new OperationsSubjectReadProperties(true, datasource, cursor);
    }

    @Bean(destroyMethod = "close")
    @Lazy
    HikariDataSource operationsSubjectReadDataSource(OperationsSubjectReadProperties properties) {
        if (properties.datasource() == null) throw new Failure(Code.SERVICE_UNAVAILABLE);
        properties.datasource().validate();
        var configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.datasource().url());
        configuration.setUsername(properties.datasource().username());
        configuration.setPassword(properties.datasource().password());
        configuration.setPoolName("operations-subject-read");
        configuration.setConnectionInitSql("SET TIME ZONE 'UTC'");
        configuration.setMaximumPoolSize(4);
        configuration.setMinimumIdle(0);
        configuration.setConnectionTimeout(3000);
        configuration.setInitializationFailTimeout(-1);
        try { return new GuardedReadDataSource(configuration); }
        catch (RuntimeException unavailable) { throw new Failure(Code.SERVICE_UNAVAILABLE); }
    }

    @Bean
    @Lazy
    DataSourceTransactionManager operationsSubjectReadTransactionManager(@Qualifier("operationsSubjectReadDataSource") DataSource datasource) {
        var manager = new DataSourceTransactionManager(datasource);
        manager.setRollbackOnCommitFailure(true);
        manager.setEnforceReadOnly(true);
        return manager;
    }

    @Bean
    @Lazy
    TransactionTemplate operationsSubjectReadTransactionTemplate(
            @Qualifier("operationsSubjectReadTransactionManager") DataSourceTransactionManager manager) {
        var template = new TransactionTemplate(manager) {
            @Override public <Result> Result execute(org.springframework.transaction.support.TransactionCallback<Result> action) {
                try { return super.execute(action); }
                finally {
                    if (manager.getDataSource() instanceof GuardedReadDataSource guarded) guarded.assertClean();
                }
            }
        };
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        template.setReadOnly(true);
        template.setTimeout(5);
        return template;
    }

    @Bean
    @Lazy
    DataSourceTransactionManager operationsSubjectReadCatalogSaleTransactionManager(
            @Qualifier("operationsSubjectReadDataSource") DataSource datasource) {
        var manager = new DataSourceTransactionManager(datasource);
        manager.setRollbackOnCommitFailure(true);
        manager.setEnforceReadOnly(false);
        return manager;
    }

    @Bean
    @Lazy
    TransactionTemplate operationsSubjectReadCatalogSaleTransactionTemplate(
            @Qualifier("operationsSubjectReadCatalogSaleTransactionManager") DataSourceTransactionManager manager) {
        var template = new TransactionTemplate(manager) {
            @Override public <Result> Result execute(org.springframework.transaction.support.TransactionCallback<Result> action) {
                try { return super.execute(action); }
                finally {
                    if (manager.getDataSource() instanceof GuardedReadDataSource guarded) guarded.assertClean();
                }
            }
        };
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        template.setReadOnly(false);
        template.setTimeout(5);
        return template;
    }

    @Bean
    @Lazy
    DSLContext operationsSubjectReadDsl(@Qualifier("operationsSubjectReadDataSource") DataSource datasource) {
        var configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.POSTGRES);
        configuration.set(new Settings().withExecuteLogging(false).withParamType(ParamType.INDEXED).withStatementType(StatementType.PREPARED_STATEMENT));
        configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(datasource)));
        return DSL.using(configuration);
    }

    @Bean
    @Lazy
    OperationsReadContext operationsSubjectReadContext(@Qualifier("operationsSubjectReadDataSource") DataSource datasource,
            @Qualifier("operationsSubjectReadDsl") DSLContext dsl) {
        return new OperationsReadContext(datasource, dsl);
    }

    @Bean
    @Lazy
    OperationsSubjectReadService operationsSubjectReadService(
            @Qualifier("operationsSubjectReadTransactionTemplate") TransactionTemplate transaction,
            @Qualifier("operationsSubjectReadDsl") DSLContext dsl,
            @Qualifier("operationsSubjectReadContext") OperationsReadContext context,
            OperationsSubjectReadProperties properties, ObjectProvider<SubjectReader> subjects,
            ObjectProvider<ActivationReader> activations, ObjectProvider<TrialReader> trials,
            ObjectProvider<EvidenceCounter> evidence, ObjectProvider<OrderCounter> orders) {
        return new DefaultOperationsSubjectReadService(transaction, dsl, context, properties, subjects, activations, trials, evidence, orders);
    }

    private static final class GuardedReadDataSource extends HikariDataSource {
        private final ThreadLocal<Boolean> cleanupFailure = new ThreadLocal<>();
        private GuardedReadDataSource(HikariConfig configuration) { super(configuration); }

        @Override public Connection getConnection() throws SQLException {
            cleanupFailure.remove();
            Connection connection = super.getConnection();
            try {
                boolean autoCommit = connection.getAutoCommit();
                boolean readOnly = connection.isReadOnly();
                int isolation = connection.getTransactionIsolation();
                String baseline = session(connection);
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                        (proxy, method, arguments) -> {
                            if ("close".equals(method.getName())) {
                                if (connection.isClosed()) return null;
                                try {
                                    if (!autoCommit || connection.getAutoCommit() != autoCommit || connection.isReadOnly() != readOnly
                                            || connection.getTransactionIsolation() != isolation || !Objects.equals(baseline, session(connection)))
                                        throw new SQLException("Read resource cleanup failed");
                                } catch (SQLException | RuntimeException unsafe) {
                                    cleanupFailure.set(true);
                                    evictConnection(connection);
                                    connection.close();
                                    throw new Failure(Code.SERVICE_UNAVAILABLE);
                                }
                                try { connection.close(); }
                                catch (SQLException | RuntimeException unsafe) {
                                    cleanupFailure.set(true);
                                    evictConnection(connection);
                                    throw new Failure(Code.SERVICE_UNAVAILABLE);
                                }
                                return null;
                            }
                            try { return method.invoke(connection, arguments); }
                            catch (InvocationTargetException failure) { throw failure.getCause(); }
                        });
            } catch (SQLException | RuntimeException unsafe) {
                evictConnection(connection);
                connection.close();
                throw new Failure(Code.SERVICE_UNAVAILABLE);
            }
        }

        private void assertClean() {
            boolean failed = Boolean.TRUE.equals(cleanupFailure.get());
            cleanupFailure.remove();
            if (failed) throw new Failure(Code.SERVICE_UNAVAILABLE);
        }

        private static String session(Connection connection) throws SQLException {
            try (var statement = connection.createStatement()) {
                statement.setQueryTimeout(2);
                try (var row = statement.executeQuery("SELECT session_user, current_user, current_setting('statement_timeout'), current_setting('lock_timeout'), current_setting('idle_in_transaction_session_timeout')")) {
                    if (!row.next() || !Objects.equals(row.getString(1), row.getString(2))) throw new SQLException("Read resource identity failed");
                    return row.getString(1) + "\n" + row.getString(3) + "\n" + row.getString(4) + "\n" + row.getString(5);
                }
            }
        }
    }
}
