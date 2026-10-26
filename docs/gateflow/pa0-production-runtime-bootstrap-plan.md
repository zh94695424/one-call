# PA0 生产运行时引导实施计划

> Work unit：`PA0-production-runtime-bootstrap`
>
> Gate：`plan`（只形成 code-generation-ready 实施计划；不实施、不进入 plan review 之后的 Gate）
>
> Branch：`codex/pa0-production-runtime-bootstrap`
>
> Base：A5 head `1686a22ac9f7715b876ac233d7202badccfd0569`；GitHub PR #7 merge commit 与 A5 head 同树且包含该 head 为父提交。
>
> Binding inputs：`docs/12-technology-stack-and-system-architecture.md` §5、§6、§14、§15 Phase A；`docs/gateflow/project-execution-control.md` 顶部 A5 binding closeout 与 next entry；`docs/14`、`docs/15`、`docs/16`、`docs/17`、`docs/18`、`docs/19` 及其 `api/`、`protocol/`、`security/`、`deployment/`、`acceptance/` machine-readable assets；`docs/gateflow/a5-production-acceptance-matrix-spec-final-closeout.md`。
>
> 本计划只创建本文件；不修改 controller、source、tests、workflow、其他 docs、screenshots、`.codex-tasks/`、`tmp/` 或 review artifacts；不 stage/commit/push、不创建/修改 PR/issue、不执行其他 Gateflow Gate。

## 0. 结论摘要

- 交付一个**单 Maven 项目、单可启动 Spring Boot 应用、14 个 Spring Modulith 包边界、无 domain 行为**的运行时引导脚手架。14 个边界 = `docs/12` §6 的 13 个模块 + `docs/14` §3.1 的 `platform-reliability`；只创建包边界、`package-info.java`、`ModuleMarker` 和验证测试，不创建表、状态机、API、migration、认证或供应商适配器。
- Modulith 验证必须非空：测试断言**精确 14 个模块**、每个模块包有 `@ApplicationModule`、从每个 package-info 读取的 `allowedDependencies()` 与 §6.2 单一 expected map **exact set equality**（含 leaf 显式 `{}` deny-all，拒绝默认 `OPEN_TOKEN`）、`verify()` 无 cycle/forbidden/internal 违规，并用 test-only fixture 证明 cycle、禁止依赖和 internal package 访问确实被拒绝。测试代码放在应用根包之外，避免测试类被误识别成第 15 个模块。
- PostgreSQL 路径使用 Testcontainers core + PostgreSQL JDBC driver（均 test scope）启动真实 `postgres:16` 容器，执行真实 JDBC 连接，并断言 `server_version_num >= 160000`、`information_schema` 无业务表、无 `flyway_schema_history`。默认 `verify` 不运行该 IT；只有显式 `-Pcontainer-smoke` 才运行；Docker 不可用时构建失败，不 skip。author 可报 `NOT RUN`（仅 author code-complete）；S2 accepted checkpoint 前必须由 controller 在其 Docker-capable 环境 pre-push 执行并通过；CI 先 `docker info`，再运行该 profile，并解析 failsafe XML 断言 `tests=1 skipped=0 failures=0 errors=0`，作为 post-push 附加证据。
- 工具链固定为 Java `21.0.13+9`（Temurin，`backend/.java-version` + CI `setup-java` 精确 pin；`maven.compiler.release=21`、Enforcer `[21,22)`）、Maven 3.9.9（Maven Wrapper 3.3.2 + 固定 distribution URL + SHA-256）、Node 22.23.4（CI 精确 pin；不修改 `api/openapi/package.json` 的 `>=22` 语义）。Node 22.23.4 已由本会话 `node --version` 独立验证；Boot/Modulith/Testcontainers 的 controller-observed 版本在 §4 明确标注为**待 S1 preflight 证明**，本 plan 不把 controller observation 冒充 author proof。
- 只新增一个最小 GitHub Actions workflow：两个 job（Java 运行时 job、A1-A5 contract regression job），只读权限，路径过滤排除 prototypes/screenshots，concurrency/timeout 仅作为 CI 机制。CI 成功只等于 build/static/synthetic 检查通过，不等于 carrier/IVR/Tencent Cloud/provider/load/restore/penetration/manual/production acceptance。
- 两个 behaviorally bounded slices：**S1 Java/Maven 运行时、14 模块边界与依赖防火墙（无 Docker）**；**S2 PostgreSQL Testcontainers smoke、CI 强制执行与 A1-A5 回归接入**。不按文件、技术层、文档或团队拆分，不增加 docs-only slice。
- 无产品/架构 scope blocker。存在明确的实现前置证据项：Boot/Modulith/Testcontainers 独立兼容性验证、Maven 3.9.9 zip SHA-256、Docker/CI 实际执行环境；它们按 owner/destination 记录并在 S1/S2 preflight fail closed，不授权 invent 版本、凭据、阈值或 acceptance authority。

## 1. 目标、动机、成功信号、非目标、边界与目标对齐

### 1.1 目标

在 `1686a22` 基础上建立 Phase A 的第一个有界生产平台基础，使后续 domain work 有一个可重复的 Java 运行时、可执行的模块边界、受支持的工具链和最小 CI/Testcontainers 路径：

1. 建立 Java 21 + Maven + Spring Boot 3.x + Spring Modulith 的可启动、可测试单模块脚手架；
2. 把 `docs/12` §6 与 `docs/14` §3.1 的模块所有权落实为 Java 包边界，并有可执行验证；
3. 建立最小应用入口，不实现任何 domain state machine；
4. 建立真实 PostgreSQL Testcontainers 连接 smoke，并定义 fail-closed 的本地/CI 执行策略；
5. 固定 Java/Maven/Node 22 工具链，建立最小 GitHub CI，运行 Java build/module/container smoke 与现有 A1-A5 contract regressions；
6. 保证 CI 成功语义严格限定为 build/static/synthetic 验证，不产生生产验收声明。

### 1.2 动机与第一性判断

1. **静态契约不能证明运行时会启动。** 当前仓库只有 A0-A5 文档、machine assets 和 Node validators；没有 `pom.xml`、Maven/Gradle wrapper、Java 源码、Dockerfile/compose 或 `.github/workflows`。因此 “Java 21 + Spring Boot + Modulith 可构建/可启动” 目前只是设计声明，不是可执行事实。
2. **模块边界必须在 domain code 之前冻结。** `docs/12` §6 要求模块所有权、同步调用走公开应用服务、异步协作走领域事件、跨模块查询用只读投影；若等 domain work 再建立，包漂移、循环依赖和越界访问会成为既成事实。最小可执行 enforcement 是：精确模块集 + package boundary + allowed dependency cap + cycle/internal/forbidden 负向证明。
3. **最小运行时不应吸收任何 unauthored behavior。** 加入 Spring Security、Flyway、jOOQ、Hikari 或真实 DataSource auto-config 会在没有认证/表/迁移设计的情况下改变启动行为。PA0 只证明“应用能启动”和“数据库能连通”，不证明“应用已能处理业务”。
4. **“未运行”不能等于“通过”。** 本地无 Docker 是合法开发场景，但 CI 若允许 skip 就会把“没有执行”包装成“成功”。因此容器测试只能通过显式 profile 执行；CI 必须先证明 Docker 存在，再执行测试，并在执行后解析报告证明 `skipped=0`。author `NOT RUN` 只构成 author code-complete；S2 accepted checkpoint 必须由 controller pre-push Docker run 关闭，后续 CI 通过不能追溯替代它。
5. **工具链版本是 foundation 的一部分，但未证明的版本不能被猜测。** Node 22.23.4 已在本会话执行 `node --version` 验证；Boot/Modulith/Testcontainers 的 controller-observed 元数据在 author 网络/执行权限被拒绝时只能作为**provisional pin + fail-closed preflight**，不能作为 author-proven compatibility。

### 1.3 成功信号

1. `backend/pom.xml` + `backend/mvnw` 可在 Java `21.0.13+9`（Temurin）/ Maven 3.9.9 下完成 `./mvnw -B -ntp verify`，生成 `onecall-runtime-0.0.1-SNAPSHOT.jar`，应用 context 启动且 jar 进程以 0 退出。
2. `ApplicationModules.of(OneCallApplication.class)` 精确发现 14 个模块；`verify()` 通过；test-only fixtures 证明 cycle、forbidden dependency、internal package access 分别被拒绝；每个模块 `package-info.java` 均有 `@ApplicationModule`。
3. `OneCallApplication` 是唯一 production entry；context 无 DataSource、Flyway、SecurityFilterChain bean；test classpath 无 Flyway/Security/jOOQ/Hikari 库。
4. `./mvnw -B -ntp -Pcontainer-smoke verify` 在 Docker 可用时启动真实 PostgreSQL 16 容器，JDBC 连接成功，并断言无 application table、无 Flyway history、`server_version_num >= 160000`；Docker 不可用时该命令失败，不 skip。author 环境可将 author Docker 记为 `NOT RUN`（exact reason），但 **S2 accepted-slice checkpoint 前必须由 controller 在其 Docker-capable 环境实际执行并通过**；controller daemon 不可用时 Gateflow 停止并请求启动 Docker，不得接受/提交 S2，也不得把该 primary proof 推迟到后续 PR/CI run。
5. `.github/workflows/ci.yml` 在 `backend/**`、contract assets 或顶层 `docs/*.md` 变化时运行；Java job 执行 module/unit/container smoke 和报告断言；contract job 以 Node 22.23.4 执行 A1-A5 回归；prototype/screenshot-only changes 不触发。真实 GitHub Actions run 属 **post-push 附加证据**，不能替代第 4 条的 controller pre-push container run。
6. CI 输出和 README 明确声明：以上只证明 build/static/synthetic，不证明 carrier、IVR media、Tencent Cloud、provider、load、restore、penetration、manual 或 production acceptance；不创建 A5 evidence manifest/authority/acceptance record，不设置 `productionAccepted=true`。

### 1.4 非目标与精确边界

- 不实现 A0 的 44 表、V001-V009 Flyway migrations、schema/role/grant/function/trigger。
- 不实现 account/login、activation、catalog/order/payment、entitlement、IVR、evidence、sharing、AI、notary、refund、invoice、support 等任何 domain 行为。
- 不集成 IVR、WeChat、COS、KMS、Redis、RabbitMQ、PostgreSQL production instance、Tencent Cloud 或 provider SDK。
- 不建设 SBOM/image publishing、Kubernetes、deployment environments、performance/load tests 或 production release pipeline。
- 不发明 production credentials、concurrency、retention、SLO/RPO/RTO、Region/AZ/SKU、cloud capability 或 acceptance authority values。
- 不把 HTML prototypes 或 `shared/onecall-business.js` 当作 production foundation。
- 不新增/修改 A0-A5 public contract、validator 语义、fixtures、counts、review artifacts 或 `docs/12`-`docs/19`。
- 不创建 parent/aggregator POM、async-worker/scheduler Maven module、Dockerfile/compose、frontend、formatter/checkstyle 或额外 CI 平台；这些不在本 goal 内。

### 1.5 目标对齐映射

| Goal contract element | Plan 落点 | 验证方式 |
| --- | --- | --- |
| Java 21（精确 Temurin `21.0.13+9` pin）+ Maven + Boot 3.x/Modulith scaffold | §4、§5 | `.java-version`/`java -version`、wrapper/Maven version、`mvn verify`、jar boot |
| 模块包边界 + 可执行 Modulith 验证 | §6 | 精确 14 模块集 + annotation + `verify()` |
| cycle/forbidden/internal 能被捕获 | §6.5 | 3 组负向 fixtures 必须抛 `Violations` |
| 最小入口，无 domain state machine | §5.4、§11 S1 | 只有 `main`/config/markers；无 domain package code |
| PostgreSQL Testcontainers 连接 smoke | §7 | 真实容器 + JDBC + SQL 断言 |
| 本地 skip 明确、CI 必须执行 | §7.4、§9 | profile-only、无 skip API、XML `skipped=0` |
| Java/Maven/Node 22 pin | §4 | wrapper 3.9.9、Enforcer、CI Node 22.23.4 |
| 最小 CI + A1-A5 regressions | §8、§9 | 两 job、精确命令、期望计数 |
| CI 不声称生产验收 | §9.5、§13 | README/job 语义、无 evidence ledger |
| 无 schema/migration/domain/cloud | §1.4、§10、§14 | 文件 scope + runtime jar firewall |
| 有界 slices | §11 | 2 个 behavior/evidence 边界 |
| 不 invent、不漂移 | §13、§14 | typed open questions + residual owner/destination |

## 2. 直接仓库证据与 A0-A5 对齐

### 2.1 当前仓库事实

| 事实 | 精确证据 | 对本计划的影响 |
| --- | --- | --- |
| 无 Java/Maven 生产工程 | 当前 HEAD 与工作树不存在 `pom.xml`、Gradle 文件、Maven/Gradle wrapper、`Dockerfile*`、`docker-compose*` | PA0 是新建 scaffold，不是改造既有服务 |
| 无 GitHub Actions | `.github` 目录不存在 | CI 是新建最小 workflow，不修改既有 pipeline |
| Node contract harness 已存在 | `api/openapi/package.json`：`engines.node >=22`；`check:s3` 等脚本；`package-lock.json` 存在 | 保留 package/scripts/lock 语义；CI 以 Node 22.23.4 执行 `npm ci` + `check:s3` |
| A0 已冻结数据模型 | `docs/14`：44 表、9 migration waves、V001-V009、模块/schema 所有权矩阵 | PA0 不创建任何表/migration；模块包名映射 schema，但不是 schema 实现 |
| A1-A4 静态资产和 validators 存在 | `api/openapi/`、`protocol/ivr/validate.mjs`、`security/validate.mjs`、`deployment/capacity/validate.mjs`、`acceptance/validate.mjs` | CI 复用既有命令，不修改 validator/fixtures |
| A5 已接受静态矩阵但无 CI | `docs/gateflow/a5-production-acceptance-matrix-spec-final-closeout.md`：无 repository checks；Node `>=22` 未 pin；A1-A5 需在 CI 运行 | PA0 精确关闭该 residual 的 CI/toolchain 部分，但不关闭 A5 runtime evidence |
| Prototypes 不是 production foundation | `docs/12` §14.2；`prototype/`、`web-prototype/`、`operations-prototype/`、`shared/onecall-business.js` | 不引用、不修改、不纳入 CI |
| 分支与 base | 当前 branch `codex/pa0-production-runtime-bootstrap`，HEAD `1686a22ac9f7715b876ac233d7202badccfd0569` | 所有实现 slice 以该 base 为起点；不合并 PR #7、不切换分支 |
| 预存 dirty paths | 任务开始时已有大量 modified screenshots/docs/prototypes 及 untracked `.codex-tasks/`、`docs/reviews/plan-review-20261021-205118.md` | 全部属于用户；PA0 不 stage/commit/修改；implementation 只改本计划允许路径 |

### 2.2 设计与 A0-A5 对齐矩阵

| 上游 | 约束 | PA0 落点 | 不在 PA0 内 |
| --- | --- | --- | --- |
| `docs/12` §5 | `core-api`/`async-worker`/`scheduler` 同源多模块；首发模块化单体，不强制 K8s | 先建立单一可执行 Java 项目；worker/scheduler 后续同一代码库再加 | async-worker、scheduler、K8s、镜像发布 |
| `docs/12` §6 | 13 个模块及允许依赖；禁止跨模块表写、循环、供应商 SDK 进入核心 domain | 14 个包边界（含 `platform-reliability`）+ allowed dependency cap + 负向测试 | 任何 domain 服务、表写、adapter、事件流 |
| `docs/12` §14.1 | JUnit 5、AssertJ、Spring Modulith Tests、Testcontainers PostgreSQL | S1 JUnit/AssertJ/Modulith；S2 Testcontainers | Web/Playwright/IVR/security/load 测试 |
| `docs/12` §14.2 | CI 做格式、静态、单元、模块、迁移、OpenAPI、漏洞等门禁 | 最小 build/module/container/A1-A5 CI | formatter、SBOM、image scan、deploy、migration |
| `docs/12` §15 Phase A | 建立 Java 模块化单体、PostgreSQL/Flyway、Outbox、审计、OpenAPI、四认证边界 | 只建立运行时/模块/Testcontainers/CI 基线 | 44 表、Flyway、Outbox、审计、认证、OpenAPI runtime |
| `docs/14` §2.1/§3.1 | schema/role 所有权；`platform-reliability` 是 platform schema 的 owning module | 增加 `platform_reliability` 包边界；包名对齐 schema 名 | schema、role、grant、function、table |
| `docs/15` | 49 operations 是静态 OpenAPI contract | 不启动 HTTP server、不加 operation；A1 静态回归保持 | controller/API implementation |
| `docs/16` | IVR protocol 是 fixture evaluator，不是 runtime | 不实现 IVR message/event/state | IVR adapter、call flow |
| `docs/17` | 五信任面/认证域/密钥/凭证/审计是安全设计 | 不加入 Security starter；不创建 auth domain/credential | 登录、session、mTLS、KMS、COS policy |
| `docs/18` | 部署/容量/恢复/云能力值待批准 | CI timeout/concurrency 只作为 CI 机制；不使用 SLO/RPO/RTO | region/AZ/SKU、备份、容量、云验证 |
| `docs/19` | 静态矩阵/validator 不能产生 runtime PASS；`authorityRegistry=[]` | CI 成功不写 evidence/authority/record；`productionAccepted=false` | 真实验收执行、authority 定义 |
| A5 closeout | 无 CI；Node 22 未 pin；A1-A5 需在 CI 运行 | §8/§9 精确修复 | A5 其余 residual |

### 2.3 本计划不重新决定的上游事实

- 不重新选择模块名、schema 名、表、状态、API、Problem code、IVR message、安全域、容量公式或验收 authority。
- 不把 `docs/12` §6 的 “允许依赖” 文字改写成新的产品规则；§6.2 只把它翻译成**同步 Java 边界的上限**，并明确 A0 的跨 schema `SELECT` grant 不是 Java module dependency。
- 不把 `docs/12` §5 的 multi-module 目标提前实现为空的 worker/scheduler 模块；PA0 只有一个 deployment artifact，后续第二个 deployment unit 出现时才拆分。
- 不把 Testcontainers/CI 的通过写成 A5 runtime evidence；A5 的 `runtimeEvidence=NOT_RUN`/`productionAccepted=false` 语义保持不变。

## 3. 产物布局与受影响文件

### 3.1 目录布局

```text
backend/
  .gitignore
  .java-version                                                  # 精确 JDK pin: 21.0.13+9
  .mvn/wrapper/maven-wrapper.properties
  mvnw
  mvnw.cmd
  pom.xml
  README.md
  src/main/java/com/onecall/core/OneCallApplication.java
  src/main/java/com/onecall/core/<module>/package-info.java        # 14 个
  src/main/java/com/onecall/core/<module>/ModuleMarker.java        # 14 个
  src/main/resources/application.yml
  src/test/java/com/onecall/archtests/OneCallApplicationTests.java
  src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java
  src/test/java/com/onecall/archtests/architecture/BootstrapDependencyBoundaryTests.java
  src/test/java/com/onecall/archtests/container/PostgresContainerIT.java
  src/test/java/com/onecall/archfixtures/cycle/CycleFixtureRoot.java
  src/test/java/com/onecall/archfixtures/cycle/alpha/{Alpha.java,ModuleMarker.java,package-info.java}
  src/test/java/com/onecall/archfixtures/cycle/beta/{Beta.java,ModuleMarker.java,package-info.java}
  src/test/java/com/onecall/archfixtures/forbidden/ForbiddenFixtureRoot.java
  src/test/java/com/onecall/archfixtures/forbidden/consumer/{Consumer.java,ModuleMarker.java,package-info.java}
  src/test/java/com/onecall/archfixtures/forbidden/provider/{Provider.java,ModuleMarker.java,package-info.java}
  src/test/java/com/onecall/archfixtures/internal/InternalFixtureRoot.java
  src/test/java/com/onecall/archfixtures/internal/consumer/{Consumer.java,ModuleMarker.java,package-info.java}
  src/test/java/com/onecall/archfixtures/internal/provider/{Provider.java,ModuleMarker.java,package-info.java}
  src/test/java/com/onecall/archfixtures/internal/provider/internal/Internal.java
.github/workflows/ci.yml
docs/gateflow/pa0-production-runtime-bootstrap-s1-implementation.md      # 未来 S1 报告
docs/gateflow/pa0-production-runtime-bootstrap-s2-implementation.md      # 未来 S2 报告
```

- 不创建 root `pom.xml`、root `mvnw`、parent/aggregator、`core-api/`/`async-worker/`/`scheduler/` Maven modules、`Dockerfile`、compose、frontend、`docs/20` 或 root README。
- 不创建 `.mvn/wrapper/maven-wrapper.jar`；使用 Maven Wrapper 3.3.2 `only-script` 模式。
- `backend/.gitignore` 只包含 `target/`，避免生成物进入版本控制。
- `backend/README.md` 是本地工具链/命令/证据边界的唯一新增说明；不新增 docs 索引行、第二 spec 或 acceptance 文档。

### 3.2 文件作用

| 文件 | 作用 | 是否 production runtime |
| --- | --- | --- |
| `backend/pom.xml` | Boot parent、Modulith BOM、精确依赖、Enforcer、failsafe profile | 构建 |
| `backend/.java-version` | 精确 JDK pin `21.0.13+9`（Temurin）；本地工具（jenv/asdf）可读 + CI `grep -Fx` 交叉断言 | 构建 |
| `backend/.mvn/wrapper/maven-wrapper.properties` | Maven 3.9.9 distribution URL + SHA-256 + wrapper 3.3.2 | 构建 |
| `backend/mvnw` / `mvnw.cmd` | 可复现 Maven 入口；CI 不依赖 runner 预装 Maven | 构建 |
| `backend/src/main/java/com/onecall/core/OneCallApplication.java` | 唯一 `main`/`@SpringBootApplication` | 是 |
| 14 个 `package-info.java` | `@ApplicationModule` 边界与 allowed dependency cap | 是（元数据） |
| 14 个 `ModuleMarker.java` | 确保 Modulith 可检测每个模块；package-private、无行为 | 是（边界标记） |
| `backend/src/main/resources/application.yml` | 应用名与 `web-application-type: none`；无 datasource/security | 是 |
| 三个 main test classes | context/boot、Modulith 正向/负向、dependency firewall | test |
| 3 组 fixture packages | 证明 cycle/forbidden/internal 被拒绝 | test-only |
| `PostgresContainerIT.java` | 真实 PostgreSQL 连接与空 schema 断言 | test-only |
| `.github/workflows/ci.yml` | 两 job 最小 CI，含报告断言与 A1-A5 | CI |
| `backend/README.md` | 本地/CI 命令、版本、skip 语义、非验收声明 | 文档 |
| 两份 implementation reports | 每个 slice 的完成报告 | Gateflow |

### 3.3 不创建的文件

- 不创建 `src/main/java/**` 下除 `OneCallApplication`、14 个 `package-info`、14 个 `ModuleMarker` 之外的任何 production type。
- 不创建 `db/migration`、`schema.sql`、`data.sql`、JPA entity、repository、controller、filter、security config、DataSource config、Flyway config、jOOQ config。
- 不创建 root `package.json`、workspace、`api/openapi` 以外的 Node 工程，不修改 `api/openapi/package.json`/`package-lock.json`。
- 不创建 Dockerfile/compose、K8s manifest、SBOM、release workflow、deployment environment、secrets。

## 4. 工具链、版本与兼容性证据

### 4.1 版本 pin

| 组件 | Pin | 用途 | 证据状态（2026-10-26） |
| --- | --- | --- | --- |
| Java | **Temurin `21.0.13+9`**（OpenJDK version `21.0.13+9-LTS`）；`maven.compiler.release=21`；Enforcer `[21,22)` | 编译/运行 | controller 于 2026-10-26 以参数化查询 Adoptium assets/feature-release API（`https://api.adoptium.net/v3/assets/feature_releases/21/ga?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=linux&vendor=eclipse&page_size=5`）获得 HTTP 200，release entry 返回 `release_name=jdk-21.0.13+9`、`openjdk_version=21.0.13+9-LTS` 及 LTS metadata；本地既有 `21.0.12.1+1` **不是**所选 pin，仅作诊断；S1 前必须由执行环境选择/安装精确 JDK 并记录 `java -version` |
| Maven | 3.9.9，经 Maven Wrapper；Enforcer `[3.9.9,4.0)` | 构建 | controller 观测 Maven 3.9.9；本会话 `mvn -version` 被权限门拒绝；wrapper preflight 必须证明 |
| Maven Wrapper | 3.3.2，`distributionType=only-script` | 可复现 Maven 入口 | author-known GA；S1 preflight resolve/校验 |
| Node | 22.23.4 精确；CI `actions/setup-node` 使用该版本 | A1-A5 contract harness | **author verified**：本会话 `node --version` = `v22.23.4` |
| Spring Boot | 3.5.16 | parent/BOM、Spring runtime | controller 观测 Maven Central GA；author 网络验证被权限门拒绝；S1 preflight 必须证明 |
| Spring Modulith | 1.4.14（1.4.x 对应 Boot 3.5 family） | module verification | controller 观测 Maven Central GA；author 网络验证被拒绝；S1 preflight 必须证明 |
| Testcontainers | 2.0.5（`org.testcontainers:testcontainers` core only） | PostgreSQL container smoke | controller 观测 Maven Central GA；author 网络验证被拒绝；S1/S2 preflight 必须证明 |
| PostgreSQL JDBC | versionless；由 Spring Boot 3.5.16 BOM 管理 | test-scope JDBC | S1 preflight dependency tree 记录实际版本 |
| PostgreSQL image | `postgres:16`（test-only） | container smoke | 只声明 design family “16+”；不是 production DB 版本承诺；digest 未 pin（见残留下沉） |
| Maven Enforcer Plugin | 3.5.0 | toolchain/dependency firewall | author-known GA；S1 preflight resolve |
| GitHub Actions | `actions/checkout@v4`、`actions/setup-java@v4`、`actions/setup-node@v4` | CI mechanics | 只使用官方 major tag；commit-SHA pinning 是 CI hardening residual |

### 4.2 证据状态与诚实限制

- **AUTHOR_VERIFIED**：Node 22.23.4（`node --version`，本会话；运行时存在）。
- **CONTROLLER_OBSERVED**：Java 21.0.12.1（既有安装，**仅诊断，不是所选 pin**）、Temurin `21.0.13+9`（Adoptium feature-release API，2026-10-26，LTS）、Maven 3.9.9、Docker CLI 29.7.2/daemon not running、Spring Boot 3.5.16、Spring Modulith 1.4.14、Testcontainers 2.0.5（controller 于 2026-10-26 观测）。本会话的 `curl`、`git ls-remote`、`node -e`、`mvn`、`java`、`docker`、WebFetch/WebSearch 均被权限门或网络策略拒绝；因此这些条目不是 author proof。
- **PREFLIGHT_REQUIRED**：S1 实现开始前必须用真实 Maven dependency resolution 证明 Boot/Modulith/Testcontainers/Enforcer 可解析，并记录 exact dependency tree、Boot/Modulith compatibility、Maven zip SHA-256 和 Boot 3.5 support status；同时必须在执行环境选择/安装精确 Temurin `21.0.13+9` 并以 `java -version` / `./mvnw -v` 证明 patch `21.0.13` 与 build `+9`。既有 `21.0.12.1+1` 的运行结果只能作诊断，不能作为 pin 证据。S2 必须由 controller 在其 Docker-capable 环境 pre-push 实际执行 container smoke。
- 若 preflight 失败，实现停止并报告；不得替换成未证明的版本、preview/RC/milestone/snapshot，也不得通过删除测试/Enforcer 规则绕过。

### 4.3 兼容性 preflight（S1 第一步，fail closed）

从 `backend/` 执行：

```bash
./mvnw -B -ntp -v
./mvnw -B -ntp -U dependency:resolve
./mvnw -B -ntp dependency:tree \
  -Dincludes=org.springframework.boot:*,org.springframework.modulith:*,org.testcontainers:*,org.postgresql:*
```

必须断言：

1. `./mvnw -v` 显示 Maven 3.9.9 和 Java `21.0.13`；`java -version` 同时包含 `21.0.13` 与 Temurin build `+9`（`21.0.13+9`），且 `backend/.java-version` 内容为精确 `21.0.13+9`。执行环境无法解析/安装该精确 release 时，S1 为 fail-closed blocker，不得回退到 `21`、`21.0.12.1+1` 或 latest；
2. `dependency:resolve` exit 0，且 dependency tree 精确包含 `spring-boot 3.5.16`、`spring-modulith-* 1.4.14`、`testcontainers 2.0.5`；PostgreSQL driver 版本来自 Boot BOM；
3. `dependencyConvergence` 与 `banDuplicatePomDependencyVersions` 在 `verify` 中通过；
4. Boot 3.5.16 的官方 support/security 状态被记录；若已超出仍受支持窗口，停止并报告，不得静默升级到 Boot 4.x（超出 goal 的 “Boot 3.x”）；
5. 任何解析/兼容/收敛失败都不得用 “latest” 或猜测版本修复；只能由 controller 提供经证明的替代 pin。

### 4.4 Maven Wrapper 可复现性

`backend/.mvn/wrapper/maven-wrapper.properties` 必须固定：

```properties
wrapperVersion=3.3.2
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
distributionSha256Sum=<S1 preflight 从官方 Apache Maven 3.9.9 bin zip 计算的 SHA-256>
```

- `<S1 preflight ...>` 不是占位符留到生产；S1 必须在实现报告记录该值、zip URL、retrieval date 和校验命令。若无法取得/校验 SHA-256，S1 停止为 blocked，不得省略 checksum。
- 生成方式：在 controller 的 Maven 3.9.9 下执行 `mvn -N wrapper:wrapper -Dmaven=3.9.9 -Dtype=only-script`（或等价官方 wrapper 生成器），提交生成的 `mvnw`/`mvnw.cmd`/properties；随后把 properties 固定为上述值并加入校验和。不得手写未经生成的 wrapper script。
- CI 只调用 `./mvnw`，不依赖 runner 预装 Maven；`./mvnw -v` 的版本断言是 CI 的 toolchain proof。
- `mvnw`/`mvnw.cmd` 必须可执行（POSIX exec bit）；不提交 wrapper jar（`only-script`）。

### 4.5 JDK 精确 pin 文件与执行环境选择

- 新增一个最小 pin 文件 `backend/.java-version`，唯一内容一行：`21.0.13+9`。
- 位置选择 `backend/` 而非仓库根：唯一 Java 构建在 `backend/`，根目录承载 Node contract harness（`api/openapi`）；root `.java-version` 会让 Node-only 工作误以为需要 JDK。`backend/.java-version` 是本地工具（jenv/asdf 等）可读的提示，不构成安装器。
- CI 的权威 pin 仍由 `actions/setup-java@v4` 的 `distribution: temurin` + 精确 `java-version: '21.0.13+9'` 决定（**不使用 `check-latest`**），并用 `grep -Fx '21.0.13+9' .java-version` 与 `java -version` / `./mvnw -v` 断言交叉核对。
- **不**新增 JDK installer script、不写全局安装 action；JDK 选择/供给属于执行环境（S1/controller/CI runner）职责。任何环境无法解析/安装 `jdk-21.0.13+9` 都是 fail-closed S1 blocker，不是可猜测或可延后的项。

## 5. Maven/Java 运行时设计

### 5.1 单 Maven 模块决策

- `backend/` 是一个单 Maven 项目（`packaging=jar`），不是 parent/aggregator，也不预建 `async-worker`/`scheduler` Maven 子模块。
- 理由：confirmed goal 只有一个最小可启动/可测试 entry；`docs/12` §5 的 “同源多模块” 是 deployment units 未来形态，不是 PA0 需要立即创建的空模块。空的 worker/scheduler 模块会增加 POM/CI/Gate 成本，却不产生独立行为或证据。模块边界由 Spring Modulith package modules 在现在冻结；未来第二个 deployment artifact 出现时再把 `backend/` 重整为 parent + children。
- 包根：`com.onecall.core`；模块包名使用 A0 schema 名（下划线），如 `customer_iam`、`platform_reliability`。这是内部 code namespace，不是 public API，也不是生产域名声明；未来可在任何 external contract 之前 bounded rename。

### 5.2 依赖清单与 dependency firewall

`backend/pom.xml` 只允许以下依赖：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.modulith</groupId>
  <artifactId>spring-modulith-starter-core</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>testcontainers</artifactId>
  <version>${testcontainers.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
```

- **Spring Security**：不加入 `spring-boot-starter-security`，不写 SecurityFilterChain；因此不存在默认登录页/401/403 意外阻断。`BootstrapDependencyBoundaryTests` 断言 `org.springframework.security.config.annotation.web.builders.HttpSecurity` 不在 test classpath；context test 断言无 `springSecurityFilterChain` bean。
- **Flyway**：不加入 Flyway；不存在 `FlywayAutoConfiguration` 可用的 library；无 `db/migration`。container smoke 断言无 `public.flyway_schema_history`。
- **jOOQ/Hikari**：不加入 jOOQ、HikariCP、`spring-boot-starter-jdbc`、`spring-boot-starter-data-jpa`；不创建 DataSource bean。因此启动不尝试连接数据库，也不会自动执行 schema 行为。
- **PostgreSQL driver**：只 test scope；production fat jar 不应包含 `BOOT-INF/lib/postgresql-*.jar`。CI 用 `jar tf` 断言。
- **Testcontainers**：只 test scope；只用 core artifact + manual lifecycle，不使用 `@Testcontainers` extension、`junit-jupiter` module 或 `postgresql` module，减少版本/坐标耦合。
- 没有 web starter/actuator：应用不是 HTTP server；不新增任何 A1 operation。
- Enforcer `bannedDependencies`（`searchTransitive=true`）禁止：`org.springframework.boot:spring-boot-starter-security`、`spring-boot-starter-data-jpa`、`spring-boot-starter-jdbc`、`org.springframework.security:*`、`org.flywaydb:*`、`org.jooq:*`、`com.zaxxer:HikariCP`。这是 PA0 的有意防火墙；未来 domain work 加入这些依赖时必须显式修改规则并重新评审，而不是让 auto-config 悄悄进入。

### 5.3 POM 属性、插件与 Enforcer

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.5.16</version>
  <relativePath/>
</parent>
<groupId>com.onecall</groupId>
<artifactId>onecall-runtime</artifactId>
<version>0.0.1-SNAPSHOT</version>
<properties>
  <java.version>21</java.version>
  <maven.compiler.release>21</maven.compiler.release>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <spring-modulith.version>1.4.14</spring-modulith.version>
  <testcontainers.version>2.0.5</testcontainers.version>
</properties>
```

- `dependencyManagement` 只 import `spring-modulith-bom:1.4.14`；Testcontainers core 用显式版本，不 import TC BOM，避免把无关 managed versions 带入 convergence。
- 上述 test-scope Testcontainers/PostgreSQL 依赖在 **S1 POM** 中声明（不执行），使 S1 version preflight 能解析并记录 2.0.5；S1 不创建 container IT/profile，S2 只增加 IT 与 profile。这样 S1 的 `verify` 仍不需要 Docker。
- `maven-enforcer-plugin:3.5.0`：`requireJavaVersion [21,22)`、`requireMavenVersion [3.9.9,4.0)`、`banDuplicatePomDependencyVersions`、`dependencyConvergence`、上述 `bannedDependencies`。
- `spring-boot-maven-plugin` 继承 Boot parent 版本；执行 `repackage` 生成可执行 fat jar。
- `maven-compiler-plugin`/surefire/jar 版本由 Boot parent 管理；不另造版本。
- 不加入 formatter/checkstyle/Spotless；goal 未要求，且新增工具链会增加未证明的依赖/规则。`ModuleBoundaryTests` + Enforcer + compiler release 是本 slice 的 static/build 检查。
- 无 `maven-failsafe-plugin` 的默认绑定；它只在 `container-smoke` profile 内声明（§7.4）。

### 5.4 应用入口与配置

```java
package com.onecall.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OneCallApplication {
    public static void main(String[] args) {
        SpringApplication.run(OneCallApplication.class, args);
    }
}
```

`src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: onecall-runtime
  main:
    web-application-type: none
```

- 无 `server.*`、datasource、flyway、security、profiles、secrets、cloud 配置。
- `web-application-type: none` 是显式启动契约：即使未来误加 web starter，PA0 的 boot smoke 仍不会默默开放端口；未来需要 HTTP 时必须显式修改并评审。
- `@SpringBootApplication` 扫描 `com.onecall.core.**`，只发现 14 个 marker/package-info 与 app class；没有 domain bean。

### 5.5 启动/失败行为

- `./mvnw -B -ntp verify` 必须完成 compile + surefire + jar package；无 Docker 依赖。
- `java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` 在无 DB/网络/凭据环境下启动 context，输出 `Started OneCallApplication`，因无 web server/非 daemon 线程而退出 0。
- 任一 Modulith 违规、Enforcer 违规、missing module/annotation、forbidden framework、context 启动失败都使 build 非 0；不得 catch/swallow、不得 `@Disabled`、不得用空模块让测试变绿。

## 6. Spring Modulith 边界与验证

### 6.1 14 个模块与包名

| Architecture/owner name（`docs/12` §6 / `docs/14` §3.1） | Java package | A0 schema（未实现） | Source |
| --- | --- | --- | --- |
| `customer-iam` | `com.onecall.core.customer_iam` | `customer_iam` | docs/12 §6 |
| `organization-access` | `com.onecall.core.organization_access` | `organization_access` | docs/12 §6 |
| `operations-iam` | `com.onecall.core.operations_iam` | `operations_iam` | docs/12 §6 |
| `subject-number` | `com.onecall.core.subject_number` | `subject_number` | docs/12 §6 |
| `activation-consent` | `com.onecall.core.activation_consent` | `activation_consent` | docs/12 §6 |
| `ivr` | `com.onecall.core.ivr` | `ivr` | docs/12 §6 |
| `catalog-order` | `com.onecall.core.catalog_order` | `catalog_order` | docs/12 §6 |
| `entitlement-metering` | `com.onecall.core.entitlement_metering` | `entitlement_metering` | docs/12 §6 |
| `evidence` | `com.onecall.core.evidence` | `evidence` | docs/12 §6 |
| `sharing` | `com.onecall.core.sharing` | `sharing` | docs/12 §6 |
| `ai-orchestration` | `com.onecall.core.ai_orchestration` | 无 A0 表 | docs/12 §6 |
| `notary` | `com.onecall.core.notary` | 无 A0 表 | docs/12 §6 |
| `operations-audit` | `com.onecall.core.operations_audit` | `audit` | docs/12 §6 |
| `platform-reliability` | `com.onecall.core.platform_reliability` | `platform` | docs/14 §3.1（binding 补充） |

- 14 个模块都是 root package 的直接子包；`OneCallApplication` 在 root，不是模块。
- 每个模块只有 `package-info.java` + package-private `ModuleMarker`；没有 domain type/表/API。
- `ai_orchestration`/`notary` 没有 A0 表，但仍按 `docs/12` §6 建立边界，避免后续 AI/notary work 新建第 15 个包时漂移。
- `platform_reliability` 来自 A0 ownership matrix，不是 `docs/12` §6 的第 13 个 domain module；把它纳入是为了让所有 A0 owning module 都有边界。它同样无表/无 Outbox 实现。

### 6.2 allowedDependencies 表

`allowedDependencies` 只表达**同步 Java 模块依赖的上限**，不是 A0 的跨 schema `SELECT`/grant 列表。A0 的双向只读 grant（如 audit 读 employee、catalog 读 entitlement）不自动变成 Java 边；未来的 read-only collaborator 必须通过 owner service/projection 或经评审的边界修订实现，不能制造 Java cycle。

| Module | allowedDependencies | 依据 |
| --- | --- | --- |
| `customer_iam` | `{}` | 允许依赖是基础安全/供应商接口，不是 module；PA0 无实现 |
| `organization_access` | `{customer_iam}` | “customer-iam 的稳定账户 ID” |
| `operations_iam` | `{operations_audit}` | “审计接口” |
| `subject_number` | `{}` | 账户 ID 可空；号码验证适配器不是 module |
| `activation_consent` | `{subject_number}` | docs/12 §6 |
| `ivr` | `{subject_number, activation_consent, entitlement_metering, evidence}` | 主体、开通、试用/权益预占、证据应用接口 |
| `catalog_order` | `{customer_iam, subject_number}` | 账户/主体稳定 ID；支付适配器不是 module |
| `entitlement_metering` | `{catalog_order, subject_number}` | 商品/订单快照、主体稳定 ID |
| `evidence` | `{entitlement_metering}` | 权益应用接口；对象存储适配器不是 module |
| `sharing` | `{evidence}` | 证据只读投影；媒体授权接口不是 module |
| `ai_orchestration` | `{evidence, entitlement_metering}` | 证据只读版本、权益应用接口 |
| `notary` | `{evidence}` | 证据不可变快照；供应商适配器不是 module |
| `operations_audit` | `{}` | 只接收审计事件；无 outgoing Java edge |
| `platform_reliability` | `{}` | A0 platform owner；无 outgoing domain edge |

- 该表无 cycle；PA0 代码也没有跨模块调用，因此 `verify()` 现在是 acyclic/empty-edge 状态。负向 fixtures 证明一旦加入非法边，验证会失败。
- **未编码的 cross-cutting 边**：A0 的 platform idempotency/outbox/audit 调用、sharing 的 workspace 读取、evidence 的 subject/ivr 读取等未来实现，不在 PA0 预授权；对应 work unit 必须显式修订本表并重新评审。不得为了“以后方便”预先放宽 allowedDependencies。
- **1.4.14 deny-all 语义（已核实，closed）**：`org.springframework.modulith.ApplicationModule.allowedDependencies()` 默认值为 `{ OPEN_TOKEN }`，其中 `OPEN_TOKEN = "¯\\_(ツ)_/¯"`，表示 unrestricted；其 published Javadoc 明确 “Declaring an empty array will allow no dependencies to other modules. To not restrict the dependencies at all, leave the attribute at its default value.” Core `ApplicationModule.getAllowedDependencies()` 只把 singleton `OPEN_TOKEN` 当作 open，其余一律构造 closed allowed dependencies，因此**显式 `allowedDependencies = {}` 是 deny-all**。证据：Spring Modulith `1.4.14` published source/Javadoc（controller 于 2026-10-26 检视）。本表所有空集合行（`customer_iam`、`subject_number`、`operations_audit`、`platform_reliability`）必须写显式 `{}`，绝不能用 bare `@ApplicationModule` 默认值。若 S1 实际解析到的 Modulith 版本对该显式形式的 published 行为与 `1.4.14` 不一致，S1 fail closed 并上报，不得改用 unrestricted、删除负向 fixture 或降低断言。

### 6.3 注解、探测与非空性

每个模块 `package-info.java` 形式：

```java
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = {"subject_number", "activation_consent", "entitlement_metering", "evidence"}
)
package com.onecall.core.ivr;
```

- 无 outgoing edge 的模块**必须**写显式 `allowedDependencies = {}`（deny-all）。**禁止** bare `@ApplicationModule`：其默认 `{ OPEN_TOKEN }` 是 unrestricted（见 §6.2 已核实语义）。leaf 形式：
  ```java
  @org.springframework.modulith.ApplicationModule(allowedDependencies = {})
  package com.onecall.core.customer_iam;
  ```
- `ModuleMarker.java` 每个包一个，package-private、无方法/字段：
  ```java
  final class ModuleMarker {
      private ModuleMarker() {
      }
  }
  ```
- **非空性四重保证**：
  1. exact module set：若删除一个 marker，模块从集合消失，测试失败；
  2. annotation presence：若 package-info 缺 `@ApplicationModule`，测试失败；
  3. exact allowedDependencies map：从每个 package-info 反射读取 `@ApplicationModule.allowedDependencies()`，与 §6.2 单一 expected map 做 exact set equality（含 leaf 空集，并拒绝默认 `OPEN_TOKEN`），见 §6.4；
  4. negative fixtures：若 verifier 不执行 cycle/forbidden/internal 检查，测试失败。
- **测试包隔离**：所有 test classes 放在 `com.onecall.archtests`，fixtures 放在 `com.onecall.archfixtures`；**禁止**把测试类放在 `com.onecall.core.**`。否则 test-classes 会被 `ApplicationModules.of(OneCallApplication.class)` 当作额外模块或内部类型，污染边界。`OneCallApplicationTests` 使用 `@SpringBootTest(classes = OneCallApplication.class, webEnvironment = NONE)` 显式指定配置。

### 6.4 正向验证测试

`com.onecall.archtests.architecture.ModuleBoundaryTests`：

```java
private static final String OPEN_TOKEN = "¯\\_(ツ)_/¯";

private static final Set<String> EXPECTED_MODULES = Set.of(
    "customer_iam", "organization_access", "operations_iam", "subject_number",
    "activation_consent", "ivr", "catalog_order", "entitlement_metering",
    "evidence", "sharing", "ai_orchestration", "notary",
    "operations_audit", "platform_reliability"
);

// 单一 expected map，逐项镜像 §6.2 表（含 leaf 空集）
private static final Map<String, Set<String>> EXPECTED_ALLOWED = Map.ofEntries(
    Map.entry("customer_iam", Set.of()),
    Map.entry("organization_access", Set.of("customer_iam")),
    Map.entry("operations_iam", Set.of("operations_audit")),
    Map.entry("subject_number", Set.of()),
    Map.entry("activation_consent", Set.of("subject_number")),
    Map.entry("ivr", Set.of("subject_number", "activation_consent",
        "entitlement_metering", "evidence")),
    Map.entry("catalog_order", Set.of("customer_iam", "subject_number")),
    Map.entry("entitlement_metering", Set.of("catalog_order", "subject_number")),
    Map.entry("evidence", Set.of("entitlement_metering")),
    Map.entry("sharing", Set.of("evidence")),
    Map.entry("ai_orchestration", Set.of("evidence", "entitlement_metering")),
    Map.entry("notary", Set.of("evidence")),
    Map.entry("operations_audit", Set.of()),
    Map.entry("platform_reliability", Set.of())
);

@Test
void discoversExactlyTheFrozenModuleSet() {
    ApplicationModules modules = ApplicationModules.of(OneCallApplication.class);
    assertThat(modules.stream().map(Module::getName))
        .containsExactlyInAnyOrderElementsOf(EXPECTED_MODULES);
}

@Test
void everyExpectedModuleDeclaresApplicationModule() throws Exception {
    for (String name : EXPECTED_MODULES) {
        Class<?> marker = Class.forName(
            "com.onecall.core." + name + ".ModuleMarker", false,
            getClass().getClassLoader());
        assertThat(marker.getPackage().isAnnotationPresent(ApplicationModule.class))
            .as(name + " must declare @ApplicationModule")
            .isTrue();
    }
}

@Test
void assertsExactAllowedDependencyMapFromPlan62() throws Exception {
    for (Map.Entry<String, Set<String>> expected : EXPECTED_ALLOWED.entrySet()) {
        Class<?> marker = Class.forName(
            "com.onecall.core." + expected.getKey() + ".ModuleMarker", false,
            getClass().getClassLoader());
        ApplicationModule annotation =
            marker.getPackage().getAnnotation(ApplicationModule.class);
        assertThat(annotation)
            .as(expected.getKey() + " must declare @ApplicationModule")
            .isNotNull();
        Set<String> actual =
            new HashSet<>(Arrays.asList(annotation.allowedDependencies()));
        assertThat(actual)
            .as(expected.getKey() + " must not use default OPEN_TOKEN")
            .doesNotContain(OPEN_TOKEN);
        assertThat(actual)
            .as(expected.getKey() + " allowedDependencies must equal plan §6.2")
            .containsExactlyInAnyOrderElementsOf(expected.getValue());
    }
}

@Test
void discoveredProductionEdgesStayInsideTheConfiguredMap() {
    ApplicationModules modules = ApplicationModules.of(OneCallApplication.class);
    Map<String, Set<String>> discovered = new HashMap<>();
    modules.stream().forEach(module -> discovered.put(
        module.getName(),
        module.getDirectDependencies(modules)
            .map(Module::getName)
            .collect(Collectors.toSet())));
    discovered.forEach((name, targets) -> assertThat(EXPECTED_ALLOWED.get(name))
        .as(name + " discovered edges must stay within allowedDependencies")
        .containsAll(targets));
    assertThat(discovered.values().stream().flatMap(Set::stream))
        .as("PA0 has no cross-module code edges yet")
        .isEmpty();
}

@Test
void verifiesNoCyclesOrBoundaryViolations() {
    ApplicationModules.of(OneCallApplication.class).verify();
}
```

- `EXPECTED_ALLOWED` 是 §6.2 表的**唯一** executable 镜像；新增/删除/改写表中任一值都必须同步此 map，否则测试失败。
- exact-map test 直接读取每个 package-info 的 `allowedDependencies()`：`ivr` 若被 widen（加 `catalog_order`）、narrow（删 `subject_number`）、omit（bare 默认 `OPEN_TOKEN`）或 change（`subject_number`→`catalog_order`），`containsExactlyInAnyOrderElementsOf` 都会失败；leaf 的默认 `OPEN_TOKEN` 还会被显式 `doesNotContain` 拒绝。
- `discoveredProductionEdgesStayInsideTheConfiguredMap` 用 Modulith discovered direct dependency 集合作 subset 断言：先要求每个模块的 discovered edges ⊆ configured map，再要求全局 discovered edges 为空（当前只有 marker，无跨模块 code edge）。这不会削弱上面的 configured-map exact equality。
- 实现必须在 S1 报告记录 **4 个稳定 mismatch probes** 的结果（每个 probe 修改一处 package-info、跑 `ModuleBoundaryTests` 必须失败、恢复后必须绿）：(a) widen `ivr` + `catalog_order`；(b) narrow `ivr` - `subject_number`；(c) leaf `customer_iam` 改 bare `@ApplicationModule`（默认 `OPEN_TOKEN`）；(d) change `organization_access` `customer_iam`→`ivr`。不得只记录 report hash。
- `Module.getName()`（`org.springframework.modulith.core.Module`）在没有 `displayName` 时返回模块简单名；本计划不设置 displayName，避免 name/allowedDependencies 语义分叉。
- 若 1.4.14 的 API 名称不同，实现可使用等价 API（含 `getDirectDependencies` 的等价形式），但必须保持 exact module set、annotation presence、exact allowed map、discovered-subset 四类断言；不得降低为 `not empty` 或 `count >= 14`。
- `verify()` 同时检查 cycle、allowed dependencies 与 CLOSED module internal access。

### 6.5 负向 fixtures（必须真实失败）

| Fixture root | 结构 | 期望 |
| --- | --- | --- |
| `com.onecall.archfixtures.cycle` | `alpha` 与 `beta` 互相依赖；两个 package-info 分别允许对方，隔离出 cycle 检测 | `ApplicationModules.of(CycleFixtureRoot.class).verify()` 抛 `Violations` |
| `com.onecall.archfixtures.forbidden` | `consumer` 的 `package-info` 为**显式** `@ApplicationModule(allowedDependencies = {})`（deny-all，禁止 bare 默认）；`Consumer` 引用 `provider.Provider` 公开根类型 | `verify()` 抛 `Violations`，证明显式 `{}` deny-all 生效且 consumer→provider 边被拒绝 |
| `com.onecall.archfixtures.internal` | `consumer` 允许 `provider`；`Consumer` 引用 `provider.internal.Internal` | `verify()` 抛 `Violations`，证明 CLOSED/internal 边界生效 |

测试形式：

```java
assertThatThrownBy(() -> ApplicationModules.of(CycleFixtureRoot.class).verify())
    .isInstanceOf(Violations.class);
```

- 不 assert 具体 message（避免 API 文案漂移）；fixture 结构本身保证违规类型，未抛异常即测试失败。cycle fixture 中 alpha/beta 互相允许且无 internal/非法边，因此任何 `Violations` 只能来自 cycle 检测。
- Fixtures 都在 `src/test/java`，不在 app root 下；不会进入 production jar。
- forbidden fixture 的 consumer **必须**显式 `allowedDependencies = {}`（§6.2 已核实的 deny-all 形式）。若它不抛 `Violations`，说明实际解析的 Modulith 版本 published 行为偏离 `1.4.14`（explicit `{}` 应视为 closed），S1 必须 fail closed 上报；不得删除测试、放宽断言或改为 unrestricted。该 fixture 同时是“explicit `{}` 下 consumer→provider 边必须失败”的负向证明。

### 6.6 dependency firewall 测试

`com.onecall.archtests.architecture.BootstrapDependencyBoundaryTests`：

```java
private static final List<String> FORBIDDEN = List.of(
    "org.flywaydb.core.Flyway",
    "org.springframework.security.config.annotation.web.builders.HttpSecurity",
    "org.jooq.DSLContext",
    "com.zaxxer.hikari.HikariDataSource"
);

@Test
void forbiddenFrameworksAreAbsentFromTheBootstrapClasspath() {
    for (String type : FORBIDDEN) {
        assertThat(ClassUtils.isPresent(type, getClass().getClassLoader()))
            .as(type + " must not be on the PA0 classpath")
            .isFalse();
    }
}
```

`OneCallApplicationTests` 额外断言：

```java
assertThat(context.containsBean("dataSource")).isFalse();
assertThat(context.containsBean("flyway")).isFalse();
assertThat(context.containsBean("springSecurityFilterChain")).isFalse();
```

- Test classpath 包含 test-scope PostgreSQL driver/Testcontainers，这是允许的；不 assert 它们不存在。
- production fat jar 的 test-scope 泄漏由 §9.3 的 `jar tf` 断言关闭。
- 未来 domain work 若合法加入 Security/Flyway/jOOQ/Hikari，必须修改防火墙和本测试，属于显式评审变更；PA0 不允许它们悄悄出现。

## 7. PostgreSQL Testcontainers 路径

### 7.1 依赖与容器

- Test scope：`org.testcontainers:testcontainers:2.0.5` + Boot-managed `org.postgresql:postgresql`。
- 使用 `org.testcontainers.containers.GenericContainer` + `DockerImageName.parse("postgres:16")`；**不使用** `PostgreSQLContainer`、`@Testcontainers` extension 或 `org.testcontainers:junit-jupiter`，减少 2.x artifact/package 耦合。
- 容器环境：`POSTGRES_USER=onecall`、`POSTGRES_DB=onecall`、`POSTGRES_PASSWORD=<UUID per JVM run>`；密码在测试运行时用 `UUID.randomUUID().toString()` 生成，仅存在于容器/JDBC 进程内，不是生产凭据、不落盘、不进日志。
- 无 volume、无固定 host port、无 reuse；Testcontainers 映射随机端口并在测试结束后停止容器。

### 7.2 生命周期与等待

`com.onecall.archtests.container.PostgresContainerIT`：

```java
static final String PASSWORD = UUID.randomUUID().toString();
static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse("postgres:16"))
    .withEnv("POSTGRES_USER", "onecall")
    .withEnv("POSTGRES_DB", "onecall")
    .withEnv("POSTGRES_PASSWORD", PASSWORD)
    .withExposedPorts(5432)
    .waitingFor(Wait.forLogMessage(
        ".*database system is ready to accept connections.*\\n", 2))
    .withStartupTimeout(Duration.ofMinutes(2));

@BeforeAll
static void startContainer() {
    POSTGRES.start();
}

@AfterAll
static void stopContainer() {
    POSTGRES.stop();
}
```

- 手动 `start()/stop()`：容器启动失败时 `@BeforeAll` 失败，测试失败；不存在 assumption/`@Disabled`/`disabledWithoutDocker` 的 skip 路径。
- `Wait` 使用官方镜像启动日志的 2 次 ready（临时 init server + 最终 server），避免过早连接。
- 默认 `verify` 不绑定 failsafe，因此该 IT 不运行、不要求 Docker；显式 `-Pcontainer-smoke` 才运行（§7.4）。

### 7.3 SQL 断言（连接/版本/无 schema/无 Flyway）

唯一测试方法：`connectsToPostgresAndFindsNoApplicationSchema()`。

JDBC URL：

```text
jdbc:postgresql://<containerHost>:<mappedPort>/onecall
```

断言：

1. `SELECT current_database(), current_user` 返回 `onecall` / `onecall`；
2. `SHOW server_version_num` 解析为整数且 `>= 160000`（PostgreSQL 16+ design family）；
3. `SELECT count(*) FROM information_schema.tables WHERE table_schema NOT IN ('pg_catalog','information_schema')` 等于 `0`，证明 PA0 没有创建业务表；
4. `SELECT to_regclass('public.flyway_schema_history') IS NULL` 为 true，证明没有 Flyway 自动启动/迁移历史；
5. 连接/查询使用 try-with-resources；失败直接抛出，测试失败。

- 不创建 schema/table/function/role，不执行 DDL，不读取 production DB。
- 该测试只证明“真实 PostgreSQL 16 container 可连接且为空”；不证明 A0 表、约束、锁、事务、RPO/RTO 或生产数据库能力。

### 7.4 本地 skip / CI fail-closed 策略

`backend/pom.xml` 的 `container-smoke` profile：

```xml
<profile>
  <id>container-smoke</id>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-failsafe-plugin</artifactId>
        <configuration>
          <includes>
            <include>**/PostgresContainerIT.java</include>
          </includes>
          <failIfNoTests>true</failIfNoTests>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>integration-test</goal>
              <goal>verify</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</profile>
```

- 默认 `./mvnw -B -ntp verify`：不运行 container IT，不要求 Docker；README 明确说明这不是“容器 smoke 通过”。
- `./mvnw -B -ntp -Pcontainer-smoke verify`：必须运行 `PostgresContainerIT`；Docker 不可用/镜像拉取失败/容器启动失败 => build 非 0。**没有** `@Testcontainers(disabledWithoutDocker=true)`、`Assumptions`、`@Disabled` 或环境变量 skip。
- `maven-failsafe-plugin` 版本优先由 Boot parent 管理；若 S1/S2 preflight 显示 Boot parent 未管理该插件，必须在 POM 中固定 preflight 解析到的 GA 版本并记录。`failIfNoTests=true` 确保 profile 激活但测试未被匹配时失败。
- 若 author 环境 Docker daemon 未运行或 `docker` 权限被拒绝，该命令记为 `NOT RUN` 并附 exact reason；这**只允许 author code-complete**。**S2 accepted-slice checkpoint 前**，controller 必须在其 Docker-capable 环境实际执行 `./mvnw -B -ntp -Pcontainer-smoke verify` 并通过（failsafe XML `tests=1 skipped=0 failures=0 errors=0` + §7.3 SQL 断言）。controller daemon 仍不可用时，Gateflow 停止并请求启动 Docker；**不得 accept/commit S2**，也不得把该 primary proof 推迟到后续 PR/CI run。CI 首次真实运行是 post-push 附加证据（§9.6），不能替代本次 controller pre-push run。

### 7.5 证据边界

- Container smoke 证明：Docker 可用、PostgreSQL 16 镜像可启动、JDBC 可连接、空 schema、无 Flyway 历史。
- 不证明：生产 PostgreSQL 配置、HA/PITR/backup、A0 表/约束/锁、并发、性能、恢复、云网络或任何业务事务。
- 不把 container smoke 写入 A5 evidence ledger/acceptance record；它不是 `PASS`/`productionAccepted`。

## 8. Node 22 与 A1-A5 contract regressions

### 8.1 Node pin 与现有 package 语义

- CI 使用 `actions/setup-node@v4` + `node-version: '22.23.4'`；job 第一步 `node --version | grep -Fx v22.23.4`。
- **不修改** `api/openapi/package.json` 的 `engines.node >=22`，不修改 scripts，不修改 `package-lock.json`；22.23.4 满足现有 range。
- 不新增 root `package.json`/workspace；Node 只用于 `api/openapi` 与零依赖 validators。
- 本会话已独立验证 `node --version` = `v22.23.4`；npm 具体版本不 pin（lockfile 决定依赖），CI `npm ci` 是唯一安装方式。
- A5 closeout 记录 controller shell 曾为 Node 20.20.1，而 `api/openapi` 声明 `>=22`；这不是受支持的回归运行时。CI 精确 22.23.4 消除该歧义，且不改变 package 的 `>=22` 语义。

### 8.2 安装/缓存边界

- `npm ci` 在 `api/openapi` 执行；只接受 `package-lock.json`，不执行 `npm install`。
- 缓存只缓存 npm download cache（`actions/setup-node` 的 `cache: npm`，key 为 `api/openapi/package-lock.json`）；不缓存 `node_modules`，不缓存 `tmp/openapi-client`。
- `tmp/openapi-client/onecall-v1.bundle.json` 每次由 `npm run check:s3` 重新生成；A5 回归必须在该生成之后运行。
- 离线环境无法 `npm ci` 时，A1 bundle/A5 不能运行；报告环境限制，不得用 fake bundle 绕过。

### 8.3 精确命令与期望输出

从仓库根执行（A1 在 `api/openapi`）：

```bash
# A1: lint + bundle + generate + typecheck + doc-map (s3)
( cd api/openapi && npm ci && npm run check:s3 )

# A2: IVR protocol s3
node protocol/ivr/validate.mjs --stage s3 \
  --model protocol/ivr/v1/ivr-protocol-v1.json \
  --fixtures protocol/ivr/v1/fixtures \
  --spec docs/16-production-ivr-protocol-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md

# A3: security s3
node security/validate.mjs --stage s3 \
  --model security/v1/security-design-v1.json \
  --fixtures security/v1/fixtures \
  --spec docs/17-production-security-design-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures

# A4: capacity/deployment s2
node deployment/capacity/validate.mjs --stage s2 \
  --template deployment/capacity/inputs.template.json \
  --fixtures deployment/capacity/fixtures

# A5 s1
node acceptance/validate.mjs --stage s1 \
  --matrix acceptance/v1/acceptance-matrix-v1.json \
  --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json \
  --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs \
  --a4-fixtures deployment/capacity/fixtures

# A5 s2 + doc-link check
node acceptance/validate.mjs --stage s2 \
  --matrix acceptance/v1/acceptance-matrix-v1.json \
  --doc docs/19-production-acceptance-matrix.md \
  --fixtures acceptance/fixtures \
  --a0 docs/14-production-data-model-and-flyway-schema-spec.md \
  --a1-bundle tmp/openapi-client/onecall-v1.bundle.json \
  --a2-model protocol/ivr/v1/ivr-protocol-v1.json \
  --a2-fixtures protocol/ivr/v1/fixtures \
  --a3-model security/v1/security-design-v1.json \
  --a3-fixtures security/v1/fixtures \
  --a4-calc deployment/capacity/calc.mjs \
  --a4-fixtures deployment/capacity/fixtures \
  --check-doc-links
```

期望语义（以 accepted A5 closeout 的当前输出为基线；实现必须核对实际输出，不得只看 `PASS` 字符串）：

- A1：`PASS check-doc-map (s3)`，`total=49`，五面计数一致，A0/doc drift 为零。
- A2：`PASS protocol-validation (s3)`，`fixtures=25 passed=25 failed=0`，`a1_operations=49 ivr_operations=6`，`a0_tables=44`。
- A3：`PASS security-validation (s3)`，`fixtures=23 passed=23 failed=0`，A0/A1/A2 drift 为零。
- A4：`PASS capacity-validation stage=s2`，`fixtureCases=113 passed=113 failed=0`，`productionReady=false procurementReady=false cloudVerified=false a3RiskClosed=false`。
- A5 s1/s2：`PASS acceptance-validation (s1|s2)`，`coverage_cells=96`、`represented=90`、`not_applicable=6`、`deferred=0`、`runtime_evidence=NOT_RUN`、`production_accepted=false`；s2 的 `--check-doc-links` 通过。
- 任何上游 validator 非 0、计数/digest drift 或 A1 bundle 缺失 => CI 失败；不得用 PA0 修改上游 fixtures/validators 来“修复”回归。

### 8.4 A0-A5 语义保持

- PA0 不修改 `api/openapi/**`、`protocol/**`、`security/**`、`deployment/capacity/**`、`acceptance/**`、`docs/12`-`docs/19`。
- 不修改 A1-A5 fixtures、counts、model、validator、review artifacts。
- 不创建 A5 evidence manifest/authority/acceptance record；`acceptance/validate.mjs` 的 `runtime_evidence=NOT_RUN`/`production_accepted=false` 保持。
- CI 只执行既有 validators，不复制其语义、不新增第二 registry。

## 9. 最小 GitHub Actions CI

### 9.1 触发与路径过滤（防 screenshot-only 触发）

`.github/workflows/ci.yml`：

```yaml
name: ci
on:
  push:
    branches: [main]
    paths:
      - 'backend/**'
      - '.github/workflows/ci.yml'
      - 'api/openapi/**'
      - 'protocol/**'
      - 'security/**'
      - 'deployment/capacity/**'
      - 'acceptance/**'
      - 'docs/*.md'
  pull_request:
    paths:
      - 'backend/**'
      - '.github/workflows/ci.yml'
      - 'api/openapi/**'
      - 'protocol/**'
      - 'security/**'
      - 'deployment/capacity/**'
      - 'acceptance/**'
      - 'docs/*.md'
```

- `docs/*.md` 只匹配顶层 docs spec/README（包含 A0-A5 validator 读取的 `docs/04`-`docs/19` 与 `docs/README.md`），不匹配 `docs/reviews/**` 或 `docs/gateflow/**`。
- 不匹配 `prototype/**`、`web-prototype/**`、`operations-prototype/**`、`shared/**`、`logo/**`、`figma设计图截图/**`；screenshot-only/UI-prototype 变更不触发 Java/contract CI。
- 不匹配 `.codex-tasks/**`、`tmp/**`。
- push 只对 `main` 运行；feature branch 通过 PR 事件运行，避免同一提交在未开 PR 时重复消耗。PR 打开/更新即运行。
- 不使用 YAML anchors（GitHub Actions 对 anchors/aliases 的支持不应成为依赖）；paths 重复列出。

### 9.2 权限、并发、超时

```yaml
permissions:
  contents: read
concurrency:
  group: ci-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.event_name == 'pull_request' }}
```

- `contents: read`：checkout/读取仓库即可；无 secrets、无 write token、无 PR comment、无 deployment。
- `cancel-in-progress` 只在 PR 事件取消旧运行；main push 不互相取消。
- job 级 `timeout-minutes`：Java 30、contract 25。这是 CI 机制，不是 production SLO/RTO；不得引用为容量/可用性承诺。

### 9.3 `java-runtime` job

```yaml
java-runtime:
  runs-on: ubuntu-latest
  timeout-minutes: 30
  defaults:
    run:
      working-directory: backend
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: '21.0.13+9'
        cache: maven
        cache-dependency-path: backend/pom.xml
    - name: assert toolchains and jdk pin
      run: |
        grep -Fx '21.0.13+9' .java-version
        ./mvnw -B -ntp -v | grep -F 'Apache Maven 3.9.9'
        ./mvnw -B -ntp -v | grep -F 'Java version: 21.0.13'
        java -version 2>&1 | grep -F '21.0.13+9'
    - name: require docker daemon
      run: docker info
    - name: build, module checks, container smoke
      run: ./mvnw -B -ntp -Pcontainer-smoke verify
    - name: assert container smoke actually ran
      run: |
        python3 - <<'PY'
        import glob
        import sys
        import xml.etree.ElementTree as ET

        def suites(pattern):
            files = glob.glob(pattern)
            if not files:
                sys.exit(f"missing report: {pattern}")
            return [ET.parse(f).getroot() for f in files]

        failsafe = suites("target/failsafe-reports/TEST-*PostgresContainerIT.xml")
        if len(failsafe) != 1:
            sys.exit(f"expected one container IT report, got {len(failsafe)}")
        suite = failsafe[0]
        if int(suite.get("tests", "0")) != 1:
            sys.exit("container IT did not report exactly one test")
        if int(suite.get("skipped", "0")) != 0:
            sys.exit("container IT was skipped")
        if int(suite.get("failures", "0")) != 0 or int(suite.get("errors", "0")) != 0:
            sys.exit("container IT failed")
        cases = [case.get("name") for case in suite.findall("testcase")]
        if "connectsToPostgresAndFindsNoApplicationSchema" not in cases:
            sys.exit("container IT method missing")
        for surefire in suites("target/surefire-reports/TEST-*.xml"):
            if int(surefire.get("skipped", "0")) != 0:
                sys.exit("a unit/module test was skipped")
        print("container-smoke report assertions passed")
        PY
    - name: boot jar smoke
      run: |
        timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1
        grep -F 'Started OneCallApplication' target/boot.log
    - name: production jar dependency firewall
      run: |
        jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
        if grep -Eq 'BOOT-INF/lib/(spring-security|flyway|jooq|HikariCP|postgresql|testcontainers)' target/jar-index.txt; then
          echo 'forbidden runtime dependency in fat jar'
          exit 1
        fi
```

- `setup-java` 固定精确 `java-version: '21.0.13+9'`，不使用 `check-latest`；`assert toolchains and jdk pin` 同时核对 `backend/.java-version`、Maven 3.9.9、Java patch `21.0.13` 与 Temurin build `+9`。CI 运行不替代 controller pre-push container run。
- `docker info` 在任何 Maven/容器步骤之前；Docker 不可用时 job 立即失败，不会出现“测试被 skip 但 job 绿”。
- `-Pcontainer-smoke verify` 同时运行 surefire（context/Modulith/firewall）和 failsafe（Postgres IT）。
- Python 断言是“实际执行”的独立证明：report 存在、精确 1 个 test、0 skipped、0 failure/error、方法名匹配；surefire 也不允许 skip。
- Boot jar smoke：`timeout 60s` 防止未来误加 web server 后无限挂起；`Started OneCallApplication` 证明启动成功，exit 0 证明进程正常退出。
- `jar tf` 证明 test-scope PostgreSQL/Testcontainers 未进入 production fat jar，且无 Security/Flyway/jOOQ/Hikari。
- 不在 CI 中运行 `mvn install/deploy`、不发布镜像、不写 artifact registry。

### 9.4 `contract-regressions` job

```yaml
contract-regressions:
  runs-on: ubuntu-latest
  timeout-minutes: 25
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-node@v4
      with:
        node-version: '22.23.4'
        cache: npm
        cache-dependency-path: api/openapi/package-lock.json
    - name: assert exact node version
      run: node --version | grep -Fx v22.23.4
    - name: install locked A1 toolchain
      working-directory: api/openapi
      run: npm ci
    - name: A1 s3
      working-directory: api/openapi
      run: npm run check:s3
    - name: A2 s3
      run: node protocol/ivr/validate.mjs --stage s3 ...
    - name: A3 s3
      run: node security/validate.mjs --stage s3 ...
    - name: A4 s2
      run: node deployment/capacity/validate.mjs --stage s2 ...
    - name: A5 s1
      run: node acceptance/validate.mjs --stage s1 ...
    - name: A5 s2
      run: node acceptance/validate.mjs --stage s2 ... --check-doc-links
```

- `...` 必须替换为 §8.3 的完整参数，不得省略 `--a1-bundle`/`--a0`/A2/A3/A4 context。
- A1 先 `npm ci` 再 `check:s3`；A5 依赖 `tmp/openapi-client/onecall-v1.bundle.json` 已生成。
- 不缓存 `node_modules`/`tmp`；只缓存 npm download cache。
- 不运行 `shared/qa`、Playwright、prototype QA；它们不是 A1-A5 contract regressions，也不属于 PA0。

### 9.5 CI 成功语义

- CI 绿 = Java build/unit/module/container smoke + Node static/synthetic contract regressions 通过。
- CI 绿 **不是** A5 `PASS`、`EVIDENCE_VALID`、`productionAccepted=true` 或任何生产验收；不生成 evidence artifact/manifest/acceptance record。
- 不证明 carrier/SIP/CDR、IVR 线路/media、真实 PostgreSQL 生产实例、Redis/RabbitMQ/COS/KMS、WeChat/payment/provider、Tencent Cloud capability、load、restore、penetration、legal/manual 或 production acceptance。
- `backend/README.md` 与 job 名称/步骤使用 “build/static/synthetic” 语义；不写 `production accepted`。若未来需要发布门禁/分支保护，由 controller/Platform owner 在 release governance 中配置，不在 PA0 发明。

### 9.6 无法本地执行 CI 的证据边界

- Author Gate 不 push、不创建/修改 PR、不触发 GitHub Actions；本会话 `docker` 也被权限门拒绝且 daemon 未运行。
- S2 只能静态检查 workflow YAML/命令/路径，并执行本地 Java/Node 命令（若权限允许）；**不得声称 CI 已运行/通过**。CI 首次真实运行证据属于 post-push PR gate。
- **证据不可互相替代**：S2 accepted checkpoint 的 primary proof 是 controller pre-push Docker run（§11.2 状态 2）；真实 GitHub Actions run 是 post-push 附加 workflow 证据（§11.2 状态 3）。CI 后续通过不能追溯满足 pre-push controller run，controller 本地 run 也不能替代 post-push CI 必须在 draft-PR-pass/final closeout 前通过。
- 若 `gh`/GitHub 不可用，报告 `NOT RUN` 及 exact denial；不伪造 status check。

## 10. 公共接口、Schema、状态机影响

**预期影响：无。**

| 面 | 影响 | 理由 |
| --- | --- | --- |
| A1 OpenAPI/API | 无 | 不加 web starter/HTTP server/controller；49 operations 不变 |
| A0 schema/migration | 无 | 不加 Flyway/DDL/DataSource；container 断言空 schema |
| A2 IVR protocol/event | 无 | 不加 IVR message/state/adapter |
| A3 security/auth domain | 无 | 不加 Security/credential/mTLS/KMS/COS policy |
| A4 capacity/deployment | 无 | 不选 region/AZ/SKU/SLO/RPO/RTO；CI timeout 不是容量值 |
| A5 acceptance matrix | 无 | 不写 evidence/authority/record；`runtimeEvidence=NOT_RUN`、`productionAccepted=false` |
| Domain state machines | 无 | 14 个模块只有 marker/package-info，无状态/业务方法 |
| Public URL/Problem code/role/event | 无 | 不新增任何 public identifier 或 enum |
| Build contract | 新增内部 Maven 坐标/包名 | `com.onecall:onecall-runtime`、`com.onecall.core.*` 是内部构建事实，不是 public production API |

- Maven group/artifact/package 不是客户端契约；未来可在没有 external contract 的情况下 bounded rename。
- Testcontainers test credentials 是容器内随机值，不是 production credential；`postgres:16` 是 test fixture，不是 production DB 版本选择。

## 11. 实施切片（2）

### 11.1 S1 — Java/Maven 运行时、14 模块边界与依赖防火墙（无 Docker）

**行为目标**：建立一个可构建、可启动、可测试的 Java `21.0.13+9`/Boot 3.5/Modulith 单模块应用；精确冻结 14 个模块边界（含 executable allowedDependencies map）并证明 cycle/forbidden/internal 会被拒绝；不要求 Docker/DB。

**允许路径**：

- `backend/pom.xml`
- `backend/.gitignore`
- `backend/.java-version`
- `backend/.mvn/wrapper/maven-wrapper.properties`
- `backend/mvnw`
- `backend/mvnw.cmd`
- `backend/README.md`
- `backend/src/main/java/com/onecall/core/OneCallApplication.java`
- `backend/src/main/java/com/onecall/core/**/package-info.java`（仅 14 个模块）
- `backend/src/main/java/com/onecall/core/**/ModuleMarker.java`（仅 14 个模块）
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/onecall/archtests/**`（不含 container）
- `backend/src/test/java/com/onecall/archfixtures/**`
- `docs/gateflow/pa0-production-runtime-bootstrap-s1-implementation.md`

**调用路径**：

- `OneCallApplication.main` -> `SpringApplication.run` -> 无 DB/security/web 的 context。
- `ModuleBoundaryTests` -> `ApplicationModules.of(OneCallApplication.class)` -> exact set/annotation/verify。
- `BootstrapDependencyBoundaryTests` -> `ClassUtils.isPresent`；`OneCallApplicationTests` -> context bean presence。
- Negative fixtures -> `ApplicationModules.of(<FixtureRoot>.class).verify()` -> `Violations`。

**不变量**：

1. 精确 14 模块；每个模块 marker + `@ApplicationModule`；无第 15 个模块/缺失模块。
2. `ModuleBoundaryTests` 从每个 package-info 反射读取 `allowedDependencies()` 并与 §6.2 单一 expected map 做 exact set equality（含 leaf 空集、拒绝默认 `OPEN_TOKEN`）；discovered production edges ⊆ configured map 且当前为空；4 个 mismatch probes 全部按 §6.4 失败/恢复；无 cycle；负向 fixtures 三组全部抛 `Violations`（forbidden fixture 使用显式 `{}`）。
3. test classes 不在 `com.onecall.core.**`；app root 只有 app class + 14 模块。
4. 无 Security/Flyway/jOOQ/Hikari 库/bean；无 DataSource/HTTP server。
5. `./mvnw -B -ntp verify` 在无 Docker 环境成功；jar 启动 exit 0 且日志含 `Started OneCallApplication`。
6. production fat jar 不含 PostgreSQL/Testcontainers/Security/Flyway/jOOQ/Hikari jar。
7. Enforcer toolchain/convergence/banned dependencies 通过。
8. §4.3 version/dependency preflight 通过；Maven SHA-256 已记录。

**错误处理**：

- 模块缺失/多余/annotation 缺失 -> 测试失败，不自动补/删模块。
- cycle/forbidden/internal -> `Violations` 使 build 失败；不放宽 allowedDependencies。
- 禁用 framework 出现 -> ClassUtils/Enforcer/jar firewall 失败；不通过移除测试或 `@ConditionalOnMissingBean` 绕过。
- context 启动失败/jar 非 0 -> S1 失败；不加 DB/security 来“让它启动”。
- 版本/收敛/checksum 失败 -> 停止并报告 blocker；不猜版本/不省略 checksum。

**命令与期望断言**：

```bash
cd backend
grep -Fx '21.0.13+9' .java-version
java -version 2>&1 | grep -F '21.0.13+9'
./mvnw -B -ntp -v
./mvnw -B -ntp -U dependency:resolve
./mvnw -B -ntp dependency:tree \
  -Dincludes=org.springframework.boot:*,org.springframework.modulith:*,org.testcontainers:*,org.postgresql:*
./mvnw -B -ntp verify
java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1
grep -F 'Started OneCallApplication' target/boot.log
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
```

- `.java-version`/`java -version`/`./mvnw -v`：精确 Temurin `21.0.13+9`（patch `21.0.13` + build `+9`）与 Maven 3.9.9。既有 `21.0.12.1+1` 仅可作诊断，不构成本条通过。
- `dependency:resolve`/`dependency:tree`：Boot 3.5.16、Modulith 1.4.14、Testcontainers 2.0.5 可解析；PostgreSQL driver 来自 Boot BOM。
- `verify`：BUILD SUCCESS；surefire 精确执行 ModuleBoundaryTests/OneCallApplicationTests/BootstrapDependencyBoundaryTests，无 skip；Enforcer 全绿。
- jar：exit 0；`Started OneCallApplication`；`jar tf` 无 `BOOT-INF/lib/(spring-security|flyway|jooq|HikariCP|postgresql|testcontainers)`。
- 不在 S1 运行/声称 container smoke 或 CI。

**S1 完成条件**：上述命令在权限允许且 **已选择/安装精确 Temurin `21.0.13+9`** 的环境执行并记录；若执行环境只有 `21.0.12.1+1`，该运行仅作诊断，不得据此声称 S1 验证通过。若 Java/Maven 被权限门拒绝，报告 exact denial 并列为 controller evidence，不得声称 PASS。S1 报告必须记录 exact JDK patch/build 与 pin 文件、version preflight 结果、SHA-256、module set/annotations、**exact allowedDependencies map + 4 个 mismatch probes**、三组负向结果、firewall 结果、boot/jar 结果、A1-A5 是否运行及环境限制。

### 11.2 S2 — PostgreSQL Testcontainers smoke、CI 强制执行与 A1-A5 回归接入

**行为目标**：在真实 Docker/PostgreSQL 上证明连接 smoke 与空 schema；把 container smoke 和 A1-A5 regressions 接入最小 CI，并保证 skip/未执行不能变成成功。

**允许路径**：

- `backend/src/test/java/com/onecall/archtests/container/PostgresContainerIT.java`
- `backend/pom.xml`（仅新增 `container-smoke` profile；test-scope Testcontainers/PostgreSQL 已在 S1 POM）
- `backend/README.md`（更新本地/CI/证据边界）
- `.github/workflows/ci.yml`
- `docs/gateflow/pa0-production-runtime-bootstrap-s2-implementation.md`

**调用路径**：

- `./mvnw -B -ntp -Pcontainer-smoke verify` -> failsafe -> `PostgresContainerIT` -> Testcontainers/Docker -> JDBC -> SQL assertions -> container stop。
- CI java job -> checkout/setup-java -> `docker info` -> Maven verify -> failsafe XML assert -> jar smoke -> jar firewall。
- CI contract job -> checkout/setup-node 22.23.4 -> `npm ci` -> `check:s3` -> A2/A3/A4/A5。

**不变量**：

1. 无 `@Disabled`/`Assumptions`/`disabledWithoutDocker`/环境变量 skip；Docker 不可用/容器失败 => build 失败。
2. profile 激活且测试不被匹配 => `failIfNoTests` 失败；failsafe XML 必须 `tests=1 skipped=0 failures=0 errors=0` 且方法名匹配。
3. surefire 报告无 skipped；所有 unit/module tests 实际执行。
4. JDBC 连接成功；`current_database()=onecall`、`current_user=onecall`、`server_version_num>=160000`；无业务表；无 Flyway history。
5. `docker info` 在 Maven 前；CI 不允许无 Docker 时进入测试步骤。
6. workflow paths 排除 prototypes/screenshots；权限 `contents: read`；concurrency/timeout 仅 CI 机制；不 push/PR/comment。
7. Node 22.23.4 精确；`npm ci` + `check:s3`；A2-A5 完整参数；A5 bundle 前置存在。
8. CI 成功不生成/不声称 A5 evidence/acceptance；`productionAccepted=false` 语义不变。
9. 不修改 api/protocol/security/deployment/acceptance/docs 12-19；A1-A5 现有语义/counts 不变。
10. S2 使用 §11.2 的三态完成语义；author `NOT RUN` 绝不等于 accepted-slice checkpoint，controller pre-push container run 是唯一接受前提。

**错误处理**：

- Docker 不可用 -> container IT 启动失败；不转换为 skip。
- 容器起来了但无 JDBC/表断言失败 -> test failure；不放宽断言。
- 测试未匹配/报告缺失/skipped -> Python/`failIfNoTests` 失败。
- A1 bundle 缺失/A2-A5 非 0 -> contract job 失败；不伪造 bundle/不改 fixtures。
- GitHub Actions 未实际运行 -> 报告 `NOT RUN`；不得声称 CI 绿。
- 需要新增 schema/migration/security 才能通过 -> 停止，属于越界。

**命令与期望断言**：

```bash
cd backend
./mvnw -B -ntp -Pcontainer-smoke verify
# CI 还会执行 §9.3 的 Python report assertions、boot jar smoke、jar firewall
```

从仓库根执行 §8.3 的 A1-A5 命令（CI contract job 使用同一组参数）。

- `-Pcontainer-smoke verify`：BUILD SUCCESS；failsafe `PostgresContainerIT` 1 test，0 skipped，0 failure/error；SQL 断言全 True；容器停止。
- CI YAML：两 job、只读权限、路径过滤、`docker info`、report assertion、Node 22.23.4、A1-A5 命令均存在且与 §8/§9 一致。
- A1-A5：§8.3 期望输出/计数全绿。
- author Docker/CI 权限被拒绝：报告 exact denial + `NOT RUN`，只标记 author code-complete，不得标记 S2 证据为 PASS。
- controller Docker 不可用/daemon 未运行：Gateflow 停止并请求启动 Docker；不得 accept/commit S2，不得以“后续 CI 会跑”替代。

**S2 完成状态（三态，互不替代）**：

1. **author code-complete**：`PostgresContainerIT.java`、`container-smoke` profile、`.github/workflows/ci.yml`、README 更新存在；非容器静态/本地检查（YAML/命令/路径、Node 与 A1-A5 若环境允许、`./mvnw -B -ntp verify`）通过。author Docker 可以为 `NOT RUN`，但必须记录 exact reason。**author code-complete 不是 accepted slice**。
2. **controller container-evidence complete（S2 accepted-slice checkpoint 的唯一前置）**：controller 在其 Docker-capable 环境执行 `./mvnw -B -ntp -Pcontainer-smoke verify`，证明**恰好一个** intended test、`skipped=0`、`failures=0`、`errors=0`，并 §7.3 SQL 断言全真。若 controller daemon 仍不可用，Gateflow 必须停止并请求启动 Docker；**不得 accept/commit S2**，也不得把该 primary proof 推迟到后续 PR run。
3. **post-push CI evidence complete（`draft-PR-pass` / final closeout 前置）**：在后续 Gateflow push/draft-PR gates 之后，真实 GitHub Actions 两个 job 必须通过。这是**额外 workflow 证据**，不替代状态 2，也不能被状态 2 替代。

| 状态 | 允许环境 | 命令/证据 | 能否接受 S2 checkpoint |
| --- | --- | --- | --- |
| author code-complete | author | 静态检查 + `./mvnw -B -ntp verify`；Docker `NOT RUN` + exact reason | 否 |
| controller container-evidence complete | controller Docker | `./mvnw -B -ntp -Pcontainer-smoke verify` + failsafe XML + SQL 断言 | **是（唯一）** |
| post-push CI evidence complete | GitHub Actions | 两个 job 真实通过 | 否（draft-PR/closeout 前置） |

- `NOT RUN` 只能落在 author 行；controller 行必须是 `PASS` 才能创建 accepted S2 checkpoint。任何把 `NOT RUN` 解释为 accepted S2 的读法都违反本节。

### 11.3 为什么是 2 个 slice

- S1 是独立的**本地运行时/边界行为**：不需要 Docker/CI 就能证明 Java/Maven/Boot/Modulith/依赖防火墙/启动。
- S2 是独立的**真实执行/证据边界**：只有 S2 才引入真实 PostgreSQL 容器、CI 强制性和 A1-A5 跨仓库回归；S1 不涉及 DB/CI，S2 不重写模块/依赖设计。
- 若拆成 Java scaffold / Modulith / Testcontainers / CI / docs 五片，会产生文件级/报告级 gate 成本，且 S1 的空模块/空测试风险无法独立验证；若合成一片，则本地无 Docker 时无法区分“scaffold 通过”和“container/CI 未运行”。两片是行为/证据的最小独立边界。
- 不按 module、technology layer、owner 或 source doc 拆分；不增加 docs-only slice。

## 12. 可能陷阱与对策

| Trap | 具体风险 | 对策与证明 |
| --- | --- | --- |
| 空的 Modulith 测试 trivially green | 0 个模块/缺 annotation 时 `verify()` 也 pass | 精确 14 模块集 + 每包 annotation 断言 + marker；删除/新增模块即失败 |
| cycle/forbidden 只声明未证明 | allowedDependencies 写错或 verifier 不生效 | 3 组 test-only fixtures 必须抛 `Violations`；不 assert message |
| 测试类污染模块 | test classes 在 `com.onecall.core.**` 被识别为第 15 模块 | 全部 tests 在 `com.onecall.archtests`；fixtures 在 `com.onecall.archfixtures` |
| CI 中 container test 被 skip | `disabledWithoutDocker`/assumption 让 job 绿 | 无 skip API；profile-only；`docker info`；`failIfNoTests`；XML `tests=1 skipped=0` |
| 本地无 Docker 被说成通过 | 默认 `verify` 没跑 IT 却报告“smoke 通过”；author `NOT RUN` 被当成 S2 接受 | README/报告强制区分 `verify` 与 `-Pcontainer-smoke verify`；author `NOT RUN` 只算 code-complete；S2 accepted checkpoint 必须 controller pre-push `-Pcontainer-smoke verify` PASS；CI 是 post-push 附加证据 |
| Flyway 自动启动但无 migration | starter 加入后 Boot 启动/连接/失败 | 不加 Flyway/DataSource；ClassUtils/Enforcer/jar firewall；container 断言无 flyway history |
| Security auto-config 阻断 smoke | 加 Security 后默认 401/login page | 不加 Security starter；无 web server；ClassUtils/context 断言 |
| GitHub Actions 被截图-only 变更触发 | prototype/screenshot 改动跑重 CI | paths 只含 backend/contracts/docs top-level；不含 prototypes/screenshots/shared |
| 现有 Node 22 package 行为被改坏 | 修改 engines/scripts/lock 或 Node 20 fallback | 不改 `api/openapi/package.json`/lock；CI 精确 22.23.4；`npm ci`/`check:s3` |
| Maven wrapper 不可复现 | 用 runner 预装 Maven 或浮动 distribution | `mvnw` + wrapper 3.3.2 + Maven 3.9.9 URL + SHA-256；CI `./mvnw -v` |
| Dependency convergence 噪音/漂移 | 多 BOM/TC 依赖冲突 | 单 Boot parent + Modulith BOM + 单个 TC core 显式版本；`dependencyConvergence`/banDuplicate |
| 把 A0-A5 validator 改绿 | 为通过 CI 修改 fixtures/validators/counts | 只允许 backend/workflow/README/report 路径；§8.4 禁改；CI 跑原命令 |
| 把 Testcontainers 凭据当生产 secret | 固定密码进 repo/log | 运行时 UUID password；容器-local；不落盘/不日志 |
| 把 postgres:16 当生产版本选择 | test tag 被误读为生产 DB 决策 | 明确 test-only；“16+”来自 docs/12；生产版本仍为 A4 pending |
| 把 CI 绿当生产验收 | 对外声称 accepted/production | README/§9.5/报告 honesty；不建 evidence/authority；`productionAccepted=false` |
| 预建空 worker/scheduler/多模块过度设计 | 无行为模块增加 gate 成本 | 单 Maven module；未来第二个 deployment artifact 再拆 |
| 版本未证明却写成事实 | 用 controller observation 冒充 author proof | §4 三态证据；S1 preflight fail closed；报告 exact denial |

## 13. 文档、发布、风险、开放问题、停止条件、完成报告

### 13.1 文档决定

- 新增 `backend/README.md`：工具链、wrapper、命令、模块边界表、container/CI skip 语义、CI 证据边界、非验收声明。
- 不新增 `docs/20`、root README、第二 spec、README index 行或 acceptance 文档。
- 本计划是唯一 plan artifact；implementation 只新增各 slice 报告；不修改 controller。
- 文档语言：中文说明 + English identifiers/enum/commands；字段和命令保持 canonical ASCII。

### 13.2 迁移/公共契约/发布声明

- 无 DB migration、无 Flyway、无 schema/role/grant、无 backfill。
- 无 OpenAPI/Problem/role/event/IVR/安全/容量/验收 contract 变更。
- 无 deployment、无 image publish、无 SBOM、无 K8s、无 environment/secret、无 feature flag。
- 不创建/修改 PR/issue、不 push、不 merge、不切换分支、不执行其他 Gate。

### 13.3 风险与残留下沉

| ID | Residual | Owner | Destination |
| --- | --- | --- | --- |
| `PA0-R-01` | Boot 3.5.16/Modulith 1.4.14/Testcontainers 2.0.5 的独立 GA 兼容性在本 author 会话未证明（网络/执行权限被拒绝）；仅 controller-observed | Platform/CI | S1 §4.3 preflight；失败则 S1 blocked |
| `PA0-R-02` | Maven 3.9.9 bin zip SHA-256 未在本会话取得 | Platform/CI | S1 wrapper preflight；不得省略 checksum |
| `PA0-R-03` | author 环境 Docker daemon 未运行且 `docker` 被权限门拒绝；container smoke 不能由 author 实际执行 | Platform/CI | **controller pre-push Docker-capable run 是 S2 accepted checkpoint 前置**；daemon 不可用则 Gateflow 停止并请求启动，不以 post-push CI 替代；author 只报 `NOT RUN` |
| `PA0-R-04` | GitHub Actions 首次真实运行需要 push/PR；author Gate 不执行 | Platform/CI | PA0 PR/controller gate；CI 未运行前不得声称绿 |
| `PA0-R-05` | `postgres:16` 是 floating test tag，digest 未 pin | Backend/Platform | S2/CI hardening（可后续 pin digest）；不影响 design family 16+ |
| `PA0-R-06` | Boot 3.5 line 的 support/security 窗口未由 controller metadata 证明 | Platform/Security | S1 preflight；若 EOL 则报告/由 release governance 决定，不静默升 Boot 4 |
| `PA0-R-07` | GitHub Actions 用 major tag 而非 commit SHA，供应链 pinning 不足 | Security/Platform | CI hardening / release governance |
| `PA0-R-08` | path-filtered workflow 与 branch protection required checks 可能交互（docs/review-only PR 不跑 checks） | Platform/CI | repository governance/branch protection 配置 |
| `PA0-R-09` | A0 cross-cutting platform/audit/sharing/evidence 边未预授权，未来 work unit 需显式修订 allowedDependencies | Backend/Architecture | first domain work unit + architecture review |
| `PA0-R-10` | 精确 JDK pin `Temurin 21.0.13+9` 已由 Adoptium feature-release API（2026-10-26）核实，并写入 `backend/.java-version` 与 CI `setup-java`；本机既有 `21.0.12.1+1` 不满足该 pin | Platform/CI | S1 前由执行环境选择/安装精确 release，并以 `java -version`/`./mvnw -v` 断言；无法解析 `jdk-21.0.13+9` 则 S1 fail-closed blocked（非可延后 hardening） |
| `PA0-R-11` | CI 绿可能被误读为生产验收 | Backend/QA/Product | README/§9.5 + A5/release governance |
| `PA0-R-12` | 未加入 formatter/checkstyle；本 slice 的 static check 以 Modulith/Enforcer/firewall 为界 | Backend/Platform | 后续 code-quality work unit（不在 PA0 goal） |
| `PA0-R-13` | module allowedDependencies 是由 docs/12 §6 prose 翻译的 capacity 上限；若上游架构变化，需同步 | Architecture | architecture change control |

### 13.4 开放问题（typed，不阻塞 plan scope）

1. **VQ-01（S1 preflight blocker）**：Boot 3.5.16 / Modulith 1.4.14 / Testcontainers 2.0.5 的独立 GA 兼容性证据。当前只有 controller 2026-10-26 observation；本会话网络/执行被拒绝。处理：S1 §4.3 用真实 Maven resolve/tree/verify 证明；失败则停止，不猜替代版本。
2. **VQ-02（S1 preflight blocker）**：Maven 3.9.9 zip 的官方 SHA-256。处理：S1 计算/记录；无法取得则 S1 blocked。
3. **VQ-03（S2 evidence dependency，已按三态拆分）**：controller pre-push Docker-capable run 是 S2 accepted checkpoint 的唯一前置；GitHub Actions 首次真实运行是 post-push 的 draft-PR/closeout 前置，不替代前者。author 只报告 author code-complete 与 `NOT RUN`。
4. **VQ-04（residual）**：test image `postgres:16` 的 digest pin。处理：后续 CI hardening；不是 production DB 版本承诺。
5. **VQ-05（preflight）**：Boot 3.5 的官方 support/security 窗口。处理：S1 记录；若不受支持，上报 release governance，不静默升 Boot 4。
6. **VQ-06（residual）**：Actions commit-SHA pinning 与 path-filter/branch protection 配置。处理：Platform/Security CI hardening。
7. **VQ-07（deferred）**：platform/audit/sharing/evidence 的 cross-cutting Java 边。处理：对应 domain work unit 显式修订 §6.2 并评审，PA0 不预授权。

- **本次 bounded fix 已关闭**：deny-all 语义（原 review OQ-01）由 Spring Modulith `1.4.14` published source/Javadoc 证实——默认 `OPEN_TOKEN` 为 unrestricted、显式 `allowedDependencies = {}` 为 deny-all（§6.2/§6.5）；Docker 供给（原 review OQ-02）由 controller pre-push container run 要求关闭（§11.2）。VQ-02（Maven zip SHA-256）与 VQ-05（Boot 3.5 support window）仍是 fail-closed S1 preflight，不得猜测。
- 以上其余均为 typed preflight/evidence/residual，不是 invent 许可，也不是产品 scope blocker；plan review 不依赖它们被伪造解决。

### 13.5 停止条件

实现遇到以下任一情况必须停止并报告，不得静默决定：

- Boot/Modulith/Testcontainers/Enforcer 无法解析、版本不兼容、convergence 无法在不改设计的情况下通过；
- Maven 3.9.9 SHA-256 无法取得/校验；
- 执行环境无法解析/安装精确 Temurin `21.0.13+9`，或 `java -version`/`./mvnw -v` 不匹配 patch `21.0.13` + build `+9`；
- 实际解析的 Modulith 版本对显式 `allowedDependencies = {}` 的 published 行为与 `1.4.14` 不一致（例如不构成 deny-all），或需要削弱/删除负向测试；
- 需要新增 Security/Flyway/jOOQ/Hikari/DataSource/HTTP server/schema/migration/domain code 才能让启动或 smoke 通过；
- 需要放宽 `docker info`/report `skipped=0`/`failIfNoTests`/无 skip API 的 CI 证据；
- 需要修改 A0-A5 assets/validators/fixtures/counts/docs 12-19 才能让回归通过；
- 需要 production credentials、cloud resource、Region/AZ/SKU/SLO/RPO/RTO/retention/acceptance authority；
- 需要 push/commit/PR/merge、切换分支或进入其他 Gate；
- author Docker 不可用：可完成 author code-complete 并报告 exact reason；controller Docker daemon 未启动：**必须停止并请求启动 Docker**，不得 accept/commit S2，也不得把 primary proof 推迟到后续 PR/CI run。

### 13.6 每个 slice 的完成报告格式

S1 不使用 container/CI 字段时标 `N/A`；S2 必须按 §11.2 的三态分别填写，不得把 author `NOT RUN` 写成 accepted。任何未执行字段不得写成 `true`/`PASS`；S2 accepted checkpoint 要求 `controller_container_smoke=PASS`。

```text
taskLabel, provider/model, gate, baseCommit, sliceId
changedFiles[] with SHA-256 and line counts
toolchain evidence:
  backend/.java-version content (exact 21.0.13+9)
  java -version / ./mvnw -v exact output (patch 21.0.13 + Temurin build +9)
  Node node --version exact output
  dependency tree: spring-boot, spring-modulith, testcontainers, postgresql
  version evidence status: AUTHOR_VERIFIED / CONTROLLER_OBSERVED / PREFLIGHT_REQUIRED
  Maven wrapper distributionUrl + distributionSha256Sum + retrieval date
module inventory:
  14 names/packages, annotation presence,
  exact allowedDependencies map assertion (pass) + 4 mismatch probes (fail/restore),
  discovered production edges subset check (empty)
commands executed + exact exit code + key stdout/stderr
  (permission-denied commands: exact denial, NOT RUN, not PASS)
semantic assertions observed:
  module_set_exact=true, annotation_present=14/14, cycle_rejected=true,
  forbidden_rejected=true, internal_rejected=true,
  forbidden_frameworks_absent=true, no_datasource_flyway_security_beans=true,
  boot_log_started=true, jar_exit=0, runtime_jar_forbidden_libs=0,
  container_tests=1, container_skipped=0, container_failures=0,
  current_database=onecall, server_version_num>=160000,
  application_tables=0, flyway_history_absent=true
A1-A5 regression outputs:
  A1 total=49; A2 fixtures=25 passed=25 failed=0;
  A3 fixtures=23 passed=23 failed=0;
  A4 fixtureCases=113 passed=113 failed=0, readiness flags false;
  A5 s1/s2 coverage=96 represented=90 N/A=6 deferred=0,
  runtime_evidence=NOT_RUN, production_accepted=false
slice completion state:
  S1: container/CI fields N/A
  S2 author_code_complete: true/false
  S2 controller_container_smoke: PASS / FAIL / PENDING (exact reason; PASS required for accepted checkpoint)
  S2 post_push_ci_container_smoke: PASS / FAIL / NOT RUN (exact reason)
CI status:
  workflow YAML/commands/paths static check result
  actual GitHub run: PASS / FAIL / NOT RUN (exact reason; never claim without run)
  note: post-push CI cannot substitute controller pre-push container run
residual owner/destination reconciliation (PA0-R-01..13)
honesty statement:
  only build/static/synthetic checks; no carrier/IVR media/cloud/provider/
  load/restore/penetration/manual/production acceptance; no A5 evidence
scope statement:
  only allowed paths changed; pre-existing dirty paths untouched;
  no commit/push/PR/issue/other Gate
open questions/blockers: exact list or none
```

## 14. 无过度设计与无目标漂移

本计划**不**建设：

- 空的多模块 parent/worker/scheduler：只有一个可执行 artifact；未来第二个 deployment unit 再拆。
- Domain/state machine/API/controller/repository/entity/adapter：14 个模块只有边界标记。
- A0 表/Flyway/schema/role/grant/function/trigger/Outbox/audit：全部留给后续 work unit。
- Security/auth/session/mTLS/KMS/COS：不加 starter、不写配置、不发明认证域。
- DataSource/Hikari/jOOQ/JPA：不加依赖、不创建 DataSource；Testcontainers 只用 test-scope JDBC。
- Dockerfile/compose/K8s/image/SBOM/registry/deployment environment：非目标。
- 额外 Testcontainers modules/extension/test platform：只用 core + manual lifecycle。
- Formatter/checkstyle/Spotless/ArchUnit 第二套架构规则：Modulith + Enforcer + firewall 已覆盖本 goal 的边界。
- 第二 registry/evidence ledger/acceptance authority/CI dashboard：A5 仍是语义 owner。
- 生产凭据/并发/保留期/SLO/RPO/RTO/Region/AZ/SKU：全部不发明；CI timeout 是 CI 机制，不是生产值。

为什么 14 个 marker + 14 个 package-info 不是过度设计：它们是让“精确模块集 + annotation + negative fixture”可执行的最小 metadata；没有 domain code。为什么 2 slices 不是机械拆分：S1 是无需 Docker 的本地运行时/边界行为，S2 是真实容器 + CI/跨仓库回归的执行证据边界；各自可独立验证且失败模式不同。

## 15. 计划要求追踪与完成定义

### 15.1 任务要求到章节追踪

| Requirement | Plan section |
| --- | --- |
| Goal/motivation/success/non-goals/boundary/alignment | §1 |
| First-principles + direct repository evidence | §1.2、§2 |
| Design doc and A0-A5 alignment | §2.2、§2.3 |
| Exact repository layout/affected files | §3 |
| Exact version pins and authoritative compatibility evidence or blocking question | §4、§13.4 |
| Maven structure/app entry/Modulith declaration/detection/allowed rules/verification | §5、§6 |
| Security/Flyway/jOOQ/Hikari/PostgreSQL bootstrap handling | §5.2、§6.6 |
| Testcontainers lifecycle/failure/skip policy; CI cannot pass unrun | §7、§9.3 |
| Node 22 pin/A1-A5 exact commands/install-cache boundaries | §8 |
| GitHub Actions jobs/permissions/caches/concurrency/timeouts | §9 |
| Public interface/schema/state-machine impact | §10 |
| Small implementation slices with paths/call paths/invariants/errors/commands/assertions | §11 |
| Docs decision/risks/open questions/residual destinations | §13 |
| Completion report format | §13.6 |
| No over-design/no goal drift | §14 |
| Likely traps | §12 |

### 15.2 计划完成定义

- 覆盖 §1-§15 全部 required content；无 plan-scope blocking question。
- 两个 behaviorally bounded slices；每个有独立行为/证据边界、精确 paths、call paths、invariants、error handling、commands、assertions 和 completion-report 要求。
- 明确 exact planned files、14 模块表、allowedDependencies、测试/negative fixtures、POM 结构、container/CI 设计、A1-A5 命令和证据边界。
- 明确不新增 public contract/migration/domain/security/cloud/acceptance；不修改 A0-A5/controller/prototypes/screenshots。
- 明确 version/SHA/Docker/CI 的证据缺口与 fail-closed preflight；不猜测、不伪造。
- 后续 implementation 只需按本计划生成 scaffold/tests/workflow/reports；无需重新决定产品状态、数据库 schema、认证、生产阈值或验收 authority。
