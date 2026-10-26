# A4 S1 实施独立评审（MiMo 2.6 Flash）

> Task label：`a4-capacity-s1-review-mimo26flash-20261026-02`（retry；前次 orchestration 会话空输出，未沿用任何先前结论）
>
> Reviewer：MiMo 2.6 Flash（independent）
>
> 评审对象：A4 S1 实施切片（容量算术、单位与未知传播）
>
> 评审基准：
> - `docs/gateflow/a4-production-capacity-deployment-spec-plan.md`（accepted plan `84233c6`）
> - `docs/18-production-capacity-and-deployment-spec.md` §1–§6
> - `deployment/capacity/` 下允许的 S1 文件（`calc.mjs`、`validate.mjs`、`inputs.template.json`、9 个 `s1-*` fixture）
> - `docs/gateflow/a4-production-capacity-deployment-spec-s1-implementation.md`（含 bounded S1 fix 1）
>
> 约束遵守：未编辑任何实现文件、fixture、docs/18、控制文档、原型或截图；仅写入本评审文件；未 commit/push/PR；未进入 S2。

## 1. 结论（必须先读）

**Pass-with-risks。**

- 静态评审未发现 P0/P1 正确性缺陷：S1 公式（F-IVR/F-API/F-NODE/F-PB/F-REC/F-UP/F-DB/F-QUEUE/F-AI/F-SPOOL）、单位与 scalarKind、显式 0 vs 缺失、worst/partial 传播、spool 快照与 end-of-outage backlog、payload/wire 基准分离、AI loadRatio vs Little 在服、provenance/approval 失败关闭、CALLER_CONTROL_FORBIDDEN、S1 边界（无 S2 漂移）均与 plan/§1–§6 对齐。
- **主要风险**：三个强制运行时命令在本评审会话中同样被权限网关阻断（见 §3），行为断言、程序化 NaN/±Infinity probe 与失败退出码均**未获运行时证据**。实现报告已如实披露；本评审同样不声称其通过。
- 另有 2 个 P2/P3 发现（§4）：fixture 残留快照矛盾、measured-nodes 判定分支的 missing-before-invalid 顺序。

完成条件意义上的“S1 可接受”仍需 controller 在允许 node 的环境执行 §3 命令并全绿；在此之前本评审只覆盖静态正确性。

## 2. 评审范围与方法

- 通读 plan 全文、docs/18 §1–§6、`calc.mjs`（3818 行）、`validate.mjs`（535 行）、`inputs.template.json` 结构（jq 复核）、9 个 fixture（s1-01..s1-09，共 85 case）与实施报告（含 §10 bounded fix 1）。
- 方法：以 fixture `expect` 为探针，对 `calc.mjs` 相应代码路径做手工静态推演（值、unit、status、reasons/limitations/missing、error code/path），交叉核对公式表与 §4.4 零/缺失/非法语义。
- 对实施报告 §10 三个修复 case（`catch-up-drain-impossible`、`positive-backlog-net-zero`、`capacity-inconsistent-existing`）逐项复核修复后输入与 `computeSpool` 语义。
- 尝试运行三个强制命令（见 §3）；均失败于环境权限，非代码失败。

## 3. 命令执行与 exit status（如实报告，不声称未运行检查）

### 3.1 本评审会话实际执行

| 命令 | 结果 | exit code |
| --- | --- | --- |
| `node --check deployment/capacity/calc.mjs` | `This command requires approval`（进程未启动） | 无 |
| `node --check deployment/capacity/validate.mjs` | 同上（见 §3.2 重试） | 无 |
| `node deployment/capacity/validate.mjs --stage s1` | 同上（见 §3.2 重试） | 无 |

权限网关拒绝发生在进程启动之前，因此**没有 exit code、没有 stdout/stderr 行为输出**。`node --version` 在先前会话返回 `v22.23.4`（node 二进制存在，执行被权限拦截）。与实施报告 §5.2/§10.3 的披露一致（作者侧亦被同一网关阻断）。

**因此本评审不声称**：模板 7 组行为断言、85 个 fixture case 的运行时断言、NaN/±Infinity 程序化 probe、失败时非 0 退出——任何一项已通过。

### 3.2 已运行的静态/哈希类检查（本评审独立复核）

| 命令 | exit code | 输出 |
| --- | --- | --- |
| `jq 'keys' deployment/capacity/inputs.template.json` | 0 | 7 组 + `meta`/`asOf`（`apiControlPlane, asOf, growthAndAsync, ivrLines, measuredNodes, mediaIngestStorage, meta, playback, spool`） |
| `jq '[.. \| objects \| select(has("value")) \| .value] \| map(select(. != null)) \| length'`（template） | 0 | `0`（无非 null 叶） |
| `jq '[.. \| objects \| select(has("provenance")) \| .provenance] \| unique'`（template） | 0 | `["MISSING"]` |
| `jq '.measuredNodes.MEASURED_ROLE \| length'`（template） | 0 | `0` |
| `jq -s '[.[].stage] \| unique'`（9 fixtures） | 0 | `["s1"]` |

fixture JSON 语法经 Read 逐文件解析确认为严格 JSON；case 数量 4/6/7/8/11/16/5/17/11（共 85，信息性）。

## 4. 发现（P0–P3）

### F-A4-S1-R2-01（P2，verification gap）运行时验证整体未执行

- **证据**：§3.1；实施报告 §5.2、§10.3 同样记录 `This command requires approval`；memory `env-permission-gate.md` 记录 2026-10-26 多会话对 node/npm/python 的同类拦截。
- **影响**：plan §6.2 定义的验证命令是 S1 完成条件的一部分；模板/fixture/probe 的行为正确性目前只有静态推演支撑。静态推演覆盖了全部 85 case 的关键期望值（见 §5），但不能替代运行时证据。
- **所需修复**：controller 在允许 node 的环境执行并记录：
  ```bash
  node --check deployment/capacity/calc.mjs
  node --check deployment/capacity/validate.mjs
  node deployment/capacity/validate.mjs --stage s1
  ```
  任一失败则 S1 必须回到 implementation correction，本 Pass-with-risks 结论不得被解读为运行时通过。

### F-A4-S1-R2-02（P2，fixture residual）`positive-backlog-net-negative` 仍含矛盾 spool 快照

- **证据**：`deployment/capacity/fixtures/s1-04-spool.json:129-131`：`SPOOL_DISK_BYTES=2000`、`SPOOL_FREE_BYTES=2000` ⇒ `used=0`，但 `SPOOL_EXISTING_BYTES=1000>0`。`calc.mjs:3518-3520` 在 `existing.usable && existing > disk-free` 时 `addError(CAPACITY_INCONSISTENT @ spool.SPOOL_EXISTING_BYTES)` 且 `spool.used=INVALID value:null`。
- **为何 case 仍通过**：该 case 只断言 `recoveryNetPayloadBytesPerSecond` 与 `drainSeconds`（fixture:140-145）；`recoveryBacklog`（`calc.mjs:3587-3593`）不依赖 `used`，`existing=1000`、`newAvg×outage=0` ⇒ COMPLETE 1000；`net=50-100=-50` ⇒ drain `NOT_MET/DRAIN_IMPOSSIBLE`，与 expect 一致。但 root 会因该未预期 error 变为 `INVALID`（`calc.mjs:3765`：`errors.length>0 ⇒ INVALID`），与 case 行为文案“cannot drain”（暗示 NOT_MET 语义）不一致；case 未断言 root/errors，故被掩盖。
- **与 bounded fix 1 的关系**：同类矛盾已在 `positive-backlog-net-zero`（FREE `2000→1000`）与 `catch-up-drain-impossible`（FREE `1000000→900000`）修复；本 case 不在 controller 报告的三个失败内，作者按“只修三个”保留并在 §10.5 披露。本评审确认该残留仍然存在。
- **对抗性影响**：若 validator 未来对未显式声明的 case 增加全局 root/errors 断言，本 case 将以 `INVALID` root 失败；当前依赖断言窄集通过，属脆弱性。
- **所需修复**：与 fix 1 同法修正输入（例如 `SPOOL_FREE_BYTES: 2000→1000`，使 `used=1000=existing`，保持 `recoveryBacklog=1000`、`net=-50`、`DRAIN_IMPOSSIBLE` 不变；或 `EXISTING→0` 并同步 backlog 期望），并记录新哈希；不改 `calc.mjs`。

### F-A4-S1-R2-03（P3，status ordering）measured-nodes 判定分支 missing 先于 invalid

- **证据**：`calc.mjs:3130`（stateless `nMinusOne`）先查 `.missing` 再查 `.invalid`（3140）；同样模式在 stateful `makeFit`（`calc.mjs:3205`/3216）与 gateway `sharedFit`（`calc.mjs:3293`/3303）。对照：同函数 `requiredNodes` 在 `calc.mjs:3061` **先查 invalid**。
- **对抗性输入**：叶可同时 `missing=true` 且 `invalid=true`：
  1. 非 plain-object 叶：`calc.mjs:316-319` `invalidate(TYPE_INVALID)` 后置 `missing=true`；
  2. `value:null` + `approval:"APPROVED"`：`calc.mjs:390-394` `APPROVAL_INVALID`，且 `calc.mjs:368-369` 置 `missing=true`。
  此时 `nMinusOne`/`makeFit`/`sharedFit` 返回 `NOT_VERIFIED/MEASUREMENT_REQUIRED` 而非 `INVALID`。
- **影响边界（fail-closed 仍成立）**：ctx.error 已在 `invalidate` 时写入；`calc.mjs:3755-3762` 将所属 group 强制 `INVALID`；`calc.mjs:3765` root 亦为 `INVALID`。故 group/root 不放宽；缺陷限于**单指标 status 标签**与 `STATUS_RANK` 严重度排序不一致（NOT_VERIFIED=1 < INVALID=4）。
- **所需修复（非阻塞，建议 bounded correction）**：将三处分支改为 invalid 优先（与 `requiredNodes` 及 STATUS_RANK 对齐），或在 missing 分支内对 `collectMissing` 中同时 invalid 的 rec 提升为 INVALID。不改公式与 error code。无 fixture 直接覆盖该组合，修复后可加一个 case。

### F-A4-S1-R2-04（P3，语义提示，非缺陷）`capacity-inconsistent-existing` 中 COMPLETE 与 INVALID 并存

- **证据**：`calc.mjs:3518-3520` 使 `spool.used=INVALID`；`recoveryBacklog`（3587-3593）仅依赖 `existing` 叶（叶级校验通过，矛盾是跨字段的）与 `outageProducedAvg`，故可为 `COMPLETE`；`drainSeconds` 进而可 COMPLETE。实施报告 §10.5 已披露并留给 review 决策。
- **判定**：按 F-SPOOL-01 字面公式（backlog = EXISTING + NEW_AVG×outage，不经过 `used`），实现是公式忠实的；plan 未规定 `CAPACITY_INCONSISTENT` 必须使 existing 对 backlog 不可用。**不计为缺陷**，仅记录：同一 case 内 `used=INVALID` 与 `recoveryBacklog/drainSeconds=COMPLETE` 并存，调用方读取时应以 group/root 的 INVALID 为准。若产品语义要求“快照矛盾 ⇒ 全部派生不可信”，需在 S2 前由 plan 侧补充裁决，不在 S1 实施内擅自收紧。

### 未发现的问题（明确核对通过的对抗面）

- 显式 0 vs 缺失：零因子短路保留未使用因子于 `missing[]`（s1-05 `ai-arrivals-zero`、s1-03 `zero-backlog-catch-up`、s1-07 `explicit-zero-legal` 推演一致）；malformed 叶旁路零短路仍 INVALID（s1-05 `ai-malformed-beside-zero-arrivals`、s1-07/s1-08 `malformed-beside-zero-factor`；`calc.mjs` multiply 路径先 validateLeaf）。
- 单位：`hertz`/`bit`/`channels`/`transactionsPerSecond`/`opsPerSecond`/`messagesPerSecond`/`requestsPerSecond`/`tasksPerSecond`/`bytesPerTask`/`countPerDay`/`days` 均在 fixture 中以正确 unit 断言；UNIT_MISMATCH、DiscreteCount 分数（COUNT_NOT_INTEGER）与负值优先 NEGATIVE_VALUE（s1-08）与 `validateKind` 一致。
- VersionFactor 1.2 合法、0.5 `RATIO_OUT_OF_RANGE`（s1-02/s1-08；`calc.mjs:280-283`）。
- spool N-01：`used=disk−free`（含 existing）、peak/avg 各加一次 outage 产量、`recoveryBacklog=EXISTING+NEW_AVG×outage`、F-UP-04 消费派生 backlog（`calc.mjs:2193-2260`）；payload 与 wire 估计分离（wire = payload/(1−overhead)，ESTIMATE_ONLY）。
- AI：`loadRatio`（ratio，>1 合法，LOAD_RATIO_NOT_OCCUPANCY）与 `avgInServiceTasks=arrivals×meanService`（tasks）及 `bytesPerSecond` 独立（s1-05 全 case 推演一致）。
- measured-nodes：stateless ceil + N-1、stateful normal/degraded、gateway 共享容量、`SHARED_CAPACITY_NODE_ARITHMETIC_FORBIDDEN`、`ROLE_FIELD_MISMATCH`、`FIT_EVIDENCE_COMPLETE` 需全 MEASURED（s1-06 推演一致；ESTIMATE demand ⇒ ARITHMETIC_ONLY）。
- provenance/approval：ESTIMATE+APPROVED ⇒ APPROVAL_INVALID；MEASURED 缺 source/owner ⇒ PROVENANCE_INVALID；synthetic 需 synthetic approval；meta 批准不批准叶（s1-09；`calc.mjs:375-398`）。
- 失败关闭常量：`productionReady/procurementReady/cloudVerified/a3RiskClosed` 恒 false；五控制键 CALLER_CONTROL_FORBIDDEN（`calc.mjs:3723-3725`；s1-09 两 case）。
- S1 边界：`validate.mjs` 拒绝 `--stage s2`；`calc.mjs` 无 F-BACKUP/F-CAP；docs/18 仅 §1–§6；无新公共契约/endpoint/表/Problem code/SKU/region/价格/SLO/RPO/RTO。
- bounded fix 1 静态复核：三个 case 的修复输入与 `computeSpool` 语义一致（`catch-up-drain-impossible`：used=100000、existing=10000≤used、backlog=10000、net=0、DRAIN_IMPOSSIBLE、catch-up NOT_MET 0；`positive-backlog-net-zero`：used=1000、existing=1000 取等合法、root NOT_MET；`capacity-inconsistent-existing`：used INVALID null、peakOccupancy 经 derivedOperand 传播 INVALID null）。

## 5. 关键静态推演摘要（非运行时证据）

| 代表 case | 推演 | 与 expect |
| --- | --- | --- |
| s1-01 `fractional-legs` | 0.5×7.4=3.7 channels；synthetic ⇒ verdict NOT_VERIFIED/ARITHMETIC_ONLY | 一致 |
| s1-01 `shared-mode-utilization-over-one` | shared=5.6、usable=4、utilization=1.4、shortfall=1.6、NOT_MET/TARGET_NOT_MET | 一致 |
| s1-02 `full-channel-bitrate-retention-version` | 64000×60×100×30×1.2=1728000000；+legalHold 1000=1728001000；×2=3456002000 | 一致 |
| s1-03 `upload-layers-and-playback` | backlog=10000000+1000×100=10100000；net=4000；drain=2525；catchUpBits=32000；wire=32000/0.9≈35555.556；PB 50×500000=25e6；1000×60×500000/8=3.75e9；origin=3.75e8 | 一致 |
| s1-03 `catch-up-drain-impossible`（fix 后） | used=100000；backlog=10000；net=0；DRAIN_IMPOSSIBLE；catch-up NOT_MET 0 | 一致 |
| s1-05 `metadata-db-queue-ai` | 30000/0/1000/…；net=−2；drain=1500；loadRatio=0.8；avgInService=40；aiBps=100000 | 一致 |
| s1-06 `stateless-required-and-nminus-one` | ceil(10/(20×0.9))=1；N-1 cap=36≥10 ⇒ true；synthetic ⇒ ARITHMETIC_ONLY | 一致 |
| s1-06 `stateful-normal-and-degraded` | pg normal 120×0.9=108≥100 true；degraded 90×0.9=81<100 NOT_MET | 一致 |
| s1-06 `gateway-shared-capacity` | 100×0.9=90≥80 true；SHARED_MEDIA_GATEWAY；synthetic ⇒ ARITHMETIC_ONLY | 一致 |
| s1-04 `positive-backlog-net-negative` | net=−50、DRAIN_IMPOSSIBLE 一致；但快照矛盾触发额外 CAPACITY_INCONSISTENT（F-A4-S1-R2-02） | 部分（见发现） |

## 6. 对 plan 完成条件的符合性判定

| 条件 | 判定 |
| --- | --- |
| 7 组模板、typed 叶、MISSING provenance/approval | 满足（jq 静态复核） |
| S1 公式全覆盖、单位/scalarKind、零/缺失/非法/partial | 静态满足；运行时未验证（F-A4-S1-R2-01） |
| spool 快照与 end-of-outage backlog、payload/wire 分离 | 静态满足；fix 1 三 case 静态一致 |
| AI loadRatio vs Little、API/nodes 行为 | 静态满足 |
| provenance/approval 失败关闭、控制键禁止、常量 false | 静态满足 |
| 非有限数 probe | 代码存在（validate.mjs），**未运行** |
| 验证命令 exit 0 | **未满足**（环境权限阻断；非代码失败证据） |
| S1 边界无 S2 漂移 | 满足 |

## 7. 结论与下一步

**Pass-with-risks。**

- 静态正确性：通过。未发现需阻塞实施正确性的 P0/P1 缺陷。
- 运行时验证：缺失（F-A4-S1-R2-01），必须由 controller 补跑 §3.1 三命令；全绿前不得将本结论当作 S1 完成或可提交依据。
- 建议在进入 code review 关闭前完成：
  1. （P2）修正 `s1-04-spool.json` `positive-backlog-net-negative` 快照矛盾并更新哈希（F-A4-S1-R2-02）；
  2. （P3，可选 bounded）`nMinusOne`/`makeFit`/`sharedFit` 改为 invalid 优先（F-A4-S1-R2-03）。
- 未编辑任何实现/fixture/控制文档；未 commit/push/PR；未进入 S2。
