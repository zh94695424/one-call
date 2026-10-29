# PA6 V006 S1 Fix-1 Completion Report

- Task label: `pa6-v006-s1-fix1-mimo26flash-20261029-113653`
- Runtime / provider / model: Linux WSL2; Claude Code harness; MiMo-V2.6-Flash (fix author)
- Author role: S1 code-review fix author only (not plan author, controller, reviewer, or approver)
- Completion timestamp: `20261029-122203` (system clock via `date +%Y%m%d-%H%M%S`)
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Branch / base: detached HEAD at accepted plan commit `e5e00bfc775de180a25fd56cae98658f5a41852e`
- Run dir: `/tmp/sub-agents.pa6-s1-fix1.5nPALu`
- Binding reviews: `docs/reviews/code-review-20261029-112307.md` (F-PA6-S1-01/02/03), `docs/reviews/code-review-20261029-112308.md` (R-A → F-04, R-C subsumed)
- Binding controller adjudication: `docs/gateflow/project-execution-control.md` code-review S1 row

## 1. Scope and non-goals

S1 code-review fix only: apply the four controller-accepted findings (F-01..F-04) as bounded test-evidence and README corrections. No V006 product-SQL change. No new `@Test` method and no method rename (still exactly 41). No CI method-set change. No S2 lifecycle/concurrency/cleanup work. No Git mutation, PR mutation, nested agent, self-review, or later-gate entry. V001-V006 migrations, CI, POM, application Java, plan/review/control artifacts, docs/12, and docs/14 remained read-only (frozen manifest re-verified).

Controller exclusions honored:
- Native fresh-grant `23505` source collision left as-is (no `TRIAL_DATA_INTEGRITY` remap).
- Publication-guard draft→published path left for S2.
- Harmless duplicate absence-list item left unchanged.
- Legacy V004/V005 table-wide TRUNCATE cleanup recorded below as an S2 review risk only (not implemented).

## 2. Changed paths (exactly four allowed writes)

| Path | Action | SHA-256 after fix |
| --- | --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | modified (still 41 `@Test`; fix delta inside existing methods/helpers) | `f3dba46ee2c6db4ad5189cf8e55c0092671895f4607ee192663da05c629b2484` |
| `backend/README.md` | modified (S1 evidence claims corrected) | `a188b06886b714442f71fce6b5319940125f928e11bd62ab37e14217cb0dca60` |
| `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-s1-implementation.md` | updated (post-fix validation + finding status) | `300e05e2a4135b6c444d3cff7074e605389f695790d2e3be9557da3a2035e3f6` |
| `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-s1-fix-1.md` | new (this report) | written at completion |

Original S1 hashes (pre-fix, from implementation report §2): PostgresContainerIT `00af9c8fd959f5d6562a57d6ac60e15005352822e24f152b80242036e1d4f3c6`, README `0d5a9f600bb971519729342eb31f3cc0613c0828e587c8cbfdb39283be02775d`. V006 migration hash unchanged: `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475`.

Tracked modified files outside the allowed set: none authored by this task. Pre-existing controller-owned `docs/gateflow/project-execution-control.md` edit was not touched.

## 3. Accepted findings and final statuses

### F-01 (medium) — CLOSED (fixed)

In `assertV006IssueFirstTrialSemantics` only (same method, no new `@Test`):

1. **Post-disable REPLAY / ALREADY_GRANTED** after expected policy `enabled=false`: same action/fingerprint returns exact `REPLAY` with original ids/`granted_at`; different action returns exact `ALREADY_GRANTED`; no new `trial_grant` row; no new Outbox event. Proves no new policy selection on the already-granted path (OQ-6 linearization preserved).
2. **Outbox timestamp tokens** `validFrom` / `expiresAt` / `grantedAt` asserted equal to persisted `entitlement_grant.valid_from` / `expires_at` and `trial_grant.granted_at` in the same UTC microsecond format the function writes (`YYYY-MM-DD"T"HH24:MI:SS.US"Z"`). Independent finite-payload Java hash oracle retained unchanged.
3. **Grant snapshot / snapshot_sha256** asserted equal to the selected policy snapshot / sha on the GRANTED path.
4. **Remaining constructible `TRIAL_DATA_INTEGRITY` classes** (each method-local, whole-transaction rollback, post-rollback zero-row assertions for every new subject):
   - missing referenced grant: savepoint-isolated `DROP CONSTRAINT fk_trial_grant_grant` → owner INSERT orphan `trial_grant` → `TRIAL_DATA_INTEGRITY` probe → savepoint rollback restores catalog (`fk_trial_grant_grant` present, orphan trial absent);
   - grant `subject_id` mismatch (second real subject on grant);
   - `source_type` mismatch via `PURCHASE`-shaped grant (real `customer_order` fixture chain satisfies `ck_entitlement_grant_source_shape` + `fk_entitlement_grant_order`);
   - `source_ref_id` ≠ `trial_policy_version_id` (random UUID);
   - `entitlement_type` = `AI_SECONDS`;
   - `valid_from` ≠ grant `granted_at`;
   - grant `granted_at` ≠ trial `granted_at`;
   - grant `expires_at` ≠ trial `expires_at`.
   Existing missing-balance, units-mismatch, and cross-subject-source probes preserved.
   `order_id`/`installment_no` non-null is only constructible under the PURCHASE shape, which already fails the earlier `source_type` predicate to the same stable `TRIAL_DATA_INTEGRITY`; no separate exclusive probe is possible under the accepted CHECK constraints.
   `balance.grant_id ≠ grant.id` is not constructible on the lookup path (balance is selected `WHERE grant_id = grant.id`).

### F-02 (low) — CLOSED (fixed)

In `assertV006OwnedAndCrossModuleGrants`, the `crossSelectRole` skip is removed. `ivrCrossSelect` / `catalogCrossSelect` maps are hoisted and reused. For every column of all seven V006 tables:
- `ivr_role` SELECT expected true only for declared `entitlement_balance` / `entitlement_grant` projections; all other table/column SELECTs false;
- `catalog_order_role` SELECT expected true only for declared `entitlement_issuance` / `entitlement_grant` projections; all other table/column SELECTs false;
- all other matrix roles retain owner/runtime SELECT expectations.
Existing dedicated sections, table-level privileges, and INSERT/UPDATE column assertions retained. Migration grants unchanged.

### F-03 (low) — CLOSED (fixed)

`backend/README.md` corrected only:
- migration-method claim no longer says V006 tables get runtime-role insert (`V006 表不在此方法写入`);
- ACL-method claim no longer says S1 proves owner immutable-guard behavior (`owner 不可变 guard 行为由 S2 方法证明`);
- PA6 S1 method bullets updated to describe the new F-01/F-02/F-04 evidence without production claims.

### F-04 (low) — CLOSED (fixed)

In `assertV006RuntimeDenialsAndLegalUpdateControl` under `entitlement_metering_role`: savepoint-bounded direct `INSERT INTO entitlement_metering.trial_grant (...)` asserted exact SQLSTATE `42501`. Existing `issue_first_trial` GRANTED path remains the only successful runtime insertion route. Matches accepted plan §9 line 661.

## 4. Validation record (author environment)

### 4.1 Bocused / iterating runs

| Run | Command / filter | Result |
| --- | --- | --- |
| Focused attempt 1 | `-Pcontainer-smoke verify -Dit.test=PostgresContainerIT#...` (3 methods) | 1 error: missing-grant `ADD CONSTRAINT` validated against the still-present orphan trial (`fk_trial_grant_grant` 23505-style INSERT message). Harness-only; no migration defect. |
| Focused attempt 2 (after savepoint isolation) | same 3-method filter | **3 run, 0 fail, 0 err, 0 skip**, BUILD SUCCESS |

### 4.2 Final full container smoke (exactly one full run after fixes complete)

| Check | Result |
| --- | --- |
| `mvn -B -ntp -Pcontainer-smoke verify` (Temurin 21.0.13 @ `/opt/jdk21`, Maven 3.9.9 @ `/opt/maven-3.9.9`) | **BUILD SUCCESS** |
| Failsafe XML `/tmp/pa6-s1-fix1/container-smoke-final.xml` | `tests=41 failures=0 errors=0 skipped=0` time 14.282s |
| Log `/tmp/pa6-s1-fix1/container-smoke-final.log` | `Tests run: 41, Failures: 0, Errors: 0, Skipped: 0` |
| PostgreSQL | 16.15 via `postgres:16` |
| source `@Test` / XML testcase / CI expected set | **41 == 41 == 41**, exact set equality (including `localImmutabilityGuardsRejectRelationAndRotationChanges`) |
| Method names / count | unchanged; no additions; no renames |

### 4.3 Secondary validations

| Check | Result |
| --- | --- |
| `mvn -B -ntp validate` | exit 0, Enforcer 5/5 pass |
| `mvn -B -ntp clean verify` (default) | exit 0 — Surefire 10 run, 0 fail/err/skip |
| `docker info` | exit 0 |
| Boot smoke | `Started OneCallApplication in 0.851 seconds` |
| Fat-jar firewall | 26 `BOOT-INF/lib` jars; none of spring-security/flyway/jooq/HikariCP/postgresql/testcontainers |
| V001-V006 SHA-256 | match frozen S1 hashes; V006 unchanged `830ed14c…` |
| frozen manifest `/tmp/sub-agents.pa6-s1-fix1.5nPALu/frozen.sha256` | before and after: all listed entries `OK` |
| `git diff --check` on the two tracked fix paths | exit 0 (CRLF working-tree warning only) |

### 4.4 V006 object counts

Unchanged from accepted S1: 7 tables / 84 columns / 50 named checks / 7 PK / 9 ordinary unique / 3 partial unique / 29 index objects / 15 immediate + 1 late FK / 19 defaults / 1 service + 7 trigger functions / 7 triggers. No migration edit.

## 5. Residual risks

- **Legacy V004/V005 concurrent TRUNCATE co-truncates all seven `entitlement_metering` tables.** S1 still has no committed V006 rows, so no live cross-method pollution. After S2 introduces committed `GRANTED` fixtures, table-wide cleanup may mask a S2 method-local cleanup failure. Recorded for S2 review only (controller-directed non-fix).
- **Publication-guard draft→published UPDATE** remains S2 (`guard_trial_policy_version` one-shot branch).
- **Fresh-grant same-source cross-subject collision** remains native `23505` (accepted).
- **`order_id`/`installment_no` integrity predicate** is not exclusively probed (only reachable behind the earlier PURCHASE `source_type` predicate under accepted CHECKs).
- Not run by this task: second full container smoke after the final green (forbidden by protocol), exact-head GitHub Actions, production provisioning (Q17/Q23).

## 6. Gateflow classifications

- Author: S1 code-review fix author only.
- This artifact does **not** claim an accepted checkpoint, controller approval, re-review verdict, production readiness, or S2 entry.
- Next controller actions (outside this task): independent S1 fix re-review / intake; later Gateflow gates.

## 7. Stop condition

S1 fix-1, bounded validation, and both Gateflow artifacts are complete. No Git mutation was performed. Stopped here without entering a later gate.
