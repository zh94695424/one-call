# PA2 V002 S1 fix 2 — 逐表 default 数拆分勘误（S1RR-01，evidence-only）

- Task label: `pa2-v002-s1-fix2-mimo26flash-20261027-01-retry1`
- Work unit: `PA2-v002-identity-organization-employee-foundation`，implementation slice S1
- Gate: `S1 fix 2`（binding: `docs/gateflow/project-execution-control.md` gate 行 “S1 re-review 1”——仅修复 controller 接受的 `S1RR-01`；不进入 re-review、S1 checkpoint、S2 或 S3）
- Provider / runtime: provider `mimo-v2.6-flash`，model `mimo-v2.6-flash`，host WSL2 Linux `6.18.33.2-microsoft-standard-WSL2`
- Base: accepted plan checkpoint `6f14f7fdf6e69960452913631b419bda75071fc8`（worktree HEAD，未推进）
- Date: 2026-10-27（Asia/Shanghai）
- Writable paths（恰好两个）:
  1. `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-1.md`（edited，仅 §2.1(a) 一处拆分文本）
  2. `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-2.md`（new，本文件）

## 0. 结论

**COMPLETE — 本 bounded fix 的 `author_code_complete=true`；`S1RR-01` 为 `已修复`。不是 S1 accepted、不是 re-review 结果、不是 checkpoint、不是 GitHub Actions 绿灯、不是生产验收。**

- 只写入上述两个 writable path；未修改 implementation、tests、migration、CI、README、control document、plan、review artifact、design doc、generated sources 或任何其他路径。
- 未添加 re-review 建议的可选 static-list completeness hardening（controller 已裁决 deferred 至 aggregate review：三份 47 列列表当前完全一致、V002 已冻结、无现存缺口）。
- 未运行、也未声称新的 Maven/容器验证或 GitHub Actions run（evidence-only 文本修正，不改执行代码）。
- 未 stage、commit、push、创建/更新 PR、merge 或进入 re-review。

## 1. Accepted finding（binding: `docs/reviews/code-review-20261027-170214.md` Finding `S1RR-01`；controller 裁决: control document gate 行 “S1 re-review 1”）

| Finding | 严重度 | 接受内容（review 摘要） | 本 fix 处置 |
| --- | --- | --- | --- |
| `S1RR-01` | 低 | S1 fix 1 artifact §2.1(a) 的逐表 `pg_attrdef` default 数拆分写为 `customer_account 3 + auth_identity 3 + identity_profile 2 + organization 3 + organization_membership 3 + employee_account 2`（合计 16 正确，但逐表拆分错误）；实际测试 `v002ColumnDetails`、V002 DDL 与 live PostgreSQL 16.15 均为 `4/2/2/4/2/2`（合计 16）。evidence-only 数值描述错误，可能误导 controller/审计者复核 “exact default counts” | 仅将 fix-1 §2.1(a) 该括号拆分改为 `customer_account 4 + auth_identity 2 + identity_profile 2 + organization 4 + organization_membership 2 + employee_account 2`（合计 16），并标注 S1 fix 2 / S1RR-01 溯源；不改任何测试或 migration |

Binding sources read: `docs/reviews/code-review-20261027-170214.md`（Finding S1RR-01 与 Open Questions）、`docs/gateflow/project-execution-control.md`（S1 re-review 1 gate 行与 current gate `S1 fix 2`）、`docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-1.md`（§2.1(a) 原文）、`backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java:2166-2250`（`v002ColumnDetails` 与 default 计数断言）、`backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql`（六表 DDL defaults）。

## 2. Exact before/after correction

文件：`docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-1.md:61`（§2.1(a) 的 default 计数行，全文仅此一处拆分）。

| | 内容 |
| --- | --- |
| **Before** | `… 总数 == **16**（`customer_account` 3 + `auth_identity` 3 + `identity_profile` 2 + `organization` 3 + `organization_membership` 3 + `employee_account` 2）；…` |
| **After** | `… 总数 == **16**（`customer_account` 4 + `auth_identity` 2 + `identity_profile` 2 + `organization` 4 + `organization_membership` 2 + `employee_account` 2；S1 fix 2 corrected the earlier 3/3/2/3/3/2 split per S1RR-01）；…` |

逐表变化：`customer_account` 3→4、`auth_identity` 3→2、`identity_profile` 2 不变、`organization` 3→4、`organization_membership` 3→2、`employee_account` 2 不变。合计前后均为 **16**（`4+2+2+4+2+2=16`）。fix-1 其余全部内容（含 `**16**`、`pg_attrdef` 断言语义描述、Finding 1/2/3 处置、验证记录、hashes、no-goal-drift、stop 语句）一字未动。

## 3. Evidence checked — 六个 truth 来源交叉一致

六表 default 数（均指 `pg_attrdef` / `column_default` 非 `<none>` 的列数）：

| Table | 值 | V002 DDL defaults（列） | `v002ColumnDetails` 中非 `<none>` 行 |
| --- | --- | --- | --- |
| `customer_account` | **4** | `status` `'ACTIVE'`、`revision` `1`、`created_at` `now()`、`updated_at` `now()`（V002:11-14） | `status`/`revision`/`created_at`/`updated_at`（IT:2170-2173） |
| `auth_identity` | **2** | `status` `'ACTIVE'`、`revision` `1`（V002:31,34） | `status`/`revision`（IT:2181,2184） |
| `identity_profile` | **2** | `real_name_status` `'UNVERIFIED'`、`revision` `1`（V002:54,60） | `real_name_status`/`revision`（IT:2188,2194） |
| `organization` | **4** | `status`、`revision`、`created_at`、`updated_at`（V002:85-88） | `status`/`revision`/`created_at`/`updated_at`（IT:2198-2201） |
| `organization_membership` | **2** | `status`、`revision`（V002:104-105） | `status`/`revision`（IT:2208-2209） |
| `employee_account` | **2** | `status`、`revision`（V002:129-130） | `status`/`revision`（IT:2218-2219） |
| **Total** | **16** | — | IT:2243-2250 total `isEqualTo(16)`；每表动态 `expectedDefaults`（IT:2230-2241） |

交叉来源：

1. **V002 DDL**（上表左）：六表 `DEFAULT` 子句逐条点名，无其他 default。
2. **PostgresContainerIT `v002ColumnDetails`**（IT:2166-2219）：47 行列契约中每表非 `<none>` 行数恰为 `4/2/2/4/2/2`；IT:2230-2241 按期望行动态计算每表 `expectedDefaults`，IT:2243-2250 断言全 V002 `pg_attrdef` 总数 == 16。测试断言本身在 fix-1 中已正确实现，本次未改测试。
3. **Live PostgreSQL 16.15 catalog**（re-review §Finding S1RR-01 “直接证据” 与 §Validation #13 已记录）：live `pg_attrdef` 查询为 `4/2/2/4/2/2`、total `16`。本 fix 为 evidence-only 文本修正，未改代码，采用 re-review 已记录的 live-catalog 证据，未重跑 Maven/容器。
4. **Re-review 独立判定**（`code-review-20261027-170214.md:32-36,102`）：预期 `4/2/2/4/2/2`，并确认测试与 catalog 本身正确、仅 fix-1 文本错误。
5. **Controller adjudication**（control doc S1 re-review 1 行）：接受 `4/2/2/4/2/2`、合计 `16`，指令为 “fix only that breakdown in the fix artifact”。
6. **算术**：`4+2+2+4+2+2 = 16`，与前后合计及 IT total 断言一致。

无证据分歧；未触发 stop condition。

## 4. Validation commands and results

Environment：WSL2 Linux `6.18.33.2-microsoft-standard-WSL2`。WSL git 无法解析本 worktree `.git` 指针（`D:/codex-call/.git/worktrees/codex-call2`，与 fix-1 §3 记录一致）；带 `GIT_DIR`/`powershell.exe`/`--git-dir` 的 git 调用在本会话需 elevated approval 未获授，故 “范围外 diff 未变” 采用与 fix-1 同源的 **frozen-hash 交叉核验法**（fix-1/re-review 已冻结的路径内容 hash 不变即证未被触碰）。

| # | Check | 结果 |
| --- | --- | --- |
| 1 | 六值证据交叉核验（§3 表：V002 DDL × `v002ColumnDetails` × re-review live-catalog 记录） | **PASS**：全部 `4/2/2/4/2/2`，total `16` |
| 2 | 修正后六值求和 | **PASS**：`4+2+2+4+2+2 = 16`（与 `**16**`、IT total 断言一致） |
| 3 | `grep`（PCRE）fix-1 §2.1(a) 现行拆分文本 | **PASS**：`customer_account` 4 + `auth_identity` 2 + `identity_profile` 2 + `organization` 4 + `organization_membership` 2 + `employee_account` 2；旧 `3 + … 3 + … 3 + … 3 + …` 拆分在该行不存在 |
| 4 | fix-1 全文旧拆分残留扫描（`customer_account` 3 + …） | **PASS**：仅 §2.1(a) 一处曾含拆分，已改为正确值；全文无 `3/3/2/3/3/2` 错误拆分残留（溯源括号内 “earlier 3/3/2/3/3/2” 为勘误标注，非当前值陈述） |
| 5 | Scoped whitespace：fix-1、fix-2 尾随空白（`[ \t]+$`） | **PASS**：两文件均 0 命中 |
| 6 | Scoped conflict markers：fix-1、fix-2（`^<{7}`/`^={7}`/`^>{7}`） | **PASS**：两文件均 0 命中 |
| 7 | Frozen-hash 交叉核验（范围外关键路径，见下表） | **PASS**：V002/IT/S1-evidence/CI/README/original-review/re-review/plan/docs-14 全部与 fix-1 §4 或 re-review §Validation 记录值逐字节一致 |
| 8 | fix-1 SHA-256 before → after | `299ae7badaf017ca14aa6d9e2b8aa3742eef31fc5ae16e52b24d0765ff662c7b` → `43bdf6428b08b6e7489f59c3846d028db3d7fc896f3c82d06b43237b1b7e9aa5` |

Frozen-hash 交叉核验明细（before == after == 记录值）：

| Path | SHA-256（本次实测 == 冻结记录） |
| --- | --- |
| `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5`（fix-1 §4 / re-review #10） |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `f3f530924ab626babd662f44ee4f68b0d9282915a81e75dafb725b375357be5b`（fix-1 §4 / re-review #10） |
| `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-implementation.md` | `ecb73e7db12193e0b24c3721797c0d07b75d56c0f05a2569e8cb5f508f553146`（fix-1 §4 / re-review #10） |
| `.github/workflows/ci.yml` | `9396aedd64bb60f394dcb3776c6514fb6394653ca1672fda01570deb132626c7`（fix-1 §4 / re-review #11） |
| `backend/README.md` | `07125182763b1ce2cffe0cb2d8bda610ef2190f6b1633afca6c42845dcd1c984`（fix-1 §4 / re-review #11） |
| `docs/reviews/code-review-20261027-162020.md` | `ee1ec3167410b75787ed3e6ad6cb63105e06b7308f9e3ea073a9fb91ff47f48d`（re-review #10） |
| `docs/reviews/code-review-20261027-170214.md` | `71e673b04cbe8bd437185a986503f1fb9270d888b02b94ac1e72e911d0e847ca`（control doc S1 re-review 1 行） |

未运行 Maven/容器（evidence-only；未改执行代码，不需也不声称全量验证）；未触发 GitHub Actions。

## 5. Changed files with SHA-256

| # | Path | Before SHA-256 | After SHA-256 | Change |
| --- | --- | --- | --- | --- |
| 1 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-1.md` | `299ae7badaf017ca14aa6d9e2b8aa3742eef31fc5ae16e52b24d0765ff662c7b` | `43bdf6428b08b6e7489f59c3846d028db3d7fc896f3c82d06b43237b1b7e9aa5` | 仅 §2.1(a) `:61` 的逐表 default 拆分 `3/3/2/3/3/2` → `4/2/2/4/2/2`（合计仍 16），并加 S1 fix 2 / S1RR-01 溯源括注；其余内容未动 |
| 2 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-2.md` | —（new） | 本文件（自引用 hash 不嵌入） | 新增 fix-2 artifact |

以下路径内容 **before == after == 冻结记录值**（证明未触碰；见 §4 表）：V002、`PostgresContainerIT.java`、S1 implementation evidence、CI workflow、README、original review、re-review、plan、`docs/14`。V001、`pom.xml`、`BootstrapDependencyBoundaryTests`、control document 未写入（frozen-hash 或内容与 HEAD/记录一致；WSL git worktree 指针问题见 §4 注，范围外无本任务写入）。

## 6. No-goal-drift statement

- 只修复 re-review 中被 controller 接受的单一 finding `S1RR-01`；未扩大、缩小或改写其验收语义（仅 fix-1 §2.1(a) 一处拆分文本）。
- 未修改 `PostgresContainerIT.java`、V002/V001 migration、任何测试、CI、README、control document、plan、review artifact、design docs 或应用代码。
- 未添加 `allColumnsByTable`/`v002ColumnDetails` completeness cross-check 硬化（controller 已明确 deferred 至 aggregate review；本次不做）。
- 未新增/删除/重命名任何 test method；未改 `@Test`=12、surefire 10、failsafe 12 计数。
- 未进入 re-review、未修改 review artifact、未推进 S1 checkpoint、未进入 S2/S3、未编辑控制文档或 plan。
- 未 stage/commit/push/PR/merge；未声称 CI 绿灯或生产验收。

## 7. Residual risks and uncovered areas

1. **WSL git worktree 指针**：本会话 WSL git 无法解析 `D:/codex-call/.git/worktrees/codex-call2`；`GIT_DIR`/`powershell.exe`/`--git-dir` 调用需 elevated approval 未获授。范围外未变由 frozen-hash 交叉核验（§4 #7）+ 本任务仅写两个允许路径共同证明，与 fix-1 §3 记录的同一环境限制一致，非本次引入。
2. **本次未重跑 live catalog**：采用 re-review §Validation #13 已记录的 live PostgreSQL 16.15 `pg_attrdef` 证据（`4/2/2/4/2/2`, total 16）+ V002 DDL + `v002ColumnDetails` 三方交叉；evidence-only 修正不需要也未声称新的容器/Maven 运行。
3. **re-review 仍未发生**：本 artifact 是 author-side 勘误证据；须按 controller 裁决回到同一 reviewer provider 做最终 re-review，且不得由本 fix 推进 checkpoint。
4. **Q2/Q17/Q23、static-list completeness hardening 仍 deferred**：与本次 finding 无关，不在本切片。

## 8. Final finding status

| Finding | Status | Basis |
| --- | --- | --- |
| `S1RR-01`（低 — fix artifact 逐表 default 拆分 `3/3/2/3/3/2` 与实际 `4/2/2/4/2/2` 不一致） | **已修复** | fix-1 `:61` 现为 `customer_account 4 + auth_identity 2 + identity_profile 2 + organization 4 + organization_membership 2 + employee_account 2`（合计 16，与 V002 DDL、`v002ColumnDetails`、re-review live-catalog 记录三方一致）；fix-1 SHA-256 `299ae7ba…` → `43bdf642…`；未改测试/migration；scoped whitespace/conflict-marker 零诊断 |

## 9. Completion status and stop statement

- Completion: **author_code_complete=true；bounded S1 fix 2 COMPLETE**。
- 仅修复 `S1RR-01`：fix-1 §2.1(a) 拆分 `3/3/2/3/3/2` → `4/2/2/4/2/2`（合计 16）；六值经 V002 DDL、`v002ColumnDetails`、re-review live-catalog 记录交叉一致。
- 变更文件恰为两个 writable path：fix-1（`299ae7ba…` → `43bdf642…`）、本 fix-2 artifact（new）。
- Scoped whitespace/conflict-marker 检查两文件均零诊断；范围外 frozen-hash 全部与冻结记录一致。
- 未 stage、未 commit、未 push、未创建/更新 PR、未 merge、未进入 re-review。
- 未声称 S1 accepted、checkpoint、GitHub Actions 绿灯或 production acceptance。
- Artifact path: `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-2.md`。
- **Stop: 本任务在此停止，等待 controller 将 S1 fix 2 交回同一 reviewer provider 进行最终 re-review。**
