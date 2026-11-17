package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.one;
import com.onecall.core.operations_iam.subjectread.OperationsCatalogProductReadService;
import java.sql.*;
import java.util.*;
import tools.jackson.databind.JsonNode;

final class OperationsCatalogProductReadFixture implements AutoCloseable {
    static final String BASE = "/api/v1/operations/catalog/products";
    final OperationsIvrSessionReadFixture shared = new OperationsIvrSessionReadFixture();
    OperationsCatalogProductReadService service;
    String baselineCatalog;
    void start() throws Exception {
        shared.start();
        try (var connection = admin()) { execute(connection, "GRANT catalog_order_role TO " + shared.login + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE"); }
        service = shared.context.getBean(OperationsCatalogProductReadService.class); baselineCatalog = catalog();
    }
    Connection admin() throws Exception { return shared.admin(); }
    void reset() throws Exception {
        shared.dsl.configuration().set(new org.jooq.ExecuteListenerProvider[0]); shared.reset();
        try (var connection = admin()) { execute(connection, "TRUNCATE catalog_order.catalog_product CASCADE"); }
    }
    UUID product(int sequence, String code) throws Exception {
        UUID id = new UUID(37, sequence); try (var connection = admin()) { execute(connection, "INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (?,?,'STANDARD')", id, code); } return id;
    }
    UUID version(UUID product, int number, String state, boolean quote) throws Exception {
        UUID id = UUID.randomUUID();
        try (var connection = admin()) {
            execute(connection, "INSERT INTO catalog_order.catalog_version(id,product_id,version_no,sale_status,offer_type,audience,price_cents,currency,billing_cycle,installments,grant_interval_months,snapshot,snapshot_sha256,created_by_employee_id,published_at) VALUES (?,?,?,?,?,?,?,'CNY',?,1,0,'{}',repeat('a',64),?,?::timestamptz)",
                    id, product, number, state, quote ? "QUOTE" : "ONE_TIME", quote ? "ENTERPRISE" : "PERSONAL", quote ? null : 100L, quote ? "QUOTE" : "ONCE", shared.employee, state.equals("DRAFT") ? null : shared.time.toString());
        } return id;
    }
    void benefit(UUID version, int sequence, String type) throws Exception { try (var connection = admin()) { execute(connection, "INSERT INTO catalog_order.catalog_benefit_line(id,catalog_version_id,entitlement_type,quantity,validity_days,grant_interval_months) VALUES (?,?,?, ?,30,0)", new UUID(0, sequence), version, type, (long) sequence); } }
    void mutate(String sql, Object... values) throws Exception { shared.mutate(sql, values); }
    JsonNode get(String path) throws Exception {
        var before = shared.fingerprints(); var result = shared.get(path); assertThat(shared.fingerprints()).isEqualTo(before); return result;
    }
    String catalog() throws Exception {
        try (var connection = admin()) {
            return one(connection, "SELECT md5(string_agg(row_to_json(row)::text,E'\\n' ORDER BY row_to_json(row)::text)) FROM (SELECT rolname,rolsuper,rolinherit,rolcreaterole,rolcreatedb,rolcanlogin,rolreplication,rolbypassrls FROM pg_roles WHERE rolname !~ '^pg_' UNION ALL SELECT nspname,false,false,false,false,false,false,false FROM pg_namespace WHERE nspname !~ '^pg_' UNION ALL SELECT p.oid::regprocedure::text,false,false,false,false,false,false,false FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname !~ '^pg_') row")
                    + one(connection, "SELECT md5(string_agg(value,E'\\n' ORDER BY value)) FROM (SELECT nspname||':'||coalesce(nspacl::text,'') value FROM pg_namespace WHERE nspname !~ '^pg_' UNION ALL SELECT c.oid::regclass::text||':'||coalesce(c.relacl::text,'') FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname !~ '^pg_' UNION ALL SELECT attrelid::regclass::text||':'||attname||':'||coalesce(attacl::text,'') FROM pg_attribute WHERE attnum>0 AND NOT attisdropped) row");
        }
    }
    void unchanged() throws Exception { assertThat(catalog()).isEqualTo(baselineCatalog); shared.clean(); }
    @Override public void close() { shared.close(); }
}
