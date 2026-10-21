# Account identity and workspaces - local delivery / draft PR readiness

## Scope / gate

- Work unit: `account-identity-workspaces`。
- Branch: `feat/operations-admin-prototype`；local baseline: `e01ed1c`。
- Gate: `ready-to-open-draft-PR` 的本地检查完成；next entry point: `push`。
- External blocker: `git remote -v` 无输出，无法确定远端仓库、默认分支、upstream 或创建真实 PR。
- 没有执行 push、创建 PR、PR review、draft-PR-pass 或 final closeout pass；不把本地检查代替这些 gate。

## Delivered behavior

- 个人与企业入口共用自然人账户；仅已登记且启用的微信/手机号身份可登录，业务绑定手机号不自动成为登录身份。
- 企业空间与只读角色来自有效成员关系；用不同演示人登录验证管理员、成员、证据管理员和审计员，移除任意切换角色入口。
- Web 设置与小程序账号页分别展示账户、登录身份、工作空间和业务号码；同一号码兼具两种用途时无矛盾标识。
- 换人、退出与切换工作空间清理 UI 草稿；导出任务按账户/空间隔离；未配置业务样例的空间保持空态。
- 账号目录与运营员工账号、业务快照分离；IVR 未实名试用、录音事实、认领、授权、支付与权益保持。

## Accepted commits / findings

- Plan: `7d1ac9b`。
- S1: `1c5e4ea`。
- S2: `b167e1b`；S2-01/S2-02 accepted / 已修复，复审通过。
- Aggregate deepreview: `5388370`；AG-01 accepted / 已修复，复审通过。
- Baseline 后四个提交均属于当前 work unit；此工作分支还包含 baseline 之前的既有原型工作。远端可用后，须核对真正 PR base 和完整 PR diff，不假设本地 baseline 就是远端默认分支。

## Validation / docs

- 共享业务与 runner 单测：31/31 PASS。
- Web、小程序（62 页）、Operations 与跨端 QA：PASS；最后文案修复后再次运行完整小程序 QA，PASS。
- 共享生成一致性与 diff whitespace checks：PASS。
- 新账号页、Web 设置和云拓空态截图已目视检查；历史 QA 截图刷新保留在工作区，未一并 stage 或删除。
- 账号生产建表建议与当前原型区别：`docs/11-account-identity-and-workspaces.md`。
- 审查证据：`docs/reviews/code-review-20261021-105625.md`；S2 复查：`docs/reviews/code-review-20261021-103742.md`。
- UI 入口：`web-prototype/preview.html`；`prototype/preview.html`；`operations-prototype/preview.html`。
- Web 演示登录：李晓 `13800006208`，验证码 `482916`；“设置”查看身份说明，“工作空间”切换组织。

## Residual risks / next entry point

- R1 真实认证与会话安全 → `assigned to later work unit`: production identity/auth。
- R2 组织号码所有权 schema → `assigned to later work unit`: production subject ownership/schema。
- R3 服务端租户与资源鉴权 → `assigned to later work unit`: production tenant authorization。
- 本地原型已交付，不代表已部署真实认证、数据库或后端权限服务。
- Remote 可用后的入口为 `push → create draft PR → PR review`，继续依 Gateflow 执行；当前无 PR URL，非 issue work unit，无 issue 关联或对外消息。
