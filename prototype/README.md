# 一通话原型目录

本目录是 ONECALL 微信小程序原型的唯一维护入口。后续页面、交互和文案调整以这里的文件为准。

## 文件结构

| 路径 | 用途 |
| --- | --- |
| `onecall-miniapp-pages.html` | 当前主原型，可交互的 63 页评审稿 |
| `preview.html` | 从主原型生成的独立浏览器预览，不作为源文件手工修改 |
| `archive/` | 已被主原型吸收的历史独立稿，仅供追溯 |
| `qa/qa.js` | 页面导航、关键流程和 393/320px 溢出检查 |
| `qa/screenshots/` | 最近一次自动检查生成的关键页面截图 |
| `qa/run.ps1` | 重新生成预览并执行原型检查 |

## 当前基线

- 版本日期：2026-10-25
- 页面数量：63
- 设计基准：393 x 852 pt，兼容 320px 窄屏
- 主色与风格：蓝灰、无渐变、轻描边、克制阴影
- 最近补充：`dialHandoff` 改为待拨任务页，新增 `callStatus`（呼叫状态）页；演示创建时冻结 30 秒的单槽待拨任务、系统拨号盘交接、IVR 原子领取、按冻结被叫自动外呼（电话中无需确认被叫）、振铃/接听/桥接/录音、上传/证据分离，以及无任务 DTMF 输入并确认被叫的路径。任务级证据状态只是诊断投影，权威证据仍来自业务 `evidence`；全部为本地评审 fixture。
- 账号入口：“我的 → 账号与登录身份”（`accountSecurity`），展示自然人账户、脱敏登录身份、成员工作空间和单列的业务号码。默认微信演示人与 Web 李晓为同一 `ACCOUNT-DEMO-001`；新增业务号码或 IVR 认领不会新增登录身份。企业空间本轮仅展示成员资格，操作请在 Web 评审。
- 首发范围：仅支持自研 IVR 能够从运营商线路传递结果取得唯一可信主叫的手机号和独立固话直线；企业总机、PBX、分机归属、统一外呼号拆分和坐席 PIN 暂不设计

## 维护规则

1. 只修改 `onecall-miniapp-pages.html`，不要直接修改 `preview.html`。
2. 新增或删除页面时，同步更新页面目录数量和 `qa/qa.js` 的覆盖范围。
3. 涉及实名、号码验证、内部业务授权、计费、存证或公证条件时，同步更新 `../docs/` 中对应需求和验收文档。
4. 每次修改后运行 `qa/run.ps1`，确认页面可导航、关键流程可操作且 393/320px 无横向溢出。
5. `archive/` 中的文件不再参与当前评审；需要恢复内容时先合并到主原型。

## 查看方式

运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\prototype\qa\run.ps1
```

检查通过后，浏览器打开 `prototype/preview.html`。

当前 runner 覆盖 63 页导航、账号 / 登录身份分离、普通开通/固话、证据/事情/AI/素材回归、动态试用、认领风险、冻结结算、UNKNOWN 原单查询、独立权益发放、待拨任务/无任务 DTMF/呼叫状态与线下客服检查；本轮执行状态与阻塞见 `docs/gateflow/ivr-three-surface-prototype-sync-implementation.md`。共享段来自 `shared/onecall-business.js`，runner 先运行 `node shared/build-prototypes.js`；禁止手改生成段。

`window.__onecallMini` 的 `businessSnapshot()`、`loadReviewSnapshot(data)`、`setReviewScenario(name)`、`navigate(page)` 和 `currentPage()` 仅供本地评审。导入只接受校验后的业务 fixture，不包含登录、员工、凭据或 widget 状态；不是实时同步、生产接口或实际号码验证。上线构建须关闭 hooks。

任务域另有独立 hooks：`callTaskSnapshot()`、`loadCallTaskSnapshot(candidate)` 与 `setCallDemoState(name)`（`waiting|claimed|ringing|recording|ended|evidence|failed-expired|failed-mismatch|failed-no-answer`）。任务快照与业务快照互不影响；`setCallDemoState` 先用共享校验快照校验候选，再替换本地任务 fixture。呼叫状态页的剩余时间在渲染时从冻结 `expiresAt` 计算，不运行实时计时器；“返回小程序后刷新”只重绘本地上游 fixture，不伪造推进。上线构建须关闭这些 hooks。

`accountContext()` 返回当前自然人账户、脱敏身份和成员空间供只读评审，不进入业务快照。建表建议与登录规则见 `docs/11-account-identity-and-workspaces.md`。

三端交接运行 `node shared/qa/cross-channel.js`。新截图使用 `alignment-*`，历史截图保留；实际运行证据与风险见 `docs/gateflow/cross-channel-alignment-s3-implementation.md`。本轮任务/呼叫截图包括 `alignment-call-task-393.png`、`alignment-call-status-{waiting,claimed,recording,evidence,failed}-393.png`、`alignment-landline-no-task-dtmf-393.png` 与 `alignment-call-status-320.png`；`alignment-dial-handoff-320.png`、`alignment-landline-call-320.png` 随文案重新生成。

S3 修复后的普通开通以验证号码解析当前账户主体，检查共享开通结果后才显示成功，并将同一主体延续到试用和号码管理。微信演示号码为 `138 **** 6208`；短信演示只接受 `18600004471` 和验证码 `628104`，不执行真实短信验证。未验证、未实名、停用或归属冲突会留在表单，不写入业务状态。订单/权益计数按当前账户聚合，包含本账户不同号码，排除其他账户。发放周期从共享商品字段读取；修复与新增回归证据见 `docs/gateflow/cross-channel-alignment-s3-fix.md`，不替代后续复审或验收。

独立固话开通同样提交共享契约：主叫挑战成功后创建或复用 `MINI-LANDLINE-01088886688`，以 `CALLER_CHALLENGE` 关联当前账户，记录 MINIAPP 内部开通并按正常首次试用规则发放。成功页只在该主体已验证、已关联且为 `ACTIVE` 时显示，并明确不据此证明登记名义或产权；同一主体继续进入试用与号码管理。重复提交返回既有结果，不新增开通或试用。认领流程在重放和同账户重复判断前检查 `CONFLICT / RECYCLED`，原请求号与新请求号都不能绕过风险或看到历史。
