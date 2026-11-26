package com.onecall.archtests.container;

import static org.assertj.core.api.Assertions.*;
import com.onecall.core.sharing.internal.admission.*;
import com.onecall.core.sharing.internal.publicadmission.*;
import com.onecall.core.sharing.internal.persistence.ShareAdmissionPersistenceConfiguration;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SharingAdmissionRoleLeastPrivilegeIT {
    private static final SharingQueryFixture BASE=new SharingQueryFixture();
    private static Map<String,String> before;
    @BeforeAll static void start() throws Exception { before=bytes(); BASE.start(); }
    @AfterAll static void stop() throws Exception { BASE.close(); }

    @Test void v012CreatesExactAdmissionRoleAndIsReplaySafe() throws Exception {
        try(Connection connection=BASE.admin(); var statement=connection.createStatement()) {
            try(var rows=statement.executeQuery("SELECT rolcanlogin,rolsuper,rolcreatedb,rolcreaterole,rolinherit,rolreplication,rolbypassrls,rolconfig IS NULL FROM pg_roles WHERE rolname='sharing_admission_role'")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getBoolean(1)).isTrue(); for(int column=2;column<=7;column++) assertThat(rows.getBoolean(column)).isFalse(); assertThat(rows.getBoolean(8)).isTrue(); assertThat(rows.next()).isFalse();
            }
            statement.execute("ALTER ROLE sharing_admission_role PASSWORD NULL");
            connection.setAutoCommit(false); statement.execute(migration()); connection.rollback();
            for (String incompatible : List.of("ALTER ROLE sharing_admission_role SUPERUSER", "ALTER ROLE sharing_admission_role INHERIT",
                    "GRANT SELECT (object_key) ON evidence.media_object TO sharing_admission_role", "GRANT EXECUTE ON FUNCTION sharing.guard_share_admission() TO sharing_admission_role")) {
                var savepoint=connection.setSavepoint(); statement.execute(incompatible);
                assertThatThrownBy(() -> statement.execute(migration())).isInstanceOfSatisfying(SQLException.class,failure -> assertThat(failure.getSQLState()).isEqualTo("P0001"));
                connection.rollback(savepoint); connection.releaseSavepoint(savepoint);
            }
            var savepoint=connection.setSavepoint();
            assertThatThrownBy(() -> statement.execute(migration()+"\nSELECT 1/0;")).isInstanceOfSatisfying(SQLException.class,failure -> assertThat(failure.getSQLState()).isEqualTo("22012"));
            connection.rollback(savepoint); connection.rollback();
            connection.setAutoCommit(true); statement.execute("ALTER ROLE sharing_admission_role PASSWORD '"+BASE.password+"'");
        }
        assertThat(BASE.flyway.migrate().migrationsExecuted).isZero();
        assertThat(BASE.flyway.info().applied()).hasSize(20);
        assertThat(BASE.flyway.info().applied()[11].getScript()).isEqualTo("V012__sharing_admission_least_privilege.sql");
    }
    @Test void v012GrantsExactSchemasAndColumnsWithoutFunctionExecution() throws Exception {
        try(Connection connection=BASE.admin()) {
            assertThat(strings(connection,"SELECT nspname FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema' AND has_schema_privilege('sharing_admission_role',oid,'USAGE') ORDER BY nspname"))
                    .containsExactly("customer_iam","evidence","organization_access","sharing","subject_number");
            assertThat(strings(connection,"SELECT p.oid::text FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname !~ '^pg_' AND n.nspname <> 'information_schema' AND has_function_privilege('sharing_admission_role',p.oid,'EXECUTE')")).isEmpty();
            assertThat(strings(connection,"SELECT a.attname FROM pg_attribute a WHERE a.attrelid='sharing.evidence_share_grant'::regclass AND a.attnum>0 AND NOT a.attisdropped AND has_column_privilege('sharing_admission_role',a.attrelid,a.attnum,'SELECT') ORDER BY a.attnum"))
                    .containsExactly("id","share_id","capability_token_hash","evidence_id","created_by_account_id","owner_organization_id","verification_mode","code_salt","code_hash","code_generation","valid_from","expires_at","max_admissions","admissions_used","download_allowed","content_snapshot","content_sha256","status","revocation_generation","revision");
            assertThat(strings(connection,"SELECT a.attname FROM pg_attribute a WHERE a.attrelid='sharing.evidence_share_grant'::regclass AND a.attnum>0 AND NOT a.attisdropped AND has_column_privilege('sharing_admission_role',a.attrelid,a.attnum,'UPDATE') ORDER BY a.attname"))
                    .containsExactly("admissions_used","revision","revocation_generation","status");
            Map<String,List<String>> expected=Map.ofEntries(
                    Map.entry("sharing.evidence_share_grant", List.of("id","share_id","capability_token_hash","evidence_id","created_by_account_id","owner_organization_id","verification_mode","code_salt","code_hash","code_generation","valid_from","expires_at","max_admissions","admissions_used","download_allowed","content_snapshot","content_sha256","status","revocation_generation","revision")),
                    Map.entry("sharing.share_admission", List.of("id","share_grant_id","admission_key","admission_no","verification_mode","verification_result","admitted_at")),
                    Map.entry("sharing.share_access_event", List.of("id","share_grant_id","share_admission_id","event_domain","event_id","event_type","result","occurred_at","risk_digest","payload_redacted","payload_sha256")),
                    Map.entry("customer_iam.customer_account", List.of("id","status")),
                    Map.entry("organization_access.organization", List.of("id","status")),
                    Map.entry("organization_access.organization_membership", List.of("id","organization_id","account_id","role","status","revision")),
                    Map.entry("evidence.evidence_record", List.of("id","subject_id","identity_at_capture","status")),
                    Map.entry("subject_number.business_subject", List.of("id","owner_account_id","owner_organization_id","revision")),
                    Map.entry("subject_number.caller_number_binding", List.of("id","subject_id","number_type","number_hmac","status","verified_at","revision")),
                    Map.entry("subject_number.subject_risk_state", List.of("id","caller_number_binding_id","risk_type","state","observed_at","cleared_at","revision")));
            for(var entry:expected.entrySet()) {
                assertThat(strings(connection,"SELECT attname FROM pg_attribute WHERE attrelid='"+entry.getKey()+"'::regclass AND attnum>0 AND NOT attisdropped AND has_column_privilege('sharing_admission_role',attrelid,attnum,'SELECT') ORDER BY attnum"))
                        .containsExactlyElementsOf(entry.getValue());
                for(String privilege:List.of("INSERT","UPDATE")) {
                    var writable=privilege.equals("INSERT") && Set.of("sharing.share_admission","sharing.share_access_event").contains(entry.getKey())
                            ? entry.getValue() : privilege.equals("UPDATE") && entry.getKey().equals("sharing.evidence_share_grant")
                            ? List.of("admissions_used","status","revocation_generation","revision") : List.<String>of();
                    assertThat(strings(connection,"SELECT attname FROM pg_attribute WHERE attrelid='"+entry.getKey()+"'::regclass AND attnum>0 AND NOT attisdropped AND has_column_privilege('sharing_admission_role',attrelid,attnum,'"+privilege+"') ORDER BY attnum"))
                            .containsExactlyElementsOf(writable);
                }
            }
            assertThat(strings(connection,"SELECT acl.privilege_type FROM pg_class c CROSS JOIN LATERAL aclexplode(c.relacl) acl WHERE acl.grantee='sharing_admission_role'::regrole")).isEmpty();
        }
    }
    @Test void v012DeniesPublicReadRoleEscalationAndCreatorRoleSubstitution() throws Exception {
        try(Connection connection=login()) { denied(connection,"SET ROLE sharing_command_role"); denied(connection,"SET ROLE sharing_public_read_role"); }
        try(Connection connection=BASE.admin()) {
            assertThat(strings(connection,"SELECT member::text FROM pg_auth_members WHERE member='sharing_admission_role'::regrole OR roleid='sharing_admission_role'::regrole")).isEmpty();
            assertThat(strings(connection,"SELECT a.attname FROM pg_attribute a WHERE a.attrelid='sharing.evidence_share_grant'::regclass AND a.attnum>0 AND NOT a.attisdropped AND has_column_privilege('sharing_public_read_role',a.attrelid,a.attnum,'UPDATE')")).isEmpty();
        }
    }
    @Test void v012DeniesDmlDdlSequencesFunctionsAndGrantOptions() throws Exception {
        try(Connection connection=login()) {
            for(String sql:List.of("DELETE FROM sharing.share_admission","UPDATE sharing.share_access_event SET result='FAILED'","TRUNCATE sharing.share_admission","CREATE TABLE sharing.pa17_forbidden(id int)","CREATE TEMP TABLE pa17_forbidden(id int)","SELECT sharing.guard_share_admission()","SELECT object_key FROM evidence.media_object")) denied(connection,sql);
        }
        try(Connection connection=BASE.admin()) {
            assertThat(strings(connection,"SELECT acl.privilege_type FROM pg_attribute a CROSS JOIN LATERAL aclexplode(a.attacl) acl WHERE acl.grantee='sharing_admission_role'::regrole AND acl.is_grantable")).isEmpty();
            assertThat(strings(connection,"SELECT schemaname||'.'||sequencename FROM pg_sequences WHERE has_sequence_privilege('sharing_admission_role',schemaname||'.'||sequencename,'USAGE') OR has_sequence_privilege('sharing_admission_role',schemaname||'.'||sequencename,'SELECT') OR has_sequence_privilege('sharing_admission_role',schemaname||'.'||sequencename,'UPDATE')")).isEmpty();
        }
    }
    @Test void admissionPoolAssertsCurrentAndSessionUserBeforeWrite() throws Exception {
        try(Connection connection=login();var statement=connection.createStatement();var rows=statement.executeQuery("SELECT current_user,session_user")) {
            assertThat(rows.next()).isTrue(); assertThat(rows.getString(1)).isEqualTo("sharing_admission_role"); assertThat(rows.getString(2)).isEqualTo("sharing_admission_role");
        }
        var source=new DriverManagerDataSource(BASE.url,"fixture_admin",BASE.password);
        var configuration=new org.jooq.impl.DefaultConfiguration(); configuration.set(org.jooq.SQLDialect.POSTGRES);
        configuration.set(new org.jooq.impl.DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(source)));
        var repository=new JooqShareAdmissionRepository(org.jooq.impl.DSL.using(configuration));
        var transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        assertThatThrownBy(() -> transaction.execute(status -> repository.lockGrant(UUID.randomUUID())))
                .isInstanceOf(com.onecall.core.sharing.internal.publicadmission.ShareAdmissionException.class);
    }
    @Test void admissionPoolResetsRoleTransactionAndReadOnlyState() throws Exception {
        var source=new com.zaxxer.hikari.HikariDataSource(); source.setJdbcUrl(BASE.url); source.setUsername("sharing_admission_role"); source.setPassword(BASE.password); source.setMaximumPoolSize(1);
        try {
            try(Connection connection=source.getConnection()) { connection.setReadOnly(true); connection.setAutoCommit(false); connection.rollback(); }
            try(Connection connection=source.getConnection();var statement=connection.createStatement();var rows=statement.executeQuery("SELECT current_user,session_user,current_setting('transaction_read_only')")) {
                assertThat(connection.getAutoCommit()).isTrue(); assertThat(connection.isReadOnly()).isFalse(); assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("sharing_admission_role"); assertThat(rows.getString(2)).isEqualTo("sharing_admission_role"); assertThat(rows.getString(3)).isEqualTo("off");
            }
        } finally { source.close(); }
    }
    @Test void admissionRoleSupportsAdmissionWriteAndRejectsUnrelatedWrites() throws Exception {
        try(Connection connection=BASE.admin()) {
            for(String table:List.of("sharing.share_admission","sharing.share_access_event")) {
                assertThat(strings(connection,"SELECT attname FROM pg_attribute WHERE attrelid='"+table+"'::regclass AND attnum>0 AND NOT attisdropped AND NOT has_column_privilege('sharing_admission_role',attrelid,attnum,'INSERT')")).isEmpty();
            }
            assertThat(strings(connection,"SELECT attname FROM pg_attribute WHERE attrelid='sharing.evidence_share_grant'::regclass AND attnum>0 AND NOT attisdropped AND has_column_privilege('sharing_admission_role',attrelid,attnum,'INSERT')")).isEmpty();
        }
        try(Connection connection=login()) { denied(connection,"INSERT INTO customer_iam.customer_account(id,display_name) VALUES ('00000000-0000-0000-0000-000000000017','Synthetic')"); }
    }
    @Test void v001ThroughV011BytesAndAclSnapshotsRemainUnchanged() throws Exception {
        assertThat(bytes()).isEqualTo(before); assertThat(bytes()).hasSize(11);
        assertThat(bytes()).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("V001__platform_schemas_roles_idempotency_outbox_audit.sql", "946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045"),
                Map.entry("V002__identity_organization_and_employee.sql", "534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5"),
                Map.entry("V003__subject_number_and_claim.sql", "1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1"),
                Map.entry("V004__catalog_order_and_payment.sql", "a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc"),
                Map.entry("V005__ivr_task_session_call.sql", "2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310"),
                Map.entry("V006__trial_entitlement_issuance_reservation_usage.sql", "830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475"),
                Map.entry("V007__activation_and_consent.sql", "bd919cf367f6713d26c7e6f1f7148e81d275c838e5e307cde13ebd014e17d659"),
                Map.entry("V008__evidence_media.sql", "2ea0c6fd2be335fddb566ce4c02fae29a0f76656538cef3592b75ca880468e7e"),
                Map.entry("V009__sharing.sql", "ac150576208a8ee3a44fdc111f00cc63ff03f6da8105fe01bade64e30c77b3ae"),
                Map.entry("V010__sharing_role_least_privilege.sql", "fadd30a3c058c8847d734f83b22af3a93b2c79ab92cba1b0d1a8a547e5f4a343"),
                Map.entry("V011__sharing_public_read_least_privilege.sql", "f5fddaf46a4a24bec427e5978364aa585f695b4ee9d03d5deb759eef0930e17b")));
        try(Connection connection=BASE.admin()) {
            var acl=strings(connection,"SELECT a.attrelid::text||':'||a.attnum||':'||acl.privilege_type FROM pg_attribute a CROSS JOIN LATERAL aclexplode(a.attacl) acl WHERE acl.grantee='sharing_public_read_role'::regrole ORDER BY 1");
            assertThat(acl).hasSize(24); connection.setAutoCommit(false); try(var statement=connection.createStatement()) { statement.execute("ALTER ROLE sharing_admission_role PASSWORD NULL"); statement.execute(migration()); }
            assertThat(strings(connection,"SELECT a.attrelid::text||':'||a.attnum||':'||acl.privilege_type FROM pg_attribute a CROSS JOIN LATERAL aclexplode(a.attacl) acl WHERE acl.grantee='sharing_public_read_role'::regrole ORDER BY 1")).isEqualTo(acl); connection.rollback();
        }
    }
    private static Connection login() throws Exception {
        try(Connection connection=BASE.admin()) { SharingQueryFixture.execute(connection,"ALTER ROLE sharing_admission_role PASSWORD '"+BASE.password+"'"); }
        return DriverManager.getConnection(BASE.url,"sharing_admission_role",BASE.password);
    }
    private static void denied(Connection connection,String sql) {
        assertThatThrownBy(() -> { try(var statement=connection.createStatement()) { statement.execute(sql); } })
                .isInstanceOfSatisfying(SQLException.class,failure -> assertThat(failure.getSQLState()).isEqualTo("42501"));
    }
    private static List<String> strings(Connection connection,String sql) throws Exception {
        var result=new ArrayList<String>(); try(var statement=connection.createStatement();var rows=statement.executeQuery(sql)) { while(rows.next()) result.add(rows.getString(1)); } return result;
    }
    private static String migration() throws Exception { return Files.readString(Path.of("src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql")); }
    private static Map<String,String> bytes() throws Exception {
        Map<String,String> result=new TreeMap<>(); try(var paths=Files.list(Path.of("src/main/resources/db/migration"))) {
            for(Path path:paths.filter(path -> path.getFileName().toString().matches("V00[1-9]__.*|V01[01]__.*")).toList())
                result.put(path.getFileName().toString(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(lfBytes(Files.readAllBytes(path)))));
        } return result;
    }
    private static byte[] lfBytes(byte[] source) {
        var result=new java.io.ByteArrayOutputStream(source.length);
        for(int index=0;index<source.length;index++) {
            if(source[index]=='\r' && index+1<source.length && source[index+1]=='\n') index++;
            result.write(source[index]);
        }
        return result.toByteArray();
    }
    @Test void v012AclSupportsMediaAuthorizationWithoutNewPrivilege() throws Exception {
        byte[] v012Before=Files.readAllBytes(Path.of("src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql"));
        BASE.reset();
        String aclBefore;
        String roleBefore;
        boolean passwordInitiallySet;
        try (Connection connection=BASE.admin()) { aclBefore=admissionAcl(connection); roleBefore=admissionRole(connection); passwordInitiallySet=admissionPasswordIsSet(connection); }
        String token="pa18-acl-token-"+UUID.randomUUID();
        String digest=sha256Hex(token);
        UUID grantId=UUID.randomUUID();
        UUID binding=UUID.randomUUID();
        try (Connection connection=BASE.admin()) {
            UUID subject;
            try (PreparedStatement statement=connection.prepareStatement("SELECT subject_id FROM evidence.evidence_record WHERE id=?")) {
                statement.setObject(1,BASE.evidence);
                try (ResultSet rows=statement.executeQuery()) { assertThat(rows.next()).isTrue(); subject=rows.getObject(1,UUID.class); }
            }
            SharingQueryFixture.execute(connection,"UPDATE subject_number.business_subject SET owner_account_id=NULL,owner_organization_id=?,revision=revision+1 WHERE id=?",BASE.org,subject);
            SharingQueryFixture.execute(connection,"INSERT INTO subject_number.caller_number_binding(id,subject_id,number_type,number_ciphertext,number_hmac,number_masked,verified_at) VALUES (?,?,'MOBILE','v1.synthetic.payload',?,'synthetic',clock_timestamp())",binding,subject,sha256Hex(binding.toString()));
            SharingQueryFixture.execute(connection,"INSERT INTO subject_number.subject_risk_state(id,caller_number_binding_id,risk_type,state,source,observed_at,cleared_at) VALUES (?,?,'CONFLICT','CLEARED','OPERATIONS',clock_timestamp(),clock_timestamp())",UUID.randomUUID(),binding);
            SharingQueryFixture.execute(connection,"""
                    INSERT INTO sharing.evidence_share_grant(id,share_id,capability_token_hash,evidence_id,
                        created_by_account_id,owner_organization_id,verification_mode,valid_from,expires_at,max_admissions,
                        content_snapshot,content_sha256,code_salt,code_hash,code_generation,status)
                    SELECT ?,?,?,?,?,?,'NONE',clock_timestamp()-interval '60 seconds',clock_timestamp()+interval '1 day',NULL,
                        jsonb_build_object('content',jsonb_build_object('evidenceId',?::text),
                            'verification',jsonb_build_object('mode','NONE'),
                            'creation',jsonb_build_object('creatorAccountId',?::text,'evidenceId',?::text,'ownerOrganizationId',?::text,
                                'scopeDigest',repeat('d',64),'creationRequestDigest',repeat('e',64),'creationFingerprint',repeat('f',64))),
                        repeat('a',64),NULL,NULL,0,'ACTIVE'
                    """,grantId,"pa18-acl",digest,BASE.evidence,BASE.account,BASE.org,BASE.evidence,BASE.account,BASE.evidence,BASE.org);
            try (PreparedStatement statement=connection.prepareStatement("SELECT id,capability_token_hash FROM sharing.evidence_share_grant WHERE share_id='pa18-acl'")) {
                try (ResultSet rows=statement.executeQuery()) {
                    assertThat(rows.next()).isTrue(); assertThat(rows.getObject(1,UUID.class)).isEqualTo(grantId); assertThat(rows.getString(2)).isEqualTo(digest); assertThat(rows.next()).isFalse();
                }
            }
            assertThatThrownBy(() -> SharingQueryFixture.execute(connection,"UPDATE sharing.evidence_share_grant SET capability_token_hash=? WHERE id=?",sha256Hex("pa18-acl-immutable"),grantId))
                    .isInstanceOfSatisfying(SQLException.class,failure -> assertThat(failure.getMessage()).contains("SHARE_GRANT_IMMUTABLE"));
        }
        setAdmissionPassword();
        var source=new DriverManagerDataSource(BASE.url,"sharing_admission_role",BASE.password);
        var manager=new DataSourceTransactionManager(source);
        var transactions=new TransactionTemplate(manager); transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var configuration=new DefaultConfiguration(); configuration.set(SQLDialect.POSTGRES);
        configuration.set(new DataSourceConnectionProvider(new TransactionAwareDataSourceProxy(source)));
        var repository=new JooqShareAdmissionRepository(DSL.using(configuration));
        var admissions=new DefaultShareAdmissionService(transactions,repository,bytes -> {
                    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
                    catch (java.security.NoSuchAlgorithmException unavailable) { throw new IllegalStateException(unavailable); }
                },
                request -> new ShareAdmissionPolicyProvider.ApprovedTicketPolicy(java.time.Duration.ofSeconds(120),java.time.Duration.ofSeconds(30)),
                request -> new ShareAdmissionCodeVerifier.Unavailable(), request -> new DesignatedMobileProofVerifier.Unavailable(),
                request -> new ShareMediaCredentialProvider.Issued("admission-credential",request.expiresAt()), request -> new ShareAdmissionTicketProvider.Written());
        var admissionOutcome=admissions.admit(new ShareAdmissionCapabilityToken(token),new ShareAdmissionRequest("pa18-acl-admission",new ShareAdmissionRequest.Verification.None()),Optional.empty());
        assertThat(admissionOutcome).isInstanceOf(ShareAdmissionService.AdmissionCommitted.class);
        ShareAdmissionView view=((ShareAdmissionService.AdmissionCommitted)admissionOutcome).view();
        assertThat(view.verificationMode()).isEqualTo("NONE");
        assertThat(view.verificationResult()).isEqualTo("NOT_REQUIRED");
        assertThat(view.mediaAuthorization()).isNotNull();
        UUID admissionId=view.admissionId();
        String admissionKey=admissions.admissionKey(grantId,"pa18-acl-admission");
        assertThat(admissionKey).hasSize(74).doesNotContain("pa18-acl-admission");
        JsonMapper mapper=JsonMapper.builder().build();
        try (Connection connection=DriverManager.getConnection(BASE.url,"sharing_admission_role",BASE.password)) {
            connection.setAutoCommit(false);
            DSLContext database=DSL.using(connection,SQLDialect.POSTGRES);
            JooqShareAdmissionRepository mediaRepository=new JooqShareAdmissionRepository(database);
            assertThat(mediaRepository.findGrantByTokenDigest(digest)).isPresent();
            assertThat(mediaRepository.lockGrant(grantId)).isPresent();
            assertThat(mediaRepository.findAdmission(grantId,admissionKey)).isPresent();
            assertThat(mediaRepository.findAdmissionById(grantId,admissionId)).isPresent();
            assertThat(mediaRepository.findAdmissionWitness(grantId,admissionId)).isPresent();
            assertThat(mediaRepository.findAccessEventsByRequestKeyDigest(grantId,admissionId,"not-present")).isEmpty();
            JsonNode payload=mapper.readTree("{\"purpose\":\"PLAYBACK\",\"requestKeyDigest\":\""+"b".repeat(64)+"\"}");
            var appended=mediaRepository.insertAccessEvent(new ShareAdmissionRepository.AccessEventInsert(grantId,admissionId,
                    "share-access:pa18-acl", "PLAYBACK_AUTHORIZED", "SUCCESS", Instant.parse("2026-01-02T03:04:05Z"), payload, "b".repeat(64)));
            assertThat(appended.status()).isEqualTo(ShareAdmissionRepository.EventWriteStatus.INSERTED);
            assertThat(appended.first().eventType()).isEqualTo("PLAYBACK_AUTHORIZED");
            connection.rollback();
            deniedAndRollback(connection,"SELECT display_name FROM customer_iam.customer_account");
            deniedAndRollback(connection,"SELECT object_key FROM evidence.media_object");
            deniedAndRollback(connection,"SELECT source_type FROM evidence.evidence_record");
            deniedAndRollback(connection,"UPDATE sharing.evidence_share_grant SET code_hash=code_hash");
        } finally { if (passwordInitiallySet) setAdmissionPassword(); else clearAdmissionPassword(); }
        try (Connection connection=BASE.admin()) {
            assertThat(admissionAcl(connection)).isEqualTo(aclBefore);
            assertThat(admissionRole(connection)).isEqualTo(roleBefore);
        }
        assertThat(bytes()).isEqualTo(before);
        assertThat(Files.readAllBytes(Path.of("src/main/resources/db/migration/V012__sharing_admission_least_privilege.sql"))).isEqualTo(v012Before);
        try (Connection connection=BASE.admin()) { assertThat(strings(connection,"SELECT event_id FROM sharing.share_access_event WHERE event_id='share-access:pa18-acl'")).isEmpty(); }
    }

    private static String sha256Hex(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    private static boolean admissionPasswordIsSet(Connection connection) throws Exception {
        return Boolean.parseBoolean(strings(connection,"SELECT (rolpassword IS NOT NULL)::text FROM pg_authid WHERE rolname='sharing_admission_role'").getFirst());
    }
    private static void setAdmissionPassword() throws Exception {
        try (Connection connection=BASE.admin()) { SharingQueryFixture.execute(connection,"ALTER ROLE sharing_admission_role PASSWORD '"+BASE.password+"'"); }
    }
    private static void clearAdmissionPassword() throws Exception {
        try (Connection connection=BASE.admin()) { SharingQueryFixture.execute(connection,"ALTER ROLE sharing_admission_role PASSWORD NULL"); }
    }
    private static void deniedAndRollback(Connection connection,String sql) throws Exception {
        denied(connection,sql); connection.rollback();
    }
    private static String admissionAcl(Connection connection) throws Exception {
        return String.join("\n",List.of(
                String.join("\n",strings(connection,"SELECT 'schema:'||nspname||':'||has_schema_privilege('sharing_admission_role',oid,'USAGE,CREATE') FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname <> 'information_schema' ORDER BY 1")),
                String.join("\n",strings(connection,"SELECT 'column:'||a.attrelid::regclass||':'||a.attnum||':'||acl.privilege_type FROM pg_attribute a CROSS JOIN LATERAL aclexplode(a.attacl) acl WHERE acl.grantee='sharing_admission_role'::regrole ORDER BY 1")),
                String.join("\n",strings(connection,"SELECT 'table:'||c.oid||':'||acl.privilege_type FROM pg_class c CROSS JOIN LATERAL aclexplode(c.relacl) acl WHERE acl.grantee='sharing_admission_role'::regrole ORDER BY 1"))));
    }
    private static String admissionRole(Connection connection) throws Exception {
        return strings(connection,"SELECT concat_ws('|',rolcanlogin,rolsuper,rolcreatedb,rolcreaterole,rolinherit,rolreplication,rolbypassrls,rolconnlimit,rolvaliduntil IS NULL,rolconfig IS NULL,(SELECT rolpassword IS NOT NULL FROM pg_authid auth_state WHERE auth_state.oid=role_state.oid)) FROM pg_roles role_state WHERE rolname='sharing_admission_role'").toString();
    }

}
