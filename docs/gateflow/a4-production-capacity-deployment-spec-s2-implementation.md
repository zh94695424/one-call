# A4 生产容量与部署规格 S2 实施报告

> Task label：`a4-capacity-s2-impl-ds-20261026-01`
>
> Provider/model：DeepSeek / `deepseek-flash`（Claude Code sub-agent）
>
> Gate：S2 implementation slice（部署/恢复/能力证据与可发现性）
>
> Base：`04c63de87996d14d0d3701b399edf46e2a921ed9`（accepted S1 checkpoint；先前报告中的 `04c63de0…` 为转录错误，见 §11）
>
> Slice：S2 implementation + controller 指定的有界 review-fix 轮；未进入 re-review、后续 Gate、commit、push、PR 或云操作。

## 1. 结论与验证状态（必须先读）

**Author-complete，但 Node 运行时验证被环境权限阻断。** 允许路径内的代码、模板、fixtures 与文档已写入，并通过：

- `jq empty` 严格 JSON 解析（模板 + 3 个 S2 fixtures，exit 0，无输出）；
- 模板顶层 9 组检查、全部叶 `value:null`、`provenance/approval` 全 `MISSING`、`MEASURED_ROLE=[]`；
- `sha256sum` 哈希与 `wc -l` 行数；
- README 中 A4 发现性链接静态唯一性检查（单行命中）。

**未运行**：`node --check`、`node deployment/capacity/validate.mjs --stage s1`、`--stage s2`、模板 9 组行为断言、S1/S2 fixture 行为断言、程序化 NaN/±Infinity probe、repeatability 与 README 运行时检查。原因：本会话 Bash 权限对 node 文件执行返回 `This command requires approval`；`node --version` 可运行（`v22.23.4`），但所有 `node --check`/validator 命令均未启动。因此**没有 exit code、没有行为断言通过结果**，本报告不声称验证器已通过，也不声称任何容量、云能力、性能或恢复结论。

**2026-10-26 有界 review-fix 轮（`a4-capacity-s2-review-fix-ds-20261026-01`）**：已处理 F-S2-R1-01（六依赖 exact-set/重复 ID）、F-S2-R1-02（能力 exact-set/重复 ID）、F-S2-R1-04（base hash）与 F-S2-R1-05（F-BACKUP-02 归因），并按 F-S2-R1-03 的口径记录 EOL 事实（见 §11）。本会话 node 执行仍被权限阻断，运行时结论仍待 controller 复核。

请 controller 在允许 node 的环境中运行 §5.2 命令后再确认 S2 完成。

## 2. 变更文件（仅允许路径）

| 文件 | SHA-256 | 行数 |
| --- | --- | --- |
| `deployment/capacity/calc.mjs` | `0ba8947561d7994e082a2f60f82054530216146cace5d2a3959e5befb3d3d891` | 4440 |
| `deployment/capacity/validate.mjs` | `593dd7e783b7f93b0f382b170c6de5eb3dfe428b9c96806e0941d2ca725e4063` | 667 |
| `deployment/capacity/inputs.template.json` | `cb11e7c9b69e34cfc71a838a5e960626e386045a4ad7908e2511885ccf339568` | 891 |
| `deployment/capacity/fixtures/s2-01-backup-recovery.json` | `be41f76c55d475d04f8624e09d329fadef93e8a15c85d9a85bcbf071d6312ec6` | 543 |
| `deployment/capacity/fixtures/s2-02-capability-assertion.json` | `9ca4348293d92b463ece460adc1b6261fd5dcda9612e20ea7fac3d78ed52a0f8` | 270 |
| `deployment/capacity/fixtures/s2-03-deployment-gates.json` | `2e895ca2c854f6547e0c1a45eaff09b0405bf9b13e52e7a9a299c5e20010402e` | 183 |
| `docs/18-production-capacity-and-deployment-spec.md` | `d98e907cccf41aa7abd57139f841f3c3598487f0a0b7f4967c265691596e8d46` | 499 |
| `docs/README.md` | `e1400278f0143413cd27970bf7cfbf39838677a3a06b84df9c3f3f42cc13fef4` | 151 |
| `docs/gateflow/a4-production-capacity-deployment-spec-s2-implementation.md` | 本报告 | — |

- `docs/README.md` 仅新增计划指定的单行入口：`| [18-production-capacity-and-deployment-spec.md](18-production-capacity-and-deployment-spec.md) | 参数化容量、部署、恢复与证据清单 | 架构、运维、安全 |`。
- **EOL 事实（F-S2-R1-03）**：`docs/README.md` 的内容增量是单行入口（Git 语义 diff 为 1 insertion / 0 deletions，controller 在 `core.autocrlf=true` 下声明）；但本 checkout 的工作区文件使用 CRLF（`file` 输出 “with CRLF line terminators”），而 HEAD blob 为 LF。本会话 `git config --get core.autocrlf` 退出 1（未设置），因此未规范化的原始 `git diff --numstat` 显示 151/150，这是换行符差异而非内容重写。按任务约束**未改写 README**；上表哈希对应 CRLF 工作区字节。
- 未修改 S1 fixtures、A0-A3 文档/契约、OpenAPI/schema、原型、截图、共享 QA、安全/协议 registry、control doc。
- 未新增依赖、endpoint、表、Problem code、协议事件或公共契约。

## 3. 实现范围与公式对应

### 3.1 S2 新增组（模板 9 组 = S1 7 组 + 2 组）

| 组 | 输入 | 输出 metric（示例） |
| --- | --- | --- |
| `backupRecovery` | 顶层 `asOf`；`BACKUP_DEPENDENCY[]`（六项固定 `dependencyId`） | `backupRecovery.<dependencyId>`、`backupRecovery.closure`、空集合时的 `backupRecovery.dependencies` |
| `capabilityEvidence` | `CAPABILITY[]`（`CV-1..CV-5` + `regionAzSku`） | `capabilityEvidence.<capabilityId>`、空集合时的 `capabilityEvidence.summary` |

模板为 additive 编辑：S1 七组结构、字段与 fixture 语义未改。

### 3.2 `F-BACKUP-01/02`

- 每依赖读取 `dependencyId`、普通布尔 `included`、`approvedRpoSeconds`/`approvedRtoSeconds`（`seconds`、`NonNegativeMeasure`、零合法）、`restoreTestedAt`（`iso8601`）、可选 `observedRpoSeconds`/`observedRtoSeconds`、`owner`、`evidenceRef`。
- `asOf` 缺失且存在依赖 => `ASOF_REQUIRED`（metric 级 `INCOMPLETE`，不是 root error）；无 `asOf` 不做未来比较并带 `ASOF_ABSENT_NO_FUTURE_CHECK`。
- `restoreTestedAt` 不可解析 => `DATE_INVALID`；晚于 `asOf` => `DATE_IN_FUTURE`；两者 `INVALID`。
- `included` 非布尔 => `TYPE_INVALID`；负 RPO/RTO => `NEGATIVE_VALUE`。
- `included=false` => `INCOMPLETE` + `DEPENDENCY_EXCLUDED`，且不把缺省字段报成 missing；`included=true` 缺字段 => `INCOMPLETE` + `DEPENDENCY_MISSING`。二者可区分。
- `observed<=approved` => `COMPLETE`/`MET`；任一项超目标 => `NOT_MET` + `TARGET_NOT_MET`；缺观测 => `INCOMPLETE` + `OBSERVATION_MISSING`。
- 六项全部 excluded/缺失 => `backupRecovery.closure` 值 `COPIES_ONLY` + `COPIES_ONLY_INCOMPLETE`。
- **Exact-set（F-S2-R1-01）**：六个固定 `dependencyId` 必须各出现且仅出现一次。数组非空时，缺失 ID 逐个输出 `backupRecovery.<dependencyId>`（`INCOMPLETE` + `DEPENDENCY_ABSENT`，`missing` 指向 `backupRecovery.BACKUP_DEPENDENCY.<dependencyId>`），因此子集即使全部 `MET` 也不能得到 `BACKUP_CLOSURE_COMPLETE`。重复 `dependencyId` 全部标记 `INVALID` + `DEPENDENCY_DUPLICATE`（同码 root error），并使用唯一回退 metric id `backupRecovery.item<index>`，输出 metric id 不重复。
- **公式归因（F-S2-R1-05）**：依赖 metric 的 `MET`/`NOT_MET` 比较判定 `formulaId="F-BACKUP-02"`；excluded/missing/absent/duplicate/typed-invalid 等完整性判定与 `backupRecovery.closure` 聚合保持 `F-BACKUP-01`。
- 所有依赖 metric 带 `EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY`；`owner`/`evidenceRef` 仅证明调用方证据完整性，不证明可恢复。
- `compute` 不读 wall clock；相同输入重复调用结果相同（validator 另做确定性检查）。

### 3.3 `F-CAP-01`

- `UNVERIFIED` => `NOT_VERIFIED`、`value="UNVERIFIED"`，不需要证据。
- `CLAIMED_VERIFIED` 字段齐全 => 仍 `NOT_VERIFIED`、`value="CLAIMED_VERIFIED"`；缺 `claimedBy/owner/source/evidenceRef/verifiedAt` 任一 => `INVALID` + `EVIDENCE_REQUIRED`。
- 未知 `status` => `INVALID` + `TYPE_INVALID`；`verifiedAt` 非法/未来 => `DATE_INVALID`/`DATE_IN_FUTURE`；无 `asOf` 不做未来比较并保留 `ASOF_ABSENT_NO_FUTURE_CHECK`。
- **Exact-set（F-S2-R1-02）**：固定 `CV-1..CV-5` + `regionAzSku` 必须各出现且仅出现一次。数组非空时，缺失项逐个输出 `capabilityEvidence.<capabilityId>`（`INCOMPLETE` + `CAPABILITY_ABSENT`），使组至少为 `INCOMPLETE`；重复 `capabilityId` 全部标记 `INVALID` + `CAPABILITY_DUPLICATE`（同码 root error），并使用唯一回退 metric id `capabilityEvidence.item<index>`。
- 组与 metric 始终输出 `DEPLOYMENT_EVIDENCE_INCOMPLETE`、`NO_CLOUD_VERIFICATION`、`A3_RISK_NOT_CLOSED`。
- root `cloudVerified=false`、`a3RiskClosed=false` 保持固定常量；顶层调用方 `productionReady`/`procurementReady`/`approved`/`cloudVerified`/`a3RiskClosed` 触发 `CALLER_CONTROL_FORBIDDEN`，能力断言不能改写常量。

### 3.4 验证器扩展

- `--stage s1`：仍检查 7 个 S1 组全部 `INCOMPLETE`、0 `INVALID`、0 error、四个 root 常量 false；S2 additive 两组存在时允许但不在 stage s1 计数。
- `--stage s2`：检查 9 组全部 `INCOMPLETE`、0 `INVALID`、空 `BACKUP_DEPENDENCY`/`CAPABILITY`、四个 root 常量 false；运行 s1+s2 fixtures（S1 回归）并做 README 精确单行检查。
- 非有限 probe：stage s2 在模板基础上注入合成 `asOf`、一条完整备份依赖和一条完整能力断言，对每个数值叶（含 `approvedRpoSeconds`/`approvedRtoSeconds`/`observedRpoSeconds`/`observedRtoSeconds`）注入真实 JS `NaN`/`+Infinity`/`-Infinity`，断言 `NUMBER_NONFINITE`。
- 确定性：模板 compute 两次 JSON 比较；fixture case 可标 `repeat:true` 做双跑比较。
- 重复 metric id 检测：每次 fixture 输出与模板 compute 都扫描所有组，若同一 metric id 出现多次则判失败（不再只看 `candidates[0]`）；`checkMetric` 支持断言 `formulaId`。
- README：只断言计划单行存在且 `18-production-capacity-and-deployment-spec.md` 链接行唯一，不改文档语义。

## 4. 决策 vs 待证据

**已定架构决策（引用 `docs/12` §4/§5，不重新选择）**：腾讯云中国大陆 + 私有 VPC；模块化单体；IVR 与 `ivr-gateway` 在运营商机房；业务服务不承载 RTP；COS 私有/SSE-KMS/versioning 为需求；PostgreSQL 主备 + PITR；播放走既有 bearer media-gateway；上传沿用 A3 CV-4 fail-closed。

**仍需证据（不在本片关闭）**：`CV-1..5`、region/AZ/SKU、storage class、价格/预算、网络路径、CDN/LB media-gateway 拓扑、数据库 HA/IOPS、每节点实测吞吐/连接/IO、SLO/RPO/RTO、媒体保留/legal hold、六类恢复演练观测与实际可恢复性。

## 5. 实际命令、exit code 与输出

### 5.1 已运行（静态/哈希，可复核）

| 命令 | exit code | 关键输出 |
| --- | --- | --- |
| `jq empty`（模板 + 3 个 S2 fixtures） | 0 | 无输出（严格 JSON 可解析） |
| `jq 'keys'` 模板 | 0 | `apiControlPlane, asOf, backupRecovery, capabilityEvidence, growthAndAsync, ivrLines, measuredNodes, mediaIngestStorage, meta, playback, spool`（9 组 + meta/asOf） |
| `jq '[.. \| objects \| select(has("value")) \| .value] \| map(select(. != null)) \| length'` 模板 | 0 | `0` |
| `jq ... provenance ... unique` 模板 | 0 | `["MISSING"]` |
| `jq ... approval ... unique` 模板 | 0 | `["MISSING"]` |
| `jq '.measuredNodes.MEASURED_ROLE \| length'` 模板 | 0 | `0` |
| `jq -s '[.[].stage] \| unique'`（3 个 S2 fixtures） | 0 | `["s2"]` |
| `jq -s '[.[].cases \| length]'`（3 个 S2 fixtures） | 0 | `[14, 9, 4]`（27 case：backup 14 + capability 9 + gates 4；数量仅为信息，不是成功判据） |
| README A4 链接静态检查（Grep） | — | 仅第 50 行命中 1 条 |
| `sha256sum` / `wc -l` / `git status --short` | 0 | 见 §2 与 §2 后状态 |
| `node --version` | 0 | `v22.23.4` |
| `jq empty`（S2 fixtures，review-fix 轮） | 0 | 无输出（严格 JSON 可解析） |
| `jq -r '.cases[].id'`（S2 fixtures） | 0 | 27 个 case id，含新增 subset/duplicate/absent 用例 |
| `git rev-parse HEAD` | 0 | `04c63de87996d14d0d3701b399edf46e2a921ed9` |
| `file docs/README.md` | 0 | `with CRLF line terminators` |
| `git diff --numstat -- docs/README.md`（未设置 autocrlf） | 0 | `151 150`（EOL 差异，非语义重写；见 §2 EOL 事实） |

### 5.2 未运行（权限阻断，不声称通过）

以下命令返回 `This command requires approval`，进程未启动，无 exit code：

```bash
node --check deployment/capacity/calc.mjs
node --check deployment/capacity/validate.mjs
node deployment/capacity/validate.mjs --stage s1
node deployment/capacity/validate.mjs --stage s2
node deployment/capacity/validate.mjs --template deployment/capacity/inputs.template.json --stage s1
node deployment/capacity/validate.mjs --template deployment/capacity/inputs.template.json --stage s2
node deployment/capacity/validate.mjs --stage s1 --fixtures deployment/capacity/fixtures
node deployment/capacity/validate.mjs --stage s2 --fixtures deployment/capacity/fixtures
```

因此以下行为断言**未验证**：S1 回归、9 组模板全 `INCOMPLETE`/0 `INVALID`、S2 fixture 的备份/能力/门禁行为、`NUMBER_NONFINITE` probe、重复运行一致性与 README 运行时检查。若任一失败，本 slice 必须回到 implementation correction，不得视为完成。

review-fix 轮（2026-10-26）单独执行 `node --check deployment/capacity/calc.mjs` 仍返回 `This command requires approval`（进程未启动、无 exit code）；同时包含 `node --check validate.mjs` / `--stage s1` / `--stage s2` 的组合命令被整体拒绝，validate/stage 命令未启动。本轮仅完成静态检查（`jq empty`、`jq` case 计数、`sha256sum`/`wc -l`、`git rev-parse`/`file`/`git diff --numstat`），**不声称**语法检查或任何行为断言通过。

## 6. 目标对齐

- 备份/恢复：`F-BACKUP-01/02` 覆盖六依赖 exact-set 闭包（缺失/重复可见且 fail closed）、target/observed 比较、零 RPO/RTO、`asOf`/日期、excluded vs missing、copies-only、证据完整性限制。
- 能力：`F-CAP-01` 覆盖固定能力 exact-set（缺失/重复可见）、caller 断言、`CLAIMED_VERIFIED` 仍未知、证据缺失 fail closed、root 常量不可改写。
- 部署门禁：无实测节点/存储/云证据时 `DEPLOYMENT_EVIDENCE_INCOMPLETE`，不产生 `FIT_EVIDENCE_COMPLETE`、SKU 建议或生产/采购批准。
- 可发现性：`docs/README.md` 单行入口；`docs/18` §7-§10 落地部署、发布/回滚门禁、恢复依赖与能力残留。
- 未新增 A0/A1/A2/A3 契约、endpoint、表、状态、角色或 SKU。

## 7. 残留下沉 owner/destination

与计划 §8 一致：线路/上传实测（Telephony/IVR）、SLO/峰值窗口（IVR/Backend/QA）、COS class/object-lock/保留（Infrastructure/Security）、审计/Outbox/queue/AI 实测（Operations/Backend）、DB 角色/降级拓扑（Platform/Database/Security）、授权窗口/时钟（IVR/Entitlement/Backend）、KMS/legal hold（Security/Infrastructure、Legal/Security）、`CV-1..5`/region/AZ/SKU 与 A3 风险 `R-A3-*`（Infrastructure/Security）、上传/证据 handoff（IVR/Evidence）。本片未关闭任何 A3 风险。

## 8. 诚实限制

未运行真实线路、SIP/CDR、IVR 媒体引擎、PostgreSQL/Redis/RabbitMQ、COS/KMS/Secret Manager/CAM、CDN/LB/media gateway、云审计或压测；未提供 benchmark、账单、region/SKU、容量或恢复实测。fixtures 数值全部为 `SYNTHETIC_TEST_ONLY`，只证明算术、维度、失败关闭与输出契约。`CLAIMED_VERIFIED` 不是云验证；恢复证据完整不等于可恢复；节点/存储证据缺失时不给正向 fit 或 SKU 建议。

## 9. 完成或阻塞声明

**Author-complete；运行时验证阻塞于环境权限。** 允许路径内产物已落地；是否完成需 controller 在可执行 node 的环境中运行 §5.2 命令并确认所有行为断言通过。本轮仅执行 controller 指定的有界 review-fix，未自行发起 review/re-review、未修改控制文档、未进入后续 Gate。

## 10. 操作声明

只修改 §2 允许路径；新增文件仅 `deployment/capacity/fixtures/s2-0*.json` 与本报告。未 stage、未 commit、未 push、未创建 PR、未 review、未云操作、未进入下一 Gate。

## 11. 评审修复记录（review-fix round）

Task：`a4-capacity-s2-review-fix-ds-20261026-01`，输入评审：`docs/reviews/a4-s2-implementation-review-mimo26flash-20261026.md`。

| 发现 | 级别 | 处理 |
| --- | --- | --- |
| F-S2-R1-01 | P1 | `computeBackupRecovery` 实施六依赖 exact-set：缺失 ID 逐个输出 `INCOMPLETE` + `DEPENDENCY_ABSENT`，子集即使全 `MET` 也不能 `BACKUP_CLOSURE_COMPLETE`；重复 `dependencyId` 标记 `INVALID` + `DEPENDENCY_DUPLICATE` 并产生同码 root error；重复项使用唯一回退 id `backupRecovery.item<index>`。新增 fixtures `subset-complete-is-not-closure`、`duplicate-dependency-id-invalid`、`nonempty-none-of-fixed-ids-present`。 |
| F-S2-R1-02 | P2 | `computeCapabilityEvidence` 实施固定 `CV-1..CV-5` + `regionAzSku` exact-set：缺失项 `INCOMPLETE` + `CAPABILITY_ABSENT`；重复 `capabilityId` 标记 `INVALID` + `CAPABILITY_DUPLICATE` 并使用唯一回退 id `capabilityEvidence.item<index>`。新增 fixtures `missing-fixed-capabilities-visible`、`duplicate-capability-id-invalid`，并把 s2-02/s2-03 既有单能力子集用例补齐为固定六项。 |
| F-S2-R1-03 | P3 | 未改写 README；按 §2 EOL 事实区分 Git 语义 diff（1 insertion / 0 deletions，`core.autocrlf=true` 口径）与 CRLF 工作区原始 diff（本会话未设置 autocrlf 时显示 151/150）。 |
| F-S2-R1-04 | P3 | 报告 §0 base hash 更正为 `04c63de87996d14d0d3701b399edf46e2a921ed9`（`git rev-parse HEAD`）。 |
| F-S2-R1-05 | P3 | 依赖 metric 的 `MET`/`NOT_MET` 比较判定归 `F-BACKUP-02`，完整性/excluded/missing/absent/duplicate/typed-invalid 与 closure 聚合归 `F-BACKUP-01`；fixtures 增加 `formulaId` 断言。 |

其他变更：

- `validate.mjs` 新增输出 metric id 唯一性扫描（fixture 与模板 compute），并在期望检查中对同一 id 的多个候选报错；`checkMetric` 支持 `formulaId` 断言，避免重复 ID 被 `candidates[0]` 掩盖。
- `docs/18` §9.1/§9.2/§10.1 增补 exact-set、`DEPENDENCY_ABSENT`/`DEPENDENCY_DUPLICATE`、`CAPABILITY_ABSENT`/`CAPABILITY_DUPLICATE` 与公式归因说明。
- 未修改模板、README、control doc、S1 fixtures、A0–A3、原型或截图；root 常量与 S1 行为未改。
- 本轮尝试的 `node --check` 与 `--stage s1`/`--stage s2` 仍被权限阻断，未获得 exit code；静态检查（`jq empty`、case 计数、哈希/行数）通过。
