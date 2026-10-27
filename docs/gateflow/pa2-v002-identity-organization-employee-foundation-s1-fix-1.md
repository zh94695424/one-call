# PA2 V002 S1 fix 1 — 精确 catalog inventory、完整 ACL 负向矩阵与 S1 证据勘误

- Task label: `pa2-v002-identity-organization-employee-foundation-s1-fix-1`
- Work unit: `PA2-v002-identity-organization-employee-foundation`，implementation slice S1
- Gate: `S1 fix`（independent code review 后，仅修复 controller 接受的三项 finding：`docs/reviews/code-review-20261027-162020.md` Finding 1/2/3；不进入 re-review、S1 checkpoint、S2 或 S3）
- Provider / runtime: provider `mimo-v2.6-flash`，model `mimo-v2.6-flash`，host WSL2 Linux 6.18.33.2-microsoft-standard-WSL2
- Base: accepted plan checkpoint `6f14f7fdf6e69960452913631b419bda75071fc8`（worktree HEAD，未推进）
- Date: 2026-10-27（Asia/Shanghai）
- Writable paths（恰好三个）:
  1. `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`（edited）
  2. `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-implementation.md`（edited）
  3. `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-1.md`（new，本文件）

## 0. 结论

**COMPLETE — 本 bounded fix 的 `author_code_complete=true`；Finding 1、Finding 2、Finding 3 均为 `已修复`。不是 S1 accepted、不是 re-review 结果、不是 checkpoint、不是 GitHub Actions 绿灯、不是生产验收。**

- 只写入上述三个 writable path；未修改 V002、V001、`backend/pom.xml`、`BootstrapDependencyBoundaryTests`、CI workflow、README、control document、plan、review artifact、`docs/12`、`docs/14` 或任何其他路径。
- 未新增 `@Test`（`PostgresContainerIT` 仍恰好 12 个 `@Test`）；未新增 test method、未改方法名、未改 surefire/failsafe 计数。
- 未修改 V002 migration 的任何权限、schema、约束或对象（V002 SHA-256 与 review 冻结值一致）。
- 未 stage、commit、push、创建/更新 PR、merge 或进入 re-review。
- 未运行、也未声称新的 GitHub Actions run 或 production acceptance。

## 1. Accepted findings（binding: `docs/reviews/code-review-20261027-162020.md`；controller 裁决: control document gate 行 “Fix only the IT and S1 evidence, keep 12 methods, rerun full validation, then return to the same reviewer provider for re-review.”）

| Finding | 严重度 | 接受内容（review 摘要） | 本 fix 处置 |
| --- | --- | --- | --- |
| `1` | 中 | S1 inventory 未冻结 V002 列类型/可空性/默认值以及 PK/unique/index 列：`PostgresContainerIT.java` 仅断言列名+列数、PK/unique 名、index 名与两个 partial `indexdef`；同名但语义被改动的 catalog 可静默通过（review §Finding 1，指向 `:2117-2157 / :2211-2236 / :2275-2308`） | 在 `v001AndV002InventoryMatchesTheFrozenContract` 现有 `assertInventory` 流程内新增三段精确断言（不加 `@Test`）：47 行逐列 `(column_name, data_type, is_nullable, column_default)` 行集相等、10 个 PK/unique 的 whole `pg_get_constraintdef` + conkey 有序列名、18 个 index 的 whole `indexdef` 相等；全部 `isEqualTo`/`containsExactlyElementsOf`，无 contains/substring |
| `2` | 中 | ACL 负向矩阵未覆盖多数 role×table 的 `SELECT/INSERT/UPDATE`：仅 5 个 cross pair 与全局 `DELETE/TRUNCATE/REFERENCES/TRIGGER` 负向，其余 role×table 的读/写泄漏不会被发现（review §Finding 2，指向 `:2810-2842 / :2858-2872`） | 在 `assertV002OwnedAndCrossModuleGrants` 内新增完整显式期望矩阵：15 S1 roles × 6 V002 tables × table-level 与 column-level `SELECT/INSERT/UPDATE` 共 270+2115 次 `has_table_privilege`/`has_column_privilege` 全量 `isEqualTo` 断言；owner 固有权限与显式 ACL 分开建模；保留既有全部负向断言 |
| `3` | 低 | S1 证据文档三处 bounded evidence defects：(a) line 73 称 `uq_membership_live` 为 “non-unique partial”（实为 `CREATE UNIQUE INDEX`）；(b) 旧 line 115 称 “7 triggers: 6 V001 audit + 6 V002 guards”（实为 1 V001 audit + 6 V002 guards）；(c) §6 deparse 表 regex 行把 raw 单反斜杠写成 `\\` | 证据文档 (a)(b)(c) 全部勘误；另更新 IT hash 行为 post-fix 最终值并标注 pre-fix hash 仅作历史；新增 §7.2 “S1 fix 1 revalidation” 记录本次完整验证（不把 pre-fix hash/results 声称为最终） |

Binding sources read: `docs/reviews/code-review-20261027-162020.md`（全文）、`docs/gateflow/project-execution-control.md`（PA2 goal/gate 记录，只读）、`docs/gateflow/pa2-v002-identity-organization-employee-foundation-plan.md`（§6.5、§8.1-§8.6、§10-§13）、`docs/14-production-data-model-and-flyway-schema-spec.md`（§4.6-§4.11、§5.3、§6.2、§9.4/§9.5/§9.9/§9.10、§10.8）、V002、V001、`pom.xml`、`BootstrapDependencyBoundaryTests`、S1 implementation 证据、本 worktree 的 IT 全文。

## 2. Exact changes and file/line evidence

### 2.1 Finding 1 — `PostgresContainerIT.java` 精确 catalog inventory（三段，全部位于既有 `assertInventory` 调用链内，无新 `@Test`）

#### (a) 逐列契约块 — `:2159-2250`（comment `// Exact per-column contract (S1 fix 1, finding 1)`）

在既有 “V002 表无 id default、`pg_attrdef` 计数” 断言之后、check-name map 之前，新增：

- `Map<String, List<String>> v002ColumnDetails`（`LinkedHashMap`，6 个表 → 共 47 行），每行为 `column_name|data_type|is_nullable|column_default`，按 `ordinal_position` 排序，例如：
  ```java
  v002ColumnDetails.put("customer_account", List.of(
          "id|uuid|NO|<none>",
          "display_name|text|NO|<none>",
          "status|text|NO|'ACTIVE'::text",
          "revision|integer|NO|1",
          "created_at|timestamp with time zone|NO|now()",
          "updated_at|timestamp with time zone|NO|now()"));
  ```
  列数 6/10/9/6/7/9；`auth_identity.verified_at` 与 `key_version` 为 `NO|<none>`（无 default 但 NOT NULL）；`identity_profile` 五个受保护字段为 `YES|<none>`；`organization_membership.department_label` 为 `YES|<none>`。
- 断言查询（逐表，parameterized）：
  ```sql
  SELECT column_name || '|' || data_type || '|' || is_nullable || '|' || coalesce(column_default, '<none>')
  FROM information_schema.columns
  WHERE table_schema = ? AND table_name = ?
  ORDER BY ordinal_position
  ```
  → `containsExactlyElementsOf(expectedRows)`（行集 + 顺序整体相等，任一类型/可空性/默认值/缺列/多列/乱序均失败）。
- 每表：`pg_attrdef` 该表 default 数 == 期望行中非 `<none>` 的条数；全 V002 三 schema `pg_attrdef` 总数 == **16**（`customer_account` 4 + `auth_identity` 2 + `identity_profile` 2 + `organization` 4 + `organization_membership` 2 + `employee_account` 2；S1 fix 2 corrected the earlier 3/3/2/3/3/2 split per S1RR-01）；既有 “V002 表 `id` 无 default” 断言保留。

#### (b) PK/unique 契约块 — `:2330-2398`（comment `// Exact PK/unique contract (S1 fix 1, finding 1)`）

- `Map<String, String[]> v002PkUniqueDefs`（10 项，key = `schema.table.conname`），value = `{schema, table, conname, contype, pg_get_constraintdef 全文, 逗号连接的有序列名}`：
  - 6 × `PRIMARY KEY (id)`（contype `p`，列 `id`）
  - `uq_auth_identity_subject` → `UNIQUE (provider, provider_scope, provider_subject_hmac)`
  - `uq_identity_profile_account` → `UNIQUE (account_id)`
  - `uq_employee_login_hmac` → `UNIQUE (login_name_hmac)`
  - `uq_employee_issuer_subject` → `UNIQUE (auth_issuer, auth_subject_hmac)`
- `hasSize(10)` 守卫；逐项查询：
  ```sql
  SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c
  JOIN pg_class r ON r.oid = c.conrelid JOIN pg_namespace n ON n.oid = r.relnamespace
  WHERE n.nspname = ? AND r.relname = ? AND c.conname = ? AND c.contype = ?
  ```
  → 恰好 1 行 + whole-string `isEqualTo(expectedDef)`；另查 `unnest(c.conkey) WITH ORDINALITY ... ORDER BY k.ord` 的列名 → `containsExactlyElementsOf(orderedColumns)`。
- 同名约束在错误表 / 错误 contype / 错误列集 / 错误列序下查不到行或定义不等 → fail closed。既有 FK def/flags 与 name-only 列表断言全部保留。

#### (c) 索引契约块 — `:2460-2535`（comment `// Exact index contract (S1 fix 1, finding 1)`）

- 删除原 “两个 partial index 各一次 `indexdef` 断言” 的 standalone 写法，替换为 `Map<String, String> v002IndexDefs`（18 项，key = `schema.table.indexname` → whole `pg_indexes.indexdef` 全文），覆盖 6 PK-backed + 4 unique-constraint-backed + 6 普通 + 2 partial，含：
  - `CREATE UNIQUE INDEX uq_membership_live ON organization_access.organization_membership USING btree (organization_id, account_id) WHERE (status <> 'REVOKED'::text)`（**unique** partial）
  - `CREATE INDEX ix_employee_enabled_admin ON operations_iam.employee_account USING btree (id) WHERE ((role = 'ADMIN'::text) AND (status = 'ACTIVE'::text))`
- `hasSize(18)` 守卫；逐项 `SELECT indexdef FROM pg_indexes WHERE schemaname = ? AND tablename = ? AND indexname = ?` → 恰好 1 行 + `isEqualTo` whole-string。
- 既有 18-name 列表断言（`:2436-2458`）保留：缺/多索引先由 name 列表失败，列序/唯一性/谓词变化由 whole `indexdef` 失败。

原始依据：47 行列契约、10 条 `pg_get_constraintdef`、18 条 `indexdef`、default 总数 16 均于本 fix 期间在 disposable `postgres:16`（16.15）容器 `pa2fix1_capture` 中（V001、V002 各 `--single-transaction` 应用后）独立采集，并与 `docs/14` §4.6-§4.11/§5.3 逐条核对一致后冻结为字面量；采集容器已删除。34-check map、FK flags、对象计数、V001 断言、trigger 矩阵等既有断言一字未动。

### 2.2 Finding 2 — `PostgresContainerIT.java:3101-3149` 完整 ACL 期望矩阵（位于 `assertV002OwnedAndCrossModuleGrants`，方法起于 `:2964`）

在既有 5-pair cross 断言与全局 `DELETE/TRUNCATE/REFERENCES/TRIGGER` 负向循环之后、guard EXECUTE 断言之前，新增（复用既有 `owningOwnerByTable`、`owningRoleByTable`、`ownedUpdateColumns`、`crossSelectColumns`、`allColumnsByTable`、`S1_ROLES`、`V002_TABLES`、`hasTable`/`hasColumn` helper）：

```java
for (String table : V002_TABLES) {
    String owner = owningOwnerByTable.get(table);
    String runtimeRole = owningRoleByTable.get(table);
    List<String> allColumns = allColumnsByTable.get(table);
    List<String> allowedUpdateColumns = ownedUpdateColumns.get(table);
    for (String role : S1_ROLES) {
        boolean ownerRow = role.equals(owner);
        boolean runtimeRow = role.equals(runtimeRole);
        boolean expectTableSelect = ownerRow || runtimeRow;
        boolean expectTableInsert = ownerRow || runtimeRow;
        boolean expectTableUpdate = ownerRow;
        // hasTable(role, table, SELECT/INSERT/UPDATE) isEqualTo 期望值
        List<String> crossColumns = crossSelectColumns.get(role + "|" + table);
        for (String column : allColumns) {
            boolean expectColumnSelect = expectTableSelect
                    || (crossColumns != null && crossColumns.contains(column));
            boolean expectColumnInsert = expectTableSelect;
            boolean expectColumnUpdate = ownerRow
                    || (runtimeRow && allowedUpdateColumns.contains(column));
            // hasColumn(role, table, column, SELECT/INSERT/UPDATE) isEqualTo 期望值
        }
    }
}
```

建模规则（与 plan §6.5 / docs/14 §9.4-§9.10 完全一致，不改 migration）：

- **表 owner（3 个）**：固有权限与显式 ACL 分开计入期望——table-level `SELECT/INSERT/UPDATE` 与全部列级权限均为 true（PostgreSQL owner 固有）。
- **owning runtime role（3 个）**：table-level `SELECT/INSERT` true、table-level `UPDATE` false；列级 `UPDATE` 仅 `ownedUpdateColumns` 中的精确列为 true，其余列 false；列级 `SELECT/INSERT` = table-level 值。
- **5 个 cross-module pair**（`subject_number_role`/`customer_iam_role` 对各表的精确列 SELECT）：table-level `SELECT/INSERT/UPDATE` 全 false；仅 `crossSelectColumns` 中的精确列为 `SELECT` true，其余所有列 `SELECT/INSERT/UPDATE` 全 false。
- **其余所有 role×table**（含 `operations_iam_role` 对非 employee 表、全部 platform/owner-role 对非本模块表）：table-level 与全部列级 `SELECT/INSERT/UPDATE` 全 false，无列级泄漏。

断言规模：15 roles × 6 tables × 3 特权 = 270 次 `has_table_privilege`；15 roles × 47 列 × 3 特权 = 2115 次 `has_column_privilege`；全部 `isEqualTo`（布尔期望），每条带 `as("matrix ...")` 上下文。既有断言（owned 正例、5-pair cross、全局 DELETE/TRUNCATE/REFERENCES/TRIGGER 负向、schema USAGE/CREATE 矩阵、PUBLIC grantee=0、guard EXECUTE owner+runtime、function PUBLIC 负向）全部保留。undeclared `GRANT`（任意 role 在错误表上获得 `SELECT/INSERT/UPDATE`，或 cross 列外泄漏）会把期望 false 翻成 true → fail closed。

### 2.3 Finding 3 — S1 证据文档勘误与补记

文件：`docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-implementation.md`（251 行）。

| 位置 | 前 | 后 |
| --- | --- | --- |
| `:73`（对象表 Indexes 行） | “includes **non-unique partial** `uq_membership_live` …” | “includes **unique partial** `uq_membership_live` (`CREATE UNIQUE INDEX ... WHERE status <> 'REVOKED'`) and non-unique partial `ix_employee_enabled_admin` …” |
| `:118`（§5.2 trigger 行；review 引用的旧行号为 115，因新增 bullets 后移） | “7 triggers: **6 V001 audit** + 6 V002 guards” | “7 triggers: **1 V001 audit** + 6 V002 guards” |
| `:125`（§6 intro）+ `:135 / :141 / :142 / :144`（regex deparse 行） | regex 转义显示为 `\\.`、`\\..+$` | 改为 raw 单反斜杠 `^v1\.[A-Za-z0-9._:/-]+\..+$`，并在 intro 增加说明 “The table below renders the raw PG16 deparse strings; regex rows show actual single backslashes … (S1 fix 1, finding 3)” |
| `:31`（§2 writable-path 表，IT 行） | IT SHA-256 为 pre-fix `23267f0c…` | `f3f53092…`，标注 “(post-fix final; the pre-fix S1 hash `23267f0c…` is superseded and recorded only as history in …-s1-fix-1.md)” |
| `:114-116`、`:120`（§5.2） | 无 | 新增 3 条 “S1 fix 1 (finding 1)” inventory bullets 与 1 条 “S1 fix 1 (finding 2)” matrix bullets（准确描述 2.1/2.2 的新断言） |
| `:198-216`（新 §7.2） | 无 | “S1 fix 1 revalidation (2026-10-27; supersedes the pre-fix hash/results above)”：10/0/0/0、两次 12/0/0/0（PG 16.15）、方法集不变/`@Test`=12、七项 mutation/GRANT 探针结果、Boot smoke、FIREWALL_OK 27 项、V001 blob 匹配且 V001/pom/Bootstrap diff=0、V002 未变、post-fix IT hash、scoped whitespace 零诊断、IT 仍为均匀 CRLF；并在 `:216` 声明 pre-fix rows/hash 仅作历史 |

原 review 指出的三处文本全部修正；§7 pre-fix 结果行按 task 要求保留为历史而非冒充最终结果，最终验证记录集中在 §7.2 与本文件 §3。

## 3. Validation commands and results

Environment（与 task 精确一致）：

- WSL2 Linux `6.18.33.2-microsoft-standard-WSL2`；Docker Server `29.1.3`（API `1.52`）
- JDK：Temurin `21.0.13+9`（`/opt/jdk21`，Eclipse Adoptium）
- Maven：Apache Maven `3.9.9`（`/opt/maven-3.9.9`），所有命令均以 `JAVA_HOME=/opt/jdk21` 前缀运行（未使用 CRLF `./mvnw`，未使用 Windows interop java）
- PostgreSQL：floating tag `postgres:16` → 观测 **16.15**（两次 fresh-container run 均为 16.15）
- Flyway `12.4.0` / JDBC `42.7.14` / Testcontainers `2.0.5` / Spring Boot `4.1.2`
- WSL git 无法解析本 worktree `.git` 指针（`D:/codex-call/.git/worktrees/codex-call2`）→ 所有 git 操作经 `powershell.exe` + Windows git 于 `C:\Users\Administrator\.codex\worktrees\pa2-v002-identity-foundation\codex-call` 执行；文件哈希用 WSL `sha256sum`/python

主验证（均从 `backend/` 运行）：

| # | Command | 结果 |
| --- | --- | --- |
| 1 | `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp clean verify` | **PASS**：`BUILD SUCCESS`；surefire XML 独立解析 `tests=10 failures=0 errors=0 skipped=0`（默认 10/10 精确保持） |
| 2 | `JAVA_HOME=/opt/jdk21 /opt/maven-3.9.9/bin/mvn -B -ntp -Pcontainer-smoke verify`（run 1，fresh container） | **PASS**：`BUILD SUCCESS`；failsafe `tests=12 failures=0 errors=0 skipped=0`；PostgreSQL **16.15**；`Successfully validated 2 migrations`，history `001`→`002`，重复 migrate 为 0 |
| 3 | 同命令（run 2，fresh container） | **PASS**：`BUILD SUCCESS`；failsafe `tests=12 failures=0 errors=0 skipped=0`；PostgreSQL 16.15 |
| 4 | failsafe/surefire XML 方法集解析（Python） | failsafe **恰为 12 个**方法，集合与 review §Validation 记录逐一相同；surefire 恰为 10；`skipped=failures=errors=0` |
| 5 | `grep -c '@Test' PostgresContainerIT.java` | `12`（无新增 `@Test`，无方法改名） |
| 6 | Boot smoke：`timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` | **PASS**：exit `0`；日志含 `Started OneCallApplication` |
| 7 | Fat-jar firewall：`jar tf` + forbidden grep（spring-security/flyway/jooq/HikariCP/postgresql/testcontainers） | **PASS**：`FIREWALL_OK`，27 个 `BOOT-INF/lib/` 项，无禁入库 |
| 8 | Windows git `git cat-file blob HEAD:...V001...` → SHA-256 | `946A0A13E07A4BFE583587A8CF5B1A24C56E3DCFB8CFECA2E28E84427CF81045`（匹配冻结值；object `d7e2e6879701a5db6517434cbb9b22d5c8f2d707`） |
| 9 | `git diff --ignore-cr-at-eol --exit-code HEAD -- <V001> <pom.xml> <BootstrapDependencyBoundaryTests>` | exit `0`（三者内容均未改变） |
| 10 | V002 `sha256sum` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5`（**未变**，与 review 冻结值一致；352 行 / 16447 字节 / LF / 0 CR） |
| 11 | Windows git `git -c core.whitespace=cr-at-eol diff --check`（三个 fix-touched 路径） | exit `0`，零诊断（IT 保持均匀 CRLF：3291 行 = 3291 CR） |
| 12 | Windows git `git diff --no-index --check /dev/null <本 fix artifact>` | exit `1`（内容差异），零 whitespace 诊断（符合预期） |
| 13 | Windows git `git status --porcelain=v1` 变更集 | 与 pre-fix S1 状态一致 + 仅新增本 artifact（见 §4）；三个 writable path 之外无任何内容改动 |

七项负向探针（全部在 disposable `postgres:16` 容器内、`BEGIN ... ROLLBACK` 事务中执行，不落库、不留容器）：

| 探针 | 变异 | 检测结果 |
| --- | --- | --- |
| A | 删除依赖 check 后 `ALTER TABLE customer_iam.auth_identity ALTER COLUMN key_version TYPE text`（类型漂移） | **检出（t）**：冻结的 `integer|NO|<none>` 行集断言不再匹配；回滚 |
| B | `customer_account.revision` default `1` → `2` | **检出（t）**：列行 default 与 `pg_attrdef` 计数断言失败；回滚 |
| C | 某 NOT NULL 列改为 NULL（nullability 漂移） | **检出（t）**：`is_nullable` 维度不匹配；回滚 |
| D | `uq_auth_identity_subject` 列集改动（同名 key 错列） | **检出（t）**：whole `pg_get_constraintdef` / conkey 有序列名不匹配；回滚 |
| E | 某 index 列顺序对调（同名列序漂移） | **检出（t）**：whole `indexdef` 不匹配；回滚 |
| F | undeclared `GRANT SELECT ... ON customer_iam.customer_account TO operations_iam_role` | **检出（t）**：矩阵中该 role×table 期望 false 翻为 true；回滚 |
| G | undeclared `GRANT INSERT ... TO platform_reliability_role`（错误表） | **检出（t）**：同上 fail closed；回滚 |

每项探针回滚后均复查对应冻结值（列行、约束定义、indexdef、`has_*_privilege` 基线）恢复原样；探针容器与采集容器 `pa2fix1_capture` 已删除；仓库与数据库零残留。

未触发任何 stop condition：修复未要求改动 migration/ACL/schema 合同；PG16 采集的 canonical defaults/index 定义与 `docs/14` 语义一致（47 行、10 键、18 索引、16 defaults 逐条核对）；全部验证一次通过（未出现需要记录的 bounded validation fix）。

## 4. Changed files with SHA-256（before → after）

| # | Path | Before SHA-256 | After SHA-256 | Change |
| --- | --- | --- | --- | --- |
| 1 | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `23267f0cc289260ded268ce75bd811f02c3774a20447be1d8f69b8d68f8b81e6` | `f3f530924ab626babd662f44ee4f68b0d9282915a81e75dafb725b375357be5b` | 新增 `:2159-2250`（逐列契约）、`:2330-2398`（PK/unique 契约）、`:2460-2535`（18 indexdef，替换 2 个 standalone partial 断言）、`:3101-3149`（完整 ACL 矩阵）；3291 行，均匀 CRLF；无新 `@Test` |
| 2 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-implementation.md` | `a20f91cc9d8cc9b7859f33814108c1b014d7cbc13cb9adcdf993f4f1a07dbf23` | `ecb73e7db12193e0b24c3721797c0d07b75d56c0f05a2569e8cb5f508f553146` | Finding 3 三处勘误（`:73`、`:118`、`:125`+regex 行）；`:31` hash 更新；§5.2 新增 4 条 fix bullets；新 §7.2（`:198-216`）；251 行，LF |
| 3 | `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-1.md` | —（new） | 本文件（自引用 hash 不嵌入） | 新增 fix artifact |

以下路径内容 **before == after**（证明未触碰）：

| Path | SHA-256（前后一致） |
| --- | --- |
| `backend/src/main/resources/db/migration/V002__identity_organization_and_employee.sql` | `534a0a06c9520856ef555275100e8b8a194de80556b336f6ed4c984c3d9795d5` |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql`（HEAD blob） | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` |
| `.github/workflows/ci.yml` | `9396aedd64bb60f394dcb3776c6514fb6394653ca1672fda01570deb132626c7` |
| `backend/README.md` | `07125182763b1ce2cffe0cb2d8bda610ef2190f6b1633afca6c42845dcd1c984` |
| `backend/pom.xml`、`BootstrapDependencyBoundaryTests`、`docs/14`、plan、control document、review artifact、V901 fixture | 未写入（`git diff --ignore-cr-at-eol` 对 HEAD = 0 或未列出） |

worktree 中 `docs/gateflow/project-execution-control.md` 显示为 modified，系 controller-owned 的 pre-existing 状态（S1 与本 fix 均未写入）；CRLF-only 波及的其余 ~513 个 tracked 文件为 Windows checkout artifact，`git diff --ignore-cr-at-eol` 下无内容差异。

## 5. No-goal-drift statement

- 只修复 review 中被 controller 接受的三项 finding；未扩大、缩小或改写任何 finding 的验收语义（Finding 1/2 均按 plan §8.2 “Columns / PK/unique constraints / Indexes / Table/column ACL” 四行的完整维度实现）。
- 未进入 re-review、未修改 review artifact、未推进 S1 checkpoint、未进入 S2/S3、未编辑控制文档或 plan。
- 未新增/删除/重命名任何 test method；`@Test` 仍为 12；surefire 10、failsafe 12 计数不变；CI expected set 未改。
- 未修改任何 migration、grant、role、schema、V003+ 对象或应用代码；V002 hash 冻结不变。
- 未猜测 Q2/KMS 轮换、Q17 migration identity、Q23 或 last-enabled-admin 语义。
- 未 stage/commit/push/PR/merge；未声称 CI 绿灯或生产验收。

## 6. Residual risks and uncovered areas

1. **deparse 字面量绑定 PG16 渲染**：列 default（`'ACTIVE'::text`/`now()`/`1`）、`pg_get_constraintdef` 与 18 条 `indexdef` 冻结自 PostgreSQL 16.15 实测。floating `postgres:16` 未来 minor/PG17 deparse 变化会使断言失败（fail-closed 方向），需相应更新测试；与 RSK-S1-02 同源，不影响 migration 语义。
2. **矩阵基于 `has_*_privilege` 有效权限**：断言的是 effective privilege 而非逐条 ACL 行；superuser/membership 派生权限在测试连接（`SET ROLE` 后以各 role 调用）下按 PG 语义求值。owner 固有权限以期望矩阵显式建模，若未来 PG owner 语义变化需同步期望。
3. **独立 capture/probe 以 superuser 应用迁移**；IT 的 runtime insert smoke 与矩阵以 runtime role 求值。生产 connection-pool/transaction 边界不在本切片。
4. **Q2（KMS/rotation）、Q17（migration identity/grantor）、Q23 仍 deferred**：auth-identity rotation guard 仅 S1 结构证明；S2 行为探针与 S3 last-admin/并发协议未执行。
5. **本次未运行 GitHub Actions**：仅本地 exact-JDK Maven + Docker 验证；不声称 CI 绿。
6. **re-review 仍未发生**：本 artifact 是 author-side 修复证据；须按 controller 裁决回到同一 reviewer provider 做 S1 re-review，且不得由本 fix 推进 checkpoint。
7. **Finding 1/2 的负向探针为一次性验证**：探针不在仓库中长期存在（task 要求不保留）；长期回归防护由正式断言本身承担。
8. **pre-existing worktree dirtiness**（control doc 修改、CRLF-only 噪声、review artifact 未跟踪）非本 fix 引入。

## 7. Final finding status

| Finding | Status | Basis |
| --- | --- | --- |
| `1`（中 — exact catalog evidence gap） | **已修复** | `PostgresContainerIT.java:2159-2250/:2330-2398/:2460-2535` 现已冻结 47 行逐列 `(name, type, nullability, default)`、10 个 PK/unique 的 whole `pg_get_constraintdef` + 有序列名、18 个 whole `indexdef`（含两 partial、uq 为 UNIQUE）；探针 A/B/C/D/E 均检出；34-check map、FK flags、对象计数与 V001 断言保留；12/12 两次通过 |
| `2`（中 — ACL negative matrix gap） | **已修复** | `PostgresContainerIT.java:3101-3149` 建立 15×6×(3 表级 + 3×列级) 完整期望矩阵（270 表级 + 2115 列级断言），owner 固有权限单独建模，owning runtime/cross/其他三类规则与 plan §6.5 一致；探针 F/G 检出 undeclared GRANT；既有全负向与 PUBLIC/guard 断言保留；未改 migration 权限 |
| `3`（低 — bounded evidence defects） | **已修复** | 证据 `:73` 已改 “unique partial `uq_membership_live`”；`:118` 已改 “1 V001 audit + 6 V002 guards”；`:125`+`:135/:141/:142/:144` 已按 raw PG16 单反斜杠记录；`:31` 更新为 post-fix hash 并标注 pre-fix 仅历史；新 §7.2 记录本 fix 完整验证 |

## 8. Completion status and stop statement

- Completion: **author_code_complete=true；bounded S1 fix COMPLETE**。
- 最终验证：默认 `clean verify` **10/0/0/0**；`-Pcontainer-smoke verify` 两次 fresh-container 均 **12/0/0/0**（PG 16.15、2 migrations validated）；方法集精确不变；七项负向探针全部检出且零残留；Boot smoke、fat-jar firewall、V001 blob/内容不变、V002 hash 冻结不变、pom/Bootstrap 不变、scoped whitespace 零诊断全部通过。
- 变更文件恰为三个 writable path：`PostgresContainerIT.java`（`23267f0c…` → `f3f53092…`）、S1 implementation 证据（`a20f91cc…` → `ecb73e7d…`）、本 fix artifact（new）。
- 本 fix 未 stage、未 commit、未 push、未创建/更新 PR、未 merge、未进入 re-review。
- 未声称 S1 accepted、checkpoint、GitHub Actions 绿灯或 production acceptance。
- Artifact path: `docs/gateflow/pa2-v002-identity-organization-employee-foundation-s1-fix-1.md`。
- **Stop: 本任务在此停止，等待 controller 将 S1 fix 交回同一 reviewer provider 进行 re-review。**
