# IVR 呼叫任务系统设计工作单元

## Goal confirmation / scope contract

- 用户已连续确认：IVR 自研并具备外呼、桥接、录音及限时挂断；小程序提交任务后拉起电话拨入；单主叫只保留一条有效待拨任务；待拨有效期默认 30 秒且可配置；IVR 独立接口快速判断业务状态、可用时长和待拨任务。本次指令为“将本次商讨的 IVR 方案落实到系统设计”。这些会话决策构成本轮确认依据，无须重复询问。
- 补充确认：现有自研声讯平台能够向业务系统上传录音、通话记录和时长；本轮定义对接协议，不改造或替换其内部媒体引擎。
- 目标：形成能够指导后续前后端与自研 IVR 对接的专项设计，并同步现有系统架构、业务规则、账号边界和验收入口。
- 成功信号：文档明确数据/语音两条链路、任务状态、30 秒边界、原子领取、返回动作、权益预占、真实通话事件、失败恢复和跨端展示；读者不会误以为本轮已经实现生产服务。
- 非目标：不实现生产 API、数据库迁移、IVR 引擎或交互原型；不引入多任务排队、自动连续拨号、微服务和在线售后流程。
- Scope ownership：仅修改 `docs/` 中下列文档及本工作单元的审查/控制记录；既有 PNG 改动与未跟踪 `ac-call/` 属用户工作，不读取或提交。

## Preflight / 直接证据

- Branch：`feat/operations-admin-prototype`，非保护主干。
- Base：`13f23ae246a45edfda661a54dab2d2949badfa45`。
- 仓库及 `docs/` 未发现适用 AGENTS.md / CLAUDE.md；当前无 Git remote。
- `12` §6 的 `ivr` 模块已有 IVR 会话所有权，§9.1 已有试用、预占、录音和幂等结算；缺少小程序待拨任务、B 路外呼和快速呼入决策。
- `11` §1/§4 已区分登录身份与业务号码，IVR 不要求平台注册实名；新任务必须延续该边界。
- `10` 明确原型为本地 fixture；本轮新增的生产设计不得标记为已在原型实现。
- `03/04/06/07` 保留历史方案，以顶部覆盖说明连接新基线，避免重写无关历史。

## Design decisions / goal alignment

| 决策 | 对应确认目标 / 必要正确性 |
| --- | --- |
| 专项文档 `13-ivr-call-task-and-inbound-design.md` | 承载自研 IVR 对接、任务和接口设计 |
| 同一规范主叫全局只有一个 PENDING；历史保留 | 用户选择单待拨任务，避免不同账户/空间各建一条再靠号码猜归属 |
| 新任务冻结 30 秒配置、失效时间，重试不续期 | 用户指定可配置 TTL；边界使用数据库时间原子判断 |
| 更换与领取串行；已领取禁止替换 | 防止被叫被更改、重复执行和跨任务误匹配 |
| resolve 是短事务决策/领取，不做录音额度核销 | 用户要求快速响应，同时避免双领取；确认后独立预占才能外呼 |
| 复用现有自研 IVR；FreeSWITCH 保留为可选媒体实现 | 用户已有系统，不把推荐技术误当新增重建要求 |
| 主叫来源可信、工作空间明确、呼叫与录音各自状态 | 延续账号/权益/证据真源，防止借登录号码推断业务号码 |
| 保留被叫语音确认、直接 IVR 未实名试用 | 延续会话确认规则和既有产品基线 |

## S1：完整系统设计增量

- Allowed files：新增 `docs/13-ivr-call-task-and-inbound-design.md`；修改 `docs/README.md`、`docs/09-business-activation-and-lean-admin.md`、`docs/10-cross-channel-rules.md`、`docs/11-account-identity-and-workspaces.md`、`docs/12-technology-stack-and-system-architecture.md`；`docs/03-technical-solution.md`、`docs/04-acceptance-roadmap.md`、`docs/06-miniapp-ui-spec.md`、`docs/07-web-capability-hub-spec.md` 只增加当前方案的覆盖导航；另含本 work unit artifacts。
- Prerequisites：会话规则已确认，生产底座只作为设计输入。
- Allowed changes：13 中定义职责/时序、任务模型与锁、客户任务接口、独立 resolve、确认/预占/结束边界、错误动作、性能查询路径、配置和验收矩阵；其他文件同步引用和相应架构事实。
- 状态：任务 `PENDING → CLAIMED → COMPLETED/FAILED/CANCELLED`，未领取可 `EXPIRED/REPLACED/CANCELLED`；通话与录音处理状态分别归属 IVR/证据模块；终态不可回退，领取后不再受待拨 TTL 控制。
- 并发/恢复：同主叫锁串行化创建、更换、领取、直接呼入占用；callId 重试不领取新任务；不凭租约超时释放未知通话，不凭预查余额外呼。
- 客户权限：任务发起者和空间由服务端认证派生；实际主叫来自可信线路；待拨任务冻结归属，预占前重新核验访问权。
- Completion signal：全部设计规则能映射到验收场景，交叉链接可解析，git diff --check 通过，仅提交本轮 docs。
- Stop：只有无法在已确认范围内解决的业务矛盾才需用户选择。当前无此问题。

一个 slice 交付完整设计闭环，避免按文件/模块机械切片。当前目标是文档，SQL/JSON 为规范示意，不增加可执行工程、测试脚手架或真实线路能力验证。

## Validation / review

1. 按 planreview 审查范围、并发替换、TTL 边界、直接试用、权限和未知通话恢复；accepted findings 修复后复审。
2. 逐份审阅变更和 13 全文；按 deepreview 检查所有权、状态、重试、查询/授权分离及新旧文档一致性。
3. `git diff --check`；使用只读 PowerShell 检查本轮 Markdown 链接、代码围栏及编号；检索残留矛盾。
4. 文档无可执行行为变化，不运行会重写 PNG 的原型 QA，不声明生产性能/并发/线路已经验证。
5. 创建范围受控的本地 checkpoint；无 remote 时记录 push/draft PR 链路受阻，不能声明 final closeout pass。

## Residual risks

- assigned to later work unit（后端/IVR/客户端）：真实 API、迁移、并发/重放/线路集成、原型交互和性能压测。
- assigned to later work unit（产品/安全/运营）：既有号码回收与历史证据访问策略、提示词定稿、线路资费与生产容量。
- existing external-state blocker（仓库所有者）：无 remote，后续 push/PR 需要配置实际远端；不阻止本轮系统设计交付。

## Gate state

- Goal confirmation：pass（依据用户本轮要求及前序明确决策）。
- Plan review：pass-with-risks，见 `../reviews/plan-review-20261021-171809.md`；无 accepted findings，因此 fix / re-review 为 no-change pass。
- Current gate / next entry point：accepted plan commit。
