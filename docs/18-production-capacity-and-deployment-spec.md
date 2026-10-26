# 生产容量与部署规格（A4）

> 状态：S1 + S2 参数化规格。**不是生产容量批准、采购依据、云能力验证或恢复证明。**
>
> 设计源：`docs/12-technology-stack-and-system-architecture.md` §4/§5/§13/§17/§18.5，结合 `docs/13`、`docs/14`、`docs/15`、`docs/16`、`docs/17` 的已接受边界。
>
> 唯一计算辅助：`deployment/capacity/calc.mjs`（Node 22 ESM、零三方依赖、确定性 `compute(input)`）。
>
> 唯一输入模板：`deployment/capacity/inputs.template.json`（S1 7 组 + S2 `backupRecovery`/`capabilityEvidence`，共 9 组，全 null typed）。
>
> 本文件 S1 交付 §1-§6（容量算术与未知传播）；S2 追加 §7-§10（部署放置、发布/回滚门禁、备份/PITR/恢复依赖、云能力证据与残留）。备份/能力结论仍只是调用方证据完整性，不是云验证或恢复证明。

## 1. 状态、范围与非声明

### 1.1 交付定位

本规格把容量问题拆成**有限具名公式 + 逐叶 typed input + 明确未知传播**，用于接收未来真实测量值与已批准运营政策。它回答：

- 该指标用了哪个公式（`formulaId`）、哪个单位（`unit`）、哪些输入；
- 输入来源、owner 与批准状态；
- 缺失什么、为什么结果是 `INCOMPLETE` 或 `NOT_VERIFIED`；
- 该结果为什么仍不能用于采购、上线、容量认证或云能力结论。

它**不**回答生产并发、保留期、Region/AZ/SKU、价格、SLO/RPO/RTO 或腾讯云能力是否已验证，也不选择这些值。模板中所有值均为 `null`；fixture 数值全部为 `SYNTHETIC_TEST_ONLY`，只证明算术、维度、失败关闭与输出契约。

### 1.2 固定 root 字段

`compute(input)` 的 root DTO 固定包含：

```text
schemaVersion: 1
status: COMPLETE | NOT_VERIFIED | INCOMPLETE | NOT_MET | INVALID
productionReady: false
procurementReady: false
cloudVerified: false
a3RiskClosed: false
synthetic: boolean
metaStatus: UNAPPROVED_TEMPLATE | SYNTHETIC_TEST_ONLY | APPROVED_SOURCE_INPUTS
asOf: string | null
groups: [...]       # S1 7 组 + S2 2 组，固定顺序
flags: string[]
errors: [{ code, path }]
```

- `cloudVerified` 与 `a3RiskClosed` 是 **root 常量 `false`**；调用方提供的同名字段、能力断言或 meta 批准都不能改变。
- 调用方提供 `productionReady`、`procurementReady`、`approved`、`cloudVerified`、`a3RiskClosed` 中任一顶层字段都会产生 `CALLER_CONTROL_FORBIDDEN`，root `status=INVALID`，但上述常量仍为 `false`。
- `metaStatus=APPROVED_SOURCE_INPUTS` 只说明 meta 的来源/批准记录完整，**不为任何叶输入背书**，也不改变生产/采购/云/A3 常量。

### 1.3 状态严重度与 partial

```text
INVALID(4) > NOT_MET(3) > INCOMPLETE(2) > NOT_VERIFIED(1) > COMPLETE(0)
```

- metric 状态取自身最坏；group 状态取组内 metric 最坏；root 取 group 最坏。
- 任何 `root.errors` 使 root `INVALID`；组内错误路径使对应 group `INVALID`。
- 同组至少一个 metric `COMPLETE` 且至少一个非 `COMPLETE` 时 `partial:true`。
- 缺失 metric 的 `value` 为 `null`，**不得填 0**；显式 0 是合法输入/结果。
- 语义 verdict 字符串（如 `FIT_EVIDENCE_COMPLETE`、`ARITHMETIC_ONLY`、`NOT_MET`、`LINE_FIT_EVIDENCE_COMPLETE`）只出现在 `metric.value`，metric.status 仍取五值。

### 1.4 切片边界与停止条件

S1 只交付容量算术与未知传播（§1-§6）；备份/恢复依赖、能力证据、部署放置、发布/回滚门禁与 README 入口在 S2（§7-§10）追加。若后续需求需要新增公共契约、选择 Region/SKU/价格/保留期、伪造测量或把合成/估计当批准，必须停止并上报，不能由本计算器扩大范围。

## 2. 单位、输入契约、来源与批准

### 2.1 ScalarInput

每个数值/枚举输入叶必须是：

```text
{
  value: T | null,
  unit: Unit,
  provenance: MISSING | SYNTHETIC_TEST_ONLY | MEASURED | ESTIMATE | APPROVED_POLICY,
  source: string | null,
  owner: string | null,
  approval: MISSING | UNAPPROVED | APPROVED | SYNTHETIC_TEST_ONLY
}
```

- `value:null` 或 `provenance:MISSING` => 该叶缺失；不得当作 0。
- `MEASURED` 要求非空 `source` 与非空 `owner`；`approval` 可为 `UNAPPROVED`。
- `APPROVED_POLICY` 要求非空 `source`、非空 `owner` 且 `approval:APPROVED`。
- `ESTIMATE` 可参与算术，但永远不能 `approval:APPROVED`（否则 `APPROVAL_INVALID`）；其指标至多 `UNAPPROVED`。
- `approval:APPROVED` 还要求 `provenance ∈ {MEASURED, APPROVED_POLICY}`、非空 `source`/`owner` 且有值。
- `SYNTHETIC_TEST_ONLY` 要求 `approval:SYNTHETIC_TEST_ONLY`；root `synthetic:true` 并输出 `NOT_APPROVED_PRODUCTION_VALUE`。
- 显式 `0` 在单位、来源、owner、provenance/approval 完整时是合法值，不等于缺失。

### 2.2 Unit 枚举

```text
callsPerSecond | seconds | channels | bridgePorts | ratio | count |
countPerDay | requestsPerSecond | requests | transactionsPerSecond |
opsPerSecond | messagesPerSecond | connections | sessions | bitsPerSecond |
bytes | bytesPerSecond | bytesPerDay | tasksPerSecond | tasks |
transactions | bytesPerTask | nodes | days | hertz | bit | boolean |
enum | iso8601
```

时长一律 `seconds`，字节一律 `bytes`（十进制基数），带宽一律 `bitsPerSecond`。公式常量仅 `8 bits/byte`、`86400 s/day`、`2^30`（显示 GiB 时只做 `/2^30` 派生并明确标签；不得 GB/GiB 混用）。

`REC_SAMPLE_RATE_HZ` 一类显示别名使用枚举成员 `hertz`；位深使用 `bit`；控制面/状态库使用 `transactionsPerSecond`、`opsPerSecond`、`messagesPerSecond`。

### 2.3 具名 scalarKind（不使用全局 ratio 0..1）

| scalarKind | 范围 | 整型 | 典型字段 |
| --- | --- | --- | --- |
| `Ratio01` | `0 <= x <= 1` | 否 | IVR headroom、spool headroom、节点 headroom、CDN cache hit、四项节点利用率 |
| `WireOverheadRatio` | `0 <= x < 1` | 否 | `UPLOAD_WIRE_OVERHEAD_RATIO` |
| `UtilizationRatio` | `x >= 0` | 否 | 派生线路 utilization；>1 合法并驱动 `NOT_MET` |
| `VersionFactor` | `x >= 1` | 否 | 原始/派生版本系数；1.2 合法 |
| `CopyFactor` | `x >= 0` | 否 | 备份副本系数；0 合法 |
| `DerivedByteRatio` | `x >= 0` | 否 | 派生对象字节比 |
| `NonNegativeRate` | `x >= 0` | 否 | 所有 rate/hertz 输入 |
| `NonNegativeMeasure` | `x >= 0` | 否 | 字节/带宽/派生平均/保留天数 |
| `DurationSeconds` | `x >= 0` | 否 | 时长；`SPOOL_OUTAGE_S` 必须 `>0` |
| `DiscreteCount` | 整数 `>=0` | 是 | 线路端口、声道、位深、日量、并发会话、节点数、观测峰值 count |
| `DailyVolume` | `x >= 0` | 否 | metadata `rowsPerDay`（允许测量分数） |

`COUNT_NOT_INTEGER` 只适用于 `DiscreteCount`。`channels`/`requests`/`tasks` 等派生平均允许非负实数（例如 `0.5 calls/s × 7.4 s = 3.7 channels`）。负值一律 `NEGATIVE_VALUE`；输入 `Ratio01 >1` 或 `WireOverheadRatio >=1`、`VersionFactor <1` 为 `RATIO_OUT_OF_RANGE`；派生 utilization >1 不是输入越界，而是 `NOT_MET`。保留天数按已接受解释使用非负 `NonNegativeMeasure`，不新增整数或上限规则。

### 2.4 七个 S1 模板组与输入 ID 映射

模板 `inputs.template.json` 恰好包含以下 7 组；顶层另有 `meta` 与 typed `asOf`，不属于容量组。

| 组 | 输入 ID（字段名即 ID） |
| --- | --- |
| `ivrLines` | `IVR_IN_RATE_PS`、`IVR_IN_HOLD_S`、`IVR_OUT_RATE_PS`、`IVR_OUT_OCC_S`、`IVR_OUT_RING_S`、`IVR_BRIDGE_RATE_PS`、`IVR_BRIDGE_MEAN_S`、`IVR_TRUNK_MODE`、`IVR_IN_LIMIT_CH`、`IVR_OUT_LIMIT_CH`、`IVR_SHARED_LIMIT_CH`、`IVR_FAILOVER_HEADROOM_RATIO`、`IVR_MIX_RATE_PS{task,noTaskDtmf,activation,rejected,failed}`、`IVR_OBSERVED_PEAK{inbound?,outbound?,shared?,bridge?}` |
| `apiControlPlane` | `API_RATE_PS{customer,operations,ivrControl,publicShare,providerCallback}`、`API_MEAN_SERVICE_S`、`API_DB_TXN_MEAN_S` |
| `measuredNodes` | `MEASURED_ROLE[]`：每项 `role`、`demandUnit`、`demandQuantity`、`headroomRatio`、四项 `measured*Utilization`、`resourceEvidenceRef`、`measuredAt`；stateless 另有 `perNodeRate`、`deployedNodes`；stateful 另有 `measuredNormalTopologyCapacity`、`measuredDegradedTopologyCapacity`；`media-gateway` 另有 `sharedCoreApiEdgeCapacity` |
| `playback` | `PB_CONCURRENT_SESSIONS`、`PB_BITRATE_BPS`、`PB_SESSIONS_PER_DAY`、`PB_MEAN_SECONDS`、`PB_CDN_CACHE_HIT_RATIO` |
| `mediaIngestStorage` | `REC_BITRATE_BPS`、`REC_CODEC`、`REC_SAMPLE_RATE_HZ`、`REC_BIT_DEPTH`、`REC_CHANNELS`、`REC_MEAN_VALIDATED_S`、`REC_PER_DAY`、`REC_ORIGINAL_VERSION_FACTOR`、`REC_RETENTION_DAYS`、`REC_DERIVED_BY_TYPE{preview,transcript,summary,export}`、`REC_LEGAL_HOLD_EXTRA_BYTES`、`REC_BACKUP_COPY_FACTOR`、`UPLOAD_CONCURRENT_PEAK`、`UPLOAD_PER_STREAM_WIRE_BPS`、`UPLOAD_MEASURED_PEAK_WIRE_BPS`、`UPLOAD_WIRE_OVERHEAD_RATIO`、`UPLOAD_COMBINED_MEASURED_WIRE_BPS` |
| `growthAndAsync` | `META_BY_TYPE{audit,outbox,callEvent,evidenceEvent,shareAccess}`（每项 `rowsPerDay`、`bytesPerRow`、`retentionDays`）、`DB_AUX_BY_KIND{index,wal,replication,backup}`、`QUEUE_ARRIVAL_TASKS_PS`、`QUEUE_SERVICE_TASKS_PS`、`QUEUE_BYTES_PER_TASK`、`QUEUE_INITIAL_BACKLOG_TASKS`、`AI_TASK_ARRIVALS_PS`、`AI_TASK_SERVICE_RATE_PS`、`AI_MEAN_SERVICE_S`、`AI_TASK_BYTES` |
| `spool` | `SPOOL_DISK_BYTES`、`SPOOL_FREE_BYTES`、`SPOOL_EXISTING_BYTES`、`SPOOL_NEW_PEAK_BYTES_PS`、`SPOOL_NEW_AVG_BYTES_PS`、`SPOOL_OUTAGE_S`、`SPOOL_HEADROOM_RATIO`、`SPOOL_RECOVERY_TOTAL_BYTES_PS`、`SPOOL_CONTINUING_BYTES_PS` |

来源/owner 按组归属：IVR/线路/录音位率与时长 -> Telephony/IVR；API 与 metadata/queue/database 辅助量 -> Backend/Operations；播放/媒体网关 -> Infrastructure/Backend；spool 与 failover -> Telephony/IVR/Operations；保留期/legal hold -> Legal/Security/Product；节点容量/资源证据 -> Platform/Database/Infrastructure。模板中所有叶的 `provenance/approval` 均为 `MISSING`，没有任何获批生产值。

### 2.5 缺失、零、非法、partial 与 flags

- **缺失 ≠ 0**：缺失产生 `missing[]`、metric `INCOMPLETE`、`value:null`，其余 metric 继续计算。
- **显式零**：完整元数据的 0 是合法测量/政策值。纯乘法中显式 0 因子可短路缺失的未使用因子为 0，并标 `ZERO_FACTOR_SHORT_CIRCUIT`，未使用缺失因子仍进 `missing[]`。
- **先校验后短路**：所有已提供叶先做类型、单位、scalarKind、整型、非有限和范围校验；malformed 叶即使旁边有 0 因子也进入 `invalid[]`/`root.errors[]`，metric 至少 `INVALID`。
- **除法只在真正除法处**：`F-IVR-05`、`F-IVR-06`、`F-NODE-01`、`F-QUEUE-01`、`F-AI-01`、`F-SPOOL-03`。仅明确 idle 语义的 `0/0` 输出 0 + `IDLE_ZERO_DENOMINATOR`；其他零分母按公式返回 `DIVISION_BY_ZERO`/`NOT_MET` 或 `MEASUREMENT_REQUIRED`/`INCOMPLETE`，不输出 `NaN`/`±Infinity`。
- **批准与 unknown 传播**：metric 级传播，group/root 取最坏；`partial:true` 保留成功 sibling；`flags` 为所有 limitation/门禁标记去重并集。
- 浮点十进制输入的和/差比较（仅混流之和）使用 `1e-9` 相对容差以避免二进制浮点伪差；业务含义仍是“分项和必须等于呼入率”，真实不等仍报 `MIX_SUM_MISMATCH`。

S1 涉及的主要错误码：

```text
TYPE_INVALID, UNIT_MISMATCH, NUMBER_NONFINITE, NEGATIVE_VALUE,
COUNT_NOT_INTEGER, RATIO_OUT_OF_RANGE, DIVISION_BY_ZERO,
OVERFLOW_UNSAFE_INTEGER, MODE_LIMIT_MISMATCH, RING_EXCEEDS_OCCUPANCY,
MIX_SUM_MISMATCH, UNKNOWN_KEY, UNKNOWN_ROLE, ROLE_FIELD_MISMATCH,
SHARED_CAPACITY_NODE_ARITHMETIC_FORBIDDEN, PROVENANCE_INVALID,
APPROVAL_INVALID, CALLER_CONTROL_FORBIDDEN, DATE_INVALID,
CAPACITY_INCONSISTENT
```

主要 limitation/flag：

```text
NOT_APPROVED_PRODUCTION_VALUE, MEASUREMENT_REQUIRED, ESTIMATE_ONLY,
ZERO_DEMAND_NOT_A_FIT, AVERAGE_NOT_PEAK, LOAD_RATIO_NOT_OCCUPANCY,
LINE_RECOMMENDATION_UNVERIFIED, PEAK_EVIDENCE_MISSING,
PEAK_INPUTS_MISSING, PEAK_AVG_WINDOW_UNQUALIFIED,
ZERO_FACTOR_SHORT_CIRCUIT, ROLE_NOT_DECLARED,
TXN_PER_REQUEST_NOT_MEASURED, CONDITIONAL_CACHE_SCENARIO,
NO_CDN_TOPOLOGY_APPROVAL, BEARER_CHECKS_UNCHANGED, SHARED_MEDIA_GATEWAY,
DB_SIZING_INCOMPLETE, NO_CLOUD_VERIFICATION, A3_RISK_NOT_CLOSED
```

### 2.6 模板与合成标记

- 模板 7 组全部 `INCOMPLETE`、`0 INVALID`、`productionReady=false`、`procurementReady=false`、`cloudVerified=false`、`a3RiskClosed=false`、`synthetic=false`；任何缺失指标都不得静默变成数值 0。
- fixture 使用 `SYNTHETIC_TEST_ONLY`；root `synthetic=true`，指标带 `NOT_APPROVED_PRODUCTION_VALUE`。合成算术通过不等于生产值批准。
- 验证器以行为断言为准，不统计文件/段落/registry 数量；它独立调用 `compute`，不把 `expected` 传入计算器。严格 JSON 只能表达负数、分数、越界、类型错误、溢出等；真正的 JS `NaN`/`±Infinity` 由程序化 probe 注入每个数值叶并断言 `NUMBER_NONFINITE`。

## 3. IVR 腿、桥接与线路（F-IVR-01..07）

### 3.1 输入与事实边界

- `IVR_IN_RATE_PS` 是峰值 A 路呼入尝试（含无任务/DTMF/开通/试用/失败）。`IVR_IN_HOLD_S` 是 A 路平均总占用（菜单、DTMF、振铃、桥接），已含菜单与振铃。
- `IVR_OUT_RATE_PS` / `IVR_OUT_OCC_S` 是 B 路尝试率与平均占用（含振铃）；`IVR_OUT_RING_S` 只描述 B 路占用中振铃/应答前部分，必须 `<= IVR_OUT_OCC_S`。
- `IVR_BRIDGE_RATE_PS` / `IVR_BRIDGE_MEAN_S` 是峰值桥接通话率与平均桥接时长。
- 完整桥接通话**不是两个永久占用的腿**。A、B、bridge 分别按各自到达率 × 平均占用计算；不得对 N 个完整通话统一乘 2。
- 所有 `rate × meanDuration` 是同一测量窗口/工况下的**稳态平均占用**（Little 定律），不是保证峰值、trunk admission 或 2N 安全证明。峰值只能独立观测并带 `count/windowSeconds/scope`。
- 观测峰值仅旁证/裁决线路证据，不参与平均公式，也不能由平均值“拟合”。

### 3.2 公式

| ID | 计算 | 单位与语义 |
| --- | --- | --- |
| `F-IVR-01` | `A = IVR_IN_RATE_PS × IVR_IN_HOLD_S` | `channels`；显式 0 短路，缺失 `INCOMPLETE`，负值 `INVALID` |
| `F-IVR-02` | `B = IVR_OUT_RATE_PS × IVR_OUT_OCC_S` | `channels`；振铃已含在占用中，不重复加 |
| `F-IVR-03` | `bridgePorts = IVR_BRIDGE_RATE_PS × IVR_BRIDGE_MEAN_S` | `bridgePorts`；不是线路通道 |
| `F-IVR-04` | `SEPARATE` 时 inbound=A、outbound=B；`SHARED` 时 shared=A+B | `channels`；模式与 limit 不匹配 => `MODE_LIMIT_MISMATCH` |
| `F-IVR-05` | `usable=limit×(1−headroom)`；`shortfall=max(0,demand−usable)`；`utilization=demand/usable` | `channels`/`ratio`；`0/0` => 0 + `IDLE_ZERO_DENOMINATOR`；正需求/零分母 => null + `DIVISION_BY_ZERO` + `NOT_MET`；utilization>1 合法并 `NOT_MET` |
| `F-IVR-05a` | 线路裁决（见 §3.3） | `enum`：`LINE_FIT_EVIDENCE_COMPLETE` / `ARITHMETIC_ONLY` / `NOT_MET`；缺失为中游状态 |
| `F-IVR-06` | `ringRatio=IVR_OUT_RING_S/IVR_OUT_OCC_S` | `ratio`；ring>occ 或 occ=0 且 ring>0 => `INVALID`/`RING_EXCEEDS_OCCUPANCY`；两者 0 => 0 + `IDLE_ZERO_DENOMINATOR` |
| `F-IVR-07` | `Σ IVR_MIX_RATE_PS = IVR_IN_RATE_PS` | `callsPerSecond`；缺失类 `INCOMPLETE`（不补 0），和不符 `MIX_SUM_MISMATCH` |

### 3.3 线路裁决（F-IVR-05a）

- `SEPARATE_INBOUND_OUTBOUND` 使用 inbound/outbound 各自限值与观测峰值；`SHARED` 使用 shared 限值与 shared 峰值。bridge 峰值只并列报告，不参与裁决。
- 只有以下全部满足才输出 `LINE_FIT_EVIDENCE_COMPLETE`：
  - `shortfall=0`；
  - 匹配 kind 的观测峰值带有效 `windowSeconds` 与 `scope`，且 `peak <= usable`；
  - demand、观测峰值、limit 为 `MEASURED`（limit 也可 `APPROVED_POLICY`）；headroom 为 `MEASURED` 或 `APPROVED_POLICY`。
- 峰值缺失/无效窗口 => `ARITHMETIC_ONLY` + `PEAK_EVIDENCE_MISSING`（`NOT_VERIFIED`），不出现 `LINE_FIT_EVIDENCE_COMPLETE`。
- 峰值超 usable 或 `shortfall>0` => `NOT_MET`。
- `ESTIMATE`/`SYNTHETIC_TEST_ONLY`/零 demand/零 usable => `ARITHMETIC_ONLY`；零状态另标 `ZERO_DEMAND_NOT_A_FIT`；带 `LINE_RECOMMENDATION_UNVERIFIED`。
- 裁决永不由平均值保证峰值，也不是采购批准。

## 4. 控制面 API、播放与共享媒体网关（F-API-01..02、F-PB-01..03、F-NODE-05）

### 4.1 控制面 API

| ID | 计算 | 单位与语义 |
| --- | --- | --- |
| `F-API-01` | `apiTotal = Σ API_RATE_PS`（customer/operations/ivrControl/publicShare/providerCallback） | `requestsPerSecond`；缺类 `INCOMPLETE`；不得由 IVR 或通话并发推导 |
| `F-API-02` | `inFlight=API_RATE×API_MEAN_SERVICE_S`；`dbTxnInFlightEstimate=API_RATE×API_DB_TXN_MEAN_S` | `requests` / `transactions`；显式 0 => 0；数据库事务数是按每请求 1 事务的**估计**，带 `TXN_PER_REQUEST_NOT_MEASURED`，不是连接数或 SKU 建议 |

`core-api` 是无状态服务；它不承载 RTP。逻辑 `media-gateway` 的播放数据面与 `core-api`/edge 共享容量，不能另列节点再求和。

### 4.2 播放

| ID | 计算 | 单位与语义 |
| --- | --- | --- |
| `F-PB-01` | `PB_CONCURRENT_SESSIONS × PB_BITRATE_BPS` | `bitsPerSecond`；媒体网关数据面播放，不是业务 RTP |
| `F-PB-02` | `PB_SESSIONS_PER_DAY × PB_MEAN_SECONDS × PB_BITRATE_BPS / 8` | `bytesPerDay`；缓存前 payload 场景，不是交付拓扑批准 |
| `F-PB-03` | `F-PB-02 × (1 − PB_CDN_CACHE_HIT_RATIO)` | `bytesPerDay`；缺 ratio 且 PB-02 非零 => `INCOMPLETE`；PB-02=0 => 0 + `ZERO_FACTOR_SHORT_CIRCUIT`；条件式缓存算术 |

`F-PB-03` 带 `CONDITIONAL_CACHE_SCENARIO`、`NO_CDN_TOPOLOGY_APPROVAL`、`BEARER_CHECKS_UNCHANGED`；它不批准 CDN/LB 拓扑，也不绕过 A3 bearer/generation 校验。

## 5. 录音、上传、存储、元数据、队列与 AI

### 5.1 录音与存储（F-REC-01..07）

| ID | 计算 | 单位与语义 |
| --- | --- | --- |
| `F-REC-01` | 优先 `REC_BITRATE_BPS`（全声道总编码位率）；否则仅 `PCM_LINEAR` 用 `REC_SAMPLE_RATE_HZ × REC_BIT_DEPTH × REC_CHANNELS` | `bitsPerSecond`；其他 codec 无位率 => `INCOMPLETE`；不猜压缩率；PCM 公式已含声道，不再乘 channels |
| `F-REC-02` | `F-REC-01 / 8 × REC_MEAN_VALIDATED_S` | `bytes`/录音；服务端校验时长唯一准据，声明时长不参与；缺失校验时长 => `INCOMPLETE`，不回退 |
| `F-REC-03` | `REC_PER_DAY × F-REC-02` | `bytesPerDay`；不含派生/版本/legal hold/备份 |
| `F-REC-04` | `F-REC-03 × REC_RETENTION_DAYS × REC_ORIGINAL_VERSION_FACTOR` | `bytes`；retention/version 各自独立，缺失即 unknown，不默认 1/0 |
| `F-REC-05` | 逐类型 `derivedDaily=F-REC-03×bytesPerOriginalByte`；`derivedRetained=derivedDaily×retentionDays[type]×versionFactor[type]` | `bytesPerDay` / `bytes`；四类各自 metric；`DISABLED` 必须是带 provenance 的显式 0；缺失类型保留 partial/unknown；不得套用原始 retention/version |
| `F-REC-06` | `logicalMedia=F-REC-04+ΣF-REC-05+REC_LEGAL_HOLD_EXTRA_BYTES`；`backupMedia=REC_BACKUP_COPY_FACTOR×logicalMedia` | `bytes`；legal hold 是普通总量之外的额外字节，不再乘 version；backup 是独立策略；不乘 COS 内部副本；任一分量缺失 => total `INCOMPLETE`，分量仍保留 |
| `F-REC-07` | 逐类 `rowsPerDay×bytesPerRow×retentionDays`（audit/outbox/callEvent/evidenceEvent/shareAccess） | `bytes`；五类各自 metric，缺失不补 0，partial 保留 |

`REC_BITRATE_BPS` 必须是全声道总位率；`hertz`/`bit` 单位成员用于 PCM fallback；`VersionFactor=1.2` 合法；`bytesPerOriginalByte=0` 的 `DISABLED` 模式仍需显式 provenance。元数据/审计/Outbox 保留属于 Q13，本文不选择值。

### 5.2 上传（F-UP-01..04）

| ID | 计算 | 单位与语义 |
| --- | --- | --- |
| `F-UP-01` | `dailyUploadAvgPayload = F-REC-03 × 8 / 86400` | `bitsPerSecond`；日均 payload **下界**，不是峰值采购依据；零录音 => 0；带 `AVERAGE_NOT_PEAK` |
| `F-UP-02` | 优先显式 `UPLOAD_MEASURED_PEAK_WIRE_BPS`；否则 `UPLOAD_CONCURRENT_PEAK × UPLOAD_PER_STREAM_WIRE_BPS` | `bitsPerSecond`；缺峰值输入 => `INCOMPLETE` + `PEAK_INPUTS_MISSING`；峰值不适用零因子短路；不使用 codec bitrate |
| `F-UP-03` | `peakPayloadEst ≈ peakWire × (1−UPLOAD_WIRE_OVERHEAD_RATIO)` | `bitsPerSecond`；overhead 缺失且 peakWire 非零 => payload 峰值 `INCOMPLETE`，wire 仍可报告；peakWire=0 => 0；`ESTIMATE_ONLY` |
| `F-UP-04` | `ongoingWire=F-UP-02`；`backlogCatchUpPayloadBits=(backlog>0 且 net>0)? net×8 : 0`；`backlogCatchUpWireEstimate=payloadBits/(1−overhead)`（仅 overhead 完整）；combined 仅显式实测 `UPLOAD_COMBINED_MEASURED_WIRE_BPS` | `bitsPerSecond`；payload 绝不标为 wire；overhead 缺失 => payload 仍 COMPLETE 而 wire `INCOMPLETE`；backlog=0 => catch-up 0；backlog>0 且 net<=0 => 0 + `DRAIN_IMPOSSIBLE`/`NOT_MET`；不自动求和 ongoing+catch-up |

`SPOOL_RECOVERY_BACKLOG_BYTES` 是 F-SPOOL-01 的派生 end-of-outage backlog（payload），F-UP-04 只消费该派生值，不用起始 existing 代替。编码位率不是文件上传 wire 速率；快于实时的上传不能反推 codec bitrate。

### 5.3 数据库辅助、队列与 AI（F-DB-01、F-QUEUE-01、F-AI-01）

- **F-DB-01**：逐 kind 回显 `DB_AUX_BY_KIND{index,wal,replication,backup}`（`bytes`）。四类独立测量，不推断数据库总量；`dbSizingComplete` 恒 `false`，带 `DB_SIZING_INCOMPLETE`。
- **F-QUEUE-01**：
  - `net = QUEUE_SERVICE_TASKS_PS − QUEUE_ARRIVAL_TASKS_PS`；
  - `backlog=0` => drain 0（net=0 另加 `IDLE_ZERO_DENOMINATOR`；net<0 仍 0）；
  - `net=0` 且有 backlog => `BACKLOG_NOT_DRAINING`/`NOT_MET`；
  - `net<0` 且有 backlog => `BACKLOG_GROWING`/`NOT_MET`；
  - `net>0` 且有 backlog => `drain=backlog/net`；
  - `backlogBytesChangePerS=(arrival−service)×bytesPerTask`，负值表示积压字节减少；任务数与字节独立。
- **F-AI-01**：
  - `loadRatio = AI_TASK_ARRIVALS_PS / AI_TASK_SERVICE_RATE_PS`（`ratio`，无量纲负载比；aggregate service rate 是整个测量池完成率）。`loadRatio>1` 合法，status `COMPLETE`，带 `LOAD_RATIO_NOT_OCCUPANCY`，不是占用率、不是平均在服任务数，也不自动判定排队。
  - `avgInServiceTasks = AI_TASK_ARRIVALS_PS × AI_MEAN_SERVICE_S`（`tasks`，Little 平均在服任务数，允许分数）。
  - `aiBytesPerSecond = AI_TASK_ARRIVALS_PS × AI_TASK_BYTES`（`bytesPerSecond`，独立）。
  - `arrivals=0` => 三项 0 + `ZERO_FACTOR_SHORT_CIRCUIT`；缺失 service/mean/bytes 仍进 `missing[]`，不得变成未知需求。
  - `arrivals=0` 且 service=0 => loadRatio 0 + `IDLE_ZERO_DENOMINATOR`；`arrivals>0` 且 service=0 => loadRatio `DIVISION_BY_ZERO`/`NOT_MET`；`arrivals>0` 且 mean 缺失 => 仅平均在服任务 `INCOMPLETE`。
  - 平均不是峰值；禁止用“录音秒数×bitrate”推导 AI 任务量或字节。

## 6. Spool、实测节点与 failover

### 6.1 Spool 快照与派生 backlog（F-SPOOL-01..03）

**单一中断开始快照**：`SPOOL_DISK_BYTES`、`SPOOL_FREE_BYTES`、`SPOOL_EXISTING_BYTES` 都是中断开始时同一次快照。`used = disk − free` 已包含 existing；因此 `SPOOL_EXISTING_BYTES` 不得再次从 free 减，也不得在 `peakOccupancy` 中重复加。

| ID | 计算 | 单位与语义 |
| --- | --- | --- |
| `F-SPOOL-01` | `used=disk−free`；`outageProducedPeak=NEW_PEAK×outage`；`outageProducedAvg=NEW_AVG×outage`；`peakOccupancy=used+outageProducedPeak`；`avgOccupancy=used+outageProducedAvg`；`recoveryBacklog=SPOOL_EXISTING_BYTES+outageProducedAvg` | `bytes`（payload）；`outage<=0` `INVALID`；peak 投影是容量上界，不是实测峰值保证；peak/avg 独立，仅同一窗口/同一 source 时断言 peak>=avg，否则 `PEAK_AVG_WINDOW_UNQUALIFIED` |
| `F-SPOOL-02` | `usable=disk×(1−headroom)`；`fitPeak=peakOccupancy<=usable`；`freeAfter=max(0,usable−peakOccupancy)`；`overflowBytes=max(0,peakOccupancy−usable)` | `bytes`/`boolean`；一致性要求 `existing<=disk−free` 且 `free<=disk`，否则 `CAPACITY_INCONSISTENT`；不得从 free 再减 existing；不输出负容量 |
| `F-SPOOL-03` | `netRecoveryPayloadBytesPerS=recoveryTotal−continuing`；backlog 使用 F-SPOOL-01 派生值；`backlog=0` => drain 0（net=0 另加 `IDLE_ZERO_DENOMINATOR`；net<0 仍 0）；`backlog>0` 且 net<=0 => `DRAIN_IMPOSSIBLE`/`NOT_MET`；否则 `drain=backlog/net` | `seconds`；end-of-outage backlog 必须是 `existing+中断产量`，不得用起始 existing 代替；continuing 只减一次；payload 基准；不输出负值/Infinity |

Spool 的所有字节/速率均为 **payload**。`SPOOL_RECOVERY_TOTAL_BYTES_PS` 是恢复窗口实测 gross payload 上传/确认吞吐，不是日均、不是 wire、不是已扣继续生产的 spare；wire 测量必须先由调用方按显式 overhead 转成 payload 或另提供 payload 实测，计算器不猜。

### 6.2 实测节点与 failover（F-NODE-01..06）

角色分类固定，调用方不能自选“无状态”：

| Role | demandUnit | 允许的容量字段 | 禁止字段 |
| --- | --- | --- | --- |
| `core-api` | `requestsPerSecond` | `perNodeRate`、`deployedNodes` | topology/shared |
| `async-worker` | `tasksPerSecond` | `perNodeRate`、`deployedNodes` | topology/shared |
| `media-ai-worker` | `tasksPerSecond` | `perNodeRate`、`deployedNodes` | topology/shared |
| `postgres` | `transactionsPerSecond` | `measuredNormalTopologyCapacity`、`measuredDegradedTopologyCapacity` | per-node |
| `redis` | `opsPerSecond` | 同上 | per-node |
| `rabbitmq` | `messagesPerSecond` | 同上 | per-node |
| `media-gateway` | `sessions` | `sharedCoreApiEdgeCapacity` | per-node/shared node arithmetic |

- 线性每节点乘法只用于实测同质无状态池；N-1 使用同一 headroom。DB/cache/queue 使用实测正常/降级聚合容量，不做 active-active 节点算术；`media-gateway` 逻辑容量共享在 `core-api`/edge 内。
- 每 role 的资源证据为 `measuredCpuUtilization`、`measuredRamUtilization`、`measuredConnectionsUtilization`、`measuredIopsUtilization`（`Ratio01`）、`resourceEvidenceRef`、`measuredAt`（ISO）。缺任何一项 => 资源检查 `NOT_VERIFIED` + `RESOURCE_EVIDENCE_MISSING`；`util+headroom>1` => `NOT_MET` + `RESOURCE_HEADROOM_INSUFFICIENT`。资源通过也只是证据，不是容量认证。

| ID | 计算 | 语义 |
| --- | --- | --- |
| `F-NODE-01` | stateless：`requiredNodes=ceil(demand/(perNodeRate×(1−headroom)))` | 缺实测/分母<=0 且 demand>0 => `INCOMPLETE`/`MEASUREMENT_REQUIRED`；demand=0 => 0 + `ZERO_DEMAND_NOT_A_FIT`；结果必须安全整数 |
| `F-NODE-02` | stateless：`nMinusOne=perNodeRate×(1−headroom)×(deployedNodes−1)`；`fit=demand<=nMinusOne 且 deployedNodes>=requiredNodes` | 缺 deployed => `NOT_VERIFIED`；<2 => `NOT_MET`/`N_MINUS_ONE_IMPOSSIBLE`；deployed<required => `NOT_MET`/`DEPLOYED_BELOW_REQUIRED`；fail => `NOT_MET` |
| `F-NODE-03` | 四项资源 `util+headroom<=1` 且证据/ref/measuredAt 齐全 | 缺任一项 => `NOT_VERIFIED`/`RESOURCE_EVIDENCE_MISSING`；超限 => `NOT_MET`/`RESOURCE_HEADROOM_INSUFFICIENT`；通过仅证据 |
| `F-NODE-04` | stateful：`normalFit=demand<=normalCapacity×(1−headroom)`；`degradedFit=demand<=degradedCapacity×(1−headroom)` | 缺正常/降级测量 => `NOT_VERIFIED`；不得用节点数 active-active 算术 |
| `F-NODE-05` | gateway：`sharedFit=PB_CONCURRENT_SESSIONS<=sharedCoreApiEdgeCapacity×(1−headroom)` | 出现 `perNodeRate`/`deployedNodes` => `INVALID` + `SHARED_CAPACITY_NODE_ARITHMETIC_FORBIDDEN`；不并入 core-api 节点求和；带 `SHARED_MEDIA_GATEWAY` |
| `F-NODE-06` | 逐 role `roleVerdict = worst(算术 fit, 资源检查, 证据充分性)` | 缺算术输入 => `INCOMPLETE`/null；任一 fit `NOT_MET` => `NOT_MET`；缺资源/拓扑证据 => `NOT_VERIFIED`；全通过但 `ESTIMATE`/synthetic/demand=0 => `ARITHMETIC_ONLY`；仅全 measured 且 N-1/降级/共享检查通过 => `FIT_EVIDENCE_COMPLETE`；永不为采购批准 |

必需 role：`core-api`、`async-worker`、`postgres`、`redis`、`rabbitmq`、`media-gateway`；存在 AI 任务输入时 `media-ai-worker` 也必需。缺任一必需 role => 该组 `INCOMPLETE`，缺省项标 `ROLE_NOT_DECLARED`。`media-gateway` demand 来自 `PB_CONCURRENT_SESSIONS`，role 中的 `demandQuantity` 若提供必须同值且单位 `sessions`，否则 `ROLE_FIELD_MISMATCH`。

节点结果无 SKU/价格/Region 建议；CPU/RAM/连接/IO 或实测吞吐缺失时只能是 `NOT_VERIFIED`，绝不输出正向 deployment-fit；原始算术估计可报告但必须带 `ESTIMATE_ONLY`。本 S1 模板全 null，因此全部组 `INCOMPLETE`，不得用于采购或上线判断。

## 7. 部署放置与边界

### 7.1 已定架构决策（不重新选择）

以下来自 `docs/12` §4/§5 的已接受设计，本规格只引用、不重新选择：

- 腾讯云中国大陆地域 + 私有 VPC；`core-api`、`async-worker`、`scheduler` 保持同源模块化单体，不因容量规格拆分为微服务。
- IVR 保持在运营商机房，拥有 SIP/RTP、A/B 路、桥接、录音和限时事实；`ivr-gateway` 与 IVR 同机房。业务服务不承载 RTP。
- COS 私有桶、SSE-KMS、versioning 是**需求**；其能力是否可用仍待 `CV-4`/`CV-5` 与 Infrastructure/Security 证据，不由本规格声称已验证。
- PostgreSQL 主备 + PITR；首发不要求 Kubernetes/微服务化。
- 播放经既有 core-api/edge 的 media-gateway `Authorization: Bearer` 数据面；不新增 A1 operation，不返回永久 COS URL，不绕过 A3 bearer/generation 校验。
- 上传沿用 A3 `CV-4` 的 single-object presigned PUT；能力不可验证即 fail closed，不降级为公共读写或永久凭证。

### 7.2 容量拓扑边界

- 只有显式测量为**同质无状态池**的 `core-api`/`async-worker`/`media-ai-worker` 才允许线性每节点乘法（`F-NODE-01`）。
- `postgres`/`redis`/`rabbitmq` 使用实测正常/降级聚合容量（`F-NODE-04`），不做 active-active 节点算术。
- `media-gateway` 逻辑容量共享在 `core-api`/edge 内（`F-NODE-05`），不得作为独立部署节点再求和。
- 固定拓扑（2+ API、单主 scheduler、双 IVR）不是容量证明；未提供实测节点/存储/云证据时，部署门禁只能 `INCOMPLETE`/`NOT_VERIFIED`。

## 8. 发布、回滚、扩缩容与门禁

### 8.1 发布与回滚

- 使用不可变镜像/配置版本；数据库迁移按 Flyway Expand/Contract，先服务端兼容再切客户端/IVR。
- 应用回滚不回滚已提交数据，必须保留迁移兼容窗口；回滚计划不改变数据保留/删除政策。
- 发布顺序：服务端兼容版本 → 客户端/IVR 切换 → 观察；任何阶段缺少已批准输入或证据即停止。

### 8.2 扩缩容

- 扩缩容只依据已批准输入与 `F-NODE-*` 结果；不按固定拓扑、价格直觉或未测量的“经验值”扩容。
- 无 SKU/规格推荐；CPU/RAM/连接/IO 或每节点实测吞吐缺失时，节点结论最高 `NOT_VERIFIED`，不得输出正向 deployment-fit。
- 原始算术估计可报告，但必须带 `ESTIMATE_ONLY`，且不等于容量认证。

### 8.3 验证门（fail closed）

| 门禁 | 失败关闭条件 |
| --- | --- |
| 模板/输入 | 9 组模板任一 required 组非 `COMPLETE`，或叶未 `APPROVED` => 不得采购或宣称可上线 |
| 线路 | 缺合格独立峰值证据、`shortfall>0` 或峰值超 usable => 不得给线路正向裁决 |
| 节点 | 缺实测 demand/capacity、四项资源证据、同 headroom 的 N-1/降级/共享检查 => 不得给 `FIT_EVIDENCE_COMPLETE` |
| 存储/上传 | 缺位率/校验时长/保留期/副本策略/实测上传峰值 => 只能 `INCOMPLETE`/`NOT_VERIFIED`；不猜压缩率、保留期或 CDN 拓扑 |
| 恢复 | 任一依赖 `NOT_MET`、缺演练观测或缺 `asOf` => 不得声称可恢复 |
| 能力 | `CLAIMED_VERIFIED` 不是云验证；缺 owner/source/evidence/date => `INVALID`；无论是否全 claimed 均输出 `DEPLOYMENT_EVIDENCE_INCOMPLETE` |
| root 常量 | `productionReady`/`procurementReady`/`cloudVerified`/`a3RiskClosed` 恒 `false`；调用方字段触发 `CALLER_CONTROL_FORBIDDEN` |

### 8.4 停止条件

若需新增公共契约、改变 IVR 媒体/播放边界、选择 region/AZ/SKU/价格/保留期/SLO/RPO/RTO、伪造测量或把合成/估计当批准，必须停止并上报。不得把 spool 起始快照与 end-of-outage backlog 混同，不得把 payload 标成 wire，不得把 load ratio 当平均在服任务数，不得把能力断言当作云验证或关闭 A3 风险。

## 9. 备份、PITR 与恢复依赖闭包（F-BACKUP-01/02）

### 9.1 输入与确定性比较基准

顶层 `asOf` 是 `ScalarInput(iso8601)`，为恢复目标的**唯一**确定性比较基准；`compute` 不读 wall clock。`backupRecovery` 组必需 `BACKUP_DEPENDENCY[]`，固定六项 `dependencyId`：

`postgresPitr`、`cosVersions`、`kmsKeyVersions`、`secretVersions`、`reconciliationOutbox`、`ivrLocalFacts`。

**固定集合语义（exact set）**：上述六个 `dependencyId` 必须各出现且仅出现一次。

- 当数组非空（调用方已提交子集）时，缺失的固定 ID 不得从输出消失：每个缺失 ID 输出 `backupRecovery.<dependencyId>`，状态 `INCOMPLETE`、原因 `DEPENDENCY_ABSENT`，`missing` 指向 `backupRecovery.BACKUP_DEPENDENCY.<dependencyId>`。因此任何缺失都会阻止 `closure` 达到 `BACKUP_CLOSURE_COMPLETE`（fail closed），即使已提交的子集全部 MET。数组为空或集合缺失时，由单个 `backupRecovery.dependencies` 汇总 metric 表达全部缺失。
- 重复的 `dependencyId` 是 `INVALID` + `DEPENDENCY_DUPLICATE`（每个重复项都标记，并产生同码 root error）。输出 metric id 必须唯一；重复项使用 `backupRecovery.item<index>` 回退 id，不得互相覆盖或掩盖。

每项必须提供：

```text
{
  dependencyId: string,
  included: boolean,                         // 普通布尔，不是 ScalarInput
  approvedRpoSeconds: ScalarInput(seconds) >= 0,
  approvedRtoSeconds: ScalarInput(seconds) >= 0,
  restoreTestedAt: ScalarInput(iso8601),
  observedRpoSeconds?: ScalarInput(seconds) >= 0,
  observedRtoSeconds?: ScalarInput(seconds) >= 0,
  owner: string,
  evidenceRef: string
}
```

- `asOf` 缺失但存在依赖项 => `ASOF_REQUIRED`，所有比较为 `INCOMPLETE`；无 `asOf` 时不做未来比较并保留 `ASOF_ABSENT_NO_FUTURE_CHECK`。
- `restoreTestedAt` 不可解析 => `DATE_INVALID`；晚于 `asOf` => `DATE_IN_FUTURE`。两者均为 `INVALID`，不是“未知”。
- `included` 非布尔 => `TYPE_INVALID`；负 RPO/RTO => `NEGATIVE_VALUE`。显式零 RPO/RTO 是合法目标与合法观测。

### 9.2 公式与判定

| ID | 计算/判定 | 语义 |
| --- | --- | --- |
| `F-BACKUP-01` | 六依赖逐项 typed 完整性 + 聚合 `backupRecovery.closure` | 每依赖 metric 取 `COMPLETE`/`INCOMPLETE`/`INVALID`/`NOT_MET`；聚合取最坏；全 excluded/缺失 => `copiesOnly` 语义 + `COPIES_ONLY_INCOMPLETE` |
| `F-BACKUP-02` | 逐依赖 `observedRpoSeconds <= approvedRpoSeconds` 且 `observedRtoSeconds <= approvedRtoSeconds` | 通过 => `MET`/`COMPLETE`；超目标 => `NOT_MET` + `TARGET_NOT_MET`；缺观测 => `OBSERVATION_MISSING`/`INCOMPLETE`；零值合法 |

**公式归因**：依赖 metric 的 `MET`/`NOT_MET` 比较判定归 `F-BACKUP-02`；`DEPENDENCY_EXCLUDED`/`DEPENDENCY_MISSING`/`DEPENDENCY_ABSENT`/`DEPENDENCY_DUPLICATE`/typed `INVALID` 等完整性判定归 `F-BACKUP-01`；`backupRecovery.closure` 聚合归 `F-BACKUP-01`。

**区分规则**：

- `included=false` => `DEPENDENCY_EXCLUDED`：该项仍保留、不是 missing，缺省字段不制造 `DEPENDENCY_MISSING` 噪音。
- `included=true` 但字段缺失 => `DEPENDENCY_MISSING`；与 excluded 明确区分。
- 六项全部 excluded/缺失 => `backupRecovery.closure` 值 `COPIES_ONLY` + `COPIES_ONLY_INCOMPLETE`。副本存在不等于可恢复。
- 所有依赖 metric 均带 `EVIDENCE_COMPLETENESS_NOT_RECOVERABILITY`：`owner`/`evidenceRef` 只是调用方证据完整性，绝不证明实际可恢复、业务一致或 key 未销毁。

### 9.3 依赖矩阵（必须输入，不得声称）

| 依赖 | 必须输入 | 不能声称 |
| --- | --- | --- |
| PostgreSQL PITR | approved RPO/RTO、restore drill observed、restoreTestedAt、owner/evidence、asOf | 有备份即恢复 |
| COS 版本 | 原始/派生、不可变历史版本、legal hold 覆盖、版本恢复演练 observed、证据 | 逻辑字节×副本数即容量 |
| KMS key versions | 历史 key version、recover/disable/destroy 策略、演练观测、证据 | 销毁 key 可恢复 |
| Secret Manager versions | 版本化凭据与恢复路径、演练观测、证据 | 当前 secret 覆盖历史 |
| Reconciliation/Outbox | 恢复后对账、Outbox 重放/去重、DLQ、观测与证据 | 数据恢复即业务一致 |
| IVR 本地事实 | B-leg operation、spool、通话中事实的恢复/对账、观测与证据 | PG PITR 覆盖 IVR 本地 |

恢复目标（approved）与演练观测（observed）是不同字段；零 RPO/RTO 是合法目标；缺演练是 `INCOMPLETE`，观测超目标是 `NOT_MET`，不是健康。`evidenceRef`/样本覆盖只是调用方证据完整性，不证明可恢复。

## 10. 云能力证据清单与残留

### 10.1 能力断言（F-CAP-01）

`capabilityEvidence` 组包含固定 `CAPABILITY[]`：`CV-1`、`CV-2`、`CV-3`、`CV-4`、`CV-5`、`regionAzSku`。每项是断言记录，不是容量 ScalarInput：

**固定集合语义（exact set）**：上述六项必须各出现且仅出现一次。

- 当数组非空时，缺失项不得从输出消失：每个缺失 ID 输出 `capabilityEvidence.<capabilityId>`，状态 `INCOMPLETE`、原因 `CAPABILITY_ABSENT`，`missing` 指向 `capabilityEvidence.CAPABILITY.<capabilityId>`；缺失使该组至少为 `INCOMPLETE`，不得被读成已断言。数组为空或集合缺失时，由单个 `capabilityEvidence.summary` 汇总 metric 表达全部缺失。
- 重复 `capabilityId` 是 `INVALID` + `CAPABILITY_DUPLICATE`（每个重复项都标记，并产生同码 root error）。输出 metric id 必须唯一；重复项使用 `capabilityEvidence.item<index>` 回退 id。

```text
{
  capabilityId: string,
  status: UNVERIFIED | CLAIMED_VERIFIED,
  claimedBy?: string,
  owner?: string,
  source?: string,
  evidenceRef?: string,
  verifiedAt?: ScalarInput(iso8601)
}
```

| 状态 | 输出 |
| --- | --- |
| `UNVERIFIED` | `NOT_VERIFIED`，`value="UNVERIFIED"`；不需要证据，保持未知 |
| `CLAIMED_VERIFIED` 且 `claimedBy/owner/source/evidenceRef/verifiedAt` 齐全 | 仍 `NOT_VERIFIED`，`value="CLAIMED_VERIFIED"`；调用方断言不是验证 |
| `CLAIMED_VERIFIED` 缺任一证据字段 | `INVALID` + `EVIDENCE_REQUIRED` |
| `status` 未知或其他值 | `INVALID` + `TYPE_INVALID` |
| `verifiedAt` 不可解析 / 晚于 `asOf` | `INVALID` + `DATE_INVALID` / `DATE_IN_FUTURE`；无 `asOf` 不做未来比较并保留 `ASOF_ABSENT_NO_FUTURE_CHECK` |

- 无论是否全部 claimed，均输出 `DEPLOYMENT_EVIDENCE_INCOMPLETE`、`NO_CLOUD_VERIFICATION`、`A3_RISK_NOT_CLOSED`。
- root `cloudVerified=false`、`a3RiskClosed=false` 是固定常量；调用方顶层 `cloudVerified`/`a3RiskClosed`/`productionReady`/`procurementReady`/`approved` 触发 `CALLER_CONTROL_FORBIDDEN`，能力断言本身也不能改写这两个常量。
- `regionAzSku` 仍须 Infrastructure/Security 手工验证；本规格不选择 region/AZ/SKU、价格、storage class 或 CDN/LB 拓扑。

### 10.2 待验证能力（只列，不声称）

`CV-1..CV-5`（KMS、Secret Manager、CAM、COS private/SSE-KMS/versioning/presign/private endpoint/object lock、CloudAudit/log）、region/AZ/SKU、storage class、价格与预算、网络路径（专线/IPSec、COS 私网或 HTTPS）、CDN/LB media-gateway 拓扑、数据库 HA/IOPS、每节点实测吞吐/连接/IO、SLO/RPO/RTO、媒体保留与 legal hold。每项记录 `claimedBy/owner/source/evidenceRef/verifiedAt`；A4 只定义证据字段与门禁，**不声称任何能力已验证、不关闭 A3 风险**。

### 10.3 残留与 owner/destination

| 残留/输入 | Owner / Destination |
| --- | --- |
| 线路/接入号/录音格式/缓冲/时钟、上传并发与网络实测 | Telephony/IVR；线路联调 + A4 input |
| 时长容差/SLO/超时/独立峰值观测窗口 | IVR/Backend/QA；容量与验收 |
| COS class/object-lock/媒体保留 | Infrastructure/Security；存储/容量 |
| 审计/Outbox 保留、DLQ、volume、queue/AI 实测 | Operations/Backend；runbook/可观测性 |
| 数据库角色/ownership 部署执行、DB 正常/降级拓扑测量 | Platform/Database/Security；部署准备 |
| 授权窗口/时钟偏差 | IVR/Entitlement/Backend；配置与验收 |
| KMS 与法律保留/删除、legal hold 额外字节口径 | Security/Infrastructure、Legal/Security |
| `CV-1..5` / region/AZ/SKU | Infrastructure/Security；能力验证证据（人工） |
| `R-A3-COS-CAPABILITY` / `R-A3-NO-WORM` | Infrastructure/Security；存储容量/对象锁 |
| `R-A3-REGION-SKU` | Infrastructure/Security；region/AZ/SKU/价格证据 |
| `R-A3-MEDIA-GATEWAY-TRANSPORT`（含 CDN/LB 拓扑） | Infrastructure/Security；播放拓扑/带宽 |
| `R-A3-AUDIT-VOLUME` | Operations/Backend；审计量/保留 |
| `R-A3-KMS-CAPABILITY` / `R-A3-KEY-DESTRUCTION` | Security/Infrastructure；恢复策略 |
| `R-A3-LEGAL-RETENTION` / `R-A3-MTLS-CAPABILITY` | Legal/Security、Telephony/IVR |
| `R-A2-UPLOAD-OBSERVE` / `R-A2-EVIDENCE-HANDOFF` / `R-A2-MULTIPLE-RECORDING` | IVR/Evidence；媒体观察与证据归属 |
| `R-A2-HEARTBEAT-VOLUME` | Telephony/Operations |

### 10.4 诚实限制

本规格不运行真实线路、SIP/CDR、IVR 媒体引擎、PostgreSQL/Redis/RabbitMQ、COS/KMS/Secret Manager/CAM、CDN/LB/media gateway、云审计或压测；不提供 benchmark、账单、region/SKU 或恢复实测。fixtures 只证明算术、维度、失败关闭和输出契约，不是生产验收、容量证明、云能力验证或恢复能力证明。`CLAIMED_VERIFIED` 不是云验证；节点估计不是 SKU 建议；既有 30 秒任务 TTL 只按原 owner 语义引用，本规格不重定义请求超时、授权窗口、spool 保留或审计保留。
