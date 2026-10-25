# A4 生产容量与部署规格实施计划（bounded plan fix）

> Gate: `plan fix` ONLY
> Task: `a4-capacity-planfix-ds-20261026-02` / provider `ds` / model `deepseek-flash`
> Base: `f3bcbd99a6f49cb64977358cea08e028bc634a54`；branch `codex/a4-production-capacity-deployment-spec`
> Upstream: `docs/12` §4/§5/§13/§17/§18.5；`docs/13`；`docs/14` §4/§5/§9/§10/§12/§14；`docs/15` + `api/openapi`；`docs/16` §6-§7/§11；`docs/17` §9/§16/§20。
> 本 fix 2 只允许修改本文件并新增 `docs/gateflow/a4-production-capacity-deployment-spec-plan-fix-2.md`。controller、既有 review、`docs/12`-`docs/17`、A0-A3 契约、prototypes/screenshots、secrets/provider 文件全部只读。

## 0. 结论摘要

A4 交付**参数化、可复核、诚实不完整**的容量与部署规格，而不是生产规模结论。唯一人类规格为 `docs/18-production-capacity-and-deployment-spec.md`（中文），唯一计算辅助为 `deployment/capacity/` 下零三方依赖 Node 22 ESM 工具。所有生产数值、云能力、region/AZ/SKU、价格、SLO/RPO/RTO、保留期与测量值保持 typed input，未获批准即输出 `INCOMPLETE`/`NOT_VERIFIED`/`UNKNOWN`，绝不填 0 或合成值。

两个行为切片：**S1 容量算术与未知传播**、**S2 部署/恢复/能力证据与可发现性**。S1 模板/验证覆盖其 7 个组，并**在本片测试其实现的每个公式**（含 API 与节点）；S2 追加 2 个组（`backupRecovery`、`capabilityEvidence`），并**明确授权对 `inputs.template.json` 做 additive 编辑**（S2 模板 9 组）以及必要的 stage 验证扩展。切片成功以**行为断言**为准，不以 fixture 数量或文档段落计数为准；文件/case 可紧凑分组。

本 fix 2 在 fix 1 已关闭 F-01..F-09 的基础上，关闭 fresh re-review 的 N-01..N-05：spool 单一中断开始快照与 end-of-outage backlog、Unit 枚举补全与具名 scalarKind 范围/整型契约、AI load ratio 与 Little 平均在服任务分离、上传 catch-up payload/wire 基准、以及 `cloudVerified`/`a3RiskClosed` 的固定 DTO 归属；同时保持逐叶输入结构、partial 指标、Little 定律边界、条件式 CDN 算术、上传/编码分离、AI/队列/数据库开销独立测量与陈旧 ID/停止条件清理不回归。

## 1. 目标、成功信号、非目标、边界与无漂移

- **目标**：给出完整公式、typed input、单位、来源/批准状态、中间值、缺失前置、限制和 unknown 传播；覆盖 IVR 完整桥接通话 vs A/B 腿、控制面 API、录音上传/存储/播放带宽、加密本地 spool、媒体版本/派生/元数据/审计/队列增长、实测节点容量与 failover、备份恢复依赖。
- **成功信号**：每个输出可回答“用了哪个单位、哪个公式、哪些输入、谁批准、缺什么、为什么不能采购”；缺生产输入时明确 `INCOMPLETE`；合成样例只证明算术并由 `SYNTHETIC_TEST_ONLY` 标记；部署章节区分**已定架构决策**与**仍须证据的能力**；`productionReady`/`procurementReady`/`cloudVerified`/`a3RiskClosed` 恒为 `false`。
- **非目标**：云采购/开通、真实压测、生产 Java/IVR/UI、迁移、公共契约变更、保留期/法务政策、并发/区域/SKU/价格/容量发明、Kubernetes/微服务化、把最小拓扑当已验证容量。
- **边界**：只新增一份规格、一个计算器、一个验证器、一个输入模板、聚焦 fixtures；`docs/README.md` 仅一行可发现性入口（当前无 A4 变更）。A0/A1/A2/A3 契约只读；不复制其 registry/catalog；不新增表、endpoint、Problem code、协议事件或安全策略。
- **无漂移检查**：不把“参数化规格”扩大为“生产容量批准”；不把合成算术转为生产值；不因补全公式而重定线路、媒体网关、COS 或法律策略；所有未决值仍由原 owner/destination 承接（§8）。上传仍受 A3 CV-4 fail-closed 约束；播放仍走 A3 bearer/generation 校验；CDN 缓存只做条件算术。

## 2. 第一性原理与精确上游证据

1. **事实所有权决定计量边界**：IVR 是 A/B 路、桥接、振铃、录音、限时和权威通话时间的事实源（`docs/12` §9.1-§9.2、`docs/13`、`docs/16` §6）；ONECALL 拥有任务、授权、权益、证据状态。容量计算只能消费这些事实的测量值。
2. **完整通话不是两个永久占用的腿**：一次桥接业务通话有 A 呼入腿和 B 外呼腿；A 在 B 振铃期间仍占用，B 的振铃/未接/忙线也占用线路（`docs/13` A/B 路、`docs/16` §6.5/§6.8）。线路/端口需求按各自到达率×平均占用时长计算；不得对“N 个完整通话”统一乘 2。
3. **平均值不是峰值**：`rate × meanDuration` 只在同一测量窗口/工况下给出**稳态平均占用**（Little 定律），不是保证峰值，也不是 trunk admission 安全证明。即使输入被标为 peak rate，平均占用也不能反推并发峰值；独立观测的 A/B/shared/bridge 峰值只能按其声明窗口与 scope 并列报告，不能从平均时长“拟合”峰值。
4. **控制面与媒体面分离**：业务服务不承载 RTP；播放走 A3 冻结的 `Authorization: Bearer` media-gateway 数据面，位于既有 core-api/edge 信任边界，不返回永久 COS URL，且不新增 A1 operation（`docs/17` §16、`docs/15` §3.3 `authorizePublicShareMedia`）。链路上行/资源成本必须单独计量；逻辑 media-gateway 容量与 core-api/edge 共享，不得作为独立部署再求和。
5. **存储按事实分类**：原始录音、派生对象（preview/transcript/summary/export）、不可变历史版本、元数据、审计/Outbox、备份分别是输入；录音位率/声道/格式和**服务端校验时长**决定字节，不用声明值或客户计费逻辑字节乘内部副本数（A0 §4.39-§4.43、`docs/16` §7）。legal hold 只计**普通保留/版本总量之外的额外字节**；备份副本是独立策略，不乘 COS 内部副本。
6. **上传不是编码**：录音编码位率（total all-channel）描述写入速度，不等于文件上传 wire 速率；快于实时的文件上传不能反推 codec bitrate。上传需要独立的并发上传数×每流 wire 速率或显式实测峰值；日均 `bytes×8/86400` 只是**日均 payload 下界**，不是峰值采购依据。ongoing 上传、spool 积压追赶和编码速率必须分开报告。
7. **恢复是依赖闭包**：PostgreSQL PITR、被引用 COS 版本（含不可变历史与 legal hold）、KMS/Secret Manager key version、Outbox/对账事实、IVR 本地事实缺一不可；副本存在不等于可恢复（`docs/12` §13、A3 `R-A3-KEY-DESTRUCTION`）。恢复目标（approved RPO/RTO）与演练观测（observed RPO/RTO）是不同字段；零值是合法目标；缺演练是 `INCOMPLETE`，观测超目标是 `NOT_MET`，不是健康。
8. **容量必须测量且按拓扑分类**：只有**显式测量为同质无状态池**的 core-api/async-worker/media-ai-worker 才可做线性每节点乘法；DB/cache/queue 使用实测正常/降级拓扑聚合容量，不做 active-active 节点算术；media-gateway 逻辑容量共享在 core-api/edge 内。CPU/RAM/连接/IO 证据缺失时容量只能是 `NOT_VERIFIED`，绝不输出正向 deployment-fit；原始算术估计可报告但必须带 `ESTIMATE_ONLY` 限制，且不给 SKU 建议。
9. **队列/AI/数据库是独立测量面**：AI 请求量按实测 task arrivals、**聚合池** service rate、mean service time、bytes/task 计算；`loadRatio` 与 Little 平均在服任务数分开，不用“录音秒数×bitrate”捷径；队列按 arrivals/service rate/bytes 计算 backlog 增长或排空；数据库行 payload 之外还必须显式列出 index/WAL/replication/backup 独立输入与限制，不能声称仅凭行字节完成数据库 sizing。
10. **既有默认不扩张**：30 秒只是 `PENDING` 任务可领取的冻结产品默认（`docs/13` §4.2、Q18），不是请求超时、授权窗口或 spool 保留期；Q25 授权窗口另有 owner。

## 3. 产物布局、canonical 结构与单一真相

```
docs/18-production-capacity-and-deployment-spec.md   # 唯一人类规格（中文）
deployment/capacity/
  calc.mjs                 # 纯函数 compute(input) + CLI；零依赖 Node 22 ESM
  validate.mjs             # 模板/stage/fixtures 行为断言；不把 expected 传给计算器
  inputs.template.json     # S1 7 组全 null typed 模板；S2 additive 扩为 9 组
  fixtures/s1-*.json       # S1 行为样例（文件/case 紧凑分组，数量不构成成功条件）
  fixtures/s2-*.json       # S2 部署/恢复/能力样例
docs/README.md             # S2 仅一行发现入口（当前无 A4 变更）
```

- 本文档是容量/部署语义唯一来源；计算器只实现本文列出的有限具名公式，不是通用公式/策略引擎。
- **模板演进（F-01）**：S1 模板严格 7 组：`ivrLines`、`apiControlPlane`、`measuredNodes`、`playback`、`mediaIngestStorage`、`growthAndAsync`、`spool`。S2 **明确授权**对 `inputs.template.json` 做 additive 编辑，新增 `backupRecovery`、`capabilityEvidence` 两组；不得改变 S1 组结构、字段或既有 fixture 语义。S1 退出只断言 7 组；9 组模板断言属于 S2。**文件边界**：S1 的 `docs/18` §1-§6 只落 7 个 S1 组的输入/公式；§4.2 的 Backup/capability 两行与 F-BACKUP-*/F-CAP-* 只在 S2 的 §9/§10 落文，S1 不写、不实现。
- 不建立跨文件 marker/registry 快照；验证器断言**计算结果与失败模式**，不统计文档段落或复制 A0/A1/A2/A3 清单。
- 引用上游时只写章节/ID（如 `docs/17` §16、`R-A3-COS-CAPABILITY`），不镜像其完整表格。
- `docs/18` 建议结构：1 状态与非声明；2 单位/输入来源/批准；3 IVR 腿与线路；4 API/播放/媒体网关；5 录音/上传/存储/元数据/队列/AI；6 spool/实测节点/failover；7 部署放置与边界；8 发布/回滚/扩缩容门禁；9 备份/PITR/恢复依赖；10 能力清单与残留。S1 写 §1-§6；S2 追加 §7-§10。
- `docs/README.md` 的 S2 变更仅新增一行：`| [18-production-capacity-and-deployment-spec.md](18-production-capacity-and-deployment-spec.md) | 参数化容量、部署、恢复与证据清单 | 架构、运维、安全 |`。

## 4. 容量模型：typed input、公式、单位与未知传播

### 4.1 逐叶输入契约、集合、批准与输出 DTO

**ScalarInput<T>**（每个数值/枚举叶都必须是此形状，不得裸值）：

```text
{ value: T | null, unit: Unit, provenance: Provenance, source: string|null,
  owner: string|null, approval: Approval }
Provenance = MISSING | SYNTHETIC_TEST_ONLY | MEASURED | ESTIMATE | APPROVED_POLICY
Approval   = MISSING | UNAPPROVED | APPROVED | SYNTHETIC_TEST_ONLY
Unit       = callsPerSecond | seconds | channels | bridgePorts | ratio | count |
             countPerDay | requestsPerSecond | requests | transactionsPerSecond |
             opsPerSecond | messagesPerSecond | connections | sessions | bitsPerSecond |
             bytes | bytesPerSecond | bytesPerDay | tasksPerSecond | tasks | transactions |
             bytesPerTask | nodes | days | hertz | bit | boolean | enum | iso8601
```

输入表中的 `calls/s`、`requests/s`、`transactions/s`、`ops/s`、`messages/s`、`tasks/s`、`bytes/s`、`count/day`、`Hz`、`bit` 等是上述 Unit 枚举的显示别名（`Hz`→`hertz`），计算器使用枚举值；`MEASURED_ROLE[].demandUnit` 必须取 `requestsPerSecond`/`tasksPerSecond`/`transactionsPerSecond`/`opsPerSecond`/`messagesPerSecond`/`sessions` 之一。

- `value:null` 或 `provenance:MISSING` => 该叶 missing；`source`/`owner`/`approval` 缺失不得被当作批准。
- `provenance:MEASURED` 要求非空 `source` 与非空 `owner`；`approval` 可为 `UNAPPROVED`。
- `provenance:APPROVED_POLICY` 要求非空 `source`（决策/政策 ID）、非空 `owner` 且 `approval:APPROVED`。
- `provenance:ESTIMATE` 是调用方算术估计，永远不得 `approval:APPROVED`（否则 `APPROVAL_INVALID`）；可参与算术，但相关指标 `approvalStatus` 至多 `UNAPPROVED`，节点/线路裁决不得为正向证据完整。
- `approval:APPROVED` 还要求 `provenance ∈ {MEASURED, APPROVED_POLICY}`、非空 `source` 与非空 `owner`；否则 `APPROVAL_INVALID`。
- `provenance:SYNTHETIC_TEST_ONLY` 要求 `approval:SYNTHETIC_TEST_ONLY`；输出 `synthetic:true` 与 `NOT_APPROVED_PRODUCTION_VALUE`。
- 显式 `0` 在 `source`/`owner`/`unit` 齐全时是合法值，不等于 missing；负值（除明确允许的差值中间量外）为 `NEGATIVE_VALUE`。
- **root 不能代替叶批准**：`meta.status=APPROVED_SOURCE_INPUTS` 只表示 meta 来源/批准记录完整，绝不把未批准或不相关的叶升级为 `APPROVED`；每个指标只根据其实际使用的叶计算 `approvalStatus`。
- **合成算术**：允许在 `SYNTHETIC_TEST_ONLY` 下完整计算；未批准的**真实**输入即使算术完整，指标仍为 `UNAPPROVED`。

**具名标量范围/整型契约（N-02；校验按 `scalarKind`，不使用全局 `ratio 0..1`）**：

| scalarKind | 范围 | 整型 | 典型字段 |
| --- | --- | --- | --- |
| `Ratio01` | `0 <= x <= 1` | 否 | `IVR_FAILOVER_HEADROOM_RATIO`、`SPOOL_HEADROOM_RATIO`、节点 `headroomRatio`、`PB_CDN_CACHE_HIT_RATIO`、`measuredCpuUtilization`/`measuredRamUtilization`/`measuredConnectionsUtilization`/`measuredIopsUtilization` |
| `WireOverheadRatio` | `0 <= x < 1` | 否 | `UPLOAD_WIRE_OVERHEAD_RATIO` |
| `UtilizationRatio` | `x >= 0` | 否 | 派生 `F-IVR-05` utilization；>1 合法并驱动 `NOT_MET`，绝不判 `RATIO_OUT_OF_RANGE` |
| `VersionFactor` | `x >= 1` | 否 | `REC_ORIGINAL_VERSION_FACTOR`、`REC_DERIVED_BY_TYPE.*.versionFactor`（1.2 合法） |
| `CopyFactor` | `x >= 0` | 否 | `REC_BACKUP_COPY_FACTOR`（0 合法，表示无备份副本） |
| `DerivedByteRatio` | `x >= 0` | 否 | `REC_DERIVED_BY_TYPE.*.bytesPerOriginalByte` |
| `NonNegativeRate` | `x >= 0` | 否 | 所有 `callsPerSecond`/`requestsPerSecond`/`transactionsPerSecond`/`opsPerSecond`/`messagesPerSecond`/`tasksPerSecond`/`bytesPerSecond`/`bitsPerSecond`/`hertz` 输入 |
| `NonNegativeMeasure` | `x >= 0` | 否 | 所有派生字节/带宽/平均占用/平均在服量（`channels`、`requests`、`tasks`、`bytes`、`bitsPerSecond` 等）；允许分数 |
| `DurationSeconds` | `x >= 0`；`SPOOL_OUTAGE_S` 要求 `> 0` | 否 | 所有 `seconds` 输入 |
| `DiscreteCount` | 整数 `>= 0` | 是 | `IVR_IN_LIMIT_CH`/`IVR_OUT_LIMIT_CH`/`IVR_SHARED_LIMIT_CH`、`REC_CHANNELS`、`REC_BIT_DEPTH`、`REC_PER_DAY`、`UPLOAD_CONCURRENT_PEAK`、`PB_CONCURRENT_SESSIONS`、`PB_SESSIONS_PER_DAY`、`QUEUE_INITIAL_BACKLOG_TASKS`、`deployedNodes`、`IVR_OBSERVED_PEAK.*.count` |
| `DailyVolume` | `x >= 0` | 否（测量日均；整数日量也合法） | `META_BY_TYPE.*.rowsPerDay` |

- `COUNT_NOT_INTEGER` 只适用于 `DiscreteCount` 字段；`channels`/`requests`/`tasks` 等派生平均允许非负实数（如 `0.5 calls/s × 7.4 s = 3.7 channels` 合法）。
- `RATIO_OUT_OF_RANGE` 适用于 `Ratio01`/`WireOverheadRatio` 越界及 `VersionFactor`/`CopyFactor`/`DerivedByteRatio` 低于下界；负值一律 `NEGATIVE_VALUE`。utilization **输入** >1 属 `Ratio01` 越界；**派生** utilization >1 属合法 `UtilizationRatio` 并输出 `NOT_MET`。
- 字节基准必须显式：录音/播放/spool 的 payload 字节与上行 wire 字节不得混标；只有显式 overhead 或显式 wire 输入才可换算/标注 wire（见 F-UP-*、F-SPOOL-*）。

**集合/聚合结构（固定键，未知键即 `UNKNOWN_KEY`）**：

- `IVR_MIX_RATE_PS`: `{task, noTaskDtmf, activation, rejected, failed}`，全部 ScalarInput(calls/s)。
- `API_RATE_PS`: `{customer, operations, ivrControl, publicShare, providerCallback}`，全部 ScalarInput(requests/s)。
- `IVR_OBSERVED_PEAK`: 可选 `{inbound?, outbound?, shared?, bridge?}`；每项 `{count: ScalarInput(count), windowSeconds: ScalarInput(seconds), scope: string}`；无有效 `windowSeconds`/`scope` 的峰值不得用于裁决。
- `MEASURED_ROLE[]`: 每项字段由固定 role 决定（§4.2）；未知 role => `UNKNOWN_ROLE`；给 role 不适用字段 => `ROLE_FIELD_MISMATCH`。
- `REC_DERIVED_BY_TYPE`: 固定 `{preview, transcript, summary, export}`；每项 `{state: PRODUCED|DISABLED, bytesPerOriginalByte: ScalarInput(DerivedByteRatio), retentionDays: ScalarInput(days), versionFactor: ScalarInput(VersionFactor)}`。`PRODUCED` 要求三项齐全；`DISABLED` 要求 `bytesPerOriginalByte` 是带 provenance 的显式 0，`retentionDays`/`versionFactor` 不参与计算、可为 MISSING。
- `META_BY_TYPE`: 固定 `{audit, outbox, callEvent, evidenceEvent, shareAccess}`；每项 `{rowsPerDay, bytesPerRow, retentionDays}` ScalarInput。
- `DB_AUX_BY_KIND`: 固定 `{index, wal, replication, backup}`，每项 ScalarInput(bytes)，独立测量。
- `BACKUP_DEPENDENCY[]`: 固定六个 `dependencyId`：`postgresPitr`、`cosVersions`、`kmsKeyVersions`、`secretVersions`、`reconciliationOutbox`、`ivrLocalFacts`。
- `CAPABILITY[]`: 固定 `CV-1`..`CV-5` + `regionAzSku`。
- 顶层 `asOf`: ScalarInput(iso8601)；`backupRecovery` 组必需，其他组仅在需要未来日期比较时使用。`compute` 不读 wall clock。

**输出 DTO（固定字段名）**：

```text
{
  schemaVersion: 1,
  status: COMPLETE|NOT_VERIFIED|INCOMPLETE|NOT_MET|INVALID,
  productionReady: false, procurementReady: false,
  cloudVerified: false, a3RiskClosed: false, synthetic: boolean,
  metaStatus: UNAPPROVED_TEMPLATE|SYNTHETIC_TEST_ONLY|APPROVED_SOURCE_INPUTS,
  asOf: string|null,
  groups: [{
    id, status, partial: boolean, formulaIds: string[], unit: string|null,
    metrics: [{
      id, formulaId, status, unit, value: number|boolean|string|null,
      missing: string[], invalid: string[], reasons: string[],
      limitations: string[], approvalStatus: SYNTHETIC_TEST_ONLY|APPROVED|UNAPPROVED|MISSING
    }],
    missing: string[], invalid: string[], limitations: string[]
  }],
  flags: string[],
  errors: [{ code: string, path: string }]
}
```

**状态严重度**：`INVALID(4) > NOT_MET(3) > INCOMPLETE(2) > NOT_VERIFIED(1) > COMPLETE(0)`。metric 状态取自身最坏；group 状态取 metric 最坏；root 状态取 group 最坏。`partial:true` 当同组至少一个 metric `COMPLETE` 且至少一个非 `COMPLETE`；缺失 metric 不得填 0。`flags` 为所有 limitation/门禁标记的去重并集。`metric.status` 只取五值；公式表中的 `LINE_FIT_EVIDENCE_COMPLETE`/`FIT_EVIDENCE_COMPLETE`/`ARITHMETIC_ONLY`/`MET`/`NOT_MET`/`OBSERVATION_MISSING` 是 `metric.value` 的固定 verdict 字符串。映射：`MET`、`LINE_FIT_EVIDENCE_COMPLETE`、`FIT_EVIDENCE_COMPLETE` => `COMPLETE`；`ARITHMETIC_ONLY`、`CLAIMED_VERIFIED` => `NOT_VERIFIED`；`NOT_MET` => `NOT_MET`；`OBSERVATION_MISSING` => `INCOMPLETE`。**语义结果 code → metric.status**：`DRAIN_IMPOSSIBLE`、`BACKLOG_NOT_DRAINING`、`BACKLOG_GROWING`、`N_MINUS_ONE_IMPOSSIBLE` 固定为 `NOT_MET`；`DEPLOYED_BELOW_REQUIRED`、`TARGET_NOT_MET`、`RESOURCE_HEADROOM_INSUFFICIENT` 同属 `NOT_MET`；`OBSERVATION_MISSING`、`PEAK_INPUTS_MISSING`、`PEAK_EVIDENCE_MISSING`、`MEASUREMENT_REQUIRED`、`RESOURCE_EVIDENCE_MISSING`、`EVIDENCE_REQUIRED`、`ASOF_REQUIRED`、`DEPENDENCY_MISSING`、`DEPENDENCY_EXCLUDED`、`COPIES_ONLY_INCOMPLETE` 按各公式为 `INCOMPLETE` 或 `NOT_VERIFIED`。显式零因子短路的 metric 为 `COMPLETE`（value 0）并带 `ZERO_FACTOR_SHORT_CIRCUIT`，缺失的未使用因子仍进 `missing[]`；malformed 叶永远不会被短路成 COMPLETE。**集合项逐项输出 metric**（id 如 `measuredNodes.core-api`、`mediaIngestStorage.derived.preview`、`backupRecovery.postgresPitr`、`capabilityEvidence.CV-1`），不得只给组总分；组 partial/最坏状态由项 metric 汇总。`cloudVerified` 与 `a3RiskClosed` 是 root 固定常量 `false`，任何调用方字段/能力断言都不得改变（N-05）；F-CAP-01 与 `s2-02`/`s2-03` 直接断言这两个 root 字段。

**稳定 error/reason code**（不得临时发明同义词）：`TYPE_INVALID`、`UNIT_MISMATCH`、`NUMBER_NONFINITE`、`NEGATIVE_VALUE`、`COUNT_NOT_INTEGER`、`RATIO_OUT_OF_RANGE`、`DIVISION_BY_ZERO`、`OVERFLOW_UNSAFE_INTEGER`、`MODE_LIMIT_MISMATCH`、`RING_EXCEEDS_OCCUPANCY`、`MIX_SUM_MISMATCH`、`UNKNOWN_KEY`、`UNKNOWN_ROLE`、`ROLE_FIELD_MISMATCH`、`SHARED_CAPACITY_NODE_ARITHMETIC_FORBIDDEN`、`PROVENANCE_INVALID`、`APPROVAL_INVALID`、`CALLER_CONTROL_FORBIDDEN`、`EVIDENCE_REQUIRED`、`DATE_INVALID`、`DATE_IN_FUTURE`、`ASOF_REQUIRED`、`DEPENDENCY_EXCLUDED`、`DEPENDENCY_MISSING`、`OBSERVATION_MISSING`、`TARGET_NOT_MET`、`DRAIN_IMPOSSIBLE`、`BACKLOG_NOT_DRAINING`、`BACKLOG_GROWING`、`CAPACITY_INCONSISTENT`、`N_MINUS_ONE_IMPOSSIBLE`、`DEPLOYED_BELOW_REQUIRED`、`RESOURCE_EVIDENCE_MISSING`、`RESOURCE_HEADROOM_INSUFFICIENT`、`MEASUREMENT_REQUIRED`、`ESTIMATE_ONLY`、`IDLE_ZERO_DENOMINATOR`、`COPIES_ONLY_INCOMPLETE`、`PEAK_EVIDENCE_MISSING`、`PEAK_INPUTS_MISSING`、`PEAK_AVG_WINDOW_UNQUALIFIED`、`ZERO_DEMAND_NOT_A_FIT`、`ROLE_NOT_DECLARED`。

**稳定 limitation/flag**（与 error/reason code 分开；可重复、去重）：`NOT_APPROVED_PRODUCTION_VALUE`、`MEASUREMENT_REQUIRED`、`ESTIMATE_ONLY`、`ZERO_DEMAND_NOT_A_FIT`、`AVERAGE_NOT_PEAK`、`LOAD_RATIO_NOT_OCCUPANCY`、`LINE_RECOMMENDATION_UNVERIFIED`、`PEAK_EVIDENCE_MISSING`、`PEAK_INPUTS_MISSING`、`PEAK_AVG_WINDOW_UNQUALIFIED`、`ZERO_FACTOR_SHORT_CIRCUIT`、`ROLE_NOT_DECLARED`、`TXN_PER_REQUEST_NOT_MEASURED`、`CONDITIONAL_CACHE_SCENARIO`、`NO_CDN_TOPOLOGY_APPROVAL`、`BEARER_CHECKS_UNCHANGED`、`SHARED_MEDIA_GATEWAY`、`DB_SIZING_INCOMPLETE`、`EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY`、`NO_CLOUD_VERIFICATION`、`A3_RISK_NOT_CLOSED`、`DEPLOYMENT_EVIDENCE_INCOMPLETE`、`ASOF_ABSENT_NO_FUTURE_CHECK`。

### 4.2 Input catalog

**IVR / API**

| Input ID | Unit | 说明 |
| --- | --- | --- |
| `IVR_IN_RATE_PS` | calls/s | 峰值 A 路呼入尝试，含 failed/无任务/DTMF/开通/试用流量 |
| `IVR_IN_HOLD_S` | s | A 路平均总占用（菜单/DTMF/振铃/桥接），实测 |
| `IVR_OUT_RATE_PS` / `IVR_OUT_OCC_S` | calls/s / s | B 路尝试率与平均占用（含振铃） |
| `IVR_OUT_RING_S` | s | B 路振铃/应答前部分，必须 `<= IVR_OUT_OCC_S` |
| `IVR_BRIDGE_RATE_PS` / `IVR_BRIDGE_MEAN_S` | calls/s / s | 峰值桥接通话率与平均桥接时长 |
| `IVR_TRUNK_MODE` | enum | `SEPARATE_INBOUND_OUTBOUND` 或 `SHARED` |
| `IVR_IN_LIMIT_CH` / `IVR_OUT_LIMIT_CH` / `IVR_SHARED_LIMIT_CH` | `DiscreteCount` | 运营商线路/端口上限；按模式只读相关项，其他可缺 |
| `IVR_FAILOVER_HEADROOM_RATIO` | `Ratio01` | 线路 failover/余量，无默认 |
| `IVR_MIX_RATE_PS` | calls/s | 五类呼入决策流量；和必须等于 `IVR_IN_RATE_PS` |
| `IVR_OBSERVED_PEAK` | `DiscreteCount` + window/scope | 独立观测 A/B/shared/bridge 同时峰值；只旁证/裁决线路证据，不参与平均公式 |
| `API_RATE_PS` | requests/s | 五类控制面请求；不得由呼叫并发推导 |
| `API_MEAN_SERVICE_S` / `API_DB_TXN_MEAN_S` | s | 实测请求与数据库事务平均时长 |

**Measured nodes（`MEASURED_ROLE[]`）**

| Role | `demandUnit` | 允许的容量字段 | 禁止字段 |
| --- | --- | --- | --- |
| `core-api` | requests/s | `perNodeRate`、`deployedNodes` | topology/shared |
| `async-worker` | tasks/s | `perNodeRate`、`deployedNodes` | topology/shared |
| `media-ai-worker` | tasks/s | `perNodeRate`、`deployedNodes` | topology/shared |
| `postgres` | transactions/s | `measuredNormalTopologyCapacity`、`measuredDegradedTopologyCapacity` | perNodeRate/deployedNodes |
| `redis` | ops/s | 同上 | 同上 |
| `rabbitmq` | messages/s | 同上 | 同上 |
| `media-gateway` | sessions | `sharedCoreApiEdgeCapacity` | perNodeRate/deployedNodes |

`demandUnit` 必须取 Unit 枚举值：`core-api`=`requestsPerSecond`，`async-worker`/`media-ai-worker`=`tasksPerSecond`，`postgres`=`transactionsPerSecond`，`redis`=`opsPerSecond`，`rabbitmq`=`messagesPerSecond`，`media-gateway`=`sessions`。每个 role 另有 `demandQuantity`（`NonNegativeMeasure`，可为分数）、`headroomRatio`、`measuredCpuUtilization`、`measuredRamUtilization`、`measuredConnectionsUtilization`、`measuredIopsUtilization`（均 `Ratio01`）、`resourceEvidenceRef`、`measuredAt`（ISO 日期，非空）；`deployedNodes` 为 `DiscreteCount`。`media-gateway` 的 demand 来自 `PB_CONCURRENT_SESSIONS`，其 role entry 不单独提供 `demandQuantity`；若提供则必须同值且单位 sessions，否则 `ROLE_FIELD_MISMATCH`。role 分类由本表固定，调用方不能自选“无状态”分类。`measuredNodes` 组要 `COMPLETE`，至少需有 `core-api`、`async-worker`、`postgres`、`redis`、`rabbitmq`、`media-gateway` 且每个 `roleVerdict` 的 metric.status 为 `COMPLETE`（值 `FIT_EVIDENCE_COMPLETE`）；`media-ai-worker` 在存在 AI 任务输入时必需，否则缺省只标 `ROLE_NOT_DECLARED`；缺任一必需 role => 该组 `INCOMPLETE`。

**Playback**

| Input ID | Unit | 说明 |
| --- | --- | --- |
| `PB_CONCURRENT_SESSIONS` | `DiscreteCount` | 峰值并发播放 |
| `PB_BITRATE_BPS` | bits/s | 实测派生对象播放位率 |
| `PB_SESSIONS_PER_DAY` / `PB_MEAN_SECONDS` | `DiscreteCount` / s | 每日播放会话（整数日量）与平均秒数 |
| `PB_CDN_CACHE_HIT_RATIO` | `Ratio01` | 仅条件式 origin 场景；缺失则 origin 未知，不假设 CDN 拓扑 |

**Recording / upload / storage**

| Input ID | Unit | 说明 |
| --- | --- | --- |
| `REC_BITRATE_BPS` | bits/s | 实测**全声道总**编码位率，优先 |
| `REC_CODEC` / `REC_SAMPLE_RATE_HZ` / `REC_BIT_DEPTH` / `REC_CHANNELS` | enum / `hertz` / `bit` / `DiscreteCount` | 仅 `PCM_LINEAR` 且无实测位率时用 CBR 公式；其他 codec 无位率则未知 |
| `REC_MEAN_VALIDATED_S` | s | 服务端校验时长；只有声明时长时该组 `INCOMPLETE`，不回退 |
| `REC_PER_DAY` | `DiscreteCount` | 实际产生并校验的录音数 |
| `UPLOAD_CONCURRENT_PEAK` | `DiscreteCount` | 实测同时上传数 |
| `UPLOAD_PER_STREAM_WIRE_BPS` | bits/s | 实测每活跃上传 wire 速率（不是 codec bitrate） |
| `UPLOAD_MEASURED_PEAK_WIRE_BPS` | bits/s | 可替代并发×每流的显式实测峰值 |
| `UPLOAD_WIRE_OVERHEAD_RATIO` | `WireOverheadRatio` | wire→payload 的显式开销假设（`0<=x<1`），用于标注估计；缺失时 catch-up wire 不得猜 0 |
| `UPLOAD_COMBINED_MEASURED_WIRE_BPS` | bits/s | 仅在调用方实测 ongoing+追赶合计时提供；否则不自动求和 |
| `REC_ORIGINAL_VERSION_FACTOR` | `VersionFactor` | 不可变历史版本系数（`>=1`，1.2 合法）；无默认 |
| `REC_RETENTION_DAYS` | days | Q3/Q11 未批准则为 null，不能默认 |
| `REC_DERIVED_BY_TYPE` | map | 四类派生对象，各自 `DerivedByteRatio`/retention/`VersionFactor`；`DISABLED` 必须是带 provenance 的显式 0 |
| `REC_LEGAL_HOLD_EXTRA_BYTES` | bytes | **普通保留/版本总量之外**的额外 legal hold 体量；缺失即未知，不当作 0 |
| `REC_BACKUP_COPY_FACTOR` | `CopyFactor` | 独立备份副本策略（`>=0`）；与 COS 内部复制无关 |

`REC_SAMPLE_RATE_HZ`=`hertz`、`REC_BIT_DEPTH`=`bit` 是 Unit 枚举成员；`REC_ORIGINAL_VERSION_FACTOR=1.2` 属合法 `VersionFactor`（不是 `RATIO_OUT_OF_RANGE`）。

**Growth / async**

| Input ID | Unit | 说明 |
| --- | --- | --- |
| `META_BY_TYPE` | map | 五类 metadata，各含 `rowsPerDay`/`bytesPerRow`/`retentionDays`；retention 属 Q13 |
| `DB_AUX_BY_KIND` | map(bytes) | `index`/`wal`/`replication`/`backup` 独立实测；不推断数据库总量 |
| `QUEUE_ARRIVAL_TASKS_PS` / `QUEUE_SERVICE_TASKS_PS` | tasks/s | 队列实测到达/服务率 |
| `QUEUE_BYTES_PER_TASK` | bytes/task | 实测平均任务字节 |
| `QUEUE_INITIAL_BACKLOG_TASKS` | `DiscreteCount` | 实测现有积压 |
| `AI_TASK_ARRIVALS_PS` | tasks/s | AI/worker 独立实测任务到达率 |
| `AI_TASK_SERVICE_RATE_PS` | tasks/s | 实测**聚合池**服务完成率（整个测量池，不是单 worker）；只用于 load ratio，不反推平均在服任务数 |
| `AI_MEAN_SERVICE_S` | s | 实测任务平均在服时长；用于 Little 平均在服任务数 `arrivals×meanService` |
| `AI_TASK_BYTES` | bytes/task | AI 任务平均字节；不得由录音秒数×bitrate 推导 |

**Spool**

| Input ID | Unit | 说明 |
| --- | --- | --- |
| `SPOOL_DISK_BYTES` | bytes | spool 卷原始总容量（不是可用空间）；**中断开始快照** |
| `SPOOL_FREE_BYTES` | bytes | 中断开始快照的**可用**空间（已排除现有 backlog；不得再减 existing） |
| `SPOOL_EXISTING_BYTES` | bytes | 中断开始快照的现有 payload backlog（含于 `used=disk−free`）；仅用于 end-of-outage backlog 与一致性，不再从 free 减一次 |
| `SPOOL_NEW_PEAK_BYTES_PS` | bytes/s (payload) | 中断期间新增 payload 峰值速率，独立测量；`peakRate×outage` 只是容量投影上界，不是实测峰值占用保证 |
| `SPOOL_NEW_AVG_BYTES_PS` | bytes/s (payload) | **同一中断窗口**新增 payload 平均速率；`avgRate×outage` 是窗口限定的中断产量，用于 end-of-outage backlog；峰值>=均值只在同窗口/同 source 时断言 |
| `SPOOL_OUTAGE_S` | s | 中断时长，必须 >0 |
| `SPOOL_HEADROOM_RATIO` | `Ratio01` | 预留水位 0..1 |
| `SPOOL_RECOVERY_TOTAL_BYTES_PS` | bytes/s (payload) | 恢复窗口实测 **gross payload** 上传/确认吞吐（不是日均、不是 wire、不是已扣继续生产的 spare） |
| `SPOOL_CONTINUING_BYTES_PS` | bytes/s (payload) | 恢复窗口内同时继续产生的 payload 吞吐；与 `SPOOL_RECOVERY_TOTAL_BYTES_PS` 同基准，只减一次 |

**Spool 快照契约（N-01）**：disk/free/existing 同为中断开始快照；`used=disk−free` 已含 existing，因此 `peakOccupancy` 只再加中断产量，绝不重复加 existing。`SPOOL_RECOVERY_BACKLOG_BYTES` 是**派生的 end-of-outage backlog 指标**（非 caller 输入）= `SPOOL_EXISTING_BYTES + SPOOL_NEW_AVG_BYTES_PS×SPOOL_OUTAGE_S`；中断产量为 0 时它等于 existing，为正时大于 existing。drain 只使用该派生 backlog，不得用 start existing 代替。所有 spool 字节/速率均为 payload 基准；wire 测量必须先由调用方按显式 overhead 转成 payload 或另提供 payload 实测，计算器不猜；F-UP-04 的 wire 输出只来自显式 overhead 估算或显式实测。

**Backup / capability（仅 S2 落文）**

| Input ID | Unit | 说明 |
| --- | --- | --- |
| `asOf` | iso8601 | 顶层显式确定性比较基准；`backupRecovery` 必需，`compute` 不读 wall clock |
| `BACKUP_DEPENDENCY[]` | typed | 六类：`postgresPitr,cosVersions,kmsKeyVersions,secretVersions,reconciliationOutbox,ivrLocalFacts`；每项字段见 §4.3 F-BACKUP-01/02 |
| `CAPABILITY[]` | typed | `CV-1..CV-5` + `regionAzSku`；每项 `{status: UNVERIFIED 或 CLAIMED_VERIFIED, claimedBy, owner, source, evidenceRef, verifiedAt}`；`CLAIMED_VERIFIED` 仍只是调用方断言，root `cloudVerified`/`a3RiskClosed` 恒 false |

`meta` 固定 `{status:UNAPPROVED_TEMPLATE|SYNTHETIC_TEST_ONLY|APPROVED_SOURCE_INPUTS, source, approvedBy, approvedAt}`；`APPROVED_SOURCE_INPUTS` 要求非空批准人、ISO 日期和来源，但仍不等于生产就绪或采购就绪，也不为任何叶背书。

### 4.3 具名公式（每个 metric 必须带 formulaId/unit）

| Formula | 计算 | 单位/失败与零规则 |
| --- | --- | --- |
| `F-IVR-01` | `A = IVR_IN_RATE_PS × IVR_IN_HOLD_S` | channels；缺（无显式零因子短路）=> `INCOMPLETE`；显式 0 => 0 `COMPLETE` + `ZERO_FACTOR_SHORT_CIRCUIT`；负值 `INVALID` |
| `F-IVR-02` | `B = IVR_OUT_RATE_PS × IVR_OUT_OCC_S` | channels；振铃已含在占用中，不重复加 |
| `F-IVR-03` | `bridgePorts = IVR_BRIDGE_RATE_PS × IVR_BRIDGE_MEAN_S` | bridge ports；不是线路通道；无独立 bridge 容量输入时只报算术 |
| `F-IVR-04` | `SEPARATE`：inbound=A、outbound=B；`SHARED`：`shared=A+B` | channels；模式与 limit 不匹配 => `MODE_LIMIT_MISMATCH` |
| `F-IVR-05` | `usable=limit×(1−headroom)`；`shortfall=max(0,demand−usable)`；`utilization=demand/usable`（`UtilizationRatio`，可为分数且可 >1）；线路裁决另见 F-IVR-05a | channels/ratio；`0/0` => utilization 0 + `IDLE_ZERO_DENOMINATOR`；demand>0 且 usable=0 => utilization null + `DIVISION_BY_ZERO` + `NOT_MET`；utilization>1 合法并走 `NOT_MET`（不是 `RATIO_OUT_OF_RANGE`）；平均 fit 不是 admission |
| `F-IVR-05a` | 线路裁决：`shortfall=0` 且匹配 kind 的合格 observed peak <= usable，且 demand/peak 为 `MEASURED`、limit/headroom 为 `MEASURED` 或 `APPROVED_POLICY` => `LINE_FIT_EVIDENCE_COMPLETE`；`SEPARATE` 用 inbound/outbound 峰值，`SHARED` 用 shared 峰值，bridge 峰值只并列不裁决；峰值缺失 => `NOT_VERIFIED` + `PEAK_EVIDENCE_MISSING`；峰值超 usable 或 shortfall>0 => `NOT_MET`；`ESTIMATE`/synthetic/零 demand 或零 usable => `ARITHMETIC_ONLY`（零状态另标 `ZERO_DEMAND_NOT_A_FIT`）；永不由平均值保证峰值；裁决仍非采购批准 |
| `F-IVR-06` | `ringRatio=IVR_OUT_RING_S/IVR_OUT_OCC_S`；observed peaks 仅带 count/window/scope 并列 | ratio；ring>occ 或 occ=0 且 ring>0 => `INVALID`；两者 0 => 0 + `IDLE_ZERO_DENOMINATOR`；观测并发不反推 QPS |
| `F-IVR-07` | `sum(IVR_MIX_RATE_PS)=IVR_IN_RATE_PS`；各类 >=0 | calls/s；缺失类 `INCOMPLETE`（不补 0）；和不符 `MIX_SUM_MISMATCH` |
| `F-API-01` | `apiTotal=sum(API_RATE_PS)` | requests/s；缺类 `INCOMPLETE`；禁止由 `IVR_*` 推导 |
| `F-API-02` | `inFlight=API_RATE×API_MEAN_SERVICE_S`；`dbTxnInFlightEstimate=API_RATE×API_DB_TXN_MEAN_S` | requests / transactions；显式 0 => 0；DB 事务数是按每请求 1 事务的**估计**，带 `TXN_PER_REQUEST_NOT_MEASURED`，不是连接数/SKU 建议 |
| `F-NODE-01` | 仅 stateless role：`requiredNodes=ceil(demand/(perNodeRate×(1−headroom)))` | nodes；缺实测/分母<=0 且 demand>0 => `INCOMPLETE`/`MEASUREMENT_REQUIRED`；demand=0 => 0 + `ZERO_DEMAND_NOT_A_FIT`（无需除法） |
| `F-NODE-02` | stateless：`nMinusOne=perNodeRate×(1−headroom)×(deployedNodes−1)`；`fit=demand<=nMinusOne` 且 `deployedNodes>=requiredNodes` | boolean；缺 deployedNodes => `NOT_VERIFIED`；<2 => `N_MINUS_ONE_IMPOSSIBLE`；deployed<required => `NOT_MET` + `DEPLOYED_BELOW_REQUIRED`；fail => `NOT_MET` |
| `F-NODE-03` | 每个 role 检查四项资源：`measuredUtilization[k]+headroom<=1`，且四项证据+ref+measuredAt 齐全 | boolean；缺任何一项 => `NOT_VERIFIED` + `RESOURCE_EVIDENCE_MISSING`；超限 => `NOT_MET` + `RESOURCE_HEADROOM_INSUFFICIENT`；通过也只是 evidence，不是容量认证 |
| `F-NODE-04` | stateful role：`normalFit=demand<=normalCapacity×(1−headroom)`；`degradedFit=demand<=degradedCapacity×(1−headroom)` | boolean；缺正常/降级测量 => `NOT_VERIFIED`；不得用节点数 active-active 算术 |
| `F-NODE-05` | media-gateway：`sharedFit=PB_CONCURRENT_SESSIONS<=sharedCoreApiEdgeCapacity×(1−headroom)` | boolean；共享逻辑容量，不另计部署节点；出现 perNodeRate/deployedNodes => `INVALID` + `SHARED_CAPACITY_NODE_ARITHMETIC_FORBIDDEN`；不并入 core-api 节点求和 |
| `F-NODE-06` | 逐 role `roleVerdict = worst(算术 fit, 资源检查, 证据充分性)` | enum：缺算术输入 => `INCOMPLETE`/null；任一 fit `NOT_MET` => `NOT_MET`；缺资源/拓扑证据 => `NOT_VERIFIED`；全部算术通过但用了 `ESTIMATE`/synthetic 或 demand=0 => `ARITHMETIC_ONLY`（demand=0 另加 `ZERO_DEMAND_NOT_A_FIT`）；仅当全部 fit 为真、`deployedNodes>=requiredNodes`、资源证据四项齐全且通过、demand/capacity 为 `MEASURED`（headroom 可为 `APPROVED_POLICY`）、stateless 已做 N-1 / stateful 已做降级 / gateway 已做共享检查时 => `FIT_EVIDENCE_COMPLETE`；永不为采购批准 |
| `F-PB-01` | `PB_CONCURRENT_SESSIONS×PB_BITRATE_BPS` | bits/s；媒体网关/数据面播放，不是业务 RTP |
| `F-PB-02` | `PB_SESSIONS_PER_DAY×PB_MEAN_SECONDS×PB_BITRATE_BPS/8` | bytes/day；客户端/origin 缓存前 payload 场景，不是交付拓扑批准 |
| `F-PB-03` | `F-PB-02×(1−PB_CDN_CACHE_HIT_RATIO)` | origin bytes/day；缺 ratio 且 F-PB-02 非零 => `INCOMPLETE`；F-PB-02=0 => 0 + `ZERO_FACTOR_SHORT_CIRCUIT`；仅条件式缓存算术；不批准 CDN 拓扑，不绕过 bearer/generation 校验 |
| `F-REC-01` | 优先 `REC_BITRATE_BPS`（全声道总量）；否则仅 `PCM_LINEAR` 用 `sampleRate×bitDepth×channels` | bits/s；其他 codec 无位率 => `INCOMPLETE`；不猜压缩率；PCM 公式已含 channels，不得再乘声道数 |
| `F-REC-02` | `F-REC-01/8×REC_MEAN_VALIDATED_S` | bytes/recording；以服务端校验时长为准；声明时长不参与 |
| `F-REC-03` | `REC_PER_DAY×F-REC-02` | bytes/day；不含派生/版本/legal hold/备份 |
| `F-REC-04` | `F-REC-03×REC_RETENTION_DAYS×REC_ORIGINAL_VERSION_FACTOR` | bytes；retention/version 各自独立，缺失即 unknown，不默认 1/0 |
| `F-REC-05` | 逐类型：`derivedDaily=F-REC-03×bytesPerOriginalByte`；`derivedRetained=derivedDaily×retentionDays[type]×versionFactor[type]` | bytes；四类各自 metric；`DISABLED` 必须显式 0+provenance；缺失类型 unknown/partial；不得套用原始 retention/version |
| `F-REC-06` | `logicalMedia=F-REC-04+ΣF-REC-05+REC_LEGAL_HOLD_EXTRA_BYTES`；`backupMedia=REC_BACKUP_COPY_FACTOR×logicalMedia` | bytes；legal hold 是普通总量之外的额外字节，不再乘 version factor；backup 独立策略；不乘 COS 内部副本；任一分量缺失 => total `INCOMPLETE`，分量仍保留 |
| `F-REC-07` | 逐类：`rowsPerDay×bytesPerRow×retentionDays` | bytes；五类各自 metric，缺失不补 0，partial 保留 |
| `F-UP-01` | `dailyUploadAvgPayload=F-REC-03×8/86400` | bits/s；日均 payload **下界**，不是峰值采购依据；零录音 => 0 |
| `F-UP-02` | `peakWire` = 显式 `UPLOAD_MEASURED_PEAK_WIRE_BPS`（优先）；否则 `UPLOAD_CONCURRENT_PEAK×UPLOAD_PER_STREAM_WIRE_BPS` | bits/s；缺任一峰值输入 => `INCOMPLETE` + `PEAK_INPUTS_MISSING`（峰值证据不适用零因子短路）；不使用 codec bitrate |
| `F-UP-03` | `peakPayloadEst≈peakWire×(1−UPLOAD_WIRE_OVERHEAD_RATIO)` | bits/s；overhead 缺失且 peakWire 非零 => payload 峰值 `INCOMPLETE`，wire 仍可报告；peakWire=0 => 0；`ESTIMATE_ONLY` |
| `F-UP-04` | 分开报告 `ongoingWire=F-UP-02`；`backlogCatchUpPayloadBits=(backlog>0 且 net>0) ? net×8 : 0`（net/backlog 见 F-SPOOL-03，payload 基准）；`backlogCatchUpWireEstimate=payloadBits/(1−UPLOAD_WIRE_OVERHEAD_RATIO)` 仅在 overhead 完整时输出并标 `ESTIMATE_ONLY`；combined 仅在显式 `UPLOAD_COMBINED_MEASURED_WIRE_BPS` 时报告 | bits/s；payload 绝不标为 wire；overhead 缺失 => payload 仍 COMPLETE，wire 为 null + `INCOMPLETE`（missing=`UPLOAD_WIRE_OVERHEAD_RATIO`），不猜 0 overhead；backlog=0 => catch-up 0；backlog 或 net 缺失 => catch-up `INCOMPLETE`（不填 0）；backlog>0 且 net<=0 => 0 + `DRAIN_IMPOSSIBLE`/`NOT_MET`；不自动求和 ongoing+catch-up；catch-up 不能把 gross 或 codec bitrate 当 net；更快于实时的上传不反推 codec bitrate |
| `F-DB-01` | 逐 kind：`auxBytes[kind]=DB_AUX_BY_KIND[kind]`（显式回显，不推断） | bytes；index/WAL/replication/backup 各自独立；`dbSizingComplete` 恒为 false（永不声称完整数据库 sizing） |
| `F-QUEUE-01` | `net=service−arrival`；backlog=0 => drain 0；net>0 且 backlog>0 => `drain=backlog/net`；net=0 => `BACKLOG_NOT_DRAINING`；net<0 => `BACKLOG_GROWING`；`backlogBytesChangePerS=(arrival−service)×bytesPerTask`（负值=积压字节减少） | seconds/bytes；只在实际除法时检查；不填 0；任务/字节独立；符号必须明确 |
| `F-AI-01` | `loadRatio=arrivals/aggregateServiceRate`（无量纲负载比）；`avgInServiceTasks=arrivals×meanServiceS`（Little 平均在服任务数，允许分数）；`aiBytesPerS=arrivals×bytesPerTask`（独立字节率） | ratio / tasks / bytes/s；serviceRate 是**聚合池**完成率，loadRatio 不是平均在服任务数且 >1 合法（status `COMPLETE`，带 `LOAD_RATIO_NOT_OCCUPANCY`，不据此自动判定排队）；arrivals=0 => 三项 0 + `ZERO_FACTOR_SHORT_CIRCUIT`（缺失 serviceRate/meanService/bytes 仍记 missing，不得变成未知需求）；arrivals=0 且 serviceRate 显式 0 => loadRatio 0 + `IDLE_ZERO_DENOMINATOR`；arrivals>0 且 serviceRate=0 => loadRatio `DIVISION_BY_ZERO`/`NOT_MET`；arrivals>0 且 meanService 缺失 => 仅 avgInServiceTasks `INCOMPLETE`；平均不是峰值；禁止录音秒数×bitrate 捷径 |
| `F-SPOOL-01` | `used=disk−free`（中断开始快照，已含 existing）；`outageProducedPeak=NEW_PEAK×outage`；`outageProducedAvg=NEW_AVG×outage`；`peakOccupancy=used+outageProducedPeak`；`avgOccupancy=used+outageProducedAvg`；`SPOOL_RECOVERY_BACKLOG_BYTES=SPOOL_EXISTING_BYTES+outageProducedAvg`（派生的 end-of-outage backlog，非 caller 输入） | bytes（payload）；existing 已含于 used，不再重复加；peak 投影是容量上界，不是实测峰值保证；peak/avg 独立，仅同窗口/同 source 时断言 peak>=avg，否则 `PEAK_AVG_WINDOW_UNQUALIFIED`；outage<=0 `INVALID`；零中断产量 => backlog=existing（合法 0 或正值） |
| `F-SPOOL-02` | `usable=disk×(1−headroom)`；`fitPeak=peakOccupancy<=usable`；`freeAfter=max(0,usable−peakOccupancy)`；`overflowBytes=max(0,peakOccupancy−usable)` | bytes/boolean；一致性要求 `existing<=disk−free=used` 且 `free<=disk`，否则 `CAPACITY_INCONSISTENT`；不得从 free 再减 existing（free 已排除它）；不输出负容量；peakOccupancy 不得二次计入中断产量 |
| `F-SPOOL-03` | `netRecoveryPayloadBytesPerS=recoveryTotal−continuing`（同 payload 基准）；backlog=`SPOOL_RECOVERY_BACKLOG_BYTES`；backlog=0 => drain 0（net=0 时另加 `IDLE_ZERO_DENOMINATOR`，net<0 时仍 0）；backlog>0 且 net<=0 => `DRAIN_IMPOSSIBLE`/`NOT_MET`；否则 `drain=backlog/net` | seconds；使用 end-of-outage backlog（existing+中断产量），**不得**用 start existing 代替；continuing 只减一次；payload 基准；不得输出负值/Infinity |
| `F-BACKUP-01` | 六依赖逐项 typed 完整性 + 聚合 `backupClosure`（见下） | 每依赖 metric：COMPLETE/INCOMPLETE/INVALID/NOT_MET；聚合取最坏；全 excluded/缺失 => `copiesOnly:true` + `COPIES_ONLY_INCOMPLETE` |
| `F-BACKUP-02` | 逐依赖 `observedRpoSeconds<=approvedRpoSeconds` 且 `observedRtoSeconds<=approvedRtoSeconds` | MET/NOT_MET/OBSERVATION_MISSING；缺观测 `INCOMPLETE`；超出 => `NOT_MET` + `TARGET_NOT_MET`；零值合法 |
| `F-CAP-01` | `CLAIMED_VERIFIED` 需 `claimedBy/owner/source/evidenceRef/verifiedAt` 齐全；`UNVERIFIED` 保持未知 | 调用方断言；`UNVERIFIED` => `NOT_VERIFIED`；`CLAIMED_VERIFIED` 结构齐全 => 仍 `NOT_VERIFIED`（value=`CLAIMED_VERIFIED`）并带限制，绝不为 COMPLETE/VERIFIED；缺任一字段 => `INVALID` + `EVIDENCE_REQUIRED`；root `cloudVerified=false`、`a3RiskClosed=false` 为固定字段（调用方断言不可改变，见 §4.1 DTO）；无论是否全 claimed，均输出 `DEPLOYMENT_EVIDENCE_INCOMPLETE`、`NO_CLOUD_VERIFICATION`、`A3_RISK_NOT_CLOSED`，生产/采购恒 false |

`F-BACKUP-01/02` 与 `F-CAP-01` 属 S2 公式；S1 不实现、不写其 docs/18 章节。

**F-BACKUP-01/02 精确字段与规则**：每个依赖项必须提供 `dependencyId`、`included:boolean`（普通布尔，不是 ScalarInput）、`approvedRpoSeconds:ScalarInput(seconds)>=0`、`approvedRtoSeconds:ScalarInput(seconds)>=0`、`restoreTestedAt:ScalarInput(iso8601)`、`observedRpoSeconds?:ScalarInput(seconds)>=0`、`observedRtoSeconds?:ScalarInput(seconds)>=0`、`owner:string`、`evidenceRef:string`。`asOf` 必须显式提供且可解析；缺失 => `ASOF_REQUIRED`。`restoreTestedAt`/`verifiedAt` 晚于 `asOf` => `DATE_IN_FUTURE`；空/不可解析 => `DATE_INVALID`。`included` 非 boolean => `TYPE_INVALID`；`included=false` => `DEPENDENCY_EXCLUDED`（不是 missing，仍保留该项）；字段缺失 => `DEPENDENCY_MISSING`；二者必须可区分。`evidenceRef` 同时承载目标覆盖/样本覆盖引用；它们只是调用方证据完整性，输出 `EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY` 限制，绝不证明实际可恢复。六项全部 excluded/missing => `COPIES_ONLY_INCOMPLETE`。`CAPABILITY[]` 项不是容量 ScalarInput，而是断言记录：`{capabilityId,status:UNVERIFIED|CLAIMED_VERIFIED,claimedBy,owner,source,evidenceRef,verifiedAt:ScalarInput(iso8601)}`；有 `asOf` 时 `verifiedAt` 晚于它 => `DATE_IN_FUTURE`，无 `asOf` 时不做未来比较并保留限制；`UNVERIFIED` 不需要证据，`CLAIMED_VERIFIED` 缺任何字段 => `EVIDENCE_REQUIRED`。

单位规则：时长一律 `s`，字节一律 `bytes`（十进制基数），带宽一律 `bits/s`；比率/因子按 §4.1 具名 `scalarKind`（`Ratio01`、`WireOverheadRatio`、`UtilizationRatio`、`VersionFactor`、`CopyFactor`、`DerivedByteRatio`）校验，不给全局 0..1 规则；`DiscreteCount` 为非负整数，`NonNegativeMeasure` 派生平均可为分数；输入/输出表中的人读缩写对应 §4.1 Unit 枚举（含 `hertz`/`bit`/`transactionsPerSecond`/`opsPerSecond`/`messagesPerSecond`）。显示 `GiB` 时只做 `/2^30` 派生并明确标签；不得把 GB/GiB 混用。公式常量仅 `8 bits/byte`、`86400 s/day`、`2^30`。

### 4.4 缺失、零、未知、非法、批准与状态

- **缺失 ≠ 0**：`null`/absent/`provenance:"MISSING"` 产生 `missing[]`、metric `INCOMPLETE`、`value:null`；不因缺值而跳过其余 metric。
- **显式零**：带完整 `unit/source/owner/provenance` 的 0 是合法测量/策略值；零呼入率、零录音、零 backlog、零 RPO 都必须得到合法零输出，而不是 `INCOMPLETE`/`DIVISION_BY_ZERO`。纯乘法中若一个因子是显式 0（其他因子非负或缺失），结果短路为 0 并标 `ZERO_FACTOR_SHORT_CIRCUIT`（记录未使用的缺失因子）；加法项、证据字段和除数仍不得以 0 补齐。
- **先校验后短路**：所有已提供的叶先做 type/unit/`scalarKind` 范围/整型/非有限校验；显式 0 只可短路**缺失**（不是 malformed）的未使用因子，并把它们记入 `missing[]`；任何 malformed 叶（负数、错误类型、错误单位、非有限、越界、`DiscreteCount` 小数）一律进入 `invalid[]`/`root.errors[]`，即使另一因子为 0 也不得被短路吞掉，metric 至少 `INVALID`。被优先分支遮蔽但已提供的叶同样先校验并上报，不得静默丢弃。
- **除法只在真正除法处**：`F-IVR-05` 的 `demand/usable`、`F-IVR-06` 的 ring/occ、`F-NODE-01` 的 per-node 除法、`F-QUEUE-01`/`F-AI-01` 的 rate 除法、`F-SPOOL-03` 的 backlog/net 是实际除法。`0/0` 仅在明确 idle 语义（零需求/零量）时输出 0 并加 `IDLE_ZERO_DENOMINATOR`；其他 `0/0` 或正分子/零分母按公式返回 `DIVISION_BY_ZERO` 或对应 `NOT_MET`/`MEASUREMENT_REQUIRED`，不得输出 Infinity/NaN。派生 utilization>1 不是除法错误，而是 `NOT_MET`；只有 `Ratio01` 输入 >1 才是 `RATIO_OUT_OF_RANGE`。
- **非法**：NaN/±Infinity、负数（允许的差值中间量除外）、`DiscreteCount` 小数、具名 ratio/factor 越界（`Ratio01`/`WireOverheadRatio` 越界或 `VersionFactor`/`CopyFactor`/`DerivedByteRatio` 低于下界；派生 `UtilizationRatio` >1 不在此列）、单位不匹配、未知键、振铃大于占用、流量分项和不等于呼入率、模式/limit 不匹配、安全整数溢出、日期非法/未来（有 asOf 时）、批准元数据不完整、`CLAIMED_VERIFIED` 无证据、share 逻辑容量被拆成独立节点、顶层调用方字段 `productionReady`/`procurementReady`/`approved`/`cloudVerified`/`a3RiskClosed`，返回对应稳定 code（后五个统一 `CALLER_CONTROL_FORBIDDEN`）。
- **批准**：叶批准与 meta 分离；`root` 不能批准不相关字段；缺失 source/owner/approval 的叶指标至少 `UNAPPROVED`，绝不显示 `APPROVED`。输入批准不改变 `productionReady=false` 与 `procurementReady=false`，也不改变 `cloudVerified=false`/`a3RiskClosed=false`；调用方不能通过 `productionReady=true` 绕过（`CALLER_CONTROL_FORBIDDEN`）。所有合成 fixture 用 `SYNTHETIC_TEST_ONLY`，输出 `synthetic:true` 与 `NOT_APPROVED_PRODUCTION_VALUE`。`ESTIMATE` 输入可算但不得 `APPROVED`，节点/线路不得据此输出正向证据完整裁决。
- **unknown 传播**：metric 级传播；group/root 取最坏；部分 metric 完整时 `partial:true`；group/root 保留各 metric 的 `missing/invalid/reasons`，不掩盖原因。
- **空集合/缺组**：必填集合为空（如 `MEASURED_ROLE=[]`、`BACKUP_DEPENDENCY=[]`、`CAPABILITY=[]`）视为 missing/`INCOMPLETE`，不是“零问题”或 COMPLETE；模板全 null/空集合必须让 7/9 组全 `INCOMPLETE`。

### 4.5 关键正确性案例

- **腿/桥接/线路**：A、B、bridge 分开算；共享线路求和、专用线路分别检查；`IVR_IN_HOLD_S` 已含菜单/DTMF/振铃，不再加 `IVR_OUT_RING_S`（后者只描述 B 路占用构成）；观测峰值只并列/用于证据裁决，不由平均值拟合；无足够独立峰值证据时线路建议 `UNVERIFIED`。
- **Little 定律**：所有 `rate×duration` 输出是同一窗口/工况的稳态平均占用；平均占用不能证明峰值、trunk admission 或 2N 安全；峰值必须独立观测并带 window/scope。`F-AI-01` 的 `loadRatio` 是无量纲负载比，不是平均在服任务数；平均在服任务数使用独立 `AI_MEAN_SERVICE_S` 做 `arrivals×meanService`。
- **类型化范围**：`Ratio01`/`UtilizationRatio`/`VersionFactor`/`CopyFactor`/`DerivedByteRatio`/`DiscreteCount` 各自校验；`versionFactor=1.2` 合法；派生 utilization>1 => `NOT_MET`；派生 channels/inFlight/avgInService 可为分数；`hertz`/`bit`/`transactionsPerSecond`/`opsPerSecond`/`messagesPerSecond` 均有 Unit 枚举成员。
- **API/媒体**：`API_RATE_PS` 是控制面实测请求，不由并发或 IVR 推算；RTP 不在业务服务；播放走 A3 bearer media-gateway；逻辑 gateway 共享 core-api/edge，不单列部署节点求和。
- **录音/存储**：`REC_BITRATE_BPS` 是**全声道总**位率；PCM fallback 才用 `sampleRate×bitDepth×channels`；位率/时长/格式/校验时长分别 typed；派生四类各有独立 ratio/retention/version；legal hold 是额外字节；backup copy factor 独立；不乘 COS 内部副本；元数据五类独立。
- **上传**：日均 payload 下界、并发峰值 wire、payload 估计分层输出；overhead 显式标注；峰值输入缺失即 `INCOMPLETE`；ongoing 与 backlog catch-up 分开（catch-up 输出 payload bits；wire 仅由显式 overhead 估算并标 `ESTIMATE_ONLY`，缺失则 wire `INCOMPLETE`）；payload 绝不标成 wire；编码 bitrate 不是上传速度。
- **Spool**：disk/free/existing 同为中断开始快照；used=disk−free 已含 existing，peakOccupancy 只加中断产量；end-of-outage backlog=`existing+avg×outage` 用于 drain，不得用 start existing 代替；零 backlog 必为零 drain，零 existing + 正中断产量必须得到正 drain；正 backlog + net<=0 => `DRAIN_IMPOSSIBLE`；所有 spool 字节/速率为 payload，不标 wire；peak/avg 独立。
- **节点/failover**：线性每节点乘法只用于实测同质无状态池；N-1 乘同一 headroom；DB/cache/queue 用实测正常/降级聚合容量；media-gateway 共享；CPU/RAM/连接/IO 缺失 => `NOT_VERIFIED`，`roleVerdict` 不得为 `FIT_EVIDENCE_COMPLETE`；估计值仅 `ARITHMETIC_ONLY`；零需求另标 `ZERO_DEMAND_NOT_A_FIT`；无 SKU 建议。
- **增长/异步**：metadata 逐类 partial；DB aux 四类独立且永不声称完整数据库 sizing；queue 零 backlog=>0 drain，net<=0 有 backlog=>`BACKLOG_NOT_DRAINING`/`BACKLOG_GROWING`；AI 明确区分聚合负载比 `loadRatio=arrivals/aggregateServiceRate`（ratio，>1 合法、非平均在服任务数）与 Little 平均在服 `arrivals×meanServiceS`（tasks，允许分数）；`aiBytesPerS` 独立；不用录音时长×bitrate。
- **恢复**：六依赖 + approved target + 演练 observed + 显式 asOf；零 RPO 合法；excluded 与 missing 区分；缺观测 `INCOMPLETE`；observed>target `NOT_MET`；证据完整≠可恢复；PG PITR 不含 IVR 本地 B-leg/spool 事实；KMS 销毁 key 不可恢复。
- **能力**：`CLAIMED_VERIFIED` 只是调用方断言；结构齐全才非 INVALID；输出明确 `NO_CLOUD_VERIFICATION`、`A3_RISK_NOT_CLOSED`；region/AZ/SKU 仍需 Infrastructure/Security 手工验证；绝不关闭 A3 风险。

## 5. 部署、发布、恢复与能力证据

### 5.1 已定决策（来自 `docs/12` §4/§5，不重新选择）

腾讯云中国大陆地域 + 私有 VPC；`core-api`/`async-worker`/`scheduler` 同源模块化单体；IVR 保持在运营商机房并拥有 SIP/RTP、A/B 路、桥接、录音、限时；`ivr-gateway` 与 IVR 同机房；业务服务不承载 RTP；COS 私有桶、SSE-KMS、versioning 为需求（能力待验证）；PostgreSQL 主备 + PITR；首发不要求 Kubernetes/微服务；播放经既有 core-api/edge 的 media-gateway bearer 数据面，不新增 A1 operation、不返回永久 COS URL；上传沿用 A3 CV-4 的 single-object presigned PUT，能力不可验证即 fail closed。

### 5.2 发布、回滚、扩缩容与门禁

- 不可变镜像/配置版本；Flyway Expand/Contract；先服务端兼容再切客户端/IVR；应用回滚不回滚已提交数据，需迁移兼容窗口。
- 扩缩容只依据已批准输入与 `F-NODE-*` 结果；拓扑（2+ API、单主 scheduler、双 IVR）不是容量证明；无 SKU/规格推荐。
- **验证门**：模板组全 `INCOMPLETE` 不得采购；任何 required 组未 `COMPLETE` 或叶未 `APPROVED` 不得宣称可上线；节点正向 `FIT_EVIDENCE_COMPLETE` 需要实测 demand/capacity + 四项资源证据 + 同 headroom 的 N-1/降级检查；线路建议需要合格独立峰值证据；恢复任何依赖 `NOT_MET`/缺观测/缺 asOf 不得声称可恢复；能力 `CLAIMED_VERIFIED` 不是云验证；`productionReady`/`procurementReady`/`cloudVerified`/`a3RiskClosed` 恒 false。
- **停止条件**：若需新增公共契约、改变 IVR 媒体/播放边界、选择 region/SKU/价格/保留期、伪造测量或把合成/估计当批准，停止并上报；不得把 spool start snapshot 与 end-of-outage backlog 混同，不得把 payload 标成 wire 或把 load ratio 当平均在服任务数。

### 5.3 备份/PITR/恢复依赖矩阵（`F-BACKUP-01/02`）

| 依赖 | 必须输入 | 不能声称 |
| --- | --- | --- |
| PostgreSQL PITR | approved RPO/RTO、restore drill observed RPO/RTO、restoreTestedAt、owner/evidence、asOf | 有备份即恢复 |
| COS 版本 | 原始/派生对象、不可变历史版本、legal hold 覆盖、版本恢复演练 observed、证据 | 逻辑字节×副本数即容量 |
| KMS key versions | 历史 key version、recover/disable/destroy 策略、演练观测、证据 | 销毁 key 可恢复 |
| Secret Manager versions | 版本化凭据与恢复路径、演练观测、证据 | 当前 secret 覆盖历史 |
| Reconciliation/Outbox | 恢复后对账、Outbox 重放/去重、DLQ、观测与证据 | 数据恢复即业务一致 |
| IVR 本地事实 | B-leg operation、spool、通话中事实的恢复/对账、观测与证据 | PG PITR 覆盖 IVR 本地 |

每行都必须有 `included=true`、数值 target（零合法）、`owner`/`evidenceRef`、`restoreTestedAt`，并在显式 `asOf` 下比较 observed；缺项为 `DEPENDENCY_MISSING`，显式排除为 `DEPENDENCY_EXCLUDED`；`observed>target` 为 `NOT_MET`。恢复依赖/样本覆盖只是调用方证据完整性，不证明可恢复。

### 5.4 云能力/容量证据清单（只列待验证）

`CV-1..5`（KMS、Secret Manager、CAM、COS private/SSE-KMS/versioning/presign/private endpoint/object lock、CloudAudit/log）；region/AZ/SKU、storage class、价格与预算、网络路径（专线/IPSec、COS 私网或 HTTPS）、CDN/LB media-gateway 拓扑、数据库 HA/IOPS、每节点实测吞吐/连接/IO、SLO/RPO/RTO、媒体保留与 legal hold。每项记录 `claimedBy/owner/source/evidenceRef/verifiedAt`；A4 只定义证据字段与门禁，**不声称**任何能力已验证、不关闭 A3 风险；region/AZ/SKU 验证保持人工，由 Infrastructure/Security 执行。root `cloudVerified`/`a3RiskClosed` 恒 false，调用方断言不能改变。

## 6. 计算器与验证设计

### 6.1 工具约束

`calc.mjs` 零三方依赖、Node 22 ESM、纯函数 `compute(input)` 与 `--input` CLI；S1 只实现 §4.3 的 7 个 S1 组公式（不含 F-BACKUP-*/F-CAP-*），S2 再加这两组；不读文档、不联网、不访问云/凭据、不读 wall clock。`validate.mjs` 加载模板与 fixtures，独立调用 `compute` 后断言状态/字段/错误 code；不把 fixture 的 `expected` 传给计算器。所有算术结果在每一步后检查 `Number.isFinite` 与安全整数范围；`DiscreteCount` 字段必须非负整数，派生 `NonNegativeMeasure`（channels/inFlight/avgInService/channels 平均等）允许非负实数；比率/因子按具名 `scalarKind`（`Ratio01`/`WireOverheadRatio`/`UtilizationRatio`/`VersionFactor`/`CopyFactor`/`DerivedByteRatio`）校验，不使用全局 0..1。**F-08**：JSON fixtures 只包含严格 JSON 可表示的非法值（负数、`DiscreteCount` 小数、越界 ratio、`VersionFactor<1`、错误字符串类型、单位不匹配、溢出等）；`validate.mjs` 另外用程序化 `nonFiniteProbes` 把真实 JS `NaN`、`+Infinity`、`-Infinity` 注入每个数值叶（及关键中间输入），断言 `NUMBER_NONFINITE`。字符串 `"NaN"`/`"Infinity"` 不得被数值化，必须 `TYPE_INVALID`（no coercion）。

### 6.2 命令与行为断言

```bash
node --check deployment/capacity/calc.mjs
node --check deployment/capacity/validate.mjs
node deployment/capacity/validate.mjs --template deployment/capacity/inputs.template.json --stage s1
# 断言：恰好 7 组，全部 INCOMPLETE，0 INVALID，productionReady=false，
#       procurementReady=false，cloudVerified=false，a3RiskClosed=false，synthetic=false；缺失指标不全为 0
node deployment/capacity/validate.mjs --stage s1 --fixtures deployment/capacity/fixtures
# 断言：每个 S1 fixture 声明的 behavior/status/code/dimension 全部通过；任一失败非 0 退出
node deployment/capacity/validate.mjs --template deployment/capacity/inputs.template.json --stage s2
# 断言：恰好 9 组，全部 INCOMPLETE，0 INVALID，productionReady=false，procurementReady=false，
#       cloudVerified=false，a3RiskClosed=false
node deployment/capacity/validate.mjs --stage s2 --fixtures deployment/capacity/fixtures
# 断言：S1 回归仍通过；每个 S2 fixture 行为通过；README 链接唯一可读
```

- **F-01 边界**：S1 只对 7 组模板与 S1 公式负责；S2 才对 9 组模板负责。S1 必须覆盖其实现的全部公式（含 `F-API-*`、`F-NODE-*`），不得把其正确性测试推迟到 S2。fixture 文件/case 数量只作信息输出，**不作为成功判据**；成功判据是每个声明的行为。
- S2 的 `docs/README.md` 检查只断言单行链接存在且唯一，不修改文档语义。

### 6.3 Fixtures（行为分组；数值全部合成）

| Fixture | 行为断言 |
| --- | --- |
| `s1-01-ivr-legs.json` | A/B/bridge 分别 = rate×duration；专用/共享 limit 分别检查；headroom 后 usable/shortfall/utilization 精确；ring>occ、mix 和不符、mode/limit 不匹配 => 稳定 INVALID；零呼入/零占用 => 合法 0；`utilization>1 => NOT_MET`（不是 INVALID）；`0.5 calls/s×7.4 s=3.7 channels` 合法分数；观测峰值仅并列且未反推 QPS；无峰值证据时 `LINE_FIT_EVIDENCE_COMPLETE` 不出现 |
| `s1-02-recording-storage.json` | 全声道 64000 bps、60s、100/日、30 天、version 1.2 => 精确十进制 bytes/GiB 派生；`VersionFactor=1.2` 合法（不得 `RATIO_OUT_OF_RANGE`）；PCM 8000×16×1 fallback，`REC_SAMPLE_RATE_HZ`=`hertz`、`REC_BIT_DEPTH`=`bit` 可赋；压缩 codec 无位率 => INCOMPLETE；派生四类独立（produced/disabled explicit 0/missing => partial）；legal hold 额外字节不乘 version；backup factor 独立；无内部副本乘数；元数据五类独立 |
| `s1-03-upload-playback.json` | 日均 payload 下界 = daily bytes×8/86400；并发×每流 wire 峰值；显式实测峰值替代；overhead 后 payload 估计带 `ESTIMATE_ONLY`；缺峰值输入 => `PEAK_INPUTS_MISSING`；codec bitrate 不进入上传；catch-up 输出 payload bits（`backlog>0` 时 `net×8`，backlog=0 时为 0），overhead 完整才给 `backlogCatchUpWireEstimate` 并标 `ESTIMATE_ONLY`，overhead 缺失时 payload COMPLETE 而 wire INCOMPLETE；payload 绝不标 wire；ongoing 与 catch-up 分开，combined 仅显式实测；播放并发/日量/origin 条件式；缺 cache ratio => origin INCOMPLETE；CDN/bearer 限制存在 |
| `s1-04-spool.json` | disk/free/existing 同为中断开始快照；used=disk−free；peak/avg 独立；headroom；零中断产量 + existing=0 => backlog=0、drain=0；**existing=0 且正中断产量** => backlog=avg×outage>0、drain>0（不得为 0）；existing>0 + 零产量 => backlog=existing；peakOccupancy 只加一次中断产量；正 backlog + net<=0 => `DRAIN_IMPOSSIBLE`；net>0 => 精确 drain；existing 不再从 free 减；existing>disk−free => `CAPACITY_INCONSISTENT`；payload 基准；无负值/Infinity |
| `s1-05-growth-async.json` | metadata 逐类 bytes；缺类 partial；DB aux 四类独立且 `dbSizingComplete=false`；queue 零 backlog=>0，net=0 有 backlog=>`BACKLOG_NOT_DRAINING`，net<0=>`BACKLOG_GROWING`，net>0=>精确排空；AI `loadRatio=arrivals/aggregateServiceRate`（unit ratio，>1 合法，带 `LOAD_RATIO_NOT_OCCUPANCY`，不得标 tasks）、`avgInServiceTasks=arrivals×meanServiceS`（tasks，可为分数）、`aiBytesPerS=arrivals×bytesPerTask` 独立；serviceRate 为聚合池口径；arrivals=0 => 三项 0（zero factor，缺失因子记 missing），arrivals=0 且 service=0 => loadRatio 0 + `IDLE_ZERO_DENOMINATOR`，arrivals>0 & service=0 => DIVISION_BY_ZERO/NOT_MET；malformed 叶在 arrivals=0 旁仍 INVALID；无录音时长×bitrate 捷径 |
| `s1-06-node-api.json` | API 五类求和、in-flight、db txn 估计带 `TXN_PER_REQUEST_NOT_MEASURED`；不由 IVR/并发推导；stateless 节点数仅实测 role；N-1 使用同一 headroom；deployedNodes<2 => `N_MINUS_ONE_IMPOSSIBLE`；deployed<required => `DEPLOYED_BELOW_REQUIRED`；stateful 正常/降级聚合容量；media-gateway 共享且拒绝独立节点求和；四项资源缺失 => `NOT_VERIFIED` 且 `roleVerdict` 不为 `FIT_EVIDENCE_COMPLETE`；util+headroom>1 => `NOT_MET`；ESTIMATE => `ARITHMETIC_ONLY`；零需求 => `ZERO_DEMAND_NOT_A_FIT`；`transactionsPerSecond`/`opsPerSecond`/`messagesPerSecond` 可赋且 role unit 匹配；`deployedNodes` 小数 => `COUNT_NOT_INTEGER`；in-flight 可为分数；输入 utilization>1 => `RATIO_OUT_OF_RANGE`（与派生 utilization>1=>`NOT_MET` 区分）；`N_MINUS_ONE_IMPOSSIBLE`/`DEPLOYED_BELOW_REQUIRED` 的 metric.status=`NOT_MET`；无 SKU |
| `s1-07-missing-zero-partial.json` | 缺值与显式 0 不同；同组部分 metric COMPLETE + 部分 missing => `partial:true`；group/root 取最坏；缺失不填 0；独立组仍计算；零值合法输出；零因子旁 malformed 叶仍 INVALID |
| `s1-08-invalid-values.json` | JSON 可表示：负数、`DiscreteCount` 小数（`COUNT_NOT_INTEGER`）、`Ratio01`/`WireOverheadRatio` 越界、`VersionFactor<1`（如 0.5 => `RATIO_OUT_OF_RANGE`）、字符串数字（TYPE_INVALID，不 coercion）、单位不匹配、ring>occ、mix 和不符、mode/limit 不符、安全整数溢出 => 稳定 INVALID；反例：`versionFactor=1.2` 合法、派生 utilization>1 => `NOT_MET`、`0.5 calls/s×7.4 s` 分数 channels 合法、malformed 叶在零因子旁仍 INVALID；`validate.mjs` 的程序化 NaN/±Infinity probe 另断言 `NUMBER_NONFINITE` |
| `s1-09-provenance-approval.json` | 缺 source/owner/approval 的指标不显示 APPROVED；meta APPROVED_SOURCE_INPUTS 不为叶背书；ESTIMATE + APPROVED => `APPROVAL_INVALID`；SYNTHETIC_TEST_ONLY => synthetic true；调用方 `productionReady`/`procurementReady`/`approved`/`cloudVerified`/`a3RiskClosed` => `CALLER_CONTROL_FORBIDDEN`；批准的输入仍 `productionReady=false`、`cloudVerified=false`、`a3RiskClosed=false` |
| `s2-01-backup-recovery.json` | 六依赖完整 + 零 RPO 合法 => 完整；无演练 => INCOMPLETE；`included=false` => `DEPENDENCY_EXCLUDED` 与缺字段 `DEPENDENCY_MISSING` 可区分；observed>target => `NOT_MET`；observed<=target => MET 仍带证据完整性限制；缺 asOf => `ASOF_REQUIRED`；未来日期 => `DATE_IN_FUTURE`；坏 boolean/number/date => INVALID；全 excluded/missing => `COPIES_ONLY_INCOMPLETE`；重复运行结果相同 |
| `s2-02-capability-assertion.json` | 全 `UNVERIFIED` 保持未知；`CLAIMED_VERIFIED` 缺 owner/source/evidence/date => `EVIDENCE_REQUIRED`；齐全仍为 `NOT_VERIFIED`（value=`CLAIMED_VERIFIED`），并输出 `NO_CLOUD_VERIFICATION`、`A3_RISK_NOT_CLOSED`；root `cloudVerified=false`、`a3RiskClosed=false` 直接断言，调用方尝试改写 => `CALLER_CONTROL_FORBIDDEN`；不关闭 A3 风险、不产生采购/生产批准 |
| `s2-03-deployment-gates.json` | 9 组模板；S1 回归通过；能力/region/SKU 未验证时门禁 `DEPLOYMENT_EVIDENCE_INCOMPLETE`；即使算术完整 `productionReady=false`、`procurementReady=false`、`cloudVerified=false`、`a3RiskClosed=false`；README 单行链接存在且唯一 |

显式负例分散在 `s1-01/03/04/05/06/07/08/09` 与 `s2-01/02/03`；不以负例数量作为成功指标，而以每文件声明的行为断言为准。

### 6.4 诚实限制

不运行真实线路、SIP/CDR、IVR 媒体引擎、PostgreSQL/Redis/RabbitMQ、COS/KMS/Secret Manager/CAM、CDN/LB/media gateway、云审计或压测；不提供 benchmark、账单、region/SKU 或恢复实测。fixtures 只证明算术、维度、失败关闭和输出契约，不是生产验收、容量证明、云能力验证或恢复能力证明。CDN 缓存命中只是条件算术，不批准交付拓扑；能力 `CLAIMED_VERIFIED` 不是云验证；节点估计不是 SKU 建议；spool 只消费 payload 实测/合成输入，不验证真实磁盘或恢复吞吐；catch-up wire 是 overhead 估算；AI loadRatio 不是占用。数据库 HA/IOPS、queue/AI service rate、每节点吞吐/资源利用等高级 sizing 仍是 owned measurement/deferred input；计算器只消费调用方提供的有限值。

## 7. 实施切片（2）

### S1 — 容量算术、单位与未知传播

- 文件（仅此）：`docs/18-production-capacity-and-deployment-spec.md`（§1-§6 容量/存储/上传/播放/spool/元数据/队列/AI/实测节点）、`deployment/capacity/calc.mjs`、`deployment/capacity/validate.mjs`、`deployment/capacity/inputs.template.json`（7 组）、`deployment/capacity/fixtures/s1-01..s1-09`、`docs/gateflow/a4-production-capacity-deployment-spec-s1-implementation.md`。
- 边界：全部 S1 容量组、模板、具名 scalarKind/整型范围、spool 快照与 end-of-outage backlog、payload/wire 基准、AI load ratio 与平均在服分离、公式、非法/缺值/零值/溢出/合成标记；**测试本片实现的每个公式，包括 API 与节点**；不含备份/能力/部署门禁与 README。
- 退出：§6.2 S1 template 行为断言通过（7 组全 INCOMPLETE）；stage=s1 每个 fixture 行为通过；所有输出带单位/公式/unknown；无生产数值；`partial`/最坏状态语义正确；root `cloudVerified`/`a3RiskClosed` 为 false 常量（DTO）。
- 停止：任何公式需要未批准生产值/云能力/契约变更，或算术需要猜测压缩率、保留期、副本策略、CDN 拓扑、峰值证据；不得混同 spool start snapshot 与 end-of-outage backlog、payload 与 wire、load ratio 与平均在服任务数。

### S2 — 部署/恢复/能力证据与可发现性

- 文件（仅此）：`docs/18`（追加 §7-§10 部署、发布/回滚、恢复依赖、能力清单、残留）、`deployment/capacity/calc.mjs`（仅新增 backup/capability 组）、`deployment/capacity/validate.mjs`（stage s2 + 9 组模板断言 + 非有限 probe/README 检查）、`deployment/capacity/inputs.template.json`（**明确授权的 additive 编辑：新增 backupRecovery、capabilityEvidence 两组**）、`deployment/capacity/fixtures/s2-01..s2-03`、`docs/README.md`（一行链接）、`docs/gateflow/a4-production-capacity-deployment-spec-s2-implementation.md`。
- 边界：恢复依赖闭包、能力证据 fail-closed、root `cloudVerified`/`a3RiskClosed` 常量断言、节点/存储采购门禁、README 发现入口；不改 S1 公式语义或已过 fixtures，但允许为 stage s2 增加断言。
- 退出：§6.2 S2 template 行为断言通过（9 组全 INCOMPLETE）；stage=s1 回归仍通过；stage=s2 每个 fixture 行为通过；仅副本恢复失败；能力无证据失败；缺实测节点不给建议；README 链接可读。行为断言通过，不以 fixture 数量为判据。
- 停止：不得把能力断言验证为“云已通过”，不得关闭 A3 风险，不得改写 root `cloudVerified`/`a3RiskClosed`，不得选择 region/SKU/价格/保留期，不得扩展公共契约，不得进入生产验收矩阵或本计划之外的实现。

## 8. 风险与残留下沉（保持原 owner/destination）

| 残留/输入 | Owner / Destination |
| --- | --- |
| Q7 线路/接入号/录音格式/缓冲/时钟、上传并发与网络实测 | Telephony/IVR；线路联调 + A4 input |
| Q8 时长容差/SLO/超时/独立峰值观测窗口 | IVR/Backend/QA；容量与验收 |
| Q11 COS class/object-lock/媒体保留 | Infrastructure/Security；存储/容量 |
| Q13 审计/Outbox 保留、DLQ、volume、queue/AI 实测 | Operations/Backend；runbook/可观测性 |
| Q17/Q23 数据库角色/ownership 部署执行、DB 正常/降级拓扑测量 | Platform/Database/Security；部署准备 |
| Q25 授权窗口/时钟偏差 | IVR/Entitlement/Backend；配置与验收（非 30s TTL） |
| Q2/Q3 KMS 与法律保留/删除、legal hold 额外字节口径 | Security/Infrastructure、Legal/Security |
| CV-1..5 / region/AZ/SKU | Infrastructure/Security；能力验证证据（人工） |
| `R-A3-COS-CAPABILITY` / `R-A3-NO-WORM` | Infrastructure/Security；存储容量/对象锁 |
| `R-A3-REGION-SKU` | Infrastructure/Security；region/AZ/SKU/价格证据 |
| `R-A3-MEDIA-GATEWAY-TRANSPORT`（含 CDN/LB 拓扑） | Infrastructure/Security；播放拓扑/带宽 |
| `R-A3-AUDIT-VOLUME` | Operations/Backend；审计量/保留 |
| `R-A3-KMS-CAPABILITY` / `R-A3-KEY-DESTRUCTION` | Security/Infrastructure；恢复策略 |
| `R-A3-LEGAL-RETENTION` / `R-A3-MTLS-CAPABILITY` | Legal/Security、Telephony/IVR |
| `R-A2-UPLOAD-OBSERVE` / `R-A2-EVIDENCE-HANDOFF` / `R-A2-MULTIPLE-RECORDING` | IVR/Evidence；媒体观察与证据归属 |
| `R-A2-HEARTBEAT-VOLUME` | Telephony/Operations；Q8/Q13 |

30 秒仅是既有任务 TTL 产品默认；请求超时、授权窗口、spool 保留、审计保留均不在 A4 决定。

## 9. 实施 Agent 完成报告模板

1. task label、provider/model、gate、base commit、slice ID。
2. 精确 changed files（只含本 slice 允许路径）与 SHA-256/行数；声明 README 无 A4 变更（只读）。
3. 实现的公式 ID、input 字段、单位/scalarKind、spool 快照与 end-of-outage backlog、payload/wire 基准、unknown/invalid/zero/partial 语义与输出 flags；逐项对应 §4.3。
4. 部署/恢复/能力决策摘要及“决策 vs 待证据”清单。
5. 实际命令、exit code、关键输出：S1/S2 template 组数与状态、fixture 行为断言、负例 code、`productionReady`/`procurementReady`/`cloudVerified`/`a3RiskClosed` 恒 false、`synthetic` 标记、非有限 probe；未运行项明确写 not run（不得声称 Node 测试已跑，若未跑）。
6. 目标对齐：每个容量组对应 §1 成功信号与 S1/S2 行为 fixture；无 A0/A1/A2/A3 契约修改、无新 endpoint/表/状态/角色/SKU。
7. 残留下沉 owner/destination；明确未验证的云/测量/恢复能力。
8. 诚实限制：无真实线路/媒体/DB/云/压测/恢复演练/benchmark/CDN 拓扑证据。
9. 完成或阻塞声明；若阻塞，列证据、未改路径、决策人。
10. 明确声明：只改允许路径；未 stage/commit/push/PR/review；未进入下一 Gate。

## 10. 无过度设计与目标对齐

| 成功信号 | 落点 | 行为 fixture |
| --- | --- | --- |
| 单位/scalarKind/来源/批准/公式/缺失/限制/partial | ScalarInput + scalarKind + output DTO + template | `s1-02/07/09` |
| 完整通话 vs A/B 腿、峰值证据、线路/failover | `F-IVR-01..07` | `s1-01` |
| API vs RTP/媒体网关 | `F-API-01/02`、`F-PB-01..03` | `s1-03/06` |
| 录音/全声道位率/PCM/派生/版本/legal hold/备份/元数据 | `F-REC-01..07` | `s1-02` |
| 上传均值/峰值/开销/追赶 payload 与 wire 分离 | `F-UP-01..04` | `s1-03` |
| spool 中断开始快照/end-of-outage backlog/payload/peak/avg/drain | `F-SPOOL-01..03` | `s1-04` |
| metadata/DB 开销/queue/AI 负荷比 vs Little 平均在服 | `F-REC-07`、`F-DB-01`、`F-QUEUE-01`、`F-AI-01` | `s1-05` |
| 实测节点/failover/stateful/shared/roleVerdict/单位枚举 | `F-NODE-01..06` | `s1-06` |
| 恢复依赖闭包/零 RPO/观测比较 | `F-BACKUP-01/02` | `s2-01` |
| 云能力调用方断言/root `cloudVerified`/`a3RiskClosed` | `F-CAP-01` + §4.1 DTO + §5.4 | `s2-02` |
| 部署门禁与发现性 | §5.2 + README 单行 | `s2-03` |

- **不新增 surface/契约**：播放沿用 A3 bearer 与既有 A1 operation；上传沿用 A3 CV-4；不新增表、endpoint、Problem code、协议事件、角色或拓扑。
- **不建通用引擎/镜像 registry**：有限具名公式；引用上游 ID，不复制 44 表/49 operation/23 deferred 清单；输入表只列 A4 自身 typed 输入。
- **不发明数值/能力**：模板全 null；fixtures 全合成；region/SKU/价格/保留/SLO/RPO/RTO/并发/吞吐不批准；固定拓扑不作为容量证明。
- **不混淆基准**：spool start 快照 ≠ end-of-outage backlog；payload ≠ wire；AI load ratio ≠ 平均在服任务数；`DiscreteCount` ≠ 派生分数平均；输入 utilization 越界 ≠ 派生 utilization>1。
- **两片最小且行为可分**：S1 算术/未知/零/partial；S2 部署/恢复/能力证据；无按文件或云产品机械拆分。
- **不把风险偷藏为设计**：所有未决值、云能力、恢复实测、IVR 本地事实、Little 定律峰值缺口、CDN 拓扑均显式 residual/limitation。
- **可回溯**：腿/桥接 ← `docs/12` §9、`docs/13`、`docs/16` §6；录音/对象/上传 ← A0 §4.39-§4.43、`docs/16` §7、`docs/17` §16；恢复 ← `docs/12` §13、A3 §20；API/播放/共享容量 ← A1/A3。

## 11. 计划完成定义

- 本文件 UTF-8、无冲突标记、无 trailing whitespace；本 fix 2 只修改本计划并新增 `docs/gateflow/a4-production-capacity-deployment-spec-plan-fix-2.md`，未改任何其他路径；README 当前无 A4 变更。
- 包含目标/成功信号/非目标/边界、第一性原理与证据、精确文件与 canonical 决策、逐叶 typed input/集合/公式/单位/scalarKind/零/unknown/invalid/partial/approval、部署/恢复/能力证据、静态验证/fixtures/行为断言、2 切片与每片文件/边界/退出/停止、残留 owner/destination、完成报告、无过度设计。
- F-01..F-09 与 controller 附加澄清均在正文落点且无回归；N-01..N-05 已关闭；无 blocking question；若实现发现上游语义矛盾或需新契约，停止并上报，不静默修改 accepted contract。
- 本 Gate 完成后停止；不实施、不 review、不 stage/commit/push/PR、不做云/区域/SKU/价格/保留期动作。
