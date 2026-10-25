# 生产安全设计规范（S1：敏感数据保护与密钥生命周期）

> Work unit：`A3-production-security-design-spec`
>
> Slice：S1 — 敏感数据保护、密钥层级、轮换/恢复与历史 replay 兼容
>
> Task label：`a3-security-s1-impl-ds-20261025-01`
>
> Base：accepted plan checkpoint `0d77f7c9eaa5e41defab92cc32b8c02a1a376c0c`
>
> Machine model：`security/v1/security-design-v1.json`
>
> Validator：`security/validate.mjs`（零第三方依赖，Node 22）
>
> Status：S1 author-complete；S2 身份/签名与 S3 媒体/审计/脱敏未实现，也未在本文件中提前冻结。

## 1. 状态、来源关系与静态限制

本文件是 A3 安全语义的人类 canonical 规范；`security/v1/security-design-v1.json` 是同一语义的机器投影，二者由 `security/validate.mjs` 做双向一致性校验。S1 只冻结数据与密钥真相：哪些数据由哪个 purpose/key 保护、如何 envelope/HMAC、如何轮换/恢复、如何保持历史解密/查找与 A2 replay 兼容。

本文件与模型是静态设计产物，**不声称**任何运行时或云能力已经执行、验证或可用。没有真实 KMS、Secret Manager、CAM、COS、Redis、PostgreSQL、IVR 线路、日志平台、analytics 或支付供应商调用发生。fixtures 只验证模型级确定性行为，不是生产验收矩阵、渗透测试或容量结论。

A0、A1、A2 的 accepted 契约保持只读：

- A0 仍是表、列、状态、named CHECK、事务、敏感字段目录和 audit/outbox 契约的权威。
- A1 仍是 operation、path、schema、Problem code、security scheme 与 credential 字段的权威。
- A2 仍是 IVR 协议、事件身份、replay、outbox、终态与 exactly-once 的权威。
- S1 只新增安全语义，不新增表、列、状态、endpoint、header、Problem code、员工 role、云区域/SKU/容量/WORM 声明或运行时实现。

## 2. 范围与切片边界

S1 冻结：

- 20 个 key class 的 custody、purpose、algorithm、data mapping、rotation 与 recovery；
- 28 个 data class 与 A0 §8.1 敏感字段目录的逐行映射；
- 23 个 typed deferred/capability 输入，精确分区为 18 个未改变的 A0 Q 子集 row 与 5 个 A3-origin `CV-*` row；
- AES-256-GCM envelope profile、12-byte nonce、16-byte tag、purpose/data-class AAD；
- 独立 HMAC-SHA-256 lookup key 与 lowercase 64-hex 表示；
- key states、purpose separation、rotation/recovery、历史解密/查找与 replay 兼容；
- 恰好 6 个 S1 invariant `INV-A3-01` 至 `INV-A3-06`。

S1 不包含、也不授权提前实现：认证域与 audience、service identity、请求签名、nonce/timestamp/replay 拒绝、媒体凭证、COS 私有策略、审计访问、高风险控制和脱敏。后续切片未在本文件中给出模型 section、fixture ID 或 invariant 文本；任何提前出现都必须被 validator 拒绝。

## 3. 上游权威与禁改边界

| 上游 | 只读权威 | S1 使用方式 |
| --- | --- | --- |
| `docs/14` | 44 表 catalog、28 行敏感字段目录、envelope/HMAC 形状、audit 契约、28 个 open input | 逐行映射 data class、复刻 envelope/HMAC 形状、复制 18 个 Q row 的 input/owner/destination |
| A1 bundle | 49 operation、6 security scheme、五个 `x-onecall-audience`、credential 字段 | 只做 drift 断言；不读取或新增模型 section |
| A2 model | 12 messages、11 eventTypes、22 identities、15 invariants、event registry digest | 只做 drift 断言并保持 replay 语义不变 |

S1 不修改 `docs/12` 至 `docs/16`、A0/A1/A2 accepted 产物、OpenAPI source、prototype、screenshots、`shared/onecall-business.js`、controller 或历史 review。A2 event registry digest 保持 `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9` 不变。

## 4. 密钥层级、用途分离与状态

逻辑层级固定为：**Tencent KMS CMK（root KEK）→ 每 purpose DEK（AES-256-GCM）→ 数据**。HMAC key 与签名 key 是独立分支，不参与加密层级。CMK 材料不出 KMS；DEK 由 KMS GenerateDataKey 生成并由 CMK 包裹（CV-1 验证），若该能力不可用则由 CSPRNG 生成 DEK 后立即用 KMS Encrypt 包裹，具体路径必须记录在 CV-1。wrapped DEK 只存 Secret Manager version，plaintext 只在进程内存。

每个 DEK 只绑定一个 purpose；`key_ref` 在 registry 中映射到唯一 purpose 与 data-class 集合。解密 API 必须接收 expected purpose/dataClass；registry 不匹配即失败。HMAC key 只用于 equality/uniqueness/indexed lookup；不得用于加密、签名或会话。IVR signing、provider verification、upload signing、playback signing 各自独立。

六个逻辑 key state 及其允许操作：

| state | allowed operations | 语义 |
| --- | --- | --- |
| `ACTIVE` | encrypt, decrypt, lookup, sign, verify | 新写入、解密、查找和签名使用 active version |
| `DECRYPT_ONLY` | decrypt | 历史加密版本只允许解密 |
| `LOOKUP_ONLY` | lookup | 历史 HMAC 版本只允许 equality lookup |
| `VERIFY_ONLY` | verify | 历史签名版本只允许验签 overlap |
| `DISABLED` | 无 | 紧急禁用；只能走授权恢复 |
| `DESTROYED` | 无 | 不可恢复；所有读取 fail closed |

状态-用途不匹配、unknown state、unknown key version 或 cross-purpose 使用必须 fail closed。任何 key/secret 不得写入数据库业务列（除 envelope 内 opaque `key_ref`）、代码、镜像、日志、trace、audit snapshot、Outbox、fixture 期望输出或 public contract。

## 5. Envelope 与 HMAC wire contract（保持 A0 形状）

**Envelope**

- 文本形状严格为 `v1.<key_ref>.<base64url(nonce || authenticated_ciphertext_and_tag)>`，与 A0 regex `^v1\.[A-Za-z0-9._:/-]+\..+$` 完全兼容。
- 解析：第一个 `.` 前必须是字面量 `v1`；最后一个 `.` 后是 base64url body；中间 `key_ref` 是 opaque 值，可包含 `.`，字符集遵守 A0。不得截断、重排或 URL-decode。
- `base64url` 使用无 padding 的 `[A-Za-z0-9_-]+`；body 至少包含 nonce + tag + 1 byte ciphertext。
- AEAD 固定为 AES-256-GCM；nonce 12 bytes；tag 16 bytes；AAD 固定为 UTF-8 ASCII `onecall:v1:<purposeId>:<dataClassId>`。
- 生产 nonce 必须由 CSPRNG 每次加密重新生成。fixtures 中的固定 nonce 只是确定性 evaluator 输入，不是生产 nonce 策略。
- unknown/malformed/retired `key_ref` 必须 fail closed，不得回退到明文或扫描。
- `customer_iam.auth_identity.key_version` 必须等于该行 envelope `key_ref` 对应的整数 version；不一致视为数据完整性错误。

**HMAC**

- 形状为 lowercase 64-hex，独立 HMAC-SHA-256 key，输入为规范化值的 UTF-8 bytes；不加盐、不截断、不使用普通 SHA-256。
- HMAC 只用于 equality/uniqueness/indexed lookup；不得 returned、logged、audited 或作为 public key。
- `accessNumberHmac` 使用与 caller/number HMAC 相同的 approved phone-number key 和 contract；只作为 ephemeral fingerprint input，不持久化、不输出。
- masked 值由服务端生成，仅用于 display；mask 不具 collision resistance，不能作为 replay identity。

## 6. Key registry（20）

下列 20 行与模型 `keyClasses` 固定顺序完全一致。`dataClasses` 列按 accepted plan 保留原始映射记号；`K02-K09` 表示 root KEK 覆盖的密钥分支，`C04`、`C06`、`C09`、`C10` 是 credential 引用，不表示 data class。

<!-- security-key-map:start -->
| keyClass | custodyStorage | purpose | algorithm | dataClasses | rotation | recovery |
| --- | --- | --- | --- | --- | --- | --- |
| KMS_CMK_DATA | Tencent KMS (region CV-1) | root KEK for DEK | KMS symmetric CMK | K02-K09 | CMK version rotation CV-1 | re-enable/restore CV-1 |
| DEK_PROVIDER_SUBJECT | KMS-wrapped Secret Manager version + memory | provider subject AEAD | AES-256-GCM/12/16/AAD | D01 | new version; old decrypt-only | SM version + KMS |
| DEK_IDENTITY_PROFILE | KMS-wrapped Secret Manager version + memory | legal name/id number AEAD | AES-256-GCM/12/16/AAD | D02 | new version; old decrypt-only | SM version + KMS |
| DEK_VERIFICATION_REF | KMS-wrapped Secret Manager version + memory | verification ref AEAD | AES-256-GCM/12/16/AAD | D02/D05 | new version; old decrypt-only | SM version + KMS |
| DEK_PHONE_NUMBER | KMS-wrapped Secret Manager version + memory | caller/callee/number AEAD | AES-256-GCM/12/16/AAD | D04/D06/D07/D09 | new version; old decrypt-only | SM version + KMS |
| DEK_IVR_DECISION | KMS-wrapped Secret Manager version + memory | frozen decision payload AEAD | AES-256-GCM/12/16/AAD | D08 | new writes; append-only old versions decrypt-only | SM version + KMS |
| DEK_CALL_EVENT | KMS-wrapped Secret Manager version + memory | call event payload AEAD | AES-256-GCM/12/16/AAD | D10 | new version; old decrypt-only | SM version + KMS |
| DEK_PROVIDER_PAYLOAD | KMS-wrapped Secret Manager version + memory | provider payload AEAD | AES-256-GCM/12/16/AAD | D11 | new version; old decrypt-only | SM version + KMS |
| DEK_INTERNAL_PAYLOAD | KMS-wrapped Secret Manager version + memory | audit/outbox/idempotency encrypted snapshot | AES-256-GCM/12/16/AAD | D27/D28 | new version; old decrypt-only | SM version + KMS |
| COS_CMK_MEDIA | Tencent KMS + COS SSE-KMS | media object at rest | COS SSE-KMS CV-4 | D19 | COS/KMS rotation CV-4 | COS/KMS CV-4 |
| HMAC_PROVIDER_SUBJECT | Secret Manager | provider subject lookup | HMAC-SHA-256/256-bit | D01 | dual-read; old lookup-only | SM version |
| HMAC_ID_NUMBER | Secret Manager | id number lookup | HMAC-SHA-256/256-bit | D02 | dual-read; old lookup-only | SM version |
| HMAC_PHONE_NUMBER | Secret Manager | caller/binding/access number lookup | HMAC-SHA-256/256-bit | D04/D06/D07/D09;accessNumberHmac | dual-read; old lookup-only | SM version |
| HMAC_EMPLOYEE_LOGIN | Secret Manager | employee login lookup | HMAC-SHA-256/256-bit | D03 | dual-read; old lookup-only | SM version |
| HMAC_EMPLOYEE_AUTH_SUBJECT | Secret Manager | employee auth subject lookup | HMAC-SHA-256/256-bit | D03 | dual-read; old lookup-only | SM version |
| HMAC_SHARE_DESIGNATED_MOBILE | Secret Manager | designated mobile proof | HMAC-SHA-256/256-bit | D24 | new grant uses new version; old snapshot retains its version | SM version |
| IVR_REQUEST_SIGNING | Secret Manager | IVR request HMAC | HMAC-SHA-256/256-bit | C04 | active + verify-only overlap | SM version |
| PROVIDER_CALLBACK_VERIFICATION | Secret Manager/cert store | provider signature verify | vendor profile Q6 | C06 | provider rotation Q6 | provider/SM |
| MEDIA_UPLOAD_SIGNING | Secret Manager/COS signing material | upload credential issuance | COS profile CV-4 | C09 | overlap until uploadExpiresAt | SM/COS |
| MEDIA_PLAYBACK_SIGNING | Secret Manager | playback token signing | HMAC-SHA-256/256-bit | C10 | overlap until max TTL + Q22 | SM version |
<!-- security-key-map:end -->

每个 key row 在机器模型中还携带唯一 owner、allowed states 与 `failClosedStatus=503`。owner 取拥有该数据事实或 custody 的既有领域；signing/verification key 的 owner 是对应安全边界，而不是新增服务或组织角色。`K01` 是 root CMK；`K17`、`K18`、`K19`、`K20` 是彼此独立的 signing/verification key，不与 DEK、HMAC 或会话复用。

## 7. Data-class registry（28）

下列 28 行必须与 A0 §8.1 的 28 个敏感字段 row 逐行相等；`a0Inventory` 列保留 A0 原始文本（含反引号），validator 直接与 A0 解析结果比较。不得新增未注册敏感字段；若实现发现新的敏感字段，必须停止并回到上游设计。

<!-- security-data-map:start -->
| dataClass | a0Inventory | representation | keyClass | owner | projection | auditAccess | rotationReplay |
| --- | --- | --- | --- | --- | --- | --- | --- |
| D01 | `customer_iam.auth_identity.provider_subject_ciphertext` / `provider_subject_hmac` | AEAD+HMAC | K02/K11 | customer_iam | never returned; HMAC is not a public key | service access redacted; employee read SECURITY_SENSITIVE_READ | old key_ref decryptable; HMAC dual-read/rebuild |
| D02 | `customer_iam.identity_profile.legal_name_ciphertext` / `id_number_ciphertext` / `id_number_hmac` / `verification_ref_ciphertext` | AEAD+HMAC | K03/K12/K04 | customer_iam | never returned plaintext; status or masked value only | same as D01 | mutable row may re-encrypt; HMAC rebuild |
| D03 | `operations_iam.employee_account.login_name_hmac` / `auth_subject_hmac` | HMAC+mask | K14/K15 | operations_iam | masked login only | employee auth access; HMAC never returned | dual-read/rebuild |
| D04 | `subject_number.caller_number_binding.number_ciphertext` / `number_hmac` / `number_masked` | AEAD+HMAC+mask | K05/K13 | subject_number | masked number only | service access redacted | mutable re-encrypt/rebuild |
| D05 | `subject_number.subject_claim.verification_ref_ciphertext` | AEAD | K04 | subject_number | never returned | claim/audit redacted | append-only; old key retained |
| D06 | `ivr.outbound_call_task.caller_number_hmac` / `callee_ciphertext` / `callee_masked` | AEAD+HMAC+mask | K05/K13 | ivr | masked callee; plaintext only in IVR internal decision payload | service access redacted | mutable task re-encrypt |
| D07 | `ivr.inbound_session.caller_ciphertext` / `caller_number_hmac` / `caller_masked` / `access_number_masked` | AEAD+HMAC+mask | K05/K13 | ivr | masked only | service access redacted | mutable session re-encrypt |
| D08 | `ivr.ivr_decision.response_snapshot` / `response_payload_ciphertext` | AEAD+redacted JSON | K06 | ivr | redacted snapshot only; replay decrypts the same payload | employee/support read audited | append-only; old key retained |
| D09 | `ivr.call_record.caller_ciphertext` / `caller_masked` / `callee_ciphertext` / `callee_masked` / `access_number_masked` | AEAD+HMAC+mask | K05/K13 | ivr | masked only | service access redacted | immutable party guard; never rewrite |
| D10 | `ivr.call_event.payload_ciphertext` / `payload_redacted` | AEAD+redacted JSON | K07 | ivr | receipt/redacted only | service access redacted | append-only; never rewrite |
| D11 | `catalog_order.payment_event.payload_ciphertext` | AEAD | K08 | catalog_order | never returned or logged | restricted read + audit | append-only |
| D12 | `catalog_order.payment_order.merchant_order_no` / `provider_transaction_id`; `payment_event.provider_event_id` / `transaction_id` | internal ID | - | catalog_order | not a public key; restricted audit | restricted access | provider-scoped; never public |
| D13 | `catalog_order.catalog_version.snapshot` / `snapshot_sha256` | redacted JSON+hash | - | catalog_order | commercial fact; no protected value | publication audit | immutable on publish |
| D14 | `catalog_order.customer_order.snapshot` / `snapshot_sha256` | redacted JSON+hash | - | catalog_order | frozen commercial fact | order access | immutable |
| D15 | `entitlement_metering.trial_policy_version.snapshot` / `snapshot_sha256` | redacted JSON+hash | - | entitlement_metering | policy facts only | publication audit | immutable version |
| D16 | `entitlement_metering.entitlement_grant.snapshot` / `snapshot_sha256` | redacted JSON+hash | - | entitlement_metering | historical grant facts | access/audit | immutable |
| D17 | `entitlement_metering.usage_fact.snapshot` / `snapshot_sha256` | redacted JSON+hash | - | entitlement_metering | usage metadata | settlement audit | append-only |
| D18 | `activation_consent.consent_record.snapshot` / `snapshot_sha256` | redacted JSON+hash | - | activation_consent | no protected value | consent audit | append-only |
| D19 | `evidence.media_object.bucket` / `object_key` / `object_version_id` / `crypto_key_ref` | COS internal ref + hash | K10/CV-4 | evidence | never returns key/bucket/version/URL | media credential/object audit | COS versioning; old CMK version decryptable |
| D20 | `evidence.recording.declared_sha256` / `validated_sha256` | hash only | - | evidence | hash prefix only if approved | recording audit | immutable validation facts |
| D21 | `evidence.evidence_record.identity_at_capture` / `consent_version` / `snapshot` | reference + redacted JSON+hash | - | evidence | no raw PII/object key/URL | evidence audit | immutable capture |
| D22 | `evidence.evidence_event.payload_redacted` / `payload_sha256` | redacted JSON+hash | - | evidence | no media bytes/raw parties | append-only audit | immutable |
| D23 | `sharing.evidence_share_grant.capability_token_hash` / `code_hash` / `code_salt` | token digest + KDF salt | C07/C08 | sharing | plaintext return-once only; never re-displayed | share create/rotate/revoke audit | capability token not rebuildable; code generation rotation |
| D24 | `sharing.evidence_share_grant.content_snapshot` / `content_sha256` | redacted JSON+hash+HMAC | K16 | sharing | no object key/URL/raw phone/unshared evidence | share transition audit | immutable snapshot; old HMAC version until grant/retention |
| D25 | `sharing.share_admission.admission_key` | opaque derived key | - | sharing | never returned | admission access event | deterministic by shareGrantId+admissionRequestId |
| D26 | `sharing.share_access_event.risk_digest` / `payload_redacted` | hash + redacted JSON | - | sharing | no raw IP/phone/code/token/full URL | SHARE_ACCESS event | append-only |
| D27 | `audit.audit_fact.before_snapshot` / `after_snapshot` | redacted JSON or AEAD | K09 when encrypted | operations-audit | closed redacted projection only | audit read access event | append-only; never rewrite |
| D28 | `platform.outbox_event.payload_redacted` / `payload_ciphertext` / `platform.idempotency_record.result_snapshot` | redacted JSON or AEAD | K09 when encrypted | platform | consumers only authorized fields | dispatch access redacted | immutable payload |
<!-- security-data-map:end -->

## 8. 轮换、恢复、历史解密/查找与 replay 兼容

**轮换**

- DEK：新建 version 为 `ACTIVE`；旧 version 为 `DECRYPT_ONLY`。新写使用 active；历史读使用 envelope `key_ref`。仅对 A0 允许 UPDATE 且无 immutability guard 的 mutable row 做 lazy/idempotent re-encrypt；append-only/frozen row 永不重写。
- CMK：按 CV-1 验证的能力执行；旧 CMK/version 保持 decrypt-only；不得要求重写 append-only row。若 provider 不支持自动 rotation，则新建 CMK + 受控 re-wrap，CV-1 记录。
- HMAC：新 version `ACTIVE`，旧 version `LOOKUP_ONLY`。mutable row 重建 HMAC；append-only/frozen row 保留旧 HMAC。lookup 依次计算 active + 仍被保留 row 引用的历史 key 候选值，只做 equality query，不扫描明文。
- share designated mobile HMAC：`content_snapshot` 内保存 key version；旧 version 保留至 grant 过期/retention；新 grant 使用 active。
- signature key：新 `ACTIVE`，旧 `VERIFY_ONLY`；重叠窗口内两者可验签；重叠结束后旧 `DISABLED`；unknown keyId 直接拒绝。该语义在 S1 只作为 key registry 的 rotation 属性存在，请求签名本身不在 S1 冻结。
- 任何 rotation 必须有 audit event、双人/授权控制（具体组织流程 Q1/Q17）与失败关闭；不得在 rotation 中输出 plaintext/key material。

**恢复**

- `DISABLED`：通过 KMS/Secret Manager 授权 re-enable；audit `SECURITY_KEY_RECOVER`；应用不提供绕过。
- pending deletion：仅当 CV-1/CV-2 证明 provider 支持取消/恢复时执行；否则视为不可恢复。
- `DESTROYED`：该 data class 不可解；所有读取 fail closed；事件/事故流程由 Security/Operations 负责；不得从备份重建 plaintext 或使用弱化 fallback。
- Secret Manager version 不可用：fail closed；不得回退到旧明文配置、环境变量或镜像内 secret。

**历史解密/查找**

- 解密必须先校验 envelope 形状、key_ref 存在、key state 允许 decrypt、purpose/dataClass 匹配；任一失败 fail closed。
- HMAC 查找只返回 equality 命中；若所需 key version 全部不可用，返回该面既有 503，不返回空结果冒充“不存在”。
- A2 `ivr_decision` replay 必须用其 envelope `key_ref` 解密同一 payload，返回同一 redacted snapshot；不得重新选择 action、重新计算余额或创建新 reservation。
- `auth_identity.key_version` 与 envelope key_ref 必须一致；不一致视为数据完整性错误，fail closed。

**Replay 兼容**

- 轮换不得改变 A1 wire、A2 semantic identity、redacted snapshot、`response_sha256`、`request_fingerprint` 的语义输入。
- A2 `accessNumberHmac` fingerprint 在 HMAC 轮换后必须能通过“active + 历史 key 候选”复现；旧 key 保留至少覆盖 call/decision replay 窗口（Q25）与 append-only retention（Q3）。若无法保证，必须停止并报告，不得静默改变 fingerprint contract。
- 传输 nonce/timestamp 不属于 A2 semantic identity；重试可重新生成，eventId/payload 不变。
- 若 replay 解密因 key 不可用失败，返回该面既有 503；不得返回空 payload、伪造成功或重新执行。

## 9. Deferred inputs（23 = 18 A0 Q 子集 + 5 A3 CV）

23 行精确分区为 18 个未改变的 A0 Q 子集 row 与 5 个 A3-origin `CV-1` 至 `CV-5` cloud-capability verification row。Q row 的 input/owner/destination 与 `docs/14` §14 逐行相等；CV row 不占用 Q 编号，也不修改 A0 §14。validator 分别断言两组，不把 23 行当作同一来源。

<!-- security-deferred-map:start -->
| inputId | input | valueKind | a3Treatment | owner | destination |
| --- | --- | --- | --- | --- | --- |
| Q1 | Employee authentication source, MFA policy and credential rotation | IDENTITY_PROVIDER/POLICY | Freeze independent employee domain/audience/fixed roles only; no MFA/IdP implementation | Security/Backend | Security design and operations IAM implementation |
| Q3 | Legal retention, erasure rights, backup deletion and audit retention | LEGAL_RETENTION | Freeze rotation/recovery must satisfy retention first; no retention period selection | Legal/Security | Retention and deletion policy; production launch review |
| Q6 | WeChat payment merchant/app identifiers, payload fields and callback retention | VENDOR_POLICY | Freeze adapter verification order/idempotency/fail-closed only; no vendor field/algorithm | Payments/Backend | Payment adapter and OpenAPI/security design |
| Q7 | SIP line/access number, callee display, recording format/size, buffer and time synchronization | VENDOR_POLICY/NUMERIC | Freeze mTLS/signature interface and fail-closed only; no line/format/certificate values | Telephony/IVR | IVR protocol and line integration |
| Q8 | Authorized-versus-reported duration tolerance and IVR SLO/timeouts | NUMERIC | Do not define tolerance/SLO/timeout | IVR/Backend/QA | IVR protocol and capacity/acceptance work |
| Q10 | Recording notice/consent wording and prompt-version lifecycle | POLICY | Pass prompt/policy reference only | Product/Legal | Consent policy and IVR prompt approval |
| Q11 | COS object-lock/WORM capability, storage class and media retention | CLOUD_CAPABILITY/LEGAL | Fix private bucket/SSE-KMS/versioning requirements; no WORM/region claim | Infrastructure/Security | Storage/security/capacity work |
| Q12 | Share-code KDF parameters, admission limits, expiry defaults and rate-limit thresholds | NUMERIC/POLICY | Freeze salt/generation/KDF interface only; no parameters | Security/Product | Security design and sharing policy |
| Q13 | Audit/Outbox retention, DLQ thresholds and replay operations | NUMERIC/POLICY | Freeze audit/access classification only; no retention selection | Operations/Backend | Operations runbook and observability design |
| Q14 | Subject real-name verification provider, evidence and re-verification policy | VENDOR_POLICY | Project status only; no document field/provider | Security/Product | Verification adapter and identity design |
| Q17 | Flyway role provisioning, schema ownership transfer and least-privilege grant execution | PLATFORM | Freeze logical service/DB privilege boundary only | Platform/Database | Deployment/security design and production readiness |
| Q18 | Production configuration key/approval for the configurable pending-task TTL, config versioning/audit and scheduler cadence | CONFIG/NUMERIC | Use the frozen 30s default; no configuration key definition | Product/Backend | IVR protocol and operations configuration |
| Q19 | WeChat appid/openid/unionid mapping and multi-app identity association | IDENTITY_PROVIDER | Freeze customer domain/audience only; no login/merge definition | Identity/Backend | Authentication design and security review |
| Q22 | Redis playback-ticket TTL, clock-skew allowance and revocation propagation window | NUMERIC | Freeze credential fail-closed/TTL upper-bound semantics; no values | Security/Sharing | Security design and sharing policy |
| Q23 | Function/role provisioning order and whether the deployment role can create SECURITY DEFINER functions | PLATFORM | Freeze role/ownership contract only; no deployment identity selection | Platform/Database/Security | Deployment/security design and production readiness |
| Q25 | Exact `authorizationExpiresAt` window, clock-skew allowance and configurable value | NUMERIC | Freeze timestamp/nonce verification order; no window selection | IVR/Entitlement/Backend | IVR protocol, configuration and capacity/acceptance work |
| Q27 | Exact organization role(s) that hold the task-create/claim-revalidation permission | ROLE_MATRIX | Keep A0/A1 lifecycle/access checks; no role matrix expansion | Organization-access/Security | Authorization design and S2 policy tests |
| Q28 | Durable IVR-local B-leg operation key, persistence protocol and lost-response recovery | PLATFORM | Read existing local A2 recovery only; no new protocol | Telephony/IVR | IVR protocol and line integration; no business schema expansion |
| CV-1 | KMS region/edition symmetric CMK, envelope/wrap, rotation/disable/recover/pending-delete | CLOUD_CAPABILITY | Logical K01-K09 profile frozen; capability to verify | Infrastructure/Security | Q2 closure + COS/KMS validation |
| CV-2 | Secret Manager region/edition versioning/rotation/dual-version | CLOUD_CAPABILITY | Logical custody frozen; capability to verify | Infrastructure/Security | Q2 closure + deployment security |
| CV-3 | CAM role/policy least privilege and workspace isolation | CLOUD_CAPABILITY | Six service identities frozen; provisioning to verify | Infrastructure/Security | Q17/Q23 deployment |
| CV-4 | COS private bucket/SSE-KMS/versioning/presign (method/object/expiry/size)/private endpoint/object lock | CLOUD_CAPABILITY | Private bucket requirements fixed; upload primary shape is COS presigned PUT gated on this verification; otherwise fail closed and stop; region/SKU/WORM to verify | Infrastructure/Security | Q11 storage/capacity |
| CV-5 | CloudAudit/log service operation evidence and retention | CLOUD_CAPABILITY | PostgreSQL audit is the source of truth; provider logs supplementary | Infrastructure/Security/Observability | Q13 observability |
<!-- security-deferred-map:end -->

不发明：region、SKU、价格、容量、WORM/object-lock 能力、MFA/IdP、法律留存期、生产 TTL/rate-limit/replay-window 数值、organization permission matrix 或 vendor-specific signature/payload/KDF 参数。所有此类值保持 typed deferred input，并附 owner 与 destination。

## 10. S1 invariants（恰好 6）

| ID | Statement |
| --- | --- |
| INV-A3-01 | Every protected datum has exactly one dataClass, owner, representation, key/HMAC class and audit/access coverage; no unregistered sensitive field exists. |
| INV-A3-02 | Encryption and HMAC keys are strictly separated; no key crosses purpose; cross-purpose decrypt or lookup must fail. |
| INV-A3-03 | Envelope shape, key_ref parsing and AEAD/AAD are fixed; unknown/malformed/retired key_ref, wrong purpose or decryption failure must fail closed with no plaintext fallback. |
| INV-A3-04 | HMAC is lowercase 64-hex under an independent key; it is used only for equality, uniqueness and indexed lookup and is never returned, logged or public. |
| INV-A3-05 | Every key and credential has an owner, custody, state, rotation and recovery path; secret material never enters a database, code, image or log. |
| INV-A3-06 | Rotation never rewrites append-only or frozen rows; historical keys are retained until retention; A2 replay returns the same payload/decision without re-selection or a second reservation. |

S1 不包含其他 invariant 的文本、ID 或断言。后续切片的行为、fixture 与 invariant 文本必须在各自 Gate 中单独冻结。

## 11. 确定性验证、fixtures 与诚实限制

验证命令（在 repo root）：

```bash
cd api/openapi && npm ci && npm run check:s3
cd /mnt/d/codex-call
node protocol/ivr/validate.mjs --stage s3 \
  --model protocol/ivr/v1/ivr-protocol-v1.json \
  --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s1 \
  --model security/v1/security-design-v1.json \
  --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures
```

S1 fixtures（6，恰 2 个显式负例）：

| Fixture | 场景 | 关键断言 |
| --- | --- | --- |
| `sec-s1-01-envelope-purpose-roundtrip` | D01/D02/D04/D06/D07/D08/D09/D10/D11 加解密/HMAC | envelope pattern；AAD purpose/dataClass；HMAC 64-hex；无 plaintext/key_ref 输出；roundtrip 一致 |
| `sec-s1-02-hmac-index-equality` | 同值/不同 purpose HMAC lookup | 同值同 HMAC；不同 purpose 不同；equality 命中/未命中；无明文扫描 |
| `sec-s1-03-cross-purpose-denied` | 负例：D01 ciphertext 配 D02 key/AAD、错误 purpose、错 purpose HMAC | `SECURITY_KEY_PURPOSE_MISMATCH`；零 plaintext；零写入 |
| `sec-s1-04-rotation-historical-replay` | K02/K05/K13 轮换；历史解密/HMAC 双读；A2 decision replay | 旧 version 可解/lookup；新写新 version；append-only 不改写；同一 payload；无第二 reservation/settlement |
| `sec-s1-05-key-unavailable-failclosed` | 负例：unknown/disabled/destroyed key、依赖不可用、恢复 | 既有 503；无 plaintext fallback；audit event；无状态写入；destroyed 不可恢复 |
| `sec-s1-06-append-only-rotation-safety` | frozen row 重写、retention 未定 retire、stage 边界 | 拒绝 UPDATE；旧 version `DECRYPT_ONLY`/`LOOKUP_ONLY`；无 A0 变更；23=18+5；6 invariants 且无未来文本 |

fixtures 使用 `TEST_ONLY_*` 合成标签与固定 nonce；密钥字节由 label 的 SHA-256 确定性派生。它们不包含真实 secret、URL、credential、phone、identity 或 cloud identifier。

validator 独立解析模型、文档 marker、A0/A1/A2 与 fixtures，并计算 fixture 行为；不读取 `expected` 来决定结果，也不存在脱离解析输入的成功打印。缺少/多余/重复/非法 row、错误 purpose/key/AAD、畸形 envelope/HMAC、错误 Q/CV 来源、unknown key/version/state、提前 stage 内容、fixture ID/slice 不匹配与上游 drift 都会失败。

诚实限制：没有运行真实 KMS、Secret Manager、CAM、COS、Redis、PostgreSQL、IVR 线路、日志平台、analytics 或供应商；不能证明生产 nonce 的 CSPRNG、真实 envelope 解密、真实云 rotation/recovery、数据库 append-only trigger、审计事务回滚或媒体对象行为。fixtures 是安全模型级断言，不是生产验收或渗透测试。

## 12. 残余风险与完成声明

| ID | 内容 | Owner / Destination |
| --- | --- | --- |
| `R-A3-HMAC-ROTATION-REPLAY` | HMAC 轮换与 A2 `accessNumberHmac` fingerprint/冻结 HMAC 的运行时兼容；需历史 key 候选与 retention | Security/IVR/Backend；integration + Q3/Q25 |
| `R-A3-KMS-CAPABILITY` | KMS/Secret Manager envelope、rotate、recover、dual-version 未验证（CV-1/CV-2） | Infrastructure/Security；Q2/CV validation |
| `R-A3-COS-CAPABILITY` | COS region/private endpoint/SSE-KMS/versioning/presign/object-lock/WORM 未验证（CV-4/Q11） | Infrastructure/Security；storage/capacity |
| `R-A3-KEY-DESTRUCTION` | destroyed key 不可恢复；无明文备份/escrow 除非另行批准 | Security/Infrastructure；incident/recovery policy |
| `R-A3-LEGAL-RETENTION` | retention/erasure/backup 未选（Q3） | Legal/Security；launch review |
| `R-A3-NO-WORM` | 不声称 COS WORM/object-lock；未来需 Q11 验证 | Infrastructure/Security；storage policy |
| `R-A3-REGION-SKU` | 不选 region/SKU/price/capacity | Infrastructure/Security；capacity/deployment |

S1 完成信号：20 keys、28 data rows、23 deferred（18 Q + 5 CV）、envelope/HMAC 规则、rotation/recovery/replay 规则、6 invariants、6 fixtures 与 validator 均已落盘；A0/A1/A2 计数与 digest 由 validator 断言不变；没有新增表、列、状态、endpoint、header、Problem code、role 或生产数值。

本文件与模型不声称任何运行时或云实现。S1 author completion 后停止；不进入 S2/S3，不 stage、commit、push、创建/修改 PR 或 merge。

### Completion report template

每个 slice 的 Gateflow 报告记录：task label、provider/model、gate、base commit、slice ID；精确 changed files 与 SHA-256；registry/marker/fixture inventory；安全决策；已执行命令与 observed exit/output；A0/A1/A2 drift 证据；负例与零副作用计数；deferred/residual 的 owner/destination；无运行时/云声明；以及未 stage/commit/push/PR/review 的声明。
