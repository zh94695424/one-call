# PA0 S1 实现报告 — Java/Maven 运行时、14 Modulith 边界与依赖防火墙（无 Docker）

> Task label：`pa0-s1-implementation-boot41-ds-20261027-01`
>
> Gate：`implementation`（仅 approved slice S1；不进入 code review/S2）
>
> Work unit：`PA0-production-runtime-bootstrap`
>
> Slice：S1 — Java/Maven 运行时、14 个 Spring Modulith 2.1.2 包边界、依赖防火墙、bootable jar（无 Docker/CI）
>
> Branch / base：`codex/pa0-production-runtime-bootstrap`；HEAD `5178964c3b0103396bf8ca1c6ac1bf84eaabaf10`（accepted revised plan checkpoint `97b94dc` + controller bookkeeping `5178964`）
>
> Author：DeepSeek（`deepseek-flash[1m]`）
>
> Date：2026-10-27（Asia/Shanghai；UTC 检索/执行窗口 `2026-10-26T20:39Z`–`20:48Z`）
>
> Artifact：本文件（`docs/gateflow/pa0-production-runtime-bootstrap-s1-implementation.md`）

## 0. 结论

**Status: COMPLETE（S1 author implementation；全部 S1 §11.1 信号已在 `backend/` 实际执行，等待 controller code review；不是 accepted slice、不是 S2、不是生产验收）。**

- 单 Maven 项目 `backend/`、单可启动 Boot `4.1.2` 应用；精确 14 个 Modulith `2.1.2` 包边界。
- Testcontainers `2.0.5` / PostgreSQL `42.7.14` 仅 test-scope 声明；S1 未创建/运行 container IT 或 profile，未使用 Docker。
- 无 domain 行为、表、migration、API/web server、Security/Flyway/jOOQ/Hikari/DataSource、provider/cloud 集成或生产凭据。
- 无 commit/stage/push/PR/merge/branch switch；未进入 code review 或 S2。
- 本报告替换历史 Boot 3.5 BLOCKED 报告（该报告仅作历史证据，不再代表当前目标线）。

## 1. 工具链与权威 preflight 证据（实际执行）

### 1.1 精确 JDK（Temurin 21.0.13+9）

| 项 | 结果 |
| --- | --- |
| 权威来源 | `https://api.adoptium.net/v3/assets/feature_releases/21/ga?architecture=x64&heap_size=normal&image_type=jdk&jvm_impl=hotspot&os=linux&vendor=eclipse&page_size=5` → **HTTP 200** |
| 选中 release | `release_name=jdk-21.0.13+9`；`openjdk_version=21.0.13+9-LTS`；`semver=21.0.13+9.0.LTS` |
| package | `OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz`；`size=207858352`；Adoptium `checksum=d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c` |
| 本地校验 | 同一 tarball `sha256sum` = `d7f5d5cf…d66d1c`，逐字符一致；解包后 `release` 文件 `IMPLEMENTOR="Eclipse Adoptium"`、`IMPLEMENTOR_VERSION="Temurin-21.0.13+9"`、`JAVA_RUNTIME_VERSION="21.0.13+9-LTS"` |
| 执行证据 | `.java-version` 精确内容 `21.0.13+9`（grep exit 0）；`java -version` → `openjdk version "21.0.13" 2026-10-20 LTS` / `Temurin-21.0.13+9 (build 21.0.13+9-LTS)`，exit 0 |
| 说明 | 系统 PATH 默认 Java 为 `21.0.12.1+1`（Temurin），**仅诊断、未用于任何 S1 权威命令**；所有权威命令使用显式 `JAVA_HOME=/tmp/pa0-tools/jdk-21.0.13+9` |

### 1.2 Maven 3.9.9 与 wrapper

| 项 | 结果 |
| --- | --- |
| distribution URL | `https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip` |
| 官方 SHA-512 | 由 Maven Central `.sha512`（**HTTP 200**）取得 `8beac8d11ef208f1e2a8df0682b9448a9a363d2ad13ca74af43705549e72e74c9378823bf689287801cbbfc2f6ea9596201d19ccacfdfb682ee8a2ff4c4418ba`；本地 zip `sha512sum` 逐字符一致 |
| 派生 SHA-256 | `4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c`（本地 zip `sha256sum`，Maven Central 重新下载的 fresh zip 同值） |
| wrapper 生成 | 使用 `/tmp/pa0-tools/apache-maven-3.9.9/bin/mvn` + `org.apache.maven.plugins:maven-wrapper-plugin:3.3.2:wrapper -Dmaven=3.9.9 -Dtype=only-script`，exit 0；生成 `mvnw`/`mvnw.cmd`/`.mvn/wrapper/maven-wrapper.properties` |
| properties pin | `wrapperVersion=3.3.2`、`distributionType=only-script`、上述 `distributionUrl`、`distributionSha256Sum=4ec3f26f…c3a3c` |
| 首次运行校验 | `./mvnw -v` 首次下载 zip 后通过 SHA-256 校验（否则 wrapper fail closed）；输出 `Apache Maven 3.9.9 (8e8579a9…)` / `Java version: 21.0.13, vendor: Eclipse Adoptium` / `Maven home: /root/.m2/wrapper/dists/apache-maven-3.9.9/3477a4f1`，exit 0 |
| 环境说明 | 本会话 PATH 无 `unzip`；为让 wrapper 走 `.zip`（而非改取 `.tar.gz` 导致固定 zip SHA-256 无法匹配）使用 `/tmp/pa0-tools/bin/unzip -> /usr/bin/busybox` 临时 shim。该 shim 属执行环境，不是仓库/系统配置变更；下载后的 SHA-256 校验仍由 wrapper 原生执行并通过 |

### 1.3 Boot / Modulith 支持与治理状态（HTTP 200，2026-10-27 检索）

| URL | 关键响应字段 |
| --- | --- |
| `https://api.spring.io/projects/spring-boot/generations/4.1.x` | `support=default`；`ossSupportEndDate=2027-07-31`；`commercialSupportEndDate=2028-07-31`；`latestPatch.oss=4.1.2`；`linkedGenerations.spring-boot=["4.1.x"]` |
| `https://api.spring.io/projects/spring-boot/releases/4.1.2` | `version=4.1.2`；`status=GENERAL_AVAILABILITY`；`current=true` |
| `https://api.spring.io/projects/spring-modulith` | `status=ACTIVE_UNSUPPORTED` |
| `https://api.spring.io/projects/spring-modulith/generations/2.1.x` | `support=none`；`initialReleaseDate=2026-06-30`；`linkedGenerations.spring-boot=["4.1.x"]`；`latestPatch.oss=2.1.2` |
| `https://api.spring.io/projects/spring-modulith/releases/2.1.2` | `version=2.1.2`；`status=GENERAL_AVAILABILITY`；`current=true` |

- Boot `4.1.x` 在 2026-10-27 仍处于 OSS 支持窗口（至 `2027-07-31`），可接受；未使用 Boot `4.0.x`/`3.x`。
- Modulith `2.1.x` 为 `ACTIVE_UNSUPPORTED` / `support=none`，**没有独立 OSS/商业支持窗口**；不得把 Boot 的 `2027-07-31` 当成 Modulith 窗口。该事实按 `PA0-R-14` 由 controller/release governance 显式接受；本报告只记录 `2.1.2` resolved version 与 2.1.x patch 跟踪，不静默替换目标线。

### 1.4 实际解析版本（来自 `backend/pom.xml`，非 /tmp 证据）

`./mvnw -B -ntp -U dependency:resolve` exit 0；`dependency:tree -Dincludes=…` exit 0：

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

全 scope `dependency:list` 扫描无 `spring-boot*:3.x`、`spring-boot*:4.0.x`、`spring-modulith*:1.x`、`spring-modulith*:2.0.x`（grep exit 1）；无 `org.springframework.security`、`org.flywaydb`、`org.jooq`、`com.zaxxer`（grep exit 1）。Enforcer `3.6.3` 显式 pin，`validate` exit 0，Rule 0–4 全部 `passed`。

## 2. 变更文件（仅允许路径）

`backend/**` 62 个新文件 + 本报告。下表 SHA-256 / 行数为最终内容。

| SHA-256 | 行数 | 文件 |
| --- | --- | --- |
| `b53ee26e6ff1da219b0563333a76d2c9ec19eec713d6ee54e11a1edacb0e9f3d` | 1 | `backend/.gitignore` |
| `91db709fe944bbecdb5b070f8fd44334664ea7e08cb74222181d898b317657fa` | 1 | `backend/.java-version` |
| `fb35ac0fc2cfe48ba099746c52099523693a952e0d4ef0a346fa5627e077a764` | 20 | `backend/.mvn/wrapper/maven-wrapper.properties` |
| `eef973c2dc230ac79cfc62ca4998b571b8a133883fb9b25bdda4e93373126d73` | 79 | `backend/README.md` |
| `7e6e5d26712efd78140f2f63dafe8d17028f6c5c97ac1f746a043110b7a1d9ad` | 259 | `backend/mvnw` |
| `3dc8d558cc15e5051a9ef3778099c6a8bcd83320ba0383ece14811194d08d730` | 149 | `backend/mvnw.cmd` |
| `ddc382dad8dc46d61c6be9123666d7ddc36be8dd697043d40aa7656b7e94fe76` | 114 | `backend/pom.xml` |
| `9cf42dab5e5f2238518778067fa7706ad72d8d89365cd9f76b1d08c6c6924a8d` | 12 | `backend/src/main/java/com/onecall/core/OneCallApplication.java` |
| `03f3dde2b47d7781c5b12846cc311aa15a677b8c2db51450428c21807787e2ac` | 2 | `backend/src/main/java/com/onecall/core/activation_consent/package-info.java` |
| `297f41c742abbce48292d72d92195b34371b32e4ed74343a9c56a7da055f23e4` | 6 | `backend/src/main/java/com/onecall/core/activation_consent/ModuleMarker.java` |
| `074490377867b44852009ad0d51c9b63388534c5f86291b6b5d7f88a2d25b702` | 2 | `backend/src/main/java/com/onecall/core/ai_orchestration/package-info.java` |
| `92af7c66f48f5ea201674f0e09d2f9d9d4d40c2ab56eba4516361287406e4b84` | 6 | `backend/src/main/java/com/onecall/core/ai_orchestration/ModuleMarker.java` |
| `c0747a735d8d72e0cbbf2f5342e937da9c6895c2f4cc07dc859f5d98e15f754a` | 2 | `backend/src/main/java/com/onecall/core/catalog_order/package-info.java` |
| `7e060a820cb6298a23a3561b6c939eb6995a36d8677e6e5a2062793d77c02c94` | 6 | `backend/src/main/java/com/onecall/core/catalog_order/ModuleMarker.java` |
| `81e30894ebf0c0609572d4d5f71691119f5f09b7cb109d0d6e9138d30225cef5` | 2 | `backend/src/main/java/com/onecall/core/customer_iam/package-info.java` |
| `f18d49a01ed416e61eca2e13b8cec218ba65b910f877670804718adfe51414fa` | 6 | `backend/src/main/java/com/onecall/core/customer_iam/ModuleMarker.java` |
| `6e51dfa6c77dca326080b542c7503ac50c35492463dc343f5a91b73405d4440f` | 2 | `backend/src/main/java/com/onecall/core/entitlement_metering/package-info.java` |
| `c9f8f35d4d9e02adc0e12878c2e37883b2d017b6f815b06c37ea19dcffaaa780` | 6 | `backend/src/main/java/com/onecall/core/entitlement_metering/ModuleMarker.java` |
| `368d66f9566d07145ef0493f2942cc7b67c14bfcd199f47d2abb9b7ceca15aee` | 2 | `backend/src/main/java/com/onecall/core/evidence/package-info.java` |
| `1141690370e4463d1bdffc8ae9b8e0e49a32e3342694125f5a75a55f8d3c9b99` | 6 | `backend/src/main/java/com/onecall/core/evidence/ModuleMarker.java` |
| `246679d70a5c8b2a22f51835fa7aadb8b6fe30d26f16e48dd18c29cb29106094` | 3 | `backend/src/main/java/com/onecall/core/ivr/package-info.java` |
| `0398c11169d7ac59e2b7cdea134736250e5d0b58aedad80051817c69285da1f8` | 6 | `backend/src/main/java/com/onecall/core/ivr/ModuleMarker.java` |
| `47f1ab843a417d45eafe64bd0ed269b2d987d28e77a211b6801405042dcaaf17` | 2 | `backend/src/main/java/com/onecall/core/notary/package-info.java` |
| `f16b705fbae3589ff9615d2a04d05fd10b7324dbe125836ba04a157bb3f3221f` | 6 | `backend/src/main/java/com/onecall/core/notary/ModuleMarker.java` |
| `354a05fce8ad111134da6359f950350b1bfd249fdff170d0ed64e9675870326b` | 2 | `backend/src/main/java/com/onecall/core/operations_audit/package-info.java` |
| `23f430c40bcc3680651e587a93074775cba8954f548dccb883b7eff09146da11` | 6 | `backend/src/main/java/com/onecall/core/operations_audit/ModuleMarker.java` |
| `09574d6dd730fc57426f0828ed9ce3827bae5ff3de11b0d24df5e02ef6758eac` | 2 | `backend/src/main/java/com/onecall/core/operations_iam/package-info.java` |
| `e62e33c3d4bf04c834890724210e431c82cb175a0bb793745026e238c8539509` | 6 | `backend/src/main/java/com/onecall/core/operations_iam/ModuleMarker.java` |
| `ea2202b9105fdbbc473f061cf0d8e9aad44cb01fbf10569983dc2406da835db4` | 2 | `backend/src/main/java/com/onecall/core/organization_access/package-info.java` |
| `a36cda0bf966df7afce6fed0c53de985e03c61115d199d18a60ae952ba8406ab` | 6 | `backend/src/main/java/com/onecall/core/organization_access/ModuleMarker.java` |
| `b0ce6ae5c04158a94ca755d47bb3b62f5f51f496aac7e26d6be5724bd6f42738` | 2 | `backend/src/main/java/com/onecall/core/platform_reliability/package-info.java` |
| `307b65386aab09d0683f10af366252ae3e9a93e5d9e1952477f4a43b38f077c0` | 6 | `backend/src/main/java/com/onecall/core/platform_reliability/ModuleMarker.java` |
| `c8d75cc0507233bef7e4d4f685350233ada280fc28c65d23341d59a147386c3d` | 2 | `backend/src/main/java/com/onecall/core/sharing/package-info.java` |
| `589b7a4083979a7164a66a99164a494843b89ae803b705034fb08857d205cca6` | 6 | `backend/src/main/java/com/onecall/core/sharing/ModuleMarker.java` |
| `5aa47ca75e732b9c8e491c4cc909424ec485cdf7e3f37f39dd76d9fbea9bdfc8` | 2 | `backend/src/main/java/com/onecall/core/subject_number/package-info.java` |
| `761a9b920cd979ad6496c4730541ba6594a57c58a105309204551b7b0fe5d6d0` | 6 | `backend/src/main/java/com/onecall/core/subject_number/ModuleMarker.java` |
| `0c048bbd592c904af067ea42c8ddd736b38543e3a7b2b3316eaff4a145bd1d15` | 10 | `backend/src/main/resources/application.yml` |
| `a9fa344defa94a15fdeab972ed68c36d76cda9e9fadb65ca0af5a8c2f99e2607` | 9 | `backend/src/test/java/com/onecall/archfixtures/cycle/CycleFixtureRoot.java` |
| `331586b4a8d9096eff955966046a15b68f65361b10e1de44dd4b4c7bc769145c` | 10 | `backend/src/test/java/com/onecall/archfixtures/cycle/alpha/Alpha.java` |
| `3940b4a25cb331d82b5743da2a5ca547352d29cc2430dca5e4004fb57b5e5cfd` | 6 | `backend/src/test/java/com/onecall/archfixtures/cycle/alpha/ModuleMarker.java` |
| `3ae2cbc3ee971542afcef9031404e3f92d6eec468cbf4d3ca877fb2e23d45450` | 2 | `backend/src/test/java/com/onecall/archfixtures/cycle/alpha/package-info.java` |
| `a31bebc555477670efd16770a4ec78e9813bad32994a9cf0f5377048f878a84b` | 10 | `backend/src/test/java/com/onecall/archfixtures/cycle/beta/Beta.java` |
| `9a2b85df6ed480f01f4d4d042d41644431748c408884704dee74208adf33ae6a` | 6 | `backend/src/test/java/com/onecall/archfixtures/cycle/beta/ModuleMarker.java` |
| `79260f0bcee4cbbb2d58bf1bcc8a69c6611b344a20750cc29cb7b63328e65495` | 2 | `backend/src/test/java/com/onecall/archfixtures/cycle/beta/package-info.java` |
| `9cc44b4cb102a09e99faab95a3d26e88807635375bd4b95c0236267d8fc39f84` | 9 | `backend/src/test/java/com/onecall/archfixtures/forbidden/ForbiddenFixtureRoot.java` |
| `9fd104e031e0adba32e26df76488b43a614b6df125fe521a4d346bf7032c1963` | 10 | `backend/src/test/java/com/onecall/archfixtures/forbidden/consumer/Consumer.java` |
| `2ca52a0a3e75968d901c08087963e600fe8a27804a3e06abb7162d36dd6ffba8` | 6 | `backend/src/test/java/com/onecall/archfixtures/forbidden/consumer/ModuleMarker.java` |
| `f02732dab5a75ce71119b69d2829329756de74098c9ae70f2e3d08801ee7e995` | 2 | `backend/src/test/java/com/onecall/archfixtures/forbidden/consumer/package-info.java` |
| `5492190f39ea387b7f800571298d73f51c422b81e6a80b676e77e97901675afc` | 8 | `backend/src/test/java/com/onecall/archfixtures/forbidden/provider/Provider.java` |
| `3e9e190d021fce8de0bc9386f3be221a26f09734322c1cec3c339263fd317283` | 6 | `backend/src/test/java/com/onecall/archfixtures/forbidden/provider/ModuleMarker.java` |
| `61eb1e65454ffb51ce91962beb9f50912d69a6b149f5d14cf5dbf18f3333ac34` | 2 | `backend/src/test/java/com/onecall/archfixtures/forbidden/provider/package-info.java` |
| `e48f89147bcc6e7f7165dff8a92450fddf1fd6524cc44e32d804130985c1feb3` | 9 | `backend/src/test/java/com/onecall/archfixtures/internal/InternalFixtureRoot.java` |
| `bad3be85c41a99acbd4b449e666f76b1527dcb5fa973d6009e29ecb6e6716444` | 10 | `backend/src/test/java/com/onecall/archfixtures/internal/consumer/Consumer.java` |
| `5dd9fafb5395b001b28edee0253fbd6f3f567f8fbf8f8baedcf56acf8d4100d9` | 6 | `backend/src/test/java/com/onecall/archfixtures/internal/consumer/ModuleMarker.java` |
| `2d0a49f84661e00768d5a24a96b33e01dc977eff8380704ffcf8e1a9654604b8` | 2 | `backend/src/test/java/com/onecall/archfixtures/internal/consumer/package-info.java` |
| `17ea0695f8069e60d916692d09cb79221aff64dcd41d6f4cce722dcc59ca2c55` | 8 | `backend/src/test/java/com/onecall/archfixtures/internal/provider/Provider.java` |
| `4ca9ab4a799f02a924309fe42ce8738ba2daaac4fe9218b2ac43a61dab4cee8c` | 6 | `backend/src/test/java/com/onecall/archfixtures/internal/provider/ModuleMarker.java` |
| `a15b840b2459b65f08ae89ffed3bc332ef238b36a1d3258c89b44389e48f2607` | 8 | `backend/src/test/java/com/onecall/archfixtures/internal/provider/internal/Internal.java` |
| `7a4549cf05549f51a6c6b4494bd505c3587350f2e03591da47c1225f214c6fe5` | 2 | `backend/src/test/java/com/onecall/archfixtures/internal/provider/package-info.java` |
| `72fd64995c83bff0066aa27febd1d29863396169b2e99ec988a49ca78dbe8fdc` | 24 | `backend/src/test/java/com/onecall/archtests/OneCallApplicationTests.java` |
| `86c3b17e349a7ef4d6e6a1b159fb2d026547dee6c990d834315056f5b1cb4500` | 25 | `backend/src/test/java/com/onecall/archtests/architecture/BootstrapDependencyBoundaryTests.java` |
| `723542b1290be8bab9a6a7165138e415f5cf8bd970bf7651548f4b9f4cb6a87a` | 143 | `backend/src/test/java/com/onecall/archtests/architecture/ModuleBoundaryTests.java` |
| self | — | `docs/gateflow/pa0-production-runtime-bootstrap-s1-implementation.md`（本报告；自身 SHA-256 在 completion response 记录） |

`git status`：`backend/` 为唯一新增目录（`?? backend/`），本报告为唯一修改的 docs 文件；无 staged changes（`git diff --cached --stat` 为空），HEAD 未移动。

## 3. 模块边界实现与断言

14 个模块均为 `com.onecall.core` 直接子包；每个包 1 个 `package-info.java` + 1 个 package-private `ModuleMarker`；inventory 精确 `package-info=14`、`ModuleMarker=14`，`src/main/java` 无其他 production type。

`ModuleBoundaryTests`（8 tests）实际覆盖：

1. `discoversExactlyTheFrozenModuleSet`：`ApplicationModules.of(OneCallApplication.class)` 的模块集精确等于 14 个名称（`getIdentifier().toString()`），无第 15 个/缺失模块。
2. `everyExpectedModuleDeclaresApplicationModule`：通过 `Class.forName("com.onecall.core." + name + ".ModuleMarker")` 反射读取每个包，`@ApplicationModule` 存在 14/14。
3. `assertsExactAllowedDependencyMapFromPlan62`：从每个 `package-info` 反射读取 `allowedDependencies()`，与 plan §6.2 单一 expected map 做 exact set equality；leaf 显式 `{}`；显式断言 `doesNotContain(OPEN_TOKEN)`（`\u00af\\_(\u30c4)_/\u00af`）。
4. `discoveredProductionEdgesStayInsideTheConfiguredMap`：使用 Modulith 2.1.2 API `module.getDirectDependencies(modules).uniqueModules()`；discovered edges ⊆ configured map，且当前全局 edges 为空。
5. `verifiesNoCyclesOrBoundaryViolations`：`ApplicationModules.of(OneCallApplication.class).verify()` 通过。
6-8. 负向 fixtures（见下）。

EXECUTED ASSERTIONS：module_set_exact=true，annotation_present=14/14，allowed_map_exact=true，discovered_edges_empty=true，verify_pass=true。

## 4. 负向 fixtures（实际抛 `Violations`）

| Fixture | 结构 | 结果 |
| --- | --- | --- |
| `com.onecall.archfixtures.cycle` | `alpha`/`beta` 互相允许并互相引用（`Alpha→Beta`、`Beta→Alpha`） | `rejectsCycleFixture` 通过；`ApplicationModules.of(..., INCLUDE_TEST_FIXTURES).verify()` 抛 `Violations` |
| `com.onecall.archfixtures.forbidden` | `consumer` 显式 `allowedDependencies = {}`（deny-all）；`Consumer` 引用 `provider.Provider` 公开根类型 | `rejectsForbiddenDependencyFixture` 通过；抛 `Violations`，证明显式 `{}` deny-all 生效 |
| `com.onecall.archfixtures.internal` | `consumer` 允许 `provider`；`Consumer` 引用 `provider.internal.Internal` | `rejectsInternalPackageAccessFixture` 通过；抛 `Violations`，证明 internal/CLOSED 边界生效 |

**实现层 Modulith 2.1.2 适配（不削弱断言，必须记录）**：

- 2.1.2 的 `ModulithMetadata.of(Class)` 仅接受带 `@Modulith`/`@Modulithic`/`@SpringBootApplication` 的 root type；三个 fixture root 因此标注 test-only `@org.springframework.modulith.Modulithic`（否则 `IllegalArgumentException`，无法进入验证）。这不是生产代码，也不改变 14 个模块。
- 2.1.2 的 `ApplicationModules.of(Class)` 固定使用 ArchUnit `DoNotIncludeTests`；fixtures 位于 `src/test/java`，会被整体排除。负向测试使用同版本公开 overload `ApplicationModules.of(root, ImportOption)` 并传入 include-all `ImportOption`，使 test-only fixture 可见。断言仍要求真实抛 `Violations`，未删除/放宽任何负向检查。生产边界测试仍使用默认 `ApplicationModules.of(OneCallApplication.class)`。

## 5. 4 个稳定 mismatch probes（每项必须失败，恢复必须绿）

命令：`./mvnw -B -ntp -Dtest=ModuleBoundaryTests test`。

| Probe | 变更 | 结果 | 失败断言 |
| --- | --- | --- | --- |
| (a) widen ivr | `ivr` + `catalog_order` | exit 1 | `[ivr allowedDependencies must equal plan §6.2]` @ `assertsExactAllowedDependencyMapFromPlan62:93` |
| (b) narrow ivr | `ivr` - `subject_number` | exit 1 | `[ivr allowedDependencies must equal plan §6.2]` @ line 93 |
| (c) leaf bare | `customer_iam` 改为 bare `@ApplicationModule`（默认 `OPEN_TOKEN`） | exit 1 | `[customer_iam must not use default OPEN_TOKEN]` @ line 90；实际值 `["¯\_(ツ)_/¯"]` |
| (d) change | `organization_access` `customer_iam` → `ivr` | exit 1 | `[organization_access allowedDependencies must equal plan §6.2]` @ line 93 |

每个 probe 后恢复原 package-info；最终恢复运行 `ModuleBoundaryTests` exit 0（`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`）。未只记录 report hash，四组失败/恢复均在本次会话实际执行。

## 6. 依赖防火墙与 jar 结果

- `BootstrapDependencyBoundaryTests`：`ClassUtils.isPresent` 断言 `org.flywaydb.core.Flyway`、`org.springframework.security.config.annotation.web.builders.HttpSecurity`、`org.jooq.DSLContext`、`com.zaxxer.hikari.HikariDataSource` 均不在 classpath；1 test 通过。
- `OneCallApplicationTests`：`@SpringBootTest(classes = OneCallApplication.class, webEnvironment = NONE)`，context 启动成功；`context.containsBean("dataSource")`、`"flyway"`、`"springSecurityFilterChain"` 均为 false；1 test 通过。
- Enforcer（`validate`/`verify`）：Rule 0 RequireJavaVersion `[21,22)`、Rule 1 RequireMavenVersion `[3.9.9,4.0)`、Rule 2 BanDuplicatePomDependencyVersions、Rule 3 DependencyConvergence、Rule 4 BannedDependencies（`searchTransitive=true`；ban `spring-boot-starter-security`/`data-jpa`/`jdbc`、`org.springframework.security:*`、`org.flywaydb:*`、`org.jooq:*`、`com.zaxxer:HikariCP`）全部 `passed`。
- `jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar` exit 0；`BOOT-INF/lib/` 下无 `spring-security|flyway|jooq|HikariCP|postgresql|testcontainers`（匹配数 0）。fat jar 27 个 runtime libs，均为 Boot/Spring/Modulith/logging/micrometer/ArchUnit（Modulith core 的 compile 依赖）等允许项。

## 7. Boot smoke / jar

- `timeout 60s java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1` → **exit 0**（60s 内自然退出）。
- `target/boot.log` 含 `Started OneCallApplication in 1.213 seconds`；Java 版本 `21.0.13`。
- **实现层适配（必须记录）**：`spring-modulith-starter-core` 传递 `spring-modulith-moments`，其 `MomentsAutoConfiguration` 带 `@EnableScheduling`，会创建非 daemon 调度线程，使无 domain event 的 bootstrap JVM 启动后无法退出（thread dump 显示非 daemon `pool-2-thread-1` 停在 `ScheduledThreadPoolExecutor$DelayedWorkQueue.take`）。为满足本 slice 的 exit-0 成功信号，`application.yml` 通过标准 `spring.autoconfigure.exclude` 排除未使用的 `org.springframework.modulith.moments.autoconfigure.MomentsAutoConfiguration`；依赖保留、未删除、未改目标线，也未加入任何 domain 行为。后续引入时间事件时必须显式重新启用并评审调度线程生命周期。

## 8. 命令与结果（全部使用精确 JDK）

| 命令 | Exit | 关键结果 |
| --- | --- | --- |
| `grep -Fx '21.0.13+9' .java-version` | 0 | 精确匹配 |
| `java -version` | 0 | `21.0.13` / `Temurin-21.0.13+9 (build 21.0.13+9-LTS)` |
| `./mvnw -B -ntp -v` | 0 | Maven 3.9.9；Java 21.0.13 Eclipse Adoptium |
| `./mvnw -B -ntp -U dependency:resolve` | 0 | BUILD SUCCESS |
| `./mvnw -B -ntp dependency:tree -Dincludes=…` | 0 | 4.1.2 / 2.1.2 / 2.0.5 / 42.7.14，见 §1.4 |
| `./mvnw -B -ntp validate` | 0 | Enforcer Rule 0–4 passed |
| `./mvnw -B -ntp clean verify` | 0 | 10 tests，0 failures/errors/skipped；jar repackage success |
| `java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar` | 0 | `Started OneCallApplication` |
| `jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar` | 0 | forbidden libs 0 |
| mismatch probes (a)–(d) | 1/1/1/1 | 各自预期断言失败；恢复后 exit 0 |
| `git diff --check`（本报告） | 0 | 无 whitespace error |
| trailing whitespace/CRLF 扫描（backend，排除 target） | 0 | 无 trailing whitespace；仅官方生成的 `mvnw.cmd` 为 CRLF、`mvnw` 内含官方生成脚本 tab |

## 9. 未运行 / 证据边界

- **Docker / container smoke：NOT RUN**。S1 未创建 `PostgresContainerIT`、`container-smoke` profile 或 failsafe 绑定；默认 `verify` 不要求 Docker。Testcontainers/PostgreSQL 仅 test-scope 声明以固定版本。不得把本报告读成容器 smoke 通过。
- **CI：NOT RUN**。S1 未创建 `.github/workflows/ci.yml`，未 push/PR，未执行 GitHub Actions。
- **A1-A5 regressions：NOT RUN**。S1 scope 不含 contract regressions/CI；未运行、未修改任何 A1-A5 asset/validator/fixture。
- 以上均为 S2/post-push 职责，本报告不声称任何 A5 evidence、`PASS` 或 `productionAccepted=true`。

## 10. Residual 对账（PA0-R-01..14，逐项 owner → destination）

| ID | 状态 / owner → destination |
| --- | --- |
| `PA0-R-01` | 已由本 S1 的 `backend/**` resolve/tree/test-compile/verify 关闭（exit 0）；Platform/CI → controller review |
| `PA0-R-02` | 已关闭：wrapper 3.3.2 生成，URL/SHA-256 固定并实际校验；Platform/CI → controller review |
| `PA0-R-03` | 仍开放：author 未运行 container smoke；Platform/CI → **controller pre-push Docker-capable `-Pcontainer-smoke verify` 是 S2 accepted checkpoint 唯一前置；daemon 不可用则停止请求启动 Docker** |
| `PA0-R-04` | 未运行：GitHub Actions 首次真实运行；Platform/CI → PA0 PR/controller gate，未运行前不得声称绿 |
| `PA0-R-05` | 未关闭：`postgres:16` digest 未 pin；Backend/Platform → S2/CI hardening |
| `PA0-R-06` | 已复核：Boot 4.1.x OSS 至 `2027-07-31`；Platform/Security → 后续 patch 跟踪 |
| `PA0-R-07` | 未关闭：Actions major tag 非 commit SHA；Security/Platform → CI hardening/release governance |
| `PA0-R-08` | 未关闭：path-filter 与 branch protection 交互；Platform/CI → repository governance |
| `PA0-R-09` | 未关闭：跨模块 platform/audit/sharing/evidence 边未预授权；Backend/Architecture → first domain work unit + architecture review |
| `PA0-R-10` | 已关闭对本 slice：精确 Temurin `21.0.13+9` 已选择/执行并断言；Platform/CI → 后续执行环境继续保持 pin |
| `PA0-R-11` | 持续披露：CI 绿 ≠ 生产验收；Backend/QA/Product → README/§9.5 + A5/release governance |
| `PA0-R-12` | 未关闭：无 formatter/checkstyle；Backend/Platform → 后续 code-quality work unit |
| `PA0-R-13` | 未关闭：allowedDependencies 为 prose 翻译上限；Architecture → architecture change control |
| `PA0-R-14` | **仍为 accepted governance residual**：Modulith `2.1.x` `ACTIVE_UNSUPPORTED` / `support=none`，无 published OSS/商业支持窗口；owner **Platform/Security + release governance** → controller 在 revised plan review 显式接受；S1/S2 记录 resolved `2.1.2` 并跟踪 2.1.x patch，**不得静默替换** |

新增 S1 实现说明（非 controller residual ledger 变更，仅披露）：`MomentsAutoConfiguration` 排除与 test-only `@Modulithic`/fixture `ImportOption` 适配。Owner Backend/Architecture；destination：首次引入 domain 时间事件或后续 S2/CI slice 时复核。

## 11. 文档决定与范围证明

- 新增 `backend/README.md`：工具链 pin、wrapper、S1 命令、14 模块表、moments 排除说明、Docker/CI/A1-A5 `NOT RUN` 与非验收声明。
- 未新增 `docs/20`、root README、第二 spec、index 行或 acceptance 文档；未修改 controller、plan/revision/fix/review artifact、prototypes/QA/screenshots、A0-A5 assets/validators/fixtures、`.github/**`、`.codex-tasks/**`。
- 变更仅限：`backend/**`（新增）与本报告（替换历史 BLOCKED 内容）。预存 dirty paths（screenshots/docs/prototypes、`.codex-tasks/`、`docs/reviews/plan-review-20261021-205118.md`）未被本任务修改。
- 未 stage、commit、push、创建/修改 PR/issue、merge、切换分支或进入 code review/S2。

## 12. 完成状态

- **S1 author implementation：COMPLETE**。S1 全部要求信号（toolchain pin、精确依赖解析、Enforcer、14 模块 exact set/annotation/map、4 mismatch probes、3 负向 fixtures、dependency/classpath/jar firewall、boot jar exit 0）均在 `backend/` 实际执行并通过。
- 不是 accepted slice；下一步由 controller 进行 code review。S2（PostgreSQL Testcontainers smoke、CI、A1-A5）未开始、未声称通过。
