# ONECALL 原型共享业务规则

本目录提供三个本地原型在本轮跨端同步中使用的单一演示规则源。它只负责套餐样例、状态标签、线下客服配置及少量可确定验证的状态转换，不是后端、规则引擎或跨页面实时同步服务。

## 文件

| 文件 | 用途 |
| --- | --- |
| `onecall-business.js` | 无依赖 UMD 模块；Node 使用 `require`，浏览器使用 `window.OnecallBusiness` |
| `build-prototypes.js` | 将同一模块写入三个 HTML 源文件的受控标记区 |
| `qa/business.test.js` | 套餐、IVR、试用、认领、订单、快照与生成一致性测试 |
| `qa/runner-exit.test.js` | 验证 Web QA runner 不会吞掉 Node 失败码 |
| `qa/cross-channel.js` | Playwright/Edge 通过三个本地 preview 与校验快照验证目录、IVR、认领、支付及导入边界 |

## 生成

三个 HTML 源文件各保留一组标记：

```html
<!-- ONECALL_BUSINESS:START -->
<!-- generated content -->
<!-- ONECALL_BUSINESS:END -->
```

不要手工编辑标记区。更新 `onecall-business.js` 后执行：

```powershell
node .\shared\build-prototypes.js
node .\shared\build-prototypes.js --check
```

三个原型的 `qa/run.ps1` 会先运行生成器，再按各自原有方式生成 `preview.html`。内联方式用于保持 `file://` 和小程序 `srcdoc` 沙箱可用；模块哈希用于检测漂移，不代表代码签名或生产安全能力。

## 接口与边界

模块导出 `schemaVersion`、`labels`、`support`、`demoCatalog` 以及套餐可见性、内部开通、IVR 会话确认、一次性试用、认领、录音计量、订单、支付结果、权益发放和评审快照函数。`visibleOffers` 为每个可见版本附加共享契约生成的 `versionToken`；调用方也可通过 `offerVersionToken` 为同一版本生成与对象属性顺序无关的 token。`createOrder` 必须提交当前 `ACTIVE` 版本的 token，缺失、空值、目录内容变化、草稿或下架状态都会原子拒绝。历史订单及其事件结果仍校验冻结的下单时快照，不会因当前目录下架或发布新版本而失效。修改类函数接收显式业务状态；拒绝时不部分写入。

事件幂等记录按 `ACTIVATION`、`CLAIM`、`IVR_CONFIRM`、`EVIDENCE`、`ORDER`、`PAYMENT`、`ENTITLEMENT` 分域。同一字符串可在不同动作域独立使用；同域同键只允许完全相同的规范化输入重放，并返回首次处理时保存的结果快照。同域同键但输入不同会原子拒绝。创建类事件身份与副作用对象保持域内一一对应：`claims[].requestId`、`orders[].idempotencyKey` 分别唯一，非 IVR 与 IVR 激活的 `eventId` 分别在 `ACTIVATION`、`IVR_CONFIRM` 域内唯一；不保存事件字段的证据和权益对象也不能被同域多个创建记录重复声明。`PAYMENT` 是明确例外，多个不同回调事件可按状态机推进同一订单。导入与重放共用同一套域级校验：结果必须符合该动作的结构，输入与结果的稳定主体/会话/订单身份必须一致，冻结快照和业务引用必须存在；支付状态、试用已用秒数等首次历史值可以早于当前状态。事件键使用安全编码，`__proto__`、`constructor`、`prototype` 等字符串不会改变对象原型。

每次事件查键和快照导入共用完整的域级校验，既检查该域每条记录的结构与引用，也检查创建结果身份唯一性；不能只凭命中的单条记录合法就返回重放成功。两个证据或权益事件声明同一结果对象时，任一旧键和新键均原子拒绝。有效状态下，不同事件创建不同证据仍合法；权益重复发放使用新键仍返回既有结果，不添加第二条创建记录。

`consumeTrial` 在读取幂等结果前先验证唯一 `CONFIRMED` 会话、相等的两个冻结主体字段、唯一主体及所有证据引用。只有结构上下文合法后才能重放；额度耗尽和时间到期属于首次成功后的可变状态，不会阻止旧事件返回首次结果。

`snapshot` / `loadReviewSnapshot` 只允许 schemaVersion 1 的业务字段。员工账号、登录凭据、客户认证会话及 UI 状态不能进入快照。快照内的 `sequence` 只是下一次本地分配的提示值；各 ID 分配器会跳过对应集合中已占用的候选值，每次成功替换状态前还会完整复验。评审快照用于后续本地跨端 fixture 传递，不是生产 API、持久化或真实联调。

当前目录交叉字段也属于共享不变量：企业版本必须是 `ENTERPRISE + QUOTE + billingCycle=QUOTE + priceCents=null`，只能咨询；个人版本不能是 `QUOTE`，也不能包含企业席位或固话权益。订单导入和权益发放会再次拒绝非个人交易快照。

S1 规则底座、S2 运营后台及 S3 小程序/Web/IVR 展示已接入同一演示契约。各端保留本地内存状态，员工、凭据和登录会话独立于业务快照。`__onecallOps`、`__onecallMini`、`__onecallWeb` 的业务快照 hooks 只用于显式 fixture 交接；不代表网络同步、真实身份验证、IVR 或支付联调。发布真实客户端须关闭这些 hooks。

先运行三个 `qa/run.ps1` 生成最新 preview，再执行 `node shared/qa/cross-channel.js`。S3 已通过运营发布目录/试用 → 客户展示 → 动态 IVR 取证/认领 → WEB、MINIAPP 订单/支付/独立发放的往返；同时覆盖冻结旧 token、下架/移除渠道、事件重放、五类主体引用篡改及逐类禁入顶层字段的原子拒绝。完整命令结果见 S3 implementation artifact。所有规则参数仍为 DEMO_NOT_APPROVED，客服联系方式保持未配置。

