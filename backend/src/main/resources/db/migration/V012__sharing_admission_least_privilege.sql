DO $preconditions$
DECLARE
    admission_role oid;
BEGIN
    IF current_setting('server_version_num')::int NOT BETWEEN 160000 AND 169999
       OR NOT (SELECT rolsuper FROM pg_roles WHERE rolname = current_user) THEN
        RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V012 incompatible migration identity or PostgreSQL version';
    END IF;
    SELECT oid INTO admission_role FROM pg_roles WHERE rolname = 'sharing_admission_role';
    IF admission_role IS NULL THEN
        CREATE ROLE sharing_admission_role LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS PASSWORD NULL;
    ELSIF NOT EXISTS (SELECT 1 FROM pg_roles WHERE oid=admission_role AND rolcanlogin AND NOT rolsuper AND NOT rolcreatedb
        AND NOT rolcreaterole AND NOT rolinherit AND NOT rolreplication AND NOT rolbypassrls
        AND rolconnlimit=-1 AND rolvaliduntil IS NULL AND rolconfig IS NULL) THEN
        RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V012 incompatible admission role';
    END IF;
    IF (SELECT rolpassword IS NOT NULL FROM pg_authid WHERE oid='sharing_admission_role'::regrole) THEN
        RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V012 incompatible admission role password state';
    END IF;
END
$preconditions$;

SET LOCAL ROLE sharing_owner;
GRANT USAGE ON SCHEMA sharing TO sharing_admission_role;
GRANT SELECT (id, share_id, capability_token_hash, evidence_id, created_by_account_id, owner_organization_id, verification_mode, code_salt, code_hash, code_generation, valid_from, expires_at, max_admissions, admissions_used, download_allowed, content_snapshot, content_sha256, status, revocation_generation, revision) ON sharing.evidence_share_grant TO sharing_admission_role;
GRANT SELECT (id, share_grant_id, admission_key, admission_no, verification_mode, verification_result, admitted_at) ON sharing.share_admission TO sharing_admission_role;
GRANT SELECT (id, share_grant_id, share_admission_id, event_domain, event_id, event_type, result, occurred_at, risk_digest, payload_redacted, payload_sha256) ON sharing.share_access_event TO sharing_admission_role;
GRANT INSERT (id, share_grant_id, admission_key, admission_no, verification_mode, verification_result, admitted_at) ON sharing.share_admission TO sharing_admission_role;
GRANT INSERT (id, share_grant_id, share_admission_id, event_domain, event_id, event_type, result, occurred_at, risk_digest, payload_redacted, payload_sha256) ON sharing.share_access_event TO sharing_admission_role;
GRANT UPDATE (status, admissions_used, revocation_generation, revision) ON sharing.evidence_share_grant TO sharing_admission_role;
RESET ROLE;

SET LOCAL ROLE customer_iam_owner;
GRANT USAGE ON SCHEMA customer_iam TO sharing_admission_role;
GRANT SELECT (id, status) ON customer_iam.customer_account TO sharing_admission_role;
RESET ROLE;

SET LOCAL ROLE organization_access_owner;
GRANT USAGE ON SCHEMA organization_access TO sharing_admission_role;
GRANT SELECT (id, status) ON organization_access.organization TO sharing_admission_role;
GRANT SELECT (id, organization_id, account_id, role, status, revision) ON organization_access.organization_membership TO sharing_admission_role;
RESET ROLE;

SET LOCAL ROLE evidence_owner;
GRANT USAGE ON SCHEMA evidence TO sharing_admission_role;
GRANT SELECT (id, subject_id, status, identity_at_capture) ON evidence.evidence_record TO sharing_admission_role;
RESET ROLE;

SET LOCAL ROLE subject_number_owner;
GRANT USAGE ON SCHEMA subject_number TO sharing_admission_role;
GRANT SELECT (id, owner_account_id, owner_organization_id, revision) ON subject_number.business_subject TO sharing_admission_role;
GRANT SELECT (id, subject_id, number_type, number_hmac, status, verified_at, revision) ON subject_number.caller_number_binding TO sharing_admission_role;
GRANT SELECT (id, caller_number_binding_id, risk_type, state, observed_at, cleared_at, revision) ON subject_number.subject_risk_state TO sharing_admission_role;
RESET ROLE;

DO $postconditions$
DECLARE
    admission_role oid := 'sharing_admission_role'::regrole;
    target record;
    attribute record;
    expected_select boolean;
    expected_insert boolean;
    expected_update boolean;
BEGIN
    IF EXISTS (SELECT 1 FROM pg_auth_members WHERE member=admission_role OR roleid=admission_role)
       OR EXISTS (SELECT 1 FROM pg_db_role_setting WHERE setrole=admission_role)
       OR EXISTS (SELECT 1 FROM pg_shdepend WHERE refclassid='pg_authid'::regclass AND refobjid=admission_role AND deptype <> 'a')
       OR EXISTS (SELECT 1 FROM pg_default_acl CROSS JOIN LATERAL aclexplode(defaclacl) acl WHERE acl.grantee=admission_role OR acl.grantor=admission_role)
       OR has_database_privilege(admission_role,current_database(),'CREATE,TEMPORARY') THEN
        RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V012 incompatible role topology';
    END IF;
    FOR target IN SELECT oid,nspname,nspowner,nspacl FROM pg_namespace WHERE nspname <> 'information_schema' AND nspname !~ '^pg_' LOOP
        IF has_schema_privilege(admission_role,target.oid,'USAGE') IS DISTINCT FROM
            (target.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number'))
           OR has_schema_privilege(admission_role,target.oid,'CREATE')
           OR EXISTS (SELECT 1 FROM aclexplode(target.nspacl) acl WHERE acl.grantee=0 OR acl.grantee=admission_role AND (acl.is_grantable OR acl.grantor <> target.nspowner)) THEN
            RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V012 incompatible schema privileges';
        END IF;
    END LOOP;
    FOR target IN SELECT relation.oid,relation.relkind,relation.relowner,relation.relacl,namespace.nspname||'.'||relation.relname AS name
        FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
        WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_' AND relation.relkind IN ('r','p','v','m','f','S') LOOP
        IF target.relkind='S' THEN
            IF has_sequence_privilege(admission_role,target.oid,'USAGE,SELECT,UPDATE') THEN
                RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V012 incompatible sequence privileges';
            END IF;
        ELSE
            IF has_table_privilege(admission_role,target.oid,'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')
               OR EXISTS (SELECT 1 FROM aclexplode(target.relacl) acl WHERE acl.grantee IN (0,admission_role)) THEN
                RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V012 incompatible table privileges';
            END IF;
            FOR attribute IN SELECT attnum,attname,attacl FROM pg_attribute WHERE attrelid=target.oid AND attnum>0 AND NOT attisdropped LOOP
                expected_select := (target.name='sharing.evidence_share_grant' AND attribute.attname IN ('id','share_id','capability_token_hash','evidence_id','created_by_account_id','owner_organization_id','verification_mode','code_salt','code_hash','code_generation','valid_from','expires_at','max_admissions','admissions_used','download_allowed','content_snapshot','content_sha256','status','revocation_generation','revision')) OR (target.name='sharing.share_admission' AND attribute.attname IN ('id','share_grant_id','admission_key','admission_no','verification_mode','verification_result','admitted_at')) OR (target.name='sharing.share_access_event' AND attribute.attname IN ('id','share_grant_id','share_admission_id','event_domain','event_id','event_type','result','occurred_at','risk_digest','payload_redacted','payload_sha256')) OR (target.name='customer_iam.customer_account' AND attribute.attname IN ('id','status')) OR (target.name='organization_access.organization' AND attribute.attname IN ('id','status')) OR (target.name='organization_access.organization_membership' AND attribute.attname IN ('id','organization_id','account_id','role','status','revision')) OR (target.name='evidence.evidence_record' AND attribute.attname IN ('id','subject_id','status','identity_at_capture')) OR (target.name='subject_number.business_subject' AND attribute.attname IN ('id','owner_account_id','owner_organization_id','revision')) OR (target.name='subject_number.caller_number_binding' AND attribute.attname IN ('id','subject_id','number_type','number_hmac','status','verified_at','revision')) OR (target.name='subject_number.subject_risk_state' AND attribute.attname IN ('id','caller_number_binding_id','risk_type','state','observed_at','cleared_at','revision'));
                expected_insert := (target.name='sharing.share_admission' AND attribute.attname IN ('id','share_grant_id','admission_key','admission_no','verification_mode','verification_result','admitted_at')) OR (target.name='sharing.share_access_event' AND attribute.attname IN ('id','share_grant_id','share_admission_id','event_domain','event_id','event_type','result','occurred_at','risk_digest','payload_redacted','payload_sha256'));
                expected_update := (target.name='sharing.evidence_share_grant' AND attribute.attname IN ('status','admissions_used','revocation_generation','revision'));
                IF has_column_privilege(admission_role,target.oid,attribute.attnum,'SELECT') IS DISTINCT FROM expected_select
                   OR has_column_privilege(admission_role,target.oid,attribute.attnum,'INSERT') IS DISTINCT FROM expected_insert
                   OR has_column_privilege(admission_role,target.oid,attribute.attnum,'UPDATE') IS DISTINCT FROM expected_update
                   OR has_column_privilege(admission_role,target.oid,attribute.attnum,'REFERENCES')
                   OR EXISTS (SELECT 1 FROM aclexplode(attribute.attacl) acl WHERE acl.grantee=0 OR acl.grantee=admission_role AND (acl.is_grantable OR acl.grantor <> target.relowner)) THEN
                    RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V012 incompatible column privileges';
                END IF;
            END LOOP;
        END IF;
    END LOOP;
    IF EXISTS (SELECT 1 FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid=routine.pronamespace
        WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_'
        AND (has_function_privilege(admission_role,routine.oid,'EXECUTE')
            OR EXISTS (SELECT 1 FROM aclexplode(coalesce(routine.proacl,acldefault('f',routine.proowner))) acl WHERE acl.grantee=0))) THEN
        RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V012 incompatible function privileges';
    END IF;
END
$postconditions$;
