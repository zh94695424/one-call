# PA0 S2 实现报告 — PostgreSQL Testcontainers smoke、CI 强制执行与 A1-A5 回归接入

> Task label：`pa0-s2-implementation-boot41-ds-20261027-01`
>
> Provider / model：DeepSeek（`deepseek-flash[1m]`）
>
> Work unit：`PA0-production-runtime-bootstrap`
>
> Gate：`implementation`（仅 approved slice S2；不进入 code review/S3）
>
> Slice：S2 — PostgreSQL 16 Testcontainers smoke、`container-smoke` profile、最小 BYO CI（`.github/workflows/ci.yml`）、A1-A5 contract regressions
>
> Branch / base：`codex/pa0-production-runtime-bootstrap`；base commit `393e19f30ea0b1d164baab26ebdb5f0df4ac3e22`（S1 accepted checkpoint `56165ba` + controller bookkeeping）
>
> Author：DeepSeek（`deepseek-flash[1m]`）
>
> Date：2026-10-27（Asia/Shanghai；本地执行窗口 `2026-10-27T05:1x+08:00`）
>
> Artifact：本文件（`docs/gateflow/pa0-production-runtime-bootstrap-s2-implementation.md`）

## 0. 结论

**Status: COMPLETE（S2 author implementation；author_code_complete=true；不是 accepted slice、不是 PASS、不是生产验收）。**

- 新增唯一 container IT `PostgresContainerIT`：真实 `postgres:16` 容器 + 真实 JDBC + 版本/空 schema/无 Flyway 断言 + 容器停止；无任何 skip 路径。
- 新增唯一 `container-smoke` Maven profile：默认 `./mvnw -B -ntp verify` 仍 Docker-free（实测日志无 failsafe、无 `target/failsafe-reports`）；`-Pcontainer-smoke verify` 绑定 failsafe、`includes` 精确匹配该 IT、`failIfNoTests=true`。
- 新增最小 `.github/workflows/ci.yml`：只读权限、路径过滤、concurrency、job timeout、Temurin `21.0.13+9`、Maven 3.9.9 断言、`docker info`、`-Pcontainer-smoke verify`、failsafe/surefire XML 断言、boot jar smoke、jar firewall；`contract-regressions` 使用 Node `22.23.4`、`npm ci` 与 A2-A5 完整参数。
- `backend/README.md` 只新增 S2 命令、Docker/CI fail-closed 语义与“build/static/synthetic 不是生产验收”声明。
- author 环境 Docker 可用，**实际执行了** container smoke：两次 `-Pcontainer-smoke verify` 均 exit 0，failsafe XML `tests=1 skipped=0 failures=0 errors=0`，方法名精确匹配。该结果仅是 **author 执行证据**；`controller_container_smoke` 仍为 **PENDING**，因为本方不是 controller。
- 未 commit/stage/push、未创建/修改 PR/issue、未 merge、未切换分支、未进入 code review/S3；未修改 controller、plan/fix/review artifacts、A0-A5 assets/validators/fixtures、prototypes/screenshots。

## 1. 变更文件（仅允许路径）

| SHA-256 | 行数 | 文件 |
| --- | --- | --- |
| `11df3b9416aa74ffbe0ed563fad3db974a0c230bedfecab79a125428b49370d1` | 142 | `backend/pom.xml`（仅新增 `container-smoke` profile；S1 版本/依赖/Enforcer 未改） |
| `dc724896b87268b35d93ff39fc7c1cdc52d339e9620d7e45905d68b308ab438f` | 83 | `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`（新建） |
| `857aac2305df793ec1194a86fbca6c6f331eed4b6fc8411a75a8576b7b84f5cd` | 99 | `backend/README.md`（S2 命令 / Docker / CI / 证据边界） |
| `02831d2a7e3153e8a9f66ee936adff337047877ba0895e66464007ae5bfc61d2` | 170 | `.github/workflows/ci.yml`（新建） |
| self | — | `docs/gateflow/pa0-production-runtime-bootstrap-s2-implementation.md`（本报告；自身 SHA-256 在 completion response 记录） |

范围证明：

- `git status --porcelain -- backend .github`：` M backend/README.md`、` M backend/pom.xml`、` M backend/mvnw.cmd`（**预存 dirty，本任务未触碰**）、`?? .github/`、`?? backend/src/test/java/com/onecall/archtests/container/`。
- `backend/mvnw.cmd` 在任务开始时即为 ` M`；本任务未编辑、未重新生成 wrapper。
- 未创建 Dockerfile/compose、migration、schema、domain/security/API 代码、额外 CI 脚本或 `package.json`/lockfile 变更。
- `api/openapi/node_modules/` 与 `tmp/openapi-client/` 为 A1 生成物，均已被 `.gitignore` 忽略；未产生新的未忽略仓库路径。

## 2. 工具链证据（实际执行）

| 项 | 结果 |
| --- | --- |
| `backend/.java-version` | 精确内容 `21.0.13+9` |
| `java -version` | `openjdk version "21.0.13" 2026-10-20 LTS`；`Temurin-21.0.13+9 (build 21.0.13+9-LTS)` |
| `./mvnw -B -ntp -v` | `Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)`；`Java version: 21.0.13, vendor: Eclipse Adoptium`；`Maven home: /root/.m2/wrapper/dists/apache-maven-3.9.9/3477a4f1` |
| `node --version` / `npm --version` | `v22.23.4` / `10.9.9` |
| `docker version` | client `29.1.3` / server `29.1.3`；`docker info` exit 0 |
| Maven wrapper pin | `wrapperVersion=3.3.2`、`distributionType=only-script`、`distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip`、`distributionSha256Sum=4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c`（S1 固定，本任务复核并实际经 wrapper 校验运行；retrieval date 2026-10-27） |
| 执行环境 | 系统 PATH Java 为 `21.0.12.1+1`，**未用于任何权威命令**；所有 Maven 命令显式 `JAVA_HOME=/tmp/pa0-tools/jdk-21.0.13+9` |

Resolved dependency tree（`./mvnw -B -ntp dependency:tree -Dincludes=org.springframework.boot:*,org.springframework.modulith:*,org.testcontainers:*,org.postgresql:*`，exit 0）：

```text
org.springframework.boot:spring-boot:jar:4.1.2:compile
org.springframework.modulith:spring-modulith-starter-core:jar:2.1.2:compile
org.springframework.modulith:spring-modulith-api:jar:2.1.2:compile
org.springframework.modulith:spring-modulith-apt:jar:2.1.2:compile
org.springframework.modulith:spring-modulith-core:jar:2.1.2:runtime
org.springframework.modulith:spring-modulith-moments:jar:2.1.2:compile
org.testcontainers:testcontainers:jar:2.0.5:test
org.postgresql:postgresql:jar:42.7.14:test
```

- `forbidden_resolved_versions_absent=true`：全 scope `dependency:list` 扫描 `spring-boot*:(3.|4.0.)` / `spring-modulith*:(1.|2.0.)`，匹配数 `0`。
- Boot support（2026-10-27 重新读取 `api.spring.io/projects/spring-boot/generations/4.1.x`，HTTP 200）：`support=default`、`ossSupportEndDate=2027-07-31`、`commercialSupportEndDate=2028-07-31`、`latestPatch.oss=4.1.2`。
- Modulith support（`api.spring.io/projects/spring-modulith/generations/2.1.x`）：`support=none`、无 `ossSupportEndDate`、`latestPatch.oss=2.1.2`、`linkedGenerations.spring-boot=["4.1.x"]`（即 `ACTIVE_UNSUPPORTED`/`support=none`，见 `PA0-R-14`）。
- 版本证据状态：author executed（S2 命令与解析均在本会话实际执行）。

## 3. Container IT 设计（无 skip 路径）

`backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`：

- `GenericContainer<>(DockerImageName.parse("postgres:16"))`，核心 Testcontainers `2.0.5` + manual lifecycle（无 `@Testcontainers` extension、无 `PostgreSQLContainer`、无 junit-jupiter 模块）。
- `POSTGRES_USER=onecall`、`POSTGRES_DB=onecall`、`POSTGRES_PASSWORD=<每次 JVM 运行 UUID>`；密码不落盘、不写日志、不是生产凭据。
- `withExposedPorts(5432)`、`Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2)`、`withStartupTimeout(Duration.ofMinutes(2))`；无 volume、无固定 host port、无 reuse。
- `@BeforeAll POSTGRES.start()`；`@AfterAll POSTGRES.stop()`；启动失败即测试失败，无 `Assumptions`/`@Disabled`/`disabledWithoutDocker`/环境变量 skip。
- 唯一测试方法 `connectsToPostgresAndFindsNoApplicationSchema`：JDBC `jdbc:postgresql://<host>:<mappedPort>/onecall`，try-with-resources；断言
  1. `SELECT current_database(), current_user` → `onecall` / `onecall`；
  2. `SHOW server_version_num` → 整数 `>= 160000`；
  3. `information_schema.tables`（排除 `pg_catalog`/`information_schema`）计数 `= 0`；
  4. `to_regclass('public.flyway_schema_history') IS NULL` 为 true。

`backend/pom.xml` 仅新增：

```xml
<profiles>
  <profile>
    <id>container-smoke</id>
    ...
    <artifactId>maven-failsafe-plugin</artifactId>
    <configuration>
      <includes><include>**/PostgresContainerIT.java</include></includes>
      <failIfNoTests>true</failIfNoTests>
    </configuration>
    <executions><execution><goals>
      <goal>integration-test</goal><goal>verify</goal>
    </goals></execution></executions>
  </profile>
</profiles>
```

failsafe 版本由 Boot `4.1.2` parent 管理（实际 `3.5.6`），未另写版本；其余 S1 dependency/Enforcer/plugin 内容未改。

## 4. 命令与 exit code（实际执行）

| 命令 | Exit | 关键结果 |
| --- | --- | --- |
| `./mvnw -B -ntp clean verify`（默认，无 profile） | 0 | surefire `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；日志中 `failsafe` 出现次数 `0`；`target/failsafe-reports` **不存在** → 默认 verify Docker-free |
| `./mvnw -B -ntp -Pcontainer-smoke verify`（第 1 次，冷拉镜像） | 0 | `PostgresContainerIT` 1 test；pull `postgres:16`（16 layers，153 MB）；容器启动并停止；failsafe `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| `./mvnw -B -ntp -Pcontainer-smoke verify`（第 2 次，镜像已缓存） | 0 | 同一 IT 4.226s 通过；`BUILD SUCCESS`；可复现 |
| failsafe XML 解析 | — | `tests=1 skipped=0 failures=0 errors=0`；testcase=`connectsToPostgresAndFindsNoApplicationSchema` |
| surefire XML 解析 | — | `OneCallApplicationTests` 1/0/0/0；`BootstrapDependencyBoundaryTests` 1/0/0/0；`ModuleBoundaryTests` 8/0/0/0（全部 skipped=0） |
| `timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` | 0 | `Started OneCallApplication in 1.337 seconds` |
| `jar tf …` + forbidden grep | 0 / no match | `BOOT-INF/lib/` 27 项；`spring-security|flyway|jooq|HikariCP|postgresql|testcontainers` 匹配 `0` |
| `./mvnw -B -ntp dependency:tree …` | 0 | 见 §2 |
| `./mvnw -B -ntp dependency:list` + forbidden grep | 0 / 0 matches | 无 Boot 3.x/4.0.x、Modulith 1.x/2.0.x |
| `git diff --check -- backend/pom.xml backend/README.md` | 0 | 无 whitespace error |
| `git diff --no-index --check /dev/null <new file>`（IT / ci.yml） | 1（预期：文件有差异） | 输出 0 bytes，无 whitespace error |

## 5. Semantic assertions observed

```text
default_verify_docker_free=true
default_verify_tests=10 skipped=0
container_profile_activates_failsafe=true
container_tests=1
container_skipped=0
container_failures=0
container_errors=0
container_method=connectsToPostgresAndFindsNoApplicationSchema
current_database=onecall
current_user=onecall
server_version_num>=160000=true
application_tables=0
flyway_history_absent=true
fail_if_no_tests=true
no_skip_api=true (@Disabled/Assumptions/disabledWithoutDocker 均未使用)
boot_log_started=true
jar_exit=0
runtime_jar_forbidden_libs=0
```

模块边界（S1 语义未改，本 slice 回归确认）：`module_set_exact=true`、`annotation_present=14/14`、`allowed_map_exact=true`、`discovered_edges_empty=true`、`cycle_rejected=true`、`forbidden_rejected=true`、`internal_rejected=true`、`forbidden_frameworks_absent=true`、`no_datasource_flyway_security_beans=true`。

## 6. A1-A5 regression outputs（Node `v22.23.4`，实际执行）

| 面 | 命令（从仓库根） | Exit | 关键输出 |
| --- | --- | --- | --- |
| A1 | `( cd api/openapi && npm ci && npm run check:s3 )` | 0 | `PASS check-doc-map (s3)`；`operations=49 customer=18 operations=21 ivr=6 public-share=3 provider-callback=1 total=49`；`doc_missing=0 doc_extra=0 doc_duplicate=0 doc_invalid=0`；`a0_catalog=44 a0_missing=0` |
| A2 | `node protocol/ivr/validate.mjs --stage s3 …` | 0 | `PASS protocol-validation (s3)`；`fixtures=25 passed=25 failed=0`；`a1_operations=49 ivr_operations=6`；`a0_tables=44`；`a1_drift=0` |
| A3 | `node security/validate.mjs --stage s3 …` | 0 | `PASS security-validation (s3)`；`fixtures=23 passed=23 failed=0`；`drift=0` |
| A4 | `node deployment/capacity/validate.mjs --stage s2 …` | 0 | `PASS capacity-validation stage=s2`；`fixtureCases=113 passed=113 failed=0`；`productionReady=false procurementReady=false cloudVerified=false a3RiskClosed=false` |
| A5 s1 | `node acceptance/validate.mjs --stage s1 …` | 0 | `PASS acceptance-validation (s1)`；`cells=96 represented=90 not_applicable=6 deferred=0`；`runtime_evidence=NOT_RUN production_accepted=false` |
| A5 s2 | `node acceptance/validate.mjs --stage s2 … --check-doc-links` | 0 | `PASS acceptance-validation (s2)`；同计数；`evidence_manifests=0 evidence_admitted=0 acceptance_records=0`；`runtime_evidence=NOT_RUN production_accepted=false`；doc-link 检查通过 |

A1 bundle `tmp/openapi-client/onecall-v1.bundle.json` 由 `check:s3` 重新生成后供 A2-A5 使用；未修改任何 A0-A5 asset/validator/fixture/count，未伪造 bundle。

## 7. CI 静态检查（workflow 未真实运行）

`.github/workflows/ci.yml` 静态断言（PyYAML 解析 + grep）：

- YAML 可解析；`jobs=['contract-regressions', 'java-runtime']`；`permissions={'contents': 'read'}`；job `timeout-minutes` = `[30, 25]`。
- `push` 仅 `branches: [main]`；`push`/`pull_request` paths 均为 `backend/**`、`.github/workflows/ci.yml`、`api/openapi/**`、`protocol/**`、`security/**`、`deployment/capacity/**`、`acceptance/**`、`docs/*.md`。
- 全文无 `prototype`、`screenshots`、`shared/`、`logo/`、`figma` 触发路径。
- `java-runtime`：`actions/setup-java@v4` + `distribution: temurin` + `java-version: '21.0.13+9'`；断言 `.java-version`、`Apache Maven 3.9.9`、`Java version: 21.0.13`、`21.0.13+9`；先 `docker info`，再 `./mvnw -B -ntp -Pcontainer-smoke verify`；Python 断言恰好 1 个 failsafe report、`tests=1`、`skipped=0`、`failures=0`、`errors=0`、方法名匹配、surefire 无 skip；boot jar smoke 与 `jar tf` firewall。
- `contract-regressions`：`actions/setup-node@v4` + `node-version: '22.23.4'` + `node --version | grep -Fx v22.23.4`；`working-directory: api/openapi` 下 `npm ci`；`npm run check:s3`；A2/A3/A4/A5 s1/A5 s2 使用 §8.3 完整参数（含 `--a1-bundle`、`--a0`、A2/A3/A4 context 与 `--check-doc-links`）。
- 未使用 YAML anchors/aliases；未缓存 `node_modules`/`tmp`；`cache` 只指向 `api/openapi/package-lock.json`。
- **实际 GitHub Actions run：NOT RUN**（未 push、未建 PR，author 不触发 CI）。不得声称 CI 绿；post-push CI 不能替代 controller pre-push container run。

## 8. Slice completion state（三态，互不替代）

| 状态 | 值 | 说明 |
| --- | --- | --- |
| S2 author_code_complete | **true** | 允许路径内文件全部存在；默认 verify、container smoke（author 环境）、A1-A5、YAML/命令/路径静态检查均实际执行通过 |
| S2 controller_container_smoke | **PENDING** | 本 author 环境已实际执行 `-Pcontainer-smoke verify` 两次并 exit 0（failsafe `1/0/0/0`），但 S2 accepted checkpoint 的唯一前置是 **controller 在其 Docker-capable 环境独立执行**；本方不是 controller，故不填 PASS |
| S2 post_push_ci_container_smoke | **NOT RUN** | 未 commit/push/PR；无真实 GitHub Actions run；exact reason：author Gate 不触发 CI |

## 9. Residual owner/destination reconciliation（PA0-R-01..14，逐项，无遗漏）

| ID | 状态 | Owner → Destination |
| --- | --- | --- |
| `PA0-R-01` | 已由 S1 accepted 的 `backend/**` resolve/tree/test-compile/verify 关闭；本 S2 复核仍绿 | Platform/CI → controller review |
| `PA0-R-02` | 已关闭：wrapper 3.3.2 + Maven 3.9.9 URL/SHA-256 固定并实际校验 | Platform/CI → controller review |
| `PA0-R-03` | author 已实际执行 container smoke 两次（exit 0）；**仍开放至 controller 独立 pre-push run** | Platform/CI → **controller Docker-capable `-Pcontainer-smoke verify` 是 S2 accepted checkpoint 唯一前置**；daemon 不可用则停止请求启动 Docker |
| `PA0-R-04` | 未关闭：GitHub Actions 首次真实运行 | Platform/CI → PA0 PR/controller gate；未运行前不得声称绿 |
| `PA0-R-05` | 未关闭：`postgres:16` floating tag，digest 未 pin；本 S2 明确 test-only | Backend/Platform → S2/CI hardening（后续 pin digest） |
| `PA0-R-06` | 已复核：Boot 4.1.x OSS 至 `2027-07-31`（2026-10-27 重新读取）；仍在窗口内 | Platform/Security → 后续 patch 跟踪；EOL 则上报 release governance |
| `PA0-R-07` | 未关闭：Actions 用 major tag，非 commit SHA | Security/Platform → CI hardening/release governance |
| `PA0-R-08` | 未关闭：path-filter 与 branch protection required checks 交互 | Platform/CI → repository governance |
| `PA0-R-09` | 未关闭：platform/audit/sharing/evidence cross-cutting Java 边未预授权 | Backend/Architecture → first domain work unit + architecture review |
| `PA0-R-10` | 已关闭对本 slice：精确 Temurin `21.0.13+9` 已选择并执行 | Platform/CI → 后续执行环境保持 pin |
| `PA0-R-11` | 持续披露：本地/CI 绿 ≠ 生产验收 | Backend/QA/Product → README/§9.5 + A5/release governance |
| `PA0-R-12` | 未关闭：无 formatter/checkstyle | Backend/Platform → 后续 code-quality work unit |
| `PA0-R-13` | 未关闭：allowedDependencies 是 prose 翻译上限 | Architecture → architecture change control |
| `PA0-R-14` | **仍为 governance residual**：Spring Modulith `2.1.x` `ACTIVE_UNSUPPORTED` / `support=none`，无 published OSS/商业支持窗口；本 S2 记录 resolved `2.1.2` 并跟踪 2.1.x patch，**未静默替换** | Platform/Security + release governance → controller 在 revised plan review 显式接受 |

## 10. Honesty / scope statements

- 只提供 build/static/synthetic 证据（Maven 构建、Enforcer、Modulith 边界、container smoke、A1-A5 静态/合成回归）；**不证明** carrier/SIP/CDR、IVR media、腾讯云、provider、load、restore、penetration、manual 或 production acceptance；未创建 A5 evidence manifest/authority/acceptance record，`productionAccepted=false` 语义不变。
- 只修改允许路径；预存 dirty paths（含 `backend/mvnw.cmd`、prototypes/screenshots/docs/reviews、`.codex-tasks/`）未被本任务触碰。
- 未 commit/stage/push、未创建/修改 PR/issue、未 merge、未切换分支、未进入 code review/S3；`docs/gateflow/project-execution-control.md` 未改，由 controller 记录 gate 状态。
- 未声称 GitHub Actions 已运行；未声称 S2 accepted。

## 11. Open questions / blockers

无 plan-scope blocker。开放项仅为已披露 residual：controller container run（`PA0-R-03`，S2 accepted 前置）、post-push CI 首次运行（`PA0-R-04`）、`postgres:16` digest pin（`PA0-R-05`）、Actions SHA pinning/branch protection（`PA0-R-07`/`R-08`）、Modulith `support=none` 治理接受（`PA0-R-14`）。

## 12. 完成状态

- **S2 author implementation：COMPLETE / author_code_complete=true**。
- `controller_container_smoke=PENDING`；`post_push_ci_container_smoke=NOT RUN`；`actual GitHub run=NOT RUN`。
- 未标记 S2 accepted；下一步由 controller 执行 pre-push container run 并 review。
