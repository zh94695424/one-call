# A1 生产 OpenAPI 五信任面契约实施计划

> Work unit：`A1-production-openapi-surfaces-spec`
>
> Gate：`fix`（bounded plan-fix-3 / F-10..F-13 与已接受的澄清；本文件只形成实施计划，不代表任何运行时能力）
>
> 基线：`origin/main` 合并提交 `4d488ccfd28947b9e557b4f801bcaa990a18a5c8`（PR #1 / A0）
>
> 设计源：`docs/12-technology-stack-and-system-architecture.md` §8、§18，并按 §18 顺序承接 A0
>
> 输入：`docs/09`、`docs/10`、`docs/11`、`docs/13`、已接受的 `docs/14-production-data-model-and-flyway-schema-spec.md`
>
> 修正依据：`docs/reviews/plan-review-20261023-162543.md` 的 F-01..F-08 及 controller 裁决；逐项修改与复核见 `docs/gateflow/a1-production-openapi-surfaces-spec-plan-fix-1.md`。controller 接受的 F-09（全部 49 个 operation 路径与路由示例缺少 `/api/v1` 前缀）由 `docs/gateflow/a1-production-openapi-surfaces-spec-plan-fix-2.md` 修正。独立的第二次 re-review（`docs/reviews/plan-review-20261023-171627.md`）判定 F-01..F-09 保持修复、F-10..F-13 未修复；本稿由 `docs/gateflow/a1-production-openapi-surfaces-spec-plan-fix-3.md` 收口 F-10..F-13 与 controller 接受的全部澄清（servers 精确性、精确错误码、凭证标注、ORGANIZATION_MEMBER P1 门控、O20 publisher 运行时前置）。F-01..F-09 与 49 operation/路径/计数/slice 分配不变。
>
> 产出目标：一份 canonical OpenAPI 3.1 源文件、最小静态验证工具和一份跨面治理/A0 映射文档；不产生后端、迁移、UI、支付/身份供应商集成或运行时服务。

## 0. 结论摘要

1. 首发只建立五个信任面的机器可读契约：客户 `/api/v1/customer/**`、运营 `/api/v1/operations/**`、自研 IVR `/api/v1/ivr/**`、公开录音分享 `/api/v1/public/shares/**`、认证供应商回调 `/api/v1/callbacks/**`。
2. 采用**一份 canonical OpenAPI 3.1 源文件**，而不是五份独立 surface 文档。五个面通过稳定的 `tags`、`x-onecall-surface`、`x-onecall-audience`、`x-onecall-authorization`、`x-onecall-a0-models` 和 `x-onecall-error-codes` 扩展表达所有权、信任边界、A0 映射和错误集合。
3. 生成 TypeScript 只作为**兼容性检查与类型投影**，不生成可发布运行时客户端，也不声称五类身份共用同一会话或同一发布权限。
4. 49 个首发 operation 全部只来自 09/10/11/13 的已确认业务路径和 A0 已接受的状态/事务/表契约；不加入通用 CRUD、在线工单/退款/发票、支付发起、员工登录、通用目录 CRUD/审批引擎、`catalog_offer` 独立下架、运营录音播放或任何供应商专用 payload。目录/试用仅新增四个有界的 DRAFT 候选创建/替换 operation；线下客服联系方式与说明是 UI/部署配置，不是 API。
5. A1 只定义类型边界与稳定业务错误；Q1/Q6/Q7/Q8/Q10/Q12/Q14/Q15/Q18/Q19/Q22/Q25/Q26/Q27/Q28 等未决输入保持原 owner/destination，不用演示值或猜测值填充。

## 1. 目标、动机、成功信号、非目标与无漂移检查

### 1.1 目标

产出一份可被代码生成器、静态校验器和人类评审共同消费的 canonical OpenAPI 3.1 契约，并附最小治理文档，完整覆盖首发五个信任面：

- 客户：登录后账户/工作空间、业务号码、认领、内部开通/试用、待拨任务、目录/订单、证据元数据、分享管理。线下客服联系方式/说明属于 UI/部署配置，不是客户 API operation。
- 运营：脱敏用户/开通/IVR 会话/目录/试用策略/订单/审计查询，以及已批准的暂停/恢复、目录版本草稿创建/替换与发布/下架、试用策略草稿创建/替换与发布/停用。线下客服说明属于后台 UI/部署配置，不是运营 API operation。
- 自研 IVR：呼入决策、开通确认后的再决策、无任务直接授权、通话事件、录音登记与完成；只定义动作契约和事件封套，不设计后续详细 IVR 会话/重试/媒体协议。
- 公开分享：能力令牌页面元数据、显式准入、短时媒体授权；页面打开/预取不消耗准入。
- 回调：已认证支付供应商回调的统一接收与确认边界；不定义供应商报文结构。

### 1.2 动机（直接仓库证据）

| 证据 | 观察 | 对 A1 的含义 |
| --- | --- | --- |
| 仓库根目录 | 只有 `docs/`、`shared/` 和三个 HTML 原型目录；没有 `api/`、`package.json`、Maven/Spring 工程或 OpenAPI 源 | A1 是**新建契约源与静态验证工具**，不是在既有服务上补注解 |
| `docs/12` §8（273-286 行） | 明确五个 API 前缀、五类认证/限制、统一 `Idempotency-Key`/事件 ID、分页、错误码、金额分和 UTC 约定 | 五面边界、幂等、分页和错误语义是架构要求，不是 A1 自创 |
| `docs/12` §18（520-529 行） | 数据模型/迁移之后紧接着要求 OpenAPI 分面契约，再之后才是 IVR 协议、安全设计、容量和验收矩阵 | A1 必须先落地契约源，但不能替代后续 IVR/安全/容量工作 |
| `docs/12` §14.2（458 行） | 明确 HTML 原型和 `shared/onecall-business.js` 只是产品演示/QA 资产，不是生产 SDK、后端或数据库起点 | 不能从原型 JS 直接“反推”生产 API；只能把它当行为证据 |
| `docs/14` §3、§4、§7、§9、§10、§14 | 已接受 44 张表、状态机、事务序列、typed snapshot、幂等/Outbox 身份和 28 个开放输入 | A1 的 schema 必须直接映射这些名称/状态/事件，不能另起一套领域模型 |
| `docs/13` §6（120-251 行） | 已给出任务、IVR resolve、确认、直接授权、事件、录音六个端点的业务职责和幂等身份 | IVR 端点清单有直接业务锚点；详细 wire/media 协议仍属后续 |
| 当前工作树 | 75 张 QA 截图、`docs/reviews/plan-review-20261021-205118.md`、`.a1-plan-*` 等属于既有脏状态/控制文件 | A1 不得改动、暂存或提交这些路径 |

### 1.3 绑定成功信号

对每个批准的 operation，OpenAPI 和映射文档必须同时满足以下十项；任一项缺失即 slice 未完成：

1. **Owning surface**：`x-onecall-surface` 只能是 `customer`、`operations`、`ivr`、`public-share`、`provider-callback` 之一。
2. **Authentication/trust boundary**：operation 的 `security` 与该面身份一致；公开分享使用能力令牌路径参数，运营与客户使用不同 audience 的不透明会话，IVR/回调使用 mTLS + 请求签名边界。
3. **Server-derived authorization**：`x-onecall-authorization` 明确列出服务端从会话/工作空间/资源归属/员工角色/能力令牌派生的事实；请求 schema 不接受 `accountId`、`ownerOrganizationId`、`role`、`priceCents`、`paymentStatus`、`evidenceSolidified`、`ownerAccountId` 等可信事实。
4. **Request/response schemas**：所有 operation 的 request/response 都引用命名 component；列表使用命名 `*Page`；不把匿名 inline schema 作为主要生成目标。
5. **Stable status and RFC 9457**：每个 operation 至少一个 `default` 或显式 4xx/5xx 响应使用 `application/problem+json` 和 `Problem`；业务码进入唯一 registry，HTTP status 映射稳定。
6. **Write idempotency or stable event identity**：客户/运营写操作使用 `Idempotency-Key`、body 稳定 event ID 或 A0 已定义的 state-conditional/publication 身份（O18-O21 只用自然唯一键/`expectedRevision` + 内容投影，不使用平台 idempotency scope 或 `Idempotency-Key`）；IVR 使用 `callId + eventId`/`purpose`；回调使用供应商事件身份。
7. **Cursor rules**：每个资源列表都有 `cursor`/`limit` 和 `nextCursor`，稳定排序字段明确；无 offset，非法 cursor 失败关闭。
8. **A0 mapping**：每个 operation 的 `x-onecall-a0-models` 至少一个 A0 表名，`docs/15` 给出对应事务/状态机。每个条目必须是 `docs/14` 44 表 catalog 中的**精确 schema-qualified 名**（如 `entitlement_metering.usage_reservation`），不得使用短名、别名、通配符或错误 schema；静态校验按 catalog 精确集合匹配并拒绝违规 fixture。A1 不提供任何无 A0 表的 API 例外。
9. **Generated TypeScript compatibility**：固定版本生成器可生成包含全部 operationId 的 `paths`/`operations` 类型，且 `client-compat.ts` 在 strict `tsc` 下通过。
10. **No runtime claim**：README/文档/完成报告明确“仅有契约和静态验证，没有服务、数据库、鉴权、支付、IVR、COS 或 Redis 运行验证”。

### 1.4 非目标

- 不写后端/Maven/Spring/controller/service、jOOQ、Flyway SQL、数据库迁移或运行时 DTO 代码。
- 不改 UI/HTML 原型/截图，不新增 QA fixture，不把原型本地 hook 当生产接口。
- 不做支付、微信登录、手机号 OTP、实名、公证、AI 或供应商适配器集成；不定义供应商专用回调 payload、签名算法、证书或商户号。
- 不定义详细 IVR 会话/重试/媒体上传/心跳/DTMF 协议；只定义动作契约、事件封套、幂等键和稳定错误边界。
- 不选安全数值策略（TTL、重放窗口、KDF 参数、限流阈值、令牌长度上限以外的策略值）或组织角色矩阵。
- 不建在线工单/退款/发票/导出任务/运营播放流程或通用目录 CRUD；只允许 §5.2 的四个 DRAFT 候选创建/替换 operation（无审批引擎、无独立 `catalog_offer` 撤下、无已发布内容改写），不拆微服务。
- 线下客服联系方式/处理说明只作为 UI/部署配置，不建匿名 API、不建 A0 表、不设公开信任边界。
- 不选 A0 的 28 个开放输入值；不把 `DEMO_NOT_APPROVED` 价格、试用数值或原型样例变成契约常量。

### 1.5 无目标漂移检查

| 漂移诱因 | 本计划的拒绝方式 |
| --- | --- |
| 为了“生成客户端好用”补登录、刷新令牌、登出端点 | 客户/员工会话建立属于认证服务与 Q1/Q19；A1 只声明不透明 session scheme，不提供登录端点 |
| 为了“完整电商”补支付发起、退款、发票、自动续费 | 支付发起和供应商 payload 属 Q6/支付适配器；A1 只有订单创建/查询和已认证支付回调接收 |
| 为了“运营方便”补录音播放、任意号码查看、手动标记支付成功 | 运营默认只有脱敏业务元数据；A0/09 明确无播放权和人工支付按钮 |
| 为了“IVR 完整”补会话心跳、重试、媒体分片、签名串 | 这些属后续 IVR 协议/安全设计（Q7/Q8/Q15/Q26/Q28）；A1 只冻结跨端点共享的动作/幂等语义 |
| 为了“目录管理完整”补通用 CRUD、审批引擎、收益行改写或 `catalog_offer` 独立下架 | 只允许四个 DRAFT 候选创建/替换 operation；候选必须 `DRAFT`/never-published，收益行按 A0 append-only，发布/下架仍走 T-S2-14；独立 offer 撤下明确 deferred 给 Product/Operations/catalog-order（OQ-4） |
| 为了“错误可读”返回供应商错误、堆栈、SQL 或内部原因 | `Problem` 只允许稳定业务码、用户可理解文案、`traceId` 和字段错误；不复制供应商/内部细节；IVR 正常领域结果放 `200 IvrDecision`，不转为 Problem |
| 为了“分页一致”返回 offset/total 或数据库排序键 | 只返回 opaque cursor 和稳定排序语义；不暴露 SQL/索引/内部列 |
| 为了“支持入口方便”建匿名客服 API 或伪造 A0 表 | 线下客服联系方式/说明保持 UI/部署配置；无匿名 operation、无 A0 表、无公开信任边界 |

## 2. 第一性原理评估

### 2.1 五类身份、五条信任边界

契约设计的第一性原理不是“把页面翻译成接口”，而是把**谁可以要求什么事实、服务端必须重新验证什么**写成可生成、可拒绝的边界：

| 身份 | 可证明的事实 | 绝不能由请求体证明的事实 |
| --- | --- | --- |
| 客户会话 | 当前自然账户、可选企业工作空间、当前成员角色 | 业务号码归属、资源所有权、实名结果、试用资格、价格、支付成功 |
| 员工会话 | 员工身份与固定 `ADMIN`/`OPS` 角色（来源/MFA 未决） | 客户账户、工作空间、录音播放权、任意业务事实改写 |
| IVR 服务 | mTLS 服务身份、可信线路主叫、规范接入号、节点/协议版本 | 客户账户、价格、支付状态、证据固证状态、任务被领取后的重放授权 |
| 公开能力 | 高熵 capability token 的持有、对应分享的验证策略 | 分享创建者身份、证据内部 ID、对象键、未授权录音内容 |
| 供应商回调 | 供应商签名/证书、时间窗、事件身份 | 订单金额、支付成功、权益到账、任何客户/员工身份 |

由此得到三条不可妥协的契约规则：

1. **服务端派生**：客户、工作空间、角色、价格、金额、支付状态、试用结果、证据状态、录音归属、任务领取、分享准入都必须由服务端在请求时重新验证/冻结。
2. **稳定身份**：写操作的 replay 由 A0 已定义的事件/幂等身份决定，不由 HTTP 重试次数、客户端时间戳或随机 requestId 决定。
3. **失败关闭**：未知枚举、缺失策略、过期/撤销、无法解析的绑定、供应商格式不明、策略未批准时返回稳定错误，不回退到“最新任务/最新被叫/上一个成功结果/默认值”。

### 2.2 为什么一份 canonical 源优于五份 surface 文档

| 方案 | 问题 |
| --- | --- |
| 五份独立 OpenAPI（customer/operations/ivr/public/callbacks） | `Problem`、分页、幂等头、金额/时间/ID、mTLS/会话 scheme、A0 事件身份会被复制五份；修改一个通用规则需要五次同步；跨面冲突（例如客户和运营对同一状态命名不同）无法被单次 lint 发现；生成器要么处理五个不同版本，要么在客户端侧再造合并层 |
| 一份 canonical 源 + 五个 tag/扩展 | 所有共享组件只有一份；跨面一致性由同一份 Redocly 规则和同一份生成验证保证；每个 operation 仍通过 `x-onecall-surface` 和 `x-onecall-audience` 明确所有权与发布边界；后端仍按模块/信任边界拆包，不因单文件而共享运行时 |
| 五份“说明文档”而不是机器可读源 | 无法生成类型、无法静态校验错误/security/cursor 完整性，必然与实现漂移；A1 明确只写一份人类治理文档，不写五份面文档 |

**结论**：单文件 canonical 源是契约层的最小真相；运行时仍按五个信任面隔离。OpenAPI 文件不是微服务拆分依据，也不是前端可整体发布的 SDK。

## 3. 产物布局、所有权与最小性

### 3.1 精确路径

| 路径 | 内容 | Owner | 是否 canonical |
| --- | --- | --- | --- |
| `api/openapi/onecall-v1.openapi.yaml` | 唯一 OpenAPI 3.1 契约源；五个 tag、全部 operation、components、错误 registry、security schemes | A1 契约作者 | 是；任何 endpoint/schema 只在这里定义 |
| `api/openapi/redocly.yaml` | Redocly lint/bundle 配置；启用 recommended 与自定义规则 | A1 验证作者 | 验证配置，不是契约 |
| `api/openapi/contract-rules.mjs` | Redocly 自定义规则：metadata、security、A0 表名、Problem、cursor、幂等、敏感字段、无绝对 server | A1 验证作者 | 验证实现 |
| `api/openapi/check-doc-map.mjs` | 读取 bundle 后的 JSON 与 `docs/15`，支持 `--stage s1|s2|s3`：断言该 stage 的精确 operationId 集合/五面计数，并断言 A0 模型表在映射文档中均被覆盖 | A1 验证作者 | 验证实现 |
| `api/openapi/client-compat.ts` | 编译期 fixture：引用生成类型的 `operations`/关键 request/response，断言全部 operationId 存在 | A1 验证作者 | 验证实现 |
| `api/openapi/tsconfig.client-compat.json` | strict TS 配置，仅编译 fixture 与生成 d.ts | A1 验证作者 | 验证配置 |
| `api/openapi/package.json` / `package-lock.json` | 固定 `@redocly/cli`、`openapi-typescript`、`typescript` 的解析版本；提供 `lint`/`bundle`/`generate`/`typecheck`/`doc-map`/`check`/`check:s1`/`check:s2`/`check:s3` 脚本 | A1 验证作者 | 工具锁文件 |
| `docs/15-production-openapi-surface-spec.md` | 中文治理文档：五面端点清单、A0 映射、错误码到状态/A0 映射、开放输入、验证命令、非运行限制 | A1 文档作者 | 人类映射文档；不重复定义 endpoint |
| `docs/gateflow/a1-production-openapi-surfaces-spec-s{1,2,3}-implementation.md` | 每个 slice 的完成报告（模板见 §10.3） | 实施 Agent | Gateflow 证据 |
| `tmp/openapi-client/*` | 生成 d.ts、bundle JSON；`tmp/` 已在 `.gitignore`，不提交 | 验证脚本 | 生成物，不是源 |

### 3.2 为什么这是最小布局

- **一个契约源**：避免五份 surface 文档的复制与漂移；跨面共享组件只维护一次。
- **一套验证工具**：Redocly 同时完成 parse/lint/bundle；openapi-typescript + tsc 完成兼容性；`check-doc-map.mjs` 只做 Redocly 无法做的“文档覆盖/计数”断言，不多建测试框架。
- **一份治理文档**：OpenAPI 适合表达机器约束，不适合长篇解释 A0 状态机、开放输入与信任边界；`docs/15` 承接这些内容，避免把大段中文塞进 schema description 或另建五份面文档。
- **不提交生成客户端**：生成物是验证输出，不是第二个真相源；避免“生成的客户端与 OpenAPI 谁先改”的漂移。
- **不新建 `docs/16` 或 API README**：命令可由 `api/openapi/package.json` 和 `docs/15` 唯一表达；减少重复入口。

### 3.3 文件所有权规则

- 只有 `api/openapi/onecall-v1.openapi.yaml` 可以新增/修改 operation；`docs/15` 必须逐 operation 对应，不能出现文档有而契约没有或反向的 endpoint。
- `contract-rules.mjs` 的 A0 表名集合从 `docs/14` 的 `<!-- catalog:start -->` 块读取，不手工复制 44 张表；`docs/14` 变化时规则自动发现差异。
- `client-compat.ts` 必须引用生成类型而不是手写 interface；不得为了让 fixture 通过而放宽 `tsconfig` 的 `strict`。
- 现有 09-14 文档不在 A1 修改范围内；A0 是只读真源。

## 4. 公共契约设计

### 4.1 契约根与版本

- `openapi: 3.1.0`；JSON Schema 使用 2020-12 语义；`info.version: 1.0.0` 表示首个契约基线，不等于生产服务版本。
- 所有路径以 `/api/v1/` 开头；`v1` 是 URL major，破坏性变更必须新建 `onecall-v2.openapi.yaml`，不得在同一文件内静默改语义。版本前缀必须出现在每个 OpenAPI `paths` 条目和 §5 每个 operation 路径中，不能只靠 server/base URL 前缀。
- 顶层 `servers` 是**必填**字段，且必须恰好包含一项相对根 `{url: /}`（可带部署说明 description）；缺失 `servers`、绝对 URL（含域名/IP/COS/Redis/微信/供应商地址）或额外 server 根均使静态校验失败。`servers: /` 只表示部署根，不承担 `/api/v1` 版本前缀，也不得用 base URL 前缀替代路径版本。
- IVR `protocolVersion` 是独立输入字符串（1-32 字符），不与 OpenAPI `info.version` 绑定；其允许值/协商规则保留 Q15/Q26。
- 顶层 `x-onecall-contract` 记录：contract version、surface 列表、problem registry、生成器版本、A0 基线引用、`runtime: not-implemented`。

### 4.2 Security scheme 抽象

| Scheme | OpenAPI 定义 | 适用面 | 明确不定义 |
| --- | --- | --- | --- |
| `customerSession` | `type: http, scheme: bearer, bearerFormat: opaque-session` | customer；`ORGANIZATION_MEMBER` 分享准入 | 微信/OTP/刷新/登出/issuer/Token 结构；Q19 |
| `employeeSession` | 同型，独立 audience `employee` | operations | 员工认证源、MFA、密码/证书；Q1 |
| `ivrMtls` | `type: mutualTLS` | ivr | 证书颁发、白名单、轮换窗口；Q7/Q15 |
| `ivrSignature` | `type: apiKey, in: header, name: X-ONECALL-SIGNATURE` | ivr（与 mTLS 同时要求） | 签名算法、时间窗、nonce 形态；Q7/Q15/Q25 |
| `providerCallbackMtls` | `type: mutualTLS` | callbacks | 供应商证书映射；Q6 |
| `providerCallbackSignature` | `type: apiKey, in: header, name: X-ONECALL-CALLBACK-SIGNATURE` | callbacks（与 mTLS 同时要求） | 供应商签名串/payload；Q6/Q15 |

补充约束：

- IVR/callback 还声明 `X-ONECALL-TIMESTAMP`、`X-ONECALL-NONCE` 为必填 header 参数；具体时间窗、nonce 存储/重放窗口和算法不在 A1。
- 公开分享不使用 OpenAPI security scheme 表示 capability token；token 是路径参数 `capabilityToken`，服务端按摘要解析。`ORGANIZATION_MEMBER` 模式额外允许/要求 `customerSession`；operation 的 `security` 表达为 `[{}, { customerSession: [] }]`，由服务端按模式强制。
- operations 的固定角色只通过 `x-onecall-required-roles` 表达（`ADMIN`、`OPS` 或两者），不建 OAuth scope/自定义角色；角色矩阵/员工认证未决部分保留 Q1/Q27。
- 所有 operation 都必须符合五面之一的信任边界，不得出现 `security: []`；公开分享 GET 也必须通过路径 capability 校验，不能因为无 security scheme 被视为公开无鉴权。A1 **没有匿名 operation**：线下客服联系方式/说明属于 UI/部署配置，不建匿名 API，也不在 OpenAPI 中定义 `x-onecall-anonymous-read` 例外。

### 4.3 服务端派生授权与请求禁止字段

每个 operation 的 `x-onecall-authorization` 用一句可执行谓词描述，例如：

- `customerSession.accountId == server-derived; workspace access re-read; subject owner/claim/risk predicate server-side`
- `employeeSession.role in fixed ADMIN|OPS; resource access derived server-side; no recording playback`
- `ivrMtls.serviceIdentity + trusted caller + canonical access number + call/session/task predicates server-side`
- `capability token digest + share status/generation/expiry + verification mode server-side`
- `provider signature verified + normalized provider event identity server-side`

所有请求 schema 禁止出现（或只允许只读不参与授权）：

`accountId`、`createdByAccountId`、`ownerAccountId`、`ownerOrganizationId`（除分享创建中由服务端从资源派生）、`role`、`workspaceRole`、`priceCents`、`amountCents`（订单请求）、`currency`、`paymentStatus`、`paymentSuccess`、`paidAt`、`evidenceSolidified`、`evidenceStatus`、`recordingOwner`、`taskClaimed`、`reservationId`（IVR 客户端不能创建）、`authorizedRecordingSeconds`、`authorizationExpiresAt`、`shareAdmissionCount`、`shareStatus`、`objectKey`、`bucket`、`objectVersionId`、`capabilityTokenHash`、`codeHash`、`codeSalt`、`cryptoKeyRef`。

例外：分享创建的 `designatedMobile` 是受保护输入，服务端规范化后只存 HMAC；返回时不回显。响应侧的 `capabilityToken`/`extractionCode`（`ShareCreated`）与 code rotation 的 `extractionCode`（`ShareCodeRotation`）使用 `readOnly: true` + `x-onecall-return-once: true` + `x-onecall-log-redaction: required`，只在生成响应中出现一次，不得出现在日志、审计、事件或后续读取。请求侧的验证秘密（如分享准入的提取码）使用 `writeOnly: true`。生成器 fixture 必须断言响应类型含一次性秘密，且 `ShareCodeRotation` 不含 `capabilityToken`、请求类型不接受响应秘密。

### 4.4 Correlation / trace

- 每个请求可带标准 W3C `traceparent`；服务端生成/继承统一 `traceId`，并在 `Problem.traceId` 和响应 header `X-Trace-Id` 返回。
- 客户端 `requestId`/`Idempotency-Key`/`eventId` 只是业务身份，不作为 trace ID，也不作为日志相关性唯一来源。
- 日志/审计只允许内部 ID、脱敏号码、事件/状态码、hash 前缀；禁止完整主叫/被叫、对象键、capability token、提取码、验证码、provider payload、KMS/HMAC 值和 session token。
- 公开分享路径中的 capability token 必须由 edge/应用日志脱敏；OpenAPI 用 `x-onecall-log-redaction: required` 标记，但 A1 不能证明 edge 已配置。

### 4.5 RFC 9457 Problem 基座

`components.schemas.Problem`（`application/problem+json`）：

| 字段 | 类型 | 约束 |
| --- | --- | --- |
| `type` | string, `format: uri-reference` | A1 使用 `about:blank` 或稳定的相对 URN；不得指向未批准外部站点 |
| `title` | string | 稳定、面向人的短标题；不复制供应商原文/堆栈 |
| `status` | integer 400-599 | 必须与 HTTP status 一致 |
| `detail` | string | 可安全展示的补充说明；不含受保护值/内部细节 |
| `instance` | string, `format: uri-reference` | 请求路径/服务端生成的 problem instance；不含 capability token |
| `code` | `$ref ProblemCode` | 稳定业务码；机器分支唯一依据 |
| `traceId` | string | 与服务端 trace 一致 |
| `errors` | array of `FieldProblem` | 可选字段级错误：`pointer`（JSON Pointer）、`code`、`message`；不返回值内容 |

`ProblemCode` 是 `components.schemas` 中的字符串 enum；顶层 `x-onecall-problem-registry` 为每个 code 记录 `{httpStatus, a0Origin, surfaces}`。`contract-rules.mjs` 断言两者集合完全一致、每个 operation 的 `x-onecall-error-codes` 都在 registry 中、每个 registry code 至少被一个 operation 引用（`INTERNAL` 除外）。A0 已有的 reason code（如 `CALL_UNKNOWN`、`EVENT_ID_CONFLICT`、`SHARE_*`）不得改名；A1 新增码只用于认证、分页、幂等、校验、状态条件/授权边界等传输或契约边界语义，不复制 IVR 决策 reason。

### 4.6 业务错误 registry 策略

- 错误码按“稳定业务含义”命名，不按 HTTP 或供应商命名；例如 `OUTBOUND_TASK_CONFLICT`、`OUTBOUND_CALL_IN_PROGRESS`、`SHARE_ADMISSION_LIMIT_REACHED`。
- A0 已定义的 IVR reason/action 继续出现在 IVR decision 的 `reasonCode`/`action` 枚举中；`Problem.code` 用于请求失败，不把每个 decision reason 都复制成 HTTP 错误。A0 IVR reason `TASK_CONFLICT`、`CALL_IN_PROGRESS`、`TASK_CLAIMED`、`TASK_UNAVAILABLE` **只允许出现在 `IvrDecision.reasonCode`**，不得作为任何 Customer/Operations/Public/IVR Problem code。
- 客户待拨任务面使用三个独立、精确的 409 Problem code：`OUTBOUND_CALL_IN_PROGRESS`（同一主体/规范主叫已存在 `ACTIVE`/`UNKNOWN` call，排除当前 `callId` 场景）、`OUTBOUND_TASK_CONFLICT`（不兼容的既有非终态任务，或同一 action identity 语义已变化）、`OUTBOUND_TASK_NOT_CANCELLABLE`（C8 任务已被领取或不再是 `PENDING`）。它们与 A0 IVR reason 语义相近但信任面/传输含义不同，不得互相替换。
- I1/I2/I3 的正常领域结果（含 `REJECT_CALL_IN_PROGRESS`、`REJECT`/`TASK_CONFLICT`、`CONFIRMATION_NOT_ALLOWED`、`CONSENT_REQUIRED` 及 direct authorization 的非外呼结果）一律返回 `200 IvrDecision`。只有认证/参数错误（400/401）、身份/指纹/access-number 冲突等**无法冻结决策**的请求失败使用 `Problem`（409 等）；`CONFIRMATION_NOT_ALLOWED` 不再是 Problem code，而是 IvrDecision 的稳定 `reasonCode`。
- 同一语义在不同面复用同一 code；不同语义不得复用一个 code。供应商错误先由适配层映射为内部 code，禁止把供应商 code/消息放进 `code`/`detail`。
- 错误 registry 是 A1 的稳定契约；新增 code 必须同时更新 OpenAPI enum、registry、`docs/15` 映射和至少一个 operation 的 `x-onecall-error-codes`。

代表性映射（完整表在 `docs/15`）：

| Code | HTTP | 含义/A0 锚点 |
| --- | --- | --- |
| `UNAUTHENTICATED` | 401 | 无/失效客户或员工会话；不区分用户是否存在 |
| `FORBIDDEN` | 403 | 会话有效但当前访问/角色/资源归属不允许；统一拒绝，避免泄露 |
| `NOT_FOUND` | 404 | 资源不存在或当前身份不可见；不返回其他对象的替代 |
| `VALIDATION_FAILED` | 422 | schema/字段/枚举/长度不合法；`errors[]` 定位 |
| `CURSOR_INVALID` | 400 | cursor 不可解析/过期/与当前过滤条件不匹配 |
| `IDEMPOTENCY_CONFLICT` | 409 | 同键不同语义输入；A0 `platform.idempotency_record` 冲突 |
| `CLAIM_RISK_BLOCKED` | 409 | A0 claim risk 优先于 replay；不返回历史成功 |
| `CLAIM_REPLAY_CONFLICT` | 409 | A0 claim replay identity 与已存结果不一致；不返回历史成功 |
| `CLAIM_CONTEXT_CONFLICT` | 409 | 同账户但组织/绑定/号码/proof/method 上下文不一致；不复用旧结果 |
| `OUTBOUND_CALL_IN_PROGRESS` | 409 | 同一主体/规范主叫已存在 `ACTIVE`/`UNKNOWN` call（排除当前 `callId`）；客户任务创建，不是 IVR reason |
| `OUTBOUND_TASK_CONFLICT` | 409 | 不兼容的既有非终态任务，或同一 action identity 的语义已变化；客户任务创建/取消，不是 IVR reason |
| `OUTBOUND_TASK_NOT_CANCELLABLE` | 409 | C8 任务已被领取或已不处于 `PENDING`；不是 IVR reason |
| `ACTIVATION_STATE_CONFLICT` | 409 | O3/O4 仅允许 ACTIVE→SUSPENDED / SUSPENDED→ACTIVE；当前状态或修订不匹配，不重发试用 |
| `CONSENT_REQUIRED` / `ACTIVATION_CONSENT_DECLINED` | 409 | 开通提示/政策缺失、过期或明确拒绝；不落有效开通同意 |
| `PURCHASE_CONSENT_REQUIRED` / `PURCHASE_CONSENT_STALE` / `PURCHASE_CONSENT_DECLINED` | 409 | 购买同意缺失、提示/政策过期或明确拒绝；不落有效购买同意、不建订单 |
| `REAL_NAME_REQUIRED` | 422 | 普通客户端购买需实名（09/12），IVR 试用例外不适用于订单 |
| `ACTIVATION_NOT_ACTIVE` | 403 | 当前 `activation_consent.service_activation.status != ACTIVE`（`INACTIVE`/`SUSPENDED`）；用于 C10 下单和 C6 任务创建；统一拒绝，不泄露其他主体事实 |
| `ORDER_UNKNOWN_PAYMENT_PENDING` | 409 | 同主体/offer 存在 `UNKNOWN` 订单，必须先查询原订单 |
| `CATALOG_DRAFT_CONFLICT` | 409 | DRAFT 目录候选的 `(product_id, version_no)` 重复、非 DRAFT 状态、同自然身份内容变化或被不可变约束拒绝 |
| `CATALOG_DRAFT_STALE_VIEW` | 409 | 替换 DRAFT 目录候选时 `expectedRevision` 不匹配且当前内容不能证明该更新已应用 |
| `TRIAL_POLICY_DRAFT_CONFLICT` | 409 | DRAFT 试用策略候选的 `(campaign_key, version_no)` 重复、非 never-published 状态或同自然身份内容变化 |
| `TRIAL_POLICY_DRAFT_STALE_VIEW` | 409 | 替换 DRAFT 试用策略候选时 `expectedRevision` 不匹配且当前内容不能证明该更新已应用 |
| `CATALOG_PUBLICATION_CONFLICT` / `PUBLICATION_STALE_VIEW` | 409 | A0 T-S2-14 期望版本/修订冲突 |
| `TRIAL_POLICY_PUBLICATION_CONFLICT` / `TRIAL_POLICY_STALE_VIEW` | 409 | 同左，试用策略发布 |
| `IVR_ARGUMENT_INVALID` | 400 | IVR 事件/确认 ID 非空 1-128 等协议参数错误 |
| `CALL_UNKNOWN` | 404 | 通话事件父 call 不存在；不得创建占位 call |
| `EVENT_ID_CONFLICT` | 409 | 同 eventId 不同指纹；首次结果吸收 |
| `UPLOAD_OBSERVATION_REQUIRED` | 409 | recording 仍 `DECLARED`，必须先由 evidence/media adapter 观察上传 |
| `RECORDING_IDENTITY_CONFLICT` | 409 | 同 recordingId 不同对象/哈希；不覆盖首次成功事实 |
| `SHARE_TOKEN_INVALID` | 404 | 缺失/无效/退役能力令牌统一拒绝，不区分是否存在 |
| `SHARE_RISK_BLOCKED` | 403 | 绑定 `OPEN` 风险；不创建准入 |
| `SHARE_WRONG_TENANT` | 403 | 企业成员分享的账户/组织/成员不匹配 |
| `SHARE_VERIFICATION_FAILED` | 403 | code/designated-mobile/企业成员验证失败；不消耗准入；不泄露期望 secret |
| `SHARE_LOCKED` / `SHARE_GENERATION_STALE` | 409 | 当前锁定/撤销世代变化；新准入拒绝，已准入会话按窗口 |
| `SHARE_ADMISSION_LIMIT_REACHED` / `SHARE_SESSION_EXPIRED` | 409 / 410 | 新准入达上限/已准入会话过期；均不重复计数 |
| `SHARE_EXPIRED` | 410 | 有效令牌但 grant 已过期；不创建准入/凭证 |
| `CALLBACK_SIGNATURE_INVALID` | 401 | 回调签名/证书失败；不处理 payload |
| `PAYMENT_EVENT_CONFLICT` | 409 | 同供应商事件不同内容；隔离，不推进订单 |
| `DEPENDENCY_UNAVAILABLE` / `CALLBACK_RETRY_LATER` | 503 | 依赖未知/不可用；失败关闭，不伪造成功 |

### 4.7 幂等与事件身份

| 写类别 | 身份 | 规则 |
| --- | --- | --- |
| 客户创建订单 | `Idempotency-Key` 1-475；A0 平台键 `accountId:key` | 同键同语义返回首单；同键不同语义 `IDEMPOTENCY_CONFLICT`；同 subject/offer 的 `UNKNOWN` 支付阻断新单 |
| 客户待拨任务 | `Idempotency-Key` 1-512；A0 `(createdByAccountId, idempotencyKey)` | 同键同指纹返回原任务，不续期；替换创建新任务并终结旧 `PENDING` |
| 客户认领 | `Idempotency-Key` 1-128 = A0 `request_id` | 风险检查先于 replay；同键不同 subject/account/binding/proof 冲突 |
| 客户激活/购买同意 | body `activationEventId` / `purchaseConsentEventId` 稳定身份 | 激活不使用平台 idempotency scope；同 event/fingerprint 返回首次；购买同意由服务端从 account+Idempotency-Key 派生，客户端不另生成 |
| 运营暂停/恢复 | `Idempotency-Key` 1-128 = A0 `source_action_event_id` | `expectedActivationRevision` 条件更新；同动作同指纹返回首次 transition |
| 运营目录/试用策略发布/停用 | state-conditional：`expectedActiveVersionId`/`expectedEnabledPolicyVersionId` + `expectedRevision` | A0 确定性 Outbox 身份；同状态重试返回当前状态；stale 返回稳定冲突，不重试选另一个版本 |
| 运营目录/试用策略 DRAFT 候选创建 | state-conditional：候选自然唯一键 `(product_id, version_no)` / `(campaign_key = FIRST-TRIAL, version_no)` + 持久化字段派生的 canonical semantic content projection；**不接受 `Idempotency-Key`** | 不使用也不新增 `platform.idempotency_record` scope。首次插入返回 201；同自然身份且投影相同（既有 `DRAFT`/never-published）返回既有/首个 DRAFT（200），不再插入；同自然身份但投影不同或状态不符返回 `CATALOG_DRAFT_CONFLICT`/`TRIAL_POLICY_DRAFT_CONFLICT`（409）。只创建 `DRAFT`/never-published 候选 |
| 运营目录/试用策略 DRAFT 候选替换 | state-conditional：body `expectedRevision` + canonical requested/current mutable-content projection；**不接受 `Idempotency-Key`** | 首次匹配 revision 时执行一次条件更新并 revision+1，返回 200；重试时若当前 revision/content 证明该次更新已应用（`currentRevision = expectedRevision + 1` 且当前投影等于请求投影）返回当前 DRAFT（200），不再更新；否则 stale revision → `*_DRAFT_STALE_VIEW`（409）；非 DRAFT、已发布或不可变内容变化 → `*_DRAFT_CONFLICT`（409）。不新增 benefit-line/channel-set 变更能力 |
| IVR 呼入/再决策/直接授权 | `callId + purpose` 决策身份；确认/直接授权另有 1-128 `eventId` | 同指纹 replay 冻结 decision，不重新领取/预占/外呼；同 event 不同指纹冲突；`authorizationExpiresAt` 永不续期；正常领域拒绝仍是 200 IvrDecision |
| IVR 通话事件 | `(IVR_EVENT, eventId)` + 语义指纹 | 同指纹返回首次 `APPLIED/QUARANTINED/IGNORED`；不同指纹 `EVENT_ID_CONFLICT`；首次终态吸收 |
| IVR 录音 | `recordingId` | 同身份同对象事实返回首次 VALIDATED/QUARANTINED；不同对象事实 `RECORDING_IDENTITY_CONFLICT` |
| 公开分享创建 | body `creationRequestId` 1-128 → deterministic `share_id` | 同身份同指纹返回既有 grant 元数据，不重发 token/code；改 scope `SHARE_CREATION_CONFLICT` |
| 公开分享准入 | body `admissionRequestId` 1-128 → `admission_key` | 同键 replay 先于上限检查；不重复计数/`ADMISSION_GRANTED`；过期会话不续期；准入已提交但 ticket 失败时失败关闭 503，重放只重发票据、不重新计数 |
| 分享撤销/换码 | `Idempotency-Key` 1-128 = A0 action request ID | 确定性 transition identity；同动作同指纹返回首次；不同动作是独立 transition |
| 支付回调 | 供应商事件身份（适配层规范化）+ payload 指纹 | 同事件同内容返回首处理；同事件不同内容隔离；不根据客户端/IVR 声明推进支付 |

**DRAFT 候选的 canonical content projection（O18-O21）**：投影只由持久化字段派生并按键名排序的 canonical JSON 计算：

- 目录版本：`{product_id, version_no, offer_type, audience, price_cents, currency, billing_cycle, installments, grant_interval_months, snapshot_sha256, benefit_lines[{entitlement_type, quantity, validity_days, grant_interval_months}] sorted by entitlement_type, channels sorted}`；`snapshot_sha256` 是服务端由这些语义字段生成的 canonical snapshot 哈希。
- 试用策略：`{campaign_key, version_no, granted_seconds, valid_days, snapshot_sha256}`。

`x-onecall-idempotency` 对 O18-O21 一律为 `state-conditional`；`x-onecall-a0-contracts` 只能引用 A0 的自然唯一约束、状态机和条件修订（`uq_catalog_version_no`、`uq_trial_policy_campaign_version`、`revision` 条件更新、§4.17/§4.29 的 publication guard），**不得引用 `platform.idempotency_begin`/`platform.idempotency_record`**。A0 `platform.idempotency_record` 的 scope registry 只有 `IVR_CONFIRM`（C5/I2 等确认）与 `PURCHASE`（C10 订单），见 `docs/14` §2.9 与 §4.3 `ck_idempotency_scope`；O18-O21 不属于任一 scope，A0 也未定义 draft create/replace 平台事务（T-S2-14 只发布既有 DRAFT）。任何新 scope（如 `CATALOG_DRAFT`/`TRIAL_POLICY_DRAFT`）、表、事务或 `Idempotency-Key` 声明都必须被静态校验拒绝。

### 4.8 Cursor 与稳定排序

- 所有 `GET` 资源集合使用 `cursor`（opaque，1-512 字符）和 `limit`（integer）。`limit` 默认 20、最大 100 仅是 **V1 API-governance 默认值**，Owner 为 Backend/API governance；它们不是安全策略或产品策略，服务端可进一步 clamp，具体见 docs/15。响应使用 `*Page {items, nextCursor, hasMore}`；不返回 `total`，除非未来明确批准。
- cursor 是服务端生成的不透明值，绑定：稳定排序键、最后一条记录的内部排序值、**规范化过滤条件摘要**（filter digest）、contract/surface 版本；不得是 offset、SQL key、HMAC、时间戳或可枚举 ID 的明文。
- 稳定排序（除非 operation 明确写出另一组）：
  - 客户业务号码：`subject.createdAt desc, binding.id asc`（A0 `business_subject.created_at` 存在；`caller_number_binding` 无独立创建时间，使用稳定 binding ID 作 tie-break）。
  - **客户目录 offer（C9）**：`productCode asc, versionNo desc, catalogOfferId asc`；cursor 绑定规范化 `channel + audience + offerType` filter digest，不能跨过滤条件复用。
  - 客户订单、证据、运营订单、证据/IVR 会话、审计：`createdAt/occurredAt desc, id desc`。
  - 客户分享：`validFrom desc, id desc`（A0 share grant 无独立 `created_at`，用冻结 `valid_from`）。
  - **运营主体（O1）**：`createdAt desc, subjectId desc`（A0 `business_subject.created_at`；`subjectId` 是稳定 uuid tie-break）；cursor 绑定规范化结构化过滤摘要 `{subjectId?, source?, claimStatus?, activationStatus?, realNameStatus?, riskState?}`，不得跨过滤条件复用；**不允许完整号码或自由文本搜索**。
  - 运营用户：`createdAt desc, id desc`；运营目录产品：`productCode asc, id asc`；目录版本：`versionNo desc, id desc`。
  - 运营激活列表：`occurredAt desc, id desc`（A0 activation transition 的 `occurred_at`）；试用策略：`versionNo desc, id desc`（A0 trial policy 无独立 `created_at`，`published_at` 仅发布后存在）。
- 过滤条件变化后沿用旧 cursor 必须失败：`CURSOR_FILTER_MISMATCH`（400）；cursor 不可解析/被篡改/跨 surface 使用：`CURSOR_INVALID`（400）。
- 页面打开、验证页、预取、失败验证不是资源列表，不使用 cursor，也不得消耗分享准入。**P1 是 observation-free GET**：页面打开/预取不写 `PAGE_OPENED`、`PREFETCH_IGNORED` 或任何 `share_access_event`。

### 4.9 敏感字段与响应投影

- 所有对外号码只返回 `masked`（A0 `number_masked`/`caller_masked`/`callee_masked`）或内部 ID；主叫/被叫/接入号明文只在 IVR mTLS 内部响应或受保护的分享媒体凭证中出现，不进入普通客户/运营/公开响应。
- 响应禁止：`objectKey`、`bucket`、`objectVersionId`、`cryptoKeyRef`、`capabilityTokenHash`、`codeHash`、`codeSalt`、`declaredSha256`（服务端验证前不作为事实）、raw provider payload、`payloadCiphertext`、HMAC、session token、Redis ticket、完整身份证/姓名。
- 分享创建响应允许一次性 `capabilityToken` 和 `extractionCode`；code rotation 响应只允许一次性新 `extractionCode`，**永不返回、暴露或重建 `capabilityToken`**。响应秘密统一 `readOnly: true` + `x-onecall-return-once: true` + `x-onecall-log-redaction: required`；请求侧验证秘密（如 `EXTRACTION_CODE` 的 `code`）使用 `writeOnly: true`。后续查询只返回存在性/模式/状态，不返回秘密。
- 公开分享验证模式枚举固定为 A0 `NONE`、`EXTRACTION_CODE`、`DESIGNATED_MOBILE`、`ORGANIZATION_MEMBER`；`NONE` 只表示持有链接，不记录为已验证身份；`DESIGNATED_MOBILE` 必须消费已批准的受信短信持有证明，单纯知道号码或比较 HMAC 不够；`ORGANIZATION_MEMBER` 必须同时使用当前客户会话检查当前企业成员关系。
- 公开分享响应不返回 `shareId`、`evidenceId`、`subjectId`、`objectKey`/URL；只返回安全的 `content` 摘要（capture time、validated duration、artifact roles、MIME/size、hash prefix 可选）和验证模式。P3 是媒体凭证签发/重建控制面：返回 `MediaAuthorization{credential, expiresAt}`，由媒体网关校验；`ORGANIZATION_MEMBER` 的重发必须绑定已准入的 recipient witness（账户/组织/成员），且 `LOCKED` 不阻止已准入有效会话的凭证重发。
- **P1 的 ORGANIZATION_MEMBER 门控**：P1 在 `ORGANIZATION_MEMBER` 模式且没有匹配的已认证客户会话时，只返回验证/挑战元数据（`verificationMode`、`status`、`expiresAt`、`downloadAllowed`、`verificationChallenge{customerSessionRequired: true}`），**必须省略共享 `content`**；只有当当前认证账户/组织/成员与 share 的租户匹配后才投影 `content`。`NONE`/`EXTRACTION_CODE`/`DESIGNATED_MOBILE` 的公开页面行为不变；P2 的新准入验证与 P3 的已准入 recipient witness 继续按 §5.4 执行。
- **媒体/上传凭证（响应侧，只读）**：`RecordingDeclaration.upload.credential`、`ShareAdmission.mediaAuthorization.credential` 和 `MediaAuthorization.credential` 必须标记 `readOnly: true` + `x-onecall-log-redaction: required`。它们由重放/重建语义决定可再次返回，**不添加 `x-onecall-return-once`**；不得进入日志、审计、事件或普通查询响应。
- 运营响应不返回原始录音、完整号码、证件、原始 provider payload、内部 SQL/堆栈；`GET /api/v1/operations/evidence` 类读取如有，只返回安全元数据；A1 不提供运营播放 endpoint。

### 4.10 时间、金额与标识符

- 时间：`format: date-time`，RFC 3339 UTC（`Z`）；数据库/服务端时间为准；客户端 `receivedAt`/展示时间不用于过期/授权判断。`expiresAt`/`authorizationExpiresAt` 是服务端冻结的绝对时间。
- 金额：`Money { amountCents: integer >= 0, currency: 'CNY' }`；人民币分整数，禁止浮点。目录中的 `priceCents: null` 只允许 `QUOTE`/企业咨询；企业报价不进入个人自助支付。
- 标识符：
  - 内部资源 ID 使用 `format: uuid`；`taskId` 1-64、`callId` 1-128、`recordingId` 1-128、`eventId` 1-128、`orderNo` 1-64、`shareId` 1-128；均是不透明字符串，不是号码/证件/自然键。
  - `sessionId` 使用 UUID；`decisionId`/`reservationId` 使用 UUID。
  - `Idempotency-Key` 客户订单 1-475，客户任务 1-512，claim/operation action 1-128；不得截断、不得用客户端生成随机值替代稳定派生身份。O18-O21 DRAFT 创建/替换**不使用** `Idempotency-Key`（state-conditional 自然身份/`expectedRevision`）。
- 枚举：全部使用 A0/09/13 的精确大写值；未知枚举按失败关闭，不默认允许。IVR eventType/payload schema version 允许稳定字符串但值保留 Q15/Q26。

### 4.11 生成 TypeScript 的兼容纪律

- `operationId` 使用稳定 camelCase；一旦发布不得重命名。新增 operation 只允许追加，破坏性变更走 v2。
- 所有 request/response 使用 `components.schemas` 命名引用；request body 必须 `required: true`；响应 `content` 指定 JSON/problem media type。
- 对象默认 `additionalProperties: false`；仅 `OpaqueProviderPayload` 与延迟的 IVR event `payload` 允许开放对象，并标记 `x-onecall-opaque: true` + 原因/owner。
- 可空字段使用 `type: [T, 'null']`（OpenAPI 3.1），不使用 `nullable: true`；不混用 `oneOf`/`anyOf` 造成生成器歧义；分享验证使用显式 `discriminator: mode`。
- `readOnly` 用于服务端返回字段；响应侧一次性秘密（`ShareCreated.capabilityToken`/`extractionCode`、`ShareCodeRotation.extractionCode`）使用 `readOnly: true` + `x-onecall-return-once: true`；请求侧秘密（分享准入 `EXTRACTION_CODE` 的 `code`）使用 `writeOnly: true`。生成器不得把 writeOnly 字段变成请求可选。媒体/上传凭证（`RecordingDeclaration.upload.credential`、`ShareAdmission.mediaAuthorization.credential`、`MediaAuthorization.credential`）使用 `readOnly: true` + `x-onecall-log-redaction: required`，但**不带** `x-onecall-return-once`。
- `client-compat.ts` 必须包含生成类型断言：`ShareCodeRotation` 不含 `capabilityToken`；`ShareCreated` 的响应类型含一次性秘密；分享创建/码轮换的请求类型不接受 `capabilityToken`/`extractionCode`；分享准入请求含 `writeOnly` code、响应不含 code；三个响应凭证字段可读且不进入请求类型；`PublicShareView.content` 在 `ORGANIZATION_MEMBER` 未认证路径可省略。
- 生成器版本固定在 `api/openapi/package-lock.json`；生成产物写入 `tmp/openapi-client/`，不提交。CI/本地检查必须重新生成并编译 fixture，不能依赖上一次产物。
- 生成 d.ts 是跨面类型投影，不表示一个前端可加载全部 operation；各端仍按 `x-onecall-audience`/surface 过滤，运营/IVR/回调类型不得进入客户小程序包。

### 4.12 Opaque 扩展预算

A1 只允许两类 opaque 边界：

1. **支付回调请求体**：`OpaqueProviderPayload`（object，`additionalProperties: true`），由供应商适配层验签、规范化并加密留档；OpenAPI 不复制供应商字段。A1 只保证 2xx/Problem/幂等确认。
2. **IVR 通话事件 body**：`IvrEventPayload`（object，`additionalProperties: true`），`eventType`/`legId`/`sequenceNo` 稳定封套字段固定，具体事件 payload、media/retry/签名串和 schema version 属后续 IVR 协议（Q15/Q26）。

除这两处外，不允许 open object 逃避 schema 设计；新增 opaque 必须在 `docs/15` 记录 owner/destination。

## 5. 五面端点清单

图例：`S`=surface；`Auth`=OpenAPI security；`Idem`=幂等/事件；`A0`=主要表/事务（完整映射见 §7.1）；`No-expose`=该 operation 不得返回的敏感/内部字段。所有 operation 默认有 401/403（公开分享用统一 404/403）、422、429、500/503 的公共 Problem 响应；表中只列该 operation 特有的成功/冲突状态。**A0 列是 `x-onecall-a0-models` 的规范来源，必须使用 docs/14 中的精确 schema-qualified 表名；短名、通配符和错误 schema 均使校验失败。**

### 5.1 客户面 `/api/v1/customer/**`（18）

| # | operationId / 路径 | 目的与访问边界 | 权威服务端输入 | Idem | 成功/特有错误 | A0 | No-expose |
| --- | --- | --- | --- | --- | --- | --- | --- |
| C1 | `getCustomerMe` `GET /api/v1/customer/me` | 当前客户账户、可用工作空间、派生角色；customerSession | session → account；ACTIVE account + ACTIVE membership/organization；不信任 query 中的 account/role | GET | 200 `CustomerContext`；401/403 | `customer_iam.customer_account`, `organization_access.organization`, `organization_access.organization_membership` | provider subject/HMAC、证件、完整号码 |
| C2 | `listCustomerBusinessNumbers` `GET /api/v1/customer/business-numbers` | 当前账户/空间可访问的业务号码（脱敏）、绑定/主体/开通状态；customerSession | session + server workspace scope；只返回有权绑定；cursor/limit | GET | 200 `BusinessNumberPage`；`CURSOR_INVALID` | `subject_number.business_subject`, `subject_number.caller_number_binding`, `activation_consent.service_activation` | 完整号码、HMAC、密文、号码归属推断 |
| C3 | `getCustomerSubject` `GET /api/v1/customer/subjects/{subjectId}` | 当前可访问主体的开通/试用/当前同意要求摘要；customerSession | server access predicate；subject owner/claim/risk；当前 policy/prompt | GET | 200 `CustomerSubjectDetail`；404 统一 | `subject_number.business_subject`, `activation_consent.service_activation`, `activation_consent.consent_record`, `entitlement_metering.trial_grant`, `entitlement_metering.entitlement_balance`, `subject_number.subject_claim` | 风险内部细节、证据字节、完整号码、他人主体 |
| C4 | `createCustomerSubjectClaim` `POST /api/v1/customer/subject-claims` | 追加已验证个人认领；customerSession | session account；subject/binding；服务端验证的 `verificationProofId`；风险/冲突先于 replay | `Idempotency-Key` 1-128 = `request_id` | 200 `ClaimResult`；409 `CLAIM_RISK_BLOCKED`/`CLAIM_REPLAY_CONFLICT`/`CLAIM_CONTEXT_CONFLICT`；422 `VALIDATION_FAILED`（proof 无效） | `subject_number.subject_claim`, `subject_number.business_subject`, `subject_number.caller_number_binding`, `subject_number.subject_risk_state` | 请求体 account/owner/role、verification ciphertext/proofRef、完整号码 |
| C5 | `createCustomerSubjectActivation` `POST /api/v1/customer/subjects/{subjectId}/activations` | MINIAPP/WEB 内部开通 + 首次试用；customerSession | session account/workspace；subject access；当前 policy/prompt/content；显式 ACCEPT/DECLINE | body `activationEventId` 1-128；同指纹 replay | 200 `ActivationResult`；409 `CONSENT_REQUIRED`/`ACTIVATION_CONSENT_DECLINED`；422 stale | `activation_consent.consent_record`, `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `entitlement_metering.trial_grant`, `entitlement_metering.entitlement_grant`, `entitlement_metering.entitlement_balance` | account/owner/trial outcome 由客户端决定；完整号码 |
| C6 | `createCustomerOutboundCallTask` `POST /api/v1/customer/outbound-call-tasks` | 创建/替换当前空间短期待拨任务；customerSession | session/workspace；`callerBindingId` 服务端归属校验；**当前 `service_activation.status = ACTIVE`**；**同一规范主叫/主体不存在其他 `ACTIVE`/`UNKNOWN` call**；callee 规范化；唯一非终态任务 | `Idempotency-Key` 1-512 | 201 `OutboundCallTask`；403 `ACTIVATION_NOT_ACTIVE`；409 `OUTBOUND_CALL_IN_PROGRESS`/`OUTBOUND_TASK_CONFLICT`；422 `VALIDATION_FAILED` | `ivr.outbound_call_task`, `subject_number.caller_number_binding`, `ivr.call_record`, `activation_consent.service_activation` | 明文 callee（仅脱敏）、他人任务、历史 callee |
| C7 | `getCustomerOutboundCallTask` `GET /api/v1/customer/outbound-call-tasks/{taskId}` | 当前账户/空间查询任务状态/倒计时；customerSession | 服务端按 task/owner/workspace 过滤；不按最新任务回退 | GET | 200 `OutboundCallTask`；404 统一 | `ivr.outbound_call_task`, `ivr.call_record` | 明文 callee、其他空间任务、内部 claim 细节 |
| C8 | `cancelCustomerOutboundCallTask` `POST /api/v1/customer/outbound-call-tasks/{taskId}/cancellations` | 取消本人当前空间仍为 PENDING 的任务；customerSession | task owner/workspace/status；条件更新 | `Idempotency-Key` 1-512 | 200 `OutboundCallTask`；409 `OUTBOUND_TASK_NOT_CANCELLABLE`/`OUTBOUND_TASK_CONFLICT` | `ivr.outbound_call_task` | 明文 callee、他人任务 |
| C9 | `listCustomerCatalogOffers` `GET /api/v1/customer/catalog/offers` | 当前渠道可见在售版本/offer/权益/versionToken；customerSession | session channel/audience；服务端读 ACTIVE version/offer；不信任 query 价格；稳定 `productCode asc, versionNo desc, catalogOfferId asc`；cursor 绑定规范化 `channel+audience+offerType` digest | GET；cursor/limit | 200 `CatalogOfferPage`；`CURSOR_INVALID`/`CURSOR_FILTER_MISMATCH` | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line` | 未批准价格/DEMO、草稿/下架版本、未授权渠道 |
| C10 | `createCustomerOrder` `POST /api/v1/customer/orders` | 创建个人/企业可支付订单快照；customerSession | session account/workspace；subject access；实名（普通客户）；**当前 `service_activation.status = ACTIVE`**；当前 ACTIVE offer/versionToken；服务端金额；购买同意 | `Idempotency-Key` 1-475；派生 purchase consent ID | 201 `Order`；403 `ACTIVATION_NOT_ACTIVE`；409 `IDEMPOTENCY_CONFLICT`/`ORDER_UNKNOWN_PAYMENT_PENDING`/`PURCHASE_CONSENT_REQUIRED`/`PURCHASE_CONSENT_STALE`/`PURCHASE_CONSENT_DECLINED`；422 `REAL_NAME_REQUIRED`/`VALIDATION_FAILED` | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `catalog_order.customer_order`, `catalog_order.payment_order`, `activation_consent.consent_record`, `activation_consent.service_activation`, `customer_iam.identity_profile` | 客户端价格/金额/支付成功/权益状态；provider 信息 |
| C11 | `listCustomerOrders` `GET /api/v1/customer/orders` | 当前账户/空间订单列表与支付状态（只读）；customerSession | session scope；cursor/limit；稳定 `createdAt desc, id desc` | GET | 200 `OrderPage` | `catalog_order.customer_order`, `catalog_order.payment_order`, `entitlement_metering.entitlement_issuance` | 其他账户订单、provider payload、权益内部细节 |
| C12 | `getCustomerOrder` `GET /api/v1/customer/orders/{orderId}` | 当前账户/空间订单详情与冻结快照；customerSession | server access；订单归属/快照；不重算当前目录 | GET | 200 `Order`；404 统一 | `catalog_order.customer_order`, `catalog_order.payment_order`, `activation_consent.consent_record` | 客户端可改价格/状态；provider 原文；他人订单 |
| C13 | `listCustomerEvidence` `GET /api/v1/customer/evidence` | 当前可访问主体的证据元数据列表；customerSession | server subject/workspace access；cursor/limit；不返回媒体 | GET | 200 `EvidencePage` | `evidence.evidence_record`, `evidence.evidence_artifact`, `evidence.recording` | object key/URL/bytes、完整号码、未共享证据 |
| C14 | `getCustomerEvidence` `GET /api/v1/customer/evidence/{evidenceId}` | 单条证据安全元数据/可用分享状态；customerSession | server access；SOLIDIFIED 等状态；不渲染媒体 | GET | 200 `EvidenceDetail`；404 统一 | `evidence.evidence_record`, `evidence.evidence_artifact`, `evidence.recording`, `evidence.media_object` | object key/URL/version/bucket、原始录音、分享秘密 |
| C15 | `createCustomerShare` `POST /api/v1/customer/shares` | 为 SOLIDIFIED 证据创建分享授权；customerSession | session account/workspace；evidence access/risk；approved sharing policy；服务端生成 token/code；冻结 content | body `creationRequestId` 1-128；deterministic `shareId` | 201 `ShareCreated`；409 `SHARE_CREATION_CONFLICT`；403 `SHARE_ACCESS_POLICY_UNAVAILABLE`/`SHARE_RISK_BLOCKED`；422 | `sharing.evidence_share_grant`, `sharing.share_access_event`, `evidence.evidence_record` | 后续读取不返回 token/code；object key/URL；他人证据 |
| C16 | `listCustomerShares` `GET /api/v1/customer/shares` | 当前账户/空间分享元数据列表；customerSession | server access；cursor/limit；`validFrom desc, id desc` | GET | 200 `SharePage` | `sharing.evidence_share_grant`, `sharing.share_access_event` | capability token/code/hash、object key、evidence 内部键（可返回 evidenceId 仅在创建者语境；见 §4.9） |
| C17 | `revokeCustomerShare` `POST /api/v1/customer/shares/{shareId}/revocations` | 撤销分享后续访问；customerSession | session creator/workspace；当前 grant status/generation；action request ID | `Idempotency-Key` 1-128 | 200 `ShareState`；409 `SHARE_TRANSITION_CONFLICT`/`SHARE_GENERATION_STALE`；410 `SHARE_REVOKED` | `sharing.evidence_share_grant`, `sharing.share_access_event` | token/code、被撤销前已签发凭证的召回承诺 |
| C18 | `rotateCustomerShareCode` `POST /api/v1/customer/shares/{shareId}/code-rotations` | 轮换提取码并解锁（显式 code rotation；`code_generation`/`revocation_generation` +1，`LOCKED→ACTIVE`）；customerSession | session creator/workspace；EXTRACTION_CODE mode；current generation；新 code 服务端生成 | `Idempotency-Key` 1-128 | 200 `ShareCodeRotation`（新 code 仅本次；**无 `capabilityToken`**）；409 `SHARE_TRANSITION_CONFLICT` | `sharing.evidence_share_grant`, `sharing.share_access_event` | 旧 code、`capabilityToken`、hash/salt、持久化明文 |

### 5.2 运营面 `/api/v1/operations/**`（21）

| # | operationId / 路径 | 目的与访问边界 | 权威服务端输入 | Idem | 成功/特有错误 | A0 | No-expose |
| --- | --- | --- | --- | --- | --- | --- | --- |
| O1 | `listOperationsSubjects` `GET /api/v1/operations/subjects` | 脱敏主体列表；employeeSession，`ADMIN`/`OPS` 均可只读 | employee session/role；仅结构化过滤 `subjectId?`/`source?`/`claimStatus?`/`activationStatus?`/`realNameStatus?`/`riskState?`（均为已暴露/A0 支持字段）；稳定 `createdAt desc, subjectId desc`；cursor 绑定规范过滤摘要；cursor/limit；**不支持完整号码或自由文本搜索** | GET | 200 `SubjectPage`；`CURSOR_INVALID`/`CURSOR_FILTER_MISMATCH`；403 role | `subject_number.business_subject`, `subject_number.caller_number_binding`, `activation_consent.service_activation`, `subject_number.subject_claim`, `subject_number.subject_risk_state` | 完整号码、证件、原始证据、provider subject/HMAC |
| O2 | `getOperationsSubject` `GET /api/v1/operations/subjects/{subjectId}` | 脱敏主体详情摘要：绑定/认领/风险/开通/试用/证据计数/订单计数 | server role/resource access；只读快照；不播放 | GET | 200 `OperationsSubjectDetail`；404 统一 | `subject_number.business_subject`, `subject_number.caller_number_binding`, `subject_number.subject_claim`, `subject_number.subject_risk_state`, `activation_consent.service_activation`, `entitlement_metering.trial_grant`, `entitlement_metering.entitlement_balance` | 原始录音/播放凭证、完整号码、他人主体细节 |
| O3 | `suspendOperationsSubject` `POST /api/v1/operations/subjects/{subjectId}/suspensions` | 运营暂停内部服务；employeeSession，`OPS` | employee identity/reason；subject activation/revision；只允许 ACTIVE→SUSPENDED | `Idempotency-Key` 1-128 = operations action event | 200 `ActivationTransition`；409 `ACTIVATION_STATE_CONFLICT`；403 | `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `audit.audit_fact` | 回滚历史、重发试用、客户/IVR 自行暂停 |
| O4 | `resumeOperationsSubject` `POST /api/v1/operations/subjects/{subjectId}/resumptions` | 运营恢复内部服务；employeeSession，`OPS` | 同上；只允许 SUSPENDED→ACTIVE；不发试用 | `Idempotency-Key` 1-128 | 200 `ActivationTransition`；409 `ACTIVATION_STATE_CONFLICT`；403 | `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `audit.audit_fact` | 重发试用/补量、客户/IVR 自行恢复 |
| O5 | `listOperationsActivations` `GET /api/v1/operations/activations` | 开通/同意/试用关联只读列表；employeeSession | server role；cursor/limit；稳定 `occurredAt desc, id desc` | GET | 200 `ActivationPage` | `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `activation_consent.consent_record`, `entitlement_metering.trial_grant` | 证件、原始录音、完整号码 |
| O6 | `listOperationsIvrSessions` `GET /api/v1/operations/ivr-sessions` | IVR 会话/通话元数据列表；employeeSession | server role；filters subjectId/callId/taskId/status；cursor/limit | GET | 200 `IvrSessionPage` | `ivr.inbound_session`, `ivr.call_record`, `ivr.outbound_call_task`, `ivr.ivr_decision` | 原始录音/播放权、明文主叫/被叫、raw event payload |
| O7 | `getOperationsIvrSession` `GET /api/v1/operations/ivr-sessions/{sessionId}` | 单会话的版本/按键/决策/通话/证据元数据链路；employeeSession | server role；只读；不返回媒体 | GET | 200 `IvrSessionDetail`；404 统一 | `ivr.inbound_session`, `ivr.call_record`, `ivr.call_leg`, `ivr.call_event`, `ivr.ivr_decision`, `evidence.evidence_record` | raw payload/ciphertext、完整号码、object key、播放凭证 |
| O8 | `listOperationsCatalogProducts` `GET /api/v1/operations/catalog/products` | 目录产品/版本/状态只读列表；employeeSession | server role；cursor/limit；`productCode asc, id asc` | GET | 200 `CatalogProductPage` | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line` | 未批准价格/DEMO、内部 SQL |
| O9 | `getOperationsCatalogProduct` `GET /api/v1/operations/catalog/products/{productId}` | 产品版本历史/在售/草稿/权益行只读详情 | server role；只读；本 operation 不写草稿 | GET | 200 `CatalogProductDetail` | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line` | 未批准值、本 operation 无写能力 |
| O10 | `publishOperationsCatalogVersion` `POST /api/v1/operations/catalog/versions/{versionId}/publications` | 发布 DRAFT 版本并退市旧 ACTIVE；employeeSession，`ADMIN` | employee identity；`expectedActiveVersionId`（可空）+ `expectedRevision`；候选 DRAFT/published_at null | state-conditional + deterministic Outbox | 200 `CatalogVersionState`；409 `CATALOG_PUBLICATION_CONFLICT`/`PUBLICATION_STALE_VIEW`；403 | `catalog_order.catalog_version`, `audit.audit_fact`, `platform.outbox_event` | 历史订单/权益改写、独立 offer 撤下、审批引擎 |
| O11 | `offsaleOperationsCatalogVersion` `POST /api/v1/operations/catalog/versions/{versionId}/offsales` | 在售版本下架，停止新订单；employeeSession，`ADMIN` | `expectedRevision`；ACTIVE/已 OFFSALE 幂等 | state-conditional | 200 `CatalogVersionState`；409 `CATALOG_PUBLICATION_CONFLICT`/`PUBLICATION_STALE_VIEW` | `catalog_order.catalog_version`, `audit.audit_fact`, `platform.outbox_event` | 历史订单/快照改写、`catalog_offer` 独立撤下 |
| O12 | `listOperationsTrialPolicies` `GET /api/v1/operations/trial-policies` | 试用策略版本/启用状态只读列表；employeeSession | server role；cursor/limit | GET | 200 `TrialPolicyPage` | `entitlement_metering.trial_policy_version` | 未批准额度/天数作为默认值、历史试用改写 |
| O13 | `publishOperationsTrialPolicy` `POST /api/v1/operations/trial-policies/{policyVersionId}/publications` | 发布试用策略版本；employeeSession，`ADMIN` | employee identity；`expectedEnabledPolicyVersionId`（可空）+ `expectedRevision`；候选 never-published | state-conditional + deterministic Outbox | 200 `TrialPolicyState`；409 `TRIAL_POLICY_PUBLICATION_CONFLICT`/`TRIAL_POLICY_STALE_VIEW`；403 | `entitlement_metering.trial_policy_version`, `audit.audit_fact`, `platform.outbox_event` | 重发/补量历史试用、发布者冒充、默认数值 |
| O14 | `disableOperationsTrialPolicy` `POST /api/v1/operations/trial-policies/{policyVersionId}/disables` | 停用当前试用策略；employeeSession，`ADMIN` | `expectedRevision`；enabled→false；已停用幂等 | state-conditional | 200 `TrialPolicyState`；409 `TRIAL_POLICY_PUBLICATION_CONFLICT`/`TRIAL_POLICY_STALE_VIEW` | `entitlement_metering.trial_policy_version`, `audit.audit_fact`, `platform.outbox_event` | 改变历史 grant 快照 |
| O15 | `listOperationsOrders` `GET /api/v1/operations/orders` | 订单/支付/发放状态只读列表；employeeSession | server role；cursor/limit；filter subjectId/paymentStatus | GET | 200 `OrderPage` | `catalog_order.customer_order`, `catalog_order.payment_order`, `entitlement_metering.entitlement_issuance` | 人工标记支付/退款/发票操作、provider payload |
| O16 | `getOperationsOrder` `GET /api/v1/operations/orders/{orderId}` | 订单冻结快照/支付/发放状态只读详情 | server role；只读 | GET | 200 `Order`；404 统一 | `catalog_order.customer_order`, `catalog_order.payment_order`, `entitlement_metering.entitlement_issuance` | provider 原文、客户端可改状态、退款/发票流程 |
| O17 | `listOperationsAuditFacts` `GET /api/v1/operations/audit` | 追加审计流水只读查询；employeeSession | server role；cursor/limit；stable `occurredAt desc, id desc`；redacted snapshots | GET | 200 `AuditFactPage` | `audit.audit_fact` | 未脱敏前后快照、客户/员工凭据、原始录音 |
| O18 | `createOperationsCatalogVersionDraft` `POST /api/v1/operations/catalog/versions` | 为已有不可变产品创建 DRAFT 目录版本候选；employeeSession，`ADMIN` | employee identity；`productId` 必须已存在；`versionNo` 正整数且在 product 内唯一；A0 类型/计费/发放矩阵；受益行；`channels`；服务端生成 token/snapshot；不接受 status/publishedAt | `state-conditional`：自然唯一 `(product_id, version_no)` + canonical 持久化内容投影；**无 `Idempotency-Key`** | 201 `CatalogVersionDraft`（首次）；200（同自然身份同投影重放）；409 `CATALOG_DRAFT_CONFLICT`；404；422 | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `operations_iam.employee_account`, `audit.audit_fact` | 审批引擎、独立 offer 撤下、已发布内容改写、客户端价格/状态 |
| O19 | `replaceOperationsCatalogVersionDraft` `PUT /api/v1/operations/catalog/versions/{versionId}/draft` | 仅替换 `DRAFT`/`published_at IS NULL` 候选的可变商业内容；employeeSession，`ADMIN` | employee identity；`expectedRevision`；只更新 version 可变内容与现有渠道 `version_token`；受益行/渠道集合不可变（A0 append-only）；不接受 status/publishedAt | `state-conditional`：`expectedRevision` + canonical requested/current 内容投影；**无 `Idempotency-Key`** | 200 `CatalogVersionDraft`（已应用或已应用重放）；409 `CATALOG_DRAFT_CONFLICT`/`CATALOG_DRAFT_STALE_VIEW`；404；422 | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `operations_iam.employee_account`, `audit.audit_fact` | 已发布内容、受益行改写、版本号/product 变更、独立 offer 撤下 |
| O20 | `createOperationsTrialPolicyDraft` `POST /api/v1/operations/trial-policies` | 创建 DRAFT/never-published 试用策略版本候选；employeeSession，`ADMIN` | employee identity；`versionNo` 正整数且 `(FIRST-TRIAL, versionNo)` 唯一；`grantedSeconds`/`validDays` 正数；服务端生成 snapshot；`enabled=false`、`publishedAt=null`。A0 `published_by_employee_id` 是 NOT NULL 的**实际发布者事实**，A0 未定义 draft 创建时的持久化取值，故运行时 DRAFT 插入存在前置条件 `R-A0-O20-PUBLISHER`（owner：A0 schema/catalog-order/entitlement-metering）。本 operation 只定义公共契约，不暴露、不重命名该字段，不声称运行时插入已解决；不设默认额度/天数 | `state-conditional`：自然唯一 `(campaign_key = FIRST-TRIAL, version_no)` + canonical 持久化内容投影；**无 `Idempotency-Key`** | 201 `TrialPolicyDraft`（首次）；200（同自然身份同投影重放）；409 `TRIAL_POLICY_DRAFT_CONFLICT`；422 | `entitlement_metering.trial_policy_version`, `operations_iam.employee_account`, `audit.audit_fact` | 重发/补量历史试用、把发布者事实误标为 draft creator、默认数值、客户端 enabled/publishedAt/publisher |
| O21 | `replaceOperationsTrialPolicyDraft` `PUT /api/v1/operations/trial-policies/{policyVersionId}/draft` | 仅替换 DRAFT/never-published 试用策略候选的可变内容；employeeSession，`ADMIN` | employee identity；`expectedRevision`；grantedSeconds/validDays/snapshot 可更新；`campaign_key`/`version_no` 不可变；published 后拒绝；不写 publisher/publishedAt/status 字段 | `state-conditional`：`expectedRevision` + canonical requested/current 内容投影；**无 `Idempotency-Key`** | 200 `TrialPolicyDraft`（已应用或已应用重放）；409 `TRIAL_POLICY_DRAFT_CONFLICT`/`TRIAL_POLICY_DRAFT_STALE_VIEW`；404；422 | `entitlement_metering.trial_policy_version`, `operations_iam.employee_account`, `audit.audit_fact` | 已发布内容、历史 grant 快照、enabled/publishedAt/publisher 客户端写入 |

### 5.3 自研 IVR 面 `/api/v1/ivr/**`（6）

| # | operationId / 路径 | 目的与访问边界 | 权威服务端输入 | Idem | 成功/特有错误 | A0 | No-expose |
| --- | --- | --- | --- | --- | --- | --- | --- |
| I1 | `resolveIvrInbound` `POST /api/v1/ivr/inbound/resolve` | 建立/复用呼入会话；原子领取唯一任务、预占并冻结授权，返回动作；ivrMtls+ivrSignature | 可信线路主叫、规范接入号、node/protocol；session/call/task/activation/entitlement/access；callId 幂等 | `callId + INBOUND_RESOLVE` 指纹 | 200 `IvrDecision`（purpose `INBOUND_RESOLVE`；含 `REJECT_CALL_IN_PROGRESS`、`REJECT`/`TASK_CONFLICT` 等正常领域结果）；400 `IVR_ARGUMENT_INVALID`；409 仅 `SESSION_IDENTITY_MISMATCH`/身份或指纹冲突（无法冻结决策） | `ivr.inbound_session`, `ivr.call_record`, `ivr.outbound_call_task`, `ivr.ivr_decision`, `entitlement_metering.usage_reservation`, `activation_consent.service_activation`, `entitlement_metering.entitlement_balance` | 除冻结授权外不返回完整 callee；不返回账户/支付/证据状态；不按最新被叫回退 |
| I2 | `confirmIvrActivation` `POST /api/v1/ivr/calls/{callId}/activation-confirmations` | 处理 `GUIDE_ACTIVATION` 的提示词/按键确认并返回同一 `callId` 的再决策；ivrMtls+ivrSignature | 原 resolve 决策/会话、提示/政策版本/内容、显式 `1`、当前任务/权益/访问；不得重新 resolve | body `eventId` 1-128 + `callId`；同指纹 replay | 200 `IvrDecision`（purpose `ACTIVATION_NEXT_ACTION`；含 `GUIDE_ACTIVATION`/`CONSENT_REQUIRED`、`REJECT`/`CONFIRMATION_NOT_ALLOWED`、后续 `DIAL_TASK_CALLEE`/`ENTER_CALLEE`/`GUIDE_PURCHASE`/`REJECT` 等正常领域结果）；400；422；409 仅 `SESSION_IDENTITY_MISMATCH`/身份或指纹冲突 | `activation_consent.consent_record`, `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `entitlement_metering.trial_grant`, `ivr.ivr_decision`, `entitlement_metering.usage_reservation` | 未确认不新开通；不返回账户/owner；callee 仅冻结授权内部 payload |
| I3 | `authorizeIvrDirectOutbound` `POST /api/v1/ivr/calls/{callId}/outbound-authorizations` | 仅无任务 `ENTER_CALLEE` 路径的直接 DTMF 被叫确认与预占授权；ivrMtls+ivrSignature | call/session/purpose chain、无 task、可信主叫、主体/开通/同意/权益；服务端规范化输入 callee | body `eventId` 1-128 + `callId`；同指纹 replay | 200 `IvrDecision`（purpose `DIRECT_AUTHORIZATION`；成功 action null/`DIRECT_AUTHORIZED`，非外呼结果 `GUIDE_PURCHASE`/`REJECT`/`REJECT_SUSPENDED`/`REJECT_CALL_IN_PROGRESS`，含 `CONFIRMATION_NOT_ALLOWED`/`CONSENT_REQUIRED` reasonCode）；400；422；409 仅 `SESSION_IDENTITY_MISMATCH`/身份或指纹冲突 | `ivr.call_record`, `ivr.ivr_decision`, `entitlement_metering.usage_reservation`, `activation_consent.service_activation`, `entitlement_metering.entitlement_balance` | 非空 taskId/任务回退；返回 callee 仅授权内部；不返回账户 |
| I4 | `ingestIvrCallEvent` `POST /api/v1/ivr/calls/{callId}/events` | 追加/收敛外呼、应答、桥接、录音、挂断和 `OUTBOUND_NOT_STARTED` 事实；ivrMtls+ivrSignature | 已知父 call/session/leg；eventId/type/sequence/occurredAt/payload；服务端 fingerprint/时长校验/结算 | `(IVR_EVENT,eventId)` 指纹 | 200 `CallEventReceipt`（APPLIED/QUARANTINED/IGNORED）；404 `CALL_UNKNOWN`；409 `EVENT_ID_CONFLICT`；400 | `ivr.call_event`, `ivr.call_record`, `ivr.call_leg`, `entitlement_metering.usage_reservation`, `entitlement_metering.usage_fact` | raw payload/ciphertext、完整号码、内部 error detail；未知 call 不建占位 |
| I5 | `declareIvrRecording` `POST /api/v1/ivr/calls/{callId}/recordings` | 登记 recordingId、声明格式/大小/时长/hash，签发单对象短时上传指令；ivrMtls+ivrSignature | 已知 call；recordingId 唯一；声明 fact 仅声明；服务端生成 upload expiry/credential | `recordingId` 唯一 | 201 `RecordingDeclaration`；409 `RECORDING_IDENTITY_CONFLICT`；404 `CALL_UNKNOWN`；422 | `evidence.recording`, `evidence.media_object`（后续观察） | object key/bucket/version、永久 URL、服务端未验证事实、凭证写入日志 |
| I6 | `completeIvrRecording` `POST /api/v1/ivr/calls/{callId}/recordings/{recordingId}/complete` | 通知上传完成，触发服务端对象/媒体校验；ivrMtls+ivrSignature | recordingId、当前 status、服务器观察到的对象事实；不信任客户端 hash/version | `recordingId`；同身份同事实返回首次 | 200 `RecordingCompletion`（VALIDATED/QUARANTINED）；409 `UPLOAD_OBSERVATION_REQUIRED`/`RECORDING_IDENTITY_CONFLICT`/`UPLOAD_EXPIRED`；503 `STORAGE_UNKNOWN` | `evidence.recording`, `evidence.media_object`；证据记录/固化由后续 T-S3-02 owner 流程处理，不在本 operation 事务内 | object key/version/bucket、raw bytes、音频 URL；`complete` 不代表证据已创建或固化 |

### 5.4 公开分享面 `/api/v1/public/shares/**`（3）

| # | operationId / 路径 | 目的与访问边界 | 权威服务端输入 | Idem | 成功/特有错误 | A0 | No-expose |
| --- | --- | --- | --- | --- | --- | --- | --- |
| P1 | `getPublicShare` `GET /api/v1/public/shares/{capabilityToken}` | 展示分享页安全元数据；能力令牌 + share 当前状态/模式/过期；页面打开/预取不消耗准入；`ORGANIZATION_MEMBER` 未认证时只返回验证/挑战元数据并省略 `content` | token digest 解析；status/time/generation；verification mode；当前认证账户/组织/成员匹配后才投影 content | GET；**observation-free**：不写 `PAGE_OPENED`/`PREFETCH_IGNORED`/任何 `share_access_event` | 200 `PublicShareView`（`ORGANIZATION_MEMBER` 无匹配会话：`content` 省略 + `verificationChallenge{customerSessionRequired: true}`）；404 `SHARE_TOKEN_INVALID` 统一；410 `SHARE_REVOKED`/`SHARE_EXPIRED`（有效令牌但已撤销/过期） | `sharing.evidence_share_grant`, `evidence.evidence_record` | shareId/evidenceId/subjectId、对象键/URL、token/hash、未共享内容；未认证时不得返回 content |
| P2 | `createPublicShareAdmission` `POST /api/v1/public/shares/{capabilityToken}/admissions` | 显式准入：按模式验证并原子计数一次，提交后签发短时媒体凭证；public capability + 可选 customerSession（member） | token digest；`admissionRequestId`；验证 proof（code/mobile possession/member session）；grant status/time/generation/limit；risk；ticket policy | body `admissionRequestId` 1-128 → admission key | 201 `ShareAdmission`；403 `SHARE_VERIFICATION_FAILED`/`SHARE_RISK_BLOCKED`/`SHARE_WRONG_TENANT`；409 `SHARE_LOCKED`/`SHARE_ADMISSION_LIMIT_REACHED`；410 `SHARE_SESSION_EXPIRED`；503 `SHARE_SESSION_POLICY_UNAVAILABLE`/`DEPENDENCY_UNAVAILABLE`（准入已提交但 ticket 签发失败：失败关闭，计数不变；同 `admissionRequestId` 重放重发票据、不重新计数） | `sharing.share_admission`, `sharing.share_access_event`, `sharing.evidence_share_grant`, `subject_number.subject_risk_state` | code/token/hash/phone/OTP、object key/URL、未共享证据；NONE 不记录为已验证身份 |
| P3 | `authorizePublicShareMedia` `POST /api/v1/public/shares/{capabilityToken}/admissions/{admissionId}/media-authorizations` | 媒体凭证签发/重建控制面：为已准入会话签发播放/定位/续签短时凭证，由媒体网关校验；public capability + 可选 customerSession（member） | token；admission witness；当前 grant status/time/generation；risk；member 当前会员/账户/组织；purpose；`ACTIVE` 或已准入 `LOCKED` | body `authorizationRequestId` 1-128；确定性 access identity | 200 `MediaAuthorization`；403 `SHARE_RISK_BLOCKED`/`SHARE_WRONG_TENANT`；409 `SHARE_GENERATION_STALE`；410 `SHARE_SESSION_EXPIRED`；503 | `sharing.share_access_event`, `sharing.share_admission`, `sharing.evidence_share_grant`, `subject_number.subject_risk_state` | permanent URL/object key/bytes；Redis ticket 结构；已缓冲字节的远程召回承诺 |

**固定请求字段（分享）**：
- `createCustomerShare` body：`creationRequestId`(1-128)、`evidenceId`、`verificationMode`(四枚举)、`expiresAt`、`maxAdmissions`(nullable，positive)、`downloadAllowed`、`designatedMobile`(仅 DESIGNATED_MOBILE，受保护输入)；不接收 creator/workspace/content/token/code。
- `createPublicShareAdmission` body：`admissionRequestId`(1-128)、`verification` 判别联合 `{mode: NONE}` | `{mode: EXTRACTION_CODE, code}` | `{mode: DESIGNATED_MOBILE, verificationProofId}` | `{mode: ORGANIZATION_MEMBER}`；仅 `ORGANIZATION_MEMBER` 要求已认证 `customerSession`；`code` 使用 `writeOnly: true`。
- `authorizePublicShareMedia` body：`authorizationRequestId`(1-128)、`purpose`(`PLAYBACK|SEEK|RENEW`)；返回 `mediaAuthorization {credential, expiresAt}`，P3 不携带当前凭证，credential 由媒体网关校验；`ORGANIZATION_MEMBER` 重发绑定已准入 recipient witness；`LOCKED` 允许已准入有效会话，只阻止 P2 新准入。
- `getPublicShare`（P1）：GET 无 body。`ORGANIZATION_MEMBER` 允许携带 `customerSession`；缺失或不匹配时响应只含 `verificationMode`、`status`、`expiresAt`、`downloadAllowed` 和 `verificationChallenge{customerSessionRequired: true}`，**不得含 `content`**；`NONE`/`EXTRACTION_CODE`/`DESIGNATED_MOBILE` 页面行为保持不变。P1 仍是 observation-free，不写 `share_access_event`。

### 5.5 认证供应商回调面 `/api/v1/callbacks/**`（1）

| # | operationId / 路径 | 目的与访问边界 | 权威服务端输入 | Idem | 成功/特有错误 | A0 | No-expose |
| --- | --- | --- | --- | --- | --- | --- | --- |
| K1 | `receivePaymentProviderCallback` `POST /api/v1/callbacks/payment-events` | 接收已验签支付事件，幂等推进 payment/order，不向供应商暴露内部结果；providerCallbackMtls+providerCallbackSignature | 供应商证书/签名/时间窗/nonce；适配层规范化 provider event identity；服务端验签；payload 加密留档 | 供应商事件身份 + payload 指纹 | 200 `CallbackAcknowledgement`（`accepted: true`, `duplicate`）；401 `CALLBACK_SIGNATURE_INVALID`；409 `PAYMENT_EVENT_CONFLICT`；422；503 `CALLBACK_RETRY_LATER` | `catalog_order.payment_event`, `catalog_order.payment_order`, `catalog_order.customer_order`, `platform.outbox_event` | provider payload/transaction details、内部错误、客户/员工身份、权益发放内部状态 |

### 5.5.1 端点清单版本化审计（F-09）

`servers` 保持为必填且恰好一项相对根 `url: /`；因此 §5.1-§5.5 的全部 operation 路径必须写成完整的 `/api/v1/...` 绝对路径，不能依赖 base URL 前缀。对 49 行清单逐行提取 `method + path` 执行以下静态审计（本计划为文档层审计；实施后同一断言必须对 OpenAPI `paths` 重新执行）：

1. **前缀完整**：每条路径以 `/api/v1/` 开头，并且只允许五个批准前缀：`/api/v1/customer/`、`/api/v1/operations/`、`/api/v1/ivr/`、`/api/v1/public/shares/`、`/api/v1/callbacks/`。不存在缺少 `/api/v1` 前缀的 customer、operations、ivr、public-share 或 callback operation 路径。
2. **唯一归属**：每条路径恰好匹配一个面前缀；五个前缀互不为前缀，因此归属判定是单值，禁止一路径多面。
3. **精确计数**：customer=18、operations=21、ivr=6、public-share=3、provider-callback=1，总计 49；operationId 与 `method + path` 均不得重复。
4. **servers 精确**：顶层 `servers` 必须存在且恰好一项相对 `url: /`（§4.1）；缺失、绝对 URL 或额外 server 根均失败；不以 base URL 前缀替代路径版本。
5. **无未版本化示例**：正文中的所有 `METHOD /...` 路由示例（如 `GET /api/v1/operations/evidence`）同样使用 `/api/v1` 前缀。

审计方法：用 `node`/`python` 正则提取本计划清单中的 `` `(GET|POST|PUT|PATCH|DELETE) (/[^`]+)` ``，按前缀分组计数并列出不匹配项；期望输出 `18/21/6/3/1/49`、unversioned=0、duplicate=0、multi-surface=0。

### 5.6 契约组件字典（关键字段）

以下为 components 的最小字段集；字段长度/枚举/可空性以 A0 列定义和本计划 §4.10 为准。所有 response 对象 `additionalProperties: false`，敏感字段按 §4.9 投影。

| Component | 关键字段（服务端返回/请求） | 来源/约束 |
| --- | --- | --- |
| `CustomerContext` | `account{accountId, displayName?, status}`, `workspaces[{workspaceType: PERSONAL|ORGANIZATION, organizationId?, role?, status}]`, `channel: MINIAPP|WEB` | 服务端派生；无 provider subject/HMAC/证件 |
| `BusinessNumber` | `bindingId`, `subjectId`, `numberMasked`, `numberType: MOBILE|LANDLINE`, `bindingStatus`, `realNameStatus` | A0 `caller_number_binding`/`business_subject`；无完整号码 |
| `CustomerSubjectDetail` | `subjectId`, `numberMasked?`, `activationStatus`, `trial{grantedSeconds, remainingSeconds, expiresAt, status}`, `consentRequirements{policyCode, policyVersion, promptVersion, contentSha256}`, `evidenceCount`, `orderCount` | A0 activation/trial/balance/consent；不返回风险内部细节 |
| `ClaimResult` | `claimId`, `status: ACCEPTED|REJECTED|CONFLICT`, `subjectId`, `acceptedAt?` | A0 `subject_claim`；`request_id` 来自 header |
| `ActivationResult` | `activationStatus`, `activationRevision`, `transitionId`, `consentId`, `trialOutcome: GRANTED|REPLAY|ALREADY_GRANTED`, `trial{grantedSeconds, expiresAt}` | A0 consent/transition/trial；不重复发放 |
| `OutboundCallTask` | `taskId`, `status`, `maskedCaller`, `maskedCallee`, `accessNumberMasked`, `ttlSeconds`, `serverTime`, `expiresAt`, `reasonCode?` | A0 task；不返回明文 callee/历史 callee |
| `CatalogOffer` | `productCode`, `catalogVersionId`, `catalogOfferId`, `channel`, `versionNo`, `offerType`, `audience`, `priceCents|null`, `currency`, `billingCycle`, `installments`, `benefitLines[{entitlementType, quantity, validityDays, grantIntervalMonths}]`, `versionToken` | A0 catalog；价格/权益为已批准存储值，契约不内置 DEMO |
| `Order` | `orderId`, `orderNo`, `subjectId`, `channel`, `catalogVersionId`, `catalogOfferId`, `amountCents`, `currency`, `paymentStatus`, `issuanceStatus`(computed projection), `createdAt`, `paidAt?`, `closedAt?`, `snapshotSha256?` | A0 order/payment/issuance；金额/状态服务端派生；不返回 provider 原文 |
| `EvidenceDetail` | `evidenceId`, `subjectId`, `status`, `sourceType`, `capturedAt`, `consentVersion`, `artifacts[{artifactRole, mimeType, sizeBytes, validatedDurationSeconds, sha256?}]` | A0 evidence safe projection；无 object key/version/URL/bytes |
| `ShareGrant/ShareState` | `shareId`, `evidenceId?`(仅创建者语境), `verificationMode`, `status`, `validFrom`, `expiresAt`, `maxAdmissions`, `admissionsUsed`, `downloadAllowed`, `codeGeneration?`, `revocationGeneration` | A0 share grant；秘密/哈希/盐不返回 |
| `ShareCreated` | `ShareGrant` 字段 + `capabilityToken`(readOnly, 仅创建时一次) + `extractionCode?`(readOnly, 仅 code 模式一次) | A0 share creation；`x-onecall-return-once`；无 `ShareCodeRotation` 复用 |
| `ShareCodeRotation` | `shareId`, `verificationMode`, `status`, `codeGeneration`, `revocationGeneration`, `extractionCode`(readOnly, 仅本次) | A0 T-S3-03 code rotation；**不得包含 `capabilityToken`**；生成类型断言强制 |
| `PublicShareView` | `verificationMode`, `status`, `expiresAt`, `downloadAllowed`, `content?{capturedAt, validatedDurationSeconds, artifacts[{artifactRole, mimeType, sizeBytes, sha256?}]}`, `verificationChallenge?{customerSessionRequired}` | A0 share projection；无 shareId/evidenceId/subjectId/object；P1 不写 observation；`ORGANIZATION_MEMBER` 无匹配会话时 `content` 必须省略 |
| `ShareAdmission` | `admissionId`, `verificationMode`, `verificationResult`, `admittedAt`, `sessionExpiresAt`, `admissionExpiresAt`, `mediaAuthorization{credential(readOnly, x-onecall-log-redaction: required), expiresAt}` | A0 admission/access event；`NONE` result = `NOT_REQUIRED`；ticket 失败不改变 admission/计数 |
| `MediaAuthorization` | `credential`(opaque, `readOnly: true`, `x-onecall-log-redaction: required`), `expiresAt` | A0 T-S3-05；P3 返回、媒体网关校验；凭证格式/TTL deferred Q22；非 return-once |
| `SubjectPage/OperationsSubjectDetail` | 列表：`subjectId`, `maskedNumber`, `source`, `claimStatus`, `activationStatus`, `realNameStatus`, `riskState`, `createdAt`；详情另含 `bindings[]`, `claims[]`, `activation`, `trial`, `evidenceCount`, `orderCount` | A0 subject/binding/claim/risk/activation；无原始证据 |
| `ActivationTransition` | `transitionId`, `statusEventId`, `fromStatus`, `toStatus`, `fromRevision`, `toRevision`, `sourceChannel`, `sourceActionDomain`, `reasonCode`, `occurredAt` | A0 transition；不返回客户/员工凭据 |
| `IvrSessionDetail` | `sessionId`, `callId`, `status`, `nodeId`, `protocolVersion`, `promptVersion`, `decisions[]`(safe), `call{status, endedAt?, authoritativeSeconds?}`, `evidence[{evidenceId, status, artifactRoles}]` | A0 IVR/evidence safe metadata；无 raw payload/播放凭证 |
| `CatalogProductDetail` | `productId`, `productCode`, `productFamily`, `versions[{catalogVersionId, versionNo, saleStatus, offerType, audience, priceCents|null, currency, billingCycle, installments, benefitLines[], publishedAt?}]` | A0 catalog；只读；草稿写仅由 O18/O19 提供 |
| `CatalogVersionState/TrialPolicyState` | 版本/策略 ID、状态、`revision`、`publishedAt?`、`currentActiveVersionId?`/`currentEnabledPolicyVersionId?` | A0 publication；stale 返回 Problem |
| `CatalogVersionDraft` | `catalogVersionId`, `productId`, `versionNo`, `saleStatus: DRAFT`, `offerType`, `audience`, `priceCents|null`, `currency`, `billingCycle`, `installments`, `grantIntervalMonths`, `benefitLines[]`, `channels[]`, `revision`, `createdAt`, `publishedAt: null` | A0 `catalog_version`/`catalog_offer`/`catalog_benefit_line`；服务端 token/snapshot；不返回内部 snapshot |
| `TrialPolicyDraft` | `policyVersionId`, `campaignKey: FIRST-TRIAL`, `versionNo`, `grantedSeconds`, `validDays`, `enabled: false`, `publishedAt: null`, `revision`, `createdAt` | A0 `trial_policy_version`；never-published；不设默认额度/天数；不暴露 `published_by_employee_id`；运行时插入受 `R-A0-O20-PUBLISHER` 前置约束 |
| `AuditFact` | `auditId`, `occurredAt`, `actorType`, `actorId?`, `actionCode`, `objectType`, `objectId`, `reasonCode?`, `beforeSnapshot?`, `afterSnapshot?`, `traceId` | A0 audit；snapshots 已脱敏 |
| `IvrDecision` | 见 §6.1；`task.calleeNumber`/`authorization.calleeNumber` 仅内部加密响应 | A0 §10.14；redacted snapshot 不含明文 callee |
| `CallEventReceipt` | `callId`, `eventId`, `processingStatus: APPLIED|QUARANTINED|IGNORED`, `reasonCode?`, `receivedAt` | A0 call_event；首次终态吸收 |
| `RecordingDeclaration` | `recordingId`, `status: DECLARED`, `uploadExpiresAt`, `upload{credential(readOnly, x-onecall-log-redaction: required), expiresAt}` | A0 recording；无 object key/URL；非 return-once |
| `RecordingCompletion` | `recordingId`, `status: VALIDATED|QUARANTINED`, `validatedDurationSeconds?`, `validatedSha256?`, `reasonCode?` | A0 recording；不返回证据/媒体 URL |
| `CallbackAcknowledgement` | `accepted: true`, `duplicate: boolean` | 支付回调；不返回内部 payment/entitlement ID |
| 公共 | `Problem`, `ProblemCode`, `FieldProblem`, `PageInfo{cursor, nextCursor, hasMore}`, `Money{amountCents, currency: CNY}`, `IdempotencyKey`, `OpaqueId` | §4.5、§4.8、§4.10 |

## 6. IVR 动作契约边界（不设计后续详细协议）

### 6.1 共享决策对象

`IvrDecision` 是 `INBOUND_RESOLVE`、`ACTIVATION_NEXT_ACTION`、`DIRECT_AUTHORIZATION` 三个 purpose 共用的 response schema，字段严格来自 A0 §10.14：

`decisionId`、`callId`、`purpose`、`action`（七动作或 direct success 的 null）、`callerState`、`taskMatch`、`reasonCode`、`availableRecordingSeconds`、`balanceAsOf`、`task {taskId, businessType, calleeMasked, calleeNumber?}`、`reservationId`、`authorizedRecordingSeconds`、`authorizationExpiresAt`、`decisionAt`。

- `action` 只允许七个 A0 动作：`GUIDE_ACTIVATION`、`DIAL_TASK_CALLEE`、`ENTER_CALLEE`、`GUIDE_PURCHASE`、`REJECT`、`REJECT_SUSPENDED`、`REJECT_CALL_IN_PROGRESS`；`DIRECT_AUTHORIZATION` 是 purpose，不是第八动作。
- `task.calleeNumber` 只在 `action = DIAL_TASK_CALLEE` 的加密内部响应中出现；`authorization.calleeNumber` 只在 direct success 的加密内部响应中出现。OpenAPI 用 `readOnly: true` + `x-onecall-internal-payload: true` + `x-onecall-log-redaction: required` 标记（它们是响应字段，不用 `writeOnly`），redacted snapshot 不包含。
- `reservationId`/`authorizedRecordingSeconds`/`authorizationExpiresAt` 只在冻结授权存在时非空；`authorizationExpiresAt` 是最后一个可启动 B 路外呼的时刻，不是通话截止，也不因 replay 续期。
- **HTTP/decision 映射**：`IvrDecision` 的正常领域结果一律 `200`，包括 `REJECT_CALL_IN_PROGRESS`、`REJECT`/`TASK_CONFLICT`、`GUIDE_ACTIVATION`/`CONSENT_REQUIRED`、`REJECT`/`CONFIRMATION_NOT_ALLOWED` 及 direct authorization 的成功/非外呼结果。`409 Problem` 仅用于 session/identity/access-number/fingerprint 冲突等**无法冻结决策**的请求失败；`TASK_CONFLICT`/`CALL_IN_PROGRESS`/`TASK_CLAIMED`/`TASK_UNAVAILABLE`/`CONFIRMATION_NOT_ALLOWED` 不是 Problem code。客户待拨任务面使用独立 `OUTBOUND_CALL_IN_PROGRESS`/`OUTBOUND_TASK_CONFLICT`/`OUTBOUND_TASK_NOT_CANCELLABLE`，不得复用上述 reason。
- 未知 action/purpose/reason/taskMatch 一律失败关闭，不默认 `DIAL_TASK_CALLEE`。

### 6.2 任务路径与直接试用路径

| 场景 | 初始 resolve 动作 | 任务 | 权益/授权 | 后续 |
| --- | --- | --- | --- | --- |
| 已开通、有权益、存在当前主叫唯一可用 `PENDING` 任务 | `DIAL_TASK_CALLEE` | 同一事务条件领取并绑定 `callId`；唯一非终态任务；30 秒冻结 `expiresAt` | 同一事务创建唯一 `UsageReservation` 并冻结授权；提交后才返回 | IVR 按任务冻结被叫自动外呼，不要求主叫确认被叫；无最新被叫回退 |
| 已开通、有权益、无有效任务 | `ENTER_CALLEE` | 不领取；不猜测过期/历史任务 | 无预占；进入独立 DTMF 输入与确认流程 | 只有后续 `authorizeIvrDirectOutbound` 才创建/复用预占 |
| 已开通但无可用权益 | `GUIDE_PURCHASE` | 不领取 | 无预占 | 只播放去小程序购买引导，不创建订单/不声明支付 |
| 未建立主体或未开通 | `GUIDE_ACTIVATION` | 不领取 | 未开通前无预占 | 版本化提示 + 明确按键 `1` 后调用 `confirmIvrActivation`；确认后可再次决策 |
| 主体停用 | `REJECT_SUSPENDED` | 不领取；可标记任务 REJECTED | 无 | 提示停用与线下客服 |
| 同一主叫已有活动通话 | `REJECT_CALL_IN_PROGRESS` | 不领取 | 无 | 失败关闭，不串话 |
| 主叫缺失/不可信/格式非法 | `REJECT` | 不领取 | 无 | 不录音取证 |
| 任务数据异常/多个候选 | `REJECT`（`TASK_CONFLICT`） | 不选择 | 无 | 告警，不由 IVR 选一个 |

**确认后的再决策**：`confirmIvrActivation` 只处理原 `INBOUND_RESOLVE = GUIDE_ACTIVATION` 的同一 `callId`；它在同一事务完成开通/试用，然后重新检查任务与权益。若任务仍有效且所有条件通过，返回新的 `ACTIVATION_NEXT_ACTION = DIAL_TASK_CALLEE`；否则返回 `ENTER_CALLEE`（无任务且有权益）、`GUIDE_PURCHASE`（无权益）或 `REJECT`。确认内容过期/缺失时冻结 `GUIDE_ACTIVATION` + `CONSENT_REQUIRED`；确认在领域上不被允许时冻结非外呼 `REJECT` + `CONFIRMATION_NOT_ALLOWED`，不产生开通/试用副作用。以上均为 `200 IvrDecision`；只有身份/指纹冲突等无法冻结决策的请求失败才是 409 Problem。它不能把原 resolve 决策改写，也不能跳过领取/预占的原子性。

**无任务直接试用**：未注册/未实名的可信主叫可走 `GUIDE_ACTIVATION → 确认 → 首次试用`，不需要客户账户、登录身份、企业成员或实名；若之后无任务且有权益，走 `ENTER_CALLEE` + 直接 DTMF 授权。该路径不创建虚假登录身份，不改变证据归属，也不因未实名阻止试用录音（实名仅约束普通客户购买）。

### 6.3 失败/过期/无权利结果

- 任务过期：`taskMatch = EXPIRED`/`NO_VALID_TASK` 语义；不返回历史被叫，不领取，不外呼; 返回 `200 IvrDecision`（非外呼 action）。
- 任务已领取/替换/并发：`CLAIMED`/`TASK_CLAIMED`/`TASK_CONFLICT`；不覆盖已领取任务，不切换到另一任务; 返回 `200 IvrDecision`（`REJECT`/`TASK_CONFLICT`）。
- 无权益：`NO_AVAILABLE_ENTITLEMENT` → `GUIDE_PURCHASE`；不预占、不创建订单；`200 IvrDecision`。
- 访问撤销/账号/组织/绑定停用：`CALLER_BINDING_REVOKED`/`WORKSPACE_ACCESS_REVOKED`/`TASK_ACCESS_REVOKED`/`ACCOUNT_DISABLED`/`ORGANIZATION_INACTIVE` → `REJECT`；不做 post-commit 放宽；`200 IvrDecision`。
- 同一主叫其他活动通话：`REJECT_CALL_IN_PROGRESS`；`200 IvrDecision`，不领取/不预占，不开第二个 call。
- 确认不允许/当前同意缺失：`REJECT`/`CONFIRMATION_NOT_ALLOWED` 或 `GUIDE_ACTIVATION`/`CONSENT_REQUIRED`；`200 IvrDecision`，不产生未授权开通/试用。
- direct authorization 非外呼结果：`GUIDE_PURCHASE`/`REJECT`/`REJECT_SUSPENDED`/`REJECT_CALL_IN_PROGRESS`（含 `CONFIRMATION_NOT_ALLOWED`/`CONSENT_REQUIRED` reasonCode）；`200 IvrDecision`，无 reservation/授权。
- 授权截止未启动 B 路：IVR 通过事件端点上报 `OUTBOUND_NOT_STARTED`/`AUTHORIZATION_EXPIRED`；后台幂等失败终结任务并释放预占；若线路结果未知，不提交该终态，进入对账。
- 已启动 B 路但截止后接通：按冻结授权秒数继续，不因截止时间中断或重新授权。
- 任何 `REJECT`/`GUIDE_*` 都不得被 IVR 解释为“可以外呼”；只有 `DIAL_TASK_CALLEE` 或 direct success 携带授权。

### 6.4 明确 deferred

详细 IVR 会话状态、重试/心跳、B 路本地唯一键、媒体上传分片、签名串、错误重试窗口、超时/SLO、事件 type/payload schema version 属后续 IVR 协议（Q7/Q8/Q15/Q26/Q28）。A1 只固定：共享决策 schema、七动作、三 purpose、`callId`/`eventId` 幂等、任务/直拨边界、授权截止语义和稳定错误/原因码。

## 7. Schema 到 A0 映射、开放输入与残余风险

### 7.1 Schema-to-A0 映射

**A0 名称基线**：以下映射只使用 `docs/14` 44 表 catalog 中的精确 schema-qualified 名。A1 实际引用的完整集合是：`platform.idempotency_record`、`platform.outbox_event`、`audit.audit_fact`、`customer_iam.customer_account`、`customer_iam.identity_profile`、`organization_access.organization`、`organization_access.organization_membership`、`operations_iam.employee_account`、`subject_number.business_subject`、`subject_number.caller_number_binding`、`subject_number.subject_claim`、`subject_number.subject_risk_state`、`catalog_order.catalog_product`、`catalog_order.catalog_version`、`catalog_order.catalog_offer`、`catalog_order.catalog_benefit_line`、`catalog_order.customer_order`、`catalog_order.payment_order`、`catalog_order.payment_event`、`ivr.outbound_call_task`、`ivr.inbound_session`、`ivr.call_record`、`ivr.call_leg`、`ivr.call_event`、`ivr.ivr_decision`、`entitlement_metering.trial_policy_version`、`entitlement_metering.entitlement_grant`、`entitlement_metering.trial_grant`、`entitlement_metering.entitlement_balance`、`entitlement_metering.entitlement_issuance`、`entitlement_metering.usage_reservation`、`entitlement_metering.usage_fact`、`activation_consent.consent_record`、`activation_consent.service_activation`、`activation_consent.service_activation_transition`、`evidence.media_object`、`evidence.recording`、`evidence.evidence_record`、`evidence.evidence_artifact`、`evidence.evidence_event`、`sharing.evidence_share_grant`、`sharing.share_admission`、`sharing.share_access_event`。`customer_iam.auth_identity` 仅属于认证 session 边界（Q19），不是本契约的业务投影；44 表中的其余 43 张均已在上列集合中显式出现。任何短名、通配符、别名或错误 schema 都使 `check-doc-map` 失败。

| 契约模型组 | 主要 A0 表 | A0 事务/状态 | 说明 |
| --- | --- | --- | --- |
| `CustomerContext`, `BusinessNumber`, `CustomerSubjectDetail` | `customer_iam.customer_account`, `organization_access.organization`, `organization_access.organization_membership`, `subject_number.business_subject`, `subject_number.caller_number_binding`, `activation_consent.service_activation`, `activation_consent.consent_record`, `entitlement_metering.trial_grant`, `entitlement_metering.entitlement_balance`, `subject_number.subject_claim` | T-S1-04、T-S1-06、T-S1-07、§7.1 状态机 | 服务端派生账户/空间/角色；号码只脱敏；主体归属不转移 |
| `ClaimRequest/ClaimResult` | `subject_number.subject_claim`, `subject_number.business_subject`, `subject_number.caller_number_binding`, `subject_number.subject_risk_state` | T-S1-06、§9.6、§7.1 | 风险先于 replay；`request_id` 唯一；accepted owner 只从 null 设置；组织认领 fail-closed |
| `ActivationRequest/ActivationResult` | `activation_consent.consent_record`, `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `entitlement_metering.trial_grant`, `entitlement_metering.entitlement_grant`, `entitlement_metering.entitlement_balance` | T-S2-13、T-S2-01、T-S2-09、T-S2-10、§7.3 | 显式同意、修订 +1、一次性 FIRST-TRIAL；不重复发放；暂停/恢复只由 operations |
| `OutboundCallTask` | `ivr.outbound_call_task`, `ivr.call_record`, `subject_number.caller_number_binding`, `subject_number.business_subject`, `activation_consent.service_activation` | T-S2-02、T-S2-03、§7.3 | 同一规范主叫/主体唯一非终态任务；创建前要求当前 ACTIVE activation 且无其他 ACTIVE/UNKNOWN call；30 秒冻结；不续期；领取后不可替换/取消 |
| `CatalogOffer/Order` | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `catalog_order.customer_order`, `catalog_order.payment_order`, `activation_consent.consent_record`, `activation_consent.service_activation`, `customer_iam.identity_profile` | T-S2-07、§9.17、§10.17、§10.19 | 服务端金额与冻结快照；versionToken 精确校验；当前 activation 必须 ACTIVE；`UNKNOWN` 阻断二次结账；IVR 不能下单 |
| `CatalogVersionDraft/TrialPolicyDraft` | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `entitlement_metering.trial_policy_version`, `operations_iam.employee_account`, `audit.audit_fact` | A0 §4.17/§4.18/§4.19/§4.29 与 §7.1/§7.3 状态机；T-S2-14 的 DRAFT/never-published 前置；**不新增 A0 事务、不新增 platform idempotency scope** | 只创建/替换未发布候选；创建用自然唯一键 + canonical 持久化内容投影，替换用 `expectedRevision` + 内容投影；收益行 append-only；已发布内容不可改写；发布仍走 O10/O13 的 T-S2-14。O20 运行时插入受 `R-A0-O20-PUBLISHER` 前置约束（A0 `published_by_employee_id` 是实际发布者事实且 NOT NULL），A1 不求解、不暴露、不改写 A0 |
| `EvidenceSummary/EvidenceDetail` | `evidence.evidence_record`, `evidence.evidence_artifact`, `evidence.recording`, `evidence.media_object` | §7.4、§9.27、§9.28 | 只返回安全元数据/artifact 安全事实；不返回对象键/版本/bytes/URL；`CALL_RECORDING` 来源 |
| `ShareCreated/ShareCodeRotation/ShareState/SharePage` | `sharing.evidence_share_grant`, `sharing.share_access_event`, `evidence.evidence_record` | T-S3-03、§4.44、§7.4 | 冻结 content/verification/creation；token 只回一次；code rotation 返回 `ShareCodeRotation` 且无 token 重建；revoke/code rotation 条件更新 |
| `PublicShareView/ShareAdmission/MediaAuthorization` | `sharing.evidence_share_grant`, `sharing.share_admission`, `sharing.share_access_event`, `subject_number.subject_risk_state`, `evidence.evidence_record` | T-S3-04、T-S3-05、§7.4 | P1 observation-free；显式准入只计一次；已提交 admission 的 ticket 失败为 503 且不重计；generation/expiry/risk 服务端校验；P3 凭证由媒体网关校验；`LOCKED` 只阻止新准入 |
| `IvrDecision` | `ivr.inbound_session`, `ivr.call_record`, `ivr.outbound_call_task`, `ivr.ivr_decision`, `entitlement_metering.usage_reservation`, `activation_consent.service_activation`, `entitlement_metering.entitlement_balance` | T-S2-01、T-S2-03、T-S2-04、T-S2-11、§10.14 | 三 purpose 冻结 decision；正常领域结果 200；同指纹 replay；任务领取/预占/授权同事务；无最新被叫回退 |
| `CallEventReceipt` | `ivr.call_event`, `ivr.call_record`, `ivr.call_leg`, `entitlement_metering.usage_reservation`, `entitlement_metering.usage_fact` | T-S2-12、T-S2-05、T-S2-06、§9.24 | 父 call 先校验；首次终态吸收；未知 call 不建占位；UNKNOWN 不释放；结算 owner-safe |
| `RecordingDeclaration/RecordingCompletion` | `evidence.recording`, `evidence.media_object`, `evidence.evidence_record`, `evidence.evidence_event`, `ivr.call_record` | T-S3-01、T-S3-02、§9.27、§9.28 | 声明不等于验证；服务端重算 hash/MIME/大小/时长；UPLOADED 前置；证据固化独立 |
| `ActivationTransition` | `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `audit.audit_fact`, `platform.outbox_event` | T-S2-09、§9.19、§4.38 | 只有 OPERATIONS 可 suspend/resume；不重发试用；修订/事件身份/审计 |
| `CatalogVersionState/TrialPolicyState` | `catalog_order.catalog_version`, `entitlement_metering.trial_policy_version`, `audit.audit_fact`, `platform.outbox_event` | T-S2-14、§9.26、§10.20 | 期望版本/修订；先退市/停用旧行再激活；stale 稳定冲突；无 offer 独立撤下 |
| `AuditFact` | `audit.audit_fact` | §4.5、§7.1 | 追加只读；redacted snapshots；不提供写入端点 |
| `Problem/ProblemCode/FieldProblem/PageInfo/Cursor` | `platform.idempotency_record`, `platform.outbox_event`（仅语义关联；`platform.idempotency_record` 只适用于 A0 已批准 scope 的 C10 订单等路径） | §10.3/§10.4、§4.3 | 错误/分页不是业务真源，不映射新表；幂等结果按 A0 存储；O18-O21 不映射此行 |
| `CallbackAcknowledgement/OpaqueProviderPayload` | `catalog_order.payment_event`, `catalog_order.payment_order`, `catalog_order.customer_order`, `platform.outbox_event` | T-S2-08、§9.18、§10.17 | 验签/时间窗/防重放；同事件同指纹首结果；支付成功与权益发放分离 |

### 7.2 未决输入与 A1 处理

| ID | 未决输入 | A1 只能做到 | Owner | Destination |
| --- | --- | --- | --- | --- |
| Q1 | 员工认证源、MFA、凭据轮换 | `employeeSession` 不透明 scheme + 固定 ADMIN/OPS 扩展；不提供登录/MFA endpoint | Security/Backend | 安全设计与运营 IAM 实现 |
| Q6 | 微信支付商户/应用标识、payload、回调留存 | `OpaqueProviderPayload` + 回调确认边界；不定义供应商字段/验签算法 | Payments/Backend | 支付适配器与 OpenAPI/安全设计 |
| Q7 | SIP 线路/接入号/录音格式/缓冲/时钟 | IVR 只定义动作/事件封套；录音声明字段为 type 边界 | Telephony/IVR | IVR 协议与线路联调 |
| Q8 | 授权/上报时长容差、IVR SLO/超时 | 不设容差；`authorizedRecordingSeconds` 来自 reservation；异常走 A0 校验/对账 | IVR/Backend/QA | IVR 协议与容量/验收 |
| Q10 | 录音告知/同意文案与提示版本生命周期 | 只传 `promptVersion`/policy 引用；不选择文案 | Product/Legal | 同意政策与 IVR 提示审批 |
| Q12 | 分享 code KDF、准入上限、过期/限流默认值 | 只定义 `EXTRACTION_CODE` 输入/一次性返回；`maxAdmissions`/`expiresAt` 由已批准请求提供；不设默认 | Security/Product | 安全设计与分享政策 |
| Q14 | 实名供应商/证据/复验策略 | 订单只要求服务端实名状态；不定义供应商/证件字段 | Security/Product | 实名适配器与身份设计 |
| Q15 | 事件域 wire 值与格式 | 固定 domain 名称/封套字段；eventType/payload wire 映射保留 | Backend/Security | IVR 协议与 OpenAPI/安全设计 |
| Q18 | 30 秒任务 TTL 配置键/审批/调度 | 契约只暴露冻结 `ttlSeconds`/`expiresAt`；不提供配置写 endpoint；30 秒为源默认 | Product/Backend | IVR 协议与运营配置 |
| Q19 | 微信 appid/openid/unionid 映射 | `customerSession` 不透明；不建登录/身份合并 endpoint | Identity/Backend | 认证设计与安全评审 |
| Q22 | Redis 播放 ticket TTL、时钟偏差、撤销传播窗口 | `MediaAuthorization` 返回服务端派生 expiry；P2 在准入提交后 ticket 失败为 503 且不重计，重放只重发；P3 是凭证签发/重建控制面，媒体网关校验凭证；不设数值策略 | Security/Sharing | 安全设计与分享政策 |
| Q25 | `authorizationExpiresAt` 窗口/时钟偏差 | 契约只复制 committed reservation 的绝对时间；不设默认值 | IVR/Entitlement/Backend | IVR 协议、配置、容量/验收 |
| Q26 | typed snapshot JSON schema 版本/IVR wire 映射 | 固定 `IvrDecision` 字段/枚举；schema version 作为输入边界保留 | IVR/Backend/Security | IVR 协议与 OpenAPI/安全设计 |
| Q27 | 组织成员任务创建/领取权限角色矩阵 | 契约要求服务端实时校验成员/组织/资源生命周期；不写角色矩阵 | Organization-access/Security | 授权设计与 S2 政策测试 |
| Q28 | IVR 本地 B 路唯一键/恢复协议 | 契约只要求 `callId` 授权/幂等和“先持久化再外呼”语义；不设计本地 key | Telephony/IVR | IVR 协议与线路联调；不扩业务 schema |
| N-1 | `catalog_product` 稳定产品身份的初始供给/迁移来源 | 不新增产品创建 endpoint；O18/O19 只接受已存在的不可变 product；productCode/productFamily 的初始数据不在 A1 内 | Product/Operations/catalog-order | 目录运营数据供给与后续 catalog operations OpenAPI 工作 |
| N-2 | 线下客服联系方式/处理说明的部署值 | 不建匿名/运营 API；由 UI/部署配置展示；无 A0 表、无公开信任边界 | Product/Operations | 部署配置与三端 UI |
| API-DEF-1 | V1 API-governance 分页默认值 | 仅固定 `limit default=20, max=100` 为契约默认值；不是安全/产品政策，服务端可 clamp | Backend/API governance | 最终 OpenAPI description 与 docs/15 |
| R-A0-O20-PUBLISHER | A0 `entitlement_metering.trial_policy_version.published_by_employee_id` 为 NOT NULL 且语义是**实际发布者事实**；A0 未定义 draft 创建时的持久化取值 | O20 只冻结公共 request/response 与静态 A0 映射；不暴露、不重命名、不临时填充该字段，不声称运行时 draft 插入已解决；S2 stop/completion 报告必须原样携带该前置 | A0 schema/catalog-order/entitlement-metering（O20 runtime implementation 之前） | A0 schema/catalog-order 后续修订 + 后续 runtime trial-policy draft implementation |

其他 A0 输入（Q2/Q3/Q4/Q5/Q9/Q11/Q13/Q16/Q17/Q20/Q21/Q23/Q24）保持 `docs/14` §14 原 owner/destination；A1 不新增/删除/改写 A0 Q 输入行。N-1/N-2/API-DEF-1 是 A1 计划级治理输入，不改变 `docs/14` §14。特别是 Q4/Q5/Q20 的价格、续购/退款、分期触发不进入契约；Q24 的 risk 只保留 A0 的“`OPEN` 阻止认领/历史证据访问”，不发明外呼禁令。

**`catalog_offer` 独立撤下**：A1 明确不提供 `WITHDRAWN` 写 endpoint/事务。现有 `catalog_version.OFFSALE` 阻断新订单；独立 offer 撤下决策 deferred 给 Product/Operations/catalog-order 与后续 catalog operations OpenAPI 工作（A0 OQ-4）。DRAFT 候选创建/替换只在未发布版本上写 offer/token，不提供已发布 offer 的独立撤下。

### 7.3 残余风险

| 风险 | 影响 | Owner/Destination |
| --- | --- | --- |
| 回调 body 为 opaque，生成客户端无法静态校验供应商字段 | A1 只能保证确认/验签边界；供应商字段错误由适配层拦截 | Payments/Backend；支付适配器 |
| IVR eventType/payload 未定 | 事件语义/重试/媒体协议仍可能改变；A1 只冻结封套和幂等 | IVR/Backend；IVR 协议 |
| capability token 在路径中出现 | edge/应用日志可能记录 secret；A1 只能声明脱敏要求，无法证明网关配置 | Security/Platform；安全设计与 edge 配置 |
| cursor 稳定排序是契约声明，未在真实数据/并发下验证 | 数据插入/删除可能使分页边界需要重新确认；实现需按稳定键和过滤摘要实现 | Backend/QA；服务实现与集成测试 |
| DRAFT 候选创建/替换只覆盖目录/试用策略的未发布候选 | 产品初始身份仍依赖部署/数据供给（N-1）；收益行 A0 append-only，变更需新建版本候选；已发布内容仍只由 T-S2-14 管理 | Product/Operations/catalog-order；目录运营数据供给 |
| O20 运行时插入与 A0 publisher 事实不一致 | A0 将 `published_by_employee_id` 定义为 NOT NULL 的实际发布者事实，且未提供 draft 创建值；A1 仅冻结公共契约与静态映射，运行时 trial-policy draft 插入在 A0 schema/catalog-order 前置解决前不可实施 | A0 schema/catalog-order/entitlement-metering；`R-A0-O20-PUBLISHER` |
| A0 精确表名依赖 docs/14 catalog 块解析 | 若 docs/14 标记格式变化，`check-doc-map` 可能误报/漏报；实现需保持 catalog 块与精确匹配规则 | A1 验证作者/Backend；docs/14 只读基线 |
| `limit` default 20/max 100 是未来源化的 V1 API-governance 默认值 | 不是安全/产品策略；服务端可 clamp，若后续批准不同默认值需更新契约 | Backend/API governance；最终 OpenAPI/docs/15 |
| 运营无完整号码搜索 | 为降低 PII 风险，A1 只提供结构化过滤/ID 查询；完整号码搜索需隐私评审的专用操作 | Product/Security/Operations；后续搜索契约 |
| 无员工认证/MFA 端点 | `employeeSession` 只是抽象；A1 不能证明运营鉴权 | Security/Backend；Q1 |
| 无支付发起 endpoint | A1 只有订单与回调；客户实际拉起微信支付属支付适配器/Phase C | Payments/Backend；Q6 |
| 无运营录音播放 | 符合当前基线，但真实客服场景可能需要在后续安全设计中单独授权 | Product/Security；后续安全/运营设计 |
| 生成 TS 类型不等于运行时客户端 | 不证明鉴权、错误处理、重试、脱敏或 network 行为 | Frontend/Backend；实现 work units |

## 8. 实施切片（3 个）

### 8.1 切片原则与最小性

按**外部行为/失败模式**分组，而不是按 namespace 或文件机械拆分：

- S1 建立公共契约基座并覆盖“人类身份/资源归属/开通”真相；它必须最先完成，否则后续 operation 会各自复制 Problem/security/cursor/A0 约定。
- S2 覆盖“服务端定价、订单、目录/试用策略控制”，其核心失败模式是价格/版本/发布并发与审计；与 S1 的只读/开通边界分开评审。
- S3 覆盖“外部执行与能力令牌”：任务、IVR 决策/事件/录音、公开分享、支付回调；其核心失败模式是 replay、过期、任务领取、准入计数和外部事实收敛。
- 三个切片分别有独立的验证信号和 stop condition；合并成一个会形成无法在单轮评审中验证的巨型契约，拆成四个以上则会机械地按 endpoint/namespace 切开并重复共享规则。S3 虽包含多个外部通道，但共享“外部事实 → 稳定事件身份 → 失败关闭”的同一行为，不可再按面拆成互不依赖的文档。

### 8.2 S1 — 公共契约基座与人类身份/资源归属/开通

| 项 | 内容 |
| --- | --- |
| 目标 | 建立 canonical OpenAPI 根、五 tag、security schemes、Problem/ProblemCode/PageInfo/Cursor/敏感投影、A0 registry 检查、生成/编译工具，并完成客户 C1/C2/C3/C4/C5/C13/C14 与运营 O1/O2/O3/O4/O5/O6/O7 共 **14** 个 operation |
| 允许文件 | `api/openapi/onecall-v1.openapi.yaml`（新建/修改）；`api/openapi/redocly.yaml`；`api/openapi/contract-rules.mjs`；`api/openapi/check-doc-map.mjs`；`api/openapi/client-compat.ts`；`api/openapi/tsconfig.client-compat.json`；`api/openapi/package.json`；`api/openapi/package-lock.json`；`docs/15-production-openapi-surface-spec.md`（新建/修改）；`docs/gateflow/a1-production-openapi-surfaces-spec-s1-implementation.md`（新建） |
| 依赖 | A0 `docs/14` 的 44 表名/状态/事务；docs 09/11；无运行时依赖 |
| 期望行为 | OpenAPI 可被 Redocly 解析；五 surface tags 与 security 映射存在；客户/运营 14 operation 均有 metadata/Problem/cursor/授权扩展；Problem registry 与 enum 一致；`GET` 不产生写副作用；运营读取不含录音播放；主体/号码/认领/开通字段与 A0 状态一致 |
| 不变量/状态 | 账号 ACTIVE + identity 才能登录（由 session 断言，不由 A1 实现）；工作空间来自 ACTIVE membership/organization；认领只追加个人 owner 且风险先于 replay；开通 `INACTIVE→ACTIVE` 只由 accepted consent；暂停/恢复只 OPERATIONS；不做尝试性 claim/reservation 的状态写 |
| 验证 | `npm run check:s1` exit 0（`check` + stage-aware 精确断言）；Redocly 0 problems；stage check 断言 operationId 集合**恰为** `C1,C2,C3,C4,C5,C13,C14,O1..O7` 且 customer=7/operations=7/ivr=0/public-share=0/provider-callback=0/总计=14；错误 stage 集合或计数必须失败；`check-doc-map` missing=extra=duplicate=invalid=0；`tsc` strict 通过；`git diff --check -- <允许路径>` 通过；`rg`/规则确认无 sensitive 字段、无短名/通配符 A0 名、`servers` 恰为一项 `url: /` |
| 完成信号 | S1 实现报告列出 14 operationId、验证命令/exit、A0 表覆盖、文档决策、开放输入未变；无运行时/数据库/UI 声明 |
| 停止条件 | 任一 operation 无法锚定 A0/业务文档；需要新增未批准角色/数值/供应商字段；Redocly/生成器不可用且无已锁定工具；需要改 09-14、原型、截图或 review 文件；需要 Flyway/后端/UI |

### 8.3 S2 — 服务端定价、订单与运营目录/试用控制

| 项 | 内容 |
| --- | --- |
| 目标 | 完成客户 C9/C10/C11/C12 与运营 O8/O9/O10/O11/O12/O13/O14/O15/O16/O17/O18/O19/O20/O21 共 **18** 个 operation；冻结订单/目录/试用策略/审计的服务端派生、DRAFT 候选创建/替换与 state-conditional 发布语义 |
| 允许文件 | `api/openapi/onecall-v1.openapi.yaml`；`api/openapi/contract-rules.mjs`（仅当 S1 规则需新增本 slice 的 registry/断言，不得放松既有规则）；`api/openapi/client-compat.ts`；`docs/15-production-openapi-surface-spec.md`；`docs/gateflow/a1-production-openapi-surfaces-spec-s2-implementation.md`（新建） |
| 依赖 | S1 已接受的 base/Problem/cursor/security/tooling；A0 T-S2-07、T-S2-14、§4.17/§4.18/§4.19/§4.29、§9.17/§9.26、§10.19 |
| 期望行为 | 目录只返回服务端 ACTIVE 版本/渠道/versionToken；订单金额/币种/快照/支付状态均由服务端计算，**创建前要求当前 activation ACTIVE**；`UNKNOWN` 阻断二次结账；购买同意是显式且当前 policy；运营可创建/替换未发布目录与试用策略 DRAFT 候选，发布/下架/停用带 expected version/revision；stale 稳定冲突；已发布内容与 append-only 收益行不可改写；审计只读；无通用 CRUD/审批引擎、无 offer 独立下架、无人工支付/退款/发票 |
| 不变量/状态 | 目录 `DRAFT→ACTIVE→OFFSALE`，同一产品至多一个 ACTIVE；DRAFT 候选只可未发布时写，收益行 append-only；订单 payment 维度 `UNPAID/UNKNOWN/PAID/CLOSED` 与 issuance 分离；试用策略至多一个 enabled published，DRAFT never-published 才可写；历史订单/试用快照不可改写；运营 metadata 读取不授予录音播放 |
| 验证 | `npm run check:s2` exit 0（`check` + stage-aware 精确断言）；累计 operationId 集合恰为 S1 集合 + `C9,C10,C11,C12,O8..O21`，customer=11/operations=21/ivr=0/public-share=0/provider-callback=0/总计=32（S1 14 + S2 18）；订单/目录/试用策略/DRAFT 候选 operation 的 `x-onecall-a0-models` 全部为 A0 44 表内精确 schema-qualified 名；O18-O21 不含 `Idempotency-Key` 且 `x-onecall-a0-models` 不含 `platform.idempotency_record`，负例必须被拒绝；文档映射行存在；生成 d.ts/tsc 通过；`git diff --check` |
| 完成信号 | S2 报告 18 operationId、累计计数、DRAFT 候选自然唯一/内容投影/`expectedRevision` 语义与发布/下架 stale 断言、A0 事务映射、`R-A0-O20-PUBLISHER` 运行时前置原样记录、验证输出、无支付/退款/发票/通用 CRUD 实现声明 |
| 停止条件 | 需要企业报价支付、支付发起、退款/发票、已发布内容或 append-only 收益行改写、offer 独立撤下、试用默认数值；需要新增数据库/后端；A0 发布/订单事务与契约冲突；需要求解 O20 运行时 `published_by_employee_id` 插入或改写 A0（停止并作为 `R-A0-O20-PUBLISHER` 报告；A1 静态 slice 可在不声称运行时的情况下完成） |

### 8.4 S3 — 外部执行、公开能力与回调收敛

| 项 | 内容 |
| --- | --- |
| 目标 | 完成客户 C6/C7/C8/C15/C16/C17/C18、IVR I1-I6、公开分享 P1-P3、回调 K1 共 17 个 operation；形成任务、IVR 决策/事件/录音、分享准入/媒体授权、支付回调的完整幂等/失败关闭边界 |
| 允许文件 | `api/openapi/onecall-v1.openapi.yaml`；`api/openapi/contract-rules.mjs`（仅新增本 slice registry/断言，不放松规则）；`api/openapi/client-compat.ts`；`docs/15-production-openapi-surface-spec.md`；`docs/gateflow/a1-production-openapi-surfaces-spec-s3-implementation.md`（新建） |
| 依赖 | S1/S2 已接受基座；A0 T-S2-02/03/04/12、T-S3-01/02/03/04/05、§10.14、§10.17、§10.18 |
| 期望行为 | 任务创建/替换/取消保持单主叫单非终态任务、30 秒冻结、不续期；IVR resolve 原子领取/预占/授权，正常领域结果统一 200 `IvrDecision`（含 in-progress/reject/confirmation-not-allowed/consent-required/direct 非外呼），409 仅身份/指纹冲突；direct 授权拒绝 task；事件未知 call 不建占位、首终态吸收、结算不重扣；录音声明/完成区分 UPLOADED 与 evidence solidification；分享创建 token 只回一次、P1 observation-free、P2 页面/预取不计数且准入一次计数、P2 ticket 失败 503 不重计、P3 允许已准入 `LOCKED` 会话且不返回 `SHARE_LOCKED`；回调验签边界 + 同事件幂等，不声称支付/权益已完成 |
| 不变量/状态 | `outbound_call_task` PENDING→CLAIMED 与其他终态；`inbound_session` RECEIVED→CONFIRMED/ABANDONED/REJECTED；`ivr_decision` 三 purpose 冻结；`recording` DECLARED→UPLOADED→VALIDATED/QUARANTINED；`evidence_record` PENDING→SOLIDIFIED/QUARANTINED/FAILED；`share_grant` ACTIVE/LOCKED/EXPIRED/REVOKED 与 generation；`share_admission` 一次准入；`payment_event` 首次处理/隔离；均无最新被叫、无重复扣量、无 token 重建 |
| 验证 | `npm run check:s3` exit 0（`check` + stage-aware 精确断言）；最终 operationId 集合恰为全部 C1-C18/O1-O21/I1-I6/P1-P3/K1，customer=18/operations=21/ivr=6/public-share=3/provider-callback=1/总计=49；全部 operationId 在 `client-compat.ts` 的期望 union 中；doc-map missing=extra=duplicate=invalid=0；生成 d.ts 两次 hash 一致；`tsc` strict 通过且 `ShareCodeRotation` 无 `capabilityToken` 断言通过；`git diff --check`；敏感字段/secret-direction/凭证标注规则无违规；P1 member 门控断言通过；仅允许路径变化 |
| 完成信号 | S3 报告 17 operationId、最终五面计数（18/21/6/3/1=49）、生成客户端 hash、A0 映射/开放输入表、不可证明项；明确“无运行时、无支付/IVR/分享/回调实现”声明 |
| 停止条件 | 需要详细 IVR 媒体/重试/签名协议、分享 KDF/TTL/上限数值、供应商 payload/证书、运营播放、在线客服工作流、支付发起、COS/Redis 凭证；需要修改 A0 或未决输入值；任一 operation 无法在 A0 找到表/事务/状态锚点 |

## 9. 静态验证与不可证明项

### 9.1 工具链与可复现前提

`api/openapi/package.json` 固定三类 dev 工具（版本在 S1 由 lockfile 解析并记录，不使用浮动的 `latest` 作为完成证据）：

- `@redocly/cli`：OpenAPI 3.1 parse/lint/bundle。
- `openapi-typescript`：生成 `paths`/`operations` 类型。
- `typescript`：strict 编译 fixture。

可复现前提：Node 22（当前环境 `v22.23.4`）、npm registry 可访问或已缓存 `npm ci`。若 registry 不可用，实施 Agent 必须**停止并报告 blocked**，不得用 `grep`/手写 parser 冒充完整 OpenAPI 校验。

### 9.2 命令与期望断言

在 `api/openapi/` 下执行：

```bash
npm ci
npm run lint      # redocly lint onecall-v1.openapi.yaml --config redocly.yaml
npm run bundle    # redocly bundle ... -o ../../tmp/openapi-client/onecall-v1.bundle.json --ext json
npm run generate  # openapi-typescript ... -o ../../tmp/openapi-client/onecall-v1.d.ts
npm run typecheck # tsc --noEmit -p tsconfig.client-compat.json
npm run doc-map   # node check-doc-map.mjs ../../tmp/openapi-client/onecall-v1.bundle.json ../../docs/15-production-openapi-surface-spec.md
npm run check     # 顺序执行以上五项
npm run check:s1  # check + check-doc-map --stage s1：精确 operationId 集合与 7/7/0/0/0=14
npm run check:s2  # check + check-doc-map --stage s2：精确累计集合与 11/21/0/0/0=32
npm run check:s3  # check + check-doc-map --stage s3：最终 18/21/6/3/1=49
```

`check:s1/s2/s3` 必须由 `package.json` 固定脚本实现（如 `node check-doc-map.mjs --stage s1 ...`），每个 stage 断言**该 stage 的精确 operationId 集合**与五面计数；错误 stage、缺失/多余 operation、错误计数都必须 exit 非 0。共享 Redocly `onecall/versioned-paths` 规则只做前缀/唯一性/无 v2/无未版本化路径检查，不在共享规则中硬编码最终 49 计数，因此 S1/S2 不会因最终计数而失败。

| 断言 | 期望 |
| --- | --- |
| Redocly lint | exit 0；`0 problems`；recommended + 自定义规则全部通过；顶层 `servers` 恰好一项相对 `url: /`（缺失/绝对/额外根失败） |
| Bundle | exit 0；bundle JSON 可解析；`openapi: 3.1.x`；operationId 集合与源一致；所有 `$ref` 已解析 |
| Stage 集合/计数（`npm run check:s1/s2/s3`） | 每个 stage 断言精确 operationId 集合：S1 = `C1,C2,C3,C4,C5,C13,C14,O1..O7`，customer=7/operations=7/ivr=0/public-share=0/provider-callback=0/总计 14；S2 = S1 + `C9..C12,O8..O21`，customer=11/operations=21/总计 32；S3 = 全部 C1-C18/O1-O21/I1-I6/P1-P3/K1，customer=18/operations=21/ivr=6/public-share=3/provider-callback=1/总计 49。错误 stage 集合或计数 exit 非 0 |
| `check-doc-map` | 每个 operationId 在 `docs/15` 有唯一行；每个 `x-onecall-a0-models` 条目与 `docs/14` 的 44 表 catalog 做**精确集合匹配**（schema-qualified，不接受短名/别名/通配符/错误 schema）；missing=0、extra=0、duplicate=0、invalid=0 |
| A0 名称负例 | fixture `usage_reservation`（短名）、`catalog_*`（通配符）、`sharing.catalog_version`（错误 schema）、`catalog_order.share_access_event`（错误 schema）、O18-O21 中的 `platform.idempotency_record` 与任何未批准 scope（`CATALOG_DRAFT`/`TRIAL_POLICY_DRAFT`）均必须被 `check-doc-map`/规则拒绝；`catalog_order.catalog_product` 等四个 catalog 表必须显式出现 |
| 生成 | exit 0；`tmp/openapi-client/onecall-v1.d.ts` 含 `export interface paths` 与 `operations`；operationId 全覆盖 |
| `tsc` | exit 0；`strict: true`；fixture 的 expected operation union 与生成 `operations` 完全相等；关键 schema（`IvrDecision`, `Problem`, `ShareAdmission`, `CustomerOrder`）可赋值；`ShareCodeRotation` 无 `capabilityToken` 断言通过 |
| Secret direction | `ShareCreated.capabilityToken`/`extractionCode` 与 `ShareCodeRotation.extractionCode` 为 `readOnly` + `x-onecall-return-once`；分享准入提取码为 `writeOnly`；`RecordingDeclaration.upload.credential`、`ShareAdmission.mediaAuthorization.credential`、`MediaAuthorization.credential` 为 `readOnly` + `x-onecall-log-redaction: required` 且**无** `x-onecall-return-once`；生成类型 fixture 断言码轮换响应无 `capabilityToken`、请求不接受响应秘密、三个凭证字段不进入请求类型 |
| 生成可重复 | 连续两次 `npm run generate` 后 `sha256sum tmp/openapi-client/onecall-v1.d.ts` 相同；记录 hash |
| 编码/空白 | `file -bi api/openapi/onecall-v1.openapi.yaml docs/15-production-openapi-surface-spec.md` 显示 UTF-8；`git diff --check -- api/openapi docs/15-production-openapi-surface-spec.md` exit 0 |
| 路径范围 | `git status --porcelain` 只出现允许路径；既有截图/`docs/reviews/plan-review-20261021-205118.md` 不出现 |
| 版本化路径清单（F-09/F-13） | 对 49 行清单与 OpenAPI `paths` 逐项提取 `method + path`：全部以 `/api/v1/` 开头且只匹配五个批准前缀；无 `/api/v2`、无未版本化 operation 路径、unversioned=0、duplicate=0、multi-surface=0；最终计数由 `check:s3` 断言 18/21/6/3/1/49，S1/S2 由各自 stage 断言（共享规则不硬编码最终计数）；顶层 `servers` 必须恰好一项相对 `url: /`（缺失/绝对/额外根失败）；正文 `METHOD /...` 示例无裸前缀 |
| 敏感字段 | 自定义规则扫描 schema property；禁止 `objectKey|objectVersionId|bucket|cryptoKeyRef|capabilityTokenHash|codeHash|codeSalt|payloadCiphertext|callerNumber$|calleeNumber$|fullNumber|idNumber|legalName|providerSubject`（除 `x-onecall-return-once` 的 create/rotation 响应和 IVR 内部 payload 标记）；`ShareCodeRotation` 不得含 `capabilityToken` |
| Problem | 每个 operation 的 `x-onecall-error-codes` 均为 `ProblemCode` 子集；每个非 `INTERNAL` code 至少被一个 operation 引用；`Problem.status` 与 registry `httpStatus` 一致；inventory 的专有冲突码必须是 registry 中的精确 code，**禁止 `*`/`_*` 通配占位、裸 409 或无码冲突**；仅 §5 图例定义的公共 401/403/404/422/429/500/503 可用状态简写并由 docs/15 统一映射，不得替代该 operation 的专有冲突码；C4 精确 `CLAIM_RISK_BLOCKED`/`CLAIM_REPLAY_CONFLICT`/`CLAIM_CONTEXT_CONFLICT`（proof 无效为 `VALIDATION_FAILED` 422），C10 精确 `PURCHASE_CONSENT_REQUIRED`/`PURCHASE_CONSENT_STALE`/`PURCHASE_CONSENT_DECLINED`，C6/C8 精确 `OUTBOUND_CALL_IN_PROGRESS`/`OUTBOUND_TASK_CONFLICT`/`OUTBOUND_TASK_NOT_CANCELLABLE`，O4 `ACTIVATION_STATE_CONFLICT`，O11 `CATALOG_PUBLICATION_CONFLICT`/`PUBLICATION_STALE_VIEW`，O14 `TRIAL_POLICY_PUBLICATION_CONFLICT`/`TRIAL_POLICY_STALE_VIEW`；`CONFIRMATION_NOT_ALLOWED`/`TASK_CONFLICT`/`CALL_IN_PROGRESS`/`TASK_CLAIMED`/`TASK_UNAVAILABLE` 不得作为 Problem code |
| IVR status | I1/I2/I3 的正常领域结果（含 `REJECT_CALL_IN_PROGRESS`、`REJECT`/`TASK_CONFLICT`、`CONFIRMATION_NOT_ALLOWED`、`CONSENT_REQUIRED`、direct 非外呼）返回 `200 IvrDecision`；409 仅身份/指纹/access-number 冲突且无冻结决策 |
| Security | 每个 operation 的 security 与 surface 表一致；IVR/callback 必有 mTLS + signature；public share 必有 capability path；operations 不得出现客户 session；无 `security: []` 或匿名 operation |
| Cursor | 每个返回 `*Page` 的 GET 必有 `cursor`/`limit`；`nextCursor` 可空但 `hasMore` 必填；无 offset/total；C9 排序与 filter digest 断言；O1 断言 `createdAt desc, subjectId desc`、结构化过滤摘要绑定；fixture：过滤条件变化复用 cursor → `CURSOR_FILTER_MISMATCH`，同 `createdAt` 由 `subjectId desc` 稳定 tie-break，完整号码/自由文本过滤被拒绝 |
| Share semantics | P1 observation-free（不写 `share_access_event`）；P1 `ORGANIZATION_MEMBER` 无匹配会话时省略 `content`、只返回挑战元数据，匹配后才投影；P2 新准入 `SHARE_LOCKED`、ticket 失败 503 不重计、同 key 重放重发票据；P3 允许已准入 `LOCKED` 会话且错误集不含 `SHARE_LOCKED` |
| DRAFT candidates | O18-O21 只创建/替换 `DRAFT`/never-published 候选；创建用自然唯一键 + canonical 内容投影（同投影重放返回既有 DRAFT，不同投影 `*_DRAFT_CONFLICT`），替换用 `expectedRevision` + 内容投影（已应用重放返回当前 DRAFT，stale `*_DRAFT_STALE_VIEW`）；O18-O21 **不得**含 `Idempotency-Key` 或 `platform.idempotency_record`；新/未批准 scope 负例（如 `CATALOG_DRAFT`/`TRIAL_POLICY_DRAFT`）必须失败；收益行/已发布内容不可改写；计数/operation union 为 49；O20 记录 `R-A0-O20-PUBLISHER` 且不声称运行时插入 |
| 幂等 | 每个 POST/PUT/PATCH/DELETE 要么有 `Idempotency-Key` 参数，要么 `x-onecall-idempotency` 属于 `event-body|state-conditional|provider-event|not-applicable`；`not-applicable` 仅允许只读动作（A1 无支持入口例外）；O18-O21 必须为 `state-conditional` 且不得声明 `Idempotency-Key`；任何不在 A0 scope registry（`IVR_CONFIRM`/`PURCHASE`）中的 `platform.idempotency_record` scope 必须失败 |

### 9.3 自定义 Redocly 规则清单

`api/openapi/contract-rules.mjs` 至少实现：

1. `onecall/operation-metadata`：operation 必须有 `x-onecall-surface`、`x-onecall-audience`、`x-onecall-authorization`、`x-onecall-a0-models`、`x-onecall-a0-contracts`、`x-onecall-error-codes`、`x-onecall-idempotency`；`x-onecall-a0-models` 每个条目必须精确匹配 `docs/14` catalog 的 schema-qualified 名（只接受 `[a-z_]+\\.[a-z_]+` 且在全集中），拒绝短名、别名、通配符、错误 schema；`x-onecall-a0-contracts` 的事务/状态名同样从允许集合读取；O18-O21 的 `x-onecall-a0-contracts` 只能引用自然唯一约束、状态机与条件修订，引用 `platform.idempotency_begin`/`platform.idempotency_record` 必须失败。
2. `onecall/security-by-surface`：按 §4.2/§5 表校验；无匿名 operation，无 `security: []`。
3. `onecall/problem-complete`：每个 operation 至少一个 Problem 响应；`Problem` schema 必填字段齐全；`application/problem+json`；`CONFIRMATION_NOT_ALLOWED`/`TASK_CONFLICT`/`CALL_IN_PROGRESS`/`TASK_CLAIMED`/`TASK_UNAVAILABLE` 不在 Problem enum；inventory 的专有冲突码不得含 `*`/`_*` 通配占位或裸 409，C4/C6/C8/C10/O4/O11/O14 的精确 code 必须与 registry 一致；公共 401/403/404/422/429/500/503 允许按 §5 图例简写并须在 docs/15 映射到 registry code。
4. `onecall/cursor-listings`：`*Page` response 必带 cursor/limit 参数与 `nextCursor`/`hasMore`；C9 与 O1 的排序/filter digest 可被静态检查（O1 为 `createdAt desc, subjectId desc` + 结构化过滤摘要）。
5. `onecall/idempotency-writes`：按 §4.7 校验 header/body/event/state-conditional；`not-applicable` 仅只读动作；O18-O21 必须为 `state-conditional`，不得声明 `Idempotency-Key`，不得引用 `platform.idempotency_record` 或 A0 scope registry 之外的新 scope。
6. `onecall/sensitive-fields`：按 §4.9 禁止字段与 `x-onecall-return-once`/`x-onecall-log-redaction` 例外；`ShareCodeRotation` 不得含 `capabilityToken`；三个媒体/上传凭证必须是 `readOnly` + `x-onecall-log-redaction: required` 且不得带 `x-onecall-return-once`。
7. `onecall/server-relative`：顶层 `servers` 必须存在且恰好一项相对 `url: /`；缺失、绝对 URL 或额外 server 根均失败。
8. `onecall/versioned-paths`：全部 `paths` 以 `/api/v1/` 开头且只匹配五个批准前缀（`/api/v1/customer/`、`/api/v1/operations/`、`/api/v1/ivr/`、`/api/v1/public/shares/`、`/api/v1/callbacks/`）；无未版本化 operation 路径、无重复 `method + path`、无 `/api/v2`。该共享规则**不硬编码最终 49 计数**；精确 stage operation 集合/计数由 `check-doc-map --stage s1|s2|s3` 断言（S1 7/7/0/0/0=14，S2 11/21/0/0/0=32，S3 18/21/6/3/1=49）。
9. `onecall/tagged`：每个 operation 恰好一个五个 tag 之一；无未归属 operation。
10. `onecall/no-unbounded-list`：数组响应必须由 `*Page` 包裹；非分页数组仅允许固定小集合（如 `errors[]`、`artifact[]`、`benefitLines[]`）并在 `x-onecall-bounded: true` 说明。
11. `onecall/secret-direction`：响应侧一次性秘密必须 `readOnly: true` + `x-onecall-return-once: true`；请求侧秘密必须 `writeOnly: true`；`ShareCodeRotation` 的属性集合不含 `capabilityToken`；`RecordingDeclaration.upload.credential`、`ShareAdmission.mediaAuthorization.credential`、`MediaAuthorization.credential` 必须 `readOnly: true` + `x-onecall-log-redaction: required` 且不得含 `x-onecall-return-once`。
12. `onecall/ivr-decision-status`：I1/I2/I3 的 200/409 映射与 §4.6/§6.1 一致；正常领域结果必须 200 `IvrDecision`，409 仅身份/指纹冲突；`TASK_CONFLICT`/`CALL_IN_PROGRESS`/`TASK_CLAIMED`/`TASK_UNAVAILABLE` 只出现在 `IvrDecision.reasonCode`/`action`，不进入 Problem enum。
13. `onecall/share-semantics`：P1 不写 `share_access_event`；P1 `ORGANIZATION_MEMBER` 无匹配认证会话时不得含 `content`；P2 错误集含 `SHARE_LOCKED`，P3 错误集不含 `SHARE_LOCKED`；P2 503 ticket 失败不重计。
14. `onecall/draft-candidates`：O18-O21 只接受 DRAFT/never-published 状态；请求不得含 `status`/`publishedAt`/`publishedByEmployeeId`；创建使用自然唯一键 + canonical 内容投影，替换使用 `expectedRevision` + 内容投影；O18-O21 不得声明 `Idempotency-Key` 或 `platform.idempotency_record`；收益行不可改写；发布仍只由 O10/O13。

### 9.4 不能在没有实现/联调时证明的内容

- 不能证明真实鉴权：客户/员工 session 的签发、微信/OTP、MFA、mTLS 证书、签名/时间窗/nonce、防重放、能力令牌摘要解析。
- 不能证明数据库行为：唯一约束、锁顺序、事务回滚、savepoint、claim/reservation 原子性、cursor 稳定排序、并发准入/撤销/发布。
- 不能证明支付/回调：供应商验签、事件规范化、支付状态推进、权益发放、重复/乱序回调。
- 不能证明 IVR：真实线路主叫、任务过期边界、A/B 外呼桥接、录音上传/校验、UNKNOWN 对账、媒体格式/时长。
- 不能证明分享：真实 COS 对象版本/WORM、Redis ticket、短时媒体网关、撤销传播窗口、已缓冲字节无法召回；P1 observation-free、P2 ticket 失败不回滚/不重计、P3 已准入 LOCKED 会话与 recipient witness 需由实现/集成测试证明。
- 不能证明目录/试用 DRAFT 候选的并发唯一性、A0 append-only 收益行触发器和发布串行化；契约只声明 DRAFT-only 与精确 A0 映射。
- 不能证明 O20 的运行时 DRAFT 插入：A0 `published_by_employee_id` 是 NOT NULL 的实际发布者事实且无 draft 创建值；`R-A0-O20-PUBLISHER` 由 A0 schema/catalog-order/entitlement-metering 在 runtime O20 implementation 之前解决，A1 只做公共契约与静态映射，不声称运行时可用。
- 不能证明 `ShareCodeRotation` 运行时不重建 token；生成类型断言只证明 schema/类型不含 `capabilityToken`。
- 不能证明客户端运行时：生成的 TS 只是类型；不证明前端按 audience 隔离、日志脱敏、错误处理或重试策略。
- 不能证明生产 SLO/容量/安全数值：Q7/Q8/Q12/Q22/Q25 等仍由后续 owner 决定。

## 10. 文档决策、无过度设计与完成报告

### 10.1 文档决策

- 新增一份 `docs/15-production-openapi-surface-spec.md` 作为人类治理/映射文档；它不复制完整 OpenAPI schema，只记录五面 operation 索引、A0 映射、错误码/状态映射、开放输入、验证命令和限制。
- 不新增五份面文档，不新增 API README，不修改 09-14；避免第二真相源。
- OpenAPI 的 `description` 只写 operation 目的、授权谓词、幂等/敏感说明和 A0 引用；长篇状态机/开放输入留在 docs/15。
- Gateflow 报告每个 slice 单独成文；不把实现报告混入 canonical 契约。

### 10.2 无过度设计判断

- 49 个 operation 每个都有 09/10/11/13 的业务锚点和 docs/14 的表/事务锚点；没有“通用 CRUD”“角色设计器”“审批引擎”“通知模板”“支付发起”“在线客服”“运营播放”。四个 DRAFT 候选 operation 是已批准 lean-admin 草稿能力的最小闭环，不是通用目录写接口。
- 只有两个 open payload（支付回调、IVR event body），且都对应明确 deferred owner；没有用 `additionalProperties: true` 逃避其他 schema。
- 没有新增数据库表、后端模块、微服务、云资源、供应商配置或安全数值；错误/分页/幂等都是契约层已要求的能力。
- 未决值一律保留为 typed input/输出或后续 destination，不以 DEMO 值、默认 TTL、默认价格、默认角色或供应商字段填充。
- 线下客服联系方式/说明不建 API、不建表；契约源不得出现 `SupportEntry` 或 support-entry path。

### 10.3 未来实施 Agent 的完成报告模板

每个 slice 报告必须包含：

1. 任务标签、provider/model、gate、基线 commit、slice ID。
2. 变更文件精确列表（只允许本计划 §8 中该 slice 的路径）。
3. 本 slice 新增/修改的 operationId 与五面计数；累计计数。
4. 契约决策摘要：security/authorization、Problem/code、idempotency、cursor、A0 映射、敏感字段。
5. 执行过的命令、exit code、关键输出（Redocly 0 problems、bundle 计数、生成 hash、tsc 0、doc-map missing/extra、`git diff --check`）。
6. 生成客户端兼容性证据：生成器版本、d.ts hash、fixture 覆盖的 operationId union。
7. A0 映射覆盖：每个 operation 的表/事务/状态；open input 未变清单。
8. 残余风险/deferred：owner/destination；明确未实现项。
9. 明确限制：没有运行服务、数据库、Flyway、鉴权、支付、IVR、COS/Redis、分享媒体、供应商回调或客户端运行时验证。
10. 完成/阻塞声明；若阻塞，给出原因、已尝试的只读证据、未修改的路径。
11. 明确声明：未 stage/commit/push/PR/merge，未修改未授权路径。

最终 S3 报告还必须给出：

- 49 个 operationId 的完整列表与五面计数（customer=18、operations=21、ivr=6、public-share=3、provider-callback=1）。
- `docs/15` 对 44 张 A0 表的实际引用覆盖（可引用多次，不要求 44 全覆盖，但每个 operation 至少一个表/事务）；所有 `x-onecall-a0-models` 均为精确 schema-qualified 名，负例 fixture 全部被拒绝。
- `ShareCodeRotation` 的生成类型不含 `capabilityToken`；响应/请求秘密方向断言通过。
- 15 个具名开放输入（Q1/Q6/Q7/Q8/Q10/Q12/Q14/Q15/Q18/Q19/Q22/Q25/Q26/Q27/Q28）的 owner/destination 原样保留声明；N-1/N-2/API-DEF-1 记录为计划输入；`R-A0-O20-PUBLISHER` 记录为未解决的运行时前置与残余风险。
- 对 `catalog_offer` 独立撤下的 deferred 声明。
- 对 UI/截图/`docs/reviews/plan-review-20261021-205118.md` 未变更的确认；对线下客服配置无 API/A0 表的确认。

## 11. 计划完成定义

本修正稿在以下条件满足时视为 bounded plan-fix 完成，可进入独立 plan re-review：

- 文件存在、UTF-8；本次 F-10..F-13 与澄清修正只修改本路径并新增 `docs/gateflow/a1-production-openapi-surfaces-spec-plan-fix-3.md`；F-01..F-09 的既有结果不变。
- 五面 operation 清单（18/21/6/3/1=49）、全部路径使用 `/api/v1` 前缀、公共契约、IVR 200/409 边界、精确 A0 映射、开放输入、3 slices、stage-aware 静态验证命令（`check:s1/s2/s3`）和完成报告模板均具备；O18-O21 无 `Idempotency-Key`/`platform.idempotency_record`；`R-A0-O20-PUBLISHER` 已记录且无运行时声明。
- 明确没有运行时实现、没有选定未决值、没有修改其他文件。
- 未暂存/提交/推送/创建 PR，也未进入 implementation/review gate。
