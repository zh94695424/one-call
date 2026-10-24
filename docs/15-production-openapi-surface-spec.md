# 生产 OpenAPI 五面契约治理与 A0 映射（S1）

> Work unit：`A1-production-openapi-surfaces-spec`
>
> Slice：S1（公共契约基座 + 人类身份/资源归属/开通）
>
> Canonical 契约：`api/openapi/onecall-v1.openapi.yaml`
>
> A0 只读基线：`docs/14-production-data-model-and-flyway-schema-spec.md`
>
> 本文只做人类治理、operation 索引与 A0/状态映射，不复制完整 schema，也不构成运行时服务、数据库、鉴权、支付、IVR、COS/Redis 或客户端实现声明。

## 0. 范围与静态限制

- 本文描述 S1 的 14 个 operation：客户 `C1/C2/C3/C4/C5/C13/C14`，运营 `O1/O2/O3/O4/O5/O6/O7`。
- 五个信任面固定为 `customer`、`operations`、`ivr`、`public-share`、`provider-callback`；S1 只冻结前两面，其余三面仅保留 tag/security 基座。
- 唯一契约源是 `api/openapi/onecall-v1.openapi.yaml`。`servers` 恰好一项相对根 `url: /`；版本前缀写在各 operation 路径中。
- 本切片只有静态契约与静态校验。数据库唯一约束、锁顺序、事务、鉴权、支付、IVR、媒体与客户端运行时均未被证明。
- 本文与配套静态校验不构成暂存、提交、推送、PR 或运行时生产就绪声明。

## 1. 五面与信任边界

| Surface | 前缀 | S1 operation 数 | 认证/信任 | 明确不授予 |
| --- | --- | ---: | --- | --- |
| customer | `/api/v1/customer/` | 7 | 不透明 `customerSession` bearer；服务端派生账户/工作空间 | 号码归属、价格、支付成功、实名结果、证据固结 |
| operations | `/api/v1/operations/` | 7 | 独立 `employeeSession`；固定 `ADMIN`/`OPS` | 客户身份、录音播放、任意事实改写 |
| ivr | `/api/v1/ivr/` | 0 | `ivrMtls` + `ivrSignature`（后续切片） | 客户账户、价格、支付状态、任务被领取后的重放授权 |
| public-share | `/api/v1/public/shares/` | 0 | 路径 `capabilityToken`；可选 `customerSession`（后续切片） | 分享创建者身份、对象键、未授权录音内容 |
| provider-callback | `/api/v1/callbacks/` | 0 | `providerCallbackMtls` + `providerCallbackSignature`（后续切片） | 订单金额、支付成功、权益到账 |

共享 security schemes 在 canonical 源中一次性定义：`customerSession`、`employeeSession`、`ivrMtls`、`ivrSignature`、`providerCallbackMtls`、`providerCallbackSignature`。S1 不使用后四种，但基座必须存在以避免后续切片复制或改名。

## 2. S1 Operation 索引与 A0 模型

下表由 `check-doc-map.mjs` 机器读取。每个 operationId 恰好一行；A0 列与 canonical `x-onecall-a0-models` 精确集合相等。

<!-- operation-map:start -->
| operationId | method | path | surface | A0 models |
| --- | --- | --- | --- | --- |
| getCustomerMe | GET | /api/v1/customer/me | customer | `customer_iam.customer_account`, `organization_access.organization`, `organization_access.organization_membership` |
| listCustomerBusinessNumbers | GET | /api/v1/customer/business-numbers | customer | `subject_number.business_subject`, `subject_number.caller_number_binding`, `activation_consent.service_activation` |
| getCustomerSubject | GET | /api/v1/customer/subjects/{subjectId} | customer | `subject_number.business_subject`, `activation_consent.service_activation`, `activation_consent.consent_record`, `entitlement_metering.trial_grant`, `entitlement_metering.entitlement_balance`, `subject_number.subject_claim` |
| createCustomerSubjectClaim | POST | /api/v1/customer/subject-claims | customer | `subject_number.subject_claim`, `subject_number.business_subject`, `subject_number.caller_number_binding`, `subject_number.subject_risk_state` |
| createCustomerSubjectActivation | POST | /api/v1/customer/subjects/{subjectId}/activations | customer | `activation_consent.consent_record`, `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `entitlement_metering.trial_grant`, `entitlement_metering.entitlement_grant`, `entitlement_metering.entitlement_balance` |
| listCustomerEvidence | GET | /api/v1/customer/evidence | customer | `evidence.evidence_record`, `evidence.evidence_artifact`, `evidence.recording` |
| getCustomerEvidence | GET | /api/v1/customer/evidence/{evidenceId} | customer | `evidence.evidence_record`, `evidence.evidence_artifact`, `evidence.recording`, `evidence.media_object` |
| listOperationsSubjects | GET | /api/v1/operations/subjects | operations | `subject_number.business_subject`, `subject_number.caller_number_binding`, `activation_consent.service_activation`, `subject_number.subject_claim`, `subject_number.subject_risk_state` |
| getOperationsSubject | GET | /api/v1/operations/subjects/{subjectId} | operations | `subject_number.business_subject`, `subject_number.caller_number_binding`, `subject_number.subject_claim`, `subject_number.subject_risk_state`, `activation_consent.service_activation`, `entitlement_metering.trial_grant`, `entitlement_metering.entitlement_balance` |
| suspendOperationsSubject | POST | /api/v1/operations/subjects/{subjectId}/suspensions | operations | `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `audit.audit_fact` |
| resumeOperationsSubject | POST | /api/v1/operations/subjects/{subjectId}/resumptions | operations | `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `audit.audit_fact` |
| listOperationsActivations | GET | /api/v1/operations/activations | operations | `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `activation_consent.consent_record`, `entitlement_metering.trial_grant` |
| listOperationsIvrSessions | GET | /api/v1/operations/ivr-sessions | operations | `ivr.inbound_session`, `ivr.call_record`, `ivr.outbound_call_task`, `ivr.ivr_decision` |
| getOperationsIvrSession | GET | /api/v1/operations/ivr-sessions/{sessionId} | operations | `ivr.inbound_session`, `ivr.call_record`, `ivr.call_leg`, `ivr.call_event`, `ivr.ivr_decision`, `evidence.evidence_record` |
<!-- operation-map:end -->

## 3. Operation 行为与 A0 事务映射

| Operation | 核心行为 | Idempotency/replay | A0 事务/状态锚点 |
| --- | --- | --- | --- |
| `getCustomerMe` | 服务端派生账户与可用空间/角色；只返回 ACTIVE 成员关系与 ACTIVE 组织 | 只读 | T-S1-04；§7.1 账户/组织/成员状态机 |
| `listCustomerBusinessNumbers` | 脱敏号码、绑定/主体/开通/实名投影；稳定 `subject.createdAt desc, bindingId asc` | 只读 cursor | §7.1 主体/绑定/开通状态 |
| `getCustomerSubject` | 开通、试用、当前同意要求、认领与计数摘要；无风险内部细节 | 只读 | §7.1、§7.3 开通/试用/同意 |
| `createCustomerSubjectClaim` | 追加已验证个人认领；风险检查先于 replay；owner 只在 null 时设置 | `Idempotency-Key` = `subject_claim.request_id` (1-128) | T-S1-06；`subject_claim` append-only |
| `createCustomerSubjectActivation` | 显式 ACCEPT/DECLINE；ACCEPT 落同意、开通与首次试用并冻结结果；DECLINE 是提交的 typed refusal（409 `ACTIVATION_CONSENT_DECLINED`）：不落接受同意、开通或试用，同一 `activationEventId` replay 返回同一 refusal；`200 ActivationResult` 仅为 ACCEPT 成功，无 DECLINE 200 schema | body `activationEventId` (1-128) | T-S2-13；§7.3 开通/同意/试用 |
| `listCustomerEvidence` | 只返回可访问证据安全元数据；稳定 `createdAt desc, evidenceId desc` | 只读 cursor | §7.4 证据状态 |
| `getCustomerEvidence` | 单条证据安全元数据与已验证 artifact 摘要；不渲染媒体 | 只读 | §7.4；§4.40/§4.41/§4.42 安全投影 |
| `listOperationsSubjects` | 固定 `ADMIN`/`OPS` 只读；六项结构化过滤；稳定 `createdAt desc, subjectId desc`；cursor 绑定过滤摘要；无完整号码/自由文本 | 只读 cursor | §7.1 主体/绑定/认领/风险/开通 |
| `getOperationsSubject` | 脱敏主体详情：绑定、认领、风险、开通、试用、证据/订单计数 | 只读 | §7.1、§7.3 |
| `suspendOperationsSubject` | 仅 `OPS`；仅 `ACTIVE -> SUSPENDED`；保留历史、不重发试用、追加审计 | `Idempotency-Key` = A0 operation action event (1-128) | T-S2-09、T-S1-03；`ck_activation_transition_matrix` |
| `resumeOperationsSubject` | 仅 `OPS`；仅 `SUSPENDED -> ACTIVE`；永不补发/重发试用；追加审计 | 同上 | T-S2-09、T-S1-03 |
| `listOperationsActivations` | 开通/同意/试用关联只读；稳定 `occurredAt desc, transitionId desc` | 只读 cursor | §7.3 |
| `listOperationsIvrSessions` | IVR 会话/通话元数据只读；结构化 `subjectId/callId/taskId/status` 过滤；稳定 `createdAt desc, sessionId desc` | 只读 cursor | §7.3 |
| `getOperationsIvrSession` | 单会话版本/决策/通话/leg/事件回执/证据安全链路；只读 | 只读 | §7.3、§7.4 |

## 4. Problem 基座与精确错误码

所有错误使用 RFC 9457 `application/problem+json`。`Problem.code` 是唯一机器分支依据；`Problem.status` 必须与 HTTP status 一致，`Problem.traceId` 为服务端 trace。S1 registry 恰好 15 个 code：

| Code | HTTP | 含义/A0 锚点 | 适用面 |
| --- | ---: | --- | --- |
| `UNAUTHENTICATED` | 401 | 缺失/失效客户或员工会话；不区分用户是否存在 | 五面 |
| `FORBIDDEN` | 403 | 会话有效但角色/资源/访问谓词不允许 | 五面 |
| `NOT_FOUND` | 404 | 资源不存在或当前身份不可见；不返回替代对象 | customer/operations/public-share |
| `VALIDATION_FAILED` | 422 | schema/字段/枚举/长度错误；`errors[]` 定位 | customer/operations/ivr/provider-callback |
| `CURSOR_INVALID` | 400 | cursor 不可解析、被篡改或跨面使用 | customer/operations |
| `CURSOR_FILTER_MISMATCH` | 400 | 过滤条件变化后复用旧 cursor | customer/operations |
| `CLAIM_RISK_BLOCKED` | 409 | `subject_risk_state` OPEN 风险先于 replay；不返回历史成功 | customer |
| `CLAIM_REPLAY_CONFLICT` | 409 | `request_id` replay 身份与已存结果不一致 | customer |
| `CLAIM_CONTEXT_CONFLICT` | 409 | 同账户重复认领的组织/绑定/号码/proof/method 上下文不同 | customer |
| `ACTIVATION_STATE_CONFLICT` | 409 | O3/O4 只允许指定 OPERATIONS 迁移；状态或 revision 不匹配 | operations |
| `CONSENT_REQUIRED` | 409 | 当前开通提示/政策缺失、过期或内容无效；不落接受同意 | customer |
| `ACTIVATION_CONSENT_DECLINED` | 409 | 显式 DECLINE 的提交 typed refusal：不落接受同意、开通或试用；同一 `activationEventId` replay 返回同一 refusal | customer |
| `RATE_LIMITED` | 429 | 平台限流/配额拒绝；S1 不冻结数值速率、窗口或重试策略 | 五面 |
| `SERVICE_UNAVAILABLE` | 503 | 依赖/容量/维护不可用；S1 不冻结供应商或 retry-after 语义 | 五面 |
| `INTERNAL` | 500 | 安全通用失败；不返回供应商/内部细节 | 五面 |

精确约束：

- 每个 operation 必须列出并显式暴露公共矩阵：`VALIDATION_FAILED`（422）、`RATE_LIMITED`（429）、`SERVICE_UNAVAILABLE`（503），以及会话/授权与 `INTERNAL`（`default`→500）通道；`default` 只满足 `INTERNAL` 回退，不能替代任何已列出 4xx/429/503 的显式响应。
- `createCustomerSubjectClaim` 的 409 集合恰为 `CLAIM_RISK_BLOCKED`、`CLAIM_REPLAY_CONFLICT`、`CLAIM_CONTEXT_CONFLICT`；proof 无效为 `VALIDATION_FAILED` 422；剔除公共码后的领域 Problem 码集合也恰为此三码。
- `suspendOperationsSubject` / `resumeOperationsSubject` 剔除公共码后的领域 Problem 码集合恰为 `NOT_FOUND` 与 `ACTIVATION_STATE_CONFLICT`。
- 任何 `*`/`_*` 通配 Problem code、裸冲突 409、无码冲突或多余/缺失的精确集合成员均使校验失败。
- A0 IVR 决策 reason `TASK_CONFLICT`、`CALL_IN_PROGRESS`、`TASK_CLAIMED`、`TASK_UNAVAILABLE`、`CONFIRMATION_NOT_ALLOWED` 不是 Problem code；它们只允许出现在后续 `IvrDecision.reasonCode`。

## 5. Cursor、排序与投影约定

- 所有列表使用不透明 `cursor`（1-512）与 `limit`（默认 20、最大 100；V1 API-governance 默认，服务端可 clamp）。无 offset/total。
- 每个 `*Page` 必含 `items`、`nextCursor`、`hasMore`；`nextCursor` 可空，`hasMore` 必填。
- cursor 绑定稳定排序键、最后记录内部排序值与规范化过滤摘要；不得是 offset、SQL key、HMAC、时间戳或可枚举 ID 明文。
- `listOperationsSubjects`：排序恰为 `createdAt desc, subjectId desc`；过滤仅 `subjectId`、`source`、`claimStatus`、`activationStatus`、`realNameStatus`、`riskState`；禁止完整号码/自由文本搜索。
- 投影只返回脱敏号码或内部 ID；禁止对象键、bucket、对象版本、`cryptoKeyRef`、token/hash/salt、provider payload、session token、完整号码与证件。S1 不返回任何媒体凭证或一次性秘密。

## 6. 授权边界摘要

- 客户端请求不得提交 `accountId`、`ownerAccountId`、`ownerOrganizationId`、`role`、`workspaceRole`、价格、金额、支付状态、证据固结状态、录音归属、任务领取或试用结果。
- 客户 operation 只使用 `customerSession`；运营 operation 只使用 `employeeSession`，角色只通过 `x-onecall-required-roles` 表达为固定 `ADMIN`/`OPS`。
- `C4` 只支持已验证个人认领；企业所有权在新政策批准前 fail-closed，S1 不暴露 `organizationId` 认领输入。
- `C5` 的 `decision` 只允许显式 `ACCEPT`/`DECLINE`；账户、工作空间、政策结果和试用结果全部服务端派生。
- `O3`/`O4` 只能由 `OPS` 调用，且只写 `OPERATIONS` 来源的 `ACTIVE->SUSPENDED` / `SUSPENDED->ACTIVE`；恢复不重发试用。
- 运营读取只授予脱敏业务元数据，**不授予录音播放权**；S1 无播放 endpoint。

## 7. A0 44 表 catalog 与负例

- `api/openapi/contract-rules.mjs` 与 `check-doc-map.mjs` 从 `docs/14` 的 `<!-- catalog:start -->` / `<!-- catalog:end -->` 块读取 44 个精确 schema-qualified 名，不手工复制。
- 每个 `x-onecall-a0-models` 条目必须精确匹配 catalog；短名、别名、通配符和错误 schema 全部失败。
- 校验要求 `a0_catalog=44`、`a0_missing=0`、`a0_extra=0`、`a0_duplicate=0`、`a0_invalid=0`、`doc_missing=0`、`doc_extra=0`、`doc_duplicate=0`、`doc_invalid=0`。
- 已知负例（必须被拒绝）：`usage_reservation`、`catalog_*`、`sharing.catalog_version`、`catalog_order.share_access_event`，以及任何未批准 scope `CATALOG_DRAFT` / `TRIAL_POLICY_DRAFT`。
- S1 不新增 A0 表、事务、状态、scope 或事务性读法。

## 8. 具名开放输入（保持原 owner/destination）

| ID | 输入 | S1 只做到 | Owner / Destination |
| --- | --- | --- | --- |
| Q1 | 员工认证源、MFA、凭据轮换 | `employeeSession` 不透明 scheme + 固定角色扩展 | Security/Backend；安全设计与运营 IAM |
| Q6 | 微信支付商户/应用标识、payload、回调留存 | 仅保留回调基座，S1 无回调 operation | Payments/Backend；支付适配器 |
| Q7 | SIP 线路/接入号/录音格式/缓冲/时钟 | 仅保留 IVR 基座，S1 无 IVR operation | Telephony/IVR；IVR 协议 |
| Q8 | 授权/上报时长容差、SLO/超时 | S1 不定义容差或默认值 | IVR/Backend/QA；IVR 协议与容量 |
| Q10 | 录音告知/同意文案与提示版本生命周期 | 只传 policy/prompt 引用，不选择文案 | Product/Legal；同意政策 |
| Q12 | 分享 code KDF、准入上限、过期/限流默认 | S1 无分享 operation | Security/Product；安全设计与分享政策 |
| Q14 | 实名供应商/证据/复验策略 | 只投影实名状态，不定义证件字段 | Security/Product；实名适配器 |
| Q15 | 事件域 wire 值与格式 | S1 无 IVR 事件 operation | Backend/Security；IVR 协议 |
| Q18 | 30 秒任务 TTL 配置键/审批/调度 | S1 无任务 operation | Product/Backend；IVR 协议与运营配置 |
| Q19 | 微信 appid/openid/unionid 映射 | `customerSession` 不透明；无登录/合并 endpoint | Identity/Backend；认证设计 |
| Q22 | Redis 播放 ticket TTL、时钟偏差、撤销传播 | S1 无媒体授权 operation | Security/Sharing；安全设计与分享政策 |
| Q25 | `authorizationExpiresAt` 窗口/时钟偏差 | S1 无授权窗口值 | IVR/Entitlement/Backend；IVR 协议 |
| Q26 | typed snapshot schema 版本/IVR wire 映射 | S1 只读投影安全元数据 | IVR/Backend/Security；IVR 协议 |
| Q27 | 组织成员任务创建/领取角色矩阵 | S1 无任务 operation；运营角色固定 ADMIN/OPS | Organization-access/Security；授权设计 |
| Q28 | IVR 本地 B 路唯一键/恢复协议 | S1 无 IVR operation | Telephony/IVR；IVR 协议 |
| N-1 | `catalog_product` 初始身份供给/迁移来源 | S1 无目录 operation | Product/Operations/catalog-order；目录数据供给 |
| N-2 | 线下客服联系方式/处理说明部署值 | 仅 UI/部署配置；无 API、无 A0 表 | Product/Operations；部署配置与 UI |
| API-DEF-1 | V1 分页默认值 | 固定 `limit` default 20 / max 100 为契约默认 | Backend/API governance |
| R-A0-O20-PUBLISHER | `trial_policy_version.published_by_employee_id` 为 NOT NULL 实际发布者事实 | S1 不涉及 O20；S2 必须原样携带该运行时前置 | A0 schema/catalog-order/entitlement-metering |

## 9. 可复现验证命令

在 `api/openapi/` 下执行：

```bash
npm ci
npm run lint
npm run bundle
npm run generate
npm run typecheck
npm run doc-map
npm run check
npm run check:s1
```

预期：

- Redocly `0 problems`；`servers` 恰好一项相对 `url: /`。
- `check:s1` 输出 S1 精确集合与 `customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`。
- `doc_missing=0`、`doc_extra=0`、`doc_duplicate=0`、`doc_invalid=0`、`a0_missing=0`、`a0_extra=0`、`a0_duplicate=0`、`a0_invalid=0`。
- 生成 d.ts 在 `tmp/openapi-client/`（已忽略、不提交）；`tsc --noEmit` strict 通过。
- `npm run check:s2` / `check:s3` 已按累计 32/49 精确集合定义；在 S1 完成前应因缺失后续 operation 而失败，且不得被放宽来伪造 S1 通过。

## 10. 静态与运行时限制

本文和 canonical 契约不能证明：

- 客户/员工会话签发、微信/OTP、MFA、mTLS、签名、时间窗、nonce 和能力令牌解析。
- 数据库唯一约束、条件修订、锁顺序、事务回滚、认领风险与 replay 的并发结果、cursor 在真实数据变化下的稳定性。
- 支付/回调、IVR 线路/任务/录音/媒体、分享准入/凭证、COS/Redis。
- 生成 TypeScript 只是类型投影，不是可发布客户端，也不证明前端按 audience 隔离、日志脱敏或重试策略。
- O20 的运行时 DRAFT 插入受 `R-A0-O20-PUBLISHER` 阻塞；S1 不涉及。
