# PA6 V006 S1 Fix-2 Completion Report

- Task label: `pa6-v006-s1-fix2-mimo26flash-20261029-123903`
- Runtime / provider / model: Linux WSL2; Claude Code harness; MiMo-V2.6-Flash (fix author)
- Author role: S1 re-review fix author only (not plan author, controller, reviewer, or approver)
- Completion timestamp: `20261029-124524` (system clock via `date +%Y%m%d-%H%M%S`)
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Branch / base: detached HEAD at accepted plan commit `e5e00bfc775de180a25fd56cae98658f5a41852e`
- Run dir: `/tmp/sub-agents.pa6-s1-fix2.yDI8Ly`
- Binding re-review: `docs/reviews/code-review-20261029-122837.md` (Reviewer A, finding N-01, verdict pass-with-risks)
- Independent pass context: `docs/reviews/code-review-20261029-122838.md` (Reviewer B, verdict pass, no material finding)
- Binding controller adjudication: `docs/gateflow/project-execution-control.md` re-review S1 round 1 row (N-01 accepted, low; F-01 `部分修复`; fix direction: trial `granted_at` = grant `granted_at`)
- Frozen read-only manifest: `/tmp/sub-agents.pa6-s1-fix2.yDI8Ly/frozen.sha256`

## 1. Scope and non-goals

S1 fix round 2 only: apply the single controller-accepted re-review finding N-01 as a bounded test-fixture correction inside the existing `assertV006IssueFirstTrialSemantics` method. No new `@Test` method, no method rename (still exactly 41 with identical names and identical CI expected set). No V006 product-SQL change, no CI change, no README change, no S2 lifecycle/concurrency/cleanup work. No Git mutation, PR mutation, nested agent, self-review, or later-gate entry. V001-V006 migrations, CI, POM, application Java, plan/review/control artifacts, docs/12, docs/14, fix-1 artifact and both re-review artifacts remained read-only (frozen manifest verified before and after).

## 2. Accepted finding and final status

### N-01 (low, test evidence) — CLOSED (author-fixed; final status awaits re-review)

Pre-fix state (Reviewer A line evidence, `PostgresContainerIT.java:9300-9309`): the `validFromSubject` fixture passed `grantValidFrom=integrityBase`, `grantGrantedAt=integrityBase.plusSeconds(1)`, `trialGrantedAt=integrityBase`. The fixture therefore simultaneously violated both independent V006 `issue_first_trial` integrity predicates `v_grant.valid_from IS DISTINCT FROM v_grant.granted_at` and `v_grant.granted_at IS DISTINCT FROM v_trial.granted_at` (`V006__trial_entitlement_issuance_reservation_usage.sql:231-232`), so deleting only the `valid_from` predicate would leave the probe green.

Fix (single required fixture argument): the `validFromSubject` fixture's `trialGrantedAt` argument is now `integrityBase.plusSeconds(1)`, equal to grant `granted_at`. The fixture now violates only `valid_from = grant.granted_at`; grant/trial `granted_at` are equal, `expires_at` values remain equal (`+7d`), and every other equality in the corrupt-bundle helper input is retained. The comment above the probe was updated to state the isolation. The separate `grantTimeSubject` probe (`grant.granted_at ≠ trial.granted_at`) and `expiresAtSubject` probe are untouched. The V006 product SQL was not modified; the fixture remains method-local and whole-transaction rolled back with the existing zero-row post-rollback assertions.

Controller fat-jar count question (26 vs 27) and pre/post fix-1 byte-diff absence were adjudicated by the controller and are not in scope.

## 3. Changed paths (exactly three writes)

| Path | Action | SHA-256 |
| --- | --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | modified (fixture argument + comment only inside existing method; still 41 `@Test`) | post-fix-2: `f56badab52038fcb3fbc9f19218c536fb48ed3aec3269aa04d62a0170f71a1e3` (post-fix-1 was `f3dba46ee2c6db4ad5189cf8e55c0092671895f4607ee192663da05c629b2484`) |
| `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-s1-implementation.md` | updated (new §8 post-fix-2 status; §7/§6 historical claims unchanged) | written at completion |
| `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-s1-fix-2.md` | new (this report) | written at completion |

Unchanged by fix-2 (re-hashed/verified): V006 migration `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475`; V001-V005 match frozen S1 hashes; `.github/workflows/ci.yml` `39d71292433ba05e7e836bb09a35d2fcb130bea5a9b7d84ec9b5cc7b7a8fd4f5`; `backend/README.md` `a188b06886b714442f71fce6b5319940125f928e11bd62ab37e14217cb0dca60`. Tracked modified files outside the allowed set: none authored by this task. Pre-existing controller-owned `docs/gateflow/project-execution-control.md` edit was not touched.

## 4. Validation record (author environment)

Toolchain: Temurin 21.0.13+9 at `/opt/jdk21`, Maven 3.9.9 at `/opt/maven-3.9.9`. Evidence root: `/tmp/pa6-s1-fix2/`.

### 4.1 Focused iterating run

| Run | Command / filter | Result |
| --- | --- | --- |
| Focused (single existing method) | `mvn -B -ntp -Pcontainer-smoke verify -Dit.test=PostgresContainerIT#v006IssueFirstTrialGrantReplayConflictAndPolicySemantics` | **1 run, 0 fail, 0 err, 0 skip**, BUILD SUCCESS (PostgreSQL 16.15); log `/tmp/pa6-s1-fix2/focused.log` |

### 4.2 Final full container smoke (exactly one full run after the change was complete)

| Check | Result |
| --- | --- |
| `mvn -B -ntp -Pcontainer-smoke verify` | **BUILD SUCCESS** |
| Failsafe XML `/tmp/pa6-s1-fix2/container-smoke-final.xml` | `tests=41 failures=0 errors=0 skipped=0`, 41 `<testcase>` nodes, no failure/error/skipped nodes |
| Log `/tmp/pa6-s1-fix2/container-smoke-final.log` | `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0`; PostgreSQL **16.15** via `postgres:16` |
| source `@Test` / XML testcase / CI expected set | **41 == 41 == 41**, exact set equality (Python triple comparison) |
| Method names / count | unchanged; no additions; no renames; CI expected set untouched |

### 4.3 Secondary validations

| Check | Result |
| --- | --- |
| `mvn -B -ntp validate` | exit 0, Enforcer pass (`/tmp/pa6-s1-fix2/validate.log`) |
| `mvn -B -ntp clean verify` (default) | exit 0 — Surefire **10** run, 0 fail/err/skip (`/tmp/pa6-s1-fix2/default-verify.log`) |
| `docker info` | exit 0 |
| Boot smoke | `Started OneCallApplication in 1.245 seconds` (`/tmp/pa6-s1-fix2/boot.log`) |
| Fat-jar firewall | `jar tf` via `/opt/jdk21/bin/jar`: 27 `BOOT-INF/lib` entries; denylist grep (`spring-security\|flyway\|jooq\|HikariCP\|postgresql\|testcontainers`) zero matches (`/tmp/pa6-s1-fix2/jar-index.txt`) |
| V001-V006 SHA-256 | all match frozen S1 hashes; V006 unchanged `830ed14c…` |
| frozen manifest `/tmp/sub-agents.pa6-s1-fix2.yDI8Ly/frozen.sha256` | before and after: all listed entries `OK` |
| `git diff --check` on the fix paths | exit 0 (CRLF working-tree warning only; no whitespace errors) |

### 4.4 V006 object counts

Unchanged from accepted S1 (no migration edit): 7 tables / 84 columns / 50 named checks / 7 PK / 9 ordinary unique / 3 partial unique / 29 index objects / 15 immediate + 1 late FK / 19 defaults / 1 service + 7 trigger functions / 7 triggers.

## 5. Residual risks

- **Discriminating power of the corrected probe** was validated by static fixture/predicate analysis (single remaining mismatch is `valid_from` vs grant `granted_at`; the `grant.granted_at ≠ trial.granted_at` predicate now holds equal). A live predicate-deletion mutation probe (`V006:231` removed → method red) was not executed in this task because it would require a temporary product-SQL edit outside the authorized write set; the corrected fixture geometry makes that mutation observable by construction.
- Legacy V004/V005 concurrent TRUNCATE co-truncation of all seven `entitlement_metering` tables remains a controller-directed S2 review item (unchanged).
- Publication-guard draft→published UPDATE, fresh-grant same-source native `23505`, `order_id`/`installment_no` structural coupling, and `balance.grant_id` lookup-path self-proof remain as classified in fix-1 §5 (unchanged; outside N-01).
- Not run by this task: a second full container smoke after the final green (forbidden by protocol); exact-head GitHub Actions; production provisioning (Q17/Q23).

## 6. Gateflow classifications

- Author: S1 re-review fix author only.
- This artifact does **not** claim an accepted checkpoint, controller approval, re-review verdict, production readiness, or S2 entry.
- N-01 is author-fixed; final status awaits fresh independent re-review / controller adjudication.
- Next controller actions (outside this task): independent S1 fix-2 re-review / intake; later Gateflow gates.

## 7. Stop condition

S1 fix-2, bounded validation, and both Gateflow artifacts are complete. No Git mutation was performed. Stopped here without entering a later gate.
