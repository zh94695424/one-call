# 生产 OpenAPI 五面契约治理与 A0 映射（S1+S2+S3）

> Work unit：`A1-production-openapi-surfaces-spec`
>
> Slice：S3（累计 49 operation：S1 14 + S2 18 + S3 17）
>
> Canonical 契约：`api/openapi/onecall-v1.openapi.yaml`
>
> A0 只读基线：`docs/14-production-data-model-and-flyway-schema-spec.md`
>
> 本文只做人类治理、operation 索引与 A0/状态映射，不复制完整 schema，也不构成运行时服务、数据库、鉴权、支付、IVR、COS/Redis 或客户端实现声明。

## 0. 范围与静态限制

- 本文描述累计 49 个 operation：S1 客户 `C1/C2/C3/C4/C5/C13/C14` 与运营 `O1..O7`；S2 客户 `C9/C10/C11/C12` 与运营 `O8..O21`；S3 客户 `C6/C7/C8/C15/C16/C17/C18`、IVR `I1..I6`、公开分享 `P1/P2/P3` 与回调 `K1`。
- 五个信任面固定为 `customer`、`operations`、`ivr`、`public-share`、`provider-callback`；S3 完成后五面全部冻结：不再有“仅保留 tag/security 基座”的空面。
- 唯一契约源是 `api/openapi/onecall-v1.openapi.yaml`。`servers` 恰好一项相对根 `url: /`；版本前缀写在各 operation 路径中。
- 本切片只有静态契约与静态校验。数据库唯一约束、锁顺序、事务、鉴权、支付、IVR、媒体与客户端运行时均未被证明。
- 本文与配套静态校验不构成暂存、提交、推送、PR 或运行时生产就绪声明。

## 1. 五面与信任边界

| Surface | 前缀 | S3 累计 operation 数 | 认证/信任 | 明确不授予 |
| --- | --- | ---: | --- | --- |
| customer | `/api/v1/customer/` | 18 | 不透明 `customerSession` bearer；服务端派生账户/工作空间 | 号码归属、客户端价格、支付成功、实名结果、证据固结 |
| operations | `/api/v1/operations/` | 21 | 独立 `employeeSession`；固定 `ADMIN`/`OPS` | 客户身份、录音播放、任意事实改写、已发布内容改写 |
| ivr | `/api/v1/ivr/` | 6 | `ivrMtls` + `ivrSignature` + `X-ONECALL-TIMESTAMP`/`X-ONECALL-NONCE` | 客户账户、价格、支付状态、任务被领取后的重放授权 |
| public-share | `/api/v1/public/shares/` | 3 | 路径 `capabilityToken`；可选 `customerSession`（仅 `ORGANIZATION_MEMBER`） | 分享创建者身份、对象键、未授权录音内容 |
| provider-callback | `/api/v1/callbacks/` | 1 | `providerCallbackMtls` + `providerCallbackSignature` + `X-ONECALL-TIMESTAMP`/`X-ONECALL-NONCE` | 订单金额、支付成功、权益到账 |

共享 security schemes 在 canonical 源中一次性定义：`customerSession`、`employeeSession`、`ivrMtls`、`ivrSignature`、`providerCallbackMtls`、`providerCallbackSignature`。S3 起五面认证全部投入使用：customer/operations 使用 `customerSession`/`employeeSession`；ivr 使用 `ivrMtls`+`ivrSignature`；public-share 使用路径 `capabilityToken` 加可选 `customerSession`；provider-callback 使用 `providerCallbackMtls`+`providerCallbackSignature`。基座不再有“定义但未使用”的 scheme。

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
| createCustomerOutboundCallTask | POST | /api/v1/customer/outbound-call-tasks | customer | `ivr.outbound_call_task`, `subject_number.caller_number_binding`, `ivr.call_record`, `activation_consent.service_activation` |
| getCustomerOutboundCallTask | GET | /api/v1/customer/outbound-call-tasks/{taskId} | customer | `ivr.outbound_call_task`, `ivr.call_record` |
| cancelCustomerOutboundCallTask | POST | /api/v1/customer/outbound-call-tasks/{taskId}/cancellations | customer | `ivr.outbound_call_task` |
| createCustomerShare | POST | /api/v1/customer/shares | customer | `sharing.evidence_share_grant`, `sharing.share_access_event`, `evidence.evidence_record` |
| listCustomerShares | GET | /api/v1/customer/shares | customer | `sharing.evidence_share_grant`, `sharing.share_access_event` |
| revokeCustomerShare | POST | /api/v1/customer/shares/{shareId}/revocations | customer | `sharing.evidence_share_grant`, `sharing.share_access_event` |
| rotateCustomerShareCode | POST | /api/v1/customer/shares/{shareId}/code-rotations | customer | `sharing.evidence_share_grant`, `sharing.share_access_event` |
| resolveIvrInbound | POST | /api/v1/ivr/inbound/resolve | ivr | `ivr.inbound_session`, `ivr.call_record`, `ivr.outbound_call_task`, `ivr.ivr_decision`, `entitlement_metering.usage_reservation`, `activation_consent.service_activation`, `entitlement_metering.entitlement_balance` |
| confirmIvrActivation | POST | /api/v1/ivr/calls/{callId}/activation-confirmations | ivr | `activation_consent.consent_record`, `activation_consent.service_activation`, `activation_consent.service_activation_transition`, `entitlement_metering.trial_grant`, `ivr.ivr_decision`, `entitlement_metering.usage_reservation` |
| authorizeIvrDirectOutbound | POST | /api/v1/ivr/calls/{callId}/outbound-authorizations | ivr | `ivr.call_record`, `ivr.ivr_decision`, `entitlement_metering.usage_reservation`, `activation_consent.service_activation`, `entitlement_metering.entitlement_balance` |
| ingestIvrCallEvent | POST | /api/v1/ivr/calls/{callId}/events | ivr | `ivr.call_event`, `ivr.call_record`, `ivr.call_leg`, `entitlement_metering.usage_reservation`, `entitlement_metering.usage_fact` |
| declareIvrRecording | POST | /api/v1/ivr/calls/{callId}/recordings | ivr | `evidence.recording`, `evidence.media_object` |
| completeIvrRecording | POST | /api/v1/ivr/calls/{callId}/recordings/{recordingId}/complete | ivr | `evidence.recording`, `evidence.media_object`, `evidence.evidence_record`, `evidence.evidence_event`, `ivr.call_record` |
| getPublicShare | GET | /api/v1/public/shares/{capabilityToken} | public-share | `sharing.evidence_share_grant`, `evidence.evidence_record` |
| createPublicShareAdmission | POST | /api/v1/public/shares/{capabilityToken}/admissions | public-share | `sharing.share_admission`, `sharing.share_access_event`, `sharing.evidence_share_grant`, `subject_number.subject_risk_state` |
| authorizePublicShareMedia | POST | /api/v1/public/shares/{capabilityToken}/admissions/{admissionId}/media-authorizations | public-share | `sharing.share_access_event`, `sharing.share_admission`, `sharing.evidence_share_grant`, `subject_number.subject_risk_state` |
| receivePaymentProviderCallback | POST | /api/v1/callbacks/payment-events | provider-callback | `catalog_order.payment_event`, `catalog_order.payment_order`, `catalog_order.customer_order`, `platform.outbox_event` |
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

### 3.3 S3 行为（C6-C8 / C15-C18 / I1-I6 / P1-P3 / K1）

| Operation | 核心行为 | Idempotency/replay | A0 事务/状态锚点 |
| --- | --- | --- | --- |
| `createCustomerOutboundCallTask` | 创建/替换当前空间唯一非终态待拨任务；要求当前 `service_activation.status=ACTIVE` 且同规范主叫/主体无其他 `ACTIVE`/`UNKNOWN` call；`callerBindingId` 归属服务端重读；callee 服务端规范化；冻结 30 秒 TTL 不续期；不返回明文 callee/历史 callee | `Idempotency-Key` (1-512)；同键同指纹 replay 返回原任务且不延长冻结 TTL；activation/call 检查先于 replay | T-S2-02、T-S2-03；§7.3 唯一非终态任务 |
| `getCustomerOutboundCallTask` | 只读：按 task owner + workspace 过滤，不按最新任务回退；明文 callee、他人/其他空间任务与内部 claim 细节不返回 | 只读 | T-S2-02；§7.3；404 统一 `NOT_FOUND` |
| `cancelCustomerOutboundCallTask` | 仅 `PENDING` 条件取消；已 CLAIMED/非 PENDING → `OUTBOUND_TASK_NOT_CANCELLABLE`；并发取消身份不兼容 → `OUTBOUND_TASK_CONFLICT` | `Idempotency-Key` (1-512) | T-S2-02；§7.3 |
| `createCustomerShare` | 为 `SOLIDIFIED` 证据创建分享；状态条件 `creationRequestId` + 确定性 `shareId`；201 返回 `ShareCreated`（`capabilityToken` 必填；`extractionCode` 仅 `EXTRACTION_CODE` 模式可选返回），200 replay 返回 metadata-only `ShareState`（无 secrets）；`validFrom` 为权威创建时间（无 `createdAt`）；风险/策略检查先于 replay | body `creationRequestId` (1-128)；state-conditional | T-S3-03；§7.4 |
| `listCustomerShares` | 当前账户/空间分享元数据只读；稳定 `validFrom desc, shareId desc`；不返回 token/code/hash/salt/对象键；`evidenceId` 仅创建者语境 | 只读 cursor | T-S3-03；§7.4 |
| `revokeCustomerShare` | 条件撤销到终态 `REVOKED`；stale generation witness → `SHARE_GENERATION_STALE`；已签发短时凭证不召回、不承诺远程缓冲回收 | `Idempotency-Key` (1-128) | T-S3-03；§7.4 |
| `rotateCustomerShareCode` | 仅 `EXTRACTION_CODE` 模式；`LOCKED→ACTIVE` 且 `codeGeneration`/`revocationGeneration` +1；响应为闭合判别结果：`ROTATED` 必填一次性 `extractionCode`，`REPLAYED` 禁止 `extractionCode`，两分支均无 `capabilityToken` | `Idempotency-Key` (1-128) | T-S3-03；§7.4 |
| `resolveIvrInbound` | 建立/复用呼入会话、原子领取唯一任务、创建预占并冻结授权；正常领域结果（含 `REJECT_CALL_IN_PROGRESS`、`REJECT`/`TASK_CONFLICT`）一律 200 `IvrDecision`；409 仅 `SESSION_IDENTITY_MISMATCH` | state-conditional：`callId + INBOUND_RESOLVE` 指纹 replay 首个冻结决策 | T-S2-01、T-S2-03、T-S2-04、T-S2-11；§10.14 |
| `confirmIvrActivation` | 同 `callId` 原 `GUIDE_ACTIVATION` 的按键确认 + 开通/试用 + 再决策（`ACTIVATION_NEXT_ACTION`）；不改写原 resolve、不绕过领取/预占原子性 | body `eventId` (1-128)；同指纹 replay 返回首个冻结决策；200 再决策 | T-S2-13；§10.14 |
| `authorizeIvrDirectOutbound` | 无任务 `ENTER_CALLEE` 直拨确认与预占；成功 null action；非外呼结果（`GUIDE_PURCHASE`/`REJECT` 等）同为 200 `IvrDecision`；不领取任务、不返回非空 taskId | body `eventId` (1-128)；同指纹 replay；授权不因 replay 续期、不回退任务 | T-S2-04、T-S2-11；§10.14 |
| `ingestIvrCallEvent` | 已知父 call/session/leg 追加/收敛事实；回执 `APPLIED`/`QUARANTINED`/`IGNORED`；首终态吸收；未知 call 不建占位 | body `eventId` (1-128)；`(IVR_EVENT, eventId)` 指纹；同身份异指纹 → `EVENT_ID_CONFLICT` | T-S2-12；§9.24 |
| `declareIvrRecording` | 登记唯一 `recordingId` 并签发单对象短时上传指令；客户端声明仅为 declaration fact；上传凭证 `readOnly` + 日志脱敏且非 return-once | state-conditional：`recordingId` 唯一 | T-S3-01；§9.27/§9.28 |
| `completeIvrRecording` | 服务端观察事实校验 → `VALIDATED`/`QUARANTINED`；`UPLOAD_EXPIRED`/`STORAGE_UNKNOWN` 失败关闭；`complete` 不创建/不固结证据 | state-conditional：同身份同事实返回首次完成 | T-S3-01、T-S3-02；§9.27/§9.28 |
| `getPublicShare` | observation-free GET：不写 `PAGE_OPENED`/`PREFETCH_IGNORED`/任何 `share_access_event`；`ORGANIZATION_MEMBER` 无匹配会话时省略 `content`；无效 token 统一 404 `SHARE_TOKEN_INVALID`；有效但已撤销/过期 410 | 只读 GET（observation-free） | T-S3-04；§7.4 |
| `createPublicShareAdmission` | `verification` 判别联合按模式验证后原子计数一次；ticket/会话策略在准入提交后失败 → 503 且不重计；`SHARE_LOCKED` 阻止新准入 | body `admissionRequestId` (1-128)；同键重放重发票据、不重新计数 | T-S3-04；§7.4 |
| `authorizePublicShareMedia` | 为已准入会话签发/重建短时媒体凭证；`ACTIVE` 或已准入 `LOCKED` 均可授权，故**不含 `SHARE_LOCKED`**；凭证由媒体网关校验且非 return-once | body `authorizationRequestId` (1-128)；确定性 access identity | T-S3-05；§7.4 |
| `receivePaymentProviderCallback` | `OpaqueProviderPayload`：验签/时间窗/nonce 后幂等推进支付与订单；成功仅 `CallbackAcknowledgement{accepted, duplicate}`；不向供应商披露内部结果 | 供应商事件身份 + payload 指纹；同事件同指纹 replay | T-S2-08；§9.18、§10.17 |

### 3.4 DRAFT 候选 canonical content projection（O18-O21）

- 目录版本：`{product_id, version_no, offer_type, audience, price_cents, currency, billing_cycle, installments, grant_interval_months, snapshot_sha256, benefit_lines[{entitlement_type, quantity, validity_days, grant_interval_months}] sorted by entitlement_type, channels sorted}`；`snapshot_sha256` 由服务端从这些语义字段生成。DRAFT 投影不返回 `versionToken`：现有渠道 version token 仅服务端按渠道重生成且非公共输入/输出（无单数 draft token、无 channel-token 字段或 mutation endpoint）。
- 试用策略：`{campaign_key, version_no, granted_seconds, valid_days, snapshot_sha256}`。
- 创建：同自然身份 + 同投影 → 200 既有 DRAFT；不同投影或非 DRAFT/never-published → 409 `*_DRAFT_CONFLICT`。
- 替换：`expectedRevision` 匹配且当前投影等于请求投影的重放 → 200 当前 DRAFT（不再写）；stale → `*_DRAFT_STALE_VIEW`；非 DRAFT/不可变内容 → `*_DRAFT_CONFLICT`。
- O18-O21 一律 `state-conditional`；不得声明 `Idempotency-Key`；不得映射 `platform.idempotency_record`；不得引用 `platform.idempotency_begin`/`platform.idempotency_record`；未批准 scope `CATALOG_DRAFT`/`TRIAL_POLICY_DRAFT` 必须被拒绝。

## 4. Problem 基座与精确错误码

所有错误使用 RFC 9457 `application/problem+json`。`Problem.code` 是唯一机器分支依据；`Problem.status` 必须与 HTTP status 一致，`Problem.traceId` 为服务端 trace。累计 registry 恰好 59 个 code（S1/S2 30 + S3 29）：

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
| `OUTBOUND_CALL_IN_PROGRESS` | 409 | 同规范主叫/主体已有 ACTIVE/UNKNOWN call | customer |
| `OUTBOUND_TASK_CONFLICT` | 409 | 非终态任务冲突或 Idempotency-Key 语义指纹变化 | customer |
| `OUTBOUND_TASK_NOT_CANCELLABLE` | 409 | 任务非 PENDING 不可取消 | customer |
| `SHARE_CREATION_CONFLICT` | 409 | 同 creationRequestId 冲突或策略拒绝 | customer |
| `SHARE_ACCESS_POLICY_UNAVAILABLE` | 403 | 分享访问策略依赖不可用 | customer |
| `SHARE_RISK_BLOCKED` | 403 | subject_risk_state OPEN | customer/public-share |
| `SHARE_TRANSITION_CONFLICT` | 409 | 撤销/轮换状态或模式不匹配 | customer |
| `SHARE_GENERATION_STALE` | 409 | expected generation 不匹配 | customer/public-share |
| `SHARE_REVOKED` | 410 | 分享已撤销 | customer/public-share |
| `IVR_ARGUMENT_INVALID` | 400 | IVR 请求参数/指纹非法 | ivr |
| `SESSION_IDENTITY_MISMATCH` | 409 | call/session 身份或指纹冲突 | ivr |
| `CALL_UNKNOWN` | 404 | 未知 callId | ivr |
| `EVENT_ID_CONFLICT` | 409 | 同 eventId 不同语义 | ivr |
| `UPLOAD_OBSERVATION_REQUIRED` | 409 | 缺少服务端观察到的上传事实 | ivr |
| `RECORDING_IDENTITY_CONFLICT` | 409 | recordingId 身份冲突 | ivr |
| `UPLOAD_EXPIRED` | 409 | 上传指令已过期 | ivr |
| `STORAGE_UNKNOWN` | 503 | 存储依赖未知 | ivr |
| `SHARE_TOKEN_INVALID` | 404 | capability token 无效统一 404 | public-share |
| `SHARE_VERIFICATION_FAILED` | 403 | 验证 proof 失败 | public-share |
| `SHARE_WRONG_TENANT` | 403 | ORGANIZATION_MEMBER 租户不匹配 | public-share |
| `SHARE_LOCKED` | 409 | LOCKED 阻止新准入 | public-share |
| `SHARE_ADMISSION_LIMIT_REACHED` | 409 | 准入次数达上限 | public-share |
| `SHARE_SESSION_EXPIRED` | 410 | 准入会话过期 | public-share |
| `SHARE_EXPIRED` | 410 | 分享已过期 | public-share |
| `SHARE_SESSION_POLICY_UNAVAILABLE` | 503 | 会话策略依赖不可用 | public-share |
| `DEPENDENCY_UNAVAILABLE` | 503 | 通用依赖不可用 | public-share |
| `CALLBACK_SIGNATURE_INVALID` | 401 | 回调签名/证书无效 | provider-callback |
| `PAYMENT_EVENT_CONFLICT` | 409 | 支付事件语义冲突 | provider-callback |
| `CALLBACK_RETRY_LATER` | 503 | 供应商应稍后重试 | provider-callback |

精确约束：

- 每个 operation 必须列出并显式暴露公共矩阵：`VALIDATION_FAILED`（422）、`RATE_LIMITED`（429）、`SERVICE_UNAVAILABLE`（503），以及会话/授权与 `INTERNAL`（`default`→500）通道；`default` 只满足 `INTERNAL` 回退，不能替代任何已列出 4xx/429/503 的显式响应。
- `createCustomerSubjectClaim` 的 409 集合恰为 `CLAIM_RISK_BLOCKED`、`CLAIM_REPLAY_CONFLICT`、`CLAIM_CONTEXT_CONFLICT`；proof 无效为 `VALIDATION_FAILED` 422；剔除公共码后的领域 Problem 码集合也恰为此三码。
- `suspendOperationsSubject` / `resumeOperationsSubject` 剔除公共码后的领域 Problem 码集合恰为 `NOT_FOUND` 与 `ACTIVATION_STATE_CONFLICT`。
- `createCustomerOrder` 剔除公共码后的领域 Problem 码集合恰为 `IDEMPOTENCY_CONFLICT`、`ORDER_UNKNOWN_PAYMENT_PENDING`、`PURCHASE_CONSENT_REQUIRED`、`PURCHASE_CONSENT_STALE`、`PURCHASE_CONSENT_DECLINED`、`ACTIVATION_NOT_ACTIVE`、`REAL_NAME_REQUIRED`。
- `publishOperationsCatalogVersion` / `offsaleOperationsCatalogVersion` 领域集合恰为 `NOT_FOUND`、`CATALOG_PUBLICATION_CONFLICT`、`PUBLICATION_STALE_VIEW`。
- `publishOperationsTrialPolicy` / `disableOperationsTrialPolicy` 领域集合恰为 `NOT_FOUND`、`TRIAL_POLICY_PUBLICATION_CONFLICT`、`TRIAL_POLICY_STALE_VIEW`。
- `createOperationsCatalogVersionDraft` 领域集合恰为 `NOT_FOUND`、`CATALOG_DRAFT_CONFLICT`；`replaceOperationsCatalogVersionDraft` 恰为 `NOT_FOUND`、`CATALOG_DRAFT_CONFLICT`、`CATALOG_DRAFT_STALE_VIEW`。
- `createOperationsTrialPolicyDraft` 领域集合恰为 `TRIAL_POLICY_DRAFT_CONFLICT`；`replaceOperationsTrialPolicyDraft` 恰为 `NOT_FOUND`、`TRIAL_POLICY_DRAFT_CONFLICT`、`TRIAL_POLICY_DRAFT_STALE_VIEW`。
- `createCustomerOutboundCallTask` 领域集合恰为 `ACTIVATION_NOT_ACTIVE`、`OUTBOUND_CALL_IN_PROGRESS`、`OUTBOUND_TASK_CONFLICT`；`getCustomerOutboundCallTask` 恰为 `NOT_FOUND`；`cancelCustomerOutboundCallTask` 恰为 `NOT_FOUND`、`OUTBOUND_TASK_NOT_CANCELLABLE`、`OUTBOUND_TASK_CONFLICT`。
- `createCustomerShare` 领域集合恰为 `SHARE_CREATION_CONFLICT`、`SHARE_ACCESS_POLICY_UNAVAILABLE`、`SHARE_RISK_BLOCKED`；`listCustomerShares` 恰为 `CURSOR_INVALID`；`revokeCustomerShare` 恰为 `NOT_FOUND`、`SHARE_TRANSITION_CONFLICT`、`SHARE_GENERATION_STALE`、`SHARE_REVOKED`；`rotateCustomerShareCode` 恰为 `NOT_FOUND`、`SHARE_TRANSITION_CONFLICT`。
- `resolveIvrInbound` / `confirmIvrActivation` / `authorizeIvrDirectOutbound` 领域集合恰为 `IVR_ARGUMENT_INVALID`、`SESSION_IDENTITY_MISMATCH`；`ingestIvrCallEvent` 恰为 `IVR_ARGUMENT_INVALID`、`CALL_UNKNOWN`、`EVENT_ID_CONFLICT`；`declareIvrRecording` 恰为 `IVR_ARGUMENT_INVALID`、`CALL_UNKNOWN`、`RECORDING_IDENTITY_CONFLICT`；`completeIvrRecording` 恰为 `IVR_ARGUMENT_INVALID`、`CALL_UNKNOWN`、`UPLOAD_OBSERVATION_REQUIRED`、`RECORDING_IDENTITY_CONFLICT`、`UPLOAD_EXPIRED`、`STORAGE_UNKNOWN`。
- `getPublicShare` 领域集合恰为 `SHARE_TOKEN_INVALID`、`SHARE_REVOKED`、`SHARE_EXPIRED`；`createPublicShareAdmission` 恰为 `SHARE_VERIFICATION_FAILED`、`SHARE_RISK_BLOCKED`、`SHARE_WRONG_TENANT`、`SHARE_TOKEN_INVALID`、`SHARE_REVOKED`、`SHARE_EXPIRED`、`SHARE_LOCKED`、`SHARE_ADMISSION_LIMIT_REACHED`、`SHARE_GENERATION_STALE`、`SHARE_SESSION_EXPIRED`、`SHARE_SESSION_POLICY_UNAVAILABLE`、`DEPENDENCY_UNAVAILABLE`；`authorizePublicShareMedia` 恰为 `SHARE_RISK_BLOCKED`、`SHARE_WRONG_TENANT`、`SHARE_TOKEN_INVALID`、`SHARE_REVOKED`、`SHARE_EXPIRED`、`SHARE_GENERATION_STALE`、`SHARE_SESSION_EXPIRED`、`SHARE_SESSION_POLICY_UNAVAILABLE`、`DEPENDENCY_UNAVAILABLE`，且**不含** `SHARE_LOCKED`。
- `receivePaymentProviderCallback` 领域集合恰为 `CALLBACK_SIGNATURE_INVALID`、`PAYMENT_EVENT_CONFLICT`、`CALLBACK_RETRY_LATER`。
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
- S3 稳定排序：
  - `listCustomerShares`：`validFrom desc, shareId desc`。
- 投影只返回脱敏号码或内部 ID；禁止对象键、bucket、对象版本、`cryptoKeyRef`、token/hash/salt、provider payload、session token、完整号码与证件。S1/S2 不返回任何媒体凭证或一次性秘密；S3 例外仅限：分享创建/码轮换响应一次性返回 token/code（`x-onecall-return-once`），以及 `RecordingDeclaration.upload.credential` / `ShareAdmission.mediaAuthorization.credential` / `MediaAuthorization.credential` 这三个 `readOnly` + `x-onecall-log-redaction: required` 且**非** return-once 的短时凭证；后续读取接口永不回显 token/code。
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
- 运营读取只授予脱敏业务元数据，**不授予录音播放权**；S1/S2/S3 均无运营播放 endpoint（S3 媒体凭证只在 public-share 面）。
- 已发布目录内容、收益行与历史订单/试用快照不可改写；独立 `catalog_offer` 撤下不提供（OQ-4 deferred）。
- `C6`-`C8` 使用 `customerSession`；客户端提交 `callerBindingId` 与 `callee`（不是 `calleeNumber`），callee 规范化与 TTL 冻结由服务端完成；响应只返回脱敏主叫/被叫，不返回明文 callee 或历史 callee。
- `C15`-`C18` 分享管理：`capabilityToken`/`extractionCode` 只在创建与轮换响应各返回一次，后续读取/撤销接口永不回显；`ShareCodeRotation` 响应不含 `capabilityToken`（生成类型断言强制）。
- `I1`-`I6` 使用 `ivrMtls`+`ivrSignature` 与 `X-ONECALL-TIMESTAMP`/`X-ONECALL-NONCE`；正常领域结果一律 200 `IvrDecision`，`TASK_CONFLICT`/`CALL_IN_PROGRESS`/`TASK_CLAIMED`/`TASK_UNAVAILABLE`/`CONFIRMATION_NOT_ALLOWED` 等 `reasonCode` 不是 Problem code。
- `P1`-`P3` 使用路径 `capabilityToken`（仅服务端存 digest）；`P1` 为 observation-free 且不写 `share_access_event`；`P3` 领域错误集合不含 `SHARE_LOCKED`（LOCKED 只阻止 P2 新准入）。
- `K1` 使用 `providerCallbackMtls`+`providerCallbackSignature`；请求体为 `OpaqueProviderPayload`，适配层验签/规范化/加密留档；响应只给 `accepted`/`duplicate`，不向供应商披露内部结果。

## 7. A0 44 表 catalog 与负例

- `api/openapi/contract-rules.mjs` 与 `check-doc-map.mjs` 从 `docs/14` 的 `<!-- catalog:start -->` / `<!-- catalog:end -->` 块读取 44 个精确 schema-qualified 名，不手工复制。
- 每个 `x-onecall-a0-models` 条目必须精确匹配 catalog；短名、别名、通配符和错误 schema 全部失败。
- 校验要求 `a0_catalog=44`、`a0_missing=0`、`a0_extra=0`、`a0_duplicate=0`、`a0_invalid=0`、`doc_missing=0`、`doc_extra=0`、`doc_duplicate=0`、`doc_invalid=0`。
- 已知负例（必须被拒绝）：`usage_reservation`、`catalog_*`、`sharing.catalog_version`、`catalog_order.share_access_event`，任何未批准 scope `CATALOG_DRAFT` / `TRIAL_POLICY_DRAFT`，以及任何发明的 idempotency scope 值，或全文任意层级（root、components、path/operation、request/response 与嵌套 schema）出现的任意未知 `x-onecall-*scope*` 扩展键（fail closed）；普通业务属性名 `scope` 与不含 `scope` 的已批准扩展（如 `x-onecall-idempotency`）不受影响。
- S1/S2/S3 不新增 A0 表、事务、状态、scope 或事务性读法；`platform.outbox_event` 与 `audit.audit_fact` 已在 44 表 catalog 内，供 O10/O11/O13/O14/O18-O21 与 K1 引用；S3 只引用既有 `ivr`/`sharing`/`evidence`/`entitlement_metering`/`activation_consent`/`subject_number`/`catalog_order` 表。

## 8. 具名开放输入（保持原 owner/destination）

| ID | 输入 | S1/S2/S3 只做到 | Owner / Destination |
| --- | --- | --- | --- |
| Q1 | 员工认证源、MFA、凭据轮换 | `employeeSession` 不透明 scheme + 固定角色扩展 | Security/Backend；安全设计与运营 IAM |
| Q6 | 微信支付商户/应用标识、payload、回调留存 | `K1` 已冻结 `OpaqueProviderPayload` + 验签/确认边界；不定义供应商字段、验签算法或留存时长 | Payments/Backend；支付适配器 |
| Q7 | SIP 线路/接入号/录音格式/缓冲/时钟 | `I1`-`I6` 已冻结动作/事件封套与录音声明类型边界；wire 线路/接入号/录音格式/缓冲/时钟值仍 deferred | Telephony/IVR；IVR 协议 |
| Q8 | 授权/上报时长容差、SLO/超时 | `I1`-`I6` 存在但不定义容差、SLO 或超时默认值 | IVR/Backend/QA；IVR 协议与容量 |
| Q10 | 录音告知/同意文案与提示版本生命周期 | 只传 policy/prompt 引用，不选择文案 | Product/Legal；同意政策 |
| Q12 | 分享 code KDF、准入上限、过期/限流默认 | `C15`-`C18`/`P1`-`P3` 已冻结 `EXTRACTION_CODE` 输入与一次性返回、`maxAdmissions`/`expiresAt` 由请求提供；KDF、准入上限与过期/限流默认值仍 deferred | Security/Product；安全设计与分享政策 |
| Q14 | 实名供应商/证据/复验策略 | 只投影实名状态，不定义证件字段 | Security/Product；实名适配器 |
| Q15 | 事件域 wire 值与格式 | `I4` 已固定 domain 名称与 `eventId`/`eventType`/`legId`/`sequenceNo` 封套；wire 值与 payload 格式仍 deferred | Backend/Security；IVR 协议 |
| Q18 | 30 秒任务 TTL 配置键/审批/调度 | `C6`-`C8` 已冻结 `ttlSeconds`/`expiresAt` 契约与 30 秒源默认；配置键/审批/调度仍 deferred | Product/Backend；IVR 协议与运营配置 |
| Q19 | 微信 appid/openid/unionid 映射 | `customerSession` 不透明；无登录/合并 endpoint | Identity/Backend；认证设计 |
| Q22 | Redis 播放 ticket TTL、时钟偏差、撤销传播 | `P2`/`P3` 已冻结 ticket 失败关闭与不重计语义、服务端派生 expiry；数值 TTL、时钟偏差与撤销传播窗口仍 deferred | Security/Sharing；安全设计与分享政策 |
| Q25 | `authorizationExpiresAt` 窗口/时钟偏差 | `I3`/`I1` 只复制 committed reservation 的绝对时间；无授权窗口默认值 | IVR/Entitlement/Backend；IVR 协议 |
| Q26 | typed snapshot schema 版本/IVR wire 映射 | `I1`-`I6` 固定 `IvrDecision` 字段/枚举与事件封套；schema version/wire 映射仍 deferred | IVR/Backend/Security；IVR 协议 |
| Q27 | 组织成员任务创建/领取角色矩阵 | `C6`-`C8` 已提供任务创建/查询/取消但不定义组织成员角色矩阵；运营角色固定 ADMIN/OPS | Organization-access/Security；授权设计 |
| Q28 | IVR 本地 B 路唯一键/恢复协议 | `I3`/`I4` 已要求 `callId` 授权/幂等与“先持久化再外呼”语义；本地 B 路唯一键/恢复协议仍 deferred | Telephony/IVR；IVR 协议 |
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
npm run check:s3
```

预期：

- Redocly `0 problems`；`servers` 恰好一项相对 `url: /`。
- `check:s1` 输出 S1 精确集合与 `customer=7 operations=7 ivr=0 public-share=0 provider-callback=0 total=14`。
- `check:s2` 输出 S2 累计精确集合与 `customer=11 operations=21 ivr=0 public-share=0 provider-callback=0 total=32`。
- `check:s3`（累计 49 精确集合的权威 stage 校验）输出 `customer=18 operations=21 ivr=6 public-share=3 provider-callback=1 total=49`，并 `PASS check-doc-map (s3)`。
- `doc_missing=0`、`doc_extra=0`、`doc_duplicate=0`、`doc_invalid=0`、`a0_missing=0`、`a0_extra=0`、`a0_duplicate=0`、`a0_invalid=0`（doc/a0 零差异计数）。
- 生成 d.ts 在 `tmp/openapi-client/`（已忽略、不提交）；`tsc --noEmit` strict 通过。

## 10. 静态与运行时限制

本文和 canonical 契约不能证明：

- 客户/员工会话签发、微信/OTP、MFA、mTLS、签名、时间窗、nonce 和能力令牌解析。
- 数据库唯一约束、条件修订、锁顺序、事务回滚、认领风险与 replay 的并发结果、cursor 在真实数据变化下的稳定性。
- 支付/回调、IVR 线路/任务/录音/媒体、分享准入/凭证、COS/Redis。
- 订单创建的真实名校验、购买同意内容、`UNKNOWN` 阻断与 `PURCHASE` 平台幂等在运行时的并发结果。
- 目录/试用 DRAFT 候选的并发唯一性、append-only 收益行触发器与 T-S2-14 发布串行化。
- 生成 TypeScript 只是类型投影，不是可发布客户端，也不证明前端按 audience 隔离、日志脱敏或重试策略。
- O20/O21 的运行时 DRAFT 插入受 `R-A0-O20-PUBLISHER` 阻塞；S2 只冻结公共静态契约与 A0 映射，不声称运行时插入已解决。
- S3 残余风险：`capabilityToken` 出现在 `P1`-`P3` 路径中，edge/应用日志可能记录 secret；契约只能声明脱敏要求，不能证明网关/日志配置。
- S3 残余风险：`K1` 请求体为 `OpaqueProviderPayload`，生成客户端无法静态校验供应商字段；验签、规范化与加密留档由适配层负责，A1 只保证 2xx/Problem/同事件幂等。
- S3 残余风险：IVR wire（线路/接入号/录音格式/事件 payload/签名串/时钟与容差）仍 deferred（Q7/Q8/Q15/Q26/Q28）；`I1`-`I6` 只冻结决策 schema、事件封套与幂等边界。
- S3 残余风险：媒体/会话 ticket TTL、时钟偏差与撤销传播窗口仍 deferred（Q22）；`P2`/`P3` 只冻结失败关闭、不重计与服务端派生 expiry 语义，不证明 Redis/媒体网关行为。
- S3 残余风险：分享准入计数、code 轮换与录音上传完成的并发结果、`STORAGE_UNKNOWN`/`SHARE_SESSION_POLICY_UNAVAILABLE` 等 503 分支均未在真实依赖下验证；cursor 稳定排序（含 `listCustomerShares`）仍是契约声明。
