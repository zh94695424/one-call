# A5 生产验收矩阵规格

> 本文是 A5 的人类 canonical spec（S1）。机器 canonical 是 `acceptance/v1/acceptance-matrix-v1.json`；validator 是 `acceptance/validate.mjs`。本文 marker block 与模型逐行一致。静态规格覆盖不等于生产验收。

## 1. 目的与范围

A5 把 accepted A0-A4 契约映射为 9 个 critical family + 7 条 cross-system convergence path 的六轴验收矩阵。S1 只建立静态、来源可解析、fail-closed 的规格矩阵；不执行真实数据库、Redis、RabbitMQ、COS、KMS、CAM、支付、线路、云、渗透、压测、恢复或人工验收。

## 2. 单一真源与文件布局

- 人类 canonical：本文。
- 机器 canonical：`acceptance/v1/acceptance-matrix-v1.json`。
- 校验器：`acceptance/validate.mjs`（零三方依赖 Node 22 ESM，stage s1/s2）。
- 对抗 fixtures：`acceptance/fixtures/s1-01-matrix-integrity.json`、`s1-02-expectation-safety.json`、`s1-03-a0-state-shapes.json`、`s2-01-evidence-admission.json`。
- 本文不复制 A0-A4 的完整表格/枚举；只引用稳定 ID。A5 不建立第二语义 owner。

## 3. 覆盖单元与来源

<!-- acceptance-family-map:start -->
| Unit | Kind | Lead owner | Lead service | Paired unit |
| --- | --- | --- | --- | --- |
| FAM-IDENTITY | FAMILY | customer-iam | core-api | - |
| FAM-SUBJECT | FAMILY | subject-number | core-api | - |
| FAM-ACTIVATION | FAMILY | activation-consent | core-api | - |
| FAM-COMMERCE | FAMILY | catalog-order | core-api | - |
| FAM-IVR | FAMILY | ivr | ivr-gateway | - |
| FAM-MEDIA-EVIDENCE | FAMILY | evidence | core-api | - |
| FAM-SHARING | FAMILY | sharing | core-api | - |
| FAM-SECURITY | FAMILY | Security/Backend | core-api | - |
| FAM-DEPLOYMENT | FAMILY | Infrastructure/Security | deployment/capacity | - |
| PATH-ACTIVATION-TRIAL | PATH | activation-consent | core-api | FAM-ACTIVATION |
| PATH-COMMERCE-ENTITLEMENT | PATH | catalog-order | core-api | FAM-COMMERCE |
| PATH-IVR-SETTLEMENT | PATH | ivr | core-api | FAM-IVR |
| PATH-RECORDING-EVIDENCE | PATH | evidence | core-api | FAM-MEDIA-EVIDENCE |
| PATH-SHARE-ACCESS | PATH | sharing | core-api | FAM-SHARING |
| PATH-AUTH-AUDIT | PATH | platform-reliability | core-api | FAM-SECURITY |
| PATH-DEPLOY-ACCEPTANCE | PATH | Infrastructure/Security | deployment/capacity | FAM-DEPLOYMENT |
<!-- acceptance-family-map:end -->

## 4. 六轴与 N/A 契约

<!-- acceptance-axis-map:start -->
| Axis | Meaning |
| --- | --- |
| NORMAL | 正常契约路径与外部可见成功投影 |
| DUPLICATE | 相同 fingerprint 重放与 changed fingerprint 冲突 |
| TIMEOUT | contract/policy deadline 越界或依赖失败；不发明数值 |
| OUT_OF_ORDER | 迟到、乱序或终态后事实的收敛与隔离 |
| RECOVERY | restart、failover、reconciliation 或 restore 恢复 |
| PERMISSION_DENIED | 认证、授权或域边界拒绝且零业务副作用 |
<!-- acceptance-axis-map:end -->

N/A reason registry（validator 只接受以下固定 ID）：

<!-- acceptance-reason-map:start -->
| Reason | Allowed cells |
| --- | --- |
| NA-NO-CONTRACT-DEADLINE | FAM-DEPLOYMENT:TIMEOUT, FAM-SUBJECT:TIMEOUT, PATH-DEPLOY-ACCEPTANCE:TIMEOUT |
| NA-NO-ORDERED-FACT-STREAM | FAM-DEPLOYMENT:OUT_OF_ORDER, PATH-DEPLOY-ACCEPTANCE:OUT_OF_ORDER |
| NA-NO-AUTHZ-BOUNDARY | FAM-DEPLOYMENT:PERMISSION_DENIED |
<!-- acceptance-reason-map:end -->

`FAM-SUBJECT:TIMEOUT`、`FAM-ACTIVATION:TIMEOUT`、`FAM-IDENTITY:TIMEOUT` 不是 N/A：其 deadline 由 A0/A3 policy input（例如 Q1/Q19/Q25、trial policy version）参数化；scenario 只引用 policy ref，不写生产数值。

## 5. 覆盖矩阵（96 cells）

<!-- acceptance-coverage-map:start -->
| Unit | Axis | State | Scenario/Reason |
| --- | --- | --- | --- |
| FAM-IDENTITY | NORMAL | REPRESENTED | A5-IDENT-NORMAL-01 |
| FAM-IDENTITY | DUPLICATE | REPRESENTED | A5-IDENT-DUPLICATE-01 |
| FAM-IDENTITY | TIMEOUT | REPRESENTED | A5-IDENT-TIMEOUT-01 |
| FAM-IDENTITY | OUT_OF_ORDER | REPRESENTED | A5-IDENT-OUT_OF_ORDER-01 |
| FAM-IDENTITY | RECOVERY | REPRESENTED | A5-IDENT-RECOVERY-01 |
| FAM-IDENTITY | PERMISSION_DENIED | REPRESENTED | A5-IDENT-PERMISSION_DENIED-01 |
| FAM-SUBJECT | NORMAL | REPRESENTED | A5-SUBJ-NORMAL-01 |
| FAM-SUBJECT | DUPLICATE | REPRESENTED | A5-SUBJ-DUPLICATE-01 |
| FAM-SUBJECT | TIMEOUT | NOT_APPLICABLE | NA-NO-CONTRACT-DEADLINE |
| FAM-SUBJECT | OUT_OF_ORDER | REPRESENTED | A5-SUBJ-OUT_OF_ORDER-01 |
| FAM-SUBJECT | RECOVERY | REPRESENTED | A5-SUBJ-RECOVERY-01 |
| FAM-SUBJECT | PERMISSION_DENIED | REPRESENTED | A5-SUBJ-PERMISSION_DENIED-01 |
| FAM-ACTIVATION | NORMAL | REPRESENTED | A5-ACTIV-NORMAL-01 |
| FAM-ACTIVATION | DUPLICATE | REPRESENTED | A5-ACTIV-DUPLICATE-01 |
| FAM-ACTIVATION | TIMEOUT | REPRESENTED | A5-ACTIV-TIMEOUT-01 |
| FAM-ACTIVATION | OUT_OF_ORDER | REPRESENTED | A5-ACTIV-OUT_OF_ORDER-01 |
| FAM-ACTIVATION | RECOVERY | REPRESENTED | A5-ACTIV-RECOVERY-01 |
| FAM-ACTIVATION | PERMISSION_DENIED | REPRESENTED | A5-ACTIV-PERMISSION_DENIED-01 |
| FAM-COMMERCE | NORMAL | REPRESENTED | A5-COMM-NORMAL-01 |
| FAM-COMMERCE | DUPLICATE | REPRESENTED | A5-COMM-DUPLICATE-01 |
| FAM-COMMERCE | TIMEOUT | REPRESENTED | A5-COMM-TIMEOUT-01 |
| FAM-COMMERCE | OUT_OF_ORDER | REPRESENTED | A5-COMM-OUT_OF_ORDER-01 |
| FAM-COMMERCE | RECOVERY | REPRESENTED | A5-COMM-RECOVERY-01 |
| FAM-COMMERCE | PERMISSION_DENIED | REPRESENTED | A5-COMM-PERMISSION_DENIED-01 |
| FAM-IVR | NORMAL | REPRESENTED | A5-IVR-NORMAL-01 |
| FAM-IVR | DUPLICATE | REPRESENTED | A5-IVR-DUPLICATE-01 |
| FAM-IVR | TIMEOUT | REPRESENTED | A5-IVR-TIMEOUT-01 |
| FAM-IVR | OUT_OF_ORDER | REPRESENTED | A5-IVR-OUT_OF_ORDER-01 |
| FAM-IVR | RECOVERY | REPRESENTED | A5-IVR-RECOVERY-01 |
| FAM-IVR | PERMISSION_DENIED | REPRESENTED | A5-IVR-PERMISSION_DENIED-01 |
| FAM-MEDIA-EVIDENCE | NORMAL | REPRESENTED | A5-MEDIA-NORMAL-01 |
| FAM-MEDIA-EVIDENCE | DUPLICATE | REPRESENTED | A5-MEDIA-DUPLICATE-01 |
| FAM-MEDIA-EVIDENCE | TIMEOUT | REPRESENTED | A5-MEDIA-TIMEOUT-01 |
| FAM-MEDIA-EVIDENCE | OUT_OF_ORDER | REPRESENTED | A5-MEDIA-OUT_OF_ORDER-01 |
| FAM-MEDIA-EVIDENCE | RECOVERY | REPRESENTED | A5-MEDIA-RECOVERY-01 |
| FAM-MEDIA-EVIDENCE | PERMISSION_DENIED | REPRESENTED | A5-MEDIA-PERMISSION_DENIED-01 |
| FAM-SHARING | NORMAL | REPRESENTED | A5-SHARE-NORMAL-01 |
| FAM-SHARING | DUPLICATE | REPRESENTED | A5-SHARE-DUPLICATE-01 |
| FAM-SHARING | TIMEOUT | REPRESENTED | A5-SHARE-TIMEOUT-01 |
| FAM-SHARING | OUT_OF_ORDER | REPRESENTED | A5-SHARE-OUT_OF_ORDER-01 |
| FAM-SHARING | RECOVERY | REPRESENTED | A5-SHARE-RECOVERY-01 |
| FAM-SHARING | PERMISSION_DENIED | REPRESENTED | A5-SHARE-PERMISSION_DENIED-01 |
| FAM-SECURITY | NORMAL | REPRESENTED | A5-SEC-NORMAL-01 |
| FAM-SECURITY | DUPLICATE | REPRESENTED | A5-SEC-DUPLICATE-01 |
| FAM-SECURITY | TIMEOUT | REPRESENTED | A5-SEC-TIMEOUT-01 |
| FAM-SECURITY | OUT_OF_ORDER | REPRESENTED | A5-SEC-OUT_OF_ORDER-01 |
| FAM-SECURITY | RECOVERY | REPRESENTED | A5-SEC-RECOVERY-01 |
| FAM-SECURITY | PERMISSION_DENIED | REPRESENTED | A5-SEC-PERMISSION_DENIED-01 |
| FAM-DEPLOYMENT | NORMAL | REPRESENTED | A5-DEPLOY-NORMAL-01 |
| FAM-DEPLOYMENT | DUPLICATE | REPRESENTED | A5-DEPLOY-DUPLICATE-01 |
| FAM-DEPLOYMENT | TIMEOUT | NOT_APPLICABLE | NA-NO-CONTRACT-DEADLINE |
| FAM-DEPLOYMENT | OUT_OF_ORDER | NOT_APPLICABLE | NA-NO-ORDERED-FACT-STREAM |
| FAM-DEPLOYMENT | RECOVERY | REPRESENTED | A5-DEPLOY-RECOVERY-01 |
| FAM-DEPLOYMENT | PERMISSION_DENIED | NOT_APPLICABLE | NA-NO-AUTHZ-BOUNDARY |
| PATH-ACTIVATION-TRIAL | NORMAL | REPRESENTED | A5-X-ACTIV-NORMAL-01 |
| PATH-ACTIVATION-TRIAL | DUPLICATE | REPRESENTED | A5-X-ACTIV-DUPLICATE-01 |
| PATH-ACTIVATION-TRIAL | TIMEOUT | REPRESENTED | A5-X-ACTIV-TIMEOUT-01 |
| PATH-ACTIVATION-TRIAL | OUT_OF_ORDER | REPRESENTED | A5-X-ACTIV-OUT_OF_ORDER-01 |
| PATH-ACTIVATION-TRIAL | RECOVERY | REPRESENTED | A5-X-ACTIV-RECOVERY-01 |
| PATH-ACTIVATION-TRIAL | PERMISSION_DENIED | REPRESENTED | A5-X-ACTIV-PERMISSION_DENIED-01 |
| PATH-COMMERCE-ENTITLEMENT | NORMAL | REPRESENTED | A5-X-COMM-NORMAL-01 |
| PATH-COMMERCE-ENTITLEMENT | DUPLICATE | REPRESENTED | A5-X-COMM-DUPLICATE-01 |
| PATH-COMMERCE-ENTITLEMENT | TIMEOUT | REPRESENTED | A5-X-COMM-TIMEOUT-01 |
| PATH-COMMERCE-ENTITLEMENT | OUT_OF_ORDER | REPRESENTED | A5-X-COMM-OUT_OF_ORDER-01 |
| PATH-COMMERCE-ENTITLEMENT | RECOVERY | REPRESENTED | A5-X-COMM-RECOVERY-01 |
| PATH-COMMERCE-ENTITLEMENT | PERMISSION_DENIED | REPRESENTED | A5-X-COMM-PERMISSION_DENIED-01 |
| PATH-IVR-SETTLEMENT | NORMAL | REPRESENTED | A5-X-IVR-NORMAL-01 |
| PATH-IVR-SETTLEMENT | DUPLICATE | REPRESENTED | A5-X-IVR-DUPLICATE-01 |
| PATH-IVR-SETTLEMENT | TIMEOUT | REPRESENTED | A5-X-IVR-TIMEOUT-01 |
| PATH-IVR-SETTLEMENT | OUT_OF_ORDER | REPRESENTED | A5-X-IVR-OUT_OF_ORDER-01 |
| PATH-IVR-SETTLEMENT | RECOVERY | REPRESENTED | A5-X-IVR-RECOVERY-01 |
| PATH-IVR-SETTLEMENT | PERMISSION_DENIED | REPRESENTED | A5-X-IVR-PERMISSION_DENIED-01 |
| PATH-RECORDING-EVIDENCE | NORMAL | REPRESENTED | A5-X-MEDIA-NORMAL-01 |
| PATH-RECORDING-EVIDENCE | DUPLICATE | REPRESENTED | A5-X-MEDIA-DUPLICATE-01 |
| PATH-RECORDING-EVIDENCE | TIMEOUT | REPRESENTED | A5-X-MEDIA-TIMEOUT-01 |
| PATH-RECORDING-EVIDENCE | OUT_OF_ORDER | REPRESENTED | A5-X-MEDIA-OUT_OF_ORDER-01 |
| PATH-RECORDING-EVIDENCE | RECOVERY | REPRESENTED | A5-X-MEDIA-RECOVERY-01 |
| PATH-RECORDING-EVIDENCE | PERMISSION_DENIED | REPRESENTED | A5-X-MEDIA-PERMISSION_DENIED-01 |
| PATH-SHARE-ACCESS | NORMAL | REPRESENTED | A5-X-SHARE-NORMAL-01 |
| PATH-SHARE-ACCESS | DUPLICATE | REPRESENTED | A5-X-SHARE-DUPLICATE-01 |
| PATH-SHARE-ACCESS | TIMEOUT | REPRESENTED | A5-X-SHARE-TIMEOUT-01 |
| PATH-SHARE-ACCESS | OUT_OF_ORDER | REPRESENTED | A5-X-SHARE-OUT_OF_ORDER-01 |
| PATH-SHARE-ACCESS | RECOVERY | REPRESENTED | A5-X-SHARE-RECOVERY-01 |
| PATH-SHARE-ACCESS | PERMISSION_DENIED | REPRESENTED | A5-X-SHARE-PERMISSION_DENIED-01 |
| PATH-AUTH-AUDIT | NORMAL | REPRESENTED | A5-X-AUTH-NORMAL-01 |
| PATH-AUTH-AUDIT | DUPLICATE | REPRESENTED | A5-X-AUTH-DUPLICATE-01 |
| PATH-AUTH-AUDIT | TIMEOUT | REPRESENTED | A5-X-AUTH-TIMEOUT-01 |
| PATH-AUTH-AUDIT | OUT_OF_ORDER | REPRESENTED | A5-X-AUTH-OUT_OF_ORDER-01 |
| PATH-AUTH-AUDIT | RECOVERY | REPRESENTED | A5-X-AUTH-RECOVERY-01 |
| PATH-AUTH-AUDIT | PERMISSION_DENIED | REPRESENTED | A5-X-AUTH-PERMISSION_DENIED-01 |
| PATH-DEPLOY-ACCEPTANCE | NORMAL | REPRESENTED | A5-X-DEPLOY-NORMAL-01 |
| PATH-DEPLOY-ACCEPTANCE | DUPLICATE | REPRESENTED | A5-X-DEPLOY-DUPLICATE-01 |
| PATH-DEPLOY-ACCEPTANCE | TIMEOUT | NOT_APPLICABLE | NA-NO-CONTRACT-DEADLINE |
| PATH-DEPLOY-ACCEPTANCE | OUT_OF_ORDER | NOT_APPLICABLE | NA-NO-ORDERED-FACT-STREAM |
| PATH-DEPLOY-ACCEPTANCE | RECOVERY | REPRESENTED | A5-X-DEPLOY-RECOVERY-01 |
| PATH-DEPLOY-ACCEPTANCE | PERMISSION_DENIED | REPRESENTED | A5-X-DEPLOY-PERMISSION_DENIED-01 |
<!-- acceptance-coverage-map:end -->

基线：90 REPRESENTED + 6 NOT_APPLICABLE + 0 DEFERRED。任何缺失、重复、非法 applicability 或 count-only 检查失败。

## 6. Scenario 契约与语义规则

每个 represented cell 恰有一个 stable scenario。scenario 必填 root `requiredEvidenceTiers`（唯一 tier 权威）、`sourceRefs`、`outcomeRefs`、preconditions、stimulus、expected、terminal absorption、idempotency、effect ledger、evidence expectation、cleanup、evidenceOwner、resultSemantics。状态枚举、edge、Problem code、event、audit code、capability/formula ID 全部由 A0-A4 resolver 提供。

- `transitionRef` 只允许 A0 `A0:SM:<aggregate>:<from>-><to>`（frozen parser 的真实 edge）或 A2 structured transition；`A0:TX:*` 只能作 basis/source。
- derived state（如 Trial `EXPIRED`）不是 mutable state；不得作为 `A0:SM` value 或 endpoint。
- `expected.external` 是最终 external projection 的唯一权威；最后一个匹配 step response 必须与 root 一致。
- duplicate/retry 不得新增成功业务事实；permission-denied 除 allowlist 外不得写业务表；out-of-order 不得二次 settlement；timeout 只引用 policy input；recovery 必须引用 authoritative reconciliation/restart/restore 契约。
- `docs/04`、prototype、screenshot、`shared/onecall-business.js` 不是 A5 source/evidence。

Scenario index：

<!-- acceptance-scenario-index:start -->
| Scenario | Unit | Axis | Title | Owner | Evidence owner | Aggregate ref | Tiers |
| --- | --- | --- | --- | --- | --- | --- | --- |
| A5-IDENT-NORMAL-01 | FAM-IDENTITY | NORMAL | Login identity resolves to exactly one account and usable workspace | customer-iam | customer-iam | A0:SM:Customer login identity | INTEGRATION |
| A5-IDENT-DUPLICATE-01 | FAM-IDENTITY | DUPLICATE | Duplicate provider identity bind returns the first account without a second identity | customer-iam | customer-iam | A0:SM:Customer login identity | INTEGRATION |
| A5-IDENT-TIMEOUT-01 | FAM-IDENTITY | TIMEOUT | Expired session is rejected using only the Q1/Q19 TTL policy input | customer-iam | customer-iam | A0:SM:Customer login identity | INTEGRATION |
| A5-IDENT-OUT_OF_ORDER-01 | FAM-IDENTITY | OUT_OF_ORDER | Identity bind and account disable serialize fail-closed in either order | customer-iam | customer-iam | A0:SM:Customer account | INTEGRATION |
| A5-IDENT-RECOVERY-01 | FAM-IDENTITY | RECOVERY | Operations re-enable appends one transition and does not restore the old session | customer-iam | customer-iam | A0:SM:Customer account | INTEGRATION |
| A5-IDENT-PERMISSION_DENIED-01 | FAM-IDENTITY | PERMISSION_DENIED | Cross-identity or cross-workspace access is denied with zero business writes | customer-iam | customer-iam | A0:SM:Customer login identity | INTEGRATION |
| A5-SUBJ-NORMAL-01 | FAM-SUBJECT | NORMAL | Verified personal claim appends ACCEPTED and sets owner only from null | subject-number | subject-number | A0:SM:Subject ownership | INTEGRATION |
| A5-SUBJ-DUPLICATE-01 | FAM-SUBJECT | DUPLICATE | Same claim request replays the first outcome while changed context conflicts | subject-number | subject-number | A0:SM:Subject ownership | INTEGRATION |
| A5-SUBJ-OUT_OF_ORDER-01 | FAM-SUBJECT | OUT_OF_ORDER | Open risk preempts a replay-success check and blocks the claim | subject-number | subject-number | A0:SM:Subject claim | INTEGRATION |
| A5-SUBJ-RECOVERY-01 | FAM-SUBJECT | RECOVERY | Risk clear appends a compliant fact without transferring ownership | subject-number | subject-number | A0:SM:Subject risk state | INTEGRATION |
| A5-SUBJ-PERMISSION_DENIED-01 | FAM-SUBJECT | PERMISSION_DENIED | Unapproved organization ownership or a different owner fails closed with zero owner change | subject-number | subject-number | A0:SM:Subject ownership | INTEGRATION |
| A5-ACTIV-NORMAL-01 | FAM-ACTIVATION | NORMAL | Explicit ACCEPT writes consent, activation transition, first trial, grant, balance and Outbox | activation-consent | activation-consent | A0:SM:Service activation | INTEGRATION |
| A5-ACTIV-DUPLICATE-01 | FAM-ACTIVATION | DUPLICATE | Activation event replay returns the first result without a second trial | activation-consent | activation-consent | A0:SM:Service activation | INTEGRATION |
| A5-ACTIV-TIMEOUT-01 | FAM-ACTIVATION | TIMEOUT | Trial policy validity and expiry are policy inputs and never reissue a trial | activation-consent | activation-consent | A0:SM:Trial | INTEGRATION |
| A5-ACTIV-OUT_OF_ORDER-01 | FAM-ACTIVATION | OUT_OF_ORDER | Confirmation then suspend never rewrites a confirmed session as rejected | activation-consent | activation-consent | A0:SM:Service activation | INTEGRATION |
| A5-ACTIV-RECOVERY-01 | FAM-ACTIVATION | RECOVERY | Operations resume appends SUSPENDED to ACTIVE and never reissues a trial | activation-consent | activation-consent | A0:SM:Service activation | INTEGRATION |
| A5-ACTIV-PERMISSION_DENIED-01 | FAM-ACTIVATION | PERMISSION_DENIED | Fresh suspended confirmation returns REJECT_SUSPENDED with no accepted consent or trial | activation-consent | activation-consent | A0:SM:Service activation | INTEGRATION |
| A5-COMM-NORMAL-01 | FAM-COMMERCE | NORMAL | Order to verified payment to issuance writes grant, balance and Outbox once | catalog-order | catalog-order | A0:SM:Customer order | INTEGRATION |
| A5-COMM-DUPLICATE-01 | FAM-COMMERCE | DUPLICATE | Duplicate order callback returns the first result without a second entitlement | catalog-order | catalog-order | A0:SM:Customer order | INTEGRATION |
| A5-COMM-TIMEOUT-01 | FAM-COMMERCE | TIMEOUT | Unknown payment stays UNKNOWN and blocks a second checkout | catalog-order | catalog-order | A0:SM:Customer order | INTEGRATION |
| A5-COMM-OUT_OF_ORDER-01 | FAM-COMMERCE | OUT_OF_ORDER | Late or duplicate payment events never produce a second grant | catalog-order | catalog-order | A0:SM:Customer order | INTEGRATION |
| A5-COMM-RECOVERY-01 | FAM-COMMERCE | RECOVERY | FAILED issuance retries only through FAILED to PENDING without a duplicate grant | catalog-order | catalog-order | A0:SM:Entitlement issuance | INTEGRATION |
| A5-COMM-PERMISSION_DENIED-01 | FAM-COMMERCE | PERMISSION_DENIED | Missing consent, real-name or active activation rejects order creation with zero orders | catalog-order | catalog-order | A0:SM:Customer order | INTEGRATION |
| A5-IVR-NORMAL-01 | FAM-IVR | NORMAL | Accepted terminal settles one usage fact and one Outbox fact | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-IVR-DUPLICATE-01 | FAM-IVR | DUPLICATE | Duplicate terminal event is absorbed without a second reservation or settlement | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-IVR-TIMEOUT-01 | FAM-IVR | TIMEOUT | Authorization expiry before start releases once and never redials | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-IVR-OUT_OF_ORDER-01 | FAM-IVR | OUT_OF_ORDER | Late consistent leg facts after terminal are ignored and contradictory facts quarantine | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-IVR-RECOVERY-01 | FAM-IVR | RECOVERY | IVR restart and reconciliation converge UNKNOWN without redial or double settlement | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-IVR-PERMISSION_DENIED-01 | FAM-IVR | PERMISSION_DENIED | Bad signature, nonce replay or cross-workspace call is rejected with zero session, call or reservation | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-MEDIA-NORMAL-01 | FAM-MEDIA-EVIDENCE | NORMAL | Declare, server observation and complete produce VALIDATED with server facts winning | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-MEDIA-DUPLICATE-01 | FAM-MEDIA-EVIDENCE | DUPLICATE | Declaration and completion retries return the first result without a second recording or media object | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-MEDIA-TIMEOUT-01 | FAM-MEDIA-EVIDENCE | TIMEOUT | Completion after upload expiry quarantines without a successful media or evidence fact | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-MEDIA-OUT_OF_ORDER-01 | FAM-MEDIA-EVIDENCE | OUT_OF_ORDER | Completion before server observation returns UPLOAD_OBSERVATION_REQUIRED and converges after observation | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-MEDIA-RECOVERY-01 | FAM-MEDIA-EVIDENCE | RECOVERY | STORAGE_UNKNOWN retries under the same identity and converges to VALIDATED without fabricating media facts | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-MEDIA-PERMISSION_DENIED-01 | FAM-MEDIA-EVIDENCE | PERMISSION_DENIED | Upload and playback credential scope violations are denied with no permanent URL or cross-object access | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-SHARE-NORMAL-01 | FAM-SHARING | NORMAL | Grant, admission, ticket and playback count the admission exactly once | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-SHARE-DUPLICATE-01 | FAM-SHARING | DUPLICATE | Same admission key returns the original admission and reissues a ticket without incrementing | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-SHARE-TIMEOUT-01 | FAM-SHARING | TIMEOUT | Grant or session expiry blocks new admission while the recorded window stays bounded | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-SHARE-OUT_OF_ORDER-01 | FAM-SHARING | OUT_OF_ORDER | Admission commits before Redis ticket derivation and a Redis failure does not recount | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-SHARE-RECOVERY-01 | FAM-SHARING | RECOVERY | Lost Redis ticket reconstruction compares generation and expiry witnesses and never revives a revoked grant | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-SHARE-PERMISSION_DENIED-01 | FAM-SHARING | PERMISSION_DENIED | Wrong tenant, failed verification or revoked generation is denied with zero new admission | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-SEC-NORMAL-01 | FAM-SECURITY | NORMAL | Five-domain authentication, least privilege, high-risk audit and redaction work together | Security/Backend | Security/Backend | A0:SM:Idempotency record | INTEGRATION |
| A5-SEC-DUPLICATE-01 | FAM-SECURITY | DUPLICATE | Nonce or provider replay returns the first result or rejects without duplicate effects | Security/Backend | Security/Backend | A0:SM:Idempotency record | INTEGRATION |
| A5-SEC-TIMEOUT-01 | FAM-SECURITY | TIMEOUT | Signature timestamp outside the Q25 window fails closed before side effects | Security/Backend | Security/Backend | A0:SM:Idempotency record | INTEGRATION |
| A5-SEC-OUT_OF_ORDER-01 | FAM-SECURITY | OUT_OF_ORDER | Verification precedes every domain side effect and audit failure rolls the transaction back | Security/Backend | Security/Backend | A0:SM:Idempotency record | INTEGRATION |
| A5-SEC-RECOVERY-01 | FAM-SECURITY | RECOVERY | Key rotation and dependency recovery permit a safe same-identity retry without a second business fact | Security/Backend | Security/Backend | A0:SM:Idempotency record | INTEGRATION+CLOUD |
| A5-SEC-PERMISSION_DENIED-01 | FAM-SECURITY | PERMISSION_DENIED | Cross-domain token, wrong role or forbidden audit read is denied with zero business writes | Security/Backend | Security/Backend | A0:SM:Idempotency record | INTEGRATION |
| A5-DEPLOY-NORMAL-01 | FAM-DEPLOYMENT | NORMAL | Missing A4 evidence keeps groups INCOMPLETE and root readiness constants false | Infrastructure/Security | Infrastructure/Security | A4:SECTION:8.3 | INTEGRATION+CLOUD |
| A5-DEPLOY-DUPLICATE-01 | FAM-DEPLOYMENT | DUPLICATE | Duplicate capability or backup dependency is INVALID and never hides a missing item | Infrastructure/Security | Infrastructure/Security | A4:SECTION:9.1 | INTEGRATION+CLOUD |
| A5-DEPLOY-RECOVERY-01 | FAM-DEPLOYMENT | RECOVERY | Missing or over-target restore observations stay UNKNOWN/INCOMPLETE/NOT_MET without a recovery claim | Infrastructure/Security | Infrastructure/Security | A4:SECTION:9.3 | INTEGRATION+CLOUD+MANUAL |
| A5-X-ACTIV-NORMAL-01 | PATH-ACTIVATION-TRIAL | NORMAL | IVR confirm to consent, activation, trial, grant, balance and Outbox converges once | activation-consent | activation-consent | A0:SM:Service activation | INTEGRATION+LINE |
| A5-X-ACTIV-DUPLICATE-01 | PATH-ACTIVATION-TRIAL | DUPLICATE | Same event and fingerprint confirmation replay never reissues a trial or a second transition | activation-consent | activation-consent | A0:SM:Service activation | INTEGRATION+LINE |
| A5-X-ACTIV-TIMEOUT-01 | PATH-ACTIVATION-TRIAL | TIMEOUT | Authorization window and trial expiry follow policy inputs and never reissue a trial | activation-consent | activation-consent | A0:SM:Trial | INTEGRATION+LINE |
| A5-X-ACTIV-OUT_OF_ORDER-01 | PATH-ACTIVATION-TRIAL | OUT_OF_ORDER | Confirmation and suspend ordering never rewrites a CONFIRMED session as REJECTED | activation-consent | activation-consent | A0:SM:Service activation | INTEGRATION+LINE |
| A5-X-ACTIV-RECOVERY-01 | PATH-ACTIVATION-TRIAL | RECOVERY | Consent-gap recovery and operations resume keep activation and trial consistent without reissuing | activation-consent | activation-consent | A0:SM:Service activation | INTEGRATION+LINE |
| A5-X-ACTIV-PERMISSION_DENIED-01 | PATH-ACTIVATION-TRIAL | PERMISSION_DENIED | Suspended, wrong-role or wrong-workspace confirmation has no activation, trial or success Outbox fact | activation-consent | activation-consent | A0:SM:Service activation | INTEGRATION+LINE |
| A5-X-COMM-NORMAL-01 | PATH-COMMERCE-ENTITLEMENT | NORMAL | Provider callback to payment, order, issuance, grant, balance and Outbox converges once | catalog-order | catalog-order | A0:SM:Customer order | INTEGRATION |
| A5-X-COMM-DUPLICATE-01 | PATH-COMMERCE-ENTITLEMENT | DUPLICATE | Same provider event and payload replays idempotently and never issues a second entitlement | catalog-order | catalog-order | A0:SM:Customer order | INTEGRATION |
| A5-X-COMM-TIMEOUT-01 | PATH-COMMERCE-ENTITLEMENT | TIMEOUT | UNKNOWN payment remains occupied and queries the original order instead of creating a second order | catalog-order | catalog-order | A0:SM:Customer order | INTEGRATION |
| A5-X-COMM-OUT_OF_ORDER-01 | PATH-COMMERCE-ENTITLEMENT | OUT_OF_ORDER | Out-of-order payment and issuance never produce a second grant and expose partial line failure | catalog-order | catalog-order | A0:SM:Customer order | INTEGRATION |
| A5-X-COMM-RECOVERY-01 | PATH-COMMERCE-ENTITLEMENT | RECOVERY | FAILED issuance retries without a duplicate grant and keeps the Outbox identity stable | catalog-order | catalog-order | A0:SM:Entitlement issuance | INTEGRATION |
| A5-X-COMM-PERMISSION_DENIED-01 | PATH-COMMERCE-ENTITLEMENT | PERMISSION_DENIED | Missing real-name, active activation or purchase consent yields zero orders and zero grants | catalog-order | catalog-order | A0:SM:Customer order | INTEGRATION |
| A5-X-IVR-NORMAL-01 | PATH-IVR-SETTLEMENT | NORMAL | Task claim, reservation, terminal, usage fact and Outbox converge exactly once | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-X-IVR-DUPLICATE-01 | PATH-IVR-SETTLEMENT | DUPLICATE | Duplicate terminal and response-loss retry never create a second reservation or settlement | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-X-IVR-TIMEOUT-01 | PATH-IVR-SETTLEMENT | TIMEOUT | Authorization expiry before dispatch releases once and OUTBOUND_NOT_STARTED is not a redial | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-X-IVR-OUT_OF_ORDER-01 | PATH-IVR-SETTLEMENT | OUT_OF_ORDER | Terminal first and late facts converge without a second settlement or release | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-X-IVR-RECOVERY-01 | PATH-IVR-SETTLEMENT | RECOVERY | Restart and reconciliation converge UNKNOWN without redial or double settlement | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-X-IVR-PERMISSION_DENIED-01 | PATH-IVR-SETTLEMENT | PERMISSION_DENIED | Signature, replay, workspace and active-call denials produce zero claim, reservation or settlement | ivr | ivr | A2:STATE:call_record | INTEGRATION+LINE |
| A5-X-MEDIA-NORMAL-01 | PATH-RECORDING-EVIDENCE | NORMAL | Recording validation, media object, evidence record, creation event and artifact converge once | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-X-MEDIA-DUPLICATE-01 | PATH-RECORDING-EVIDENCE | DUPLICATE | Declaration, completion and evidence creation replay never create a second recording, media or evidence | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-X-MEDIA-TIMEOUT-01 | PATH-RECORDING-EVIDENCE | TIMEOUT | Upload expiry quarantines the recording and leaves evidence non-solid | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-X-MEDIA-OUT_OF_ORDER-01 | PATH-RECORDING-EVIDENCE | OUT_OF_ORDER | Completion before observation and observation after upload converge without premature evidence | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-X-MEDIA-RECOVERY-01 | PATH-RECORDING-EVIDENCE | RECOVERY | STORAGE_UNKNOWN retries under the same identity and evidence is created only after VALIDATED | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-X-MEDIA-PERMISSION_DENIED-01 | PATH-RECORDING-EVIDENCE | PERMISSION_DENIED | Upload, playback or object scope denial produces zero evidence or share success facts | evidence | evidence | A0:SM:Recording | INTEGRATION+CLOUD |
| A5-X-SHARE-NORMAL-01 | PATH-SHARE-ACCESS | NORMAL | Grant, admission, ticket, playback and access event converge once | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-X-SHARE-DUPLICATE-01 | PATH-SHARE-ACCESS | DUPLICATE | Same admission key returns the original admission and ticket without recounting | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-X-SHARE-TIMEOUT-01 | PATH-SHARE-ACCESS | TIMEOUT | Share and session expiry block new or renewed access while the recorded window stays bounded | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-X-SHARE-OUT_OF_ORDER-01 | PATH-SHARE-ACCESS | OUT_OF_ORDER | Admission commit precedes ticket issue and Redis failure does not recount | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-X-SHARE-RECOVERY-01 | PATH-SHARE-ACCESS | RECOVERY | Ticket reconstruction and generation witness reject stale admissions | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-X-SHARE-PERMISSION_DENIED-01 | PATH-SHARE-ACCESS | PERMISSION_DENIED | Wrong tenant, verification failure or revocation denial creates zero new admissions | sharing | sharing | A0:SM:Share grant | INTEGRATION+CLOUD |
| A5-X-AUTH-NORMAL-01 | PATH-AUTH-AUDIT | NORMAL | Five-domain auth to domain mutation to append-only audit, high-risk and redaction converges once | platform-reliability | platform-reliability | A0:SM:Idempotency record | INTEGRATION |
| A5-X-AUTH-DUPLICATE-01 | PATH-AUTH-AUDIT | DUPLICATE | Nonce or provider duplicate returns the first result and never duplicates audit | platform-reliability | platform-reliability | A0:SM:Idempotency record | INTEGRATION |
| A5-X-AUTH-TIMEOUT-01 | PATH-AUTH-AUDIT | TIMEOUT | Timestamp outside the Q25 window is rejected with zero domain side effects | platform-reliability | platform-reliability | A0:SM:Idempotency record | INTEGRATION |
| A5-X-AUTH-OUT_OF_ORDER-01 | PATH-AUTH-AUDIT | OUT_OF_ORDER | Auth failure precedes every side effect and an audit transaction failure rolls back | platform-reliability | platform-reliability | A0:SM:Idempotency record | INTEGRATION |
| A5-X-AUTH-RECOVERY-01 | PATH-AUTH-AUDIT | RECOVERY | Key rotation and Redis outage recovery retry the same identity with continuous audit | platform-reliability | platform-reliability | A0:SM:Idempotency record | INTEGRATION+CLOUD |
| A5-X-AUTH-PERMISSION_DENIED-01 | PATH-AUTH-AUDIT | PERMISSION_DENIED | Cross-domain token, wrong role or forbidden audit read has zero business writes | platform-reliability | platform-reliability | A0:SM:Idempotency record | INTEGRATION |
| A5-X-DEPLOY-NORMAL-01 | PATH-DEPLOY-ACCEPTANCE | NORMAL | A4 capability evidence crossing the A5 boundary can only be BLOCKED and never raises readiness | Infrastructure/Security | Infrastructure/Security | A4:SECTION:10.2 | INTEGRATION+CLOUD+MANUAL |
| A5-X-DEPLOY-DUPLICATE-01 | PATH-DEPLOY-ACCEPTANCE | DUPLICATE | Replayed A4 evidence is admitted once by digest and a changed digest is an immutability violation | Infrastructure/Security | Infrastructure/Security | A4:SECTION:10.3 | INTEGRATION+CLOUD+MANUAL |
| A5-X-DEPLOY-RECOVERY-01 | PATH-DEPLOY-ACCEPTANCE | RECOVERY | Missing CLOUD or MANUAL restore closure evidence stays EVIDENCE_VALID or BLOCKED without a restore claim | Infrastructure/Security | Infrastructure/Security | A4:SECTION:10.4 | INTEGRATION+CLOUD+MANUAL |
| A5-X-DEPLOY-PERMISSION_DENIED-01 | PATH-DEPLOY-ACCEPTANCE | PERMISSION_DENIED | Caller-forged acceptance, synthetic capability or unknown authority is rejected at the A5 boundary | Infrastructure/Security | Infrastructure/Security | A4:SECTION:10.2 | INTEGRATION+CLOUD+MANUAL |
<!-- acceptance-scenario-index:end -->

## 7. Owner、execution tier 与 result semantics

`owners[]` 是封闭 A5 验收责任 registry，不是 production IAM role。MODULE owner 精确来自 A0 12 个 owning module；RESPONSIBILITY 必须由上游 whole-cell owner 文本支撑；`A5-UPSTREAM-DELEGATED` 只用于原样委托的 residual，不作 scenario owner。slash 组件猜测、substring 匹配和自由文本 owner 一律失败。

Execution tiers 是精确集合，不是线性等级假设：`STATIC_SPEC`、`MODEL_FIXTURE`、`INTEGRATION`、`LINE`、`CLOUD`、`MANUAL`。静态/model fixture 只能产出 `SPEC_VERIFIED`，不能替代任何 runtime tier。

Result partition：specification 为 `SPEC_DEFINED` 或 `SPEC_INVALID`；execution 在 S1 全部为 `NOT_RUN`；accepted 在 authorized acceptance record 出现前恒 false。`PASS` 是验收结果，不是 manifest observation。

## 8. 验收证据状态机与 fail-closed

<!-- acceptance-evidence-state-map:start -->
| From | To | Condition |
| --- | --- | --- |
| NOT_RUN | EVIDENCE_SUBMITTED | manifest exists and schema/digest/owner/tier/time are structurally valid |
| EVIDENCE_SUBMITTED | EVIDENCE_REJECTED | synthetic runtime, caller result, tier downgrade, owner mismatch or invalid artifact |
| EVIDENCE_SUBMITTED | EVIDENCE_VALID | every required tier and artifact is admissible; no contract contradiction |
| EVIDENCE_SUBMITTED | BLOCKED | evidence is valid but a required tier/environment/dependency is missing |
| EVIDENCE_VALID | PASS | an independent acceptance record references manifestDigest and an approved authority |
| EVIDENCE_VALID | FAIL | an acceptance record explicitly fails with complete failure evidence |
| PASS | PASS | terminal and immutable; conflicting content is rejected |
| FAIL | FAIL | terminal and immutable; conflicting content is rejected |
| ANY | NOT_APPLICABLE | only a registered coverage N/A row may declare it |
<!-- acceptance-evidence-state-map:end -->

Aggregate gates：

<!-- acceptance-gate-map:start -->
| Gate | Expression |
| --- | --- |
| COVERAGE_COMPLETE | 96 cells, 0 missing/duplicate/illegal, 0 deferred, 90 represented, 6 N/A |
| SOURCE_INTEGRITY | all refs resolve; 0 unresolved/ambiguous/drift; A3 residual=18; A0 state rows=31 |
| SCENARIO_SEMANTICS | state/edge, terminal absorption, idempotency, effects, denial, ordering, cleanup, projection all pass |
| EVIDENCE_HONESTY | single root tier; evidence owners resolve; every runtime result is NOT_RUN; no caller/synthetic upgrade |
| DOC_PARITY | all eight marker blocks match the model row-for-row |
| REGISTRY_BASELINE | A0-A4 counts/digests/ID sets equal the frozen source baseline |
<!-- acceptance-gate-map:end -->

`specificationCoverageComplete` 是上述 gates 的 AND，不是 96 行计数的别名。`productionAccepted` 是 validator 输出，不是 model 输入；S1 没有 authority registry，因此 PASS/production acceptance 不可达。任何 caller 字段、synthetic runtime、tier downgrade、count-only 或 readiness flag 都被拒绝。

`A5_COUNT_ONLY_COVERAGE` 是 caller aggregate-coverage guard：model root 出现 `specificationCoverageComplete` 或 `specCoverageComplete`（调用方/模型自带的聚合覆盖断言）即拒绝，且不再叠加 `A5_SECTION_EXTRA`。该 guard 只拒绝这种 root 捷径，不替代任何 root/ID/ref/owner/coverage/semantic/doc/baseline 检查；`all-counts-but-unbacked-na`（S1）与 count-only acceptance（S2）继续证明计数不能绕过底层语义/证据 gate。

## 9. 执行证据准入、聚合结果与不可达 PASS（S2）

S2 只做静态证据准入：证明 caller 断言、synthetic runtime、tier downgrade、count-only 完整性、伪造 authority、坏 digest/时间、状态矛盾与 replay/immutability 违规都不能变成 `PASS` 或 `productionAccepted=true`。不执行真实系统，不存储证据。

### 9.1 `evidencePolicy` 与空 `authorityRegistry`

模型 root additive 字段：

- `evidencePolicy`：冻结的 manifest / acceptance record / authority 契约元数据。`evidenceClasses` 恰为 `STATIC_SPEC`、`MODEL_FIXTURE`、`SYNTHETIC`、`RUNTIME`；`specOnlyTiers` 为 `STATIC_SPEC`、`MODEL_FIXTURE`；`runtimeTiers` 为 `INTEGRATION`、`LINE`、`CLOUD`、`MANUAL`；`maximumAdmittedState=EVIDENCE_VALID`；`unreachableResidual=A5-RES-02`。
- `authorityRegistry`：**必须为空数组**。这是 controller-accepted OQ-03 决定，不是 placeholder。没有 `APPROVED` 外部 authority 时 `PASS`/`productionAccepted` 故意不可达；任何非空 row（即使自带 basis）都按 `A5_ACCEPTANCE_AUTHORITY_INVALID` 拒绝，不得伪造 authority row。
- `STATIC_SPEC`/`MODEL_FIXTURE`/`SYNTHETIC` 最多证明 `SPEC_VERIFIED`，不能替代任何 runtime tier。`RUNTIME` manifest 也只能到 `EVIDENCE_VALID`；`PASS` 需要独立 acceptance record + approved authority。

### 9.2 EvidenceArtifact / EvidenceManifest / AcceptanceRecord

EvidenceArtifact（`evidenceRef` object）：

```text
{ artifactId, role, locatorRef, sha256, capturedAt, producerRef, redactionRef }
role ∈ { AUDIT_EXPORT, OUTBOX_EXPORT, DB_STATE_EXPORT, METRIC_SNAPSHOT,
         LOG_EXPORT, LINE_CDR, CLOUD_EVIDENCE, MANUAL_RECORD, TEST_REPORT }
sha256 = ^[0-9a-f]{64}$；capturedAt ∈ [executedAt, recordedAt]
locatorRef 为 opaque non-secret；禁止 URL/query/COS key/bucket/version/token/bearer/credential
producerRef/redactionRef 必须是可解析 source ref（例如 A3:SERVICE:core-api、A3:INV:INV-A3-15）；
自由文本、未识别前缀或解析不到 => A5_EVIDENCE_REF_INVALID
```

EvidenceManifest：

```text
{ manifestId, scenarioId, evidenceOwner, evidenceClass, evidenceTiers,
  executedAt, recordedAt, artifacts[], observations[], manifestDigest }
```

- `manifestDigest` 是排除自身后的 recursive sorted-key JSON 的 SHA-256；artifact digest 必须是 lowercase 64-hex。
- `executedAt <= recordedAt <= asOf`，`capturedAt` 落在 `[executedAt, recordedAt]`；未来时间拒绝。
- `manifestId`/`scenarioId` 为 closed 非空字符串；非字符串 identity 按 manifest invalid 拒绝。
- `evidenceOwner` 缺失或为 `null` => `A5_EVIDENCE_OWNER_MISSING`；present 但非字符串，或字符串不等于 scenario 的 `evidenceOwner` => `A5_EVIDENCE_OWNER_MISMATCH`（不做类型静默放行）。静态阶段没有 approved delegation。
- observations 的 `(targetRef, field)` 必须属于该 scenario 的 step `expectedState` 或 final `persisted` 断言；对无关 aggregate 的 state claim（即使该值在别处合法）=> `A5_EVIDENCE_STATE_CONTRADICTION`。
- manifest root 禁止 `result`、`pass`、`productionAccepted`、`execution`；`accepted` 单独按 caller-asserted PASS 拒绝。manifest 不能携带 `PASS` 结论。

AcceptanceRecord（独立记录，不由 manifest 自声明）：

```text
{ recordId, scenarioId, manifestId, manifestDigest, authorityRef, acceptedAt, result }
result ∈ { PASS, FAIL }；acceptedAt >= manifest.recordedAt 且 <= asOf
```

- `recordId`/`scenarioId`/`manifestId` 为 closed 非空字符串；`record.scenarioId` 必须等于所引用 manifest 的 `scenarioId`。scenario 不一致、或同一 `recordId` 对应不同 body => `A5_ACCEPTANCE_IMMUTABILITY_VIOLATION`。
- record 必须引用已准入 manifest 的同一 `manifestDigest`；没有独立 record 时 manifest 最多 `EVIDENCE_VALID`。
- 无 manifest、无 authority 或 authority 非 approved 时不得 `PASS`。`PASS`/`FAIL` 是终态；同 scenario 的冲突 record 或同 `manifestId` 的不同 digest 一律 immutability violation。
- 结构合法的 FAIL record 将对应已准入 manifest/scenario 转为终态 FAIL，root `runtime_evidence=FAIL`，且绝不会被 `EVIDENCE_VALID` roll-up 掩盖；不新增公开计数器。

### 9.3 准入与拒绝规则

| 规则 | 拒绝码 |
| --- | --- |
| manifest 携带 `result`/`pass`/`productionAccepted`/`execution` | `A5_CALLER_CONTROL_FORBIDDEN` |
| manifest 携带 `accepted` | `A5_CALLER_ASSERTED_PASS` |
| `evidenceClass=SYNTHETIC` 或 fixture-only 用于 runtime tier | `A5_SYNTHETIC_EVIDENCE_REJECTED` |
| `STATIC_SPEC`/`MODEL_FIXTURE` 不满足 required runtime tier；或缺任一 required tier | `A5_EVIDENCE_TIER_INSUFFICIENT` |
| evidenceOwner present 但非字符串，或字符串不等于 scenario evidenceOwner | `A5_EVIDENCE_OWNER_MISMATCH`；缺失或 `null` 为 `A5_EVIDENCE_OWNER_MISSING` |
| required artifact role 缺失 | `A5_EVIDENCE_ARTIFACT_MISSING` |
| artifact 字段/role/locatorRef/sha256 非法 | `A5_EVIDENCE_REF_INVALID` |
| manifestDigest 与 canonical JSON 不符 | `A5_EVIDENCE_DIGEST_MISMATCH` |
| 时间格式、顺序或未来时间非法 | `A5_EVIDENCE_TIME_INVALID` |
| observation 与 scenario final persisted/edge/终态矛盾，或 `(targetRef, field)` 不在 scenario step/final 断言内 | `A5_EVIDENCE_STATE_CONTRADICTION` |
| acceptance record 缺 authority/无 approved authority | `A5_ACCEPTANCE_AUTHORITY_MISSING` / `A5_ACCEPTANCE_AUTHORITY_INVALID` |
| acceptance record 无对应已准入 evidence | `A5_ACCEPTANCE_EVIDENCE_MISSING` |
| record `scenarioId` 与 manifest 不一致、reuse `recordId` 不同 body | `A5_ACCEPTANCE_IMMUTABILITY_VIOLATION` |
| 同 manifestId 不同 digest、record digest 不符或 PASS/FAIL 冲突 | `A5_ACCEPTANCE_IMMUTABILITY_VIOLATION` |

- `--evidence` 对 s2 可选；没有 ledger 时 `evidence_manifests=0`、`evidence_admitted=0`、`acceptance_records=0`、`blocked=0`、`runtime_evidence=NOT_RUN`。
- 当前 tier 不足的 manifest 状态为 `BLOCKED`，不是 PASS，也不是静默忽略；结构/伪造/矛盾证据状态为 `EVIDENCE_REJECTED`。
- 96 cells、90 scenarios、A2/A3/A4 fixture pass、docs 完整性、prototype QA 都不是 runtime evidence；count-only 不能产生 `EVIDENCE_VALID` 或 `productionAccepted`。

### 9.4 状态机与聚合推导

状态机沿用 §8（`NOT_RUN → EVIDENCE_SUBMITTED → EVIDENCE_REJECTED|EVIDENCE_VALID|BLOCKED`；`EVIDENCE_VALID → PASS|FAIL`）。静态 S2：

```text
runtimeEvidenceComplete = every represented scenario has PASS (or N/A) at required tiers
productionAccepted = runtimeEvidenceComplete
                     && approved authority for every represented scenario/path
                     && no caller control fields
```

- 聚合优先级保持不变并先处理坏证据：malformed/rejected evidence => `EVIDENCE_REJECTED`；blocked evidence => `BLOCKED`；随后若存在结构合法的终态 FAIL => `FAIL`；全部 represented 已准入 => `EVIDENCE_VALID`；否则 `EVIDENCE_SUBMITTED`。`FAIL` 复用现有 summary 字段，不新增公开计数器。
- 空 `authorityRegistry` 使 `runtimeEvidenceComplete` 与 `productionAccepted` 不可达；S2 输出恒为 `production_accepted=false`。
- `PASS` 不由 manifest observation、fixture 通过或 `SPEC_VERIFIED` 自声明。

### 9.5 对抗 fixture

`acceptance/fixtures/s2-01-evidence-admission.json`（42 个聚焦 case，exact primary-code 集合）：caller-forged PASS、caller accepted、synthetic runtime、model-fixture-as-runtime、missing LINE tier、bad artifact digest、manifest digest mismatch、future timestamp、owner mismatch、owner non-string number/object、no authority、synthetic authority、authority row without basis、accept without evidence、state contradiction、out-of-scope observation、manifest/record immutability、record digest mismatch、record scenario mismatch（FAIL/PASS）、duplicate recordId、valid FAIL record（`runtime_evidence=FAIL`）、manifest replay same digest（正向）、count-only acceptance、positive MODEL_FIXTURE（仅准入，不 PASS）、positive RUNTIME（仅 `EVIDENCE_VALID`）、missing artifact、invalid locator（含 bucket/version/bare-token/bearer）、missing artifact field、time order、missing owner、invalid manifest shape、producer/redaction free text、unrecognized prefix、unresolved ref。fixture 文件见 [s2-01-evidence-admission.json](../acceptance/fixtures/s2-01-evidence-admission.json)，冻结计划见 [A5 plan](gateflow/a5-production-acceptance-matrix-spec-plan.md)。

正向 control 只证明结构可准入；它们不改变真实矩阵、`authorityRegistry` 仍为空、`productionAccepted=false`。

## 10. 验证命令与诚实限制

从仓库根执行：

```bash
node --check acceptance/validate.mjs
node acceptance/validate.mjs --stage s1 \
  --matrix acceptance/v1/acceptance-matrix-v1.json \
  --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json \
  --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs \
  --a4-fixtures deployment/capacity/fixtures
node acceptance/validate.mjs --stage s2 \
  --matrix acceptance/v1/acceptance-matrix-v1.json \
  --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json \
  --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs \
  --a4-fixtures deployment/capacity/fixtures \
  --check-doc-links
```

`--stage s2` 额外接受可选 `--evidence <ledger.json>`（无 ledger 时 evidence counters 为零、`runtime_evidence=NOT_RUN`）；`--check-doc-links` 只检查本文件相对链接与 `docs/README.md` 的 docs/19 行唯一性，不访问网络。`--evidence`/`--check-doc-links` 与 s1 组合是 invalid-arguments。

A1 bundle 是未跟踪生成物，唯一命令是 `( cd api/openapi && npm ci && npm run check:s3 )`。离线环境无法生成 bundle 时必须报告环境限制，不得用 fake bundle 绕过。validator 不读 network、环境变量、wall clock 或 random，不写文件。

Determinism 由 CLI 自身验证，而不是硬编码：顶层调用先用 `process.execPath` 和当前 validator entry path 启动两次隔离子进程，参数与顶层完全相同并追加内部 `--determinism-probe`，保持 cwd，不读 env/wall clock/network/random。每次 probe 走完整 validation path（全部输入、A0-A4 context、fixtures、evidence 与 `--check-doc-links`），成功时只输出一个非最终标记 `deterministic=probe`，永不输出 `deterministic=true`。父进程逐字节比较两次 exit status/stdout/stderr；launch 失败、signal/status 不可用、输出不一致，或成功输出不含恰好一个 probe 标记时，fail closed 为 `A5_DETERMINISM_FAILURE`（稳定 path `determinism.probe`、id `two-probe`，exit 1，不输出 PASS）。两次一致且失败时原样转发第一次失败；两次一致且成功时仅把唯一 probe 标记替换为 `deterministic=true` 并保持 exit status。该检查只证明同一输入下 validator 输出确定，不证明任何外部 runtime 行为。

A5 validator 证明矩阵覆盖完整、来源可解析、expectation 与 accepted state/effect 语义不矛盾、静态/model 证据不能升级为 runtime PASS；**不证明**真实 DB/锁/事务、Redis/RabbitMQ/COS/KMS/CAM、支付、IVR 线路/B-leg/录音、跨节点 callId、真实恢复、SLO/RPO/RTO、容量/云能力、安全渗透、脱敏配置或任何 runtime 行为。

## 11. Residual risks

<!-- acceptance-residual-map:start -->
| Residual | Owner | Destination | Upstream refs |
| --- | --- | --- | --- |
| A5-RES-01 | Infrastructure/Security | evidence platform + launch governance | A0:Q:Q3, A0:Q:Q13, A3:RESIDUAL:R-A3-COS-CAPABILITY, A3:RESIDUAL:R-A3-LEGAL-RETENTION, A4:SECTION:10.3 |
| A5-RES-02 | Product/Security/Operations | release governance; PASS 在未来授权 authority 前不可达 | A3:RESIDUAL:R-A3-SUPPORT-VIEW, A4:SECTION:10.3 |
| A5-RES-03 | Telephony/IVR | authorized production execution gate | A3:RESIDUAL:R-A3-MTLS-CAPABILITY, A3:RESIDUAL:R-A3-A2-AUTH-OUTBOX, A4:SECTION:10.4 |
| A5-RES-04 | Operations/Backend | runbook/observability design | DOC12:S12#TRANSACTION, A3:RESIDUAL:R-A3-REDACTION-RUNTIME, A3:RESIDUAL:R-A3-AUDIT-VOLUME |
| A5-RES-05 | A5-UPSTREAM-DELEGATED | 各 spec owner change control/independent review | DOC12:S18, A3:RESIDUAL:R-A3-HMAC-ROTATION-REPLAY, A4:SECTION:10.4 |
| A5-RES-06 | Telephony/IVR/Security | A4/A5 revision | A4:SECTION:8.4, A4:SECTION:10.4, A4:RESIDUAL:2 |
| A5-RES-07 | Platform/CI | repository CI before production merge policy | A4:CLOSEOUT:10 |
| A5-RES-08 | Infrastructure/Security | evidence platform | A3:RESIDUAL:R-A3-MEDIA-GATEWAY-TRANSPORT, A3:RESIDUAL:R-A3-AUDIT-VOLUME |
| A5-RES-09 | Backend/QA | independent review + future execution | DOC12:S18, A0:Q:Q9 |
| A5-RES-10 | Product/Legal | future accepted contracts | A0:Q:Q4, DOC12:S17.1 |
| A5-RES-11 | A5-UPSTREAM-DELEGATED | 每行原 owner/destination 不变 | DOC14:S14, A3:RESIDUAL:R-A3-LEGAL-RETENTION, A4:SECTION:10.3 |
| A5-RES-12 | Backend/QA | A5 evidence honesty + independent review | A2:INV:INV-A2-15, A3:RESIDUAL:R-A3-AUDIT-VOLUME, A4:SECTION:10.4 |
<!-- acceptance-residual-map:end -->

所有 residual 均有 owner/destination；上游 `R-A2-*`/`R-A3-*`/`Q*`/`CV-*`/A4 residual row 不重命名、不改写。

## 12. 结论与无生产验收声明

S1/S2 输出 `specificationCoverageComplete=true`（仅在所有 semantic checks 通过后）、无证据时 `runtimeEvidence=NOT_RUN`、`productionAccepted=false`。S2 即使接受结构合法的 runtime manifest，也只能到 `EVIDENCE_VALID`；空 `authorityRegistry` 使 `PASS`/production acceptance 不可达（`A5-RES-02`）。静态/model/fixture/caller/count-only 成功都只是规格证据，不是生产验收。没有 runtime/cloud/line/manual 执行，也没有 production acceptance。
