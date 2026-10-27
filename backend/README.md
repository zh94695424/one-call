# onecall-runtime（PA0 / PA1 S1–S3 / PA2 V002 S1+S2）

Phase A 生产运行时引导脚手架，外加 PA2 V002 identity/organization/employee foundation 的 S1 证据。本工程包含运行时入口、14 个 Spring Modulith 包边界、架构/依赖防火墙测试，以及 V001+V002 的容器行为证据：S1 的 V001+V002 PostgreSQL 16 migration、精确 inventory 和 fail-visible delivery，S2 的 idempotency/Outbox first-result 与事务原子性，以及 S3 的 audit append-only、reason/denylist、固定 search_path、owner/PUBLIC/角色权限边界和 hostile search_path 防护；PA2 V002 S2 追加四个 DB 行为方法，证明六表默认值/条件 revision/34 个命名 check/FK RESTRICT、本地不可变 guard 与 auth rotation、identity/profile/employee/membership 的唯一性与 revoke-regrant，以及并发 identity/live-membership 唯一冲突。V001 只创建六个 schema（其中四个为空）、15 个 NOLOGIN 角色、`platform`/`audit` 三张表、append-only audit guard 和四个 `SECURITY DEFINER` 函数；V002 在既有角色/scope 上再创建 `customer_iam`/`organization_access`/`operations_iam` 六张表、34 个命名 check、四个 RESTRICT FK、六个本地 guard 触发器与精确 owned/cross-module grants；**不包含 V003+ 对象、运行时 DataSource/Flyway、API、认证、供应商或云集成**。S3 只证明既有 V001/V002 行为，不新增 migration 或生产对象。

## 工具链 pin

| 组件 | Pin | 说明 |
| --- | --- | --- |
| Java | Temurin `21.0.13+9` | `backend/.java-version`；`maven.compiler.release=21`；Enforcer `[21,22)` |
| Maven | `3.9.9` | Maven Wrapper 3.3.2（`only-script`）；distribution SHA-256 `4ec3f26fb1a692473aea0235c300bd20f0f9fe741947c82c1234cefd76ac3a3c` |
| Spring Boot | `4.1.2` | parent/BOM；Boot 4.0.x 与 3.x 均不可用 |
| Spring Modulith | `2.1.2` | `spring-modulith-bom` + `spring-modulith-starter-core` |
| Testcontainers | `2.0.5` | 仅 test scope（core + manual lifecycle；`container-smoke` profile） |
| PostgreSQL JDBC | `42.7.14` | 由 Boot `4.1.2` BOM 管理；仅 test scope |
| Flyway core / database-postgresql | 已由 Boot `4.1.2` BOM 管理并解析为 `12.4.0`（R-02 已关闭） | 仅 test scope；Surefire classpath 排除；Enforcer 禁止 compile/runtime；不进入 production fat jar |

执行 PA1 命令前必须显式选择精确 JDK（示例）：

```bash
export JAVA_HOME=/path/to/jdk-21.0.13+9
export PATH="$JAVA_HOME/bin:$PATH"
```

`./mvnw` 首次运行需要从 Maven Central 下载 Maven 3.9.9，并在下载后校验固定的 SHA-256。运行 wrapper 的环境还需具备 `unzip`（或等价的 zip 解压能力）；缺少 `unzip` 时 wrapper 会改走 `.tar.gz` 而固定的 zip SHA-256 无法匹配，因此会 fail closed。

## PA1 默认命令（S1+S2+S3 证据构建）

```bash
cd backend
grep -Fx '21.0.13+9' .java-version
java -version 2>&1 | grep -F '21.0.13+9'
./mvnw -B -ntp -v
./mvnw -B -ntp -U dependency:resolve
./mvnw -B -ntp dependency:tree \
  -Dincludes=org.springframework.boot:*,org.springframework.modulith:*,org.testcontainers:*,org.postgresql:*,org.flywaydb:*
./mvnw -B -ntp validate              # Enforcer: toolchain / convergence / banned deps
./mvnw -B -ntp verify                # compile + surefire + jar + Enforcer
java -jar target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/boot.log 2>&1
grep -F 'Started OneCallApplication' target/boot.log
jar tf target/onecall-runtime-0.0.1-SNAPSHOT.jar > target/jar-index.txt
```

预期：

- `verify` 运行 10 个测试（ModuleBoundaryTests 8、OneCallApplicationTests 1、BootstrapDependencyBoundaryTests 1），无 skip；默认 `verify` **不运行** container IT，也不需要 Docker。
- `BootstrapDependencyBoundaryTests` 保持不变：Flyway 仅以 test scope 存在，并由 Surefire `classpathDependencyExcludes` 从 unit/bootstrap classpath 排除；Failsafe container IT 仍可见 Flyway。
- 应用启动后输出 `Started OneCallApplication` 并以 `0` 退出（无 web server、无 DB、无调度线程）。
- production fat jar 不包含 `spring-security`、`flyway`、`jooq`、`HikariCP`、`postgresql`、`testcontainers` 库。

## PA1 S1+S2+S3 命令：PostgreSQL container smoke

```bash
cd backend
./mvnw -B -ntp -Pcontainer-smoke verify
```

- 只在显式 `container-smoke` profile 下运行 `PostgresContainerIT`：启动真实 `postgres:16` 容器（`onecall`/`onecall`，密码为每次 JVM 运行生成的 UUID，不落盘、不写日志），先断言 clean database，再执行 Flyway `classpath:db/migration`。
- S1+S2+S3 恰好包含 16 个容器测试方法（PA2 V002 S2 新增四个）。三个 S1 方法：`v001ThenV002MigrateCleanPostgres16WithoutSkipping`（依次应用 V001+V002 共两个 migration、`validate()` 通过、重复 migrate 为零、以已授权角色成功调用 V001 函数并对六张 V002 表做 runtime-role insert 后回滚）、`v001AndV002InventoryMatchesTheFrozenContract`（六个 schema/owner、15 个角色属性、九张表列/命名 check/index、十一个函数/七个 trigger、精确 schema/table/column/function grant、34 个 check 谓词的 normalized whole `pg_get_constraintdef` inventory、无 V003+ 对象）和 `brokenMigrationFailsVisibly`（隔离 history schema/table + 非冲突 version，断言 sentinel 来自故意失败语句且前置 valid DDL 的 marker table 回滚后不存在）。
- 四个 S2 方法：`idempotencyNewReplayConflictAndFinalizeOnce`（`NEW` revision 1 空结果、complete 一次性成功、同 key/同 fingerprint `REPLAY` 返回首次结果、同 key/改 fingerprint `CONFLICT` 无副作用、同 hash 重复为 `ALREADY_COMPLETED`、改 hash/过期 revision 为 `IDEMPOTENCY_CONFLICT`，存储行不变且方法局部仅一行）、`committedNewIdempotencyRowIsAlertableAndRolledBackWritesLeaveNoRow`（committed `NEW` 使下次 begin 抛 `IDEMPOTENCY_INCOMPLETE`，`ix_idempotency_new_alert` 可查到该行，begin-only 与 begin+complete 回滚均不留下行）、`concurrentIdempotencyRetryObservesOneWinner`（两个连接经 latch/阻塞探测竞争同一 key：winner commit 时 loser 为 `REPLAY`，winner rollback 时 loser 为 `NEW`；最终仅一行且仅一个 `NEW` winner）和 `outboxReplayConflictRollbackAndDenylist`（同 domain/dedupe key 同 hash 返回原 id 且一行、改 hash 抛 `OUTBOX_DEDUPE_CONFLICT` 且存储行不变、调用方回滚不留行、缺失 payload/坏 envelope/受保护明文均抛 `OUTBOX_PAYLOAD_INVALID` 且不留行）。
- 五个 S3 方法：`auditIsAppendOnlyAndRuntimeWritesAreDenied`（授权 runtime role 经 `audit.append_fact` 追加、调用方回滚删除该行、domain role 直接 insert 与 `audit_reader_role` update/delete/truncate 均以 `42501` 拒绝、`audit_owner` update/delete 触发 `AUDIT_APPEND_ONLY`/`P0001`）、`auditReasonAndDenylistRulesAreEnforced`（EMPLOYEE actor 与高风险 SERVICE action 缺 reason 均抛 `AUDIT_REASON_REQUIRED`、带 reason 成功、受保护 key 抛 `AUDIT_ARGUMENT_INVALID`、masked variant 成功）、`functionsHaveFixedSearchPathAndNoTransactionControl`（四个声明函数为 `SECURITY DEFINER`、六个 V002 guard 为 `SECURITY INVOKER`、owner 与有序 `<owner schema>, pg_temp` search_path 精确、无事务控制语句、`pg_catalog.gen_random_uuid()` 仅出现在三个插入函数且 guard 不生成 ID、`issue_first_trial` 不存在）、`ownerPublicAndRoleExecutionBoundariesAreExact`（owner 角色 NOLOGIN/非超级/不可建库建角色、`PUBLIC` 无 schema/table/function 权限、声明角色精确 EXECUTE/USAGE 无 CREATE、dispatcher 仅可 UPDATE 五个列且 DELETE/TRUNCATE 被拒）、`hostileSearchPathCannotRedirectFunctionEffects`（唯一 decoy schema/table/function + hostile caller search_path 不能重定向 `platform.idempotency_begin`，真实行写入、decoy 不变，finally 清理）。
- PA2 V002 S2 的四个行为方法：`mutableHeadsEnforceLifecycleRevisionAndNamedChecks`（六表默认值与有效条件 revision 更新、stale revision 零行、34 个命名 check 的精确 `23514`+约束名违反探针、FK 缺失父/父删除 RESTRICT `23503`、无 id default/sequence）、`localImmutabilityGuardsRejectRelationAndRotationChanges`（owner 对全部不可变列与 auth rotation 的稳定 guard 错误、runtime 列权限 `42501`、允许的可变列更新成功）、`identityCollisionAndMembershipRevokeRegrantSemantics`（同 account 幂等/不同 account `IDENTITY_CONFLICT`、profile/employee 唯一、membership live 唯一/revoke/new-row regrant/revoked revival 冲突/多个 revoked/不可变 pair）和 `concurrentIdentityAndMembershipLiveCollisionsHaveOneWinner`（两个连接竞争 identity tuple 与 live membership pair：commit race 一个 winner、loser `23505` 后读取分类；rollback race loser 成功插入；每个场景恰好一个 committed winner，方法局部 cleanup）。
- S2/S3 断言全部按方法局部 key/UUID 范围计数；共享容器中其他方法或历史行不影响断言。并发用例使用两个连接、`pg_stat_activity` 阻塞探测、`statement_timeout` 与 future 超时，不依赖仅靠 sleep 的时序断言。S3 的临时 decoy schema、outbox 行与 hostile-test idempotency 行均在 `finally` 中有界清理；audit append-only 表从不 truncate。
- **fail-closed**：Docker daemon 不可用、镜像拉取失败、容器启动失败、JDBC 连接失败、Flyway 失败或任一断言失败都使 Maven build 非 0。没有 `@Disabled`、`Assumptions`、`disabledWithoutDocker` 或环境变量 skip。
- `maven-failsafe-plugin` 只在 `container-smoke` profile 内绑定，`failIfNoTests=true`；若 profile 激活但测试未被匹配，build 失败。
- 默认 `./mvnw -B -ntp verify` 成功**不等于** container smoke 通过；只有 `-Pcontainer-smoke verify` 的 failsafe 结果（S1+S2+S3 恰好 16 个 test、`skipped=0 failures=0 errors=0`）才算容器证据。
- V001 的创建顺序、checksum 和 inventory 是 S1 证据；S2 证据覆盖 idempotency/Outbox first-result 与事务原子性，S3 证据覆盖 audit/security 行为矩阵。S3 仅为既有 V001 的行为证明，不新增 migration 或生产对象；author 证据不等于 accepted checkpoint 或生产验收。
- `postgres:16` 是 test-only fixture（design family `16+`），不是 production DB 版本承诺；digest 尚未 pin。Testcontainers superuser 仅用于 `CREATE ROLE`/`SET LOCAL ROLE` 测试，不是 Q17/Q23 production provisioning。

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
- `java-runtime`：Temurin `21.0.13+9`（`actions/setup-java` 精确 pin，不用 `check-latest`）、`./mvnw` 断言 Maven `3.9.9`、先 `docker info`、再 `./mvnw -B -ntp -Pcontainer-smoke verify`，随后解析 failsafe/surefire XML（S1+S2+S3 恰好 16 个具名 container test、`skipped=0`），并执行 boot jar smoke 与 production jar firewall。
- `contract-regressions`：Node `22.23.4` 精确 pin、`npm ci` + `npm run check:s3`，以及 A2/A3/A4/A5 既有 validator 命令（不修改其 fixtures/语义）。
- `docker info` 在任何 Maven/容器步骤之前；Docker 不可用时 job 立即失败，不会出现“测试被 skip 但 job 绿”。

## 证据边界（重要）

- 本工程只提供 **build/static/synthetic** 证据：Maven 构建、Enforcer 规则、Modulith 边界测试、classpath/jar firewall、boot smoke、真实 PostgreSQL container smoke，以及 A1-A5 静态/合成 contract regressions。
- 默认 `./mvnw verify` 不需要 Docker，且**不等于**容器 smoke 通过；author 环境的 container smoke 可以是 `NOT RUN`，但 PA1 accepted checkpoint 前必须由 controller 在 Docker-capable 环境实际执行 `-Pcontainer-smoke verify` 并通过。
- 本地/CI 成功都**不是** A5 `PASS`、`productionAccepted=true` 或生产验收；不证明 carrier/IVR media/腾讯云/provider/load/restore/penetration/manual/production acceptance，不创建 A5 evidence/authority/acceptance record。GitHub Actions 结果只有在真实运行时才算证据，未运行前不得声称 CI 绿。
