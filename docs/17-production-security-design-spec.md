# 生产安全设计规范（S1+S2+S3：敏感数据、认证域、媒体、审计与脱敏）

## PA29 Current Runtime Boundary (V017)

This current boundary supersedes PA28 V016 and all older current capability/history/inventory statements below; their evidence remains historical. PA29 adds only `GET /api/v1/customer/evidence` / `listCustomerEvidence` and `GET /api/v1/customer/evidence/{evidenceId}` / `getCustomerEvidence`. OpenAPI and V001–V016 remain byte-frozen. Existing customer endpoints retain their prior contracts.

Exact safe shapes: page has only `items`, `nextCursor`, `hasMore`; summary has only `evidenceId`, `subjectId`, `status`, `sourceType`, `capturedAt`, `consentVersion`, `createdAt`, `artifactRoles` (always present, including `[]`). Detail has only `evidenceId`, `subjectId`, `status`, `sourceType`, `capturedAt`, `consentVersion`, optional `solidifiedAt`, `artifacts`; **detail has no `createdAt`**. Each artifact requires `artifactRole`; optional `mimeType`, `sizeBytes`, `validatedDurationSeconds`, `sha256` are omitted when absent, not fabricated. `capturedAt` is the validated immutable `identity_at_capture.capturedAt` scalar, never live identity or snapshot fallback. All four statuses PENDING, SOLIDIFIED, QUARANTINED, FAILED are visible; detail re-reads current facts. Artifacts order ORIGINAL then DERIVED, then hash/size/MIME; duration is authoritative only for ORIGINAL with a unique validated source/media match, never DERIVED.

Only the verified session supplies account/workspace authority. Personal scope matches subject `owner_account_id`; selected organization scope matches `owner_organization_id`, requires ACTIVE account/organization/membership and accepts ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER, AUDITOR. Inactive lifecycle is FORBIDDEN 403. Missing/unowned/foreign-workspace/other-subject detail is uniform NOT_FOUND 404. Any OPEN risk on any binding, regardless of binding status, of **any accessible subject having evidence** denies the **whole list with 403 before pagination**, including off-page subjects; owned subject without evidence does not deny the list. Detail checks requested subject risk after ownership. Malformed UUID/limit returns 422; malformed/tampered/context-replayed cursor returns 400; unsupported methods/aliases/extra route segments return 404. Authentication precedes input parsing; ignored query/header/body selectors never grant authority. Unrepresentable persisted detail facts fail closed as 404, list facts as sanitized 500; dependency/completion failure is sanitized 503.

Stable keyset order is `createdAt DESC, evidenceId DESC` using PostgreSQL UUID order, fetch limit+1, next anchor from the last returned item; tail has `nextCursor: null`. Default limit 20, inclusive 1–100. HMAC-SHA256 cursor binds version, fixed evidence endpoint, verified account, workspace type/organization, fixed sort and anchor; limit is deliberately not cursor-bound; no offset/count or cross-request snapshot promise. Explicit independent `onecall.customer.evidence.cursor-hmac-key` must be canonical base64 of exactly 32 bytes; there is no production default. Key/service resolution stays lazy after structural input validation; detail never resolves the cursor key.

Customer composes only named `customer-subject-query` and `customer-evidence-query` owner interfaces. Lifecycle, ownership, all-binding risk and batched safe facts share one customer-role **read-only REQUIRES_NEW / REPEATABLE_READ** transaction/connection; no N+1 or business/event/audit/outbox writes. Pool state and suspended transactions restore on success/failure. Authorization is application predicates plus exact ACLs, not RLS or linearizable revocation: later committed changes govern the next request.

V017 is privilege-only: **19** owner-issued column SELECT tuples, no grant options: `evidence_record(id,status,source_type,source_ref_id,identity_at_capture,consent_version,created_at,solidified_at)` (8), `evidence_artifact(evidence_id,artifact_role,media_object_id)` (3), `media_object(id,mime_type,size_bytes,sha256)` (4), `recording(id,status,media_object_id,validated_duration)` (4). All grantors are `evidence_owner`, grantee `customer_iam_role`. Arithmetic: **269 + 19 = 288 = customer53 + evidence16 + others219**; customer53 = 6 schema USAGE + 47 column SELECT, target pair50→69. No new table SELECT, writes, routines, sequences, schema CREATE, roles, memberships or pools. The JSONB column grant makes full capture JSON readable at the raw-role boundary: scalar-only SQL/output/logging is a trusted-application Security/Database residual, **not JSON-path security**.

Apply/Flyway-validate V001→V017 before current PA25 apply/check: exact **17** successful SQL history rows, V016-only rejected. V017 raw SHA256 `57f1cac30dabdaee3e08fd43baaf6e696b453473ee8753f4c5da44cb0ecafbad`; Flyway checksum **-41822162**. Replay accepts only exact V016 pre-state or complete V017 final state; partial/widened/wrong-grantor/grant-option/history drift rejects atomically without PA25 repair. Six logins/seven effective roles/four membership edges and credential/provenance/owner/PUBLIC/default-ACL invariants remain unchanged.

Required source=CI=POM=XML inventory is **49 classes / 417 Surefire + 21 / 336 Failsafe = 70 / 753**, 76 additions (42/34), annotation lines **754** with one historical comment, **21** exact Failsafe includes. Execution outcomes and any NOT EXECUTED gates belong to `docs/gateflow/pa29-customer-evidence-read-foundation-s1-implementation.md`, not an inferred pass from these requirements.

No media bytes/playback/download/URLs/storage/crypto references, share credentials/operations, live identity, snapshot/event payloads, lifecycle mutation or session issuance is added. External production credentials, TLS/pg_hba, migrator custody, deployment/rotation/pool drain/session invalidation and Q17/Q23 remain Platform/Database/Security/Operations work: **NOT EXECUTED; production/live readiness NOT CLAIMED**.

## PA28 Historical Runtime Boundary (V016; superseded by PA29 V017)

This boundary supersedes all older current-runtime capability, history and inventory claims below; historical model/catalog evidence remains unchanged. Only `GET /api/v1/customer/subjects/{subjectId}` / `getCustomerSubject` is added: success contains exactly `subjectId`, `activationStatus`, `evidenceCount`, `orderCount`. OpenAPI and V001–V015 remain byte-frozen. Optional `numberMasked`, `trial`, `consentRequirements` are absent, including nulls; `claimState` is not a schema field. Larger operation prose does not authorize those future projections.

Authority comes only from the verified session's account and selected workspace. ACTIVE account is required. Personal access requires matching `owner_account_id`; organization access requires matching `owner_organization_id`, ACTIVE organization/membership and ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER or AUDITOR. Missing/unowned/foreign-workspace subjects return the same NOT_FOUND after lifecycle checks; inactive contexts are FORBIDDEN. After ownership, any OPEN risk on any binding of the subject is FORBIDDEN, regardless of binding status; absent risk is not OPEN. No client owner/workspace/role selector is authoritative. Extra query/body inputs are ignored; malformed full UUID values return 422, extra path segments/unsupported methods 404. Null owner/service results are INTERNAL, not not-found.

Activation uses the existing owner projection, defaulting to INACTIVE without insertion. Evidence counts every `evidence_record` for the subject, all statuses. Orders count all statuses for that subject: personal requires purchaser account equality AND NULL organization; organization requires selected organization equality, regardless of purchaser. Counts disclose no row content/media. Lifecycle, ownership, all-binding risk, activation and counts execute on one customer-role read-only REQUIRES_NEW / REPEATABLE_READ snapshot; commit/completion failures return sanitized 503, including the empty/not-found path. This is not RLS or linearizable revocation and adds no business writes.

Owner seams remain in subject-number (`customer-subject-query`), evidence and catalog/order (`customer-subject-count-query`); customer orchestrates without owner-internal SQL. Existing activation seam is reused. V016 is privilege-only and adds exactly **8 = 2 schema USAGE + 6 column SELECT** owner-issued/no-grant-option tuples: USAGE on evidence/catalog_order; SELECT `subject_number.subject_risk_state(caller_number_binding_id,state)`, `evidence.evidence_record.subject_id`, and `catalog_order.customer_order(subject_id,account_id,owner_organization_id)`. Customer becomes **34 = 6 schema + 28 column**; evidence remains **16**, other roles **219**, independent seven-effective-role PA25 oracle **269**. No table SELECT, ciphertext/HMAC, claim/consent/trial reads, routines/sequences, writes, schema CREATE, grant options, new roles or pools. Predicate-column raw reads remain sensitive within the trusted-application boundary.

Apply and Flyway-validate V001 → V016 before current PA25 apply/check. Exactly sixteen successful SQL rows with exact rank/version/script/description/checksum are required; V014/V015-only is rejected. V016 `V016__customer_subject_detail_read_privileges.sql` has Flyway checksum **1669606497**. Replay accepts only exact clean V015 pre-state or complete V016 final state; partial/widened/wrong-grantor/grant-option states fail without repair. Six logins / seven effective roles / four edges remain LQ→sharing, LC→customer, LW→command and evidence, with ADMIN FALSE / INHERIT FALSE / SET TRUE; direct login boundaries, OIDs, credential verifiers, provenance, owners, PUBLIC/default ACLs and unrelated privileges are preserved.

Required inventory: **44 classes / 375 Surefire + 19 / 302 Failsafe = 63 / 677**, annotation-line scan **678** (one historical comment), nineteen exact Failsafe includes; 64 new named tests preserve all 613 baseline identities. Prefix-pinned PostgresContainerIT/PA26/PA27 security checks remain historical; only explicit latest upgrades use V016/current PA25. Execution evidence, not these requirements, is recorded in `docs/gateflow/pa28-customer-subject-detail-read-foundation-s1-implementation.md`.

External production connection, credential assignment, TLS/pg_hba, migrator/comment custody, deployment/rotation/pool-drain/session-invalidation acceptance and Q17/Q23 remain Platform/Database/Security/Operations residuals: **NOT EXECUTED; production readiness NOT CLAIMED**.

## PA27 historical (V015; superseded by PA28 V016)

Historical-only notice: the following PA27 statements, numbers and commands describe V015, not the current V016 runtime. Current PA25 requires V001→V016 as specified above.

This section supersedes the following PA26 V014 section, its capability rows, history prerequisites and inventory claims, and all older current customer-capability claims. It does not change the structural/business/security model or unrelated roles. V001–V014 and OpenAPI remain byte-frozen. Apply and validate V001 → V015 before current PA25 apply/check: exactly fifteen successful SQL history entries with independent exact script/description/checksum/rank validation are required; V014-only is rejected. V015 is privilege-only, transactional and replayable only on the exact clean pre-state or complete final state; partial, widened, wrong-grantor or grant-option states fail without repair.

Customer has **26 = 4 schema USAGE + 22 column SELECT** explicit owner-issued/no-grant-option tuples. Retained V014 reads are `customer_iam.customer_account(id,display_name,status)`, `organization_access.organization(id,display_name,status)` and `organization_access.organization_membership(organization_id,account_id,role,status)` plus USAGE on those two schemas. Exactly **14 = 2 schema + 12 column** additions are:

| Owner and grantor | Exact addition to `customer_iam_role` |
| --- | --- |
| `subject_number_owner` | USAGE on `subject_number`; SELECT `(id,owner_account_id,owner_organization_id,real_name_status,created_at)` on `subject_number.business_subject`; SELECT `(id,subject_id,number_masked,number_type,status)` on `subject_number.caller_number_binding` |
| `activation_consent_owner` | USAGE on `activation_consent`; SELECT `(subject_id,status)` on `activation_consent.service_activation` |

Evidence remains **16** tuples; the independent seven-effective-role PA25 oracle is **261**, with the other-role **219** unchanged. No table SELECT, number ciphertext/HMAC, claim/risk/consent reads, business writes, routines, sequences, schema CREATE, grant option or privilege outside this delta is added. Existing V014 restrictions remain. Six logins / seven effective roles / four edges remain unchanged: LQ → sharing; LC → customer; LW → command and evidence; three direct logins retain their boundaries. Edge options remain ADMIN FALSE / INHERIT FALSE / SET TRUE; provenance, OIDs, credentials, owners, PUBLIC/default ACLs and unrelated grants are preserved, never repaired or logged.

Only `GET /api/v1/customer/business-numbers` / `listCustomerBusinessNumbers` is added. Verified session account and selected workspace alone select authority. Current ACTIVE account is required; organizations additionally require ACTIVE organization/membership with any of ADMINISTRATOR, MEMBER, EVIDENCE_MANAGER, AUDITOR. Inactive/missing context returns generic 403; active empty scope returns 200 with `items: []`, `nextCursor: null`, `hasMore: false`. Every owned binding is visible for ACTIVE/DISABLED/RETIRED and both number types/real-name states. All INACTIVE/ACTIVE/SUSPENDED activations remain visible; absent activation alone defaults INACTIVE. Seven item fields and three page fields are the frozen masked projection; timestamps and owner IDs are not serialized.

One LC/customer-pool read-only REQUIRES_NEW REPEATABLE_READ transaction sets/asserts `customer_iam_role`, checks lifecycle and runs the owned subject-number query plus one bounded activation batch on the same snapshot. Cleanup restores pooled state. This is not RLS or linearizable revocation: application predicates enforce tenant isolation; a later committed revocation applies on the next request. Order is subject.createdAt descending / native binding UUID ascending; limit defaults 20, range 1–100, fetch limit+1, cursor from last returned row, null on tail; no offset/count or cross-page snapshot promise.

Security/Operations must externally supply `ONECALL_CUSTOMER_BUSINESS_NUMBER_CURSOR_HMAC_KEY`, canonical padded standard base64 of exactly 32 random bytes. The endpoint-only HMAC-SHA256 cursor is canonical 93 bytes / 124 base64url characters, binds account/workspace/order/anchor and has no TTL. The key is mandatory even for first/empty pages; absent/invalid key produces sanitized 503 without eagerly activating unrelated endpoints. All replicas require one consistent key; replacement invalidates prior cursors with CURSOR_INVALID. No live secret is assigned here. Error precedence is route 404 → authentication 401/503 → workspace 503 → limit 422 → structural cursor 400 → key 503 → MAC/scope 400 → lifecycle 403; role failures map 503 and projection failures 500.

Current exact acceptance inventory is **40 Surefire classes / 341 methods + 17 Failsafe classes / 272 methods = 57 / 613**, annotation-line scan **614** (one retained historical comment), **17** explicit Failsafe includes. Existing methods are preserved. `PostgresContainerIT` remains the V001–V013 prefix (64 methods), observing V014/V015 ABOVE_TARGET; twelve PA26 methods remain pinned to V014; latest-chain suites target V015. These are source/CI requirements, not a passing XML or production claim. Actual commands, failures and unavailable validation are recorded in the PA27 S1 implementation report.

Backend/Architecture owns deferred qualified-DSL inversion review; Backend/Database owns representative sort/index EXPLAIN; Security/Database owns the non-RLS limitation. Platform/Database/Security retain Q17/Q23, migration/comment custody, credential assignment, TLS/pg_hba and exclusive administration; Security/Operations retain key distribution/rotation, old-session invalidation and pool drain. Production connection, deployment, credential assignment and live acceptance: **NOT EXECUTED**. Production readiness: **NOT CLAIMED**.

## PA26 Historical Runtime Boundary (V014; superseded by PA27 V015)

Historical-only notice: every capability row, history prerequisite, supersession ledger and inventory claim in this PA26 section describes V014-era evidence only. The preceding PA27 section is the current authority, including fifteen-row history and customer26/global261; no older “current” wording in this section overrides it.

This section supersedes the two target roles' V001–V013 capability claims, not the frozen structural/business model or any other role. Apply/validate V001 → V014 before PA25 apply/check; a V013-only database is rejected. V014 is append-only and privilege-only: no production Java/configuration, role graph, ownership, business object, trigger/function definition, PUBLIC or default-ACL change. No future customer/profile mutation or evidence uploader capability is reserved.

All grants below are explicit owner-issued grants without grant option. There are no target-role table-level grants.

| Role | Schema USAGE only | Exact column SELECT only |
| --- | --- | --- |
| `customer_iam_role` | `customer_iam`, `organization_access` | `customer_iam.customer_account(id,display_name,status)`; `organization_access.organization(id,display_name,status)`; `organization_access.organization_membership(organization_id,account_id,role,status)` |
| `evidence_role` | `evidence` | `evidence.evidence_record(id,subject_id,status,source_type,source_ref_id,identity_at_capture)`; `evidence.evidence_artifact(evidence_id,media_object_id,artifact_role)`; `evidence.media_object(id,sha256,size_bytes,mime_type)`; `evidence.recording(media_object_id,validated_duration)` |

The grantors are `customer_iam_owner`, `organization_access_owner` and `evidence_owner` for their respective objects. Customer has 12 explicit tuples (2 schema + 10 column), replacing 39; evidence has 16 (1 + 15), replacing 42. PA25's independent seven-role oracle is exactly 247 tuples: the other 219 are unchanged. Neither target can read any `auth_identity`, `identity_profile` or `evidence_event` column; evidence cannot read storage locators/keys or remaining recording/record payloads. No INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER, sequence privilege, schema CREATE, application routine EXECUTE or grant option remains. FOR UPDATE/FOR SHARE are denied. PostgreSQL built-ins are not revoked.

The historical customer `platform`/`audit` USAGE and four append/idempotency routines, three IAM guard EXECUTEs, all three IAM table SELECT/INSERT and fifteen IAM UPDATE-column tuples are superseded by the customer row above. Its seven organization projection columns remain. Evidence's five old table SELECT/INSERT pairs, nine UPDATE columns, five guard EXECUTEs, two append routines, former subject/IVR/entitlement column reads and `subject_number`/`ivr`/`entitlement_metering`/`platform`/`audit` USAGE are removed. Owner fixtures retain ordinary invoker guards and force deferred FK/cycle checks before leaving their owner role; guard definitions are not upgraded to SECURITY DEFINER.

Six logins / seven effective roles / four edges remain: LQ → `sharing_role`; LC → `customer_iam_role`; LW → `sharing_command_role` and separately `evidence_role`; three direct login boundaries remain unchanged. Each edge is ADMIN FALSE / INHERIT FALSE / SET TRUE. SET ROLE authority follows `session_user`: LW command ↔ evidence restoration is allowed; LC cannot enter evidence/command. No additional membership or production Java switch is introduced. Exact PA25 v1 database-OID/role-OID/slot/name provenance is retained; non-NULL externally assigned verifiers are allowed and preserved byte-identically, never printed or cleared. No unmarked-name adoption or broad ACL repair exists.

The current required inventory is 36 classes / 309 Surefire methods + 15 / 246 Failsafe = 51 / 555; annotation scan 556 (one historical comment), fifteen exact Failsafe includes. `PostgresContainerIT` deliberately proves only the V001–V013 historical prefix (64 methods); the twelve-method PA26 suite and every other latest-chain application/provisioning suite prove V014. Counts are requirements, not a substitute for zero-skip XML evidence.

Projection access remains cross-row sensitive, especially `identity_at_capture`; no RLS/tenant boundary or content declassification is claimed. Platform/Database/Security retain Q17/Q23, migrator/comment custody, external login selection, pg_hba/TLS and exclusive administration; Security/Operations retain rotation, old-session invalidation and pool drain. Live production connection, credential assignment, deployment/rotation acceptance: **NOT EXECUTED**. Production readiness: **NOT CLAIMED**.


> Work unit：`A3-production-security-design-spec`
>
> Slice：S1 — 敏感数据保护、密钥层级、轮换/恢复与历史 replay 兼容；S2 — 认证域、服务身份、凭据、签名与 replay 拒绝；S3 — 媒体凭证、COS 私有策略、审计访问、高风险控制与确定性脱敏
>
> Task label：`a3-security-s3-impl-ds-20261026-01`
>
> Base：accepted S2 checkpoint `b20be2edfa576e4e27bdf94e067335df25e9cf88`
>
> Machine model：`security/v1/security-design-v1.json`
>
> Validator：`security/validate.mjs`（零第三方依赖，Node 22）
>
> Status：S1/S2/S3 slice checkpoint 均已 accepted；live aggregate/PR Gateflow 状态仅以 `docs/gateflow/project-execution-control.md` 为权威。

## 1. 状态、来源关系与静态限制

本文件是 A3 安全语义的人类 canonical 规范；`security/v1/security-design-v1.json` 是同一语义的机器投影，二者由 `security/validate.mjs` 做双向一致性校验。S1 冻结数据与密钥真相：哪些数据由哪个 purpose/key 保护、如何 envelope/HMAC、如何轮换/恢复、如何保持历史解密/查找与 A2 replay 兼容。S2 在不改变 S1 语义的前提下冻结五个信任面/认证域、七个 trust boundary、六个内部服务身份、十个凭据类、两个签名 profile、audience/nonce 策略，以及 IVR/provider 的 mTLS + 签名 + timestamp + nonce/replay 确定性验证和失败零副作用语义。S3 在不改变 S1/S2 的前提下冻结媒体 upload/playback 凭证、COS 私有对象策略、39 个审计/访问事件及其 persistence route、10 个高风险操作、10 个调用类覆盖、确定性脱敏与 fail-closed 矩阵，并累计 21 个 invariant。

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

S2 追加冻结（不改变 S1 语义）：

- 恰好 5 个信任面/认证域，精确等于 A1 的 `customer`、`employee`、`ivr-service`、`public-capability`、`provider` 及 operation 计数 18/21/6/3/1；
- 恰好 7 个 trust boundary `TB1` 至 `TB7`；
- 恰好 6 个内部服务身份及其 allowed calls、CAM/DB 角色、secret custody、audience、fail-closed 与轮换边界；前端不持有 service secret；
- 恰好 10 个凭据类 `C01` 至 `C10` 与 2 个签名 profile；
- audience registry、session/credential/mTLS/signature key 生命周期和 nonce 策略，所有数值 TTL/window/backoff 仍为 typed deferred；
- IVR 与 provider 的 mTLS + 签名 + timestamp + nonce/replay 验证顺序、错误映射与零 A2 副作用；
- IVR-local outbox 在观测 401/403 时的同一 `PENDING` entry/security-blocked 策略与显式恢复重试；
- 累计 invariant 恰好 11 个（新增 `INV-A3-07` 至 `INV-A3-11`）；
- 恰好 9 个 S2 fixture，累计 15 个（6+9），其中 7 个显式负例。

S3 追加冻结（不改变 S1/S2 语义）：

- 恰好 2 个媒体策略：U1 short-lived single-object Tencent COS presigned PUT（CV-4 门禁，不可验证则 fail closed 且停止，不 fallback）与 P1 opaque media-gateway bearer（`Authorization: Bearer`，无 COS key/bucket/version/permanent URL）；
- COS 私有策略：私有 bucket、禁匿名、TLS-only、SSE-KMS、versioning、不改写原始对象、不声称 WORM/object-lock/region/SKU/private endpoint；
- 恰好 39 个审计/访问事件，每行带 `persistence`，精确为 2 个 `access_event`（`AUDIT_READ`/`AUDIT_READ_DENIED`，不递归调用 `audit.append_fact`）与 37 个 `append_fact`；
- 恰好 10 个高风险员工 mutation，固定 ADMIN/OPS、role/state/reason/audit 同事务、audit 失败回滚；UI 二次确认标记 runtime-unverified，不作为 validator-assertable 成功项；
- 恰好 10 个调用类覆盖行；
- emit 前确定性脱敏：结构化 allow-list → 任意深度递归 deny-list → credential/header/URL pattern → destination policy；失败 block/drop 并告警，绝不先 emit 后清洗；support view 无 playback；
- fail-closed 矩阵与 `INV-A3-12` 至 `INV-A3-21`（累计 21）；
- 恰好 8 个 S3 fixture，累计 23（`6+9+8`），其中累计 10 个显式负例。

`--stage s1` 和 `--stage s2` 在同一累计文件上只验证各自 projection 的 registry、marker 与 fixture，忽略后续 slice；`--stage s3` 要求全部 S3 section/marker/invariant/fixture 存在且精确，任何缺失、额外、畸形、未知或篡改内容都失败。

## 3. 上游权威与禁改边界

| 上游 | 只读权威 | A3 使用方式 |
| --- | --- | --- |
| `docs/14` | 44 表 catalog、28 行敏感字段目录、envelope/HMAC 形状、audit 契约、28 个 open input | 逐行映射 data class、复刻 envelope/HMAC 形状、复制 18 个 Q row 的 input/owner/destination |
| A1 bundle | 49 operation、6 security scheme、五个 `x-onecall-audience`、credential 字段和 IVR/provider header | S1/S2 只做 drift 与形状断言；五域/operation 数/audience/header 必须等于 A1；不新增 operation/header/schema/code |
| A2 model | 12 messages、11 eventTypes、22 identities、15 invariants、event registry digest、outbox 四态 | 只做 drift 断言并保持 replay/exactly-once 语义不变；S2 只新增 security-layer 的 401/403 本地策略 |

S1/S2 不修改 `docs/12` 至 `docs/16`、A0/A1/A2 accepted 产物、OpenAPI source、prototype、screenshots、`shared/onecall-business.js`、controller 或历史 review。A2 event registry digest 保持 `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9` 不变。

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

## 10. 信任面、认证域与 trust boundary（5 + 7）

五个认证域精确来自 A1 `x-onecall-audience`，不新增、不合并；每个域有独立的 token/credential/verifier/失败映射。`customerSession` 与 `employeeSession` 是不透明 bearer，服务端解析 audience、账户/员工和 workspace/role；不得使用客户端可解码且无 audience 的 JWT。五个域的 signing/verification key、session store、audience 和失败映射互不复用。

| trust surface | domain | prefix | operations | boundary |
| --- | --- | --- | ---: | --- |
| TS1 | customer | /api/v1/customer/ | 18 | TB1, TB2 |
| TS2 | employee | /api/v1/operations/ | 21 | TB1, TB2 |
| TS3 | ivr-service | /api/v1/ivr/ | 6 | TB3 |
| TS4 | public-capability | /api/v1/public/shares/ | 3 | TB1, TB2 |
| TS5 | provider | /api/v1/callbacks/ | 1 | TB4 |

<!-- security-domain-map:start -->
| domain | prefix | operations | credentialClasses | storage | tokenAudience | crossDomainRule |
| --- | --- | --- | --- | --- | --- | --- |
| customer | /api/v1/customer/ | 18 | C01 customerSession | Redis session record + account state | onecall:customer-session:v1 | reject any other domain token; gateway trust never bypasses the audience check |
| employee | /api/v1/operations/ | 21 | C02 employeeSession | Redis session record + employee state | onecall:employee-session:v1 | independent issuer; fixed ADMIN/OPS; never reused with the customer domain |
| ivr-service | /api/v1/ivr/ | 6 | C03 ivrMtls, C04 ivrSignature | mTLS certificate store + Secret Manager HMAC key | onecall:ivr-request:v1 | accept mTLS plus signature only; never a session or capability token |
| public-capability | /api/v1/public/shares/ | 3 | C07 publicShareCapabilityToken, C08 extractionCode, C01 customerSession (optional ORGANIZATION_MEMBER only), C10 playbackCredential | DB SHA-256 digest + admission state + Redis ticket Q22 | onecall:share-capability:v1; onecall:media-playback:v1 | capability token cannot be used on another surface; customer session cannot replace capability; C10 is presented to media-gateway only |
| provider | /api/v1/callbacks/ | 1 | C05 providerCallbackMtls, C06 providerCallbackSignature | mTLS certificate store + Secret Manager vendor verification material | onecall:provider-callback:v1 | accept provider mTLS plus vendor signature only; never disclose an internal result |
<!-- security-domain-map:end -->

七个固定 trust boundary：

| ID | 边界 | 允许 | 禁止 |
| --- | --- | --- | --- |
| TB1 | Internet 与 edge → customer/public-share/operations 前端 | TLS/WAF；customer/employee/capability 域 | 直接访问 DB/Redis/MQ/COS |
| TB2 | Edge → core-api 的 customer/operations/public-share API | 匹配的 session/capability credential 与 path/audience 校验 | 跨域 token、永久对象 URL |
| TB3 | 运营商机房/IVR 网络 → core-api IVR API | mTLS + IVR 签名 + timestamp + nonce | DB/MQ 直连、无签名请求 |
| TB4 | Internet/provider → callback API | provider mTLS + 厂商签名 + timestamp + nonce | 内部结果披露、订单金额/支付成功声明 |
| TB5 | core-api/worker → PostgreSQL/Redis/RabbitMQ | NOLOGIN DB role、Outbox、最小权限 | 跨模块写、客户端直连、绕过 Outbox |
| TB6 | core-api/worker/ivr-gateway → Tencent KMS/Secret Manager/COS | CAM service role、短时 upload credential、私有 bucket | 静态 AccessKey、公开对象、永久 URL |
| TB7 | 应用 → logs/traces/metrics/analytics/support/public export | redaction pipeline、masked/ID/status、audit access | plaintext/secret/credential URL/HMAC/ciphertext |

mTLS 只证明 service/provider 身份；IVR 仍必须每请求签名并校验 timestamp/nonce，provider 仍必须验厂商签名。运营读取只授予脱敏业务元数据；A1 没有 operations playback endpoint，A3 不新增。fail-closed 只映射 A1 既有 `UNAUTHENTICATED`、`FORBIDDEN`、`SERVICE_UNAVAILABLE`、`CALLBACK_SIGNATURE_INVALID`、`PAYMENT_EVENT_CONFLICT`、`CALLBACK_RETRY_LATER` 等 code，不新增 security-specific code。

## 11. 服务身份与 token audience（6 + audience registry）

六个内部服务身份映射既有 deployment unit，不新增拓扑；IVR/provider 外部身份不进入内部 service identity registry。每个 CAM role 只授予该服务必需的 action/resource；禁止 `*` action/resource、跨环境 role 复用和长期静态 AccessKey。前端 `customer-web`、`public-share-web`、`operations-web` 和小程序不持有 service credential、KMS/Secret Manager 访问权或永久 COS 凭证。

<!-- security-service-map:start -->
| serviceIdentity | mechanism | audience | allowedCalls | secretCustody |
| --- | --- | --- | --- | --- |
| core-api | Tencent CAM service role + DB NOLOGIN role | onecall:service:core:v1 | DB, Redis, Outbox, KMS, Secret Manager, COS evidence metadata | Secret Manager or instance role; no static key |
| async-worker | Tencent CAM service role + DB role | onecall:service:worker:v1 | DB, MQ consumer, KMS, Secret Manager, COS, notary adapter, AI adapter | Secret Manager or instance role |
| scheduler | Tencent CAM service role + DB role | onecall:service:scheduler:v1 | DB lease, expiry scan | minimal least privilege; no external provider key |
| media-ai-worker | Tencent CAM service role | onecall:service:media-ai:v1 | MQ, COS read, COS write, AI provider Secret Manager | Secret Manager; no business DB write |
| ivr-gateway | mTLS client + Secret Manager | onecall:ivr-request:v1 | A1 IVR API, controlled COS upload | C03 mTLS credential + C04 signing key |
| media-gateway | mTLS or internal audience + Secret Manager | onecall:media-playback:v1 | C10 validation, COS media read | C10 signing key; never issues upload credential |
<!-- security-service-map:end -->

`media-gateway` 是逻辑校验点，部署在既有 core-api/edge 内，不是新部署单元。每个 service identity 的 CAM role/证书/密钥按 Q17/Q23/CV-3 轮换；轮换期间保留旧身份 verify-only/overlap，重叠后 disable；不得复用其他服务的 role。

Audience registry（5 个领域 audience + 1 个 upload 签发侧分类 + 4 个内部服务 audience）：

| audience | owner | credential/identity | verifier |
| --- | --- | --- | --- |
| onecall:customer-session:v1 | customer | C01 customerSession | core-api |
| onecall:employee-session:v1 | employee | C02 employeeSession | core-api |
| onecall:ivr-request:v1 | ivr-service | C03 ivrMtls, C04 ivrSignature, ivr-gateway | core-api |
| onecall:share-capability:v1 | public-capability | C07 publicShareCapabilityToken, C08 extractionCode | core-api |
| onecall:media-playback:v1 | public-capability | C10 playbackCredential, media-gateway | media-gateway |
| onecall:provider-callback:v1 | provider | C05 providerCallbackMtls, C06 providerCallbackSignature | core-api |
| onecall:evidence-upload:v1 | ivr-service | C09 uploadCredential (issuance-side policy classification only) | issuance policy |
| onecall:service:core:v1 | internal | core-api | Tencent CAM service role |
| onecall:service:worker:v1 | internal | async-worker | Tencent CAM service role |
| onecall:service:scheduler:v1 | internal | scheduler | Tencent CAM service role |
| onecall:service:media-ai:v1 | internal | media-ai-worker | Tencent CAM service role |

一个域的有效 token 在另一个域必须按 audience/verifier 拒绝；不能因 gateway 信任、network position 或 IP allow-list 而放行。

## 12. 凭据类与生命周期（10）

<!-- security-credential-map:start -->
| credentialClass | domain | audience | storage | ttlInput | scope | rotationRevocation | redaction |
| --- | --- | --- | --- | --- | --- | --- | --- |
| C01 customerSession | customer | onecall:customer-session:v1 | Redis session record + account state | Q1/Q19 | customer API only | logout, account disable or expiry revokes the session | bearer and Authorization header always removed |
| C02 employeeSession | employee | onecall:employee-session:v1 | Redis session record + employee state | Q1 | operations API only | role change, disable or credential reset revokes the session | bearer and Authorization header always removed |
| C03 ivrMtls | ivr-service | onecall:ivr-request:v1 | mTLS certificate store | Q7 | IVR mTLS identity only | certificate overlap and rotation per Q7 | certificate and private key never logged |
| C04 ivrSignature | ivr-service | onecall:ivr-request:v1 | Secret Manager K17 | Q25 window | per-request signature | keyId overlap; old key VERIFY_ONLY until retirement | signature and nonce always removed |
| C05 providerCallbackMtls | provider | onecall:provider-callback:v1 | mTLS certificate store | Q6 | provider callback only | provider certificate rotation per Q6 | certificate never logged |
| C06 providerCallbackSignature | provider | onecall:provider-callback:v1 | Secret Manager or certificate store K18 | Q6 | per-callback signature | provider rotation per Q6 | signature and nonce always removed |
| C07 publicShareCapabilityToken | public-capability | onecall:share-capability:v1 | DB SHA-256 digest only | share expiresAt / Q12 | path capability; created once | revoke or generation change; not rebuildable | capability token always removed and return-once |
| C08 extractionCode | public-capability | onecall:share-capability:v1 | per-grant salt + slow KDF digest | Q12 | EXTRACTION_CODE | code_generation rotation | code and OTP always removed and return-once |
| C09 uploadCredential | ivr-service | onecall:evidence-upload:v1 (issuance-side policy classification only; not a COS embedded audience claim) | COS presign CV-4 verified or K19 | uploadExpiresAt | single-object COS presigned PUT only; fail closed and stop if CV-4 capability is unverified | no renewal or re-sign; expiry only | credential URL always removed |
| C10 playbackCredential | public-capability | onecall:media-playback:v1 | media gateway K20 | Q22 | Authorization Bearer to media-gateway; share, admission, generation, object and PLAYBACK/SEEK/RENEW | issued credential is not recalled; expiry only | credential URL and bearer value always removed |
<!-- security-credential-map:end -->

凭据生命周期固定为可观测的状态迁移：session `UNAUTHENTICATED → AUTHENTICATED → REVOKED/EXPIRED`；mTLS/signature key `ACTIVE → VERIFY_ONLY → DISABLED`；nonce `ABSENT → SEEN(TTL)`。这些是安全层状态，不是 A2 业务状态，不新增 A0 列/状态或 A2 event。所有 TTL/window/backoff 数值仍为 Q1/Q6/Q7/Q12/Q19/Q22/Q25 等 typed deferred；本文件不给出具体秒数或阈值。字段边界沿用 A1：`capabilityToken` 1-512，`extractionCode` 1-128，`upload.credential`/`mediaAuthorization.credential`/`MediaAuthorization.credential` 1-2048。

## 13. 签名 profile、nonce/replay 与 A2 保持（2）

### 13.1 IVR request profile（`ivr-request-v1`）

适用 `/api/v1/ivr/**` 的 6 个 operation，必须同时具备 `ivrMtls` 与 `ivrSignature`。

- Header 名称为 A1 已冻结的 `X-ONECALL-SIGNATURE`、`X-ONECALL-TIMESTAMP`、`X-ONECALL-NONCE`。A1 冻结 TIMESTAMP 长度 1-64、NONCE 长度 1-128；A1 **没有**冻结 SIGNATURE header 长度，A3 只冻结其值格式，不引入长度断言。
- 签名值：`v1.<keyId>.<base64url(HMAC-SHA-256(K17, canonicalString))>`；`keyId` 是 Secret Manager version label，匹配 `[A-Za-z0-9_-]{1,64}`；比较必须 constant-time。
- Timestamp：ASCII Unix epoch seconds，匹配 `^[0-9]{1,12}$`；接受窗口是 `±W`，`W` 是 Q25 deferred；以 backend receive time 为准，不使用客户端 `occurredAt`。
- Nonce：匹配 `[A-Za-z0-9_-]{1,128}`；在 `(serviceIdentity, nonce)` 维度唯一；Redis `SET NX`；TTL 为 `W + skew`，数值 Q25 deferred。
- Canonical string 固定为六行 UTF-8，行间 `\n`，末尾无额外换行：
  1. 字面量 `ONECALL-IVR-V1`
  2. uppercase HTTP method
  3. raw request path（不含 query，不 decode、不归一化）
  4. `X-ONECALL-TIMESTAMP`
  5. `X-ONECALL-NONCE`
  6. exact request body bytes 的 lowercase hex SHA-256（空 body 为空字节哈希）
- 验证顺序固定为：mTLS client identity → 必需 header/长度 → timestamp 解析/窗口 → signature verify（keyId active/verify-only）→ nonce `SET NX` → A1/A2 processing。
- 失败映射：缺/坏 mTLS、坏签名、超窗口、nonce replay → `401 UNAUTHENTICATED`；service identity 不允许 → `403 FORBIDDEN`；nonce/key/Secret Manager 不可用 → `503 SERVICE_UNAVAILABLE`。
- 零副作用：认证失败发生在 session/call/decision/task/reservation/event 写入之前；不得返回 200 `IvrDecision`、不得领取任务、不得预占/结算、不得写 outbox 业务事实。
- A2 保持：同一 `callId`/`eventId`/payload 的重试使用新的 timestamp/nonce；A2 idempotency 返回首次结果；不重拨、不新建 `callId`、不重复预占/结算。IVR 不得因 401/403 重新 resolve 获取新授权。
- 轮换：新/旧 keyId 重叠验签；旧 key 只 `VERIFY_ONLY`；重叠结束旧 key 拒绝。未知 keyId 直接拒绝，不 fallback 到默认 key。

### 13.2 Provider callback profile（`provider-callback-v1`）

适用 `POST /api/v1/callbacks/payment-events`，必须同时具备 `providerCallbackMtls` 与 `providerCallbackSignature`。

- Header：`X-ONECALL-CALLBACK-SIGNATURE`、`X-ONECALL-TIMESTAMP`、`X-ONECALL-NONCE`；TIMESTAMP 1-64、NONCE 1-128 沿用 A1。vendor signature 算法、canonicalization、payload 字段与窗口全部 Q6 typed deferred；本文件不发明。
- 验证顺序：provider mTLS identity → header → provider profile（Q6）→ timestamp window → constant-time signature verify → exact duplicate event/payload fingerprint 检查（命中即 200 duplicate 早退，不消费 nonce）→ nonce `SET NX`（新事件或 changed payload）→ changed payload 冲突检查（仅在 nonce 检查成功后返回 409 `PAYMENT_EVENT_CONFLICT`）→ 支付处理。
- exact duplicate 是零副作用的幂等结果，不要求消费 nonce；新事件或 changed payload 必须先通过 nonce 检查。依赖不可用返回 503，不推进支付。
- 失败映射：签名/证书/时间窗/nonce replay → `401 CALLBACK_SIGNATURE_INVALID`；同 event 不同 payload → `409 PAYMENT_EVENT_CONFLICT`；依赖不可用 → `503 CALLBACK_RETRY_LATER`；重复同 event/同 fingerprint → `200 {accepted:true,duplicate:true}`。
- 原始 provider payload 由 adapter 加密留档（A0 `payment_event.payload_ciphertext`），不得明文日志、不得返回内部结果；响应只含 `accepted`/`duplicate`。
- provider nonce 是按 HTTP delivery 的 transport replay 保护；vendor 重试是否复用 nonce 属 vendor policy Q6。A3 冻结 fail-closed 与 event idempotency，不发明 vendor 重试行为。
- A2 不受影响：provider callback 不参与 IVR call/event 状态机；无 redial/reservation/settlement 副作用。

### 13.3 Nonce/replay 与 A2 exactly-once

- Nonce 只用于 transport replay protection，不进入 A2 semantic identity；`eventId`/`payload` 不变，transport timestamp/nonce 每次重试更新。
- 认证/签名/timestamp/nonce/replay 失败在 `resolve`、`confirm`、`direct`、`events`、`declare`、`complete` 的 A2 处理前返回；不得创建/复用 session、call、decision、task、reservation、call_event、recording、media_object、usage_fact。
- 同一 `callId`/`eventId`/`recordingId`/`decisionId` 的身份规则不变；重试不得 mint 新身份。
- A2 的 event registry digest、12 messages、22 identities、15 invariants、outbox 四态、终态吸收、UNKNOWN/reconciliation 和 settlement 规则不变。

## 14. IVR outbox 401/403 本地安全策略

A2 §6.6 outbox 分类未命名 `UNAUTHENTICATED`/`FORBIDDEN`（401/403）。S2 冻结 IVR-local security-layer policy，不修改 A2：

- 观测到 401 或 403 时，同一 outbox entry 保持既有 `PENDING` 状态，并在本地 operational metadata 可用时逻辑标记 security-blocked。
- 不得自动热重试、不得新增/改写 event identity、不得改 payload、不得重拨、不得 re-resolve、不得预占或结算、不得写 A2 业务事实。
- 必须发出不含 credential/signature/nonce 的 alert/audit signal；固定 redaction token 为 `[REDACTED:CREDENTIAL]`、`[REDACTED:SIGNATURE]`、`[REDACTED:NONCE]`。
- 只有在收到显式 credential/key/certificate 或 service-authorization recovery signal 后才允许重试；重试使用同一 domain identity（`callId`/`eventId`/`recordingId`/`decisionId`）与 immutable payload，并生成新的 timestamp/nonce。
- 数值 retry/backoff/alert 值仍为 Q8/Q13/Q25 deferred。本策略不新增 A2 state/event/digest 或 server classification；`PENDING` 是 A2 既有四态之一。

## 15. Invariants（累计 21）

| ID | Statement |
| --- | --- |
| INV-A3-01 | Every protected datum has exactly one dataClass, owner, representation, key/HMAC class and audit/access coverage; no unregistered sensitive field exists. |
| INV-A3-02 | Encryption and HMAC keys are strictly separated; no key crosses purpose; cross-purpose decrypt or lookup must fail. |
| INV-A3-03 | Envelope shape, key_ref parsing and AEAD/AAD are fixed; unknown/malformed/retired key_ref, wrong purpose or decryption failure must fail closed with no plaintext fallback. |
| INV-A3-04 | HMAC is lowercase 64-hex under an independent key; it is used only for equality, uniqueness and indexed lookup and is never returned, logged or public. |
| INV-A3-05 | Every key and credential has an owner, custody, state, rotation and recovery path; secret material never enters a database, code, image or log. |
| INV-A3-06 | Rotation never rewrites append-only or frozen rows; historical keys are retained until retention; A2 replay returns the same payload/decision without re-selection or a second reservation. |
| INV-A3-07 | The five authentication domains never share a token, credential, audience or verifier; a request that crosses domains is rejected. |
| INV-A3-08 | The six service identities use least privilege, independent CAM roles and rotation; no frontend holds a service secret and no policy grants a wildcard action or resource. |
| INV-A3-09 | Every IVR request must pass mTLS, signature, timestamp and nonce verification; the verification order and canonical string are fixed. |
| INV-A3-10 | Replay rejection occurs before any domain side effect; transport nonce and timestamp are refreshed on every retry while A2 eventId and payload stay unchanged. |
| INV-A3-11 | Provider callbacks require mTLS, vendor signature, timestamp and nonce; the same event and fingerprint is idempotent while a changed payload is a conflict. |
| INV-A3-12 | Media credentials are short-lived, single-object or single-purpose and least-privilege; upload cannot read, list or delete, playback cannot write, and both are redacted. |
| INV-A3-13 | COS remains private with no anonymous access; no object key, bucket, version or permanent URL appears in a public contract or log. |
| INV-A3-14 | Audit is append-only through audit.append_fact; high-risk operations require role, state, reason and before-after in one transaction; audit reads are employee-only. |
| INV-A3-15 | Redaction runs deterministically before emit; deny-listed fields, credential URLs and header secrets never enter logs, traces, errors, analytics, support or public contracts. |
| INV-A3-16 | Support views show only masked, redacted, internal ID and status values and never grant recording playback or expose an original sensitive value. |
| INV-A3-17 | KMS, Secret Manager, Redis nonce, COS, database and audit failures fail closed with no plaintext fallback, credential issuance, evidence solidification, redial or duplicate A2 business side effect. |
| INV-A3-18 | Region, SKU, price, capacity, WORM and object-lock are never invented; every production value is a typed deferred input with an owner and destination. |
| INV-A3-19 | Only the fixed ADMIN and OPS employee roles exist; no new employee role, service decomposition or infrastructure topology is introduced. |
| INV-A3-20 | Every key, credential and service-call class has an audit or access owner and a testable scenario; the registry has no orphan row. |
| INV-A3-21 | The accepted A0, A1 and A2 contracts are unchanged; the validator proves their counts, shapes and digests, and any necessary change is a blocker. |

S3 累计 invariant 恰好 21 个（S1 冻结 6，S2 累计 11，S3 累计 21）。任何未批准的更高 invariant ID、section 或 fixture ID 都被 validator 拒绝。

## 16. 媒体凭证与 COS 私有策略（S3）

媒体 authority 只有两个策略。U1 upload 的唯一 primary shape 是 short-lived single-object Tencent COS presigned PUT，仅在 CV-4 验证 required method/object/expiry/size 后签发；能力不可验证时 fail closed 并停止，不 fallback 到 custom HMAC token 或 signing gateway。P1 playback 是 opaque media-gateway bearer，客户端以 `Authorization: Bearer <credential>` 调用已鉴权的 media-gateway data plane；bearer 单独解析 share/admission/generation/object/purpose，客户端不提供 COS key/bucket/version 或任何 public A1 resource identifier。两者都不返回永久 URL，且在任何 egress 中被 redacted。

<!-- security-media-map:start -->
| policyId | mediaKind | credentialClass | primaryShape | issuanceGate | scope | presentation | forbiddenOperations | redaction |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| U1 | UPLOAD | C09 uploadCredential | short-lived single-object Tencent COS presigned PUT credential | CV-4 verifies the required method, object and expiry plus supported size restrictions before issuance; otherwise fail closed and stop for architecture resolution | single recording object; PUT only; declared size, MIME and duration limits; valid until uploadExpiresAt | response-only opaque 1-2048 credential; the IVR presents it directly to the private COS object over a controlled channel; it never crosses a public contract | READ/LIST/DELETE | the credential is redacted in every egress and is never logged, split into object fields or reissued on replay |
| P1 | PLAYBACK | C10 playbackCredential | opaque short-lived media-gateway bearer credential | share admission verifies share, admission, revocation generation, object scope and purpose before issuance; expiry is bounded by the session and Q22 | share, admission, generation, object and PLAYBACK/SEEK/RENEW only; no write, list or delete | Authorization: Bearer <credential> to the authenticated media-gateway data plane; the bearer alone resolves the object and the client supplies no object key, bucket, version or public resource identifier | WRITE/LIST/DELETE | the bearer and any credential URL are redacted in every egress |
<!-- security-media-map:end -->

Upload 的签发侧应用校验 `callId`/`recordingId`/`purpose` 并推导 hidden target object；`onecall:evidence-upload:v1` 只是签发侧 policy/audience 分类，不是 COS 内嵌 audience claim。完成后服务端重算 object identity、SHA-256、size、MIME 与 duration 并与 declaration 绑定；A2 complete 不固化 evidence。Playback 的 issued credential 不被远程召回，只受 Q22 撤销传播窗口与 expiry 约束；`SHARE_LOCKED` 只阻止新准入，不阻止已准入 P3 授权。精确 gateway origin/path、CDN/LB、range/streaming 与 delivery topology 仍是 `R-A3-MEDIA-GATEWAY-TRANSPORT` 的部署输入。

**COS 私有策略**

- Bucket 固定私有；禁止匿名 read/write/list；TLS-only；block public access。
- 对象按 RAW_RECORDING、DERIVED_PREVIEW、TRANSCRIPT、SUMMARY、EXPORT 分类；每类独立 prefix/class；派生对象不覆盖原始录音。
- 加密固定为 `COS SSE-KMS under K10`；版本控制固定为 `COS versioning enabled`；对象 key 不可猜测且永不 public。
- 访问只经 Tencent CAM service role、short-lived upload credential 或经 media-gateway 的 short-lived playback credential；日志只记录 object identity digest 与 hash prefix，不记录 key/bucket/version/URL。
- `region`、storage class、retention、`object lock/WORM`、private endpoint/VPC 与 HTTPS delivery path 全部 Q11/CV-4 typed deferred。本文件**不声称** `WORM`、`object lock`、`region`、`SKU`、`price`、`capacity` 或 `private endpoint` 已验证或可用。

## 17. 审计访问、高风险操作与调用类覆盖（S3）

审计真源是 PostgreSQL `audit.audit_fact`；日志平台副本不是审计真源。只通过 `audit.append_fact` 写入；`audit_reader_role` 只有 SELECT，无 UPDATE/DELETE/TRUNCATE。审计读取只限 `employee` domain 的固定 ADMIN/OPS，成功读取写 `AUDIT_READ` access event，拒绝写 `AUDIT_READ_DENIED` access event；两者都**不得**递归调用 `audit.append_fact`。其余 37 行使用 `append_fact`。projection 只含 status/revision/reasonCode，不返回 raw snapshot。

<!-- security-audit-map:start -->
| actionCode | actor | objectType | trigger | persistence |
| --- | --- | --- | --- | --- |
| SECURITY_KEY_ACTIVATE | SERVICE/SYSTEM | KEY | key version 激活 | append_fact |
| SECURITY_KEY_ROTATE | SERVICE/SYSTEM | KEY | 新旧 version overlap | append_fact |
| SECURITY_KEY_RETIRE | SERVICE/SYSTEM | KEY | 旧 version retire | append_fact |
| SECURITY_KEY_DISABLE | SERVICE/SYSTEM | KEY | 紧急 disable | append_fact |
| SECURITY_KEY_RECOVER | SERVICE/SYSTEM | KEY | re-enable/restore | append_fact |
| SECURITY_KEY_DESTROY | SYSTEM | KEY | pending/executed destroy | append_fact |
| SECURITY_KEY_PURPOSE_MISMATCH | SERVICE/SYSTEM | KEY | purpose/dataClass 不匹配 | append_fact |
| SECURITY_KEY_ACCESS_DENIED | SERVICE/SYSTEM | KEY | 未授权 key/secret 访问 | append_fact |
| SECURITY_AUTH_DENIED | SERVICE/SYSTEM | CREDENTIAL | 会话/凭据无效 | append_fact |
| SECURITY_AUDIENCE_MISMATCH | SERVICE/SYSTEM | CREDENTIAL | 跨域 token/credential | append_fact |
| SECURITY_SERVICE_IDENTITY_DENIED | SERVICE/SYSTEM | SERVICE | mTLS/CAM 身份不允许 | append_fact |
| SECURITY_SIGNATURE_INVALID | SERVICE/SYSTEM | REQUEST | IVR/provider 签名失败 | append_fact |
| SECURITY_REPLAY_REJECTED | SERVICE/SYSTEM | REQUEST | nonce replay | append_fact |
| SECURITY_TIMESTAMP_REJECTED | SERVICE/SYSTEM | REQUEST | timestamp 超窗 | append_fact |
| SECURITY_PROVIDER_CALLBACK_REJECTED | SERVICE/SYSTEM | PAYMENT_EVENT | provider 回调拒绝 | append_fact |
| SECURITY_UPLOAD_CREDENTIAL_ISSUED | SERVICE | RECORDING | upload credential 签发 | append_fact |
| SECURITY_UPLOAD_CREDENTIAL_DENIED | SERVICE | RECORDING | upload scope/expiry deny | append_fact |
| SECURITY_PLAYBACK_CREDENTIAL_ISSUED | SERVICE | SHARE_ADMISSION | playback credential 签发 | append_fact |
| SECURITY_PLAYBACK_CREDENTIAL_DENIED | SERVICE | SHARE_ADMISSION | playback scope/expiry deny | append_fact |
| SECURITY_COS_POLICY_DENIED | SERVICE/SYSTEM | MEDIA_OBJECT | 私有策略/对象访问拒绝 | append_fact |
| AUDIT_READ | EMPLOYEE | AUDIT | 审计读取 access event | access_event |
| AUDIT_READ_DENIED | SERVICE/EMPLOYEE | AUDIT | 审计读取拒绝 | access_event |
| OPERATIONS_HIGH_RISK_DENIED | EMPLOYEE | OPERATIONS | 高风险 role/state/reason 拒绝 | append_fact |
| SECURITY_SENSITIVE_READ | EMPLOYEE/SERVICE | DATA_CLASS | 员工/支持读取敏感字段 | append_fact |
| SECURITY_REDACTION_BLOCKED | SERVICE/SYSTEM | EXPORT | 未知字段/脱敏失败被阻断 | append_fact |
| SECURITY_REDACTION_VIOLATION | SERVICE/SYSTEM | EXPORT | 检测到明文/secret 出口 | append_fact |
| SUBJECT_SUSPEND | EMPLOYEE | SUBJECT | OPS suspend | append_fact |
| SUBJECT_RESUME | EMPLOYEE | SUBJECT | OPS resume | append_fact |
| CATALOG_VERSION_PUBLISH | EMPLOYEE | CATALOG_VERSION | ADMIN publish | append_fact |
| CATALOG_VERSION_OFFSALE | EMPLOYEE | CATALOG_VERSION | ADMIN offsale | append_fact |
| TRIAL_POLICY_CREATE | EMPLOYEE | TRIAL_POLICY | ADMIN create draft | append_fact |
| TRIAL_POLICY_REPLACE | EMPLOYEE | TRIAL_POLICY | ADMIN replace draft | append_fact |
| TRIAL_POLICY_PUBLISH | EMPLOYEE | TRIAL_POLICY | ADMIN publish | append_fact |
| TRIAL_POLICY_DISABLE | EMPLOYEE | TRIAL_POLICY | ADMIN disable | append_fact |
| CATALOG_DRAFT_CREATE | EMPLOYEE | CATALOG_VERSION | ADMIN create draft | append_fact |
| CATALOG_DRAFT_REPLACE | EMPLOYEE | CATALOG_VERSION | ADMIN replace draft | append_fact |
| SHARE_REVOKE | ACCOUNT | SHARE_GRANT | customer revoke | append_fact |
| SHARE_CODE_ROTATE | ACCOUNT | SHARE_GRANT | customer code rotation | append_fact |
| PAYMENT_CALLBACK_ACCEPTED | SERVICE | PAYMENT_EVENT | accepted/duplicate provider event | append_fact |
<!-- security-audit-map:end -->

高风险员工 mutation 固定 10 个，只允许 ADMIN/OPS。合法请求必须校验 employee session、role、当前 status/revision、expected revision 与 reason source，并在同一事务写 domain mutation 与 `audit.append_fact`；audit 失败整体回滚。`ACTION_DERIVED` 仅是 A1 请求 schema 所需的 A0 `reason_code` 技术投影，不是员工手工输入的动机；语义缺口仍由 `R-A3-A1-EMPLOYEE-REASON-PROJECTION` 承接。运营 UI 二次确认是架构要求的运行时控制，但 S3 不做 UI/prototype 工作；静态模型、validator 与 fixtures **不**声称其已实现，标记 runtime-unverified，不作为 validator-assertable success criterion。

<!-- security-high-risk-map:start -->
| operationId | role | reasonSource | preconditionState | auditCode | conflictCode | beforeAfter |
| --- | --- | --- | --- | --- | --- | --- |
| suspendOperationsSubject | OPS | REQUEST reasonCode | ACTIVE | SUBJECT_SUSPEND | ACTIVATION_STATE_CONFLICT | status/revision/reason |
| resumeOperationsSubject | OPS | REQUEST reasonCode | SUSPENDED | SUBJECT_RESUME | ACTIVATION_STATE_CONFLICT | status/revision/reason |
| publishOperationsCatalogVersion | ADMIN | ACTION_DERIVED | DRAFT_NEVER_PUBLISHED | CATALOG_VERSION_PUBLISH | PUBLICATION_STALE_VIEW | status/revision/snapshot hash |
| offsaleOperationsCatalogVersion | ADMIN | ACTION_DERIVED | ACTIVE | CATALOG_VERSION_OFFSALE | PUBLICATION_STALE_VIEW | status/revision |
| createOperationsTrialPolicyDraft | ADMIN | ACTION_DERIVED | NATURAL_KEY_ABSENT | TRIAL_POLICY_CREATE | TRIAL_POLICY_DRAFT_CONFLICT | policy identity/revision |
| replaceOperationsTrialPolicyDraft | ADMIN | ACTION_DERIVED | DRAFT_NEVER_PUBLISHED | TRIAL_POLICY_REPLACE | TRIAL_POLICY_DRAFT_STALE_VIEW | revision/snapshot hash |
| publishOperationsTrialPolicy | ADMIN | ACTION_DERIVED | DRAFT_NEVER_PUBLISHED | TRIAL_POLICY_PUBLISH | TRIAL_POLICY_STALE_VIEW | enabled/publisher/revision |
| disableOperationsTrialPolicy | ADMIN | ACTION_DERIVED | ENABLED | TRIAL_POLICY_DISABLE | TRIAL_POLICY_STALE_VIEW | enabled/revision |
| createOperationsCatalogVersionDraft | ADMIN | ACTION_DERIVED | NATURAL_KEY_ABSENT | CATALOG_DRAFT_CREATE | CATALOG_DRAFT_CONFLICT | identity/revision |
| replaceOperationsCatalogVersionDraft | ADMIN | ACTION_DERIVED | DRAFT | CATALOG_DRAFT_REPLACE | CATALOG_DRAFT_STALE_VIEW | revision/snapshot hash |
<!-- security-high-risk-map:end -->

敏感非员工操作保持 ACCOUNT/SERVICE actor，不新增 role：`revokeCustomerShare` → `SHARE_REVOKE`；`rotateCustomerShareCode` → `SHARE_CODE_ROTATE`（generation +1，响应不返回 capability token）；`receivePaymentProviderCallback` → `PAYMENT_CALLBACK_ACCEPTED`，拒绝走 `SECURITY_PROVIDER_CALLBACK_REJECTED`，同 event/同 payload 幂等。

<!-- security-coverage-map:start -->
| callClass | owner | authentication | auditAccessClass | redaction | failClosed |
| --- | --- | --- | --- | --- | --- |
| CUSTOMER_API | customer_iam/catalog_order/sharing/activation_consent | customerSession audience onecall:customer-session:v1 | business mutations append_fact; AUDIT_READ and AUDIT_READ_DENIED are access events | masked value, internal ID or status only; no credential, capability token or ciphertext | existing 401/403/503 with no plaintext fallback |
| OPERATIONS_API | operations_iam/audit | employeeSession audience onecall:employee-session:v1 with fixed ADMIN/OPS | high-risk operations append_fact; audit reads use access events | no raw snapshot, PII, credential or object identifier | existing 401/403/409/500 with transaction rollback |
| IVR_API | ivr/evidence | ivrMtls plus ivrSignature plus timestamp plus nonce | security and media events follow the registry persistence route | no signature, nonce, credential, object key or plaintext party number | existing 401/403/503 with zero A2 business side effect |
| PUBLIC_SHARE_API | sharing/evidence/media-gateway | capability C07 or C08; C10 bearer only at media-gateway | credential events append_fact; denied audit reads are access events | no capability token, playback credential, object key or URL | existing 404/403/410/503 without admission recount |
| PROVIDER_CALLBACK_API | catalog_order | providerCallbackMtls plus providerCallbackSignature plus timestamp plus nonce | accepted event uses PAYMENT_CALLBACK_ACCEPTED; rejection uses SECURITY_PROVIDER_CALLBACK_REJECTED | no provider payload, signature, nonce or internal result | existing 401/409/503 without payment advance |
| INTERNAL_SERVICE_CALL | all owning domains | Tencent CAM service role plus audience | dispatch access is redacted; security failures append_fact | authorized fields only; no cross-module secret | existing 503/500 without partial write |
| KMS_OPERATION | Security/Infrastructure | Tencent CAM service role | SECURITY_KEY events append_fact | never log key_ref, plaintext, DEK or ciphertext | existing 503 with no plaintext fallback |
| SECRET_MANAGER_READ | Security/Infrastructure | Tencent CAM service role | SECURITY_KEY_ACCESS_DENIED or SECURITY_KEY_RECOVER appends a fact | secret value never emitted | existing 503 with no environment or image fallback |
| COS_OPERATION | evidence/media | Tencent CAM service role or a short-lived upload or playback credential | media and COS policy events append_fact; provider logs are supplementary | object identity digest and hash prefix only | existing 503 or STORAGE_UNKNOWN without fabricated media facts |
| DATABASE_ACCESS | each domain NOLOGIN role | NOLOGIN database role | audit.append_fact for facts; access events for reads | redacted snapshots only | transaction rollback without partial write |
<!-- security-coverage-map:end -->

## 18. 确定性脱敏与 fail-closed 矩阵（S3）

统一 redaction pipeline 必须在任何 emit 前执行：结构化 allow-list → 任意深度递归 deny-list → credential/header/URL pattern controls → destination policy → emit。deny-list 大小写不敏感、任意深度，固定包含 A0 §10.7 全部键 `phone,mobile,callerNumber,calleeNumber,providerSubject,identityNumber,idNumber,legalName,shareCode,otp,objectKey,objectVersionId,bucket,providerTransactionId,providerEventId,secret,token,password,hmac,ciphertext,keyRef,kmsKey`，并追加 `credential,capabilityToken,extractionCode,authorization,cookie,signature,nonce,accessNumberHmac`。masked display 仅允许 safe-class `PHONE`、`IDENTITY_NUMBER`、`LEGAL_NAME` 三类（如 `calleeNumberMasked`）；任何其他或未知的 `*Masked` key 一律按未知字段 fail-closed（block/drop），不得仅凭后缀信任。不得把 encrypted envelope 当普通日志字段。固定 token 形如 `[REDACTED:<CLASS>]`；同一输入在相同出口必须产生相同 token，禁止随机值。

| class token | 覆盖字段 |
| --- | --- |
| [REDACTED:PHONE] | phone / mobile / callerNumber / calleeNumber |
| [REDACTED:PROVIDER_SUBJECT] | providerSubject |
| [REDACTED:IDENTITY_NUMBER] | identityNumber / idNumber |
| [REDACTED:LEGAL_NAME] | legalName |
| [REDACTED:SHARE_CODE] | shareCode |
| [REDACTED:OTP] | otp |
| [REDACTED:OBJECT_KEY] | objectKey |
| [REDACTED:OBJECT_VERSION] | objectVersionId |
| [REDACTED:BUCKET] | bucket |
| [REDACTED:PROVIDER_TRANSACTION_ID] | providerTransactionId |
| [REDACTED:PROVIDER_EVENT_ID] | providerEventId |
| [REDACTED:SECRET] | secret |
| [REDACTED:TOKEN] | token |
| [REDACTED:PASSWORD] | password |
| [REDACTED:HMAC] | hmac |
| [REDACTED:CIPHERTEXT] | ciphertext |
| [REDACTED:KEY_REF] | keyRef |
| [REDACTED:KMS_KEY] | kmsKey |
| [REDACTED:CREDENTIAL] | credential / uploadCredential / playbackCredential |
| [REDACTED:CAPABILITY_TOKEN] | capabilityToken |
| [REDACTED:EXTRACTION_CODE] | extractionCode |
| [REDACTED:AUTHORIZATION] | authorization |
| [REDACTED:COOKIE] | cookie |
| [REDACTED:SIGNATURE] | signature |
| [REDACTED:NONCE] | nonce |
| [REDACTED:ACCESS_NUMBER_HMAC] | accessNumberHmac |
| [REDACTED:PAYLOAD] | provider payload |
| [REDACTED:URL] | any absolute URL pattern |

路径脱敏固定为：`/api/v1/public/shares/{capabilityToken}/... is emitted as /api/v1/public/shares/[REDACTED:CAPABILITY_TOKEN]/...`。Error 只返回 stable Problem code、用户可理解信息与 `traceId`，不返回 provider/stack/SQL/secret。Support view 只允许 masked/redacted/internal ID/status/reason，不授予 playback，不展示 object key/URL/credential/HMAC/ciphertext。出口失败关闭：未知字段或 redaction 失败 → block/drop + `SECURITY_REDACTION_BLOCKED` + alert；对外路径返回 `500 INTERNAL`；**不得**先 emit 后清洗。

**Fail-closed 矩阵**

| 故障 | 受影响面 | 既有错误（不新增 code） | 副作用 |
| --- | --- | --- | --- |
| KMS/DEK decrypt 不可用、key disabled/destroyed、unknown key_ref | customer/operations/ivr | 503 SERVICE_UNAVAILABLE | 不返回 plaintext；无业务写入 |
| 同上 | public-share | 503 DEPENDENCY_UNAVAILABLE 或 503 SHARE_SESSION_POLICY_UNAVAILABLE | 不签发媒体凭证；不重计准入 |
| 同上 | provider | 503 CALLBACK_RETRY_LATER | 不推进支付/权益 |
| Secret Manager/IVR signing key 不可用 | ivr | 503 SERVICE_UNAVAILABLE | 不验签成功、不进入 A2 |
| Redis/nonce store 不可用 | ivr | 503 SERVICE_UNAVAILABLE | 不消费 nonce、不进入 A2 |
| Redis/session store 不可用 | customer/employee/public-share | 503 SERVICE_UNAVAILABLE 或 503 SHARE_SESSION_POLICY_UNAVAILABLE | 不绕过认证；不创建会话 |
| COS/upload signing 不可用 | ivr declare | 503 SERVICE_UNAVAILABLE | 不签发 upload credential |
| COS/storage unknown | ivr complete | 503 STORAGE_UNKNOWN | 不产生 media fact、不终态变更 |
| COS/playback signing 不可用 | public-share P3 | 503 DEPENDENCY_UNAVAILABLE 或 503 SHARE_SESSION_POLICY_UNAVAILABLE | 不签发 playback credential |
| audit append 失败 | operations/ivr media/payment | 500 INTERNAL（或该面既有 503） | 领域事务回滚；无半写 |
| HMAC key 不可用 | 任意 lookup | 该面既有 503 | 不做明文扫描、不返回假空 |
| redaction pipeline 失败 | 任意出口 | 500 INTERNAL / 丢弃日志字段 | 不 emit 原文；告警 |
| MFA/IdP 不可用（Q1） | employee/customer 登录 | 该面既有 401/503 | 不建 session |
| provider profile/nonce 不可用 | provider | 503 CALLBACK_RETRY_LATER | 不推进支付 |

<!-- security-failclosed-map:start -->
| failureId | failure | affectedSurfaces | existingError | sideEffects |
| --- | --- | --- | --- | --- |
| KMS_DECRYPT_UNAVAILABLE | KMS or DEK decrypt unavailable, key disabled or destroyed, or unknown key_ref | customer, operations, ivr | 503 SERVICE_UNAVAILABLE | no plaintext returned; no business write |
| KMS_DECRYPT_PUBLIC_SHARE | KMS or DEK decrypt unavailable, key disabled or destroyed, or unknown key_ref | public-share | 503 DEPENDENCY_UNAVAILABLE or 503 SHARE_SESSION_POLICY_UNAVAILABLE | no media credential issued; no admission recount |
| KMS_DECRYPT_PROVIDER | KMS or DEK decrypt unavailable, key disabled or destroyed, or unknown key_ref | provider | 503 CALLBACK_RETRY_LATER | no payment or entitlement advance |
| SECRET_MANAGER_SIGNING_UNAVAILABLE | Secret Manager or IVR signing key unavailable | ivr | 503 SERVICE_UNAVAILABLE | no verification success; no A2 processing |
| REDIS_NONCE_UNAVAILABLE | Redis nonce store unavailable | ivr | 503 SERVICE_UNAVAILABLE | no nonce consumption; no A2 processing |
| REDIS_SESSION_UNAVAILABLE | Redis session store unavailable | customer, employee, public-share | 503 SERVICE_UNAVAILABLE or 503 SHARE_SESSION_POLICY_UNAVAILABLE | no authentication bypass; no session creation |
| COS_UPLOAD_SIGNING_UNAVAILABLE | COS or upload signing unavailable | ivr | 503 SERVICE_UNAVAILABLE | no upload credential issued |
| COS_STORAGE_UNKNOWN | COS or storage outcome unknown | ivr | 503 STORAGE_UNKNOWN | no media fact; no terminal state change |
| COS_PLAYBACK_SIGNING_UNAVAILABLE | COS or playback signing unavailable | public-share | 503 DEPENDENCY_UNAVAILABLE or 503 SHARE_SESSION_POLICY_UNAVAILABLE | no playback credential issued |
| AUDIT_APPEND_FAILED | audit append failure | operations, ivr media, payment | 500 INTERNAL or the existing 503 when no 4xx semantics apply | domain transaction rollback; no partial write |
| HMAC_KEY_UNAVAILABLE | HMAC key unavailable | any lookup | existing 503 for the surface | no plaintext scan; no fake empty result |
| REDACTION_PIPELINE_FAILED | redaction pipeline failure | any egress | 500 INTERNAL or a dropped log field | no raw value emitted; alert raised |
| MFA_OR_IDP_UNAVAILABLE | MFA or identity provider unavailable (Q1) | employee login, customer login | existing 401 or 503 for the surface | no session creation |
| PROVIDER_PROFILE_OR_NONCE_UNAVAILABLE | provider profile or nonce store unavailable | provider | 503 CALLBACK_RETRY_LATER | no payment advance |
<!-- security-failclosed-map:end -->

任何认证/签名/nonce/COS/audit/redaction 失败都发生在领域副作用之前或使领域事务回滚：无 plaintext fallback、无凭证签发、无 evidence 固化、无 redial、无第二 reservation/settlement/event/media object。所有错误引用 A1 既有 Problem code 或 `500 INTERNAL`，不新增 code。

## 19. 确定性验证、fixtures 与诚实限制

验证命令（在 repo root，S1/S2/S3 使用相同参数并切换 `--stage`）：

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
node security/validate.mjs --stage s2 \
  --model security/v1/security-design-v1.json \
  --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures
node security/validate.mjs --stage s3 \
  --model security/v1/security-design-v1.json \
  --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures
```

`--stage s1` 只评估 6 个 S1 fixture，`--stage s2` 评估累计 15 个（`6+9`，7 个负例），二者忽略后续 slice 的 section/marker/fixture。`--stage s3` 评估累计 23 个（`6+9+8`，10 个负例）并要求全部 S3 section/marker/invariant/fixture 精确存在。S3 期望计数固定为：

```text
stage=s3 trust_surfaces=5 auth_domains=5 trust_boundaries=7 service_identities=6 credential_classes=10 signature_profiles=2 media_policies=2 audit_events=39 audit_persistence=39 audit_access_events=2 audit_append_facts=37 high_risk_actions=10 audit_coverage=10 key_classes=20 data_classes=28 deferred_inputs=23 deferred_q=18 deferred_cv=5 invariants=21 fixtures=23 passed=23 failed=0 negative=10 slice_fixtures=6+9+8
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

S2 fixtures（9，恰 5 个显式负例）：

| Fixture | 场景 | 关键断言 |
| --- | --- | --- |
| `sec-s2-01-five-domains-accepted` | 五个域各自有效 credential | 仅本面 2xx；audience/operation 计数正确；零跨域与 A2 副作用 |
| `sec-s2-02-cross-domain-rejected` | 负例：customer/employee/capability/ivr/provider token 交叉使用 | A1 既有 401/403/404；零 A2 状态写入 |
| `sec-s2-03-service-identity-least-privilege` | 6 service identities allowed/denied calls | 仅 allowed calls；无 `*`；前端无 secret；拒绝 audit read |
| `sec-s2-04-ivr-signature-invalid` | 负例：缺/坏 mTLS、坏签名、超窗、超窗+坏签名组合（timestamp 先于 signature）、未知 keyId、坏 nonce、service identity 不允许 | 401/403；header 名与 TIMESTAMP 1-64/NONCE 1-128 精确，SIGNATURE 只断言名称+值格式（无长度断言）；组合步骤固定 `TIMESTAMP_OUT_OF_WINDOW`；零 session/call/decision/task/reservation/event |
| `sec-s2-05-ivr-replay-rejected` | 负例：同 nonce replay；Redis 不可用；修复后同 eventId 新 nonce 重试 | 401/503；零副作用；重试后 A2 首次结果；无 redial/第二 reservation；成功请求独立固定六行 canonical string（lowercase method 归一化为 uppercase、percent-encoded raw path 不 decode、body lowercase hex SHA-256） |
| `sec-s2-06-provider-signature-replay` | 负例：坏签名、nonce replay、duplicate、changed payload | 401 `CALLBACK_SIGNATURE_INVALID`；duplicate 200；changed 409 `PAYMENT_EVENT_CONFLICT`；无重复支付/权益 |
| `sec-s2-07-signing-key-rotation` | IVR/provider key 新旧 overlap/retire | 旧 key `VERIFY_ONLY` 可验；retire 后 401；无 A2 状态副作用 |
| `sec-s2-08-failclosed-dependency-preserves-a2` | SM/Redis 不可用；恢复后同 identity 重试 | 503；无 A2 写入；outbox 不改 eventId；A2 digest/状态不变 |
| `sec-s2-09-ivr-outbox-auth-blocked` | 负例：IVR outbox 分别观测 401 与 403；hot retry/identity/payload/redial/re-resolve/reservation/settlement 均被拒绝；显式 recovery 后重试 | 同一 entry 保持 `PENDING`/security-blocked；alert/audit 不含 credential；recovery 使用同一 domain identity + immutable payload + 新 timestamp/nonce；零 A2 业务副作用；`invariants=11` 且无未来文本 |

S3 fixtures（8，恰 3 个显式负例）：

| Fixture | 场景 | 关键断言 |
| --- | --- | --- |
| `sec-s3-01-upload-credential-scope` | CV-4 未验证 fail closed；签发/使用/完成 upload credential | 唯一 COS presigned PUT；单对象/单 purpose；跨对象/purpose/过期/超 size/read/list/delete deny；无永久 URL；complete 重校验 server-observed identity/hash/size/MIME/duration；无 fallback |
| `sec-s3-02-playback-credential-scope` | 负例：bearer presentation；跨对象/purpose/generation/过期/撤销/写/客户端 object 字段 | bearer 被验证且 credential redacted；无 COS URL/key/bucket/version；无写；无准入重计 |
| `sec-s3-03-cos-private-policy` | 匿名读/写/list、public URL、key/bucket/version 泄漏、WORM 声称 | 全部 deny；SSE-KMS/versioning/私有/TLS-only 固定；WORM/object-lock/region/SKU 不声称 |
| `sec-s3-04-audit-access-denied` | 负例：customer/ivr/public/provider 读 audit；employee 读；UPDATE/DELETE/TRUNCATE | 非 employee 拒绝；employee 只 redacted projection；access_event route；无递归 append_fact；39 行 persistence 精确 2+37 |
| `sec-s3-05-high-risk-controls` | 10 个员工 mutation 的 role/state/reason/audit | 合法通过；坏 role/state/reason 拒绝；audit 失败回滚；registry 中无 last-admin 移除或角色创建操作；UI 二次确认不作断言 |
| `sec-s3-06-redaction-deterministic` | 递归 deny-list、masked、credential/header/path pattern、未知字段 | 固定 `[REDACTED:*]`；masked 仅 PHONE/IDENTITY_NUMBER/LEGAL_NAME safe-class 允许；未知字段 block；确定性；无原文 |
| `sec-s3-07-credential-url-leak` | 负例：capabilityToken path、upload/playback credential、signature/nonce、Authorization/Cookie | 所有出口无 secret；public 只 masked/ID；analytics/support 无 playback |
| `sec-s3-08-end-to-end-security` | IVR media + share playback + audit + provider callback | 全链路身份/凭证/审计/脱敏/fail-closed；complete 不固化 evidence；累计计数正确 |

fixtures 使用 `TEST_ONLY_*` 合成标签与固定 nonce；密钥字节由 label 的 SHA-256 确定性派生。fixture 中的 window/TTL 输入只是确定性 evaluator 输入，不是生产 Q25 值。它们不包含真实 secret、URL、credential、phone、identity 或 cloud identifier。S3 fixture 不得把 UI 二次确认作为静态断言。

validator 独立解析模型、文档 marker、A0/A1/A2 与 fixtures，并计算 fixture 行为；不读取 `expected` 来决定结果，也不存在脱离解析输入的成功打印。缺少/多余/重复/非法 row、跨域 token、通配权限、前端 secret、畸形/缺失 header、时间戳格式/窗口、unknown/disabled key、坏签名、nonce replay/Redis 不可用、provider duplicate-vs-conflict 顺序、IVR outbox 违规重试/身份变更、upload/playback scope 越界、CV-4 未验证 fallback、COS public URL、audit 越权/改写、`AUDIT_READ`/`AUDIT_READ_DENIED` 误走 `append_fact`、high-risk 缺 role/state/reason、redaction 泄漏、未知字段未阻断、stage 内容提前/缺失、fixture ID/slice 不匹配与上游 drift 都会失败。

诚实限制：没有运行真实 KMS、Secret Manager、CAM、COS、Redis、PostgreSQL、IVR 线路、日志平台、analytics 或供应商；不能证明真实 mTLS 证书、真实 Redis `SET NX`、生产 nonce 的 CSPRNG、真实 envelope 解密、真实云 rotation/recovery、数据库 append-only trigger、审计事务回滚或媒体对象行为。fixtures 是安全模型级断言，不是生产验收或渗透测试。

## 20. 残余风险与完成声明

| ID | 内容 | Owner / Destination |
| --- | --- | --- |
| `R-A3-HMAC-ROTATION-REPLAY` | HMAC 轮换与 A2 `accessNumberHmac` fingerprint/冻结 HMAC 的运行时兼容；需历史 key 候选与 retention | Security/IVR/Backend；integration + Q3/Q25 |
| `R-A3-KMS-CAPABILITY` | KMS/Secret Manager envelope、rotate、recover、dual-version 未验证（CV-1/CV-2） | Infrastructure/Security；Q2/CV validation |
| `R-A3-COS-CAPABILITY` | COS region/private endpoint/SSE-KMS/versioning/presign/object-lock/WORM 未验证（CV-4/Q11） | Infrastructure/Security；storage/capacity |
| `R-A3-KEY-DESTRUCTION` | destroyed key 不可恢复；无明文备份/escrow 除非另行批准 | Security/Infrastructure；incident/recovery policy |
| `R-A3-LEGAL-RETENTION` | retention/erasure/backup 未选（Q3） | Legal/Security；launch review |
| `R-A3-NO-WORM` | 不声称 COS WORM/object-lock；未来需 Q11 验证 | Infrastructure/Security；storage policy |
| `R-A3-REGION-SKU` | 不选 region/SKU/price/capacity | Infrastructure/Security；capacity/deployment |
| `R-A3-A2-AUTH-OUTBOX` | IVR outbox 401/403 本地策略已冻结，但 Q8/Q13/Q25 的 retry/backoff/alert 数值与真实 Redis/IVR 集成未验证 | IVR/Backend；A2/IVR integration review |
| `R-A3-MTLS-CAPABILITY` | 真实 mTLS 证书签发、pinning、时钟同步与 IVR 线路身份未验证（Q7） | Telephony/IVR/Security；line integration |
| `R-A3-PROVIDER-VERIFY` | vendor signature 算法/canonicalization/payload/window 未冻结（Q6） | Payments/Backend；payment adapter |
| `R-A3-SESSION-LIFECYCLE` | 真实 session/OTP/WeChat 登录、员工 MFA 与 TTL 未验证（Q1/Q19） | Identity/Backend；authentication design |
| `R-A3-NONCE-STORE` | 真实 Redis `SET NX` 原子性、TTL 与故障切换未验证（Q25） | Security/Backend；production readiness |
| `R-A3-A1-EMPLOYEE-REASON-PROJECTION` | `ACTION_DERIVED` 只是 A0 `reason_code` 技术投影，不是员工动机；语义 operation-reason 缺口与 UI 二次确认 runtime 控制未在静态层证明 | Product/Operations + API governance；未来 A1/UI refinement |
| `R-A3-A1-UPLOAD-CREDENTIAL-SHAPE` | upload 唯一 primary shape 为 CV-4 门禁的 COS presigned PUT；能力不可验证时 fail closed 并停止，不 fallback | Security/Backend/Evidence；API/evidence review |
| `R-A3-MEDIA-GATEWAY-TRANSPORT` | C10 bearer presentation 已冻结；gateway origin/path、CDN/LB、range/streaming 与 delivery topology 未定，且不新增 A1 operation | Infrastructure/Security；media gateway deployment / storage capacity |
| `R-A3-REDACTION-RUNTIME` | 不能证明 edge/gateway/analytics/log collector 的真实脱敏配置；A3 只冻结 contract/fixtures | Platform/Security/Observability；logging review |
| `R-A3-AUDIT-VOLUME` | audit/access event volume、retention 与 DLQ 未定（Q13） | Operations/Backend；runbook/observability |
| `R-A3-SUPPORT-VIEW` | A1 无 operations playback；未来 break-glass 不在 A3 | Product/Security/Operations；future work unit |

S3 完成信号：2 media policies、私有 COS policy、39 audit/access events（2 `access_event` + 37 `append_fact`）、10 high-risk actions、10 call classes、确定性 redaction policy、14-row fail-closed matrix、21 cumulative invariants、23 fixtures（`6+9+8`，10 negatives）与 validator 均已落盘；S1/S2 的 registry/fixture/invariant 保持不变；A0/A1/A2 计数与 digest 由 validator 断言不变；没有新增表、列、状态、endpoint、header、Problem code、role 或生产数值。

本文件与模型不声称任何运行时或云实现，也不声称真实 KMS、Secret Manager、CAM、COS、Redis、PostgreSQL、IVR、gateway、日志平台、analytics 或支付供应商行为已被证明。S1/S2/S3 slice checkpoint 均已 accepted；本文件不承载 live aggregate/PR/Gateflow 生命周期，该状态仅以 `docs/gateflow/project-execution-control.md` 为权威。

### Completion report template

每个 slice 的 Gateflow 报告记录：task label、provider/model、gate、base commit、slice ID；精确 changed files 与 SHA-256；registry/marker/fixture inventory；安全决策；已执行命令与 observed exit/output；A0/A1/A2 drift 证据；负例与零副作用计数；deferred/residual 的 owner/destination；无运行时/云声明；以及未 stage/commit/push/PR/review 的声明。
