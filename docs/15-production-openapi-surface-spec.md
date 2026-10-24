# 生产 OpenAPI 五面契约治理与 A0 映射（S1+S2）

> Work unit：`A1-production-openapi-surfaces-spec`
>
> Slice：S2（累计 32 operation：S1 14 + S2 18）
>
> Canonical 契约：`api/openapi/onecall-v1.openapi.yaml`
>
> A0 只读基线：`docs/14-production-data-model-and-flyway-schema-spec.md`
>
> 本文只做人类治理、operation 索引与 A0/状态映射，不复制完整 schema，也不构成运行时服务、数据库、鉴权、支付、IVR、COS/Redis 或客户端实现声明。

## 0. 范围与静态限制

- 本文描述累计 32 个 operation：S1 客户 `C1/C2/C3/C4/C5/C13/C14` 与运营 `O1..O7`；S2 客户 `C9/C10/C11/C12` 与运营 `O8..O21`。
- 五个信任面固定为 `customer`、`operations`、`ivr`、`public-share`、`provider-callback`；S1/S2 只冻结前两面，其余三面仅保留 tag/security 基座。
- 唯一契约源是 `api/openapi/onecall-v1.openapi.yaml`。`servers` 恰好一项相对根 `url: /`；版本前缀写在各 operation 路径中。
- 本切片只有静态契约与静态校验。数据库唯一约束、锁顺序、事务、鉴权、支付、IVR、媒体与客户端运行时均未被证明。
- 本文与配套静态校验不构成暂存、提交、推送、PR 或运行时生产就绪声明。

## 1. 五面与信任边界

| Surface | 前缀 | S2 累计 operation 数 | 认证/信任 | 明确不授予 |
| --- | --- | ---: | --- | --- |
| customer | `/api/v1/customer/` | 11 | 不透明 `customerSession` bearer；服务端派生账户/工作空间 | 号码归属、客户端价格、支付成功、实名结果、证据固结 |
| operations | `/api/v1/operations/` | 21 | 独立 `employeeSession`；固定 `ADMIN`/`OPS` | 客户身份、录音播放、任意事实改写、已发布内容改写 |
| ivr | `/api/v1/ivr/` | 0 | `ivrMtls` + `ivrSignature`（后续切片） | 客户账户、价格、支付状态、任务被领取后的重放授权 |
| public-share | `/api/v1/public/shares/` | 0 | 路径 `capabilityToken`；可选 `customerSession`（后续切片） | 分享创建者身份、对象键、未授权录音内容 |
| provider-callback | `/api/v1/callbacks/` | 0 | `providerCallbackMtls` + `providerCallbackSignature`（后续切片） | 订单金额、支付成功、权益到账 |

共享 security schemes 在 canonical 源中一次性定义：`customerSession`、`employeeSession`、`ivrMtls`、`ivrSignature`、`providerCallbackMtls`、`providerCallbackSignature`。S1/S2 不使用后四种，但基座必须存在以避免后续切片复制或改名。

## 2. 累计 Operation 索引与 A0 模型

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
| listCustomerCatalogOffers | GET | /api/v1/customer/catalog/offers | customer | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line` |
| createCustomerOrder | POST | /api/v1/customer/orders | customer | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `catalog_order.customer_order`, `catalog_order.payment_order`, `activation_consent.consent_record`, `activation_consent.service_activation`, `customer_iam.identity_profile` |
| listCustomerOrders | GET | /api/v1/customer/orders | customer | `catalog_order.customer_order`, `catalog_order.payment_order`, `entitlement_metering.entitlement_issuance` |
| getCustomerOrder | GET | /api/v1/customer/orders/{orderId} | customer | `catalog_order.customer_order`, `catalog_order.payment_order`, `activation_consent.consent_record` |
| listOperationsCatalogProducts | GET | /api/v1/operations/catalog/products | operations | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line` |
| getOperationsCatalogProduct | GET | /api/v1/operations/catalog/products/{productId} | operations | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line` |
| publishOperationsCatalogVersion | POST | /api/v1/operations/catalog/versions/{versionId}/publications | operations | `catalog_order.catalog_version`, `audit.audit_fact`, `platform.outbox_event` |
| offsaleOperationsCatalogVersion | POST | /api/v1/operations/catalog/versions/{versionId}/offsales | operations | `catalog_order.catalog_version`, `audit.audit_fact`, `platform.outbox_event` |
| listOperationsTrialPolicies | GET | /api/v1/operations/trial-policies | operations | `entitlement_metering.trial_policy_version` |
| createOperationsTrialPolicyDraft | POST | /api/v1/operations/trial-policies | operations | `entitlement_metering.trial_policy_version`, `operations_iam.employee_account`, `audit.audit_fact` |
| publishOperationsTrialPolicy | POST | /api/v1/operations/trial-policies/{policyVersionId}/publications | operations | `entitlement_metering.trial_policy_version`, `audit.audit_fact`, `platform.outbox_event` |
| disableOperationsTrialPolicy | POST | /api/v1/operations/trial-policies/{policyVersionId}/disables | operations | `entitlement_metering.trial_policy_version`, `audit.audit_fact`, `platform.outbox_event` |
| listOperationsOrders | GET | /api/v1/operations/orders | operations | `catalog_order.customer_order`, `catalog_order.payment_order`, `entitlement_metering.entitlement_issuance` |
| getOperationsOrder | GET | /api/v1/operations/orders/{orderId} | operations | `catalog_order.customer_order`, `catalog_order.payment_order`, `entitlement_metering.entitlement_issuance` |
| listOperationsAuditFacts | GET | /api/v1/operations/audit | operations | `audit.audit_fact` |
| createOperationsCatalogVersionDraft | POST | /api/v1/operations/catalog/versions | operations | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `operations_iam.employee_account`, `audit.audit_fact` |
| replaceOperationsCatalogVersionDraft | PUT | /api/v1/operations/catalog/versions/{versionId}/draft | operations | `catalog_order.catalog_product`, `catalog_order.catalog_version`, `catalog_order.catalog_offer`, `catalog_order.catalog_benefit_line`, `operations_iam.employee_account`, `audit.audit_fact` |
| replaceOperationsTrialPolicyDraft | PUT | /api/v1/operations/trial-policies/{policyVersionId}/draft | operations | `entitlement_metering.trial_policy_version`, `operations_iam.employee_account`, `audit.audit_fact` |
<!-- operation-map:end -->

## 3. Operation 行为与 A0 事务映射

### 3.1 S1 行为（保持不变）

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

### 3.2 S2 行为（C9-C12 / O8-O21）

| Operation | 核心行为 | Idempotency/replay | A0 事务/状态锚点 |
| --- | --- | --- | --- |
| `listCustomerCatalogOffers` | 只投影服务端 ACTIVE 版本/渠道 offer/权益/`versionToken`；稳定 `productCode asc, versionNo desc, catalogOfferId asc`；cursor 绑定 `channel+audience+offerType` digest；不信任 query 价格 | 只读 cursor | §7.3；`uq_catalog_version_active` |
| `createCustomerOrder` | 客户端必须提交显式购买同意 `decision=ACCEPT`（`DECLINE` 不是下单）+ 必填 `policyCode`/`policyVersion`/`promptVersion`/`contentSha256`；服务端派生 workspace/实名/当前 `service_activation=ACTIVE`/purchase consent event ID/金额/快照/支付状态；`Idempotency-Key` 1-475 = A0 `PURCHASE` client key；`UNKNOWN` 阻断二次结账；缺失/过期/DECLINE 同意不落订单 | `Idempotency-Key` (1-475)；服务端派生 purchase consent ID（永不客户端提交） | T-S2-07；§7.3；`ck_catalog_version_published_shape` |
| `listCustomerOrders` | 当前账户/工作空间订单 + `paymentStatus` + 计算 `issuanceStatus`；稳定 `createdAt desc, orderId desc` | 只读 cursor | §7.3；payment-only 状态 |
| `getCustomerOrder` | 单订单安全投影（金额/支付/同意引用/冻结快照）；不返回 provider 原文 | 只读 | T-S2-07；§7.3 |
| `listOperationsCatalogProducts` | `ADMIN`/`OPS` 只读产品列表 + 有界 `versions` 投影（仅 `catalogVersionId`/`versionNo`/`saleStatus`/`offerType`/`audience`）；稳定 `productCode asc, productId asc`；无 benefits/payment/snapshots | 只读 cursor | §7.3 |
| `getOperationsCatalogProduct` | 产品详情 + 版本/offer/权益摘要（含 DRAFT） | 只读 | §7.3 |
| `publishOperationsCatalogVersion` | `ADMIN`；T-S2-14：先旧 ACTIVE→OFFSALE 再候选 ACTIVE；必填可空 `expectedActiveVersionId`（显式 null = 无当前 ACTIVE）+ 必填 `expectedRevision`；stale → `CATALOG_PUBLICATION_CONFLICT`/`PUBLICATION_STALE_VIEW` | state-conditional | T-S2-14；`uq_catalog_version_active`；`uq_catalog_version_no`；`ck_catalog_version_published_shape` |
| `offsaleOperationsCatalogVersion` | `ADMIN`；ACTIVE→OFFSALE 幂等；`expectedRevision`；不改写历史订单 | state-conditional | T-S2-14；`uq_catalog_version_active`；§7.3 |
| `listOperationsTrialPolicies` | 试用策略只读；稳定 `versionNo desc, policyVersionId desc` | 只读 cursor | §7.3；`uq_trial_policy_campaign_version` |
| `createOperationsTrialPolicyDraft` | `ADMIN`；自然键 `(FIRST-TRIAL, versionNo)` + canonical 内容投影；201 首次 / 200 同投影重放 / 409 `TRIAL_POLICY_DRAFT_CONFLICT`；无 `Idempotency-Key`；`enabled` 必填且恒为布尔 `false`（永不 nullable/default true），`publishedAt` 恒为 null；不写 publisher；不接受 `priceCents`/`currency` | state-conditional | `uq_trial_policy_campaign_version`；`uq_trial_policy_enabled`；`ck_trial_policy_campaign`；`ck_trial_policy_version_no`；§7.3；**`R-A0-O20-PUBLISHER`** |
| `publishOperationsTrialPolicy` | `ADMIN`；先禁用旧 enabled 再单条 UPDATE 同时写 publisher/publishedAt/enabled/revision；必填可空 `expectedEnabledPolicyVersionId`（显式 null = 无 enabled）+ 必填 `expectedRevision`；stale → `TRIAL_POLICY_PUBLICATION_CONFLICT`/`TRIAL_POLICY_STALE_VIEW` | state-conditional | T-S2-14；`uq_trial_policy_campaign_version`；`uq_trial_policy_enabled`；`ck_trial_policy_revision` |
| `disableOperationsTrialPolicy` | `ADMIN`；enabled→false 幂等；`expectedRevision`；历史 grant 快照不可改写 | state-conditional | T-S2-14；`uq_trial_policy_enabled`；§7.3 |
| `listOperationsOrders` | 运营订单只读；digest `subjectId+paymentStatus`；稳定 `createdAt desc, orderId desc` | 只读 cursor | §7.3 |
| `getOperationsOrder` | 运营订单详情 + issuance 投影 | 只读 | T-S2-07；§7.3 |
| `listOperationsAuditFacts` | 追加审计只读；稳定 `occurredAt desc, auditId desc`；`beforeSnapshot`/`afterSnapshot` 为闭合可复用 nullable `RedactedAuditSnapshot`（仅可选 `status`/`revision`/`reasonCode`，`additionalProperties: false`，`minProperties: 1`；无 maps/标识符/PII/payload/provider 字段） | 只读 cursor | §7.3；`audit.audit_fact` |
| `createOperationsCatalogVersionDraft` | `ADMIN`；自然键 `(productId, versionNo)` + canonical 投影；201/200/409 `CATALOG_DRAFT_CONFLICT`；收益行与 channels 创建后 append-only；无 `Idempotency-Key` | state-conditional | `uq_catalog_version_no`；`ck_catalog_version_published_shape`；§7.3 |
| `replaceOperationsCatalogVersionDraft` | `ADMIN`；仅 DRAFT/never-published 可变商业内容；`expectedRevision` + 投影；现有渠道 version token 仅服务端重生成且非公共输入/输出（无 channel-token 字段或 mutation endpoint）；stale → `CATALOG_DRAFT_STALE_VIEW`；不可变键/收益行/channels 变更 → `CATALOG_DRAFT_CONFLICT` | state-conditional | `uq_catalog_version_no`；`ck_catalog_version_published_shape`；§7.3 |
| `replaceOperationsTrialPolicyDraft` | `ADMIN`；仅 never-published；`expectedRevision` + 投影；stale → `TRIAL_POLICY_DRAFT_STALE_VIEW`；campaign/version 不可变；不写 publisher/publishedAt/enabled；不接受 `priceCents`/`currency` | state-conditional | `uq_trial_policy_campaign_version`；`ck_trial_policy_revision`；§7.3；**`R-A0-O20-PUBLISHER`** |

### 3.3 DRAFT 候选 canonical content projection（O18-O21）

- 目录版本：`{product_id, version_no, offer_type, audience, price_cents, currency, billing_cycle, installments, grant_interval_months, snapshot_sha256, benefit_lines[{entitlement_type, quantity, validity_days, grant_interval_months}] sorted by entitlement_type, channels sorted}`；`snapshot_sha256` 由服务端从这些语义字段生成。DRAFT 投影不返回 `versionToken`：现有渠道 version token 仅服务端按渠道重生成且非公共输入/输出（无单数 draft token、无 channel-token 字段或 mutation endpoint）。
- 试用策略：`{campaign_key, version_no, granted_seconds, valid_days, snapshot_sha256}`。
- 创建：同自然身份 + 同投影 → 200 既有 DRAFT；不同投影或非 DRAFT/never-published → 409 `*_DRAFT_CONFLICT`。
- 替换：`expectedRevision` 匹配且当前投影等于请求投影的重放 → 200 当前 DRAFT（不再写）；stale → `*_DRAFT_STALE_VIEW`；非 DRAFT/不可变内容 → `*_DRAFT_CONFLICT`。
- O18-O21 一律 `state-conditional`；不得声明 `Idempotency-Key`；不得映射 `platform.idempotency_record`；不得引用 `platform.idempotency_begin`/`platform.idempotency_record`；未批准 scope `CATALOG_DRAFT`/`TRIAL_POLICY_DRAFT` 必须被拒绝。

## 4. Problem 基座与精确错误码

所有错误使用 RFC 9457 `application/problem+json`。`Problem.code` 是唯一机器分支依据；`Problem.status` 必须与 HTTP status 一致，`Problem.traceId` 为服务端 trace。累计 registry 恰好 30 个 code：

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
| `PURCHASE_CONSENT_REQUIRED` | 409 | 当前 PURCHASE 同意缺失/无效；不落接受同意或订单；T-S2-07 | customer |
| `PURCHASE_CONSENT_STALE` | 409 | 当前 PURCHASE 提示/政策过期或内容不匹配；T-S2-07 | customer |
| `PURCHASE_CONSENT_DECLINED` | 409 | 显式 PURCHASE DECLINE；不落接受同意或订单；T-S2-07 | customer |
| `REAL_NAME_REQUIRED` | 422 | 普通客户下单前实名前置；T-S2-07 | customer |
| `ACTIVATION_NOT_ACTIVE` | 403 | 购买主体当前 `service_activation.status` 非 ACTIVE；T-S2-07 | customer |
| `ORDER_UNKNOWN_PAYMENT_PENDING` | 409 | 同主体/offer 存在 `UNKNOWN` 支付订单，须先查询原订单；§7.3 | customer |
| `IDEMPOTENCY_CONFLICT` | 409 | `PURCHASE` scope 同键语义指纹变化；T-S2-07 | customer |
| `CATALOG_DRAFT_CONFLICT` | 409 | DRAFT 目录候选自然键冲突、非 DRAFT、同身份内容变化或不可变约束拒绝 | operations |
| `CATALOG_DRAFT_STALE_VIEW` | 409 | 替换 DRAFT 目录候选时 `expectedRevision` 不匹配且不能证明已应用 | operations |
| `TRIAL_POLICY_DRAFT_CONFLICT` | 409 | DRAFT 试用策略候选自然键冲突、非 never-published 或同身份内容变化 | operations |
| `TRIAL_POLICY_DRAFT_STALE_VIEW` | 409 | 替换 DRAFT 试用策略时 `expectedRevision` 不匹配且不能证明已应用 | operations |
| `CATALOG_PUBLICATION_CONFLICT` | 409 | T-S2-14 并发/非预期当前 ACTIVE 版本；所有者发布锁 | operations |
| `PUBLICATION_STALE_VIEW` | 409 | T-S2-14 期望 ACTIVE 版本/修订不再匹配；败者不重试 | operations |
| `TRIAL_POLICY_PUBLICATION_CONFLICT` | 409 | T-S2-14 并发/非预期 enabled published 版本 | operations |
| `TRIAL_POLICY_STALE_VIEW` | 409 | T-S2-14 期望 enabled 版本/修订不再匹配 | operations |
| `RATE_LIMITED` | 429 | 平台限流/配额拒绝；不冻结数值速率、窗口或重试策略 | 五面 |
| `SERVICE_UNAVAILABLE` | 503 | 依赖/容量/维护不可用；不冻结供应商或 retry-after 语义 | 五面 |
| `INTERNAL` | 500 | 安全通用失败；不返回供应商/内部细节 | 五面 |

精确约束：

- 每个 operation 必须列出并显式暴露公共矩阵：`VALIDATION_FAILED`（422）、`RATE_LIMITED`（429）、`SERVICE_UNAVAILABLE`（503），以及会话/授权与 `INTERNAL`（`default`→500）通道；`default` 只满足 `INTERNAL` 回退，不能替代任何已列出 4xx/429/503 的显式响应。
- `createCustomerSubjectClaim` 的 409 集合恰为 `CLAIM_RISK_BLOCKED`、`CLAIM_REPLAY_CONFLICT`、`CLAIM_CONTEXT_CONFLICT`；proof 无效为 `VALIDATION_FAILED` 422；剔除公共码后的领域 Problem 码集合也恰为此三码。
- `suspendOperationsSubject` / `resumeOperationsSubject` 剔除公共码后的领域 Problem 码集合恰为 `NOT_FOUND` 与 `ACTIVATION_STATE_CONFLICT`。
- `createCustomerOrder` 剔除公共码后的领域 Problem 码集合恰为 `IDEMPOTENCY_CONFLICT`、`ORDER_UNKNOWN_PAYMENT_PENDING`、`PURCHASE_CONSENT_REQUIRED`、`PURCHASE_CONSENT_STALE`、`PURCHASE_CONSENT_DECLINED`、`ACTIVATION_NOT_ACTIVE`、`REAL_NAME_REQUIRED`。
- `publishOperationsCatalogVersion` / `offsaleOperationsCatalogVersion` 领域集合恰为 `NOT_FOUND`、`CATALOG_PUBLICATION_CONFLICT`、`PUBLICATION_STALE_VIEW`。
- `publishOperationsTrialPolicy` / `disableOperationsTrialPolicy` 领域集合恰为 `NOT_FOUND`、`TRIAL_POLICY_PUBLICATION_CONFLICT`、`TRIAL_POLICY_STALE_VIEW`。
- `createOperationsCatalogVersionDraft` 领域集合恰为 `NOT_FOUND`、`CATALOG_DRAFT_CONFLICT`；`replaceOperationsCatalogVersionDraft` 恰为 `NOT_FOUND`、`CATALOG_DRAFT_CONFLICT`、`CATALOG_DRAFT_STALE_VIEW`。
- `createOperationsTrialPolicyDraft` 领域集合恰为 `TRIAL_POLICY_DRAFT_CONFLICT`；`replaceOperationsTrialPolicyDraft` 恰为 `NOT_FOUND`、`TRIAL_POLICY_DRAFT_CONFLICT`、`TRIAL_POLICY_DRAFT_STALE_VIEW`。
- 任何 `*`/`_*` 通配 Problem code、裸冲突 409、无码冲突或多余/缺失的精确集合成员均使校验失败。
- A0 IVR 决策 reason `TASK_CONFLICT`、`CALL_IN_PROGRESS`、`TASK_CLAIMED`、`TASK_UNAVAILABLE`、`CONFIRMATION_NOT_ALLOWED` 不是 Problem code；它们只允许出现在后续 `IvrDecision.reasonCode`。

## 5. Cursor、排序与投影约定

- 所有列表使用不透明 `cursor`（1-512）与 `limit`（默认 20、最大 100；V1 API-governance 默认，服务端可 clamp）。无 offset/total。
- 每个 `*Page` 必含 `items`、`nextCursor`、`hasMore`；`nextCursor` 可空，`hasMore` 必填。
- cursor 绑定稳定排序键、最后记录内部排序值与规范化过滤摘要；不得是 offset、SQL key、HMAC、时间戳或可枚举 ID 明文。
- `listOperationsSubjects`：排序恰为 `createdAt desc, subjectId desc`；过滤仅 `subjectId`、`source`、`claimStatus`、`activationStatus`、`realNameStatus`、`riskState`；禁止完整号码/自由文本搜索。
- S2 稳定排序：
  - `listCustomerCatalogOffers`：`productCode asc, versionNo desc, catalogOfferId asc`；digest `channel, audience, offerType`。
  - `listCustomerOrders` / `listOperationsOrders`：`createdAt desc, orderId desc`；O15 digest `subjectId, paymentStatus`。
  - `listOperationsCatalogProducts`：`productCode asc, productId asc`。
  - `listOperationsTrialPolicies`：`versionNo desc, policyVersionId desc`。
  - `listOperationsAuditFacts`：`occurredAt desc, auditId desc`。
- 投影只返回脱敏号码或内部 ID；禁止对象键、bucket、对象版本、`cryptoKeyRef`、token/hash/salt、provider payload、session token、完整号码与证件。S1/S2 不返回任何媒体凭证或一次性秘密。
- 订单 `paymentStatus` 仅 `UNPAID/UNKNOWN/PAID/CLOSED`；`issuanceStatus` 为对 `entitlement_issuance` 的计算投影 `NONE/PENDING/ISSUED/FAILED/MIXED`，不存储在订单上。
- `Money` 为 `{amountCents >= 0, currency: 'CNY'}`；公开 v1 所有货币 `currency` 字段冻结为字面量 `CNY`（请求/响应 schema + 生成兼容断言）；目录 `priceCents: null` 只允许 `QUOTE`。该冻结仅针对公开 v1 surface，不修改 A0 更宽的内部 ISO 货币存储域。

## 6. 授权边界摘要

- 客户端请求不得提交 `accountId`、`ownerAccountId`、`ownerOrganizationId`、`role`、`workspaceRole`、价格、金额、支付状态、证据固结状态、录音归属、任务领取或试用结果。
- 例外且仅例外：O18/O19 DRAFT 候选请求可携带草稿商业 `priceCents`（null 仅 QUOTE）与 `currency`（恒 `CNY`）；O20/O21 与 `OrderCreateRequest` 或其他客户请求一律拒绝这两个字段。
- 客户 operation 只使用 `customerSession`；运营 operation 只使用 `employeeSession`，角色只通过 `x-onecall-required-roles` 表达为固定 `ADMIN`/`OPS`。
- `C4` 只支持已验证个人认领；企业所有权在新政策批准前 fail-closed，S1 不暴露 `organizationId` 认领输入。
- `C5` 的 `decision` 只允许显式 `ACCEPT`/`DECLINE`；账户、工作空间、政策结果和试用结果全部服务端派生。
- `O3`/`O4` 只能由 `OPS` 调用，且只写 `OPERATIONS` 来源的 `ACTIVE->SUSPENDED` / `SUSPENDED->ACTIVE`；恢复不重发试用。
- `C10` 客户端提交 `subjectId`/`catalogOfferId`/`channel`（可选 `expectedVersionToken`）+ 显式购买同意 `decision=ACCEPT`（固定 `ACCEPT`，`DECLINE` 不是下单）+ 必填 `policyCode`/`policyVersion`/`promptVersion`/`contentSha256`；金额、purchase consent event ID、activation、支付与 issuance 全部服务端派生（consent ID 由 authenticated account + `Idempotency-Key` 派生，永不客户端提交/inferred）。
- `O10`/`O11`/`O13`/`O14`/`O18`-`O21` 仅 `ADMIN`；发布/下架/停用/草稿写不提供通用 CRUD 或审批引擎。
- 运营读取只授予脱敏业务元数据，**不授予录音播放权**；S2 无播放 endpoint。
- 已发布目录内容、收益行与历史订单/试用快照不可改写；独立 `catalog_offer` 撤下不提供（OQ-4 deferred）。

## 7. A0 44 表 catalog 与负例

- `api/openapi/contract-rules.mjs` 与 `check-doc-map.mjs` 从 `docs/14` 的 `<!-- catalog:start -->` / `<!-- catalog:end -->` 块读取 44 个精确 schema-qualified 名，不手工复制。
- 每个 `x-onecall-a0-models` 条目必须精确匹配 catalog；短名、别名、通配符和错误 schema 全部失败。
- 校验要求 `a0_catalog=44`、`a0_missing=0`、`a0_extra=0`、`a0_duplicate=0`、`a0_invalid=0`、`doc_missing=0`、`doc_extra=0`、`doc_duplicate=0`、`doc_invalid=0`。
- 已知负例（必须被拒绝）：`usage_reservation`、`catalog_*`、`sharing.catalog_version`、`catalog_order.share_access_event`，任何未批准 scope `CATALOG_DRAFT` / `TRIAL_POLICY_DRAFT`，以及任何发明的 idempotency scope 值，或全文任意层级（root、components、path/operation、request/response 与嵌套 schema）出现的任意未知 `x-onecall-*scope*` 扩展键（fail closed）；普通业务属性名 `scope` 与不含 `scope` 的已批准扩展（如 `x-onecall-idempotency`）不受影响。
- S1/S2 不新增 A0 表、事务、状态、scope 或事务性读法；`platform.outbox_event` 与 `audit.audit_fact` 已在 44 表 catalog 内，供 O10/O11/O13/O14/O18-O21 引用。

## 8. 具名开放输入（保持原 owner/destination）

| ID | 输入 | S1/S2 只做到 | Owner / Destination |
| --- | --- | --- | --- |
| Q1 | 员工认证源、MFA、凭据轮换 | `employeeSession` 不透明 scheme + 固定角色扩展 | Security/Backend；安全设计与运营 IAM |
| Q6 | 微信支付商户/应用标识、payload、回调留存 | 仅保留回调基座，S1/S2 无回调 operation | Payments/Backend；支付适配器 |
| Q7 | SIP 线路/接入号/录音格式/缓冲/时钟 | 仅保留 IVR 基座，S1/S2 无 IVR operation | Telephony/IVR；IVR 协议 |
| Q8 | 授权/上报时长容差、SLO/超时 | 不定义容差或默认值 | IVR/Backend/QA；IVR 协议与容量 |
| Q10 | 录音告知/同意文案与提示版本生命周期 | 只传 policy/prompt 引用，不选择文案 | Product/Legal；同意政策 |
| Q12 | 分享 code KDF、准入上限、过期/限流默认 | S1/S2 无分享 operation | Security/Product；安全设计与分享政策 |
| Q14 | 实名供应商/证据/复验策略 | 只投影实名状态，不定义证件字段 | Security/Product；实名适配器 |
| Q15 | 事件域 wire 值与格式 | S1/S2 无 IVR 事件 operation | Backend/Security；IVR 协议 |
| Q18 | 30 秒任务 TTL 配置键/审批/调度 | S1/S2 无任务 operation | Product/Backend；IVR 协议与运营配置 |
| Q19 | 微信 appid/openid/unionid 映射 | `customerSession` 不透明；无登录/合并 endpoint | Identity/Backend；认证设计 |
| Q22 | Redis 播放 ticket TTL、时钟偏差、撤销传播 | S1/S2 无媒体授权 operation | Security/Sharing；安全设计与分享政策 |
| Q25 | `authorizationExpiresAt` 窗口/时钟偏差 | 无授权窗口值 | IVR/Entitlement/Backend；IVR 协议 |
| Q26 | typed snapshot schema 版本/IVR wire 映射 | 只读投影安全元数据 | IVR/Backend/Security；IVR 协议 |
| Q27 | 组织成员任务创建/领取角色矩阵 | 无任务 operation；运营角色固定 ADMIN/OPS | Organization-access/Security；授权设计 |
| Q28 | IVR 本地 B 路唯一键/恢复协议 | 无 IVR operation | Telephony/IVR；IVR 协议 |
| N-1 | `catalog_product` 初始身份供给/迁移来源 | O18 要求产品已存在；无产品创建 endpoint | Product/Operations/catalog-order；目录数据供给 |
| N-2 | 线下客服联系方式/处理说明部署值 | 仅 UI/部署配置；无 API、无 A0 表 | Product/Operations；部署配置与 UI |
| API-DEF-1 | V1 分页默认值 | 固定 `limit` default 20 / max 100 为契约默认 | Backend/API governance |
| R-A0-O20-PUBLISHER | `trial_policy_version.published_by_employee_id` 为 NOT NULL 实际发布者事实 | S2 O20/O21 原样携带该运行时前置；不暴露、不重命名、不伪造、不求解；不声称运行时插入可用 | A0 schema/catalog-order/entitlement-metering |

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
npm run check:s2
```

预期：

- Redocly `0 problems`；`servers` 恰好一项相对 `url: /`。
- `check:s1` 输出 S1 精确集合与 `customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`。
- `check:s2` 输出 S2 累计精确集合与 `customer=11 operations=21 ivr=0 public-share=0 provider-callback=0 total=32`。
- `doc_missing=0`、`doc_extra=0`、`doc_duplicate=0`、`doc_invalid=0`、`a0_missing=0`、`a0_extra=0`、`a0_duplicate=0`、`a0_invalid=0`。
- 生成 d.ts 在 `tmp/openapi-client/`（已忽略、不提交）；`tsc --noEmit` strict 通过。
- `npm run check:s3` 已按累计 49 精确集合定义；在 S2 完成前应因缺失后续 operation 而失败，且不得被放宽来伪造 S2 通过。

## 10. 静态与运行时限制

本文和 canonical 契约不能证明：

- 客户/员工会话签发、微信/OTP、MFA、mTLS、签名、时间窗、nonce 和能力令牌解析。
- 数据库唯一约束、条件修订、锁顺序、事务回滚、认领风险与 replay 的并发结果、cursor 在真实数据变化下的稳定性。
- 支付/回调、IVR 线路/任务/录音/媒体、分享准入/凭证、COS/Redis。
- 订单创建的真实名校验、购买同意内容、`UNKNOWN` 阻断与 `PURCHASE` 平台幂等在运行时的并发结果。
- 目录/试用 DRAFT 候选的并发唯一性、append-only 收益行触发器与 T-S2-14 发布串行化。
- 生成 TypeScript 只是类型投影，不是可发布客户端，也不证明前端按 audience 隔离、日志脱敏或重试策略。
- O20/O21 的运行时 DRAFT 插入受 `R-A0-O20-PUBLISHER` 阻塞；S2 只冻结公共静态契约与 A0 映射，不声称运行时插入已解决。
