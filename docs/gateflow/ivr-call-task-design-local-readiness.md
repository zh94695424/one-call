# IVR 呼叫任务系统设计 Local Delivery Readiness

## Delivered

- 主设计：`../13-ivr-call-task-and-inbound-design.md`。
- 总架构同步：`../12-technology-stack-and-system-architecture.md`。
- 当前业务/跨端/账号同步：09、10、11；README 已加入导航；03/04/06/07 已标注当前覆盖入口。
- 复用现有自研声讯平台的外呼、桥接、录音、限时、通话记录/时长和上传能力，不把平台内部重建纳入本轮。

## Accepted decisions

- 同一规范主叫采用单槽待拨任务，不使用多条 FIFO 队列；历史终态保留。
- 待拨窗口默认 30 秒且服务端可配置，创建时冻结；数据库时间和原子领取为安全边界。
- 独立 IVR 呼入决策一次返回主叫状态、可用时长快照、任务匹配和动作；余额快照不代替确认后的权益预占。
- IVR 拥有实际通话、桥接、录音、通话记录与权威时长；后台拥有任务、开通、权益、结算和证据状态。
- 事件元数据走业务 API，录音大文件使用业务系统签发的受控对象上传；结束、上传、校验和固证分别展示状态。
- 电话网络不携带 `taskId`；迟到呼入由被叫播报/按键确认防误拨，严格绑定留待一次性任务码或线路路由标识的后续工作单元。

## Verification

- Plan review：pass-with-risks；无 accepted findings。
- S1 code review：pass；无实质性问题。
- Aggregate deepreview / validation re-review：pass；三处尾空格已修复。
- `git diff --check 13f23ae..HEAD`：pass。
- HEAD 中 10 个业务/架构设计文件的本地 Markdown 链接全部可解析。
- 12/13 的 Markdown 代码围栏成对。
- 未运行原型 QA：本轮无原型行为变更，且 runner 会重写用户已有截图；静态原型也不能证明生产 IVR 协议。

## Commits

- `331c6d2`：accepted plan。
- `1fde04a`：accepted S1 docs slice。
- `2ef84dd`：aggregate deepreview artifact。
- `64d8fb0`：aggregate validation fix 与最终 re-review。

## Residual risk / next owners

- 后端/IVR/客户端：Schema、OpenAPI、签名与错误码、实现、原型及联调。
- 测试/运维：真实线路的 30 秒边界、双卡、并发、崩溃对账、性能和录音重传。
- 产品/安全/运营：提示词、资费/容量、号码回收、密钥轮换和生产 SLO。

## External blocker

`git remote -v` 为空，无法执行 Gateflow 的 push、draft PR 和 PR review 链。因此当前本地设计交付 ready，Gateflow 停在 `push`，不能声明 `draft-PR-pass` 或 `final closeout pass`。配置实际 Git remote 后可从 push 继续。
