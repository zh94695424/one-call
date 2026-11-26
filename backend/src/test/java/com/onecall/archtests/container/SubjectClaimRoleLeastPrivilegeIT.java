package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.execute;
import static com.onecall.archtests.container.CustomerBusinessNumbersFixture.one;

import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;

@Timeout(30)
class SubjectClaimRoleLeastPrivilegeIT {
    static CustomerSubjectClaimFixture fixture;
    @BeforeAll static void start() throws Exception { fixture = new CustomerSubjectClaimFixture(); fixture.start(); }
    @AfterAll static void stop() { if (fixture != null) fixture.close(); }
    @BeforeEach void reset() throws Exception { fixture.reset(); }
    private static void denied(boolean localRole, String sql, Object... arguments) throws Exception {
        try (var connection = fixture.runtime()) {
            connection.setAutoCommit(false);
            if (localRole) execute(connection, "SET LOCAL ROLE subject_number_role");
            assertThatThrownBy(() -> execute(connection, sql, arguments)).isInstanceOfSatisfying(SQLException.class,
                    failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
            connection.rollback();
        }
    }
    @Test
    void syntheticLoginHasOnlyExistingSubjectNumberMembership() throws Exception {
        assertThat(fixture.value("SELECT rolcanlogin AND NOT rolinherit AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolreplication AND NOT rolbypassrls FROM pg_roles WHERE rolname=?", fixture.login)).isEqualTo("t");
        assertThat(fixture.value("SELECT string_agg(parent.rolname,',' ORDER BY parent.rolname) FROM pg_auth_members membership JOIN pg_roles parent ON parent.oid=membership.roleid JOIN pg_roles child ON child.oid=membership.member WHERE child.rolname=?", fixture.login)).isEqualTo("subject_number_role");
        assertThat(fixture.value("SELECT bool_and(NOT admin_option AND NOT inherit_option AND set_option) FROM pg_auth_members WHERE member=(SELECT oid FROM pg_roles WHERE rolname=?)", fixture.login)).isEqualTo("t");
    }
    @Test
    void directLoginCannotReadOrWriteBeforeLocalRole() throws Exception {
        denied(false, "SELECT id FROM subject_number.business_subject");
        denied(false, "INSERT INTO subject_number.business_subject(id,source) VALUES (?,'WEB')", UUID.randomUUID());
        denied(false, "SELECT id,status FROM customer_iam.customer_account"); fixture.cleanPool();
    }
    @Test
    void assumedRoleCanReadExactClaimPredicatesAndInsertHistory() throws Exception {
        var seed = fixture.seed();
        try (var connection = fixture.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE subject_number_role"); assertThat(one(connection, "SELECT current_user")).isEqualTo("subject_number_role");
            execute(connection, "SELECT id,owner_account_id,owner_organization_id,revision FROM subject_number.business_subject WHERE id=? FOR UPDATE", seed.subject());
            execute(connection, "SELECT id,subject_id,number_hmac,status,verified_at FROM subject_number.caller_number_binding WHERE id=? FOR SHARE", seed.binding());
            execute(connection, "SELECT id,state FROM subject_number.subject_risk_state WHERE caller_number_binding_id=? ORDER BY id FOR SHARE", seed.binding());
            execute(connection, "SELECT id,status FROM customer_iam.customer_account WHERE id=?", fixture.account);
            execute(connection, "INSERT INTO subject_number.subject_claim(id,subject_id,account_id,verification_method,verification_ref_ciphertext,request_id,status,risk_checked_at) VALUES (?,?,?,'SMS','v1.test.AAAA','acl','REJECTED',clock_timestamp())", UUID.randomUUID(), seed.subject(), fixture.account);
            assertThat(one(connection, "SELECT count(*) FROM subject_number.subject_claim")).isEqualTo("1"); connection.rollback();
        }
        fixture.post(seed, "approved", 200); fixture.audit("CLAIM_ACCEPTED", "ACCEPTED", "NO_OPEN"); fixture.cleanPool();
    }
    @Test
    void ownerUpdatePermitsOnlyAccountRevisionAndTimeColumns() throws Exception {
        var seed = fixture.seed(); denied(true, "UPDATE subject_number.business_subject SET owner_organization_id=? WHERE id=?", fixture.base.organizations.getFirst(), seed.subject());
        denied(true, "UPDATE subject_number.business_subject SET real_name_status='VERIFIED',real_name_verified_at=clock_timestamp() WHERE id=?", seed.subject());
        denied(true, "UPDATE subject_number.business_subject SET source='MINIAPP' WHERE id=?", seed.subject());
        try (var connection = fixture.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE subject_number_role");
            try (var statement = connection.prepareStatement("UPDATE subject_number.business_subject SET owner_account_id=?,revision=revision+1,updated_at=clock_timestamp() WHERE id=? AND owner_account_id IS NULL AND owner_organization_id IS NULL AND revision=1")) {
                statement.setObject(1, fixture.account); statement.setObject(2, seed.subject()); assertThat(statement.executeUpdate()).isEqualTo(1);
            }
            connection.rollback();
        }
        assertThat(fixture.owner(seed)).isEqualTo("null|null|1");
    }
    @Test
    void claimUpdateDeleteAndTruncateAreDenied() throws Exception {
        var seed = fixture.seed(); fixture.post(seed, "append-only", 200);
        denied(true, "UPDATE subject_number.subject_claim SET status='REJECTED'"); denied(true, "DELETE FROM subject_number.subject_claim"); denied(true, "TRUNCATE subject_number.subject_claim");
        for (String sql : List.of("UPDATE subject_number.subject_claim SET request_id='changed'", "DELETE FROM subject_number.subject_claim")) {
            try (var connection = fixture.admin()) {
                connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE subject_number_owner");
                assertThatThrownBy(() -> execute(connection, sql)).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("P0001")); connection.rollback();
            }
        }
        assertThat(fixture.count("subject_number.subject_claim")).isEqualTo(1);
    }
    @Test
    void foreignAccountWritesLocksAndAdjacentReadsAreDenied() throws Exception {
        denied(true, "SELECT display_name FROM customer_iam.customer_account");
        denied(true, "SELECT id,status FROM customer_iam.customer_account FOR SHARE"); denied(true, "SELECT id,status FROM customer_iam.customer_account FOR UPDATE");
        denied(true, "UPDATE customer_iam.customer_account SET status='DISABLED'"); denied(true, "SELECT id FROM customer_iam.auth_identity");
        denied(true, "SELECT id FROM evidence.evidence_record"); denied(true, "SELECT id FROM sharing.evidence_share_grant");
    }
    @Test
    void unrelatedDomainWritesDdlAndOwnerEscalationAreDenied() throws Exception {
        for (String sql : List.of("DELETE FROM evidence.evidence_record", "DELETE FROM sharing.evidence_share_grant", "DELETE FROM activation_consent.service_activation", "DELETE FROM catalog_order.customer_order", "DELETE FROM customer_iam.customer_account", "CREATE TABLE subject_number.pa30_forbidden(id int)", "SET LOCAL ROLE subject_number_owner", "SET LOCAL ROLE customer_iam_role", "SET LOCAL ROLE fixture_admin")) denied(true, sql);
        fixture.cleanPool();
    }
    @Test
    void advisoryAndAuditFunctionsNeedNoNewGrantOrMigration() throws Exception {
        var seed = fixture.seed();
        try (var connection = fixture.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE subject_number_role");
            for (String key : List.of("subject_number.number:" + seed.hmac(), "subject_number.subject:" + seed.subject(), "customer_iam.account:" + fixture.account))
                execute(connection, "SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))", key);
            execute(connection, "SELECT audit.append_fact(clock_timestamp(),'ACCOUNT',?::uuid,'SUBJECT_CLAIM_DECIDED','BUSINESS_SUBJECT',?::uuid,'CLAIM_REPLAYED',NULL::jsonb,'{\"reason\":\"CLAIM_REPLAYED\",\"outcome\":\"REPLAYED\",\"riskEvaluation\":\"NO_OPEN\"}'::jsonb,'acl',?::text)", fixture.account, seed.subject(), UUID.randomUUID().toString());
            connection.commit();
        }
        fixture.audit("CLAIM_REPLAYED", "REPLAYED", "NO_OPEN"); denied(true, "DELETE FROM audit.audit_fact");
        denied(true, "INSERT INTO audit.audit_fact(id) VALUES (?)", UUID.randomUUID());
        denied(true, "DELETE FROM platform.outbox_event"); fixture.cleanPool();
    }
    @Test
    void localRoleAndTransactionLocksClearOnCommitRollbackAndDenial() throws Exception {
        for (boolean rollback : List.of(false, true)) {
            fixture.transaction.execute(status -> {
                fixture.dsl.execute("SET LOCAL ROLE subject_number_role");
                fixture.dsl.fetch("SELECT pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(?,0))", "subject_number.subject:" + UUID.randomUUID());
                if (rollback) status.setRollbackOnly(); return null;
            }); fixture.cleanPool();
        }
        var seed = fixture.seed(); fixture.risk(seed, "OPEN"); fixture.post(seed, "denied", 409); fixture.cleanPool();
        assertThatThrownBy(() -> fixture.transaction.execute(status -> { fixture.dsl.execute("SET LOCAL ROLE subject_number_role"); fixture.dsl.fetch("SELECT display_name FROM customer_iam.customer_account"); return null; })).isInstanceOf(RuntimeException.class);
        fixture.cleanPool();
    }
    @Test
    void frozenAclsHistoryAndCustomerQueryPrincipalRemainUnchanged() throws Exception {
        fixture.flyway().validate(); assertThat(fixture.flyway().info().applied()).hasSize(20);
        assertThat(fixture.value("SELECT has_table_privilege('subject_number_role','subject_number.subject_claim','SELECT,INSERT')")).isEqualTo("t");
        assertThat(fixture.value("SELECT has_table_privilege('subject_number_role','subject_number.subject_claim','UPDATE,DELETE,TRUNCATE')")).isEqualTo("f");
        try (var connection = fixture.base.runtime()) {
            connection.setAutoCommit(false); execute(connection, "SET LOCAL ROLE customer_iam_role");
            assertThatThrownBy(() -> execute(connection, "INSERT INTO subject_number.subject_claim(id) VALUES (?)", UUID.randomUUID())).isInstanceOfSatisfying(SQLException.class, failure -> assertThat(failure.getSQLState()).isEqualTo("42501")); connection.rollback();
        }
        assertThat(fixture.value("SELECT rolname FROM pg_roles WHERE rolname='pa27_customer'")).isEqualTo("pa27_customer");
    }
}
