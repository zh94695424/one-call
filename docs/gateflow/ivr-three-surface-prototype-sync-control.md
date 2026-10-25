# IVR Three-Surface Prototype Sync Control

## Current work unit

- ID: `ivr-three-surface-prototype-sync`
- Type: feature / prototype alignment.
- Status: active.
- Base: `origin/main` at `754b7dfc53c277e79c5d63e24d578a93fa557cda`.
- Branch: `codex/ivr-prototype-sync` in an isolated managed worktree.
- Current gate / next entry point: `aggregate deepreview`; accepted slice commit `42409af` exists, but the fresh MiMo 2.6 Flash aggregate reviewer did not start because the runner rejected its root permission mode before model execution.
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
| accepted plan commit | passed | `9851289`, `gateflow: accept plan for ivr-three-surface-prototype-sync`; exactly control, plan and review artifact | implementation |
| implementation | author-complete; controller validation failed | `docs/gateflow/ivr-three-surface-prototype-sync-implementation.md`; DeepSeek task `ivr-prototype-sync-impl-ds-20261025-01`, exit 0, structured success, empty stderr; controller syntax/unit checks | fix two test defects, regenerate, rerun full validation before code review |
| fix | passed (fifth bounded return) | DeepSeek task `ivr-prototype-sync-fix5-ds-20261025-01`, exit 0, structured success, empty stderr; implementation artifact `Controller validation fix 5`; controller observed call-task 15/15, aggregate 46/46, all three surface runners, cross-channel, build equality, syntax and diff checks pass; Web 1440/1024/720 and cross-channel 1280 inspected clean | independent re-review of CF-09/CF-10 |
| re-review | passed | `docs/reviews/code-review-20261025-112537.md`; fresh MiMo 2.6 Flash task `ivr-prototype-sync-rereview5-mimo26flash-20261025-01`, exit 0, structured success, empty stderr; no findings; CF-09 and CF-10 verified closed | accepted slice commit |
| accepted slice commit | passed | `42409af`, `gateflow: accept IVR prototype sync slice`; 50 implementation, QA, documentation, review and workflow-owned screenshot files; pre-commit syntax, 46/46 shared tests, generated equality and diff checks passed | aggregate deepreview |
| aggregate deepreview | blocked before model execution | MiMo 2.6 Flash dispatch `ivr-prototype-sync-deepreview-mimo26flash-20261025-01` and its single retry both exited 1 with empty JSON and stderr `--dangerously-skip-permissions cannot be used with root/sudo privileges`; run dirs `/tmp/sub-agents.ivr-prototype-deepreview.KCdG5V` and `/tmp/sub-agents.ivr-prototype-deepreview.mN6SBr` | repair runner permission-mode configuration, then start a fresh compliant aggregate reviewer |

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

## Implementation validation findings

- CF-01 `accepted`: `shared/qa/cross-channel.js` redeclares `const claimed` in the same function scope, so `node --check` fails before any cross-channel assertion can run. Rename the new call-task state variable and all references without changing behavior.
- CF-02 `accepted`: `shared/qa/call-task.test.js` incorrectly expects exactly one call for normalized caller `18600004471`, but the deterministic fixture intentionally already contains historical mismatch call `OC-C2036`. The implementation correctly creates/replays only `CALL-QA-TASK-01`; fix the test to capture the pre-claim caller call count and prove successful claim adds exactly one while replay adds zero, retaining exact-callId uniqueness assertions.
- The business-module hash failure is expected until `shared/build-prototypes.js` runs; it is not a code finding. After CF-01/CF-02, controller must regenerate controlled blocks/previews and rerun all required checks.
- CF-01/CF-02 fix evidence: DeepSeek task `ivr-prototype-sync-fix1-ds-20261025-01`, exit 0 with valid structured success and empty stderr. Controller observed `node --check shared/qa/cross-channel.js` pass, `node --test shared/qa/call-task.test.js` 11/11 pass, `node --test shared/qa/runner-exit.test.js` pass, and `git diff --check` pass.
- CF-03 `accepted`: the Mini runner still requires the stale phrase `真实主叫` for the landline challenge, although the updated UI correctly identifies the self-developed IVR and the carrier line as the transport of the trusted caller fact. Update the assertion to require `自研 IVR`, `线路传递`, and `可信主叫` while retaining the `95XXX` and `483 621` checks. Do not restore the old carrier-owned wording or change the Mini UI.
- CF-04 `accepted`: the Web call-detail QA requires an explicit visible distinction that a task-backed call needs no callee confirmation, but the rendered meta currently says only `无需确认（任务自动外呼）`. Change that visible value to `无需确认被叫（任务自动外呼）`; keep the QA assertion and all call-path behavior unchanged.
- Runner evidence after CF-01/CF-02: Operations passed its full suite, including read-only diagnostics, frozen 30-second task semantics, automatic task dialing, no-task DTMF, upload/evidence separation, foreign-account isolation and 1440/1280/1024/720 responsive checks. Mini stopped at CF-03; Web stopped at CF-04. All three suites must be rerun after the bounded fix.
- CF-03/CF-04 fix evidence: DeepSeek task `ivr-prototype-sync-fix2-ds-20261025-01`, exit 0 with valid structured success and empty stderr. Controller then observed Mini and Operations full runners pass. Web passed on a clean screenshot directory after two earlier runs reached unrelated existing PNG overwrite errors (`open UNKNOWN`); historical screenshots were restored and only workflow-owned call-task screenshots retained.
- CF-05 `accepted`: `shared/qa/cross-channel.js` declares a global helper `facts` and later declares `const facts` for call events in the same async block. JavaScript temporal-dead-zone rules make the earlier account-claim call at line 99 resolve to the later local declaration and throw `ReferenceError` before cross-channel checks run. Rename the call-event array and its loop reference only.
- CF-06 `accepted`: the current Mini README and two current Web usage statements still attribute trusted/actual caller confirmation directly to the carrier. Align these maintained current-baseline statements with the accepted ownership: the carrier line transports the caller fact; the self-developed IVR obtains/normalizes it; ONECALL uses it for scope and accounting. Do not rewrite sections explicitly marked historical.
- CF-05/CF-06 fix evidence: DeepSeek task `ivr-prototype-sync-fix3-ds-20261025-01`, exit 0 with valid structured success and empty stderr. Controller observed `node shared/qa/cross-channel.js` pass after the fix.
- CF-07 `accepted-high`: `resolveIvrInbound` currently claims a valid pending task before checking `callerState` and `availableSeconds`; an inactive/unregistered caller or an account with zero seconds can therefore bypass the current activation/entitlement decision through an old task. The no-task branch also creates `ENTER_CALLEE` when `uniqueCallerBinding` returns null, allowing missing or ambiguous caller ownership into DTMF. Reorder current-state/entitlement checks before claim, require a unique active binding before task/no-task execution, keep `directAllowed` specific to the no-task branch, and add atomic no-mutation tests for all denied/guide paths.
- CF-08 `accepted-low`: visual inspection found unrelated transient toasts obscuring the new Web and Operations diagnostic screenshots. Wait for the existing toast lifecycle to finish before capturing the task/no-task screenshots, including the cross-channel Web capture. Do not change application toast behavior or weaken assertions.
- CF-07/CF-08 fix evidence: DeepSeek task `ivr-prototype-sync-fix4-ds-20261025-01`, exit 0 with valid structured success and empty stderr. Controller observed 14/14 call-task tests, all three surface runners, generated equality, syntax, diff and cross-channel checks pass after regenerating. The Web runner and cross-channel runner required empty screenshot directories because Windows denied overwriting some existing PNG files; original directories were restored after each run.
- CF-09 `accepted-high`: the `SUSPENDED` branch in `resolveIvrInbound` mutates every PENDING task for the normalized caller before resolving the unique active binding. A valid snapshot with two active bindings therefore returns `REJECT_SUSPENDED` and changes another ownership scope's task to `REJECTED`, instead of failing closed without mutation. Resolve unique ownership before any task write, scope the suspended rejection to that binding, and add missing/ambiguous SUSPENDED atomicity tests plus the unique-binding positive case.
- CF-10 `accepted-low`: Web QA and cross-channel QA wait only for the Toast `visible` class to disappear, while the application CSS continues a 0.2-second opacity transition. Original-resolution screenshots still contain a gray Toast ghost over the diagnostic table. Wait for computed opacity `0` without a fixed sleep, rerun Web 1440/1024/720 and cross-channel 1280, and reinspect them.

## Controller direct review round

- Artifact: `docs/reviews/code-review-20261025-094254.md`.
- Verdict: fail; CR-01/CF-09 and CR-02/CF-10 are accepted and unresolved.
- Behavior validation before findings: shared tests 45/45, all three full surface runners, cross-channel runner, generated equality, syntax and diff checks passed.
- Visual validation: Mini 393/320, Web 1440/1024/720 and details, Operations 1440/1280/720, and three cross-channel task screenshots inspected at original resolution.
- Gate decision: remain in `fix`; do not create the accepted slice commit until both findings are fixed and re-reviewed.

## Fifth bounded re-review adjudication

- Artifact: `docs/reviews/code-review-20261025-112537.md`.
- Verdict accepted: `pass`; the independent reviewer found no material issue in the bounded CF-09/CF-10 fix.
- CF-09 `closed`: unique caller ownership is resolved before the `SUSPENDED` write path; missing or ambiguous bindings fail closed without mutation, and a uniquely owned pending task is the only task eligible for suspended rejection.
- CF-10 `closed`: Web and cross-channel screenshots wait for computed Toast opacity `0`, covering the existing 0.2-second fade without a fixed sleep or application behavior change.
- Validation accepted: call-task tests 15/15, aggregate shared tests 46/46, all three full surface runners, cross-channel, generated equality, syntax and diff checks passed; Web 1440/1024/720 and cross-channel 1280 screenshots were inspected without Toast residue.
- Nonblocking review notes remain residual risks: this prototype does not prove production concurrency/telephony/cloud behavior; `callerTrusted: false` lacks a direct unit case but was untouched; Product/Legal owns final recording and consent copy.
- Gate decision: create the accepted slice commit, then enter `aggregate deepreview`.

