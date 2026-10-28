# PA5 V005 IVR Task, Session and Call Foundation - Implementation Plan

- Task label: `pa5-v005-plan-ds-20261028-01`
- Work unit: `PA5-v005-ivr-task-session-call-foundation`
- Gate: `plan`
- Author: DeepSeek (`deepseek-flash`), plan only; no implementation, commit, stage, push, PR, merge, review or agent dispatch
- Branch: `codex/pa5-v005-ivr-task-session-call-foundation`
- Base/head: `4006e8e09c39e4527cb543f03646a30073d1e1fa`
- Windows worktree: `C:\Users\Administrator\.codex\worktrees\pa4-v004-catalog-order-payment\codex-call`
- Preserved Git environment: `GIT_DIR=/mnt/d/codex-call/.git/worktrees/codex-call4`, `GIT_WORK_TREE=/mnt/c/Users/Administrator/.codex/worktrees/pa4-v004-catalog-order-payment/codex-call`
- Binding inputs: the user-confirmed PA5 goal contract, slice principle and gate record in `docs/gateflow/project-execution-control.md`; `docs/12-technology-stack-and-system-architecture.md` sections 5, 6, 14 and 15 Phase B; `docs/14-production-data-model-and-flyway-schema-spec.md` sections 2.9, 4.23-4.28, 5.4, 9.0.1, 9.11-9.16, 9.21-9.24, 10.12-10.17 and 11.4; accepted V001-V004 migrations and the current 31-method `PostgresContainerIT`; `.github/workflows/ci.yml`; `backend/README.md`.
- Plan fix 1: task `pa5-v005-planfix-ds-20261028-01`, recording the user-authorized option-B errata that adds `ivr.call_record.created_at`; bounded record in `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan-fix-1.md`.

This plan implements exactly `V005__ivr_task_session_call.sql` and database-visible evidence for the six frozen `ivr` tables. It does not implement IVR resolve, task-claim, direct-DTMF, confirmation, call-event, reservation, settlement, activation/consent, evidence, API, JPA, runtime wiring or any V006+ object. The controller contract is the binding scope; this document adds no goal, acceptance criterion, hardening wave, application service or future-slice work.

## 1. Goal, success signals, scope and non-goals

### 1.1 Goal

Create one forward-only Flyway migration, `V005__ivr_task_session_call.sql`, so later IVR service, entitlement/reservation, activation/consent and evidence waves have stable outbound-task, inbound-session, canonical-call, call-leg, call-event and frozen-decision parents. V004 already created the `ivr` schema, `ivr_owner`/`ivr_role`, the other S2 schemas/roles and the S2 runtime's platform/audit grants. V005 must not recreate or alter those roles or schemas incompatibly.

V005 creates exactly six `ivr` tables: `outbound_call_task`, `inbound_session`, `call_record`, `call_leg`, `call_event`, `ivr_decision`. `inbound_session.call_id` and `call_record.call_id` are intentional root `NO_FK` identities. `outbound_call_task.claimed_call_id` is a nullable column whose ordinary immediate FK is added by same-wave `ALTER` after `call_record` exists. `ivr_decision.reservation_id` exists as a nullable column in V005 but receives no FK until V006. `issue_first_trial` remains absent. No V005 SQL service function is created.

### 1.2 Frozen success signals

- Flyway applies V001 through V005 to a clean PostgreSQL 16 database. V001-V004 remain byte-identical.
- Exactly six `ivr` tables; only those six tables exist in `ivr` after V005; `entitlement_metering` and `activation_consent` contain zero tables; `evidence`/`sharing` remain absent.
- Exact V005 inventory: 103 columns; 73 named checks; six PKs; 14 unique keys (8 ordinary unique constraints + 6 partial unique indexes); 31 total indexes; 15 ordinary immediate RESTRICT FK constraints (14 created in the V005 FK block and the same-wave late `fk_outbound_task_claimed_call`); two `NO_FK` root identities; one V006-deferred FK decision with a V005 column only; five `SECURITY INVOKER` trigger functions; five triggers; 17 defaulted columns; zero `id` defaults; zero sequences.
- Exact V005 owner/runtime/cross-module/PUBLIC ACL. `ivr_role` receives `USAGE` without `CREATE` on `ivr`, `customer_iam`, `organization_access` and `subject_number`; owns `SELECT`/`INSERT` and only the exact column `UPDATE` sets on its six tables; receives exact cross-module column `SELECT` on six S1 tables; receives `EXECUTE` only on the four `guard_*` functions; `PUBLIC` has zero privileges; every undeclared grant is denied.
- No V006+ table, no `issue_first_trial`, no `ivr.select_call_action`, no `ivr.record_activation_next_action`, no `fk_ivr_decision_reservation`, no evidence/media/sharing object, no application service or runtime DataSource/Flyway wiring.
- Default verification, the PostgreSQL 16 container profile and boot/fat-jar checks pass with no skip, failure or error; source/CI/Failsafe method sets are exact.
- Executable PostgreSQL evidence proves: default 30-second task TTL frozen per row and not refreshed by duplicate/replay (database-visible portion); one live task and one active/unknown call per canonical caller; root/canonical `callId` semantics; exact reciprocal task/call linkage; lifecycle/revision/terminal shapes; frozen parties and event identity; append-only decisions; all named checks, FKs and ACLs; bounded representative concurrency and failure rollback.
- `ENTER_CALLEE` and direct-DTMF/`DIRECT_AUTHORIZATION` are proven only as frozen decision/check shapes. They are not claimed as implemented IVR services.

### 1.3 Scope boundary

Allowed changes: the new V005 migration; focused cumulative additions to `PostgresContainerIT`; the minimum CI expected-set update; `backend/README.md` evidence; the required PA5 Gateflow artifacts/controller records (controller-owned, not written by the implementation author). Database tests may use bounded transient SQL and fixtures to prove documented backstops, but must label all IVR orchestration and later trial/evidence behavior as deferred.

### 1.4 Non-goals

- No V006-V009 migration or object: no trial policy, trial/entitlement grant, balance, issuance, usage reservation, usage fact, consent record, service activation/transition, media/recording/evidence/artifact/event or sharing table/function.
- No `issue_first_trial`; no `fk_ivr_decision_reservation`; no V006 decision-reservation FK.
- No `ivr.select_call_action`, `ivr.record_activation_next_action`, reservation, settlement, activation/consent, call-event, publication or reconciliation application service; no SQL service function of any kind in `ivr`.
- No API, Gateway, controller, JPA aggregate/repository, outbound bridge, recording/media/upload, settlement/reconciliation, payment, frontend, cloud/provisioning or runtime DataSource/Flyway wiring.
- Do not guess Q7/Q8/Q10/Q15/Q16/Q18/Q24/Q25/Q26/Q27/Q28 or create a production TTL configuration key. The 30-second value is the approved source default frozen per task; Q18 approval/configuration remains later.
- Do not add columns, tables, checks, indexes, triggers, grants, roles or schemas beyond the frozen dictionaries and §11.4 V005 order.

### 1.5 Goal-alignment mapping

| Confirmed goal area | V005 database evidence | Deferred behavior (not implemented) |
| --- | --- | --- |
| Outbound task identity and 30-s TTL | `outbound_call_task` columns/defaults/checks; `ttl_seconds` default 30; `expires_at > created_at`; immutable `ttl_seconds`/`expires_at`; unique idempotency tuple; one live task per canonical caller | Task create/replace/cancel (§9.12), replay-before-housekeeping, expiry housekeeping |
| Inbound session and canonical caller | `inbound_session` immutable origin identity; `call_id` unique root with no FK; trusted-caller/confirmation shape checks; caller HMAC index | Initial resolve, confirmation chain (§9.11/§9.13), `find_or_create_ivr_subject` |
| Canonical call and one active/unknown call | `call_record.call_id` unique root with no parent FK, referenced by leg/event/decision; one `ACTIVE`/`UNKNOWN` call per canonical caller; terminal/time/authoritative shapes | Resolve/direct authorization call binding, `CALL_IN_PROGRESS` rejection protocol, `UNKNOWN` reconciliation |
| Reciprocal task/call link | `claimed_call_id` unique partial index and same-wave immediate FK; guard admits only NULL→an existing call; call guard admits `task_id` only NULL→the task whose `claimed_call_id` equals this call; savepoint rollback clears both while the call survives | Full claim+reservation+decision atomic orchestration |
| Legs, events and frozen decisions | `call_leg` identity/state shapes; `call_event` domain/event/sequence identity, redacted/encrypted payload and processing shape; `ivr_decision` append-only, `(call_id,purpose)` and source-event identities, typed shape checks | `process_call_event`, settlement handoff, `select_call_action`, `record_activation_next_action` |
| Least privilege | Schema USAGE, owned column ACL, six cross-module reads, four guard EXECUTE grants, PUBLIC zero | Production role provisioning (Q17/Q23), owner role services |
| Later waves | Stable parents for V006 reservation/usage and V007 consent/activation; `reservation_id` column only | V006/V007 migration, service wiring |

## 2. First-principles judgment and current-code evidence

### 2.1 First-principles judgment

V005 is the smallest useful next persistence wave because V006 reservation/usage, V007 activation/consent and V008 evidence all require durable call/session/task identities, while the self-developed IVR integration needs one canonical `callId` and retained task/decision/event facts. Creating only database-owned identities, shapes and fail-closed backstops preserves module ownership and avoids coupling migration work to unresolved telephony, prompt, policy, wire and access inputs. Giving `ivr_role` only the frozen grants avoids inventing the runtime provisioning model (Q17/Q23) and avoids premature V006/V007 table privileges.

### 2.2 Direct current-code evidence

- `backend/src/main/resources/db/migration/` contains only V001, V002, V003 and V004. V004 creates the eight S2 roles (lines 10-17), four S2 schemas (19-22), PUBLIC revocation (24), platform/audit grants including `ivr_role` (26-46), and `GRANT USAGE` on `activation_consent` and `entitlement_metering` to `ivr_role` (485-493). V005 must reuse these and must not create roles/schemas.
- V003/V004 establish the repository pattern: owner-issued table/function/trigger/grant DDL under `SET LOCAL ROLE <owner>`, `RESET ROLE`, then ordinary immediate RESTRICT FKs added by the migration identity; `SECURITY INVOKER` guard functions with `SET search_path = <schema>, pg_temp`; exact catalog/ACL introspection. V005 reuses this pattern.
- `PostgresContainerIT` has exactly 31 `@Test` methods. The migration method is `v001ThroughV004MigrateCleanPostgres16WithoutSkipping` (line 641), asserts `migrationsExecuted=4` and four history rows, and calls the V001/V002/V003 smoke helpers. The global function list in `assertFunctionsAndTrigger` (line 5333) and the global trigger list (line 5454) currently contain the 20 V001-V004 functions and 18 V001-V004 triggers; both must add the five V005 functions/triggers or the cumulative V003 inventory method fails.
- `assertNoV005PlusObjects` (line 6147) asserts the V005 tables do not exist and is called by `v004CatalogOrderInventoryAndGuardsAreExact` (line 1623). After V005 this must become a V006+ absence helper called by the V005 inventory method; the V004 method must stop asserting V005 absence.
- `assertV004SchemaUsageMatrix` (line 6817) currently asserts `ivr_role` has no `USAGE` on `ivr` (line 6887) and lists `ivr_table` usage without `ivr_role`; `assertV004OwnedAndCrossModuleGrants` (line 6897) asserts every V004 role has no privileges on the 13 S1 tables. Both must evolve to the exact post-V005 grant set; the `ivr_role`→`catalog_order` assertion at line 6889 is mislabeled "deferred to V005": §10.12 never grants it, so it stays false with a corrected comment.
- `riskOpenClearReopenRevisionProtocol` contains a no-IVR-table block (around line 8248) asserting `ivr` has zero tables and the six V005 tables are null. After V005 it must assert the six tables exist, `entitlement_metering`/`activation_consent` have zero tables, and `evidence`/`sharing` remain absent.
- `.github/workflows/ci.yml` asserts the exact 31-name Failsafe set, `tests == len(expected)`, `skipped=0`, `failures=0`, `errors=0`, `cases == expected` (lines 73-123).
- `backend/README.md` states 31 methods and "no V005+" repeatedly (lines 1-3, 58, 64, 102, 111).
- Frozen blob-content SHA-256 (Git object content, LF): V001 `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`; V002 `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5`; V003 `1d3ffb5ffd60d1be5f85b93b87f52c727e7b43dad5929280123cc567ab873fb1`; V004 `a7ba7a8cbaf5eecaf04f117b53c1b4bfd31c31b04e9432984d6c108b6e496edc`. V005 must preserve all four.
- The only pre-existing worktree modification is the controller-owned `docs/gateflow/project-execution-control.md`; `.pa5-plan-launch.sh` and `.pa5-plan-prompt.md` are pre-existing untracked runner files. The implementation author must not touch any of them.

## 3. Exact affected paths and allowed changes

| Path | Slice | Change | Explicit boundary |
| --- | --- | --- | --- |
| `backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql` | S1 (authored once) | New complete V005 migration | Only six `ivr` tables, 103 columns, 73 checks, 14 unique keys, 31 indexes, 15 immediate FKs, five trigger functions, five triggers and exact grants; no schema/role creation, no service function, no V006+ |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | S1/S2 | S1: rename/evolve migration and absence assertions, update cumulative function/trigger/schema/ACL lists, add two inventory/ACL methods and helpers/constants. S2: add five behavior/concurrency methods and bounded helpers. | Same class; no new test class; preserve all 31 prior methods except the explicitly renamed migration method and the enumerated cumulative-assertion evolutions |
| `.github/workflows/ci.yml` | S1/S2 | Update only the Failsafe expected method set/count (31 -> 33 -> 38) and V005 labels/error text | Docker-first, boot smoke, fat-jar firewall, contract jobs, permissions and filters unchanged |
| `backend/README.md` | S1/S2 | Describe V005 inventory/behavior and the 33/38 method profiles | Keep toolchain pins, module table, firewall description and evidence boundary; no production claim |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s1-implementation.md` | S1 | New evidence/completion report (implementation author) | Only S1 evidence |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-s2-implementation.md` | S2 | New evidence/completion report (implementation author) | Only S2 evidence |
| `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan.md` | plan | This plan artifact | The only path written by this gate |

Explicitly untouched: V001/V002/V003/V004 SQL; `backend/pom.xml`; application Java (including package/module markers); `docs/12`; `docs/14`; `docs/gateflow/project-execution-control.md`; PA1-PA4 artifacts; API/UI/protocol/security/deployment/acceptance assets; `.pa5-plan-launch.sh`; `.pa5-plan-prompt.md`; every migration other than V005; Git metadata.

## 4. Exact V005 schema and inventory

### 4.1 Migration artifact and DDL creation order

- Filename exactly `V005__ivr_task_session_call.sql`; Flyway version `005`; description `ivr task session call`.
- Runs in Flyway's PostgreSQL transaction; no top-level `COMMIT`, `ROLLBACK`, `BEGIN`, autonomous transaction or isolation change. `SET LOCAL ROLE` is used for object ownership and owner-issued grants; PL/pgSQL bodies use ordinary block `BEGIN`/`END`.
- Do not `CREATE ROLE` or `CREATE SCHEMA`; do not `ALTER SCHEMA`; do not re-grant V004-owned platform/audit or activation/entitlement schema `USAGE`.

Exact order:

1. `SET LOCAL ROLE ivr_owner`.
2. `CREATE TABLE ivr.outbound_call_task` with columns, PK, the two ordinary unique constraints and all 16 named checks (no FK yet).
3. Create the five outbound indexes (two partial uniques + three ordinary).
4. `CREATE TABLE ivr.inbound_session` with columns, PK, unique `call_id`, 14 checks; create its two indexes.
5. `CREATE TABLE ivr.call_record` with columns, PK, unique `call_id`/`session_id`, 15 checks; create its four indexes (two partial uniques + two ordinary).
6. `CREATE TABLE ivr.call_leg` with columns, PK, unique `(call_id, leg_id)`, seven checks; create its one index.
7. `CREATE TABLE ivr.call_event` with columns, PK, unique `(event_domain, event_id)`, 10 checks; create its three indexes (one partial unique + two ordinary).
8. `CREATE TABLE ivr.ivr_decision` with columns, PK, unique `(call_id, purpose)`, 11 checks; create its two indexes (one partial unique + one ordinary). `reservation_id` is a nullable column with no FK.
9. Create the five trigger functions in §5.3.
10. Create the five triggers in §5.3.
11. `REVOKE ALL` on each of the six tables and five functions from `PUBLIC`.
12. `GRANT USAGE ON SCHEMA ivr TO ivr_role` (no owner grant; owner privileges are inherent; no other role).
13. Grant owned-table privileges exactly per §4.6.
14. Grant `EXECUTE` on the four `guard_*` functions to `ivr_role` (not `reject_row_mutation`, following the accepted V003/V004 precedent).
15. `RESET ROLE`.
16. Cross-module grants by the owning S1 roles, each followed by `RESET ROLE`: `customer_iam_owner`, `organization_access_owner`, `subject_number_owner`, exactly per §4.6. No `activation_consent`/`entitlement_metering` table grants (tables absent until V006/V007).
17. Add the 14 ordinary immediate RESTRICT FKs in the order listed in §4.5, then add `fk_outbound_task_claimed_call` last as the same-wave `ALTER_FK`. All are ordinary immediate, not deferrable.
18. No other statement.

### 4.2 Column inventory and derivation

| Table | Columns | Ordered column names (test map source) |
| --- | ---: | --- |
| `outbound_call_task` | 23 | `id,task_id,caller_binding_id,subject_id,owner_account_id,owner_organization_id,created_by_account_id,caller_number_hmac,callee_ciphertext,callee_masked,business_type,status,reason_code,ttl_seconds,expires_at,idempotency_key,request_fingerprint,claimed_call_id,claimed_at,terminal_at,created_at,updated_at,revision` |
| `inbound_session` | 17 | `id,call_id,subject_id,trusted_caller,caller_ciphertext,caller_number_hmac,caller_masked,access_number_masked,node_id,protocol_version,prompt_version,status,confirmed_subject_id,guidance_status,created_at,confirmed_at,revision` |
| `call_record` | 23 | `id,call_id,session_id,task_id,subject_id,caller_ciphertext,caller_number_hmac,caller_masked,callee_ciphertext,callee_masked,access_number_masked,status,node_id,protocol_version,started_at,bridged_at,recording_started_at,recording_ended_at,ended_at,authoritative_seconds,end_reason,created_at,revision` |
| `call_leg` | 11 | `id,call_id,leg_id,leg_type,direction,state,started_at,answered_at,ended_at,end_reason,revision` |
| `call_event` | 14 | `id,call_id,event_domain,event_id,sequence_no,event_type,occurred_at,received_at,payload_ciphertext,payload_redacted,payload_sha256,processing_status,reason_code,revision` |
| `ivr_decision` | 15 | `id,session_id,call_id,purpose,source_event_domain,source_event_id,request_fingerprint,action,reason_code,response_snapshot,response_payload_ciphertext,response_sha256,key_version,reservation_id,decided_at` |
| **Total** | **103** | 23 + 17 + 23 + 11 + 14 + 15 |

Exact types, nullability, defaults and named checks are the frozen dictionary text in `docs/14` §4.23-§4.28. The test's `V005_COLUMN_DETAILS` must assert the exact `column_name|data_type|is_nullable|coalesce(column_default,'<none>')` rows for every column, as V004 does; no semantic or substring equivalence is allowed.

Defaulted columns: 17 total = outbound 5 (`status`, `ttl_seconds`, `created_at`, `updated_at`, `revision`), inbound 3 (`status`, `created_at`, `revision`), call_record 3 (`status`, `created_at`, `revision`), call_leg 1 (`revision`), call_event 4 (`event_domain`, `received_at`, `processing_status`, `revision`), decision 1 (`decided_at`). No `id` has a default; no sequence exists. The `ttl_seconds` default is exactly `30` (approved source default; no configuration key invented). `expires_at` has no default and must be `> created_at`.

### 4.3 Named checks: 73 total

| Table | Count | Exact names |
| --- | ---: | --- |
| `outbound_call_task` | 16 | `ck_outbound_task_task_id`, `ck_outbound_task_single_owner`, `ck_outbound_task_caller_hmac`, `ck_outbound_task_callee_ciphertext`, `ck_outbound_task_callee_masked`, `ck_outbound_task_business_type`, `ck_outbound_task_status`, `ck_outbound_task_reason`, `ck_outbound_task_ttl`, `ck_outbound_task_expiry`, `ck_outbound_task_idempotency_key`, `ck_outbound_task_fingerprint`, `ck_outbound_task_claimed_shape`, `ck_outbound_task_terminal_shape`, `ck_outbound_task_time_order`, `ck_outbound_task_revision` |
| `inbound_session` | 14 | `ck_inbound_session_call_id`, `ck_inbound_session_caller_shape`, `ck_inbound_session_caller_ciphertext`, `ck_inbound_session_caller_hmac`, `ck_inbound_session_caller_masked`, `ck_inbound_session_access_masked`, `ck_inbound_session_node`, `ck_inbound_session_protocol`, `ck_inbound_session_prompt`, `ck_inbound_session_status`, `ck_inbound_session_guidance`, `ck_inbound_session_confirmed_shape`, `ck_inbound_session_time_order`, `ck_inbound_session_revision` |
| `call_record` | 15 | `ck_call_record_call_id`, `ck_call_record_caller_ciphertext`, `ck_call_record_caller_hmac`, `ck_call_record_caller_masked`, `ck_call_record_callee_ciphertext`, `ck_call_record_callee_masked`, `ck_call_record_access_masked`, `ck_call_record_status`, `ck_call_record_node`, `ck_call_record_protocol`, `ck_call_record_terminal_shape`, `ck_call_record_time_order`, `ck_call_record_authoritative_seconds`, `ck_call_record_end_reason`, `ck_call_record_revision` |
| `call_leg` | 7 | `ck_call_leg_leg_id`, `ck_call_leg_type`, `ck_call_leg_direction`, `ck_call_leg_state`, `ck_call_leg_time_order`, `ck_call_leg_end_reason`, `ck_call_leg_revision` |
| `call_event` | 10 | `ck_call_event_domain`, `ck_call_event_event_id`, `ck_call_event_sequence`, `ck_call_event_type`, `ck_call_event_payload_ciphertext`, `ck_call_event_payload_redacted`, `ck_call_event_payload_sha`, `ck_call_event_processing_status`, `ck_call_event_reason`, `ck_call_event_revision` |
| `ivr_decision` | 11 | `ck_ivr_decision_purpose`, `ck_ivr_decision_source`, `ck_ivr_decision_fingerprint`, `ck_ivr_decision_action`, `ck_ivr_decision_reason`, `ck_ivr_decision_direct_reason`, `ck_ivr_decision_shape`, `ck_ivr_decision_snapshot`, `ck_ivr_decision_payload`, `ck_ivr_decision_sha`, `ck_ivr_decision_key_version` |
| **Total** | **73** | 16 + 14 + 15 + 7 + 10 + 11 |

Predicates must be copied verbatim from the frozen dictionary: `docs/14` lines 1296-1313 (outbound), 1360-1375 (inbound), 1423-1439 (call_record), 1477-1485 (call_leg), 1519-1530 (call_event), 1567-1579 (decision). The test freezes the exact PG16.15 `pg_get_constraintdef` deparse for each and compares whole definitions after ASCII-whitespace normalization, never substring/contains/regex/semantic equivalence.

One frozen-text transcription correction and one explicitly authorized schema errata are required. The transcription correction preserves the frozen semantics and changes no name, count or column. The errata is an authorized contract change, not a transcription (F-PA5-PR-02 classification):

1. `ck_call_event_payload_redacted` must use explicit grouping for the jsonb `-` and `->` chains. The executable form is:
   ```sql
   jsonb_typeof(payload_redacted) = 'object'
   AND payload_redacted ? 'source'
   AND payload_redacted ? 'receipt'
   AND jsonb_typeof(payload_redacted->'source') = 'object'
   AND jsonb_typeof(payload_redacted->'receipt') = 'object'
   AND ((payload_redacted - 'source') - 'receipt') = '{}'::jsonb
   AND ((((payload_redacted->'source') - 'eventType') - 'legId') - 'sequenceNo' - 'payload') = '{}'::jsonb
   AND (((payload_redacted->'receipt') - 'payloadSha256') - 'eventFingerprint') = '{}'::jsonb
   AND (payload_redacted->'source') ? 'payload'
   AND (payload_redacted->'receipt') ? 'payloadSha256'
   AND (payload_redacted->'receipt') ? 'eventFingerprint'
   AND ((payload_redacted->'receipt')->>'payloadSha256') = payload_sha256
   ```
   The unparenthesized frozen text fails PostgreSQL 16 operator resolution; the grouped form was executed successfully in a disposable PG16 probe.
2. `ix_call_record_subject_time` names `created_at`, but the original frozen 22-column `call_record` dictionary (and §13.2.1) omitted it; see §9 F-PA5-PLAN-01. **Resolved by user decision (option B, 2026-10-28) as authorized schema errata:** `docs/14` §4.25 now adds `created_at timestamptz NOT NULL DEFAULT now()`, so `call_record` has 23 columns, the six-table V005 total is 103 and defaults total 17, and `CREATE INDEX ix_call_record_subject_time ON ivr.call_record (subject_id, ended_at, created_at);` is created literally in the frozen three-column form. The earlier two-column `(subject_id, ended_at)` recommendation is withdrawn and is not an operative instruction. This is not a semantic-preserving transcription; it is the only authorized contract change. No check, key, FK, guard, grant, function/trigger or V001-V004 contract changes. The disposable probe evidence in §12 remains a historical 102-column result and is not evidence for the 103-column variant.

### 4.4 Keys and indexes: 31 total

Counting convention (repository-established, same as V004): "unique keys" = ordinary `UNIQUE` constraints + partial unique indexes; total indexes = `pg_indexes` count = six PK-backed indexes + eight ordinary-unique-backed indexes + 17 explicit `CREATE INDEX` statements (6 partial unique + 11 ordinary).

| Table | PK | Ordinary UNIQUE constraints | Partial unique indexes | Ordinary indexes | Explicit `CREATE INDEX` | Total `pg_indexes` |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `outbound_call_task` | 1 | 2 | 2 | 3 | 5 | 8 |
| `inbound_session` | 1 | 1 | 0 | 2 | 2 | 4 |
| `call_record` | 1 | 2 | 2 | 2 | 4 | 7 |
| `call_leg` | 1 | 1 | 0 | 1 | 1 | 3 |
| `call_event` | 1 | 1 | 1 | 2 | 3 | 5 |
| `ivr_decision` | 1 | 1 | 1 | 1 | 2 | 4 |
| **Total** | **6** | **8** | **6** | **11** | **17** | **31** |

Ordinary unique constraints (8): `uq_outbound_call_task_task_id`, `uq_outbound_task_idempotency`, `uq_inbound_session_call`, `uq_call_record_call`, `uq_call_record_session`, `uq_call_leg_id`, `uq_call_event_domain_id`, `uq_ivr_decision_call_purpose`.

Partial unique indexes (6): `uq_outbound_task_live_caller` on `(caller_number_hmac) WHERE status IN ('PENDING','CLAIMED')`; `uq_outbound_task_claimed_call` on `(claimed_call_id) WHERE claimed_call_id IS NOT NULL`; `uq_call_record_task` on `(task_id) WHERE task_id IS NOT NULL`; `uq_call_record_active_caller` on `(caller_number_hmac) WHERE status IN ('ACTIVE','UNKNOWN')`; `uq_call_event_sequence` on `(call_id, sequence_no) WHERE sequence_no IS NOT NULL`; `uq_ivr_decision_source` on `(call_id, source_event_domain, source_event_id) WHERE source_event_id IS NOT NULL`.

Ordinary indexes (11): `ix_outbound_task_subject_status (subject_id, status, created_at DESC)`, `ix_outbound_task_org_status (owner_organization_id, status, created_at DESC)`, `ix_outbound_task_creator_status (created_by_account_id, status)`, `ix_inbound_session_hmac_status (caller_number_hmac, status, created_at DESC)`, `ix_inbound_session_subject_time (subject_id, created_at DESC)`, `ix_call_record_subject_time (subject_id, ended_at, created_at)`, `ix_call_record_task (task_id)`, `ix_call_leg_call_type (call_id, leg_type)`, `ix_call_event_call_time (call_id, occurred_at)`, `ix_call_event_processing (processing_status, received_at)`, `ix_ivr_decision_call_time (call_id, decided_at)`.

The test must assert the exact 31-name `pg_indexes.indexname` set and the exact 31 `indexdef` values from a PG16.15 probe. A missing, extra, renamed or semantically different index fails.

### 4.5 Foreign keys: 15 immediate V005 constraints + 2 NO_FK + 1 V006-deferred

All V005 constraints are ordinary immediate `ON UPDATE RESTRICT ON DELETE RESTRICT`, not deferrable, validated.

Created in the V005 FK block (14):

| Name | Child → parent |
| --- | --- |
| `fk_outbound_task_binding` | `outbound_call_task.caller_binding_id` → `subject_number.caller_number_binding(id)` |
| `fk_outbound_task_subject` | `outbound_call_task.subject_id` → `subject_number.business_subject(id)` |
| `fk_outbound_task_owner_account` | `outbound_call_task.owner_account_id` → `customer_iam.customer_account(id)` |
| `fk_outbound_task_owner_org` | `outbound_call_task.owner_organization_id` → `organization_access.organization(id)` |
| `fk_outbound_task_creator` | `outbound_call_task.created_by_account_id` → `customer_iam.customer_account(id)` |
| `fk_inbound_session_subject` | `inbound_session.subject_id` → `subject_number.business_subject(id)` |
| `fk_inbound_session_confirmed_subject` | `inbound_session.confirmed_subject_id` → `subject_number.business_subject(id)` |
| `fk_call_record_session` | `call_record.session_id` → `ivr.inbound_session(id)` |
| `fk_call_record_task` | `call_record.task_id` → `ivr.outbound_call_task(id)` |
| `fk_call_record_subject` | `call_record.subject_id` → `subject_number.business_subject(id)` |
| `fk_call_leg_call` | `call_leg.call_id` → `ivr.call_record(call_id)` |
| `fk_call_event_call` | `call_event.call_id` → `ivr.call_record(call_id)` |
| `fk_ivr_decision_session` | `ivr_decision.session_id` → `ivr.inbound_session(id)` |
| `fk_ivr_decision_call` | `ivr_decision.call_id` → `ivr.call_record(call_id)` |

Same-wave late `ALTER_FK` (1, added last): `fk_outbound_task_claimed_call` on `outbound_call_task.claimed_call_id` → `ivr.call_record(call_id)`.

Intentional `NO_FK` (2): `inbound_session.call_id` (root IVR protocol identity, no durable parent) and `call_record.call_id` (canonical call-scoped parent). Tests must assert no FK constraint exists on either column and that a root/canonical `callId` can be inserted without a parent identity.

Deferred V006 decision (1): `ivr_decision.reservation_id` is nullable in V005; `fk_ivr_decision_reservation` must be absent; `entitlement_metering.usage_reservation` must not exist. `ck_ivr_decision_shape` may still reference the column; a test may insert a random UUID reservation reference to prove the column/shape exist without any reservation semantics.

### 4.6 ACL: exact owner/runtime/cross-module/PUBLIC matrix

Owner: all six tables, five functions and the `ivr` schema are owned by `ivr_owner`. Owner-inherent privileges are not re-granted. `ivr_owner` remains NOLOGIN/NOSUPERUSER/NOCREATEDB/NOCREATEROLE (created V004).

Schema:

- `GRANT USAGE ON SCHEMA ivr TO ivr_role` — no `CREATE`, no other grantee.
- `GRANT USAGE ON SCHEMA customer_iam TO ivr_role`.
- `GRANT USAGE ON SCHEMA organization_access TO ivr_role`.
- `GRANT USAGE ON SCHEMA subject_number TO ivr_role`.
- No V005 change to `activation_consent`, `entitlement_metering`, `platform` or `audit` schema `USAGE` (already granted V004); no table privilege on those schemas in V005.

Owned-table grants to `ivr_role` (no `DELETE`, `TRUNCATE`, `REFERENCES` or `TRIGGER`):

| Table | SELECT | INSERT | UPDATE columns |
| --- | --- | --- | --- |
| `ivr.outbound_call_task` | all | all | `status`, `reason_code`, `claimed_call_id`, `claimed_at`, `terminal_at`, `updated_at`, `revision` |
| `ivr.inbound_session` | all | all | `status`, `confirmed_subject_id`, `guidance_status`, `confirmed_at`, `revision` |
| `ivr.call_record` | all | all | `task_id`, `status`, `started_at`, `bridged_at`, `recording_started_at`, `recording_ended_at`, `ended_at`, `authoritative_seconds`, `end_reason`, `revision` |
| `ivr.call_leg` | all | all | `state`, `answered_at`, `ended_at`, `end_reason`, `revision` |
| `ivr.call_event` | all | all | `processing_status`, `reason_code`, `revision` |
| `ivr.ivr_decision` | all | all | none (append-only; `SELECT`/`INSERT` only) |

Cross-module column `SELECT` to `ivr_role` (read-only; no table-level `SELECT`, no `INSERT`/`UPDATE`/`DELETE`, no row-lock privilege):

| Table | Exact columns |
| --- | --- |
| `customer_iam.customer_account` | `id`, `status` |
| `organization_access.organization` | `id`, `status` |
| `organization_access.organization_membership` | `organization_id`, `account_id`, `role`, `status` |
| `subject_number.business_subject` | `id`, `owner_account_id`, `owner_organization_id`, `revision` |
| `subject_number.caller_number_binding` | `id`, `subject_id`, `number_hmac`, `status`, `verified_at`, `revision` |
| `subject_number.subject_risk_state` | `caller_number_binding_id`, `risk_type`, `state`, `revision` |

Function `EXECUTE` to `ivr_role`: `ivr.guard_outbound_call_task()`, `ivr.guard_inbound_session()`, `ivr.guard_call_record_parties()`, `ivr.guard_call_leg()`. `ivr.reject_row_mutation()` receives no runtime grant (owner-inherent only), following the accepted V003/V004 precedent; its trigger still fires for every writer. No other role receives `EXECUTE` on any V005 function.

Explicit denials: `ivr_role` has no `UPDATE` on `ivr.ivr_decision`; no `UPDATE` on undeclared columns of any owned table; no table-level `UPDATE`; no `DELETE`/`TRUNCATE`/`REFERENCES`/`TRIGGER` on any owned table; no `INSERT`/`UPDATE`/`DELETE` on the six cross-module tables; no `SELECT ... FOR UPDATE` on a cross-module table (no `UPDATE` privilege, so PostgreSQL returns `42501`); no `CREATE` on `ivr`, `customer_iam`, `organization_access` or `subject_number`; no `USAGE` on `ivr` to any other role; no V006/V007 table grant. `PUBLIC` has zero schema, table, column and function privileges.

### 4.7 Trigger functions and triggers

Five functions, all `SECURITY INVOKER`, owned by `ivr_owner`, `SET search_path = ivr, pg_temp`, no transaction control, no ID generation, not `SECURITY DEFINER`:

| Function | Trigger | Fires |
| --- | --- | --- |
| `ivr.guard_outbound_call_task` | `trg_outbound_call_task_guard` | `BEFORE UPDATE ON ivr.outbound_call_task FOR EACH ROW` |
| `ivr.guard_inbound_session` | `trg_inbound_session_guard` | `BEFORE UPDATE ON ivr.inbound_session FOR EACH ROW` |
| `ivr.guard_call_record_parties` | `trg_call_record_guard` | `BEFORE UPDATE ON ivr.call_record FOR EACH ROW` |
| `ivr.guard_call_leg` | `trg_call_leg_guard` | `BEFORE UPDATE ON ivr.call_leg FOR EACH ROW` |
| `ivr.reject_row_mutation` | `trg_ivr_decision_immutable` | `BEFORE UPDATE OR DELETE ON ivr.ivr_decision FOR EACH ROW` |

No trigger is created on `ivr.call_event`: §4.27 freezes mutable `processing_status`/`reason_code`/`revision` and immutable payload/identity, but §10.16 freezes no `call_event` guard. Payload/identity immutability for runtime is enforced by the column `UPDATE` grant; owner-level payload immutability is service-owned and not claimed as a database trigger guarantee.

No `ivr.select_call_action`, `ivr.record_activation_next_action` or any other SQL service function is created. `ivr` contains exactly five functions after V005.

## 5. State, immutability, append-only and sensitive-data decisions

### 5.1 Lifecycle classes

- Mutable heads with `revision`: `outbound_call_task`, `inbound_session`, `call_record`, `call_leg`, `call_event` (processing status/reason/revision only). Conditional service updates are owner behavior; the database enforces revision/terminal shapes by named checks and identity by guards.
- Append-only/frozen: `ivr_decision` — no `UPDATE` privilege to `ivr_role` and `trg_ivr_decision_immutable` rejects owner `UPDATE`/`DELETE` with `IVR_DECISION_APPEND_ONLY`/`P0001`.
- Terminal task rows are immutable by identity columns, rejection of terminal→non-terminal transitions, and `ck_outbound_task_terminal_shape`. The service-owned PENDING-only replace/cancel predicate is not added to the guard because §10.16 freezes only the terminal→non-terminal rule; the test labels `CLAIMED`→`REPLACED`/`CANCELLED` rejection as service-owned (§9.12).
- `UNKNOWN` call remains occupied; the DB does not finalize it. `ACTIVE`/`UNKNOWN` occupancy and terminal shape are check/index backstops; reconciliation and the `CALL_IN_PROGRESS` protocol are service-owned (§9.13/§9.24).
- `call_event` first-payload/first-terminal-outcome absorption, replay/conflict classification and `CALL_UNKNOWN` parent validation are service-owned (§9.24); the DB provides identity uniqueness, sequence uniqueness, payload-shape checks and column-grant immutability.

### 5.2 Exact guard predicates and error identifiers

All errors use `ERRCODE = 'P0001'`.

`ivr.guard_outbound_call_task`:
- Reject changes to `id`, `task_id`, `caller_binding_id`, `subject_id`, `owner_account_id`, `owner_organization_id`, `created_by_account_id`, `caller_number_hmac`, `callee_ciphertext`, `callee_masked`, `business_type`, `ttl_seconds`, `expires_at`, `idempotency_key`, `request_fingerprint`, `created_at` → `OUTBOUND_CALL_TASK_IMMUTABLE`.
- Reject `OLD.status NOT IN ('PENDING','CLAIMED') AND NEW.status IN ('PENDING','CLAIMED')` → `OUTBOUND_CALL_TASK_TERMINAL`.
- If `claimed_call_id` changes: require old NULL and new non-NULL, otherwise `OUTBOUND_CALL_TASK_CLAIM_IMMUTABLE`; require an existing `ivr.call_record` row with that `call_id`, otherwise `OUTBOUND_CALL_TASK_CLAIM_INVALID`.
- `RETURN NEW`.

`ivr.guard_inbound_session`:
- Reject changes to `id`, `call_id`, `subject_id`, `trusted_caller`, `caller_ciphertext`, `caller_number_hmac`, `caller_masked`, `access_number_masked`, `node_id`, `protocol_version`, `prompt_version`, `created_at` → `INBOUND_SESSION_IMMUTABLE`. (The allowed mutable set is only `status`, `confirmed_subject_id`, `guidance_status`, `confirmed_at`, `revision`; `subject_id` is included because the frozen mutable list excludes it and runtime cannot update it.)
- `RETURN NEW`.

`ivr.guard_call_record_parties`:
- Reject changes to `id`, `call_id`, `session_id`, `caller_ciphertext`, `caller_number_hmac`, `caller_masked`, `callee_ciphertext`, `callee_masked`, `access_number_masked`, `node_id`, `protocol_version`, `started_at` → `CALL_RECORD_PARTIES_IMMUTABLE`.
- If `task_id` changes: require old NULL and new non-NULL, and require `EXISTS (SELECT 1 FROM ivr.outbound_call_task t WHERE t.id = NEW.task_id AND t.claimed_call_id = NEW.call_id)`, otherwise `CALL_RECORD_PARTIES_IMMUTABLE`.
- `RETURN NEW`.
- Consequence (validated): the claim transaction must set `task.claimed_call_id` before setting `call.task_id`; the reverse order is rejected. This is the database-observable reciprocal ordering, not proof of the full service orchestration.

`ivr.guard_call_leg`:
- Reject changes to `id`, `call_id`, `leg_id`, `leg_type`, `direction`, `started_at` → `CALL_LEG_IDENTITY_IMMUTABLE`.
- `RETURN NEW`.

`ivr.reject_row_mutation`:
- Always raise `IVR_DECISION_APPEND_ONLY`.

Frozen inconsistency: the `call_record` column `UPDATE` grant includes `started_at`, while the frozen guard rejects any change to `started_at`. V005 implements both exactly as frozen; the observable result is that `started_at` is set only on `INSERT`, and an `UPDATE` fails with `CALL_RECORD_PARTIES_IMMUTABLE` (the privilege check passes first). This is recorded as F-PA5-PLAN-04; the plan does not "fix" either frozen statement.

Authorized `created_at` errata boundary: `call_record.created_at` is absent from the frozen `ivr_role` `UPDATE` column allowlist and the frozen guard list is unchanged, so runtime writes are INSERT-only (`42501` on any runtime `UPDATE` of `created_at`) and owner-level immutability remains service-owned, the same residual class as `call_record.subject_id`. No new guard, check or grant is added for `created_at`.

### 5.3 Sensitive-data boundaries

- `caller_number_hmac` is lookup-only lowercase 64-hex; `callee_ciphertext`/`caller_ciphertext`/`payload_ciphertext`/`response_payload_ciphertext` use the `v1.<key_ref>.<base64url>` envelope; masked values are display-only; no full caller/callee/access number is stored in clear, logged or returned by ordinary APIs.
- `call_event.payload_redacted` is a redacted object with exactly `source` + server-written `receipt`; `payload_sha256` hashes only the canonical `source.payload`; `receipt` is excluded and never client-selectable.
- `ivr_decision.response_snapshot` is the redacted typed snapshot; `response_payload_ciphertext` holds the internal payload; `response_sha256` hashes the canonical redacted snapshot. The full task callee and confirmed direct callee exist only in the encrypted internal payload, never in the redacted snapshot, logs, audit, Outbox or customer responses.
- V005 stores data only; it does not implement fingerprint construction, encryption, hashing, replay or response projection. Those remain service/security-owned (Q2/Q15/Q26).

## 6. Slice design: two slices

Every slice leaves a runnable V001-V005 prefix and is fail-closed. No application-service or V006+ work enters a slice.

### 6.1 Why two slices

- **S1 — migration, exact inventory and ACL** is one observable contract boundary: "V005 exists, applies after V004, is exactly the frozen six-table persistence/security contract, and V001-V004 are unchanged." It includes the migration, the exact catalog/ACL assertions, the cumulative absence/global-list/ACL evolutions, and two new methods.
- **S2 — executable lifecycle, guard, FK, append-only and bounded concurrency backstops** is the second boundary: "the DDL's claimed behaviors are executable and fail closed." It adds five methods and does not edit V005 unless a controller-approved bounded correction is required by a failing frozen-contract assertion.
- Two slices are the minimum falsifiable checkpoints. A third slice would mechanically split task, session/call and event/decision behavior even though the six tables form one reciprocal identity wave with shared fixtures; splitting by table/file/owner/layer is forbidden. One slice would combine a large migration, 73 checks, 15 FKs, five guards, the ACL matrix and all concurrency evidence into a single review surface. **Two slices** comply with the "normally no more than three" principle.

### 6.2 S1 - V005 applies after V004; exact inventory and ACL

**Prerequisites.** Accepted PA4/V004 foundation on base `4006e8e09c39e4527cb543f03646a30073d1e1fa`; V001-V004 hashes in §2.2; Docker-capable PostgreSQL 16; current 31-method suite green; clean worktree except the controller edit and pre-existing untracked runner files; F-PA5-PLAN-01 resolved by the user's option-B decision (authorized `call_record.created_at` errata; 103 columns/17 defaults; three-column index).

**Allowed files.** V005 migration (new, authored once); `PostgresContainerIT.java` (S1 changes below); `.github/workflows/ci.yml` (expected set 33); `backend/README.md` (S1, 33); the S1 implementation artifact (new).

**Exact DDL.** Implement §4.1/§4.5/§4.7/§4.6 in one migration. No other object. No schema/role creation. No service function.

**Exact test changes.**

1. Rename `v001ThroughV004MigrateCleanPostgres16WithoutSkipping` to `v001ThroughV005MigrateCleanPostgres16WithoutSkipping`; assert `migrationsExecuted=5`; add the V005 history row (`version=005`, description `ivr task session call`, script `V005__ivr_task_session_call.sql`, success true); preserve `validate()`/repeat-migrate-zero and the V001/V002/V003 smoke calls; add `exerciseV005IvrInsertsAsRuntimeRoleAndRollBack()` (insert one task, session, call, leg, event and decision as `ivr_role` with method-local parents, then roll back; no committed row).
2. Replace `assertNoV005PlusObjects` with `assertNoV006PlusObjects`: `ivr` has exactly the six V005 tables; `entitlement_metering` and `activation_consent` have zero tables; `evidence`/`sharing` schemas and their roles are absent; the V006+ table names (`trial_policy_version`, `entitlement_grant`, `trial_grant`, `entitlement_balance`, `entitlement_issuance`, `usage_reservation`, `usage_fact`, `consent_record`, `service_activation`, `service_activation_transition`, `media_object`, `recording`, `evidence_record`, `evidence_artifact`, `evidence_event`, `evidence_share_grant`, `share_admission`, `share_access_event`) are absent; `issue_first_trial` is absent; `ivr.select_call_action`/`ivr.record_activation_next_action` are absent; `fk_ivr_decision_reservation` is absent; legacy `ivr.call_task`/`ivr.call_session` remain absent. Call it from the V005 inventory method; remove its call from `v004CatalogOrderInventoryAndGuardsAreExact`.
3. Update the global function list in `assertFunctionsAndTrigger` (line 5333) to add `ivr.guard_call_leg`, `ivr.guard_call_record_parties`, `ivr.guard_inbound_session`, `ivr.guard_outbound_call_task`, `ivr.reject_row_mutation` (25 total V001-V005 functions, alphabetical). Update the global trigger list (line 5454) to add `ivr.call_leg.trg_call_leg_guard`, `ivr.call_record.trg_call_record_guard`, `ivr.inbound_session.trg_inbound_session_guard`, `ivr.ivr_decision.trg_ivr_decision_immutable`, `ivr.outbound_call_task.trg_outbound_call_task_guard` (23 total V001-V005 triggers, alphabetical). Keep every existing entry.
4. Update the no-IVR-table block in `assertRiskOpenClearReopenRevisionProtocol` (around line 8248): assert the six `ivr` tables exist; assert `entitlement_metering` and `activation_consent` have zero tables; keep `evidence`/`sharing` absent; keep legacy `ivr.call_task`/`ivr.call_session` null; change the label from "no IVR/entitlement/consent table" to "V005 ivr tables exist; entitlement/consent tables absent".
5. Evolve `assertV004SchemaUsageMatrix`: add `ivr_role` to `ivr` usage (owner + `ivr_role`), and to `customer_iam`, `organization_access` and `subject_number` usage; keep `ivr_role` absent from `catalog_order` usage and correct the line-6889 comment to "not granted in V005 (§10.12)"; keep `activation_consent_role`/`entitlement_metering_role` subject/entitlement wave boundaries.
6. Evolve `assertV004OwnedAndCrossModuleGrants` S1-universe loop: add the exact `ivr_role` cross-module column `SELECT` map in §4.6. For every S1 table × V004 role, table-level privileges remain false; column `SELECT` is true only for `catalog_order_role`'s existing V004 map and `ivr_role`'s six-table exact map; all other column `SELECT`/`INSERT`/`UPDATE` remain false. Do not drop the prior RR-09 owner-role denials.
7. Extend `functionsHaveFixedSearchPathAndNoTransactionControl`: add the exact five `ivr` functions (`prosecdef=false`, owner `ivr_owner`, `search_path=ivr, pg_temp`); include them in the transaction-control query and the `gen_random_uuid` zero check; assert `ivr.select_call_action`/`ivr.record_activation_next_action` absent; keep `issue_first_trial` absent.
8. Add `V005_TABLES`, `V005_CHECK_DEF_MAP` (73), `V005_PK_UNIQUE_DEFS` (14), `V005_FK_DEFS` (15), `V005_INDEX_DEFS` (31) and helpers `assertV005TablesColumnsChecksIndexesFks`, `assertV005CheckPredicateInventory`, `assertV005FunctionsAndTriggersExact`, `assertV005NoIdDefaultsOrSequences`, `assertV005SchemaUsageMatrix`, `assertV005OwnedAndCrossModuleGrants`, `assertV005PublicAclIsZero`, `assertV005GuardFunctionExecuteBoundaries`, `assertNoV006PlusObjects`.
9. Add exactly two methods:
   - `v005IvrTaskSessionCallInventoryAndGuardShapesAreExact`: exact six tables/owners; 103 ordered column rows with types/nullability/defaults; 73 checks with exact names and whole normalized `pg_get_constraintdef`; 6 PKs, 8 ordinary uniques and 6 partial unique indexes (14 keys); 31 exact `indexdef`s; 15 FK defs/properties (`rrs|false|false|true`); `fk_ivr_decision_reservation` absent and `reservation_id` nullable with no FK; no FK on either root `call_id`; five functions; five triggers with exact tgtype matrix; 17 defaults; zero id defaults/sequences; exact schema/table owners; `assertNoV006PlusObjects`.
   - `v005OwnerPublicRuntimeAndCrossModuleAclsAreExact`: exact schema `USAGE`/`CREATE`; exact owned-table `SELECT`/`INSERT`/column `UPDATE`; exact six cross-module column `SELECT` sets; exact function `EXECUTE` (four guards; `reject_row_mutation` absent); PUBLIC zero on schema/table/function/column; every undeclared privilege false; runtime `42501` denials for undeclared `UPDATE`/`DELETE`/`TRUNCATE` and cross-module writes/`FOR UPDATE`.
10. Update the CI expected set to exactly 33 names and V005 labels/error text.
11. Update README to V005 S1: six tables/103 columns/73 checks/14 unique keys/31 indexes/15 immediate FKs/5 functions/5 triggers/17 defaults; no V006+; 33 methods; V001-V004 hashes preserved.

**Invariants.** V001-V004 blobs and CR-normalized diffs unchanged; V005 is the only new object source; no V006+ object/service function/`issue_first_trial`/reservation FK; no application Java/POM/runtime wiring; all prior 31 methods remain behaviorally identical except the explicitly renamed migration method and the enumerated cumulative assertions; any unexpected catalog mismatch is fixed only inside V005, and any need to change V001-V004, a frozen predicate, a grant outside §10.12/§11.4 or the approved F-PA5-PLAN-01 errata (`created_at`, 103 columns, 17 defaults, three-column index) stops the slice.

**Completion signal.** `./mvnw -B -ntp validate` and `clean verify` pass (10 surefire, no skip); `docker info`; `-Pcontainer-smoke verify` reports exactly 33 methods, `skipped=0 failures=0 errors=0`, exact set; V001-V004 hashes unchanged; V005 SHA-256 recorded; only allowed files changed.

**Stop condition.** Any need to edit V001-V004, the controller, application code or POM; add a V006+ object/service function/`issue_first_trial`/reservation FK; add a role/schema/column/check/index/grant outside the frozen contract; or weaken an assertion.

### 6.3 S2 - Executable lifecycle, guard, FK, append-only and bounded concurrency backstops

**Prerequisites.** Accepted S1 checkpoint; recorded V005 SHA-256; all 33 methods passing; V005 frozen.

**Allowed files.** `PostgresContainerIT.java` (add exactly five methods and bounded helpers); `.github/workflows/ci.yml` (expected set 38); `backend/README.md` (final 38); the S2 implementation artifact (new).

**Exact test changes.** Add:

1. `v005TaskTtlLifecycleAndGuardBackstops` — default `ttl_seconds=30`, `status=PENDING`, `revision=1`, `expires_at > created_at`; duplicate `(created_by_account_id,idempotency_key)` fails `23505 uq_outbound_task_idempotency` and leaves the original TTL/expiry unchanged; a duplicate/replay that attempts to refresh `ttl_seconds`/`expires_at` fails owner `OUTBOUND_CALL_TASK_IMMUTABLE` (service replay/non-refresh deferred); second live `PENDING` task for the same canonical caller fails `23505 uq_outbound_task_live_caller`, and a new `PENDING` task succeeds after the first becomes terminal; `PENDING`→terminal conditional revision succeeds and stale revision affects zero rows; terminal→`PENDING` fails `OUTBOUND_CALL_TASK_TERMINAL`; owner `claimed_call_id` to a nonexistent call fails `OUTBOUND_CALL_TASK_CLAIM_INVALID`; runtime immutable-column `UPDATE` is `42501`; all 16 outbound checks probed with `23514` + exact name; rollback.
2. `v005SessionCallCanonicalCallerAndReciprocalLinkBackstops` — inbound session `RECEIVED`/`revision=1`, trusted-caller/confirmation shapes, unique `call_id`, immutable origin `P0001` as owner and `42501` as runtime; call_record `ACTIVE`/`revision=1`, `task_id=NULL`; second `ACTIVE` (and `UNKNOWN`+`ACTIVE`) call for the same canonical caller fails `23505 uq_call_record_active_caller`, and a new `ACTIVE` call succeeds after the first becomes terminal; root `inbound_session.call_id` and canonical `call_record.call_id` have no FK and can be inserted without a parent identity; party/`started_at` changes fail `CALL_RECORD_PARTIES_IMMUTABLE` as owner and `42501` as runtime; reciprocal link: `task.claimed_call_id` NULL→existing call succeeds, then `call.task_id` NULL→that task succeeds, both directions equal; reverse order, relink, clear or second task fails `CALL_RECORD_PARTIES_IMMUTABLE`/`OUTBOUND_CALL_TASK_CLAIM_IMMUTABLE`; all 14 session + 15 call_record checks probed; rollback.
3. `v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops` — valid CALLER/INBOUND and CALLEE/OUTBOUND legs; duplicate `(call_id,leg_id)` `23505`; guard identity `P0001`; event insert with the exact `payload_redacted` shape and `IVR_EVENT` default; duplicate `(event_domain,event_id)` across calls `23505`; duplicate `(call_id,sequence_no)` `23505`; redacted-shape check rejects missing/extra members; processing status transitions succeed; runtime payload/identity `UPDATE` `42501`; decision `INBOUND_RESOLVE`/`ACTIVATION_NEXT_ACTION`/`DIRECT_AUTHORIZATION` shape inserts including `ENTER_CALLEE`, `DIAL_TASK_CALLEE` with a random reservation UUID (no FK) and successful direct authorization (`action NULL`, `DIRECT_AUTHORIZED`, non-null reservation UUID); duplicate `(call_id,purpose)` `23505`; duplicate `(call_id,source_event_domain,source_event_id)` `23505`; owner `UPDATE`/`DELETE` `IVR_DECISION_APPEND_ONLY`, runtime `42501`; all 7 leg + 10 event + 11 decision checks probed; label direct-DTMF/ENTER_CALLEE as shapes only; rollback.
4. `v005ForeignKeyAndDeferredReservationFkBackstops` — missing-parent `23503` + exact name for all 15 V005 FKs; representative parent-delete RESTRICT `23503` for `fk_outbound_task_binding`, `fk_outbound_task_owner_account`, `fk_outbound_task_owner_org`, `fk_inbound_session_subject`, `fk_call_record_session`, `fk_call_record_task`, `fk_call_leg_call` and `fk_outbound_task_claimed_call` (choose fixtures so the reported constraint is unambiguous); exact 15-row FK map; `fk_ivr_decision_reservation` absent; `reservation_id` nullable/no FK; both root `call_id` columns have no FK; `entitlement_metering`/`activation_consent` zero tables; rollback.
5. `concurrentV005TaskClaimAndActiveCallHasOneWinner` — bounded two-connection races with distinct `application_name`, `pg_stat_activity` lock-wait detection, `statement_timeout` and future timeout, no sleep-only assertion: (a) two `PENDING` inserts for one canonical caller → one winner, loser `23505 uq_outbound_task_live_caller`; (b) two `ACTIVE` calls for one canonical caller → one winner, loser `23505 uq_call_record_active_caller`; (c) two conditional claims of one pre-committed `PENDING` task with the same call → exactly one row updated, loser zero rows, winner links the call; (d) duplicate `(event_domain,event_id)` and duplicate `(call_id,sequence_no)` → one winner each; (e) duplicate `(call_id,purpose)` → one winner; (f) category-2 savepoint: set `task.claimed_call_id` and `call.task_id`, `ROLLBACK TO SAVEPOINT`, assert both NULL and the pre-savepoint call survives; (g) category-3 whole-transaction failure: force a check/SQL error after both assignments, roll back the transaction, assert no claim/link persists. Cleanup in `finally`: close both connections, reconnect as migration identity, one `TRUNCATE` of the six `ivr` tables (test-fixture hygiene only; runtime `TRUNCATE` denial separately asserted), delete method-local V003/V002 parents in FK-safe order, assert zero method-local rows.

**Invariants.** V005 unchanged; only the test class, CI, README and S2 artifact change; every expected failure uses a savepoint/separate connection and asserts exact SQLSTATE + constraint/exception name; valid paths are demonstrated alongside rejections; no append-only row survives cleanup; no runtime `DELETE`/`TRUNCATE`; claim/reservation/decision orchestration remains deferred and labeled.

**Completion signal.** Full container profile exactly 38 methods, `skipped=0 failures=0 errors=0`, exact set; focused behavior methods once each; concurrency method at least three independent runs (`tests=1`, no skip/failure/error); default verify 10/0/0/0; boot smoke; fat-jar firewall; V001-V004 hashes unchanged; V005 hash unchanged; cleanup/whitespace/status checks pass.

**Stop condition.** Any need to edit V005, V001-V004, the controller, application code or POM; any new schema/function/trigger/grant/service; any append-only cleanup that weakens a runtime denial; any unexplained mismatch requiring a relaxed assertion.

## 7. Test matrix and expected totals

### 7.1 Method evolution

Current 31 → S1 33 → S2 38. No placeholder, `@Disabled`, `Assumptions`, environment skip or weakened method. CI asserts exactly the method set present at each checkpoint.

| Slice | Count | Exact new/renamed methods | Preserved |
| --- | ---: | --- | --- |
| S1 | 33 | `v001ThroughV005MigrateCleanPostgres16WithoutSkipping` (renamed), `v005IvrTaskSessionCallInventoryAndGuardShapesAreExact`, `v005OwnerPublicRuntimeAndCrossModuleAclsAreExact` | All other 30 current methods |
| S2 | 38 | `v005TaskTtlLifecycleAndGuardBackstops`, `v005SessionCallCanonicalCallerAndReciprocalLinkBackstops`, `v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops`, `v005ForeignKeyAndDeferredReservationFkBackstops`, `concurrentV005TaskClaimAndActiveCallHasOneWinner` | All S1 33 methods |

The full final 38-name set (S1 uses the first 33; S2 uses all 38) is exactly:

```text
v001ThroughV005MigrateCleanPostgres16WithoutSkipping
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
```

The CI expected-set edit must rename the migration method and add exactly the seven new names (two at S1, five at S2), updating the label/count; no other workflow behavior changes.

### 7.2 Probe distribution

| Evidence | Method | Count |
| --- | --- | ---: |
| Inventory/guard shapes | `v005IvrTaskSessionCallInventoryAndGuardShapesAreExact` | six tables, 103 columns, 73 checks, 14 keys, 31 indexes, 15 FKs, 5 functions, 5 triggers, 17 defaults, 0 id defaults, 0 sequences |
| ACL | `v005OwnerPublicRuntimeAndCrossModuleAclsAreExact` | schema/table/column/function/PUBLIC matrix + denials |
| Outbound checks | `v005TaskTtlLifecycleAndGuardBackstops` | 16 |
| Session + call_record checks | `v005SessionCallCanonicalCallerAndReciprocalLinkBackstops` | 14 + 15 = 29 |
| Leg + event + decision checks | `v005LegEventDecisionFrozenIdentityAndAppendOnlyBackstops` | 7 + 10 + 11 = 28 |
| **Total named checks** | | **73** |
| FK missing-parent | `v005ForeignKeyAndDeferredReservationFkBackstops` | 15 |
| Concurrency/rollback | `concurrentV005TaskClaimAndActiveCallHasOneWinner` | 5 races + 2 rollback scenarios |

### 7.3 Cleanup and determinism

- Behavior methods 1-4 use a transaction/savepoint and roll back; no committed fixture survives.
- The concurrency method commits only method-local random fixtures and cleans them with one migration-identity `TRUNCATE` of all six `ivr` tables plus FK-safe V003/V002 parent deletion; it asserts zero method-local rows. Runtime `TRUNCATE`/`DELETE` denial remains asserted in the ACL method.
- Time assertions use explicit `timestamptz` values and `clock_timestamp()`; no sleep-only timing. Concurrency uses two real connections, distinct `application_name`, `pg_stat_activity` wait-event detection, bounded `statement_timeout` and future timeouts. Every expected failure asserts exact SQLSTATE and exact constraint/exception name.
- Shared-container isolation: every assertion is scoped to method-local UUID/key/hash values; no global count depends on rows from other methods.

### 7.4 Commands and expected results

```bash
cd backend
./mvnw -B -ntp validate                 # Enforcer/toolchain, exit 0
./mvnw -B -ntp clean verify             # 10 surefire tests, 0 skipped/failures/errors
docker info                             # Docker available
./mvnw -B -ntp -Pcontainer-smoke verify # S1: 33; S2: 38; skipped=0 failures=0 errors=0
java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1
grep -F 'Started OneCallApplication' target/boot.log
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar | grep -E 'BOOT-INF/lib/(spring-security|flyway|jooq|HikariCP|postgresql|testcontainers)' && exit 1 || true
# V001-V004 blob-content hashes must equal §2.2; V005 SHA-256 recorded at S1 and unchanged at S2
git cat-file -p HEAD:backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql | sha256sum
# ... V002, V003, V004
sha256sum backend/src/main/resources/db/migration/V005__ivr_task_session_call.sql
```

Expected Failsafe XML: one `TEST-*PostgresContainerIT.xml`; `tests == expected size`; `skipped=0`; `failures=0`; `errors=0`; `cases == expected`; every surefire report `skipped=0`.

## 8. Documentation decision

- Update `backend/README.md` in S1 and S2 with the V005 inventory/behavior, 33/38 profiles, V001-V004 hashes and the "no V006+" boundary. Keep toolchain pins, module table, firewall and evidence-boundary sections.
- Do not modify `docs/12` or `docs/14`: they are frozen design sources. Do not add a new design document.
- Required Gateflow artifacts: S1 and S2 implementation/evidence reports, controller gate records and the plan-review record (controller-owned). No implementation author may write the control document.
- The README and CI are evidence descriptions, not production acceptance. Container/CI success is not A5 `PASS` or production acceptance.

## 9. Risks, open inputs, owners and destinations

| ID | Item | Status / impact | Owner / destination |
| --- | --- | --- | --- |
| F-PA5-PLAN-01 | `ix_call_record_subject_time` names `created_at`, absent from the original frozen 22-column `call_record` dictionary and §13.2.1. | **Resolved by user decision (option B, 2026-10-28).** Authorized schema errata adds `ivr.call_record.created_at timestamptz NOT NULL DEFAULT now()`; `call_record` is 23 columns, the six-table V005 total is 103 and defaults total 17; `ix_call_record_subject_time (subject_id, ended_at, created_at)` is created literally and the original three-column index is preserved. Not a semantic-preserving transcription; the two-column recommendation is withdrawn. `docs/14` §4.25/§13.2.1 and this plan are amended; no other contract change. | User/schema authority; plan-fix artifact `docs/gateflow/pa5-v005-ivr-task-session-call-foundation-plan-fix-1.md`; S1 artifact. |
| F-PA5-PR-01 | Plan review: the frozen three-column index conflicts with the 22-column dictionary and needs explicit user/schema authority, not a silent substitution. | **Closed.** User selected option B; the `created_at` errata is the explicit authority decision, and the `docs/14`/plan totals are amended to 103 columns/17 defaults. | User/schema authority; fix-1 artifact; fresh re-review. |
| F-PA5-PR-02 | Plan review: §4.3 misclassified the index resolution as a semantics-preserving transcription. | **Closed.** §4.3 now separates the one genuine transcription correction (F-PA5-PLAN-02 JSONB grouping) from the authorized schema errata and labels the errata as the only authorized contract change. | Plan author; fix-1 artifact; fresh re-review. |
| F-PA5-PLAN-02 | `ck_call_event_payload_redacted` frozen text fails PG16 operator resolution for the jsonb `-`/`->` chains. | **Resolved transcription correction.** Use the grouped expression in §4.3; semantics unchanged; check count/name unchanged; record in the S1 artifact. | Implementation author; S1 artifact. |
| F-PA5-PLAN-03 | Existing V004 ACL assertion comment says `ivr_role`→`catalog_order` USAGE is "deferred to V005"; §10.12 never grants it. | **Non-blocking correction.** Keep false in V005; correct the comment only. | Implementation author; S1 test change. |
| F-PA5-PLAN-04 | `call_record.started_at` is in the runtime `UPDATE` grant but the frozen guard rejects changes to it. | **Non-blocking frozen inconsistency.** Implement both as written; `started_at` is set on INSERT only; an UPDATE raises `CALL_RECORD_PARTIES_IMMUTABLE` (privilege passes first). | Implementation author; S1/S2 artifact risk note. |
| Q18 | Production TTL configuration key/approval for the 30-second default. | Deferred. V005 freezes the source default 30 per task; no config key invented. | Platform/configuration owner; later wave. |
| Q28 | Authoritative IVR-local B-leg uniqueness/lost-response recovery. | Deferred. `ivr.call_leg` is a business fact, not proof of outbound uniqueness. | IVR protocol owner; later wave. |
| Q15/Q26 | Wire encoding and IVR JSON-schema version; fingerprint construction. | Deferred. V005 stores columns only; no fingerprint/encryption service. | IVR/API owner; later wave. |
| Q7/Q8/Q10/Q16/Q24/Q25/Q27 | Telephony, prompt, risk, access-policy, authorization-window and IVR-local inputs. | Deferred, not guessed. | Named later owners; their waves. |
| Q17/Q23 | Runtime role provisioning/authentication and connection role switching. | Deferred. Testcontainers superuser is test-only; same-connection owner-service contract unchanged. | Platform/Database/Security; later wave. |
| — | Claim/reservation/decision atomic orchestration; `CALL_IN_PROGRESS`; `UNKNOWN` reconciliation; call-event replay/conflict/absorption. | Service-owned; V005 proves only DB backstops. | IVR/entitlement service waves. |
| — | `ivr_role` entitlement/activation table reads. | Cannot be granted in V005 because the tables are created in V006/V007; schema USAGE already exists. | V006/V007. |
| — | `call_record.subject_id` is not in the frozen guard list and has no runtime UPDATE grant; owner-level immutability is service-owned. | Residual risk, not a V005 DB guarantee. | IVR service wave. |
| — | `call_record.created_at` (authorized errata) is outside the frozen runtime UPDATE column grant and the frozen guard list is unchanged; runtime writes are INSERT-only and owner-level immutability is service-owned. | Residual risk, not a V005 DB trigger guarantee; no new guard/check/grant. | IVR service wave. |
| — | `call_event` has no frozen guard; payload/identity immutability is column-grant-only for runtime; owner-level immutability is service-owned. | Residual risk, not a V005 DB trigger guarantee. | IVR service wave. |

No other blocking open question is known for planning. F-PA5-PLAN-01 is resolved by the user's option-B decision recorded in this plan fix; all other items are deferred inputs with named destinations, not guessed values.

## 10. No-goal-drift and no-over-design analysis

- No V006-V009 object, no trial/entitlement/balance/reservation/usage, no activation/consent, no recording/media/evidence/sharing table/function, no `issue_first_trial`, no reservation FK, no evidence FK, no V006 service.
- No `ivr.select_call_action`, `ivr.record_activation_next_action` or any SQL service function; no application service, API, Gateway, controller, JPA, bridge, upload, settlement, reconciliation, frontend, cloud or runtime DataSource/Flyway wiring.
- No new role/schema; no schema `CREATE`; no table privilege implied by schema `USAGE`; no grant outside §10.12/§11.4; no V006/V007 table grant.
- No extra column, table, check, index, trigger, enum value, registry, provider field, soft-delete, retention policy, queue framework, workflow engine, state-machine SQL or speculative configuration key.
- No "fix" of frozen inconsistencies by changing the design: F-PA5-PLAN-01 is the single user-authorized schema errata recorded in §4.3/§9 (`created_at`, 103 columns, 17 defaults, three-column index preserved); F-PA5-PLAN-02 is an executable transcription with identical semantics; F-PA5-PLAN-03/04 are recorded, not redesigned.
- No weakening of existing 31 methods: the only changes are the enumerated rename, cumulative absence/global-list/schema/ACL evolutions and added methods.
- No service-owned behavior is claimed as database-enforced: claim+reservation atomicity, expiry housekeeping, replay/conflict classification, `CALL_IN_PROGRESS`, `UNKNOWN` reconciliation, event absorption and decision selection remain deferred and are labeled as such in tests.

## 11. Completion report format

The implementation-gate completion report must contain:

1. Runtime/provider/task: Java/Maven/Docker/PostgreSQL versions; provider (`mimo26flash` or fallback) and task ID; runner exit/JSON status and stderr.
2. Artifact path and SHA-256 for the V005 migration, test class, CI, README and each Gateflow implementation artifact.
3. Chosen slice count and rationale (two slices, as §6.1).
4. Exact inventory totals: 6 tables, 103 columns, 73 checks, 6 PKs, 8 ordinary uniques, 6 partial unique indexes, 14 unique keys, 31 indexes, 15 immediate FKs, 2 NO_FK, 1 V006-deferred FK, 5 functions, 5 triggers, 17 defaults, 0 id defaults, 0 sequences; method totals 31→33→38.
5. Validation performed: exact commands, Surefire/Failsafe counts, exact method-set comparison, V001-V004 hashes, V005 hash, boot/jar checks, cleanup/status, and any command not run.
6. Risks/open inputs: F-PA5-PLAN-01/F-PA5-PR-01/F-PA5-PR-02 closure (option B authorized errata), F-PA5-PLAN-02 correction, F-PA5-PLAN-03/04 notes, deferred Q items and residual risks.
7. Permission denials/blockers reported separately; do not fabricate executable validation.

## 12. Plan validation performed by this author

- Verified workspace, branch `codex/pa5-v005-ivr-task-session-call-foundation`, HEAD `4006e8e09c39e4527cb543f03646a30073d1e1fa`, preserved `GIT_DIR`/`GIT_WORK_TREE`, and the expected controller modification.
- Read the complete PA5 control section, docs/12 sections 5/6/14/15 Phase B, docs/14 §2.9, §2.10-2.11, §4.23-4.28, §5.4, §9.0.1, §9.11-9.16, §9.21-9.24, §10.12-10.17, §11.4 and the V001-V004 migrations, `PostgresContainerIT`, CI and README.
- **Historical/superseded probe evidence (102-column variant; not validation of the current 103-column contract):** independently derived and then verified the inventory with a disposable `postgres:16` container: V001-V004 applied; a temporary draft V005 (outside the repository) applied using the then-proposed two-column `ix_call_record_subject_time (subject_id, ended_at)` substitution and the F-PA5-PLAN-02 JSONB grouping correction. Live catalog results of that superseded draft: 6 tables; 102 columns (23/17/22/11/14/15); 73 checks (16/14/15/7/10/11); 6 PK; 8 unique constraints; 6 partial unique indexes; 14 unique keys; 31 indexes; 15 FKs all `rrs|false|false|true`; 5 functions (`prosecdef=false`, `ivr_owner`, `search_path=ivr, pg_temp`); 5 triggers; 16 defaults; 0 id defaults; 0 sequences; schema/table owner `ivr_owner`; `fk_ivr_decision_reservation` absent; `reservation_id` nullable/no FK; both root `call_id` columns no FK; `entitlement_metering`/`activation_consent` zero tables; `issue_first_trial` and IVR service functions absent. The current 103-column/17-default/three-column-index contract was **not** executed in that probe and is not claimed as executable evidence by this fix; S1 must revalidate it.
- Verified ACL: `ivr_role` schema USAGE on `ivr`/`customer_iam`/`organization_access`/`subject_number`; no CREATE; exact owned table SELECT/INSERT and column UPDATE sets; no table-level UPDATE/DELETE/TRUNCATE; exact six cross-module column SELECT sets; four guard EXECUTE grants; `reject_row_mutation` no runtime grant; PUBLIC zero.
- Verified behavior: default `ttl_seconds=30`; duplicate live-caller `23505`; duplicate idempotency `23505`; reciprocal claim link succeeds in the required order; relink/clear/immutable columns reject with exact `P0001` as owner and `42501` as runtime; terminal→non-terminal `OUTBOUND_CALL_TASK_TERMINAL`; nonexistent claimed call `OUTBOUND_CALL_TASK_CLAIM_INVALID`; session/call/leg identity guards; decision append-only; duplicate task/call/event/decision identities `23505`; missing-parent FK `23503`; category-2 savepoint clears both link assignments while the pre-savepoint call survives.
- Removed the probe container and temporary files; confirmed the repository delta is only the controller-owned control modification and the pre-existing untracked runner files. No commit, stage, push, PR, merge or agent dispatch was performed.
- Permission denials: none observed. The probe used Docker and `psql` directly; Maven was not run by this planning gate.

## 13. Resolved blocker summary for the plan gate

**F-PA5-PLAN-01 is resolved; no blocking planning input remains.** The user selected option B on 2026-10-28: `ivr.call_record.created_at timestamptz NOT NULL DEFAULT now()` is added by authorized schema errata, the frozen `ix_call_record_subject_time (subject_id, ended_at, created_at)` is preserved, `call_record` is 23 columns, V005 totals 103 columns/17 defaults, and no other count/key/check/guard/grant changes. The earlier two-column recommendation is withdrawn. All other design inputs are resolved, explicitly deferred with named owners, or recorded as non-blocking transcription/consistency notes.
