# A2 Production IVR Protocol Spec — bounded plan-fix 1

> Work unit: `A2-production-ivr-protocol-spec`
>
> Gate: `fix` (bounded plan-only correction; no implementation, no later gate)
>
> Task label: `a2-ivr-planfix1-ds-20261025-01`
>
> Reviewed target: `docs/gateflow/a2-production-ivr-protocol-spec-plan.md`, original review baseline 877 lines / 86880 bytes, SHA-256 `b9569effd19a07499096fbad7c3854436fecd47b30fbef51996a093840453351` (matches the controller plan-gate record)
>
> Independent review: `docs/reviews/plan-review-20261024-224656.md` (`pass-with-risks`; accepted Low finding F-1 and accepted review open question 2 as a bounded negative-fixture clarification)
>
> Controller adjudication: `docs/gateflow/project-execution-control.md`, subsection `A2 independent plan review adjudication and fix entry`
>
> Revised target: same plan path, now 981 lines / 102417 bytes, SHA-256 `502cc27adcd5f767dba7ccfc5b130161415f82dfafc8f49384c917c5c9e6a128`
>
> Result: revised plan plus this new fix artifact. No other repository path was edited by this task.

## 1. Gate, reviewed target and scope

- The reviewed target is the A2 implementation plan only. The binding scope remains the A2 goal contract in `docs/gateflow/project-execution-control.md`: deterministic static IVR protocol specification, machine model, fixtures and zero-dependency validation; no runtime service, UI/prototype change, invented provider/numeric/security value, SIP/media redesign, operator integration, capacity/deployment or acceptance-matrix work.
- The independent review concluded `pass-with-risks` with no prototype-flow mismatch and one Low precision finding. The controller accepted exactly two bounded corrections: F-1 (exact S1/S2 stage gating) and review open question 2 (resolve-never-committed negative case). Review open question 1 (confirm/direct unknown call maps to the A1-forced 409) remains `deferred-with-owner`; no A1 error, endpoint, state or fallback was added.
- Only `docs/gateflow/a2-production-ivr-protocol-spec-plan.md` and this new artifact were written. The controller document, review artifacts, A0/A1 sources, OpenAPI, prototypes, screenshots and all pre-existing dirty/untracked paths were preserved untouched and unstaged. No stage, commit, push, PR, merge, re-review or implementation occurred.

## 2. Accepted findings and disposition

| Item | Verdict | Disposition |
| --- | --- | --- |
| F-1 (Low) — stage scoping of doc-marker validation was underspecified | Resolved | Added an exact `--stage s1|s2|s3` projection contract in §7.4.1/§7.4.2; the event block is identity-only at S1, promoted to a full frozen 4-column form at S2, and read-only/digest-verified at S3. S2 payload/state/terminal behavior was not moved into S1. |
| Review open question 2 — resolve never committed | Resolved as accepted clarification | Added negative fixture `s2-11-resolve-never-committed-not-started` and matching recovery/error/path wording: later `OUTBOUND_NOT_STARTED` returns `404 CALL_UNKNOWN`, creates no placeholder, releases no reservation, creates no usage fact and becomes a permanent local outbox failure. The committed-but-response-lost case remains a frozen-decision replay (`s2-05`/`s2-07`). |
| Review open question 1 — confirm/direct unknown call maps to 409 | Deferred-with-owner | Unchanged. A2 does not invent a new Problem code; destination remains IVR/Backend observability and possible later A1 refinement. |
| All other plan residuals | Preserved as classified | Task-ID presentation, heartbeat volume/retention, declaration replay, session `ABANDONED`, upload observation, evidence handoff, static-validator co-authorship, caller correlation/CDR recovery and deferred Q-values keep their owners/destinations. |

## 3. Exact changes made

| Area | Edit |
| --- | --- |
| Plan header | Changed `Gate` to `fix` (bounded plan-fix-1), added the fix task label and the review/controller fix basis, and retained the original author task label and base commit. |
| §0 summary | Added the stage-projection summary and the resolve-never-committed fail-closed summary; replaced “this task only creates the plan file” with the plan-only fix-artifact wording. |
| §1.3 success signals | Added `s2-11` to the unknown fail-closed fixture evidence. |
| §3.3 single-truth rule | Added the exact marker-block stage contract: message 12 rows and identity 22 rows frozen from S1; event block identity-only at S1 with `DEFERRED_S2`; S2 promotes it to the full 4-column frozen form plus digest; S3 cannot modify it. Added the no-skip/no-partial-row `--stage` rule. |
| §4.5 event registry intro | Marked the section as the S2 frozen target form; S1 registers only the 11 event identities/names and the `DEFERRED_S2` sentinel. |
| §4.7 recovery rule 5 | Split the lost-response recovery into (a) original resolve committed — retry the same `callId` for the frozen decision, and a later not-started report acts on the existing call/reservation; and (b) original resolve never committed — no backend call/reservation, later `OUTBOUND_NOT_STARTED` returns `404 CALL_UNKNOWN`, no placeholder/release/usage, local outbox is permanent. |
| §4.9 terminal mapping | Constrained the `OUTBOUND_NOT_STARTED` terminal mapping to a committed resolve and added the never-committed unknown-parent-call behavior. |
| §4.12 stable errors | Added the explicit `404 CALL_UNKNOWN` / permanent-outbox / no-release rule for a never-committed `OUTBOUND_NOT_STARTED`. |
| §6.4 legal/illegal path matrix | Relabelled the existing expiry path as “resolve committed”; added the committed-but-response-lost replay row and the never-committed `OUTBOUND_NOT_STARTED` 404 row. |
| §7.4 | Replaced the flat exact-comparison bullets with a stage-aware contract. |
| §7.4.1 (new) | Defined exact marker columns/rows, the S1/S2/S3 event-map forms, the 11 frozen event rows (`envelopeLegId`, `payloadFields`, `normalProcessingOutcome`), the per-stage model top-level section allowlist, the `eventRegistry` freeze digest, and the exact cumulative fixture ID sets. |
| §7.4.2 (new) | Defined the stage rejection conditions and the new zero-count checks (`stage_field_missing`, `stage_field_premature`, `fixture_id_missing`, `fixture_id_extra`). |
| §7.5 fixtures | Clarified `s2-05` as the committed-but-response-lost replay, appended `s2-11` to the S2 negative set, and recomputed cumulative fixtures to S1=6, S2=17, S3=25. |
| §7.6 negatives/determinism | Added stage-gating negative cases and the definition of `negative` (cumulative rows explicitly marked `(负例)`); replaced the single S1 output example with exact S1/S2/S3 output lines including `event_map_form`, cumulative/slice fixtures, stage counters and the S2 freeze digest. |
| §8.2 S1 | Made the event block identity-only explicit, added the 15 cross-cutting invariant IDs, and added the stage-specific expected assertions and deferred event/media semantics. |
| §8.3 S2 | Made the promotion of the S1 event block explicit, added the full freeze components and `eventRegistry` digest, added the never-committed negative path, and recomputed expected fixtures to 17 with three negatives. |
| §8.4 S3 | Required the S2 event registry to stay unchanged (digest equal), made the final error/identity work a consistency check rather than a new registry, and recomputed expected fixtures to 25. |
| §10 completion template | Added the event-block stage form to the slice report, the new stage counters to the drift evidence, and changed the final S3 inventory to 25 fixtures with the unchanged S2 event digest. |

## 4. Exact stage contract after the fix

| Element | S1 | S2 | S3 |
| --- | --- | --- | --- |
| `protocol-message-map` | 12 rows; columns `messageId,slice,operationId,methodPath,requestSchema,responseSchema,identity` | same 12 rows/columns/values | same |
| `protocol-identity-map` | 22 rows; columns `identity,owner,uniqueness,a0a1Mapping` | same 22 rows/columns/values | same |
| `protocol-event-map` | 11 rows; columns `eventType,payloadState`; every value `DEFERRED_S2` | 11 rows; columns `eventType,envelopeLegId,payloadFields,normalProcessingOutcome`; `payloadState` absent | identical to S2 |
| model `eventTypes[]` | exactly `{eventType,payloadState}` | exactly `{eventType,envelopeLegId,payloadFields,normalProcessingOutcome}` | identical to S2; digest recheck |
| model `eventRegistry` | absent | exactly `{frozenAt:"s2",sha256}` | same as S2; recomputed digest must match |
| S2-only model sections | absent | `eventEnvelope`, `endReasons` (9), `quarantineReasons` (10), `stateMachines`, `blegOperation`, `eventOutbox` | same as S2 |
| S3-only model sections | absent | absent | `recordingLifecycle`, `mediaEvidenceBoundary` |
| fixture slices | `s1-01..s1-06` = 6 | `s2-01..s2-11` = 11 | `s3-01..s3-08` = 8 |
| cumulative fixtures | 6 | 17 | 25 |
| cumulative marked negatives | 0 | 3 (`s2-08`, `s2-10`, `s2-11`) | 3 |
| output form | `event_map_form=identity-only` | `event_map_form=full-frozen` | `event_map_form=full-frozen` |

- The 11 event identities are unchanged: `CALLER_LEG_ANSWERED`, `CALLEE_LEG_RINGING`, `CALLEE_LEG_ANSWERED`, `BRIDGE_STARTED`, `RECORDING_STARTED`, `RECORDING_ENDED`, `CALL_TERMINATED`, `CALL_TERMINATION_UNKNOWN`, `CALL_TERMINAL_RECONCILED`, `OUTBOUND_NOT_STARTED`, `CALL_HEARTBEAT`.
- The S2/S3 closed payload field sets in §7.4.1 are derived field-for-field from §4.5; `CALL_TERMINAL_RECONCILED` is the `CALL_TERMINATED` set plus `resolvesUnknownEventId`. `payloadFields` uses `field?` for optional/conditional fields and the fixed §4.5 row order; empty payloads are `[]` in the model and `{}` in the doc.
- The validator must reject missing current-stage fields/sections, premature next-stage fields/sections, a skipped event block, a partial event-row comparison, a non-sentinel S1 payload state, residual `payloadState` at S2/S3, a changed S3 event digest, and missing/extra stage fixture IDs.

## 5. Resolve-never-committed negative case

- Added `s2-11-resolve-never-committed-not-started` to the S2 negative fixture set in §7.5. Scenario: the resolve response is lost and the original resolve never committed; the IVR has no usable decision, no local B-leg operation and no line attempt (`lineAttemptCount = 0`); a later `OUTBOUND_NOT_STARTED` event is sent for that `callId`.
- Assertions: `404 CALL_UNKNOWN`; no `call_record`, task or reservation placeholder is created; no reservation release and no `usage_fact`; the local outbox entry becomes `PERMANENT_FAILURE`. The fixture is not a release-success path and cannot be represented as one.
- The committed-but-response-lost case is distinct: §4.7 rule 5, §6.4, §7.5 `s2-05` and §8.3 keep the same-`callId` retry that returns the first frozen decision; a later authorized-not-started report then acts on the existing call/reservation (`s2-07`).
- No endpoint, Problem code, A0 state, reservation behavior or fallback was added. `404 CALL_UNKNOWN` is the existing A1 event-ingestion error already mapped by §4.12 for a missing parent call; the fix only makes the fail-closed outcome explicit and testable.

## 6. Evidence

- The review's A0 audit (`docs/reviews/plan-review-20261024-224656.md`, assumptions table row 2) records T-S2-12 steps 5-8 as replay-first, `CALL_UNKNOWN` with no placeholder, first-terminal absorption, `UNKNOWN` from a non-authoritative terminal, mandatory settlement handoff and no double settlement. The new fixture asserts only those already-verified outcomes.
- The controller adjudication (lines 84-85) requires exactly: S1 registers event identities/names and validates the S1-approved projection; S2 adds/freezes payload/state semantics and promotes the event block; the validator rejects premature/missing stage fields and cannot skip the event map; the never-committed event returns `404 CALL_UNKNOWN`, is a permanent outbox failure and cannot be represented as release. Both requirements are implemented as plan text.
- The plan's existing A1/A0 mapping is unchanged: 6 IVR operations, 49 total operations, 44 A0 tables, 12 messages, 11 event identities, 22 identities, 15 invariants and 3 behavior slices remain. No new route, Problem code, table, column, state or transaction is introduced.
- §4.5, §4.7, §4.9, §4.12 and §6.4 remain the protocol-behavior authorities. The fix changes validation staging, stage tables/outputs and the explicit fail-closed negative path; it does not redesign event types, payload behavior, A1 operations, A0 state or slice ownership.

## 7. Validation performed

| Check | Method | Outcome |
| --- | --- | --- |
| Encoding | `file -bi docs/gateflow/a2-production-ivr-protocol-spec-plan.md` | `text/plain; charset=utf-8` |
| Conflict markers | Grep for `^(<<<<<<<|=======|>>>>>>>)` in the plan | No matches |
| Trailing whitespace | Grep for `[ \t]+$` in the plan | No matches |
| Size/hash | `wc -l` / `wc -c` / `sha256sum` | 981 lines / 102417 bytes / `502cc27adcd5f767dba7ccfc5b130161415f82dfafc8f49384c917c5c9e6a128` |
| Message rows | Grep count of §4.3.1 message-row prefix | 12 |
| Event rows | Grep count of the §4.5 event table and the §7.4.1 frozen event table | 11 and 11; names/order match |
| Identity rows | Manual count of the §5 matrix | 22 (unchanged) |
| Fixture IDs/counts | Grep count of §7.5 row prefixes | `s1-*` 6, `s2-*` 11, `s3-*` 8; cumulative 6/17/25; negatives 0/3/3 |
| Slice count | §8.2/§8.3/§8.4 exist as S1/S2/S3 | 3 slices preserved |
| Binding constraints | Re-read §1, §4, §5, §6, §8-§12 | 6 A1 IVR operations, no new endpoint/code/table/state; prototype flows, A0/A1 ownership and deferred inputs preserved |
| Scope | `git status --porcelain` | Only the plan (pre-existing untracked target, revised) and this new fix artifact are task-owned; pre-existing modified screenshots/prototype files, controller file and historical review artifacts are untouched |

No npm, Redocly, Node or executable fixture validation was run: the planned `docs/16`, model, fixtures and `validate.mjs` do not exist yet, so all checks in this fix are document-level/static checks. The stage contract is a specification for the S1 implementation to encode and test.

## 8. Documentation decision

- Keep exactly one human canonical protocol document (`docs/16-production-ivr-protocol-spec.md`), one machine model (`protocol/ivr/v1/ivr-protocol-v1.json`), deterministic fixtures and the zero-dependency `protocol/ivr/validate.mjs`.
- The fix is plan-only. No README, second protocol document, JSON Schema copy, A0/A1 edit, OpenAPI edit, prototype/screenshot edit or package dependency was added.
- The S1/S2/S3 document ownership is unchanged: S1 creates the base and identity-only event block; S2 promotes and freezes the event block; S3 adds recording/media semantics and verifies the frozen event registry.

## 9. Residual risks

- `R-A2-A0-TASKID-TYPE`, `R-A2-HEARTBEAT-VOLUME`, `R-A2-DECLARE-REPLAY`, `R-A2-SESSION-ABANDONED`, `R-A2-UPLOAD-OBSERVE`, `R-A2-EVIDENCE-HANDOFF` and the other plan residuals remain classified with their existing owners/destinations.
- The `ProtocolMachine`/fixture co-authorship risk remains: a self-consistent static machine can still be semantically wrong about a real backend or line; adversarial fixture review at each slice gate and later integration/QA remain required.
- The S2 `eventRegistry.sha256` is a deterministic drift guard, not tamper proof. A reviewer must still verify that S3 did not semantically rewrite an event row and recompute the digest. Destination: A2 S2/S3 review.
- The IVR local trigger for emitting `OUTBOUND_NOT_STARTED` after a permanently unusable resolve remains local runtime behavior; A2 fixes the fail-closed backend response, not a numeric timeout or retry policy (Q8 deferred).
- Deferred inputs Q7/Q8/Q10/Q18/Q25/Q27 (and the other named Q-values) remain untouched and must not be invented during implementation.

## 10. Completion status and artifact path

- Status: **complete for this bounded fix Gate**. The revised plan is ready for a fresh independent plan re-review; implementation remains unauthorized until that re-review is accepted by the controller.
- Changed files: `docs/gateflow/a2-production-ivr-protocol-spec-plan.md` (revised) and this artifact, `docs/gateflow/a2-production-ivr-protocol-spec-plan-fix-1.md` (new).
- No stage/commit/push/PR/merge and no entry into re-review or implementation from this task.
