# ONECALL 原型共享业务规则

本目录提供三个本地原型在本轮跨端同步中使用的单一演示规则源。它只负责套餐样例、状态标签、线下客服配置及少量可确定验证的状态转换，不是后端、规则引擎或跨页面实时同步服务。

## 文件

| 文件 | 用途 |
| --- | --- |
| `onecall-business.js` | 无依赖 UMD 模块；Node 使用 `require`，浏览器使用 `window.OnecallBusiness` |
| `build-prototypes.js` | 将同一模块写入三个 HTML 源文件的受控标记区 |
| `qa/business.test.js` | 套餐、IVR、试用、认领、订单、快照与生成一致性测试 |
| `qa/call-task.test.js` | 独立待拨任务域：30 秒冻结、单非终态、替换/取消、原子领取、无任务 DTMF、事件顺序/幂等、上传/证据门控、快照篡改与双域隔离 |
| `qa/runner-exit.test.js` | 验证 Web QA runner 不会吞掉 Node 失败码 |
| `qa/cross-channel.js` | Playwright/Edge 通过三个本地 preview 与校验快照验证目录、IVR、认领、支付、任务/无任务诊断及导入边界 |

## 生成

三个 HTML 源文件各保留一组标记：

```html
<!-- ONECALL_BUSINESS:START -->
<!-- generated content -->
<!-- ONECALL_BUSINESS:END -->
```

不要手工编辑标记区。更新 `onecall-business.js` 后执行：

```powershell
node .\shared\build-prototypes.js
node .\shared\build-prototypes.js --check
```

三个原型的 `qa/run.ps1` 会先运行生成器，再按各自原有方式生成 `preview.html`。内联方式用于保持 `file://` 和小程序 `srcdoc` 沙箱可用；模块哈希用于检测漂移，不代表代码签名或生产安全能力。

## 接口与边界

模块导出 `schemaVersion`、`labels`、`support`、`demoCatalog` 以及套餐可见性、内部开通、开通状态迁移、IVR 会话确认、一次性试用、认领、录音计量、订单、支付结果、权益发放和评审快照函数。它还提供独立的客户账号演示契约：`resolveDemoLogin` 只把已启用的微信/手机号登录身份解析为自然人账户，`workspacesForAccount` 只从有效组织成员关系返回工作空间与只读角色。`demoAccountDirectory` 是不含原始 provider subject 的脱敏只读投影；`createAccountDirectoryFixture` 只供本地测试和原型评审，不是生产账号接口。企业没有共享登录凭据，业务绑定手机号/固话也不会自动进入登录身份目录。

`setActivationStatus` 是暂停/恢复内部服务的共享所有者：Mini/Web 客户端与运营后台分别使用明确的 `MINIAPP`、`WEB`、`OPERATIONS` 迁移来源；函数更新主体状态和修订号，追加可审计的 `activationTransitions` 记录，且不会重新发放试用。每条迁移冻结 `fromRevision` / `toRevision`，并要求 `toRevision = fromRevision + 1`。`visibleOffers` 为每个可见版本附加共享契约生成的 `versionToken`；调用方也可通过 `offerVersionToken` 为同一版本生成与对象属性顺序无关的 token。`createOrder` 必须提交当前 `ACTIVE` 版本的 token，缺失、空值、目录内容变化、草稿或下架状态都会原子拒绝。历史订单及其事件结果仍校验冻结的下单时快照，不会因当前目录下架或发布新版本而失效。修改类函数接收显式业务状态；拒绝时不部分写入。

事件幂等记录按 `ACTIVATION`、`ACTIVATION_STATUS`、`CLAIM`、`IVR_CONFIRM`、`EVIDENCE`、`ORDER`、`PAYMENT`、`ENTITLEMENT` 分域。同一字符串可在不同动作域独立使用；同域同键只允许完全相同的规范化输入重放，并返回首次处理时保存的结果快照。同域同键但输入不同会原子拒绝。创建类事件身份与副作用对象保持域内一一对应：`claims[].requestId`、`orders[].idempotencyKey` 分别唯一，非 IVR 与 IVR 激活的 `eventId` 分别在 `ACTIVATION`、`IVR_CONFIRM` 域内唯一；每条证据和每条开通状态迁移都必须分别由且仅由一条 `EVIDENCE`、`ACTIVATION_STATUS` 事件拥有，事件也只能拥有对应的一条规范结果。权益对象不能被同域多个创建记录重复声明。`PAYMENT` 是明确例外，多个不同回调事件可按状态机推进同一订单。导入与重放共用同一套域级校验：结果必须符合该动作的结构，输入与结果的稳定主体/会话/订单身份必须一致，冻结快照和业务引用必须存在；支付状态、试用已用秒数等首次历史值可以早于当前状态。事件键使用安全编码，`__proto__`、`constructor`、`prototype` 等字符串不会改变对象原型。

每次事件查键和快照导入共用完整的域级校验，既检查该域每条记录的结构与引用，也检查创建结果身份唯一性和反向覆盖；不能只凭命中的单条记录合法就返回重放成功。开通迁移还共用完整账本与最终状态校验：同主体数组顺序按修订严格递增，前后状态连续，时间不倒退，主体修订不早于账本尾部且当前状态等于尾部 `to`。乱序、重复或倒退修订、状态断链、尾状态不匹配及时间回退会在快照导入、旧键重放和新键写入前原子拒绝。缺少所属事件的证据/状态迁移、指向不存在结果的事件，或两个事件声明同一结果对象时也同样拒绝。有效状态下，不同事件创建不同证据仍合法；权益重复发放使用新键仍返回既有结果，不添加第二条创建记录。

`claimSubject` 在读取 `CLAIM` 重放记录或处理同账户重复认领前，先检查主体的 `CONFLICT` / `RECYCLED` 风险。风险主体对原请求号和新请求号都返回阻断结果，不能借历史成功事件或既有关联开放证据，且拒绝不改变状态。

`consumeTrial` 在读取幂等结果前先验证唯一 `CONFIRMED` 会话、相等的两个冻结主体字段、唯一主体及所有证据引用。只有结构上下文合法后才能重放；额度耗尽和时间到期属于首次成功后的可变状态，不会阻止旧事件返回首次结果。

`snapshot` / `loadReviewSnapshot` 只允许 schemaVersion 1 的业务字段。当前 schemaVersion 1 形状严格要求迁移修订锚点，不兼容修复过程中曾出现但未被接受的无锚点临时快照。员工账号、客户账号目录、登录凭据、客户认证会话及 UI 状态不能进入快照。快照内的 `sequence` 只是下一次本地分配的提示值；各 ID 分配器会跳过对应集合中已占用的候选值，每次成功替换状态前还会完整复验。评审快照用于后续本地跨端 fixture 传递，不是生产 API、持久化或真实联调。

当前目录交叉字段也属于共享不变量：企业版本必须是 `ENTERPRISE + QUOTE + billingCycle=QUOTE + priceCents=null`，只能咨询；个人版本不能是 `QUOTE`，也不能包含企业席位或固话权益。套餐类型还约束计费与发放矩阵：`SUBSCRIPTION` 只接受 `MONTHLY / QUARTERLY / ANNUAL` 且期数、发放间隔均为正整数；`ONE_TIME / ADD_ON` 必须是 `ONCE + 1 期 + 0 月间隔`；`QUOTE` 必须是 `QUOTE + 1 期 + 0 月间隔`。例如自定义 `3 months / 4 installments` 的订阅仍合法。订单导入和权益发放会再次拒绝不符合矩阵或非个人交易的快照。

S1 规则底座、S2 运营后台及 S3 小程序/Web/IVR 展示已接入同一演示契约。各端保留本地内存状态，员工、凭据和登录会话独立于业务快照。`__onecallOps`、`__onecallMini`、`__onecallWeb` 的业务快照 hooks 只用于显式 fixture 交接；不代表网络同步、真实身份验证、IVR 或支付联调。发布真实客户端须关闭这些 hooks。

## 呼叫任务演示域（独立本地 fixture）

`createCallTaskDemoState(now)` 返回 `{ schemaVersion: 1, ttlSeconds: 30, callerBindings, tasks, calls, sequence }`，拥有独立的严格校验器 `validateCallTask`。它不是业务快照的一部分，也没有进入 `BUSINESS_FIELDS` 或 `schemaVersion`；业务快照导入不会改变它，任务快照导入也不会改变业务字段。`callerBindings` 记录脱敏号码与账户/组织空间；`tasks` 记录被叫、所有者、创建时冻结的 `ttlSeconds=30` 与 `expiresAt`、领取 `callId` 和小程序拨号盘时间；`calls` 记录 IVR `callId`、决策、匹配、被叫确认方式、进度、录音秒数、上传/证据状态与只追加事件。

关键规则：

- 同一规范主叫全局最多一条非终态任务（`PENDING` / `CLAIMED`）；重复同键/同输入只返回原任务和原 `expiresAt`，不续期。显式替换只允许替换自己的 `PENDING` 任务；取消同样只允许自己的 `PENDING` 且未到期任务。
- `resolveIvrInbound` 在一次草稿变更内完成期限检查、任务 `CLAIMED`、`callId` 绑定与呼叫事实；同一 `callId` 返回首次决策，不同 `callId` 在主叫已有活动呼叫时返回 `REJECT_CALL_IN_PROGRESS`。有任务路径使用 `DIAL_TASK_CALLEE + calleeConfirmation=NONE`，不要求电话中确认被叫；只有无有效任务时才进入 `ENTER_CALLEE + DTMF_REQUIRED`，再由 `confirmDirectCallee` 转为 `DTMF_CONFIRMED`。无任务路径不读取历史任务被叫。
- `recordDialerLaunch` 只写任务上的 `dialerLaunchedAt`，不推进任务状态或呼叫进度；拨号盘打开不等于 IVR 呼入、领取、接通或录音。
- 呼叫事实只追加且必须符合顺序、时间单调与事件唯一性；`CALL_ENDED` 完成任务、`CALL_FAILED` 在领取后完成任务失败。`RECORDING_RECEIVED`、`EVIDENCE_PROCESSING`、`EVIDENCE_READY` 分别推进上传与证据，`EVIDENCE_READY` 要求上传已接收且经过处理；桥接前失败保持 0 秒、未上传、未创建证据。
- `labels.ownership` 与 `labels.callPath` 是唯一所有权/路径文案；`labels.taskStatus`、`ivrAction`、`taskMatch`、`callProgress`、`uploadStatus`、`evidenceStatus` 供三端一致渲染。呼叫级 `evidenceStatus` 只是诊断投影，权威证据列表仍是业务 `evidence`。
- 快照 hooks：`callTaskSnapshot()` / `loadCallTaskSnapshot(candidate)`；所有导入先完整校验再原子替换。Mini 另有 `setCallDemoState(name)`（先经 `callTaskSnapshot` 校验再替换），Web/Operations 使用只读诊断选择；Operations 的导入仍要求当前管理员与正常数据状态。`uniqueCallerBinding` 对缺失或歧义绑定返回 `null`，无任务呼叫的账户/空间归属据此失败关闭。
- `qa/call-task.test.js` 覆盖冻结/不续期、跨空间单槽、替换、边界领取、原子领取与幂等、无任务 DTMF、事件顺序/幂等/证据门控、失败零量、快照篡改和双域隔离。`qa/cross-channel.js` 另验证三端相同快照、渲染一致、外账户任务隔离与双向导入独立性。

这些函数只证明本地 fixture 语义，不证明数据库并发、真实线路、媒体引擎、对象存储、上传或云集成。

先运行三个 `qa/run.ps1` 生成最新 preview，再执行 `node shared/qa/cross-channel.js`。S3 已通过运营发布目录/试用 → 客户展示 → 动态 IVR 取证/认领 → WEB、MINIAPP 订单/支付/独立发放的往返；同时覆盖冻结旧 token、下架/移除渠道、事件重放、五类主体引用篡改及逐类禁入顶层字段的原子拒绝。完整命令结果见 S3 implementation artifact。所有规则参数仍为 DEMO_NOT_APPROVED，客服联系方式保持未配置。
