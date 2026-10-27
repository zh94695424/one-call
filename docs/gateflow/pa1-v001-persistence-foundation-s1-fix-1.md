# PA1 S1 fix 1 — README Flyway 版本记录与 partial index 精确断言

- Task label: `pa1-v001-s1-fix1-ds-20261027-01`
- Work unit: `PA1-v001-persistence-foundation`，implementation slice S1
- Gate: `S1 fix`（independent code review 后，仅修复 controller 接受的两项 low-severity finding；不进入 re-review、S1 checkpoint、S2 或 S3）
- Base: merged `main` @ `9fe3619af968d896400742d90df819bb66d868bd` 的 PA1 managed worktree；S1 实现内容仍未提交
- Date: 2026-10-27（Asia/Shanghai）
- Artifact: 本文件（`docs/gateflow/pa1-v001-persistence-foundation-s1-fix-1.md`）

## 0. 结论

**COMPLETE — 本 bounded fix 的 `author_code_complete=true`；`F-01` 与 `F-02` 均为 `已修复`。不是 S1 accepted、不是 re-review 结果、不是 checkpoint、不是 GitHub Actions 绿灯、不是生产验收。**

- 只修改 `backend/README.md:15` 与 `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java:607-624`，并新增本 fix artifact。
- 未修改 V001 SQL、`backend/pom.xml`、V901 fixture、CI workflow、control document、plan、review artifact、`docs/12`、`docs/14`、main source、existing guardrail tests 或任何其他路径。
- 未 stage、commit、push、创建/更新 PR、merge 或进入 re-review。
- 未运行、也未声称新的 GitHub Actions run 或 production acceptance。

## 1. Accepted findings

| Finding | 严重度 | 接受内容 | 本 fix 处置 |
| --- | --- | --- | --- |
| `F-01` | P3 / Low / documentary | `backend/README.md:15` 仍写 Flyway BOM 管理“待 `dependency:tree` 确认”，但 controller 已用 `dependency:tree` 将 `flyway-core` 与 `flyway-database-postgresql` 解析为 `12.4.0` test scope（Boot `4.1.2` BOM），R-02 已关闭 | 仅更新该 stale pin cell，记录已解析版本与关闭状态；保留 test scope、Surefire、Enforcer、fat-jar firewall 措辞 |
| `F-02` | P3 / Low / test-hardening | `PostgresContainerIT.java:607-621` 对三个 partial index 使用多 substring `.contains`，`WHERE status <> 'NEW'` 或带额外 predicate 的 `indexdef` 也可能满足，无法区分 predicate 极性 | 改为对 PostgreSQL 16 `pg_indexes.indexdef` 的完整精确字符串断言；一次断言同时固定 indexed columns 与正向 equality predicate |

Binding sources read: `docs/gateflow/project-execution-control.md`（PA1 current work unit 与 S1 code review 行）、`docs/gateflow/pa1-v001-persistence-foundation-plan.md`（§6.1、§7.1、§7.2、§8）、`docs/reviews/code-review-20261027-111932.md`（F-01/F-02）、`docs/gateflow/pa1-v001-persistence-foundation-s1-implementation.md`（§8 Flyway versions、R-02 closure）、`docs/14-production-data-model-and-flyway-schema-spec.md`（§3.2/§3.3 partial index contract）、V001 SQL、README、IT 与 POM。

## 2. Exact changes and file/line evidence

### 2.1 `backend/README.md:15`（F-01）

```diff
-| Flyway core / database-postgresql | 预期由 Boot `4.1.2` BOM 管理（待 `dependency:tree` 确认） | 仅 test scope；Surefire classpath 排除；Enforcer 禁止 compile/runtime；不进入 production fat jar |
+| Flyway core / database-postgresql | 已由 Boot `4.1.2` BOM 管理并解析为 `12.4.0`（R-02 已关闭） | 仅 test scope；Surefire classpath 排除；Enforcer 禁止 compile/runtime；不进入 production fat jar |
```

只改 pin cell；同一行的 test scope、Surefire `classpathDependencyExcludes`、Enforcer compile/runtime ban、production fat jar firewall 文字逐字保留。

### 2.2 `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java:607-624`（F-02）

```diff
         assertThat(queryString(
                 connection,
                 "SELECT indexdef FROM pg_indexes WHERE schemaname = 'platform' AND indexname = ?",
                 "ix_idempotency_new_alert"))
-                .contains("WHERE", "status", "'NEW'");
+                .isEqualTo("CREATE INDEX ix_idempotency_new_alert ON platform.idempotency_record "
+                        + "USING btree (created_at) WHERE (status = 'NEW'::text)");
         assertThat(queryString(
                 connection,
                 "SELECT indexdef FROM pg_indexes WHERE schemaname = 'platform' AND indexname = ?",
                 "ix_outbox_pending_available"))
-                .contains("WHERE", "status", "'PENDING'");
+                .isEqualTo("CREATE INDEX ix_outbox_pending_available ON platform.outbox_event "
+                        + "USING btree (available_at, id) WHERE (status = 'PENDING'::text)");
         assertThat(queryString(
                 connection,
                 "SELECT indexdef FROM pg_indexes WHERE schemaname = 'platform' AND indexname = ?",
                 "ix_outbox_failed_available"))
-                .contains("WHERE", "status", "'FAILED'");
+                .isEqualTo("CREATE INDEX ix_outbox_failed_available ON platform.outbox_event "
+                        + "USING btree (available_at, id) WHERE (status = 'FAILED'::text)");
```

Evidence basis:

- V001 定义：`:64` `ix_idempotency_new_alert ON platform.idempotency_record (created_at) WHERE status = 'NEW'`；`:97` `ix_outbox_pending_available ON platform.outbox_event (available_at, id) WHERE status = 'PENDING'`；`:98` `ix_outbox_failed_available ON platform.outbox_event (available_at, id) WHERE status = 'FAILED'`。
- `docs/14` §3.2 `:498` 与 §3.3 `:545-546` 冻结相同 columns 与正向 equality predicates。
- PostgreSQL 16.15 通过独立 ephemeral probe 确认 `pg_indexes.indexdef` 的精确渲染（含 `::text` cast）为上述三个完整字符串；container smoke 在真实 V001 上再次通过，证明该渲染与 V001 + `postgres:16` 一致。
- 精确 `isEqualTo` 使任何 predicate 反转（如 `WHERE (status <> 'NEW'::text)`）、额外 predicate、columns 变化、index 名称/表名变化都产生不同字符串并失败；原 `.contains` 的极性盲区已关闭。
- 断言仍复用现有 `queryString` helper 与同一 catalog 查询形状，未新增 helper、未改 V001、未扩大测试范围。

## 3. Validation commands and results

Environment（与 task-level 要求一致）：

- WSL Ubuntu `22.04.5 LTS`
- `JAVA_HOME=/tmp/temurin-21.0.13`；`PATH` 以 `/tmp/temurin-21.0.13/bin:/mnt/d/tools/maven/bin` 开头
- `java -version`: Temurin `21.0.13+9`（`21.0.13+9-LTS`）
- `mvn -v`: Apache Maven `3.9.9`，Java `21.0.13`，Eclipse Adoptium
- Docker server `29.1.3`；container PostgreSQL `16.15`
- `-Dmaven.repo.local=/mnt/d/tools/m2`

| # | Command | Result |
| --- | --- | --- |
| 1 | `cd backend && mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 clean verify` | **PASS**：`BUILD SUCCESS`；`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`；未运行 container IT |
| 2 | `mvn -B -ntp -Dmaven.repo.local=/mnt/d/tools/m2 -Pcontainer-smoke verify` | **PASS**：`BUILD SUCCESS`；`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`；PostgreSQL `16.15`；Flyway 以 `001` 应用 V001，重复 migrate 为 no-op；V901 到达 sentinel 并 rollback |
| 3 | `java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1` | **PASS**：exit `0`；日志含 `Started OneCallApplication` |
| 4 | `jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar \| grep -E 'BOOT-INF/lib/(spring-security\|flyway\|jooq\|HikariCP\|postgresql\|testcontainers)'` | **PASS**：无匹配，grep exit `1`（firewall 通过） |
| 5 | Windows git `git diff --check`（PowerShell，worktree `C:/Users/Administrator/.codex/worktrees/pa1-v001-persistence-foundation/codex-call`） | **PASS**：exit `0`；仅有 `PostgresContainerIT.java` LF→CRLF 提示，无 whitespace error |
| 6 | V001 SHA-256 | **PASS**：`946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045`，与要求的冻结值一致 |

Forbidden-file spot check（与 review artifact §Scope 的 hash 对照）：

| Path | SHA-256 | 结论 |
| --- | --- | --- |
| `backend/pom.xml` | `fbc2d9ecedc2dac942c3c460b1bfc17049d4cb5ffbdda39c7b1496ebc6ddfda5` | 与 review 记录一致，未改变 |
| `backend/src/test/resources/db/migration-broken/V901__pa1_broken_migration_fixture.sql` | `fb3b83b384f1ccf3c9bf7ed9c60e6c3413cc987dd2035dd9d062c698cf287923` | 与 review 记录一致，未改变 |
| `.github/workflows/ci.yml` | `8bf27cc2f31826c278df27dfefcbcedfe5347a8b52e923ca568126e1636837b1` | 与 review 记录一致，未改变 |
| `backend/src/main/resources/db/migration/V001__platform_schemas_roles_idempotency_outbox_audit.sql` | `946a0a13e07a4bfe583587a8cf5b1a24c56e3dcfb8cfeca2e28e84427cf81045` | 冻结值一致，未改变 |

Windows `git status --porcelain=v1` 显示的 `pom.xml`、`ci.yml`、`project-execution-control.md` 修改及 `backend/src/main/resources/db/`、`backend/src/test/resources/`、S1 implementation report、review artifact 的未跟踪状态，均为此 fix 之前已存在的 S1/controller worktree 状态；本 fix 未写入这些路径。相对 base `main` 的 worktree 本就不干净，因此以 review 记录的 hash 对照证明其内容未被本 fix 触碰。

## 4. Changed files with SHA-256

| Path | SHA-256 | Change |
| --- | --- | --- |
| `backend/README.md` | `50a37b35d734c86291f53eeebbf07665188829f1aa80b042146849a7dfa05d72` | 仅 `:15` Flyway pin cell |
| `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java` | `660f1154efa01b7073abf09cd1da50664793c6db671f4cd7bf61c0fd7988b8b0` | 仅 `:607-624` 三个 partial-index 断言 |
| `docs/gateflow/pa1-v001-persistence-foundation-s1-fix-1.md` | 本文件（自引用 hash 不嵌入） | 新增 fix artifact |

## 5. Docs decision

- 只更新 `backend/README.md` 的 stale Flyway 版本说明；不改写其他 README 段落。
- 不修改 `docs/12`、`docs/14`、plan、control document、review artifact 或 A0 contract；不新增 architecture/schema/API/deployment 文档。
- 本 fix artifact 是 gateflow 要求的 S1 fix 证据；不构成 S1 acceptance、checkpoint、re-review 结论或生产验收记录。

## 6. Residual risks and uncovered areas

1. **PostgreSQL deparse 依赖**：`F-02` 的精确字符串绑定 PostgreSQL 16 的 `pg_get_indexdef`/`pg_indexes.indexdef` 渲染（含 `::text` cast）。container tag 仍是 plan-accepted 的浮动 `postgres:16`（R-05），若未来在 PostgreSQL 17+ 上运行且 deparse 格式变化，测试需要相应更新；这是 test-only 断言风险，不影响 V001 或生产行为。
2. **仍不证明 optimizer 使用索引**：断言证明 catalog 中的 columns 与 predicate 精确正确，不证明查询计划会使用该索引；S1 inventory 合同不要求 plan/usage 证据。
3. **本 fix 只覆盖 F-01/F-02**：S2 idempotency/Outbox 行为矩阵与 S3 audit/security 行为矩阵仍未实现，不能由本 fix 或 S1 suite 声称通过。
4. **GitHub Actions 未运行**：本会话只执行本地 exact-JDK Maven、container、boot smoke、fat-jar firewall 与 Windows git 检查；未产生新的 CI run，不声称 CI 绿。
5. **独立 re-review 仍未发生**：本 artifact 是 author-side 修复与验证；controller intake 后仍需 fresh MiMo Flash re-review，且不得由本 fix 推进 S1 checkpoint。
6. **浮动的 `postgres:16` digest 未 pin**：与既有 R-05 一致，本 fix 未改变该风险状态。
7. **Pre-existing worktree dirtiness**：base `main` 与该 worktree 的 S1 未提交状态差异不是本 fix 引入；本 fix 未 stage/commit。

## 7. Final finding status

| Finding | Status | Basis |
| --- | --- | --- |
| `F-01` | **已修复** | `backend/README.md:15` 已记录 Boot `4.1.2` BOM 解析为 `12.4.0` 且 R-02 已关闭；firewall 措辞保留 |
| `F-02` | **已修复** | `PostgresContainerIT.java:607-624` 对三个 named partial index 使用完整 `indexdef` 精确相等断言；columns 与正向 equality predicate 同时被固定，反转/额外 predicate 会失败 |

## 8. Completion status

- Completion: **author_code_complete=true；bounded fix COMPLETE**。
- 本 fix 未 stage、未 commit、未 push、未创建/更新 PR、未 merge、未进入 re-review。
- 未声称 S1 accepted、checkpoint、GitHub Actions 绿灯或 production acceptance。
- Artifact path: `docs/gateflow/pa1-v001-persistence-foundation-s1-fix-1.md`。
