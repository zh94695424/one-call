# lean-ivr-admin S1 实施记录

## Scope / decision

以用户确认的自建 IVR、三端开通订购、IVR 免实名试用产证和 5 人团队精简后台为范围。Plan：`docs/09-business-activation-and-lean-admin.md`；accepted plan commit：`9c9a290`；plan review：`docs/reviews/plan-review-20261020-094608.md`。

一个完整 slice S1 已实现：

- 当前后台由旧 55 路由/十角色改为 13 路由/管理员与运营人员两类权限。
- 用户搜索/来源/状态筛选，账户关联、实名、开通与权益分列；暂停/恢复需确认与理由。
- 自建 IVR 呼入、告知按键、未实名试用产证、重复领取拒绝、到期/耗尽/暂停拒绝、验证认领与绑定冲突、购买引导评审。
- 套餐新版本草稿、发布、上下架，试用规则版本与后续首次发放；订单快照/既有试用/证据主体保护。
- 订单按支付查询，支付与权益分列；无退款执行、开票、工单或人工标记支付功能。
- 追加操作记录、脱敏 fixture、正常/空/加载/不可用数据模式、登录与键盘弹窗行为。

Changed files：operations-prototype 的 HTML/生成 preview、README、QA/runner、6 张 lean 截图；docs README 与 01/02/03/04/06/07/08 顶部旧规则覆盖声明；本 work unit review/fix artifacts。`prototype/`、`web-prototype/` 无改动。旧 p0/p1/p2 截图保留为历史，旧功能可由 Git 恢复，不代表当前本期范围。

## Validation

- `powershell -ExecutionPolicy Bypass -File operations-prototype/qa/run.ps1`：通过。
- `node --check operations-prototype/qa/qa.js`：通过。
- `fc.exe /b operations-prototype\onecall-operations-admin.html operations-prototype\preview.html`：无差异。
- `git diff --check`：通过。
- 自动化覆盖 13 路由、未知对象/旧路由拒绝、低权限伪造写入、确认/取消与跨页面清理、未确认/无主叫、未实名产证、幂等试用、认领不改证据、冲突拒绝、过期/关闭/耗尽、套餐版本与规则快照。
- 1024/1280/1440/720 CSS 像素全部页面无页面横向溢出；人工检查概览、套餐、用户、IVR 详情、试用规则截图。

## Review / docs / residual risks

Code review：`docs/reviews/code-review-20261020-095520.md`；LR-01/02 均已修复并复核，详见 `lean-ivr-admin-fix.md`。

Docs decision：新增 09 为当前统一业务真源；旧稿保留历史但显式声明被覆盖；不宣称已同步修改客户原型或交付真实后台。

- assigned to later work unit：真实 IVR/服务端鉴权、事务幂等、限时挂断、支付；owner：研发。
- assigned to later work unit：号码回收风控与历史原文访问、客户端改版；owner：产品/安全/客户前端。
- assigned to later work unit：工单、退款、发票，当前线下客服处理；owner：运营/财务。
- existing external-state blocker：无 Git 远端；本地全部 gate 后停在 push，不冒称 PR/final closeout pass。

Completion：S1 pass-with-risks；无未修复 finding。下一 gate：accepted slice commit，之后 aggregate deepreview。
