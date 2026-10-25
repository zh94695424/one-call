# A2 生产 IVR 协议规范实施计划

> Work unit：`A2-production-ivr-protocol-spec`
>
> Gate：`fix`（bounded plan-fix-1 / F-1 与 resolve-never-committed 负例澄清；本文件仍只形成实施计划）
>
> Task label：`a2-ivr-plan-ds-20261025-01`；fix task label：`a2-ivr-planfix1-ds-20261025-01`
>
> Base：merged A1 commit `754b7dfc53c277e79c5d63e24d578a93fa557cda`（PR #2 / A1）
>
> Binding contract：`docs/gateflow/project-execution-control.md` 顶部 A2 binding goal contract / project constraints / slice principle
>
> Fix basis：`docs/reviews/plan-review-20261024-224656.md` 的 F-1 与 controller 裁决（review open question 2）；逐项修改与复核见 `docs/gateflow/a2-production-ivr-protocol-spec-plan-fix-1.md`
>
> Design sources：`docs/12` §8/§9.2/§10.2/§13/§18；`docs/13`；`docs/14`（IVR、预占/用量、evidence、recording/media、幂等/Outbox）；`api/openapi/onecall-v1.openapi.yaml` 六个 IVR operation；`docs/15` §3.3/§4/§8

## 0. 结论摘要

- 产出一份人类 canonical 协议规范 `docs/16-production-ivr-protocol-spec.md`，一份机器可读协议模型 `protocol/ivr/v1/ivr-protocol-v1.json`，一组确定性 fixtures，以及零第三方依赖的校验器 `protocol/ivr/validate.mjs`。
- `validate.mjs --stage` 使用唯一投影：S1 的 event 块是 11 个 identity/name + `DEFERRED_S2` 哨兵，S2 将其提升为完整 4 列 frozen 形态并写入 digest，S3 只读校验；不把 S2 payload/state 行为移入 S1。
- 协议完全复用 A1 已接受的 6 个 IVR operation，不新增 endpoint，不新增 Problem code，不新增 A0 表/列/状态/事务。
- 心跳、未知终态对账、B-leg 恢复都映射到既有 `POST /api/v1/ivr/calls/{callId}/events` 的稳定事件身份，因此不需要 A1 refinement。
- 只有三个实现切片：S1 冻结动作与会话身份，S2 呼叫执行/B-leg exactly-once/终态收敛，S3 录音媒体生命周期与端到端恢复。三者是按行为与失败模式分组，不是按文件/消息/owner 拆分。
- 无 blocking open question。所有 Q7/Q8/Q10/Q18/Q25/Q27 数值或 provider 策略保持 typed deferred input；Q15/Q26/Q28 的协议形状与恢复语义在本计划内闭环。
- 发现一个 **非阻塞的 A0 表述不一致**（A0 §10.14 把 `task.taskId` 标为 `uuid`，但 A0 §4.23 实际列为 `text`，A1 也是 1-64 string）以及两个 **A1/A0 投影边界**（declaration replay、session `ABANDONED`）。它们被显式记录为 residual，不修改任何 accepted contract，也不阻塞 A2。
- resolve 从未 commit 的失败关闭负例已写入 S2：本地无 B-leg/line attempt，之后 `OUTBOUND_NOT_STARTED` 返回 `404 CALL_UNKNOWN`，不建占位、不 release、无 usage，outbox permanent；与 committed-but-response-lost 的 frozen decision replay 明确区分。
- 本计划只形成规范/模型/fixtures/校验器的实施契约；fix artifact 记录本轮 accepted findings；不实施、不 stage/commit/push、不创建 PR、不进入下一 Gate。

## 1. 目标、动机、成功信号、非目标、范围边界与契约对齐

### 1.1 目标

形成一份 code-generation-ready、版本化的 IVR 协议规范，明确自研 IVR 与 ONECALL backend 从呼入会话建立、统一动作执行、开通再决策、B-leg 恢复、桥接/录音事件、心跳、终态结算到录音媒体上传的完整协作契约。

规范必须让每条协议消息具有稳定身份、owner 与状态前置条件；让初始 `resolve` 与 activation re-decision 共享同一个 action-selection contract；定义 normal/duplicate/delayed/reordered/conflicting/lost-response 路径；让 IVR 视角的 B-leg 恰好一次；把 terminal/recording/media 状态精确映射到 A0/A1；未知结果失败关闭且可对账恢复；并定义协议版本兼容、稳定错误与确定性静态校验/fixtures。

### 1.2 动机与直接仓库证据

| 证据 | 位置 | 对本计划的作用 |
| --- | --- | --- |
| A1 明确把详细 IVR session/event/retry/media protocol 留给 A2/Q15/Q26/Q28 | `docs/gateflow/project-execution-control.md` A2 块；`docs/15` §8 | 本计划的边界与输入 |
| 六个 IVR operation 与精确错误集 | `api/openapi/onecall-v1.openapi.yaml:3087-3620`；`docs/15` §3.3/§4 | 不得新增 endpoint/code |
| 共享 frozen decision | `api/openapi/onecall-v1.openapi.yaml:6653-6788`；`docs/14` §10.14 | action-selection contract 的唯一结果形状 |
| 事件 envelope 与 opaque payload | `api/openapi/onecall-v1.openapi.yaml:6866-6924` | 心跳/leg/bridge/terminal 的 wire 承载体 |
| 录音声明/完成 wire | `api/openapi/onecall-v1.openapi.yaml:6925-7019` | 媒体消息的 wire 承载体 |
| 单主叫单非终态任务、30 秒冻结、原子领取/预占/冻结决策 | `docs/13` §4.2/§5/§6.3/§6.5；`docs/14` §4.23/§9.12/§9.13 | 任务路径不变量 |
| 直接 DTMF 路径与 purpose chain | `docs/13` §6.4；`docs/14` §7.3/§9.14 | 直接路径不得回落任务 |
| IVR 拥有通信事实；backend 拥有业务/权益/证据事实 | `docs/12` §1.1/§9.1/§9.2；`docs/13` §2/§7 | ownership matrix 的基础 |
| 终态/UNKNOWN/对账/结算边界 | `docs/13` §7；`docs/14` §7.3/§9.15/§9.16/§9.24 | unknown fail-closed 设计 |
| 录音/媒体/evidence 状态机 | `docs/14` §4.39-§4.43/§7.4/§9.27/§9.28/§10.24 | 媒体状态映射 |
| `callId` 根身份与 B-leg 本地先写后拨 | `docs/14` §2.9/§10.14/§9.14；`docs/13` §6.5/§7 | Q28 本地恢复契约 |

### 1.3 成功信号与计划落点

| 成功信号 | 本计划落点 |
| --- | --- |
| 每条消息有稳定身份、owner、状态前置条件 | §5 identity/ownership/precondition 矩阵；§4 消息设计 |
| resolve 与 activation re-decision 共享 action contract | §4.3；模型 `actionSelection`；fixture `s1-01`/`s1-03` |
| normal/duplicate/delayed/reordered/conflicting/lost-response 显式 | §4.11；§6.4；fixtures 全部场景 |
| IVR 视角 B-leg exactly-once | §4.7；§6.2；invariant `INV-A2-03`；fixtures `s2-05`/`s2-06`/`s2-08` |
| terminal/recording/media 精确映射 A0/A1 | §4.9/§4.10；§5；§6 |
| unknown fail closed，不退避为 redial/double charge | §4.8；§6.3；fixtures `s2-04`/`s2-10`/`s2-11` |
| 版本兼容、稳定错误、确定性静态校验/fixtures | §4.2/§4.12；§7 |
| code-generation-ready | §3 路径；§4 精确字段；§7 模型/命令；§8 切片 |

### 1.4 非目标与禁止发明

不做：Java/runtime 服务、IVR 媒体引擎重写、SIP/provider 内部细节、Tencent Cloud 资源、真实凭证、UI/prototype、支付/订单、PBX/分机/坐席 PIN、operator 集成、容量/部署 sizing、security-design work unit、production acceptance matrix。

禁止发明：Q7/Q8/Q10/Q18/Q25/Q27 的数值或 provider 策略，包括 key hierarchy、replay window、duration tolerance、authorization window、心跳间隔、重试退避、超时、录音格式/大小、云资源细节。Q15/Q26/Q28 的协议形状与恢复语义在本计划内。

### 1.5 已批准行为与 A2 提议的 wire/detail 选择

| 类别 | 内容 | 来源/理由 |
| --- | --- | --- |
| 已批准 | 6 个 IVR operation、`IvrDecision`、七个 action + direct success null、三个 purpose、21 个 reason、精确错误集 | A1/`docs/15` |
| 已批准 | 单主叫单非终态任务、30 秒冻结、原子 claim/reservation/frozen decision、任务路径自动拨冻结被叫、无被叫确认 | `docs/13`/`docs/14` |
| 已批准 | 直接 DTMF purpose chain、拒绝 task fallback | `docs/13`/`docs/14` §7.3/§9.14 |
| 已批准 | `callId` 根、A/B `legId`、录音 `recordingId`、状态机与 settlement/evidence 分离 | `docs/14` |
| A2 提议 | `protocolVersion = 1.0` 的 grammar/兼容规则 | 绑定目标要求版本兼容；Q15/Q26 形状由本 work unit 负责 |
| A2 提议 | 11 个稳定 event type 的 closed payload（Q15） | A1 `IvrEventPayload` 明确 opaque，A2 是其 owner |
| A2 提议 | `CALL_HEARTBEAT` 作为 event type，经既有 event ingestion 处理为 `IGNORED` | 目标要求心跳；绑定约束禁止无必要 A1 refinement |
| A2 提议 | 9 个稳定 `endReason` 与 task `COMPLETED/FAILED` 映射 | 由 `docs/13` §7/§10 的终态语义直接推导 |
| A2 提议 | IVR-local `BlegOperation`（key = `callId`）与恢复判定（Q28） | A0 要求“先持久化唯一 B-leg 再拨”，恢复协议 deferred 给本 work unit |
| A2 提议 | `CALL_TERMINATION_UNKNOWN` / `CALL_TERMINAL_RECONCILED` | A0 允许 `UNKNOWN → ENDED/FAILED` 仅经 authoritative reconciliation；需要 wire 形状 |
| A2 提议 | terminal event 携带完整权威 A/B/bridge/recording/end facts | 让 out-of-order 与 restart 后仍能收敛，不依赖事件到达顺序 |
| A2 提议 | IVR-local event outbox（同 `eventId` 重传） | lost response/restart 不产生第二事实 |
| A2 提议 | declaration replay 只在 `DECLARED` 状态返回首次声明；上传后 re-declare 失败关闭 | A1 只有 201/DECLARED 成功形状；见 §4.13 投影说明 |
| A2 提议 | 心跳 interval/timeout、重试退避、时钟偏差等保持 deferred | Q8/Q25 不允许发明 |
| A2 提议 | 录音 upload observation 由 backend 内部观测流程完成，A2 不新增 endpoint | A1 complete 明确 evidence/observation 分离 |

### 1.6 无目标漂移检查

- 不新增路由：所有协议消息映射到 A1 的 6 个 IVR operation。
- 不新增 Problem code：所有失败映射到 A1 既有 IVR 错误集。
- 不新增 A0 表/列/状态：所有持久化状态引用 A0 既有 named CHECK 与状态机。
- 不新增数值：除已批准的 30 秒默认 TTL 与 A1 既有字段边界外，不冻结任何 timeout/window/tolerance/interval。
- 不触碰产品路径：任务路径不做被叫确认；直接路径不领取任务；不出现 latest-callee fallback。
- 不越界实现：切片只产出规范、机器模型、fixtures、校验器与 Gateflow 报告。

## 2. 第一性原理判断与仓库证据

### 2.1 事实所有权决定协议形状

- IVR 拥有：实际接听/外呼/B-leg 发起、A/B leg、桥接、录音、限时挂断、通信时间点、结束原因、权威录音秒数、媒体字节与上传完成事实。
- Backend 拥有：激活/同意、任务、工作空间访问、试用/权益/预占/结算、决策/action、任务终态、媒体对象元数据、录音校验与 evidence 生命周期。
- 因此协议只能承载两类东西：**冻结决策**（backend → IVR）与**不可变通信事实**（IVR → backend）。IVR 不得写业务/权益/证据状态；backend 不得改写通信事实或替 IVR 重算路由。

### 2.2 三个第一性约束

1. **冻结动作**：路由是一次性的，不是每跳重算。`resolve` 与 activation re-decision 必须返回同一 `IvrDecision` 结构；IVR 只执行 `action`，不得根据 caller/balance/task 自行改选。
2. **单调事实 + 首终态吸收**：事件只前进；终态一旦接受不可回退；`UNKNOWN` 是 occupied 状态，不是失败、成功或重拨许可。这样 duplicate/delayed/reordered 事件最终收敛到同一事实。
3. **本地先写后拨 + 权威对账**：B-leg exactly-once 不能靠“重试 API”实现，只能靠 IVR 本地唯一 operation 的 write-before-side-effect + 线路/CDR 对账。API timeout 是未知结果，不是失败；未知不得重拨。

### 2.3 为什么不需要新增 A1 endpoint / Problem code / A0 表

- A1 的 `ingestIvrCallEvent` 已接受稳定 `eventId`、`eventType`、`legId`、`sequenceNo`、`occurredAt` 与 opaque payload；这足以承载 leg/bridge/recording/heartbeat/terminal/reconciliation。
- A1 的 `IvrDecision` 已覆盖三个 purpose；A1 的 `RecordingDeclaration`/`RecordingCompletion` 已覆盖媒体 wire。
- A0 已有 `call_record`、`call_leg`、`call_event`、`ivr_decision`、`usage_reservation`、`usage_fact`、`recording`、`media_object`、`evidence_*` 的全部状态与事务。
- 新增 endpoint 会改变 A1 已冻结的 49 operation 契约，且没有不可避免的矛盾支撑；新增 code 会破坏 A1 精确错误集。因此 A2 只在既有 surface 内补 wire/detail。

### 2.4 关键证据索引

| 主题 | 精确引用 |
| --- | --- |
| 五类信任面与 IVR mTLS/signature | `docs/12` §8；`docs/15` §1 |
| 30 秒 TTL、单槽任务 | `docs/13` §4.2/§5；`docs/14` §4.23/`uq_outbound_task_live_caller` |
| 原子 claim/reservation/frozen decision | `docs/14` §9.13；`docs/13` §6.3 |
| direct purpose chain | `docs/14` §9.14/§7.3 |
| decision fingerprint / replay 优先级 | `docs/14` §10.17/§9.21 |
| event 幂等/顺序/首终态 | `docs/14` §4.27/§9.24/§10.17 |
| UNKNOWN 与 reconciliation | `docs/14` §7.3/§9.16/§9.24；`docs/13` §7 |
| settlement 边界（无权 tolerance） | `docs/14` §4.34/§9.15 |
| recording 状态与冲突事实 | `docs/14` §4.40/§7.4/§9.27 |
| evidence 下游 | `docs/14` §4.41/§9.28/§10.23/§10.24 |
| A1 IVR 错误集 | `docs/15` §4；OpenAPI `x-onecall-error-codes` |
| A1 校验工具 | `api/openapi/package.json`、`check-doc-map.mjs`、`contract-rules.mjs` |

## 3. 产物布局与 canonical 决策

### 3.1 精确路径

| 路径 | 作用 | 创建切片 |
| --- | --- | --- |
| `docs/16-production-ivr-protocol-spec.md` | 人类 canonical 协议规范与治理/映射 | S1 创建，S2/S3 增补 |
| `protocol/ivr/v1/ivr-protocol-v1.json` | 机器可读协议模型：版本、消息、event types、identity、action/reason/end registry、状态机、错误映射、A0/A1 引用 | S1 创建，S2/S3 增补 |
| `protocol/ivr/v1/fixtures/*.json` | 确定性可执行场景 | S1/S2/S3 分别新增 |
| `protocol/ivr/validate.mjs` | 零第三方依赖的静态校验 + fixture runner | S1 创建，S2/S3 增补 |
| `docs/gateflow/a2-production-ivr-protocol-spec-s1-implementation.md` | S1 完成报告 | S1 |
| `docs/gateflow/a2-production-ivr-protocol-spec-s2-implementation.md` | S2 完成报告 | S2 |
| `docs/gateflow/a2-production-ivr-protocol-spec-s3-implementation.md` | S3 完成报告 | S3 |

不创建 README、第二份协议文档、JSON Schema 副本、A1/A0 修改或 package 依赖树。

### 3.2 为什么是 `docs/16` + `protocol/ivr`

- `docs/12` §18 把 IVR 协议列为数据模型/OpenAPI 之后的第 3 个可编码规格；A1 已使用 `docs/15`，因此下一个编号是 `docs/16`。
- 协议的人类语义、状态机、ownership、deferred inputs 放在 `docs/16`；wire 字段/枚举/状态/映射的机器投影放在 `protocol/ivr/v1/ivr-protocol-v1.json`。
- `docs/16` 不复制 A1 schema，只记录协议增量、映射与验证；避免与 A1 形成第二 wire 真相。
- `protocol/ivr/validate.mjs` 使用 Node 22 内置模块，不新增 `package.json`/lockfile，保持最小交付面。

### 3.3 单一真相规则

- A1 仍是 operation/path/envelope/Problem/现有 request/response schema 的权威。
- A0 仍是表/列/状态/事务/身份/幂等的权威。
- `docs/16` 是协议语义（sequencing、heartbeat、B-leg recovery、terminal reconciliation、media 行为）的权威。
- 机器模型是 wire 字段/枚举/registry 的机器投影；不得包含与 `docs/16` 或 A1/A0 冲突的值。
- `docs/16` 必须包含三个机器可读标记块，校验器逐行对照模型：
  - `<!-- protocol-message-map:start/end -->`
  - `<!-- protocol-event-map:start/end -->`
  - `<!-- protocol-identity-map:start/end -->`
- 三个标记块的 stage 形态由 §7.4.1 唯一定义：message 12 行与 identity 22 行从 S1 起完整冻结；`protocol-event-map` 在 S1 为 identity-only（11 行、`eventType,payloadState`，哨兵 `DEFERRED_S2`），S2 提升为 4 列完整冻结形态并写入 digest，S3 不得修改。
- `validate.mjs` 必须按 `--stage s1|s2|s3` 选择唯一投影；缺少当前 stage 字段或提前出现未来 slice 字段均失败，不得跳过事件块或接受行/列子集。
- 模型、doc 标记块、A1 bundle、A0 named CHECK 四者任一漂移都必须使校验失败。

### 3.4 文件所有权与禁改

- 只允许 §8 各切片列出的文件。
- 不修改 `docs/12`-`docs/15`、A0/A1 accepted 产物、OpenAPI/schema source、prototype、screenshot、`shared/onecall-business.js`、`docs/gateflow/project-execution-control.md`。
- 既有 dirty screenshots、`logo/preview.html`、untracked `.a2-ivr-plan-task.md`、`docs/reviews/plan-review-20261021-205118.md` 保持原样。

## 4. 协议总体设计

### 4.1 传输、认证与封套

- 传输沿用 A1：HTTPS/JSON、`ivrMtls` + `ivrSignature`、`X-ONECALL-TIMESTAMP`、`X-ONECALL-NONCE`；签名串、窗口、nonce 存储、时钟源保持 Q7/Q15/Q25 deferred，A2 不冻结。
- 可信主叫、规范接入号、`nodeId` 由 IVR gateway/线路上下文经服务端派生；A1 request 不接受客户端声明的 caller/access/node/账户/价格/支付/证据状态。
- 六个 operation 与 A1 完全一致；A2 不新增 path/method。
- `callId` 是唯一跨 IVR/backend 的根协议身份；`sessionId`、decision、task、reservation 等 backend 身份不要求 IVR 输入。
- 所有时间使用 UTC `date-time`；业务过期/截止以 backend `authorizationExpiresAt`/`uploadExpiresAt` 绝对值为准；事件 `occurredAt` 是 IVR 事实时间，不用于任务 TTL 或授权判断。

### 4.2 协议版本与兼容

| 项 | 契约 |
| --- | --- |
| 版本字面量 | `1.0` |
| grammar | `^[0-9]+\.[0-9]+$`，长度 1-32（A1 既有边界） |
| 传输位置 | 只有 `POST /api/v1/ivr/inbound/resolve` body 的 `protocolVersion`；其它 operation 不携带版本 |
| 冻结 | resolve 成功时冻结到 `inbound_session.protocol_version` / `call_record.protocol_version`；同 `callId` 后续消息一律按冻结版本解释 |
| 支持集 | v1 server 只接受已注册的 `1.0`；未知 major/minor 或 malformed → `400 IVR_ARGUMENT_INVALID`，在任何 session/call/task/reservation 写入前拒绝 |
| minor 兼容 | 未来 minor 只能新增 optional 字段或新 event type，且必须有注册模型与服务端支持；新 session 才能协商；不得 mid-call 升级 |
| major 兼容 | 新 major 需要新 session；in-flight v1 session 必须继续按 v1 完成；若服务端无法继续支持，失败关闭 `503 SERVICE_UNAVAILABLE`，不得重拨/重建 session |
| 未知 enum/event type/payload 字段 | 失败关闭；resolve/confirm/direct/recording 的未知 body 字段由 A1 closed schema 返回 `422 VALIDATION_FAILED`；opaque event payload 内或未知 `eventType` 由 A2 返回 `400 IVR_ARGUMENT_INVALID` |
| 降级 | 不回退到“最新版本”、不忽略未知字段、不做 best-effort |

模型 `supportedVersions` 恰为 `["1.0"]`；`modelVersion = 1`。

### 4.3 统一 action-selection contract

所有三个 decision purpose 共享 A1 `IvrDecision` 与同一组 action invariants；backend 的 `ivr.select_call_action` 是唯一 selector。IVR 只执行返回的 action，不重算路由、不读取/推导 caller state、不选择其它被叫。

| action / direct success | 前置结果 | IVR 唯一允许行为 | 禁止 |
| --- | --- | --- | --- |
| `DIAL_TASK_CALLEE` | `taskMatch=CLAIMED`；task/reservation/authorized seconds/expiry 非空；`task.calleeNumber` 仅在加密内部 payload | 持久化唯一 B-leg operation 后拨冻结 task callee；接通后桥接录音；不要求被叫确认 | 调 direct authorization；选其它被叫；重算 task |
| `ENTER_CALLEE` | 无 task；`taskMatch=NONE`；无 reservation | 进入 DTMF 输入与确认；确认后调用 direct authorization；执行其返回结果 | 领取 task；把 DTMF callee 当 task callee |
| `GUIDE_ACTIVATION` | 无 reservation；reason `ACTIVATION_REQUIRED` 或 `CONSENT_REQUIRED` | 播放版本化告知；收到明确按键 1 后调用一次 activation confirmation；执行返回的下一个 action | 未按键直接开通；重复 confirm；改写 resolve |
| `GUIDE_PURCHASE` | 无 reservation；`NO_AVAILABLE_ENTITLEMENT` | 播放购买引导后结束 | 创建订单/支付；外呼 |
| `REJECT` | 无 reservation；匹配 reason | 播放失败提示后结束 | 外呼/领取/重试路由 |
| `REJECT_SUSPENDED` | 无 reservation；`ACTIVATION_SUSPENDED` | 播放停用提示/线下入口后结束 | 通过 IVR 恢复开通 |
| `REJECT_CALL_IN_PROGRESS` | 无 reservation；`CALL_IN_PROGRESS` | 播放稍后重试后结束 | 并发外呼 |
| direct success（`purpose=DIRECT_AUTHORIZATION`, `action=null`, `reasonCode=DIRECT_AUTHORIZED`） | `taskMatch=NONE`；task fields null；reservation 非空；`authorization.calleeNumber` 仅在加密内部 payload | 持久化唯一 B-leg operation 后拨确认的 direct callee | 使用 task 字段；再次 direct authorize；重新 collect callee |

共享不变量：

- 一次 decision 只有一个 executable intent；direct success 不是第八个 action，而是 purpose-specific 执行规则。
- `availableRecordingSeconds` 只是 routing snapshot，不是 spend authorization；`authorizedRecordingSeconds` 是唯一授权上限。
- `authorizationExpiresAt` 是开始 B-leg 的最后时刻；已在截止前启动的 B-leg 继续按冻结秒数完成；replay 不续期。
- 任务路径绝不出现被叫确认；直接路径绝不回落任务。
- 同一 `callId` 的 confirm 最多一次成功 re-decision；direct authorization 最多一次成功授权；不得通过重复调用改写首个冻结结果。

#### 4.3.1 协议消息清单（12）

| message ID | A1 operation | method/path | request schema | success response | 身份/owner |
| --- | --- | --- | --- | --- | --- |
| `resolve.request` | `resolveIvrInbound` | POST `/api/v1/ivr/inbound/resolve` | `IvrInboundResolveRequest` | `IvrDecision` | `callId` / IVR |
| `resolve.decision` | 同上 | 同上 | — | `IvrDecision` | `decisionId` / backend |
| `activation-confirm.request` | `confirmIvrActivation` | POST `/api/v1/ivr/calls/{callId}/activation-confirmations` | `IvrActivationConfirmRequest` | `IvrDecision` | `eventId` + `callId` / IVR |
| `activation-confirm.decision` | 同上 | 同上 | — | `IvrDecision` | `decisionId` / backend |
| `direct-authorization.request` | `authorizeIvrDirectOutbound` | POST `/api/v1/ivr/calls/{callId}/outbound-authorizations` | `IvrDirectAuthorizationRequest` | `IvrDecision` | `eventId` + `callId` / IVR |
| `direct-authorization.decision` | 同上 | 同上 | — | `IvrDecision` | `decisionId` / backend |
| `call-event.request` | `ingestIvrCallEvent` | POST `/api/v1/ivr/calls/{callId}/events` | `IvrCallEventRequest` | `CallEventReceipt` | `eventId` / IVR |
| `call-event.receipt` | 同上 | 同上 | — | `CallEventReceipt` | `eventId` / backend |
| `recording-declare.request` | `declareIvrRecording` | POST `/api/v1/ivr/calls/{callId}/recordings` | `RecordingDeclarationRequest` | `RecordingDeclaration` | `recordingId` / IVR |
| `recording-declare.result` | 同上 | 同上 | — | `RecordingDeclaration` | `recordingId` / backend |
| `recording-complete.request` | `completeIvrRecording` | POST `/api/v1/ivr/calls/{callId}/recordings/{recordingId}/complete` | `RecordingCompletionRequest` | `RecordingCompletion` | `recordingId` / IVR |
| `recording-complete.result` | 同上 | 同上 | — | `RecordingCompletion` | `recordingId` / backend |

### 4.4 会话与执行生命周期

Backend A0 状态与 IVR-local 执行 phase 分离；后者不写入 A0，也不替代 A0 状态。

| IVR-local phase | 含义 | 允许下一 phase | 对应 backend 事实 |
| --- | --- | --- | --- |
| `INBOUND_ANSWERED` | 真实主叫已接听，`callId` 已生成 | `DECISION_RECEIVED` | resolve 创建/复用 session/call |
| `DECISION_RECEIVED` | 收到 frozen `IvrDecision` | 按 action 分支 | `ivr_decision` |
| `GUIDANCE_PENDING` | 播放 activation/purchase 告知 | `ACTIVATION_CONFIRMED` / `ENDED` | session `RECEIVED`；无 reservation |
| `CALLEE_INPUT_PENDING` | DTMF 收集/确认 direct callee | `DIRECT_AUTHORIZED` / `ENDED` | session `RECEIVED`；无 task |
| `ACTIVATION_CONFIRMED` | 已收到 activation next action | `AUTHORIZED` / `DECISION_RECEIVED` / `ENDED` | session `CONFIRMED`；`ACTIVATION_NEXT_ACTION` decision |
| `DIRECT_AUTHORIZED` | direct authorization success | `AUTHORIZED` / `ENDED` | reservation `HELD`；无 task |
| `AUTHORIZED` | 有 task/direct 外呼授权 | `B_LEG_PREPARED` | task `CLAIMED` + reservation `HELD` |
| `B_LEG_PREPARED` | 本地唯一 B-leg operation 已持久化 | `B_LEG_DISPATCHED` | 无 backend 变化 |
| `B_LEG_DISPATCHED` | 已向线路发起唯一外呼 | `B_LEG_RINGING` / `B_LEG_ANSWERED` / `B_LEG_FAILED` | `CALLEE_LEG_*` 事件 |
| `B_LEG_RINGING` | 被叫振铃 | `B_LEG_ANSWERED` / `B_LEG_FAILED` | callee leg `RINGING` |
| `B_LEG_ANSWERED` | 被叫接通 | `BRIDGED` / `B_LEG_FAILED` | callee leg `ANSWERED` |
| `B_LEG_FAILED` | B 路失败/忙线/无应答/拒接 | `TERMINATING` | callee leg `FAILED` |
| `BRIDGED` | A/B 桥接 | `RECORDING` / `TERMINATING` | `call_record.bridged_at` |
| `RECORDING` | 录音中 | `TERMINATING` | recording start fact |
| `TERMINATING` | 本地结束/限时挂断 | `TERMINAL_REPORTED` / `UNKNOWN_PENDING_RECONCILIATION` | terminal event |
| `TERMINAL_REPORTED` | 权威终态已加入本地 outbox | `DONE` | call/task/reservation terminal |
| `UNKNOWN_PENDING_RECONCILIATION` | 线路结果未知 | `TERMINAL_REPORTED` only via reconciliation | call `UNKNOWN`；reservation occupied |
| `DONE` | 本次通话结束 | — | terminal/UNKNOWN |

Backend session 映射：

- `resolve` 走非终态 action 时创建/复用 `inbound_session` 与 `call_record ACTIVE`；rejected 路径创建 `call_record REJECTED`。
- activation confirmation 成功使 `inbound_session RECEIVED → CONFIRMED`；`GUIDE_ACTIVATION` 的 resolve 之后、confirm 之前保持 `RECEIVED`。
- task 路径和 direct 路径都不需要 session `CONFIRMED`；`CONFIRMED` 只表示 activation confirmation 已完成。
- `ABANDONED` 在 v1 没有 wire 触发（见 §4.13）。

### 4.5 事件 envelope 与 event type registry

Envelope 完全使用 A1 `IvrCallEventRequest`：`eventId`、`eventType`、`legId?`、`sequenceNo?`、`occurredAt`、`payload`。A2 在 opaque payload 内定义 closed payload；未知 `eventType`/payload 字段失败关闭。`leg_type`/`direction` 不由 wire 提交，而由 eventType 或 payload 字段（`callerLeg*` = `CALLER`/`INBOUND`，`calleeLeg*` = `CALLEE`/`OUTBOUND`）派生。

本节是 **S2 冻结目标形态**。S1 只在 doc/model 中登记 11 个 eventType 名称与 `DEFERRED_S2` 哨兵，不包含本节的 envelope/payload/state/terminal 语义；stage 投影见 §7.4.1，S1/S2 边界见 §8.2/§8.3。

| eventType | envelope `legId` | payload（closed） | A0 效果 |
| --- | --- | --- | --- |
| `CALLER_LEG_ANSWERED` | required | `{}` | 创建/推进 caller leg 为 `ANSWERED`；`started_at`/`answered_at` = `occurredAt` |
| `CALLEE_LEG_RINGING` | required | `{}` | 创建 callee leg 为 `RINGING`；`started_at` = `occurredAt` |
| `CALLEE_LEG_ANSWERED` | required | `{}` | callee leg → `ANSWERED`；`answered_at` = `occurredAt` |
| `BRIDGE_STARTED` | absent | `{callerLegId, calleeLegId}` | 要求两条 leg 已存在且均 `ANSWERED`；`call_record.bridged_at` = `occurredAt` |
| `RECORDING_STARTED` | absent | `{}` | 要求 `bridged_at` 已存在且 `occurredAt >= bridged_at`；`recording_started_at` |
| `RECORDING_ENDED` | absent | `{}` | 要求 `recording_started_at` 已存在且 `occurredAt >=`；`recording_ended_at` |
| `CALL_TERMINATED` | absent | `{callerLegId, calleeLegId?, callerLegAnsweredAt?, calleeLegStartedAt?, calleeLegAnsweredAt?, bridgedAt?, recordingStartedAt?, recordingEndedAt?, callerLegOutcome, calleeLegOutcome?, endReason, authoritativeRecordingSeconds}` | 权威终态：`ENDED`/`FAILED`、leg ends、bridge/recording times、authoritative seconds、task terminal、settlement handoff |
| `CALL_TERMINATION_UNKNOWN` | absent | `{reasonCode: "LINE_OUTCOME_UNKNOWN"}` | `ACTIVE → UNKNOWN`；`end_reason=LINE_OUTCOME_UNKNOWN`；`ended_at`/`authoritative_seconds` 保持 NULL；不释放、不重拨 |
| `CALL_TERMINAL_RECONCILED` | absent | `CALL_TERMINATED` 字段 + `resolvesUnknownEventId` | 仅 `UNKNOWN → ENDED/FAILED`；要求引用同一 call 已接受的 `CALL_TERMINATION_UNKNOWN`；结算/终态 |
| `OUTBOUND_NOT_STARTED` | absent | `{reasonCode: "AUTHORIZATION_EXPIRED", reservationId?}` | 权威证明从未发起线路请求且已过截止；`FAILED`/0 秒、task `FAILED`、reservation `RELEASED` |
| `CALL_HEARTBEAT` | absent | `{}` | 无业务状态变化；processing `IGNORED` |

`CALL_TERMINATED` payload 规则：

- `occurredAt` 即 call `ended_at`；所有提供的时间必须非递减：caller answer ≤ bridgedAt ≤ recordingStartedAt ≤ recordingEndedAt ≤ occurredAt；callee 时间同序。
- 当前 call/leg 已存在的事实时，payload 必须与之一致；任一冲突使整个 event `QUARANTINED`/`CONTRADICTORY_FACT`，不做部分应用。
- leg 行不存在时，`callerLegAnsweredAt` / `calleeLegStartedAt` / `calleeLegAnsweredAt` 必须提供，否则 `QUARANTINED`/`CONTRADICTORY_FACT`。
- `authoritativeRecordingSeconds` ≥ 0；`recordingStartedAt` 缺失时只能为 0；大于授权秒数不拒绝事件本身，但 settlement 进入 `RECONCILING`（A0 F-10），不生成 evidence。
- `endReason` 只用 §4.9 registry；`callerLegOutcome`/`calleeLegOutcome` 只用 `ENDED`/`FAILED`，并与 endReason 映射一致。

事件顺序与身份：

- `eventId` 在 `IVR_EVENT` 域全局唯一，1-128；同一 ID 出现在另一 call 也是身份冲突。
- 同一 `eventId` + 同 fingerprint → 返回首次 receipt，不改状态；同 ID + 不同 fingerprint/type/sequence → `409 EVENT_ID_CONFLICT`，不覆盖首次结果。
- `sequenceNo` 可选，正整数；服务端不要求按序到达，只用于冲突检测与观测。不同 `eventId` 复用同一 `(callId, sequenceNo)` → 新事件 `QUARANTINED`/`SEQUENCE_CONFLICT`，索引列存 NULL，原 sequence 留在 redacted source。
- 未知 parent call → `404 CALL_UNKNOWN`，不建占位。
- 未知 event type / payload 字段 / leg 类型不符 → `400 IVR_ARGUMENT_INVALID`，不写 event。
- call 已终态后，只有 `CALL_HEARTBEAT` 可接受（`IGNORED`）；其它事件若事实已完全一致 → `IGNORED`/`ALREADY_TERMINAL`，若要新增/改变事实 → `QUARANTINED`/`CALL_TERMINAL`。
- `CALL_TERMINATED` 在 `UNKNOWN` 上 → `QUARANTINED`/`USE_RECONCILIATION_EVENT`；只有 `CALL_TERMINAL_RECONCILED` 能收敛 `UNKNOWN`。
- `CALL_TERMINAL_RECONCILED` 的 `resolvesUnknownEventId` 缺失、不是同一 call 的已接受 `CALL_TERMINATION_UNKNOWN` → `400 IVR_ARGUMENT_INVALID`，不写 event；call 非 `UNKNOWN` → `QUARANTINED`/`NOT_UNKNOWN`。
- `OUTBOUND_NOT_STARTED` 未到 `authorizationExpiresAt` → `QUARANTINED`/`AUTHORIZATION_NOT_EXPIRED`；已有 callee leg/outbound/bridge/recording 事实 → `QUARANTINED`/`OUTBOUND_ALREADY_STARTED`；无 reservation → `400 IVR_ARGUMENT_INVALID`。
- 事件处理状态只用 A0 `RECEIVED/APPLIED/IGNORED/QUARANTINED`；首次终态吸收。

Quarantine/ignore reason registry（写入 `call_event.reason_code`，不是 Problem code）：`LEG_UNKNOWN`、`SEQUENCE_CONFLICT`、`CONTRADICTORY_FACT`、`CALL_TERMINAL`、`ALREADY_TERMINAL`、`AUTHORIZATION_NOT_EXPIRED`、`OUTBOUND_ALREADY_STARTED`、`USE_RECONCILIATION_EVENT`、`NOT_UNKNOWN`、`HEARTBEAT_IGNORED`。

### 4.6 心跳与存活

- 心跳是 `CALL_HEARTBEAT` event，不是 lease、不是 reservation 证明、不是终态证明。
- 处理：已知 call → 200 `CallEventReceipt{processingStatus: "IGNORED", reasonCode: "HEARTBEAT_IGNORED"}`；未知 call → `404 CALL_UNKNOWN`；重复同 `eventId` → 首次 receipt。
- 心跳绝不改变 call/task/reservation/recording/evidence，绝不释放预占、绝不授权或触发重拨。
- 缺失心跳只能作为 backend/IVR 运维触发对账的信号；interval、timeout、告警阈值保持 Q8 deferred。
- 心跳使用既有 event ingestion，因此 `call_event` 会保留 durable receipt；其保留/容量是 Q13/Q8 residual。

### 4.7 B-leg 本地 operation 与 exactly-once 恢复

IVR 必须在任何 line request 之前，以 `callId` 为唯一 key 持久化一条本地权威 `BlegOperation`。该记录不属于 A0 表，也不要求重建媒体引擎；它只固定逻辑状态与恢复判定。

| 字段 | 规则 |
| --- | --- |
| key | `callId`；每个 call 最多一个 operation（one reservation per call + no-redial） |
| `decisionId` | 触发授权的 frozen decision |
| `reservationId` | 触发授权的 reservation；direct/task 均非空 |
| `authorizationExpiresAt` | 从 decision 复制；不可续期 |
| `frozenCalleeDigest` | 冻结被叫的本地摘要；不要求跨系统传输 |
| `lineAttemptCount` | 0 或 1；只有实际向线路发起请求时才从 0 变 1，永不为 2 |
| `phase` | 见状态机 |
| `dispatchMarker` | 进入 `DISPATCHING` 前持久化的唯一 marker |
| `terminalEventId` | 本地权威终态事件身份（若已产生） |

| phase | 允许下一 phase | 守卫 |
| --- | --- | --- |
| `ABSENT` | `PREPARED` | decision 为 `DIAL_TASK_CALLEE` 或 direct success |
| `PREPARED` | `DISPATCHING` | 已持久化 marker；`now < authorizationExpiresAt`；`lineAttemptCount = 0` |
| `DISPATCHING` | `DIALING` / `DISPATCH_UNKNOWN` / `FAILED` | 唯一一次 line request；marker 已先提交 |
| `DIALING` | `RINGING` / `ANSWERED` / `FAILED` / `ENDED` | 线路接受 |
| `RINGING` | `ANSWERED` / `FAILED` / `ENDED` | — |
| `ANSWERED` | `BRIDGED` / `FAILED` / `ENDED` | — |
| `BRIDGED` | `RECORDING` / `FAILED` / `ENDED` | — |
| `DISPATCH_UNKNOWN` | `DIALING` / `ANSWERED` / `ENDED` / `FAILED` / `NOT_DISPATCHED` | 仅凭线路/CDR 权威记录；不得直接重拨 |
| `NOT_DISPATCHED` | `PREPARED` / `AUTHORIZATION_EXPIRED` | 权威证明从未实际发起；`lineAttemptCount` 仍为 0 |
| `AUTHORIZATION_EXPIRED` | 上报 `OUTBOUND_NOT_STARTED` | 截止已到且证明从未发起 |
| `FAILED` / `ENDED` | 终态事件 outbox | 不可重拨 |

恢复判定：

1. restart 后先按 `callId` 读取本地 operation 与 event outbox；不得生成新 `callId`、不得重新 resolve 以“获取新授权”。
2. `PREPARED`：可首次 dispatch；若已过截止，上报 `OUTBOUND_NOT_STARTED`。
3. `DISPATCHING`/`DISPATCH_UNKNOWN`：查询本地权威线路/CDR；`ATTEMPTED/IN_PROGRESS/ANSWERED/COMPLETED` → 恢复事实，`lineAttemptCount=1`，不重拨；权威 `NO_ATTEMPT` 且未过截止 → `NOT_DISPATCHED → PREPARED`，允许首次 dispatch；无法证明 → 保持 unknown，不重拨，直到对账或人工处理。
4. 已过截止且无法证明“从未发起” → 不得上报 `OUTBOUND_NOT_STARTED`；保持 unknown/occupied，交由对账。
5. decision response 丢失且本地从未进入 `PREPARED`：不得猜测被叫；应以同一 `callId` 重试取得 frozen decision，并区分两种互斥情况：
   - **原始 resolve 已 commit**（backend 已有 call/reservation）：同 `callId` 重试返回首次 frozen decision；若最终仍无可用 decision、授权截止已到且本地无 B-leg/line attempt，可在截止后上报 `OUTBOUND_NOT_STARTED`，由 backend 终结既有 call/task 并释放既有 reservation（fixtures `s2-05`/`s2-07`）。
   - **原始 resolve 从未 commit**（backend 无 call/reservation）：同 `callId` 重试仍无可用 decision 时，之后任何 `OUTBOUND_NOT_STARTED` 上报都返回 `404 CALL_UNKNOWN`；不创建占位 call/task/reservation，不释放 reservation，不生成 usage；本地 outbox 记 `PERMANENT_FAILURE` 并保留本地失败证据供运维。不得在本地伪造“已释放”，不得以新身份重报（fixture `s2-11`）。
6. B-leg 已启动后即使授权截止已过，也按冻结秒数完成；截止不是中断/重拨许可。
7. v1 每个 call 恰好一个 B-leg；任何“重试被叫”都必须作为新 call/新 reservation 的产品变更，不在 A2 内（见 residual）。

本地 event outbox：

| 状态 | 规则 |
| --- | --- |
| `PENDING` | 事件/声明/完成意图先持久化，再发送 |
| `SENT` | 已发送但无终态 receipt |
| `ACKED` | 收到 2xx 终态 receipt（`APPLIED`/`IGNORED`/`QUARANTINED`） |
| `PERMANENT_FAILURE` | 身份/参数/终止性冲突；告警，不自动改写成新身份 |

- 重传使用完全相同 `eventId` 与 payload；terminal/recording declaration/completion 必须先持久化再发送。
- `CALL_EVENT`：2xx（含 `QUARANTINED`）为终态；`EVENT_ID_CONFLICT`/`SESSION_IDENTITY_MISMATCH`/`CALL_UNKNOWN`/`IVR_ARGUMENT_INVALID`/`VALIDATION_FAILED` 为 permanent；429/5xx/网络可重传。
- `RECORDING_DECLARE`：201 为终态；`RECORDING_IDENTITY_CONFLICT`/400/422 为 permanent；429/5xx/网络可重传。
- `RECORDING_COMPLETE`：200 为终态；`UPLOAD_OBSERVATION_REQUIRED` 可重传；`UPLOAD_EXPIRED`/`RECORDING_IDENTITY_CONFLICT`/400/422 为 permanent；`STORAGE_UNKNOWN`/429/5xx/网络可重传。
- 所有重传都不得改变身份或重拨。

### 4.8 终态、UNKNOWN 与对账

- 权威终态只有 `CALL_TERMINATED` / `CALL_TERMINAL_RECONCILED` / `OUTBOUND_NOT_STARTED` 三类。
- 线路/呼叫结果不明时只能上报 `CALL_TERMINATION_UNKNOWN`：call `UNKNOWN`、无 `ended_at`、无 `authoritative_seconds`、task 保持 `CLAIMED`、reservation 保持 `HELD` 并在授权过期后进入 `RECONCILING`；绝不释放、绝不重拨。
- `UNKNOWN` 只能由 `CALL_TERMINAL_RECONCILED`（引用原 unknown event）或 backend 运维对账收敛到 `ENDED/FAILED`；收敛时同一事务完成 usage settlement/release 与 task 终态。
- 迟到/矛盾终态事实进入 `QUARANTINED`，不重开任务、不重复扣量、不生成 evidence。
- backend 不得因为 30 秒 TTL、本地 lease 或 heartbeat 缺失把 `CLAIMED`/`UNKNOWN` 恢复为可领取。
- task 终态与 call 终态分离：通信事实完成不等于 recording validated 或 evidence solidified。

### 4.9 终态原因与 task/settlement 映射

`endReason` registry（9 值；`CALL_TERMINATION_UNKNOWN`/`OUTBOUND_NOT_STARTED` 的 reason 不在其中）：

| endReason | call_record | task（task-backed） | 备注 |
| --- | --- | --- | --- |
| `CALLER_HANGUP_AFTER_BRIDGE` | `ENDED` | `COMPLETED` | 正常完成 |
| `CALLEE_HANGUP_AFTER_BRIDGE` | `ENDED` | `COMPLETED` | 正常完成 |
| `RECORDING_LIMIT_REACHED` | `ENDED` | `COMPLETED` | IVR 按授权秒数主动挂断 |
| `CALLER_HANGUP_BEFORE_BRIDGE` | `FAILED` | `FAILED` | A 路桥接前退出 |
| `CALLEE_BUSY` | `FAILED` | `FAILED` | 未接通，无有效录音 |
| `CALLEE_NO_ANSWER` | `FAILED` | `FAILED` | 未接通，无有效录音 |
| `CALLEE_REJECTED` | `FAILED` | `FAILED` | 被叫拒接 |
| `B_LEG_FAILED` | `FAILED` | `FAILED` | B-leg 失败 |
| `BRIDGE_FAILED` | `FAILED` | `FAILED` | 桥接失败 |

- `OUTBOUND_NOT_STARTED`：call `FAILED`、`end_reason=AUTHORIZATION_EXPIRED`、`authoritative_seconds=0`；task `FAILED`；reservation `RELEASED`；无 `usage_fact`。该映射以原始 resolve 已 commit、backend 已有 call/reservation 为前提。
- resolve 从未 commit 时 backend 无 call/reservation；此后的 `OUTBOUND_NOT_STARTED` 按 unknown parent call 处理：`404 CALL_UNKNOWN`、不建占位、不 release、无 usage，IVR outbox 永久失败（§4.7 恢复判定 5 / fixture `s2-11`）。
- `CALL_TERMINATION_UNKNOWN`：call `UNKNOWN`、`end_reason=LINE_OUTCOME_UNKNOWN`；task 不变；reservation 不释放。
- `authoritativeRecordingSeconds` 与 task 终态独立：即使 call/task `FAILED`，已发生的有效录音秒数仍按 A0 T-S2-05 结算；0 秒按 known-zero release。
- 秒数在 `1..authorized_units`：settle 并释放余量，生成一条 `usage_fact`；0 秒：`RELEASED` 且无 usage fact；超过授权：不 settle、不 release、不生成 usage fact，reservation 进入/保持 `RECONCILING`，`last_error_code=DURATION_EXCEEDS_AUTHORIZED`（无 tolerance）。
- task `FAILED/COMPLETED` 只由 accepted terminal/reconciliation 触发；不得由 heartbeat、声明、完成或 evidence 状态反向触发。
- task 终态更新是 `ivr` owner 在同一 call-event 本地事务内对既有 `outbound_call_task` 的条件更新，不新增 A0 事务、不跨 owner 写表；settlement 仍由 entitlement owner 在同一事务通过既有服务完成。

leg 映射：`CALLER_HANGUP_BEFORE_BRIDGE` → caller `ENDED`（或 `FAILED` 若本地失败）、callee `FAILED`（若存在）；`CALLEE_BUSY/NO_ANSWER/REJECTED/B_LEG_FAILED` → callee `FAILED`、caller `ENDED`；桥接后正常/限时 → 两腿 `ENDED`；`BRIDGE_FAILED` → 两腿 `ENDED`；无 callee leg 时 callee 字段为 null。leg `end_reason` 使用同一 `endReason` 值。

### 4.10 录音声明、上传、完成与隔离

| 阶段 | wire | A0/A1 映射 | 规则 |
| --- | --- | --- | --- |
| declare | `POST .../recordings`；`RecordingDeclarationRequest` | `evidence.recording` `DECLARED`；`recording_id` 唯一；`declared_format=mimeType`、`declared_size=sizeBytes`、`declared_duration=durationSeconds`、`declared_sha256=sha256`；`declaredAt=created_at`；`uploadExpiresAt=upload.expires_at=upload.expiresAt` | 声明事实不是 valid media fact；不返回 object key/bucket/version/永久 URL |
| upload | IVR 用 `upload.credential` 直传对象存储 | backend 受控上传观测；不新增 A1 endpoint | 凭证不是 return-once；日志必须脱敏；upload expiry 过期不得续期 |
| complete | `POST .../recordings/{recordingId}/complete`；`RecordingCompletionRequest` | `evidence.recording` `DECLARED→UPLOADED→VALIDATED/QUARANTINED`；`validatedDuration/validatedSha256/validatedAt`；`completedAt=validated_at` | 客户端不提交 hash/version/bucket；`DECLARED` 未观测 → `409 UPLOAD_OBSERVATION_REQUIRED`；过期 → `409 UPLOAD_EXPIRED` 并隔离；存储未知 → `503 STORAGE_UNKNOWN`；同 identity 同 facts → 首次终态 |
| conflict | 同上 | 同 `recordingId` 不同 object/version/hash/size/MIME/duration | pre-terminal → 一次 `QUARANTINED`；post-terminal → `409 RECORDING_IDENTITY_CONFLICT` + 一条去重 redacted Outbox conflict fact；不覆盖首次 facts/revision |
| evidence | 无 IVR wire | A0 `evidence.evidence_record`/`evidence_event` 由 evidence owner downstream flow 创建/固化 | A2 不新增 evidence creation endpoint；`complete` 不创建/固化 evidence |

declare replay：

- 同 `recordingId` + 完全相同的 declaration facts，且当前仍为 `DECLARED` → `201 RecordingDeclaration` 首次快照，不续期、不新增 recording。
- 同 identity 但 declaration facts 不同 → `409 RECORDING_IDENTITY_CONFLICT`。
- 已 `UPLOADED`/`VALIDATED`/`QUARANTINED` 后再次 declare → `409 RECORDING_IDENTITY_CONFLICT`；IVR 必须使用已持久化的 declaration/credential 并调用 complete。该投影边界见 §4.13/R-A2-DECLARE-REPLAY。
- `clientReference` 只是可选关联提示，不构成身份、fingerprint 或 A0 列。

complete replay：

- 同 `recordingId` + 同 server-observed facts → 首次 `VALIDATED`/`QUARANTINED` 结果，不新增 media object/recording。
- 不同 object/version/hash/size/MIME/duration → 按上表隔离/冲突。
- `UPLOAD_OBSERVATION_REQUIRED` 后必须用同 `recordingId` 重试 complete；不得重新 declare/upload；重试间隔 Q8 deferred。
- `STORAGE_UNKNOWN` 失败关闭；不生成 media facts；同 identity 重试收敛。
- `complete` 成功不表示 evidence solidified；fixture 只断言 recording 状态，不声称 evidence 完成。

### 4.11 重试、replay、顺序与冲突

| 场景 | 身份 | 规则 |
| --- | --- | --- |
| resolve 重试 | `callId + INBOUND_RESOLVE` fingerprint | 同 fingerprint → 首次 frozen decision，不重新 claim/reserve；不同 fingerprint → `409 SESSION_IDENTITY_MISMATCH`；replay 先于 task housekeeping |
| activation confirm 重试 | body `eventId` + canonical confirmation fingerprint；`(callId, ACTIVATION_NEXT_ACTION)` | 同语义（含新 eventId）→ 首次 decision；同 eventId 不同语义/不同 fingerprint → 409；不改写 resolve |
| direct authorize 重试 | body `eventId` 排除在 fingerprint 外；`(callId, DIRECT_AUTHORIZATION)` | 同语义含新 eventId → 首次授权；不同 callee/prompt/key/content → 409；不创建第二 reservation |
| call event 重试 | `(IVR_EVENT, eventId)` | 同 fingerprint → 首次 receipt；不同 → `409 EVENT_ID_CONFLICT`；首次终态吸收 |
| 乱序事件 | 事件身份 + `occurredAt`/facts | 一致的乱序事实各应用一次；缺失 sequence gap 不阻止权威终态；矛盾/倒退 → `QUARANTINED` |
| 重复 terminal | 不同 eventId 的 terminal | 首个终态吸收；后续改变事实 → `QUARANTINED`；完全一致 → `IGNORED` |
| recording declare | `recordingId` | 同 declaration + `DECLARED` → 首次 201；其它 → 409 |
| recording complete | `recordingId` + server-observed facts | 同 facts → 首次终态；不同 → 隔离/冲突 |
| lost response（所有写） | 原 identity | 重发完全相同 identity/payload；不得生成第二 claim/reservation/B-leg/media/event；5xx/网络可重试，4xx 冲突不自动改写身份 |
| IVR restart | `callId` / local outbox | 读取本地 operation/outbox；重发未 ACK 事件；B-leg 按 §4.7 恢复；不得新 callId/新 resolve 取新授权 |

### 4.12 稳定错误映射

不新增 Problem code。A1 的 exact per-operation error set 是唯一映射：

| 协议条件 | operation | HTTP | `Problem.code` | 可重试 | 副作用 |
| --- | --- | ---: | --- | --- | --- |
| A1 closed schema/字段类型/长度 | 任一 | 422 | `VALIDATION_FAILED` | 否（修 payload） | 无 |
| 版本 malformed/不支持、未知 eventType、A2 payload 字段/leg 规则违反 | 任一 | 400 | `IVR_ARGUMENT_INVALID` | 否 | 无 session/call/event 写入 |
| decision/session/access-number/fingerprint 冲突 | resolve/confirm/direct | 409 | `SESSION_IDENTITY_MISMATCH` | 否 | 无 |
| confirm/direct 的 call 不存在 | confirm/direct | 409 | `SESSION_IDENTITY_MISMATCH` | 否 | 无（A1 未声明 CALL_UNKNOWN） |
| event 身份/语义冲突 | events | 409 | `EVENT_ID_CONFLICT` | 否 | 首次结果不变 |
| event/declare/complete 的 parent call 不存在 | events/declare/complete | 404 | `CALL_UNKNOWN` | 否 | 不建占位 |
| recording identity/declaration 冲突 | declare/complete | 409 | `RECORDING_IDENTITY_CONFLICT` | 否 | 无 |
| complete 尚无 upload observation | complete | 409 | `UPLOAD_OBSERVATION_REQUIRED` | 是（同 identity） | 无 |
| upload 已过期 | complete | 409 | `UPLOAD_EXPIRED` | 否 | recording 隔离，无 media fact |
| 存储结果未知 | complete | 503 | `STORAGE_UNKNOWN` | 是（同 identity） | 无 |
| 限流/依赖/内部 | 任一 | 429/503/500 | `RATE_LIMITED`/`SERVICE_UNAVAILABLE`/`INTERNAL` | 视情况 | 无/回滚 |
| 业务拒绝（停用/无权益/冲突/无任务等） | resolve/confirm/direct | 200 | `IvrDecision` | 否 | 按 decision |
| 事件事实矛盾/顺序/leg 未知/未到截止等 | events | 200 | `CallEventReceipt{QUARANTINED}` | 否（按 reason） | 不应用事实 |
| 线路结果未知 | events | 200 | `CALL_TERMINATION_UNKNOWN` 事件 | 对账 | UNKNOWN occupied |

事件 quarantine 不是 Problem；处理结果 200 + `processingStatus`。`OUTBOUND_NOT_STARTED` 的“未到截止”使用 `QUARANTINED/AUTHORIZATION_NOT_EXPIRED`，不是 400，以便 IVR 在截止后以新事件重报且首条记录可审计。

`OUTBOUND_NOT_STARTED` 对不存在的 call 同样是 `404 CALL_UNKNOWN`；不得据此创建占位或把 release 视为已完成；IVR outbox 记 `PERMANENT_FAILURE`。这是 resolve 从未 commit 的失败关闭路径，与 resolve 已 commit 但响应丢失（同 `callId` 取回 frozen decision）不同（fixture `s2-11` 对 `s2-05`）。

### 4.13 A1/A0 边界说明（不修改任何 accepted contract）

| 编号 | 说明 | 处理 |
| --- | --- | --- |
| N-1 | `docs/13` §6.2 示例含 `requestId`/`nodeId`/`accessNumber`/`callerNumber`/`receivedAt`；A1 已改为服务端派生并移除这些 body 字段 | A2 跟随 A1；不新增这些字段 |
| N-2 | A0 §10.17 fingerprint 含 `sessionId`/`entryMode`/`nodeId`/`accessNumberHmac`/`callerNumberHmac`，A1 request 不携带 | 全部服务端派生；`entryMode` 由 operation 决定：resolve=`INITIAL_RESOLVE`、confirm=`POST_ACTIVATION`、direct 走其 purpose chain |
| N-3 | A1 confirm 用 `decision:ACCEPT` + `contentSha256`；A0 fingerprint 用 `confirmationKey`/`confirmationContent` | 固定映射：`confirmationKey="1"`（docs/13 的明确按键 1），`confirmationContent=contentSha256`；服务端再校验当前 prompt/policy |
| N-4 | A0 §10.14 把 `task.taskId` 标为 `uuid`；A0 §4.23 `task_id` 是 `text` 1-64，A1 是 string 1-64 | 采用 normative schema/A1；记录 `R-A2-A0-TASKID-TYPE`，不修改 A0，不阻塞 |
| N-5 | A1 declare 只有 `201` + `status: DECLARED`；A0 internal service 可能已有 `UPLOADED` 行 | A2 定义 first-declaration replay；上传后 re-declare 失败关闭；记录 `R-A2-DECLARE-REPLAY` |
| N-6 | A0 `inbound_session.ABANDONED` 无 A1 触发 | v1 不 wire；session 保持 `RECEIVED`/`CONFIRMED`/`REJECTED`；记录 `R-A2-SESSION-ABANDONED` |
| N-7 | A1 `IvrEventPayload` 是 opaque | A2 在 payload 内 closed；A1 不变 |
| N-8 | A1 complete 明确 evidence solidification 是 later owner flow | A2 只映射 recording/media，不新增 evidence endpoint，不声称 evidence 完成 |
| N-9 | A1 `RecordingCompletionRequest.clientReference` 无 A0 列 | 仅 correlation hint，不参与身份/fingerprint/A0 |
| N-10 | A1 同时有 `uploadExpiresAt` 与 `upload.expiresAt` | 二者都等于 A0 `recording.upload_expires_at`，必须相等 |

## 5. 身份/所有权/前置条件矩阵

| 对象 | 签发/owner | wire/A1 位置 | 唯一性/生命周期 | 状态前置 | replay/conflict | A0/A1 映射 |
| --- | --- | --- | --- | --- | --- | --- |
| `callId` | IVR gateway | resolve body；所有路径；event/recording parent | 跨节点全局唯一；1-128；通话生命周期；immutable | 首次呼入创建；后续必须已存在 | 同 ID 同 fingerprint replay；跨 caller/access/node 冲突 | `inbound_session.call_id`、`call_record.call_id`；resolve/path |
| `sessionId` | backend | 不 wire | UUID；每 call 一个；immutable | resolve 创建/复用 | 由 callId 派生；无客户端输入 | `inbound_session.id` |
| `decisionId` | backend | `IvrDecision.decisionId` | UUID；每个 `(callId,purpose)` 一个；冻结 | 首个 decision 提交后 | 同 fingerprint replay；changed fingerprint 409 | `ivr_decision.id`；`uq_ivr_decision_call_purpose` |
| decision fingerprint | backend | 不 wire | 每个 purpose 一个 canonical SHA-256 | decision 计算时 | 同 fingerprint replay；changed → 409 | A0 §10.17 |
| `taskId` | backend | `IvrDecision.task.taskId`（仅 task 存在） | 1-64 text；全局唯一；claim 后 immutable | `PENDING` claim | 同 task replay；不同 task 不得替换 | `ivr.outbound_call_task.task_id`；reciprocal `call_record.task_id` |
| `reservationId` | backend | `IvrDecision.reservationId` | UUID；每 call 一个；`HELD` terminal once | 授权/claim 提交 | replay 不续期；同 call 不建第二 reservation | `usage_reservation`；`uq_usage_reservation_call` |
| action / executable intent | backend | `IvrDecision.action`（direct success null） | 每个 decision 一个 | 冻结 decision | replay 返回同 action；IVR 不得改选 | A0 action registry；`ck_ivr_decision_action` |
| `eventId` | IVR | event body | 1-128；`IVR_EVENT` 域全局唯一；immutable | parent call 存在 | 同 ID/fingerprint 首次 receipt；changed → `EVENT_ID_CONFLICT` | `call_event.event_id`；`uq_call_event_domain_id` |
| `sequenceNo` | IVR | event body optional | 正整数；每 call 唯一 when present | event 创建 | 不同 event 复用 → `SEQUENCE_CONFLICT` | `call_event.sequence_no`；partial unique |
| `legId` | IVR | event body / terminal payload | 1-64；每 call 唯一；immutable | leg event/terminal | 同 leg facts 幂等；冲突隔离 | `call_leg(call_id, leg_id)` |
| B-leg operation | IVR local | 不 wire | key=`callId`；每 call 一个；本地 durable | 授权 decision 后、line 前 | 只能恢复，不能新建第二 B-leg | A0 无表；Q28 |
| `recordingId` | IVR | declare/complete body/path | 1-128；全局唯一；immutable | call 已知；upload 未过期 | 同 declaration/observed facts replay；changed → `RECORDING_IDENTITY_CONFLICT` | `evidence.recording.recording_id` |
| upload credential | backend | `RecordingDeclaration.upload.credential` | 短时、单对象；非 identity；非 return-once | declaration 成功 | 可重签/重放但不得换 recording/object；日志脱敏 | 无 A0 列；A0 upload session |
| upload expiry | backend | `uploadExpiresAt` / `upload.expiresAt` | 与 A0 `recording.upload_expires_at` 相同；不续期 | declaration 创建 | replay 不延长；过期 → `UPLOAD_EXPIRED` | `recording.upload_expires_at` |
| terminal fact | IVR → backend | `CALL_TERMINATED`/`CALL_TERMINATION_UNKNOWN`/`CALL_TERMINAL_RECONCILED`/`OUTBOUND_NOT_STARTED` | 每 call 首个终态吸收；eventId 身份 | call 存在；状态允许 | duplicate → 首次；conflict → quarantine | `call_record` terminal fields；`call_event` |
| heartbeat | IVR | `CALL_HEARTBEAT` | eventId；可重复发送 | call 已知 | 同 ID replay；不影响业务 | `call_event` `IGNORED` |
| bridge fact | IVR | `BRIDGE_STARTED`/terminal `bridgedAt` | 无独立 backend ID；callId + legIds | 两腿已 answered（progress）或 terminal | 一致 → 幂等；倒退 → quarantine | `call_record.bridged_at` |
| recording start/end | IVR | `RECORDING_STARTED`/`RECORDING_ENDED`/terminal times | callId + eventId | bridge 已发生 | 一致 → 幂等；矛盾 → quarantine | `call_record.recording_started_at/recording_ended_at` |
| media object | backend | 不 wire | 内部 UUID/version/hash；immutable | upload observation | 同 object/version/hash 复用；不同 → conflict | `evidence.media_object` |
| evidence record | backend | 不 wire | 内部 UUID；downstream | recording `VALIDATED` + owner flow | A2 不定义 wire replay | `evidence.evidence_record/evidence_event` |
| settlement fact | backend | 不 wire | reservation/type + source event 唯一 | accepted terminal | duplicate → 首次；conflict → quarantine | `usage_fact`；`usage_reservation` |
| IVR event outbox | IVR local | 不 wire | `(callId,eventId)`；immutable payload | event 生成 | 重传同 identity | Q28 local |

## 6. 状态机、不变量与合法/非法路径

### 6.1 Backend A0 状态与协议触发

| Aggregate | 合法转移 | 协议触发 | 守卫 |
| --- | --- | --- | --- |
| `outbound_call_task` | `PENDING→CLAIMED` | resolve/confirm `DIAL_TASK_CALLEE` | 原子 claim + reservation + frozen decision；reciprocal call `task_id` |
|  | `CLAIMED→COMPLETED` | accepted `CALL_TERMINATED` (ENDED-class) 或 reconciliation | 同事务 settlement；首终态 |
|  | `CLAIMED→FAILED` | `CALL_TERMINATED` (FAILED-class)、`OUTBOUND_NOT_STARTED`、reconciled FAILED | 不重开；不重扣 |
|  | `PENDING→REPLACED/CANCELLED/EXPIRED/REJECTED` | customer/backend 操作 | A2 非 wire；保持 A0 |
| `inbound_session` | `RECEIVED→CONFIRMED` | 成功 activation confirmation | 原 decision `GUIDE_ACTIVATION`；key 1；current consent |
|  | `RECEIVED→REJECTED` | suspended/competing rejection | 仅 never-confirmed session |
|  | `ABANDONED` | 无 v1 wire | owner-internal；见 R-A2-SESSION-ABANDONED |
| `call_record` | new→`ACTIVE` | resolve 非终态 action / direct auth | callId 根 |
|  | new→`REJECTED` terminal | competing call / invalid | `end_reason=CALL_IN_PROGRESS` |
|  | `ACTIVE→ENDED` | `CALL_TERMINATED` ENDED-class | 设置 ended_at/authoritative_seconds/end_reason |
|  | `ACTIVE→FAILED` | `CALL_TERMINATED` FAILED-class / `OUTBOUND_NOT_STARTED` | 设置 ended_at；0 或实际秒数 |
|  | `ACTIVE→UNKNOWN` | `CALL_TERMINATION_UNKNOWN` | ended_at/seconds NULL；occupied |
|  | `UNKNOWN→ENDED/FAILED` | `CALL_TERMINAL_RECONCILED` | 权威 reconciliation；引用 unknown event |
| `call_leg` | create `RINGING`/`ANSWERED` | leg events/terminal | 1 caller + 0/1 callee |
|  | `RINGING→ANSWERED` | `CALLEE_LEG_ANSWERED` | 时间非递减 |
|  | `ANSWERED→ENDED/FAILED` | accepted terminal | leg outcome/endReason |
| `ivr_decision` | frozen | resolve/confirm/direct | `(callId,purpose)` 唯一 |
| `usage_reservation` | `HELD→SETTLED/RELEASED/RECONCILING` | accepted terminal settlement | 一次终态；未知不释放 |
|  | `RECONCILING→SETTLED/RELEASED` | `CALL_TERMINAL_RECONCILED`/ops | 无 tolerance |
| `recording` | `DECLARED→UPLOADED` | server upload observation | 非 wire；complete 可返回 observation required |
|  | `UPLOADED→VALIDATED/QUARANTINED` | complete | server facts |
|  | `DECLARED→QUARANTINED` | expiry/storage permanent failure | 无成功 media fact |
| `evidence_record` | `PENDING→SOLIDIFIED/QUARANTINED/FAILED` | evidence owner downstream | A2 不直接 wire |

### 6.2 IVR local B-leg / outbox

见 §4.7。补充不变量：

- `lineAttemptCount` 单调 0→1，永不为 2；`dispatchMarker` 只有在 `lineAttemptCount=0` 时允许进入。
- `DISPATCH_UNKNOWN` 不得直接进入 `PREPARED`；必须先由权威线路/CDR 进入 `NOT_DISPATCHED`。
- `AUTHORIZATION_EXPIRED` 只能由 `NOT_DISPATCHED` 且已过截止进入；不得由 timeout/未知进入。
- event outbox 的 `eventId`/payload immutable；`ACKED`/`PERMANENT_FAILURE` 为本地终态。

### 6.3 不变量

| ID | 不变量 |
| --- | --- |
| `INV-A2-01` | 一个 `callId` 对应一个 inbound session/call；不得为同一呼入新建第二 callId。 |
| `INV-A2-02` | 一个 decision 只有一个 executable intent；IVR 不重算路由、不换被叫。 |
| `INV-A2-03` | 一个 call 最多一个 B-leg operation 与一次实际 line request；未知/重启不产生第二次外呼。 |
| `INV-A2-04` | 一个 call 最多一个 reservation、一次 terminal settlement/release、一个 usage fact。 |
| `INV-A2-05` | 首个 accepted terminal 吸收；终态不回退；`UNKNOWN` 只经 `CALL_TERMINAL_RECONCILED` 收敛。 |
| `INV-A2-06` | `(callId,purpose)` 决策不可改写；confirm 不改写 resolve。 |
| `INV-A2-07` | 单主叫单非终态任务与 reciprocal `task_id`/`claimed_call_id` 保持 A0。 |
| `INV-A2-08` | event identity immutable；同 identity 同 fingerprint 首结果；changed → conflict/quarantine。 |
| `INV-A2-09` | recording identity immutable；declared facts 不覆盖 server-observed facts；complete 不固化 evidence。 |
| `INV-A2-10` | task 路径从不确认被叫；direct 路径从不 claim task；无 latest-callee fallback。 |
| `INV-A2-11` | heartbeat 不改变业务状态、不释放、不授权、不触发 redial。 |
| `INV-A2-12` | 不输出 callee 明文到 redacted snapshot/log/audit/outbox；不输出 object key/URL/credential 到日志/公共响应。 |
| `INV-A2-13` | 协议版本在 resolve 冻结；不支持版本在副作用前失败；mid-call 不升级。 |
| `INV-A2-14` | 任何 unknown outcome 失败关闭：不 redial、不 double reserve/settle、不生成 contradictory facts。 |
| `INV-A2-15` | declaration/complete 重试复用 `recordingId`；不得创建第二 recording/media object。 |

### 6.4 合法/非法/重复/冲突/乱序路径

| 场景 | 期望 |
| --- | --- |
| resolve 首次、存在有效 task、全部条件满足 | 200 `DIAL_TASK_CALLEE`；task `CLAIMED`；reservation `HELD`；decision 冻结；IVR 只拨 frozen callee |
| resolve 同 callId 同 fingerprint 重试 | 200 首次 decision；不重新 claim、不新建 reservation、不续期 |
| resolve 同 callId 不同 caller/access/node/binding fingerprint | 409 `SESSION_IDENTITY_MISMATCH`；无副作用 |
| task TTL 恰好/超过 `expiresAt` | 不 claim；返回非外呼 decision；不猜历史被叫 |
| 两个呼入并发同一 caller | 一个成功；另一个 `REJECT_CALL_IN_PROGRESS` 或 `REJECT`/`CALL_IN_PROGRESS`；不双占 |
| `GUIDE_ACTIVATION` → key 1 confirm | 200 `ACTIVATION_NEXT_ACTION`；session `CONFIRMED`；trial 一次；随后 action 与 resolve 共享形状 |
| confirm 无 `/GUIDE_ACTIVATION` origin | 200 `REJECT`/`CONFIRMATION_NOT_ALLOWED`；无 activation/trial/reservation |
| 第二次 confirm（同语义新 eventId） | 首次 `ACTIVATION_NEXT_ACTION` replay；无第二 trial/reservation |
| direct authorize 在 task-backed call | 200 `REJECT`/`TASK_CLAIMED`；无 reservation；task 不变 |
| direct authorize 在 `ENTER_CALLEE`/`CONFIRMED→ENTER_CALLEE` chain | success `action=null` + reservation；无 task |
| direct authorize 改 callee 重试 | 409 `SESSION_IDENTITY_MISMATCH`；无第二 reservation |
| 事件重复同 eventId | 200 首次 receipt；无重复 settlement |
| 事件乱序但一致（例如 answer 晚于 bridge，但时间顺序正确且 leg 已存在） | 按可达事实应用一次；缺失 sequence gap 不阻止 terminal |
| 事件矛盾（bridge before answer、terminal 后退、重复 terminal 不同时长） | 200 `QUARANTINED`/`CONTRADICTORY_FACT`；不应用 |
| 不同 event 复用 sequenceNo | 新事件 `QUARANTINED`/`SEQUENCE_CONFLICT`；不改原事件 |
| 未知 call 的 event/heartbeat/declare/complete | 404 `CALL_UNKNOWN`；不建占位 |
| 心跳 before/after terminal | `IGNORED`/`HEARTBEAT_IGNORED`；无业务效果 |
| 权威终态 | call/task terminal + settlement；首终态吸收 |
| 线路结果未知 | `CALL_TERMINATION_UNKNOWN`；call `UNKNOWN`；reservation occupied；不释放/不重拨 |
| `UNKNOWN` 后收到 `CALL_TERMINATED` | `QUARANTINED`/`USE_RECONCILIATION_EVENT` |
| `UNKNOWN` 后 `CALL_TERMINAL_RECONCILED` 引用原 unknown | `UNKNOWN→ENDED/FAILED`；settle/release；task terminal |
| `CALL_TERMINAL_RECONCILED` 在非 UNKNOWN | `QUARANTINED`/`NOT_UNKNOWN` |
| resolve 已 commit；授权截止且证明从未 dispatch | `OUTBOUND_NOT_STARTED`；task `FAILED`；reservation `RELEASED`；无 usage fact |
| resolve 已 commit 但响应丢失；同 `callId` 重试 resolve | 返回首次 frozen decision；不重新 claim/reserve；后续未启动上报可终结既有 call/reservation |
| resolve 从未 commit；本地无 B-leg/line attempt；之后上报 `OUTBOUND_NOT_STARTED` | `404 CALL_UNKNOWN`；不建占位 call/task/reservation；不 release；无 usage；outbox `PERMANENT_FAILURE` |
| 授权截止但 B-leg 已在截止前启动 | 继续按冻结秒数完成；不中断/不释放 |
| 未到截止就报 `OUTBOUND_NOT_STARTED` | `QUARANTINED`/`AUTHORIZATION_NOT_EXPIRED` |
| 已有 outbound facts 后报 `OUTBOUND_NOT_STARTED` | `QUARANTINED`/`OUTBOUND_ALREADY_STARTED` |
| 第二次 B-leg / redial | IVR-local 禁止；无 wire 消息；fixture 断言 `lineAttemptCount<=1` |
| stream 秒数 > authorized | 事件可记录但 reservation `RECONCILING`；不 settle/release/evidence |
| 秒数 0 known | `RELEASED`；无 usage fact |
| declare 同 identity 同 facts（DECLARED） | 201 首次 declaration；不续期/不新增 |
| declare 上传后 | 409 `RECORDING_IDENTITY_CONFLICT`；无状态变化 |
| complete before observation | 409 `UPLOAD_OBSERVATION_REQUIRED`；同 identity 重试 |
| complete after expiry | 409 `UPLOAD_EXPIRED`；recording 隔离；无 media fact |
| complete storage unknown | 503 `STORAGE_UNKNOWN`；无 media fact；同 identity 重试收敛 |
| complete 同 identity 同 server facts | 首次 `VALIDATED`/`QUARANTINED` |
| complete 不同 object/version/hash | pre-terminal 隔离；post-terminal 409 + 一条 conflict Outbox |
| protocolVersion 不支持 | 400 `IVR_ARGUMENT_INVALID`；无 session/call |
| 未知 eventType / payload 字段 | 400 `IVR_ARGUMENT_INVALID`；无 event |
| A1 closed body 未知字段 | 422 `VALIDATION_FAILED` |

## 7. 静态验证设计

### 7.1 工具链与命令

A2 校验器无第三方依赖，Node >=22。A1 bundle 由既有 A1 工具生成，不修改 A1。

在 repo root 执行：

```bash
cd api/openapi && npm ci && npm run check:s3
cd /mnt/d/codex-call
node protocol/ivr/validate.mjs --stage s1 \
  --model protocol/ivr/v1/ivr-protocol-v1.json \
  --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node protocol/ivr/validate.mjs --stage s2 ...   # 同参数
node protocol/ivr/validate.mjs --stage s3 ...   # 同参数
```

补充检查：

```bash
file -bi docs/16-production-ivr-protocol-spec.md protocol/ivr/v1/ivr-protocol-v1.json protocol/ivr/validate.mjs
rg -n "^(<<<<<<<|=======|>>>>>>>)" docs/16-production-ivr-protocol-spec.md protocol/ivr
rg -n "[ \t]+$" docs/16-production-ivr-protocol-spec.md protocol/ivr
git diff --check -- docs/16-production-ivr-protocol-spec.md protocol/ivr
git status --porcelain
```

### 7.2 A1 漂移检查

`validate.mjs` 从 bundled JSON 断言：

- IVR operation 恰为 6 个，operationId/method/path 精确一致；总 operation 数 49，IVR 面 6。
- 必需 schema 存在且字段/required/enum 与模型一致：`IvrDecision`、`IvrInboundResolveRequest`、`IvrActivationConfirmRequest`、`IvrDirectAuthorizationRequest`、`IvrCallEventRequest`、`CallEventReceipt`、`RecordingDeclarationRequest`、`RecordingDeclaration`、`RecordingCompletionRequest`、`RecordingCompletion`、`Problem`、`ProblemCode`。
- 每条协议 message 的 A1 operation/request/response 引用存在；模型字段是 A1 字段子集，A1 关键字段（callId/decisionId/action/purpose/reasonCode/taskMatch/callerState/reservation/expiry/recordingId/status）全部有映射。
- 每个模型错误码属于该 operation 的 `x-onecall-error-codes` 且属于 `ProblemCode`；六个 operation 的领域错误集与 `docs/15` §4 精确一致。
- `IvrEventPayload` 在 A1 仍为 opaque；模型不声称修改 A1 envelope。
- 负例：任何新增 IVR path/operation、任何新 Problem code、把 `TASK_CONFLICT`/`CALL_IN_PROGRESS`/`CONFIRMATION_NOT_ALLOWED` 当 Problem code、任何模型引用不存在 schema/字段，均失败。

### 7.3 A0 漂移检查

从 `docs/14` 的 catalog 与 named CHECK 提取并精确比较：

- catalog 恰 44 表；模型引用的每个 A0 表都在 catalog 内；无新表。
- `ck_outbound_task_status`、`ck_inbound_session_status`、`ck_call_record_status`、`ck_call_leg_state`、`ck_call_event_processing_status`、`ck_ivr_decision_action`、`ck_ivr_decision_purpose`、`ck_ivr_decision_reason`、`ck_usage_reservation_status`、`ck_recording_status`、`ck_evidence_status`、`ck_usage_fact_type`、`ck_usage_fact_event_domain`、`ck_idempotency_scope` 的集合与模型 registry 精确相等。
- 模型引用的列必须出现在对应 §4 表的列清单内；模型不得引用 A0 未定义列。
- `docs/14` §10.14 的 decision 字段名必须覆盖模型 `decisionFields`；`task.taskId` 的 N-4 说明被明确记录，不静默修改。
- 事件域、IVR reason/action registry、`usage_fact` source domain 与模型一致。
- 负例：模型新增状态/action/reason/表/列、或遗漏 A0 named CHECK 值，均失败。

### 7.4 文档/模型一致性（按 stage 投影）

`validate.mjs --stage s1|s2|s3` 必须选择唯一 stage 投影；缺少或未知 `--stage` 直接失败。任一 stage 都必须解析并精确比较三个标记块，不得跳过 `protocol-event-map`，也不得接受行/列子集。§7.4.1 定义每个 stage 的精确形态；§7.4.2 定义拒绝条件。

#### 7.4.1 Stage 投影契约

S1/S2/S3 都完整冻结的标记块：

| 标记块 | 行数 | 精确列 |
| --- | ---: | --- |
| `protocol-message-map` | 12 | `messageId,slice,operationId,methodPath,requestSchema,responseSchema,identity` |
| `protocol-identity-map` | 22 | `identity,owner,uniqueness,a0a1Mapping` |

- 12 个 message row 从 S1 起全部存在；`slice` 只标注行为归属（6 个 decision message=S1，`call-event.request`/`call-event.receipt`=S2，4 个 recording message=S3），不改变 row 存在性。message identity/operation/schema 是 S1 的 A1 映射交付；S2/S3 只补相应 payload/媒体行为，不得改 message row。
- 22 个 identity row 在 S1 冻结；后续 stage 不得改 identity/owner/唯一性/A0/A1 映射。
- 两个 registry 在 S1 即覆盖完整 v1 surface，是 wire 身份冻结，不代表 S2/S3 行为已实现。
- row 顺序固定：message 按 §4.3.1 顺序，identity 按 §5 顺序，event 按 §4.5（即下表）顺序；doc 与模型同序。

`protocol-event-map` 与模型 `eventTypes` 按 stage 切换：

| stage | doc 标记块精确列 | event row 数 | 模型 `eventTypes[]` 精确键集 | 语义状态 |
| --- | --- | ---: | --- | --- |
| S1 | `eventType,payloadState` | 11 | `{eventType,payloadState}`；`payloadState` 恒为 `DEFERRED_S2` | identity-only；payload/envelope/state/terminal 语义不存在也未被授权 |
| S2 | `eventType,envelopeLegId,payloadFields,normalProcessingOutcome` | 11 | `{eventType,envelopeLegId,payloadFields,normalProcessingOutcome}` | 完整冻结 |
| S3 | 与 S2 完全相同 | 11 | 与 S2 完全相同，且 `eventRegistry.sha256` 重算相等 | 只读；不得修改 |

- 11 个 eventType 固定为下表名称；缺失/多余/重复/改名/重排均失败。
- `envelopeLegId ∈ {REQUIRED, ABSENT}`。
- `payloadFields` 是该 event closed payload 的允许键集合，按字典序；必填字段写 `field`，可选/条件字段写 `field?`；空 payload 模型用 `[]`，doc 用 `{}`。
- `normalProcessingOutcome ∈ {APPLIED, IGNORED}`，表示正常受理结果；quarantine/ignore reason 由 §4.5/§4.12 shared registry 按事实决定，不改变该列。
- S1 doc 必须在该标记块紧邻文本明确写“event identities only; payload/state/terminal semantics deferred to S2”；不得复制完整 payload 表，不得把 `DEFERRED_S2` 描述为已完成协议。S1 事件块除 `payloadState` 哨兵列外，出现 `envelopeLegId`/`payloadFields`/`normalProcessingOutcome` 或任何其它 payload/状态/终态列即失败。
- S2 必须删除 `payloadState`，并新增冻结下列全部内容：`eventEnvelope`（A1 envelope 精确字段与 legType 派生规则）、11 个 closed payload、`endReasons`（9）、`quarantineReasons`（10）、task/session/call/leg/decision/reservation state machines、`blegOperation`（phases/字段/恢复判定）、`eventOutbox`（状态与 permanent/retryable 分类）、以及 §4.8/§4.11 的终态吸收/顺序/恢复语义。任一缺失或残留 S1 partial 形态失败。
- S2 通过后 model 写入 `eventRegistry`（恰含 `frozenAt="s2"` 与 `sha256=<对 11 个 eventTypes 条目按 §4.5 固定顺序做 canonical JSON 的 SHA-256>`）；canonical JSON 指 UTF-8、对象键排序、无多余空白。S3 validator 重算并必须相等；digest 是漂移守卫，不是修改授权。

S2/S3 event map 冻结值（与 §4.5 一致）：

| eventType | envelopeLegId | payloadFields | normalProcessingOutcome |
| --- | --- | --- | --- |
| `CALLER_LEG_ANSWERED` | `REQUIRED` | `[]` | `APPLIED` |
| `CALLEE_LEG_RINGING` | `REQUIRED` | `[]` | `APPLIED` |
| `CALLEE_LEG_ANSWERED` | `REQUIRED` | `[]` | `APPLIED` |
| `BRIDGE_STARTED` | `ABSENT` | `calleeLegId,callerLegId` | `APPLIED` |
| `RECORDING_STARTED` | `ABSENT` | `[]` | `APPLIED` |
| `RECORDING_ENDED` | `ABSENT` | `[]` | `APPLIED` |
| `CALL_TERMINATED` | `ABSENT` | `authoritativeRecordingSeconds,bridgedAt?,calleeLegAnsweredAt?,calleeLegId?,calleeLegOutcome?,calleeLegStartedAt?,callerLegAnsweredAt?,callerLegId,callerLegOutcome,endReason,recordingEndedAt?,recordingStartedAt?` | `APPLIED` |
| `CALL_TERMINATION_UNKNOWN` | `ABSENT` | `reasonCode` | `APPLIED` |
| `CALL_TERMINAL_RECONCILED` | `ABSENT` | `authoritativeRecordingSeconds,bridgedAt?,calleeLegAnsweredAt?,calleeLegId?,calleeLegOutcome?,calleeLegStartedAt?,callerLegAnsweredAt?,callerLegId,callerLegOutcome,endReason,recordingEndedAt?,recordingStartedAt?,resolvesUnknownEventId` | `APPLIED` |
| `OUTBOUND_NOT_STARTED` | `ABSENT` | `reasonCode,reservationId?` | `APPLIED` |
| `CALL_HEARTBEAT` | `ABSENT` | `[]` | `IGNORED` |

`CALL_TERMINAL_RECONCILED` 的完整字段串：`authoritativeRecordingSeconds,bridgedAt?,calleeLegAnsweredAt?,calleeLegId?,calleeLegOutcome?,calleeLegStartedAt?,callerLegAnsweredAt?,callerLegId,callerLegOutcome,endReason,recordingEndedAt?,recordingStartedAt?,resolvesUnknownEventId`。doc 的空 payload 单元写作 `{}`；validator 将 `{}` 规范化为空集后与模型 `[]` 比较。

模型顶层 section 的 stage 精确集合（不属于当前 stage 的 section 即 premature，必须不存在）：

| section | S1 | S2 | S3 |
| --- | --- | --- | --- |
| `modelVersion`、`protocolVersion`、`supportedVersions` | required | same | same |
| `messages`(12)、`identities`(22)、`actionSelection`、`decisionFields`、`actions`、`purposes`、`decisionReasons`、`errors`、`preconditions` | required & complete | same | same |
| `invariants`（15 个 cross-cutting ID） | required | same | same |
| `eventTypes` | 11 identity-only | 11 full | same as S2 |
| `eventRegistry` | absent | required | same as S2 |
| `eventEnvelope`、`endReasons`、`quarantineReasons`、`stateMachines`、`blegOperation`、`eventOutbox` | absent | required | same as S2 |
| `recordingLifecycle`、`mediaEvidenceBoundary` | absent | absent | required |

- `errors` 在 S1 即完整映射 A1 六个 operation 的既有错误集（12-message registry 需要它）；S2/S3 不得新增/改写 Problem code。
- `preconditions` 只覆盖 S1 决策路径（task claim、session confirm、decision freeze、reservation `HELD`）；S2 的 `stateMachines` 覆盖完整 terminal/failure 转移，且不得与 S1 `preconditions` 冲突。
- `invariants` 是 cross-cutting ID registry，S1 冻结 15 个 ID；S2/S3 增加 §6 prose、执行语义与 fixtures，但不新增 ID。
- doc 与模型的标记块必须逐行逐字段相等，包括 header、列顺序、排序和空值记号（`-`=空 schema/identity 值，`{}`=空 payload 字段集）。
- `fixtures` 的 stage ID 集合也必须精确：S1=`s1-01..s1-06`；S2=上述 + `s2-01..s2-11`；S3=上述 + `s3-01..s3-08`。validator 用 cumulative 集合比较，缺一或多一均失败。

#### 7.4.2 stage 拒绝条件

以下任一情况必须使校验失败并计入非零计数：缺 `--stage` 或未知值；缺任一标记块；header/列集合不精确；message/identity/event row 数不等于 12/22/11；event 块被跳过或只比较子集；当前 stage 应有字段/section 缺失；下一 stage 字段/section 提前出现；S1 event 块除 `payloadState` 哨兵外含 payload/state/terminal 列或非 `DEFERRED_S2` 值；S2/S3 event 块残留 `payloadState`；S3 event digest 不等；stage fixture ID 集合缺/多。输出计数 `doc_missing`、`doc_extra`、`doc_duplicate`、`doc_invalid`、`token_missing`、`stage_field_missing`、`stage_field_premature`、`fixture_id_missing`、`fixture_id_extra` 必须全为 0。token coverage 只检查当前 stage 存在的 registry。

### 7.5 Fixture 场景与断言

Fixture 格式：`{schemaVersion, id, slice, description, steps[]}`。每 step 为 request、local operation、restart、line outcome 或 assert；`expected` 包含 HTTP status、response 子集、Problem code、persisted fact snapshot、local B-leg/outbox 状态、fact counts。validator 用内置 `ProtocolMachine` 计算并比对，不能只回显 expected。`ProtocolMachine` 只是 fixture 求值器，不是 backend/IVR runtime，不连接数据库、线路或 COS。

| Fixture | Slice | 场景 | 关键断言 |
| --- | --- | --- | --- |
| `s1-01-task-path-normal` | S1 | resolve→DIAL_TASK_CALLEE | 200；task CLAIMED；reservation HELD；action contract 字段完整；IVR 不确认被叫 |
| `s1-02-direct-path-normal` | S1 | ENTER_CALLEE→direct success | `action=null`；reservation；无 task；无 task 字段 |
| `s1-03-activation-redecision` | S1 | GUIDE_ACTIVATION→confirm→DIAL_TASK_CALLEE | session CONFIRMED；trial 一次；resolve 不改写；decision 形状与 resolve 相同 |
| `s1-04-decision-exact-replay` | S1 | resolve/confirm/direct 同 fingerprint 重试 | 首次 decision；无第二 claim/reservation/trial |
| `s1-05-decision-semantic-conflict` | S1 | 同 callId 改 caller/access/callee/prompt | 409 SESSION_IDENTITY_MISMATCH；无副作用 |
| `s1-06-version-unsupported` | S1 | `protocolVersion=2.0`/malformed | 400 IVR_ARGUMENT_INVALID；无 session/call |
| `s2-01-task-terminal-completed` | S2 | 正常 bridge/recording/terminal | call ENDED；task COMPLETED；reservation SETTLED；usage fact 1；duration 正确 |
| `s2-02-event-duplicate-replay` | S2 | 同 eventId 重复 terminal/heartbeat | 首次 receipt；settlement/fact 不增加 |
| `s2-03-event-out-of-order` | S2 | 乱序但一致的 leg/bridge/recording/terminal | 事实一次应用；缺失 sequence 不阻止 terminal |
| `s2-04-terminal-unknown-reconciliation` | S2 | unknown→reconciled terminal | UNKNOWN occupied；reconcile 后 ENDED/FAILED；settle 一次 |
| `s2-05-response-loss-retry` | S2 | resolve（已 commit）/event/complete 响应丢失后重试 | resolve 同 callId 取回 frozen decision；同 identity 首次结果；无第二 B-leg/reservation/event |
| `s2-06-ivr-restart-bleg-recovery` | S2 | PREPARED/DISPATCH_UNKNOWN restart | PREPARED 首次拨；DISPATCH_UNKNOWN 不重拨；`lineAttemptCount<=1` |
| `s2-07-authorization-expiry-not-started` | S2 | 截止且证明未 dispatch | OUTBOUND_NOT_STARTED；task FAILED；reservation RELEASED；无 usage fact |
| `s2-08-redial-second-bleg-rejected` | S2（负例） | 尝试第二次 B-leg/重复 direct authorize | 无第二 B-leg；`lineAttemptCount<=1`；无第二 reservation |
| `s2-09-heartbeat-and-terminal` | S2 | heartbeat before/after terminal | IGNORED；无业务变化；terminal 仍吸收 |
| `s2-10-terminal-contradiction-quarantine` | S2（负例） | 第二个 terminal 不同时长/UNKNOWN 后 CALL_TERMINATED | QUARANTINED；不重开、不重扣 |
| `s2-11-resolve-never-committed-not-started` | S2（负例） | resolve 从未 commit（响应丢失）、同 callId 重试仍无 usable decision、本地无 B-leg/line attempt；之后上报 `OUTBOUND_NOT_STARTED` | 404 `CALL_UNKNOWN`；无 call/task/reservation placeholder；无 release/usage；outbox `PERMANENT_FAILURE`；与 `s2-05` 的 committed-but-response-lost 区分 |
| `s3-01-recording-validated` | S3 | declare→upload observe→complete VALIDATED | recording VALIDATED；server facts；无 object key 返回 |
| `s3-02-declaration-replay` | S3 | declare 响应丢失后同 identity 重试 | 同 201 首次快照；无新 recording；不续期 |
| `s3-03-upload-observation-required` | S3 | complete before observation | 409 UPLOAD_OBSERVATION_REQUIRED；同 identity retry 后 VALIDATED |
| `s3-04-upload-expired-quarantine` | S3 | 过期后 complete | 409 UPLOAD_EXPIRED；recording QUARANTINED；无 media fact |
| `s3-05-storage-unknown-retry` | S3 | complete storage unknown | 503 STORAGE_UNKNOWN；无 media fact；retry 收敛 |
| `s3-06-recording-identity-conflict` | S3 | 同 recordingId 不同 object/hash | 409 RECORDING_IDENTITY_CONFLICT；无覆盖 |
| `s3-07-post-terminal-conflict-fact` | S3 | VALIDATED 后冲突 complete | 409；状态/revision 不变；conflict Outbox 恰 1 |
| `s3-08-end-to-end-task-media` | S3 | task resolve→B-leg→terminal→settle→declare→complete→evidence handoff boundary | 全链路 counts 正确；evidence 不由 complete 固化 |

累计 fixture 数：S1=6，S2=17，S3=25。S2 slice=`s2-01..s2-11`（11 个，其中负例 `s2-08`/`s2-10`/`s2-11` 共 3 个），S3 slice=`s3-01..s3-08`（8 个）。validator 必须断言每个 stage 的 cumulative fixture ID 集合精确相等。

### 7.6 负例与确定性

- 负例必须被模型/runner 拒绝：第二 B-leg、第二 reservation、redial after unknown、changed event fingerprint、changed decision fingerprint、changed recording identity、unsupported version、unknown event type、out-of-order contradictory fact、over-authorized duration 被 settle、complete 固化 evidence、heartbeat 释放预占。
- stage gating 负例同样必须被拒绝：缺当前 stage 字段/section、提前出现下一 stage 字段/section、跳过 event 块、只比较 event 行子集、S1 除 `payloadState` 哨兵外出现 payload/state 列、S2/S3 残留 `payloadState`、S3 event digest 变化、stage fixture ID 缺/多。
- 无网络、无当前时间、无随机；fixture 时间固定；对象 key 排序；重复运行 stdout 与 fixture 结果 SHA-256 一致。
- validator 输出固定计数行（`fixtures=` 与 `negative=` 均为 cumulative；`slice_fixtures=` 给出各 slice 数）：

```text
stage=s1 messages=12 event_types=11 event_map_form=identity-only identities=22 fixtures=6 passed=6 failed=0 negative=0 slice_fixtures=6
a1_operations=49 ivr_operations=6 a1_drift=0
a0_tables=44 a0_status_drift=0 a0_registry_drift=0 a0_column_drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
invariants=15
PASS protocol-validation (s1)
```

```text
stage=s2 messages=12 event_types=11 event_map_form=full-frozen identities=22 fixtures=17 passed=17 failed=0 negative=3 slice_fixtures=6+11
a1_operations=49 ivr_operations=6 a1_drift=0
a0_tables=44 a0_status_drift=0 a0_registry_drift=0 a0_column_drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
invariants=15
event_registry_frozen=s2 event_registry_sha256=<digest>
PASS protocol-validation (s2)
```

```text
stage=s3 messages=12 event_types=11 event_map_form=full-frozen identities=22 fixtures=25 passed=25 failed=0 negative=3 slice_fixtures=6+11+8
a1_operations=49 ivr_operations=6 a1_drift=0
a0_tables=44 a0_status_drift=0 a0_registry_drift=0 a0_column_drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
invariants=15
event_registry_frozen=s2 event_registry_sha256=<digest>
PASS protocol-validation (s3)
```

`negative` 只统计 §7.5 中显式标注 `(负例)` 的累计 fixture 数；其它包含 rejection/retry 断言的普通 fixture 不计入。S2/S3 的 `event_registry_sha256` 必须相等。

### 7.7 不可证明项

- 不能证明真实 mTLS/签名/时间窗/nonce/时钟同步（Q7/Q15/Q25）。
- 不能证明真实 SIP/线路/CDR、B-leg 拨号、振铃/接通、桥接、限时挂断（Q7/Q28/IVR）。
- 不能证明 PostgreSQL 并发、唯一约束、锁顺序、事务回滚、savepoint、task/call reciprocal link。
- 不能证明 COS 对象版本/上传观测/恶意文件扫描/哈希/时长校验。
- 不能证明 evidence creation/solidification 的运行时触发与 source event identity。
- 不能证明生产容量、心跳保留、SLO、告警阈值。
- fixtures 是协议模型级断言，不是生产验收矩阵。

## 8. 实施切片（3 个）

### 8.1 切片原则与最小性

- S1 是“授权真相”：一次性决定 IVR 该做什么；验证 resolve/confirm/direct 共享 action contract、identity、version、replay/conflict。
- S2 是“执行真相”：一次性决定实际发生了什么；验证 B-leg exactly-once、leg/bridge/recording 事件、heartbeat、terminal/UNKNOWN、settlement/task 终态。
- S3 是“媒体真相”：一次性决定录音/媒体事实；验证 declare/upload/complete/quarantine、conflict、evidence 边界，并用端到端 fixture 覆盖 recovery。
- 不按 owner 拆（每片都跨 IVR/backend），不按消息类型机械拆。合并 S1+S2 会让“授权”与“执行”失败模式无法独立评审；把 S3 并入 S2 会让 telephony terminal 与 media/evidence 状态混在一起。三片各有独立验证信号和 stop condition，是满足“≤3”且不过度拆分的最小方案。

### 8.2 S1 — 冻结动作、会话身份与版本

| 项 | 内容 |
| --- | --- |
| 目标 | 建立 canonical 协议规范与机器模型基座；完成 resolve/confirm/direct 的共享 action contract、消息身份、版本、replay/conflict、错误映射与 S1 fixtures |
| 允许文件 | `docs/16-production-ivr-protocol-spec.md`（新建）；`protocol/ivr/v1/ivr-protocol-v1.json`（新建）；`protocol/ivr/v1/fixtures/s1-*.json`（新建）；`protocol/ivr/validate.mjs`（新建）；`docs/gateflow/a2-production-ivr-protocol-spec-s1-implementation.md`（新建） |
| 前置 | accepted A1/A0；A1 `npm ci`/Redocly 可用；无 runtime 依赖 |
| 变更 | `protocolVersion=1.0` 与兼容规则；12 message registry；22 identity 矩阵；action/response/reason registry；session/decision/task 状态前置；`confirmationKey`/`confirmationContent` 映射；direct purpose chain；错误映射；15 个 cross-cutting invariant ID；doc 三个标记块：`protocol-message-map` 12 行完整（含 S2/S3 消息的 A1 身份/schema，仅 `slice` 标注归属）、`protocol-identity-map` 22 行完整、`protocol-event-map` 11 行 identity-only（`eventType,payloadState=DEFERRED_S2`）。S1 不包含 event payload/envelope/state/terminal、endReason、quarantine registry、B-leg/outbox 或 recording/media 生命周期语义 |
| 状态转移 | task `PENDING→CLAIMED`（仅 task decision）；session `RECEIVED→CONFIRMED`；decision 冻结；reservation `HELD` 创建；拒绝路径不 claim/reserve |
| 错误处理 | A1 exact sets；409 仅 identity/fingerprint；业务拒绝 200 `IvrDecision`；未知字段 422；版本/payload 400 |
| 验证命令 | `cd api/openapi && npm ci && npm run check:s3`；`node protocol/ivr/validate.mjs --stage s1 ...`；`git diff --check`；UTF-8/冲突标记/空白检查 |
| 期望断言 | `stage=s1 messages=12 event_types=11 event_map_form=identity-only identities=22 fixtures=6 passed=6 negative=0`；A1 49/6、A0 44；a1_drift=a0_*_drift=doc_*=token_missing=stage_field_missing=stage_field_premature=fixture_id_missing=fixture_id_extra=0；`s1-01..06` 全过；version/conflict fixture 拒绝；event 块除 `payloadState` 哨兵外无 payload/state/terminal 列，模拟 S2 完整行或缺失 event 行必须被 stage 校验拒绝；无新 endpoint/code/state |
| 完成信号 | 报告列出 12 message ID、11 eventType 名称（identity-only）、22 identity 行、action contract 映射、fixture 输出、A1/A0 drift=0、明确“无 runtime 且 event payload 语义未在 S1 冻结” |
| 停止条件 | 需要修改 A1/A0；需要新 action/reason/state/error；无法把 A1 字段映射到 A0 fingerprint；需要数值/policy；出现未解释的 A0/A1 语义矛盾（报告 blocker） |
| 未覆盖/残留下沉 | 11 event payload/envelope/state/terminal 语义与全部 S2 registries（S2）；recording/media/evidence（S3）；真实签名/nonce/时间窗（Q7/Q15/Q25）；真实 prompt/consent 文案（Q10）；org 成员角色矩阵（Q27）；runtime session/decision 并发 |

### 8.3 S2 — 呼叫执行、B-leg exactly-once、终态与结算

| 项 | 内容 |
| --- | --- |
| 目标 | 将 S1 identity-only event block 提升为完整冻结形态：event envelope、11 个 closed payload、leg/bridge/recording facts、heartbeat、terminal/UNKNOWN/reconciliation/OUTBOUND_NOT_STARTED（含 resolve 从未 commit 的 404 负例）、IVR-local B-leg/outbox、task/reservation 终态映射与 S2 fixtures |
| 允许文件 | `docs/16-production-ivr-protocol-spec.md`（修改）；`protocol/ivr/v1/ivr-protocol-v1.json`（修改）；`protocol/ivr/v1/fixtures/s2-*.json`（新建）；`protocol/ivr/validate.mjs`（修改）；`docs/gateflow/a2-production-ivr-protocol-spec-s2-implementation.md`（新建） |
| 前置 | S1 accepted；S1 模型/validator/标记块已存在；S1 event block 为 identity-only，S2 必须原地提升而非并行保留 partial 形态 |
| 变更 | 删除 `payloadState` 并将 11 行 event block 提升为 `eventType,envelopeLegId,payloadFields,normalProcessingOutcome` 完整冻结形态；`eventEnvelope`；11 closed payloads；`endReason` 9 值与 task/call 映射；`quarantineReasons` 10；B-leg phase/lineAttemptCount/恢复；event outbox；事件顺序/终态吸收；heartbeat；UNKNOWN/reconciliation；settlement 边界；resolve-never-committed 的 `OUTBOUND_NOT_STARTED`/404 负例；§6 状态机/非法路径与既有 invariant 的执行语义（不新增 invariant ID）；写入 `eventRegistry` digest |
| 状态转移 | call `ACTIVE→ENDED/FAILED/UNKNOWN`；`UNKNOWN→ENDED/FAILED`；leg forward-only；reservation HELD→SETTLED/RELEASED/RECONCILING；task CLAIMED→COMPLETED/FAILED |
| 错误处理 | event 200 receipt/QUARANTINED；404 CALL_UNKNOWN；409 EVENT_ID_CONFLICT；400 payload/枚举；unknown 不 redial/release；over-authorized 不 settle；resolve 从未 commit 的 `OUTBOUND_NOT_STARTED` → 404 `CALL_UNKNOWN`，不建占位/不 release/无 usage，outbox `PERMANENT_FAILURE`；event block/当前 stage 字段缺失或残留 S1 `payloadState` 失败 |
| 验证命令 | `cd api/openapi && npm ci && npm run check:s3`；`node protocol/ivr/validate.mjs --stage s2 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md`；`git diff --check -- docs/16-production-ivr-protocol-spec.md protocol/ivr`；`file -bi`；冲突标记/trailing whitespace/`git status --porcelain` scope |
| 期望断言 | `stage=s2 event_map_form=full-frozen fixtures=17 passed=17 negative=3 slice_fixtures=6+11`；`lineAttemptCount<=1`、`reservationTerminalCount<=1`、`usageFactCount<=1`；`s2-01..11` 全过；负例 `s2-08`/`s2-10`/`s2-11` 拒绝且 `s2-11` 断言 404 `CALL_UNKNOWN`/无占位/无 release/outbox permanent；event block 4 列完整、digest 存在；stage/drift counters=0 |
| 完成信号 | 报告列出 11 eventType 完整 4 列 + digest、payload 字段闭集、endReason/quarantine 映射、B-leg 恢复判定、never-committed 负例、fixture 输出、counts、无 runtime 声明 |
| 停止条件 | 需要新 endpoint/表/状态/code；需要 SIP/provider 字段；需要 timeout/interval/tolerance 数值；无法表达 UNKNOWN/reconciliation；需要修改 A0 terminal/settlement 事务 |
| 未覆盖/残留下沉 | 真实线路/CDR/桥接/限时（Q7/Q28/Telephony）；时钟偏差（Q25）；心跳 interval/保留（Q8/Q13）；真实 DB 并发/锁；SLO |

### 8.4 S3 — 录音媒体生命周期与端到端恢复

| 项 | 内容 |
| --- | --- |
| 目标 | 完成 declare/upload observe/complete/quarantine 协议、recording identity/expiry/storage unknown、post-terminal conflict fact、evidence 边界、版本/错误/identity 最终一致性核对（不新增/改写 message、identity、event registry 或 Problem code）与 S3/端到端 fixtures |
| 允许文件 | `docs/16-production-ivr-protocol-spec.md`（修改）；`protocol/ivr/v1/ivr-protocol-v1.json`（修改）；`protocol/ivr/v1/fixtures/s3-*.json`（新建）；`protocol/ivr/validate.mjs`（修改）；`docs/gateflow/a2-production-ivr-protocol-spec-s3-implementation.md`（新建） |
| 前置 | S1/S2 accepted；媒体 wire/state 模型基座；S2 event registry 已冻结，S3 不得修改（digest 必须相等） |
| 变更 | declaration/complete 字段映射；upload expiry/credential 语义；observation/store unknown/expiry/conflict；recording 状态机；evidence handoff boundary；完整 recovery/restart/lost-response 端到端 fixture；最终 doc 兼容/错误/identity 一致性核对（event 块保持 S2 冻结形态，digest 重算相等） |
| 状态转移 | recording `DECLARED→UPLOADED→VALIDATED/QUARANTINED`；`DECLARED→QUARANTINED` on expiry/impossible；evidence downstream 不 wire |
| 错误处理 | 409 OBSERVATION_REQUIRED/IDENTITY_CONFLICT/UPLOAD_EXPIRED；503 STORAGE_UNKNOWN；无 client hash/version/bucket；complete 不固化 evidence |
| 验证命令 | `cd api/openapi && npm ci && npm run check:s3`；`node protocol/ivr/validate.mjs --stage s3 --model protocol/ivr/v1/ivr-protocol-v1.json --fixtures protocol/ivr/v1/fixtures --spec docs/16-production-ivr-protocol-spec.md --a1-bundle tmp/openapi-client/onecall-v1.bundle.json --a0 docs/14-production-data-model-and-flyway-schema-spec.md`；`git diff --check -- docs/16-production-ivr-protocol-spec.md protocol/ivr`；`file -bi`；冲突标记/trailing whitespace/`git status --porcelain` scope |
| 期望断言 | `stage=s3 event_map_form=full-frozen fixtures=25 passed=25 negative=3 slice_fixtures=6+11+8`；`s3-01..08` 全过；recording/media counts ≤1；post-terminal conflict fact=1；event registry digest 与 S2 相同；stage/drift counters=0；两次运行输出 hash 一致 |
| 完成信号 | 报告列出 recording message 映射、媒体状态、fixture 输出、最终 drift/counts、event registry 未变（digest 相同）、evidence 边界与 deferred owners |
| 停止条件 | 需要新增 upload/evidence endpoint；需要 COS/provider 具体值；需要修改 A1 recording schema；需要数值 expiry/format/tolerance；需要 evidence runtime |
| 未覆盖/残留下沉 | 真实 COS 版本/上传事件/恶意扫描（Q7/Q11/Infrastructure）；upload expiry 值（Q8）；evidence creation source identity（Evidence/Backend）；多录音/多原始 artifact 策略 |

## 9. 文档决策、风险与残留下沉

### 9.1 文档决策

- 新增且仅新增 `docs/16-production-ivr-protocol-spec.md` 作为人类协议规范；不复制 A1 schema，不新增 README/第二协议文档。
- `protocol/ivr/v1/ivr-protocol-v1.json` 是唯一机器 wire 模型；fixtures 与 validator 围绕它。
- Gateflow 每个 slice 单独报告；不把实现报告混入 canonical 规范。
- 不修改 `docs/12`-`docs/15`、A0/A1 accepted artifacts、OpenAPI、prototype、screenshots。

### 9.2 风险/开放问题分类

| ID | 分类 | 内容 | Owner / Destination |
| --- | --- | --- | --- |
| `R-A2-A0-TASKID-TYPE` | 非阻塞表述不一致 | A0 §10.14 `task.taskId` 标 `uuid`；A0 §4.23 `text` 1-64；A1 string 1-64。采用 normative schema/A1，不修改 A0 | A0 schema/catalog-order；后续 A0 文档澄清 |
| `R-A2-DECLARE-REPLAY` | A1/A0 投影边界 | A1 declare 只有 201/DECLARED；A0 internal 可能已 UPLOADED。A2 只允许 `DECLARED` 状态 replay，上传后 re-declare 失败关闭 | A1/A2 owner；后续 API/evidence projection review |
| `R-A2-SESSION-ABANDONED` | 未使用状态 | A0 `inbound_session.ABANDONED` 无 v1 wire；v1 保持 RECEIVED/CONFIRMED/REJECTED | IVR/Backend；后续 product/session polish |
| `R-A2-HEARTBEAT-VOLUME` | 容量/保留 | 心跳作为 durable `call_event` 会增加行数；interval/retention 未定 | Telephony/IVR/Backend + Operations；Q8/Q13 |
| `R-A2-UPLOAD-OBSERVE` | 运行时集成 | `record_upload_observed` 的触发（对象存储事件/轮询）不在 A1/A2 wire 内 | Telephony/IVR + Evidence；Q7/Q8 |
| `R-A2-EVIDENCE-HANDOFF` | 下游 owner | evidence creation/solidification 的 source event identity 与触发在 A0 evidence owner，不在 A2 wire | Evidence/Backend；A0 §9.28/§10.24 |
| `R-A2-TASK-TERMINAL` | 运行时 | task `COMPLETED/FAILED` 映射使用既有条件更新；未在真实 DB 并发验证 | IVR/Backend；实现/集成测试 |
| `R-A2-RECONCILIATION-AUTHORITY` | 运行时/线路 | `CALL_TERMINAL_RECONCILED` 的权威性依赖 IVR 本地 CDR/线路记录 | Telephony/IVR；Q28 |
| `R-A2-CALLID-UNIQUENESS` | 运行时 | 跨节点唯一 `callId` 由 IVR gateway 保证；静态不可证明 | Telephony/IVR；Q28 |
| `R-A2-MULTI-RECORDING` | 范围 | A0 允许一 call 多 recording；v1 不额外限制；evidence 基线只取一个 ORIGINAL | Evidence/Backend；后续 evidence policy |
| `R-A2-NO-RETRY-CALLEE` | 产品范围 | v1 一 call 一个 B-leg；被叫重试需新 call/reservation，属产品变更 | Product/IVR；后续 work unit |
| `R-A2-PROMPT-DTMF` | 产品/Q10 | prompt 文案、DTMF 语法/等待行为未冻结；只传 `promptVersion`/confirmation content | Product/Legal；Q10 |
| `R-A2-SIGNATURE` | 安全/Q7/Q15/Q25 | 签名串、nonce、时间窗、时钟偏差未冻结 | Security/Backend；security design |

### 9.3 Deferred inputs（不发明）

| ID | 输入 | A2 只做什么 | Owner / Destination |
| --- | --- | --- | --- |
| Q7 | SIP/线路/接入号/录音格式/缓冲/时钟 | 服务端派生 access/node；只声明/传递媒体格式；不定义 provider 字段/线路值 | Telephony/IVR；IVR protocol/line integration |
| Q8 | 心跳间隔、超时、重试退避、duration tolerance、SLO | 只定义语义与 fail-closed；不冻结数值；over-authorized 无 tolerance | IVR/Backend/QA；protocol/capacity |
| Q10 | 录音告知/同意文案、提示版本生命周期 | 只传 `promptVersion`/policy ref；不选文案 | Product/Legal；consent policy |
| Q18 | 30 秒 TTL 配置键/审批/调度 | 使用已冻结的 30 秒默认与 per-task freeze；不定义配置键 | Product/Backend；operations config |
| Q25 | `authorizationExpiresAt` 窗口/时钟偏差 | 复制 committed absolute time；cutoff 语义已定；不定义窗口/偏差 | IVR/Entitlement/Backend；IVR protocol |
| Q27 | 组织成员任务创建/领取角色矩阵 | 只保留 A0/A1 既有 lifecycle/access 检查；不定义角色矩阵 | Organization-access/Security；authorization design |
| Q13 | retention/outbox 保留 | 不定义心跳/事件保留期 | Operations/Backend；operations policy |
| Q24 | caller-only task association 风险 | 保留 no-latest-callee fallback；不声称可完全消除错配 | Product/Telephony/Backend；pre-launch risk |
| Q15/Q26/Q28 | event wire values、snapshot/wire mapping、B-leg local key/recovery | **A2 范围内闭环**：event types/payloads、protocolVersion 1.0、B-leg local key=`callId`/phases/recovery | IVR/Backend/Security；本 work unit |

## 10. 实施 Agent 完成报告模板

每个 slice 报告必须包含：

1. Task label、provider/model、gate、base commit、slice ID。
2. 精确变更文件列表（只允许 §8 中该 slice 的路径）。
3. 本 slice 的 message/event/identity/fixture ID 与累计计数，以及 event block 的 stage 形态（S1 identity-only / S2/S3 full-frozen + digest）。
4. 协议决策摘要：action contract、identity/replay、event/facts、B-leg recovery、terminal/settlement、media。
5. 执行过的命令、exit code、关键输出（A1 `check:s3`、`stage=sN` 计数、drift=0、fixture passed/failed、UTF-8/whitespace、scope）。
6. A1/A0 漂移证据：`a1_operations=49`、`ivr_operations=6`、`a0_tables=44`、`a1_drift=0`、`a0_*_drift=0`、`doc_*_missing/extra/duplicate/invalid=0`、`stage_field_missing=0`、`stage_field_premature=0`、`fixture_id_missing=0`、`fixture_id_extra=0`。
7. 负例证据：哪些 fixture 被拒绝、哪些副作用计数保持 0（第二 B-leg/reservation/settlement/event/media）。
8. 残留下沉：owner/destination；明确未实现项。
9. 明确限制：没有运行 backend/DB/Flyway/SIP/CDR/COS/Redis/evidence runtime。
10. 完成/阻塞声明；若阻塞，列出只读证据、未修改路径、需要谁决策。
11. 明确声明：只改允许路径；未 stage/commit/push/PR/merge；未进入下一 Gate。

S3 报告还必须给出：最终 12 messages、11 event types、22 identities、25 fixtures、event registry 与 S2 冻结 digest 相同、全部 drift/stage counters=0、两次运行确定性 hash、完整 terminal/recording/media A0/A1 映射、Q15/Q26/Q28 闭环声明、Q7/Q8/Q10/Q18/Q25/Q27 原样 deferred 声明。

## 11. 无过度设计与目标对齐

- **不新增 surface**：12 条协议消息全部落在 A1 的 6 个 IVR operation；心跳/reconciliation 使用既有 event ingestion。
- **不新增持久化契约**：所有状态/action/reason/表/列引用 A0 既有 named CHECK 与 catalog；没有新表、列、状态、事务。
- **不新增错误码**：所有失败使用 A1 既有 `Problem.code`；quarantine reason 只写入 `call_event.reason_code`，不是 Problem。
- **不发明数值**：只使用已批准的 30 秒 TTL 与 A1 字段边界；心跳/重试/窗口/tolerance/格式/容量全部 deferred。
- **不越界实现**：B-leg 是逻辑本地 operation，不触碰 SIP/媒体引擎；evidence 是 A0 downstream，不新增 endpoint/服务。
- **不重复文档**：一份人类规范 + 一份机器模型 + fixtures，不复制 A1 schema，不新增 README。
- **三片最小**：S1 授权真相、S2 执行真相、S3 媒体真相；每片有独立失败模式、验证信号和 stop condition。
- **每项设计可回溯**：action contract ← 绑定目标/A1；B-leg ← Q28/`docs/14` §10.14；heartbeat/UNKNOWN/reconciliation ← 绑定目标/A0 状态机；media ← A0/A1 既有 recording 契约。
- **不把风险偷藏为设计**：A0 表述不一致、A1/A0 投影边界、runtime/容量/evidence identity 全部显式列为 residual，不吸收进 A2。

## 12. 计划完成定义

- 本文件存在且 UTF-8；无冲突标记、无 trailing whitespace；只创建本文件。
- 包含 §1-§12 的全部必需内容：目标/成功信号/非目标/边界、第一性原理与精确证据、完整文件清单与 canonical 决策、精确协议/状态机/恢复/版本/错误设计、identity/ownership 矩阵、合法性/重复/冲突/乱序/失败关闭表、≤3 切片及每片完整字段、确定性校验/fixtures、文档/风险/残留下沉、完成报告、无过度设计说明。
- 三个切片的行为边界清晰且每片可独立验证；不按文件/消息/owner 机械拆分。
- 无 blocking question。若实现阶段发现无法用既有 A1/A0 表达的语义矛盾，必须停止并作为 blocker 报告，不得静默修改 accepted contract。
- 本 Gate 完成后停止；不实施、不进入 plan review 之外的 Gate、不 stage/commit/push/PR。
