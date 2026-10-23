# A0 Production Data Model and Flyway Schema Specification Plan — fix artifact (round 6)

- Task label: `a0-ivr-contract-fix6-ds-20261022-01`; gate: `fix` round 6; date: 2026-10-22.
- Revised plan: `docs/gateflow/a0-production-data-model-schema-spec-plan.md`.
- Accepted inputs: `docs/reviews/plan-review-20261022-173816.md` (F-14, OQ-1, OQ-2) and the controller fix-round-6 adjudication.
- Historical artifacts preserved unmodified: fix-5, fix-4, fix-3, fix-2, fix, all reviews and the controller.
- Scope: plan correction and this artifact only; no SQL/migration, code, prototype, cloud, credential, final specification, commit, push, PR, merge, re-review or later gate.

## 1. Finding status

### 1.1 F-14 — **已修复**

- §10.1 scenario (line 611) now matches the normative rule (line 602) and tests (lines 748/770):
  - (a) revocation commits before the confirmation acquires the locks → phase B observes revoked access and freezes a category-2 no-dial `REJECT` (`TASK_ACCESS_REVOKED`/`ORGANIZATION_INACTIVE`/`ACCOUNT_DISABLED`); valid activation/trial and `CONFIRMED` commit with no claim/reservation.
  - (b) confirmation acquires the locks first and, while every task/entitlement predicate passes, commits a frozen `DIAL_TASK_CALLEE` with claim/reservation; the later revocation governs later calls only and cannot retroactively reject or reverse it.
- The scenario prohibits post-commit authorization rereads, decision reversal and invented rejection, keeps the no-fall-through rule, and does not require dialing independent of predicates.
- One §11 assertion (after the existing revocation assertion) now requires both serial orders, the category-2 no-dial loser, the conditional dial-possible winner with claim/reservation, and no post-commit reread/reversal. No executable test at this PLAN gate.

### 1.2 OQ-1 — **已澄清** (aligned to existing write sets; no outcome change)

- Authenticated fresh normal rejection persists only its stable rejected decision/completion, the required `call_record` parent (create terminal `REJECTED` when absent, or finalize its own never-started/rejectable `ACTIVE` row), and marks a not-yet-confirmed session `REJECTED` only where the existing branch already permits it (write set: fresh `SUSPENDED`/`REJECT_CALL_IN_PROGRESS`).
- Current `UNKNOWN`, already-`CONFIRMED` and unrelated competing calls never change destructively.
- Identity/authentication failure or invalid-origin/semantic/fingerprint conflict: no accepted effect, no unauthorized persistent write, discard any new idempotency row (and uncommitted parent); invalid-origin keeps its no-session-mutation behavior.
- Matches write-set rows 588-589; no action, table or lifecycle state added.

### 1.3 OQ-2 — **已澄清** (no new service or security mechanism)

- Sequence 1, the §11 published-service row and §10.1 step 4 state that `activation-consent.record_activation_confirmation` — the single implementation — validates current prompt and consent policy-version/key/content before any accepted consent/activation/trial append, returning stable `REJECT`/`CONSENT_REQUIRED` with no accepted effect for invalid/stale content.
- The wrapper invokes it only after event/call replay and phase-A guards and does not reimplement the predicate; phase B then checks the resulting consent is current. The focused assertion (line 770) records the same order.

### 1.4 Preserved states

- F-09, F-12 and normative F-13 remain **已修复**; F-10/F-11, PR-001..PR-014 and F-01..F-08 unchanged. No table, invariant, wave, slice, module, function, action or lifecycle state added, removed or reordered.

## 2. Changed locations

| Area | Change |
| --- | --- |
| Header (§1-§3) | Fix-6 label, inputs, current artifact and preserved history |
| §10.1 lines 596/600, step 4, scenario, sequence 1 | Category-1 write set; OQ-2 placement; F-14 scenario; single-owner consent-content validation |
| §11 service row/assertions | `record_activation_confirmation` validation and rejection; two-order revocation assertion; sharpened stale-content assertion |
| §14/§15/§17/§18 | Current fix-6 paths, affected-file table and report-format references |

## 3. Preserved baseline

- 44 tables / 40 invariants / 9 waves (3/6/4/7/6/7/3/5/3) / 3 slices (13/23/8); lifecycle 28/16; 81 FK = 74 `FK` + 3 `ALTER_FK` + 2 `DEFERRABLE_FK` + 2 `NO_FK`; 24 key sets; 28 sensitive; 5 functions (four V001 + one V006); shared `ivr.select_call_action` application service. No expansion.

## 4. Validation

- Extracted the exact embedded `structural-script-v2` unchanged to `/tmp/a0-ivr-contract-fix6-structural-v2.sh`; `cmp` against `/tmp/a0-ivr-contract-controller-structural-v5.sh`: identical. Executed from `/mnt/d/codex-call` with `FILE=docs/gateflow/a0-production-data-model-schema-spec-plan.md` and `FIX2=docs/gateflow/a0-production-data-model-schema-spec-plan-fix-6.md`.
- Exit `0`; output: `fk_modes=FK:74,ALTER_FK:3,DEFERRABLE_FK:2,NO_FK:2 total=81`; `catalog=44 matrix=44 waves=9 wave_distribution=3/6/4/7/6/7/3/5/3 slices=S1:13,S2:23,S3:8 invariants=40 lifecycle=28/16 sensitive=28 fk_rows=81 key_sets=24`.
- Trailing whitespace: plan `0`, this artifact `0`. Cross-check: scenario, normative rule, sequence and assertions agree on both serial orders; category-1 summary agrees with the write-set rows; consent-content validation appears once, before accepted effects.
- Document/semantic checks only; no executable PostgreSQL, Testcontainers or IVR-line concurrency test is claimed.

## 5. Risks, owners and destinations

| Risk | Owner / destination |
| --- | --- |
| No executable migration/concurrency suite; both serial orders are planned assertions | IVR/Backend/Activation; implementation migration and Testcontainers suite |
| Category-1 finalization and consent-content order are not runtime-tested | Backend/Activation/IVR; suspended-confirmation and replay/content tests |
| Prior residuals (TTL key, risk rule, role matrix, B-leg protocol, retention/cloud/SIP/provider choices) unchanged | Owners/destinations in the plan and prior review artifacts |

## 6. Allowed files and Git status

- Modified: the plan. Created: this artifact. No controller, prior artifact, source doc, code, prototype, screenshot or Git metadata modified or staged; no commit, push, PR, merge, re-review or later gate entered.
