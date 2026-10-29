# PA6 V006 S1 Implementation Completion Report

- Task label: `pa6-v006-s1-impl-retry-mimo26flash-20261029-095617`
- Runtime / provider / model: Linux WSL2; Claude Code harness; MiMo-V2.6-Flash (implementation author)
- Author role: S1 implementation author only (not plan author, controller, reviewer, or approver)
- Completion timestamp: `20261029-111603` (system clock via `date +%Y%m%d-%H%M%S`)
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Branch / base: detached HEAD at accepted plan commit `e5e00bfc775de180a25fd56cae98658f5a41852e` (`gateflow: accept plan for PA6-v006-entitlement-reservation-usage-foundation`)
- Run dir: `/tmp/sub-agents.pa6-s1-impl-retry.I1kvrC`
- Binding plan: `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan.md`
- Binding review residuals: `docs/reviews/plan-review-20261029-090806.md` R-01..R-05 (S1 destinations)

## 1. Scope and non-goals

S1-only: complete accepted V006 migration + exact catalog/ACL evidence + three exact S1 methods + 38→41 method evolution + CI/README synchronization + this durable artifact. No S2 methods, no application service behavior (reserve/settle/issue orchestration, API/Gateway/JPA, runtime DataSource/Flyway wiring). No stage, commit, push, PR mutation, nested agent, self-review, or later-gate entry. No V007+ objects. V001-V005 remain byte-for-byte frozen. `pom.xml` / application Java / design/control documents / accepted gate artifacts preserved (controller-owned `docs/gateflow/project-execution-control.md` pre-existing edit left untouched).

This task is the single same-provider retry after a prior one-shot stopped with a provider content filter and left partial untrusted edits. Partial V006 migration and `PostgresContainerIT` work was inspected against the accepted plan, corrected, and completed; valid partial work was retained.

## 2. Changed paths (exactly five allowed writes)

| Path | Action | SHA-256 |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql` | new (772 lines) | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` (unchanged by fix-1) |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | modified (38→41 methods; fix-1 delta inside existing methods) | post-fix-1: `f3dba46ee2c6db4ad5189cf8e55c0092671895f4607ee192663da05c629b2484` (original author delivery: `00af9c8fd959f5d6562a57d6ac60e15005352822e24f152b80242036e1d4f3c6`) |
| `.github/workflows/ci.yml` | modified (expected set/labels to V006 S1 / 41) | `39d71292433ba05e7e836bb09a35d2fcb130bea5a9b7d84ec9b5cc7b7a8fd4f5` (unchanged by fix-1) |
| `backend/README.md` | modified (S1 evidence to PA6 V006 / 41; fix-1 claim corrections) | post-fix-1: `a188b06886b714442f71fce6b5319940125f928e11bd62ab37e14217cb0dca60` (original author delivery: `0d5a9f600bb971519729342eb31f3cc0613c0828e587c8cbfdb39283be02775d`) |
| `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-s1-implementation.md` | new (this report; updated post-fix-1) | written at completion; updated `20261029-122203` |

Tracked modified files outside the allowed set: none authored by this task. Pre-existing controller-owned `docs/gateflow/project-execution-control.md` edit (accepted-plan gate row) was already present at session start and was not touched.

## 3. V006 contract implemented (exact frozen counts)

| Quantity | Value |
| --- | --- |
| Tables | 7 (`trial_policy_version`, `entitlement_grant`, `trial_grant`, `entitlement_balance`, `entitlement_issuance`, `usage_reservation`, `usage_fact`) |
| Columns | 11+13+10+8+14+16+12 = **84** |
| Named checks | 50 (7+8+5+5+10+9+6) |
| PKs | 7 |
| Ordinary UNIQUE constraints | 9 |
| Partial unique indexes | 3 |
| Unique keys | 12 |
| Secondary indexes | 10 |
| Total index objects | 29 (7 PK + 12 unique + 10 secondary) |
| Immediate RESTRICT FKs in V006 block | 15 (`entitlement_metering` only) |
| Late FK | 1 (`fk_ivr_decision_reservation` on `ivr.ivr_decision.reservation_id` → `usage_reservation(id)`); 16 FK decisions total |
| SECURITY DEFINER service functions | 1 (`issue_first_trial`) |
| SECURITY INVOKER trigger functions | 7 |
| Triggers | 7 |
| Defaulted columns | 19 |
| Id defaults / sequences | 0 / 0 |
| New roles / schemas | 0 (reuse V004 roles/schemas) |
| Function-wave block | remains 5 rows (4 V001 + 1 V006 `issue_first_trial`) |

### 3.1 Option A trigger binding (F-PA6-PLAN-01)

| Trigger | Function | Table |
| --- | --- | --- |
| `trg_trial_policy_version_guard` | `guard_trial_policy_version` | `trial_policy_version` |
| `trg_entitlement_grant_immutable` | `guard_entitlement_grant` | `entitlement_grant` |
| `trg_trial_grant_immutable` | `guard_trial_grant` | `trial_grant` |
| `trg_entitlement_balance_guard` | `guard_entitlement_balance` | `entitlement_balance` |
| `trg_entitlement_issuance_guard` | `guard_entitlement_issuance` | `entitlement_issuance` |
| `trg_usage_reservation_guard` | `guard_usage_reservation` | `usage_reservation` |
| `trg_usage_fact_immutable` | `reject_row_mutation` | `usage_fact` |

All seven trigger functions are `SECURITY INVOKER`, owned by `entitlement_metering_owner`, `SET search_path = entitlement_metering, pg_temp`, no transaction control.

### 3.2 `issue_first_trial` contract

Signature: `(p_subject_id uuid, p_campaign_key text, p_trial_policy_version_id uuid, p_action_domain text, p_action_event_id text, p_request_fingerprint text) RETURNS TABLE (trial_grant_id uuid, entitlement_grant_id uuid, entitlement_balance_id uuid, outcome text, first_granted_at timestamptz)`.

- Owner `entitlement_metering_owner`; `SECURITY DEFINER`; `SET search_path = entitlement_metering, platform, pg_temp`.
- Success outcomes: `GRANTED` / `REPLAY` / `ALREADY_GRANTED`.
- Stable errors (P0001): `TRIAL_ARGUMENT_INVALID`, `TRIAL_CAMPAIGN_INVALID`, `TRIAL_ACTION_CONFLICT`, `TRIAL_DATA_INTEGRITY`, `TRIAL_POLICY_NOT_FOUND`.
- Replay/already-granted checked before any policy selection; sole authoritative enabled/published/expected-version policy read inside the function.
- Nested `platform.outbox_append` for domain `ACTIVATION`, dedupe `trial_grant:<id>`, aggregate `TRIAL_GRANT/<id>`, event `FIRST_TRIAL_GRANTED`, fixed 10-field payload.
- Missing non-null subject remains native FK `23503` (no owner `subject_number` read). R-03 applied.

### 3.3 ACL summary

- Runtime `entitlement_metering_role`: schema USAGE without CREATE; SELECT/INSERT on all seven; exact column UPDATE allowlists; `trial_grant` INSERT-none (function only); `entitlement_grant`/`usage_fact` append-only; EXECUTE on six dedicated guards only; no EXECUTE on `reject_row_mutation`.
- `issue_first_trial` EXECUTE only to `activation_consent_role` and `entitlement_metering_role`.
- Cross-module column SELECT: `ivr_role` on `entitlement_balance` / `entitlement_grant` (routing snapshot); `catalog_order_role` on `entitlement_issuance` / `entitlement_grant` (R-04 projection).
- F-02 owner capability: `USAGE` on `platform` + `EXECUTE` on `platform.outbox_append` to NOLOGIN `entitlement_metering_owner` only; other S2 owners and PUBLIC remain false.
- `PUBLIC`: zero ACL on every V006 object.
- Advisory lock: docs/14 §9.0.1 rank 3 `subject_number.subject:<subject_id>` via `pg_advisory_xact_lock(hashtextextended(...))`. R-04 applied.

## 4. Residual decisions (R-01..R-05 as applied)

| Residual | Decision | Evidence |
| --- | --- | --- |
| R-01 unnamed cumulative helpers | Explicitly evolved `assertV005TablesColumnsChecksIndexesFks` (V005 FK universe excludes late FK; late FK asserted separately) and `assertV004OwnedAndCrossModuleGrants` / `assertV004SchemaUsageMatrix` / `assertV004GuardFunctionExecuteBoundaries` for V006 grants (`catalog_order_role` USAGE, `entitlement_metering_role`→subject USAGE, F-02 owner platform USAGE + outbox EXECUTE). | Full 41-method container run green |
| R-02 trigger names | Option A convention-derived guard trigger names bound in §3.1; exact `pg_trigger` / `tgfoid` / `proacl` inventory asserted. | `v006EntitlementReservationUsageInventoryAndGuardShapesAreExact` |
| R-03 missing subject SQLSTATE | Native FK `23503` retained; no owner subject grant/read. | Function body + tests |
| R-04 advisory key | `subject_number.subject:<subject_id>` | Function body line 204-205 |
| R-05 Outbox hash oracle | Independent Java flat-JSON token re-serializer (fixed UTF-16 key order + SHA-256 UTF-8 hex); explicitly not a generic RFC 8785 claim. Canonical keys: `campaignKey`, `entitlementBalanceId`, `entitlementGrantId`, `expiresAt`, `grantedAt`, `grantedUnits`, `subjectId`, `trialGrantId`, `trialPolicyVersionId`, `validFrom`. | `v006CanonicalOutboxSha256` / `assertV006IssueFirstTrialSemantics` |

R-06 (S2 cleanup-trigger restoration) and R-07 (deferred product values) were not entered by S1.

## 5. Test / CI changes

- Migration method renamed: `v001ThroughV005MigrateCleanPostgres16WithoutSkipping` → `v001ThroughV006MigrateCleanPostgres16WithoutSkipping` (staged target-005 then V006; `migrationsExecuted` V006=1).
- Added exactly three S1 methods after `concurrentV005TaskClaimAndActiveCallHasOneWinner`:
  - `v006EntitlementReservationUsageInventoryAndGuardShapesAreExact`
  - `v006OwnerPublicRuntimeAndCrossModuleAclsAreExact`
  - `v006IssueFirstTrialGrantReplayConflictAndPolicySemantics`
- No S2 methods.
- Total `@Test` methods: **41** (38 + 3).
- Cumulative helper evolution: global function/trigger inventories, staged V005 absence, V004/V005 schema-usage matrices, concurrent-fixture TRUNCATE legality after V006 FKs (`entitlement_metering` tables + `ivr.ivr_decision` co-truncated with `usage_reservation`).
- `issue_first_trial` test harness uses savepoint-isolated P0001 expectations; `callIssueFirstTrial` rethrows without `RESET ROLE` (avoids 25P02 masking).
- CI expected set: exactly **41** names; labels/error text "V006 S1"; Docker-first flow, boot smoke, fat-jar firewall, contract jobs unchanged in structure.

## 6. Validation results (author environment)

| Check | Exit / result |
| --- | --- |
| `mvn -B -ntp validate` (Temurin 21.0.13+9 @ `/opt/jdk21`, Maven 3.9.9 @ `/opt/maven-3.9.9`) | **0** — Enforcer pass |
| `mvn -B -ntp clean verify` (default) | **0** — Surefire **10** run, 0 fail/err/skip |
| `docker info` | **0** |
| `mvn -B -ntp -Pcontainer-smoke verify` | **0** — Failsafe **41** run, **0** failure, **0** error, **0** skip (PostgreSQL 16.15) |
| Source / CI / Failsafe method-set equality | **41 == 41 == 41**, all equal |
| Boot smoke (`Started OneCallApplication`, timeout 60s) | exit 0 on jar built by default verify |
| Fat-jar firewall | pass — no spring-security/flyway/jooq/HikariCP/postgresql/testcontainers in `BOOT-INF/lib` |
| `git diff --check` on five allowed paths | clean (CRLF working-tree warning only) |
| Diff-scope | only the five allowed paths authored; V001-V005/pom/application untouched |

### 6.1 V001-V006 SHA-256 (recomputed at completion)

| File | SHA-256 |
| --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| `V004__catalog_order_and_payment.sql` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `V005__ivr_task_session_call.sql` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `V006__trial_entitlement_issuance_reservation_usage.sql` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |

V001-V005 match the frozen manifest and plan §2.2. V006 is the new migration hash recorded for this S1 slice.

### 6.2 Intermediate container-smoke fix cycle (transparency)

The first post-splice 41-method container run failed with 5 failures / 2 errors. All root causes were test-harness (not migration) and were fixed before the green evidence run:

1. `V006_FK_DEFS` incorrectly contained the late `fk_ivr_decision_reservation` while the query filters `entitlement_metering` only → late FK moved to separate `lateFkRows` assertion; defs size 15.
2. V004/V006 schema-usage matrices expected `entitlement_metering_owner` platform USAGE false → flipped true (F-02).
3. Concurrent cleanups could not TRUNCATE parents referenced by V006 FKs → co-truncate all seven `entitlement_metering` tables; V004 path also co-truncates `ivr.ivr_decision` (late FK on `usage_reservation`).
4. `callIssueFirstTrial` `RESET ROLE` after P0001 masked error as 25P02 → rethrow without reset; savepoint handler rolls back then resets.
5. V006 owned/cross-module matrix looked up qualified-key maps with bare table names (NPE) and treated column SELECT as owner/runtime-only while V006 grants cross-module column SELECTs → qualified keys; matrix skips `ivr_role`/`catalog_order_role` column SELECT (asserted in dedicated sections).
6. V004 owner EXECUTE negative matrix failed F-02 outbox grant → skip only `entitlement_metering_owner`×`outbox_append` pair; positive F-02 assertion retained.
7. `has_function_privilege` rejected named-parameter identity for `issue_first_trial` → type-only identity `issue_first_trial(uuid,text,uuid,text,text,text)`.

The green evidence run is the subsequent full container-smoke (41/0/0/0). Intermediate failing logs are under `/tmp/pa6-s1-gen/container-smoke*.log`; the preserved green XML is `/tmp/pa6-s1-gen/container-smoke-final.xml`.

### 6.3 Note on `./mvnw`

`backend/mvnw` has CRLF line endings (`/bin/sh^M: bad interpreter`). Author used `/opt/maven-3.9.9/bin/mvn` with `JAVA_HOME=/opt/jdk21`. CI uses `./mvnw` on Ubuntu runners and is unaffected.

## 7. Post-fix-1 status (controller-accepted code-review findings)

Original §6 validation above is the S1 implementation author's pre-review evidence and is retained as-is. After dual code review, the controller accepted F-01/F-02/F-03 (Reviewer A) and F-04 (Reviewer B R-A). A separate same-provider fix task (`pa6-v006-s1-fix1-mimo26flash-20261029-113653`, report: `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-s1-fix-1.md`) applied test-evidence and README corrections only:

| Finding | Status | Where |
| --- | --- | --- |
| F-01 integrity / replay-ordering / payload-value probes | fixed | `assertV006IssueFirstTrialSemantics` |
| F-02 exact cross-module column-SELECT sets on all 7 tables | fixed | `assertV006OwnedAndCrossModuleGrants` |
| F-03 README evidence overstatement | fixed | `backend/README.md` |
| F-04 runtime `trial_grant` INSERT `42501` probe | fixed | `assertV006RuntimeDenialsAndLegalUpdateControl` |

Post-fix-1 validation (separate from original author §6): focused 3-method container run green after one savepoint-isolation harness correction; **exactly one** final full `-Pcontainer-smoke verify` → Failsafe **41/0/0/0** (`/tmp/pa6-s1-fix1/container-smoke-final.xml`), source/CI/XML 41==41==41; `mvn validate` exit 0; default `clean verify` Surefire 10/0/0/0; boot smoke OK; fat-jar firewall OK; V001-V006 hashes unchanged; frozen manifest OK; `git diff --check` clean on fix paths. V006 SQL, CI expected set, and method count/names were not changed by fix-1.

Gateflow: F-01..F-04 remain closed pending independent re-review / controller adjudication. This section does not claim re-review acceptance.

## 8. Post-fix-2 status (controller-accepted re-review finding N-01)

After fix-1, dual independent re-review produced Reviewer A `pass-with-risks` with one new finding N-01 (low, test evidence; `docs/reviews/code-review-20261029-122837.md`) and Reviewer B `pass` with no material finding (`docs/reviews/code-review-20261029-122838.md`). The controller accepted only N-01 (`docs/gateflow/project-execution-control.md` re-review S1 round 1 row): the pre-fix-2 `validFromSubject` fixture in `assertV006IssueFirstTrialSemantics` set grant `valid_from=base`, grant `granted_at=base+1s` and trial `granted_at=base`, so it simultaneously violated both V006 predicates `valid_from = grant.granted_at` and `grant.granted_at = trial.granted_at` — deleting only the first predicate would leave the probe green. F-01 was classified `部分修复` on that basis; F-02/F-03/F-04 remained `已修复`.

A separate same-provider fix task (`pa6-v006-s1-fix2-mimo26flash-20261029-123903`, report: `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-s1-fix-2.md`) applied the bounded fixture-only correction: the `validFromSubject` fixture's trial `granted_at` argument is now `integrityBase.plusSeconds(1)` (equal to grant `granted_at`), so the fixture violates only `valid_from = grant.granted_at` while grant/trial `granted_at` are equal and every other equality is retained. No new `@Test` method, no rename, still exactly 41; V006 product SQL, CI, and README unchanged by fix-2.

| Item | Status |
| --- | --- |
| N-01 (low) — `valid_from` predicate probe masked by coupled `granted_at` mismatch | fixed (author) — final status awaits independent re-review / controller adjudication |
| F-01 (medium) — was `部分修复` solely due to N-01 | N-01 closed author-side; F-01 final status not claimed by this task |

Post-fix-2 validation (separate from original §6 and post-fix-1 §7): focused single-method run of `v006IssueFirstTrialGrantReplayConflictAndPolicySemantics` green (1/0/0/0); **exactly one** final full `-Pcontainer-smoke verify` → Failsafe **41/0/0/0** on PostgreSQL 16.15 (`/tmp/pa6-s1-fix2/container-smoke-final.xml`, log `/tmp/pa6-s1-fix2/container-smoke-final.log`); source `@Test` / XML testcase / CI expected set **41 == 41 == 41** exact set equality; `mvn validate` exit 0; default `clean verify` Surefire 10/0/0/0; boot smoke `Started OneCallApplication in 1.245 seconds`; fat-jar firewall denylist empty; V001-V006 SHA-256 unchanged (V006 remains `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475`); frozen manifest re-verified all OK; `git diff --check` clean on the fix paths (CRLF working-tree warning only). Post-fix-2 `PostgresContainerIT.java` SHA-256: `f56badab52038fcb3fbc9f19218c536fb48ed3aec3269aa04d62a0170f71a1e3` (post-fix-1 was `f3dba46ee2c6db4ad5189cf8e55c0092671895f4607ee192663da05c629b2484`).

Gateflow: N-01 is author-fixed pending independent re-review. This section does not claim re-review acceptance, an accepted S1 checkpoint, or S2 entry.

## 9. Gateflow classifications

- Author: S1 implementation author; fix-1 delta authored by the separate fix task named in §7; fix-2 delta authored by the separate fix task named in §8.
- This artifact does **not** claim an accepted checkpoint, controller approval, re-review verdict, production readiness, or S2 entry.
- Next controller actions (outside this task): independent S1 fix re-review / intake; later Gateflow gates.
- Frozen prerequisites preserved: V001-V005 migrations, accepted plan commit `e5e00bfc775de180a25fd56cae98658f5a41852e`, plan/fix/review hashes in the frozen manifest.

## 10. Stop condition

S1 implementation, fix-1 and fix-2 updates, and artifacts are complete. No Git mutation was performed. Stopped here without entering a later gate.
