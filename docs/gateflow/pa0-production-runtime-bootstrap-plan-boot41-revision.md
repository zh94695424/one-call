# PA0 运行时基线修订说明：Boot 4.1.x + Modulith 2.1.x

> Task label：`pa0-plan-boot41-revision-ds-20261027-01`
>
> Work unit：`PA0-production-runtime-bootstrap`
>
> Gate：`plan`（仅计划修订与权威证据；不实施、不进入 review/implementation Gate）
>
> Status：**COMPLETE**
>
> Date：2026-10-27（Asia/Shanghai；检索 UTC `2026-10-26T20:05Z`–`20:11Z`）
>
> Author：DeepSeek（`deepseek-flash[1m]`）
>
> 关联 active plan：`docs/gateflow/pa0-production-runtime-bootstrap-plan.md`
>
> 历史边界：accepted checkpoint `c241cbd272d2f58d4ecf7d0e66737a4c768631bf`；历史 S1 BLOCKED 报告 `docs/gateflow/pa0-production-runtime-bootstrap-s1-implementation.md`。两者保持不可变，仅作历史证据。

## 1. 为什么旧计划被替代

1. 历史 accepted plan（checkpoint `c241cbd`，plan SHA-256 `5C3DC6C2FB9E0FF976B566249CEFBA49E64CCF4068573224190A8E670B317D51`）固定 **Spring Boot `3.5.16` + Spring Modulith `1.4.14`**。
2. 历史 S1 实现报告以 fail-closed preflight 记录：Spring 官方 API 对 `spring-boot` generation `3.5.x` 返回 `support=extended`、`ossSupportEndDate=2026-06-30`；`3.5.16` 是该线最后一个 OSS patch。2026-10-26 已超出 OSS 支持窗口 118 天。
3. 历史 accepted plan §4.3(4) 明确要求：若已超出 OSS 支持窗口，停止并报告，不得静默升级。因此 S1 未创建任何 `backend/**` 代码，只留下 BLOCKED 报告。该报告**不是**实现证据，也不是 acceptance。
4. 用户于 2026-10-27 在 controller 的 revised goal confirmation 中显式选择迁移路径，将硬目标修订为 **Spring Boot 4.1.x + Spring Modulith 2.1.x**；**Boot 4.0.x 明令禁止**。该决定属于支持治理/基线决策，不是新增 domain 行为或第三个 slice 的授权。
5. 本修订只把 active plan 的版本、POM、Modulith API 代码样例、preflight/CI 断言和 residual 更新到新目标线；不修改实现范围、14 个模块、allowedDependencies map、S1/S2 行为边界或 A0-A5 契约。

## 2. 精确目标与 pin

| 组件 | Pin | 结论 |
| --- | --- | --- |
| Spring Boot | **`4.1.2`** | `4.1.x` 线最高 GA；不是 `4.0.x`，不是 RC/milestone/snapshot |
| Spring Modulith | **`2.1.2`** | `2.1.x` 线最高 GA；`linkedGenerations.spring-boot=["4.1.x"]` |
| Java | **Temurin `21.0.13+9`** | 用户接受基线；Boot `4.1.2` 官方要求 Java 17–27，Java 21 在范围内 |
| Maven | **`3.9.9`** | Boot `4.1.2` 官方要求 Maven 3.6.3+；3.9.9 满足 |
| Maven Wrapper | **`3.3.2`**，`only-script` | 保持既有计划；SHA-256 `4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c` |
| Node | **`22.23.4`** | 保持既有 A1–A5 harness pin；本会话 `node --version` = `v22.23.4` |
| Testcontainers | **`2.0.5`** | Boot `4.1.2` BOM 也管理为 `2.0.5` |
| PostgreSQL JDBC | **Boot `4.1.2` BOM 管理 → `42.7.14`** | test scope；真实 dependency tree 已确认 |
| Maven Enforcer Plugin | **`3.6.3`** | Boot `4.1.2` BOM 管理版本；真实 `validate` Rule 0–4 全绿 |
| PostgreSQL image | `postgres:16` | 仍为 test-only design family，不是生产 DB 版本承诺；digest 仍为 residual |

**硬约束**：Boot `4.0.x`、Boot `3.x`、Modulith `1.x`、Modulith `2.0.x` 均不是允许 fallback；任何解析到这些版本的结果都必须 fail closed，不得以“latest”或未证明 patch 替代。

## 3. 权威证据（实际执行）

### 3.1 检索时间

```text
date (Asia/Shanghai) = 2026-10-27T04:11:17+0800
date -u              = 2026-10-26T20:11:17Z
```

所有 HTTP 读取均在上述时间窗口完成。以下 URL 均返回 HTTP `200`。

### 3.2 Spring 官方项目/支持 API

| URL | HTTP | 关键响应字段 |
| --- | --- | --- |
| `https://api.spring.io/projects/spring-boot/generations/4.1.x` | 200 | `name=4.1.x`；`initialReleaseDate=2026-06-30`；`support=default`；`ossSupportEndDate=2027-07-31`；`commercialSupportEndDate=2028-07-31`；`latestPatch.oss=4.1.2`；`latestPatch.enterprise=4.1.0.1`；`linkedGenerations.spring-boot=["4.1.x"]` |
| `https://api.spring.io/projects/spring-boot/releases/4.1.2` | 200 | `version=4.1.2`；`status=GENERAL_AVAILABILITY`；`current=true` |
| `https://api.spring.io/projects/spring-modulith` | 200 | `name=Spring Modulith`；`status=ACTIVE_UNSUPPORTED` |
| `https://api.spring.io/projects/spring-modulith/generations/2.1.x` | 200 | `name=2.1.x`；`initialReleaseDate=2026-06-30`；`support=none`；`linkedGenerations.spring-boot=["4.1.x"]`；`latestPatch.oss=2.1.2` |
| `https://api.spring.io/projects/spring-modulith/releases/2.1.2` | 200 | `version=2.1.2`；`status=GENERAL_AVAILABILITY`；`current=true` |

### 3.3 Maven Central metadata 与 POM

以下 `.../` 省略公共前缀 `https://repo.maven.apache.org/maven2/`。

| URL | HTTP | 结果 |
| --- | --- | --- |
| `https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml` | 200 | Boot `4.0.0`–`4.0.9` 与 `4.1.0`–`4.1.2` 存在；`latest/release=4.2.0-RC1`（RC，不采用）；`lastUpdated=20261022123848`；`4.1.x` 线最高 GA 为 `4.1.2` |
| `.../spring-boot-starter-parent/4.1.2/spring-boot-starter-parent-4.1.2.pom` | 200 | `Last-Modified: 2026-10-22T12:03:58Z`；`<java.version>17</java.version>`；`<maven.compiler.release>${java.version}</maven.compiler.release>`（项目显式覆盖为 21） |
| `.../spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom` | 200 | `spring-framework.version=7.0.10`；`postgresql.version=42.7.14`；`testcontainers.version=2.0.5`；`maven-enforcer-plugin.version=3.6.3`；`maven-failsafe-plugin.version=3.5.6`；`maven-surefire-plugin.version=3.5.6`；`junit-jupiter.version=6.0.3`；`assertj.version=3.27.7` |
| `.../spring-modulith/spring-modulith-bom/maven-metadata.xml` | 200 | Modulith `2.0.0`–`2.0.9` 与 `2.1.0`–`2.1.2` 存在；`latest/release=2.2.0-RC1`（RC，不采用）；`lastUpdated=20261022132443`；`2.1.x` 线最高 GA 为 `2.1.2` |
| `.../spring-modulith/spring-modulith-bom/2.1.2/spring-modulith-bom-2.1.2.pom` | 200 | BOM 内所有 `spring-modulith-*` 版本为 `2.1.2`；`scm.tag=2.1.x` |
| `.../spring-modulith/spring-modulith-api/2.1.2/spring-modulith-api-2.1.2.pom` | 200 | compile 依赖 `org.springframework.boot:spring-boot-autoconfigure:4.1.2`、`org.springframework:spring-tx:7.0.9`、`org.springframework:spring-context:7.0.9`、`org.jspecify:jspecify:1.0.1` |
| `.../spring-modulith/spring-modulith-starter-core/2.1.2/spring-modulith-starter-core-2.1.2.pom` | 200 | 依赖 `spring-modulith-api/apt/core/moments:2.1.2` 与 `spring-boot-starter:4.1.2`；`spring-modulith-core` 为 runtime scope |
| `.../org/testcontainers/testcontainers/maven-metadata.xml` | 200 | `release=2.0.5`；`lastUpdated=20260420182013` |
| `.../maven-enforcer-plugin/3.6.3/maven-enforcer-plugin-3.6.3.pom` | 200 | `Last-Modified: 2026-05-15T10:39:16Z`；metadata `release=3.6.3` |

### 3.4 官方文档兼容性

| URL | HTTP | 关键内容 |
| --- | --- | --- |
| `https://docs.spring.io/spring-boot/4.1.2/system-requirements.html` | 200 | “Spring Boot 4.1.2 requires at least Java 17 and is compatible with versions up to and including Java 27.”；“Maven 3.6.3 or later” |
| `https://docs.spring.io/spring-modulith/docs/current/api/org/springframework/modulith/ApplicationModule.html` | 200 | `allowedDependencies` 默认值显示为 `{"¯\_(ツ)_/¯"}`；Javadoc 明确 “Declaring an empty array will allow no dependencies to other modules. To not restrict the dependencies at all, leave the attribute at its default value.” |
| `https://docs.spring.io/spring-modulith/reference/2.1.2/index.html` | 200 | Modulith 2.1 参考文档可读；compatibility 章节指向矩阵 |
| `https://docs.spring.io/spring-modulith/reference/2.1.2/appendix.html` | 200 | 兼容性矩阵未列出 2.1（附录滞后）；因此以 `api.spring.io` `linkedGenerations` 与 2.1.2 POM 依赖为权威兼容证据，不以附录缺失作为否定证据 |

### 3.5 Modulith 2.1.2 API 兼容性（published sources）

下载并解包：

```text
https://repo.maven.apache.org/maven2/org/springframework/modulith/spring-modulith-api/2.1.2/spring-modulith-api-2.1.2-sources.jar   HTTP 200, 14312 bytes
https://repo.maven.apache.org/maven2/org/springframework/modulith/spring-modulith-core/2.1.2/spring-modulith-core-2.1.2-sources.jar  HTTP 200, 94555 bytes
```

实际检视结果：

1. `org/springframework/modulith/ApplicationModule.java`：`String[] allowedDependencies() default { OPEN_TOKEN };`；`OPEN_TOKEN = "¯\\_(ツ)_/¯"`；Javadoc 与 published Javadoc 一致。
2. `org/springframework/modulith/core/ApplicationModule.java`：`AllowedDependencies.isOpen` 仅在列表为 singleton `OPEN_TOKEN` 时视为 open；因此显式 `allowedDependencies = {}` 是 deny-all。
3. **API 变化**：`org.springframework.modulith.core.Module` 在 2.1.2 已不存在；`ApplicationModules.stream()` 的返回类型为 `Stream<org.springframework.modulith.core.ApplicationModule>`。
4. 模块名通过 `ApplicationModule.getIdentifier().toString()` 获取；直接依赖通过 `getDirectDependencies(ApplicationModules, DependencyType...)` 返回 `ApplicationModuleDependencies`，其 `uniqueModules()` 返回 `Stream<ApplicationModule>`。
5. `ApplicationModules.verify()` 仍存在，内部调用 `detectViolations(...).throwIfPresent()`；`Violations` 仍在 `org.springframework.modulith.core`。

因此 active plan §6.4 的测试代码已从 1.4 的 `Module::getName` / Stream 形式更新为 2.1.2 的 `getIdentifier()` / `uniqueModules()` 形式；这不是 scope 变化，而是 API 兼容性修正。

### 3.6 真实 Maven preflight（仓库外临时 POM）

临时 POM：`/tmp/pa0-boot41-preflight/pom.xml`（不属于仓库产物），内容镜像 active plan §5.2/§5.3：Boot parent `4.1.2`、Modulith BOM `2.1.2`、`spring-boot-starter`、`spring-modulith-starter-core`、test-scope `spring-boot-starter-test`、`testcontainers 2.0.5`、Boot-managed `postgresql`；Enforcer `3.6.3` 含 `requireJavaVersion [21,22)`、`requireMavenVersion [3.9.9,4.0)`、`banDuplicatePomDependencyVersions`、`dependencyConvergence`、`bannedDependencies`。

执行环境：`/tmp/pa0-tools/jdk-21.0.13+9`（Temurin `21.0.13+9`）与 `/tmp/pa0-tools/apache-maven-3.9.9`，显式设置 `JAVA_HOME` / `MAVEN_HOME`。

| 命令 | Exit | 关键结果 |
| --- | --- | --- |
| `java -version` | 0 | `openjdk version "21.0.13"` / `Temurin-21.0.13+9 (build 21.0.13+9-LTS)` |
| `mvn -v` | 0 | `Apache Maven 3.9.9`；`Java version: 21.0.13, vendor: Eclipse Adoptium` |
| `mvn -B -ntp -o dependency:resolve` | 0 | `BUILD SUCCESS` |
| `mvn -B -ntp -o dependency:tree -Dincludes=org.springframework.boot:*,org.springframework.modulith:*,org.testcontainers:*,org.postgresql:*` | 0 | 见下 |
| `mvn -B -ntp -o validate`（Enforcer 3.6.3） | 0 | Rule 0–4 全部 `passed`；`BUILD SUCCESS` |
| `mvn -B -ntp -o dependency:build-classpath -Dmdep.includeScope=test` | 0 | `BUILD SUCCESS`；输出 classpath 含 `spring-modulith-core-2.1.2.jar`，证明 runtime-scope core 在 test classpath 可用 |
| `docker version` / `docker info` | 0 | client=29.1.3、server=29.1.3；本修订未运行 container smoke（尚无 backend IT），该结果不构成 S2 证据 |

关键 dependency tree：

```text
org.springframework.boot:spring-boot-starter:jar:4.1.2:compile
org.springframework.boot:spring-boot-autoconfigure:jar:4.1.2:compile
org.springframework.boot:spring-boot:jar:4.1.2:compile
org.springframework.modulith:spring-modulith-starter-core:jar:2.1.2:compile
org.springframework.modulith:spring-modulith-api:jar:2.1.2:compile
org.springframework.modulith:spring-modulith-apt:jar:2.1.2:compile
org.springframework.modulith:spring-modulith-core:jar:2.1.2:runtime
org.springframework.modulith:spring-modulith-moments:jar:2.1.2:compile
org.springframework.boot:spring-boot-starter-test:jar:4.1.2:test
org.testcontainers:testcontainers:jar:2.0.5:test
org.postgresql:postgresql:jar:42.7.14:test
```

Enforcer 输出：

```text
Rule 0: RequireJavaVersion passed
Rule 1: RequireMavenVersion passed
Rule 2: BanDuplicatePomDependencyVersions passed
Rule 3: DependencyConvergence passed
Rule 4: BannedDependencies passed
BUILD SUCCESS
```

该 preflight 只关闭“坐标可解析/可收敛/Enforcer 规则可通过”和“计划期版本兼容”部分；不关闭 `backend/**` 的 compile/test/verify、boot jar、Docker container smoke、GitHub Actions 或任何 acceptance。

### 3.7 Boot 4.1 兼容性影响清单

| 检查项 | 结论 | 证据/处理 |
| --- | --- | --- |
| 最小依赖坐标 | **不变** | `spring-boot-starter`、`spring-modulith-starter-core`、`spring-boot-starter-test` 坐标仍存在且解析为 `4.1.2`/`2.1.2`；Testcontainers core 仍为 `org.testcontainers:testcontainers:2.0.5` |
| Java baseline | **兼容** | Boot `4.1.2` 要求 Java 17–27；目标 Java 21 在范围内；`maven.compiler.release=21` 保持 |
| Maven compiler / Enforcer 范围 | **保留 3.9.9；Enforcer 更新到 BOM 的 3.6.3** | Boot 4.1.2 要求 Maven 3.6.3+；compiler 3.15.0；Enforcer `[21,22)`/`[3.9.9,4.0)` 不变，插件版本 `3.6.3` 真实 validate 通过 |
| Modulith BOM import | **坐标不变，版本更新** | `org.springframework.modulith:spring-modulith-bom:2.1.2` import；starter-core 由 BOM 管理 |
| 应用入口/配置 | **不变** | `org.springframework.boot.SpringApplication` 与 `org.springframework.boot.autoconfigure.SpringBootApplication` 包路径在 4.1.2 仍存在；`spring.main.web-application-type=none` 仍由 `SpringApplication` 支持 |
| 测试 API | **有兼容性修正** | Boot 测试注解 `@SpringBootTest(webEnvironment=NONE)` 仍存在；Modulith `core.Module` 已移除，改为 `core.ApplicationModule` + `getIdentifier()` + `uniqueModules()`；测试代码已在 plan §6.4 更新 |
| jar firewall | **布局不变，防火墙规则不变** | Boot 4.1.2 loader sources 仍使用 `BOOT-INF/lib`；Enforcer `bannedDependencies` 全通过；`jar tf` 断言保持 |

### 3.8 可复现命令

```bash
# 1) Spring 官方 API（HTTP 200）
curl -sS https://api.spring.io/projects/spring-boot/generations/4.1.x
curl -sS https://api.spring.io/projects/spring-boot/releases/4.1.2
curl -sS https://api.spring.io/projects/spring-modulith
curl -sS https://api.spring.io/projects/spring-modulith/generations/2.1.x
curl -sS https://api.spring.io/projects/spring-modulith/releases/2.1.2

# 2) Maven Central metadata / POM
curl -sS https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml
curl -sS https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/4.1.2/spring-boot-starter-parent-4.1.2.pom
curl -sS https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom
curl -sS https://repo.maven.apache.org/maven2/org/springframework/modulith/spring-modulith-bom/maven-metadata.xml
curl -sS https://repo.maven.apache.org/maven2/org/springframework/modulith/spring-modulith-bom/2.1.2/spring-modulith-bom-2.1.2.pom
curl -sS https://repo.maven.apache.org/maven2/org/springframework/modulith/spring-modulith-api/2.1.2/spring-modulith-api-2.1.2.pom
curl -sS https://repo.maven.apache.org/maven2/org/springframework/modulith/spring-modulith-starter-core/2.1.2/spring-modulith-starter-core-2.1.2.pom

# 3) 真实 Maven preflight（临时 POM 在 /tmp，不属于仓库）
cd /tmp/pa0-boot41-preflight
JAVA_HOME=/tmp/pa0-tools/jdk-21.0.13+9 \
MAVEN_HOME=/tmp/pa0-tools/apache-maven-3.9.9 \
/tmp/pa0-tools/apache-maven-3.9.9/bin/mvn -B -ntp -U dependency:resolve
JAVA_HOME=/tmp/pa0-tools/jdk-21.0.13+9 \
MAVEN_HOME=/tmp/pa0-tools/apache-maven-3.9.9 \
/tmp/pa0-tools/apache-maven-3.9.9/bin/mvn -B -ntp dependency:tree \
  -Dincludes=org.springframework.boot:*,org.springframework.modulith:*,org.testcontainers:*,org.postgresql:*
JAVA_HOME=/tmp/pa0-tools/jdk-21.0.13+9 \
MAVEN_HOME=/tmp/pa0-tools/apache-maven-3.9.9 \
/tmp/pa0-tools/apache-maven-3.9.9/bin/mvn -B -ntp validate
```

## 4. 支持/安全窗口判定

### 4.1 Spring Boot 4.1.x

- `support=default`；`ossSupportEndDate=2027-07-31`；`commercialSupportEndDate=2028-07-31`；`latestPatch.oss=4.1.2`；`4.1.2` 为 `GENERAL_AVAILABILITY` 且 `current=true`。
- 以 2026-10-27 为目标日期，`4.1.x` 仍在 OSS 支持窗口内（约 9 个月余量），**可接受**。
- 未选择 `4.0.x`；`4.0.x` 虽存在于 Maven Central，但被用户明令禁止，不能作为 fallback。

### 4.2 Spring Modulith 2.1.x

- 项目级 `status=ACTIVE_UNSUPPORTED`；generation `support=none`；无 `ossSupportEndDate` / `commercialSupportEndDate`；`latestPatch.oss=2.1.2`；`2.1.2` 为 `GENERAL_AVAILABILITY` 且 `current=true`。
- 精确治理结论：**Modulith 2.1.x 没有独立发布的 OSS/商业支持窗口**；不能把 Boot 4.1 的 `2027-07-31` 当成 Modulith 的窗口。该状态不是 EOL 信号，但意味着没有 Spring 支持 SLA 承诺。
- 因为用户已显式固定 Modulith 2.1.x 且 `2.1.2` 是 current GA、与 Boot 4.1.x 官方 linked generation，本修订**不将该项作为 plan-scope blocker**，而是作为 `PA0-R-14` 的已披露 governance residual，交由 controller/revised plan review 显式接受。
- 若 controller 要求每个运行时组件都有 published OSS support window，则精确 blocker 是：**Spring Modulith 2.1.x 为 `ACTIVE_UNSUPPORTED` / `support=none`，无 published support dates**；解决路径只能是 release governance 接受该状态或重新修订目标线，不能由 author 静默替换为 Modulith 1.x/2.0.x 或未证明版本。

## 5. active plan 中被修改的章节

| 章节 | 修订内容 |
| --- | --- |
| Header | 增加 2026-10-27 revision 声明、硬目标、Boot 4.0.x 禁止、历史 checkpoint 仅为证据 |
| §0 | 明确 Boot `4.1.2`/Modulith `2.1.2`、支持窗口与 Modulith governance residual、preflight 证据状态 |
| §1.1/§1.2/§1.5 | 目标与证据状态从 Boot 3.5/Modulith 1.4 更新到 4.1.2/2.1.2 |
| §2.1/§2.3 | 增加基线修订历史行；说明不修改 `docs/12` 的治理决定与 scope 边界 |
| §4.1 | 全量版本 pin 表更新：Boot/Modulith/TC/PG/Enforcer；JDK/Maven/Node 证据更新 |
| §4.2/§4.3 | 证据三态改为 AUTHOR_VERIFIED / PREFLIGHT_REQUIRED；解析断言改为 4.1.2/2.1.2；增加禁止 4.0.x/3.x/1.x 与 API 兼容断言 |
| §4.4 | 写入已取得的 Maven 3.9.9 SHA-256 与官方 SHA-512 |
| §5.2/§5.3 | POM 版本更新；Enforcer 改 3.6.3；补充 failsafe/surefire/JUnit/AssertJ 管理版本与 Modulith core test classpath 事实 |
| §6.2 | deny-all 语义证据从 1.4.14 更新为 2.1.2；历史 1.4.14 仅作历史证据 |
| §6.4 | 测试代码改为 Modulith 2.1.2 API：`getIdentifier()`、`uniqueModules()`；移除 `Module` 引用；增加 import 冲突说明 |
| §6.5 | 版本引用更新为 2.1.2 |
| §7.1/§7.4 | Testcontainers 版本确认；failsafe 版本由 Boot 4.1.2 管理为 3.5.6 |
| §9.6 | 移除“docker 被权限门拒绝/daemon 未运行”的过时表述；记录本修订 Docker 可用但未执行 container smoke（尚无 backend IT） |
| §11.1 | 行为目标与 expected versions 更新；增加 resolved-version 不变量与 Modulith 2.1 API test-compile |
| §12 | 增加 Boot 4.0.x 静默替换、Modulith API 漂移、Modulith support=none 被忽略三类陷阱 |
| §13.3/§13.4/§13.5/§13.6 | Residual 更新（R-01/R-02/R-06/R-10、新增 R-14）；VQ-01/02/05 更新；停止条件增加 4.0.x/3.x/1.x/2.0.x 与 API 失配；完成报告增加 resolved version/support 字段 |
| §14/§15 | 增加“本次修订不增加能力/不增加第三 slice”说明与硬目标完成定义 |

## 6. 未改变的范围

以下保持与旧 accepted plan 完全一致：

- 单 Maven 项目、单可启动应用、14 个 Modulith 包边界；14 个模块名与 `allowedDependencies` map 不变。
- S1（Java/Maven 运行时、14 模块边界、依赖防火墙、无 Docker）与 S2（PostgreSQL Testcontainers smoke、CI 强制执行、A1–A5 回归）两个 behaviorally bounded slices；不增加第三 slice。
- 不实现 A0 44 表、V001–V009 migration、domain state machine、API/controller、Security/Flyway/jOOQ/Hikari/DataSource、provider/cloud/KMS/COS/IVR/WeChat/payment 集成。
- 不修改 `docs/12`–`docs/19`、A0–A5 assets/validators/fixtures/counts、prototypes/screenshots、controller、review/fix artifacts。
- 不发明生产凭据、容量、SLO/RPO/RTO、Region/AZ/SKU、retention 或 acceptance authority。
- CI 成功仍只等于 build/static/synthetic 证据，不是 A5 `PASS`/`productionAccepted=true`。

## 7. 迁移假设

1. **无数据库迁移、无 schema 变更、无 backfill**：PA0 仍不创建表/角色/grant/function；`postgres:16` 仅 test-only 空库 smoke。
2. **无源代码迁移**：历史 S1 BLOCKED 报告没有 `backend/**` 产物；S1 仍从零创建 scaffold。修订不把 BLOCKED 报告当作代码基线。
3. **无历史 artifact 重写**：`c241cbd` checkpoint、旧 fix/review artifacts、A0–A5 文档和 S1 BLOCKED 报告保持原样；本修订只新增本说明并修改 active plan。
4. **无 Boot 4.0.x 过渡期**：不先落 4.0.x 再升级；直接以 4.1.2 为唯一目标。
5. **无 Modulith 1.x/2.0.x 兼容层**：不引入双版本/反射 fallback；测试代码直接按 2.1.2 API 编写。
6. **S1 重新开始**：新 slice 派发时以 revised plan 为唯一契约；历史 S1 BLOCKED 报告中的工具链证据（Temurin/Maven/SHA）可作为环境证据复用，但不构成 `backend/**` 实现或验证完成。

## 8. 验证预期（S1/S2）

- S1 必须在 `backend/` 真实执行：`./mvnw -v`、`dependency:resolve`、`dependency:tree`、`validate`、`verify`、boot jar、jar firewall；断言 resolved Boot `4.1.2`、Modulith `2.1.2`、Testcontainers `2.0.5`、PostgreSQL `42.7.14`，且无 4.0.x/3.x/1.x/2.0.x。
- S1 必须 test-compile 通过 2.1.2 API 形式的 `ModuleBoundaryTests`（`getIdentifier()` / `uniqueModules()`）和 3 组负向 fixtures。
- S2 仍必须由 controller 在 Docker-capable 环境 pre-push 执行 `-Pcontainer-smoke verify`，failsafe `tests=1 skipped=0 failures=0 errors=0`；CI 首次真实运行是 post-push 附加证据，不能替代 pre-push run。
- 真实 GitHub Actions 首次运行仍需在 PR/controller gate 产生；本修订未执行 CI，不声称 CI 绿。

## 9. 残余风险

| ID | Residual | Owner / Destination |
| --- | --- | --- |
| `PA0-R-01` | 计划期兼容性已证明；`backend/**` 实际 build/test-compile/verify 尚未执行 | Platform/CI → S1 preflight + `verify` |
| `PA0-R-02` | Maven 3.9.9 SHA-256 已取得，但 wrapper 尚未生成/写入 `backend/` | Platform/CI → S1 wrapper preflight |
| `PA0-R-03` | 本修订环境 Docker 可用（`docker info` exit 0），但尚无 `backend/**`/container IT，container smoke 未执行 | controller pre-push Docker run 是 S2 accepted checkpoint 唯一前置 |
| `PA0-R-04` | GitHub Actions 首次真实运行未发生 | PA0 PR/controller gate；未运行前不得声称绿 |
| `PA0-R-05` | `postgres:16` digest 未 pin | S2/CI hardening |
| `PA0-R-06` | Boot 4.1.x OSS 支持到 2027-07-31；S1 时需复核 | Platform/Security → S1 preflight |
| `PA0-R-07` | Actions 仍用 major tag，非 commit SHA | Security/Platform → CI hardening |
| `PA0-R-08` | path-filter 与 branch protection 交互 | repository governance |
| `PA0-R-09` | 未来 cross-cutting Java 边未预授权 | first domain work unit + architecture review |
| `PA0-R-10` | 精确 JDK 已在本修订执行验证，但 S1 执行环境仍需安装/选择 | Platform/CI → S1 env proof |
| `PA0-R-11` | CI 绿可能被误读为生产验收 | README/§9.5 + A5/release governance |
| `PA0-R-12` | 无 formatter/checkstyle | 后续 code-quality work unit |
| `PA0-R-13` | allowedDependencies 是 prose 翻译上限 | architecture change control |
| `PA0-R-14` | **Modulith `2.1.x` 为 `ACTIVE_UNSUPPORTED` / `support=none`，无 published OSS/商业支持窗口** | Platform/Security + release governance → controller 在 revised plan review 显式接受；跟踪 2.1.x patch；不得静默替换 |

## 10. 本次修订实际执行的验证

| 验证 | 命令/结果 |
| --- | --- |
| 计划 whitespace | `git diff --check -- docs/gateflow/pa0-production-runtime-bootstrap-plan.md` → exit 0，无输出 |
| 本 artifact whitespace | 写入后执行 `git diff --no-index --check /dev/null docs/gateflow/pa0-production-runtime-bootstrap-plan-boot41-revision.md`；rc=1（新文件存在差异，属预期），输出 0 bytes，无 whitespace error |
| 禁止路径 | 本修订只修改 active plan + 本 artifact；`git status` 与修订前基线逐行 diff 证明无新增/修改禁止路径（pre-existing dirty paths 保持不变） |
| 版本 stale 扫描 | 对 active plan 搜索 `3.5.16|1.4.14|1.4.x|Boot 3.5|Boot 3.x|Boot 4.x|4.0.x`；所有命中均为历史说明或“禁止 fallback”语句，无 active target |
| 官方 API 读取 | 5 个 `api.spring.io` URL 均 HTTP 200；字段见 §3.2 |
| Maven Central 读取 | metadata/POM/sources 均 HTTP 200（关键 URL 与字段见 §3.3/§3.5；状态复核覆盖 9 个 repo URL） |
| 真实 Maven preflight | resolve/tree/validate 均 exit 0；见 §3.6 |
| 工具链 | Temurin `21.0.13+9`、Maven 3.9.9、Node `v22.23.4` 实际执行；见 §3.6 |

## 11. 完成状态

- **COMPLETE**：active plan 已按 Boot `4.1.2` + Modulith `2.1.2` 修订；本 artifact 记录原因、证据、变更、未变范围、迁移假设、验证预期和 residual。
- active plan 修订后 SHA-256 `6131FC6C28DB2C083138B23A3EB650DBF81C4BCEA193E565FADE51173BF742D1`（1364 行；若后续再编辑会变化，以 completion response 为准）；本 artifact 因自引用不内嵌自身 SHA-256。
- 未创建/修改 `backend/**`、`.github/**`、migration、container test、prototype/QA/screenshot、controller、历史 review/fix artifact 或 A0–A5 asset。
- 未 commit/stage/push/PR/merge，未进入 implementation/review Gate。
