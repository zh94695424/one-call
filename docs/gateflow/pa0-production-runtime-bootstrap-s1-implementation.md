# PA0 S1 实现报告（BLOCKED：Boot 3.5 OSS 支持窗口已结束）

> Task label：`pa0-s1-implementation-ds-20261026-resolved-01`
>
> Gate：`implementation`（仅 approved slice S1）
>
> Work unit：`PA0-production-runtime-bootstrap`
>
> Slice：S1 — Java/Maven 运行时、14 个 Modulith 模块边界与依赖防火墙（无 Docker）
>
> Base：accepted plan checkpoint `c241cbd272d2f58d4ecf7d0e66737a4c768631bf`
>
> Author：DeepSeek（`deepseek-flash[1m]`）

## 0. 结论

**Status: BLOCKED（fail-closed preflight：Spring Boot 3.5 OSS 支持窗口已于 2026-06-30 结束）**。

上一轮作者报告的 runner 权限门阻塞已由 controller 的 explicit allowed-tools dispatch 解除：本会话实际执行了 `java`、`mvn`、`curl`、Maven Central/Adoptium/Spring API 网络读取、精确 Temurin 下载、Maven 3.9.9 checksum 校验、真实 Maven dependency resolution 与 Enforcer validate。**该 runner 阻塞不再存在，也不作为产品/运行时风险保留。**

但 accepted plan §4.3(4) 要求记录 Boot 3.5.16 的官方 support/security 状态，并规定“若已超出仍受支持窗口，停止并报告，不得静默升级到 Boot 4.x”。Spring 官方项目 API 给出：

- Spring Boot `3.5.x` generation：`support=extended`，`ossSupportEndDate=2026-06-30`，`commercialSupportEndDate=2032-06-30`，`latestPatch.oss=3.5.16`，`latestPatch.enterprise=3.5.17`。
- 当前日期 `2026-10-26` 已晚于 OSS 支持结束日 `2026-06-30` 118 天。
- Spring Modulith `1.4.x` generation：`support=none`，`linkedGenerations.spring-boot=[3.5.x]`，`latestPatch.oss=1.4.14`。

即：accepted plan 固定的 `Boot 3.5.16 + Modulith 1.4.14` 是 **OSS 支持已结束** 的 `3.5.x` 线的最后一个 OSS patch。继续使用需要 release governance 的明确决定（例如购买/确认 Spring Enterprise 商业支持覆盖 Boot 3.5.16），或由 controller 重新评审迁移到仍受支持的 Boot 线（Boot 4.x/Modulith 2.x，超出当前 accepted plan 与 goal 的“Boot 3.x”）。作者不得自行选择、不得静默升级、不得把商业支持存在性当作已成立事实。

因此本 slice **未创建任何 `backend/**` 文件**，未运行 `./mvnw verify`、boot jar、jar firewall 或 mismatch probes；只更新本报告。没有 commit/push/PR/stage/merge，未进入其他 Gate。

## 1. 工具链 preflight（实际执行，非声明）

所有命令均使用本会话下载到 `/tmp/pa0-tools` 的临时工具；未修改系统/全局配置。临时工具目录不属于仓库产物。

### 1.1 精确 Temurin `21.0.13+9`

| 项 | 实际结果 |
| --- | --- |
| 来源 | Adoptium API assets：`https://api.adoptium.net/v3/assets/feature_releases/21/ga?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=linux&vendor=eclipse&page_size=20`（HTTP 200） |
| 选中 release | `release_name=jdk-21.0.13+9`，`openjdk_version=21.0.13+9-LTS`，`build=9`，`security=13` |
| 官方 asset | `OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz`，`size=207858352` |
| 官方 SHA-256（Adoptium API `package.checksum`） | `d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c` |
| 下载 | 直接 GitHub release 下载在 300s 超时（1,716,657 / 207,858,352 bytes）；改从清华 Adoptium 镜像 `https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/linux/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz` 完整取得 `207858352` bytes，Last-Modified `2026-10-22T12:07:52Z`，与官方 asset size/mtime 一致 |
| 字节校验 | `sha256sum OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz` = `d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c`，与 Adoptium API 官方 checksum **逐字节相等** |
| 执行验证 | `/tmp/pa0-tools/jdk-21.0.13+9/bin/java -version` exit 0：`openjdk version "21.0.13" 2026-10-20 LTS` / `OpenJDK Runtime Environment Temurin-21.0.13+9 (build 21.0.13+9-LTS)` / `OpenJDK 64-Bit Server VM Temurin-21.0.13+9 (build 21.0.13+9-LTS, mixed mode, sharing)` |

结论：精确 patch `21.0.13` + Temurin build `+9` **已获得并验证**。既有 `21.0.12.1+1` 仅作诊断，未用于任何 S1 判定。（官方 GitHub checksum 文件下载在 60s 超时、0 bytes；改用 Adoptium API 官方 asset checksum，来源与字节均已记录，不构成猜测。）

### 1.2 Apache Maven 3.9.9 与 wrapper checksum

| 项 | 实际结果 |
| --- | --- |
| 下载 URL | `https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip`（HTTP 200，`9202456` bytes） |
| 官方 SHA-512 文件 | `apache-maven-3.9.9-bin.zip.sha512`（HTTP 200）：`8beac8d11ef208f1e2a8df0682b9448a9a363d2ad13ca74af43705549e72e74c9378823bf689287801cbbfc2f6ea9596201d19ccacfdfb682ee8a2ff4c4418ba` |
| 本地校验 | `sha512sum apache-maven-3.9.9-bin.zip` 与官方值 **逐字符相等**；与任务给定值一致 |
| 派生 SHA-256 | `sha256sum apache-maven-3.9.9-bin.zip` = `4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c` |
| 执行验证 | 解压后 `mvn -version` exit 0：`Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)`；`Java version: 21.0.13, vendor: Eclipse Adoptium` |

结论：Maven zip 字节、官方 SHA-512、派生 SHA-256 与 Maven 3.9.9 执行版本均已证明。由于 S1 在实现前被支持状态阻塞，未生成/写入 `backend/mvnw`、`backend/mvnw.cmd` 或 `backend/.mvn/wrapper/maven-wrapper.properties`；上述 SHA-256 是后续 S1 重派时必须固定的值。

### 1.3 依赖解析与兼容性（临时 preflight 工程）

在仓库外 `/tmp/pa0-preflight/pom.xml`（SHA-256 `315275b8aa9a1c5dc92cc2f9420b3c53d5961f21930142e6248afff4800f8b25`）使用与 accepted plan §5.2/§5.3 相同的 parent/BOM/依赖/Enforcer 规则执行真实 Maven 命令；该 POM 是临时 preflight 文件，不是仓库产物。

| 命令 | 实际结果 |
| --- | --- |
| `mvn -B -ntp -U dependency:resolve` | exit 0；`BUILD SUCCESS`；总耗时 8:03 min |
| `mvn -B -ntp dependency:tree -Dincludes=org.springframework.boot:*,org.springframework.modulith:*,org.testcontainers:*,org.postgresql:*` | exit 0；`BUILD SUCCESS` |
| `mvn -B -ntp validate`（Enforcer 3.5.0: requireJavaVersion `[21,22)`、requireMavenVersion `[3.9.9,4.0)`、banDuplicatePomDependencyVersions、dependencyConvergence、bannedDependencies searchTransitive） | exit 0；Rule 0-4 全部 `passed`；`BUILD SUCCESS` |

解析到的关键版本：

```text
org.springframework.boot:spring-boot:3.5.16
org.springframework.boot:spring-boot-starter:3.5.16
org.springframework.boot:spring-boot-starter-test:3.5.16
org.springframework.modulith:spring-modulith-starter-core:1.4.14
org.springframework.modulith:spring-modulith-api:1.4.14
org.springframework.modulith:spring-modulith-apt:1.4.14
org.springframework.modulith:spring-modulith-core:1.4.14
org.springframework.modulith:spring-modulith-moments:1.4.14
org.testcontainers:testcontainers:2.0.5
org.postgresql:postgresql:42.7.11   (Boot 3.5.16 BOM 管理，test scope)
org.springframework:spring-core:6.2.19
org.slf4j:slf4j-api:2.0.18
```

Maven Central 直接探测（HTTP 200）：`spring-boot-starter-parent-3.5.16.pom`、`spring-modulith-bom-1.4.14.pom`、`testcontainers-2.0.5.pom`、`maven-enforcer-plugin-3.5.0.pom`。

结论：Boot `3.5.16`、Modulith `1.4.14`、Testcontainers `2.0.5`、Enforcer `3.5.0` 与 Boot-BOM PostgreSQL `42.7.11` **可解析、可收敛**，Enforcer 全绿。该证据只关闭“解析/收敛”前置，不关闭官方支持状态阻塞。

## 2. 官方 Spring Boot 3.5 support/security 状态（阻塞点）

### 2.1 权威来源与实际响应

来源：Spring 官方项目 API（`api.spring.io`，Spring 网站 support 数据源）。

| 请求 | 实际结果 |
| --- | --- |
| `GET https://api.spring.io/projects/spring-boot/generations/3.5.x` | HTTP 200 |
| 响应字段 | `name=3.5.x`；`initialReleaseDate=2025-05-31`；`support=extended`；`ossSupportEndDate=2026-06-30`；`commercialSupportEndDate=2032-06-30`；`latestPatch.oss=3.5.16`；`latestPatch.enterprise=3.5.17` |
| `GET https://api.spring.io/projects/spring-boot/releases/3.5.16` | HTTP 200；`version=3.5.16`；`status=GENERAL_AVAILABILITY`；`current=false` |
| `GET https://api.spring.io/projects/spring-modulith/generations/1.4.x` | HTTP 200；`name=1.4.x`；`initialReleaseDate=2025-05-31`；`support=none`；`linkedGenerations.spring-boot=[3.5.x]`；`latestPatch.oss=1.4.14` |
| 当前时间 | `date -u` = `2026-10-26T15:58:31Z` |

交叉核对（非唯一依据）：`https://endoflife.date/api/spring-boot.json` 对 cycle `3.5` 给出 `eol=2026-06-30`、`latest=3.5.16`、`latestReleaseDate=2026-06-25`、`extendedSupport=2032-06-30`，与 Spring API 的 OSS 结束日一致。

### 2.2 判定

1. accepted plan 固定 `Boot 3.5.16`；它是 `3.5.x` 的最后一个 OSS patch，而 `3.5.x` 的 OSS 支持已于 `2026-06-30` 结束。
2. `commercialSupportEndDate=2032-06-30` 只说明存在 Spring Enterprise 商业支持轨道；仓库与 accepted plan 中没有任何商业支持订阅/治理授权证据。作者不得把“可购买商业支持”当作已获支持或自行决定采购。
3. accepted plan §4.3(4) 明文要求“若已超出仍受支持窗口，停止并报告，不得静默升级到 Boot 4.x”。`3.5.x` 已超出 OSS 支持窗口；迁移到 `4.x` 超出当前 accepted plan 与 goal 的 “Boot 3.x”，必须重新评审，不能由实现作者决定。
4. Modulith `1.4.x` 与 Boot `3.5.x` 绑定，`support=none`、`latestPatch.oss=1.4.14`，不提供独立的支持窗口证据。

因此按 accepted plan §4.3、§13.5、§13.4 VQ-05 以及本任务 stop condition，S1 在实现前 **fail-closed 停止**。这不是 runner 权限问题，而是需要 release governance / controller 决策的产品支持状态问题。

## 3. 变更文件

| Path | 状态 | 说明 |
| --- | --- | --- |
| `docs/gateflow/pa0-production-runtime-bootstrap-s1-implementation.md` | 更新（untracked artifact） | 本 BLOCKED 报告；替换上一轮 runner-权限 BLOCKED 报告 |

- `backend/**`：**0 新增、0 修改**。未创建 `pom.xml`、`mvnw`、`.java-version`、源码、测试、`application.yml` 或 README。
- 本报告行数：`212`。
- 本报告 SHA-256：文件内容自引用会改变自身哈希，最终值由 author 在 completion response 中记录。
- 临时工具/日志（`/tmp/pa0-tools/**`、`/tmp/pa0-preflight/**`）不属于仓库，不会被提交。

## 4. 未执行的 S1 验证（NOT RUN，不是 PASS）

由于 preflight 在支持状态处 fail-closed，以下 accepted plan §11.1 命令均 **NOT RUN**：

```text
grep -Fx '21.0.13+9' .java-version                       NOT RUN (未创建 backend/.java-version)
./mvnw -B -ntp -v                                        NOT RUN (未生成 wrapper)
./mvnw -B -ntp -U dependency:resolve                     NOT RUN in backend (临时 preflight 已执行)
./mvnw -B -ntp dependency:tree ...                       NOT RUN in backend (临时 preflight 已执行)
./mvnw -B -ntp verify                                    NOT RUN
java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar      NOT RUN
grep -F 'Started OneCallApplication' target/boot.log     NOT RUN
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar         NOT RUN
4 mismatch probes / 3 negative fixtures / firewall tests NOT RUN
container smoke / CI                                     N/A for S1 (S2 scope)
```

语义断言状态：

```text
module_set_exact=NOT OBSERVED (no backend source)
annotation_present=NOT OBSERVED (0/14)
allowed_dependency_map_exact=NOT OBSERVED
mismatch_probes=NOT RUN (0/4)
cycle_rejected=NOT OBSERVED
forbidden_rejected=NOT OBSERVED
internal_rejected=NOT OBSERVED
forbidden_frameworks_absent=NOT OBSERVED
no_datasource_flyway_security_beans=NOT OBSERVED
boot_log_started=NOT OBSERVED
jar_exit=NOT OBSERVED
runtime_jar_forbidden_libs=NOT OBSERVED
container_tests=NOT RUN
```

## 5. A1-A5 回归

**NOT RUN**。本 slice 实现阶段未进入 build/test；S1 也不要求运行 A1-A5。未修改任何 A1-A5 asset/validator/fixture/count/doc。未运行 container smoke 或 CI。

## 6. 残余风险与分类

| ID | 内容 | 分类 / Owner-Destination |
| --- | --- | --- |
| `PA0-S1-SUPPORT-01`（本次阻塞） | accepted pin `Boot 3.5.16 + Modulith 1.4.14` 属于 OSS 支持已于 `2026-06-30` 结束的 `3.5.x`/`1.4.x` 线；商业 extended support 到 `2032-06-30` 但无订阅/治理授权证据 | **requiring new issue or explicit user decision**：由 release governance/controller 决定 (a) 授权并证明 Spring Enterprise 商业支持覆盖 Boot 3.5.16，或 (b) 重新评审迁移到仍受支持的 Boot 线（4.x/Modulith 2.x，超出当前 plan/goal），或 (c) 暂停 PA0 |
| `PA0-R-01` | Boot/Modulith/Testcontainers/Enforcer GA 解析与收敛 | **fixed in current preflight (resolution portion)**：真实 Maven resolve/tree + Enforcer 全绿；完整 compile/verify 因支持阻塞未执行 |
| `PA0-R-02` | Maven 3.9.9 bin zip SHA-256 | **fixed in current slice (preflight)**：`4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c`；wrapper 尚未生成 |
| `PA0-R-06` | Boot 3.5 line support/security 窗口 | **本次阻塞；requiring new issue or explicit user decision**：Spring API 证明 OSS EOL `2026-06-30` |
| `PA0-R-10` | 精确 JDK `21.0.13+9` | **fixed in current preflight**：已下载、官方 SHA-256 校验并执行 `java -version`；未写入仓库 `.java-version`（实现未开始） |
| `PA0-R-03` | author Docker/container smoke | 维持 plan：**controller pre-push Docker run 是 S2 accepted checkpoint 前置**；本 slice 未触碰 |
| `PA0-R-04` | GitHub Actions 首次真实运行 | **assigned to later work unit**：PA0 PR/controller gate；未运行 |
| `PA0-R-05` | `postgres:16` digest 未 pin | **assigned to later work unit**：S2/CI hardening |
| `PA0-R-07` | Actions major tag 非 commit SHA | **assigned to later work unit**：CI hardening/release governance |
| `PA0-R-08` | path-filter 与 branch protection 交互 | **tracked by existing issue / repository governance** |
| `PA0-R-09` | cross-cutting platform/audit/sharing/evidence Java 边未预授权 | **assigned to later work unit**：first domain work unit + architecture review |
| `PA0-R-11` | CI 绿被误读为生产验收 | **covered by later approved slice**：README/CI 语义 + A5/release governance |
| `PA0-R-12` | 无 formatter/checkstyle | **assigned to later work unit**：code-quality work unit |
| `PA0-R-13` | allowedDependencies 是 prose 翻译的上限 | **assigned to later work unit**：architecture change control |

上一轮报告的 `PA0-S1-BLOCKER-01`（runner 权限门拒绝 Java/Maven/网络）已由 explicit allowed-tools dispatch 解决，**关闭，不再保留**。

## 7. 解除阻塞所需的 controller/governance 决策

作者不能自行选择以下任一路径：

1. **商业支持路径**：release governance 明确批准 `Boot 3.5.16` 的 OSS EOL 状态可接受，并提供 Spring Enterprise 商业支持订阅/覆盖证据（`commercialSupportEndDate=2032-06-30` 对应的具体 entitlement），然后重新派发 S1。S1 仍须以精确 Temurin `21.0.13+9`、Maven 3.9.9 wrapper SHA-256 `4ec3f26f...` 和本报告记录的可解析版本执行完整 verify/boot/jar/probes。
2. **重新计划路径**：controller 修改 accepted plan/goal，允许迁移到仍受 OSS 支持的 Boot 4.x/Modulith 2.x，并重新执行 plan review；这不是当前 accepted plan 的 S1 实现。
3. **暂停/阻塞路径**：若既不能建立商业支持授权，也不允许修改 Boot 3.x goal，则将 PA0 标记 blocked/重新调度。

不得用“后续 issue 会升级 Boot”“CI 会补”或“商业支持大概可以购买”来替代上述治理决定。

## 8. 范围与禁止路径检查

- 仅更新本报告；`backend/**` 无任何文件。
- 未修改 `docs/gateflow/project-execution-control.md`（read-only）。
- 未修改任何既有 docs、prototype/QA/screenshot、`.codex-tasks/**`、`docs/reviews/plan-review-20261021-205118.md`、`.github/**`。
- 未创建容器测试/profile、production code、schema/migration/domain/security/DataSource/HTTP server。
- 未 stage/commit/push、未创建/修改 PR/issue、未 merge、未切换分支、未进入其他 Gate。
- 预存 dirty paths 保持原状。
- `git diff --check` 的仓库级结果可能包含任务开始前既有的 whitespace/CRLF 问题；本报告自身通过 trailing-whitespace 检查（见 completion response 的 exact 命令结果）。

## 9. 诚实声明

本报告只声称以下**实际执行事实**：精确 Temurin `21.0.13+9` 已下载并按 Adoptium 官方 SHA-256 校验、可执行；Maven 3.9.9 zip 已按官方 SHA-512 校验并派生 SHA-256；临时 preflight 工程依赖解析/树/Enforcer 通过；Spring 官方 API 证明 Boot 3.5 OSS 支持已于 `2026-06-30` 结束。

本报告**不**声称 S1 实现完成、`mvn verify` 通过、boot jar 启动、module set/annotation/allowedDependencies 断言、4 个 mismatch probes、3 组负向 fixtures、dependency firewall、container smoke、CI、A1-A5 或任何生产验收通过。没有创建 A5 evidence/authority/acceptance record，`productionAccepted=false` 语义不变。
