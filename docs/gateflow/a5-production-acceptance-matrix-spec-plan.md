# A5 生产验收矩阵规格实施计划

> Work unit：`A5-production-acceptance-matrix-spec`
>
> Gate：`plan`（只形成 code-generation-ready 实施计划；不实施、不进入 plan review 之后的 Gate）
>
> Task label：`a5-production-acceptance-matrix-plan-ds-20261026-01`
>
> Plan fix 1：`a5-production-acceptance-matrix-plan-fix1-ds-20261026-01`（按 `docs/reviews/plan-review-20261026-142913.md` 的 F-A5-PR-01..06 与 OQ-01..04 做 bounded plan-only 修正）
>
> Base：merged `origin/main` commit `9b54f0fc29946123f1fb6ee80b5845c51c080d65`（PR #6 / A4）；branch `codex/a5-production-acceptance-matrix-spec`
>
> Binding contract：`docs/gateflow/project-execution-control.md` 顶部 A5 binding goal contract、A5 slice principle 与 A5 gate record
>
> Design sources：`docs/12` §12-§14、§18 item 6；`docs/14` §6-§7、§9、§13.7、§13.9-§13.11、§14；`docs/15`；`api/openapi/onecall-v1.openapi.yaml`；`docs/16` §2-§9、§11；`protocol/ivr/v1/ivr-protocol-v1.json` + `protocol/ivr/validate.mjs` + 25 fixtures；`docs/17` §10-§20；`security/v1/security-design-v1.json` + `security/validate.mjs` + 23 fixtures；`docs/18` §8-§10；`deployment/capacity/` + 12 fixtures；A0-A4 final closeout artifacts；`docs/04` 仅作 legacy 场景清单，冲突时以 A0-A4 为准。

## 0. 结论摘要

- 交付一份**静态可验证、来源可解析、证据诚实**的生产验收矩阵规格：一份人类 canonical 文档 `docs/19-production-acceptance-matrix.md`、一份机器模型 `acceptance/v1/acceptance-matrix-v1.json`、一个零三方依赖 Node 22 校验器 `acceptance/validate.mjs`，以及 4 个聚焦对抗 fixture 文件（含 A0 state/edge shape fixture）。
- 矩阵分子固定为 **9 个 critical family + 7 条 cross-system convergence path = 16 个 coverage unit**，每 unit 必须覆盖 **6 个 mandatory axis**，共 **96 个 coverage cell**。计划基线为 **90 个 represented scenario + 6 个 contract-backed `NOT_APPLICABLE` + 0 个 required `DEFERRED`**。缺失、重复、非法 applicability 或只有计数的矩阵一律失败。
- 每个 scenario 必须带稳定 ID、A0-A4 来源引用、owner/aggregate/service、execution tier、前置状态、stimulus/ordering/fault、persisted + externally visible 期望、terminal absorption、idempotency/replay、精确零或有界副作用、audit/Outbox/metric/log 证据、cleanup/recovery、evidence owner 与 acceptance-result semantics。
- **规格覆盖 ≠ 生产验收**。静态 validator、A2/A3/A4 既有 synthetic fixtures、HTML prototypes、设计完成度、调用方字段和 count-only 检查都不能产生 `productionAccepted=true`。A5 静态阶段所有 runtime scenario 保持 `NOT_RUN`；root 输出 `productionAccepted=false`、`runtimeEvidence=NOT_RUN`。
- 只有两个 behaviorally bounded slices：**S1 静态矩阵契约、完整六轴覆盖与诚实证据状态**；**S2 执行证据准入、聚合结果与仓库回归接入**。不按 source doc、subsystem、axis 或 team 拆分。
- 不新增 A0-A4 表、状态、Problem code、事件、role、trust boundary、capacity 公式或产品阈值。A5 只引用 A0-A4 的 machine-readable registries / accepted IDs，不复制其语义所有权。
- 无 blocking open question。acceptance authority、不可变证据存储、真实执行环境、精确 metric/log 字段名均是 typed evidence dependency，按原 owner/destination 保留，不构成 invent 的许可。Controller 已明确接受静态 S2 `authorityRegistry` 为空这一决定：PASS/production acceptance 在未来授权证据源出现前**故意不可达**，这是 typed residual `A5-RES-02`，不是 blocker，也不是伪造 authority row 的许可（OQ-03）。
- Plan fix 1 保留已接受方向（16 units、96 cells、90 represented、6 N/A、2 slices、fail-closed evidence semantics），只收紧六个契约精度缺口：A3 residual 真源、A0 state/edge 形状解析、可满足的 owner registry、A0 edge transition ref 与单一 tier 权威、PATH-DEPLOY acceptance-boundary 区分、clean-checkout 命令。
- 本计划只创建本文件；不修改任何 source spec、model、validator、fixture、README、control doc、prototype 或 screenshot；不 stage/commit/push、不创建/修改 PR 或 issue、不执行真实基础设施测试。

## 1. 目标、动机、成功信号、非目标、边界与目标对齐

### 1.1 目标

形成可被后续实现 agent 直接生成并静态验证的生产验收矩阵规格，满足：

1. 每个 critical accepted state-machine family 与 cross-system convergence path 都映射到 normal、duplicate、timeout、out-of-order、recovery、permission-denied 六个 axis；
2. 每个 cell 要么由稳定 scenario 代表，要么是带 contract-backed reason 与 source refs 的 `NOT_APPLICABLE`；不允许静默缺失；
3. 每个 scenario 同时约束 persisted state、external state、terminal absorption、idempotency/replay、副作用、audit/Outbox/metric/log 证据、cleanup/recovery、owner 与 result semantics；
4. 规格覆盖、fixture 通过、设计完成度与真实执行证据被严格区分；无授权证据时不得出现生产验收通过；
5. 复用 A0-A4 的 ID、registry、validator 输出和 residual owner/destination，不建立第二语义 owner。

### 1.2 成功信号

- `node acceptance/validate.mjs --stage s1 ...` 输出 `PASS acceptance-validation (s1)`，并断言 `coverage_cells=96`、`represented=90`、`not_applicable=6`、`deferred=0`、`coverage_missing=0`、`coverage_duplicate=0`、`coverage_illegal=0`、`na_unbacked=0`、`source_unresolved=0`、`source_ambiguous=0`、`source_drift=0`、`a3_residuals=18`、`a0_state_rows=31`、`a0_shape_unrecognized=0`、`owner_upstream_unresolved=0`、`residual_owner_unknown=0`、`transition_ref_invalid=0`、`path_family_duplicate=0`、`external_projection_contradiction=0`、`terminal_mutation=0`、`duplicate_side_effect=0`、`ordering_regression=0`、`denied_with_business_effect=0`、`production_accepted=false`。
- `node acceptance/validate.mjs --stage s2 ...` 额外断言 `evidence_manifests=0`、`evidence_admitted=0`、`acceptance_records=0`、`runtime_evidence=NOT_RUN`、`production_accepted=false`，并以 fixture 证明 caller-forged PASS、synthetic runtime evidence、tier downgrade、无 authority 的 acceptance、伪造 digest、future timestamp 全部被拒绝。
- A0-A4 既有回归全绿：A1 `PASS check-doc-map (s3)` 且 `total=49`；A2 `PASS protocol-validation (s3)` 且 `fixtures=25 passed=25 failed=0 negative=3`；A3 `PASS security-validation (s3)` 且 `fixtures=23 passed=23 failed=0 negative=10`；A4 `PASS capacity-validation stage=s2` 且 `productionReady=false procurementReady=false cloudVerified=false a3RiskClosed=false`。
- 文档 marker block 与模型逐行一致；来源引用全部解析；residual 全部有 owner/destination；无未分类 gap。

### 1.3 非目标

- 不实现 production Java/frontend/IVR/provider adapter/COS/KMS/Redis/DB migration/queue/CI/test environment；
- 不执行 carrier/SIP/CDR、云、数据库、队列、支付、KMS/COS、渗透、压测、恢复或人工验收；
- 不新增或改变 A0-A4 的表、列、状态、transition、Problem code、role、trust boundary、event、capacity formula、部署选择；
- 不发明 timeout/SLO/RPO/RTO/retention/concurrency/pricing/Region/AZ/SKU/vendor capability 或任何产品策略值；
- 不把 AI、notary、refund、invoice、online work-order、PBX 等没有 accepted upstream production contract 的 workflow 纳入 critical matrix；它们保持 deferred 且不生成 scenario；
- 不建立 test-management platform、runner、orchestration engine、live dashboard、CI pipeline、evidence store 或第二 registry。

### 1.4 范围边界

- 只新增一份矩阵规格、一个机器模型、一个 validator、四个 fixture 文件、`docs/README.md` 一行索引、两份 implementation report。
- `docs/12`-`docs/18`、`api/openapi/`、`protocol/`、`security/`、`deployment/capacity/`、A0-A4 control/closeout/review 全部只读。
- 现有 HTML prototypes、screenshots、`shared/onecall-business.js` 与 local fixtures 只是演示/QA 资产，不是 A5 证据。
- A5 model 不是 public production contract；`acceptance/` 不是 runtime module。

### 1.5 目标对齐映射

| Goal contract element | Plan 落点 | 验证方式 |
| --- | --- | --- |
| 每个 critical family/path 映射六 axis | §4.2、§4.3、§4.10 | 96-cell coverage；missing/duplicate/illegal 检查 |
| stable ID + authoritative source refs | §4.1、§4.9 | ID regex/uniqueness；source resolver |
| owner/aggregate/service + execution tier | §4.5、§4.7 | required-field + owner/tier registry |
| preconditions + starting state | §4.5 | required-state assertions + A0/A2 enum check |
| stimulus/ordering/fault injection | §4.5 | orderingClass ↔ axis 对应检查 |
| persisted + external expected state | §4.5、§4.6 | state transition edge check |
| terminal absorption | §4.6、§8.3 | 终态无出边；post-terminal 零副作用 |
| idempotency/replay | §4.6、§8.3 | duplicate replay identity + zero-delta 检查 |
| exact zero / bounded side effects | §4.6、§8.3 | baseline+step+overall 算术；DENIED/DUP 规则 |
| audit/Outbox/metric/log evidence | §4.6、§4.9 | A3 audit、A0 outbox、DOC12 §12 引用解析 |
| cleanup/recovery | §4.5、§4.6 | cleanup mode + append-only/terminal 保护 |
| evidence owner + result semantics | §4.7、§7 | owner registry + evidence state machine |
| 六 axis 或 contract-backed N/A | §4.3、§5 | registered reason + source refs |
| missing 不静默算覆盖 | §5、§8.3 | `A5_COVERAGE_CELL_MISSING`；count-only fixture |
| static/model/fixture 只是规格证据 | §7、§12.4 | `STATIC_SPEC`/`MODEL_FIXTURE` 最高 `SPEC_VERIFIED` |
| 无 runtime/cloud/line/manual 证据保持 NOT_RUN | §7、§10 | root `runtimeEvidence=NOT_RUN` |
| 不新增 public contract / 产品值 | §3、§12.2 | changed-files + forbidden-field checks |
| 少量 behavior slices | §11 | 2 slices，按 evidence boundary |
| 不过度设计 | §13 | explicit non-platform assessment |

## 2. 第一性原理与直接仓库证据

### 2.1 为什么必须有 A5

1. **验收的真源是“事实 + 副作用 + 证据”，不是 HTTP 成功。** A0 已冻结 44 表、40 invariant、31 个 state-machine row、29 个 transaction sequence，但 A0 只定义 schema/事务/状态；它没有说明每个状态机在 normal/duplicate/timeout/out-of-order/recovery/permission-denied 下应产生什么持久化事实、什么外部可见结果、什么 audit/Outbox 证据。A5 必须把这些 cell 显式化。
2. **跨系统收敛路径不属于任何单一 A0-A4 契约。** `confirmIvrActivation → activation/trial → reservation → call event → settlement → Outbox`、`provider callback → payment → issuance → grant/balance`、`recording validated → media object → evidence creation/solidification`、`share admission → ticket → playback`、`auth domain/signature → domain mutation → audit/redaction` 都是跨 owner 的收敛；单个 spec 的 fixture 无法证明其验收 expectation 一致。
3. **fault axis 不对称，N/A 必须合同支撑。** 例如 A0 subject claim 没有 deadline；FAM-DEPLOYMENT 没有 runtime caller auth；A4 的 backup/capability inputs 是无序 exact set。若把所有 axis 强行套上 scenario，就会发明 timeout、SLO 或权限模型；若允许留空，又会把缺失伪装成覆盖。A5 用 registered `NA-*` reason + source refs 解决这两难。
4. **证据有等级与权威。** A2/A3/A4 fixtures 是 deterministic model/fixture evaluator，不是真实 PostgreSQL/Redis/COS/线路/云；A1 是静态 OpenAPI；A4 明确 `productionReady`/`procurementReady`/`cloudVerified`/`a3RiskClosed` 恒 false。A5 必须把 `SPEC_VERIFIED`、`EVIDENCE_VALID`、`PASS`、`productionAccepted` 分开，否则会把静态成功升级为生产验收。
5. **调用方断言与 synthetic 输入不能成为验收。** A4 已用 `CALLER_CONTROL_FORBIDDEN` 固定 root readiness 常量；A5 必须对 matrix/evidence ledger 采用同一 fail-closed 原则：输入中的 `productionAccepted`、`result=PASS`、`authority` 或 synthetic runtime evidence 只能被拒绝，不能被接受。

### 2.2 直接仓库证据

| 证据 | 精确位置 | 为什么现有内容不足 |
| --- | --- | --- |
| Architecture 明确要求 A5 | `docs/12` §18 item 6：把每条关键状态机映射到正常、重复、超时、乱序、恢复和权限拒绝场景 | 这是 mandate，不是已完成实现；当前无矩阵文档/model/validator |
| A0 只有 schema/invariant/state/transaction 契约 | `docs/14` §4.1（44 表）、§6.1（40 invariants）、§7.1/§7.3/§7.4（31 state-machine rows）、§9（29 transactions）、§10.17/§10.24（Outbox/evidence identity） | 无 scenario、无 axis、无 evidence owner/result、无 runtime evidence 状态 |
| A1 明确静态限制 | `docs/15` §10：不能证明会话签发、DB 约束/锁/事务、支付/IVR/媒体/COS/Redis、真实并发与脱敏 | 49 operations + 59 Problem codes 只冻结契约，不是验收 |
| A2 明确自述不是验收矩阵 | `docs/16` §10 末句：“The machine is a fixture evaluator, not a backend, IVR runtime, media adapter or production acceptance matrix.”；§11 末段列 13 个 residual | 25 个 fixture 是 IVR protocol 的 deterministic behavior 证明，不是跨 family/axis 验收 |
| A3 明确自述不是验收/渗透 | `docs/17` §19 末段：“fixtures 是安全模型级断言，不是生产验收或渗透测试。”；§20 列 18 个 residual | 23 个 fixture 只证明 security model/fail-closed；无 runtime/cloud/credential 证据 |
| A4 明确自述不是容量/恢复/云验证 | `docs/18` §10.4；`docs/gateflow/a4-production-capacity-deployment-spec-final-closeout.md` §Verification/Residual risks | 12 个 fixture 只证明算术、维度、fail-closed、输出契约；A4 closeout 多行明确要求把 failure/limit/timeout/recovery/permission-denied 路径 map into A5 |
| 无跨规格 coverage/evidence owner | 在 `docs/14`-`docs/18` 与 `protocol/`、`security/`、`deployment/` 中不存在 9 families × 6 axes、cross-system path、acceptance evidence state、evidence owner 或 acceptance authority | 这是 A5 要关闭的真实缺口 |
| prototypes 不是证据 | `docs/12` §14.2 末句；`docs/README.md` 顶部 S3 说明 | HTML/prototype/screenshot 只能证明演示/QA，不能替代生产验收 |
| A4 residual 明确指向 A5 | A4 closeout Residual risks 表：line/load、media、spool outage、SLO/RPO/RTO、A3 risks 均写 “map … into A5” / “A5 evidence rows” | 若 A5 不建立矩阵，这些 residual 没有验收 destination |

### 2.3 关键计数基线（用于来源完整性，不是成功条件本身）

- A0：44 table、40 invariant、31 state-machine row（S1 12 + S2 13 + S3 6）、29 transaction sequence（T-S1×10、T-S2×14、T-S3×5）、28 open-input row（§14）。
- A1：49 operation、59 Problem code、6 security scheme、五面 `customer=18 operations=21 ivr=6 public-share=3 provider-callback=1`。
- A2：12 message、11 event type、22 identity、15 invariant、25 fixtures、event registry digest `c6b769d4953db7b38d5bf1b5d396845b51a651ad38e53157f20dec978249f4e9`。
- A3：5 trust surface、5 auth domain、7 trust boundary、6 service identity、10 credential class、2 signature profile、2 media policy、39 audit/access event（2 access_event + 37 append_fact）、10 high-risk action、10 call-class coverage、20 key class、28 data class、23 deferred inputs、21 invariant、23 fixtures、**18 residual rows（`docs/17` §20，lines 692–709）**。A3 model 内不得出现 `R-A3` 字符串或 `residuals` key；residual 只在 `docs/17` 真源。
- A4：9 input group、12 fixtures、6 capability IDs（`CV-1..CV-5`、`regionAzSku`）、6 backup dependency IDs、`productionReady=false`/`procurementReady=false`/`cloudVerified=false`/`a3RiskClosed=false` 固定常量、`docs/18` §10.3 residual row 16、A4 final closeout residual row 10。

这些计数只作为 **source integrity assertions**；任何 count-only 成功打印、缺失 semantic/evidence 检查都视为不合格。

## 3. 产物布局、单一真相与公共契约决定

### 3.1 文件布局

```text
docs/19-production-acceptance-matrix.md              # 唯一人类 canonical spec（S1 写主体；S2 追加 §9）
acceptance/
  v1/acceptance-matrix-v1.json                       # 唯一机器矩阵（S1 写主体；S2 additive evidence 段）
  validate.mjs                                       # 零三方依赖 Node 22 ESM validator/CLI
  fixtures/
    s1-01-matrix-integrity.json                      # S1 覆盖/来源/owner/residual/config-boundary 对抗 cases
    s1-02-expectation-safety.json                    # S1 终态/副作用/replay/ordering/denial/transition/tier/external 对抗 cases
    s1-03-a0-state-shapes.json                       # S1 A0 真实 state/edge 形状冻结 map + 负例
    s2-01-evidence-admission.json                    # S2 caller/synthetic/tier/authority/digest/time cases
docs/README.md                                       # S2 仅新增 docs/19 一行索引
docs/gateflow/a5-production-acceptance-matrix-spec-s1-implementation.md
docs/gateflow/a5-production-acceptance-matrix-spec-s2-implementation.md
```

- 不新增 `package.json`、JSON Schema 文件、第二 protocol/security 文档、README、CI 配置或 runtime module。
- `acceptance/validate.mjs` 是唯一 validator entry point；不导出 public production API。
- `docs/19` 是语义/字段/规则的 human canonical；`acceptance/v1/acceptance-matrix-v1.json` 是 scenario/coverage/state 的 machine canonical。二者由 marker block 强制逐行一致；任何不一致 fail，不存在“文档优先”或“模型优先”的静默漂移。
- 不复制 A0-A4 的完整表格；只引用稳定 ID。A5 model 中的计数/摘要只为 drift detection，不成为第二语义 owner。

### 3.2 公共契约决定

- **无 public production contract 变更**：不新增/修改 OpenAPI operation、schema、Problem code、role、security scheme、IVR message/event、A0 table/column/state、A4 formula 或 deployment 选择。因此本计划**不需要 public production contract 变更**。
- `acceptance/` 是 non-public specification tooling；其 JSON 字段和 CLI 不是 backend API，不被客户端/生产服务消费。
- **无 migration**：不新增 Flyway、DDL、data backfill 或 runtime configuration。
- **无 rollout**：不部署、不启动 environment、不启用 feature flag；A5 产物只用于静态评审和后续授权执行前的 matrix 定义。
- **无 CI 平台建设**：只记录可复现命令；不新增 GitHub Actions/流水线。现有 PR 无 CI checks 的事实保持 A4 residual，不在 A5 关闭。

## 4. 验收矩阵数据契约

### 4.1 稳定 ID 与命名

- Coverage unit ID：
  - families：`FAM-IDENTITY`、`FAM-SUBJECT`、`FAM-ACTIVATION`、`FAM-COMMERCE`、`FAM-IVR`、`FAM-MEDIA-EVIDENCE`、`FAM-SHARING`、`FAM-SECURITY`、`FAM-DEPLOYMENT`。
  - paths：`PATH-ACTIVATION-TRIAL`、`PATH-COMMERCE-ENTITLEMENT`、`PATH-IVR-SETTLEMENT`、`PATH-RECORDING-EVIDENCE`、`PATH-SHARE-ACCESS`、`PATH-AUTH-AUDIT`、`PATH-DEPLOY-ACCEPTANCE`。
- Axis ID 恰为：`NORMAL`、`DUPLICATE`、`TIMEOUT`、`OUT_OF_ORDER`、`RECOVERY`、`PERMISSION_DENIED`。
- Scenario ID 正则：
  `^A5-(IDENT|SUBJ|ACTIV|COMM|IVR|MEDIA|SHARE|SEC|DEPLOY|X-(ACTIV|COMM|IVR|MEDIA|SHARE|AUTH|DEPLOY))-(NORMAL|DUPLICATE|TIMEOUT|OUT_OF_ORDER|RECOVERY|PERMISSION_DENIED)-[0-9]{2}$`
- 每个 represented cell 恰有一个 scenario；`duplicate scenarioId`、`scenario unit/axis mismatch`、`orphan scenario`、`scenario for N/A cell` 全部失败。
- Residual ID：A5 自有 `A5-RES-01..99`，必须有 `upstreamRefs`；上游 `R-A2-*`/`R-A3-*`/`Q*`/`CV-*`/A4 residual row 不重命名、不改写。
- Model root 固定字段：`modelVersion`、`matrixId`、`specAsOf`、`axes`、`coverageUnits`、`notApplicableReasons`、`owners`、`executionTiers`、`coverage`、`scenarios`、`residuals`、`sourceBaseline`、`reasonRegistry`、`acceptanceStateMachine`。S2 additive：`evidencePolicy`、empty `authorityRegistry`。`specAsOf` 是规格作者时间，不是生产阈值；runtime 时间只出现在 evidence manifest。

### 4.2 Coverage units 与来源映射

每个 unit 的 source refs 是“应覆盖的权威契约”，不是复制；完整 scenario 仍需逐条选择更精确的 op/event/fixture ref。

| Unit | 范围 | 主要 A0 state/invariant | A1 操作/错误锚点 | A2/A3/A4 验证锚点 | 运行时最低证据 |
| --- | --- | --- | --- | --- | --- |
| `FAM-IDENTITY` | 客户/员工身份、workspace、organization access | Customer account；Customer login identity；Account real-name profile；Organization；Organization membership；Employee account；INV-01..04、INV-30、INV-31、INV-33 | `getCustomerMe`、`listCustomerBusinessNumbers`、`getOperationsSubject`、Problem `UNAUTHENTICATED/FORBIDDEN/NOT_FOUND` | A3 `TS1/TS2`、`TB1/TB2`、`C01/C02`、`INV-A3-07/09/14`；A0 T-S1-04/05/09/10 | `INTEGRATION` |
| `FAM-SUBJECT` | subject/number claim、binding、risk | Subject ownership；Caller-number binding；Subject claim；Subject risk state；INV-05、06、07、33 | `createCustomerSubjectClaim`、`getCustomerSubject`、`listOperationsSubjects`、Problem `CLAIM_RISK_BLOCKED/CLAIM_REPLAY_CONFLICT/CLAIM_CONTEXT_CONFLICT` | A0 T-S1-06/07/08；A3 data/redaction refs；A4 无 | `INTEGRATION` |
| `FAM-ACTIVATION` | activation、consent、first-trial issuance | Service activation；Consent record；Trial；INV-08、09、10、16、17、27、37 | `createCustomerSubjectActivation`、`confirmIvrActivation`、`suspendOperationsSubject`、`resumeOperationsSubject`、Problem `CONSENT_REQUIRED/ACTIVATION_CONSENT_DECLINED/ACTIVATION_STATE_CONFLICT` | A2 `s1-03`、`s1-04`、`s2-01`；A3 `SUBJECT_SUSPEND/RESUME` high-risk；A0 T-S2-01/09/13 | `INTEGRATION`；IVR 路径另需 `LINE` |
| `FAM-COMMERCE` | catalog/order/payment/entitlement/usage reservation & reconciliation | Catalog version；Customer order；Payment order；Entitlement issuance；Usage reservation；INV-18、19、20、21、22、23、36、38 | `createCustomerOrder`、`getCustomerOrder`、`receivePaymentProviderCallback`、catalog/trial/order operations、Problem `PURCHASE_*`、`IDEMPOTENCY_CONFLICT`、`ORDER_UNKNOWN_PAYMENT_PENDING`、`ACTIVATION_NOT_ACTIVE`、`REAL_NAME_REQUIRED` | A2 `s2-01`、`s2-04`、`s2-07`、`s2-11`；A3 `sec-s2-06`、high-risk catalog/trial；A0 T-S2-07/08/14/15/16 | `INTEGRATION` |
| `FAM-IVR` | task/decision/session/call/leg/event outbox/reconciliation/B-leg recovery | Outbound task；Inbound session；IVR decision；Direct authorization；Call record；Usage reservation；INV-11..15、19、32、34、35、38；A2 INV-A2-01..15 | `createCustomerOutboundCallTask`、`cancelCustomerOutboundCallTask`、`resolveIvrInbound`、`confirmIvrActivation`、`authorizeIvrDirectOutbound`、`ingestIvrCallEvent`；Problem `IVR_ARGUMENT_INVALID/SESSION_IDENTITY_MISMATCH/CALL_UNKNOWN/EVENT_ID_CONFLICT` | A2 全部 25 fixtures，尤其 `s1-01..06`、`s2-01..11`；A3 `TS3/C03/C04`、`sec-s2-04..09`；A4 `F-IVR-01..07`、`F-SPOOL-*` | `INTEGRATION`；真实线路/B-leg 场景需 `LINE` |
| `FAM-MEDIA-EVIDENCE` | recording/media upload、evidence creation/solidification、artifact lifecycle | Recording；Evidence record；Evidence creation cycle；`A0:TABLE:evidence.media_object`/`evidence.evidence_artifact`/`evidence.evidence_event`；INV-15、24、25；A2 INV-A2-09/15 | `declareIvrRecording`、`completeIvrRecording`、`listCustomerEvidence`、`getCustomerEvidence`、`getOperationsIvrSession`；Problem `UPLOAD_OBSERVATION_REQUIRED/UPLOAD_EXPIRED/STORAGE_UNKNOWN/RECORDING_IDENTITY_CONFLICT` | A2 `s3-01..08`；A3 `C09/C10`、`U1/P1`、COS/redaction、`sec-s3-01..08`；A4 `F-REC-*`、`F-UP-*`、`F-PB-*` | `INTEGRATION` |
| `FAM-SHARING` | share grant/admission/ticket/playback/access-count | Share grant；Share admission and derived session；Share access event；INV-26、39 | `createCustomerShare`、`revokeCustomerShare`、`rotateCustomerShareCode`、`getPublicShare`、`createPublicShareAdmission`、`authorizePublicShareMedia`；Problem `SHARE_*` exact sets | A3 `C07/C08/C10`、`Q22`、media policy、`sec-s3-02/07`；A4 `F-PB-01..03`；A0 T-S3-03/04/05 | `INTEGRATION` |
| `FAM-SECURITY` | auth domains、signing/nonce/replay、credentials、audit/high-risk mutation、deterministic redaction | Idempotency record；Outbox event；INV-27..31、35、40 | 全部 49 operations 的 auth/Problem 面；`listOperationsAuditFacts`；10 个 high-risk operations | A3 `TS1..TS5`、`TB1..TB7`、`C01..C10`、39 audit events、`INV-A3-01..21`、`sec-s2-01..09`、`sec-s3-04..08` | `INTEGRATION`；key authority 场景需 `MANUAL` |
| `FAM-DEPLOYMENT` | deployment dependencies、degraded operation/failover、backup/restore、capacity gates where A3/A4 require runtime evidence | 无新增 state machine；A0 INV-28/30/40；A3 `INV-A3-17/18`；`docs/12` §13 | 无 runtime API；引用 A4 root constants/flags | A4 `backupRecovery`/`capabilityEvidence` groups、`F-BACKUP-01/02`、`F-CAP-01`、`s2-01..03`、§10.3 residual | `CLOUD`/`MANUAL`/`INTEGRATION`（按 scenario） |
| `PATH-ACTIVATION-TRIAL` | IVR/client confirmation → consent/activation → first trial → grant/balance/Outbox | INV-08/09/10/16/37；T-S2-01/13 | `confirmIvrActivation`、`createCustomerSubjectActivation` | A2 `s1-03`、`s2-01`；A3 high-risk audit；A0 `FIRST_TRIAL_GRANTED`/`ACTIVATION_TRANSITION_APPLIED` | `INTEGRATION` |
| `PATH-COMMERCE-ENTITLEMENT` | provider callback → payment event/order → issuance → grant/balance/Outbox | INV-21/22/23/36；T-S2-07/08 | `receivePaymentProviderCallback`、`createCustomerOrder`、order read ops | A3 `sec-s2-06`；A0 `PAYMENT_EVENT_APPLIED`/`ENTITLEMENT_ISSUED`；A4 无 | `INTEGRATION` |
| `PATH-IVR-SETTLEMENT` | resolve/claim → reservation → call events → settlement/reconciliation → Outbox | INV-11/12/13/18/19/38；A2 INV-A2-04/05/14 | `resolveIvrInbound`、`ingestIvrCallEvent` | A2 `s2-01..11`；A0 `USAGE_SETTLED/RELEASED`；A4 `F-IVR-*` | `INTEGRATION`；真实线路需 `LINE` |
| `PATH-RECORDING-EVIDENCE` | declare/observe/complete → media object → recording validated → evidence creation/solidification/artifact | INV-15/24/25；A2 INV-A2-09/15 | `declareIvrRecording`、`completeIvrRecording` | A2 `s3-01..08`；A3 `C09/U1/COS`；A0 `EVIDENCE_RECORDING_VALIDATED`、`EVIDENCE_CREATED` | `INTEGRATION` |
| `PATH-SHARE-ACCESS` | grant → admission → ticket → playback/seek/renew → access events | INV-26/39；T-S3-03/04/05 | `createCustomerShare`、`createPublicShareAdmission`、`authorizePublicShareMedia`、`getPublicShare` | A3 `C07/C08/C10/Q22`；A4 `F-PB-*` | `INTEGRATION` |
| `PATH-AUTH-AUDIT` | auth domain/signature → domain mutation → audit/high-risk/redaction → Outbox | INV-27/28/29/31/35/40；A3 `INV-A3-07/09/10/11/14/15/17` | 所有五个 surface 的代表 operation；`listOperationsAuditFacts`；10 high-risk | A3 `sec-s2-01..09`、`sec-s3-04..08`；A2 signature/replay refs | `INTEGRATION` |
| `PATH-DEPLOY-ACCEPTANCE` | A4 deployment gate/evidence → A5 acceptance-admission **boundary observations**（不重复 `FAM-DEPLOYMENT` 内部检查，不自测 validator 计数） | A4 §8.3/§9/§10；A3 `INV-A3-17/18`；`docs/12` §13 | 无 API；A5 evidence manifest/state-machine admission boundary | A4 `s2-01..03`、root constants/flags、§10.2/§10.4；A5 evidence manifest/state machine | `CLOUD`/`MANUAL`/`INTEGRATION`（按 scenario） |

Family lead owner：`FAM-IDENTITY`=`customer-iam`；`FAM-SUBJECT`=`subject-number`；`FAM-ACTIVATION`=`activation-consent`；`FAM-COMMERCE`=`catalog-order`；`FAM-IVR`=`ivr`；`FAM-MEDIA-EVIDENCE`=`evidence`；`FAM-SHARING`=`sharing`；`FAM-SECURITY`=`Security/Backend`（co-owner `operations-audit`、`platform-reliability`）；`FAM-DEPLOYMENT`=`Infrastructure/Security`（co-owner `Platform/CI`、`Telephony/IVR`；non-runtime tooling）。全部 owner 必须是 §4.7 registry 中的 exact ID。

Path lead owner/service（其他参与方仍须在 sourceRefs/effects 中出现）：

| Path | Lead owner | Lead service |
| --- | --- | --- |
| `PATH-ACTIVATION-TRIAL` | `activation-consent` | `core-api` |
| `PATH-COMMERCE-ENTITLEMENT` | `catalog-order` | `core-api` |
| `PATH-IVR-SETTLEMENT` | `ivr` | `core-api` |
| `PATH-RECORDING-EVIDENCE` | `evidence` | `core-api` |
| `PATH-SHARE-ACCESS` | `sharing` | `core-api` |
| `PATH-AUTH-AUDIT` | `platform-reliability` | `core-api` |
| `PATH-DEPLOY-ACCEPTANCE` | `Infrastructure/Security` | `deployment/capacity` |

Path→family pairing（仅用于 §8.3 的唯一性检查和 acceptance-boundary 评审，不改变 coverage 计数）：`PATH-ACTIVATION-TRIAL`↔`FAM-ACTIVATION`、`PATH-COMMERCE-ENTITLEMENT`↔`FAM-COMMERCE`、`PATH-IVR-SETTLEMENT`↔`FAM-IVR`、`PATH-RECORDING-EVIDENCE`↔`FAM-MEDIA-EVIDENCE`、`PATH-SHARE-ACCESS`↔`FAM-SHARING`、`PATH-AUTH-AUDIT`↔`FAM-SECURITY`、`PATH-DEPLOY-ACCEPTANCE`↔`FAM-DEPLOYMENT`。每个 path scenario 必须至少有一个 `sourceRefs` 和一个 `outcomeRefs` 不被同 axis 且 `REPRESENTED` 的 paired family scenario 共享（paired family 为 N/A 时不适用）；`PATH-DEPLOY-*` 必须是 A4→A5 acceptance-boundary observation，而不是 family 内部检查的复述。

### 4.3 Axis 与 `NOT_APPLICABLE` reason registry

- 每个 `(unitId, axis)` 恰有一行 coverage；`REPRESENTED` 必带 scenarioId；`NOT_APPLICABLE` 必带 registered reason + `sourceRefs`；`DEFERRED` 必带 residualId + owner/destination + blockedBy，且**不计为覆盖**。
- 计划基线 `DEFERRED=0`。任何 required cell 的 deferred 都使 `specificationCoverageComplete=false`，不得以 owner/destination 存在为由算覆盖。
- `NA-*` registry（validator 只接受以下固定 ID；free-text-only N/A 失败）：

| Reason ID | 允许的 unit/axis | 精确理由 | 必须引用的 source refs |
| --- | --- | --- | --- |
| `NA-NO-CONTRACT-DEADLINE` | `FAM-SUBJECT:TIMEOUT`；`FAM-DEPLOYMENT:TIMEOUT`；`PATH-DEPLOY-ACCEPTANCE:TIMEOUT` | A0 subject claim/binding/risk 行没有 deadline/expiry 语义；A4 明确 SLO/timeout/RPO/RTO 未批准，backup/capability gates 不是请求 deadline | `A0:SM:Subject claim`、`A0:SM:Subject risk state`、`A0:SM:Caller-number binding`、`A4:SECTION:8.4`、`A4:SECTION:10.4`、`DOC12:S17.1` |
| `NA-NO-ORDERED-FACT-STREAM` | `FAM-DEPLOYMENT:OUT_OF_ORDER`；`PATH-DEPLOY-ACCEPTANCE:OUT_OF_ORDER` | A4 `BACKUP_DEPENDENCY` 与 `CAPABILITY` 是 exact-set 输入，重复是 INVALID；不存在事件 sequence/arrival order 语义 | `A4:SECTION:9.1`、`A4:SECTION:10.1`、`A4:FIX:s2-01-backup-recovery`、`A4:FIX:s2-02-capability-assertion` |
| `NA-NO-AUTHZ-BOUNDARY` | `FAM-DEPLOYMENT:PERMISSION_DENIED` | A4 calculator 没有 runtime caller identity/role/tenant；`CALLER_CONTROL_FORBIDDEN` 是 input integrity，不是 permission；A3 五个 auth domain 只覆盖 API surfaces，不覆盖 capacity calculator | `A4:SECTION:1.2`、`A4:SECTION:8.3`、`A4:SECTION:10.4`、`A3:TS:TS1`、`A3:TS:TS5` |

- 每个 N/A 至少引用与该 `(unitId, axis)` 对应的 2 个 refs；下表列出该 reason 的可用/必需锚点集合，不要求一个 N/A 跨 unit 引用整行所有 refs。
- `FAM-IDENTITY:TIMEOUT` **不是** N/A：会话过期由 A3 credential class `C01/C02` 的 `ttlInput=Q1/Q19` 参数化，scenario 只引用 policy input，不写数值。
- `FAM-ACTIVATION:TIMEOUT` **不是** N/A：first-trial 的 `valid_days`/expiry 由 `trial_policy_version` 输入参数化，scenario 断言“不重发 trial”，不写生产值。
- `FAM-DEPLOYMENT:PERMISSION_DENIED` 的 N/A 与 `PATH-DEPLOY-ACCEPTANCE:PERMISSION_DENIED` 的 represented 不矛盾：前者是 A4 runtime calculator，后者是 A5 evidence-admission boundary；A5 的 caller-forged evidence 拒绝由本 work unit 的 binding contract 支撑。

### 4.4 Coverage matrix 基线（16 × 6）

`R` = represented（必有一个 scenario）；`NA` = registered N/A。基线为 90 R + 6 NA = 96 cells。

| Unit | NORMAL | DUPLICATE | TIMEOUT | OUT_OF_ORDER | RECOVERY | PERMISSION_DENIED |
| --- | --- | --- | --- | --- | --- | --- |
| `FAM-IDENTITY` | R | R | R | R | R | R |
| `FAM-SUBJECT` | R | R | NA `NA-NO-CONTRACT-DEADLINE` | R | R | R |
| `FAM-ACTIVATION` | R | R | R | R | R | R |
| `FAM-COMMERCE` | R | R | R | R | R | R |
| `FAM-IVR` | R | R | R | R | R | R |
| `FAM-MEDIA-EVIDENCE` | R | R | R | R | R | R |
| `FAM-SHARING` | R | R | R | R | R | R |
| `FAM-SECURITY` | R | R | R | R | R | R |
| `FAM-DEPLOYMENT` | R | R | NA `NA-NO-CONTRACT-DEADLINE` | NA `NA-NO-ORDERED-FACT-STREAM` | R | NA `NA-NO-AUTHZ-BOUNDARY` |
| `PATH-ACTIVATION-TRIAL` | R | R | R | R | R | R |
| `PATH-COMMERCE-ENTITLEMENT` | R | R | R | R | R | R |
| `PATH-IVR-SETTLEMENT` | R | R | R | R | R | R |
| `PATH-RECORDING-EVIDENCE` | R | R | R | R | R | R |
| `PATH-SHARE-ACCESS` | R | R | R | R | R | R |
| `PATH-AUTH-AUDIT` | R | R | R | R | R | R |
| `PATH-DEPLOY-ACCEPTANCE` | R | R | NA `NA-NO-CONTRACT-DEADLINE` | NA `NA-NO-ORDERED-FACT-STREAM` | R | R |

Scenario 计数按 unit：IDENT 6、SUBJ 5、ACTIV 6、COMM 6、IVR 6、MEDIA 6、SHARE 6、SEC 6、DEPLOY 3、X-ACTIV 6、X-COMM 6、X-IVR 6、X-MEDIA 6、X-SHARE 6、X-AUTH 6、X-DEPLOY 4 = **90**。

### 4.5 Coverage row 与 scenario schema

Coverage row（model `coverage[]`）：

```text
{
  unitId, axis,
  state: "REPRESENTED" | "NOT_APPLICABLE" | "DEFERRED",
  scenarioId?: string,             // required iff REPRESENTED
  reasonCode?: "NA-*",             // required iff NOT_APPLICABLE
  sourceRefs?: string[],           // required iff NOT_APPLICABLE
  residualId?: string,             // required iff DEFERRED
  blockedBy?: string,              // required iff DEFERRED
  owner?, destination?             // required iff DEFERRED
}
```

Scenario（model `scenarios[]`）必填字段：

```text
{
  scenarioId, coverageUnitId, axis, title,
  owner, aggregateRef, service,
  requiredEvidenceTiers: ("STATIC_SPEC"|"MODEL_FIXTURE"|"INTEGRATION"|"LINE"|"CLOUD"|"MANUAL")[], // 唯一权威 tier 字段
  sourceRefs: string[],                       // >=2；必须含 state/invariant + verification anchor
  outcomeRefs: string[],                      // >=1；必须是 sourceRefs 的非空子集，定义 expected outcome 的 contract 依据
  preconditions: {
    startingState: StateAssertion[],
    fixtureRefs: string[],                    // A2:FIX/A3:FIX/A4:FIX；没有则 []
    environmentRefs: string[],                // group/surface/domain refs；不得含环境值
    assumptionRefs: string[]
  },
  stimulus: {
    orderingClass: "NORMAL"|"IDENTICAL_RETRY"|"CONFLICTING_RETRY"|"OUT_OF_ORDER"|
                   "POST_TERMINAL"|"DEADLINE_CROSSING"|"DEPENDENCY_FAILURE"|
                   "RESTART"|"FAILOVER"|"PERMISSION_DENIED",
    steps: Step[],                            // ordered；每步有 stepId
    faultInjection: { kind, parameterRef? }   // 数值只能来自 source/policy input
  },
  expected: {
    persisted: StateAssertion[],
    external: ExternalAssertion[],
    terminalAbsorption: {
      terminalStates: string[],               // resolved from A0/A2
      postTerminalOutcome: "ABSORB"|"IGNORE"|"QUARANTINE"|"REJECT",
      sourceRefs: string[]
    },
    idempotency: {
      identityRef, sameFingerprintOutcome, changedFingerprintOutcome, sourceRefs
    },
    effects: EffectLedger,                    // §4.6
    evidence: EvidenceExpectation,            // §4.6
    cleanup: {
      mode: "NONE"|"IDEMPOTENT_REPLAY"|"AUTHORITATIVE_RECONCILIATION"|
            "NEW_IDENTITY_REQUIRES_REF"|"MANUAL_RESTORE",
      steps: string[], sourceRefs: string[]
    }
  },
  duplicateReplay?: {                          // required for DUPLICATE axis
    ofStepId, businessEffect: "ZERO", responseOutcome: "FIRST_RESULT"|"CONFLICT"
  },
  evidenceOwner,                               // owner registry ID
  acceptanceAuthorityRef?: string|null,        // S2 authority registry；默认 null
  resultSemantics: {                           // 封闭字段；不得出现 requiredEvidenceTiers 第二副本
    specResult: "SPEC_DEFINED", runtimeResult: "NOT_RUN",
    acceptanceRuleRef, failConditionRefs
  },
  residualIds: string[]
}
```

- `requiredEvidenceTiers` 只有 scenario root 一处权威；`resultSemantics` 是封闭字段集合，出现 `requiredEvidenceTiers` 即 `A5_SCENARIO_FIELD_INVALID`（v1 不保留 derived projection，因此不存在“root vs projection”双真源）。
- `expected.external` 是该 scenario **最终** externally visible projection 的唯一权威；step `expectedResponse` 只是该 step 的局部观察，不能改写最终 projection（reconciliation 规则见 §4.6/OQ-01）。

Step：

```text
{
  stepId,
  actorRef,                                    // A3:TS/A3:SERVICE 或 A0 owner module
  operationRef?: "A1:OP:*",
  eventRef?: "A2:EVENT:*",
  faultRef?: string,
  identityRefs: string[],                      // A2:IDENTITY、A0:TABLE key 或 A2 message identity
  timingRef?: string,                          // 只能指向 contract/policy input
  expectedResponse?: ExternalAssertion,
  expectedState?: StateAssertion[],            // 该 step 后的状态；用于逐 step 终态/regression 检查
  expectedDelta?: EffectDelta                  // 该 step 的局部业务/审计/Outbox 增量
}
```

### 4.6 State、effect 与 evidence 断言

**StateAssertion**：

```text
{
  targetRef: "A0:SM:<aggregate>" | "A2:STATE:<machine>" | "A0:TABLE:<schema.table>",
  field: string,
  op: "EQUALS"|"UNCHANGED"|"NULL"|"NOT_NULL",
  value?: string|number|boolean,
  transitionRef?: "A0:SM:<aggregate>:<from>-><to>" | "A2:STATE:<machine>:<from>-><to>",
  basisRefs: string[]
}
```

- `transitionRef` 的合法形式只有两种：`A0:SM:<aggregate>:<from>-><to>`（由 §4.9.1 frozen parser 产出的 directed edge）与 `A2:STATE:<machine>:<from>-><to>`（A2 structured transition）。`A0:TX:*` **只能**出现在 `sourceRefs`/`basisRefs`/`outcomeRefs`，作为 transaction-sequence 依据；把它放进 `transitionRef` => `A5_TRANSITION_REF_INVALID`。
- `A0:SM`/`A2:STATE` 的 `value` 必须属于该状态机 allowed states；derived state（如 Trial `EXHAUSTED`/`EXPIRED`）不是 mutable state，不得作为 `A0:SM` value 或 transition endpoint，违反 => `A5_TRANSITION_REF_INVALID`。
- 省略 `transitionRef` 只允许两类：(a) target machine 被 resolver 分类为 empty-edge row（§4.9.1 的 `INSERT_ONLY`/`APPEND_ONLY`/`FROZEN_RESULT`/`DERIVED_IMMUTABLE`/`DERIVED_FACT`/`APPEND_ONLY_HISTORY`/`APPEND_ONLY_OUTCOMES`/`PROSE_PARENTHESIZED`）；(b) 非 transition 表断言，即 `targetRef` 是 `A0:TABLE:*`，或该 row 在 step 前不存在（creation，preState=`ABSENT`，不是 state transition）。其他 state-changing `EQUALS` 省略 `transitionRef` => `A5_SCENARIO_FIELD_MISSING`。
- `preconditions.startingState` 与最终 `expected.persisted` 的 assertion 不得携带 `transitionRef`（违反 => `A5_SCENARIO_FIELD_INVALID`）；transition edge 只在 `stimulus.steps[].expectedState` 上表达。`UNCHANGED` 不得携带 `transitionRef`（违反 => `A5_SCENARIO_FIELD_INVALID`）。
- step 上的 `EQUALS` 若 value 与 preState 相同，必须改用 `UNCHANGED`；终态在无允许出边时只能 `UNCHANGED`，否则 `A5_TERMINAL_MUTATION`。
- `A0:TABLE` 的 `field` 必须是 A0 表 catalog 的最小列；未知列 fail。
- 不复制状态枚举；枚举、derived state 与 edge 均由 A0/A2 resolver 提供。

**ExternalAssertion**：`{operationRef, field, op, valueRef?, basisRefs}`。`field` 只允许 `status|action|reasonCode|problemCode|processingStatus|terminalStatus|counts.*`；`valueRef` 必须解析到 A2 action/reason/endReason/quarantineReason、A1 ProblemCode 或 A0 enum；禁止自由文本业务结论。

**External projection reconciliation（OQ-01）**：

- `expected.external` 是 final authoritative projection，必须非空。
- step `expectedResponse` 是 step-local；对每个同时在 root external 与至少一个 step `expectedResponse` 中出现的 `(operationRef, field)`，按 step 顺序取**最后一个**匹配 step，其 `op`/`valueRef` 必须与 root assertion 完全一致。前序 step 可以不同（例如先 `APPLIED` 后 `IGNORED`），但最终 step 不得与 root 冲突。
- root external 声明但 steps 未观察的 key 合法（projection 可由执行外部观测补充）；step 观察但 root 未声明的 transient key 也合法，不成为第二 final projection。
- 任何最后一个匹配 step 与 root 的 op/valueRef 不一致 => `A5_EXTERNAL_PROJECTION_CONTRADICTION`；不得以任意一侧静默覆盖。

**EffectLedger**：

```text
{
  baseline: EffectVector,          // 场景开始前已有计数
  steps: { stepId, delta: EffectVector }[],
  overall: EffectVector,           // 必须 = baseline + Σ delta（EXACT），或 BOUNDED 的 max >= 该和
  zeroOrBoundedBasisRefs: string[]
}
EffectVector = {
  domainWrites: [{ tableRef, mode:"EXACT"|"BOUNDED", value?, min?, max?, basisRef }],
  audit:        [{ auditRef:"A3:AUDIT:*", mode, value?, min?, max?, basisRef }],
  outbox:       [{ outboxRef:"A0:OUTBOX:*", mode, value?, min?, max?, basisRef }],
  accessEvents: [{ accessEventRef:"A3:AUDIT:*", mode, value?, min?, max?, basisRef }],
  externalCalls:[{ operationRef:"A1:OP:*", mode, value?, min?, max?, basisRef }]
}
EffectDelta = 同 EffectVector，但计数必须是非负整数；未列 ref 视为 0，不得用“未列”掩盖应有写入。
```

- `mode=EXACT` 必须给 `value>=0`；`BOUNDED` 必须给 `0<=min<=max` 且 `basisRef`；任何负值/缺 basis/未知 ref fail。
- `auditRef`/`accessEventRef` 还必须与 scenario 的 owner/operation/trigger 兼容：high-risk mutation 使用 A3 high-risk 表的对应 `auditCode`；认证/签名/replay/媒体/审计使用 A3 对应 security/media/audit 行；不兼容的 audit code fail（`A5_SCENARIO_FIELD_INVALID`），不得用任意 audit 行凑数。
- 终态/重复/拒绝规则的硬约束：
  - `axis=DUPLICATE` 或 `orderingClass` 为 `IDENTICAL_RETRY`/`CONFLICTING_RETRY`：`duplicateReplay.businessEffect` 必须 `ZERO`；duplicate step 不得新增任何成功业务事实（`domainWrites` 中除 contract-defined replay/idempotency record 外必须为 0，且该例外必须给 `basisRef`）；`externalCalls` 的重试请求计数可非 0，但必须有 `basisRef`。任何第二个 usage/settlement/grant/order/evidence/admission/activation fact => `A5_DUPLICATE_SIDE_EFFECT`。
  - `axis=PERMISSION_DENIED` 或 `orderingClass=PERMISSION_DENIED`：除 contract-defined denial/rejection/access facts 外，`overall.domainWrites` 必须与 `baseline` 相等。`denialAllowlist` 固定为：`audit.audit_fact`、`sharing.share_access_event`、`ivr.call_event`（仅 quarantine/rejection receipt）、`ivr.ivr_decision`（仅 frozen refusal）、`ivr.call_record`（仅 contract-defined terminal `REJECTED`）、`platform.idempotency_record`（仅 replay/conflict bookkeeping）；每个 allowlist 写入必须有 source ref，且不得是 success fact。任何 `entitlement_metering.usage_fact`、`entitlement_metering.usage_reservation` 的 HELD/SETTLED、`entitlement_metering.entitlement_grant`、`catalog_order.customer_order`/`payment_event`、`evidence.evidence_record`、`sharing.share_admission`、`activation_consent.service_activation` 接受态或 `subject_number.subject_claim` ACCEPTED 新增 => `A5_DENIED_WITH_BUSINESS_EFFECT`。
  - `axis=OUT_OF_ORDER`：最终 terminal state 必须沿合法 edge 收敛；不得出现 terminal regression；重复/迟到事实只允许 `IGNORED`/`QUARANTINED`，不得二次 settlement。
  - `axis=TIMEOUT`：`timingRef` 必须指向 contract/policy input；不得写裸秒数/SLO；超时后新业务事实只能是 contract-defined 的 release/quarantine/refusal。
  - `axis=RECOVERY`：cleanup 必须引用 authoritative reconciliation / restart / restore contract；不得删除 audit/evidence/terminal facts。

**EvidenceExpectation**：

```text
{
  audit: [{ auditRef:"A3:AUDIT:*", expectation:"EXACT"|"ZERO"|"BOUNDED", value?, max?, basisRef }],
  outbox: [{ outboxRef:"A0:OUTBOX:*", expectation:"EXACT"|"ZERO"|"BOUNDED", value?, max?, basisRef }],
  observability: [{
    metricCategoryRef: "DOC12:S12#IVR" | "#TRANSACTION" | "#EVIDENCE" |
                       "#SHARING" | "#ASYNC" | "#EXTERNAL_DEPENDENCY",
    correlationRefs: string[],
    assertion: "CORRELATED_BY_TRACE_ID"|"NO_FORBIDDEN_PAYLOAD"
  }],
  logs: [{ boundaryRef:"A3:TB:TB7", redactionRef:"A3:INV-A3-15", assertion:"NO_DENYLISTED_FIELD" }],
  accessEvents: [{ auditRef:"A3:AUDIT:*", expectation, value?, max?, basisRef }],
  zeroBasisRefs: string[]                      // 任一类别为空时必填，证明“预期为零”
}
```

- 不发明 metric 名称或 log message ID；只用 `docs/12` §12 的六个 metric category + A3 的 `TB7`/redaction invariant。精确 dashboard/label/retention 仍由 Observability/Operations owner 承接。
- `audit`/`outbox` 引用必须由 A3/A0 resolver 解析；`ZERO` 也必须给出 `basisRef`（例如 replay/denied invariant），不能省略。
- 空数组只表示“该类没有可枚举的 ref”，必须由 `zeroBasisRefs` 覆盖；否则 fail。
- `A0:OUTBOX:*` 只允许 A0 §10.17/§10.24 已冻结 event type；`A0:EVIDENCE-EVENT:*` 同理。

### 4.7 Owner、execution tier 与 result semantics

**Owner registry（closed A5 responsibility registry；OQ-02）**

Registry 是 model root `owners[]` 的封闭集合；它只定义**验收责任 principal**，不是 production IAM role、auth role 或新的组织角色。Registry 同时约束 `scenario.owner`、`scenario.evidenceOwner` 与每个 `A5-RES-*` residual 的 owner；上游 Q/A3/A4 行的 owner/destination 原文不被重命名、不被 A5 重新拥有。

Registry entry 固定形状：

```text
{
  ownerId, kind: "MODULE" | "RESPONSIBILITY" | "DELEGATED_SET",
  acceptanceOnly: true,
  upstream: [{
    ref,                       // 可解析 source ref
    field: "owner" | "ownerDestination" | "rowSet" | "section",
    exactText?,                // 上游字段/cell 的完整原文（WHOLE_CELL 时必须）
    match: "WHOLE_CELL" | "ROW_SET"
  }],
  scope                        // 验收责任范围；不是 IAM/auth/组织角色
}
```

匹配与解析规则（fail-closed，不做 slash 组件猜测）：

1. `ownerId` 唯一；`owners[]` 之外没有 implicit owner。`canonicalOwnerText(s)` 只做 `trim`、把连续空白折叠为单个空格、移除 `/` 两侧空白；**不**拆分 `/`、不做大小写折叠、不做 substring/component 匹配。
2. `kind=MODULE`：`ownerId` 必须等于 A0 的 12 个 owning schema/role ID 之一（`customer-iam`、`organization-access`、`operations-iam`、`subject-number`、`catalog-order`、`entitlement-metering`、`activation-consent`、`ivr`、`evidence`、`sharing`、`platform-reliability`、`operations-audit`）。`upstream` 至少一条 `A0:SM:*`/`A0:TABLE:*` owner 字段 exact 匹配。
3. `kind=RESPONSIBILITY`：至少一条 `match=WHOLE_CELL` mapping；`canonicalOwnerText(exactText) == canonicalOwnerText(ownerId)`，且该 ref 解析到的 owner/destination 字段确实包含该完整文本。0 条或文本不匹配 => `A5_OWNER_UPSTREAM_UNRESOLVED`。禁止从 `A/B/C` 字符串里推断 `A`、`B` 或 `B/C`。
4. `kind=DELEGATED_SET`：仅允许 `A5-UPSTREAM-DELEGATED`，只用于“A5 不重新拥有、原样委托上游 owner”的 residual；`upstream` 必须是 `ROW_SET`/section ref，validator 断言该 row set 的 owner/destination 非空且数量与 §2.3 一致。不得作为 `scenario.owner`/`evidenceOwner`。
5. 缺失 `scenario.owner`/`evidenceOwner` => `A5_EVIDENCE_OWNER_MISSING`；不在 registry => `A5_EVIDENCE_OWNER_UNKNOWN`。`evidenceOwner` 可等于 owner；delegation 只有 approved delegation 才允许（静态阶段无记录，故默认必须相等）。
6. residual owner 必须解析到 registry；缺失 => `A5_RESIDUAL_OWNER_MISSING`，未知 => `A5_RESIDUAL_OWNER_UNKNOWN`。
7. mapping 的 ref 解析到 0/multiple 行 => `A5_OWNER_UPSTREAM_UNRESOLVED`；registry 形状/重复/kind 非法 => `A5_OWNER_UPSTREAM_UNRESOLVED` 或 `A5_DUPLICATE_ID`。

封闭 registry（计划基线；实现时逐条落盘）：

| ownerId | kind | upstream mapping（ref / field / exact canonical text） | 验收责任 |
| --- | --- | --- | --- |
| `customer-iam` | MODULE | `A0:SM:Customer account` owner=`customer-iam` | 客户身份/workspace |
| `organization-access` | MODULE | `A0:SM:Organization` owner=`organization-access` | 组织/成员 |
| `operations-iam` | MODULE | `A0:SM:Employee account` owner=`operations-iam` | 员工身份 |
| `subject-number` | MODULE | `A0:SM:Subject claim` owner=`subject-number` | subject/number/risk |
| `catalog-order` | MODULE | `A0:SM:Customer order` owner=`catalog-order` | catalog/order/payment |
| `entitlement-metering` | MODULE | `A0:SM:Usage reservation` owner=`entitlement-metering` | entitlement/usage |
| `activation-consent` | MODULE | `A0:SM:Service activation` owner=`activation-consent` | activation/consent/trial |
| `ivr` | MODULE | `A0:SM:Call record` owner=`ivr` | IVR task/decision/call |
| `evidence` | MODULE | `A0:SM:Recording` owner=`evidence` | recording/media/evidence |
| `sharing` | MODULE | `A0:SM:Share grant` owner=`sharing` | share grant/admission |
| `platform-reliability` | MODULE | `A0:SM:Outbox event` owner=`platform-reliability` | idempotency/outbox |
| `operations-audit` | MODULE | `A0:TABLE:audit.audit_fact` owner=`operations-audit` | audit/append-only |
| `Security/Backend` | RESPONSIBILITY | `A0:Q:Q1` owner=`Security/Backend`；`A3:RESIDUAL:R-A3-NONCE-STORE` owner=`Security/Backend` | security acceptance co-lead |
| `Infrastructure/Security` | RESPONSIBILITY | `A0:Q:Q11` owner=`Infrastructure/Security`；`A3:RESIDUAL:R-A3-KMS-CAPABILITY` owner=`Infrastructure/Security` | deployment/cloud/evidence storage lead |
| `Product/Security/Operations` | RESPONSIBILITY | `A3:RESIDUAL:R-A3-SUPPORT-VIEW` owner=`Product/Security/Operations` | acceptance authority / launch governance |
| `Telephony/IVR` | RESPONSIBILITY | `A0:Q:Q7` owner=`Telephony/IVR`；`A4:CLOSEOUT:1` owner=`Telephony / IVR` | line/IVR acceptance |
| `Backend/QA` | RESPONSIBILITY | `A4:CLOSEOUT:9` owner=`Backend / QA` | QA/evidence honesty |
| `Operations/Backend` | RESPONSIBILITY | `A0:Q:Q13` owner=`Operations/Backend`；`A3:RESIDUAL:R-A3-AUDIT-VOLUME` owner=`Operations/Backend` | runbook/observability |
| `Telephony/IVR/Security` | RESPONSIBILITY | `A3:RESIDUAL:R-A3-MTLS-CAPABILITY` owner=`Telephony/IVR/Security` | line security |
| `Platform/CI` | RESPONSIBILITY | `A4:CLOSEOUT:10` owner=`Platform / CI` | CI enforcement |
| `Product/Legal` | RESPONSIBILITY | `A0:Q:Q10` owner=`Product/Legal` | legacy/contract scope |
| `A5-UPSTREAM-DELEGATED` | DELEGATED_SET | `DOC14:S14` rowSet（28 行）；`DOC12:S18` | 原样委托上游 owner；residual only |

计划中不再使用、且不能用自由文本新建的标签：

- `Security/Operations` → 由 `Infrastructure/Security` 承接 deployment/evidence-storage 验收责任；
- `Operations/Observability` → 由 `Operations/Backend` 承接（A0 Q13 的 observability destination）；
- `QA/Architecture`、bare `QA`、`QA/Review` → 由 `Backend/QA` 承接；
- `Database` → 不是 A5 principal；需要 DB ownership 时引用 upstream whole `Platform/Database`（`A0:Q:Q17`）或 `Backend / Platform / Database / Security`（`A4:CLOSEOUT:5`）；
- `Platform/CI` → 保留，但只映射 `A4:CLOSEOUT:10`；
- `Product/Operations` / `Product/Legal/Operations` / `Platform/Telephony/Security` → 分别由 `Product/Security/Operations`、`Product/Legal`、`Telephony/IVR/Security` 承接。

`scenario.service` 必须解析到 A0 owning module 或 A3 `serviceIdentities`（`core-api`/`async-worker`/`media-ai-worker`/`ivr-gateway`/`media-gateway` 等），不得为自由文本或新服务；`FAM-DEPLOYMENT`/`PATH-DEPLOY-ACCEPTANCE` 例外为 non-runtime tooling ID `deployment/capacity`（不是生产服务）。

**Execution tiers**（精确集合，不做线性等级假设）：

- `STATIC_SPEC`：A0-A4 validator/document contract 检查；只能产出 `SPEC_VERIFIED`。
- `MODEL_FIXTURE`：A2/A3/A4 deterministic fixture evaluator；只能产出 `SPEC_VERIFIED`，不是 runtime evidence。
- `INTEGRATION`：真实服务/DB/queue/cache/auth/payment adapter 环境。
- `LINE`：真实 carrier/SIP/IVR/B-leg/recording 线路。
- `CLOUD`：真实 COS/KMS/Secret Manager/CAM/audit 能力。
- `MANUAL`：人工/法律/运营/发布权威记录。

Scenario root 的 `requiredEvidenceTiers` 是**必须全部满足**的集合（唯一权威副本）；evidence manifest 的 `evidenceTiers` 必须包含它们。`STATIC_SPEC`/`MODEL_FIXTURE` 不能替代任何 runtime tier。

**Result semantics**：

```text
Scenario result partition:
  specification: SPEC_DEFINED | SPEC_INVALID
  execution:     NOT_RUN | EVIDENCE_SUBMITTED | EVIDENCE_REJECTED |
                 EVIDENCE_VALID | PASS | FAIL | BLOCKED | NOT_APPLICABLE
  accepted:      false until an authorized acceptance record exists
```

- `NOT_APPLICABLE` 只允许来自 coverage 行的 registered N/A，不允许 scenario 自声明。
- `PASS` 是 acceptance 结果，不是 manifest observation；manifest 不包含 `result`/`PASS` 字段（见 §7.4）。
- `SPEC_VERIFIED` 是静态证据状态，不是 execution 状态；不得用来填充 `PASS`。

### 4.8 时间戳与不可变 evidence 引用规则

- `asOf` / `executedAt` / `recordedAt` / `acceptedAt` / `capturedAt` 全部为 UTC ISO-8601（`Z`）；validator 不读 wall clock，只用输入 `asOf` 比较。
- 顺序：`executedAt <= recordedAt <= acceptedAt`；`capturedAt` 必须落在 `[executedAt, recordedAt]`；未来时间 => `A5_EVIDENCE_TIME_INVALID`。
- `evidenceRef` object：

```text
{
  artifactId, role: "AUDIT_EXPORT"|"OUTBOX_EXPORT"|"DB_STATE_EXPORT"|
                     "METRIC_SNAPSHOT"|"LOG_EXPORT"|"LINE_CDR"|
                     "CLOUD_EVIDENCE"|"MANUAL_RECORD"|"TEST_REPORT",
  locatorRef,                    // opaque, non-secret; 禁止 URL/credential/object key
  sha256,                        // lowercase 64-hex
  capturedAt, producerRef, redactionRef
}
```

- `locatorRef` 不得含 `?` query、permanent URL、COS key/bucket/version、token、credential、Authorization header 或明文 PII；违反 => `A5_EVIDENCE_REF_INVALID`。
- `sha256` 必须匹配 `^[0-9a-f]{64}$`；artifact digest 与 manifest 声明不一致 => `A5_EVIDENCE_DIGEST_MISMATCH`。
- `manifestDigest` 由 canonical JSON 计算（排除自身）；acceptance record 引用 manifestDigest。同一 manifestId 出现两个不同 digest => `A5_EVIDENCE_IMMUTABILITY_VIOLATION`。
- A5 只校验引用完整性，不存储证据、不证明外部存储的 WORM/retention/authorization；该能力保持 residual（§12.5）。

### 4.9 Source reference 语法与 resolver

| Prefix | 解析来源 | 有效示例 |
| --- | --- | --- |
| `A0:TABLE:<schema.table>` | `docs/14` `<!-- catalog:start/end -->` 44 表精确名；owner 字段取 catalog owner | `A0:TABLE:ivr.call_record` |
| `A0:INV:<INV-xx>` | `docs/14` §6.1 invariant 行 | `A0:INV:INV-19` |
| `A0:SM:<aggregate>` | `docs/14` §7.1/§7.3/§7.4 parsed state-machine row（§4.9.1） | `A0:SM:Usage reservation` |
| `A0:SM:<aggregate>:<from>-><to>` | §4.9.1 frozen parser 产出的 directed edge；**只用于 `transitionRef`** | `A0:SM:Share grant:ACTIVE->REVOKED` |
| `A0:TX:<T-Sx-yy>` | `docs/14` §9 小节标题；**只作 basis/source ref** | `A0:TX:T-S2-12` |
| `A0:OUTBOX:<eventType>` | `docs/14` §10.17 Outbox identity 表 | `A0:OUTBOX:FIRST_TRIAL_GRANTED` |
| `A0:EVIDENCE-EVENT:<eventType>` | `docs/14` §10.24 evidence lifecycle/recording Outbox 表 | `A0:EVIDENCE-EVENT:EVIDENCE_CREATED` |
| `A0:Q:<Qx>` | `docs/14` §14 open-input row；owner/destination 字段 | `A0:Q:Q13` |
| `A1:OP:<operationId>` | A1 bundle operationId | `A1:OP:ingestIvrCallEvent` |
| `A1:PROBLEM:<CODE>` | A1 bundle `ProblemCode.enum` / `x-onecall-error-codes` | `A1:PROBLEM:CONSENT_REQUIRED` |
| `A2:MSG:<messageId>` | A2 model `messages` | `A2:MSG:call-event.request` |
| `A2:EVENT:<eventType>` | A2 model `eventTypes` | `A2:EVENT:CALL_TERMINATED` |
| `A2:IDENTITY:<identity>` | A2 model `identities` | `A2:IDENTITY:eventId` |
| `A2:INV:<INV-A2-xx>` | A2 model `invariants` | `A2:INV:INV-A2-08` |
| `A2:ACTION:<value>` / `A2:REASON:<value>` | A2 model `actions`/`decisionReasons` | `A2:ACTION:REJECT_CALL_IN_PROGRESS` |
| `A2:PROCESSING:<value>` | A2 model `decisionFields`/event processing enum（`APPLIED/IGNORED/QUARANTINED/RECEIVED`） | `A2:PROCESSING:APPLIED` |
| `A2:STATE:<machine>:<from>-><to>` | A2 model `stateMachines`/`recordingLifecycle` | `A2:STATE:call_record:ACTIVE->ENDED` |
| `A2:FIX:<fixtureId>` | `protocol/ivr/v1/fixtures/*.json` `id` | `A2:FIX:s2-02-event-duplicate-replay` |
| `A3:TS:<TSx>` / `A3:TB:<TBx>` / `A3:AUTH:<domain>` | A3 model arrays | `A3:TB:TB7` |
| `A3:SERVICE:<id>` / `A3:CRED:<Cxx>` | A3 model arrays | `A3:CRED:C09` |
| `A3:INV:<INV-A3-xx>` / `A3:KEY:<Kxx>` / `A3:DATA:<Dxx>` / `A3:CV:<CV-x>` / `A3:MEDIA:<U1|P1>` | A3 model arrays / deferred inputs / mediaPolicies | `A3:INV:INV-A3-12`、`A3:CV:CV-4`、`A3:MEDIA:U1` |
| `A3:AUDIT:<actionCode>` / `A3:HIGH-RISK:<operationId>` / `A3:COVERAGE:<callClass>` | A3 model audit/high-risk/coverage rows | `A3:AUDIT:SUBJECT_SUSPEND` |
| `A3:DEFERRED:<Qx/CV-x>` | A3 model `deferredInputs`（23 行） | `A3:DEFERRED:Q25` |
| `A3:RESIDUAL:<R-A3-*>` | **`docs/17` §20 residual table（18 行，lines 692–709）**，不是 A3 model | `A3:RESIDUAL:R-A3-AUDIT-VOLUME` |
| `A3:FIX:<fixtureId>` | `security/v1/fixtures/*.json` `id`（23 个） | `A3:FIX:sec-s2-04-ivr-signature-invalid` |
| `A4:GROUP:<groupId>` / `A4:FORMULA:<F-*>` / `A4:FIX:<fixtureId>` | A4 `calc.mjs` exported `S2_GROUP_IDS` + `compute(inputs.template.json)` output recursively collected `formulaId` + fixture file `fixture` | `A4:FORMULA:F-BACKUP-01` |
| `A4:CONSTANT:<name>` / `A4:FLAG:<flag>` / `A4:CAPABILITY:<id>` | A4 root constants/flags；capability exact set 从 `docs/18` §10.1 解析并核对 6 项 | `A4:CONSTANT:cloudVerified`、`A4:CAPABILITY:CV-4` |
| `A4:SECTION:<n>` / `A4:RESIDUAL:<index>` | `docs/18` heading / §10.3 residual row index（16 行） | `A4:SECTION:10.3`、`A4:RESIDUAL:1` |
| `A4:CLOSEOUT:<index>` | A4 final closeout §Residual risks and destinations row（10 行） | `A4:CLOSEOUT:9` |
| `DOC12:S12#<CATEGORY>` / `DOC12:S13` / `DOC12:S17.1` / `DOC12:S18` | `docs/12` §12/§13/§17.1/§18 item 6 | `DOC12:S12#IVR`、`DOC12:S18` |
| `DOC14:S14` | `docs/14` §14 open-input row set（28 行） | `DOC14:S14` |

Resolver 规则：

- A0/A1/A2/A3/A4 每个 resolver 先断言固定计数/摘要（§2.3）；计数漂移 => `A5_SOURCE_DRIFT`，不继续用陈旧解析。A0 state-row 解析必须得到 31 行（12/13/6）；A3 必须得到 `deferredInputs=23`、`docs/17` residual=18、fixtures=23；A0 §14 Q=28；A4 §10.3 residual=16、closeout residual=10。
- 同一 ref 解析到 0 行 => `A5_SOURCE_REF_UNRESOLVED`；多行 => `A5_SOURCE_REF_AMBIGUOUS`。
- `A3:DEFERRED` 只从 A3 model `deferredInputs` 解析；`A3:FIX` 只从 A3 fixtures 解析；两者语义不变。`A3:RESIDUAL` 只从 `docs/17` §20 解析，并断言 18 行 ID 唯一、owner/destination 非空、A3 model 内 `R-A3` 出现次数为 0。正例 `A3:RESIDUAL:R-A3-AUDIT-VOLUME` 解析 1 行；负例 `A3:RESIDUAL:R-A3-NOPE` => `A5_SOURCE_REF_UNRESOLVED`。
- A0 state/edge 解析严格按 §4.9.1；A2 用 structured transitions；解析失败 fail closed，不回退到自由文本猜测。
- A0 §10.17/§10.24 的 `A or B` event-type cell 展开为两个独立 ID（如 `USAGE_SETTLED`、`USAGE_RELEASED`）；不把整格当作单个 ID。
- 每个 scenario 至少含一个 state/invariant ref（`A0:SM:*`/`A0:INV:*`/`A2:INV:*`/`A2:STATE:*`/`A3:INV:*`）与一个 verification anchor（`A1:OP`/`A1:PROBLEM`/`A2:EVENT`/`A2:FIX`/`A3:*`/`A4:*`）；`FAM-DEPLOYMENT`/`PATH-DEPLOY-ACCEPTANCE` 例外：必须含 `A4:*` + `DOC12:S13`。
- 每个 scenario 必须给 `outcomeRefs`（非空、`⊆ sourceRefs`）；path scenario 还必须满足 §8.3 的 paired-family 唯一性。
- refs 不复制 A0-A4 的完整表格、错误集合或状态定义；validator 不把 A5 的期望当作新的语义源，只检查其引用的 ID/值是否存在且允许。

#### 4.9.1 A0 state/edge parser（frozen；S1 必须逐项实现）

输入：`docs/14` §7.1（12 行）、§7.3（13 行）、§7.4（6 行），恰 31 行。Parser 是 A0 真实表格形状的**专用**解析，不是通用 DSL 或自由文本推断。

1. **行形状**：前四列固定为 `Aggregate`/`Allowed states`/`Allowed transitions and guards`/`Owner`。只有 `Call record` 允许第 5 列 attached note；其他行 >4 列、缺列、header 改变 => `A5_A0_STATE_SHAPE_UNRECOGNIZED`。31 行/aggregate 唯一/12+13+6 分布漂移 => `A5_SOURCE_DRIFT`。
2. **Allowed states tokenization**：
   - 先匹配第 4 条的 closed row-shape classification；命中则返回该 classification 的 states/derivedStates/edges。
   - 否则扫描单元格：括号内 balanced `(...)` 整体是一个 token，不按内部逗号拆分；括号外按 `,` 拆分；trim 空白与 backticks。
   - `(...)` token 的判定：若紧跟在一个 backticked plain state 之后、且括号内容**不含顶层逗号**（如 `(payment dimension only)`、`(derived ...)`），它是该 state 的 annotation，不单独成为 state；若括号内容含顶层逗号（如 `(owner null, owner null)`、`(account, null)`），它是 composite tuple state literal（保留内部空格/逗号原文）。
   - plain token 必须是 `^[A-Z][A-Z0-9_]*$`；若 token 形如 `<backticked state> (derived ...)`，state 取该 backticked literal，括号内全部 backticked 名进入 `derivedStates`，classification `DERIVED_IMMUTABLE`；若 token 是 `<backticked state>` 后跟非 derived 的尾注（如 `(payment dimension only)` 或 `fixed at insert`），只取 backticked literal，尾注不作为 state。
   - Allowed-states cell 自身含 `→`/`↔`（Direct authorization）时：截断 ` or ...` prose，按箭头拆分，两侧必须是 state literal；生成 directed edge（`↔` 双向），classification `STATE_CELL_ARROW`。
   - 其他无法识别的 token、括号不平衡或空 state set（非 classification 行）=> `A5_A0_STATE_SHAPE_UNRECOGNIZED`。
3. **Edge extraction（enum / STATE_CELL_ARROW 行）**：
   - 从 `Allowed transitions and guards` 中所有 `→`/`↔` 出现处提取；箭头前的 contiguous backticked/slash-combined group 与箭头后的 group 各自展开（`ACTIVE`/`SUSPENDED` = 两个 source；`ENDED/FAILED/UNKNOWN` = 三个 target）；笛卡尔积生成 directed edge；`↔` 双向；self-edge 仅在原文显式箭头为 `X → X` 时接受（如 `VERIFIED → VERIFIED`、Share grant extension 的 `ACTIVE → ACTIVE`），不得由 prose 推断。
   - 每个 endpoint 必须是该行 Allowed states 成员。**一侧是 state、另一侧不是** => `A5_A0_STATE_SHAPE_UNRECOGNIZED`（ambiguity）。**两侧都不是 state** 的 arrow（Subject ownership 的 `null → personal account`）是 prose-only：忽略、不生成 edge、不 fail。
   - guard prose（`on ...`/`only through ...`/`only after ...`）不产生 state；同一 cell 的多个 arrow 全部处理（Catalog version 的 `ACTIVE → OFFSALE`）。
   - 非 arrow 的 prose transition（Call record 的 “may become REJECTED”）不生成 edge；scenario 只能使用 parser 冻结的 arrow edge 或 creation assertion，不得自行猜测。
   - edges 去重；输出按 `(from,to)` 字典序稳定排序。
4. **Closed row-shape classification**（aggregate + 下列 exact keyword phrase；cell 可含其他 guard prose，但 keyword 必须逐字符匹配，且 zero-arrow 类不得含未解释 arrow；覆盖 non-enum/same-as/frozen-result 等真实形状；不在表内的 arrowless/ambiguous row => `A5_A0_STATE_SHAPE_UNRECOGNIZED`）：

| Aggregate | 识别形状 | classification | states | derivedStates | edges |
| --- | --- | --- | --- | --- | --- |
| `Subject claim` | `Insert-only outcome; no transition` | `INSERT_ONLY` | `ACCEPTED`,`REJECTED`,`CONFLICT` | `[]` | `[]` |
| `Payment order` | `Same verified-evidence transitions as the order`（无显式 arrow，引用 Customer order） | `SHARED_EDGE_SET` | `UNPAID`,`UNKNOWN`,`PAID`,`CLOSED` | `[]` | 复制 `Customer order` 的 6 条 parsed edges（不复制状态语义源） |
| `Consent record` | `No transition; append-only` | `APPEND_ONLY` | `ACCEPT`,`DECLINE` | `[]` | `[]` |
| `Trial` | `GRANTED` (derived `EXHAUSTED`/`EXPIRED` ...) + `No row transition` | `DERIVED_IMMUTABLE` | `GRANTED` | `EXHAUSTED`,`EXPIRED` | `[]` |
| `IVR decision` | `Frozen purpose-specific result (no status transition)` | `FROZEN_RESULT` | `[]` | `[]` | `[]` |
| `Evidence creation cycle` | `other evidence events are append-only history` | `APPEND_ONLY_HISTORY` | `[]` | `[]` | `[]` |
| `Share admission and derived session` | `Admission is an immutable one-time fact` + `derived, not a PostgreSQL state` | `DERIVED_FACT` | `[]` | `[]` | `[]` |
| `Share access event` | `Append-only success/denied/failed/ignored outcomes` | `APPEND_ONLY_OUTCOMES` | `[]` | `[]` | `[]` |
| `Subject ownership` | composite tuple states + prose-only arrows | `PROSE_PARENTHESIZED` | `(owner null, owner null)`,`(account, null)`,`(null, organization)` | `[]` | `[]` |
| `Direct authorization` | `NONE → AUTHORIZED` or a frozen refusal | `STATE_CELL_ARROW` | `NONE`,`AUTHORIZED` | `[]` | `NONE->AUTHORIZED` |

`SHARED_EDGE_SET` 只允许 `Payment order`：它在 A0 中明确写 “Same verified-evidence transitions as the order”，故 parser 先解析 `Customer order`，再从同一 frozen parse 复制 6 条 edges，并要求 states 集合相等；依赖无环；A5 不复制状态语义定义、不新增 edge。任何其他 “same as …” row 若无此 closed entry => `A5_A0_STATE_SHAPE_UNRECOGNIZED`。

5. **Derived states** 只存在于 `derivedStates`；永远不是 mutable row state，不得作为 `A0:SM` value 或 transition endpoint（违反 => `A5_TRANSITION_REF_INVALID`）。需要表达 Trial 过期结果时，只能引用 policy/time/read model，不能把 `EXPIRED` 写成 A0 state。
6. **Frozen expected map**：S1 fixture `s1-03-a0-state-shapes.json` 对代表行（Call record、Organization、Organization membership、Catalog version、Customer order、Payment order、Subject ownership、Trial、Subject claim、IVR decision、Direct authorization）逐项比较 `states`/`edges`/`derivedStates`/`classification`/attached note。与 parser 输出不一致 => `A5_SOURCE_DRIFT`；无法解析/歧义 => `A5_A0_STATE_SHAPE_UNRECOGNIZED`。A0 文档本身不得修改。

### 4.10 一个完整 scenario 示例（形状基线，不是运行时值）

```json
{
  "scenarioId": "A5-IVR-DUPLICATE-01",
  "coverageUnitId": "FAM-IVR",
  "axis": "DUPLICATE",
  "title": "Duplicate terminal event after absorption",
  "owner": "ivr",
  "aggregateRef": "A0:SM:Call record",
  "service": "ivr",
  "requiredEvidenceTiers": ["INTEGRATION"],
  "sourceRefs": [
    "A0:INV:INV-19",
    "A0:SM:Call record",
    "A0:SM:Usage reservation",
    "A2:EVENT:CALL_TERMINATED",
    "A2:IDENTITY:eventId",
    "A2:IDENTITY:terminal fact",
    "A2:INV:INV-A2-08",
    "A2:FIX:s2-02-event-duplicate-replay",
    "A1:OP:ingestIvrCallEvent"
  ],
  "outcomeRefs": ["A2:INV:INV-A2-08", "A0:INV:INV-19"],
  "preconditions": {
    "startingState": [
      {"targetRef": "A0:SM:Call record", "field": "status", "op": "EQUALS", "value": "ACTIVE", "basisRefs": ["A0:TX:T-S2-12"]},
      {"targetRef": "A0:SM:Usage reservation", "field": "status", "op": "EQUALS", "value": "HELD", "basisRefs": ["A0:INV:INV-19"]}
    ],
    "fixtureRefs": ["A2:FIX:s2-02-event-duplicate-replay"],
    "environmentRefs": ["A3:TS:TS3", "A4:GROUP:ivrLines"],
    "assumptionRefs": ["A3:CRED:C04"]
  },
  "stimulus": {
    "orderingClass": "IDENTICAL_RETRY",
    "steps": [
      {
        "stepId": "terminal-1",
        "actorRef": "A3:SERVICE:ivr-gateway",
        "operationRef": "A1:OP:ingestIvrCallEvent",
        "eventRef": "A2:EVENT:CALL_TERMINATED",
        "identityRefs": ["A2:IDENTITY:eventId"],
        "expectedState": [
          {"targetRef": "A0:SM:Call record", "field": "status", "op": "EQUALS", "value": "ENDED", "transitionRef": "A2:STATE:call_record:ACTIVE->ENDED", "basisRefs": ["A0:TX:T-S2-12"]},
          {"targetRef": "A0:SM:Usage reservation", "field": "status", "op": "EQUALS", "value": "SETTLED", "transitionRef": "A2:STATE:usage_reservation:HELD->SETTLED", "basisRefs": ["A0:TX:T-S2-05"]}
        ],
        "expectedDelta": {
          "domainWrites": [{"tableRef": "A0:TABLE:entitlement_metering.usage_fact", "mode": "EXACT", "value": 1, "basisRef": "A2:INV:INV-A2-04"}],
          "audit": [],
          "outbox": [{"outboxRef": "A0:OUTBOX:USAGE_SETTLED", "mode": "EXACT", "value": 1, "basisRef": "A0:TX:T-S2-05"}],
          "accessEvents": [],
          "externalCalls": [{"operationRef": "A1:OP:ingestIvrCallEvent", "mode": "EXACT", "value": 1, "basisRef": "A2:FIX:s2-02-event-duplicate-replay"}]
        }
      },
      {
        "stepId": "terminal-1-retry",
        "actorRef": "A3:SERVICE:ivr-gateway",
        "operationRef": "A1:OP:ingestIvrCallEvent",
        "eventRef": "A2:EVENT:CALL_TERMINATED",
        "identityRefs": ["A2:IDENTITY:eventId"],
        "expectedState": [
          {"targetRef": "A0:SM:Call record", "field": "status", "op": "UNCHANGED", "value": "ENDED", "basisRefs": ["A2:INV:INV-A2-05"]},
          {"targetRef": "A0:SM:Usage reservation", "field": "status", "op": "UNCHANGED", "value": "SETTLED", "basisRefs": ["A0:INV:INV-19"]}
        ],
        "expectedDelta": {
          "domainWrites": [{"tableRef": "A0:TABLE:entitlement_metering.usage_fact", "mode": "EXACT", "value": 0, "basisRef": "A2:INV:INV-A2-08"}],
          "audit": [],
          "outbox": [],
          "accessEvents": [],
          "externalCalls": [{"operationRef": "A1:OP:ingestIvrCallEvent", "mode": "EXACT", "value": 1, "basisRef": "A2:FIX:s2-02-event-duplicate-replay"}]
        }
      }
    ],
    "faultInjection": {"kind": "DUPLICATE_DELIVERY", "parameterRef": "A2:IDENTITY:eventId"}
  },
  "expected": {
    "persisted": [
      {"targetRef": "A0:SM:Call record", "field": "status", "op": "EQUALS", "value": "ENDED", "basisRefs": ["A2:INV:INV-A2-05"]},
      {"targetRef": "A0:SM:Usage reservation", "field": "status", "op": "EQUALS", "value": "SETTLED", "basisRefs": ["A0:INV:INV-19"]}
    ],
    "external": [
      {"operationRef": "A1:OP:ingestIvrCallEvent", "field": "processingStatus", "op": "EQUALS", "valueRef": "A2:PROCESSING:APPLIED", "basisRefs": ["A2:FIX:s2-02-event-duplicate-replay"]}
    ],
    "terminalAbsorption": {
      "terminalStates": ["ENDED", "FAILED", "REJECTED"],
      "postTerminalOutcome": "ABSORB",
      "sourceRefs": ["A2:INV:INV-A2-05", "A0:INV:INV-19"]
    },
    "idempotency": {
      "identityRef": "A2:IDENTITY:eventId",
      "sameFingerprintOutcome": "FIRST_RESULT",
      "changedFingerprintOutcome": "CONFLICT",
      "sourceRefs": ["A2:INV:INV-A2-08", "A1:PROBLEM:EVENT_ID_CONFLICT"]
    },
    "effects": {
      "baseline": {"domainWrites": [], "audit": [], "outbox": [], "accessEvents": [], "externalCalls": []},
      "steps": [
        {"stepId": "terminal-1", "delta": {"domainWrites": [{"tableRef": "A0:TABLE:entitlement_metering.usage_fact", "mode": "EXACT", "value": 1, "basisRef": "A2:INV:INV-A2-04"}], "audit": [], "outbox": [{"outboxRef": "A0:OUTBOX:USAGE_SETTLED", "mode": "EXACT", "value": 1, "basisRef": "A0:TX:T-S2-05"}], "accessEvents": [], "externalCalls": [{"operationRef": "A1:OP:ingestIvrCallEvent", "mode": "EXACT", "value": 1, "basisRef": "A2:FIX:s2-02-event-duplicate-replay"}]}},
        {"stepId": "terminal-1-retry", "delta": {"domainWrites": [{"tableRef": "A0:TABLE:entitlement_metering.usage_fact", "mode": "EXACT", "value": 0, "basisRef": "A2:INV:INV-A2-08"}], "audit": [], "outbox": [], "accessEvents": [], "externalCalls": [{"operationRef": "A1:OP:ingestIvrCallEvent", "mode": "EXACT", "value": 1, "basisRef": "A2:FIX:s2-02-event-duplicate-replay"}]}}
      ],
      "overall": {"domainWrites": [{"tableRef": "A0:TABLE:entitlement_metering.usage_fact", "mode": "EXACT", "value": 1, "basisRef": "A2:INV:INV-A2-04"}], "audit": [], "outbox": [{"outboxRef": "A0:OUTBOX:USAGE_SETTLED", "mode": "EXACT", "value": 1, "basisRef": "A0:TX:T-S2-05"}], "accessEvents": [], "externalCalls": [{"operationRef": "A1:OP:ingestIvrCallEvent", "mode": "EXACT", "value": 2, "basisRef": "A2:FIX:s2-02-event-duplicate-replay"}]},
      "zeroOrBoundedBasisRefs": ["A2:INV:INV-A2-08", "A0:INV:INV-19"]
    },
    "evidence": {
      "audit": [],
      "outbox": [{"outboxRef": "A0:OUTBOX:USAGE_SETTLED", "expectation": "EXACT", "value": 1, "basisRef": "A0:TX:T-S2-05"}],
      "observability": [{"metricCategoryRef": "DOC12:S12#IVR", "correlationRefs": ["A0:TABLE:ivr.call_event"], "assertion": "CORRELATED_BY_TRACE_ID"}],
      "logs": [{"boundaryRef": "A3:TB:TB7", "redactionRef": "A3:INV-A3-15", "assertion": "NO_DENYLISTED_FIELD"}],
      "accessEvents": [],
      "zeroBasisRefs": ["A2:INV:INV-A2-08"]
    },
    "cleanup": {"mode": "NONE", "steps": [], "sourceRefs": ["A2:INV:INV-A2-08"]}
  },
  "duplicateReplay": {"ofStepId": "terminal-1", "businessEffect": "ZERO", "responseOutcome": "FIRST_RESULT"},
  "evidenceOwner": "ivr",
  "acceptanceAuthorityRef": null,
  "resultSemantics": {"specResult": "SPEC_DEFINED", "runtimeResult": "NOT_RUN", "acceptanceRuleRef": "A5_ACCEPT_RUNTIME_TIER_AND_AUTHORITY", "failConditionRefs": ["A2:INV:INV-A2-04", "A2:INV:INV-A2-08"]},
  "residualIds": []
}
```

示例中的 zero-audit 由 `evidence.zeroBasisRefs` 覆盖；实现时不得把不适用的 audit code 硬塞进 IVR 场景。Validator 要求每个 auditRef 真实存在且与 scenario owner/trigger 兼容；否则用 `[]` + `zeroBasisRefs` 表示“没有该类证据”。`A5_ACCEPT_RUNTIME_TIER_AND_AUTHORITY` 是 A5 内置 acceptance rule ID，不是 source ref，不参与 resolver；validator 只接受固定 rule registry 中的值。示例把 `A2:STATE:*` 放在 step `expectedState` 的 state-changing assertion 上；`preconditions.startingState` 与最终 `expected.persisted` 不带 `transitionRef`。`requiredEvidenceTiers` 只在 root 出现一次；`outcomeRefs` 是 `sourceRefs` 的子集。若实现 `A0:SM:*` edge，必须写成 §4.9.1 parser 实际产出的 edge（如 `A0:SM:Share grant:ACTIVE->REVOKED`），不得使用 `A0:TX:*`。

## 5. 覆盖规则与反 count-only

1. `coverage` 必须恰含 16 × 6 = 96 行；`unitId`/`axis` 各在固定 registry；重复 => `A5_COVERAGE_CELL_DUPLICATE`；缺失 => `A5_COVERAGE_CELL_MISSING`；未知 => `A5_COVERAGE_CELL_ILLEGAL`。
2. `REPRESENTED` 必须有且仅有一个 scenarioId，且 scenario 的 `coverageUnitId/axis` 完全一致；orphan scenario => `A5_ORPHAN_SCENARIO`；N/A cell 绑定 scenario => `A5_APPLICABILITY_ILLEGAL`。
3. `NOT_APPLICABLE` 必须命中 §4.3 的 `(reasonId, unit, axis)` allowlist，且 `sourceRefs` 至少 2 个且全部解析；否则 `A5_NA_REASON_UNKNOWN` / `A5_NA_REASON_UNBACKED`。
4. `DEFERRED` 必须有 residualId、blockedBy、owner、destination；residual 必须存在且 owner/destination 非空；仍计入 gap（`specificationCoverageComplete=false`）。
5. `specificationCoverageComplete` 不是“96 行存在”的计数标志，而是以下独立 checks 的 AND：
   - `COVERAGE_COMPLETE`：96 行、无缺失/重复/非法、0 required deferred；
   - `SOURCE_INTEGRITY`：所有 scenario/coverage/residual refs 解析，无 drift/ambiguous；
   - `SCENARIO_SEMANTICS`：所有 mandatory fields、state edge、terminal absorption、idempotency、effect arithmetic、denial/duplicate/ordering 规则通过；
   - `EVIDENCE_HONESTY`：S1 检查 evidence owner/tier/result 结构合法、全部 runtime 为 `NOT_RUN`、无 caller/synthetic/count-only 升级；S2 再叠加 manifest/authority admission；
   - `DOC_PARITY`：所有 marker block 与 model 逐行一致；
   - `REGISTRY_BASELINE`：A0-A4 计数/摘要/ID set 与 §2.3 基线一致。
6. 任一 check 为 false，root `specificationCoverageComplete=false`，即使 fixture 计数、scenario 计数、source ref 计数全部“看起来完整”。fixture `all-counts-but-unbacked-na` 专门证明这一点。
7. `productionAccepted` 不是 `specificationCoverageComplete` 的别名；即使 specification 完整、所有 90 scenario 都 `SPEC_DEFINED`，没有 authorized runtime evidence + authority record 时仍为 false。
8. `A3:RESIDUAL:*` 必须解析 `docs/17` §20 的 18 行；0 行 => `A5_SOURCE_REF_UNRESOLVED`，多行/重复 ID => `A5_SOURCE_REF_AMBIGUOUS`，A3 model 内出现 `R-A3` => `A5_SOURCE_DRIFT`。`A3:DEFERRED`/`A3:FIX` 仍按 A3 model/fixtures 解析。
9. A0 state/edge 必须等于 §4.9.1 的 frozen parse 结果；31 行/12+13+6/aggregate 集合漂移 => `A5_SOURCE_DRIFT`；任何未识别 arrowless/ambiguous row => `A5_A0_STATE_SHAPE_UNRECOGNIZED`。A0 文档不得修改。
10. `owners[]` 是封闭 registry；scenario `owner`/`evidenceOwner` 与 residual owner 全部解析；0 上游 mapping 或 exact-text 不匹配 => `A5_OWNER_UPSTREAM_UNRESOLVED`；未知 residual owner => `A5_RESIDUAL_OWNER_UNKNOWN`。禁止 slash 组件推断或自由文本 owner。
11. `transitionRef` 只接受 §4.6 的 A0 edge/A2 state form；`A0:TX:*` 只能作 basis/source。`requiredEvidenceTiers` 只在 scenario root；`resultSemantics` 出现第二副本 => `A5_SCENARIO_FIELD_INVALID`。
12. 每个 path scenario 与同 axis 且状态为 `REPRESENTED` 的 paired family scenario 必须满足 `sourceRefs` 差集非空且 `outcomeRefs` 差集非空；否则 `A5_PATH_FAMILY_DUPLICATE`（paired family 为 N/A 时不做 ref 差集，但仍须是 boundary observation）。`PATH-DEPLOY-*` 必须是 A4→A5 acceptance-boundary observation。
13. `expected.external` 是 final authoritative projection；最后一个匹配 `(operationRef, field)` 的 step `expectedResponse` 必须与 root 一致，否则 `A5_EXTERNAL_PROJECTION_CONTRADICTION`。

## 6. 来源引用完整性与所有权

### 6.1 只引用，不复制

- A5 不复制 A0 的 44 表/40 invariants/31 state rows/29 transactions，不复制 A1 的 49 operations/59 Problem codes，不复制 A2 的 12 messages/11 events/15 invariants/25 fixtures，不复制 A3 的 21 invariants/39 audit events/20 keys/28 data classes，不复制 A4 的公式表/输入契约。
- A5 只在 model 的 `sourceBaseline` 保存**计数与摘要断言**，用于 drift detection；这些数字不是语义定义。
- 状态枚举、Problem code、event/event type、owner、audit code、capability ID、formula ID 全部由 resolver 从原 registry 读取；A5 不手写第二份 enum。
- A5 scenario 的 `expected` 值是对 source 允许 state/transition/error/evidence 的**断言**，不是新定义；值不在 source enum、edge 不存在或 error 不在 A1 registry 时 fail，不新增状态/错误。
- 任何 A0-A4 变更导致 resolver 计数/摘要/ID set 变化 => `A5_SOURCE_DRIFT`，A5 必须重新评审，而不是自动跟随。

### 6.2 所有权

- `owner`/`evidenceOwner` 必须是 §4.7 closed registry 中的 exact ID：MODULE 来自 A0 12 个 owning schema/role；RESPONSIBILITY 由显式 upstream mapping 支撑；`A5-UPSTREAM-DELEGATED` 只用于原样委托的 residual，不作 scenario owner。A5 不新增 team/role/IAM principal。
- `evidenceOwner` 是证据责任人，不是产品 owner 的替代；默认与 owner 相同，delegation 必须引用 approved delegation。
- `aggregateRef` 必须指向 A0 state machine 或 A2 state machine；`FAM-DEPLOYMENT` 与 `PATH-DEPLOY-ACCEPTANCE` 使用 `A4` gate/constant ref 作为 aggregate，不伪造 A0 aggregate。
- Cross-system path 的 owner 是 lead owner（通常 `platform-reliability` 或 path 起点 owner）；每个参与方仍需在 `sourceRefs`/effect owner 中出现，不能由 lead 一人吞掉。Path owner 也必须解析 registry。

### 6.3 与 legacy `docs/04` 的关系

- `docs/04` 只作为产品场景/vocabulary inventory；A0-A4 accepted contract 与其冲突时以 A0-A4 为准。
- 不把 `docs/04` 的 AI/notary/refund/invoice/线下公证书/工单场景纳入 matrix；它们没有 accepted upstream production contract，保持 deferred（§12.5）。
- `docs/04` 的 `ACC/PHN/EVI/SUB/ENT/CRT/NPS/OPS/NFR` 编号不成为 A5 scenario ID；A5 ID 只由 §4.1 生成。

## 7. 验收结果状态机与 fail-closed 规则

### 7.1 业务状态与证据状态分离

- A0/A1/A2/A3/A4 的业务状态（`ACTIVE/ENDED/CLAIMED/SETTLED/VALIDATED/...`）只由 source resolver 提供；A5 不新增业务状态。
- A5 的 `NOT_RUN/EVIDENCE_SUBMITTED/EVIDENCE_VALID/PASS/...` 是**验收证据状态**，不写入任何产品表、不改变事件/Problem/状态机。
- 终态吸收、幂等、副作用断言使用业务状态机；证据状态机只决定“这个 scenario 是否被证据覆盖”。

### 7.2 Scenario evidence state machine

| From | To | 条件 | 失败/拒绝 |
| --- | --- | --- | --- |
| `NOT_RUN` | `EVIDENCE_SUBMITTED` | manifest 存在，schema/digest/owner/tier/时间全部结构合法 | 结构/时间/digest 问题 => `EVIDENCE_REJECTED` |
| `EVIDENCE_SUBMITTED` | `EVIDENCE_REJECTED` | synthetic runtime、caller result、tier downgrade、owner mismatch、artifact 缺失/非法、observation 与 state machine 矛盾 | 记录拒绝码；不得回退到 NOT_RUN 以掩盖 |
| `EVIDENCE_SUBMITTED` | `EVIDENCE_VALID` | 所有 required tiers 满足，artifacts/digests 完整，observations 不与 contract 矛盾（admissible，不代表 accepted） | 缺 required tier => `BLOCKED` |
| `EVIDENCE_SUBMITTED` | `BLOCKED` | required tier/环境/依赖缺失（evidence 本身合法但不足） | 不得算 PASS；补证后只能按同一 manifest identity 重新准入 |
| `EVIDENCE_VALID` | `PASS` | 存在独立 acceptance record，引用 manifestDigest + authority；authority approved；record 时间合法 | 无 record/authority => 保持 EVIDENCE_VALID；caller 字段 => 拒绝 |
| `EVIDENCE_VALID` | `FAIL` | acceptance record 明确 fail；失败证据完整 | 不得由 manifest observation 自声明 |
| `PASS`/`FAIL` | — | 终态，不可改写 | 重复/不同内容 => `A5_ACCEPTANCE_IMMUTABILITY_VIOLATION` |
| 任何 | `NOT_APPLICABLE` | 只允许 coverage row N/A | scenario 自声明 => `A5_APPLICABILITY_ILLEGAL` |

- 静态 A5 阶段：所有 scenario `execution=NOT_RUN`；`evidence_manifests=0`、`acceptance_records=0`；`runtimeEvidence=NOT_RUN`。
- `EVIDENCE_VALID` 只表示“可准入”，不表示业务真实发生，也不表示 accepted；真实验收需要 authority record。
- `BLOCKED` 表示缺少 required tier/环境/依赖，不是失败；不得被算作 PASS，也不得被 count-only 忽略。无 authority 时保持 `EVIDENCE_VALID` 且 `productionAccepted=false`，不是 BLOCKED。

### 7.3 Aggregate acceptance derivation

```text
specificationCoverageComplete =
  COVERAGE_COMPLETE && SOURCE_INTEGRITY && SCENARIO_SEMANTICS &&
  EVIDENCE_HONESTY && DOC_PARITY && REGISTRY_BASELINE

runtimeEvidenceComplete =
  every REPRESENTED scenario has PASS (or N/A) at required tiers
  && no FAIL/BLOCKED/EVIDENCE_REJECTED/NOT_RUN
  && every required cell is represented/N/A (0 deferred)

productionAccepted =
  runtimeEvidenceComplete
  && authorityRegistry has an APPROVED release authority for every represented scenario and path unit
  && no forbidden caller control fields
```

- `productionAccepted` 是 validator **输出**，不是 model 输入字段；model/evidence 中出现同名字段 => `A5_CALLER_CONTROL_FORBIDDEN`。
- `productionReady`/`procurementReady`/`cloudVerified`/`a3RiskClosed` 不是 A5 的输出；A5 不声明、不提升、不覆盖 A4 的固定 false 常量。
- `specificationCoverageComplete=true` 可以在静态阶段为 true；它只表示“矩阵规格覆盖完整”，不参与 `productionAccepted` 的完成度替代。
- `acceptanceRuleRef` 只接受固定 rule registry：`A5_ACCEPT_RUNTIME_TIER_AND_AUTHORITY`（运行时 tier + authority）与 `A5_ACCEPT_NOT_APPLICABLE`（仅 N/A 行，无 scenario）；不得把 rule ID 当 source ref 解析。
- **OQ-03 controller decision（2026-10-26，A5 plan review 1 accepted）**：静态 S2 `authorityRegistry` 故意为空；因此没有 `APPROVED` authority row 时 `PASS`/`productionAccepted` **不可达**——S1/S2 无证据时保持 `NOT_RUN`，S2 即使提交合法证据也最多到 `EVIDENCE_VALID`/`BLOCKED`。这是 typed residual `A5-RES-02`，不是 blocker，也不是伪造 authority row 或把 SPEC_VERIFIED 升级为 PASS 的许可。未来授权证据源出现前，validator 必须维持该不可达性。

### 7.4 禁止的“假通过”来源

- **Caller assertion**：manifest 出现 root `result`/`pass`/`accepted`/`productionAccepted`，或 model/acceptance record 由 caller 填 PASS => `A5_CALLER_CONTROL_FORBIDDEN` / `A5_CALLER_ASSERTED_PASS`。
- **Synthetic evidence**：`evidenceClass=SYNTHETIC` 或 fixture `SYNTHETIC_TEST_ONLY` 用于 runtime tier => `A5_SYNTHETIC_EVIDENCE_REJECTED`；只能留在 `SPEC_VERIFIED`。
- **Existing design completion**：A0-A4 validator green、A2/A3/A4 fixture pass、docs complete、prototype QA pass 都不是 runtime evidence；不得映射为 `PASS`。
- **Count-only checks**：96 coverage rows、90 scenarios、25+23+12 fixtures、49 operations 等计数不能单独产生 `specificationCoverageComplete` 或 `productionAccepted`；必须同时通过 source/semantic/evidence/doc checks。
- **Tier downgrade**：`STATIC_SPEC`/`MODEL_FIXTURE` 不满足 `INTEGRATION/LINE/CLOUD/MANUAL`；缺一个 required tier => `A5_EVIDENCE_TIER_INSUFFICIENT`。
- **Authority forgery**：registry 为空/无 `APPROVED`/approval 时间非法/authority owner 不匹配 => `A5_ACCEPTANCE_AUTHORITY_MISSING`/`INVALID`。静态 S1/S2 默认 registry 必须为空；任何 authority row 必须带可解析的外部 `authorizationBasisRef`，否则不得让 record 进入 PASS。
- **Manifest replay/immutability**：同一 manifestId 不同 digest、acceptance record 引用不同 digest、PASS 后再 FAIL/PASS => `A5_ACCEPTANCE_IMMUTABILITY_VIOLATION`。

## 8. Validator / model 设计与验证算法

### 8.1 CLI 与 stage

```bash
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

node acceptance/validate.mjs --stage s2 ... [same args] \
  [--evidence path/to/evidence-ledger.json]
```

- `--stage s1`：只评估 S1 sections、matrix integrity、source refs、scenario semantics、S1 fixtures；不要求 evidence ledger；所有 runtime 输出 `NOT_RUN`。
- `--stage s2`：额外要求 `evidencePolicy`、empty `authorityRegistry`、evidence admission checks，评估 S2 fixture；`--evidence` 可选；无 evidence 时 `evidence_manifests=0`。
- 参数未知/缺失 => 非 0 exit；stdout `FAIL acceptance-validation (invalid-arguments)`，stderr 精确错误；不吞异常。
- 不读取任何网络、环境变量、wall clock、random；`asOf` 来自 model/evidence；`fs` 只读本地已接受路径；不写文件（fixture mutation 在内存克隆上执行）。
- 数据流固定为：A0 doc / A1 bundle / A2 model+fixtures / A3 model+fixtures / A4 calc+fixtures → source resolvers（ID set/count/digest）→ A5 model/coverage/scenarios → coverage/source/semantic checks → doc marker parity → evidence admission（S2）→ aggregate output。任何解析失败都在进入下一阶段前 fail closed，不使用未解析的 partial registry。

### 8.2 Parse/验证顺序（invalid vs missing）

1. UTF-8/JSON parse；非 JSON/非 UTF-8 => `A5_MODEL_INVALID`。
2. Root object/`modelVersion`/`matrixId` 类型；缺 => `A5_MODEL_MISSING`，类型错 => `A5_MODEL_INVALID`。
3. Section 存在性：缺 required section => `A5_SECTION_MISSING`；未知 section => `A5_SECTION_EXTRA`。
4. Duplicate IDs：scenarioId/unitId/axis/residualId/ownerId/sourceBaseline key；重复 => `A5_DUPLICATE_ID`（不 last-wins）。
5. Required fields：缺失 => `A5_SCENARIO_FIELD_MISSING`（不再同时报 value invalid）；present-but-null => `A5_SCENARIO_FIELD_INVALID`。
6. Enum/type/value：非法 => `A5_SCENARIO_FIELD_INVALID`；数值边界 => `A5_EFFECT_VALUE_INVALID`。
7. Source refs：先识别 `docs/04`、`prototype`、`operations-prototype`、`web-prototype`、screenshot、`shared/onecall-business.js` 等 legacy/prototype ref => `A5_LEGACY_REFERENCE_FORBIDDEN`；其余语法 => `A5_SOURCE_REF_INVALID`；0 行 => `A5_SOURCE_REF_UNRESOLVED`；多行 => `A5_SOURCE_REF_AMBIGUOUS`；重复 ref => `A5_SOURCE_REF_DUPLICATE`。A0 state/edge ref 还必须通过 §4.9.1 frozen parse（`A5_A0_STATE_SHAPE_UNRECOGNIZED`/`A5_TRANSITION_REF_INVALID`）；A3 residual 必须来自 `docs/17` §20（18 行）。
8. Owner registry：`owners[]` 形状/唯一性/upstream exact mapping；scenario owner/evidenceOwner 与 residual owner 解析（`A5_OWNER_UPSTREAM_UNRESOLVED`/`A5_EVIDENCE_OWNER_*`/`A5_RESIDUAL_OWNER_*`）。
9. Coverage：missing/duplicate/illegal/N/A unbacked/deferred gap（§5）；path↔family 唯一性（`A5_PATH_FAMILY_DUPLICATE`）。
10. Scenario semantics：state edge/transitionRef、terminal absorption、axis↔ordering、idempotency、effect arithmetic、denial/duplicate/ordering、单一 tier 字段、external projection reconciliation（`A5_EXTERNAL_PROJECTION_CONTRADICTION`）。
11. Evidence admission（S2）：schema、tier、owner、artifact、digest、time、authority、immutability、count-only。
12. Doc marker parity：每个 block 的 start/end 恰一次、行/列/顺序与 model 一致。
13. Source baseline/registry drift：§2.3 计数/摘要（含 `a3_residuals=18`、A0 31 state rows、A0 Q=28、A4 §10.3=16、A4 closeout=10）。
14. Aggregate derivation：§7.3。
- 所有问题都收集（最多固定上限，例如 500），按 `(code, path, id)` 排序输出；任一 issue => exit 1。每类只有一个 primary code，避免级联淹没；fixture 只比较 primary code 集合。

### 8.3 核心算法

1. **State edge**：从 A0/A2 构建 `machine -> {states, derivedStates, edges, classification}`；A0 严格用 §4.9.1 frozen parser，A2 读 structured `transitions`。对每个 `StateAssertion`：
   - `targetRef` 必须解析；`value` 必须在 `states`，derived state 不得作为 value。
   - step `EQUALS`：若 preState 是 concrete 且 `value == preState` => `A5_SCENARIO_FIELD_INVALID`（必须改用 `UNCHANGED`）。若 preState 是 concrete 且 `value != preState`，且 machine 非 empty-edge、`targetRef` 非 `A0:TABLE`、不是 creation（preState=`ABSENT`），则 `transitionRef` 必须存在；其 `from` 必须等于 preState，`to` 必须等于 `value`，且 `(from,to)` 在 edges。错误 form/未知 edge/derived endpoint => `A5_TRANSITION_REF_INVALID`；from/to 不符 => `A5_STATE_SEQUENCE_MISMATCH`。省略规则见 §4.6。
   - `UNCHANGED`：`value` == 该 step 的 preState，且不得带 `transitionRef`；final `expected.persisted` 不得用 `UNCHANGED`，必须用 `EQUALS` 表达最终值。
   - 逐 step 用 `steps[].expectedState` 推进 preState；最终 `expected.persisted` 必须与最后一步状态一致，否则 `A5_STATE_SEQUENCE_MISMATCH`。终态无出边时任何变化 => `A5_TERMINAL_MUTATION`。
   - preState 由同一 targetRef 的 precondition/前序 step 推导；无前序 row 时为 `ABSENT`（creation）。
2. **Axis/ordering**：
   - `NORMAL -> NORMAL`；`DUPLICATE -> IDENTICAL_RETRY|CONFLICTING_RETRY`；`TIMEOUT -> DEADLINE_CROSSING|DEPENDENCY_FAILURE`；`OUT_OF_ORDER -> OUT_OF_ORDER|POST_TERMINAL`；`RECOVERY -> RESTART|FAILOVER`；`PERMISSION_DENIED -> PERMISSION_DENIED`；其他 => `A5_AXIS_STIMULUS_MISMATCH`。
3. **Duplicate zero effect**：`axis=DUPLICATE` 必须有 `duplicateReplay`；其 `ofStepId` 存在；该 step 与后续 retry step 不得新增成功业务事实（§4.6 的 replay/idempotency allowlist 例外必须有 `basisRef`）；`businessEffect=ZERO`；否则 `A5_DUPLICATE_SIDE_EFFECT`。
4. **Permission denial**：除 §4.6 的 fixed `denialAllowlist` 外，所有 step 的 domain delta 0；overall domain == baseline；allowlist 写入必须有 source ref 且不得是 success fact；任何 allowlist 外业务/成功事实 => `A5_DENIED_WITH_BUSINESS_EFFECT`。
5. **Out-of-order**：检查最终 persisted state 沿 edge 可达；terminal states 不 regress；duplicate/late fact 只能 `IGNORED/QUARANTINED`；二次 settlement/release/usage fact >1 => `A5_ORDERING_REGRESSION`。
6. **Effect arithmetic**：对每个 ref 计算 `baseline + Σ delta`，与 `overall` 比较；EXACT 必须相等；BOUNDED 的 `min <= sum <= max`；未知 ref/负值/缺 basis => `A5_EFFECT_*`；`overall` 中缺失但 delta 非零 => `A5_EFFECT_LEDGER_MISMATCH`。
7. **Terminal absorption**：terminalStates 必须来自 A0/A2 allowed states；postTerminalOutcome 必须与 axis/statement 一致；终态后任何 domain write >0（除 contract-defined reconciliation event）=> `A5_TERMINAL_MUTATION`。
8. **Cleanup**：禁止 delete/truncate audit/evidence/terminal/append-only facts；`NEW_IDENTITY_REQUIRES_REF` 必须有允许新身份的 source ref；`MANUAL_RESTORE` 必须有 A4/A2 recovery ref；否则 `A5_CLEANUP_ILLEGAL`。
9. **Evidence admission**：
   - scenario `execution` 字段若来自 model/evidence 而不是 validator 派生 => caller control；
   - manifest `evidenceClass` 与 required tiers 匹配；SYNTHETIC 不得用于 runtime；
   - artifact role 覆盖 expected audit/outbox/metric/log/line/cloud/manual 证据；缺 role => `A5_EVIDENCE_ARTIFACT_MISSING`；
   - `locatorRef` 黑名单/`sha256`/digest/time；
   - observations 不得包含 scenario expected 之外的 business state mutation；矛盾 => `A5_EVIDENCE_STATE_CONTRADICTION`；
   - `acceptanceRecord` 必须独立于 manifest，引用 authority + digest；无 authority => 保持 EVIDENCE_VALID 且 `productionAccepted=false`，不是 PASS；缺 required tier => BLOCKED。
10. **Count-only guard**：`specificationCoverageComplete` 只能由 §7.3 的 AND 计算；任何 shortcut 读取 fixture count/coverage count 直接置 true => `A5_COUNT_ONLY_COVERAGE`（代码级 fail-safe，不允许）。
11. **Doc parity**：每个 marker block 的 header/row/column 与 model 投影逐项比较；缺/多/重复/顺序错 => `A5_DOC_MODEL_DRIFT` / `A5_DOC_MARKER_*`。
12. **Determinism**：同一输入重复运行两次，canonical output 相同；否则 `A5_DETERMINISM_FAILURE`。
13. **Path/family uniqueness**：按 §4.2 的 pair map，对每个 path scenario 找到同 axis 且状态为 `REPRESENTED` 的 paired family scenario。要求 `set(sourceRefs(path)) − set(sourceRefs(family))` 非空且 `set(outcomeRefs(path)) − set(outcomeRefs(family))` 非空；否则 `A5_PATH_FAMILY_DUPLICATE`。只做 exact ref set difference，不做文本相似度匹配。`PATH-DEPLOY-*` 另需人工/评审确认 expected 是 A4→A5 boundary observation（§11 S1 完成条件）。
14. **External projection reconciliation**：按 §4.6 的 `(operationRef, field)` 规则，最后匹配 step 必须与 root external 一致；否则 `A5_EXTERNAL_PROJECTION_CONTRADICTION`。
15. **Single tier source**：`requiredEvidenceTiers` 只在 scenario root；`resultSemantics` 字段集合封闭，出现第二副本 => `A5_SCENARIO_FIELD_INVALID`。tier 比对只读 root。

### 8.4 Error / reason ID registry（validator 输出，不是 A1 Problem code）

```text
A5_MODEL_MISSING, A5_MODEL_INVALID, A5_SECTION_MISSING, A5_SECTION_EXTRA,
A5_DUPLICATE_ID, A5_UNKNOWN_AXIS, A5_UNKNOWN_UNIT, A5_ORPHAN_SCENARIO,
A5_COVERAGE_CELL_MISSING, A5_COVERAGE_CELL_DUPLICATE, A5_COVERAGE_CELL_ILLEGAL,
A5_APPLICABILITY_ILLEGAL, A5_NA_REASON_UNKNOWN, A5_NA_REASON_UNBACKED,
A5_SCENARIO_FIELD_MISSING, A5_SCENARIO_FIELD_INVALID,
A5_SOURCE_REF_INVALID, A5_SOURCE_REF_UNRESOLVED, A5_SOURCE_REF_AMBIGUOUS,
A5_SOURCE_REF_DUPLICATE, A5_SOURCE_DRIFT, A5_SOURCE_BASELINE_MISMATCH,
A5_A0_STATE_SHAPE_UNRECOGNIZED, A5_TRANSITION_REF_INVALID,
A5_OWNER_UPSTREAM_UNRESOLVED, A5_RESIDUAL_OWNER_UNKNOWN,
A5_PATH_FAMILY_DUPLICATE, A5_EXTERNAL_PROJECTION_CONTRADICTION,
A5_EVIDENCE_OWNER_MISSING, A5_EVIDENCE_OWNER_UNKNOWN,
A5_EVIDENCE_OWNER_MISMATCH, A5_EVIDENCE_DELEGATION_UNRESOLVED,
A5_EFFECT_VALUE_INVALID, A5_EFFECT_BOUND_UNBACKED, A5_EFFECT_LEDGER_MISMATCH,
A5_AXIS_STIMULUS_MISMATCH, A5_STATE_SEQUENCE_MISMATCH, A5_TERMINAL_MUTATION,
A5_DUPLICATE_SIDE_EFFECT, A5_ORDERING_REGRESSION, A5_DENIED_WITH_BUSINESS_EFFECT,
A5_CLEANUP_ILLEGAL,
A5_EVIDENCE_MANIFEST_INVALID, A5_EVIDENCE_TIER_INSUFFICIENT,
A5_EVIDENCE_ARTIFACT_MISSING, A5_EVIDENCE_REF_INVALID,
A5_EVIDENCE_DIGEST_MISMATCH, A5_EVIDENCE_TIME_INVALID,
A5_EVIDENCE_STATE_CONTRADICTION, A5_SYNTHETIC_EVIDENCE_REJECTED,
A5_CALLER_CONTROL_FORBIDDEN, A5_CALLER_ASSERTED_PASS,
A5_ACCEPTANCE_AUTHORITY_MISSING, A5_ACCEPTANCE_AUTHORITY_INVALID,
A5_ACCEPTANCE_EVIDENCE_MISSING, A5_ACCEPTANCE_IMMUTABILITY_VIOLATION,
A5_RESIDUAL_OWNER_MISSING, A5_RESIDUAL_DESTINATION_MISSING,
A5_DOC_MODEL_DRIFT, A5_DOC_MARKER_MISSING, A5_DOC_MARKER_DUPLICATE,
A5_PROHIBITED_READINESS_FLAG, A5_COUNT_ONLY_COVERAGE,
A5_DETERMINISM_FAILURE, A5_LEGACY_REFERENCE_FORBIDDEN
```

- 这些是 A5 validator 的分类码，不是生产 Problem code；不得出现在 OpenAPI/Problem registry。
- `A5_LEGACY_REFERENCE_FORBIDDEN` 用于把 `docs/04`/prototype/screenshot 当作 runtime evidence 的 scenario。
- `A5_A0_STATE_SHAPE_UNRECOGNIZED`：A0 row 形状无法按 §4.9.1 解析或出现一侧 state、一侧非 state 的歧义；`A5_TRANSITION_REF_INVALID`：transitionRef 使用 `A0:TX:*`、derived state、未知 edge 或错误 form。
- `A5_OWNER_UPSTREAM_UNRESOLVED`：registry entry 的 upstream mapping 为 0/multiple 行或 exact text 不匹配；`A5_RESIDUAL_OWNER_UNKNOWN`：residual owner 不在 registry。
- `A5_PATH_FAMILY_DUPLICATE`：path row 的 source/outcome ref 与 paired family 完全相同；`A5_EXTERNAL_PROJECTION_CONTRADICTION`：最后 step 局部响应与 root external projection 冲突。
- 所有 code 稳定；新增 code 必须同时更新 docs/19 reason block 与 fixture。

### 8.5 输出格式

成功：

```text
stage=s1 matrix=A5-PRODUCTION-ACCEPTANCE-MATRIX-V1 axes=6 units=16 cells=96 represented=90 not_applicable=6 deferred=0 scenarios=90 source_refs=<n> unresolved=0 ambiguous=0 drift=0 a3_residuals=18 a0_state_rows=31 a0_shape_unrecognized=0 owner_upstream_unresolved=0 residual_owner_unknown=0 transition_ref_invalid=0 path_family_duplicate=0 external_projection_contradiction=0 evidence_owner_missing=0 coverage_missing=0 coverage_duplicate=0 coverage_illegal=0 na_unbacked=0 terminal_mutation=0 duplicate_side_effect=0 ordering_regression=0 denied_with_business_effect=0 caller_control=0 synthetic_acceptance=0 runtime_evidence=NOT_RUN production_accepted=false spec_coverage=COMPLETE fixtures=3 passed=3 failed=0 negative=3 deterministic=true
PASS acceptance-validation (s1)
```

S2 追加：

```text
stage=s2 ... evidence_manifests=0 evidence_admitted=0 acceptance_records=0 blocked=0 runtime_evidence=NOT_RUN production_accepted=false spec_coverage=COMPLETE fixtures=4 passed=4 failed=0 negative=4 deterministic=true
PASS acceptance-validation (s2)
```

失败：

```text
FAIL acceptance-validation (s1)
ISSUE <CODE> <path> <id>
...
```

- stdout 只输出稳定 summary；stderr 输出 issue。不得用“fixture 数=通过数”代替语义断言。
- fixture 成功要求每个 case 的 primary code 集合精确匹配、exit code 精确匹配；多报/少报都失败。

## 9. Fixtures 与对抗案例

不按 cell 建 fixture；按一般 invariant 分组。每个 fixture 是 `kind: MODEL_MUTATION`、`kind: EVIDENCE_MANIFEST` 或 `kind: A0_STATE_SHAPE`，基于真实 matrix/A0 parse 结果的内存克隆，不写文件。

### 9.1 `s1-01-matrix-integrity.json`

| Case | 变异 | 期望 primary code |
| --- | --- | --- |
| `missing-coverage-cell` | remove `FAM-IVR/DUPLICATE` | `A5_COVERAGE_CELL_MISSING` |
| `duplicate-scenario-id` | duplicate `A5-IVR-NORMAL-01` | `A5_DUPLICATE_ID` |
| `bad-source-ref` | set `A0:INV:INV-99` | `A5_SOURCE_REF_UNRESOLVED` |
| `duplicate-source-ref` | repeat one ref inside a scenario | `A5_SOURCE_REF_DUPLICATE` |
| `illegal-applicability` | set `FAM-IVR/DUPLICATE` to N/A using a registered-but-not-allowed reason | `A5_APPLICABILITY_ILLEGAL` |
| `unknown-na-reason` | N/A with `NA-FREE-TEXT` | `A5_NA_REASON_UNKNOWN` |
| `unbacked-na` | valid reason, empty refs | `A5_NA_REASON_UNBACKED` |
| `missing-evidence-owner` | unset scenario.evidenceOwner | `A5_EVIDENCE_OWNER_MISSING` |
| `unknown-evidence-owner` | evidenceOwner=`unknown-team` | `A5_EVIDENCE_OWNER_UNKNOWN` |
| `orphan-scenario` | scenario unit/axis not in coverage | `A5_ORPHAN_SCENARIO` |
| `residual-no-owner` | append residual owner=null | `A5_RESIDUAL_OWNER_MISSING` |
| `residual-no-destination` | append residual destination=null | `A5_RESIDUAL_DESTINATION_MISSING` |
| `doc-model-drift` | change scenario title only | `A5_DOC_MODEL_DRIFT` |
| `prohibited-readiness-flag` | set root `productionReady=true` | `A5_PROHIBITED_READINESS_FLAG` |
| `caller-production-accepted` | set root `productionAccepted=true` | `A5_CALLER_CONTROL_FORBIDDEN` |
| `all-counts-but-unbacked-na` | 96 rows/90 scenarios but one N/A unbacked | `A5_NA_REASON_UNBACKED`（证明 count-only 失败） |
| `legacy-prototype-evidence` | sourceRef=`docs/04`/prototype | `A5_LEGACY_REFERENCE_FORBIDDEN` |
| `source-residual-ok` | scenario basisRef 使用 `A3:RESIDUAL:R-A3-AUDIT-VOLUME`（`docs/17` §20 真源） | exit 0（正向 control；18 行内解析） |
| `source-residual-unresolved` | `A3:RESIDUAL:R-A3-NOPE` | `A5_SOURCE_REF_UNRESOLVED` |
| `owner-registry-ok` | 全 matrix 的 scenario/evidenceOwner 与 residual owner 均解析 registry | exit 0（正向 control） |
| `owner-upstream-ref-missing` | registry entry `upstream=[]` 或 exact text 不匹配 | `A5_OWNER_UPSTREAM_UNRESOLVED` |
| `residual-owner-unknown` | append residual owner=`unknown-team` | `A5_RESIDUAL_OWNER_UNKNOWN` |
| `path-family-duplicate` | PATH row 完全复制 paired FAM 的 sourceRefs/outcomeRefs | `A5_PATH_FAMILY_DUPLICATE` |

### 9.2 `s1-02-expectation-safety.json`

| Case | 变异 | 期望 primary code |
| --- | --- | --- |
| `terminal-mutation` | ENDED → ACTIVE | `A5_TERMINAL_MUTATION` |
| `post-terminal-business-write` | post-terminal step domain delta >0 | `A5_TERMINAL_MUTATION` |
| `duplicate-second-settlement` | DUPLICATE overall usage_fact=2 | `A5_DUPLICATE_SIDE_EFFECT` |
| `duplicate-nonzero-domain-delta` | retry step delta>0 | `A5_DUPLICATE_SIDE_EFFECT` |
| `out-of-order-regression` | OOO final state earlier than pre-state | `A5_ORDERING_REGRESSION` |
| `denied-with-side-effect` | PERMISSION_DENIED domain delta>0 | `A5_DENIED_WITH_BUSINESS_EFFECT` |
| `unbacked-bound` | BOUNDED effect without basisRef | `A5_EFFECT_BOUND_UNBACKED` |
| `effect-ledger-mismatch` | overall ≠ baseline+Σdelta | `A5_EFFECT_LEDGER_MISMATCH` |
| `axis-ordering-mismatch` | DUPLICATE axis with NORMAL ordering | `A5_AXIS_STIMULUS_MISMATCH` |
| `illegal-cleanup-delete` | cleanup deletes audit/evidence | `A5_CLEANUP_ILLEGAL` |
| `missing-idempotency` | DUPLICATE without duplicateReplay | `A5_SCENARIO_FIELD_MISSING` |
| `transition-ref-a0-edge` | step transitionRef=`A0:SM:Share grant:ACTIVE->REVOKED` | exit 0（正向 control） |
| `transition-ref-a0-tx-illegal` | transitionRef=`A0:TX:T-S3-03` | `A5_TRANSITION_REF_INVALID` |
| `transition-ref-unknown-edge` | transitionRef=`A0:SM:Share grant:EXPIRED->ACTIVE` | `A5_TRANSITION_REF_INVALID` |
| `transition-ref-derived-state` | transitionRef=`A0:SM:Trial:GRANTED->EXPIRED` | `A5_TRANSITION_REF_INVALID` |
| `transition-ref-omitted-nonempty` | remove transitionRef from state-changing step on non-empty-edge machine | `A5_SCENARIO_FIELD_MISSING` |
| `duplicate-tier-field` | `resultSemantics.requiredEvidenceTiers` exists (different from root) | `A5_SCENARIO_FIELD_INVALID` |
| `external-projection-contradiction` | root `processingStatus=APPLIED`; last matching step response `IGNORED` | `A5_EXTERNAL_PROJECTION_CONTRADICTION` |

### 9.3 `s1-03-a0-state-shapes.json`

`kind: A0_STATE_SHAPE`；基于 `docs/14` §7.1/§7.3/§7.4 的内存解析结果，不修改 A0。每个正例冻结代表行的 `states`/`edges`/`derivedStates`/`classification`/attached note；负例对 parser 输入做内存 mutation。

| Case | 形状/变异 | 期望 |
| --- | --- | --- |
| `a0-call-record` | 多 target arrows + 第 5 列 attached note | `ACTIVE→ENDED/FAILED/UNKNOWN`、`UNKNOWN→ENDED/FAILED`；5 states；STANDARD |
| `a0-organization` | combined source `ACTIVE`/`SUSPENDED → DISABLED` | 4 edges：ACTIVE↔SUSPENDED、ACTIVE→DISABLED、SUSPENDED→DISABLED |
| `a0-org-membership` | `↔` + combined source | 4 edges；REVOKED terminal |
| `a0-catalog-version` | guard prose + 同一 cell 两个 arrow | `DRAFT→ACTIVE`、`ACTIVE→OFFSALE` |
| `a0-customer-order` | multi-target `UNPAID → UNKNOWN/PAID/CLOSED` | 6 edges；不把 prose token 当 state |
| `a0-payment-order` | “Same verified-evidence transitions as the order”（无显式 arrow） | states 4；edges 复制 Customer order 的 6 条；`SHARED_EDGE_SET` |
| `a0-subject-ownership` | composite tuple states | 3 个 exact tuple literals；edges=[]；`PROSE_PARENTHESIZED` |
| `a0-trial` | derived annotation + `No row transition` | states=[`GRANTED`]；derived=[`EXHAUSTED`,`EXPIRED`]；edges=[]；`DERIVED_IMMUTABLE` |
| `a0-subject-claim` | insert-only | 3 states；edges=[]；`INSERT_ONLY` |
| `a0-ivr-decision` | frozen result（OQ-04） | states=[]；edges=[]；`FROZEN_RESULT`（不 fail） |
| `a0-direct-authorization` | Allowed-states cell 自身含 arrow | states=[`NONE`,`AUTHORIZED`]；edges=[`NONE→AUTHORIZED`]；`STATE_CELL_ARROW` |
| `a0-unknown-arrowless` | 将一个标准 row 的 states + transitions cell 都改为 closed list 之外的 arrowless prose | `A5_A0_STATE_SHAPE_UNRECOGNIZED` |
| `a0-half-state-arrow` | 篡改 edge 为 `ACTIVE → PROSE`（一侧非 state） | `A5_A0_STATE_SHAPE_UNRECOGNIZED` |

### 9.4 `s2-01-evidence-admission.json`

| Case | 输入/变异 | 期望 primary code |
| --- | --- | --- |
| `caller-forged-pass` | manifest root `result="PASS"` | `A5_CALLER_CONTROL_FORBIDDEN` |
| `caller-accepted` | manifest `accepted=true` | `A5_CALLER_ASSERTED_PASS` |
| `synthetic-runtime` | `evidenceClass=SYNTHETIC`, `evidenceTiers=[INTEGRATION]` | `A5_SYNTHETIC_EVIDENCE_REJECTED` |
| `model-fixture-as-runtime` | `evidenceClass=MODEL_FIXTURE` for required INTEGRATION | `A5_EVIDENCE_TIER_INSUFFICIENT` |
| `missing-line-tier` | required `[INTEGRATION,LINE]`, provided `[INTEGRATION]` | `A5_EVIDENCE_TIER_INSUFFICIENT` |
| `bad-artifact-digest` | sha256 non-hex | `A5_EVIDENCE_REF_INVALID` |
| `digest-mismatch` | manifestDigest altered | `A5_EVIDENCE_DIGEST_MISMATCH` |
| `future-timestamp` | executedAt > asOf | `A5_EVIDENCE_TIME_INVALID` |
| `owner-mismatch` | evidenceOwner ≠ scenario owner without delegation | `A5_EVIDENCE_OWNER_MISMATCH` |
| `no-authority` | acceptanceRecord authorityRef missing | `A5_ACCEPTANCE_AUTHORITY_MISSING` |
| `synthetic-authority` | authority status `SYNTHETIC_TEST_ONLY` | `A5_ACCEPTANCE_AUTHORITY_INVALID` |
| `authority-row-without-basis` | 非空 authority registry row 无 approved `authorizationBasisRef` | `A5_ACCEPTANCE_AUTHORITY_INVALID`（OQ-03：静态 registry 必须为空） |
| `accept-without-evidence` | acceptanceRecord with no manifest | `A5_ACCEPTANCE_EVIDENCE_MISSING` |
| `state-contradiction` | observed ENDED→ACTIVE | `A5_EVIDENCE_STATE_CONTRADICTION` |
| `immutability` | same manifestId, different digest | `A5_ACCEPTANCE_IMMUTABILITY_VIOLATION` |
| `count-only-acceptance` | 96 cells, no evidence, root accepted | `A5_ACCEPTANCE_EVIDENCE_MISSING` + `A5_CALLER_CONTROL_FORBIDDEN` |
| `positive-model-evidence` | fixture-only override one scenario's `requiredEvidenceTiers` to `[MODEL_FIXTURE]` + matching manifest | exit 0; `evidence_admitted=1`, `productionAccepted=false`（control 不改变真实矩阵） |

- 正向 control 只证明 manifest 可准入、root 仍 false；不证明真实执行。
- 每个 case 期望 primary code 集合精确；不得只断言“失败”。

## 10. 验证命令与预期语义断言

### 10.1 前置 A0-A4 回归

```bash
ROOT="$(git rev-parse --show-toplevel)"; cd "$ROOT"
# A1 bundle 的唯一生成命令（需要 registry/network 访问；见下方诚实限制）
( cd api/openapi && npm ci && npm run check:s3 )
test -f tmp/openapi-client/onecall-v1.bundle.json
node protocol/ivr/validate.mjs --stage s3 \
  --model protocol/ivr/v1/ivr-protocol-v1.json \
  --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md
node security/validate.mjs --stage s3 \
  --model security/v1/security-design-v1.json \
  --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures
node deployment/capacity/validate.mjs --stage s2 \
  --template deployment/capacity/inputs.template.json \
  --fixtures deployment/capacity/fixtures
```

**Clean-checkout 顺序与 A1 bundle 前置条件（F-A5-PR-06）**：

- 所有命令使用 `git rev-parse --show-toplevel`，不依赖 WSL 绝对路径。在任何干净 clone 中从仓库根执行。
- `tmp/openapi-client/onecall-v1.bundle.json` 是**未跟踪的生成物**；唯一生产命令是 `( cd api/openapi && npm ci && npm run check:s3 )`（`check:s3` 内含 `bundle`）。
- 正确顺序：clean checkout → `npm ci`/`check:s3` 生成 bundle → §10.2/§10.3。**§10.2 在 bundle 存在前不能独立运行**，不得声称其已通过。
- 离线环境无法执行 `npm ci`，因此无法生成 bundle，§10.2 不能运行；这是 `RSK-03` 环境限制，不是可用静态/合成证据绕过的许可。validator 在 `--a1-bundle` 缺失/不可读时以 `FAIL acceptance-validation (invalid-arguments)` 退出，不打印成功。

预期语义：

- A1：`PASS check-doc-map (s3)`，`total=49`，`customer=18 operations=21 ivr=6 public-share=3 provider-callback=1`，`a0_*`/`doc_*` 零差异。
- A2：`PASS protocol-validation (s3)`，`messages=12 event_types=11 identities=22 fixtures=25 passed=25 failed=0 negative=3`，`event_registry_sha256=c6b769d4...f4e9`，`a1_operations=49 ivr_operations=6`，`a0_tables=44`。
- A3：`PASS security-validation (s3)`，`trust_surfaces=5 auth_domains=5 trust_boundaries=7 credential_classes=10 invariants=21 fixtures=23 passed=23 failed=0 negative=10`，A0/A1/A2 drift 为零。
- A4：`PASS capacity-validation stage=s2`，`fixtureCases=113 passed=113 failed=0`（以实际 accepted 输出为准；不得用旧计数替代），`productionReady=false procurementReady=false cloudVerified=false a3RiskClosed=false`。
- 任一上游回归失败 => A5 不得通过；A5 validator 也必须独立解析并报告 drift，而不是只信任上游打印。

### 10.2 A5 验证

```bash
ROOT="$(git rev-parse --show-toplevel)"; cd "$ROOT"
# 前置：§10.1 已生成 tmp/openapi-client/onecall-v1.bundle.json
test -f tmp/openapi-client/onecall-v1.bundle.json
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
  --a4-fixtures deployment/capacity/fixtures
```

必须断言：

- `coverage_cells=96`、`represented=90`、`not_applicable=6`、`deferred=0`、`scenarios=90`；
- `coverage_missing=0`、`coverage_duplicate=0`、`coverage_illegal=0`、`na_unbacked=0`、`orphan=0`、`path_family_duplicate=0`；
- `source_unresolved=0`、`source_ambiguous=0`、`source_drift=0`、`source_baseline_mismatch=0`、`a3_residuals=18`、`a0_state_rows=31`、`a0_shape_unrecognized=0`；
- `owner_upstream_unresolved=0`、`residual_owner_unknown=0`、`evidence_owner_missing=0`、`evidence_owner_unknown=0`、`evidence_owner_mismatch=0`；
- `transition_ref_invalid=0`、`external_projection_contradiction=0`；tier 只从 root 读取；
- `terminal_mutation=0`、`duplicate_side_effect=0`、`ordering_regression=0`、`denied_with_business_effect=0`、`effect_ledger_mismatch=0`、`cleanup_illegal=0`；
- `caller_control=0`、`caller_asserted_pass=0`、`synthetic_acceptance=0`、`count_only=0`；
- `evidence_manifests=0`、`evidence_admitted=0`、`acceptance_records=0`、`runtime_evidence=NOT_RUN`；
- `production_accepted=false`、`spec_coverage=COMPLETE`；
- 两次运行 summary 完全相同（deterministic）。

### 10.3 文档/链接/scope 检查

```bash
ROOT="$(git rev-parse --show-toplevel)"; cd "$ROOT"
git diff --check -- docs/19-production-acceptance-matrix.md acceptance/
node acceptance/validate.mjs --stage s2 ... [same] --check-doc-links
```

- `--check-doc-links`（validator 内置）检查 docs/19 内相对链接指向真实文件、`docs/README.md` 的 docs/19 行唯一且 link 可解析；不访问网络。
- `git diff --check` 无 whitespace error；line count/section inspection。
- Scope verification：只允许计划/实现定义的 paths；A0-A4、prototypes、screenshots、control doc 不变；不 stage/commit/push。

### 10.4 不依赖“成功字符串”的语义断言

- 每个 fixture case 必须精确匹配 primary code 集合；不只看 `failed=0`。
- 每个 source ref 必须有解析结果；不只看 `source_refs=<n>`。
- 每个 coverage cell 必须有状态/owner/refs；不只看 `cells=96`。
- 每个 scenario 必须过 state/effect/evidence 语义；不只看 `scenarios=90`。
- A3 residual 必须核对 `docs/17` §20 的 18 行与 owner/destination 非空，不只看某个 ref 能打印。
- A0 state/edge 必须核对 §4.9.1 的 31 行与代表行 expected map，不只看 `A0:SM` 能解析。
- A0-A4 regression 必须核对计数/摘要/negative 数，不只看 `PASS` 字符串。

## 11. 实施切片（2）

### S1 — 静态矩阵契约、完整六轴覆盖与诚实证据状态

**行为目标**：把 accepted A0-A4 contracts 映射成 96-cell / 90-scenario 的可验证矩阵，并证明所有 scenario expectation 不与上游状态机/副作用语义矛盾；runtime 仍全部 `NOT_RUN`。

**文件**：

- `docs/19-production-acceptance-matrix.md` §1-§8、§10-§12（§9 留给 S2；S2 追加并回写 §10/§11 命令）；
- `acceptance/v1/acceptance-matrix-v1.json`（units/axes/N/A/coverage/scenarios/effects/evidence owner/state machine/residual/source baseline/reason registry；S1 不含 `evidencePolicy`/`authorityRegistry`）；
- `acceptance/validate.mjs`（stage s1）；
- `acceptance/fixtures/s1-01-matrix-integrity.json`、`s1-02-expectation-safety.json`、`s1-03-a0-state-shapes.json`；
- `docs/gateflow/a5-production-acceptance-matrix-spec-s1-implementation.md`。

**行为断言**：

1. 96 cells、90 scenarios、6 N/A、0 deferred；所有 coverage/scenario/residual refs 解析；A3 residual 来自 `docs/17` §20（18 行）。
2. A0 §7.1/§7.3/§7.4 的 31 行按 §4.9.1 解析；代表行 expected map 与 `s1-03` 完全一致；未识别/歧义形状 fail closed。
3. `owners[]` closed registry 建立；所有 scenario owner/evidenceOwner 与 residual owner 解析；`s1-01` 的 owner positive/negative cases 精确匹配。
4. 所有 state assertion 通过 A0/A2 state/edge 检查；`transitionRef` 只使用 A0 edge/A2 state；终态无 mutation；duplicate/denied/out-of-order/cleanup/effect 规则全绿。
5. 所有 scenario 有 owner/evidenceOwner/单一 root tier/outcomeRefs/sourceRefs/preconditions/stimulus/expected/terminal/idempotency/effects/evidence/cleanup/result semantics；external projection 与最后 step 一致。
6. 每个 path row 与 paired family 的 source/outcome ref 差集非空；`PATH-DEPLOY-*` 是 A4→A5 boundary observation。
7. Doc marker blocks 与 model 一致；source baseline 无 drift。
8. S1 fixtures（3 文件）的 primary codes 精确匹配；determinism 双跑一致。
9. root `productionAccepted=false`、`runtimeEvidence=NOT_RUN`、`specCoverage=COMPLETE`。

**S1 完成条件**：`PASS acceptance-validation (s1)` + 上述断言 + `git diff --check` 通过 + 只修改 S1 允许路径。

### S2 — 执行证据准入、聚合结果与仓库回归接入

**行为目标**：在不执行真实系统的前提下，证明 evidence manifest 不能靠 caller/synthetic/tier/count-only 变成 PASS；接入 A0-A4 回归、docs 索引与 residual/closeout 报告。

**文件**：

- `acceptance/v1/acceptance-matrix-v1.json` additive：`evidencePolicy`、empty `authorityRegistry`、evidence manifest/acceptance record contract；
- `acceptance/validate.mjs` stage s2 + evidence admission checks + `--check-doc-links`;
- `acceptance/fixtures/s2-01-evidence-admission.json`；
- `docs/19-production-acceptance-matrix.md` §9、§10 residual/validation 更新；
- `docs/README.md` 仅新增 docs/19 一行；
- `docs/gateflow/a5-production-acceptance-matrix-spec-s2-implementation.md`。

**行为断言**：

1. S1 全绿保持不回归；S2 fixture 的 caller/synthetic/tier/authority/digest/time/immutability/state-contradiction cases 全部精确拒绝。
2. 无 manifest/authority 时 `productionAccepted=false`，即使 96 cells/90 scenarios 完整。
3. 一个 MODEL_FIXTURE positive control 只能得到 `EVIDENCE_VALID`/`SPEC_VERIFIED`，不能得到 PASS。
4. `authorityRegistry` 必须为空；无 `APPROVED` authority 时 PASS/production acceptance 不可达（OQ-03 controller-accepted），任何非空 row 无 approved `authorizationBasisRef` 都被拒绝。
5. A0-A4 regression 命令全部通过；A1/A2/A3/A4 计数/digest 与 accepted baseline 一致。
6. docs/README docs/19 行唯一；docs/19 相对链接可解析；无 public contract/migration/CI 变更。

**S2 完成条件**：`PASS acceptance-validation (s2)` + S1 回归 + A0-A4 回归 + docs/scope 检查 + completion report 声明无 runtime/cloud/line/manual 执行。

### 11.1 为什么是 2 个 slice

- S1 直接映射 goal 的 coverage/ID/source/state/effect/evidence-owner/result-semantics 元素：完整六轴覆盖、A0-A4 引用、终态/幂等/零或有界副作用、cleanup、evidence owner。
- S2 直接映射 goal 的 evidence honesty 元素：specification 与 runtime 分离、caller/synthetic/count-only 拒绝、A0-A4 回归和 docs 可发现性。
- S1 是一个可独立验证的 acceptance outcome：矩阵规格覆盖完整且 expectation 与 accepted contracts 不矛盾；失败模式（漏 cell、坏 ref、终态 mutation、重复副作用）都在 S1 可测。
- S2 是另一个 executable evidence boundary：只有 manifest/authority/acceptance record 的准入语义、聚合结果、跨仓库回归与可发现性；S1 不涉及任何 runtime evidence，S2 不重写 scenario 语义。
- 不按 source doc（A0/A1/A2/A3/A4）、subsystem（IVR/media/sharing）、axis（normal/duplicate/...）或 team 拆分；也不把 docs/closeout 单独拆成第三片。若强行拆三片，只会制造文件级/报告级 gate 成本，不产生独立行为结果。

## 12. 文档、迁移/公共契约、发布、风险、开放问题、停止条件与完成报告

### 12.1 文档决定

- 人类 canonical：`docs/19-production-acceptance-matrix.md`。
- 机器 canonical：`acceptance/v1/acceptance-matrix-v1.json`。
- `docs/README.md` 只新增一行：

  `| [19-production-acceptance-matrix.md](19-production-acceptance-matrix.md) | 生产验收矩阵、六轴覆盖与证据状态 | 架构、QA、安全、运维、产品 |`

- 不新增 `acceptance/README.md`、第二份协议/安全/容量文档、截图、prototype 入口或 Figma 资产。
- `docs/19` marker blocks 固定：`acceptance-family-map`、`acceptance-axis-map`、`acceptance-coverage-map`、`acceptance-scenario-index`、`acceptance-evidence-state-map`、`acceptance-reason-map`、`acceptance-residual-map`、`acceptance-gate-map`；validator 逐块比对。
- 文档写中文（repo 既有 gateflow/spec 惯例），字段/ID/枚举保持英文 canonical。

### 12.2 迁移/公共契约决定

- 无 DB migration、无 Flyway、无 backfill、无 schema/API/Problem/role/event 变更。
- 无 production runtime module；`acceptance/` 不进入部署物、不由客户端调用、不成为 OpenAPI 的一部分。
- 无第三 registry；owner/ID/enum 全部引用 A0-A4。

### 12.3 发布/非发布声明

- 不部署、不发布、不启用 feature flag、不执行 cloud/line/manual run。
- 不创建 CI pipeline、不修改 branch protection、不创建/修改 PR/issue、不 stage/commit/push。
- A5 产物在静态阶段只用于评审和后续授权执行前的矩阵定义；`productionAccepted=false`。

### 12.4 诚实限制

- A5 validator 证明的是：矩阵覆盖完整、来源可解析、scenario expectation 与 accepted state/effect 语义不矛盾、evidence admission 不允许 caller/synthetic/count-only 升级。
- A5 **不证明**：真实 DB 约束/锁/事务、Redis/RabbitMQ/COS/KMS/CAM、支付供应商、IVR 线路/B-leg/录音、跨节点 callId、真实恢复、SLO/RPO/RTO、容量/云能力、安全渗透、生产脱敏配置或任何 runtime 行为。
- A2/A3/A4 fixtures 仍是 model-level；不是 A5 的 runtime evidence。
- 矩阵中的 expected counts/states 是**验收断言**，不是实现；若实现与 accepted contract 冲突，应修实现或回退上游契约，不得改 A5 期望掩盖。

### 12.5 Residual risks（全部有 owner/destination）

| A5 residual | 内容 | Upstream refs | Owner / Destination |
| --- | --- | --- | --- |
| `A5-RES-01` | 不可变证据存储、retention、authorization、WORM/object-lock 未选 | A0 Q3/Q13、A3 `R-A3-COS-CAPABILITY`/`R-A3-LEGAL-RETENTION`、A4 §10.3 | `Infrastructure/Security`；evidence platform + launch governance |
| `A5-RES-02` | acceptance authority 未定义；静态 registry 故意为空（OQ-03 controller-accepted） | A5 binding contract；A3 `R-A3-SUPPORT-VIEW`；A4 §10.3 “SLO/RPO/RTO … acceptance policy” row | `Product/Security/Operations`；release governance；PASS 在未来授权 authority 前不可达 |
| `A5-RES-03` | 真实 integration/line/cloud/manual 执行环境缺席 | A2 §11、A3 §19、A4 §10.4 | `Telephony/IVR`、`Infrastructure/Security`、`Backend/QA`；authorized production execution gate |
| `A5-RES-04` | 精确 metric/log label、dashboard、采集配置未冻结 | `docs/12` §12、A3 `R-A3-REDACTION-RUNTIME`/`R-A3-AUDIT-VOLUME` | `Operations/Backend`；runbook/observability design |
| `A5-RES-05` | A0-A4 深层语义漂移只能靠各自 validator + 本矩阵结构/digest 检测，无法证明语义完全未变 | A0-A4 accepted closeouts；`DOC12:S18` | `A5-UPSTREAM-DELEGATED`；各 spec owner change control/independent review |
| `A5-RES-06` | `FAM-DEPLOYMENT` 的 timeout/out-of-order/permission-denied 为 N/A；若 A4 未来新增语义，需修订矩阵 | `docs/18` §8.4/§10.4 | `Telephony/IVR/Security`；A4/A5 revision |
| `A5-RES-07` | 无 CI enforcement，命令需人工执行 | `A4:CLOSEOUT:10` | `Platform/CI`；repository CI before production merge policy |
| `A5-RES-08` | evidence manifest 的完整性依赖外部 authorization/immutable storage；A5 只校验引用 | A3 `R-A3-MEDIA-GATEWAY-TRANSPORT`/`R-A3-AUDIT-VOLUME` | `Infrastructure/Security`；evidence platform |
| `A5-RES-09` | 90 scenarios 的人工 authoring/N/A judgment 存在遗漏风险；validator 只能证明结构与上游一致性 | A5 goal contract；A0-A4 | `Backend/QA`；independent review + future execution |
| `A5-RES-10` | `docs/04` legacy 场景与 A0-A4 冲突时不覆盖；AI/notary/refund/invoice/work-order/PBX 未纳入 | A0 §14、`docs/12` §17.2 | `Product/Legal`；future accepted contracts |
| `A5-RES-11` | 组织所有权与 Q1-Q28 中与验收相关的未决项未决；A5 不重新拥有，原行 owner/destination 不变 | A0 §14（`DOC14:S14` 28 行）、A3 §9、A4 §10.3 | `A5-UPSTREAM-DELEGATED`；每行原 owner/destination 不变 |
| `A5-RES-12` | A2/A3/A4 既有 synthetic fixtures 可能被误读为验收证据 | A2 §10、A3 §19、A4 §10.4 | `Backend/QA`；A5 evidence honesty + independent review |

### 12.6 开放问题（不阻塞本计划）

1. 哪个外部系统是权威不可变 evidence store（对象存储/证据平台/受控 vault）？Owner `Infrastructure/Security`；destination launch governance。
2. 谁有权签署 production acceptance record？Owner `Product/Security/Operations`；destination release governance。
3. 精确 metric/log 字段名与 dashboard 由 `Operations/Backend` 选定（A0 Q13 observability destination）；A5 只引用 A3/`docs/12` category。
4. `FAM-DEPLOYMENT` 三个 N/A 已被 plan review 1 的 Q2 接受；理由已合同化，若上游新增 deadline/order/authz，A5 必须修订。
5. Evidence manifest 的 retention/redaction 存储策略；由 A3/A4 owner（`Infrastructure/Security`、`Operations/Backend`）承接。

**Plan fix 1 open-question dispositions（OQ-01..OQ-04）**：

- **OQ-01（已解决，非开放）**：`expected.external` 是 final authoritative projection；step `expectedResponse` 是 step-local；最后一个匹配 `(operationRef, field)` 的 step 必须与 root 一致，冲突用 `A5_EXTERNAL_PROJECTION_CONTRADICTION`（§4.6/§8.3）。
- **OQ-02（已解决，非开放）**：§4.7 owner registry 同时约束 scenario owner/evidenceOwner 与 A5 residual owner；上游 Q/A3/A4 owner/destination 不被重命名。
- **OQ-03（controller-accepted decision，非开放）**：静态 S2 `authorityRegistry` 为空；PASS/production acceptance 在未来授权证据源前故意不可达；typed `A5-RES-02`，非 blocker，禁止伪造 authority row。
- **OQ-04（已解决，非开放）**：IVR decision 等 `frozen … result` row 由 §4.9.1 closed classification 解析为 empty state set + `FROZEN_RESULT`，不 fail；未列入 closed list 的 arrowless/ambiguous row 仍 fail closed。

以上（含 OQ-03）均为 typed deferred/evidence dependency 或已记录决定，不是 invent 许可，也不是计划 blocking question。

### 12.7 停止条件

遇到以下任一情况，停止实现并报告，不得静默决定：

- 任一 required source ID 无法解析、解析歧义，或 A0-A4 计数/摘要 drift 需要改上游契约才能继续；
- 需要新增/修改产品状态、Problem code、role、trust boundary、API、table/column、event 或 capacity formula；
- 需要发明 timeout/SLO/RPO/RTO/retention/concurrency/price/Region/AZ/SKU/vendor capability 或任何产品策略值；
- 需要把 synthetic/static/prototype fixture 当成 runtime evidence，或需要调用方字段/计数产生 PASS；
- 需要填充 acceptance authority、evidence ledger 或执行真实 runtime/cloud/line/manual 测试；
- A0-A4 回归失败且原因不在 A5 允许范围；
- 需要新增 public production contract、migration、CI platform 或 runtime module；
- 发现两个 accepted specs 对同一事实/状态/owner 冲突，无法用引用而非改契约解决；
- A0 真实 state-machine row 无法按 §4.9.1 的 closed shapes 解析，或需要把 derived state 当 mutable state、把 `A0:TX:*` 当 transition edge 才能继续；
- owner registry 无法用 closed ID + exact upstream whole-cell mapping 满足，或需要拆 slash 字符串推断组件/新建 IAM 角色才能继续。

### 12.8 Completion report 格式

每个 slice 报告固定字段：

```text
taskLabel, provider/model, gate, baseCommit, sliceId
changedFiles[] with SHA-256 and line counts
artifact inventory: families/paths/axes/coverage/scenarios/N-A/deferred
source baseline: A0/A1/A2/A3/A4 counts/digests/ID sets
commands executed + exact exit code + key stdout lines + stderr
semantic assertions observed (coverage_missing=0, source_unresolved=0,
  a3_residuals=18, a0_state_rows=31, a0_shape_unrecognized=0,
  owner_upstream_unresolved=0, residual_owner_unknown=0,
  transition_ref_invalid=0, path_family_duplicate=0,
  external_projection_contradiction=0,
  terminal_mutation=0, duplicate_side_effect=0, ordering_regression=0,
  denied_with_business_effect=0, caller_control=0, synthetic_acceptance=0,
  runtime_evidence=NOT_RUN, production_accepted=false)
clean-checkout note: ROOT-relative commands executed after the A1 bundle
  prerequisite; offline limitation stated; no success claimed without bundle
A0-A4 regression evidence (A1/A2/A3/A4 exact outputs)
negative fixture primary-code evidence
residual owner/destination reconciliation
honesty statement: no runtime/cloud/line/manual execution; no production acceptance
scope statement: no source/prototype/control/PR/commit changes; unrelated dirty paths untouched
open questions/blockers (must state none or exact blocking question)
```

## 13. 无过度设计评估

本计划**不**建设：

- **Test-management platform**：无 suite/run/schedule/result database、无用户/权限/UI、无测试用例管理层；只是一个静态 JSON 矩阵 + validator。
- **Orchestration/live runner**：无 execution engine、无环境编排、无 SUT 连接、无 retry/parallelism/scheduler；所有 runtime 执行留在未来授权 gate。
- **CI system**：不新增 pipeline、checks、webhook、artifact store；只记录手动命令。
- **第二 contract registry**：不复制 A0-A4 的 table/invariant/state/Problem/event/audit/key/formula 表；只保存引用和 drift counts。owner/enum 全部 resolver 自原 registry。
- **Evidence platform**：不存证据、不做 WORM/retention/authorization；只校验 bounded manifest reference。
- **通用 matrix DSL/策略引擎**：字段/enums/axis 固定，validator 是有限语义检查，不是可编程规则引擎。
- **每 cell 一个 fixture**：90 scenarios 是 mandated coverage；fixtures 只有 4 个文件、71 个聚焦 adversarial/invariant/shape cases（`s1-01` 23 + `s1-02` 18 + `s1-03` 13 + `s2-01` 17），用 mutation 证明一般规则。
- **跨产品 workflow**：AI/notary/refund/invoice/work-order/PBX 不纳入；没有 accepted upstream contract 就不发明。
- **运行时/云/线路实现**：不建 Java/UI/IVR/provider/COS/KMS/Redis/DB/queue/load/penetration。

为什么 90 scenarios 不是过度设计：goal contract 明确要求每个 critical family 与 convergence path 的每个 axis 都有 stable scenario 或 N/A；这是规格覆盖的最小表达，不是测试套件数量。为什么 96 cells 不是 count-only：§5/§7 要求六个独立 check 的 AND，且 fixture 专门证明“计数完整但语义/证据缺失”失败。

## 14. 计划要求到章节追踪

| Task requirement | Plan section |
| --- | --- |
| 1. Goal/motivation/success/non-goals/boundary/alignment | §1 |
| 2. First-principles + repo evidence A5 needed/fixtures insufficient | §2 |
| 3. Precise matrix data contract | §4.1-§4.9、附录 A |
| 4. Coverage rules, no count-only, N/A/deferred | §4.3、§4.4、§5 |
| 5. Source-ref integrity/ownership without copying | §4.9、§6 |
| 6. Acceptance result state machine / fail-closed | §7 |
| 7. Exact files/modules/interfaces/schemas/validator entry; public contract decision | §3、§8.1 |
| 8. Behaviorally bounded slices ≤3 | §11 |
| 9. Implementation decisions/data flow/algorithms/error IDs/determinism/duplicate/invalid-vs-missing/negative effects | §4.6、§8.2-§8.5 |
| 10. Focused fixtures/adversarial cases | §9 |
| 11. Exact validation commands/semantic assertions/regressions/doc integrity | §10 |
| 12. Documentation/migration/rollout/residual/open questions/stop/completion report | §12 |
| 13. Over-design assessment | §13 |
| Plan review F-A5-PR-01: A3 residual real source + 18-row baseline + positive/unresolved cases | §2.3、§4.9、§5.8、§9.1（s1-01） |
| Plan review F-A5-PR-02/OQ-04: A0 real state/edge parser + closed classifications + shape fixture | §4.9.1、§8.3.1、§9.3 |
| Plan review F-A5-PR-03/OQ-02: closed owner registry + exact upstream mapping + residual owners | §4.7、§5.10、§6.2、§12.5、§9.1（s1-01） |
| Plan review F-A5-PR-04: A0 edge transitionRef + single root tier + negative cases | §4.5、§4.6、§5.11、§8.3.1/15、§9.2（s1-02） |
| Plan review F-A5-PR-05: PATH-DEPLOY acceptance-boundary differentiation + uniqueness check | §4.2、§5.12、§8.3.13、附录 A |
| Plan review F-A5-PR-06: clean-checkout ROOT-relative commands + A1 bundle prerequisite | §10.1、§10.2、§10.3 |
| OQ-01/OQ-03: external projection authority + empty-authority acceptance | §4.6、§7.3、§12.6 |

## 15. 计划完成定义

- 本文件覆盖 §1-§14 全部 required content；无 blocking open question。
- 计划只提出两个 slice；每个 slice 有独立 behavior/evidence 边界和可复现断言。
- 明确不新增 public contract/migration/runtime/CI/第二 registry；明确 static/model/fixture 只能证明 specification，不能产生 production acceptance。
- 明确 exact planned files、validator CLI、JSON schema/字段、error IDs、fixture cases、commands 和 completion report；附录 A 固定 90 个 scenario ID/axis/outcome anchors/tier。
- 冻结 A0 31-row state/edge parser、closed owner registry、A3 residual 真源、A0 edge transitionRef、单一 root tier 与 PATH↔FAM 唯一性规则；实现不得在 S1 重新协商这些语义。
- 后续实现只需按本计划生成 docs/19、model、validator、4 个 fixtures、README 行和两份 report，无需重新决定产品状态、证据权威或生产阈值。

## 附录 A. Planned scenario inventory（90）

`Primary source anchors` 是每个 scenario 必须包含的最小锚点集合；实现时可按需要补充更精确的 op/event/fixture ref，但不得少于这些锚点，且所有 ref 必须解析。`Tiers` 是 `requiredEvidenceTiers` 的计划基线；任何变更必须报告为 plan deviation。

| Scenario ID | Axis | Planned acceptance outcome | Primary source anchors | Tiers |
| --- | --- | --- | --- | --- |
| `A5-IDENT-NORMAL-01` | NORMAL | 登录身份解析到唯一账户/可用 workspace；无跨身份替代 | `A0:INV:INV-01`、`A0:INV:INV-02`、`A0:INV:INV-03`、`A1:OP:getCustomerMe`、`A3:TS:TS1` | `INTEGRATION` |
| `A5-IDENT-DUPLICATE-01` | DUPLICATE | 并发/重复 provider identity bind 返回同一账户或稳定 conflict，不产生第二身份 | `A0:INV:INV-02`、`A0:TX:T-S1-05`、`A0:SM:Customer login identity`、`A3:TS:TS1` | `INTEGRATION` |
| `A5-IDENT-TIMEOUT-01` | TIMEOUT | session/credential 过期后不可继续；TTL 只引用 Q1/Q19 policy input | `A3:CRED:C01`、`A3:DEFERRED:Q1`、`A3:DEFERRED:Q19`、`A1:PROBLEM:UNAUTHENTICATED` | `INTEGRATION` |
| `A5-IDENT-OUT_OF_ORDER-01` | OUT_OF_ORDER | identity bind 与 account/employee disable 的两种串行顺序均按当前状态 fail closed | `A0:INV:INV-02`、`A0:TX:T-S1-05`、`A0:TX:T-S1-10`、`A3:TS:TS2` | `INTEGRATION` |
| `A5-IDENT-RECOVERY-01` | RECOVERY | operations re-enable/recover 只追加对应 transition/audit，不恢复旧会话 | `A0:SM:Customer account`、`A0:SM:Employee account`、`A0:INV:INV-04`、`A0:INV:INV-29` | `INTEGRATION` |
| `A5-IDENT-PERMISSION_DENIED-01` | PERMISSION_DENIED | 跨身份/跨 workspace/错误 role 被拒绝，零业务写入 | `A0:INV:INV-01`、`A0:INV:INV-03`、`A1:PROBLEM:FORBIDDEN`、`A3:TB:TB2` | `INTEGRATION` |
| `A5-SUBJ-NORMAL-01` | NORMAL | 已验证个人 claim 追加 ACCEPTED，owner 仅从 null 设置 | `A0:INV:INV-05`、`A0:INV:INV-07`、`A1:OP:createCustomerSubjectClaim`、`A0:TX:T-S1-06` | `INTEGRATION` |
| `A5-SUBJ-DUPLICATE-01` | DUPLICATE | 同 request replay 返回首个结果；不同上下文为稳定 conflict | `A0:INV:INV-07`、`A1:PROBLEM:CLAIM_REPLAY_CONFLICT`、`A1:PROBLEM:CLAIM_CONTEXT_CONFLICT` | `INTEGRATION` |
| `A5-SUBJ-OUT_OF_ORDER-01` | OUT_OF_ORDER | OPEN risk 先于 replay/duplicate success 检查；风险阻止 claim | `A0:INV:INV-07`、`A0:SM:Subject risk state`、`A1:PROBLEM:CLAIM_RISK_BLOCKED`、`A0:TX:T-S1-08` | `INTEGRATION` |
| `A5-SUBJ-RECOVERY-01` | RECOVERY | risk CLEARED/rebind 只追加合规事实，不自动转移 owner | `A0:SM:Caller-number binding`、`A0:SM:Subject risk state`、`A0:TX:T-S1-07`、`A0:TX:T-S1-08` | `INTEGRATION` |
| `A5-SUBJ-PERMISSION_DENIED-01` | PERMISSION_DENIED | 组织所有权未批准/已有不同 owner 时 fail closed，零 owner 变更 | `A0:INV:INV-05`、`A0:TX:T-S1-06`、`A0:SM:Subject ownership` | `INTEGRATION` |
| `A5-ACTIV-NORMAL-01` | NORMAL | 显式 ACCEPT 产生 consent、activation transition、首次 trial/grant/balance/Outbox | `A0:INV:INV-08`、`A0:INV:INV-09`、`A0:INV:INV-16`、`A1:OP:createCustomerSubjectActivation`、`A0:OUTBOX:FIRST_TRIAL_GRANTED` | `INTEGRATION` |
| `A5-ACTIV-DUPLICATE-01` | DUPLICATE | 同 activation event/fingerprint replay 返回首个结果；不改写 resolve/decision | `A0:INV:INV-37`、`A1:OP:confirmIvrActivation`、`A2:FIX:s1-04-decision-exact-replay` | `INTEGRATION` |
| `A5-ACTIV-TIMEOUT-01` | TIMEOUT | trial policy valid_days/expiry 只由 policy input 决定；过期不重发 trial | `A0:INV:INV-16`、`A0:SM:Trial`、`A0:TABLE:entitlement_metering.trial_policy_version` | `INTEGRATION` |
| `A5-ACTIV-OUT_OF_ORDER-01` | OUT_OF_ORDER | confirmation 与 SUSPEND 的先后顺序均不把 CONFIRMED session 改成 REJECTED | `A0:INV:INV-10`、`A0:INV:INV-37`、`A2:FIX:s1-03-activation-redecision`、`A1:OP:confirmIvrActivation` | `INTEGRATION` |
| `A5-ACTIV-RECOVERY-01` | RECOVERY | operations resume 只追加 SUSPENDED→ACTIVE，不重发 trial；consent gap 可恢复 | `A0:INV:INV-10`、`A0:TX:T-S2-09`、`A0:TX:T-S2-10`、`A1:OP:resumeOperationsSubject` | `INTEGRATION` |
| `A5-ACTIV-PERMISSION_DENIED-01` | PERMISSION_DENIED | fresh SUSPENDED confirmation 返回 REJECT_SUSPENDED；无接受 consent/trial/reservation | `A0:INV:INV-10`、`A0:INV:INV-08`、`A1:PROBLEM:ACTIVATION_STATE_CONFLICT` | `INTEGRATION` |
| `A5-COMM-NORMAL-01` | NORMAL | order → verified payment → issuance → grant/balance/Outbox，快照冻结 | `A0:INV:INV-21`、`A0:INV:INV-22`、`A0:INV:INV-23`、`A1:OP:createCustomerOrder`、`A0:OUTBOX:ENTITLEMENT_ISSUED` | `INTEGRATION` |
| `A5-COMM-DUPLICATE-01` | DUPLICATE | duplicate order/callback 返回首结果或 accepted duplicate，不重复发权益 | `A0:INV:INV-21`、`A0:INV:INV-22`、`A3:FIX:sec-s2-06-provider-signature-replay`、`A1:OP:receivePaymentProviderCallback` | `INTEGRATION` |
| `A5-COMM-TIMEOUT-01` | TIMEOUT | payment UNKNOWN/回调超时保持 UNKNOWN，阻断二次结账并查询原订单 | `A0:INV:INV-22`、`A1:PROBLEM:ORDER_UNKNOWN_PAYMENT_PENDING`、`A1:PROBLEM:CALLBACK_RETRY_LATER` | `INTEGRATION` |
| `A5-COMM-OUT_OF_ORDER-01` | OUT_OF_ORDER | 多支付事件/分期 issuance 乱序不产生第二 grant；partial failure 可观察 | `A0:INV:INV-23`、`A0:INV:INV-36`、`A0:TX:T-S2-08` | `INTEGRATION` |
| `A5-COMM-RECOVERY-01` | RECOVERY | FAILED issuance 重试只从 FAILED→PENDING，不重复 grant | `A0:INV:INV-36`、`A0:SM:Entitlement issuance`、`A0:TX:T-S2-08` | `INTEGRATION` |
| `A5-COMM-PERMISSION_DENIED-01` | PERMISSION_DENIED | 缺 purchase consent/实名/ACTIVE activation 时拒绝下单，零订单/发放 | `A0:INV:INV-08`、`A0:INV:INV-21`、`A1:PROBLEM:PURCHASE_CONSENT_REQUIRED`、`A1:PROBLEM:REAL_NAME_REQUIRED`、`A1:PROBLEM:ACTIVATION_NOT_ACTIVE` | `INTEGRATION` |
| `A5-IVR-NORMAL-01` | NORMAL | resolve 原子 claim + call + reservation + frozen decision + terminal settlement | `A0:INV:INV-11`、`A0:INV:INV-12`、`A0:INV:INV-13`、`A0:INV:INV-19`、`A1:OP:resolveIvrInbound`、`A2:FIX:s2-01-task-terminal-completed` | `INTEGRATION+LINE` |
| `A5-IVR-DUPLICATE-01` | DUPLICATE | duplicate terminal/heartbeat 返回首结果；无第二 reservation/settlement/usage | `A0:INV:INV-19`、`A2:FIX:s2-02-event-duplicate-replay`、`A2:INV:INV-A2-08` | `INTEGRATION+LINE` |
| `A5-IVR-TIMEOUT-01` | TIMEOUT | TTL/authorization_expires/OUTBOUND_NOT_STARTED 按 contract deadline fail closed，不重拨 | `A0:INV:INV-12`、`A2:FIX:s2-07-authorization-expiry-not-started`、`A1:OP:ingestIvrCallEvent` | `INTEGRATION+LINE` |
| `A5-IVR-OUT_OF_ORDER-01` | OUT_OF_ORDER | terminal 后迟到一致的 leg facts 收敛一次；矛盾事实 quarantine | `A2:FIX:s2-03-event-out-of-order`、`A0:INV:INV-19`、`A2:INV:INV-A2-05` | `INTEGRATION+LINE` |
| `A5-IVR-RECOVERY-01` | RECOVERY | IVR restart/DISPATCH_UNKNOWN 只恢复，不重拨；UNKNOWN 只由 reconciliation 收敛 | `A2:FIX:s2-06-ivr-restart-bleg-recovery`、`A2:FIX:s2-04-terminal-unknown-reconciliation`、`A0:INV:INV-38` | `INTEGRATION+LINE` |
| `A5-IVR-PERMISSION_DENIED-01` | PERMISSION_DENIED | 坏签名/nonce replay/跨 workspace/active-call 拒绝；零 session/call/reservation | `A3:FIX:sec-s2-04-ivr-signature-invalid`、`A3:FIX:sec-s2-05-ivr-replay-rejected`、`A0:INV:INV-11`、`A1:PROBLEM:SESSION_IDENTITY_MISMATCH` | `INTEGRATION+LINE` |
| `A5-MEDIA-NORMAL-01` | NORMAL | declare→server observation→complete VALIDATED，服务端 hash/duration 获胜 | `A0:INV:INV-24`、`A0:INV:INV-15`、`A2:FIX:s3-01-recording-validated`、`A1:OP:completeIvrRecording` | `INTEGRATION+CLOUD` |
| `A5-MEDIA-DUPLICATE-01` | DUPLICATE | declaration/completion replay 返回首结果；无第二 recording/media object | `A2:FIX:s3-02-declaration-replay`、`A2:FIX:s3-06-recording-identity-conflict`、`A2:INV:INV-A2-15` | `INTEGRATION+CLOUD` |
| `A5-MEDIA-TIMEOUT-01` | TIMEOUT | upload_expires_at 后 completion QUARANTINED；无成功 media/evidence fact | `A2:FIX:s3-04-upload-expired-quarantine`、`A0:INV:INV-24`、`A1:PROBLEM:UPLOAD_EXPIRED` | `INTEGRATION+CLOUD` |
| `A5-MEDIA-OUT_OF_ORDER-01` | OUT_OF_ORDER | complete before observation 返回 UPLOAD_OBSERVATION_REQUIRED；补 observation 后收敛 | `A2:FIX:s3-03-upload-observation-required`、`A2:FIX:s3-05-storage-unknown-retry`、`A0:INV:INV-24` | `INTEGRATION+CLOUD` |
| `A5-MEDIA-RECOVERY-01` | RECOVERY | STORAGE_UNKNOWN 同 identity 重试收敛 VALIDATED；不伪造 media fact | `A2:FIX:s3-05-storage-unknown-retry`、`A1:PROBLEM:STORAGE_UNKNOWN`、`A2:INV:INV-A2-14` | `INTEGRATION+CLOUD` |
| `A5-MEDIA-PERMISSION_DENIED-01` | PERMISSION_DENIED | upload credential scope/playback/read 越界被拒绝；无永久 URL/跨对象访问 | `A3:FIX:sec-s3-01-upload-credential-scope`、`A3:FIX:sec-s3-02-playback-credential-scope`、`A3:MEDIA:U1`、`A3:MEDIA:P1` | `INTEGRATION+CLOUD` |
| `A5-SHARE-NORMAL-01` | NORMAL | grant→admission→ticket→playback，admission 计数一次 | `A0:INV:INV-26`、`A0:INV:INV-39`、`A1:OP:createPublicShareAdmission`、`A0:TX:T-S3-04` | `INTEGRATION+CLOUD` |
| `A5-SHARE-DUPLICATE-01` | DUPLICATE | 同 admission_key 重放返回原 admission 并重发票据，不增加计数 | `A0:INV:INV-39`、`A1:OP:createPublicShareAdmission`、`A0:SM:Share admission and derived session` | `INTEGRATION+CLOUD` |
| `A5-SHARE-TIMEOUT-01` | TIMEOUT | grant/session expiry 阻止新准入；已准入 session 只在 recorded window 内 | `A0:INV:INV-26`、`A1:PROBLEM:SHARE_EXPIRED`、`A1:PROBLEM:SHARE_SESSION_EXPIRED`、`A3:DEFERRED:Q22` | `INTEGRATION+CLOUD` |
| `A5-SHARE-OUT_OF_ORDER-01` | OUT_OF_ORDER | PostgreSQL admission 先提交，Redis ticket 后派生；Redis 失败不重计 | `A0:INV:INV-39`、`A1:OP:authorizePublicShareMedia`、`A3:FIX:sec-s3-02-playback-credential-scope` | `INTEGRATION+CLOUD` |
| `A5-SHARE-RECOVERY-01` | RECOVERY | lost cache/ticket reconstruction 比较 generation/expiry witness；撤销后不复活 | `A0:INV:INV-39`、`A1:OP:authorizePublicShareMedia`、`A3:CRED:C10`、`A3:DEFERRED:Q22` | `INTEGRATION+CLOUD` |
| `A5-SHARE-PERMISSION_DENIED-01` | PERMISSION_DENIED | wrong tenant/verification failure/revoked generation 被拒绝；零新 admission | `A0:INV:INV-26`、`A1:PROBLEM:SHARE_WRONG_TENANT`、`A1:PROBLEM:SHARE_VERIFICATION_FAILED`、`A3:CRED:C07` | `INTEGRATION+CLOUD` |
| `A5-SEC-NORMAL-01` | NORMAL | 五域认证/least privilege/high-risk audit/redaction 正常链路 | `A3:TS:TS1`、`A3:TS:TS5`、`A3:INV:INV-A3-07`、`A3:INV:INV-A3-14`、`A3:INV:INV-A3-15`、`A1:OP:listOperationsAuditFacts` | `INTEGRATION` |
| `A5-SEC-DUPLICATE-01` | DUPLICATE | nonce replay/provider duplicate 返回首结果或拒绝；无重复业务副作用 | `A3:FIX:sec-s2-05-ivr-replay-rejected`、`A3:FIX:sec-s2-06-provider-signature-replay`、`A3:INV:INV-A3-10`、`A3:INV:INV-A3-11` | `INTEGRATION` |
| `A5-SEC-TIMEOUT-01` | TIMEOUT | timestamp 超窗/nonce TTL 只引用 Q25 policy input；超窗 fail closed | `A3:FIX:sec-s2-04-ivr-signature-invalid`、`A3:DEFERRED:Q25`、`A3:INV:INV-A3-09` | `INTEGRATION` |
| `A5-SEC-OUT_OF_ORDER-01` | OUT_OF_ORDER | verification order 先于 domain side effect；audit failure 整体回滚 | `A3:FIX:sec-s2-04-ivr-signature-invalid`、`A3:INV:INV-A3-09`、`A3:INV:INV-A3-14` | `INTEGRATION` |
| `A5-SEC-RECOVERY-01` | RECOVERY | key rotation/recover/Redis outage 后同 identity 安全重试，不产生第二业务事实 | `A3:FIX:sec-s2-07-signing-key-rotation`、`A3:FIX:sec-s2-08-failclosed-dependency-preserves-a2`、`A3:INV:INV-A3-06`、`A3:INV:INV-A3-17` | `INTEGRATION+CLOUD` |
| `A5-SEC-PERMISSION_DENIED-01` | PERMISSION_DENIED | cross-domain token/wrong role/audit read/红action 被拒绝；零业务写入 | `A3:FIX:sec-s2-02-cross-domain-rejected`、`A3:FIX:sec-s2-03-service-identity-least-privilege`、`A3:FIX:sec-s3-04-audit-access-denied`、`A3:INV:INV-A3-16` | `INTEGRATION` |
| `A5-DEPLOY-NORMAL-01` | NORMAL | 缺证据时 groups INCOMPLETE/NOT_VERIFIED，root readiness 常量 false | `A4:SECTION:8.3`、`A4:CONSTANT:productionReady`、`A4:CONSTANT:cloudVerified`、`A4:FIX:s2-03-deployment-gates`、`DOC12:S13` | `INTEGRATION+CLOUD` |
| `A5-DEPLOY-DUPLICATE-01` | DUPLICATE | 重复 capability/backup dependency 为 INVALID，不掩盖缺失项 | `A4:SECTION:9.1`、`A4:SECTION:10.1`、`A4:FIX:s2-02-capability-assertion`、`A4:FORMULA:F-BACKUP-01`、`DOC12:S13` | `INTEGRATION+CLOUD` |
| `A5-DEPLOY-RECOVERY-01` | RECOVERY | 六依赖 restore 观测缺失/超目标时 UNKNOWN/INCOMPLETE/NOT_MET，不声称可恢复 | `A4:FIX:s2-01-backup-recovery`、`A4:FORMULA:F-BACKUP-01`、`A4:FORMULA:F-BACKUP-02`、`A4:SECTION:9.3`、`DOC12:S13` | `INTEGRATION+CLOUD+MANUAL` |
| `A5-X-ACTIV-NORMAL-01` | NORMAL | IVR confirm→consent/activation→trial→grant/balance/Outbox 全链路一次 | `A0:INV:INV-08`、`A0:INV:INV-16`、`A0:INV:INV-37`、`A1:OP:confirmIvrActivation`、`A0:OUTBOX:FIRST_TRIAL_GRANTED` | `INTEGRATION+LINE` |
| `A5-X-ACTIV-DUPLICATE-01` | DUPLICATE | 同 event/fingerprint confirm replay 不重发 trial/第二 transition | `A0:INV:INV-37`、`A2:FIX:s1-04-decision-exact-replay`、`A0:INV:INV-16` | `INTEGRATION+LINE` |
| `A5-X-ACTIV-TIMEOUT-01` | TIMEOUT | 授权窗口/trial expiry 只按 policy input；过期不重发 trial | `A0:INV:INV-16`、`A3:DEFERRED:Q25`、`A0:SM:Trial` | `INTEGRATION+LINE` |
| `A5-X-ACTIV-OUT_OF_ORDER-01` | OUT_OF_ORDER | confirmation 与 suspend 交叉顺序不把 CONFIRMED 改 REJECTED | `A0:INV:INV-10`、`A0:INV:INV-37`、`A2:FIX:s1-03-activation-redecision`、`A0:SM:Inbound session` | `INTEGRATION+LINE` |
| `A5-X-ACTIV-RECOVERY-01` | RECOVERY | consent gap/operations resume 后 activation/trial 状态一致，不重发 trial | `A0:INV:INV-10`、`A0:TX:T-S2-09`、`A0:TX:T-S2-10`、`A0:SM:Service activation` | `INTEGRATION+LINE` |
| `A5-X-ACTIV-PERMISSION_DENIED-01` | PERMISSION_DENIED | suspended/role/workspace denial 无 activation/trial/Outbox 成功事实 | `A0:INV:INV-10`、`A0:INV:INV-08`、`A1:PROBLEM:ACTIVATION_STATE_CONFLICT`、`A1:OP:confirmIvrActivation` | `INTEGRATION+LINE` |
| `A5-X-COMM-NORMAL-01` | NORMAL | callback→payment→order→issuance→grant/balance/Outbox 全链路一次 | `A0:INV:INV-21`、`A0:INV:INV-22`、`A0:INV:INV-23`、`A0:OUTBOX:ENTITLEMENT_ISSUED`、`A1:OP:receivePaymentProviderCallback` | `INTEGRATION` |
| `A5-X-COMM-DUPLICATE-01` | DUPLICATE | 同 provider event/payload replay 幂等；changed payload conflict；不发第二权益 | `A0:INV:INV-22`、`A3:FIX:sec-s2-06-provider-signature-replay`、`A1:PROBLEM:PAYMENT_EVENT_CONFLICT` | `INTEGRATION` |
| `A5-X-COMM-TIMEOUT-01` | TIMEOUT | UNKNOWN payment 保持占用并查询原订单，不创建第二订单 | `A0:INV:INV-22`、`A1:PROBLEM:ORDER_UNKNOWN_PAYMENT_PENDING`、`A1:PROBLEM:CALLBACK_RETRY_LATER`、`A0:TX:T-S2-07` | `INTEGRATION` |
| `A5-X-COMM-OUT_OF_ORDER-01` | OUT_OF_ORDER | 乱序支付/issuance 不产生第二 grant；partial line failure 可观察 | `A0:INV:INV-23`、`A0:INV:INV-36`、`A0:SM:Entitlement issuance` | `INTEGRATION` |
| `A5-X-COMM-RECOVERY-01` | RECOVERY | FAILED issuance 重试不重复 grant；Outbox 同 identity | `A0:INV:INV-36`、`A0:INV:INV-28`、`A0:TX:T-S2-08` | `INTEGRATION` |
| `A5-X-COMM-PERMISSION_DENIED-01` | PERMISSION_DENIED | 未实名/未 ACTIVE/无 purchase consent 时零订单/发放 | `A0:INV:INV-21`、`A1:PROBLEM:REAL_NAME_REQUIRED`、`A1:PROBLEM:ACTIVATION_NOT_ACTIVE`、`A1:PROBLEM:PURCHASE_CONSENT_REQUIRED`、`A1:OP:createCustomerOrder` | `INTEGRATION` |
| `A5-X-IVR-NORMAL-01` | NORMAL | task claim→reservation→terminal→usage fact→outbox 一次 | `A0:INV:INV-13`、`A0:INV:INV-19`、`A2:FIX:s2-01-task-terminal-completed`、`A0:OUTBOX:USAGE_SETTLED` | `INTEGRATION+LINE` |
| `A5-X-IVR-DUPLICATE-01` | DUPLICATE | duplicate terminal/response-loss retry 无第二 reservation/settlement | `A2:FIX:s2-02-event-duplicate-replay`、`A2:FIX:s2-05-response-loss-retry`、`A0:INV:INV-19` | `INTEGRATION+LINE` |
| `A5-X-IVR-TIMEOUT-01` | TIMEOUT | TTL/authorization expiry 后不 dispatch；OUTBOUND_NOT_STARTED 释放一次 | `A2:FIX:s2-07-authorization-expiry-not-started`、`A0:INV:INV-12`、`A0:INV:INV-38` | `INTEGRATION+LINE` |
| `A5-X-IVR-OUT_OF_ORDER-01` | OUT_OF_ORDER | terminal first + late facts 收敛；无第二 settlement/release | `A2:FIX:s2-03-event-out-of-order`、`A2:INV:INV-A2-05`、`A0:INV:INV-19`、`A0:TX:T-S2-12` | `INTEGRATION+LINE` |
| `A5-X-IVR-RECOVERY-01` | RECOVERY | restart/reconciliation 收敛 UNKNOWN，不重拨、不双结算 | `A2:FIX:s2-04-terminal-unknown-reconciliation`、`A2:FIX:s2-06-ivr-restart-bleg-recovery`、`A0:INV:INV-38`、`A2:INV:INV-A2-05` | `INTEGRATION+LINE` |
| `A5-X-IVR-PERMISSION_DENIED-01` | PERMISSION_DENIED | signature/replay/workspace/active-call denial 零 claim/reservation/settlement | `A3:FIX:sec-s2-04-ivr-signature-invalid`、`A3:FIX:sec-s2-05-ivr-replay-rejected`、`A0:INV:INV-11`、`A0:TX:T-S2-11` | `INTEGRATION+LINE` |
| `A5-X-MEDIA-NORMAL-01` | NORMAL | recording validated→media object→evidence record/creation event/artifact 全链路，solidification 独立 | `A0:INV:INV-24`、`A0:INV:INV-25`、`A2:FIX:s3-08-end-to-end-task-media`、`A0:EVIDENCE-EVENT:EVIDENCE_CREATED` | `INTEGRATION+CLOUD` |
| `A5-X-MEDIA-DUPLICATE-01` | DUPLICATE | declaration/completion/evidence creation replay 返回首结果，无第二 recording/media/evidence | `A2:FIX:s3-02-declaration-replay`、`A2:FIX:s3-06-recording-identity-conflict`、`A0:INV:INV-25` | `INTEGRATION+CLOUD` |
| `A5-X-MEDIA-TIMEOUT-01` | TIMEOUT | upload expiry 后 quarantine，不产生 media/evidence success fact | `A2:FIX:s3-04-upload-expired-quarantine`、`A0:INV:INV-24`、`A1:PROBLEM:UPLOAD_EXPIRED`、`A0:TX:T-S3-01` | `INTEGRATION+CLOUD` |
| `A5-X-MEDIA-OUT_OF_ORDER-01` | OUT_OF_ORDER | complete before observation/observation after upload 顺序按 contract 收敛 | `A2:FIX:s3-03-upload-observation-required`、`A2:FIX:s3-05-storage-unknown-retry`、`A2:INV:INV-A2-09` | `INTEGRATION+CLOUD` |
| `A5-X-MEDIA-RECOVERY-01` | RECOVERY | STORAGE_UNKNOWN 同 identity 重试，evidence 只在 VALIDATED 后创建 | `A2:FIX:s3-05-storage-unknown-retry`、`A0:INV:INV-25`、`A2:INV:INV-A2-14` | `INTEGRATION+CLOUD` |
| `A5-X-MEDIA-PERMISSION_DENIED-01` | PERMISSION_DENIED | upload/playback credential/object scope denial 零 evidence/share 成功事实 | `A3:FIX:sec-s3-01-upload-credential-scope`、`A3:FIX:sec-s3-02-playback-credential-scope`、`A3:INV:INV-A3-12` | `INTEGRATION+CLOUD` |
| `A5-X-SHARE-NORMAL-01` | NORMAL | grant→admission→ticket→playback/access event 全链路，admission 计数一次 | `A0:INV:INV-26`、`A0:INV:INV-39`、`A1:OP:createPublicShareAdmission`、`A1:OP:authorizePublicShareMedia` | `INTEGRATION+CLOUD` |
| `A5-X-SHARE-DUPLICATE-01` | DUPLICATE | same admission key replay 返回原 admission + 重发票据，不重计 | `A0:INV:INV-39`、`A1:OP:createPublicShareAdmission`、`A0:SM:Share admission and derived session`、`A0:TX:T-S3-04` | `INTEGRATION+CLOUD` |
| `A5-X-SHARE-TIMEOUT-01` | TIMEOUT | share/session expiry 阻止新/续期访问；已准入窗口只按 recorded expiry | `A0:INV:INV-26`、`A1:PROBLEM:SHARE_EXPIRED`、`A3:DEFERRED:Q22`、`A1:PROBLEM:SHARE_SESSION_EXPIRED` | `INTEGRATION+CLOUD` |
| `A5-X-SHARE-OUT_OF_ORDER-01` | OUT_OF_ORDER | admission commit 先于 ticket；Redis failure 不重计且 fail closed | `A0:INV:INV-39`、`A1:OP:authorizePublicShareMedia`、`A3:FIX:sec-s3-02-playback-credential-scope`、`A0:TX:T-S3-05` | `INTEGRATION+CLOUD` |
| `A5-X-SHARE-RECOVERY-01` | RECOVERY | ticket reconstruction/generation witness 拒绝 stale admission | `A0:INV:INV-39`、`A1:OP:authorizePublicShareMedia`、`A3:CRED:C10`、`A0:TX:T-S3-05` | `INTEGRATION+CLOUD` |
| `A5-X-SHARE-PERMISSION_DENIED-01` | PERMISSION_DENIED | wrong tenant/verification/revocation denial 零新 admission | `A0:INV:INV-26`、`A1:PROBLEM:SHARE_WRONG_TENANT`、`A1:PROBLEM:SHARE_REVOKED`、`A3:CRED:C08` | `INTEGRATION+CLOUD` |
| `A5-X-AUTH-NORMAL-01` | NORMAL | five-domain auth→domain mutation→append-only audit/high-risk/redaction 正常链路 | `A3:TS:TS1`、`A3:TS:TS5`、`A3:INV:INV-A3-07`、`A3:INV:INV-A3-14`、`A3:INV:INV-A3-15`、`A1:OP:listOperationsAuditFacts`、`A3:AUDIT:SUBJECT_SUSPEND` | `INTEGRATION` |
| `A5-X-AUTH-DUPLICATE-01` | DUPLICATE | nonce/provider duplicate 首结果；audit 不重复/不伪造 | `A3:FIX:sec-s2-05-ivr-replay-rejected`、`A3:FIX:sec-s2-06-provider-signature-replay`、`A3:INV:INV-A3-10`、`A3:INV:INV-A3-11` | `INTEGRATION` |
| `A5-X-AUTH-TIMEOUT-01` | TIMEOUT | timestamp window Q25 超窗拒绝，零 domain side effect | `A3:FIX:sec-s2-04-ivr-signature-invalid`、`A3:DEFERRED:Q25`、`A3:INV:INV-A3-09`、`A3:INV:INV-A3-17` | `INTEGRATION` |
| `A5-X-AUTH-OUT_OF_ORDER-01` | OUT_OF_ORDER | auth failure before side effect；audit transaction failure rollback | `A3:FIX:sec-s2-04-ivr-signature-invalid`、`A3:INV:INV-A3-14`、`A3:INV:INV-A3-17`、`A0:TX:T-S1-03` | `INTEGRATION` |
| `A5-X-AUTH-RECOVERY-01` | RECOVERY | key rotation/Redis outage 后同 identity 安全重试，audit 连续 | `A3:FIX:sec-s2-07-signing-key-rotation`、`A3:FIX:sec-s2-08-failclosed-dependency-preserves-a2`、`A3:INV:INV-A3-06`、`A3:INV:INV-A3-17` | `INTEGRATION+CLOUD` |
| `A5-X-AUTH-PERMISSION_DENIED-01` | PERMISSION_DENIED | cross-domain/wrong role/audit read denial 零业务写入 | `A3:FIX:sec-s2-02-cross-domain-rejected`、`A3:FIX:sec-s3-04-audit-access-denied`、`A3:INV:INV-A3-16`、`A3:TB:TB2` | `INTEGRATION` |
| `A5-X-DEPLOY-NORMAL-01` | NORMAL | A4 capability evidence（`cloudVerified=false`、`procurementReady=false`、`a3RiskClosed=false`）跨 A5 边界后只能 `BLOCKED`（CLOUD/MANUAL），A5 不发 PASS、不提升 readiness | `A4:SECTION:10.2`、`A4:CONSTANT:procurementReady`、`A4:CONSTANT:a3RiskClosed`、`A4:FORMULA:F-CAP-01`、`DOC12:S13` | `INTEGRATION+CLOUD+MANUAL` |
| `A5-X-DEPLOY-DUPLICATE-01` | DUPLICATE | 同一 A4 evidence 跨 A5 边界重放：同 digest 恰好准入一次；同 identity 不同 digest => immutability violation；重复证据不能产生 positive gate | `A4:FIX:s2-01-backup-recovery`、`A4:SECTION:10.3`、`A4:CONSTANT:cloudVerified`、`DOC12:S13` | `INTEGRATION+CLOUD+MANUAL` |
| `A5-X-DEPLOY-RECOVERY-01` | RECOVERY | restore/closure evidence 在 CLOUD/MANUAL tier 缺失或 NOT_MET => `EVIDENCE_VALID`/`BLOCKED`，A5 不作 restore claim、不从 A4 算术推断恢复成功 | `A4:CONSTANT:a3RiskClosed`、`A4:SECTION:10.4`、`A4:FIX:s2-01-backup-recovery`、`A4:FORMULA:F-BACKUP-02`、`DOC12:S13` | `INTEGRATION+CLOUD+MANUAL` |
| `A5-X-DEPLOY-PERMISSION_DENIED-01` | PERMISSION_DENIED | caller-forged acceptance record、synthetic capability claim 或 registry 外 authority 在 A5 边界被拒绝；A4 evidence 不能授予 acceptance authority；零 accepted record | `A4:SECTION:10.2`、`A4:CONSTANT:productionReady`、`A4:FORMULA:F-CAP-01`、`A4:SECTION:8.3`、`DOC12:S13` | `INTEGRATION+CLOUD+MANUAL` |

- 上表恰好 90 个 scenario ID，覆盖 §4.4 的 90 个 represented cell；6 个 N/A cell 不生成 scenario。
- 每个 scenario 至少从本行 anchors 指定一个 `outcomeRef`（`⊆ sourceRefs`）。每个 `PATH-*` row 必须至少有一个 sourceRef 和一个 outcomeRef 不被同 axis 的 paired family row 共享；上表 `A5-X-DEPLOY-*` 已按 A4→A5 acceptance-boundary observation 重写，不得退回为 `FAM-DEPLOYMENT` 的复述或 validator 计数自测。
- `A5_ACCEPT_RUNTIME_TIER_AND_AUTHORITY` 是 rule ID，不是 source ref；`A5-RES-*` 是 §12.5 的 residual ID，不是 A0-A4 source ref，validator 只按其 residual registry 解析。相关 scenario 的 `residualIds` 必须引用具体 `A5-RES-*`，不得写进 `sourceRefs`。
- 表中所有 `A1:PROBLEM:*` 必须由 A1 bundle 的 `ProblemCode.enum`/`x-onecall-error-codes` 实际存在性校验；若 A1 名称不同，以 A1 registry 为准并更新本表，不新增 Problem code。
- 表中 `A3:MEDIA:U1`/`A3:MEDIA:P1` 是 A3 `mediaPolicies` 的 policyId；`A3:CRED:C07/C08/C10` 是 A3 `credentialClasses`；`A0:SM:...` 必须由 §4.9 resolver 解析。
