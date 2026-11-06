DO $preconditions$
DECLARE
    public_role oid;
    target record;
BEGIN
    IF current_setting('server_version_num')::int NOT BETWEEN 160000 AND 169999
       OR NOT (SELECT rolsuper FROM pg_roles WHERE rolname = current_user) THEN
        RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible migration identity or PostgreSQL version';
    END IF;
    FOR target IN
        SELECT * FROM (VALUES
            ('sharing', 'evidence_share_grant', 'sharing_owner'),
            ('sharing', 'share_admission', 'sharing_owner'),
            ('sharing', 'share_access_event', 'sharing_owner'),
            ('evidence', 'evidence_record', 'evidence_owner'),
            ('evidence', 'evidence_artifact', 'evidence_owner'),
            ('evidence', 'media_object', 'evidence_owner'),
            ('evidence', 'recording', 'evidence_owner')) AS targets(schema_name, table_name, owner_name)
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
            JOIN pg_roles owner_role ON owner_role.oid = relation.relowner
            WHERE namespace.nspname = target.schema_name AND relation.relname = target.table_name
              AND relation.relkind = 'r' AND owner_role.rolname = target.owner_name
              AND namespace.nspowner = owner_role.oid
        ) THEN
            RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible application ownership or structure';
        END IF;
    END LOOP;
    FOR target IN
        SELECT targets.table_name, unnest(targets.columns) AS column_name FROM (VALUES
            ('sharing.evidence_share_grant', ARRAY['capability_token_hash','evidence_id','owner_organization_id','verification_mode','status','valid_from','expires_at','download_allowed','content_snapshot']),
            ('evidence.evidence_record', ARRAY['id','subject_id','status','source_type','source_ref_id','identity_at_capture']),
            ('evidence.evidence_artifact', ARRAY['evidence_id','media_object_id','artifact_role']),
            ('evidence.media_object', ARRAY['id','sha256','size_bytes','mime_type']),
            ('evidence.recording', ARRAY['media_object_id','validated_duration'])) AS targets(table_name, columns)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = target.table_name::regclass
            AND attname = target.column_name AND attnum > 0 AND NOT attisdropped) THEN
            RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible projection columns';
        END IF;
    END LOOP;
    IF EXISTS (
        SELECT 1 FROM pg_namespace namespace CROSS JOIN LATERAL aclexplode(namespace.nspacl) acl
        WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_'
          AND acl.grantee = 0
        UNION ALL
        SELECT 1 FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
        CROSS JOIN LATERAL aclexplode(relation.relacl) acl
        WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_' AND acl.grantee = 0
        UNION ALL
        SELECT 1 FROM pg_attribute attribute JOIN pg_class relation ON relation.oid = attribute.attrelid
        JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
        CROSS JOIN LATERAL aclexplode(attribute.attacl) acl
        WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_' AND acl.grantee = 0
        UNION ALL
        SELECT 1 FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid = routine.pronamespace
        CROSS JOIN LATERAL aclexplode(coalesce(routine.proacl, acldefault('f', routine.proowner))) acl
        WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_' AND acl.grantee = 0
        UNION ALL
        SELECT 1 FROM pg_database database CROSS JOIN LATERAL aclexplode(coalesce(database.datacl, acldefault('d', database.datdba))) acl
        WHERE database.datname = current_database() AND acl.grantee = 0 AND acl.privilege_type = 'CREATE'
    ) THEN
        RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible PUBLIC application privileges';
    END IF;
    SELECT oid INTO public_role FROM pg_roles WHERE rolname = 'sharing_public_read_role';
    IF public_role IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_roles WHERE oid = public_role AND rolcanlogin AND NOT rolsuper AND NOT rolcreatedb
              AND NOT rolcreaterole AND NOT rolinherit AND NOT rolreplication AND NOT rolbypassrls
              AND rolconnlimit = -1 AND rolvaliduntil IS NULL AND rolconfig IS NULL
        ) OR EXISTS (SELECT 1 FROM pg_auth_members WHERE member = public_role OR roleid = public_role)
          OR EXISTS (SELECT 1 FROM pg_db_role_setting WHERE setrole = public_role)
          OR EXISTS (
            SELECT 1 FROM pg_shdepend dependency WHERE dependency.refclassid = 'pg_authid'::regclass
              AND dependency.refobjid = public_role AND (
                dependency.deptype <> 'a' OR dependency.dbid <> (SELECT oid FROM pg_database WHERE datname = current_database())
                OR NOT (
                    (dependency.classid = 'pg_namespace'::regclass AND dependency.objid IN ('sharing'::regnamespace, 'evidence'::regnamespace))
                    OR (dependency.classid = 'pg_class'::regclass AND dependency.objid IN (
                        'sharing.evidence_share_grant'::regclass, 'evidence.evidence_record'::regclass,
                        'evidence.evidence_artifact'::regclass, 'evidence.media_object'::regclass, 'evidence.recording'::regclass))
                )
              )
          ) THEN
            RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible public role attributes or dependencies';
        END IF;
        IF EXISTS (
            SELECT 1 FROM pg_namespace namespace CROSS JOIN LATERAL aclexplode(namespace.nspacl) acl
            WHERE acl.grantee = public_role AND (namespace.nspname NOT IN ('sharing', 'evidence')
                OR acl.privilege_type <> 'USAGE' OR acl.is_grantable OR acl.grantor <> namespace.nspowner)
            UNION ALL
            SELECT 1 FROM pg_class relation CROSS JOIN LATERAL aclexplode(relation.relacl) acl WHERE acl.grantee = public_role
            UNION ALL
            SELECT 1 FROM pg_attribute attribute JOIN pg_class relation ON relation.oid = attribute.attrelid
            JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
            CROSS JOIN LATERAL aclexplode(attribute.attacl) acl
            WHERE acl.grantee = public_role AND (acl.privilege_type <> 'SELECT' OR acl.is_grantable
                OR acl.grantor <> relation.relowner OR NOT (
                    (namespace.nspname = 'sharing' AND relation.relname = 'evidence_share_grant' AND attribute.attname = ANY(ARRAY[
                        'capability_token_hash','evidence_id','owner_organization_id','verification_mode','status','valid_from','expires_at','download_allowed','content_snapshot']))
                    OR (namespace.nspname = 'evidence' AND relation.relname = 'evidence_record' AND attribute.attname = ANY(ARRAY[
                        'id','subject_id','status','source_type','source_ref_id','identity_at_capture']))
                    OR (namespace.nspname = 'evidence' AND relation.relname = 'evidence_artifact' AND attribute.attname = ANY(ARRAY['evidence_id','media_object_id','artifact_role']))
                    OR (namespace.nspname = 'evidence' AND relation.relname = 'media_object' AND attribute.attname = ANY(ARRAY['id','sha256','size_bytes','mime_type']))
                    OR (namespace.nspname = 'evidence' AND relation.relname = 'recording' AND attribute.attname = ANY(ARRAY['media_object_id','validated_duration']))
                ))
        ) THEN
            RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible public role ACL';
        END IF;
    ELSE
        CREATE ROLE sharing_public_read_role LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE
            NOINHERIT NOREPLICATION NOBYPASSRLS PASSWORD NULL;
    END IF;
    EXECUTE format('REVOKE TEMPORARY ON DATABASE %I FROM PUBLIC', current_database());
END
$preconditions$;

SET LOCAL ROLE sharing_owner;
GRANT USAGE ON SCHEMA sharing TO sharing_public_read_role;
GRANT SELECT (capability_token_hash, evidence_id, owner_organization_id,
    verification_mode, status, valid_from, expires_at, download_allowed, content_snapshot)
    ON sharing.evidence_share_grant TO sharing_public_read_role;
RESET ROLE;

SET LOCAL ROLE evidence_owner;
GRANT USAGE ON SCHEMA evidence TO sharing_public_read_role;
GRANT SELECT (id, subject_id, status, source_type, source_ref_id, identity_at_capture)
    ON evidence.evidence_record TO sharing_public_read_role;
GRANT SELECT (evidence_id, media_object_id, artifact_role)
    ON evidence.evidence_artifact TO sharing_public_read_role;
GRANT SELECT (id, sha256, size_bytes, mime_type)
    ON evidence.media_object TO sharing_public_read_role;
GRANT SELECT (media_object_id, validated_duration)
    ON evidence.recording TO sharing_public_read_role;
RESET ROLE;

DO $postconditions$
DECLARE
    public_role oid := 'sharing_public_read_role'::regrole;
    target record;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_roles WHERE oid = public_role AND rolcanlogin AND NOT rolsuper AND NOT rolcreatedb
          AND NOT rolcreaterole AND NOT rolinherit AND NOT rolreplication AND NOT rolbypassrls
          AND rolconnlimit = -1 AND rolvaliduntil IS NULL AND rolconfig IS NULL
    ) OR EXISTS (SELECT 1 FROM pg_auth_members WHERE member = public_role OR roleid = public_role)
      OR EXISTS (SELECT 1 FROM pg_db_role_setting WHERE setrole = public_role)
      OR EXISTS (SELECT 1 FROM pg_shdepend WHERE refclassid = 'pg_authid'::regclass AND refobjid = public_role AND deptype <> 'a')
      OR EXISTS (SELECT 1 FROM pg_class CROSS JOIN LATERAL aclexplode(relacl) acl WHERE acl.grantee = public_role)
      OR EXISTS (SELECT 1 FROM pg_proc CROSS JOIN LATERAL aclexplode(proacl) acl WHERE acl.grantee = public_role)
      OR EXISTS (SELECT 1 FROM pg_default_acl CROSS JOIN LATERAL aclexplode(defaclacl) acl WHERE acl.grantee = public_role OR acl.grantor = public_role) THEN
        RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible public role postconditions';
    END IF;
    IF (SELECT count(*) FROM pg_namespace namespace CROSS JOIN LATERAL aclexplode(namespace.nspacl) acl
        WHERE acl.grantee = 'sharing_public_read_role'::regrole AND acl.privilege_type = 'USAGE'
          AND acl.grantor = namespace.nspowner AND NOT acl.is_grantable) <> 2
       OR (SELECT count(*) FROM pg_attribute attribute JOIN pg_class relation ON relation.oid = attribute.attrelid
           CROSS JOIN LATERAL aclexplode(attribute.attacl) acl
           WHERE acl.grantee = 'sharing_public_read_role'::regrole AND acl.privilege_type = 'SELECT'
             AND acl.grantor = relation.relowner AND NOT acl.is_grantable) <> 24
       OR has_database_privilege('sharing_public_read_role', current_database(), 'CREATE')
       OR has_database_privilege('sharing_public_read_role', current_database(), 'TEMPORARY') THEN
        RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible public role postconditions';
    END IF;
    FOR target IN
        SELECT namespace.oid, namespace.nspname FROM pg_namespace namespace
        WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_'
    LOOP
        IF has_schema_privilege(public_role, target.oid, 'USAGE') IS DISTINCT FROM (target.nspname IN ('sharing', 'evidence'))
           OR has_schema_privilege(public_role, target.oid, 'CREATE') THEN
            RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible effective schema privileges';
        END IF;
    END LOOP;
    FOR target IN
        SELECT relation.oid, relation.relkind FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
        WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_'
          AND relation.relkind IN ('r','p','v','m','f','S')
    LOOP
        IF target.relkind = 'S' THEN
            IF has_sequence_privilege(public_role, target.oid, 'USAGE,SELECT,UPDATE') THEN
                RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible effective sequence privileges';
            END IF;
        ELSIF has_table_privilege(public_role, target.oid, 'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER') THEN
            RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible effective table privileges';
        END IF;
    END LOOP;
    IF EXISTS (
        SELECT 1 FROM pg_attribute attribute JOIN pg_class relation ON relation.oid = attribute.attrelid
        JOIN pg_namespace namespace ON namespace.oid = relation.relnamespace
        WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_'
          AND relation.relkind IN ('r','p','v','m','f') AND attribute.attnum > 0 AND NOT attribute.attisdropped
          AND (has_column_privilege(public_role, relation.oid, attribute.attnum, 'INSERT,UPDATE,REFERENCES')
            OR has_column_privilege(public_role, relation.oid, attribute.attnum, 'SELECT') IS DISTINCT FROM
                EXISTS (SELECT 1 FROM aclexplode(attribute.attacl) acl WHERE acl.grantee = public_role AND acl.privilege_type = 'SELECT'))
    ) OR EXISTS (
        SELECT 1 FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid = routine.pronamespace
        WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_'
          AND has_function_privilege(public_role, routine.oid, 'EXECUTE')
    ) THEN
        RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'V011 incompatible effective column or routine privileges';
    END IF;
END
$postconditions$;
