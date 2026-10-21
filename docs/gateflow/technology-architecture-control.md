# Technology stack and system architecture — control

- Work unit: `technology-stack-system-architecture`; baseline: `d0ca57a`; branch: `feat/operations-admin-prototype`.
- Binding goal: 输出一份可供研发评审的生产目标技术栈与系统架构设计，覆盖小程序、客户 Web、接收方分享页、运营后台、自建 IVR、Java 服务端、数据与基础设施。
- Motivation: `docs/03-technical-solution.md` 已包含领域与状态机，但仍混有被当前业务覆盖的运营商订购对象，并未收敛前后端、IVR、部署单元、网络边界和 5 人团队首发形态；现有静态原型也不能代表生产技术栈。
- Success signal: 新文档能够让研发继续拆分前端、后端、IVR、数据和运维任务；明确确定项、推荐项与待决策项；与账号、套餐、免实名 IVR 试用、录音分享和线下客服边界一致；文档导航可达。
- Scope: 新增 `docs/12-technology-stack-and-system-architecture.md`，更新 `docs/README.md` 导航及必要的架构指向；仅文档变更。
- Non-goals: 字段级 DDL、完整 OpenAPI、生产容量报价、具体云资源采购与规格锁定、应用代码、基础设施脚本、原型页面改造。
- Architecture boundary: 首发采用模块化单体 + 独立异步 Worker + 独立 IVR 媒体节点；不提前拆微服务，不把 Kubernetes 设为上线前提。
- Direct evidence: 当前产品基线已锁定原生微信小程序、客户 Web、自建 IVR、Spring Boot、PostgreSQL、Redis、对象存储和消息队列；账号域、共享业务规则及精简后台边界分别见 09—11 文档。
- Overdesign guard: 只给出足以指导首发实现和后续拆分的模块、接口、部署与质量边界；扩容阈值、供应商型号和详细表结构保留为后续专项设计。
- Blocking questions: none; user confirmed the stated scope on 2026-10-21.

## Confirmed deployment decision

- 2026-10-21: 本期部署平台确定为腾讯云中国大陆地域；业务服务与数据按同地域、私有 VPC 优先部署，录音与证据对象采用腾讯云 COS 私有存储桶。
- Still to configure: exact region and availability-zone layout, compute/database/cache/message product sizing, COS storage class and retention, WORM/object-lock and KMS validation, disaster-recovery objectives, and cost estimate.

## Gate state

- Plan: `docs/gateflow/technology-architecture-plan.md`.
- Plan review: `docs/reviews/plan-review-20261021-152509.md`; `pass-with-risks`, no findings or fix required.
- Accepted plan commit: `44a5642`.
- S1 implementation: `docs/gateflow/technology-architecture-s1-implementation.md`; architecture document and navigation complete, structural validation passed.
- S1 code review: `docs/reviews/code-review-20261021-153413.md`; re-review `pass-with-risks`, all three accepted findings `已修复`: atomic IVR reservation, Outbox-only publication and preservation of the last admitted share session.
- S1 fix: `docs/gateflow/technology-architecture-s1-fix.md`.
- S1 accepted slice commit: `d1a8c27`.
- Aggregate deepreview: `docs/reviews/code-review-20261021-155440.md`; re-review `pass-with-risks`, A-01 `accepted` / `已修复`.
- Aggregate fix: `docs/gateflow/technology-architecture-aggregate-fix.md`; IVR current-state/validity guards and late usage settlement verified.
- Aggregate validation: local links, 18 sections, two inspected Mermaid diagrams, critical business invariants and aggregate whitespace checks passed; no runtime tests required for this documentation-only change.
- Accepted deepreview commit: `9636a76`.
- Local readiness: `docs/gateflow/technology-architecture-local-readiness.md`; approved document deliverable complete locally, intended commits only since `d0ca57a`, unrelated QA PNGs preserved.
- External state: `git remote -v` is empty; push/draft PR cannot proceed without an intended remote. No PR or final-closeout-pass claimed.
- Current gate / next entry point: `push` (blocked: no configured Git remote).
- R1: FreeSWITCH/SIP 接入、线路容量和运营商机房网络条件仍需线路供应商与运维验证。Assigned to IVR integration work unit, owner: telephony/backend.
- R2: 腾讯云/COS 已选定；对象存储 WORM、KMS、具体境内地域/规格、灾备策略及公证/AI供应商仍需采购和安全评审定型。Assigned to infrastructure/security work unit.
- R3: SLO、RPO/RTO 和容量数字缺少真实业务量输入。Assigned to capacity and production-readiness work unit.

## Tencent Cloud decision follow-up

- Work unit: `tencent-cloud-deployment-decision`; selected after the architecture document's initial local readiness record.
- Plan: `docs/gateflow/tencent-cloud-deployment-plan.md`; plan review: `docs/reviews/plan-review-20261021-230717.md`, `pass-with-risks`, no findings requiring fixes.
- S1 implementation: `docs/gateflow/tencent-cloud-deployment-implementation.md`; code review: `docs/reviews/code-review-20261021-231012.md`, no material findings.
- Scope: documentation only. Tencent Cloud mainland region and COS private bucket are selected for this release; exact region, products/specifications, object-retention/security controls, disaster recovery and cost remain open.
- Validation: focused architecture/control consistency scan and `git diff --check` passed; no runtime tests apply.
- Current gate / next entry point: accepted S1 slice commit, followed by aggregate deepreview. Existing prototype QA image changes remain outside this work unit.
