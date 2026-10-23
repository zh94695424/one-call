# A0 S2 fix 1 artifact (V004-V007, specification only)

- Task label: `a0-spec-s2-fix1-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix`, bounded S2 fix 1 for independent review F-01..F-16 and OQ-1..OQ-4
- Runtime/provider: Claude Code / `ds` / DeepSeek-V4.1-Flash
- Date: 2026-10-22
- Binding checkpoint: S1 accepted at `5404d9caa73b72c412ca2065bef19dfb46d154d3`; S2 implementation is authored but was not accepted; this fix is **author-complete / independent re-review pending**, not S2 acceptance, not A0 completion and not production/runtime completion.

## 1. Scope and changed files

| Path | Change | SHA256 after fix |
| --- | --- | --- |
| `docs/14-production-data-model-and-flyway-schema-spec.md` | Cumulative specification; fix 1 corrections | `69CDE93B0E13442AC766AD2EAC353F53095B3D62D8C355AFADCCA301DA16A354` |
| `docs/gateflow/a0-production-data-model-schema-spec-s2-fix-1.md` | This fix artifact (new) | Reported in the completion report; not self-embedded |

The specification was 3686 lines / 504210 bytes at the reviewed S2 author checkpoint. After fix 1 it is 3958 lines / 593859 bytes. The increase is specification text only: no dictionary row, table, column, invariant, wave, slice, action, reason-registry value, SQL function or migration was added.

No controller, plan, prior author/review artifact, source document, prototype, screenshot, `logo/preview.html`, `docs/reviews/plan-review-20261021-205118.md`, `.git`/config/credential path or unrelated document was modified. No stage, commit, push, PR, merge or S3 work was performed. No other agent or provider was invoked.

## 2. Finding status

All accepted findings are **已修复** at the specification level. None is claimed as runtime-proven.

| Finding | Status | Concrete correction location | Evidence |
| --- | --- | --- | --- |
| F-01 exact missing schema `USAGE` | 已修复 | §3.1; §10.12 schema rows; §11.4 V004 step 1/4, V005 step 4, V006 step 5, V007 step 4; T-S2-41 | Every declared S2 cross-module `SELECT` now has containing-schema `USAGE` without `CREATE` for the exact reader (`customer_iam`, `organization_access`, `subject_number` additions; existing S2-schema rows retained). No table write or `CREATE` is granted. |
| F-02 `SECURITY DEFINER` owner's nested Outbox privilege | 已修复 | §10.9 owner row; §10.11; §10.12 internal owner row; §11.4 V006 step 3; T-S2-42 | The NOLOGIN `entitlement_metering_owner` receives only internal `USAGE` on `platform` and `EXECUTE` on `platform.outbox_append`; `PUBLIC`/runtime roles do not. Forced nested-Outbox failure rolls back with the caller. |
| F-03 `ivr_role` `granted_units` | 已修复 | §10.12 `entitlement_balance` read row; §10.18; T-S2-40/41 | `ivr_role` now reads `granted_units` and can compute `granted_units - reserved_units - consumed_units`; the owner still performs the spend authorization. |
| F-04 deterministic single-balance authorization | 已修复 | New §10.18; §10.14 (`availableRecordingSeconds`, `DIAL_TASK_CALLEE`); §9.11/§9.13/§9.14/§9.17/§9.18; T-S2-34/40/52 | Exact subject/type/`AVAILABLE`/`valid_from <= clock_timestamp() < expires_at`/positive-available predicate; approved allocation-policy reference required; `SOLE_ELIGIBLE` single-candidate mechanics; absent/unapproved/ambiguous policy fails closed with typed denial and no claim/reservation; `authorizedUnits = min(present approved request/cap, available)` with no invented default; conditional debit repeats the exact predicates; routing and reservation use the same selector. Q5/Q25 are not treated as approving trial-first/FIFO/stacking. |
| F-05 MINIAPP/WEB activation and trial | 已修复 | New §9.25; §10.13 service row; §10.20 `record_client_activation`; §4.37; T-S2-54 | One `ACTIVATION` consent (`ACCOUNT`, `MINIAPP`/`WEB`), one `INACTIVE → ACTIVE` transition, and `issueFirstTrial` with `source_action_domain = ACTIVATION` in one transaction; exact event/fingerprint replay, changed-input conflict, already-active no duplicate transition, stale/declined refusal, category-3 rollback; no login identity/owner and no invented trial eligibility. |
| F-06 idempotency-compatible lock order and savepoint | 已修复 | §9.0.1 items 10-12 and bullets; §9.11 steps 1-13; §9.12 steps 1-3; §9.13 steps 2-8; §9.14 steps 1-6; §10.13 selector row; T-S2-15/32/33/63 | `platform.idempotency_record` is acquired after caller/session/access locks and explicitly before task/reservation; replay precedes housekeeping and effects; the attempt savepoint is established after accepted activation/trial and before any tentative claim/reservation; category 2 rolls back to it and then writes the no-dial decision/completion; category 3 rolls back the whole transaction and never swallows a DB exception. |
| F-07 purchase-consent service/validation | 已修复 | §4.36; §9.17 steps 1-5; §10.13 service row; §10.20 `append_purchase_consent`; §10.12 `catalog_order_role` consent read grant; T-S2-55 | `activation-consent` owns the one immutable `PURCHASE` `ACCEPT` append with current policy/prompt/content, subject/account/channel/source/event/fingerprint; the order service validates purpose/subject/channel/source/account/current policy/action/fingerprint in the same transaction and rolls back the `NEW` row and tentative consent on denial; no direct `consent_record` insert by catalog-order. |
| F-08 call-event processing and settlement handoff | 已修复 | §4.27; new §9.24; §10.13 service rows; §10.20 `process_call_event`/`settle_call_usage`; T-S2-56 | One `call_event` insert/dedupe by event ID; same-fingerprint duplicate returns first outcome; changed event/sequence quarantined; out-of-order consistent events converge once; terminal contradictions quarantined; call/leg updates are owner-only; accepted terminal usage hands off same-transaction to the entitlement owner, which never reads/writes IVR tables; no double settlement and no `UNKNOWN` release. |
| F-09 catalog/trial-policy publication serialization | 已修复 | §4.17; §4.29; §9.0.1 publication advisory keys; new §9.26; §10.13/§10.20 publication services; §10.12 `trial_policy_version` `published_at` update grant; §10.16 guard; T-S2-57/64 | Owner-scoped advisory serialization plus expected-current-version compare; one winner sets `published_at`/`ACTIVE` and offsales the previous active version (or one enabled trial policy) in one transaction; stale/concurrent loser gets a stable conflict/current state; immutable content/history and existing orders/grants are never rewritten; no raw unique error and no generic workflow. |
| F-10 excess duration and zero-use boundary | 已修复 | §4.34 settlement-boundary note; §9.15; §9.16; §12.6; T-S2-35/36 | `settled_units <= authorized_units` and positive `usage_fact.units` remain exact; known use in `1..authorized_units` settles once and releases the remainder; known zero use becomes `RELEASED` with `settled_units = 0` and **no** positive fact; reported excess stays `RECONCILING` with `DURATION_EXCEEDS_AUTHORIZED`, no clamp/settle/release/fact/evidence; `UNKNOWN` preserves occupancy and never releases. The frozen catalog's "plus approved tolerance" phrase is preserved and explicitly qualified outside the frozen block; Q8 remains open. |
| F-11 exact fingerprint/Outbox identities | 已修复 | New §10.17; §2.9; §4.27; §9.11/§9.24/§9.25/§9.26; T-S2-58 | Bounded persisted-data registry: exact semantic fields and exclusions for `IVR_CONFIRM`/`ACTIVATION_NEXT_ACTION`, `INBOUND_RESOLVE`, `DIRECT_AUTHORIZATION`, both trial action domains, `PURCHASE` order/consent, task, call event, activation transition, payment event and usage; canonical sorting/null-safe normalization; event ID excluded where replay must survive a regenerated ID; exact Outbox domain/dedupe key/aggregate/type/payload fields for every S2 fact; replay appends no duplicate; same key/different hash conflicts. Q15 wire encoding and Q26 final JSON schema version remain later. |
| F-12 frozen typed commerce/schedule snapshots | 已修复 | New §10.19; §9.17; §9.18; §9.26; T-S2-04/59 | Versioned snapshot shape with exact keys/types; `lineKey` identity; one schedule entry per installment/type; approved absolute `validFrom`/`expiresAt` copied from validated approved input; `units = quantity`; publication and issuance validation; never reconstructed from mutable catalog; missing/ambiguous/expired-window timing records `FAILED` with no guessed grant while the payment fact remains observable; trial dates use the explicit `granted_at + valid_days * interval '1 day'` calculation with `clock_timestamp()`; no Q20 due/grace/calendar arithmetic or price/quantity is invented. |
| F-13 client key bound 475 | 已修复 | §4.20 column/check/prose; §9.17 step 1; T-S2-53 | `customer_order.idempotency_key` is 1-475, so `account_id || ':' || key` is at most 512; 476/512 are rejected before any mutation; platform 1-512 remains unchanged. |
| F-14 correct status | 已修复 | Header; §1.1; §1.5; §4.1/§4.1.1/§5.1/§6.1/§7.1/§8.1/§11.1/§11.2/§12.1 introductions; §13.1 status check | Status is S1 accepted / S2 fix-1 authored-re-review-pending / S3 planned; `issue_first_trial` is authored in V006/S2; frozen blocks are structural closure, not S2/S3 implementation evidence. |
| F-15 reason reference outside frozen catalog | 已修复 | §4.28 navigation note; §13.1 check | All nine frozen blocks remain byte-identical; the §4.1 catalog row's "section 11" phrase is preserved. The normative note points to §2.9 and the named `ck_ivr_decision_reason`; §11 is identified as Flyway order only. |
| F-16 valid direct-authorization chains | 已修复 | §9.14 steps 1-6; §7.3 direct row; §10.14 direct rules; INV-34 S2 expansion; T-S2-21/22/60/61 | Accepts both (a) frozen `INBOUND_RESOLVE = ENTER_CALLEE` with no superseding/conflicting decision and (b) frozen `GUIDE_ACTIVATION` + `CONFIRMED` + committed `ACTIVATION_NEXT_ACTION = ENTER_CALLEE`. Rejects obsolete earlier `ENTER_CALLEE`, later no-dial decisions, task-backed fallback/non-empty `taskId`, unconfirmed/contradictory chains and competing calls; rechecks current no-task/access/activation/consent/entitlement predicates excluding the current call; no registration/login/membership/real-name prerequisite; never rewrites resolve, never adds an eighth action, never redials on replay; task-backed dial uses the frozen callee without DTMF confirmation. |

## 3. OQ outcomes

| OQ | Outcome | Record |
| --- | --- | --- |
| OQ-1 first-time unregistered IVR subject/binding | **已接受/已实现 (accepted)** | New §10.20 `subject-number.find_or_create_ivr_subject` and §9.13 steps 2-3: after immutable replay checks, under the canonical-number lock, before subject/session/call/activation locks, reuses the unique active binding or atomically creates one unowned `IVR` subject (both owner columns null) and one active binding; never creates an account/login/owner/enterprise claim. Disabled/retired/conflicting and concurrent-unique outcomes plus category-3 rollback are explicit; S1 ownership/claim semantics are unchanged. |
| OQ-2 runtime role provisioning | **deferred-with-owner** | §9.0.1 bullet: same-connection owner services are required, but no LOGIN/`SET ROLE`/pool-provisioning mechanism is claimed. Owner: Platform/Database/Security; destination: deployment-security design and executable role-provisioning/privilege tests (Q17/Q23). |
| OQ-3 production policies/values | **deferred-with-owner** | §14 fix-1 paragraph: allocation policy/cap, approved schedule/windows, prompt/policy content, tolerance and authorization window remain with Product/Finance/Operations/IVR/Backend and Q5/Q8/Q10/Q20/Q25. Fix 1 defines validated inputs and fail-closed boundaries only. |
| OQ-4 trial-policy publication | **covered by accepted F-09** | §9.26 and §10.20 publication services; no separate OQ or new table/service family. |

## 4. Preserved baseline and S1 semantics

- 44 tables; 40 invariants; 9 waves (`3/6/4/7/6/7/3/5/3`); slices `S1:13,S2:23,S3:8`; lifecycle `28/16`; sensitive `28`; FK `74 FK + 3 ALTER_FK + 2 DEFERRABLE_FK + 2 NO_FK = 81`; 24 key sets; five function-wave rows (four V001 + one V006).
- Exactly 36 detailed dictionaries and 428 accepted columns (S1 13/121, S2 23/307); no dictionary/column added, removed or renamed.
- Nine frozen comment blocks and the nine wave rows, 40 invariant rows and 28 open-input rows are byte-identical to the accepted plan.
- No SQL function beyond the four V001 functions and the one V006 `issue_first_trial`; all new collaborator services are application services on one connection/one local transaction with `Propagation.MANDATORY` and no function-wave entry.
- S1 ownership, claim replay/risk/fail-closed enterprise boundary, no-savepoint whole-transaction retry, stable logical membership-pair lock, canonical-number placement and owner-only row-lock/cross-module read contracts are preserved.
- No table, column, executable migration, production/API/UI code, credential/config, cloud resource or speculative abstraction was added.

## 5. Validation commands and results

Run from `/mnt/d/codex-call` on 2026-10-22. These are document/shell checks only; no PostgreSQL, Flyway, privilege, lock, savepoint, concurrency, Testcontainers, IVR, payment-provider, KMS or cloud execution was performed.

1. Structural script hash:
   `sha256sum /tmp/a0s1/structural-v2-run.sh` -> `ee2a0f17c8486250ee25fdbfae9196d53f8b5599be82e1bf17c8bc5f06b78545` (expected match).
2. Unchanged structural script:
   `FILE=docs/14-production-data-model-and-flyway-schema-spec.md FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md bash /tmp/a0s1/structural-v2-run.sh` -> exit `0`; `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24`.
3. Dictionary counts: independent awk check -> exit `0`, exactly 36 dictionaries and 428 column rows (S1 13/121, S2 23/307).
4. Exact column sets: independent catalog-vs-dictionary loop -> 36 `exact` lines and `ALL_EXACT`; S2 counts `4,17,7,6,21,11,12,23,17,22,11,14,15,11,13,10,8,14,16,12,19,6,18`.
5. Frozen blocks: independent extraction and `cmp` for `catalog`, `baseline-matrix`, `sensitive`, `fk-matrix`, `key-catalog`, `function-wave`, `slice-S1-tables`, `slice-S2-tables`, `slice-S3-tables` -> all identical; wave, invariant and open-input row sets identical.
6. Document checks: §13.1 status/sections/heading counts/terms, §13.5 S1 control coverage and §13.6 S2 control coverage -> pass (`S13_1_OK`, `S13_5_6_OK` in the author's shell run).
7. Whitespace/EOF: CR count `0`; trailing-whitespace lines `0`; terminal LF count `1`; `git diff --check -- docs/14-production-data-model-and-flyway-schema-spec.md` -> exit `0`; no `.sql` or `db/migration/` path in `git status`.
8. Git scope: only the specification is modified in this work unit and this artifact is the only new file; pre-existing dirty screenshots, `logo/preview.html`, the controller and `docs/reviews/plan-review-20261021-205118.md` are preserved. No staged path and no Git mutation.

## 6. Docs decision

The independent S2 review's fail-pending corrections F-01..F-16 and accepted OQ-1 are incorporated at the specification level, while the frozen baseline, exact dictionaries/columns and accepted S1 semantics are preserved. The document decision for this artifact is **fix-1 author-complete awaiting independent re-review**. This is not S2 acceptance, not an A0 checkpoint, not a PostgreSQL/Flyway/privilege/lock/concurrency verification and not production readiness.

## 7. Residuals

### fixed-current (document evidence)

| Item | Evidence |
| --- | --- |
| F-01..F-16 and OQ-1 corrections | Specification sections and tests in §2 above; structural/dictionary/frozen/whitespace checks pass. These are document assertions only. |
| Frozen baseline, exact columns, S1 semantics | Independent comparisons and the unchanged structural script; no S1 behavior rewrite. |
| Tool limitation | `apply_patch` is not available in this environment. The bounded edits were made with the available `Edit` tool (plus one in-place text replacement for stale section references) only in the two allowed files; no other interface, scope or claim is used. |

### covered-later-approved-slice

| Item | Owner / destination |
| --- | --- |
| S3 evidence/media/sharing dictionaries, transactions, deferred evidence cycle and final 44-table closure | S3 author / V008-V009 and S3 gate. S3 remains planned and is not claimed authored here. |
| Any later wire/JSON-schema version beyond §10.19 version 1 | IVR/Backend/Security; Q15/Q26 and later protocol/schema work. |

### assigned-later-work-unit

| Item | Owner / destination |
| --- | --- |
| Executable PostgreSQL 16 DDL, Flyway V001-V007, privilege/schema-USAGE tests, nested-owner Outbox test, lock/savepoint/category tests, concurrency/Testcontainers suites | Backend/Database/Platform; implementation, migration and deployment work units (Q17/Q23). No runtime test is claimed. |
| Runtime role provisioning (LOGIN/`SET ROLE`/pool mapping) for same-connection owner services | Platform/Database/Security; deployment-security design and executable privilege tests (OQ-2/Q17/Q23). |
| Allocation-policy code/version and request/per-call cap; approved due-installment/schedule windows; prices/quantities; prompt/policy content; authorization window; duration tolerance | Product/Finance/Operations/Entitlement/IVR/Backend; Q4/Q5/Q8/Q10/Q20/Q25 and Phase C/commercial/IVR acceptance work. Fix 1 defines validation and fail-closed behavior only. |
| IVR B-leg local authoritative key/persistence/lost-response recovery and line/media behavior | Telephony/IVR; Q28 and IVR protocol/line integration. |
| KMS/envelope/HMAC rotation, authentication/session design, legal retention/erasure | Security/Infrastructure/Legal/Platform; Q1/Q2/Q3/Q19/Q21 and later security/legal units. |

## 8. Coverage limitations

- Structural script output proves document structure/counts only; it does not prove PostgreSQL privileges, SQL semantics, lock behavior, savepoints, Flyway execution, concurrency or production behavior.
- The S2 test matrix (§13.7) is a document requirement list, not executed test code. No PostgreSQL instance, Testcontainers suite, IVR line, payment provider, KMS/cloud resource or production data was used.
- Independent re-review has not yet occurred. This artifact does not accept S2, does not advance the gate and does not authorize S3.
- The frozen catalog's historical "section 11" citation and "plus approved tolerance" phrase remain byte-identical; the normative corrections are explicit notes outside those blocks. The frozen 28 open-input rows remain unchanged; fix-1 inputs are named in prose and residuals rather than added as rows.
- Pre-existing modified QA screenshots, `logo/preview.html` and the unrelated old review are outside this work unit and were not changed. No `apply_patch` usage is claimed.

## 9. Completion statement

**Author-complete; independent re-review required.** No S2 acceptance, no S3 authoring, no Git stage/commit/push/PR/merge and no production/runtime test is claimed.
