DO $migration$
DECLARE
    reader oid;
    phase integer;
    granted boolean;
    target record;
    column_fact record;
    expected_select boolean;
    expected_columns text[] := ARRAY[
        'evidence_record.id','evidence_record.source_type','evidence_record.source_ref_id','evidence_record.status',
        'evidence_artifact.evidence_id','evidence_artifact.media_object_id','evidence_artifact.artifact_role',
        'media_object.id','media_object.sha256','media_object.size_bytes','media_object.mime_type',
        'recording.id','recording.media_object_id','recording.status','recording.validated_duration'];
BEGIN
    IF current_setting('server_version_num')::integer NOT BETWEEN 160000 AND 169999
       OR NOT (SELECT rolsuper FROM pg_roles WHERE rolname=current_user) THEN
        RAISE EXCEPTION 'V013 requires PostgreSQL 16 and migration superuser';
    END IF;
    IF (SELECT count(*) FROM public.flyway_schema_history WHERE version IN ('012','12')) <> 1
       OR (SELECT count(*) FROM public.flyway_schema_history WHERE version='012' AND success) <> 1
       OR (SELECT count(*) FROM public.flyway_schema_history WHERE version IN ('013','13')) > 1
       OR EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE version IN ('013','13')
           AND (version <> '013' OR NOT success OR type <> 'SQL' OR script <> 'V013__evidence_media_reader_role.sql'
               OR description <> 'evidence media reader role')) THEN
        RAISE EXCEPTION 'V013 incompatible migration history';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='evidence_media_reader_role') THEN
        CREATE ROLE evidence_media_reader_role LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT
            NOREPLICATION NOBYPASSRLS CONNECTION LIMIT -1 VALID UNTIL 'infinity' PASSWORD NULL;
    END IF;
    reader := 'evidence_media_reader_role'::regrole;
    FOR phase IN 0..1 LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE oid=reader AND rolcanlogin AND NOT rolsuper
            AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolinherit AND NOT rolreplication
            AND NOT rolbypassrls AND rolconnlimit=-1 AND rolvaliduntil='infinity'::timestamptz
            AND rolconfig IS NULL)
           OR NOT EXISTS (SELECT 1 FROM pg_authid WHERE oid=reader AND rolpassword IS NULL)
           OR EXISTS (SELECT 1 FROM pg_auth_members WHERE member=reader OR roleid=reader)
           OR EXISTS (SELECT 1 FROM pg_db_role_setting WHERE setrole=reader)
           OR has_database_privilege(reader,current_database(),'CREATE,TEMPORARY')
           OR EXISTS (SELECT 1 FROM pg_database CROSS JOIN LATERAL aclexplode(datacl) acl
               WHERE acl.grantee=reader OR acl.grantor=reader)
           OR EXISTS (SELECT 1 FROM pg_default_acl CROSS JOIN LATERAL aclexplode(defaclacl) acl
               WHERE acl.grantee IN (0,reader) OR acl.grantor=reader) THEN
            RAISE EXCEPTION 'V013 incompatible role topology';
        END IF;
        granted := EXISTS (SELECT 1 FROM pg_shdepend WHERE refclassid='pg_authid'::regclass AND refobjid=reader);
        IF phase=1 AND NOT granted THEN
            RAISE EXCEPTION 'V013 missing grants';
        END IF;
        IF granted AND EXISTS (
            WITH expected AS (
                SELECT 'pg_class'::regclass::oid AS classid, relation.oid AS objid,
                    attribute.attnum::integer AS objsubid, 'a'::"char" AS deptype,
                    (SELECT oid FROM pg_database WHERE datname=current_database()) AS dbid
                FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
                JOIN pg_attribute attribute ON attribute.attrelid=relation.oid
                WHERE namespace.nspname='evidence' AND relation.relname||'.'||attribute.attname=ANY(expected_columns)
                UNION ALL SELECT 'pg_namespace'::regclass::oid,'evidence'::regnamespace::oid,0,'a'::"char",
                    (SELECT oid FROM pg_database WHERE datname=current_database())
            ), actual AS (
                SELECT classid,objid,objsubid,deptype,dbid FROM pg_shdepend
                WHERE refclassid='pg_authid'::regclass AND refobjid=reader
            )
            SELECT 1 FROM ((SELECT classid,objid,objsubid,deptype,dbid FROM actual
                    EXCEPT SELECT classid,objid,objsubid,deptype,dbid FROM expected)
                UNION ALL (SELECT classid,objid,objsubid,deptype,dbid FROM expected
                    EXCEPT SELECT classid,objid,objsubid,deptype,dbid FROM actual)) difference
        ) THEN
            RAISE EXCEPTION 'V013 incompatible exact dependency set';
        END IF;
        IF EXISTS (SELECT 1 FROM pg_class CROSS JOIN LATERAL aclexplode(relacl) acl WHERE acl.grantee=reader OR acl.grantor=reader)
           OR EXISTS (SELECT 1 FROM pg_proc CROSS JOIN LATERAL aclexplode(proacl) acl WHERE acl.grantee=reader OR acl.grantor=reader)
           OR EXISTS (SELECT 1 FROM pg_namespace CROSS JOIN LATERAL aclexplode(nspacl) acl WHERE acl.grantor=reader)
           OR EXISTS (SELECT 1 FROM pg_attribute CROSS JOIN LATERAL aclexplode(attacl) acl WHERE acl.grantor=reader)
           OR (granted AND (SELECT count(*) FROM pg_namespace CROSS JOIN LATERAL aclexplode(nspacl) acl WHERE acl.grantee=reader) <> 1)
           OR (NOT granted AND (SELECT count(*) FROM pg_namespace CROSS JOIN LATERAL aclexplode(nspacl) acl WHERE acl.grantee=reader) <> 0)
           OR (granted AND (SELECT count(*) FROM pg_attribute CROSS JOIN LATERAL aclexplode(attacl) acl WHERE acl.grantee=reader) <> 15)
           OR (NOT granted AND (SELECT count(*) FROM pg_attribute CROSS JOIN LATERAL aclexplode(attacl) acl WHERE acl.grantee=reader) <> 0) THEN
            RAISE EXCEPTION 'V013 incompatible ACL inventory';
        END IF;
        FOR target IN SELECT oid,nspname,nspacl FROM pg_namespace
            WHERE nspname <> 'information_schema' AND nspname !~ '^pg_' LOOP
            IF has_schema_privilege(reader,target.oid,'USAGE') IS DISTINCT FROM (granted AND target.nspname='evidence')
               OR has_schema_privilege(reader,target.oid,'CREATE')
               OR EXISTS (SELECT 1 FROM aclexplode(target.nspacl) acl WHERE acl.grantee=0
                   OR acl.grantee=reader AND (target.nspname <> 'evidence' OR acl.privilege_type <> 'USAGE'
                       OR acl.grantor <> 'evidence_owner'::regrole OR acl.is_grantable)) THEN
                RAISE EXCEPTION 'V013 incompatible schema privileges';
            END IF;
        END LOOP;
        FOR target IN SELECT relation.oid,relation.relkind,relation.relname,relation.relacl,namespace.nspname
            FROM pg_class relation JOIN pg_namespace namespace ON namespace.oid=relation.relnamespace
            WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_'
                AND relation.relkind IN ('r','p','v','m','f','S') LOOP
            IF target.relkind='S' THEN
                IF has_sequence_privilege(reader,target.oid,'USAGE,SELECT,UPDATE') THEN
                    RAISE EXCEPTION 'V013 incompatible sequence privileges';
                END IF;
            ELSE
                IF has_table_privilege(reader,target.oid,'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')
                   OR EXISTS (SELECT 1 FROM aclexplode(target.relacl) acl WHERE acl.grantee=0) THEN
                    RAISE EXCEPTION 'V013 incompatible table privileges';
                END IF;
                FOR column_fact IN SELECT attnum,attname,attacl FROM pg_attribute
                    WHERE attrelid=target.oid AND attnum>0 AND NOT attisdropped LOOP
                    expected_select := granted AND target.nspname='evidence'
                        AND target.relname||'.'||column_fact.attname=ANY(expected_columns);
                    IF has_column_privilege(reader,target.oid,column_fact.attnum,'SELECT') IS DISTINCT FROM expected_select
                       OR has_column_privilege(reader,target.oid,column_fact.attnum,'INSERT,UPDATE,REFERENCES')
                       OR EXISTS (SELECT 1 FROM aclexplode(column_fact.attacl) acl WHERE acl.grantee=0
                           OR acl.grantee=reader AND (NOT expected_select OR acl.privilege_type <> 'SELECT'
                               OR acl.grantor <> 'evidence_owner'::regrole OR acl.is_grantable)) THEN
                        RAISE EXCEPTION 'V013 incompatible column privileges';
                    END IF;
                END LOOP;
            END IF;
        END LOOP;
        IF EXISTS (SELECT 1 FROM pg_proc routine JOIN pg_namespace namespace ON namespace.oid=routine.pronamespace
            WHERE namespace.nspname <> 'information_schema' AND namespace.nspname !~ '^pg_'
                AND (has_function_privilege(reader,routine.oid,'EXECUTE')
                    OR EXISTS (SELECT 1 FROM aclexplode(coalesce(routine.proacl,acldefault('f',routine.proowner))) acl WHERE acl.grantee=0))) THEN
            RAISE EXCEPTION 'V013 incompatible routine privileges';
        END IF;
        IF phase=0 THEN
            SET LOCAL ROLE evidence_owner;
            GRANT USAGE ON SCHEMA evidence TO evidence_media_reader_role;
            GRANT SELECT (id, source_type, source_ref_id, status) ON evidence.evidence_record TO evidence_media_reader_role;
            GRANT SELECT (evidence_id, media_object_id, artifact_role) ON evidence.evidence_artifact TO evidence_media_reader_role;
            GRANT SELECT (id, sha256, size_bytes, mime_type) ON evidence.media_object TO evidence_media_reader_role;
            GRANT SELECT (id, media_object_id, status, validated_duration) ON evidence.recording TO evidence_media_reader_role;
            RESET ROLE;
        END IF;
    END LOOP;
END
$migration$;
