DO $migration$
DECLARE
    required_owner text;
BEGIN
    IF NOT (SELECT rolsuper FROM pg_roles WHERE rolname = current_user) THEN
        RAISE EXCEPTION 'V010 requires a superuser-equivalent migration identity';
    END IF;
    FOREACH required_owner IN ARRAY ARRAY[
        'sharing_owner', 'evidence_owner', 'customer_iam_owner',
        'organization_access_owner', 'subject_number_owner',
        'platform_reliability_owner', 'audit_owner'
    ] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = required_owner) THEN
            RAISE EXCEPTION 'V010 required owner role % is missing', required_owner;
        END IF;
    END LOOP;
    IF to_regclass('sharing.evidence_share_grant') IS NULL
        OR to_regclass('sharing.share_admission') IS NULL
        OR to_regclass('sharing.share_access_event') IS NULL THEN
        RAISE EXCEPTION 'V010 sharing objects are missing';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sharing_command_role') THEN
        IF (SELECT (rolcanlogin, rolsuper, rolcreatedb, rolcreaterole, rolinherit,
                    rolreplication, rolbypassrls, rolconnlimit, rolpassword IS NULL)
            FROM pg_authid WHERE rolname = 'sharing_command_role')
            IS DISTINCT FROM (false, false, false, false, false, false, false, -1, true) THEN
            RAISE EXCEPTION 'sharing_command_role exists with incompatible attributes';
        END IF;
    ELSE
        EXECUTE 'CREATE ROLE sharing_command_role NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT';
    END IF;
    IF (SELECT (rolcanlogin, rolsuper, rolcreatedb, rolcreaterole, rolinherit,
                rolreplication, rolbypassrls, rolconnlimit, rolpassword IS NULL)
        FROM pg_authid WHERE rolname = 'sharing_owner')
        IS DISTINCT FROM (false, false, false, false, true, false, false, -1, true)
       OR (SELECT (rolcanlogin, rolsuper, rolcreatedb, rolcreaterole, rolinherit,
                   rolreplication, rolbypassrls, rolconnlimit, rolpassword IS NULL)
           FROM pg_authid WHERE rolname = 'sharing_role')
        IS DISTINCT FROM (false, false, false, false, true, false, false, -1, true) THEN
        RAISE EXCEPTION 'sharing_owner or sharing_role exists with incompatible attributes';
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_auth_members m
        WHERE m.member IN ('sharing_owner'::regrole, 'sharing_role'::regrole, 'sharing_command_role'::regrole)
           OR m.roleid IN ('sharing_owner'::regrole, 'sharing_command_role'::regrole)
           OR (pg_has_role(m.member, 'sharing_role'::regrole, 'MEMBER')
               AND pg_has_role(m.roleid, 'sharing_command_role'::regrole, 'MEMBER'))
    ) THEN
        RAISE EXCEPTION 'sharing roles have incompatible membership state';
    END IF;
END
$migration$;

DO $reset_acl$
DECLARE
    target_schema text;
    target_relation record;
    target_column record;
    target_function record;
    target_sequence record;
BEGIN
    FOREACH target_schema IN ARRAY ARRAY['sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering'] LOOP
        EXECUTE format('REVOKE ALL PRIVILEGES ON SCHEMA %I FROM PUBLIC, sharing_role, sharing_command_role', target_schema);
    END LOOP;
    FOR target_relation IN
        SELECT n.nspname, c.relname, c.oid
        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering')
          AND c.relkind IN ('r','p','v','m','f')
    LOOP
        EXECUTE format('REVOKE ALL PRIVILEGES ON TABLE %I.%I FROM PUBLIC, sharing_role, sharing_command_role', target_relation.nspname, target_relation.relname);
        FOR target_column IN SELECT attname FROM pg_attribute WHERE attrelid = target_relation.oid AND attnum > 0 AND NOT attisdropped LOOP
            EXECUTE format('REVOKE SELECT (%1$I), INSERT (%1$I), UPDATE (%1$I), REFERENCES (%1$I) ON TABLE %2$I.%3$I FROM PUBLIC, sharing_role, sharing_command_role', target_column.attname, target_relation.nspname, target_relation.relname);
        END LOOP;
    END LOOP;
    FOR target_sequence IN
        SELECT n.nspname, c.relname FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
        WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering')
          AND c.relkind='S'
    LOOP
        EXECUTE format('REVOKE ALL PRIVILEGES ON SEQUENCE %I.%I FROM PUBLIC, sharing_role, sharing_command_role', target_sequence.nspname, target_sequence.relname);
    END LOOP;
    FOR target_function IN
        SELECT n.nspname, p.proname, pg_get_function_identity_arguments(p.oid) AS arguments
        FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
        WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering')
    LOOP
        EXECUTE format('REVOKE ALL PRIVILEGES ON ROUTINE %I.%I(%s) FROM PUBLIC, sharing_role, sharing_command_role', target_function.nspname, target_function.proname, target_function.arguments);
    END LOOP;
END
$reset_acl$;

REVOKE ALL ON SCHEMA sharing FROM PUBLIC;
REVOKE USAGE ON SCHEMA sharing FROM sharing_role;
REVOKE USAGE ON SCHEMA evidence FROM sharing_role;

SET LOCAL ROLE sharing_owner;
REVOKE ALL PRIVILEGES ON TABLE sharing.evidence_share_grant, sharing.share_admission, sharing.share_access_event FROM sharing_role, sharing_command_role;
REVOKE SELECT (id, share_id, capability_token_hash, evidence_id, created_by_account_id, owner_organization_id, verification_mode, code_salt, code_hash, code_generation, valid_from, expires_at, max_admissions, admissions_used, download_allowed, content_snapshot, content_sha256, status, revocation_generation, revision), INSERT (id, share_id, capability_token_hash, evidence_id, created_by_account_id, owner_organization_id, verification_mode, code_salt, code_hash, code_generation, valid_from, expires_at, max_admissions, admissions_used, download_allowed, content_snapshot, content_sha256, status, revocation_generation, revision), UPDATE (id, share_id, capability_token_hash, evidence_id, created_by_account_id, owner_organization_id, verification_mode, code_salt, code_hash, code_generation, valid_from, expires_at, max_admissions, admissions_used, download_allowed, content_snapshot, content_sha256, status, revocation_generation, revision) ON sharing.evidence_share_grant FROM sharing_role, sharing_command_role;
REVOKE SELECT (id, share_grant_id, admission_key, admission_no, verification_mode, verification_result, admitted_at), INSERT (id, share_grant_id, admission_key, admission_no, verification_mode, verification_result, admitted_at), UPDATE (id, share_grant_id, admission_key, admission_no, verification_mode, verification_result, admitted_at) ON sharing.share_admission FROM sharing_role, sharing_command_role;
REVOKE SELECT (id, share_grant_id, share_admission_id, event_domain, event_id, event_type, result, occurred_at, risk_digest, payload_redacted, payload_sha256), INSERT (id, share_grant_id, share_admission_id, event_domain, event_id, event_type, result, occurred_at, risk_digest, payload_redacted, payload_sha256), UPDATE (id, share_grant_id, share_admission_id, event_domain, event_id, event_type, result, occurred_at, risk_digest, payload_redacted, payload_sha256) ON sharing.share_access_event FROM sharing_role, sharing_command_role;
REVOKE ALL ON FUNCTION sharing.guard_share_grant(), sharing.guard_share_admission(), sharing.reject_row_mutation() FROM PUBLIC, sharing_role, sharing_command_role;
REVOKE CREATE ON SCHEMA sharing FROM sharing_role, sharing_command_role;
GRANT USAGE ON SCHEMA sharing TO sharing_role, sharing_command_role;
GRANT SELECT, INSERT ON sharing.evidence_share_grant, sharing.share_admission, sharing.share_access_event TO sharing_command_role;
GRANT UPDATE (status, admissions_used, revocation_generation, revision, expires_at, code_salt, code_hash, code_generation) ON sharing.evidence_share_grant TO sharing_command_role;
GRANT SELECT (share_id, evidence_id, verification_mode, status, valid_from, expires_at, max_admissions, admissions_used, download_allowed, code_generation, revocation_generation, created_by_account_id, owner_organization_id) ON sharing.evidence_share_grant TO sharing_role;
RESET ROLE;

SET LOCAL ROLE evidence_owner;
REVOKE ALL ON SCHEMA evidence FROM PUBLIC, sharing_role, sharing_command_role;
REVOKE SELECT (id, subject_id, status, source_type, source_ref_id, call_id, session_id, reservation_id, identity_at_capture, created_at) ON evidence.evidence_record FROM sharing_role, sharing_command_role;
REVOKE SELECT (evidence_id, media_object_id, artifact_role) ON evidence.evidence_artifact FROM sharing_role, sharing_command_role;
GRANT USAGE ON SCHEMA evidence TO sharing_command_role;
GRANT SELECT (id, subject_id, status, source_type, source_ref_id, call_id, session_id, reservation_id, identity_at_capture, created_at) ON evidence.evidence_record TO sharing_command_role;
GRANT SELECT (evidence_id, media_object_id, artifact_role) ON evidence.evidence_artifact TO sharing_command_role;
RESET ROLE;

SET LOCAL ROLE customer_iam_owner;
REVOKE ALL ON SCHEMA customer_iam FROM PUBLIC, sharing_role, sharing_command_role;
REVOKE SELECT (id, status) ON customer_iam.customer_account FROM sharing_role, sharing_command_role;
GRANT USAGE ON SCHEMA customer_iam TO sharing_role, sharing_command_role;
GRANT SELECT (id, status) ON customer_iam.customer_account TO sharing_role, sharing_command_role;
RESET ROLE;

SET LOCAL ROLE organization_access_owner;
REVOKE ALL ON SCHEMA organization_access FROM PUBLIC, sharing_role, sharing_command_role;
REVOKE SELECT (id, status) ON organization_access.organization FROM sharing_role, sharing_command_role;
REVOKE SELECT (organization_id, account_id, role, status) ON organization_access.organization_membership FROM sharing_role, sharing_command_role;
GRANT USAGE ON SCHEMA organization_access TO sharing_role, sharing_command_role;
GRANT SELECT (id, status) ON organization_access.organization TO sharing_role, sharing_command_role;
GRANT SELECT (organization_id, account_id, status) ON organization_access.organization_membership TO sharing_role;
GRANT SELECT (organization_id, account_id, role, status) ON organization_access.organization_membership TO sharing_command_role;
RESET ROLE;

SET LOCAL ROLE subject_number_owner;
REVOKE ALL ON SCHEMA subject_number FROM PUBLIC, sharing_role, sharing_command_role;
REVOKE SELECT (id, owner_account_id, owner_organization_id, revision) ON subject_number.business_subject FROM sharing_role, sharing_command_role;
REVOKE SELECT (id, subject_id, status, verified_at, revision) ON subject_number.caller_number_binding FROM sharing_role, sharing_command_role;
REVOKE SELECT (caller_number_binding_id, risk_type, state, revision) ON subject_number.subject_risk_state FROM sharing_role, sharing_command_role;
GRANT USAGE ON SCHEMA subject_number TO sharing_command_role;
GRANT SELECT (id, owner_account_id, owner_organization_id, revision) ON subject_number.business_subject TO sharing_command_role;
GRANT SELECT (id, subject_id, status, verified_at, revision) ON subject_number.caller_number_binding TO sharing_command_role;
GRANT SELECT (caller_number_binding_id, risk_type, state, revision) ON subject_number.subject_risk_state TO sharing_command_role;
RESET ROLE;

SET LOCAL ROLE platform_reliability_owner;
REVOKE ALL ON SCHEMA platform FROM PUBLIC, sharing_role, sharing_command_role;
REVOKE EXECUTE ON FUNCTION platform.outbox_append(text,text,text,uuid,text,jsonb,text,text) FROM PUBLIC, sharing_role, sharing_command_role;
GRANT USAGE ON SCHEMA platform TO sharing_command_role;
GRANT EXECUTE ON FUNCTION platform.outbox_append(text,text,text,uuid,text,jsonb,text,text) TO sharing_command_role;
RESET ROLE;

SET LOCAL ROLE audit_owner;
REVOKE ALL ON SCHEMA audit FROM PUBLIC, sharing_role, sharing_command_role;
REVOKE EXECUTE ON FUNCTION audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text) FROM PUBLIC, sharing_role, sharing_command_role;
GRANT USAGE ON SCHEMA audit TO sharing_command_role;
GRANT EXECUTE ON FUNCTION audit.append_fact(timestamptz,text,uuid,text,text,uuid,text,jsonb,jsonb,text,text) TO sharing_command_role;
RESET ROLE;

DO $postconditions$
DECLARE query_usage integer; command_usage integer; query_rel_acl integer; command_rel_acl integer;
        query_column_select integer; command_column_select integer; command_column_update integer;
BEGIN
    SELECT count(*) INTO query_usage FROM pg_namespace WHERE nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND has_schema_privilege('sharing_role', oid, 'USAGE');
    SELECT count(*) INTO command_usage FROM pg_namespace WHERE nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND has_schema_privilege('sharing_command_role', oid, 'USAGE');
    IF query_usage <> 3 OR command_usage <> 7 OR EXISTS (SELECT 1 FROM pg_namespace WHERE nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND (has_schema_privilege('sharing_role', oid, 'CREATE') OR has_schema_privilege('sharing_command_role', oid, 'CREATE'))) THEN RAISE EXCEPTION 'V010 schema ACL postcondition failed'; END IF;
    IF EXISTS (SELECT 1 FROM pg_auth_members m WHERE m.member IN ('sharing_owner'::regrole,'sharing_role'::regrole,'sharing_command_role'::regrole) OR m.roleid IN ('sharing_owner'::regrole,'sharing_command_role'::regrole))
       OR pg_has_role('sharing_role','sharing_command_role','MEMBER') OR pg_has_role('sharing_command_role','sharing_role','MEMBER') THEN RAISE EXCEPTION 'V010 role membership postcondition failed'; END IF;
    IF (SELECT (rolcanlogin, rolsuper, rolcreatedb, rolcreaterole, rolinherit, rolreplication, rolbypassrls, rolconnlimit, rolpassword IS NULL) FROM pg_authid WHERE rolname='sharing_owner') IS DISTINCT FROM (false,false,false,false,true,false,false,-1,true)
       OR (SELECT (rolcanlogin, rolsuper, rolcreatedb, rolcreaterole, rolinherit, rolreplication, rolbypassrls, rolconnlimit, rolpassword IS NULL) FROM pg_authid WHERE rolname='sharing_role') IS DISTINCT FROM (false,false,false,false,true,false,false,-1,true) THEN RAISE EXCEPTION 'V010 owner/query role attribute postcondition failed'; END IF;
    IF (SELECT (rolcanlogin, rolsuper, rolcreatedb, rolcreaterole, rolinherit, rolreplication, rolbypassrls, rolconnlimit, rolpassword IS NULL) FROM pg_authid WHERE rolname = 'sharing_command_role') IS DISTINCT FROM (false,false,false,false,false,false,false,-1,true) THEN RAISE EXCEPTION 'V010 command role attribute postcondition failed'; END IF;
    SELECT count(*) INTO query_rel_acl FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(c.relacl,acldefault('r',c.relowner))) a WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND c.relkind IN ('r','p','v','m','f') AND a.grantee='sharing_role'::regrole;
    SELECT count(*) INTO command_rel_acl FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(c.relacl,acldefault('r',c.relowner))) a WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND c.relkind IN ('r','p','v','m','f') AND a.grantee='sharing_command_role'::regrole;
    SELECT count(*) INTO query_column_select FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(a.attacl,acldefault('c',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND a.attnum>0 AND NOT a.attisdropped AND x.grantee='sharing_role'::regrole AND x.privilege_type='SELECT';
    SELECT count(*) INTO command_column_select FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(a.attacl,acldefault('c',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND a.attnum>0 AND NOT a.attisdropped AND x.grantee='sharing_command_role'::regrole AND x.privilege_type='SELECT';
    SELECT count(*) INTO command_column_update FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(a.attacl,acldefault('c',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND a.attnum>0 AND NOT a.attisdropped AND x.grantee='sharing_command_role'::regrole AND x.privilege_type='UPDATE';
    IF query_rel_acl<>0 OR command_rel_acl<>6 OR query_column_select<>20 OR command_column_select<>34 OR command_column_update<>8 THEN RAISE EXCEPTION 'V010 exact relation/column ACL postcondition failed'; END IF;
    IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(c.relacl,acldefault('r',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND c.relkind IN ('r','p','v','m','f') AND x.grantee='sharing_command_role'::regrole AND (n.nspname<>'sharing' OR c.relname NOT IN ('evidence_share_grant','share_admission','share_access_event') OR x.privilege_type NOT IN ('SELECT','INSERT')))
       OR (SELECT count(DISTINCT c.oid) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(c.relacl,acldefault('r',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND c.relkind IN ('r','p','v','m','f') AND x.grantee='sharing_command_role'::regrole)<>3 THEN RAISE EXCEPTION 'V010 exact command relation allow-list postcondition failed'; END IF;
    IF EXISTS (SELECT 1 FROM pg_namespace n CROSS JOIN LATERAL aclexplode(coalesce(n.nspacl,acldefault('n',n.nspowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND x.grantee IN ('sharing_role'::regrole,'sharing_command_role'::regrole) AND (x.grantor<>n.nspowner OR x.is_grantable))
       OR EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(c.relacl,acldefault('r',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND c.relkind IN ('r','p','v','m','f') AND x.grantee IN ('sharing_role'::regrole,'sharing_command_role'::regrole) AND (x.grantor<>c.relowner OR x.is_grantable))
       OR EXISTS (SELECT 1 FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(a.attacl,acldefault('c',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND a.attnum>0 AND NOT a.attisdropped AND x.grantee IN ('sharing_role'::regrole,'sharing_command_role'::regrole) AND (x.grantor<>c.relowner OR x.is_grantable)) THEN RAISE EXCEPTION 'V010 grantor/grant-option postcondition failed'; END IF;
    IF EXISTS (SELECT 1 FROM pg_namespace n CROSS JOIN LATERAL aclexplode(coalesce(n.nspacl,acldefault('n',n.nspowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND x.grantee=0)
       OR EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(c.relacl,acldefault('r',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND c.relkind IN ('r','p','v','m','f') AND x.grantee=0)
       OR EXISTS (SELECT 1 FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(a.attacl,acldefault('c',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND a.attnum>0 AND NOT a.attisdropped AND x.grantee=0) THEN RAISE EXCEPTION 'V010 PUBLIC ACL postcondition failed'; END IF;
    IF EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace CROSS JOIN LATERAL aclexplode(coalesce(p.proacl,acldefault('f',p.proowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND x.grantee=0) THEN RAISE EXCEPTION 'V010 PUBLIC procedure ACL postcondition failed'; END IF;
    IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(c.relacl,acldefault('S',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND c.relkind='S' AND (x.grantee=0 OR (x.grantee IN ('sharing_role'::regrole,'sharing_command_role'::regrole) AND (x.grantor<>c.relowner OR x.is_grantable)))) THEN RAISE EXCEPTION 'V010 sequence ACL postcondition failed'; END IF;
    IF EXISTS (SELECT 1 FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL aclexplode(coalesce(a.attacl,acldefault('c',c.relowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND a.attnum>0 AND NOT a.attisdropped AND ((x.grantee='sharing_role'::regrole AND x.privilege_type<>'SELECT') OR (x.grantee='sharing_command_role'::regrole AND x.privilege_type NOT IN ('SELECT','UPDATE')))) THEN RAISE EXCEPTION 'V010 unexpected column privilege postcondition failed'; END IF;
    IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND c.relkind IN ('r','p','v','m','f','S') AND (has_table_privilege('sharing_role',c.oid,'INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER') OR has_table_privilege('sharing_command_role',c.oid,'DELETE,TRUNCATE,REFERENCES,TRIGGER'))) OR EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE c.relkind='S' AND n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND (has_sequence_privilege('sharing_role',c.oid,'USAGE,SELECT,UPDATE') OR has_sequence_privilege('sharing_command_role',c.oid,'USAGE,SELECT,UPDATE'))) THEN RAISE EXCEPTION 'V010 relation or sequence ACL postcondition failed'; END IF;
    IF EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND has_function_privilege('sharing_role',p.oid,'EXECUTE')) OR (SELECT count(*) FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND has_function_privilege('sharing_command_role',p.oid,'EXECUTE')) <> 2 THEN RAISE EXCEPTION 'V010 function ACL postcondition failed'; END IF;
    IF EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace CROSS JOIN LATERAL aclexplode(coalesce(p.proacl,acldefault('f',p.proowner))) x WHERE n.nspname IN ('sharing','evidence','customer_iam','organization_access','subject_number','platform','audit','ivr','entitlement_metering') AND x.grantee IN ('sharing_role'::regrole,'sharing_command_role'::regrole) AND (x.grantor<>p.proowner OR x.is_grantable)) THEN RAISE EXCEPTION 'V010 function grantor postcondition failed'; END IF;
END
$postconditions$;
