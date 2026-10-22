# A0 Production Data Model and Flyway Schema Specification Plan — fix artifact

- Task label: `a0-plan-fix-claude-ds-20261022-01`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix`
- Date: 2026-10-22
- Revised plan: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`
- Accepted review: `docs/reviews/plan-review-20261022-113039.md`
- Controller: `docs/gateflow/project-execution-control.md`
- Scope: plan correction only; no final specification, executable migration, re-review, commit, push, PR, merge or later gate was entered

## 1. Fix summary

All 14 accepted findings (PR-001 through PR-014) are fixed in the revised plan. No accepted finding is unresolved.

The plan now uses a dependency-ordered three-slice graph that is acyclic and testable on each slice's own and earlier waves:

- S1 = V001-V003, platform/identity/subject foundation, 13 tables.
- S2 = V004-V007, commerce + IVR + entitlement + activation transaction core, 23 tables.
- S3 = V008-V009, evidence + sharing closure, 8 tables.

The controller-authorized 44th table is added as `entitlement_metering.entitlement_issuance`. It owns durable per-order, per-installment and per-entitlement-type issuance state, attempts, revision, error and grant linkage. The former order-level entitlement status is not stored in `catalog_order.customer_order`; order-level entitlement display is a computed projection from issuance rows.

Baseline after the fix:

| Dimension | Before | After | Derivation |
| --- | --- | --- | --- |
| Tables | 43 | 44 | +`entitlement_metering.entitlement_issuance` (PR-004) |
| Invariants | 35 | 40 | +INV-36 durable issuance state; +INV-37 activation replay/operations event; +INV-38 reservation reconciliation; +INV-39 share-ticket reconstruction; +INV-40 cross-schema function contracts |
| Migration waves | 9 | 9 | Unchanged; table distribution V001-V009 is 3/6/4/7/6/7/3/5/3 |
| Slices | 3 | 3 | Repartitioned to S1 13 / S2 23 / S3 8 |
| Invariants by verified slice | n/a | 14 / 21 / 5 | S1 / S2 / S3 |
| Lifecycle classes | 2 classes | 27 mutable + 17 append-only | Every table has exactly one class |
| Sensitive-covered tables | n/a | 28 `Y` in the baseline matrix | Every `Y` table is in section 8.1 |
| FK decisions | n/a | 81 rows: 75 FK, 4 deferred FK, 2 no-FK | Every call/session/task/reservation/evidence link has a row |

The counts are derived from the catalogs and matrices, not asserted as a scope freeze. Every added table and invariant maps to an accepted finding and a confirmed goal condition.

## 2. PR-001 through PR-014 final status

| Finding | Final status | Plan change | Direct evidence | Validation evidence |
| --- | --- | --- | --- | --- |
| PR-001 slice order and forward dependencies | 已修复 | Slices reordered and repartitioned: S1 = V001-V003, S2 = V004-V007, S3 = V008-V009. S1 no longer owns activation (V007) or trial/IVR (V005-V006); S2 no longer consumes a frozen S3 catalog/order contract; S3 consumes only S1/S2. Dependency table states `none`, `S1 only`, `S1, S2 only`. | Review lines 33-38 and 237; source `docs/12` line 295 (consent/activation/first trial in one transaction) and `docs/13` §6.3. Plan §13 slices, D20, dependency summary; §14.1 no-later-slice check. | Exactly 3 slice headings; matrix slice counts 13/23/8; S1 table waves all V001-V003; S2 V004-V007; S3 V008-V009; no later-slice prerequisite text. |
| PR-002 activation transaction crosses module ownership and lacks grant/balance writes | 已修复 | Added D22 and INV-08: activation calls published `entitlement-metering.issueFirstTrial` with `Propagation.MANDATORY`; the service alone writes `trial_grant`, `entitlement_grant`, `entitlement_balance` and Outbox; replay returns the first grant; privilege and rollback tests are named. | Review lines 42-51; source `docs/12` line 295 (one transaction for consent, activation and first trial), `docs/09` line 84 (trial frozen at issuance), `docs/12` line 296 (reservation from valid entitlement). Plan D22, INV-08, §10 sequence 1, §11 function contract and tests. | INV-08 names `issueFirstTrial`, same-transaction propagation and rollback; four function-contract tests are required; S2 contains activation and entitlement tables. |
| PR-003 direct DTMF authorization lacks caller lock and no-task proof | 已修复 | Added D23, INV-11, INV-34: direct authorization acquires the canonical-caller lock, requires the session's `ENTER_CALLEE` no-task path, rejects a task ID, rechecks no `PENDING/CLAIMED` task and no other `ACTIVE/UNKNOWN` call, creates or binds `call_record ACTIVE` and one reservation. | Review lines 55-64; source `docs/13` lines 93-98 (lock on direct session establishment), 208 (transaction order), 219-220 (`ENTER_CALLEE` no-task), 228-232 (direct endpoint rules). Plan D23, INV-11, INV-34, §10 direct-authorization row and sequence 4. | Direct sequence contains lock, `ENTER_CALLEE`, no-task, no-active-call and active-guard creation; `call_event`/`call_record` active partial unique is specified; concurrency tests are named. |
| PR-004 issuance state has no durable owner; order status cannot represent lines/installments | 已修复 | Added 44th table `entitlement_metering.entitlement_issuance` with order, installment, type, status, attempt count, revision, error, grant link, source event and timestamps; unique `(order_id, installment_no, entitlement_type)`; state machine `PENDING/FAILED/ISSUED`; removed the order-level entitlement status; added INV-36 and multi-line/partial-failure tests. | Review lines 68-77; source `docs/09` line 86 (payment and entitlement separate), `docs/10` lines 22 and 46 (issuance states and independent arrival), `docs/12` lines 328 and 368 (independent observable states; unique by order/installment/type). Plan D21, table row, INV-23/36, §10 issuance state machine and sequence 8. | 44 catalog rows; `entitlement_issuance` appears once in the matrix at V006/S2; no `entitlement_status` token remains; INV-36 names multi-line/partial-failure tests. |
| PR-005 event/action identity not unique within its domain | 已修复 | `ivr.call_event` now stores `event_domain` with check `IVR_EVENT` and unique `(event_domain, event_id)`; `ivr_decision` uses `(call_id, source_event_domain, source_event_id)`; `usage_fact` stores source event domain and unique `(source_event_domain, source_event_id)`; `consent_record`, `evidence_event` and `share_access_event` have domain-scoped identity; the same string across domains remains allowed. | Review lines 81-90; source `docs/13` lines 174 and 243 (call/session idempotency; each event ID unique in the IVR event domain), `docs/10` line 49 (same string across domains; unique within a domain). Plan D24, INV-14/35, table rows, §11 event-domain registry. | INV-35 names composite unique constraints on the affected tables; event-domain registry lists `IVR_EVENT`, `IVR_DIRECT_AUTH`, `EVIDENCE`, `PAYMENT`, `SHARE_ACCESS`, `ACTIVATION`, `IVR_CONFIRM`, `ACTIVATION_STATUS`, `OPERATIONS`, `PURCHASE`, `SUBJECT_CLAIM`. |
| PR-006 activation confirmation lacks fingerprint/first result; operations event undefined | 已修复 | Added D25 and INV-37: activation uses `platform.idempotency_begin('IVR_CONFIRM', eventId, requestFingerprint)` and the stored first result; `consent_record.request_fingerprint` and `service_activation_transition.request_fingerprint` are required; `source_action_event_id` is NOT NULL for all domains, including `OPERATIONS`; unique per action domain; operations must supply its audit/request event identity. | Review lines 94-103; source `docs/10` line 49 (atomic rejection of changed input; first immutable result), `docs/13` line 241 (`eventId + request fingerprint`), `docs/09` line 82 (`OPERATIONS` transitions with revisions). Plan D25, INV-09/37, table rows, §10 sequence 1, §11 idempotency scope. | Both table rows carry the fingerprint; transition requires non-null action event; INV-37 names the platform scope and replay tests; the activation sequence returns the first result. |
| PR-007 call_id chain parents, creation timing and waves unspecified | 已修复 | Added D26 and a complete FK catalog. `ivr.call_record.call_id` is the canonical call-scoped parent with a unique protocol identity; `resolve` and direct authorization create/bind it with `ACTIVE` status before any decision/reservation. `inbound_session.call_id` is the root no-FK protocol identity. Every call/session/task/reservation/evidence link has an exact parent, FK/deferred-FK/no-FK decision and wave. | Review lines 107-116; source `docs/13` lines 253-269 (task → call → legs → reservation → recording → evidence), `docs/12` line 356 (call association truth). Plan D26, INV-15, FK matrix, §10 sequences 3/4/9, §12 intra-wave order. | FK matrix contains all required links, including `ivr.call_record.call_id`, `call_record.session_id/task_id`, `call_leg/call_event/ivr_decision.call_id`, `ivr_decision.reservation_id`, `usage_reservation/usage_fact.call_id`, `recording.call_id`, `evidence_record.call/session/reservation_id` and `consent_record.ivr_session_id`; parent wave never exceeds FK wave. |
| PR-008 new campaign key can bypass permanent first trial | 已修复 | Added D27: `trial_policy_version.campaign_key` and `trial_grant.campaign_key` are fixed to `FIRST-TRIAL`; `trial_grant` has unique `subject_id` (permanent), not merely `(subject_id, campaign_key)`. | Review lines 120-129; source `docs/10` line 20 (`subjectId + FIRST-TRIAL` permanently unique), `docs/09` line 84 (trial unique by subject/campaign and frozen), `docs/09` line 64 (no reissue on re-entry/version change). Plan D27, INV-16, table rows. | INV-16 says one permanent FIRST-TRIAL per subject; table rows show the check and unique `subject_id`; INV-16 covers new campaign keys, policy versions, claims and imports. |
| PR-009 evidence creation ownership is one-way only | 已修复 | Added D28: `evidence_event` partial unique `(evidence_id)` for `EVIDENCE_CREATED`; `evidence_record.source_event_id` has a deferrable FK to `evidence_event.event_id`; a deferred constraint trigger checks at commit that exactly one creation event exists, that it is `EVIDENCE_CREATED`, and that its `event_id` equals the record's source event ID; insert is atomic. | Review lines 133-142; source `docs/09` line 91 (each evidence owned by exactly one `EVIDENCE` event; bidirectional coverage), `docs/10` line 49 (result identity and reciprocal coverage). Plan D28, INV-25, table rows, §10 sequence 9, FK matrix. | INV-25 states reciprocal ownership and atomic insertion; the FK matrix row is `DEFERRED_FK`; the partial unique and deferred trigger are named; tests for duplicate/mismatched/missing creation events are required. |
| PR-010 Redis ticket failure/retry contract missing | 已修复 | Added D29 and INV-39: the durable admission is the authorization fact; the ticket key is derived from `share_grant_id + admission_id`; same `admission_key` retry returns the existing admission and reissues the ticket without incrementing; `TICKET_ISSUED`/`TICKET_FAILED` access events record the handoff; Redis loss is reconstructible. | Review lines 145-153; source `docs/12` lines 335-338 (admission count once; repeated submission returns original session), line 360 (Redis reconstructible), line 424 (Redis outage failure-closed but durable state recoverable). Plan D29, INV-26/39, share tables, §10 sequence 10. | INV-39 names unique admission key, derived ticket key, access events and Redis-failure/lost-cache tests; sequence 10 states the same-key reissue and no second count. |
| PR-011 reservation recovery for missing end events absent | 已修复 | Added D30: reservation states `HELD → SETTLED/RELEASED`, `HELD → RECONCILING`, `RECONCILING → SETTLED/RELEASED`; fields `reconciliation_due_at`, `reconciliation_attempt_count`, `last_reconciled_at`, `authority_source`, `last_error_code`; owned recovery sequence queries authoritative IVR use, settles known use first, releases the remainder once, keeps unknown use `RECONCILING`, alerts and blocks a new reservation; added INV-38 and missing/late/duplicate tests. | Review lines 157-165; source `docs/12` line 298 (expired reservation must settle known use, keep unknown occupied, alert), `docs/13` line 271 (crashed claimed task reconciled by `callId`; unknown stays occupied). Plan D30, INV-19/38, reservation row and state machine, §10 sequence 6. | INV-38 states unknown use is never released; the row carries the reconciliation fields; sequence 6 covers missing, late and duplicate facts; S2 verification includes the recovery path. |
| PR-012 validation only checks counts, not structure/FK order | 已修复 | Added a machine-readable baseline matrix, a complete FK matrix and a deterministic structural script in §14.1. The script reports duplicate, missing and extra tables; checks one valid wave/slice/lifecycle/sensitive flag per table; checks slice/wave binding; checks sensitive coverage; checks 40 unique invariants with one owner and DB/TX/SERVICE enforcement; checks FK field shape, parent wave order, known child/parent tables and all required links. | Review lines 168-177; confirmed success signal (one owner/wave/slice, explicit FKs, deterministic validation); plan §8.3, §12 FK catalog, §14.1 script. | The plan contains 44 catalog rows, 44 matrix rows, 44 unique matrix names, 13/23/8 slice counts, 27/17 lifecycle counts, 28 sensitive `Y` tables, 40 invariants with 14/21/5 slice verification, 79 FK rows plus 2 no-FK decisions and 4 deferred FKs. The embedded script is the binding reproducible check for the final specification. |
| PR-013 catalog_product lifecycle contradiction | 已修复 | Added D31: `catalog_product` is immutable append-only identity with no draft status and no product draft-delete exception. Only a never-published, unreferenced `catalog_version` draft is a future deletion candidate, with no deletion in the baseline and separate future design. | Review lines 181-189; source `docs/12` line 377 (only never-published, unreferenced drafts may be a future deletion candidate), `docs/09` line 130 (new version drafts, publish/offsale; history unchanged). Plan D31, INV-20/30, `catalog_product`/`catalog_version` rows, §8.2 deletion text. | `catalog_product` is `APPEND_ONLY` in the matrix; the deletion exception names only `catalog_version`; no product draft-delete path remains. |
| PR-014 cross-schema function contracts not code-generation-ready | 已修复 | Added D32 and INV-40: exact specification contracts for `platform.idempotency_begin`, `platform.outbox_append`, `operations_audit.append_fact` and `entitlement_metering.issue_first_trial`, including owner, `SECURITY DEFINER`, fixed `search_path`, EXECUTE grants, no transaction control, exception-propagating rollback, first-result/replay semantics, redaction/encryption and privilege/atomicity tests. | Review lines 193-201; source `docs/12` line 242 (no direct cross-module table writes; published services), lines 366-372 (single local transaction, Outbox, idempotent consumers, row locks, external unknown results). Plan D32, INV-27/28/29/40, §11 function contracts and test list. | All four functions have arguments, returns, owner/mode, search path, grants, no-commit rule and replay semantics; the test list covers privilege, atomicity, replay, redaction and `search_path`; S1 verifies the contracts. |

Status totals: 已修复 14, 部分修复 0, 未修复 0, 证据失效 0.

## 3. Slice dependency proof

| Slice | Waves | Table count | Tables | Invariants verified | Later-slice prerequisites |
| --- | --- | --- | --- | --- | --- |
| S1 | V001-V003 | 13 | `platform.idempotency_record`, `platform.outbox_event`, `audit.audit_fact`, `customer_iam.customer_account`, `customer_iam.auth_identity`, `customer_iam.identity_profile`, `organization_access.organization`, `organization_access.organization_membership`, `operations_iam.employee_account`, `subject_number.business_subject`, `subject_number.caller_number_binding`, `subject_number.subject_claim`, `subject_number.subject_risk_state` | INV-01..07, INV-27..31, INV-33, INV-40 (14) | none |
| S2 | V004-V007 | 23 | `catalog_order.*` (7), `ivr.*` (6), `entitlement_metering.*` (7), `activation_consent.*` (3) | INV-08..14, INV-16..23, INV-32, INV-34..38 (21) | none; consumes S1 only |
| S3 | V008-V009 | 8 | `evidence.*` (5), `sharing.*` (3) | INV-15, INV-24..26, INV-39 (5) | none; consumes S1 and S2 |

The graph is acyclic: S1 → S2 → S3. Every FK parent is in the same or an earlier wave, and the child slice is the same as or later than the parent slice. The only deferred cross-wave FKs are `ivr.ivr_decision.reservation_id` (V006), `catalog_order.customer_order.purchase_consent_id` (V007), `ivr.outbound_call_task.claimed_call_id` (same-wave V005) and `evidence.evidence_record.source_event_id` (same-wave V008, deferrable).

## 4. Exact validations executed

The following assertions were executed against `docs/gateflow/a0-production-data-model-schema-spec-plan.md` and the fix artifact with repository-local tools. All passed.

| Check | Command / evidence | Outcome |
| --- | --- | --- |
| Both artifacts non-empty | `wc -l` on plan and fix artifact | plan 942 lines; fix artifact non-empty |
| Required plan sections | `rg '^## [0-9]+\.'` | 18 of 18 numbered sections present |
| Slice count | `rg -c '^### Slice S[1-3]'` | 3 |
| Wave count | wave table row count | 9 |
| Catalog table rows | catalog rows matching the wave and slice columns | 44 |
| Baseline matrix rows | baseline matrix row count | 44 |
| Unique matrix table names | matrix table names after sort and unique | 44 |
| Slice matrix counts | S1/S2/S3 regex counts | 13 / 23 / 8 |
| Lifecycle counts | mutable/append regex counts | 27 / 17 |
| Sensitive `Y` tables | matrix regex | 28 |
| Invariant rows | invariant row count | 40 |
| Invariants with DB/TX/SERVICE | regex count | 40 |
| Invariant slice counts | S1/S2/S3 regex counts | 14 / 21 / 5 |
| Multi-owner table or invariant rows | owner-cell regex | none |
| FK decisions | matrix rows | 79 FK/DEFERRED + 2 NO_FK = 81 |
| Deferred FK rows | `rg 'DEFERRED_FK'` | 4 matrix rows |
| Required call/session/task/reservation/evidence links | required-link list in §14.1 | all present |
| Deferred FK assignment | `purchase_consent_id`/`V007`; `reservation_id`/`V006` | both present |
| No later-slice prerequisite | character-class regex from §14.1 | no output |
| No stale 43-table or 35-invariant counts | stale-count scan | no output |
| No `entitlement_status` token | `rg 'entitlement_status'` | no output |
| Trailing whitespace | `rg '[ \t]+$'` with Grep tool | no matches |
| Implementation path absent | no `docs/14-...`, `.sql` or `db/migration` write performed | pass |
| Scope | only the two allowed artifacts were written by this task | pass |

The full deterministic script is embedded in plan §14.1. This gate executed its constituent assertions with the commands above; the final specification must run the unchanged script with `FILE=docs/14-production-data-model-and-flyway-schema-spec.md`.

## 5. Residual risks and blockers

No accepted finding is a blocker. The following residual risks remain assigned and are not solved by a plan-only gate:

- No executable Flyway migration, PostgreSQL constraint test or Testcontainers concurrency test exists at this gate. Owner: Backend; destination: implementation gate.
- The `SECURITY DEFINER` function contracts still depend on environment-specific role provisioning, ownership transfer and security review. Owner: Platform/Database/Security; destination: security design and production readiness (Q17/Q23).
- Installment due-date timing, grace/retry policy and scheduler cadence for future `entitlement_issuance` rows are not approved. Owner: Product/Finance/Backend; destination: entitlement issuance design (Q20).
- Reconciliation scan cadence, alert thresholds and manual adjustment approval are not approved. Owner: Operations/Backend/IVR; destination: operations runbook (Q21).
- Redis playback-ticket TTL, clock skew and revocation propagation are not approved. Owner: Security/Sharing; destination: security design and sharing policy (Q22).
- The source-level launch-blocking risks (caller-only matching, number recycling, real-line/payment/COS validation) remain unchanged and are recorded in plan §16.

## 6. Scope statement

This gate modified `docs/gateflow/a0-production-data-model-schema-spec-plan.md` and created `docs/gateflow/a0-production-data-model-schema-spec-plan-fix.md`. It did not modify the controller, the accepted review, `docs/09` through `docs/13`, source code, prototypes, QA screenshots, Git metadata or credentials. It did not stage, commit, push, create a PR, merge, re-review, implement the final specification or enter any later gate.
