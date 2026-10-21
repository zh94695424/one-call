# IVR 呼叫任务系统设计 S1 实施记录

## Gate / scope

- Gate：implementation，slice `S1`。
- Scope contract：`ivr-call-task-design-plan.md`。
- Base：accepted plan commit `331c6d2`。
- Included：新增专项设计 13；同步 03/04/06/07 顶部覆盖、09/10/11 业务与账号边界、12 架构、README 导航。
- Excluded：生产 API、数据库迁移、自研 IVR 内部媒体引擎、客户/运营原型、用户已有 PNG、未跟踪 `ac-call/`。

## Implemented decisions

- 明确复用现有自研声讯平台已有的接听、外呼、桥接、录音、限时、通话记录/时长和录音上传能力；FreeSWITCH 不再是强制技术前提。
- 将多任务队列收敛为同一规范主叫全局唯一的单槽待拨任务，保留历史终态；跨账户/空间不能替换或查看他人任务。
- 默认 30 秒有效、服务端配置与创建时冻结；数据库时间、懒过期和原子领取为安全边界，Scheduler 只收敛显示状态。
- 定义 `OutboundCallTask`、状态机、部分唯一约束、主叫锁、任务/通话/预占/录音/证据关联以及领取后崩溃对账。
- 定义客户创建/查询/取消接口、独立 `/api/v1/ivr/inbound/resolve`、开通确认、外呼授权、通话事件和录音登记/完成端点。
- 呼入决策一次返回主叫业务状态、可用秒数快照、任务匹配和动作；确认后重新校验并原子预占，余额快照不构成外呼授权。
- 明确 IVR 上传的通话、时间点、时长与录音是真实通信事实；小程序拨号回调不写接通/完成，通话终态不替代录音校验或证据固化。
- 记录电话网络不携带 `taskId` 的剩余边界：取消/替换后的迟到呼入由被叫播报和 DTMF 确认阻止错误外呼；严格一一绑定需后续一次性任务码或线路路由标识。

## Validation

- `git diff --check`：pass，仅有 Windows 工作区预期的 LF/CRLF warning，无 whitespace error。
- 本地 Markdown 链接检查：10 个变更设计文件中的 `.md` 链接全部存在。
- Markdown 围栏检查：12/13 的代码围栏成对。
- 定向检索：旧架构中的强制 FreeSWITCH 表述已从当前生产链路替换为自研 IVR；保留“不得按最新被叫回退”的显式保护。
- 原型 QA：未运行。本 slice 只修改设计文档；运行现有原型 QA 会改写用户已有截图，且不能验证尚未实现的生产协议。

## Docs decision

本 slice 本身就是设计文档交付；13 为专项真源，12 为总架构摘要，其他当前基线以覆盖导航与边界规则同步，历史内容不做无关重写。

## Residual risks

- assigned to later work unit（后端/IVR/客户端）：Schema/OpenAPI、接口实现、原型、真实并发/重放/性能与线路联调。
- assigned to later work unit（产品/安全/运营）：录音提示、线路资费/容量、号码回收、协议密钥和 SLO。
- existing external-state blocker（仓库所有者）：无 Git remote；push / draft PR 尚不可执行。

## Completion

S1 implementation complete；next gate：code review。
