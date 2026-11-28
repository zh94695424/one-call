package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;

import com.onecall.core.operations_iam.subjectread.OperationsOrderReadService;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext;
import com.onecall.core.operations_iam.internal.security.*;
import com.onecall.archtests.support.SyntheticEmployeeSessions;
import com.onecall.core.OneCallApplication;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.jooq.DSLContext;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.servlet.*;
import tools.jackson.databind.json.JsonMapper;

final class OperationsOrderReadFixture implements AutoCloseable {
    static final String BASE = "/api/v1/operations/orders";
    final CustomerBusinessNumbersFixture base = new CustomerBusinessNumbersFixture();
    final UUID employee = new UUID(43, 1), ops = new UUID(43, 2), disabled = new UUID(43, 3);
    final UUID subject = new UUID(43, 4), account = new UUID(43, 5), product = new UUID(43, 6),
            version = new UUID(43, 7), offer = new UUID(43, 8), consent = new UUID(43, 9);
    final Instant time = Instant.parse("2026-11-27T00:00:00.123456Z");
    final String login = "pa36_operations";
    final HttpClient client = HttpClient.newHttpClient();
    final JsonMapper mapper = JsonMapper.builder().build();
    ConfigurableApplicationContext context;
    HikariDataSource pool;
    DSLContext dsl;
    TransactionTemplate transaction;
    OperationsReadContext scope;
    SyntheticEmployeeSessions sessions;
    ErrorWitness witness;
    OperationsOrderReadService service;
    int port;

    @TestConfiguration(proxyBeanMethods = false)
    static class Sessions {
        @Bean SyntheticEmployeeSessions syntheticEmployeeSessions() { return new SyntheticEmployeeSessions(); }
        @Bean ErrorWitness errorWitness() { return new ErrorWitness(); }
        @Bean FilterRegistrationBean<Filter> errorWitnessRegistration(ErrorWitness witness) {
            Filter observer = (incoming, outgoing, chain) -> {
                witness.observe((jakarta.servlet.http.HttpServletRequest) incoming);
                chain.doFilter(incoming, outgoing);
            };
            var registration = new FilterRegistrationBean<>(observer);
            registration.addUrlPatterns("/*");
            registration.setDispatcherTypes(DispatcherType.ERROR);
            registration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
            return registration;
        }
        @Bean static org.springframework.beans.factory.config.BeanPostProcessor productionErrorFilterAfterWitness() {
            return new org.springframework.beans.factory.config.BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if ("operationsErrorBoundaryFilterRegistration".equals(beanName) && bean instanceof FilterRegistrationBean<?> registration)
                        registration.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1);
                    return bean;
                }
            };
        }
    }
    static final class ErrorWitness {
        private final List<Map<String, Object>> observations = Collections.synchronizedList(new ArrayList<>());
        void observe(jakarta.servlet.http.HttpServletRequest request) {
            var observation = new LinkedHashMap<String, Object>();
            observation.put("method", request.getAttribute(RequestDispatcher.ERROR_METHOD));
            observation.put("uri", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
            observation.put("status", request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
            observation.put("contextPath", request.getContextPath());
            observations.add(observation);
            System.out.println("PA43 observed ERROR attributes method=" + observation.get("method")
                    + " uri=" + observation.get("uri") + " status=" + observation.get("status")
                    + " contextPath=" + observation.get("contextPath"));
        }
        List<Map<String, Object>> drain() {
            synchronized (observations) {
                var drained = List.copyOf(observations);
                observations.clear();
                return drained;
            }
        }
    }
    void start() throws Exception {
        try {
            base.start(null);
            try (var connection = admin()) {
                execute(connection, "ALTER ROLE pa36_operations PASSWORD '" + base.password + "'");
                execute(connection, "ALTER ROLE pa27_query PASSWORD '" + base.password + "'");
            }
            var settings = new HashMap<String, Object>();
            String prefix = "onecall.operations-subject-read.";
            settings.put(prefix + "enabled", "true");
            settings.put(prefix + "datasource.url", base.url);
            settings.put(prefix + "datasource.username", login);
            settings.put(prefix + "datasource.password", base.password);
            var key = com.onecall.archtests.operations.OperationsOrderReadTestProviders.properties().cursor().keys().getFirst();
            settings.put(prefix + "cursor.active-key-id", key.id());
            settings.put(prefix + "cursor.keys[0].id", key.id());
            settings.put(prefix + "cursor.keys[0].material", key.material());
            settings.put("onecall.customer.persistence.url", base.url);
            settings.put("onecall.customer.persistence.username", "pa27_customer");
            settings.put("onecall.customer.persistence.password", base.password);
            settings.put("onecall.persistence.url", base.url);
            settings.put("onecall.persistence.username", "pa27_query");
            settings.put("onecall.persistence.password", base.password);
            settings.put("onecall.customer.business-numbers.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]));
            settings.put("onecall.customer.evidence.cursor-hmac-key", Base64.getEncoder().encodeToString(new byte[32]));
            for (String keyName : List.of("onecall.sharing-command.enabled", "onecall.sharing-admission.datasource.enabled", "onecall.evidence-media-read.datasource.enabled"))
                settings.put(keyName, "false");
            context = new SpringApplicationBuilder(OneCallApplication.class, Sessions.class).profiles("web", "persistence")
                    .initializers(application -> application.getEnvironment().getPropertySources().addFirst(new MapPropertySource("pa43-fixture", settings)))
                    .run("--server.port=0", "--spring.main.banner-mode=off");
            port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
            service = context.getBean(OperationsOrderReadService.class);
            dsl = context.getBean("operationsSubjectReadDsl", DSLContext.class);
            scope = context.getBean("operationsSubjectReadContext", OperationsReadContext.class);
            transaction = context.getBean("operationsSubjectReadTransactionTemplate", TransactionTemplate.class);
            pool = context.getBean("operationsSubjectReadDataSource", HikariDataSource.class);
            sessions = context.getBean(SyntheticEmployeeSessions.class);
            witness = context.getBean(ErrorWitness.class);
            assertErrorWitnessOrderPin();
        } catch (Exception | Error failure) { close(); throw failure; }
    }
    private void assertErrorWitnessOrderPin() {
        var errorDispatch = new TreeMap<String, FilterRegistrationBean<?>>();
        for (var entry : context.getBeansOfType(FilterRegistrationBean.class).entrySet())
            if (entry.getValue().determineDispatcherTypes() != null && entry.getValue().determineDispatcherTypes().contains(DispatcherType.ERROR))
                errorDispatch.put(entry.getKey(), entry.getValue());
        assertThat(errorDispatch.keySet()).as("ERROR-dispatch registrations are exactly the test witness plus the production boundary filter"
                + " (production bean renamed, absent or a second ERROR filter appeared?)")
                .containsExactlyInAnyOrder("errorWitnessRegistration", "operationsErrorBoundaryFilterRegistration");
        var production = errorDispatch.get("operationsErrorBoundaryFilterRegistration");
        var observer = errorDispatch.get("errorWitnessRegistration");
        assertThat(production.getOrder()).as("production ERROR filter order pin not applied: bean-name/order pinning did not match"
                + " operationsErrorBoundaryFilterRegistration; witness-before-boundary evidence would be tie-break dependent")
                .isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1);
        assertThat(observer.getOrder()).as("test ERROR witness keeps highest precedence")
                .isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
        assertThat(observer.getOrder()).as("test ERROR witness runs before the production ERROR boundary filter")
                .isLessThan(production.getOrder());
    }
    Connection admin() throws Exception { return base.admin(); }
    Connection runtime() throws Exception { return DriverManager.getConnection(base.url, login, base.password); }
    void reset() throws Exception {
        dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]);
        try (var connection = admin()) {
            execute(connection, "TRUNCATE customer_iam.customer_account, organization_access.organization, subject_number.business_subject, operations_iam.employee_account, catalog_order.catalog_product, platform.outbox_event, audit.audit_fact CASCADE");
            int index = 1;
            for (UUID identifier : List.of(employee, ops, disabled)) {
                execute(connection, "INSERT INTO operations_iam.employee_account(id,login_name_hmac,login_name_masked,auth_issuer,auth_subject_hmac,display_name,role,status) VALUES (?,?,'fixture','fixture',?,'PA43',?,?)",
                        identifier, "%064x".formatted(index), "%064x".formatted(index + 100), identifier.equals(ops) ? "OPS" : "ADMIN", identifier.equals(disabled) ? "DISABLED" : "ACTIVE");
                index++;
            }
        }
        sessions.sessions.clear();
        for (var entry : Map.of("admin", employee, "ops", ops, "disabled", disabled, "missing", new UUID(43, 99)).entrySet())
            sessions.sessions.put(entry.getKey(), new VerifiedEmployeeSession(entry.getValue(), EmployeeBearerAuthenticator.AUDIENCE));
        sessions.sessions.put("wrong", new VerifiedEmployeeSession(employee, "onecall:customer-session:v1"));
    }
    static Map<String, Object> fields(Object... pairs) {
        var result = new TreeMap<String, Object>();
        for (int index = 0; index < pairs.length; index += 2) result.put((String) pairs[index], pairs[index + 1]);
        return result;
    }
    String snapshot() {
        return mapper.writeValueAsString(fields("schemaVersion", 1, "catalogVersionId", version.toString(), "catalogOfferId", offer.toString(),
                "channel", "WEB", "versionToken", "PA43-historical", "productCode", "PA43", "versionNo", 1, "offerType", "ONE_TIME",
                "audience", "PERSONAL", "priceCents", 123L, "currency", "CNY", "billingCycle", "ONCE", "installments", 1,
                "benefitLines", List.of(fields("lineKey", new UUID(43, 10).toString(), "entitlementType", "AI_SECONDS", "quantity", 100,
                        "validityDays", 30, "grantIntervalMonths", 0, "schedule", List.of(fields("installmentNo", 1,
                                "entitlementType", "AI_SECONDS", "units", 100, "validFrom", "2027-01-01T00:00:00Z", "expiresAt", "2027-02-01T00:00:00Z")))),
                "terms", fields("historical", true)));
    }
    static String sha(String value) throws Exception {
        return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    void seed() throws Exception {
        try (var connection = admin()) {
            execute(connection, "INSERT INTO customer_iam.customer_account(id,display_name) VALUES (?,'PA43')", account);
            execute(connection, "INSERT INTO subject_number.business_subject(id,owner_account_id,source) VALUES (?,?,'WEB')", subject, account);
            execute(connection, "INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (?,'PA43','STANDARD')", product);
            execute(connection, "INSERT INTO catalog_order.catalog_version(id,product_id,version_no,sale_status,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,created_by_employee_id) VALUES (?,?,1,'DRAFT','ONE_TIME','PERSONAL',123,'CNY','ONCE',1,0,'{}',repeat('a',64),?)",
                    version, product, employee);
            execute(connection, "INSERT INTO catalog_order.catalog_offer(id,catalog_version_id,channel,version_token) VALUES (?,?,'WEB','current-offer')", offer, version);
            execute(connection, "INSERT INTO activation_consent.consent_record(id,subject_id,purpose,channel,event_domain,policy_code,policy_version,prompt_version,action,source_type,source_ref,account_id,snapshot,snapshot_sha256,event_id,request_fingerprint) VALUES (?,?,'PURCHASE','WEB','PURCHASE','PA43','1','1','ACCEPT','ACCOUNT',?,?,'{}',repeat('a',64),'pa43',repeat('b',64))",
                    consent, subject, account.toString(), account);
        }
    }
    UUID order(int sequence, String payment) throws Exception {
        UUID identity = new UUID(44, sequence);
        insertOrder(identity, subject, payment, time.minusSeconds(sequence / 10), 123L, snapshot(), null);
        return identity;
    }
    void insertOrder(UUID identity, UUID subjectId, String payment, Instant created, long amount, String snapshot, String hash) throws Exception {
        try (var connection = admin()) {
            execute(connection, "INSERT INTO catalog_order.customer_order(id,order_no,account_id,subject_id,channel,catalog_version_id,catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,version_token,idempotency_key,request_fingerprint,created_at,paid_at,closed_at) VALUES (?,?,?,?,'WEB',?,?,?,?, 'CNY',?,?::jsonb,?,'PA43-historical',?,repeat('c',64),?::timestamptz,CASE WHEN ?='PAID' THEN ?::timestamptz END,CASE WHEN ?='CLOSED' THEN ?::timestamptz END)",
                    identity, "PA43-" + identity, account, subjectId, version, offer, consent, amount, payment, snapshot,
                    hash == null ? sha(snapshot) : hash, identity.toString(), created.toString(), payment, created.toString(), payment, created.toString());
        }
    }
    void issuance(UUID order, int installment, String status) throws Exception {
        try (var connection = admin()) {
            UUID grant = UUID.randomUUID();
            if ("ISSUED".equals(status)) execute(connection,
                    "INSERT INTO entitlement_metering.entitlement_grant(id,subject_id,source_type,source_ref_id,entitlement_type,granted_units,valid_from,expires_at,order_id,installment_no,snapshot,snapshot_sha256) VALUES (?,?,'PURCHASE',?,'AI_SECONDS',100,now(),now()+interval '30 days',?,?,'{}',repeat('a',64))",
                    grant, subject, order, order, installment);
            execute(connection, "INSERT INTO entitlement_metering.entitlement_issuance(id,order_id,installment_no,entitlement_type,status,last_error_code,grant_id,issued_at,source_event_domain,source_event_id) VALUES (?,?,?,'AI_SECONDS',?,?,?,CASE WHEN ?='ISSUED' THEN now() END,'PAYMENT',?)",
                    UUID.randomUUID(), order, installment, status, "FAILED".equals(status) ? "SYNTHETIC" : null,
                    "ISSUED".equals(status) ? grant : null, status, UUID.randomUUID().toString());
        }
    }
    void bulk() throws Exception {
        seed();
        UUID original = order(1, "UNKNOWN");
        try (var connection = admin()) {
            execute(connection, """
                    INSERT INTO catalog_order.customer_order(id,order_no,account_id,subject_id,channel,catalog_version_id,
                        catalog_offer_id,purchase_consent_id,amount_cents,currency,payment_status,snapshot,snapshot_sha256,
                        version_token,idempotency_key,request_fingerprint,created_at)
                    SELECT md5('pa43-'||n)::uuid,'BULK-'||n,account_id,subject_id,channel,catalog_version_id,
                        catalog_offer_id,purchase_consent_id,amount_cents,currency,CASE WHEN n%2=0 THEN 'UNKNOWN' ELSE 'UNPAID' END,
                        snapshot,snapshot_sha256,version_token,'bulk-'||n,request_fingerprint,
                        created_at-(n%100)*interval '1 second'
                    FROM catalog_order.customer_order CROSS JOIN generate_series(1,10000) n WHERE id=?
                    """, original);
            execute(connection, """
                    INSERT INTO entitlement_metering.entitlement_issuance(id,order_id,installment_no,entitlement_type,
                        source_event_domain,source_event_id)
                    SELECT md5('pa43-issuance-'||n)::uuid,md5('pa43-'||n)::uuid,1,'AI_SECONDS','PAYMENT','pa43-'||n
                    FROM generate_series(1,10000) n
                    """);
            for (int index = 1; index <= 100; index++)
                execute(connection, "INSERT INTO entitlement_metering.entitlement_issuance(id,order_id,installment_no,entitlement_type,source_event_domain,source_event_id) VALUES (?,?,?,'AI_SECONDS','PAYMENT',?)",
                        UUID.randomUUID(), original, index, "dense-" + index);
            execute(connection, "ANALYZE catalog_order.customer_order");
            execute(connection, "ANALYZE entitlement_metering.entitlement_issuance");
        }
    }
    String inventory() throws Exception {
        try (var connection = admin()) {
            var names = new ArrayList<String>();
            try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                    "SELECT quote_ident(n.nspname)||'.'||quote_ident(c.relname) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind='r' AND n.nspname NOT IN ('pg_catalog','information_schema') AND n.nspname !~ '^pg_toast' ORDER BY 1")) {
                while (rows.next()) names.add(rows.getString(1));
            }
            var result = new StringBuilder();
            for (String table : names) result.append(table).append(':').append(OperationsIvrSessionReadFixture.one(connection,
                    "SELECT coalesce(md5(string_agg(row_to_json(facts)::text,E'\\n' ORDER BY row_to_json(facts)::text)),'EMPTY') FROM " + table + " facts")).append('\n');
            return result.toString();
        }
    }
    String securityInventory() throws Exception {
        try (var connection = admin()) {
            var result = new StringBuilder();
            for (String sql : List.of(
                    "SELECT oid,rolname,rolsuper,rolinherit,rolcreaterole,rolcreatedb,rolcanlogin,rolreplication,rolbypassrls,rolconfig FROM pg_roles",
                    "SELECT * FROM pg_auth_members",
                    "SELECT * FROM pg_db_role_setting",
                    "SELECT oid,nspname,nspowner,nspacl FROM pg_namespace",
                    "SELECT oid,relname,relnamespace,relowner,relkind,relacl,relnatts,relchecks,reloptions FROM pg_class WHERE relnamespace IN (SELECT oid FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema')",
                    "SELECT attrelid,attname,atttypid,attnum,attnotnull,attisdropped,attacl FROM pg_attribute WHERE attrelid IN (SELECT oid FROM pg_class WHERE relnamespace IN (SELECT oid FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema'))",
                    "SELECT * FROM pg_default_acl",
                    "SELECT * FROM pg_constraint WHERE connamespace IN (SELECT oid FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema')",
                    "SELECT oid,tgrelid,tgname,tgfoid,tgtype,tgenabled,tgisinternal,tgconstraint,tgdeferrable,tginitdeferred,tgattr,tgargs,tgqual FROM pg_trigger WHERE tgrelid IN (SELECT oid FROM pg_class WHERE relnamespace IN (SELECT oid FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema'))",
                    "SELECT oid,proname,pronamespace,proowner,prolang,prosecdef,provolatile,proargtypes,prosrc,proconfig,proacl FROM pg_proc WHERE pronamespace IN (SELECT oid FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema')",
                    "SELECT * FROM pg_sequences",
                    "SELECT name,setting,unit,context,source FROM pg_settings",
                    "SELECT datname,datdba,datacl FROM pg_database WHERE datname=current_database()")) {
                result.append(sql).append(':').append(OperationsIvrSessionReadFixture.one(connection,
                        "SELECT coalesce(md5(string_agg(row_to_json(facts)::text,E'\\n' ORDER BY row_to_json(facts)::text)),'EMPTY') FROM (" + sql + ") facts")).append('\n');
            }
            return result.toString();
        }
    }
    void clean() throws Exception {
        for (int index = 0; index < 4; index++) try (var connection = pool.getConnection()) {
            assertThat(connection.getAutoCommit()).isTrue();
            assertThat(connection.isReadOnly()).isFalse();
            assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
            assertThat(OperationsIvrSessionReadFixture.one(connection, "SELECT current_user||'|'||session_user")).isEqualTo(login + "|" + login);
            for (String setting : List.of("statement_timeout", "lock_timeout", "idle_in_transaction_session_timeout"))
                assertThat(OperationsIvrSessionReadFixture.one(connection, "SHOW " + setting)).isEqualTo("0");
        }
    }
    HttpResponse<String> request(String method, String path, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(15))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    @Override public void close() {
        if (context != null) context.close();
        base.close();
    }
}
