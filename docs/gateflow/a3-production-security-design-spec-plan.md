# A3 生产安全设计规范实施计划

> Work unit：`A3-production-security-design-spec`
>
> Gate：`plan`（只形成实施计划；不实施、不修改 accepted 契约、不进入 plan review 之后的 Gate）
>
> Task label：`a3-security-plan-ds-20261025-01`
>
> Base：merged `origin/main` commit `a6018f5ebf205dfe9519820c63fddfa43c508256`（PR #4 / A2）
>
> Binding contract：`docs/gateflow/project-execution-control.md` 顶部 A3 binding goal contract / project constraints / slice principle
>
> Design sources：`docs/12` §4/§11/§18；`docs/14` §2.6/§4.5/§8/§10.7/§14；`api/openapi/onecall-v1.openapi.yaml`（49 operation、6 security scheme、五面 audience、媒体凭证 schema）；`docs/15` §1/§5/§6/§8/§10；`docs/16` §4/§6.3/§6.6/§9/§11

## 0. 结论摘要

- 产出一份人类 canonical 安全设计规范 `docs/17-production-security-design-spec.md`、一份机器可读安全模型 `security/v1/security-design-v1.json`、23 个确定性 fixtures，以及零第三方依赖的 Node 22 校验器 `security/validate.mjs`。
- 安全模型冻结 5 个信任面/认证域、7 个 trust boundaries、6 个内部服务身份、20 个密钥类、28 个敏感数据类、10 个凭据类、2 个签名 profile、2 个媒体策略、39 个审计/访问事件、10 个高风险操作、10 个调用类覆盖和 23 个 typed deferred/capability inputs（18 个 A0 Q 子集 + 5 个 A3 `CV-*`）。
- 保持 A0 已接受的 ciphertext envelope `v1.<key_ref>.<base64url(nonce || authenticated_ciphertext_and_tag)>` 与 HMAC-SHA-256 64-hex 查找形状；A3 只补齐 purpose 绑定、AAD、密钥用途、轮换/恢复、历史解密/查找和 replay 兼容，不替换 schema。
- 保持 A1 五面与 6 个 security scheme、49 个 operation、既有 Problem code 和 media/credential 字段；不新增 endpoint、Problem code、schema 字段、employee role 或数据库表/列/状态。
- 保持 A2 `callId`/`eventId`/`recordingId`/`decisionId` 身份、exactly-once 恢复、outbox 语义、event registry digest `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9` 和 15 个 invariant；签名/nonce/时间戳/replay 校验失败发生在 A2 状态机之前，零领域副作用，不重拨、不重复预占或结算、不改变语义身份。
- 只有三个实现切片：S1 敏感数据保护/密钥层级/轮换恢复；S2 认证域/服务身份/签名与 replay 拒绝；S3 媒体凭证/COS 私有策略/审计访问/高风险控制/确定性脱敏。三者按连贯安全结果与对抗失败行为分组，不按文件、文档章节、云产品或 owner 机械拆分。
- Tencent Cloud 中国部署与私有 COS bucket 固定；KMS/Secret Manager/CAM/COS 映射区分“固定要求”和“区域/能力待验证输入”，不发明 region、SKU、价格、容量、WORM/object-lock 能力。
- 无 blocking open question。Q1/Q3/Q6/Q7/Q8/Q10/Q11/Q12/Q13/Q14/Q17/Q18/Q19/Q22/Q23/Q25/Q27/Q28 这 18 个 A0 Q 子集 row 保持 typed deferred 且 owner/destination 与 `docs/14` §14 原值一致；A3 另行新增 5 个云能力验证输入 CV-1..CV-5（不占用 Q 编号，不改变 A0 §14）。Q2 的逻辑算法/密钥层级由 A3 关闭，供应商能力仍由 CV-1 验证。
- 三个 A1/A2 投影边界已按 controller 裁决处理，且不静默 redesign accepted contract：员工发布类 operation 无 operator reason 字段 → `ACTION_DERIVED` 仅作技术 `reason_code` 投影，语义缺口保持 residual；上传凭证 → 单一 COS presigned PUT shape，CV-4 不满足则 fail closed 并停止；A2 outbox 未命名 401/403 → 冻结 IVR-local `PENDING`/security-blocked 策略，不改 A2。若实现阶段发现新的不可避免矛盾，必须停止并报告，不得修改 A0/A1/A2。
- 本计划只创建本文件；不实施、不 stage/commit/push、不创建或修改 PR、不进入 plan review 之外的下一个 Gate。

## 1. 目标、动机、成功信号、非目标与范围边界

### 1.1 目标

形成一份 code-generation-ready、静态可验证的生产安全设计规范，覆盖：

- 密钥层级与用途分离；
- 信封加密与 HMAC 索引，并保持 A0 wire shape；
- 密钥轮换、恢复、历史解密/查找与 replay 兼容；
- customer、employee、IVR、public-share、provider 五个独立认证域；
- 服务身份与 token audience；
- IVR/provider 请求或回调签名、nonce、timestamp 与 replay 保护；
- 短时、最小权限的媒体上传/播放凭证；
- Tencent COS 私有对象策略；
- 审计访问、确定性日志脱敏与高风险操作控制。

### 1.2 动机与直接仓库证据

| 证据 | 精确位置 | 对本计划的作用 |
| --- | --- | --- |
| 安全设计是 A0/A1/A2 之后的第 4 个 code-ready 规格 | `docs/12` §18（第 520-529 行，第 4 项） | A3 的编号、边界和顺序 |
| KMS 信封加密、独立 HMAC 索引、短时对象凭证、高风险审计 | `docs/12` §11.1/§11.2/§11.3（第 381-403 行） | 安全目标的第一性来源（§11.1 的四个认证域是认证机制分类） |
| 五个信任面/认证域的前缀、认证方式和 audience | `docs/12` §8 五个 API face（第 269-277 行）+ A1 `x-onecall-audience` 五值 | 五域设计的直接来源；`docs/12` §11.1 的四个认证机制类别是另一项独立事实，不是五面枚举 |
| 网络与 COS 边界、IVR mTLS/签名/timestamp/nonce、COS 私有桶、上传后服务端重算哈希 | `docs/12` §4（第 197-204 行） | trust boundary、媒体凭证和 COS 策略 |
| A0 envelope 与 HMAC 形状、敏感字段目录、保护 key denylist | `docs/14` §2.6/§8.1/§8.4/§10.7（第 191-198、2827-2858、2892-2897、3509-3513 行） | 不得替换的 schema/wire 与脱敏基础 |
| A0 audit fact、append-only 与 high-risk reason 规则 | `docs/14` §4.5/§9.3（第 553-596、3045-3051 行） | 审计访问与高风险控制 |
| A0 28 个 open inputs 及 owner/destination | `docs/14` §14（第 4772-4805 行） | typed deferred registry |
| A1 五面、6 scheme、operation 计数、凭证字段与 return-once 例外 | `docs/15` §1/§5/§6/§8/§10；OpenAPI 第 3994-4020、4066-4101、6948-6977、7167-7231 行 | 不得新增/收窄的 wire 与 projection |
| A1 精确 Problem code registry 与五面领域错误集 | `docs/15` §4（第 174-253 行） | fail-closed 只能映射既有 code |
| A2 签名/nonce/窗口仍 deferred 的 residual | `docs/16` §11 `R-A2-SIGNATURE`（第 668 行） | A3 关闭签名串/nonce 语义，窗口数值仍 Q25 |
| A2 event replay/outbox/终态吸收与 exactly-once | `docs/16` §6.3/§6.6/§6.7（第 256-267、317-326、328-335 行） | A3 失败关闭不得破坏 A2 |
| A2 event registry digest 与 15 invariants | `docs/16` §6 第 196 行、§9 第 516-536 行 | validator 的 drift guard |

### 1.3 成功信号与计划落点

| 成功信号 | 本计划落点 |
| --- | --- |
| 每个敏感数据有 owner、存储、用途、轮换/恢复、fail-closed、audit/access、testable scenario | §5.5 28-row data registry；§6.1/§6.9；S1 fixtures |
| 每个 key 有 custody、purpose、state、rotation、recovery | §5.3 20-row key registry；§4.3/§4.6；S1 fixtures |
| 每个 credential 有 domain、audience、storage、TTL input、redaction、rotation/revocation | §5.1/§5.4；§4.1/§4.2；S2 fixtures |
| 每个 service call class 有身份、权限、审计/访问事件和 fail-closed | §5.2/§5.8；§4.2/§4.12；S2/S3 fixtures |
| A0/A1/A2 wire 与身份不变 | §3.6/§7.2；validator drift counters |
| 三片可独立验证、不过度拆分 | §8；每片独立 stop condition 与 fixture slice |

### 1.4 非目标与禁止发明

不做：Java/runtime 实现、真实云资源/凭证、真实 KMS/Secret Manager/CAM/COS 调用、UI/prototype 修改、容量/部署 sizing、法律留存选择、微信/实名/支付 provider 实现、Kubernetes、service mesh、SPIFFE、自定义 IAM 引擎、微服务拆分、新员工角色、运营播放 endpoint。

禁止发明：region、SKU、价格、容量、WORM/object-lock 能力、MFA/IdP、法律留存期、生产 TTL/rate-limit/replay-window 数值、organization permission matrix、vendor-specific signature/payload/KDF 参数。所有此类值保持 typed deferred input，附 owner 与 destination。

范围边界：A3 只交付安全规范、机器可读模型、确定性 fixtures、零依赖 validator 和 Gateflow 报告；A0/A1/A2 accepted 契约只读，只有证明不可避免矛盾时才可报告 blocker，不得静默扩展。

### 1.5 已批准契约与 A3 提议的安全细节

| 类别 | 内容 | 来源/理由 |
| --- | --- | --- |
| 已批准 | 五面 prefix、49 operation、6 security scheme、五面 audience 值 `customer/employee/ivr-service/public-capability/provider` | A1/`docs/15` §1 |
| 已批准 | `ivrMtls`+`ivrSignature`、`providerCallbackMtls`+`providerCallbackSignature`、`X-ONECALL-SIGNATURE`/`X-ONECALL-TIMESTAMP`/`X-ONECALL-NONCE` 名称；TIMESTAMP 长度 1-64、NONCE 长度 1-128；SIGNATURE header 长度未在 A1 冻结 | OpenAPI 第 4005-4020、4066-4101 行 |
| 已批准 | ciphertext envelope、HMAC-SHA-256 64-hex、`key_ref` 字符集、A0 sensitive 28 rows | `docs/14` §2.6/§8.1 |
| 已批准 | audit fact append-only、`audit.append_fact`、`audit_reader_role` SELECT-only、employee action reason 规则 | `docs/14` §4.5/§9.3/§10.6/§10.8 |
| 已批准 | A2 12 messages、11 event types、22 identities、15 invariants、event digest、outbox 分类、媒体状态机 | `docs/16` |
| A3 提议 | AEAD profile 为 AES-256-GCM、12-byte nonce、16-byte tag、AAD 绑定 purpose/dataClass | Q2 明确把算法/KMS product 交给 security design；A3 关闭逻辑算法，云能力交 CV-1 |
| A3 提议 | 每个 DEK 由 KMS CMK 保护并以 Secret Manager version 承载；HMAC key 由 Secret Manager 承载（不假设 KMS HMAC 能力） | KMS/Secret Manager 固定映射；CV-1/CV-2 |
| A3 提议 | `key_ref` 解析：首 `.` 前为版本，末 `.` 后为 base64url，中间为 opaque key_ref（可含 `.`），完全兼容 A0 regex | A0 `^v1\.[A-Za-z0-9._:/-]+\..+$` |
| A3 提议 | IVR signature header `v1.<keyId>.<base64url(HMAC-SHA-256(...))>`；canonical string 固定 | A1 已冻结 header 名；A3 关闭 Q15/Q25 的字符串/nonce 形状，窗口数值仍 Q25 |
| A3 提议 | 传输 nonce 每次重试重新生成，事件 `eventId`/payload 不变 | 分离 transport replay 与 A2 semantic identity |
| A3 提议 | upload credential 是单一 shape：short-lived、single-object Tencent COS presigned PUT credential，仅在 CV-4 验证 method/object/expiry 与支持的 size 限制后使用；若这些能力无法验证则签发 fail closed、实施停止等待架构决议，不 fallback 到 custom HMAC/signing gateway；`onecall:evidence-upload:v1` 只是签发侧 policy/audience 分类，不是 COS 内嵌 audience | 保持 A1 `credential` 单字段（opaque 1-2048、redaction-required、可能 URL-shaped、永不日志/拆分） |
| A3 提议 | playback credential 以 `Authorization: Bearer <credential>` 呈现给已鉴权的 media-gateway data plane；bearer credential 单独在服务端解析其授权对象/scope；客户端不提供 COS object key/bucket/version 或 public A1 resource identifier；gateway origin/path、CDN/LB、range/streaming 与 delivery topology 是 Infrastructure/Security 的 typed deployment residual | A1 `MediaAuthorization` 由 media gateway 校验；这不新增 A1 业务 operation |
| A3 提议 | 普通读取使用确定性 access event；安全/高风险操作使用 audit fact | 避免将每次读取写成 audit fact 的过度设计；Q13 保留量级 |

### 1.6 无目标漂移检查

- 不新增路由：安全控制映射到 A1 既有 49 operation；不新增 upload/playback/audit endpoint。C10 以 bearer 呈现给既有、已鉴权的 media-gateway data plane；这不是新增 A1 业务 operation，gateway origin/path 与 delivery topology 是 Infrastructure/Security 的部署输入/residual（`R-A3-MEDIA-GATEWAY-TRANSPORT`）。
- 不新增 Problem code：所有失败映射到 A1 既有 code；不新增 security-specific code。
- 不新增 A0 表/列/状态/事务：所有数据类引用 A0 既有 44 表 catalog 与 28 sensitive rows。
- 不新增 A2 消息/事件/身份/invariant：只读取 A2 模型和 digest，不修改。
- 不新增 employee role：固定 `ADMIN`/`OPS`；不新增 break-glass playback。
- 不新增基础设施拓扑：service identity 映射既有 deployment units；`media-gateway` 只是逻辑校验点，不新增部署单元。
- 不发明数值/策略：窗口、TTL、KDF 参数、retention、region、SKU、WORM 全部 deferred。
- 不越界实现：切片只产出规范、模型、fixtures、validator、Gateflow 报告。

## 2. 第一性原理判断与精确上游证据

### 2.1 六个第一性判断

1. **数据所有权决定密钥用途**：密钥不是按表或云产品划分，而是按“谁拥有该敏感事实、允许哪些服务在哪个 purpose 下使用”划分。A0 的 28 个 sensitive rows 是数据边界的唯一目录。
2. **schema 形状不可变，密码学属性可补齐**：A0 已冻结 envelope 文本形状；A3 只能冻结 `key_ref` 解析、AEAD/AAD、purpose 绑定、轮换/recovery 和兼容规则，不能引入新列或新 envelope 版本。
3. **append-only 事实不能为轮换重写**：`ivr_decision`、`call_event`、`audit_fact`、`evidence_event` 等 frozen/append-only 行必须保留原 ciphertext/HMAC 与 key_ref；旧 key version 必须可 decrypt/lookup 到 retention 结束（Q3），否则只能 fail closed。
4. **replay 是两层问题**：transport nonce/timestamp 防重放；domain `eventId`/`callId`/`recordingId` 做幂等。两者不能混用：IVR 重试必须保留 domain identity、重新生成 transport nonce/timestamp，否则会破坏 A2 exactly-once。
5. **认证域由 audience + custody + verifier 共同定义**：仅靠 path prefix 不是安全边界；一个域的有效 token 在另一个域必须被 audience/verifier 拒绝。
6. **脱敏必须在 emit 之前确定性发生**：日志、trace、error、analytics、support view 与 public contract 是不同出口，但共享同一 deny-list + allow-list 分类；不能依赖下游清洗。

### 2.2 上游权威与只读边界

- A0 是表、列、状态、named CHECK、事务、敏感字段目录、audit/outbox 契约的权威。
- A1 是 operation、path、request/response schema、Problem code、security scheme、credential 字段的权威。
- A2 是 IVR 授权/执行/媒体语义、事件身份、replay、outbox、终态与 exactly-once 的权威。
- A3 只新增安全语义：key purpose、AAD、custody、rotation/recovery、auth domain/audience、signature profile、media policy、audit/redaction/fail-closed。`docs/17` 不复制 A1 schema、A0 表字典或 A2 消息表。

### 2.3 精确证据索引

| 主题 | 精确引用 |
| --- | --- |
| 五面 prefix、operation 数、认证/不授予 | `docs/15` §1（第 21-31 行） |
| 6 security scheme 与 header 名 | OpenAPI 第 3994-4020、4066-4101 行 |
| A1 五面 audience 值 | OpenAPI `x-onecall-audience`：customer/employee/ivr-service/public-capability/provider |
| A1 领域错误集与公共 code | `docs/15` §4（第 174-253 行）；`docs/16` §4（第 150-169 行） |
| A0 envelope/HMAC `key_version` | `docs/14` §2.6（第 191-198 行）、§8.4（第 2892-2897 行） |
| A0 envelope regex 与 key_ref 字符集 | `docs/14` `ck_*_ciphertext`（例如第 533、649、686、1301、1522、1574 行） |
| A0 28 sensitive rows | `docs/14` §8.1（第 2827-2858 行） |
| A0 protected-key denylist | `docs/14` §10.7（第 3509-3513 行） |
| A0 audit fact 与 reason 规则 | `docs/14` §4.5（第 553-596 行）、§9.3（第 3045-3051 行） |
| A0 evidence/media/share 敏感表示 | `docs/14` §8.6（第 2929-2945 行）、§2.12（第 241-260 行） |
| A2 12 messages/22 identities/15 invariants | `docs/16` §5/§8/§9 |
| A2 event registry digest | `docs/16` 第 196、629 行 |
| A2 event replay/terminal/outbox | `docs/16` §6.3/§6.6/§6.7 |
| A2 媒体声明/上传/完成/conflict | `docs/16` §7 |
| A2 signature residual | `docs/16` §11 `R-A2-SIGNATURE` |

### 2.4 已识别的投影边界与停止规则

以下三项是 **显式记录的投影边界**，不是 A3 的 redesign，且已按 controller 裁决处理（ACTION_DERIVED 技术投影、upload 单一 presigned shape + fail-closed、A2 outbox 401/403 本地策略）；任何新的不可避免 accepted-contract 矛盾仍必须停止并报告，不得修改 A0/A1/A2：

| ID | 精确矛盾/边界 | A3 的处理 | Owner / Destination |
| --- | --- | --- | --- |
| `R-A3-A1-EMPLOYEE-REASON-PROJECTION` | A0 §9.3 要求所有 employee-actor audit 有 reason；A1 的目录/试用策略 publish/off sale/draft 请求没有 `reasonCode` 字段 | 对有 `reasonCode` 的 OPS suspend/resume 使用请求值；对无字段的 ADMIN 发布类只使用 `ACTION_DERIVED` 作为 A0 `reason_code` 的技术投影，以满足当前 A1 schema/A0 CHECK；该值不是员工的真实动机，也不关闭架构的语义 operation-reason 要求，此语义缺口保持命名 residual。A0 同事务 audit/reason fail-closed 行为不变。UI 二次确认仍是架构要求的运行时高风险控制，但 A3 不做 UI 工作，静态模型/validator/fixtures 不声称其已实现或已证明（runtime-unverified）。 | Product/Operations + API governance；未来 A1/UI refinement |
| `R-A3-A1-UPLOAD-CREDENTIAL-SHAPE` | `docs/12` §4 要求 IVR 直传 COS 的短时单对象凭证；A1 `RecordingDeclaration` 声明不返回 object key/bucket/permanent URL | 冻结唯一主 shape：short-lived single-object Tencent COS presigned PUT credential，仅在 CV-4 验证 method/object/expiry 与支持的 size 限制后使用；否则签发 fail closed 并停止等待架构决议，不 fallback 到 custom HMAC/signing gateway。`onecall:evidence-upload:v1` 只是签发侧 policy/audience 分类。`upload.credential` 保持 A1 单个 opaque 1-2048 redaction-required 字段，可能 URL-shaped，永不日志或拆分为 public object 字段 | Security/Backend/Evidence；API/evidence projection review |
| `R-A3-A2-AUTH-OUTBOX` | A2 §6.6 outbox 分类未命名 `UNAUTHENTICATED`/`FORBIDDEN`（401/403） | 冻结 IVR-local security-layer policy：观测到 401 或 403 时同一 outbox entry 保持既有 `PENDING`（可用本地 operational metadata 标记 security-blocked），不自动热重试、不新增 event identity、不改 payload、不重拨、不 re-resolve、不预占/结算；发出不含 credential 的 alert/audit signal；仅在显式 credential/key/certificate 或 service-authorization recovery signal 后重试，重试使用同一 domain identity 与 immutable payload + 新 timestamp/nonce。数值 retry/backoff/alert 仍 Q8/Q13/Q25。不新增/修改 A2 event type/state/digest 或 server classification | IVR/Backend；A2/IVR integration review |

## 3. 产物布局、canonical 结构与单一真相

### 3.1 精确路径

| 路径 | 作用 | 创建切片 |
| --- | --- | --- |
| `docs/17-production-security-design-spec.md` | 人类 canonical 安全设计规范与治理/映射 | S1 创建，S2/S3 增补 |
| `security/v1/security-design-v1.json` | 机器可读安全模型：domains、services、keys、data、credentials、signatures、media、audit、redaction、deferred | S1 创建，S2/S3 增补 |
| `security/v1/fixtures/sec-s1-*.json` / `sec-s2-*.json` / `sec-s3-*.json` | 确定性安全场景 | 各 slice 新增 |
| `security/validate.mjs` | 零第三方依赖 Node 22 静态校验 + fixture evaluator | S1 创建，S2/S3 增补 |
| `docs/gateflow/a3-production-security-design-spec-s1-implementation.md` | S1 完成报告 | S1 |
| `docs/gateflow/a3-production-security-design-spec-s2-implementation.md` | S2 完成报告 | S2 |
| `docs/gateflow/a3-production-security-design-spec-s3-implementation.md` | S3 完成报告 | S3 |

不创建 README、第二份安全文档、JSON Schema 副本、A1/A2 修改、A0 迁移或 package 依赖树。

### 3.2 为什么是 `docs/17` + `security/`

- `docs/12` §18 把“安全设计：密钥层级、字段加密/HMAC、服务身份、对象存储策略和审计访问”列为数据模型/OpenAPI/IVR 协议之后的第 4 个规格；`docs/16` 已被 A2 使用，下一个编号是 `docs/17`。
- 人类语义、trust boundary、deferred inputs、risk/residual 放在 `docs/17`；机器 registry 放在 `security/v1/security-design-v1.json`，避免第二 wire 真相。
- `security/validate.mjs` 使用 Node 22 内置模块（`node:fs`、`node:crypto`、`node:buffer`），不新增 `package.json`/lockfile。
- fixtures 只含 `TEST_ONLY_*` 合成密钥/凭据，不含真实 secret、真实 URL 或生产配置。

### 3.3 canonical 文档结构

`docs/17` 必须按以下顺序包含：

1. 状态、来源关系、A0/A1/A2 只读声明与静态限制；
2. trust surfaces/auth domains 与 trust boundaries；
3. service identities 与 token audiences；
4. key hierarchy、purpose separation、states、rotation/recovery；
5. envelope/HMAC wire contract 与 28-row data registry；
6. signature profiles（IVR/provider）、nonce/timestamp/replay；
7. media credentials、COS private-object policy；
8. audit access、high-risk operations、redaction；
9. fail-closed matrix 与 A2 exactly-once preservation；
10. Tencent KMS/Secret Manager/CAM/COS capability mapping（fixed vs verification input）；
11. deterministic validation commands、fixtures、honest limitations；
12. deferred inputs、residual risks、completion report template。

### 3.4 机器模型顶层结构（按 stage 精确）

| section | S1 | S2 | S3 |
| --- | --- | --- | --- |
| `modelVersion`、`specVersion`、`upstream` | required | same | same |
| `envelopeProfile`、`hmacProfile`、`keyStates`、`rotationPolicy`、`recoveryPolicy`、`replayCompatibility` | required & complete | same | same |
| `keyClasses`（20）、`dataClasses`（28）、`deferredInputs`（23 = 18 A0 Q 子集 + 5 A3 `CV-*`） | required | same | same |
| `invariants` | required exactly INV-A3-01..06（6） | 01..06 + INV-A3-07..11（cumulative 11） | 01..11 + INV-A3-12..21（cumulative 21） |
| `trustSurfaces`（5）、`authDomains`（5）、`trustBoundaries`（7）、`serviceIdentities`（6）、`credentialClasses`（10）、`signatureProfiles`（2）、`noncePolicy`、`audienceRegistry` | absent | required | same |
| `mediaPolicies`（2）、`cosPolicy`、`auditEvents`（39，每行含 `persistence`）、`highRiskActions`（10）、`auditCoverage`（10）、`redactionPolicy`、`failClosedMatrix` | absent | absent | required |

任何当前 stage 应有 section 缺失，或未来 stage section 提前出现，validator 必须失败。

### 3.5 文档 marker blocks 与 stage 投影

`docs/17` 必须包含以下机器读取区块；row 顺序与模型固定顺序一致：

| 标记块 | stage | 行数 | 精确列 |
| --- | --- | ---: | --- |
| `security-key-map` | S1 | 20 | `keyClass,custodyStorage,purpose,algorithm,dataClasses,rotation,recovery` |
| `security-data-map` | S1 | 28 | `dataClass,a0Inventory,representation,keyClass,owner,projection,auditAccess,rotationReplay` |
| `security-deferred-map` | S1 | 23 | `inputId,input,valueKind,a3Treatment,owner,destination` |
| `security-domain-map` | S2 | 5 | `domain,prefix,operations,credentialClasses,storage,tokenAudience,crossDomainRule` |
| `security-service-map` | S2 | 6 | `serviceIdentity,mechanism,audience,allowedCalls,secretCustody` |
| `security-credential-map` | S2 | 10 | `credentialClass,domain,audience,storage,ttlInput,scope,rotationRevocation,redaction` |
| `security-audit-map` | S3 | 39 | `actionCode,actor,objectType,trigger,persistence` |

`signatureProfiles`、`mediaPolicies`、`highRiskActions`、`auditCoverage`、`failClosedMatrix` 是模型 section，文档以叙述和映射表说明；validator 用 A1/A2 schema 和模型精确值交叉检查，不复制第二份 schema。`auditEvents` 的模型 row 除 marker 的 5 个核心列（含 `persistence`）外，还必须携带 `highRisk`、`reasonSource`、`destination`，由 §5.8/§5.9/§5.10 交叉校验。`security-deferred-map` 的 23 行必须按 origin 分区：18 行 `Q*` 与 `docs/14` §14 原值逐行相等；5 行 `CV-*` 为 A3-origin capability-verification rows，不占用 Q 编号。

### 3.6 单一真相与禁改

- A1 仍是 operation/path/schema/Problem/security scheme/credential 字段的权威；A0 仍是表/列/状态/事务/敏感目录的权威；A2 仍是 IVR 协议/身份/replay/outbox 的权威。
- `docs/17` 是安全语义的权威；`security/v1/security-design-v1.json` 是其机器投影，不得包含与 `docs/17` 或 A0/A1/A2 冲突的值。
- validator 必须同时校验：doc marker ↔ model、model ↔ A1 bundle、model ↔ A0 sensitive/catalog、model ↔ A2 模型/digest、fixtures ↔ evaluator。
- 只允许写 §8 各 slice 列出的路径；不修改 `docs/12`-`docs/16`、A0/A1/A2 accepted 产物、OpenAPI source、prototype、screenshots、`shared/onecall-business.js`、`docs/gateflow/project-execution-control.md`、`logo/preview.html`、untracked `.a3-security-plan-task.md` 和 `docs/reviews/plan-review-20261021-205118.md`。

## 4. 安全总体设计

### 4.1 信任面、认证域与凭据边界

五个信任面/认证域来自 `docs/12` §8 的五个 API face（第 269-277 行）与 A1 `x-onecall-audience` 五个值，不新增域；`docs/12` §11.1 的四个认证域（客户/运营员工/IVR/供应商回调）是认证机制类别，不是五面枚举，二者并列不矛盾：

| domain | prefix | operation 数 | credential | storage | token audience | 跨域规则 |
| --- | --- | ---: | --- | --- | --- | --- |
| `customer` | `/api/v1/customer/` | 18 | C01 customerSession | Redis session record + account state | `onecall:customer-session:v1` | 拒绝任何其他域 token；不因 gateway 信任而放行 |
| `employee` | `/api/v1/operations/` | 21 | C02 employeeSession | Redis session record + employee state | `onecall:employee-session:v1` | 独立 issuer；固定 ADMIN/OPS；不与 customer 复用 |
| `ivr-service` | `/api/v1/ivr/` | 6 | C03 ivrMtls + C04 ivrSignature | mTLS cert store + Secret Manager HMAC key | `onecall:ivr-request:v1` | 只接受 mTLS+签名；不接受 session/capability token |
| `public-capability` | `/api/v1/public/shares/` | 3 | C07 capability token；EXTRACTION_CODE 用 C08；可选 C01 仅 `ORGANIZATION_MEMBER`；P3 用 C10 | DB digest + admission + Redis ticket Q22 | `onecall:share-capability:v1` / `onecall:media-playback:v1` | capability token 不能用于其他面；customer token 不能替代 capability token |
| `provider` | `/api/v1/callbacks/` | 1 | C05 providerCallbackMtls + C06 providerCallbackSignature | mTLS cert store + Secret Manager vendor verification material | `onecall:provider-callback:v1` | 只接受 provider mTLS+厂商签名；不泄露内部结果 |

- `customerSession`/`employeeSession` 是不透明 bearer；服务端解析 audience、账户/员工、workspace/role。不得使用客户端可解码且无 audience 的 JWT。
- 五个域的 signing/verification key、session store、audience 和失败映射互不复用。

七个固定 trust boundaries：

| ID | 边界 | 允许的认证/通道 | 明确禁止 |
| --- | --- | --- | --- |
| TB1 | Internet/edge → customer/public-share/operations 前端 | TLS/WAF；customer/employee/capability domain | 直接访问 DB/Redis/MQ/COS |
| TB2 | Edge → core-api 的 customer/operations/public-share API | 对应 session/capability credential；路径/audience | 跨域 token、永久对象 URL |
| TB3 | 运营商机房/IVR 网络 → core-api IVR API | mTLS + IVR signature + timestamp + nonce | DB/MQ 直连、无签名请求 |
| TB4 | Internet/provider → callback API | provider mTLS + 厂商签名 + timestamp + nonce | 内部结果披露、订单金额/支付成功声明 |
| TB5 | core/worker → PostgreSQL/Redis/RabbitMQ | NOLOGIN DB role、Outbox、最小权限 | 跨模块写、客户端直连、绕过 Outbox |
| TB6 | core/worker/ivr-gateway → Tencent KMS/Secret Manager/COS | CAM service role、短时 upload credential、私有 bucket | 静态 AK、公开对象、永久 URL |
| TB7 | 应用 → logs/traces/metrics/analytics/support/public export | redaction pipeline、masked/ID/status、audit access | plaintext/secret/credential URL/HMAC/ciphertext |
- 运营读取只授予脱敏业务元数据；A1 没有 operations playback endpoint，A3 不新增；支持视图无原始录音播放权。
- mTLS 证书只证明 service/provider 身份；IVR 仍必须每请求签名、校验 timestamp/nonce；provider 仍必须验厂商签名。

### 4.2 服务身份与 token audience

内部服务身份映射既有 deployment unit（`docs/12` §5），不新增拓扑：

| service identity | mechanism | audience | allowed calls | secret custody |
| --- | --- | --- | --- | --- |
| `core-api` | Tencent CAM service role + DB NOLOGIN role | `onecall:service:core:v1` | DB、Redis、Outbox、KMS、Secret Manager、COS evidence metadata | Secret Manager/instance role；无静态 key |
| `async-worker` | CAM service role + DB role | `onecall:service:worker:v1` | DB、MQ consumer、KMS、Secret Manager、COS、notary/AI adapters | Secret Manager/instance role |
| `scheduler` | CAM service role + DB role | `onecall:service:scheduler:v1` | DB lease/到期扫描；无外部敏感 secret | 最小；无外部 provider key |
| `media-ai-worker` | CAM service role | `onecall:service:media-ai:v1` | MQ、COS read/write、AI provider Secret Manager；不写业务 DB | Secret Manager |
| `ivr-gateway` | mTLS client + Secret Manager | `onecall:ivr-request:v1` | 仅 A1 IVR API；受控 COS upload；无 DB/MQ | C03/C04 |
| `media-gateway`（逻辑点，部署在既有 core-api/edge 内） | mTLS/内部 audience + Secret Manager | `onecall:media-playback:v1` | 校验 C10、读取 COS 媒体；不签发 upload credential | C10 signing key |

- 前端 `customer-web`、`public-share-web`、`operations-web`、小程序不持有 service credential、KMS/Secret Manager 访问权或永久 COS 凭证。
- 每个 CAM role 只授予该服务必需的 action/resource；禁止 `*` action、跨环境 role 复用和长期静态 AccessKey。
- provider/IVR 是外部身份，不进入内部 service identity registry；它们通过各自认证域验证。
- 每个 service identity 的 CAM role/证书/密钥按 Q17/Q23/CV-3 轮换；轮换期间保留旧身份 verify-only/overlap，重叠后 disable；不得复用其他服务的 role。

### 4.3 密钥层级、用途分离与生命周期

逻辑层级固定为：**Tencent KMS CMK（root KEK）→ 每 purpose DEK（AES-256-GCM）→ 数据**；HMAC key 与签名 key 为独立分支，不参与加密层级。

- CMK 材料不出 KMS；DEK 由 KMS GenerateDataKey 生成并由 CMK 包裹（CV-1 验证）；若该能力不可用，则由 CSPRNG 生成 DEK 后立即用 KMS Encrypt 包裹，二者必须在 CV-1 记录所选路径。wrapped DEK 只存 Secret Manager version，plaintext 只在进程内存。
- 每个 DEK 只绑定一个 purpose；`key_ref` 在控制面 registry 中映射到唯一 purpose + dataClass 集合。解密 API 必须接收 expected purpose/dataClass，registry 不匹配即失败。
- HMAC key 只用于 equality/uniqueness/indexed lookup；不得用于加密、签名或会话。HMAC 与 encryption key 永不共用。
- IVR signing key、provider verification material、upload signing key、playback signing key 各自独立；不得复用一个 secret。
- 会话 token 为 CSPRNG 不透明随机值（至少 128 bit），服务端存储 audience/身份绑定；不是 KMS key。capability token 同为 CSPRNG 随机值，仅存 SHA-256 digest，创建时 return-once。
- 逻辑 key 状态：`ACTIVE`、`DECRYPT_ONLY`、`LOOKUP_ONLY`、`VERIFY_ONLY`、`DISABLED`、`DESTROYED`。每个状态有唯一允许用途；validator 必须拒绝状态-用途不匹配。
- 任何 key/secret 不得写入数据库业务列（除 envelope 内的 opaque `key_ref`）、代码、镜像、日志、trace、audit snapshot、Outbox、fixture 期望输出或 public contract。

### 4.4 信封加密与 HMAC 索引契约（保持 A0 wire shape）

**Envelope**

- 文本形状严格为 `v1.<key_ref>.<base64url(nonce || authenticated_ciphertext_and_tag)>`。
- 解析：第一个 `.` 前必须是字面量 `v1`；最后一个 `.` 后是 base64url body；中间的 `key_ref` 可包含 `.`，长度与字符集遵守 A0 regex `^[A-Za-z0-9._:/-]+$`；不得截断、重排或 URL-decode。
- `base64url` 使用无 padding 的 `[A-Za-z0-9_-]+`；body 至少包含 nonce + tag + 1 byte ciphertext。
- AEAD profile：AES-256-GCM；nonce 12 bytes，由 CSPRNG 每次加密重新生成；tag 16 bytes；AAD 固定为 UTF-8 ASCII `onecall:v1:<purposeId>:<dataClassId>`。
- `key_ref` 是内部 opaque 值，不 returned、不 logged、不 audited；registry 映射 key purpose/version。unknown/malformed/retired key_ref 必须 fail closed，不得回退到明文或扫描。
- `customer_iam.auth_identity.key_version` 必须等于该行 envelope `key_ref` 对应的整数 version；其他表只依赖 envelope 内 key_ref。
- 加密前 plaintext 必须是 canonical JSON（A0 §2.5）或明确字段字节；解密后只返回 owning service 允许的字段。

**HMAC**

- 形状：lowercase 64-hex，独立 HMAC-SHA-256 key；输入为规范化值（号码/主体/登录名/designated mobile）的 UTF-8 bytes；不得加盐、截断或使用普通 SHA-256。
- HMAC 只用于 equality/uniqueness/indexed lookup；不得 returned、logged、audited 或作为 public key。
- `accessNumberHmac` 使用与 caller/number HMAC 相同的 approved phone-number key 和 contract（A0 §2.5）；只作为 ephemeral fingerprint input，不持久化、不输出。
- masked 值由服务端生成，仅用于 display；mask 不具 collision resistance，不能作为 replay identity。

### 4.5 敏感数据目录（28 rows）

实现必须冻结 A0 §8.1 的全部 28 rows，逐行映射到以下 data class。不得新增未注册敏感字段；若实现发现新的敏感字段，必须停止并回到上游设计。

| ID | A0 §8.1 row | representation | key/HMAC | owner | public/support projection | audit/access | rotation/replay |
| --- | --- | --- | --- | --- | --- | --- | --- |
| D01 | provider subject ciphertext+hmac | AEAD+HMAC | K02/K11 | customer_iam | 永不返回；HMAC 不作 public key | service access redacted；员工读取 `SECURITY_SENSITIVE_READ` | 旧 key_ref 可解；HMAC 双读/重建 |
| D02 | legal name/id number/verification ref | AEAD+HMAC | K03/K12/K04 | customer_iam | 永不返回明文；仅状态/掩码 | 同上 | 可 re-encrypt 的 mutable row |
| D03 | employee login/auth subject HMAC | HMAC+mask | K14/K15 | operations_iam | masked login only | employee auth access；不返回 HMAC | 双读/重建 |
| D04 | caller binding number ciphertext/hmac/mask | AEAD+HMAC+mask | K05/K13 | subject_number | masked number only | service access redacted | 可 re-encrypt/重建 |
| D05 | subject claim verification ref | AEAD | K04 | subject_number | 永不返回 | claim/audit redacted | append-only：不得改写，旧 key 保留 |
| D06 | outbound task caller hmac/callee ciphertext/mask | AEAD+HMAC+mask | K05/K13 | ivr | masked callee；明文仅 IVR 内部 decision payload | service access redacted | mutable task 可 re-encrypt |
| D07 | inbound session caller ciphertext/hmac/mask/access mask | AEAD+HMAC+mask | K05/K13 | ivr | masked only | service access redacted | mutable session 可 re-encrypt |
| D08 | ivr decision redacted snapshot + encrypted payload | AEAD+redacted JSON | K06 | ivr | redacted snapshot only；replay 解密同一 payload | employee/support read audit | append-only：不得 re-encrypt；旧 key 保留 |
| D09 | call record caller/callee ciphertext+hmac+mask | AEAD+HMAC+mask | K05/K13 | ivr | masked only | service access redacted | immutable party guard：不得改写 |
| D10 | call event payload ciphertext + redacted view | AEAD+redacted JSON | K07 | ivr | receipt/redacted only | service access redacted | append-only：不得改写 |
| D11 | payment event provider payload | AEAD | K08 | catalog_order | 永不返回/日志 | restricted read + audit | append-only |
| D12 | payment provider/transaction IDs | internal ID | - | catalog_order | 不作 public key；受限审计 | restricted access | provider-scoped；不轮换为 public |
| D13 | catalog version snapshot/hash | redacted JSON+hash | - | catalog_order | 商业事实，无 protected value | publication audit | immutable on publish |
| D14 | customer order snapshot/hash | redacted JSON+hash | - | catalog_order | 冻结商业事实 | order access | immutable |
| D15 | trial policy snapshot/hash | redacted JSON+hash | - | entitlement_metering | policy facts only | publication audit | immutable version |
| D16 | entitlement grant snapshot/hash | redacted JSON+hash | - | entitlement_metering | historical grant facts | access/audit | immutable |
| D17 | usage fact snapshot/hash | redacted JSON+hash | - | entitlement_metering | usage metadata | settlement audit | append-only |
| D18 | consent record snapshot/hash | redacted JSON+hash | - | activation_consent | no protected value | consent audit | append-only |
| D19 | media object bucket/key/version/crypto_key_ref + hash | COS internal ref + hash | K10/CV-4 | evidence | 永不返回 key/bucket/version/URL | media credential/object audit | COS versioning；旧 CMK version 可解 |
| D20 | recording declared/validated SHA-256 | hash only | - | evidence | hash prefix only if approved | recording audit | immutable validation facts |
| D21 | evidence identity-at-capture/consent/snapshot | reference + redacted JSON+hash | - | evidence | no raw PII/object key/URL | evidence audit | immutable capture |
| D22 | evidence event redacted payload/hash | redacted JSON+hash | - | evidence | no media bytes/raw parties | append-only audit | immutable |
| D23 | share capability token hash/code hash/salt | token digest + KDF salt | C07/C08 | sharing | plaintext only return-once；后续永不回显 | share create/rotate/revoke audit | capability token 不可重建；code generation 轮换 |
| D24 | share content snapshot/hash + designated mobile HMAC/key version | redacted JSON+hash+HMAC | K16 | sharing | no object key/URL/raw phone/unshared evidence | share transition audit | immutable snapshot；旧 HMAC version 保留至 grant/retention |
| D25 | share admission key | opaque derived key | - | sharing | never returned | admission access event | deterministic by `shareGrantId+admissionRequestId` |
| D26 | share access event risk digest/redacted payload | hash + redacted JSON | - | sharing | no raw IP/phone/code/token/full URL | `SHARE_ACCESS` event | append-only |
| D27 | audit fact before/after snapshots | redacted JSON or AEAD | K09 when encrypted | operations-audit | closed redacted projection only | audit read access event | append-only；不得改写 |
| D28 | outbox/idempotency redacted/encrypted payload | redacted JSON or AEAD | K09 when encrypted | platform | consumers only authorized fields | dispatch access redacted | immutable payload |

### 4.6 轮换、恢复、历史解密/查找与 replay 兼容

**轮换**

- DEK rotation：新建 version → `ACTIVE`；旧 version → `DECRYPT_ONLY`。新写使用 active；历史读用 envelope `key_ref`。仅对 A0 允许 UPDATE 且无 immutability guard 的行做 lazy/idempotent re-encrypt；append-only/frozen 行永不重写。
- KMS CMK rotation：按 CV-1 验证的能力执行；旧 CMK/version 保持 decrypt-only；不得要求重写 append-only 行。若 provider 不支持自动 rotation，则新建 CMK + 受控 re-wrap，CV-1 记录。
- HMAC rotation：新 version `ACTIVE`，旧 version `LOOKUP_ONLY`。mutable 行重建 HMAC；append-only/frozen 行保留旧 HMAC。lookup 依次计算 active + 仍被保留行引用的历史 key 候选值，仅做 equality query，不扫描明文。
- share designated mobile HMAC：`content_snapshot` 内保存 key version；旧 version 保留至 grant 过期/retention；新 grant 使用 active。
- signature key rotation：新 `ACTIVE`，旧 `VERIFY_ONLY`；重叠窗口内两者可验签；重叠结束后旧 `DISABLED`；unknown keyId 直接拒绝。
- media playback signing key rotation：旧 key `VERIFY_ONLY` 至少覆盖已签发 credential 的最大 TTL + Q22 撤销传播窗口，然后 `DISABLED`；upload signing key 同理覆盖 `uploadExpiresAt`。
- 任何 rotation 必须有 audit event、双人/授权控制（具体组织流程 Q1/Q17）、失败关闭；不得在 rotation 中输出 plaintext/key material。

**恢复**

- `DISABLED`：通过 KMS/Secret Manager 授权 re-enable；audit `SECURITY_KEY_RECOVER`；应用不提供绕过。
- pending deletion：仅当 CV-1/CV-2 证明 provider 支持取消/恢复时执行；否则视为不可恢复。
- `DESTROYED`：该 data class 不可解；所有读取 fail closed；事件/事故流程由 Security/Operations 负责；不得从备份重建 plaintext 或使用弱化 fallback。
- Secret Manager version 不可用：fail closed；不得回退到旧明文配置、环境变量或镜像内 secret。

**历史解密/查找**

- 解密必须先校验 envelope 形状、key_ref 存在、key 状态允许 decrypt、purpose/dataClass 匹配；任一失败 fail closed。
- HMAC 查找只返回 equality 命中；若所需 key version 全部不可用，返回 `SERVICE_UNAVAILABLE`（或该面既有 503），不返回空结果冒充“不存在”。
- A2 `ivr_decision` replay 必须用其 envelope `key_ref` 解密同一 payload，返回同一 redacted snapshot；不得重新选择 action、重新计算余额或创建新 reservation。
- `auth_identity.key_version` 与 envelope key_ref 必须一致；不一致视为数据完整性错误，fail closed。

**Replay 兼容**

- 轮换不得改变 A1 wire、A2 semantic identity、redacted snapshot、`response_sha256`、`request_fingerprint` 的语义输入。
- A2 `accessNumberHmac` fingerprint 在 HMAC 轮换后必须能通过“active + 历史 key 候选”复现；旧 key 保留至少覆盖 call/decision replay 窗口（Q25）与 append-only retention（Q3）。若无法保证，必须停止并报告，不得静默改变 fingerprint contract。
- 传输 nonce/timestamp 不属于 A2 semantic identity；重试可重新生成，eventId/payload 不变。
- 若 replay 解密因 key 不可用失败，返回该面既有 503；不得返回空 payload、伪造成功或重新执行。

### 4.7 IVR 请求签名、nonce、timestamp 与 replay

- 适用面：`/api/v1/ivr/**` 的 6 个 operation；必须同时具备 `ivrMtls` 与 `ivrSignature`。
- Header：A1 已冻结三个 header 名称 `X-ONECALL-SIGNATURE`、`X-ONECALL-TIMESTAMP`、`X-ONECALL-NONCE`；TIMESTAMP 长度 1-64、NONCE 长度 1-128 已冻结；SIGNATURE header 长度**未**由 A1 冻结，其值格式由 A3 冻结为下一行的 `v1.<keyId>.<base64url(...)>`。
- Signature 值：`v1.<keyId>.<base64url(HMAC-SHA-256(K17, canonicalString))>`；`keyId` 为 Secret Manager version label `[A-Za-z0-9_-]{1,64}`；constant-time compare。
- Timestamp：ASCII Unix epoch seconds（`^[0-9]{1,12}$`）；接受窗口 `±W`，`W` 为 Q25 deferred；以 backend receive time 为准，不使用客户端 `occurredAt`。
- Nonce：`[A-Za-z0-9_-]{1,128}`；在 `(serviceIdentity,nonce)` 维度唯一；Redis `SET NX` + TTL = `W + skew`（Q25 deferred）。
- Canonical string 固定为六行 UTF-8，行间 `\n`，末尾无额外换行：
  1. 字面量 `ONECALL-IVR-V1`
  2. uppercase HTTP method
  3. raw request path（不含 query，不 decode、不归一化；A1 path 固定）
  4. `X-ONECALL-TIMESTAMP`
  5. `X-ONECALL-NONCE`
  6. lowercase hex SHA-256 of exact request body bytes（空 body 为空字节哈希）
- 验证顺序固定：mTLS client identity → 必需 header/长度 → timestamp 解析/窗口 → signature verify（keyId active/verify-only）→ nonce `SET NX` → A1/A2 处理。
- 失败映射：缺/坏 mTLS、坏签名、超窗口、nonce replay → `401 UNAUTHENTICATED`；service identity 不允许 → `403 FORBIDDEN`；nonce/key/Secret Manager 不可用 → `503 SERVICE_UNAVAILABLE`。不得新增 code。
- 零副作用：认证失败发生在 session/call/decision/task/reservation/event 写入之前；不得返回 200 `IvrDecision`、不得领取任务、不得预占/结算、不得写 outbox 业务事实。
- A2 保持：同一 `callId`/`eventId`/payload 的重试使用新的 timestamp/nonce；A2 idempotency 返回首次结果；不重拨、不新建 `callId`、不重复预占/结算。IVR 不得因 401/403 重新 resolve 获取新授权。
- **IVR outbox 401/403 本地策略**：当 IVR outbox 投递后观测到 `401` 或 `403`，同一 outbox entry 保持既有 `PENDING` 状态，并在本地 operational metadata 可用时逻辑标记为 security-blocked；不得自动热重试、不得新增/改写 event identity、不得改 payload、不得重拨、不得 re-resolve、不得预占或结算、不得写 A2 业务事实。必须发出不含 credential/signature/nonce 的 alert/audit signal。只有在收到显式 credential/key/certificate 或 service-authorization recovery signal 后才允许重试；重试使用同一 domain identity（`callId`/`eventId`/`recordingId`/`decisionId`）与 immutable payload，并生成新的 timestamp/nonce。数值 retry/backoff/alert 值仍为 Q8/Q13/Q25 deferred。这是 A3 对 A2 deferred authentication boundary 的 security-layer refinement：A2 server-side 仍未命名 401/403，且 `PENDING` 是 A2 既有四态之一，因此不新增 A2 state/event/digest 或 server classification。
- 密钥轮换：新/旧 keyId 重叠验签；旧 key 只 `VERIFY_ONLY`；重叠结束旧 key 拒绝。未知 keyId 直接拒绝，不 fallback 到默认 key。

### 4.8 Provider callback 签名与 replay

- 适用面：`POST /api/v1/callbacks/payment-events`；必须同时具备 `providerCallbackMtls` 与 `providerCallbackSignature`。
- Header：`X-ONECALL-CALLBACK-SIGNATURE`、`X-ONECALL-TIMESTAMP`、`X-ONECALL-NONCE`；vendor signature 算法、canonicalization、payload 字段与窗口全部 Q6/Q15 typed deferred。
- 验证顺序：provider mTLS identity → header → provider profile（Q6）→ timestamp window → constant-time signature verify → event identity/payload fingerprint（exact duplicate 返回 200 duplicate；同 event 异 payload 返回 409）→ nonce `SET NX` → 支付处理。exact duplicate 是零副作用的幂等结果，不要求消费 nonce；新事件或 changed payload 必须先通过 nonce 检查。
- 失败映射：签名/证书/时间窗/nonce replay → `401 CALLBACK_SIGNATURE_INVALID`；同 event 不同 payload → `409 PAYMENT_EVENT_CONFLICT`；依赖不可用 → `503 CALLBACK_RETRY_LATER`；重复同 event/同 fingerprint → `200 {accepted:true, duplicate:true}`。
- 原始 provider payload 由 adapter 加密留档（A0 `payment_event.payload_ciphertext`），不得明文日志、不得返回内部结果；响应只含 `accepted`/`duplicate`。
- provider nonce 是按 HTTP delivery 的 transport replay 保护；vendor 重试若复用 nonce 属 vendor policy Q6。A3 冻结 fail-closed 与 event idempotency，不发明 vendor 重试行为。
- A2 不受影响：provider callback 不参与 IVR call/event 状态机；无 redial/reservation/settlement 副作用。

### 4.9 媒体上传/播放凭证与 COS 私有对象策略

**Upload**

- 签发：`declareIvrRecording` 的 `RecordingDeclaration.upload.credential`；C09。
- scope：单次录音对象、仅 PUT、限 size/mime/duration 声明范围、有效至 `uploadExpiresAt`；不得 read/list/delete；绑定 `(callId,recordingId,objectIdentityDigest,purpose=UPLOAD)`。
- 形态（唯一主 shape）：short-lived、single-object Tencent COS presigned PUT credential。仅在 CV-4 验证 required method/object/expiry 与 supported size restrictions 后签发；若这些能力无法验证，签发必须 fail closed，实施停止并回到架构决议，**不得**静默 fallback 到 custom HMAC token 或 signing gateway。
- 签发/完成分工：`onecall:evidence-upload:v1` 是签发侧 policy/audience 分类，不是 COS 内嵌 audience claim。签发时应用逻辑校验 `callId`/`recordingId`/`purpose` 并推导 hidden target object；COS 只执行已验证的 provider constraints（PUT-only、object、expiry、size 等）。上传观测/完成时应用逻辑校验 server-observed object identity、hash、size、MIME/duration 与 declaration binding。A1 字段仍是单个 opaque 1-2048 redaction-required credential，可能 URL-shaped；永不日志，也不拆分为 public object 字段。
- 响应边界：不返回独立 object key/bucket/version；非永久 URL；response-only、`x-onecall-log-redaction: required`、非 return-once；replay 返回首次 snapshot，不续期/不重签。
- 路径：IVR 经受控 HTTPS 出口直传私有 COS；上传完成由服务端观察对象并重算 SHA-256/size/mime/duration（A2 complete）；客户端声明不是验证事实。
- 失败：credential 过期/越界/读操作 → deny + `SECURITY_UPLOAD_CREDENTIAL_DENIED`；签发依赖不可用 → `503 SERVICE_UNAVAILABLE`（declare 错误集不含 STORAGE_UNKNOWN）；complete 观测失败 → `503 STORAGE_UNKNOWN`；不得创建 evidence。

**Playback**

- 签发：`createPublicShareAdmission` 的 `mediaAuthorization.credential` 与 `authorizePublicShareMedia` 的 `MediaAuthorization.credential`；C10。
- scope：audience `onecall:media-playback:v1`；绑定 `shareId/admissionId/revocationGeneration/objectIdentityDigest/purpose ∈ {PLAYBACK,SEEK,RENEW}`；有效期 ≤ session/admission 到期并受 Q22 TTL；不得 write。
- 形态：opaque media-gateway bearer token `v1.<keyId>.<base64url(HMAC-SHA-256(...))>`；media gateway 校验签名/audience/expiry/generation/object scope 后用自己的 CAM 身份读取 COS；不向客户端返回 COS URL/key/bucket/version。
- 传输：C10 以 `Authorization: Bearer <credential>` 发送到已鉴权的 media-gateway data plane。bearer credential 单独在服务端解析其授权对象/scope；客户端不提供 COS object key/bucket/version 或任何 public A1 resource identifier。这**不是**新的 A1 业务 operation。精确 gateway origin/path、CDN/LB wiring、range/streaming protocol 与 delivery topology 是 typed deployment input/residual，owner Infrastructure/Security，destination 为 media gateway deployment / storage capacity，见 `R-A3-MEDIA-GATEWAY-TRANSPORT`；fixtures 只验证 bearer presentation、token redaction、audience/object/purpose/generation/expiry scope 与拒绝，不验证任何具体 URL/route。
- 撤销：已签发 credential 不远程召回；其有效期受 Q22 撤销传播窗口限制；撤销/过期后新签发拒绝（`SHARE_REVOKED`/`SHARE_EXPIRED`/`SHARE_SESSION_EXPIRED`）。`SHARE_LOCKED` 只阻止 P2 新准入，不阻止已准入 P3 授权。
- 失败：scope/audience/expiry/generation 不匹配 → deny + `SECURITY_PLAYBACK_CREDENTIAL_DENIED`；依赖不可用 → `DEPENDENCY_UNAVAILABLE`/`SHARE_SESSION_POLICY_UNAVAILABLE`（A1 既有 503）；不得降级为永久 URL。

**COS private policy**

- Bucket 固定私有；禁止匿名 read/write/list；TLS-only；block public access。
- 对象按类别分 prefix/类：原始录音、派生预览、转写、摘要、导出；原始对象不被派生覆盖；具体 bucket/prefix 命名与 region Q11。
- 服务端加密使用 KMS CMK K10（SSE-KMS）；COS 版本控制；对象 key 不可猜测且永不 public。
- object lock/WORM、storage class、retention、region、VPC/private endpoint 或 HTTPS 路径全部 Q11/CV-4，**不声称已验证**。
- 访问只经 CAM service role 或短时 upload/playback credential；日志只记录 object identity digest/hash prefix，不记录 key/bucket/version/URL。

### 4.10 审计访问、高风险操作控制

- 审计真源是 PostgreSQL `audit.audit_fact`；日志平台副本不是审计真源。
- 只通过 `audit.append_fact` 写入；`audit_reader_role` 只有 SELECT；无 UPDATE/DELETE/TRUNCATE；snapshot 必须 redacted 或 AEAD，denylist 适用。
- 读取仅限 `employee` domain + 固定 ADMIN/OPS 的 `listOperationsAuditFacts`；customer/ivr/public-capability/provider 一律拒绝；projection 只含 `RedactedAuditSnapshot`（status/revision/reasonCode），不含 maps/标识符/PII/payload/provider 字段。
- 高风险员工 mutation 固定 10 个（§5.9）：必须校验 employee session、固定 role、当前 status/revision、expected revision、reason 来源；在**同一事务**写 domain mutation + `audit.append_fact`；audit 失败则整体回滚，不得吞异常。
- 二次确认：运营 UI 二次确认是架构要求的运行时高风险控制；A3 不做 UI/prototype 工作，静态模型/validator/fixtures 不声称其已实现或已证明，标记为 runtime-unverified，并从 validator-assertable success criteria 移除。MFA 由 Q1 决定，A3 不实现。无新 role、无审批引擎。
- reason 语义：`ACTION_DERIVED` 只是当前 A1 请求 schema 所需的 A0 `reason_code` 技术投影，不是员工手工输入的动机，也不关闭架构的语义 operation-reason 要求；该语义缺口由 `R-A3-A1-EMPLOYEE-REASON-PROJECTION` 承接。A0 同事务 audit/reason fail-closed 不变。
- 审计读取、导出、拒绝写 access event；审计记录本身不写第二条 audit fact（避免 read amplification）。39 行 registry 每行必须带 `persistence` 判别（`append_fact` 或 `access_event`）：`AUDIT_READ` 与 `AUDIT_READ_DENIED` 固定为 `access_event`，且不得递归调用 `audit.append_fact`；其余 37 行为 `append_fact`。
- 敏感数据被员工/支持读取时写 `SECURITY_SENSITIVE_READ` audit fact（redacted object/field class，不含值）。

### 4.11 确定性日志/Trace/Error/Analytics/Support 脱敏

- 统一 redaction pipeline 在任何 emit 前执行：结构化 allow-list → 递归 deny-list → URL/credential pattern → 出口策略。
- Deny-list（大小写不敏感、任意深度）至少包含 A0 §10.7 全部键：`phone,mobile,callerNumber,calleeNumber,providerSubject,identityNumber,idNumber,legalName,shareCode,otp,objectKey,objectVersionId,bucket,providerTransactionId,providerEventId,secret,token,password,hmac,ciphertext,keyRef,kmsKey`；A3 追加 `credential,capabilityToken,extractionCode,authorization,cookie,signature,nonce,accessNumberHmac`。
- 允许 masked 变体（如 `calleeNumberMasked`）与明确非敏感业务字段；不得把 encrypted envelope 当普通日志字段。
- 固定 redaction token：`[REDACTED:<CLASS>]`（例如 `[REDACTED:CREDENTIAL]`、`[REDACTED:PHONE]`）；同一输入在相同出口必须产生相同 token，保证确定性；不得用随机值。
- 禁止记录：request/response body（认证/回调/媒体凭证接口）、Authorization/Cookie/签名/nonce header、完整 URL/query、`capabilityToken` path 段、upload/playback credential、provider payload、KMS key_ref、ciphertext、OTP/share code。
- 路径脱敏：`/api/v1/public/shares/{capabilityToken}/...` 记录为 `/api/v1/public/shares/[REDACTED:CAPABILITY_TOKEN]/...`；不得记录完整 Referer/URL。
- Error：只返回 stable Problem code、用户可理解信息、`traceId`；不返回 provider/stack/SQL/secret；安全失败统一泛化。
- Analytics/支持视图：只允许内部 ID、masked 值、status/reason、计数；不授予播放权、不展示 object key/URL/credential/HMAC/ciphertext；支持视图字段 allow-list 固定。
- 出口失败关闭：未知字段或 redaction 失败 → 丢弃/`SECURITY_REDACTION_BLOCKED`；对外响应路径返回 `500 INTERNAL`（不泄露原因），日志路径丢弃字段并告警；不得“先记录后清洗”。

### 4.12 Fail-closed 矩阵与 A2 exactly-once 保持

| 故障 | 受影响面 | 既有错误（不新增 code） | 副作用 |
| --- | --- | --- | --- |
| KMS/DEK decrypt 不可用、key disabled/destroyed、unknown key_ref | customer/operations/ivr | `503 SERVICE_UNAVAILABLE` | 不返回 plaintext；无业务写入 |
| 同上 | public-share | `503 DEPENDENCY_UNAVAILABLE` 或 `503 SHARE_SESSION_POLICY_UNAVAILABLE` | 不签发媒体凭证；不重计准入 |
| 同上 | provider | `503 CALLBACK_RETRY_LATER` | 不推进支付/权益 |
| Secret Manager/IVR signing key 不可用 | ivr | `503 SERVICE_UNAVAILABLE` | 不验签成功、不进入 A2 |
| Redis/nonce store 不可用 | ivr | `503 SERVICE_UNAVAILABLE` | 不消费 nonce、不进入 A2 |
| Redis/session store 不可用 | customer/employee/public-share | `503 SERVICE_UNAVAILABLE` / `503 SHARE_SESSION_POLICY_UNAVAILABLE` | 不绕过认证；不创建会话 |
| COS/upload signing 不可用 | ivr declare | `503 SERVICE_UNAVAILABLE` | 不签发 upload credential |
| COS/storage unknown | ivr complete | `503 STORAGE_UNKNOWN` | 不产生 media fact、不终态变更 |
| COS/playback signing 不可用 | public-share P3 | `503 DEPENDENCY_UNAVAILABLE`/`503 SHARE_SESSION_POLICY_UNAVAILABLE` | 不签发 credential |
| audit append 失败 | operations/ivr 媒体/支付 | `500 INTERNAL`（或该面既有 503，若无 4xx 语义） | 领域事务回滚；无半写 |
| HMAC key 不可用 | 任意 lookup | 该面既有 503 | 不做明文扫描、不返回假空 |
| redaction pipeline 失败 | 任意出口 | `500 INTERNAL` / 丢弃日志字段 | 不 emit 原文；告警 |
| MFA/IdP 不可用（Q1） | employee/customer 登录 | 该面既有 401/503 | 不建 session |
| provider profile/nonce 不可用 | provider | `503 CALLBACK_RETRY_LATER` | 不推进支付 |

**A2 exactly-once 保持规则**

1. 认证/签名/timestamp/nonce/replay 失败在 `resolve`、`confirm`、`direct`、`events`、`declare`、`complete` 的 A2 处理前返回；不得创建/复用 session、call、decision、task、reservation、call_event、recording、media_object、usage_fact。
2. 同一 `callId`/`eventId`/`recordingId`/`decisionId` 的身份规则不变；重试不得 mint 新身份。
3. IVR outbox 重传保留 `eventId` 与 payload，只更新 timestamp/nonce；不得因 401/403 改 eventId、重拨或重新 resolve。观测到 401/403 时按 §4.7 的 IVR-local 策略：同一 entry 保持 `PENDING`（可逻辑标记 security-blocked），不热重试，只在显式 recovery signal 后用同一 domain identity + immutable payload + 新 timestamp/nonce 重试；无 A2 业务副作用。
4. A2 的 event registry digest、12 messages、22 identities、15 invariants、outbox 分类、终态吸收、UNKNOWN/reconciliation、settlement 规则不变。
5. 安全失败不得转换为 `200 IvrDecision`/`200 CallEventReceipt`/`200 RecordingCompletion` 的业务拒绝；不得用“trusted network”或 IP allow-list 绕过签名。
6. 若 credential/nonce 修复后重试，A2 首次结果仍由原 identity 返回；不得重复结算/扣量/建媒体对象。

### 4.13 Tencent KMS/Secret Manager/CAM/COS 能力映射

| 能力 | 固定要求 | A3 映射 | 验证输入 |
| --- | --- | --- | --- |
| KMS CMK + DEK envelope | CMK 材料不出 KMS；每 purpose key/version 可 decrypt | K01 + K02-K09；CMK 保护 DEK；envelope key_ref | CV-1 |
| KMS rotation/disable/recover | 旧 version 可解；disable/re-enable；pending deletion 可取消/恢复若支持 | key states/rotation/recovery policy | CV-1 |
| Secret Manager versioned secret | versioned storage、双版本 overlap、无明文代码/镜像 | DEK wrapped secret、HMAC/signature keys | CV-2 |
| CAM least privilege | per-service role、无 `*`、无长期 AccessKey、环境隔离 | 6 service identities；DB NOLOGIN roles | CV-3 |
| COS private bucket | 私有、禁匿名、TLS、SSE-KMS、versioning | S3 media policy/COS policy | CV-4 |
| COS single-object upload auth | 短时、单对象、限操作/size | C09 = 唯一 primary shape：COS presigned PUT；仅当 CV-4 验证 method/object/expiry/size 后可用；否则 fail closed 并停止 | CV-4 |
| COS object lock/WORM | **不假设** | 只记录 Q11 输入；不声称 WORM | Q11/CV-4 |
| COS private network/VPC endpoint | **不假设** | 记录 HTTPS/private endpoint 为部署输入 | Q11/CV-4 |
| CloudAudit/log service | 补充 KMS/Secret/COS 操作证据；PostgreSQL audit 才是真源 | 服务调用类覆盖；access event | CV-5 |
| Region/SKU/price/capacity | **不发明** | 全部 deferred | Q11/CV-1..5 |

## 5. 实现必须冻结的安全注册表

### 5.1 Trust surface / auth domain registry（5）

见 §4.1。五个信任面/认证域来源为 `docs/12` §8 五个 API face（第 269-277 行）与 A1 `x-onecall-audience` 五个值；`docs/12` §11.1 的四个认证域是认证机制分类，不是五面枚举。实现必须在 `security-domain-map` 冻结 5 行，domain 值必须精确等于 A1 `x-onecall-audience` 的五个值，operation 数精确 `18/21/6/3/1`；同时在模型 `trustBoundaries` 冻结 7 个边界（TB1-TB7），不得新增/合并/删除。

### 5.2 Service identity registry（6）

见 §4.2。实现必须冻结 6 个 service identity，每个有唯一 CAM principal/audience、allowed calls、secret custody、rotation、fail-closed；禁止通配符资源/action；前端不持有 service secret。

### 5.3 Key registry（20）

| ID | keyClass | custodyStorage | purpose | algorithm | dataClasses | rotation | recovery |
| --- | --- | --- | --- | --- | --- | --- | --- |
| K01 | `KMS_CMK_DATA` | Tencent KMS（region CV-1） | root KEK for DEK | KMS symmetric CMK | K02-K09 | CMK version rotation CV-1 | re-enable/restore CV-1 |
| K02 | `DEK_PROVIDER_SUBJECT` | KMS-wrapped Secret Manager version + memory | provider subject AEAD | AES-256-GCM/12/16/AAD | D01 | new version; old decrypt-only | SM version + KMS |
| K03 | `DEK_IDENTITY_PROFILE` | 同上 | legal name/id number AEAD | 同上 | D02 | 同上 | 同上 |
| K04 | `DEK_VERIFICATION_REF` | 同上 | verification ref AEAD | 同上 | D02/D05 | 同上 | 同上 |
| K05 | `DEK_PHONE_NUMBER` | 同上 | caller/callee/number AEAD | 同上 | D04/D06/D07/D09 | 同上 | 同上 |
| K06 | `DEK_IVR_DECISION` | 同上 | frozen decision payload AEAD | 同上 | D08 | 新写；append-only 旧版只解 | 同上 |
| K07 | `DEK_CALL_EVENT` | 同上 | call event payload AEAD | 同上 | D10 | 同上 | 同上 |
| K08 | `DEK_PROVIDER_PAYLOAD` | 同上 | provider payload AEAD | 同上 | D11 | 同上 | 同上 |
| K09 | `DEK_INTERNAL_PAYLOAD` | 同上 | audit/outbox/idempotency encrypted snapshot | 同上 | D27/D28 | 同上 | 同上 |
| K10 | `COS_CMK_MEDIA` | Tencent KMS + COS SSE-KMS | media object at rest | COS SSE-KMS CV-4 | D19 | COS/KMS rotation CV-4 | COS/KMS CV-4 |
| K11 | `HMAC_PROVIDER_SUBJECT` | Secret Manager | provider subject lookup | HMAC-SHA-256/256-bit | D01 | dual-read; old lookup-only | SM version |
| K12 | `HMAC_ID_NUMBER` | Secret Manager | id number lookup | 同上 | D02 | 同上 | 同上 |
| K13 | `HMAC_PHONE_NUMBER` | Secret Manager | caller/binding/access number lookup | 同上 | D04/D06/D07/D09；accessNumberHmac | 同上 | 同上 |
| K14 | `HMAC_EMPLOYEE_LOGIN` | Secret Manager | employee login lookup | 同上 | D03 | 同上 | 同上 |
| K15 | `HMAC_EMPLOYEE_AUTH_SUBJECT` | Secret Manager | employee auth subject lookup | 同上 | D03 | 同上 | 同上 |
| K16 | `HMAC_SHARE_DESIGNATED_MOBILE` | Secret Manager | designated mobile proof | 同上 | D24 | 新 grant 新版；旧 snapshot 保留 version | 同上 |
| K17 | `IVR_REQUEST_SIGNING` | Secret Manager | IVR request HMAC | HMAC-SHA-256/256-bit | C04 | active + verify-only overlap | SM version |
| K18 | `PROVIDER_CALLBACK_VERIFICATION` | Secret Manager/cert store | provider signature verify | vendor profile Q6 | C06 | provider rotation Q6 | provider/SM |
| K19 | `MEDIA_UPLOAD_SIGNING` | Secret Manager/COS signing material | upload credential issuance | COS profile CV-4 | C09 | overlap until uploadExpiresAt | SM/COS |
| K20 | `MEDIA_PLAYBACK_SIGNING` | Secret Manager | playback token signing | HMAC-SHA-256/256-bit | C10 | overlap until max TTL + Q22 | SM version |

Key registry 的每个 row 必须有唯一 owner、存储位置、允许状态、允许算法和 fail-closed 错误；不得出现用途重叠（encryption key 不得签 HMAC，HMAC key 不得 AEAD）。

### 5.4 Credential registry（10）

| ID | credential | domain | audience | storage | TTL input | scope | rotation/revocation | redaction |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| C01 | customerSession | customer | `onecall:customer-session:v1` | Redis session + account | Q1/Q19 | customer API only | logout/account disable/expiry | bearer/Authorization 必删 |
| C02 | employeeSession | employee | `onecall:employee-session:v1` | Redis + employee | Q1 | operations API only | role change/disable/reset revoke | 同上 |
| C03 | ivrMtls | ivr-service | `onecall:ivr-request:v1` | cert store | Q7/Q15 | 仅 IVR mTLS 身份 | cert overlap/rotate Q7 | cert/key 不日志 |
| C04 | ivrSignature | ivr-service | `onecall:ivr-request:v1` | Secret Manager K17 | Q25 window | 每请求签名 | keyId overlap | signature/nonce 必删 |
| C05 | providerCallbackMtls | provider | `onecall:provider-callback:v1` | cert store | Q6/Q15 | provider callback only | provider rotation | cert 不日志 |
| C06 | providerCallbackSignature | provider | `onecall:provider-callback:v1` | Secret Manager/cert K18 | Q6 | 每回调签名 | provider rotation | signature/nonce 必删 |
| C07 | publicShareCapabilityToken | public-capability | `onecall:share-capability:v1` | DB SHA-256 digest only | share `expiresAt` / Q12 | path capability；一次创建 | revoke/generation；不可重建 | token 必删/return-once |
| C08 | extractionCode | public-capability | `onecall:share-capability:v1` | per-grant salt + slow KDF digest | Q12 | EXTRACTION_CODE | code_generation rotation | code/OTP 必删/return-once |
| C09 | uploadCredential | ivr-service | 签发侧 policy/audience 分类 `onecall:evidence-upload:v1`（非 COS 内嵌 audience） | COS presign（CV-4 verified）/K19 | `uploadExpiresAt` | single-object COS presigned PUT only；CV-4 能力不可验证则 fail closed + stop | 不续期/不重签；expiry | credential URL 必删 |
| C10 | playbackCredential | public-capability | `onecall:media-playback:v1` | media gateway K20 | Q22 | `Authorization: Bearer` 到 media-gateway；share/admission/generation/object；PLAYBACK/SEEK/RENEW | 旧 credential 不召回；expiry | credential URL 必删 |

字段边界沿用 A1：`capabilityToken` 1-512，`extractionCode` 1-128，`upload.credential`/`mediaAuthorization.credential`/`MediaAuthorization.credential` 1-2048；不得改变这些 schema 或把它们变成 return-once。

### 5.5 Data-class registry（28）

见 §4.5。实现必须冻结 D01-D28 与 A0 §8.1 的逐行映射；validator 必须断言 `a0_sensitive_rows=28`、`dataClasses=28`，无 missing/extra/duplicate，且每个 data class 的 owner/key/HMAC/representation 唯一且不跨 purpose。

### 5.6 Signature profiles（2）

**IVR profile**：见 §4.7；固定 canonical string、HMAC-SHA-256、keyId、timestamp、nonce、验证顺序和失败映射。窗口值 Q25。

**Provider profile**：见 §4.8；固定 mTLS+签名+timestamp+nonce 的验证顺序、replay/duplicate/conflict 语义、payload 加密留档和不泄露内部结果；vendor algorithm/canonicalization/payload/window Q6/Q15。

### 5.7 Media policies（2）

**Upload policy U1**：唯一 primary shape 为 short-lived、single-object Tencent COS presigned PUT credential，仅在 CV-4 验证 required method/object/expiry 与 supported size restrictions 后签发；能力不可验证则 fail closed 并停止，不 fallback。签发侧应用校验 `callId`/`recordingId`/`purpose` 并推导 hidden target object；`onecall:evidence-upload:v1` 为签发侧 policy/audience 分类，非 COS 内嵌 audience。COS 执行已验证 provider constraints；complete 时应用校验 server-observed object identity/hash/size/MIME/duration 与 declaration binding。A1 字段为单个 opaque 1-2048 redaction-required credential，可能 URL-shaped、永不日志/拆分。单对象、PUT-only、size/mime/duration scope、`uploadExpiresAt`、response-only、no read/list/delete、no permanent URL、server observation/re-hash、A2 complete 边界不变。

**Playback policy P1**：C10 以 `Authorization: Bearer <credential>` 发送到已鉴权的 media-gateway data plane；bearer credential 单独在服务端解析授权对象/scope，客户端不提供 COS key/bucket/version 或 public A1 resource identifier。gateway origin/path、CDN/LB、range/streaming 与 delivery topology 为 Infrastructure/Security 的 typed deployment residual（`R-A3-MEDIA-GATEWAY-TRANSPORT`），不属于 A1 wire。media-gateway audience、share/admission/generation/object/purpose scope、Q22 TTL、no write、no COS URL/key/bucket/version、revoke/expiry generation、`SHARE_LOCKED` 只阻止新准入不变。

### 5.8 审计/访问事件 registry（39）与调用类覆盖（10）

审计/访问事件 registry 每行固定 `persistence`：`append_fact`（经 `audit.append_fact` 写 audit fact）或 `access_event`（写访问事件，不写 audit fact）。`AUDIT_READ`/`AUDIT_READ_DENIED` 固定 `access_event`，且不得递归调用 `audit.append_fact`；其余 37 行为 `append_fact`。actor/object/reason 均使用稳定 code，不含值/secret。39 个 action code：

| # | actionCode | actor | objectType | trigger | persistence |
| ---: | --- | --- | --- | --- | --- |
| 1 | `SECURITY_KEY_ACTIVATE` | SERVICE/SYSTEM | KEY | key version 激活 | `append_fact` |
| 2 | `SECURITY_KEY_ROTATE` | SERVICE/SYSTEM | KEY | 新旧 version overlap | `append_fact` |
| 3 | `SECURITY_KEY_RETIRE` | SERVICE/SYSTEM | KEY | 旧 version retire | `append_fact` |
| 4 | `SECURITY_KEY_DISABLE` | SERVICE/SYSTEM | KEY | 紧急 disable | `append_fact` |
| 5 | `SECURITY_KEY_RECOVER` | SERVICE/SYSTEM | KEY | re-enable/restore | `append_fact` |
| 6 | `SECURITY_KEY_DESTROY` | SYSTEM | KEY | pending/executed destroy | `append_fact` |
| 7 | `SECURITY_KEY_PURPOSE_MISMATCH` | SERVICE/SYSTEM | KEY | purpose/dataClass 不匹配 | `append_fact` |
| 8 | `SECURITY_KEY_ACCESS_DENIED` | SERVICE/SYSTEM | KEY | 未授权 key/secret 访问 | `append_fact` |
| 9 | `SECURITY_AUTH_DENIED` | SERVICE/SYSTEM | CREDENTIAL | 会话/凭据无效 | `append_fact` |
| 10 | `SECURITY_AUDIENCE_MISMATCH` | SERVICE/SYSTEM | CREDENTIAL | 跨域 token/credential | `append_fact` |
| 11 | `SECURITY_SERVICE_IDENTITY_DENIED` | SERVICE/SYSTEM | SERVICE | mTLS/CAM 身份不允许 | `append_fact` |
| 12 | `SECURITY_SIGNATURE_INVALID` | SERVICE/SYSTEM | REQUEST | IVR/provider 签名失败 | `append_fact` |
| 13 | `SECURITY_REPLAY_REJECTED` | SERVICE/SYSTEM | REQUEST | nonce replay | `append_fact` |
| 14 | `SECURITY_TIMESTAMP_REJECTED` | SERVICE/SYSTEM | REQUEST | timestamp 超窗 | `append_fact` |
| 15 | `SECURITY_PROVIDER_CALLBACK_REJECTED` | SERVICE/SYSTEM | PAYMENT_EVENT | provider 回调拒绝 | `append_fact` |
| 16 | `SECURITY_UPLOAD_CREDENTIAL_ISSUED` | SERVICE | RECORDING | upload credential 签发 | `append_fact` |
| 17 | `SECURITY_UPLOAD_CREDENTIAL_DENIED` | SERVICE | RECORDING | upload scope/expiry deny | `append_fact` |
| 18 | `SECURITY_PLAYBACK_CREDENTIAL_ISSUED` | SERVICE | SHARE_ADMISSION | playback credential 签发 | `append_fact` |
| 19 | `SECURITY_PLAYBACK_CREDENTIAL_DENIED` | SERVICE | SHARE_ADMISSION | playback scope/expiry deny | `append_fact` |
| 20 | `SECURITY_COS_POLICY_DENIED` | SERVICE/SYSTEM | MEDIA_OBJECT | 私有策略/对象访问拒绝 | `append_fact` |
| 21 | `AUDIT_READ` | EMPLOYEE | AUDIT | 审计读取 access event | `access_event` |
| 22 | `AUDIT_READ_DENIED` | SERVICE/EMPLOYEE | AUDIT | 审计读取拒绝 | `access_event` |
| 23 | `OPERATIONS_HIGH_RISK_DENIED` | EMPLOYEE | OPERATIONS | 高风险 role/state/reason 拒绝 | `append_fact` |
| 24 | `SECURITY_SENSITIVE_READ` | EMPLOYEE/SERVICE | DATA_CLASS | 员工/支持读取敏感字段 | `append_fact` |
| 25 | `SECURITY_REDACTION_BLOCKED` | SERVICE/SYSTEM | EXPORT | 未知字段/脱敏失败被阻断 | `append_fact` |
| 26 | `SECURITY_REDACTION_VIOLATION` | SERVICE/SYSTEM | EXPORT | 检测到明文/secret 出口 | `append_fact` |
| 27 | `SUBJECT_SUSPEND` | EMPLOYEE | SUBJECT | OPS suspend | `append_fact` |
| 28 | `SUBJECT_RESUME` | EMPLOYEE | SUBJECT | OPS resume | `append_fact` |
| 29 | `CATALOG_VERSION_PUBLISH` | EMPLOYEE | CATALOG_VERSION | ADMIN publish | `append_fact` |
| 30 | `CATALOG_VERSION_OFFSALE` | EMPLOYEE | CATALOG_VERSION | ADMIN offsale | `append_fact` |
| 31 | `TRIAL_POLICY_CREATE` | EMPLOYEE | TRIAL_POLICY | ADMIN create draft | `append_fact` |
| 32 | `TRIAL_POLICY_REPLACE` | EMPLOYEE | TRIAL_POLICY | ADMIN replace draft | `append_fact` |
| 33 | `TRIAL_POLICY_PUBLISH` | EMPLOYEE | TRIAL_POLICY | ADMIN publish | `append_fact` |
| 34 | `TRIAL_POLICY_DISABLE` | EMPLOYEE | TRIAL_POLICY | ADMIN disable | `append_fact` |
| 35 | `CATALOG_DRAFT_CREATE` | EMPLOYEE | CATALOG_VERSION | ADMIN create draft | `append_fact` |
| 36 | `CATALOG_DRAFT_REPLACE` | EMPLOYEE | CATALOG_VERSION | ADMIN replace draft | `append_fact` |
| 37 | `SHARE_REVOKE` | ACCOUNT | SHARE_GRANT | customer revoke | `append_fact` |
| 38 | `SHARE_CODE_ROTATE` | ACCOUNT | SHARE_GRANT | customer code rotation | `append_fact` |
| 39 | `PAYMENT_CALLBACK_ACCEPTED` | SERVICE | PAYMENT_EVENT | accepted/duplicate provider event | `append_fact` |

调用类覆盖 10 个（每个有 owner、认证、audit/access 类、redaction、fail-closed）：

`CUSTOMER_API`、`OPERATIONS_API`、`IVR_API`、`PUBLIC_SHARE_API`、`PROVIDER_CALLBACK_API`、`INTERNAL_SERVICE_CALL`、`KMS_OPERATION`、`SECRET_MANAGER_READ`、`COS_OPERATION`、`DATABASE_ACCESS`。普通读取与 `AUDIT_READ`/`AUDIT_READ_DENIED` 使用 access event；§5.8 其余审计事件/高风险/凭据/拒绝按每行 `persistence` 使用 audit fact（`append_fact`）。KMS/Secret/COS provider 日志仅作补充，CV-5；audit truth 仍是 PostgreSQL。

### 5.9 高风险操作 registry（10 employee mutations + 3 sensitive actions）

| operationId | role | reasonSource | state/precondition | auditCode | before/after |
| --- | --- | --- | --- | --- | --- |
| `suspendOperationsSubject` | OPS | REQUEST `reasonCode` | ACTIVE + expected revision | `SUBJECT_SUSPEND` | status/revision/reason |
| `resumeOperationsSubject` | OPS | REQUEST `reasonCode` | SUSPENDED + expected revision | `SUBJECT_RESUME` | status/revision/reason |
| `publishOperationsCatalogVersion` | ADMIN | ACTION_DERIVED（技术投影，见下） | never-published DRAFT + expected active/revision | `CATALOG_VERSION_PUBLISH` | status/revision/snapshot hash |
| `offsaleOperationsCatalogVersion` | ADMIN | ACTION_DERIVED | ACTIVE + expected revision | `CATALOG_VERSION_OFFSALE` | status/revision |
| `createOperationsTrialPolicyDraft` | ADMIN | ACTION_DERIVED | never-published natural key | `TRIAL_POLICY_CREATE` | policy identity/revision |
| `replaceOperationsTrialPolicyDraft` | ADMIN | ACTION_DERIVED | never-published + expected revision | `TRIAL_POLICY_REPLACE` | revision/snapshot hash |
| `publishOperationsTrialPolicy` | ADMIN | ACTION_DERIVED | expected enabled/revision | `TRIAL_POLICY_PUBLISH` | enabled/publisher/revision |
| `disableOperationsTrialPolicy` | ADMIN | ACTION_DERIVED | enabled + expected revision | `TRIAL_POLICY_DISABLE` | enabled/revision |
| `createOperationsCatalogVersionDraft` | ADMIN | ACTION_DERIVED | natural key + DRAFT | `CATALOG_DRAFT_CREATE` | identity/revision |
| `replaceOperationsCatalogVersionDraft` | ADMIN | ACTION_DERIVED | DRAFT + expected revision | `CATALOG_DRAFT_REPLACE` | revision/snapshot hash |

- `ACTION_DERIVED` 仅是当前 A1 请求 schema 所需的 A0 `reason_code` 技术投影，**不是**员工手工输入的动机，也不关闭架构的语义 operation-reason 要求；该语义缺口保持 `R-A3-A1-EMPLOYEE-REASON-PROJECTION`，owner Product/Operations + API governance，destination 为未来 A1/UI refinement。A0 同事务 audit/reason fail-closed 行为不变（audit append 失败则 domain mutation 回滚）。
- UI 二次确认仍是架构要求的运行时高风险控制，但 A3 不做 UI/prototype 工作；静态模型、validator 与 fixtures **不**声称其已实现或已证明。该控制标记为 runtime-unverified，**不是** validator-assertable success criterion；S3 fixtures 只断言 role/state/reasonSource/audit-failure-rollback。

敏感非员工操作（audit/access 事件，不新增 role）：

- `revokeCustomerShare` → `SHARE_REVOKE`（ACCOUNT），不改写历史证据。
- `rotateCustomerShareCode` → `SHARE_CODE_ROTATE`（ACCOUNT），generation +1，响应不返回 capability token。
- `receivePaymentProviderCallback` → `PAYMENT_CALLBACK_ACCEPTED`；拒绝走 `SECURITY_PROVIDER_CALLBACK_REJECTED`；同 event/payload 幂等。

### 5.10 Deferred inputs（18 A0 Q 子集）与 capability verification（5 A3 CV，共 23）

23 行 = 18 个未改变的 A0 Q 子集 row + 5 个 A3-origin `CV-1..CV-5` cloud-capability verification row。CV rows 不占用 Q 编号，也不修改 A0 §14；18 个 Q rows 的 owner/destination 必须与 `docs/14` §14 逐行相等，validator 必须分别断言两组（不是把 23 行当作同一来源）。

| ID | 输入 | valueKind | A3 只做什么 | Owner | Destination |
| --- | --- | --- | --- | --- | --- |
| Q1 | employee auth source、MFA、credential rotation | IDENTITY_PROVIDER/POLICY | 只冻结独立 employee domain/audience/fixed roles；不实现 MFA/IdP | Security/Backend | Security design + operations IAM |
| Q3 | legal retention、erasure、backup deletion、audit retention | LEGAL_RETENTION | 冻结 rotation/recovery 必须先满足 retention；不选期限 | Legal/Security | Retention/deletion policy + launch review |
| Q6 | payment merchant/app IDs、payload、callback retention、vendor signature | VENDOR_POLICY | 只冻结 adapter 验证顺序/幂等/fail-closed；不定义 vendor 字段/算法 | Payments/Backend | Payment adapter |
| Q7 | SIP/line/access number、cert issuance、recording format/size、buffer、clock sync | VENDOR_POLICY/NUMERIC | 只冻结 mTLS/签名接口与 fail-closed；不定义线路/格式/证书值 | Telephony/IVR | IVR protocol + line integration |
| Q8 | duration tolerance、SLO/timeouts | NUMERIC | 不定义 tolerance/SLO/timeout | IVR/Backend/QA | IVR protocol + capacity |
| Q10 | recording notice/consent wording、prompt lifecycle | POLICY | 只传 prompt/policy ref | Product/Legal | Consent policy |
| Q11 | COS region/storage class/object-lock/WORM/private access/media retention | CLOUD_CAPABILITY/LEGAL | 固定私有 bucket/SSE-KMS/versioning 要求；不声称 WORM/region | Infrastructure/Security | Storage/security/capacity |
| Q12 | share code KDF params、admission/expiry/rate-limit、OTP lifecycle | NUMERIC/POLICY | 只冻结 salt/generation/KDF 接口；不选参数 | Security/Product | Security design + sharing policy |
| Q13 | audit/Outbox retention、DLQ thresholds、replay ops | NUMERIC/POLICY | 只冻结 audit/access 分类；不选 retention | Operations/Backend | Operations runbook/observability |
| Q14 | real-name provider/evidence/re-verification | VENDOR_POLICY | 只投影状态；不定义证件字段/provider | Security/Product | Verification adapter |
| Q17 | Flyway role provisioning、ownership transfer、least-privilege execution | PLATFORM | 只冻结 logical service/DB 权限边界 | Platform/Database | Deployment/security design |
| Q18 | 30s TTL config key/approval/scheduler | CONFIG/NUMERIC | 使用已冻结 30s 默认；不定义配置键 | Product/Backend | IVR protocol + operations config |
| Q19 | WeChat appid/openid/unionid mapping | IDENTITY_PROVIDER | 只冻结 customer domain/audience；不定义登录/合并 | Identity/Backend | Authentication design |
| Q22 | Redis ticket TTL、clock skew、revocation propagation | NUMERIC | 冻结 credential fail-closed/TTL 上限语义；不选值 | Security/Sharing | Security design + sharing policy |
| Q23 | deployment role 是否可创建 SECURITY DEFINER/trigger functions | PLATFORM | 只冻结角色/所有权契约；不选择部署身份 | Platform/Database/Security | Deployment/security design |
| Q25 | authorizationExpiresAt window、clock skew | NUMERIC | 冻结 timestamp/nonce 验证顺序；不选窗口 | IVR/Entitlement/Backend | IVR protocol/config/capacity |
| Q27 | organization task create/claim permission matrix | ROLE_MATRIX | 保留 A0/A1 lifecycle/access 检查；不扩角色矩阵 | Organization-access/Security | Authorization design |
| Q28 | IVR-local B-leg key/recovery | PLATFORM | 只读 A2 已有本地恢复；不新增协议 | Telephony/IVR | IVR protocol/line integration |
| CV-1 | KMS region/edition symmetric CMK、envelope/wrap、rotation/disable/recover/pending-delete | CLOUD_CAPABILITY | 逻辑 K01-K09 profile 已冻结；能力待验证 | Infrastructure/Security | Q2 closure + COS/KMS validation |
| CV-2 | Secret Manager region/edition versioning/rotation/dual-version | CLOUD_CAPABILITY | 逻辑 custody 已冻结；能力待验证 | Infrastructure/Security | Q2 closure + deployment security |
| CV-3 | CAM role/policy least privilege and workspace isolation | CLOUD_CAPABILITY | 6 service identities 已冻结；provisioning 待验证 | Infrastructure/Security | Q17/Q23 deployment |
| CV-4 | COS private bucket/SSE-KMS/versioning/presign（method/object/expiry/size）/private endpoint/object lock | CLOUD_CAPABILITY | 私有 bucket 要求固定；upload 唯一 primary shape 为 COS presigned PUT，仅当本行验证 required method/object/expiry 与 supported size 后签发，否则 fail closed 并停止（不 fallback custom HMAC/signing gateway）；region/SKU/WORM 待验证 | Infrastructure/Security | Q11 storage/capacity |
| CV-5 | CloudAudit/log service operation evidence and retention | CLOUD_CAPABILITY | PostgreSQL audit 为真源；provider log 仅补充 | Infrastructure/Security/Observability | Q13 observability |

## 6. 调用/数据流、状态机、不变量与对抗路径

### 6.1 数据加密/解密/查找流

1. 调用方提供 plaintext、`expectedPurpose`、`expectedDataClass`。
2. 服务校验 dataClass registry 存在且 keyClass 绑定该 purpose；不匹配 fail closed。
3. 从 Secret Manager/KMS 取得 active DEK/version；CSPRNG 生成 12-byte nonce；AES-256-GCM + AAD `onecall:v1:<purpose>:<dataClass>` 加密。
4. 组装 `v1.<key_ref>.<base64url(nonce||ct||tag)>`；写 DB；不记录 plaintext/nonce/DEK/key_ref。
5. HMAC 独立计算并写 64-hex；不得与 ciphertext 混用。
6. 读取：解析 envelope → registry 解析 key_ref → 校验 key state/purpose/dataClass → 解密；失败 fail closed。
7. 查找：用 active + 允许的历史 HMAC key 计算候选值 → indexed equality query；不扫描明文；HMAC 不可用返回 503。
8. 输出：只返回 owning service 允许字段；日志/trace/audit 只使用 masked/redacted 形式。

### 6.2 轮换/恢复流

- 状态：`ACTIVE → DECRYPT_ONLY/LOOKUP_ONLY/VERIFY_ONLY → DISABLED → DESTROYED`；只允许 §5.3 声明的用途。
- 轮换步骤：授权 → 新 version → 新写切 active → 旧 version 降级 → mutable reindex（幂等）→ frozen 行保留 → retention/窗口后 retire → audit。
- 恢复步骤：授权 → re-enable/restore → 校验 key_ref/version → 恢复读取 → audit；不可恢复则 fail closed/incident。
- 任何步骤失败不得使 plaintext 进入日志或 fallback；不得跳过 purpose 校验。

### 6.3 IVR 签名验证流

mTLS identity → required headers → timestamp window → signature verify → nonce SET NX → A1/A2 processing。任一步失败返回 §4.7 的既有 code，零副作用；成功后才允许 A2 幂等/状态机。

### 6.4 Provider callback 验证流

mTLS provider → headers → vendor profile → timestamp window → signature verify → event identity/fingerprint（exact duplicate 零副作用幂等；changed payload 冲突）→ nonce → payment idempotency。失败按 §4.8 映射；原始 payload 加密留档。

### 6.5 媒体凭证流

- Upload：declare → evidence owner 校验 call/recording/purpose/expiry 并推导 hidden target object → 仅在 CV-4 验证 presign method/object/expiry/size 后签发 C09（short-lived single-object COS presigned PUT）；能力不可验证则 fail closed + stop → IVR 受控直传 → server observation → complete 校验 server-observed object identity/hash/size/MIME/duration 与 declaration binding → evidence handoff（complete 不固化 evidence）。
- Playback：share admission → 校验 verification/generation/status → 签发 C10 → 客户端以 `Authorization: Bearer <credential>` 调用已鉴权 media-gateway data plane → gateway 由 bearer credential 解析授权对象并校验 → 读取 COS → 不返回 COS 地址；客户端不提供 COS key/bucket/version 或 public A1 resource identifier。
- 凭证不进入日志/trace/error/analytics/support；不永久化。

### 6.6 审计/高风险操作流

1. employee session + audience + fixed role。
2. 读取当前 status/revision；校验 expected revision/前置状态。
3. 运营 UI 二次确认是架构要求的运行时高风险控制；A3 不做 UI 工作，静态实现不声称该运行时控制已实现或已证明（runtime-unverified）；MFA Q1。
4. 同一事务执行 domain mutation + `audit.append_fact`（actor/reason/before-after redacted）。`ACTION_DERIVED` 仅作为技术 `reason_code` 投影，不代表员工动机。
5. audit 失败 → 整体回滚；成功 → 返回既有 A1 response。
6. 审计读取只在 employee domain；`AUDIT_READ`/`AUDIT_READ_DENIED` 以 `access_event` 记录读取/拒绝（不递归调用 `audit.append_fact`）；不暴露原始 snapshot。

### 6.7 Redaction 流

分类 → allow-list → deny-list（任意深度）→ URL/credential pattern → 出口策略 → emit。未知/失败 → block/drop/alert；不得先 emit 后清洗。public contract 只保留 masked/ID/status；support view 无 playback。

### 6.8 不变量（最终 21；S1 冻结 01-06，S2 累计 01-11，S3 累计 01-21）

| ID | 不变量 | 首次冻结 stage |
| --- | --- | --- |
| INV-A3-01 | 每个受保护数据都有唯一 dataClass、owner、representation、key/HMAC class 和 audit/access 覆盖；无未注册敏感字段。 | S1 |
| INV-A3-02 | encryption 与 HMAC key 严格分离；任何 key 不得跨 purpose；跨用途解密/查找必须失败。 | S1 |
| INV-A3-03 | envelope 形状、key_ref 解析、AEAD/AAD 固定；unknown/malformed/retired key_ref、错误 purpose 或解密失败必须 fail closed，无 plaintext fallback。 | S1 |
| INV-A3-04 | HMAC 是独立 key 的 lowercase 64-hex；只用于 equality/uniqueness/lookup；永不 returned/logged/public。 | S1 |
| INV-A3-05 | 每个 key/credential 有 owner、custody、state、rotation、recovery；secret 不进 DB/code/image/log。 | S1 |
| INV-A3-06 | 轮换不重写 append-only/frozen 行；历史 key 保留到 retention；A2 replay 返回同一 payload/decision，不重选/不重复预占。 | S1 |
| INV-A3-07 | 五个 auth domain 的 token/credential/audience/verifier 不可互换；跨域请求必须被拒绝。 | S2 |
| INV-A3-08 | 6 个 service identity 最小权限、独立 CAM role、可轮换；前端无 service secret；无 `*` 权限。 | S2 |
| INV-A3-09 | IVR 请求必须同时通过 mTLS、签名、timestamp、nonce；验证顺序和 canonical string 固定。 | S2 |
| INV-A3-10 | replay 拒绝发生在领域副作用之前；transport nonce/timestamp 每次重试更新，A2 `eventId`/payload 不变。 | S2 |
| INV-A3-11 | provider callback 必须 mTLS+厂商签名+timestamp+nonce；同 event/fingerprint 幂等，异 payload 冲突。 | S2 |
| INV-A3-12 | 媒体凭证短时、单对象/purpose、最小权限；upload 不可读/list/delete，playback 不可写；均 redacted。 | S3 |
| INV-A3-13 | COS 私有；无匿名访问；public contract/log 不出现 object key/bucket/version/permanent URL。 | S3 |
| INV-A3-14 | audit append-only via `audit.append_fact`；高风险操作要求 role/state/reason/before-after 同事务；audit 读取仅 employee。 | S3 |
| INV-A3-15 | redaction 在 emit 前确定性执行；denylist/credential URL/header secret 永不进入日志/trace/error/analytics/support/public contract。 | S3 |
| INV-A3-16 | support view 只显示 masked/redacted/ID/status；不授予录音播放或原始敏感值。 | S3 |
| INV-A3-17 | KMS/Secret Manager/Redis/nonce/COS/DB/audit 不可用时 fail closed；无 plaintext fallback、无凭证签发、无 evidence 固化、无 A2 双副作用。 | S3 |
| INV-A3-18 | 不发明 region/SKU/price/capacity/WORM/object-lock；所有生产值 typed deferred，owner/destination 明确。 | S3 |
| INV-A3-19 | 固定 ADMIN/OPS；不新增 employee role、service decomposition 或 infrastructure topology。 | S3 |
| INV-A3-20 | 每个 key/credential/service-call class 有 audit/access 归属与 testable scenario；registry 无 orphan row。 | S3 |
| INV-A3-21 | A0/A1/A2 accepted contracts 不变；validator 证明 counts/shapes/digests；任何必要修改都是 blocker。 | S3 |

Per-stage 精确计数：S1 = INV-A3-01..06（6）；S2 = 01..06 + 07..11（累计 11，新增 5）；S3 = 01..11 + 12..21（累计 21，新增 10）。S1/S2 不得包含 S2/S3-owned invariant 的全文；未来 stage 的 invariant 文本提前出现必须被 validator 拒绝。

### 6.9 负例/对抗路径（必须被拒绝）

- 用 D01 ciphertext 配 D02 key/AAD；用 encryption key 做 HMAC；用 HMAC key 做 AEAD。
- unknown/retired/destroyed key_ref、错误 purpose/dataClass、Secret Manager/KMS 不可用。
- 试图 re-encrypt/UPDATE append-only `ivr_decision`/`call_event`/`audit_fact`/`evidence_event`。
- HMAC rotation 后无法复现 A2 `accessNumberHmac` fingerprint / 旧 decision replay。
- customer token 调用 operations/ivr/public/provider；employee token 调用 customer；capability token 调用 operations；IVR signature 调用 provider。
- 错误 audience、过期/撤销 session、unknown service identity、CAM 越权。
- 缺 mTLS、坏签名、超窗 timestamp、nonce replay、未知 keyId；Redis nonce store 不可用。
- provider 坏签名、nonce replay、同 event 异 payload、依赖不可用。
- upload credential 读/list/delete、跨对象、跨 purpose、过期、超 size；playback credential 写/跨对象/跨 generation/过期/撤销后新签。
- COS 匿名访问/public URL/object key/version 泄漏；对象锁/WORM 被声称已支持。
- 非 employee 读取 audit；audit UPDATE/DELETE/TRUNCATE；高风险角色/状态/reason 缺失；audit 失败但 domain 已写。
- 日志/trace/error/analytics/support/public 输出 plaintext、HMAC、ciphertext、key_ref、object key、credential URL、capability token、signature/nonce、Authorization/Cookie。
- 认证失败返回 200 业务拒绝/`IvrDecision`/`CallEventReceipt`/`RecordingCompletion`；重拨、第二 B-leg、第二 reservation/settlement、第二 recording/media object。
- 任何新 endpoint/Problem code/table/column/state/role 或发明 region/SKU/WORM/TTL。

## 7. 静态验证与 fixtures

### 7.1 工具链与命令

在 repo root 执行（实现/controller 环境）：

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
node security/validate.mjs --stage s2 ...   # 同参数
node security/validate.mjs --stage s3 ...   # 同参数
```

补充检查：

```bash
file -bi docs/17-production-security-design-spec.md security/v1/security-design-v1.json security/validate.mjs
grep -n -E '^(<<<<<<<|=======|>>>>>>>)' docs/17-production-security-design-spec.md security/v1 security/validate.mjs
grep -n '[[:blank:]]$' docs/17-production-security-design-spec.md security/v1 security/validate.mjs
git diff --check -- docs/17-production-security-design-spec.md security
git status --short
```

注意：`npm`/`node` 若被环境权限门禁拒绝，作者不得重试；使用 controller 的 hash-bound green run 作为执行证据，并继续 `sha256sum`/`grep`/`file` 静态检查。此限制不改变 validator 契约。

### 7.2 A0/A1/A2 drift checks

validator 必须从 bundled A1 与 A0/A2 文档/模型断言：

- A0 catalog 恰 44 表；A0 sensitive block 恰 28 rows；模型 28 dataClasses 与 A0 §8.1 逐行相等（无 missing/extra/duplicate/invalid）。
- envelope regex 精确为 `^v1\.[A-Za-z0-9._:/-]+\..+$`；HMAC 为 `^[0-9a-f]{64}$`；`auth_identity.key_version` 与 envelope version 一致规则存在。
- A1 operation 恰 49：customer 18、operations 21、ivr 6、public-share 3、provider-callback 1；security scheme 恰 6；五面 audience 值精确；7 个 trust boundaries 完整；IVR/provider header 名称精确；TIMESTAMP 长度 1-64、NONCE 长度 1-128 精确；SIGNATURE header 名称精确、值格式符合 A3 冻结（A1 无长度约束，validator 不得断言其长度）。
- A1 credential 字段：`RecordingDeclaration.upload.credential`、`ShareAdmission.mediaAuthorization.credential`、`MediaAuthorization.credential` 均 `readOnly:true`、`x-onecall-log-redaction:required`、非 `x-onecall-return-once`；`ShareCreated.capabilityToken`/`extractionCode` 为 return-once；`ShareCodeRotation` 不返回 capability token。
- A1 Problem code 集合与 `docs/15` §4 精确一致；安全失败只引用既有 code；不得新增/删除/重命名。
- A2 模型/目录：12 messages、11 eventTypes、22 identities、15 invariants、25 fixtures、event registry digest 精确；A3 不修改 A2 任何字段。
- Deferred registry：18 个 `Q*` rows 与 `docs/14` §14 的 owner/destination 逐行相等；5 个 `CV-*` rows 存在、A3-origin、owner/destination 非空且不出现在 A0 §14；不得把 23 行误当作同一来源。
- 负例：任何 A1 new operation/schema/field/code、A0 new table/column/state、A2 new message/identity/invariant/digest 变化都必须使 validator 失败。

### 7.3 文档/模型一致性与 stage gating

- `security/validate.mjs --stage s1|s2|s3` 必须选择唯一投影；缺/未知 stage 失败。
- S1 必须精确包含 `security-key-map` 20 行、`security-data-map` 28 行、`security-deferred-map` 23 行；模型 `keyClasses/dataClasses/deferredInputs` 精确相等。`security-deferred-map` 必须分区断言：18 个 `Q*` rows 与 A0 §14 相等，5 个 `CV-*` rows 为 A3-origin。S1 的 `invariants` 必须恰好是 INV-A3-01..06（6），任何 INV-A3-07..21 文本出现即失败。
- S2 必须新增 `security-domain-map` 5 行、`security-service-map` 6 行、`security-credential-map` 10 行；模型对应 section 精确；S1 section 不变。`invariants` 必须累计为 INV-A3-01..11（11，新增 07..11），任何 INV-A3-12..21 文本出现即失败。
- S3 必须新增 `security-audit-map` 39 行；模型 `auditEvents` 精确；`mediaPolicies`/`highRiskActions`/`auditCoverage`/`redactionPolicy`/`failClosedMatrix` 完整。`invariants` 必须累计为 INV-A3-01..21（21，新增 12..21）。`security-audit-map` 每行必须有 `persistence` 且取值仅为 `append_fact`|`access_event`；`AUDIT_READ`/`AUDIT_READ_DENIED` 必须为 `access_event` 且不得递归调用 `audit.append_fact`，其余 37 行必须为 `append_fact`；validator 必须校验 route，不只比较 39 行计数。
- 缺当前 stage section/row/列、下一 stage section 提前出现、row 数不符、列集合不符、重复/排序变化、deferred 字段出现具体数值、未来 stage invariant 文本提前出现、audit persistence 缺失或错误，均失败。
- marker block 的 row 顺序、列名、空值记号（`-`）必须与模型完全一致；validator 不能只比较子集。
- 输出计数 `doc_missing/doc_extra/doc_duplicate/doc_invalid/token_missing/stage_field_missing/stage_field_premature/fixture_id_missing/fixture_id_extra` 必须全 0。

### 7.4 Fixture 场景与断言（23）

Fixture 格式：`{schemaVersion,id,slice,description,steps[]}`；step 为 `encrypt/decrypt/lookup/rotate/recover/request/verify/persist/assert`。`expected` 包含 status、既有 Problem code、持久化事实、key state、audit/access、计数、redaction 输出。validator 用内置 `SecurityMachine` 计算，不读取 expected 决定结果；`SecurityMachine` 只做确定性 fixture evaluation，不连接 KMS/Secret Manager/COS/Redis/DB。Fixture 的密钥/凭据必须是 `TEST_ONLY_*` 合成值，不含真实 secret。

| Fixture | Slice | 场景 | 关键断言 |
| --- | --- | --- | --- |
| `sec-s1-01-envelope-purpose-roundtrip` | S1 | 对 D01/D02/D04/D06/D07/D08/D09/D10/D11 加解密/HMAC | envelope regex；AAD purpose/dataClass；HMAC 64-hex；无 plaintext/key_ref 输出；roundtrip 一致 |
| `sec-s1-02-hmac-index-equality` | S1 | 同值/不同 purpose HMAC lookup | 同值同 HMAC；不同 purpose 不同；只返回 equality；无明文扫描 |
| `sec-s1-03-cross-purpose-denied` | S1（负例） | D01 ciphertext 配 D02 key/AAD；HMAC 错 purpose | `SECURITY_KEY_PURPOSE_MISMATCH`；零 plaintext；无数据写入 |
| `sec-s1-04-rotation-historical-replay` | S1 | K02/K05/K13 轮换；旧数据解密/HMAC 双读；A2 decision replay | 旧 key 可解；新写新 key；append-only 不改写；A2 replay 同 payload；无第二 reservation/settlement |
| `sec-s1-05-key-unavailable-failclosed` | S1（负例） | unknown/disabled/destroyed key_ref、SM/KMS 不可用 | 各面既有 503；无 plaintext fallback；audit event；无状态写入 |
| `sec-s1-06-append-only-rotation-safety` | S1 | 尝试重写 frozen ciphertext/HMAC；retention 未定时 retire；deferred/invariant stage 边界 | 拒绝 UPDATE；旧 version `DECRYPT_ONLY`/`LOOKUP_ONLY`；无 A0 变更；`deferred_inputs=23` 且 18 Q + 5 CV 分区正确；`invariants=6` 且无 INV-A3-07..21 文本 |
| `sec-s2-01-five-domains-accepted` | S2 | 五个域各自有效 credential | 仅本面 2xx；audience/计数正确；无跨域副作用 |
| `sec-s2-02-cross-domain-rejected` | S2（负例） | customer/employee/capability/ivr/provider token 交叉使用 | A1 既有 401/403/404；零 A2 状态写入 |
| `sec-s2-03-service-identity-least-privilege` | S2 | 6 service identities allowed/denied calls | 仅 allowed calls；无 `*`；前端无 secret；拒绝审计 |
| `sec-s2-04-ivr-signature-invalid` | S2（负例） | 缺/坏 mTLS、坏签名、超窗、未知 keyId、坏 nonce | `401 UNAUTHENTICATED`/`403 FORBIDDEN`；无 session/call/decision/task/reservation/event；header 名与 TIMESTAMP 1-64/NONCE 1-128 精确，SIGNATURE 名称+值格式符合 A3（无长度断言） |
| `sec-s2-05-ivr-replay-rejected` | S2（负例） | 同 nonce replay；Redis 不可用；修复后同 eventId 新 nonce 重试 | 401/503；无副作用；重试后 A2 首次结果；无 redial/第二 reservation |
| `sec-s2-06-provider-signature-replay` | S2（负例） | 坏签名、nonce replay、duplicate、changed payload | 401 `CALLBACK_SIGNATURE_INVALID`；duplicate 200；changed 409 `PAYMENT_EVENT_CONFLICT`；无重复支付/权益 |
| `sec-s2-07-signing-key-rotation` | S2 | IVR/provider key 新旧 overlap/retire | 旧 key `VERIFY_ONLY` 可验；retire 后 401；无状态副作用 |
| `sec-s2-08-failclosed-dependency-preserves-a2` | S2 | SM/Redis 不可用；恢复后同 identity 重试 | 503；无 A2 写入；outbox 不改 eventId；A2 digest/状态不变 |
| `sec-s2-09-ivr-outbox-auth-blocked` | S2（负例） | IVR outbox 分别观测到 401 与 403；显式 credential/key/certificate 或 service-authorization recovery 后重试 | 同一 outbox entry 保持既有 `PENDING`（可逻辑标记 security-blocked）；不自动热重试、不新增/改写 event identity、不改 payload、不重拨、不 re-resolve、不预占/结算；alert/audit signal 不含 credential；recovery 后使用同一 domain identity + immutable payload + 新 timestamp/nonce；零 A2 业务副作用；`invariants=11` 且无 INV-A3-12..21 文本 |
| `sec-s3-01-upload-credential-scope` | S3 | 签发/使用 upload credential | 唯一 primary shape：COS presigned PUT（CV-4 验证 method/object/expiry/size）；单对象/单 purpose；跨对象/purpose/过期/超 size/read/list/delete deny；无永久 URL；complete 时 server-observed identity/hash/size/MIME/duration 与 declaration binding 重校验；CV-4 能力不可验证时 fail closed + stop，不 fallback custom HMAC |
| `sec-s3-02-playback-credential-scope` | S3（负例） | playback credential 以 `Authorization: Bearer` 呈现；跨对象/purpose/generation/过期/撤销 | bearer presentation 被验证且 credential 被 redacted；bearer 单独解析 object/scope，客户端不提供 key/bucket/version/public resource ID；跨范围 deny；无写；无 COS URL；不重计准入；旧 credential 仅到 Q22 TTL；不验证任何具体 gateway URL/route |
| `sec-s3-03-cos-private-policy` | S3 | 匿名读/公网 URL/对象 key 泄漏；SSE-KMS/versioning | 匿名 deny；public contract 无 key/bucket/version/URL；WORM 不声称 |
| `sec-s3-04-audit-access-denied` | S3（负例） | customer/ivr/public/provider 读 audit；employee 读；DB 改 audit | 非 employee 拒绝；employee 只 redacted；`audit_reader_role` SELECT-only；无 UPDATE/DELETE/TRUNCATE；39 行 `persistence` 全部存在；`AUDIT_READ`/`AUDIT_READ_DENIED` 路由为 `access_event` 且不递归调用 `audit.append_fact`；其余 37 行为 `append_fact` |
| `sec-s3-05-high-risk-controls` | S3 | 10 employee mutations 的 role/state/reason/audit | 合法通过；坏 role/state/reason 拒绝；audit 失败回滚；last-admin 不变；仅断言 role/state/reasonSource/audit-failure-rollback，不把 UI 二次确认作为可断言项 |
| `sec-s3-06-redaction-deterministic` | S3 | 递归 deny-list、masked、credential URL、header、未知字段 | 固定 `[REDACTED:*]`；masked 允许；未知字段 block；无原文 |
| `sec-s3-07-credential-url-leak` | S3（负例） | capabilityToken path、upload/playback credential、signature/nonce、Authorization/Cookie | 所有出口无 secret；public 只 masked/ID；analytics/support 无 playback |
| `sec-s3-08-end-to-end-security` | S3 | IVR 媒体 + share playback + audit + provider callback | 全链路身份/凭证/审计/脱敏/fail-closed；complete 不固化 evidence；counts 正确；`invariants=21` 且 stage 累计完整 |

累计 fixture 数：S1=6，S2=15，S3=23；slice `6+9+8`；显式负例累计 `2/7/10`。S2 的 `sec-s2-09` 是新增的 401/403 outbox 负例。

### 7.5 负例与确定性

- 负例必须被模型/evaluator 拒绝：跨 purpose、unknown/retired key、append-only 重写、跨域 token、bad mTLS/signature/timestamp/nonce、provider payload conflict、IVR outbox 观测 401/403 时的 hot retry/identity 变化/redial/reservation/settlement、upload/playback scope 越界（含 upload 在 CV-4 未验证时 fallback）、COS public/URL、audit 越权/改写、`AUDIT_READ`/`AUDIT_READ_DENIED` 误走 `append_fact`、high-risk 缺 role/state/reason、redaction 泄漏、A2 双副作用。
- stage gating 负例：缺 stage、缺 marker block、row/列数不符、未来 stage section/row/invariant 文本提前、audit `persistence` 缺失或错误、deferred 出现具体数值、18 Q/5 CV 分区错误、fixture ID 缺/多、把 UI 二次确认当作可断言项。
- 无网络、无当前时钟、无 CSPRNG 依赖的真实随机值：fixture nonce/key 固定为 `TEST_ONLY_*`；AEAD 使用 fixture 固定 nonce 以确定性复现（生产必须 CSPRNG，fixture 仅 evaluator 输入）。
- 重复运行 stdout 与 fixture 结果 SHA-256 必须一致；两次运行 hash 相同。
- 输出固定计数行：

```text
stage=s1 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=6 fixtures=6 passed=6 failed=0 negative=2 slice_fixtures=6
a0_tables=44 a0_sensitive_rows=28 a1_operations=49 a1_schemes=6 a2_messages=12 a2_event_types=11 a2_identities=22 a2_invariants=15 a2_event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9 drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
PASS security-validation (s1)
```

```text
stage=s2 trust_surfaces=5 auth_domains=5 trust_boundaries=7 service_identities=6 credential_classes=10 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=11 fixtures=15 passed=15 failed=0 negative=7 slice_fixtures=6+9
a0_tables=44 a0_sensitive_rows=28 a1_operations=49 a1_schemes=6 a2_messages=12 a2_event_types=11 a2_identities=22 a2_invariants=15 a2_event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9 drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
PASS security-validation (s2)
```

```text
stage=s3 trust_surfaces=5 auth_domains=5 trust_boundaries=7 service_identities=6 credential_classes=10 signature_profiles=2 media_policies=2 audit_events=39 audit_persistence=39 audit_access_events=2 audit_append_facts=37 high_risk_actions=10 audit_coverage=10 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=21 fixtures=23 passed=23 failed=0 negative=10 slice_fixtures=6+9+8
a0_tables=44 a0_sensitive_rows=28 a1_operations=49 a1_schemes=6 a2_messages=12 a2_event_types=11 a2_identities=22 a2_invariants=15 a2_event_registry_sha256=c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9 drift=0
doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0 token_missing=0
stage_field_missing=0 stage_field_premature=0 fixture_id_missing=0 fixture_id_extra=0
PASS security-validation (s3)
```

### 7.6 不可证明项

- 不能证明真实 Tencent KMS/Secret Manager/CAM/COS 的能力、region、SKU、WORM/object-lock、private endpoint、价格或容量（CV-1..5/Q11）。
- 不能证明真实 mTLS/证书签发/签名算法/时钟同步/Redis nonce store/provider vendor 行为（Q1/Q6/Q7/Q15/Q25）。
- 不能证明真实 session/OTP/WeChat 登录/employee MFA（Q1/Q19）。
- 不能证明 PostgreSQL 并发/审计事务回滚/append-only trigger/role 权限的真实执行（A0/Q17/Q23）。
- 不能证明 gateway/edge/analytics/log collector 的运行时脱敏配置；A3 只冻结 contract 与 negative fixtures。
- 不能证明 media-gateway data plane 的部署 origin/path、CDN/LB wiring、range/streaming protocol 或 delivery topology；这些是 `R-A3-MEDIA-GATEWAY-TRANSPORT` 的部署输入，fixtures 不验证具体 URL/route。
- 不能证明真实 COS 上传/观测/SSE-KMS/版本/恶意文件扫描/evidence 固化（A2/Q11）。
- fixtures 是安全模型级断言，不是生产验收矩阵或渗透测试。

## 8. 实施切片（3 个）

### 8.1 切片原则与最小性

- S1 是“数据与密钥真相”：一次性冻结哪些数据用哪个 purpose/key、如何 envelope/HMAC、如何轮换/恢复/历史 replay。
- S2 是“身份与请求真相”：一次性冻结五个认证域、service identity/audience、IVR/provider 签名与 replay，且证明失败时 A2 exactly-once 不变。
- S3 是“媒体、审计与控制真相”：一次性冻结 upload/playback 凭证、COS 私有策略、audit access、high-risk 和确定性脱敏。
- 不按文件、文档章节、云产品或 owner 拆。合并 S1+S2 会让“数据密钥”与“认证签名”两类独立失败模式无法分别验证；合并 S2+S3 会让 transport auth 与 media/audit 不变量混杂。三片各有独立 fixtures、stop condition 和 residual，是满足“≤3”的最小方案。

### 8.2 S1 — 敏感数据保护、密钥层级、轮换/恢复

| 项 | 内容 |
| --- | --- |
| 目标 | 建立 `docs/17` + 安全模型基座；冻结 20 keys、28 data classes、23 deferred（18 Q + 5 CV）、envelope/HMAC、purpose/AAD、key states、rotation/recovery、A2 replay 兼容与 S1-owned 6 invariants（INV-A3-01..06；最终累计到 S3 的 21）；产出 S1 fixtures |
| 允许文件 | `docs/17-production-security-design-spec.md`（新建）；`security/v1/security-design-v1.json`（新建）；`security/v1/fixtures/sec-s1-*.json`（新建）；`security/validate.mjs`（新建）；`docs/gateflow/a3-production-security-design-spec-s1-implementation.md`（新建） |
| 前置 | accepted A0/A1/A2；A1 bundle/A2 model 可读；无 runtime/cloud 依赖 |
| 变更 | `envelopeProfile`（AES-256-GCM/12/16/AAD）、`hmacProfile`、20 keyClasses、28 dataClasses、23 deferred（18 Q + 5 CV）、keyStates/rotation/recovery/replayCompatibility、6 invariants（INV-A3-01..06）、`security-key-map`/`security-data-map`/`security-deferred-map`；6 个 S1 fixtures |
| 状态转移 | key `ACTIVE→DECRYPT_ONLY/LOOKUP_ONLY/DISABLED→DESTROYED`；mutable rotation/reindex；frozen 保留旧 version |
| 错误处理 | unknown key/purpose/decrypt/lookup → 各面既有 503；无 plaintext fallback；audit `SECURITY_KEY_*` |
| 验证命令 | `cd api/openapi && npm ci && npm run check:s3`；`node security/validate.mjs --stage s1 ...`；`node protocol/ivr/validate.mjs --stage s3 ...`；`file -bi`；冲突标记/trailing whitespace/`git status --short` |
| 期望断言 | `key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=6 fixtures=6 passed=6 negative=2`；A0 44/28、A1 49/6、A2 12/11/22/15/digest；drift=0；无新表/列/状态/字段；无 INV-A3-07..21 提前文本 |
| 完成信号 | 报告列出 20 keys、28 data rows、23 deferred、envelope/HMAC 规则、rotation/recovery/replay 证据、fixture 输出、无 runtime 声明 |
| 停止条件 | 需要修改 A0 envelope/HMAC/schema、A1 wire、A2 identity/digest；需要新 key 无法映射 A0 敏感行；无法保留 append-only 历史；需要发明数值/policy |
| 未覆盖/残留下沉 | S2 身份/签名/replay；S3 媒体/audit/redaction；Q1/Q2 capability/Q3/Q6/Q7/Q8/Q10/Q11/Q12/Q13/Q14/Q17/Q18/Q19/Q22/Q23/Q25/Q27/Q28；真实 KMS/SM |

### 8.3 S2 — 认证域、服务身份、签名与 replay 拒绝

| 项 | 内容 |
| --- | --- |
| 目标 | 冻结五域、6 service identities、10 credentials、2 signature profiles；实现 IVR/provider mTLS+签名+timestamp+nonce+replay 的确定性语义；冻结 IVR outbox 401/403 本地安全策略；新增 INV-A3-07..11（累计 11）；证明失败时 A2 exactly-once/replay 不变；产出 S2 fixtures |
| 允许文件 | `docs/17...`（修改）；`security/v1/security-design-v1.json`（修改）；`security/v1/fixtures/sec-s2-*.json`（新建）；`security/validate.mjs`（修改）；`docs/gateflow/...-s2-implementation.md`（新建） |
| 前置 | S1 accepted；S1 key/data registry 已存在；A2 `R-A2-SIGNATURE` 仍可映射 |
| 变更 | `trustSurfaces` 5、`authDomains` 5、`trustBoundaries` 7、`serviceIdentities` 6、`credentialClasses` 10、`signatureProfiles` 2、`noncePolicy`、`audienceRegistry`、cumulative `invariants` 11（新增 INV-A3-07..11）、IVR outbox 401/403 本地策略；`security-domain-map`/`security-service-map`/`security-credential-map`；9 个 S2 fixtures |
| 状态转移 | session `UNAUTHENTICATED→AUTHENTICATED→REVOKED/EXPIRED`；mTLS/signature key `ACTIVE→VERIFY_ONLY→DISABLED`；nonce `ABSENT→SEEN(TTL)`；无 A2 新状态 |
| 错误处理 | 401/403/503 使用 A1 既有 code；签名/replay 失败零 A2 副作用；provider duplicate/conflict 既有 200/409；A2 重试同 identity；IVR outbox 观测 401/403 时保持 `PENDING`/security-blocked、不热重试、显式 recovery 后同 identity/payload + 新 nonce |
| 验证命令 | A1 `check:s3`；A2 `validate.mjs --stage s3`；`node security/validate.mjs --stage s2 ...`；UTF-8/whitespace/scope |
| 期望断言 | `trust_surfaces=5 auth_domains=5 trust_boundaries=7 service_identities=6 credential_classes=10 invariants=11 fixtures=15 passed=15 negative=7 slice_fixtures=6+9`；A1 49/6、A2 digest 不变；drift=0；无新 header/schema/code；无 INV-A3-12..21 提前文本 |
| 完成信号 | 报告列出 5 domains/6 services/10 credentials/2 profiles、canonical string、nonce/replay 顺序、A2 exactly-once 证据、fixture 输出、deferred Q1/Q6/Q7/Q15/Q19/Q25/Q27/Q28 |
| 停止条件 | 需要新 Problem code/header/schema/role；需要固定窗口/TTL 数值；需要 vendor algorithm/payload；需要修改 A2 outbox/state/digest；无法证明零副作用 |
| 未覆盖/残留下沉 | Q1 MFA/IdP、Q6 vendor、Q7 cert/line、Q15 wire、Q19 WeChat、Q25 window、Q27 matrix、Q28 local；R-A3-A2-AUTH-OUTBOX（401/403 状态策略已在 §4.7 冻结，剩余 Q8/Q13/Q25 数值与集成验证）；真实 Redis/mTLS |

### 8.4 S3 — 媒体凭证、COS 私有策略、审计访问、高风险控制与脱敏

| 项 | 内容 |
| --- | --- |
| 目标 | 冻结 upload/playback media policies、COS private policy、39 audit/access events（含 persistence）、10 high-risk actions、10 call-class coverage、deterministic redaction、fail-closed matrix；新增 INV-A3-12..21（累计 21）；产出 S3/端到端 fixtures |
| 允许文件 | `docs/17...`（修改）；`security/v1/security-design-v1.json`（修改）；`security/v1/fixtures/sec-s3-*.json`（新建）；`security/validate.mjs`（修改）；`docs/gateflow/...-s3-implementation.md`（新建） |
| 前置 | S1/S2 accepted；A2 media 状态/identity/digest 不变；A1 media credential schema 不变 |
| 变更 | `mediaPolicies` U1/P1、`cosPolicy`、`auditEvents` 39（每行 `persistence`）、`highRiskActions` 10、`auditCoverage` 10、`redactionPolicy`、`failClosedMatrix`、cumulative `invariants` 21（新增 INV-A3-12..21）；`security-audit-map` 39（含 persistence）；8 个 S3 fixtures |
| 状态转移 | 不新增 A0 状态；upload credential `ISSUED→EXPIRED/DENIED`（逻辑，不建表）；playback credential 同；share/evidence 状态沿用 A0/A1/A2；high-risk 操作仍 state-conditional |
| 错误处理 | A1 既有 401/403/404/409/410/503；upload/playback scope deny；audit 失败回滚；redaction 失败 block；无新 code |
| 验证命令 | A1 `check:s3`；A2 `validate.mjs --stage s3`；`node security/validate.mjs --stage s3 ...`；UTF-8/whitespace/scope；两次运行 hash |
| 期望断言 | `media_policies=2 audit_events=39 audit_persistence=39 audit_access_events=2 audit_append_facts=37 high_risk_actions=10 audit_coverage=10 invariants=21 fixtures=23 passed=23 negative=10 slice_fixtures=6+9+8`；A0 44/28、A1 49/6、A2 digest；drift=0；无 secret/URL 泄漏 |
| 完成信号 | 报告列出 media policies、COS fixed vs Q11、39 audit codes、10 high-risk、10 call classes、redaction tokens、fail-closed matrix、fixture 输出、两次运行 hash、无 runtime/cloud 声明 |
| 停止条件 | 需要新 endpoint/role/table/column/COS/WORM 能力；需要固定 TTL/retention/rate/KDF 数值；需要 operations playback；需要修改 A1 media schema 或 A2 state；CV-4 无法验证 COS presign method/object/expiry/size（upload 签发 fail closed，停止等待架构决议，不 fallback） |
| 未覆盖/残留下沉 | Q3 retention、Q11 COS/WORM/region、Q12 KDF/limits、Q13 audit volume、Q22 TTL/skew、Q23 provisioning；R-A3-A1-EMPLOYEE-REASON-PROJECTION/R-A3-A1-UPLOAD-CREDENTIAL-SHAPE/R-A3-MEDIA-GATEWAY-TRANSPORT；真实 logging/gateway/analytics/COS |

## 9. 文档决策、风险、开放问题与残留下沉

### 9.1 文档决策

- 新增且仅新增 `docs/17-production-security-design-spec.md` 作为人类 canonical 安全设计；不复制 A1 schema、A0 表字典、A2 消息表。
- `security/v1/security-design-v1.json` 是唯一机器安全 registry；fixtures 与 validator 围绕它。
- Gateflow 每个 slice 单独报告；不把实现报告混入 canonical 规范。
- 不修改 `docs/12`-`docs/16`、A0/A1/A2 accepted artifacts、OpenAPI、prototype、screenshots、`shared/onecall-business.js`、control doc。

### 9.2 风险、开放问题与残留下沉

| ID | 分类 | 内容 | Owner / Destination |
| --- | --- | --- | --- |
| `R-A3-A1-EMPLOYEE-REASON-PROJECTION` | A1/A0 投影边界 | 发布类 ADMIN operation 无 operator reason；`ACTION_DERIVED` 仅为当前 A1 schema 所需的 A0 `reason_code` 技术投影，**不是**员工动机，也不关闭架构的语义 operation-reason 要求；UI 二次确认是架构要求的 runtime 高风险控制，但 A3 不做 UI 工作，标记 runtime-unverified 且不作为 validator-assertable；A0 同事务 audit/reason fail-closed 不变 | Product/Operations + API governance；未来 A1/UI refinement |
| `R-A3-A1-UPLOAD-CREDENTIAL-SHAPE` | A1/设计边界 | 唯一 primary shape 为 short-lived single-object COS presigned PUT，仅在 CV-4 验证 method/object/expiry/size 后使用；否则 fail closed 并停止，不 fallback custom HMAC/signing gateway；A1 字段为单个 opaque 1-2048 redaction-required credential，可能 URL-shaped、永不日志/拆分 | Security/Backend/Evidence；API/evidence review |
| `R-A3-MEDIA-GATEWAY-TRANSPORT` | 部署/传输边界 | C10 bearer presentation 已冻结：`Authorization: Bearer <credential>` 到已鉴权 media-gateway data plane，bearer 单独解析对象/scope，客户端不提供 COS key/bucket/version 或 public A1 resource identifier；精确 gateway origin/path、CDN/LB wiring、range/streaming protocol 与 delivery topology 未定，且不新增 A1 业务 operation | Infrastructure/Security；media gateway deployment / storage capacity |
| `R-A3-A2-AUTH-OUTBOX` | A2 分类边界 | A2 server-side 仍未命名 401/403；A3 在 §4.7 冻结 IVR-local 策略：same `PENDING`/security-blocked、不热重试、显式 recovery 后同 domain identity/payload + 新 timestamp/nonce、零 A2 业务副作用；A2 event/state/digest/classification 不变；numeric retry/backoff/alert 仍 Q8/Q13/Q25 | IVR/Backend；A2/IVR integration + Q8/Q13/Q25 |
| `R-A3-HMAC-ROTATION-REPLAY` | 运行时兼容 | HMAC 轮换 vs A2 `accessNumberHmac` fingerprint/冻结 HMAC；需历史 key 候选和 retention；未运行时证明 | Security/IVR/Backend；integration + Q3/Q25 |
| `R-A3-KMS-CAPABILITY` | 云能力 | KMS/Secret Manager envelope/rotate/recover/dual-version 未验证（CV-1/CV-2） | Infrastructure/Security；Q2/CV validation |
| `R-A3-COS-CAPABILITY` | 云能力 | COS region/private endpoint/SSE-KMS/versioning/presign/object-lock/WORM 未验证（CV-4/Q11） | Infrastructure/Security；storage/capacity |
| `R-A3-REDACTION-RUNTIME` | 运行时控制 | 不能证明 edge/gateway/analytics/log collector 配置；A3 只冻结 contract/fixtures | Platform/Security/Observability；logging review |
| `R-A3-AUDIT-VOLUME` | 容量/retention | audit/access event volume、retention、DLQ 未定（Q13） | Operations/Backend；runbook/observability |
| `R-A3-SESSION-ISSUANCE` | 身份 | customer/employee session 签发/audience/revocation 依赖 Q1/Q19 | Identity/Backend；authentication design |
| `R-A3-PROVIDER-VENDOR` | vendor | provider signature/canonicalization/payload/window 依赖 Q6/Q15 | Payments/Backend；payment adapter |
| `R-A3-MFA` | 身份 | MFA/IdP 未实现（Q1） | Security/Backend；operations IAM |
| `R-A3-LEGAL-RETENTION` | legal | retention/erasure/backup 未选（Q3） | Legal/Security；launch review |
| `R-A3-NO-WORM` | 云能力 | 不声称 COS WORM/object-lock；未来需 Q11 验证 | Infrastructure/Security；storage policy |
| `R-A3-KEY-DESTRUCTION` | 恢复 | destroyed key 不可恢复；无明文备份/escrow 除非另行批准 | Security/Infrastructure；incident/recovery policy |
| `R-A3-SUPPORT-VIEW` | 产品/权限 | A1 无 operations playback；未来 break-glass 不在 A3 | Product/Security/Operations；future work unit |
| `R-A3-REGION-SKU` | 云能力 | 不选 region/SKU/price/capacity | Infrastructure/Security；capacity/deployment |

开放问题：无 blocking question。上述条目均为 typed residual/deferred，不阻止 planning；若实现阶段发现必须修改 accepted contract，按 stop condition 报告 blocker。

### 9.3 Deferred inputs（不发明）

见 §5.10 的 23 行，精确为 **18 + 5**：18 个未改变的 A0 Q 子集 rows（Q1/Q3/Q6/Q7/Q8/Q10/Q11/Q12/Q13/Q14/Q17/Q18/Q19/Q22/Q23/Q25/Q27/Q28）的 owner/destination 与 `docs/14` §14 逐行相等；另有 5 个 **A3-origin** `CV-1..CV-5` cloud-capability verification rows，owner/destination 非空，不占用 Q 编号、不修改 A0 §14。即 A3 确实新增了 5 个 CV 输入行（不是“不新增 open-input row”），23 = 18 + 5。validator 必须分别断言 18 Q 与 A0 相等、5 CV 为 A3-origin；不把 deferred 值写成 fixture 成功条件。

## 10. 实施 Agent 完成报告模板

每个 slice 报告必须包含：

1. Task label、provider/model、gate、base commit、slice ID。
2. 精确变更文件列表（只允许 §8 中该 slice 的路径）。
3. 本 slice 的 registry counts、marker block rows、fixture ID 与累计计数。
4. 安全决策摘要：key/data/purpose、envelope/HMAC、rotation/recovery、domain/audience、signature/replay、media/audit/redaction。
5. 执行过的命令、exit code、关键输出（A1 `check:s3`、A2 `validate.mjs --stage s3`、`security/validate.mjs --stage sN`、fixture passed/failed、UTF-8/whitespace/scope）。
6. Drift 证据：`a0_tables=44`、`a0_sensitive_rows=28`、`a1_operations=49`、`a1_schemes=6`、A2 12/11/22/15 + digest、`drift=0`、doc/stage/fixture counters=0。
7. 负例证据：哪些 fixture 被拒绝、哪些副作用计数保持 0（A2 reservation/settlement/event/media、A0 audit mutation、COS credential）。
8. Deferred/residual：owner/destination；明确未实现项。
9. 明确限制：没有运行真实 KMS/Secret Manager/CAM/COS/Redis/DB/IVR/日志平台/analytics。
10. 完成/阻塞声明；若阻塞，列出只读证据、未修改路径、需要谁决策。
11. 明确声明：只改允许路径；未 stage/commit/push/PR/merge；未进入下一 Gate。

S3 报告还必须给出：5 surfaces、6 services、20 keys、28 data、10 credentials、2 signature、2 media、39 audit/access（persistence: 2 `access_event` + 37 `append_fact`）、10 high-risk、10 call classes、23 deferred（18 Q + 5 CV）、21 invariants（stage 累计 6/11/21）、23 fixtures、A0/A1/A2 drift=0、A2 digest 不变、两次运行 hash 相同、Tencent fixed vs CV 声明、无 WORM/region/SKU 声称。

## 11. 无过度设计与目标对齐

| Goal area | 设计决策 | Slice | Fixtures | Validation |
| --- | --- | --- | --- | --- |
| key hierarchy/purpose separation | 20 keyClasses、KMS CMK→DEK→data、purpose/dataClass/AAD 绑定、独立 HMAC | S1 | `sec-s1-01/03` | key/data registry、purpose 负例、A0 drift |
| envelope/HMAC wire shape | 保持 `v1.<key_ref>.<base64url(...)>` 与 64-hex HMAC；只补解析/算法/版本规则 | S1 | `sec-s1-01/02` | A0 regex/HMAC/sensitive 28 行 drift |
| rotation/recovery/historical/replay | key states、append-only 不重写、旧 version 保留、A2 replay 同一 payload | S1 | `sec-s1-04/05/06` | state/rotation/replay counters、A2 digest |
| five auth domains | 5 domain、独立 credential/audience/verifier、跨域拒绝 | S2 | `sec-s2-01/02` | A1 audience/scheme/operation drift |
| service identities/audiences | 6 service identities、CAM 最小权限、audience 注册表 | S2 | `sec-s2-03` | service map、无 `*`/前端 secret |
| IVR/provider signing/replay | mTLS+signature+timestamp+nonce；canonical string；A2 前 fail-closed；401/403 outbox 无热重试 | S2 | `sec-s2-04/05/06/07/08/09` | A1 header/schema、A2 exactly-once 负例 |
| media credentials/COS | upload/playback scope、短时、COS 私有/SSE-KMS/versioning、无永久 URL | S3 | `sec-s3-01/02/03` | A1 media schema、COS policy 负例、Q11 |
| audit/high-risk/redaction | audit append-only、employee-only read、10 high-risk、deny-list/URL/header redaction | S3 | `sec-s3-04/05/06/07/08` | A0 audit 契约、39 audit codes、redaction 负例 |
| Tencent mapping/deferred | fixed requirement vs CV-1..5/Q11；23 typed inputs（18 Q + 5 CV）；不发明 region/SKU/WORM/TTL | S1-S3 | deferred map | `deferred_inputs=23 deferred_q=18 deferred_cv=5`、无数值断言、CV rows |
| success signal | 每个 datum/key/credential/service call 有 owner/storage/purpose/rotation/fail-closed/audit/test | S1-S3 | 23 fixtures | registry counts + coverage + negative fixtures |

- **不新增 surface**：全部控制映射 A1 49 operation；upload/playback/audit 不新增 endpoint。
- **不新增持久化契约**：所有 data class 引用 A0 44 表与 28 sensitive rows；不新增表/列/状态/事务。
- **不新增错误码**：所有安全失败使用 A1 既有 `Problem.code`。
- **不新增角色/拓扑**：固定 ADMIN/OPS；6 service identities 映射既有 units；`media-gateway` 只是逻辑校验点。
- **不发明数值/能力**：window/TTL/retention/KDF/region/SKU/WORM/CV 全部 typed deferred。
- **三片最小**：S1 数据/密钥、S2 身份/签名、S3 媒体/审计/脱敏；每片有独立 fixtures、验证信号、stop condition。
- **每项设计可回溯**：key hierarchy ← `docs/12` §11 + `docs/14` §2.6/§8；五域 ← `docs/12` §8 五个 API face + A1 `x-onecall-audience`（`docs/15` §1；`docs/12` §11.1 的四个认证域是认证机制分类，不是五面枚举）；签名 ← A1 header 名称/长度 + A2 `R-A2-SIGNATURE`；媒体 ← `docs/12` §4 + A1 media schema + A2 §7；audit/high-risk ← A0 §4.5/§9.3 + `docs/12` §11.1；redaction ← A0 §10.7 + `docs/15` §5。
- **不把风险偷藏为设计**：A1/A2 投影边界、云能力、runtime 脱敏、HMAC rotation replay、retention 全部显式 residual。

## 12. 计划完成定义

- 本文件存在且 UTF-8；无冲突标记、无 trailing whitespace；只创建本文件。
- 包含 §1-§12 的全部必需内容：目标/动机/成功信号/非目标/边界、第一性原理与精确证据、精确文件/模型/registry、key/data/domain/credential/signature/media/audit/deferred 设计、状态/流/不变量/负例、静态验证/fixtures、≤3 切片及每片完整字段、文档/风险/残留下沉、完成报告、无过度设计说明。
- 三个切片行为边界清晰且可独立验证；不按文件/云产品/owner 机械拆分。
- 无 blocking question。若实现阶段发现无法用 A0/A1/A2 表达的语义矛盾，必须停止并作为 blocker 报告，不得静默修改 accepted contract。
- 本 Gate 完成后停止；不实施、不进入 plan review 之外的 Gate、不 stage/commit/push/PR。
