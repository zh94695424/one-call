# A0 Production Data Model and Flyway Schema Specification Plan — fix artifact (round 4)

- Task label: `a0-ivr-contract-fix4-ds-20261022-165224`
- Work unit: `A0-production-data-model-schema-spec`
- Gate: `fix` round 4
- Date: 2026-10-22
- Revised plan: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`
- Accepted inputs: `docs/reviews/plan-review-20261022-133903.md` (F-09/F-10 and preserved PR/F states), `docs/reviews/plan-review-20261022-140542.md` (F-09..F-12), the controller fix-round-4 requirements and the user-approved unified IVR action-selection contract refinement.
- Historical artifacts preserved unmodified: `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-3.md`, `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-2.md`, `docs/gateflow/a0-production-data-model-schema-spec-plan-fix.md`, all review artifacts and the controller.
- Binding scope: plan correction and this fix artifact only. No executable SQL/migration, Java/Spring code, API implementation, UI/prototype change, cloud resource, credential, deployment detail, final specification, commit, push, PR, merge, re-review or later gate was produced.

## 1. Final finding status

### 1.1 F-09 — post-activation DIAL branch authorization, access and claim predicates

- Status: **已修复**.
- The plan now selects `DIAL_TASK_CALLEE` only when the task is `PENDING`; the caller, subject, caller binding and owner workspace match the task; the creator still has current task-level access on the task's owner workspace/subject; the expected revision matches; the activation is `ACTIVE` with the required current consent; the effective entitlement has positive, nonexpired available units; and `expires_at > clock_timestamp()` is re-evaluated in the actual conditional claim `UPDATE`, not only in an earlier read.
- The same transaction performs the conditional claim, current `ACTIVE` call binding, reservation through `entitlement-metering.reserve_call_usage`, frozen typed decision and Outbox/audit. A failed claim or reservation rolls the claim and reservation back together; no `CLAIMED` task can commit without its reservation, no second reservation is authored and no arbitrary second task is selected.
- Revocation cannot race the claim: the claim holds the canonical-caller and subject lifecycle locks and the access rows described in section 10.1; binding disable/revocation, membership revocation, owner change and activation lifecycle writers take the corresponding row/subject lock before commit.
- With zero available entitlement the action is `GUIDE_PURCHASE` with no claim and no reservation; `ENTER_CALLEE` is used only when no valid task remains and available balance is positive. Expiry/cancel/replacement may fall through as a no-task input; access/binding/integrity failures reject stably and never fall through to direct dialing.
- Approved risk semantics are preserved: `subject_risk_state` `OPEN` blocks number claims and historical-evidence access, not automatically all future dialing; no blanket dialing ban or platform-registration/real-name requirement for IVR trial is introduced. Any future call-blocking risk rule is a named deferred policy input (Q24).
- Direct plan evidence: D40-D43/D45; `outbound_call_task`, `call_record` and `usage_reservation` rows in section 8; INV-11/INV-13/INV-19/INV-34; activation-confirmation, outbound-task and call-record state rows in section 10; section 10.1 ordered predicate set and matrix; sequences 1, 3 and 4; section 11 semantic assertions; S2 behavior and completion assertions in section 13.

### 1.2 F-10 — different event ID, call/purpose uniqueness and SUSPENDED-after-confirmed

- Status: **已修复**.
- Replay is handled before confirmation side effects and before the current `SUSPENDED` status is rejected. The IVR service authenticates the service identity and validates immutable `callId`, session, caller and access identity before disclosing any stored result.
- Canonical semantic fingerprints are defined per purpose. The confirmation fingerprint includes `callId`, session/subject/binding identity, immutable caller/access identity, prompt version, consent policy/version, confirmation key/content and excludes transport `eventId`, timestamps and current mutable balance/status/revision. A regenerated `eventId` for the same semantic confirmation matches; the same event reused on another call conflicts because `callId` is in the fingerprint.
- Under the canonical-caller/call lock, the wrapper handles same-event `REPLAY`/`CONFLICT`, then reads existing `(callId, ACTIVATION_NEXT_ACTION)`. A new event ID with the same fingerprint returns the existing decision and frozen action and completes that event's `NEW` row with the existing decision ID without repeating consent/trial/claim/reservation/Outbox. A changed fingerprint rolls back the transaction (discarding any `NEW` row) and returns a stable conflict.
- Existing-result handling precedes the `SUSPENDED` check. A `CONFIRMED` session is never changed to `REJECTED`, and a `CONFIRMED` session without its activation decision fails closed as `DATA_INTEGRITY`. A fresh `POST_ACTIVATION` confirmation is allowed only when the original `(callId, INBOUND_RESOLVE)` decision is `GUIDE_ACTIVATION`, so an already-authorized initial task dial cannot be followed by an arbitrary extra confirmation.
- Direct plan evidence: D10/D25/D41; `platform.idempotency_record` and `ivr.ivr_decision` rows in section 8; INV-08/INV-10/INV-14/INV-27/INV-35/INV-37; activation-confirmation and IVR-decision state rows; section 10.1 fingerprint and precedence rules; sequence 1; section 11 replay/conflict assertions; S2 tests and completion in section 13.

### 1.3 F-11 — stable active/unknown-call branch

- Status: **已修复**.
- Both fresh decision paths run one ordered guard under the canonical-caller lock: another `ACTIVE` or `UNKNOWN` call for the same canonical caller is rejected with stable `REJECT_CALL_IN_PROGRESS` when its `call_id` differs from the current call. The current `callId` is always excluded, so a call never blocks itself.
- The rejection writes no task claim and no reservation. The current call gets a valid non-active `call_record` parent: if no current row exists, it is created terminal `REJECTED` with `end_reason = CALL_IN_PROGRESS`; if the current row is live and a later decision rejects, only that current row is finalized. An unrelated competing call's guard/decision is never changed.
- The partial unique active-caller index remains a backstop. A unique conflict is caught, rolled back and converted by one bounded deterministic reread to `REJECT_CALL_IN_PROGRESS` or a stable conflict; a raw unique violation is never the protocol response.
- Direct plan evidence: D26/D42; `ivr.call_record` and `ivr.ivr_decision` rows in section 8; INV-11/INV-14/INV-34; Call record and Direct authorization state rows; section 10.1 scenarios; sequences 1/3/4; section 11 active-call assertions; S2 completion in section 13.

### 1.4 F-12 — typed immutable action-specific response

- Status: **已修复**.
- The plan defines a typed snapshot with `decisionId`, `callId`, `purpose`, `action`, `callerState`, `taskMatch`, `availableRecordingSeconds`, `balanceAsOf`, task fields (`taskId`, `businessType`, callee boundary), `reservationId`, `authorizedRecordingSeconds`, `authorizationExpiresAt`, `reasonCode` and `decisionAt`, including explicit nullability and the stable `callerState`/`taskMatch`/reason registries.
- Authorization values come only from the committed reservation. Baseline recording units are seconds (1 unit = 1 second), so `authorizedRecordingSeconds` is an explicit identity conversion of `usage_reservation.authorized_units`; `authorizationExpiresAt` is copied from the reservation and means the last instant to start the B-leg, not the end of a running call.
- The full callee exists only in `response_payload_ciphertext` for the authenticated IVR internal response; the typed `response_snapshot`, logs, audit and customer responses contain the masked callee. `response_sha256` is over the canonical redacted snapshot, and the encrypted payload's redacted projection must equal it.
- Replay decrypts the same immutable payload and returns the same logical result. It never renews the cutoff and is never redial permission. The IVR persists one unique `callId` B-leg operation before dialing, never prompts the task-path caller to confirm the callee and never redials on a lost response or unknown line outcome.
- Direct plan evidence: D44; `ivr.ivr_decision`, `ivr.call_record` and `usage_reservation` rows in section 8; section 8.1 sensitive row; INV-31/INV-37; section 11 typed snapshot table and B-leg rules; section 10.1 scenarios; section 11 semantic assertions; S2 completion in section 13.

### 1.5 Unified IVR-owned application-service contract

- Status: **已修复**.
- One small IVR-owned application service, `ivr.select_call_action`, serves `INITIAL_RESOLVE` (purpose `INBOUND_RESOLVE`) and `POST_ACTIVATION` (purpose `ACTIVATION_NEXT_ACTION`). The public endpoints and immutable decision purposes remain separate; `ivr.record_activation_next_action` is a thin entry wrapper that delegates to the shared service and contains no duplicate predicates.
- The service is an application service inside the existing local IVR transaction. It is **not** an additional SQL `SECURITY DEFINER` function, adds no microservice, workflow engine, generic framework, table or slice, and preserves per-module ownership.
- Entry-mode/purpose mapping, input identity, authoritative reads/locks, ordered predicates, write sets, return type and rollback are stated in section 10.1 and section 11. The initial-resolve and confirmation fingerprints are distinct and are never compared to each other.
- Direct plan evidence: D40-D45; section 10.1; the `ivr.select_call_action` and `ivr.record_activation_next_action` rows in section 11; INV-14/INV-37/INV-40; S2 behavior/completion in section 13.

### 1.6 Preserved finding states

- PR-001 through PR-014 remain **已修复** as independently accepted in `docs/reviews/plan-review-20261022-133903.md`; the round-4 changes do not alter their evidence or the slice graph.
- F-01 through F-08 remain **已修复** as independently accepted in the same review. F-07's structural-script repair is preserved unchanged; no check was deleted or weakened.
- No prior accepted correction was reverted. The plan adds no table, invariant, migration wave, slice or scope domain.

## 2. Changed locations in the revised plan

| Area | Change |
| --- | --- |
| Header and scope (§1-§3) | Fix-round-4 label, inputs, fix-4 artifact, preserved history and unchanged baseline counts |
| Success signal (§2) | Shared contract, canonical fingerprints, replay precedence, active-call guard, claim predicates, risk grounding and typed snapshot |
| Design alignment (§4) | Shared contract, direct-path predicate reuse, typed response/cutoff and approved risk scope |
| Decisions (§7) | D10/D23/D25/D26/D39 revised; D40-D45 added for the shared service, fingerprints/precedence, active-call guard, risk grounding, typed snapshot and activation-versus-call separation |
| Table catalog and sensitive inventory (§8, §8.1) | Idempotency fingerprint/key semantics, risk scope, task claim predicates, session confirmation gate, rejected call parent, IVR decision reason/typed snapshot, reservation frozen units/cutoff and callee redaction |
| Invariants (§9) | INV-08, INV-10, INV-11, INV-13, INV-14, INV-19, INV-27, INV-31, INV-34, INV-35, INV-37 and INV-40 updated in place; count remains 40 |
| State machines and transactions (§10) | Activation-confirmation, inbound-session, IVR-decision, direct-authorization, call-record, outbound-task and usage-reservation rows; new normative §10.1 shared contract, ordered predicate pseudocode, fingerprints, precedence, action/reason matrix and scenarios; sequences 1/3/4 rewritten |
| Contract (§11) | Ownership contract, published-service table, shared-service boundary, typed snapshot table, B-leg/replay rules, privilege/atomicity and semantic validation assertions |
| Validation (§14) | Gate commands and final-spec term checks updated to fix-4; the embedded structural-script-v2 block is unchanged and executed with explicit `FILE`/`FIX2` |
| Docs/risks/completion (§15-§18) | Fix-4 artifact paths, Q24-Q27 deferred policy/configuration inputs, residual-risk rows and report format |

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

No table, invariant, wave or slice was added, removed, re-membered or reordered. The new contract is prose, decisions, state-machine rows and test assertions inside existing tables/sections.

## 4. Exact validation commands and results

Commands are run from the repository root `/mnt/d/codex-call` with `FILE` set to the revised plan and `FIX2` set to this fix-4 artifact. Results are recorded after execution; if the environment blocks a command, the limitation is stated and no pass is invented.

### 4.1 Extract and run the embedded structural script unchanged

```bash
FILE=docs/gateflow/a0-production-data-model-schema-spec-plan.md
FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-4.md
export FILE FIX2
awk '/^# structural-script-v2$/{f=1} f{print} /^# end structural-script-v2$/{exit}' "$FILE" > /tmp/a0-structural-v2.sh
bash /tmp/a0-structural-v2.sh
```

- Executed unchanged from `/mnt/d/codex-call` with `FILE` and `FIX2` explicitly set to the revised plan and this fix-4 artifact.
- Exit: `0`.
- Output:

```text
fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81
catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24
```

- The embedded structural block is unchanged from fix round 3 (the fallback `FIX2` default still names the preserved fix-3 artifact and was overridden by the exported value). No check was deleted or weakened.

### 4.2 Gate preconditions, counts and whitespace

- Executed from `/mnt/d/codex-call`:
  - `test -s "$FILE"` and `test -s "$FIX2"`: pass.
  - 18 required numbered sections: pass.
  - 3 slice headings and 9 wave rows: pass.
  - S1 completion names the four platform/audit functions and does not require `issue_first_trial`: pass.
  - Trailing-whitespace scan on both artifacts: no matches.
  - `git diff --check -- docs/gateflow/a0-production-data-model-schema-spec-plan.md`: exit `0` (the plan is untracked in this worktree, so the direct whitespace scan above is the substantive evidence).
- Output: `GATE_PRECONDITIONS_EXIT=0 sections=18 slices=3 waves=9 no_trailing_whitespace=1`.
- Derived counts from the structural script match the binding baseline: 44 tables, 40 invariants, 9 waves, 13/23/8 slices, 28/16 lifecycle, 28 sensitive tables, 81 FK decisions (`74/3/2/2`) and 24 key sets.

## 5. Residual risks, owners and uncovered areas

This gate validates the plan's structure and written contracts only. It does not prove executable production behavior, concurrent PostgreSQL behavior, real IVR line/B-leg behavior or the actual authorization cutoff.

| Risk / uncovered area | Owner and destination |
| --- | --- |
| No executable Flyway migration or Testcontainers concurrency suite exists at this plan gate; the shared predicate parity, current-call exclusion, competing-call branch, fingerprint replay/conflict, claim/reservation rollback, revocation races and typed snapshot are plan contracts only | IVR/Activation/Entitlement/Backend; implementation Testcontainers and integration suite |
| The embedded structural script proves document structure and selected terms, not PostgreSQL behavior; deferred evidence constraints, row locks, idempotency concurrency and B-leg cutoff remain implementation tests | Backend/Database; implementation migration and concurrency suites |
| Exact `authorizationExpiresAt` window/clock skew, typed snapshot wire/version mapping, task-access role matrix and any future call-blocking risk rule are not approved production inputs | IVR/Entitlement/Security/Product/Risk; Q24-Q27 destinations in plan §16 |
| `SECURITY DEFINER` role provisioning, isolation-level assumptions, committed-`NEW` alert durability, trial subject-lock mechanism and evidence trigger privileges remain open questions from prior reviews | Platform/Database/Security; security design, operations runbook and implementation suite |
| Caller-only matching, number recycling/historical-evidence access and legal retention remain launch-blocking product/telephony/legal risks unchanged by this plan | Product/Telephony/Legal/Risk; IVR integration and launch-readiness review |
| No final specification `docs/14-production-data-model-and-flyway-schema-spec.md` or production behavior is produced at this gate | Future implementation gate |

## 6. Allowed files, scope and Git status

- Modified: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`.
- Created: `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-4.md` (this artifact).
- No controller, prior fix/review artifact, `docs/09`-`docs/13`, source code, prototype, QA screenshot, `logo/preview.html`, `.codex`, credential or Git metadata was modified.
- No file was staged, committed, pushed, merged or opened as a PR.
- No re-review, implementation or later gate was entered.
- This is a local fix artifact only; no remote-acceptance claim is made.

## 7. Completion report

- Task label: `a0-ivr-contract-fix4-ds-20261022-165224`; gate: fix round 4.
- Artifact path: `docs/gateflow/a0-production-data-model-schema-spec-plan-fix-4.md`; revised plan: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`.
- Status: F-09 **已修复**; F-10 **已修复**; F-11 **已修复**; F-12 **已修复**; unified IVR contract **已修复**; PR-001..PR-014 and F-01..F-08 preserved.
- Validation: the embedded structural script was extracted unchanged and executed with `FILE`/`FIX2` set to the revised plan and this artifact; exit `0`; output `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24` and `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; gate preconditions exit `0`; no trailing whitespace.
- No later gate entered.
