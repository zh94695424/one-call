# IVR Three-Surface Prototype Sync Control

## Current work unit

- ID: `ivr-three-surface-prototype-sync`
- Type: feature / prototype alignment.
- Status: active.
- Base: `origin/main` at `754b7dfc53c277e79c5d63e24d578a93fa557cda`.
- Branch: `codex/ivr-prototype-sync` in an isolated managed worktree.
- Current gate / next entry point: `accepted plan commit`.
- Dispatch protocol: `sub-agents`; DeepSeek authors implementation, fresh MiMo 2.6 Flash reviews.

## Binding goal confirmation

- Preserve the accepted no-task IVR path: only when no valid pending task exists may the caller enter and confirm a callee through DTMF.
- Synchronize the Mini Program, customer Web and operations prototypes with the accepted single-slot outbound task design: one current nonterminal task per normalized caller, server-frozen default 30-second expiry, atomic IVR claim, automatic dialing of the frozen task callee without voice confirmation, and return/diagnostic state visibility.
- Correct stale ownership copy: the carrier provides number, line and transport facts; the self-developed IVR owns inbound handling, B-leg outbound dialing, bridge, recording, time limiting and call/record upload; ONECALL owns activation, task, entitlement and evidence processing.
- Keep IVR first-trial activation for an unregistered or unverified caller after explicit key `1`; do not create a login or real-name identity. IVR purchase remains guidance to Mini Program/Web only.

## Success signals

- Mini Program visibly demonstrates task creation/replacement, server-derived 30-second expiry, system-dial handoff, no task-backed callee confirmation, and truthful return states without presenting dialer launch as connection success.
- Customer Web explains and displays the same call/task outcome vocabulary without inventing a browser-controlled telephone flow.
- Operations provides read-only task/call/IVR diagnostics and keeps telephony facts immutable.
- The no-task DTMF direct path remains explicit and cannot be confused with task-backed dialing.
- Shared fixtures and all affected Playwright/cross-channel/build checks pass; required desktop/mobile screenshots are inspected.

## Scope boundaries

- Allowed: the three maintained prototype sources, their README/spec updates, shared demo contract/build tooling when needed, affected QA and workflow-owned screenshots, and this work unit's gate/review artifacts.
- Excluded: production backend/runtime, A0/A1/A2 accepted contracts, IVR media engine, SIP/provider internals, real payment, real cloud resources, real credentials, PBX/extensions/seat PINs, online work orders/refunds/invoices, and production pricing.
- No new numeric policy beyond the already accepted default task TTL of 30 seconds.
- Existing historical artifacts and unrelated screenshot changes in other worktrees remain untouched.

## Gate ledger

| Gate | State | Artifact / evidence | Next |
| --- | --- | --- | --- |
| goal confirmation | passed | User explicitly retained no-task DTMF and requested immediate three-surface synchronization on 2026-10-25 | plan |
| plan | author-complete; controller checked | `docs/gateflow/ivr-three-surface-prototype-sync-plan.md`; DeepSeek task `ivr-prototype-sync-plan-ds-20261025-01`, exit 0, structured success, empty stderr | plan review |
| plan review | passed-with-risks; controller adjudicated | `docs/reviews/plan-review-20261025-084114.md`; fresh MiMo 2.6 Flash task `ivr-prototype-sync-planreview-mimo26flash-20261025-01`, exit 0, structured success, empty stderr | create accepted-plan checkpoint, then implementation |
| accepted plan commit | pending | stage exactly this control, plan and plan-review artifact | implementation |

## Residual risks

- Prototype behavior is local fixture evidence and does not prove production concurrency, telephony, persistence or cloud integration.
- Formal prompt wording and legal consent copy remain owned by Product/Legal.

## Plan return notes

- The plan uses one behavior slice because the shared fixture, three surface projections and cross-channel QA must agree at one acceptance boundary.
- The proposed call-task fixture remains separate from the strict existing business snapshot, with independent validation and bidirectional isolation tests; this avoids an unrelated schema/version migration.
- The planning subprocess could not read the external UI skill files, but the controller read them completely and confirmed the plan follows their source ownership, generated-file, visual, responsive and Playwright requirements. This is not a blocker.
- The subprocess could not resolve the managed-worktree Git metadata from WSL. Controller-side Windows Git checks pass and remain authoritative for gate bookkeeping.

## Plan review adjudication

- Verdict accepted: `pass-with-risks`; the separate validated call-task fixture and one behavior slice are the most practical option for this prototype work unit.
- F-01 `accepted`: implementation must use the live 15-route Operations definition and preserve six primary entries; it must not assert the stale 13-route count and must add no route.
- F-02 `accepted`: no-task calls derive owner/workspace only through the unique caller binding; missing or ambiguous binding fails closed and cannot appear in customer Web.
- F-03 `accepted`: review-state hooks must load validated snapshots or drive shared mutations; they cannot render unreachable arbitrary state.
- F-04 `accepted`: Mini remaining time is computed at render from frozen server fields and injected review time; no live interval is required or asserted.
- F-05 `accepted`: call-task `evidenceStatus` is explicitly a diagnostic projection; `business.evidence` remains the authoritative evidence list.
- F-06 `rejected-with-reason`: the skill/Git blockers are already superseded by controller access and Windows Git validation.
- F-07 `accepted-as-nonblocking-evidence-note`: implementation uses actual source ownership and the whole-source stale-copy grep, not the imprecise line hints.
- F-08 `deferred-with-owner`: the accepted activation-session demo and task decision demo remain separate local fixture models; real unified resolve belongs to the later Backend/IVR integration work unit and must not be claimed here.

