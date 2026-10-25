# IVR Three-Surface Prototype Sync — Implementation

- Work unit: `ivr-three-surface-prototype-sync`
- Task label: `ivr-prototype-sync-impl-ds-20261025-01`
- Base: `origin/main` at `754b7dfc53c277e79c5d63e24d578a93fa557cda` (branch `codex/ivr-prototype-sync`, isolated managed worktree)
- Gate: implementation authoring only. No code review, commit, push, PR, controller-state edit or gate advancement was performed.
- Final state: **BLOCKED (environment execution)** — source slice implemented, but the worktree cannot execute Node/PowerShell through this session's permission layer, so the builder, runners, unit tests, cross-channel QA and screenshot inspection could not run. No success is claimed.

## 1. Changed files (exact)

Shared contract/tooling:

- `shared/onecall-business.js`
- `shared/README.md`
- `shared/qa/call-task.test.js` (new)
- `shared/qa/cross-channel.js`

Mini Program:

- `prototype/onecall-miniapp-pages.html`
- `prototype/qa/qa.js`
- `prototype/README.md`

Customer Web:

- `web-prototype/evidence-hub.html`
- `web-prototype/qa/qa.js`
- `web-prototype/README.md`

Operations:

- `operations-prototype/onecall-operations-admin.html`
- `operations-prototype/qa/qa.js`
- `operations-prototype/README.md`

Docs:

- `docs/06-miniapp-ui-spec.md`
- `docs/07-web-capability-hub-spec.md`
- `docs/09-business-activation-and-lean-admin.md`
- `docs/10-cross-channel-rules.md`
- `docs/13-ivr-call-task-and-inbound-design.md`
- `docs/README.md`
- `docs/gateflow/ivr-three-surface-prototype-sync-implementation.md` (this artifact)

Not modified: this control file, accepted plan/review, A0/A1/A2 contracts, `api/`, production code, `prototype/preview.html`, `web-prototype/preview.html`, `operations-prototype/preview.html`, historical screenshots, `docs/11-account-identity-and-workspaces.md`, `docs/01–05/08/12`, `docs/reviews/`, archives, Figma assets, logo assets, `.docx` files.

## 2. Slice decision

**One behavior slice.** The shared call-task contract, three surface projections, generated-block ownership and cross-channel QA must agree at one acceptance boundary. Splitting by surface would create divergent fixtures and duplicate 30-second/claim semantics; splitting the shared contract from the surfaces would leave no runnable acceptance. The slice remains atomic.

## 3. Shared contract and transitions

### 3.1 Placement and prototype-only rationale

The call-task domain lives in `shared/onecall-business.js` but **outside** the strict business snapshot:

- `BUSINESS_FIELDS`, business `schemaVersion: 1`, business event domains and `validateState` are untouched.
- The call-task domain has its own `{ schemaVersion: 1, ttlSeconds: 30, callerBindings, tasks, calls, sequence }` shape and its own strict `validateCallTask`.
- This follows the existing account-directory precedent and avoids a business schema bump or weakening accepted validators.
- Bidirectional isolation is enforced by separate `snapshot`/`loadReviewSnapshot` versus `callTaskSnapshot`/`loadCallTaskSnapshot` paths and tested by `call-task.test.js` and cross-channel QA.
- Everything is a local review fixture. It does not prove database concurrency, persistence, real telephony, media bridging, recording, object storage, upload or cloud integration.

### 3.2 Exports and vocabulary

Added/exported:

- `CALL_TASK_TTL_SECONDS = 30`
- `labels.taskStatus`, `labels.ivrAction`, `labels.taskMatch`, `labels.callProgress`, `labels.uploadStatus`, `labels.evidenceStatus`
- `labels.ownership` (canonical: carrier provides number/line/transport; self-developed IVR owns inbound, B-leg dialing, bridge, recording, time limit and upload; ONECALL owns activation, tasks, entitlement and evidence processing)
- `labels.callPath.{task,noTask,launch,localFixture}`
- `createCallTaskDemoState`, `createCallTask`, `cancelCallTask`, `recordDialerLaunch`, `resolveIvrInbound`, `confirmDirectCallee`, `recordCallFact`
- `callTaskSnapshot`, `loadCallTaskSnapshot`, `validateCallTask`, `uniqueCallerBinding`

`recordDialerLaunch` is the shared mutation behind the Mini launch fact. It writes only the task's `dialerLaunchedAt`; it cannot advance task status, call progress, evidence or connection.

### 3.3 State shape and invariants

- `callerBindings[]`: id, owner type/id, workspace, masked/normalized caller, number type, active.
- `tasks[]`: id, binding, normalized/masked caller, masked callee, owner/workspace, creator, `EVIDENCE_CALL`, status, reason, frozen `ttlSeconds=30`/`expiresAt`, dialer launch, claimed call/time, idempotency key, revision.
- `calls[]`: IVR `callId`, task or none, task match, IVR action, task path, trusted/expected/masked callers, masked callee, callee confirmation, progress, end reason, recording seconds, upload status, evidence status, append-only events with `{eventId,type,at,source,detail}`.
- One nonterminal task (`PENDING`/`CLAIMED`) per normalized caller globally across workspaces; at most one active call per caller.
- `expiresAt - createdAt === 30000`; claim at `expiresAt-1ms` succeeds; at/after `expiresAt` the task expires and is never reused. Duplicate create/replay never renews. Replacement/cancel only for the caller's own eligible `PENDING` task.
- Atomic claim writes task `CLAIMED` + `claimedCallId/claimedAt` + call + `INBOUND_RECEIVED`/`TASK_CLAIMED` in one validated draft mutation. Rejections leave state byte-identical.
- Task path: `DIAL_TASK_CALLEE` + `calleeConfirmation=NONE`, frozen callee auto-dialed with no voice confirmation.
- No-task path: `ENTER_CALLEE` + `DTMF_REQUIRED`, then `confirmDirectCallee` → `DTMF_CONFIRMED`; no task/historical callee is read or guessed.
- Event order and monotonic time are validated; `eventId` is globally unique and replay is idempotent. `CALL_ENDED` completes a claimed task; `CALL_FAILED` fails it. `RECORDING_RECEIVED`, `EVIDENCE_PROCESSING`, `EVIDENCE_READY` are separate; `EVIDENCE_READY` requires received upload + processing. Pre-bridge failure keeps 0 seconds, no upload, no evidence.
- Strict keys reject forbidden fields (`employees`, auth/session/UI, `code`, account-directory keys) atomically.
- `call-task.evidenceStatus` is explicitly a diagnostic projection; `business.evidence` remains the authoritative evidence list.

### 3.4 Deterministic fixture

`createCallTaskDemoState(now)` contains the four caller bindings and five tasks (`CT-DEMO-0101` completed, `0102` expired/mismatch, `0103` failed/no-answer, `0104` completed for account 002, `0201` foreign-account pending) plus the five mirrored calls (`OC-C2034..C2038`), including the no-task DTMF success `OC-C2037` and mismatch `OC-C2036`. It creates no login, real-name, order, payment or entitlement fact.

## 4. Per-surface behavior

### 4.1 Mini Program (`prototype/onecall-miniapp-pages.html`)

- `dialHandoff` is now the pending-task page: task ID, masked caller/callee, frozen 30 seconds, expiry, render-time remaining, dialer launch state, launch fact, ownership and local-fixture notices.
- Primary launch writes only `dialerLaunchedAt`, then opens `callStatus`; it never advances task/call status. The page supports explicit replacement, cancellation, expiry/retry and error codes (`CALLER_BUSY`, `CALL_IN_PROGRESS`, `PENDING_TASK_EXISTS`, `TASK_EXPIRED`) without leaking another owner's callee.
- New `callStatus` page (index count 63): root `data-call-path`, `data-callee-confirmation`, `data-call-progress`; source-badged facts; task/frozen expiry/claim; upload and evidence separated; truthful waiting/claimed/ringing/recording/ended/evidence/failure copy; “dialer opened” never shown as connected; “evidence available” only at `EVIDENCE_READY`; refresh re-renders the local fixture without fake progression.
- Mini no-task `landlineCall`: root `data-call-path="no-task"`, `data-callee-confirmation="DTMF_REQUIRED"`, canonical no-task copy, no “automatic dial” wording, ownership statement.
- `solidified` is the evidence-available result and attributes trusted caller, bridge/recording/duration to the self-developed IVR and evidence processing to ONECALL.
- Review hooks added: `callTaskSnapshot()`, `loadCallTaskSnapshot(candidate)`, `setCallDemoState(name)` for `waiting|claimed|ringing|recording|ended|evidence|failed-expired|failed-mismatch|failed-no-answer`. Demo states are built with shared mutations and must pass `B.callTaskSnapshot` before assignment. Business hooks and snapshot are unchanged.
- Mini countdown is computed at render time from frozen `expiresAt`; no live interval was added.

### 4.2 Customer Web (`web-prototype/evidence-hub.html`)

- Initializes a separate `callTaskState`.
- Adds a read-only “任务与 IVR 诊断（只读）” panel to the calls page: task and call tables, frozen 30 seconds, task auto-dial/no-confirmation versus no-task DTMF copies, upload/evidence separation, ownership statement, diagnostic-projection note, no inputs/buttons.
- Adds a task-decision micro subline under “呼叫结果” for the historical call table (task ID + auto-dial; no-task DTMF; expired/mismatch; no-answer) and a callinfo diagnostic block for the same `callId`.
- Renames caller/recording/settlement facts to `IVR callId`, `IVR 可信主叫（线路传递）`, `IVR 权威录音时长`, `ONECALL 结算扣减`; updates the timeline to launch → inbound → claim → outbound → ringing/answer/bridge/record → upload → evidence processing/ready.
- No-task call owner/workspace is derived only through `B.uniqueCallerBinding`; missing/ambiguous binding returns `null`, so the call is absent from account views. Other-account tasks/calls are hidden from account 001 personal views.
- Web is read-only: no browser dial, no task edit, no retry upload, no mark-evidence action, no new route; auditor still has no calls route.
- Hooks added: `callTaskSnapshot()`, `loadCallTaskSnapshot(candidate)`, `selectCallDiagnostic(callId)`. Task import re-renders but does not change business/login/workspace state.

### 4.3 Operations (`operations-prototype/onecall-operations-admin.html`)

- Initializes a separate `callTaskState`.
- Adds `#call-diagnostics` to the existing `/ivr-sessions` route: read-only task and call tables (all fixture owners), frozen 30 seconds, task auto-dial/no-confirmation and no-task DTMF examples, ownership statement, key-1 unregistered/unverified trial note (“按 1 开通…不创建登录或实名身份”), purchase-guidance note (GUIDED only, no order/payment), diagnostic-projection note.
- No `data-action`/button/input inside the panel. Forged `edit-task`/`retry-upload`/`mark-evidence` do nothing.
- Review-only selectors “任务自动外呼诊断” / “无任务 DTMF 诊断” only set `state.reviewCallScenario` and navigate/highlight the existing `/ivr-sessions`; they do not create sessions or mutate task/call facts. The existing twelve IVR scenarios are unchanged.
- `sessionPage` now uses the canonical ownership statement.
- Hooks added: `callTaskSnapshot()`, `loadCallTaskSnapshot(candidate)` (admin + normal data mode guard), `setReviewCallScenario('task'|'no-task')`.
- Live `routeDefs` remains **15 routes** (6 primary + 9 secondary); no route added and six primary entries preserved. The historical docs/09 S1 “13 routes” paragraph is retained as history and explicitly labeled as such in the new note; it is not used as an assertion.

## 5. Accepted finding resolution

- **F-01 (route count):** No route added. Verified live `routeDefs`: 15 total, 6 primary (`/dashboard`, `/users`, `/catalog`, `/orders`, `/employees`, `/audit`). QA asserts the six primary entries and no new route. docs/09 now states the live 15-route count and labels the S1 13-route paragraph as historical.
- **F-02 (no-task owner derivation):** Web/Operations call visibility derives owner/workspace only through `B.uniqueCallerBinding(callTaskState, call.normalizedCaller)`. Missing or multiple active bindings return `null` and the call is hidden. Mini call visibility uses task/unique-binding ownership and hides foreign tasks. Fixture `CT-DEMO-0201` is visible only in Operations; Mini/Web account 001 views assert its absence. `uniqueCallerBinding` ambiguity/missing behavior is unit-tested.
- **F-03 (validated review states):** Mini `setCallDemoState` builds states through shared mutations and rejects any candidate failing `B.callTaskSnapshot`. `loadCallTaskSnapshot` validates before atomic replacement on all surfaces. Web `selectCallDiagnostic` only selects existing validated fixture calls; Operations `setReviewCallScenario` only highlights. No arbitrary render states are introduced.
- **F-04 (countdown):** Mini remaining time is computed on render from frozen `expiresAt` and the render/injected time. No `setInterval`/live timer was added. QA asserts TTL/expiry attributes, not tick counts.
- **F-05 (evidence projection):** `call-task.evidenceStatus` is labeled a diagnostic projection in Mini, Web and Operations; `business.evidence` remains authoritative. No referential link is claimed.

## 6. Validation matrix

Environment behavior: the session permission layer rejected **all** Node and PowerShell program executions with `This command requires approval`. Representative attempts are recorded below; the same gate applies to every command in the required list. No exit code, PASS line or test result is available, and none is claimed.

| Command | Result |
| --- | --- |
| `node --check shared/onecall-business.js` | NOT EXECUTED — `This command requires approval` |
| `node --check shared/build-prototypes.js` | NOT EXECUTED — same Node execution gate |
| `node --check prototype/qa/qa.js` | NOT EXECUTED — same Node execution gate |
| `node --check web-prototype/qa/qa.js` | NOT EXECUTED — same Node execution gate |
| `node --check operations-prototype/qa/qa.js` | NOT EXECUTED — same Node execution gate |
| `node --check shared/qa/cross-channel.js` | NOT EXECUTED — same Node execution gate |
| `node --test shared/qa/business.test.js` | NOT EXECUTED — same Node execution gate |
| `node --test shared/qa/call-task.test.js` | NOT EXECUTED — attempted; `This command requires approval` |
| `node --test shared/qa/runner-exit.test.js` | NOT EXECUTED — same Node execution gate |
| `node shared/build-prototypes.js` | NOT EXECUTED — attempted; `This command requires approval` |
| `node shared/build-prototypes.js --check` | NOT EXECUTED — same Node execution gate |
| `powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./prototype/qa/run.ps1` | NOT EXECUTED — attempted; `This command requires approval` |
| `powershell.exe ... ./web-prototype/qa/run.ps1` | NOT EXECUTED — same PowerShell execution gate |
| `powershell.exe ... ./operations-prototype/qa/run.ps1` | NOT EXECUTED — same PowerShell execution gate |
| `node shared/qa/cross-channel.js` | NOT EXECUTED — attempted; `This command requires approval` |
| `git diff --check` | NOT AVAILABLE — worktree `.git` points to `D:/codex-call/.git/worktrees/codex-call`, outside WSL; no Git metadata. No success claimed. |

Read-only checks that did execute (Bash `rg`/`sha256sum`):

| Check | Result |
| --- | --- |
| Mini index buttons | **63** (`rg -c 'class="index-button"'`) |
| Operations `routeDefs` | **15** total; 6 primary (`/dashboard`, `/users`, `/catalog`, `/orders`, `/employees`, `/audit`); no route added |
| Generated marker pairs | exactly **1 START + 1 END** per maintained source |
| Module SHA-256 | `40ccabcdfb713bc9b2e5d65ea8c6cf7bbc712972c50402ff15d6e4a0f0533ac3` |
| Generated block SHA-256 (all three sources) | `c0356ee31a0d5b6af28b79c4409e33f02059d0c06e59a29cc03e289cb155a7e4` |
| `B.labels.ownership` render references | Mini 4, Web 2, Operations 2 |
| Forbidden carrier-attribution wording in the three maintained sources | **0 hits** for the exact regex `运营商.{0,16}(负责\|确认有效\|确认实际\|识别真实\|完成开通\|开通回执\|同步开通\|生成录音\|固证\|计费\|号码归属)` |

**Stale-wording grep nuance:** running the exact regex over the whole repository matches 12 files, but none of the three maintained sources. The other matches are: stale `prototype/preview.html` and `web-prototype/preview.html` (generated copies not regenerated because runners were denied), `web-prototype/qa/qa.js` (the two intentional negative assertions contain the removed strings), this implementation artifact (it quotes the regex), and historical/out-of-scope docs/plan/review files. The three maintained source HTML files are clean; stale previews must be regenerated before review.

**Canonical ownership:** the canonical string lives in `shared/onecall-business.js` and is rendered at runtime through `B.labels.ownership` in all three maintained surfaces (Mini 4 references, Web 2, Operations 2). On a clean build each inlined generated block will also contain the literal, satisfying `运营商提供(号码|线路|通信承载)`; the current inlined blocks are stale.

**Generated/hash result:** the generated blocks are **stale**. The module hash is `40cc…`, while all three inlined blocks still carry the previous `c035…` hash. `node shared/build-prototypes.js` and `--check` could not run, so the previews and inlined blocks must be regenerated before any prototype can load the new `B.createCallTaskDemoState`/`B.labels.ownership` APIs and before `business.test.js` hash equality can pass.

## 7. Screenshot matrix and visual verdicts

No runner executed, so **no new workflow-owned screenshot was generated or inspected this round**. Existing PNGs are historical and are not presented as current evidence. No screenshot was deleted or hand-edited.

| Surface | Planned file / viewport | Verdict |
| --- | --- | --- |
| Mini | `alignment-call-task-393.png` (393×852) | NOT GENERATED / NOT INSPECTED |
| Mini | `alignment-call-status-waiting-393.png` | NOT GENERATED / NOT INSPECTED |
| Mini | `alignment-call-status-claimed-393.png` | NOT GENERATED / NOT INSPECTED |
| Mini | `alignment-call-status-recording-393.png` | NOT GENERATED / NOT INSPECTED |
| Mini | `alignment-call-status-evidence-393.png` | NOT GENERATED / NOT INSPECTED |
| Mini | `alignment-call-status-failed-393.png` | NOT GENERATED / NOT INSPECTED |
| Mini | `alignment-landline-no-task-dtmf-393.png` | NOT GENERATED / NOT INSPECTED |
| Mini | `alignment-call-status-320.png` (320×760) | NOT GENERATED / NOT INSPECTED |
| Mini | `alignment-dial-handoff-320.png`, `alignment-landline-call-320.png` | STALE (not regenerated; old copy/flow) |
| Web | `alignment-call-task-diagnostics-1440.png` (1440×960) | NOT GENERATED / NOT INSPECTED |
| Web | `alignment-call-task-diagnostics-1024.png` (1024×900) | NOT GENERATED / NOT INSPECTED |
| Web | `alignment-call-task-diagnostics-720.png` (720×900) | NOT GENERATED / NOT INSPECTED |
| Web | `alignment-call-task-detail-1440.png` (1440×980) | NOT GENERATED / NOT INSPECTED |
| Web | `alignment-call-no-task-dtmf-1280.png` (1280×900) | NOT GENERATED / NOT INSPECTED |
| Web | `alignment-call-task-failed-1280.png` (1280×900) | NOT GENERATED / NOT INSPECTED |
| Ops | `alignment-ivr-task-diagnostics-1440.png` (1440×960) | NOT GENERATED / NOT INSPECTED |
| Ops | `alignment-ivr-task-diagnostics-1280.png` (1280×900) | NOT GENERATED / NOT INSPECTED |
| Ops | `alignment-ivr-task-diagnostics-720.png` (720×900) | NOT GENERATED / NOT INSPECTED |
| Ops | `alignment-ivr-no-task-dtmf-1440.png` (1440×960) | NOT GENERATED / NOT INSPECTED |
| Cross | `alignment-cross-call-task-393.png`, `alignment-cross-call-task-1280.png`, `alignment-cross-ivr-task-1440.png` | NOT GENERATED / NOT INSPECTED |

The runner scripts now contain 393/320 overflow assertions for the Mini task/status/no-task pages and 1440/1024/720 checks for the Web panel and 720/1024/1280/1440 checks for Operations; they simply could not execute here.

## 8. Docs decision and deliberate no-change

- docs/06 updated to 63 pages, new `callStatus`, task/no-task flow and local-fixture status.
- docs/07 updated with read-only task/IVR diagnostics, call-detail vocabulary, no browser dial and no route.
- docs/09 adds the local-fixture implementation note; live route count stated as 15 (6 primary + 9 secondary); the historical S1 “13 routes” paragraph is retained but labeled historical; no business rules, prices, trial quota or 30-second policy changed.
- docs/10 replaces the “not implemented” status with local-fixture status, updates the page count to 63, adds `call-task.test.js` to the validation command list, and keeps the cross-channel rules unchanged.
- docs/13 updates only the implementation-status note in §11; interfaces, enums, state machine, 30 seconds and production acceptance are unchanged.
- docs/README updates the prototype status and route count.
- `shared/README.md`, `prototype/README.md`, `web-prototype/README.md`, `operations-prototype/README.md` document the call-task domain, hooks, review-only behavior and screenshots, and explicitly state this round’s execution is blocked.
- **docs/11 deliberately unchanged:** its §1/§4 already state that task creation requires login, task selection is a business number, and direct IVR does not require a task. No new rule was needed.

## 9. Residual risks and owners

| Risk | Why it remains | Owner / destination |
| --- | --- | --- |
| No Node/PowerShell validation, build, preview, runner or cross-channel execution. | Session permission layer rejected every program execution (`This command requires approval`). | Controller/CI: rerun the exact §6 commands in a permitted environment. |
| Generated blocks and previews are stale (`c035…` vs module `40cc…`). | `node shared/build-prototypes.js` could not run. | Implementation/CI: run builder, then all three runners; verify `--check`. |
| No screenshot visual inspection. | Runners could not execute; no new PNGs. | Controller/reviewer: regenerate and inspect the §7 matrix at required widths. |
| Atomicity is local only. | Prototype draft-mutation simulation does not prove DB concurrency/locking/rollback. | Backend/IVR integration work unit. |
| Two IVR decision models coexist (accepted session activation demo + task-domain `resolveIvrInbound`). | Preserving the twelve accepted scenarios; no unified production resolve is claimed. | Later IVR-integration work unit. |
| `call-task.evidenceStatus` is not referentially linked to `business.evidence`. | It is a diagnostic projection; the authoritative list remains business evidence. | Evidence-service work unit. |
| Web/Operations diagnostics visibility is role/dept-filtered in the client fixture. | Prototype authorization is not server-enforced. | Web/backend production authorization work. |
| Formal consent, recording notice and ownership wording. | Product/Legal owns final copy. | Product/Legal before production release. |
| Git metadata unavailable from WSL. | `.git` points to `D:/codex-call/.git/worktrees/codex-call`. | Controller-side Windows Git remains authoritative. |

## 10. Completion / blocker state

- Source implementation of the single accepted behavior slice is complete across the shared module, tests, three surfaces, QA scripts, READMEs and permitted docs.
- The implementation gate **cannot be claimed passed**: the builder, four QA programs, unit tests, cross-channel parity and screenshot inspection were all blocked by the environment permission layer; the generated blocks/previews are stale as a direct consequence.
- No production telephony, backend, persistence, upload, payment, cloud or media-engine capability is claimed. Nothing outside the prototype and permitted docs was changed.
- Recommended next step: in a permitted environment, run `node shared/build-prototypes.js`, the three PowerShell runners, `node --test shared/qa/business.test.js`, `node --test shared/qa/call-task.test.js`, `node --test shared/qa/runner-exit.test.js`, `node shared/qa/cross-channel.js`, all `node --check` commands and `node shared/build-prototypes.js --check`; inspect the §7 screenshots; then proceed to code review only after those results are recorded. No gate is advanced here.

## Controller validation fix 1

- Task label: `ivr-prototype-sync-fix1-ds-20261025-01`. Bounded fix of the two controller findings only. No behavior, assertion intent, shared production semantics, controller state, commit, push, PR or gate advancement was changed.

### CF-01 — cross-channel syntax collision

In `shared/qa/cross-channel.js`, the call-task claim fixture reused the identifier `claimed`, which was already declared in the same async function at the earlier Mini subject-claim step. Renamed only the new call-task local to `claimedTaskState` (`const claimedTaskState=copy(miniCreatedState);`) and updated every reference in that block: the `resolveIvrInbound` input, task/call assertions, `recordCallFact`, surface imports and the three parity assertions. The earlier `claimed` snapshot, all assertions and business behavior are unchanged.

### CF-02 — fixture-aware idempotency assertion

In `shared/qa/call-task.test.js` (`test('IVR claim is one atomic mutation and repeated resolutions do not re-claim')`):

- Added `callerCallsBefore = state.calls.filter(item => item.normalizedCaller === '18600004471').length;` immediately before the claim. The deterministic fixture already includes historical mismatch call `OC-C2036` for that caller.
- After the successful claim: assert the caller call count is `callerCallsBefore + 1` and that `CALL-QA-TASK-01` appears exactly once.
- After the competing-call rejection and exact replay: assert the caller call count remains `callerCallsBefore + 1`. The existing `assert.deepEqual(state, afterClaim)` byte-identical checks are retained.
- The historical mismatch fixture and every other assertion are unchanged.

### Commands observed

| Command | Result |
| --- | --- |
| `node --check shared/qa/cross-channel.js` | NOT EXECUTED — `This command requires approval` |
| `node --test shared/qa/call-task.test.js` | NOT EXECUTED — `This command requires approval` |

Program execution remains denied in this session, so no syntax, PASS or unit-test result is claimed. The controller should rerun syntax, unit, builder and all surface QA in a permitted environment.

## Controller validation fix 2

- Task label: `ivr-prototype-sync-fix2-ds-20261025-01`. Bounded fix of CF-03 and CF-04 only. No behavior, shared contract semantics, Mini UI, QA intent, controller state, commit, push, PR or gate advancement was changed.

### CF-03 — Mini landline challenge assertion

In `prototype/qa/qa.js:216`, the landline verification assertion still required the stale `真实主叫` phrase. Replaced that single phrase with the current authoritative UI requirement, so the assertion now requires all five of `95XXX`, `483 621`, `自研 IVR`, `线路传递` and `可信主叫`. The stale carrier-owned wording was not restored and the Mini UI (`prototype/onecall-miniapp-pages.html`) was not modified. The current UI copy (`prototype/onecall-miniapp-pages.html:5651`) reads “自研 IVR 将取得经运营商线路传递的可信主叫…”, so all five checks are satisfied.

### CF-04 — Web task-backed callee confirmation copy

In `web-prototype/evidence-hub.html` inside `callDiagnosticsHtml()`, the `calleeConfirmation === 'NONE'` display value changed from `无需确认（任务自动外呼）` to `无需确认被叫（任务自动外呼）`. The QA assertion requiring `无需确认被叫`, all no-task DTMF copy and all call-path behavior are unchanged.

### Commands observed

| Command | Result |
| --- | --- |
| `node --check prototype/qa/qa.js` | NOT EXECUTED — `This command requires approval` |
| `grep -c "无需确认被叫（任务自动外呼）" web-prototype/evidence-hub.html` | `1` |
| `grep -c "真实主叫" prototype/qa/qa.js` | `0` |

No surface runner was run, as instructed; the controller will regenerate and rerun all suites. Program execution remains denied in this session, so no syntax or PASS result is claimed for the edited QA script.

## Controller validation fix 3

- Task label: `ivr-prototype-sync-fix3-ds-20261025-01`. Bounded fix of CF-05 and CF-06 only. No behavior, shared contract semantics, assertions, events, ordering, product rules, controller state, commit, push, PR or gate advancement was changed.

### CF-05 — cross-channel temporal dead zone

In `shared/qa/cross-channel.js`, the call-event array declared at line 261 shadowed the module-level `facts` helper (line 10) within the same async block, causing `ReferenceError: Cannot access 'facts' before initialization` at the earlier `facts(claimed)` call (line 99). Renamed only that array to `callFacts` and updated the immediately following loop (`for(const [type,offset,extra] of callFacts)`). The module helper, assertions, events and ordering are unchanged.

### CF-06 — current-baseline ownership wording

- `prototype/README.md` current-baseline `首发范围` bullet now reads: “仅支持自研 IVR 能够从运营商线路传递结果取得唯一可信主叫的手机号和独立固话直线”; carrier ownership of the trusted-caller decision is no longer stated.
- `docs/07-web-capability-hub-spec.md` §6.1 list sentence: `运营商确认的实际主叫` → `自研 IVR 从线路传递结果取得的可信主叫`; all other fields and the stats/billing-by-caller rule are retained verbatim.
- `docs/07-web-capability-hub-spec.md` enterprise member usage paragraph (line 225): `成员用量按运营商确认的实际主叫归集` → `成员用量按自研 IVR 从线路传递结果取得的可信主叫归集`, preserving the unconfirmed-landline rule.
- Historical sections at the top of the Web spec and other stale references were left unchanged.

### Commands observed

| Command | Result |
| --- | --- |
| `node --check shared/qa/cross-channel.js` | NOT EXECUTED — `This command requires approval` |
| `grep -n "callFacts\|const facts=" shared/qa/cross-channel.js` | helper at line 10 unchanged; array/loop renamed at lines 261–262 |
| `grep -n "运营商确认的实际主叫" docs/07-web-capability-hub-spec.md` | `0` matches |

No cross-channel or surface runner was run, as instructed; the controller will rerun them. Program execution remains denied in this session, so no syntax or PASS result is claimed.

## Controller validation fix 4

- Task label: `ivr-prototype-sync-fix4-ds-20261025-01`. Bounded fix of CF-07 and CF-08 only. No production API, gate, controller state, commit, push or PR change was made.

### CF-07 — current eligibility before claim and unique-binding fail-closed

In `shared/onecall-business.js::resolveIvrInbound`, the existing same-call replay, untrusted-caller rejection, suspended handling, active-call rejection and `reapExpiredCallTasks` order is preserved. After reaping:

- `uniqueCallerBinding(draft, normalizedCaller)` is resolved before any claim; `null` (missing or ambiguous active binding) returns the existing `fail('REJECT')` style. Because `callTaskMutate` discards a failed draft, no claim, call, state replacement, owner or callee is produced.
- `INACTIVE` and `UNREGISTERED` now return `GUIDE_ACTIVATION` before the pending-task lookup/claim, leaving the pending task PENDING.
- `availableSeconds <= 0` now returns `GUIDE_PURCHASE` before the pending-task lookup/claim, also leaving the task PENDING.
- The unchanged PENDING claim block follows those checks, so an ACTIVE caller with positive seconds still atomically receives `DIAL_TASK_CALLEE` with the frozen callee and `calleeConfirmation: 'NONE'`.
- The `directAllowed !== true` guard moved after the claim block, so it only gates the no-task path; `directAllowed=false` no longer blocks a valid task claim.
- The no-task `ENTER_CALLEE.maskedCaller` is now always `binding.maskedCaller`; the unused `maskNormalizedCaller` fallback helper was deleted.

`shared/qa/call-task.test.js`:

- The existing no-task DTMF test now uses the uniquely bound fixture landline `01088886688` instead of unbound `13700001111`, and asserts the call's `maskedCaller` is `010 **** 6688`.
- New `current caller state and entitlement are revalidated before any task claim`: `INACTIVE` and `UNREGISTERED` each return `GUIDE_ACTIVATION`, and zero seconds returns `GUIDE_PURCHASE`; each case asserts the pending task stays PENDING, no call is created and state is byte-identical.
- New `task claim requires a unique binding but not direct DTMF allowance`: `directAllowed=false` still claims the task and returns `DIAL_TASK_CALLEE` with the frozen callee and no confirmation; missing binding and ambiguous active binding both return `REJECT` with no call or state mutation (the ambiguous case retains a valid pending task and adds a second active binding for the same normalized caller).
- New `no-task path requires direct DTMF allowance and creates no call when disabled`: the bound landline with `directAllowed=false` returns `GUIDE_PURCHASE` with identical state and no call.

`shared/qa/cross-channel.js`: the no-task direct fixture now resolves and confirms with the uniquely bound `01088886688` instead of unbound `13700001111`; CF-07 makes the unbound caller fail closed, so this keeps the accepted no-task path exercised with valid ownership.

### CF-08 — transient toast sequencing

- `web-prototype/qa/qa.js`: immediately before the first `call-task-diagnostics` screenshot, `page.waitForFunction` waits until `#toast` no longer has the `visible` class, so the earlier number-verification toast cannot cover the task tables/details.
- `operations-prototype/qa/qa.js`: after the forged read-only task-action checks and before `ivr-no-task-dtmf-1440`, `page.locator('#toast-root .toast').waitFor({ state: 'detached' })`.
- `shared/qa/cross-channel.js`: immediately before the task-parity screenshots, `web.waitForFunction` waits until the Web `#toast` no longer has the `visible` class.

Toast duration, application behavior, existing assertions, screenshot coverage and fixed sleeps were not changed or added; `#toast-root .toast` detaches only through the existing application toast timer.

### Commands observed

| Command | Result |
| --- | --- |
| `node --check shared/onecall-business.js` | NOT EXECUTED — `This command requires approval` |
| `node --check` for the four other edited JS files (combined invocation) | NOT EXECUTED — rejected part-wise with `This command requires approval` |
| `node --test shared/qa/call-task.test.js` | NOT ATTEMPTED — all `node` execution was already denied by the session permission layer |
| `git diff --stat` | FAILED — `fatal: not a git repository: .../D:/codex-call/.git/worktrees/codex-call` (known WSL metadata limitation; controller-side Windows Git remains authoritative) |

No surface runner or cross-channel suite was run, as instructed. Program execution remains denied in this session, so no syntax or PASS result is claimed. The controller must rebuild the generated blocks/previews, rerun the syntax/unit/surface checks and inspect the regenerated screenshots.

## Controller validation fix 5

- Task label: `ivr-prototype-sync-fix5-ds-20261025-01`. Bounded fix of CF-09 and CF-10 only. No production API, review artifact, control document, controller state, commit, push, PR or gate advancement was changed.

### CF-09 — unique ownership before a SUSPENDED task rejection

In `shared/onecall-business.js::resolveIvrInbound`:

- Same-call replay and untrusted-caller rejection remain the first checks.
- `uniqueCallerBinding(draft, normalizedCaller)` now runs once immediately after the untrusted-caller check and before the `SUSPENDED` branch. The `SUSPENDED` branch fails closed with the existing `REJECT` style when the binding is missing or ambiguous, so no draft write is retained and state is byte-identical.
- The `SUSPENDED` loop no longer selects by normalized caller alone. It rejects only a `PENDING` task whose status, normalized caller, `callerBindingId`, `ownerType`, `ownerId` and `workspaceId` all match the unique binding. `taskRejected` is `true` only for that owned task; `REJECT_SUSPENDED` still returns `callId: null` and `maskedCallee: null` and creates no call.
- For non-SUSPENDED callers the existing order is preserved: active-call rejection, then `reapExpiredCallTasks`, then the pre-existing missing/ambiguous `REJECT`, then activation/purchase guidance, task claim and no-task DTMF. The later binding resolution was removed because the binding is now resolved once above; a second `!binding` rejection remains at the original post-reap point so active-call precedence is unchanged.

`shared/qa/call-task.test.js` adds one focused test, `SUSPENDED resolves unique ownership before rejecting a pending task`, with the four required cases:

- unbound caller `13700001111` + `SUSPENDED` → `REJECT`, byte-identical state, no call;
- pending task for `18600004471` plus a second active binding → `SUSPENDED` returns `REJECT`, byte-identical state, task stays `PENDING`, no call;
- unique binding + owned pending task → `REJECT_SUSPENDED`, `taskRejected: true`, task `REJECTED/SUSPENDED`, foreign `CT-DEMO-0201` stays `PENDING`, call count unchanged and no new call;
- unique binding with no pending task (`01088886688`) → `REJECT_SUSPENDED`, `taskRejected: false`, byte-identical state, no call.

### CF-10 — computed-opacity Toast wait

- `web-prototype/qa/qa.js`: the wait before the first call-task diagnostic screenshot now resolves only when `#toast` is absent or `getComputedStyle(toast).opacity === '0'`. No fixed sleep added.
- `shared/qa/cross-channel.js`: the wait immediately before the task-parity screenshots uses the same computed-opacity condition for the Web Toast. No fixed sleep added.
- The Operations detachment wait was left unchanged. Application toast duration and CSS (`web-prototype/evidence-hub.html`) were not modified; `.toast { opacity: 0; transition: .2s }` is unchanged.

### Commands observed

| Command | Result |
| --- | --- |
| `node --check shared/onecall-business.js` (chained with the other three `--check` commands) | NOT EXECUTED — `This command requires approval` |
| `node --check shared/onecall-business.js` (single) | NOT EXECUTED — `This command requires approval` |
| `node --test shared/qa/call-task.test.js` | NOT EXECUTED — `This command requires approval` |
| `git status --porcelain && git diff --stat` | FAILED — `fatal: not a git repository: .../D:/codex-call/.git/worktrees/codex-call` (known WSL metadata limitation; controller-side Windows Git remains authoritative) |

No surface runner or cross-channel suite was run, as instructed. Program execution remains denied in this session, so no syntax or PASS result is claimed. The controller must rerun the four `node --check` commands and `node --test shared/qa/call-task.test.js`, regenerate the Web/cross-channel screenshots, and inspect the at-original-size results.
