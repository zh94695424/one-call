# PA1 V001 Persistence Foundation — S1 Implementation (Evidence Complete; Ready for Independent Code Review)

- Evidence-completion task label: `pa1-v001-s1-evidence-ds-20261027-02`
- Implementation task label: `pa1-v001-s1-impl-ds-20261027-01`
- Validation-fix task label: `pa1-v001-s1-validation-fix1-ds-20261027-01`
- Work unit: `PA1-v001-persistence-foundation`
- Gate: `S1 implementation` — bounded evidence completion; not a new design or review gate
- Author/provider: DeepSeek-V4.1-Flash (`ds`)
- Date: 2026-10-27
- Base: `main` at `9fe3619af968d896400742d90df819bb66d868bd` (merged PR #8)
- Worktree: `/mnt/c/Users/Administrator/.codex/worktrees/pa1-v001-persistence-foundation/codex-call`
- Artifact: `docs/gateflow/pa1-v001-persistence-foundation-s1-implementation.md`
- Status: **S1 implementation evidence COMPLETE and ready for independent code review.** S1 is **not accepted**; no independent code review has been performed; no GitHub Actions, CI-green, production or production-acceptance claim is made.

## 1. Evidence completion summary

The controller completed the exact-JDK validation that had been outstanding after validation fix 1. The controller-observed results are:

| Check | Controller-observed result |
| --- | --- |
| Exact-JDK container validation | **PASS.** Run from WSL Ubuntu-22.04 with `JAVA_HOME=/tmp/temurin-21.0.13`, `PATH` beginning `/tmp/temurin-21.0.13/bin:/mnt/d/tools/maven/bin` (system Maven), Maven local repository `/mnt/d/tools/m2`, and the S1 container command `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 -Pcontainer-smoke verify`. Java was Temurin `21.0.13+9`; the PostgreSQL container reported `16.15`. |
| S1 container tests | **PASS.** Exactly `3` tests passed: `0` failures, `0` errors, `0` skipped. |
| V001 application | **PASS.** V001 applied as Flyway version `001`; `validate()` passed; the repeat `migrate()` applied zero migrations. |
| Broken-migration fixture | **PASS.** The isolated V901 fixture reached its intentional sentinel (`PA1_S1_BROKEN_MIGRATION_SENTINEL_2f4c9a7e`); PostgreSQL/Flyway reported that the changes were successfully rolled back. |
| Default exact-JDK verification | **PASS.** `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 clean verify` ran `10` tests with `0` failures, `0` errors and `0` skips. |
| Boot jar smoke | **PASS.** The boot jar started `OneCallApplication` and exited `0`. |
| Fat-jar firewall | **PASS.** Inspection found none of `spring-security`, `flyway`, `jooq`, `HikariCP`, `postgresql` or `testcontainers` under `BOOT-INF/lib`. |
| Final Enforcer check | **PASS.** `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 validate` passed. |
| Negative Enforcer probe | **PASS.** Temporarily changing `flyway-core` from `test` to `compile` made the exact-JDK `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 validate` fail at `BannedDependencies`, explicitly naming `flyway-core 12.4.0` as banned. The POM was restored. |
| Negative Surefire classpath probe | **PASS.** Temporarily removing only the `flyway-core` classpath exclusion made `BootstrapDependencyBoundaryTests` fail because `org.flywaydb.core.Flyway` was present. The exclusion was restored. |
| GitHub Actions | **NOT RUN.** No CI-green claim is made. |
| Production acceptance | **NOT CLAIMED.** |

The accidental untracked private-use-colon directory observed in the worktree was diagnosed as a Maven dependency cache caused by Windows-path translation and was moved out of the worktree to `C:/Temp/pa1-generated-m2-cache-20261027`. It is not part of the deliverable.

## 2. Scope actually implemented

S1 only:

- One transactional PostgreSQL 16 Flyway migration at
  `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql`:
  - exactly six S1 schemas: `platform`, `audit`, `customer_iam`, `organization_access`, `operations_iam`, `subject_number`;
  - exactly 15 V001 NOLOGIN/NOSUPERUSER/NOCREATEDB/NOCREATEROLE roles;
  - exactly three V001 tables (`platform.idempotency_record`, `platform.outbox_event`, `audit.audit_fact`) with the accepted columns, named checks, PK/unique/partial indexes;
  - the append-only audit function `audit.reject_row_mutation()` and trigger `trg_audit_fact_immutable`;
  - exactly four declared `SECURITY DEFINER` functions with fixed `search_path` and the accepted grant matrix;
  - exact schema/table/column/function grants and `PUBLIC` denial.
- Test-scope Flyway wiring plus the PA0 runtime/fat-jar firewall preserved:
  - `flyway-core` and `flyway-database-postgresql` at `test` scope only;
  - Enforcer Flyway ban narrowed to `compile` and `runtime` scopes;
  - Surefire `classpathDependencyExcludes` for both Flyway artifacts;
  - failsafe `container-smoke` remains the only container profile.
- S1 `PostgresContainerIT` with exactly three test methods:
  - `v001MigratesCleanPostgres16WithoutSkipping`;
  - `v001InventoryMatchesTheFrozenContract`;
  - `brokenMigrationFailsVisibly`.
- The four-function happy-path smoke is a helper called by the migration test as
  `subject_number_role`, not an extra test method and not a placeholder. It rolls back all
  inserted rows and asserts method-local absence afterward.
- Isolated broken-migration fixture under
  `backend/src/test/resources/db/migration-broken/V901__pa1_broken_migration_fixture.sql`
  with a dedicated history schema/table (`pa1_broken_history` /
  `flyway_schema_history_broken`), a non-colliding version `901`, a unique sentinel
  (`PA1_S1_BROKEN_MIGRATION_SENTINEL_2f4c9a7e`), and a marker table created by the preceding
  valid DDL.
- Bounded README and CI report-assertion updates for S1 only.
- Validation fix 1 changed only two assertions inside `PostgresContainerIT`: the Flyway
  history version representation and the trigger-definition check. No migration, dependency,
  CI, README, fixture, contract or scope change was made by that fix.

Not implemented: S2 idempotency/Outbox behavioral matrix, S3 audit/security behavioral matrix,
V002-V009, runtime DataSource/Flyway/Hikari/jOOQ/Spring Security, domain services, API/UI/cloud,
dispatcher/queue/scheduler, production role provisioning, credentials, or Q17/Q23 guesses.

## 3. Changed files and SHA-256

The seven S1 deliverable paths are the six content files below plus this artifact. All hashes
were recomputed read-only with `sha256sum`; the six content-file hashes were unchanged by this
evidence update. Line counts from `wc -l`: V001 SQL `536`; `PostgresContainerIT` `984`; broken
fixture `15`.

| Path | SHA-256 |
| --- | --- |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `backend/pom.xml` | `fbc2d9ecedc2dac942c3c460b1bfc17049d4cb5ffbdda39c7b1496ebc6ddfda5` |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `f1f0c5ff9d7d20ac0c4ce374c6350d3dca6d390e542352f9c33ed7420a0c16ef` |
| `backend/src/test/resources/db/migration-broken/V901__pa1_broken_migration_fixture.sql` | `fb3b83b384f1ccf3c9bf7ed9c60e6c3413cc987dd2035dd9d062c698cf287923` |
| `backend/README.md` | `c9c81580d49c529eb1f83deaea47067c00143bc55889f4ac1ab4b8214a390d99` |
| `.github/workflows/ci.yml` | `8bf27cc2f31826c278df27dfefcbcedfe5347a8b52e923ca568126e1636837b1` |
| `docs/gateflow/pa1-v001-persistence-foundation-s1-implementation.md` (this artifact) | A file cannot embed its own final SHA-256 without changing it. The final read-only digest is reported in the task completion response. Pre-update digest: `f58b37e44b68211b6d9461bb7dea36e53408714a2f916bae263bb38f8cb67e79` (superseded by this update). |

Unchanged by the S1 slice: `docs/14-production-data-model-and-flyway-schema-spec.md`, all A0
artifacts, `application.yml`, main Java sources, `BootstrapDependencyBoundaryTests`,
`OneCallApplicationTests`, `ModuleBoundaryTests`, screenshots and Git metadata. The control
document, accepted plan and accepted review were not modified by S1 implementation, validation
fix 1 or this evidence-completion task.

## 4. Binding inputs read

- `docs/gateflow/project-execution-control.md`: PA1 contract, slice principle, accepted plan
  review adjudication, S1 allowed paths, slice-CI clarification and the
  `S1 implementation — validation fix 1 in progress` gate record.
- `docs/gateflow/pa1-v001-persistence-foundation-plan.md`: §§4-8, exact V001 inventory,
  function/grant contracts, S1 behavior, command/CI expectations.
- `docs/reviews/plan-review-20261027-093407.md`: F-PA1-PR-01/02/03.
- `docs/12-technology-stack-and-system-architecture.md` §§5, 6, 14, 15 Phase A, 18 item 1.
- `docs/14-production-data-model-and-flyway-schema-spec.md` §§2, 3.1-3.2, 4.3-4.5, 6.1-6.2,
  7.1-7.2, 9.0-9.3, 9.23, 10.1-10.8, 10.10, 11.1, 11.3, 12.1-12.4, 14 and 15.
- `backend/pom.xml`, `backend/README.md`, all existing tests, `.github/workflows/ci.yml`.

`docs/14` was not modified. There is no V002-V009 production migration and no V002+ production
schema, role, table or function in the worktree. The only other migration version is the
test-only V901 fixture under `src/test/resources/db/migration-broken`, which is never on the
production classpath.

## 5. Controller executable evidence (cumulative)

The controller ran the required validation on the exact toolchain: Temurin `21.0.13+9`
(`JAVA_HOME=/tmp/temurin-21.0.13`), system Maven `3.9.9` at `/mnt/d/tools/maven/bin` with
`PATH` beginning `/tmp/temurin-21.0.13/bin:/mnt/d/tools/maven/bin`, WSL Ubuntu-22.04, Maven
local repository `/mnt/d/tools/m2`, WSL Docker `29.1.3` and PostgreSQL `16.15`.

| Check | Result |
| --- | --- |
| `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 validate` | **PASS.** |
| `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 dependency:tree -Dincludes=org.flywaydb:*` | **PASS.** `flyway-core:12.4.0:test` and `flyway-database-postgresql:12.4.0:test`; no compile/runtime occurrence reported. |
| `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 clean verify` (default) | **PASS.** 10 tests, 0 failures, 0 errors, 0 skipped; no container required. |
| boot jar smoke | **PASS.** Started `OneCallApplication`; exited `0`. |
| fat-jar forbidden dependency search | **PASS.** Empty result (`spring-security`, `flyway`, `jooq`, `HikariCP`, `postgresql`, `testcontainers` absent). |
| `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 -Pcontainer-smoke verify` | **PASS.** Exactly 3 S1 container tests, 0 failures, 0 errors, 0 skipped. V001 applied as version `001`; `validate()` passed; repeat migrate applied zero; the isolated V901 fixture reached its sentinel and rolled back. |

The two assertion defects reported by the earlier controller run were fixed as described in §6.
The corrected container run passed all three S1 methods. The earlier exact failures were:

1. `v001MigratesCleanPostgres16WithoutSkipping` expected the
   `public.flyway_schema_history.version` value `1`, but Flyway/PostgreSQL store the exact
   representation `001` for filename `V001__...`.
2. `v001InventoryMatchesTheFrozenContract` expected the `pg_get_triggerdef` substring
   `BEFORE UPDATE OR DELETE`, but PostgreSQL normalized the definition text to
   `BEFORE DELETE OR UPDATE`. The event set and all other trigger semantics were correct.

Both were assertion-representation defects in the test, not migration, schema, function or
grant defects. The post-fix container run passed.

## 6. Bounded validation fix 1

Only `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` was
changed.

1. Flyway history representation: the migration test now asserts the accepted exact value
   `"001"` instead of `"1"`, consistently with Flyway's representation of `V001__...`. The
   script name, description, type and `success = true` assertions are unchanged.
2. Audit trigger assertion: the order-sensitive `pg_get_triggerdef` substring check was
   replaced by a structured `pg_trigger` catalog query. It asserts:
   - `tgrelid = 'audit.audit_fact'::regclass`;
   - `tgfoid = 'audit.reject_row_mutation()'::regprocedure`;
   - BEFORE (`tgtype` bit 2);
   - row-level (`tgtype` bit 1);
   - both UPDATE (bit 16) and DELETE (bit 8) are present;
   - INSERT (bit 4) and TRUNCATE (bit 32) are absent;
   - exactly one non-internal `trg_audit_fact_immutable` row.

   This verifies BEFORE, row-level, target table, trigger function and both events without
   depending on PostgreSQL's textual event ordering, and does not weaken away either event.

No V001 SQL, contract, migration, POM, README, CI, fixture, control document, plan/review,
source document or other path was modified by validation fix 1. The V001 SQL SHA-256 is
unchanged at `946a0a13...`, confirming the fix did not alter the migration. No test,
placeholder or S2/S3 behavior was added.

## 7. Evidence-completion validation performed by this task

This task changed only this artifact. The following checks were performed read-only after the
artifact update:

| Check | Result |
| --- | --- |
| `sha256sum` of the six S1 content files | Exit `0`; all six hashes matched the values recorded in §3. |
| `sha256sum` of this artifact | Exit `0`; final digest is reported in the task completion response because a file cannot contain its own final digest. |
| `wc -l` of V001 SQL, `PostgresContainerIT`, broken fixture | Exit `0`; `536`, `984`, `15`. |
| Scoped whitespace/status check on this artifact | `git diff --check -- docs/gateflow/pa1-v001-persistence-foundation-s1-implementation.md` could not run in this harness: the worktree `.git` file points to `D:/codex-call/.git/worktrees/codex-call1`, which git resolves as `fatal: not a git repository` in WSL. This is an environment limitation, not a claim that the working tree is clean. The six content files were independently re-hashed and are byte-identical to their pre-update values. |
| Scope of writes by this task | Only this artifact was written. No source, test, migration, POM, README, workflow, accepted plan/review, control document, docs/12, docs/14 or other file was modified by this task. |

The controller's build and container results in §5 are controller-observed and were not
re-executed by this evidence-completion task. This task adds no new build, container, CI or
production evidence. At the time of this evidence completion, the control document recorded
the S1 row as `S1 implementation — validation fix 1 in progress`; this artifact is the S1
evidence return for the controller's gate-record update and does not modify the control
document.

## 8. Flyway dependency versions

The POM declares, without a guessed version:

- `org.flywaydb:flyway-core` — `test` scope;
- `org.flywaydb:flyway-database-postgresql` — `test` scope.

The controller's dependency tree resolved both artifacts to `12.4.0` at `test` scope under the
Spring Boot `4.1.2` BOM. Plan risk R-02 is closed with resolved-version evidence.

## 9. Migration/inventory evidence implemented

`v001MigratesCleanPostgres16WithoutSkipping` asserts, on a real `postgres:16` container:

- the database is clean before migration: `server_version_num >= 160000`, database/user
  `onecall`, no six S1 schemas, no non-system tables, no `public.flyway_schema_history`;
- exactly one migration executed; exactly one successful
  `public.flyway_schema_history` row with version `001`, script
  `V001__platform_schemas_roles_idempotency_outbox_audit.sql`;
- `flyway.validate()` returns without error; no pending migrations; a second `flyway.migrate()`
  executes zero migrations;
- the four-function happy-path smoke as `subject_number_role`, followed by rollback and
  method-local absence checks for the idempotency key, Outbox dedupe key and audit object ID.

Controller evidence confirms this test passed on the exact toolchain: V001 applied as version
`001`, `validate()` passed, the repeat migrate applied zero, and the test completed with no
failures.

`v001InventoryMatchesTheFrozenContract` asserts, excluding system schemas and Flyway metadata:

- exactly six S1 schemas and their owners;
- exactly 15 non-system roles (plus the container `onecall` superuser), each
  `NOLOGIN/NOSUPERUSER/NOCREATEDB/NOCREATEROLE`;
- exact three business tables, exact 12/14/12 columns and exact 10/12/10 named checks;
- exact PK/unique constraints, exact 12 indexes, and the three partial-index predicates;
- no `id` default, no sequence, no foreign key;
- exactly five user functions (four declared plus the trigger function), exact signatures,
  owners, `SECURITY DEFINER`/`SECURITY INVOKER` mode and `search_path`;
- no `p_id` argument; `pg_catalog.gen_random_uuid()` present in the three insert functions; no
  transaction-control keyword in any function body; `issue_first_trial` absent;
- exactly one trigger, `audit.audit_fact.trg_audit_fact_immutable`, with the structured
  BEFORE/ROW/UPDATE/DELETE semantics described in §6;
- exact schema `USAGE`/`CREATE`, table SELECT/column UPDATE and function EXECUTE grants, plus
  no `DELETE`/`TRUNCATE`, no `PUBLIC` ACL entry, and no S2/S3 schema/role.

Controller evidence confirms this test passed on the exact toolchain. The earlier
order-sensitive trigger-text assertion was fixed as described in §6.

`brokenMigrationFailsVisibly` asserts:

- a dedicated Flyway instance with `locations("classpath:db/migration-broken")`,
  `schemas/defaultSchema = pa1_broken_history`, and
  `table = flyway_schema_history_broken`; version `901` cannot collide with production `V001`;
- a `FlywayException` whose cause chain contains
  `PA1_S1_BROKEN_MIGRATION_SENTINEL_2f4c9a7e`, proving the intentional failure statement ran;
- the marker table `pa1_broken_marker.first_valid_ddl` from the preceding valid DDL is absent
  after rollback, with an explicit pre-run absence check and `finally`-equivalent cleanup of
  both the marker schema and isolated history schema;
- no successful history row for version `901`.

Controller evidence confirms the isolated V901 fixture reached its sentinel and that
PostgreSQL/Flyway reported the changes successfully rolled back. The test passed.

No row count in these tests is global: the smoke uses method-local UUID keys and the inventory
uses exact business-object sets, excluding `public.flyway_schema_history` and system schemas.

## 10. Function-grant smoke

The helper `exerciseV001FunctionsAsGrantedRole`:

- opens a transaction, executes `SET LOCAL ROLE subject_number_role`, and asserts
  `current_user = subject_number_role` (a declared granted role, not superuser);
- calls `platform.idempotency_begin` (`NEW`), `platform.idempotency_complete`
  (`COMPLETED`), `platform.idempotency_begin` again (`REPLAY`), `platform.outbox_append`,
  and `audit.append_fact`;
- rolls back and asserts the method-local idempotency, Outbox and audit rows are absent.

This is the S1 minimum happy-path smoke; it is not the S2/S3 behavioral matrix. It passed as
part of the controller's corrected container run.

## 11. Negative probes

Both required negative probes were executed by the controller and passed. Each temporary edit
was restored; the final `backend/pom.xml` SHA-256 is
`fbc2d9ecedc2dac942c3c460b1bfc17049d4cb5ffbdda39c7b1496ebc6ddfda5`, matching the pre-probe
POM.

1. **Enforcer compile-scope probe.** Temporarily changing `flyway-core` from `test` to
   `compile` made the exact-JDK `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 validate` fail at `BannedDependencies`,
   explicitly naming `flyway-core 12.4.0` as banned. The POM was restored.
2. **Surefire classpath probe.** Temporarily removing only the `flyway-core` classpath
   exclusion made `BootstrapDependencyBoundaryTests` fail because
   `org.flywaydb.core.Flyway` was present. The exclusion was restored.

`BootstrapDependencyBoundaryTests` remains unchanged. The final classpath design is:
`test`-scope Flyway + Surefire `classpathDependencyExcludes` + Failsafe sees Flyway.

## 12. CI assertion

`.github/workflows/ci.yml` asserts exactly the three S1 container test methods implemented at
this checkpoint:

- `v001MigratesCleanPostgres16WithoutSkipping`;
- `v001InventoryMatchesTheFrozenContract`;
- `brokenMigrationFailsVisibly`.

It asserts `tests = 3`, `skipped = 0`, `failures = 0`, `errors = 0`, the exact method set, and
still rejects any skipped surefire test. Docker-first, boot jar smoke, production fat-jar
firewall, contract regressions, permissions and path filters are unchanged.

**GitHub Actions has not been run.** No CI-green claim is made. S2/S3 must expand this
assertion when their tests are implemented; S1 does not wait for the final 12.

## 13. Docs decision

- `backend/README.md` contains S1-only content: V001 objects, test-scope Flyway firewall,
  three S1 container methods, failure isolation, 10 default surefire tests, and the Q17/Q23
  limitation.
- This artifact is the cumulative S1 implementation, validation-fix and evidence-completion
  record; it does not claim S2/S3 behavior evidence.
- `docs/14` and all A0 artifacts are unchanged.
- No architecture, schema, API or deployment docs were added.
- This artifact is the only gate document written by validation fix 1 and by this
  evidence-completion task.

## 14. Residual risks and open inputs

| ID | Risk / input | Status | Owner / destination |
| --- | --- | --- | --- |
| R-01 | Enforcer 3.6.3 must honor `org.flywaydb:*:*:*:compile|runtime` scope-qualified excludes. | **Closed** — the compile-scope negative probe failed at `BannedDependencies`, naming `flyway-core 12.4.0`; POM restored | Backend/Platform; no further action |
| R-02 | Whether Boot 4.1.2 BOM manages `flyway-database-postgresql`. | **Closed** — controller resolved `flyway-core:12.4.0:test` and `flyway-database-postgresql:12.4.0:test` | Backend; no pin required |
| R-04 | Flyway history in `public` is migration metadata, excluded from business inventory. | Implemented in tests | Backend/Database; deployment design |
| R-06 | Test-scope Flyway vs PA0 bootstrap guardrail. | **Closed for S1 evidence** — default `clean verify` passed and the surefire negative probe failed as expected; exclusion restored | Backend/Platform; no further S1 action |
| R-10 | `postgres:16` digest unpinned. | **Open** — inherited PA0 residual | Platform/CI; supply-chain hardening, no guessed digest |
| R-12 | Shared-container test isolation. | S1 container tests passed; method-local keys, exact sets and `finally` cleanup are implemented. Cross-slice isolation remains reviewable | Backend/QA; S2/S3 review |
| R-13 | V001 checksum not frozen until S1 acceptance; S2/S3 may correct V001 only with prior assertions rerun. | **Open** — S1 is not accepted yet | Backend/Controller |
| S2 | Idempotency/Outbox behavior matrix. | Deferred; not implemented or claimed | S2 implementation/review |
| S3 | Audit/security behavior matrix. | Deferred; not implemented or claimed | S3 implementation/review |
| Q17 | Flyway role provisioning, schema ownership transfer, least-privilege grant execution. | Deferred; not guessed | Platform/Database/Security |
| Q23 | Function/role provisioning order and whether the deployment role can create `SECURITY DEFINER` functions. | Deferred; not guessed | Platform/Database/Security |
| CI | GitHub Actions execution. | **Not run**; no CI-green claim | Controller/Platform |

The Testcontainers superuser `onecall` is a test fixture, not production provisioning. No
production role, credential, region, capacity, retention or SLO value is claimed. The
`postgres:16` digest remains unpinned, and production Q17/Q23 provisioning is deferred.

## 15. Behavior deferred to S2/S3

S1 does not implement or claim:

- the S2 `NEW`/`REPLAY`/`CONFLICT`/`ALREADY_COMPLETED`/committed-`NEW`/concurrent-winner
  matrix, same-transaction rollback matrix, or Outbox replay/conflict/denylist behavior;
- the S3 audit append-only trigger behavior, high-risk reason matrix, runtime write denial,
  caller rollback, hostile `search_path` defense, or full owner/`PUBLIC` execution-boundary
  behavior;
- any V002-V009 table, function, schema, role or domain service.

V001 is complete as a single runnable migration and its S1 evidence has passed the controller's
exact-JDK validation, but it is not accepted until independent code review and the S1
acceptance checkpoint complete.

## 16. Completion status

**S1 IMPLEMENTATION EVIDENCE COMPLETE — READY FOR INDEPENDENT CODE REVIEW.**

The controller completed the exact-JDK validation on Temurin `21.0.13+9` in WSL Ubuntu-22.04
with system Maven at `/mnt/d/tools/maven/bin`, the Maven local repository at
`/mnt/d/tools/m2` and PostgreSQL `16.15`:

- the default `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 clean verify` passed 10 tests with zero failures, errors and skips;
- the boot jar started `OneCallApplication` and exited 0;
- the fat-jar inspection found no `spring-security`, `flyway`, `jooq`, `HikariCP`, `postgresql`
  or `testcontainers` under `BOOT-INF/lib`;
- `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 validate` passed;
- the container profile `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 -Pcontainer-smoke verify` passed exactly 3 tests with
  zero failures, errors and skips; V001 applied as version `001`, `validate()` passed, repeat
  migrate applied zero, and the isolated V901 fixture reached its sentinel and rolled back;
- both required negative probes failed as expected and were restored.

This artifact is the S1 implementation evidence record. It does **not** claim independent code
review, S1 acceptance, GitHub Actions success, CI-green status or production acceptance. The
next gate is independent code review of the S1 implementation; this task does not enter it.

- Completion status: **S1 implementation evidence complete; S1 not accepted; independent code review pending.**
- Artifact: `docs/gateflow/pa1-v001-persistence-foundation-s1-implementation.md`.
- Files modified by this evidence-completion task: this artifact only.
- Files modified by validation fix 1: `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` only.
- No commit, push, PR, merge, staging or further gate was performed by this task.

---

- Task label: `pa1-v001-s1-evidence-ds-20261027-02`
- Provider: `ds` / DeepSeek-V4.1-Flash
- Artifact: `docs/gateflow/pa1-v001-persistence-foundation-s1-implementation.md`
- Validations: controller exact-JDK default and container validation passed; both negative probes passed and were restored; this task performed read-only hash and scope checks
- Status: **COMPLETE EVIDENCE — READY FOR INDEPENDENT CODE REVIEW; S1 NOT ACCEPTED; NO CI OR PRODUCTION-ACCEPTANCE CLAIM**
