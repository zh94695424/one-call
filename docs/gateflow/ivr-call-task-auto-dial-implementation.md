# IVR 呼入自动外呼设计修订实施记录

## Gate / scope

- Gate：implementation，slice `S1 — Documentation correction`。
- Scope contract：`ivr-call-task-auto-dial-plan.md`。
- Accepted plan commit：`bdaccae`。
- Included：13 专项设计，12 架构摘要，09/10 业务摘要，03 历史方案标识，04 验收入口，README 导航，以及原 IVR 计划/控制/S1 记录的历史修订说明。
- Excluded：生产 API、数据库迁移、自研 IVR 媒体运行时代码、小程序/Web/运营后台原型、QA 截图和 `ac-call/`。

## Implemented decisions

- 符合条件的首次 IVR 呼入由 `/api/v1/ivr/inbound/resolve` 在同一数据库事务中领取 `PENDING` 任务、绑定 `callId`、创建录音权益预占并冻结 `DIAL_TASK_CALLEE` 决策；事务失败时领取与预占一起回滚，不能返回外呼授权。
- IVR 以冻结被叫号码自动创建 B 路外呼；被叫接通后桥接 A/B 并录音。IVR 是桥接/录音媒体节点，不额外拨打第三位参与人。
- IVR 以 `callId` 持久化 B 路操作身份，重复请求只恢复/查询原操作；未知通话结果需对账，不能因 API 超时直接重复拨号。
- 任务路径不要求主叫确认被叫号码。录音告知/适用的同意和未注册用户的开通确认保持独立；没有待拨任务时，直接 DTMF 输入被叫的流程仍可确认主叫输入的号码。
- 将迟到旧呼入可能领取替换后任务的 caller-only 关联歧义保留为上线风险；不声称被叫确认能防止该风险，也未引入任务码、队列或线路能力假设。
- 03 中的供应商呼叫流程保留作历史，明确当前生产契约以 13 为准。原计划、控制与 S1 实施记录的历史结论保留，添加了后续修订指引。

## Validation

- `git diff --check`: pass; no whitespace errors in changed tracked lines.
- Markdown integrity: pass for 11 scoped design/control/review documents; fenced code-block counts are balanced and all checked relative Markdown links resolve locally.
- Current-contract search: pass; no `CONFIRM_CALLEE` enum or task-path instruction requiring the caller to confirm the frozen callee remains in the current architecture/design/business/acceptance summaries. Remaining references explicitly describe no-confirm behavior, historical supersession, recording/activation confirmation, or direct no-task DTMF confirmation.
- 原型 QA 未运行：本 slice 没有原型行为变更，且运行 QA 会改写用户已有截图；生产 IVR 幂等和线路行为不能由静态原型证明。

## Docs decision

13 是任务状态、接口和验收的规范真源；12 描述系统架构；09/10/04/README 提供当前摘要和入口。03 的历史流程明确标为非当前契约。

## Residual risks

- assigned to later work unit（后端/IVR）：实现数据库唯一约束、事务、接口、IVR 持久幂等和未知 B 路结果对账；以真实线路验证不重复外呼。
- assigned to later work unit（产品/法务）：确定录音告知与适用同意的措辞/时点，不以被叫号码确认替代。
- requiring explicit launch decision（产品/线路/后端）：评估电话网络不携带 `taskId` 时迟到呼入领取替换任务的风险及可用线路关联标识。

## Completion

S1 文档实施完成；next gate：code review。
