# Account identity and workspaces - Gateflow control

## Goal confirmation

- Work unit: `account-identity-workspaces`.
- Binding request: 补齐并优化客户账号原型，落实“自然人账户、登录身份、组织成员关系、业务号码、运营后台员工账号”分域。
- Motivation: 当前 Web 可用任意 11 位手机号加固定验证码登录，并可在已登录状态任意切换企业角色；小程序只有硬编码账户 ID。两者都不能准确表达已确认的账号规则。
- Success signals:
  1. 只有共享目录中已验证、启用的微信或手机号登录身份能解析到唯一自然人账户；未知手机号和错误验证码失败。
  2. 每个自然人始终拥有个人空间，企业空间及只读角色只来自启用的组织成员关系；企业没有共享登录账号。
  3. Web 以不同自然人账号演示管理员、普通成员、证据管理员、审计员权限，不能通过角色下拉框提权。
  4. 小程序与 Web 对李晓使用同一 `ACCOUNT-DEMO-001`；两端均明确业务手机号/固话不是登录身份。
  5. 客户账号目录不进入业务快照；运营后台 `employee_account` 原型保持独立，既有 IVR、认领、授权撤回、套餐和支付联调不回归。
- Non-goals: 真实微信 OAuth、短信发送、密码、token/session 持久化、风控、企业 SSO、邀请落库、账号合并/换绑、生产数据库迁移、生产级租户授权引擎、运营员工账号改造。
- Scope boundary: `shared/onecall-business.js` 只提供不可变演示目录和纯查询/解析函数；Web/小程序消费该契约并更新 QA；业务快照字段及运营后台登录实现不变。
- Direct evidence:
  - `web-prototype/evidence-hub.html` 的手机号登录仅校验格式和 `482916`，工作空间及 `#role-select` 均为静态可变项。
  - 同文件的陈雨、周宁号码目前也写入 `ACCOUNT-DEMO-001`。
  - `prototype/onecall-miniapp-pages.html` 仅硬编码 `ACCOUNT-DEMO-001`，没有登录身份/工作空间视图。
  - `shared/onecall-business.js` 的快照契约明确排除 customer auth/session/UI state，适合保持业务数据与身份目录分离。
- Overdesign guard: 本轮只做有限演示身份、查询函数和界面采用，不设计通用 IAM、RBAC DSL、数据库 DAO 或会话服务。
- Blocking open questions: none。用户已明确规则与实施顺序，本轮按原型边界实现。

## Gate state

- Branch: `feat/operations-admin-prototype`（非受保护主干）。
- Baseline: `e01ed1c`。
- Plan artifact: `docs/gateflow/account-identity-workspaces-plan.md`.
- Plan review: `docs/reviews/plan-review-20261021-093554.md`; conclusion `pass-with-risks`; PR-01 and PR-02 fixed in plan.
- Accepted plan commit: `7d1ac9b`.
- S1 implementation: `docs/gateflow/account-identity-workspaces-s1-implementation.md`; validation passed.
- S1 code review: `docs/reviews/code-review-20261021-094700.md`; `pass-with-risks`, no findings and no fix/re-review required.
- Accepted S1 commit: `1c5e4ea`.
- S2 implementation: `docs/gateflow/account-identity-workspaces-s2-implementation.md`.
- S2 code review/re-review: `docs/reviews/code-review-20261021-103742.md`; S2-01 / S2-02 accepted and `已修复`; final Web QA passed.
- S2 fix artifact: `docs/gateflow/account-identity-workspaces-s2-fix.md`.
- Accepted S2 commit: `b167e1b`.
- Aggregate deepreview / re-review: `docs/reviews/code-review-20261021-105625.md`; `pass-with-risks`; AG-01 accepted and `已修复`.
- Aggregate fix: `docs/gateflow/account-identity-workspaces-aggregate-fix.md`; complete Mini QA and generated check passed, screenshots reviewed.
- Current gate / next entry point: `accepted deepreview commit`。
- External state: repository has no configured remote; push and draft PR gates are expected to remain unavailable and must be reported, not fabricated.

## Residual risk register

- `R1`: 固定演示验证码和微信 subject 仅用于原型，不代表真实认证安全。Destination: production identity/auth work unit.
- `R2`: 现有业务快照的 `BusinessSubject.accountId` 仍是简化的自然人关联，组织号码的生产所有权模型未迁移。Destination: production subject ownership/schema work unit.
- `R3`: 静态企业证据数据仍按角色/姓名样例过滤，不是后端授权裁决。Destination: production tenant authorization work unit.

