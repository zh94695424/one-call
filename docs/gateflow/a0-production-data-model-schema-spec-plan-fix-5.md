# A0 Production Data Model and Flyway Schema Specification Plan — fix artifact (round 5)

- Task label: `a0-ivr-contract-fix5-ds-20261022-171600`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix` round 5
- Date: 2026-10-22
- Revised plan: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`
- Accepted inputs: `docs/reviews/plan-review-20261022-171403.md` (F-09/F-12 partial, F-13 new, F-10/F-11 and preserved PR/F states), the controller fix-round-5 adjudication and the user-approved shared IVR contract.
- Historical artifacts preserved unmodified: `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-4.md`, `fix-3`, `fix-2`, `fix`, all review artifacts and the controller.
- Binding scope: plan correction and this fix artifact only. No executable SQL/migration, Java/Spring code, API implementation, UI/prototype change, cloud resource, credential, deployment detail, final specification, commit, push, PR, merge, re-review or later gate was produced.

## 1. Final finding status

### 1.1 F-09 — task-backed workspace/creator authorization and lifecycle locks

- Status: **已修复**.
- Task claim now explicitly requires, for an organization task, `organization.status = 'ACTIVE'`, the creator `customer_account.status = 'ACTIVE'` and an `ACTIVE` membership whose current role grants task access; for a personal-owner task, the applicable owner and creator account rows must be `ACTIVE`. Subject/binding/activation/consent remain mandatory.
- The shared read/lock order names those rows in a fixed order after the caller binding and subject owner: organization, membership, owner account, creator account, with UUID tie-breaking; lifecycle writers (organization suspend/disable, account disable, membership revocation, binding disable, activation suspend/resume) must take the corresponding row or subject lifecycle lock before commit.
- The direct no-task IVR path applies no customer login, membership, owner-account or real-name predicate; unregistered/unverified/unowned callers remain eligible for direct activation/trial/recording.
- Direct plan evidence: D43, §10.1 read/lock order and step 4, `ivr.outbound_call_task` row, INV-13/INV-34, §11 tests, §13 S2 behavior/completion.

### 1.2 F-12 — `DIRECT_AUTHORIZATION` is a purpose, not an eighth action

- Status: **已修复**.
- Successful direct authorization now freezes `action = NULL` with stable `reasonCode = DIRECT_AUTHORIZED`, the committed `reservationId`, `authorizedRecordingSeconds`, `authorizationExpiresAt` and the confirmed callee only under `authorization.calleeNumber` inside the encrypted payload. Direct refusal uses one of the existing non-dial actions (`GUIDE_PURCHASE`, `REJECT`, `REJECT_SUSPENDED`, `REJECT_CALL_IN_PROGRESS`) with its existing stable reason and no reservation. `INBOUND_RESOLVE` and `ACTIVATION_NEXT_ACTION` always carry one of the seven non-null actions.
- The catalog row names the purpose/action/nullability CHECK; the typed table, write set, action-specific rules, sensitive inventory, INV-31/INV-34, replay rules and tests now agree. Direct success never populates task fields and never returns `ENTER_CALLEE` after the caller entered/confirmed a callee. No eighth action, table or function is added.
- Direct plan evidence: D44, `ivr.ivr_decision` and §8.1 rows, §10 direct state/write set and matrix note, §11 typed table/action rules, §13 S2 and tests.

### 1.3 F-13 — one unambiguous transaction-outcome contract

- Status: **已修复**.
- The plan now defines three categories everywhere: category 1 refusal before accepted consent/activation/trial (suspended, invalid confirmation origin/content, identity failure, phase-A other-call guard) writes no accepted side effect and never destructively finalizes a current `UNKNOWN`; category 2 normal domain no-dial result after valid activation/trial commits activation/trial, keeps the session `CONFIRMED`, commits one frozen no-dial decision/completion and no claim/reservation; category 3 technical failure rolls back every write including the `NEW` row, activation/trial and any tentative claim/reservation, commits no decision and returns a bounded retryable error with no swallowed database exception.
- An expected claim/reservation denial uses a same-connection savepoint established after activation/trial writes and before the tentative claim; rollback proceeds to that savepoint, then one no-dial decision is frozen. No autonomous transaction, independent service or independent commit is introduced.
- Pre-confirmation guards (phase A) and post-activation shared fresh selection (phase B) are explicit sequential phases of one locked transaction, not contradictory orders. Revocation tests assert the two serialized orders and never a mid-lock interleaving.
- Direct plan evidence: D45, §10.1 outcome contract and ordered set, activation-confirmation state row, INV-08/INV-13/INV-19/INV-37, sequences 1/3/4, tests and S2 assertions.

### 1.4 Related contract clarifications

- Status: **已并入** (inside D46 and the referenced sections).
- Consent gap: an `ACTIVE` activation with missing/expired current consent returns `GUIDE_ACTIVATION` with stable `CONSENT_REQUIRED` on initial resolve; the confirmation path appends current consent and reuses the existing trial. Stale/invalid confirmation content is rejected with `CONSENT_REQUIRED` before accepted side effects.
- `taskMatch = NONE` now means no candidate was selected/evaluated; it is not proof that no task exists. `ENTER_CALLEE` and direct authorization require a positive no-`PENDING`/`CLAIMED`-task check; task fields exist only for an evaluated/claimed candidate.
- The durable unique B-leg operation is stated to live in the existing IVR platform's local authoritative store, keyed by `callId` and persisted before the line call; business `ivr.call_leg` proves no local outbound uniqueness. Executable local key/protocol is deferred to the IVR work unit with no business schema expansion.
- A current `UNKNOWN` call replay precedes any mutation; an inconsistent fresh attempt returns `REJECT`/`CALL_STATE_UNRESOLVED` with no reservation/dial/release and no `UNKNOWN`-to-`REJECTED` change. Only never-started/rejectable current `ACTIVE` rows may become `REJECTED`; a competing call is untouched.
- The default pending-task TTL is 30 seconds, server-configurable, frozen per task; duplicates never refresh it and no production configuration key is invented.
- Initial-resolve replay precedes task-expiry housekeeping or any other mutation, preserving the frozen first result.

### 1.5 Preserved finding states

- F-10 and F-11 remain **已修复** as accepted in `docs/reviews/plan-review-20261022-171403.md`; the round-5 edits do not revert their evidence.
- PR-001..PR-014 and F-01..F-08 remain **已修复** with no regression; the exact structural script is unchanged and green.
- No table, invariant, migration wave, slice, module or SQL function was added, removed, re-membered or reordered.

## 2. Changed locations in the revised plan

| Area | Change |
| --- | --- |
| Header/scope (§1-§3) | Fix-round-5 label, inputs, fix-5 artifact, preserved history and unchanged baseline |
| Success signal (§2) | Lifecycle predicates, purpose-discriminated direct success, outcome contract, consent-gap/`UNKNOWN`/TTL/replay-order clarifications |
| Design alignment (§4) | Direct purpose result, lifecycle access, IVR-local B-leg, transaction categories |
| Decisions (§7) | D23/D39 revised; D43/D44/D45 rewritten; D46 added for the related clarifications |
| Table catalog/sensitive (§8, §8.1) | Organization/account claim predicates, TTL default, `UNKNOWN` preservation, purpose/action CHECK and callee placement |
| Invariants (§9) | INV-08, INV-12, INV-13, INV-19, INV-31, INV-34 and INV-37 updated in place; count remains 40 |
| State machines/transactions (§10) | Activation-confirmation phases, direct/`UNKNOWN`/reservation rows; §10.1 lock order, predicate set, matrix, `taskMatch`/direct notes, write set, outcome contract, scenarios and sequences 1/3/4 |
| Contract/tests (§11) | Published reservation denial, shared-service returns, typed table/rules, B-leg local-store rule, privilege/atomicity and semantic assertions |
| Slices/validation/docs/risks (§13-§18) | S2 assertions, fix-5 paths and terms, Q18/Q27/Q28, residual risks and over-design statement |

## 3. Preserved baseline

| Dimension | Value |
| --- | --- |
| Tables | 44 |
| Invariants | 40 |
| Migration waves | 9 (V001-V009; table distribution 3/6/4/7/6/7/3/5/3) |
| Slices | 3 (S1 13, S2 23, S3 8; no fourth slice) |
| Lifecycle classes | 28 mutable-head + 16 append-only |
| Sensitive-covered tables | 28 |
| FK decisions | 81 (74 `FK`, 3 `ALTER_FK`, 2 `DEFERRABLE_FK`, 2 `NO_FK`) |
| Function contracts | 5 (4 V001 platform/audit + 1 V006 first-trial); no additional SQL function |

## 4. Exact validation commands and results

Run from `/mnt/d/codex-call`. The embedded `structural-script-v2` was not edited or weakened.

### 4.1 Extract and run the unchanged structural script

```bash
FILE=docs/gateflow/a0-production-data-model-schema-spec-plan.md
FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-5.md
export FILE FIX2
awk '/^# structural-script-v2$/{f=1} f{print} /^# end structural-script-v2$/{exit}' "$FILE" > /tmp/a0-structural-v2.sh
bash /tmp/a0-structural-v2.sh
```

- Exit: `0`.
- Output:

```text
fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81
catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24
```

### 4.2 Gate preconditions and whitespace

- `sections=18`, `slices=3`, `waves=9`, `trailing_ws=0`; fix-5 references present and fix-4 remains only as preserved history.
- Plan and fix artifact non-empty; no executable SQL/migration file was created.

### 4.3 Semantic spot checks

- Scanned for the old blanket rollback wording (`any failed predicate`, `any failure before commit`, blanket `any failure rolls back consent`) and for misuse of `action = DIRECT_AUTHORIZATION`: none.
- Confirmed all of: organization/account lifecycle predicates, `ORGANIZATION_INACTIVE`/`ACCOUNT_DISABLED`, `authorization.calleeNumber` (task fields excluded for direct), category-1/2/3 outcome language, same-connection savepoint, `CONSENT_REQUIRED`, `CALL_STATE_UNRESOLVED`, default 30-second non-refreshing TTL and replay-before-expiry-housekeeping.

These are document/semantic checks only. No executable PostgreSQL, Testcontainers or IVR-line behavior is claimed.

## 5. Residual risks, owners and destinations

| Risk / uncovered area | Owner and destination |
| --- | --- |
| No executable Flyway migration or Testcontainers suite at this plan gate; lifecycle predicates/locks, direct typed contract, category outcomes and savepoint behavior are planned assertions only | IVR/Activation/Entitlement/Organization-access/Backend; implementation migration and concurrency suite |
| IVR-local durable B-leg operation key/protocol and lost-response recovery are not implemented in the business schema | Telephony/IVR; IVR protocol and line integration |
| Exact organization role matrix, call-blocking risk rule, consent wording, TTL configuration key and authorization window remain unapproved inputs | Product/Security/Organization-access/IVR/Backend; Q18/Q24/Q25/Q27/Q28 destinations |
| Caller-only matching, number recycling, retention/legal and cloud/SIP/provider choices remain prior launch-blocking or deferred residuals | Product/Telephony/Risk/Legal/Security/Infrastructure; unchanged destinations |
| No final specification `docs/14-production-data-model-and-flyway-schema-spec.md` or production behavior is produced here | Future implementation gate |

## 6. Allowed files, scope and Git status

- Modified: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`.
- Created: `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-5.md` (this artifact).
- No controller, prior fix/review artifact, source doc, code, prototype, QA screenshot, `logo/preview.html`, `.codex`, credential or Git metadata was modified.
- No file was staged, committed, pushed, merged or opened as a PR; no re-review, implementation or later gate was entered.

## 7. Completion report

- Task label: `a0-ivr-contract-fix5-ds-20261022-171600`; gate: fix round 5.
- Artifacts: revised plan and this fix-5 report.
- Status: F-09 **已修复**; F-12 **已修复**; F-13 **已修复**; related clarifications **已并入**; F-10/F-11, PR-001..PR-014 and F-01..F-08 preserved.
- Validation: exact embedded structural script exit `0` with the baseline summaries; 18 sections, 3 slices, 9 waves, no trailing whitespace; semantic contradiction scans clean.
- No later gate entered; stop at fix completion.
