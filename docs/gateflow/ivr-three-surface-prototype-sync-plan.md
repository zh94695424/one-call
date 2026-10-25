# IVR Three-Surface Prototype Sync — Implementation Plan

- Work unit: `ivr-three-surface-prototype-sync`
- Task label: `ivr-prototype-sync-plan-ds-20261025-01`
- Control: `docs/gateflow/ivr-three-surface-prototype-sync-control.md`
- Base: `origin/main` at `754b7dfc53c277e79c5d63e24d578a93fa557cda`
- Slice count: **1**
- Gate: plan only; no implementation, review, commit, push, PR, or gate advancement.

## 0. Required-reading evidence and blocker

Read scope: the control document, docs 06/07/09/10/11/13, all three prototype READMEs, `shared/onecall-business.js`, `shared/build-prototypes.js`, and `shared/qa/cross-channel.js` were read in full. The three prototype sources were read across their application scripts and all IVR/call/task/return/diagnostic page code; non-flow HTML/CSS scaffolding was inspected only for page ownership and styling conventions. The Mini and Operations `qa/qa.js` files were read in full; the Web `qa/qa.js` was read across its login/session, calls, role, responsive and final assertion sections.

**Blocker / unread required reading:** the two skill files:

- `C:/Users/Administrator/.codex/skills/onecall-ui-development/SKILL.md`
- `C:/Users/Administrator/.codex/skills/onecall-ui-development/references/prototype-mode.md`

are outside this session's allowed working directory and were hard-blocked by the harness (Read permission and sandboxed read both rejected). This plan therefore relies on the in-repo prototype conventions and the existing generated-block/README rules. Implementation must not start until a reviewer either grants access or confirms these two files add no constraints beyond this plan. If the skill files contain stricter prototype-mode rules, this plan's UI ownership and QA sections must be amended before code generation.

**Environment blocker (non-plan):** this worktree's `.git` points at `D:/codex-call/.git/worktrees/codex-call`, unavailable from WSL, so `git status/diff/check` cannot run here. The implementation/review environment must run the git checks in §7.3.

## 1. Direct code evidence

| Evidence | Location | Consequence |
| --- | --- | --- |
| Shared business state has no task/call domain; only IVR activation/trial sessions and evidence | `shared/onecall-business.js:10-18`, `:480-554`, `:1107-1136` | A separate validated call-task demo domain is needed; do not overload the accepted business snapshot. |
| Business snapshot is strict (`schemaVersion 1`, exact fields, event/domain coverage) | `shared/onecall-business.js:631-718` | Adding task/call arrays to it would force a schema bump or weaken accepted checks. Keep the domains separate. |
| Mini dialer handoff jumps straight to `solidified` or `trialLimit` | `prototype/onecall-miniapp-pages.html:4703-4710`; QA asserts it at `prototype/qa/qa.js:332-336` | Replace with frozen task → dialer-launch-only → truthful return states. Remove the old success assertion. |
| Mini has no task, claim, ringing, recording or upload state | `prototype/onecall-miniapp-pages.html:4694-4710`, page map `:4897-4935` | Add one `callStatus` page and repurpose `dialHandoff` as the pending-task/handoff page. |
| Mini still attributes caller verification, recording and evidence to the carrier | `prototype/onecall-miniapp-pages.html:4304,4434,4691,4700,4706,4710,4836,4915,4920,4925,4935,4960,5038` | Replace with the canonical three-party ownership statement. |
| Web calls are static historical fixtures with no task/decision chain | `web-prototype/evidence-hub.html:1854-1860`, `:2254-2290` | Add task/IVR diagnostics from a shared call-task fixture; keep Web read-only. |
| Web attributes actual caller/recording to the carrier | `web-prototype/evidence-hub.html:2257,2286,2288` | Replace with “IVR trusted caller/authoritative recording” wording. |
| Ops IVR pages show activation/trial sessions only; no task/call diagnostics | `operations-prototype/onecall-operations-admin.html:1273-1274`, `:1291-1292` | Add a read-only task/call diagnostic panel on `/ivr-sessions`; no new route. |
| Ops session detail uses stale ownership copy | `operations-prototype/onecall-operations-admin.html:1292` | Replace with the canonical ownership statement. |
| Ops QA covers twelve activation/trial IVR scenarios but no task path | `operations-prototype/qa/qa.js:111-138` | Preserve all twelve; add task/no-task diagnostic assertions. |
| Cross-channel handoff compares only `sessions/evidence/trialGrants` | `shared/qa/cross-channel.js:10,90-107` | Add a second independently validated call-task snapshot; prove neither import changes the other. |
| Build owns one generated inline block per source and rejects drift | `shared/build-prototypes.js:7-15,34-47` | Edit only `shared/onecall-business.js`; run the builder. Never hand-edit the block or `preview.html`. |
| Docs still say prototypes do not implement the task flow | `docs/10-cross-channel-rules.md:3`, `docs/06-miniapp-ui-spec.md:7`, `docs/07-web-capability-hub-spec.md:7`, `docs/13-ivr-call-task-and-inbound-design.md:330` | Update status/implementation notes only; do not change accepted contracts or numbers. |

## 2. Goal, non-goals and invariants

### 2.1 Binding goal

1. Keep the accepted no-task path: only when there is no valid pending task may the IVR collect and confirm a callee by DTMF.
2. Show one current nonterminal task per normalized caller in all three surfaces, default **30 seconds**, frozen by the server at creation.
3. Show the IVR atomically claiming the task, then automatically dialing the frozen callee with **no voice/dial-key confirmation of the callee**.
4. Show truthful return/diagnostic states: dialer launch, inbound wait, claim, B-leg outbound, ringing, answered, bridged, recording, call end, upload and evidence processing are separate facts.
5. Use one ownership vocabulary: the carrier supplies number, line and transport; the self-developed IVR handles inbound, B-leg dialing, bridge, recording, time limiting and call/recording upload; ONECALL owns activation, task, entitlement and evidence processing.
6. Preserve key-1 unregistered/unverified first-trial activation and evidence without creating a login or real-name identity; IVR purchase remains guidance only.

### 2.2 Non-goals

No production runtime/backend, no A0/A1/A2 contract edits, no media engine/SIP/provider implementation, no cloud/payment integration, no PBX/extensions/seat PINs, no online work orders/refunds/invoices, no production prices, no new timeout numbers, no broad visual redesign.

### 2.3 Invariants

- **One nonterminal task per normalized caller across all accounts/workspaces.** A task is nonterminal when `PENDING` or `CLAIMED`; an active call is any call in `WAITING_CALLEE_INPUT`, `CALLEE_CONFIRMED`, `CLAIMED`, `OUTBOUND_STARTED`, `RINGING`, `ANSWERED`, `BRIDGED` or `RECORDING`.
- **30 seconds only.** `CALL_TASK_TTL_SECONDS = 30`; create freezes `ttlSeconds` and `expiresAt`; no helper accepts a caller-supplied TTL; configuration changes are not modeled.
- **No renewal.** Retry with the same idempotency key/input returns the original task and original `expiresAt`; explicit replacement is a new task and marks the old one `REPLACED`.
- **Expiry authority.** `now < expiresAt` is claimable; `now >= expiresAt` is not. The client clock cannot extend a task. An expired task is never reused and its historical callee is never guessed.
- **Atomic claim.** Resolve/claim performs every check and both writes (task `CLAIMED` + call record) in one draft mutation; any rejection leaves the prior state byte-identical.
- **Task path ≠ no-task path.** `DIAL_TASK_CALLEE` + `calleeConfirmation=NONE`; `ENTER_CALLEE` + `calleeConfirmation=DTMF_CONFIRMED`. Neither UI nor QA may blur them.
- **Dialer launch ≠ connection.** `dialerLaunchedAt` is a mini/client fact only. It cannot advance task or call progress, create evidence, or imply ringing/answer.
- **Immutable communication facts.** Call facts are append-only events with unique `eventId`, monotonic time, allowed transitions and no rewrite. Upload and evidence status are separate from task completion.
- **Account/workspace isolation.** Task/call fixture data is outside the business snapshot but still filtered by the current account/workspace for Mini and Web; operations diagnostics are read-only and may see the full fixture.
- **No fake backend success.** All task/call/upload/evidence states are local demo fixtures. “Refresh” must say it is not connected; only review hooks/fixture imports may change demo states.

## 3. Slice decision

**One slice.** The shared contract, three surfaces, cross-channel handoff and QA are one behavior. Splitting by surface would require temporary divergent fixtures, duplicate vocabulary and two reviews of the same 30-second/claim semantics. Splitting the shared contract from the surfaces would leave no runnable acceptance. One slice keeps the review boundary at “one task path plus one no-task path, shown consistently and truthfully.”

## 4. Shared call-task demo contract

### 4.1 Placement and isolation

Add the contract to `shared/onecall-business.js` (same UMD module and generated block), but **outside** the business snapshot:

- Do **not** add task/call fields to `BUSINESS_FIELDS` or `schemaVersion`.
- Keep the accepted business state and event domains untouched. This follows the existing account-directory precedent (separate read-only fixture domain) and avoids destabilizing the verified snapshot/event validators.
- The call-task domain has its own `{ schemaVersion: 1, ttlSeconds: 30, callerBindings, tasks, calls, sequence }` shape and its own `callTaskSnapshot` / `loadCallTaskSnapshot` validation. It is explicitly a local review fixture, not a backend or telephony claim.
- Cross-channel QA must prove both directions: importing a call-task snapshot never changes the business snapshot, and importing a business snapshot never changes the call-task snapshot.

### 4.2 Constants, labels and canonical copy

Export these from the module so all surfaces render identical vocabulary:

```text
CALL_TASK_TTL_SECONDS = 30

task status:
PENDING 待呼入 | CLAIMED 已领取 | COMPLETED 已完成 | FAILED 失败
EXPIRED 已过期 | REPLACED 已替换 | CANCELLED 已取消 | REJECTED 已拒绝

IVR action:
DIAL_TASK_CALLEE 任务自动外呼 | ENTER_CALLEE 无任务 DTMF 输入
GUIDE_ACTIVATION 引导开通 | GUIDE_PURCHASE 引导购买
REJECT 拒绝 | REJECT_CALL_IN_PROGRESS 呼叫处理中 | REJECT_SUSPENDED 业务已停用

task match:
CLAIMED 已领取 | NONE 无任务 | MISMATCH 主叫不匹配
EXPIRED 已过期 | REJECTED 已拒绝 | CONFLICT 数据冲突

call progress:
WAITING_INBOUND 等待呼入 | CLAIMED 任务已领取
WAITING_CALLEE_INPUT 等待被叫输入 | CALLEE_CONFIRMED 被叫已确认
OUTBOUND_STARTED 已启动 B 路外呼 | RINGING 被叫振铃
ANSWERED 被叫已接听 | BRIDGED 已桥接 | RECORDING 录音中
ENDED 通话已结束 | RECORDING_RECEIVED 录音已接收
EVIDENCE_PROCESSING 证据处理中 | EVIDENCE_READY 证据可用 | FAILED 通话失败

recording upload:
NOT_STARTED 未上传 | UPLOADING 上传中 | RECEIVED 已接收 | FAILED 上传失败

evidence processing:
NOT_CREATED 未创建 | PROCESSING 处理中 | AVAILABLE 可用 | FAILED 处理失败
```

Canonical ownership copy (export as `labels.ownership`):

> 运营商提供号码、线路和通信承载；自研 IVR 负责呼入接听、B 路外呼、双向桥接、录音、限时挂断及通话/录音上传；ONECALL 负责业务开通、待拨任务、权益与证据处理。

Canonical path copy:

- Task path: **“任务路径：IVR 原子领取后按冻结被叫自动外呼，电话中不要求确认被叫号码。”**
- No-task path: **“无任务路径：只有没有有效待拨任务时，IVR 才引导主叫通过 DTMF 输入并确认被叫；不读取或猜测历史任务被叫。”**
- Launch fact: **“系统拨号盘已打开仅表示客户端调用了系统拨号能力，不代表已接通、已领取或正在录音。”**
- Local fixture: **“本地演示/评审状态，不连接真实线路、媒体引擎、对象存储或后台。”**

### 4.3 State shape

```text
callerBindings[]:
  id, ownerType(PERSONAL_ACCOUNT|ORGANIZATION), ownerId, workspaceId,
  maskedCaller, normalizedCaller, numberType(MOBILE|LANDLINE), active

tasks[]:
  id, callerBindingId, normalizedCaller, maskedCaller, maskedCallee,
  ownerType, ownerId, workspaceId, createdByAccountId,
  businessType(EVIDENCE_CALL), status, reasonCode,
  ttlSeconds(30), createdAt, expiresAt, dialerLaunchedAt,
  claimedCallId, claimedAt, idempotencyKey, revision

calls[]:
  id (IVR callId), taskId|null, taskMatch, action, taskPath,
  normalizedCaller, expectedCallerMasked|null, maskedCaller, maskedCallee|null,
  calleeConfirmation(NONE|DTMF_REQUIRED|DTMF_CONFIRMED|NOT_APPLICABLE),
  progress, endReason, recordingSeconds,
  uploadStatus, evidenceStatus,
  events[]: { eventId, type, at, source(MINI|IVR|ONECALL), detail }
```

Allowed call event types and order:

```text
INBOUND_RECEIVED (IVR)            # both paths, first event
TASK_CLAIMED (ONECALL)            # task path only
DTMF_CALLEE_CONFIRMED (IVR)       # no-task path only
OUTBOUND_STARTED (IVR)
RINGING (IVR)
ANSWERED (IVR)
BRIDGED (IVR)
RECORDING_STARTED (IVR)
CALL_ENDED (IVR)
RECORDING_RECEIVED (ONECALL)
EVIDENCE_PROCESSING (ONECALL)
EVIDENCE_READY (ONECALL)
CALL_FAILED (IVR or ONECALL, with reason)
```

`dialerLaunchedAt` is stored on the task, not as an IVR call event. `EVIDENCE_READY` is rejected unless upload is `RECEIVED` and evidence passed through `EVIDENCE_PROCESSING`. `CALL_FAILED` before bridge must leave `recordingSeconds = 0`, upload `NOT_STARTED`, evidence `NOT_CREATED`.

### 4.4 Demo fixtures

`createCallTaskDemoState(now)` returns a validated fixture with:

| Binding | Owner | Workspace | Caller | Type |
| --- | --- | --- | --- | --- |
| `CB-PERSONAL-6208` | `ACCOUNT-DEMO-001` | `personal:ACCOUNT-DEMO-001` | `138 **** 6208` | MOBILE |
| `CB-PERSONAL-4471` | `ACCOUNT-DEMO-001` | `personal:ACCOUNT-DEMO-001` | `186 **** 4471` | MOBILE |
| `CB-ORG-6688` | `ORG-HUAQI-001` | `organization:ORG-HUAQI-001` | `010 **** 6688` | LANDLINE |
| `CB-PERSONAL-7812` | `ACCOUNT-DEMO-002` | `personal:ACCOUNT-DEMO-002` | `139 **** 7812` | MOBILE |

| Task | Status | Purpose |
| --- | --- | --- |
| `CT-DEMO-0101` | COMPLETED | Task-backed success for `OC-C2038` |
| `CT-DEMO-0102` | EXPIRED | Unclaimed task linked to the caller-mismatch diagnostic |
| `CT-DEMO-0103` | FAILED | Task claimed, B-leg no answer, no evidence/debit |
| `CT-DEMO-0104` | COMPLETED | Task-backed success for `OC-C2034` |
| `CT-DEMO-0201` | PENDING | Other-account nonterminal task; must be hidden from account 001 Mini/Web |

Calls mirror the Web historical fixture IDs so the three surfaces can display the same facts:

| Call | Task path | Decision | Progress | Truthful result |
| --- | --- | --- | --- | --- |
| `OC-C2038` | `CT-DEMO-0101` | `DIAL_TASK_CALLEE`, match CLAIMED | EVIDENCE_READY | 522s, upload RECEIVED, evidence AVAILABLE |
| `OC-C2037` | null | `ENTER_CALLEE`, match NONE | EVIDENCE_READY | DTMF-confirmed callee, 1038s, upload RECEIVED, evidence AVAILABLE |
| `OC-C2036` | `CT-DEMO-0102` | `REJECT`, match MISMATCH | FAILED | expected caller differs; no claim/outbound/evidence/debit |
| `OC-C2035` | `CT-DEMO-0103` | `DIAL_TASK_CALLEE`, match CLAIMED | FAILED | no answer; 0s, no evidence/debit |
| `OC-C2034` | `CT-DEMO-0104` | `DIAL_TASK_CALLEE`, match CLAIMED | EVIDENCE_READY | 744s, upload RECEIVED, evidence AVAILABLE |

No fixture creates a login identity, real-name profile, order, payment or entitlement.

### 4.5 Functions and transitions

```text
createCallTask(state, input, now)
  input: callerBindingId, maskedCallee, businessType=EVIDENCE_CALL,
         idempotencyKey, replacePending=false
  - resolve account/workspace/owner from the binding; never accept client owner fields
  - reap expired PENDING tasks for the normalized caller
  - same key + same fingerprint -> DUPLICATE original task (no renewal)
  - same caller/callee/workspace repeated while the original is still PENDING ->
    DUPLICATE original PENDING (no renewal)
  - other owner/workspace PENDING -> CALLER_BUSY; no callee/owner leak
  - CLAIMED or active call -> CALL_IN_PROGRESS
  - own PENDING with a different callee: only with replacePending=true,
    mark old REPLACED and create a new PENDING
  - freeze ttlSeconds=30 and expiresAt=createdAt+30s; never accept ttl input

cancelCallTask(state, {taskId, accountId, workspaceId}, now)
  - only own PENDING -> CANCELLED; otherwise reject

resolveIvrInbound(state, {callId, normalizedCaller, callerTrusted,
                          callerState, availableSeconds, directAllowed}, now)
  - same callId -> DUPLICATE first decision, no re-claim
  - no trusted caller -> REJECT, no state change
  - suspended -> REJECT_SUSPENDED; if a PENDING task exists, REJECTED
  - active call for caller -> REJECT_CALL_IN_PROGRESS
  - unique valid PENDING task and caller matches -> atomically:
      task CLAIMED + claimedCallId/claimedAt; call with action DIAL_TASK_CALLEE,
      taskMatch CLAIMED, taskPath=true, calleeConfirmation=NONE,
      progress CLAIMED, events INBOUND_RECEIVED + TASK_CLAIMED;
      return the frozen masked callee and no-confirmation flag
  - no valid task (none/expired) -> create a call with action ENTER_CALLEE,
      taskPath=false, calleeConfirmation=DTMF_REQUIRED, progress
      WAITING_CALLEE_INPUT, first event INBOUND_RECEIVED; return the DTMF-required
      action; do not return any historical/frozen callee
  - no recorded seconds -> GUIDE_PURCHASE
  - inactive/unregistered -> GUIDE_ACTIVATION

confirmDirectCallee(state, {callId, normalizedCaller, maskedCallee,
                            eventId, taskId must be null}, now)
  - only for an existing ENTER_CALLEE call in WAITING_CALLEE_INPUT; append
    DTMF_CALLEE_CONFIRMED, set calleeConfirmation=DTMF_CONFIRMED and
    progress CALLEE_CONFIRMED; reject any non-null taskId; duplicate eventId
    returns the first result

recordCallFact(state, {callId, eventId, type, at, detail}, now)
  - append-only; duplicate eventId returns the first result;
    conflicting duplicate input fails atomically
  - enforce the allowed order and monotonic `at`
  - CALL_ENDED -> task COMPLETED; CALL_FAILED -> task FAILED
  - RECORDING_RECEIVED -> upload RECEIVED
  - EVIDENCE_PROCESSING -> evidence PROCESSING
  - EVIDENCE_READY -> evidence AVAILABLE only after upload RECEIVED
  - failed before bridge -> no recording, upload, evidence or debit

callTaskSnapshot(state) / loadCallTaskSnapshot(state, candidate)
  - strict clone/validate/replace; atomic rejection with no partial write
```

### 4.6 Validation invariants

Validate on every mutation and import:

1. Exact top-level keys; independent call schema version 1; `ttlSeconds === 30`; unique IDs/keys.
2. At most one nonterminal task per `normalizedCaller` across all workspaces; at most one active call per caller, where active means `WAITING_CALLEE_INPUT`, `CALLEE_CONFIRMED`, `CLAIMED`, `OUTBOUND_STARTED`, `RINGING`, `ANSWERED`, `BRIDGED` or `RECORDING`.
3. Task status/time consistency: `expiresAt - createdAt === 30000`; `CLAIMED` has `claimedCallId`/`claimedAt`; terminal statuses never return to `PENDING`; frozen fields cannot be rewritten.
4. Call/task consistency: `DIAL_TASK_CALLEE` requires a task and `calleeConfirmation=NONE`; `ENTER_CALLEE` requires `taskId=null` and `calleeConfirmation=DTMF_REQUIRED` while `WAITING_CALLEE_INPUT`, or `DTMF_CONFIRMED` from `CALLEE_CONFIRMED` onward; a mismatch call may reference an unclaimed/expired task but must not claim it.
5. Event uniqueness/monotonicity/allowed order; derived `progress` matches the last fact; no evidence before upload/processing; no debit/evidence on pre-bridge failure.
6. Forbidden fields (`employees`, account directory, auth/identity/session/UI, `code`) are rejected atomically. The call-task domain never contains those fields.

## 5. Surface changes

Use existing mobile screen classes, desktop panels, tables, pills, status icons and typography. No new design system, gradients, animation loops, decorative cards or broad layout redesign. New content must fit the existing 393/320px, 1024/1280/1440px and 720px review widths.

### 5.1 Mini Program — `prototype/onecall-miniapp-pages.html`

**Editable area:** the application script after the generated block ends at `:4278`. Never edit `:3138-4278` (generated shared block) or `prototype/preview.html`.

1. **Initialize the shared call-task demo state** near `business` (`:4290`) with `B.createCallTaskDemoState(new Date())`. Map `phone-6208`/`phone-4471` to `CB-PERSONAL-6208`/`CB-PERSONAL-4471`.
2. **Repurpose `dialHandoff` as the pending-task page** (`:4703-4707`, page title at `:4908`):
   - On entry from `call`/`trialCall`, create/reuse the task through `B.createCallTask` with `replacePending:false`; do not navigate to a success page.
   - Show `data-task-id`, status `待呼入`, masked caller/callee, `data-task-ttl="30"`, `data-task-expires-at`, server-frozen expiry and a presentation-only countdown. Show `dialerLaunchedAt` if already set.
   - Add the launch-fact and ownership notices.
   - Primary `data-action="launch-dialer"`: set `dialerLaunchedAt` only, then open **`callStatus`**. It must not change task status or call progress.
   - Secondary “更换本次被叫（演示）”: call `createCallTask` with a different masked callee and `replacePending:true`; re-render. Text action “取消待拨任务”: `cancelCallTask`, show `已取消`.
   - On `CALLER_BUSY`, `CALL_IN_PROGRESS`, `PENDING_TASK_EXISTS` or expiry, show the specific code message without leaking another owner’s callee/account. Expired: disable launch and offer “重新发起”.
3. **Add one page `callStatus`** (title “呼叫状态”, `back:true`, index button + QA count 63):
   - Root carries `data-call-path` (`task`/`no-task`), `data-callee-confirmation` (`NONE`/`DTMF_CONFIRMED`/`DTMF_REQUIRED`) and `data-call-progress`.
   - Header always says “系统拨号盘已打开” when `dialerLaunchedAt` exists, with “尚未接通” until `ANSWERED`; never show “证据已固化” before `EVIDENCE_READY`.
   - Timeline/rows with source badges: 小程序（仅拨号盘事件）、自研 IVR（通信事实）、ONECALL（任务/权益/证据）。Show task ID, frozen 30s/expiry, owner statement, and the appropriate path notice.
   - State copy:
     - `waiting`: 等待呼入；30 秒内未呼入将失效，不会猜测历史被叫。
     - `claimed`: IVR 已原子领取；按冻结被叫自动外呼；电话中不会要求确认被叫。
     - `ringing`: 被叫振铃，尚未接通。
     - `recording`: A/B 已桥接，自研 IVR 录音中；限时与上传由 IVR 执行。
     - `ended`: 通话已结束；录音上传与证据处理是独立状态。
     - `evidence`: ONECALL 已完成证据处理；只有此状态可进入 `solidified`/证据详情。
     - `failed`: show reason (`TTL_EXPIRED`, `CALLER_MISMATCH`, `NO_ANSWER`, `BUSY`, `OUTBOUND_FAILED`); show “未形成证据、未扣有效录音”.
   - “返回小程序后刷新” re-renders the local fixture and states “原型未连接服务端；状态来自本地评审 fixture”. No fake progression.
   - Review hook `setCallDemoState(name)` selects `waiting|claimed|ringing|recording|ended|evidence|failed-expired|failed-mismatch|failed-no-answer`.
4. **Update `landlineCall`** (`:4915`) to the no-task path:
   - State “无待拨任务 · DTMF 输入并确认被叫”; add canonical no-task path copy and ownership statement.
   - Root carries `data-call-path="no-task"` and `data-callee-confirmation="DTMF_REQUIRED"` (the instruction page has not confirmed a callee yet).
   - Remove “运营商识别/运营商确认/运营商作为号码归属…依据” claims; keep “运营商提供号码、线路与承载”.
   - Keep the existing steps; step 3 is the DTMF callee entry, explicitly not task auto-dial.
5. **Update `solidified`** (`:4709-4711`) to attribute actual caller, bridge, recording and duration to the self-developed IVR, and evidence processing to ONECALL. Escalate to this page only from `callStatus` evidence state.
6. **Replace stale copy** in `captureReady` (`:4836`), `callerSelect` (`:4691`), `callPreparationHtml` (`:4698-4700`), `dialHandoff` (`:4703-4707`), `solidified` (`:4710`), `landlineCall` (`:4915`), `callerMismatch` (`:4920`), `callerUnavailable` (`:4925`), `detail` (`:4935`), `usageDetail` (`:4960`), `privacy` (`:5038`), `introWave` (`:4304`), `captureHomeHtml` (`:4434`), and `landlineBind`/`landlineVerify` (`:4792-4797`). Use `B.labels.ownership` and the canonical path copies. Allowed residual carrier claims are only “运营商提供号码、线路和通信承载” and “经运营商线路传递的可信主叫”.
7. **Review hooks** on `window.__onecallMini`: `callTaskSnapshot()`, `loadCallTaskSnapshot(candidate)`, `setCallDemoState(name)`. Existing business hooks remain unchanged. The call-task hooks must not alter the business snapshot.
8. **Page count** becomes **63** (one new `callStatus` index button). Update the README, docs 06/10 and the QA count/log.

### 5.2 Customer Web — `web-prototype/evidence-hub.html`

**Editable area:** application script after the generated block ends at `:1782`. Never edit `:642-1782` or `preview.html`.

1. Initialize `callTaskState = B.createCallTaskDemoState(new Date())` near the existing business state (`:1917`).
2. Add a read-only **“任务与 IVR 诊断（只读）”** panel to `callsPage` (`:2254-2260`), above the historical table:
   - Task rows: ID, masked caller, owner/workspace (desensitized), status, frozen 30s/expiry, claimed callId, mini `dialerLaunchedAt`.
   - Call rows: callId, task or “无任务”, decision/match, progress, recording seconds, upload, evidence processing, end reason.
   - Filter by the current account/workspace: personal/member = own account; enterprise admin/evidence manager = current organization scope; auditor has no calls route. Other-account tasks (`CT-DEMO-0201`) must not appear in account 001 personal views.
   - No input/button inside the panel; no browser dial, no task edit, no “retry upload”, no “mark evidence complete”.
   - Render `B.labels.ownership` and both path copies. Task rows carry `data-call-path="task"`, `data-callee-confirmation="NONE"`; no-task rows carry `data-call-path="no-task"` and `data-callee-confirmation="DTMF_REQUIRED"` or `DTMF_CONFIRMED` according to the call state.
3. Add a compact task/decision subline under the call table’s “呼叫结果” cell (`callTable`, `:2262`):
   - task path: `CT-… · 已领取 · 自动外呼（无需确认被叫）`
   - no-task: `无任务 · DTMF 输入并确认被叫`
   - expired/mismatch: `任务过期 · 未外呼`
   - unanswered: `已领取 · 被叫未接 · 未形成证据`
   To avoid 1024px overflow, use a micro subline, not a new column.
4. In `callDetailPage` `callinfo` tab (`:2286`), add the same task/IVR diagnostic block for that callId:
   - Task ID/status/frozen expiry/claimed callId/dialer event.
   - Decision and match; task path copy vs no-task copy.
   - IVR facts: inbound, B-leg outbound, ringing, answered, bridge, recording, end, duration; upload status.
   - ONECALL processing: evidence processing/availability. Keep upload and evidence separate.
   - Failure/mismatch/unanswered: show “未形成证据、未扣录音额度” and no share/apply actions.
   - Rename labels: “IVR callId”, “IVR 可信主叫（线路传递）”, “IVR 权威录音时长”, “ONECALL 结算扣减”.
5. Update `timeline` tab (`:2288`) to include the truthful order: mini creates task/opens system dialer (client) → IVR inbound → ONECALL claim → IVR auto-dials frozen callee (no confirm) → ringing/answered/bridged/recorded → upload received → ONECALL evidence processing/ready. For no-task: IVR DTMF input → callee confirmed → IVR outbound. For failed: no claim/outbound or no answer, no evidence/debit.
6. Replace stale carrier wording in `callsPage` description (`:2257`), evidence sources (`:1845,1851`), call detail (`:2286,2288`), number verification toast/labels (`:2910,2918`) and any other `运营商` claim found by §7.5. Use `B.labels.ownership`.
7. Add read-only review hooks: `callTaskSnapshot()`, `loadCallTaskSnapshot(candidate)`, `setCallDemoState(name)` (or `selectCallDiagnostic(callId)`). Loading a call-task fixture must re-render but must not alter business data, login or workspace context.
8. No new Web route and no browser-controlled telephone flow.

### 5.3 Operations — `operations-prototype/onecall-operations-admin.html`

**Editable area:** application script after the generated block ends at `:1163`. Never edit `:23-1163` or `preview.html`.

1. Initialize `callTaskState = B.createCallTaskDemoState(new Date())` near `business` (`:1190`).
2. Add a read-only **task/call diagnostics panel** to `/ivr-sessions` (`:1273`):
   - Task table: task ID, masked caller, owner/workspace, status, frozen 30s/expiry, claimed callId, `dialerLaunchedAt`.
   - Call table: callId, task/no-task, decision/match, progress, recording seconds, upload, evidence processing, end reason.
   - Include `DIAL_TASK_CALLEE` example with `calleeConfirmation=NONE` (“任务自动外呼；无需确认被叫”), `ENTER_CALLEE` example with `DTMF_CONFIRMED` (“无任务 DTMF；主叫已确认被叫”), mismatch with no claim/outbound/evidence, and no-answer failure.
   - Add the canonical ownership statement, key-1 first-trial note (“无待拨任务的未注册/未实名主叫仍可按 1 开通并领取一次试用；不创建登录或实名身份”) and purchase guidance note (“只记录引导，不创建订单/支付”).
   - No `data-action` inside the diagnostic panel; no edit/retry/reassign controls.
3. Add a review-only selector or two buttons in the review controls: “任务自动外呼诊断” and “无任务 DTMF 诊断”. They only set `state.reviewCallScenario` and navigate to `/ivr-sessions`, highlighting the matching row. They do not mutate task/call facts or create IVR sessions. Keep the existing “IVR 流程演示” twelve scenarios unchanged.
4. Update `sessionPage` (`:1292`) ownership copy to `B.labels.ownership`; keep the activation/trial panels and the twelve-scenario demo behavior (key 1, unverified/unregistered, repeat, exhausted, conflict, recycled, purchase guidance).
5. Add read-only review hooks: `callTaskSnapshot()`, `loadCallTaskSnapshot(candidate)` (admin + normal data mode, like the business import guard), `setReviewCallScenario(name)`. They must not expose employee credentials or customer login sessions.
6. Preserve the 13 business routes and six primary entries; no new route.

## 6. Generated-file ownership

- Edit **only** `shared/onecall-business.js` for shared rules. Run `node shared/build-prototypes.js` to regenerate the controlled inline block in all three source HTML files. Do not hand-edit between `<!-- ONECALL_BUSINESS:START -->` and `<!-- ONECALL_BUSINESS:END -->`.
- The block ranges are: operations `:23-1163`, Mini `:3138-4278`, Web `:642-1782` in the current sources.
- `preview.html` in all three directories is generated by each `qa/run.ps1` (Mini additionally via the visualize renderer). Never hand-edit a preview; regenerate it through the runner.
- The shared module hash in all three generated blocks must remain identical; cross-channel QA checks this.

## 7. QA changes, commands and expected assertions

### 7.1 Shared unit tests

Create `shared/qa/call-task.test.js` (Node `node:test` + `assert/strict`) covering:

1. Create freezes `ttlSeconds=30` and `expiresAt-createdAt=30000`; same key/input and same caller/callee repeat return the original task and original expiry (no renewal).
2. One nonterminal task per normalized caller across workspaces; another owner gets `CALLER_BUSY` with no callee/owner leak.
3. Explicit replacement only replaces the caller’s own `PENDING`; old task `REPLACED`, new task new expiry.
4. Boundary: claim at `expiresAt-1ms` succeeds; at `expiresAt` (and later) fails; an expired task is not claimable and its historical callee is not returned; eligible no-task path becomes `ENTER_CALLEE`.
5. Resolve with a valid task atomically writes `CLAIMED` + call + `TASK_CLAIMED` + `DIAL_TASK_CALLEE`, returns the frozen callee and `calleeConfirmation=NONE`; a second resolve with a different callId returns `REJECT_CALL_IN_PROGRESS`; the same callId returns the first decision without re-claiming.
6. No task → `ENTER_CALLEE` with `WAITING_CALLEE_INPUT`/`DTMF_REQUIRED`; `confirmDirectCallee` with a non-null taskId is rejected; a valid DTMF confirm moves to `CALLEE_CONFIRMED`, `taskPath=false`, `calleeConfirmation=DTMF_CONFIRMED`.
7. Fact append-only/monotonic/allowed-order; duplicate eventId is idempotent; `EVIDENCE_READY` before `RECORDING_RECEIVED`/`EVIDENCE_PROCESSING` is rejected; pre-bridge failure leaves zero seconds/no upload/no evidence.
8. Snapshot import rejects duplicate nonterminal tasks, frozen-field changes, event order tampering and forbidden fields atomically (state unchanged).
9. Business snapshot and call-task snapshot remain independent in both import directions.

Expected: all tests pass; no new timeout constant other than 30.

### 7.2 Runner assertions

**Mini (`prototype/qa/qa.js`):**

- Update `newPages` with `callStatus`; assert index count **63**.
- Remove `dialHandoff → solidified` success assertion (`:332-336`). Assert:
  - `call`/`trialCall` text includes “创建待拨任务”, “30 秒”, “系统拨号只打开拨号盘”.
  - After primary, page is `dialHandoff`; text includes `CT-`, “待呼入”, “到期”, `data-task-ttl="30"`.
  - `launch-dialer` → `callStatus`; text includes “系统拨号盘已打开”, “尚未接通”; no “证据已固化”; `data-call-path="task"` and `data-callee-confirmation="NONE"`.
  - `setCallDemoState('claimed')`: “已领取”, “自动外呼”, “不会要求确认被叫”; scoped task path does not contain “DTMF”.
  - `recording`: “桥接”, “录音中”.
  - `evidence`: “证据可用”; only then `solidified` is reachable; it labels IVR facts and ONECALL evidence.
  - `failed-expired`: “任务已过期”, “未领取”, “未外呼”, “未生成证据”; `failed-mismatch`: “主叫不一致”, “未领取任务”, “未扣”; `failed-no-answer`: “被叫未接”, “未形成证据”.
  - `landlineCall`: “无待拨任务”, “DTMF”, “确认被叫”, and no “自动外呼”; `data-call-path="no-task"`.
  - Other-account fixture: Mini does not render `CT-DEMO-0201` or its callee.
  - Business snapshot unchanged across task actions; `callTaskSnapshot()` has TTL 30 and one nonterminal for the caller.
  - 320px overflow loop includes `callStatus`, `dialHandoff`, `landlineCall`.
- Update the final log to mention 63 pages, task/claim/return states and no-task DTMF.

**Web (`web-prototype/qa/qa.js`):**

- Calls page: “任务与 IVR 诊断（只读）”, task ID, “冻结 30 秒”, `DIAL_TASK_CALLEE`/“任务自动外呼”, `ENTER_CALLEE`/“DTMF”, “录音上传”, “证据处理”.
- `OC-C2038` callinfo: task path, “无需确认被叫”, “IVR 可信主叫”, “IVR 权威录音时长”, “证据可用”.
- `OC-C2037` callinfo: no-task path, “DTMF 输入并确认被叫”.
- `OC-C2036`: “未领取任务”, “未外呼”, “未形成证据”, “未扣录音额度”; no share/apply.
- `OC-C2035`: “被叫未接/未接通”, “未形成证据”, “未扣录音额度”.
- Assert no stale “运营商确认有效录音”/“运营商实际主叫” in the diagnostics or call detail.
- Role filters: member/evidence-manager diagnostics respect the existing call visibility; auditor still has no calls route.
- Existing four tabs and all prior tests remain green.

**Operations (`operations-prototype/qa/qa.js`):**

- `/ivr-sessions`: diagnostic panel exists; contains `DIAL_TASK_CALLEE`, `ENTER_CALLEE`, “冻结 30 秒”, “自动外呼”, “无需确认被叫”, “DTMF 输入并确认被叫”, “录音上传”, “证据处理”, ownership statement, key-1 note and purchase-guidance note.
- Panel is read-only: no `data-action` inside `#call-diagnostics` (or equivalent); forged `edit-task`/`retry-upload`/`mark-evidence` do nothing and do not change state.
- Task/call import: load a fixture with `CT-DEMO-0201`; operations sees it, Mini/Web account 001 do not.
- Preserve all twelve existing IVR scenarios (`NEW_MOBILE`, `NEW_LANDLINE`, `REPEAT_CALL`, `TIMEOUT`, `WRONG_KEY`, `MISSING_CALLER`, `SUSPENDED`, `TRIAL_DISABLED`, `EXPIRED_TRIAL`, `EXHAUSTED_TRIAL`, `CONFLICT`, `RECYCLED`), the stable replay/consume behavior, key-1 unverified/unregistered trial, no order/payment on guidance, and immutable historical facts.
- Unknown IDs still return “记录不存在”; no task facts are editable; 720/1024/1280/1440 no page overflow.
- Update the final log to mention task/call diagnostics.

**Cross-channel (`shared/qa/cross-channel.js`):**

1. Create a task through the Mini UI; assert frozen TTL/expiry and one nonterminal task.
2. Resolve with a fresh callId (`CALL-QA-TASK-01`) through the shared helper; assert `DIAL_TASK_CALLEE`, `calleeConfirmation=NONE`, atomic claim, then record facts through upload/evidence; load the resulting call-task snapshot into Mini, Web and Operations. Do not reuse `OC-C2038` for the new claim because the fixture already owns that callId.
3. Assert all three `callTaskSnapshot()` values deep-equal; Mini/Web/Operations render the same task ID, status, decision and progress; business snapshots are unchanged.
4. Assert a second resolve with a different callId is `REJECT_CALL_IN_PROGRESS`; the same callId returns the first decision; no duplicate task/call.
5. Assert expiry at/after 30s → `EXPIRED`, no claim, `ENTER_CALLEE` (eligible) and no historical callee.
6. Assert no-task `ENTER_CALLEE` → DTMF-confirmed call; task path is false and no task is created.
7. Assert `CT-DEMO-0201` is visible in Operations but absent from Mini/Web account 001 views.
8. Assert call-task import never changes business facts (sessions/evidence/trial/orders/entitlements) and business import never changes call-task facts.
9. Assert forbidden call-task fields are rejected atomically.
10. Assert the three generated shared-block hashes remain identical and there are no browser errors.
11. Keep the existing IVR activation/trial/claim/payment tests unchanged and green.

### 7.3 Exact commands

Run from the worktree root:

```bash
node --check shared/onecall-business.js
node --check shared/build-prototypes.js
node --check prototype/qa/qa.js
node --check web-prototype/qa/qa.js
node --check operations-prototype/qa/qa.js
node --check shared/qa/cross-channel.js
node --test shared/qa/business.test.js
node --test shared/qa/call-task.test.js
node --test shared/qa/runner-exit.test.js
node shared/build-prototypes.js
node shared/build-prototypes.js --check
```

Then the three runners (Windows/PowerShell canonical; each regenerates its preview and calls the builder):

```powershell
powershell -ExecutionPolicy Bypass -File .\prototype\qa\run.ps1
powershell -ExecutionPolicy Bypass -File .\web-prototype\qa\run.ps1
powershell -ExecutionPolicy Bypass -File .\operations-prototype\qa\run.ps1
node .\shared\qa\cross-channel.js
git diff --check
```

Expected:

- All `node --check` exit 0.
- `business.test.js`, `call-task.test.js`, `runner-exit.test.js`: all tests pass.
- Builder: `Updated 3 ONECALL business section(s).`; `--check`: `ONECALL business sections are current.`
- Mini runner: new PASS line, exit 0, 63 pages.
- Web runner: `PASS: login/session, interaction, role authorization, content, and responsive checks`, exit 0.
- Operations runner: updated alignment PASS line, exit 0.
- Cross-channel: PASS line including task/claim/no-task/import parity, exit 0.
- `git diff --check`: no output, exit 0 (run where git metadata is valid; see §0).

### 7.4 Expected assertion outcomes

| Area | Must be true |
| --- | --- |
| 30s freeze | UI and tests show `data-task-ttl="30"` and a frozen `expiresAt`; duplicate create does not change it. |
| One nonterminal task | Creation/claim/replacement tests and snapshot validation reject a second nonterminal task for the same normalized caller, across workspaces. |
| Atomic claim | Failed claim leaves the state unchanged; successful claim writes task + call facts in one mutation. |
| Task vs no-task | Task path has `calleeConfirmation=NONE` and no DTMF; no-task path moves from `DTMF_REQUIRED` to `DTMF_CONFIRMED` and has no task/frozen callee. |
| Launch vs connection | Immediately after launch, the task remains `PENDING` and the UI progress is `WAITING_INBOUND`; “尚未接通” is visible; no evidence. |
| Upload vs evidence | `RECORDING_RECEIVED` and `EVIDENCE_PROCESSING` are separate; evidence is available only after `EVIDENCE_READY`. |
| Failure truthfulness | Mismatch/no-answer/expiry show no evidence, no effective-second debit and no success styling. |
| Isolation | Account 001 Mini/Web do not show `CT-DEMO-0201`; Operations can read it; no login/identity creation. |
| Key-1 trial | Existing unregistered/unverified IVR scenario still grants one trial and one evidence record; purchase guidance creates no order/payment. |
| Ownership copy | All surfaces show `B.labels.ownership`; only approved carrier facts remain. |
| Generated blocks | One identical hash across all three; no hand-edited inline block or preview. |

### 7.5 Stale-wording grep gate

Run from the worktree root and inspect every hit:

```bash
rg -n "运营商.{0,16}(负责|确认有效|确认实际|识别真实|完成开通|开通回执|同步开通|生成录音|固证|计费|号码归属)" \
  prototype/onecall-miniapp-pages.html web-prototype/evidence-hub.html operations-prototype/onecall-operations-admin.html
rg -n "运营商提供(号码|线路|通信承载)" \
  prototype/onecall-miniapp-pages.html web-prototype/evidence-hub.html operations-prototype/onecall-operations-admin.html
rg -n "ONECALL_BUSINESS:(START|END)" \
  prototype/onecall-miniapp-pages.html web-prototype/evidence-hub.html operations-prototype/onecall-operations-admin.html
```

Expected:

- First grep: no hit, or only a sentence that explicitly separates carrier transport from IVR/ONECALL ownership. Historical docs are out of scope; the three maintained prototype sources must not retain the removed claims.
- Second grep: the canonical ownership statement (or a direct fragment) appears in each surface.
- Third grep: exactly one marker pair per source; no hand-edited block.
- Search `preview.html` only to confirm it matches the source after the runner; never edit it.
- Confirm `B.labels.ownership`, task labels and path copies are identical across surfaces via cross-channel QA equality, not just visual similarity.

## 8. Screenshot matrix and stale screenshot handling

The runners generate `alignment-*` screenshots. Inspect at least:

| Surface | Viewport | File | Required visual state |
| --- | --- | --- | --- |
| Mini | 393×852 | `alignment-call-task-393.png` | PENDING task, `30 秒` frozen expiry, launch/cancel/replace, “未接通” disclaimer |
| Mini | 393×852 | `alignment-call-status-waiting-393.png` | Dialer opened, waiting inbound, not connected |
| Mini | 393×852 | `alignment-call-status-claimed-393.png` | Claimed, auto-dial, no callee confirmation |
| Mini | 393×852 | `alignment-call-status-recording-393.png` | Bridged/recording, upload not yet evidence |
| Mini | 393×852 | `alignment-call-status-evidence-393.png` | Evidence available with IVR/ONECALL attribution |
| Mini | 393×852 | `alignment-call-status-failed-393.png` | Expired/mismatch/no-answer, no evidence/debit |
| Mini | 393×852 | `alignment-landline-no-task-dtmf-393.png` | No-task DTMF confirmation path |
| Mini | 320×760 | `alignment-call-status-320.png` | No horizontal overflow; key text/buttons intact |
| Web | 1440×960 | `alignment-call-task-diagnostics-1440.png` | Read-only task/call panel and call table |
| Web | 1024×900 | `alignment-call-task-diagnostics-1024.png` | No page overflow; micro sublines wrap |
| Web | 1440×980 | `alignment-call-task-detail-1440.png` | Callinfo task/IVR facts and separate upload/evidence |
| Web | 1280×900 | `alignment-call-no-task-dtmf-1280.png` | Landline no-task DTMF detail |
| Web | 1280×900 | `alignment-call-task-failed-1280.png` | Mismatch/no-answer, no evidence/debit |
| Ops | 1440×960 | `alignment-ivr-task-diagnostics-1440.png` | Task + call diagnostic tables, read-only |
| Ops | 1280×900 | `alignment-ivr-task-diagnostics-1280.png` | Responsive table/panel |
| Ops | 720×900 | `alignment-ivr-task-diagnostics-720.png` | Internal table scroll, no document overflow |
| Ops | 1440×960 | `alignment-ivr-no-task-dtmf-1440.png` | No-task DTMF highlighted; task path distinct |

Also regenerate/check the existing affected `alignment-dial-handoff-320.png`, `alignment-landline-call-320.png`, `alignment-call-detail-1440.png` and any `alignment-cross-*` files whose copy changes.

Stale handling:

- Do not hand-edit or delete PNGs. Only the runners generate them.
- Overwrite workflow-owned `alignment-*` names when the same state is regenerated; use new names for new states.
- Do not delete `lean-*`, non-alignment, archived, or historical screenshots. If an old screenshot shows the removed “dialer → success” behavior and is not regenerated, report it as stale in the implementation artifact; do not present it as current evidence.
- The implementation report must list each inspected file, viewport and verdict (pass / stale / not inspected).

## 9. Docs and README updates

- `docs/06-miniapp-ui-spec.md`: update the 2026-10-21 note to say the prototype now models local task/return states; add the task/no-task page flow and 63-page count; keep production interface claims excluded.
- `docs/07-web-capability-hub-spec.md`: update the IVR note to describe read-only task/IVR diagnostics, no browser dial, and the task vs no-task vocabulary.
- `docs/09-business-activation-and-lean-admin.md`: add a short implementation note that the three prototypes now demonstrate the task/no-task UI as local fixtures; do not change business rules, prices or timeouts.
- `docs/10-cross-channel-rules.md`: replace “当前原型尚未实现该生产协议” with the local-fixture status; update page count and add `call-task.test.js` to the validation commands; keep the rules unchanged.
- `docs/13-ivr-call-task-and-inbound-design.md`: update only the implementation-status note in §11/§1; do not change interfaces, enums, state machines, 30s or production acceptance.
- `docs/README.md`: update the prototype status sentence; mention the task/no-task diagnostics and that they are local fixtures.
- `shared/README.md`: document the call-task demo domain, its separation from the business snapshot, the 30s/no-task/path rules, review hooks and the new test file.
- `prototype/README.md`: 63 pages, new `callStatus`, task/no-task flow, hooks, screenshots.
- `web-prototype/README.md`: read-only task/IVR diagnostics, call detail vocabulary, no browser dial.
- `operations-prototype/README.md`: read-only task/call diagnostics, review-only task/no-task selectors, no edits to telephony facts.
- `docs/11-account-identity-and-workspaces.md` needs no change: its §1/§4 already state that task creation requires login, task selection is a business number, and direct IVR does not require a task. Note this deliberate no-change in the implementation report.

## 10. Allowed and forbidden files

### 10.1 Allowed

Shared contract/tooling:

- `shared/onecall-business.js`
- `shared/README.md`
- `shared/qa/call-task.test.js` (new)
- `shared/qa/cross-channel.js`

Mini:

- `prototype/onecall-miniapp-pages.html`
- `prototype/qa/qa.js`
- `prototype/README.md`
- `prototype/qa/screenshots/alignment-*.png` (runner-generated only)
- `prototype/preview.html` (runner-generated only; not hand-edited)

Web:

- `web-prototype/evidence-hub.html`
- `web-prototype/qa/qa.js`
- `web-prototype/README.md`
- `web-prototype/qa/screenshots/alignment-*.png` (runner-generated only)
- `web-prototype/preview.html` (runner-generated only; not hand-edited)

Operations:

- `operations-prototype/onecall-operations-admin.html`
- `operations-prototype/qa/qa.js`
- `operations-prototype/README.md`
- `operations-prototype/qa/screenshots/alignment-*.png` (runner-generated only)
- `operations-prototype/preview.html` (runner-generated only; not hand-edited)

Docs and this work unit:

- `docs/06-miniapp-ui-spec.md`
- `docs/07-web-capability-hub-spec.md`
- `docs/09-business-activation-and-lean-admin.md`
- `docs/10-cross-channel-rules.md`
- `docs/13-ivr-call-task-and-inbound-design.md`
- `docs/README.md`
- `docs/gateflow/ivr-three-surface-prototype-sync-plan.md` (this artifact)
- Later-gate implementation/fix/review artifacts under `docs/gateflow/ivr-three-surface-prototype-sync-*` produced by the proper gates.

### 10.2 Forbidden

- `docs/gateflow/ivr-three-surface-prototype-sync-control.md` (read-only).
- Any hand edit between `ONECALL_BUSINESS:START/END` in the three source HTML files.
- Hand-editing any `preview.html`.
- A0/A1/A2 contracts (`api/`, `docs/14-*`, `docs/15-*`, generated production schemas/OpenAPI).
- `docs/01-05`, `docs/08`, `docs/12`, `docs/reviews/`, historical gate artifacts of other work units.
- `prototype/archive/`, non-alignment/old screenshots, `figma设计图截图/`, `logo/`, `*.docx`.
- Any new backend/runtime, media engine, SIP/provider, cloud, payment, PBX/extension/seat-PIN or online work-order/refund/invoice code.
- Any new timeout value beyond 30s; any production price or entitlement change.

## 11. Risks, stop conditions and open questions

### 11.1 Risks

- **Two validated fixture domains** (business + call-task) could be mistaken for one. Mitigation: explicit hooks, README documentation, cross-channel bidirectional independence tests, and UI labels “本地评审 fixture”.
- **Atomicity is local.** Shared functions simulate atomic draft mutation; they do not prove database concurrency. Do not claim production concurrency.
- **Timer flakiness.** Use injected `now`, frozen `expiresAt`, and deterministic fixture states; assert data attributes, not exact countdown ticks.
- **Over-modeling.** Keep only the states needed to distinguish launch/claim/ringing/answer/recording/upload/evidence/failure; do not add reservations, billing, retries or provider internals.
- **Wide layouts.** Use micro sublines and internal table scrolling; verify 1024/720 no page overflow.
- **Existing QA coupling.** The current Mini QA asserts the removed dialer→success path; update it deliberately and keep all unrelated assertions.
- **Stale carrier copy** may remain in historical sections. Use targeted updates and the grep gate in §7.5; do not rewrite historical docs.
- **Skill-mode constraints unknown** due to the §0 blocker. Do not start implementation until resolved.

### 11.2 Stop conditions

Stop and report instead of guessing if any of these occurs:

1. The skill files remain unreadable after a grant attempt, and the reviewer cannot confirm they add no constraints.
2. The implementation would require changing the accepted business `schemaVersion`, event domains or A0/A1/A2 contracts rather than adding the separate call-task domain.
3. The implementation would require a new timeout number, production backend/runtime, real telephony/media/upload, payment/cloud integration or PBX semantics.
4. Account/workspace isolation cannot be preserved (especially hiding another workspace’s normalized-caller task).
5. Deterministic QA cannot distinguish task path from no-task path or dialer launch from claim/connection without faking progress.
6. Git metadata remains unavailable where `git diff --check` is required; report the environment blocker rather than claiming the gate passed.

### 11.3 Open questions / blockers for the reviewer

- **Skill files:** unreadable in this session (§0). Need access or explicit confirmation that no additional prototype-mode rules apply.
- **Separate call-task snapshot:** this plan deliberately keeps it outside the business snapshot to avoid a schemaVersion bump/contract risk. Confirm this is acceptable; if a single snapshot is mandatory, the plan must be reworked with an explicit schema migration and full revalidation.
- **Formal copy:** Product/Legal owns final consent, recording-notice and ownership wording. This plan uses neutral operational copy and preserves existing authorization text unchanged.
- **UI count:** adding `callStatus` changes the Mini review page count from 62 to 63. Confirm this review artifact change is acceptable; no product navigation change is implied.
- **Git/WSL:** this worktree's git metadata is unavailable; the implementation/review runner must execute §7.3 in a valid checkout.

## 12. Completion report format (implementation gate)

The implementation report must contain:

1. Artifact path(s), work unit, base commit and changed-file groups.
2. Slice count and rationale (expected: 1).
3. Shared contract summary: new exports, state shape, fixtures, hooks, invariants.
4. Per-surface behavior summary with exact task/no-task, launch/claim/return states.
5. Validation matrix: every command from §7.3, exit code, PASS line or failure excerpt.
6. Screenshot table: path, viewport, inspected state, verdict; stale screenshots listed separately.
7. Grep evidence for stale carrier wording and for generated-block hash equality.
8. Docs/README changes and the deliberate no-change note for docs 11.
9. Open questions/blockers (skill files, git metadata, separate snapshot approval).
10. Residual risks and explicit statement that no production telephony/backend/upload/payment capability is claimed.
11. Next gate recommendation only (implementation/review); never advance another gate from the implementation report.

## 13. Definition of done

- One slice implements the shared call-task contract and all three surfaces.
- The no-task DTMF path remains explicit and cannot be confused with task-backed auto-dial.
- Every surface shows one nonterminal task per normalized caller, 30s frozen expiry, atomic claim, frozen-callee auto-dial without callee confirmation, and truthful launch/claim/ringing/answer/bridge/recording/upload/evidence/failure states.
- Operations is read-only for task/call/IVR facts and preserves immutable telephony facts.
- Account/workspace isolation, key-1 unregistered/unverified trial, and purchase-guidance-only behavior are preserved.
- Stale carrier-ownership wording is replaced with the canonical ownership statement.
- All commands in §7.3 pass in a valid environment; required screenshots are inspected; historical artifacts are preserved.
- No production runtime/backend, contract edit, new timeout, payment/cloud integration or broad redesign is introduced.
