package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.*;
import static com.onecall.archtests.container.OperationsIvrSessionReadFixture.one;
import com.onecall.core.operations_iam.subjectread.OperationsReadContext.Role;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;

class OperationsOrderReadRoleLeastPrivilegeIT {
    static final OperationsOrderReadFixture fixture = new OperationsOrderReadFixture();
    String securityBefore;
    @BeforeAll static void start() throws Exception { fixture.start(); }
    @AfterAll static void stop() { fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); securityBefore = fixture.securityInventory(); }
    @AfterEach void clean() throws Exception { fixture.clean(); assertThat(fixture.securityInventory()).isEqualTo(securityBefore); }

    private void denied(String role, String sql, String state, boolean readonly) throws Exception {
        try (var connection = fixture.runtime()) {
            connection.setReadOnly(readonly);
            connection.setAutoCommit(false);
            if (role != null) execute(connection, "SET LOCAL ROLE " + role);
            assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class,
                    failure -> assertThat(failure.getSQLState()).isEqualTo(state));
            connection.rollback();
        }
    }
    @Test void provisionedLoginHasExactlyExistingSafeSetEdgesAndNoDirectObjectAccess() throws Exception {
        try (var connection = fixture.admin()) {
            assertThat(one(connection, "SELECT count(*) FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.member WHERE r.rolname='pa36_operations' AND NOT m.admin_option AND NOT m.inherit_option AND m.set_option")).isEqualTo("7");
            assertThat(one(connection, "SELECT string_agg(target.rolname,',' ORDER BY target.rolname) FROM pg_auth_members m JOIN pg_roles member ON member.oid=m.member JOIN pg_roles target ON target.oid=m.roleid WHERE member.rolname='pa36_operations'"))
                    .isEqualTo("activation_consent_role,catalog_order_role,entitlement_metering_role,evidence_role,ivr_role,operations_iam_role,subject_number_role");
            var field = EvidenceRoleIvrAssociationAclIT.class.getDeclaredField("ACL"); field.setAccessible(true);
            try (var statement = connection.createStatement(); var rows = statement.executeQuery((String) field.get(null))) {
                var actual = new TreeSet<String>();
                while (rows.next()) actual.add(rows.getString(1));
                assertThat(actual).hasSize(449).isEqualTo(CustomerOrderReadPrivilegesIT.current449Oracle());
            }
        }
        denied(null, "SELECT id FROM catalog_order.customer_order", "42501", false);
        denied("operations_iam_role", "SELECT id FROM catalog_order.customer_order", "42501", false);
        denied("operations_iam_role", "SELECT order_id FROM entitlement_metering.entitlement_issuance", "42501", false);
        for (String role : List.of("catalog_order_owner", "entitlement_metering_owner", "customer_iam_role",
                "platform_reliability_owner", "operations_service_role"))
            denied(null, "SET LOCAL ROLE " + role, "42501", false);
    }
    @Test void readOnlyTransactionDeniesWritesAndRowLocksWithoutPretendingOwnersAreReadOnly() throws Exception {
        fixture.seed(); fixture.order(1, "UNKNOWN");
        for (String sql : List.of("UPDATE catalog_order.customer_order SET revision=revision+1",
                "DELETE FROM catalog_order.customer_order", "SELECT id FROM catalog_order.customer_order FOR UPDATE",
                "SELECT id FROM catalog_order.customer_order FOR SHARE",
                "INSERT INTO catalog_order.catalog_product(id,product_code,product_family) VALUES (gen_random_uuid(),'PA43_DENIED','STANDARD')"))
            denied("catalog_order_role", sql, "25006", true);
        denied("catalog_order_role", "CREATE TABLE catalog_order.pa43_denied(id integer)", "42501", false);
        denied("catalog_order_role", "ALTER DEFAULT PRIVILEGES FOR ROLE catalog_order_owner GRANT SELECT ON TABLES TO operations_iam_role", "42501", false);
    }
    @Test void bothOwnerScopesWitnessOneReadOnlyRepeatableReadBackendAndRestoreAfterFailure() throws Exception {
        fixture.transaction.execute(status -> {
            fixture.scope.enter();
            try {
                int backend = fixture.dsl.fetchSingle("SELECT pg_backend_pid()").get(0, Integer.class);
                for (var role : Map.of(Role.ORDER, "catalog_order_role", Role.TRIAL, "entitlement_metering_role").entrySet()) {
                    fixture.scope.scoped(role.getKey(), () -> {
                        assertThat(fixture.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo(role.getValue());
                        assertThat(fixture.dsl.fetchSingle("SELECT session_user").get(0, String.class)).isEqualTo(fixture.login);
                        assertThat(fixture.dsl.fetchSingle("SELECT pg_backend_pid()").get(0, Integer.class)).isEqualTo(backend);
                        assertThat(fixture.dsl.fetchSingle("SHOW transaction_read_only").get(0, String.class)).isEqualTo("on");
                        assertThat(fixture.dsl.fetchSingle("SHOW transaction_isolation").get(0, String.class)).isEqualTo("repeatable read");
                        return null;
                    });
                    assertThat(fixture.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo("operations_iam_role");
                }
                assertThatThrownBy(() -> fixture.scope.scoped(Role.ORDER, () -> { throw new IllegalStateException("SYNTHETIC"); }))
                        .isInstanceOf(IllegalStateException.class);
                assertThat(fixture.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo("operations_iam_role");
            } finally { fixture.scope.exit(); }
            assertThat(fixture.dsl.fetchSingle("SELECT current_user").get(0, String.class)).isEqualTo(fixture.login);
            return null;
        });
    }
    @Test void constraintsRejectImpossibleAuthorityAndStatusWithoutDisablingAnything() throws Exception {
        fixture.seed(); var identity = fixture.order(1, "UNKNOWN");
        try (var connection = fixture.admin()) {
            assertThatThrownBy(() -> execute(connection, "UPDATE operations_iam.employee_account SET role='SUPPORT' WHERE id=?", fixture.employee))
                    .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("23514"));
            assertThatThrownBy(() -> execute(connection, "UPDATE catalog_order.customer_order SET payment_status='PAID',revision=revision+1 WHERE id=?", identity))
                    .isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("23514"));
        }
    }
}
