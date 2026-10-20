# 三端业务同步与精简后台总控

## 当前入口

- work unit：`cross-channel-alignment`，类型：feature（本地产品原型同步）。
- design_doc：`docs/09-business-activation-and-lean-admin.md`。
- control_doc：`docs/gateflow/cross-channel-alignment-control.md`。
- 当前未完成 gate / next entry point：`accepted plan commit`。
- 状态：第二次计划复审 `pass-with-risks`；三项 accepted finding 均已修复，无新增实质性 finding。业务实现、验收和 final closeout 尚未完成。
- 派发协议：用户在默认 runner 重试仍不可用后，明确回复“继续”，授权改用当前会话内置子 Agent；这是用户明确覆盖，不是静默切换。后续本 work unit 统一使用内置子 Agent。
- 用户已确认业务顺序与文档路径，并于本轮明确回复“开始”；不再要求重复确认相同业务范围。

## 已确认目标、边界与成功信号

1. 先统一套餐与状态规则，再补后台账号和套餐原型，随后同步小程序/Web、细化 IVR 流程，最后做跨端验收。
2. 本轮仅修改业务原型、相关文档与自动化验收；不建设真实后端，不接入真实员工身份、支付、短信、IVR 线路或对象存储。
3. 运营后台进一步精简；多账号按后台员工独立账号理解，保留管理员/运营两类权限，操作审计关联具体员工；不扩展复杂组织、多租户和多级审批。
4. 套餐细化名称/编码、说明、适用范围、价格、实际售卖权益、有效期、购买渠道、展示及版本管理；历史订单、已购权益和已发试用不因配置调整被覆盖。正式价格、企业规格、续购叠加与自动续费等未定规则不得伪装成已批准的生产配置。
5. IVR 未完成平台注册实名认证的用户允许开通试用并产生录音证据；后续号码核验与认领不改变实名、不重复发试用、不改写证据产生时的主体；号码冲突及回收风险不能自动放开历史证据。
6. 小程序与 Web 均可完成内部业务开通、订购与支付流程演示；IVR 仅作购买引导。支付成功和权益到账保持独立。
7. 工单、退款、发票继续线下人工客服支持；本期只统一入口、联系方式和处理说明，不建设在线办理/审批。真实客服联系方式尚未提供，原型须显式标明待配置，不能编造可用联系方式。
8. 跨端验收指在同一规则、商品与业务场景下验证本地原型，不声称真实服务已联调或已上线。

本轮用户确认的范围扩展了 design_doc 中上一轮“不改客户小程序/Web”的实施边界；旧 work unit 与审查记录保持历史事实，不覆盖为本轮结果。

## 目标成立的直接证据

- `operations-prototype/onecall-operations-admin.html` 登录只选择角色，缺少独立员工账号；套餐编辑仅包含名称、价格、录音分钟与有效天数。
- 小程序与 Web 仍有运营商业务开通/回执的旧表达，缺少 IVR 回流认领和试用延续场景；Web 首次使用引导仍要求在小程序完成开通，并保留在线发票申请演示。
- `operations-prototype/README.md` 明确上一轮未修改客户小程序/Web，且现有后台为纯内存模拟。

## 用户确认的推进顺序

以下是范围与顺序记录，不是已批准的 code-generation-ready plan；具体工作单元与 slice 划分需由 plan gate 提出并经审查。

| 顺序 | 业务增量 | 当前状态 |
| --- | --- | --- |
| 1 | 套餐与跨端状态规则统一 | 计划已完成，等待计划审查 |
| 2 | 后台员工账号与套餐配置原型 | 未开始 |
| 3 | 小程序/Web 同步及 IVR 场景细化 | 未开始 |
| 4 | 跨端一致性与回归验收 | 未开始 |

## Preflight 与执行证据

- 日期：2026-10-20。
- 分支：`feat/operations-admin-prototype`，非保护主干。
- 起始提交：`f6ea510bb23c98fa04aee91acbdb3200dc3f70f4`。
- 起始 `git status --short` 无输出；本轮新增本 control_doc，不混入其它用户变更。
- `git remote -v` 无输出；远端 push/PR gates 仍需真实远端，不虚构完成。
- 已读取 phaseflow、gateflow、sub-agents 技能；phaseflow 要求具体 plan/implementation/review/fix 由 Agent 执行，默认协议为 sub-agents runner。
- Windows `Get-Command codex-agent-run,claude-agent-run` 未找到命令，`C:\Users\Administrator\.local\bin` 下对应 runner 不存在。
- WSL Ubuntu-22.04 当前用户 root；`command -v` / `type -a` 及 `/root/.local/bin` 对应路径均未找到两种 runner。
- 未启动 Agent 子进程，没有运行中任务；未自行替换派发协议，未修改业务文件，未执行实现或验收。

## Gate 记录

| Gate | Artifact / decision / validation |
| --- | --- |
| goal confirmation | 用户确认前述范围与顺序、允许新建 control_doc 并明确“开始”；accepted |
| plan | 内置 `alignment_plan` Agent 完成；`docs/gateflow/cross-channel-alignment-plan.md`；三行为切片，未运行实现/QA；controller accepted as review input |
| plan review | `docs/reviews/plan-review-20261020-110536.md`；fail；PR-001、PR-002 均 accepted |
| fix | `docs/gateflow/cross-channel-alignment-plan-fix.md`；PR-001/002 均记录为已修复；readback/rg/`git diff --check` 通过 |
| re-review | `docs/reviews/plan-review-20261020-113049.md`；fail；PR-001/002 已修复，新增 PR-003 accepted |
| fix（第 2 轮） | `docs/gateflow/cross-channel-alignment-plan-fix.md` 已更新；PR-003 记录为已修复；readback/rg/`git diff --check` 通过 |
| re-review（第 2 轮） | `docs/reviews/plan-review-20261020-113802.md`；pass-with-risks；PR-001/002/003 均已修复，无新增 finding |
| accepted plan commit | 待创建，仅包含本 work unit 的计划、总控、fix 和三份 plan review artifacts |
| 后续 gates | 均未进入 |

### Plan review finding 裁决

| Finding | 裁决 | 理由 / 要求 |
| --- | --- | --- |
| PR-001 | accepted | `consumeTrial` 必须由已确认 IVR 会话派生唯一主体；任何会话/主体错配、未解析会话均在写入前拒绝，grant/evidence/sequence/processedEvents 不变，并校验 evidence→session→subject 引用一致。 |
| PR-002 | accepted | Web runner 当前不能传播 Node QA 非零退出码；S1 首次改动三端生成路径，必须把 runner 修复和受控失败验证提前到 S1，避免假绿。 |
| PR-003 | accepted | 导入快照允许且必须包含业务 `IVRSession`；只排除员工记录/登录凭据/权限会话、客户认证会话和 UI/widget session 状态。有效动态 IVR 会话及其证据需原样往返，认证/权限字段按显式策略拒绝且不得导入。 |

Open Questions 裁决：`loadReviewSnapshot` 的 known IDs 明确为导入快照内部结构、枚举和引用完整性校验，不是固定 fixture ID allowlist；动态发布版本/主体/订单可导入。QA hooks 仅属于明确标注的本地 demo/review 原型；未来若复用为发布物，由后续生产构建 work unit 移除或关闭。

## 阻断与残余风险

| 分类 | 事项 | Owner / destination |
| --- | --- | --- |
| fixed in current slice | runner 派发阻断已由用户明确授权内置子 Agent 解除；未宣称外部 runner 恢复 | 总控 / 本轮统一使用内置派发协议 |
| assigned to later work unit | 真实 IVR、支付、员工鉴权、并发扣量和持久化事务 | 研发 / 后续真实服务实现 |
| assigned to later work unit | 号码回收、认领凭证与历史证据访问策略 | 产品与安全 / 后续真实认领服务 |
| requiring explicit user decision | 正式商品参数、续购规则、客服联系方式 | 产品与运营 / 原型可清晰标记待确认，不伪造上线参数 |
| existing external-state blocker | 无 Git remote，不能 push 或创建 PR | 仓库维护者 / 本地审查完成后的 push gate |

## 下一步

创建 accepted plan commit，随后进入 S1 implementation：统一可执行规则、受控内联打包、Web runner 失败传播与规则测试。不得将计划解释为规则、页面或跨端联调已完成。
