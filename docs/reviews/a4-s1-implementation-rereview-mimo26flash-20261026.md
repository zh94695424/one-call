# A4 S1 实施复评（MiMo 2.6 Flash）— bounded fix 2 后 R2-02/R2-03 关闭核验

> Task label：`a4-capacity-s1-rereview-mimo26flash-20261026`
>
> Reviewer：MiMo 2.6 Flash（independent re-review）
>
> 评审对象：仅 F-A4-S1-R2-02 与 F-A4-S1-R2-03 的关闭正确性（bounded S1 fix 2）
>
> 评审基准：
> - `docs/reviews/a4-s1-implementation-review-mimo26flash-20261026.md`（原评审）
> - `docs/gateflow/a4-production-capacity-deployment-spec-s1-implementation.md` §11（fix 2 报告）
> - `deployment/capacity/calc.mjs`、`fixtures/s1-04-spool.json`、`fixtures/s1-06-node-api.json` 当前内容
> - controller 提供的运行时证据：`node --check`（calc/validate）与 `node deployment/capacity/validate.mjs --stage s1` 全绿，**86/86 cases passed、459 nonfinite probes、exit 0**
>
> 约束遵守：仅写入本评审文件；未编辑实现、fixture、其他 docs、控制文档、原型或截图；未 commit/push/PR；未进入 S2。

## 1. 结论（必须先读）

**Pass。**

- **F-A4-S1-R2-02 已正确关闭**：`positive-backlog-net-negative` 快照改为 `DISK=2000 / FREE=1000 / EXISTING=1000`（`used=1000`，取等号合法），静态推演与新增 expect（root `NOT_MET`、`used=1000`、`errorsAbsent=["CAPACITY_INCONSISTENT"]`、`noInvalidMetrics`）一致；无回归、无范围漂移。
- **F-A4-S1-R2-03 已正确关闭**：`nMinusOne`、stateful `makeFit`、gateway `sharedFit` 均改为 **invalid 优先于 missing**，invalid 分支 `missing` 改用 `collectMissing(...)`（与 `requiredNodes` 对齐）；新增 case `invalid-dominates-missing-in-node-fit` 覆盖三处分支的 missing+invalid 组合，静态推演与 expect 一致。
- **controller 运行时证据已采信并与静态核验交叉一致**：controller 报告 86/86 cases、459 probes、exit 0；本评审独立 `jq` 计数当前 fixture 恰为 **86 case**（原 85 + fix 2 新增 1），与 controller 结果数量吻合。本评审会话 node 仍被权限网关阻断（§3），**不声称自己运行了 node**。
- 未发现 fix 2 引入的 P0/P1/P2 新缺陷；无 S2/公共契约/docs/18/template/validate.mjs 漂移。

## 2. 评审范围与方法

- 对照原评审 §4 的 R2-02/R2-03 证据行，逐项检查当前文件。
- 对 `calc.mjs` 三处判定分支做静态推演（含 missing-only、invalid-only、missing+invalid 三态），核对 `STATUS_RANK` 与 `requiredNodes` 顺序。
- 对 `s1-04` 修复 case 与 `s1-06` 新增 case 做公式级静态推演（值、status、reasons、error path）。
- 独立执行允许的静态命令（`jq`、`sha256sum`、`wc`、`git status`）；尝试三个 node 命令并如实记录权限结果（§3）。
- 核对 fix 2 哈希/行数与报告 §11.2/§11.3，以及未改文件哈希与 §2/§10.2 是否一致（范围漂移检查）。

## 3. 命令执行与 exit status（如实区分本会话权限与 controller 证据）

### 3.1 本复评会话实际执行

| 命令 | 结果 | exit code |
| --- | --- | --- |
| `node --check deployment/capacity/calc.mjs` | `This command requires approval`（进程未启动） | 无 |
| `node --check deployment/capacity/validate.mjs` | 同上 | 无 |
| `node deployment/capacity/validate.mjs --stage s1` | 同上 | 无 |

与原评审 §3.1 及 memory `env-permission-gate.md` 一致：本会话无法获得 node 运行时输出。**本评审不声称**亲自验证了 86 case 断言、459 probe 或退出码。

### 3.2 本会话独立运行的静态检查（可复核）

| 命令 | exit code | 关键输出 |
| --- | --- | --- |
| `jq -s 'map(.cases\|length) \| {per, total: add}'`（9 fixtures） | 0 | per=`[4,6,7,8,11,17,5,17,11]`，**total=86** |
| `jq -s '[.[].stage] \| unique'`（9 fixtures） | 0 | `["s1"]` |
| `jq empty`（s1-04、s1-06、inputs.template.json） | 0 | 无输出（严格 JSON） |
| `jq` 提取 `positive-backlog-net-negative` 输入/expect | 0 | FREE=1000、DISK=2000、EXISTING=1000；expect 含 root NOT_MET、errorsAbsent、noInvalidMetrics |
| `jq` 提取 `invalid-dominates-missing-in-node-fit` | 0 | 三断言 metric + 三条 TYPE_INVALID error path |
| `sha256sum`（fix 2 三文件 + 未改文件） | 0 | 见 §4.3；与报告哈希一致 |
| `wc -l`（calc / s1-04 / s1-06） | 0 | `3818 / 231 / 546`（与 §11.3 一致） |
| `git status --short`（A4 相关路径） | 0 | `deployment/capacity/`、`docs/18*`、S1 implementation、原评审均为 untracked（`??`），无意外已跟踪改动 |

### 3.3 controller 运行时证据（本评审采信，非本会话执行）

| 证据来源 | 内容 |
| --- | --- |
| 任务给定 / 实施报告披露的 controller 执行 | `node --check` calc + validate；`node deployment/capacity/validate.mjs --stage s1` |
| 结果 | **86/86 cases passed，459 nonfinite probes，exit 0** |

交叉一致性：本评审静态 case 计数 86 = controller 报告 86；fix 2 恰新增 1 case（`invalid-dominates-missing-in-node-fit`），与 85→86 差值吻合。运行时证据与静态推演无冲突。

## 4. 发现（本复评范围内）

### 无新发现（P0–P2）

本复评未发现 R2-02/R2-03 关闭错误、回归或范围漂移类缺陷。

### F-A4-S1-R2-02 关闭核验 — **已关闭**

- **原问题**：`s1-04-spool.json` `positive-backlog-net-negative` 曾为 `DISK=FREE=2000` ⇒ `used=0` 且 `EXISTING=1000>0`，触发未预期 `CAPACITY_INCONSISTENT`，root 被掩盖为 `INVALID`。
- **当前输入**（`deployment/capacity/fixtures/s1-04-spool.json:129-137`）：`SPOOL_DISK_BYTES=2000`、`SPOOL_FREE_BYTES=1000`、`SPOOL_EXISTING_BYTES=1000`；`NEW_PEAK=NEW_AVG=0`、`OUTAGE=10`、`RECOVERY_TOTAL=50`、`CONTINUING=100`。
- **当前 expect**（同文件 :140-150）：`root.status=NOT_MET`；`spool.used=COMPLETE 1000`；`recoveryBacklog=COMPLETE 1000`；`net=COMPLETE -50`；`drain=NOT_MET valueNull DRAIN_IMPOSSIBLE`；`errorsAbsent=["CAPACITY_INCONSISTENT"]`；`noInvalidMetrics=true`。behavior 文案（:125）同步声明快照一致。
- **静态推演**（对照 `calc.mjs`）：
  1. `used`：`free(1000) ≤ disk(2000)`，`value=1000`；`existing.usable && existing.value > value` ⇒ `1000 > 1000` 为 false（实现只拒绝严格大于，`calc.mjs:3518`）⇒ `used=COMPLETE 1000`，无 `addError(CAPACITY_INCONSISTENT)`（:3519-3522）。
  2. `recoveryBacklog = existing + newAvg×outage = 1000 + 0 = 1000` COMPLETE（:3587-3593）。
  3. `net = 50 − 100 = −50` COMPLETE（:3643-3650）。
  4. `drain`：backlog COMPLETE 且 `net.value ≤ 0` ⇒ `NOT_MET / DRAIN_IMPOSSIBLE / value null`（:3676-3677）。
  5. `ctx.errors` 为空 ⇒ root = `worst(groups)`；spool 最差 `NOT_MET`（rank 3）高于其他组 `INCOMPLETE`（rank 2）⇒ root `NOT_MET`（:3765；`STATUS_RANK` :25-31）。
  6. 无 INVALID metric ⇒ `noInvalidMetrics` 成立（`validate.mjs:181-186`）。
- **判定**：关闭正确。与原评审 §4 所要求的修法（`FREE 2000→1000`，保持 backlog/net/drain 不变）一致；expect 收紧为正向断言，脆弱性消除。

### F-A4-S1-R2-03 关闭核验 — **已关闭**

- **原问题**：`nMinusOne`/`makeFit`/`sharedFit` 在叶同时 `missing=true` 且 `invalid=true` 时先命中 missing 分支，返回 `NOT_VERIFIED/MEASUREMENT_REQUIRED`，与 `STATUS_RANK`（INVALID=4）及同函数 `requiredNodes`（invalid 优先）不一致。
- **当前代码**：
  - `nMinusOne`：`calc.mjs:3130-3138` **先** `demand.invalid || perNode.invalid || deployed.invalid || headroom.invalid` ⇒ `INVALID`，`missing: collectMissing(...)`、`invalid: collectInvalid(...)`、`reasons: invalidReasons(...)`；missing 分支退至 :3139-3148。
  - stateful `makeFit`：`calc.mjs:3205-3214` invalid 优先，`missing: collectMissing([demand, capacityRec, headroom])`；missing 分支 :3215-3225。
  - gateway `sharedFit`：`calc.mjs:3293-3301` invalid 优先（`playbackConcurrent/shared/headroom`），`missing: collectMissing(...)`；missing 分支 :3302-3311。
  - 对照 `requiredNodes`：`calc.mjs:3061-3069` 同样 invalid 优先且用 `collectMissing` — 三处分支现已对齐。
- **双标志叶确实可产生**：非 plain-object 叶 `invalidate(TYPE_INVALID)` 后置 `missing=true`（`calc.mjs:316-319`）；`value:null + approval:APPROVED` 走 `APPROVAL_INVALID` 且 `missing=true`（:390-394、:368-369）。
- **三态回归核对**：
  | 叶状态 | 修复前 | 修复后 | 期望 |
  | --- | --- | --- | --- |
  | missing-only | `NOT_VERIFIED` | `NOT_VERIFIED`（invalid 分支不命中） | 不变 |
  | invalid-only | `INVALID` | `INVALID` | 不变 |
  | missing+invalid | `NOT_VERIFIED`（缺陷） | `INVALID`（invalid 优先 + `collectMissing`） | **按 R2-03 修复** |
- **新增 fixture case**（`s1-06-node-api.json:471-518` `invalid-dominates-missing-in-node-fit`）：
  - 三角色各含一个非对象叶：`perNodeRate: 20`（core-api）、`measuredNormalTopologyCapacity: 120`（postgres）、`sharedCoreApiEdgeCapacity: 100`（media-gateway）。
  - 该类叶同时 `invalid=true`（`TYPE_INVALID`）与 `missing=true`（`calc.mjs:316-319`）。
  - expect 断言 `core-api.nMinusOne`、`postgres.normalFit`、`media-gateway.sharedFit` 均为 `INVALID`（`valueNull`、`invalidInclude` 叶路径、`reasonsInclude: TYPE_INVALID`），并断言三条 `TYPE_INVALID` error path 与 root `INVALID`。
  - 静态推演：`roleLeaf` 路径 `measuredNodes.MEASURED_ROLE[i].<key>` 与 expect 一致（:2853、:2785-2787）；invalid 分支 `collectInvalid` 收录该路径；`ctx.errors` 非空 ⇒ root `INVALID`（:3765）；`ROLE_NOT_DECLARED` 缺角色仅产生 `INCOMPLETE` metric 不产生 error（:2910-2924），不干扰 root。
  - `validate.mjs` 支持所用断言键：`invalidInclude`（:128）、`errorsInclude`（:161-172）、`errorsAbsent`（:173-177）、`noInvalidMetrics`（:181-186）。
- **判定**：关闭正确。代码顺序修复 + 组合 case 覆盖 + 与 `requiredNodes`/`STATUS_RANK` 对齐；missing-only 路径行为保持不变。

### 范围漂移检查 — **无漂移**

| 检查项 | 结果 |
| --- | --- |
| fix 2 变更文件（报告 §11.2） | 仅 `calc.mjs`、`s1-04-spool.json`、`s1-06-node-api.json`、本实施报告 §11 |
| `validate.mjs` 哈希 | `a0504ba8…` 与 §2 一致（未改） |
| 未改 fixture/template 哈希 | s1-01/02/05/07/08/09 与 §2 一致；s1-03 与 §10.2 fix 1 哈希 `335c2007…` 一致；template `bd408224…` 一致 |
| fix 2 哈希/行数 | calc `4b3e09c9…`、s1-04 `a9001384…`、s1-06 `42c19e5a…`；行数 `3818/231/546` — 与 §11.2/§11.3 一致 |
| S2 漂移 | `calc.mjs` 无 `F-BACKUP`/`F-CAP`/`backupRecovery`/`capabilityEvidence`/SKU/SLO/RPO/RTO 命中；未改 docs/18 |
| 公共 DTO/endpoint/表/Problem code | 未改；`validate.mjs` 未改 |
| 未越权编辑 | 本复评仅写入本文件 |

### 遗留（不在本复评关闭范围，如实记录）

- **F-A4-S1-R2-01**：原运行时验证缺口。本会话 node 仍被阻断；依据任务给定的 controller 证据（86/86、459 probes、exit 0）视为**由 controller 侧补齐**，非本评审执行。
- **F-A4-S1-R2-04**：`capacity-inconsistent-existing` 中 `used=INVALID` 与 `recoveryBacklog/drainSeconds=COMPLETE` 并存 — 原评审判定非缺陷（公式忠实），fix 2 未改（§11.1 明示），维持原判定。
- fix 2 新 case 未断言三角色的 `resourceEvidence`/role verdict 中间值（仅断言三 fit metric + root + errors）；这不影响 R2-03 关闭，属断言面选择而非缺陷。

## 5. 原发现关闭表

| Finding | 原严重度 | 关闭状态 | 证据摘要 | 复评判定 |
| --- | --- | --- | --- | --- |
| F-A4-S1-R2-01（运行时验证整体未执行） | P2 | **controller 侧补齐**（非 fix 2 范围） | controller：`node --check` ×2 + `validate --stage s1`，86/86、459 probes、exit 0；本会话仍权限阻断（§3.1） | 采信 controller 证据；本评审不重复声称 |
| F-A4-S1-R2-02（spool 快照矛盾残留） | P2 | **已关闭** | `s1-04-spool.json:129-150` FREE 2000→1000；静态推演 root NOT_MET、无 CAPACITY_INCONSISTENT；expect 正向收紧 | 关闭正确，无回归 |
| F-A4-S1-R2-03（missing 先于 invalid） | P3 | **已关闭** | `calc.mjs:3130/3205/3293` invalid 优先 + `collectMissing`；`s1-06-node-api.json:471-518` 新 case 覆盖三分支 | 关闭正确，missing-only 行为不变 |
| F-A4-S1-R2-04（COMPLETE/INVALID 并存语义） | P3（非缺陷） | 维持原判定 | fix 2 未改（§11.1）；原评审 §4 判定公式忠实 | 不重开 |

## 6. 结论与下一步

**Pass。**

- F-A4-S1-R2-02、F-A4-S1-R2-03 均已按原评审要求正确关闭：修复落在允许路径内，静态语义与 expect 一致，missing-only 无回归，无范围漂移。
- 运行时维度：本会话无法执行 node（权限网关，§3.1）；controller 证据（86/86、459 probes、exit 0）与本评审静态 case 计数 86 交叉一致，予以采信并如实区分归属。
- 残余风险：① 运行时结论依赖 controller 环境，本评审未独立复跑；② R2-04 语义裁决仍留 plan 侧（原判定非缺陷）；③ 新 case 断言面为 fit/root/errors，未覆盖 role verdict 全中间值（非阻塞）。
- 未编辑实现/fixture/其他文档/控制文档/原型/截图；未 commit/push/PR；未进入 S2。
