# onecall-runtime（PA0 / S1 + S2）

Phase A 生产运行时引导脚手架。本工程只包含运行时入口、14 个 Spring Modulith 包边界、架构/依赖防火墙测试，以及 S2 的真实 PostgreSQL 16 Testcontainers 连接 smoke；**不包含任何 domain 行为、表、migration、API、认证、供应商或云集成**。

## 工具链 pin

| 组件 | Pin | 说明 |
| --- | --- | --- |
| Java | Temurin `21.0.13+9` | `backend/.java-version`；`maven.compiler.release=21`；Enforcer `[21,22)` |
| Maven | `3.9.9` | Maven Wrapper 3.3.2（`only-script`）；distribution SHA-256 `4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c` |
| Spring Boot | `4.1.2` | parent/BOM；Boot 4.0.x 与 3.x 均不可用 |
| Spring Modulith | `2.1.2` | `spring-modulith-bom` + `spring-modulith-starter-core` |
| Testcontainers | `2.0.5` | 仅 test scope（core + manual lifecycle；`container-smoke` profile） |
| PostgreSQL JDBC | `42.7.14` | 由 Boot `4.1.2` BOM 管理；仅 test scope |

执行 S1 命令前必须显式选择精确 JDK（示例）：

```bash
export JAVA_HOME=/path/to/jdk-21.0.13+9
export PATH="$JAVA_HOME/bin:$PATH"
```

`./mvnw` 首次运行需要从 Maven Central 下载 Maven 3.9.9，并在下载后校验固定的 SHA-256。运行 wrapper 的环境还需具备 `unzip`（或等价的 zip 解压能力）；缺少 `unzip` 时 wrapper 会改走 `.tar.gz` 而固定的 zip SHA-256 无法匹配，因此会 fail closed。

## S1 命令

```bash
cd backend
grep -Fx '21.0.13+9' .java-version
java -version 2>&1 | grep -F '21.0.13+9'
./mvnw -B -ntp -v
./mvnw -B -ntp -U dependency:resolve
./mvnw -B -ntp dependency:tree \
  -Dincludes=org.springframework.boot:*,org.springframework.modulith:*,org.testcontainers:*,org.postgresql:*
./mvnw -B -ntp validate              # Enforcer: toolchain / convergence / banned deps
./mvnw -B -ntp verify                # compile + surefire + jar + Enforcer
java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1
grep -F 'Started OneCallApplication' target/boot.log
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
```

预期：

- `verify` 运行 10 个测试（ModuleBoundaryTests 8、OneCallApplicationTests 1、BootstrapDependencyBoundaryTests 1），无 skip；默认 `verify` **不运行** container IT，也不需要 Docker。
- 应用启动后输出 `Started OneCallApplication` 并以 `0` 退出（无 web server、无 DB、无调度线程）。
- production fat jar 不包含 `spring-security`、`flyway`、`jooq`、`HikariCP`、`postgresql`、`testcontainers` 库。

## S2 命令：PostgreSQL container smoke

```bash
cd backend
./mvnw -B -ntp -Pcontainer-smoke verify
```

- 只在显式 `container-smoke` profile 下运行 `PostgresContainerIT`：启动真实 `postgres:16` 容器（`onecall`/`onecall`，密码为每次 JVM 运行生成的 UUID，不落盘、不写日志），通过 JDBC 断言 `server_version_num >= 160000`、`information_schema` 无业务表、无 `flyway_schema_history`，随后停止容器。
- **fail-closed**：Docker daemon 不可用、镜像拉取失败、容器启动失败、JDBC 连接失败或任一断言失败都使 Maven build 非 0。没有 `@Disabled`、`Assumptions`、`disabledWithoutDocker` 或环境变量 skip。
- `maven-failsafe-plugin` 只在 `container-smoke` profile 内绑定，`failIfNoTests=true`；若 profile 激活但测试未被匹配，build 失败。
- 默认 `./mvnw -B -ntp verify` 成功**不等于** container smoke 通过；只有 `-Pcontainer-smoke verify` 的 failsafe 结果（恰好 1 个 test、`skipped=0 failures=0 errors=0`）才算容器证据。
- `postgres:16` 是 test-only fixture（design family `16+`），不是 production DB 版本承诺；digest 尚未 pin。

## 模块边界

14 个模块都是 `com.onecall.core` 的直接子包；每个包包含 `package-info.java`（`@ApplicationModule`）和 package-private `ModuleMarker`。`allowedDependencies` 表示同步 Java 依赖上限；空集合必须是显式 `{}`（deny-all），不得使用 bare `@ApplicationModule`（默认 `OPEN_TOKEN` 为 unrestricted）。

| 模块包 | allowedDependencies |
| --- | --- |
| `customer_iam` | `{}` |
| `organization_access` | `{customer_iam}` |
| `operations_iam` | `{operations_audit}` |
| `subject_number` | `{}` |
| `activation_consent` | `{subject_number}` |
| `ivr` | `{subject_number, activation_consent, entitlement_metering, evidence}` |
| `catalog_order` | `{customer_iam, subject_number}` |
| `entitlement_metering` | `{catalog_order, subject_number}` |
| `evidence` | `{entitlement_metering}` |
| `sharing` | `{evidence}` |
| `ai_orchestration` | `{evidence, entitlement_metering}` |
| `notary` | `{evidence}` |
| `operations_audit` | `{}` |
| `platform_reliability` | `{}` |

`ModuleBoundaryTests` 断言精确 14 模块集、annotation 存在、与上表 exact set equality、discovered production edges 为空且不超过配置上限，并证明 cycle / forbidden / internal 三类 test-only fixture 均被 `Violations` 拒绝。测试类位于 `com.onecall.archtests`，fixtures 位于 `com.onecall.archfixtures`，不会成为第 15 个模块。

## Modulith moments auto-configuration

`spring-modulith-starter-core` 传递引入 `spring-modulith-moments`，其 `MomentsAutoConfiguration` 启用 `@EnableScheduling` 并创建非 daemon 调度线程，使本无 domain event 的 bootstrap JVM 启动后无法退出。S1 在 `application.yml` 通过 `spring.autoconfigure.exclude` 排除该未使用的 auto-configuration；依赖本身保留。后续 domain work 需要时间事件时，必须显式重新启用并评审生命周期/调度线程配置。

## CI（`.github/workflows/ci.yml`）

- 两个 job、只读权限（`contents: read`），只在 `backend/**`、contract assets（`api/openapi/**`、`protocol/**`、`security/**`、`deployment/capacity/**`、`acceptance/**`）、本 workflow 与顶层 `docs/*.md` 变化时触发；prototype/screenshot-only 变更不触发。
- `java-runtime`：Temurin `21.0.13+9`（`actions/setup-java` 精确 pin，不用 `check-latest`）、`./mvnw` 断言 Maven `3.9.9`、先 `docker info`、再 `./mvnw -B -ntp -Pcontainer-smoke verify`，随后解析 failsafe/surefire XML（恰好 1 个 container test、`skipped=0`），并执行 boot jar smoke 与 production jar firewall。
- `contract-regressions`：Node `22.23.4` 精确 pin、`npm ci` + `npm run check:s3`，以及 A2/A3/A4/A5 既有 validator 命令（不修改其 fixtures/语义）。
- `docker info` 在任何 Maven/容器步骤之前；Docker 不可用时 job 立即失败，不会出现“测试被 skip 但 job 绿”。

## 证据边界（重要）

- 本工程只提供 **build/static/synthetic** 证据：Maven 构建、Enforcer 规则、Modulith 边界测试、classpath/jar firewall、boot smoke、真实 PostgreSQL container smoke，以及 A1-A5 静态/合成 contract regressions。
- 默认 `./mvnw verify` 不需要 Docker，且**不等于**容器 smoke 通过；author 环境的 container smoke 可以是 `NOT RUN`，但 S2 accepted checkpoint 前必须由 controller 在 Docker-capable 环境实际执行 `-Pcontainer-smoke verify` 并通过。
- 本地/CI 成功都**不是** A5 `PASS`、`productionAccepted=true` 或生产验收；不证明 carrier/IVR media/腾讯云/provider/load/restore/penetration/manual/production acceptance，不创建 A5 evidence/authority/acceptance record。GitHub Actions 结果只有在真实运行时才算证据，未运行前不得声称 CI 绿。
