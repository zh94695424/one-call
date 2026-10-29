# PA6 V006 Trial, Entitlement, Issuance, Reservation and Usage Foundation - Implementation Plan

- Original plan task label: `pa6-v006-plan-mimo26pro-20261029-062904`
- Plan-fix task label: `pa6-v006-planfix-gpt56-20261029-083342`
- Work unit: `PA6-v006-entitlement-reservation-usage-foundation`
- Gate: `fix` after failed adversarial plan review; this revision does not enter re-review or implementation
- Author: original plan by mimo26pro; bounded plan fix by GPT-5.6-equivalent Codex runtime; no implementation, commit, stage, push, PR, merge, review or agent dispatch
- Branch: `codex/pa6-v006-entitlement-reservation-usage-foundation`
- Base/head: `aa2b0925e6b8ffdac4587f2e44c4c0dde393d649`
- Windows worktree: `C:\Users\Administrator\.codex\worktrees\pa4-v004-catalog-order-payment\codex-call`
- Preserved Git environment: `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call4`, `GIT_WORK_TREE=/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Binding inputs: the user-confirmed PA6 goal contract, slice principle and gate record in `docs/gateflow/project-execution-control.md`; `docs/12-technology-stack-and-system-architecture.md` sections 5, 6, 14 and 15 Phase B; `docs/14-production-data-model-and-flyway-schema-spec.md` sections 2.9, 4.29-4.35, 5, 9.0.1, 9.11-9.16, 9.21-9.26, 10.9, 10.11-10.20, 11.1 and 11.4; accepted V001-V005 migrations and the current 38-method `PostgresContainerIT`; `.github/workflows/ci.yml`; `backend/README.md`.

This plan implements exactly `V006__trial_entitlement_issuance_reservation_usage.sql` and database-visible evidence for the seven frozen `entitlement_metering` tables. It does not implement reservation/settlement/activation/consent/issuance application services, IVR orchestration, API, JPA, runtime wiring or any V007+ object. The controller contract is the binding scope; this document adds no goal, acceptance criterion, hardening wave, application service or future-slice work.

Authorized F-PA6-PLAN-01 adjudication (Option A) is binding for this revision: docs/14 §10.16 and §11.4 control the trigger-function bindings; V006 has exactly seven trigger functions. `trg_entitlement_grant_immutable` executes `entitlement_metering.guard_entitlement_grant()`, `trg_trial_grant_immutable` executes `entitlement_metering.guard_trial_grant()`, and `trg_usage_fact_immutable` continues to execute the shared `entitlement_metering.reject_row_mutation()`. The two conflicting table-control sentences in docs/14 §4.30 and §4.31 are corrected by this fix; §4.35 is unchanged. Resulting function inventory is exactly one service function plus seven trigger functions.

## 1. Goal, success signals, scope and non-goals

### 1.1 Goal

Create one forward-only Flyway migration, `V006__trial_entitlement_issuance_reservation_usage.sql`, so later activation/consent, IVR reservation/settlement, evidence and sharing waves have stable trial-policy, grant, balance, issuance, reservation and usage-fact parents. V005 already created the `ivr` tables and the `ivr_decision.reservation_id` nullable column without its FK. V004 already created the `entitlement_metering` schema, `entitlement_metering_owner`/`entitlement_metering_role` and the S2 schema/role set. V006 must not recreate or alter those roles or schemas incompatibly.

V006 creates exactly seven `entitlement_metering` tables: `trial_policy_version`, `entitlement_grant`, `trial_grant`, `entitlement_balance`, `entitlement_issuance`, `usage_reservation`, `usage_fact`. It creates the sole V006 SQL service function `entitlement_metering.issue_first_trial`. It adds the late ordinary immediate FK `fk_ivr_decision_reservation` from `ivr.ivr_decision.reservation_id` to `entitlement_metering.usage_reservation.id`. No V007+ object is created.

### 1.2 Frozen success signals

- Flyway applies V001 through V006 to a clean PostgreSQL 16 database. V001-V005 remain byte-identical. V005 SHA-256 remains `2F4CDEF48FD5B1CEC9B5F4479E99DF3359073C94BF2C45CBC5B2CF8E87BA9310`.
- Exactly seven `entitlement_metering` tables; only those seven tables exist in `entitlement_metering` after V006; `activation_consent` contains zero tables; `evidence`/`sharing` remain absent.
- Exact V006 inventory: 84 columns (11/13/10/8/14/16/12); 50 named checks (7/8/5/5/10/9/6); seven PKs; 12 unique keys (9 ordinary unique constraints + 3 partial unique indexes); 10 secondary indexes; 29 total index objects (7 PK + 12 unique + 10 secondary); 15 ordinary immediate RESTRICT FK constraints created in the V006 FK block plus the late `fk_ivr_decision_reservation` (16 FK decisions); zero new `NO_FK` catalog decisions (`entitlement_grant.source_ref_id` is a documented polymorphic non-FK column, not a catalog `NO_FK` row); one `SECURITY DEFINER` service function (the frozen function-wave block remains five rows: 4 V001 + 1 V006); seven `SECURITY INVOKER` trigger functions; seven triggers; 19 defaulted columns; zero `id` defaults; zero sequences.
- Exact V006 owner/runtime/cross-module/PUBLIC ACL. `entitlement_metering_role` receives `SELECT`/`INSERT` and only the exact column `UPDATE` sets on its seven tables; `trial_grant` is INSERT-none (function only) and `entitlement_grant`/`usage_fact` are append-only; `issue_first_trial` is `SECURITY DEFINER`, owned by `entitlement_metering_owner`, `SET search_path = entitlement_metering, platform, pg_temp`, `EXECUTE` only to `activation_consent_role` and `entitlement_metering_role`; the NOLOGIN owner holds internal `USAGE` on `platform` and `EXECUTE` on `platform.outbox_append` (F-02); `PUBLIC` has zero privileges; every undeclared grant is denied.
- `issue_first_trial` returns exactly `GRANTED` / `REPLAY` / `ALREADY_GRANTED` for the documented success outcomes and raises exactly `TRIAL_ARGUMENT_INVALID` / `TRIAL_CAMPAIGN_INVALID` / `TRIAL_ACTION_CONFLICT` / `TRIAL_DATA_INTEGRITY` / `TRIAL_POLICY_NOT_FOUND` for the documented failure outcomes. Replay/already-granted is checked before any policy selection. The sole authoritative enabled/published/expected-version policy read is inside the function (OQ-6). Nested `platform.outbox_append` failure rolls back trial/grant/balance/Outbox with the caller transaction.
- The late `fk_ivr_decision_reservation` is present, ordinary immediate, `ON UPDATE RESTRICT ON DELETE RESTRICT`, not deferrable, and references `usage_reservation(id)`.
- Default verification, the PostgreSQL 16 container profile and boot/fat-jar checks pass with no skip, failure or error; source/CI/Failsafe method sets are exact.
- Executable PostgreSQL evidence proves: catalog/ACL/guard shapes; `issue_first_trial` grant/replay/conflict/policy semantics; append-only and mutable-head guards; late-FK timing and FK/check/index backstops; nested-Outbox rollback; bounded representative concurrency; V001-V005 immutability.

### 1.3 Scope boundary

Allowed changes: the new V006 migration; focused cumulative additions to `PostgresContainerIT`; the minimum CI expected-set update; `backend/README.md` evidence; the required PA6 Gateflow artifacts/controller records (controller-owned, not written by the implementation author). Database tests may use bounded transient SQL and fixtures to prove documented backstops, but must label all application-service orchestration (reserve/settle/issue/publish/select) as deferred.

### 1.4 Non-goals

- No V007-V009 migration or object: no consent record, service activation/transition, media/recording/evidence/artifact/event or sharing table/function.
- No SQL service function beyond `entitlement_metering.issue_first_trial`. No `reserve_call_usage`, `settle_call_usage`, `publish_trial_policy`, `disable_trial_policy`, `select_call_action`, `record_activation_next_action` or any other SQL service function.
- No application service, API, Gateway, controller, JPA aggregate/repository, outbound bridge, recording/media/upload, settlement/reconciliation, payment, frontend, cloud/provisioning or runtime DataSource/Flyway wiring.
- Do not guess Q4/Q5/Q8/Q10/Q15/Q17/Q18/Q20/Q21/Q23/Q25/Q26/Q28 or create a production allocation/tolerance/TTL/telephony/protocol value. F-10 has no approved tolerance; the normative settlement boundary is exactly `ck_usage_reservation_settled` (`settled_units <= authorized_units`). F-04 allocation policy remains absent/unapproved and is not invented.
- Do not add columns, tables, checks, indexes, triggers, grants, roles or schemas beyond the frozen dictionaries and §11.4 V006 order.
- Do not edit frozen design documents. If a contradiction is found, record exact file/section/line evidence and stop rather than silently resolving it.

### 1.5 Goal-alignment mapping

| Confirmed goal area | V006 database evidence | Deferred behavior (not implemented) |
| --- | --- | --- |
| Seven frozen tables, 84 columns | `trial_policy_version` (11) / `entitlement_grant` (13) / `trial_grant` (10) / `entitlement_balance` (8) / `entitlement_issuance` (14) / `usage_reservation` (16) / `usage_fact` (12); exact keys/checks/indexes/FKs/defaults | Application CRUD, publication, issuance, reservation and settlement services |
| `issue_first_trial` | `SECURITY DEFINER` function, exact signature/outcomes/errors/lock/policy read/nested Outbox, owner F-02 grants, EXECUTE to two roles | `entitlement-metering.issueFirstTrial` application wrapper, `Propagation.MANDATORY` transaction rules |
| Late decision-reservation FK | `fk_ivr_decision_reservation` ordinary immediate `ALTER_FK` after `usage_reservation` exists | `ivr.select_call_action` writing committed `reservation_id` values |
| Balance invariants and F-10 shape | `ck_entitlement_balance_units`, `ck_entitlement_balance_status_shape`, `ck_usage_reservation_settled`, `ck_usage_reservation_state_shape` | `reserve_call_usage` / `settle_call_usage`, F-04 `SOLE_ELIGIBLE` selector, reconciliation, quarantine |
| Append-only / mutable-head semantics | 3 append-only + 4 mutable-head tables; 7 guard/append-only triggers; exact column UPDATE grants | Service-owned lifecycle orchestration |
| Least privilege | Exact owned-table, cross-module, function EXECUTE, PUBLIC-zero ACL | Production role provisioning (Q17/Q23) |
| Later waves | Stable parents for V007 consent/activation and V008 evidence/reservation references | V007/V008/V009 migrations and services |

## 2. First-principles judgment and current-code evidence

### 2.1 First-principles judgment

V006 is the smallest useful next persistence wave because activation/consent (V007), IVR reservation/settlement and evidence (V008) all require durable trial-policy, grant, balance, reservation and usage-fact identities, and `ivr.ivr_decision.reservation_id` cannot receive its FK until `usage_reservation` exists. Creating only database-owned identities, shapes and fail-closed backstops preserves module ownership and avoids coupling migration work to unresolved allocation, tolerance, TTL, telephony and protocol inputs. Giving `entitlement_metering_role` only the frozen grants and creating exactly one SQL function (`issue_first_trial`) avoids inventing the runtime provisioning model and avoids premature V007 table privileges or application-service SQL.

### 2.2 Direct current-code evidence

- `backend/src/main/resources/db/migration/` contains exactly V001, V002, V003, V004 and V005. V004 creates the eight S2 roles, four S2 schemas, PUBLIC revocation, platform/audit grants including `entitlement_metering_role`/`activation_consent_role`/`ivr_role`/`catalog_order_role`, and `GRANT USAGE` on `entitlement_metering` to `entitlement_metering_role`/`activation_consent_role`/`ivr_role`. V006 must reuse these and must not create roles/schemas.
- V003/V004/V005 establish the repository pattern: owner-issued table/function/trigger/grant DDL under `SET LOCAL ROLE <owner>`, `RESET ROLE`, then ordinary immediate RESTRICT FKs added by the migration identity; `SECURITY INVOKER` guard functions with `SET search_path = <schema>, pg_temp`; exact catalog/ACL introspection. V006 reuses this pattern, with `issue_first_trial` as the first V006 `SECURITY DEFINER` service function.
- `PostgresContainerIT` has exactly 38 `@Test` methods. The migration method is `v001ThroughV005MigrateCleanPostgres16WithoutSkipping` and asserts V006 absence. `assertNoV006PlusObjects` asserts the seven V006 tables, `issue_first_trial` and `fk_ivr_decision_reservation` are absent and that `entitlement_metering`/`activation_consent` have zero tables. Several ACL assertions are explicitly "deferred to V006" (`entitlement_metering_role`→`subject_number` USAGE; `catalog_order_role`→`entitlement_metering` USAGE; `entitlement_metering_owner` platform capability).
- `.github/workflows/ci.yml` asserts the exact 38-name Failsafe set, `tests == len(expected)`, `skipped=0`, `failures=0`, `errors=0`, `cases == expected`, and the label "V005 S2 test count (38)".
- `backend/README.md` states 38 methods and "no V006+" / "no `issue_first_trial`" as the PA5 boundary.
- Frozen working-tree SHA-256 (LF-normalized files as present): V001 `96e72bdacb32610dd03975e3e4fd6b828579b574a375e4241482f19c96b4e1c6`; V002 `4cf2b66d5ad7870272102c90868a0d9ecbcf9e620ec1bd7ea26dec9541f66505`; V003 `4148f4acdeb9e56b57fe2d5d0a4fde616087f87bf16a836569edddbb0c8ae1d7`; V004 `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc`; V005 `2f4cdef48fd5b1cec9b5f4479e99df3359073c94bf2c45cbc5b2cf8e87ba9310`. V005 matches the PA6 frozen contract hash. V006 must preserve all five.
- The only pre-existing worktree modification is the controller-owned `docs/gateflow/project-execution-control.md`. Historical `.pa5-*` runner files are known and excluded. The implementation author must not touch any of them.

## 3. Exact affected paths and allowed changes

| Path | Slice | Change | Explicit boundary |
| --- | --- | --- | --- |
| `backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql` | S1 (authored once) | New complete V006 migration | Only seven `entitlement_metering` tables, 84 columns, 50 checks, 12 unique keys, 29 indexes, 16 FK decisions (15 + late), seven trigger functions, seven triggers, one `issue_first_trial` and exact grants; no schema/role creation, no other service function, no V007+ |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | S1/S2 | S1: rename/evolve migration and absence assertions, update cumulative function/trigger/schema/ACL lists, add three inventory/ACL/function methods and helpers/constants. S2: add four behavior/concurrency methods and bounded helpers. | Same class; no new test class; preserve all 38 prior methods except the explicitly renamed migration method and the enumerated cumulative-assertion evolutions |
| `.github/workflows/ci.yml` | S1/S2 | Update only the Failsafe expected method set/count (38 -> 41 -> 45) and V006 labels/error text | Docker-first, boot smoke, fat-jar firewall, contract jobs, permissions and filters unchanged |
| `backend/README.md` | S1/S2 | Describe V006 inventory/behavior and the 41/45 method profiles | Keep toolchain pins, module table, firewall description and evidence boundary; no production claim |
| `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-s1-implementation.md` | S1 | New evidence/completion report (implementation author) | Only S1 evidence |
| `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-s2-implementation.md` | S2 | New evidence/completion report (implementation author) | Only S2 evidence |
| `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan.md` | fix | Revised plan artifact | Plan-only correction; no implementation |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | fix | Correct only the §4.30 and §4.31 append-only control sentences to name their dedicated guards | No §4.35 or unrelated design edit |
| `docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan-fix-1.md` | fix | Durable finding/validation artifact | No gate transition |

Explicitly untouched by this fix except for the two authorized docs/14 sentences above: V001/V002/V003/V004/V005 SQL; `backend/pom.xml`; application Java (including package/module markers); `docs/12`; every other docs/14 sentence; `docs/gateflow/project-execution-control.md`; review artifacts; PA1-PA5 artifacts; API/UI/protocol/security/deployment/acceptance assets; historical `.pa5-*` files; every migration; Git metadata.

## 4. Exact V006 schema and inventory

### 4.1 Migration artifact and DDL creation order

- Filename exactly `V006__trial_entitlement_issuance_reservation_usage.sql`; Flyway version `006`; description `trial entitlement issuance reservation usage`.
- Header comment follows the V002-V005 style: wave tag (`PA6 V006`), frozen object counts, "no V007+ object", "no service function beyond `issue_first_trial`", and the deliberate `ivr.ivr_decision.reservation_id` late-FK note.
- Runs in Flyway's PostgreSQL transaction; no top-level `COMMIT`, `ROLLBACK`, `BEGIN`, autonomous transaction or isolation change. `SET LOCAL ROLE` is used for object ownership and owner-issued grants; PL/pgSQL bodies use ordinary block `BEGIN`/`END`.
- Style follows V003-V005: `CREATE TABLE` with inline `CONSTRAINT pk_/uq_/ck_`; `CREATE [UNIQUE] INDEX` immediately after each table (interleaved, not a separate index section); trigger functions then `CREATE TRIGGER`; `REVOKE ALL ... FROM PUBLIC`; column-level `GRANT`; `RESET ROLE`; cross-schema owner grants; late FK block last under the migration identity.
- `issue_first_trial` body uses `LANGUAGE plpgsql SECURITY DEFINER SET search_path = entitlement_metering, platform, pg_temp` and `#variable_conflict use_column`, matching the V001 service-function convention.
- Do not `CREATE ROLE` or `CREATE SCHEMA`; do not `ALTER SCHEMA`; do not re-grant V004-owned platform/audit or S2 schema `USAGE` or the V004 `entitlement_metering_role`→`catalog_order` column SELECTs except the exact new grants listed below.

Exact order (§11.4 V006, mechanically mapped):

1. `SET LOCAL ROLE entitlement_metering_owner`.
2. `CREATE TABLE entitlement_metering.trial_policy_version` with columns, PK, two unique keys (one partial) and seven named checks; FK to `operations_iam.employee_account` is in the later FK block. Create `ix_trial_policy_enabled`.
3. `CREATE TABLE entitlement_metering.entitlement_grant` with columns, PK, one partial unique and eight named checks. Create `ix_entitlement_grant_subject_type`, `ix_entitlement_grant_order`.
4. `CREATE TABLE entitlement_metering.trial_grant` with columns, PK, three unique keys and five named checks. Create `ix_trial_grant_campaign_time`.
5. `CREATE TABLE entitlement_metering.entitlement_balance` with columns, PK, one unique key and five named checks. Create `ix_entitlement_balance_status`.
6. `CREATE FUNCTION entitlement_metering.issue_first_trial(...)` with the exact §6.2 signature now that all four required tables exist; this placement is explicit and no later step may infer or move it before a dependency table.
7. `CREATE TABLE entitlement_metering.entitlement_issuance` with columns, PK, one unique + one partial unique and ten named checks. Create `ix_issuance_order_status`, `ix_issuance_event`.
8. `CREATE TABLE entitlement_metering.usage_reservation` with columns, PK, one unique key and nine named checks. Create `ix_usage_reservation_balance_status`, `ix_usage_reservation_reconcile`.
9. `CREATE TABLE entitlement_metering.usage_fact` with columns, PK, two unique keys and six named checks. Create `ix_usage_fact_call`.
10. Create exactly the seven trigger functions in §4.7.
11. Create exactly the seven triggers in §4.7 with the Option A bindings.
12. Still under `entitlement_metering_owner`, `REVOKE ALL` on each of the seven tables and all eight functions (one service + seven trigger functions) from `PUBLIC`.
13. Still under `entitlement_metering_owner`, issue every grant on an `entitlement_metering`-owned object: the exact owned-table privileges to `entitlement_metering_role`; `EXECUTE` on the six dedicated `guard_*` functions to `entitlement_metering_role`; no runtime `EXECUTE` on `reject_row_mutation`; `EXECUTE` on `issue_first_trial` only to `activation_consent_role` and `entitlement_metering_role`; `USAGE` without `CREATE` on schema `entitlement_metering` to `catalog_order_role`; and the exact V006 column `SELECT` grants on the new tables to `ivr_role` and `catalog_order_role`. Existing V004 schema/function grants are verified, not reissued.
14. `RESET ROLE`.
15. `SET LOCAL ROLE platform_reliability_owner`; grant `USAGE` without `CREATE` on schema `platform` and `EXECUTE` on `platform.outbox_append(text,text,text,uuid,text,jsonb,text,text)` to the NOLOGIN `entitlement_metering_owner` only; then `RESET ROLE`. This owner block is the only source of the internal F-02 capability.
16. `SET LOCAL ROLE subject_number_owner`; grant `USAGE` without `CREATE` on schema `subject_number` and the exact `business_subject(id, owner_account_id, owner_organization_id, revision)` column `SELECT` to `entitlement_metering_role`; then `RESET ROLE`.
17. Do not create a `catalog_order_owner` grant block: V006 grants no privilege on a `catalog_order_owner`-owned object. The accepted V004 `entitlement_metering_role` reads on `customer_order`, `payment_order` and `payment_event`, plus V004 platform grants to all four S2 runtime roles, remain unchanged and are verified cumulatively.
18. As the migration identity after all owner blocks are reset, add the 15 ordinary immediate RESTRICT FKs in the order listed in §4.5.
19. Add `ALTER TABLE ivr.ivr_decision ADD CONSTRAINT fk_ivr_decision_reservation FOREIGN KEY (reservation_id) REFERENCES entitlement_metering.usage_reservation(id) ON UPDATE RESTRICT ON DELETE RESTRICT` last as the late `ALTER_FK`. Ordinary immediate, not deferrable.
20. No other statement, owner block, grantor bypass or superuser-only substitute.

Grantor invariant: grants on `platform` objects are issued only as `platform_reliability_owner`; grants on schema/tables/functions owned by `entitlement_metering_owner` are issued only as `entitlement_metering_owner`; grants on `subject_number` are issued only as `subject_number_owner`. The migration identity is used only for the cross-owner FK block after `RESET ROLE`. Tests assert both the effective ACL and the object owner that was capable of issuing it; `PUBLIC` remains zero and no runtime role gains owner-only capability.

### 4.2 Column dictionary (84 columns)

All column lists are mechanical transcriptions of docs/14 §4.29-§4.35. Types/nullability/defaults are frozen; the plan adds none.

#### 4.2.1 `entitlement_metering.trial_policy_version` — 11 columns

| Column | Type | Null | Default | Notes |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_trial_policy_version` |
| campaign_key | text | NO | none | CHECK `ck_trial_policy_campaign`; `uq_trial_policy_campaign_version`; `uq_trial_policy_enabled` |
| version_no | integer | NO | none | CHECK `ck_trial_policy_version_no`; `uq_trial_policy_campaign_version` |
| granted_seconds | integer | NO | none | CHECK `ck_trial_policy_granted_seconds` |
| valid_days | integer | NO | none | CHECK `ck_trial_policy_valid_days` |
| enabled | boolean | NO | `true` | `uq_trial_policy_enabled` |
| snapshot | jsonb | NO | none | CHECK `ck_trial_policy_snapshot_type` |
| snapshot_sha256 | text | NO | none | CHECK `ck_trial_policy_snapshot_sha` |
| published_by_employee_id | uuid | NO | none | FK `fk_trial_policy_employee` |
| published_at | timestamptz | YES | none | Null draft; set once at publication |
| revision | integer | NO | `1` | CHECK `ck_trial_policy_revision` |

#### 4.2.2 `entitlement_metering.entitlement_grant` — 13 columns

| Column | Type | Null | Default | Notes |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_entitlement_grant` |
| subject_id | uuid | NO | none | FK `fk_entitlement_grant_subject` |
| source_type | text | NO | none | CHECK `ck_entitlement_grant_source`; `ck_entitlement_grant_source_shape` |
| source_ref_id | uuid | NO | none | Polymorphic non-FK (policy version for TRIAL, order for PURCHASE) |
| entitlement_type | text | NO | none | CHECK `ck_entitlement_grant_type`; `uq_entitlement_grant_paid` |
| granted_units | bigint | NO | none | CHECK `ck_entitlement_grant_units` |
| valid_from | timestamptz | NO | none | |
| expires_at | timestamptz | NO | none | CHECK `ck_entitlement_grant_expiry` |
| order_id | uuid | YES | none | FK `fk_entitlement_grant_order`; `uq_entitlement_grant_paid` |
| installment_no | integer | YES | none | CHECK `ck_entitlement_grant_installment`; `uq_entitlement_grant_paid` |
| snapshot | jsonb | NO | none | CHECK `ck_entitlement_grant_snapshot_type` |
| snapshot_sha256 | text | NO | none | CHECK `ck_entitlement_grant_snapshot_sha` |
| granted_at | timestamptz | NO | `now()` | |

#### 4.2.3 `entitlement_metering.trial_grant` — 10 columns

| Column | Type | Null | Default | Notes |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_trial_grant` |
| subject_id | uuid | NO | none | UNIQUE `uq_trial_grant_subject`; FK `fk_trial_grant_subject` |
| campaign_key | text | NO | none | CHECK `ck_trial_grant_campaign` |
| trial_policy_version_id | uuid | NO | none | FK `fk_trial_grant_policy` |
| entitlement_grant_id | uuid | NO | none | UNIQUE `uq_trial_grant_grant`; FK `fk_trial_grant_grant` |
| source_action_domain | text | NO | none | CHECK `ck_trial_grant_action_domain`; `uq_trial_grant_source` |
| source_action_event_id | text | NO | none | CHECK `ck_trial_grant_event_id`; `uq_trial_grant_source` |
| request_fingerprint | text | NO | none | CHECK `ck_trial_grant_fingerprint` |
| granted_at | timestamptz | NO | `now()` | |
| expires_at | timestamptz | NO | none | CHECK `ck_trial_grant_expiry` |

#### 4.2.4 `entitlement_metering.entitlement_balance` — 8 columns

| Column | Type | Null | Default | Notes |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_entitlement_balance` |
| grant_id | uuid | NO | none | UNIQUE `uq_entitlement_balance_grant`; FK `fk_entitlement_balance_grant` |
| granted_units | bigint | NO | none | CHECK `ck_entitlement_balance_granted` |
| reserved_units | bigint | NO | `0` | CHECK `ck_entitlement_balance_units` |
| consumed_units | bigint | NO | `0` | CHECK `ck_entitlement_balance_units` |
| status | text | NO | `'AVAILABLE'` | CHECK `ck_entitlement_balance_status`; `ck_entitlement_balance_status_shape` |
| revision | integer | NO | `1` | CHECK `ck_entitlement_balance_revision` |
| updated_at | timestamptz | NO | `now()` | |

#### 4.2.5 `entitlement_metering.entitlement_issuance` — 14 columns

| Column | Type | Null | Default | Notes |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_entitlement_issuance` |
| order_id | uuid | NO | none | FK `fk_issuance_order`; `uq_issuance_line` |
| installment_no | integer | NO | none | CHECK `ck_issuance_installment`; `uq_issuance_line` |
| entitlement_type | text | NO | none | CHECK `ck_issuance_type`; `uq_issuance_line` |
| status | text | NO | `'PENDING'` | CHECK `ck_issuance_status`; `ck_issuance_state_shape` |
| attempt_count | integer | NO | `0` | CHECK `ck_issuance_attempt_count` |
| revision | integer | NO | `1` | CHECK `ck_issuance_revision` |
| last_error_code | text | YES | none | CHECK `ck_issuance_last_error` |
| grant_id | uuid | YES | none | UNIQUE `uq_issuance_grant`; FK `fk_issuance_grant` |
| source_event_domain | text | NO | none | CHECK `ck_issuance_event_domain` |
| source_event_id | text | NO | none | CHECK `ck_issuance_event_id` |
| first_attempt_at | timestamptz | YES | none | |
| issued_at | timestamptz | YES | none | CHECK `ck_issuance_state_shape` |
| updated_at | timestamptz | NO | `now()` | |

#### 4.2.6 `entitlement_metering.usage_reservation` — 16 columns

| Column | Type | Null | Default | Notes |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_usage_reservation` |
| call_id | text | NO | none | UNIQUE `uq_usage_reservation_call`; FK `fk_usage_reservation_call` |
| balance_id | uuid | NO | none | FK `fk_usage_reservation_balance` |
| subject_id | uuid | NO | none | FK `fk_usage_reservation_subject` |
| authorized_units | bigint | NO | none | CHECK `ck_usage_reservation_authorized` |
| status | text | NO | `'HELD'` | CHECK `ck_usage_reservation_status`; `ck_usage_reservation_state_shape` |
| authorization_expires_at | timestamptz | NO | none | Never renewed by replay |
| settled_units | bigint | NO | `0` | CHECK `ck_usage_reservation_settled`; `ck_usage_reservation_state_shape` |
| reconciliation_due_at | timestamptz | YES | none | CHECK `ck_usage_reservation_state_shape` |
| reconciliation_attempt_count | integer | NO | `0` | CHECK `ck_usage_reservation_reconciliation_attempts` |
| last_reconciled_at | timestamptz | YES | none | |
| authority_source | text | YES | none | CHECK `ck_usage_reservation_authority`; `ck_usage_reservation_state_shape` |
| last_error_code | text | YES | none | CHECK `ck_usage_reservation_last_error` |
| created_at | timestamptz | NO | `now()` | |
| terminal_at | timestamptz | YES | none | CHECK `ck_usage_reservation_state_shape` |
| revision | integer | NO | `1` | CHECK `ck_usage_reservation_revision` |

#### 4.2.7 `entitlement_metering.usage_fact` — 12 columns

| Column | Type | Null | Default | Notes |
| --- | --- | --- | --- | --- |
| id | uuid | NO | none | PK `pk_usage_fact` |
| reservation_id | uuid | NO | none | FK `fk_usage_fact_reservation`; `uq_usage_fact_reservation_type` |
| balance_id | uuid | NO | none | FK `fk_usage_fact_balance` |
| call_id | text | NO | none | FK `fk_usage_fact_call` |
| usage_type | text | NO | none | CHECK `ck_usage_fact_type`; `uq_usage_fact_reservation_type` |
| units | bigint | NO | none | CHECK `ck_usage_fact_units` |
| source_event_domain | text | NO | none | CHECK `ck_usage_fact_event_domain`; `uq_usage_fact_source` |
| source_event_id | text | NO | none | CHECK `ck_usage_fact_event_id`; `uq_usage_fact_source` |
| occurred_at | timestamptz | NO | none | |
| created_at | timestamptz | NO | `now()` | |
| snapshot | jsonb | NO | none | CHECK `ck_usage_fact_snapshot_type` |
| snapshot_sha256 | text | NO | none | CHECK `ck_usage_fact_snapshot_sha` |

Column total: 11+13+10+8+14+16+12 = **84**.

### 4.3 Named checks (50)

Exact names and predicates from docs/14 §4.29-§4.35. The implementation must store the whole normalized `pg_get_constraintdef` and assert name + predicate, not name only.

| Table | Count | Names |
| --- | ---: | --- |
| trial_policy_version | 7 | `ck_trial_policy_campaign`, `ck_trial_policy_version_no`, `ck_trial_policy_granted_seconds`, `ck_trial_policy_valid_days`, `ck_trial_policy_snapshot_type`, `ck_trial_policy_snapshot_sha`, `ck_trial_policy_revision` |
| entitlement_grant | 8 | `ck_entitlement_grant_source`, `ck_entitlement_grant_source_shape`, `ck_entitlement_grant_type`, `ck_entitlement_grant_units`, `ck_entitlement_grant_expiry`, `ck_entitlement_grant_installment`, `ck_entitlement_grant_snapshot_type`, `ck_entitlement_grant_snapshot_sha` |
| trial_grant | 5 | `ck_trial_grant_campaign`, `ck_trial_grant_action_domain`, `ck_trial_grant_event_id`, `ck_trial_grant_fingerprint`, `ck_trial_grant_expiry` |
| entitlement_balance | 5 | `ck_entitlement_balance_granted`, `ck_entitlement_balance_units`, `ck_entitlement_balance_status`, `ck_entitlement_balance_status_shape`, `ck_entitlement_balance_revision` |
| entitlement_issuance | 10 | `ck_issuance_installment`, `ck_issuance_type`, `ck_issuance_status`, `ck_issuance_state_shape`, `ck_issuance_attempt_count`, `ck_issuance_revision`, `ck_issuance_last_error`, `ck_issuance_event_domain`, `ck_issuance_event_id`, `ck_issuance_time_order` |
| usage_reservation | 9 | `ck_usage_reservation_authorized`, `ck_usage_reservation_status`, `ck_usage_reservation_settled`, `ck_usage_reservation_state_shape`, `ck_usage_reservation_reconciliation_attempts`, `ck_usage_reservation_authority`, `ck_usage_reservation_last_error`, `ck_usage_reservation_time_order`, `ck_usage_reservation_revision` |
| usage_fact | 6 | `ck_usage_fact_type`, `ck_usage_fact_units`, `ck_usage_fact_event_domain`, `ck_usage_fact_event_id`, `ck_usage_fact_snapshot_type`, `ck_usage_fact_snapshot_sha` |
| **Total** | **50** | |

Normative predicates that tests must probe with `23514` + exact name include at least:

- `ck_trial_policy_campaign`: `campaign_key = 'FIRST-TRIAL'`.
- `ck_entitlement_grant_source_shape`: TRIAL implies null order/installment; PURCHASE implies non-null order/installment, `installment_no >= 1`, and `source_ref_id = order_id`.
- `ck_entitlement_grant_type` / `ck_issuance_type`: `IN ('RECORDING_SECONDS','AI_SECONDS','EVIDENCE_COUNT','STORAGE_BYTES')`.
- `ck_entitlement_balance_units`: `reserved_units >= 0 AND consumed_units >= 0 AND reserved_units + consumed_units <= granted_units`.
- `ck_entitlement_balance_status_shape`: `(status = 'EXHAUSTED') = (consumed_units = granted_units)`.
- `ck_issuance_state_shape`: ISSUED iff grant_id and issued_at non-null; FAILED iff last_error_code non-null.
- `ck_usage_reservation_settled`: `settled_units >= 0 AND settled_units <= authorized_units` (F-10: no tolerance parameter).
- `ck_usage_reservation_state_shape`: exact HELD/RECONCILING/SETTLED/RELEASED quadrants.
- `ck_usage_fact_type`: `usage_type IN ('RECORDING_SECONDS')`.
- Snapshot checks: `jsonb_typeof(snapshot) = 'object'` and `snapshot_sha256 ~ '^[0-9a-f]{64}$'`.

### 4.4 Keys and indexes

| Table | PK | Unique keys | Secondary indexes | Total index objects |
| --- | --- | --- | --- | ---: |
| trial_policy_version | `pk_trial_policy_version (id)` | `uq_trial_policy_campaign_version (campaign_key, version_no)`; partial `uq_trial_policy_enabled (campaign_key) WHERE enabled AND published_at IS NOT NULL` | `ix_trial_policy_enabled (enabled, published_at)` | 4 |
| entitlement_grant | `pk_entitlement_grant (id)` | partial `uq_entitlement_grant_paid (order_id, installment_no, entitlement_type) WHERE source_type = 'PURCHASE'` | `ix_entitlement_grant_subject_type (subject_id, entitlement_type, expires_at)`; `ix_entitlement_grant_order (order_id, installment_no)` | 4 |
| trial_grant | `pk_trial_grant (id)` | `uq_trial_grant_subject (subject_id)`; `uq_trial_grant_grant (entitlement_grant_id)`; `uq_trial_grant_source (source_action_domain, source_action_event_id)` | `ix_trial_grant_campaign_time (campaign_key, granted_at)` | 5 |
| entitlement_balance | `pk_entitlement_balance (id)` | `uq_entitlement_balance_grant (grant_id)` | `ix_entitlement_balance_status (status)` | 3 |
| entitlement_issuance | `pk_entitlement_issuance (id)` | `uq_issuance_line (order_id, installment_no, entitlement_type)`; partial `uq_issuance_grant (grant_id) WHERE grant_id IS NOT NULL` | `ix_issuance_order_status (order_id, status)`; `ix_issuance_event (source_event_domain, source_event_id)` | 5 |
| usage_reservation | `pk_usage_reservation (id)` | `uq_usage_reservation_call (call_id)` | `ix_usage_reservation_balance_status (balance_id, status)`; `ix_usage_reservation_reconcile (status, reconciliation_due_at)` | 4 |
| usage_fact | `pk_usage_fact (id)` | `uq_usage_fact_reservation_type (reservation_id, usage_type)`; `uq_usage_fact_source (source_event_domain, source_event_id)` | `ix_usage_fact_call (call_id)` | 4 |
| **Totals** | **7** | **12** (3 partial) | **10** | **29** |

### 4.5 Foreign keys (16 decisions)

All FKs are `ON UPDATE RESTRICT ON DELETE RESTRICT`. Ordinary immediate, not deferrable. No cascade.

| # | Constraint | Child | Parent | Mode |
| ---: | --- | --- | --- | --- |
| 1 | `fk_trial_policy_employee` | `trial_policy_version.published_by_employee_id` | `operations_iam.employee_account(id)` | immediate |
| 2 | `fk_entitlement_grant_subject` | `entitlement_grant.subject_id` | `subject_number.business_subject(id)` | immediate |
| 3 | `fk_entitlement_grant_order` | `entitlement_grant.order_id` | `catalog_order.customer_order(id)` | immediate |
| 4 | `fk_trial_grant_subject` | `trial_grant.subject_id` | `subject_number.business_subject(id)` | immediate |
| 5 | `fk_trial_grant_policy` | `trial_grant.trial_policy_version_id` | `entitlement_metering.trial_policy_version(id)` | immediate |
| 6 | `fk_trial_grant_grant` | `trial_grant.entitlement_grant_id` | `entitlement_metering.entitlement_grant(id)` | immediate |
| 7 | `fk_entitlement_balance_grant` | `entitlement_balance.grant_id` | `entitlement_metering.entitlement_grant(id)` | immediate |
| 8 | `fk_issuance_order` | `entitlement_issuance.order_id` | `catalog_order.customer_order(id)` | immediate |
| 9 | `fk_issuance_grant` | `entitlement_issuance.grant_id` | `entitlement_metering.entitlement_grant(id)` | immediate |
| 10 | `fk_usage_reservation_call` | `usage_reservation.call_id` | `ivr.call_record(call_id)` | immediate |
| 11 | `fk_usage_reservation_balance` | `usage_reservation.balance_id` | `entitlement_metering.entitlement_balance(id)` | immediate |
| 12 | `fk_usage_reservation_subject` | `usage_reservation.subject_id` | `subject_number.business_subject(id)` | immediate |
| 13 | `fk_usage_fact_reservation` | `usage_fact.reservation_id` | `entitlement_metering.usage_reservation(id)` | immediate |
| 14 | `fk_usage_fact_balance` | `usage_fact.balance_id` | `entitlement_metering.entitlement_balance(id)` | immediate |
| 15 | `fk_usage_fact_call` | `usage_fact.call_id` | `ivr.call_record(call_id)` | immediate |
| 16 | `fk_ivr_decision_reservation` | `ivr.ivr_decision.reservation_id` | `entitlement_metering.usage_reservation(id)` | **ALTER_FK** (late) |

`entitlement_grant.source_ref_id` is intentionally not an FK (polymorphic: trial-policy version or order). It is not a catalog `NO_FK` decision row. No new root `NO_FK` identity is introduced.

### 4.6 ACL (owner, runtime, cross-module, PUBLIC)

Owned-table grants to `entitlement_metering_role` (no `DELETE`/`TRUNCATE`):

| Table | SELECT | INSERT | UPDATE columns |
| --- | --- | --- | --- |
| `trial_policy_version` | all | all | `published_by_employee_id`, `enabled`, `published_at`, `revision` (the single publication UPDATE) |
| `entitlement_grant` | all | all | none (append-only) |
| `trial_grant` | all | none (function only) | none (append-only) |
| `entitlement_balance` | all | all | `reserved_units`, `consumed_units`, `status`, `revision`, `updated_at` |
| `entitlement_issuance` | all | all | `status`, `attempt_count`, `last_error_code`, `grant_id`, `first_attempt_at`, `issued_at`, `updated_at`, `revision` |
| `usage_reservation` | all | all | `status`, `settled_units`, `reconciliation_due_at`, `reconciliation_attempt_count`, `last_reconciled_at`, `authority_source`, `last_error_code`, `terminal_at`, `revision` |
| `usage_fact` | all | all | none (append-only) |

Cross-module `SELECT` grants **newly issued by V006** (no write, no row lock) — these tables did not exist before V006:

| Grantee | Table | Exact columns | Note |
| --- | --- | --- | --- |
| `ivr_role` | `entitlement_metering.entitlement_balance` | `id`, `grant_id`, `granted_units`, `reserved_units`, `consumed_units`, `status` | Routing snapshot only (F-03/OQ-2) |
| `ivr_role` | `entitlement_metering.entitlement_grant` | `id`, `subject_id`, `entitlement_type`, `granted_units`, `valid_from`, `expires_at` | Routing snapshot validity |
| `catalog_order_role` | `entitlement_metering.entitlement_issuance` | `order_id`, `installment_no`, `entitlement_type`, `status`, `grant_id`, `issued_at`, `last_error_code` | R-04 projection |
| `catalog_order_role` | `entitlement_metering.entitlement_grant` | `id`, `subject_id`, `entitlement_type`, `granted_units`, `valid_from`, `expires_at` | R-04 projection inputs |
| `entitlement_metering_role` | `subject_number.business_subject` | `id`, `owner_account_id`, `owner_organization_id`, `revision` | Subject/owner facts; paired with new `subject_number` USAGE |

Cross-module `SELECT` grants **already issued by V004** (do not re-grant; verify only):

| Grantee | Table | Exact columns | Note |
| --- | --- | --- | --- |
| `entitlement_metering_role` | `catalog_order.customer_order` | `id`, `account_id`, `owner_organization_id`, `subject_id`, `catalog_version_id`, `snapshot`, `snapshot_sha256`, `payment_status`, `currency`, `created_at` | Frozen paid-order snapshot (V004) |
| `entitlement_metering_role` | `catalog_order.payment_order` | `id`, `order_id`, `status`, `amount_cents`, `currency`, `updated_at` | Verified payment facts (V004) |
| `entitlement_metering_role` | `catalog_order.payment_event` | `id`, `payment_order_id`, `provider`, `provider_event_id`, `event_type`, `signature_verified`, `amount_cents`, `occurred_at` | Source event identity (V004) |

Schema USAGE completed by V006 (F-01 pairing for the new reads):

- `GRANT USAGE ON SCHEMA entitlement_metering TO catalog_order_role` (without `CREATE`).
- `GRANT USAGE ON SCHEMA subject_number TO entitlement_metering_role` (without `CREATE`).
- Existing V004 USAGE grants to `entitlement_metering_role`/`activation_consent_role`/`ivr_role` on `entitlement_metering`, and to `entitlement_metering_role` on `catalog_order`, remain unchanged.

Function grants:

- `EXECUTE` on `entitlement_metering.issue_first_trial` only to `activation_consent_role` and `entitlement_metering_role`.
- `EXECUTE` on the six `guard_*` trigger functions only to `entitlement_metering_role`.
- No `EXECUTE` on `entitlement_metering.reject_row_mutation` to any runtime role (owner retains inherent EXECUTE as function owner).
- Internal owner capability (F-02): V006 newly grants `USAGE` on `platform` and `EXECUTE` on `platform.outbox_append` to the NOLOGIN `entitlement_metering_owner`; no other S2 owner role or `PUBLIC` receives that owner capability. This does not revoke or contradict the accepted V004 runtime grants: `catalog_order_role`, `ivr_role`, `entitlement_metering_role` and `activation_consent_role` already have platform `USAGE` and `EXECUTE` on `outbox_append` and continue to have them.
- `PUBLIC` receives zero schema/table/function/column privileges on every V006 object.

### 4.7 Triggers and trigger functions (7 + 7)

All are `SECURITY INVOKER`, owned by `entitlement_metering_owner`, `SET search_path = entitlement_metering, pg_temp` (except any that only reject; still set the fixed path), no transaction control, no function-wave entry.

| Trigger name | Function | Table | Predicate |
| --- | --- | --- | --- |
| `trg_trial_policy_version_guard` | `guard_trial_policy_version` | `trial_policy_version` | Reject campaign/version/granted seconds/valid days/snapshot/publisher/`published_at` changes after publication; while `OLD.published_at IS NULL`, allow the one publication UPDATE that sets publisher, `published_at` once from NULL, `enabled` and `revision` together; reject any later publisher/content change |
| `trg_entitlement_grant_immutable` | `guard_entitlement_grant` | `entitlement_grant` | Reject any update/delete |
| `trg_trial_grant_immutable` | `guard_trial_grant` | `trial_grant` | Reject any update/delete |
| `trg_entitlement_balance_guard` | `guard_entitlement_balance` | `entitlement_balance` | Reject changes to `grant_id` or `granted_units` |
| `trg_entitlement_issuance_guard` | `guard_entitlement_issuance` | `entitlement_issuance` | Reject changes to `order_id`, `installment_no`, `entitlement_type` or source-event columns |
| `trg_usage_reservation_guard` | `guard_usage_reservation` | `usage_reservation` | Reject changes to `call_id`, `balance_id`, `subject_id`, `authorized_units`, `authorization_expires_at` or `created_at` |
| `trg_usage_fact_immutable` | `reject_row_mutation` | `usage_fact` | Reject update/delete |

Authorized Option A adjudication (F-PA6-PLAN-01): docs/14 §10.16 and §11.4 are controlling. The §4.30 control sentence is corrected to `guard_entitlement_grant()`, the §4.31 control sentence is corrected to `guard_trial_grant()`, and the §4.35 `usage_fact` sentence remains unchanged with `reject_row_mutation()`. Therefore the exact inventory is seven trigger functions and seven trigger bindings, not five; the ACL oracle is six dedicated `guard_*` functions executable by `entitlement_metering_role`, shared `reject_row_mutation()` executable only inherently by its owner, and zero `PUBLIC` execute. Tests compare the exact `pg_proc`, `pg_trigger`, `tgfoid` and `proacl` sets shown above.

### 4.8 Defaults (19) and sequences

| Table | Defaults |
| --- | --- |
| trial_policy_version | `enabled = true`, `revision = 1` |
| entitlement_grant | `granted_at = now()` |
| trial_grant | `granted_at = now()` |
| entitlement_balance | `reserved_units = 0`, `consumed_units = 0`, `status = 'AVAILABLE'`, `revision = 1`, `updated_at = now()` |
| entitlement_issuance | `status = 'PENDING'`, `attempt_count = 0`, `revision = 1`, `updated_at = now()` |
| usage_reservation | `status = 'HELD'`, `settled_units = 0`, `reconciliation_attempt_count = 0`, `created_at = now()`, `revision = 1` |
| usage_fact | `created_at = now()` |

Zero `id` defaults. Zero sequences. Application (or `issue_first_trial`) generates `id` values with `pg_catalog.gen_random_uuid()`; there is no `p_id` argument and no serial column.

### 4.9 Lifecycle classes (§12.5)

| Class | V006 tables | Rule |
| --- | --- | --- |
| Mutable head | `trial_policy_version`, `entitlement_balance`, `entitlement_issuance`, `usage_reservation` | Status/terminal-time retirement, conditional revision, no ordinary hard delete |
| Append-only / frozen | `entitlement_grant`, `trial_grant`, `usage_fact` | No update/delete privilege; append-only trigger; stable identity and hash |

## 5. State, immutability and sensitive-data boundaries

### 5.1 State and immutability

- `trial_policy_version`: `campaign_key` permanently `FIRST-TRIAL`; published content immutable; publication is one owner UPDATE under the `entitlement_metering.trial_policy:FIRST-TRIAL` advisory key (§9.26); disable then publish so `uq_trial_policy_enabled` sees at most one qualifying row per statement.
- `entitlement_grant` / `trial_grant` / `usage_fact`: append-only; `entitlement_metering_role` has `SELECT`/`INSERT` only (trial_grant INSERT none); guards reject update/delete.
- `entitlement_balance`: `grant_id`/`granted_units` immutable; `reserved_units`/`consumed_units`/`status`/`revision`/`updated_at` change only under the balance row lock in the reservation/settlement transaction; `EXHAUSTED` is derived (`consumed_units = granted_units`), never a second row (OQ-8).
- `entitlement_issuance`: line identity and source event immutable; `PENDING → ISSUED` terminal; `PENDING → FAILED` and `FAILED → PENDING` retry allowed; grant written at most once via `uq_issuance_grant` + paid grant partial unique.
- `usage_reservation`: call/balance/subject/authorized_units/authorization_expires_at/created_at immutable; settlement at most once; known-zero use releases once with `settled_units = 0` and no positive `usage_fact`; unknown use stays `RECONCILING` and is never released (F-10).
- F-10 settlement boundary: no tolerance value is approved (Q8 open). The normative baseline is exactly `ck_usage_reservation_settled`. Over-authorized duration is not clamped, not settled, not released; it stays/moves to `RECONCILING` with a stable quarantine code. The phrase "plus approved tolerance" grants no current tolerance.

### 5.2 Sensitive-data boundaries

- No protected party/media value is stored in V006 tables. Snapshots contain policy/grant/usage facts only.
- `issue_first_trial` Outbox and result snapshots contain internal ids, campaign/policy facts and redacted status only (§10.7 denylist).
- Trial-grant fingerprints are lowercase SHA-256; source action identity is internal replay fact.
- No raw number, callee, identity or secret is written by `issue_first_trial`.

## 6. Slice design: two slices

Every slice leaves a runnable V001-V006 prefix and is fail-closed. No application-service or V007+ work enters a slice.

### 6.1 Why two slices

- **S1 — V006 migration, exact inventory/ACL and `issue_first_trial` core outcomes** is one observable contract boundary: "V006 exists, applies after V005, is exactly the frozen seven-table + one-function + late-FK persistence/security contract, `issue_first_trial` returns GRANTED/REPLAY/ALREADY_GRANTED and fails closed on conflict/policy/argument errors, and V001-V005 are unchanged." It includes the complete migration (tables, guards, function, late FK, grants), the catalog/ACL assertions and the core function-outcome method.
- **S2 — executable lifecycle, guard, FK, nested-Outbox rollback, concurrency and cumulative evidence** is the second boundary: "the DDL's claimed behaviors are executable and fail closed." It adds lifecycle/guard, FK/late-FK, nested-Outbox rollback and concurrency methods, synchronizes CI/README, and runs one full Testcontainers profile plus V001-V005 immutability checks. It does not edit V006 unless a controller-approved bounded correction is required by a failing frozen-contract assertion.
- Two slices are the minimum falsifiable checkpoints. A third slice would mechanically split "tables" from "function" or "inventory" from "behavior" even though the seven tables, `issue_first_trial` and the late FK form one identity/contract wave with shared fixtures and one Flyway file; splitting by table/file/owner/layer is forbidden. One slice would combine a large migration, 50 checks, 16 FK decisions, seven guards, the function contract, the ACL matrix and all concurrency evidence into a single review surface. **Two slices** comply with the "normally no more than three" principle and match the accepted PA5 cost model.

### 6.2 S1 - V006 applies after V005; exact inventory/ACL and issue_first_trial core outcomes

**Prerequisites.** Accepted PA5/V005 foundation on base `aa2b0925e6b8ffdac4587f2e44c4c0dde393d649`; V001-V005 hashes in §2.2; Docker-capable PostgreSQL 16; current 38-method suite green; clean worktree except the controller edit and known historical `.pa5-*` files.

**Allowed files.** V006 migration (new, authored once); `PostgresContainerIT.java` (S1 changes below); `.github/workflows/ci.yml` (expected set 41); `backend/README.md` (S1, 41); the S1 implementation artifact (new).

**Exact DDL.** Implement §4.1/§4.2/§4.3/§4.4/§4.5/§4.6/§4.7/§4.8 in one migration. No other object. No schema/role creation. No service function besides `issue_first_trial`.

**Exact `issue_first_trial` contract (§10.9, implemented in S1).**

Signature:

`entitlement_metering.issue_first_trial(p_subject_id uuid, p_campaign_key text, p_trial_policy_version_id uuid, p_action_domain text, p_action_event_id text, p_request_fingerprint text) RETURNS TABLE (trial_grant_id uuid, entitlement_grant_id uuid, entitlement_balance_id uuid, outcome text, first_granted_at timestamptz)`

| Item | Contract |
| --- | --- |
| Owner / mode | `entitlement_metering_owner`; `SECURITY DEFINER`; `SET search_path = entitlement_metering, platform, pg_temp`; created after `trial_policy_version`, `entitlement_grant`, `trial_grant` and `entitlement_balance`; nested `platform.outbox_append` runs with this owner's privileges (F-02) |
| Caller roles | `EXECUTE` to `activation_consent_role` and `entitlement_metering_role` only; `PUBLIC` denied |
| Arguments | Validate before the subject lock or any write. `p_subject_id`, `p_trial_policy_version_id`, `p_campaign_key`, `p_action_domain`, `p_action_event_id` and `p_request_fingerprint` must be non-null; the subject must exist; `p_action_domain` must be exactly `IVR_CONFIRM` or `ACTIVATION`; `p_action_event_id` must have length 1-128 and `btrim(...) <> ''`; `p_request_fingerprint` must match `^[0-9a-f]{64}$`. Nulls, unsupported/whitespace action domain, empty/whitespace-only/overlength event id and malformed/whitespace fingerprint raise `TRIAL_ARGUMENT_INVALID`. A non-null campaign value other than exact `FIRST-TRIAL`, including padded or whitespace-only text, raises `TRIAL_CAMPAIGN_INVALID`. No trimming, case folding or alternate spelling is accepted. |
| Outcomes | `GRANTED` returns the new ids and the one captured `v_granted_at`; `REPLAY` returns the original ids and persisted `trial_grant.granted_at` for the same action identity + same fingerprint; `ALREADY_GRANTED` returns the same original ids and persisted `trial_grant.granted_at` for a different action identity. `first_granted_at` never uses call time, policy publication time, balance `updated_at` or a recomputation. |
| Errors | Every named `TRIAL_*` exception is raised as exact message text with SQLSTATE `P0001`, matching accepted V001 custom-error precedent. `TRIAL_ARGUMENT_INVALID`; `TRIAL_CAMPAIGN_INVALID`; `TRIAL_ACTION_CONFLICT` for the existing subject trial with the same action identity and changed fingerprint; `TRIAL_DATA_INTEGRITY` for the exact persisted-row predicates below; `TRIAL_POLICY_NOT_FOUND` for a missing/disabled/unpublished/stale expected policy at the sole authoritative read. Constraint/FK/privilege failures not mapped by this contract retain their native SQLSTATE and are never swallowed or relabeled. |
| Order | 1) subject lifecycle advisory lock (re-entrant with activation); 2) read `trial_grant` for subject first (REPLAY / ALREADY_GRANTED / conflict before policy); 3) only if no trial exists, sole authoritative policy read (`campaign_key='FIRST-TRIAL' AND enabled AND published_at IS NOT NULL AND id = p_trial_policy_version_id`); 4) insert one `entitlement_grant` (`source_type='TRIAL'`, `source_ref_id=p_trial_policy_version_id`, retained units/window/snapshot), one `trial_grant`, one `entitlement_balance`, one Outbox via `platform.outbox_append`; 5) return `GRANTED` |
| Time | `granted_at` once from `clock_timestamp()`; `valid_from = granted_at`; `expires_at = granted_at + (valid_days * interval '1 day')` UTC |
| IDs | `pg_catalog.gen_random_uuid()`; no `p_id` argument |
| Atomicity | Caller's transaction; no `COMMIT`/`ROLLBACK`/autonomous transaction/transaction-level `SET`; any exception aborts the caller and rolls back trial/grant/balance/Outbox together; nested Outbox failure rolls back all |
| OQ-6 | The function-internal enabled/published/expected-version read is the policy-selection linearization point; replay/already-granted return prior ids before any policy selection; no second enabled recheck; publication advisory key never combined with IVR call-path lock order |

`TRIAL_DATA_INTEGRITY` is bounded to facts derivable from frozen rows, not a new product rule. After finding the subject's permanent `trial_grant`, the function must find exactly its referenced `entitlement_grant` and that grant's unique `entitlement_balance`, and require: grant `subject_id = trial_grant.subject_id`; grant `source_type = 'TRIAL'`; grant `source_ref_id = trial_grant.trial_policy_version_id`; grant `entitlement_type = 'RECORDING_SECONDS'`; grant `order_id` and `installment_no` are null; grant `granted_units = balance.granted_units`; grant `valid_from = grant.granted_at`; grant `granted_at = trial_grant.granted_at`; grant `expires_at = trial_grant.expires_at`; and balance `grant_id = grant.id`. A missing grant, missing balance, any mismatch, or an existing `(source_action_domain, source_action_event_id)` row bound to a different subject is `TRIAL_DATA_INTEGRITY`. The function does not reinterpret mutable balance counters/status as trial identity. For a valid existing subject trial, same action + same fingerprint is `REPLAY`, same action + changed fingerprint is `TRIAL_ACTION_CONFLICT`, and different action is `ALREADY_GRANTED`.

Fingerprint and Outbox hash construction are separated precisely:

- `p_request_fingerprint` is service-owned input. The owning application service computes lowercase SHA-256 over RFC 8785-compatible canonical JSON using exactly the §10.17 semantic fields for the applicable `trial_grant` scope (`IVR_CONFIRM` or `ACTIVATION`). V006 validates only the 64-lowercase-hex shape, persists the value and compares it for replay/conflict; it does not implement or claim a generic RFC 8785 canonicalizer and does not invent a new semantic field.
- The function itself produces the fixed §10.17 Outbox fact: domain `ACTIVATION`; dedupe key `trial_grant:<trial_grant_id>`; aggregate `TRIAL_GRANT` / `trial_grant_id`; event `FIRST_TRIAL_GRANTED`; exact redacted payload fields `trialGrantId`, `entitlementGrantId`, `entitlementBalanceId`, `subjectId`, `campaignKey`, `trialPolicyVersionId`, `grantedUnits`, `validFrom`, `expiresAt`, `grantedAt` and no others. It first builds one `jsonb` payload from the inserted immutable facts. For this one bounded payload only, it constructs the hash input as a no-whitespace JSON object in exact UTF-16 key order `campaignKey`, `entitlementBalanceId`, `entitlementGrantId`, `expiresAt`, `grantedAt`, `grantedUnits`, `subjectId`, `trialGrantId`, `trialPolicyVersionId`, `validFrom`, taking each already-stored JSON token from `payload -> '<field>'`; then `encode(sha256(convert_to(canonical_text, 'UTF8')), 'hex')` supplies `payload_sha256`. UUID/text/timestamp tokens are therefore the exact JSON strings stored in the payload and `grantedUnits` is the exact integer token. This is not exposed as a reusable SQL canonicalization function or claimed as general PostgreSQL RFC 8785 support; it is a fixed registry-row encoder whose result is independently checked by the Java canonicalizer.
- Test oracle: Java builds the applicable request semantic object from the exact §10.17 field set and independently canonicalizes/hashes it for `p_request_fingerprint`; for the Outbox row, Java reads `payload_redacted`, canonicalizes that finite JSON value with the test-side RFC 8785-compatible helper, hashes the UTF-8 bytes, and requires equality with stored `payload_sha256`. It also asserts the exact field set, values, dedupe/aggregate/event identity and same-key/same-hash replay. Final wire encoding remains Q15/Q26 and no serializer framework enters V006.

Forced nested-Outbox failure mechanism: in one method-local transaction, as `platform_reliability_owner`, transactionally `REVOKE EXECUTE ON FUNCTION platform.outbox_append(text,text,text,uuid,text,jsonb,text,text) FROM entitlement_metering_owner`; reset to the migration identity, create a savepoint, set the caller role, and invoke `issue_first_trial` for fresh method-local ids. The exact expected failure is native SQLSTATE `42501` with message containing `permission denied for function outbox_append`; the function must not translate or swallow it. Roll back to the savepoint to clear the failed-statement state while retaining the earlier transactional revoke, reset to the migration identity, and assert zero method-local trial/grant/balance/Outbox rows plus owner `EXECUTE = false` inside the transaction. Roll back the outer transaction so the privilege revocation is undone, then on a fresh connection assert owner `EXECUTE = true` and a normal call can succeed. No catalog mutation is committed, no other method can observe the revoked privilege, and the dedicated runtime ACL method still proves ordinary runtime denials.

**Exact test changes (S1).**

1. Preserve executable V001-V005 negative proof before V006 exists. In `@BeforeAll`, configure Flyway with target `005`, migrate the clean database through V005, and capture/assert on that live prefix: five successful history rows; no `issue_first_trial`; no `fk_ivr_decision_reservation`; zero `entitlement_metering`/`activation_consent` tables; and the accepted V005 nullable `ivr_decision.reservation_id`. Then reconfigure the same database without the target and migrate exactly one pending migration (V006). Rename the test to `v001ThroughV006MigrateCleanPostgres16WithoutSkipping`; assert the staged results are `5` then `1`, the final history has six exact rows including V006, `validate()` succeeds and repeat migrate executes zero. This is executable prefix evidence, not a post-V006 global zero-count or source-text surrogate.
2. Replace `assertNoV006PlusObjects` with `assertNoV007PlusObjects`: `entitlement_metering` has exactly the seven V006 tables; `activation_consent` has zero tables; `evidence`/`sharing` schemas and roles are absent; the V007+ table names (`consent_record`, `service_activation`, `service_activation_transition`, `media_object`, `recording`, `evidence_record`, `evidence_artifact`, `evidence_event`, `evidence_share_grant`, `share_admission`, `share_access_event`) are absent; **no service function beyond `issue_first_trial`** exists in `entitlement_metering` (the seven trigger functions are expected and separately asserted); `fk_customer_order_consent` is absent. Call it from the V006 inventory method. The existing V005 inventory method keeps all V005 table/check/index/guard assertions but replaces only its V006-absence helper call with exact V006-presence/late-FK assertions.
3. Exhaustively evolve every global/cumulative function and trigger assertion without deleting an existing entry: `assertFunctionsAndTrigger` exact name list, exact identity-argument list, owner/`prosecdef`/`proconfig` list, `p_id` zero query, `pg_catalog.gen_random_uuid()` allowlist/count, transaction-control query and global trigger list; and `functionsHaveFixedSearchPathAndNoTransactionControl` service/guard lists, transaction-control query, qualified UUID-generator assertion and “no IVR SQL service” assertion. Add the exact eight V006 functions (`guard_trial_policy_version`, `guard_entitlement_grant`, `guard_trial_grant`, `guard_entitlement_balance`, `guard_entitlement_issuance`, `guard_usage_reservation`, `reject_row_mutation`, `issue_first_trial`) and seven triggers; assert `issue_first_trial` is the only V006 service function and that each trigger's `tgfoid` matches the Option A table in §4.7.
4. Exhaustively evolve the existing V003/V004/V005 absence and ACL sites cited by the review: `assertRiskOpenClearReopenRevisionProtocol` changes its schema-level zero count to exactly seven `entitlement_metering` tables and zero `activation_consent` tables while preserving every risk assertion; `assertV004SchemaUsageMatrix` and `assertV005SchemaUsageMatrix` flip only `catalog_order_role`→`entitlement_metering` and `entitlement_metering_role`→`subject_number` to true; `assertV004GuardFunctionExecuteBoundaries` flips the owner platform capability and full-prefix `issue_first_trial` observations to V006 presence while the staged V005 capture preserves the historical negatives. Platform/outbox expectations retain the accepted V004 positives for `catalog_order_role`, `ivr_role`, `entitlement_metering_role` and `activation_consent_role`; the new owner capability is false for the other S2 owner roles and `PUBLIC`, not for those runtime roles.
5. Exhaustively evolve late-FK and decision fixtures. In `assertV005ForeignKeyAndDeferredReservationFkBackstops`, keep `V005_FK_DEFS` as the exact original 15 by querying/validating those 15 names, then separately assert the sixteenth `fk_ivr_decision_reservation` definition/properties and nullable child column; replace the reservation-FK zero count and entitlement-table zero count with exact presence counts. In `assertV005ErrataCheckRegressionProbes` legal `DIRECT_AUTHORIZATION`, and in `assertV005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops` legal `DIAL_TASK_CALLEE` and legal `DIRECT_AUTHORIZATION`, replace random orphan reservation UUIDs with method-local real `usage_reservation` parents created through exact subject → trial policy/grant/balance → call → reservation fixtures. Preserve every existing legal-shape, CHECK, unique, append-only and source-identity assertion; replace only the obsolete no-FK statements with exact late-FK presence/parent linkage. Negative missing-parent evidence moves to the V006 FK method and must fail `23503 fk_ivr_decision_reservation`.
6. Update cumulative schema/table/ACL helper universes and comments wherever they enumerate all current objects: seven V006 table names/owners, eight function names/signatures/configs, seven triggers, 16 V006 FK decisions, 29 V006 indexes, V006 schema usage, exact table/column grants, platform owner capability and PUBLIC-zero queries. Keep every V001-V005 constant/list and behavior assertion; extend or filter queries rather than broadening them to “contains” checks. The only historical negatives are the staged target-005 evidence captured before V006.
7. Add `V006_TABLES`, `V006_CHECK_DEF_MAP` (50), `V006_PK_UNIQUE_DEFS` (12), `V006_FK_DEFS` (16), `V006_INDEX_DEFS` (29) and helpers `assertV006TablesColumnsChecksIndexesFks`, `assertV006CheckPredicateInventory`, `assertV006FunctionsAndTriggersExact`, `assertV006NoIdDefaultsOrSequences`, `assertV006SchemaUsageMatrix`, `assertV006OwnedAndCrossModuleGrants`, `assertV006PublicAclIsZero`, `assertV006GuardFunctionExecuteBoundaries`, `assertNoV007PlusObjects`, plus bounded fixture helpers for a real trial grant/balance/reservation parent and a staged-V005 evidence record. Helpers accept method-local ids and never delete or rewrite another method's rows.
8. Add exactly three methods:
   - `v006EntitlementReservationUsageInventoryAndGuardShapesAreExact`: exact seven tables/owners; 84 ordered column rows with types/nullability/defaults; 50 checks with exact names and whole normalized `pg_get_constraintdef`; 7 PKs, 9 ordinary uniques and 3 partial unique indexes (12 keys); 29 exact `indexdef`s; 16 FK defs/properties (`rrs|false|false|true`); late `fk_ivr_decision_reservation` present and last; `source_ref_id` non-FK; 19 defaults; zero id defaults/sequences; seven functions + seven triggers with exact tgtype matrix; exact schema/table owners; `assertNoV007PlusObjects`.
   - `v006OwnerPublicRuntimeAndCrossModuleAclsAreExact`: exact schema `USAGE`/`CREATE`; exact owned-table `SELECT`/`INSERT`/column `UPDATE`; exact cross-module column `SELECT` sets; exact function `EXECUTE` (`issue_first_trial` to two roles; six `guard_*` to `entitlement_metering_role`; `reject_row_mutation` no runtime grant); F-02 owner platform capability isolated; PUBLIC zero; every undeclared privilege false; runtime `42501` denials for undeclared `UPDATE`/`DELETE`/`TRUNCATE` and cross-module writes/`FOR UPDATE`, plus legal column `UPDATE` positive controls, all savepoint-bounded and rolled back.
   - `v006IssueFirstTrialGrantReplayConflictAndPolicySemantics`: `GRANTED` inserts exactly one trial/grant/balance/Outbox and returns the captured `trial_grant.granted_at`; same action + same service-computed fingerprint returns original ids, persisted `granted_at` and `REPLAY`; same action + changed fingerprint raises exact `P0001/TRIAL_ACTION_CONFLICT`; different action returns original ids, persisted `granted_at` and `ALREADY_GRANTED`; missing/disabled/unpublished/stale expected policy raises exact `P0001/TRIAL_POLICY_NOT_FOUND` and creates nothing. Probe null, unsupported, malformed and whitespace-only arguments with exact `P0001/TRIAL_ARGUMENT_INVALID`, and non-null non-exact/padded/whitespace campaign with exact `P0001/TRIAL_CAMPAIGN_INVALID`. Probe every §6.2 `TRIAL_DATA_INTEGRITY` class that can be constructed transactionally (missing balance; grant subject/source/ref/type/units/time mismatch; balance units mismatch; source identity bound to another subject), using a savepoint and transactional constraint/fixture manipulation only where an accepted FK otherwise makes an impossible corrupt row; rollback restores the catalog. Replay/already-granted perform no new policy selection. Assert nested Outbox identity `ACTIVATION | trial_grant:<id> | TRIAL_GRANT/<id> | FIRST_TRIAL_GRANTED`, exact payload fields/values, independent Java canonical hash oracle, `valid_from`/`expires_at` calculation, and zero rows after rollback. Label all reserve/settle/issue/publish orchestration as deferred.
9. Update the CI expected set to exactly 41 names and V006 labels/error text ("V006 S1 test count (41)"). The expected set, the size label and the name list must be edited together (same coupling as PA5).
10. Update README to V006 S1: seven tables/84 columns/50 checks/12 unique keys/29 indexes/16 FK decisions/8 functions/7 triggers/19 defaults; `issue_first_trial` present; no V007+; 41 methods with per-method inventory bullets in the existing prose style; V001-V005 hashes preserved.

**Invariants.** V001-V005 blobs and CR-normalized diffs unchanged; V006 is the only new object source; no V007+ object; no SQL service function besides `issue_first_trial`; no application Java/POM/runtime wiring; all prior 38 methods remain behaviorally identical except the explicitly renamed migration method and the enumerated cumulative assertions; any unexpected catalog mismatch is fixed only inside V006, and any need to change V001-V005, a frozen predicate or a grant outside §10.12/§11.4 stops the slice.

**Completion signal.** `./mvnw -B -ntp validate` and `clean verify` pass (no skip); `docker info`; `-Pcontainer-smoke verify` reports exactly 41 methods, `skipped=0 failures=0 errors=0`, exact set; V001-V005 hashes unchanged; V006 SHA-256 recorded; only allowed files changed.

**Stop condition.** Any need to edit V001-V005, the controller, application code or POM; add a V007+ object or a second service function; add a role/schema/column/check/index/grant outside the frozen contract; or weaken an assertion.

### 6.3 S2 - Executable lifecycle, guard, FK, nested-Outbox rollback, concurrency and cumulative evidence

**Prerequisites.** Accepted S1 checkpoint; recorded V006 SHA-256; all 41 methods passing; V006 frozen.

**Allowed files.** `PostgresContainerIT.java` (S2 methods and bounded helpers only); `.github/workflows/ci.yml` (expected set 45); `backend/README.md` (final 45); the S2 implementation artifact (new). V006 migration is unchanged unless a controller-approved bounded correction is required by a failing frozen-contract assertion.

**Exact test changes (S2).** Add exactly four methods:

1. `v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops` — trial_policy publication/disable shapes (`uq_trial_policy_enabled` partial unique, guard admits the one publication UPDATE and rejects later content/publisher changes); entitlement_grant/trial_grant/usage_fact append-only `P0001` as owner and `42501` as runtime; balance invariant `23514 ck_entitlement_balance_units` / `ck_entitlement_balance_status_shape`; issuance state shape and terminal `ISSUED`; all 7+8+5+5+10 checks probed with `23514` + exact name where applicable to this method's tables; rollback.
2. `v006ReservationUsageForeignKeyAndLateFkBackstops` — reservation state shapes (`ck_usage_reservation_state_shape`, `ck_usage_reservation_settled` with no tolerance); usage_fact unique reservation/type and source/event; missing-parent `23503` + exact name for all 16 FKs including late `fk_ivr_decision_reservation`; representative parent-delete RESTRICT `23503`; late-FK timing evidence (the constraint exists after V006 and is not deferrable); `source_ref_id` non-FK; all 9+6 checks probed; rollback.
3. `v006NestedOutboxAtomicityAndReplayRollbackEvidence` — use the exact transaction-local privilege-revoke mechanism in §6.2 to force nested `platform.outbox_append` failure and assert native `42501`, zero partial rows, rollback restoration of the owner grant and subsequent normal success. A separate whole-transaction failure after trial writes rolls back everything. For committed observation, commit one fresh method-local `GRANTED` in a dedicated transaction, observe from a new connection exactly one trial/grant/balance/Outbox with the frozen ids/times/hash, prove replay/already-granted add nothing, then delete only those method-local rows in the shared FK-safe cleanup. `TRIAL_DATA_INTEGRITY` uses the exact predicates in §6.2. No second trial/grant/balance for any replay/retry/policy-version/channel path; reserve/settle/issue/publish remain labeled deferred.
4. `concurrentV006FirstTrialAndReservationIdentityHasOneWinner` — bounded two-connection races with distinct `application_name`, `pg_stat_activity` lock-wait detection, `statement_timeout` and future timeout, no sleep-only assertion: (a) two concurrent `issue_first_trial` calls for one subject → one `GRANTED`, the other `REPLAY`/`ALREADY_GRANTED`/`TRIAL_ACTION_CONFLICT` per identity, never two trials; (b) two inserts of `usage_reservation` for one `call_id` → one winner, loser `23505 uq_usage_reservation_call`; (c) two inserts of `trial_grant` for one `subject_id` → one winner, loser `23505 uq_trial_grant_subject`; (d) two inserts of `usage_fact` for one `(reservation_id, usage_type)` or one `(source_event_domain, source_event_id)` → one winner each; (e) category-2 savepoint: failed reservation/issuance insert rolls back to savepoint and preserves the committed trial/grant/balance; (f) category-3 whole-transaction failure after trial writes leaves no partial rows. Cleanup in `finally` uses only the explicit method-local FK-safe `DELETE` strategy in §7.3; no `TRUNCATE`, no `CASCADE`, no unscoped delete.

**Invariants.** V006 unchanged; only the test class, CI, README and S2 artifact change; every expected failure uses a savepoint/separate connection and asserts exact SQLSTATE + constraint/exception name; valid paths are demonstrated alongside rejections; no append-only row survives cleanup; no runtime `DELETE`/`TRUNCATE`; reserve/settle/issue/publish/select orchestration remains deferred and labeled.

**Completion signal.** Full container profile exactly 45 methods, `skipped=0 failures=0 errors=0`, exact set; focused methods may be used during iteration and the concurrency method may be repeated boundedly, but each accepted implementation checkpoint ends with exactly one final full Testcontainers profile; default verify 0-skip; boot smoke; fat-jar firewall; V001-V005 hashes unchanged; V006 hash unchanged; cleanup/whitespace/status checks pass. No unbounded retry loop or duplicate “final” full run is required.

**Stop condition.** Any need to edit V006/V001-V005, the controller, application code or POM; any new schema/function/trigger/grant/service; any append-only cleanup that weakens a runtime denial; any unexplained mismatch requiring a relaxed assertion.

## 7. Test matrix and expected totals

### 7.1 Method evolution

Current 38 → S1 41 → S2 45. No placeholder, `@Disabled`, `Assumptions`, environment skip or weakened method. CI asserts exactly the method set present at each checkpoint.

| Slice | Count | Exact new/renamed methods | Preserved |
| --- | ---: | --- | --- |
| S1 | 41 | `v001ThroughV006MigrateCleanPostgres16WithoutSkipping` (renamed), `v006EntitlementReservationUsageInventoryAndGuardShapesAreExact`, `v006OwnerPublicRuntimeAndCrossModuleAclsAreExact`, `v006IssueFirstTrialGrantReplayConflictAndPolicySemantics` | All other 37 current methods |
| S2 | 45 | `v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops`, `v006ReservationUsageForeignKeyAndLateFkBackstops`, `v006NestedOutboxAtomicityAndReplayRollbackEvidence`, `concurrentV006FirstTrialAndReservationIdentityHasOneWinner` | All S1 41 methods |

The full final 45-name set (S1 uses the first 41; S2 uses all 45) is exactly:

```text
v001ThroughV006MigrateCleanPostgres16WithoutSkipping
v001ThroughV003InventoryMatchesTheFrozenContract
brokenMigrationFailsVisibly
idempotencyNewReplayConflictAndFinalizeOnce
committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow
concurrentIdempotencyRetryObservesOneWinner
outboxReplayConflictRollbackAndDenylist
auditIsAppendOnlyAndRuntimeWritesAreDenied
auditReasonAndDenylistRulesAreEnforced
functionsHaveFixedSearchPathAndNoTransactionControl
ownerPublicAndRoleExecutionBoundariesAreExact
hostileSearchPathCannotRedirectFunctionEffects
mutableHeadsEnforceLifecycleRevisionAndNamedChecks
localImmutabilityGuardsRejectRelationAndRotationChanges
identityCollisionAndMembershipRevokeRegrantSemantics
concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner
runtimeAndCrossModulePrivilegesAreExactAndWritesDenied
lastEnabledAdminTransactionIsSerializedAndFailsClosed
v003SubjectNumberCatalogAndGuardsAreExact
v003OwnerPublicAndRuntimeAclsAreExact
subjectOwnerSetOnceAndRealNameSeparation
bindingLifecycleTerminalityAndActiveNumberUniqueness
appendOnlyClaimAndForeignKeyBackstops
riskOpenClearReopenRevisionProtocol
concurrentActiveBindingUniquenessHasOneWinner
v004CatalogOrderInventoryAndGuardsAreExact
v004OwnerPublicRuntimeAndCrossModuleAclsAreExact
v004CatalogVersionLifecycleAndGuardBackstops
v004OrderPaymentLifecycleAndAppendOnlyBackstops
v004ForeignKeyAndAbsentConsentFkBackstops
concurrentCatalogPublicationAndCommerceIdentityHasOneWinner
v005IvrTaskSessionCallInventoryAndGuardShapesAreExact
v005OwnerPublicRuntimeAndCrossModuleAclsAreExact
v005TaskTtlLifecycleAndGuardBackstops
v005SessionCallCanonicalCallerAndReciprocalLinkBackstops
v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops
v005ForeignKeyAndDeferredReservationFkBackstops
concurrentV005TaskClaimAndActiveCallHasOneWinner
v006EntitlementReservationUsageInventoryAndGuardShapesAreExact
v006OwnerPublicRuntimeAndCrossModuleAclsAreExact
v006IssueFirstTrialGrantReplayConflictAndPolicySemantics
v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops
v006ReservationUsageForeignKeyAndLateFkBackstops
v006NestedOutboxAtomicityAndReplayRollbackEvidence
concurrentV006FirstTrialAndReservationIdentityHasOneWinner
```

The CI expected-set edit must rename the migration method and add exactly the seven new names (three at S1, four at S2), updating the label/count; no other workflow behavior changes.

### 7.2 Probe distribution

| Evidence | Method | Count |
| --- | --- | ---: |
| Inventory/guard shapes | `v006EntitlementReservationUsageInventoryAndGuardShapesAreExact` | 7 tables, 84 columns, 50 checks, 12 keys, 29 indexes, 16 FKs, 8 functions, 7 triggers, 19 defaults, 0 id defaults, 0 sequences |
| ACL | `v006OwnerPublicRuntimeAndCrossModuleAclsAreExact` | schema/table/column/function/PUBLIC matrix + denials |
| `issue_first_trial` outcomes | `v006IssueFirstTrialGrantReplayConflictAndPolicySemantics` | GRANTED/REPLAY/ALREADY_GRANTED/5 errors/Outbox/rollback |
| Trial/grant/balance/issuance checks | `v006TrialBalanceIssuanceLifecycleAndAppendOnlyBackstops` | 7+8+5+5+10 = 35 |
| Reservation/usage checks + FKs | `v006ReservationUsageForeignKeyAndLateFkBackstops` | 9+6 = 15 checks; 16 FKs |
| Nested Outbox / replay rollback | `v006NestedOutboxAtomicityAndReplayRollbackEvidence` | rollback + no-second-row matrix |
| Concurrency | `concurrentV006FirstTrialAndReservationIdentityHasOneWinner` | 4 races + 2 rollback scenarios |
| **Total named checks** | | **50** |

### 7.3 Cleanup and determinism

- Rollback-only behavior probes use a transaction/savepoint and leave no committed fixture. The committed-`GRANTED` probe and concurrency races use fresh method-local UUIDs/keys, commit only when cross-connection observation is the assertion, and always enter a `finally` cleanup even after assertion failure.
- Cleanup reconnects as the migration identity, starts one cleanup transaction, and deletes only rows whose exact ids/keys were allocated by that method. Because four touched tables are append-only by trigger, transactionally disable only `ivr.trg_ivr_decision_immutable`, `entitlement_metering.trg_usage_fact_immutable`, `entitlement_metering.trg_trial_grant_immutable` and `entitlement_metering.trg_entitlement_grant_immutable`; never use `session_replication_role`, never disable an internal FK trigger, and never disable any trigger globally. Required dependency order is: external `ivr.ivr_decision` rows that reference method-local reservations first; method-local `platform.outbox_event` rows by exact `ACTIVATION` dedupe key; `entitlement_metering.usage_fact`; `usage_reservation`; `entitlement_issuance`; `entitlement_balance`; `trial_grant`; `entitlement_grant`; `trial_policy_version`; then any touched V005 children/parents (`ivr.call_event`, `ivr.call_leg`, `ivr.call_record`, `ivr.inbound_session`, `ivr.outbound_call_task`) and touched V004/V003/V002 parents in reverse dependency order (payment/ordering rows if created; subject claim/risk/binding then `business_subject`; organization membership then organization; identity/profile then customer account; employee publisher last). Re-enable all four named triggers and assert `tgenabled = 'O'` before commit; any cleanup error rolls back both deletes and trigger-state changes. If a fixture does not create a listed table, no delete is issued for it. Cyclic V005 task/call fixtures are either not used or have their method-local nullable link cleared before deleting both parents.
- Every `DELETE` has a method-local `WHERE` predicate over recorded ids/keys; there is no `CASCADE`, no table-wide delete, no cross-method data deletion and no seven-table `TRUNCATE`. After cleanup, query every touched table and assert zero rows for every recorded method-local id/key, including `platform.outbox_event` dedupe keys and external `ivr_decision` ids. A cleanup failure is a test failure and is not hidden by the primary assertion.
- The ACL method separately retains executable runtime-role denial evidence for both `DELETE` and `TRUNCATE` (`42501`) on V006 tables. Replacing fixture cleanup does not weaken that security assertion.
- Time assertions use explicit `timestamptz` values and `clock_timestamp()`; no sleep-only timing. Concurrency uses two real connections, distinct `application_name`, `pg_stat_activity` wait-event detection, bounded `statement_timeout` and future timeouts. Every expected failure asserts exact SQLSTATE and exact constraint/exception name.
- Shared-container isolation: every assertion is scoped to method-local UUID/key/hash values; no global count depends on rows from other methods.
- V001-V005 immutability evidence: recompute the §2.2 working-tree SHA-256 values for V001-V005 and assert they match; assert V006 hash is recorded and unchanged across S2.

### 7.4 Commands and expected results (bounded, no unbounded awk/grep/sort pipelines)

```bash
set -euo pipefail
cd backend
./mvnw -B -ntp validate                 # Enforcer/toolchain, exit 0
./mvnw -B -ntp clean verify             # unit/module tests, 0 skipped/failures/errors
docker info                             # Docker available
./mvnw -B -ntp -Pcontainer-smoke verify # exactly one final full run/checkpoint: S1 41 or S2 45
timeout 90s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1 &
boot_pid=$!
for attempt in $(seq 1 45); do
  if grep -Fq 'Started OneCallApplication' target/boot.log; then break; fi
  if ! kill -0 "$boot_pid" 2>/dev/null; then wait "$boot_pid"; exit 1; fi
  sleep 2
done
grep -Fq 'Started OneCallApplication' target/boot.log
kill "$boot_pid" 2>/dev/null || true
wait "$boot_pid" || test "$?" -eq 143
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
# forbidden runtime dependency check uses the CI firewall command, not a pipeline
sha256sum ../backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql
sha256sum ../backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql
sha256sum ../backend/src/main/resources/db/migration/V003__subject_number_and_claim.sql
sha256sum ../backend/src/main/resources/db/migration/V004__catalog_order_and_payment.sql
sha256sum ../backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql
sha256sum ../backend/src/main/resources/db/migration/V006__trial_entitlement_issuance_reservation_usage.sql
```

Expected Failsafe XML: one `TEST-*PostgresContainerIT.xml`; `tests == expected size`; `skipped=0`; `failures=0`; `errors=0`; `cases == expected`; every surefire report `skipped=0`. The CI inline Python assertion is the source=CI=XML equality check and must be updated to the 41/45 name sets.

Validation is fail-closed: every shell group starts with `set -euo pipefail`; any bounded polling loop has a fixed attempt count and a final positive assertion; no `tee`, `grep`, subshell or background-process status may mask the command being validated. Focused methods may run during iteration, but one and only one final full Testcontainers run closes each accepted S1/S2 checkpoint.

Plan-gate validation of this artifact (bounded):

```bash
git diff --check -- docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan.md
git diff --check -- docs/14-production-data-model-and-flyway-schema-spec.md
git diff --check -- docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan-fix-1.md
test -s docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan.md
test -s docs/gateflow/pa6-v006-entitlement-reservation-usage-foundation-plan-fix-1.md
```

## 8. Documentation decision

- Update `backend/README.md` in S1 and S2 with the V006 inventory/behavior, 41/45 profiles, V001-V005 hashes and the "no V007+" boundary. Keep toolchain pins, module table, firewall and evidence-boundary sections.
- Do not modify `docs/12`. The only docs/14 changes are the user-authorized §4.30 and §4.31 trigger-function sentence corrections recorded by this fix; §4.35 and every unrelated design sentence remain unchanged. Do not add a new design document.
- Required Gateflow artifacts: S1 and S2 implementation/evidence reports, controller gate records and the plan-review record (controller-owned). No implementation author may write the control document.
- The README and CI are evidence descriptions, not production acceptance. Container/CI success is not A5 `PASS` or production acceptance.

## 9. Risks, open inputs, owners and destinations

| ID | Item | Status / impact | Owner / destination |
| --- | --- | --- | --- |
| F-PA6-PLAN-01 | Conflicting §4.30/§4.31 control sentences versus §10.16/§11.4. | **Resolved by user-authorized Option A.** The two sentences are corrected to `guard_entitlement_grant()` / `guard_trial_grant()`; §4.35 stays `reject_row_mutation()`. Exact result: 1 service + 7 trigger functions, 7 triggers, ACL/test matrix in §4.6-§4.7. No residual naming risk. | Plan-fix artifact; implementation must follow the exact inventory. |
| Q8 / F-10 | Settlement tolerance phrase "plus approved tolerance" vs no approved tolerance value. | Deferred, not guessed. Normative baseline is exactly `ck_usage_reservation_settled`; no tolerance field/clamp is added. | Product/Finance/Entitlement + IVR/Backend later owners; later wave. |
| Q5/Q25 / F-04 | Allocation policy, per-call cap, authorization window inputs; `SOLE_ELIGIBLE` selector. | Deferred. V006 creates only the balance/reservation shapes. No allocation policy is invented. | Product/Finance/Entitlement + IVR/Backend; later wave. |
| Q4/Q10/Q20 | Trial validity beyond the approved positive `valid_days` calculation. | Deferred. `expires_at = granted_at + valid_days * interval '1 day'` UTC only. | Entitlement owner; later wave. |
| Q18 | Production TTL configuration key. | Deferred; not stored in `trial_policy_version`. | Platform/configuration owner; later wave. |
| Q17/Q23 | Runtime role provisioning/authentication and connection role switching. | Deferred. Testcontainers superuser is test-only; same-connection owner-service contract unchanged. | Platform/Database/Security; later wave. |
| Q15/Q26 | Wire encoding, IVR JSON-schema version, fingerprint construction beyond the §10.17 registry. | Deferred. V006 stores columns and calls `platform.outbox_append`; no serializer framework. | IVR/API owner; later wave. |
| OQ-5 | Publisher fact on `trial_policy_version`; no same-employee-only rule invented. | Recorded; publication UPDATE sets the actual authenticated publisher as specified. | Operations/entitlement service wave. |
| OQ-6 | Policy-selection linearization point. | Specified as the function-internal sole authoritative read (§4.2 S1 contract). | Implementation author; S1 tests. |
| — | Reserve/settle/issue/publish/select application services; `Propagation.MANDATORY` wrappers. | Service-owned; V006 proves only DB backstops. | Later PA waves. |
| — | `source_ref_id` polymorphic non-FK. | Intentional; not a catalog `NO_FK` decision. Tests assert no FK on that column. | Implementation author; S1 inventory method. |
| — | `trial_grant` INSERT is function-only (`issue_first_trial`); `entitlement_metering_role` has no INSERT. | Frozen; tests assert `42501` on direct runtime INSERT and success only through the function. | Implementation author; S1 ACL + function tests. |

No blocking open question is known for planning after the authorized adjudication. All other design inputs are resolved or explicitly deferred with named owners; no contradiction is relabeled as a naming note.

## 10. No-goal-drift and no-over-design analysis

- No V007-V009 object; no consent/activation, evidence/media/sharing table/function; no second SQL service function.
- No reserve/settle/issue/publish/select application service; no API, Gateway, controller, JPA, bridge, upload, settlement, reconciliation, frontend, cloud or runtime DataSource/Flyway wiring.
- No new role/schema; no schema `CREATE`; no table privilege implied by schema `USAGE`; no grant outside §10.12/§11.4; no V007 table grant.
- No extra column, table, check, index, trigger, enum value, registry, provider field, soft-delete, retention policy, queue framework, workflow engine, state-machine SQL or speculative configuration key.
- The only frozen-design correction is the user-authorized two-sentence docs/14 erratum for F-PA6-PLAN-01; §4.35 and all unrelated design text remain unchanged.
- No weakening of existing 38 methods: the only changes are the exhaustively enumerated staged-prefix, presence/global-list/schema/ACL/fixture evolutions and added methods; every prior behavior assertion remains.
- No service-owned behavior is claimed as database-enforced: allocation selection, settlement reconciliation, issuance retry, policy publication orchestration and IVR decision selection remain deferred and are labeled as such in tests.
- Slice count is two, matching the accepted PA5 cost model; a third slice would be a mechanical split of one Flyway file.

## 11. Completion report format

The implementation-gate completion report must contain:

1. Runtime/provider/task: Java/Maven/Docker/PostgreSQL versions; provider and task ID; runner exit/JSON status and stderr.
2. Artifact path and SHA-256 for the V006 migration, test class, CI, README and each Gateflow implementation artifact.
3. Chosen slice count and rationale (two slices, as §6.1).
4. Exact inventory totals: 7 tables, 84 columns, 50 checks, 7 PKs, 9 ordinary uniques, 3 partial unique indexes, 12 unique keys, 10 secondary indexes, 29 index objects, 15 immediate FKs, 1 late `ALTER_FK`, 0 new NO_FK, 8 functions (1 service + 7 triggers), 7 triggers, 19 defaults, 0 id defaults, 0 sequences; method totals 38→41→45.
5. Validation performed: exact commands, Surefire/Failsafe counts, exact method-set comparison, V001-V005 hashes, V006 hash, boot/jar checks, cleanup/status, and any command not run.
6. Risks/open inputs: record F-PA6-PLAN-01 as resolved Option A (not residual), deferred Q items (Q4/Q5/Q8/Q10/Q15/Q17/Q18/Q20/Q21/Q23/Q25/Q26/Q28), OQ-5/OQ-6 handling and residual implementation risks.
7. Permission denials/blockers reported separately; do not fabricate executable validation.

## 12. Plan validation performed by this author

- Verified workspace, branch `codex/pa6-v006-entitlement-reservation-usage-foundation`, HEAD `aa2b0925e6b8ffdac4587f2e44c4c0dde393d649`, preserved `GIT_DIR`/`GIT_WORK_TREE`, and the task-start dirty baseline (pre-existing tracked `project-execution-control.md`, the untracked plan/review/helpers and historical `.pa5-*`/`.pa6-*` files).
- Read the failed review and the real cited sources rather than trusting review prose: the PA6 control/slice contract; docs/12 sections 5/6/14/15 Phase B; docs/14 §2.5, §2.9, §4.29-§4.35, §5, §9.0.1, §9.11-§9.16, §9.21-§9.26, §10.9, §10.11-§10.20 and §11.4; V001 custom-error/Outbox precedent; V004 owner-grant blocks; and the cited `PostgresContainerIT` function lists, zero-table/function assertions, V004/V005 ACL matrices, risk method, three legal reservation-id fixtures and V005 FK method.
- Mechanically derived the V006 inventory from the seven table dictionaries and cross-checked FK/key/grant/trigger/creation-order catalogs: 84 columns (11/13/10/8/14/16/12), 50 named checks (7/8/5/5/10/9/6), 7 PKs, 12 unique keys (3 partial), 10 secondary indexes, 29 index objects, 15 immediate FKs + 1 late `ALTER_FK`, 19 defaults, 1 service function, 7 trigger functions, 7 triggers, 0 id defaults, 0 sequences.
- Confirmed working-tree SHA-256 of V001-V005; V005 equals the frozen PA6 contract hash `2F4CDEF48FD5B1CEC9B5F4479E99DF3359073C94BF2C45CBC5B2CF8E87BA9310`.
- Confirmed current `PostgresContainerIT` has 38 real `@Test` methods and exhaustively located the V006-sensitive sites enumerated in §6.2, including the global helper lists and all three random orphan reservation fixtures.
- Confirmed CI expected-set label "V005 S2 test count (38)" and the 38-name list.
- Applied exactly the two authorized docs/14 sentence corrections; path-scoped diff is 2 insertions/2 deletions and §4.35 remains byte-content unchanged.
- Ran targeted `git diff --check` for all three allowed paths; because the plan and fix artifact are untracked, also ran `git -c core.autocrlf=false diff --no-index --check /dev/null <path>` for each and received the expected difference exit `1` with zero check output. Bounded content checks passed for the errata, exact 1+7 inventory, owner blocks, staged target-005 proof, stale naming-note removal, real reservation parents, cleanup order and balanced Markdown fences.
- Verified tracked diff names are exactly the pre-existing `docs/gateflow/project-execution-control.md` plus this task's allowed `docs/14-production-data-model-and-flyway-schema-spec.md`; allowed-path status is docs/14 modified plus the revised plan and new fix artifact untracked. No other tracked path changed during this task.
- Did **not** run Maven, Docker, Flyway or Testcontainers in this documentation-only fix gate. No executable V006 catalog claim is made here; S1 must revalidate against PostgreSQL 16.
- No file was removed, staged or committed; no push, PR mutation, merge or agent dispatch was performed.

## 13. Resolved blocker summary for the plan gate

**All four accepted review findings are addressed in this fix plan.** F-PA6-PLAN-01 is resolved by the user-authorized Option A and the two bounded docs/14 errata; DDL order/grant ownership is executable; cumulative V003/V004/V005 assertion and fixture evolution is exhaustive with live target-005 negative proof; and cleanup is method-local FK-safe `DELETE` with no `TRUNCATE`/`CASCADE`. All other design inputs are resolved, explicitly deferred with named owners (Q4/Q5/Q8/Q10/Q15/Q17/Q18/Q20/Q21/Q23/Q25/Q26/Q28, OQ-5), or retained as later service-owned behavior. This statement completes only the `fix` artifact; it does not enter re-review or implementation.
