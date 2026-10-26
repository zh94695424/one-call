# A4 生产容量与部署规格 S1 实施报告

> Task label：`a4-capacity-s1-impl-ds-20261026-01`
>
> Provider/model：DeepSeek / `deepseek-flash`（Claude Code sub-agent）
>
> Gate：S1 implementation slice（容量算术、单位与未知传播）
>
> Base：`84233c6`（accepted A4 plan checkpoint）
>
> Slice：S1 only；未进入 code review、fix、re-review、S2、commit、push、PR 或云操作。

## 1. 结论与验证状态（必须先读）

**Author-complete，但运行时验证被环境权限阻断。** 本 slice 允许的全部文件已写入并通过：
- `jq empty` 严格 JSON 语法检查（模板 + 9 个 fixture，exit 0，无输出）；
- template 结构静态检查（7 组、0 个非 null value、provenance/approval 全部 `MISSING`、`MEASURED_ROLE=[]`）；
- `sha256sum` 计算。

**未运行**：`node --check`、`node deployment/capacity/validate.mjs --stage s1`（含模板 7 组行为断言、fixture 行为断言、程序化 NaN/±Infinity probe）。原因：本会话 Bash 权限对上述 node 命令均返回 `This command requires approval`；主 agent 和独立 sub-agent 重试结果相同。由于命令未执行，**没有 exit code、没有行为断言通过结果**，本报告不声称模板/fixture/probe 已通过，也不声称生产容量、云能力、性能或恢复证据。

完成条件因此部分未满足：代码与文档已落地，但“所有验证命令结果真实”需由 controller 在允许 node 执行的环境中运行 §5 的命令后再确认。

> **2026-10-26 bounded S1 fix 1**：针对 controller 观测到的三个 S1 失败修正了一个实现语义和两个 fixture 输入；node 运行时验证在本会话仍被权限网关阻断。详见 §10。

## 2. 变更文件（仅允许路径）

| 文件 | SHA-256 | 行数 |
| --- | --- | --- |
| `deployment/capacity/calc.mjs` | `d38c904f4e50ef320a0b618bb1976cceb0c654f69607f867f46ff3b0bdbb4bf1` | 3818 |
| `deployment/capacity/validate.mjs` | `a0504ba8e4b828ba1891085735fa213f7c9787fb17935fbd60258b997d9952d0` | 535 |
| `deployment/capacity/inputs.template.json` | `bd4082241370e5a1e5840c12e011c25c9508bbc3602603fd4cab8c9135b8a2ea` | 885 |
| `deployment/capacity/fixtures/s1-01-ivr-legs.json` | `e6e2bc84de4f9a28181ec182f22ba79a2158015c5a9d1dd63beb90591307503b` | 185 |
| `deployment/capacity/fixtures/s1-02-recording-storage.json` | `5066c6ff10bb4fe9090f74f3556bfcbadc9d753e950df98a310309537a7c2bfc` | 165 |
| `deployment/capacity/fixtures/s1-03-upload-playback.json` | `08c2b5b10b9aa49ed10c1e49e2b8b5c4eb59eed6404dbd3e7039a9a8627a15cb` | 222 |
| `deployment/capacity/fixtures/s1-04-spool.json` | `5bdd1dc8b25a9610ba6e490d174d26d0642662143d49239140ec044f48c294a7` | 226 |
| `deployment/capacity/fixtures/s1-05-growth-async.json` | `e948e9beafafdc17384fbd851bbdfee2685699db968afaaecdba949d616144e0` | 245 |
| `deployment/capacity/fixtures/s1-06-node-api.json` | `19ff8f288d9ac771c90e3c8830ae688b038ff5ba0af9337987ab0ed4e44758f5` | 497 |
| `deployment/capacity/fixtures/s1-07-missing-zero-partial.json` | `3115d7a6616524838add4c6ce060bb45150b2062e50684a6063a1864951f3816` | 106 |
| `deployment/capacity/fixtures/s1-08-invalid-values.json` | `15d4a77cc7ce9b09ab8d57630576bb7cdca2cb5c0d23eaf9c93b09834a698080` | 109 |
| `deployment/capacity/fixtures/s1-09-provenance-approval.json` | `765040cbb8e25a5657d3f4aa8dda7e071b27c9549d13de14c5215d9b6122a8fd` | 167 |
| `docs/18-production-capacity-and-deployment-spec.md` | `d7a3ddefa7100e783993922bd7e6db63e7311497eb0d1a44feef48f8d6a0ea3f` | 327 |
| `docs/gateflow/a4-production-capacity-deployment-spec-s1-implementation.md` | 本报告 | — |

- `docs/README.md` **无 A4 变更**；S2 才加发现性单行入口。
- 未修改 A0-A3 文档、契约、OpenAPI/schema、原型、截图、共享 QA、安全/协议 registry。
- 未新增依赖、通用容量引擎、endpoint、表、Problem code 或公共契约。

## 3. 实现范围与公式对应

### 3.1 七个 S1 模板组（与输入 ID 映射）

| 组 | 模板中的输入 ID |
| --- | --- |
| `ivrLines` | `IVR_IN_RATE_PS`、`IVR_IN_HOLD_S`、`IVR_OUT_RATE_PS`、`IVR_OUT_OCC_S`、`IVR_OUT_RING_S`、`IVR_BRIDGE_RATE_PS`、`IVR_BRIDGE_MEAN_S`、`IVR_TRUNK_MODE`、`IVR_IN_LIMIT_CH`、`IVR_OUT_LIMIT_CH`、`IVR_SHARED_LIMIT_CH`、`IVR_FAILOVER_HEADROOM_RATIO`、`IVR_MIX_RATE_PS{task,noTaskDtmf,activation,rejected,failed}`、`IVR_OBSERVED_PEAK{inbound?,outbound?,shared?,bridge?}` |
| `apiControlPlane` | `API_RATE_PS{customer,operations,ivrControl,publicShare,providerCallback}`、`API_MEAN_SERVICE_S`、`API_DB_TXN_MEAN_S` |
| `measuredNodes` | `MEASURED_ROLE[]`：`role`、`demandUnit`、`demandQuantity`、`headroomRatio`、四项 `measured*Utilization`、`resourceEvidenceRef`、`measuredAt`；stateless 加 `perNodeRate`、`deployedNodes`；stateful 加 `measuredNormalTopologyCapacity`、`measuredDegradedTopologyCapacity`；gateway 加 `sharedCoreApiEdgeCapacity` |
| `playback` | `PB_CONCURRENT_SESSIONS`、`PB_BITRATE_BPS`、`PB_SESSIONS_PER_DAY`、`PB_MEAN_SECONDS`、`PB_CDN_CACHE_HIT_RATIO` |
| `mediaIngestStorage` | `REC_BITRATE_BPS`、`REC_CODEC`、`REC_SAMPLE_RATE_HZ`、`REC_BIT_DEPTH`、`REC_CHANNELS`、`REC_MEAN_VALIDATED_S`、`REC_PER_DAY`、`REC_ORIGINAL_VERSION_FACTOR`、`REC_RETENTION_DAYS`、`REC_DERIVED_BY_TYPE{preview,transcript,summary,export}`、`REC_LEGAL_HOLD_EXTRA_BYTES`、`REC_BACKUP_COPY_FACTOR`、`UPLOAD_CONCURRENT_PEAK`、`UPLOAD_PER_STREAM_WIRE_BPS`、`UPLOAD_MEASURED_PEAK_WIRE_BPS`、`UPLOAD_WIRE_OVERHEAD_RATIO`、`UPLOAD_COMBINED_MEASURED_WIRE_BPS` |
| `growthAndAsync` | `META_BY_TYPE{audit,outbox,callEvent,evidenceEvent,shareAccess}`、`DB_AUX_BY_KIND{index,wal,replication,backup}`、`QUEUE_ARRIVAL_TASKS_PS`、`QUEUE_SERVICE_TASKS_PS`、`QUEUE_BYTES_PER_TASK`、`QUEUE_INITIAL_BACKLOG_TASKS`、`AI_TASK_ARRIVALS_PS`、`AI_TASK_SERVICE_RATE_PS`、`AI_MEAN_SERVICE_S`、`AI_TASK_BYTES` |
| `spool` | `SPOOL_DISK_BYTES`、`SPOOL_FREE_BYTES`、`SPOOL_EXISTING_BYTES`、`SPOOL_NEW_PEAK_BYTES_PS`、`SPOOL_NEW_AVG_BYTES_PS`、`SPOOL_OUTAGE_S`、`SPOOL_HEADROOM_RATIO`、`SPOOL_RECOVERY_TOTAL_BYTES_PS`、`SPOOL_CONTINUING_BYTES_PS` |

模板顶层另有 `meta` 与 typed `asOf`，不计入 7 个容量组。未建立跨文件第二 registry；映射直接来自模板字段与本文件阅读路径。

### 3.2 公式、单位、snapshot 与基准

- **IVR**：`F-IVR-01` A 路 `callsPerSecond × seconds -> channels`；`F-IVR-02` B 路；`F-IVR-03` bridge `bridgePorts`；`F-IVR-04` SEPARATE inbound/outbound 或 SHARED A+B；`F-IVR-05` usable/shortfall/utilization（派生 utilization 可 >1 -> `NOT_MET`）；`F-IVR-05a` 线路裁决；`F-IVR-06` ringRatio；`F-IVR-07` mix sum。观测峰值只并列/裁决，不参与平均公式；`LINE_FIT_EVIDENCE_COMPLETE` 需 measured demand/peak/limit 与 measured/approved headroom。
- **API**：`F-API-01` 五类请求求和；`F-API-02` in-flight `requests` 与 DB 事务估计 `transactions`（`TXN_PER_REQUEST_NOT_MEASURED`）。
- **节点**：`F-NODE-01` stateless `ceil(demand/(perNodeRate×(1−headroom)))`；`F-NODE-02` N-1 同 headroom；`F-NODE-03` 四项资源 + ref + measuredAt；`F-NODE-04` stateful normal/degraded；`F-NODE-05` gateway 共享 `sessions`；`F-NODE-06` roleVerdict 最坏。角色分类固定；未知 role/不适字段分别 `UNKNOWN_ROLE`/`ROLE_FIELD_MISMATCH`；gateway 节点算术 `SHARED_CAPACITY_NODE_ARITHMETIC_FORBIDDEN`。
- **播放**：`F-PB-01` bits/s；`F-PB-02` bytes/day；`F-PB-03` 条件式 origin bytes/day；CDN/bearer 限制显式。
- **录音/存储**：`F-REC-01` 优先全声道 `REC_BITRATE_BPS`，否则仅 `PCM_LINEAR` 用 `Hz × bit × channels`；`F-REC-02` 以服务端校验时长；`F-REC-03` 日原始字节；`F-REC-04` retention × version；`F-REC-05` 四类派生各自 ratio/retention/version；`F-REC-06` legal hold 额外字节 + 独立 backup factor；`F-REC-07` 五类 metadata。
- **上传**：`F-UP-01` 日均 payload 下界；`F-UP-02` 显式 wire 峰值优先，否则 concurrent × per-stream（缺输入 -> `PEAK_INPUTS_MISSING`，不零短路）；`F-UP-03` overhead payload 估计（`ESTIMATE_ONLY`）；`F-UP-04` ongoing wire、catch-up payload bits/s 与 catch-up wire 估算分离；combined 仅显式实测。payload 不标 wire。
- **DB/队列/AI**：`F-DB-01` 四类 bytes 回显且 `dbSizingComplete=false`；`F-QUEUE-01` net/backlog/drain/bytes change；`F-AI-01` `loadRatio=arrivals/aggregateServiceRate`（`ratio`，>1 合法，带 `LOAD_RATIO_NOT_OCCUPANCY`）、`avgInServiceTasks=arrivals×meanServiceS`（`tasks`）、`aiBytesPerSecond` 独立。
- **Spool**：`F-SPOOL-01` 同一中断开始快照：`used=disk−free`（已含 existing）；`peakOccupancy=used+NEW_PEAK×outage`；`avgOccupancy=used+NEW_AVG×outage`；`recoveryBacklog=EXISTING+NEW_AVG×outage`（end-of-outage backlog，非 caller 输入）；`F-SPOOL-02` usable/fit/freeAfter/overflow 与 `CAPACITY_INCONSISTENT`；`F-SPOOL-03` `net=recoveryTotal−continuing`，drain 使用派生 backlog；spool 全部为 payload 基准。

单位枚举包含 `hertz`、`bit`、`transactionsPerSecond`、`opsPerSecond`、`messagesPerSecond`；`DiscreteCount` 仅用于整数字段，派生 `NonNegativeMeasure` 允许分数；`VersionFactor=1.2` 合法；retention days 使用非负 `NonNegativeMeasure`。模板/计算器无生产数值。

## 4. 合成标记与失败语义

- 模板全部叶 `value:null`、`provenance/approval:MISSING`；计算结果组应为 `INCOMPLETE`、0 `INVALID`、`synthetic:false`、四个 root 常量 `false`。
- fixtures 全部 `SYNTHETIC_TEST_ONLY`；成功算术仍标 `NOT_APPROVED_PRODUCTION_VALUE`，root `synthetic:true`。
- 缺失 metric `value:null`；显式 0 合法；纯乘法显式 0 因子短路缺失未使用因子并标 `ZERO_FACTOR_SHORT_CIRCUIT`；malformed 叶先校验后短路。
- root `cloudVerified`、`a3RiskClosed` 固定 `false`；调用方提供 `productionReady/procurementReady/approved/cloudVerified/a3RiskClosed` 任一字段 => `CALLER_CONTROL_FORBIDDEN`。
- 验证器不把 fixture `expect` 传给 `compute`；程序化 probe 使用真实 JS `NaN`/`+Infinity`/`-Infinity` 注入每个数值叶，断言 `NUMBER_NONFINITE`（未执行，见 §5）。

## 5. 实际命令、exit code 与输出

### 5.1 已运行（静态/哈希，可复核）

| 命令 | exit code | 关键输出 |
| --- | --- | --- |
| `jq empty deployment/capacity/inputs.template.json deployment/capacity/fixtures/s1-01-ivr-legs.json ... s1-09-provenance-approval.json` | 0 | 无输出（全部严格 JSON 可解析） |
| `jq 'keys' deployment/capacity/inputs.template.json` | 0 | `apiControlPlane, asOf, growthAndAsync, ivrLines, measuredNodes, mediaIngestStorage, meta, playback, spool`（7 组 + meta/asOf） |
| `jq '[.. \| objects \| select(has("value")) \| .value] \| map(select(. != null)) \| length' deployment/capacity/inputs.template.json` | 0 | `0`（无任何非 null 叶值） |
| `jq '[.. \| objects \| select(has("provenance")) \| .provenance] \| unique' ...` | 0 | `["MISSING"]` |
| `jq '[.. \| objects \| select(has("approval")) \| .approval] \| unique' ...` | 0 | `["MISSING"]` |
| `jq '.measuredNodes.MEASURED_ROLE \| length' ...` | 0 | `0` |
| `jq '.cases \| length'`（9 个 fixture） | 0 | `4, 6, 7, 8, 11, 16, 5, 17, 11`（共 85 case；数量仅为信息，不是成功判据） |
| `jq -s '[.[].stage] \| unique'`（9 个 fixture） | 0 | `["s1"]` |
| `sha256sum`（§2 文件） | 0 | 见 §2 哈希 |

### 5.2 未运行（权限阻断，不声称通过）

以下命令各自由主 agent 与独立 sub-agent 重试，均被权限系统以 `This command requires approval` 拒绝，进程未启动，故无 exit code：

```bash
node --check deployment/capacity/calc.mjs
node --check deployment/capacity/validate.mjs
node deployment/capacity/validate.mjs --stage s1
node deployment/capacity/validate.mjs --template deployment/capacity/inputs.template.json --stage s1
node deployment/capacity/validate.mjs --stage s1 --fixtures deployment/capacity/fixtures
```

因此以下行为断言**未验证**：
- 模板恰好 7 组、全部 `INCOMPLETE`、0 `INVALID`、`productionReady/procurementReady/cloudVerified/a3RiskClosed=false`、`synthetic=false`；
- 每个 fixture 声明的 behavior/status/value/unit/code/dimension；
- 程序化 NaN/±Infinity probe 与 `NUMBER_NONFINITE`；
- 失败时非 0 退出。

请 controller 在允许 node 的环境中执行上述命令；若任一失败，本 slice 必须回到 implementation correction，不得视为完成。

## 6. 目标对齐与无漂移

- 覆盖计划 S1 的 7 个容量组、所有 S1 公式（含 `F-API-*`、`F-NODE-*`）、单位/scalarKind、snapshot/end-of-outage backlog、payload/wire 基准、AI load ratio vs Little 平均在服、缺失/零/非法/partial/批准/合成标记。
- 未实现 S2 `backupRecovery`、`capabilityEvidence`、`F-BACKUP-*`、`F-CAP-*`、部署/恢复门禁或 README 链接。
- 未新增公共契约、endpoint、表、Problem code、协议事件、角色、SKU/region/AZ/价格/保留期/SLO/RPO/RTO 或生产默认值。
- 30 秒仅为既有任务 TTL 产品默认，本 slice 未把它当请求超时、授权窗口或 spool 保留期。
- `docs/18` 只写 §1-§6；S2 内容未写。

## 7. 残留下沉（保持原 owner/destination）

| 残留/输入 | Owner / Destination |
| --- | --- |
| Q7 线路/接入号/录音格式/缓冲/时钟、上传并发与网络实测 | Telephony/IVR；线路联调 + A4 input |
| Q8 时长容差/SLO/超时/独立峰值观测窗口 | IVR/Backend/QA；容量与验收 |
| Q11 COS class/object-lock/媒体保留 | Infrastructure/Security；存储/容量 |
| Q13 审计/Outbox 保留、DLQ、volume、queue/AI 实测 | Operations/Backend；runbook/可观测性 |
| Q17/Q23 DB 角色/ownership、DB 正常/降级拓扑测量 | Platform/Database/Security；部署准备 |
| Q25 授权窗口/时钟偏差 | IVR/Entitlement/Backend；配置与验收（非 30s TTL） |
| Q2/Q3 KMS 与法律保留/删除、legal hold 额外字节口径 | Security/Infrastructure、Legal/Security |
| CV-1..5 / region/AZ/SKU | Infrastructure/Security；能力验证证据（人工，S2） |
| `R-A3-COS-CAPABILITY` / `R-A3-NO-WORM` / `R-A3-REGION-SKU` / `R-A3-MEDIA-GATEWAY-TRANSPORT` / `R-A3-AUDIT-VOLUME` / `R-A3-KMS-CAPABILITY` / `R-A3-KEY-DESTRUCTION` / `R-A3-LEGAL-RETENTION` | 原 A3 owner/destination，A4 不关闭 |
| `R-A2-UPLOAD-OBSERVE` / `R-A2-EVIDENCE-HANDOFF` / `R-A2-MULTIPLE-RECORDING` / `R-A2-HEARTBEAT-VOLUME` | IVR/Evidence/Telephony/Operations，按原 destination |

## 8. 诚实限制

- 未运行真实线路、SIP/CDR、IVR 媒体引擎、PostgreSQL/Redis/RabbitMQ、COS/KMS/Secret Manager/CAM、CDN/LB/media gateway、云审计或压测；无 benchmark、账单、region/SKU、恢复实测或云能力证据。
- fixtures 只证明算术、维度、失败关闭与输出契约（在 node 验证执行后）；不是生产验收、容量证明、云能力验证或恢复能力证明。
- spool 只消费 payload 实测/合成输入，不验证真实磁盘或恢复吞吐；catch-up wire 仅是 overhead 估算；AI `loadRatio` 不是占用率或平均在服任务数。
- 数据库 HA/IOPS、queue/AI service rate、每节点吞吐/资源利用等高级 sizing 仍是 owned measurement/deferred input；计算器只消费调用方提供的有限值。
- 本报告不声称 Node 测试已跑、模板/fixture 已通过或 probe 已通过。

## 9. 完成/阻塞声明

- 允许文件均已写入；S1 文档、计算器、验证器、模板、9 个行为分组 fixture、本报告存在。
- **阻塞项**：本会话无法执行 node，运行时验证未完成。除该环境权限外，未发现计划矛盾或越界需求；未自行扩大范围。
- 未 stage、commit、push、PR，未进入 code review/re-review/S2/云操作。
- 下一步由 controller 在允许 node 的环境执行 §5.2 命令；全部通过后再进入 S1 review。

## 10. Bounded S1 fix 1（2026-10-26）

### 10.1 范围与观测失败

仅处理 controller 报告的三个 S1 失败；未进入 S2、未改验证器、未改公式、未改其他 fixture。

| # | 失败 case | 根因 |
| --- | --- | --- |
| 1 | `s1-03-upload-playback/catch-up-drain-impossible` | spool 快照矛盾：`FREE=DISK` ⇒ `used=0`，但 `EXISTING=10000>0`，先触发 `CAPACITY_INCONSISTENT`，未进入预期的 `DRAIN_IMPOSSIBLE`。 |
| 2 | `s1-04-spool/positive-backlog-net-zero` | 同类快照矛盾：`DISK=FREE=2000` ⇒ `used=0`，但 `EXISTING=1000>0`，先触发 `CAPACITY_INCONSISTENT`。 |
| 3 | `s1-04-spool/capacity-inconsistent-existing` | `spool.used` 在 `CAPACITY_INCONSISTENT` 时返回了派生的 `disk-free` 数值（200），而期望 `INVALID` 且 `value=null`；依赖的 `peakOccupancy` 已因 `derivedOperand` 为 null。 |

### 10.2 变更（仅允许路径）

| 文件 | 变更 |
| --- | --- |
| `deployment/capacity/fixtures/s1-03-upload-playback.json` | `catch-up-drain-impossible.SPOOL_FREE_BYTES`：`1000000 -> 900000`。快照变为 `used=100000`，满足 `existing=10000 <= used`；不变量仍为 `recoveryBacklog=10000`、`net=0`、`drain=NOT_MET/DRAIN_IMPOSSIBLE`、catch-up `NOT_MET/0`。 |
| `deployment/capacity/fixtures/s1-04-spool.json` | `positive-backlog-net-zero.SPOOL_FREE_BYTES`：`2000 -> 1000`。快照变为 `used=1000`，`existing=1000` 取等号（实现只拒绝严格大于）；不变量仍为 `recoveryBacklog=1000`、`net=0`、`drain=NOT_MET/DRAIN_IMPOSSIBLE`、root `NOT_MET`。 |
| `deployment/capacity/calc.mjs` | `computeSpool` 中 `existing > disk-free` 分支的 `used` 由 `value` 改为 `value: null`（`CAPACITY_INCONSISTENT` 即 `INVALID`，不再携带推导数值）。公式、阈值、错误 code、验证器均未改。 |

- 未修改 `positive-backlog-net-negative`（不在三个观测失败内）；其输入仍为 `FREE=DISK=2000` 且 `EXISTING=1000`，见 §10.5 残留风险。
- 变更后文件哈希（`sha256sum`，exit 0）：

| 文件 | SHA-256 |
| --- | --- |
| `deployment/capacity/calc.mjs` | `809a3b380f8cc5371fac1a87ced1a4fadebcf1b933caca23e282c904ee3dcd18` |
| `deployment/capacity/fixtures/s1-03-upload-playback.json` | `335c20074f5bc87ca962d8dc01053b6ddb6b3e9eb6a6ddeddf020f094b648808` |
| `deployment/capacity/fixtures/s1-04-spool.json` | `294a46417f4a64aef1afac1e5a386f2038bd1bf83b904c9b04b0e7af907747a7` |

§2 表中上述三个文件的哈希为 fix 前快照，以本节为准。

### 10.3 命令与结果

| 命令 | exit code | 结果 |
| --- | --- | --- |
| `jq empty deployment/capacity/fixtures/s1-03-upload-playback.json deployment/capacity/fixtures/s1-04-spool.json` | 0 | 无输出（严格 JSON 可解析） |
| `sha256sum deployment/capacity/calc.mjs deployment/capacity/fixtures/s1-03-upload-playback.json deployment/capacity/fixtures/s1-04-spool.json` | 0 | 见 §10.2 |
| `wc -l deployment/capacity/calc.mjs deployment/capacity/fixtures/s1-03-upload-playback.json deployment/capacity/fixtures/s1-04-spool.json` | 0 | `3818 / 222 / 226` |
| `node --check deployment/capacity/calc.mjs` | 未执行 | 权限网关：`This command requires approval`；无 exit code |
| `node --check deployment/capacity/validate.mjs` | 未执行 | 权限网关：`This command requires approval`；无 exit code |
| `node deployment/capacity/validate.mjs --stage s1` | 未执行 | 权限网关：`This command requires approval`（同一会话重试 3 次）；无 exit code |

**因此本 fix 不声称 `node --check` 或 S1 validator 已通过。** 三个 case 的预期输出仅由代码静态推演（§10.4），必须由 controller 在允许 node 的环境复核。

### 10.4 静态推演（非运行时证据）

| case | 推演关键值 | 期望 |
| --- | --- | --- |
| `catch-up-drain-impossible` | `used=100000`、`existing=10000<=used`（无 error）、`recoveryBacklog=10000`、`net=1000-1000=0`、`drain=NOT_MET null DRAIN_IMPOSSIBLE`、catch-up payload/wire `NOT_MET 0` | 与 `expect` 一致 |
| `positive-backlog-net-zero` | `used=1000`、`existing=1000`（取等号合法）、`recoveryBacklog=1000`、`net=0`、`drain=NOT_MET null DRAIN_IMPOSSIBLE`；spool 最差状态 `NOT_MET` ⇒ root `NOT_MET` | 与 `expect` 一致 |
| `capacity-inconsistent-existing` | `free=800<=disk=1000`、`used=disk-free=200`、`actualExisting=300>200` ⇒ `CAPACITY_INCONSISTENT @ spool.SPOOL_EXISTING_BYTES`；`spool.used=INVALID value=null`；`peakOccupancy` 经 `derivedOperand` 传播为 `INVALID value=null`；root INVALID | 与 `expect` 一致 |

### 10.5 残留风险

- **运行时未验证**：本会话 `node` 全被权限网关阻断；`node --check` 与 validator 均未运行，不能排除语法/断言级失败。controller 必须执行 §10.3 的后三条命令。
- **同类快照矛盾仍在 `positive-backlog-net-negative`**：其输入 `DISK=FREE=2000`、`EXISTING=1000` 仍会触发 `CAPACITY_INCONSISTENT`（`used=INVALID`），从而可能使 root 为 `INVALID` 并产生一个非预期 error。该 case 未断言 `root`、`errors` 或 `used`，且不在本次三个观测失败内，故按“只修三个”要求未改；若 validator 未来增加全局 error/root 断言，需要单独判定。
- `capacity-inconsistent-existing` 中 `recoveryBacklog`/`drainSeconds` 仍可 COMPLETE（不依赖 `used`），与 `used` 同案出现 INVALID；fixture 未断言这些值，语义是否需进一步收紧属 review 决策，未在本 bounded fix 内改动。
- 未新增/删除依赖、未改公共契约；未 commit/push/PR；未进入 S2。

## 11. Bounded S1 fix 2（2026-10-26，review 发现 R2-02/R2-03）

### 11.1 范围

仅采纳并修复独立评审的两个发现：
- **F-A4-S1-R2-02**：`s1-04-spool/positive-backlog-net-negative` 的 spool 快照矛盾（`DISK=FREE=2000` ⇒ `used=0`，而 `EXISTING=1000>0`）会额外触发 `CAPACITY_INCONSISTENT`，使本应 `NOT_MET/DRAIN_IMPOSSIBLE` 的 case 实际 root 为 `INVALID`。
- **F-A4-S1-R2-03**：measured-node 的 `nMinusOne`、stateful `makeFit`、gateway `sharedFit` 在叶同时 `missing=true` 且 `invalid=true` 时先命中 missing 分支返回 `NOT_VERIFIED`，与 `STATUS_RANK`（INVALID=4 > NOT_VERIFIED=1）及同函数 `requiredNodes`（invalid 优先）不一致。

未采纳/未改动：F-A4-S1-R2-01（运行时验证缺失，需 controller 执行命令）、F-A4-S1-R2-04（评审判定非缺陷）。未改公式、公共 DTO、验证器、S2、docs/18、template、其他组、原型、截图、A0-A3 或控制文档。

### 11.2 变更（仅允许路径）

| 文件 | 变更 |
| --- | --- |
| `deployment/capacity/fixtures/s1-04-spool.json` | `positive-backlog-net-negative.SPOOL_FREE_BYTES`：`2000 -> 1000`，使 `used=1000`、`existing=1000` 取等号（实现只拒绝严格大于），保持 `recoveryBacklog=1000`、`net=-50`、`drain=NOT_MET/DRAIN_IMPOSSIBLE`。expect 新增 `root.status=NOT_MET`、`spool.used=1000`、`spool.recoveryBacklog=1000`、`errorsAbsent=["CAPACITY_INCONSISTENT"]`、`noInvalidMetrics=true`；behavior 文案同步说明快照一致。 |
| `deployment/capacity/calc.mjs` | `nMinusOne`、stateful `makeFit`、gateway `sharedFit` 改为 invalid 分支优先于 missing；invalid 分支的 `missing` 由 `[]` 改为 `collectMissing(...)`，与 `requiredNodes` 一致。无公式/阈值/error code 变化。 |
| `deployment/capacity/fixtures/s1-06-node-api.json` | 新增一个 case `invalid-dominates-missing-in-node-fit`：三个角色各含一个非对象叶（同时 missing+invalid），断言 `core-api.nMinusOne`、`postgres.normalFit`、`media-gateway.sharedFit` 均为 `INVALID`（含 `invalidInclude` 与 `TYPE_INVALID`），并断言对应 `TYPE_INVALID` error path 及 root `INVALID`。 |
| `docs/gateflow/a4-production-capacity-deployment-spec-s1-implementation.md` | 本节。 |

变更后文件哈希（`sha256sum`，exit 0）：

| 文件 | SHA-256 |
| --- | --- |
| `deployment/capacity/calc.mjs` | `4b3e09c96ebff65a21576914072e1fe7e9d2e49f7639b0c57014da477d8e53ff` |
| `deployment/capacity/fixtures/s1-04-spool.json` | `a9001384c2fa491cdd76a483e5f51bea606d18729303febc03269e61dfcc8bb1` |
| `deployment/capacity/fixtures/s1-06-node-api.json` | `42c19e5a1a45ebc6d64b2ec8541a0d4e42b8ec038d3fd1241ba6b3122a436473` |

§2 与 §10.2 的哈希为更早快照，以上述三行为准。

### 11.3 命令与结果

| 命令 | exit code | 结果 |
| --- | --- | --- |
| `jq empty deployment/capacity/fixtures/s1-04-spool.json deployment/capacity/fixtures/s1-06-node-api.json` | 0 | 无输出（严格 JSON 可解析） |
| `sha256sum ...`（§11.2 三文件） | 0 | 见 §11.2 |
| `wc -l`（calc / s1-04 / s1-06） | 0 | `3818 / 231 / 546` |
| `node --check deployment/capacity/calc.mjs` | 未执行 | 权限网关：`This command requires approval`；无 exit code |
| `node --check deployment/capacity/validate.mjs` | 未执行 | 权限网关：`This command requires approval`；无 exit code |
| `node deployment/capacity/validate.mjs --stage s1` | 未执行 | 权限网关：`This command requires approval`；无 exit code |

**本 fix 不声称 node 语法检查或 S1 validator 已通过。** 必须由 controller 在允许 node 的环境执行上述三条命令。

### 11.4 静态推演（非运行时证据）

| 对象 | 推演 | 期望 |
| --- | --- | --- |
| `positive-backlog-net-negative` | `used=2000-1000=1000`；`existing=1000` 取等号合法；`recoveryBacklog=1000`；`net=50-100=-50`；`drain=NOT_MET null DRAIN_IMPOSSIBLE`；无 error ⇒ root `NOT_MET`，无 `CAPACITY_INCONSISTENT` | 与新增 expect 一致 |
| `invalid-dominates-missing-in-node-fit` | 非对象叶 `invalidate(TYPE_INVALID)` 同时置 `missing=true`；三处分支 invalid 优先 ⇒ `INVALID`（value null，invalid 含叶路径，reasons `TYPE_INVALID`）；每叶独立 error path；root `INVALID` | 与 expect 一致 |
| 原 missing-only 路径 | missing 分支仍返回 `NOT_VERIFIED/MEASUREMENT_REQUIRED`（invalid 为假时不进入 invalid 分支） | 不受影响 |

### 11.5 残留风险

- **运行时仍未验证**：本会话 `node` 命令均被权限网关拒绝（进程未启动，无 exit code）。以上均为静态推演。
- 未改 F-A4-S1-R2-04 揭示的 `capacity-inconsistent-existing` 中 `used=INVALID` 与 `recoveryBacklog/drainSeconds=COMPLETE` 并存语义（评审判定非缺陷，且属 plan 裁决范围）。
- 未 commit/push/PR；未进入 S2。
