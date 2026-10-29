# PA6 V006 S2 Implementation Completion Report

- Task label: `pa6-v006-s2-impl-mimo26flash-20261029-125647`
- Runtime / provider / model: Linux WSL2; Claude Code harness; MiMo-V2.6-Flash (implementation author)
- Author role: S2 implementation author only (not plan author, controller, reviewer, or approver)
- Completion timestamp: `20261029-144315` (system clock via `date +%Y%m%d-%H%M%S`)
- Workspace: `/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Branch / base: detached HEAD at accepted S1 checkpoint `a7ccadabcba29d70d3678bd30eac44013e2a7b32` (`gateflow: accept PA6-v006-entitlement-reservation-usage-foundation S1`)
- Run dir: `/tmp/sub-agents.pa6-s2-implementation.yUvRjf`
- Binding plan: `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan.md` sections 6.3, 7.1–7.4, 8–11
- Binding controller: `docs/gateflow/project-execution-control.md` PA6 goal contract and accepted S1 checkpoint row
- Binding schema: `docs/14-production-data-model-and-flyway-schema-spec.md` V006 lifecycle/guard/FK/transaction sections
- Frozen read-only manifest: `/tmp/sub-agents.pa6-s2-implementation.yUvRjf/frozen.sha256`

## 1. Scope and non-goals

S2-only database-backstop evidence: prove the accepted, byte-frozen V006 DDL's lifecycle, guard, FK, nested-Outbox rollback, bounded concurrency and cumulative evidence with exactly four new executable PostgreSQL 16 methods. Preserve all 41 existing method names and add exactly four → exactly 45 `@Test` methods with exact source/CI/XML set equality.

No application reserve/settle/issue/publish/select service, API/controller/JPA/runtime DataSource/Flyway, V007+ object, new function/grant/schema/table, tolerance, TTL, allocation policy or production claim. V001–V006 migrations, POM/application code, docs/12, docs/14, plan, controller and S1/review artifacts remained read-only (frozen manifest verified all OK after work). If a frozen-contract failure had appeared to require V006 correction, the task would stop and report rather than edit the migration; that stop condition was not reached.

No stage, commit, push, PR mutation, merge, dispatch, self-review or code-review entry.

## 2. Changed paths (exactly four allowed writes)

| Path | Action | SHA-256 |
| --- | --- | --- |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | modified (four S2 methods + bounded helpers; still 45 `@Test`) | `38acd9e16419a3d7623589385dfcefaa995c1fde37835ba4ad594f1efa509adf` |
| `.github/workflows/ci.yml` | modified (expected set/count 41→45 and V006 S2 labels only) | `02ceafa60ae1bfcaa71a67367ccead968f417c3d556c67131d68d284bb93fc20` |
| `backend/README.md` | modified (45-method V006 S2 database-evidence boundary) | `c98063cf6cc98ff6193fd2ed6be3cae50c52a3be95d46973932ba699b2427cc4` |
| `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-s2-implementation.md` | new (this report) | written at completion |

Tracked modified files outside the allowed set: none authored by this task. Pre-existing controller-owned `docs/gateflow/project-execution-control.md` edit was already present at session start and was not touched.

## 3. Four-method evidence map

### 3.1 `v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops`

- Trial-policy publication/disable shapes; partial unique `uq_trial_policy_enabled`; guard admits the one publication UPDATE (NULL→NOT NULL `published_at` + `enabled=true` + revision+1 + publisher) and rejects later content/publisher/`published_at` changes with P0001 `TRIAL_POLICY_IMMUTABLE`. Live `SET published_at = published_at + interval '1 second'` probe (not `now()`, which is transaction-stable and a no-op the guard allows).
- Entitlement grant, trial grant and usage fact append-only: owner exact P0001 (`ENTITLEMENT_GRANT_IMMUTABLE` / `TRIAL_GRANT_IMMUTABLE` / `USAGE_FACT_APPEND_ONLY`) and runtime `42501` denials, savepoint-bounded.
- Balance invariants exact `23514` `ck_entitlement_balance_units` / `ck_entitlement_balance_status_shape`.
- Issuance state shape and terminal `ISSUED`.
- Probes all applicable 7+8+5+5+10 named checks with exact `23514` + constraint name; valid positive controls; whole-transaction rollback with zero-row post-rollback assertions.
- Call parents use a distinct random subject (`callSubjectId`) so `v006S2CommitCallParents` does not re-insert an already-inserted `pk_business_subject`.

### 3.2 `v006ReservationUsageForeignKeyAndLateFkBackstops`

- Reservation state shapes including exact `ck_usage_reservation_state_shape` and `ck_usage_reservation_settled`, with no invented tolerance.
- `ck_usage_reservation_status` cannot be isolated from `state_shape` (every `state_shape` OR branch requires a valid status); the probe asserts SQLSTATE `23514` only and the constraint name via catalog inventory; the status name is still registered so the 9+6 names set equality holds.
- `ck_usage_reservation_time_order` isolated with a state_shape-valid `RELEASED` row (`authority_source='IVR'`, `settled_units=0`, `terminal_at < created_at`).
- Legal `RECONCILING`/`SETTLED`/`RELEASED` inserts use real call parents (distinct random subjects) and always supply `authorization_expires_at` (`NOT NULL`, no default).
- Usage-fact unique reservation/type and source/event identities; source-unique probes use a fresh reservation so `uq_usage_fact_reservation_type` cannot fire first.
- Missing-parent exact `23503` + constraint name for all 16 FKs including late `fk_ivr_decision_reservation` (isolation: distinct parents/versions per probe; trial-policy probe uses `version_no=99` to avoid `uq_trial_policy_campaign_version`).
- Representative parent-delete RESTRICT `23503` on `entitlement_balance` (grant DELETE yields P0001 `ENTITLEMENT_GRANT_IMMUTABLE` because append-only BEFORE DELETE fires before FK).
- Late FK exists only after V006, is immediate/not deferrable; `source_ref_id` has no FK.
- Probes all 9 reservation + 6 usage checks; whole-transaction rollback with zero-row post-rollback assertions for local reservation/fact IDs.

### 3.3 `v006NestedOutboxAtomicityAndReplayRollbackEvidence`

- Part A: one method-local transaction as `platform_reliability_owner` transactionally `REVOKE EXECUTE` on exact `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)` from `entitlement_metering_owner`; reset to migration identity; savepoint; invoke `issue_first_trial` for fresh local ids as caller role; native `42501` with `permission denied for function outbox_append`, no translation/swallowing. Roll back to savepoint while retaining the earlier transactional revoke; assert zero local trial/grant/balance/Outbox rows and owner EXECUTE=false. Roll back outer transaction; fresh connection sees EXECUTE=true and a normal call succeeds.
- Part B: whole-transaction failure after trial writes rolls back everything.
- Part C: commit one fresh local `GRANTED` transaction; observe from a new connection exactly one trial/grant/balance/Outbox with frozen ids/times/hash; prove `REPLAY`/`ALREADY_GRANTED` add nothing; clean only method-local rows via the approved FK-safe cleanup (`cleanupV006S2LocalRows`).

### 3.4 `concurrentV006FirstTrialAndReservationIdentityHasOneWinner`

- Two real connections, distinct `application_name`, `pg_stat_activity` lock-wait detection, bounded `statement_timeout` and future timeout; no sleep-only assertions.
- Race A: two `issue_first_trial` calls for one subject against the single enabled+published policy (`campaign_key` constrained to `FIRST-TRIAL`; `uq_trial_policy_enabled` allows one enabled+published row per campaign — the race reuses the setup policy rather than inserting a colliding version). One `GRANTED`; the other exact `REPLAY`/`ALREADY_GRANTED`/`TRIAL_ACTION_CONFLICT` per identity; never two trials.
- Race B: two `usage_reservation` inserts for one `call_id`; one winner; loser exact `23505` `uq_usage_reservation_call`.
- Race C: two `trial_grant` inserts for one `subject_id`; one winner; loser exact `23505` `uq_trial_grant_subject`.
- Race D1/D2: `usage_fact` for one reservation/type and one source domain/id; one winner each with exact named unique failure. D2 uses different reservations sharing only `sharedSourceEventId` so `uq_usage_fact_reservation_type` cannot fire before `uq_usage_fact_source`.
- Category-2 savepoint: failed reservation insert rolls back to savepoint while preserving committed trial/grant/balance.
- Category-3 whole-transaction failure after trial writes leaves no partial rows.
- Race helper takes caller-allocated winner/loser UUIDs so the committed winner row is the registered cleanup ID (avoids leftover reservation → balance FK failures).

## 4. Cleanup / trigger restoration evidence

- Rollback-only probes remain uncommitted. Committed observation and races allocate fresh method-local IDs/keys and always enter `finally` → `cleanupV006S2LocalRows(rows)`.
- Cleanup reconnects as migration identity in one transaction and deletes only recorded method-local IDs/keys in the accepted dependency order: external method-local `ivr_decision` rows first, method-local ACTIVATION Outbox by exact dedupe key, then `usage_fact`, `usage_reservation`, `entitlement_issuance`, `entitlement_balance`, `trial_grant`, `entitlement_grant`, `trial_policy_version`, followed only by actually-created V005/V004/V003/V002 parents in reverse dependency order.
- Transactionally disables only the four named triggers when required: `ivr.trg_ivr_decision_immutable`, `entitlement_metering.trg_usage_fact_immutable`, `entitlement_metering.trg_trial_grant_immutable`, `entitlement_metering.trg_entitlement_grant_immutable`.
- Never uses `session_replication_role`; never disables FK/internal/global triggers; never uses `TRUNCATE`, `CASCADE`, table-wide or unscoped DELETE. Does not weaken the existing ACL method's runtime DELETE/TRUNCATE `42501` evidence.
- Re-enables all four named triggers and asserts `tgenabled='O'` before commit. Any cleanup error rolls back delete and trigger-state changes and fails the test.
- Focused and final runs include post-cleanup zero-row assertions for every touched local ID/key (observed green in all four focused runs and the final full run).

## 5. Validation record (author environment)

Toolchain: Temurin 21.0.13+9 at `/opt/jdk21`, Maven 3.9.9 at `/opt/maven-3.9.9`. Evidence root: `/tmp/pa6-s2-gen/`. `backend/mvnw` has CRLF line endings; used `/opt/maven-3.9.9/bin/mvn` with `JAVA_HOME=/opt/jdk21`.

### 5.1 Focused iterating runs

| Run | Method | Result |
| --- | --- | --- |
| Focused | `v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops` | 1/0/0/0 green |
| Focused | `v006ReservationUsageForeignKeyAndLateFkBackstops` | 1/0/0/0 green after probe-isolation fixes (status SQLSTATE-only + catalog name; time_order RELEASED isolation) |
| Focused | `v006NestedOutboxAtomicityAndReplayRollbackEvidence` | 1/0/0/0 green |
| Focused | `concurrentV006FirstTrialAndReservationIdentityHasOneWinner` | 1/0/0/0 green after policy-reuse, race-ID registration, D2 parent subject/account/hmac isolation and category-3 policy reuse |

### 5.2 Final full container smoke (exactly one full run after all code was complete)

| Check | Result |
| --- | --- |
| `mvn -B -ntp -Pcontainer-smoke verify` | **BUILD SUCCESS** |
| Failsafe XML `/tmp/pa6-s2-gen/container-smoke-final.xml` | `tests=45 failures=0 errors=0 skipped=0`, 45 `<testcase>` nodes |
| Log `/tmp/pa6-s2-gen/container-smoke-final.log` | `Tests run: 45, Failures: 0, Errors: 0, Skipped: 0`; PostgreSQL **16.15** via `postgres:16` |
| source `@Test` / XML testcase / CI expected set | **45 == 45 == 45**, exact set equality (Python triple comparison) |
| Method names / count | four S2 names added; all 41 existing names preserved |

### 5.3 Secondary validations

| Check | Result |
| --- | --- |
| `mvn -B -ntp validate` | exit 0, Enforcer pass (`/tmp/pa6-s2-gen/validate.log`) |
| `mvn -B -ntp clean verify` (default) | exit 0 — Surefire **10** run, 0 fail/err/skip (`/tmp/pa6-s2-gen/default-verify.log`) |
| `docker info` | exit 0 |
| Boot smoke | `Started OneCallApplication in 1.239 seconds` (`/tmp/pa6-s2-gen/boot.log`) |
| Fat-jar firewall | `jar tf` via `/opt/jdk21/bin/jar`: 27 `BOOT-INF/lib` entries; denylist grep (`spring-security\|flyway\|jooq\|HikariCP\|postgresql\|testcontainers`) zero matches (`/tmp/pa6-s2-gen/jar-index.txt`) |
| V001–V006 SHA-256 | all match frozen S1 hashes; V006 unchanged `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |
| frozen manifest `/tmp/sub-agents.pa6-s2-implementation.yUvRjf/frozen.sha256` | all 15 listed entries `OK` |
| `git diff --check` on the three modified allowed paths | exit 0 (CRLF working-tree warning only; no whitespace errors) |

### 5.4 V001–V006 SHA-256 (recomputed at completion)

| File | SHA-256 |
| --- | --- |
| `V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6` |
| `V002__identity_organization_and_employee.sql` | `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505` |
| `V003__subject_number_and_claim.sql` | `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7` |
| `V004__catalog_order_and_payment.sql` | `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc` |
| `V005__ivr_task_session_call.sql` | `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310` |
| `V006__trial_entitlement_issuance_reservation_usage.sql` | `830ed14c0acab61bb2ce74fd2f3a314239fc133ef59b76d53b08e1f95f36c475` |

V006 remains byte-identical to the accepted S1 migration. V001–V005 match the frozen manifest.

## 6. Intermediate fix cycle (transparency)

Focused method 2 and method 4 each required harness-only corrections (no migration change). All were constraint-ordering, isolation or cleanup-registration issues:

1. `ck_usage_reservation_status` co-fails with `ck_usage_reservation_state_shape` (status='BAD' violates every `state_shape` OR branch) → SQLSTATE-only probe + catalog assertion + manual name registration.
2. `ck_usage_reservation_time_order` with default `HELD` status also violates `state_shape` (HELD requires `terminal_at IS NULL`) → isolate with a valid `RELEASED` row.
3. Race A second published policy collided on `uq_trial_policy_campaign_version` (campaign constrained to `FIRST-TRIAL`, version 1 already committed) → reuse the single enabled+published policy.
4. `raceV006UniqueInsert` generated its own winner/loser UUIDs while callers registered different IDs, so the committed winner reservation was not cleaned (balance delete hit `fk_usage_reservation_balance`) → helper now takes caller-allocated winner/loser IDs.
5. D2 source-unique parents reused `raceCallSubjectId` (`pk_business_subject` duplicate) and `raceHmac` (`uq_binding_active_number` duplicate) → distinct random subjects/accounts/bindings and fresh HMAC per parent chain; parent IDs registered for cleanup.
6. Category-3 inserted a second `FIRST-TRIAL` version-1 policy inside a to-be-rolled-back transaction, which still fails the unique immediately against committed data → reuse the setup `policyId`.

No intermediate failing logs were preserved as formal Gateflow evidence; the green evidence runs are the four focused runs and the single final full container-smoke recorded above.

## 7. Residual risks

- **`ck_usage_reservation_status` discriminating power** is catalog-backed (constraint exists) plus SQLSTATE `23514`, not a live named-constraint raise, because PostgreSQL evaluates `state_shape` first for any invalid status. A live `state_shape` probe is retained separately with its exact name.
- Legacy V004/V005 concurrent TRUNCATE co-truncation of all seven `entitlement_metering` tables remains a controller-directed S2 review item; S2 cleanup deliberately avoids TRUNCATE and uses scoped deletes with four named trigger disables only.
- Publication-guard draft→published UPDATE, fresh-grant same-source native `23505`, `order_id`/`installment_no` structural coupling, and `balance.grant_id` lookup-path self-proof remain as classified in S1 fix-1 §5 (unchanged; outside S2 scope).
- Concurrency races are bounded and deterministic via `pg_stat_activity` lock-wait detection; they are not a production load or chaos test.
- Not run by this task: a second full container smoke after the final green (forbidden by protocol); exact-head GitHub Actions; production provisioning (Q17/Q23).

## 8. Docs decision

Only `backend/README.md` was synchronized to the 45-method V006 S2 database-evidence boundary (title, overview, method count, four S2 method bullets, CI count, evidence-boundary paragraph). `docs/12`, `docs/14`, plan, controller and S1/review artifacts were not modified (read-only / frozen).

## 9. Gateflow classifications

- Author: S2 implementation author only.
- This artifact does **not** claim an accepted checkpoint, controller approval, re-review verdict, production readiness, or S3 entry.
- S2 is database-backstop evidence only. It does **not** establish application or production readiness, carrier/IVR media behavior, cloud/provider integration, load/restore/penetration/manual acceptance, or an A5 evidence/authority/acceptance record.
- Next controller actions (outside this task): independent S2 code review / intake; later Gateflow gates.

## 10. Stop condition

S2 implementation, validation and this artifact are complete. No Git mutation was performed. Stopped here without entering a later gate.
