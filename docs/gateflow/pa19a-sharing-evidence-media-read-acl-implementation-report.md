# PA19A implementation continuation 1 report

Status: `implementation-complete-controller-validated-pending-independent-review`.

- Task: `pa19a-sharing-evidence-media-read-acl-implementation-continuation1-gpt-20261108`.
- Requested runtime identity: GPT `gpt-6.1-sol`, high.
- Date: 2026-11-08.
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa19a-sharing-evidence-media-read-acl/codex-call`.
- Inspected HEAD: `75fb2c8e03208c4f9025d90788fd802c45954f5c`.
- Binding plan: `docs/gateflow/pa19a-sharing-evidence-media-read-acl-plan.md`, 141 lines / 25,517 bytes; SHA256 `B2BACCBBF260E2382558D3EE445EF32ED56D7F8ABD761EC71771D5015B05E8AC`, verified unchanged.
- Binding inputs read: complete original implementation prompt, accepted plan, current Control PA19A section, all eight partial candidate paths and prior implementation report, V008 linkage and V001-V012 privilege/migration patterns, relevant current source/tests/POM/CI witnesses.

This report supersedes the previous eight-path `blocked-incomplete` report. Valid partial API/query structure was retained rather than automatically discarded. The candidate now contains every approved implementation path. There is no source-level contradiction requiring an expanded path or privilege contract. The original continuation handoff did not claim a runtime pass; completed Controller validation is recorded below and is not implementation acceptance, approval, or a readiness action.

## Controller runtime reconciliation — 2026-11-08

- Reconciliation task: `pa19a-sharing-evidence-media-read-acl-controller-runtime-report-gpt-20261108`; Controller-validated candidate HEAD: `4c447c30beacbf440f1f697e5bc13fe64971ed9b`. This bounded reconciliation changes only this report; the continuation and implementation fixes 1–5 below remain historical records. Their pending-runtime/rerun wording and local non-execution statements describe those handoffs, not the current validation state, and are superseded by this section.
- Controller command: equivalent to `mvn -B -ntp -Pcontainer-smoke clean verify` from `backend/` in Linux container `maven:3.9.9-eclipse-temurin-21`, with the full repository mounted and Docker socket visible; `--add-host=host.docker.internal:192.168.65.254`, `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`, and `TESTCONTAINERS_RYUK_DISABLED=true`.
- Authoritative full runtime log: `C:\Users\Administrator\.codex\worktrees\pa19a-sharing-evidence-media-read-acl\controller-runtime\implementation-full-verify-retry2.log` (inspected in full as UTF-16). It records PostgreSQL `16.15`, Maven `BUILD SUCCESS`, and final `CONTROLLER_VERIFY_EXIT=0`; Maven finished at `2026-11-08T01:44:35Z`. This is completed Controller runtime evidence, not a new Maven run by this report-reconciliation task.
- Fresh runtime XML is green: Surefire **22 classes / 128 methods**; Failsafe **11 classes / 192 methods**; combined **33 classes / 320 methods**, with **zero failures, errors, or skips** in both phases. `PostgresContainerIT` passes **64/64** methods; `EvidenceMediaReadRoleLeastPrivilegeIT` and `EvidenceMediaReadIT` each pass **8/8**. These results supersede the earlier runtime blockers without removing their fix history.
- Exact CI inventory: the unchanged embedded source=CI=XML script from `.github/workflows/ci.yml` was executed by the Controller from `backend/` in WSL Ubuntu 22.04 with Python UTF-8 mode and exited `0`. It printed:

  ```text
  PA19A exact source=CI=XML inventory passed: 22 Surefire classes/128 methods; 11 Failsafe classes/192 methods (64+10+10+8+12+18+8+37+9+8+8=192); combined 33/320; actual-method recipe 112+178=290 -> 128+192=320; comment-inclusive word-boundary line-scan recipe 291 -> 321 (one PostgresContainerIT comment hit, not a method); Failsafe includes 11; zero skips/errors/failures
  ```

  The same unchanged embedded script was also re-executed during this reconciliation against the existing runtime XML with Python UTF-8 mode and exited `0` with the same output. This local inventory execution is not a GitHub Actions or remote CI run.
- Final Controller audits pass: the exact public candidate changed-path set equals the approved **22 paths** below; `git diff --check` exits `0`; there are exactly **13 contiguous migrations**, with **V013 latest**. V001–V012 retain all accepted pinned SHA-256 values recorded below. V013 currently has SHA-256 `691f9a773e1a4c2d5f8c46b1be18515e1dccdbeadacfd15c5d113fcf1878e9f6` and is unchanged by this reconciliation. Private `.codex` and `.pa19a-prompts/` remain untracked and excluded from the candidate.
- Next/current Gateflow gate after this bounded reconciliation: **`code review`**, pending independent **A/B code review**. The implementation is **not accepted**. No reviewer execution, GitHub Actions/remote CI, PR, approval, merge, production readiness, or later-gate result is claimed. Residual-risk owners and destinations remain unchanged.

## Exact changed-path inventory: 22 / 22

1. `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql`
2. `backend/src/main/java/com/onecall/core/evidence/mediaread/package-info.java`
3. `backend/src/main/java/com/onecall/core/evidence/mediaread/EvidenceMediaObjectReader.java`
4. `backend/src/main/java/com/onecall/core/evidence/mediaread/ResolvedMediaObject.java`
5. `backend/src/main/java/com/onecall/core/evidence/internal/mediaread/JooqEvidenceMediaObjectReader.java`
6. `backend/src/main/java/com/onecall/core/evidence/internal/mediaread/EvidenceMediaReadPersistenceConfiguration.java`
7. `backend/src/main/java/com/onecall/core/evidence/internal/mediaread/EvidenceMediaReadPersistenceProperties.java`
8. `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java`
9. `backend/src/test/java/com/onecall/archtests/EvidenceMediaReadPersistenceConfigurationTests.java`
10. `backend/src/test/java/com/onecall/archtests/evidence/EvidenceMediaObjectReaderTests.java`
11. `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java`
12. `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadIT.java`
13. `backend/src/test/java/com/onecall/archtests/container/SharingAdmissionRoleLeastPrivilegeIT.java`
14. `backend/src/test/java/com/onecall/archtests/container/SharingRoleLeastPrivilegeIT.java`
15. `backend/src/test/java/com/onecall/archtests/container/SharingQueryFixture.java`
16. `backend/src/test/java/com/onecall/archtests/container/CustomerMeFixture.java`
17. `backend/src/test/java/com/onecall/archtests/container/SharingQueryServiceIT.java`
18. `backend/src/test/java/com/onecall/archtests/container/PublicShareAdmissionIT.java`
19. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
20. `backend/pom.xml`
21. `.github/workflows/ci.yml`
22. `docs/gateflow/pa19a-sharing-evidence-media-read-acl-implementation-report.md`

There are 10 modified tracked paths and 12 candidate untracked paths. The pre-existing `.codex` file and `.pa19a-prompts/` directory remain untouched and excluded from the candidate. No Control, plan/review/fix artifact, OpenAPI, README, application configuration, V001-V012 migration, or other implementation path is changed. Generated static-check outputs live only under `/tmp`; pre-existing `backend/target` output is not candidate validation evidence.

## Implementation and corrected partial defects

### V013 privilege-only migration

| Area | Implemented source contract | Controller-validated runtime witness |
| --- | --- | --- |
| Identity | PostgreSQL 16 only; migration superuser; dedicated LOGIN, NOSUPERUSER/NOCREATEDB/NOCREATEROLE/NOINHERIT/NOREPLICATION/NOBYPASSRLS, connection limit -1, validity infinity, password NULL, no settings/memberships/ownership | Exact role tuple and boolean-only `pg_authid.rolpassword IS NULL` before/after grants |
| Schema | Only `USAGE` on `evidence`, granted as `evidence_owner` | Exact owner/grantee/USAGE/no-grant-option schema ACL |
| Columns | `evidence_record(id,source_type,source_ref_id,status)`; `evidence_artifact(evidence_id,media_object_id,artifact_role)`; `media_object(id,sha256,size_bytes,mime_type)`; `recording(id,media_object_id,status,validated_duration)` | All 15 actual-role column reads; exhaustive effective evidence-column permissions |
| Dependencies | Zero ACL state or exact 16-row state; symmetric expected/actual set difference over class/object/subobject/dependency/database identities | Fifteen exact `pg_class` column dependencies plus one `pg_namespace` schema dependency, all `deptype='a'`, enumerated after grants and replay |
| Exclusions | No table/DML/function/sequence/database/default-ACL/membership/grant-option expansion; no location columns; no repair of incompatible state | Actual-role denied SQL expects `42501`; effective permissions/catalogs must match exact negative matrix |
| Replay/history | Exactly one successful historical `012`; `013` absent or one compatible successful SQL/script/description row; no V012-latest assumption; compatible null-password role accepted | Zero-ACL compatibility, two committed manual V013 executions, then ordinary `flyway.migrate()` executes zero |
| Atomicity | One Flyway transaction, owner-scoped grants, `RESET ROLE` before privileged postchecks, no database REVOKE | Injected division-by-zero `22012` after new-role creation rolls back role/grants; incompatible topology/history yields `P0001` |

The partial migration only counted a subset of dependencies, ignored incompatible extra dependency/ACL states, used unpadded history versions, and lacked complete postconditions. These defects are replaced with exact dependency and expanded ACL checks plus separately enumerated effective schema/table/column/function/sequence/database/default privileges. Both membership directions and role settings are rejected. Same-count substituted column grants, partial grants, PUBLIC exposure, unexpected owner/dependency states, incompatible role attributes, and failed/duplicate/incompatible history have executable rejection witnesses.

### Reader, configuration, and module boundary

- The public named interface is only `evidence::media-read`. The sealed API is `Result.Found(ResolvedMediaObject)`, `Result.Missing`, and `Result.Unavailable`; the partial sealed permits declaration is corrected.
- One parameterized query selects exactly the approved 15 fields. It anchors on `er.id`, preserves artifact/recording restrictions in three LEFT JOINs, and has no aggregation, LIMIT, storage-location field, or raw-row API.
- Only zero rows become `Missing`; multiple rows and incomplete/inconsistent/invalid existing evidence become `Unavailable`. UUID linkage, SOLIDIFIED/CALL_RECORDING/ORIGINAL/VALIDATED facts, lowercase 64-hex hash, positive size, nonblank MIME, and nullable-positive duration are checked before constructing the safe DTO.
- Null input throws `NullPointerException` before transactions/database access. An explicitly qualified read-only REQUIRES_NEW/READ_COMMITTED transaction template encloses the query and mapping. Connection acquisition, transaction completion, SQL, and mapping failures are caught at the public boundary; no exception detail or partial DTO escapes. This corrects the partial annotation-only transaction boundary, which could throw connection failures outside its catch.
- Configuration requires the `persistence` profile, explicit enabled=true, a well-formed PostgreSQL host/database URL without embedded URL credentials/options, exact `evidence_media_reader_role` username, and nonblank password. Missing/disabled settings are inert; enabled-invalid settings fail startup with a constant sanitized cause. Credentials remain separate external properties. There is no fallback datasource or repository credential value.
- Datasource, transaction manager/template, and DSL are separately named/qualified. The manager enforces database read-only transactions, Hikari is lazy/read-only, and jOOQ execution logging is disabled. Properties, datasource, configuration, reader, DTO, and Found string forms redact configuration/row data.
- The added architecture method asserts the named public API, absence of internal types from every evidence interface, and an ArchUnit prohibition against cross-module imports of `evidence.internal.mediaread`. The existing allowed-dependency map remains unchanged.

### Test and inventory implementation

The two new ITs each have exactly eight real test methods. They provision generated transient passwords and authenticate as the actual dedicated role, not owner/superuser SET ROLE for positive reads. The reader IT includes qualified wiring, actual session/current-user/read-only/isolation evidence, parameter/query capture, valid linkage, zero-row Missing, missing graph edges, statuses, linkage mismatches, corrupted metadata, optional duration, duplicate ORIGINAL rows, and connection/permission failure behavior. Privileged fixture corruptions are isolated to test databases and are not production schema changes.

The two new unit classes each have exactly four methods. They cover configuration activation/fail-closed/qualified wiring/redaction and mock-JDBC reader mapping/API/serialization/DTO validation/error boundaries. `ModuleBoundaryTests` gains exactly one method. No existing test method is removed, disabled, parameterized, or replaced with inventory padding.

All seven existing allowlisted migration witnesses now account for V013. Historical V012 index-11 assertions and negative-access probes remain intact. PostgresContainerIT changes its post-V008 batch 4 to 5, appends the exact V013 history/description/script, and includes `evidence_media_reader_role:true:false:false:false` in its exhaustive role inventory. Both new ITs are unconditional Failsafe includes within the existing profile, giving 11 includes. CI literal method/class sets, early Surefire total, final XML assertions, recipe assertions, and printed arithmetic are updated.

| Inventory | Verified source/CI expectation | Runtime result |
| --- | --- | --- |
| Surefire | 22 classes / 128 methods | Controller XML passes; zero failures/errors/skips |
| Failsafe | 11 classes / 192 methods | Controller XML passes; zero failures/errors/skips |
| Combined | 33 classes / 320 methods | Controller XML passes; zero failures/errors/skips |
| Actual-method recipe | `[112,178]=290` to `[128,192]=320` | Full unchanged source=CI=XML inventory passes |
| Comment-inclusive line scan | 291 to 321, exactly one existing PostgresContainerIT comment hit | Full unchanged source=CI=XML inventory passes |
| Failsafe includes | 9 to 11, exact class-set equality | Full unchanged source=CI=XML inventory passes |

## Static checks actually performed

Historical continuation-only checks follow. Their local non-execution and stale-XML qualifications are preserved as history; the Controller runtime reconciliation above supplies the completed current runtime evidence.

1. Read and verified the accepted plan SHA256 and unchanged HEAD identity. Git's Windows worktree pointer is not edited; commands use `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call20` and the current `GIT_WORK_TREE` explicitly.
2. Java 21 static compilation: all 127 main Java files and all 60 test Java files compile successfully with `/opt/jdk21/bin/javac -proc:none --release 21`, outputs in `/tmp/pa19a-classes` and `/tmp/pa19a-test-classes`. The classpath uses the 95 existing cached Surefire dependency JAR paths plus cached Flyway JARs for test compilation; no old target classes are used as candidate compilation output. Existing deprecation/unchecked notes remain. This is compilation, not test execution or Maven/enforcer/annotation-processing validation.
3. An initial exploratory javac invocation over every cached JAR failed in Spring Modulith annotation processing with `NoSuchMethodError` from mixed cached dependency versions. It is superseded by the successful explicit-classpath, annotation-processing-disabled compilation above; no annotation-processing pass is claimed.
4. Parsed the exact CI inventory Python as an AST and executed its source/CI/POM-only portion, intentionally excluding report reads and runtime XML assertions. Exact method-name equality passes for every class, as do 22/128 + 11/192, baseline `[112,178]`, line scans 291/321, one documented comment hit, and the 11 exact POM includes. The complete CI Python parses successfully. Existing report XML is stale and was not represented as new evidence.
5. Statically verified the production query's exact 15 selected expressions, one UUID bind, three LEFT JOINs, ON restrictions, and absence of LIMIT/aggregation/location fields. Verified V013's exactly five capability GRANT statements: one schema grant and 15 column SELECT grants; no schema object creation or broader grant statement.
6. Ran the exact `sha256sum src/main/resources/db/migration/V0{01..12}__*.sql` and sorted migration inventory commands from `backend/`. There are exactly 12 historical hash rows and 13 contiguous migrations, V013 latest. Each historical migration matches the initial observed digest, the accepted pinned digest, and `git cat-file --filters HEAD:<path>` checkout bytes. This explicitly respects the repository's CRLF checkout filter rather than treating canonical Git blob LF bytes as an unauthorized edit.
7. Checked the changed-path set against the original prompt's exact 22-path allowlist. No missing or extra candidate path. Checked `git diff --check` and all candidate files for trailing whitespace/final newlines. The existing dependency firewall source and all following CI steps remain unchanged; no full resolved-production-JAR firewall execution is claimed.

No Docker, docker.exe, Maven, PostgreSQL process, Testcontainers process, or JUnit runtime validation was invoked during this continuation. No new Surefire/Failsafe XML exists from this continuation. No zero-failure/error/skip runtime result, actual SQLSTATE/catalog result, or runtime unchanged-admission claim is made.

## V001-V012 before/after byte identity

The SHA256 in each row is both the observed pre-continuation and final digest; all 12 match the accepted plan exactly.

| Migration | Before SHA256 = after SHA256 |
| --- | --- |
| V001 | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| V002 | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| V003 | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| V004 | `8d03b32814baf9a9a812e89d3f56f666a5fb025fc518ab92d454fdcf6f5ea030` |
| V005 | `6feb3cbc87014fc83362e6faa33cc8d4107348c31540bf382df320657dc8d065` |
| V006 | `7a30ab8a97ad4062918a58dbcfcab97580fdca5752b939dd56c484c425a0a09d` |
| V007 | `bf3fc710b76eb7b95b317458089f1f796f9aa42fde50444762db54152a350d81` |
| V008 | `32aab5baf90ca09774241f5b9a4dd819e03352c5fbeb7e62d53cde8c3292ddde` |
| V009 | `3226255afdf44a2dcc06b55d8059511b6cec5795a38083471f310c1c89bcd1b1` |
| V010 | `d48c624d5f5c6d9ec47f0152a61fc48355f1025ac7b8b84cf9343170bfa94559` |
| V011 | `ba7d485dce97d6cfd81a9f1c5a2e384b68a82fe865d73af7ee9edc92defde48e` |
| V012 | `ff5e65fe808c8c98b1a71a5abadaab513516206e173fc6a7dada5f599fde39c6` |

## Historical Controller runtime handoff and residual owners

The runtime requests and pending wording below record the original handoff and are superseded by the completed Controller runtime reconciliation above. Residual ownership and destinations remain in force; no separate dependency-firewall script execution is claimed here.

- Controller: run `mvn -B -DskipTests=false test` and `mvn -B -DskipTests=false -Pcontainer-smoke verify` in the already proven Linux Maven/Testcontainers environment. Require actual XML 22/128 + 11/192 = 33/320, with zero failures/errors/skips, and run the full exact CI source=expected=XML inventory plus existing dependency firewall. Static compilation does not prove these results.
- Controller: execute PostgreSQL 16 migration/role/ACL/dependency/transaction/history probes, including exactly 16 dependency rows after initial grants and each replay; zero before grants; superuser boolean password-null checks; twice-compatible replay followed by zero ordinary migrations; incompatible-state rollback; actual-role read/denial SQLSTATEs; preserved V012 access/denials. PL/pgSQL catalog behavior has not been executed here.
- Controller: execute qualified datasource/session read-only and reader/configuration/API/serialization/redaction/architecture tests against the final tree. Recheck all historical hashes and the 22-path boundary afterward. The candidate does not rely on prior baseline target output as a pass.
- Deployment/database operations: provision the dedicated role password and explicit datasource properties externally after authorization; no repository secret or deployment change is included. The URL validator intentionally requires a PostgreSQL host/database URL with credentials supplied separately, not URL query options.
- Evidence service/integrity: recording hash/subject and broader integrity cross-checks remain deferred; this query does not read those fields and makes no such claim.
- Later PA19 plan/implementation: owns mapping Missing/Unavailable into playback outcomes and all transport/credential/streaming work, none of which is implemented here.

Sharing-admission source privileges are unchanged: V012 is byte-identical, the existing role IT only changes the latest migration count, and the new role IT adds an actual-login witness denying artifact/media/recording while retaining its four evidence-record columns. Controller runtime confirmation is now recorded in the green full verification and XML above, including the existing sharing-admission and new evidence role suites.

No commit, staging, branch creation, push, PR, approval, readiness action, review dispatch, reviewer request, external comment, or issue mutation was performed.

## Implementation fix 1 — Controller unit-test finding

Implementation fixes 1–5 are historical handoff records. All pending Controller rerun/status statements in these records are superseded by the Controller runtime reconciliation above; their findings, bounded edits, and local non-execution facts are retained unchanged.

- Task: `pa19a-sharing-evidence-media-read-acl-implementation-fix1-gpt-20261108`.
- Date: 2026-11-08. Controller-provided candidate HEAD: `0dac63fe13f0a15fbd845552f557a0acbd006bcc`.
- Controller fresh compilation succeeded for 127 main and 60 test sources. Surefire found only the impossible-unit-test-vector defect in `EvidenceMediaObjectReaderTests.incompleteInconsistentAndInvalidExistingFactsNeverBecomeMissing`: field 14 with `"not-an-integer"`.
- Removed only that corruption-array vector. PostgreSQL enforces the V008 `evidence.recording.validated_duration` integer type, so a real row cannot contain this string. The jOOQ mock converted it to null; null validated duration is valid, so production correctly returned `Found`.
- Retained duration `0` and `-1`, the valid optional-null duration test, all null graph-field cases, all UUID/content/status mismatches, and SQL and transaction exception coverage. No test method names or counts changed; no production code, grants, query, schema, POM, CI, allowlist, or other test changed.
- Status: pending Controller full rerun. No Docker or Maven rerun was performed for this fix, and no runtime pass is claimed.

## Implementation fix 2 — Controller V013 syntax finding

- Task: `pa19a-sharing-evidence-media-read-acl-implementation-fix2-gpt-20261108`.
- Requested runtime identity: GPT `gpt-6.1-sol`, high. Date: 2026-11-08. Inspected HEAD: `bea742799cdbbc3d33533e7b7c7ba3b774dd13d1`.
- Controller evidence: Surefire passed 128/128; all 11/11 Failsafe suites were blocked while migrating V013 by `ERROR: syntax error at end of input`, SQLSTATE `42601`, position `5228`, parser line 1. The position maps to the first of the two ACL-inventory `CASE` expressions. Flyway rolled back, so no V013 effect persisted.
- Replaced only those two `CASE` constructs with CASE-free rejection predicates: `(granted AND namespace_count <> 1) OR (NOT granted AND namespace_count <> 0)` and `(granted AND attribute_count <> 15) OR (NOT granted AND attribute_count <> 0)`, retaining the original catalog count subqueries. `granted` is assigned from `EXISTS`, so it is non-null; these predicates preserve both exact accepted states without weakening any count.
- Every other role/schema/table/column/dependency/replay precondition, grant, and expected count remains unchanged. Static inspection checked the single dollar-quoted `DO` statement, balanced IF/LOOP blocks and parentheses, all internal `END IF;`/`END LOOP;` terminators, and the final outer `END` followed by `$migration$;`; no delimiters were changed. PostgreSQL 16/Flyway runtime confirmation remains pending Controller rerun.
- Changed paths for this fix only: `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql` and `docs/gateflow/pa19a-sharing-evidence-media-read-acl-implementation-report.md`.
- Status: pending Controller full rerun. No Docker, Maven, or PostgreSQL runtime validation was performed for this fix; no runtime pass is claimed. No commit, staging, push, PR, approval, readiness, review, or external action was performed.

## Implementation fix 3 — Controller V013 role-catalog finding

- Task: `pa19a-sharing-evidence-media-read-acl-implementation-fix3-gpt-20261108`.
- Requested runtime identity: GPT `gpt-6.1-sol`, high. Date: 2026-11-08. Controller-provided HEAD: `45dad2215435cce7f1051175823365e75c77e275`; local Git inspection is unavailable because the worktree Git pointer resolves to an invalid Windows-style path in this environment.
- Controller evidence: the targeted PostgreSQL 16.15 run passed the former CASE parser point and failed with SQLSTATE `42703`, `column "rolconfig" does not exist`.
- Split role qualification into two exact `NOT EXISTS` rejection checks keyed by `oid=reader`: `pg_roles` supplies login, superuser/createdb/createrole/inherit/replication/bypassrls flags, connection limit, validity, and null role configuration; `pg_authid` supplies only the boolean `rolpassword IS NULL` check. No password value is selected or printed. Either failed check still raises `V013 incompatible role topology` in both phases.
- Static verification confirms the only direct `pg_authid` query references `oid` and `rolpassword`; the `pg_roles` predicate uses only PostgreSQL 16 columns (`oid`, `rolcanlogin`, `rolsuper`, `rolcreatedb`, `rolcreaterole`, `rolinherit`, `rolreplication`, `rolbypassrls`, `rolconnlimit`, `rolvaliduntil`, `rolconfig`). Existing `pg_authid` regclass references for shared dependencies remain unchanged. Dollar quotes, parentheses, and IF/LOOP/outer block delimiters remain balanced.
- Every other role, membership, setting, database, dependency, ACL, replay predicate, and grant remains byte-for-byte unchanged. Changed paths for this fix only: `backend/src/main/resources/db/migration/V013__evidence_media_reader_role.sql` and this report.
- Status: pending Controller targeted role IT and full verification. No Docker, Maven, PostgreSQL runtime validation, commit, push, PR, approval, review, or external action was performed for this fix; no runtime pass is claimed.

## Implementation fix 4 — Controller role-IT catalog/cast findings

- Task: `pa19a-sharing-evidence-media-read-acl-implementation-fix4-gpt-20261108`.
- Requested runtime identity: GPT `gpt-6.1-sol`, high. Date: 2026-11-08. Inspected HEAD: `9713d16ed9ba9c2ffd8817774520971d71eeb485` (using the mounted Git metadata path without modifying the worktree pointer).
- Controller evidence: targeted `EvidenceMediaReadRoleLeastPrivilegeIT` successfully applied all 13 migrations, including V013, on PostgreSQL 16.15. The remaining failures were test SQL catalog/cast defects: querying `rolconfig` from `pg_authid` instead of `pg_roles`, and concatenating `pg_shdepend.deptype` (`"char"`) with text without an explicit cast (`operator is not unique: text || "char"`).
- Fixed only role-IT SQL: the role attribute/config tuple now reads from `pg_roles` and retains the same 10 expected values through `(rolconfig IS NULL)`; a separate `pg_authid` query asserts `(rolpassword IS NULL)` is true (PostgreSQL JDBC text representation `t`), without selecting or printing the password. `dependencies()` now concatenates `dependency.deptype::text`.
- Static verification preserves all eight test method names, the exact 16 dependency rows, and every ACL assertion. Production Java, V013, POM, CI, allowlist, and other tests remain unchanged by this fix. Changed paths: `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java` and this report.
- Status: pending Controller targeted role-IT and full rerun. No Docker, Maven, or PostgreSQL runtime validation was performed locally for this fix; no role-IT pass is claimed. No commit, staging, push, PR, approval, readiness, review, or external action was performed.

## Implementation fix 5 — Controller role-IT sequence-query finding

- Task: `pa19a-sharing-evidence-media-read-acl-implementation-fix5-gpt-20261108`.
- Requested runtime identity: GPT `gpt-6.1-sol`, high. Date: 2026-11-08. Controller-provided HEAD: `4c447c30beacbf440f1f697e5bc13fe64971ed9b`; default local Git inspection is unavailable because the worktree Git pointer resolves to an invalid Windows-style path in this environment.
- Controller evidence: targeted role IT applies V013 and reaches all assertions; five methods fail at `assertExactAcls` line 262 with `ERROR: "pg_toast_..." is not a sequence`. The test query calls `has_sequence_privilege` on arbitrary `pg_class` rows, and planner evaluation can reach TOAST relations before the `relkind='S'` filter excludes them.
- Replaced the unsafe query with an expected-empty assertion over `pg_sequences` selecting any sequence on which the reader has `USAGE`, `SELECT`, or `UPDATE`. Sequence names are schema-qualified and identifier-quoted with `format('%I.%I',schemaname,sequencename)`. Separately assert that `SELECT oid::text FROM pg_class WHERE relnamespace='evidence'::regnamespace AND relkind='S'` is empty, without passing arbitrary relation OIDs to `has_sequence_privilege`.
- All eight test method names/counts and every existing expected value remain unchanged. Changed paths for this fix only: `backend/src/test/java/com/onecall/archtests/container/EvidenceMediaReadRoleLeastPrivilegeIT.java` and this report. Production, V013, POM, CI, allowlist, and all other tests remain untouched.
- Status: pending Controller targeted role-IT and full rerun. No Docker, Maven, or PostgreSQL runtime validation was performed locally for this fix; no runtime pass is claimed. No commit, staging, push, PR, approval, review, or external action was performed.
