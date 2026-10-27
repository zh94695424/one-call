# PA0 S2 fix 1 — setup-java Temurin 版本标识符修正

- Task label: `pa0-s2-fix1-boot41-ds-20261027-01`
- Work unit: `PA0-production-runtime-bootstrap`
- Gate: `implementation fix`（仅修复 S2 CI 中 `actions/setup-java@v4` 的版本标识符命名不匹配；不进入 re-review、S3 或任何新 gate）
- Slice: S2（`.github/workflows/ci.yml` 的 java-runtime job）
- Base: accepted-content HEAD `7a04f4f`；Draft PR #8 保持打开
- Date: 2026-10-27（Asia/Shanghai）
- Artifact: 本文件（`docs/gateflow/pa0-production-runtime-bootstrap-s2-fix-1.md`）

## 0. 结论

**COMPLETE — 本 bounded fix 的 `author_code_complete=true`；不是 CI rerun 绿、不是 S2 accepted、不是生产验收。**

- 只改 `.github/workflows/ci.yml` 的 setup-java 输入值与 toolchain 断言；语义 pin 仍精确为 `21.0.13+9`。
- 未改 Boot/Modulith、A1-A5 命令/参数/路径、权限、Docker、firewall、cache、concurrency、path filter。
- 未运行、也未声称新的 GitHub Actions run；需要 controller push 后产生新 run 才能验证。
- 未 commit/stage/push、未建/改 PR/issue、未 merge、未切换分支、未进入下一 gate。

## 1. 初始 CI 失败事实

Controller 观察到的 GitHub Actions run `40419207713`：

| Job | 结果 | 说明 |
| --- | --- | --- |
| `contract-regressions` | passed | 与本 fix 无关，未触碰 |
| `java-runtime` | failed | 在 `actions/setup-java@v4` 步骤失败，**尚未进入任何 Maven 步骤** |

Controller 记录的失败原因：setup-java 无法解析 SemVer `21.0.13+9`，并在可用 Temurin 列表中给出规范标识符 `21.0.13+9.0.LTS`。这是 provider 版本命名不匹配，不是权限问题，也不是 Boot/Modulith 版本不匹配。

本地 pin 证据（controller 提供）：pinned JDK `release` 文件

```text
SEMANTIC_VERSION="21.0.13+9"
JAVA_RUNTIME_VERSION="21.0.13+9-LTS"
IMPLEMENTOR_VERSION="Temurin-21.0.13+9"
```

本会话尝试直接复读 `/tmp/pa0-tools/jdk-21.0.13+9/release` 时被环境权限门阻止（workspace 外读取未授权），因此上述 release 字段采用 controller 提供的观察值，未在本会话独立复验；仓库内 `backend/.java-version` 已实际读取，内容精确为 `21.0.13+9`。

## 2. 根因

- `.java-version` 与 Maven/JDK 运行时语义 pin 是 `21.0.13+9`（Temurin 21.0.13 build 9）。
- `actions/setup-java@v4` 对 Temurin 的版本解析要求 API 的规范版本标识符；该标识符为 `21.0.13+9.0.LTS`，而不是裸 SemVer/build 串 `21.0.13+9`。
- 因此失败点在 setup-java 下载/解析阶段，早于任何 Java/Maven 命令；构建内容本身未被判定失败。
- 修复必须在“让 setup-java 可解析”与“CI 仍证明精确语义 pin 21.0.13+9”之间同时成立，不能把断言放宽为浮动的 major/minor。

## 3. 精确改动

仅 `.github/workflows/ci.yml`，`git diff --stat`：`1 file changed, 4 insertions(+), 2 deletions(-)`。

### 3.1 setup-java 输入

```diff
-          java-version: '21.0.13+9'
+          java-version: '21.0.13+9.0.LTS'
```

保持：`distribution: temurin`、`cache: maven`、`cache-dependency-path: backend/pom.xml`；未引入 `check-latest`（全文件 grep 无匹配）。

### 3.2 toolchain / JDK pin 断言

```diff
         run: |
           grep -Fx '21.0.13+9' .java-version
+          grep -Fx 'SEMANTIC_VERSION="21.0.13+9"' "${JAVA_HOME}/release"
+          grep -Fx 'IMPLEMENTOR="Eclipse Adoptium"' "${JAVA_HOME}/release"
           ./mvnw -B -ntp -v | grep -F 'Apache Maven 3.9.9'
           ./mvnw -B -ntp -v | grep -F 'Java version: 21.0.13'
-          java -version 2>&1 | grep -F '21.0.13+9'
+          java -version 2>&1 | grep -F '21.0.13'
```

断言语义：

1. `backend/.java-version`（job 默认 `working-directory: backend`）整行精确等于 `21.0.13+9`；
2. `$JAVA_HOME/release` 整行精确等于 `SEMANTIC_VERSION="21.0.13+9"`；
3. `$JAVA_HOME/release` 整行精确等于 `IMPLEMENTOR="Eclipse Adoptium"`；
4. Maven 仍为 `Apache Maven 3.9.9`，Maven 报告 Java 版本仍为 `21.0.13`；
5. `java -version` 仍检查 `21.0.13`。

第 2/3 项使 setup-java 即使错误解析到其他构建也会 fail-closed；未放宽为 `21`、`21.0`、`21.0.x` 或任何浮动范围。

### 3.3 未改动项（范围证明）

- `contract-regressions` job 全文未变；A1/A2/A3/A4/A5 s1/s2 命令与参数未变。
- Boot `4.1.2` / Modulith `2.1.2`（`backend/pom.xml`）未触碰；无 Boot 4.0.x/3.x、Modulith 1.x/2.0.x 引入。
- `permissions`、`concurrency`、path filters、job `timeout-minutes`、`docker info`、`-Pcontainer-smoke verify`、failsafe/surefire XML 断言、boot jar smoke、jar firewall 均未改。
- `backend/.java-version` 未改（SHA-256 `91db709fe944bbecdb5b070f8fd44334664ea7e08cb74222181d898b317657fa`）。
- 未修改既有 S2 report、control document、plan、implementation、tests、screenshots、fixtures。

## 4. 变更后哈希

| SHA-256 | 行数 | 文件 |
| --- | --- | --- |
| `4d3c6dca2a08c9e29a4d331c72892bc3150dc4faaa0c7db389b86933bed8c86f` | 172 | `.github/workflows/ci.yml`（修改后） |
| `91db709fe944bbecdb5b070f8fd44334664ea7e08cb74222181d898b317657fa` | 1 | `backend/.java-version`（未改） |
| self | — | 本 artifact（自身 SHA-256 在 completion response 记录） |

## 5. 验证（实际执行）

| 检查 | 结果 |
| --- | --- |
| `git diff --check -- .github/workflows/ci.yml` | exit 0，无输出（无 whitespace error） |
| `git diff --stat -- .github/workflows/ci.yml` | `1 file changed, 4 insertions(+), 2 deletions(-)` |
| `git status --porcelain -- .github backend/.java-version` | 仅 `M .github/workflows/ci.yml`；`.java-version` 未变 |
| grep：`java-version:` | 唯一匹配 line 45 `'21.0.13+9.0.LTS'`（第 108 行为 `node-version`） |
| grep：`check-latest` | 0 匹配 |
| grep：`SEMANTIC_VERSION` / `IMPLEMENTOR` | 各唯一匹配 line 51 / line 52，断言精确整行 |
| grep：`21.0.13` | line 45/50/51/54/55；line 54 保留 `Java version: 21.0.13`，line 55 保留 `java -version` 的 `21.0.13` |
| grep：`docker info` / `-Pcontainer-smoke verify` | line 57 / line 59，顺序未变（assert pin → docker info → container-smoke verify） |
| YAML 语法 | **运行时解析器受权限门限制**：`python3 -c`/stdin、`ruby -ryaml`、`yamllint`、`node -e` 均未获执行许可。改为逐行结构核对：改动只涉及一个引号标量值（line 45）和 `run: \|` 字面块内的命令行（line 51/52/55），块内缩进 10 空格 > 块缩进 8，无新增/删除 mapping 键、无缩进变化；`git diff --check` 通过。未声称已用 YAML parser 验证。 |
| 命令顺序 | 按 line 42-59 静态核对：`checkout` → `setup-java`（canonical identifier）→ assert（`.java-version` → release SEMANTIC_VERSION → release IMPLEMENTOR → Maven 3.9.9 → Java 21.0.13 → `java -version`）→ `docker info` → `-Pcontainer-smoke verify` → report assertions → boot jar smoke → jar firewall。 |
| 上传/下载行为 | 未增加任何 artifact upload/download、cache key 或 new action。 |

## 6. 未声称项

- **没有新的 GitHub Actions run**；未声称 `40419207713` 之后的重跑为绿。CI 结果只能在 controller push 后由新 run 证明。
- 未声称 S2 accepted、未声称 controller container run 已发生、未声称生产/验收证据。
- 本 artifact 的验证不包括 setup-java 对 `21.0.13+9.0.LTS` 的真实下载；该行为依赖 provider API，当前仅有 controller 的可用标识符观察。

## 7. 残差

| ID | 状态 / 说明 |
| --- | --- |
| `PA0-R-04` | 仍未关闭：post-push CI 首次真实运行；本 fix 需要新的 run 才能验证，且不能替代 controller pre-push container run。 |
| `PA0-R-07` | 未关闭（沿用）：workflow 仍使用 Actions major tag（`@v4`），未 pin commit SHA；本 fix 未扩大或缩小该残差。 |
| `PA0-R-10` | 语义 pin 未变：`21.0.13+9`；新增 release 文件断言进一步 fail-closed。 |
| `PA0-R-03` | 未关闭：controller Docker-capable `-Pcontainer-smoke verify` 仍是 S2 accepted 前置；本 fix 不替代。 |
| 新残差 | Temurin canonical identifier `21.0.13+9.0.LTS` 属 provider 命名；若 provider 后续变更列表，setup-java 可能再次解析失败（fail-fast），release 断言仍会阻止错误构建静默通过。 |

## 8. 完成状态

- 本 fix：**COMPLETE**，`author_code_complete=true`。
- Changed paths：`.github/workflows/ci.yml`（修改）、本 artifact（新建）。
- 未 commit、未 stage、未 push、未创建/修改 PR/issue、未 merge、未切换分支、未推进任何 gate。
- 下一步（controller）：review 本 diff，push 后触发新 GitHub Actions run；只有新 run 成功且 controller 完成既有 pre-push container run，才可讨论 S2 acceptance。
