# PA2 V002 Identity, Organization and Employee Foundation — S3 Implementation Evidence

- Task label: `pa2-v002-s3-impl-mimo26flash-20261027-01`
- Work unit: `PA2-v002-identity-organization-employee-foundation`
- Gate: `S3 implementation` (approved slice S3 only; author evidence; not a self-review)
- Slice: S3 — exact least privilege and last-enabled-admin transaction protection
- Date: 2026-10-27
- Author/provider: controller-dispatched implementation author (MiMo 2.6 Flash implementation author)
- This artifact is evidence written by the S3 author. It does **not** accept the checkpoint, does **not** perform S3 review, and does **not** enter aggregate review.

## 1. Scope and stop statement

Implemented and validated exactly the approved S3 slice:

1. Added exactly two container `@Test` methods, preserving the sixteen S1+S2 methods unchanged.
2. Proved runtime and cross-module least privilege under the actual runtime roles: owned-table SELECT/INSERT success; exact granted UPDATE columns success; undeclared UPDATE columns, DELETE and TRUNCATE denied `42501`; cross-module exact granted columns readable; undeclared column reads and `FOR UPDATE` denied `42501`; cross-module writes denied; customer/organization/employee domains separate; exact schema USAGE with no CREATE; guard-function EXECUTE only to the owning runtime role and not PUBLIC.
3. Proved the exact last-enabled-admin serialization protocol with transient SQL (no database function): advisory lock `pg_advisory_xact_lock(hashtextextended('operations_iam.admin_mutation', 0))`, target row `FOR UPDATE` under `operations_iam_role`, enabled-admin count, conditional revision update; two-admin disable/demote commits with revision+1; final enabled ADMIN disable and demote each raise stable `LAST_ENABLED_ADMIN` and change nothing; two concurrent admin mutations serialize on the advisory lock (first commits, second observes the new count and fails closed); final enabled-admin count stays at least one; `operations_iam_role` `FOR UPDATE` feasibility reasserted with its column-level UPDATE.
4. Updated only the CI expected failsafe set/count to 18 and the README final V002 behavior/count/evidence boundary.

**Did not:** commit, stage, push, open a PR, merge, self-review, accept S3, enter aggregate review, edit V001/V002, edit `pom.xml`, edit application Java, edit `BootstrapDependencyBoundaryTests`, edit design/plan/control/review/S1/S2 docs, add any grant/role/function/service/schema/sequence/V003+ object, add a production last-admin function, or weaken any prior S1/S2 assertion.

## 2. Binding inputs read and control-document status

Read at implementation time:

- `docs/gateflow/project-execution-control.md` — PA2 goal / current gate `S3 implementation` (read-only; not modified).
- `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md` §7.3, §8.1, §8.3–§8.7, §10–§12 (binding).
- `docs/14-production-data-model-and-flyway-schema-spec.md` §4.11, §9.9 (T-S1-09), §10.8, §10.8.1 and cited grant/guard text.
- Accepted S1/S2 migration, tests, evidence and reviews: `pa2-v002-identity-organization-employee-foundation-s1-implementation.md`, `pa2-v002-identity-organization-employee-foundation-s2-implementation.md`, S1/S2 fix notes and accepted checkpoint records.
- `V001` / `V002` migrations, `PostgresContainerIT.java`, `pom.xml`, `.github/workflows/ci.yml`, `backend/README.md`.

The control document, plan, `docs/14` and the S1/S2 artifacts were **not modified** by this task. `git status` shows only the three allowed tracked edits plus the new artifact path below.

## 3. Base, prior checkpoint and frozen hashes

| Item | Value |
| --- | --- |
| Controller HEAD | `d7125687f587c8f3b038d561298c7dbd2473eaa6` |
| Accepted S1 checkpoint | `8d322814cf8d9aa36ce1d618222e39441afbd010` |
| Accepted S2 checkpoint | `7d00831d24c079c50bd3f24a5261d73dcc72238f` |
| V001 HEAD blob SHA-256 (re-verified) | `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045` |
| V001 HEAD-blob vs worktree CR-normalized diff | exit `0`, no content change |
| V002 frozen SHA-256 | `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5` |
| V002 SHA-256 before this task | `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5` |
| V002 SHA-256 after this task | `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5` |
| `pom.xml` / `src/main/java` / `BootstrapDependencyBoundaryTests` diff vs HEAD | exit `0` (unchanged) |

V001 is not edited. The worktree V001 file remains the CRLF checkout baseline (raw on-disk hash differs from the HEAD blob; the authoritative check is the HEAD blob hash plus the CR-normalized diff, per plan §8.7).

## 4. Changed paths, hashes and content-only diff manifest

Allowed tracked edits (uniform CRLF worktree; LF in the git index under `core.autocrlf=true`):

| # | Path | Action | On-disk SHA-256 (CRLF) | LF-normalized SHA-256 |
| --- | --- | --- | --- | --- |
| 1 | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | edited (S3 fix 1) | `0ceb33bafa033128e4206f862cd03a5c8b85eef5b0006b9d063cc4aec0d8c4d8` | `978477400bd8ddc0a9e4728bceb31ba80e03c0a9deb993ba9fb93c8ac1dbd380` |
| 2 | `.github/workflows/ci.yml` | edited | `1dc020baea6a37e2eddf7c783ad39816c192f5360389f74cef899aa5ad8c0e3b` | `96e6bcb3e659aaec02cfbeb2ea2c03e509fff3bac0c2e9ecebb159fa7791bcee` |
| 3 | `backend/README.md` | edited | `451fc79ee0b35ad1c66021269b93601fc9b0e527d34792e86c8dbb56f2ca3bde` | `3d9d3c86cffa6624351f607954a89eabcf4ee0bf0b1304a4685fc108c228b78c` |
| 4 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-implementation.md` | new (this artifact, LF) | recorded in the task return report | recorded in the task return report |

Content-only diff manifest (`git diff --numstat` against HEAD):

| Path | Added | Deleted |
| --- | --- | --- |
| `.github/workflows/ci.yml` | 4 | 2 |
| `backend/README.md` | 7 | 5 |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | 672 | 3 |

The IT content-only row is the post-fix total `672/3` = `667/3` from the S3 slice plus 5 added lines from S3 fix 1. The three IT deletions are only the class-javadoc tail lines rewritten to mention the S3 methods; all sixteen S1/S2 `@Test` methods and their helpers are otherwise untouched (additions only, plus that javadoc wording). No file outside the four allowed paths changed.

CRLF baseline accounting: no repository-wide line-ending normalization was performed. The three tracked files remain uniform CRLF in this Windows checkout (CR count equals LF count: IT 5405/5405 after S3 fix 1, CI 192/192, README 109/109). Git stores LF (`i/lf`), so the content diff is line-ending-neutral. The new artifact is LF.

## 5. Scope implemented (methods and helpers)

Exactly two `@Test` methods added (the sixteen S1/S2 methods are preserved unchanged):

| Method | Evidence |
| --- | --- |
| `runtimeAndCrossModulePrivilegesAreExactAndWritesDenied` | As `customer_iam_role` / `organization_access_role` / `operations_iam_role`: SELECT+INSERT of all six owned tables; granted UPDATE columns succeed; undeclared UPDATE, DELETE, TRUNCATE `42501`; as `customer_iam_role` and `subject_number_role`: exact cross-module columns readable with `WHERE` restricted to granted columns, undeclared column reads `42501`, cross writes `42501`; organization, customer_account and organization_membership `FOR UPDATE` `42501`, where both cross roles' membership row-lock probes project and filter only cross-granted columns (`SELECT account_id … WHERE account_id = ? FOR UPDATE`, per the V002 column grant `organization_id, account_id, role, status` with no UPDATE), so the exact `42501` isolates the missing row-lock/UPDATE privilege rather than a missing SELECT; `organization_access_role` / `operations_iam_role` cannot read the other domains; runtime `CREATE TABLE` `42501` in each V002 schema with zero residual table; owning runtime role passes guard EXECUTE and reaches the trigger-context error, non-owning runtime roles get `42501`; catalog assertions for exact schema USAGE (no CREATE, PUBLIC denied) and guard EXECUTE only to the owning runtime role / not PUBLIC / not other runtime roles. The whole probe runs in one transaction and rolls back (method-local UUIDs/random 64-hex HMACs; no runtime DELETE/TRUNCATE for cleanup). |
| `lastEnabledAdminTransactionIsSerializedAndFailsClosed` | (1) `operations_iam_role` `SELECT … FOR UPDATE` succeeds on `employee_account` and the conditional column UPDATE succeeds (R-15 reassertion). (2) Sequential protocol with exact transient SQL: advisory lock, `FOR UPDATE`, enabled-admin count, conditional update or `DO … RAISE EXCEPTION 'LAST_ENABLED_ADMIN' USING ERRCODE = 'P0001'`; with two enabled admins, disable commits `ACTIVE|ADMIN|1` → `DISABLED|ADMIN|2` and demote commits `ACTIVE|ADMIN|1` → `ACTIVE|OPS|2`; the empty global enabled-admin set is asserted before this sequential family and again immediately before committing the demote pair (after the first pair's cleanup); each final enabled ADMIN disable/demote check runs while the retained admin is exactly the sole enabled admin (global enabled count = 1), and the method-local pair is cleaned only after those checks; final enabled ADMIN disable and demote each raise `P0001`/`LAST_ENABLED_ADMIN` via savepoint and change nothing (`ACTIVE|ADMIN|1` re-read). (3) Concurrent race: two connections, distinct `application_name`, empty global enabled-admin set asserted before the pair commits, winner runs the protocol first and holds the advisory lock, loser blocks (detected in `pg_stat_activity` with `wait_event_type='Lock'`, bounded 15s), winner commits, loser observes the new count and fails closed with `LAST_ENABLED_ADMIN`; final states `DISABLED|ADMIN|2` / `ACTIVE|ADMIN|1`, enabled count among the pair is `1`; migration-identity cleanup of method-local employees only, after those checks. |

Bounded private helpers only: cross-module/owned/schema/guard privilege assertion helpers, `expectSqlQueryFailure` (savepoint-scoped SELECT denials), `assertOperationsIamRoleCanForUpdateWithColumnUpdate`, sequential and concurrent last-admin scenarios, `assertNoLeftoverEnabledAdministrators`, `commitEmployees`, `runLastEnabledAdminMutation`, `expectLastEnabledAdminFailure`. No production function, no generic framework, no new database object.

## 6. Test methods and CI expected set

- `@Test` count in `PostgresContainerIT.java`: **18**.
- Failsafe report methods: exactly the 18 S1+S2+S3 names; set equality with `@Test` methods and with the CI `expected` set (verified by parsing the failsafe XML and the workflow text).
- CI change: two names appended to `expected`; test-count / set-mismatch messages changed `S2`→`V002`; `len(expected)` now requires 18. No step, job, permission, path filter, toolchain pin, boot smoke or firewall step was removed or weakened.
- README change: title/intro and container count 16→18 plus the two PA2 V002 S3 method descriptions and a final evidence-boundary bullet. Default 10-test count, toolchain pins, module-boundary table, firewall description and Modulith note unchanged.

Exact S3 method set (S2 set plus):

1. `runtimeAndCrossModulePrivilegesAreExactAndWritesDenied`
2. `lastEnabledAdminTransactionIsSerializedAndFailsClosed`

## 7. Commands, environment and exact results

Environment observed:

| Component | Observed |
| --- | --- |
| Host | Linux WSL2 6.18.33.2-microsoft-standard-WSL2 |
| JDK | Temurin `21.0.13+9` at `/opt/jdk21` |
| Maven | Apache Maven `3.9.9` at `/opt/maven-3.9.9` |
| Docker | Docker Engine server `29.1.3` |
| PostgreSQL image | floating `postgres:16` → server **PostgreSQL 16.15** |
| Flyway | `12.4.0` (Boot 4.1.2 BOM, test scope) |
| Testcontainers | `2.0.5` |

All commands run from `backend/` with `JAVA_HOME=/opt/jdk21` and `/opt/maven-3.9.9/bin/mvn` (the CRLF `./mvnw` and Windows `java.exe` remain unusable in this WSL shell, as recorded in S1/S2).

| Check | Command | Result |
| --- | --- | --- |
| Validate | `mvn -B -ntp validate` | exit `0`, `BUILD SUCCESS`; Enforcer rules pass |
| Default build | `mvn -B -ntp clean verify` | exit `0`; surefire **10** tests, `failures=0 errors=0 skipped=0` |
| Docker | `docker info` | exit `0`, server `29.1.3` |
| Full container suite | `mvn -B -ntp -Pcontainer-smoke verify` | exit `0`; failsafe **18** tests, `skipped=0 failures=0 errors=0`; applied V001→V002 (`Successfully applied 2 migrations ... now at version v002`); repeated migrate `No migration necessary`; PG **16.15** |
| Focused last-admin run 1 | `mvn -B -ntp -Pcontainer-smoke verify -Dit.test='PostgresContainerIT#lastEnabledAdminTransactionIsSerializedAndFailsClosed'` | exit `0`; failsafe **1** test, `skipped=0 failures=0 errors=0` |
| Focused last-admin run 2 | same command (separate execution) | exit `0`; failsafe **1** test, `skipped=0 failures=0 errors=0` |
| Focused last-admin run 3 | same command (separate execution) | exit `0`; failsafe **1** test, `skipped=0 failures=0 errors=0` |
| Boot smoke | `timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` | exit `0`; log contains `Started OneCallApplication` |
| Fat-jar firewall | `jar tf ... > target/jar-index.txt` + forbidden-library grep | `FIREWALL_OK`; 27 `BOOT-INF/lib/` entries; no `spring-security`/`flyway`/`jooq`/`HikariCP`/`postgresql`/`testcontainers` |
| V001 HEAD blob hash | Windows `git cat-file blob HEAD:...V001...` → SHA-256 | `946A0A13...CF81045` (matches frozen) |
| V001 CR-normalized diff | `git diff --ignore-cr-at-eol --exit-code HEAD -- ...V001...` | exit `0` |
| V002 hash | `sha256sum` | `534A0A06...9795D5` (unchanged) |
| Forbidden-scope diff | `git diff --exit-code HEAD -- backend/pom.xml backend/src/main/java` and `...BootstrapDependencyBoundaryTests.java` | exit `0` |
| Scoped whitespace | `git -c core.whitespace=cr-at-eol diff --check -- <three edited paths>` | exit `0`, zero diagnostics |
| New-artifact whitespace | `git -c core.whitespace=cr-at-eol diff --no-index --check /dev/null <artifact>` | exit `1` with zero diagnostic output |
| Conflict markers | scoped grep for `<<<<<<<`/`=======`/`>>>>>>>` | none |
| Trailing whitespace | CR-aware scan of the three edited files | zero lines |
| Exact sets | Python parse of CI `expected`, `@Test` names, failsafe XML | 18 = 18 = 18; CI set == XML set == `@Test` set |
| Skip scan | `@Disabled` / `Assumptions` / `assumeTrue` / `@Ignore` in the IT | all `0` |

## 8. Negative, privilege and concurrency evidence

Expected failures are savepoint-scoped and assert exact SQLSTATE and exact marker:

| Area | SQLSTATE / code | Exact expectations |
| --- | --- | --- |
| Undeclared owned UPDATE / DELETE / TRUNCATE | `42501` | all six owned tables; undeclared columns are the non-granted set from plan §6.5 / docs/14 §10.8 |
| Cross-module undeclared reads, `FOR UPDATE`, writes | `42501` | `customer_iam_role` and `subject_number_role` on organization/membership and customer reads outside the exact cross columns; cross `FOR UPDATE` denied, with both roles' membership row-lock probes projecting/filtering only granted columns so the `42501` proves no cross-owner row-lock/UPDATE privilege |
| Cross-domain table reads | `42501` | customer / organization / employee domains remain separate |
| Runtime schema `CREATE` | `42501` | `customer_iam`, `organization_access`, `operations_iam` probes; zero residual `s3_runtime_probe_forbidden` relation |
| Non-owning guard EXECUTE | `42501` | `subject_number_role`, `operations_iam_role`, `organization_access_role`, `customer_iam_role` on foreign guards |
| Owning guard invocation | not `42501`, message contains `trigger` | owning runtime role passes EXECUTE and reaches the trigger-context error (PostgreSQL 16.15 observed SQLSTATE `0A000`: `trigger functions can only be called as triggers`), savepoint-rolled-back |
| Last enabled admin | `P0001` + `LAST_ENABLED_ADMIN` | sequential final disable and final demote (both scenarios); concurrent loser after winner commit |

Concurrency evidence:

- Two connections; method-local UUIDs and random 64-hex HMACs; distinct `application_name` (`pa2-s3-admin-race-winner` / `pa2-s3-admin-race-loser`); loser `statement_timeout=20000`; future bound `30s`.
- Lock wait detected from `pg_stat_activity` (`application_name`, `state='active'`, `wait_event_type='Lock'`), bounded to 15s; no bare-sleep assertion.
- Winner holds `pg_advisory_xact_lock(hashtextextended('operations_iam.admin_mutation', 0))` through commit; loser blocks, then after the winner commits recounts enabled admins, sees the new count and raises `LAST_ENABLED_ADMIN` without writing.
- Final row states: winner target `DISABLED|ADMIN|2`; loser target `ACTIVE|ADMIN|1`; enabled-admin count among the pair is `1` (≥1 invariant).
- Sequential protocol also reasserts `operations_iam_role` `FOR UPDATE` feasibility under column-level UPDATE before the scenarios.
- Cleanup is migration-identity and exact: method-local employee ids only (via the existing `cleanupCommittedRaceRows` employee branch); uncommitted privilege probes roll back entirely; no runtime `DELETE`/`TRUNCATE`; no append-only mutation.

## 9. Hash, line-ending and whitespace evidence

- V001: HEAD blob `946A0A13...CF81045`; CR-normalized diff exit `0`; no worktree normalization.
- V002: `534A0A06...9795D5` before and after; untouched.
- Changed tracked files: uniform CRLF worktree (CR == LF), LF-equivalent content hashes recorded in §4; no mixed endings introduced.
- Scoped `git -c core.whitespace=cr-at-eol diff --check` on the three edited tracked paths: exit `0`, zero diagnostics.
- New artifact: LF, no trailing whitespace, no conflict markers; `git diff --no-index --check /dev/null <artifact>` reports only the expected exit `1` for a new file with zero diagnostic output.
- A global raw `git diff --check` is not authoritative in this CRLF Windows checkout (baseline CRLF and pre-existing documentation whitespace), per plan §8.4/§8.7; the scoped CR-aware check plus no-index new-file check is the fail-closed gate.

## 10. README/CI changes and documentation decision

- `.github/workflows/ci.yml`: only the failsafe expected set (two names) and its count/labels (`S2`→`V002`) changed. Other steps preserved.
- `backend/README.md`: title/intro and the 16→18 container count plus the two PA2 V002 S3 method descriptions and a final V002 evidence-boundary bullet. Default 10-test count, toolchain pins, module boundaries, Modulith note and firewall description are unchanged.
- No design/plan/control/review/S1/S2 document was modified. Only this one new evidence artifact was added.

## 11. Deviations, stop conditions and unresolved issues

1. **Bounded calibration 1 (cross-module `WHERE` column scope).** The first internal container run failed the privilege method with `permission denied for table organization_membership`: the positive cross-module reads used `WHERE id = ?`, but `customer_iam_role` / `subject_number_role` hold column-level SELECT only on `organization_id, account_id, role, status`. Correction: filter those reads (and the paired denial probes) on the granted `account_id` column. This matches the frozen grant matrix exactly; no grant or S1/S2 assertion changed.
2. **Bounded calibration 2 (enabled-admin set hygiene).** The first internal run failed the last-admin method with `expected LAST_ENABLED_ADMIN but the mutation succeeded`: sequential scenario 1 left its surviving enabled ADMIN in the table, so scenario 2's target was not the global last enabled administrator (and would likewise have polluted the concurrent count). Correction: assert an empty enabled-admin set before the sequential family and again before the concurrent family; within the sequential family, run each pair's final-admin checks while its retained admin is the sole enabled admin, clean the first pair only after those checks, then re-assert the empty set immediately before committing the demote pair; scope the final sequential count assertion to the scenario-2 pair. Cleanup remains migration-identity and method-local only and runs after the final-admin checks.
3. No stop condition was triggered: no V002/grant defect required a migration change, the last-admin protocol required no database function or service, no S1/S2 behavior was weakened, and no forbidden path was touched.
4. No unresolved implementation issue. Deferred inputs (Q2/Q17/Q23) remain deferred and were not guessed.

## 12. Residual risks

| ID | Residual | Classification | Owner | Destination |
| --- | --- | --- | --- | --- |
| RSK-S3-01 | The last-admin enabled-admin count is global to `operations_iam.employee_account`; scenarios assert an empty pre-set so method-local rows are the only enabled admins during the run. A future committed enabled ADMIN from another suite would fail the precondition rather than weaken the protocol. | Test-only fail-closed precondition | Backend/QA | Keep precondition; review if other suites begin committing enabled admins |
| RSK-S3-02 | Concurrency races are timing-sensitive; mitigated by two connections, `application_name`, `pg_stat_activity` lock-wait detection, `statement_timeout=20s`, `30s` futures and three separate focused repetitions. | Bounded test risk (plan R-14) | Backend/QA | Keep bounded waits; focused repetitions |
| RSK-S3-03 | Floating `postgres:16` tag (observed 16.15) remains unpinned. | Inherited (plan R-11) | Platform/CI | Supply-chain hardening; no digest guess |
| RSK-S3-04 | Q2 provider-subject rotation, Q17 migration provisioning and Q23 `SECURITY DEFINER` capability remain deferred; only frozen grant/guard behavior is tested. | Deferred input | Security/Platform/Database | Later design work units |
| RSK-S3-05 | The advisory/count/update protocol lives only in this IT as transient SQL (plan R-09); the future operations-iam service must implement the same order under Q17/Q23 constraints. | Deferred behavior (plan R-09) | operations-iam service | Later service slice |
| RSK-S3-06 | GitHub Actions was not run at this gate; local container/boot/firewall success is not production acceptance. | Gate residual (plan R-16) | Phaseflow/CI | Exact-head PR gate; no production claim |

## 13. Completion and non-actions

S3 implementation and author validation are **complete**: exactly 18 container methods green, the focused last-enabled-admin method green in three separate `tests=1` executions, default 10-test verify green, validate/docker/boot/firewall/hash/whitespace checks green, and V002/V001 unchanged.

No later gate, commit, stage, push, PR, merge, review, S3 acceptance or production acceptance was performed. This artifact reports to the controller for independent S3 review.

Post-review update (S3 fix 1, 2026-10-27): the two controller-accepted low findings from `docs/reviews/code-review-20261027-192100.md` were corrected here and in the IT — F-01 by making both cross roles' membership `FOR UPDATE` negative probes project and filter only cross-granted columns, F-02 by the explicit empty-set re-assert before the demote pair plus the corrected §5/§8/§11 wording above. Post-fix before/after locations, validation results, hashes and final finding statuses are recorded in `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-1.md`. The exact 18-method set, V002/V001, CI and README are unchanged.

## 14. No-goal-drift and exact changed-file list

No goal drift:

- No V003+ object, schema, role, grant, sequence or migration.
- No production service, SQL function (last-admin stays transient test SQL), generic trigger framework or application wiring.
- No POM/runtime dependency change.
- No UI/API change or production acceptance.
- No edit to V001/V002, `pom.xml`, production Java, `BootstrapDependencyBoundaryTests`, design/plan/control/review/S1/S2 documents, `.git` or `.claude`.

Exact changed paths:

1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (edited)
2. `.github/workflows/ci.yml` (edited)
3. `backend/README.md` (edited)
4. `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-implementation.md` (new)
