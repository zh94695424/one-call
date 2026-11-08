# PA19A sharing evidence media read ACL plan

Status: `plan-complete` (plan gate only; no implementation authorized by this artifact).

## Goal and binding alignment

Implement the smallest evidence-owned PostgreSQL read capability that resolves one `evidence.evidence_record.id` to exactly one private media object for the frozen PA19 media-playback foundation. It returns only an opaque `mediaObjectId` (the UUID `evidence.media_object.id`) and safe bounded metadata. No storage location, URL, credential, or raw row crosses the evidence boundary. `sharing_admission_role` remains unable to read the private graph. This is one composed implementation slice: one privilege-only V013 plus the minimum evidence-owned reader/configuration and focused proof.

Motivation and success signal remain as confirmed in Control: V008 owns private evidence facts; V012 intentionally grants sharing admission only its four columns; there is no lawful evidence-owned runtime datasource. PostgreSQL 16/Flyway tests must prove exact grants/denials, reader result mappings, explicit-credential activation, and unchanged V012 denial. V001–V012 remain byte-identical.

Non-goals: gateway/transport, URLs, COS/KMS, production credentials/secrets, byte recall/streaming/ranges/signing/Q22; PA15–PA19 authorization/OpenAPI changes; schema/lifecycle/trigger/service-function changes; generic storage abstractions, caches, role administration, or transport scaffolding; owner/superuser runtime access; widening sharing/public projections or `sharing_admission_role`; any implementation in this plan-fix gate; commit, push, PR, approval, review dispatch, or external action.

## Fixed decisions

1. **Role and migration.** V013 is privilege-only and follows V012. It creates `evidence_media_reader_role`, with direct login and no memberships: `LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS CONNECTION LIMIT -1 VALID UNTIL 'infinity' PASSWORD NULL`. No role settings/configuration, ownership, role grants, or broad privileges. V001–V012 are immutable.
2. **Exact columns.** Grant `USAGE` on schema `evidence`; grant column-level `SELECT` only on:
   - `evidence.evidence_record(id, source_type, source_ref_id, status)`
   - `evidence.evidence_artifact(evidence_id, media_object_id, artifact_role)`
   - `evidence.media_object(id, sha256, size_bytes, mime_type)`
   - `evidence.recording(id, media_object_id, status, validated_duration)`
   The public opaque reference is only `media_object.id`. `bucket`, `object_key`, and `object_version_id` are forbidden in V013, query, probes, DTO/API, logs, and claims.
3. **API.** Publish named interface `evidence::media-read` only via `backend/src/main/java/com/onecall/core/evidence/mediaread/package-info.java`. `EvidenceMediaObjectReader.resolve(UUID evidenceId)` returns sealed `Result`: `Found(ResolvedMediaObject media)`, `Missing`, or `Unavailable`. `ResolvedMediaObject(UUID mediaObjectId, String sha256, long sizeBytes, String mimeType, Integer durationSeconds)` requires nonnull UUID/hash/MIME, lowercase `[0-9a-f]{64}` hash, positive size, and null or positive duration. Null `evidenceId` throws `NullPointerException` via `Objects.requireNonNull` before database access. SQL, configuration, cardinality, and mapping faults return `Unavailable` with no exception detail or partial DTO.
4. **Query and result contract.** Execute one parameterized read-only query, rooted at `evidence.evidence_record er` and constrained by `er.id = :evidenceId`. LEFT JOIN artifact using `a.evidence_id = er.id AND a.artifact_role = 'ORIGINAL'`; LEFT JOIN media object on `m.id = a.media_object_id`; LEFT JOIN recording on `er.source_type = 'CALL_RECORDING' AND r.id = er.source_ref_id AND r.media_object_id = m.id`. Put artifact-role and recording predicates in `ON`, never in a filtering `WHERE` clause. Select only the 15 granted fields: `er.id, er.source_type, er.source_ref_id, er.status, a.evidence_id, a.media_object_id, a.artifact_role, m.id, m.sha256, m.size_bytes, m.mime_type, r.id, r.media_object_id, r.status, r.validated_duration` (15 fields, all from the four granted column sets). Return rows without SQL aggregation or `LIMIT`; cardinality is checked in Java.
   - zero rows: `Missing` (there is no evidence record with the requested id).
   - exactly one row: `Found` only if `er.status='SOLIDIFIED'`, `er.source_type='CALL_RECORDING'`, `er.source_ref_id` is nonnull, exactly one joined ORIGINAL artifact exists, `a.evidence_id=er.id`, artifact media UUID equals `m.id`, exactly one recording exists with `r.id=er.source_ref_id`, `r.media_object_id=m.id`, `r.status='VALIDATED'`, hash matches lowercase 64-hex, size is positive, MIME is nonblank, and `validated_duration` is null or positive. Otherwise `Unavailable`.
   - more than one row: `Unavailable`, including duplicate ORIGINAL artifacts or ambiguous linked facts.
   LEFT JOIN-null evidence presence is distinguished from absent evidence; no invalid existing evidence can collapse to `Missing`.
5. **No unsupported integrity claim.** This query does not read or compare a recording hash or subject identifier. PA19A makes no recording-hash/subject cross-check claim. Broader evidence integrity remains Evidence/service-owned residual work unless a later authorized query reads and compares the required column.
6. **Configuration.** Evidence-owned exact properties: `onecall.evidence-media-read.datasource.enabled`, `.url`, `.username`, `.password`. Activate only when `enabled=true` explicitly and URL, username, password are nonblank; username must equal `evidence_media_reader_role`. Missing/false enablement creates no reader, datasource, or transaction manager. If enabled but any value is absent, malformed, or username differs, fail startup closed; no fallback datasource. Use a separately named and qualified datasource, transaction manager, and `DSLContext`; internal reader uses only the qualified read datasource and read-only transaction. Password-bearing properties, datasource, exceptions, and row values must never be logged. String forms for properties/config/DTO/result/reader redact secrets and avoid sensitive row data. No credential goes in repository configuration or fixture output; deployment provisions credentials externally. No `application-persistence.yml` edit.
7. **Module boundary.** No `evidence/package-info.java`, internal mediaread package-info, `ai_orchestration/package-info.java`, or `SharingQueryBoundaryTests` edit. Do not narrow `ai_orchestration`; it has no consumer in this unit. Keep `ModuleBoundaryTests` with an actual assertion that `evidence::media-read` is the named public API and that other modules cannot import `com.onecall.core.evidence.internal.mediaread`; preserve exact existing allowed dependency map otherwise. Evidence and sharing remain independent of PA19.
8. **V013 structure and replay.** PostgreSQL 16 and migration-runner superuser are mandatory. History must contain exactly one successful V012 row. V013 history may be absent (initial application/manual replay before history insertion) or exactly one successful row (manual replay after application); reject failed, duplicate, or incompatible V013 states. Never require V012 to remain latest after V013 is recorded. Check history version, success, and counts explicitly. Before action, assert PG16 and direct database CREATE/TEMP are false for the reader role; do not attempt database REVOKE as `evidence_owner`. As migration superuser, query `pg_authid.rolpassword IS NULL` before action and after `RESET ROLE`; never read, echo, or store the value. Role absent means create it; an existing role must already match. Execute only evidence schema/table grant statements under `SET LOCAL ROLE evidence_owner`. Permit only exact expected ACL state or no ACLs for replay; the exact permitted ACL state is 15 expected column-level rows plus one expected schema-USAGE row as defined below. The role IT enumerates and compares all 16 rows after initial grants and on replay. Execute V013 SQL twice on a compatible fully migrated fixture, both succeed, then ordinary `flyway.migrate()` executes zero migrations.

## V013 exact privilege matrix and assertions

After validating the preconditions, execute in one transaction with `SET LOCAL ROLE evidence_owner` and exactly these capability grants (identifier/column order is fixed):

```sql
GRANT USAGE ON SCHEMA evidence TO evidence_media_reader_role;
GRANT SELECT (id, source_type, source_ref_id, status) ON evidence.evidence_record TO evidence_media_reader_role;
GRANT SELECT (evidence_id, media_object_id, artifact_role) ON evidence.evidence_artifact TO evidence_media_reader_role;
GRANT SELECT (id, sha256, size_bytes, mime_type) ON evidence.media_object TO evidence_media_reader_role;
GRANT SELECT (id, media_object_id, status, validated_duration) ON evidence.recording TO evidence_media_reader_role;
```

Do not repair unexpected ACL state: a pre-existing role with any unexpected direct grant fails precondition. Never issue database-level REVOKE as `evidence_owner`; incompatible direct database CREATE/TEMP fails the superuser precondition. V011's PUBLIC TEMP revocation remains baseline evidence. Do not revoke PUBLIC privileges or mutate unrelated principals. Only evidence schema/table grants run as `evidence_owner`; role creation and privileged catalog reads run as migration superuser.

| Scope | Required state for `evidence_media_reader_role` | Forbidden state |
|---|---|---|
| Role identity | `pg_roles` attributes: `rolcanlogin=true`; superuser/createdb/createrole/inherit/replication/bypassrls false; `rolconnlimit=-1`; `rolvaliduntil='infinity'`; superuser-only `pg_authid.rolpassword IS NULL` before action and after `RESET ROLE` | Any role config, membership, ownership/dependency, role grant/option |
| Database | Connect only as allowed by existing database baseline; no V013 database grant; `has_database_privilege(...,'CREATE')` and `TEMP` false | CREATE, TEMP, grant option |
| Schema | `USAGE` on `evidence` true; CREATE false | USAGE/CREATE on any other schema; evidence CREATE |
| Tables | No table-level SELECT or any table DML/REFERENCES/TRIGGER | SELECT beyond listed columns; INSERT/UPDATE/DELETE/TRUNCATE/REFERENCES/TRIGGER on every table |
| Columns | SELECT exactly the four listed column sets in Fixed decisions | SELECT on every unlisted column, including media location columns and recording hash/subject; no grant option |
| Sequences/functions | No sequence USAGE/SELECT/UPDATE; no function EXECUTE | Any sequence/function privilege, including evidence trigger functions |
| Defaults | No default ACL entries granting role or PUBLIC relevant privileges | Any default table/sequence/function ACL privilege or grant option |
| PUBLIC | V013 introduces no grant to PUBLIC | Any V013 object/default grant to PUBLIC |

Exact dependency predicate: `pg_shdepend` for the reader role permits exactly 16 `deptype='a'` ACL dependencies after grants: (1) 15 `classid='pg_class'` column ACL rows, with `objid` limited to the four listed tables and each expanded `attacl` item matching expected grantor `evidence_owner`, grantee `evidence_media_reader_role`, privilege `SELECT`, and `is_grantable=false`; and (2) one `classid='pg_namespace'` row for schema `evidence`, expanded from `nspacl`, matching expected grantor `evidence_owner`, grantee `evidence_media_reader_role`, privilege `USAGE`, and `is_grantable=false`. Require zero ACL rows before grants. Reject every other classid, objid, deptype (including membership `p`), ACL item, membership (`pg_auth_members`), role setting (`pg_db_role_setting`), ownership/non-ACL dependency, and unexpected grant. Expand `relacl`, `attacl`, and `nspacl` with `aclexplode`, comparing these exact table/column/schema privilege sets, no table-level SELECT, expected grantor/grantee, and no grant options. Check effective permissions separately with `has_*_privilege`; enumerate all evidence relations/columns/functions/sequences, database/schema ACLs, PUBLIC, `pg_auth_members`, `pg_db_role_setting`, and `pg_default_acl`. Pre/postconditions compare exact state, not samples. The role IT enumerates all 16 expected rows (15 table/column plus one schema) after initial grants and on replay; the pre-grant role returns zero rows. PG16 replay test runs V013 SQL twice on one compatible fully migrated fixture and requires both succeed; then ordinary `flyway.migrate()` executes zero migrations. History checks allow successful V013 without requiring V012 latest.

## Reader and configuration proof

The implementation adds tests for: `Found`, zero-row `Missing`, absent artifact, absent media, absent recording, wrong evidence status/type, nonvalidated recording, media UUID mismatch, invalid/null metadata, duplicate ORIGINAL/ambiguous joins, SQL failure, null UUID `NullPointerException`; configuration disabled/missing, enabled-invalid startup failure, enabled-valid qualified wiring and redaction; API reflection/serialization and every relevant `toString` contain no location, credential, or raw-row fields. Public DTO serialization contains only `mediaObjectId`, `sha256`, `sizeBytes`, `mimeType`, `durationSeconds`.

Positive database probes authenticate as the actual `evidence_media_reader_role` using a transient generated fixture password (not a privileged `SET ROLE` positive path), and execute the reader/query through its qualified datasource. Assert exact permitted columns and successful resolution. Forbidden probes use the actual role for table/column reads, unrelated evidence objects, all mutations, schema/database privileges, sequence/function access, membership escalation, and storage-location columns; assert permission denied (`42501`) where SQL is applicable and catalog false otherwise. Separately prove `sharing_admission_role` remains denied on artifact/media/recording and retains its V012 assertions. `SET ROLE evidence_owner` is used only by V013 to perform grants, never as evidence of positive runtime access.

## Exact unconditional implementation allowlist (22 paths)

| # | Path | Classification / exact purpose |
|---:|---|---|
| 1 | `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql` | New privilege-only migration; no schema objects/data |
| 2 | `backend/src/main/java/com/onecall/core/evidence/mediaread/package-info.java` | New public named interface declaration `media-read` |
| 3 | `backend/src/main/java/com/onecall/core/evidence/mediaread/EvidenceMediaObjectReader.java` | Public resolve/result API |
| 4 | `backend/src/main/java/com/onecall/core/evidence/mediaread/ResolvedMediaObject.java` | Safe bounded DTO |
| 5 | `backend/src/main/java/com/onecall/core/evidence/internal/mediaread/JooqEvidenceMediaObjectReader.java` | Internal parameterized query and exact mappings |
| 6 | `backend/src/main/java/com/onecall/core/evidence/internal/mediaread/EvidenceMediaReadPersistenceConfiguration.java` | Conditional qualified datasource/reader wiring |
| 7 | `backend/src/main/java/com/onecall/core/evidence/internal/mediaread/EvidenceMediaReadPersistenceProperties.java` | Strict validated property contract and redaction |
| 8 | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` | Named interface plus forbidden internal-import assertion |
| 9 | `backend/src/test/java/com/onecall/archtests/EvidenceMediaReadPersistenceConfigurationTests.java` | Configuration tests; exactly 4 methods |
| 10 | `backend/src/test/java/com/onecall/archtests/evidence/EvidenceMediaObjectReaderTests.java` | API/DTO/mapping/redaction tests; exactly 4 methods |
| 11 | `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java` | New PostgreSQL role/grant proof; exactly 8 methods |
| 12 | `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java` | New PostgreSQL reader proof; exactly 8 methods |
| 13 | `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java` | Update migration count; preserve historical V012 index-11 assertion and denials |
| 14 | `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java` | Latest migration/count only |
| 15 | `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java` | Latest migration/count only |
| 16 | `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java` | Latest migration/count only |
| 17 | `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java` | Hardcoded migration count `12 -> 13` only |
| 18 | `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java` | Hardcoded migration count `12 -> 13` only |
| 19 | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | Post-V008 batch `4 -> 5`; V013 final history/script inventories; exhaustive role tuple includes `evidence_media_reader_role:true:false:false:false` |
| 20 | `backend/pom.xml` | Unconditional Failsafe includes for both new `*IT` classes |
| 21 | `.github/workflows/ci.yml` | Exact source/CI/XML inventory and all measured counters |
| 22 | `docs/gateflow/pa19a-sharing-evidence-media-read-acl-implementation-report.md` | New implementation completion report only |

No other path is permitted. In particular, `evidence/package-info.java`, any internal `package-info.java`, `ai_orchestration/package-info.java`, `SharingQueryBoundaryTests`, `application-persistence.yml`, OpenAPI, README, and V001–V012 are prohibited. POM, CI, and both migration-count witnesses are unconditional, not conditional.

## Migration witnesses and inventory update sites

Update every current-latest assertion from 12 to 13 in the allowlisted files. Preserve assertions specifically about historical V012 and its index 11. In `PostgresContainerIT`, update (a) the post-V008 expected migration batch from 4 to 5, (b) final migration history rows to include version 13 and exact description `evidence media reader role`, (c) exact script inventory through `V013__evidence_media_reader_role.sql`, and (d) exhaustive role inventory with `evidence_media_reader_role:true:false:false:false`. Do not rewrite unrelated V001–V012 history. Search each allowlisted test and CI source for `12`, `V012`, history/script lists, expected batches, and role tuple; classify each match historical or latest and update only latest witnesses.

## Source = CI = XML arithmetic

Binding final totals: Surefire 22 classes / 128 methods; Failsafe 11 classes / 192 methods; combined 33 classes / 320 methods. New config and reader unit classes each contribute 4 methods; `ModuleBoundaryTests` adds 1 method; total Surefire increment is 9 methods, from early/current 119 to 128. (No `SharingQueryBoundaryTests` method is added.) New role IT and reader IT each contribute exactly 8 methods; Failsafe increment is 16 methods from 176 to 192. `backend/pom.xml` exact Failsafe include count changes 9 to 11.

CI baseline recipe is fixed: actual-method baseline `[112,178]`, sum 290, becomes `[128,192]`, sum 320. Comment-inclusive line scan is 291 before and 321 after, with exactly one documented comment hit over the actual-method sum. Early Surefire total changes 119 to 128. Expected maps/class sets and XML report class/method counts must match these exact targets, with zero failures/errors/skips. Preserve PA18-specific historical text/counts as historical; update only current PA19A assertions, including every echo-server literal and printed arithmetic. Do not treat `migration inventory` as a nonexistent CI script: migration evidence consists of the enumerated source assertions above; dependency firewall is a separate CI check and remains exact.

## Commands and required assertions

Run from `backend/`:

1. Before and after implementation, run `sha256sum src/main/resources/db/migration/V0{01..12}__*.sql` from `backend/`; require exactly 12 output rows in V001–V012 order, compare all rows to the pinned 12 digests below, and require byte-identical digests to pass. The fixed-plan baseline sequence is retained from the preceding plan: `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6`, `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505`, `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7`, `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030`, `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065`, `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d`, `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81`, `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde`, `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1`, `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559`, `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e`, `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6`.
2. `find src/main/resources/db/migration -maxdepth 1 -name 'V*.sql' | sort`; exactly 13 contiguous migrations, V013 latest.
3. `mvn -B -DskipTests=false test`; Surefire exactly 22/128.
4. `mvn -B -DskipTests=false -Pcontainer-smoke verify`; PostgreSQL 16 required; Failsafe exactly 11/192 and combined 33/320; no skips/failures/errors. Docker/image failure is failure, never a skip or H2 fallback.
5. Run the exact source=expected=XML script in `.github/workflows/ci.yml`; assert all arithmetic above, including baseline, line scan, early total, and POM include count. Run existing dependency firewall check; confirm no migration-inventory CI script is assumed.
6. Run the exact evidence reader, role, V012-denial, and migration-history probes specified above; `git diff --check`; compare changed paths to the exact 22-path allowlist.
7. Implementation report records all 12 before/after migration digests and test command outputs. No generated reports or other artifacts are added to the allowlist.

## One-slice implementation contract

**Slice 1 — Private evidence media read capability end-to-end.** Allowed files are exactly the 22 listed paths. Preserve one composed slice; V013 and the reader are proved together. Invariants: only dedicated role, exact grants, no privilege escalation, no location columns, no sharing widening, no PA19 authorization/OpenAPI change, one query with fixed mapping, fail-closed configuration, no internal API leakage, V001–V012 byte identity.

Required tests are the role and reader ITs (8 methods each), configuration and reader unit tests (4 each), one `ModuleBoundaryTests` method, existing V012 denial tests, both migration-count witnesses, PostgresContainer migration/role/history assertions, and CI source=CI=XML inventory. Positive probes authenticate as actual reader role; privileged grant execution is not a positive-access proof.

Stop before implementation if V008 fixture evidence cannot establish the defined evidence-record/artifact/recording linkage or if exact grants/query require a schema change, broader identity, location grant, sharing widening, or alteration of V001–V012. Do not invent an alternate query or conditional path; report the contradictory evidence to Controller.

## Residual risks and ownership

- Recording hash/subject and broader evidence-integrity cross-checks are not performed by this query. Owner: Evidence service/integrity; destination: later evidence-owned integrity slice. Not a PA19A claim or blocker.
- Dedicated role password must be provisioned externally after authorization. Owner: deployment/database operations; destination: PA19A deployment runbook; no in-repo secret/config deployment.
- PostgreSQL 16 container/image availability is environmental. Owner: implementation/CI runner; destination: required `-Pcontainer-smoke verify`; do not skip or downgrade.
- Future mapping of `Missing`/`Unavailable` to playback outcomes is outside PA19A. Owner: PA19 plan/implementation; destination: frozen PA19 implementation contract.
- V008 linkage semantics must be checked before coding. Owner: Evidence implementation; destination: stop condition above if inconsistent.

No other current-scope residual or open design choice is unclassified.

## Goal-drift and minimality demonstration

This plan adds only the privilege-only V013 and the minimum evidence-owned query/configuration/API needed to consume it, in one slice. The public reference is the existing media-object UUID and the only other data are hash, size, MIME, and validated duration. No storage location is needed or granted. The dedicated login role is isolated from `evidence_role` and sharing admission. Only one public package marker is needed; no module dependency narrowing or sharing test edit is justified. The two missing migration witnesses, POM includes, CI arithmetic, and migration inventory are strictly necessary for existing hardcoded tests and Failsafe execution. No schema object, public behavior, deployment secret, transport, or PA19 authorization work is introduced.

## Implementation completion report

Report status, exact touched-path list/count (22), V013 SQL/role/grant/postcondition matrix, V001–V012 before/after hashes, PG16 actual-role positive/negative probes and SQLSTATE/catalog outcomes, query mappings, configuration activation/fail-closed/redaction assertions, source=CI=XML arithmetic and zero skip/error/failure totals, exact commands/results, `git diff --check`, residual owners, and unchanged sharing-admission access. No commit, push, PR, approval, or review action is implied.
