# PA2 V002 S3 fix 1 — granted-column membership `FOR UPDATE` probes and truthful last-admin evidence

- Task label: `pa2-v002-s3-fix1-mimo26flash-20261027-01`
- Work unit: `PA2-v002-identity-organization-employee-foundation`
- Gate: `S3 fix` (independent code review accepted exactly two Low findings; fix only those; no re-review, no S3 acceptance, no aggregate)
- Binding controller: `docs/gateflow/project-execution-control.md` S3 review row
- Review (read-only): `docs/reviews/code-review-20261027-192100.md`, SHA-256 `F9CEC7FDEEE20F85D73D926A8255133EA23B734AB8A92C263BAAA5BF252C29B3` (unchanged by this task)
- Plan/design unchanged and binding: `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md`
- Date: 2026-10-27 (Asia/Shanghai)
- Author: controller-dispatched MiMo 2.6 Flash author returning for one bounded Gateflow fix
- Artifact: this file (`docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-1.md`)

## 0. Conclusion

**COMPLETE — this bounded fix is author-complete; `F-01` and `F-02` are both `已修复` (fixed).** Exactly the two controller-accepted findings were corrected in exactly two allowed paths plus this new artifact. The exact 18 `@Test` set, V001/V002, CI, README, POM, production code and all plan/control/review/S1/S2 documents are unchanged. No stage, commit, push, PR, merge, re-review, S3 acceptance, aggregate review or production acceptance was performed.

## 1. Accepted findings and disposition

| Finding | Severity | Accepted content (review §2) | This fix |
| --- | --- | --- | --- |
| `F-01` | Low (test-evidence, catalog-compensated) | The `customer_iam_role` membership `FOR UPDATE` probe projected ungranted `id`, so `42501` could arise at column `SELECT` before the row-lock check; `subject_number_role` had no membership `FOR UPDATE` probe at all | Both cross roles now execute a membership `FOR UPDATE` negative probe that projects and filters only cross-granted columns (`SELECT account_id … WHERE account_id = ? FOR UPDATE`), so the exact `42501` isolates the missing row-lock/UPDATE privilege |
| `F-02` | Low (evidence-only) | The S3 evidence claimed the final-admin checks ran with an empty global enabled-admin set and that pairs were cleaned "after each success"; the code actually runs those checks with the retained admin as the sole enabled admin (global count = 1) and cleans after them; no explicit empty-set re-assert before the demote pair | Evidence corrected (§5/§8/§11/§13 of the S3 implementation artifact) to the truthful ordering, and an explicit `assertNoLeftoverEnabledAdministrators()` added after first-pair cleanup and immediately before committing the demote pair |

Binding inputs read: `docs/gateflow/project-execution-control.md` (S3 review / S3 fix row), `docs/reviews/code-review-20261027-192100.md` (F-01/F-02), `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-implementation.md`, `docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md` §7.3/§8, `docs/14-production-data-model-and-flyway-schema-spec.md` §4.11/§9.9, V002 migration grant block, `PostgresContainerIT.java`.

## 2. Exact changes — before/after locations

Allowed write #1: `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` (edited; CRLF preserved; content-only diff vs HEAD `672/3`, i.e. `667/3` from S3 plus 5 added lines from this fix).

### 2.1 F-01 — membership `FOR UPDATE` probes project/filter only granted columns

Before (original S3 numbering, review-cited):

- `:1295-1298` (`customer_iam_role`): `SELECT id, organization_id, account_id, role, status FROM organization_access.organization_membership WHERE account_id = ? FOR UPDATE` — `id` is outside the cross-module grant (`organization_id, account_id, role, status`), so `42501` could be raised at column `SELECT` before the row-lock/UPDATE check.
- `:1344-1346` (`subject_number_role`): only a plain undeclared-`id` read; no membership `FOR UPDATE` probe existed.

After (current numbering):

```java
// customer_iam_role — IT:1295-1298 (SQL at :1296-1297)
expectSqlQueryFailure(connection, "42501",
        "SELECT account_id FROM organization_access.organization_membership "
                + "WHERE account_id = ? FOR UPDATE",
        accountId);
```

```java
// subject_number_role — IT:1347-1350 (SQL at :1348-1349), inserted after the
// preserved plain undeclared-id read at :1344-1346
expectSqlQueryFailure(connection, "42501",
        "SELECT account_id FROM organization_access.organization_membership "
                + "WHERE account_id = ? FOR UPDATE",
        accountId);
```

Preserved unchanged in the same helper (`assertCrossModuleRuntimePrivileges`, `:1274-1358`):

- `customer_iam_role`: organization `FOR UPDATE` probe on granted `id` (`:1290-1291`), plain undeclared-`id` membership read denial (`:1292-1294`), undeclared-column read/`UPDATE`/`INSERT`/`DELETE`/employee-domain denials.
- `subject_number_role`: customer_account `FOR UPDATE` (`:1338-1339`), organization `FOR UPDATE` (`:1342-1343`), undeclared-column read denials and cross-write denials (`:1351-1355`).
- All positive cross-module reads, both `switchLocalRole` blocks, schema-`CREATE` and guard-boundary helpers untouched.

Isolation basis (frozen V002, read-only): `V002` grants `SELECT (organization_id, account_id, role, status)` on `organization_access.organization_membership` to both `customer_iam_role` and `subject_number_role` and no `UPDATE` on that table to either role. `expectSqlQueryFailure` (`IT:1758-1779`) asserts exact SQLSTATE equality and calls `fail(...)` if the statement succeeds; savepoint rollback always runs.

### 2.2 F-02 — explicit demote-pair precondition

Before (original numbering): after the final-admin checks for pair 1, `cleanupCommittedRaceRows(...)` at `:1534-1536` was followed immediately by the demote pair creation (`:1538`) and `commitEmployees` (`:1542`), with no re-assert between them. The only empty-set preconditions were at method start (`:1485`) and before the concurrent family (`:1604`).

After (current numbering), inside `assertLastEnabledAdminSequentialFailClosedAndRevisionIncrements` (`:1486`):

```java
// IT:1538-1547
cleanupCommittedRaceRows(
        List.of(), List.of(), List.of(), List.of(), List.of(),
        List.of(keepAdmin, disableTarget));
assertNoLeftoverEnabledAdministrators();          // NEW — IT:1541

UUID demoteKeepAdmin = UUID.randomUUID();
UUID demoteTarget = UUID.randomUUID();
allEmployeeIds.add(demoteKeepAdmin);
allEmployeeIds.add(demoteTarget);
commitEmployees(List.of(demoteKeepAdmin, demoteTarget));
```

Empty-set precondition call sites are now exactly three: `:1489` (before the sequential family), `:1541` (new — after first-pair cleanup, immediately before the demote pair), `:1609` (before the concurrent family). Final-admin disable/demote checks (`:1523-1524`, `:1575-1576`) run while the retained admin is the sole enabled admin; cleanup happens only after those checks.

### 2.3 F-02 — evidence wording (allowed write #2)

`docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-implementation.md` (before SHA-256 `2B684EFE…1F45`, after this fix-1 `EBCD9549…7A71A`; further revised only for F-03/F-04 by S3 fix 2 — current hash is recorded in `...-s3-fix-2.md`):

| Section / line | Before | After |
| --- | --- | --- |
| §4 `:56` (IT hash row) | `418D3640…1C07EB` / LF `3F606796…60870A`, action `edited` | `0CEB33BA…0D8C4D8` / LF `97847740…DBD380`, action `edited (S3 fix 1)` |
| §4 `:71` (CR accounting) | IT `5400/5400` | IT `5405/5405` |
| §5 `:79` (privilege-method row) | "undeclared column reads and `FOR UPDATE` `42501`" (no probe-isolation claim) | states both cross roles' membership row-lock probes project/filter only cross-granted columns (`SELECT account_id … WHERE account_id = ? FOR UPDATE`), so `42501` isolates the missing row-lock/UPDATE privilege rather than a missing SELECT |
| §5 `:80` (last-admin row) | "after each success the method-local pair is cleaned so the final-enabled-admin checks run with an empty global enabled-admin set (precondition asserted)" — false | empty set asserted before the sequential family and again immediately before committing the demote pair; each final check runs while the retained admin is exactly the sole enabled admin (global count = 1); pair cleaned only after those checks; concurrent family precondition also stated |
| §8 `:141` (denial matrix) | "cross `FOR UPDATE` denied (no cross-owner row-lock privilege)" | same claim plus "both roles' membership row-lock probes projecting/filtering only granted columns so the `42501` proves no cross-owner row-lock/UPDATE privilege" |
| §11 `:175` (calibration 2) | "assert an empty enabled-admin set before each scenario family" (no re-assert before demote pair) | asserts empty set before the sequential and concurrent families; final checks run with the retained admin sole; first pair cleaned after those checks; re-assert immediately before committing the demote pair; cleanup runs after the checks |
| §13 `:196` | (absent) | Post-review update paragraph recording both fixes and pointing to this artifact |

No false claim of final checks running on an empty set remains anywhere in the artifact (scan of `empty`/`precondition`/`cleaned` confirmed).

## 3. Validation commands and exact results

Environment: WSL2 Linux `6.18.33.2-microsoft-standard-WSL2`; Temurin JDK `21.0.13+9` (`JAVA_HOME=/opt/jdk21`); Apache Maven `3.9.9` (`/opt/maven-3.9.9/bin/mvn`); Docker server `29.1.3`; floating `postgres:16` → PostgreSQL `16.15`; Testcontainers `2.0.5`; Flyway `12.4.0`. All Maven commands run from `backend/`.

| # | Check | Command | Result |
| --- | --- | --- | --- |
| 1 | Validate | `mvn -B -ntp validate` | exit `0`; Enforcer rules 0–4 pass; `BUILD SUCCESS` |
| 2 | Default build | `mvn -B -ntp clean verify` | exit `0`; surefire **10** tests, `failures=0 errors=0 skipped=0` |
| 3 | Focused F-01 method | `mvn -B -ntp -Pcontainer-smoke verify -Dit.test='PostgresContainerIT#runtimeAndCrossModulePrivilegesAreExactAndWritesDenied'` | exit `0`; failsafe **1**, `skipped=0 failures=0 errors=0`; PG `16.15` — proves both membership `FOR UPDATE` probes still get exact `42501` under real grants |
| 4 | Full container (run 1) | `mvn -B -ntp -Pcontainer-smoke verify` | exit `0`; failsafe **18**, `skipped=0 failures=0 errors=0`; `Successfully applied 2 migrations ... now at version v002`; PG `16.15` |
| 5 | Focused last-admin run 1 | `mvn -B -ntp -Pcontainer-smoke verify -Dit.test='PostgresContainerIT#lastEnabledAdminTransactionIsSerializedAndFailsClosed'` | exit `0`; failsafe **1**, `failures=0 errors=0 skipped=0` |
| 6 | Focused last-admin run 2 | same (separate execution) | exit `0`; failsafe **1** |
| 7 | Focused last-admin run 3 | same (separate execution) | exit `0`; failsafe **1** |
| 8 | Full container (run 2, final) | `mvn -B -ntp -Pcontainer-smoke verify` | exit `0`; failsafe **18**, `skipped=0 failures=0 errors=0`; PG `16.15`; V001→V002 applied (final failsafe XML = the 18-name set) |
| 9 | Boot smoke | `timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` | exit `0`; log contains `Started OneCallApplication` |
| 10 | Fat-jar firewall | `jar tf ...` + forbidden-lib grep | `FIREWALL_OK`; 27 `BOOT-INF/lib/` entries; no `spring-security`/`flyway`/`jooq`/`HikariCP`/`postgresql`/`testcontainers` |
| 11 | Exact set equality | Python parse of `@Test`, CI `expected`, failsafe XML | source `18` = CI `18` = XML `18`; sets equal; `tests=18 skipped=0 failures=0 errors=0`; zero `@Disabled`/`Assumptions`/`@Ignore` in the IT |
| 12 | V001 frozen | `git cat-file blob HEAD:...V001...` → SHA-256 | `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045` (matches frozen) |
| 13 | V001 CR-normalized diff | `git diff --ignore-cr-at-eol --exit-code HEAD -- ...V001...` | exit `0` |
| 14 | V002 frozen | `sha256sum` | `534A0A06C9520856EF555275100E8B8A194DE80556B336F6ED4C984C3D9795D5` (unchanged) |
| 15 | Forbidden-scope diff | `git diff --ignore-cr-at-eol --exit-code HEAD -- backend/pom.xml backend/src/main/java backend/src/test/java/com/onecall/archtests/architecture` | exit `0` |
| 16 | CI/README/POM untouched | `sha256sum` vs S3/review records | CI `1DC020BA…C0E3B`, README `451FC79E…A3BDE`, POM `FBC2D9EC…DDA5` — all identical to the pre-fix records |
| 17 | Review artifact untouched | `sha256sum` | `F9CEC7FDEEE20F85D73D926A8255133EA23B734AB8A92C263BAAA5BF252C29B3` (matches control record) |
| 18 | Scoped whitespace | `git -c core.whitespace=cr-at-eol diff --check HEAD -- <the two edited tracked paths>` | exit `0`, zero diagnostics |
| 19 | Trailing whitespace / conflict markers | CR-aware scan of both edited files; marker grep | zero trailing-whitespace lines; no conflict markers (IT `CR=5405=LF`; artifact `CR=0`) |
| 20 | Content-only inventory | `git diff --ignore-cr-at-eol --numstat HEAD` | CI `4/2`, README `7/5`, IT `672/3`, controller control doc `3/1` (controller-owned, excluded); untracked: S3 implementation artifact + review artifact (+ this new artifact) |

Git note: the WSL worktree pointer (`gitdir: D:/codex-call/.git/...`) cannot be resolved by plain `git`; all git operations used the explicit `--git-dir=/mnt/d/codex-call/.git/worktrees/codex-call2 --work-tree=.` form; no Windows Git fallback and no `.git`/`.claude` path was edited. One intermediate Maven invocation from the worktree root failed with "no POM in this directory" (wrong CWD) and was re-run successfully from `backend/`; no test failed at any point.

## 4. F-01 isolation proof (disposable PostgreSQL 16.15 probe)

A disposable `postgres:16` container (server PostgreSQL `16.15`) was started solely to prove the corrected probes isolate the row-lock denial, then removed (`docker rm -f s3fix1-probe`); no repository state was changed.

Setup replicated the frozen grant shape: `GRANT SELECT (organization_id, account_id, role, status) ON organization_access.organization_membership TO customer_iam_role, subject_number_role;` with no `UPDATE` grant to either role, and one seeded row.

| Role | Statement | Result |
| --- | --- | --- |
| `customer_iam_role` | `SELECT account_id … WHERE account_id = ?` (no `FOR UPDATE`) | **succeeds** (SELECT privilege on projected/filter columns is granted) |
| `customer_iam_role` | `SELECT account_id … WHERE account_id = ? FOR UPDATE` | `ERROR: 42501: permission denied for table organization_membership`, `LOCATION: aclcheck_error, aclchk.c:2812` |
| `subject_number_role` | `SELECT account_id … WHERE account_id = ?` (no `FOR UPDATE`) | **succeeds** |
| `subject_number_role` | `SELECT account_id … WHERE account_id = ? FOR UPDATE` | `ERROR: 42501: permission denied for table organization_membership`, `LOCATION: aclcheck_error, aclchk.c:2812` |
| `subject_number_role` (control) | old-style `SELECT id … FOR UPDATE` (ungranted `id`) | same `42501` message, but raised at column `SELECT` — demonstrating the pre-fix masking |

Because the plain projection/filter query succeeds under the same role, the `42501` on the `FOR UPDATE` variant can only originate from the row-lock/UPDATE authorization check, not from a missing SELECT. Combined with `expectSqlQueryFailure`'s exact SQLSTATE assertion and the green focused run in the real V002 container, both `customer_iam_role` and `subject_number_role` membership `FOR UPDATE` probes are behaviorally isolated and still exact `42501`.

## 5. F-02 precondition proof (code + evidence)

- Code: `assertNoLeftoverEnabledAdministrators()` is now called at `IT:1489` (before sequential family), `IT:1541` (after first-pair cleanup, immediately before `commitEmployees` of the demote pair at `IT:1547`) and `IT:1609` (before concurrent family). The helper (`IT:1668-1676`) asserts the global `role='ADMIN' AND status='ACTIVE'` count is exactly zero, so pair independence is executable: leftover enabled admins fail the re-assert instead of corrupting the demote scenario's count.
- Evidence: the S3 implementation artifact §5 `:80` now states the empty set is asserted before the sequential family and again before the demote pair, that final checks run while the retained admin is the sole enabled admin (global count = 1), and that cleanup happens after those checks; §11 `:175` matches; §13 `:196` records the post-review correction. No section claims final checks run on an empty set.

## 6. Changed-file inventory and SHA-256

| # | Path | Action | SHA-256 before | SHA-256 after |
| --- | --- | --- | --- | --- |
| 1 | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | edited | `418D3640012D5DA6DBB27DE794C6C03A022D94198D2D5EE6401914B2BF1C07EB` (CRLF) / `3F6067962891926FC7A0E8AB7578809F76AA9166D91C1F252C5E526D3660870A` (LF) | `0CEB33BAFA033128E4206F862CD03A5C8B85EEF5B0006B9D063CC4AEC0D8C4D8` (CRLF) / `978477400BD8DDC0A9E4728BCEB31BA80E03C0A9DEB993BA9FB93C8AC1DBD380` (LF) |
| 2 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-implementation.md` | edited | `2B684EFEBC37A27008F1B1E1F86C1EEBE11AF6025DF3D0734981E7DEF37AF1F45` | `EBCD9549CC4119A3057D764F657F4AFFD63989656D1ED8F50F3ADE418147A71A` (fix-1 after-hash; superseded for the F-03/F-04 lines by S3 fix 2 — see `...-s3-fix-2.md`) |
| 3 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-1.md` | new (this artifact, LF) | n/a | recorded in the task return report (self-hash not embedded) |

Uniform CRLF worktree preserved for the IT (`CR=LF=5405`); the S3 implementation artifact remains LF (`CR=0`).

## 7. Scope, no-goal-drift and non-actions

No goal drift:

- No V001/V002/V003+ edit, no grant/role/function/schema/sequence/trigger change; V002 hash `534A0A06…9795D5` unchanged; V001 blob `946A0A13…CF81045` and CR-normalized diff unchanged.
- No CI, README, POM, production Java, application resource, `BootstrapDependencyBoundaryTests` or arch-test change: `git diff --ignore-cr-at-eol --exit-code HEAD` over `backend/pom.xml`, `backend/src/main/java` and the architecture tests exits `0`; CI/README/POM content hashes identical to their pre-fix records.
- No plan, control, review, S1 or S2 document edited (review artifact hash unchanged); the controller control document diff is controller-owned and excluded.
- Exact `@Test` set/count stays **18** with zero skips; no assertion from S1/S2 was weakened; no new test method, helper or database object added beyond the single re-assert call.
- Stop condition: none triggered — no V002/grant/schema/CI/README defect surfaced, so no forbidden file needed to change.

Non-actions: no stage, no commit, no push, no PR, no merge, no re-review, no S3 acceptance, no aggregate review, no checkpoint claim, no GitHub Actions run, no production acceptance. Author completion is not acceptance; fresh DeepSeek re-review owns the next gate.

## 8. Residual risks

| ID | Residual | Classification | Owner / destination |
| --- | --- | --- | --- |
| RR-01 | F-01/F-02 closed by this fix; prior RR-01/RR-02 from the review are resolved pending fresh re-review confirmation | Fixed-pending-re-review | Backend/QA; S3 re-review |
| RR-03 (inherited) | Floating `postgres:16` tag (observed 16.15), no digest pin | Inherited (plan R-11) | Platform/CI supply-chain hardening |
| RR-04 (inherited) | Concurrency timing sensitivity; mitigated by bounded waits and three focused repetitions (all green here) | Bounded test risk (plan R-14) | Backend/QA |
| RR-05 (inherited) | Global enabled-admin precondition fails closed if another suite commits an enabled ADMIN; the new `IT:1541` re-assert extends the same fail-closed property between pairs | Test-only fail-closed (RSK-S3-01) | Backend/QA |
| RR-08 (inherited) | GitHub Actions not run at this gate; local green is not production acceptance | Gate residual (plan R-16) | Phaseflow/CI |

## 9. Final finding status

| Finding | Status | Basis |
| --- | --- | --- |
| `F-01` cross-module membership `FOR UPDATE` isolation | **已修复 (fixed)** | `IT:1295-1298` (customer) and `IT:1347-1350` (subject) both run `SELECT account_id … WHERE account_id = ? FOR UPDATE` under exact-`42501` assertions; disposable PG16.15 probe shows plain granted SELECT succeeds and only `FOR UPDATE` is denied (`42501` at `aclchk.c:2812`) for both roles; focused method and full 18-suite green |
| `F-02` evidence wording + executable demote precondition | **已修复 (fixed)** | Explicit `assertNoLeftoverEnabledAdministrators()` at `IT:1541` between first-pair cleanup and demote-pair commit; S3 evidence §4/§5/§8/§11/§13 corrected to the truthful ordering (empty-set asserts before sequential and concurrent families plus the demote pair; final checks with the retained admin as sole enabled admin; cleanup after checks); no empty-set-during-final-checks claim remains; three focused last-admin runs green |

## 10. Completion status

- Completion: **author_code_complete = true; bounded S3 fix COMPLETE.**
- Validation: validate, default 10/10, full container 18/18 (twice), focused runtime method 1/1, focused last-admin 1/1 × 3, boot smoke, fat-jar firewall, exact 18-set equality, frozen V001/V002 hashes, forbidden-scope diff, scoped whitespace, CI/README/POM/review hash checks, disposable isolation probe — all green.
- This fix did not stage, commit, push, open/update a PR, merge, re-review, accept S3 or enter aggregate review, and claims no GitHub Actions or production acceptance.
- Artifact path: `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s3-fix-1.md`.
